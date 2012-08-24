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
        this._radius = editor.attributes.radius;
        $super(node, x, y);
        this._width = editor.attributes.radius * 4;
        this.setGenderSymbol();
        this._highlightBox = editor.getPaper().rect(this.getX()-(this._width/2), this.getY()-(this._width/2),
            this._width, this._width, 5).attr(editor.attributes.boxOnHover);
        this._highlightBox.attr({fill: 'black', opacity: 0, 'fill-opacity': 0});
        this._highlightBox.insertBefore(this.getGenderSymbol().flatten());
        this._idLabel = editor.getPaper().text(x, y,  editor.DEBUG_MODE ? node.getID() : "").attr(editor.attributes.dragMeLabel).insertAfter(this._genderSymbol.flatten());
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
     * Changes the position of the node to (X,Y)
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param animate set to true if you want to animate the transition
     * @param callback the function called at the end of the animation
     */
    setPos: function(x, y, animate, callback) {
        var me = this;
        this.getNode().getPartnerships().each(function(partnership) {
            partnership.getGraphics().updatePartnerConnection(me.getNode(), x, y, partnership.getX(), partnership.getY(),  animate);
        });
        var p = this.getNode().getParentPartnership();
        p && p.getGraphics().updateChildConnection(this.getNode(), x, y, p.getX(), p.getY(), animate);

        if(animate){
            this.getAllGraphics().stop().animate({'transform': "t " + (x-this.getX()) + "," +(y-this.getY()) + "..."},
                1000, "easeInOut", function() { me.updatePositionData(x, y);callback && callback();});
        }
        else {
            this.getAllGraphics().transform("t " + (x-this.getX()) + "," +(y-this.getY()) + "...");
            me.updatePositionData(x, y);
        }
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
        var shape,
            x = this.getX(),
            y = this.getY(),
            radius = this._radius = this._radius = (this.getNode().getGender() == 'U') ? editor.attributes.radius * (Math.sqrt(3)/2) : editor.attributes.radius;

        if (this.getNode().getGender() == 'F') {
            shape = editor.getPaper().circle(x, y, editor.attributes.radius);
        }
        else {
            shape = editor.getPaper().rect(x - radius, y - radius, radius * 2, radius * 2);
        }
        shape.attr(editor.attributes.nodeShape);
        shape = (this.getNode().getGender() == 'U') ? shape.transform("...r45") : shape;
        this._genderShape = shape;

        //var shadow = shape.glow({width: 5, fill: true, opacity: 0.1}).translate(3,3);
        var shadow = shape.clone().attr({stroke: 'none', fill: 'gray', opacity: .3});
        shadow.translate(3,3);
        shadow.insertBefore(shape);
        this.getGenderSymbol() && this.getGenderSymbol().remove();
        this._genderSymbol = editor.getPaper().set(shadow, shape);

        var p = this.getNode().getParentPartnership();
        p && p.getGraphics().updateChildConnection(this.getNode(), this.getX(), this.getY(), p.getX(), p.getY());

        var me = this;
        this.getNode().getPartnerships().each(function(partnership) {
            partnership.getGraphics().updatePartnerConnection(me.getNode(), me.getX(), me.getY(), partnership.getX(), partnership.getY());
        })
    },

    /*
     * Returns the distance from the center of the genderSymbol to the rightmost point of the shape.
     */
    getRadius: function() {
        return this._radius;
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
    getShapes: function($super) {
        return $super().push(this.getGenderSymbol());
    },

    /*
     * Returns a Raphael set or element that contains all the graphics and labels associated with this node.
     */
    getAllGraphics: function($super) {
        return editor.getPaper().set(this.getHighlightBox(), this._idLabel).concat($super());
    }
});