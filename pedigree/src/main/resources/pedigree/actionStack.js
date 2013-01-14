var ActionStack = Class.create({
    initialize: function() {
        this._index = 0;
        this._stack = [];
        this._maxMarkerID = 1;
    },

    getStack: function() {
        return this._stack;
    },

    setStack: function(newStack) {
        this._stack = newStack;
    },

    getIndex: function() {
        return this._index;
    },

    setIndex: function(index) {
        (index >= 0) && (this._index = index);
    },

    getMaxMarkerID: function() {
        return this._maxMarkerID;
    },

    setMaxMarkerID: function(id) {
        this._maxMarkerID = id;
    },

    redo: function() {
        document.fire("pedigree:actionEvent", {actionType: "redo"})
        var action = this.getStack()[this.getIndex()];
        if(action) {
            this.setIndex(this.getIndex() + 1)
            if (action.redo) {
                action.redo(action);
            }
            else if(action.markerType == "start") {
                var targetID = action.id;
                while(this.getStack()[this.getIndex()] && this.getStack()[this.getIndex()].markerType != "end"
                    && this.getStack()[this.getIndex()].id != targetID) {
                    this.redo();
                }
                this.getStack()[this.getIndex()] && this.setIndex(this.getIndex() + 1);
            }
        }
    },

    undo: function() {
        document.fire("pedigree:actionEvent", {actionType: "undo"})
        if(this.getIndex() > 0) {
            this.setIndex(this.getIndex() - 1);
            var o = this.getStack()[this.getIndex()];
            if (o && o.undo) {
                o.undo(o);
            }
            else if(o.markerType == "end") {
                var targetID = o.id;
                while(this.getStack()[this.getIndex()-1].markerType != "start"
                                    && this.getStack()[this.getIndex()-1].id != targetID && this.getIndex() > 0) {
                    this.undo();
                }
                this.setIndex(this.getIndex() - 1);
            }
        }
    },

    push: function(el) {
        if(el && typeof(el["undo"]) == 'function' && typeof(el["redo"]) == 'function') {
            document.fire("pedigree:actionEvent", {actionType: "push"})
            this.setStack(this.getStack().splice(0, this.getIndex()));
            this.setIndex(this.getIndex() + 1);
            this.getStack().push(el);
        }
    },

    pushStartMarker: function() {
        this.setIndex(this.getIndex() + 1);
        var markerID = this.getMaxMarkerID() + 1;
        this.setMaxMarkerID(markerID);
        this.getStack().push({markerType: "start", id: markerID});
        return markerID;
    },

    pushEndMarker: function(markerID) {
        this.setIndex(this.getIndex() + 1);
        this.getStack().push({markerType: "end", id: markerID});
        return markerID;
    },

    pop: function() {
        this.setStack(this.getStack().splice(0, this.getIndex() - 1));
        this.setIndex(this.getIndex() - 1);
    },

    replace: function(el) {
        if(el && typeof(el["undo"]) == 'function' && typeof(el["redo"]) == 'function') {
            this.setStack(this.getStack().splice(0, this.getIndex()-1));
            this.getStack().push(el);
        }
    },

    peek: function() {
        return this.size() == 0 ? null : this.getStack()[this.size() - 1];
    },

    size: function() {
        return this.getStack().length;
    }
});