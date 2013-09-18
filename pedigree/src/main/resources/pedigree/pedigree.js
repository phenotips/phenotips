
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
        this.DEBUG_MODE = true;
        window.editor = this;
                        
        // initialize main data structure which holds the graph structure        
        var G = new InternalGraph(PedigreeEditor.attributes.layoutRelativePersonWidth, PedigreeEditor.attributes.layoutRelativeOtherWidth);
        var drawGraph = new DrawGraph(G);
        this._mainGraph = new PositionedGraph(drawGraph);                
               
        //initialize the elements of the app
        this._workspace = new Workspace();
        this._nodeMenu = this.generateNodeMenu();
        this._partnershipMenu = this.generatePartnershipMenu();
        this._nodetypeSelectionBubble = new NodetypeSelectionBubble();
        this._legend = new Legend();
                
        this._graphicsSet = new GraphicsSet();
        
        this._actionStack = new ActionStack();                
        this._templateSelector = new TemplateSelector();
        this._saveLoadIndicator = new SaveLoadIndicator();               
        this._saveLoadEngine = new SaveLoadEngine();
        this._probandData = new ProbandDataLoader();
        
        // load proband data and load the graph after proband data is available
        this._probandData.load( this._saveLoadEngine.load.bind(this._saveLoadEngine) );
        
        this._controller = new Controller();

        //attach actions to buttons on the top bar
        var undoButton = $('action-undo');
        undoButton && undoButton.on("click", function(event) {
            editor.getActionStack().undo();
        });
        var redoButton = $('action-redo');
        redoButton && redoButton.on("click", function(event) {
            editor.getActionStack().redo();
        });

        var clearButton = $('action-clear');
        clearButton && clearButton.on("click", function(event) {
            editor.getGraphicsSet().clearGraphAction();
        });
        
        var saveButton = $('action-save');
        saveButton && saveButton.on("click", function(event) {
            editor.getSaveLoadEngine().save();
        });

        var loadButton = $('action-load');
        loadButton && loadButton.on("click", function(event) {
            editor.getSaveLoadEngine().load();
        });

        var templatesButton = $('action-templates');
        templatesButton && templatesButton.on("click", function(event) {
            editor.getTemplateSelector().show();
        });
        
        //this.startAutoSave(30);               
    },

    /**
     * Returns the graph node with the corresponding nodeID
     * @method getNode
     * @param {Number} nodeID The id of the desired node
     * @return {AbstractNode} the node whose id is nodeID
     */
    getNode: function(nodeID) {
        return this.getGraphicsSet().getNode(nodeID);
    },
 
    /**
     * @method getGraphicsGraph
     * @return {Graph} (responsible for managing graphical representations of nodes in the editor)
     */
    getGraphicsSet: function() {
        return this._graphicsSet;
    },
    
    /**
     * @method getGraph
     * @return {PositionedGraph} (responsible for managing nodes and their positions)
     */    
    getGraph: function() {
    	return this._mainGraph;
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
        console.log("generateNodeMenu");    	
        var _this = this;
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
        setInterval(function(){editor.getGraphicsSet().save()}, intervalInSeconds*1000);
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
        partnershipLines : {"stroke-width": 1.25, stroke : '#303058'},
    graphToCanvasScale: 12,
    layoutRelativePersonWidth: 10,
    layoutRelativeOtherWidth: 2,
    layoutScale: { xscale: 12.0, yscale: 3.0 }
};

document.observe("dom:loaded",function() {
    editor = new PedigreeEditor();
});
