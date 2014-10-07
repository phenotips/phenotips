/**
 * Class for visualizing partnerships and organizing the graphical elements.
 *
 * @class PartnershipVisuals
 * @extends AbstractNodeVisuals
 * @constructor
 * @param {Partnership} node The node for which the graphics are handled
 * @param {Number} x The x coordinate on the canvas
 * @param {Number} y The y coordinate on the canvas
 */
var PartnershipVisuals = Class.create(AbstractNodeVisuals, {

    initialize: function($super, partnership, x, y) {
        //console.log("partnership visuals");
        $super(partnership, x,y);
        this._childlessShape = null;
        this._childlessStatusLabel = null;
        this._junctionShape = editor.getPaper().circle(x,y, PedigreeEditor.attributes.partnershipRadius).attr(PedigreeEditor.attributes.partnershipNode);

        if (editor.isReadOnlyMode()) {
            this._hoverBox = new ReadOnlyHoverbox(partnership, x, y, this.getShapes());
        } else {        
            this._hoverBox = new PartnershipHoverbox(partnership, x, y, this.getShapes());
        }
        this.updateIDLabel();
        
        this._childhubConnection = null;
        this._partnerConnections = null;
        
        this.updatePartnerConnections();
        this.updateChildhubConnection();
        //console.log("partnership visuals end");
    },

    updateIDLabel: function() {
        var x = this.getX();
        var y = this.getY();
        this._idLabel && this._idLabel.remove();
        this._idLabel = editor.getPaper().text(x, y-20, editor.DEBUG_MODE ? this.getNode().getID() : "").attr(PedigreeEditor.attributes.dragMeLabel).insertAfter(this._junctionShape.flatten());
    },
    
    /**
     * Updates whatever needs to change when node id changes (e.g. id label) 
     *
     * @method onSetID
     */    
    onSetID: function($super, id) {
        $super(id);
        this.updateIDLabel();
    },
    
    /**
     * Expands the partnership circle
     *
     * @method grow
     */
    grow: function() {
        if (this.area) return;
        this.area = this.getJunctionShape().clone().flatten().insertBefore(this.getJunctionShape().flatten());
        this.area.attr({'fill': 'green', stroke: 'none'});
        this.area.ot = this.area.transform();
        this.area.animate(Raphael.animation({transform : "...S2"}, 400, 'bounce'));
    },
    
    /**
     * Shrinks node graphics to the original size
     *
     * @method shrink
     */    
    shrink: function() {
        this.area && this.area.remove();
        delete this.area;
    },

    /**
     * Marks the node in a way different from glow
     *
     * @method grow
     */
    markPregnancy: function() {
        // TODO: maybe mark pregnancy bubble?
        if (this.mark) return;
        this.mark = this.getJunctionShape().glow({width: 10, fill: true, opacity: 0.3, color: "blue"}).insertBefore(this.getJunctionShape().flatten());
    },
    
    /**
     * Unmarks the node
     *
     * @method unmark
     */    
    unmarkPregnancy: function() {
        this.mark && this.mark.remove();
        delete this.mark;
    },
    
    markPermanently: function() {
        if (this.mark2) return;
        this.mark2 = this.getJunctionShape().glow({width: 18, fill: true, opacity: 0.4, color: "#ee8d00"}).insertBefore(this.getJunctionShape().flatten());
    },
    unmark: function() {
        this.mark2 && this.mark2.remove();
        delete this.mark2;
    },
    
    /**
     * Returns the circle that joins connections
     *
     * @method getJunctionShape
     * @return {Raphael.st}
     */
    getJunctionShape: function() {
        return this._junctionShape;
    },
    
    /**
     * Returns the Y coordinate of the lowest part of this node's graphic on the canvas
     *
     * @method getY
     * @return {Number} The y coordinate
     */      
    getBottomY: function() {
        return this._absoluteY + PedigreeEditor.attributes.partnershipRadius + PedigreeEditor.attributes.childlessLength;
    },    
        
    /**
     * Updates the path of all connections to all partners
     *
     * @method updatePartnerConnections
     */
    updatePartnerConnections: function() {
        this._partnerConnections && this._partnerConnections.remove();
        
        editor.getPaper().setStart();
        
        var positionedGraph = editor.getGraph();
        
        var id = this.getNode().getID();
        
        var consangr = positionedGraph.isConsangrRelationship(id);
        var nodeConsangrPreference = this.getNode().getConsanguinity();
        if (nodeConsangrPreference == "N")
            consangr = false;
        if (nodeConsangrPreference == "Y")
            consangr = true;
            
        var lineAttr = consangr ? PedigreeEditor.attributes.consangrPartnershipLines : PedigreeEditor.attributes.partnershipLines;
        
        var partnerPaths = positionedGraph.getPathToParents(id);  // partnerPaths = [ [virtual_node_11, ..., virtual_node_1n, parent1], [virtual_node_21, ..., virtual_node_2n, parent21] ]
        
        // TODO: a better curve algo for the entire curve at once?
        var smoothCorners = true;                                   
        var cornerRadius  = PedigreeEditor.attributes.curvedLinesCornerRadius;

        for (var p = 0; p < partnerPaths.length; p++) {
            var path = partnerPaths[p];

            // for the last piece which attaches to the person:
            // need to consider which attachment point to use, and may have to do a bended curve from current Y to the attachment point Y
            var person           = path[path.length-1];
            var finalSegmentInfo = editor.getGraph().getRelationshipLineInfo(id, person);

            var nodePos       = editor.getGraph().getPosition(person);
            var finalPosition = editor.convertGraphCoordToCanvasCoord( nodePos.x, nodePos.y );
            var finalYTo      = editor.convertGraphCoordToCanvasCoord( 0, finalSegmentInfo.attachY ).y;
            var yTop          = editor.convertGraphCoordToCanvasCoord( 0, finalSegmentInfo.verticalY ).y;
            var lastBend      = ((finalYTo == yTop) && (yTop < this.getY()) && finalSegmentInfo.attachmentPort == 1) ?
                                Infinity :
                                ( finalSegmentInfo.numAttachPorts > 1 ?
                                   PedigreeEditor.attributes.radius * (1.8 + finalSegmentInfo.numAttachPorts*0.1 - finalSegmentInfo.attachmentPort*0.35) :
                                   PedigreeEditor.attributes.radius * 1.6
                                );
            //console.log("Rel: " + id + ", Y: " + this.getY() + ", Attach/FinalY: " +finalYTo + ", yTOP: " + yTop + ", lastbend: " + lastBend + ", finalPos: " + stringifyObject(finalPosition));

            var goesLeft = false;                        // indicates if the current step fo the path is right-to-left or left-to-right            
            var xFrom    = this.getX();                  // always the X of the end of the previous segment of the curve
            var yFrom    = this.getY();                  // always the Y of the end of the previous segment of the curve
            var xTo      = xFrom;
            var yTo      = yFrom;
            var prevY    = yFrom;                        // y-coordinate of the previous node: used to determine vertical vs horizontal segments
            var prevX    = xFrom;
            var vertical = false;                        // direction of the previous segment
            var wasAngle = false;

            //console.log("Path: " + stringifyObject(path));

            for (var i = 0; i < path.length; i++) {
                var nextNodeOnPath = path[i];

                var nodePos  = editor.getGraph().getPosition(nextNodeOnPath);
                var position = editor.convertGraphCoordToCanvasCoord( nodePos.x, nodePos.y );
                //console.log("NextNode: " + nextNodeOnPath + ", nodePos: " + stringifyObject(nodePos) + ", position: " + stringifyObject(position) );
                                
                if (position.x < xFrom)   // depending on curve direction upper/lower curves of  adouble-line are shifted in different directions
                    goesLeft = true;
                else if (position.x > xFrom)
                    goesLeft = false;
                                                                                                         
                var newVertical = (prevY != position.y);                    
                                
                var angled = (prevX != position.x && prevY != position.y);
                                
                var changesDirection = ((vertical && !newVertical) || (!vertical && newVertical)) || angled;

                if (i == path.length-1 && prevY == yTop) {
                    angled = false;
                    changesDirection = (xFrom != xTo || yFrom != yTo);
                    newVertical = false;
                }                

                // if necessary, mark first segment on the left as broken
                if (i == 0 && goesLeft && this.getNode().getBrokenStatus()) {
                    editor.getView().drawLineWithCrossings(id, xFrom, yFrom, xFrom-16, yFrom, lineAttr, consangr, goesLeft);
                    editor.getPaper().path("M " + (xFrom-29) + " " + (yFrom+9) + " L " + (xFrom-15) + " " + (yFrom-9)).attr(lineAttr).toBack();
                    editor.getPaper().path("M " + (xFrom-24) + " " + (yFrom+9) + " L " + (xFrom-10) + " " + (yFrom-9)).attr(lineAttr).toBack();
                    xFrom -= 23;
                }                
                
                //console.log("angled: " + angled + ", changes: " + changesDirection);
                        
                if (changesDirection) {  // finish drawing the current segment
                    editor.getView().drawLineWithCrossings(id, xFrom, yFrom, xTo, yTo, lineAttr, consangr, goesLeft);
                    xFrom = xTo;
                    yFrom = yTo;
                }
                                
                xTo      = position.x;
                yTo      = (i >= path.length - 2) ? yTop : position.y;
                prevY    = position.y;                
                prevX    = position.x;
                                                
                //------------------
                // note: assume that we always draw bottom to top, as relationship nodes are always at or below partner level                
                
                if (smoothCorners && ( (!wasAngle && !angled) || (i >= path.length - 2 && path.length > 1)) ) {
                    //console.log("corner from " + xFrom + "," + yFrom + ", newVert: " + newVertical );
                    if (newVertical && !vertical) {
                        // was horizontal, now vertical - draw the smooth corner Horiz->Vert (curve bends down)
                        if (xTo < xFrom) {                            
                            drawCornerCurve( xFrom, yFrom, xFrom - cornerRadius, yFrom - cornerRadius, true, lineAttr, consangr, +2.5, -2.5, -2.5, +2.5 );
                            xFrom -= cornerRadius;
                            yFrom -= cornerRadius;
                        }
                        else {
                            drawCornerCurve( xFrom, yFrom, xFrom + cornerRadius, yFrom - cornerRadius, true, lineAttr, consangr, +2.5, 2.5, -2.5, -2.5 );
                            xFrom += cornerRadius;
                            yFrom -= cornerRadius;
                        }
                    } else if (!newVertical && vertical) {
                        // was vertical, now vertical - draw the smooth corner Vert->Horiz (curve bends up)
                        if (xTo < xFrom) {
                            drawCornerCurve( xFrom, yFrom, xFrom - cornerRadius, yFrom - cornerRadius, false, lineAttr, consangr, -2.5, 2.5, 2.5, -2.5 );
                            xFrom -= cornerRadius;
                            yFrom -= cornerRadius;
                        }
                        else {
                            drawCornerCurve( xFrom, yFrom, xFrom + cornerRadius, yFrom - cornerRadius, false, lineAttr, consangr, 2.5, 2.5, -2.5, -2.5 );
                            xFrom += cornerRadius;
                            yFrom -= cornerRadius;                            
                        }                        
                    } else if (!newVertical)                    
                    {   
                        // horizontal: stop the line a bit earlier so that we can draw a smooth corner
                        if (i != path.length-1) {
                            if (position.x > xFrom)
                                xTo -= cornerRadius;             // going right: stop a bit to the right
                            else
                                xTo += cornerRadius;             // going left: stop a bit to the left
                        }
                    } else {
                        // vertical: stop the line a bit earlier so that we can draw a smooth corner
                        yTo += cornerRadius;                     // always going up, so stop a bit below
                    }
                }
                //------------------
                                
                vertical = newVertical;
                wasAngle = angled;
            }
            
            if (yFrom >= finalPosition.y + cornerRadius*2) {
                editor.getView().drawLineWithCrossings(id, xFrom, yFrom, xTo, finalYTo, lineAttr, consangr, false);
            }
            else {
                // draw a line/curve from (xFrom, yFrom) trough (..., yTop) to (xTo, yTo).
                // It may be a line if all y are the same, a line with one bend or a line with two bends
                editor.getView().drawCurvedLineWithCrossings( id, xFrom, yFrom, yTop, xTo, finalYTo, lastBend, lineAttr, consangr, goesLeft );
            }

            var lostContact = !editor.getView().getNode(person).isProband() &&
                              editor.getView().getNode(person).getLostContact() &&
                              editor.getGraph().isPartnershipRelatedToProband(id);
            if (lostContact) {
                var xCross = goesLeft ? this.getX() - 20 : this.getX() + 20;
                var lineSize = PedigreeEditor.attributes.notInContactLineSize;
                editor.getPaper().path("M " + (xCross) + " " + (this.getY() - lineSize) + " L " + (xCross) + " " + (this.getY() + lineSize)).attr(PedigreeEditor.attributes.noContactLines).toBack();
            }
        }
        
        this._partnerConnections = editor.getPaper().setFinish().toBack(); 
        if (this.getNode().getGraphics()) {
            this.getHoverBox().regenerateHandles();
            this.getHoverBox().regenerateButtons();
        }
    },

    /**
     * Updates the path of the connection for the given pregnancy or creates a new
     * connection if it doesn't exist.
     *
     * @method updateChildhubConnection
     */
    updateChildhubConnection: function() {
        this._childhubConnection && this._childhubConnection.remove();
        
        var twinCommonVerticalPieceLength = PedigreeEditor.attributes.twinCommonVerticalLength;        
        
        var positionedGraph = editor.getGraph();
        
        var id = this.getNode().getID();

        editor.getPaper().setStart();
        
        var childlinePos = positionedGraph.getRelationshipChildhubPosition(id);
        var childlineY   = editor.convertGraphCoordToCanvasCoord( childlinePos.x, childlinePos.y ).y;
                      
        // draw child edges from childhub
        var children = positionedGraph.getRelationshipChildrenSortedByOrder(id);

        var leftmostX  = this.getX();
        var rightmostX = this.getX();
        
        var currentTwinGroup        = null;
        var currentTwinGroupCenterX = null;
        var currentIsMonozygothic   = false;
                
        var numPregnancies = 0;
        
        for ( var j = 0; j < children.length; j++ ) {
            var child  = children[j];
            
            var twinGroupId = positionedGraph.getTwinGroupId(child);
            
            if (twinGroupId != currentTwinGroup) {
                numPregnancies++;
                
                currentTwinGroup = twinGroupId;
                
                var allTwins  = positionedGraph.getAllTwinsSortedByOrder(child);
                var positionL = editor.getView().getNode(allTwins[0]).getX();
                var positionR = editor.getView().getNode(allTwins[allTwins.length-1]).getX();
                var positionY = editor.getView().getNode(allTwins[0]).getY();
                currentTwinGroupCenterX = (positionL + positionR)/2;
                if (allTwins.length == 3)
                    currentTwinGroupCenterX = editor.getView().getNode(allTwins[1]).getX();
                editor.getView().drawLineWithCrossings( id, currentTwinGroupCenterX, childlineY, currentTwinGroupCenterX, childlineY+twinCommonVerticalPieceLength, PedigreeEditor.attributes.partnershipLines);
                
                currentIsMonozygothic = editor.getView().getNode(allTwins[0]).getMonozygotic();
                
                // draw the monozygothinc line, if necessary
                if (currentIsMonozygothic) {
                    var twinlineY   = childlineY+PedigreeEditor.attributes.twinMonozygothicLineShiftY;
                    var xIntercept1 = findXInterceptGivenLineAndY( twinlineY, currentTwinGroupCenterX, childlineY+twinCommonVerticalPieceLength, positionL, positionY);
                    var xIntercept2 = findXInterceptGivenLineAndY( twinlineY, currentTwinGroupCenterX, childlineY+twinCommonVerticalPieceLength, positionR, positionY);
                    editor.getView().drawLineWithCrossings( id, xIntercept1, twinlineY, xIntercept2, twinlineY, PedigreeEditor.attributes.partnershipLines); 
                }
            }
            else if (twinGroupId == null) {
                numPregnancies++;
                currentIsMonozygothic = false;
            }
            
            var childX = editor.getView().getNode(child).getX();
            var childY = editor.getView().getNode(child).getY();
            
            var topLineX = (currentTwinGroup === null) ? childX     : currentTwinGroupCenterX;
            var topLineY = (currentTwinGroup === null) ? childlineY : childlineY + twinCommonVerticalPieceLength;
                
            if (topLineX > rightmostX)
                rightmostX = topLineX;
            if (topLineX < leftmostX)
                leftmostX = topLineX;

            // draw regular child line - for all nodes which are not monozygothic twins and for the
            // rightmost and leftmost monozygothic twin
            if (!currentIsMonozygothic || childX == positionL || childX == positionR ) { 
                editor.getView().drawLineWithCrossings( id, topLineX, topLineY, childX, childY, PedigreeEditor.attributes.partnershipLines);
            }
            else {                
                var xIntercept = findXInterceptGivenLineAndY( twinlineY, currentTwinGroupCenterX, childlineY+twinCommonVerticalPieceLength, childX, childY);
                editor.getView().drawLineWithCrossings( id, xIntercept, twinlineY, childX, childY, PedigreeEditor.attributes.partnershipLines);
            }

            var lostContact = editor.getGraph().isChildOfProband(child) && editor.getView().getNode(child).getLostContact();
            if (lostContact) {
                if (twinGroupId == null) {
                    var lineSize = PedigreeEditor.attributes.notInContactLineSize;
                    editor.getPaper().path("M " + (topLineX - lineSize) + " " + (topLineY + 20) + " L " + (topLineX + lineSize) + " " + (topLineY + 20)).attr(PedigreeEditor.attributes.noContactLines).toBack();
                } else {
                }
            }
        }

        editor.getView().drawLineWithCrossings( id, leftmostX, childlineY, rightmostX, childlineY, PedigreeEditor.attributes.partnershipLines);        
        editor.getView().drawLineWithCrossings( id, this.getX(), this.getY(), this.getX(), childlineY, PedigreeEditor.attributes.partnershipLines);
        
        //draw small non-functional childhub junction orb
        if (numPregnancies > 1)
            editor.getPaper().circle(this.getX(), childlineY, PedigreeEditor.attributes.partnershipRadius/2).attr({fill: '#666666', stroke: '#888888', 'stroke-width':1, 'opacity': 1});
        
        this._childhubConnection = editor.getPaper().setFinish();        
    },   
    
    /**
     * Changes the position of the junction to the coordinate (x,y) and updates all surrounding connections.
     *
     * @method setPos
     * @param {Number} x X coordinate relative to the Raphael canvas
     * @param {Number} y Y coordinate relative to the Raphael canvas
     * @param {Boolean} animate Set to True to animate the transition
     * @param {Function} callback Executed at the end of the animation
     */
    setPos: function($super, x, y, animate, callback) {
        
        this.getHoverBox().removeHandles();
        this.getHoverBox().removeButtons();
        
        if(animate) {
            throw "Can't animate a partnership node";
        }
        
        this.mark && this.mark.remove();
        this.mark2 && this.mark2.remove();

        this.getAllGraphics().transform("t " + (x-this.getX()) + "," + (y-this.getY()) + "...");
        $super(x,y, animate, callback);
                
        this.updatePartnerConnections();
        this.updateChildhubConnection();
        this.updateChildlessStatusLabel();
    },

    /**
     * Removes all the graphical elements of this partnership from the canvas
     *
     * @method remove
     */
    remove: function() {
        this.getJunctionShape().remove();
        this.getHoverBox().remove();
        this._idLabel && this._idLabel.remove();
        this.getChildlessShape() && this.getChildlessShape().remove();
        this.getChildlessStatusLabel() && this.getChildlessStatusLabel().remove();
        this._childhubConnection && this._childhubConnection.remove();
        this._partnerConnections && this._partnerConnections.remove();
        this.area && this.area.remove();
        this.mark && this.mark.remove();
        this.mark2 && this.mark2.remove();
    },

    /**
     * Returns a Raphael set of graphic elements of which the icon of the Partnership consists. Does not
     * include hoverbox elements and labels.
     *
     * @method getShapes
     * @return {Raphael.st}
     */
    getShapes: function($super) {
        return $super().push(this.getJunctionShape());
    },

    /**
     * Returns a Raphael set of all the graphics and labels associated with this Partnership. Includes the hoverbox
     * elements and labels
     *
     * @method getAllGraphics
     * @return {Raphael.st}
     */
    getAllGraphics: function($super) {
        return editor.getPaper().set(this.getHoverBox().getBackElements(), this._idLabel, this._childlessShape).concat($super()).push(this.getHoverBox().getFrontElements());
    },
    
    /**
     * Displays all the appropriate labels for this Partnership in the correct layering order
     *
     * @method drawLabels
     */
    drawLabels: function() {
        // if need to add some - see PersonVisuals.drawLabels()
    }  
});

//ATTACH CHILDLESS BEHAVIOR METHODS TO PARTNERSHIP
PartnershipVisuals.addMethods(ChildlessBehaviorVisuals);