/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
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

package org.exoplatform.web.login.recovery;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.wci.security.Credentials;

import org.exoplatform.commons.utils.I18N;
import org.exoplatform.commons.utils.MailUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.Constants;
import org.exoplatform.services.mail.MailService;
import org.exoplatform.services.mail.Message;
import org.exoplatform.services.organization.*;
import org.exoplatform.services.resources.LocaleContextInfo;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.controller.router.Router;
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
    private final WebAppController webController;

    public PasswordRecoveryServiceImpl(OrganizationService orgService, MailService mailService, ResourceBundleService bundleService, RemindPasswordTokenService remindPasswordTokenService, WebAppController controller) {
        this.orgService = orgService;
        this.mailService = mailService;
        this.bundleService = bundleService;
        this.remindPasswordTokenService = remindPasswordTokenService;
        this.webController = controller;
    }

    @Override
    public Credentials verifyToken(String tokenId) {
        Token token = remindPasswordTokenService.getToken(tokenId);
        if (token == null || token.isExpired()) {
            return null;
        }
        return token.getPayload();
    }

    @Override
    public boolean changePass(final String tokenId, final String username, final String password) {
        try {
            User user = orgService.getUserHandler().findUserByName(username);
            user.setPassword(password);
            orgService.getUserHandler().saveUser(user, true);

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
    public boolean sendRecoverPasswordEmail(User user, Locale defaultLocale, HttpServletRequest req) {
        if (user == null) {
            throw new IllegalArgumentException("User or Locale must not be null");
        }

        Locale locale = getLocaleOfUser(user.getUserName(), defaultLocale);

        PortalContainer container = PortalContainer.getCurrentInstance(req.getServletContext());

        ResourceBundle bundle = bundleService.getResourceBundle(bundleService.getSharedResourceBundleNames(), locale);

        Credentials credentials = new Credentials(user.getUserName(), "");
        String tokenId = remindPasswordTokenService.createToken(credentials);

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

        String emailBody = buildEmailBody(user, bundle, url.toString());
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

    private String buildEmailBody(User user, ResourceBundle bundle, String link) {
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
