/**
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.portal.webui.page;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import org.exoplatform.webui.application.WebuiApplication;
import org.exoplatform.webui.application.WebuiRequestContext;

/**
 * An UIPage abstract factory
 *
 * @author <a href="trongtt@gmail.com">Trong Tran</a>
 * @version $Revision$
 */
public class UIPageFactory {

  public static final String                  DEFAULT_FACTORY_ID = "Default";

  protected static Map<String, UIPageFactory> pageFactory        = new HashMap<>();

  static {
    ServiceLoader<UIPageFactory> loader = ServiceLoader.load(UIPageFactory.class);
    for (UIPageFactory factory : loader) {
      pageFactory.put(factory.getType(), factory);
    }
  }

  public static UIPageFactory getInstance(String type) {
    UIPageFactory factory = pageFactory.get(type);
    if (type == null) {
      factory = pageFactory.get(DEFAULT_FACTORY_ID);
    }
    return factory;
  }

  public String getType() {
    return DEFAULT_FACTORY_ID;
  }

  public UIPage createUIPage(WebuiRequestContext context) throws Exception {
    if (context == null) {
      context = WebuiRequestContext.getCurrentInstance();
    }
    WebuiApplication app = (WebuiApplication) context.getApplication();
    UIPage uiPage = app.createUIComponent(UIPage.class, null, null, context);
    return uiPage;
  }

}
