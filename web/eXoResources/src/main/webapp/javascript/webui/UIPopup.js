/**
 * Copyright (C) 2009 eXo Platform SAS.
 * 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

/* TODO: need to manage zIndex for all popup */
/**
 * Main class to manage popups
 */

(function($, base) {
	eXo.webui.UIPopup = {
	
	  zIndex : 2000,
	
	  /**
	   * Inits the popup . calls changezIndex when the users presses the popup
	   */
	  init : function(popup, containerId) {
	    if (typeof (popup) == "string")
	      popup = document.getElementById(popup);
	    if (containerId)
	      popup.containerId = containerId;
	    popup.onmousedown = this.changezIndex;
	  },
	  /**
	   * Increments the current zIndex value and sets this popup's zIndex property
	   * to this value
	   */
	  changezIndex : function() {
	    this.style.zIndex = ++eXo.webui.UIPopup.zIndex;
	  },
	  /**
	   * Creates and returns a div element with the following style properties .
	   * position: relative . display: none
	   */
	  create : function() {
	    var popup = document.createElement("div");
	    popup.style.position = "relative";
	    popup.style.display = "none";
	    return popup;
	  },
	  /**
	   * Sets the size of the popup with the given width and height parameters
	   */
	  setSize : function(popup, w, h) {
	    popup.style.width = w + "px";
	    popup.style.height = h + "px";
	  },
	  /**
	   * Shows (display: block) the popup
	   */
	  show : function(popup) {
	    if (typeof (popup) == "string") {
	      popup = document.getElementById(popup);
	    }
	
	    var uiMaskWS = document.getElementById("UIMaskWorkspace");
	    if (uiMaskWS) {
	      uiMaskWSzIndex = $(uiMaskWS).css("zIndex");
	      if (uiMaskWSzIndex && (uiMaskWSzIndex > eXo.webui.UIPopup.zIndex)) {
	        eXo.webui.UIPopup.zIndex = uiMaskWSzIndex;
	      }
	    }
	
	    popup.style.zIndex = ++eXo.webui.UIPopup.zIndex;
	    popup.style.display = "block";
	  },
	  /**
	   * Shows (display: none) the popup
	   */
	  hide : function(popup) {
	    if (typeof (popup) == "string") {
	      popup = document.getElementById(popup);
	    }
	
	    popup.style.display = "none";
	  },
	  /**
	   * Sets the position of the popup to x and y values changes the style
	   * properties : . position: absolute . top and left to y and x respectively if
	   * the popup has a container, set its position: relative too
	   */
	  setPosition : function(popup, x, y, isRTL) {
	    if (popup.containerId) {
	      var container = document.getElementById(popup.containerId);
	      container.style.position = "relative";
	    }
	    popup.style.position = "absolute";
	    popup.style.top = y + "px";
	    if (isRTL) {
	      popup.style.right = x + "px";
	      popup.style.left = "";
	    } else {
	      popup.style.left = x + "px";
	      popup.style.right = "";
	    }
	  },
	  /**
	   * Aligns the popup according to the following values : 1 : top left 2 : top
	   * right 3 : bottom left 4 : bottom right other : center
	   */
	  setAlign : function(popup, pos, hozMargin, verMargin) {
	    if (typeof (popup) == 'string')
	      popup = document.getElementById(popup);
	    var stdLeft = $(window).width()
	        - base.Browser.findPosX(document
	            .getElementById("UIWorkingWorkspace"));
	    var intTop = 0;
	    var intLeft = 0;
	    if (!hozMargin)
	      hozMargin = 0;
	    if (!verMargin)
	      verMargin = 0;           
	
	    var browserHeight = $(window).height();
	    switch (pos) {
	    case 1: // Top Left
	      intTop = verMargin;
	      intLeft = hozMargin;
	      break;
	    case 2: // Top Right
	      intTop = verMargin;
	      intLeft = (stdLeft - popup.offsetWidth) - hozMargin;
	      break;
	    case 3: // Bottom Left
	      intTop = (browserHeight - popup.offsetHeight)
	          - verMargin;
	      intLeft = hozMargin;
	      break;
	    case 4: // Bottom Right
	      intTop = (browserHeight - popup.offsetHeight)
	          - verMargin;
	      intLeft = (stdLeft - popup.offsetWidth) - hozMargin;
	      break;
	    default:
	      intTop = (browserHeight - popup.offsetHeight) / 2;
	      intLeft = (uiWorkingWS.offsetWidth - popup.offsetWidth) / 2;
	      break;
	    }
	
	    this.setPosition(popup, intLeft, intTop, eXo.core.I18n.isRT());
	  },
	  /**
	   * Inits the DragDrop class with empty values
	   */
	  initDND : function(evt) {
	    var DragDrop = eXo.core.DragDrop;
	
	    var clickBlock = this;
	    var dragBlock = $(clickBlock).parent().closest(".UIDragObject")[0];
	    DragDrop.init(clickBlock, dragBlock);
	  }
	};
	return eXo.webui.UIPopup;
})($, base);
