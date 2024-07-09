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

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.portal.resource.compressor.ResourceType;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.Orientation;
import org.exoplatform.web.ControllerContext;

import lombok.Getter;

/**
 * An implementation of the skin config. Created by The eXo Platform SAS Jan 19,
 * 2007
 */
class SimpleSkin implements SkinConfig {

  private static final Log     LOG   = ExoLogger.getLogger(SimpleSkin.class);

  private final String         module;

  private final String         name;

  private final String         cssPath;

  private final String         id;

  private final int            priority;

  private final boolean        filtered;

  private final List<String>   additionalModules;

  private String               type;

  @Getter
  private Map<Integer, String> urls  = new HashMap<>();

  @Getter
  private Map<Integer, File>   files = new HashMap<>();

  public SimpleSkin(SkinService service, String module, String name, String cssPath) {
    this(service, module, name, cssPath, Integer.MAX_VALUE);
  }

  public SimpleSkin(SkinService service, String module, String name, String cssPath, int cssPriority) {
    this.module = module;
    this.name = name;
    this.cssPath = cssPath;
    this.id = module.replace('/', '_');
    this.priority = cssPriority;
    this.additionalModules = null;
    this.filtered = false;
  }

  public SimpleSkin(String module, String name, String cssPath, int cssPriority, List<String> additionalModules) {
    this.module = module;
    this.name = name;
    this.cssPath = cssPath;
    this.id = module.replace('/', '_');
    this.priority = cssPriority;
    this.additionalModules = additionalModules;
    this.filtered = false;
  }

  public SimpleSkin(String module, String name, String cssPath, int cssPriority, boolean filtered) {
    this.module = module;
    this.name = name;
    this.cssPath = cssPath;
    this.id = module.replace('/', '_');
    this.priority = cssPriority;
    this.additionalModules = null;
    this.filtered = filtered;
  }

  @Override
  public int getCSSPriority() {
    return priority;
  }

  @Override
  public String getId() {
    return this.id;
  }

  @Override
  public String getModule() {
    return this.module;
  }

  @Override
  public String getCSSPath() {
    return this.cssPath;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public boolean isFiltered() {
    return filtered;
  }

  @Override
  public List<String> getAdditionalModules() {
    return additionalModules;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public void setType(String type) {
    this.type = type;
  }

  public String toString() {
    return "SimpleSkin[id=" + this.id + ",module=" + this.module + ",name=" + this.name + ",cssPath=" + this.cssPath +
        ", priority=" + priority +
        "]";
  }

  public SkinURL createURL(final ControllerContext context) {
    if (StringUtils.isBlank(this.cssPath)) {
      return null;
    }
    return new SkinURL() {

      Orientation orientation = null;

      boolean     compress    = !PropertyManager.isDevelopping();

      public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
      }

      public Orientation getOrientation() {
        return orientation == null ? Orientation.LT : Orientation.RT;
      }

      @Override
      public String toString() {
        return urls.computeIfAbsent(Objects.hash(orientation, compress), k -> {
          ConfigurationManager configurationManager = ExoContainerContext.getService(ConfigurationManager.class);
          SkinService skinService = ExoContainerContext.getService(SkinService.class);

          String absolutePath = ("/" + cssPath).replace("//", "/");
          try (InputStream inputStream = configurationManager.getInputStream(absolutePath)) {
            String fileContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            File file = File.createTempFile(module, ".css");
            file.deleteOnExit();
            try (Reader sourceReader = new StringReader(fileContent)) {
              StringBuilder sb = new StringBuilder();
              skinService.processCSSRecursively(absolutePath,
                                                sourceReader,
                                                sb,
                                                orientation);
              fileContent = sb.toString();
            }
            if (skinService.compressor.isSupported(ResourceType.STYLESHEET)) {
              fileContent = skinService.compressor.compress(fileContent, ResourceType.STYLESHEET);
            }
            FileUtils.write(file, fileContent, StandardCharsets.UTF_8);
            int fileContentHash = fileContent.hashCode();
            int hash = Objects.hash(fileContentHash, orientation, compress);
            files.put(hash, file);
            return absolutePath + "?orientation=" + getOrientation().name() + "&minify=" + compress + "&hash=" + fileContentHash;
          } catch (Exception e) {
            LOG.error("Error while processing CSS file {}", absolutePath, e);
            return null;
          }
        });
      }
    };
  }
}
