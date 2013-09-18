/**
 * GraphicsSet is responsible for the adding and removal of nodes. It is also responsible for
 * node selection and interaction between nodes.
 *
 * @class GraphicsSet
 * @constructor
 */

var GraphicsSet = Class.create({

    initialize: function() {
    	console.log("--- graph init ---");
    	
    	this._nodeMap = {};    	// {nodeID} : {AbstractNode}
    	
        this.hoverModeZones = editor.getPaper().set();
        
        this._currentMarkedNew   = [];
        this._currentGrownNodes  = [];             
        this._currentHoveredNode = null;
        this._currentDraggable   = null;        
    },

    /**
     * Returns a map of node IDs to nodes
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
     * Returns a node with the given node ID
     *
     * @method getNode
     * @param {nodeId} id of the node to be returned
     * @return {AbstractNode}
     *
     */    
    getNode: function(nodeId) {
        return this._nodeMap[nodeId];
    },    

    /**
     * Returns the person node containing x and y coordinates, or null if outside all person nodes
     *
     * @method getPersonNodeNear
     * @return {Object} or null
     */    
    getPersonNodeNear: function(x, y) {
        for (var nodeID in this._nodeMap) {
            if (this._nodeMap.hasOwnProperty(nodeID)) {
                var node = this.getNode(nodeID);
                if (node.getType() == "Person" && node.getGraphics().containsXY(x,y))
                    return node;
            }
        }
        return null;
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
     * Removes given node from node index (Does not delete the node visuals).
     *
     * @method removeFromNodeMap
     * @param {nodeId} id of the node to be removed
     */    
    removeFromNodeMap: function(nodeID) {
        delete this.getNodeMap()[nodeID];        
    },
    
    /**
     * Deletes all nodes except the proband.
     *
     * @method clearGraph
     */
    clearGraph: function() {                
    	for (var node in this.getNodeMap()) { 
    		if (this.getNodeMap().hasOwnProperty(node) && this.getNodeMap()[node].getID() != 0) {
    			this.getNodeMap()[node].remove(true);
    			this.removeFromNodeMap(node);    			
    		}
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
            this.clearGraph();
            var undo = function() {
                editor.getSaveLoadEngine().createGraphFromSerializedData(saveData);
            };
            var redo = function() {
                editor.getGraphicsSet().clearGraph();
            };
            editor.getActionStack().push({undo: undo, redo:redo, property: "clearGraph"});
        }
    },
 
    /**
     * Creates a new node in the graph and returns it. The node type is obtained from
     * editor.getGraph() and may be on of Person, Partnership or ... TODO. The position
     * of the node is also obtained form editor.getGraph()
     *
     * @method addPerson
     * @param {Number} [id] The id of the node
     * @return {Person}
     */
    addNode: function(id) {
        console.log("add node");
        var positionedGraph = editor.getGraph();
        
        if (!positionedGraph.isValidID(id))
            throw "addNode(): Invalid id";

        var node;        
        var properties = positionedGraph.getProperties(id);        
        
        var graphPos = positionedGraph.getPosition(id);
        var position = editor.convertGraphCoordToCanvasCoord(graphPos.x, graphPos.y );        
        
        if (positionedGraph.isRelationship(id)) {
            console.log("-> add partnership");
            node = new Partnership(position.x, position.y, id);
        }
        else if (positionedGraph.isPerson(id)) {
            console.log("-> add person");
            node = new Person(position.x, position.y, properties["gender"], id);
        }
        else {
            throw "addNode(): unsupported node type";
        }
        
        //console.log("properties: " + stringifyObject(properties));
        node.assignProperties(properties);
        
        this.getNodeMap()[id] = node;
        
        return node;
    },
    
    moveNode: function(id, animate) {
        //console.log("moving: " + id + ", animate: " + animate);
        var positionedGraph = editor.getGraph();
        var graphPos = positionedGraph.getPosition(id);
        var position = editor.convertGraphCoordToCanvasCoord(graphPos.x, graphPos.y );
        this.getNode(id).setPos(position.x, position.y, animate);
    },
  
    /**
     * Enters hover-mode state, which is when a handle or a PlaceHolder is being dragged around the screen
     *
     * @method enterHoverMode
     * @param sourceNode The node whose handle is being dragged, or the placeholder that is being dragged
     * @param hoverTypes Should be 'parent', 'child' or 'partner'. Only nodes which can be in the correponding
     *                   relationship with sourceNode will be highlighted
     * dragged on top of them.
     */
    enterHoverMode: function(sourceNode, hoverType) {
        
        var me = this;
        var validTargets = this.getValidDragTargets(sourceNode.getID(), hoverType);
        
        validTargets.each(function(nodeID) {
            me._currentGrownNodes.push(nodeID);
            
            var node = me.getNode(nodeID);            
            node.getGraphics().grow();
                        
            var hoverModeZone = node.getGraphics().getHoverBox().getHoverZoneMask().clone().toFront();
            hoverModeZone.attr("cursor", "pointer");
            hoverModeZone.hover(
                function() {
                    me._currentHoveredNode = nodeID;
                    node.getGraphics().getHoverBox().setHighlighted(true);
                },
                function() {                    
                    me._currentHoveredNode = null;
                    node.getGraphics().getHoverBox().setHighlighted(false);                    
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
        
        var me = this;
        this._currentGrownNodes.each(function(nodeID) {
            var node = me.getNode(nodeID)
            node.getGraphics().shrink();
            node.getGraphics().getHoverBox().setHighlighted(false);            
        });
        
        this._currentGrownNodes = [];
    },
    
    getValidDragTargets: function(sourceNodeID, hoverType) {
        var result = [];
        switch (hoverType) {
        case "child":
            // all person nodes which are not ancestors of sourse node and which do not already have parents            
            result = editor.getGraph().getPossibleChildrenOf(sourceNodeID);
            break;
        case "parent":
            // all person nodes which are not descendants of source node
            // TODO: plus all relationships?
            result = editor.getGraph().getPossibleParentsOf(sourceNodeID);
            break;
        case "partnerR":            
        case "partnerL":
            // all person nodes of the other gender or unknown gender
            var oppositeGender  = this.getNode(sourceNodeID).getOppositeGender();
            var validGendersSet = (oppositeGender == 'U') ? ['m','f','u'] : [oppositeGender,'u'];            
            result = editor.getGraph().getAllPersonsOfGenders(validGendersSet);
            result = result.without(sourceNodeID);
            //console.log("possible partners: " + stringifyObject(result));
            break;
        case "PlaceHolder":
            // all nodes which can be this placehodler: e.g. all that can be child of it's parents && 
            // partners of it's partners
            throw "TODO";
        default:
            throw "Incorrect hoverType";
        }     
        return result;
    },
    
    applyChanges: function( changeSet, markNew ) {
        // applies change set of the form {"new": {list of nodes}, "moved": {list of nodes} }        
        console.log("Change set: " + stringifyObject(changeSet));
        
        for (var i = 0; i < this._currentMarkedNew.length; i++) {
            var node = this.getNode(this._currentMarkedNew[i]);
            node.getGraphics().unmark();
        }
        this._currentMarkedNew = [];
        
        // 0. remove all removed
        //
        // 1. move all person nodes
        // 2. create all new person nodes
        //
        // 3. move all existing relationships - as all lines are attached to relationships we want to draw
        //                                      them after all person nodes are already in correct position
        // 4. create new relationships        
                
        if (changeSet.hasOwnProperty("removed")) {
            for (var i = 0; i < changeSet.removed.length; i++) {
                var nextRemoved = changeSet.removed[i];
                
                this.getNodeMap()[nextRemoved].remove(true);
                this.removeFromNodeMap(nextRemoved);
            }
        }
                   
        var movedPersons       = [];
        var movedRelationships = [];
        var newPersons         = [];
        var newRelationships   = [];
        var animate            = {};
        
        if (changeSet.hasOwnProperty("animate")) {
            for (var i = 0; i < changeSet.animate.length; i++) {
                //animate[changeSet.animate[i]] = true;     // TODO: animations disabled becaus ehoverboxes behave strangely
            }
        }
        
        if (changeSet.hasOwnProperty("moved")) {
            for (var i = 0; i < changeSet.moved.length; i++) {
                var nextMoved = changeSet.moved[i];                
                if (editor.getGraph().DG.GG.isRelationship(nextMoved))
                    movedRelationships.push(nextMoved);
                else
                    movedPersons.push(nextMoved);
            }
        }
        if (changeSet.hasOwnProperty("new")) {
            for (var i = 0; i < changeSet.new.length; i++) {
                var nextNew = changeSet.new[i];                
                if (editor.getGraph().DG.GG.isRelationship(nextNew))
                    newRelationships.push(nextNew);
                else
                    newPersons.push(nextNew);
            }
        }        
        
        // TODO: find which relationship nodes are affected by the move of other relationship nodes
        //       via LineSet (because of the line intersection graphics) and add them to the moved set

        for (var i = 0; i < movedPersons.length; i++)
            editor.getGraphicsSet().moveNode(movedPersons[i], animate.hasOwnProperty(movedPersons[i]));        
        for (var i = 0; i < newPersons.length; i++) {
            var newPerson = editor.getGraphicsSet().addNode(newPersons[i]);
            if (markNew) {
                newPerson.getGraphics().markPermanently();
                this._currentMarkedNew.push(newPersons[i]);
            }
        }
        
        for (var i = 0; i < movedRelationships.length; i++)
            editor.getGraphicsSet().moveNode(movedRelationships[i]);        
        for (var i = 0; i < newRelationships.length; i++)
            editor.getGraphicsSet().addNode(newRelationships[i]);
                
        if (changeSet.hasOwnProperty("highlight")) {
            for (var i = 0; i < changeSet.highlight.length; i++) {
                var nextHighlight = changeSet.highlight[i];
                //this.getNode(nextHighlight).getGraphics().markPermanently();
                //this._currentMarkedNew.push(nextHighlight);                
            }
        }
    }
});
