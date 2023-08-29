/*
 * JBoss, a division of Red Hat
 * Copyright 2010, Red Hat Middleware, LLC, and individual
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

package org.exoplatform.portal.webui.application;

import org.exoplatform.commons.utils.Safe;

import org.exoplatform.portal.Constants;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.NoSuchDataException;
import org.exoplatform.portal.webui.page.UIPage;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIMaskWorkspace;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.portal.webui.workspace.UIWorkingWorkspace;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.gatein.common.util.MultiValuedPropertyMap;
import org.gatein.common.util.ParameterValidation;
import org.gatein.pc.api.Mode;
import org.gatein.pc.api.ParametersStateString;
import org.gatein.pc.api.PortletContext;
import org.gatein.pc.api.StateString;
import org.gatein.pc.api.StatefulPortletContext;
import org.gatein.pc.api.invocation.ActionInvocation;
import org.gatein.pc.api.invocation.EventInvocation;
import org.gatein.pc.api.invocation.ResourceInvocation;
import org.gatein.pc.api.invocation.response.ContentResponse;
import org.gatein.pc.api.invocation.response.ErrorResponse;
import org.gatein.pc.api.invocation.response.HTTPRedirectionResponse;
import org.gatein.pc.api.invocation.response.PortletInvocationResponse;
import org.gatein.pc.api.invocation.response.ResponseProperties;
import org.gatein.pc.api.invocation.response.SecurityErrorResponse;
import org.gatein.pc.api.invocation.response.SecurityResponse;
import org.gatein.pc.api.invocation.response.UpdateNavigationalStateResponse;

import javax.portlet.PortletMode;
import javax.portlet.ResourceResponse;
import javax.portlet.WindowState;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/** May 29, 2006 */
public class UIPortletActionListener {

    public static final String PORTLET_EVENTS = "PortletEvents";

    public static final String CHANGE_WINDOW_STATE_EVENT = "PortletChangeWindowStateEvent";
    public static final String CHANGE_PORTLET_MODE_EVENT = "ChangePortletModeEvent";

    protected static Log log = ExoLogger.getLogger("portal:UIPortletActionListener");

    /**
     * The process action listener is called when an ActionURL generated by the portlet container has been invoked by the client
     * <br>
     * The call is delegated to the portlet container iteself using the method portletContainer.processAction(...). It returns
     * an object of type ActionOutput that contains several information such as the next window state and portlet modes (if they
     * have to change) as well as a list of Events to be broadcasted to the other portlets located in the same portal page
     */
    public static class ProcessActionActionListener<S, C extends Serializable, I> extends EventListener<UIPortlet<S, C>> {
        public void execute(Event<UIPortlet<S, C>> event) throws Exception {
            UIPortlet<S, C> uiPortlet = event.getSource();
            PortalRequestContext prcontext = (PortalRequestContext) event.getRequestContext();

            // set the public render parameters from the request before creating the invocation
            HttpServletRequest request = prcontext.getRequest();
            setupPublicRenderParams(uiPortlet, request.getParameterMap());

            // set the navigational state
            String navState = prcontext.getRequestParameter(ExoPortletInvocationContext.NAVIGATIONAL_STATE_PARAM_NAME);
            if (navState != null) {
                uiPortlet.setNavigationalState(ParametersStateString.create(navState));
            }

            //
            ActionInvocation actionInvocation = uiPortlet.create(ActionInvocation.class, prcontext);
            if (actionInvocation == null) {
                return;
            }
            //
            PortletInvocationResponse portletResponse = uiPortlet.invoke(actionInvocation);

            // deal with potential portlet context modifications
            ExoPortletInstanceContext instanceCtx = (ExoPortletInstanceContext) actionInvocation.getInstanceContext();
            if (instanceCtx.getModifiedContext() != null) {
                StatefulPortletContext<C> updatedCtx = (StatefulPortletContext<C>) instanceCtx.getModifiedContext();
                C portletState = uiPortlet.getModifiedState(updatedCtx);
                uiPortlet.update(portletState);
            } else {
                // todo: fix me as this shouldn't probably be done only for the WSRP case
                PortletContext clonedContext = instanceCtx.getClonedContext();
                if (clonedContext != null) {
                    C state = uiPortlet.getClonedState(clonedContext);
                    uiPortlet.update(state);
                }
            }

            if (portletResponse instanceof UpdateNavigationalStateResponse) {
                handleUpdateNavigationalStateResponse((UpdateNavigationalStateResponse) portletResponse, uiPortlet, prcontext);
            } else if (portletResponse instanceof HTTPRedirectionResponse) {
                handleRedirectionResponse((HTTPRedirectionResponse) portletResponse, prcontext.getResponse());
                prcontext.setResponseComplete(true);
            } else if (portletResponse instanceof ErrorResponse) {
                handleErrorResponse((ErrorResponse) portletResponse);
            } else if (portletResponse instanceof SecurityResponse) {
                handleSecurityResponse((SecurityResponse) portletResponse);
            } else {
                throw new Exception("Unexpected response type [" + portletResponse + "]. Expected an UpdateNavigationResponse"
                        + ", a HTTPRedirectionResponse or an ErrorResponse.");
            }
        }

