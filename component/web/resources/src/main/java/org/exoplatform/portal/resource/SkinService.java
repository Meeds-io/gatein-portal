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

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.cache.future.FutureMap;
import org.exoplatform.commons.cache.future.Loader;
import org.exoplatform.commons.utils.BinaryOutput;
import org.exoplatform.commons.utils.ByteArrayOutput;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.commons.utils.Safe;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.management.annotations.*;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.management.rest.annotations.RESTEndpoint;
import org.exoplatform.portal.resource.compressor.ResourceCompressor;
import org.exoplatform.portal.resource.compressor.ResourceType;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.Orientation;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.controller.router.URIWriter;
import org.exoplatform.web.url.MimeType;
import org.gatein.portal.controller.resource.ResourceRequestHandler;
import org.gatein.wci.ServletContainerFactory;
import org.gatein.wci.WebAppListener;
import org.picocontainer.Startable;

import jakarta.servlet.ServletContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Managed
@NameTemplate({ @Property(key = "view", value = "portal"), @Property(key = "service", value = "management"),
    @Property(key = "type", value = "skin") })
@ManagedDescription("Skin service")
@RESTEndpoint(path = "skinservice")
public class SkinService extends AbstractResourceService implements Startable {

  protected static Log                                           log                     =
                                                                     ExoLogger.getLogger("portal.SkinService");

  private static final String                                    LEFT_P                  = "\\(";

  private static final String                                    RIGHT_P                 = "\\)";

  /** Immutable and therefore thread safe. */
  private static final Pattern                                   IMPORT_PATTERN          = Pattern.compile("(@import\\s+" + "url"
      + LEFT_P + "['\"]?" + ")([^'\";]+.css)(" + "['\"]?" + RIGHT_P + "\\s*;)");

  /** Immutable and therefore thread safe. */
  private static final Pattern                                   BACKGROUND_PATTERN      =
                                                                                    Pattern.compile("(background[^;])+([^;]*;)");

  private static final Pattern                                   FONT_FACE_PATTERN       = Pattern.compile("(src[^;])+([^;]*;)");

  private static final Pattern                                   URL_PATTERN             = Pattern.compile("(url" + LEFT_P
      + "['\"]?)([^'\";" + RIGHT_P + "]+)(['\"]?\\))");

  /** Immutable and therefore thread safe. */
  private static final Pattern                                   LT                      =
                                                                    Pattern.compile(".*/\\*.*orientation=lt.*\\*/.*");

  /** Immutable and therefore thread safe. */
  private static final Pattern                                   RT                      =
                                                                    Pattern.compile(".*/\\*.*orientation=rt.*\\*/.*");

  public static final String                                     DEFAULT_SKIN_PARAM_NAME = "skin.default";

  private static final String                                    DEFAULT_SKIN            = "Enterprise";

  public static final String                                     CUSTOM_MODULE_ID        = "customModule";

  /** The deployer. */
  private final WebAppListener                                   deployer;

  private final Map<SkinKey, SkinConfig>                         portalSkins_;

  private final Map<SkinKey, SkinConfig>                         customPortalSkins_;

  private final Map<SkinKey, SkinConfig>                         skinConfigs_;

  private final HashSet<String>                                  availableSkins_;

  private final FutureMap<String, CachedStylesheet, SkinContext> ltCache;

  private final FutureMap<String, CachedStylesheet, SkinContext> rtCache;

  private final Map<String, Set<String>>                         portletThemes_;

  private String                                                 defaultSkin;

  /**
   * The name of the portal container
   */
  final String                                                   portalContainerName;

  /**
   * An id used for caching request. The id life cycle is the same than the
   * class instance because we consider css will change until server is
   * restarted. Of course this only applies for the developing mode set to
   * false.
   */
  final String                                                   id                      =
                                                                    Long.toString(System.currentTimeMillis());

  public static final long                                      MAX_AGE;

  static {
    long seconds = 31536000L;
    String propValue = PropertyManager.getProperty("gatein.assets.css.max-age");
    if (propValue != null) {
      try {
        seconds = Long.valueOf(propValue);
      } catch (NumberFormatException e) {
        log.warn("The gatein.assets.css.max-age property is not set properly.");
      }
    }

    MAX_AGE = seconds;
  }

  static class SkinContext {
    final ControllerContext controller;

    final Orientation       orientation;

    SkinContext(ControllerContext controller, Orientation orientation) {
      this.controller = controller;
      this.orientation = orientation;
    }
  }

