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
            // TODO: cache new XWiki.Document('ExportPatient', 'PhenoTips')? Speed vs memory usage?

            // TODO: IE caches AJAX requests, so adding a random part to "load" URL to break that cache;
            //       investigate if this is still the case with new caching policy in phenotips
        },

        getLoadPatientDataJSONURL: function(patientList) {
            var pList = "";
            for (var i = 0; i < patientList.length; i++) {
              if (i != 0) {
                  pList += ",";
              }
              pList += patientList[i];
            }
            return new XWiki.Document('ExportMultiplePatients', 'PhenoTips').getURL('get', 'idlist='+pList) +
                "&rand=" + Math.random();
        },

        getLoadPatientPedigreeJSONURL: function(patientID) {
            return new XWiki.Document('ExportPatient', 'PhenoTips').getURL('get', 'id='+patientID)
                + "&data=pedigree"
                + "&rand=" + Math.random();
        },

        getSavePedigreeURL: function() {
            return this._getBaseFamilyInterfaceURL() + "&action=save";
        },

        getFamilyInfoURL: function() {
            return this._getBaseFamilyInterfaceURL() + "&action=familystatus";
        },

        getFamilyCheckLinkURL: function() {
            return this._getBaseFamilyInterfaceURL() + "&action=checklink";
        },

        getFamilyNewPatientURL: function() {
            return this._getBaseFamilyInterfaceURL() + "&action=createpatient";
        },
        
        //getFamilyEditURL: function(familyID) {
        //	 return new XWiki.Document(familyID, 'Families').getURL('edit', 'sheet=PhenoTips.PedigreeEditor');
        //},
        
        getFamilySearchURL: function() {
            return new XWiki.Document('FamilySearch', 'PhenoTips').getURL('get', 'outputSyntax=plain');
        },

        _getBaseFamilyInterfaceURL: function() {
            return new XWiki.Document('FamilyPedigreeInterface', 'PhenoTips').getURL('get', 'rand='+ Math.random());
        }
    });
    return ExternalEndpointsManager;
});