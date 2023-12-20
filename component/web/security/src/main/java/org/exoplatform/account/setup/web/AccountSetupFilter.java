package org.exoplatform.account.setup.web;

import java.io.IOException;

import org.exoplatform.web.filter.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.PortalContainer;

/**
 * @author <a href="fbradai@exoplatform.com">Fbradai</a>
 */
public class AccountSetupFilter implements Filter {
  private static final String PLF_PLATFORM_EXTENSION_SERVLET_CTX = "/portal";

  private static final String ACCOUNT_SETUP_SERVLET              = "/accountSetup";
  private static final String ACCOUNT_SETUP_ACTION              = "/accountSetupAction";
  
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpServletRequest = (HttpServletRequest) request;
    HttpServletResponse httpServletResponse = (HttpServletResponse) response;

    ExoContainer container = PortalContainer.getInstance();
    AccountSetupService accountSetupService = container.getComponentInstanceOfType(AccountSetupService.class);

    boolean setupDone = accountSetupService.mustSkipAccountSetup();

    String requestUri = httpServletRequest.getRequestURI();
    boolean isRestUri = requestUri.contains(container.getContext().getRestContextName());
    boolean isAccountSetupUri = requestUri.equals(PLF_PLATFORM_EXTENSION_SERVLET_CTX+ACCOUNT_SETUP_SERVLET) ||
        requestUri.equals(PLF_PLATFORM_EXTENSION_SERVLET_CTX+ACCOUNT_SETUP_ACTION);
    if (!isAccountSetupUri && !setupDone && !isRestUri) {
      ServletContext platformExtensionContext = httpServletRequest.getSession()
                                                                  .getServletContext()
                                                                  .getContext(PLF_PLATFORM_EXTENSION_SERVLET_CTX);
      platformExtensionContext.getRequestDispatcher(ACCOUNT_SETUP_SERVLET).forward(httpServletRequest, httpServletResponse);
      return;
    }
    chain.doFilter(request, response);
  }
}