  public SkinService(InitParams initParams, ExoContainerContext context, ResourceCompressor compressor) {
    super(compressor);
    Loader<String, CachedStylesheet, SkinContext> loader = new Loader<String, CachedStylesheet, SkinContext>() {
      public CachedStylesheet retrieve(SkinContext context, String key) throws Exception {
        Resource skin = getCSSResource(key, key);
        if (skin == null) {
          return null;
        }

        StringBuffer sb = new StringBuffer();
        processCSSRecursively(context.controller, sb, true, skin, context.orientation);
        String css = sb.toString();
        try {
          if (SkinService.this.compressor.isSupported(ResourceType.STYLESHEET)) {
            css = SkinService.this.compressor.compress(css, ResourceType.STYLESHEET);
          }
        } catch (Exception e) {
          log.warn("Error when compressing CSS " + key + " for orientation " + context + " will use normal CSS instead", e);
        }

        return new CachedStylesheet(css);
      }
    };

    //
    portalSkins_ = new LinkedHashMap<SkinKey, SkinConfig>();
    customPortalSkins_ = new LinkedHashMap<SkinKey, SkinConfig>();
    skinConfigs_ = new LinkedHashMap<SkinKey, SkinConfig>(20);
    availableSkins_ = new HashSet<String>(5);
    ltCache = new FutureMap<String, CachedStylesheet, SkinContext>(loader);
    rtCache = new FutureMap<String, CachedStylesheet, SkinContext>(loader);
    portletThemes_ = new HashMap<String, Set<String>>();
    portalContainerName = context.getPortalContainerName();
    deployer = new GateInSkinConfigDeployer(portalContainerName, this);

    defaultSkin = DEFAULT_SKIN;
    if (initParams != null) {
      ValueParam defaultSkinValueParam = initParams.getValueParam(DEFAULT_SKIN_PARAM_NAME);
      if (defaultSkinValueParam != null && StringUtils.isNotEmpty(defaultSkinValueParam.getValue())) {
        defaultSkin = defaultSkinValueParam.getValue();
      }
    }

    addResourceResolver(new CompositeResourceResolver(portalContainerName, skinConfigs_));
  }

  public void addSkinConfig(SkinConfigPlugin skinConfigPlugin) {
    if (!skinConfigPlugin.getAvailableSkins().isEmpty()) {
      for (String newSkin : skinConfigPlugin.getAvailableSkins()) {
        availableSkins_.add(newSkin);
      }
    }
    if (StringUtils.isNotBlank(skinConfigPlugin.getDefaultSkin())) {
      defaultSkin = skinConfigPlugin.getDefaultSkin();
    }
  }

  public String getDefaultSkin() {
    if (!availableSkins_.contains(defaultSkin)) {
      log.warn("Skin \"{}\" does not exist, switching to skin \"Default\" as the default skin", defaultSkin);
      defaultSkin = DEFAULT_SKIN;
    }
    return defaultSkin;
  }

  /**
   * Add a new category for portlet themes if it does not exist
   *
   * @param categoryName the category name
   */
  public void addCategoryTheme(String categoryName) {
    if (!portletThemes_.containsKey(categoryName)) {
      portletThemes_.put(categoryName, new HashSet<String>());
    }
  }

  /**
   * @deprecated use {@link #addPortalSkin(String, String, String)} instead
   */
  @Deprecated
  public void addPortalSkin(String module, String skinName, String cssPath, ServletContext scontext) {
    addPortalSkin(module, skinName, cssPath);
  }

  /**
   * Add a portal skin with the <code>priority</code> is Integer.MAX_VALUE and
   * the <code>overwrite</code> is false by default
   *
   * @param module
   * @param skinName
   * @param cssPath
   */
  public void addPortalSkin(String module, String skinName, String cssPath) {
    addPortalSkin(module, skinName, cssPath, Integer.MAX_VALUE, false);
  }

  /**
   * @deprecated use {@link #addPortalSkin(String, String, String, boolean)}
   *             instead
   */
  @Deprecated
  public void addPortalSkin(String module, String skinName, String cssPath, ServletContext scontext, boolean overwrite) {
    addPortalSkin(module, skinName, cssPath, overwrite);
  }

  /**
   * Add a portal skin with the <code>priority</code> is Integer.MAX_VALUE by
   * default
   *
   * @param module
   * @param skinName
   * @param cssPath
   * @param overwrite
   */
  public void addPortalSkin(String module, String skinName, String cssPath, boolean overwrite) {
    addPortalSkin(module, skinName, cssPath, Integer.MAX_VALUE, overwrite);
  }