        private void handleRedirectionResponse(HTTPRedirectionResponse redirectionResponse, HttpServletResponse response)
                throws IOException {
            String redirectionURL = redirectionResponse.getLocation();
            response.sendRedirect(redirectionURL);
        }

        private void handleUpdateNavigationalStateResponse(UpdateNavigationalStateResponse navStateResponse,
                UIPortlet<S, C> uiPortlet, PortalRequestContext prcontext) throws Exception {
            /*
             * Update the portlet window state according to the action output information
             *
             * If the current node is displaying a usual layout page, also tells the page which portlet to render or not when
             * the state is maximized
             */
            // Note: we should only update the WindowState if the UpdateNavigationalStateResponse.getWindowState is not null,
            // otherwise it means the WindowState has not changed and we should use the current value.
            if (navStateResponse.getWindowState() != null) {
                WindowState state = new WindowState(getWindowStateOrDefault(navStateResponse));
                setNextState(uiPortlet, state);
            }

            // update the portlet with the next mode to display
            // Note: we should only update the Mode if the UpdateNavigationalStateResponse.getMode is not null,
            // otherwise it means the mode has not changed and we should use the current value.
            if (navStateResponse.getMode() != null) {
                PortletMode mode = new PortletMode(getPortletModeOrDefault(navStateResponse));
                setNextMode(uiPortlet, mode);
            }

            /*
             * Cache the render parameters in the UI portlet component to handle the navigational state. Each time a portlet is
             * rendered (except using directly a RenderURL) those parameters are added to the portlet request to preserve the
             * portlet state among all the portal clicks
             */

            //
            StateString navigationalState = navStateResponse.getNavigationalState();
            if (navigationalState != null) {
                uiPortlet.setNavigationalState(navigationalState);
            }

            // update the public render parameters with the changes from the invocation
            setupPublicRenderParams(uiPortlet, navStateResponse.getPublicNavigationalStateUpdates());

            /*
             * Handle the events returned by the action output and broadcast a new UI event to the ProcessEventsActionListener
             * that will then target the portlet container service directly
             */

            // TODO: (mwringe) add this to the UpdateNavigationStateResponse.Event class instead of here
            class PortletEvent implements javax.portlet.Event {
                QName qName;

                Serializable value;

                public PortletEvent(QName qName, Serializable value) {
                    this.qName = qName;
                    this.value = value;
                }

                public String getName() {
                    return qName.getLocalPart();
                }

                public QName getQName() {
                    return qName;
                }

                public Serializable getValue() {
                    return value;
                }
            }

            List<UpdateNavigationalStateResponse.Event> nsEvents = navStateResponse.getEvents();
            List<javax.portlet.Event> events = new ArrayList<javax.portlet.Event>(nsEvents.size());
            if (nsEvents != null && !nsEvents.isEmpty()) {
                for (UpdateNavigationalStateResponse.Event nsEvent : nsEvents) {
                    if (uiPortlet.supportsPublishingEvent(nsEvent.getName())) {
                        javax.portlet.Event portletEvent = new PortletEvent(nsEvent.getName(), nsEvent.getPayload());
                        events.add(portletEvent);
                    }
                }
            }

            if (events != null) {
                prcontext.setAttribute(PORTLET_EVENTS, new EventsWrapper(events));
                uiPortlet.createEvent("ProcessEvents", Phase.PROCESS, prcontext).broadcast();
            }

        }

