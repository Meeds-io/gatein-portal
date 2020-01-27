package org.exoplatform.portal.jdbc.entity;

import java.io.Serializable;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public abstract class ComponentEntity implements Serializable {
  private static final long serialVersionUID = 1181255637761644181L;

  public JSONObject toJSON() {
    JSONObject obj = new JSONObject();
    obj.put("type", getType().name());
    obj.put("id", getId());
    return obj;
  }

  public abstract Long getId();

  public abstract void setId(Long id);

  public abstract TYPE getType();

  public enum TYPE {
    SITE,
    PAGE,
    CONTAINER,
    WINDOW
  }

  protected static final String getJSONString(JSONObject jsonObject) {
    return jsonObject.toJSONString();
  }

  protected static final String getJSONString(JSONArray jsonArray) {
    return jsonArray.toJSONString();
  }
}