  /**
   * Register a portal skin
   *
   * @param module skin module identifier
   * @param skinName skin name
   * @param cssPath path uri to the css file. This is relative to the root
   *          context, use leading '/'
   * @param priority priority to support sorting in skin list
   * @param overwrite if any previous skin should be replaced by that one
   */
  public void addPortalSkin(String module, String skinName, String cssPath, int priority, boolean overwrite) {
    availableSkins_.add(skinName);
    SkinKey key = new SkinKey(module, skinName);
    SkinConfig skinConfig = portalSkins_.get(key);
    if (skinConfig == null || overwrite) {
      if (priority < 0) {
        priority = Integer.MAX_VALUE;
      }

      skinConfig = new SimpleSkin(this, module, skinName, cssPath, priority);
      if (module.startsWith(CUSTOM_MODULE_ID)) {
        skinConfig.setType("custom-skin");
        customPortalSkins_.put(key, skinConfig);
      } else {
        skinConfig.setType("portal-skin");
        portalSkins_.put(key, skinConfig);
      }
      if (log.isDebugEnabled()) {
        log.debug("Adding Portal skin : Bind " + key + " to " + skinConfig);
      }
    }
  }

  /**
   * Register a portal skin
   *
   * @param module skin module identifier
   * @param skinName skin name
   * @param cssPath path uri to the css file. This is relative to the root
   *          context, use leading '/'
   * @param priority priority to support sorting in skin list
   * @param overwrite if any previous skin should be replaced by that one
   * @param filtered if true, then the portal skin will be loaded only when required by a portlet
   */
  public void addPortalSkin(String module, String skinName, String cssPath, int priority, boolean overwrite, boolean filtered) {
    availableSkins_.add(skinName);
    SkinKey key = new SkinKey(module, skinName);
    SkinConfig skinConfig = portalSkins_.get(key);
    if (skinConfig == null || overwrite) {
      if (priority < 0) {
        priority = Integer.MAX_VALUE;
      }

      SimpleSkin skin = new SimpleSkin(module, skinName, cssPath, priority, filtered);
      if (module.startsWith(CUSTOM_MODULE_ID)) {
        skin.setType("custom-skin");
        customPortalSkins_.put(key, skin);
      } else {
        skin.setType("portal-skin");
        portalSkins_.put(key, skin);
      }
    }
  }

  /**
   * Register a portal skin with the specific <code>cssData</code>
   *
   * @deprecated This method is not supported anymore. The resource resolver
   *             pluggability mechanism should be used somehow
   * @param module skin module identifier
   * @param skinName skin name
   * @param cssPath path uri to the css file. This is relative to the root
   *          context, use leading '/'
   * @param cssData the content of css
   */
  @Deprecated
  public void addPortalSkin(String module, String skinName, String cssPath, String cssData) {
    throw new UnsupportedOperationException("This method is not supported anymore.");
  }

  /**
   * @deprecated use {@link #addSkin(String, String, String)} instead
   */
  @Deprecated
  public void addSkin(String module, String skinName, String cssPath, ServletContext scontext) {
    addSkin(module, skinName, cssPath);
  }

  /**
   * Add a skin with the <code>priority</code> is Integer.MAX_VALUE and the
   * <code>overwrite</code> is false by default
   *
   * @param module
   * @param skinName
   * @param cssPath
   */
  public void addSkin(String module, String skinName, String cssPath) {
    addSkin(module, skinName, cssPath, Integer.MAX_VALUE, false);
  }

  /**
   * @deprecated use {@link #addSkin(String, String, String, boolean)} instead
   */
  @Deprecated
  public void addSkin(String module, String skinName, String cssPath, ServletContext scontext, boolean overwrite) {
    addSkin(module, skinName, cssPath, overwrite);
  }

  /**
   * Add a portal skin with the <code>priority</code> is Integer.MAX_VALUE
   *
   * @param module
   * @param skinName
   * @param cssPath
   * @param overwrite
   */
  public void addSkin(String module, String skinName, String cssPath, boolean overwrite) {
    addSkin(module, skinName, cssPath, Integer.MAX_VALUE, overwrite);
  }

  /**
   * Register the Skin for available portal Skins. Support priority
   *
   * @param module skin module identifier
   * @param skinName skin name
   * @param cssPath path uri to the css file. This is relative to the root
   *          context, use leading '/'
   * @param overwrite if any previous skin should be replaced by that one
   * @param priority priority to support sorting in skin list
   */
  public void addSkin(String module, String skinName, String cssPath, int priority, boolean overwrite) {
    addSkin(module, skinName, cssPath, priority, overwrite, null);
  }

