document.observe('dom:loaded', function() {
    // hpo: namespace:medical_genetics
    // go : namespace:
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
            resultInfo : {"Definition" : "str[name=def]", "Synonyms" : "arr[name=synonym] str"},
            fadeOnClear : false
        }
    };
    if (typeof(MS.widgets.Suggest) != "undefined") {
      var keys = Object.keys(suggestionsMapping);
      for (var i=0;i<keys.length;i++) {
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
              var suggestPicker = new MS.widgets.SuggestPicker(item, suggest);
            }
            item.addClassName('initialized');
          }
        });
      }
    }
});
