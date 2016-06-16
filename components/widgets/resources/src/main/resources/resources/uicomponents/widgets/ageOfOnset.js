var PhenoTips = (function(PhenoTips) {
  // Start PhenoTips augmentation
  var widgets = PhenoTips.widgets = PhenoTips.widgets || {};

  widgets.AgeOfOnset = Class.create({
    // The age until which the specified HPO term applies
    ageMonthsToHpoTerm: [
      [0.92008, "HP:0003623"],
      [12, "HP:0003593"],
      [5*12, "HP:0011463"],
      [15*12, "HP:0003621"],
      [40*12, "HP:0011462"],
      [60*12, "HP:0003596"],
      [Number.MAX_SAFE_INTEGER, "HP:0003584"],
    ],
    initialize: function(el) {
      this.el = el;

      // Try to find the corresponding HPO branch among DOM siblings. Go up a
      // maximum of 2 nesting levels.
      var siblingsRef = el;
      var hpoBranch;
      for (var i = 0; i < 3; i++) {
        hpoBranch = Prototype.Selector.find(siblingsRef.siblings(), '.hpo-branch');
        if (hpoBranch) {
          break;
        } else {
          siblingsRef = siblingsRef.up();
        }
      }
      if (hpoBranch) {
        this._hpoBranch = hpoBranch;
      } else {
        console && console.error('Linked HPO branch not found');
        return;
      }

      this._handleAgeChange = this._handleAgeChange.bind(this);

      // Attach handlers
      el.observe('duration:format', this._handleAgeChange);
      el.observe('duration:change', this._handleAgeChange);

      // If the age has already been initialized, set the initial state
      if (this.el.__ageCalculatedFromDate) {
        this._handleAgeChange();
      }

      el.__ageOfOnset = this;
    },

    _handleAgeChange: function() {
      var ageMonths = this.el.title;

      var termToSelect;
      for (var i = 0; i < this.ageMonthsToHpoTerm.length; i++) {
        if (ageMonths < this.ageMonthsToHpoTerm[i][0]) {
          termToSelect = this.ageMonthsToHpoTerm[i][1];
          break;
        }
      }

      this._hpoBranch.select("input[type=radio]").each(function(radioEl) {
        if (!ageMonths || ageMonths == "0") {
          radioEl.disabled = false;
        } else {
          if (radioEl.value == termToSelect) {
            // Expand the section containing this option in case it's collapsed
            if (radioEl.up(1) && radioEl.up(1).hasClassName('collapsed')) {
              radioEl.up(1).previous('.expand-tool').click();
            }
            radioEl.disabled = false;
            radioEl.checked = true;
            radioEl.fire('value:change');
          } else {
            radioEl.checked = false;
            radioEl.disabled = true;
          }
        }
      });
    },
  });

  var init = function(event) {
    ((event && event.memo.elements) || [$('body')]).each(function(element) {
      element.select('input[type="text"].pt-age-of-onset').each(function(item) {
        if (!item.__ageOfOnset) {
          new PhenoTips.widgets.AgeOfOnset(item);
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