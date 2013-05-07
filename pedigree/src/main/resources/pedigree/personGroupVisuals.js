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

var PersonGroupVisuals = Class.create(AbstractPersonVisuals, {

    initialize: function($super, node, x, y) {
        $super(node,x,y);
        this.label = editor.getPaper().text(x, y, "n").attr(PedigreeEditor.attributes.descendantGroupLabel);
        this.label.insertAfter(this._icon.flatten());
        this._icon.push(this.label);
        this._hoverBox = new GroupHoverbox(node, x, y, this._icon);
    },

    /**
     * Returns all the graphics associated with this PersonGroup
     *
     * @method getAllGraphics
     * @param [$super]
     * @return {Raphael.st} Raphael set containing graphics elements
     */
    getAllGraphics: function ($super) {
        return this.getHoverBox().getBackElements().concat($super()).push(this.getHoverBox().getFrontElements());
    },

    /**
     * Changes the label for the number of people in this group
     *
     * @method setNumPersons
     * @param {Number} numPersons The number of people in this group
     */
    setNumPersons: function(numPersons) {
        this._icon.pop();
        this.label.remove();
        var text = (numPersons && numPersons > 1) ? String(numPersons) : "n";
        this.label = editor.getPaper().text(this.getX(), this.getY(), text).attr(PedigreeEditor.attributes.descendantGroupLabel);
        this._icon.push(this.label);
    }
});