/**
 * ProbandDataLoader is responsible for retrieving data on the proband.
 *
 * @class ProbandDataLoader
 * @constructor
 */

define(["pedigree/model/helpers"], function(Helpers){
    var FamilyDataLoader = Class.create( {
        initialize: function() {
            this.familyPage = null;
            this.familyMembers = [];
            this.familyMembersIndex = {};
            this.warningMessage = "";
        },

        load: function(callWhenReady) {
            var probandID = editor.getGraph().getCurrentPatientId();
            var familyJsonURL = editor.getExternalEndpoint().getFamilyInfoURL();
            new Ajax.Request(familyJsonURL, {
                method: "POST",
                onSuccess: this._onFamilyDataReady.bind(this),
                onComplete: callWhenReady ? callWhenReady : {},
                parameters: {"proband": probandID, "family_status": true }
            });
        },

        _onFamilyDataReady: function(response) {
            if (response.responseJSON) {
                this.familyPage = (response.responseJSON.hasOwnProperty("familyPage") && response.responseJSON.familyPage) ?
                                  response.responseJSON.familyPage : null;

                this.familyMembers = response.responseJSON.hasOwnProperty("familyMembers") ?
                                     response.responseJSON.familyMembers: [];

                for (var i = 0; i < this.familyMembers.length; i++) {
                    this.familyMembersIndex[this.familyMembers[i].id] = i;
                }

                this.warningMessage = response.responseJSON.hasOwnProperty("warning") ? response.responseJSON.warning : "";
                /* Will display a warning if there is a warning message */
                if (this.hasWarningMessage()) {
                    editor.getOkCancelDialogue().showCustomized(this.warningMessage,"Attention: This pedigree contains sensitive information.", "OK", null);
                }
            } else {
                console.log("[!] Error parsing family JSON");
            }
            console.log("Family data:  [familyPage: " + this.familyPage +
                                   "], [isFamily: " + Helpers.stringifyObject(this.isFamily()) + "], [Members:" +
                                   Helpers.stringifyObject(this.familyMembers) + "]");
        },

        isFamily: function() {
            return (this.familyPage == editor.getGraph().getCurrentPatientId());
        },

        hasFamily: function() {
            return (this.familyPage != null);
        },

        hasWarningMessage: function() {
            return (this.warningMessage != "");
        },

        getAllFamilyMembersList: function() {
            return this.familyMembers;
        },

        getWarningMesage: function() {
            return this.warningMessage;
        },

        isFamilyMember: function(patientID) {
            return this.familyMembersIndex.hasOwnProperty(patientID);
        },

        getFamilyMemberByPatyientID: function(patientID) {
            if (!this.isFamilyMember(patientID)) {
                return null;
            }
            return this.familyMembers[this.familyMembersIndex[patientID]];
        },

        getPatientAccessPermissions: function(patientID) {
            var familyhMemberData = this.getFamilyMemberByPatyientID(patientID);
            if (familyhMemberData == null || !familyhMemberData.hasOwnProperty("permissions")) {
                return null;
            }
            return familyhMemberData.permissions;
        }
    });

    return FamilyDataLoader;
});
