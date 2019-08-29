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
        getPedigreePhenotipsJSON: function() {
            var jsonObject = editor.getGraph().toJSONObject();

            jsonObject["settings"] = editor.getView().getSettings();

            return JSON.stringify(jsonObject);
        },

        createGraphFromUndoRedoState: function(undoRedoState, eventName) {
            // a shortcut loader which avoids error checkinhg and ensures node IDs are preserved

            var changeSet = editor.getGraph().fromUndoRedoState(undoRedoState);

            if (changeSet == null) {
                throw "unable to create a pedigree from undo/redo state";
            }

            editor.getView().applyChanges(changeSet, false);
        },

        createGraphFromStoredData: function(JSONString, dataSource, markAsEquivalentToSaved) {
            var addSaveEventOnceLoaded = function() {
                if (markAsEquivalentToSaved) {
                    // since we just loaded data from disk, data in memory is equivalent to data on disk
                    editor.getUndoRedoManager().addSaveEvent();
                }
            }
            this.createGraphFromImportData(JSONString, "phenotipsJSON", null, false, true, dataSource, "pedigreeLoaded", addSaveEventOnceLoaded);
        },

        createGraphFromImportData: function(importString, importType, importOptions, noUndo, centerAroundProband, dataSource, eventName, callbackWhenDataLoaded) {
            console.log("---- load/import: parsing data ----");
            document.fire("pedigree:blockinteraction:start");

            try {
                var changeSet = editor.getGraph().fromImport(importString, importType, importOptions);
                if (changeSet == null) {
                    throw "unable to create a pedigree from imported data";
                }

                if (importType == "phenotipsJSON") {
                    // load/process metadata such as pedigree options and color choices (which is an extension of top of base pedigree)
                    var jsonObject = JSON.parse(importString);
                    if (jsonObject.hasOwnProperty("settings")) {
                        editor.getView().loadSettings(jsonObject.settings);
                    }
                }
            }
            catch(err)
            {
                console.log("ERROR importing pedigree: " + err);
                alert("Error importing pedigree: " + err);
                document.fire("pedigree:blockinteraction:finish");

                // if there is no pedigree and import was used to initialize a pedigree need to display the import dialogue again
                if (!editor.pedigreeExists()) {
                    this.initializeNewPedigree(eventName && (eventName != "pedigreeLoaded") /* go to import tab if the operation was an import*/);
                }
                return;
            }

            if (!eventName) {
                eventName = "pedigreeImported";
            }
            this._finalizeCreateGraph(eventName, changeSet, noUndo, centerAroundProband, (callbackWhenDataLoaded ? callbackWhenDataLoaded : null), dataSource);
        },

        // common code for pedigree creation called after the actual pedigree has been initialized using whatever input data
        _finalizeCreateGraph: function(eventName, changeSet, noUndo, centerAroundProband, callbackWhenDataLoaded, dataSource) {

            var _this = this;

            // assigns node properties, either using loaded patient JSONs, or stored "raw JSON"s
            var finalizeCreation = function(loadedPatientData) {

                try {

                    // 1. for nodes linked to PT patients, update stored "raw JSON"s with the loaded values
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

                                if (!patientJSONObject) {
                                    // no data for this patient: it is ok just don't update the stored RAW JSON
                                    // (the already stored RAW JSON is the JSON found within loaded pedigree JSON, it may
                                    // be out of date, but can still be used to render the pedigree node linked to the patient)
                                    // (may happen if current user has no view rights for the patient)
                                    continue;
                                }

                                if (!allLinkedNodes.patientToNodeMapping.hasOwnProperty(patient)) {
                                  continue;
                                }
                                var nodeID = allLinkedNodes.patientToNodeMapping[patient];

                                editor.getGraph().setRawJSONProperties(nodeID, patientJSONObject);
                            }
                        }
                    }

                    // 2. for all node use stored "raw JSON" values to set pedigree properties
                    var unsupportedVersions = {};
                    var allPersonNodes = editor.getGraph().getAllPersonIDs();
                    for (var i = 0; i < allPersonNodes.length; i++) {
                        var nodeID = allPersonNodes[i];

                        var rawJSON = editor.getGraph().getRawJSONProperties(nodeID);

                        editor.getGraph().setPersonNodeDataFromPhenotipsJSON(nodeID, rawJSON);

                        // do some basic version checking
                        if (rawJSON.hasOwnProperty("phenotips_version")
                            && !PhenotipsJSON.isVersionSupported(rawJSON.phenotips_version)) {
                            if (!unsupportedVersions.hasOwnProperty(rawJSON.phenotips_version)) {
                                unsupportedVersions[rawJSON.phenotips_version] = [];
                            }
                            unsupportedVersions[rawJSON.phenotips_version].push(nodeID);
                        }
                    }

                    if (!Helpers.isObjectEmpty(unsupportedVersions)) {
                        var warningString = "Some of the versions of loaded Patient JSONs are not supported:";
                        for (var version in unsupportedVersions) {
                            if (unsupportedVersions.hasOwnProperty(version)) {
                                warningString += "\nversion " + version;
                            }
                        }
                        alert(warningString);
                    }

                    // 3. add patients that are not in pedigree to the patient legend
                    for (var i = 0; i < changeSet.unlinked.length; i++) {
                        var nextMemberID = changeSet.unlinked[i];
                        editor.getPatientLegend().addCase(nextMemberID, {});
                    }

                    // 4. synchronize the list of unlinked patients between the legend and the model:
                    //    if any patients were added or removed to the legend at step 3) those are already
                    //    synchronized, but it is possible a patient was in the legend before step 3, and
                    //    the operation of loading a pedigree reset the list of unlinked patients in the model
                    editor.getPatientLegend().updateDataModel();

                    // new loaded data may take up some space below some nodes, so need to recompute vertical positioning
                    editor.getGraph().updateYPositioning();

                    if (editor.getView().applyChanges(changeSet, false)) {
                        editor.getWorkspace().adjustSizeToScreen();
                    }

                    if (!noUndo && !editor.isReadOnlyMode()) {
                        var undoRedoState = editor.getGraph().toUndoRedoState();
                        editor.getUndoRedoManager().addState({"eventName": eventName}, null, undoRedoState);
                    }

                    callbackWhenDataLoaded && callbackWhenDataLoaded();

                    if (centerAroundProband) {
                        editor.getWorkspace().centerAroundNode(editor.getGraph().getProbandId());
                    }
                } catch (err) {
                    var content = new Element('div', {'class' : 'box errormessage'});
                    content.insert(new Element('p').update(new Element('strong').update("PEDIGREE INITIALIZATION FAILED")));
                    content.insert(new Element('p').update("<br>This " + editor.getExternalEndpoint().getParentDocument().type.toLowerCase()
                            + " has a pedigree but there was an internal error loading it.<br><br>"
                            + "Please notify your PhenoTips administrator of this error."));
                    var d = new PhenoTips.widgets.ModalPopup(content, '', {'titleColor' : '#000'});
                    d.show();
                }

                document.fire("pedigree:blockinteraction:finish");
                document.fire("pedigree:initialization:finish");
            };

            if (!noUndo && !editor.isReadOnlyMode()) {
                //----------------------------------------------------------------------------------------
                // update all linked patient records by loading their current (JSON) data from PhenoTips
                //
                // TODO: if/when pedigree JSON is updated whenever any of the patients are updated there will
                //  be no more need to load patient JSONs separately. However patient modification is a very
                //  common operation which may be up to 2x slower if pedigree JSON should be updated as well,
                //  so there is a good (performance-related) reason not to implement that change
                //----------------------------------------------------------------------------------------

                // include all patients linked to all pedigree nodes
                var allLinkedNodes = editor.getGraph().getAllPatientLinks();

                // ...add all loaded unlinked patients
                var patientList = allLinkedNodes.linkedPatients.concat(changeSet.unlinked);

                // ...and add patients currently in the legend - e.g. when creating a new pedigree for a patient from a
                // template the current patient will be in the legend but not in the pedigree and not in the loaded unlinked list
                patientList = Helpers.filterUnique(patientList.concat(editor.getPatientLegend().getListOfPatientsInTheLegend()));

                // load data and initialize pedigree once data is loaded
                editor.getPatientDataLoader().load(patientList, finalizeCreation /* callback when data is loaded */);
                //----------------------------------------------------------------------------------------
            } else {
                finalizeCreation(null /* do not update nodes using data loaded from PhenoTips */);
            }
        },

        save: function(callAfterSuccessfulSave, callAfterFailedSave) {
            if (this._saveInProgress) {
                return;   // Don't send parallel save requests
            }

            var savingNotification = new XWiki.widgets.Notification("Saving", "inprogress");

            var onSaveFailed = function (errorMessage) {
                savingNotification.replace(new XWiki.widgets.Notification("Save failed: " + errorMessage));
                var content = new Element('div', {'class' : 'box errormessage'});
                content.insert(new Element('p').update(new Element('strong').update("SAVE FAILED: " + errorMessage)));
                content.insert(new Element('p').update("YOUR PEDIGREE IS NOT SAVED. To avoid losing your work, we recommend taking a screenshot of the pedigree and exporting the pedigree data as simple JSON using the 'Export' option in the menu at the top of the pedigree editor. Please notify your PhenoTips administrator of this error."));
                var d = new PhenoTips.widgets.ModalPopup(content, '', {'titleColor' : '#000'});
                d.show();
            }

            try {
                editor.getView().unmarkAll();

                var me = this;

                me._notSaved = true;

                var jsonData = this.getPedigreePhenotipsJSON();

                console.log("[SAVE] data: " + Helpers.stringifyObject(jsonData));

                var svg = editor.getWorkspace().getSVGCopy();
                var svgText = svg.getSVGText();

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
                        document.fire("pedigree:blockinteraction:start", {"message": "Saving pedigree..."});
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
                        document.fire("pedigree:blockinteraction:finish");
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
                      onSaveFailed(errorMessage);
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
            } catch (err) {
                onSaveFailed("internal pedigree error");
            }
        },

        load: function(familyOrPatientId) {
            console.log("Initiating load process...");
            var familyJsonURL = editor.getExternalEndpoint().getFamilyInfoURL();
            var loaded = false;
            var _this = this;
            new Ajax.Request(familyJsonURL, {
                method: "POST",
                onCreate: function() {
                    document.fire("pedigree:blockinteraction:start", {"message": "Loading pedigree..."});
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

        _showSensitiveDataWarning: function(showDetails) {
            var title = "Attention: sensitive information";
            var message = "This pedigree contains sensitive information.";
            var warningMessage = editor.getFamilyData().getWarningMessage();
            var continueMessage = "Continue to pedigree";
            var cancelMessage = editor.isFamilyPage() ? "Go to family record" : "Go to patient record";
            var toggleDetailsButtonText = showDetails ? "Hide details" : "Show details";
            var toggleDetails = this._showSensitiveDataWarning.bind(this, !showDetails);
            var options = { "screenBackground": "opaque", "button1": { "keepBackgroundScreenWhenPressed": true } };

            // Just append warning message to dialog mody if showing details
            if (showDetails && warningMessage) {
                message += "<br /><br />" + warningMessage;
            }

            // Don't display toggleDetails button if there are no details to display
            if (!warningMessage) {
                toggleDetailsButtonText = null;
                toggleDetails = null;
            }

            editor.getOkCancelDialogue().showWithOptions(
                message,
                title,
                options,
                continueMessage, null,
                cancelMessage, editor.closePedigree,
                toggleDetailsButtonText, toggleDetails);
        },

        _loadFromFamilyInfoJSON: function(responseJSON) {
            if (responseJSON) {
                console.log("[LOAD] received JSON: " + Helpers.stringifyObject(responseJSON));
                console.log("[LOAD] Family: " + Helpers.stringifyObject(responseJSON.family));
                console.log("[LOAD] Pedigree: " + Helpers.stringifyObject(responseJSON.pedigree));

                editor.getFamilyData().updateFromJSON(responseJSON.family);


                if (responseJSON.hasOwnProperty("pedigree")) {
                    try {
                        // note: supporting only new full JSON format

                        // TODO: avoid unnecessary JSON -> string -> JSON conversions
                        var updatedJSONData = JSON.stringify(responseJSON.pedigree);

                        var markAsEquivalentToSaved = true;

                        // if pedigree is misformatted createGraphFromStoredData() may throw as well
                        this.createGraphFromStoredData(updatedJSONData, "familyPedigree" /* data source */, markAsEquivalentToSaved);

                        // display a warning if there is "sensitive information" associated with the family
                        if (editor.getFamilyData().hasWarningMessage()) {
                            this._showSensitiveDataWarning(false);
                        }

                        return;
                    } catch (error) {
                        console.log("[LOAD] error parsing pedigree JSON");
                    }
                }
            }

            console.log("[LOAD] no pedigree defined, need to initialize from a template or import");
            this.initializeNewPedigree();
        },

        initializeNewPedigree: function(showImportTab) {
            document.fire("pedigree:blockinteraction:finish");
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
