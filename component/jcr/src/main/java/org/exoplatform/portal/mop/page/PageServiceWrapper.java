package org.exoplatform.portal.mop.page;

import java.util.List;

import javax.transaction.Status;

import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.common.transaction.JTAUserTransactionLifecycleService;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.mop.*;
import org.exoplatform.portal.pom.config.POMSession;
import org.exoplatform.portal.pom.config.POMSessionManager;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.listener.ListenerService;

/**
 * <p>
 * A wrapper for the {@link PageServiceImpl}, the wrappers takes care of integrating the implementation with the GateIn runtime.
 * </p>
 *
 * <p>
 * The wrapper emits events when page modifications are performed:
 * </p>
 * <ul>
 * <li>{@link EventType#PAGE_CREATED}: when a page is created</li>
 * <li>{@link EventType#PAGE_UPDATED}: when a page is updated</li>
 * <li>{@link EventType#PAGE_DESTROYED}: when a page is destroyed</li>
 * </ul>
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public class PageServiceWrapper implements PageService {

    /** . */
    private static final Logger log = LoggerFactory.getLogger(PageServiceWrapper.class);

    /** . */
    private final PageServiceImpl service;

    /** . */
    private final POMSessionManager manager;

    /** . */
    private final ListenerService listenerService;

    public PageServiceWrapper(POMSessionManager manager, ListenerService listenerService) {
        this.service = new PageServiceImpl(manager);
        this.manager = manager;
        this.listenerService = listenerService;
    }

    public PageServiceWrapper(POMSessionManager manager, ListenerService listenerService, CacheService cacheService) {
        this.service = new PageServiceImpl(manager, new ExoDataCache(cacheService));
        this.manager = manager;
        this.listenerService = listenerService;
    }

    @Override
    public PageContext loadPage(PageKey key) {
        return service.loadPage(key);
    }

    public List<PageContext> loadPages(SiteKey siteKey) throws NullPointerException, PageServiceException {
        return service.loadPages(siteKey);
    }

    @Override
    public boolean savePage(PageContext page) {
        boolean created = service.savePage(page);

        //
        if (created) {
            notify(EventType.PAGE_CREATED, page.key);
        } else {
            notify(EventType.PAGE_UPDATED, page.key);
        }

        //
        return created;
    }

    @Override
    public boolean destroyPage(PageKey key) {
        boolean destroyed = service.destroyPage(key);

        //
        if (destroyed) {
            notify(EventType.PAGE_DESTROYED, key);
        }

        //
        return destroyed;
    }

    @Override
    public PageContext clone(PageKey src, PageKey dst) {
        PageContext pageContext = service.clone(src, dst);
        notify(EventType.PAGE_CREATED, dst);
        return pageContext;
    }

    @Override
    public QueryResult<PageContext> findPages(int offset, int limit, SiteType siteType, String siteName, String pageName,
            String pageTitle) {
        try {
            JTAUserTransactionLifecycleService jtaUserTransactionLifecycleService = (JTAUserTransactionLifecycleService) PortalContainer
                    .getInstance().getComponentInstanceOfType(JTAUserTransactionLifecycleService.class);
            if (jtaUserTransactionLifecycleService.getUserTransaction().getStatus() == Status.STATUS_ACTIVE) {
                POMSession pomSession = manager.getSession();
                if (pomSession.isModified()) {
                    if (log.isTraceEnabled()) {
                        log.trace("Active JTA transaction found. Going to sync MOP session and JTA transaction");
                    }

                    // Sync current MOP session first
                    pomSession.save();

                    jtaUserTransactionLifecycleService.finishJTATransaction();
                    jtaUserTransactionLifecycleService.beginJTATransaction();
                }
            }
        } catch (Exception e) {
            log.warn("Error during sync of JTA transaction", e);
        }
        return service.findPages(offset, limit, siteType, siteName, pageName, pageTitle);
    }

    private void notify(String name, PageKey key) {
        try {
            listenerService.broadcast(name, this, key);
        } catch (Exception e) {
            log.error("Error when delivering notification " + name + " for page " + key, e);
        }
    }
}
