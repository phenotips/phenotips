var Vertical = Class.create( {

    initialize: function(parents, child) {
        this._parents = parents;
        this._child = child;
    },

    getChild: function() {
        return this._child;
    },

    setChild: function(child) {
        this._child = child;
    },

    getParents: function() {
        return this._parents;
    },

    setParents: function(parents) {
        this._parents = parents;
    },

    getMother: function() {
        return this.getParents().getMother();
    },

    getFather: function() {
        return this.getParents().getFather();
    },

    remove: function() {
        this.getChild().setParents(null);
        this.getParents().getVerticals().indexOf(this) != -1 && this.getParents().removeVertical(this);
    }
});