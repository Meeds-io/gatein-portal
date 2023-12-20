package org.exoplatform.account.setup.web;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import org.exoplatform.container.PortalContainer;

/**
 * @author <a href="fbradai@exoplatform.com">Fbradai</a>
 */
public class AccountSetupViewServlet extends HttpServlet {

  private final static String AS_JSP_RESOURCE    = "/WEB-INF/jsp/welcome-screens/accountSetup.jsp";

  private AccountSetupService accountSetupService;

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    PortalContainer container = PortalContainer.getInstance();
    accountSetupService = container.getComponentInstanceOfType(AccountSetupService.class);
    if (accountSetupService.mustSkipAccountSetup()) {
      response.sendRedirect("/");
    } else {
      // Redirect to requested page
      HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
        @Override
        public String getContextPath() {
          return "/portal";
        }
      };
      container.getPortalContext().getRequestDispatcher(AS_JSP_RESOURCE).forward(wrappedRequest, response);
    }
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doPost(request, response);
  }

}
