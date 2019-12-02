package org.exoplatform.groovyscript.text;

import java.util.List;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;

/**
 * Component plugin of Template extension. Component name is identified as the
 * parent Template name to extend.
 */
public class TemplateExtensionPlugin extends BaseComponentPlugin {

  /**
   * List of templates to include to parent gtmpl
   */
  private List<String> templates = null;

  public TemplateExtensionPlugin(InitParams params) {
    templates = params.getValuesParam("templates").getValues();
  }

  public List<String> getTemplates() {
    return templates;
  }

  public void setTemplates(List<String> templates) {
    this.templates = templates;
  }

}
