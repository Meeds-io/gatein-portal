package org.gatein.portal.idm.impl.repository;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gatein.portal.idm.IdentityStoreSource;
import org.picketlink.idm.common.exception.IdentityException;
import org.picketlink.idm.impl.api.IdentitySearchCriteriaImpl;
import org.picketlink.idm.impl.repository.FallbackIdentityStoreRepository;
import org.picketlink.idm.impl.repository.RepositoryIdentityStoreSessionImpl;
import org.picketlink.idm.impl.store.SimpleIdentityStoreInvocationContext;
import org.picketlink.idm.spi.configuration.IdentityRepositoryConfigurationContext;
import org.picketlink.idm.spi.configuration.metadata.IdentityRepositoryConfigurationMetaData;
import org.picketlink.idm.spi.configuration.metadata.IdentityStoreMappingMetaData;
import org.picketlink.idm.spi.model.IdentityObject;
import org.picketlink.idm.spi.model.IdentityObjectAttribute;
import org.picketlink.idm.spi.model.IdentityObjectType;
import org.picketlink.idm.spi.search.IdentityObjectSearchCriteria;
import org.picketlink.idm.spi.store.AttributeStore;
import org.picketlink.idm.spi.store.IdentityStore;
import org.picketlink.idm.spi.store.IdentityStoreInvocationContext;
import org.picketlink.idm.spi.store.IdentityStoreSession;


/**
 * extends the class FallbackIdentityStoreRepository from PicketLink Idm in order to customize the PortalRepository
 * and to work with the mandatoryStoredObjects option which is added in the LDAP configuration
 */
public class ExoFallbackIdentityStoreRepository extends FallbackIdentityStoreRepository {

    private static Logger log = Logger.getLogger(ExoFallbackIdentityStoreRepository.class.getName());

    //New option added in the LDAP configuration
    public static final String OPTION_MANDATORY_OBJECTS = "mandatoryStoredObjects";

    // Map<MandatoryIdentityTypeIds, CorrespondingIdentityStore>
    private Map<String, IdentityStore> mandatoryStores = new HashMap<String, IdentityStore>();

    // Set<MandatoryIdentityTypeIds>
    private Set<String> mandatoryStoresObjects = new HashSet<String>();

    private final Set<IdentityStore> configuredIdentityStores = new HashSet<IdentityStore>();

    public ExoFallbackIdentityStoreRepository(String id) {
        super(id);
    }

    /**override bootstrap() method in order to add
     *  the mandatoryStoredObjects option of the new LDAP configuration into consideration
     * @param configurationContext
     * @param bootstrappedIdentityStores
     * @param bootstrappedAttributeStores
     * @throws IdentityException
     */
    @Override
    public void bootstrap(IdentityRepositoryConfigurationContext configurationContext, Map<String, IdentityStore> bootstrappedIdentityStores, Map<String, AttributeStore> bootstrappedAttributeStores)
            throws IdentityException {
        super.bootstrap(configurationContext, bootstrappedIdentityStores, bootstrappedAttributeStores);
        // Helper collection to keep all identity stores in use
        if (getIdentityStoreMappings().size() > 0) {
            configuredIdentityStores.addAll(getMappedIdentityStores());
        }
        IdentityRepositoryConfigurationMetaData configurationMD = configurationContext.getRepositoryConfigurationMetaData();
        List<IdentityStoreMappingMetaData> mappingMDs = configurationMD.getIdentityStoreToIdentityObjectTypeMappings();
        for (IdentityStoreMappingMetaData mappingMD : mappingMDs) {
            String identityTypeIdsValue = mappingMD.getOptionSingleValue(OPTION_MANDATORY_OBJECTS);
            if (identityTypeIdsValue != null && !identityTypeIdsValue.trim().isEmpty()) {
                identityTypeIdsValue = identityTypeIdsValue.trim();
                String identityStoreId = mappingMD.getIdentityStoreId();
                String[] identityTypeIds = identityTypeIdsValue.split(",");
                for (String identityTypeId : identityTypeIds) {
                    identityTypeId = identityTypeId.trim();
                    if (identityTypeId.isEmpty()) {
                        continue;
                    }
                    if (!mappingMD.getIdentityObjectTypeMappings().contains(identityTypeId)) {
                        log.warning("'mandatoryStoredObjects' parameter contains an identityTypeId that is not mapped in Store : " + identityStoreId);
                        continue;
                    }
                    if (mandatoryStoresObjects.contains(identityTypeId)) {
                        log.warning("'mandatoryStoredObjects' parameter contains an identityTypeId that is already defined as mandatory in a Store. This parameter value will be ignored in store: "
                                + identityStoreId);
                        continue;
                    }
                    Iterator<IdentityStore> identityStoresIterator = configuredIdentityStores.iterator();
                    do {
                        IdentityStore identityStore = identityStoresIterator.next();
                        if (identityStore.getId().equals(identityStoreId)) {
                            mandatoryStores.put(identityTypeId, identityStore);
                        }
                    } while (identityStoresIterator.hasNext() && !mandatoryStores.containsKey(identityTypeId));
                    mandatoryStoresObjects.add(identityTypeId);
                }
            }
        }
    }

