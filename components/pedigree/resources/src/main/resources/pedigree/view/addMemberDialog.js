/**
 * Add new family member dialog
 *
 * @class AddNewMemberDialog
 */
define([
        "pedigree/pedigreeEditorParameters"
    ], function(
        PedigreeEditorParameters
    ){
    var AddNewMemberDialog = Class.create( {

        initialize: function() {
            var _this = this;

            var mainDiv = new Element('div', {'class': 'export-selector'});

            this.patientPicker = new Element('input', {type: 'text', 'class': 'suggest suggest-patients'});
            this.patientPicker.observe('ms:suggest:selected', function(event) {
               _this._onLinkExisting(event.memo.id);
            });

            var createButton = new Element('input', {type: 'button', name : 'createnew', 'value': 'Create new', 'class' : 'create-family-member-button', 'id': 'create_new_patient_button'});
            createButton.observe('click', function(event) {
                _this._onCreateNew();
            });

            var linkSection = new Element('div', {'class' : 'add-link-to-existing-patient'}).update("Link to an existing patient record:");
            linkSection.insert("&nbsp;").insert(this.patientPicker).insert(" or ").insert(createButton);
            mainDiv.insert(linkSection);

            var buttons = new Element('div', {'class' : 'buttons import-block-bottom'});
            buttons.insert(new Element('input', {type: 'button', name : 'cancel', 'value': 'Cancel', 'class' : 'button secondary'}).wrap('span', {'class' : 'buttonwrapper'}));
            mainDiv.insert(buttons);

            var cancelButton = buttons.down('input[name="cancel"]');
            cancelButton.observe('click', function(event) {
                _this.hide();
            });

            var closeShortcut = ['Esc'];
            this.dialog = new PhenoTips.widgets.ModalPopup(mainDiv, {close: {method : this.hide.bind(this), keys : closeShortcut}}, {extraClassName: "pedigree-create-patient", title: "Add family member", displayCloseButton: false});

            this._generatePatientPicker();
        },

        /**
         * Displays the dialog
         *
         * @method show
         */
        show: function() {
            this.patientPicker.value = "";
            this.patientPicker.placeholder = "type patient name or identifier";
            this.dialog.show();
        },

        /**
         * Removes the dialog
         *
         * @method hide
         */
        hide: function() {
            this.dialog.closeDialog();
        },

        _onCreateNew: function() {
            this.hide();
            document.fire("pedigree:patient:createrequest", {"onCreatedHandler": this._onPatientCreated.bind(this) } );
        },

        _onPatientCreated: function(newID, patientJSON) {
            // a newly linked patient record has no pedigree-specific properties yet
            var pedigreeProperties = {};
            editor.getPatientLegend().addCase(newID, pedigreeProperties);
        },

        _onLinkExisting: function(phenotipsID) {
            this.hide();
            console.log("Linking to existing patient: " + phenotipsID);

            var onValidCallback = function() {
                editor.getPatientLegend().addCase(phenotipsID);
            };

            document.fire("pedigree:patient:checklinkvalidity", {"phenotipsPatientID": phenotipsID,
                                                                 "onValidCallback": onValidCallback });
        },

        _generatePatientPicker: function() {
            var options = {
                        script: editor.getExternalEndpoint().getPatientSuggestServiceURL(),
                        noresults: "No matching patients",
                        width: 337,
                        resultsParameter: "matchedPatients",
                        resultValue: "textSummary",
                        resultInfo: {}
            };

            var item = this.patientPicker;
            if (!item.hasClassName('initialized')) {
                options.parentContainer = item.up();
                item._suggest = new PhenoTips.widgets.Suggest(item, options);
                item.addClassName('initialized');
                document.observe('ms:suggest:containerCreated', function(event) {
                    if (event.memo && event.memo.suggest === item._suggest) {
                        item._suggest.container.setStyle({'overflow': 'auto', 'maxHeight': document.viewport.getHeight() - item._suggest.container.cumulativeOffset().top + 'px'})
                    }
                });
            }
        },
    });

    return AddNewMemberDialog;
});
