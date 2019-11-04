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

package org.exoplatform.web.handler;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.download.DownloadResource;
import org.exoplatform.download.DownloadService;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.WebRequestHandler;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;

/**
 * Created by The eXo Platform SARL Author : LeBienThuy thuy.le@exoplatform.com Dec 9, 2006
 */
public class DownloadHandler extends WebRequestHandler {

    private final Logger log = LoggerFactory.getLogger(DownloadHandler.class);

    public String getHandlerName() {
        return "download";
    }

    @Override
    public boolean execute(ControllerContext context) throws Exception {
        execute(context.getController(), context.getRequest(), context.getResponse());
        return true;
    }

    public void execute(WebAppController controller, HttpServletRequest req, HttpServletResponse res) throws Exception {
        String resourceId = req.getParameter("resourceId");
        boolean remove = !"false".equals(req.getParameter("remove"));
        res.setHeader("Cache-Control", "private max-age=600, s-maxage=120");
        ExoContainer container = ExoContainerContext.getCurrentContainer();
        DownloadService dservice = container.getComponentInstanceOfType(DownloadService.class);
        DownloadResource dresource = dservice.getDownloadResource(resourceId, remove);
        if (dresource == null) {
            res.setContentType("text/plain");
            res.getWriter().write("NO DOWNDLOAD RESOURCE CONTENT  OR YOU DO NOT HAVE THE RIGHT TO ACCESS THE CONTENT");
            return;
        }
        String userAgent = req.getHeader("User-Agent");
        if (dresource.getDownloadName() != null) {
            if (userAgent != null && userAgent.contains("Firefox")) {
                res.setHeader("Content-Disposition",
                        "attachment; filename*=utf-8''" + URLEncoder.encode(dresource.getDownloadName(), "UTF-8").replace("+", "%20") + "");
            } else {
                res.setHeader("Content-Disposition",
                        "attachment;filename=\"" + URLEncoder.encode(dresource.getDownloadName(), "UTF-8").replace("+", "%20") + "\"");
            }
        }
        res.setContentType(dresource.getResourceMimeType());
        InputStream is = dresource.getInputStream();
        try {
            optimalRead(is, res.getOutputStream());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            is.close();
        }
    }

    @Override
    protected boolean getRequiresLifeCycle() {
        return false;
    }

    private static void optimalRead(InputStream is, OutputStream os) throws Exception {
        int bufferLength = 1024; // TODO: Better to compute bufferLength in term of -Xms, -Xmx properties
        int readLength = 0;
        while (readLength > -1) {
            byte[] chunk = new byte[bufferLength];
            readLength = is.read(chunk);
            if (readLength > 0) {
                os.write(chunk, 0, readLength);
            }
        }
    }
}
