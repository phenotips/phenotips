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
        return this.getFamilyInterfaceURL();
    },

    getFamilyInterfaceURL: function() {
        return new XWiki.Document('FamilyPedigreeInterface', 'PhenoTips').getURL('get', 'rand='+ Math.random());
    },
});