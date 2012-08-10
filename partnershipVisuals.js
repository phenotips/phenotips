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
        this._junctionShape = editor.getPaper().circle(x, y, 2).attr("fill", "black");
        //TODO: find out whether there is an arc
        this._connections = [null, null];
        this.updatePartnerConnection(this.getPartnership().getPartners()[0]);
        this.updatePartnerConnection(this.getPartnership().getPartners()[1]);
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
     * Updates the path of the connection for the given partner based on the partner's location
     *
     * @param partner an AbstractPerson who's a partner in this Partnership
     */
    updatePartnerConnection: function(partner) {
        var connectionIndex = +(partner == this.getPartnership().getPartners()[1]);
        var x2 = (this.getX() < partner.getX()) ? partner.getGraphics().getLeftCoordinate().x :
            partner.getGraphics().getRightCoordinate().x;
        var y2 = partner.getGraphics().getLeftCoordinate().y;
        var path = [["M", this.getX(), this.getY()], ["L", x2, y2]];
        partner.connectionPath = path;
        if(this.getConnections()[connectionIndex]) {
            this.getConnections()[connectionIndex].attr({path: path});
        }
        else {
            this.getConnections()[connectionIndex] = editor.getPaper().path(path).attr(this.getConnectionAttributes(partner, 'partner')).toBack();
        }
    },

    /*
     * Animates the connection for the given partner by 'pulling' the junction end to the coordinate (x, y)
     *
     * @param partner an AbstractPerson who's a partner in this Partnership
     * @param x the x coordinate towards which the connection is animated
     * @param y the y coordinate towards which the connection is animated
     */
    translatePartnerConnection: function(partner, x, y) {
        var connectionIndex = +(partner == this.getPartnership().getPartners()[1]);
        var xDisplacement = x - partner.getX();
        var yDisplacement = y - partner.getY();
        var x2 = (this.getX() < x) ? partner.getGraphics().getLeftCoordinate().x + xDisplacement :
            partner.getGraphics().getRightCoordinate().x + xDisplacement;
        var y2 = partner.getGraphics().getLeftCoordinate().y + yDisplacement;
        var path = [["M", this.getX(), this.getY()], ["L", x2, y2]];
        partner.connectionPath = path;
        this.getConnections()[connectionIndex].stop().animate({path: path}, 1000, "easeInOut")
    },

    /*
     * Updates the path of the connection for the given child based on the child's location
     *
     * @param child an AbstractPerson who's a child of this Partnership
     */
    updateChildConnection: function(child) {
        var yDistance = (child.getGraphics().getTopCoordinate().y - this.getY())/2;
        var xDistance = (child.getX() - this.getX());
        var path = [["M", this.getX(), this.getY()],["l",0, yDistance],["l",xDistance,0], ["L", child.getX(), child.getGraphics().getTopCoordinate().y]];
        child.parentConnectionPath = path;
        if(this.getPartnership().hasChild(child) && child.parentConnection) {
            child.parentConnection.attr({path: path});
        }
        else {
            return editor.getPaper().path(path).attr(this.getConnectionAttributes(child, 'child')).toBack();
        }
    },

    /*
     * Animates the connection for the given partner by 'pulling' the junction end to the coordinate (x, y)
     *
     * @param partner an AbstractPerson who's a partner in this Partnership
     * @param x the x coordinate towards which the connection is animated
     * @param y the y coordinate towards which the connection is animated
     */
    translateChildConnection: function(child, x, y) {
        var xDisplacement = x - child.getX();
        var yDisplacement = y - child.getY();
        var yDistance = (child.getGraphics().getTopCoordinate().y + yDisplacement - this.getY())/2;
        var xDistance = (child.getX() + xDisplacement - this.getX());
        var path = [["M", this.getX(), this.getY()],["l",0, yDistance],["l",xDistance,0], ["L", child.getX() + xDisplacement, child.getGraphics().getTopCoordinate().y + yDisplacement]];
        child.parentConnection.stop().animate({path: path}, 1000, "<>");
    },

    /*
     * Animates the junction to the coordinate (x,y) and animates all the connections to follow the junction.
     *
     * @param x the x coordinate towards which the junction is animated
     * @param y the y coordinate towards which the junction is animated
     */
    setPos: function(x,y) {
        var me = this;
        me.getNode().getPartners()[0].connectionPath[0][1] = x;
        me.getNode().getPartners()[0].connectionPath[0][2] = y;
        this.getConnections()[0].stop().animate({path: me.getNode().getPartners()[0].connectionPath}, 1000, "<>");

        me.getNode().getPartners()[1].connectionPath[0][1] = x;
        me.getNode().getPartners()[1].connectionPath[0][2] = y;
        this.getConnections()[1].stop().animate({path: me.getNode().getPartners()[1].connectionPath}, 1000, "<>");

        this.getNode().getChildren().each(function(child) {
            var yDistance = (child.getGraphics().getTopCoordinate().y - y)/2;
            var xDistance = (child.getX() - x);
            child.parentConnectionPath = [["M", x, y],["l",0, yDistance],["l",xDistance,0], ["L", child.getX(), child.getGraphics().getTopCoordinate().y]];
            child.parentConnection.stop().animate({path: child.parentConnectionPath}, 1000, "<>");
        });

        this.getJunctionShape().stop().animate({'transform': "t " + (x-this.getX()) + "," + (y-this.getY()) + "..."},
            1000, "<>", function() {
                me._absoluteX = x;
                me._absoluteY = y;
                this.updatePartnerConnection(me.getNode().getPartners()[0]);
                this.updatePartnerConnection(me.getNode().getPartners()[1]);
                me.getNode().getChildren().each(function(child) {
                    me.updateChildConnection(child);
                });
        });
    },

    /*
     * Removes all the graphical elements of this partnership from the canvas
     */
    remove: function() {
        this.getJunctionShape().remove();
        this.getConnections()[0].remove();
        this.getConnections()[1].remove();
    },

    /*
     * Returns color and style attributes for a connection based on the type of the connection and type of the node.
     * PlaceHolder connections have a dash-array stroke.
     *
     * @param node the node for which the connection is being drawn
     * @param type can be either "partner" or "child"
     */
    getConnectionAttributes: function(node, type) {
        var attr = {"stroke-width": 2};
        (type == 'partner') ? attr.stroke = '#2E2E56' : attr.stroke = '#2E2E56';
        (node.getType() == "ph") && (attr['stroke-dasharray'] = '- ');
        return attr;
    }
});