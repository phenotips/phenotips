/**
 * Class for organizing graphics for PersonPlaceholder nodes.
 *
 * @class PersonPlaceholderVisuals
 */
define([
        "pedigree/view/personVisuals",
        "pedigree/view/readonlyHoverbox"
    ], function(
        PersonVisuals,
        ReadOnlyHoverbox
    ){
    var PersonPlaceholderVisuals = Class.create(PersonVisuals, {
        initialize: function($super, node, x, y) {
            $super(node,x,y);
        },

        generateHoverbox: function(x, y) {
            return new ReadOnlyHoverbox(this.getNode(), x, y, this.getGenderGraphics());
            //if (editor.isReadOnlyMode()) {
            //    return new ReadOnlyHoverbox(this.getNode(), x, y, this.getGenderGraphics());
            //} else {
            //    return new PersonPlaceholderHoverbox(this.getNode(), x, y, this.getGenderGraphics());
            //}
        },

        markPermanently: function() {
        },

        grow: function() {
        },

        setGenderGraphics: function() {
            this._genderGraphics && this._genderGraphics.remove();
            var x      = this.getX();
            var y      = this.getY();
            var radius = 10;
            var shape = editor.getPaper().rect(x - radius, y - radius, radius * 2, radius * 2).hide();
            this._genderShape = shape;
            this._genderGraphics = editor.getPaper().set(shape);
        },
            
        /**
         * Overridden to prevent the highlightBox from blocking dragging
         * action at the location of an invisible placeholder node.
         */
        setHighlightBox: function() {
            this._highlightBox && this._highlightBox.remove();
        },

        /**
         * Overridden to make sure some runaway label does not appear for a placeholder
         * (E.g. when father's name is propagated)
         */
        drawLabels: function() {
            var labels = this.getLabels();
            for (var i = 0; i < labels.length; i++) {
                labels[i].hide();
            }
        }
    });
    return PersonPlaceholderVisuals;
});
