/**
 * AbstractNodeVisuals is the general abstract class for the graphic engine used by nodes on the Pedigree graph.
 * Objects of this class have information about the positioning of the graphical elements of the node.
 *
 * @class AbstractNodeVisuals
 * @constructor
 * @param {AbstractNode} node The node for which the graphics are drawn
 * @param {Number} x The x coordinate on the canvas
 * @param {Number} y The y coordinate on the canvas
 */

var AbstractNodeVisuals = Class.create({

    initialize: function(node, x, y) {
    	//console.log("abstract node visuals");
        this._node = node;
        this._absoluteX = x;
        this._absoluteY = y;
        this._hoverBox  = null;
        this._isGrown   = false;
        //console.log("abstract node visuals end");
    },

    /**
     * Returns the node for which the graphics are being drawn
     *
     * @method getNode
     * @return {AbstractNode}
     */
    getNode: function() {
        return this._node;
    },

    /**
     * Returns the current X coordinate of this node on the canvas, taking into consideration transformation data.
     *
     * @method getX
     * @return {Number} the x coordinate
     */
    getX: function() {
        return this._absoluteX;
    },
    
    /**
     * Updates whatever needs to change when node id changes (e.g. id label) 
     *
     * @method onSetID
     */    
    onSetID: function(id) {
    },

    /**
     * Returns the current Y coordinate of this node on the canvas, taking into consideration transformation data.
     *
     * @method getY
     * @return {Number} The y coordinate
     */
    getY: function() {
        return this._absoluteY;
    },
      
    /**
     * Returns the Y coordinate of the lowest part of this node's graphic on the canvas
     *
     * @method getY
     * @return {Number} The y coordinate
     */    
    getBottomY: function() {
    	return this._absoluteY;
    },

    /**
     * Changes the position of the node to (X,Y)
     *
     * @method setPos
     * @param {Number} x The x coordinate
     * @param {Number} y The y coordinate
     * @param {Boolean} animate Set to true if you want to animate the transition
     * @param {Function} callback The function called at the end of the animation
     */
    setPos: function(x, y, animate, callback) {
        //console.log("Node " + this.getNode().getID() + ", xy: " + x + "/" + y);
        this._absoluteX = x;
        this._absoluteY = y; 
        callback && callback();
    },
    
    /**
     * Expands the node graphics a bit
     *
     * @method grow
     */
    grow: function() {
        this._isGrown = true;
    },    

    /**
     * Shrinks node graphics to the original size
     *
     * @method shrink
     */
    shrink: function() {
        this._isGrown = false;
    },

    /**
     * Returns current growth status of the node (true if grown, false if not)
     *
     * @method isGrown
     */    
    isGrown: function() { 
        return this._isGrown;
    },

    /**
     * Returns true if this node's graphic representation covers coordinates (x,y)
     *
     * @method containsXY
     */    
    containsXY: function(x,y) {
        return false;
    },

    /**
     * Returns true if this node is selected. A selected node is a node with visible Hoverbox.
     *
     * @method isSelected
     * @return {Boolean}
     */
    isSelected: function() {
        return this._isSelected;
    },

    /**
     * Sets this node's selected property to isSelected. A selected node is a node with visible Hoverbox.
     *
     * @method setSelected
     * @param {Boolean} isSelected True if the node is selected
     */
    setSelected: function(isSelected) {
        this._isSelected = isSelected;
    },

    /**
     * Returns a Raphael set of all the graphics and labels associated with this node.
     *
     * @method getAllGraphics
     * @return {Raphael.st}
     */
    getAllGraphics: function() {
        return editor.getPaper().set(this.getShapes());
    },

    /**
     * Returns a Raphael set of graphic elements of which the icon of the node consists. Does not
     * include hoverbox elements or labels.
     *
     * @method getShapes
     * @return {Raphael.st}
     */
    getShapes: function() {
        return editor.getPaper().set()
    },

    /**
     * Removes all the graphical elements of this node from the canvas
     *
     * @method remove
     */
    remove: function() {
        this.getHoverBox() && this.getHoverBox().remove();
        this.getAllGraphics().remove();
    },

    /**
     * Returns the hoverbox object for this node
     *
     * @method getHoverBox
     * @return {AbstractHoverbox}
     */
    getHoverBox: function() {
        return this._hoverBox;
    }
});


var ChildlessBehaviorVisuals = {

    /**
     * Returns the childless status shape for this Person
     *
     * @method getChildlessShape
     * @return {Raphael.el}
     */
    getChildlessShape: function() {
        return this._childlessShape;
    },

    /**
     * Returns the Raphaël element for this Person's childless status reason label
     *
     * @method getChildlessStatusLabel
     * @return {Raphael.el}
     */
    getChildlessStatusLabel: function() {
        return this._childlessStatusLabel;
    },
    
    /**
     * Updates the childless status icon for this Node based on the childless/infertility status.
     *
     * @method updateChildlessShapes
     */
    updateChildlessShapes: function() {
        var status = this.getNode().getChildlessStatus();
        this._childlessShape && this._childlessShape.remove();
        
        if(status) {
	        var x    = this.getX();
	        var y    = this.getY();
	        var r    = PedigreeEditor.attributes.infertileMarkerWidth;
	        var lowY = this.getBottomY() + PedigreeEditor.attributes.infertileMarkerHeight;
	        
	        var childlessPath = [["M", x, y],["L", x, lowY],["M", x - r, lowY], ["l", 2 * r, 0]];
	        if(status == 'infertile')
	            childlessPath.push(["M", x - r, lowY + 5], ["l", 2 * r, 0]);

	        var strokeWidth = 2.5; //editor.getWorkspace().getSizeNormalizedToDefaultZoom(2);
            this._childlessShape = editor.getPaper().path(childlessPath);
            this._childlessShape.attr({"stroke-width": strokeWidth, stroke: "#3C3C3C"});
            this._childlessShape.toBack();            
        }
    },

    /**
     * Updates the childless status reason label for this Person
     *
     * @method updateChildlessStatusLabel
     */
    updateChildlessStatusLabel: function() {
        this._childlessStatusLabel && this._childlessStatusLabel.remove();
        this._childlessStatusLabel = null;
        
        var text = "";
        this.getNode().getChildlessReason() && (text += this.getNode().getChildlessReason());
        
        if(text.strip() != '') {
            this._childlessStatusLabel = editor.getPaper().text(this.getX(), this.getBottomY() + 18, "(" + text.slice(0, 15) +")" );
            this._childlessStatusLabel.attr({'font-size': 18, 'font-family': 'Cambria'});
            this._childlessStatusLabel.toBack();
        }        
        
        this.drawLabels();
    }
};