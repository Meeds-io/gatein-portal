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
(function(Backbone, _) {	
    var Todo = Backbone.Model.extend({
    	defaults : {
    		'job' : '',
    		'completed' : false,
    		'editing' : false,
    		'display' : true
    	},		
    	
    	/** @expose */
    	toggle : function(options) {			
    		var opt = {'completed' : !this.get('completed')};
    		_.extend(opt, options);
    		this.save(opt);
    	},		
    	
    	finishEdit : function(job) {
    		this.save({'job' : job, 'editing' : false});
    	},
    	
    	tryEdit : function() {
    		this.save({'editing' : true});
    	},		
    	
    	/** @expose */
    	setDisplay : function(display) {
    		this.set({'display' : display});
    	}
    });
    
    var TodoList = Backbone.Collection.extend({
    
    	model : Todo,
    
    	localStorage : new Backbone.LocalStorage('todoPortlet'),
    
    	initialize : function() {
    		this.on('add change:completed', function(todo) {
    			if (this.filterParam === 'active' && todo.get('completed') || 
    					this.filterParam === 'completed' && !todo.get('completed')) {
    				todo.setDisplay(false);					
    			}
    		}, this);
    	},
    	
    	addTodo : function(job) {
    		return this.create({'job' : job});
    	},
    
    	completed : function() {
    		return this.filter(function(todo) {
    			return todo.get('completed');
    		});
    	},
    
    	active : function() {
    		return this.without.apply(this, this.completed());
    	},
    	
    	/** @expose */
    	filterTodo : function(param) {
    		if (param === 'active') {
    			_.invoke(this.completed(), 'setDisplay', false);
    			_.invoke(this.active(), 'setDisplay', true);
    		} else if (param === 'completed') {
    			_.invoke(this.active(), 'setDisplay', false);
    			_.invoke(this.completed(), 'setDisplay', true);
    		} else {
    			_.invoke(this.models, 'setDisplay', true);
    			param = '';
    		}
    		this.filterParam = param;
    		this.trigger("filter");
    	},
    	
    	clearCompleted : function() {
    		_.invoke(this.completed(), 'destroy');
    	},
    	
    	toggleAll : function(completed) {
    		_.invoke(this.models, 'toggle', {'completed' : completed});
    	}
    });
    
    return {'Todo' : Todo, 'TodoList' : TodoList};
})(Backbone, _);