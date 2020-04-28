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
<div style="float: left; width: 70%">
<ul>
  <li>
    <p style="color: blue">Get information on portlet 's own JQuery use:</p>
    <pre>$(function() {</pre>
    <pre>  $(document).delegate("#portletJQuery", "click", function() {</pre>
    <pre>    //bind to document so that events presist over ajax page reloads</pre>
    <pre>    //delegate since jQuery 1.6 doesn't include on()</pre>
    <pre>    $('#result').append("&lt;p&gt;The JQuery's version: " + $().jquery + "&lt;/p&gt;");</pre>
    <pre>    $('#result').children('p').fadeOut(3200);</pre>
    <pre>  });</pre>
    <pre>});</pre>
    <div id="portletJQuery"><span style="color:green">Click here</span></div>
  </li>
  <li>
    <p style="color: blue">Get information on GateIn 's own JQuery use:</p>
    <pre>require(["SHARED/jquery"], function($) {</pre>
    <pre>  $(document).on("click", "#gateinJQuery", function() {</pre>
    <pre>    //bind to document so that events presist over ajax page reloads</pre>
    <pre>    $('#result').append("&lt;p&gt;The JQuery's version: " + $().jquery + "&lt;/p&gt;");</pre>
    <pre>    $('#result').children('p').fadeOut(3200);</pre>
    <pre>  });</pre> 
    <pre>});</pre>
    <div id="gateinJQuery"><span style="color:green">Click here</span></div>
  </li>
</ul>
</div>
<div id="result" style="float: right; width: 30%"><p></p></div>
