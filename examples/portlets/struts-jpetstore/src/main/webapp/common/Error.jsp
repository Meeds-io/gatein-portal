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
<%@ page import="java.io.PrintWriter" %>
<%@ include file="../common/IncludeTop.jsp" %>

<logic:notPresent name="BeanActionException">
  <logic:notPresent name="message">
    <h3>Something happened...</h3>
    <b>But no further information was provided.</b>
  </logic:notPresent>
</logic:notPresent>
<p/>
<logic:present name="BeanActionException">
  <h3>Error!</h3>
  <b><bean:write name="BeanActionException" property="class.name"/></b>

  <p/>
  <bean:write name="BeanActionException" property="message"/>
</logic:present>
<p/>
<logic:present name="BeanActionException">
  <h4>Stack</h4>
  <pre>
    <%
      Exception e = (Exception) request.getAttribute("BeanActionException");
      e.printStackTrace(new PrintWriter(out));
    %>
  </pre>
</logic:present>

<%@ include file="../common/IncludeBottom.jsp" %>