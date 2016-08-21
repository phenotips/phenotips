define("PhenotypeSelectionUtils", [], function() {
  return {
    /*
      phenotype data structure:
      id <- an HPO id or arbitrary text
      isObserved <- boolean describing whether or not the phenotype was observed
      qualifiers: {
        spatial_pattern <- HPO ID
        severity <- HPO ID
      }
    */

    /**
     * Selects a phenotype. If the phenotype is already selected, do nothing.
     * @param  {object} phenotype         the phenotype. see object spec above.
     * @param  {string} autoSelectSrcDesc a string describing the reason for auto-selection of this phenotype, if applicable.
     */
    selectPhenotype: function(phenotype, autoSelectSrcDesc) {
      var _this = this;
      var addAutomaticSelectionIndicator = function(phenotypeEl) {
        var addedItem = $('current-phenotype-selection').down('label input[value="' + phenotypeEl.value + '"]');
        if (addedItem && autoSelectSrcDesc) {
          addedItem.up('label').insert({before : new Element('span', {
                                                               'class' : 'fa fa-bolt',
                                                               'title' : 'This phenotype was automatically added based on ' + autoSelectSrcDesc
          }).update(' ')});
        }
      };

      var existingEl = this._findFormElementForPhenotype(phenotype.id, !phenotype.isObserved);
      if (existingEl) {
        if (existingEl._ynpicker) {
          existingEl._ynpicker._onSelect(phenotype.isObserved ? 'yes' : 'no');
        } else {
          existingEl.checked = true;
        }
        addAutomaticSelectionIndicator(existingEl);
      } else {
        var suggestWidget = $("quick-phenotype-search")._suggestWidget;
        if (!suggestWidget) {
           return null;
        }
        var id = phenotype.id;
        var searchUrl = new XWiki.Document('SolrService', 'PhenoTips').getURL("get", "q=" + id);
        new Ajax.Request(searchUrl, {
          method: 'get',
          onSuccess: function(transport) {
            var response = transport.responseJSON.rows[0];
            if (response) {
              var categories = "";
              if (!phenotype.isObserved) {
                categories += '<input type="hidden" name="fieldName" value="PhenoTips.PatientClass_0_negative_phenotype" class="no">';
              }
              categories += '<span class="hidden term-category">';
              response.term_category.each(function(category){categories += '<input type="hidden" value="' + category + '">';});
              categories += "</span>";
              var data = {
                id: id,
                value: response.name,
                category: categories,
                negative: !phenotype.isObserved
              };
              var title = response.name;
              suggestWidget.acceptEntry(data, title, title, true);

              existingEl = _this._findFormElementForPhenotype(phenotype.id, !phenotype.isObserved);
              addAutomaticSelectionIndicator(existingEl);
            }
          }
        });
      }
    },

    /**
     * Deselects a phenotype. If the phenotype is not selected, do nothing.
     * @param  {object} phenotype the phenotype. see object spec above.
     */
    deselectPhenotype: function(phenotype) {
      var phenotypeEl = this._findFormElementForPhenotype(phenotype.id, !phenotype.isObserved);
      if (phenotypeEl) {
        if (phenotypeEl._ynpicker) {
          phenotypeEl._ynpicker._onUnselect();
        } else {
          phenotypeEl.checked = false;
        }
      }
    },

    /**
     * Gets an array of phenotypes that are currently selected.
     * @return {array} phenotypes the selected phenotypes. see object spec above.
     */
    getSelectedPhenotypes: function() {
      return $$('div.phenotype .yes-no-picker').map(function(e) {
        var yes = e.down('.yes input');
        var no = e.down('.no input');
        /* Not a phenotype */
        if (yes.name === no.name) {
          return null;
        }
        var id = yes.value;
        var set = yes.checked || no.checked;
        if(set) {
          return {id: id, isObserved: yes.checked};
        } else {
          return null;
        }
      }).filter(function(e) { return e; });
    },

    _findFormElementForPhenotype: function(id, negative, subtype) {
      var result = null;
      if ($('prefix')) {
        result = $($('prefix').value + (negative ? 'negative_' : '') + (subtype ? subtype + '_' : '') + 'phenotype_' + id);
      }
      return result;
    },
  };
});
