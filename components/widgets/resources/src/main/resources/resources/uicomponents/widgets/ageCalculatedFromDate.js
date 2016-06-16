var PhenoTips = (function(PhenoTips) {
  // Start PhenoTips augmentation
  var widgets = PhenoTips.widgets = PhenoTips.widgets || {};

  widgets.AgeCalculatedFromDate = Class.create({
    initialize: function(el) {
      // DOM element pointers
      this.el = el; // Assumed to be a duration widget field
      this.linkedDateEl = $(el.readAttribute("data-linked-date"));
      if (!this.linkedDateEl) {
        console && console.error("Linked date element not found");
      }
      this._patientDobEl = $(document.documentElement).down('input[name$=date_of_birth]');

      // Bind methods
      this._handleDateChange = this._handleDateChange.bind(this);

      // Attach handlers
      this._patientDobEl.observe('xwiki:date:changed', this._handleDateChange);
      this.linkedDateEl.observe('xwiki:date:changed', this._handleDateChange);
      this.linkedDateEl.observe('datepicker:init', this._handleDateChange);

      // If the datepicker has already been initialized, set the initial state
      if (this.linkedDateEl.__datePicker) {
        this._handleDateChange();
      }

      // Save in the DOM
      this.el.__ageCalculatedFromDate = this;
    },

    _handleDateChange: function() {
      if (this.linkedDateEl.alt.length && 
          !this.linkedDateEl.hasClassName('LV_invalid_field') &&
          this._patientDobEl.alt.length) {
        this.el.readOnly = true;
        this.el.value = this.getCalculatedAge();
        this.el.fire("duration:change");
      } else if (this.el.readOnly) {
        this.el.readOnly = false;
        this.el.value = '';
        this.el.fire("duration:change");
      }
    },

    getCalculatedAge: function() {
      var cls = widgets.AgeCalculatedFromDate;

      var thisDateExact = cls.getExactDate(this.linkedDateEl);
      var dobExact = cls.getExactDate(this._patientDobEl);
      // We can't do anything unless there is some date set on both
      // Fuzzy date picker behaviour is to have an exact date set even when not
      // all date components are present.
      // This could also be adapted to support other datepickers, but currently
      // only supports fuzzy date picker.
      if (thisDateExact && dobExact) {
        var dobParts = cls.getEnteredDateComponents(this._patientDobEl);
        var thisDateParts = cls.getEnteredDateComponents(this.linkedDateEl);
        if (!cls.areComponentsFullDate(dobParts) || !cls.areComponentsFullDate(thisDateParts)) {
          // We'll need to compare components
          return cls.getCalculatedAgeFromPartialDateAndDob(dobParts, thisDateParts);
        } else {
          // We can use the exact dates directly
          return cls.getCalculatedAgeFromExactDateAndDob(dobExact, thisDateExact);
        }
      } else {
        return undefined;
      }

      return ageStr;
    },
  });

  // Class (non-instance) methods
  Object.extend(widgets.AgeCalculatedFromDate, {
    getEnteredDateComponents: function(el) {
      return (el && el.__datePicker && el.__datePicker.dateParts) || undefined;
    },

    areComponentsFullDate: function(partialDate) {
      return partialDate && partialDate.year && partialDate.month && partialDate.day;
    },

    getExactDate: function(el) {
      if (!el.alt) {
        return undefined;
      } else {
        var date = new Date(el.alt);

        if (el.hasClassName('fuzzy-date')) {
          return date.toUTC();
        } else {
          return date;
        }
      }
    },

    getCalculatedAgeFromExactDateAndDob: function(bday, thisDate) {
      var age = new TimePeriod(bday, thisDate);
      var dateDisplayParts = {
        y: age.years,
        m: age.months,
        d: age.days,
      };
      var ageStr = "";
      Object.keys(dateDisplayParts).each(function(k) {
        if (dateDisplayParts[k] > 0) ageStr += dateDisplayParts[k] + k;
      });

      return ageStr;
    },

    getCalculatedAgeFromPartialDateAndDob: function(dobParts, dateParts) {
      var dobMonths = +dobParts.year * 12;
      var dateMonths = +dateParts.year * 12;
      if (dobParts.month && dateParts.month) {
        dobMonths += +dobParts.month;
        dateMonths += +dateParts.month;
      }

      var diff = dateMonths - dobMonths;
      if (diff < 0) {
        console && console.error('Calculated age should not be negative');
      }
      var ageStr = '';
      if (Math.floor(diff / 12) > 0) {
        ageStr += Math.floor(diff / 12) + 'y';
      }
      if (diff % 12 > 0) {
        ageStr += (diff % 12) + 'm';
      }

      return ageStr;
    },
  });

  var init = function(event) {
    ((event && event.memo.elements) || [$('body')]).each(function(element) {
      element.select('input[type="text"].pt-age-calculated-from-date').each(function(item) {
        if (!item.__ageCalculatedFromDate) {
          new PhenoTips.widgets.AgeCalculatedFromDate(item);
        }
      });
    });
    return true;
  };

  (XWiki.domIsLoaded && init()) || document.observe("xwiki:dom:loaded", init);
  document.observe("xwiki:dom:updated", init);

  // End PhenoTips augmentation.
  return PhenoTips;
}(PhenoTips || {}));