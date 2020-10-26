/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.portal.resource.compressor;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;


/**
 * ResourceCompressor acts as a router of compression to compressor plugin
 *
 * @author <a href="mailto:hoang281283@gmail.com">Minh Hoang TO</a> Aug 19, 2010
 */

public interface ResourceCompressor {
    boolean isSupported(ResourceType resourceType);

    void compress(Reader input, Writer output, ResourceType resourceType) throws ResourceCompressorException, IOException;

    String compress(String input, ResourceType resourceType) throws ResourceCompressorException, IOException;
}
