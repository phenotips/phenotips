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
    }
});
