document.observe('dom:loaded', function() {
    // hpo: namespace:medical_genetics
    // go : namespace:
    var highlightChecked = function(element) {
      if (element.checked) {
        element.up('label').addClassName('selected');
      } else {
        element.up('label').removeClassName('selected');
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
                           "Definition" : {"selector"  : "str[name=def]"},
                           "Synonyms"   : {"selector"  : "arr[name=synonym] str"},
                           "Is a"       : {"selector"  : "arr[name=is_a] str",
                                           "processor" : function (text){
                                                         return text.replace(/(HP:[0-9]+)\s*!\s*(.*)/, "[$1] $2");
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
                  'showDeleteTool' : false,
                  'enableSort' : false,
                  'showClearTool' : false,
                  'inputType': 'checkbox',
                  'listInsertionEltSelector' : '.label-other-phenotype',
                  'listInsertionPosition' : 'before',
                  'onItemAdded' : enableHighlightChecked
                },
      'quickSearch' : {
                  'showKey' : false,
                  'showTooltip' : false,
                  'showDeleteTool' : false,
                  'enableSort' : false,
                  'showClearTool' : false,
                  'inputType': 'checkbox',
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
              parentContainer : item.up()
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
});
