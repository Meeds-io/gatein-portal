package org.exoplatform.portal.rest.model;

public class UserRestEntity {
  private String userName;

  private String firstName;

  private String lastName;

  private String fullName;

  private String email;

  private String password;

  private boolean isInternal;

  private String createdDate;

  private boolean enabled;

  private boolean platformAdministrator;

  public UserRestEntity() {
  }

  public UserRestEntity(String userName, String firstName, String lastName, String fullName, String email, boolean enabled, boolean platformAdministrator, boolean isInternal, String createdDate) {
    this.userName = userName;
    this.firstName = firstName;
    this.lastName = lastName;
    this.fullName = fullName;
    this.email = email;
    this.enabled = enabled;
    this.enabled = enabled;
    this.platformAdministrator = platformAdministrator;
    this.isInternal = isInternal;
    this.createdDate = createdDate;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public boolean getIsInternal() { return isInternal; }

  public void setIsInternal(boolean isInternal) { this.isInternal = isInternal; }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getCreatedDate() { return createdDate; }

  public void setCreatedDate(String createdDate) { this.createdDate = createdDate; }

  public boolean isPlatformAdministrator() {
    return platformAdministrator;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public void setPlatformAdministrator(boolean platformAdministrator) {
    this.platformAdministrator = platformAdministrator;
  }
}
