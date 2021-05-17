package org.exoplatform.web.login.recovery;

import org.exoplatform.container.component.BaseComponentPlugin;

public abstract class ChangePasswordConnector  extends BaseComponentPlugin {
  
  protected abstract void changePassword(final String username, final String password) throws Exception;
  protected abstract boolean isAllowChangeExternalPassword();
}
