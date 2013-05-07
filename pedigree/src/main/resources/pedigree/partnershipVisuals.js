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
        $super(partnership, x,y);
        this._childlessShape = null;
        this._childlessStatusLabel = null;
        this._junctionShape = editor.getPaper().circle(x,y, PedigreeEditor.attributes.partnershipRadius).attr({fill: '#EA5E48', stroke: 'black', 'stroke-width':2});
        this._junctionShape.insertBefore(editor.getGraph().getProband().getGraphics().getAllGraphics().flatten());

        //TODO: find out whether there is an arc
        this._connections = [null, null];
        var me = this;
        me.getNode().getPartners().each(function(partner) {
            me.updatePartnerConnection(partner, partner.getX(), partner.getY(), x, y);
        });
        this._hoverBox = new PartnershipHoverbox(partnership, x, y, this.getShapes());
        this.area = null;
        this._idLabel = editor.getPaper().text(x, y-20, editor.DEBUG_MODE ? partnership.getID() : "").attr(PedigreeEditor.attributes.dragMeLabel).insertAfter(this._junctionShape.flatten());
    },

    /**
     * Expands the partnership circle
     *
     * @method grow
     */
    grow: function() {
        this.area = this.getJunctionShape().clone().flatten().insertBefore(this.getJunctionShape().flatten());
        this.area.attr({'fill': '#587498', stroke: 'none'});
        this.area.ot = this.area.transform();
        this.area.animate(Raphael.animation({transform : "...S2"}, 400, 'bounce'));
    },

    /**
     * Returns the Partnership for which the graphics are drawn
     *
     * @method getPartnership
     * @return {Partnership}
     */
    getPartnership: function() {
        return this._node;
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
     * Returns an array containing the two partner connections
     *
     * @method getConnections
     *
     * @return {Array}
     */
    getConnections: function() {
        return this._connections;
    },

    /**
     * Updates the path of the connection for the given partner or creates a new
     * connection if it doesn't exist.
     *
     * @method updatePartnerConnection
     * @param {AbstractPerson} partner A partner in this Partnership
     * @param {Number} partnerX X coordinate of the partner
     * @param {Number} partnerY Y coordinate of the partner
     * @param {Number} junctionX X coordinate of the junction
     * @param {Number} junctionY Y coordinate of the junction
     * @param {Boolean} animate Set to True to animate the transition to the new location
     */
    updatePartnerConnection: function(partner, partnerX, partnerY, junctionX, junctionY, animate) {
        var radius = (partner.getGender() == 'U') ? partner.getGraphics().getRadius() * Math.sqrt(2) : partner.getGraphics().getRadius();
        var connectionIndex = +(partner == this.getPartnership().getPartners()[1]);
        var newSide = (junctionX < partnerX) ? -1 : 1;
        var x2 = partnerX + radius * newSide;
        var path = [["M", junctionX, junctionY], ["L", x2, partnerY]];
        if(this.getConnections()[connectionIndex]) {
            if(animate) {
                this.getConnections()[connectionIndex].stop().animate({path: path}, 1000, "easeInOut");
            }
            else {
                this.getConnections()[connectionIndex].attr({path: path});
            }
        }
        else {
            this.getConnections()[connectionIndex] = editor.getPaper().path(path).attr(PedigreeEditor.attributes.partnershipLines).toBack();
        }
    },

    /**
     * Updates the path of the connection for the given pregnancy or creates a new
     * connection if it doesn't exist.
     *
     * @method updatePregnancyConnection
     * @param {Pregnancy} preg Pregnancy associated with this partnership
     * @param {Number} pregX X coordinate of the pregnancy
     * @param {Number} pregY Y coordinate of the pregnancy
     * @param {Number} partnershipX Y coordinate of the junction
     * @param {Number} partnershipY Y coordinate of the junction
     * @param {Boolean} animate Set to true to animate the transition to the new location
     * @return {Raphael.el} The updated connection between the partnership and the pregnancy
     */
    updatePregnancyConnection: function(preg, pregX, pregY, partnershipX, partnershipY, animate) {
        var xDistance = (pregX - partnershipX);
        var yDistance = (pregY - partnershipY) * 0.8;
        var path = [["M", partnershipX, partnershipY],["l",0, yDistance],["l", xDistance,0], ["L", pregX, pregY]];
        preg.pregnancyConnectionPath = path;
        if(this.getPartnership().hasPregnancy(preg) && preg.getGraphics().pregnancyConnection) {
            if(animate) {
                preg.getGraphics().pregnancyConnection.animate({path: path}, 1000, "<>")
            }
            else {
                preg.getGraphics().pregnancyConnection.attr({path: path});
            }
            return preg.getGraphics().pregnancyConnection
        }
        else {
            return editor.getPaper().path(path).attr(PedigreeEditor.attributes.partnershipLines).toBack();
        }
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
    setPos: function(x, y, animate, callback) {
        var me = this;
        var junctionCallback = function () {
            me._absoluteX = x;
            me._absoluteY = y;
        };

        this.getNode().getPregnancies().each(function(pregnancy) {
            me.updatePregnancyConnection(pregnancy, pregnancy.getX(), pregnancy.getY(), x, y,  animate)
        });

        me.getNode().getPartners().each(function(partner) {
            me.updatePartnerConnection(partner, partner.getX(), partner.getY(), x, y, animate);
        });

        if(animate) {
            this.getAllGraphics().stop().animate({'transform': "t " + (x-this.getX()) + "," + (y-this.getY()) + "..."},
                1000, "easeInOut", function() {junctionCallback(); callback && callback();});
        }
        else {
            this.getAllGraphics().transform("t " + (x-this.getX()) + "," + (y-this.getY()) + "...");
            junctionCallback();
        }
    },

    /**
     * Removes all the graphical elements of this partnership from the canvas
     *
     * @method remove
     */
    remove: function() {
        this.getJunctionShape().remove();
        this.getConnections()[0].remove();
        this.getConnections()[1].remove();
        this.getHoverBox().remove();
        this.getChildlessShape() && this.getChildlessShape().remove();
        this.getChildlessStatusLabel() && this.getChildlessStatusLabel().remove();
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
    }
});

//ATTACH CHILDLESS BEHAVIOR METHODS TO PARTNERSHIP
PartnershipVisuals.addMethods(ChildlessBehaviorVisuals);