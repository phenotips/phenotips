/**
 * ProbandDataLoader is responsible for retrieving data on the proband.
 *
 * @class ProbandDataLoader
 * @constructor
 */

define(["pedigree/model/helpers"], function(Helpers){
    var PatientDataLoader = Class.create( {
        initialize: function() {
        },

        load: function(patientList, dataProcessorWhenReady) {

            // check which patients have been already loaded and are available via editor.getPatientRecordData()
            var needToLoadList = editor.getPatientRecordData().getMissingPatientIDs(patientList);
            if (needToLoadList.length == 0) {
                this._onAllDataAvailable(patientList, dataProcessorWhenReady);
                return;
            }

            var _this = this;
            var patientDataJsonURL = editor.getExternalEndpoint().getLoadPatientDataJSONURL(needToLoadList);
            document.fire("pedigree:blockinteraction:start");
            new Ajax.Request(patientDataJsonURL, {
                method: "GET",
                onSuccess: function(response) {
                    if (response.responseJSON) {
                        console.log("Received patient data: " + Helpers.stringifyObject(response.responseJSON));

                        // store JSON as loaded from PT as the last know approved JSON for this patient
                        for (var patient in response.responseJSON) {
                            if (response.responseJSON.hasOwnProperty(patient)) {
                                editor.getPatientRecordData().update(patient, response.responseJSON[patient]);
                            }
                        }

                        _this._onAllDataAvailable(patientList, dataProcessorWhenReady);
                    } else {
                        console.log("[!] Error parsing patient data JSON");
                    }
                },
                onComplete: function() {
                    document.fire("pedigree:blockinteraction:finish");
                }
            });
        },

        _onAllDataAvailable: function(patientList, dataProcessorWhenReady) {
            var data = editor.getPatientRecordData().getAll(patientList);

            // update external ID information according to the latest loaded data
            patientList.forEach(function(patientRecordId) {
                var extID = data[patientRecordId].hasOwnProperty("external_id") ? data[patientRecordId].external_id : "";
                editor.getExternalIdManager().set(patientRecordId, extID);
            });

            dataProcessorWhenReady && dataProcessorWhenReady(data);
            document.fire("pedigree:blockinteraction:finish");
        }
    });

    return PatientDataLoader;
});