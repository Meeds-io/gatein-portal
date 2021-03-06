package org.exoplatform.portal.page;

import java.util.*;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.webui.core.model.SelectItemCategory;

/**
 * A service to manage the list of Page templates to use in Page creator Wizard
 */
public class PageTemplateService {

  private List<SelectItemCategory<String>> categories = new ArrayList<>();

  /**
   * Add a new page templates using a plugin injected by Kernel configuration
   * 
   * @param pageTemplatePlugin {@link PageTemplatePlugin} containing new or
   *          existing template category with the list of templates
   */
  public void addPageTemplate(PageTemplatePlugin pageTemplatePlugin) {
    if (pageTemplatePlugin == null) {
      throw new IllegalArgumentException("Plugin is empty");
    }
    if (pageTemplatePlugin.getCategory() == null) {
      return;
    }
    SelectItemCategory<String> category = pageTemplatePlugin.getCategory();
    SelectItemCategory<String> foundCategory =
                                             categories.stream()
                                                       .filter(existingCategory -> StringUtils.equals(existingCategory.getName(),
                                                                                                      category.getName()))
                                                       .findAny()
                                                       .orElse(null);

    if (foundCategory == null) {
      categories.add(category);
      if (category.getSelectItemOptions() == null) {
        category.setSelectItemOptions(new ArrayList<>());
      }
    } else {
      foundCategory.getSelectItemOptions().addAll(category.getSelectItemOptions());
    }
  }

  /**
   * @return {@link List} of {@link SelectItemCategory} containing {@link List}
   *         of page templates
   */
  public List<SelectItemCategory<String>> getPageTemplateCategories() {
    return Collections.unmodifiableList(categories);
  }
}
