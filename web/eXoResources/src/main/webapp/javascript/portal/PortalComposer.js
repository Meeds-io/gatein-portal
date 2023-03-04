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

(function($, portalControl, portalDragDrop) {
	var portalComposer = {

	  init : function(id, width, height, isEditted, portalMode)
	  {
		eXo.portal.portalMode = portalMode;
		eXo.portal.hasEditted = isEditted;
    $("div#" + id).attr("exo:minWidth", width).attr("exo:minHeight", height).find(".uiIconArrowRightMini, .uiIconArrowDownMini").on("click", function()
	    {
	      portalComposer.toggle($(this));
	    });
    	$("div#" + id).find('[rel="tooltip"]').tooltip();
	  },

	  initComposerContent : function(id, selTabId)
	  {
		  portalComposer.showTab(selTabId);

			var tabs = $("#" + id).find(".MiddleTab, .nav-tabs a");
			tabs.each(function(index) {
			  $(this).on("click", function() {
                  			  var jTab = $(this), hiddenInput;
                  			  if (jTab.closest(".nav-tabs").length) {
                  				  jTab.tab('show');
                  				  hiddenInput = $(this).next("input");
                  			  } else {
                  				  hiddenInput = $(this).children("input");
                  			  }
				  portalComposer.showTab(hiddenInput.attr("name"));
				  $.globalEval(hiddenInput.attr("value"));

				  if(eXo.portal.portalMode) eXo.portal.portalMode += (index==0 ? -1 : 1)*2;
			  });
		  });
	  },

	  toggle : function(icon)
	  {
	    var compWindow = icon.closest(".uiPortalComposer");
	    var contWindow = compWindow.children(".popupContent").eq(0);
	    if(contWindow.css("display") == "block")
	    {
          contWindow.hide();
          contWindow.next(".uiAction").hide();
          compWindow.children("span:first").removeClass('uiIconResize');
          icon.attr("class", "uiIconArrowRightMini pull-left");
	    }
	    else
	    {
          contWindow.show();
          contWindow.next(".uiAction").show();
          compWindow.children("span:first").addClass('uiIconResize');
          icon.attr("class", "uiIconArrowDownMini pull-left");
	    }

	    ajaxAsyncGetRequest(eXo.env.server.createPortalURL(compWindow.attr("id"), "Toggle", true));
	  },

	  showTab : function(id)
	  {
	    var toolPanel = $("#UIPortalToolPanel");
	    if(id == "UIApplicationList")
	    {
	      toolPanel.attr("class", "ApplicationMode");
	      portalDragDrop.init(['UIPageBody']);
	    }
	    else if(id == "UIContainerList")
	    {
	      toolPanel.attr("class", "ContainerMode");
	      $("#UIPageBody").find(".DragControlArea, .uiIconDragDrop").off("mousedown");
	    }
	  },

	  /**
	   * Invoked when content is modified (comparing to persisted one)
	   *
	   * The method toggles the floppy-disk icon to 'THERE IS SOME NEW STUFF' status.
	   */
	  toggleSaveButton : function()
	  {
	    //Avoid execute method body multiple times
	    if(!eXo.portal.hasEditted)
	    {
	      eXo.portal.hasEditted = true;
	      var compWindow = $("#UIWorkingWorkspace").find(".uiPortalComposer").eq(0);
	      compWindow.find(".uiIconSave.uiIconLightGray").removeClass("uiIconLightGray").addClass("uiIconDarkGray");

	      ajaxAsyncGetRequest(eXo.env.server.createPortalURL(compWindow.attr("id"), "ChangeEdittedState", true));
	    }
	  }
	};

	return portalComposer;
})($, portalControl, portalDragDrop);