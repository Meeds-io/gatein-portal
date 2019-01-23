package org.exoplatform.portal.rest;

import java.util.List;

public class UserRestEntity {
  private String userName;

  private String firstName;

  private String lastName;

  private String fullName;

  private String email;

  private boolean platformAdministrator;

  public UserRestEntity(String userName, String firstName, String lastName, String fullName, String email, boolean platformAdministrator) {
    this.userName = userName;
    this.firstName = firstName;
    this.lastName = lastName;
    this.fullName = fullName;
    this.email = email;
    this.platformAdministrator = platformAdministrator;
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

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public boolean isPlatformAdministrator() {
    return platformAdministrator;
  }

  public void setPlatformAdministrator(boolean platformAdministrator) {
    this.platformAdministrator = platformAdministrator;
  }
}
