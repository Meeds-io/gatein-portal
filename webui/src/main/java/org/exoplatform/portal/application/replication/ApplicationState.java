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

package org.exoplatform.portal.application.replication;

import java.io.Serializable;

import org.exoplatform.webui.core.UIApplication;

import lombok.Getter;

/**
 * The state of an application.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class ApplicationState implements Serializable {

  private static final long serialVersionUID = -5815789830129802739L;

  /** . */
  @Getter
  private UIApplication     application;

  /** . */
  @Getter
  private String            userName;

  public ApplicationState(UIApplication application, String userName) {
    if (application == null) {
      throw new NullPointerException();
    }
    this.application = application;
    this.userName = userName;
  }

}
