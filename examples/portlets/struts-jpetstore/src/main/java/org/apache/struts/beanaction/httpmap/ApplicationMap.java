package org.apache.struts.beanaction.httpmap;

import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;


/**
 * Map to wrap application scope attributes.
 * <br>
 * <br>
 * <br>
 * Date: Mar 11, 2004 11:21:25 PM
 *
 * @author Clinton Begin
 */
public class ApplicationMap extends BaseHttpMap {
    private ServletContext context;

    public ApplicationMap(HttpServletRequest request) {
        context = request.getSession().getServletContext();
    }

    protected Enumeration getNames() {
        return context.getAttributeNames();
    }

    protected Object getValue(Object key) {
        return context.getAttribute(String.valueOf(key));
    }

    protected void putValue(Object key, Object value) {
        context.setAttribute(String.valueOf(key), value);
    }

    protected void removeValue(Object key) {
        context.removeAttribute(String.valueOf(key));
    }
}
