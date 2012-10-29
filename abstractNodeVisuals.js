/*
 * A general superclass for the a graphic engine used by nodes on the Pedigree graph. Contains
 * information about positioning of the graphical elements of the node
 */

var AbstractNodeVisuals = Class.create({

    initialize: function(node, x, y) {
        this._node = node;
        this._absoluteX = x;
        this._absoluteY = y;
    },

    /*
     * Returns the node for which the graphics are being drawn
     */
    getNode: function() {
        return this._node;
    },

    /*
     * Returns the current X coordinate of this node on the canvas, taking into consideration transformation data.
     */
    getX: function() {
        return this._absoluteX;
    },

    /*
     * Changes the X coordinate of the node
     *
     * @param x the x coordinate on the canvas
     * @param animate set to true if you want to animate the transition
     * @param callback the function called at the end of the animation
     */
    setX: function(x, animate, callback) {
        this.setPos(x, this.getY(), animate, callback);
    },

    /*
     * Returns the current Y coordinate of this node on the canvas, taking into consideration transformation data.
     */
    getY: function() {
        return this._absoluteY;
    },

    /*
     * Changes the Y coordinate of the node
     *
     * @param y the y coordinate on the canvas
     * @param animate set to true if you want to animate the transition
     * @param callback the function called at the end of the animation
     */
    setY: function(y, animate, callback) {
        this.setPos(y, this.getX(), animate, callback);
    },

    /*
     * Returns an array containing the x and y coordinates of the node on canvas.
     */
    getPos: function() {
        return [this.getX(), this.getY()];
    },

    /*
     * [Abstract Method]
     * Changes the position of the node to (X,Y)
     *
     * @param x the x coordinate on the canvas
     * @param y the y coordinate on the canvas
     * @param animate set to true if you want to animate the transition
     * @param callback the function called at the end of the animation
     */
    setPos: function(x, y, animate, callback) {
    },

    /*
     * Determines whether this node is selected
     */
    setSelected: function(isSelected) {
        this._isSelected = isSelected;
    },

    /*
     * Returns a Raphael set of all the graphics and labels associated with this node.
     */
    getAllGraphics: function() {
        return editor.getPaper().set(this.getShapes());
    },

    /*
     * Returns a Raphael set of graphic elements of which the icon of the node consists. Does not
     * include hoverbox elements or labels.
     */
    getShapes: function() {
        return editor.getPaper().set()
    },

    /*
     * Removes all the graphical elements of this node from the canvas
     */
    remove: function() {
        this.getHoverBox() && this.getHoverBox().remove();
        this.getAllGraphics().remove();
    },

    /*
     * Returns the hoverbox object for this node
     */
    getHoverBox: function() {
        return this._hoverBox;
    }
});

var ChildlessBehaviorVisuals = {

    /*
     * Returns the childless status shape for this Person
     */
    getChildlessShape: function() {
        return this._childlessShape;
    },

    /*
     * Updates the childless status icon for this Person based on the childless/infertility status.
     */
    updateChildlessShapes: function() {
        var status = this.getNode().getChildlessStatus();
        this.getChildlessShape() && this.getChildlessShape().remove();

        var x = this.getX(),
            y = this.getY(),
            r = editor.attributes.radius,
            lowY = 1.6 * r + y;

        var childlessPath = [["M", x, y],["L", x, lowY],["M", x - r, lowY], ["l", 2 * r, 0]];
        if(status == 'infertile') {
            childlessPath.push(["M", x - r, lowY + 4], ["l", 2 * r, 0]);
        }

        if(status) {
            this._childlessShape = editor.getPaper().path(childlessPath);
            this._childlessShape.attr({"stroke-width": 2.5, stroke: "#3C3C3C"});
            this._childlessShape.insertAfter(this.getHoverBox().getBackElements().flatten());
            this.getHoverBox().hideChildHandle()
        }
        else {
            this._childlessShape && this._childlessShape.remove();
            this.getHoverBox().unhideChildHandle()
        }
    },

    /*
     * Updates the childless status reason label for this Person
     */
    updateChildlessStatusLabel: function() {
        this._childlessStatusLabel && this._childlessStatusLabel.remove();
        var text =  "";
        this.getNode().getChildlessReason() && (text += this.getNode().getChildlessReason());
        if(text.strip() != '') {
            this._childlessStatusLabel = editor.getPaper().text(this.getX(), this.getY() + editor.attributes.radius * 2, "(" + text.slice(0, 14) +")" );
            this._childlessStatusLabel.attr({'font-size': 18, 'font-family': 'Cambria'});
        }
        else {
            this._childlessStatusLabel = null;
        }
        this._childlessStatusLabel && this._childlessStatusLabel.insertAfter(this.getChildlessShape().flatten());
        this.getNode().getType() == "Person" && this.drawLabels();
    },

    /*
     * Returns the Raphaël element for this Person's childless status reason label
     */
    getChildlessStatusLabel: function() {
        return this._childlessStatusLabel;
    }
};