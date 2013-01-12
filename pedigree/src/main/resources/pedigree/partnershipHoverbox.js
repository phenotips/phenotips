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

var PartnershipHoverbox = Class.create(AbstractHoverbox, {

    /**
     * @param partnership the partnership for which the hoverbox is drawn
     * @param x the x coordinate on the Raphael canvas at which the hoverbox will be centered
     * @param y the y coordinate on the Raphael canvas at which the hoverbox will be centered
     */
    initialize: function($super, partnership, junctionX, junctionY, shapes) {
        var radius = editor.attributes.radius;
        this._isMenuToggled = false;
        $super(partnership, junctionX - radius/1.5, junctionY - radius/2, radius*(4/3), radius*1.7, junctionX, junctionY, shapes);
    },

    /**
     * Creates four handles around the node. Returns a Raphael set.
     */
    generateHandles: function($super) {
        this._downHandle = this.generateHandle('child', this.getNodeX(), this.getNodeY() + (editor.attributes.radius *.8));
        return $super().push(this._downHandle);
    },

    /**
     * Creates delete button. Returns a raphael set.
     */
    generateButtons: function($super) {
        var deleteButton = this.generateDeleteBtn();
        var menuButton = this.generateMenuBtn();
        return $super().push(deleteButton, menuButton);
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
     * Returns true if the menu is toggled for this partnership node
     */
    isMenuToggled: function() {
        return this._isMenuToggled;
    },

    /*
     * Shows/hides the menu for this partnership node
     */
    toggleMenu: function(isMenuToggled) {
        this._isMenuToggled = isMenuToggled;
        if(isMenuToggled) {
            this.disable();
            var optBBox = this.getBoxOnHover().getBBox();
            var x = optBBox.x2;
            var y = optBBox.y;
            var position = editor.getWorkspace().canvasToDiv(x+5, y);
            editor.getPartnershipMenu().show(this.getNode(), position.x, position.y);
        }
        else {
            editor.getPartnershipMenu().hide();
        }
    },

    /*
     * Performs the appropriate action for clicking on the handle of type handleType
     *
     * @param handleType can be either "child", "partner" or "parent"
     */
    handleAction : function(handleType, isDrag) {
        var curHovered = editor.getGraph().getCurrentHoveredNode();
        if(isDrag && curHovered && curHovered.validChildSelected) {
            curHovered.validChildSelected = false;
            this.addChildAction(this.getNode().addChild(curHovered));
        }
        else if (!isDrag && handleType == "child") {
            //this.getNode().createChild();
            var position = editor.getWorkspace().canvasToDiv(this.getNodeX(), (this.getNodeY() + editor.attributes.radius * 2.3));
            editor.getNodeTypeOptions().show(this.getNode(), position.x, position.y);
            this.disable();
            curHovered && curHovered.getGraphics().getHoverBox().getBoxOnHover().attr(editor.attributes.boxOnHover);
        }
        editor.getGraph().setCurrentHoveredNode(null);
        editor.getGraph().setCurrentDraggable(null);
    },

    /*
     * Generates an actionStack entry for adding a child
     * @param child the child that is being added to this partnership
     */
    addChildAction: function(child) {
        if(child) {
            var childID = child.getID(),
                preg = child.getParentPregnancy().getInfo(),
                part = preg.getPartnership().getInfo();

            var redoFunct = function() {
                var source = editor.getGraph().getNodeMap()[part.partnershipID];
                var theChild = editor.getGraph().getNodeMap()[childID];
                if(source && theChild) {
                    var pregnancy = editor.getGraph().addPregnancy(preg.x, preg.y, source, preg.id);
                    pregnancy.addChild(theChild);
                }
            };
            var undoFunct = function() {
                var pregnancy = editor.getGraph().getNodeMap()[preg.id];
                pregnancy && pregnancy.remove(false);
            };
            editor.getActionStack().push({undo: undoFunct, redo: redoFunct});
        }
    }
});