    /**Add this original method from the FallbackIdentityStoreRepository class
     * @param iot
     * @return List<IdentityStore> used in the implementation of findIdentityObject() method
     */
    List<IdentityStore> resolveIdentityStores(IdentityObjectType iot) {

        List<IdentityStore> ids = null;
        try {
            ids = getIdentityStores(iot);
        } catch (IdentityException e) {
            if (isAllowNotDefinedAttributes()) {
                ids = new LinkedList<IdentityStore>();
                ids.add(defaultIdentityStore);
                return ids;
            }
            if (log.isLoggable(Level.FINER)) {
                log.log(Level.FINER, "Exception occurred: ", e);
            }
            throw new IllegalStateException("Used IdentityObjectType not mapped. Consider using " + ALLOW_NOT_DEFINED_IDENTITY_OBJECT_TYPES_OPTION + " repository option switch: " + iot);
        }
        if (ids == null || ids.size() == 0) {
            ids = new LinkedList<IdentityStore>();
            ids.add(defaultIdentityStore);
        } else {
            boolean isReadOnly = true;
            for (IdentityStore identityStore : ids) {
                isReadOnly &= isIdentityStoreReadOnly(identityStore);
            }
            if (isReadOnly) {
                ids.add(defaultIdentityStore);
            }
        }
        return ids;
    }

    /**Add this original method from the FallbackIdentityStoreRepository class
     * @param targetStore
     * @param invocationCtx
     * @return IdentityStoreInvocationContext used in the implementation of findIdentityObject() method
     */
    IdentityStoreInvocationContext resolveInvocationContext(IdentityStore targetStore, IdentityStoreInvocationContext invocationCtx) {
        return resolveInvocationContext(targetStore.getId(), invocationCtx);
    }

    /**Add this original method from the FallbackIdentityStoreRepository class
     * @param targetStore
     * @param invocationCtx
     * @return IdentityStoreInvocationContext used in the implementation of findIdentityObject() method
     */
    IdentityStoreInvocationContext resolveInvocationContext(AttributeStore targetStore, IdentityStoreInvocationContext invocationCtx) {
        return resolveInvocationContext(targetStore.getId(), invocationCtx);
    }

    /** Add this original method from the FallbackIdentityStoreRepository class
     * @param id
     * @param invocationCtx
     * @return IdentityStoreInvocationContext used in the implementation of findIdentityObject() method
     */
    IdentityStoreInvocationContext resolveInvocationContext(String id, IdentityStoreInvocationContext invocationCtx) {
        RepositoryIdentityStoreSessionImpl repoSession = (RepositoryIdentityStoreSessionImpl) invocationCtx.getIdentityStoreSession();
        IdentityStoreSession targetSession = repoSession.getIdentityStoreSession(id);

        return new SimpleIdentityStoreInvocationContext(targetSession, invocationCtx.getRealmId(), String.valueOf(this.hashCode()));
    }

