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
        "pedigree/undoRedoManager",
        "pedigree/controller",
        "pedigree/pedigreeEditorParameters",
        "pedigree/preferencesManager",
        "pedigree/probandDataLoader",
        "pedigree/saveLoadEngine",
        "pedigree/versionUpdater",
        "pedigree/view",
        "pedigree/nodeMenuFields",
        "pedigree/model/dynamicGraph",
        "pedigree/model/helpers",
        "pedigree/view/workspace",
        "pedigree/view/disorderLegend",
        "pedigree/view/exportSelector",
        "pedigree/view/geneLegend",
        "pedigree/view/hpoLegend",
        "pedigree/view/importSelector",
        "pedigree/view/cancersLegend",
        "pedigree/view/nodeMenu",
        "pedigree/view/nodetypeSelectionBubble",
        "pedigree/view/okCancelDialogue",
        "pedigree/view/saveLoadIndicator",
        "pedigree/view/templateSelector",
        "pedigree/view/printDialog"
    ],
    function(
        PedigreeExtensionManager,
        UndoRedoManager,
        Controller,
        PedigreeEditorParameters,
        PreferencesManager,
        ProbandDataLoader,
        SaveLoadEngine,
        VersionUpdater,
        View,
        NodeMenuFields,
        DynamicPositionedGraph,
        Helpers,
        Workspace,
        DisorderLegend,
        ExportSelector,
        GeneLegend,
        HPOLegend,
        ImportSelector,
        CancerLegend,
        NodeMenu,
        NodetypeSelectionBubble,
        OkCancelDialogue,
        SaveLoadIndicator,
        TemplateSelector,
        PrintDialog
){

    var PedigreeEditor = Class.create({
        initialize: function() {
            //this.DEBUG_MODE = true;
            window.editor = this;

            this._extensionManager = new PedigreeExtensionManager();

            // Available options:
            //
            //  nonStandardAdoptedOutGraphic: {true|false}   - use out-brackets for adopted out persons; default "false"
            //  hideDraggingHint:             {true|false}   - do not display the hint on top of the legend; default "false"
            //  propagateFatherLastName:      {true|false}   - auto-propagate father's last name or not; default: "true"
            //  dateDisplayFormat:            {"MDY"|"DMY"|"MY"|"Y"|"MMY"}  - date display format; default "MDY"; MY = "02-2015", MMY = "Feb 2015"
            //  dateEditFormat:               {"YMD"|"DMY"|"MY"|"Y"}  - defines order of fields in the date picker; default "YMD"
            //  drawNodeShadows:              {true|false}   - display small shadow under node graphic; default: "true"
            //  disabledFields:               [array]        - list of node-menu fields disabled for this installation
            //  displayCancerLabels:          {true|false}   - display labels for each afecting cancer; default: "true"
            //  lineStyle:                    {"thin"|"regular"|"bold"} - controls the thickness of all lines in pedigree
            //
            this._defaultPreferences = { global:   { nonStandardAdoptedOutGraphic: false,
                                                     propagateFatherLastName: true,
                                                     dateDisplayFormat: "YMD",
                                                     dateEditFormat: "YMD",
                                                     drawNodeShadows: true,
                                                     disabledFields: [],
                                                     displayCancerLabels: true,
                                                     lineStyle: "regular" },
                                         user:     { hideDraggingHint: false,
                                                     firstName: "",
                                                     lastName: "" },
                                         pedigree: {}
                                       };
            this._preferencesManager = new PreferencesManager(this._defaultPreferences);

            // initialize main data structure which holds the graph structure
            this._graphModel = DynamicPositionedGraph.makeEmpty(PedigreeEditorParameters.attributes.layoutRelativePersonWidth, PedigreeEditorParameters.attributes.layoutRelativeOtherWidth);

            //initialize the elements of the app
            this._workspace = new Workspace();
            this._disorderLegend = new DisorderLegend();
            this._geneLegend = new GeneLegend();
            this._hpoLegend = new HPOLegend();
            this._cancerLegend = new CancerLegend();
            this._nodetypeSelectionBubble = new NodetypeSelectionBubble(false);
            this._siblingSelectionBubble  = new NodetypeSelectionBubble(true);
            this._okCancelDialogue = new OkCancelDialogue();

            this._view = new View();

            this._actionStack = new UndoRedoManager();
            this._templateSelector = new TemplateSelector();
            this._saveLoadIndicator = new SaveLoadIndicator();
            this._versionUpdater = new VersionUpdater();
            this._saveLoadEngine = new SaveLoadEngine();
            this._probandData = new ProbandDataLoader();

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

                    // load proband data and load the graph after proband data is available
                    this._probandData.load( this._saveLoadEngine.load.bind(this._saveLoadEngine) );

                    // generate various dialogues after preferences have been loaded
                    this._nodeMenu = this.generateNodeMenu();
                    this._nodeGroupMenu = this.generateNodeGroupMenu();
                    this._partnershipMenu = this.generatePartnershipMenu();
                    this._importSelector = new ImportSelector();
                    this._exportSelector = new ExportSelector();
                    this._printDialog = new PrintDialog();
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

            var saveButton = $('action-save');
            saveButton && saveButton.on("click", function(event) {
                editor.getSaveLoadEngine().save();
            });

            var templatesButton = $('action-templates');
            templatesButton && templatesButton.on("click", function(event) {
                editor.getTemplateSelector().show();
            });
            var importButton = $('action-import');
            importButton && importButton.on("click", function(event) {
                editor.getImportSelector().show();
            });
            var exportButton = $('action-export');
            exportButton && exportButton.on("click", function(event) {
                editor.getExportSelector().show();
            });
            var printButton = $('action-print');
            printButton && printButton.on("click", function(event) {
                editor.getPrintDialog().show();
            });

            var onLeavePageFunc = function() {
                if (!editor.isReadOnlyMode() && editor.getUndoRedoManager().hasUnsavedChanges()) {
                    return "All changes will be lost when navigating away from this page.";
                }
            };
            window.onbeforeunload = onLeavePageFunc;

            var onCloseButtonClickFunc = function(event) {
                var dontQuitFunc    = function() { window.onbeforeunload = onLeavePageFunc; };
                var quitFunc        = function() { window.location=XWiki.currentDocument.getURL(XWiki.contextaction); };
                var saveAndQuitFunc = function() { editor._afterSaveFunc = quitFunc;
                                                   editor.getSaveLoadEngine().save(); }

                if (editor.isReadOnlyMode()) {
                    quitFunc();
                } else {
                    window.onbeforeunload = undefined;

                    if (editor.getUndoRedoManager().hasUnsavedChanges()) {
                        editor.getOkCancelDialogue().showCustomized( 'There are unsaved changes, do you want to save the pedigree before closing the pedigree editor?',
                                                                     'Save before closing?',
                                                                     "Save", saveAndQuitFunc,
                                                                     "Don't save", quitFunc,
                                                                     "Don't close", dontQuitFunc, true );
                    } else {
                        quitFunc();
                    }
                }
            };
            var closeButton = $('action-close');
            this._afterSaveFunc = null;
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
         * @method getExtensionManager
         * @return {PedigreeExtensionManager}
         */
        getExtensionManager: function() {
            return this._extensionManager;
        },

        /**
         * @method getPreferencesManager
         * @return {PreferencesManager}
         */
        getPreferencesManager: function() {
            return this._preferencesManager;
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
         * @method getVersionUpdater
         * @return {VersionUpdater}
         */
        getVersionUpdater: function() {
            return this._versionUpdater;
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
         * The action which should happen after pedigree is saved
         * (normally null, close the editor when "save on quit")
         */
        getAfterSaveAction: function() {
            return this._afterSaveFunc;
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
         * @return {Legend} Responsible for managing and displaying the candidate genes legend
         */
        getGeneLegend: function() {
            return this._geneLegend;
        },

        /**
         * @method getCancerLegend
         * @return {Legend} Responsible for managing and displaying the common cancers legend
         */
        getCancerLegend: function() {
            return this._cancerLegend;
        },

        /**
         * @method getPaper
         * @return {Workspace.paper} Raphael paper element
         */
        getPaper: function() {
            return this.getWorkspace().getPaper();
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
         * @method getProbandDataFromPhenotips
         * @return {firstName: "...", lastName: "..."}
         */
        getProbandDataFromPhenotips: function() {
            return this._probandData.probandData;
        },

        /**
         * @method getTemplateSelector
         * @return {TemplateSelector}
         */
        getTemplateSelector: function() {
            return this._templateSelector
        },

        /**
         * @method getImportSelector
         * @return {ImportSelector}
         */
        getImportSelector: function() {
            return this._importSelector
        },

        /**
         * @method getExportSelector
         * @return {ExportSelector}
         */
        getExportSelector: function() {
            return this._exportSelector
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
            if (this.getNodeMenu().isVisible() || this.getNodeGroupMenu().isVisible() || this.getPartnershipMenu().isVisible()) {
                return;
            }
        },

        /**
         * Creates the context menu for Person nodes
         *
         * @method generateNodeMenu
         * @return {NodeMenu}
         */
        generateNodeMenu: function() {
            if (this.isReadOnlyMode()) return null;
            var disabledFields = this.getPreferencesManager().getConfigurationOption("disabledFields");
            var personMenuFields = NodeMenuFields.getPersonNodeMenuFields(disabledFields);
            return new NodeMenu(personMenuFields, "person-node-menu");
        },

        /**
         * @method getNodeMenu
         * @return {NodeMenu} Context menu for nodes
         */
        getNodeMenu: function() {
            return this._nodeMenu;
        },

        /**
         * Creates the context menu for PersonGroup nodes
         *
         * @method generateNodeGroupMenu
         * @return {NodeMenu}
         */
        generateNodeGroupMenu: function() {
            if (this.isReadOnlyMode()) return null;
            var disabledFields = this.getPreferencesManager().getConfigurationOption("disabledFields");
            var groupMenuFields = NodeMenuFields.getGroupNodeMenuFields(disabledFields);
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
            var disabledFields = this.getPreferencesManager().getConfigurationOption("disabledFields");
            var relationshipMenuFields = NodeMenuFields.getRelationshipMenuFields(disabledFields);
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
            setInterval(function(){editor.getSaveLoadEngine().save()}, intervalInSeconds*1000);
        }
    });

    return PedigreeEditor;
});
