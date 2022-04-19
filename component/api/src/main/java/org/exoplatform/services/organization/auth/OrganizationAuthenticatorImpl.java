/*
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.services.organization.auth;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.container.component.ComponentRequestLifecycle;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.AccountTemporaryLockedException;
import org.exoplatform.services.organization.DisabledUserException;
import org.exoplatform.services.organization.ExtendedUserHandler;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.Credential;
import org.exoplatform.services.security.DigestPasswordEncrypter;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.services.security.MembershipHashSet;
import org.exoplatform.services.security.PasswordCredential;
import org.exoplatform.services.security.PasswordEncrypter;
import org.exoplatform.services.security.RolesExtractor;
import org.exoplatform.services.security.UsernameCredential;
import org.exoplatform.web.login.recovery.PasswordRecoveryService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import javax.security.auth.login.LoginException;

/**
 * Created by The eXo Platform SAS . An authentication wrapper over Organization
 * service
 * 
 * @author Gennady Azarenkov
 * @version $Id:$
 */

public class OrganizationAuthenticatorImpl implements Authenticator
{

   private static final String     STATUS_NOT_OK        = "ko";

   private static final String     STATUS_OK               = "ok";

   private static final String     ACCOUNT_LOCKED       = "accountLocked";

   private static final String     WRONG_CREDENTIALS    = "wrongCredentials";

   private static final String     SERVICE                 = "service";

   private static final String     LOGIN                   = "login";

   private static final String     OPERATION               = "operation";

   private static final String     STATUS                  = "status";

   private static final String     AUTHENTICATION_ATTEMPTS = "authenticationAttempts";

   private static final String     LATEST_AUTH_TIME        = "latestAuthFailureTime";

   private static final String     MAX_AUTHENTICATION_ATTEMPTS = "maxAuthenticationAttempts";

   private static final String     BLOCKING_TIME        = "blockingTime";

   private int maxAuthenticationAttempts = 5;

   private int blockingTime = 10;

   protected static final Log LOG =
      ExoLogger.getLogger("exo.core.component.organization.api.OrganizationUserRegistry");

   /**
    * The thread local in which we store the last exception that occurs while calling the method 
    * validateUser
    */
   private final ThreadLocal<Exception> lastExceptionOnValidateUser = new ThreadLocal<Exception>();

   private final OrganizationService orgService;

   private final PasswordEncrypter encrypter;

   private final RolesExtractor rolesExtractor;

   private final ListenerService listenerService;

   private List<AuthenticatorPlugin> plugins = new ArrayList<>();

   public OrganizationAuthenticatorImpl(OrganizationService orgService, RolesExtractor rolesExtractor,
                                        PasswordEncrypter encrypter, ListenerService listenerService, InitParams initParams)
   {
      this.orgService = orgService;
      this.encrypter = encrypter;
      this.rolesExtractor = rolesExtractor;
      this.listenerService = listenerService;
      if (initParams != null && initParams.getValueParam(MAX_AUTHENTICATION_ATTEMPTS)!=null) {
         this.maxAuthenticationAttempts=Integer.parseInt(initParams.getValueParam(MAX_AUTHENTICATION_ATTEMPTS).getValue());
      }
      if (initParams != null && initParams.getValueParam(BLOCKING_TIME)!=null) {
         this.blockingTime=Integer.parseInt(initParams.getValueParam(BLOCKING_TIME).getValue());
      }


   }

   public OrganizationAuthenticatorImpl(OrganizationService orgService, RolesExtractor rolesExtractor,
                                        ListenerService listenerService, InitParams initParams)
   {
      this(orgService, rolesExtractor, null, listenerService, initParams);
   }

   public OrganizationAuthenticatorImpl(OrganizationService orgService, ListenerService listenerService, InitParams initParams)
   {
      this(orgService, null, null, listenerService,initParams);
   }

   public OrganizationService getOrganizationService()
   {
      return orgService;
   }

