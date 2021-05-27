package org.exoplatform.mock;

import org.exoplatform.web.login.recovery.ChangePasswordConnector;

public class MockChangePasswordConnector extends ChangePasswordConnector {
  @Override
  public void changePassword(String username, String password) throws Exception {
  
  }
  
  @Override
  protected boolean isAllowChangeExternalPassword() {
    return false;
  }
}
