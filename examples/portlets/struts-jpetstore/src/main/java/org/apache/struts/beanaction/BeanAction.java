/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.apache.struts.beanaction;

import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * BeanAction is an extension to the typical Struts Action class that
 * <br>
 * enables mappings to bean methods. This allows for a more typical
 * <br>
 * Object Oriented design where each object has behaviour as part of
 * <br>
 * its definition. Instead of writing separate Actions and Forms,
 * <br>
 * BeanAction allows you to simply have a Bean, which models both
 * <br>
 * the state and the methods that operate on that state.
 * <br>
 * <br>
 * <br>
 * In addition to the simpler packaging, BeanAction also simplifies the
 * <br>
 * Struts progamming paradigm and reduces dependency on Struts. Using
 * <br>
 * this pattern could allow easier migration to newer frameworks like JSF.
 * <br>
 * <br>
 * <br>
 * The method signatures are greatly simplified to the following
 * <br>
 *
 * <pre>
 * <br>
 * public String myActionMethod() {
 * <br>
 *   //..work
 * <br>
 *   return "success";
 * <br>
 * }
 * <br>
 * </pre>
 * <br>
 * The return parameter becomes simply the name of the forward (as defined
 * <br>
 * in the config file as usual). Form parameters, request, response, session,
 * <br>
 * attributes, and cookies are all accessed via the ActionContext class (see the
 * <br>
 * ActionContext javadocs for more).
 * <br>
 * <br>
 * <br>
 * The forms that you map to a BaseAction mapping must be a subclass of the
 * <br>
 * BaseBean class. BaseBean continues to simplify the validation and
 * <br>
 * reset methods by removing the parameters from the signature as was done with
 * <br>
 * the above action method example.
 * <br>
 * <br>
 * <br>
 * There are 3 ways to map a BeanAction in the struts configuration file.
 * <br>
 * They are as follows.
 * <br>
 * <br>
 * <br>
 * <B>URL Pattern</B>
 * <br>
 * <br>
 * <br>
 * This approach uses the end of the action definition to determine which
 * <br>
 * method to call on the Bean. For example if you request the URL:
 * <br>
 * <br>
 * <br>
 * http://localhost/jpetstore4/shop/viewOrder.do
 * <br>
 * <br>
 * <br>
 * Then the method called would be "viewOrder" (of the mapped bean as specified
 * <br>
 * by the name="" parameter in the mapping below). The mapping used for this
 * <br>
 * approach is as follows.
 * <br>
 *
 * <pre>
 * <br>
 *  &lt;action path="/shop/<b>viewOrder</b>" type="org.apache.struts.beanaction.BeanAction"
 * <br>
 *    name="orderBean" scope="session"
 * <br>
 *    validate="false"&gt;
 * <br>
 *    &lt;forward name="success" path="/order/ViewOrder.jsp"/&gt;
 * <br>
 *  &lt;/action&gt;
 * <br>
 * </pre>
 * <br>
 * <br>
 * <br>
 * <B>Method Parameter</B>
 * <br>
 * <br>
 * <br>
 * This approach uses the Struts action parameter within the mapping
 * <br>
 * to determine the method to call on the Bean. For example the
 * <br>
 * following action mapping would cause the "viewOrder" method to
 * <br>
 * be called on the bean ("orderBean"). The mapping used for this
 * <br>
 * approach is as follows.
 * <br>
 *
 * <pre>
 * <br>
 *  &lt;action path="/shop/viewOrder" type="org.apache.struts.beanaction.BeanAction"
 * <br>
 *    <b>name="orderBean" parameter="viewOrder"</b> scope="session"
 * <br>
 *    validate="false"&gt;
 * <br>
 *    &lt;forward name="success" path="/order/ViewOrder.jsp"/&gt;
 * <br>
 *  &lt;/action&gt;
 * <br>
 * </pre>
 * <br>
 * <B>No Method call</B>
 * <br>
 * <br>
 * <br>
 * BeanAction will ignore any Struts action mappings without beans associated
 * <br>
 * to them (i.e. no name="" attribute in the mapping). If you do want to associate
 * <br>
 * a bean to the action mapping, but do not want a method to be called, simply
 * <br>
 * set the parameter to an asterisk ("*"). The mapping used for this approach
 * <br>
 * is as follows (no method will be called).
 * <br>
 *
 * <pre>
 * <br>
 *  &lt;action path="/shop/viewOrder" type="org.apache.struts.beanaction.BeanAction"
 * <br>
 *    <b>name="orderBean" parameter="*"</b> scope="session"
 * <br>
 *    validate="false"&gt;
 * <br>
 *    &lt;forward name="success" path="/order/ViewOrder.jsp"/&gt;
 * <br>
 *  &lt;/action&gt;
 * <br>
 * </pre>
 * <br>
 * <br>
 * <br>
 * <br>
 * <br>
 * TO-DO List
 * <br>
 * <ul>
 * <li>Ignore mappings to methods that don't exist.
 * </ul>
 * <br>
 * <br>
 * <B>A WORK IN PROGRESS</B>
 * <br>
 * <br>
 * <br>
 * <i>The BeanAction Struts extension is a work in progress. While it demonstrates
 * <br>
 * good patterns for application development, the framework itself is very new and
 * <br>
 * should not be considered stable. Your comments and suggestions are welcome.
 * <br>
 * Please visit <a href="http://www.ibatis.com">http://www.ibatis.com</a> for contact information.</i>
 * <br>
 * <br>
 * <br>
 * Date: Mar 11, 2004 10:03:56 PM
 *
 * @author Clinton Begin
 * @see BaseBean
 * @see org.apache.struts.beanaction.ActionContext
 */
