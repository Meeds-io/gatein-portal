/**
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.portal.jdbc.service;

import java.util.LinkedList;
import java.util.List;

import org.apache.shindig.gadgets.oauth.BasicOAuthStoreTokenIndex;
import org.apache.shindig.gadgets.oauth.OAuthStore.TokenInfo;
import org.exoplatform.commons.chromattic.ChromatticManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.gadget.core.GadgetToken;
import org.exoplatform.portal.jdbc.dao.TokenDAO;
import org.exoplatform.portal.jdbc.entity.GadgetTokenEntity;
import org.gatein.wci.security.Credentials;

public class GadgetTokenInfoService extends
                                    org.exoplatform.portal.gadget.core.GadgetTokenInfoService {

  private TokenDAO tokenDAO;

  public GadgetTokenInfoService(InitParams initParams, TokenDAO tokenDAO)
      throws Exception {
    super(initParams, new ChromatticManager(null));
    this.tokenDAO = tokenDAO;
  }

  public GadgetToken createToken(final BasicOAuthStoreTokenIndex tokenKey,
                                 final TokenInfo tokenInfo) {
    long expirationTimeMillis = System.currentTimeMillis() + validityMillis;

    GadgetTokenEntity entry = null;
    if (entry == null) {
      entry = new GadgetTokenEntity();
      tokenDAO.create(entry);

    }
    entry.setGadgetUri(tokenKey.getGadgetUri());
    entry.setModuleId(tokenKey.getModuleId());
    entry.setServiceName(tokenKey.getServiceName());
    entry.setTokenName(tokenKey.getTokenName());
    entry.setUserId(tokenKey.getUserId());

    entry.setAccessToken(tokenInfo.getAccessToken());
    entry.setTokenSecret(tokenInfo.getTokenSecret());
    entry.setSessionHandle(tokenInfo.getSessionHandle() == null ? ""
                                                                : tokenInfo.getSessionHandle());
    entry.setTokenExpireMillis(expirationTimeMillis);
    tokenDAO.update(entry);
    return buildGadgetToken(entry);
  }

  private GadgetToken buildGadgetToken(GadgetTokenEntity entry) {
    return new GadgetToken(entry.getAccessToken(),
                           entry.getTokenSecret(),
                           entry.getSessionHandle(),
                           entry.getTokenExpireMillis());
  }

  @Override
  public GadgetToken getToken(final BasicOAuthStoreTokenIndex key) {
    return buildGadgetToken(tokenDAO.findByKey(key));
  }

  @Override
  public GadgetToken deleteToken(final BasicOAuthStoreTokenIndex key) {
    GadgetToken token = getToken(key);
    if (token != null) {
      tokenDAO.deleteByKey(key);
    }
    return token;
  }

  @Override
  public BasicOAuthStoreTokenIndex[] getAllTokens() {
    List<BasicOAuthStoreTokenIndex> keys = new LinkedList<BasicOAuthStoreTokenIndex>();
    List<GadgetTokenEntity> entities = tokenDAO.findAll();
    for (GadgetTokenEntity entity : entities) {
      keys.add(buildTokenKey(entity));
    }

    return keys.toArray(new BasicOAuthStoreTokenIndex[keys.size()]);
  }

  private BasicOAuthStoreTokenIndex buildTokenKey(GadgetTokenEntity entity) {
    BasicOAuthStoreTokenIndex key = new BasicOAuthStoreTokenIndex();
    key.setGadgetUri(entity.getGadgetUri());
    key.setModuleId(entity.getModuleId());
    key.setServiceName(entity.getServiceName());
    key.setTokenName(entity.getTokenName());
    key.setUserId(entity.getUserId());
    return key;
  }

  @Override
  public long size() {
    return tokenDAO.count();
  }

  public String createToken(Credentials credentials) throws IllegalArgumentException,
                                                     NullPointerException {
    return null;
  }

  @Override
  protected BasicOAuthStoreTokenIndex decodeKey(String stringKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void start() {
  }

}
