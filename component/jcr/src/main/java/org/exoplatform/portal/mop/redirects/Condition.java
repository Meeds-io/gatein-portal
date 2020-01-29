/******************************************************************************
 * JBoss, a division of Red Hat                                               *
 * Copyright 2011, Red Hat Middleware, LLC, and individual                    *
 * contributors as indicated by the @authors tag. See the                     *
 * copyright.txt in the distribution for a full listing of                    *
 * individual contributors.                                                   *
 *                                                                            *
 * This is free software; you can redistribute it and/or modify it            *
 * under the terms of the GNU Lesser General Public License as                *
 * published by the Free Software Foundation; either version 2.1 of           *
 * the License, or (at your option) any later version.                        *
 *                                                                            *
 * This software is distributed in the hope that it will be useful,           *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU           *
 * Lesser General Public License for more details.                            *
 *                                                                            *
 * You should have received a copy of the GNU Lesser General Public           *
 * License along with this software; if not, write to the Free                *
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA         *
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.                   *
 ******************************************************************************/
package org.exoplatform.portal.mop.redirects;

import java.util.List;
import java.util.Map;

import org.chromattic.api.annotations.Create;
import org.chromattic.api.annotations.OneToMany;
import org.chromattic.api.annotations.PrimaryType;
import org.chromattic.api.annotations.Property;

/**
 * @author <a href="mailto:mwringe@redhat.com">Matt Wringe</a>
 * @version $Revision$
 */
@PrimaryType(name = "gtn:redirectCondition")
public abstract class Condition {

    @Property(name = "gtn:redirectConditionName")
    public abstract String getName();

    public abstract void setName(String redirectName);

    @Property(name = "gtn:redirectConditionUASContains")
    public abstract List<String> getUserAgentContains();

    public abstract void setUserAgentContains(List<String> userAgentContains);

    @Property(name = "gtn:redirectConditionUASDoesNotContain")
    public abstract List<String> getUserAgentDoesNotContain();

    public abstract void setUserAgentDoesNotContain(List<String> userAgentDoesNotContain);

    @OneToMany
    public abstract Map<String, DeviceProperty> getDeviceProperties();

    @Create
    public abstract DeviceProperty createDeviceProperty();

}