    /**override findIdentityObjectByUniqueAttribute() method in order to add
     *  the mandatoryStoredObjects option of the new LDAP configuration into consideration
     * @param invocationContext
     * @param name
     * @param identityObjectType
     * @return
     * @throws IdentityException
     */
    @Override
    public IdentityObject findIdentityObject(IdentityStoreInvocationContext invocationContext, String name, IdentityObjectType identityObjectType) throws IdentityException {
        IdentityObject mandatoryIO = null;
        List<IdentityStore> targetStores = resolveIdentityStores(identityObjectType);
        IdentityObject io = null;
        for (IdentityStore targetStore : targetStores) {
            IdentityStoreInvocationContext targetCtx = resolveInvocationContext(targetStore, invocationContext);
            try {
                io = targetStore.findIdentityObject(targetCtx, name, identityObjectType);
            } catch (IdentityException e) {
                log.log(Level.SEVERE, "Failed to find IdentityObject in target store: ", e);
            }
            if (io != null) {
                break;
            }
        }
        if (io != null) {
            IdentityStoreInvocationContext defaultInvocationContext = resolveInvocationContext(defaultIdentityStore, invocationContext);
            if (!isFirstlyCreatedIn(defaultInvocationContext, defaultIdentityStore, io) && mandatoryStoresObjects.contains(identityObjectType.getName())) {
                IdentityStore mandatoryStore = mandatoryStores.get(identityObjectType.getName());

                IdentityStoreInvocationContext mappedContext = resolveInvocationContext(mandatoryStore, invocationContext);
                try {
                    mandatoryIO = mandatoryStore.findIdentityObject(mappedContext, name, identityObjectType);
                } catch (IdentityException e) {
                    log.log(Level.SEVERE, "Failed to find IdentityObject in target store: ", e);
                }

                if (mandatoryIO == null) {
                    return null;
                }
            }

            return io;
        }

        try {
            io = defaultIdentityStore.findIdentityObject(resolveInvocationContext(defaultIdentityStore, invocationContext), name, identityObjectType);
        } catch (IdentityException e) {
            log.log(Level.SEVERE, "Failed to find IdentityObject in default store: ", e);
        }
        return io;
    }

    /**override findIdentityObjectByUniqueAttribute() method in order to add
     *  the mandatoryStoredObjects option of the new LDAP configuration into consideration
     * @param invocationContext
     * @param id
     * @return
     * @throws IdentityException
     */
    @Override
    public IdentityObject findIdentityObject(IdentityStoreInvocationContext invocationContext, String id) throws IdentityException {
        // TODO: information about the store mapping should be encoded in id as now
        // its like random guess and this kills performance ...
        IdentityStoreInvocationContext defaultInvocationContext = resolveInvocationContext(defaultIdentityStore, invocationContext);

        for (IdentityStore identityStore : getIdentityStoreMappings().values()) {
            IdentityStoreInvocationContext targetCtx = resolveInvocationContext(identityStore, invocationContext);

            IdentityObject io = identityStore.findIdentityObject(targetCtx, id);
            if (io != null) {
                if (!isFirstlyCreatedIn(defaultInvocationContext, defaultIdentityStore, io) && mandatoryStoresObjects.contains(io.getIdentityType().getName())) {
                    IdentityStore mandatoryStore = mandatoryStores.get(io.getIdentityType().getName());

                    // If it's retrieved from mandatoryStore, so return it. No need to
                    // check it again.
                    if (identityStore.getId().equals(mandatoryStore.getId())) {
                        return io;
                    }

                    IdentityStoreInvocationContext mappedContext = resolveInvocationContext(mandatoryStore, invocationContext);
                    IdentityObject mandatoryIO = null;
                    try {
                        mandatoryIO = mandatoryStore.findIdentityObject(mappedContext, id);
                    } catch (IdentityException e) {
                        log.log(Level.SEVERE, "Failed to find IdentityObject in target store: ", e);
                    }

                    if (mandatoryIO == null) {
                        return null;
                    }
                }
                return io;
            }
        }

        return defaultIdentityStore.findIdentityObject(invocationContext, id);
    }

