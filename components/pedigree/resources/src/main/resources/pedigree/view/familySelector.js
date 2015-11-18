/**
 * The UI Element for importing pedigrees from text representationin various formats
 *
 * @class ImportSelector
 */

var FamilySelector = Class.create( {

    initialize: function(options) {

    	this.familyID = null;
    	this.selected = null

    	this.probandID = editor.getGraph().getCurrentPatientId();
    	this.familySearchUrl = editor.getExternalEndpoint().getFamilySearchURL();

        var mainDiv = new Element('div', {'class': 'family-selector'});     

        //create radio buttons for selection
        var createLabel = new Element('label', {'class' : 'create-family-label'});
        var assignLabel = new Element('label', {'class' : 'assign-family-label'});

        var createFamily =  new Element('input', {'type' : 'radio', 'id': 'pedigreeInputCreateFamily', 'name' : 'select-family', 'value' : 'create'});
        var assignFamily =  new Element('input', {'type' : 'radio', 'id': 'pedigreeInputAssignFamily', 'name' : 'select-family', 'value' : 'assign'});
        createFamily.observe('click', this.selectOptions.bind(this) );
        assignFamily.observe('click', this.selectOptions.bind(this) );

        createLabel.insert(createFamily).insert("Create a new family for this patient");
        assignLabel.insert(assignFamily).insert("$services.localization.render('phenotips.UIXField.family_membership.add')");
        var ul = new Element('ul');
        ul.insert(createLabel.wrap('li', {'class' : 'left-aligned'})).insert(assignLabel.wrap('li', {'class' : 'left-aligned'}));
        mainDiv.insert(ul);

        /*//create family warning 
		var block = new Element('div');
		block.update("The pedigree is shared by all members of this family. Patient records may have collaborators. Adding an existing patient to this family will grant all collaborators from that patient access to all family members' records. Also, all collaborators will be able to edit that patient's record.");
        mainDiv.insert(block.wrap('div', {'id' : 'new-family-warning', 'class' : 'box warningmessage'}));*/

        //input field that will suggest families upon typing
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

        document.observe("ms:suggest:containerCreated", function(event) {
        	event.memo.suggest.container.setStyle({'z-index': 100015});
        });

        document.observe("ms:suggest:selected", this.familySelected.bind(this));

        var buttons = new Element('div', {'class' : 'buttons', 'style' : 'margin-top: 10px;'});
        var okButton = new Element('input', {'type': 'button', 'name' : 'ok', 'value' : 'OK', 'class' : 'button'});
        buttons.insert(okButton.wrap('span', {'class' : 'buttonwrapper'}));
        mainDiv.insert(buttons);

        okButton.observe('click', this.hide.bind(this) );
        var closeShortcut = ['Esc'];
        this.dialog = new PhenoTips.widgets.ModalPopup(mainDiv, {'close': {'method' : this.hide.bind(this), 'keys' : closeShortcut}}, {'extraClassName' : "pedigree-family-chooser", title: 'Assign patient ot a family', 'displayCloseButton' : false});
        this.dialog.show();
        //$('new-family-warning').hide();
    },

    familySelected: function (event) {
        this.familyID = event.memo.id;
    },

    selectOptions: function() {
        this.selected = $$('input:checked[type=radio][name="select-family"]')[0].value;
        if (this.selected == "assign") {
        	//$('new-family-warning').show();
        } else {
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
    },
    
    /**
     * Creates patient legend
     *
     * @method show
     */
    _createPatientLegend: function(patientDataJsonURL, patientID) {
    	new Ajax.Request(patientDataJsonURL, {
            method: "GET",
            onSuccess: function (response) {
                if (response.responseJSON) {
                	var patient = response.responseJSON[patientID];
                	var name = null;
                	if (patient.hasOwnProperty('patient_name')) {
                		if (patient.patient_name.first_name && patient.patient_name.first_name.trim()) {
                			name = patient.patient_name.first_name;
                		}
                		if (patient.patient_name.last_name && patient.patient_name.last_name.trim()) {
                			name += ' ' + patient.patient_name.last_name;
                		}
                	}
                	editor.getPatientLegend().addCase(patientID, 'new', patient.sex, name, patient.external_id);
                }
            }
        });
    },

    /**
     * Removes the family selector
     *
     * @method hide
     */
    hide: function() {

    	var patientID = this.probandID;

    	this.familyID = this.familyID || $('family-search-input').value;

    	if (this.selected == null || this.selected == 'assign' && (this.familyID == null || this.familyID == '')) {
    		//warn user that he did not finish work ask if he really want to leave
    		//if yes - close pedigree and redirect to the patient sheet form
            var continueFunc    = function() {};
            var quitFunc        = function() { window.location = XWiki.currentDocument.getURL(XWiki.contextaction); };
            editor.getOkCancelDialogue().showCustomized( 'The patient has not been assigned to any family, do you want to continue assigning it?', '',
                                                         'Continue', continueFunc,
                                                         'Quit', quitFunc);
    	} else if (this.selected == 'create') {

    		this.dialog.closeDialog();

    		var patientDataJsonURL = editor.getExternalEndpoint().getLoadPatientDataJSONURL([patientID]);
    		this._createPatientLegend(patientDataJsonURL, patientID);

        	document.fire("pedigree:family:assigned");

    	} else if (this.selected == 'assign' && this.familyID != null && this.familyID != '') {

    		var errorTitle = "Could not assign patient " + this.probandID +" to a family " + this.familyID;
    		var dialog = this.dialog;
            // load family pedigree JSON
            var getFamilyPedigreeURL = editor.getExternalEndpoint().getLoadPatientPedigreeJSONURL(this.familyID);

            new Ajax.Request(getFamilyPedigreeURL, {
                method: 'GET',
                onSuccess: function (response) {
                    if (response.responseJSON) {
                        //console.log("[LOAD] recived JSON: " + stringifyObject(response.responseJSON));
                        var updatedJSONData = editor.getVersionUpdater().updateToCurrentVersion(response.responseText);

                        var addSaveEventOnceLoaded = function() {
                            // since we just loaded data from disk data in memory is equivalent to data on disk
                            editor.getUndoRedoManager().addSaveEvent();
                        }

                        editor.getSaveLoadEngine().createGraphFromSerializedData(updatedJSONData, false, true, addSaveEventOnceLoaded);
                        var patientDataJsonURL = editor.getExternalEndpoint().getLoadPatientDataJSONURL([patientID]);
                        this._createPatientLegend(patientDataJsonURL, patientID);

                        dialog.closeDialog();
                    } else {
                    	//there is no pedigree or no such family
                    	editor.getOkCancelDialogue().showError('Server error - unable to assign patient: ' + response.responseText,
                    			errorTitle, "OK", undefined );
                    }
                }.bind(this)
            })

    	}
    }

});