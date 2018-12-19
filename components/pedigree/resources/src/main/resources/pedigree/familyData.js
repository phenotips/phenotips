/**
 * ProbandDataLoader is responsible for retrieving data on the proband.
 *
 * @class ProbandDataLoader
 * @constructor
 */

define([
        "pedigree/model/helpers"
    ], function(
        Helpers
    ){
    var FamilyData = Class.create( {
        DEFAULT_SENSITIVE_DATA_MESSAGE: "This pedigree was marked as containing sensitive information. No further details were provided.",

        initialize: function() {
            this.familyPage = null;
            this.familyMembers = [];
            this.familyMembersIndex = {};
            this.warningMessage = "";
        },

        updateFromJSON: function(familyJSON) {
            this.familyPage = familyJSON.hasOwnProperty("id") ?
                              familyJSON.id : null;

            this.familyExtId = familyJSON.hasOwnProperty("externalId") ?
                               familyJSON.externalId: "";

            this.familyMembers = familyJSON.hasOwnProperty("familyMembers") ?
                                 familyJSON.familyMembers: [];

            this.familyMembersIndex = {};
            for (var i = 0; i < this.familyMembers.length; i++) {
                this.familyMembersIndex[this.familyMembers[i].id] = i;
            }

            // set to null if no sensitive data present, set to DEFAULT_SENSITIVE_DATA_MESSAGE if provided message is blank, otherwise use provided
            this.warningMessage = familyJSON["contains_sensitive_data"] ? (familyJSON["sensitive_data_message"] || this.DEFAULT_SENSITIVE_DATA_MESSAGE) : null;

            console.log("Family data:  [familyPage: " + this.familyPage +
                    "], [editingFamilyPage: " + Helpers.stringifyObject(this.isFamilyPage()) + "], [Members:" +
                    Helpers.stringifyObject(this.familyMembers) + "]");
        },

        isFamilyPage: function() {
            return (this.familyPage == editor.getGraph().getCurrentPatientId());
        },

        getFamilyId: function() {
            return this.familyPage;
        },

        getFamilyExternalId: function() {
            return this.familyExtId;
        },

        hasWarningMessage: function() {
            return (this.warningMessage != null);
        },

        getLoadedFamilyMembers: function() {
            return this.familyMembers;
        },

        getWarningMessage: function() {
            return this.warningMessage;
        },

        isFamilyMember: function(patientID) {
            return this.familyMembersIndex.hasOwnProperty(patientID);
        },

        getPatientAccessPermissions: function(patientID) {
            var familyhMemberData = this._getFamilyMemberDetails(patientID);
            if (familyhMemberData == null || !familyhMemberData.hasOwnProperty("permissions")) {
                return null;
            }
            return familyhMemberData.permissions;
        },

        _getFamilyMemberDetails: function(patientID) {
            if (!this.isFamilyMember(patientID)) {
                return null;
            }
            return this.familyMembers[this.familyMembersIndex[patientID]];
        }
    });

    return FamilyData;
});
