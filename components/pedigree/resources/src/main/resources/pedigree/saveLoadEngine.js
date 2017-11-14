/**
 * SaveLoadEngine is responsible for automatic and manual save and load operations.
 *
 * @class SaveLoadEngine
 * @constructor
 */

define([
        "pedigree/model/helpers"
    ], function(
        Helpers
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

        createGraphFromSerializedData: function(JSONString, noUndo, centerAroundProband, callbackWhenDataLoaded, dataSource) {
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
                document.fire("pedigree:load:finish");

                // if there is no pedigree and import was used to initialize a pedigree need to display the import dialogue again
                if (!editor.pedigreeExists()) {
                    this.showInitializeDialogue();
                }
                return;
            }

            this._finalizeCreateGraph("pedigreeLoaded", changeSet, noUndo, centerAroundProband, callbackWhenDataLoaded, dataSource);
        },

        createGraphFromImportData: function(importString, importType, importOptions, noUndo, centerAroundProband, dataSource) {
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
                document.fire("pedigree:load:finish");

                // if there is no pedigree and import was used to initialize a pedigree need to display the import dialogue again
                if (!editor.pedigreeExists()) {
                    this.showInitializeDialogue(true /* go to import tab */);
                }
                return;
            }

            this._finalizeCreateGraph("pedigreeImported", changeSet, noUndo, centerAroundProband, null /* no callback */, dataSource);
        },

        // common code for pedigree creation called after the actual pedigree has been initialized using whatever input data
        _finalizeCreateGraph: function(eventName, changeSet, noUndo, centerAroundProband, callbackWhenDataLoaded, dataSource) {

            var _this = this;

            var finalizeCreation = function(loadedPatientData) {

                if (loadedPatientData !== null) {
                    var familyMemberIds = Object.keys(loadedPatientData);

                    var allLinkedNodes = editor.getGraph().getAllPatientLinks();

                    if (dataSource && dataSource == "template") {
                        if (familyMemberIds.length == 1 && allLinkedNodes.linkedPatients.length == 0) {
                            var probandNodeID = editor.getGraph().getProbandId();
                            var probandProperties = editor.getGraph().getProperties(probandNodeID);
                            probandProperties["phenotipsId"] = familyMemberIds[0];
                            editor.getGraph().setProperties(probandNodeID, probandProperties);
                            allLinkedNodes = editor.getGraph().getAllPatientLinks();
                        }
                    }

                    for (var patient in loadedPatientData) {
                        if (loadedPatientData.hasOwnProperty(patient)) {
                            var patientJSONObject = loadedPatientData[patient];

                            if (patientJSONObject === null) {
                                // no data for this patient: it is ok just don't set any properties
                                // (may happen if a patient is deleted; we'll still keep the properties as stored in the pedigree)
                                continue;
                            }

                            if (!allLinkedNodes.patientToNodeMapping.hasOwnProperty(patient)) {
                              continue;
                            }
                            var nodeID = allLinkedNodes.patientToNodeMapping[patient];

                            // TODO: check if data is correctly cleared on import

                            // reuse some properties which are not currently saved into patient record
                            // such as cancers and pedigree specific stuff
                            patientJSONObject.pedigreeProperties = editor.getGraph().getNodePropertiesNotStoredInPatientProfile(nodeID);

                            editor.getGraph().setNodeDataFromPhenotipsJSON( nodeID, patientJSONObject);
                        }
                    }

                    if (!noUndo && !editor.isReadOnlyMode()) {
                        var JSONString = _this.serialize();
                        editor.getUndoRedoManager().addState({"eventName": eventName}, null, JSONString);
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
                // update to include nodes possibly added to the set of linked nodes above
                var allLinkedNodes = editor.getGraph().getAllPatientLinks();

                // get all patients in the pedigree and those in the patient legend (those are not in pedigree but may get assigned)
                var patientList = Helpers.filterUnique(allLinkedNodes.linkedPatients.concat(editor.getPatientLegend().getListOfPatientsInTheLegend()));

                editor.getPatientDataLoader().load(patientList, finalizeCreation);
            } else {
                finalizeCreation(null /* do not update nodes using data loaded from PhenoTips */);
            }
        },

        save: function(callAfterSuccessfulSave, callAfterFailedSave) {
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
                    document.fire("pedigree:load:start", {"message": "Saving pedigree..."});
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
                    if (me._notSaved) {
                        callAfterFailedSave && callAfterFailedSave();
                    } else {
                        callAfterSuccessfulSave && callAfterSuccessfulSave();
                    }
                },
                // 0 is returned for network failures, except on IE which converts it to a strange large number (12031)
                on0 : function(response) {
                  response.request.options.onFailure(response);
                },
                onFailure : function(response) {
                  var errorMessage = '';
                  if (response.statusText == '' /* No response */ || response.status == 12031 /* In IE */) {
                    errorMessage = 'Server not responding';
                  } else if (response.getHeader('Content-Type').match(/^\s*text\/plain/)) {
                    // Regard the body of plain text responses as custom status messages.
                    errorMessage = response.responseText;
                  } else {
                    errorMessage = response.statusText;
                  }
                  savingNotification.replace(new XWiki.widgets.Notification("Saving failed: " + errorMessage));
                  var content = new Element('div', {'class' : 'box errormessage'});
                  content.insert(new Element('p').update(new Element('strong').update("SAVING FAILED: " + errorMessage)));
                  content.insert(new Element('p').update("YOUR PEDIGREE IS NOT SAVED. To avoid losing your work, we recommend taking a screenshot of the pedigree and exporting the pedigree data as simple JSON using the 'Export' option in the menu at the top of the pedigree editor. Please notify your PhenoTips administrator of this error."));
                  var d = new PhenoTips.widgets.ModalPopup(content, '', {'titleColor' : '#000'});
                  d.show();
                },
                onSuccess: function(response) {
                    if (response.responseJSON) {
                        if (response.responseJSON.error) {
                            savingNotification.replace(new XWiki.widgets.Notification("Pedigree was not saved"));
                            SaveLoadEngine._displayFamilyPedigreeInterfaceError(response.responseJSON, "Error saving pedigree", "Unable to save pedigree: ");
                        } else {
                            me._notSaved = false;
                            editor.getUndoRedoManager().addSaveEvent();
                            editor.getFamilyData().updateFromJSON(response.responseJSON.family);
                            savingNotification.replace(new XWiki.widgets.Notification("Successfully saved"));
                            document.fire("pedigree:save:finish");
                        }
                    } else  {
                        savingNotification.replace(new XWiki.widgets.Notification("Save attempt failed: server reply is incorrect"));
                        editor.getOkCancelDialogue().showError('Server error - unable to save pedigree',
                                'Error saving pedigree', "OK", undefined );
                    }
                },
                parameters: {"family_id": editor.getFamilyData().getFamilyId(), "json": jsonData, "image": svgText}
            });
        },

        load: function(familyOrPatientId) {
            console.log("Initiating load process...");
            var familyJsonURL = editor.getExternalEndpoint().getFamilyInfoURL();
            var loaded = false;
            var _this = this;
            new Ajax.Request(familyJsonURL, {
                method: "POST",
                onCreate: function() {
                    document.fire("pedigree:load:start", {"message": "Loading pedigree..."});
                },
                onSuccess: function(response) {
                    if (response.responseJSON) {
                        if (!response.responseJSON.hasOwnProperty("error")) {
                            loaded = true;
                            _this._loadFromFamilyInfoJSON(response.responseJSON);
                        } else {
                            console.log("[LOAD] received family info JSON with an error: " + Helpers.stringifyObject(response.responseJSON));
                        }
                    } else {
                        console.log("[LOAD] no family info JSON received");
                    }
                },
                onComplete: function() {
                    if (!loaded) {
                        console.log("No family data");
                        SaveLoadEngine._displayFamilyPedigreeInterfaceError(
                                {"errorMessage": "Family data is not available, pedigree can not be edited"},
                                "Error loading family data", "");
                    }
                },
                parameters: {"document_id": familyOrPatientId }
            });
        },

        _loadFromFamilyInfoJSON: function(responseJSON) {
            if (responseJSON) {
                console.log("[LOAD] received JSON: " + Helpers.stringifyObject(responseJSON));
                console.log("[LOAD] Family: " + Helpers.stringifyObject(responseJSON.family));
                console.log("[LOAD] Pedigree: " + Helpers.stringifyObject(responseJSON.pedigree));

                editor.getFamilyData().updateFromJSON(responseJSON.family);

                // display a warning if there is "sensitive information" associated with the family
                if (editor.getFamilyData().hasWarningMessage()) {
                    editor.getOkCancelDialogue().showCustomized(editor.getFamilyData().getWarningMessage(),"Attention: This pedigree contains sensitive information.", "OK", null);
                }

                try {
                    var addSaveEventOnceLoaded = function() {
                        // since we just loaded data from disk data in memory is equivalent to data on disk
                        editor.getUndoRedoManager().addSaveEvent();
                    }

                    // Auto-detect pedigree format: "internal" or "simpleJSON"
                    if (!responseJSON.pedigree.hasOwnProperty("GG")
                        && responseJSON.pedigree.hasOwnProperty("data")
                        && Array.isArray(responseJSON.pedigree.data)) {
                        // looks like SimpleJSON format
                        this.createGraphFromImportData(JSON.stringify(responseJSON.pedigree.data), "simpleJSON", undefined,
                                false, true, addSaveEventOnceLoaded, "familyPedigree");
                    } else {
                        // else: old internal format

                        // run migrator from older versions
                        // (some updates are done in JS, but eventually all are supposed to be moved to JAVA database migrators)
                        var updatedJSONData = editor.getVersionUpdater().updateToCurrentVersion(JSON.stringify(responseJSON.pedigree));

                        this.createGraphFromSerializedData(updatedJSONData,
                                false, true, addSaveEventOnceLoaded, "familyPedigree");
                    }
                } catch (error) {
                    console.log("[LOAD] error parsing pedigree JSON");
                    this.initializeNewPedigree();
                }
            } else {
                console.log("[LOAD] no pedigree defined, need to initialize from a template or import");
                this.initializeNewPedigree();
            }
        },

        initializeNewPedigree: function(showImportTab) {
            document.fire("pedigree:load:finish");
            editor.getTemplateImportSelector().show(showImportTab ? 1 : 0, false,
                "No pedigree is currently defined. Please select a template to start a pedigree, or import an existing pedigree",
                "box infomessage"
                );
        }
    });

    SaveLoadEngine._displayFamilyPedigreeInterfaceError = function(replyJSON, title, messageIntro, callWhenDone)
    {
        var errorMessage = replyJSON.errorMessage ? replyJSON.errorMessage : "Unknown problem";
        errorMessage = "<font color='#660000'>" + errorMessage + "</font><br/><br/>";
        if (replyJSON.errorType == "familyConflict") {
            //errorMessage += "(for now it is only possible to add persons who is not in another family to a family)";
        }
        if (replyJSON.errorType == "pedigreeConflict") {
            //errorMessage += "(for now it is only possible to add persons without an already existing pedigree to a family)";
        }
        if (replyJSON.errorType == "permissions") {
            //errorMessage += "(you need to have edit permissions for the patient to be able to add it to a family)";
        }
        //errorMessage = "<font color='#660000'>" + errorMessage + "</font><br><br>";
        editor.getOkCancelDialogue().showError('<br/>' + messageIntro + errorMessage, title, "OK", callWhenDone );
    }

    return SaveLoadEngine;
});
