package org.exoplatform.web.controller.router;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.exoplatform.web.WebAppController;
import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.controller.metadata.ControllerDescriptor;
import org.exoplatform.web.controller.metadata.DescriptorBuilder;

/**
 * @author <a href="trongtt@exoplatform.com">Trong Tran</a>
 * @version $Revision$
 */
public class TestRouter extends AbstractTestController {

    public void testMatcher() throws Exception {

        URL routerURL = TestRouter.class.getResource("controller.xml");
        ControllerDescriptor routerDesc = new DescriptorBuilder().build(routerURL.openStream());
        Router router = new Router(routerDesc);

        //
        Iterator<Map<QualifiedName, String>> matcher = router.matcher("/", null);
        assertTrue(matcher.hasNext());
        Map<QualifiedName, String> params = matcher.next();
        Map<QualifiedName, String>  expected = new HashMap<QualifiedName, String>();
        expected.put(WebAppController.HANDLER_PARAM, "default");
        assertEquals(expected, params);

        assertFalse(matcher.hasNext());

        //
        matcher = router.matcher("/non/exist/point", null);

        assertTrue(matcher.hasNext());
        params = matcher.next();
        assertEquals("portal", params.get(WebAppController.HANDLER_PARAM));
        expected = new HashMap<QualifiedName, String>();
        expected.put(WebAppController.HANDLER_PARAM, "portal");
        expected.put(Names.GTN_SITETYPE, "portal");
        expected.put(Names.GTN_LANG, "non");
        expected.put(Names.GTN_SITENAME, "exist");
        expected.put(Names.GTN_PATH, "point");
        assertEquals(expected, params);

        assertFalse(matcher.hasNext());

        //
        matcher = router.matcher("/download", null);

        assertTrue(matcher.hasNext());
        params = matcher.next();
        assertEquals("download", params.get(WebAppController.HANDLER_PARAM));
        expected = new HashMap<QualifiedName, String>();
        expected.put(WebAppController.HANDLER_PARAM, "download");
        assertEquals(expected, params);

        assertTrue(matcher.hasNext());
        params = matcher.next();
        assertEquals("portal", params.get(WebAppController.HANDLER_PARAM));
        expected = new HashMap<QualifiedName, String>();
        expected.put(WebAppController.HANDLER_PARAM, "portal");
        expected.put(Names.GTN_SITETYPE, "portal");
        expected.put(Names.GTN_LANG, "");
        expected.put(Names.GTN_SITENAME, "download");
        expected.put(Names.GTN_PATH, "");
        assertEquals(expected, params);

        assertFalse(matcher.hasNext());

        //
        matcher = router.matcher("/en/classic/home", null);

        assertTrue(matcher.hasNext());
        params = matcher.next();
        expected = new HashMap<QualifiedName, String>();
        expected.put(WebAppController.HANDLER_PARAM, "portal");
        expected.put(Names.GTN_SITETYPE, "portal");
        expected.put(Names.GTN_LANG, "en");
        expected.put(Names.GTN_SITENAME, "classic");
        expected.put(Names.GTN_PATH, "home");
        assertEquals(expected, params);

        assertFalse(matcher.hasNext());

        //
        matcher = router.matcher("/g/classic/register",
                                 new HashMap<String, String[]>(Collections.singletonMap("lang", new String[] { "fr" })));
        assertTrue(matcher.hasNext());
        params = matcher.next();
        expected = new HashMap<QualifiedName, String>();
        expected.put(WebAppController.HANDLER_PARAM, "portal");
        expected.put(Names.GTN_SITETYPE, "group");
        expected.put(Names.GTN_LANG, "fr");
        expected.put(Names.GTN_SITENAME, "classic");
        expected.put(Names.GTN_PATH, "register");
        assertEquals(expected, params);

        assertTrue(matcher.hasNext());
        params = matcher.next();
        expected = new HashMap<QualifiedName, String>();
        expected.put(WebAppController.HANDLER_PARAM, "portal");
        expected.put(Names.GTN_SITETYPE, "portal");
        expected.put(Names.GTN_LANG, "");
        expected.put(Names.GTN_SITENAME, "g");
        expected.put(Names.GTN_PATH, "classic/register");
        assertEquals(expected, params);

        assertFalse(matcher.hasNext());
    }
}
