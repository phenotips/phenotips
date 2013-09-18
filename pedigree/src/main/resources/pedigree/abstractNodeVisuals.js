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
    },    

    /**
     * Shrinks node graphics to the original size
     *
     * @method shrink
     */
    shrink: function() {
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
        // TODO:  && this._childlessShape = null ?
        
        if(status) {
            console.log("childless shape!");
            
	        var x    = this.getX();
	        var y    = this.getY();
	        var r    = PedigreeEditor.attributes.partnershipRadius;
	        var lowY = this.getBottomY();
	        
	        var childlessPath = [["M", x, y],["L", x, lowY],["M", x - r, lowY], ["l", 2 * r, 0]];
	        if(status == 'infertile')
	            childlessPath.push(["M", x - r, lowY + 4], ["l", 2 * r, 0]);

            this._childlessShape = editor.getPaper().path(childlessPath);
            this._childlessShape.attr({"stroke-width": 2.5, stroke: "#3C3C3C"});
            this._childlessShape.insertAfter(this.getHoverBox().getBackElements().flatten());
            this.getHoverBox().hideChildHandle();
        }
        else {
           	this.getHoverBox().unhideChildHandle();
        }
    },

    /**
     * Updates the childless status reason label for this Person
     *
     * @method updateChildlessStatusLabel
     */
    updateChildlessStatusLabel: function() {
        this._childlessStatusLabel && this._childlessStatusLabel.remove();
        // TODO: && this._childlessStatusLabel = null ??
        
        var text = "";
        this.getNode().getChildlessReason() && (text += this.getNode().getChildlessReason());
        
        if(text.strip() != '') {
            this._childlessStatusLabel = editor.getPaper().text(this.getX(), this.getY() + PedigreeEditor.attributes.radius * 2, "(" + text.slice(0, 14) +")" );
            this._childlessStatusLabel.attr({'font-size': 18, 'font-family': 'Cambria'});
        }
        
        this._childlessStatusLabel && this._childlessStatusLabel.insertAfter(this.getChildlessShape().flatten());
        this.drawLabels();
    }
};