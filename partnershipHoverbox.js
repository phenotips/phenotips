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
        $super(partnership, junctionX - radius/2, junctionY - radius/2, radius, radius*1.7, junctionX, junctionY, shapes);
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
        var me = this;
        var action = function() {
            var confirmation = confirm("Are you sure you want to delete this partnership?");
            confirmation && me.getNode().remove(false, true)
        };
        var path = "M24.778,21.419 19.276,15.917 24.777,10.415 21.949,7.585 16.447,13.087 10.945,7.585 8.117,10.415 13.618,15.917 8.116,21.419 10.946,24.248 16.447,18.746 21.948,24.248z";
        var attributes = editor.attributes.deleteBtnIcon;
        var x = this.getX() + editor.attributes.radius * 0.05;
        var y = this.getY();
        var deleteButton = this.createButton(x, y, path, attributes, action);
        return $super().push(deleteButton);
    }
});
