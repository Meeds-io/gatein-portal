package org.exoplatform.portal.mop.page;

import java.io.Serializable;
import java.util.*;

import org.exoplatform.commons.utils.Safe;

import lombok.Getter;
import lombok.Setter;

import org.exoplatform.portal.mop.PageType;

/**
 * An immutable page state class, modifying an existing state should use the
 * {@link Builder} builder class to rebuild a new immutable state object.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public class PageState implements Serializable {

  private static final long serialVersionUID = 7874166775312871923L;

  /** . */
  @Getter
  @Setter
  private String            storageId;

  /** . */
  final String              editPermission;

  /** . */
  final boolean             showMaxWindow;

  /** . */
  @Getter
  final boolean             hideSharedLayout;

  final String              profiles;

  /** . */
  final String              factoryId;

  /** . */
  final String              displayName;

  /** . */
  final String              description;

  /** . */
  final String              type;

  /** . */
  final String              link;

  /** . */
  final List<String>        accessPermissions;

  public PageState(String displayName, // NOSONAR
                   String description,
                   boolean showMaxWindow,
                   String factoryId,
                   List<String> accessPermissions,
                   String editPermission,
                   String type,
                   String link) {
    this(displayName,
         description,
         showMaxWindow,
         false,
         factoryId,
         null,
         accessPermissions,
         editPermission,
         type,
         link);
  }

  public PageState(String displayName, // NOSONAR
                   String description,
                   boolean showMaxWindow,
                   String factoryId,
                   List<String> accessPermissions,
                   String editPermission) {
    this(displayName, description, showMaxWindow, false, factoryId, accessPermissions, editPermission);
  }

  public PageState(String displayName, // NOSONAR
                   String description,
                   boolean showMaxWindow,
                   boolean hideSharedLayout,
                   String factoryId,
                   String profiles,
                   List<String> accessPermissions,
                   String editPermission,
                   String type,
                   String link) {
    this.editPermission = editPermission;
    this.showMaxWindow = showMaxWindow;
    this.hideSharedLayout = hideSharedLayout;
    this.factoryId = factoryId;
    this.profiles = profiles;
    this.displayName = displayName;
    this.description = description;
    this.accessPermissions = accessPermissions;
    this.type = type;
    this.link = link;
  }

  public PageState(String displayName, // NOSONAR
                   String description,
                   boolean showMaxWindow,
                   boolean hideSharedLayout,
                   String factoryId,
                   List<String> accessPermissions,
                   String editPermission) {
    this.editPermission = editPermission;
    this.showMaxWindow = showMaxWindow;
    this.hideSharedLayout = hideSharedLayout;
    this.factoryId = factoryId;
    this.profiles = null;
    this.displayName = displayName;
    this.description = description;
    this.accessPermissions = accessPermissions;
    this.type = PageType.PAGE.name();
    this.link = null;
  }

  public String getEditPermission() {
    return editPermission;
  }

  public boolean getShowMaxWindow() {
    return showMaxWindow;
  }

  public String getFactoryId() {
    return factoryId;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getDescription() {
    return description;
  }

  public String getProfiles() {
    return profiles;
  }

  public List<String> getAccessPermissions() {
    return accessPermissions;
  }

  public String getType() {
    return type;
  }

  public String getLink() {
    return link;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PageState)) {
      return false;
    }
    PageState that = (PageState) o;
    return Safe.equals(editPermission, that.editPermission) && showMaxWindow == that.showMaxWindow
           && Safe.equals(factoryId, that.factoryId)
           && Safe.equals(displayName, that.displayName)
           && Safe.equals(description, that.description)
           && Safe.equals(accessPermissions, that.accessPermissions);
  }

  @Override
  public int hashCode() {
    int result = editPermission != null ? editPermission.hashCode() : 0;
    result = 31 * result + (showMaxWindow ? 1 : 0);
    result = 31 * result + (factoryId != null ? factoryId.hashCode() : 0);
    result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
    result = 31 * result + (description != null ? description.hashCode() : 0);
    result = 31 * result + (accessPermissions != null ? accessPermissions.hashCode() : 0);
    return result;
  }

  public Builder builder() {
    return new Builder(editPermission,
                       showMaxWindow,
                       hideSharedLayout,
                       factoryId,
                       profiles,
                       displayName,
                       description,
                       accessPermissions,
                       type,
                       link);
  }

  public static class Builder {

    /** . */
    private String       editPermission;

    /** . */
    private boolean      showMaxWindow;

    /** . */
    private boolean      hideSharedLayout;

    /** . */
    private String       factoryId;

    private String       profiles;

    /** . */
    private String       displayName;

    /** . */
    private String       description;

    /** . */
    private String       type;

    /** . */
    private String       link;

    /** . */
    private List<String> accessPermissions;

    private Builder(String editPermission, // NOSONAR
                    boolean showMaxWindow,
                    boolean hideSharedLayout,
                    String factoryId,
                    String profiles,
                    String displayName,
                    String description,
                    List<String> accessPermissions,
                    String type,
                    String link) {
      this.editPermission = editPermission;
      this.showMaxWindow = showMaxWindow;
      this.showMaxWindow = hideSharedLayout;
      this.factoryId = factoryId;
      this.profiles = profiles;
      this.displayName = displayName;
      this.description = description;
      this.accessPermissions = accessPermissions;
      this.type = type;
      this.link = link;
    }

    public Builder editPermission(String editPermission) {
      this.editPermission = editPermission;
      return this;
    }

    public Builder accessPermissions(List<String> accessPermissions) {
      this.accessPermissions = accessPermissions;
      return this;
    }

    public Builder accessPermissions(String... accessPermissions) {
      this.accessPermissions = new ArrayList<>(Arrays.asList(accessPermissions));
      return this;
    }

    public Builder showMaxWindow(boolean showMaxWindow) {
      this.showMaxWindow = showMaxWindow;
      return this;
    }

    public Builder hideSharedLayout(boolean hideSharedLayout) {
      this.hideSharedLayout = hideSharedLayout;
      return this;
    }

    public Builder displayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    public Builder profiles(String profiles) {
      this.profiles = profiles;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder factoryId(String factoryId) {
      this.factoryId = factoryId;
      return this;
    }

    public Builder type(String type) {
      this.type = type;
      return this;
    }

    public Builder link(String link) {
      this.link = link;
      return this;
    }

    public PageState build() {
      return new PageState(displayName,
                           description,
                           showMaxWindow,
                           hideSharedLayout,
                           factoryId,
                           profiles,
                           accessPermissions,
                           editPermission,
                           type,
                           link);
    }
  }
}
