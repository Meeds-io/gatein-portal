package org.apache.struts.beanaction.httpmap;

import java.util.Enumeration;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;


/**
 * Map to wrap cookie names and values (READ ONLY).
 * <br>
 * <br>
 * <br>
 * Date: Mar 11, 2004 11:31:35 PM
 *
 * @author Clinton Begin
 */
public class CookieMap extends BaseHttpMap {
    private Cookie[] cookies;

    public CookieMap(HttpServletRequest request) {
        cookies = request.getCookies();
    }

    protected Enumeration getNames() {
        return new CookieEnumerator(cookies);
    }

    protected Object getValue(Object key) {
        for (int i = 0; i < cookies.length; i++) {
            if (key.equals(cookies[i].getName())) {
                return cookies[i].getValue();
            }
        }
        return null;
    }

    protected void putValue(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    protected void removeValue(Object key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Cookie Enumerator Class
     */
    private class CookieEnumerator implements Enumeration {
        private int i = 0;
        private Cookie[] cookieArray;

        public CookieEnumerator(Cookie[] cookies) {
            this.cookieArray = cookies;
        }

        public boolean hasMoreElements() {
            return cookieArray.length > i;
        }

        public Object nextElement() {
            Object element = cookieArray[i];
            i++;
            return element;
        }
    }
}
