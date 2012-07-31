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
        this._genderShape = null;
        this.draw();
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
        return this._relativeX;
    },

    /*
     * Replaces the current X coordinate of this node on the canvas, taking into consideration transformation data.
     */
    setAbsX: function(x) {
        this._relativeX = x;
    },

    /*
     * Returns the y coordinate at which the node was originally drawn. Disregards transformation data.
     */
    getAbsY: function() {
        return this._relativeY;
    },

    /*
     * Replaces the current Y coordinate of this node on the canvas, taking into consideration transformation data.
     */
    setAbsY: function(y) {
        this._relativeY = y;
    },

    /*
     * Returns the Raphael element representing the gender of the node.
     */
    getGenderShape: function() {
        return this._genderShape;
    },

    /*
     * Removes the current Raphael element or set representing the gender of the node, and
     * replaces it with the one passed in the parameter.
     *
     * @param: shape a Raphael element or set
     */
    setGenderShape: function(shape) {
        this.getGenderShape().remove();
        this._genderShape = shape;
    },

    /*
     * Returns a Raphael set containing a shape that represents the gender of the node and a
     * shadow behind it
     */
    updateGenderShape: function() {
        this._genderShape && this._genderShape.remove();
        var radius = editor.graphics.getRadius(),
            shape,
            x = this.getX(),
            y = this.getY();
        if (this.getNode().getGender() == 'M') {
            shape = editor.paper.rect(x - radius, y - radius, radius * 2, radius * 2, 2);
        }
        else if (this.getNode().getGender() == 'F') {
            shape = editor.paper.circle(x, y, radius);
        }
        else {
            shape = editor.paper.rect(x - radius * (Math.sqrt(3)/2), y - radius * (Math.sqrt(3)/2),
                radius * Math.sqrt(3), radius * Math.sqrt(3));
            shape.attr(editor.graphics._attributes.nodeShape);

        }
        shape.attr(editor.graphics._attributes.nodeShape);
        shape = (this.getNode().getGender() == 'U') ? shape.transform("...r45") : shape;

        //TODO: figure out whether glow is ok on all browsers
        var shadow =// shape.clone().attr({"fill": "gray", "opacity":.2, stroke: 'none', 'x': shape.attr("x") + 4, 'y': shape.attr("y") +4});
                shape.glow({width: 5, fill: true, opacity: 0.1}).translate(3,3);
        this._genderShape = editor.paper.set(shadow, shape);
    },

    /*
     * Returns a Raphael set or element that contains the graphics associated with this node, excluding the labels.
     */
    getShapes: function() {
        return editor.paper.set(this.getGenderShape());
    },

    /*
     * Returns a Raphael set or element that contains all the graphics and labels associated with this node.
     */
    getAllGraphics: function() {
        return this.getShapes();
    },

    /*
     * Updates the graphical elements of this node excluding labels, and brings them to front in the correct
     * layering order.
     */
    drawShapes: function() {
        this.updateGenderShape();
        this.getShapes().toFront();
    },

    /*
     * Updates the graphical elements of this node including labels, and brings them to front in the correct
     * layering order.
     */
    draw: function() {
        this.drawShapes();
    },

    move: function(x, y) {
        this.getAllGraphics().stop().animate({'transform': "t " + (x-this.getAbsX()) + "," +(y-this.getAbsY()) + "..."}, 2000, "linear");
        this.setAbsX(x);
        this.setAbsY(y);
    },

    /*
     * Removes all the graphical elements of this node from the canvas
     */
    remove: function() {
        this.getAllGraphics().remove();
    }
});