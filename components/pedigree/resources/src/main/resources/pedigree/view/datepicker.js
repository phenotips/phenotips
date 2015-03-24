var PhenoTips = (function (PhenoTips) {
  var widgets = PhenoTips.widgets = PhenoTips.widgets || {};

  widgets.PedigreeFuzzyDatePickerDropdown = Class.create({
    initialize : function(options) {
      this.span     = new Element('span');
      this.options  = options;
      this.callback = null;
    },

    populate : function(values) {
      var selectedIndex = this.dropdown ? (this.dropdown.selectedIndex || this._tmpSelectedIndex) : 0;

      // using raw HTML for performance reasons: generating many years takes a noticeable time using
      // more proper methods (e.g. new Element()...)
      // (Note: using span around select because IE9 does not allow setting innerHTML of <select>-s)
      var optionsHTML = '<select name="' + this.options.name +
                             '" class="' + (this.options.cssClass || this.options.name || '') +
                             '" placeholder="' + (this.options.hint || this.options.name || '') +
                             '" title="'       + (this.options.hint || this.options.name || '') + '">';

      optionsHTML += '<option value="" class="empty"></option>';
      var _this = this;
      values.each(function (item) {
         optionsHTML += '<option value="' + item.value + '"';
         if (item.cssClass) {
             optionsHTML += ' class="' + item.cssClass + '"';
         }
         optionsHTML += '>' + (item.text || item.value || '') + '</option>';
      });
      optionsHTML += "</select>";
      this.span.innerHTML = optionsHTML;
      this.dropdown = this.span.firstChild;
      this.callback && this.onSelect(this.callback);
      if (this.dropdown.selectedIndex <= 0 && selectedIndex >= 0 && selectedIndex < this.dropdown.options.length) {
        this.dropdown.selectedIndex = selectedIndex;
      }
    },

    enable : function () {
      if (!this.options.alwaysEnabled) {
          this.dropdown.enable();
          if (this.dropdown.selectedIndex <= 0 && this._tmpSelectedIndex < this.dropdown.options.length) {
            this.dropdown.selectedIndex = this._tmpSelectedIndex;
            return (this._tmpSelectedIndex > 0);
          }
      }
      return false;
    },

    disable : function () {
      if (!this.options.alwaysEnabled) {
          this.dropdown.disable();
          this._tmpSelectedIndex = this.dropdown.selectedIndex;
          this.dropdown.selectedIndex = 0;
      }
    },

    getElement : function() {
      return this.span;
    },

    onSelect : function(callback) {
      var _this = this;
      this.callback = callback;
      var events = ['change'];
      browser.isGecko && events.push('keyup');
      events.each(function(eventName) {
        _this.dropdown.observe(eventName, function() {
          callback();
          _this._tmpSelectedIndex = _this.dropdown.selectedIndex;
        });
      });
    },

    onFocus : function(callback) {
      var _this = this;
      this.dropdown.observe('focus', function() {
        callback();
        if (_this.dropdown.selectedIndex == -1 && _this._tmpSelectedIndex < _this.dropdown.options.size()) {
          _this.dropdown.selectedIndex = _this._tmpSelectedIndex;
        }
      });
    },
    onBlur : function(callback) {
      this.dropdown.observe('blur', callback);
    },

    getSelectedValue : function () {
       return (this.dropdown.selectedIndex >= 0) ? this.dropdown.options[this.dropdown.selectedIndex].value : '';
    },

    getSelectedOption : function () {
       return (this.dropdown.selectedIndex >= 0) ? this.dropdown.options[this.dropdown.selectedIndex].innerHTML : '';
    }
  });

  widgets.PedigreeFuzzyDatePicker = Class.create({
    initialize : function (input, inputFormat) {
      this.inputFormat = inputFormat ? inputFormat : "YMD";

      if (!input) {return};
      this.__input = input;
      this.__input.hide();

      this.container = new Element('div', {'class' : 'fuzzy-date-picker'});
      this.__input.insert({after : this.container});

      if (this.inputFormat == "DMY") {
          this.container.insert(this.createDayDropdown());
          this.container.insert(this.createMonthDropdown());
          this.container.insert(this.createYearDropdown());
      } else {
          this.container.insert(this.createYearDropdown());
          this.container.insert(this.createMonthDropdown());
          this.container.insert(this.createDayDropdown());
      }

      // TODO: yearSelector's (and month's & day's) .onSelect() does not seem to fire
      //       upon programmatic update if a substitute is found can remove these hackish events
      this.container.observe("datepicker:date:changed", this.onProgrammaticUpdate.bind(this));
    },

    onProgrammaticUpdate : function() {
        this.yearSelected(true);
        this.monthSelected(true);
        this.updateDate(true);
    },

    createYearDropdown : function() {
      //var timer = new Timer();
      this.yearSelector = new widgets.PedigreeFuzzyDatePickerDropdown({name: "year", alwaysEnabled: (this.inputFormat == "DMY")});

      var today = new Date();
      var crtYear = today.getYear() + 1900;
      var startYear = 1900;

      var values = [];
      for (var y = crtYear; y >= startYear; --y) {
        values.push({"value" : y});
        if (y % 10 == 0) {
            values.push({"value" : (y + "s"), "cssClass" : "decade", "text" : (y + 's')});
        }
      }
      values.push({"value": "1800s", "cssClass" : "decade"});
      values.push({"value": "1700s", "cssClass" : "decade"});
      values.push({"value": "1600s", "cssClass" : "decade"});

      this.yearSelector.populate(values);
      this.yearSelector.onSelect(this.yearSelected.bind(this));

      //console.log( "=== Generate year dropdown time: " + timer.report() + "ms ==========" );
      return this.yearSelector.getElement();
    },

    yearSelected : function(doNotNotifyOnChange) {
      if (this.yearSelector.getSelectedValue() > 0) {
        this.monthSelector.enable();
        this.monthSelected(doNotNotifyOnChange);
      } else {
        this.monthSelector.disable();
        this.daySelector.disable();
      }
      this.updateDate(doNotNotifyOnChange);
    },

    createMonthDropdown : function() {
      this.monthSelector = new widgets.PedigreeFuzzyDatePickerDropdown({name: "month", alwaysEnabled: (this.inputFormat == "DMY")});
      this.monthSelector.populate(this.getZeroPaddedValueRange(1,12));
      this.monthSelector.disable();
      this.monthSelector.onSelect(this.monthSelected.bind(this));
      return this.monthSelector.getElement();
    },

    monthSelected : function(doNotNotifyOnChange) {
      if (this.monthSelector.getSelectedValue() > 0) {
        this.daySelector.populate(this.getAvailableDays());
        this.daySelector.enable();
      } else {
        if (this.inputFormat == "DMY") {
            // in "DMY" mode let user pick any day if no month is selected
            this.daySelector.populate(this.getZeroPaddedValueRange(1,31));
        } else {
            this.daySelector.disable();
        }
      }
      this.updateDate(doNotNotifyOnChange);
    },

    createDayDropdown : function() {
      this.daySelector = new widgets.PedigreeFuzzyDatePickerDropdown({name: "day", alwaysEnabled: (this.inputFormat == "DMY")});
      this.daySelector.populate(this.getZeroPaddedValueRange(1,31));
      this.daySelector.disable();
      this.daySelector.onSelect(this.updateDate.bind(this));
      return this.daySelector.getElement();
    },

    getAvailableDays : function () {
      var year = this.yearSelector.getSelectedValue() * 1;
      var month = this.monthSelector.getSelectedValue() * 1;
      var lastDayOfMonth = 0;
      if ([1,3,5,7,8,10,12].indexOf(month) >= 0) {
        lastDayOfMonth = 31;
      } else if ([4,6,9,11].indexOf(month) >= 0) {
        lastDayOfMonth = 30
      } else if (month == 2) {
        if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) {
          lastDayOfMonth = 29;
        } else {
          lastDayOfMonth = 28;
        }
      }
      return this.getZeroPaddedValueRange(1, lastDayOfMonth);
    },

    getZeroPaddedValueRange : function (start, end) {
      var values = [];
      if (start <= end) {
          for (var v = start; v <= end; ++v) {
              values.push({'value': v, 'text' : ("0" + v).slice(-2)});
          }
      } else {
          for (var v = end; v <= start; --v) {
              values.push({'value': v, 'text' : ("0" + v).slice(-2)});
          }
      }
      return values;
    },

    updateDate : function (doNotFireEventOnChange) {
        var dateObject = {};

        var y = this.yearSelector.getSelectedValue();
        if (y.match(/\d\d\d\ds$/)) {
            dateObject["decade"] = y;
        } else {
            if (y != "") {
                dateObject["year"] = y;
            }
        }

        if (y > 0 || this.inputFormat == "DMY") {
            var m = this.monthSelector.getSelectedValue();
            if (m > 0) {
                dateObject["month"] = this.monthSelector.getSelectedOption();
            }

            if (m > 0 || this.inputFormat == "DMY") {
                var d = this.daySelector.getSelectedValue();
                if (d > 0) {
                    dateObject["day"] = this.daySelector.getSelectedOption();
                }
            }
        }

        var newValue = JSON.stringify(dateObject);
        if (newValue != this.__input.value) {
            this.__input.value = JSON.stringify(dateObject);
            if (!doNotFireEventOnChange) {
                this.__input.fire("xwiki:date:changed");
            }
        }
    }
  });

  // End augmentation.

  return PhenoTips;
}(PhenoTips || {}));