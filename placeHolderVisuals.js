var PlaceHolderVisuals = Class.create(AbstractPersonVisuals, {

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
        shape.attr("cursor", "pointer");
        shape.ox = shape.getBBox().x;
        shape.oy = shape.getBBox().y;
    },

    setDraggable: function() {
        var me = this,
            isDragged,
            draggable = true;

        var ox = 0;
        var oy = 0;
        var absOx;
        var absOy;

        var start = function() {
            absOx = me.getAbsX();
            //alert(absOx);
            absOy = me.getAbsY();
//            editor.zpd.opts['zoom'] = false;
//            editor.zpd.opts['pan'] = false;
            isDragged = false;
            me.getShapes().toFront();
            editor.enterHoverMode(me.getNode());
        };

        var move = function(dx, dy) {

            me.getGenderShape().stop().transform("T" + (dx - ox) + "," + (dy - oy) + "...");
            me.setAbsPos(absOx + dx, absOy + dy);
            ox = dx;
            oy = dy;
            if(dx > 5 || dx < -5 || dy > 5 || dy < -5 ) {
                isDragged = true;
                editor.currentDraggable.placeholder = me.getNode();
            }
        };

        var end = function() {

//
//            editor.zpd.opts['zoom'] = true;
//            editor.zpd.opts['pan'] = true;

            if(isDragged) {
                draggable = false;
                var node = editor.currentHoveredNode;
                var vp = editor.validPlaceholderNode;
                editor.validPlaceholderNode = false;
                if(node && vp) {
                    me.getNode().merge(node);
                }
                else {
                    me.moveTo(absOx, absOy);
                    ox = 0;
                    oy = 0;
               }}

            else {
                me.getNode().convertToPerson();
            }
            editor.exitHoverMode();
            editor.currentDraggable.placeholder = null;
        };

        me.getGenderShape().drag(move, start, end);
    }

});