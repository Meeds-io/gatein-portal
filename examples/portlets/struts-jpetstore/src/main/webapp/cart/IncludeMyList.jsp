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
<bean:define id="myList" name="accountBean" property="myList"/>

<logic:present name="myList">
  <p>
    Pet Favorites
    <br/>
    Shop for more of your favorite pets here.
  </p>
  <ul>
    <logic:iterate id="product" name="myList">
      <li><html:link paramId="productId" paramName="product" paramProperty="productId" page="/shop/viewProduct.shtml">
        <bean:write name="product" property="name"/></html:link>
      (<bean:write name="product" property="productId"/>)</li>
    </logic:iterate>
  </ul>

  <p>
    <logic:notEqual name="myList" property="firstPage" value="true">
      <a href="switchMyListPage.shtml?pageDirection=previous&account.listOption=<bean:write name="accountBean"
          property="account.listOption"/>&account.bannerOption=< bean:write name="accountBean"
                                                                 property="account.bannerOption"/>">&lt;&lt;Prev</a>
    </logic:notEqual>
    <logic:notEqual name="myList" property="lastPage" value="true">
      <a href="switchMyListPage.shtml?pageDirection=next&account.listOption=<bean:write name="accountBean"
          property="account.listOption"/>&account.bannerOption=< bean:write name="accountBean"
                                                                 property="account.bannerOption"/>">Next &gt;&gt;</a>
    </logic:notEqual>
  </p>

</logic:present>




