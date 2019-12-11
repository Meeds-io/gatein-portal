package org.exoplatform.portal.module;

import java.util.*;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;

public class ModulePlugin extends BaseComponentPlugin {

  private Map<String, Module> modulesByName = new HashMap<>();

  public ModulePlugin(InitParams initParams) {
    Iterator<Module> iterator = initParams.getObjectParamValues(Module.class).iterator();
    while (iterator.hasNext()) {
      Module module = iterator.next();
      modulesByName.put(module.getName(), module);
    }
  }

  public Map<String, Module> getModulesByName() {
    return modulesByName;
  }
}
