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

<bean:define id="product" name="catalogBean" property="product"/>
<bean:define id="itemList" name="catalogBean" property="itemList"/>

<div id="BackLink">

  <html:link paramId="categoryId" paramName="product" paramProperty="categoryId" page="/shop/viewCategory.shtml">
    Return to <bean:write name="product" property="categoryId"/></html:link>

</div>

<div id="Catalog">

  <h2><bean:write name="product" property="name"/></h2>

  <table>
    <tr><th>Item ID</th>  <th>Product ID</th>  <th>Description</th>  <th>List
      Price</th>  <th>&nbsp;</th></tr>
    <logic:iterate id="item" name="itemList">
      <tr>
        <td>
          <html:link paramId="itemId" paramName="item" paramProperty="itemId" page="/shop/viewItem.shtml">
            <bean:write name="item" property="itemId"/></html:link></td>
        <td><bean:write name="item" property="productId"/></td>
        <td>
          <bean:write name="item" property="attribute1"/>
          <bean:write name="item" property="attribute2"/>
          <bean:write name="item" property="attribute3"/>
          <bean:write name="item" property="attribute4"/>
          <bean:write name="item" property="attribute5"/>
          <bean:write name="product" property="name"/>
        </td>
        <td><bean:write name="item" property="listPrice" format="$#,##0.00"/></td>
        <td><html:link styleClass="Button" paramId="workingItemId" paramName="item" paramProperty="itemId" page="/shop/addItemToCart.shtml">
          Add to Cart</html:link></td>
      </tr>
    </logic:iterate>
    <tr><td>
      <logic:notEqual name="itemList" property="firstPage" value="true">
        <a class="Button" href="switchItemListPage.shtml?pageDirection=previous">&lt;&lt; Prev</a>
      </logic:notEqual>
      <logic:notEqual name="itemList" property="lastPage" value="true">
        <a class="Button" href="switchItemListPage.shtml?pageDirection=next">Next &gt;&gt;</a>
      </logic:notEqual>
    </td></tr>
  </table>

</div>

<%@ include file="../common/IncludeBottom.jsp" %>





