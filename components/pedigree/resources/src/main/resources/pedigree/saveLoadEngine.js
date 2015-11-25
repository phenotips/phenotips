/**
 * SaveLoadEngine is responsible for automatic and manual save and load operations.
 *
 * @class SaveLoadEngine
 * @constructor
 */

define([
        "pedigree/model/helpers",
        "pedigree/view/templateSelector"
    ], function(
        Helpers,
        TemplateSelector
    ){
    var SaveLoadEngine = Class.create( {

        initialize: function() {
            this._saveInProgress = false;
        },

        /**
         * Saves the state of the pedigree (including any user preferences and current color scheme)
         *
         * @return Serialization data for the entire graph
         */
        serialize: function() {
            var jsonObject = editor.getGraph().toJSONObject();

            jsonObject["settings"] = editor.getView().getSettings();

            return JSON.stringify(jsonObject);
        },

        createGraphFromSerializedData: function(JSONString, noUndo, centerAroundProband, callbackWhenDataLoaded) {
            console.log("---- load: parsing data ----");
            document.fire("pedigree:load:start");

            try {
                var jsonObject = JSON.parse(JSONString);

                // load the graph model of the pedigree & node data
                var changeSet = editor.getGraph().fromJSONObject(jsonObject);

                // load/process metadata such as pedigree options and color choices
                if (jsonObject.hasOwnProperty("settings")) {
                    editor.getView().loadSettings(jsonObject.settings);
                }
            }
            catch(err)
            {
                console.log("ERROR loading pedigree: " + err);
                alert("Error loading pedigree");
                document.fire("pedigree:graph:clear");
                document.fire("pedigree:load:finish");
                return;
            }

            this._finalizeCreateGraph(changeSet, noUndo, centerAroundProband, callbackWhenDataLoaded);
        },

        createGraphFromImportData: function(importString, importType, importOptions, noUndo, centerAroundProband) {
            console.log("---- import: parsing data ----");
            document.fire("pedigree:load:start");

            try {
                var changeSet = editor.getGraph().fromImport(importString, importType, importOptions);
                if (changeSet == null) {
                    throw "unable to create a pedigree from imported data";
                }
            }
            catch(err)
            {
                console.log("ERROR importing pedigree: " + err);
                alert("Error importing pedigree: " + err);
                document.fire("pedigree:graph:clear");
                document.fire("pedigree:load:finish");
                return;
            }

            this._finalizeCreateGraph(changeSet, noUndo, centerAroundProband);
        },

        // common code for pedigree creation called after the actual pedigree has been initialized using whatever input data
        _finalizeCreateGraph: function(changeSet, noUndo, centerAroundProband, callbackWhenDataLoaded) {

            var _this = this;

            var finalizeCreation = function(loadedPatientData) {
                if (loadedPatientData !== null) {

                    var allLinkedNodes = editor.getGraph().getAllPatientLinks();

                    for (var patient in loadedPatientData) {
                        if (loadedPatientData.hasOwnProperty(patient)) {
                            var patientJSONObject = loadedPatientData[patient];

                            if (patientJSONObject === null) {
                                // no data for this patient: it is ok just don't set any properties
                                // (may happen if a patient is deleted; we'll still keep the properties as stored in the pedigree)
                                continue;
                            }

                            var nodeID = allLinkedNodes.patientToNodeMapping[patient];

                            // TODO: check if data is correctly cleared on import

                            // reuse some properties which are not currently saved into patient record
                            // such as cancers and pedigree specific stuff
                            patientJSONObject.pedigreeProperties = editor.getGraph().getNodePropertiesNotStoredInPatientProfile(nodeID);

                            var genderOk = editor.getGraph().setNodeDataFromPhenotipsJSON( nodeID, patientJSONObject);
                            if (!genderOk)
                                alert("Gender defined in Phenotips for patient " + patient + " is incompatible with this pedigree. Setting pedigree node gender to 'Unknown'");
                        }
                    }

                    if (!noUndo && !editor.isReadOnlyMode()) {
                        var JSONString = _this.serialize();
                        editor.getUndoRedoManager().addState(null, null, JSONString);
                    }

                    // FIXME: load will set callbackWhenDataLoaded() to be actionStack.addSaveEvent(), effectively
                    //        a) duplicating undo states and b) doing it for read-only pedigrees

                    callbackWhenDataLoaded && callbackWhenDataLoaded();
                }

                if (editor.getView().applyChanges(changeSet, false)) {
                    editor.getWorkspace().adjustSizeToScreen();
                }

                if (centerAroundProband) {
                    editor.getWorkspace().centerAroundNode(editor.getGraph().getProbandId());
                }

                document.fire("pedigree:load:finish");
            };

            if (!noUndo && !editor.isReadOnlyMode()) {
                var allLinkedNodes = editor.getGraph().getAllPatientLinks();

                if (allLinkedNodes.linkedPatients.length == 0) {
                    if (editor.isFamilyPage()) {
                        // if no nodes are linked to any patient documents, and there is only one family member
                        // => auto-assign the only family member to the assumed proband node. This case is only
                        //    possible when  apedigree is created form a template or imported, as any family
                        //    pedigree on disk will always have at least one node linked to a patient
                        //
                        // TODO: code below assumes template proband is node with id 0 and/or imported proband is node with id 0,
                        //       which may change in the future

                        var familyMembers = editor.getCurrentFamilyPageFamilyMembers();

                        if (familyMembers.length == 1 && allLinkedNodes.linkedPatients.length == 0) {
                            var probandProperties = editor.getGraph().getProperties(0);
                            probandProperties["phenotipsId"] = familyMembers[0].id;
                            editor.getGraph().setProperties(0, probandProperties);
                        }
                    }
                    else {
                        // similar to family page, if no node is linked to the current patient link assumed proband node
                        // to current patient.
                        var probandProperties = editor.getGraph().getProperties(editor.getGraph().getProbandId());
                        if (!probandProperties.hasOwnProperty("phenotipsId")) {
                            // TODO: it is currently guaranteed that a node is linked to editor.getGraph().getCurrentPatientId()
                            //       it will be the proband. But in theory may want to check if non-proband node is linked
                            //       to current documen, which implies some kind of data inconsistency
                            probandProperties["phenotipsId"] = editor.getGraph().getCurrentPatientId();
                            editor.getGraph().setProperties(editor.getGraph().getProbandId(), probandProperties);
                        }
                    }
                }

                // update to include nodes possibly added to the set of linked nodes above
                allLinkedNodes = editor.getGraph().getAllPatientLinks();

                var patientDataLoader = editor.getPatientDataLoader();

                patientDataLoader.load(allLinkedNodes.linkedPatients, finalizeCreation);
            } else {
                finalizeCreation(null /* do not update nodes using data loaded from PhenoTips */);
            }
        },

        save: function() {
            if (this._saveInProgress) {
                return;   // Don't send parallel save requests
            }

            editor.getView().unmarkAll();

            var me = this;
            me._notSaved = true;

            var jsonData = this.serialize();

            console.log("[SAVE] data: " + Helpers.stringifyObject(jsonData));

            var svg = editor.getWorkspace().getSVGCopy();
            var svgText = svg.getSVGText();

            var savingNotification = new XWiki.widgets.Notification("Saving", "inprogress");

            var familyServiceURL = editor.getExternalEndpoint().getSavePedigreeURL();
            new Ajax.Request(familyServiceURL, {
                method: 'POST',
                onCreate: function() {
                    me._saveInProgress = true;
                    // Disable save and close buttons during a save
                    var closeButton = $('action-close');
                    var saveButton = $('action-save');
                    Element.addClassName(saveButton, "disabled-menu-item");
                    Element.removeClassName(saveButton, "menu-item");
                    Element.addClassName(saveButton, "no-mouse-interaction");
                    Element.addClassName(closeButton, "disabled-menu-item");
                    Element.removeClassName(closeButton, "menu-item");
                    Element.addClassName(closeButton, "no-mouse-interaction");
                    // IE9 & IE10 do not support "no-mouse-interaction", so add JS to handle this
                    Helpers.disableMouseclicks(closeButton);
                    Helpers.disableMouseclicks(saveButton);
                    // disable user interaction while save is in progress
                    document.fire("pedigree:load:start");
                },
                onComplete: function() {
                    me._saveInProgress = false;
                    // Enable save and close buttons after a save
                    var closeButton = $('action-close');
                    var saveButton = $('action-save');
                    Element.addClassName(saveButton, "menu-item");
                    Element.removeClassName(saveButton, "disabled-menu-item");
                    Element.removeClassName(saveButton, "no-mouse-interaction");
                    Element.addClassName(closeButton, "menu-item");
                    Element.removeClassName(closeButton, "disabled-menu-item");
                    Element.removeClassName(closeButton, "no-mouse-interaction");
                    // remove IE9/IE10 specific handlers
                    Helpers.enableMouseclicks(closeButton);
                    Helpers.enableMouseclicks(saveButton);
                    // Re-enable user-interaction
                    document.fire("pedigree:load:finish");
                    if (!me._notSaved) {
                        var actionAfterSave = editor.getAfterSaveAction();
                        actionAfterSave && actionAfterSave();
                    }
                },
                onSuccess: function(response) {
                    if (response.responseJSON) {
                        if (response.responseJSON.error) {
                            savingNotification.replace(new XWiki.widgets.Notification("Pedigree was not saved"));
                            SaveLoadEngine._displayFamilyPedigreeInterfaceError(response.responseJSON, "Error saving pedigree", "Unable to save pedigree: ");
                        } else {
                            me._notSaved = false;
                            editor.getUndoRedoManager().addSaveEvent();
                            savingNotification.replace(new XWiki.widgets.Notification("Successfully saved"));
                        }
                    } else  {
                        savingNotification.replace(new XWiki.widgets.Notification("Save attempt failed: server reply is incorrect"));
                        editor.getOkCancelDialogue().showError('Server error - unable to save pedigree',
                                'Error saving pedigree', "OK", undefined );
                    }
                },
                parameters: {"proband": editor.getGraph().getCurrentPatientId(), "json": jsonData, "image": svgText}
            });
        },

        load: function() {
            console.log("initiating load process");

            var probandID = editor.getGraph().getCurrentPatientId();
            var pedigreeJsonURL = editor.getExternalEndpoint().getLoadPatientPedigreeJSONURL(probandID);

            new Ajax.Request(pedigreeJsonURL, {
                method: 'GET',
                onCreate: function() {
                    document.fire("pedigree:load:start");
                },
                onSuccess: function (response) {
                    //console.log("Data from LOAD: >>" + response.responseText + "<<");
                    if (response.responseJSON) {
                        console.log("[LOAD] recived JSON: " + Helpers.stringifyObject(response.responseJSON));

                        var updatedJSONData = editor.getVersionUpdater().updateToCurrentVersion(response.responseText);

                        var addSaveEventOnceLoaded = function() {
                            // since we just loaded data from disk data in memory is equivalent to data on disk
                            editor.getUndoRedoManager().addSaveEvent();
                        }

                        // FIXME: it seems like load will generate saveEvent + undoEvent in undoredo stack

                        this.createGraphFromSerializedData(updatedJSONData, false, true, addSaveEventOnceLoaded);
                    } else {
                        new TemplateSelector(true);
                    }
                }.bind(this)
            })
        }
    });

    SaveLoadEngine._displayFamilyPedigreeInterfaceError = function(replyJSON, title, messageIntro, callWhenDone)
    {
        var errorMessage = replyJSON.errorMessage ? replyJSON.errorMessage : "Unknown problem";
        errorMessage = "<font color='#660000'>" + errorMessage + "</font><br><br><br>";
        if (replyJSON.errorType == "familyConflict") {
            errorMessage += "(for now it is only possible to add persons who is not in another family to a family)";
        }
        if (replyJSON.errorType == "pedigreeConflict") {
            errorMessage += "(for now it is only possible to add persons without an already existing pedigree to a family)";
        }
        if (replyJSON.errorType == "permissions") {
            errorMessage += "(you need to have edit permissions for the patient to be able to add it to a family)";
        }
        editor.getOkCancelDialogue().showError('<br>' + messageIntro + errorMessage, title, "OK", callWhenDone );
    }

    return SaveLoadEngine;
});