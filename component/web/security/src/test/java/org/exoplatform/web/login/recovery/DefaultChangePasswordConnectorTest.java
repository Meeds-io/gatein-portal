package org.exoplatform.web.login.recovery;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.idm.PicketLinkIDMService;
import org.exoplatform.services.organization.idm.UserDAOImpl;
import org.exoplatform.services.organization.idm.UserImpl;
import org.exoplatform.services.organization.idm.externalstore.PicketLinkIDMExternalStoreService;
import org.exoplatform.web.security.security.CookieTokenService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;

@RunWith(MockitoJUnitRunner.class)
public class DefaultChangePasswordConnectorTest {
  
  @Mock
  OrganizationService               organizationService;

  @Mock
  CookieTokenService                cookieTokenService;
  @Mock
  PicketLinkIDMService picketLinkIDMService;
  @Mock
  PicketLinkIDMExternalStoreService picketLinkIDMExternalStoreService;
  
  @Test
  public void testChangePasswordFromInternalStore() throws Exception {
    User userTest = new UserImpl("utest");
    userTest.setFirstName("User");
    userTest.setLastName("Test");
    userTest.setEmail("user.test@acme.com");
    userTest.setPassword("P@ssword123");
    userTest.setOriginatingStore(OrganizationService.INTERNAL_STORE);
    
    UserDAOImpl userHandler = Mockito.mock(UserDAOImpl.class);
    Mockito.when(userHandler.findUserByName(userTest.getUserName())).thenReturn(userTest);
    Mockito.when(organizationService.getUserHandler()).thenReturn(userHandler);
    InitParams initParams = new InitParams();

    Mockito.doNothing().when(cookieTokenService).deleteTokensByUsernameAndType(any(), any());
  
    DefaultChangePasswordConnector defaultChangePasswordConnector =
                                                                  new DefaultChangePasswordConnector(initParams,
                                                                                                     organizationService,
                                                                                                     cookieTokenService);
    
    String newPassword="newPassword";
    defaultChangePasswordConnector.changePassword(userTest.getUserName(),newPassword);
  
    ArgumentCaptor<User> argument = ArgumentCaptor.forClass(User.class);
    Mockito.verify(userHandler).saveUser(argument.capture(),Mockito.anyBoolean());
    Assert.assertEquals(newPassword, argument.getValue().getPassword());
  
    Mockito.verify(userHandler, Mockito.times(1)).saveUser(any(), Mockito.anyBoolean());
    Mockito.verify(cookieTokenService, Mockito.times(1)).deleteTokensByUsernameAndType("utest", "");

  }
  
  @Test
  public void testChangePasswordFromExternalStoreAndNotAllowed() throws Exception {
    User userTest = new UserImpl("utest");
    userTest.setFirstName("User");
    userTest.setLastName("Test");
    userTest.setEmail("user.test@acme.com");
    userTest.setPassword("P@ssword123");
    userTest.setOriginatingStore(OrganizationService.EXTERNAL_STORE);
    
    UserDAOImpl userHandler = Mockito.mock(UserDAOImpl.class);
    Mockito.when(userHandler.findUserByName(userTest.getUserName())).thenReturn(userTest);
    Mockito.when(organizationService.getUserHandler()).thenReturn(userHandler);

    InitParams initParams = new InitParams();
    ValueParam valueParam = new ValueParam();
    valueParam.setName("allowChangeExternalPassword");
    valueParam.setValue("false");
    initParams.addParameter(valueParam);
  
    DefaultChangePasswordConnector defaultChangePasswordConnector = new DefaultChangePasswordConnector(initParams,
                                                                                                       organizationService,
                                                                                                       cookieTokenService);
    String newPassword="newPassword";
    try {
      defaultChangePasswordConnector.changePassword(userTest.getUserName(), newPassword);
    } catch (Exception e) {
    
    }
  
    //if user is from external store and change password is not allowed for external store
    // we should not save his password
    Mockito.verify(userHandler, Mockito.times(0)).saveUser(any(), Mockito.anyBoolean());
    Mockito.verify(cookieTokenService, Mockito.times(0)).deleteTokensByUsernameAndType("utest", "");
  }
}
