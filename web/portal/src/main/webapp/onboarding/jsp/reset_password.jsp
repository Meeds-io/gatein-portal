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

<%@ page import="org.exoplatform.container.PortalContainer"%>
<%@ page import="org.exoplatform.services.resources.ResourceBundleService"%>
<%@ page import="org.exoplatform.portal.resource.SkinService"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="org.exoplatform.services.organization.User"%>
<%@ page import="org.exoplatform.services.organization.impl.UserImpl" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="org.exoplatform.web.controller.QualifiedName" %>
<%@ page import="org.exoplatform.web.login.recovery.PasswordRecoveryService" %>
<%@ page import="org.exoplatform.portal.resource.SkinConfig" %>
<%@ page import="java.util.Collection" %>
<%@ page import="java.util.Locale" %>
<%@ page import="org.exoplatform.commons.utils.I18N" %>
<%@ page import="org.exoplatform.portal.config.UserPortalConfigService" %>
<%@ page import="org.exoplatform.portal.resource.config.tasks.PortalSkinTask" %>
<%@ page import="org.exoplatform.portal.branding.BrandingService"%>
<%@ page import="org.exoplatform.services.organization.OrganizationService" %>
<%@ page import="org.exoplatform.services.organization.User"%>
<%@ page language="java" %>
<%

	PortalContainer portalContainer = PortalContainer.getCurrentInstance(session.getServletContext());
    ResourceBundleService service = portalContainer.getComponentInstanceOfType(ResourceBundleService.class);
    Locale locale = (Locale)request.getAttribute("request_locale");
    if (locale == null) {
        locale = request.getLocale();
    }
    ResourceBundle res = service.getResourceBundle(service.getSharedResourceBundleNames(), locale);
    String contextPath = portalContainer.getPortalContext().getContextPath();

    SkinService skinService = PortalContainer.getCurrentInstance(session.getServletContext())
            .getComponentInstanceOfType(SkinService.class);

    UserPortalConfigService userPortalConfigService = portalContainer.getComponentInstanceOfType(UserPortalConfigService.class);
    String skinName = userPortalConfigService.getDefaultPortalSkinName();
    String loginCssPath = skinService.getSkin("portal/login", skinName).getCSSPath();
    String coreCssPath = skinService.getPortalSkin(PortalSkinTask.DEFAULT_MODULE_NAME, skinName).getCSSPath();

    String username = (String)request.getAttribute("username");
    String tokenId = (String)request.getAttribute("tokenId");

    String password = (String)request.getAttribute("password");
    String password2 = (String)request.getAttribute("password2");
	
	OrganizationService organizationService = portalContainer.getComponentInstanceOfType(OrganizationService.class);
	User user = organizationService.getUserHandler().findUserByName(username);
	String fullUsername = user.getDisplayName();

    PasswordRecoveryService passRecoveryServ = portalContainer.getComponentInstanceOfType(PasswordRecoveryService.class);
    String onboardingPasswordPath = passRecoveryServ.getOnboardingURL(tokenId, I18N.toTagIdentifier(locale));


    List<String> errors = (List<String>)request.getAttribute("errors");
    String success = (String)request.getAttribute("success");
    if (errors == null) {
        errors = new ArrayList<String>();
    }
	
	BrandingService brandingService = portalContainer.getComponentInstanceOfType(BrandingService.class);
	String companyName = brandingService.getCompanyName();

    response.setCharacterEncoding("UTF-8");
    response.setContentType("text/html; charset=UTF-8");  
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%=res.getString("onboarding.changePass.title")%></title>
    <%if (success != null && !success.isEmpty()) {%>
        <meta http-equiv="refresh" content="5; url=<%=contextPath+ "/login"%>" />
    <%}%>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <link rel="shortcut icon" type="image/x-icon"  href="<%=contextPath%>/favicon.ico" />
    <link id="brandingSkin" rel="stylesheet" type="text/css" href="/rest/v1/platform/branding/css">
    <link href="<%=loginCssPath%>" rel="stylesheet" type="text/css"/>
	<link rel="stylesheet" type="text/css" href="<%=contextPath%>/login/skin/Stylesheet.css"/>
    <script type="text/javascript" src="/eXoResources/javascript/jquery-3.2.1.js"></script>
    <script type="text/javascript" src="/eXoResources/javascript/eXo/webui/FormValidation.js"></script>
  </head>
  <body>
	<div class="loginBGLight"><span></span></div>
  	<p class="welcomeContent"><%=res.getString("onboarding.login.welcomeTo")%>  <%=companyName%>!</p>
    <div class="uiLogin">
		<div class="uiLoginCondition">
			<p><%=res.getString("onboarding.login.hello")%>  <%=fullUsername%>,</p>
			<p><%=res.getString("onboarding.login.loginText")%>  <%=username%></p>
			<p><%=res.getString("onboarding.login.condition")%></p>
			<p><%=res.getString("onboarding.login.allowCreatePassword")%></p>
		</div>
    	<div class="loginContainer">
			<div class="loginContent">
				<div class="centerLoginContent">
					<% if (errors.size() > 0) { %>
					<div class="alertForm">
						<div class="alert alert-error mgT0 mgB20">
							<ul>
								<% for (String error : errors) { %>
								<li><i class="uiIconError"></i><span><%=error%></span></li>
								<%}%>
							</ul>
						</div>
					</div>
					<%} else if (success != null && !success.isEmpty()) {%>
					<div class="alertForm">
						<div class="alert alert-success">
							<i class="uiIconSuccess"></i><%=success%>
						</div>
					</div>
					<%}%>
					<form name="registerForm" action="<%= contextPath + onboardingPasswordPath %>" method="post" style="margin: 0px;">
						<div class="userCredentials">
							<span class="iconUser"></span>
							<input class="username" data-validation="require" name="username" type="text" value="<%=username%>" readonly="readonly" />
						</div>
						<div class="userCredentials">
						  <span class="iconPswrd"></span>
						  <input data-validation="require" type="password" name="password" autocomplete="off" value="<%=(password != null ? password : "")%>"  placeholder="<%=res.getString("portal.login.Password")%>" onblur="this.placeholder = '<%=res.getString("portal.login.Password")%>'" onfocus="this.placeholder = ''">
						</div>
						<div class="userCredentials">
						  <span class="iconPswrd"></span>
						   <input data-validation="require" type="password" name="password2" autocomplete="off" value="<%=(password2 != null ? password2 : "")%>"  placeholder="<%=res.getString("onboarding.login.confirmPassword")%>" onblur="this.placeholder = '<%=res.getString("onboarding.login.confirmPassword")%>'" onfocus="this.placeholder = ''">
						</div>
						<p class="capatchaMessage"><%=res.getString("onboarding.login.capatcha")%></p>
						<div id="UIPortalLoginFormAction" class="loginButton">
							<button class="button" type="submit"><%=res.getString("onboarding.login.save")%></button>
						</div>
						<input type="hidden" name="action" value="resetPassword"/>
					</form>
					<%/*End form*/%>
				</div>
			</div>
    	</div>
    </div>
    <div class="logoImageContent">
      <img src="/portal/logo/Logo.png" class="logoImage"/>
    </div>
	<script type="text/javascript">
	  var $form = $('form[name="registerForm"]');
	  $form.on('formValidate', function(e, valid) {
		var $btnSubmit = $form.find('.btn[type="submit"]');
		if (valid) {            
		  $btnSubmit.attr('disabled', false).removeClass('disabled');
		} else {
		  $btnSubmit.attr('disabled', true).addClass('disabled');
		}
	  });
	  $form.find('input[type="text"], input[type="password"]').on('keyup', function() {
		$form.validate();
	  });
	</script>
  </body>
</html>
