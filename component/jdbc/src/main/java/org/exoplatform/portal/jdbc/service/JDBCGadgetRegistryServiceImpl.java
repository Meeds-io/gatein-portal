package org.exoplatform.portal.jdbc.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.ModulePrefs;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;

import org.exoplatform.application.gadget.Gadget;
import org.exoplatform.application.gadget.GadgetImporter;
import org.exoplatform.application.gadget.impl.GadgetRegistryServiceImpl;
import org.exoplatform.commons.chromattic.ChromatticManager;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.portal.jdbc.dao.GadgetDAO;
import org.exoplatform.portal.jdbc.entity.GadgetEntity;

public class JDBCGadgetRegistryServiceImpl extends GadgetRegistryServiceImpl {

  /** . */
  private final Logger        log                     = LoggerFactory.getLogger(JDBCGadgetRegistryServiceImpl.class);

  /** . */
  private static final String DEFAULT_DEVELOPER_GROUP = "/platform/administrators";

  /** . */
  private String              country;

  /** . */
  private String              language;

  /** . */
  private String              moduleId;

  /** . */
  private String              hostName;

  private GadgetDAO           gadgetDAO;

  public JDBCGadgetRegistryServiceImpl(InitParams params, GadgetDAO gadgetDAO) throws Exception {
    super(new ChromatticManager(null), params);
    this.gadgetDAO = gadgetDAO;

    //
    String gadgetDeveloperGroup = null;
    String country = null;
    String language = null;
    String moduleId = null;
    String hostName = null;
    if (params != null) {
      PropertiesParam properties = params.getPropertiesParam("developerInfo");
      gadgetDeveloperGroup = properties != null ? properties.getProperty("developer.group") : null;
      ValueParam gadgetCountry = params.getValueParam("gadgets.country");
      country = gadgetCountry != null ? gadgetCountry.getValue() : null;
      ValueParam gadgetLanguage = params.getValueParam("gadgets.language");
      language = gadgetLanguage != null ? gadgetLanguage.getValue() : null;
      ValueParam gadgetModuleId = params.getValueParam("gadgets.moduleId");
      moduleId = gadgetModuleId != null ? gadgetModuleId.getValue() : null;
      ValueParam gadgetHostName = params.getValueParam("gadgets.hostName");
      hostName = gadgetHostName != null ? gadgetHostName.getValue() : null;
    }

    //
    if (gadgetDeveloperGroup == null) {
      gadgetDeveloperGroup = DEFAULT_DEVELOPER_GROUP;
    }

    //
    this.country = country;
    this.language = language;
    this.moduleId = moduleId;
    this.hostName = hostName;
  }

  public void deploy(Iterable<GadgetImporter> gadgets) {
    for (GadgetImporter importer : gadgets) {
      try {
        new DeployTask(importer).call();
      } catch (Exception e) {
        log.error("Could not process gadget file " + importer, e);
      }
    }
  }

  @Override
  public List<Gadget> getAllGadgets() throws Exception {
    return getAllGadgets(null);
  }

  @Override
  public List<Gadget> getAllGadgets(Comparator<Gadget> sortComparator) {
    List<Gadget> gadgets = new ArrayList<Gadget>();
    for (GadgetEntity def : gadgetDAO.findAll()) {
      Gadget gadget = buildGadget(def);
      gadgets.add(gadget);
    }
    if (sortComparator != null) {
      Collections.sort(gadgets, sortComparator);
    }
    return gadgets;
  }

  @Override
  public String getCountry() {
    return this.country;
  }

  @Override
  public Gadget getGadget(String name) {
    GadgetEntity gadgetEntity = gadgetDAO.find(name);
    if (gadgetEntity != null) {
      return buildGadget(gadgetEntity);
    } else {
      return null;
    }
  }

  @Override
  public String getGadgetURL(String name) {
    String url = null;
    GadgetEntity gadgetEntity = gadgetDAO.find(name);

    if (gadgetEntity != null) {
      if (gadgetEntity.isLocal()) {
        url = "/" + PortalContainer.getCurrentRestContextName() + "/gadget/" + gadgetEntity.getId();
      } else {
        url = gadgetEntity.getUrl();
      }
    }

    return url;
  }

