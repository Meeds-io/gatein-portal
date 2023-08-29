package org.exoplatform.mock;

import org.exoplatform.services.organization.User;
import org.exoplatform.web.login.recovery.ChangePasswordConnector;
import org.exoplatform.web.login.recovery.PasswordRecoveryService;
import org.gatein.wci.security.Credentials;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;

public class MockPasswordRecoveryService implements PasswordRecoveryService {
  
  ChangePasswordConnector mockChangePasswordConnector;
  
  public MockPasswordRecoveryService() {
    this.mockChangePasswordConnector = new MockChangePasswordConnector();
    
  }
  
  @Override
  public void addConnector(ChangePasswordConnector connector) {
  
  }
  
  @Override
  public String verifyToken(String tokenId, String type) {
    return null;
  }
  
  @Override
  public String verifyToken(String tokenId) {
    return null;
  }
  
  @Override
  public boolean changePass(String tokenId, String tokenType, String username, String password) {
    return false;
  }
  
  @Override
  public boolean sendRecoverPasswordEmail(User user, Locale defaultLocale, HttpServletRequest req) {
    return false;
  }
  
  @Override
  public boolean sendOnboardingEmail(User user, Locale defaultLocale, StringBuilder url) {
    return false;
  }
  
  @Override
  public String sendExternalRegisterEmail(String sender, String email, Locale locale, String space, StringBuilder url) throws
                                                                                                                       Exception {
    return null;
  }
  
  @Override
  public boolean sendAccountCreatedConfirmationEmail(String sender, Locale locale, StringBuilder url) {
    return false;
  }

  @Override
  public boolean allowChangePassword(String username) throws Exception {
    return true;
  }
  
  @Override
  public String getPasswordRecoverURL(String tokenId, String lang) {
    return null;
  }
  
  @Override
  public String getOnboardingURL(String tokenId, String lang) {
    return null;
  }
  
  @Override
  public String getExternalRegistrationURL(String tokenId, String lang) {
    return null;
  }
  
  @Override
  public ChangePasswordConnector getActiveChangePasswordConnector() {
    return this.mockChangePasswordConnector;
  }

  @Override
  public void deleteToken(String tokenId, String type) {
    // Delete Token
  }

  @Override
  public boolean sendAccountVerificationEmail(String data, String username, String firstName, String lastName, String email, Locale locale, StringBuilder url) {
    return false;
  }
}
