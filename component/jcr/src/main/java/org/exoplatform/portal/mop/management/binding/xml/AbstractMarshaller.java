/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.exoplatform.portal.mop.management.binding.xml;

import static org.gatein.common.xml.stax.navigator.Exceptions.*;
import static org.gatein.common.xml.stax.navigator.StaxNavUtils.*;
import static org.gatein.common.xml.stax.writer.StaxWriterUtils.writeOptionalContent;
import static org.gatein.common.xml.stax.writer.StaxWriterUtils.writeOptionalElement;

import java.util.*;

import javax.xml.stream.XMLStreamException;

import org.gatein.common.xml.stax.navigator.Exceptions;
import org.gatein.common.xml.stax.writer.StaxWriter;
import org.gatein.common.xml.stax.writer.WritableValueTypes;
import org.gatein.management.api.binding.Marshaller;
import org.staxnav.StaxNavigator;
import org.staxnav.ValueType;

import org.exoplatform.container.*;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.model.*;
import org.exoplatform.portal.config.serialize.JibxArraySerialize;
import org.exoplatform.portal.pom.spi.portlet.*;

/**
 * @author <a href="mailto:nscavell@redhat.com">Nick Scavelli</a>
 * @version $Revision$
 */
public abstract class AbstractMarshaller<T> implements Marshaller<T> {
    protected void marshalModelObject(StaxWriter<Element> writer, ModelObject modelObject) throws XMLStreamException {
        if (modelObject instanceof Application) {
            Application<?> application = (Application<?>) modelObject;
            ApplicationType<?> type = application.getType();
            if (ApplicationType.PORTLET == type) {
                marshalPortletApplication(writer, safeCast(application, Portlet.class));
            }
        } else if (modelObject instanceof Page) {
            // marshalPageData(writer, (PageData) componentData);
            throw new XMLStreamException("Unexpected PageData object. Storage id: " + modelObject.getStorageId());
        } else if (modelObject instanceof Container) {
            marshalContainer(writer, (Container) modelObject);
        } else if (modelObject instanceof PageBody) {
            writer.writeStartElement(Element.PAGE_BODY).writeEndElement();
        } else {
            throw new XMLStreamException("Unknown ComponentData type " + modelObject);
        }
    }

    protected void marshalContainer(StaxWriter<Element> writer, Container container) throws XMLStreamException {
        writer.writeStartElement(Element.CONTAINER);

        writeOptionalAttribute(writer, Attribute.ID, container.getId());
        writeOptionalAttribute(writer, Attribute.TEMPLATE, container.getTemplate());
        writeOptionalAttribute(writer, Attribute.WIDTH, container.getWidth());
        writeOptionalAttribute(writer, Attribute.HEIGHT, container.getHeight());

        writeOptionalElement(writer, Element.NAME, container.getName());
        writeOptionalElement(writer, Element.TITLE, container.getTitle());
        writeOptionalElement(writer, Element.ICON, container.getIcon());
        writeOptionalElement(writer, Element.DESCRIPTION, container.getDescription());

        marshalAccessPermissions(writer, container.getAccessPermissions());

        writeOptionalElement(writer, Element.FACTORY_ID, container.getFactoryId());

        marshalPermissions(writer, Element.MOVE_APPLICATIONS_PERMISSIONS, container.getMoveAppsPermissions());
        marshalPermissions(writer, Element.MOVE_CONTAINERS_PERMISSIONS, container.getMoveContainersPermissions());

        List<ModelObject> children = container.getChildren();
        for (ModelObject child : children) {
            marshalModelObject(writer, child);
        }

        writer.writeEndElement(); // End of container element
    }

