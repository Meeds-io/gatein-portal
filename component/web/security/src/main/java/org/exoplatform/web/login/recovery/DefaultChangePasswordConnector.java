package org.exoplatform.web.login.recovery;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.web.security.security.CookieTokenService;

public class DefaultChangePasswordConnector extends ChangePasswordConnector {
  
  private OrganizationService organizationService;

  private CookieTokenService  cookieTokenService;

  public final static String LOG_SERVICE_NAME = "changePassword";
  
  protected static Log log = ExoLogger.getLogger(DefaultChangePasswordConnector.class);
  
  public DefaultChangePasswordConnector(InitParams initParams,
                                        OrganizationService organizationService,
                                        CookieTokenService cookieTokenService) {
    this.organizationService=organizationService;
    this.cookieTokenService = cookieTokenService;

  }
  
  /**
   * @return the allowChangeExternalPassword
   */
  @Override
  public boolean isAllowChangeExternalPassword() {
    return false;
  }
  
  @Override
  public void changePassword(final String username, final String password) throws Exception {
    User user = organizationService.getUserHandler().findUserByName(username);
    
    if (user.isInternalStore()) {
      changeInternalPassword(user, password);
    } else {
      throw new Exception("Change password in external store in not allowed");
    }
  }
  
  private void changeInternalPassword(User user, String password) throws Exception {
    long startTime = System.currentTimeMillis();
    user.setPassword(password);
    organizationService.getUserHandler().saveUser(user, true);
    cookieTokenService.deleteTokensByUsernameAndType(user.getUserName(), "");
    long totalTime = System.currentTimeMillis() - startTime;

    log.info("service={} operation={} parameters=\"user:{}\" status=ok duration_ms={}",
             LOG_SERVICE_NAME, "changeInternalPassword", user.getUserName(), totalTime);
  }
}
