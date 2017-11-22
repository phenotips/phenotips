/**
 * ExternalEndpointsManager provides various URLs for family-services and abstracts away
 * XWiki-specific URLs and initialization parameters.
 *
 * @class ExternalEndpointsManager
 * @constructor
 */

define([
    ], function(
    ){
    var ExternalEndpointsManager = Class.create({
        initialize: function() {
            // parent document is passed to the pedigree editor via URL parameters
            // - patient_id - when an existing pedigree is opened from the patient page
            // - new_patient_id - when a new patient is added to an existing pedigree
            var patientId = window.self.location.href.toQueryParams().patient_id || window.self.location.href.toQueryParams().new_patient_id;
            this._parentDocumentID = patientId || XWiki.currentDocument.page;

            // normally pedigree editor runs in Family space, however when a pedigree is created for a patient with no family, it
            // initially opens in patient space ("data") and patient_id is not passed in the URL as it is redundant in that case
            this._parentDocumentType = (patientId || XWiki.currentSpace == "data") ? "Patient" : "Family";
            this._parentDocumentAction = window.self.location.href.toQueryParams().action || (XWiki.contextaction ? XWiki.contextaction : "view");

            this.exportMultiplePatients = new XWiki.Document('ExportMultiplePatients', 'PhenoTips');

            this.familyPedigreeInterface = new XWiki.Document('FamilyPedigreeInterface', 'PhenoTips');

            this.familySearch = new XWiki.Document('FamilySearch', 'PhenoTips');

            this.patientSuggestService = new XWiki.Document('SuggestPatientsService', 'PhenoTips');

            this.vocabulariesService = XWiki.contextPath + "/rest/vocabularies";

            this.pedigreeInterface = new XWiki.Document('PedigreeInterface', 'PhenoTips');

            this.pedigreeImageService = new XWiki.Document('PedigreeImageService', 'PhenoTips');

            // TODO: IE caches AJAX requests, so adding a random part to "load" URL to break that cache;
            //       investigate if this is still the case with new caching policy in PhenoTips
        },

        getParentDocument: function() {
            var returnURL = (this._parentDocumentType == "Patient") ?
                this.getPhenotipsPatientURL(this._parentDocumentID, this._parentDocumentAction) :
                XWiki.currentDocument.getURL(this._parentDocumentAction);

            return {
                "id": this._parentDocumentID,
                "type": this._parentDocumentType,
                "returnURL": returnURL
            }
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

        getFamilyRemoveURL: function() {
            return this._getBaseFamilyInterfaceURL() + "&action=removefamily";
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
            return this.vocabulariesService + "/ethnicity";
        },

        getHGNCServiceURL: function() {
            return this.vocabulariesService + "/hgnc";
        },

        getHPOServiceURL: function() {
            return this.vocabulariesService + "/hpo";
        },

        getOMIMServiceURL: function() {
            return this.vocabulariesService + "/omim";
        },

        getPatientDeleteURL: function(patientId) {
            return XWiki.contextPath + "/rest/patients/" + patientId + "?method=DELETE";
        },

        getPatientSuggestServiceURL: function() {
            return this.patientSuggestService.getURL("get", 'outputSyntax=plain&rand='+ Math.random());
        },

        // contextaction is optional parameter
        getPhenotipsPatientURL: function(patientId, contextaction) {
            return new XWiki.Document(patientId, 'data').getURL(contextaction);
        },

        getPhenotipsPatientHTMLLink: function(patientId) {
            return "<a href='" + this.getPhenotipsPatientURL(patientId) + "'>" + patientId + "</a>";
        },

        // contextaction is optional parameter
        getPhenotipsFamilyURL: function(familyId, contextaction) {
            return new XWiki.Document(familyId, 'Families').getURL(contextaction);
        },

        getPedigreeEditorURL: function(familyId, useCurrentReturnAction) {
             var familyHref = this.getPhenotipsFamilyURL(familyId);
             familyHref += '?sheet=PhenoTips.PedigreeEditor';
             if (useCurrentReturnAction) {
                 familyHref += "&action=" + this._parentDocumentAction;
             }
             return familyHref;
        },

        getPedigreeTemplatesURL: function() {
            return new XWiki.Document('WebHome', 'data').getRestURL('objects/PhenoTips.PedigreeClass/');
        },

        getPedigreePreferencesURL: function() {
            return this.pedigreeInterface.getURL('get', 'action=getPreferences');
        },

        getPedigreeImageExportServiceURL: function() {
            return this.pedigreeImageService.getURL('get','action=export&force-download=1');
        },

        redirectToURL: function(targetURL){
            window.self.location = XWiki.currentDocument.getURL('cancel', 'xredirect=' + encodeURIComponent(targetURL));
        }
    });
    return ExternalEndpointsManager;
});