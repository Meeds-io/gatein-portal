package org.exoplatform.portal.mop.storage;

import static org.exoplatform.portal.mop.storage.utils.MOPUtils.convertSiteType;
import static org.exoplatform.portal.mop.storage.utils.MOPUtils.parseJsonArray;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.gatein.api.page.PageQuery;
import org.json.simple.JSONArray;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.jdbc.entity.ComponentEntity;
import org.exoplatform.portal.jdbc.entity.PageEntity;
import org.exoplatform.portal.jdbc.entity.PermissionEntity;
import org.exoplatform.portal.jdbc.entity.SiteEntity;
import org.exoplatform.portal.mop.dao.PageDAO;
import org.exoplatform.portal.mop.dao.SiteDAO;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageError;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageServiceException;
import org.exoplatform.portal.mop.page.PageState;
import org.exoplatform.portal.pom.data.PageData;
import org.exoplatform.portal.mop.EventType;
import org.exoplatform.portal.mop.QueryResult;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.Utils;
import org.exoplatform.portal.mop.PageType;
import org.exoplatform.services.listener.ListenerService;

public class PageStorageImpl extends AbstractPageStorage {

  private static final String NO_NULL_KEY_ACCEPTED = "No null key accepted";

  public PageStorageImpl(ListenerService listenerService,
                         LayoutStorage layoutStorage,
                         SiteDAO siteDAO,
                         PageDAO pageDAO) {
    super(listenerService, layoutStorage, siteDAO, pageDAO);
  }

  @Override
  public Page getPage(String pageKey) {
    PageKey key = PageKey.parse(pageKey);
    return getPage(key);
  }

  @Override
  public Page getPage(PageKey key) {
    if (key == null) {
      throw new IllegalArgumentException(NO_NULL_KEY_ACCEPTED);
    }
    PageData pageData = getPage(key.toPomPageKey());
    if (pageData == null) {
      return null;
    } else {
      return new Page(pageData);
    }
  }

  @Override
  public PageContext loadPage(PageKey key) {
    if (key == null) {
      throw new IllegalArgumentException(NO_NULL_KEY_ACCEPTED);
    }

    PageData pageData = getPage(key.toPomPageKey());
    if (pageData == null) {
      return null;
    } else {
      return new PageContext(key, Utils.toPageState(pageData));
    }
  }

  /**
   * <p>
   * Load all the pages of a specific site. Note that this method can
   * potentially raise performance issues if the number of pages is very large
   * and should be used with cautions. That's the motiviation for not having
   * this method on the {@link PageStorage} interface.
   * </p>
   *
   * @param  siteKey              the site key
   * @return                      the list of pages
   * @throws PageServiceException anything that would prevent the operation to
   *                                succeed
   */
  public List<PageContext> loadPages(SiteKey siteKey) {
    if (siteKey == null) {
      throw new IllegalArgumentException("No null site key accepted");
    }
    return findPages(siteKey.getType(), siteKey.getName(), null, 0, -1);
  }

  @Override
  public boolean savePage(PageContext page) {
    if (page == null) {
      throw new IllegalArgumentException("PageContext is mandatory");
    }

    PageEntity entity = pageDAO.findByKey(page.getKey());
    boolean created = false;
    if (entity == null) {
      entity = new PageEntity();
      applyPageContextToEntity(entity, page);
      entity = pageDAO.create(entity);
      created = true;
    } else {
      applyPageContextToEntity(entity, page);
      entity = pageDAO.update(entity);
    }

    PageState state = page.getState();
    if (state != null) {
      savePagePermissions(PageEntity.class.getName(),
                          entity.getId(),
                          state.getAccessPermissions(),
                          Arrays.asList(state.getEditPermission()),
                          state.getMoveAppsPermissions(),
                          state.getMoveContainersPermissions());
    }

    if (created) {
      broadcastEvent(EventType.PAGE_CREATED, page.getKey());
    } else {
      broadcastEvent(EventType.PAGE_UPDATED, page.getKey());
    }
    return created;
  }

  @Override
  public boolean destroyPage(PageKey key) {
    if (key == null) {
      throw new IllegalArgumentException("PageKey is mandatory");
    }

    PageEntity page = pageDAO.findByKey(key);
    if (page != null) {
      String pageBody = page.getPageBody();
      JSONArray children = parseJsonArray(pageBody);
      layoutStorage.deleteChildren(children);
      layoutStorage.deletePermissions(PageEntity.class.getName(), page.getId());
      pageDAO.delete(page);

      broadcastEvent(EventType.PAGE_DESTROYED, key);
      return true;
    }
    return false;
  }

  @Override
  public boolean destroyPages(SiteKey siteKey) {
    List<PageKey> pageKeys = findPageKeys(siteKey.getType(), siteKey.getName(), null, 0, -1);
    pageKeys.forEach(this::destroyPage);
    return !pageKeys.isEmpty();
  }

