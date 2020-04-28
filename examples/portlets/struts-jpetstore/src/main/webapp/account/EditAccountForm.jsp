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
<%@ include file="../common/IncludeTop.jsp" %>

<div id="Catalog">

  <html:form method="post" action="/shop/editAccount.shtml">

    <html:hidden name="accountBean" property="validation" value="edit"/>
    <html:hidden name="accountBean" property="username"/>

    <h3>User Information</h3>

    <table>
      <tr>
        <td>User ID:</td><td><bean:write name="accountBean" property="username"/></td>
      </tr><tr>
      <td>New password:</td><td><html:password name="accountBean" property="password"/></td>
    </tr><tr>
      <td>Repeat password:</td><td><html:password name="accountBean" property="repeatedPassword"/></td>
    </tr>
    </table>
    <%@ include file="IncludeAccountFields.jsp" %>

    <input type="submit" name="submit" value="Save Account Information"/>

  </html:form>

  <html:link page="/shop/listOrders.shtml">My Orders</html:link>

</div>

<%@ include file="../common/IncludeBottom.jsp" %>


