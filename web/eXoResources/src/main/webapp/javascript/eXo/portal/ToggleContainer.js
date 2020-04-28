/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
(function($) {

  eXo.portal.ToggleContainer  = {
    addContainer: function(containerId, toggleWidth) {
      if (!containerId || !containerId.length || !toggleWidth || !toggleWidth.length || toggleWidth.indexOf("%") > 0 ) {
        return;
      }
      
      toggleWidth = toggleWidth.split("px")[0];
      toggleWidth = parseInt(toggleWidth);
      if (toggleWidth == NaN)
      {
	return;
      }

      
      var checkSizeHandler = function(event) {
        var containerId = event.data.containerId;
        var toggleWidth = event.data.toggleWidth;
        checkSize(containerId, toggleWidth);
      }

      var checkSize = function(containerId, toggleWidth) {
        var container = $("#" + containerId);
        var currentWidth = container.outerWidth();
        if (currentWidth > toggleWidth) { 
          container.removeClass("ToggledRow");
        } else {
          container.addClass("ToggledRow");
        }
      }
       
      $(window).resize({"containerId": containerId, "toggleWidth": toggleWidth}, checkSizeHandler);
      checkSize(containerId,toggleWidth);

    }
  };
  
  return eXo.portal.ToggleContainer;
})($); 
