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

package org.exoplatform.portal.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.Orientation;

import lombok.Data;
import lombok.EqualsAndHashCode.Exclude;
import lombok.Getter;
import lombok.Setter;

/**
 * An implementation of the skin config. Created by The eXo Platform SAS Jan 19,
 * 2007
 */
@Data
public class SimpleSkin implements SkinConfig {

  public static final String   ORIENTATION_QUERY_PARAM = "orientation";

  public static final String   MINIFY_QUERY_PARAM      = "minify";

  public static final String   HASH_QUERY_PARAM        = "hash";

  private static final Log     LOG                     = ExoLogger.getLogger(SimpleSkin.class);

  @Setter
  private static boolean       developing              = PropertyManager.isDevelopping();

  private String               module;

  private String               name;

  private String               cssPath;

  private String               id;

  private int                  priority;

  private boolean              filtered;

  private List<String>         additionalModules;

  private String               type;

  @Exclude
  private int                  fileContentHash;

  @Getter
  private Map<Integer, String> urls                    = new ConcurrentHashMap<>();

  public SimpleSkin(String module, String name, String cssPath) {
    this(module, name, cssPath, Integer.MAX_VALUE, false, null);
  }

  public SimpleSkin(String module, String name, String cssPath, int cssPriority) {
    this(module, name, cssPath, cssPriority, false, null);
  }

  public SimpleSkin(String module, String name, String cssPath, int cssPriority, List<String> additionalModules) {
    this(module, name, cssPath, cssPriority, false, additionalModules);
  }

  public SimpleSkin(String module, String name, String cssPath, int cssPriority, boolean filtered) {
    this(module, name, cssPath, cssPriority, filtered, null);
  }

  public SimpleSkin(String module,
                    String name,
                    String cssPath,
                    int cssPriority,
                    boolean filtered,
                    List<String> additionalModules) {
    this.module = module;
    this.name = name;
    this.cssPath = cssPath;
    this.id = module.replace('/', '_');
    this.priority = cssPriority;
    this.additionalModules = additionalModules == null ? new ArrayList<>() : additionalModules;
    this.filtered = filtered;
  }

  @Override
  public String getCSSPath() {
    return cssPath;
  }

  @Override
  public void setCSSPath(String cssPath) {
    this.cssPath = cssPath;
  }

  @Override
  public int getCSSPriority() {
    return getPriority();
  }

  @Override
  public int getFileContentHash() {
    if (developing) {
      return 0;
    } else if (fileContentHash == 0) {
      String absolutePath = ("/" + cssPath).replace("//", "/"); // NOSONAR
      SkinService skinService = ExoContainerContext.getService(SkinService.class);
      try {
        String fileContent = skinService.getSkinModuleFileContent(absolutePath);
        fileContentHash = fileContent.hashCode();
      } catch (Exception e) {
        LOG.error("Error while processing CSS file {}", absolutePath, e);
      }
    }
    return fileContentHash;
  }

  @Override
  public SkinURL createURL() {
    if (StringUtils.isBlank(this.cssPath)) {
      return null;
    } else {
      return new SkinURL() {

        @Setter
        private Orientation orientation;

        @Override
        public String toString() {
          return toString(orientation);
        }

        @Override
        public String toString(Orientation orientation) {
          Orientation cssOrientation = orientation == null ? Orientation.LT : orientation;
          return urls.computeIfAbsent(Objects.hash(cssOrientation), k -> {
            String absolutePath = ("/" + cssPath).replace("//", "/"); // NOSONAR
            return absolutePath + "?" + ORIENTATION_QUERY_PARAM + "=" + cssOrientation.name() + "&" +
                MINIFY_QUERY_PARAM + "=" + !developing + "&" + HASH_QUERY_PARAM + "=" + getFileContentHash();
          });
        }
      };
    }
  }

}
