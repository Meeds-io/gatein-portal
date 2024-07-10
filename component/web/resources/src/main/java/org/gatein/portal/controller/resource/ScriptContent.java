/*
 * Copyright (C) 2011 eXo Platform SAS.
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
package org.gatein.portal.controller.resource;

import java.io.File;

import com.mchange.io.FileUtils;

import lombok.Getter;
import lombok.SneakyThrows;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public class ScriptContent {

  public static final ScriptContent NOT_FOUND = new ScriptContent(new byte[0], 0);

  @Getter
  private File                      file;

  @Getter
  private long                      hash;

  @Getter
  private byte[]                    bytes;

  /**
   * Used in prod mode
   */
  public ScriptContent(File file, long hash) {
    this.file = file;
    this.hash = hash;
  }

  /**
   * Used in dev mode
   */
  public ScriptContent(byte[] bytes, long hash) {
    this.bytes = bytes;
    this.hash = hash;
  }

  @SneakyThrows
  public byte[] getContentAsBytes() {
    if (this.bytes != null && this.bytes.length > 0) {
      return this.bytes;
    } else if (file != null) {
      return FileUtils.getBytes(file);
    } else {
      return null; // NOSONAR
    }
  }
}
