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
(function(Backbone, _, $) {
    var TodoView = Backbone.View.extend({
    
        tagName : "li",
    
        template : _.template($('.TodoPortlet > .ItemTmpl').html()),
    
        events : {
            "click .toggle" : "toggle",
            "dblclick .view" : "edit",
            "click .destroy" : "removeTodo",
            "keypress .Edit" : "finish",
            "blur .Edit" : "cancel"
        },
    
        initialize : function() {
    	    this.model.on('change', this.render, this);
    	    this.model.on('destroy', this.remove, this);
        },
    
        render : function() {
    	    this.$el.html(this.template(this.model.toJSON()));
    	    return this;
        },
    
        /** @expose */
        toggle : function() {
    	    this.model.toggle();
        },
    
        /** @expose */
        edit : function() {
    	    this.model.tryEdit();
    	    this.$('.Edit').focus();
        },
    
        /** @expose */
        cancel : function() {
    	    this.model.finishEdit(this.model.get('job'));
        },
    
        /** @expose */
        finish : function(e) {
    	    if (e.keyCode == 13) {
    		    var job = this.$('.Edit').val();
    		    if (!job) {
    			    this.removeTodo();
    		    } else {
    			    this.model.finishEdit(job);
    		    }
    	    } else if (e.keyCode === 27) {
    		    this.cancel();
    	    }
        },
    
        /** @expose */
        removeTodo : function() {
    	    this.model.destroy();
        }
    });
    
    var AppView = Backbone.View.extend({
        statsTemplate : _.template($('.StatsTmpl').html()),
    
        events : {
            "keypress .NewTodo" : "createTodo",
            "click .ClearCompleted" : "clearCompleted",
            "click .ToggleAll" : "toggleAll"
        },
    
        initialize : function() {
            this.input = this.$(".NewTodo");
            this.allCheckbox = this.$(".ToggleAll");
    
            this.model.on('add', this.renderTodo, this);
            this.model.on('add remove change:completed filter',
                    this.renderDecorator, this);
    
            _.each(this.model.models, this.renderTodo, this);
        },
    
        renderDecorator : function() {
            var completed = this.model.completed().length;
            var active = this.model.active().length;
    
            this.$('.Footer').html(this.statsTemplate({
                'active' : active,
                'completed' : completed,
                'filter' : this.model.filterParam
            }));
    
            this.allCheckbox.attr('checked', active == 0
                    && this.model.length != 0);
        },
    
        renderTodo : function(todo) {
            var view = new TodoView({
                model : todo
            });
            this.$('.TodoList').append(view.render().el);
        },
    
        /** @expose */
        createTodo : function(e) {
            if (e.keyCode != 13)
                return;
            if (!this.input.val())
                return;
    
            this.model.addTodo(this.input.val());
            this.input.val('');
        },
    
        /** @expose */
        clearCompleted : function() {
            this.model.clearCompleted();
        },
    
        /** @expose */
        toggleAll : function() {
            var completed = this.allCheckbox.attr("checked");
            this.model.toggleAll(completed === "checked");
        }
    });
    
    return AppView;
})(Backbone, _, $);