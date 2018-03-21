/**
 * Copyright (C) 2009 eXo Platform SAS.
 * 
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */

(function($, base, uiPopup) {
	eXo.webui.UIRightClickPopupMenu = {
	
	  /**
	   * Initialize a UIRightClickPopupMenu object
	   * 
	   * @param contextMenuId
	   *          identifier of a document object
	   */
	  init : function(contextMenuId) {
	    var menu = $("#" + contextMenuId);
	    menu.mousedown(function()
	    {
	      return false;
	    });
	
	    /**
	     * Disable/enable browser's default right click handler
	     */
	    this.disableContextMenu(menu.parent());
	
	    menu.find('.UIRightPopupMenuContainer, .uiRightPopupMenuContainer').on('click', 'div.MenuItem a, .menuItem a', eXo.webui.UIRightClickPopupMenu.prepareObjectIdEvt);
	  },
	  /**
	   * Hide and disable mouse down event of context menu object
	   * 
	   * @param contextId
	   *          identifier of context menu
	   */
	  hideContextMenu : function(contextId) {
	    $("#" + contextId).css("display", "none");
	  },
	
	  /**
	   * Disable default context menu of browser
	   * 
	   * @param comp
	   *          identifier or document object
	   */
	  disableContextMenu : function(comp) {
	    if (typeof (comp) == "string")
	      comp = $("#" + comp);
	
	    comp.mouseover(function()
	    {
	      document.oncontextmenu = function()
	      {
	        return false;
	      }
	    });
	
	    comp.mouseout(function()
	    {
	      document.oncontextmenu = function()
	      {
	        return true;
	      }
	    });
	  },
	
	  /**
	   * An event handler in JQuery
	   * 
	   * Prepare objectId for context menu Make ajaxPost request if needed
	   */
	  prepareObjectIdEvt : function(event) {
	    event.stopPropagation();
	
	    var contextMenu = $(this).closest(".UIRightClickPopupMenu, .uiRightClickPopupMenu")[0];
	    contextMenu.style.display = "none";
	    var href = this.getAttribute('href');
	    if (!href) {
	      return;
	    }
	    if (href.indexOf("ajaxGet") != -1) {
	      href = href.replace("ajaxGet", "ajaxPost");
	      this.setAttribute('href', href);
	    }
	    if (href.indexOf("objectId") != -1 || !contextMenu.objId) {
	      return;
	    }
	    var objId = encodeURIComponent(contextMenu.objId.replace(/'/g, "\\'"));
	
	    if (href.indexOf("javascript") == -1) {
	      this.setAttribute('href', href + "&objectId=" + objId);
	      return;
	    } else if (href.indexOf("window.location") != -1) {
	      href = href.substr(0, href.length - 1) + "&objectId=" + objId + "'";
	    } else if (href.indexOf("ajaxPost") != -1) {
	      href = href.substr(0, href.length - 2) + "', 'objectId=" + objId + "')";
	    } else {
	      href = href.substr(0, href.length - 2) + "&objectId=" + objId + "')";
	    }
	
	    eval(href);
	    if (event && event.preventDefault)
	      event.preventDefault();
	    else
	      window.event.returnValue = false;
	    return false;
	  },
	
	  /**
	   * Mouse click on element, If click is right-click, the context menu will be
	   * shown
	   * 
	   * @param {Object}
	   *          event
	   * @param {Object}
	   *          elemt clicked element
	   * @param {String}
	   *          menuId identifier of context menu will be shown
	   * @param {String}
	   *          objId object identifier in tree
	   * @param {Array}
	   *          params
	   * @param {Number}
	   *          opt option
	   */
	  clickRightMouse : function(event, elemt, menuId, objId, whiteList, opt) {
	    if (!event)
	      event = window.event;
	
	    var contextMenu = document.getElementById(menuId);
	    contextMenu.objId = objId;
	
	    //help to disable browser context menu
	    //when onmouseover is registered after the dom has already displayed, mouseover evt'll not be raised
	    var parent = $(contextMenu).parent();
	    if (!document.oncontextmenu) {
	    	parent.trigger("mouseover");
	    }
	    
	    if(event.which != 2 && event.which !=3 && event.button !=2)
	    {
	      contextMenu.style.display = "none";
	      return;
	    }
	
	    var jDoc = $(document);
	    jDoc.trigger("mousedown.RightClickPopUpMenu");    
	    //Register closing contextual menu callback on document
	    jDoc.one("mousedown.RightClickPopUpMenu", function(e)
	    {
	    	eXo.webui.UIRightClickPopupMenu.hideContextMenu(menuId);
	    });
	
	    //The callback registered on document won't be triggered by current 'mousedown' event
	    if ( event.stopPropagation ) {
	    	event.stopPropagation();
	    }
	    event.cancelBubble = true;
	
	    if (whiteList) {
	      $(contextMenu).find("a").each(function()
	      {
	        var item = $(this);
	        if(whiteList.indexOf(item.attr("exo:attr")) > -1)
	        {
	          item.css("display", "block");
	        }
	        else
	        {
	          item.css("display", "none");
	        }
	      });
	    }
	
	    var customItem = $(elemt).find(".RightClickCustomItem").eq(0);
	    var tmpCustomItem = $(contextMenu).find(".RightClickCustomItem").eq(0);
	    if(customItem && tmpCustomItem)
	    {
	      tmpCustomItem.html(customItem.html());
	      tmpCustomItem.css("display", "inline");
	    }
	    else if(tmpCustomItem)
	    {
	      tmpCustomItem.css("display", "none");
	    }
	    /*
	     * fix bug right click in IE7.
	     */
	    var fixWidthForIE7 = 0;
	    var UIWorkingWorkspace = document.getElementById("UIWorkingWorkspace");
	    if (base.Browser.isIE7() && document.getElementById("UIDockBar")) {
	      if (event.clientX > UIWorkingWorkspace.offsetLeft)
	        fixWidthForIE7 = UIWorkingWorkspace.offsetLeft;
	    }
	
	    eXo.core.Mouse.update(event);
	    uiPopup.show(contextMenu);
	
	    var ctxMenuContainer = $(contextMenu).children("div.UIContextMenuContainer, .uiContextMenuContainer")[0];
	    var offset = $(contextMenu).offset();
	    var intTop = eXo.core.Mouse.mouseyInPage
	        - (offset.top - contextMenu.offsetTop);
	    var intLeft = eXo.core.Mouse.mousexInPage
	        - (offset.left - contextMenu.offsetLeft)
	        + fixWidthForIE7;
        var overflowValue = intLeft + contextMenu.offsetWidth - $(window).width();
        
        if(overflowValue > 0) { // if the context menu overflow we subtract the overflow value from left value
          intLeft -= overflowValue;
        }
        
	    if (eXo.core.I18n.isRT()) {
	      // scrollWidth is width of browser scrollbar
	      var scrollWidth = 16;
	      if (base.Browser.isFF())
	        scrollWidth = 0;
	      intLeft = contextMenu.offsetParent.offsetWidth - intLeft + fixWidthForIE7
	          + scrollWidth;
	      var clickCenter = $(contextMenu).find("div.ClickCenterBottom")[0];
	      if (clickCenter) {
	        var clickCenterWidth = clickCenter ? parseInt($(clickCenter).css("marginRight")) : 0;
	        intLeft += (ctxMenuContainer.offsetWidth - 2 * clickCenterWidth);
	      }
	    }
	
	    var jWin = $(window);
	    var browserHeight = jWin.height();
	    var browserWidth = jWin.width();
	    switch (opt) {
	    case 1:
	      intTop -= ctxMenuContainer.offsetHeight;
	      break;
	    case 2:
	      break;
	    case 3:
	      break;
	    case 4:
	      break;
	    default:
	      // if it isn't fit to be showed down BUT is fit to to be showed up
	      if ((eXo.core.Mouse.mouseyInClient + ctxMenuContainer.offsetHeight) > browserHeight
	          && (intTop > ctxMenuContainer.offsetHeight)) {
	        intTop -= ctxMenuContainer.offsetHeight;
	      }
	      break;
	    }
	
	    if (eXo.core.I18n.isLT()) {
	      // move context menu to center of screen to fix width
	      contextMenu.style.left = browserWidth * 0.5 + "px";
	      ctxMenuContainer.style.width = "auto";
	      ctxMenuContainer.style.width = ctxMenuContainer.offsetWidth + 2 + "px";
	      // end fix width
	      // need to add 1 more pixel because IE8 will dispatch onmouseout event to
	      // contextMenu.parent
	      contextMenu.style.left = (intLeft + 1) + "px";
	    } else {
	      // move context menu to center of screen to fix width
	      contextMenu.style.right = browserWidth * 0.5 + "px";
	      ctxMenuContainer.style.width = "auto";
	      ctxMenuContainer.style.width = ctxMenuContainer.offsetWidth + 2 + "px";
	      // end fix width
	      contextMenu.style.right = intLeft + "px";
	    }
	    ctxMenuContainer.style.width = ctxMenuContainer.offsetWidth + "px";
	    // need to add 1 more pixel because IE8 will dispatch onmouseout event to
	    // contextMenu.parent
	    if ((eXo.core.Mouse.mouseyInClient + ctxMenuContainer.offsetHeight) <= browserHeight) {
	      intTop += 1
	    }
	    contextMenu.style.top = intTop + "px";
	  }
	};
	
	eXo.core.Mouse = {
	  init : function (mouseEvent) {
	    this.mousexInPage = null ;
	    this.mouseyInPage = null ;
	
	    this.mousexInClient = null ;
	    this.mouseyInClient = null ;
	
	    this.lastMousexInClient = null ;
	    this.lastMouseyInClient = null ;
	
	    this.deltax = null ;
	    this.deltay = null ;
	    if(mouseEvent != null) this.update(mouseEvent) ;
	  },
	
	  update : function(mouseEvent) {
	    browser = base.Browser;
	    
	    mouseEvent = $.event.fix(mouseEvent);
	    this.mousexInPage = mouseEvent.pageX;
	    this.mouseyInPage = mouseEvent.pageY;
	
	    var x  =  mouseEvent.clientX;
	    var y  =  mouseEvent.clientY;
	
	    this.lastMousexInClient =  this.mousexInClient != null ? this.mousexInClient : x ;
	    this.lastMouseyInClient =  this.mouseyInClient != null ? this.mouseyInClient : y ;
	
	    this.mousexInClient = x ;
	    this.mouseyInClient = y ;
	
	    this.deltax = this.mousexInClient - this.lastMousexInClient ;
	    this.deltay = this.mouseyInClient - this.lastMouseyInClient ;
	  }
	};
	
	return eXo.webui.UIRightClickPopupMenu;
})($, base, uiPopup);