        private void handleErrorResponse(ErrorResponse response) throws Exception {
            throw new Exception(response.getCause());
        }

        private void handleSecurityResponse(SecurityResponse response) throws Exception {
            if (response instanceof SecurityErrorResponse) {
                SecurityErrorResponse securityErrorResponse = (SecurityErrorResponse) response;
                throw new Exception("SecurityErrorResponse Returned while trying to process portlet action. ",
                        securityErrorResponse.getThrowable());
            } else {
                throw new Exception("Security Response of type " + response.getClass()
                        + " encountered while trying to process portlet action.");
            }
        }
    }

    private static void clearMaximizedUIComponent(UIPage uiPage, UIPortlet uiPortlet) {
        if(uiPage.getMaximizedUIPortlet() != null && uiPage.getMaximizedUIPortlet().getId().equals(uiPortlet.getId())) {
            uiPage.setMaximizedUIPortlet(null);
        }
    }

    /**
     * This method is used to set the next portlet window state if this one needs to be modified because of the incoming request
     */
    public static void setNextState(UIPortlet uiPortlet, WindowState state) {
        if (state != null) {
            UIPage uiPage = uiPortlet.getAncestorOfType(UIPage.class);

            if (WindowState.MAXIMIZED.equals(state)) {
                if (uiPage != null) {
                    uiPage.normalizePortletWindowStates();
                    uiPage.setMaximizedUIPortlet(uiPortlet);
                    uiPortlet.setCurrentWindowState(WindowState.MAXIMIZED);
                }
            } else if (WindowState.MINIMIZED.equals(state)) {
                uiPortlet.setCurrentWindowState(WindowState.MINIMIZED);
                if (uiPage != null) {
                    clearMaximizedUIComponent(uiPage, uiPortlet);
                }
            } else {
                uiPortlet.setCurrentWindowState(WindowState.NORMAL);
                if (uiPage != null) {
                    clearMaximizedUIComponent(uiPage, uiPortlet);
                }
            }
        }
    }

    /** This method is used to set the next portlet mode if this one needs to be modified because of the incoming request */
    public static void setNextMode(UIPortlet uiPortlet, PortletMode portletMode) {
        if (portletMode != null) {
            if (portletMode.equals(PortletMode.HELP)) {
                uiPortlet.setCurrentPortletMode(PortletMode.HELP);
            } else if (portletMode.equals(PortletMode.EDIT)) {
                uiPortlet.setCurrentPortletMode(PortletMode.EDIT);
            } else {
                uiPortlet.setCurrentPortletMode(PortletMode.VIEW);
            }
        }
    }

