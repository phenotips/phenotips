/**
 * ProbandDataLoader is responsible for retrieving data on the proband.
 *
 * @class ProbandDataLoader
 * @constructor
 */

define(["pedigree/model/helpers"], function(Helpers){
    var ProbandDataLoader = Class.create( {
        initialize: function() {
            this.probandData = {};
        },

        load: function(callWhenReady) {
            var probandID = XWiki.currentDocument.page;
            var patientJsonURL = new XWiki.Document('ExportPatient', 'PhenoTips').getURL('get', 'id='+probandID);
            // IE caches AJAX requests, use a random URL to break that cache (TODO: investigate)
            patientJsonURL += "&rand=" + Math.random();
            new Ajax.Request(patientJsonURL, {
                method: "GET",
                onSuccess: this.onProbandDataReady.bind(this),
                onComplete: callWhenReady ? callWhenReady : {}
            });
        },

        onProbandDataReady : function(response) {
            if (response.responseJSON) {
                this.probandData = response.responseJSON;
            } else {
                console.log("[!] Error parsing patient JSON");
            }
            console.log("Proband data: " + Helpers.stringifyObject(this.probandData));
        },
    });
    return ProbandDataLoader;
});