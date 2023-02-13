package org.exoplatform.portal.mop.page;

import java.util.List;

import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.QueryResult;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;

/**
 * <p>
 * The page service manages the page objects in GateIn, it focus on the page
 * entities and does not provide access to the underlying page layout associated
 * with the page.
 * </p>
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public interface PageService {

  /**
   * Find and returns a page, if no such page exist, null is returned.
   *
   * @param  key                  the page key
   * @return                      the matching page
   * @throws NullPointerException if the key is null
   * @throws PageServiceException anything that would prevent the operation to
   *                                succeed
   */
  PageContext loadPage(PageKey key) throws NullPointerException, PageServiceException;

  List<PageContext> loadPages(SiteKey siteKey) throws NullPointerException, PageServiceException;

  /**
   * Create, update a page. When the page state is not null, the page will be
   * created or updated depending on whether or not the page already exists.
   *
   * @param  page                 the page
   * @return                      true if the page is not already existed,
   *                              otherwise return false.
   * @throws NullPointerException if the key is null
   * @throws PageServiceException anything that would prevent the operation to
   *                                succeed
   */
  boolean savePage(PageContext page) throws NullPointerException, PageServiceException;

  /**
   * Destroy a page.
   *
   * @param  key                  the page key
   * @return                      true when the page was destroyed
   * @throws PageServiceException anything that would prevent the operation to
   *                                succeed
   */
  boolean destroyPage(PageKey key) throws PageServiceException;

  /**
   * Deletes Pages of a given site
   * 
   * @param  siteKey {@link SiteKey}
   * @return         true if deleted, else false
   */
  boolean destroyPages(SiteKey siteKey);

  /**
   * Clone a page.
   *
   * @param  src                  the source key
   * @param  dst                  the destination key
   * @return                      the cloned page
   * @throws PageServiceException anything that would prevent the operation to
   *                                succeed
   */
  PageContext clone(PageKey src, PageKey dst) throws PageServiceException;

  /**
   * Query the page service to find pages that match the <code>siteType</code>,
   * <code>siteName</code>, <code>pageName</code> and <code>title</code>
   * criterions.
   *
   * @param  offset               the query offset
   * @param  limit                the query limit
   * @param  siteType             the site type
   * @param  siteName             the site name
   * @param  pageName             the page name
   * @param  pageTitle            the page title
   * @return                      the query result
   * @throws PageServiceException anything that would prevent the operation to
   *                                succeed
   */
  QueryResult<PageContext> findPages(int offset, int limit, SiteType siteType, String siteName, String pageName,
                                     String pageTitle) throws PageServiceException;

  /**
   * Saves a page. If a page with the same id already exists then a merge
   * operation will occur, otherwise it throws {@link IllegalStateException}
   * From PLF 5.3.x (RDBMS implementation) we drop support return the change
   * list as it's not used any where. So the method always return the empty
   * list.
   *
   * @param  page the page to save
   */
  void save(org.exoplatform.portal.pom.data.PageData page);

  /**
   * Retrieves page data switch given key
   * 
   * @param  key {@link org.exoplatform.portal.pom.data.PageKey}
   * @return     {@link PageData}
   */
  org.exoplatform.portal.pom.data.PageData getPage(org.exoplatform.portal.pom.data.PageKey key);

  Page getPage(String pageKey);

  Page getPage(PageKey pageKey);

}