  public void addSkin(String module,
                      String skinName,
                      String cssPath,
                      int priority,
                      boolean overwrite,
                      List<String> additionalModules) {
    availableSkins_.add(skinName);
    SkinKey key = new SkinKey(module, skinName);
    SkinConfig skinConfig = skinConfigs_.get(key);
    if (skinConfig == null || overwrite) {
      if (priority < 0) {
        priority = Integer.MAX_VALUE;
      }
      skinConfig = new SimpleSkin(module, skinName, cssPath, priority, additionalModules);
      skinConfig.setType("portlet-skin");
      skinConfigs_.put(key, skinConfig);
    }
  }

  /**
   * Register the Skin for available portal Skins. Do not replace existed Skin
   *
   * @param module skin module identifier
   * @param skinName skin name
   * @param cssPath path uri to the css file. This is relative to the root
   *          context, use leading '/'
   * @param cssData
   */
  @Deprecated
  public void addSkin(String module, String skinName, String cssPath, String cssData) {
    availableSkins_.add(skinName);
    SkinKey key = new SkinKey(module, skinName);
    SkinConfig skinConfig = skinConfigs_.get(key);
    if (skinConfig == null) {
      skinConfig = new SimpleSkin(this, module, skinName, cssPath);
      skinConfig.setType("custom");
      skinConfigs_.put(key, skinConfig);
    }
    ltCache.remove(cssPath);
    rtCache.remove(cssPath);
  }

  /**
   * Merge several skins into one single skin.
   *
   * @param skins the skins to merge
   * @return the merged skin
   */
  public Skin merge(Collection<SkinConfig> skins) {
    return merge(skins, null);
  }

  public Skin merge(Collection<SkinConfig> skins, String id) {
    return new CompositeSkin(this, skins, id);
  }

  /**
   * Registry theme category with its themes for portlet Theme
   *
   * @param categoryName category name that will be registried
   * @param themesName list theme name of categoryName
   */
  public void addTheme(String categoryName, List<String> themesName) {
    if (!portletThemes_.containsKey(categoryName))
      portletThemes_.put(categoryName, new HashSet<String>());
    Set<String> catThemes = portletThemes_.get(categoryName);
    for (String theme : themesName)
      catThemes.add(theme);
  }

  /**
   * Get names of all the currently registered skins.
   *
   * @return an unmodifiable Set of the currently registered skins
   */
  public Set<String> getAvailableSkinNames() {
    return availableSkins_;
  }

  /**
   * Return the CSS content of the file specified by the given URI.
   *
   * @param context
   * @param compress
   * @return the css contet or null if not found.
   */
  public String getCSS(ControllerContext context, boolean compress) {
    try {
      final ByteArrayOutput output = new ByteArrayOutput();
      boolean success = renderCSS(context, new ResourceRenderer() {
        public BinaryOutput getOutput() {
          return output;
        }

        public void setExpiration(long seconds) {
        }
      }, compress);

      if (success) {
        return output.getString();
      } else {
        return null;
      }
    } catch (IOException e) {
      // log.error("Error while rendering css " + path, e);
      return null;
    } catch (RenderingException e) {
      // log.error("Error while rendering css " + path, e);
      return null;
    }
  }

  /**
   * Render css content of the file specified by the given URI
   *
   * @param context
   * @param renderer the webapp's
   *          {@link org.exoplatform.portal.resource.ResourceRenderer}
   * @param compress
   * @throws RenderingException
   * @throws IOException
   * @return <code>true</code> if the <code>CSS resource </code>is found and
   *         rendered; <code>false</code> otherwise.
   */
  public boolean renderCSS(ControllerContext context, ResourceRenderer renderer, boolean compress) throws RenderingException,
                                                                                                   IOException {
    String dir = context.getParameter(ResourceRequestHandler.ORIENTATION_QN);

    Orientation orientation = "rt".equals(dir) ? Orientation.RT : Orientation.LT;

    // Check if it is running under developing mode
    String resource = "/" + context.getParameter(ResourceRequestHandler.RESOURCE_QN) + ".css";
    if (!compress) {
      StringBuffer sb = new StringBuffer();
      Resource skin = getCSSResource(resource, resource);
      if (skin != null) {
        processCSSRecursively(context, sb, false, skin, orientation);
        byte[] bytes = sb.toString().getBytes("UTF-8");
        renderer.setExpiration(MAX_AGE);
        if (context.getResponse() != null) {
          context.getResponse().setContentLength(bytes.length);
        }
        renderer.getOutput().write(bytes);
        return true;
      }
    } else {
      FutureMap<String, CachedStylesheet, SkinContext> cache = orientation == Orientation.LT ? ltCache : rtCache;
      CachedStylesheet cachedCss = cache.get(new SkinContext(context, orientation), resource);
      if (cachedCss != null) {
        renderer.setExpiration(MAX_AGE);
        if (context.getResponse() != null) {
          context.getResponse().setContentLength(cachedCss.getLength());
        }
        cachedCss.writeTo(renderer.getOutput());
        return true;
      }
    }

    //
    return false;
  }