public class BeanAction extends Action {
    private static final String NO_METHOD_CALL = "*";
    private static final String SUCCESS_FORWARD = "success";

    public final ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        String forward = SUCCESS_FORWARD;
        try {
            if (!(form instanceof BaseBean)) {
                if (form != null) {
                    throw new BeanActionException("The form for mapping '" + mapping.getPath() + "' named '"
                            + mapping.getName()
                            + "' was not an instance of BaseBean.  BeanAction requires an BaseBean instance.");
                } else {
                    throw new BeanActionException("The form for mapping '" + mapping.getPath() + "' named '"
                            + mapping.getName() + "' was null.  BeanAction requires an BaseBean instance.");
                }
            }
            BaseBean bean = (BaseBean) form;
            ActionContext.initCurrentContext(request, response);
            if (bean != null) {
                // Explicit Method Mapping

                Method method = null;
                String methodName = mapping.getParameter();
                if (methodName != null && !NO_METHOD_CALL.equals(methodName)) {
                    try {
                        method = bean.getClass().getMethod(methodName, (Class[]) null);
                        synchronized (bean) {
                            forward = bean.getInterceptor().intercept(new ActionInvoker(bean, method));
                        }
                    } catch (Exception e) {
                        throw new BeanActionException("Error dispatching bean action via method parameter ('" + methodName
                                + "').  Cause: " + e, e);
                    }
                }

                // Path Based Method Mapping

                if (method == null && !NO_METHOD_CALL.equals(methodName)) {
                    methodName = mapping.getPath();
                    if (methodName.length() > 1) {
                        int slash = methodName.lastIndexOf("/") + 1;
                        methodName = methodName.substring(slash);
                        if (methodName.length() > 0) {
                            try {
                                method = bean.getClass().getMethod(methodName, (Class[]) null);
                                synchronized (bean) {
                                    forward = bean.getInterceptor().intercept(new ActionInvoker(bean, method));
                                }
                            } catch (Exception e) {
                                throw new BeanActionException("Error dispatching bean action via URL pattern ('" + methodName
                                        + "').  Cause: " + e, e);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            forward = "error";
            request.setAttribute("BeanActionException", e);
        }
        return mapping.findForward(forward);
    }
}
