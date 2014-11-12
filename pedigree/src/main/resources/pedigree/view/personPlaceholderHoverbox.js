/**
 * PersonPlaceholderHoverbox is a class for all the UI elements and graphics surrounding a Placeholder node.
 *
 * @class PersonPlaceholderHoverbox
 * @extends PersonHoverbox
 * @constructor
 */
var PersonPlaceholderHoverbox = Class.create(PersonHoverbox, {
    initialize: function($super, personNode, centerX, centerY, nodeShapes) {
        $super(personNode, centerX, centerY, nodeShapes);
       },

    /**
    * Creates the handles used in this hoverbox - overriden to generate no handles 
    *
    * @method generateHandles
    * @return {Raphael.st} A set of handles
    */
    generateHandles: function($super) {
        return;
    },
 
    /**
     * Creates the buttons used in this hoverbox
     *
     * @method generateButtons
     */
    generateButtons: function($super) {
        if (this._currentButtons !== null) return;
        this._currentButtons = [];
        // note: no call to super as we don't want default person buttons
        this.generateMenuBtn();
    },

    /**
     * Shows/hides the menu for this node
     *
     * @method toggleMenu
     */
    toggleMenu: function(isMenuToggled) {
        if (this._justClosedMenu) return;
        this._isMenuToggled = isMenuToggled;
        if(isMenuToggled) {
            this.getNode().getGraphics().unmark();
            var optBBox = this.getBoxOnHover().getBBox();
            var x = optBBox.x2;
            var y = optBBox.y;
            var position = editor.getWorkspace().canvasToDiv(x+5, y);
            editor.getNodeGroupMenu().show(this.getNode(), position.x, position.y);
        }
    }
});
