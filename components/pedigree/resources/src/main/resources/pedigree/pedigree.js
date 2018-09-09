/**
 * The main class of the Pedigree Editor, responsible for initializing all the basic elements of the app.
 * Contains wrapper methods for the most commonly used functions.
 * This class should be initialized only once.
 *
 * @class PedigreeEditor
 * @constructor
 */
define([
        "pedigree/extensionManager",
        "pedigree/externalEndpoints",
        "pedigree/externalIdManager",
        "pedigree/undoRedoManager",
        "pedigree/controller",
        "pedigree/pedigreeEditorParameters",
        "pedigree/preferencesManager",
        "pedigree/familyData",
        "pedigree/patientDataLoader",
        "pedigree/saveLoadEngine",
        "pedigree/view",
        "pedigree/nodeMenuFields",
        "pedigree/model/dynamicGraph",
        "pedigree/model/helpers",
        "pedigree/model/patientRecordData",
        "pedigree/view/workspace",
        "pedigree/view/disorderLegend",
        "pedigree/view/exportSelector",
        "pedigree/view/candidateGeneLegend",
        "pedigree/view/causalGeneLegend",
        "pedigree/view/rejectedGeneLegend",
        "pedigree/view/rejectedCandidateGeneLegend",
        "pedigree/view/carrierGeneLegend",
        "pedigree/view/hpoLegend",
        "pedigree/view/patientDropLegend",
        "pedigree/view/importSelector",
        "pedigree/view/cancersLegend",
        "pedigree/view/nodeMenu",
        "pedigree/view/deceasedMenu",
        "pedigree/view/nodetypeSelectionBubble",
        "pedigree/view/okCancelDialogue",
        "pedigree/view/saveLoadIndicator",
        "pedigree/view/templateSelector",
        "pedigree/view/tabbedSelector",
        "pedigree/view/printDialog",
        "pedigree/view/addMemberDialog",
        "pedigree/view/studySelectionDialog"
    ],
    function(
        PedigreeExtensionManager,
        ExternalEndpointsManager,
        ExternalIdManager,
        UndoRedoManager,
        Controller,
        PedigreeEditorParameters,
        PreferencesManager,
        FamilyData,
        PatientDataLoader,
        SaveLoadEngine,
        View,
        NodeMenuFields,
        DynamicPositionedGraph,
        Helpers,
        PatientRecordData,
        Workspace,
        DisorderLegend,
        ExportSelector,
        CandidateGeneLegend,
        CausalGeneLegend,
        RejectedGeneLegend,
        RejectedCandidateGeneLegend,
        CarrierGeneLegend,
        HPOLegend,
        PatientDropLegend,
        ImportSelector,
        CancerLegend,
        NodeMenu,
        DeceasedMenu,
        NodetypeSelectionBubble,
        OkCancelDialogue,
        SaveLoadIndicator,
        TemplateSelector,
        TabbedSelector,
        PrintDialog,
        AddNewMemberDialog,
        StudySelectionDialog
){

    var PedigreeEditor = Class.create({
        initialize: function() {
            this.DEBUG_MODE = false;

            window.editor = this;

            this._extensionManager = new PedigreeExtensionManager();
            this._externalEndpointManager = new ExternalEndpointsManager();

            // Available options:
            //
            //  nonStandardAdoptedOutGraphic: {true|false}   - use out-brackets for adopted out persons; default "false"
            //  hideDraggingHint:             {true|false}   - do not display the hint on top of the legend; default "false"
            //  propagateFatherLastName:      {true|false}   - auto-propagate father's last name or not; default: "true"
            //  dateDisplayFormat:            {"MDY"|"DMY"|"MY"|"Y"|"MMY"}  - date display format; default "MDY"; MY = "02-2015", MMY = "Feb 2015"
            //  dateEditFormat:               {"YMD"|"DMY"|"MY"|"Y"}  - defines order of fields in the date picker; default "YMD"
            //  drawNodeShadows:              {true|false}   - display small shadow under node graphic; default: "true"
            //  disabledFields:               [array]        - list of node-menu fields disabled for this installation
            //  replaceIdWithExternalID:      {true|false}   - when true, patient links display external ID as the link text (instead of PT ids)
            //  uniqueExternalID:             {true|false}   - when true, patient records will not be allowed to have duplicate external IDs
            //  displayCancerLabels:          {true|false}   - display labels for each affecting cancer; default: "true"
            //  lineStyle:                    {"thin"|"regular"|"bold"} - controls the thickness of all lines in pedigree
            //  studies:                      [array]        - array of available study objects in the following format: {"id": ..., "name": ..., "description": ...}
            //  advancedUser:                 {true|false}   - some lesser used/more advanced/deprecated UI elements may only be shown to "advanced" users
            //
            this._defaultPreferences = { global:   { nonStandardAdoptedOutGraphic: false,
                                                     propagateFatherLastName: true,
                                                     dateDisplayFormat: "YMD",
                                                     dateEditFormat: "YMD",
                                                     drawNodeShadows: true,
                                                     disabledFields: [],
                                                     requiredFields: [],
                                                     replaceIdWithExternalID: false,
                                                     uniqueExternalID: false,
                                                     displayCancerLabels: true,
                                                     lineStyle: "regular",
                                                     studies: []},
                                         user:     { hideDraggingHint: false,
                                                     hidePatientDraggingHint: false,
                                                     hideShareConsentDialog: false,
                                                     firstName: "",
                                                     lastName: "",
                                                     advancedUser: false },
                                         pedigree: {}
                                       };
            this._preferencesManager = new PreferencesManager(this._defaultPreferences);

            // initialize main data structure which holds the graph structure
            this._graphModel = DynamicPositionedGraph.makeEmpty();

            //initialize the elements of the app
            this._workspace = new Workspace();

            this._nodetypeSelectionBubble = new NodetypeSelectionBubble(false);
            this._siblingSelectionBubble  = new NodetypeSelectionBubble(true);
            this._okCancelDialogue = new OkCancelDialogue();

            this._view = new View();

            this._actionStack = new UndoRedoManager();
            this._saveLoadIndicator = new SaveLoadIndicator();
            this._saveLoadEngine = new SaveLoadEngine();
            this._familyData = new FamilyData();
            this._patientDataLoader = new PatientDataLoader();
            this._patientRecordData = new PatientRecordData();
            this._externalIdManager = new ExternalIdManager();

            // load global pedigree preferences before a specific pedigree is loaded, since
            // preferences may affect the way it is rendered. Once preferences are loaded the
            // provided function will get execute and will load/render the rest of the pedigree.
            this._preferencesManager.load( function() {
                    Helpers.copyProperties(PedigreeEditorParameters.styles.blackAndWhite, PedigreeEditorParameters.attributes);
                    // set up constants which depend on preferences
                    if (editor.getPreferencesManager().getConfigurationOption("lineStyle") == "thin") {
                        Helpers.copyProperties(PedigreeEditorParameters.lineStyles.thinLines, PedigreeEditorParameters.attributes);
                    } else if (editor.getPreferencesManager().getConfigurationOption("lineStyle") == "bold") {
                        Helpers.copyProperties(PedigreeEditorParameters.lineStyles.boldLines, PedigreeEditorParameters.attributes);
                    } else {
                        Helpers.copyProperties(PedigreeEditorParameters.lineStyles.regularLines, PedigreeEditorParameters.attributes);
                    }

                    var _importSelector = new ImportSelector();
                    var _templateSelector = new TemplateSelector();
                    this._templateImportSelector = new TabbedSelector("Select a pedigree template or import a pedigree",
                                                                      [_templateSelector, _importSelector]);

                    // generate various dialogues after preferences have been loaded
                    this._disorderLegend = new DisorderLegend();
                    this._candidateGeneLegend = new CandidateGeneLegend();
                    this._causalGeneLegend = new CausalGeneLegend();
                    this._rejectedGeneLegend = new RejectedGeneLegend();
                    this._rejectedCandidateGeneLegend = new RejectedCandidateGeneLegend();
                    this._carrierGeneLegend = new CarrierGeneLegend();
                    this._hpoLegend = new HPOLegend();
                    this._cancerLegend = new CancerLegend();
                    this._patientLegend = new PatientDropLegend();
                    this._addNewMember = new AddNewMemberDialog();

                    this._nodeMenu = this.generateNodeMenu();
                    this._deceasedMenu = this.generateDeceasedMenu();
                    this._nodeGroupMenu = this.generateNodeGroupMenu();
                    this._newPersonMenu = this.generateNewPersonMenu();
                    this._partnershipMenu = this.generatePartnershipMenu();
                    this._exportSelector = new ExportSelector();
                    this._printDialog = new PrintDialog();
                    this._studySelectionDialog = new StudySelectionDialog();

                    var newPatientId = window.self.location.href.toQueryParams().new_patient_id;

                    // finally, initialize the pedigree (if exists load from disk, otherwise from template or from import)
                    if (newPatientId && newPatientId != "") {
                        // since this is a new patient, we do not want to load that patient's data, we explicitly
                        // want to load the family document (either new family or existing), and add new patient
                        // to the un-linked patients legend

                        // note: in case it is a new family and a template is loaded, patient may be auto-assigned
                        // to a node in the template - for that to happen it should be added to the patient legend
                        // before the template is loaded
                        this.getPatientLegend().addCase(newPatientId);

                        this._saveLoadEngine.load(XWiki.currentDocument.page);

                        // normally once a pedigree is loaded from disk it is assumed to have no changes,
                        // but when a new patient is added to an existing family there is an unsaved change
                        // (and it is easier to mark it as such than to complicate the logic in the load code)
                        var markAsUnsaved = function() {
                            editor.getUndoRedoManager().markAsUnsaved();
                            document.stopObserving("pedigree:initialization:finish", markAsUnsaved);
                        }
                        document.observe("pedigree:initialization:finish", markAsUnsaved);
                    } else {
                        var documentId = editor.getGraph().getCurrentPatientId();
                        this._saveLoadEngine.load(documentId);
                    }

                    // regardless of the way pedigree is initialized (loaded from disk, via a template, etc.)
                    // and regardless if it was done from a patient page or a family page, make sure all family
                    // members not already in the pedigree are added as un-linked members
                    // (e.g. when a pedigree is created for a family which already has members but no pedigree)
                    var addMissingFamilyMembers = function() {
                        // the list of linked patient records that pedigree already knows about:
                        var linkedPatients = Helpers.toObjectWithTrue(editor.getAllLinkedPatients());
                        // the list of patients that family page has:
                        var membersInTheFamily = editor.getFamilyData().getLoadedFamilyMembers();
                        membersInTheFamily.forEach(function(familyMember) {
                            var phenotipsID = familyMember.id;
                            if (!linkedPatients.hasOwnProperty(phenotipsID)) {
                                editor.getPatientLegend().addCase(phenotipsID);
                            }
                        });
                        document.stopObserving("pedigree:initialization:finish", addMissingFamilyMembers);
                    }
                    document.observe("pedigree:initialization:finish", addMissingFamilyMembers);

                }.bind(this) );

            this._controller = new Controller();

            //attach actions to buttons on the top bar
            var undoButton = $('action-undo');
            undoButton && undoButton.on("click", function(event) {
                document.fire("pedigree:undo");
            });
            var redoButton = $('action-redo');
            redoButton && redoButton.on("click", function(event) {
                document.fire("pedigree:redo");
            });

            var autolayoutButton = $('action-layout');
            autolayoutButton && autolayoutButton.on("click", function(event) {
                document.fire("pedigree:autolayout");
            });
            var clearButton = $('action-clear');
            clearButton && clearButton.on("click", function(event) {
                document.fire("pedigree:graph:clear");
            });

            var replacePedigreeWarning = "Existing pedigree will be replaced by the selected one";
            var templatesButton = $('action-templates');
            templatesButton && templatesButton.on("click", function(event) {
                editor.getTemplateImportSelector().show(0 /* tab 0 - templates */, true /* allow cancel */, replacePedigreeWarning, "box warningmessage");
            });
            var importButton = $('action-import');
            importButton && importButton.on("click", function(event) {
                editor.getTemplateImportSelector().show(1 /* tab1 - import */, true /* allow cancel */, replacePedigreeWarning, "box warningmessage");
            });
            var exportButton = $('action-export');
            exportButton && exportButton.on("click", function(event) {
                editor.getExportSelector().show();
            });
            var printButton = $('action-print');
            printButton && printButton.on("click", function(event) {
                editor.getPrintDialog().show();
            });

            var saveButton = $('action-save');
            saveButton && saveButton.on("click", function(event) {
                editor.checkAndSaveOrQuit(false /* user doesnot want to quit */);
            });

            var onLeavePageFunc = function() {
                if (!editor.isReadOnlyMode() && editor.getUndoRedoManager().hasUnsavedChanges()) {
                    return "All changes will be lost when navigating away from this page.";
                }
            };
            if (!editor.isReadOnlyMode()) {
                window.onbeforeunload = onLeavePageFunc;
            }

            var onCloseButtonClickFunc = function(event) {
                if (editor.isReadOnlyMode()) {
                    this.closePedigree();
                } else {
                    editor.checkAndSaveOrQuit(true /* user wanted to quit */);
                }
            };
            var closeButton = $('action-close');
            closeButton && (closeButton.onclick = onCloseButtonClickFunc);

            var renumberButton = $('action-number');
            renumberButton && renumberButton.on("click", function(event) {
                document.fire("pedigree:renumber");
            });

            var unsupportedBrowserButton = $('action-readonlymessage');
            unsupportedBrowserButton && unsupportedBrowserButton.on("click", function(event) {
                alert("Your browser does not support all the features required for " +
                      "Pedigree Editor, so pedigree is displayed in read-only mode (and may have quirks).\n\n" +
                      "Supported browsers include Firefox v3.5+, Internet Explorer v9+, " +
                      "Chrome, Safari v4+, Opera v10.5+ and most mobile browsers.");
            });

            //this.startAutoSave(30);
        },

        /**
         * Closes pedigree editor and navigates to the proper page (patient of family, view or edit mode)
         * depending on where the editor was called from
         *
         * @method closePedigree
         */
        closePedigree: function() {
            if (!editor.isReadOnlyMode()) {
                window.onbeforeunload = undefined;
            }
            editor.getExternalEndpoint().redirectToURL(editor.getExternalEndpoint().getParentDocument().returnURL);
        },

        /**
         * Either offers to save the pedigree when the user wants to quit (with unsaved changes), or to keep
         * editing if saving would result in a possibly unintended pedigree (with missing patients).
         *
         * The checks are combined to avoid having two dialogues one after the other (in case of quit
         * with unsaved changes AND unintended pedigree: first, "save?", then "save will result
         * in strange pedigree, proceed?")
         *
         * @method checkAndSaveOrQuit
         */
        checkAndSaveOrQuit: function(userWantsToQuit) {

            var patientLinks = editor.getGraph().getAllPatientLinks();
            var currentPatientId = editor.getGraph().getCurrentPatientId();

            var noPatientsAreLinked = !editor.isFamilyPage()
                                      && (patientLinks.linkedPatients.length == 0)
                                      && (editor.getPatientLegend().getListOfPatientsInTheLegend().length == 0);

            var currentPatientIsNotLinked = !editor.isFamilyPage()
                                            && !patientLinks.patientToNodeMapping.hasOwnProperty(currentPatientId)
                                            && !editor.getPatientLegend().hasPatient(currentPatientId);

            var unsavedChanges = editor.getUndoRedoManager().hasUnsavedChanges();

            var noPatientsLinkedMessage = "There are no patients in this family.<br/><br/>";
            var currentPatientUnlinkedMessage = "Current patient is not a member of this family.<br/><br/>";

            //-----------------
            var saveWithChecks = function(quitAfterSave) {

                // when configured, do not allow saving pedigree if there are patient records with duplicate exterenal IDs
                if (editor.getPreferencesManager().getConfigurationOption("uniqueExternalID")) {
                    var duplicateIDReport = editor.getExternalIdManager().duplicateIDPresent();
                    if (duplicateIDReport.dupliucateIDPresent) {
                        if (duplicateIDReport.involvesExternalPatient) {
                            var duplicateIDMessage = "Can not save pedigree, a patient record in the pedigree has the same identifier (\""
                                                     + duplicateIDReport.id + "\") as some other patient record in the system";
                        } else {
                            var duplicateIDMessage = "Can not save pedigree, some patient records in the pedigree have the same identifier (\""
                                                     + duplicateIDReport.id + "\")";
                        }
                        editor.getOkCancelDialogue().showError(duplicateIDMessage, "Can not save pedigree", "OK", undefined);
                        return;
                    }
                }

                var saveAndQuitFunc = function() {
                    editor.getSaveLoadEngine().save(editor.closePedigree);
                }
                var saveAndKeepEditingFunc = function() {
                    editor.getSaveLoadEngine().save();
                }
                var removeFamily = function() {
                    var removed = false;
                    new Ajax.Request(editor.getExternalEndpoint().getFamilyRemoveURL(), {
                        method: "POST",
                        onSuccess: function(response) {
                            if (response.responseJSON && response.responseJSON.hasOwnProperty("success")) {
                                if (response.responseJSON.success) {
                                    removed = true;
                                    editor.closePedigree();
                                }
                            }
                        },
                        onComplete: function() {
                            if (!removed) {
                                console.log("Remove failed");
                                editor.getOkCancelDialogue().showError("Family removal failed", "Family removal failed", "OK", undefined);
                            }
                        },
                        parameters: {"family_id": editor.getFamilyData().getFamilyId() }
                    });
                }

                if (noPatientsAreLinked) {
                    if (quitAfterSave) {
                        editor.getOkCancelDialogue().showCustomized(noPatientsLinkedMessage +
                                "Do you want to remove the family or keep the family with no members?",
                                "Keep the family with no members?",
                                " Keep the family ", saveAndQuitFunc,
                                " Remove the family ", removeFamily,
                                " Keep editing pedigree ", undefined, true );
                    } else {
                        editor.getOkCancelDialogue().showCustomized(noPatientsLinkedMessage +
                                "Do you want to save this pedigree and get a family with no members?",
                                "Save with no members?",
                                "Keep editing pedigree", undefined,
                                "Save and get a family with no members", saveAndKeepEditingFunc);
                    }
                } else if (currentPatientIsNotLinked) {
                    if (quitAfterSave) {
                        editor.getOkCancelDialogue().showCustomized(currentPatientUnlinkedMessage +
                                "Do you want to remove current patient from the family?",
                                "Remove current patient from the family?",
                                "Keep editing pedigree", undefined,
                                "Close pedigree and remove current patient from the family", saveAndQuitFunc);
                    } else {
                        editor.getOkCancelDialogue().showCustomized(currentPatientUnlinkedMessage +
                                "Do you want to save the pedigree and remove current patient from the family?",
                                "Proceed with save?",
                                "Keep editing pedigree", undefined,
                                "Save pedigree with current patient excluded from the family", saveAndKeepEditingFunc);
                    }
                } else {
                    if (quitAfterSave) {
                        saveAndQuitFunc();
                    } else {
                        saveAndKeepEditingFunc();
                    }
                }
            }
            //-----------------

            if (userWantsToQuit) {
                if (unsavedChanges) {
                    var saveAndQuitFunc = function() {
                        saveWithChecks(true);
                    };
                    editor.getOkCancelDialogue().showCustomized('There are unsaved changes, do you want to save the pedigree before closing the pedigree editor?',
                            'Save before closing?',
                            " Save and quit ", saveAndQuitFunc,
                            " Don't save and quit ", editor.closePedigree,
                            " Keep editing pedigree ", undefined, true );
                } else {
                    editor.closePedigree();
                }
            } else {
                saveWithChecks(false);
            }
        },

        /**
         * @method getExtensionManager
         * @return {PedigreeExtensionManager}
         */
        getExtensionManager: function() {
            return this._extensionManager;
        },

        /**
         * Returns the version of the PhenoTips instance (used mainly for generating Patient JSONs)
         */
        getPhenotipsVersion: function() {
            return this.getPreferencesManager().getPhenotipsVersion();
        },

        /**
         * Returns a class managing external connections for pedigree editor, e.g. load/save URLs etc.
         *
         * @method getPreferencesManager
         * @return {ExternalEndpointsManager}
         */
        getExternalEndpoint: function() {
            return this._externalEndpointManager;
        },

        /**
         * @method getPreferencesManager
         * @return {PreferencesManager}
         */
        getPreferencesManager: function() {
            return this._preferencesManager;
        },

        /**
         * @method getPatientDataLoader
         * @return {PatientDataLoader}
         */
        getPatientDataLoader: function() {
            return this._patientDataLoader;
        },

        /**
         * @method getPatientRecordData
         * @return {PatientRecordData}
         */
        getPatientRecordData: function() {
            return this._patientRecordData;
        },

        /**
         * @method getgetExternalIdManager
         * @return {ExternalIdManager}
         */
        getExternalIdManager: function() {
            return this._externalIdManager;
        },

        /**
         * Returns false if pedigree is not defined
         *
         * @method pedigreeExists
         */
        pedigreeExists: function() {
            return this.getGraph().getMaxNodeId() >= 0;
        },

        /**
         * Returns the graph node with the corresponding nodeID
         * @method getNode
         * @param {Number} nodeID The id of the desired node
         * @return {AbstractNode} the node whose id is nodeID
         */
        getNode: function(nodeID) {
            return this.getView().getNode(nodeID);
        },

        /**
         * @method getView
         * @return {View} (responsible for managing graphical representations of nodes and interactive elements)
         */
        getView: function() {
            return this._view;
        },

        /**
         * @method getGraph
         * @return {DynamicPositionedGraph} (data model: responsible for managing nodes and their positions)
         */
        getGraph: function() {
            return this._graphModel;
        },

        /**
         * @method getController
         * @return {Controller} (responsible for managing user input and corresponding data changes)
         */
        getController: function() {
            return this._controller;
        },

        /**
         * @method getUndoRedoManager
         * @return {UndoRedoManager} (responsible for undoing and redoing actions)
         */
        getUndoRedoManager: function() {
            return this._actionStack;
        },

        /**
         * @method getOkCancelDialogue
         * @return {OkCancelDialogue} (responsible for displaying ok/cancel prompts)
         */
        getOkCancelDialogue: function() {
            return this._okCancelDialogue;
        },

        /**
         * @method getNodetypeSelectionBubble
         * @return {NodetypeSelectionBubble} (floating window with initialization options for new nodes)
         */
        getNodetypeSelectionBubble: function() {
            return this._nodetypeSelectionBubble;
        },

        /**
         * @method getSiblingSelectionBubble
         * @return {NodetypeSelectionBubble} (floating window with initialization options for new sibling nodes)
         */
        getSiblingSelectionBubble: function() {
            return this._siblingSelectionBubble;
        },

        /**
         * @method getWorkspace
         * @return {Workspace}
         */
        getWorkspace: function() {
            return this._workspace;
        },

        /**
         * @method getDisorderLegend
         * @return {Legend} Responsible for managing and displaying the disorder legend
         */
        getDisorderLegend: function() {
            return this._disorderLegend;
        },

        /**
         * @method getHPOLegend
         * @return {Legend} Responsible for managing and displaying the phenotype/HPO legend
         */
        getHPOLegend: function() {
            return this._hpoLegend;
        },

        /**
         * @method getGeneLegend
         * @return {Legend} Responsible for managing and displaying the genes having this status.
         *                  May be null for statuses with no legend.
         */
        getGeneLegend: function(geneStatus) {
            if (geneStatus == "candidate") {
                return this.getCandidateGeneLegend();
            } else if (geneStatus == "solved") {
                return this.getCausalGeneLegend();
            } else if (geneStatus == "rejected") {
                return this.getRejectedGeneLegend();
            } else if (geneStatus == "rejected_candidate") {
                return this.getRejectedCandidateGeneLegend();
            } else if (geneStatus == "carrier") {
                return this.getCarrierGeneLegend();
            }
            return null;
        },

        /**
         * @method getCausalGeneLegend
         * @return {Legend} Responsible for managing and displaying the causal genes
         */
        getCausalGeneLegend: function() {
            return this._causalGeneLegend;
        },

        /**
         * @method getCandidateGeneLegend
         * @return {Legend} Responsible for managing and displaying the candidate genes
         */
        getCandidateGeneLegend: function() {
            return this._candidateGeneLegend;
        },

        /**
         * @method getRejectedGeneLegend
         * @return {Legend} Responsible for managing tested negative genes
         */
        getRejectedGeneLegend: function() {
            return this._rejectedGeneLegend;
        },

        /**
         * @method getRejectedCandidateGeneLegend
         * @return {Legend} Responsible for managing rejected candidate genes
         */
        getRejectedCandidateGeneLegend: function() {
            return this._rejectedCandidateGeneLegend;
        },

        /**
         * @method getCarrierGeneLegend
         * @return {Legend} Responsible for managing carrier genes
         */
        getCarrierGeneLegend: function() {
            return this._carrierGeneLegend;
        },

        /**
         * @method getCancerLegend
         * @return {Legend} Responsible for managing and displaying the common cancers legend
         */
        getCancerLegend: function() {
            return this._cancerLegend;
        },

        /**
         * Returns a list of all available legends
         */
        getAllLegends: function() {
            return [ this.getCancerLegend(),
                     this.getHPOLegend(),
                     this.getDisorderLegend(),
                     this.getCarrierGeneLegend(), this.getRejectedGeneLegend(), this.getCandidateGeneLegend(), this.getCausalGeneLegend() ];
        },

        /**
         * @method getPaper
         * @return {Workspace.paper} Raphael paper element
         */
        getPaper: function() {
            return this.getWorkspace().getPaper();
        },

        /**
         * @method getAddNewMemberDialog
         * @return {Dialog} A dialog for adding new members to the family
         */
        getAddNewMemberDialog: function() {
            return this._addNewMember;
        },

        /**
         * @method getPatientLegend
         * @return {Legend} Responsible for managing and displaying legend for patients that are unassigned to a family
         */
        getPatientLegend: function() {
            return this._patientLegend;
        },

        /**
         * @method isReadOnlyMode
         * @return {Boolean} True iff pedigree drawn should be read only with no handles
         *                   (read-only mode is used for IE8 as well as for template display and
         *                   print and export versions).
         */
        isReadOnlyMode: function() {
            if (this.isUnsupportedBrowser()) return true;
            return false;
        },

        isUnsupportedBrowser: function() {
            // http://voormedia.com/blog/2012/10/displaying-and-detecting-support-for-svg-images
            if (!document.implementation.hasFeature("http://www.w3.org/TR/SVG11/feature#BasicStructure", "1.1")) {
                // implies unpredictable behavior when using handles & interactive elements,
                // and most likely extremely slow on any CPU
                return true;
            }
            // http://kangax.github.io/es5-compat-table/
            if (!window.JSON) {
                // no built-in JSON parser - can't proceed in any way; note that this also implies
                // no support for some other functions such as parsing XML.
                //
                // TODO: include free third-party JSON parser and replace XML with JSON when loading data;
                //       (e.g. https://github.com/douglascrockford/JSON-js)
                //
                //       => at that point all browsers which suport SVG but are treated as unsupported
                //          should theoreticaly start working (FF 3.0, Safari 3 & Opera 9/10 - need to test).
                //          IE7 does not support SVG and JSON and is completely out of the running;
                alert("Your browser is not supported and is unable to load and display any pedigrees.\n\n" +
                      "Suported browsers include Internet Explorer version 9 and higher, Safari version 4 and higher, "+
                      "Firefox version 3.6 and higher, Opera version 10.5 and higher, any version of Chrome and most "+
                      "other modern browsers (including mobile). IE8 is able to display pedigrees in read-only mode.");
                window.stop && window.stop();
                return true;
            }
            return false;
        },

        /**
         * @method getSaveLoadEngine
         * @return {SaveLoadEngine} Engine responsible for saving and loading operations
         */
        getSaveLoadEngine: function() {
            return this._saveLoadEngine;
        },

        /**
         * @return {FamilyData} containign information about the current family, as of last load.
         */
        getFamilyData: function() {
            return this._familyData;
        },

        /**
         * True iff current pedigree belongs toa family page, not a patient
         * @method isFamilyPage
         * @return {boolean}
         */
        isFamilyPage: function() {
            return this.getFamilyData().isFamilyPage();
        },

        /**
         * Returns a list of of all PhenoTips patient record IDs currently present in the family.
         * @method getAllLinkedPatients
         * @return {Array}
         */
        getAllLinkedPatients: function() {
            var allLinkedNodes = editor.getGraph().getAllPatientLinks();
            var patientList = Helpers.filterUnique(allLinkedNodes.linkedPatients.concat(editor.getPatientLegend().getListOfPatientsInTheLegend()));
            return patientList;
        },

        /**
         * Returns iff the given patient is already a member of the current family
         */
        isFamilyMember: function(patientID) {
            return Helpers.arrayContains(this.getAllLinkedPatients(), patientID);
        },

        /**
         * Returns permission object which as "hasEdit" and "hasView" fields.
         * @method getPatientAccessPermissions
         * @return {Object}
         */
        getPatientAccessPermissions: function(patientID) {
            var permissions = (this.getFamilyData().isFamilyMember(patientID))
                              ? this.getFamilyData().getPatientAccessPermissions(patientID) : null;
            if (permissions == null) {
                permissions = { "hasEdit": true, "hasView": true };
            }
            return permissions;
        },

        /**
         * @method getTemplateImportSelector
         * @return {TemplateSelector}
         */
        getTemplateImportSelector: function() {
            return this._templateImportSelector;
        },

        /**
         * @method getExportSelector
         * @return {ExportSelector}
         */
        getExportSelector: function() {
            return this._exportSelector;
        },

        /**
         * @method getPrintDialog
         * @return {PrintDialog}
         */
        getPrintDialog: function() {
            return this._printDialog;
         },

        /**
         * Returns true if any of the node menus are visible
         * (since some UI interactions should be disabled while menu is active - e.g. mouse wheel zoom)
         *
         * @method isAnyMenuVisible
         */
        isAnyMenuVisible: function() {
            if (this.isReadOnlyMode()) {
                return false;
            }
            if (this.getNodeMenu().isVisible() || this.getNodeGroupMenu().isVisible() || this.getPartnershipMenu().isVisible()) {
                return true;
            }
            return false;
        },

        /**
         * Creates the context menu for Person nodes
         *
         * @method generateNodeMenu
         * @return {NodeMenu}
         */
        generateNodeMenu: function() {
            if (this.isReadOnlyMode()) return null;
            var personMenuFields = NodeMenuFields.getPersonNodeMenuFields();
            return new NodeMenu(personMenuFields, "person-node-menu");
        },

        /**
         * Creates a menu to input data to populayte new patient with (e.g. required fields which
         * should always be populated, even for new otherwise blank patients)
         *
         * @method generateNewPersonMenu
         * @return {NodeMenu}
         */
        generateNewPersonMenu: function() {
            if (this.isReadOnlyMode()) return null;
            var newPersonMenuFields = NodeMenuFields.getNewPersonMenuFields();
            return new NodeMenu(newPersonMenuFields, "new-person-node-menu",
                    { "modalMode": true,
                      "noDynamicUpdates": true });
        },

        /**
         * Creates the deceased inputs menu for Person nodes
         *
         * @method generateDeceasedMenu
         * @return {DeceasedMenu}
         */
        generateDeceasedMenu: function() {
            if (this.isReadOnlyMode()) return null;
            return new DeceasedMenu();
        },

        /**
         * @method getNodeMenu
         * @return {NodeMenu} Context menu for nodes
         */
        getNodeMenu: function() {
            return this._nodeMenu;
        },

        /**
         * @method getNewPersonMenu
         * @return {NodeMenu} Context menu for filling data for creating new patients
         */
        getNewPersonMenu: function() {
            return this._newPersonMenu;
        },

        /**
         * @method getDeceasedMenu
         * @return {DeceasedMenu} Deceased inputs menu for nodes
         */
        getDeceasedMenu: function() {
            return this._deceasedMenu;
        },

        /**
         * Creates the context menu for PersonGroup nodes
         *
         * @method generateNodeGroupMenu
         * @return {NodeMenu}
         */
        generateNodeGroupMenu: function() {
            if (this.isReadOnlyMode()) return null;
            var groupMenuFields = NodeMenuFields.getGroupNodeMenuFields();
            return new NodeMenu(groupMenuFields, "group-node-menu");
        },

        /**
         * @method getNodeGroupMenu
         * @return {NodeMenu} Context menu for nodes
         */
        getNodeGroupMenu: function() {
            return this._nodeGroupMenu;
        },

        /**
         * Creates the context menu for Partnership nodes
         *
         * @method generatePartnershipMenu
         * @return {NodeMenu}
         */
        generatePartnershipMenu: function() {
            if (this.isReadOnlyMode()) return null;
            var relationshipMenuFields = NodeMenuFields.getRelationshipMenuFields();
            return new NodeMenu(relationshipMenuFields, "relationship-menu");
        },

        /**
         * @method getPartnershipMenu
         * @return {NodeMenu} The context menu for Partnership nodes
         */
        getPartnershipMenu: function() {
            return this._partnershipMenu;
        },

        /**
         * @method convertGraphCoordToCanvasCoord
         * @return [x,y] coordinates on the canvas
         */
        convertGraphCoordToCanvasCoord: function(x, y) {
            var scale = PedigreeEditorParameters.attributes.layoutScale;
            return { x: x * scale.xscale,
                     y: y * scale.yscale };
        },

        /**
         * Starts a timer to save the application state every 30 seconds
         *
         * @method initializeSave
         */
        startAutoSave: function(intervalInSeconds) {
            setInterval(function(){editor.getSaveLoadEngine().save(false)}, intervalInSeconds*1000);
        },

        /**
         * @method getStudySelectionDialog
         * @return {StudySelectionDialog} The dialog to select study
         */
        getStudySelectionDialog: function() {
            return this._studySelectionDialog;
        }
    });

    return PedigreeEditor;
});
