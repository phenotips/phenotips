var XWiki = (function(XWiki) {
  // Start XWiki augmentation
  var widgets = XWiki.widgets = XWiki.widgets || {};

  widgets.DuplicateValidator = Class.create({
    initialize : function(input, selector, message) {
      this.input = input;
      this.valid = true;
      this.state = 'NEW';
      this.value = input.value;
      this.selector = selector;
      this.message = message;

      if (!this.input.__validation && !(this.input.type == "hidden")) {
        try {
          this.input.__validation = new LiveValidation(this.input, {validMessage: '', wait : 500});
        }
        catch(err) {
          //console.log(err);
        }
      }
      if (this.input.__validation)
        this.input.__validation.add(this.validate.bind(this));
    },
    check : function() {
      if (this.input.value != this.value) {
        this.value = this.input.value;
        this.state = 'CHECKING';
        var el = this.input;
        var allItems = [];
        $$(this.selector).each( function (item) {
          if (item != el) {
            allItems.push(item.value);
          }
        });
        if (allItems.indexOf(el.value) > -1) {
          this.invalid();
        } else {
          this.available();
        }
        this.responded();
      }
    },
    validate : function(value) {
      if (value.blank()) {
        return true;
      }
      if (this.state == 'DONE' &&
          this.value == value &&
          !this.valid) {
        Validate.fail(this.message);
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
      element.select('input.gene-name').each(function(input) {
        if (!input.__Gene_validator) {
          input.__Gene_validator = new XWiki.widgets.DuplicateValidator(input, 'input.gene-name', "$services.localization.render('PhenoTips.GeneClass.geneAlreadyExist')");
        }
      });    
      element.select('.variant.cdna input').each(function(input) {
        if (!input.__Variant_validator) {
          input.__Variant_validator = new XWiki.widgets.DuplicateValidator(input, '.variant.cdna input', "$services.localization.render('PhenoTips.GeneVariantClass.variantAlreadyExist')");
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
