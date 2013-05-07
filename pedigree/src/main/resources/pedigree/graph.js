/**
 * Graph is responsible for the adding and removal of nodes. It is also responsible for
 * node selection and interaction between nodes.
 *
 * @class Graph
 * @constructor
 */

var Graph = Class.create({

    initialize: function() {
        this.hoverModeZones = editor.getPaper().set();
        this._placeHolderNodes = [];
        this._partnershipNodes = [];
        this._pregnancyNodes = [];
        this._personGroupNodes = [];
        this._personNodes = [];
        this._idCount = 1;
        this._nodeMap = {};
        this._currentHoveredNode = null;
        this._currentDraggable = null;
    },

    /**
     * Returns a map node IDs to nodes
     *
     * @method getNodeMap
     * @return {Object}
     *
     {
        {nodeID} : {AbstractNode}
     }
     */
    getNodeMap: function() {
        return this._nodeMap;
    },

    /**
     * Returns the node that is currently selected
     *
     * @method getCurrentHoveredNode
     * @return {AbstractNode}
     */
    getCurrentHoveredNode: function() {
        return this._currentHoveredNode;
    },

    /**
     * Changes currentHoveredNode to the specified node.
     *
     * @method getCurrentHoveredNode
     * @param {AbstractNode} node
     */
    setCurrentHoveredNode: function(node) {
        this._currentHoveredNode = node;
    },

    /**
     * Returns the currently dragged element
     *
     * @method getCurrentDraggable
     * @return Either a handle from a hoverbox, or a PlaceHolder
     */
    getCurrentDraggable: function() {
        return this._currentDraggable;
    },

    /**
     * Returns the Object that is currently being dragged
     *
     * @method setCurrentDraggable
     * @param draggable A handle or a PlaceHolder
     */
    setCurrentDraggable: function(draggable) {
        this._currentDraggable = draggable;
    },

    /**
     * Generates an id for a node
     *
     * @method generateID
     * @return {Number} A unique id
     */
    generateID: function() {
        return this._idCount++;
    },

    /**
     * Returns the highest generated id in this graph
     *
     * @method getIdCount
     * @return {Number}
     */
    getIdCount: function() {
        return this._idCount
    },

    /**
     * Sets the highest generated id to maxID
     *
     * @method setIdCount
     * @param maxID
     */
    setIdCount: function(maxID) {
        this._idCount = maxID;
    },

    /**
     * Returns the Proband node
     *
     * @method getProband
     * @return {Person}
     */
    getProband: function() {
        return this.getNodeMap()[1];
    },

    /**
     * Returns a list containing all the nodes in the graph
     *
     * @method getAllNodes
     * @return {Array} A list containing all nodes. The last element in the proband.
     */
    getAllNodes: function() {
        var pregs = this.getPregnancyNodes(),
            partnerships = this.getPartnershipNodes(),
            placeHolders = this.getPlaceHolderNodes(),
            persons = this.getPersonNodes(),
            personGroups = this.getPersonGroupNodes();

        return pregs.concat(partnerships, placeHolders, personGroups, persons.reverse());
    },

    /**
     * Deletes all nodes except the proband.
     *
     * @method clearGraph
     * @param {Boolean} removeProband If True, the proband is deleted as well.
     */
    clearGraph: function(removeProband) {
        var nodes = this.getAllNodes();
        var length = removeProband ? nodes.length : nodes.length - 1;
        for(var i = 0 ; i< length ; i++) {
            nodes[i] && nodes[i].remove(false);
        }
    },

    /**
     * Removes all nodes except the proband and adds this action to the action stack.
     *
     * @method clearGraphAction
     */
    clearGraphAction: function() {
        var lastAction = editor.getActionStack().peek();
        if(!lastAction || lastAction.property != "clearGraph") {
            var saveData = editor.getSaveLoadEngine().serialize();
            this.clearGraph(false);
            var undo = function() {
                editor.getSaveLoadEngine().load(saveData);
            };
            var redo = function() {
                editor.getGraph().clearGraph(false);
            };
            editor.getActionStack().push({undo: undo, redo:redo, property: "clearGraph"});
        }
    },

    /**
     * Creates a Partnership and adds it to index of nodes.
     *
     * @method addPartnership
     * @param {Number} x The x coordinate for the node
     * @param {Number} y The y coordinate for the node
     * @param {AbstractNode} node1 The first node in the partnership
     * @param {AbstractNode} node2 The second node in the partnership
     * @param {Number} [id] The id of the node
     * @return {Partnership}
     */
    addPartnership : function(x, y, node1, node2, id) {
        var partnership = new Partnership(x, y, node1, node2, id);
        this.getNodeMap()[partnership.getID()] = partnership;
        editor.getNodeIndex()._addNode(partnership, true);
        this._partnershipNodes.push(partnership);
        return partnership;
    },

    /**
     * Removes partnership from index of nodes
     *
     * @method removePartnership
     * @param partnership
     */
    removePartnership: function(partnership) {
        delete this.getNodeMap()[partnership.getID()];
        this._partnershipNodes = this._partnershipNodes.without(partnership);
    },

    /**
     * Returns list containing all the pregnancy nodes in the graph
     *
     * @method getPregnancyNodes
     * @return {Array}
     */
    getPregnancyNodes: function() {
        return this._pregnancyNodes;
    },

    /**
     * Returns list containing all the Person nodes in the graph
     *
     * @method getPersonNodes
     * @return {Array}
     */
    getPersonNodes: function() {
        return this._personNodes;
    },

    /**
     * Returns list containing all the PlaceHolder nodes in the graph
     *
     * @method getPlaceHolderNodes
     * @return {Array}
     */
    getPlaceHolderNodes: function() {
        return this._placeHolderNodes;
    },

    /**
     * Returns list containing all the Partnership nodes in the graph
     *
     * @method getPartnershipNodes
     * @return {Array}
     */
    getPartnershipNodes: function() {
        return this._partnershipNodes;
    },

    /**
     * Returns list containing all the PersonGroup nodes in the graph
     *
     * @method getPersonGroupNodes
     * @return {Array}
     */
    getPersonGroupNodes: function() {
        return this._personGroupNodes;
    },

    /**
     * Creates a Person in the graph and returns it
     *
     * @method addPerson
     * @param {Number} x The x coordinate for the node
     * @param {Number} y The y coordinate for the node
     * @param {String} gender Can be "M", "F", or "U"
     * @param {Number} [id] The id of the node
     * @return {Person}
     */
    addPerson: function(x, y, gender, id) {
        var isProband = this.getPersonNodes().length == 0;
        if(!isProband) {
        }
        var node = new Person(x, y, gender, id, isProband);
        this.getPersonNodes().push(node);
        this.getNodeMap()[node.getID()] = node;
        editor.getNodeIndex()._addNode(node, true);
        return node;
    },

    removePerson: function(person) {
        delete this.getNodeMap()[person.getID()];
        this._personNodes = this._personNodes.without(person);
    },

    /**
     * Creates a PlaceHolder in the graph and returns it
     *
     * @method addPlaceHolder
     * @param {Number} x The x coordinate for the node
     * @param {Number} y The y coordinate for the node
     * @param {String} gender Can be "M", "F", or "U"
     * @param {Number} [id] The id of the node
     * @return {PlaceHolder}
     */
    addPlaceHolder: function(x, y, gender, id) {
        var node = new PlaceHolder(x, y, gender, id);
        this.getPlaceHolderNodes().push(node);
        this.getNodeMap()[node.getID()] = node;
        editor.getNodeIndex()._addNode(node, true);
        return node;
    },

    /**
     * Removes given PlaceHolder node from node index (Does not delete the node visuals).
     *
     * @method removePlaceHolder
     * @param {PlaceHolder} placeholder
     */
    removePlaceHolder: function(placeholder) {
        delete this.getNodeMap()[placeholder.getID()];
        this._placeHolderNodes = this._placeHolderNodes.without(placeholder);
    },

    /**
     * Creates a PersonGroup in the graph and returns it
     *
     * @method addPersonGroup
     * @param {Number} x The x coordinate for the node
     * @param {Number} y The y coordinate for the node
     * @param {String} gender Can be "M", "F", or "U"
     * @param {Number} [id] The id of the node
     * @return {PersonGroup}
     */
    addPersonGroup: function(x, y, gender, id) {
        var node = new PersonGroup(x, y, gender, id);
        this.getPersonGroupNodes().push(node);
        this.getNodeMap()[node.getID()] = node;
        editor.getNodeIndex()._addNode(node, true);
        return node;
    },

    /**
     * Removes given PersonGroup node from node index (Does not delete the node visuals).
     *
     * @method removePersonGroup
     * @param {PersonGroup} groupNode
     */
    removePersonGroup: function(groupNode) {
        delete this.getNodeMap()[groupNode.getID()];
        this._personGroupNodes = this._personGroupNodes.without(groupNode);
    },

    /**
     * Creates a Pregnancy node in the graph and returns it
     *
     * @method addPregnancy
     * @param {Number} x The x coordinate for the node
     * @param {Number} y The y coordinate for the node
     * @param {Partnership} partnership The Partnership that has this pregnancy
     * @param {Number} [id] The id of the node
     * @return {Pregnancy}
     */
    addPregnancy: function(x, y, partnership, id) {
        var node = new Pregnancy(x, y, partnership, id);
        this.getPregnancyNodes().push(node);
        this.getNodeMap()[node.getID()] = node;
        editor.getNodeIndex()._addNode(node, true);
        return node;
    },

    /**
     * Removes given Pregnancy node from node index (Does not delete the node visuals).
     *
     * @method removePregnancy
     * @param {Pregnancy} pregnancy
     */
    removePregnancy: function(pregnancy) {
        delete this.getNodeMap()[pregnancy.getID()];
        this._pregnancyNodes = this._pregnancyNodes.without(pregnancy);
    },

    /**
     * Enters hover-mode state, which is when a handle or a PlaceHolder is being dragged around the screen
     *
     * @method enterHoverMode
     * @param sourceNode The node whose handle is being dragged, or the placeholder that is being dragged
     * @param {Array} hoverTypes An array of strings containing the types of nodes that "react" to the sourceNode being
     * dragged on top of them.
     */
    enterHoverMode: function(sourceNode, hoverTypes) {
        if(this.getCurrentDraggable().getType() == "parent") {
            this.getPartnershipNodes().each(function(partnershipBubble) {
                partnershipBubble.getGraphics().grow();
            })
        }
        var me = this,
            color,
            hoverNodes = [];
        hoverTypes.each(function(type) {
            hoverNodes = hoverNodes.concat(me["get" + type + "Nodes"]())
        });
        hoverNodes.without(sourceNode).each(function(node) {
            var hoverModeZone = node.getGraphics().getHoverBox().getHoverZoneMask().clone().toFront();
            hoverModeZone.attr("cursor", "pointer");
            hoverModeZone.hover(
                function() {
                    me._currentHoveredNode = node;
                    node.getGraphics().getHoverBox().setHovered(true);
                    node.getGraphics().getHoverBox().getBoxOnHover().attr(PedigreeEditor.attributes.boxOnHover);

                    if(me.getCurrentDraggable().getType() == 'PlaceHolder' && me.getCurrentDraggable().canMergeWith(node)) {
                        me.getCurrentDraggable().validHoveredNode = node;
                        color = "green";
                    }
                    else if(me.getCurrentDraggable().getType() == "partner" && sourceNode.canPartnerWith(node)) {
                        node.validPartnerSelected = true;
                        color = "green";
                    }
                    else if(me.getCurrentDraggable().getType() == "child" && sourceNode.canBeParentOf(node)) {
                        node.validChildSelected = true;
                        color = "green";
                    }
                    else if(me.getCurrentDraggable().getType() == "parent" && node.canBeParentOf(sourceNode)) {
                        if(node.getType() == 'Person') {
                            node.validParentSelected = true;
                        }
                        else {
                            node.validParentsSelected = true;
                        }
                        color = "green";
                    }
                    else {
                        color = "red";
                    }
                    node.getGraphics().getHoverBox().getBoxOnHover().attr("fill", color);
                },
                function() {
                    me.getCurrentDraggable() && (me.getCurrentDraggable().validHoveredNode = null);
                    node.getGraphics().getHoverBox().setHovered(false);
                    node.getGraphics().getHoverBox().getBoxOnHover().attr(PedigreeEditor.attributes.boxOnHover).attr('opacity', 0);
                    me._currentHoveredNode = null;
                    node.validPartnerSelected = node.validChildSelected =  node.validParentSelected = node.validParentsSelected = false;
                });
            me.hoverModeZones.push(hoverModeZone);
        });
    },

    /**
     * Exits hover-mode state, which is when a handle or a PlaceHolder is being dragged around the screen
     *
     * @method exitHoverMode
     */
    exitHoverMode: function() {
        this.hoverModeZones.remove();
        this.getPartnershipNodes().each(function(partnership) {
            partnership.getGraphics().area && partnership.getGraphics().area.remove();
        });
    }
});