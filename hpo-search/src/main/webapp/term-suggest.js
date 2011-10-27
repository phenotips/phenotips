document.observe('dom:loaded', function() {
    // hpo: namespace:medical_genetics
    // go : namespace:
    var highlightChecked = function(element) {
      var subsection = element.up('.subsection');
      if (subsection) {
	var subsectionTitle = subsection.previous('label.section');
      }
      if (element.checked) {
        element.up('label').addClassName('selected');
	if (subsectionTitle) {
	  subsectionTitle.addClassName('selected');
	}
      } else {
        element.up('label').removeClassName('selected');
	if (subsectionTitle) {
	  subsectionTitle.removeClassName('selected');
	}
      }
    };
    var enableHighlightChecked = function(element) {
      highlightChecked(element);
      ['change', 'suggest:change'].each(function(eventName) {
        element.observe(eventName, highlightChecked.bind(element,element));
      });
    };
    $$('label input[type=checkbox]').each(enableHighlightChecked);
    var suggestionsMapping = {
        "hpo" : {
            script: "/solr/select?start=0&rows=15&debugQuery=on&",
            queryProcessor: typeof(MS.widgets.SolrQueryProcessor) == "undefined" ? null : new MS.widgets.SolrQueryProcessor({
                           'name' : { 'stub': true, 'boost': 50 },
                           'synonym' : { 'stub': true, 'boost': 50 },
                           'text' : { 'stub': true, 'default': true },
                           'phonetic' : {'boost': 0.1 },
                           'id' : {'activationRegex' : 'HP:[0-9]+', 'stub': true, 'boost' : 50}
                         }),
            varname: "q",
            noresults: "No matching terms",
            json: false,
            resultsParameter : "doc",
            resultId : "str[name=id]",
            resultValue : "str[name=name]",
            resultInfo : {
                           "Definition"    : {"selector"  : "str[name=def]"},
                           "Synonyms"      : {"selector"  : "arr[name=synonym] str"},
                           "Is a"          : {"selector"  : "arr[name=is_a] str",
                                              "processor" : function (text){
                                                            return text.replace(/(HP:[0-9]+)\s*!\s*(.*)/, "[$1] $2");
                                                          },
                                              "collapsed" : true
                                             },
                           "Subcategories" : {"selector"  : "str[name=id]",
                                              "dynamic"   : true,
                                              "queryProcessor" : typeof(MS.widgets.SolrQueryProcessor) == "undefined" ? null : new MS.widgets.SolrQueryProcessor({
                                                                 'is_a' : { 'stub': false, 'activationRegex' : 'HP:[0-9]+' }
                                               }),
                                              "processor" : function (response) {
                                                              var suggestions = this.getSuggestionList(response);
                                                              for (var i = 0; i < suggestions.length; ++i) {
                                                                suggestions[i].info = "";
                                                                /*var info = suggestions[i].info;
                                                                Element.select(info, 'dt').each(function (elt){
                                                                  if (!elt.hasClassName('subcategories')) {
                                                                    elt.next('dd').remove();
                                                                    elt.remove();
                                                                  }
                                                                });*/
                                                              }
                                                              return this.createListElement(suggestions, this);
                                                            }
                                             }
                         },
            enableHierarchy: true,
            resultParent : "arr[name=is_a] str",
            fadeOnClear : false
        }
    };
    var pickerSpecialClassOptions = {
      'defaultPicker' : {},
      'generateCheckboxes' : {
                  'showKey' : false,
                  'showTooltip' : false,
                  'showDeleteTool' : true,
                  'enableSort' : false,
                  'showClearTool' : false,
                  'inputType': 'checkbox',
                  'listInsertionElt' : '.label-other-phenotype',
                  'listInsertionPosition' : 'before',
                  'onItemAdded' : enableHighlightChecked
                },
      'quickSearch' : {
                  'showKey' : false,
                  'showTooltip' : false,
                  'showDeleteTool' : true,
                  'enableSort' : false,
                  'showClearTool' : false,
                  'inputType': 'checkbox',
                  'listInsertionElt' : document.documentElement.down('.clinical-info .phenotype-group:last-child'),
                  'listInsertionPosition' : 'bottom',
                  'onItemAdded' : enableHighlightChecked
                }
    }
    if (typeof(MS.widgets.Suggest) != "undefined") {
      var keys = Object.keys(suggestionsMapping);
      var specialClasses = Object.keys(pickerSpecialClassOptions);
      for (var i = 0; i < keys.length; i++) {
        var selector = 'input.suggest-' + keys[i];
        $$(selector).each(function(item) {
          if (!item.hasClassName('initialized')) {
            var options = {
              timeout : 30000,
              parentContainer : null
            };
            Object.extend(options, suggestionsMapping[keys[i]]);
            // Create the Suggest.
            var suggest = new MS.widgets.Suggest(item, options);
            if (item.hasClassName('multi') && typeof(MS.widgets.SuggestPicker) != "undefined") {
              var multiSuggestOptions = {};
              for (var j = 0; j < specialClasses.length; j++) {
                if (item.hasClassName(specialClasses[j])) {
                  multiSuggestOptions = pickerSpecialClassOptions[specialClasses[j]];
                  break;
                }
              }
              var suggestPicker = new MS.widgets.SuggestPicker(item, suggest, multiSuggestOptions);
            }
            item.addClassName('initialized');
          }
        });
      }
    }
    
    var qsBox = $('quick-search-box');
    if (qsBox) {
      var content = qsBox.next('div');
      var qsInput = qsBox.down('input[type=text]');
      Event.observe(window, 'scroll', function(){
	var boxHeight = qsBox.getHeight();
	var boxWidth = qsBox.getWidth();
	var boxMinTop = content.cumulativeOffset().top ;
	var boxMaxTop = content.cumulativeOffset().top + content.getHeight() - boxHeight;
	var boxLeft = qsBox.cumulativeOffset().left;
	var qsSuggest = null;
	if (qsInput && qsInput._suggestWidget && typeof(qsInput._suggestWidget.suggest) != "undefined") {
	  qsSuggest = qsInput._suggestWidget.suggest;
	  qsSuggest.style.left = qsInput.cumulativeOffset().left;
	  qsSuggest.style.width = qsInput.getWidth();
	}
	if (document.viewport.getScrollOffsets().top >= boxMinTop && document.viewport.getScrollOffsets().top < boxMaxTop) {
	  qsBox.style.position = 'fixed';
	  qsBox.style.left = boxLeft + 'px';
	  qsBox.style.width = boxWidth + 'px';
	  qsBox.style.top = 0;
	  if (qsSuggest) {
	    qsSuggest.style.position = 'fixed';
	    qsSuggest.style.top = qsInput.viewportOffset().top + qsInput.getHeight();
	  }
	} else if (document.viewport.getScrollOffsets().top >= boxMaxTop) {
	  qsBox.style.position = 'absolute';
	  qsBox.style.top = boxMaxTop;
	  qsBox.style.left = '';//boxLeft + 'px';
	  qsBox.style.right = 0;
	  if (qsSuggest) {
	    qsSuggest.style.position = 'absolute';
	    qsSuggest.style.top = qsInput.cumulativeOffset().top + qsInput.getHeight();
	  }
	} else {
	  qsBox.style.position = '';
	  qsBox.style.top = '';
	  qsBox.style.left = '';
	  qsBox.style.width = '';
	  if (qsSuggest) {
	    qsSuggest.style.position = 'absolute';
	    qsSuggest.style.top = qsInput.cumulativeOffset().top + qsInput.getHeight();
	  }
	}
      });
    }
    $$('fieldset.phenotype-group legend').invoke('observe', 'click', function(event) {
      event.element().up('fieldset.phenotype-group').toggleClassName('collapsed');
    });
});
