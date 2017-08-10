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
@Table(name = "PORTAL_TOKENS")
@NamedQueries({
  @NamedQuery(name = "GadgetTokenEntity.deleteByKey", query = "DELETE FROM GadgetTokenEntity g WHERE g.userId = :userId AND g.gadgetUri = :gadgetUri AND g.moduleId = :moduleId AND g.tokenName = :tokenName AND g.serviceName = :serviceName"),
  @NamedQuery(name = "GadgetTokenEntity.findByKey", query = "SELECT g FROM GadgetTokenEntity g WHERE g.userId = :userId AND g.gadgetUri = :gadgetUri AND g.moduleId = :moduleId AND g.tokenName = :tokenName AND g.serviceName = :serviceName") })
public class GadgetTokenEntity implements Serializable {

  private static final long serialVersionUID = -7178403295319561214L;

  @Id
  @SequenceGenerator(name = "SEQ_PORTAL_TOKENS_ID", sequenceName = "SEQ_PORTAL_TOKENS_ID")
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_PORTAL_TOKENS_ID")
  @Column(name = "TOKEN_ID")
  private Long              id;

  @Column(name = "USER_ID", length = 100)
  private String            userId;
  
  @Column(name = "GADGET_URI", length = 500)
  private String            gadgetUri;
  
  @Column(name = "MODULE_ID")
  private long            moduleId;
  
  @Column(name = "TOKEN_NAME", length = 200)
  private String            tokenName;
  
  @Column(name = "SERVICE_NAME", length = 500)
  private String            serviceName;
  
  @Column(name = "ACCESS_TOKEN", length = 500)
  private String            accessToken;

  @Column(name = "TOKEN_SECRET", length = 500)
  private String            tokenSecret;

  @Column(name = "SESSION_HANDLE", length = 500)
  private String            sessionHandle;

  @Column(name = "EXPIRED_TIME")
  private long              tokenExpireMillis;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public String getTokenSecret() {
    return tokenSecret;
  }

  public void setTokenSecret(String tokenSecret) {
    this.tokenSecret = tokenSecret;
  }

  public String getSessionHandle() {
    return sessionHandle;
  }

  public void setSessionHandle(String sessionHandle) {
    this.sessionHandle = sessionHandle;
  }

  public long getTokenExpireMillis() {
    return tokenExpireMillis;
  }

  public void setTokenExpireMillis(long tokenExpireMillis) {
    this.tokenExpireMillis = tokenExpireMillis;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getGadgetUri() {
    return gadgetUri;
  }

  public void setGadgetUri(String gadgetUri) {
    this.gadgetUri = gadgetUri;
  }

  public long getModuleId() {
    return moduleId;
  }

  public void setModuleId(long moduleId) {
    this.moduleId = moduleId;
  }

  public String getTokenName() {
    return tokenName;
  }

  public void setTokenName(String tokenName) {
    this.tokenName = tokenName;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }
  
}
