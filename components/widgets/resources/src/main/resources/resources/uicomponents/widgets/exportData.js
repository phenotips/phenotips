document.observe('xwiki:dom:loading', function() {
  var suggestionsMapping = {
        "hpo" : {
            script: new XWiki.Document('SolrService', 'PhenoTips').getURL("get") + "?",
            varname: "q",
            noresults: "$services.localization.render('phenotips.DBWebHomeSheet.noResults')",
            json: true,
            resultsParameter : "rows",
            resultId : "id",
            resultValue : "name",
            resultAltName: "synonym",
            resultCategory : "term_category",
            resultInfo : {},
            tooltip: 'phenotype-info',
            enableHierarchy: true,
            resultParent : "is_a",
            fadeOnClear : false,
            timeout : 30000
        },
        "omim" : {
            script: new XWiki.Document('SolrService', 'PhenoTips').getURL("get", "vocabulary=omim") + "&amp;",
            varname: "q",
            noresults: "$services.localization.render('phenotips.DBWebHomeSheet.noResults')",
            json: true,
            resultsParameter : "rows",
            resultId : "id",
            resultValue : "name",
            resultInfo : {
                           "Locus"      : {"selector"  : "Locus"}
                         },
            tooltip: 'omim-disease-info',
            enableHierarchy: false,
            fadeOnClear : false,
            timeout : 30000
        }
  };
  // =================================================================================
  // Suggest maps
  var pickerSpecialClassOptions = {
    'defaultPicker' : {},
    'generateShortList' : {
                  'showKey' : true,
                  'showTooltip' : false,
                  'showDeleteTool' : true,
                  'enableSort' : false,
                  'showClearTool' : true,
                  'inputType': 'checkbox'
                },
    'generateCheckboxes' : {
                  'showKey' : false,
                  'showTooltip' : false,
                  'showDeleteTool' : true,
                  'enableSort' : false,
                  'showClearTool' : true,
                  'inputType': 'checkbox',
                  'listInsertionElt' : '.label-other',
                  'listInsertionPosition' : 'after',
                  'acceptFreeText' : true
                }
  };

  var enhanceDialog = function(content) {
    // =============================================================================
    // Prepare to copy existing filters:
    var tableFilters = $('patients-topfilters');
    if (tableFilters) {
      tableFilters = tableFilters.down('ul.filters');
    }
    //============================================================================
    // Copy all text inputs, checkboxes and radio buttons
    if (tableFilters) {
      content.select('input[type="text"]').each(function(input) {
        var source = tableFilters.down('input[type="' + input.type + '"][name="' + input.name + '"]');
        if (source) {
          input.value = source.value;
        }
      });
      content.select('input[type="radio"], input[type="checkbox"]').each(function(input) {
        var source = tableFilters.down('input[type="' + input.type + '"][name="' + input.name + '"][value="' + input.value + '"]');
        if (source) {
          input.checked = source.checked;
        }
      });
    }
    // =============================================================================
    // Copy entries from users suggest
    if (tableFilters) {
      content.select('input.suggestUsers').each(function(input) {
        var source = tableFilters.down('input[type="text"][name="' + input.name + '"]');
        var selectionContainer = source && source.previous('ul.accepted-suggestions')
        if (selectionContainer) {
          selectionContainer.select('li').each(function(entry) {
            var entryData = {
              'id': entry.down('.user-alias').childNodes[0].nodeValue,
              'value': entry.down('input[type="hidden"]').value,
              'info': entry.down('img').alt,
              'icon' : entry.down('img').src,
              'hint' : ''
            };
            input.__x_suggest.options.callback(entryData);
          });
        } // done with selected entries
      });
      // TODO: Groups
    }
    // =============================================================================
    // Phenotype suggest: create suggest and copy existing entries
    if (typeof(PhenoTips.widgets.Suggest) != "undefined") {
      var keys = Object.keys(suggestionsMapping);
      var specialClasses = Object.keys(pickerSpecialClassOptions);
      for (var i = 0; i < keys.length; i++) {
        var selector = 'input.suggest' + keys[i].capitalize();
        content.select(selector).each(function(item) {
          if (!item.hasClassName('initialized')) {
            item._customOptions = Object.clone(suggestionsMapping[keys[i]]);
            item._restriction = item.up('.phenotype-group')
            if (item._restriction) {
              item._restriction = item._restriction.down('input[name=_category]');
              if (item._restriction && item._restriction.value.strip() != '') {
                item._restriction = item._restriction.value.strip().split(",");
              } else {
                item._restriction = null;
              }
            }
            if (item._customOptions.queryProcessor && item._restriction) {
              item._customOptions.queryProcessor = Object.clone(item._customOptions.queryProcessor);
              item._customOptions.queryProcessor.restriction = {
                'term_category' : item._restriction
              }
            }
            // Create the Suggest.
            item._suggest = new PhenoTips.widgets.Suggest(item, item._customOptions);
            if (item.hasClassName('multi') && typeof(PhenoTips.widgets.SuggestPicker) != "undefined") {
              var multiSuggestOptions = {};
              for (var j = 0; j < specialClasses.length; j++) {
                if (item.hasClassName(specialClasses[j])) {
                  multiSuggestOptions = pickerSpecialClassOptions[specialClasses[j]];
                  break;
                }
              }
              var suggestPicker = new PhenoTips.widgets.SuggestPicker(item, item._suggest, multiSuggestOptions);
              item._suggestPicker = suggestPicker;

              // Format the predefined value
              item.value.split(',').each(function(value) {
                item._suggestPicker.addItem(value, value, '', '');
              });
              item.value = '';
            }
            if (tableFilters) {
              // Integrate the custom fields
              // 1. find the container element displaying them
              var source = tableFilters.down('input[type="text"][name="' + item.name + '"]');
              var selectionContainer = source && source.next('.accepted-suggestions');
              if (selectionContainer) {
                // 2. find all the values and display them as part of the multi suggest picker
                var tmp = suggestPicker.silent;
                suggestPicker.silent = true;
                selectionContainer.select('.accepted-suggestion').each(function(entry) {
                  var value =  entry.down('input[type="checkbox"]').value;
                  var text = entry.down('.value').innerHTML;
                  var category = entry.down('.term-category');
                  suggestPicker.addItem(value, text, '', category || '');
                });
                suggestPicker.silent = tmp;
              }
            } // done with selected entries
            item.addClassName('initialized');
          }
        });
      }
    }

    //============================================================================
    // Date pickers: create pickers and copy existing dates
    if (typeof (XWiki.widgets.DateTimePicker) != "undefined") {
      var crtYear = new Date().getFullYear();
      if (tableFilters) {
        content.select('.xwiki-date').each(function(input) {
          var source = tableFilters.down('input[name="' + input.name + '"]');
          if (source) {
            input.value = source.value;
            input.alt = source.alt;
          }
        });
      }
      window.dateTimePicker = new XWiki.widgets.DateTimePicker({year_range: [crtYear - 99, crtYear + 1]});
    }
    
    //============================================================================
    // Column selection
    var columnList = content.down('.section.columns ul');
    if (columnList) {
      var selectionTools = new Element('div', { 'class' : 'selection-tools' }).update("$services.localization.render('phenotips.DBWebHomeSheet.colSelect.label') ");
      var all = new Element('span', {'class' : 'selection-tool select-all'}).update("$services.localization.render('phenotips.DBWebHomeSheet.colSelect.all')");
      var none = new Element('span', {'class' : 'selection-tool select-none'}).update("$services.localization.render('phenotips.DBWebHomeSheet.colSelect.none')");
      var invert = new Element('span', {'class' : 'selection-tool select-invert'}).update("$services.localization.render('phenotips.DBWebHomeSheet.colSelect.invert')");
      var restore = new Element('span', {'class' : 'selection-tool select-restore'}).update("$services.localization.render('phenotips.DBWebHomeSheet.colSelect.restore')");
      selectionTools.insert(all).insert(' · ').insert(none).insert(' · ').insert(invert).insert(' · ').insert(restore);

      columnList.select('input[type=checkbox]').each(function(elt) {elt._originallyChecked = elt.checked});

      all.observe('click', function(event) {
        columnList.select('input[type=checkbox]').each(function(elt) {elt.checked = true});
      });
      none.observe('click', function(event) {
        columnList.select('input[type=checkbox]').each(function(elt) {elt.checked = false});
      });
      invert.observe('click', function(event) {
        columnList.select('input[type=checkbox]').each(function(elt) {elt.checked = !elt.checked});
      });
      restore.observe('click', function(event) {
        columnList.select('input[type=checkbox]').each(function(elt) {elt.checked = elt._originallyChecked});
      });
      columnList.insert({'before' : selectionTools});
    }

    //============================================================================
    // Column selection
    var cancelButton = $('export_cancel');
    if (cancelButton) {
      cancelButton.observe('click', function(event) {
        content.__dialog && content.__dialog.closeDialog();
      });
    }

    //==========================================================================
    // Live updates
    var liveMatchCounter = $('filter-match-count');
    var form = liveMatchCounter && liveMatchCounter.up('form');
    if (liveMatchCounter && form && !liveMatchCounter.initialized) {
      liveMatchCounter.initialized = true;
      var lastRequestID = 0;
      var updateMatchCounter = function(event) {
        var url = "$xwiki.getURL('PhenoTips.ExportFilter', 'get')?count=true&amp;" + form.serialize();
        var requestID = ++lastRequestID;
        var ajx = new Ajax.Request(url, {
          method: 'get',
          onSuccess: function(response) {
            if (requestID < lastRequestID) {return;}
            liveMatchCounter.update(response.responseText);
          },
          onFailure: function (response) {}
        });
      };
      updateMatchCounter();
      content.select('.xwiki-date').invoke('observe', 'xwiki:form:field-value-changed', updateMatchCounter);
      document.observe('custom:selection:changed', updateMatchCounter);
      document.observe('xwiki:multisuggestpicker:selectionchanged', updateMatchCounter);
      content.select('input[type=radio]').invoke('observe', 'click', updateMatchCounter);
      content.select('input[type=checkbox]').invoke('observe', 'click', updateMatchCounter);
      content.select('input[name=external_id]').invoke('observe', 'keyup', updateMatchCounter);
    }
  };
  var exportTools = $$('.phenotips_export_tool');
  if (exportTools && exportTools.length != 0) {
    exportTools.each(function(exportTool) {
      exportTool.observe('click', function(event) {
        event.stop();
        var dialog = new PhenoTips.widgets.ModalPopup('&lt;img src="$xwiki.getSkinFile('icons/xwiki/ajax-loader-large.gif')"/&gt;', false, {'title':"$services.localization.render('phenotips.DBWebHomeSheet.exportPreferences.title')", 'verticalPosition': 'top', 'removeOnClose': true, 'extraClassName': 'export-dialog'});
        dialog.showDialog();
        // =================================================================================
        // Generate the dialog content
        new Ajax.Request(new XWiki.Document('ExportPreferences', 'PhenoTips').getURL('get', 'space=' + /space=([^&amp;]+)/.exec(exportTool.href)[1]), {
          parameters: {export_endpoint: exportTool.readAttribute("href"), export_id: exportTool.readAttribute("id")},
          onSuccess: function(transport) {
            var content = dialog.dialogBox._x_contentPlug;
            content.update(transport.responseText);
            content.__dialog = dialog;
            document.fire('xwiki:dom:updated', {'elements': [content]});
          }
        });
      });
    });
  }

  document.observe('xwiki:multisuggestpicker:selectionchanged', function() {
    document.fire('xwiki:livetable:patients:filtersChanged');
  });
  ['xwiki:dom:loaded', 'xwiki:dom:updated'].each(function(eventName) {
    document.observe(eventName, function(event) {
      var elements = event.memo && event.memo.elements || [document.documentElement];
      elements && elements.each(function(item) {
        // We defer so that the standard suggests have time to initialize before applying the enhanced suggests
        enhanceDialog.defer(item);
      });
    });
  });
});