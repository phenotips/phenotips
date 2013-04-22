
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
        this.DEBUG_MODE = false;
        window.editor = this;

        //initialize the elements of the app
        this._workspace = new Workspace();
        this._nodeMenu = this.generateNodeMenu();
        this._partnershipMenu = this.generatePartnershipMenu();
        this._nodetypeSelectionBubble = new NodetypeSelectionBubble();
        this._legend = new Legend();
        this._nodeIndex = new NodeIndex();
        this._graph = new Graph();
        this._actionStack = new ActionStack();
        this._saveLoadEngine = new SaveLoadEngine();
        this._saveLoadIndicator = new SaveLoadIndicator();
        this._templateSelector = new TemplateSelector();
        this._saveLoadEngine.load();

        //attach actions to buttons on the top bar
        var undoButton = $('action-undo');
        undoButton && undoButton.on("click", function(event) {
            editor.getActionStack().undo();
        });
        var redoButton = $('action-redo');
        redoButton && redoButton.on("click", function(event) {
            editor.getActionStack().redo();
        });

        var saveButton = $('action-save');
        saveButton && saveButton.on("click", function(event) {
            editor.getSaveLoadEngine().serialize();
        });

        var loadButton = $('action-load');
        loadButton && loadButton.on("click", function(event) {
            editor.getSaveLoadEngine().load();
        });

        var templatesButton = $('action-templates');
        templatesButton && templatesButton.on("click", function(event) {
            editor.getTemplateSelector().show();
        });
    },

    /**
     * Returns the graph node with the corresponding nodeID
     * @method getNode
     * @param {Number} nodeID The id of the desired node
     * @return {AbstractNode} the node whose id is nodeID
     */
    getNode: function(nodeID) {
        return editor.getGraph().getNodeMap()[nodeID];
    },

    /**
     * @method getGraph
     * @return {Graph} (responsible for managing nodes in the editor)
     */
    getGraph: function() {
        return this._graph;
    },

    /**
     * @method getActionStack
     * @return {ActionStack} (responsible undoing and redoing actions)
     */
    getActionStack: function() {
        return this._actionStack;
    },

    /**
     * @method getNodetypeSelectionBubble
     * @return {NodetypeSelectionBubble} (floating window with initialization options for new nodes)
     */
    getNodetypeSelectionBubble: function() {
        return this._nodetypeSelectionBubble;
    },

    /**
     * @method getNodeIndex
     * @return {NodeIndex} (indexes nodes and arranges the layout)
     */
    getNodeIndex: function() {
        return this._nodeIndex;
    },

    /**
     * @method getWorkspace
     * @return {Workspace}
     */
    getWorkspace: function() {
        return this._workspace;
    },

    /**
     * @method getLegend
     * @return {Legend} Responsible for managing and displaying the disorder legend
     */
    getLegend: function() {
        return this._legend;
    },

    /**
     * @method getPaper
     * @return {Workspace.paper} Raphael paper element
     */
    getPaper: function() {
        return this.getWorkspace().getPaper();
    },

    /**
     * @method getSaveLoadEngine
     * @return {SaveLoadEngine} Engine responsible for saving and loading operations
     */
    getSaveLoadEngine: function() {
        return this._saveLoadEngine;
    },

    /**
     * @method getTemplateSelector
     * @return {TemplateSelector}
     */
    getTemplateSelector: function() {
        return this._templateSelector
    },

    /**
     * Creates the context menu for Person nodes
     *
     * @method generateNodeMenu
     * @return {NodeMenu}
     */
    generateNodeMenu: function() {
        var _this = this;
        document.observe('mousedown', function(event) {
            if (_this.getNodeMenu() && _this.getNodeMenu().isActive()) {
                if (event.element().getAttribute('class') != 'menu-trigger' &&
                    (!event.element().up || !event.element().up('.menu-box, .calendar_date_select, .suggestItems') && event.element().up('body'))) {
                    _this.getNodeMenu().hide();
                }
            }
        });
        return new NodeMenu([
            {
                'name' : 'identifier',
                'label' : '',
                'type'  : 'hidden'
            },
            {
                'name' : 'gender',
                'label' : 'Gender',
                'type' : 'radio',
                'values' : [
                    { 'actual' : 'M', 'displayed' : 'Male' },
                    { 'actual' : 'F', 'displayed' : 'Female' },
                    { 'actual' : 'U', 'displayed' : 'Unknown' }
                ],
                'default' : 'U',
                'function' : 'setGenderAction'
            },
            {
                'name' : 'first_name',
                'label': 'First name',
                'type' : 'text',
                'function' : 'setFirstNameAction'
            },
            {
                'name' : 'last_name',
                'label': 'Last name',
                'type' : 'text',
                'function' : 'setLastNameAction'
            },
            {
                'name' : 'date_of_birth',
                'label' : 'Date of birth',
                'type' : 'date-picker',
                'format' : 'dd/MM/yyyy',
                'function' : 'setBirthDateAction'
            },
            {
                'name' : 'date_of_death',
                'label' : 'Date of death',
                'type' : 'date-picker',
                'format' : 'dd/MM/yyyy',
                'function' : 'setDeathDateAction'
            },
            {
                'name' : 'disorders',
                'label' : 'Known disorders of this individual',
                'type' : 'disease-picker',
                'function' : 'setDisordersAction'
            },
            {
                'name' : 'gestation_age',
                'label' : 'Gestation age',
                'type' : 'select',
                'range' : {'start': 0, 'end': 50, 'item' : ['week', 'weeks']},
                'nullValue' : true,
                'function' : 'setGestationAgeAction'
            },
            {
                'name' : 'state',
                'label' : 'Individual is',
                'type' : 'radio',
                'values' : [
                    { 'actual' : 'alive', 'displayed' : 'Alive' },
                    { 'actual' : 'deceased', 'displayed' : 'Deceased' },
                    { 'actual' : 'stillborn', 'displayed' : 'Stillborn' },
                    { 'actual' : 'aborted', 'displayed' : 'Aborted' },
                    { 'actual' : 'unborn', 'displayed' : 'Unborn' }
                ],
                'default' : 'alive',
                'function' : 'setLifeStatusAction'
            },
            {
                'label' : 'Heredity options',
                'name' : 'childlessSelect',
                'values' : [{'actual': 'none', displayed: 'None'},{'actual': 'childless', displayed: 'Childless'},{'actual': 'infertile', displayed: 'Infertile'}],
                'type' : 'select',
                'function' : 'setChildlessStatusAction'
            },
            {
                'name' : 'childlessText',
                'type' : 'text',
                'dependency' : 'childlessSelect != none',
                'tip' : 'Reason',
                'function' : 'setChildlessReasonAction'
            },
            {
                'name' : 'adopted',
                'label' : 'Adopted',
                'type' : 'checkbox',
                'function' : 'setAdoptedAction'
            }
        ]);
    },

    /**
     * @method getNodeMenu
     * @return {NodeMenu} Context menu for nodes
     */
    getNodeMenu: function() {
        return this._nodeMenu;
    },

    /**
     * Creates the context menu for Partnership nodes
     *
     * @method generatePartnershipMenu
     * @return {NodeMenu}
     */
    generatePartnershipMenu: function() {
        var _this = this;
        document.observe('mousedown', function(event) {
            if (_this.getPartnershipMenu() && _this.getPartnershipMenu().isActive()) {
                if (event.element().getAttribute('class') != 'menu-trigger' &&
                    (!event.element().up || !event.element().up('.menu-box, .calendar_date_select, .suggestItems') && event.element().up('body'))) {
                    _this.getPartnershipMenu().hide();
                }
            }
        });
        return new NodeMenu([
            {
                'label' : 'Heredity options',
                'name' : 'childlessSelect',
                'values' : [{'actual': 'none', displayed: 'None'},{'actual': 'childless', displayed: 'Childless'},{'actual': 'infertile', displayed: 'Infertile'}],
                'type' : 'select',
                'function' : 'setChildlessStatusAction'
            },
            {
                'name' : 'childlessText',
                'type' : 'text',
                'dependency' : 'childlessSelect != none',
                'tip' : 'Reason',
                'function' : 'setChildlessReasonAction'
            }
        ]);
    },

    /**
     * @method getPartnershipMenu
     * @return {NodeMenu} The context menu for Partnership nodes
     */
    getPartnershipMenu: function() {
        return this._partnershipMenu;
    },

    /**
     * Starts a timer to save the application state every 30 seconds
     *
     * @method initializeSave
     */
    initializeSave: function() {
        setInterval(function(){editor.getGraph().serialize()},30000);
    },

    /**
     * Find the best position to insert one or more new neighbors for an existing node
     *
     * @method findPosition
     * @param relativePosition an object with one field, which can be either 'above', 'below', 'side', or 'join', whose value indicated the id of a node
     * @param identifiers an array of new ids for which positions must be found
     *
     * @return an object where each field is one of the ids given as input, and the value is the point where that node should be placed
     */
    findPosition : function (relativePosition, identifiers) {
      return this.getNodeIndex().findPosition(relativePosition, identifiers);
    }
});

