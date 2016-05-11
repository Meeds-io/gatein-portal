package org.gatein.portal.idm.impl.store.hibernate;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gatein.portal.idm.IdentityStoreSource;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.picketlink.idm.common.exception.IdentityException;
import org.picketlink.idm.impl.api.PasswordCredential;
import org.picketlink.idm.impl.model.hibernate.HibernateIdentityObject;
import org.picketlink.idm.impl.model.hibernate.HibernateIdentityObjectCredential;
import org.picketlink.idm.impl.model.hibernate.HibernateIdentityObjectCredentialType;
import org.picketlink.idm.impl.store.hibernate.HibernateIdentityStoreImpl;
import org.picketlink.idm.spi.model.IdentityObject;
import org.picketlink.idm.spi.model.IdentityObjectCredentialType;
import org.picketlink.idm.spi.store.IdentityStore;
import org.picketlink.idm.spi.store.IdentityStoreInvocationContext;


/**
 * extends the class HibernateIdentityStoreImpl from PicketLink Idm to benefit from its methods in the implementation of isFirstlyCreatedIn() method declared in the IdentityStoreSource interface
 *in order to customize the Hibernate store
 */
public class ExoHibernateIdentityStoreImpl extends HibernateIdentityStoreImpl implements IdentityStore, IdentityStoreSource, Serializable {

    private static Logger log = Logger.getLogger(ExoHibernateIdentityStoreImpl.class.getName());

    public static final String DEFAULT_REALM_NAME = ExoHibernateIdentityStoreImpl.class.getName() + ".DEFAULT_REALM";

    private boolean isRealmAware = false;

    public ExoHibernateIdentityStoreImpl(String id) {
        super(id);
    }

    /** Add this method from the HibernateIdentityStoreImpl class because it has a private access in its original class
     *
     * @param ctx
     * @return DEFAULT_REALM_NAME (String) used in the implementation of getHibernateIdentityObject() method
     */
    private String getRealmName(IdentityStoreInvocationContext ctx) {
        if (isRealmAware()) {
            return ctx.getRealmId();
        } else {
            return DEFAULT_REALM_NAME;
        }
    }

    /** Add this method from the HibernateIdentityStoreImpl class because it has a private access in its original class
     * used in the implementation of getRealmName() method
     * @return
     */
    private boolean isRealmAware() {
        return isRealmAware;
    }

    /** Add this method from the HibernateIdentityStoreImpl class because and it has a private access in its original class
     *  used in the implementation of safeGet() method
     * @param io
     */
    private void checkIOInstance(IdentityObject io) {
        if (io == null) {
            throw new IllegalArgumentException("IdentityObject is null");
        }

    }

    /** Add this method from the HibernateIdentityStoreImpl class because it has a private access in its original class
     * @param ctx
     * @param credentialType
     * @return HibernateIdentityObjectCredentialType used in the implementation of isFirstlyCreatedIn() method
     * @throws IdentityException
     */
    private HibernateIdentityObjectCredentialType getHibernateIdentityObjectCredentialType(IdentityStoreInvocationContext ctx, IdentityObjectCredentialType credentialType) throws IdentityException {
        Session session = getHibernateSession(ctx);

        HibernateIdentityObjectCredentialType hibernateType = null;

        try {
            hibernateType = (HibernateIdentityObjectCredentialType) session.createCriteria(HibernateIdentityObjectCredentialType.class).add(Restrictions.eq("name", credentialType.getName())).setCacheable(true).uniqueResult();
        } catch (HibernateException e) {
            if (log.isLoggable(Level.FINER)) {
                log.log(Level.FINER, "Exception occurred: ", e);
            }

            throw new IdentityException("IdentityObjectCredentialType[ " + credentialType.getName() + "] not present in the store.");
        }

        return hibernateType;

    }

    /** Add this method from the HibernateIdentityStoreImpl class because it has a private access in its original class
     * @param ctx
     * @param io
     * @return HibernateIdentityObject used in the implementation of isFirstlyCreatedIn() method
     * @throws IdentityException
     */
    private HibernateIdentityObject safeGet(IdentityStoreInvocationContext ctx, IdentityObject io) throws IdentityException
    {
        checkIOInstance(io);

        if (io instanceof HibernateIdentityObject)
        {
            return (HibernateIdentityObject)io;
        }

        return getHibernateIdentityObject(ctx, io);

    }

    /** Add this method from the HibernateIdentityStoreImpl class because it has a private access
     * @param ctx
     * @param io
     * @return  HibernateIdentityObject used in the implementation of safeGet() method
     * @throws IdentityException
     */
    private HibernateIdentityObject getHibernateIdentityObject(IdentityStoreInvocationContext ctx, IdentityObject io) throws IdentityException
    {

        HibernateIdentityObject hibernateObject = null;

        Session hibernateSession = getHibernateSession(ctx);

        try
        {

            hibernateObject = (HibernateIdentityObject)hibernateSession.createCriteria(HibernateIdentityObject.class)
                    .add(Restrictions.eq("name", io.getName()).ignoreCase())
                    .createAlias("identityType", "type")
                    .add(Restrictions.eq("type.name", io.getIdentityType().getName()))
                    .createAlias("realm", "rm")
                    .add(Restrictions.eq("rm.name", getRealmName(ctx)))
                    .setCacheable(true)
                    .uniqueResult();

        }
        catch (Exception e)
        {
            if (log.isLoggable(Level.FINER))
            {
                log.log(Level.FINER, "Exception occurred: ", e);
            }

            throw new IdentityException("IdentityObject[ " + io.getName() + " | " + io.getIdentityType().getName() + "] not present in the store.", e);
        }

        return hibernateObject;
    }

    /** Add the implementation of the method isFirstlyCreatedIn()
     *       declared in the interface IdentityStoreSource
     * @param ctx
     * @param identityObject
     * @return
     * @throws Exception
     */
    public boolean isFirstlyCreatedIn(IdentityStoreInvocationContext ctx, IdentityObject identityObject) throws Exception {
        HibernateIdentityObject hibernateObject = safeGet(ctx, identityObject);
        if (hibernateObject != null) {
            if (getSupportedFeatures().isCredentialSupported(hibernateObject.getIdentityType(), PasswordCredential.TYPE)) {
                HibernateIdentityObjectCredentialType hibernateCredentialType = getHibernateIdentityObjectCredentialType(ctx, PasswordCredential.TYPE);

                if (hibernateCredentialType == null) {
                    throw new IllegalStateException("Credential type not present in this store: " + PasswordCredential.TYPE.getName());
                }

                HibernateIdentityObjectCredential hibernateCredential = hibernateObject.getCredential(PasswordCredential.TYPE);

                return (hibernateCredential != null);
            }
        }
        return false;
    }

}