    /**
     * The serveResource() method defined in the JSR 286 specs has several goals: - provide binary output like images to be
     * displayed in the portlet (in the previous spec - JSR 168 - a servlet was needed) - provide text output that does not
     * impact the entire portal rendering, it is for instance usefull when dealing with Javascript to return some JSON
     * structures
     * <br>
     * The method delegates the call to the portlet container serverResource method after filling the ResourceInput object with
     * the current request state.
     * <br>
     * This returns a ResourceOutput object that can content binary or text contentType
     * <br>
     * Finally the content is set in the portal response writer or outputstream depending on the type; the processRender()
     * method of the portal is not called as we set the response as complete
     */
    public static class ServeResourceActionListener<S, C extends Serializable, I> extends EventListener<UIPortlet<S, C>> {
        public void execute(Event<UIPortlet<S, C>> event) throws Exception {
            UIPortlet<S, C> uiPortlet = event.getSource();
            log.trace("Serve Resource for portlet: " + uiPortlet.getPortletContext());
            String resourceId = null;

            //
            PortalRequestContext context = (PortalRequestContext) event.getRequestContext();
            HttpServletResponse response = context.getResponse();

            //
            try {
                // Set the NavigationalState
                String navState = context.getRequestParameter(ExoPortletInvocationContext.NAVIGATIONAL_STATE_PARAM_NAME);
                if (navState != null) {
                    uiPortlet.setNavigationalState(ParametersStateString.create(navState));
                }

                //
                ResourceInvocation resourceInvocation = uiPortlet.create(ResourceInvocation.class, context);

                // set the resourceId to be used in case of a problem
                resourceId = resourceInvocation.getResourceId();

                //
                PortletInvocationResponse portletResponse = uiPortlet.invoke(resourceInvocation);

                //
                int statusCode;
                MultiValuedPropertyMap<String> transportHeaders;
                String contentType;
                String charset;
                Object content;
                if (!(portletResponse instanceof ContentResponse)) {
                    if (portletResponse instanceof ErrorResponse) {
                        ErrorResponse errorResponse = (ErrorResponse) portletResponse;
                        Throwable cause = errorResponse.getCause();
                        if (cause != null) {
                            log.trace("Got error response from portlet", cause);
                        } else if (errorResponse.getMessage() != null) {
                            log.trace("Got error response from portlet:" + errorResponse.getMessage());
                        } else {
                            log.trace("Got error response from portlet");
                        }
                    } else {
                        log.trace("Unexpected response type [" + portletResponse
                                + "]. Expected a ContentResponse or an ErrorResponse.");
                    }
                    statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                    contentType = null;
                    charset = null;
                    transportHeaders = null;
                    content = null;
                } else {
                    //
                    ContentResponse piResponse = (ContentResponse) portletResponse;
                    ResponseProperties properties = piResponse.getProperties();
                    transportHeaders = properties != null ? properties.getTransportHeaders() : null;

                    // Look at status code if there is one and honour it
                    String status = transportHeaders != null ? transportHeaders.getValue(ResourceResponse.HTTP_STATUS_CODE)
                            : null;
                    if (status != null) {
                        try {
                            statusCode = Integer.parseInt(status);
                        } catch (NumberFormatException e) {
                            statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                        }
                    } else {
                        statusCode = HttpServletResponse.SC_OK;
                    }

                    //
                    contentType = piResponse.getContentType();
                    charset = piResponse.getEncoding();

                    //
                    log.trace("Try to get a resource of type: " + contentType + " for the portlet: "
                            + uiPortlet.getId());
                    if (piResponse.getChars() != null) {
                        content = piResponse.getChars();
                    } else if (piResponse.getBytes() != null) {
                        content = piResponse.getBytes();
                    } else {
                        content = null;
                    }
                }

                //
                response.setStatus(statusCode);

                // Set content type if any
                if (contentType != null) {
                    response.setContentType(contentType);
                }

                // Set encoding
                if (charset != null) {
                    response.setCharacterEncoding(charset);
                }

                // Send headers if any
                if (transportHeaders != null) {
                    sendHeaders(transportHeaders, context);
                }

                // Send body if any
                if (content instanceof String) {
                    context.getWriter().write((String) content);
                } else if (content instanceof byte[]) {
                    byte[] bytes = (byte[]) content;
                    response.setContentLength(bytes.length);
                    OutputStream stream = response.getOutputStream();
                    try {
                        stream.write(bytes);
                    } finally {
                        Safe.close(stream);
                    }
                }

                //
                response.flushBuffer();
            } catch (NoSuchDataException e) {
                UIPortalApplication uiApp = Util.getUIPortalApplication();
                uiApp.refreshCachedUI();
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (Exception e) {
                if (!e.getClass().toString().contains("ClientAbortException")) {
                    log.error("Problem while serving resource " + (resourceId != null ? resourceId : "") + " for the portlet: "
                            + uiPortlet.getPortletContext().getId(), e);
                }
            } finally {
                /**
                 * The resource method does not need to go through the render phase
                 */
                event.getRequestContext().setResponseComplete(true);
            }
        }

        /**
         * Send any header to the client
         *
         * @param headers the headers
         * @param context the context
         * @throws IOException any io exception
         */
        private void sendHeaders(MultiValuedPropertyMap<String> headers, PortalRequestContext context) {
            Map<String, String> map = new HashMap<String, String>();
            for (String key : headers.keySet()) {
                for (String value : headers.getValues(key)) {
                    map.put(key, value);
                }
            }

            // We need to remove it if it there
            map.remove(ResourceResponse.HTTP_STATUS_CODE);
            context.setHeaders(map);
        }
    }

