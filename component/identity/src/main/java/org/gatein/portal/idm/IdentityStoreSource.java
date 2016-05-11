package org.gatein.portal.idm;

import org.picketlink.idm.spi.model.IdentityObject;
import org.picketlink.idm.spi.store.IdentityStoreInvocationContext;

public interface IdentityStoreSource {
    /**
     *
     * @param mappedContext
     * @param identityObject
     * @return
     */
    boolean isFirstlyCreatedIn(IdentityStoreInvocationContext mappedContext, IdentityObject identityObject) throws Exception;

}