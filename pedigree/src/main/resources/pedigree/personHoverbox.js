/*
 * PersonHoverbox is a class for all the UI elements and graphics surrounding a Person node and
 * its labels. This includes the box that appears around the node when it's hovered by a mouse, as
 * well as the handles used for creating connections and creating new nodes.
 *
 * @param pedigree_node the
 * @param x the x coordinate on the Raphael canvas at which the node drawing will be centered
 * @param the y coordinate on the Raphael canvas at which the node drawing will be centered
 * @param gender either 'M', 'F' or 'U' depending on the gender
 * @param id a unique numerical ID number
 */

var PersonHoverbox = Class.create(AbstractHoverbox, {

    /**
     * @param personNode the Person around which the box is drawn
     * @param centerX the x coordinate on the Raphael canvas at which the hoverbox will be centered
     * @param centerY the y coordinate on the Raphael canvas at which the hoverbox will be centered
     */
    initialize: function($super, personNode, centerX, centerY, nodeShapes) {
        var radius = editor.attributes.radius * 2;
        $super(personNode, centerX - radius, centerY - radius, radius * 2, radius * 2, centerX, centerY, nodeShapes);
        this._isMenuToggled = false;
        var r = editor.attributes.radius;
    },

    /**
     * Creates four handles around the node. Returns a Raphael set.
     */
    generateHandles: function($super) {
        this._upHandle = this.generateHandle('parent', this.getNodeX(), this.getNodeY() - (editor.attributes.radius * 1.6));
        this._downHandle = this.generateHandle('child', this.getNodeX(), this.getNodeY() + (editor.attributes.radius * 1.6));
        this._rightHandle = this.generateHandle('partner', this.getNodeX() + (editor.attributes.radius * 1.6), this.getNodeY());
        this._leftHandle = this.generateHandle('partner', this.getNodeX() - (editor.attributes.radius * 1.6), this.getNodeY());
        return $super().push(this._upHandle, this._downHandle, this._rightHandle, this._leftHandle);
    },
    /**
     * Creates remove and menu buttons. Returns a raphael set.
     */
    generateButtons: function($super) {
        var buttons = $super().push(this.generateMenuBtn());
        (!this.getNode().isProband()) && buttons.push(this.generateDeleteBtn());
        return buttons;
    },

    /*
     * Hides the partner and children handles
     */
    hidePartnerHandles: function() {
        this.getCurrentHandles().exclude(this._rightHandle.hide());
        this.getCurrentHandles().exclude(this._leftHandle.hide());
    },

    /*
     * Unhides the partner and children handles
     */
    unhidePartnerHandles: function() {
        if(this.isHovered() || this.isMenuToggled()) {
            this._rightHandle.show();
            this._leftHandle.show();
        }
        (!this.getCurrentHandles().contains(this._rightHandle)) && this.getCurrentHandles().push(this._rightHandle);
        (!this.getCurrentHandles().contains(this._leftHandle)) && this.getCurrentHandles().push(this._leftHandle);
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
     * Hides the parent handle
     */
    hideParentHandle: function() {
        this.getCurrentHandles().exclude(this._upHandle.hide());
    },

    /*
     * Unhides the parent handle
     */
    unHideParentHandle: function() {
        if(this.isHovered() || this.isMenuToggled()) {
            this._upHandle.show();
        }
        this.getCurrentHandles().push(this._upHandle);
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
            var position = editor.getWorkspace().canvasToDiv(x+5, y);
            editor.getNodeMenu().show(this.getNode(), position.x, position.y);
        }
        else {
            //this.enable();
            editor.getNodeMenu().hide();
        }
    },

    /*
     * Fades the hoverbox graphics out
     */
    animateHideHoverZone: function($super) {
        if(!this.isMenuToggled()){
            this.getNode().getParentPregnancy() && this.getNode().getParentPregnancy().getGraphics().shrink();
            $super();
        }
    },

    animateDrawHoverZone: function($super) {
        this.getNode().getParentPregnancy() && this.getNode().getParentPregnancy().getGraphics().grow();
        $super();
    },

    /*
     * Performs the appropriate action for clicking on the handle of type handleType
     *
     * @param handleType can be either "child", "partner" or "parent"
     */
    handleAction : function(handleType, isDrag) {
        var curHovered = editor.getGraph().getCurrentHoveredNode();
        if(isDrag && curHovered) {
            if(curHovered.validPartnerSelected) {
                curHovered.validPartnerSelected = false;
                this.addPartnerAction(curHovered);
            }
            else if(curHovered.validChildSelected) {
                curHovered.validChildSelected = false;
                this.addChildAction(this.getNode().addChild(curHovered));

            }
            else if(curHovered.validParentSelected) {
                curHovered.validParentSelected = false;
                this.addParentAction(this.getNode().addParent(curHovered), curHovered);
            }
            else if(curHovered.validParentsSelected) {
                curHovered.validParentsSelected = false;
                this.addParentsAction(this.getNode().addParents(curHovered));
            }
        }
        else if (!isDrag) {
            if(handleType == "partner") {
                this.createPartnerAction(this.getNode().createPartner(false));
            }
            else if(handleType == "child") {
                //this.getNode().createChild();
                var position = editor.getWorkspace().canvasToDiv(this.getNodeX(), (this.getNodeY() + editor.attributes.radius * 2.3));
                editor.getNodeTypeOptions().show(this.getNode(), position.x, position.y);
                this.disable();
            }
            else if(handleType == "parent") {
                this.createParentsAction(this.getNode().createParents());
            }
            curHovered && curHovered.getGraphics().getHoverBox().getBoxOnHover().attr(editor.attributes.boxOnHover);
        }
        editor.getGraph().setCurrentHoveredNode(null);
        editor.getGraph().setCurrentDraggable(null);
    },

    createPartnerAction: function(partnership) {
        if(partnership){
            var nodeID = this.getNode().getID(),
                part = partnership.getInfo(),
                partner = partnership.getPartnerOf(this.getNode()).getInfo(),
                preg = partnership.getPregnancies()[0].getInfo(),
                ph = partnership.getPregnancies()[0].getChildren()[0].getInfo();

            var redoFunct = function() {
                var source = editor.getGraph().getNodeMap()[nodeID];
                if(source) {
                    var person = editor.getGraph().addPerson(partner.x, partner.y, partner.gender, partner.id);
                    var newPartnership = editor.getGraph().addPartnership(part.x, part.y, source, person, part.id);
                    var pr = editor.getGraph().addPregnancy(preg.x, preg.y, newPartnership, preg.id);
                    var child = editor.getGraph().addPlaceHolder(ph.x, ph.y, ph.gender, ph.id);
                    pr.addChild(child);
                }
            };

            var undoFunct = function() {
                var placeholder = editor.getGraph().getNodeMap()[ph.id];
                placeholder && placeholder.remove(false);
                var thePartner = editor.getGraph().getNodeMap()[partner.id];
                thePartner && thePartner.remove(false);
            };
            editor.getActionStack().push({undo: undoFunct, redo: redoFunct})
        }
    },

    addPartnerAction: function(partner) {
        var partnership = this.getNode().addPartner(partner);
        if(partnership) {
            var part = partnership.getInfo(),
                preg = partnership.getPregnancies()[0].getInfo(),
                ph = partnership.getPregnancies()[0].getChildren()[0].getInfo(),
                nodeID = this.getNode().getID(),
                partnerID = partner.getID();

            var redoFunct = function() {
                var source = editor.getGraph().getNodeMap()[nodeID];
                var person = editor.getGraph().getNodeMap()[partnerID];
                if(source && person) {
                    var p = editor.getGraph().addPartnership(part.x, part.y, source, person, part.id);
                    var pr = editor.getGraph().addPregnancy(preg.x, preg.y, p, preg.id);
                    var child = editor.getGraph().addPlaceHolder(ph.x, ph.y, ph.gender, ph.id);
                    pr.addChild(child);
                }
            };

            var undoFunct = function() {
                var placeHolder = editor.getGraph().getNodeMap()[ph.id];
                placeHolder && placeHolder.remove(false);
                var partner = editor.getGraph().getNodeMap()[part.id];
                partner && partner.remove(false);
            };
            editor.getActionStack().push({undo: undoFunct, redo: redoFunct})
        }
    },

    addChildAction: function(child) {
        if(child) {
            var childID = child.getID(),
                nodeID = this.getNode().getID(),
                p = child.getParentPregnancy(),
                pa = p.getPartnership(),
                preg = p.getInfo(),
                part = pa.getInfo(),
                parent = pa.getPartnerOf(this.getNode()).getInfo();

            var redoFunct = function() {
                var source = editor.getGraph().getNodeMap()[nodeID];
                var theChild = editor.getGraph().getNodeMap()[childID];
                if(source && theChild) {
                    var ph = editor.getGraph().addPlaceHolder(parent.x, parent.y, parent.gender, parent.id);
                    var partnership = editor.getGraph().addPartnership(part.x, part.y, source, ph, part.id);
                    var pregnancy = editor.getGraph().addPregnancy(preg.x, preg.y, partnership, preg.id);
                    pregnancy.addChild(theChild);
                }
            };
            var undoFunct = function() {
                var partnership = editor.getGraph().getNodeMap()[part.id];
                partnership && partnership.remove(false);
                var par = editor.getGraph().getNodeMap()[parent.id];
                par && par.remove(false);
            };
            editor.getActionStack().push({undo: undoFunct, redo: redoFunct});
        }
    },

    createParentsAction: function(partnership) {
        if(partnership) {
            var nodeID = this.getNode().getID(),
                part = partnership.getInfo(),
                preg = partnership.getPregnancies()[0].getInfo(),
                parent1 = partnership.getPartners()[0].getInfo(),
                parent2 = partnership.getPartners()[1].getInfo();

            var redoFunct = function() {
                var child = editor.getGraph().getNodeMap()[nodeID];
                if(child) {
                    var par1 = editor.getGraph().addPerson(parent1.x, parent1.y, parent1.gender, parent1.id);
                    var par2 = editor.getGraph().addPerson(parent2.x, parent2.y, parent2.gender, parent2.id);
                    var partn = editor.getGraph().addPartnership(part.x, part.y, par1, par2, part.id);
                    var pregnancy = editor.getGraph().addPregnancy(preg.x, preg.y, partn, preg.id);
                    pregnancy.addChild(child);
                }
            };
            var undoFunct = function() {
                var par1 = editor.getGraph().getNodeMap()[parent1.id];
                var par2 = editor.getGraph().getNodeMap()[parent2.id];
                var partn = editor.getGraph().getNodeMap()[part.id];
                partn && partn.remove(false);
                par1 && par1.remove(false);
                par2 && par2.remove(false);
            };
            editor.getActionStack().push({undo: undoFunct, redo: redoFunct})
        }
    },

    addParentsAction: function(partnership) {
        if(partnership) {
            var nodeID = this.getNode().getID(),
                partID = partnership.getID(),
                preg = this.getNode().getParentPregnancy().getInfo();

            var redoFunct = function() {
                var child = editor.getGraph().getNodeMap()[nodeID];
                var partn  = editor.getGraph().getNodeMap()[partID];
                if(child && partn) {
                    var pregnancy = editor.getGraph().addPregnancy(preg.x, preg.y, partn, preg.id);
                    pregnancy.addChild(child);
                }
            };
            var undoFunct = function() {
                var pregnancy = editor.getGraph().getNodeMap()[preg.id];
                pregnancy && pregnancy.remove();
            };
            editor.getActionStack().push({undo: undoFunct, redo: redoFunct})
        }
    },

    addParentAction: function(partnership, parent) {
        if(partnership && parent) {
            var parentID = parent.getID(),
                nodeID = this.getNode().getID(),
                part = partnership.getInfo(),
                partner = partnership.getPartnerOf(parent).getInfo(),
                preg = partnership.getPregnancies()[0].getInfo();
        }

        var redoFunct = function() {
            var child = editor.getGraph().getNodeMap()[nodeID];
            var par = editor.getGraph().getNodeMap()[parentID];
            if(child && par) {
                var ph =  editor.getGraph().addPlaceHolder(partner.x, partner.y, partner.gender, partner.id);
                var partnership = editor.getGraph().addPartnership(part.x, part.y, par, ph, part.id);
                var pregnancy = editor.getGraph().addPregnancy(preg.x, preg.y, partnership, preg.id);
                pregnancy.addChild(child);
            }
        };
        var undoFunct = function() {
            var partnership = editor.getGraph().getNodeMap()[part.id];
            partnership && partnership.remove();
            var ph = editor.getGraph().getNodeMap()[partner.id];
            ph && ph.remove(false);

        };
        editor.getActionStack().push({undo: undoFunct, redo: redoFunct})
    }
});