    protected Container unmarshalContainer(StaxNavigator<Element> navigator) throws XMLStreamException {
        Container container = new Container();
        container.setId(navigator.getAttribute(Attribute.ID.getLocalName()));
        container.setTemplate(navigator.getAttribute(Attribute.TEMPLATE.getLocalName()));
        container.setWidth(navigator.getAttribute(Attribute.WIDTH.getLocalName()));
        container.setHeight(navigator.getAttribute(Attribute.HEIGHT.getLocalName()));

        Element current = navigator.child();
        while (current != null) {
            switch (current) {
                case NAME:
                    container.setName(navigator.getContent());
                    current = navigator.sibling();
                    break;
                case TITLE:
                    container.setTitle(navigator.getContent());
                    current = navigator.sibling();
                    break;
                case ICON:
                    container.setIcon(navigator.getContent());
                    current = navigator.sibling();
                    break;
                case DESCRIPTION:
                    container.setDescription(navigator.getContent());
                    current = navigator.sibling();
                    break;
                case ACCESS_PERMISSIONS:
                    container.setAccessPermissions(unmarshalAccessPermissions(navigator, false));
                    current = navigator.sibling();
                    break;
                case MOVE_APPLICATIONS_PERMISSIONS:
                    container.setMoveAppsPermissions(unmarshalPermissions(navigator, false));
                    current = navigator.sibling();
                    break;
                case MOVE_CONTAINERS_PERMISSIONS:
                    container.setMoveContainersPermissions(unmarshalPermissions(navigator, false));
                    current = navigator.sibling();
                    break;
                case FACTORY_ID:
                    container.setFactoryId(navigator.getContent());
                    current = navigator.sibling();
                    break;
                case CONTAINER:
                    if (container.getChildren() == null) {
                        container.setChildren(new ArrayList<ModelObject>());
                    }
                    container.getChildren().add(unmarshalContainer(navigator.fork()));
                    current = navigator.sibling();
                    break;
                case PORTLET_APPLICATION:
                    if (container.getChildren() == null) {
                        container.setChildren(new ArrayList<ModelObject>());
                    }
                    container.getChildren().add(unmarshalPortletApplication(navigator.fork()));
                    current = navigator.sibling();
                    break;
                case PAGE_BODY:
                    if (container.getChildren() == null) {
                        container.setChildren(new ArrayList<ModelObject>());
                    }
                    container.getChildren().add(new PageBody());
                    current = navigator.sibling();
                    break;
                case UNKNOWN:
                    throw unknownElement(navigator);
                default:
                    throw unexpectedElement(navigator);
            }
        }

        return container;
    }

    protected void marshalPortletApplication(StaxWriter<Element> writer, Application<Portlet> portletApplication)
            throws XMLStreamException {
        writer.writeStartElement(Element.PORTLET_APPLICATION).writeStartElement(Element.PORTLET);

        // Marshal ApplicationState
        ApplicationState<Portlet> state = portletApplication.getState();

        // Marshal application state
        String contentId;
        Portlet portlet;
        // If transient we have all the information we need
        if (state instanceof TransientApplicationState) {
            TransientApplicationState<Portlet> transientApplicationState = (TransientApplicationState<Portlet>) state;
            contentId = transientApplicationState.getContentId();
            portlet = transientApplicationState.getContentState();
        } else {
            // The only way to retrieve the information if the state is not transient is if we're within the portal context
            ExoContainer container = ExoContainerContext.getCurrentContainer();
            if (container instanceof PortalContainer) {
                DataStorage dataStorage = (DataStorage) container.getComponentInstanceOfType(DataStorage.class);
                try {
                    portlet = dataStorage.load(state, ApplicationType.PORTLET);
                } catch (Exception e) {
                    throw new XMLStreamException("Could not obtain portlet state.");
                }

                try {
                    contentId = dataStorage.getId(state);
                } catch (Exception e) {
                    throw new XMLStreamException("Could not obtain contentId.", e);
                }
            } else {
                throw new XMLStreamException("Cannot marshal application state " + state
                        + " outside the context of the portal.");
            }
        }

        // Marshal portlet application id
        if (contentId == null)
            throw new XMLStreamException("Portlet application ID was null.");
        writer.writeElement(Element.APPLICATION_REF, contentId.substring(0, contentId.indexOf("/")));
        writer.writeElement(Element.PORTLET_REF, contentId.substring(contentId.indexOf("/") + 1, contentId.length()));

        // Marshal preferences
        if (portlet != null) {
            boolean prefsWritten = false;
            for (Preference preference : portlet) {
                if (!prefsWritten) {
                    writer.writeStartElement(Element.PREFERENCES);
                    prefsWritten = true;
                }

                writer.writeStartElement(Element.PREFERENCE);
                writer.writeElement(Element.NAME, preference.getName());
                for (String value : preference.getValues()) {
                    writeOptionalContent(writer, Element.PREFERENCE_VALUE, value);
                }
                writer.writeElement(Element.PREFERENCE_READONLY, WritableValueTypes.BOOLEAN, preference.isReadOnly());
                writer.writeEndElement(); // End of preference
            }
            if (prefsWritten) {
                writer.writeEndElement(); // End of preferences
            }
        }
        writer.writeEndElement(); // End of portlet

        marshalApplication(writer, portletApplication);

        writer.writeEndElement(); // End of portlet-application
    }

