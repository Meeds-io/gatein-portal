package org.exoplatform.portal.webui.test;


import junit.framework.TestCase;
import org.exoplatform.commons.utils.ExpressionUtil;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageState;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.web.application.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.http.HttpServletRequest;
import java.util.ResourceBundle;

import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.*;


@RunWith(PowerMockRunner.class)
public class PortalRequestContextTest extends TestCase {


    @PrepareForTest({ Util.class, ExpressionUtil.class})
    @Test
    public void testGetTitle() throws Exception {

        PowerMockito.mockStatic(ExpressionUtil.class);
        PowerMockito.mockStatic(Util.class);

        PortalRequestContext prc = mock(PortalRequestContext.class);
        HttpServletRequest request = mock(HttpServletRequest.class, Mockito.CALLS_REAL_METHODS);
        ExoContainer container = mock(ExoContainer.class);
        UserNode userNode = mock(UserNode.class);
        UserPortalConfigService configService = mock(UserPortalConfigService.class);
        UIPortal uiPortal = mock(UIPortal.class);
        PageState state = mock(PageState.class);
        PageContext page = mock(PageContext.class);
        Application app = mock(Application.class);

        ResourceBundle bundle = ResourceBundle.getBundle("test");

        PowerMockito.when(Util.getUIPortal()).thenReturn(uiPortal);
        PowerMockito.when(ExpressionUtil.getExpressionValue(bundle,"title")).thenCallRealMethod();

        doCallRealMethod().when(prc).setPageTitle(anyString());

        when(prc.getTitle()).thenCallRealMethod();

        when(container.getComponentInstanceOfType(UserPortalConfigService.class)).thenReturn(configService);
        when(configService.getPage(Matchers.anyObject())).thenReturn(page);
        when(page.getState()).thenReturn(state);
        when(state.getDisplayName()).thenReturn("title");
        when(userNode.getResolvedLabel()).thenReturn("test");
        when(app.getApplicationServiceContainer()).thenReturn(container);
        when(prc.getApplication()).thenReturn(app);
        when(prc.getApplicationResourceBundle()).thenReturn(bundle);
        when(uiPortal.getSelectedUserNode()).thenReturn(userNode);
        when(prc.getRequest()).thenReturn(request);

        request.setAttribute(PortalRequestContext.REQUEST_TITLE,"title");
        assertEquals("test", prc.getTitle());

        prc.setPageTitle("otherTest");
        assertEquals("otherTest", prc.getTitle());

    }
}
