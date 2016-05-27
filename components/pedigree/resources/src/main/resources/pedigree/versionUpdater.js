/*
 * VersionUpdater is responsible for updating pedigree JSON represenatation to the current version.
 */
define([], function(){
    VersionUpdater = Class.create( {
        initialize: function() {
            this.availableUpdates = [ { "comment":    "group node comment representation",
                                        "introduced": "May2014",
                                        "func":       "updateGroupNodeComments"},
                                      { "comment":    "adopted status",
                                        "introduced": "Nov2014",
                                        "func":       "updateAdoptedStatus"},
                                      { "comment":    "id desanitation",
                                        "introduced": "Mar2015",
                                        "func":       "updateId"},
                                      { "comment":    "proband link",
                                        "introduced": "Nov2015",
                                        "func":       "updateNode0ProbandLink"},
                                      { "comment":    "version info",
                                        "introduced": "Nov2015",
                                        "func":       "updateJSONVersionInfo"}
                                    ];
        },

        updateToCurrentVersion: function(pedigreeJSON) {
            for (var i = 0; i < this.availableUpdates.length; i++) {
                var update = this.availableUpdates[i];

                var updateResult = this[update.func](pedigreeJSON);

                if (updateResult !== null) {
                    console.log("[update #" + i + "] [updating to " + update.introduced + " version] - performing " + update.comment + " update");
                    pedigreeJSON = updateResult;
                }
            }

            return pedigreeJSON;
        },

        /* - assumes input is in the pre-May-2014 format
         * - returns null if there were no changes; returns new JSON if there was a change
         */
        updateGroupNodeComments: function(pedigreeJSON) {

            var data = JSON.parse(pedigreeJSON);
            if (data.hasOwnProperty("JSON_version")) {
                // any pedigree which has a JSON_version is known to have the updated data
                return null;
            }
            var change = false;
            for (var i = 0; i < data.GG.length; i++) {
                var node = data.GG[i];

                if (node.hasOwnProperty("prop")) {
                    if (node.prop.hasOwnProperty("numPersons") && !node.prop.hasOwnProperty("comments") && node.prop.hasOwnProperty("fName") && node.prop.hasOwnProperty("fName") != "") {
                        node.prop["comments"] = node.prop.fName;
                        delete node.prop.fName;
                        change = true;
                    }
                }
            }

            if (!change) {
                return null;
            }

            return JSON.stringify(data);
        },

        /* - assumes input is in the pre-Nov-2014 format
         * - returns null if there were no changes; returns new JSON if there was a change
         */
        updateAdoptedStatus: function(pedigreeJSON) {
            var data = JSON.parse(pedigreeJSON);
            if (data.hasOwnProperty("JSON_version")) {
                // any pedigree which has a JSON_version is known to have the updated data
                return null;
            }
            var change = false;
            for (var i = 0; i < data.GG.length; i++) {
                var node = data.GG[i];

                if (node.hasOwnProperty("prop")) {
                    if (node.prop.hasOwnProperty("isAdopted") ) {
                        if (node.prop.isAdopted) {
                            node.prop["adoptedStatus"] = "adoptedIn";
                        }
                        delete node.prop.isAdopted;
                        change = true;
                    }
                }
            }

            if (!change) {
                return null;
            }

            return JSON.stringify(data);
        },
        /* - assumes input is in the pre-Mar-2015 format
         * - returns null if there were no changes; returns new JSON if there was a change
         */
        updateId: function(pedigreeJSON) {
            var data = JSON.parse(pedigreeJSON);
            if (data.hasOwnProperty("JSON_version")) {
                // any pedigree which has a JSON_version is known to have the updated data
                return null;
            }
            var change = false;
            for (var i = 0; i < data.GG.length; i++) {
                var node = data.GG[i];

                if (node.hasOwnProperty("prop")) {
                    if (node.prop.hasOwnProperty("disorders") ) {
                      for (var j = 0 ; j < node.prop.disorders.length; j++) {
                        node.prop.disorders[j] = desanitizeId(node.prop.disorders[j]);
                        change = true;
                      }
                    }
                    if (node.prop.hasOwnProperty("hpoTerms") ) {
                      for (var j = 0 ; j < node.prop.hpoTerms.length; j++) {
                        node.prop.hpoTerms[j] = desanitizeId(node.prop.hpoTerms[j]);
                        change = true;
                      }
                    }
                    if (node.prop.hasOwnProperty("candidateGenes") ) {
                      for (var j = 0 ; j < node.prop.candidateGenes.length; j++) {
                        node.prop.candidateGenes[j] = desanitizeId(node.prop.candidateGenes[j]);
                        change = true;
                      }
                    }
                }
            }

            if (!change) {
                return null;
            }

            return JSON.stringify(data);

            function desanitizeId(id){
              var temp = id.replace(/__/g, " ");
              temp = temp.replace(/_C_/g, ":");
              temp = temp.replace(/_L_/g, "(");
              return temp.replace(/_J_/g, ")");
            }
        },

        /* - assumes input is in the pre-Nov-2015 format
         * - returns null if there were no changes; returns new JSON if there was a change
         */
        updateNode0ProbandLink: function(pedigreeJSON) {
            // check if at least one node is linked to the current patient.
            // Iff none are, assumenode 0 is the proband and link it to the patient

            var data = JSON.parse(pedigreeJSON);
            if (data.hasOwnProperty("JSON_version")) {
                // any pedigree which has a JSON_version is known to have the updated data
                return null;
            }

            if (editor.isFamilyPage()) {
                if (data.hasOwnProperty("probandNodeID")) {
                    return null;
                }
                data["probandNodeID"] = 0;
                return JSON.stringify(data);
            }

            var currentPatient = XWiki.currentDocument.page;

            /*
            //look through all person nodes for a node linked to the current patient
            for (var i = 0; i < data.GG.length; i++) {
                var node = data.GG[i];

                if (node.hasOwnProperty("prop")) {
                    if (node.prop.hasOwnProperty("phenotipsId") ) {
                        if (node.prop.phenotipsId == currentPatient) {

                            // if there is no proband make this node the proband
                            // if there is proband, we are done
                            if (!data.hasOwnProperty("probandNodeID")) {
                                data["probandNodeID"] = i;
                                return JSON.stringify(data);
                            }
                            else {
                                return null;
                            }
                        }
                    }
                }
            }*/

            // no nodes are linked to the current patient. Either link the proband node or node 0 if no
            // proband is defined
            if (!data.hasOwnProperty("probandNodeID")) {
                var probandID = 0;
                data["probandNodeID"] = 0;
            } else {
                var probandID = data["probandNodeID"];
            }
            /*
            // assign node with id = 0 to be the proband
            for (var i = 0; i < data.GG.length; i++) {
                var node = data.GG[i];

                if (node.id == probandID) {
                    if (!node.hasOwnProperty("prop")) {
                        node.prop = {};
                    }
                    if (node.prop.hasOwnProperty("phenotipsId")) {
                        alert("Loaded pedigree is inconsistent - assumed proband node is linked to a different patient");
                        return null;
                    } else {
                        node.prop.phenotipsId = currentPatient;
                        break;
                    }
                }
            }*/
            return JSON.stringify(data);
        },

        updateJSONVersionInfo: function(pedigreeJSON) {
            var data = JSON.parse(pedigreeJSON);
            if (data.hasOwnProperty("JSON_version")) {
                return null;
            }
            // all updates ran before this point should bring JSON to version "1.0", which
            // will be recorded here
            data["JSON_version"] = "1.0";
            return JSON.stringify(data);
        }
    });
    return VersionUpdater;
});
