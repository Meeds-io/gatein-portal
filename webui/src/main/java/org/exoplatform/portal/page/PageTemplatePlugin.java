package org.exoplatform.portal.page;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.webui.core.model.SelectItemCategory;

/**
 * Page Template plugin to inject in {@link PageTemplateService}
 */
public class PageTemplatePlugin extends BaseComponentPlugin {

  private SelectItemCategory<String> category;

  @SuppressWarnings("unchecked")
  public PageTemplatePlugin(InitParams params) {
    if (params != null && params.containsKey("category")) {
      this.category = (SelectItemCategory<String>) params.getObjectParam("category").getObject();
    }
  }

  public SelectItemCategory<String> getCategory() {
    return category;
  }

  public void setCategory(SelectItemCategory<String> category) {
    this.category = category;
  }
}
