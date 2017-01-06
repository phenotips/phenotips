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
            script: new XWiki.Document('SolrService', 'PhenoTips').getURL("get", "vocabulary=omim") + "&",
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
        },
        "genes" : {
            script: new XWiki.Document('GeneNameService', 'PhenoTips').getURL('get', 'outputSyntax=plain') + "&",
            varname: "q",
            noresults: "No matching gene names",
            json: true,
            resultsParameter : "docs",
            resultId : 'symbol',
            resultValue : 'symbol',
            resultAltName : "alias_symbol",
            enableHierarchy: false,
            tooltip : 'gene-info',
            resultInfo : {},
            fadeOnClear : false,
            timeout : 30000,
            parentContainer : null
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
      if (tableFilters) {
        content.select('.xwiki-date').each(function(input) {
          var source = tableFilters.down('input[name="' + input.name + '"]');
          if (source) {
            input.value = source.value;
            input.alt = source.alt;
          }
        });
      }
      if (window.dateTimePicker) {
        window.dateTimePicker.attachPickers(content);
      } else {
        var crtYear = new Date().getFullYear();
        window.dateTimePicker = new XWiki.widgets.DateTimePicker({year_range: [crtYear - 99, crtYear + 1]});
      }
    }

    //============================================================================
    // Push dialog check boxes and expand tools
    var titles = content.select('.push-fields.section.columns > div h5');
      if (titles) {
        titles.each(function(item, index) {
          var sectionList = item.next('ul');
          var showIcon = '<span class="fa fa-plus-square-o fa-lg"></span>';
          var chapterShow = new Element('button', {'class' : 'tool button secondary', 'type' : 'button'}).update(showIcon+" "+"$services.localization.render('phenotips.patientSheet.expandSection')");
          var hideIcon = '<span class="fa fa-minus-square-o fa-lg"></span>';
          var chapterHide = new Element('button', {'class' : 'tool button secondary', 'type' : 'button'}).update(hideIcon+" "+"$services.localization.render('phenotips.patientSheet.collapseSection')");
          var chapterShowWrapper = new Element('span', {'class' : 'buttonwrapper show'}).insert(chapterShow);
          var chapterHideWrapper = new Element('span', {'class' : 'buttonwrapper hide'}).insert(chapterHide);
          var chapterExpandTools = new Element('span', {'class' : 'expand-tools'}).insert(chapterShowWrapper).insert(chapterHideWrapper);
          item.insert({after: chapterExpandTools});
          // section expand tools behaviour
          [chapterShow, chapterHide, item.down('span')].invoke('observe', 'click', function (event) {
            event.stop();
            sectionList.toggleClassName('v-collapsed');
            chapterShowWrapper.toggleClassName('v-collapsed');
            chapterHideWrapper.toggleClassName('v-collapsed');
          });
          // leave patient info section expanded
          if (index != 0) {
            chapterHideWrapper.toggleClassName('v-collapsed');
          } else {
            chapterShowWrapper.toggleClassName('v-collapsed');
          }

          // checkbox behaviour:
          //  1) parent checkbox sets child checkboxes to the same value
          //  2) when not all child checkboxes are in the same state paret checkbox is set to an "indeterminate" state
          // (see https://css-tricks.com/indeterminate-checkboxes and Prototype conversion @ http://codepen.io/anon/pen/BKeVRP)

          var parentChildController = function(elt, onClick) {
            var checked = elt.checked;
            var container = elt.up(".checkbox_tree_container");

            if (onClick) {
              // set all child checkboxes to a determinate state equal to the current state of the checkbox
              // note that this should not be done for initial values of the checkboxes
              container.select('input[type="checkbox"]').each(function(elt){
                elt.indeterminate = false;
                elt.checked = checked;
              });
            }

            // update parent checkboxes to set/unset/indetermninate, depending on all children
            function checkSiblings(el) {
              var parent = el.up('.checkbox_tree_container');
              if (parent) {
                var parentCheckbox = parent.down('input[type="checkbox"]');

                var allSiblingsSameStatus = true;
                el.siblings().each(function(sibling) {
                  sibling.select('input[type="checkbox"]').each(function(elt) {
                    if (elt.checked != checked) {
                      allSiblingsSameStatus = false;
                    }
                  });
                });

                if (allSiblingsSameStatus && checked) {
                  // mark parent node as checked
                  parentCheckbox.indeterminate = false;
                  parentCheckbox.checked = checked;
                  // TODO: enable when/if we have nested categories
                  // checkSiblings(parent);
                } else if (allSiblingsSameStatus && !checked) {
                  parentCheckbox.checked = checked;
                  var hasCheckedChildren = (parent.select('input[type="checkbox"]:checked').length > 0);
                  parentCheckbox.indeterminate = hasCheckedChildren;
                  // TODO: enable when/if we have nested categories:
                  // checkSiblings(parent);
                } else {
                  var setAllParentsIndeterminate = function(elt) {
                    var parent = elt.up('.checkbox_tree_container');
                    if (parent) {
                      var parentChekcbox = parent.down('input[type="checkbox"]');
                      parentCheckbox.indeterminate = true;
                      parentCheckbox.checked = false;
                      // TODO: enable when/if we have nested categories
                      // setAllParentsIndeterminate(parent);
                    }
                  };
                  setAllParentsIndeterminate(el);
                }
              }
            }
            checkSiblings(container);
          }

          // current structure is <div> <h5><parent_checkbox></h5> <ul>...<li>child_checkbox</li>...</ul> </div>
          item.up('div').select('input[type=checkbox]').each(function(elt) {
            elt.onchange = function() {
              parentChildController(elt, true);  // update based on current state, and propagate state change if necessary
            };
            parentChildController(elt, false);   // initial update using pre-set values: do not propagate state
          });
       });
    }

    //============================================================================
    // Column selection "select all" tools
    var checkboxList = content.select('.section.columns input[type=checkbox]');
    var columnList = content.down('.section.columns h3');
    if (columnList && checkboxList) {
      var all = $$('.selection-tool.select-all')[0];
      var none = $$('.selection-tool.select-none')[0];
      var invert = $$('.selection-tool.select-invert')[0];
      var restore = $$('.selection-tool.select-restore')[0];

      checkboxList.each(function(elt) {
          elt._originallyChecked = elt.checked;
          elt._originallyIndeterminate = elt.indeterminate;
      });

      all.observe('click', function(event) {
        checkboxList.each(function(elt) { elt.checked = true; elt.indeterminate = false; });
      });
      none.observe('click', function(event) {
        checkboxList.each(function(elt) {elt.checked = false; elt.indeterminate = false;});
      });
      invert.observe('click', function(event) {
        checkboxList.each(function(elt) {if (!elt.indeterminate) { elt.checked = !elt.checked; } });
      });
      restore.observe('click', function(event) {
        checkboxList.each(function(elt) {
          elt.checked = elt._originallyChecked;
          elt.indeterminate = elt._originallyIndeterminate;
        });
      });
    }


    //============================================================================
    // Column selection cancel button
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
        var url = "$xwiki.getURL('PhenoTips.ExportFilter', 'get')?count=true&" + form.serialize();
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
        new Ajax.Request(new XWiki.Document('ExportPreferences', 'PhenoTips').getURL('get', 'space=' + /space=([^&]+)/.exec(exportTool.href)[1]), {
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
