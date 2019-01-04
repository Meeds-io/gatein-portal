/*
 * Copyright (C) 2018 eXo Platform SAS.
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
package org.exoplatform.portal.gadget;

import static org.mockito.Mockito.*;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.portal.gadget.core.CharResponseWrapper;
import org.exoplatform.portal.gadget.core.ProxyServletFilter;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

/**
 *
 */
@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/gadget-proxy-configuration.xml") })
public class TestProxyServletFilter extends AbstractKernelTest {

  public void testShouldNotSendErrorWhenCorrectURLInParameters() throws Exception {
    // Given
    FilterConfig filterConfig = mock(FilterConfig.class);
    ServletContext servletContext = mock(ServletContext.class);
    when(servletContext.getServletContextName()).thenReturn("portal");
    when(filterConfig.getServletContext()).thenReturn(servletContext);
    ProxyServletFilter proxyServletFilter = new ProxyServletFilter();
    proxyServletFilter.init(filterConfig);

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getParameter(eq("url"))).thenReturn("http://localhost:8080");
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.getWriter()).thenReturn(new PrintWriter(new ByteArrayOutputStream()));
    when(response.getOutputStream()).thenReturn(new CharResponseWrapper.ByteArrayServletStream(new ByteArrayOutputStream()));
    FilterChain filterChain = mock(FilterChain.class);

    // When
    proxyServletFilter.doFilter(request, response, filterChain);

    // Then
    verify(response, never()).sendError(anyInt(), anyString());
  }

  public void testShouldSendErrorWhenNoURLInParameters() throws Exception {
    // Given
    ProxyServletFilter proxyServletFilter = new ProxyServletFilter();

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getParameter(eq("url"))).thenReturn(null);
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.getWriter()).thenReturn(new PrintWriter(new ByteArrayOutputStream()));
    when(response.getOutputStream()).thenReturn(new CharResponseWrapper.ByteArrayServletStream(new ByteArrayOutputStream()));
    FilterChain filterChain = mock(FilterChain.class);

    // When
    proxyServletFilter.doFilter(request, response, filterChain);

    // Then
    verify(response, times(1)).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), eq("No URL"));
  }

  public void testShouldSendErrorWhenBlacklistedURLInParameters() throws Exception {
    // Given
    FilterConfig filterConfig = mock(FilterConfig.class);
    ServletContext servletContext = mock(ServletContext.class);
    when(servletContext.getServletContextName()).thenReturn("portal");
    when(filterConfig.getServletContext()).thenReturn(servletContext);
    ProxyServletFilter proxyServletFilter = new ProxyServletFilter();
    proxyServletFilter.init(filterConfig);

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getParameter(eq("url"))).thenReturn("http://blacklisted.evil.org");
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.getWriter()).thenReturn(new PrintWriter(new ByteArrayOutputStream()));
    when(response.getOutputStream()).thenReturn(new CharResponseWrapper.ByteArrayServletStream(new ByteArrayOutputStream()));
    FilterChain filterChain = mock(FilterChain.class);

    // When
    proxyServletFilter.doFilter(request, response, filterChain);

    // Then
    verify(response, times(1)).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
  }

  /**
   * In order to avoid disclosing if a port is open on the server, the error
   * returned must be the same when the port is opened or closed
   * 
   * @throws Exception
   */
  public void testShouldSendBadRequestErrorWhenServletReturnsGatewayTimeoutError() throws Exception {
    // Given
    FilterConfig filterConfig = mock(FilterConfig.class);
    ServletContext servletContext = mock(ServletContext.class);
    when(servletContext.getServletContextName()).thenReturn("portal");
    when(filterConfig.getServletContext()).thenReturn(servletContext);
    ProxyServletFilter proxyServletFilter = new ProxyServletFilter();
    proxyServletFilter.init(filterConfig);

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getParameter(eq("url"))).thenReturn("http://localhost:8999");
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.getWriter()).thenReturn(new PrintWriter(new ByteArrayOutputStream()));
    when(response.getOutputStream()).thenReturn(new CharResponseWrapper.ByteArrayServletStream(new ByteArrayOutputStream()));
    FilterChain filterChain = mock(FilterChain.class);

    when(response.getStatus()).thenReturn(HttpServletResponse.SC_GATEWAY_TIMEOUT);

    // When
    proxyServletFilter.doFilter(request, response, filterChain);

    // Then
    verify(response, times(1)).setStatus(eq(HttpServletResponse.SC_BAD_REQUEST));
  }
}
