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

            this.ethnicitySearch = new XWiki.Document('EthnicitySearch', 'PhenoTips');

            this.geneNameService = new XWiki.Document('GeneNameService', 'PhenoTips');

            this.patientSuggestService = new XWiki.Document('SuggestPatientsService', 'PhenoTips');

            this.omimService = new XWiki.Document('OmimService', 'PhenoTips');

            this.solrService = new XWiki.Document('SolrService', 'PhenoTips');

            this.pedigreeInterface = new XWiki.Document('PedigreeInterface', 'PhenoTips');

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
        },

        getEthnicitySearchURL: function() {
            return this.ethnicitySearch.getURL("get", 'outputSyntax=plain&rand='+ Math.random());
        },

        getGeneNameServiceURL: function() {
            return this.geneNameService.getURL("get", 'outputSyntax=plain&rand='+ Math.random());
        },

        getPatientSuggestServiceURL: function() {
            return this.patientSuggestService.getURL("get", 'outputSyntax=plain&rand='+ Math.random());
        },

        getPhenotipsPatientURL: function(patientId) {
            return new XWiki.Document(patientId, 'data').getURL();
        },

        getPedigreeTemplatesURL: function() {
            return new XWiki.Document('WebHome', 'data').getRestURL('objects/PhenoTips.PedigreeClass/');
        },

        getOMIMServiceURL: function() {
            return this.omimService.getURL("get", 'outputSyntax=plain&rand='+ Math.random());
        },

        getSolrServiceURL: function() {
            return this.solrService.getURL("get");
        },

        getPedigreePreferencesURL: function() {
            return this.pedigreeInterface.getURL('get', 'action=getPreferences');
        },
    });
    return ExternalEndpointsManager;
});