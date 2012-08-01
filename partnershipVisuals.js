/*
 * A general superclass for the a graphic engine used by nodes on the Pedigree graph. Can display
 * a shape representing the gender of the attached node.
 */

var PartnershipVisuals = Class.create(AbstractNodeVisuals, {

    initialize: function($super, node, x, y) {
        $super(node, x, y);
    }
});