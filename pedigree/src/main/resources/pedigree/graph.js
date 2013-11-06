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
        
        this._lineSet = new LineSet();   // used to track intersecting lines
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
        if (!this._nodeMap.hasOwnProperty(nodeId)) {
            console.log("ERROR: requesting non-existent node " + nodeId); 
            return null;
        }
        return this._nodeMap[nodeId];
    },    
    
    getMaxNodeID: function() {
        var max = 0;
        for (node in this._nodeMap)
            if (this._nodeMap.hasOwnProperty(node))
                if (parseInt(node) > max)
                    max = node;
        return max;
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
                if ((node.getType() == "Person" || node.getType() == "PersonGroup") && node.getGraphics().containsXY(x,y))
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
     * Creates a new set of raphael objects representing a curve from (xFrom, yFrom) trough (...,yTop) to (xTo, yTo).
     * The bend from (xTo,yTo) to vertical level yTop will happen "lastBend" pixels from xTo.
     * In case the flat part intersects any existing known lines a special crossing is drawn and added to the set.
     *
     * @method drawCurvedLineWithCrossings
     */    
    drawCurvedLineWithCrossings: function ( id, xFrom, yFrom, yTop, xTo, yTo, lastBend, attr, twoLines, secondLineBelow ) {
        //console.log("yFrom: " + yFrom + ", yTo: " + yTo + ", yTop: " + yTop);
        
        if (yFrom == yTop && yFrom == yTo)            
            return editor.getGraphicsSet().drawLineWithCrossings(id, xFrom, yFrom, xTo, yTo, attr, twoLines, secondLineBelow);
        
        var cornerRadius     = PedigreeEditor.attributes.curvedLinesCornerRadius * 0.8;
        var goesRight        = ( xFrom > xTo );
        var xFinalBend       = goesRight ? xTo + lastBend                  : xTo - lastBend; 
        var xFinalBendVert   = goesRight ? xTo + lastBend + cornerRadius   : xTo - lastBend - cornerRadius;
        var xBeforeFinalBend = goesRight ? xTo + lastBend + cornerRadius*2 : xTo - lastBend - cornerRadius*2;
        var xFromAndBit        = goesRight ? xFrom - cornerRadius/2        : xFrom + cornerRadius/2;
        var xFromAfterCorner   = goesRight ? xFromAndBit - cornerRadius    : xFromAndBit + cornerRadius;
        var xFromAfter2Corners = goesRight ? xFromAndBit - 2*cornerRadius  : xFromAndBit + 2 * cornerRadius;
        
        //console.log("XFinalBend: " + xFinalBend + ", xTo : " + xTo);
        
        if (yFrom <= yTop) {
            editor.getGraphicsSet().drawLineWithCrossings(id, xFrom, yFrom, xBeforeFinalBend, yFrom, attr, twoLines, !goesRight, true);
        }
        else {
            editor.getGraphicsSet().drawLineWithCrossings(id, xFrom, yFrom, xFromAndBit, yFrom, attr, twoLines, !goesRight, true);
            if (goesRight)
                drawCornerCurve( xFromAndBit, yFrom, xFromAfterCorner, yFrom-cornerRadius, true, attr, twoLines, -2.5, 2.5, 2.5, -2.5 );
            else
                drawCornerCurve( xFromAndBit, yFrom, xFromAfterCorner, yFrom-cornerRadius, true, attr, twoLines, 2.5, 2.5, -2.5, -2.5 );            
            editor.getGraphicsSet().drawLineWithCrossings(id, xFromAfterCorner, yFrom-cornerRadius, xFromAfterCorner, yTop+cornerRadius, attr, twoLines, goesRight);
            if (goesRight)
                drawCornerCurve( xFromAfterCorner, yTop+cornerRadius, xFromAfter2Corners, yTop, false, attr, twoLines, -2.5, 2.5, 2.5, -2.5 );
            else
                drawCornerCurve( xFromAfterCorner, yTop+cornerRadius, xFromAfter2Corners, yTop, false, attr, twoLines, 2.5, 2.5, -2.5, -2.5 );            
            editor.getGraphicsSet().drawLineWithCrossings(id, xFromAfter2Corners, yTop, xBeforeFinalBend, yTop, attr, twoLines, !goesRight, true);
        }
        
        // curve down to yTo level
        // draw corner        
        if (goesRight)
            drawCornerCurve( xBeforeFinalBend, yTop, xFinalBendVert, yTop+cornerRadius, true, attr, twoLines, 2.5, 2.5, -2.5, -2.5 );
        else
            drawCornerCurve( xBeforeFinalBend, yTop, xFinalBendVert, yTop+cornerRadius, true, attr, twoLines, 2.5, -2.5, -2.5, 2.5 );
        editor.getGraphicsSet().drawLineWithCrossings(id, xFinalBendVert, yTop+cornerRadius, xFinalBendVert, yTo-cornerRadius, attr, twoLines, !goesRight);
        if (goesRight)
            drawCornerCurve( xFinalBendVert, yTo-cornerRadius, xFinalBend, yTo, false, attr, twoLines, 2.5, 2.5, -2.5, -2.5 );
        else
            drawCornerCurve( xFinalBendVert, yTo-cornerRadius, xFinalBend, yTo, false, attr, twoLines, 2.5, -2.5, -2.5, 2.5 );
        editor.getGraphicsSet().drawLineWithCrossings(id, xFinalBend, yTo, xTo, yTo, attr, twoLines, !goesRight);

    },
    
    /**
     * Creates a new set of raphael objects representing a line segment from (x1,y1) to (x2,y2).
     * In case this line segment intersects any existing known segments a special crossing is drawn and added to the set.
     *
     * @method drawLineWithCrossings
     */    
    drawLineWithCrossings: function(owner, x1, y1, x2, y2, attr, twoLines, secondLineBelow, bothEndsGoDown) {
        
        // make sure line goes from the left to the right (and if vertical from the top to the bottom):
        // this simplifies drawing the line piece by piece from intersection to intersection
        if (x1 > x2 || ((x1 == x2) && (y1 > y2))) {
            var tx = x1;
            var ty = y1;
            x1 = x2;
            y1 = y2;
            x2 = tx;
            y2 = ty;            
        }
        
        var isHorizontal = (y1 == y2);
        var isVertical   = (x1 == x2);
                                       
        var intersections = this._lineSet.addLine( owner, x1, y1, x2, y2 );                        
        
        // sort intersections by distance form the start
        var compareDistanceToStart = function( p1, p2 ) {
                var dist1 = (x1-p1.x)*(x1-p1.x) + (y1-p1.y)*(y1-p1.y);
                var dist2 = (x1-p2.x)*(x1-p2.x) + (y1-p2.y)*(y1-p2.y);
                return dist1 > dist2;
            };
        intersections.sort(compareDistanceToStart);        
        //console.log("intersection points: " + stringifyObject(intersections));
        
        for (var lineNum = 0; lineNum < (twoLines ? 2 : 1); lineNum++) { 
            
            // TODO: this is a bit hairy, just a quick hack to make two nice parallel curves
            //       for consang. relatiomnships: simple raphael.transform() does not work well
            //       because then the curves around crossings wont be exactly above the crossing then
            if (twoLines) {
                if (!bothEndsGoDown) {
                    x1 += (-2.5 + lineNum * 7.5);
                    x2 += (-2.5 + lineNum * 7.5);
                } else {
                    x1 -= 2.5;
                    x2 += 2.5;
                }
                
                if (secondLineBelow) {
                    y1 += ( 2.5 - lineNum * 7.5);
                    y2 += ( 2.5 - lineNum * 7.5);
                }
                else {
                    y1 += (-2.5 + lineNum * 7.5);
                    y2 += (-2.5 + lineNum * 7.5);
                }
            }
            
            var raphaelPath = "M " + x1 + " " + y1;            
            for (var i = 0; i < intersections.length; i++) {
                var intersectPoint = intersections[i];
                
                var distance = function(p1, p2) {
                    return (p1.x-p2.x)*(p1.x-p2.x) + (p1.y-p2.y)*(p1.y-p2.y);
                };                
                if (distance(intersectPoint, {"x": x1, "y": y1}) < 20*20)
                    continue;
                if (distance(intersectPoint, {"x": x2, "y": y2}) < 20*20)
                    continue;
                
                if (isHorizontal) {                    
                    if (twoLines) {
                        if (secondLineBelow)
                            intersectPoint.y += ( 2.5 - lineNum * 7.5);
                        else
                            intersectPoint.y += (-2.5 + lineNum * 7.5);
                    }                    
                    // a curve above the crossing
                    raphaelPath += " L " + (intersectPoint.x - 10) + " " + intersectPoint.y;                    
                    raphaelPath += " C " + (intersectPoint.x - 7)  + " " + (intersectPoint.y + 1) +
                                     " " + (intersectPoint.x - 7)  + " " + (intersectPoint.y - 7) +
                                     " " + (intersectPoint.x)      + " " + (intersectPoint.y - 7);
                    raphaelPath += " C " + (intersectPoint.x + 7)  + " " + (intersectPoint.y - 7) +
                                     " " + (intersectPoint.x + 7)  + " " + (intersectPoint.y + 1) +
                                     " " + (intersectPoint.x + 10) + " " + (intersectPoint.y);                                        
                } else if (isVertical) {
                    if (twoLines) {
                        intersectPoint.x += ( -2.5 + lineNum * 7.5);
                    }
                    // a curve on the right around crossing
                    raphaelPath += " L " + intersectPoint.x        + " " + (intersectPoint.y - 10);
                    raphaelPath += " C " + (intersectPoint.x - 1)  + " " + (intersectPoint.y - 7) +
                                     " " + (intersectPoint.x + 7)  + " " + (intersectPoint.y - 7) +
                                     " " + (intersectPoint.x + 7)  + " " + (intersectPoint.y);
                    raphaelPath += " C " + (intersectPoint.x + 7)  + " " + (intersectPoint.y + 7) +
                                     " " + (intersectPoint.x - 1)  + " " + (intersectPoint.y + 7) +
                                     " " + (intersectPoint.x)      + " " + (intersectPoint.y + 10);                    
                }   
                // else: some diagonal line: presumably there should be none, if there are some
                //       everything will be ok except there will be no special intersection graphic drawn                
            }        
            raphaelPath += " L " + x2 + " " + y2; 
            editor.getPaper().path(raphaelPath).attr(attr).toBack();
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
        //console.log("add node");
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
        else if (positionedGraph.isPersonGroup(id)) {
            console.log("-> add person group");
            node = new PersonGroup(position.x, position.y, properties["gender"], id, properties["numPersons"]);
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
        var positionedGraph = editor.getGraph();
        var graphPos = positionedGraph.getPosition(id);
        var position = editor.convertGraphCoordToCanvasCoord(graphPos.x, graphPos.y );
        this.getNode(id).setPos(position.x, position.y, animate);
    },
    
    changeNodeIds: function( changedIdsSet ) {
        var newNodeMap = {};
        
        // change all IDs at once so that have both new and old references at the same time
        for (oldID in this._nodeMap) {
            var node  = this.getNode(oldID);
            
            var newID = changedIdsSet.hasOwnProperty(oldID) ? changedIdsSet[oldID] : oldID;                             
            node.setID( newID );
             
            newNodeMap[newID] = node;
        }
        
        this._nodeMap = newNodeMap;
        
        this._lineSet.replaceIDs(changedIdsSet);
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
        this._currentHoveredNode = null;
        
        this.hoverModeZones.remove();
        
        var me = this;
        this._currentGrownNodes.each(function(nodeID) {
            var node = me.getNode(nodeID)
            node.getGraphics().shrink();
            node.getGraphics().getHoverBox().setHighlighted(false);            
        });
        
        this._currentGrownNodes = [];
    },
    
    unmarkAll: function() {
        for (var i = 0; i < this._currentMarkedNew.length; i++) {
            var node = this.getNode(this._currentMarkedNew[i]);
            node.getGraphics().unmark();
        }
        this._currentMarkedNew = [];        
    },
    
    getValidDragTargets: function(sourceNodeID, hoverType) {
        var result = [];
        switch (hoverType) {
        case "child":
            // all person nodes which are not ancestors of sourse node and which do not already have parents            
            result = editor.getGraph().getPossibleChildrenOf(sourceNodeID);
            break;
        case "parent":
            result = editor.getGraph().getPossibleParentsOf(sourceNodeID);
            break;
        case "partnerR":            
        case "partnerL":
            // all person nodes of the other gender or unknown gender (who ar enot already partners)
            result = editor.getGraph().getPossiblePartnersOf(sourceNodeID)
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
        
        var timer = new Timer();
        var timer2 = new Timer();
        
        try {
        
        this.unmarkAll();
        
        // to simplify code which deals woith removed nodes making other mnodes to move
        if (!changeSet.hasOwnProperty("moved"))
            changeSet["moved"] = [];               
        if (!changeSet.hasOwnProperty("removed"))
            changeSet["removed"] = []; 
        if (!changeSet.hasOwnProperty("removedInternally"))        
            changeSet["removedInternally"] = [];
        
        // 0. remove all removed
        //
        // 1. move all person nodes
        // 2. create all new person nodes
        //
        // 3. move all existing relationships - as all lines are attached to relationships we want to draw
        //                                      them after all person nodes are already in correct position
        // 4. create new relationships        
                
                        
        if (changeSet.hasOwnProperty("removed")) {
            var affectedByLineRemoval = {};
            
            for (var i = 0; i < changeSet.removed.length; i++) {
                var nextRemoved = changeSet.removed[i];
                
                this.getNodeMap()[nextRemoved].remove();
                this.removeFromNodeMap(nextRemoved);
                
                var affected = this._lineSet.removeAllLinesAffectedByOwnerMovement(nextRemoved);
                
                for (var j = 0; j < affected.length; j++)
                    if (!arrayContains(changeSet.removed, affected[j])) { // ignore nodes which are removed anyway
                        //console.log("adding due to line removal: " + affected[j]);
                        affectedByLineRemoval[affected[j]] = true;
                    }
            }
                        
            // for each removed node all nodes with higher ids get their IDs shifted down by 1
            var idChanged = false;
            var changedIDs = {};
            var maxCurrentNodeId = this.getMaxNodeID();
            for (var i = 0; i < changeSet.removedInternally.length; i++) {                    
                var nextRemoved = changeSet.removedInternally[i];            
                for (var u = nextRemoved + 1; u <= maxCurrentNodeId; u++) {
                    idChanged = true;
                    if (!changedIDs.hasOwnProperty(u))
                        changedIDs[u] = u - 1;
                    else
                        changedIDs[u]--;                                        
                }
            }
            
            // change all IDs at once so that have both new and old references at the same time
            if (idChanged)
                this.changeNodeIds(changedIDs);
            
            //console.log("LineSet: " + stringifyObject(this._lineSet));
            
            for (node in affectedByLineRemoval)
                if (affectedByLineRemoval.hasOwnProperty(node)) {
                    var newID = changedIDs.hasOwnProperty(node) ? changedIDs[node] : node; 
                    if (!arrayContains(changeSet.moved, newID)) {
                        //console.log("moved due to line removal: oldID="+node + ", newID=" + newID); 
                        changeSet.moved.push(newID);
                    }
                }
        }
        
        timer.printSinceLast("=== Removal runtime: ");
                            
                   
        var movedPersons       = [];
        var movedRelationships = [];
        var newPersons         = [];
        var newRelationships   = [];
        var animate            = {};
        
        if (changeSet.hasOwnProperty("animate")) {
            for (var i = 0; i < changeSet.animate.length; i++) {                
                //animate[changeSet.animate[i]] = true;     // TODO: animations disabled because hoverboxes & labels behave strangely
            }
        }        
        
        //console.log("moved: " + stringifyObject(changeSet.moved));
                
        if (changeSet.hasOwnProperty("moved")) {
            // remove all lines so that we start drawing anew
            for (var i = 0; i < changeSet.moved.length; i++) {
                var nextMoved = changeSet.moved[i];                      
                if (editor.getGraph().isRelationship(nextMoved)) {        
                    var affected = this._lineSet.removeAllLinesAffectedByOwnerMovement(nextMoved);
                    for (var j = 0; j < affected.length; j++) {
                        var node = affected[j];                        
                        if (!arrayContains(changeSet.moved, node))
                            changeSet.moved.push(node);
                    }
                }
            }                        
               
            // move actual nodes
            for (var i = 0; i < changeSet.moved.length; i++) {
                var nextMoved = changeSet.moved[i];                
                if (editor.getGraph().isRelationship(nextMoved))
                    movedRelationships.push(nextMoved);                                       
                else
                    movedPersons.push(nextMoved);
            }
        }
        console.log("moved: " + stringifyObject(changeSet.moved));
        if (changeSet.hasOwnProperty("new")) {
            for (var i = 0; i < changeSet.new.length; i++) {
                var nextNew = changeSet.new[i];                
                if (editor.getGraph().isRelationship(nextNew))
                    newRelationships.push(nextNew);
                else
                    newPersons.push(nextNew);
            }
        }      
        
        timer.printSinceLast("=== Bookkeeping/sorting runtime: ");
        
        
        for (var i = 0; i < movedPersons.length; i++)
            editor.getGraphicsSet().moveNode(movedPersons[i], animate.hasOwnProperty(movedPersons[i]));
        
        timer.printSinceLast("=== Move persons runtime: ");
        
        for (var i = 0; i < newPersons.length; i++) {            
            var newPerson = editor.getGraphicsSet().addNode(newPersons[i]);
            if (markNew) {
                newPerson.getGraphics().markPermanently();
                this._currentMarkedNew.push(newPersons[i]);
            }
        }
        
        timer.printSinceLast("=== New persons runtime: ");
        
        for (var i = 0; i < movedRelationships.length; i++)
            editor.getGraphicsSet().moveNode(movedRelationships[i]);
        
        timer.printSinceLast("=== Move rels runtime: ");
        
        for (var i = 0; i < newRelationships.length; i++)
            editor.getGraphicsSet().addNode(newRelationships[i]);
        
        timer.printSinceLast("=== New rels runtime: ");
                
        if (changeSet.hasOwnProperty("highlight")) {
            for (var i = 0; i < changeSet.highlight.length; i++) {
                var nextHighlight = changeSet.highlight[i];
                this.getNode(nextHighlight).getGraphics().markPermanently();
                this._currentMarkedNew.push(nextHighlight);                
            }
        }
        
        
        // re-evaluate which handles are hidden and which are shown
        for (nodeID in this._nodeMap)
            if (this._nodeMap.hasOwnProperty(nodeID))
                if (editor.getGraph().isPerson(nodeID)) {
                    if (editor.getGraph().getParentRelationship(nodeID) !== null) {
                        this.getNode(nodeID).getGraphics().getHoverBox().hideParentHandle();
                    } else {
                        this.getNode(nodeID).getGraphics().getHoverBox().unHideParentHandle();
                    }
                }
            
        
        // TODO: move the viewport to make changeSet.makevisible nodes visible on screen
        
        timer.printSinceLast("=== highlight + update handled runtime: ");
        timer2.printSinceLast("=== Total aply changes runtime: ");
        
        } catch(err) {
            console.log("err: " + err);
        }           
    }
});
