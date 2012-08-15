/*
 * AbstractHoverbox is an abstract class for all the UI elements and graphics surrounding a a node on the canvas (a Person
  * or a partnership). This includes the box that appears around the node when it's hovered by a mouse, as
 * well as the handles used for creating connections and creating new nodes.
 *
 * @node the node Person or Partnership for which the hoverbox is drawn
 * @param x the x coordinate for the hoverbox
 * @param y the y coordinate for the hoverbox
 */

var AbstractHoverbox = Class.create({

    initialize: function(node, x, y, width, height) {
        this._node = node;
        this._relativeX = x;
        this._relativeY = y;
        this._width = width;
        this._height = height;
        this._isHovered = false;
        this._orbs = editor.getPaper().set();
        this._handles = this.generateHandles();

        var boxOnHover = editor.getPaper().rect(this.getX()-(this._width/2), this.getY()-(this._height/2),
            this._width, this._height, 5).attr(editor.attributes.boxOnHover);
        this._backElements =  editor.getPaper().set().push(boxOnHover, this.getHandles());
        var mask = editor.getPaper().rect(this.getX()-(this._width/2), this.getY()-(this._height/2),this._width, this._height);
        mask.attr({fill: 'gray', opacity: 0});

        var me = this;
        this._frontElements = editor.getPaper().set().push(mask, this.getDeleteBtn(), this._orbs);
        this._frontElements.hover(function() {me.setHovered(true)}, function() {me.setHovered(false)});
        this.animateDrawHoverZone = this.animateDrawHoverZone.bind(this);
        this.animateHideHoverZone =  this.animateHideHoverZone.bind(this);
        this.hide();
        this.enable();
    },

    /*
     * [Abstract] Creates the handles used in this hoverbox. Returns a set of handles.
     */
    generateHandles: function() {
        return editor.getPaper().set();
    },

    /*
     * Returns the node Person or Partnership for which the hoverbox is drawn
     */
    getNode: function() {
        return this._node;
    },

    /*
     * Returns the x coordinate on the Raphael canvas at which the hoverbox was originally drawn
     */
    getX: function() {
        return this._relativeX;
    },

    /*
     * Returns the y coordinate on the Raphael canvas at which the hoverbox was originally drawn
     */
    getY: function() {
        return this._relativeY;
    },

    /*
     * Returns the a Raphael set containing the four draggable handles
     */
    getHandles: function() {
        return this._handles;
    },

    /*
     * Creates a Delete button with a red X. Returns a Raphael set.
     */
    generateDeleteBtn: function(x, y) {
        var me = this,
            path = "M24.778,21.419 19.276,15.917 24.777,10.415 21.949,7.585 16.447,13.087 10.945,7.585 8.117,10.415 13.618,15.917 8.116,21.419 10.946,24.248 16.447,18.746 21.948,24.248z",
            iconScale = editor.attributes.radius * 0.014,
            deleteBtnIcon = editor.getPaper().path(path).attr(editor.attributes.deleteBtnIcon);

        deleteBtnIcon.transform(["t" , x , y, "s", iconScale, iconScale, 0, 0]);
        var deleteBtnMask = editor.getPaper().rect(deleteBtnIcon.getBBox().x, deleteBtnIcon.getBBox().y,
            deleteBtnIcon.getBBox().width, deleteBtnIcon.getBBox().height, 1);
        deleteBtnMask.attr({fill: 'gray', opacity: 0}).transform("s1.5");

        var button = editor.getPaper().set(deleteBtnMask, deleteBtnIcon);
        button.click(function(){
            var confirmation = confirm("Are you sure you want to delete this node?");
            confirmation && me.getPartnership().remove(false, true)
        });
        button.mousedown(function(){deleteBtnMask.attr(editor.attributes.btnMaskClick)});
        button.hover(function() {
                deleteBtnMask.attr(editor.attributes.btnMaskHoverOn)
            },
            function() {
                deleteBtnMask.attr(editor.attributes.btnMaskHoverOff)
            });

    },

    /*
     * Returns a Raphael set containing the button for deleting the node
     */
    getDeleteBtn: function() {
        return this._deleteBtn;
    },

    /*
     * Returns the Raphael element for the delete button graphic
     */
    getDeleteBtnIcon: function() {
        return this._deleteBtn[0];
    },

    /*
     * Returns the gray box that appears when the node is hovered
     */
    getBoxOnHover: function() {
        return this.getBackElements()[0];
    },

    /*
     * Returns true if the hover box is currently hovered
     */
    isHovered: function() {
        return this._isHovered;
    },

    /*
     * Sets the hovered property to isHovered.
     *
     * @param isHovered set to true if the box is hovered
     */
    setHovered: function(isHovered) {
        this._isHovered = isHovered;
    },

    /*
     * Returns the invisible mask layer in front of the hoverbox
     */
    getHoverZoneMask: function() {
        return this.getFrontElements()[0];
    },

    /*
     * Returns a Raphael set containing all hoverbox elements that are layered
     * in front of the node graphics
     */
    getFrontElements: function() {
        return this._frontElements;
    },

    /*
     * Returns a Raphael set containing all hoverbox elements that are layered
     * behind of the node graphics
     */
    getBackElements: function() {
        return this._backElements;
    },

    /*
     * Creates a handle with a blue orb from the center of the node to the coordinate (orbX, orbY);
     *
     * @param type should be 'parent', 'child' or 'partner'
     */
    generateHandle: function(type, orbX, orbY) {
        var path = [["M", this.getX(), this.getY()],["L", orbX, orbY]],
            connection = editor.getPaper().path(path).attr({"stroke-width": 4, stroke: "gray"}),
            orbRadius = editor.attributes.radius/7,
            orbHue = editor.attributes.orbHue,
            orb = generateOrb(editor.getPaper(), orbX, orbY, orbRadius, orbHue).attr("cursor", "pointer"),
            handle = editor.getPaper().set().push(connection, orb),
            hasEnded = true,
            me = this;
        handle.type = type;
        connection.oPath = path;

        var start = function() {
            if(!hasEnded) {
                //isDrag = true;
                //end();
                return;
            }
            hasEnded = false;
            //document.observe('mousedown', catchRightClick);
            me.disable();
            me.getNode().getGraphics().getAllGraphics().toFront();
            orb.ox = orb[0].attr("cx");
            orb.oy = orb[0].attr("cy");
            connection.ox = connection.oPath[1][1];
            connection.oy = connection.oPath[1][2];
            editor.currentDraggable.node = me.getNode();
            editor.currentDraggable.handle = type;
            handle.isDragged = false;
            editor.enterHoverMode(me.getPartnership());
        };
        var move = function(dx, dy) {
            orb.attr("cx", orb.ox + dx);
            orb.attr("cy", orb.oy + dy);
            connection.oPath[1][1] = connection.ox + dx;
            connection.oPath[1][2] = connection.oy + dy;
            connection.attr("path", connection.oPath);
            if(dx > 5 || dx < -5 || dy > 5 || dy < -5 ) {
                handle.isDragged = true;
            }
        };
        var end = function() {
            if(hasEnded) {
                return;
            }
            //document.stopObserving('mousedown', catchRightClick);
            orb.animate({"cx": orb.ox, "cy": orb.oy}, +handle.isDragged * 1000, "elastic",
                function() {
                    var numMovingHandles = 0;
                    me.getHandles().each(function(handle) {
                        handle.isDragged && numMovingHandles++;
                    });
                    if(numMovingHandles == 0) {
                        me.enable();
                        me.animateHideHoverZone();
                        hasEnded = true;
                    }
                });
            editor.exitHoverMode();
            me.handleAction(handle, handle.isDragged);
            connection.oPath[1][1] = connection.ox;
            connection.oPath[1][2] = connection.oy;
            connection.animate({"path": connection.oPath},1000, "elastic");
            handle.isDragged = false;
        };

        orb.drag(move, start, end);
        this._orbs.push(orb);
        return handle;
    },

    /*
     * Performs the appropriate action for clicking on the handle of type handleType
     *
     * @param handleType can be either "child", "partner" or "parent"
     */
    handleAction : function(handleType, isDrag) {
        if(isDrag) {
            if(editor.currentHoveredNode && editor.currentHoveredNode.validPartnerSelected) {
                editor.currentHoveredNode.validPartnerSelected = false;
                this.getNode().addPartner(editor.currentHoveredNode);
            }
            else if(editor.currentHoveredNode && editor.currentHoveredNode.validChildSelected) {
                    editor.currentHoveredNode.validChildSelected = false;
                this.getNode().addChild(editor.currentHoveredNode);

            }
            else if(editor.currentHoveredNode && editor.currentHoveredNode.validParentSelected) {
                editor.currentHoveredNode.validParentSelected = false;
                this.getNode().addParent(editor.currentHoveredNode);
            }
            else if(editor.currentHoveredNode && editor.currentHoveredNode.validParentsSelected) {
                editor.currentHoveredNode.validParentsSelected = false;
                this.getNode().addParents(editor.currentHoveredNode);
            }
        }
        else {
            if(handleType == "partner") {
                this.getNode().createPartner(false);
            }
            else if(handleType == "child") {
                this.getNode().createChild();
            }
            else if(handleType == "parent") {
                this.getPartnership().createParents();
            }
            editor.currentHoveredNode && editor.currentHoveredNode.getGraphics().getHoverBox().getBoxOnHover().attr(editor.attributes.boxOnHover);
        }
        editor.currentHoveredNode =  editor.currentDraggable.node = editor.currentDraggable.handle = null;
    },

    /*
     * Fades the hoverbox graphics in
     */
    animateDrawHoverZone: function() {
        this.getPartnership().getGraphics().setSelected(true);
        this.getBoxOnHover().stop().animate({opacity:0.7}, 300);
        this.getDeleteBtnIcon().stop().animate({opacity:1}, 300);
    },

    /*
     * Fades the hoverbox graphics out
     */
    animateHideHoverZone: function() {
        if(!this.isHovered()) {
            this.getPartnership().getGraphics().setSelected(false);
            this.getBoxOnHover().stop().animate({opacity:0}, 200);
            this.getDeleteBtnIcon().stop().animate({opacity:0}, 200);
        }
    },

    /*
     * Hides the hoverbox's graphical elements
     */
    hide: function() {
        this.getBoxOnHover().attr({opacity:0});
        this.getDeleteBtnIcon().attr({opacity:0});
        this.getHandles().hide();
    },

    /*
     * Stops responding to mouseovers
     */
    disable: function() {
        this.getFrontElements().unhover(this.animateDrawHoverZone, this.animateHideHoverZone);
    },

    /*
     * Starts responding to mouseovers
     */
    enable: function() {
        this.getFrontElements().hover(this.animateDrawHoverZone, this.animateHideHoverZone);
    }
});
