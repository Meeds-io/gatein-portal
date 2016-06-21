package org.gatein.portal.controller.resource;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import java.util.Locale;


/**
 * Created by exo on 6/14/16.
 * @author : <a href="mailto:mhannechi@exoplatform.com">Mohammed Hannechi</a>
 */
public class ScriptKeyTest extends TestCase {

    final ResourceId id = new ResourceId(ResourceScope.PORTAL, "platformNavigation/UIGroupsNavigationPortlet");
    final boolean minified = false ;
    final Locale locale = null;

    public void testEqualsMethod() {
        ScriptKey scriptKey = new ScriptKey(id, minified, locale);
        ScriptKey scriptKey1 = new ScriptKey(id, false, locale);
        ScriptKey scriptKey2 = new ScriptKey(id, true,locale);

        assertTrue(scriptKey.equals(scriptKey1));
        assertFalse(scriptKey.equals(scriptKey2));

    }
}