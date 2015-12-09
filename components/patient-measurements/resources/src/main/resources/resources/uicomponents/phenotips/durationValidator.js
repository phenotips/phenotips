var PhenoTips = (function(PhenoTips) {
  // Start PhenoTips augmentation
  var widgets = PhenoTips.widgets = PhenoTips.widgets || {};

  widgets.DurationValidator =  Class.create({
    pieces : {
      y : 'years',
      m : 'months',
      w : 'weeks',
      d : 'days',
      n : '([1-9][0-9]*)',
      s : '\\s*'
    },
    initialize : function(element) {
      if (!element) {
        return;
      }
      var sep = this.pieces.s;
      this.subgroups = {
         y  : this._regexpifyDurationUnit('y', true),
         m  : this._regexpifyDurationUnit('m', true),
         w  : this._regexpifyDurationUnit('w', true),
         d  : this._regexpifyDurationUnit('d', true)
      };
      this.regexp = new RegExp("^((" + this.subgroups.y + sep + this.subgroups.m + ")|" + this.subgroups.w + ")" + sep + this.subgroups.d + "$");
      this.element = element;
      var _this = this;
      ['keyup', 'input'].each(function(ev) {
        element.observe(ev, function(event) {
          if (_this.regexp.match(element.value)) {
            element.removeClassName('error');
          } else {
            element.addClassName('error');
          }
        });
      });
      element.observe('blur', function(event) {
        element.value = _this.format(element.value);
        Element.fire(element, 'duration:format');
        element.title = _this.getValue(element.value);
      });
    },
    _regexpifyDurationUnit : function(unit, makeOptional) {
      return "(" + this.pieces.s + this.pieces.n + this.pieces.s + this._regexpifyWord(this.pieces[unit] || "") + this.pieces.s + ")" + (makeOptional ? '?' : '');
    },
    _regexpifyWord : function(word) {
      return word.replace(/(.)/g, "$1?").replace("?", "");
    },
    match : function(text) {
      return (text || "").match(this.regexp);
    },
    getValue : function(text) {
      if (this.match(text)) {
        var result = 0;
        var _this = this;
        var item;
        [['y', 12], ['m', 1], ['w', 0.23], ['d', 0.03286]].each(function(unit) {
          item = text.match(new RegExp(this._regexpifyDurationUnit(unit[0], false)));
          if (item) {
            result += item[2] * unit[1];
          }
        }.bind(this));
        return result;
      } else {
        return -1;
      }
    },
    format : function(text) {
      if (this.match(text)) {
        var result = text;
        var _this = this;
        ['y', 'm', 'w', 'd'].each(function(unit) {
          result = result.replace(new RegExp(this._regexpifyDurationUnit(unit, false)), "$2" + this.pieces[unit][0]);
        }.bind(this));
        return result.replace(this.pieces.sep, "", "g");
      } else {
        return text;
      }
    }
  });

  var init = function(event) {
    ((event && event.memo.elements) || [$('body')]).each(function(element) {
      element.select('input[type="text"].pt-duration:not(.initialized)').each(function(item) {
        new PhenoTips.widgets.DurationValidator(item);
      });
    });
    return true;
  };

  (XWiki.domIsLoaded && init()) || document.observe("xwiki:dom:loaded", init);
  document.observe("xwiki:dom:updated", init);

  // End PhenoTips augmentation.
  return PhenoTips;
}(PhenoTips || {}));