<!--
This file is part of the Meeds project (https://meeds.io/).
Copyright (C) 2020 Meeds Association
contact@meeds.io
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 3 of the License, or (at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.
You should have received a copy of the GNU Lesser General Public License
along with this program; if not, write to the Free Software Foundation,
Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
-->
<%@ page contentType="text/html" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://portals.apache.org/bridges/struts/tags-portlet-html-el" prefix="html" %>


<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">


<style>
body {
  margin: 0ex 10ex 0ex 10ex;
  padding: 0ex;
  font-family: helvetica, tahoma, arial, verdana, sans-serif;
  font-size: 2ex;
  color: #333;
  background-color: #444;
}

pre {
  font-family: "Courier New", Courier, mono;

  font-style: normal;
  background-color: #FFFFFF;
  white-space: pre
}

h1 {
  margin: 1ex 0ex 1ex 0ex;
  padding: 0ex;

  line-height: 3ex;
  font-weight: 900;
  color: #666;
}

h2 {
  margin: 2ex 0ex 1ex 0ex;
  padding: 0ex;

  line-height: 2ex;
  font-weight: 700;
  color: #444;
}

h3 {
  margin: 1ex 0ex 1ex 0ex;
  padding: 0ex;

  line-height: 1.6ex;
  font-weight: 700;
  color: #222;
}

p {
  font-family: helvetica, tahoma, arial, verdana, sans-serif;

  margin: 0ex 0ex 0ex 0ex;
  padding: 2ex;
}

img {
  border: 0;
}

li {
  font-family: helvetica, tahoma, arial, verdana, sans-serif;

  margin: 0ex 0ex 0ex 0ex;
  padding: 0ex;
}

table {
  border-width: 0;
  empty-cells: show;
}

td, th {
  empty-cells: show;
  padding: .3ex .3ex;
  vertical-align: top;
  text-align: left;
  border-width: 0;
  border-spacing: 0;
  background-color: #ececec
}

th {
  font-weight: bold;
  background-color: #e2e2e2;
}

a, a:visited, a:link {
  color: #039;

  text-decoration: none;
  font-family: helvetica, tahoma, arial, verdana, sans-serif;
}

a:hover {
  color: #69f;
}

a.Button, a.Button:link, a.Button:visited {
  padding: .3ex;
  color: #fff;
  background-color: #005e21;
  text-decoration: none;
  font-family: helvetica, tahoma, arial, verdana, sans-serif;
  font-size: 1.5ex;
}

a.Button:hover {
  color: #000;
  background-color: #54c07a;
}

#Logo {
  width: 33%;
  height: 9ex;
  margin: 0ex 0ex 0ex 0ex;
  padding: 0ex 0ex 0ex 0ex;
  border-width: 0ex 0ex .3ex 0px;
  border-style: solid;
  border-color: #ccc;
  float: left;
  background-color: #000;
  color: #fff;
  line-height: 9ex;
  voice-family: "\"}\"";
  voice-family: inherit;
  height: 9ex;
}

body>#Logo {
  height: 9ex;
}

#Menu {
  width: 33%;
  height: 9ex;
  margin: 0ex 0ex 0ex 0ex;
  padding: 0ex 0ex 0ex 0ex;
  border-width: 0ex 0ex .3ex 0px;
  border-style: solid;
  border-color: #ccc;
  float: left;
  background-color: #000;
  color: #eaac00;
  text-decoration: none;
  font-family: helvetica, tahoma, arial, verdana, sans-serif;
  text-align: center;
  line-height: 9ex;
  voice-family: "\"}\"";
  voice-family: inherit;
  height: 9ex;
}

#Menu, #Menu a, #Menu a:link, #Menu a:visited, #Menu a:hover {
  color: #eaac00;
  text-decoration: none;
  font-family: helvetica, tahoma, arial, verdana, sans-serif;
}

body>#Menu {
  height: 9ex;
}

 {
}

#Search {
  width: 33%;
  height: 9ex;
  margin: 0ex 0ex 0ex 0ex;
  padding: 0ex 0ex 0ex 0ex;
  border-width: 0ex 0ex .3ex 0px;
  border-style: solid;
  border-color: #ccc;
  float: left;
  text-align: center;
  background-color: #000;
  color: #eaac00;
  line-height: 9ex;
  voice-family: "\"}\"";
  voice-family: inherit;
  height: 9ex;
}

body>#Search {
  height: 9ex;
}

#Search input {
  border-width: .1ex .1ex .1ex .1ex;
  border-style: solid;
  border-color: #aaa;
  background-color: #666;
  color: #eaac00;
}

#QuickLinks {
  text-align: center;
  background-color: #FFF;
  width: 99%;
}

#PoweredBy {
  width: 30%;
  height: 9ex;
  margin: 0ex 0ex 0ex 0ex;
  padding: 0ex 0ex 0ex 0ex;
  border-width: .3ex 0ex .3ex 0px;
  border-style: solid;
  border-color: #ccc;
  float: left;
  background-color: #000;
  color: #fff;
  line-height: 9ex;
  voice-family: "\"}\"";
  voice-family: inherit;
  height: 9ex;
}

body>#PoweredBy {
  height: 9ex;
}

#Banner {
  width: 69%;
  height: 9ex;
  margin: 0ex 0ex 0ex 0ex;
  padding: 0ex 0ex 0ex 0ex;
  border-width: .3ex 0ex .3ex 0px;
  border-style: solid;
  border-color: #ccc;
  float: left;
  background-color: #000;
  color: #fff;
  line-height: 9ex;
  voice-family: "\"}\"";
  voice-family: inherit;
  height: 9ex;
}