    /**
     * Process Events sent by the portlet API during the processAction() and serverResource() methods defined in Portlet API 2.0
     * (JSR 286)
     */
    public static class ProcessEventsActionListener extends EventListener<UIPortlet> {
        public void execute(Event<UIPortlet> event) throws Exception {
            UIPortlet uiPortlet = event.getSource();
            PortalRequestContext context = (PortalRequestContext) event.getRequestContext();
            List<UIPortlet> portletInstancesInPage = new ArrayList<UIPortlet>();
            // UIPortalApplication uiPortal = uiPortlet.getAncestorOfType(UIPortalApplication.class);
            UIPortalApplication uiPortal = (UIPortalApplication) context.getUIApplication();
            uiPortal.findComponentOfType(portletInstancesInPage, UIPortlet.class);
            EventsWrapper eventsWrapper = (EventsWrapper) event.getRequestContext().getAttribute(PORTLET_EVENTS);
            List<javax.portlet.Event> events = eventsWrapper.getEvents();

            /*
             * Iterate over all the events that the processAction has generated. Check among all the portlet instances deployed
             * in the page (usual layout or webos) which instance can be targeted by the event and then process the event on the
             * associated UIPortlet component
             */
            while (events.size() > 0) {
                javax.portlet.Event nativeEvent = events.remove(0);
                QName eventName = nativeEvent.getQName();
                for (UIPortlet uiPortletInPage : portletInstancesInPage) {
                    if (uiPortletInPage.supportsProcessingEvent(eventName)
                            && !eventsWrapper.isInvokedTooManyTimes(uiPortletInPage)) {
                        List<javax.portlet.Event> newEvents = processEvent(uiPortletInPage, nativeEvent);
                        eventsWrapper.increaseCounter(uiPortletInPage);
                        if (context.useAjax()) {
                            log.info("Events were generated inside the scope of an AJAX call, hence will only refresh the targeted portlets");
                            event.getRequestContext().addUIComponentToUpdateByAjax(uiPortletInPage);
                        } else {
                            log.info("Events were generated outside the scope of an AJAX call, hence will make a full render of the page");
                            context.ignoreAJAXUpdateOnPortlets(true);
                        }
                        if (newEvents != null && !newEvents.isEmpty()) {
                            log.trace("The portlet: " + uiPortletInPage.getPortletContext().getId()
                                    + " processEvent() method has generated new events itself");
                            events.addAll(newEvents);
                        }
                    }
                }
            }
            for (UIPortlet uiPortletInPage : portletInstancesInPage) {
                setNextState(uiPortletInPage, uiPortletInPage.getCurrentWindowState());
            }
        }
    }

