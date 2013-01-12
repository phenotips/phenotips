var ActionStack = Class.create({
    initialize: function() {
        this._index = 0;
        this._stack = [];
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

    redo: function() {
        var o = this.getStack()[this.getIndex()];
        if (o && o.redo) {
            o.redo();
            this.setIndex(this.getIndex() + 1)
        }
    },

    undo: function() {
        if(this.getIndex() > 0) {
            var o = this.getStack()[this.getIndex() - 1];
            if (o && o.undo) {
                o.undo();
                this.setIndex(this.getIndex() - 1)
            }
        }
    },

    push: function(el) {
        if(el && typeof(el["undo"]) == 'function' && typeof(el["redo"]) == 'function') {
            this.setStack(this.getStack().splice(0, this.getIndex()));
            this.setIndex(this.getIndex() + 1);
            this.getStack().push(el);
        }
    }
});