    /** override findIdentityObjectByUniqueAttribute() method in order to add
     *  the mandatoryStoredObjects option of the new LDAP configuration into consideration
     * @param invocationCtx
     * @param identityType
     * @param criteria
     * @return Collection&lt;IdentityObject&gt;
     * @throws IdentityException
     */
    @Override
    public Collection<IdentityObject> findIdentityObject(IdentityStoreInvocationContext invocationCtx, IdentityObjectType identityType, IdentityObjectSearchCriteria criteria) throws IdentityException {
        List<IdentityStore> targetStores = resolveIdentityStores(identityType);

        Collection<IdentityObject> results = new LinkedList<IdentityObject>();
        Collection<IdentityObject> defaultIOs = new LinkedList<IdentityObject>();

        IdentityStoreInvocationContext defaultInvocationContext = resolveInvocationContext(defaultIdentityStore, invocationCtx);
        if (targetStores.size() == 1 && targetStores.contains(defaultIdentityStore)) {
            Collection<IdentityObject> resx = new LinkedList<IdentityObject>();

            try {
                resx = defaultIdentityStore.findIdentityObject(defaultInvocationContext, identityType, criteria);
            } catch (IdentityException e) {
                log.log(Level.SEVERE, "Exception occurred: ", e);
            }

            return resx;
        } else {

            IdentitySearchCriteriaImpl c = null;
            if (criteria != null) {
                c = new IdentitySearchCriteriaImpl(criteria);
                c.setPaged(false);
            }
            // Get results from default store with not paged criteria
            defaultIOs = defaultIdentityStore.findIdentityObject(defaultInvocationContext, identityType, c);

            // if default store results are not present then apply criteria. Otherwise
            // apply criteria without page
            // as result need to be merged
            if (defaultIOs.size() == 0 && targetStores.size() == 1) {
                Collection<IdentityObject> resx = new LinkedList<IdentityObject>();

                try {
                    IdentityStore targetStore = targetStores.get(0);
                    IdentityStoreInvocationContext targetCtx = resolveInvocationContext(targetStore, invocationCtx);
                    resx = targetStore.findIdentityObject(targetCtx, identityType, criteria);
                } catch (IdentityException e) {
                    log.log(Level.SEVERE, "Exception occurred: ", e);
                }

                return resx;
            } else {

                for (IdentityStore targetStore : targetStores) {
                    try {
                        IdentityStoreInvocationContext targetCtx = resolveInvocationContext(targetStore, invocationCtx);

                        Collection<IdentityObject> identityObjects = targetStore.findIdentityObject(targetCtx, identityType, c);

                        if (mandatoryStoresObjects.contains(identityType.getName())) {
                            IdentityStore mandatoryStore = mandatoryStores.get(identityType.getName());

                            // Delete from result, identities not found in mandatory store
                            if (!targetStore.getId().equals(mandatoryStore.getId())) {
                                IdentityStoreInvocationContext mappedContext = resolveInvocationContext(mandatoryStore, invocationCtx);
                                Iterator<IdentityObject> identityObjectsIterator = identityObjects.iterator();
                                while (identityObjectsIterator.hasNext()) {
                                    IdentityObject identityObject = identityObjectsIterator.next();
                                    if (!isFirstlyCreatedIn(defaultInvocationContext, defaultIdentityStore, identityObject) && !hasIdentityObject(mappedContext, mandatoryStore, identityObject)) {
                                        identityObjectsIterator.remove();
                                    }
                                }
                            }
                        }
                        results.addAll(identityObjects);
                    } catch (IdentityException e) {
                        log.log(Level.SEVERE, "Exception occurred: ", e);
                    }
                }
            }
        }
        // Filter out duplicates
        HashSet<IdentityObject> merged = new HashSet<IdentityObject>();
        merged.addAll(results);
        if (mandatoryStoresObjects.contains(identityType.getName())) {
            IdentityStore mandatoryStore = mandatoryStores.get(identityType.getName());
            // Delete from result, identities not found in mandatory store
            if (!defaultIdentityStore.getId().equals(mandatoryStore.getId())) {
                IdentityStoreInvocationContext mappedContext = resolveInvocationContext(mandatoryStore, invocationCtx);
                Iterator<IdentityObject> identityObjectsIterator = defaultIOs.iterator();
                while (identityObjectsIterator.hasNext()) {
                    IdentityObject identityObject = identityObjectsIterator.next();
                    if (!isFirstlyCreatedIn(defaultInvocationContext, defaultIdentityStore, identityObject) && !hasIdentityObject(mappedContext, mandatoryStore, identityObject)) {
                        identityObjectsIterator.remove();
                        // delete IdentityObject from default store because it was
                        // deleted from mandatory store
                        defaultIdentityStore.removeIdentityObject(defaultInvocationContext, identityObject);
                    }
                }
            }
        }
        merged.addAll(defaultIOs);
        // Apply criteria not applied at store level (sort/page)
        if (criteria != null) {
            LinkedList<IdentityObject> processed = new LinkedList<IdentityObject>(merged);
            // TODO: hardcoded - expects List
            if (criteria.isSorted()) {
                sortByName(processed, criteria.isAscending());
            }
            results = processed;
            // TODO: hardcoded - expects List
            if (criteria.isPaged()) {
                results = cutPageFromResults(processed, criteria);
            }
        } else {
            results = merged;
        }
        return results;
    }

    /** Add this original method from the FallbackIdentityStoreRepository class because it has a private access in its original class
     *  used in the implementation of findIdentityObject() method
     * @param objects
     * @param ascending
     */
    private void sortByName(List<IdentityObject> objects, final boolean ascending) {
        Collections.sort(objects, new Comparator<IdentityObject>() {
            public int compare(IdentityObject o1, IdentityObject o2) {
                if (ascending) {
                    return o1.getName().compareTo(o2.getName());
                } else {
                    return o2.getName().compareTo(o1.getName());
                }
            }
        });
    }

