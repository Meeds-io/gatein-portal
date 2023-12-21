package org.exoplatform.portal.page;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.webui.core.model.SelectItemCategory;
import org.exoplatform.webui.core.model.SelectItemOption;

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

  /**
   * @return {@link List} of page templates of page templates
   */
  public List<SelectItemOption<String>> getPageTemplates() {
    return categories.stream().map(SelectItemCategory::getSelectItemOptions).flatMap(Collection::stream).toList();
  }
}
