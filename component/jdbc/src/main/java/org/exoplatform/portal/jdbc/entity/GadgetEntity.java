package org.exoplatform.portal.jdbc.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.exoplatform.commons.api.persistence.ExoEntity;

@Entity
@ExoEntity
@Table(name = "PORTAL_GADGETS")
@NamedQueries({
    @NamedQuery(name = "GadgetEntity.find", query = "SELECT g FROM GadgetEntity g WHERE g.name = :name") })
public class GadgetEntity implements Serializable {

  private static final long serialVersionUID = -7234685756984011687L;

  @Id
  @SequenceGenerator(name = "SEQ_PORTAL_GADGETS_ID", sequenceName = "SEQ_PORTAL_GADGETS_ID")
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_PORTAL_GADGETS_ID")
  @Column(name = "GADGET_ID")
  private Long    id;

  @Column(name = "NAME", length = 200)
  private String  name;

  @Column(name = "URL", length = 500)
  private String  url;

  @Column(name = "TITLE", length = 200)
  private String  title;

  @Column(name = "DESCRIPTION", length = 2000)
  private String  description;

  @Column(name = "REF_URL", length = 500)
  private String  referenceUrl;

  @Column(name = "THUMBNAIL", length = 200)
  private String  thumbnail;

  @Column(name = "IS_LOCAL")
  private boolean isLocal = true;

  @Column(name = "SOURCE", length = 50000)
  private String  source;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getReferenceUrl() {
    return referenceUrl;
  }

  public void setReferenceUrl(String referenceUrl) {
    this.referenceUrl = referenceUrl;
  }

  public String getThumbnail() {
    return thumbnail;
  }

  public void setThumbnail(String thumbnail) {
    this.thumbnail = thumbnail;
  }

  public boolean isLocal() {
    return isLocal;
  }

  public void setLocal(boolean isLocal) {
    this.isLocal = isLocal;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }
}
