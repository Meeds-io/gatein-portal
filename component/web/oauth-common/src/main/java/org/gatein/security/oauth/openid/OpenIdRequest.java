/*
 * JBoss, a division of Red Hat
 * Copyright 2013, Red Hat Middleware, LLC, and individual
 * contributors as indicated by the @authors tag. See the
 * copyright.txt in the distribution for a full listing of
 * individual contributors.
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

package org.gatein.security.oauth.openid;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.security.oauth.exception.OAuthException;
import org.gatein.security.oauth.exception.OAuthExceptionCode;
import org.json.JSONException;

abstract class OpenIdRequest<T> {

    private static Logger log = LoggerFactory.getLogger(OpenIdRequest.class);

    protected abstract URL createURL() throws IOException;
    
    protected abstract T invokeRequest(Map<String, String>  params) throws IOException, JSONException;
    
    protected abstract T parseResponse(String httpResponse) throws JSONException;

    public T executeRequest(Map<String, String>  params) {
        try {
            return invokeRequest(params);
        } catch (JSONException e) {
            throw new OAuthException(OAuthExceptionCode.IO_ERROR, e);
        } catch (IOException e) {
            throw new OAuthException(OAuthExceptionCode.IO_ERROR, e);
        }
    }
}
