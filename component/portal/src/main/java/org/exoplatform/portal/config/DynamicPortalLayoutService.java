package org.exoplatform.portal.config;

import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.picocontainer.Startable;

import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.*;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * A Service to make displayed sites Portal Layout dynamic switch
 * configurations. This service will be used in front-end services to make
 * displayed site layout more dynamic and relying on stored site layout only.
 */
public class DynamicPortalLayoutService implements Startable {

  private static final Log                              LOG                         =
                                                            ExoLogger.getLogger(DynamicPortalLayoutService.class);

  private ConfigurationManager                          configurationManager;

  private DataStorage                                   dataStorage;

  /**
   * Whether to force relying on configured dynamic site layout when one of
   * {@link #getDynamicLayoutMatchers()} matches even when the
   * {@link PortalConfig} is not marked as
   * {@link PortalConfig#isDefaultLayout()} or not.
   */
  private boolean                                       forceIgnoreStoredLayout;

  /**
   * A set of {@link DynamicPortalLayoutMatcherPlugin} collected in orderd
   * {@link Map}.
   * 
   * <pre>
   * The order will be ensured by {@link ComponentPlugin#getPriority()}) retieved from IOC XML configuration of plugin
   * </pre>
   * 
   * <pre>
   * The unicity will be ensured by {@link ComponentPlugin#getName()}) retieved from IOC XML configuration of plugin
   * </pre>
   */
  private Map<String, DynamicPortalLayoutMatcherPlugin> dynamicLayoutMatcherPlugins = new LinkedHashMap<>();

  private List<DynamicPortalLayoutMatcherPlugin>        dynamicLayoutMatchers       = new ArrayList<>();

  public DynamicPortalLayoutService(ConfigurationManager configurationManager, DataStorage dataStorage, InitParams params) {
    this.dataStorage = dataStorage;
    this.configurationManager = configurationManager;

    ValueParam ignoreStoredLayoutParam = params.getValueParam("forceIgnoreStoredLayout");
    if (ignoreStoredLayoutParam != null && ignoreStoredLayoutParam.getValue() != null) {
      this.forceIgnoreStoredLayout = Boolean.parseBoolean(ignoreStoredLayoutParam.getValue().toLowerCase());
    }
  }

  @Override
  public void start() {
    for (DynamicPortalLayoutMatcherPlugin dynamicLayoutMatcherPlugin : getDynamicLayoutMatchers()) {
      dynamicLayoutMatcherPlugin.init(this.configurationManager);
    }
  }

  @Override
  public void stop() {
    // Nothing to stop
  }

  /**
   * A method to inject {@link DynamicPortalLayoutMatcherPlugin} by IOC.
   * 
   * @param dynamicPortalLayoutMatcherPlugin plugin of type
   *          {@link ComponentPlugin} to inject
   */
  public void addDynamicLayoutMatcher(DynamicPortalLayoutMatcherPlugin dynamicPortalLayoutMatcherPlugin) {
    if (dynamicPortalLayoutMatcherPlugin == null) {
      throw new IllegalArgumentException("plugin is null");
    }

    String matcherName = dynamicPortalLayoutMatcherPlugin.getName();
    if (dynamicLayoutMatcherPlugins.containsKey(matcherName)) {
      LOG.info("Redefine an existing matcher with name {}", matcherName);

      // Delete old key to use new defined order switch defined component plugin
      // priority
      dynamicLayoutMatcherPlugins.remove(matcherName);
    }
    dynamicLayoutMatcherPlugins.put(matcherName, dynamicPortalLayoutMatcherPlugin);
  }

  /**
   * @return unmodifiable {@link List} of
   *         {@link DynamicPortalLayoutMatcherPlugin} ordered by
   *         {@link ComponentPlugin#getPriority()}). The returned elements are
   *         unique using {@link ComponentPlugin#getName()}).
   */
  public List<DynamicPortalLayoutMatcherPlugin> getDynamicLayoutMatchers() {
    if (dynamicLayoutMatchers.isEmpty()) {
      dynamicLayoutMatchers.addAll(dynamicLayoutMatcherPlugins.values());
      Collections.reverse(dynamicLayoutMatchers);
    }
    return Collections.unmodifiableList(dynamicLayoutMatchers);
  }

  /**
   * Computes the Portal Layout container to use when displaying site designated
   * with siteKey parameter. The Portal Layout container is computed using
   * components plugins of type {@link DynamicPortalLayoutMatcherPlugin} that
   * will use matchers inheriting from {@link DynamicPortalLayoutMatcher} to
   * produce the portal layout to use for the currently diplaying siteKey.
   * 
   * @param siteKey mandatory site key of site to display
   * @param currentPortalSiteName last displayed site of type PORTAL
   * @return {@link PortalConfig} with modified dynamic layout if matching, else
   *         the stored {@link PortalConfig} of siteKey as it is.
   * @throws Exception when an error occurs while retrieving
   *           {@link PortalConfig} of current portal site or current site
   *           designated by siteKey parameter.
   */
  public PortalConfig getPortalConfigWithDynamicLayout(SiteKey siteKey, String currentPortalSiteName) throws Exception {
    if (siteKey == null) {
      throw new IllegalArgumentException("siteKey is mandatory");
    }

    PortalConfig storedPortalConfig = dataStorage.getPortalConfig(siteKey.getTypeName(), siteKey.getName());
    if (storedPortalConfig == null) {
      return null;
    }

    if (!storedPortalConfig.isDefaultLayout() && !forceIgnoreStoredLayout) {
      return storedPortalConfig;
    }

    // Add a specific warning when using this API with an empty current site
    // name to help detecting troubleshoot
    if (StringUtils.isBlank(currentPortalSiteName)) {
      LOG.warn("Current portal site name is blank, thus return stored portal config");
      return storedPortalConfig;
    }

    if (StringUtils.equals(currentPortalSiteName, siteKey.getName())) {
      return storedPortalConfig;
    }

    PortalConfig currentSitePortalConfig = dataStorage.getPortalConfig(SiteType.PORTAL.getName(), currentPortalSiteName);
    if (currentSitePortalConfig == null) {
      return storedPortalConfig;
    }

    for (DynamicPortalLayoutMatcherPlugin dynamicLayoutMatcherPlugin : getDynamicLayoutMatchers()) {
      PortalConfig dynamicPortalConfig = dynamicLayoutMatcherPlugin.getPortalConfigWithDynamicLayout(siteKey,
                                                                                                     storedPortalConfig,
                                                                                                     currentSitePortalConfig);
      if (dynamicPortalConfig != null) {
        return dynamicPortalConfig;
      }
    }
    return storedPortalConfig;
  }

}
