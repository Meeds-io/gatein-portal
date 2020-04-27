package org.picketlink.idm.impl.configuration;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.commons.utils.PropertyManager;
import org.picketlink.idm.impl.api.attribute.IdentityObjectAttributeMetaDataImpl;
import org.picketlink.idm.impl.configuration.metadata.IdentityObjectTypeMetaDataImpl;
import org.picketlink.idm.impl.configuration.metadata.IdentityStoreConfigurationMetaDataImpl;
import org.picketlink.idm.spi.configuration.IdentityConfigurationContextRegistry;
import org.picketlink.idm.spi.configuration.IdentityStoreConfigurationContext;
import org.picketlink.idm.spi.configuration.metadata.IdentityConfigurationMetaData;
import org.picketlink.idm.spi.configuration.metadata.IdentityObjectAttributeMetaData;
import org.picketlink.idm.spi.configuration.metadata.IdentityObjectTypeMetaData;
import org.picketlink.idm.spi.configuration.metadata.IdentityStoreConfigurationMetaData;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is a wrapper for PicketLink IdentityStoreConfigurationContext class.
 * It allows to change/enrich the default configuration dynamically at startup.
 * It is used to:
 * * add custom attributes for users and groups defined by properties:
 * ** exo.ldap.users.attributes.custom.names: comma-separated list of names of new custom attributes for users
 * ** exo.ldap.groups.attributes.custom.names: comma-separated list of names of new custom attributes for groups
 * ** exo.ldap.{users|groups}.attributes.custom.{name}.mapping: name of the LDAP attribute mapped with the custom attribute (defaults to the attribute name)
 * ** exo.ldap.{users|groups}.attributes.custom.{name}.type: type of the LDAP attribute mapped with the custom attribute, can be "text" or "binary" (defaults to "text")
 * ** exo.ldap.{users|groups}.attributes.custom.{name}.isRequired: is the custom attribute mandatory ? (defaults to false)
 * ** exo.ldap.{users|groups}.attributes.custom.{name}.isMultivalued: is the custom attribute multi-valued ? (defaults to false)
 */
public class ExoIdentityStoreConfigurationContext implements IdentityStoreConfigurationContext {

  public static final String LDAP_USERS_ATTRIBUTES_NAMES_PROP = "exo.ldap.users.attributes.custom.names";

  public static final String LDAP_GROUPS_ATTRIBUTES_NAMES_PROP = "exo.ldap.groups.attributes.custom.names";

  public static final String LDAP_USERS_ATTRIBUTES_PROPS_PREFIX = "exo.ldap.users.attributes.";

  public static final String LDAP_GROUPS_ATTRIBUTES_PROPS_PREFIX = "exo.ldap.groups.attributes.";

  private IdentityStoreConfigurationContext identityStoreConfigurationContext;

  public ExoIdentityStoreConfigurationContext(IdentityStoreConfigurationContext identityStoreConfigurationContext) {
    this.identityStoreConfigurationContext = identityStoreConfigurationContext;
  }

  @Override
  public IdentityStoreConfigurationMetaData getStoreConfigurationMetaData() {
    IdentityStoreConfigurationMetaDataImpl storeConfigurationMetaData = (IdentityStoreConfigurationMetaDataImpl) this.identityStoreConfigurationContext.getStoreConfigurationMetaData();

    List<IdentityObjectTypeMetaData> supportedIdentityTypes = storeConfigurationMetaData.getSupportedIdentityTypes();

    List<IdentityObjectTypeMetaData> enrichedSupportedIdentityTypes = new ArrayList<>(supportedIdentityTypes.size());
    for (IdentityObjectTypeMetaData supportedIdentityType : supportedIdentityTypes) {

      List<IdentityObjectAttributeMetaData> enrichedAttributes = enrichAttributes(supportedIdentityType);

      ((IdentityObjectTypeMetaDataImpl) supportedIdentityType).setAttributes(enrichedAttributes);

      enrichedSupportedIdentityTypes.add(supportedIdentityType);
    }

    storeConfigurationMetaData.setSupportedIdentityTypes(enrichedSupportedIdentityTypes);

    return storeConfigurationMetaData;
  }

  protected List<IdentityObjectAttributeMetaData> enrichAttributes(IdentityObjectTypeMetaData supportedIdentityType) {
    // Add custom attributes
    String customNamesPropertyName = LDAP_USERS_ATTRIBUTES_NAMES_PROP;
    String attributesPropertyPrefix = LDAP_USERS_ATTRIBUTES_PROPS_PREFIX;
    if("GROUP".equals(supportedIdentityType.getName())) {
      customNamesPropertyName = LDAP_GROUPS_ATTRIBUTES_NAMES_PROP;
      attributesPropertyPrefix = LDAP_GROUPS_ATTRIBUTES_PROPS_PREFIX;
    }

    List<IdentityObjectAttributeMetaData> attributes = supportedIdentityType.getAttributes();

    String customAttributesNamesProperty = PropertyManager.getProperty(customNamesPropertyName);
    if(StringUtils.isNotBlank(customAttributesNamesProperty)) {
      //
      String[] customAttributesNames = customAttributesNamesProperty.split(",");
      for(String name : customAttributesNames) {
        String mapping = PropertyManager.getProperty(attributesPropertyPrefix + name + ".mapping");
        String type = PropertyManager.getProperty(attributesPropertyPrefix + name + ".type");
        String isRequired = PropertyManager.getProperty(attributesPropertyPrefix + name + ".isRequired");
        String isMultivalued = PropertyManager.getProperty(attributesPropertyPrefix + name + ".isMultivalued");

        IdentityObjectAttributeMetaData attribute = new IdentityObjectAttributeMetaDataImpl(name,
                mapping != null ? mapping : name,
                type != null ? type : IdentityObjectAttributeMetaData.TEXT_TYPE,
                true,
                isMultivalued != null ? Boolean.valueOf(isMultivalued) : false,
                isRequired != null ? Boolean.valueOf(isRequired) : false,
                true);

        attributes.add(attribute);
      }
    }

    return attributes;
  }

  @Override
  public IdentityConfigurationMetaData getConfigurationMetaData() {
    return this.identityStoreConfigurationContext.getConfigurationMetaData();
  }

  @Override
  public IdentityConfigurationContextRegistry getConfigurationRegistry() {
    return this.identityStoreConfigurationContext.getConfigurationRegistry();
  }
}
