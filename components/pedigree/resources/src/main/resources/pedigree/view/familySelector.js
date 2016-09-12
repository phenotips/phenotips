/**
 * The UI Element for assigning patient to a new or existing family.
 *
 * @class FamilySelector
 */

define(["pedigree/view/familySelector"], function(Legend){
    var FamilySelector = Class.create( {

        initialize: function(options) {
            this.familyID = null;
            this.selected = null;

            var _this = this;

            this.probandID = editor.getGraph().getCurrentPatientId();
            this.familySearchUrl = editor.getExternalEndpoint().getFamilySearchURL();

            var mainDiv = new Element('div', {'class': 'family-selector'});

            // create radio buttons for selection
            var createLabel = new Element('label', {'class' : 'create-family-label'});
            var assignLabel = new Element('label', {'class' : 'assign-family-label'});

            var createFamily =  new Element('input', {'type' : 'radio', 'id': 'pedigreeInputCreateFamily', 'name' : 'select-family', 'value' : 'create'});
            var assignFamily =  new Element('input', {'type' : 'radio', 'id': 'pedigreeInputAssignFamily', 'name' : 'select-family', 'value' : 'assign'});
            createFamily.observe('click', this.selectOptions.bind(this) );
            assignFamily.observe('click', this.selectOptions.bind(this) );

            createLabel.insert(createFamily).insert("Create a new family");
            assignLabel.insert(assignFamily).insert("$services.localization.render('phenotips.UIXField.family_membership.add')");
            var ul = new Element('ul');
            ul.insert(createLabel.wrap('li', {'class' : 'left-aligned'})).insert(assignLabel.wrap('li', {'class' : 'left-aligned'}));
            mainDiv.insert(ul);

            /*
               //create family warning
               var block = new Element('div');
               block.update("The pedigree is shared by all members of this family. Patient records may have collaborators. Adding an existing patient to this family will grant all collaborators from that patient access to all family members' records. Also, all collaborators will be able to edit that patient's record.");
               mainDiv.insert(block.wrap('div', {'id' : 'new-family-warning', 'class' : 'box warningmessage'}));
            */

            // input field that will suggest families upon typing
            var searchBox = new Element('div', {'class': 'half-width'});
            this.searchInput = new Element('input', {'type': 'text', 'id' : 'family-search-input', 'class' : 'family-search-input', 'placeholder' : 'Enter a family name'});
            searchBox.insert(this.searchInput).insert(new Element('span', {'class' : 'clear'}).wrap('div', {'class': 'add-to-family-wrapper'}));
            mainDiv.insert(searchBox);

            this.searchInput._suggest = new PhenoTips.widgets.Suggest(this.searchInput, {
                script: this.familySearchUrl + "&json=true&",
                varname: "input",
                noresults: "No matching terms",
                resultsParameter: "matchedFamilies",
                json: true,
                resultId: "id",
                resultValue: "textSummary",
                enableHierarchy: false,
                fadeOnClear: false,
                timeout: 30000,
                parentContainer: $$('.pedigree-family-chooser')[0]
            });

            this.searchInput.addClassName('initialized');
            this.searchInput.observe("focus", function (event) {
                $('pedigreeInputAssignFamily').click();
            });

            this._onClickOutsideSuggest = this._onClickOutsideSuggest.bindAsEventListener(this);
            this._onSelectedFamily = this._onSelectedFamily.bindAsEventListener(this);

            document.observe("ms:suggest:containerCreated", function(event) {
                if (event.memo && event.memo.suggest === _this.searchInput._suggest) {
                    _this.searchInput._suggest.container.setStyle({'z-index': 100015});
                }
            });

            var buttons = new Element('div', {'class' : 'buttons', 'style' : 'margin-top: 10px;'});
            this.okButton = new Element('input', {'type': 'button', 'name' : 'ok', 'value' : ' OK ', 'class' : 'button'});
            this.okButton.setStyle({"min-width": "140px"});
            this.quitButton = new Element('input', {'type': 'button', 'name' : 'quit', 'value' : ' Close pedigree editor ', 'class' : 'button secondary'});
            this.quitButton.setStyle({"marginLeft": "10px"});
            buttons.insert(this.okButton.wrap('span', {'class' : 'buttonwrapper'}));
            buttons.insert(this.quitButton.wrap('span', {'class' : 'buttonwrapper'}));
            mainDiv.insert(buttons);

            this.okButton.disable();

            this.okButton.observe('click', this.onSelectOK.bind(this) );
            this.quitButton.observe('click', this.quit.bind(this) );

            this.dialog = new PhenoTips.widgets.ModalPopup(mainDiv, {'close': {'method': null, 'keys': []} }, {'extraClassName' : "pedigree-family-chooser", title: 'Assign patient to a family', 'displayCloseButton' : false});

            //$('new-family-warning').hide();
        },

        familySelected: function (event) {
            this.familyID = event.memo.id;
            console.log("select, ID: " + this.familyID);
            this.okButton.enable();
        },

        selectOptions: function() {
            this.selected = $$('input:checked[type=radio][name="select-family"]')[0].value;
            if (this.selected == "assign") {
                $('family-search-input').value = this.familyID;
                if (this.familyID == null || this.familyID == '') {
                    this.okButton.disable();
                } else {
                    this.okButton.enable();
                }
                //$('new-family-warning').show();
            } else {
                this.okButton.enable();
                $('family-search-input').value = "";
                //$('new-family-warning').hide();
            }
        },

        /**
         * Displays the family selector
         *
         * @method show
         */
        show: function() {
            this.dialog.show();
            document.observe('mousedown', this._onClickOutsideSuggest);
            document.observe("ms:suggest:selected", this._onSelectedFamily);
        },

        hide: function() {
            this.dialog.closeDialog();
            document.stopObserving("ms:suggest:selected", this._onSelectedFamily);
            document.stopObserving('mousedown', this._onClickOutsideSuggest);
        },

        _onClickOutsideSuggest: function (event) {
            if (!event.findElement('.suggestItems')) {
                this._hideSuggestPicker();
            }
        },

        _hideSuggestPicker: function() {
            this.searchInput._suggest.clearSuggestions();
        },

        _onSelectedFamily: function(event) {
            if (event.memo && event.memo.suggest === this.searchInput._suggest) {
                this.familySelected(event);
            }
        },

        /**
         * Closes pedigree editor
         */
        quit: function() {
            window.location = XWiki.currentDocument.getURL(XWiki.contextaction);
        },

        /**
         * Removes the family selector
         *
         * @method hide
         */
        onSelectOK: function() {

            this.familyID = $('family-search-input').value || this.familyID;

            if (this.selected == null || this.selected == 'assign' && (this.familyID == null || this.familyID == '')) {

                // warn user that he did not finish work ask if he really want to leave
                // if yes - close pedigree and redirect to the patient sheet form

                var continueFunc = function() {};
                editor.getOkCancelDialogue().showCustomized( "The patient has not been assigned to any family and can't contain a pedigree, do you want to close pedigree editor?", 'Quit pedigree editor?',
                                                             'Select a family', continueFunc,
                                                             'Close', this.quit);
            } else if (this.selected == 'create') {

                this.hide();

                editor.getPatientLegend().addCase(this.probandID, 'new');

                editor.__justCreateNewFamily = true;
                editor.getSaveLoadEngine().initializeNewPedigree();

            } else if (this.selected == 'assign' && this.familyID != null && this.familyID != '') {

                this.hide();

                var pedigreeEditorURL = editor.getExternalEndpoint().getPedigreeEditorURL(this.familyID, true);
                editor.getExternalEndpoint().redirectToURL(pedigreeEditorURL + '&new_patient_id=' + editor.getExternalEndpoint().getParentDocument().id);
          }
        }
    });
    return FamilySelector;
});
