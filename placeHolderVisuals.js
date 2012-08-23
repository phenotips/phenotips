/*
 * A class responsible for the graphic representation of a placeholder object.
 * Also handles dragging and clicking behavior of placeholders.
 *
 * @param node the PlaceHolder object for which this graphics are handled
 * @param x the x coordinate on the canvas
 * @param x the y coordinate on the canvas
 */

var PlaceHolderVisuals = Class.create(AbstractPersonVisuals, {

    initialize: function($super, node, x, y) {
        $super(node, x, y);
        this.setDraggable();
    },

    /*
     * Sets/replaces the gender symbol with the symbol appropriate for the gender. Returns raphael set containing
     * the genderShape, a shadow behind it, and the text "drag me or click me".
     */
    setGenderSymbol: function($super) {
        $super();
        var shape = this.getGenderSymbol().attr(editor.attributes.phShape);
        var text = editor.getPaper().text(this.getX(), this.getY() - editor.attributes.radius/20, "DRAG ME\nOR\nCLICK ME");
        text.attr(editor.attributes.dragMeLabel);
        shape.push(text);
        shape.attr("cursor", "pointer");
        shape.ox = shape.getBBox().x;
        shape.oy = shape.getBBox().y;
        shape.flatten().insertAfter(editor.getProband().getGraphics().getAllGraphics().flatten());
    },

    /*
     * Handles the dragging and clicking behavior of the placeholder
     */
    setDraggable: function() {
        var me = this,
            isDragged,
            draggable = true,
            ox = 0,
            oy = 0,
            absOx,
            absOy;

        //Action on mouse click
        var start = function() {
            if(!me.isAnimating) {
                absOx = me.getX();
                absOy = me.getY();
                isDragged = false;
                me.getShapes().toFront();
                editor.enterHoverMode(me.getNode());
            }
        };

        //Called when the placeholder is dragged
        var move = function(dx, dy) {
            dx = dx/editor.zoomCoefficient;
            dy = dy/editor.zoomCoefficient;
            if(!me.isAnimating) {
                me.setPos(absOx + dx, absOy + dy);
                ox = dx;
                oy = dy;
                if(dx > 2 || dx < -2 || dy > 2 || dy < -2 ) {
                    isDragged = true;
                    editor.currentDraggable.placeholder = me.getNode();
                }
            }
        };

        //Called when the mouse button is released
        var end = function() {
            if(!me.isAnimating){
                if(isDragged) {
                    draggable = false;
                    var node = editor.currentHoveredNode;
                    var vp = editor.validPlaceholderNode;
                    editor.validPlaceholderNode = false;
                    if(node && vp) {
                        me.getNode().merge(node);
                    }
                    else {
                        me.isAnimating = true;
                        me.setPos(absOx, absOy, true, function() {
                            me.isAnimating = false;
                        });
                        ox = 0;
                        oy = 0;
                    }
                }
                else {
                    me.setPos(absOx, absOy, false);
                    me.getNode().convertToPerson();
                }
                editor.exitHoverMode();
                editor.currentDraggable.placeholder = null;
            }
        };

        //Adds dragging capability to the genderSymbols
        me.getGenderSymbol().drag(move, start, end);
    }
});