    /**
     * This method is called when the javax.portlet.Event is supported by the current portlet stored in the Portlet Caontainer
     * <br>
     * The processEvent() method can also generates IPC events and hence the portal itself will call the
     * ProcessEventsActionListener once again
     */
    public static <S, C extends Serializable, I> List<javax.portlet.Event> processEvent(UIPortlet<S, C> uiPortlet,
            javax.portlet.Event event) {
        log.trace("Process Event: " + event.getName() + " for portlet: " + uiPortlet.getState());
        try {
            PortalRequestContext context = (PortalRequestContext) WebuiRequestContext.getCurrentInstance();

            //
            EventInvocation eventInvocation = uiPortlet.create(EventInvocation.class, context);

            //
            eventInvocation.setName(event.getQName());
            eventInvocation.setPayload(event.getValue());

            //
            PortletInvocationResponse piResponse = uiPortlet.invoke(eventInvocation);

            //
            ExoPortletInstanceContext instanceCtx = (ExoPortletInstanceContext) eventInvocation.getInstanceContext();
            if (instanceCtx.getModifiedContext() != null) {
                StatefulPortletContext<C> updatedCtx = (StatefulPortletContext<C>) instanceCtx.getModifiedContext();
                C portletState = updatedCtx.getState();
                uiPortlet.update(portletState);
            }

            // todo: handle the error response better than this.
            if (!(piResponse instanceof UpdateNavigationalStateResponse)) {
                if (piResponse instanceof ErrorResponse) {
                    ErrorResponse errorResponse = (ErrorResponse) piResponse;
                    throw (Exception) errorResponse.getCause();
                } else {
                    throw new Exception("Unexpected response type [" + piResponse
                            + "]. Expected a UpdateNavigationResponse or an ErrorResponse.");
                }
            }

            UpdateNavigationalStateResponse navResponse = (UpdateNavigationalStateResponse) piResponse;

            //

            /*
             * Update the portlet window state according to the action output information
             *
             * If the current node is displaying a usual layout page, also tells the page which portlet to render or not when
             * the state is maximized
             */
            WindowState state = new WindowState(getWindowStateOrDefault(navResponse));
            setNextState(uiPortlet, state);

            // update the portlet with the next mode to display
            PortletMode mode = new PortletMode(getPortletModeOrDefault(navResponse));
            setNextMode(uiPortlet, mode);

            StateString navState = navResponse.getNavigationalState();
            if (navState != null) {
                uiPortlet.setNavigationalState(navResponse.getNavigationalState());
            }
            setupPublicRenderParams(uiPortlet, navResponse.getPublicNavigationalStateUpdates());

            // TODO: (mwringe) add this to the UpdateNavigationStateResponse.Event class instead of here
            class PortletEvent implements javax.portlet.Event {
                QName qName;

                Serializable value;

                public PortletEvent(QName qName, Serializable value) {
                    this.qName = qName;
                    this.value = value;
                }

                public String getName() {
                    return qName.getLocalPart();
                }

                public QName getQName() {
                    return qName;
                }

                public Serializable getValue() {
                    return value;
                }
            }

            List<UpdateNavigationalStateResponse.Event> nsEvents = navResponse.getEvents();
            List<javax.portlet.Event> events = new ArrayList<javax.portlet.Event>(nsEvents.size());
            if (nsEvents != null && !nsEvents.isEmpty()) {
                for (UpdateNavigationalStateResponse.Event nsEvent : nsEvents) {
                    javax.portlet.Event portletEvent = new PortletEvent(nsEvent.getName(), nsEvent.getPayload());
                    events.add(portletEvent);
                }
            }

            return events;
        } catch (Exception e) {
            log.error("Problem while processesing event for the portlet: " + uiPortlet.getState(), e);
        }
        return null;
    }