  /**
   * Return CSS data corresponding to the <code>path</code>
   *
   * @param context
   * @param path path uri to the css file
   * @return css content of URI file or null if not found
   */
  @Deprecated
  public String getMergedCSS(ControllerContext context, String path) {
    return getCSS(context, true);
  }

  /**
   * Return a collection of Portal Skins that its elements are ordered by CSS
   * priority
   *
   * @param skinName name of Portal Skin
   * @return all org.exoplatform.portal.resource.SkinConfig of Portal Skin
   */
  public Collection<SkinConfig> getPortalSkins(String skinName) {
    if (StringUtils.isEmpty(skinName)) {
      skinName = getDefaultSkin();
    }

    Set<SkinKey> keys = portalSkins_.keySet();
    List<SkinConfig> portalSkins = new ArrayList<SkinConfig>();
    for (SkinKey key : keys) {
      if (key.getName().equals(skinName)) {
        SkinConfig skinConfig = portalSkins_.get(key);
        if (!skinConfig.isFiltered()) {
          portalSkins.add(skinConfig);
        }
      }
    }
    Collections.sort(portalSkins, new Comparator<SkinConfig>() {
      public int compare(SkinConfig o1, SkinConfig o2) {
        return o1.getCSSPriority() - o2.getCSSPriority();
      }
    });
    return portalSkins;
  }

  /**
   * Return a collection of SkinConfig based on SkinVisitor provided as the
   * argument
   *
   * @param visitor
   * @return
   */
  public Collection<SkinConfig> findSkins(SkinVisitor visitor) {
    return visitor.getSkins(portalSkins_.entrySet(), skinConfigs_.entrySet());
  }

  /**
   * Return the collection of custom portal skins
   *
   * @param skinName
   * @return the map of custom portal skins
   */
  public Collection<SkinConfig> getCustomPortalSkins(String skinName) {
    if (StringUtils.isEmpty(skinName)) {
      skinName = getDefaultSkin();
    }

    Set<SkinKey> keys = customPortalSkins_.keySet();
    List<SkinConfig> customPortalSkins = new ArrayList<SkinConfig>();
    for (SkinKey key : keys) {
      if (key.getName().equals(skinName)) {
        SkinConfig skinConfig = customPortalSkins_.get(key);
        if (!skinConfig.isFiltered()) {
          customPortalSkins.add(skinConfig);
        }
      }
    }
    Collections.sort(customPortalSkins, new Comparator<SkinConfig>() {
      public int compare(SkinConfig o1, SkinConfig o2) {
        return o1.getCSSPriority() - o2.getCSSPriority();
      }
    });
    return customPortalSkins;
  }

  /**
   * Return the map of portlet themes
   *
   * @return the map of portlet themes
   */
  public Map<String, Set<String>> getPortletThemes() {
    return portletThemes_;
  }

  /**
   * Return a SkinConfig mapping by the module and skin name
   *
   * @param module
   * @param skinName
   * @return SkinConfig by SkinKey(module, skinName), or SkinConfig by
   *         SkinKey(module, defaultSkin)
   */
  public SkinConfig getSkin(String module, String skinName) {
    if (StringUtils.isBlank(skinName)) {
      skinName = getDefaultSkin();
    }

    SkinConfig config = skinConfigs_.get(new SkinKey(module, skinName));
    if (config == null) {
      config = skinConfigs_.get(new SkinKey(module, getDefaultSkin()));
    }
    return config;
  }

  /**
   * Return a Portal SkinConfig mapping by the module and skin name
   *
   * @param module
   * @param skinName
   * @return SkinConfig by SkinKey(module, skinName), or SkinConfig by
   *         SkinKey(module, SkinService.DEFAULT_SKIN)
   */
  public SkinConfig getPortalSkin(String module, String skinName) {
    SkinConfig portalSkin = getPortalSkin(module, skinName, portalSkins_);
    if (portalSkin == null) {
      portalSkin = getPortalSkin(module, skinName, customPortalSkins_);
    }
    return portalSkin;
  }

