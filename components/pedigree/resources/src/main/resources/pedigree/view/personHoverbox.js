/**
 * PersonHoverbox is a class for all the UI elements and graphics surrounding a Person node and
 * its labels. This includes the box that appears around the node when it's hovered by a mouse, as
 * well as the handles used for creating connections and creating new nodes.
 *
 * @class PersonHoverbox
 * @extends AbstractHoverbox
 * @constructor
 * @param {Person} personNode The person for whom this hoverbox is being drawn.
 * @param {Number} centerX The X coordinate for the center of the hoverbox
 * @param {Number} centerY The Y coordinate for the center of the hoverbox
 * @param {Raphael.st} nodeShapes All shapes associated with the person node
 * @param {Number} baseHeight The base height of the hoverbox
 */
define([
        "pedigree/pedigreeEditorParameters",
        "pedigree/model/helpers",
        "pedigree/view/abstractHoverbox"
    ], function(
        PedigreeEditorParameters,
        Helpers,
        AbstractHoverbox
    ){
    var PersonHoverbox = Class.create(AbstractHoverbox, {

        initialize: function($super, personNode, centerX, centerY, nodeShapes, nodeMenu) {
            var width  = PedigreeEditorParameters.attributes.personHoverBoxWidth;
            // note: at this point pedigree node is not yet initialized, so hoverbox size can not depend on node properties
            var height = PedigreeEditorParameters.attributes.personHoverBoxHeight;
            $super(personNode, -width/2, -height/2, width, height, centerX, centerY, nodeShapes);
            this._nodeMenu = nodeMenu;
        },

        _updateHoverBoxHeight: function()
        {
            this._height = this._computeHoverBoxHeight();
            this.getBoxOnHover().attr({'height': this._height});
            this.getHoverZoneMask().attr({'height': this._height});
        },

        _computeHoverBoxHeight: function()
        {
            return this._baseHeight + this.getBottomExtensionHeight();
        },

        /**
         * Returns the height of hover box extension at the bottom, which for person nodes is used to display A&W section.
         *
         * @method getBottomExtensionHeight
         * @return {int} 0 by default or PedigreeEditorParameters.attributes.personHoverBoxAWExtensionHeight
         *               if we display alive and well radio buttons
         */
        getBottomExtensionHeight: function() {
            if (this.getNode().getLifeStatus() == "alive" || this.getNode().getLifeStatus() == "deceased") {
                return PedigreeEditorParameters.attributes.personHoverBoxAWExtensionHeight;
            } else {
                return 0;
            }
        },

        /**
         * Creates the handles used in this hoverbox
         *
         * @method generateHandles
         * @return {Raphael.st} A set of handles
         */
        generateHandles: function($super) {
            if (this._currentHandles !== null) return;
            $super();

            //var timer = new Helpers.Timer();

            var x          = this.getNodeX();
            var y          = this.getNodeY();
            var node       = this.getNode();
            var nodeShapes = node.getGraphics().getGenderGraphics().flatten();

            editor.getPaper().setStart();

            if (PedigreeEditorParameters.attributes.newHandles) {
                var strokeWidth = editor.getWorkspace().getSizeNormalizedToDefaultZoom(PedigreeEditorParameters.attributes.handleStrokeWidth);

                var partnerGender = 'U';
                if (node.getGender() == 'F') partnerGender = 'M';
                if (node.getGender() == 'M') partnerGender = 'F';

                // static part (2 lines: going above the node + going to the right)
                var splitLocationY = y-PedigreeEditorParameters.attributes.personHandleBreakY-4;
                var path = [["M", x, y],["L", x, splitLocationY], ["L", x+PedigreeEditorParameters.attributes.personSiblingHandleLengthX, splitLocationY]];
                editor.getPaper().path(path).attr({"stroke-width": strokeWidth, stroke: "gray"}).insertBefore(nodeShapes);

                // sibling handle
                this.generateHandle('sibling', x+PedigreeEditorParameters.attributes.personSiblingHandleLengthX-strokeWidth/3, splitLocationY, x+PedigreeEditorParameters.attributes.personSiblingHandleLengthX-strokeWidth/2, splitLocationY+PedigreeEditorParameters.attributes.personSiblingHandleLengthY,
                                    "Click to create a sibling or drag to an existing parentless person (valid choices will be highlighted in green)", "U");

                if (editor.getGraph().getParentRelationship(node.getID()) === null) {
                    // hint for the parent handle
                    var topHandleHint = undefined;
                    if (PedigreeEditorParameters.attributes.enableHandleHintImages) {
                        var hintSize = PedigreeEditorParameters.attributes.radius/2;
                        var path = [["M", x-hintSize, y- PedigreeEditorParameters.attributes.personHandleLength],["L", x+hintSize, y- PedigreeEditorParameters.attributes.personHandleLength]];
                        var line1  = editor.getPaper().path(path).attr({"stroke-width": strokeWidth/3, stroke: "#555555"}).toBack();
                        var father = editor.getPaper().rect(x-hintSize-11,y-PedigreeEditorParameters.attributes.personHandleLength-5.5,11,11).attr({fill: "#CCCCCC"}).toBack();
                        var mother = editor.getPaper().circle(x+hintSize+6,y-PedigreeEditorParameters.attributes.personHandleLength,6).attr({fill: "#CCCCCC"}).toBack();
                        var topHandleHint = editor.getPaper().set().push(line1, father, mother);
                    }
                    // parent handle
                    this.generateHandle('parent', x, splitLocationY, x, y - PedigreeEditorParameters.attributes.personHandleLength,
                                        "Click to draw parents or drag to an existing person or partnership (valid choices will be highlighted in green). Dragging to a person will create a new relationship.", "F", topHandleHint);
                }

                if (!node.isFetus()) {

                    if (node.getChildlessStatus() === null) {
                        // children handle
                        //static part (going right below the node)
                        var path = [["M", x, y],["L", x, y+PedigreeEditorParameters.attributes.personHandleBreakX]];
                        editor.getPaper().path(path).attr({"stroke-width": strokeWidth, stroke: "gray"}).insertBefore(nodeShapes);
                        this.generateHandle('child', x, y+PedigreeEditorParameters.attributes.personHandleBreakX-2, x, y+PedigreeEditorParameters.attributes.personHandleLength,
                                            "Click to draw a child or drag to an existing person without parents (valid choices will be highlighted in green)", "U");
                    }

                    // partner handle
                    var vertPosForPartnerHandles = y;
                    //static part (going left from the node)
                    var path = [["M", x, vertPosForPartnerHandles],["L", x - PedigreeEditorParameters.attributes.personHandleBreakX, vertPosForPartnerHandles]];
                    editor.getPaper().path(path).attr({"stroke-width": strokeWidth, stroke: "gray"}).insertBefore(nodeShapes);
                    this.generateHandle('partnerR', x - PedigreeEditorParameters.attributes.personHandleBreakX + 2, vertPosForPartnerHandles, x - PedigreeEditorParameters.attributes.personHandleLength, vertPosForPartnerHandles,
                                        "Click to draw a partner or drag to an existing person to create a partnership (valid choices will be highlighted in green)", partnerGender);
                }
            }
            else {
                if (editor.getGraph().getParentRelationship(node.getID()) === null)
                    this.generateHandle('parent',   x, y, x, y - PedigreeEditorParameters.attributes.personHandleLength, "Click to draw parents or drag to an existing person or partnership (valid choices will be highlighted in green)");

                if (!node.isFetus()) {
                    if (node.getChildlessStatus() === null)
                        this.generateHandle('child',x, y, x, y + PedigreeEditorParameters.attributes.personHandleLength, "Click to draw a child or drag to a person without parents to create a parent-child relationship (valid choices will be highlighted in green)");
                    this.generateHandle('partnerR', x, y, x + PedigreeEditorParameters.attributes.personHandleLength, y, "Click to draw a partner or drag to an existing person to create a partnership (valid choices will be highlighted in green)");
                    this.generateHandle('partnerL', x, y, x - PedigreeEditorParameters.attributes.personHandleLength, y, "Click to draw a partner or drag to an existing person to create a partnership (valid choices will be highlighted in green)");
                }
            }

            var handleElements = editor.getPaper().setFinish();
            this.setDoNotHideHoverbox(handleElements);

            this._currentHandles.push(handleElements);

            //timer.printSinceLast("Generate handles ");
        },

        /**
         * Creates the buttons used in this hoverbox
         *
         * @method generateButtons
         */
        generateButtons: function($super) {
            if (this._currentButtons !== null) return;
            $super();

            this.generateMenuBtn();

            var hasEditRights = editor.getPatientAccessPermissions(this.getNode().getPhenotipsPatientId()).hasEdit;

            if (hasEditRights) {
                if (this.getNode().getLifeStatus() == "alive" || this.getNode().getLifeStatus() == "deceased") {
                    this.generateAliveWell();
                }
            }

            this._updateHoverBoxHeight();

            // proband can't be removed, and the only remaining node can't be removed
            // TODO: nodes which the used has no edit rights for can not be removed: this should be improved in the future,
            //       see discussion for PT-3442
            if (hasEditRights
                && !this.getNode().isProband()
                && !editor.getGraph().getMaxNodeId() == 0) {
                this.generateDeleteBtn();
            }
        },

        /**
         * Creates a node-shaped show-menu button
         *
         * @method generateMenuBtn
         * @return {Raphael.st} The generated button
         */
        generateMenuBtn: function() {
            var me = this;
            var action = function() {
                me.toggleMenu(!me.isMenuToggled());
            };
            var genderShapedButton = this.getNode().getGraphics().getGenderShape().clone();
            genderShapedButton.attr(PedigreeEditorParameters.attributes.nodeShapeMenuOff);
            genderShapedButton.click(action);
            genderShapedButton.hover(function() { genderShapedButton.attr(PedigreeEditorParameters.attributes.nodeShapeMenuOn)},
                                     function() { genderShapedButton.attr(PedigreeEditorParameters.attributes.nodeShapeMenuOff)});
            genderShapedButton.attr("cursor", "pointer");
            this.setDoNotHideHoverbox(genderShapedButton);
            this._currentButtons.push(genderShapedButton);
            this.disable();
            this.getFrontElements().push(genderShapedButton);
            this.enable();
        },

        /**
         * Generates alive, alive and well and deceased radio button section
         *
         * @method generateAliveWell
         * @return {Raphael.el} Raphael element set
         */
        generateAliveWell: function() {
            var node = this.getNode();

            var lifeStatus = node.getLifeStatus();
            var awStatus = node.getAliveAndWell();

            // list of radio buttons
            var buttons = [ { "label": "Alive",        "lifeStatus": "alive",  "aw": false },
                            { "label": "Alive & Well", "lifeStatus": "alive",  "aw": true },
                            { "label": "Deceased",     "lifeStatus": "deceased" } ];

            var yPos = this.getY() + this._baseHeight + 5;
            var itemHeight = PedigreeEditorParameters.attributes.awLabel["font-size"] + 3;
            var computeItemPosition = function(itemIndex) {
                return yPos + itemHeight * itemIndex;
            };

            var yTickIndex = 0;
            var aliveAndWell = editor.getPaper().set();
            var animatedElements = editor.getPaper().set();

            var tick = this._generateRadioTickCircle(this.getX()+15, computeItemPosition(yTickIndex), true);

            for (var index = 0; index < buttons.length; index++) {

                if (lifeStatus == buttons[index].lifeStatus) {
                    if ((!buttons[index].hasOwnProperty("aw") && !awStatus) ||
                        (buttons[index].hasOwnProperty("aw") && buttons[index].aw == awStatus)) {
                        yTickIndex = index;
                    }
                }

                var circle = this._generateRadioTickCircle(this.getX()+15, computeItemPosition(index), false);
                var text = editor.getPaper().text(this.getX()+25, computeItemPosition(index), buttons[index].label).attr(PedigreeEditorParameters.attributes.awLabel);
                text.node.setAttribute("class", "field-no-user-select");
                var rect = editor.getPaper().rect(this.getX(), computeItemPosition(index)-itemHeight/2, this._width-10, itemHeight, 1).attr(PedigreeEditorParameters.attributes.awRect);

                rect.click(function(i) {
                    tick.attr({'cy' : computeItemPosition(i)});

                    var properties = {};
                    buttons[i].hasOwnProperty("aw") && (properties["setAliveAndWell"] = buttons[i].aw);
                    properties["setLifeStatus"] = buttons[i].lifeStatus;
                    var event = { "nodeID": this.getNode().getID(), "properties": properties };

                    if (buttons[i].lifeStatus == 'deceased') {
                        this._generateDeceasedMenu(node, tick);
                    }
                    document.fire("pedigree:node:setproperty", event);
                }.bind(this, index));

                animatedElements.push(circle, text);
                aliveAndWell.push(rect);
            }

            tick.attr({'cy': computeItemPosition(yTickIndex)});
            tick.toFront();  // tick should be on top of radio empty circles
            animatedElements.push(tick);

            aliveAndWell.push(animatedElements);
            aliveAndWell.icon = animatedElements;
            aliveAndWell.mask = animatedElements;

            this.setDoNotHideHoverbox(aliveAndWell);

            if (this._hidden && !this.isMenuToggled()) {
                aliveAndWell.hide();
            }

            this._currentButtons.push(aliveAndWell);
            this.disable();
            this.getFrontElements().push(aliveAndWell);
            this.enable();
        },

        _generateDeceasedMenu: function(node, tick) {
            this._isDeceasedToggled = true;
            var x = tick.getBBox().x;
            var y = tick.getBBox().y2;
            var position = editor.getWorkspace().canvasToDiv(x, y);
            editor.getDeceasedMenu().show(node, position.x, position.y + 10);
        },

        /**
         * Returns true if the menu for this node is open
         *
         * @method isMenuToggled
         * @return {Boolean}
         */
        isMenuToggled: function() {
            return this._isMenuToggled;
        },

        /**
         * Returns true if the deceased menu for this node is open
         *
         * @method isDeceasedToggled
         * @return {Boolean}
         */
        isDeceasedToggled: function() {
            return this._isDeceasedToggled;
        },

        /**
         * Shows/hides the menu for this node
         *
         * @method toggleMenu
         */
        toggleMenu: function(isMenuToggled) {
            if (this._justClosedMenu) return;
            var _this = this;
            this._isMenuToggled = isMenuToggled;

            // do not display menu if current user has no view permission for this patient
            if (!editor.getPatientAccessPermissions(this.getNode().getPhenotipsPatientId()).hasView) {
                var allowUnhighlightNode = function() {
                    _this._isMenuToggled = false;
                    _this.animateHideHoverZone();
                }
                editor.getOkCancelDialogue().showError("You do not have permission to view information about this patient.",
                                                       "Cannot display patient details", "OK", allowUnhighlightNode);
                return;
            }

            var displayMenu = function() {
                if(isMenuToggled) {
                    _this.getNode().getGraphics().unmark();
                    var optBBox = _this.getBoxOnHover().getBBox();
                    var x = optBBox.x2;
                    var y = optBBox.y;
                    var position = editor.getWorkspace().canvasToDiv(x+5, y);
                    _this._nodeMenu.show(_this.getNode(), position.x, position.y);
                }
            }

            if (!editor.getPatientAccessPermissions(this.getNode().getPhenotipsPatientId()).hasEdit) {
                editor.getOkCancelDialogue().showError("You do not have permission to modify this patient. The patient summary will be displayed in read-only mode.",
                                                       "Cannot modify this patient", "OK", displayMenu);
            } else {
                displayMenu();
            }
        },

        /**
         * Hides the hoverbox with a fade out animation
         *
         * @method animateHideHoverZone
         */
        animateHideHoverZone: function($super, event, x, y) {
            this._hidden = true;

            var allLegends = editor.getAllLegends();
            for (var i = 0; i < allLegends.length; i++) {
                allLegends[i].unhighlightAllObjects();
            }

            if(!this.isMenuToggled() && !this.isDeceasedToggled()){
                var parentPartnershipNode = editor.getGraph().getParentRelationship(this.getNode().getID());
                //console.log("Node: " + this.getNode().getID() + ", parentPartnershipNode: " + parentPartnershipNode);
                if (parentPartnershipNode && editor.getNode(parentPartnershipNode)) {
                    editor.getNode(parentPartnershipNode).getGraphics().unmarkPregnancy();
                }
                $super(event, x, y);
            }
        },

        /**
         * Displays the hoverbox with a fade in animation
         *
         * @method animateDrawHoverZone
         */
        animateDrawHoverZone: function($super, event, x, y) {
            this._hidden = false;

            var allLegends = editor.getAllLegends();
            for (var i = 0; i < allLegends.length; i++) {
                allLegends[i].highlightObjectsForNode(this.getNode().getID());
            }

            if (!this.isMenuToggled()) {
                var parentPartnershipNode = editor.getGraph().getParentRelationship(this.getNode().getID());
                if (parentPartnershipNode && editor.getNode(parentPartnershipNode)) {
                    editor.getNode(parentPartnershipNode).getGraphics().markPregnancy();
                }
                $super(event, x, y);
            }
        },

        /**
         * Performs the appropriate action for clicking on the handle of type handleType
         *
         * @method handleAction
         * @param {String} handleType "child", "partner" or "parent"
         * @param {Boolean} isDrag True if this handle is being dragged
         */
        handleAction : function(handleType, isDrag, curHoveredId) {
            console.log("handleType: " + handleType + ", isDrag: " + isDrag + ", curHovered: " + curHoveredId);

            if(isDrag && curHoveredId !== null) {
                if(handleType == "parent") {
                    this.removeHandles();
                    this.removeButtons();
                    var event = { "personID": this.getNode().getID(), "parentID": curHoveredId };
                    document.fire("pedigree:person:drag:newparent", event);
                }
                else if(handleType == "partnerR" || handleType == "partnerL") {
                    this.removeHandles();
                    var event = { "personID": this.getNode().getID(), "partnerID": curHoveredId };
                    document.fire("pedigree:person:drag:newpartner", event);
                }
                else if(handleType == "child") {
                    var event = { "personID": curHoveredId, "parentID": this.getNode().getID() };
                    document.fire("pedigree:person:drag:newparent", event);
                }
                else if(handleType == "sibling") {
                    var event = { "sibling2ID": curHoveredId, "sibling1ID": this.getNode().getID() };
                    document.fire("pedigree:person:drag:newsibling", event);
                }
            }
            else if (!isDrag) {
                if(handleType == "partnerR" || handleType == "partnerL") {
                    this.removeHandles();
                    var preferLeft = (this.getNode().getGender() == 'F') || (handleType == "partnerL");
                    var event = { "personID": this.getNode().getID(), "preferLeft": preferLeft };
                    document.fire("pedigree:person:newpartnerandchild", event);
                }
                else if(handleType == "child") {
                    var position = editor.getWorkspace().canvasToDiv(this.getNodeX(), (this.getNodeY() + PedigreeEditorParameters.attributes.personHandleLength + 15));
                    editor.getNodetypeSelectionBubble().show(this.getNode(), position.x, position.y);
                    // if user selects anything the bubble will fire an even on its own
                }
                else if(handleType == "sibling") {
                    var position = editor.getWorkspace().canvasToDiv(this.getNodeX() + PedigreeEditorParameters.attributes.personSiblingHandleLengthX - 4,
                                                                     this.getNodeY() - PedigreeEditorParameters.attributes.personHandleBreakY+PedigreeEditorParameters.attributes.personSiblingHandleLengthY + 15);
                    editor.getSiblingSelectionBubble().show(this.getNode(), position.x, position.y);
                }
                else if(handleType == "parent") {
                    this.removeHandles();
                    this.removeButtons();
                    var event = { "personID": this.getNode().getID() };
                    document.fire("pedigree:person:newparent", event);
                }
            }
            this.animateHideHoverZone();
        },

        /**
         * Generate circles to draw radio buttons, white under-circle if tick if false and black if true
         *
         * @method _generateRadioTickCircle
         */
        _generateRadioTickCircle: function(x, y, tick) {
            return editor.getPaper().circle(x, y, 5).attr({"fill": (tick) ? "#000": "#fff"});
        },
    });
    return PersonHoverbox;
});
