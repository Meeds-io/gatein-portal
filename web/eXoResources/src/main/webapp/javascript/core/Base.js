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
	/**
	 * log out of user session
	 */
	eXo.portal.logout = function() {
		// Dispatch logout event
		var logoutEvent = document.createEvent('Event');
		logoutEvent.initEvent('exo-logout', true, true);
		window.dispatchEvent(logoutEvent);
		// Perform logout
		window.location = eXo.env.server.createPortalURL("UIPortal", "Logout", false) ;
	} ;
	
	eXo.session.openUrl = null ;
	eXo.session.itvTime = null ;
	eXo.session.itvObj = null;
	eXo.session.initialized = false;
	
	eXo.session.itvInit = function() {
	   var session = eXo.session, env = eXo.env;
	   if (!session.initialized && session.canKeepState && env.portal.accessMode == 'private') {
	      if (!session.openUrl) session.openUrl = env.server.createPortalURL("UIPortal", "Ping", false) ;
	      if (!session.itvTime) session.itvTime = 1800;
	      session.initialized = true;
	      session.openItv();
	   }
	} ;
	
	eXo.session.startItv = function() {
	   var session = eXo.session;
	   if (session.initialized) {
	      session.destroyItv();
	      if (session.canKeepState && eXo.env.portal.accessMode == 'private') {
	         if (session.itvTime > 0) session.itvObj = window.setTimeout("eXo.session.openItv()", (session.itvTime - 10) * 1000) ;
	      }
	   } else if (session.isOpen) {
	      session.itvInit();
	   }
	} ;
	
	eXo.session.openItv = function() {
		var session = eXo.session;
		var request = window.ActiveXObject ? new ActiveXObject( "Msxml2.XMLHTTP" ) : new XMLHttpRequest();
		request.open("GET", session.openUrl, true);
		request.setRequestHeader("Cache-Control", "max-age=86400");
		request.onreadystatechange = function() {
			if (request.readyState == 4) { 
				if (request.status == 200) {
					var result = request.responseText;
					if(!isNaN(result)) session.itvTime = parseInt(result); 				
				}
				delete request['onreadystatechange'];
			}
		}
		request.send(null);
	} ;
	
	eXo.session.destroyItv = function () {
	   var session = eXo.session;
	   window.clearTimeout(session.itvObj) ;
	   session.itvObj = null ;
	} ;
	
	/**
	 * Generates an id based on the current time and random number
	 */
	eXo.generateId = function(objectId) {
		return (objectId + "-" + new Date().getTime() + Math.random().toString().substring(2)) ;
	};
	
	eXo.debug = function(message) {
		if(!eXo.developing) return;
		
		var webui = eXo.webui;
		if(webui.UINotification) {
			message = "DEBUG: " + message;
			webui.UINotification.addMessage(message);
		}
	};
	
	eXo.core.I18n = {
			  
	  /**
	   * initialize some information as language, orientation, etc of I18n object                       $
	   */
	  init : function() {
	    var html = document.getElementsByTagName('html')[0];
	    var lang = html.getAttribute('xml:lang') || html.getAttribute('lang') || "en";
	    var dir = html.getAttribute('dir') || "lt";
	    this.lang = lang;
	    this.dir = dir;
	    this.orientation = "rtl" == dir ? "rt" : "lt";
	    this.lt = this.orientation == "lt";
	  },
	  
	  /**
	   * return language
	   */
	  getLanguage : function() {
	    return this.lang;
	  },
	  
	  /**
	   * return orientation (right to left, left to right), 
	   * some languages (such as Arabic) used "right to left" view
	   */
	  getOrientation : function() {
	    return this.orientation;
	  },
	  
	  /**
	   * return directory
	   */
	  getDir : function() {
	    return !this.lt;
	  },
	  
	  /**
	   * return "left to"(lt) state
	   */
	  isLT : function() {
	    return this.lt;
	  },
	  
	  /**
	   * return "right to" state
	   */
	  isRT : function() {
	    return !this.lt;
	  }
	};
	eXo.core.I18n.init();
	
	/************************************************************************************/
	/**
	* This function aims is to configure the javascript environment according to the browser in use
	*
	* Common configuration are made first, then we detect the current browser and according to the one 
	* in use, we call delegated methods such as this.initIE() which will add some new configuration or
	* overide the existing ones
	*
	* In all cases the method createHttpRequest is acting as the usual XMLHttpRequest in use in all AJAX
	* calls
	*/
	eXo.core.Browser = {
      onLoadCallback : {},
      onResizeCallback : {},
      onScrollCallback : {},
      title : null,
      titlePrefix : null,
      breakStream : null,
      setTitle : function(title) {
        this.title = title;
        if (this.title == null || typeof this.title == "undefined"){
          this.title = '';
        }
        if (this.titlePrefix == null || typeof this.titlePrefix == "undefined"){
          this.titlePrefix = '';
        }
        document.title = this.titlePrefix + this.title;
      },
      setTitlePrefix : function(titlePrefix) {
        this.titlePrefix = titlePrefix;
        if (this.titlePrefix == null || typeof this.titlePrefix == "undefined"){
          this.titlePrefix = '';
        }
        if (this.title == null || typeof this.title == "undefined"){
          this.title = document.title;
        }
        document.title = this.titlePrefix + this.title;
      },
	  init : function() {
		  $(window).on("resize", this.managerResize);
		  $(window).on("scroll", this.onScroll);
	
		  this.detectBrowser();
	  },
	
	  /**
	    Copyright (c) 2008, Yahoo! Inc. All rights reserved.
	    Code licensed under the BSD License:
	    http://developer.yahoo.net/yui/license.txt
	    version: 2.5.2
	  */
	  detectBrowser : function() {	
		  /**
		   * Internet Explorer version number or 0.  Example: 6
		   * @property ie
		   * @type float
		   */
		  this.ie = 0;
		
		  /**
		   * Opera version number or 0.  Example: 9.2
		   * @property opera
		   * @type float
		   */
		  this.opera = 0;
		
		  /**
		   * Gecko engine revision number.  Will evaluate to 1 if Gecko 
		   * is detected but the revision could not be found. Other browsers
		   * will be 0.  Example: 1.8
		   * <pre>
		   * Firefox 1.0.0.4: 1.7.8   <-- Reports 1.7
		   * Firefox 1.5.0.9: 1.8.0.9 <-- Reports 1.8
		   * Firefox 2.0.0.3: 1.8.1.3 <-- Reports 1.8
		   * Firefox 3 alpha: 1.9a4   <-- Reports 1.9
		   * </pre>
		   * @property gecko
		   * @type float
		   */
		  this.gecko = 0;
		
		  /**
		   * AppleWebKit version.  KHTML browsers that are not WebKit browsers 
		   * will evaluate to 1, other browsers 0.  Example: 418.9.1
		   * <pre>
		   * Safari 1.3.2 (312.6): 312.8.1 <-- Reports 312.8 -- currently the 
		   *                                   latest available for Mac OSX 10.3.
		   * Safari 2.0.2:         416     <-- hasOwnProperty introduced
		   * Safari 2.0.4:         418     <-- preventDefault fixed
		   * Safari 2.0.4 (419.3): 418.9.1 <-- One version of Safari may run
		   *                                   different versions of webkit
		   * Safari 2.0.4 (419.3): 419     <-- Tiger installations that have been
		   *                                   updated, but not updated
		   *                                   to the latest patch.
		   * Webkit 212 nightly:   522+    <-- Safari 3.0 precursor (with native SVG
		   *                                   and many major issues fixed).  
		   * 3.x yahoo.com, flickr:422     <-- Safari 3.x hacks the user agent
		   *                                   string when hitting yahoo.com and 
		   *                                   flickr.com.
		   * Safari 3.0.4 (523.12):523.12  <-- First Tiger release - automatic update
		   *                                   from 2.x via the 10.4.11 OS patch
		   * Webkit nightly 1/2008:525+    <-- Supports DOMContentLoaded event.
		   *                                   yahoo.com user agent hack removed.
		   *                                   
		   * </pre>
		   * http://developer.apple.com/internet/safari/uamatrix.html
		   * @property webkit
		   * @type float
		   */
		  this.webkit = 0;
		
		  /**
		   * The mobile property will be set to a string containing any relevant
		   * user agent information when a modern mobile browser is detected.
		   * Currently limited to Safari on the iPhone/iPod Touch, Nokia N-series
		   * devices with the WebKit-based browser, and Opera Mini.  
		   * @property mobile 
		   * @type string
		   */
		  this.mobile = null;
		
		  /**
		   * Adobe AIR version number or 0.  Only populated if webkit is detected.
		   * Example: 1.0
		   * @property air
		   * @type float
		   */
		  this.air = 0;
		
		
		  var ua=navigator.userAgent, m;
		
		  // Modern KHTML browsers should qualify as Safari X-Grade
		  if ((/KHTML/).test(ua)) {
		      this.webkit=1;
		  }
		  // Modern WebKit browsers are at least X-Grade
		  m=ua.match(/AppleWebKit\/([^\s]*)/);
		  if (m&&m[1]) {
		      this.webkit=parseFloat(m[1]);
		
		      // Mobile browser check
		      if (/ Mobile\//.test(ua)) {
		          this.mobile = "Apple"; // iPhone or iPod Touch
		      } else {
		          m=ua.match(/NokiaN[^\/]*/);
		          if (m) {
		              this.mobile = m[0]; // Nokia N-series, ex: NokiaN95
		          }
		      }
		
		      m=ua.match(/AdobeAIR\/([^\s]*)/);
		      if (m) {
		          this.air = m[0]; // Adobe AIR 1.0 or better
		      }
		
		  }
		
		  if (!this.webkit) { // not webkit
		      // @todo check Opera/8.01 (J2ME/MIDP; Opera Mini/2.0.4509/1316; fi; U; ssr)
		      m=ua.match(/Opera[\s\/]([^\s]*)/);
		      if (m&&m[1]) {
		          this.opera=parseFloat(m[1]);
		          m=ua.match(/Opera Mini[^;]*/);
		          if (m) {
		              this.mobile = m[0]; // ex: Opera Mini/2.0.4509/1316
		          }
		      } else { // not opera or webkit
		          m=ua.match(/MSIE\s([^;]*)/);
		          if (m&&m[1]) {
		              this.ie=parseFloat(m[1]);
		          } else { // not opera, webkit, or ie
		              m=ua.match(/Gecko\/([^\s]*)/);
		              if (m) {
		                  this.gecko=1; // Gecko detected, look for revision
		                  m=ua.match(/rv:([^\s\)]*)/);
		                  if (m&&m[1]) {
		                      this.gecko=parseFloat(m[1]);
		                  }
		              }
		          }
		      }
		  }
		},
		
		managerResize : function() {
		  var browser = eXo.core.Browser;
		  var jWin = $(window);
		  if(browser.currheight != jWin.height() || (browser.currWidth != jWin.width())) {
		    clearTimeout(browser.breakStream) ;
		    browser.breakStream = setTimeout(browser.onResize, 100) ;
		  }
		  browser.currheight = jWin.height();
		}, 	
		
		isIE : function() {
		  var agent = navigator.userAgent ;
		  return (agent.indexOf("MSIE") >=0);
		},		
		
		isIE6 : function() {
		  var agent = navigator.userAgent ;
		  return (agent.indexOf("MSIE 6") >=0);
		},
		
		isIE7 : function() {
		  var agent = navigator.userAgent ;
		  return (agent.indexOf("MSIE 7") >=0);	
		},
		
		isFF : function() {
		  return this.gecko;
		},		
		
		isFF3 : function() {
		  return (navigator.userAgent.indexOf("Firefox/3") >= 0);
		},		
		
		/**
		 * Adds a function to the list of functions to call on load
		 */
		addOnLoadCallback : function(id, method) {
		  this.onLoadCallback[id] = method;
		},
		
		/**
		 * Calls the functions in the onLoadCallback array, if they exist
		 * and clean the array
		 */
		onLoad : function() {
		  var callback = eXo.core.Browser.onLoadCallback;
		  for(var name in callback) {
		    var method = callback[name];
		    if (typeof(method) == "function") try{method()}catch(e){};
		  }
		  this.onLoadCallback = {};
		},
		
		/**
		 * Adds a function to the list of functions to call when the window is resized
		 */
		addOnResizeCallback : function(id, method) {
		  this.onResizeCallback[id] = method;
		},
		
		/**
		 * Calls the functions in the onResizeCallback array, if they exist
		 */
		onResize : function(event) {
		  var callback = eXo.core.Browser.onResizeCallback ;
		 for(var name in callback) {
		   var method = callback[name];
		   if (typeof(method) == "function") method(event) ;
		 }
		},
		
		/**
		 * Adds a function to the list of functions to call when the user scrolls
		 */
		addOnScrollCallback : function(id, method) {
		  this.onScrollCallback[id] = method;
		},
		
		/**
		 * Calls the functions in the onScrollCallback array, if they exist
		 */
		onScroll : function(event) {
		  var callback = eXo.core.Browser.onScrollCallback ;
		  for(var name in callback) {
		    var method = callback[name];
		    try {
		      if (typeof(method) == "function") method(event) ;
		    }catch(err){}
		  }
		},		
		
		/**
		 * Returns the horizontal position of an object relative to the window
		 */
		findPosX : function(obj, isRTL) {
		  var curleft = $(obj).offset().left;
		  // if RTL return right position of obj
		  if(isRTL) return curleft + obj.offsetWidth;
		  return curleft;
		},		
		
		/**
		 * Returns the horizontal position of an object relative to its container
		 */
		findPosXInContainer : function(obj, container, isRTL) {
		  var browser = eXo.core.Browser;
		  var objX = browser.findPosX(obj, isRTL) ;
		  var containerX = browser.findPosX(container, isRTL) ;
		  if(isRTL) return -(objX - containerX) ;
		  return (objX - containerX) ;
		},
		
		/**
		 * Returns the vertical position of an object relative to its container
		 */
		findPosYInContainer : function(obj, container) {
		  var browser = eXo.core.Browser;
		  var objY = $(obj).offset().top;
		  var containerY = $(container).offset().top;
		  return (objY - containerY);
		},		
		
		/**
		 * find the x position of the mouse relative to object
		 */
		findMouseRelativeX : function(object, e, isRTL) {
		  var browser = eXo.core.Browser;
		  var posXObject = browser.findPosX(object,isRTL) ;	  
		  if (!e) e = window.event;
		  e = $.event.fix(e);
		  var mouseX = e.pageX || e.originalEvent.touches[0].pageX;  
		  return mouseX == -1 ? -1 : mouseX - posXObject ;
		},
		
		/**
		 * find the y position of the mouse relative to object
		 */
		findMouseRelativeY : function(object, e) {
		  var browser = eXo.core.Browser;
		  var posYObject = $(object).offset().top;
		  if (!e) e = window.event;
		  e = $.event.fix(e);
		  var mouseY = e.pageY || e.originalEvent.touches[0].pageY;  
		  return  mouseY == -1 ? -1 : mouseY - posYObject ;
		},
		
		/* 
		 * Set Position for a Component in a container
		 */
		setPositionInContainer : function(container, component, posX, posY) {
		  var offsetX = component.offsetLeft;
		  var offsetY = component.offsetTop;
	
		  var browser = eXo.core.Browser;
		  var posXInContainer = browser.findPosXInContainer(component, container);
		  var posYInContainer = browser.findPosYInContainer(component, container);
	
		  var deltaX = posX - (posXInContainer - offsetX);
		  var deltaY = posY - (posYInContainer - offsetY);
	
		  component.style.left = deltaX + "px";
		  component.style.top = deltaY + "px";
		},
		
		/* 
		 * Set Cookie
		 */
		setCookie : function(name,value,expiredays) {
		  var exdate = new Date() ;
		  exdate.setDate(exdate.getDate() + expiredays) ;
		  document.cookie = name + "=" + escape(value) + ((expiredays==null) ? "" : ";expires="+exdate.toGMTString()) ;
		},
		
		/* 
		 * Get Cookie
		 */
		getCookie : function(name) {
		  if (document.cookie.length > 0) {
		    var start = document.cookie.indexOf(name + "=")
		    if (start != -1) {
		      start = start + name.length + 1 ;
		      var end = document.cookie.indexOf(";",start) ;
		      if (end == -1) end = document.cookie.length ;
		        return unescape(document.cookie.substring(start,end)) ;
		    } 
		  }
		  return "" ;
		},
		
		isDesktop : function() {
		  if(document.getElementById("UIPageDesktop")) return true ;
		  return false ;
		},
		
		/**
		 * Return the height of free space in the page if it is available.
		 * Otherwise, returns a negative which is equal to the height of content not visible on the screen.
		 */
		getHeightOfFreeSpace : function() {
		  var elements = document.body.children;
		  var height = 0; 
		  var ln = elements.length ;
		  for(var k = 0; k < ln; k++) {
		    height += elements[k].offsetHeight ;
		  }
		  return ($(window).height() - height);
		},
		
		/**
		 * Adjust height of the element to fill up free space if any
		 */
		fillUpFreeSpace : function(elemt) {
		  if (typeof(elemt) == "string") {
		    elemt = document.getElementById(elemt);
      }
      elemt.style.maxHeight = `${elemt.scrollHeight - 50}px`;
		}
	};
	
	eXo.core.Browser.init();
	return {Browser : eXo.core.Browser, I18n : eXo.core.I18n};
})($);