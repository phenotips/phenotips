/**
 * PatientRecordData is used to store latest version of Phenotips JSON for each patient record.
 *
 * The PT JSONs are only needed to initialize nodes after loading, and to generate the "saved"
 * pedigree, and is not used in any way during the pedigree operation (since PT JSONs may have
 * data not yet supported by the pedigree, so changes in pedigree-suported data need to be merged
 * with original PT JSONs to generate final PT JSONs which can be passed back to PT to update
 * patient records.
 *
 * Having a separeate class to store PT JSONs helps to separate them from data actively being modified,
 * and provides a storage for data not linked to any pedigree nodes (since on the model side of things
 * all other data is stored in the BaseGraph class, but BaseGraph was designed around the concept of a
 * pedigree node as a unit of data - thus family members not linked to a pedigree node have no placedo
 * to have their data preserved e.g. after a node was edited, but then unlinked - in which case while
 * unlinked, changes should still be saved in the corresponding patient record).
 *
 * @class PatientRecordData
 * @constructor
 */

define([], function(){
    var PatientRecordData = Class.create( {
        initialize: function() {
            this._patientRecordData = {};

            document.observe("pedigree:patient:deleted", this.handlePatientRecordDeleted.bind(this));
        },

        hasPatient: function(phenotipsPatientID) {
            return this._patientRecordData.hasOwnProperty(phenotipsPatientID);
        },

        get: function(phenotipsPatientID) {
            if (!this.hasPatient(phenotipsPatientID)) {
                console.log("[PatientRecordData] Requesting data for non-existent patient record " + phenotipsPatientID);
            }
            return this._patientRecordData[phenotipsPatientID];
        },

        getAll: function(patientList) {
            var result = {};
            patientList.forEach(function(patientID) {
                result[patientID] = this.get(patientID);
            }, this);
            return result;
        },

        getMissingPatientIDs: function(patientList) {
            var result = [];
            patientList.forEach(function(patientID) {
                if (!this.hasPatient(patientID)) {
                    result.push(patientID);
                }
            }, this);
            return result;
        },

        update: function(phenotipsPatientID, phenotipsJSON) {
            console.log("[PatientRecordData] Updated stored data for patient record " + phenotipsPatientID);
            this._patientRecordData[phenotipsPatientID] = phenotipsJSON;
        },

        handlePatientRecordDeleted: function(event)
        {
            delete this._patientRecordData[event.memo.phenotipsPatientID];
        }
    });

    return PatientRecordData;
});