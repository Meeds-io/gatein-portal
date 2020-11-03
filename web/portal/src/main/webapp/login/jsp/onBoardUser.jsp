
<%--

    Copyright (C) 2020 eXo Platform SAS.
    
    This is free software; you can redistribute it and/or modify it
    under the terms of the GNU Lesser General Public License as
    published by the Free Software Foundation; either version 2.1 of
    the License, or (at your option) any later version.
    
    This software is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
    Lesser General Public License for more details.
    
    You should have received a copy of the GNU Lesser General Public
    License along with this software; if not, write to the Free
    Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
    02110-1301 USA, or see the FSF site: http://www.fsf.org.

--%>

<%@ page import="java.net.URLEncoder"%>
<%@ page import="javax.servlet.http.Cookie"%>
<%@ page import="org.exoplatform.web.login.LoginError"%>
<%@ page import="org.exoplatform.container.PortalContainer"%>
<%@ page import="org.exoplatform.services.resources.ResourceBundleService"%>
<%@ page import="org.gatein.security.oauth.spi.OAuthProviderType"%>
<%@ page import="org.gatein.security.oauth.spi.OAuthProviderTypeRegistry"%>
<%@ page import="org.exoplatform.portal.resource.SkinService"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="org.gatein.common.text.EntityEncoder"%>
<%@ page import="org.exoplatform.web.login.recovery.PasswordRecoveryService" %>
<%@ page import="org.exoplatform.web.controller.QualifiedName" %>
<%@ page import="org.exoplatform.portal.config.UserPortalConfigService" %>
<%@ page import="org.exoplatform.commons.utils.CommonsUtils"%>
<%@ page import="org.exoplatform.portal.branding.BrandingService"%>
<%@ page import="org.exoplatform.portal.branding.Branding"%>
<%@ page language="java" %>
<%
  String contextPath = request.getContextPath() ;
  String logoTitle = null;
  String username = request.getParameter("username");
  if(username == null) {
      username = "";
  } else {
      EntityEncoder encoder = EntityEncoder.FULL;
      username = encoder.encode(username);
  }


  BrandingService brandingService = CommonsUtils.getService(BrandingService.class);
  Branding branding = brandingService.getBrandingInformation();
  logoTitle = branding.getCompanyName();

  PortalContainer portalContainer = PortalContainer.getCurrentInstance(session.getServletContext());
  ResourceBundleService service = (ResourceBundleService) portalContainer.getComponentInstanceOfType(ResourceBundleService.class);
  ResourceBundle res = service.getResourceBundle(service.getSharedResourceBundleNames(), request.getLocale());

  OAuthProviderTypeRegistry registry = (OAuthProviderTypeRegistry) portalContainer.getComponentInstanceOfType(OAuthProviderTypeRegistry.class);
  Cookie cookie = new Cookie(org.exoplatform.web.login.LoginServlet.COOKIE_NAME, "");
	cookie.setPath(request.getContextPath());
	cookie.setMaxAge(0);
	response.addCookie(cookie);

  UserPortalConfigService userPortalConfigService = portalContainer.getComponentInstanceOfType(UserPortalConfigService.class);
  SkinService skinService = portalContainer.getComponentInstanceOfType(SkinService.class);
  String skinName = userPortalConfigService.getDefaultPortalSkinName();
  String loginCssPath = skinService.getSkin("portal/login", skinName).getCSSPath();

  PasswordRecoveryService passRecoveryServ = portalContainer.getComponentInstanceOfType(PasswordRecoveryService.class);
  String forgotPasswordPath = passRecoveryServ.getPasswordRecoverURL(null, null);

  //
  String uri = (String)request.getAttribute("org.gatein.portal.login.initial_uri");
  boolean error = request.getAttribute("org.gatein.portal.login.error") != null;
  String errorParam = (String)request.getParameter(org.exoplatform.web.login.LoginError.ERROR_PARAM);
  LoginError errorData = null;
  if (errorParam != null) {
      errorData = LoginError.parse(errorParam);
  }
  
  response.setCharacterEncoding("UTF-8"); 
  response.setContentType("text/html; charset=UTF-8");
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title>Login</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>   
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
      <link id="brandingSkin" rel="stylesheet" type="text/css" href="/rest/v1/platform/branding/css">
      <link rel="shortcut icon" type="image/x-icon"  href="<%=contextPath%>/favicon.ico" />
      <link href="<%=loginCssPath%>" rel="stylesheet" type="text/css"/>
      <link rel="stylesheet" type="text/css" href="<%=contextPath%>/login/skin/Stylesheet.css"/>

    <script type="text/javascript" src="/eXoResources/javascript/jquery-3.2.1.js"></script>
    <script type="text/javascript" src="/commons-extension/javascript/switch-button.js"></script>
    <script type="text/javascript">
        $(document).ready(function() {
            var contentLink = jQuery("#platformInfoPoweredBy");
            var contentVersion = jQuery("#platformInfoVersion");
            var htmlLink = contentLink.data("labelfooter") +" eXo Platform ";
            var htmlVersion = "";
            var requestJsonPlatformInfo = jQuery.ajax({ type: "GET", url: "/portal/rest/platform/info", async: false, dataType: 'json' });
            if(requestJsonPlatformInfo.readyState == 4 && requestJsonPlatformInfo.status == 200){
                //readyState 4: request finished and response is ready
                //status 200: "OK"
                var myresponseText = requestJsonPlatformInfo.responseText;
                var jsonPlatformInfo = jQuery.parseJSON(myresponseText);
                htmlVersion += "v";
                htmlVersion += jsonPlatformInfo.platformVersion;
                htmlVersion += " - build ";
                htmlVersion += jsonPlatformInfo.platformBuildNumber;
            }else{
                htmlVersion += "4.0";
            }
            contentLink.text(htmlLink);
            contentVersion.text(htmlVersion);
        });
    </script>
  </head>
  <body>
  	<div class="loginBGLight"><span></span></div>
  	<p id="WelcomeContent"><%=res.getString("portal.login.welcomeTo")%> <%= logoTitle%></p>
    <div class="uiLogin">
     <div class="uiLoginCondition">
      <p><%=res.getString("portal.login.Hello")%></p>
      <p><%=res.getString("portal.login.loginText")%></p>
      <p><%=res.getString("portal.login.condition")%></p>
      <p><%=res.getString("portal.login.allowCreatePassword")%></p>
     </div>
    	<div class="loginContainer">
	      <div class="loginHeader introBox">
					<div class="userLoginIcon"><%=res.getString("portal.login.Connectlabel")%></div>
				</div>
	      <div class="loginContent">
				<div class="titleLogin">
					<%/*Begin form*/%>
          <% if (error || errorData != null) {
              if (errorData != null) {
						if (org.exoplatform.web.login.LoginError.DISABLED_USER_ERROR == errorData.getCode()) {
        	 %>
          <div class="signinFail"><i class="uiIconError"></i><%=res.getString("UILoginForm.label.DisabledUserSignin")%></div>
          <%
        				}
        		  } else {%>          
          <div class="signinFail"><i class="uiIconError"></i><%=res.getString("portal.login.SigninFail")%></div>          
          <%  }
             }%>
				</div>
        <div class="centerLoginContent">
          <form name="loginForm" action="<%= contextPath + "/login"%>" method="post" style="margin: 0px;">
                <% if (uri != null) { 
                        uri = EntityEncoder.FULL.encode(uri);
                %>
          		<input type="hidden" name="initialURI" value="<%=uri%>"/>
                <% } %>

                <div class="userCredentials">
                  <span class="iconUser"></span>
                  <input  tabindex="1" id="username" name="username" type="text" placeholder="<%=res.getString("portal.login.Username")%>" onblur="this.placeholder = '<%=res.getString("portal.login.Username.blur")%>'" onfocus="this.placeholder = ''">
                </div>
                <div class="userCredentials">
                  <span class="iconPswrd"></span>
                  <input  tabindex="2"  type="password" id="password" name="password" placeholder="<%=res.getString("portal.login.Password")%>" onblur="this.placeholder = '<%=res.getString("portal.login.Password")%>'" onfocus="this.placeholder = ''">
                </div>
                <div class="userCredentials">
                  <span class="iconPswrd"></span>
                  <input  tabindex="2"  type="password" id="password" name="password" placeholder="<%=res.getString("portal.login.ConfirmPassword")%>" onblur="this.placeholder = '<%=res.getString("portal.login.ConfirmPassword")%>'" onfocus="this.placeholder = ''">
                </div>
                <p class="capatchaMessage"><%=res.getString("portal.login.capatcha")%></p>
				<div id="UIPortalLoginFormAction" class="loginButton">
					<button class="button" tabindex="4"  onclick="login();"><%=res.getString("portal.login.create")%></button>
				</div>

                <script type='text/javascript'>


					function login() {
						document.loginForm.submit();                   
					}
				</script>
				</form>
				<%/*End form*/%>
        </div>
        <div>
            <% if(registry.isOAuthEnabled()) { %>
            <script type="text/javascript">
                function goSocialLoginUrl(url) {
                    if(document.getElementById('rememberme').checked) {
                        url += '&_rememberme=true';
                    }
                    window.location = url;
                    return false;
                }
            </script>
            <div id="social-pane">
                <div class="signInDelimiter">
                    <%=res.getString("UILoginForm.label.mobile.login.oauth.Delimiter")%>
                </div>
                <div id="social-login">
                    <% for (OAuthProviderType oauthProvType : registry.getEnabledOAuthProviders()) { %>
                    <a href="javascript:void(0)" onclick="goSocialLoginUrl('<%= oauthProvType.getInitOAuthURL(contextPath, uri) %>')" id="login-<%= oauthProvType.getKey() %>">
                        <i class="uiIconOauth <%= oauthProvType.getKey().toLowerCase() %>"></i>
                    </a>
                    <% } %>
                </div>
            </div>
            <% } %>
        </div>
      </div>
    	</div>
    </div>
    <div class="logoImage">
      <img src="/logo/Logo.png"/>
    </div>
  </body>
</html>
