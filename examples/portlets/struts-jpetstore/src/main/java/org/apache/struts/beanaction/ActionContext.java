package org.apache.struts.beanaction;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.beanaction.httpmap.ApplicationMap;
import org.apache.struts.beanaction.httpmap.CookieMap;
import org.apache.struts.beanaction.httpmap.ParameterMap;
import org.apache.struts.beanaction.httpmap.RequestMap;
import org.apache.struts.beanaction.httpmap.SessionMap;

/**
 * The ActionContext class gives simplified, thread-safe access to
 * <br>
 * the request and response, as well as form parameters, request
 * <br>
 * attributes, session attributes, application attributes. Much
 * <br>
 * of this can be accopmplished without using the Struts or even
 * <br>
 * the Servlet API, therefore isolating your application from
 * <br>
 * presentation framework details.
 * <br>
 * <br>
 * <br>
 * This class also provides facilities for simpler message and error
 * <br>
 * message handling. Although not as powerful as that provided by
 * <br>
 * Struts, it is great for simple applications that don't require
 * <br>
 * internationalization or the flexibility of resource bundles.
 * <br>
 * <br>
 * <br>
 * <i>Note: A more complete error and message handling API will be implemented.</i>
 * <br>
 * <br>
 * <br>
 * Date: Mar 9, 2004 9:57:39 PM
 *
 * @author Clinton Begin
 */
public class ActionContext {
    private static final ThreadLocal localContext = new ThreadLocal();
    private HttpServletRequest request;
    private HttpServletResponse response;
    private Map cookieMap;
    private Map parameterMap;
    private Map requestMap;
    private Map sessionMap;
    private Map applicationMap;

    public ActionContext() {
        cookieMap = new HashMap();
        parameterMap = new HashMap();
        requestMap = new HashMap();
        sessionMap = new HashMap();
        applicationMap = new HashMap();
    }

    static void initCurrentContext(HttpServletRequest request, HttpServletResponse response) {
        ActionContext ctx = getActionContext();
        ctx.request = request;
        ctx.response = response;
        ctx.cookieMap = null;
        ctx.parameterMap = null;
        ctx.requestMap = null;
        ctx.sessionMap = null;
        ctx.applicationMap = null;
    }

    public Map getCookieMap() {
        if (cookieMap == null) {
            cookieMap = new CookieMap(request);
        }
        return cookieMap;
    }

    public Map getParameterMap() {
        if (parameterMap == null) {
            parameterMap = new ParameterMap(request);
        }
        return parameterMap;
    }

    public Map getRequestMap() {
        if (requestMap == null) {
            requestMap = new RequestMap(request);
        }
        return requestMap;
    }

    public Map getSessionMap() {
        if (sessionMap == null) {
            sessionMap = new SessionMap(request);
        }
        return sessionMap;
    }

    public Map getApplicationMap() {
        if (applicationMap == null) {
            applicationMap = new ApplicationMap(request);
        }
        return applicationMap;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public static ActionContext getActionContext() {
        ActionContext ctx = (ActionContext) localContext.get();
        if (ctx == null) {
            ctx = new ActionContext();
            localContext.set(ctx);
        }
        return ctx;
    }
}
