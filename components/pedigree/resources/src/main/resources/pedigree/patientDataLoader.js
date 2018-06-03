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
            if (patientList.length == 0) {
                dataProcessorWhenReady({});
                document.fire("pedigree:blockinteraction:finish");
                return;
            }
            var _this = this;
            var patientDataJsonURL = editor.getExternalEndpoint().getLoadPatientDataJSONURL(patientList);
            document.fire("pedigree:blockinteraction:start");
            new Ajax.Request(patientDataJsonURL, {
                method: "GET",
                onSuccess: function(response) {
                    if (response.responseJSON) {
                        console.log("Received patient data: " + Helpers.stringifyObject(response.responseJSON));

                        // store JSON as loaded from PT as the last know aproved JSON for this patient
                        for (var patient in response.responseJSON) {
                            if (response.responseJSON.hasOwnProperty(patient)) {
                                editor.getPatientRecordData().update(patient, response.responseJSON[patient]);
                            }
                        }

                        dataProcessorWhenReady && dataProcessorWhenReady(response.responseJSON);
                    } else {
                        console.log("[!] Error parsing patient data JSON");
                    }
                },
                onComplete: function() {
                    document.fire("pedigree:blockinteraction:finish");
                }
            });
        }
    });

    return PatientDataLoader;
});