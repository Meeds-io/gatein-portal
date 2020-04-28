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
require(['jquery', 'mustache', 'text!/amd-js/requirejs/jsp/hello.mustache'], function($, mustache, template) {									
	
	$("body").on("click", ".requirejs-example button", function() {
		var portlet = $(this).closest(".requirejs-example"); 
		
		var name = portlet.find(".name").val();
		name = name == "" ? "world" : name;
		
		var output = mustache.render(template, {"name": name});
		portlet.find(".result").html(output);
	});
	
	$('.requirejs-example pre.code').highlight({source:1, zebra:1, indent:'space', list:'ol'});
	$("body").on("click", ".requirejs-example .nav-tabs li", function() {
		var jLi = $(this);
		var portlet = jLi.closest(".requirejs-example");
		
		portlet.find(".active").removeClass("active");
		jLi.addClass("active");
		
		var contentId = jLi.find("a").attr("class");
		
		portlet.find(".fade.in.active").removeClass("fade in active").addClass("fade");
		portlet.find("#" + contentId).removeClass("fade").addClass("fade in active");		
	});		
});	