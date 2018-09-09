/**
 * AbstractHoverbox is an abstract class for all the UI elements and graphics surrounding a node on the canvas (a Person
 * or a partnership). This includes the box that appears around the node when it's hovered by a mouse, as
 * well as the handles used for creating connections and creating new nodes.
 *
 * @class AbstractHoverbox
 * @constructor
 * @param {AbstractNode} node The node Person or Partnership for which the hoverbox is drawn
 * @param {Number} x The x coordinate for the hoverbox
 * @param {Number} y The y coordinate for the hoverbox
 * @param {Number} width The width in pixels
 * @param {Number} height The height in pixels
 * @param {Number} nodeX The x coordinate of the node for which the hoverbox is drawn
 * @param {Number} nodeY The y coordinate of the node for which the hoverbox is drawn
 * @param {Raphael.st} nodeShapes Raphaël set containing the graphical elements that make up the node
 */
define([
        "pedigree/pedigreeEditorParameters",
        "pedigree/model/helpers",
        "pedigree/view/graphicHelpers"
    ], function(
        PedigreeEditorParameters,
        Helpers,
        GraphicHelpers
    ){
    var AbstractHoverbox = Class.create({

        initialize: function(node, shiftX, shiftY, width, height, nodeX, nodeY, nodeShapes) {
            //var timer = new Helpers.Timer();
            var me = this;
            this._node = node;
            this._relativeX = shiftX;
            this._relativeY = shiftY;
            this._nodeX     = nodeX;
            this._nodeY     = nodeY;
            this._hidden    = true;
            this._enabled   = false;
            this._width     = width;
            this._height    = height;
            this._baseHeight = height;
            this._isHovered = false;
            this._currentHandles = null;
            this._currentOrbs    = null;
            this._currentButtons = null;
            this._handlesZoomSz  = null;
            this._boxOnHover     = editor.getPaper().rect(this.getX(), this.getY(), this._width, this._height, 5).attr(PedigreeEditorParameters.attributes.boxOnHover);
            this._boxOnHover.node.setAttribute("class","pedigree-hoverbox");
            this._backElements  = editor.getPaper().set(this._boxOnHover);
            this._mask          = this._boxOnHover.clone().attr({fill: 'green', opacity: 0});
            this._frontElements = editor.getPaper().set().push(this._mask);

            var nodeShapeSet = nodeShapes.flatten();
            this._backElements.insertBefore(nodeShapeSet);
            this._frontElements.insertAfter(nodeShapeSet);

            this.setDoNotHideHoverbox(this._backElements);
            this.setDoNotHideHoverbox(this._frontElements);

            this.animateDrawHoverZone = this.animateDrawHoverZone.bind(this);
            this.animateHideHoverZone = this.animateHideHoverZone.bind(this);
            // hide initially
            this.getBoxOnHover().attr({opacity:0});
            this.enable();
            //timer.printSinceLast("=== abstract hoverbox runtime: ");
            this._isMenuToggled  = false;
            this._justClosedMenu = false;
        },

        /**
         * Returns the x coordinate of the hoverbox
         *
         * @method getX
         * @return {Number} The x coordinate in pixels
         */
        getX: function() {
            return this.getNodeX() + this._relativeX;
        },

        /**
         * Returns the y coordinate of the hoverbox
         *
         * @method getY
         * @return {Number} The y coordinate in pixels
         */
        getY: function() {
            return this.getNodeY() + this._relativeY;
        },

        /**
         * Returns the x coordinate of the attached node
         *
         * @method getNodeX
         * @return {Number} The x coordinate in pixels
         */
        getNodeX: function() {
            // note: during construction getGraphics() isnot yet available, so need to store nodeX.
            //       however node may have been moved later, in which case we need to use current graphics X
            var nodeGraphics = this.getNode().getGraphics();
            if (nodeGraphics)
                this._nodeX = nodeGraphics.getX();
            return this._nodeX;
        },

        /**
         * Returns the y coordinate of the attached node
         *
         * @method getNodeY
         * @return {Number} The y coordinate in pixels
         */
        getNodeY: function() {
            var nodeGraphics = this.getNode().getGraphics();
            if (nodeGraphics)
                this._nodeY = nodeGraphics.getY();
            return this._nodeY;
        },

        /**
         * Returns the width of the hoverbox
         *
         * @method getWidth
         * @return {Number} The width in pixels
         */
        getWidth: function() {
            return this._width;
        },

        /**
         * Returns the height of the hoverbox
         *
         * @method getHeight
         * @return {Number} The height in pixels
         */
        getHeight: function() {
            return this._height;
        },

        /**
         * Returns the node for which the hoverbox is drawn
         *
         * @method getNode
         * @return {AbstractNode} Can be either a Partnership or a Person
         */
        getNode: function() {
            return this._node;
        },

        /**
         * Returns the height of hover box extension at the bottom (e.g. specifically below the node))
         *
         * @method getBottomExtensionHeight
         * @return {int} 0 by default
         */
        getBottomExtensionHeight: function() {
            return 0;
        },

        /**
         * Creates the buttons used in this hoverbox
         *
         * @method generateButtons
         * @return {Raphael.st} A set of buttons
         */
        generateButtons: function() {
            if (this._currentButtons !== null) return;
            this._currentButtons = [];
        },

        regenerateButtons: function() {
            this.removeButtons();
            this.generateButtons();
        },

        removeButtons: function () {
            if (!this._currentButtons) return;

            var enableState = this._enabled;

            enableState && this.disable();
            for (var i = 0; i < this._currentButtons.length; i++) {
                this.getFrontElements().exclude(this._currentButtons[i]);
                this._currentButtons[i].remove();
            }
            this._currentButtons = null;
            enableState && this.enable();
        },

        hideButtons: function() {
            if (!this._currentButtons) return;
            for (var i = 0; i < this._currentButtons.length; i++) {
                if (this._currentButtons[i].hasOwnProperty("icon")) {
                    this._currentButtons[i].icon.stop();
                }
                if (this._currentButtons[i].hasOwnProperty("mask")) {
                    this._currentButtons[i].mask.attr(PedigreeEditorParameters.attributes.btnMaskHoverOff);
                }
                this._currentButtons[i].hide();
            }
        },

        showButtons: function() {
            if (!this._currentButtons) return;
            for (var i = 0; i < this._currentButtons.length; i++) {
                this._currentButtons[i].show();
            }
        },

        /**
         * Returns Raphael set of the buttons in this hoverbox
         *
         * @method getCurrentButtons
         * @return {Raphael.st} A set of buttons
         */
        getCurrentButtons: function() {
            return this._currentButtons;
        },

        /**
         * Removes all handles currently used in this hoverbox
         *
         * @method removeHandles
         */
        removeHandles: function () {
            if (!this._currentHandles) return;

            var enableState = this._enabled;
            enableState && this.disable();
            for (var i = 0; i < this._currentOrbs.length; i++)
                this.getFrontElements().exclude(this._currentOrbs[i]);
            this._currentOrbs = null;
            enableState && this.enable();

            for (var i = 0; i < this._currentHandles.length; i++)
                this._currentHandles[i].remove();
            this._currentHandles = null;
        },

        hideHandles: function() {
            if (!this._currentHandles) return;
            for (var i = 0; i < this._currentHandles.length; i++)
                this._currentHandles[i].hide();
        },

        showHandles: function() {
            if (!this._currentHandles) return;
            for (var i = 0; i < this._currentHandles.length; i++)
                this._currentHandles[i].show();
        },

        /**
         * Creates the handles used in this hoverbox. Returns a list of handles
         *
         * @method generateHandles
         */
        generateHandles: function() {
            if (this._currentHandles !== null) return;
            this._currentHandles = [];
            this._currentOrbs    = [];
            this._handlesZoomSz  = editor.getWorkspace().getCurrentZoomLevel();
        },

        /**
         * Iff handles are present, removes all and creates new set of handles
         *
         * @method regenerateHandles
         */
        regenerateHandles: function() {
            if (this._currentHandles)
                this.removeHandles();
            if (!this._hidden || this.isMenuToggled())
                this.generateHandles();
        },

        setDoNotHideHoverbox: function(raphaelElement) {
            // raphaelElement is either a set which has .forEach() defined (but has no .node),
            // or a single element which has .node but not .forEach()
            if (raphaelElement.node) {
                if (!Helpers.elementHasClassName(raphaelElement.node, 'donothidehoverbox')) {
                    var existingClasses = raphaelElement.node.getAttribute('class') ? (" " + raphaelElement.node.getAttribute('class')) : "";
                    raphaelElement.node.setAttribute('class', 'donothidehoverbox' + existingClasses);
                    // a hack for raphael <tspan> complication when using SVG (on most modern browsers)
                    if (raphaelElement.node.children &&
                        raphaelElement.node.children.length > 0 &&
                        raphaelElement.node.children[0].nodeName == "tspan") {
                        raphaelElement.node.children[0].setAttribute("class","donothidehoverbox");
                    }
                }
            } else {
                var _this = this;
                raphaelElement.forEach(function(element) {
                    _this.setDoNotHideHoverbox(element);
                });
            }
        },

        /**
         * Generates a button and places it on the hoverbox
         *
         * @method createButton
         * @param {Number} x The x coordinate of the button
         * @param {Number} y The y coordinate of the button
         * @param {String|Array} svgPath The svg path for the button (correctly scaled)
         * @param {Object} svgPathBBox The BBox for the svg path. Precomputed for performance reasons
         * @param attributes The svg attributes
         * @param {Function} onClick Callback for the button
         * @param {String} className The class attribute for the button
         * @param {Function} onMouseDown Action to be called when mouse button is pressed - this is different from onClick, since
         *                               this action is performed before the button is released, allowing handling of dragging, etc.
         * @return {Raphael.st} The generated button
         */
        createButton: function(x, y, svgPath, svgPathBBox, attributes, onClick, className, title, onMouseDown) {
            var icon = editor.getPaper().path(svgPath).attr(attributes);
            if (title) {
                icon.attr({"title": title});
            }
            icon.transform(["t" , x , y]);

            // manually compute the size of the mask because Raphael.transform() is exptremely slow
            var xShift    = svgPathBBox.width/4;
            var yShift    = svgPathBBox.height/4;
            var newWidth  = svgPathBBox.width  * 1.5;
            var newHeight = svgPathBBox.height * 1.5;
            var mask = editor.getPaper().rect(x + svgPathBBox.x - xShift, y + svgPathBBox.y - yShift, newWidth, newHeight, 1);
            mask.attr({fill: 'gray', opacity: 0, "stroke-width" : 0});

            var button = editor.getPaper().set(mask, icon).toFront();

            var me = this;

            if (onClick) {
                var clickFunct = function() {
                    if (me._hidden) {
                        button.isClicked = false;
                        return;
                    }
                    button.isClicked = !button.isClicked;
                    if(button.isClicked) {
                        mask.attr(PedigreeEditorParameters.attributes.btnMaskClick);
                    }
                    else {
                        mask.attr(PedigreeEditorParameters.attributes.btnMaskHoverOn);
                    }
                    onClick();
                };
                button.click(clickFunct);
            }

            if (onMouseDown) {
                button.mousedown(onMouseDown);
            } else {
                button.mousedown(function(){mask.attr(PedigreeEditorParameters.attributes.btnMaskClick)});
            }

            button.hover(function() {
                    //console.log("button hover");
                    mask.attr(PedigreeEditorParameters.attributes.btnMaskHoverOn);
                    if (title)
                        mask.attr({"title": title});
                },
                function() {
                    //console.log("button unhover");
                    mask.attr(PedigreeEditorParameters.attributes.btnMaskHoverOff);
                });
            className && button.forEach(function(element) {
                element.node.setAttribute('class', className);
            });
            this.setDoNotHideHoverbox(button);
            button.icon = icon;
            button.mask = mask;
            if (this._hidden && !this.isMenuToggled())
                button.hide();

            this._currentButtons.push(button);
            this.disable();
            this.getFrontElements().push(button);
            this.enable();
        },

        /**
         * Creates a show-menu button
         *
         * @method generateMenuBtn
         * @return {Raphael.st} The generated button
         */
        generateMenuBtn: function() {
            throw "Not implemented";
        },

        /**
         * Creates and returns a delete button (big red X).
         *
         * @method generateDeleteBtn
         * @return {Raphael.st} the generated button
         */
        generateDeleteBtn: function() {
            var me = this;
            var action = function() {
                me.animateHideHoverZone();
                var event = { "nodeID": me.getNode().getID() };
                document.fire("pedigree:node:remove", event);
            };
            var attributes = PedigreeEditorParameters.attributes.deleteBtnIcon;
            var x = this.getX() + this.getWidth() - 20 - this.getWidth()/40;
            var y = this.getY() + 5;
            this.createButton(x, y, editor.getView().__deleteButton_svgPath, editor.getView().__deleteButton_BBox,
                              attributes, action, "delete", "Remove this person");
        },

        /**
         * Returns the gray box that appears when the node is hovered
         *
         * @method getBoxOnHover
         * @return {Raphael.el} Raphael rectangle element
         */
        getBoxOnHover: function() {
            return this._boxOnHover;
        },

        /**
         * Returns true box if the hoverbox is currently hovered
         *
         * @method isHovered
         * @return {Boolean} Raphael rectangle element
         */
        isHovered: function() {
            return this._isHovered;
        },

        /**
         * Sets the hovered property to isHovered.
         * @method setHovered
         * @param {Boolean} isHovered Set to true if the box is hovered
         */
        setHovered: function(isHovered) {
            this._isHovered = isHovered;
        },

        /**
         * Enbales or disables the highlighting of the node
         * @method setHighlighted
         * @param {Boolean} isHighlighted Set to true enables green highlight box, false disables it
         */
        setHighlighted: function(isHighlighted) {
            if (isHighlighted) {
                this.getBoxOnHover().attr({'height': this._baseHeight});
                this.getBoxOnHover().attr(PedigreeEditorParameters.attributes.boxOnHover);
                this.getBoxOnHover().attr("fill", PedigreeEditorParameters.attributes.hoverboxHighlightColor);
            }
            else {
                this.getBoxOnHover().attr({'height': this._height});
                this.getBoxOnHover().attr(PedigreeEditorParameters.attributes.boxOnHover).attr('opacity', 0);
            }
        },

        /**
         * Returns the invisible mask layer in front of the hoverbox
         *
         * @method getHoverZoneMask
         * @return {Raphael.el} Raphael rectangle
         */
        getHoverZoneMask: function() {
            return this._mask;
        },

        /**
         * Returns a Raphael set containing all hoverbox elements that are layered
         * in front of the node graphics
         *
         * @method getFrontElements
         * @return {Raphael.st} set of Raphael elements
         */
        getFrontElements: function() {
            return this._frontElements;
        },

        /**
         * Returns a Raphael set containing all hoverbox elements that are layered
         * behind of the node graphics
         *
         * @method getBackElements
         * @return {Raphael.st} set of Raphael elements
         */
        getBackElements: function() {
            return this._backElements;
        },

        /**
         * Creates a handle with a blue orb from the center of the node and places it behind the node icon
         *
         * @method generateHandle
         * @param {String} type Should be 'parent', 'child' or 'partner'
         * @param {Number} orbX The x coordinate of the orb
         * @param {Number} orbY The y coordinate of the orb
         * @return {Raphael.st} Raphael set of elements that make up the handle
         */
        generateHandle: function(type, startX, startY, orbX, orbY, title, orbShapeGender, toHide) {
            if (!orbShapeGender)
                orbShapeGender = "F";
            var strokeWidth = editor.getWorkspace().getSizeNormalizedToDefaultZoom(PedigreeEditorParameters.attributes.handleStrokeWidth);
            var path = [["M", startX, startY],["L", orbX, orbY]];
            var connection   = editor.getPaper().path(path).attr({"stroke-width": strokeWidth, stroke: "gray"}).toBack();
            connection.oPath = path;

            var touchPresent  = "createTouch" in document;

            var orbRadius     = touchPresent ? PedigreeEditorParameters.attributes.touchOrbRadius : PedigreeEditorParameters.attributes.orbRadius;
            var orbHue        = PedigreeEditorParameters.attributes.orbHue;

            var normalOrbAttr   = (orbShapeGender != "F") ? {fill: "0-hsb(" + orbHue + ", 1, .75)-hsb(" + orbHue + ", .5, .25)", stroke: "#555", "stroke-width": "0.75"}
                                                          : {fill: "r(.5,.9)hsb(" + orbHue + ", 1, .75)-hsb(" + orbHue + ", .5, .25)", stroke: "none"};
            var selectedOrbAttr = (orbShapeGender != "F") ? {fill: "0-hsb(" + (orbHue + .36) + ", 1, .75)-hsb(" + (orbHue + .36) + ", .5, .25)"}
                                                          : {fill: "r(.5,.9)hsb(" + (orbHue + .36) + ", 1, .75)-hsb(" + (orbHue + .36) + ", .5, .25)"};
            var orbAttrX        = (orbShapeGender != "F") ? "x" : "cx";
            var orbAttrY        = (orbShapeGender != "F") ? "y" : "cy";

            var orb = GraphicHelpers.generateOrb(editor.getPaper(), orbX, orbY, orbRadius*1.1, orbShapeGender).attr("cursor", "pointer");
            orb[0].attr(normalOrbAttr);

            var handle  = editor.getPaper().set().push(connection, orb);
            handle.type = type;
            connection.insertBefore(this.getHoverZoneMask());
            orb.toFront();

            var me = this;
            var inHoverMode = false;
            var interactionStarted = false;

            var onDragHandle = function() {
                if (!inHoverMode) {
                    //console.log("on drag");
                    inHoverMode = true;
                    if (editor.getView().getCurrentDraggable() !== null)
                        editor.getView().enterHoverMode(me.getNode(), type);
                    toHide && toHide.hide();
                }
            };

            // is true when any button other than the left mouse button is presses
            var wrongClick = false;
            var start = function(x,y,e) {
                if (interactionStarted) return;
                interactionStarted = true;

                //console.log("handle: start: " + e.button);
                wrongClick = false;
                if (e.button != 0) {
                    interactionStarted = false;
                     wrongClick = true;
                     return;
                }
                connection.toFront();
                orb.stop();
                orb.toFront();
                inHoverMode = false;
                me.disable();
                //me.getFrontElements().toFront();
                if (!orb.ot) {
                    orb.ot = orb[0].transform();
                    orb.ox = orb[0].attr(orbAttrX);
                    orb.oy = orb[0].attr(orbAttrY);
                } else {
                    // revert to base transformation if next click started while "end" animation was still running
                    orb.transform("");
                    orb.attr(orbAttrX, orb.ox);
                    orb.attr(orbAttrY, orb.oy);
                    orb.transform(orb.ot);
                }
                connection.ox = connection.oPath[1][1];
                connection.oy = connection.oPath[1][2];
                handle.isDragged = false;
                editor.getView().setCurrentDraggable(me.getNode().getID());
                // highlight valid targets (after a small delay - so that nothing gets annoyingly highlighted
                // and instantly un-highlighted if the person just clicks the orb without dragging)
                setTimeout(onDragHandle, 100);
            };
            var move = function(dx, dy) {
                if (wrongClick) return;
                if (!interactionStarted) return;
                //console.log("handle: move");
                onDragHandle();
                dx = dx/editor.getWorkspace().zoomCoefficient;
                dy = dy/editor.getWorkspace().zoomCoefficient;
                (orb.ot.length > 0) && orb.transform("");
                orb.attr(orbAttrX, orb.ox + dx);
                orb.attr(orbAttrY, orb.oy + dy);
                (orb.ot.length > 0) && orb.transform(orb.ot);
                connection.oPath[1][1] = connection.ox + dx;
                connection.oPath[1][2] = connection.oy + dy;
                connection.attr("path", connection.oPath);
                if(dx > 1 || dx < -1 || dy > 1 || dy < -1 ) {
                    handle.isDragged = true;
                }
                //console.log("currentHover: " + editor.getView()._currentHoveredNode + ", currentDrag: " + editor.getView()._currentDraggable);
            };
            var end = function() {
                inHoverMode        = false;
                interactionStarted = false;
                if (wrongClick) return;

                var curHoveredId = editor.getView().getCurrentHoveredNode();

                editor.getView().setCurrentDraggable(null);
                editor.getView().exitHoverMode();

                if(handle.isDragged) {
                    if (orb.ot.length == 0) {
                        var finalPosition = {};
                        finalPosition[orbAttrX] = orb.ox;
                        finalPosition[orbAttrY] = orb.oy;
                        orb.animate(finalPosition, 1000, "elastic", function() {});
                    }
                    else {
                        // animation for shapes with transformations (movement and animation via transform() could have been
                        // used in all cases, but works noticeably slower than plain coordinate manipulation in some browsers)
                        var dx = orb.ox - orb[0].attr(orbAttrX);
                        var dy = orb.oy - orb[0].attr(orbAttrY);
                        orb.animate( {"transform": "T" + dx + "," + dy + "R45"}, 1000, "elastic", function() {
                            orb.transform("");
                            orb.attr(orbAttrX, orb.ox);
                            orb.attr(orbAttrY, orb.oy);
                            orb.transform(orb.ot); });
                    }
                }

                console.log("handle.isDragged: " + handle.isDragged + ", currentHover: " + curHoveredId);
                connection.oPath[1][1] = connection.ox;
                connection.oPath[1][2] = connection.oy;
                connection.animate({"path": connection.oPath}, 1000, "elastic");
                orb[0].attr(normalOrbAttr);
                connection.insertBefore(me.getHoverZoneMask());

                me.enable();

                if (!handle.isDragged || curHoveredId != null)
                    me.handleAction(handle.type, handle.isDragged, curHoveredId);
            };

            orb.drag(move, start, end);
            orb.hover(function() {
                    //console.log("orbon hover");
                    orb[0].attr(selectedOrbAttr);
                    if (title) {
                        orb[0].attr({"title": title});
                        orb[1].attr({"title": title});
                    }
                },
                function () {
                     orb[0].attr(normalOrbAttr);
                });

            this._currentOrbs.push(orb[0]);
            this._currentOrbs.push(orb[1]);
            this.disable();
            //this.getFrontElements().forEach(function(el) { console.log("o"); });
            //console.log("Orb: " + orb);
            this.getFrontElements().push(orb[0]);
            this.getFrontElements().push(orb[1]);
            //this.getFrontElements().forEach(function(el) { console.log("*"); });
            this.enable();

            //handle.getType = function() {
            //    return type;
            //};
            return handle;
        },

        /**
         * Returns true if the menu for this node is open
         *
         * @method isMenuToggled
         * @return {Boolean}
         */
        isMenuToggled: function() {
            return false;
        },

        /*
         * Fades the hoverbox graphics in
         *
         * @method animateDrawHoverZone
         */
        animateDrawHoverZone: function(event, x, y) {
            if (editor.getView().getCurrentDraggable() !== null) return; // do not redraw when dragging
            this._hidden = false;
            //console.log("node: " + this.getNode().getID() + " -> show HB");

            this.getNode().getGraphics().setSelected(true);
            this.getBoxOnHover().stop().animate({opacity:0.7}, 200);

            this.generateButtons();
            this.showButtons();
            this.getCurrentButtons().forEach(function(button) {
                if (button.hasOwnProperty("icon")) {
                    button.icon.stop().animate({opacity:1}, 200);
                }
            });

            if (this._handlesZoomSz  != editor.getWorkspace().getCurrentZoomLevel()) {
                this.removeHandles();
            }
            this.generateHandles();
            this.showHandles();
        },

        /**
         * Fades the hoverbox graphics out
         *
         * @method animateHideHoverZone
         */
        animateHideHoverZone: function(event, x, y) {
            if (editor.getView().getCurrentDraggable() !== null) return; // do not hide when dragging

            // some older browsers (IE9, IE8) may represent classes of a node differently, and/or may not have event.relatedTarget defined,
            // instead of checking all the conditions it is easier to just use what we need, and do not apply special logic on failure
            try {
                if (event && Helpers.elementHasClassName(event.relatedTarget, "donothidehoverbox")) {
                    // make sure the cursor is within this node's hoverbox, not some other node's
                    // (which may also have "donothide" attribute)
                    var hoverarea = this.getFrontElements().getBBox();
                    var div = editor.getWorkspace().viewportToDiv(x,y);
                    var click = editor.getWorkspace().divToCanvas(div.x,div.y);
                    if (click.x > hoverarea.x && click.x < hoverarea.x2 &&
                        click.y > hoverarea.y && click.y < hoverarea.y2) {
                        return;
                    }
                }
            } catch (err) {
                // do nothing on purpose: skip special "do not hide to avoid flicker" logic
                // to make basic hide/show hoverbox functionality reliable
            }

            this._hidden = true;

            this.getNode().getGraphics().setSelected(false);
            this.getBoxOnHover().stop().animate({opacity:0}, 200);

            this.hideButtons();
            //this.removeHandles();  // does not work, since hideHoverZone is triggered when a mouse is over an orb (to fix?)
            this.hideHandles();
        },

        /**
         * Stops the hoverbox from responding to mouseovers
         *
         * @method disable
         */
        disable: function() {
            //console.log("disable HB");
            this._enabled = false;
            this.getFrontElements().unhover(this.animateDrawHoverZone, this.animateHideHoverZone);
        },

        /**
         * Attaches onMouseOver behavior to the hoverbox
         *
         * @method enable
         */
        enable: function() {
            this._enabled = true;
            //console.log("enable HB");
            //this.getFrontElements().forEach(function(el) { console.log("."); });
            this.getFrontElements().hover(this.animateDrawHoverZone, this.animateHideHoverZone);
        },

        /**
         * Deletes the hoverbox
         *
         * @method remove
         */
        remove: function() {
            this.disable();
            this.removeButtons();
            this.removeHandles();
            this.getBackElements().remove();
            this.getFrontElements().remove();
        },

        /**
         * Updates the hoverbox behavior after a widget (like the menu) is closed
         *
         * @method onWidgetHide
         */
        onWidgetHide: function() {
            this._isMenuToggled  = false;
            this._isDeceasedToggled = false;
            // prevent menu from closing and opening right away upon a click on the menu button while menu is open
            this._justClosedMenu = true;
            var me = this;
            setTimeout(function() { me._justClosedMenu = false; }, 100);

            if (this._hidden)
                this.animateHideHoverZone();
            else
                this.animateDrawHoverZone();
        },

        onWidgetShow: function() {
            this._isMenuToggled = true;
        }
    });
    return AbstractHoverbox;
});
