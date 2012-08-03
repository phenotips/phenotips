/*
 * A general superclass for the a graphic engine used by nodes on the Pedigree graph. Can display
 * a shape representing the gender of the attached node.
 */

var PartnershipVisuals = Class.create(AbstractNodeVisuals, {

    initialize: function($super, partnership, x, y) {
        $super(partnership, x,y);
        this._junctionShape = editor.paper.circle(x, y, 2).attr("fill", "black");
        //TODO: find out whether there is an arc
        this._connection1 = this.updatePartnerConnection(this.getPartnership().getPartner1());
        this._connection2 = this.updatePartnerConnection(this.getPartnership().getPartner2());
    },

    getPartnership: function() {
        return this._node;
    },

    getJunctionShape: function() {
        return this._junctionShape;
    },

    getConnection1: function() {
        return this._connection1;
    },

    getConnection2: function() {
        return this._connection2;
    },

    updatePartnerConnection: function(partner) {
        var connection = (partner == this.getPartnership().getPartner1()) ? this.getConnection1() : this.getConnection2();
        var x2 = (this.getAbsX() < partner.getX()) ? partner.getGraphics().getLeftCoordinate().x :
            partner.getGraphics().getRightCoordinate().x;
        var y2 = partner.getGraphics().getLeftCoordinate().y;
        var path = [["M", this.getAbsX(), this.getAbsY()], ["L", x2, y2]];
        partner.connectionPath = path;
        if(connection) {
            connection.attr({path: path});
        }
        else {
            return editor.paper.path(path).attr(this.getConnectionAttributes(partner, 'partner')).toBack();
        }
    },

    updateChildConnection: function(child) {
        var yDistance = (child.getGraphics().getUpCoordinate().y - this.getAbsY())/2;
        var xDistance = (child.getX() - this.getAbsX());
        var path = [["M", this.getAbsX(), this.getAbsY()],["l",0, yDistance],["l",xDistance,0], ["L", child.getX(), child.getGraphics().getUpCoordinate().y]];
        child.parentConnectionPath = path;
        if(this.getPartnership().hasChild(child) && child.parentConnection) {
            child.parentConnection.attr({path: path});
        }
        else {
            return editor.paper.path(path).attr(this.getConnectionAttributes(child, 'child')).toBack();
        }
    },

    getConnectionAttributes: function(node, type) {
        var attr = {"stroke-width": 2};
        (type == 'partner') ? attr.stroke = '#2E2E56' : attr.stroke = '#2E2E56';
        (node.getType() == "ph") && (attr['stroke-dasharray'] = '- ');
        return attr;
    },

    remove: function() {
        this.getJunctionShape().remove();
        this.getConnection1().remove();
        this.getConnection2().remove();
    },

    movePartnerConnectionTo: function(partner, x, y) {
        var connection = (partner == this.getPartnership().getPartner1()) ? this.getConnection1() : this.getConnection2();
        var xDisplacement = x - partner.getX();
        var yDisplacement = y - partner.getY();
        var displacement = Math.sqrt(xDisplacement * xDisplacement + yDisplacement * yDisplacement);
        var x2 = (this.getAbsX() < x) ? partner.getGraphics().getLeftCoordinate().x + xDisplacement :
            partner.getGraphics().getRightCoordinate().x + xDisplacement;
        var y2 = partner.getGraphics().getLeftCoordinate().y + yDisplacement;
        var path = [["M", this.getAbsX(), this.getAbsY()], ["L", x2, y2]];
        partner.connectionPath = path;
        connection.stop().animate({path: path}, displacement /.4, "<>")
    },

    moveChildConnectionTo: function(child, x, y) {
        var xDisplacement = x - child.getX();
        var yDisplacement = y - child.getY();
        var displacement = Math.sqrt(xDisplacement * xDisplacement + yDisplacement * yDisplacement);
        var yDistance = (child.getGraphics().getUpCoordinate().y + yDisplacement - this.getAbsY())/2;
        var xDistance = (child.getX() + xDisplacement - this.getAbsX());
        var path = [["M", this.getAbsX(), this.getAbsY()],["l",0, yDistance],["l",xDistance,0], ["L", child.getX() + xDisplacement, child.getGraphics().getUpCoordinate().y + yDisplacement]];
        child.parentConnection.stop().animate({path: path}, displacement /.4, "<>");
    },

    setAbsPos: function($super, x,y) {
        $super(x,y);
        this.updatePartnerConnection(this.getNode().getPartner1());
        this.updatePartnerConnection(this.getNode().getPartner2());
        var me = this;
        this.getNode().getChildren().each(function(child) {
            me.updateChildConnection(child);
        });
    },

    moveTo: function(x,y) {
        var xDisplacement = x - this.getAbsX();
        var yDisplacement = y - this.getAbsY();
        var displacement = Math.sqrt(xDisplacement * xDisplacement + yDisplacement * yDisplacement);
        var me = this;
        me.getNode().getPartner1().connectionPath[0][1] = x;
        me.getNode().getPartner1().connectionPath[0][2] = y;
        this.getConnection1().stop().animate({path: me.getNode().getPartner1().connectionPath}, displacement /.4, "<>");

        me.getNode().getPartner2().connectionPath[0][1] = x;
        me.getNode().getPartner2().connectionPath[0][2] = y;
        this.getConnection2().stop().animate({path: me.getNode().getPartner2().connectionPath}, displacement /.4, "<>");

        this.getNode().getChildren.each(function(child) {
            var yDistance = (child.getGraphics().getUpCoordinate().y - y)/2;
            var xDistance = (child.getX() - x);
            child.parentConnectionPath = [["M", x, y],["l",0, yDistance],["l",xDistance,0], ["L", child.getX(), child.getGraphics().getUpCoordinate().y]];
            child.parentConnection.stop().animate({path: child.parentConnectionPath}, displacement /.4, "<>");
        });



        this.getJunctionShape().stop().animate({'transform': "t " + (x-this.getAbsX()) + "," + (y-this.getAbsY()) + "..."},
            displacement /.4, "<>", function() {me.setAbsPos(x,y);});
    }
});