/*
 * A general superclass for the a graphic engine used by nodes on the Pedigree graph. Can display
 * a shape representing the gender of the attached node.
 */

var AbstractPersonVisuals = Class.create(AbstractNodeVisuals, {

    initialize: function($super, node, x, y) {
        this._genderShape = null;
        this._width = editor.graphics.getRadius() * 4;
        $super(node, x, y);
        this._highlightBox = editor.paper.rect(this.getX()-(this._width/2), this.getY()-(this._width/2),
            this._width, this._width, 5).attr(editor.graphics._attributes.boxOnHover);
        this._highlightBox.attr({fill: 'black', opacity: 0, 'fill-opacity': 0});
        this.draw();
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
        this.getGenderShape() && this.getGenderShape().remove();
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

        var shadow = shape.glow({width: 5, fill: true, opacity: 0.1}).translate(3,3);
        this.setGenderShape(editor.paper.set(shadow, shape));
    },

    getHighlightBox: function() {
        return this._highlightBox;
    },

    highlight: function() {
        this.getHighlightBox().attr({"opacity": .5, 'fill-opacity':.5});
    },

    unHighlight: function() {
        this.getHighlightBox().attr({"opacity": 0, 'fill-opacity':0});
    },

    /*
     * Returns a Raphael set or element that contains the graphics associated with this node, excluding the labels.
     */
    getShapes: function() {
        return editor.paper.set(this.getHighlightBox(), this.getGenderShape());
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
    }
});