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
        this._junctionShape = editor.getPaper().circle(x,y, PedigreeEditor.attributes.partnershipRadius).attr({fill: '#EA5E48', stroke: 'black', 'stroke-width':2});

        this._hoverBox = new PartnershipHoverbox(partnership, x, y, this.getShapes());
        this._idLabel = editor.getPaper().text(x, y-20, editor.DEBUG_MODE ? partnership.getID() : "").attr(PedigreeEditor.attributes.dragMeLabel).insertAfter(this._junctionShape.flatten());
        
        this._childhubConnection = null;
        this._partnerConnections = {};    // {partnerID} : {Raphael set of graphic objects}
        
        this.updatePartnerConnections();
        this.updateChildhubConnection();
        //console.log("partnership visuals end");
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
     * Marks the node in  away different from glow
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
        return this._absoluteY + PedigreeEditor.attributes.partnershipRadius * 2;
    },    

    /**
     * Updates the path of all connections to all partners
     *
     * @method updatePartnerConnections
     */
    updatePartnerConnections: function() {
        for (var partner in this._partnerConnections)
            if (this._partnerConnections.hasOwnProperty(partner))
                this._partnerConnections[partner].remove();
        this._partnerConnections = {};
        
        var positionedGraph = editor.getGraph();
        
        var partnerPaths = positionedGraph.getPathToParents( this.getNode().getID() );        
        // partnerPaths = [ [virtual_node_1, virtual_node_2, ..., parent1] [virtual_node_1, virtual_node_2, ..., parent2] ]
        
        //console.log(stringifyObject(partnerPaths));        
        // TODO: smooth curves
        
        var path1    = partnerPaths[0];
        var partner1 = path1[path1.length-1];        
        var raphaelPath = [["M", this.getX(), this.getY()]];        
        for (var i = 0; i < path1.length; i++) {
            var nextNodeOnPath = path1[i];
            var nodePos  = editor.getGraph().getPosition(nextNodeOnPath);
            var position = editor.convertGraphCoordToCanvasCoord( nodePos.x, nodePos.y );
            raphaelPath.push(["L", position.x, position.y]); 
        }               
        this._partnerConnections[partner1] = editor.getPaper().path(raphaelPath).attr(PedigreeEditor.attributes.partnershipLines).toBack()

        var path2    = partnerPaths[1];
        var partner2 = path2[path2.length-1];        
        var raphaelPath = [["M", this.getX(), this.getY()]];        
        for (var i = 0; i < path2.length; i++) {
            var nextNodeOnPath = path2[i];
            var nodePos  = editor.getGraph().getPosition(nextNodeOnPath);
            var position = editor.convertGraphCoordToCanvasCoord( nodePos.x, nodePos.y );
            raphaelPath.push(["L", position.x, position.y]); 
        }               
        this._partnerConnections[partner2] = editor.getPaper().path(raphaelPath).attr(PedigreeEditor.attributes.partnershipLines).toBack()        
    },

    /**
     * Updates the path of the connection for the given pregnancy or creates a new
     * connection if it doesn't exist.
     *
     * @method updateChildhubConnection
     */
    updateChildhubConnection: function(preg, pregX, pregY, partnershipX, partnershipY, animate) {
        this._childhubConnection && this._childhubConnection.remove();
        
        var positionedGraph = editor.getGraph();
                
        editor.getPaper().setStart();
        
        var childlinePos = positionedGraph.getRelationshipChildhubPosition(this.getNode().getID());
        var childlineY   = editor.convertGraphCoordToCanvasCoord( childlinePos.x, childlinePos.y ).y;
                      
        // draw child edges from childhub
        var children = positionedGraph.getRelationshipChildren(this.getNode().getID());

        var leftmostX  = this.getX();
        var rightmostX = this.getX();
        for ( var j = 0; j < children.length; j++ ) {
            var child  = children[j];
            var childX = editor.getGraphicsSet().getNode(child).getX();
            var childY = editor.getGraphicsSet().getNode(child).getY();
            if (childX > rightmostX)
                rightmostX = childX;
            if (childX < leftmostX)
                leftmostX = childX;            
            //console.log("childX: " + childX);

            this.drawVerticalLine(childX, childlineY, childY);
        }
        
        this.drawHorizontalLine(leftmostX, rightmostX, childlineY);
        this.drawVerticalLine(this.getX(), this.getY(), childlineY);
        
        //draw childhub junction orb
        //if (children.length > 1)
        editor.getPaper().circle(this.getX(), childlineY, PedigreeEditor.attributes.partnershipRadius/2).attr({fill: '#CCEECC', stroke: 'black', 'stroke-width':1, 'opacity': 0.5});
        
        this._childhubConnection = editor.getPaper().setFinish();        
    },

    drawVerticalLine: function(x, y1, y2) {
        // TODO: use editor.drawLine which will use LineSet and detect line crossings
        var line = editor.getPaper().path("M " + x + " " + y1 + " L " + x + " " + y2).attr(PedigreeEditor.attributes.partnershipLines).toBack();
        //line.node.setAttribute("shape-rendering","crispEdges");        
    },

    drawHorizontalLine: function(x1, x2, y) {
        // TODO: use editor.drawLine which will use LineSet and detect line crossings        
        var line = editor.getPaper().path("M " + x1 + " " + y + " L " + x2 + " " + y).attr(PedigreeEditor.attributes.partnershipLines).toBack();
        //line.node.setAttribute("shape-rendering","crispEdges");        
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
        if(animate) {
            throw "Can't animate a partnership node";
        }
        
        this.getAllGraphics().transform("t " + (x-this.getX()) + "," + (y-this.getY()) + "...");
        $super(x,y, animate, callback);
                
        this.updatePartnerConnections();
        this.updateChildhubConnection();        
    },

    /**
     * Removes all the graphical elements of this partnership from the canvas
     *
     * @method remove
     */
    remove: function() {
        this.getJunctionShape().remove();
        this.getHoverBox().remove();
        this._idLabel.remove();
        this.getChildlessShape() && this.getChildlessShape().remove();
        this.getChildlessStatusLabel() && this.getChildlessStatusLabel().remove();
        this._childhubConnection && this._childhubConnection.remove();
        for (var partner in this._partnerConnections)
            if (this._partnerConnections.hasOwnProperty(partner))
                this._partnerConnections[partner].remove();
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
        return editor.getPaper().set(this.getHoverBox().getBackElements(), this._idLabel).concat($super()).push(this.getHoverBox().getFrontElements());
    },
    
    /**
     * Displays all the appropriate labels for this Partnership in the correct layering order
     *
     * @method drawLabels
     */
    drawLabels: function() {
        // TODO
        /*
        var selectionOffset = (this.isSelected() && !this.getChildlessStatusLabel()) ? PedigreeEditor.attributes.radius/1.5 : 0;
        var childlessOffset = (this.getChildlessStatusLabel()) ? PedigreeEditor.attributes.radius/2 : 0;
        var startY = this.getY() + PedigreeEditor.attributes.radius * 1.7 + selectionOffset + childlessOffset;
        
        for (var i = 0; i < labels.length; i++) {
            labels[i].attr("y", startY + 11);
            labels[i].attr(PedigreeEditor.attributes.label);
            labels[i].oy = (labels[i].attr("y") - selectionOffset);
            startY = labels[i].getBBox().y2;
        }
        labels.flatten().insertBefore(this.getHoverBox().getFrontElements().flatten());
        */
    }  
});

//ATTACH CHILDLESS BEHAVIOR METHODS TO PARTNERSHIP
PartnershipVisuals.addMethods(ChildlessBehaviorVisuals);