  /**
   * Remove SkinKey from SkinCache by portalName and skinName
   *
   * @deprecated the method name is wrong to the behaviour it does. Use
   *             {@link #removeSkin(String, String)} instead
   * @param portalName
   * @param skinName
   */
  @Deprecated
  public void invalidatePortalSkinCache(String portalName, String skinName) {
    SkinKey key = new SkinKey(portalName, skinName);
    skinConfigs_.remove(key);
  }

  /**
   * Invalidate skin from the cache
   *
   * @param path the key
   */
  public void invalidateCachedSkin(String path) {
    ltCache.remove(path);
    rtCache.remove(path);
  }

  /**
   * Returns last modified date of cached css.
   * <p>
   * In development mode, it always returns {@link Long#MAX_VALUE}. Return null
   * if cached css can not be found
   *
   * @param context
   */
  public long getLastModified(ControllerContext context) {
    String resource = "/" + context.getParameter(ResourceRequestHandler.RESOURCE_QN) + ".css";

    FutureMap<String, CachedStylesheet, SkinContext> cache = ltCache;
    Orientation orientation = Orientation.LT;
    String dir = context.getParameter(ResourceRequestHandler.ORIENTATION_QN);
    if ("rt".equals(dir)) {
      orientation = Orientation.RT;
      cache = rtCache;
    }

    CachedStylesheet cachedCSS = cache.get(new SkinContext(context, orientation), resource);
    if (cachedCSS == null) {
      return System.currentTimeMillis();
    } else {
      return cachedCSS.getLastModified();
    }
  }

  /**
   * @deprecated The method name is not clear. Using
   *             {@link #removeSkin(String, String)} instead
   */
  @Deprecated
  public void remove(String module, String skinName) {
    removeSkin(module, skinName);
  }

  /**
   * Remove a Skin from the service as well as its cache
   *
   * @param module
   * @param skinName
   */
  public void removeSkin(String module, String skinName) {
    SkinKey key;
    if (skinName == null || skinName.length() == 0) {
      key = new SkinKey(module, getDefaultSkin());
    } else {
      key = new SkinKey(module, skinName);
    }

    removeSkin(key);
  }

  /**
   * Remove a Skin mapped to the <code>key</code>
   *
   * @param key key whose mapping skin is to be removed from the service
   */
  public void removeSkin(SkinKey key) {
    if (key == null) {
      return;
    }

    SkinConfig remove = skinConfigs_.remove(key);

    if (remove != null && StringUtils.isNotBlank(remove.getCSSPath())) {
      invalidateCachedSkin(remove.getCSSPath());
    }
  }

  /**
   * Remove a Skin from the service as well as its cache
   *
   * @param module
   * @param skinName
   */
  public void removePortalSkin(String module, String skinName) {
    SkinKey key;
    if (StringUtils.isBlank(skinName)) {
      key = new SkinKey(module, getDefaultSkin());
    } else {
      key = new SkinKey(module, skinName);
    }

    removePortalSkin(key);
  }

  /**
   * Remove a Skin mapped to the <code>key</code>
   *
   * @param key key whose mapping skin is to be removed from the service
   */
  public void removePortalSkin(SkinKey key) {
    if (key == null) {
      return;
    }

    SkinConfig remove = null;
    if (key.getModule().startsWith(CUSTOM_MODULE_ID)) {
      remove = customPortalSkins_.remove(key);
    } else {
      remove = portalSkins_.remove(key);
    }

    if (remove != null && StringUtils.isNotBlank(remove.getCSSPath())) {
      invalidateCachedSkin(remove.getCSSPath());
    }
  }

  /**
   * @deprecated This is deprecated as its name was not clear. Use
   *             {@link #removeSkins(List)} instead
   */
  @Deprecated
  public void remove(List<SkinKey> keys) throws Exception {
    removeSkins(keys);
  }

  /**
   * Remove SkinConfig from Portal Skin Config by SkinKey
   *
   * @param keys SkinKey list these will be removed
   */
  public void removeSkins(List<SkinKey> keys) {
    if (keys == null) {
      return;
    }

    for (SkinKey key : keys) {
      removeSkin(key);
    }
  }

