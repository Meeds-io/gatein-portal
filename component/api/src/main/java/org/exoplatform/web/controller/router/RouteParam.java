/*
 * Copyright (C) 2010 eXo Platform SAS.
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

package org.exoplatform.web.controller.router;

import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.controller.metadata.RouteParamDescriptor;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
class RouteParam extends Param {

    static RouteParam create(RouteParamDescriptor descriptor) {
        if (descriptor == null) {
            throw new NullPointerException("No null descriptor accepted");
        }

        //
        return new RouteParam(descriptor.getQualifiedName(), descriptor.getValue());
    }

    /** . */
    final String value;

    RouteParam(QualifiedName name, String value) {
        super(name);

        //
        if (value == null) {
            throw new NullPointerException("No null value accepted");
        }

        //
        this.value = value;
    }

    @Override
    public String toString() {
        return "RouteParam[name=" + name + ",value=" + value + "]";
    }
}
