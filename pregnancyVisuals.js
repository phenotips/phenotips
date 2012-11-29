
var PregnancyVisuals = Class.create(AbstractNodeVisuals, {

    initialize: function($super, pregnancy, x, y) {
        $super(pregnancy, x,y);
        this._junctionShape = editor.getPaper().circle(x,y, editor.attributes.partnershipRadius/3).attr({fill: 'black', stroke: 'none'});
        this._junctionMask = editor.getPaper().circle(x,y, editor.attributes.partnershipRadius*2).attr({fill: 'black', stroke: 'none', opacity: 0});
        this._junctionSet = editor.getPaper().set(this._junctionShape, this._junctionMask);
        this._junctionSet.insertBefore(editor.getGraph().getProband().getGraphics().getAllGraphics().flatten());
        this._idLabel = editor.getPaper().text(x, y-20, editor.DEBUG_MODE ? pregnancy.getID() : "").attr(editor.attributes.dragMeLabel).insertAfter(this._junctionShape.flatten());
        this.grow = this.grow.bind(this);
        this.shrink = this.shrink.bind(this);
        this.onClick = this.onClick.bind(this);
        var p = pregnancy.getPartnership().getGraphics();
        this.pregnancyConnection = p.updatePregnancyConnection(pregnancy, x, y, p.getX(), p.getY(), false)
    },

    grow: function() {
        this._junctionShape.animate({transform: "S4 4 " + this.getX() + " " + this.getY(), fill: 'green' }, 100);
    },

    shrink: function() {
        this._junctionShape.animate({transform: "", fill: 'black' }, 100);
    },

    updateActive: function() {
        if(this.getPregnancy().isActive()) {
            this.getJunctionSet().hover(this.grow, this.shrink);
            this.getJunctionSet().click(this.onClick);
        }
        else {
            this.getJunctionSet().unhover(this.grow, this.shrink);
            this.getJunctionSet().unclick(this.onClick);
        }
    },

    onClick: function() {
        this.getPregnancy().createChild("PlaceHolder", this.getPregnancy().getGender());
        this.shrink();
        this.updateActive();
    },

    /*
     * Returns the Pregnancy for which the graphics are drawn
     */
    getPregnancy: function() {
        return this._node;
    },

    /*
     * Returns the raphael shape that joins connections
     */
    getJunctionSet: function() {
        return this._junctionSet;
    },

    /*
     * Updates the path of the connection for the given child or creates a new
     * connection if it doesn't exist.
     *
     * @param child an AbstractPerson who's a child of this pregnancy
     * @param childX X coordinate of the child
     * @param childY Y coordinate of the child
     * @junctionX the X coordinate of the junction
     * @junctionY Y coordinate of the junction
     * @animate set to true if you want to animate a transition to the new location
     */
    updateChildConnection: function(child, childX, childY, junctionX, junctionY, animate) {
        var radius = (child.getGender && child.getGender() == "U") ? editor.attributes.radius * (Math.sqrt(6)/2) : child.getGraphics().getRadius();
        var topCoordinate = childY - radius;
        var xDistance = (childX - junctionX);
        var yDistance = (childY - junctionY)/2;
        var path = [["M", junctionX, junctionY],["L", childX, topCoordinate]];
        child.parentConnectionPath = path;
        if(this.getPregnancy().hasChild(child) && child.parentConnection) {
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
        this.pregnancyConnection.remove();
        this.getJunctionSet().remove();
    },

    /*
     * Removes the lines connecting child to this pregnancy
     *
     * @param child an AbstractNode who is a child of this pregnancy
     */
    removeChild: function(child) {
        if(child.getParentPregnancy() == this.getPregnancy()) {
            child.parentConnection.remove();
            child.parentConnection = null;
        }
    },

    addChild: function(child) {
        if(child.getParentPregnancy() == this.getPregnancy()) {
            child.parentConnection = this.updateChildConnection(child, child.getX(), child.getY(), this.getX(), this.getY());
        }
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
        (node.getType() == "PlaceHolder") && (attr['stroke-dasharray'] = '- ');
        return attr;
    },

    getShapes: function($super) {
        return $super().push(this.getJunctionSet());
    },

    getAllGraphics: function($super) {
        return editor.getPaper().set(this._idLabel).concat($super());
    }
});