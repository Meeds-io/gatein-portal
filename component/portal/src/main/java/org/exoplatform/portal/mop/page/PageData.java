package org.exoplatform.portal.mop.page;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;
import org.gatein.mop.api.workspace.Page;
import org.gatein.mop.api.workspace.Site;

import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.Utils;

/**
 * An immutable page data class.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public class PageData implements Serializable {

  private static final long serialVersionUID = -2859289738034643799L;

  /** Useful. */
  static final PageData     EMPTY            = new PageData();

  /** . */
  final PageKey             key;

  /** . */
  final String              id;

  /** . */
  final PageState           state;

  public PageData(PageKey key, String id, PageState state) {
    this.key = key;
    this.id = id;
    this.state = state;
  }

  private PageData() {
    this.key = null;
    this.id = null;
    this.state = null;
  }

  protected Object readResolve() {
    if (key == null && state == null && id == null) {
      return EMPTY;
    } else {
      return this;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof PageData))
      return false;

    PageData pageData = (PageData) o;

    if (key != null ? !key.equals(pageData.key) : pageData.key != null)
      return false;
    return StringUtils.equals(id, pageData.id);
  }

  @Override
  public int hashCode() {
    int result = key != null ? key.hashCode() : 0;
    result = 31 * result + (id != null ? id.hashCode() : 0);
    return result;
  }
}