    protected Application<?> unmarshalPortletApplication(StaxNavigator<Element> navigator) throws XMLStreamException {
        Application<?> application;
        switch (navigator.child()) {
            case PORTLET:
                ApplicationState<Portlet> state = unmarshalPortletApplicationState(navigator.fork());
                Application<Portlet> portlet = new Application<Portlet>(ApplicationType.PORTLET);
                portlet.setState(state);
                application = portlet;
                break;
            case WSRP:
                throw new UnsupportedOperationException("We dont support wsrp portlet any more");
            case UNKNOWN:
                throw unexpectedElement(navigator);
            default:
                throw unexpectedElement(navigator);
        }

        return unmarshalApplication(navigator, application);
    }

    private ApplicationState<Portlet> unmarshalPortletApplicationState(StaxNavigator<Element> navigator)
            throws XMLStreamException {
        // Application name
        requiresChild(navigator, Element.APPLICATION_REF);
        String applicationRef = getRequiredContent(navigator, true);

        // Portlet name
        requiresSibling(navigator, Element.PORTLET_REF);
        String portletRef = getRequiredContent(navigator, true);

        // Preferences
        PortletBuilder portletBuilder = null;
        if (navigator.sibling() == Element.PREFERENCES && navigator.child() == Element.PREFERENCE) {
            portletBuilder = new PortletBuilder();
            for (StaxNavigator<Element> fork : navigator.fork(Element.PREFERENCE)) {
                // Preference name
                requiresChild(fork, Element.NAME);
                String prefName = getRequiredContent(fork, false);

                // Preference values
                List<String> values = null;
                while (fork.sibling() == Element.PREFERENCE_VALUE) {
                    if (values == null)
                        values = new ArrayList<String>();
                    values.add(getContent(fork, false));
                }
                if (values == null) {
                    values = Collections.singletonList(null);
                }

                // Preference readonly
                Boolean readOnly = null;
                if (fork.getName() == Element.PREFERENCE_READONLY) {
                    readOnly = parseRequiredContent(fork, ValueType.BOOLEAN);
                }

                // Ensure nothing is left.
                if (fork.next() != null) {
                    throw unexpectedElement(fork);
                }

                if (readOnly == null) {
                    portletBuilder.add(prefName, values);
                } else {
                    portletBuilder.add(prefName, values, readOnly);
                }
            }
        }

        TransientApplicationState<Portlet> state = new TransientApplicationState<Portlet>(applicationRef + "/" + portletRef);
        if (portletBuilder != null) {
            state.setContentState(portletBuilder.build());
        }

        return state;
    }

    protected void marshalApplication(StaxWriter<Element> writer, Application<?> application) throws XMLStreamException {
        // Theme, Title
        writeOptionalElement(writer, Element.THEME, application.getTheme());
        writeOptionalElement(writer, Element.TITLE, application.getTitle());

        // Access Permissions
        marshalAccessPermissions(writer, application.getAccessPermissions());

        // common application elements
        writeOptionalElement(writer, Element.SHOW_INFO_BAR, String.valueOf(application.getShowInfoBar()));
        writeOptionalElement(writer, Element.SHOW_APPLICATION_STATE, String.valueOf(application.getShowApplicationState()));
        writeOptionalElement(writer, Element.SHOW_APPLICATION_MODE, String.valueOf(application.getShowApplicationMode()));

        // Description, Icon
        writeOptionalElement(writer, Element.DESCRIPTION, application.getDescription());
        writeOptionalElement(writer, Element.ICON, application.getIcon());

        // Width & Height
        writeOptionalElement(writer, Element.WIDTH, application.getWidth());
        writeOptionalElement(writer, Element.HEIGHT, application.getHeight());
    }

