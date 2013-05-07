/**
 * Class for organizing graphics for Pregnancy nodes.
 *
 * @class PregnancyVisuals
 * @extends AbstractNodeVisuals
 * @constructor
 * @param {Pregnancy} pregnancy The node for which the graphics are handled
 * @param {Number} x The x coordinate on the canvas
 * @param {Number} y The y coordinate on the canvas
 */
var PregnancyVisuals = Class.create(AbstractNodeVisuals, {

    initialize: function($super, pregnancy, x, y) {
        $super(pregnancy, x,y);
        this._junctionShape = editor.getPaper().circle(x,y, PedigreeEditor.attributes.partnershipRadius/3).attr({fill: 'black', stroke: 'none'});
        this._junctionMask = editor.getPaper().circle(x,y, PedigreeEditor.attributes.partnershipRadius*2).attr({fill: 'black', stroke: 'none', opacity: 0});
        this._junctionSet = editor.getPaper().set(this._junctionShape, this._junctionMask);
        this._junctionSet.insertBefore(editor.getGraph().getProband().getGraphics().getAllGraphics().flatten());
        this._idLabel = editor.getPaper().text(x, y-20, editor.DEBUG_MODE ? pregnancy.getID() : "").attr(PedigreeEditor.attributes.dragMeLabel).insertAfter(this._junctionShape.flatten());
        this.grow = this.grow.bind(this);
        this.shrink = this.shrink.bind(this);
        this._onClick = this._onClick.bind(this);
        var p = pregnancy.getPartnership().getGraphics();
        this.pregnancyConnection = p.updatePregnancyConnection(pregnancy, x, y, p.getX(), p.getY(), false);
        this._junctionShape.shrunk = true;
    },

    /**
     * Expands the pregnancy circle
     *
     * @method grow
     */
    grow: function() {
        if(this._junctionShape.shrunk && this.getPregnancy().isActive()) {
            this._junctionShape.ot = this._junctionShape.transform();
            this._junctionShape.stop().animate({transform: this._junctionShape.ot + ", S4 4 " + this.getX() + " " + this.getY(), fill: 'green' }, 100, function(){});
        }
    },

    /**
     * Shrinks the pregnancy circle
     *
     * @method shrink
     */
    shrink: function() {
        var me = this;
        this._junctionShape.shrunk = false;
        this._junctionShape.stop().animate({transform: this._junctionShape.ot, fill: 'black' }, 100, function() {me._junctionShape.shrunk = true});
    },

    /**
     * Enables or disables pregnancy interactivity based on the "active" status
     *
     * @method updateActive
     */
    updateActive: function() {
        if(this.getPregnancy().isActive()) {
            this.getJunctionSet().hover(this.grow, this.shrink);
            this.getJunctionSet().click(this._onClick);
        }
        else {
            this.getJunctionSet().unhover(this.grow, this.shrink);
            this.getJunctionSet().unclick(this._onClick);
        }
    },

    /**
     * Actions that happen when the pregnancy bubble is clicked
     *
     * @method _onClick
     * @private
     */
    _onClick: function() {
        this.getPregnancy().createChildAction();
        this.shrink();
        this.updateActive();
    },

    /**
     * Returns the Pregnancy for which the graphics are drawn
     *
     * @method
     * @return {Pregnancy}
     */
    getPregnancy: function() {
        return this._node;
    },

    /*
     * Returns the pregnancy bubble
     *
     * @method getJunctionSet
     * @return {Raphael.st}
     */
    getJunctionSet: function() {
        return this._junctionSet;
    },

    /**
     * Updates the path of the connection for the given child or creates a new
     * connection if it doesn't exist.
     *
     * @method updateChildConnection
     * @param {AbstractPerson} child Child of this pregnancy
     * @param {Number} childX X coordinate of the child
     * @param {Number} childY Y coordinate of the child
     * @param {Number} junctionX the X coordinate of the junction
     * @param {Number} junctionY Y coordinate of the junction
     * @param {Boolean} animate Set to true to animate the transition to the new location
     * @return {Raphael.el} Edge connecting child and pregnancy
     */
    updateChildConnection: function(child, childX, childY, junctionX, junctionY, animate) {
        var radius = (child.getGender && child.getGender() == "U") ? PedigreeEditor.attributes.radius * (Math.sqrt(6)/2) : child.getGraphics().getRadius();
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
            return child.parentConnection;
        }
        else {
            return editor.getPaper().path(path).attr(this.getConnectionAttributes(child)).toBack();
        }
    },

    /**
     * Changes the position of the pregnancy bubble to the coordinate (x,y) and updates all surrounding connections.
     *
     * @method setPos
     * @param {Number} x the x coordinate
     * @param {Number} y the y coordinate
     * @param {Boolean} animate set to true if you want to animate the transition
     * @param {Function} callback the function called at the end of the animation
     */
    setPos: function(x, y, animate, callback) {
        var me = this;
        var junctionCallback = function () {
            me._absoluteX = x;
            me._absoluteY = y;
        };

        this.getNode().getChildren().each(function(child) {
            me.updateChildConnection(child, child.getX(), child.getY(), x, y,  animate)
        });
        var p = this.getNode().getPartnership();
        p.getGraphics().updatePregnancyConnection(this.getNode(), x, y, p.getX(), p.getY(), animate);

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
        this.pregnancyConnection.remove();
        this.getJunctionSet().remove();
    },

    /**
     * Removes the edge connecting child to this pregnancy
     *
     * @method removeChild
     * @param {AbstractPerson} child Child of this pregnancy
     */
    removeChild: function(child) {
        if(child.getParentPregnancy() == this.getPregnancy()) {
            child.parentConnection.remove();
            child.parentConnection = null;
        }
    },

    /**
     * Creates an edge connecting child to this pregnancy
     *
     * @method addChild
     * @param {AbstractPerson} child
     */
    addChild: function(child) {
        if(child.getParentPregnancy() == this.getPregnancy()) {
            child.parentConnection = this.updateChildConnection(child, child.getX(), child.getY(), this.getX(), this.getY());
        }
    },

    /**
     * Returns color and style attributes for a connection based on the type of the connection and type of the node.
     * PlaceHolder connections have a dash-array stroke.
     *
     * @method getConnectionAttributes
     * @param {AbstractPerson} node the node for which the connection is being drawn
     * @return Object with raphael attributes
     */
    getConnectionAttributes: function(node) {
        var attr = {"stroke-width": 2, stroke: '#2E2E56'};
        (node.getType() == "PlaceHolder") && (attr['stroke-dasharray'] = '- ');
        return attr;
    },

    /**
     * Returns a Raphael set of graphic elements of which the icon of the Pregnancy consists. Does not
     * include labels.
     *
     * @method getShapes
     * @return {Raphael.st}
     */
    getShapes: function($super) {
        return $super().push(this.getJunctionSet());
    },

    /**
     * Returns a Raphael set of all the graphics and labels associated with this Pregnancy. Includes labels.
     *
     * @method getAllGraphics
     * @return {Raphael.st}
     */
    getAllGraphics: function($super) {
        return editor.getPaper().set(this._idLabel).concat($super());
    }
});