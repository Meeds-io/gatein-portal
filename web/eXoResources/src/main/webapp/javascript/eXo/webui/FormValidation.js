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
  var RequireValidator = function() {
      this.init = function(input) {
        this.$input = $(input);
      };

      this.validate = function() {
        var $input = this.$input;
        var type = $input.attr('type');
        var valid = false;
        if (type == 'checkbox' || type == 'radio') {
          valid = $input.is(':checked');
        } else {
          valid = $.trim($input.val());
        }

        $input.trigger('validate', valid);
        return valid;
      };
  };

  var validation = {
     validators : {},

     init : function(form) {
       if (!form || $(form).data('validation_initialized')) return;

       var $inputs = getInputs(form);
       $.each($inputs, function(idx, input) {
         var validator = validation.validators[$(input).data('validation')];
         if (validator) {
           var inst = new validator();
           $(input).data('validator', inst);
           inst.init(input);           
         }
       });
       $(form).data('validation_initialized', true);
     },

     validate : function(form) {
       var $inputs = getInputs(form);
       var valid = true;
       
       if ($inputs.length) {
         $.each($inputs, function(idx, input) {
           var $input = $(input);
           var validator = $input.data('validator');
           if (validator && !validator.validate()) {
             valid = false;
           }
         });
         
         $inputs.closest('form').trigger('formValidate', valid);
       }
       return valid;         
     },
     
     addValidator : function(type, validator) {
       this.validators[type] = validator;
     }
  };

  function getInputs(form) {
    var $form = $(form);
    if ($form) {
      return $(form).find('[data-validation]');
    }
    return [];
  }
  
  $.fn.initValidate = function() {
    validation.init(this);
  }
  
  $.fn.validate = function() {
    validation.init(this);
    validation.validate(this);
  }

  //default
  validation.addValidator('require', RequireValidator);
  return validation;
})($);