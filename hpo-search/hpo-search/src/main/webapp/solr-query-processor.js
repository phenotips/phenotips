var MS = (function (MS) {
  var widgets = MS.widgets = MS.widgets || {};
  widgets.SolrQueryProcessor = Class.create({
  initialize: function(queryFields) {
    this.queryFields = queryFields;
  },

  processQuery: function(query)
  {
    var txt = query.strip();
    var result = "";

    for (var field in this.queryFields) {
      var fieldOptions = this.queryFields[field];
      var activationRegex = fieldOptions['activationRegex'];
      if (activationRegex) {
        if (!txt.match(new RegExp(activationRegex, "i"))) {
          continue;
        }
        if (activationRegex == activationRegex.toUpperCase()) {
          txt = txt.toUpperCase();
        } else if (activationRegex == activationRegex.toLowerCase()) {
          txt = txt.toLowerCase();
        }
        this.getQueryPieces(txt).each(function(word) {
          result += this.generateQueryForField(field, word, true);
        }.bind(this));
        // only one such field allowed; ignore all other fields
        if (result != "") {
          return result.strip();
        }
      }
    }

    this.getQueryPieces(txt).each(function(word) {
      for (var field in this.queryFields) {
        result += this.generateQueryForField(field, word, false);
      }
    }.bind(this));
    return result.strip();
  },

  getQueryPieces : function(txt) {
    return txt.replace(/[^a-zA-Z0-9 :]/g, ' ').strip().split(/ +/);
  },

  generateQueryForField : function (field, word, matchAll)
  {
    var result = "";
    var fieldOptions = this.queryFields[field];
    var suffixes = [""];
    if (fieldOptions['stub']) {
      suffixes.push("*");
    }
    word = word.replace(/:/g, "\\:");
    result += matchAll ? "+(" : "";
    suffixes.each(function(suffix) {
      if (!fieldOptions['default']) {
        result += field + ":";
      }
      result += word + suffix + (fieldOptions['boost'] ? "^" + fieldOptions['boost'] : "") + " ";
    });
    result += matchAll ? ") " : " ";
    return result;
  }
});
  return MS;
}(MS || {}));
