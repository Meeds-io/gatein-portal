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

<bean:define id="productList" name="catalogBean" property="productList"/>

<div id="BackLink">

  <html:link page="/shop/index.shtml">Return to Main Menu</html:link>

</div>

<div id="Catalog">

  <table>
    <tr><th>&nbsp;</th>  <th>Product ID</th>  <th>Name</th></tr>
    <logic:iterate id="product" name="productList">
      <tr>
        <td><html:link paramId="productId" paramName="product" paramProperty="productId" page="/shop/viewProduct.shtml">
              <logic:notEmpty name="product" property="imageSource">
                <html:img src="${product.imageSource}"/>
              </logic:notEmpty>
          <bean:write filter="false" name="product" property="cleanDescription"/>
          </html:link></td>
        <td><b><html:link paramId="productId" paramName="product" paramProperty="productId"
                          page="/shop/viewProduct.shtml"><font color="BLACK"><bean:write name="product"
                                                                                         property="productId"/></font>
        </html:link></b></td>
        <td><bean:write name="product" property="name"/></td>
      </tr>
    </logic:iterate>
    <tr>
      <td>
        <logic:notEqual name="productList" property="firstPage" value="true">
          <a href="switchSearchListPage.shtml?pageDirection=previous">&lt;&lt; Previous</a>
        </logic:notEqual>
        <logic:notEqual name="productList" property="lastPage" value="true">
          <a href="switchSearchListPage.shtml?pageDirection=next">Next &gt;&gt;</a>
        </logic:notEqual>
      </td>
    </tr>

  </table>

</div>

<%@ include file="../common/IncludeBottom.jsp" %>




