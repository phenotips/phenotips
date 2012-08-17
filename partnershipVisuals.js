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
        this._connections = [null, null]
        var me = this;
        me.getNode().getPartners().each(function(partner) {
            me.updatePartnerConnection(partner, partner.getX(), partner.getY(), x, y);
        });
        this._hoverbox = new PartnershipHoverbox(partnership, x, y, this.getShapes());
        //this.draw();
    },

    getHoverBox: function() {
        return this._hoverbox;
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
            this.getConnections()[connectionIndex] = editor.getPaper().path(path).attr(this.getConnectionAttributes(partner, 'partner')).toBack();
        }
    },

    /*
     * Updates the path of the connection for the given child or creates a new
     * connection if it doesn't exist.
     *
     * @param child an AbstractPerson who's a child in this Partnership
     * @param childX X coordinate of the child
     * @param childY Y coordinate of the child
     * @junctionX the X coordinate of the junction
     * @junctionY Y coordinate of the junction
     * @animate set to true if you want to animate a transition to the new location
     */
    updateChildConnection: function(child, childX, childY, junctionX, junctionY, animate) {
        var radius = (child.getGender() == "U") ? editor.attributes.radius * (Math.sqrt(6)/2) : child.getGraphics().getRadius();
        var topCoordinate = childY - radius;
        var xDistance = (childX - junctionX);
        var yDistance = (topCoordinate - junctionY)/2;
        var path = [["M", junctionX, junctionY],["l",0, yDistance],["l",xDistance,0], ["L", childX, topCoordinate]];
        child.parentConnectionPath = path;
        if(this.getPartnership().hasChild(child) && child.parentConnection) {
            if(animate) {
                child.parentConnection.animate({path: path}, 1000, "<>")
            }
            else {
                child.parentConnection.attr({path: path});
            }
        }
        else {
            return editor.getPaper().path(path).attr(this.getConnectionAttributes(child, 'child')).toBack();
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

        this.getNode().getChildren().each(function(child) {
            me.updateChildConnection(child, child.getX(), child.getY(), x, y,  animate)
        });

        me.getNode().getPartners().each(function(partner) {
            me.updatePartnerConnection(partner, partner.getX(), partner.getY(), x, y, animate);
        });

        if(animate) {
            this.getJunctionShape().stop().animate({'transform': "t " + (x-this.getX()) + "," + (y-this.getY()) + "..."},
                1000, "easeInOut", function() {junctionCallback(); callback && callback();});
        }
        else {
            this.getJunctionShape().transform("t " + (x-this.getX()) + "," + (y-this.getY()) + "...");
            junctionCallback();
        }
    },

    /*
     * Removes all the graphical elements of this partnership from the canvas
     */
    remove: function() {
        this.getJunctionShape().remove();
        alert(this.getConnections().length)
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
    },

    getShapes: function() {
        var connections = editor.getPaper().set(this.getConnections()[0], this.getConnections()[1]);
        this.getNode().getChildren().each(function(child) {
            connections.push(child.parentConnection);
        });
        return editor.getPaper().set(connections, this.getJunctionShape());
    },

    getAllGraphics: function($super) {
        var connections = editor.getPaper().set(this.getConnections()[0], this.getConnections()[1]);
        this.getNode().getChildren().each(function(child) {
            connections.push(child.parentConnection);
        });
        return $super().push(this.getHoverBox().getFrontElements(), this.getHoverBox().getBackElements());
    }
});