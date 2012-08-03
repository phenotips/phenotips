/*
 * A general superclass for the a graphic engine used by nodes on the Pedigree graph. Can display
 * a shape representing the gender of the attached node.
 */

var AbstractNodeVisuals = Class.create( {

    initialize: function(node, x, y) {
        this._node = node;
        this._relativeX= x;
        this._relativeY = y;
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
        return this._relativeX;
    },

    /*
     * Returns the y coordinate at which the node was originally drawn. Disregards transformation data.
     */
    getY: function() {
        return this._relativeY;
    },

    /*
     * Returns the current X coordinate of this node on the canvas, taking into consideration transformation data.
     */
    getAbsX: function() {
        return this._absoluteX;
    },

    /*
     * Replaces the current X coordinate of this node on the canvas, taking into consideration transformation data.
     */
    setAbsX: function(x) {
        this._absoluteX = x;
    },

    /*
     * Returns the y coordinate at which the node was originally drawn. Disregards transformation data.
     */
    getAbsY: function() {
        return this._absoluteY;
    },

    setAbsPos: function(x, y) {
        this.setAbsX(x);
        this.setAbsY(y);
    },
    /*
     * Replaces the current Y coordinate of this node on the canvas, taking into consideration transformation data.
     */
    setAbsY: function(y) {
        this._absoluteY = y;
    },

    /*
     * Returns a Raphael set or element that contains all the graphics and labels associated with this node.
     */
    getAllGraphics: function() {
        return editor.paper.set();
    },

    /*
     * Updates the graphical elements of this node including labels, and brings them to front in the correct
     * layering order.
     */
    draw: function() {
        this.getAllGraphics().toFront();
    },

    moveTo: function(x, y) {
        var xDisplacement = x - this.getAbsX();
        var yDisplacement = y - this.getAbsY();
        var me = this;
        var displacement = Math.sqrt(xDisplacement * xDisplacement + yDisplacement * yDisplacement);
        this.getAllGraphics().stop().animate({'transform': "t " + (x-this.getAbsX()) + "," + (y-this.getAbsY()) + "..."},
            displacement /.4, "<>", function() {me.setAbsPos(x,y);});
    },

    /*
     * Removes all the graphical elements of this node from the canvas
     */
    remove: function() {
        this.getAllGraphics().remove();
    }
});