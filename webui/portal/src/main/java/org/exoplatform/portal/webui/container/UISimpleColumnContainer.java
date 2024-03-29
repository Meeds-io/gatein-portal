/*
 * This file is part of the Meeds project (https://meeds.io/).
 * 
 * Copyright (C) 2023 Meeds Association contact@meeds.io
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.portal.webui.container;

import java.util.Collections;
import java.util.List;

import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.container.UIContainerActionListener.EditContainerActionListener;
import org.exoplatform.portal.webui.portal.UIPortalComponent;
import org.exoplatform.portal.webui.portal.UIPortalComponentActionListener.DeleteComponentActionListener;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

@ComponentConfig(template = "system:/groovy/portal/webui/container/UISimpleColumnContainer.gtmpl", events = {
    @EventConfig(listeners = UISimpleColumnContainer.InsertColumnActionListener.class),
    @EventConfig(listeners = DeleteComponentActionListener.class, confirm = "UIColumnContainer.deleteColumnContainer"),
    @EventConfig(listeners = EditContainerActionListener.class) })
public class UISimpleColumnContainer extends UIContainer {
  public static final String SIMPLE_COLUMN_CONTAINER = "SimpleColumnContainer";

  public static final String INSERT_AFTER            = "insertColumnAfter";

  public static final String INSERT_BEFORE           = "insertColumnBefore";

  public UISimpleColumnContainer() {
    super();
  }

  public static class InsertColumnActionListener extends EventListener<UISimpleColumnContainer> {
    @Override
    public void execute(Event<UISimpleColumnContainer> event) throws Exception {
      String insertPosition = event.getRequestContext().getRequestParameter(UIComponent.OBJECTID);
      UISimpleColumnContainer uiSelectedColumn = event.getSource();
      UIPortalComponent uiParent = (UIPortalComponent) uiSelectedColumn.getParent();
      if (insertPosition.equals(INSERT_AFTER)) {
        UISimpleColumnContainer.insertColumn(uiSelectedColumn, true);
      } else if (insertPosition.equals(INSERT_BEFORE)) {
        UISimpleColumnContainer.insertColumn(uiSelectedColumn, false);
      }

      PortalRequestContext pcontext = (PortalRequestContext) event.getRequestContext();
      pcontext.addUIComponentToUpdateByAjax(uiParent);
      pcontext.ignoreAJAXUpdateOnPortlets(true);
      pcontext.getJavascriptManager()
              .require("SHARED/portalComposer", "portalComposer")
              .addScripts("portalComposer.toggleSaveButton();");
    }
  }

  private static void insertColumn(UISimpleColumnContainer selectedColumn, boolean isInsertAfter) throws Exception {
    UIContainer uiParent = selectedColumn.getParent();
    UISimpleColumnContainer uiNewColumn = uiParent.addChild(UISimpleColumnContainer.class, null, null);

    uiNewColumn.setTemplate(selectedColumn.getTemplate());
    uiNewColumn.setFactoryId(selectedColumn.getFactoryId());
    uiNewColumn.setId(String.valueOf(Math.abs(uiNewColumn.hashCode())));
    uiNewColumn.setMoveAppsPermissions(uiNewColumn.getAccessPermissions());
    uiNewColumn.setMoveContainersPermissions(uiNewColumn.getAccessPermissions());
    List<UIComponent> listColumn = uiParent.getChildren();
    int position = listColumn.indexOf(selectedColumn);
    if (isInsertAfter) {
      position += 1;
    }
    Collections.rotate(listColumn.subList(position, listColumn.size()), 1);
  }

}
