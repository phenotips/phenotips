/**
 * PersonGroupHoverbox is a class for all the UI elements and graphics surrounding a PersonGroup node and
 * its labels. This includes the box that appears around the node when it's hovered by a mouse.
 *
 * @class GroupHoverbox
 * @extends AbstractHoverbox
 * @constructor
 * @param {PersonGroup} node The node PersonGroup for which the hoverbox is drawn
 * @param {Number} centerX The x coordinate for the hoverbox
 * @param {Number} centerY The y coordinate for the hoverbox
 * @param {Raphael.st} nodeShapes Raphaël set containing the graphical elements that make up the node
 */

var PersonGroupHoverbox = Class.create(PersonHoverbox, {
    initialize: function($super, personNode, centerX, centerY, nodeShapes) {
        var radius = PedigreeEditor.attributes.radius * 2;
        $super(personNode, centerX, centerY, nodeShapes);
       },

    /**
    * Creates the handles used in this hoverbox
    *
    * @method generateHandles
    * @return {Raphael.st} A set of handles
    */
    generateHandles: function($super) {        
        return editor.getPaper().set();
    },
       
    /**
     * Creates the buttons used in this hoverbox
     *
     * @method generateButtons
     * @return {Raphael.st} A set of buttons
     */
    generateButtons: function($super) {
        var buttons = editor.getPaper().set();
        buttons.push(this.generateMenuBtn());
        buttons.push(this.generateDeleteBtn());        
        return buttons;
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
            editor.getNodeGroupMenu().show(this.getNode(), position.x, position.y);
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
            //console.log("Node: " + this.getNode().getID() + ", parentPartnershipNode: " + parentPartnershipNode);            
            if (parentPartnershipNode && editor.getNode(parentPartnershipNode))
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
        if (parentPartnershipNode && editor.getNode(parentPartnershipNode))
            editor.getNode(parentPartnershipNode).getGraphics().markPregnancy();
        $super();
    }    
});