    /** Add this original method from the FallbackIdentityStoreRepository class because it has a private access in its original class
     * @param objects
     * @param criteria
     * @return List<IdentityObject> used in the implementation of findIdentityObject() method
     */
    private List<IdentityObject> cutPageFromResults(List<IdentityObject> objects, IdentityObjectSearchCriteria criteria) {

        List<IdentityObject> results = new LinkedList<IdentityObject>();

        if (criteria.getMaxResults() == 0) {
            for (int i = criteria.getFirstResult(); i < objects.size(); i++) {
                if (i < objects.size()) {
                    results.add(objects.get(i));
                }
            }
        } else {
            for (int i = criteria.getFirstResult(); i < criteria.getFirstResult() + criteria.getMaxResults(); i++) {
                if (i < objects.size()) {
                    results.add(objects.get(i));
                }
            }
        }
        return results;
    }


    /** re-implement isFirstlyCreatedIn() method, declared in the interface IdentityStoreSource, in order to add
     * the mandatoryStoredObjects option of the new LDAP configuration into consideration
     * @param mappedContext
     * @param targetStore
     * @param identityObject
     * @return
     * @throws IdentityException
     */
    private boolean isFirstlyCreatedIn(IdentityStoreInvocationContext mappedContext, IdentityStore targetStore, IdentityObject identityObject) throws IdentityException {
        if (targetStore instanceof IdentityStoreSource) {
            try {
                if (!mandatoryStoresObjects.contains(identityObject.getIdentityType().getName())) {
                    return true;
                } else {
                    return ((IdentityStoreSource) targetStore).isFirstlyCreatedIn(mappedContext, identityObject);
                }
            } catch (Exception e) {
                throw new IdentityException(e);
            }
        }
        return false;
    }

    /** override findIdentityObjectByUniqueAttribute() method in order to add
     *  the mandatoryStoredObjects option of the new LDAP configuration into consideration
     * @param invocationCtx
     * @param identityObjectType
     * @param attribute
     * @return
     * @throws IdentityException
     */
    @Override
    public IdentityObject findIdentityObjectByUniqueAttribute(IdentityStoreInvocationContext invocationCtx, IdentityObjectType identityObjectType, IdentityObjectAttribute attribute)
            throws IdentityException {
        try {
            // Test is identity exists in mandatory store, else return null anyway

            if (mandatoryStoresObjects.contains(identityObjectType.getName())) {
                IdentityStore mandatoryStore = mandatoryStores.get(identityObjectType.getName());
                IdentityStoreInvocationContext mappedContext = resolveInvocationContext(mandatoryStore, invocationCtx);
                Set<String> supportedAttrs = mandatoryStore.getSupportedAttributeNames(mappedContext, identityObjectType);
                if (supportedAttrs.contains(attribute.getName())) {
                    if (mandatoryStore.findIdentityObjectByUniqueAttribute(mappedContext, identityObjectType, attribute) == null) {
                        return null;
                    }
                }
            }
            Collection<IdentityStore> mappedStores = resolveIdentityStores(identityObjectType);
            IdentityObject result = null;
            for (IdentityStore mappedStore : mappedStores) {
                if (mappedStore != defaultAttributeStore) {
                    IdentityStoreInvocationContext targetCtx = resolveInvocationContext(mappedStore, invocationCtx);
                    Set<String> supportedAttrs = mappedStore.getSupportedAttributeNames(targetCtx, identityObjectType);
                    if (supportedAttrs.contains(attribute.getName())) {
                        result = mappedStore.findIdentityObjectByUniqueAttribute(targetCtx, identityObjectType, attribute);
                    }
                    // First with any result win
                    if (result != null) {
                        return result;
                    }
                }
            }
            // And if we are still here just go with default
            IdentityStoreInvocationContext defaultCtx = resolveInvocationContext(defaultAttributeStore, invocationCtx);

            if (isAllowNotDefinedAttributes()) {
                result = defaultAttributeStore.findIdentityObjectByUniqueAttribute(defaultCtx, identityObjectType, attribute);
            } else {
                Set<String> supportedAttrs = defaultAttributeStore.getSupportedAttributeNames(defaultCtx, identityObjectType);
                if (supportedAttrs.contains(attribute.getName())) {
                    result = defaultAttributeStore.findIdentityObjectByUniqueAttribute(defaultCtx, identityObjectType, attribute);
                }
            }
            return result;
        } catch (IdentityException e) {
            if (log.isLoggable(Level.FINER)) {
                log.log(Level.FINER, "Exception occurred: ", e);
            }
            throw e;
        }
    }
}