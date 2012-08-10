/*
 * A general superclass for the a graphic engine used by nodes on the Pedigree graph. Can display
 * a shape representing the gender of the attached node.
 *
 * @param node the AbstractPerson object for which this graphics are handled
 * @param x the x coordinate on the canvas
 * @param x the y coordinate on the canvas
 */

var AbstractPersonVisuals = Class.create(AbstractNodeVisuals, {

    initialize: function($super, node, x, y) {
        this._genderSymbol = null;
        this._genderShape = null;
        this._width = editor.attributes.radius * 4;
        $super(node, x, y);
        this._highlightBox = editor.getPaper().rect(this.getRelativeX()-(this._width/2), this.getRelativeY()-(this._width/2),
            this._width, this._width, 5).attr(editor.attributes.boxOnHover);
        this._highlightBox.attr({fill: 'black', opacity: 0, 'fill-opacity': 0});
        this.draw();
    },

    /*
     * Returns the Raphael element representing the gender of the node.
     */
    getGenderSymbol: function() {
        return this._genderSymbol;
    },

    /*
     * Removes the current Raphael element or set representing the gender of the node, and
     * replaces it with the one passed in the parameter.
     *
     * @param: shape a Raphael element or set
     */

    /*
     * Transitions the node to the coordinate (x,y)
     *
     * @param x the x coordinate
     * @param y the y coordinate
     */
    setPos: function(x, y) {
        var me = this;
        this.getNode().getPartnerships().each(function(partnership) {
            partnership.getGraphics().translatePartnerConnection(me.getNode(), x, y);
        });
        this.getNode().getParentPartnership() && this.getNode().getParentPartnership().getGraphics().translateChildConnection(this.getNode(), x, y);
        this.getAllGraphics().stop().animate({'transform': "t " + (x-this.getX()) + "," +(y-this.getY()) + "..."},
            1000, "easeInOut", function() {me.updatePositionData(x, y)});
    },

    /*
     * [Helper for setPos] Saves the x and y values as current coordinates and updates connections with the new position
     *
     * @param x the new x coordinate
     * @param y the new y coordinate
     */
    updatePositionData: function(x, y) {
        var me = this;
        me._absoluteX = x;
        me._absoluteY = y;
        me.getNode().getPartnerships().each(function(partnership) {
            partnership.getGraphics().updatePartnerConnection(me.getNode());
        });
        me.getNode().getParentPartnership() && me.getNode().getParentPartnership().getGraphics().updateChildConnection(me.getNode());

    },

    /*
     * Returns only the shape element from the genderSymbol
     */
    getGenderShape: function() {
        return this._genderShape;
    },

    /*
     * Sets/replaces the gender symbol with the symbol appropriate for the gender. Returns raphael set containing
     * the genderShape and a shadow behind it.
     */
    setGenderSymbol: function() {
        this._genderSymbol && this._genderSymbol.remove();
        var radius = editor.attributes.radius,
            shape,
            x = this.getRelativeX(),
            y = this.getRelativeY();
        if (this.getNode().getGender() == 'M') {
            shape = editor.getPaper().rect(x - radius, y - radius, radius * 2, radius * 2, 2);
        }
        else if (this.getNode().getGender() == 'F') {
            shape = editor.getPaper().circle(x, y, radius);
        }
        else {
            shape = editor.getPaper().rect(x - radius * (Math.sqrt(3)/2), y - radius * (Math.sqrt(3)/2),
                radius * Math.sqrt(3), radius * Math.sqrt(3));
        }
        shape.attr(editor.attributes.nodeShape);
        shape = (this.getNode().getGender() == 'U') ? shape.transform("...r45") : shape;
        this._genderShape = shape;

        var shadow = shape.glow({width: 5, fill: true, opacity: 0.1}).translate(3,3);
        this.getGenderSymbol() && this.getGenderSymbol().remove();
        this._genderSymbol = editor.getPaper().set(shadow, shape);
    },

    /*
     * Returns the box around the element that appears when the node is highlighted
     */
    getHighlightBox: function() {
        return this._highlightBox;
    },

    /*
     * Displays the highlightBox around the node
     */
    highlight: function() {
        this.getHighlightBox().attr({"opacity": .5, 'fill-opacity':.5});
    },

    /*
     * Hides the highlightBox around the node
     */
    unHighlight: function() {
        this.getHighlightBox().attr({"opacity": 0, 'fill-opacity':0});
    },

    /*
     * Returns a Raphael set or element that contains the graphics associated with this node, excluding the labels.
     */
    getShapes: function() {
        return editor.getPaper().set(this.getHighlightBox(), this.getGenderSymbol());
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
        this.setGenderSymbol();
        this.getShapes().toFront();
    },

    /*
     * Updates the graphical elements of this node including labels, and brings them to front in the correct
     * layering order.
     */
    draw: function() {
        this.drawShapes();
    },

    /*
     * Returns and object containing the current y coordinate of the node and the leftmost x coordinate of
     * the genderShape
     */
    getLeftCoordinate: function() {
        return {x: this.getGenderShape().getBBox().x, y: this.getY()};
    },

    /*
     * Returns and object containing the current y coordinate of the node and the rightmost x coordinate of
     * the genderShape
     */
    getRightCoordinate: function() {
        return {x: this.getGenderShape().getBBox().x2, y: this.getY()};
    },

    /*
     * Returns and object containing the current x coordinate of the node and the top y coordinate of
     * the genderShape
     */
    getTopCoordinate: function() {
        return {x: this.getX(), y: this.getGenderShape().getBBox().y};
    },

    /*
     * Returns and object containing the current x coordinate of the node and the bottom y coordinate of
     * the genderShape
     */
    getBottomCoordinate: function() {
        return {x: this.getX(), y: this.getGenderShape().getBBox().y2};
    }

});