  @Override
  public PageContext clone(PageKey srcPageKey, PageKey dstPageKey) {
    if (srcPageKey == null) {
      throw new IllegalArgumentException("No null source accepted");
    }
    if (dstPageKey == null) {
      throw new IllegalArgumentException("No null destination accepted");
    }

    PageEntity pageSrc = pageDAO.findByKey(srcPageKey);
    if (pageSrc == null) {
      throw new PageServiceException(PageError.CLONE_NO_SRC_PAGE,
                                     String.format("Could not clone non existing page %s from site of type %s with id %s",
                                                   srcPageKey.getName(),
                                                   srcPageKey.getSite().getType(),
                                                   srcPageKey.getSite().getName()));
    } else {
      PageEntity pageDst = pageDAO.findByKey(dstPageKey);
      if (pageDst != null) {
        throw new PageServiceException(PageError.CLONE_DST_ALREADY_EXIST,
                                       String.format("Could not clone page %s to existing page %s with id %s",
                                                     dstPageKey.getName(),
                                                     dstPageKey.getSite().getType(),
                                                     dstPageKey.getSite().getName()));
      } else {
        SiteKey siteKey = dstPageKey.getSite();
        SiteEntity owner = siteDAO.findByKey(siteKey);
        if (owner == null) {
          throw new PageServiceException(PageError.CLONE_NO_DST_SITE,
                                         String.format("Could not clone page %s to non existing site of type %s with id %s",
                                                       dstPageKey.getName(),
                                                       siteKey.getTypeName(),
                                                       siteKey.getName()));
        }

        pageDst = new PageEntity();
        applyPageContextToEntity(pageDst, buildPageContext(pageSrc));
        List<ComponentEntity> children = layoutStorage.clone(PageEntity.class.getName(), pageSrc.getPageBody());
        pageDst.setChildren(children);
        pageDst.setPageBody(((JSONArray) pageDst.toJSON().get("children")).toJSONString());
        //

        pageDst.setName(dstPageKey.getName());
        pageDst.setOwner(owner);
        pageDst = pageDAO.create(pageDst);
        layoutStorage.clonePermissions(PageEntity.class.getName(), pageDst.getId(), pageSrc.getId());

        PageContext pageContext = buildPageContext(pageDst);
        broadcastEvent(EventType.PAGE_CREATED, dstPageKey);
        return pageContext;
      }
    }
  }

  @Override
  public QueryResult<PageContext> findPages(int from,
                                            int limit,
                                            SiteType siteType,
                                            String siteName,
                                            String pageName,
                                            String pageTitle) {
    List<PageContext> pages = findPages(siteType, siteName, pageTitle, from, limit);
    return new QueryResult<>(from, pages.size(), pages);
  }

  private List<PageContext> findPages(SiteType siteType, String siteName, String pageTitle, int from, int limit) {
    List<PageKey> pageKeys = findPageKeys(siteType, siteName, pageTitle, from, limit);
    return pageKeys.stream()
                   .map(this::loadPage)
                   .toList();
  }

  private List<PageKey> findPageKeys(SiteType siteType, String siteName, String pageTitle, int from, int limit) {
    PageQuery pageQuery = new PageQuery.Builder().withDisplayName(pageTitle)
                                                 .withSiteType(convertSiteType(siteType))
                                                 .withSiteName(StringUtils.trim(siteName))
                                                 .withPagination(from, limit)
                                                 .build();
    try {
      ListAccess<PageKey> pagesListAccess = pageDAO.findByQuery(pageQuery);
      PageKey[] pageKeys = pagesListAccess.load(0, pagesListAccess.getSize());
      return Arrays.asList(pageKeys);
    } catch (Exception ex) {
      throw new IllegalStateException("Error retrieving pages using query " + pageQuery);
    }
  }

  private void applyPageContextToEntity(PageEntity entity, PageContext pageContext) {
    PageState state = pageContext.getState();
    if (state != null) {
      entity.setDescription(state.getDescription());
      entity.setDisplayName(state.getDisplayName());
      entity.setFactoryId(state.getFactoryId());
      entity.setShowMaxWindow(state.getShowMaxWindow());
      entity.setHideSharedLayout(state.isHideSharedLayout());
      entity.setPageType(!StringUtils.isBlank(state.getType()) ? PageType.valueOf(state.getType()) : PageType.PAGE);
      entity.setLink(state.getLink());
    } else {
      entity.setPageType(PageType.PAGE);
    }

    SiteKey siteKey = pageContext.getKey().getSite();
    entity.setOwner(siteDAO.findByKey(siteKey));
    entity.setName(pageContext.getKey().getName());
  }

  private PageContext buildPageContext(PageEntity entity) {
    PageData pageData = buildPageData(entity);
    return new PageContext(pageData.getKey().toMopPageKey(), Utils.toPageState(pageData));
  }

  private void savePagePermissions(String objectType,
                                   long objectId,
                                   List<String> accessPermissions,
                                   List<String> editPermissions,
                                   List<String> moveAppsPermissions,
                                   List<String> moveContainersPermissions) {
    layoutStorage.savePermissions(objectType,
                                  objectId,
                                  PermissionEntity.TYPE.ACCESS,
                                  accessPermissions);
    layoutStorage.savePermissions(objectType,
                                  objectId,
                                  PermissionEntity.TYPE.EDIT,
                                  editPermissions);
    layoutStorage.savePermissions(objectType,
                                  objectId,
                                  PermissionEntity.TYPE.MOVE_APP,
                                  moveAppsPermissions);
    layoutStorage.savePermissions(objectType,
                                  objectId,
                                  PermissionEntity.TYPE.MOVE_CONTAINER,
                                  moveContainersPermissions);
  }

}
