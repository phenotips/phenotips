/*
 * The user interface for managing partnerships. Contains information about
 * connections, junction, and the children handle.
 *
 * @param partnership the Partnership for which the graphics are drawn
 * @param x the x coordinate of the junction node on the canvas
 * @param y the y coordinate of the junction node on the canvas
 */

var PartnershipVisuals = Class.create(AbstractNodeVisuals, {

    initialize: function($super, partnership, x, y) {
        $super(partnership, x,y);
        this._childlessShape = null;
        this._childlessStatusLabel = null;
        this._junctionShape = editor.getPaper().circle(x,y, editor.attributes.partnershipRadius).attr({fill: '#EA5E48', stroke: 'black', 'stroke-width':2});
        this._junctionShape.insertBefore(editor.getGraph().getProband().getGraphics().getAllGraphics().flatten());

        //TODO: find out whether there is an arc
        this._connections = [null, null];
        var me = this;
        me.getNode().getPartners().each(function(partner) {
            me.updatePartnerConnection(partner, partner.getX(), partner.getY(), x, y);
        });
        this._hoverBox = new PartnershipHoverbox(partnership, x, y, this.getShapes());
        this.area = null;
        this._idLabel = editor.getPaper().text(x, y-20, editor.DEBUG_MODE ? partnership.getID() : "").attr(editor.attributes.dragMeLabel).insertAfter(this._junctionShape.flatten());
    },

    grow: function() {
        this.area = this.getJunctionShape().clone().flatten().insertBefore(this.getJunctionShape().flatten());
        this.area.attr({'fill': '#587498', stroke: 'none'});
        this.area.ot = this.area.transform();
        this.area.animate(Raphael.animation({transform : "...S2"}, 400, 'bounce'));
    },

    /*
     * Returns the Partnership for which the graphics are drawn
     */
    getPartnership: function() {
        return this._node;
    },

    /*
     * Returns the raphael shape that joins connections
     */
    getJunctionShape: function() {
        return this._junctionShape;
    },

    /*
     * Returns an array containing the two partner connections
     */
    getConnections: function() {
        return this._connections;
    },

    /*
     * Updates the path of the connection for the given partner or creates a new
     * connection if it doesn't exist.
     *
     * @param partner an AbstractPerson who's a partner in this Partnership
     * @param partnerX X coordinate of the partner
     * @param partnerY Y coordinate of the partner
     * @junctionX the X coordinate of the junction
     * @junctionY Y coordinate of the junction
     * @animate set to true if you want to animate a transition to the new location
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
            this.getConnections()[connectionIndex] = editor.getPaper().path(path).attr(editor.attributes.partnershipLines).toBack();
        }
    },

    /*
     * Updates the path of the connection for the given pregnancy or creates a new
     * connection if it doesn't exist.
     *
     * @param preg pregnancy of this partnership
     * @param pregX the X coordinate of the pregnancy
     * @param pregY the Y coordinate of the pregnancy
     * @partnershipX the X coordinate of the junction
     * @partnershipY Y coordinate of the junction
     * @animate set to true if you want to animate a transition to the new location
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
        }
        else {
            return editor.getPaper().path(path).attr(editor.attributes.partnershipLines).toBack();
        }
    },

    /*
     * Changes the position of the junction to the coordinate (x,y) and updates all surrounding connections.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param animate set to true if you want to animate the transition
     * @param callback the function called at the end of the animation
     */
    setPos: function(x,y, animate, callback) {
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

    /*
     * Removes all the graphical elements of this partnership from the canvas
     */
    remove: function() {
        this.getJunctionShape().remove();
        this.getConnections()[0].remove();
        this.getConnections()[1].remove();
        this.getHoverBox().remove();
        this.getChildlessShape() && this.getChildlessShape().remove();
        this.getChildlessStatusLabel() && this.getChildlessStatusLabel().remove();
    },
//
//    /*
//     * Returns color and style attributes for a connection based on the type of the connection and type of the node.
//     * PlaceHolder connections have a dash-array stroke.
//     *
//     * @param node the node for which the connection is being drawn
//     * @param type can be either "partner" or "child"
//     */
//    getConnectionAttributes: function(node, type) {
//        return {"stroke-width": 2, stroke : '#2E2E56'};
//    },

    getShapes: function($super) {
        return $super().push(this.getJunctionShape());
    },

    getAllGraphics: function($super) {
        return editor.getPaper().set(this.getHoverBox().getBackElements(), this._idLabel).concat($super()).push(this.getHoverBox().getFrontElements());
    }
});

PartnershipVisuals.addMethods(ChildlessBehaviorVisuals);