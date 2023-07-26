/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2023 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.gatein.portal.idm.impl.store.attribute;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.organization.idm.PicketLinkIDMService;
import org.picketlink.idm.api.Attribute;
import org.picketlink.idm.api.User;
import org.picketlink.idm.common.exception.IdentityException;
import org.picketlink.idm.impl.api.PasswordCredential;
import org.picketlink.idm.impl.api.session.IdentitySessionImpl;
import org.picketlink.idm.impl.api.session.managers.AttributesManagerImpl;
import org.picketlink.idm.impl.credential.DatabaseReadingSaltEncoder;

import java.util.HashMap;
import java.util.Map;

public class ExtendedAttributeManager extends AttributesManagerImpl {

  public static final String          OLD_PASSWORD_SALT_USER_ATTRIBUTE = "passwordSalt";

  public static final String          PASSWORD_SALT_USER_ATTRIBUTE     = "passwordSalt128";

  private static PicketLinkIDMService idmService;

  public ExtendedAttributeManager(IdentitySessionImpl session) {
    super(session);
  }

  @Override
  public boolean validatePassword(User user, String password) throws IdentityException {

    Attribute salt128 = getAttribute(user.getKey(), PASSWORD_SALT_USER_ATTRIBUTE);
    if (salt128 != null) {
      return super.validatePassword(user, password);
    } else {
      DatabaseReadingSaltEncoder oldCredentialEncoder = new DatabaseReadingSaltEncoder();
      oldCredentialEncoder.setIdentitySession(getIdentitySession());
      oldCredentialEncoder.initialize(getCredentialEncoderProps(),
                                      getIdmService().getIdentityConfiguration().getIdentityConfigurationRegistry());
      if (getRepository().validateCredential(getInvocationContext(),
                                             createIdentityObject(user),
                                             new PasswordCredential(password, oldCredentialEncoder, user.getKey()))) {
        removeAttributes(user.getKey(), new String[] { OLD_PASSWORD_SALT_USER_ATTRIBUTE });
        updatePassword(user, password);
        return true;
      }
    }
    return false;
  }

  private Map<String, String> getCredentialEncoderProps() {
    Map<String, String> props = new HashMap<>();
    getIdmService().getConfigMD().getRealms().get(0).getOptions().forEach((k, v) -> props.put(k, v.get(0)));
    return props;
  }

  private static PicketLinkIDMService getIdmService() {
    if (idmService == null) {
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      idmService = container.getComponentInstanceOfType(PicketLinkIDMService.class);
    }
    return idmService;
  }
}
