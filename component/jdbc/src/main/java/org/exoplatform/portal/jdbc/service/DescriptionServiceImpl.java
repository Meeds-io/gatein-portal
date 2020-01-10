/*
 * Copyright (C) 2016 eXo Platform SAS.
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

package org.exoplatform.portal.jdbc.service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.exoplatform.commons.utils.I18N;
import org.exoplatform.portal.jdbc.dao.DescriptionDAO;
import org.exoplatform.portal.jdbc.entity.DescriptionEntity;
import org.exoplatform.portal.jdbc.entity.DescriptionState;
import org.exoplatform.portal.mop.Described;
import org.exoplatform.portal.mop.description.DescriptionService;

public class DescriptionServiceImpl implements DescriptionService {

  private DescriptionDAO descDAO;

  public DescriptionServiceImpl(DescriptionDAO descDAO) {
    this.descDAO = descDAO;
  }

  public Described.State resolveDescription(String id, Locale locale) throws NullPointerException {
    if (id == null) {
      throw new NullPointerException("No null id accepted");
    }
    if (locale == null) {
      throw new NullPointerException("No null locale accepted");
    }
    DescriptionEntity desc = descDAO.getByRefId(id);

    if (desc != null) {
      for (Locale current = locale; current != null; current = parent(current)) {
        DescriptionState state = desc.getLocalized().get(I18N.toTagIdentifier(current));
        if (state != null) {
          Described.State result = new Described.State(state.getName(), state.getDescription());
          return result;
        }
      }
    }
    return null;
  }

  public Described.State resolveDescription(String id, Locale locale2, Locale locale1) throws NullPointerException {
    Described.State state = resolveDescription(id, locale1);
    if (state == null && locale2 != null) {
      state = resolveDescription(id, locale2);
    }
    return state;
  }

  public Described.State getDescription(String id, Locale locale) {
    if (id == null) {
      throw new NullPointerException("No null id accepted");
    }
    if (locale == null) {
      throw new NullPointerException("No null locale accepted");
    }

    DescriptionEntity desc = descDAO.getByRefId(id);

    if (desc != null) {
      DescriptionState state = desc.getLocalized().get(I18N.toTagIdentifier(locale));
      if (state != null) {
        Described.State result = new Described.State(state.getName(), state.getDescription());
        return result;
      }
    }
    return null;
  }

  public void setDescription(String id, Locale locale, Described.State description) {
    if (id == null) {
      throw new NullPointerException("No null id accepted");
    }
    if (locale == null) {
      throw new NullPointerException("No null locale accepted");
    }
    if (locale.getLanguage().length() == 0) {
      throw new IllegalArgumentException("No language set on locale");
    }
    if (locale.getVariant().length() > 0) {
      throw new IllegalArgumentException("No variant cab be set on locale");
    }

    Map<String, DescriptionState> state = new HashMap<String, DescriptionState>();

    DescriptionEntity entity = descDAO.getByRefId(id);
    if (entity != null) {
      state.putAll(entity.getLocalized());
    }

    if (description != null) {
      state.put(I18N.toTagIdentifier(locale), new DescriptionState(description.getName(), description.getDescription()));
    } else {
      state.remove(I18N.toTagIdentifier(locale));
    }

    descDAO.saveDescriptions(id, state);
  }

  public Described.State getDescription(String id) {
    if (id == null) {
      throw new NullPointerException("No null id accepted");
    }

    DescriptionEntity desc = descDAO.getByRefId(id);

    if (desc != null) {
      DescriptionState state = desc.getState();
      if (state != null) {
        Described.State result = new Described.State(state.getName(), state.getDescription());
        return result;
      }
    }
    return null;
  }

  public void setDescription(String id, Described.State description) {
    if (id == null) {
      throw new NullPointerException("No null id accepted");
    }

    if (description != null) {
      descDAO.saveDescription(id, new DescriptionState(description.getName(), description.getDescription()));
    } else {
      descDAO.deleteByRefId(id);
    }
  }

  public Map<Locale, Described.State> getDescriptions(String id) {
    if (id == null) {
      throw new NullPointerException("No null id accepted");
    }
    Map<Locale, Described.State> names = null;

    DescriptionEntity desc = descDAO.getByRefId(id);
    if (desc != null) {
      Map<String, DescriptionState> localized = desc.getLocalized();

      if (localized != null) {
        names = new HashMap<Locale, Described.State>(localized.size());
        for (String locale : localized.keySet()) {
          DescriptionState state = localized.get(locale);
          names.put(I18N.parseTagIdentifier(locale), new Described.State(state.getName(), state.getDescription()));
        }
      }
    }
    return names;
  }

  public void setDescriptions(String id, Map<Locale, Described.State> descriptions) {
    if (id == null) {
      throw new NullPointerException("No null id accepted");
    }
    if (descriptions != null) {
      Map<String, DescriptionState> localized = new HashMap<String, DescriptionState>(descriptions.size());
      for (Locale locale : descriptions.keySet()) {
        if (locale.getLanguage().length() == 0) {
          throw new IllegalArgumentException("No language set on locale");
        }
        if (locale.getVariant().length() > 0) {
          throw new IllegalArgumentException("No variant cab be set on locale");
        }

        Described.State state = descriptions.get(locale);
        localized.put(I18N.toTagIdentifier(locale), new DescriptionState(state.getName(), state.getDescription()));
      }

      descDAO.saveDescriptions(id, localized);
    } else {
      descDAO.deleteByRefId(id);
    }
  }

  private static Locale parent(Locale locale) {
    if (locale.getVariant() != null && !locale.getVariant().isEmpty()) {
      return new Locale(locale.getLanguage(), locale.getCountry());
    } else if (locale.getCountry() != null && !locale.getCountry().isEmpty()) {
      return new Locale(locale.getLanguage());
    } else {
      return null;
    }
  }
}
