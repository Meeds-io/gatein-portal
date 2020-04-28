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
  <html:form action="/shop/signon" method="POST">

    <p>Please enter your username and password.</p>
    <p>
      Username:<input type="text" name="username" value="j2ee"/>
      <br/>
      Password:<input type="password" name="password" value="j2ee"/>
    </p>
    <input type="submit" name="submit" value="Login"/>

  </html:form>

  Need a username and password?
  <html:link page="/shop/newAccountForm.shtml">Register Now!</html:link>

</div>

<%@ include file="../common/IncludeBottom.jsp" %>

