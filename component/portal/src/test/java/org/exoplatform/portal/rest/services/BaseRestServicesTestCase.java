package org.exoplatform.portal.rest.services;

import java.lang.reflect.*;
import java.security.Principal;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.component.test.*;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.services.rest.impl.*;
import org.exoplatform.services.rest.tools.ResourceLauncher;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.test.mock.MockPrincipal;

@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/portal/config/conf/configuration.xml") })
public abstract class BaseRestServicesTestCase extends AbstractKernelTest {

  protected ProviderBinder   providers;

  protected ResourceBinder   binder;

  protected ResourceLauncher launcher;

  protected ExoContainer     container;

  public void setUp() throws Exception {
    begin();
    ExoContainer container = getContainer();
    this.container = container;

    binder = (ResourceBinder) container.getComponentInstanceOfType(ResourceBinder.class);
    RequestHandlerImpl requestHandler = (RequestHandlerImpl) container.getComponentInstanceOfType(RequestHandlerImpl.class);

    // reset default providers to be sure it is clean.
    ProviderBinder.setInstance(new ProviderBinder());
    providers = ProviderBinder.getInstance();

    binder.clear();

    ApplicationContextImpl.setCurrent(new ApplicationContextImpl(null, null, providers, null));

    launcher = new ResourceLauncher(requestHandler);
    registry(getComponentClass());
  }

  public void tearDown() throws Exception {
    unregistry(getComponentClass());
    end();
  }

  protected abstract Class<?> getComponentClass();

  protected <T> T getService(Class<T> clazz) {
    return clazz.cast(this.container.getComponentInstanceOfType(clazz));
  }

  protected void registry(Class<?> resourceClass) throws Exception {
    binder.addResource(resourceClass, null);
  }

  protected void unregistry(Class<?> resourceClass) {
    binder.removeResource(resourceClass);
  }

  protected ContainerResponse getResponse(String method, String restPath, String input) throws Exception {
    byte[] jsonData = input.getBytes("UTF-8");
    MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
    headers.putSingle("content-type", "application/json");
    headers.putSingle("content-length", "" + jsonData.length);
    return launcher.service(method, restPath, "", headers, jsonData, null);
  }

  protected <T> T createProxy(Class<T> type, final Map<String, Object> result) {
    Object o = Proxy.newProxyInstance(getClass().getClassLoader(),
                                      new Class<?>[] { type },
                                      new InvocationHandler() {
                                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                                          if (method.getName().equals("equals")) {
                                            return proxy == args[0];
                                          }
                                          Object o = result.get(method.getName());
                                          return o instanceof Invoker ? ((Invoker) o).invoke(args) : o;
                                        }
                                      });
    return type.cast(o);
  }

  protected void startUserSession(String username) {
    Identity identity = new Identity(username);
    ConversationState state = new ConversationState(identity);
    ConversationState.setCurrent(state);
  }

  protected static interface Invoker {
    Object invoke(Object[] args);
  }

  protected static class MockSecurityContext implements SecurityContext {

    private final String username;

    public MockSecurityContext(String username) {
      this.username = username;
    }

    public Principal getUserPrincipal() {
      return new MockPrincipal(username);
    }

    public boolean isUserInRole(String role) {
      return false;
    }

    public boolean isSecure() {
      return false;
    }

    public String getAuthenticationScheme() {
      return null;
    }
  }

  protected static class MockListAccess<T> implements ListAccess<T> {
    private final T[] values;

    public MockListAccess(T[] values) {
      this.values = values;
    }

    public T[] load(int index, int length) throws Exception, IllegalArgumentException {
      return values;
    }

    public int getSize() throws Exception {
      return values.length;
    }
  }
}
