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

var GroupHoverbox = Class.create(AbstractHoverbox, {

    /**
     * @param personNode the Person around which the box is drawn
     * @param centerX the x coordinate on the Raphael canvas at which the hoverbox will be centered
     * @param centerY the y coordinate on the Raphael canvas at which the hoverbox will be centered
     */
    initialize: function($super, personNode, centerX, centerY, nodeShapes) {
        var radius = editor.attributes.radius * 2;
        $super(personNode, centerX - radius, centerY - radius, radius * 2, radius * 2, centerX, centerY, nodeShapes);
       },

    /**
     * Creates remove and menu buttons. Returns a raphael set.
     */
    generateButtons: function($super) {
        return $super().push(this.generateDeleteBtn());
    }
});
