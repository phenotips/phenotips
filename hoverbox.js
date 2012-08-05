/*
 * Hoverbox is a class for all the UI elements and graphics surrounding an individual node and
 * its labels. This includes the box that appears around the node when it's hovered by a mouse, as
 * well as the handles used for creating connections and creating new nodes.
 *
 * @param x the x coordinate on the Raphael canvas at which the node drawing will be centered
 * @param the y coordinate on the Raphael canvas at which the node drawing will be centered
 * @param gender either 'M', 'F' or 'U' depending on the gender
 * @param id a unique numerical ID number
 */

var Hoverbox = Class.create( {

    /**
     * @param pedigree_node the Person around which the box is drawn
     * @param x the x coordinate on the Raphael canvas at which the hoverbox will be centered
     * @param y the y coordinate on the Raphael canvas at which the hoverbox will be centered
     */
    initialize: function(pedigree_node, x, y) {
        this._node = pedigree_node;
        this._relativeX = x;
        this._relativeY = y;
        this._width = editor.graphics.getRadius() * 4;
        this._isMenuToggled = false;
        var me = this;
        this._optionsBtn = this.generateMenuBtn();
        this._handles = this.generateHandles();
        this._currentHandles = this._handles;
        this._isHovered = false;
        this._isMenuToggled = false;

        var boxOnHover = editor.paper.rect(this.getX()-(this._width/2), this.getY()-(this._width/2),
            this._width, this._width, 5).attr(editor.graphics._attributes.boxOnHover);
        this._backElements =  editor.paper.set().push(boxOnHover, this.getHandles());
        var mask = editor.paper.rect(this.getX()-(this._width/2), this.getY()-(this._width/2),this._width, this._width);
        mask.attr({fill: 'gray', opacity: 0});

        this._frontElements = editor.paper.set().push(mask, this.showMenuBtn(), this.handleOrbs);
        this._frontElements.hover(function() {me.setHovered(true)}, function() {me.setHovered(false)});
        this.animateDrawHoverZone = this.animateDrawHoverZone.bind(this);
        this.animateHideHoverZone =  this.animateHideHoverZone.bind(this);
        this.hide();
        this.enable();
    },

    /*
     * Returns the Person which this hoverbox is attached to
     */
    getNode: function() {
        return this._node;
    },

    /*
     * Returns the x coordinate on the Raphael canvas at which the hoverbox centered
     */
    getX: function() {
        return this._relativeX;
    },

    /*
     * Returns the y coordinate on the Raphael canvas at which the hoverbox centered
     */
    getY: function() {
        return this._relativeY;
    },

    /*
     * TODO: get rid of this? As well as the event catcher on top
     */
    hideMenu: function() {
        editor.nodeMenu.hide();
        this.toggleMenu(!this.isMenuToggled());
        this.animateHideHoverZone();
    },

    /*
     * Returns a Raphael set of the currently visible handles
     */
    getCurrentHandles: function() {
        return this._currentHandles;
    },

    /*
     * Replaces the set of currently visible handles with newSet
     *
     * @param newSet should be a Raphael set containing handles
     */
    setCurrentHandles: function(newSet){
        this._currentHandles = newSet;
    },

    /*
     * Returns the a Raphael set containing the four draggable handles
     */
    getHandles: function() {
        return this._handles;
    },

    /*
     * Hides generates the button that shows/hides the menu
     */
    generateMenuBtn: function() {
        var me = this,
            path = "M2.021,9.748L2.021,9.748V9.746V9.748zM2.022,9.746l5.771,5.773l-5.772,5.771l2.122,2.123l7.894-7.895L4.143,7.623L2.022,9.746zM12.248,23.269h14.419V20.27H12.248V23.269zM16.583,17.019h10.084V14.02H16.583V17.019zM12.248,7.769v3.001h14.419V7.769H12.248z",
            iconX = this.getX() + editor.graphics.getRadius() * 1.45,
            iconY = this.getY() - editor.graphics.getRadius() * 1.9,
            iconScale = editor.graphics.getRadius() * 0.014,
            optionsBtnIcon = editor.paper.path(path);

        optionsBtnIcon.attr(editor.graphics._attributes.optionsBtnIcon);
        optionsBtnIcon.transform(["t" , iconX , iconY, "s", iconScale, iconScale, 0, 0]);
        var optionsBtnMask = editor.paper.rect(optionsBtnIcon.getBBox().x, optionsBtnIcon.getBBox().y,
            optionsBtnIcon.getBBox().width, optionsBtnIcon.getBBox().height, 1);
        optionsBtnMask.attr({fill: 'gray', opacity: 0}).transform("s1.5");

        optionsBtnIcon.node.setAttribute('class', 'menu-trigger');
        optionsBtnMask.node.setAttribute('class', 'menu-trigger');

        var button = editor.paper.set(optionsBtnMask, optionsBtnIcon);
        button.click(function(){
            me.toggleMenu(!me.isMenuToggled());
        });
        button.mousedown(function(){optionsBtnMask.attr(editor.graphics._attributes.optionsBtnMaskClick)});
        button.hover(function() {
                optionsBtnMask.attr(editor.graphics._attributes.optionsBtnMaskHoverOn)
            },
            function() {
                optionsBtnMask.attr(editor.graphics._attributes.optionsBtnMaskHoverOff)
            });
        return button;
    },

    /*
     * Returns the icon used for the menu button
     */
    getOptionsBtnIcon: function() {
        return this.showMenuBtn()[1];
    },
    /*
     * Returns a Raphael set containing the button for showing/hiding the menu
     */
    showMenuBtn: function() {
        return this._optionsBtn;
    },

    /*
     * Returns the gray box that appears when the Person is hovered
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
     * Returns the invisible layer in front of the hoverbox
     */
    getHoverZoneMask: function() {
        return this.getFrontElements()[0];
    },

    /*
     * Returns a Raphael set containing all hoverbox elements that are layered
     * in front of the Person graphics
     */
    getFrontElements: function() {
        return this._frontElements;
    },

    /*
     * Returns a Raphael set containing all hoverbox elements that are layered
     * behind of the Person graphics
     */
    getBackElements: function() {
        return this._backElements;
    },

    /*
     * Hides the partner and children handles
     */
    hidePartnerHandles: function() {
        var crtHandles = editor.paper.set();
        crtHandles.push(this.getHandles()[2]);
        this.getHandles()[0].hide();
        this.getHandles()[1].hide();
        this.getHandles()[3].hide();
        this.setCurrentHandles(crtHandles);
    },

    /*
     * Shows the partner and children handles
     */
    unhidePartnerHandles: function() {
        this.setCurrentHandles(this.getHandles());
        if(this.isHovered() || this.isMenuToggled()) {
            this.getHandles()[0].show();
            this.getHandles()[1].show();
            this.getHandles()[3].show();
        }
    },

    hideParentHandle: function() {
        var crtHandles = editor.paper.set();
        crtHandles.push(this.getHandles()[0], this.getHandles()[1], this.getHandles()[3]);
        this.getHandles()[2].hide();
        this.setCurrentHandles(crtHandles);
    },

    unHideParentHandle: function() {
        this.setCurrentHandles(this.getHandles());
        if(this.isHovered() || this.isMenuToggled()) {
            this.getHandles()[2].show();
        }
    },
    /*
     * Creates and returns a set with four draggable handles for creating new relatives and connections
     */
    generateHandles: function() {
        var rightPath = [["M", this.getX(), this.getY()],["L", this.getX() + (editor.graphics.getRadius() * 1.6), this.getY()]],
            leftPath = [["M", this.getX(), this.getY()],["L", this.getX() - (editor.graphics.getRadius() * 1.6), this.getY()]],
            upPath = [["M", this.getX(), this.getY()],["L", this.getX(), this.getY() - (editor.graphics.getRadius() * 1.6)]],
            downPath = [["M", this.getX(), this.getY()],["L", this.getX(), this.getY() + (editor.graphics.getRadius() * 1.6)]],

            connectionAttr = {"stroke-width": 4, stroke: "gray"},
            rightConnection = editor.paper.path(rightPath).attr(connectionAttr),
            leftConnection = editor.paper.path(leftPath).attr(connectionAttr),
            downConnection = editor.paper.path(downPath).attr(connectionAttr),
            upConnection = editor.paper.path(upPath).attr(connectionAttr),

            orbRadius = editor.graphics._attributes.orbRadius,
            orbHue = editor.graphics._attributes.orbHue,
            upCirc = editor.graphics.generateOrb(upPath[1][1], upPath[1][2], orbRadius, orbHue),
            downCirc = editor.graphics.generateOrb(downPath[1][1], downPath[1][2], orbRadius, orbHue),
            leftCirc = editor.graphics.generateOrb(leftPath[1][1], leftPath[1][2], orbRadius, orbHue),
            rightCirc = editor.graphics.generateOrb(rightPath[1][1], rightPath[1][2], orbRadius, orbHue),

            rightHandle = editor.paper.set().push(rightConnection, rightCirc),
            leftHandle = editor.paper.set().push(leftConnection, leftCirc),
            upHandle = editor.paper.set().push(upConnection, upCirc),
            downHandle = editor.paper.set().push(downConnection, downCirc);

        rightHandle.type = leftHandle.type = 'partner';
        upHandle.type = 'parent';
        downHandle.type = 'child';


        rightConnection.oPath = rightPath;
        leftConnection.oPath = leftPath;
        upConnection.oPath = upPath;
        downConnection.oPath = downPath;
        this.enable =  this.enable.bind(this);
        this.disable =  this.disable.bind(this);
        this.handleOrbs = editor.paper.set().push(rightCirc, leftCirc, upCirc, downCirc).attr("cursor", "pointer");
        var me = this,
            orb,
            connection,
            isDrag,
            movingHandles = 0;

        var hasEnded = true;

        var start = function(handle) {
            if(!hasEnded) {
                //isDrag = true;
                //end();
                return;
            }
            hasEnded = false;
            //           document.observe('mousedown', catchRightClick);
//            editor.zpd.opts['zoom'] = false;
//            editor.zpd.opts['pan'] = false;
            me.disable();
            orb = handle[1];
            connection = handle[0];
            me.getNode().getGraphics().getAllGraphics().toFront();
            orb.ox = orb[0].attr("cx");
            orb.oy = orb[0].attr("cy");
            connection.ox = connection.oPath[1][1];
            connection.oy = connection.oPath[1][2];
            movingHandles++;
            editor.currentDraggable.node = me.node;
            editor.currentDraggable.handle = handle.type;
            isDrag = false;
            editor.enterHoverMode(me.getNode());
        };
        var move = function(dx, dy) {
            orb.attr("cx", orb.ox + dx);
            orb.attr("cy", orb.oy + dy);
            connection.oPath[1][1] = connection.ox + dx;
            connection.oPath[1][2] = connection.oy + dy;
            connection.attr("path", connection.oPath);
            if(dx > 5 || dx < -5 || dy > 5 || dy < -5 ) {
                isDrag = true;
            }
        };
        var end = function() {
            if(hasEnded) {
                return;
            }
            //document.stopObserving('mousedown', catchRightClick);
//            editor.zpd.opts['zoom'] = true;
//            editor.zpd.opts['pan'] = true;
            orb.animate({"cx": orb.ox, "cy": orb.oy}, +isDrag * 1000, "elastic",
                function() {
                    movingHandles--;
                    if(movingHandles == 0) {
                        me.enable();
                        me.animateHideHoverZone();
                        hasEnded = true;
                    }
                });
            editor.exitHoverMode();
            me.handleAction(editor.currentDraggable.handle, isDrag);

            connection.oPath[1][1] = connection.ox;
            connection.oPath[1][2] = connection.oy;
            connection.animate({"path": connection.oPath},1000, "elastic");
            isDrag = false;


        };

        rightCirc.drag(move, function() {start(rightHandle)},end);
        leftCirc.drag(move, function() {start(leftHandle)},end);
        upCirc.drag(move, function() {start(upHandle)},end);
        downCirc.drag(move, function() {start(downHandle)},end);

        return editor.paper.set().push(rightHandle, leftHandle, upHandle, downHandle);
    },

    /*
     * Performs the appropriate action for clicking on the handle of type handleType
     *
     * @param handleType can be either "child", "partner" or "parent"
     */
    handleAction : function(handleType, isDrag)
    {
        if(isDrag) {
            if(editor.currentHoveredNode && editor.currentHoveredNode.validPartnerSelected) {
                editor.currentHoveredNode.validPartnerSelected = false;
                this.getNode().addPartner(editor.currentHoveredNode);
            }
            else if(editor.currentHoveredNode && editor.currentHoveredNode.validChildSelected) {
                if(this.getNode().getChildren().indexOf(editor.currentHoveredNode) == -1) {
                    editor.currentHoveredNode.validChildSelected = false;
                    var partnership = this.getNode().createPartner(true);
                    partnership.addChild(editor.currentHoveredNode);
                }
            }
            else if(editor.currentHoveredNode && editor.currentHoveredNode.validParentSelected) {
                editor.currentHoveredNode.validParentSelected = false;
                var partnership = editor.currentHoveredNode.createPartner(true);
                partnership.addChild(this.getNode());
            }
            editor.currentHoveredNode = null;
        }
        else {
            if(handleType == "partner") {
                var partnership = this.getNode().createPartner(false);
                partnership.createChild(true);
            }
            else if(handleType == "child") {
                var partnership = this.getNode().createPartner(true);
                partnership.createChild(false);
            }
            else if(handleType == "parent") {
                this.getNode().createParents();
            }
            editor.currentHoveredNode && editor.currentHoveredNode.getGraphics().getHoverBox().getBoxOnHover().attr(editor.graphics._attributes.boxOnHover);
            editor.currentDraggable.node = null;
            editor.currentDraggable.handle = null;
        }
    },

    /*
     * Draws the hoverbox with a "fade in" animation effect
     */
    animateDrawHoverZone: function() {
        this.getNode().getGraphics().setSelected(true);
        this.getBoxOnHover().stop().animate({opacity:0.7}, 300);
        this.getOptionsBtnIcon().stop().animate({opacity:1}, 300);
        this.getCurrentHandles().show();
    },

    /*
     * Hides the hover box elements with a "fade out" animation effect
     */
    animateHideHoverZone: function() {
        if(!this.isMenuToggled() && !this.isHovered()) {
            this.getNode().getGraphics().setSelected(false);
            this.getBoxOnHover().stop().animate({opacity:0}, 200);
            this.getOptionsBtnIcon().stop().animate({opacity:0}, 200);
            this.getCurrentHandles().hide();
        }
    },

    /*
     * Returns true if the menu for this node is open
     */
    isMenuToggled: function() {
        return this._isMenuToggled;
    },

    /*
     * Shows/hides the menu for this node
     */
    toggleMenu: function(isMenuToggled) {
        this._isMenuToggled = isMenuToggled;
        if(isMenuToggled) {
            this.disable();
            var optBBox = this.getBoxOnHover().getBBox();
            var x = optBBox.x2;
            var y = optBBox.y;
//            editor.zpd.opts['pan'] = false;
//            editor.zpd.opts['zoom'] = false;
            editor.nodeMenu.show(this.getNode(), x+5, y);
        }
        else {
            //this.enable();
//            editor.zpd.opts['pan'] = true;
//            editor.zpd.opts['zoom'] = true;
            editor.nodeMenu.hide();
        }
    },

    /*
     * Hides the hover zone without a fade out animation
     */
    hide: function() {
        this.getBoxOnHover().attr({opacity:0});
        this.getOptionsBtnIcon().attr({opacity:0});
        this._handles.hide();
    },

    /*
     * Stops responding to mouseovers
     */
    disable: function() {
        this._frontElements.unhover(this.animateDrawHoverZone, this.animateHideHoverZone);
    },

    /*
     * Starts responding to mouseovers
     */
    enable: function() {
        this._frontElements.hover(this.animateDrawHoverZone, this.animateHideHoverZone);
    }
});
