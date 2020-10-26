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
package org.exoplatform.portal.resource;

import static org.exoplatform.web.controller.metadata.DescriptorBuilder.*;

import java.net.MalformedURLException;
import java.util.Arrays;

import org.exoplatform.services.resources.Orientation;
import org.exoplatform.web.controller.router.Router;
import org.exoplatform.web.controller.router.RouterConfigException;

/**
 * @author <a href="trongtt@gmail.com">Trong Tran</a>
 * @version $Revision$
 */
public class TestSkinService extends AbstractSkinServiceTest {
    private static boolean isFirstStartup = true;

    boolean isDevelopingMode() {
        return false;
    }

    @Override
    boolean setUpTestEnvironment() {
        return isFirstStartup;
    }

    Router getRouter() {
        Router router;
        try {
            router = router().add(
                    route("/skins/{gtn:version}/{gtn:resource}{gtn:compress}{gtn:orientation}.css")
                            .with(routeParam("gtn:handler").withValue("skin"))
                            .with(pathParam("gtn:version").matchedBy("[^/]*").preservePath())
                            .with(pathParam("gtn:orientation").matchedBy("-(lt)|-(rt)|").captureGroup(true))
                            .with(pathParam("gtn:compress").matchedBy("-(min)|").captureGroup(true))
                            .with(pathParam("gtn:resource").matchedBy(".+?").preservePath())).build();
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
        SkinURL skinURL = skinService.getSkin("mockwebapp/FirstPortlet", "TestSkin").createURL(controllerCtx);
        assertEquals("/portal/skins/" + ASSETS_VERSION + "/mockwebapp/skin/FirstPortlet-min-lt.css", skinURL.toString());
        skinURL.setOrientation(Orientation.RT);
        assertEquals("/portal/skins/" + ASSETS_VERSION + "/mockwebapp/skin/FirstPortlet-min-rt.css", skinURL.toString());
    }

    public void testCompositeSkin() throws NullPointerException, MalformedURLException {
        SkinConfig fSkin = skinService.getSkin("mockwebapp/FirstPortlet", "TestSkin");
        SkinConfig sSkin = skinService.getSkin("mockwebapp/SecondPortlet", "TestSkin");
        assertNotNull(fSkin);
        assertNotNull(sSkin);

        Skin merged = skinService.merge(Arrays.asList(fSkin, sSkin));
        SkinURL url = merged.createURL(controllerCtx);

        url.setOrientation(Orientation.LT);
        assertEquals(".FirstPortlet {foo1 : bar1}\n.SecondPortlet {foo2 : bar2}",
                skinService.getCSS(newControllerContext(getRouter(), url.toString()), true));

        url.setOrientation(Orientation.RT);
        assertEquals(".FirstPortlet {foo1 : bar1}\n.SecondPortlet {foo2 : bar2}",
                skinService.getCSS(newControllerContext(getRouter(), url.toString()), true));
    }

    public void testCache() throws Exception {
        String resource = "/path/to/test/cache.css";
        String url = newSimpleSkin(resource).createURL(controllerCtx).toString();

        resResolver.addResource(resource, "foo");
        assertEquals("foo", skinService.getCSS(newControllerContext(getRouter(), url), true));

        resResolver.addResource(resource, "bar");
        assertEquals("foo", skinService.getCSS(newControllerContext(getRouter(), url), true));
    }

    public void testInvalidateCache() throws Exception {
        String resource = "/path/to/test/invalidate/cache.css";
        String url = newSimpleSkin(resource).createURL(controllerCtx).toString();

        resResolver.addResource(resource, "foo");
        assertEquals("foo", skinService.getCSS(newControllerContext(getRouter(), url), true));

        resResolver.addResource(resource, "bar");
        skinService.invalidateCachedSkin(resource);
        assertEquals("bar", skinService.getCSS(newControllerContext(getRouter(), url), true));
    }

    public void testProcessImportCSS() throws Exception {
        String resource = "/process/import/css.css";
        String url = newSimpleSkin(resource).createURL(controllerCtx).toString();

        resResolver.addResource(resource, "@import url(Portlet/Stylesheet.css); aaa;");
        assertEquals(" aaa;", skinService.getCSS(newControllerContext(getRouter(), url), true));
        skinService.invalidateCachedSkin(resource);

        resResolver.addResource(resource, "@import url('/Portlet/Stylesheet.css'); aaa;");
        assertEquals(" aaa;", skinService.getCSS(newControllerContext(getRouter(), url), true));
        skinService.invalidateCachedSkin(resource);

        // parent file import child css file
        resResolver.addResource(resource, "@import url(childCSS/child.css);  background:url(images/foo.gif);");
        String childResource = "/process/import/childCSS/child.css";
        resResolver.addResource(childResource, "background:url(bar.gif);");

        /*
         * Now test merge and process recursively (run in non-dev mode) We have folder /path/to/parent.css /images/foo.gif
         * /childCSS/child.css /bar.gif
         */
        assertEquals("background:url(/process/import/childCSS/bar.gif);  background:url(/process/import/images/foo.gif);",
                skinService.getCSS(newControllerContext(getRouter(), url), true));

        url = newSimpleSkin(childResource).createURL(controllerCtx).toString();
        assertEquals("background:url(/process/import/childCSS/bar.gif);",
                skinService.getCSS(newControllerContext(getRouter(), url), true));
    }

    public void testProcessImportWithUnicodeCssChar() throws Exception {
        String childResource = "/process/import/childCSS/child.css";
        resResolver.addResource(childResource, "#test:after {content: \"\\00a0 \\003e \\00a0\";}");

        String resource = "/process/import/css.css";
        String url = newSimpleSkin(resource).createURL(controllerCtx).toString();

        resResolver.addResource(resource, "@import url(childCSS/child.css); aaa;");
        String css = skinService.getCSS(newControllerContext(getRouter(), url), true);
        assertEquals("#test:after {content: \"\\00a0 \\003e \\00a0\";} aaa;", css);

        skinService.invalidateCachedSkin(resource);
    }

    public void testLastModifiedSince() throws Exception {
        String resource = "/last/modify/since.css";
        SkinURL skinURL = newSimpleSkin(resource).createURL(controllerCtx);

        resResolver.addResource(resource, "foo");

        assertTrue(skinService.getCSS(newControllerContext(getRouter(), skinURL.toString()), true).length() > 0);
        long lastModified = skinService.getLastModified(newControllerContext(getRouter(), skinURL.toString()));
        Thread.sleep(1000);
        assertEquals(lastModified, skinService.getLastModified(newControllerContext(getRouter(), skinURL.toString())));

        skinURL.setOrientation(Orientation.RT);
        Thread.sleep(1000);
        assertTrue(lastModified < skinService.getLastModified(newControllerContext(getRouter(), skinURL.toString())));
    }
}
