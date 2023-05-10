package org.exoplatform.portal.mop.service;

import org.exoplatform.portal.mop.State;
import org.exoplatform.portal.mop.storage.DescriptionStorage;

import java.util.Locale;
import java.util.Map;

public class DescriptionServiceImpl implements DescriptionService {

  private DescriptionStorage descriptionStorage;

  public DescriptionServiceImpl(DescriptionStorage descriptionStorage) {
    this.descriptionStorage = descriptionStorage;
  }

  @Override
  public Map<Locale, State> getDescriptions(String id) {
    return descriptionStorage.getDescriptions(id);
  }

  @Override
  public void setDescriptions(String id, Map<Locale, State> descriptions) {
    descriptionStorage.setDescriptions(id, descriptions);
  }
}
