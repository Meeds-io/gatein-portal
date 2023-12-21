package org.exoplatform.portal.resource;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer.PortalContainerPostInitTask;
import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;

/**
 * This is a Component Plugin for {@link SkinService} in order to add available skins and default skin by
 * configuration. The available skins are computed after {@link PortalContainer} startup by a
 * {@link PortalContainerPostInitTask} injected by {@link GateInSkinConfigDeployer}.
 * This plugin ensures that all real available skins are injected into {@link SkinService} at
 * {@link PortalContainer} startup to avoid falling back to Default Skin.
 * 
 * @see <a href="https://jira.exoplatform.org/browse/PLF-7851">PLF-7851</a>
 */
public class SkinConfigPlugin extends BaseComponentPlugin {
  private static final String DEFAULT_SKIN_PARAM               = "skin.default.name";

  private static final String ADDITIONAL_AVAILABLE_SKINS_PARAM = "additional.skins.available.name";

  private String              defaultSkin                      = null;

  private List<String>        availableSkins                   = new ArrayList<>();

  public SkinConfigPlugin(InitParams params) {
    if (params != null) {
      if (params.containsKey(DEFAULT_SKIN_PARAM) && StringUtils.isNotBlank(params.getValueParam(DEFAULT_SKIN_PARAM).getValue())) {
        defaultSkin = params.getValueParam(DEFAULT_SKIN_PARAM).getValue();
      }
      if (params.containsKey(ADDITIONAL_AVAILABLE_SKINS_PARAM)
          && params.getValuesParam(ADDITIONAL_AVAILABLE_SKINS_PARAM).getValues() != null
          && !params.getValuesParam(ADDITIONAL_AVAILABLE_SKINS_PARAM).getValues().isEmpty()) {
        List<String> skinNames = params.getValuesParam(ADDITIONAL_AVAILABLE_SKINS_PARAM).getValues();
        for (String skinName : skinNames) {
          if (StringUtils.isNotBlank(skinName)) {
            availableSkins.add(skinName);
          }
        }
      }
    }
  }

  public List<String> getAvailableSkins() {
    return availableSkins;
  }

  public String getDefaultSkin() {
    return defaultSkin;
  }
}
