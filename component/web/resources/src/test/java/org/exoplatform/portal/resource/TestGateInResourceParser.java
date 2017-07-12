package org.exoplatform.portal.resource;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import junit.framework.TestCase;

import org.exoplatform.commons.xml.DocumentSource;
import org.exoplatform.portal.resource.config.tasks.SkinConfigTask;
import org.exoplatform.portal.resource.config.xml.SkinConfigParser;

public class TestGateInResourceParser extends TestCase {
    public void testResources1_0() throws MalformedURLException {
        assertDescriptorCanBeLoaded("org/exoplatform/portal/resource/gatein-resources-1_0.xml");
    }

    public void testResources1_0WithSkinModule() throws MalformedURLException {
        assertDescriptorCanBeLoaded("org/exoplatform/portal/resource/gatein-resources-1_0-with-skin-module.xml");
    }

    public void testResources1_1() throws MalformedURLException {
        assertDescriptorCanBeLoaded("org/exoplatform/portal/resource/gatein-resources-1_1.xml");
    }

    public void testResources1_2() throws MalformedURLException {
        assertDescriptorCanBeLoaded("org/exoplatform/portal/resource/gatein-resources-1_2.xml");
    }

    public void testResources1_3() throws MalformedURLException {
        assertDescriptorCanBeLoaded("org/exoplatform/portal/resource/gatein-resources-1_3.xml");
    }

    public void testResources1_4() throws MalformedURLException {
        assertDescriptorCanBeLoaded("org/exoplatform/portal/resource/gatein-resources-1_4.xml");
    }

    private void assertDescriptorCanBeLoaded(String descriptorPath) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(descriptorPath);
        assertNotNull("The " + descriptorPath + " can not be found", url);
        DocumentSource source = DocumentSource.create(url);
        List<SkinConfigTask> tasks = SkinConfigParser.fetchTasks(source);
        assertNotNull("There are no tasks", tasks);
        assertEquals(8, tasks.size());
    }
}
