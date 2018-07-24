/**
 * A class designed to keep track of all external IDs assigned to all patient records in the pedigree (but not pedigree nodes).
 *
 * The class is also used as a cache of known external IDs, to avoid constant calls to the back-end if e.g. all IDs start with the
 * same prefix - the manager will know after the first query that the prefix (as wel las all partially completed versions of it)
 * is valid or invalid and will be able to report that instantly.
 *
 * @class ExternalIdManager
 * @constructor
 */

define([
        "pedigree/model/helpers"
    ], function(
        Helpers
    ){
    var ExternalIdManager = Class.create( {
        initialize: function() {
            this._EXTERNAL_PATIENT = "UNKNOWN_EXTERNAL_PATIENT_RECORD";

            this.externalIDs = {};        // externalID -> [list of PhenotipsPatientIDs, including possibly IDs not in the pedigree]
            this.patientRecordToID = {};  // phenotipsID -> externalID
            this.externallyAbsentIDs = {};

            document.observe("pedigree:patient:deleted", this._handlePatientRecordRemoved.bind(this));
            document.observe("pedigree:patient:removedfromfamily", this._handlePatientRecordRemoved.bind(this));
        },

        set: function(phenotipsID, externalID)
        {
            if (this.patientRecordToID.hasOwnProperty(phenotipsID)) {
                // this patient record is already known: supposedly we know the externalID that this patient had before
                if (this.patientRecordToID[phenotipsID] == externalID) {
                    // new ID == old ID, nothing to do
                    return;
                }
                this.remove(phenotipsID);
            } else {
                // a new patient is added to the pedigree: assume it was the "other" record with this external ID
                if (this.externalIDs.hasOwnProperty(externalID)) {
                    Helpers.removeFirstOccurrenceByValue(this.externalIDs[externalID], this._EXTERNAL_PATIENT);
                }
            }
            if (externalID && externalID.length > 0) {
                // update all the data structures
                this._add(externalID, phenotipsID);
                this.patientRecordToID[phenotipsID] = externalID;
            }
        },

        remove: function(phenotipsID, removedFromPedigree)
        {
            if (this.patientRecordToID.hasOwnProperty(phenotipsID)) {
                var oldID = this.patientRecordToID[phenotipsID];
                Helpers.removeFirstOccurrenceByValue(this.externalIDs[oldID], phenotipsID);
                if (this.externalIDs[oldID].length == 0) {
                    delete this.externalIDs[oldID];
                }
                delete this.patientRecordToID[phenotipsID];

                // when a patient is removed from the pedigree, its external ID may now be present outside of pedigree
                if (removedFromPedigree) {
                    Helpers.removeFirstOccurrenceByValue(this.externallyAbsentIDs, oldID);
                }
            }
        },

        isUniqueID: function(phenotipsID, candidateID, callbackOnYes, callbackOnNo)
        {
            if (candidateID.length == 0) {
                return true;
            }

            if (this.externalIDs.hasOwnProperty(candidateID)) {
                var recordsWithThisID = this.externalIDs[candidateID];
                if (recordsWithThisID.length > 1) {
                    callbackOnNo();
                    return;
                }
                if (recordsWithThisID.length == 1 && recordsWithThisID[0] != phenotipsID) {
                    callbackOnNo();
                    return;
                }
                callbackOnYes();
                return;
            }

            if (this.externallyAbsentIDs.hasOwnProperty(candidateID)) {
                callbackOnYes();
                return;
            }

            var onInvalid = function() {
                // add it to the list of known taken IDs to avoid sending the same request again
                this._add(candidateID, this._EXTERNAL_PATIENT);
                callbackOnNo();
            }.bind(this);

            var onValid = function() {
                this.externallyAbsentIDs[candidateID] = true;
                callbackOnYes();
            }.bind(this);

            this._checkExternalIDinPhenotips(phenotipsID, candidateID, onValid, onInvalid);
        },

        duplicateIDPresent: function()
        {
            for(var id in this.externalIDs) {
                if (this.externalIDs.hasOwnProperty(id)) {
                    if (this.externalIDs[id].length > 1) {
                        var involvesExternalPatient = false;
                        if (Helpers.arrayContains(this.externalIDs[id], this._EXTERNAL_PATIENT)) {
                            involvesExternalPatient = true;
                        }
                        return { "dupliucateIDPresent": true, "involvesExternalPatient": involvesExternalPatient, "id": id};
                    }
                }
            }
            return { "dupliucateIDPresent": false }
        },

        _add: function(externalID, recordID)
        {
            if (!this.externalIDs.hasOwnProperty(externalID)) {
                this.externalIDs[externalID] = [];
            }
            this.externalIDs[externalID].push(recordID);

            // just in case, since there can be  ascenaio where ID is changed, patient is saved,
            // then id is changed again and patient is removed from the pedigree - the manager
            // will forget about the original ID, but it will be present outside of pedigree
            Helpers.removeFirstOccurrenceByValue(this.externallyAbsentIDs, externalID);
        },

        _checkExternalIDinPhenotips: function(patientID, externalID, onValid, onInvalid) {
            var serviceUrl = editor.getExternalEndpoint().getPatientExternalIDValidationURL();
            new Ajax.Request(serviceUrl, {
              parameters : { outputSyntax: 'plain', eid: externalID, id: patientID, entity : "patients"},
              on200 : onValid,
              on403 : onInvalid,
              on404 : onValid,
              on409 : onInvalid
            });
        },

        _handlePatientRecordRemoved: function(event)
        {
            this.remove(event.memo.phenotipsPatientID, true);
        }
    });

    return ExternalIdManager;
});