body>#Banner {
  height: 9ex;
}

#Content {
  margin: 0;
  padding: 0ex 0ex 0ex 0ex;
  width: 99%;
  color: #333;
  background-color: #FFF;
  border-width: 0;
}

#Separator {
  clear:both;
  margin: 0;
  height:0;
}

#Main {
  margin: 0;
  padding: 1ex;
  color: #333;
  background-color: #FFF;
  border-width: 1ex 0ex 2ex 0px;
  border-style: solid;
  border-color: #fff;
}

#Sidebar {
  float: left;
  background:inherit;
  width: 30%;
}

#MainImage {
  float: left;
  background:inherit;
  text-align:center;
  width: 50%;
}

#Catalog {
  padding: 1ex;
  background:inherit;
  text-align:center;
}

#Catalog input[type="submit"]{
  padding: .3ex;
  color: #fff;
  background-color: #005e21;
  text-decoration: none;
  font-family: helvetica, tahoma, arial, verdana, sans-serif;
  font-size: 1.5ex;
  border-width:0;
}
#Catalog input[type="submit"]:hover {
  color: #000;
  background-color: #54c07a;
  cursor:pointer;
}

#Catalog table{
  margin-left:auto;
  margin-right:auto;
}

#BackLink{
  padding: 1ex;
  float: right;
  border-width: .1ex 0ex .1ex 0px;
  border-style: solid;
  border-color: #000;
}

#Cart{
  width: 69.99%;
  float: left;
  background-color:#fff;
}

#MyList{
  width: 30%;
  float: left;
  background-color:#ccc;
  text-align:left;
}


</style>
<head>
  <meta name="generator"
        content="HTML Tidy for Linux/x86 (vers 1st November 2002), see www.w3.org"/>
  <title>JPetStore Demo</title>
  <meta content="text/html; charset=windows-1252" http-equiv="Content-Type"/>
  <meta http-equiv="Cache-Control" content="max-age=0"/>
  <meta http-equiv="Cache-Control" content="no-cache"/>
  <meta http-equiv="expires" content="0"/>
  <meta http-equiv="Expires" content="Tue, 01 Jan 1980 1:00:00 GMT"/>
  <meta http-equiv="Pragma" content="no-cache"/>
</head>

<body>

<div id="Header">
  
  <div id="Logo">
    <div id="LogoContent">
      <html:link page="/shop/index.shtml"><html:img src="../images/logo-topbar.gif"/></html:link>
    </div>
  </div>

  <div id="Menu">
    <div id="MenuContent">
      <html:link page="/shop/viewCart.shtml"><html:img align="middle" imageName="img_cart" src="../images/cart.gif"/></html:link>
      <html:img align="middle" src="../images/separator.gif"/>
      <logic:notPresent name="accountBean" scope="session">
        <html:link page="/shop/signonForm.shtml">Sign In</html:link>
      </logic:notPresent>
      <logic:present name="accountBean" scope="session">
        <logic:notEqual name="accountBean" property="authenticated" value="true" scope="session">
          <html:link page="/shop/signonForm.shtml">Sign In</html:link>
        </logic:notEqual>
      </logic:present>
      <logic:present name="accountBean" scope="session">
        <logic:equal name="accountBean" property="authenticated" value="true" scope="session">
          <html:link page="/shop/signoff.shtml">Sign Out</html:link>
          <html:img align="middle" src="../images/separator.gif"/>
          <html:link page="/shop/editAccountForm.shtml">My Account</html:link>
        </logic:equal>
      </logic:present>

      <html:img align="middle" src="../images/separator.gif"/>
      <html:link href="../help.html">?</html:link>
    </div>
  </div>

  <div id="Search">
    <div id="SearchContent">
      <html:form method="post" action="/shop/searchProducts.shtml">
        <input name="keyword" size="14"/>&nbsp;<input type="submit" name="SearchButton"
        value="Search"/>
      </html:form>
    </div>
  </div>

  <div id="QuickLinks">
    <html:link page="/shop/viewCategory.shtml?categoryId=FISH">
      <html:img src="../images/sm_fish.gif"/></html:link>
    <html:img src="../images/separator.gif"/>
    <html:link page="/shop/viewCategory.shtml?categoryId=DOGS">
      <html:img src="../images/sm_dogs.gif"/></html:link>
    <html:img src="../images/separator.gif"/>
    <html:link page="/shop/viewCategory.shtml?categoryId=REPTILES">
      <html:img src="../images/sm_reptiles.gif"/></html:link>
    <html:img src="../images/separator.gif"/>
    <html:link page="/shop/viewCategory.shtml?categoryId=CATS">
      <html:img src="../images/sm_cats.gif"/></html:link>
    <html:img src="../images/separator.gif"/>
    <html:link page="/shop/viewCategory.shtml?categoryId=BIRDS">
      <html:img src="../images/sm_birds.gif"/></html:link>
  </div>

</div>

<div id="Content">

<html:errors/>

<!-- Support for non-traditional but simple message -->
<logic:present name="message">
  <bean:write name="message"/>
</logic:present>

<!-- Support for non-traditional but simpler use of errors... -->
<logic:present name="errors">
  <logic:iterate id="error" name="errors">
    <bean:write name="error"/>
  </logic:iterate>
</logic:present>