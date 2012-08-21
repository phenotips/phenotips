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

    /**
     * Creates and returns a delete button (big red X). Returns Raphael set.
     */
    generateDeleteBtn: function() {
        var me = this;
        var action = function() {
            var confirmation = confirm("Are you sure you want to delete this node?");
            confirmation && me.getNode().remove(false, true)
        };
        var path = "M24.778,21.419 19.276,15.917 24.777,10.415 21.949,7.585 16.447,13.087 10.945,7.585 8.117,10.415 13.618,15.917 8.116,21.419 10.946,24.248 16.447,18.746 21.948,24.248z";
        var attributes = editor.attributes.deleteBtnIcon;
        var x = this.getX() + editor.attributes.radius * 0.05;
        var y = this.getY() + editor.attributes.radius * 0.1;
        return this.createButton(x, y, path, attributes, action);
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
        var x = this.getX() + this.getWidth() - editor.attributes.radius * 0.55;
        var y = this.getY() + editor.attributes.radius * 0.1;
        var className = "menu-trigger";
        return this.createButton(x, y, path, attributes, action, className);
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
            var position = editor.getPositionInViewBox(x+5, y);
            editor.nodeMenu.show(this.getNode(), position.x, position.y);
        }
        else {
            //this.enable();
            editor.nodeMenu.hide();
        }
    },

    /*
     * Fades the hoverbox graphics out
     */
    animateHideHoverZone: function($super) {
        if(!this.isMenuToggled()){
            $super();
        }
    }
});