  @Override
  public String getHostName() {
    return this.hostName;
  }

  @Override
  public String getLanguage() {
    return this.language;
  }

  @Override
  public String getModuleId() {
    return this.moduleId;
  }

  @Override
  public boolean isGadgetDeveloper(String arg0) {
    return PropertyManager.isDevelopping();
  }

  @Override
  public void removeGadget(String name) {
    GadgetEntity gadgetEntity = gadgetDAO.find(name);
    if (gadgetEntity == null) {
      throw new IllegalArgumentException("No such gadget " + name);
    } else {
      gadgetDAO.delete(gadgetEntity);
    }
  }

  @Override
  public void saveGadget(Gadget gadget) throws Exception {
    if (gadget == null) {
      throw new NullPointerException();
    }

    //
    GadgetEntity gadgetEntity = gadgetDAO.find(gadget.getName());

    //
    if (gadgetEntity == null) {
      gadgetEntity = new GadgetEntity();
      gadgetEntity.setName(gadget.getName());
      if (gadget.isLocal()) {
        gadgetEntity.setLocal(true);
        gadgetEntity.setSource("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Module><ModulePrefs title=\"\" />"
            + "<Content type=\"html\"> <![CDATA[]]></Content></Module>");
      } else {
        gadgetEntity.setLocal(false);
        gadgetEntity.setUrl(gadget.getUrl());
      }

      gadgetDAO.create(gadgetEntity);
    }

    if (!gadget.isLocal()) {
      gadgetEntity.setDescription(gadget.getDescription());
      gadgetEntity.setReferenceUrl(gadget.getReferenceUrl());
      gadgetEntity.setTitle(gadget.getTitle());
      gadgetEntity.setThumbnail(gadget.getThumbnail());
      gadgetDAO.update(gadgetEntity);
    }
  }

  private Gadget buildGadget(GadgetEntity gadgetEntity) {
    Gadget gadget = new Gadget();

    //
    if (gadgetEntity.isLocal()) {
      try {
        String gadgetName = gadgetEntity.getName();
        String content = gadgetEntity.getSource();
        GadgetSpec gadgetSpec = new GadgetSpec(Uri.parse(getGadgetURL(gadgetName)), content);
        ModulePrefs prefs = gadgetSpec.getModulePrefs();

        String title = prefs.getDirectoryTitle();
        if (title == null || title.trim().length() < 1) {
          title = prefs.getTitle();
        }
        if (title == null || title.trim().length() < 1) {
          title = gadgetName;
        }
        gadget.setName(gadgetEntity.getName());
        gadget.setDescription(prefs.getDescription());
        gadget.setLocal(true);
        gadget.setTitle(title);
        gadget.setReferenceUrl(prefs.getTitleUrl().toString());
        gadget.setThumbnail(prefs.getThumbnail().toString());
        gadget.setUrl(getGadgetURL(gadgetName));
      } catch (Exception ex) {
        log.error("Error while loading the content of local gadget " + gadgetEntity.getName(), ex);
      }
    } else {
      gadget.setName(gadgetEntity.getName());
      gadget.setDescription(gadgetEntity.getDescription());
      gadget.setLocal(false);
      gadget.setTitle(gadgetEntity.getTitle());
      gadget.setReferenceUrl(gadgetEntity.getReferenceUrl());
      gadget.setThumbnail(gadgetEntity.getThumbnail());
      gadget.setUrl(gadgetEntity.getUrl());
    }
    //
    return gadget;
  }

  private class DeployTask implements Callable<Boolean> {

    private final GadgetImporter importer;

    private DeployTask(GadgetImporter importer) {
      this.importer = importer;
    }

    public Boolean call() throws Exception {
      boolean done = true;
//      try {
//        if (getRegistry().getGadget(importer.getGadgetName()) == null) {
//          GadgetDefinition def = getRegistry().addGadget(importer.getGadgetName());
//          importer.doImport(def);
//        } else {
//          log.debug("Will not import existing gagdet " + importer.getGadgetName());
//        }
//      } catch (Exception e) {
//        done = false;
//      }
      return done;
    }
  }
}
