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
<h3>Account Information</h3>

<table>
  <tr>
    <td>First name:</td><td><html:text name="accountBean" property="account.firstName"/></td>
  </tr><tr>
  <td>Last name:</td><td><html:text name="accountBean" property="account.lastName"/></td>
</tr><tr>
  <td>Email:</td><td><html:text size="40" name="accountBean" property="account.email"/></td>
</tr><tr>
  <td>Phone:</td><td><html:text name="accountBean" property="account.phone"/></td>
</tr><tr>
  <td>Address 1:</td><td><html:text size="40" name="accountBean" property="account.address1"/></td>
</tr><tr>
  <td>Address 2:</td><td><html:text size="40" name="accountBean" property="account.address2"/></td>
</tr><tr>
  <td>City:</td><td><html:text name="accountBean" property="account.city"/></td>
</tr><tr>
  <td>State:</td><td><html:text size="4" name="accountBean" property="account.state"/></td>
</tr><tr>
  <td>Zip:</td><td><html:text size="10" name="accountBean" property="account.zip"/></td>
</tr><tr>
  <td>Country:</td><td><html:text size="15" name="accountBean" property="account.country"/></td>
</tr>
</table>

<h3>Profile Information</h3>

<table>
  <tr>
    <td>Language Preference:</td><td>
    <html:select name="accountBean" property="account.languagePreference">
      <html:options name="accountBean" property="languages"/>
    </html:select></td>
  </tr><tr>
  <td>Favourite Category:</td><td>
  <html:select name="accountBean" property="account.favouriteCategoryId">
    <html:options name="accountBean" property="categories"/>
  </html:select></td>
</tr><tr>
  <td>Enable MyList</td><td><html:checkbox name="accountBean" property="account.listOption"/></td>
</tr><tr>
  <td>Enable MyBanner</td><td><html:checkbox name="accountBean" property="account.bannerOption"/></td>
</tr>

</table>