    protected <S> Application<S> unmarshalApplication(StaxNavigator<Element> navigator, Application<S> application)
            throws XMLStreamException {
        boolean showInfoBarParsed = false;

        Element current = navigator.sibling();
        while (current != null) {
            switch (current) {
                case THEME:
                    application.setTheme(navigator.getContent());
                    current = navigator.sibling();
                    break;
                case TITLE:
                    application.setTitle(navigator.getContent());
                    current = navigator.sibling();
                    break;
                case ACCESS_PERMISSIONS:
                    application.setAccessPermissions(unmarshalAccessPermissions(navigator, true));
                    current = navigator.sibling();
                    break;
                case SHOW_INFO_BAR:
                    application.setShowInfoBar(parseRequiredContent(navigator, ValueType.BOOLEAN));
                    showInfoBarParsed = true;
                    current = navigator.sibling();
                    break;
                case SHOW_APPLICATION_STATE:
                    application.setShowApplicationState(navigator.parseContent(ValueType.BOOLEAN));
                    current = navigator.sibling();
                    break;
                case SHOW_APPLICATION_MODE:
                    application.setShowApplicationMode(navigator.parseContent(ValueType.BOOLEAN));
                    current = navigator.sibling();
                    break;
                case DESCRIPTION:
                    application.setDescription(navigator.getContent());
                    current = navigator.sibling();
                    break;
                case ICON:
                    application.setIcon(navigator.getContent());
                    current = navigator.sibling();
                    break;
                case WIDTH:
                    application.setWidth(navigator.getContent());
                    current = navigator.sibling();
                    break;
                case HEIGHT:
                    application.setHeight(navigator.getContent());
                    current = navigator.sibling();
                    break;
                case UNKNOWN:
                    throw unknownElement(navigator);
                default:
                    throw unexpectedElement(navigator);
            }
        }

        // TODO: We should raise this exception as soon as we know so location is accurate
        if (application.getAccessPermissions() == null)
            throw expectedElement(navigator, Element.ACCESS_PERMISSIONS);
        if (!showInfoBarParsed)
            throw expectedElement(navigator, Element.SHOW_INFO_BAR);

        return application;
    }

    protected void marshalAccessPermissions(StaxWriter<Element> writer, String[] accessPermissions) throws XMLStreamException {
        accessPermissions = (accessPermissions == null || accessPermissions.length == 0) ? null : accessPermissions;
        writeOptionalElement(writer, Element.ACCESS_PERMISSIONS, DelimitedValueType.SEMI_COLON, accessPermissions);
    }

    protected void marshalPermissions(StaxWriter<Element> writer, Element element, String[] accessPermissions) throws XMLStreamException {
        writeOptionalElement(writer, element, JibxArraySerialize.serializePermissions(accessPermissions));
    }

    protected String[] unmarshalAccessPermissions(StaxNavigator<Element> navigator, boolean required) throws XMLStreamException {
        if (required) {
            return Utils.tidyUp(parseRequiredContent(navigator, DelimitedValueType.SEMI_COLON));
        } else {
            return Utils.tidyUp(parseContent(navigator, DelimitedValueType.SEMI_COLON, null));
        }
    }

    protected String[] unmarshalPermissions(StaxNavigator<Element> navigator, boolean required) throws XMLStreamException {
        String content = parseContent(navigator, DelimitedValueType.TRIMMED_STRING, null);
        if (required && content == null) {
            throw Exceptions.contentRequired(navigator);
        } else {
            return JibxArraySerialize.deserializePermissions(content);
        }
    }

    protected void marshalEditPermission(StaxWriter<Element> writer, String editPermission) throws XMLStreamException {
        writeOptionalElement(writer, Element.EDIT_PERMISSION, editPermission);
    }

    protected String unmarshalEditPermission(StaxNavigator<Element> navigator) throws XMLStreamException {
        return getContent(navigator, true);
    }

    protected void writeGateinObjectsNamespace(StaxWriter<Element> writer) throws XMLStreamException {
        Utils.writeGateinObjectsNamespace(writer);
    }

    @SuppressWarnings("unchecked")
    private <S> Application<S> safeCast(Application<?> application, Class<S> stateClass) {
        return (Application<S>) application;
    }

    private static void writeOptionalAttribute(StaxWriter<?> writer, Attribute attribute, String value) throws XMLStreamException {
        if (value == null)
            return;

        writer.writeAttribute(attribute.getLocalName(), value);
    }

    private static enum Attribute {
        ID("id"), TEMPLATE("template"), WIDTH("width"), HEIGHT("height");

        private final String name;

        Attribute(final String name) {
            this.name = name;
        }

        /**
         * Get the local name of this element.
         *
         * @return the local name
         */
        public String getLocalName() {
            return name;
        }
    }
}
