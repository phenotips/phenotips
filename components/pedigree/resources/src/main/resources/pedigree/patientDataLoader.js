/**
 * ProbandDataLoader is responsible for retrieving data on the proband.
 *
 * @class ProbandDataLoader
 * @constructor
 */

define(["pedigree/model/helpers"], function(Helpers){
    var PatientDataLoader = Class.create( {
        initialize: function() {
            this._patientData = {};    // ...as last loaded from PhenoTips

            document.observe("pedigree:patient:deleted", this.handlePatientDeleted.bind(this));
        },

        load: function(patientList, dataProcessorWhenReady) {
            if (patientList.length == 0) {
                dataProcessorWhenReady(this._patientData);
                document.fire("pedigree:blockinteraction:finish");
                return;
            }
            var _this = this;
            var patientDataJsonURL = editor.getExternalEndpoint().getLoadPatientDataJSONURL(patientList);
            document.fire("pedigree:blockinteraction:start");
            new Ajax.Request(patientDataJsonURL, {
                method: "GET",
                onSuccess: this._onPatientDataReady.bind(this),
                onComplete: function() {
                    dataProcessorWhenReady && dataProcessorWhenReady(_this._patientData);
                    document.fire("pedigree:blockinteraction:finish");
                }
            });
        },

        _onPatientDataReady : function(response) {
            if (response.responseJSON) {
                console.log("Received patient data: " + Helpers.stringifyObject(response.responseJSON));
                this._patientData = response.responseJSON;
            } else {
                console.log("[!] Error parsing patient data JSON");
            }
        },

        handlePatientDeleted: function(event)
        {
            delete this._patientData[event.memo.phenotipsPatientID];
        }
    });

    return PatientDataLoader;
});