   /*
    * (non-Javadoc)
    * @see
    * org.exoplatform.services.security.Authenticator#createIdentity(java.lang
    * .String)
    */
   public Identity createIdentity(String userId) throws Exception
   {
      Set<MembershipEntry> entries = new MembershipHashSet();
      Collection<Membership> memberships;
      begin(orgService);
      try
      {
         memberships = orgService.getMembershipHandler().findMembershipsByUser(userId);
      }
      finally
      {
         end(orgService);
      }
      if (memberships != null)
      {
         for (Membership membership : memberships)
            entries.add(new MembershipEntry(membership.getGroupId(), membership.getMembershipType()));
      }
      Identity identity = null;
      if (rolesExtractor == null) {
        identity = new Identity(userId, entries);
      } else {
        identity = new Identity(userId, entries, rolesExtractor.extractRoles(userId, entries));
      }
      return identity;
   }

   /*
    * (non-Javadoc)
    * @see
    * org.exoplatform.services.security.Authenticator#validateUser(org.exoplatform
    * .services.security.Credential[])
    */
   public String validateUser(Credential[] credentials) throws LoginException, Exception
   {
      String username = null;
      String password = null;
      Map<String, String> passwordContext= null;
      for (Credential cred : credentials)
      {
         if (cred instanceof UsernameCredential)
         {
            username = ((UsernameCredential)cred).getUsername();
         }
         if (cred instanceof PasswordCredential)
         {
            password = ((PasswordCredential)cred).getPassword();
            passwordContext = ((PasswordCredential)cred).getPasswordContext();
         }
      }
      boolean success = false;
      if (username != null && password != null) {
        if (this.encrypter != null)
           password = new String(encrypter.encrypt(password.getBytes()));
  
        begin(orgService);
        try
        {
           UserHandler userHandler = orgService.getUserHandler();
           if (userHandler.findUserByName(username)!=null) {
              checkLockedAccount(userHandler.findUserByName(username)); // throw exception if account is locked
           }

           if (passwordContext != null && userHandler instanceof ExtendedUserHandler)
           {
              PasswordEncrypter pe = new DigestPasswordEncrypter(username, passwordContext);
              success = ((ExtendedUserHandler)userHandler).authenticate(username, password, pe);
           }
           else
           {
              success = userHandler.authenticate(username, password);
           }
           // No exception occurred
           lastExceptionOnValidateUser.remove();
        }
        catch (DisabledUserException e)
        {
           lastExceptionOnValidateUser.set(e);
           throw new LoginException("The user account " + username.replace("\n", " ").replace("\r", " ") + " is disabled");
        }
        catch (AccountTemporaryLockedException e)
        {
           lastExceptionOnValidateUser.set(e);
           throw new LoginException("The user account " + username.replace("\n", " ").replace("\r", " ")
               + " is temporarily locked "
                                      + "until "+e.getUnlockTime());
        }
        catch (Exception e)
        {
           lastExceptionOnValidateUser.set(e);
           throw e;
        }
        finally
        {
           end(orgService);
        }
      }

      if (!success) {
        for (AuthenticatorPlugin plugin : getPlugins()) {
          try {
            String validatedUserName = plugin.validateUser(credentials);
            if (StringUtils.isNotBlank(validatedUserName)) {
              success = true;
              username = validatedUserName;
              break;
            }
          } catch (Exception e) {
            LOG.debug("Error while authenticating user using plugin {}", plugin.getClass());
          }
        }
      }

      if (!success) {
         saveLastLoginFail(username);
         throw new LoginException("Login failed for " + username.replace("\n", " ").replace("\r", " "));
      } else {
         resetAuthenticationAttempts(username);
      }

      listenerService.broadcast(OrganizationService.USER_AUTHENTICATED_EVENT, orgService, username);

      return username;
   }

