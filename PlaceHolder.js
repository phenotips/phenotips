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

    generateDrawing: function() {
        var shape = this.generateGenderShape();
        var glow = shape.glow({width: 5, fill: false, opacity: 0.1}).translate(3,3);
        var drawing = editor.paper.set(glow, shape, this.generateDragMeLabel());
        drawing.shapeOt = drawing[0].transform();
        drawing.textOt = drawing[1].transform();
        return drawing;
    },

    removeParent: function($super, parent) {
        $super(parent);
        this.removeIfDisconnected();
    },

    removeChild: function($super, child) {
        $super(child);
        this.removeIfDisconnected();
    },

    removePartner: function($super, partner) {
        $super(partner);
        this.removeIfDisconnected();
    },

    removeIfDisconnected: function() {
        var hasNoParents = !this.getMother && !this.getPhMother() && !this.getFather() && !this.getPhFather();
        var hasNoKids = this.getChildren().flatten().length == 0;
        var isSingleParent = (this.getChildren().flatten().length > 0 && this.getPartners().flatten().length == 0);
        if (hasNoParents &&  (hasNoKids || isSingleParent)) {
            this.remove(false);
        }
    },

    setDraggable: function()
    {
        var me = this,
            isDragged;

        var start = function() {
            editor.paper.ZPD({ zoom: false, pan: false, drag: false });
            isDragged = false;
            me.drawShapes();
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
            editor.paper.ZPD({ zoom: true, pan: true, drag: false });
            if(isDragged) {
                editor.currentHoveredNode && editor.validPlaceholderNode && me.merge(editor.currentHoveredNode);
                me.getDrawing()[0].stop().animate({"transform": me.getDrawing().shapeOt}, 2000, "elastic");
                me.getDrawing()[1].stop().animate({"transform": me.getDrawing().textOt}, 2000, "elastic");
            }
            else {
                var newNode = new Person(me.getX(), me.getY(), me.getGender());
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
        return (node._gender == this._gender || this._gender == 'U' ) &&
            !node.isPartnerOf(this) &&
            !this.hasConflictingParents(node) &&
            !node.isDescendantOf(this) &&
            !node.isAncestorOf(this);
    },

    hasConflictingParents: function(node) {
        var hasConflictingDads = this._father && node._father && this._father != node._father;
        var hasConflictingMoms = this._mother && node._mother && this._mother != node._mother;
        var notReversedParents = (this._mother && this._mother._gender != 'U') ||
            (this._father && this._father._gender != 'U') ||
            (node._mother && node._mother._gender != 'U') ||
            (node._father && node._father._gender != 'U') ||
            (this._mother && node._father && this._mother != node._father) ||
            (this._father && node._mother && this._father != node._mother);

        return notReversedParents && (hasConflictingDads || hasConflictingMoms);
    }
});