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

(function () {
  eXo.core.Skin = {
    /**
     * Adds a css file, idnetified by url, to the page
     * componentId identifies the component to which the style applies
     */
    addSkin: function (componentId, url, replaceIfExist) {
      var link = document.getElementById(componentId);
      if (link != null) {
        if (replaceIfExist) {
          link.setAttribute('href', url);
        }
        return;
      }

      link = document.createElement('link');
      link.setAttribute('id', componentId);
      link.setAttribute('rel', 'stylesheet');
      link.setAttribute('type', 'text/css');
      link.setAttribute('href', url);
      var head = document.getElementsByTagName("head")[0];
      var customModule = document.getElementById("customModule");
      if (customModule) {
        head.insertBefore(link, customModule);
      } else {
        head.appendChild(link);
      }
    }
  };
  return eXo.core.Skin;
})();