   public void addAuthenticatorPlugin(AuthenticatorPlugin plugin) {
      this.plugins.add(plugin);
   }

   public List<AuthenticatorPlugin> getPlugins() {
      return plugins;
   }

   public void setPlugins(List<AuthenticatorPlugin> plugins) {
     this.plugins = plugins;
   }

   public void begin(OrganizationService orgService) throws Exception
   {
      if (orgService instanceof ComponentRequestLifecycle)
      {
         RequestLifeCycle.begin((ComponentRequestLifecycle)orgService);
      }
   }

   public void end(OrganizationService orgService) throws Exception
   {
      if (orgService instanceof ComponentRequestLifecycle)
      {
         RequestLifeCycle.end();
      }
   }

   /**
    * {@inheritDoc}
    */
   public Exception getLastExceptionOnValidateUser()
   {
      Exception e = lastExceptionOnValidateUser.get();
      if (e != null)
      {
         // To prevent a memory leak, we apply an auto-cleanup strategy
         lastExceptionOnValidateUser.remove();
      }
      return e;
   }


   private void checkLockedAccount(User user) throws AccountTemporaryLockedException {
      try {
         UserProfile profile = orgService.getUserProfileHandler().findUserProfileByName(user.getUserName());
         if (profile != null) {
            //if there is no userProfile, the user have never fail his login, we do no lock him

            int currentNbFail =
                profile.getAttribute(AUTHENTICATION_ATTEMPTS) != null ? Integer.parseInt(profile.getAttribute(AUTHENTICATION_ATTEMPTS))
                                                                      : 0;
            Instant latestAuthFailureTime =
                Instant.ofEpochMilli(profile.getAttribute(LATEST_AUTH_TIME) != null ? Long.parseLong(profile.getAttribute(LATEST_AUTH_TIME))
                                                                                           : Instant.EPOCH.toEpochMilli());
            if (currentNbFail >= this.maxAuthenticationAttempts
                && latestAuthFailureTime.plus(this.blockingTime, ChronoUnit.MINUTES)
                                        .isAfter(Instant.now())) {

               LOG.warn(SERVICE + "=" + LOGIN + " " + OPERATION + "=" + LOGIN + " " + STATUS + "=" + STATUS_NOT_OK
                            + " parameters=\"username:{}, authenticationAttempts:{}, maxAuthenticationAttempts:{}, latestAuthFailureTime={}, "
                            + "lockTimeInMinutes={}, unlockTime={}\"" + " error_msg=\"Account is locked\"",
                        user.getUserName(),
                        currentNbFail,
                        this.maxAuthenticationAttempts,
                        latestAuthFailureTime,
                        this.blockingTime,
                        latestAuthFailureTime.plus(this.blockingTime, ChronoUnit.MINUTES));
               broacastFailedLoginEvent(user.getUserName(), STATUS_NOT_OK, ACCOUNT_LOCKED);
               throw new AccountTemporaryLockedException(user.getUserName(),
                                                         latestAuthFailureTime.plus(this.blockingTime,
                                                                                    ChronoUnit.MINUTES));
            }
         }
      } catch (AccountTemporaryLockedException atle) {
         throw atle;
      } catch (Exception e) {
         LOG.error("Unable to get gatein user profile for user {}", user.getUserName(), e);
      }
   }

   private void broacastFailedLoginEvent(String userId, String status, String reason) {
      ExoContainer currentContainer = ExoContainerContext.getCurrentContainer();
      if (currentContainer == null || (currentContainer instanceof RootContainer)) {
         currentContainer = PortalContainer.getInstance();
      }
      ListenerService listenerService = currentContainer.getComponentInstanceOfType(ListenerService.class);

      try {
         Map<String, String> info = new HashMap<>();
         info.put("user_id", userId);
         info.put(STATUS, status);
         info.put("reason", reason);

         listenerService.broadcast("login.failed", null, info);
      } catch (Exception e) {
         LOG.error("Error while broadcasting event 'login.failed' for user '{}'", userId, e);
      }
   }

