define([], function(){

  /**
  * AbstractFuzzyDateInput is an object representing a text input for the @class FuzzyDatePicker
  *
  * @class AbstractFuzzyDateInput
  * @constructor
  * @param {element} A text input element. The behaviour of the element will be modified.
  * @param {alwayEnabled} Booolean indicating whether this input should be enabled at all times
  */
  var AbstractFuzzyDateInput = Class.create({
    initialize : function(element, alwaysEnabled){
      this.element = element;
      this.alwaysEnabled = alwaysEnabled;

      //The isFuzzy boolean specifies whether the value contained in the iput is specific or fuzzy(a range, decade etc.). Blank and invalid values are not fuzzy.
      this.isFuzzy = false;
      this.isValid = true;

      if (this.alwaysEnabled) {
        this.enable();
      } else {
        this.disable();
      }
      this.element.onkeypress = this.onKeyPress.bind(this);
      this.element.observe("focus", this.onFocus.bind(this));
      this.element.observe("blur", this.onBlur.bind(this));
      this.element.observe("keydown", this.onKeyDown.bind(this));
    },
    getValue : function() {
      return this.element.value;
    },
    isEmpty : function() {
      return this.getValue() === "";
    },
    setValue : function (value) {
      value && this.enable();
      var newVal = this.formatValue(value);
      this.element.setValue(newVal);
    },
    getElement : function() {
      return this.element;
    },
    onDateChange : function() {
      this.refreshAvailableValues();
      this.validate();
      this.element.toggleClassName("invalid", (!this.isValid && this.getValue() !== ""));
    },
    enable: function() {
      this.refreshAvailableValues();
      this.element.enable();
    },
    disable: function() {
      this.element.setValue("");
      this.element.removeClassName("invalid");
      this.isValid = true;
      this.element.disable();
    },
    onFocus : function(e) {
      this.element.removeClassName("invalid");
    },
    onBlur : function(e) {
      this.onValueSelected(e);
    },
    onValueSelected : function(e) {
      this.element.fire("datepicker:date:changed");
    },
    onKeyDown : function(e) {
      var charCode = e.key || e.which;
      if (charCode == 9) {
        this.onValueSelected(e);
      }
    },
    onKeyPress : function(e, regexp, maxLength, padZeros) {
      var charCode = e.charCode || e.keyCode;
      var character = String.fromCharCode(charCode);
      //Call select handler on tab press.
      if(charCode == 9){
        this.onValueSelected();
        return true;
      }
      //Allow non-character keys always
      if (character === "") {return true;}
      //Block non-digits
      if (!regexp.test(character)) {return false;}

      var currentVal = this.getValue();
      var caretPos = this.getCaretPosition();

      currentVal = currentVal.replace(this.getSelectedText(), "");
      if (currentVal.length == maxLength){return false;}

      if (padZeros && character != "0"){
        currentVal = (currentVal || "0") + character; //zero is prepended to character to allow for "2" as valid input
      } else {
        currentVal = currentVal.substr(0, caretPos) + character + currentVal.substr(caretPos, currentVal.length);
      }

      return this.availableValues.some(function(a){return a.indexOf(currentVal) == 0});
    },
    //Returns the currently selected text.
    getSelectedText : function() {
      var text = "";
      if (typeof window.getSelection != "undefined") {
        text = window.getSelection().toString();
      } else if (typeof document.selection != "undefined" && document.selection.type == "Text") {
        text = document.selection.createRange().text;
      }
      return text;
    },
    getCaretPosition: function() {
      if (this.element.createTextRange) {
        var range = document.selection.createRange().duplicate()
        range.moveEnd('character', this.element.value.length)
        if (range.text == '') return this.element.value.length
        return this.element.value.lastIndexOf(range.text)
      } else return this.element.selectionStart;
    },
    validate: function() {
      this.isValid = this.availableValues.indexOf(this.getValue()) > -1 || this.getValue() === "";
      return this.isValid;
    },
    formatValue : function(value) {return value;},
    refreshAvailableValues : function() {}
  });

  /**
  * FuzzyYearInput is the text input year component of the @class FuzzyDatePicker .
  * Note that the year input will always be enabled.
  *
  * @class FuzzyYearInput
  * @Extends AbstractFuzzyDateInput
  * @constructor
  * @param {element} A text input element. The behaviour of the element will be modified.
  * @param {decadesEnabled} Booolean indicating whether deacades are supported.
  */
  var FuzzyYearInput = Class.create(AbstractFuzzyDateInput,{
    initialize : function($super, element, decadesEnabled) {
      $super(element, true);
      this.decadesEnabled = decadesEnabled || false;
    },
    onKeyPress : function($super, e) {
      if (this.decadesEnabled) {
        return $super(e, /[0-9]|s/, 5, false);
      } else {
        return $super(e, /[0-9]/, 4, false)
      }
    },
    refreshAvailableValues : function() {
      var availableValues = [];
      for (var i = (new Date()).getYear() + 1900; i >= 1800; i--){
        availableValues.push(i + "");
        if (this.decadesEnabled && i % 10 == 0){
          availableValues.push(i + "s");
        }
      }
      this.availableValues = availableValues;
      return availableValues;
    },
    validate : function($super) {
      $super();
      if ( !this.isValid || (this.decadesEnabled && /\d\d\d\ds/.test(this.getValue())) ) {
        this.isFuzzy = true;
      } else {
        this.isFuzzy = false;
      }
      return this.isValid;
    }
  });

  /**
  * FuzzyMonthInput is the text input month component of the @class FuzzyDatePicker
  *
  * @class FuzzyMonthInput
  * @Extends AbstractFuzzyDateInput
  * @constructor
  * @param {element} A text input element. The behaviour of the element will be modified.
  * @param {alwaysEnabled}  Booolean indicating whether the month value should always be enabled. If false
  *                         the value will be enabled iff the year input is valid and not fuzzy.
  * @param {yearInput} The @class FuzzyYearInput to which this month input is associated with.
  */
  var FuzzyMonthInput = Class.create(AbstractFuzzyDateInput, {
    initialize : function($super, element, alwaysEnabled, yearInput) {
      this.yearInput = yearInput;
      $super(element, alwaysEnabled);
    },
    onDateChange : function($super) {
      $super();
      if (this.yearInput.isFuzzy) {
        this.disable();
      } else if (this.alwaysEnabled) {
        this.enable();
      } else if (this.yearInput.isEmpty()) {
        this.disable();
      } else if (this.yearInput.isValid) {
        this.enable();
      } else {
        this.disable();
      }
    },
    onValueSelected: function ($super) {
      var value = this.getValue();
      if (value.length == 1) {
        this.element.setValue("0" + value);
      }
      $super();
    },
    onKeyPress : function($super, e) {
      return $super(e, /[0-9]/, 2, true);
    },

    refreshAvailableValues : function() {
      var availableValues = [];
      for (var i = 1; i <= 12; i++) {
        availableValues.push(("00" + i).slice(-2));
      }
      this.availableValues = availableValues;
      return availableValues;
    },
    formatValue : function(value) {
      if (typeof value == 'number') {
        value = value.toString();
      }
      if (value.length == 1) {
        value = "0" + value;
      }
      return value;
    }
  });

  /**
  * FuzzyDayInput is the text input day component of the @class FuzzyDatePicker
  *
  * @class FuzzyDayInput
  * @Extends AbstractFuzzyDateInput
  * @constructor
  * @param {element} A text input element. The behaviour of the element will be modified.
  * @param {alwaysEnabled}  Booolean indicating whether the day value should always be enabled. If false
  *                         the value will be enabled iff the year input is valid and not fuzzy.
  * @param {yearInput} The @class FuzzyYearInput to which this day input is associated with.
  * @param {monthInput} The @class FuzzyMonthInput to which this day input is associated with.
  */
  var FuzzyDayInput = Class.create(AbstractFuzzyDateInput, {
    initialize : function($super, element, alwaysEnabled, yearInput, monthInput) {
      this.yearInput = yearInput;
      this.monthInput = monthInput;
      $super(element, alwaysEnabled);
    },

    onKeyPress : function($super, e) {
      return $super(e, /[0-9]/, 2, true);
    },

    onDateChange : function($super) {
      $super();
      if (this.yearInput.isFuzzy || this.monthInput.isFuzzy) {
        this.disable();
      } else if (this.alwaysEnabled) {
        this.enable();
      } else if (this.yearInput.isEmpty() || this.monthInput.isEmpty()) {
        this.disable();
      } else if (this.yearInput.isValid && this.monthInput.isValid) {
        this.enable();
      } else {
        this.disable();
      }
    },
    onValueSelected: function ($super) {
      var value = this.getValue();
      if (value.length == 1) {
        this.element.setValue("0" + value);
      }
      $super();
    },
    refreshAvailableValues : function() {
      var year = this.yearInput.getValue() || 1;
      var month = this.monthInput.getValue() || 1;
      var lastDayOfMonth = 0;
      if (["01","03","05","07","08","10","12"].indexOf(month) >= 0) {
        lastDayOfMonth = 31;
      } else if (["04","06","09","11"].indexOf(month) >= 0) {
        lastDayOfMonth = 30
      } else if (month === "02") {
        if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) {
          lastDayOfMonth = 29;
        } else {
          lastDayOfMonth = 28;
        }
      }
      var availableValues = [];
      for (var i = 1; i<= lastDayOfMonth; i++) {
        availableValues.push(("00" + i).slice(-2));
      }
      this.availableValues = availableValues;
      return availableValues;
     },
     formatValue : function(value) {
      if (typeof value == 'number') {
        value = value.toString();
      }
      if (value.length == 1) {
        value = "0" + value;
      }
      return value;
    }
  });

  /**
  * FuzzyDatePicker A javascript component for turning a text input into a fuzzy date picker.
  *
  * @class FuzzyDatePicker
  * @constructor
  * @param {input} A text input element. Date values will be read and written to this element.
  * @param {options}  An optional map of options:
  *         inputFormat: any permutation or combination of "YMD". Defaults to YMD,
  *         alwaysEnabled: Set to true to disable dynamic input enabling. Defaults to false,
  *         decadesEnabled: Toggle support for decades. Defaults to false
  *
  * @Usage
  *         var element = new Element('text', {'name':'fuzzy-date-picker'});
  *         var options = {inputFormat : "MY", decadesEnabled : true};
  *         var fuzzyDatePicker = new FuzzyDatePicker(element, options);
  *
  * @Events
  *         datepicker:date:changed -> Fired. Occures when the user changes the date. Causes an xwiki:date:changed event to be fired as well.
  *         datepicker:date:updated -> Listened. The fuzzy date picker will update its displayed value from its text element on this event.
  */
  var FuzzyDatePicker = Class.create({
    initialize: function(input, options) {
      this.options = options || {};
      this.inputFormat = options.inputFormat || "YMD";
      this.alwaysEnabled = options.alwaysEnabled || this.inputFormat != "YMD";
      this.decadesEnabled = options.decadesEnabled || false;

      if (!input) {return};
      this._input = input;
      this._input.hide();

      this.initStrings();

      this.container = new Element('div', {'class' : 'fuzzy-date-picker'});
      this.container.observe("datepicker:date:changed", this.onDateChange.bind(this))
      this.container.observe("datepicker:date:updated", this.updateDateFromInput.bind(this));
      this._input.wrap(this.container);
      this.createInputs();
      this.insertInputs();
    },
    createInputs : function() {
      var yearElement = new Element('input', {'type': 'text', 'class' : 'text', 'name': 'year', 'placeholder': this.strings.yearPlaceholder});
      var monthElement = new Element('input', {'type': 'text', 'class' : 'text', 'name': 'month', 'placeholder': this.strings.monthPlaceholder});
      var dayElement = new Element('input', {'type': 'text', 'class' : 'text', 'name': 'day', 'placeholder': this.strings.dayPlaceholder});
      this.inputs = {};
      this.inputs.y = new FuzzyYearInput(yearElement, this.decadesEnabled);
      this.inputs.m = new FuzzyMonthInput(monthElement, this.alwaysEnabled, this.inputs.y);
      this.inputs.d = new FuzzyDayInput(dayElement, this.alwaysEnabled, this.inputs.y, this.inputs.m);

    },
    insertInputs : function(){
      this.inputFormat.toLocaleLowerCase().split("").forEach(function(identifier){
        this.container.insert(this.inputs[identifier].getElement());
      }.bind(this));
    },
    updateDateFromInput : function() {
      var dateObject = JSON.parse(this._input.value);
      if (dateObject["year"] != null) {
        this.inputs.y.setValue(dateObject["year"]);
      } else if (dateObject["decade"] != null) {
        this.inputs.y.setValue(dateObject["decade"]);
      }

      dateObject["month"] != null && this.inputs.m.setValue(dateObject["month"]);
      dateObject["day"] != null && this.inputs.d.setValue(dateObject["day"]);
      this.container.fire("datepicker:date:changed");
    },
    onDateChange : function(e) {
      var validDate = true;
      for (input in this.inputs) {
        if (this.inputs.hasOwnProperty(input)){
          this.inputs[input].onDateChange();
          validDate = validDate && this.inputs[input].isValid;
        }
      }
      if (validDate) {
        this.updateDate(false);
      }
    },
    updateDate : function (doNotFireEventOnChange) {
        var dateObject = {};

        var y = this.inputs.y.getValue();
        if (y.match(/\d\d\d\ds$/)) {
          dateObject["decade"] = y;
        } else if (y != "") {
          dateObject["year"] = y;
        }

        if (y > 0 || this.inputFormat == "DMY") {
          var m = this.inputs.m.getValue();
          if (m > 0) {
            dateObject["month"] = m;
          }

          if (m > 0 || this.inputFormat == "DMY") {
            var d = this.inputs.d.getValue();
            if (d > 0) {
              dateObject["day"] = d;
            }
          }
        }

        var newValue = JSON.stringify(dateObject);
        if (newValue != this._input.value) {
          this._input.value = JSON.stringify(dateObject);
          if (!doNotFireEventOnChange) {
            this._input.fire("xwiki:date:changed");
          }
        }
    },
    initStrings: function() {
      this.strings = {
        dayPlaceholder: 'DD',
        monthPlaceholder: 'MM',
        yearPlaceholder: this.decadesEnabled ? 'YYYYs' : 'YYYY'
      };
    }
  });

  return FuzzyDatePicker;
});