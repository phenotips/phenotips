/**
 * ExternalEndpointsManager provides various URLs for family-services.
 *
 * @class ExternalEndpointsManager
 * @constructor
 */

define([
    ], function(
    ){
    var ExternalEndpointsManager = Class.create({
        initialize: function() {
            this.exportMultiplePatients = new XWiki.Document('ExportMultiplePatients', 'PhenoTips');

            this.familyPedigreeInterface = new XWiki.Document('FamilyPedigreeInterface', 'PhenoTips');

            this.familySearch = new XWiki.Document('FamilySearch', 'PhenoTips');

            // TODO: IE caches AJAX requests, so adding a random part to "load" URL to break that cache;
            //       investigate if this is still the case with new caching policy in PhenoTips
        },

        getLoadPatientDataJSONURL: function(patientList) {
            var pList = "";
            for (var i = 0; i < patientList.length; i++) {
              if (i != 0) {
                  pList += ",";
              }
              pList += patientList[i];
            }
            return this.exportMultiplePatients.getURL('get', 'idlist='+pList) + "&rand=" + Math.random();
        },

        getFamilySearchURL: function() {
            return this.familySearch.getURL('get', 'outputSyntax=plain&rand='+ Math.random());
        },

        getSavePedigreeURL: function() {
            return this._getBaseFamilyInterfaceURL() + "&action=save";
        },

        getFamilyInfoURL: function() {
            return this._getBaseFamilyInterfaceURL() + "&action=familyinfo";
        },

        getFamilyCheckLinkURL: function() {
            return this._getBaseFamilyInterfaceURL() + "&action=checklink";
        },

        getFamilyNewPatientURL: function() {
            return this._getBaseFamilyInterfaceURL() + "&action=createpatient";
        },

        _getBaseFamilyInterfaceURL: function() {
            return this.familyPedigreeInterface.getURL('get', 'rand='+ Math.random());
        }
    });
    return ExternalEndpointsManager;
});