  /**
   * Remove Skin from Portal available Skin by skin name
   *
   * @param skinName name of skin that will be removed
   */
  public void removeSupportedSkin(String skinName) {
    availableSkins_.remove(skinName);
  }

  /**
   * Return the number of skin config maintaining in this SkinService
   *
   * @return the number of skin config maintaining in this SkinService
   */
  public int size() {
    return skinConfigs_.size();
  }

  /**
   * This method delegates the resource resolving to MainResourceResolver and
   * prints out appropriated log messages Consider the two cases the method is
   * invoked Case 1: Resolve nested .css file In Stylesheet.css we have the
   * statement
   *
   * @import url(xyzt.css); To resolve the resource from xyzt.css,
   *         getCSSResource("xyzt.css", "Stylesheet.css") is called Case 2:
   *         Resolve top root .css file To resolve a top root Stylesheet.css
   *         file, getCSSResource("Stylesheet.css", "Stylesheet.css") is called
   * @param cssPath
   * @param outerCssFile
   * @return
   */
  private Resource getCSSResource(String cssPath, String outerCssFile) {
    Resource resource = mainResolver.resolve(cssPath);
    if (resource == null && log.isErrorEnabled()) {
      String logMessage;
      if (!cssPath.equals(outerCssFile)) {
        int lastIndexOfSlash = cssPath.lastIndexOf('/');
        String loadedCssFile = (lastIndexOfSlash >= 0) ? (cssPath.substring(lastIndexOfSlash + 1)) : cssPath;
        logMessage = "Invalid <CSS FILE> configuration, please check the @import url(" + loadedCssFile + ") in " + outerCssFile
            + " , SkinService could not load the skin " + cssPath;
      } else {
        logMessage = "Not found <CSS FILE>, the path " + cssPath + " is invalid, SkinService could not load the skin " + cssPath;
      }
      log.error(logMessage);
    }
    return resource;
  }

  /**
   * Apply CSS for Skin <br>
   * If skin is null, do nothing
   *
   * @param context
   * @param appendable
   * @param merge
   * @param skin
   * @param orientation
   * @throws RenderingException
   * @throws IOException
   */
  private void processCSSRecursively(ControllerContext context,
                                     Appendable appendable,
                                     boolean merge,
                                     Resource skin,
                                     Orientation orientation) throws RenderingException, IOException {
    if (skin == null) {
      return;
    }
    // The root URL for the entry
    String basePath = skin.getContextPath() + skin.getParentPath();

    //
    Reader tmp = skin.read();
    if (tmp == null) {
      throw new RenderingException("No skin resolved for path " + skin.getResourcePath());
    }
    BufferedReader reader = new SkipCommentReader(tmp, new CommentBlockHandler.OrientationCommentBlockHandler());
    try {
      String line = reader.readLine();
      while (line != null) {
        line = proccessOrientation(line, orientation);
        line = processURL(BACKGROUND_PATTERN, line, basePath);
        line = processURL(FONT_FACE_PATTERN, line, basePath);

        Matcher matcher = IMPORT_PATTERN.matcher(line);
        while (matcher.find()) {
          String includedPath = matcher.group(2);
          if (!includedPath.startsWith("/")) {
            includedPath = basePath + includedPath;
          }

          StringBuffer strReplace = new StringBuffer();
          if (merge) {
            Resource ssskin = getCSSResource(includedPath, basePath + skin.getFileName());
            processCSSRecursively(context, strReplace, merge, ssskin, orientation);
          } else {
            // Remove leading '/' and trailing '.css'
            String resource = includedPath.substring(1, includedPath.length() - ".css".length());

            //
            Map<QualifiedName, String> params = new HashMap<QualifiedName, String>();
            params.put(ResourceRequestHandler.VERSION_QN, ResourceRequestHandler.VERSION);
            params.put(ResourceRequestHandler.ORIENTATION_QN, orientation == Orientation.RT ? "rt" : "lt");
            params.put(ResourceRequestHandler.COMPRESS_QN, merge ? "min" : "");
            params.put(WebAppController.HANDLER_PARAM, "skin");
            params.put(ResourceRequestHandler.RESOURCE_QN, resource);
            StringBuilder embeddedPath = new StringBuilder();
            context.renderURL(params, new URIWriter(embeddedPath, MimeType.PLAIN));

            //
            strReplace.append(matcher.group(1));
            strReplace.append(embeddedPath);
            strReplace.append(matcher.group(3));
          }
          String str = strReplace.toString().replaceAll("\\$", "\\\\\\$");
          // Add one more "\" before ISO-in-CSS-content character: for example:
          // "\00a0" -> "\\00a0"
          // because method Matcher#appendReplacement() will ignore one
          str = str.replaceAll("(\\\\[0-9a-fA-F]{4})", "\\\\$1");
          matcher.appendReplacement((StringBuffer) appendable, str);
        }
        matcher.appendTail((StringBuffer) appendable);

        if ((line = reader.readLine()) != null) {
          appendable.append("\n");
        }
      }
    } finally {
      Safe.close(reader);
    }
  }

