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
          existingEl._ynpicker._select(phenotype.isObserved ? 'yes' : 'no');
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
        if (phenotypeEl) {
          phenotypeEl._ynpicker._onUnselect();
        } else {
          phenotypeEl.checked = false;
        }
      }
    },

    /**
     * Updates a selected phenotype. If the phenotype is not selected, do nothing.
     */
    /*updateSelectedPhenotype: function(phenotype) {
    },*/

    _findFormElementForPhenotype: function(id, negative, subtype) {
      var result = null;
      if ($('prefix')) {
        result = $($('prefix').value + (negative ? 'negative_' : '') + (subtype ? subtype + '_' : '') + 'phenotype_' + id);
      }
      return result;
    },
  };
});
