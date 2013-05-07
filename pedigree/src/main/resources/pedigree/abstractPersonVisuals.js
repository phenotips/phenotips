/**
 * An abstract superclass for the a graphic engine used by nodes on the Pedigree graph. Can display
 * a shape representing the gender of the attached node.
 *
 * @class AbstractPersonVisuals
 * @extends AbstractNodeVisuals
 * @constructor
 * @param {AbstractPerson} node The node for which this graphics are handled
 * @param {Number} x The x coordinate on the canvas
 * @param {Number} y the y coordinate on the canvas
 */

var AbstractPersonVisuals = Class.create(AbstractNodeVisuals, {

    initialize: function($super, node, x, y) {
        this._icon = null;
        this._radius = PedigreeEditor.attributes.radius;
        $super(node, x, y);
        this._width = PedigreeEditor.attributes.radius * 4;
        this._adoptedShape = null;
        this.setIcon();
        this._highlightBox = editor.getPaper().rect(this.getX()-(this._width/2), this.getY()-(this._width/2),
            this._width, this._width, 5).attr(PedigreeEditor.attributes.boxOnHover);
        this._highlightBox.attr({fill: 'black', opacity: 0, 'fill-opacity': 0});
        this._highlightBox.insertBefore(this.getIcon().flatten());
        this._idLabel = editor.getPaper().text(x, y,  editor.DEBUG_MODE ? node.getID() : "").attr(PedigreeEditor.attributes.dragMeLabel).insertAfter(this._icon.flatten());
    },

    /**
     * Returns the Raphael element representing the gender of the node.
     *
     * @method getIcon
     * @return {Raphael.st | Raphael.el} Raphael element
     */
    getIcon: function() {
        return this._icon;
    },

    /**
     * Updates the icon representing the gender of the node
     *
     * @method setIcon
     * @param [$super]
     */
    setIcon: function($super) {
        this.setGenderSymbol();
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
        var me = this;
        this.getNode().getPartnerships().each(function(partnership) {
            partnership.getGraphics().updatePartnerConnection(me.getNode(), x, y, partnership.getX(), partnership.getY(), animate);
        });
        var p = this.getNode().getParentPregnancy();
        p && p.getGraphics().updateChildConnection(this.getNode(), x, y, p.getX(), p.getY(), animate);

        if(animate && (this.getX() != x || this.getY() != y)) {
            this.getAllGraphics().stop().animate({'transform': "t " + (x-this.getX()) + "," +(y-this.getY()) + "..."},
                1000, "easeInOut", function() { me._updatePositionData(x, y);callback && callback();});
        }
        else {
            this.getAllGraphics().transform("t " + (x-this.getX()) + "," +(y-this.getY()) + "...");
            me._updatePositionData(x, y);
        }
    },

    /**
     * Saves the x and y values as current coordinates and updates connections with the new position
     *
     * @method _updatePositionData
     * @param {Number} x The new x coordinate
     * @param {Number} y The new y coordinate
     * @private
     */
    _updatePositionData: function(x, y) {
        var me = this;
        me._absoluteX = x;
        me._absoluteY = y;
    },

    /**
     * Returns the distance from the center of the genderSymbol to the rightmost point of the shape.
     *
     * @method getRadius
     * @return {Number}
     */
    getRadius: function() {
        return this._radius;
    },

    /**
     * Returns the box around the element that appears when the node is highlighted
     *
     * @method getHighlightBox
     * @return {Raphael.rect} Raphael rectangle element
     */
    getHighlightBox: function() {
        return this._highlightBox;
    },

    /**
     * Displays the highlightBox around the node
     *
     * @method highlight
     */
    highlight: function() {
        this.getHighlightBox().attr({"opacity": .5, 'fill-opacity':.5});
    },

    /**
     * Draws brackets around the node icon to show that this node is adopted
     *
     * @method drawAdoptedShape
     */
    drawAdoptedShape: function() {
        this._adoptedShape && this._adoptedShape.remove();
        var r = PedigreeEditor.attributes.radius,
            x1 = this.getX() - ((0.8) * r),
            x2 = this.getX() + ((0.8) * r),
            y = this.getY() - ((1.3) * r),
            brackets = "M" + x1 + " " + y + "l" + r/(-2) +
                " " + 0 + "l0 " + (2.6 * r) + "l" + (r)/2 + " 0M" + x2 +
                " " + y + "l" + (r)/2 + " 0" + "l0 " + (2.6 * r) + "l" +
                (r)/(-2) + " 0";
        this._adoptedShape = editor.getPaper().path(brackets).attr("stroke-width", 3);
        this._adoptedShape.insertBefore(this.getGenderShape().flatten());
    },

    /**
     * Removes the brackets around the node icon that show that this node is adopted
     *
     * @method removeAdoptedShape
     */
    removeAdoptedShape: function() {
        this._adoptedShape && this._adoptedShape.remove();
    },

    /**
     * Returns the raphael element or set containing the adoption shape
     *
     * @method getAdoptedShape
     * @return {Raphael.el} Raphael Element
     */
    getAdoptedShape: function() {
        return this._adoptedShape;
    },

    /**
     * Hides the highlightBox around the node
     *
     * @method unHighlight
     */
    unHighlight: function() {
        this.getHighlightBox().attr({"opacity": 0, 'fill-opacity':0});
    },

    /**
     * Returns a Raphael set or element that contains the graphics associated with this node, excluding the labels.
     *
     * @method getShapes
     */
    getShapes: function($super) {
        var shapes = $super().push(this.getIcon());
        this.getAdoptedShape() && shapes.push(this.getAdoptedShape());
        return shapes;
    },

    /**
     * Returns a Raphael set that contains all the graphics and labels associated with this node.
     *
     * @method getAllGraphics
     * @return {Raphael.st}
     */
    getAllGraphics: function($super) {
        return editor.getPaper().set(this.getHighlightBox(), this._idLabel).concat($super());
    },

    /**
     * Returns the Raphael element representing the gender of the node.
     *
     * @method getGenderSymbol
     * @return {Raphael.st|Raphael.el} Raphael set or Raphael element
     */
    getGenderSymbol: function() {
        return this._genderSymbol;
    },

    /**
     * Returns only the shape element from the genderSymbol
     *
     * @method getGenderShape
     * @return {Raphael.st|Raphael.el}
     */
    getGenderShape: function() {
        return this._genderShape;
    },

    /**
     * Sets/replaces the gender symbol with the symbol appropriate for the gender.s
     *
     * @method setGenderSymbol
     */
    setGenderSymbol: function() {
        this._genderSymbol && this._genderSymbol.remove();
        var shape,
            x = this.getX(),
            y = this.getY(),
            radius = this._radius = (this.getNode().getGender() == 'U') ? PedigreeEditor.attributes.radius * (Math.sqrt(3)/2) : PedigreeEditor.attributes.radius;

        if (this.getNode().getGender() == 'F') {
            shape = editor.getPaper().circle(x, y, PedigreeEditor.attributes.radius);
        }
        else {
            shape = editor.getPaper().rect(x - radius, y - radius, radius * 2, radius * 2);
        }
        shape.attr(PedigreeEditor.attributes.nodeShape);
        shape = (this.getNode().getGender() == 'U') ? shape.transform("...r45") : shape;
        this._genderShape = shape;

        //var shadow = shape.glow({width: 5, fill: true, opacity: 0.1}).translate(3,3);
        var shadow = shape.clone().attr({stroke: 'none', fill: 'gray', opacity: .3});
        shadow.translate(3,3);
        shadow.insertBefore(shape);
        this.getGenderSymbol() && this.getGenderSymbol().remove();
        this._genderSymbol = editor.getPaper().set(shadow, shape);

        var p = this.getNode().getParentPregnancy();
        p && p.getGraphics().updateChildConnection(this.getNode(), this.getX(), this.getY(), p.getX(), p.getY());

        var me = this;
        this.getNode().getPartnerships().each(function(partnership) {
            partnership.getGraphics().updatePartnerConnection(me.getNode(), me.getX(), me.getY(), partnership.getX(), partnership.getY());
        });
        this._icon = this._genderSymbol;
    }
});