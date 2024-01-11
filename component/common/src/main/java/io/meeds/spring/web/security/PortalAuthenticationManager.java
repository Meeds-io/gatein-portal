/*
 * This file is part of the Meeds project (https://meeds.io/).
 * 
 * Copyright (C) 2020 - 2022 Meeds Association contact@meeds.io
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package io.meeds.spring.web.security;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.jaas.JaasGrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.ConversationRegistry;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.StateKey;
import org.exoplatform.services.security.web.HttpSessionStateKey;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class PortalAuthenticationManager implements AuthenticationProvider {

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    HttpServletRequest request = requestAttributes.getRequest();

    try {
      ConversationState conversationState = getCurrentState(request);
      Principal userPrincipal = request.getUserPrincipal();
      if (conversationState == null
          || (!conversationState.getIdentity().isMemberOf("/platform/users")
              && !conversationState.getIdentity().isMemberOf("/platform/externals"))) {
        return new AnonymousAuthenticationToken(IdentityConstants.ANONIM,
                                                authentication.getPrincipal(),
                                                Collections.singletonList(new JaasGrantedAuthority("guests",
                                                                                                   userPrincipal)));
      } else {
        List<GrantedAuthority> authorities = getAuthorities(conversationState.getIdentity(),
                                                            userPrincipal);
        List<GrantedAuthority> extendedAuthorities = getExtendedAuthorities(userPrincipal);
        if (CollectionUtils.isNotEmpty(extendedAuthorities)) {
          authorities = new ArrayList<>(authorities);
          authorities.addAll(extendedAuthorities);
        }
        authentication.setAuthenticated(true);
        return new PreAuthenticatedAuthenticationToken(authentication.getPrincipal(),
                                                       null,
                                                       authorities);
      }
    } catch (Exception e) {
      throw new AuthenticationServiceException("An unknown error is encountered while authenticating user", e);
    }
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return true;
  }

  public ConversationState getCurrentState(HttpServletRequest httpRequest) throws Exception {
    ConversationState state = ConversationState.getCurrent();
    if (state != null) {
      return state;
    }

    String userId = httpRequest.getRemoteUser();
    // only if user authenticated, otherwise there is no reason to do anythings
    if (userId != null) {
      StateKey stateKey = new HttpSessionStateKey(httpRequest.getSession());

      state = getStateBySessionId(userId, stateKey);
      if (state == null) {
        return buildState(userId, stateKey);
      } else {
        return state;
      }
    } else {
      return new ConversationState(new Identity(IdentityConstants.ANONIM));
    }
  }

  /**
   * A method to allow extending attributed roles to user in current portal
   * context
   * 
   * @param userPrincipal current user {@link Principal}
   * @return {@link List} of {@link GrantedAuthority} to add to current user
   */
  protected List<GrantedAuthority> getExtendedAuthorities(Principal userPrincipal) {
    return Collections.emptyList();
  }

  private List<GrantedAuthority> getAuthorities(Identity identity, Principal principal) {
    return identity.getRoles()
                   .stream()
                   .map(role -> (GrantedAuthority) new JaasGrantedAuthority(role, principal))
                   .toList();
  }

  private ConversationState buildState(String userId, StateKey stateKey) throws Exception {
    Identity identity = buildIdentity(userId);
    if (identity == null) {
      return null;
    } else {
      return buildState(identity, stateKey);
    }
  }

  private ConversationState buildState(Identity identity, StateKey stateKey) {
    ConversationRegistry conversationRegistry = ExoContainerContext.getService(ConversationRegistry.class);

    ConversationState state = new ConversationState(identity);
    conversationRegistry.register(stateKey, state);
    return state;
  }

  private Identity buildIdentity(String userId) throws Exception {
    IdentityRegistry identityRegistry = ExoContainerContext.getService(IdentityRegistry.class);
    Authenticator authenticator = ExoContainerContext.getService(Authenticator.class);

    Identity identity = identityRegistry.getIdentity(userId);
    if (identity == null) {
      identity = authenticator.createIdentity(userId);
      identityRegistry.register(identity);
    }
    return identity;
  }

  private ConversationState getStateBySessionId(String userId, StateKey stateKey) {
    ConversationRegistry conversationRegistry = ExoContainerContext.getService(ConversationRegistry.class);

    ConversationState state = conversationRegistry.getState(stateKey);
    if (state != null && !userId.equals(state.getIdentity().getUserId())) {
      state = null;
      conversationRegistry.unregister(stateKey, false);
    }
    return state;
  }

}
