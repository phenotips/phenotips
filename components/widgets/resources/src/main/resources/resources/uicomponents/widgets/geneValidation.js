var XWiki = (function(XWiki) {
  // Start XWiki augmentation
  var widgets = XWiki.widgets = XWiki.widgets || {};

  widgets.GeneValidator = Class.create({
    initialize : function(input) {
      this.input = input;
      this.valid = true;
      this.state = 'NEW';
      this.value = input.value;
      if (!this.input.__validation) {
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
        var genesymbols = [];
        $$('.gene.col-label.gene-input-label').each( function (item) {
          if (item.next() != el)
            genesymbols.push(item.textContent || item.innerText);
        });
        if (genesymbols.indexOf(el.value) > -1) {
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
        Validate.fail("This gene has already been entered.");
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
          input.__Gene_validator = new XWiki.widgets.GeneValidator(input);
        }
      });
    });
    return true;
  };

  (XWiki.domIsLoaded && init()) || document.observe("xwiki:dom:loaded", init);
  document.observe('xwiki:dom:updated', init);
  document.observe('ms:suggest:selected', function(event) {
    var inputElement = event.findElement();
    var genesymbols = [];
    $$('.gene.col-label.gene-input-label').each( function (item) {
      if (item.next() != inputElement)
        genesymbols.push(item.textContent || item.innerText);
    });
    if (genesymbols.indexOf(event.memo.value) > -1) {
      inputElement.__Gene_validator.input.value = event.memo.value;
      inputElement.__Gene_validator.validate(event.memo.value);
    } else {
      var triggeredEvent = new Event('keyup');
      inputElement.dispatchEvent(triggeredEvent);
    }
  });

  // End XWiki augmentation.
  return XWiki;
}(XWiki || {}));
