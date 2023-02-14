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

package org.exoplatform.portal.webui.application;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.gatein.pc.api.PortletContext;
import org.gatein.pc.api.PortletInvoker;
import org.gatein.pc.api.StatefulPortletContext;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.portal.config.model.ApplicationState;
import org.exoplatform.portal.config.model.ApplicationType;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.pc.ExoPortletState;
import org.exoplatform.portal.pc.ExoPortletStateType;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.portal.pom.spi.portlet.PortletBuilder;
import org.exoplatform.portal.pom.spi.portlet.Preference;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public abstract class ModelAdapter<S, C extends Serializable> {
    private static final String LOCAL_STATE_ID = PortletContext.LOCAL_CONSUMER_CLONE.getId();

    public static <S, C extends Serializable, I> ModelAdapter<S, C> getAdapter(ApplicationType<S> type) {
        if (type == ApplicationType.PORTLET) {
            @SuppressWarnings("unchecked")
            ModelAdapter<S, C> adapter = (ModelAdapter<S, C>) PORTLET;
            return adapter;
        } else {
            throw new AssertionError();
        }
    }

    /** . */
    private static final ModelAdapter<Portlet, ExoPortletState> PORTLET = new ModelAdapter<Portlet, ExoPortletState>() {

        @Override
        public StatefulPortletContext<ExoPortletState> getPortletContext(ExoContainer container, String applicationId,
                ApplicationState<Portlet> applicationState) throws Exception {
            LayoutService layoutService = container.getComponentInstanceOfType(LayoutService.class);
            Portlet preferences = layoutService.load(applicationState, ApplicationType.PORTLET);
            PortletContext producerOfferedPortletContext = getProducerOfferedPortletContext(applicationId);
            ExoPortletState map = new ExoPortletState(producerOfferedPortletContext.getId());
            if (preferences != null) {
                for (Preference pref : preferences) {
                    map.getState().put(pref.getName(), pref.getValues());
                }
            }
            return StatefulPortletContext.create(LOCAL_STATE_ID, ExoPortletStateType.getInstance(), map);
        }

        @Override
        public ApplicationState<Portlet> update(ExoContainer container, ExoPortletState updateState,
                ApplicationState<Portlet> applicationState) throws Exception {
            // Compute new preferences
            PortletBuilder builder = new PortletBuilder();
            for (Map.Entry<String, List<String>> entry : updateState.getState().entrySet()) {
                builder.add(entry.getKey(), entry.getValue());
            }

            if (applicationState instanceof TransientApplicationState) {
                TransientApplicationState<Portlet> transientState = (TransientApplicationState<Portlet>) applicationState;
                transientState.setContentState(builder.build());
                return transientState;
            } else {
                LayoutService layoutService = container.getComponentInstanceOfType(LayoutService.class);
                return layoutService.save(applicationState, builder.build());
            }
        }

        @Override
        public PortletContext getProducerOfferedPortletContext(String applicationState) {
            int indexOfSeparator = applicationState.lastIndexOf("/");
            String appName = applicationState.substring(0, indexOfSeparator);
            String portletName = applicationState.substring(indexOfSeparator + 1);
            return PortletContext.reference(PortletInvoker.LOCAL_PORTLET_INVOKER_ID,
                    PortletContext.createPortletContext(appName, portletName));
        }

        @Override
        public Portlet getState(ExoContainer container, ApplicationState<Portlet> applicationState) throws Exception {
            if (applicationState instanceof TransientApplicationState) {
                TransientApplicationState<Portlet> transientState = (TransientApplicationState<Portlet>) applicationState;
                Portlet pref = transientState.getContentState();
                if (pref == null) {
                    pref = new Portlet();
                }
                return pref;
            } else {
                LayoutService layoutService = container.getComponentInstanceOfType(LayoutService.class);
                Portlet pref = layoutService.load(applicationState, ApplicationType.PORTLET);
                if (pref == null) {
                    pref = new Portlet();
                }
                return pref;
            }
        }

        @Override
        public ExoPortletState getStateFromModifiedContext(PortletContext originalPortletContext,
                PortletContext modifiedPortletContext) {
            if (modifiedPortletContext != null && modifiedPortletContext instanceof StatefulPortletContext) {
                StatefulPortletContext statefulContext = (StatefulPortletContext) modifiedPortletContext;
                if (statefulContext.getState() instanceof ExoPortletState) {
                    return (ExoPortletState) statefulContext.getState();
                }
            }
            return null;
        }

        @Override
        public ExoPortletState getstateFromClonedContext(PortletContext originalPortletContext,
                PortletContext clonedPortletContext) {
            if (clonedPortletContext != null && clonedPortletContext instanceof StatefulPortletContext) {
                StatefulPortletContext statefulContext = (StatefulPortletContext) clonedPortletContext;
                if (statefulContext.getState() instanceof ExoPortletState) {
                    return (ExoPortletState) statefulContext.getState();
                }
            }
            return null;
        }
    };

    /**
     * StatefulPortletContext returned by getPortletContext is actually of type PortletStateType.OPAQUE so that it can be
     * properly handled in WSRP... This model needs to be revisited if we want to properly support consumer-side state
     * management. See GTNPORTAL-736.
     */

    public abstract PortletContext getProducerOfferedPortletContext(String applicationId);

    public abstract StatefulPortletContext<C> getPortletContext(ExoContainer container, String applicationId,
            ApplicationState<S> applicationState) throws Exception;

    public abstract ApplicationState<S> update(ExoContainer container, C updateState, ApplicationState<S> applicationState)
            throws Exception;

    /**
     * Returns the state of the gadget as preferences or null if the preferences cannot be edited as such.
     *
     * @param container the container
     * @param applicationState the application state
     * @return the preferences
     * @throws Exception any exception
     */
    public abstract Portlet getState(ExoContainer container, ApplicationState<S> applicationState) throws Exception;

    /**
     * Extracts the state based on what the current PortletContext is and the new modified PortletContext.
     *
     * @param originalPortletContext The current PortletContext for the Portlet
     * @param modifiedPortletContext The new modified PortletContext
     * @return
     */
    public abstract C getStateFromModifiedContext(PortletContext originalPortletContext, PortletContext modifiedPortletContext);

    /**
     * Extracts the state based on what the current PortletContext is and the new cloned PortletContext
     *
     * @param originalPortletContext The current PortletContext for the Portlet
     * @param clonedPortletContext The new cloned PortletContext
     * @return
     */
    public abstract C getstateFromClonedContext(PortletContext originalPortletContext, PortletContext clonedPortletContext);

}
