/**
 * Class for organizing graphics for PersonGroup nodes.
 *
 * @class PersonGroupVisuals
 * @constructor
 * @extends AbstractPersonVisuals
 * @param {PersonGroup} node The node for which this graphics are handled
 * @param {Number} x The x coordinate on the canvas
 * @param {Number} y The y coordinate on the canvas
 */

var PersonGroupVisuals = Class.create(PersonVisuals, {

    initialize: function($super, node, x, y) {
        $super(node,x,y);
        this.setNumPersons(node.getNumPersons());
    },

    generateHoverbox: function(x, y) {
        if (editor.isReadOnlyMode()) {
            return new ReadOnlyHoverbox(this.getNode(), x, y, this.getGenderGraphics());
        } else {
            return new PersonGroupHoverbox(this.getNode(), x, y, this.getGenderGraphics());
        }
    },

    /**
     * Returns all the graphics associated with this PersonGroup
     *
     * @method getAllGraphics
     * @param [$super]
     * @return {Raphael.st} Raphael set containing graphics elements
     */
    getAllGraphics: function ($super) {
        return $super().push(this._label);
    },

    /**
     * Changes the label for the number of people in this group
     *
     * @method setNumPersons
     * @param {Number} numPersons The number of people in this group
     */
    setNumPersons: function(numPersons) {
        this._label && this._label.remove();
        var text = (numPersons && numPersons > 1) ? String(numPersons) : "n";
        var y = (this.getNode().getLifeStatus() == 'aborted' || this.getNode().getLifeStatus() == 'miscarriage') ? this.getY() - 12 : this.getY();
        var x = (this.getNode().getLifeStatus() == 'aborted') ? this.getX() + 8  : this.getX();
        this._label = editor.getPaper().text(x, y, text).attr(PedigreeEditor.attributes.descendantGroupLabel);
        this._label.node.setAttribute("class", "no-mouse-interaction");
    }
});