var PlaceHolderVisuals = Class.create(AbstractNodeVisuals, {

    initialize: function($super, x, y, gender) {
        $super(x, y, gender);
        this.setDraggable();
    },

    updateGenderShape: function($super) {
        $super();
        var shape = this.getGenderShape().attr(editor.graphics._attributes.phShape);
        var text = editor.paper.text(this.getX(), this.getY() - editor.graphics.getRadius()/20, "DRAG ME\nOR\nCLICK ME");
        text.attr(editor.graphics._attributes.dragMeLabel);
        shape.push(text);
        shape.ox = shape.getBBox().x;
        shape.oy = shape.getBBox().y;
    },

    setDraggable: function() {
        var me = this,
            isDragged,
            draggable = true;

        var start = function() {
            //editor.paper.ZPD({ zoom: false, pan: false, drag: false });
            isDragged = false;
            me.getShapes().toFront();
            editor.enterHoverMode(this);
        };

        var move = function(dx, dy) {
            me.getGenderShape().stop().transform("t" + dx +"," + dy);
            if(dx > 5 || dx < -5 || dy > 5 || dy < -5 ) {
                isDragged = true;
                editor.currentDraggable.placeholder = me;
            }
        };

        var end = function() {
            //editor.paper.ZPD({ zoom: true, pan: true, drag: false });
            if(isDragged) {
                draggable = false;
                editor.currentHoveredNode && editor.validPlaceholderNode && me.merge(editor.currentHoveredNode);
                var originalPos = "t" + me.getGenderShape().ox +"," + me.getGenderShape().oy;
                me.getGenderShape()[0].stop().animate({"transform": originalPos}, 2000, "elastic", function() {
                    draggable = true;
                });
            }
            else {
                me.getNode().convertToPerson();
            }
            editor.exitHoverMode();
            editor.currentDraggable.placeholder = null;
        };

        me.getGenderShape().drag(move, start, end);
    }

});