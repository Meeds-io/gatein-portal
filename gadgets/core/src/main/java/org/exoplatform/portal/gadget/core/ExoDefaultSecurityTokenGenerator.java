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

package org.exoplatform.portal.gadget.core;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.shindig.auth.AbstractSecurityToken;
import org.apache.shindig.auth.BlobCrypterSecurityToken;
import org.apache.shindig.auth.BlobCrypterSecurityTokenCodec;
import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypterException;
import org.apache.shindig.common.util.TimeSource;
import org.exoplatform.web.application.RequestContext;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;

import javax.inject.Inject;

public class ExoDefaultSecurityTokenGenerator implements SecurityTokenGenerator {
    private final TimeSource timeSource;

    /** . */
    private final Logger log = LoggerFactory.getLogger(ExoDefaultSecurityTokenGenerator.class);

    public ExoDefaultSecurityTokenGenerator() throws Exception {
        this.timeSource = new TimeSource();
    }

    protected String createToken(String gadgetURL, String owner, String viewer, Long moduleId, String container) {
        try {
            BlobCrypter blobCrypter = getBlobCrypter();

            Map<String, String> values = new HashMap<>();
            values.put(BlobCrypterSecurityToken.Keys.APP_URL.getKey(), gadgetURL);
            values.put(BlobCrypterSecurityToken.Keys.MODULE_ID.getKey(), String.valueOf(moduleId));
            values.put(BlobCrypterSecurityToken.Keys.OWNER.getKey(), owner);
            values.put(BlobCrypterSecurityToken.Keys.VIEWER.getKey(), viewer);
            values.put(BlobCrypterSecurityToken.Keys.TRUSTED_JSON.getKey(), "");
            BlobCrypterSecurityToken t = new BlobCrypterSecurityToken(container, null, null, values);

            return t.getContainer() + ':' + blobCrypter.wrap(t.toMap());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public String createToken(String gadgetURL, Long moduleId) {
        RequestContext context = RequestContext.getCurrentInstance();
        // context.get
        String rUser = context.getRemoteUser();
        String viewer = rUser;

        return createToken(gadgetURL, viewer, rUser, moduleId, "default");
    }

    protected BlobCrypter getBlobCrypter() throws IOException {
        String fileName = getKeyFilePath();
        BasicBlobCrypter c = new BasicBlobCrypter(fileName);
        c.timeSource = timeSource;
        return c;
    }

    /**
     * Method returns a path to the file containing the encryption key
     */
    protected String getKeyFilePath() {
        String keyPath = ExoContainerConfig.getTokenKeyPath();
        File keyFile = null;
        if (keyPath != null) {
            keyFile = new File(keyPath);
        } else {
            keyFile = new File("key.txt");
        }

        return keyFile.getAbsolutePath();
    }
}
