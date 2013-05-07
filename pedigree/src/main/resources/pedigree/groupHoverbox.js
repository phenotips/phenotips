/**
 * GroupHoverbox is a class for all the UI elements and graphics surrounding a PersonGroup node and
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

var GroupHoverbox = Class.create(AbstractHoverbox, {
    initialize: function($super, node, centerX, centerY, nodeShapes) {
        var radius = PedigreeEditor.attributes.radius * 2;
        $super(node, centerX - radius, centerY - radius, radius * 2, radius * 2, centerX, centerY, nodeShapes);
       },

    /**
     * Creates the buttons used in this hoverbox
     *
     * @method generateButtons
     * @return {Raphael.st} A set of buttons
     */
    generateButtons: function($super) {
        return $super().push(this.generateDeleteBtn());
    }
});
