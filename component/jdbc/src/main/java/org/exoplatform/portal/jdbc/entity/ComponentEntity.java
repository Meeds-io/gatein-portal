package org.exoplatform.portal.jdbc.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.json.simple.JSONObject;

import org.exoplatform.commons.api.persistence.ExoEntity;

@Entity
@ExoEntity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class ComponentEntity implements Serializable {
  private static final long serialVersionUID = 1181255637761644181L;

  @Id
  @Column(name = "ID", length = 200)
  private String            id;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public JSONObject toJSON() {
    JSONObject obj = new JSONObject();
    obj.put("type", getType().name());
    obj.put("id", getId());
    return obj;
  }

  public abstract TYPE getType();

  public static enum TYPE {
    SITE, PAGE, CONTAINER, WINDOW
  }
}