  private String processURL(Pattern pattern, String line, String basePath) {
    Matcher patternMatcher = pattern.matcher(line);
    StringBuffer tmpBuilder = new StringBuffer();
    while (patternMatcher.find()) {
      StringBuilder fontFaceReplace = new StringBuilder();
      Matcher urlMatcher = URL_PATTERN.matcher(patternMatcher.group());
      StringBuffer tmpURL = new StringBuffer();
      while (urlMatcher.find()) {
        if (!urlMatcher.group(2).startsWith("\"/") && !urlMatcher.group(2).startsWith("'/")
            && !urlMatcher.group(2).startsWith("/")) {
          StringBuilder urlBuilder = new StringBuilder();
          urlBuilder.append(urlMatcher.group(1));
          urlBuilder.append(basePath);
          urlBuilder.append(urlMatcher.group(2));
          urlBuilder.append(urlMatcher.group(3));
          urlMatcher.appendReplacement(tmpURL, urlBuilder.toString());
        }
      }
      urlMatcher.appendTail(tmpURL);
      patternMatcher.appendReplacement(tmpBuilder, tmpURL.toString());
    }

    patternMatcher.appendTail(tmpBuilder);
    return tmpBuilder.toString();
  }

  private String proccessOrientation(String line, Orientation orientation) {
    Pattern orientationPattern = orientation == Orientation.LT ? RT : LT;
    Matcher matcher = orientationPattern.matcher(line);
    StringBuffer tmpBuilder = new StringBuffer();
    while (matcher.find()) {
      matcher.appendReplacement(tmpBuilder, "");
    }
    matcher.appendTail(tmpBuilder);
    return tmpBuilder.toString();
  }

  private SkinConfig getPortalSkin(String module, String skinName, Map<SkinKey, SkinConfig> portalSkins) {
    if (StringUtils.isEmpty(skinName)) {
      skinName = getDefaultSkin();
    }

    SkinConfig config = portalSkins.get(new SkinKey(module, skinName));
    if (config == null) {
      config = portalSkins.get(new SkinKey(module, getDefaultSkin()));
    }
    return config;
  }

  /**
   * Get all available skin
   *
   * @return all available skin
   */
  @Managed
  @ManagedDescription("The list of registered skins identifiers")
  public String[] getSkinList() {
    // get all available skin
    List<String> availableSkin = new ArrayList<String>();
    for (String skin : availableSkins_) {
      availableSkin.add(skin);
    }
    // sort skin name asc
    Collections.sort(availableSkin);

    return availableSkin.toArray(new String[availableSkin.size()]);
  }

  /**
   * Clean cache, reload all Skins
   */
  @Managed
  @ManagedDescription("Reload all skins")
  @Impact(ImpactType.WRITE)
  public void reloadSkins() {
    // remove all ltCache, rtCache
    ltCache.clear();
    rtCache.clear();
  }

  /**
   * reload skin by skin ID
   *
   * @param skinId the skin ID that will be reloaded
   */
  @Managed
  @ManagedDescription("Reload a specified skin")
  public void reloadSkin(@ManagedDescription("The skin id") @ManagedName("skinId") String skinId) {
    ltCache.remove(skinId);
    rtCache.remove(skinId);
  }

  /**
   * Start service. Registry
   * org.exoplatform.portal.resource.GateInSkinConfigDeployer and
   * org.exoplatform.portal.resource.GateInSkinConfigRemoval into
   * ServletContainer.
   *
   * @see org.picocontainer.Startable#start()
   */
  public void start() {
    ServletContainerFactory.getServletContainer().addWebAppListener(deployer);
  }

  /**
   * Stop service Remove
   * org.exoplatform.portal.resource.GateInSkinConfigDeployer and
   * org.exoplatform.portal.resource.GateInSkinConfigRemoval from
   * ServletContainer.
   *
   * @see org.picocontainer.Startable#stop()
   */
  public void stop() {
    ServletContainerFactory.getServletContainer().removeWebAppListener(deployer);
  }

}
