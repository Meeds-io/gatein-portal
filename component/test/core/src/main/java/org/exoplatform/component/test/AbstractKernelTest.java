/*
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

package org.exoplatform.component.test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.container.component.RequestLifeCycle;

import junit.framework.TestSuite;

/**
 * An abstract test that takes care of running the unit tests with the semantic described by the {#link GateInTestClassLoader}.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
})
public abstract class AbstractKernelTest extends AbstractGateInTest {

    /** . */
    private static KernelBootstrap bootstrap;

    private boolean forceContainerReload = false;

    /**
     * This is made static because the Class attributes are initialized each
     * method run, since the class is instantiated each Test method run
     */
    private static final Map<String, AtomicLong> COUNTERS = new HashMap<String, AtomicLong>();

    static {
      if (System.getProperty("gatein.email.domain.url") == null) {
        System.setProperty("gatein.email.domain.url", "http://localhost:8080");
      }
      if (System.getProperty("exo.files.storage.dir") == null) {
        System.setProperty("exo.files.storage.dir", "target/files");
      }
      if (System.getProperty("com.arjuna.ats.arjuna.objectstore.objectStoreDir") == null) {
        System.setProperty("com.arjuna.ats.arjuna.objectstore.objectStoreDir", "target/com.arjuna.ats.arjuna.objectstore.objectStoreDir");
      }
    }

    /** . */
    protected AbstractKernelTest() {
        super();
    }

    protected AbstractKernelTest(String name) {
        super(name);
    }

    public PortalContainer getContainer() {
        return bootstrap == null ? bootContainer() : bootstrap.getContainer();
    }

    public void setForceContainerReload(boolean forceContainerReload) {
      this.forceContainerReload = forceContainerReload;
    }

    public boolean isForceContainerReload() {
      return forceContainerReload;
    }

    protected void begin() {
        PortalContainer container = getContainer();
        ExoContainerContext.setCurrentContainer(container);
        RequestLifeCycle.begin(container);
    }

    protected void end() {
        RequestLifeCycle.end();
    }

    protected void restartTransaction() {
      int i = 0;
      // Close transactions until no encapsulated transaction
      boolean success = true;
      do {
        try {
          end();
          i++;
        } catch (IllegalStateException e) {
          success = false;
        }
      } while (success);

      // Restart transactions with the same number of encapsulations
      for (int j = 0; j < i; j++) {
        begin();
      }
    }

    @Override
    protected void beforeRunBare() {
      String className = getClass().getName();
      if(!COUNTERS.containsKey(className)) {
        AtomicLong classTestCount = new AtomicLong(new TestSuite(getClass()).testCount());
        COUNTERS.put(getClass().getName(), classTestCount);
      }
      beforeClass();
      super.beforeRunBare();
    }

    @Override
    protected void afterRunBare() {
      String className = getClass().getName();
      if(COUNTERS.containsKey(className) && COUNTERS.get(className).decrementAndGet() == 0) {
        COUNTERS.remove(className);
        super.afterRunBare();
        afterClass();
      }
    }

    protected void beforeClass() {
        //
        if (isPortalContainerPresent()) {
          if (isForceContainerReload()) {
            log.warn("PortalContainer seems to not be properly stopped by previous tests. PortalContainer will be forced to restart.");
            if (bootstrap == null) {
              forceStop();
            } else {
              bootstrap.dispose();
            }
            bootContainer();
          }
        } else {
            bootContainer();
        }
    }

    private void forceStop() {
      RootContainer.getInstance().stop();
      ExoContainerContext.setCurrentContainer(null);
    }

    protected void afterClass() {
        //
        if (bootstrap != null) {
            bootstrap.dispose();

            //
            bootstrap = null;
        } else if(isPortalContainerPresent() && isForceContainerReload()) {
            log.info("PortalContainer will be stopped in class " + getClass().getName() + ".");
            forceStop();
        }
    }

    private PortalContainer bootContainer() {
      //
      bootstrap = new KernelBootstrap(Thread.currentThread().getContextClassLoader());

      // Configure ourselves
      bootstrap.addConfiguration(getClass());
      //
      bootstrap.boot();

      //
      PortalContainer portalContainer = bootstrap.getContainer();
      ExoContainerContext.setCurrentContainer(portalContainer);
      return portalContainer;
    }

    private boolean isPortalContainerPresent() {
      return ExoContainerContext.getCurrentContainerIfPresent() != null && PortalContainer.getInstanceIfPresent() != null;
    }

}
