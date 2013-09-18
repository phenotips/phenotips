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
 */

var PersonHoverbox = Class.create(AbstractHoverbox, {

    initialize: function($super, personNode, centerX, centerY, nodeShapes) {
        console.log("abstract hower box start");
        var radius = PedigreeEditor.attributes.radius * 2;
        $super(personNode, -radius, -radius, radius * 2, radius * 2, centerX, centerY, nodeShapes);
        this._isMenuToggled = false;
        console.log("abstract hower box end");
    },

    /**
     * Creates the handles used in this hoverbox
     *
     * @method generateHandles
     * @return {Raphael.st} A set of handles
     */
    generateHandles: function($super) {
        if (editor.getGraph().DG.GG.getInEdges(this.getNode().getID()).length == 0)           
            this._upHandle = this.generateHandle('parent', this.getNodeX(), this.getNodeY() - (PedigreeEditor.attributes.radius * 1.6));
        this._downHandle = this.generateHandle('child', this.getNodeX(), this.getNodeY() + (PedigreeEditor.attributes.radius * 1.6));
        this._rightHandle = this.generateHandle('partnerR', this.getNodeX() + (PedigreeEditor.attributes.radius * 1.6), this.getNodeY());
        this._leftHandle = this.generateHandle('partnerL', this.getNodeX() - (PedigreeEditor.attributes.radius * 1.6), this.getNodeY());
        return $super().push(this._upHandle, this._downHandle, this._rightHandle, this._leftHandle);
    },

    /**
     * Creates the buttons used in this hoverbox
     *
     * @method generateButtons
     * @return {Raphael.st} A set of buttons
     */
    generateButtons: function($super) {
        var buttons = $super().push(this.generateMenuBtn());
        (!this.getNode().isProband()) && buttons.push(this.generateDeleteBtn());
        return buttons;
    },

    /**
     * Hides the partner and children handles
     *
     * @method hidePartnerHandles
     */
    hidePartnerHandles: function() {
        this.getCurrentHandles().exclude(this._rightHandle.hide());
        this.getCurrentHandles().exclude(this._leftHandle.hide());
    },

    /**
     * Displays the partner and children handles
     *
     * @method unhidePartnerHandles
     */
    unhidePartnerHandles: function() {
        if(this.isHovered() || this.isMenuToggled()) {
            this._rightHandle.show();
            this._leftHandle.show();
        }
        (!this.getCurrentHandles().contains(this._rightHandle)) && this.getCurrentHandles().push(this._rightHandle);
        (!this.getCurrentHandles().contains(this._leftHandle)) && this.getCurrentHandles().push(this._leftHandle);
    },

    /**
     * Hides the child handle
     *
     * @method hideChildHandle
     */
    hideChildHandle: function() {
        this.getCurrentHandles().exclude(this._downHandle.hide());
    },

    /**
     * Displays the child handle
     *
     * @method unhideChildHandle
     */
    unhideChildHandle: function() {
        if(this.isHovered() || this.isMenuToggled()) {
            this._downHandle.show();
        }
        (!this.getCurrentHandles().contains(this._downHandle)) && this.getCurrentHandles().push(this._downHandle);
    },

    /**
     * Hides the parent handle
     *
     * @method hideParentHandle
     */
    hideParentHandle: function() {
        this.getCurrentHandles().exclude(this._upHandle.hide());
    },

    /**
     * Displays the parent handle
     *
     * @method unHideParentHandle
     */
    unHideParentHandle: function() {
        if(this.isHovered() || this.isMenuToggled()) {
            this._upHandle.show();
        }
        this.getCurrentHandles().push(this._upHandle);
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
     * Shows/hides the menu for this node
     *
     * @method toggleMenu
     */
    toggleMenu: function(isMenuToggled) {
        console.log("toggle menu");
        this._isMenuToggled = isMenuToggled;
        if(isMenuToggled) {
            this.getNode().getGraphics().unmark();
            var optBBox = this.getBoxOnHover().getBBox();
            var x = optBBox.x2;
            var y = optBBox.y;
            var position = editor.getWorkspace().canvasToDiv(x+5, y);
            editor.getNodeMenu().show(this.getNode(), position.x, position.y);
        }
        else {
            editor.getNodeMenu().hide();
        }
    },

    /**
     * Hides the hoverbox with a fade out animation
     *
     * @method animateHideHoverZone
     */
    animateHideHoverZone: function($super) {
        if(!this.isMenuToggled()){
            var parentPartnershipNode = editor.getGraph().getParentRelationship(this.getNode().getID());
            if (parentPartnershipNode)
                editor.getNode(parentPartnershipNode).getGraphics().unmarkPregnancy();
            $super();
        }
    },

    /**
     * Displays the hoverbox with a fade in animation
     *
     * @method animateDrawHoverZone
     */
    animateDrawHoverZone: function($super) {
        var parentPartnershipNode = editor.getGraph().getParentRelationship(this.getNode().getID());
        if (parentPartnershipNode)
            editor.getNode(parentPartnershipNode).getGraphics().markPregnancy();
        $super();
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
        
        if(isDrag && curHoveredId) {            
            if(handleType == "parent") {                
                this.hideParentHandle();
                var changeSet = editor.getGraph().assignParent(curHoveredId, this.getNode().getID());
                editor.getGraphicsSet().applyChanges(changeSet, true);
            }
            else if(handleType == "partnerR" || handleType == "partnerL") {
                // TODO
            }
            else if(handleType == "child") {
                // TODO
            }
        }
        else if (!isDrag) {
            if(handleType == "partnerR" || handleType == "partnerL") {
                var preferLeft = (handleType == "partnerL");                
                var changeSet = editor.getGraph().addNewRelationship(this.getNode().getID(), {}, preferLeft);                
                editor.getGraphicsSet().applyChanges(changeSet, true);
            }
            else if(handleType == "child") {
                var position = editor.getWorkspace().canvasToDiv(this.getNodeX(), (this.getNodeY() + PedigreeEditor.attributes.radius * 2.3));
                editor.getNodetypeSelectionBubble().show(this.getNode(), position.x, position.y);                
            }
            else if(handleType == "parent") {
                this.hideParentHandle();
                var changeSet = editor.getGraph().addNewParents(this.getNode().getID());
                editor.getGraphicsSet().applyChanges(changeSet, true);
            }
        }
        editor.getGraphicsSet().setCurrentHoveredNode(null);
        editor.getGraphicsSet().setCurrentDraggable(null);        
        this.animateHideHoverZone();
    }
});
