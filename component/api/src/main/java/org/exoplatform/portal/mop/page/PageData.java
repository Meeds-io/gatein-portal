package org.exoplatform.portal.mop.page;

import java.io.Serializable;

import lombok.Data;

/**
 * An immutable page data class.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
@Data
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

}
