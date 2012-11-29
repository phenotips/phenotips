/*
 * The main class of the Pedigree Editor, responsible for initializing all the basic elements of the app.
 * Contains wrapper methods for the most commonly used functions.
 * This class should be initialized only once.
 */
var PedigreeEditor = Class.create({

//Graphical attributes for graphical elements in the editor
    attributes: {
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
    },

    /*
     * Initializes the workspace, menu, top menu-bar, child creation bubble, legend, layout and the Raphael elements.
     */
    initialize: function() {
        this.DEBUG_MODE = false;
        window.editor = this;

        this._workspace = new Workspace();
        this._nodeMenu = this.generateNodeMenu();
        this._partnershipMenu = this.generatePartnershipMenu();
        this._nodeTypeOptions = new NodeTypeOptions();
        this._legend = new Legend();
        this._nodeIndex = new NodeIndex();
        this._graph = new Graph();
        this._actionStack = new ActionStack();

        var undoButton = $('action-undo');
        undoButton && undoButton.on("click", function(event) {
            editor.getActionStack().undo();
        });
        var redoButton = $('action-redo');
        redoButton && redoButton.on("click", function(event) {
            editor.getActionStack().redo();
        });
    },

    /*
     * Returns the Graph object, responsible for managing nodes in the editor
     */
    getGraph: function() {
        return this._graph;
    },

    getActionStack: function() {
        return this._actionStack;
    },

    /*
     * Returns the menu object
     */
    getNodeMenu: function() {
        return this._nodeMenu;
    },

    /*
     * Returns the NodeTypeOptions object
     */
    getNodeTypeOptions: function() {
        return this._nodeTypeOptions;
    },

    /*
     * Returns the NodeIndex object responsible for the graphical layout of nodes
     */
    getNodeIndex: function() {
        return this._nodeIndex;
    },

    /*
     * Returns the Workspace object responsible managing the canvas
     */
    getWorkspace: function() {
        return this._workspace;
    },

    /*
     * Returns the Legend object responsible for managing and displaying the disorder legend
     */
    getLegend: function() {
        return this._legend;
    },

    /*
     * Returns the Raphael paper object
     */
    getPaper: function() {
        return this.getWorkspace().getPaper();
    },

    /*
     * Creates and returns the menu for Person nodes
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
                'function' : 'setGender'
            },
            {
                'name' : 'first_name',
                'label': 'First name',
                'type' : 'text',
                'function' : 'setFirstName'
            },
            {
                'name' : 'last_name',
                'label': 'Last name',
                'type' : 'text',
                'function' : 'setLastName'
            },
            {
                'name' : 'date_of_birth',
                'label' : 'Date of birth',
                'type' : 'date-picker',
                'format' : 'dd/MM/yyyy',
                'function' : 'setBirthDate'
            },
            {
                'name' : 'date_of_death',
                'label' : 'Date of death',
                'type' : 'date-picker',
                'format' : 'dd/MM/yyyy',
                'function' : 'setDeathDate'
            },
            {
                'name' : 'disorders',
                'label' : 'Known disorders of this individual',
                'type' : 'disease-picker',
                'function' : 'updateDisorders'
            },
            {
                'name' : 'gestation_age',
                'label' : 'Gestation age',
                'type' : 'select',
                'range' : {'start': 0, 'end': 50, 'item' : ['week', 'weeks']},
                'nullValue' : true,
                'function' : 'setGestationAge'
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
                'function' : 'setLifeStatus'
            },
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
                'name' : 'adopted',
                'label' : 'Adopted',
                'type' : 'checkbox',
                'function' : 'setAdopted'
            }
        ]);
    },

    /*
     * Returns the menu of the partnership node
     */
    getPartnershipMenu: function() {
        return this._partnershipMenu;
    },

    /*
     * Creates and returns the menu for Partnership nodes
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
                'function' : 'setChildlessStatus'
            },
            {
                'name' : 'childlessText',
                'type' : 'text',
                'dependency' : 'childlessSelect != none',
                'tip' : 'Reason',
                'function' : 'setChildlessReason'
            }
        ]);
    },

    // WRAPPERS FOR THE NODE INDEX & GRID FUNCITONS
    getGridUnitX : function () {
      return this.getNodeIndex().gridUnit.x;
    },

    getGridUnitY : function () {
      return this.getNodeIndex().gridUnit.y;
    },

    findPosition : function (relativeNodePosition, ids) {
      return this.getNodeIndex().findPosition(relativeNodePosition, ids);
    }
});

var editor;

document.observe("dom:loaded",function() {
    editor = new PedigreeEditor();
});
