var XWiki = (function(XWiki) {
  // Start XWiki augmentation
  var widgets = XWiki.widgets = XWiki.widgets || {};

  widgets.VariantValidator = Class.create({
    initialize : function(input) {
      this.input = input;
      this.valid = true;
      this.state = 'NEW';
      this.value = input.value;
      if (!this.input.__validation) {
        this.input.__validation = new LiveValidation(this.input, {validMessage: '', wait : 500});
      }
      this.input.__validation.add(this.validate.bind(this));
    },
    check : function() {
      if (this.input.value != this.value) {
        this.value = this.input.value;
        this.state = 'CHECKING';
        var el = this.input;
        var cdnas = [];
        $$('.variant.variant-default-input.cdna input').each( function (item) {
          if (item != el)
            cdnas.push(item.value);
        });
        if (cdnas.indexOf(el.value) > -1) {
          this.invalid();
        } else {
          this.available();
        }
        this.responded();
      }
    },
    validate : function(value) {
      if (this.state == 'DONE' &&
          this.value == value &&
          !this.valid) {
        Validate.fail("$services.localization.render('phenotips.tableMacros.variantAlreadyExist')");
      }
      this.check();
      return true;
    },

    available : function() {
      this.valid = true;
    },
    invalid : function() {
      this.valid = false;
    },
    responded : function() {
      this.state = 'DONE';
      this.input.__validation.validate();
    },
  });

  var init = function(event) {
    ((event && event.memo.elements) || [$('body')]).each(function(element) {
      element.select('.variant.variant-default-input.cdna input').each(function(input) {
        if (!input.__Variant_validator) {
          input.__Variant_validator = new XWiki.widgets.VariantValidator(input);
        }
      });
    });
    return true;
  };

  (XWiki.domIsLoaded && init()) || document.observe("xwiki:dom:loaded", init);
  document.observe('xwiki:dom:updated', init);

  // End XWiki augmentation.
  return XWiki;
}(XWiki || {}));