var editor;

//attributes for graphical elements in the editor
PedigreeEditor.attributes = {
    radius: 40,
    unbornShape: {'font-size': 50, 'font-family': 'Cambria'},
    nodeShape: {fill: "0-#ffffff:0-#B8B8B8:100", stroke: "#595959"},
    boxOnHover : {fill: "gray", stroke: "none",opacity: 1, "fill-opacity":.25},
    menuBtnIcon : {fill: "#1F1F1F", stroke: "none"},
    deleteBtnIcon : {fill: "#990000", stroke: "none"},
    btnMaskHoverOn : {opacity:.6, stroke: 'none'},
    btnMaskHoverOff : {opacity:0},
    btnMaskClick: {opacity:1},
    orbHue : .53,
        phShape: {fill: "white","fill-opacity": 0, "stroke": 'black', "stroke-dasharray": "- "},
    dragMeLabel: {'font-size': 14, 'font-family': 'Tahoma'},
    descendantGroupLabel: {'font-size': 20, 'font-family': 'Tahoma'},
    label: {'font-size': 18, 'font-family': 'Cambria'},
    disorderShapes: {},
    partnershipRadius: 6,
        partnershipLines : {"stroke-width": 2, stroke : '#2E2E56'}
};

document.observe("dom:loaded",function() {
    editor = new PedigreeEditor();
});
