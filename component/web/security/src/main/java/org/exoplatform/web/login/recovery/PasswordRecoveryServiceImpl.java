/*
 * Copyright (C) 2015 eXo Platform SAS.
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

package org.exoplatform.web.login.recovery;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.exoplatform.container.xml.InitParams;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.wci.security.Credentials;

import org.exoplatform.commons.utils.I18N;
import org.exoplatform.commons.utils.MailUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.Constants;
import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.services.mail.MailService;
import org.exoplatform.services.mail.Message;
import org.exoplatform.services.organization.*;
import org.exoplatform.services.resources.LocaleContextInfo;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.controller.router.Router;
import org.exoplatform.web.login.onboarding.OnboardingHandler;
import org.exoplatform.web.security.Token;
import org.exoplatform.web.security.security.RemindPasswordTokenService;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
public class PasswordRecoveryServiceImpl implements PasswordRecoveryService {
  
    protected static Logger                  log                = LoggerFactory.getLogger(PasswordRecoveryServiceImpl.class);

    private final OrganizationService orgService;
    private final MailService mailService;
    private final ResourceBundleService bundleService;
    private final RemindPasswordTokenService remindPasswordTokenService;
    private final BrandingService brandingService;
    private final WebAppController webController;
    
    
    private String changePasswordConnectorName;
    private Map<String,ChangePasswordConnector> changePasswordConnectorMap;

    
    public PasswordRecoveryServiceImpl(InitParams initParams, OrganizationService orgService, MailService mailService,
                                       ResourceBundleService bundleService, RemindPasswordTokenService remindPasswordTokenService, WebAppController controller, BrandingService brandingService) {
        this.orgService = orgService;
        this.mailService = mailService;
        this.bundleService = bundleService;
        this.remindPasswordTokenService = remindPasswordTokenService;
        this.webController = controller;
        this.brandingService = brandingService;
        this.changePasswordConnectorMap=new HashMap<>();
        this.changePasswordConnectorName = initParams.getValueParam("changePasswordConnector").getValue();
   
    }

    @Override
    public void addConnector(ChangePasswordConnector connector) {
        if (!this.changePasswordConnectorMap.containsKey(connector.getName())) {
            changePasswordConnectorMap.put(connector.getName(),connector);
        }
    }
    
    
    @Override
    public Credentials verifyToken(String tokenId, String type) {
        Token token = remindPasswordTokenService.getToken(tokenId,type);
        if (token == null || token.isExpired()) {
            return null;
        }
        return token.getPayload();
    }
    
    @Override
    public Credentials verifyToken(String tokenId) {
        return verifyToken(tokenId,"");
    }
    
    @Override
    public boolean allowChangePassword(String username) throws Exception {
      User user = orgService.getUserHandler().findUserByName(username);//To be changed later by checking internal store information from social user profile
      return user != null && (user.isInternalStore() || this.changePasswordConnectorMap.get(this.changePasswordConnectorName).isAllowChangeExternalPassword());
    }
    
    @Override
    public boolean changePass(final String tokenId, final String username, final String password) {
        try {
            this.changePasswordConnectorMap.get(this.changePasswordConnectorName).changePassword(username,password);
            try {
                remindPasswordTokenService.deleteToken(tokenId);
            } catch (Exception ex) {
                log.warn("Can not delete token: " + tokenId, ex);
            }

            return true;
        } catch (Exception ex) {
            log.error("Can not change pass for user: " + username, ex);
            return false;
        }
    }
    
    @Override
    public boolean sendOnboardingEmail(User user, Locale locale, StringBuilder url) {
        if (user == null) {
            throw new IllegalArgumentException("User or Locale must not be null");
        }

        ResourceBundle bundle = bundleService.getResourceBundle(bundleService.getSharedResourceBundleNames(), locale);

        Credentials credentials = new Credentials(user.getUserName(), "");
        String tokenId = remindPasswordTokenService.createToken(credentials, remindPasswordTokenService.ONBOARD_TOKEN);
        StringBuilder redirectUrl = new StringBuilder();
        redirectUrl.append(url);
        redirectUrl.append("/" + OnboardingHandler.NAME);
        redirectUrl.append("?lang=" + I18N.toTagIdentifier(locale));
        redirectUrl.append("&token=" + tokenId);
        String emailBody = buildOnboardingEmailBody(user, bundle, redirectUrl.toString());
        String emailSubject = bundle.getString("onboarding.email.header") + " " + brandingService.getCompanyName();

        String senderName = MailUtils.getSenderName();
        String from = MailUtils.getSenderEmail();
        if (senderName != null && !senderName.trim().isEmpty()) {
            from = senderName + " <" + from + ">";
        }

        Message message = new Message();
        message.setFrom(from);
        message.setTo(user.getEmail());
        message.setSubject(emailSubject);
        message.setBody(emailBody);
        message.setMimeType("text/html");

        try {
            mailService.sendMessage(message);
        } catch (Exception ex) {
            log.error("Failure to send onboarding email", ex);
            return false;
        }

        return true;
    }
    
    private String buildOnboardingEmailBody(User user, ResourceBundle bundle, String link) {
      String content;
      InputStream input = this.getClass().getClassLoader().getResourceAsStream("conf/onBoarding_email_template.html");
      if (input == null) {
          content = "";
      } else {
          content = resolveLanguage(input, bundle);
      }

      content = content.replaceAll("\\$\\{USER_DISPLAY_NAME\\}", user.getDisplayName());
      content = content.replaceAll("\\$\\{COMPANY_NAME\\}", brandingService.getCompanyName());
      content = content.replaceAll("\\$\\{RESET_PASSWORD_LINK\\}", link);

      return content;
  }

    @Override
    public boolean sendEmailForExternalUser(String sender, String email, Locale locale, String space, StringBuilder url) throws Exception {

        UserHandler uHandler = orgService.getUserHandler();
        String senderFullName = uHandler.findUserByName(sender).getDisplayName();

        ResourceBundle bundle = bundleService.getResourceBundle(bundleService.getSharedResourceBundleNames(), locale);

        Credentials credentials = new Credentials(email, "");
        String tokenId = remindPasswordTokenService.createToken(credentials, remindPasswordTokenService.EXTERNAL_REGISTRATION_TOKEN);
        StringBuilder redirectUrl = new StringBuilder();
        redirectUrl.append(url);
        redirectUrl.append("/external-registration");
        redirectUrl.append("?lang=" + I18N.toTagIdentifier(locale));
        redirectUrl.append("&token=" + tokenId);

        String emailBody = buildExternalEmailBody(senderFullName, space, redirectUrl.toString(), bundle);
        String emailSubject = senderFullName + " " + bundle.getString("external.email.subject") + " " + brandingService.getCompanyName() + " : " + space;

        String senderName = MailUtils.getSenderName();
        String from = MailUtils.getSenderEmail();
        if (senderName != null && !senderName.trim().isEmpty()) {
            from = senderName + " <" + from + ">";
        }

        Message message = new Message();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject(emailSubject);
        message.setBody(emailBody);
        message.setMimeType("text/html");

        try {
            mailService.sendMessage(message);
        } catch (Exception ex) {
            log.error("Failure to send external user email", ex);
            return false;
        }

        return true;
    }

    private String buildExternalEmailBody(String sender, String space, String link, ResourceBundle bundle) {
        String content;
        InputStream input = this.getClass().getClassLoader().getResourceAsStream("conf/external_email_template.html");
        if (input == null) {
            content = "";
        } else {
            content = resolveLanguage(input, bundle);
        }

        content = content.replaceAll("\\$\\{SENDER_DISPLAY_NAME\\}", sender);
        content = content.replaceAll("\\$\\{COMPANY_NAME\\}", brandingService.getCompanyName());
        content = content.replaceAll("\\$\\{SPACE_DISPLAY_NAME\\}", space);
        content = content.replaceAll("\\$\\{EXTERNAL_REGISTRATION_LINK\\}", link);

        return content;
    }

    @Override
    public boolean sendRecoverPasswordEmail(User user, Locale defaultLocale, HttpServletRequest req) {
        if (user == null) {
            throw new IllegalArgumentException("User or Locale must not be null");
        }

        Locale locale = getLocaleOfUser(user.getUserName(), defaultLocale);

        PortalContainer container = PortalContainer.getCurrentInstance(req.getServletContext());

        ResourceBundle bundle = bundleService.getResourceBundle(bundleService.getSharedResourceBundleNames(), locale);

        Credentials credentials = new Credentials(user.getUserName(), "");
        String tokenId = remindPasswordTokenService.createToken(credentials,remindPasswordTokenService.FORGOT_PASSWORD_TOKEN);

        Router router = webController.getRouter();
        Map<QualifiedName, String> params = new HashMap<>();
        params.put(WebAppController.HANDLER_PARAM, PasswordRecoveryHandler.NAME);
        params.put(PasswordRecoveryHandler.TOKEN, tokenId);
        params.put(PasswordRecoveryHandler.LANG, I18N.toTagIdentifier(locale));

        StringBuilder url = new StringBuilder();
        url.append(req.getScheme()).append("://").append(req.getServerName());
        if (req.getServerPort() != 80 && req.getServerPort() != 443) {
            url.append(':').append(req.getServerPort());
        }
        url.append(container.getPortalContext().getContextPath());
        url.append(router.render(params));

        String emailBody = buildRecoverEmailBody(user, bundle, url.toString());
        String emailSubject = getEmailSubject(user, bundle);

        String senderName = MailUtils.getSenderName();
        String from = MailUtils.getSenderEmail();
        if (senderName != null && !senderName.trim().isEmpty()) {
            from = senderName + " <" + from + ">";
        }

        Message message = new Message();
        message.setFrom(from);
        message.setTo(user.getEmail());
        message.setSubject(emailSubject);
        message.setBody(emailBody);
        message.setMimeType("text/html");

        try {
            mailService.sendMessage(message);
        } catch (Exception ex) {
            log.error("Failure to send recover password email", ex);
            return false;
        }

        return true;
    }

    private Locale getLocaleOfUser(String username, Locale defLocale) {
        try {
            UserProfile profile = orgService.getUserProfileHandler().findUserProfileByName(username);
            String lang = profile == null ? null : profile.getUserInfoMap().get(Constants.USER_LANGUAGE);
            return (lang != null) ? LocaleContextInfo.getLocale(lang) : defLocale;
        } catch (Exception ex) { //NOSONAR
            log.debug("Can not load user profile language", ex);
            return defLocale;
        }
    }

    private String buildRecoverEmailBody(User user, ResourceBundle bundle, String link) {
        String content;
        InputStream input = this.getClass().getClassLoader().getResourceAsStream("conf/forgot_password_email_template.html");
        if (input == null) {
            content = "";
        } else {
            content = resolveLanguage(input, bundle);
        }

        content = content.replaceAll("\\$\\{FIRST_NAME\\}", user.getFirstName());
        content = content.replaceAll("\\$\\{USERNAME\\}", user.getUserName());
        content = content.replaceAll("\\$\\{RESET_PASSWORD_LINK\\}", link);

        return content;
    }

    private String resolveLanguage(InputStream input, ResourceBundle bundle) {
        // Read from input string
        StringBuffer content = new StringBuffer();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String line;
            while ((line = reader.readLine()) != null) {
                if (content.length() > 0) {
                    content.append("\n");
                }
                resolveLanguage(content, line, bundle);
            }
        } catch (IOException ex) {
            log.error(ex);
        }
        return content.toString();
    }

    private static final Pattern PATTERN = Pattern.compile("&\\{([a-zA-Z0-9\\.]+)\\}");
    private void resolveLanguage(StringBuffer sb, String input, ResourceBundle bundle) {
        Matcher matcher = PATTERN.matcher(input);
        while (matcher.find()) {
            String key = matcher.group(1);
            String resource;
            try {
                resource = bundle.getString(key);
            } catch (MissingResourceException ex) {
                resource = key;
            }
            matcher.appendReplacement(sb, resource);
        }
        matcher.appendTail(sb);
    }

    // These method will be overwrite on Platform project
    protected String getEmailSubject(User user, ResourceBundle bundle) {
        return bundle.getString("gatein.forgotPassword.email.subject");
    }
    
    @Override
    public String getOnboardingURL(String tokenId, String lang) {
        Router router = webController.getRouter();
        Map<QualifiedName, String> params = new HashMap<>();
        params.put(WebAppController.HANDLER_PARAM, OnboardingHandler.NAME);
        if (tokenId != null) {
          params.put(OnboardingHandler.TOKEN, tokenId);
        }
        if (lang != null) {
          params.put(OnboardingHandler.LANG, lang);
        }
        return router.render(params);
    }

    @Override
    public String getPasswordRecoverURL(String tokenId, String lang) {
        Router router = webController.getRouter();
        Map<QualifiedName, String> params = new HashMap<>();
        params.put(WebAppController.HANDLER_PARAM, PasswordRecoveryHandler.NAME);
        if (tokenId != null) {
          params.put(PasswordRecoveryHandler.TOKEN, tokenId);
        }
        if (lang != null) {
          params.put(PasswordRecoveryHandler.LANG, lang);
        }
        return router.render(params);
    }
}
