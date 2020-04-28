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

<h2>My Orders</h2>

<table>
  <tr><th>Order ID</th>  <th>Date</th>  <th>Total Price</th></tr>

  <logic:iterate id="order" name="orderBean" property="orderList">
    <tr>
      <td><html:link paramId="orderId" paramName="order" paramProperty="orderId" page="/shop/viewOrder.shtml">
        <bean:write name="order" property="orderId"/></html:link></td>
      <td><bean:write name="order" property="orderDate" format="yyyy/MM/dd hh:mm:ss"/></td>
      <td><bean:write name="order" property="totalPrice" format="$#,##0.00"/></td>
    </tr>
  </logic:iterate>
</table>

<logic:notEqual name="orderBean" property="orderList.firstPage" value="true">
  <a href="switchOrderPage.shtml?pageDirection=previous">&lt;&lt; Previous</a>
</logic:notEqual>
<logic:notEqual name="orderBean" property="orderList.lastPage" value="true">
  <a href="switchOrderPage.shtml?pageDirection=next">Next &gt;&gt;</a>
</logic:notEqual>

<%@ include file="../common/IncludeBottom.jsp" %>


