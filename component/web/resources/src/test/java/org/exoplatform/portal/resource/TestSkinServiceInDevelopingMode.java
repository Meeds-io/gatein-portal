/*
 * Copyright (C) 2011 eXo Platform SAS.
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
package org.exoplatform.portal.resource;

import static org.exoplatform.web.controller.metadata.DescriptorBuilder.*;

import java.io.IOException;

import org.exoplatform.services.resources.Orientation;
import org.exoplatform.web.controller.router.Router;
import org.exoplatform.web.controller.router.RouterConfigException;

/**
 * @author <a href="trongtt@gmail.com">Trong Tran</a>
 * @version $Revision$
 */

public class TestSkinServiceInDevelopingMode extends AbstractSkinServiceTest {
  private static boolean isFirstStartup = true;

  boolean isDevelopingMode() {
    return true;
  }

  @Override
  boolean setUpTestEnvironment() {
    return isFirstStartup;
  }

  @Override
  Router getRouter() {
    Router router;
    try {
      router = router().add(
                            route("/skins/{gtn:version}/{gtn:resource}{gtn:compress}{gtn:orientation}.css")
                                                                                                           .with(routeParam("gtn:handler").withValue("skin"))
                                                                                                           .with(pathParam("gtn:version").matchedBy("[^/]*")
                                                                                                                                         .preservePath())
                                                                                                           .with(pathParam("gtn:orientation").matchedBy("-(lt)|-(rt)|")
                                                                                                                                             .captureGroup(true))
                                                                                                           .with(pathParam("gtn:compress").matchedBy("-(min)|")
                                                                                                                                          .captureGroup(true))
                                                                                                           .with(pathParam("gtn:resource").matchedBy(".+?")
                                                                                                                                          .preservePath()))
                       .build();
      return router;
    } catch (RouterConfigException e) {
      return null;
    }
  }

  @Override
  void touchSetUp() {
    isFirstStartup = false;
  }

  public void testRenderURL() {
    SkinURL skinURL = skinService.getSkin("mockwebapp/FirstPortlet", "TestSkin").createURL();
    assertEquals("/mockwebapp/skin/FirstPortlet.css?orientation=LT&minify=false&hash=0", skinURL.toString());
    skinURL.setOrientation(Orientation.RT);
    assertEquals("/mockwebapp/skin/FirstPortlet.css?orientation=RT&minify=false&hash=0", skinURL.toString());
  }

  public void testGetSkinModuleFileContent() throws IOException {
    assertEquals(".FirstPortlet {foo1 : bar1}", skinService.getSkinModuleFileContent("/mockwebapp/skin/FirstPortlet.css"));
  }

}
