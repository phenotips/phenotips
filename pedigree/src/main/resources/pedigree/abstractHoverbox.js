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

    initialize: function(node, x, y, width, height, nodeX, nodeY, nodeShapes) {
        var me = this;
        this._node = node;
        this._relativeX = x;
        this._relativeY = y;
        this._nodeX = nodeX;
        this._nodeY = nodeY;
        this._width = width;
        this._height = height;
        this._isHovered = false;
        this._activeHandles = 0;
        this._orbs = editor.getPaper().set();
        this._connections = editor.getPaper().set();
        this._handles = this.generateHandles();
        this._currentHandles = this._handles;
        this._buttons = this.generateButtons();
        this._boxOnHover = editor.getPaper().rect(x, y, this._width, this._height, 5).attr(editor.attributes.boxOnHover);
        this._backElements = editor.getPaper().set(this._boxOnHover, this._connections);
        this._backElements.insertBefore(nodeShapes.flatten());
        var mask = this._boxOnHover.clone().attr({fill: 'green', opacity: 0});
        this._frontElements = editor.getPaper().set().push(mask, this._buttons, this._orbs);
        this._frontElements.insertAfter(nodeShapes.flatten());
        this._frontElements.hover(function() {me.setHovered(true)}, function() {me.setHovered(false)});
        this.animateDrawHoverZone = this.animateDrawHoverZone.bind(this);
        this.animateHideHoverZone =  this.animateHideHoverZone.bind(this);
        this.hide();
        this.enable();
    },

    getX: function() {
        return this._relativeX;
    },

    getY: function() {
        return this._relativeY;
    },

    getNodeX: function() {
        return this._nodeX;
    },

    getNodeY: function() {
        return this._nodeY;
    },

    getWidth: function() {
        return this._width;
    },

    getHeight: function() {
        return this._height;
    },

    /*
     * [Abstract] Creates the buttons used in this hoverbox. Returns a set of handles.
     */
    generateButtons: function() {
        return editor.getPaper().set();
    },

    /*
     * Returns Raphael set of the buttons in this hoverbox
     */
    getButtons: function() {
        return this._buttons;
    },

    /*
     * [Abstract] Creates the handles used in this hoverbox. Returns a set of handles.
     */
    generateHandles: function() {
        return editor.getPaper().set();
    },

    /*
     * Returns a Raphael set of the currently visible handles
     */
    getCurrentHandles: function() {
        return this._currentHandles;
    },

    /*
     * Returns the node Person or Partnership for which the hoverbox is drawn
     */
    getNode: function() {
        return this._node;
    },

    /*
     * Returns the a Raphael set containing the four draggable handles
     */
    getHandles: function() {
        return this._handles;
    },

    createButton: function(x, y, svgPath, attributes, onClick, className) {
        var iconScale = editor.attributes.radius * 0.014,
            icon = editor.getPaper().path(svgPath).attr(attributes);

        icon.transform(["t" , x , y, "s", iconScale, iconScale, 0, 0]);
        var mask = editor.getPaper().rect(icon.getBBox().x, icon.getBBox().y,
            icon.getBBox().width, icon.getBBox().height, 1);
        mask.attr({fill: 'gray', opacity: 0, "stroke-width" : 0}).transform("s1.5");
        var button = editor.getPaper().set(mask, icon);
        var clickFunct = function() {
            onClick && onClick();
            button.isClicked = !button.isClicked;
            if(button.isClicked)
                mask.attr(editor.attributes.btnMaskClick);
            else
                mask.attr(editor.attributes.btnMaskHoverOn);
        };
        button.click(clickFunct);
        button.mousedown(function(){mask.attr(editor.attributes.btnMaskClick)});
        button.hover(function() {
                mask.attr(editor.attributes.btnMaskHoverOn)
            },
            function() {
                mask.attr(editor.attributes.btnMaskHoverOff)
            });
        className && button.forEach(function(element) {
            element.node.setAttribute('class', className);
        });
        button.icon = icon;
        return button;
    },

    /**
     * Creates and returns a show-menu button. Returns Raphael set.
     */
    generateMenuBtn: function() {
        var me = this;
        var action = function() {
            me.toggleMenu(!me.isMenuToggled());
        };
        var path = "M2.021,9.748L2.021,9.748V9.746V9.748zM2.022,9.746l5.771,5.773l-5.772,5.771l2.122,2.123l7.894-7.895L4.143,7.623L2.022,9.746zM12.248,23.269h14.419V20.27H12.248V23.269zM16.583,17.019h10.084V14.02H16.583V17.019zM12.248,7.769v3.001h14.419V7.769H12.248z";
        var attributes = editor.attributes.menuBtnIcon;
        var x = this.getX() + this.getWidth() - 18 - this.getWidth()/40;
        var y = this.getY() + this.getHeight()/40;
        var className = "menu-trigger";
        return this.createButton(x, y, path, attributes, action, className);
    },

    /**
     * Creates and returns a delete button (big red X). Returns Raphael set.
     */
    generateDeleteBtn: function() {
        var me = this;
        var action = function() {
            me.getNode().remove(true);
        };
        var path = "M24.778,21.419 19.276,15.917 24.777,10.415 21.949,7.585 16.447,13.087 10.945,7.585 8.117,10.415 13.618,15.917 8.116,21.419 10.946,24.248 16.447,18.746 21.948,24.248z";
        var attributes = editor.attributes.deleteBtnIcon;
        var x = this.getX() + this.getWidth()/40;
        var y = this.getY() + this.getHeight()/40;
        return this.createButton(x, y, path, attributes, action);
    },

    /*
     * Returns the gray box that appears when the node is hovered
     */
    getBoxOnHover: function() {
        return this._boxOnHover;
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
        var path = [["M", this.getNodeX(), this.getNodeY()],["L", orbX, orbY]],
            connection = editor.getPaper().path(path).attr({"stroke-width": 4, stroke: "gray"}),
            orbRadius = editor.attributes.radius/7,
            orbHue = editor.attributes.orbHue,
            orb = generateOrb(editor.getPaper(), orbX, orbY, orbRadius*1.1, orbHue).attr("cursor", "pointer");

            var handle = editor.getPaper().set().push(connection, orb),
                hasEnded = true,
                hoverTypes = ["Person"],
            me = this;
        handle.type = type;
        connection.oPath = path;

        if(type == 'parent') {
            hoverTypes.push("Partnership")
        }

        var start = function() {
//            if(!hasEnded) {
//                //isDrag = true;
//                //end();
//                return;
//            }
            hasEnded = false;
            //document.observe('mousedown', catchRightClick);
            me.disable();
            me.getFrontElements().toFront();
            orb.ox = orb[0].attr("cx");
            orb.oy = orb[0].attr("cy");
            connection.ox = connection.oPath[1][1];
            connection.oy = connection.oPath[1][2];
            handle.isDragged = false;
            editor.getGraph().setCurrentDraggable(handle);
            editor.getGraph().enterHoverMode(me.getNode(), hoverTypes);
            me._activeHandles++;
        };
        var move = function(dx, dy) {
            dx = dx/editor.getWorkspace().zoomCoefficient;
            dy = dy/editor.getWorkspace().zoomCoefficient;
            orb.attr("cx", orb.ox + dx);
            orb.attr("cy", orb.oy + dy);
            connection.oPath[1][1] = connection.ox + dx;
            connection.oPath[1][2] = connection.oy + dy;
            connection.attr("path", connection.oPath);
            if(dx > 1 || dx < 1 || dy > 1 || dy < -1 ) {
                handle.isDragged = true;
            }
        };
        var end = function() {
//            if(hasEnded) {
//                return;
//            }
            //document.stopObserving('mousedown', catchRightClick);
            editor.getGraph().exitHoverMode();
            if(handle.isDragged){
                orb.animate({"cx": orb.ox, "cy": orb.oy}, + handle.isDragged * 1000, "elastic",
                    function() {
                        me._activeHandles--;
                        if(me._activeHandles == 0) {
                            me.enable();
                            me.animateHideHoverZone();
                            hasEnded = true;
                            handle.isDragged = false;
                        }
                    });
            }
            else {
                me._activeHandles--;
                if(me._activeHandles == 0) {
                    me.enable();
                    hasEnded = true;
                }
            }
            me.handleAction(handle.type, handle.isDragged);
            connection.oPath[1][1] = connection.ox;
            connection.oPath[1][2] = connection.oy;
            connection.animate({"path": connection.oPath},1000, "elastic");
        };

        orb.drag(move, start, end);
        orb.hover(function() {
                orb[0].attr({fill: "r(.5,.9)hsb(" + (orbHue + .36) + ", 1, .75)-hsb(" + (orbHue + .36) + ", .5, .25)", stroke: "none"});
                //orb[0].attr({fill: "r(.5,.9)hsb(" + orbHue +.7 + ", 1, .75)-hsb(" + orbHue + .7 + ", .5, .25)"})
            },
            function () {
                orb[0].attr({fill: "r(.5,.9)hsb(" + orbHue + ", 1, .75)-hsb(" + orbHue + ", .5, .25)"})
            });
        this._orbs.push(orb);
        this._connections.push(connection);

        handle.getType = function() {
            return type;
        };
        return handle;
    },

    /*
     * Hides the child handle
     */
    hideChildHandle: function() {
        this.getCurrentHandles().exclude(this._downHandle.hide());
    },

    /*
     * Unhides the child handle
     */
    unhideChildHandle: function() {
        if(this.isHovered() || this.isMenuToggled()) {
            this._downHandle.show();
        }
        (!this.getCurrentHandles().contains(this._downHandle)) && this.getCurrentHandles().push(this._downHandle);
    },

    /*
     * Fades the hoverbox graphics in
     */
    animateDrawHoverZone: function() {
        this.getNode().getGraphics().setSelected(true);
        this.getBoxOnHover().stop().animate({opacity:0.7}, 300);
        this.getButtons().forEach(function(button) {
            button.icon.stop().animate({opacity:1}, 300);
        });
        this.getCurrentHandles().show();
    },

    /*
     * Fades the hoverbox graphics out
     */
    animateHideHoverZone: function() {
        if(!this.isHovered()) {
            this.getNode().getGraphics().setSelected(false);
            this.getBoxOnHover().stop().animate({opacity:0}, 200);
            this.getButtons().forEach(function(button) {
                button.icon.stop().animate({opacity:0}, 200);
            });
            this.getCurrentHandles().hide();
        }
    },

    /*
     * Hides the hoverbox's graphical elements
     */
    hide: function() {
        this.getBoxOnHover().attr({opacity:0});
        this.getButtons().forEach(function(button) {
            button.icon.attr({opacity:0});
        });
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
    },

    remove: function() {
        this.disable();
        this.getBackElements().remove();
        this.getFrontElements().remove();
    },

    onWidgetHide: function() {
        this._isMenuToggled = false;
        !this.isHovered() && this.animateHideHoverZone();
        this.enable();
    }
});
