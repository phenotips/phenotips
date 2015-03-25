var ExternalEndpointsManager = Class.create({
    initialize: function() {
        // TODO: cache new XWiki.Document('ExportPatient', 'PhenoTips')? Speed vs memory usage?
        
        // TODO: IE caches AJAX requests, so adding a random part to "load" URL to break that cache;
        //       investigate if this is still the case with new caching policy in phenotips
    },
    
    getLoadPatientDataJSONURL: function(patientID) {
        return new XWiki.Document('ExportPatient', 'PhenoTips').getURL('get', 'id='+patientID) +
            "&rand=" + Math.random();
    },

    getLoadPatientPedigreeJSONURL: function(patientID) {
        return new XWiki.Document('ExportPatient', 'PhenoTips').getURL('get', 'id='+patientID)
            + "&data=pedigree"
            + "&rand=" + Math.random();        
    },

    getSavePedigreeURL: function() {
        return this.getFamilyInterfaceURL();
    },

    getFamilyInterfaceURL: function() {
        return new XWiki.Document('FamilyPedigreeInterface', 'PhenoTips').getURL('get');
    },
});