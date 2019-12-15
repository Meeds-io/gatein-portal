package org.exoplatform.web.security;

import org.chromattic.api.ChromatticSession;
import org.chromattic.api.query.QueryResult;
import org.exoplatform.commons.chromattic.ChromatticLifeCycle;
import org.exoplatform.commons.chromattic.ChromatticManager;
import org.exoplatform.commons.chromattic.ContextualTask;
import org.exoplatform.commons.chromattic.SessionContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.portal.pom.config.Utils;
import org.exoplatform.web.security.security.*;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.wci.security.Credentials;

import java.util.ArrayList;
import java.util.List;

public class JCRGateInTokenStorage implements GateInTokenStore {
    private static final String LIFECYCLE_NAME = "chromatic-lifecycle";

    private final Logger log = LoggerFactory.getLogger(JCRGateInTokenStorage.class);

    /** . */
    private ChromatticLifeCycle chromatticLifeCycle;

    /** . */
    private String lifecycleName = "autologin";

    public JCRGateInTokenStorage(InitParams initParams, ChromatticManager chromatticManager) {

        if (initParams != null) {
            ValueParam param = initParams.getValueParam(LIFECYCLE_NAME);
            if (param != null) {
                lifecycleName = param.getValue();
            }
        }
        this.chromatticLifeCycle = chromatticManager.getLifeCycle(lifecycleName);
    }

    public void cleanLegacy() {
        new TokenTask<Void>() {
            @Override
            protected Void execute(SessionContext context) {
                ChromatticSession session = context.getSession();
                TokenContainer container = session.findByPath(TokenContainer.class, lifecycleName);
                if (container != null) {
                    /* if the container does not exist, it makes no sense to clean the legacy tokens */
                    container.cleanLegacyTokens();
                } else {
                    session.insert(TokenContainer.class, lifecycleName);
                }
                return null;
            }

        }.executeWith(chromatticLifeCycle);
    }

    @Override
    public void saveToken(TokenData data) throws TokenExistsException {
        boolean saved = new TokenTask<Boolean>() {
            @Override
            protected Boolean execute(SessionContext context) {
                try {
                    TokenContainer tokenContainer = getTokenContainer();
                    tokenContainer.saveToken(context.getSession(), data.tokenId, data.hash, data.payload, data.expirationTime);
                } catch (TokenExistsException e) {
                    return false;
                }
                return true;
            }
        }.executeWith(chromatticLifeCycle);
        if (!saved) {
            throw new TokenExistsException();
        }
    }

    public TokenData getToken(String tokenId) {
        return new TokenTask<TokenData>() {
            @Override
            protected TokenData execute(SessionContext context) {
                TokenEntry en = getTokenContainer().getTokens().get(tokenId);
                if (en != null) {
                    HashedToken hashedToken = getMixin(en, HashedToken.class);
                    if (hashedToken != null && hashedToken.getHashedToken() != null) {
                        return new TokenData(en.getId(), hashedToken.getHashedToken(),
                                new Credentials(en.getUserName(), en.getPassword()), en.getExpirationTime());
                    }
                }
                return null;
            }
        }.executeWith(chromatticLifeCycle);
    }

    @Override
    public void deleteToken(String tokenId) {
        new TokenTask<Boolean>() {
            @Override
            protected Boolean execute(SessionContext context) {
                getTokenContainer().removeToken(tokenId);
                return true;
            }
        }.executeWith(chromatticLifeCycle);
    }

    @Override
    public void deleteTokenOfUser(String user) {
        new TokenTask<Void>() {
            @Override
            protected Void execute(SessionContext context) {
                QueryResult<TokenEntry> result = findTokensOfUser(user);
                while (result.hasNext()) {
                    TokenEntry en = result.next();
                    en.remove();
                }
                return null;
            }

        }.executeWith(chromatticLifeCycle);
    }

    @Override
    public void deleteAll() {
        new TokenTask<Void>() {
            @Override
            protected Void execute(SessionContext context) {
                TokenContainer container = getTokenContainer();
                if (container != null && container.size() > 0) {
                    container.removeAll();
                }
                return null;
            }

        }.executeWith(chromatticLifeCycle);
    }

    @Override
    public void cleanExpired() {
        new TokenTask<Void>() {
            @Override
            protected Void execute(SessionContext context) {
                getTokenContainer().cleanExpiredTokens();
                return null;
            }
        }.executeWith(chromatticLifeCycle);
    }

    @Override
    public long size() {
        return new TokenTask<Long>() {
            @Override
            protected Long execute(SessionContext context) {
                return (long) getTokenContainer().size();
            }
        }.executeWith(chromatticLifeCycle);
    }

    public List<TokenData> getAll(long offset, long limit) {
        return new TokenTask<List<TokenData>>() {
            @Override
            protected List<TokenData> execute(SessionContext context) {
                List<TokenData> result = new ArrayList<>();
                QueryResult<TokenEntry> entries = this.findAll(offset, limit);
                if (entries != null && entries.size() > 0) {
                    while (entries.hasNext()) {
                        TokenEntry en = entries.next();
                        HashedToken hashedToken = getMixin(en, HashedToken.class);
                        if (hashedToken != null && hashedToken.getHashedToken() != null) {
                            result.add(new TokenData(en.getId(), hashedToken.getHashedToken(),
                                    new Credentials(en.getUserName(), en.getPassword()), en.getExpirationTime()));
                        }
                    }
                }

                return result;
            }
        }.executeWith(chromatticLifeCycle);
    }

    /**
     * Wraps token store logic conveniently.
     *
     * @param <V> the return type
     */
    private abstract class TokenTask<V> extends ContextualTask<V> {

        protected final TokenContainer getTokenContainer() {
            SessionContext ctx = chromatticLifeCycle.getContext();
            ChromatticSession session = ctx.getSession();
            return session.findByPath(TokenContainer.class, lifecycleName);
        }

        protected final <A> A getMixin(Object o, Class<A> type) {
            SessionContext ctx = chromatticLifeCycle.getContext();
            ChromatticSession session = ctx.getSession();
            return session.getEmbedded(o, type);
        }

        protected final QueryResult<TokenEntry> findTokensOfUser(String user) {
            SessionContext ctx = chromatticLifeCycle.getContext();
            ChromatticSession session = ctx.getSession();
            TokenContainer tokenContainer = getTokenContainer();

            String statement = new StringBuilder(128).append("jcr:path LIKE '").append(session.getPath(tokenContainer))
                    .append("/%'").append(" AND username='").append(Utils.queryEscape(user)).append("'").toString();
            return session.createQueryBuilder(TokenEntry.class).where(statement).get().objects();
        }

        protected final QueryResult<TokenEntry> findAll(long offset, long limit) {
            SessionContext ctx = chromatticLifeCycle.getContext();
            ChromatticSession session = ctx.getSession();
            TokenContainer tokenContainer = getTokenContainer();

            if (tokenContainer == null) {
                return null;
            }
            String statement = new StringBuilder(128).append("jcr:path LIKE '").append(session.getPath(tokenContainer))
                    .append("/%'").toString();
            return session.createQueryBuilder(TokenEntry.class).where(statement).get().objects(offset, limit);
        }
    }
}
