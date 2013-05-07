/**
 * ActionStack is responsible for keeping track of user actions and providing an undo/redo functionality
 *
 * @class ActionStack
 * @constructor
 */
var ActionStack = Class.create({
    initialize: function() {
        this._index = 0;
        this._stack = [];
        this._maxMarkerID = 1;
    },

    /**
     * Returns the array implementation of the action stack
     *
     * @method getStack
     * @return {Array}
     */
    getStack: function() {
        return this._stack;
    },

    /**
     * Replaces the current stack with newStack
     *
     * @method setStack
     * @param {Array} newStack
     */
    setStack: function(newStack) {
        this._stack = newStack;
    },

    /**
     * Returns the index of the current state of the application
     *
     * @method getIndex
     * @return l{Number}
     */
    getIndex: function() {
        return this._index;
    },

    /**
     * Changes the index of the current state of the application to newIndex
     *
     * @method setIndex
     * @param {Number} newIndex
     */
    setIndex: function(newIndex) {
        (newIndex >= 0) && (this._index = newIndex);
    },

    /**
     * Moves one state forward in the action stack
     *
     * @method redo
     */
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

    /**
     * Moves one state backwards in the action stack
     *
     * @method undo
     */
    undo: function() {
        document.fire("pedigree:actionEvent", {actionType: "undo"})
        if(this.getIndex() > 0) {
            this.setIndex(this.getIndex() - 1);
            var action = this.getStack()[this.getIndex()];
            if(action) {
                if (action.undo) {
                    action.undo(action);
                }
                else if(action.markerType == "end") {
                    var targetID = action.id;
                    while(this.getStack()[this.getIndex()-1].markerType != "start"
                        && this.getStack()[this.getIndex()-1].id != targetID && this.getIndex() > 0) {
                        this.undo();
                    }
                    this.setIndex(this.getIndex() - 1);
                }
            }
        }
    },

    /**
     * Pushes a new state to the front of the action stack
     *
     * @method push
     * @param el Such that el.undo and el.redo are functions
     */
    push: function(el) {
        if(el && typeof(el["undo"]) == 'function' && typeof(el["redo"]) == 'function') {
            document.fire("pedigree:actionEvent", {actionType: "push"})
            this.setStack(this.getStack().splice(0, this.getIndex()));
            this.setIndex(this.getIndex() + 1);
            this.getStack().push(el);
        }
    },

    /**
     * Starts an action grouping in the stack
     *
     * @method pushStartMarker
     * @return {Number} The id of the action group
     */
    pushStartMarker: function() {
        this.setIndex(this.getIndex() + 1);
        var markerID = this.getMaxMarkerID() + 1;
        this.setMaxMarkerID(markerID);
        this.getStack().push({markerType: "start", id: markerID});
        return markerID;
    },

    /**
     * Closes action grouping with id markerID in the stack
     *
     * @method pushEndMarker
     * @param {Number} markerID ID of the action group
     */
    pushEndMarker: function(markerID) {
        this.setIndex(this.getIndex() + 1);
        this.getStack().push({markerType: "end", id: markerID});
    },

    /**
     * Returns ID of the action group with the largest ID
     *
     * @method getMaxMarkerID
     * @return {Number}
     */
    getMaxMarkerID: function() {
        return this._maxMarkerID;
    },

    /**
     * Sets newID as the largest action group ID
     *
     * @method setMaxMarkerID
     */
    setMaxMarkerID: function(newID) {
        this._maxMarkerID = newID;
    },

    /**
     * Pops the front element of the stack
     *
     * @method pop
     */
    pop: function() {
        this.setStack(this.getStack().splice(0, this.getIndex() - 1));
        this.setIndex(this.getIndex() - 1);
    },

    /**
     * Returns the front element of the stack
     *
     * @method peek
     * @return {null|Object}
     */
    peek: function() {
        return this.size() == 0 ? null : this.getStack()[this.size() - 1];
    },

    /**
     * Returns the number of elements in the stack
     *
     * @method size
     * @return {Number}
     */
    size: function() {
        return this.getStack().length;
    }
});