    private static String getPortletModeOrDefault(UpdateNavigationalStateResponse navResponse) {
        Mode mode = navResponse.getMode();
        if (mode == null) {
            mode = Mode.VIEW;
        }
        return mode.toString();
    }

    private static String getWindowStateOrDefault(UpdateNavigationalStateResponse navResponse) {
        org.gatein.pc.api.WindowState state = navResponse.getWindowState();
        if (state == null) {
            state = org.gatein.pc.api.WindowState.NORMAL;
        }
        return state.toString();
    }

    /**
     * This listener is called when a RenderURL url has been generated by the portlet container. In that case it means that the
     * render() method of a targeted portlet will be directly called and that the existing navigational state will be reset by
     * removing all the Render Parameters from the cache map located in the UIPortlet
     */
    public static class RenderActionListener extends EventListener<UIPortlet> {
        public void execute(Event<UIPortlet> event) throws Exception {
            UIPortlet uiPortlet = event.getSource();
            uiPortlet.setNavigationalState(null);

            // set the public params
            HttpServletRequest request = event.getRequestContext().getRequest();
            setupPublicRenderParams(uiPortlet, request.getParameterMap());

            // set render params
            String navState = event.getRequestContext().getRequestParameter(
                    ExoPortletInvocationContext.NAVIGATIONAL_STATE_PARAM_NAME);
            if (navState != null) {
                uiPortlet.setNavigationalState(ParametersStateString.create(navState));
            }
        }
    }

    /**
     * This method is called by the process action and render action listeners, aka during the processDecode() phase of our UI
     * framework
     * <br>
     * It goes throughs all the request parameters and add to the public render parameters Map the one that are supported by the
     * targeted portlet
     */
    public static void setupPublicRenderParams(UIPortlet uiPortlet, Map<String, String[]> requestParams) {
        if (ParameterValidation.existsAndIsNotEmpty(requestParams)) {
            UIPortal uiPortal = Util.getUIPortal();
            Map<String, String[]> publicParams = uiPortal.getPublicParameters();

            for (String key : requestParams.keySet()) {
                String[] value = requestParams.get(key);
                if (uiPortlet.supportsPublicParam(key)) {
                    if (value.length > 0) {
                        publicParams.put(key, value);
                    } else {
                        publicParams.remove(key);
                    }
                }
            }
        }

    }

    /**
     * This listener is called when the portlet portlet window state has to be changed.
     * It can be changed by building specific URL, or by programatically in the portlet, then triggered after action or event response
     */
    public static class ChangeWindowStateActionListener extends EventListener<UIPortlet> {
        public void execute(Event<UIPortlet> event) throws Exception {
            UIPortlet uiPortlet = event.getSource();

            UIPortalApplication uiPortalApp = uiPortlet.getAncestorOfType(UIPortalApplication.class);
            UIWorkingWorkspace uiWorkingWS = uiPortalApp.getChildById(UIPortalApplication.UI_WORKING_WS_ID);
            PortalRequestContext pcontext = (PortalRequestContext) event.getRequestContext();
            pcontext.addUIComponentToUpdateByAjax(uiWorkingWS);
            pcontext.ignoreAJAXUpdateOnPortlets(true);

            String windowState = null;

            Object changeWindowStateAttribute = event.getRequestContext().getAttribute(CHANGE_WINDOW_STATE_EVENT);
            if (changeWindowStateAttribute != null && changeWindowStateAttribute instanceof String) {
                windowState = (String) changeWindowStateAttribute;
            }

            if (windowState == null) {
                windowState = event.getRequestContext().getRequestParameter(Constants.PORTAL_WINDOW_STATE);
            }
            if (windowState == null) {
                if(event.getRequestContext().getRequestParameter(UIComponent.OBJECTID) != null) {
                    windowState = event.getRequestContext().getRequestParameter(UIComponent.OBJECTID).trim();
                }
            }

            if (windowState == null) {
                windowState = uiPortlet.getCurrentWindowState().toString();
            }

            UIPage uiPage = uiPortlet.getAncestorOfType(UIPage.class);
            if (windowState.equals(WindowState.MAXIMIZED.toString())) {
                if(uiPage != null) {
                    uiPage.normalizePortletWindowStates();
                    uiPage.setMaximizedUIPortlet(uiPortlet);
                }
                uiPortlet.setCurrentWindowState(WindowState.MAXIMIZED);
                return;
            } else {
                if (windowState.equals(WindowState.MINIMIZED.toString())) {
                    uiPortlet.setCurrentWindowState(WindowState.MINIMIZED);
                } else {
                    uiPortlet.setCurrentWindowState(WindowState.NORMAL);
                }
                if (uiPage != null) {
                    clearMaximizedUIComponent(uiPage, uiPortlet);
                }
            }
        }
    }