   private void saveLastLoginFail(String username) {
      try {
         User user = orgService.getUserHandler().findUserByName(username);
         if (user != null) {
            UserProfile profile = orgService.getUserProfileHandler().findUserProfileByName(username);
            if (profile == null) {
               profile = orgService.getUserProfileHandler().createUserProfileInstance(username);
            }
            int currentNbFail =
                profile.getAttribute(AUTHENTICATION_ATTEMPTS) != null ? Integer.parseInt(profile.getAttribute(AUTHENTICATION_ATTEMPTS))
                                                                      : 0;
            currentNbFail++;
            profile.setAttribute(AUTHENTICATION_ATTEMPTS, String.valueOf(currentNbFail));
            Instant now = Instant.now();
            profile.setAttribute(LATEST_AUTH_TIME, String.valueOf(now.toEpochMilli()));
            orgService.getUserProfileHandler().saveUserProfile(profile, true);

            if (currentNbFail >= this.maxAuthenticationAttempts) {
               LOG.warn(SERVICE + "=" + LOGIN + " " + OPERATION + "=" + LOGIN + " " + STATUS + "=" + STATUS_NOT_OK
                            + " parameters=\"username:{}, authenticationAttempts:{}, maxAuthenticationAttempts:{}, latestAuthFailureTime={}, "
                            + "lockTimeInMinutes={}, unlockTime={}\"" + " error_msg=\"Account is locked\"",
                        user.getUserName(),
                        currentNbFail,
                        this.maxAuthenticationAttempts,
                        now,
                        this.blockingTime,
                        now.plus(this.blockingTime, ChronoUnit.MINUTES));

               broacastFailedLoginEvent(user.getUserName(), STATUS_NOT_OK, ACCOUNT_LOCKED);

               ExoContainer container = ExoContainerContext.getCurrentContainer();
               PasswordRecoveryService passwordRecoveryService = container.getComponentInstanceOfType(PasswordRecoveryService.class);
               passwordRecoveryService.sendAccountLockedEmail(user, Locale.ENGLISH);

            } else {
               LOG.warn(SERVICE + "=" + LOGIN + " " + OPERATION + "=" + LOGIN + " " + STATUS + "=" + STATUS_NOT_OK
                            + " parameters=\"username:{}, authenticationAttempts:{}, latestAuthFailureTime:{}, maxAuthenticationAttempts:{}\""
                            + " error_msg=\"Login failed\"",
                        username,
                        currentNbFail,
                        now,
                        this.maxAuthenticationAttempts);
               broacastFailedLoginEvent(user.getUserName(), STATUS_NOT_OK, WRONG_CREDENTIALS);

            }
         }
      } catch (Exception e) {
         LOG.error("Unable to get gatein user profile for user {}", username, e);
      }

   }

   private void resetAuthenticationAttempts(String username) {
      try {
         User user = orgService.getUserHandler().findUserByName(username);
         if (user != null) {
            UserProfile profile = orgService.getUserProfileHandler().findUserProfileByName(username);
            if (profile == null) {
               profile = orgService.getUserProfileHandler().createUserProfileInstance(username);
            }
            profile.setAttribute(AUTHENTICATION_ATTEMPTS, String.valueOf(0));
            orgService.getUserProfileHandler().saveUserProfile(profile, true);
            if (LOG.isDebugEnabled()) {
               LOG.debug(SERVICE + "=" + LOGIN + " " + OPERATION + "=" + LOGIN + " " + STATUS + "=" + STATUS_OK
                             + " parameters=\"username:{}, authenticationAttempts:{}, maxAuthenticationAttempts:{}\"",
                         username,
                         0,
                         this.maxAuthenticationAttempts);
            }
         }
      } catch (Exception e) {
         LOG.error("Unable to get gatein user profile for user {}", username, e);
      }

   }
}
