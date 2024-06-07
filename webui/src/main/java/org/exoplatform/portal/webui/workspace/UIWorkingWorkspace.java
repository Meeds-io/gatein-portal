/**
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

package org.exoplatform.portal.webui.workspace;

import org.exoplatform.portal.webui.page.UISiteBody;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;

/**
 * Created by The eXo Platform SARL Author : Dang Van Minh minhdv81@yahoo.com Jun 12, 2006
 */

@ComponentConfig(id = "UIWorkingWorkspace", template = "system:/groovy/portal/webui/workspace/UIWorkingWorkspace.gtmpl")
public class UIWorkingWorkspace extends UIContainer {

    private UIPortal backupUIPortal = null;

    public UIPortal getBackupUIPortal() {
        return backupUIPortal;
    }

    public void setBackupUIPortal(UIPortal uiPortal) {
        backupUIPortal = uiPortal;
    }

    public UIPortal restoreUIPortal() {
        UIPortal result = backupUIPortal;
        if (result == null) {
            throw new IllegalStateException("backupUIPortal not available");
        } else {
            UISiteBody siteBody = findFirstComponentOfType(UISiteBody.class);
            siteBody.setUIComponent(result);
            return result;
        }
    }
}
