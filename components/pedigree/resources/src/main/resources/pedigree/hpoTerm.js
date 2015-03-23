/*
 * HPOTerm is a class for storing phenotype information and loading it from the
 * the HPO database. These phenotypes can be attributed to an individual in the Pedigree.
 *
 * @param hpoID the id number for the HPO term, taken from the HPO database
 * @param name a string representing the name of the term e.g. "Abnormality of the eye"
 */

var HPOTerm = Class.create( {

    initialize: function(hpoID, name, callWhenReady) {
        // user-defined terms
        if (name == null && !HPOTerm.isValidID(HPOTerm.desanitizeID(hpoID))) {
            name = HPOTerm.desanitizeID(hpoID);
        }

        this._hpoID  = HPOTerm.sanitizeID(hpoID);
        this._name   = name ? name : "loading...";

        if (!name && callWhenReady)
            this.load(callWhenReady);
    },

    /*
     * Returns the hpoID of the phenotype
     */
    getID: function() {
        return this._hpoID;
    },

    /*
     * Returns the name of the term
     */
    getName: function() {
        return this._name;
    },

    load: function(callWhenReady) {
        var baseServiceURL = HPOTerm.getServiceURL();
        var queryURL       = baseServiceURL + "&q=" + HPOTerm.desanitizeID(this._hpoID).replace(":","%3A");
        //console.log("QueryURL: " + queryURL);
        new Ajax.Request(queryURL, {
            method: "GET",
            onSuccess: this.onDataReady.bind(this),
            //onComplete: complete.bind(this)
            onComplete: callWhenReady ? callWhenReady : {}
        });
    },

    onDataReady : function(response) {
        try {
            var parsed = JSON.parse(response.responseText);
            //console.log(stringifyObject(parsed));
            console.log("LOADED HPO TERM: id = " + HPOTerm.desanitizeID(this._hpoID) + ", name = " + parsed.rows[0].name);
            this._name = parsed.rows[0].name;
        } catch (err) {
            console.log("[LOAD HPO TERM] Error: " +  err);
        }
    }
});

/*
 * IDs are used as part of HTML IDs in the Legend box, which breaks when IDs contain some non-alphanumeric symbols.
 * For that purpose these symbols in IDs are converted in memory (but not in the stored pedigree) to some underscores.
 */
HPOTerm.sanitizeID = function(id) {
    var temp = id.replace(/[\(\[]/g, '_L_');
    temp = temp.replace(/[\)\]]/g, '_J_');
    temp = temp.replace(/[:]/g, '_C_');
    return temp.replace(/[^a-zA-Z0-9,;_\-*]/g, '__');
}

HPOTerm.desanitizeID = function(id) {
    var temp = id.replace(/__/g, " ");
    temp = temp.replace(/_C_/g, ":");
    temp = temp.replace(/_L_/g, "(");
    return temp.replace(/_J_/g, ")");
}

HPOTerm.isValidID = function(id) {
    var pattern = /^HP\:(\d)+$/i;
    return pattern.test(id);
}

HPOTerm.getServiceURL = function() {
    return new XWiki.Document('SolrService', 'PhenoTips').getURL("get") + "?";
}
