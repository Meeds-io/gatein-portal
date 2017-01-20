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

(function($) {
	var itemSelector = {

	  init : function(selector, data, clickOnly) {
		  var items = $(selector);
		  if (!clickOnly) {
			  items.on("mouseover", function() {
				  itemSelector.onOver(this, true);
			  });
			  items.on("mouseout", function() {
				  itemSelector.onOver(this, false);
			  });
		  }
		  items.each(function(index) {
			  var itm = $(this);
			  itm.on("click", function() {
				  itemSelector.onClick(this);
				  itm.find(".ExtraActions, .extraActions").each(function() {
					  var act = $(this).html();
					  eval(act);
				  });
				  if (data) {
					 itemSelector.onClickCategory(this, null, data[index].componentName, data[index].categoryName);
				  }
			  });
		  });
	  },

	  /**
	   * Mouse over event, Set highlight to OverItem
	   *
	   * @param {Object}
	   *          selectedElement focused element
	   * @param {boolean}
	   *          mouseOver
	   */
	  onOver : function(selectedElement, mouseOver) {
	    if (selectedElement.className == "Item") {
	      itemSelector.beforeActionHappen(selectedElement);
	    }
	    if (mouseOver) {
	      this.backupClass = selectedElement.className;
	      selectedElement.className = "OverItem Item";
	      // minh.js.exo
	      // this.onChangeItemDetail(selectedElement, true);
	    } else {
	      selectedElement.className = this.backupClass;
	      // this.onChangeItemDetail(selectedElement, false);
	    }
	  },
	  /**
	   * Mouse click event, highlight selected item and non-highlight other items
	   * There are 3 types of item: Item, OverItem, SeletedItem
	   *
	   * @param {Object}
	   *          clickedElement
	   */
	  onClick : function(clickedElement) {
	    var itemListContainer = $(clickedElement).closest(".itemListContainer, .ItemListContainer")[0];
	    var allItems = $(itemListContainer).find("div.Item, .item").get();
	    itemSelector.beforeActionHappen(clickedElement);
	    if (this.allItems.length <= 0)
	      return;
	    for ( var i = 0; i < allItems.length; i++) {
	    	var $item = $(allItems[i]);
	      if ($item[0] != clickedElement) {
	        $item.removeClass("SelectedItem selectedItem");
	        this.onChangeItemDetail(clickedElement, true);
	      } else {	      	
	      	var selected = "SelectedItem";
	      	this.backupClass = "SelectedItem Item";
	      	if ($item.hasClass("item")) {
	      		selected = "selectedItem";
	      		this.backupClass = "selectedItem item"
	      	}
	        $item.hasClass(selected) || $item.addClass(selected);
	        this.onChangeItemDetail(clickedElement, false);
	      }
	    }
	  },
	  /**
	   * Change UI of new selected item, selected item will be displayed and others
	   * will be hidden
	   *
	   * @param {Object}
	   *          itemSelected selected item
	   * @param {boolean}
	   *          mouseOver
	   */
	  onChangeItemDetail : function(itemSelected, mouseOver) {
	    if (!this.allItems || this.allItems.length <= 0)
	      return;
	    if (mouseOver) {
	      for ( var i = 0; i < this.allItems.length; i++) {
	        if (this.allItems[i] == itemSelected) {
	          this.itemDetails[i].style.display = "block";
	        } else {
	          this.itemDetails[i].style.display = "none";
	        }
	      }
	    } else {
	      for ( var i = 0; i < this.allItems.length; i++) {
	        if (this.allItems[i] == itemSelected) {
	          this.itemDetails[i].style.display = "block";
	        } else {
	          this.itemDetails[i].style.display = "none";
	        }
	      }
	    }
	  },

	  /* Pham Thanh Tung added */
	  onClickCategory : function(clickedElement, form, component, option) {
	    itemSelector.onClick(clickedElement);
	    if (itemSelector.SelectedItem == null) {
	      itemSelector.SelectedItem = new Object();
	    }
	    itemSelector.SelectedItem.component = component;
	    itemSelector.SelectedItem.option = option;
	  },

	  /* Pham Thanh Tung added */
	  onClickOption : function(clickedElement, form, component, option) {
	    var selectedItems = $(clickedElement).closest(".itemDetailList").find(".selectedItem").get();
	    for ( var i = 0; i < selectedItems.length; i++) {
	      selectedItems[i].className = "normalItem";
	    }
	    clickedElement.className = "selectedItem";
	    if (itemSelector.SelectedItem == null) {
	      itemSelector.SelectedItem = new Object();
	    }
	    itemSelector.SelectedItem.component = component;
	    itemSelector.SelectedItem.option = option;
	  },

	  /* TODO: Review This Function (Ha's comment) */
	  beforeActionHappen : function(selectedItem) {
	    var jqObj = $(selectedItem);
	    this.uiItemSelector = jqObj.closest(".UIItemSelector, .uiItemSelector")[0];
	    var listCont = jqObj.closest(".ItemListContainer, .itemListContainer");
	    this.itemListContainer = listCont[0];
	    this.itemListAray = listCont.parent().find("div.ItemList, .itemList").get();

	    if (this.itemListAray.length > 1) {
	      this.itemDetailLists = $(this.uiItemSelector).find("div.ItemDetailList, .itemDetailList").get();
	      this.itemDetailList = null;
	      for ( var i = 0; i < this.itemListAray.length; i++) {
	        if (this.itemListAray[i].style.display == "none") {
	          this.itemDetailLists[i].style.display = "none";
	        } else {
	          this.itemDetailList = this.itemDetailLists[i];
	          this.itemDetailList.style.display = "block";
	        }
	      }
	    } else {
	      this.itemDetailList = $(this.uiItemSelector).find("div.ItemDetailList, .itemDetailList")[0];
	    }

	    this.itemDetails = $(this.itemDetailList).find("div.ItemDetail, .itemDetail").get();
	    this.itemList = jqObj.closest(".ItemList, .itemList")[0];
	    this.allItems = $(this.itemList).find("div.Item, .item").get();
	  },

	  showPopupCategory : function(selectedNode) {
	    var itemListCont = $(selectedNode).closest(".ItemListContainer");
	    var popupCategory = itemListCont.find("div.UIPopupCategory").eq(0);

	    itemListCont.css("position", "relative");

	    if(popupCategory.css("display") == "none")
	    {
	      popupCategory.css({"position" : "absolute", "top" : "23px", "left" : "0px", "display" : "block", "width" : "100%"});
	    }
	    else
	    {
	      popupCategory.css("display", "none");
	    }
	  },

	  selectCategory : function(selectedNode) {
	    var jqObj = $(selectedNode);
	    var itemListCont = jqObj.closest(".OverflowContainer");
	    var selectedNodeIndex = itemSelector.findIndex(selectedNode);

	    var itemList = itemListCont.find("div.ItemList");
	    var itemDetailList = itemListCont.find("div.ItemDetailList");

	    itemList.each(function(index)
	    {
	      if (index == selectedNodeIndex)
	      {
	        $(this).css("display", "block");
	        itemDetailList.get(index).style.display = "block";
	      }
	      else
	      {
	        $(this).css("display", "none");
	        itemDetailList.get(index).style.display = "none";
	      }
	    });

	    jqObj.closest(".UIPopupCategory").css("display", "none");
	  },

	  findIndex : function(object) {
	    var siblings = $(object).parent().children("div." + object.className).get();
	    for ( var i = 0; i < siblings.length; i++) {
	      if (siblings[i] == object)
	        return i;
	    }
	  }
	};

	var languageSelector = {
		init : function(selected, selectOptions) {
			var selector = languageSelector;
			var langForm = $(".uiChangeLanguageForm");
            var saveButton = langForm.find(".uiAction").find(".btn").first();
			var href = saveButton.attr("href");
			saveButton.on("click", function() {selector.changeLanguage(href);return false;});

	        selector.SelectedItem = {"component": selected.component, "option" : selected.option};
	        langForm.find(".nodeLabel").closest(".selectedItem, .normalItem").each(function(index) {
	        	var opt = selectOptions[index];
	        	$(this).on("click", function() {
	            	itemSelector.onClickOption(this, null, opt.component, opt.option);
	            });
	        });
		},

		changeLanguage : function(url) {
		   var language = "";
		   if(itemSelector.SelectedItem != undefined) {
			   language = itemSelector.SelectedItem.option;
		   }
		   if(language == undefined) {
			   language = "";
		   }
		   var originalParams = "";
		   var originalUrl = window.location.href;
	           var parametersFirstIndex = originalUrl.indexOf('?');
		   if(parametersFirstIndex > 0) {
		     originalParams = originalUrl.substring(parametersFirstIndex + 1, originalUrl.length);
		     var startSearchIndex = originalParams.indexOf("&language=");
		     if(startSearchIndex > -1) {
		       // Index used to search the original parameters
		       var startIndexOfOriginalParams = originalParams.indexOf("&", startSearchIndex + "&language=".length);
		       if(startIndexOfOriginalParams > -1) {
		         originalParams = originalParams.substring(startIndexOfOriginalParams, originalParams.length);
		       } else {
		         originalParams = "";
		       }
		     }
		     if(originalParams.length > 0 && originalParams.indexOf("&") != 0) {
		       originalParams = "&" + originalParams;
		     }
		   }
		   window.location = url + "&language=" + language + originalParams;
		}
	};

	var skinSelector = {
		init : function() {
			var selector = skinSelector;
			var langForm = $(".UIChangeSkinForm");
            var saveButton = langForm.find(".UIAction, .uiAction").find("a, .btn").first();
			var href = saveButton.attr("href");
			saveButton.on("click", function() {selector.changeSkin(href);return false;});
		},

		changeSkin : function(url) {
		   var skin = "";
		   if(itemSelector.SelectedItem != undefined) {
			   skin = itemSelector.SelectedItem.option;
		   }
		   if(skin == undefined) {
			   skin = "";
		   }
		   window.location = url + "&skin=" + skin;
		}
	};

	var userSelector = {
	  /**
	   * Init information and event for items table
	   *
	   * @param {Object,
	   *          String} cont Object or identifier of Object that contains items
	   *          table
	   */
	  init : function(cont, groupLabel, searchLabel) {
	    if (typeof (cont) == "string")
	      cont = document.getElementById(cont);
	    var jCont = $(cont);
	    var checkboxes = jCont.find("input.checkbox");
	    checkboxes.each(function(index)
	    {
	      if(index == 0)
	      {
	        checkboxes[index].onclick = userSelector.checkAll;
	      }
	      else
	      {
	        checkboxes[index].onclick = userSelector.check;
	      }
	    });
	    
	    jCont.find(".searchByGroup input").attr("placeholder", groupLabel);
	    jCont.find(".searchByUser input").attr("placeholder", searchLabel).each(function() {
	    	$(this).on("keypress", function(event) {
	    		if (userSelector.isEnterPress(event)) {
	    			$.globalEval($(this).closest(".uiSearch").find(".btnSearchUser").attr("href"));
	    			return false;
	    		}
	    	});
	    });
	  },
	  /**
	   * Check or uncheck all items in table
	   */
	  checkAll : function() {
	    userSelector.checkAllItem(this);
	  },
	  /**
	   * Get all item in table list
	   *
	   * @param {Object}
	   *          obj first object of table
	   */
	  getItems : function(obj) {
	    return $(obj).parent().closest("table").find("input.checkbox").get();
	  },
	  /**
	   * Check and uncheck first item
	   */
	  check : function() {
	    userSelector.checkItem(this);
	  },
	  /**
	   * check and uncheck all items in table
	   *
	   * @param {Object}
	   *          obj first object of table, if obj.checked is true, check all item.
	   *          obj.checked is false, uncheck all items
	   */
	  checkAllItem : function(obj) {
	    var checked = obj.checked;
	    var items = userSelector.getItems(obj);
	    var len = items.length;
	    for ( var i = 1; i < len; i++) {
	      items[i].checked = checked;
	    }
	  },
	  /**
	   * Check and uncheck first item, state has dependence on obj state
	   *
	   * @param {Object}
	   *          obj selected object
	   */
	  checkItem : function(obj) {
	    var checkboxes = userSelector.getItems(obj);
	    var len = checkboxes.length;
	    var state = true;
	    if (!obj.checked) {
	      checkboxes[0].checked = false;
	    } else {
	      for ( var i = 1; i < len; i++) {
	        state = state && checkboxes[i].checked;
	      }
	      checkboxes[0].checked = state;
	    }
	  },
	  /**
	   * Get key code of pressed key
	   *
	   * @param {Object}
	   *          event event that user presses a key
	   */
	  getKeynum : function(event) {
	    var keynum = false;
	    if (window.event) { /* IE */
	      keynum = window.event.keyCode;
	      event = window.event;
	    } else if (event.which) { /* Netscape/Firefox/Opera */
	      keynum = event.which;
	    }
	    if (keynum == 0) {
	      keynum = event.keyCode;
	    }
	    return keynum;
	  },

	  /**
	   * Return true if the key pressed is the Enter. Otherwise return false.
	   */
	  isEnterPress : function(evt) {
	    var _e = evt || window.event;
	    var keynum = userSelector.getKeynum(_e);
	    if (keynum == 13) {
	      return true;
	    }
	    return false;
	  },

	  /**
	   * Cancel submit action
	   */
	  cancelSubmit : function() {
	    return false;
	  }
	};

	return {
		UIItemSelector :itemSelector,
		UILanguageSelector : languageSelector,
		UISkinSelector : skinSelector,
		UIUserSelector : userSelector
	};
})($);
