package org.apache.struts.beanaction;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.ValidatorActionForm;

/**
 * All actions mapped through the BeanAction class should be mapped
 * <br>
 * to a subclass of BaseBean (or have no form bean mapping at all).
 * <br>
 * <br>
 * <br>
 * The BaseBean class simplifies the validate() and reset() methods
 * <br>
 * by allowing them to be managed without Struts dependencies. Quite
 * <br>
 * simply, subclasses can override the parameterless validate()
 * <br>
 * and reset() methods and set errors and messages using the ActionContext
 * <br>
 * class.
 * <br>
 * <br>
 * <br>
 * <i>Note: Full error, message and internationalization support is not complete.</i>
 * <br>
 * <br>
 * <br>
 * Date: Mar 12, 2004 9:20:39 PM
 *
 * @author Clinton Begin
 */
public abstract class BaseBean extends ValidatorActionForm {

    private ActionInterceptor interceptor;

    protected BaseBean() {
        this.interceptor = new DefaultActionInterceptor();
    }

    protected BaseBean(ActionInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    public final void reset(ActionMapping mapping, ServletRequest request) {
        ActionContext.initCurrentContext((HttpServletRequest) request, null);
        reset();
    }

    public final void reset(ActionMapping mapping, HttpServletRequest request) {
        ActionContext.initCurrentContext((HttpServletRequest) request, null);
        reset();
    }

    public void reset() {
    }

    public ActionInterceptor getInterceptor() {
        return interceptor;
    }
}