    /** This listener is called when the portlet mode of a portlet has to be changed. */
    public static class ChangePortletModeActionListener extends EventListener<UIPortlet> {
        public void execute(Event<UIPortlet> event) throws Exception {
            UIPortlet uiPortlet = event.getSource();

            String portletMode = null;

            Object changePortletModeAttribute = event.getRequestContext().getAttribute(CHANGE_PORTLET_MODE_EVENT);
            if (changePortletModeAttribute != null && changePortletModeAttribute instanceof String) {
                portletMode = (String) changePortletModeAttribute;
            }

            if (portletMode == null) {
                portletMode = event.getRequestContext().getRequestParameter(Constants.PORTAL_PORTLET_MODE);
            }
            if (portletMode == null) {
                portletMode = event.getRequestContext().getRequestParameter(UIComponent.OBJECTID);
            }
            if (portletMode == null) {
                portletMode = uiPortlet.getCurrentPortletMode().toString();
            }

            log.trace("Change portlet mode of " + uiPortlet.getPortletContext().getId() + " to " + portletMode);
            if (portletMode.equals(PortletMode.HELP.toString())) {
                uiPortlet.setCurrentPortletMode(PortletMode.HELP);
            } else if (portletMode.equals(PortletMode.EDIT.toString())) {
                uiPortlet.setCurrentPortletMode(PortletMode.EDIT);
            } else if (portletMode.equals(PortletMode.VIEW.toString())) {
                uiPortlet.setCurrentPortletMode(PortletMode.VIEW);
            } else {
                PortletMode customMode = new PortletMode(portletMode);
                uiPortlet.setCurrentPortletMode(customMode);
            }
            event.getRequestContext().addUIComponentToUpdateByAjax(uiPortlet);
        }
    }

    /**
     * This listener is called when the portlet edit form (which tells information about the portlet width or height as well as
     * if the info bar and its content should be shown) is invoked.
     * <br>
     * It places the form in the portal black mask
     */
    public static class EditPortletActionListener extends EventListener<UIPortlet> {
        public void execute(Event<UIPortlet> event) throws Exception {
            UIPortlet uiPortlet = event.getSource();
            UIPortalApplication uiApp = Util.getUIPortalApplication();
            UIMaskWorkspace uiMaskWS = uiApp.getChildById(UIPortalApplication.UI_MASK_WS_ID);
            UIPortletForm uiPortletForm = uiMaskWS.createUIComponent(UIPortletForm.class, null, null);

            if (uiPortletForm.setValues(uiPortlet) == false) {
                uiMaskWS.setUIComponent(null);
                WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
                context.getUIApplication().addMessage(
                        (new ApplicationMessage("UIPortlet.message.portletDeleted", null, ApplicationMessage.ERROR)));
            } else {
                uiMaskWS.setUpdated(true);
                uiMaskWS.setWindowSize(800, -1);
                event.getRequestContext().addUIComponentToUpdateByAjax(uiMaskWS);
            }
        }
    }

}
