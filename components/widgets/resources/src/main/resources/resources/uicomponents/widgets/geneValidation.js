var XWiki = (function(XWiki) {
  // Start XWiki augmentation
  var widgets = XWiki.widgets = XWiki.widgets || {};

  widgets.GeneVariantValidator = Class.create({
    initialize : function(input) {
      this.input = input;
      this.valid = true;
      this.state = 'NEW';
      this.value = input.value;
      if (!this.input.__validation) {
        try {
          this.input.__validation = new LiveValidation(this.input, {validMessage: '', wait : 500, displayMessageWhenEmpty: true});
        }
        catch(err) {
          //console.log(err);
        }
      }
      if (this.input.__validation)
        this.input.__validation.add(this.validate.bind(this));
    },
    check : function() {
      if (this.input.value.blank() || this.input.value != this.value) {
        this.value = this.input.value;
        this.state = 'CHECKING';
        var el = this.input;
        if (el.value.blank()) {
          this.invalid();
        } else if (this.input.className.include('gene-name')){
          var genesymbols = [];
          $$('.gene.col-label.gene-input-label').each( function (item) {
            if (item.next() != el) {
              genesymbols.push(item.textContent || item.innerText);
            }
          });
          if (genesymbols.indexOf(el.value) > -1) {
            this.invalid();
          } else {
            this.available();
          }
        }
        this.responded();
      }
    },
    validate : function(value) {
      if ((this.state == 'DONE' && this.value == value && !this.valid) ||
          (this.state == 'NEW' && value.blank())) {
        var message = (value.blank()) ? "Input is blank." : "This gene has already been entered.";
        Validate.fail(message);
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
      element.select('[name^="PhenoTips.GeneClass_"][name$="_gene"]').each(function(input) {
        if (!input.__Gene_validator) {
          input.__Gene_validator = new XWiki.widgets.GeneVariantValidator(input);
        }
      });
	  element.select('[name^="PhenoTips.GeneVariantClass_"][name$="_cdna"]').each(function(input) {
        if (!input.__Variant_validator) {
          input.__Variant_validator = new XWiki.widgets.GeneVariantValidator(input);
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
