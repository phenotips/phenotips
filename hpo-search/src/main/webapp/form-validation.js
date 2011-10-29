document.observe('dom:loaded', function() {
    // ------------------------------------------------------------------------
    // Live validation of the date of birth
    
    var hasErrors = false;
    
    var dateValidation = function (value) {
      if (!value || !value.match(/^(0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01])[/](19|20)\d\d$/)) {
	return false;
      }
      var parts = value.split("/");
      var month = parseInt(parts[0]);
      var day = parseInt(parts[1]);
      var year = parseInt(parts[2]);
      if (((month == 4 || month == 6 || month == 9 || month == 11) && day > 30) ||
          (month == 2 && (day > 29 || (year % 4 != 0 || year % 100 == 0 && year % 400 != 0) && day > 28))) {
        return false;
      }
      return true;
    }
    
    var markIfHasErrors = function(inputHasErrors, input) {
      if (!input) {
        return false;
      }
      var label = null;
      if (input.id) {
        label = input.previous("label[for=" + input.id + "]");
      }
      if (inputHasErrors) {
        input.addClassName('wrongValue');
        if (label) {
          label.addClassName('wrongValue');
        }
        return true;
      } else {
        input.removeClassName('wrongValue');
        if (label) {
          label.removeClassName('wrongValue');
        }
        return false;
      }
    }
    
    var dateField = $('date_of_birth');
    if (dateField) {
      dateField.observe('blur', function(event){
	markIfHasErrors(!dateValidation(dateField.value), dateField);
      });
    }
    
    // ------------------------------------------------------------------------
    // Prevent form submission if there's missing data

    try {
    $('clinical-info-form').observe('submit', function(event) {
      var form = event.element();
      var hasErrors = false;
      
      var mandatoryInputFields = ['last_name', 'first_name', 'date_of_birth'];
      var mandatoryCheckboxes = {'gender' : form.down('.patient-info .gender label'), 'phenotype' : form.down('.clinical-info legend')};
      
      mandatoryInputFields.each(function(item) {
	var input = form.down("input[name=" + item + "]");
	hasErrors = markIfHasErrors((!input.value || input.value.strip() == ""), input) || hasErrors;
      });
      
      for (var item in mandatoryCheckboxes) {
	var hasValue = false;
	var targetElt = null;
	if (mandatoryCheckboxes[item] && typeof(mandatoryCheckboxes[item].addClassName) == 'function') {
	  targetElt = mandatoryCheckboxes[item];
	}
	form.select("input[name=" + item + "]").each(function(input){
	  if (input.checked) {
	    hasValue = true;
	  }
	});
	if (!hasValue) {
	  hasErrors = true;
	  if (targetElt) {
	    targetElt.addClassName('wrongValue');
	  }
	} else {
	  if (targetElt) {
	    targetElt.removeClassName('wrongValue');
	  }
	}
      }
      
      if (dateField) {
	hasErrors = markIfHasErrors((!dateValidation(dateField.value)), dateField) || hasErrors;
      } else {
	alert('no date');
      }
      
      if (hasErrors) {
	if (!form.down('div.error')) {
	   form.insert({'top' : new Element('div', {'class' : 'error'}).update("Some mandatory fields are missing or have incorrect values.")});
	}
	form.scrollTo();
        event.stop();
      }
    });
    } catch (error) { /* No form? No problem */ }
});