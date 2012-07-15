var PlaceHolder = Class.create(AbstractNode, {

    initialize: function($super, x, y, gender) {
        $super(x, y, gender);
        this._type = "ph";
        this.setDraggable();
    },

    getType: function() {
        return this._type;
    },

    generateGenderShape: function($super) {
        return $super().attr(editor.graphics._attributes.phShape);
    },

    generateDragMeLabel: function() {
        return editor.paper.text(
            this.getX(), this.getY() - editor.graphics.getRadius()/20, "DRAG ME\nOR\nCLICK ME"
        ).attr(editor.graphics._attributes.dragMeLabel);
    },

    generateDrawing: function($super) {
        var drawing = $super().push(this.generateDragMeLabel());
        drawing.shapeOt = drawing[0].transform();
        drawing.textOt = drawing[1].transform();
        return drawing;
    },

    setDraggable: function()
    {
        var me = this,
            isDragged;

        var start = function() {
            isDragged = false;
            me.draw();
            editor.enterHoverMode(this);
        };

        var move = function(dx, dy) {
            me.getDrawing()[0].stop().transform("t" + dx +"," + dy + "," + me.getDrawing().shapeOt);
            me.getDrawing()[1].stop().transform("t" + dx +"," + dy + "," + me.getDrawing().textOt);
            if(dx > 5 || dx < -5 || dy > 5 || dy < -5 ) {
                isDragged = true;
                editor.currentDraggable.placeholder = me;
            }
        };

        var end = function() {
            if(isDragged) {
                editor.currentHoveredNode && editor.validPlaceholderNode && me.merge(editor.currentHoveredNode);
                me.getDrawing()[0].stop().animate({"transform": me.getDrawing().shapeOt}, 2000, "elastic");
                me.getDrawing()[1].stop().animate({"transform": me.getDrawing().textOt}, 2000, "elastic");
            }
            else {
                var newNode = new PedigreeNode(me.getX(), me.getY(), me.getGender());
                this.merge(newNode);
            }
            editor.exitHoverMode();
            editor.currentDraggable.placeholder = null;
        };

        me.getDrawing().drag(move, start, end);
    },

    merge: function(node) {
        alert('merging');
        //this._child && node.addChild(this._child);
        //this._phChild && node.addPhChild(this._phChild);
        //this.

        //should merge parent/children placeholders
        //build an array of non null values from placeholder and replace/push them on to the node
    },

    canMergeWith: function(node) {
        return (node._gender == this._gender || this._gender == 'unknown' ) &&
            !node.isPartnerOf(this) &&
            !this.hasConflictingParents(node) &&
            !node.isDescendantOf(this) &&
            !node.isAncestorOf(this);
    },

    hasConflictingParents: function(node) {
        var hasConflictingDads = this._father && node._father && this._father != node._father;
        var hasConflictingMoms = this._mother && node._mother && this._mother != node._mother;
        var notReversedParents = (this._mother && this._mother._gender != 'unknown') ||
            (this._father && this._father._gender != 'unknown') ||
            (node._mother && node._mother._gender != 'unknown') ||
            (node._father && node._father._gender != 'unknown') ||
            (this._mother && node._father && this._mother != node._father) ||
            (this._father && node._mother && this._father != node._mother);

        return notReversedParents && (hasConflictingDads || hasConflictingMoms);
    },
});