/**
 * The main class of the Pedigree Editor, responsible for initializing all the basic elements of the app.
 * Contains wrapper methods for the most commonly used functions.
 * This class should be initialized only once.
 *
 * @class PedigreeEditor
 * @constructor
 */
var PedigreeEditor = Class.create({
    initialize: function() {
        //this.DEBUG_MODE = true;
        window.editor = this;

        // Available options:
        //
        //  nonStandardAdoptedOutGraphic: {true|false}   - use out-brackets for adopted out persons; default "false"
        //  hideDraggingHint:             {true|false}   - do not display the hint on top of the legend; default "false"
        //  propagateFatherLastName:      {true|false}   - auto-propagate father's last name or not; default: "true"
        //  dateDisplayFormat:            {"MDY"|"DMY"}  - date display format; default "MDY";
        //  dateEditFormat:               {"YMD"|"DMY"}  - defines order of fields in the date picker; default "YMD";
        //
        this._defaultPreferences = { global:   { nonStandardAdoptedOutGraphic: false,
                                                 propagateFatherLastName: true,
                                                 dateDisplayFormat: "YMD",
                                                 dateEditFormat: "YMD" },
                                     user:     { hideDraggingHint: false },
                                     pedigree: {}
                                   };
        this._preferencesManager = new PreferencesManager(this._defaultPreferences);

        // initialize main data structure which holds the graph structure
        this._graphModel = DynamicPositionedGraph.makeEmpty(PedigreeEditor.attributes.layoutRelativePersonWidth, PedigreeEditor.attributes.layoutRelativeOtherWidth);

        //initialize the elements of the app
        this._workspace = new Workspace();
        this._disorderLegend = new DisorgerLegend();
        this._geneLegend = new GeneLegend();
        this._hpoLegend = new HPOLegend();
        this._cancerLegend = new CancerLegend();
        this._nodetypeSelectionBubble = new NodetypeSelectionBubble(false);
        this._siblingSelectionBubble  = new NodetypeSelectionBubble(true);
        this._okCancelDialogue = new OkCancelDialogue();

        this._view = new View();

        this._actionStack = new ActionStack();
        this._templateSelector = new TemplateSelector();
        this._saveLoadIndicator = new SaveLoadIndicator();
        this._versionUpdater = new VersionUpdater();
        this._saveLoadEngine = new SaveLoadEngine();
        this._probandData = new ProbandDataLoader();

        this._preferencesManager.load( function() {
                // load proband data and load the graph after proband data is available 
                this._probandData.load( this._saveLoadEngine.load.bind(this._saveLoadEngine) );

                // generate various dialogues after preferences have been loaded
                this._nodeMenu = this.generateNodeMenu();
                this._nodeGroupMenu = this.generateNodeGroupMenu();
                this._partnershipMenu = this.generatePartnershipMenu();
                this._importSelector = new ImportSelector();
                this._exportSelector = new ExportSelector();
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
        var loadButton = $('action-reload');
        loadButton && loadButton.on("click", function(event) {
            editor.getSaveLoadEngine().load();
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

        var onLeavePageFunc = function() {
            if (editor.getActionStack().hasUnsavedChanges()) {
                return "All changes will be lost when navigating away from this page.";
            }
        };
        window.onbeforeunload = onLeavePageFunc;

        var closeButton = $('action-close');
        this._afterSaveFunc = null;
        closeButton && closeButton.on("click", function(event) {
            var dontQuitFunc    = function() { window.onbeforeunload = onLeavePageFunc; };
            var quitFunc        = function() { window.location=XWiki.currentDocument.getURL('edit'); };
            var saveAndQuitFunc = function() { editor._afterSaveFunc = quitFunc;
                                               editor.getSaveLoadEngine().save(); }

            if (editor.isReadOnlyMode()) {
                quitFunc();
            } else {
                window.onbeforeunload = undefined;

                if (editor.getActionStack().hasUnsavedChanges()) {
                    editor.getOkCancelDialogue().showCustomized( 'There are unsaved changes, do you want to save the pedigree before closing the pedigree editor?',
                                                                 'Save before closing?',
                                                                 "Save", saveAndQuitFunc,
                                                                 "Don't save", quitFunc,
                                                                 "Don't quit", dontQuitFunc, true );
                } else {
                    quitFunc();
                }
            }
        });

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
     * @method getActionStack
     * @return {ActionStack} (responsible for undoing and redoing actions)
     */
    getActionStack: function() {
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
        var _this = this;
        return new NodeMenu([
            {
                'name' : 'identifier',
                'label' : '',
                'type'  : 'hidden',
                'tab': 'Personal'
            },
            {
                'name' : 'gender',
                'label' : 'Gender',
                'type' : 'radio',
                'tab': 'Personal',
                'columns': 3,
                'values' : [
                    { 'actual' : 'M', 'displayed' : 'Male' },
                    { 'actual' : 'F', 'displayed' : 'Female' },
                    { 'actual' : 'U', 'displayed' : 'Unknown' }
                ],
                'default' : 'U',
                'function' : 'setGender'
            },
            {
                'name' : 'first_name',
                'label': 'First name',
                'type' : 'text',
                'tab': 'Personal',
                'function' : 'setFirstName'
            },
            {
                'name' : 'last_name',
                'label': 'Last name',
                'type' : 'text',
                'tab': 'Personal',
                'function' : 'setLastName'
            },
            {
                'name' : 'last_name_birth',
                'label': 'Last name at birth',
                'type' : 'text',
                'tab': 'Personal',
                'function' : 'setLastNameAtBirth'
            },
            {
                'name' : 'external_id',
                'label': 'External ID',
                'type' : 'text',
                'tab': 'Personal',
                'function' : 'setExternalID'
            },
            {
                'name' : 'ethnicity',
                'label' : 'Ethnicities',
                'type' : 'ethnicity-picker',
                'tab': 'Personal',
                'function' : 'setEthnicities'
            },
            {
                'name' : 'carrier',
                'label' : 'Carrier status',
                'type' : 'radio',
                'tab': 'Clinical',
                'values' : [
                    { 'actual' : '', 'displayed' : 'Not affected' },
                    { 'actual' : 'carrier', 'displayed' : 'Carrier' },
                    //{ 'actual' : 'obligate', 'displayed' : 'Obligate carrier' },
                    { 'actual' : 'uncertain', 'displayed' : 'Uncertain' },
                    { 'actual' : 'affected', 'displayed' : 'Affected' },
                    { 'actual' : 'presymptomatic', 'displayed' : 'Pre-symptomatic' }
                ],
                'default' : '',
                'function' : 'setCarrierStatus'
            },
            {
                'name' : 'disorders',
                'label' : 'Known disorders of this individual',
                'type' : 'disease-picker',
                'tab': 'Clinical',
                'function' : 'setDisorders'
            },
            {
                'name' : 'hpo_positive',
                'label' : 'Clinical symptoms: observed phenotypes',
                'type' : 'hpo-picker',
                'tab': 'Clinical',
                'function' : 'setHPO'
            },
            {
                'name' : 'candidate_genes',
                'label' : 'Genotype information: candidate genes',
                'type' : 'gene-picker',
                'tab': 'Clinical',
                'function' : 'setGenes'
            },
            {
                'name' : 'date_of_birth',
                'label' : 'Date of birth',
                'type' : 'date-picker',
                'tab': 'Personal',
                'function' : 'setBirthDate'
            },
            {
                'name' : 'date_of_death',
                'label' : 'Date of death',
                'type' : 'date-picker',
                'tab': 'Personal',
                'function' : 'setDeathDate'
            },
            {
                'name' : 'gestation_age',
                'label' : 'Gestation age',
                'type' : 'select',
                'tab': 'Personal',
                'range' : {'start': 0, 'end': 50, 'item' : ['week', 'weeks']},
                'nullValue' : true,
                'function' : 'setGestationAge'
            },
            {
                'name' : 'state',
                'label' : 'Individual is',
                'type' : 'radio',
                'tab': 'Personal',
                'columns': 3,
                'valuesIE9' : [
                    // different order of options because they are displayed sequentially instead of in 3-column layout
                    { 'actual' : 'alive', 'displayed' : 'Alive' },
                    { 'actual' : 'deceased', 'displayed' : 'Deceased' },
                    { 'actual' : 'stillborn', 'displayed' : 'Stillborn' },
                    { 'actual' : 'unborn', 'displayed' : 'Unborn' },
                    { 'actual' : 'miscarriage', 'displayed' : 'Miscarried' },
                    { 'actual' : 'aborted', 'displayed' : 'Elective abortion' }],
                'values' : [
                    { 'actual' : 'alive', 'displayed' : 'Alive' },
                    { 'actual' : 'stillborn', 'displayed' : 'Stillborn' },
                    { 'actual' : 'deceased', 'displayed' : 'Deceased', 'columnshiftPX': -2 },
                    { 'actual' : 'miscarriage', 'displayed' : 'Miscarried', 'columnshiftPX': -2},
                    { 'actual' : 'unborn', 'displayed' : 'Unborn', 'columnshiftPX': 8 },
                    { 'actual' : 'aborted', 'displayed' : 'Aborted', 'columnshiftPX': 8 }],
                'default' : 'alive',
                'function' : 'setLifeStatus'
            },
            {
                'label' : 'Heredity options',
                'name' : 'childlessSelect',
                'values' : [{'actual': 'none', displayed: 'None'},
                            {'actual': 'childless', displayed: 'Childless'},
                            {'actual': 'infertile', displayed: 'Infertile'}],
                'type' : 'select',
                'tab': 'Personal',
                'function' : 'setChildlessStatus'
            },
            {
                'name' : 'childlessText',
                'type' : 'text',
                'dependency' : 'childlessSelect != none',
                'tip' : 'Reason',
                'tab': 'Personal',
                'function' : 'setChildlessReason'
            },
            {
                'name' : 'adopted',
                'label' : 'Adopted status',
                'type' : 'radio',
                'tab': 'Personal',
                'columns': 3,
                'values' : [
                    { 'actual' : '',           'displayed' : 'Not adopted' },
                    { 'actual' : 'adoptedIn',  'displayed' : 'Adopted in' },
                    { 'actual' : 'adoptedOut', 'displayed' : 'Adopted out' }
                ],
                'default' : '',
                'function' : 'setAdopted'
            },
            {
                'name' : 'monozygotic',
                'label' : 'Monozygotic twin',
                'type' : 'checkbox',
                'tab': 'Personal',
                'function' : 'setMonozygotic'
            },
            {
                'name' : 'nocontact',
                'label' : 'Not in contact with proband',
                'type' : 'checkbox',
                'tab': 'Personal',
                'function' : 'setLostContact'
            },
            {
                'name' : 'placeholder',
                'label' : 'Placeholder node',
                'type' : 'checkbox',
                'tab': 'Personal',
                'function' : 'makePlaceholder'
            },
            {
                'name' : 'comments',
                'label' : 'Comments',
                'type' : 'textarea',
                'tab': 'Clinical',
                'rows' : 4,
                'function' : 'setComments'
            },
            {
                'name' : 'evaluated',
                'label' : 'Documented evaluation',
                'type' : 'checkbox',
                'tab': 'Clinical',
                'function' : 'setEvaluated'
            },
            {   'name' : 'cancers',
                'label' : 'Common Cancers',
                'type' : 'cancerlist',
                'tab' : 'Cancers',
                'function' : 'setCancers'
            }
        ], ["Personal", "Clinical", "Cancers"]);
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
        var _this = this;
        return new NodeMenu([
            {
                'name' : 'identifier',
                'label' : '',
                'type'  : 'hidden',
                'tab': 'Personal'
            },
            {
                'name' : 'gender',
                'label' : 'Gender',
                'type' : 'radio',
                'columns': 3,
                'values' : [
                    { 'actual' : 'M', 'displayed' : 'Male' },
                    { 'actual' : 'F', 'displayed' : 'Female' },
                    { 'actual' : 'U', 'displayed' : 'Unknown' }
                ],
                'default' : 'U',
                'tab': 'Personal',
                'function' : 'setGender'
            },
            {
                'name' : 'numInGroup',
                'label': 'Number of persons in this group',
                'type' : 'select',
                'values' : [{'actual': 1, displayed: 'N'}, {'actual': 2, displayed: '2'}, {'actual': 3, displayed: '3'},
                            {'actual': 4, displayed: '4'}, {'actual': 5, displayed: '5'}, {'actual': 6, displayed: '6'},
                            {'actual': 7, displayed: '7'}, {'actual': 8, displayed: '8'}, {'actual': 9, displayed: '9'}],
                'tab': 'Personal',
                'function' : 'setNumPersons'
            },
            {
                'name' : 'external_ids',
                'label': 'External ID(s)',
                'type' : 'text',
                'tab': 'Personal',
                'function' : 'setExternalID'
            },
            {
                'name' : 'ethnicity',
                'label' : 'Ethnicities<br>(common to all individuals in the group)',
                'type' : 'ethnicity-picker',
                'tab': 'Personal',
                'function' : 'setEthnicities'
            },
            {
                'name' : 'disorders',
                'label' : 'Known disorders<br>(common to all individuals in the group)',
                'type' : 'disease-picker',
                'tab': 'Clinical',
                'function' : 'setDisorders'
            },
            {
                'name' : 'comments',
                'label' : 'Comments',
                'type' : 'textarea',
                'rows' : 4,
                'tab': 'Clinical',
                'function' : 'setComments'
            },
            {
                'name' : 'state',
                'label' : 'All individuals in the group are',
                'type' : 'radio',
                'values' : [
                    { 'actual' : 'alive', 'displayed' : 'Alive' },
                    { 'actual' : 'aborted', 'displayed' : 'Aborted electively' },
                    { 'actual' : 'deceased', 'displayed' : 'Deceased' },
                    { 'actual' : 'miscarriage', 'displayed' : 'Miscarried' }
                ],
                'default' : 'alive',
                'tab': 'Personal',
                'function' : 'setLifeStatus'
            },
            {
                'name' : 'evaluatedGrp',
                'label' : 'Documented evaluation',
                'type' : 'checkbox',
                'tab': 'Clinical',
                'function' : 'setEvaluated'
            },
            {
                'name' : 'adopted',
                'label' : 'Adopted status',
                'type' : 'radio',
                'tab': 'Personal',
                'columns': 3,
                'values' : [
                    { 'actual' : '',           'displayed' : 'Not adopted' },
                    { 'actual' : 'adoptedIn',  'displayed' : 'Adopted in' },
                    { 'actual' : 'adoptedOut', 'displayed' : 'Adopted out' }
                ],
                'default' : '',
                'tab': 'Personal',
                'function' : 'setAdopted'
            }
        ], ["Personal", "Clinical"]);
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
        var _this = this;
        return new NodeMenu([
            {
                'label' : 'Heredity options',
                'name' : 'childlessSelect',
                'values' : [{'actual': 'none', displayed: 'None'},{'actual': 'childless', displayed: 'Childless'},{'actual': 'infertile', displayed: 'Infertile'}],
                'type' : 'select',
                'function' : 'setChildlessStatus'
            },
            {
                'name' : 'childlessText',
                'type' : 'text',
                'dependency' : 'childlessSelect != none',
                'tip' : 'Reason',
                'function' : 'setChildlessReason'
            },
            {
                'name' : 'consangr',
                'label' : 'Consanguinity of this relationship',
                'type' : 'radio',
                'values' : [
                    { 'actual' : 'A', 'displayed' : 'Automatic' },
                    { 'actual' : 'Y', 'displayed' : 'Yes' },
                    { 'actual' : 'N', 'displayed' : 'No' }
                ],
                'default' : 'A',
                'function' : 'setConsanguinity'
            },
            {
                'name' : 'broken',
                'label' : 'Separated',
                'type' : 'checkbox',
                'function' : 'setBrokenStatus'
            }
        ], [], "relationship-menu");
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
        var scale = PedigreeEditor.attributes.layoutScale;
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

var editor;

//attributes for graphical elements in the editor
PedigreeEditor.attributes = {
    radius: 40,
    orbRadius: 6,
    touchOrbRadius: 8,
    personHoverBoxRadius: 90,  // 80    for old handles, 90 for new
    newHandles: true,          // false for old handles
    personHandleLength: 75,    // 60    for old handles, 75 for new
    personHandleBreakX: 55,
    personHandleBreakY: 53,
    personSiblingHandleLengthX: 65,
    personSiblingHandleLengthY: 30,
    enableHandleHintImages: true,
    handleStrokeWidth: 5,
    groupNodesScale: 0.85,
    infertileMarkerHeight: 4,
    infertileMarkerWidth: 14,
    twinCommonVerticalLength: 6,
    twinMonozygothicLineShiftY: 24,
    curvedLinesCornerRadius: 25,
    unbornShape: {'font-size': 50, 'font-family': 'Cambria'},
    carrierShape: {fill : '#595959'},
    carrierDotRadius: 8,
    presymptomaticShape: {fill : '#777777', "stroke": "#777777"},
    presymptomaticShapeWidth: 8,
    uncertainShape:      {'font-size': '45px', 'font-family': 'Arial', 'fill': '#696969', 'font-weight': 'bold'},
    uncertainSmallShape: {'font-size': '30px', 'font-family': 'Arial', 'fill': '#696969', 'font-weight': 'bold'},
    evaluationShape: {'font-size': 40, 'font-family': 'Arial'},
    nodeShapeFemale: {fill: "0-#ffffff:0-#B8B8B8:100",  stroke: "#595959"},
    nodeShapeMale:   {fill: "0-#ffffff:0-#B8B8B8:100",  stroke: "#696969"},
    nodeShapeDiag:   {fill: "45-#ffffff:0-#B8B8B8:100", stroke: "#595959"},
    nodeShapeAborted:{fill: "0-#ffffff:0-#B8B8B8:100",  stroke: "#595959"},
    nodeShapeMenuOn:         {fill: "#000", stroke: "none", "fill-opacity": 0.1},
    nodeShapeMenuOff:        {fill: "#000", stroke: "none", "fill-opacity": 0},
    nodeShapeMenuOnPartner:  {fill: "#000", stroke: "none", "fill-opacity": 0.1},
    nodeShapeMenuOffPartner: {fill: "#000", stroke: "none", "fill-opacity": 0},
    boxOnHover : {fill: "gray", stroke: "none", opacity: 1, "fill-opacity":.35},
    menuBtnIcon : {fill: "#1F1F1F", stroke: "none"},
    deleteBtnIcon : {fill: "#990000", stroke: "none"},
    btnMaskHoverOn : {opacity:.6, stroke: 'none'},
    btnMaskHoverOff : {opacity:0},
    btnMaskClick: {opacity:1},
    orbHue : .53,
    phShape: {fill: "white","fill-opacity": 0, "stroke": 'black', "stroke-dasharray": "- "},
    dragMeLabel: {'font-size': 14, 'font-family': 'Tahoma'},
    pedNumberLabel: {'font-size': 19, 'font-family': 'Serif'},
    descendantGroupLabel: {'font-size': 21, 'font-family': 'Tahoma'},
    label: {'font-size': 20, 'font-family': 'Arial'},
    nameLabels: {'font-size': 20, 'font-family': 'Arial'},
    commentLabel: {'font-size': 19, 'font-family': 'Arial' },
    externalIDLabels: {'font-size': 18, 'font-family': 'Arial' },
    disorderShapes: {},
    partnershipNode: {fill: '#dc7868', stroke: 'black', 'stroke-width':2},  //#E25740
    partnershipRadius: 6.5,
    partnershipHandleBreakY: 18,
    partnershipHandleLength: 36,
    partnershipLines :         {"stroke-width": 1.25, stroke : '#303058'},
    partnershipLinesAdoptedIn: {"stroke-width": 1.25, stroke : '#303058', "stroke-dasharray": "--"},
    consangrPartnershipLines : {"stroke-width": 1.25, stroke : '#402058'},
    noContactLines:            {"stroke-width": 1.25, stroke : '#303058', "stroke-dasharray": "-.-."},
    noContactAdoptedIn:        {"stroke-width": 1.25, stroke : '#303058', "stroke-dasharray": "-- ."},
    noContactLinesConsangr:    {"stroke-width": 1.25, stroke : '#402058', "stroke-dasharray": "-.-."},
    childlessShapeAttr:            {"stroke-width": 2.5, stroke: "#3C3C3C"},
    partnershipChildlessShapeAttr: {"stroke-width": 2.0, stroke: "#3C3C3C"},
    childlessLength: 14,
    parnershipChildlessLength: 27,
    graphToCanvasScale: 12,
    layoutRelativePersonWidth: 10,
    layoutRelativeOtherWidth: 2,
    layoutScale: { xscale: 12.0, yscale: 8 }
};

document.observe("xwiki:dom:loaded",function() {
    editor = new PedigreeEditor();
});
