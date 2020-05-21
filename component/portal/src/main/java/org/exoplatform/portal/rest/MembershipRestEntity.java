package org.exoplatform.portal.rest;

public class MembershipRestEntity {
  private String id;

  private String membershipType;

  private String groupId;

  private String groupLabel;

  private String userName;

  private String userFullName;

  public MembershipRestEntity(String id,
                              String membershipType,
                              String groupId,
                              String groupLabel,
                              String userName,
                              String userFullName) {
    this.id = id;
    this.membershipType = membershipType;
    this.groupId = groupId;
    this.groupLabel = groupLabel;
    this.userName = userName;
    this.userFullName = userFullName;
  }

  public String getMembershipType() {
    return membershipType;
  }

  public void setMembershipType(String membershipType) {
    this.membershipType = membershipType;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public String getGroupLabel() {
    return groupLabel;
  }

  public void setGroupLabel(String groupLabel) {
    this.groupLabel = groupLabel;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getUserFullName() {
    return userFullName;
  }

  public void setUserFullName(String userFullName) {
    this.userFullName = userFullName;
  }

}
