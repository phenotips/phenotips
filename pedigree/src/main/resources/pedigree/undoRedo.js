/**
 * ActionStack is responsible for keeping track of user actions and providing an undo/redo functionality
 *
 * @class ActionStack
 * @constructor
 */
var ActionStack = Class.create({
    initialize: function() {
        this._currentState = 0;
        this._stack        = [];
        this._MAXUNDOSIZE  = 100;
    },

    /**
     * Moves one state forward in the action stack
     *
     * @method redo
     */
    redo: function() {
        var nextState = this._getNextState();        
        //console.log("Next state: " + stringifyObject(nextState));
        if (!nextState) return;
        
        if (nextState.eventToGetToThisState) {
            var memo = nextState.eventToGetToThisState.memo;
            memo["noUndoRedo"] = true;  // so that this event is not added to the undo/redo stack again
            document.fire( nextState.eventToGetToThisState.eventName, memo );
            this._currentState++;
            return;
        }
        
        editor.getSaveLoadEngine().createGraphFromSerializedData( nextState.serializedState, true /* do not re-add to undo/redo stack */ );
        this._currentState++;
    },

    /**
     * Moves one state backwards in the action stack
     *
     * @method undo
     */
    undo: function() {
        var prevState = this._getPreviousState();
        if(!prevState) return;
        
        // it may be more efficient to undo the current state instead of full prev state restore
        var currentState = this._getCurrentState();   
        //console.log("Current state: " + stringifyObject(currentState));
        if (currentState.eventToUndo) {
            var memo = currentState.eventToUndo.memo;
            memo["noUndoRedo"] = true; // so that this event is not added to the undo/redo stack again
            document.fire( currentState.eventToUndo.eventName, memo );
            this._currentState--;
            return;                
        }
            
        // no easy way - have to recreate the graph from serialization
        editor.getSaveLoadEngine().createGraphFromSerializedData( prevState.serializedState, true /* do not re-add to undo/redo stack */);
        this._currentState--;
    },
    
    /**
     * Pushes a new state to the end of the action stack
     * 
     *   eventToGetToThisState - optional. Event which should bring the graph from the previous state to this tsate
     *   eventToGoBack         - optional. Event which should bring the graph back to the previous state
     *   serializedState       - optional. Serialized state of the graph as accepted by the load() funciton.
     *                           may only be used when one of the events is not provided. Will be generated
     *                           automatically when needed if not provided.
     *   
     * If one of the events is not provided a complete serializatiomn of the graph will be used to transition
     * in that direction, which is less efficient (slower/requires more memory for state storage). 
     *
     * @method addState
     */
    addState: function( eventToGetToThisState, eventToUndo, serializedState ) {
       //this._debug_print_states();
       
        // 1. remove all states after current state (i.e. all "redo" states) -
        //    they are replaced by the current chain of states starting with this state 
        if (this._currentState < this._size())
            this._stack.splice(this._currentState, this._stack.length - this._currentState);

        if (!serializedState) {
            serializedState = editor.getSaveLoadEngine().serialize();
        }
        //console.log("Serialized state: " + stringifyObject(serializedState));
        
        //if (!eventToGetToThisState && !serializedState)
        //    serializedState = editor.getSaveLoadEngine().serialize();
        //
        //if (!eventToUndo && this._currentState > 0) {
        //    // no event procided to undo this action AND have a current state:
        //    // => current state needs to have a serialization
        //    .. TODO
        //}
        
        var state = new State( serializedState, eventToGetToThisState, eventToUndo );
                
        // 2. push this new state to the array and increment the current index
        
        // spcial case: consequtive name property changes are combined into one property change        
        var currentState = this._getCurrentState();
        if (eventToGetToThisState &&             
             currentState && currentState.eventToGetToThisState &&
             currentState.eventToGetToThisState.eventName == "pedigree:node:setproperty" &&
             this._combinableEvents(currentState.eventToGetToThisState, eventToGetToThisState) ) {
            //console.log("[UNDOREDO] combining state changes");
            currentState.eventToGetToThisState = eventToGetToThisState;
            currentState.serializedState       = serializedState;
            //this._debug_print_states();
            return;
        }
        
        this._addNewest(state);
        
        if (this._size() > this._MAXUNDOSIZE)
            this._removeOldest();
        
        //this._debug_print_states();
    },

    /**
     * Returns true iff undo/redo should combine event1 and event2,
     * e.g. name change from "some_old_value" to "Abc" and then to "Abcd" will be combined into
     *      one name chnage from "some_old_value" to "Abcd"
     *
     * @method _size
     * @return {Number}
     */    
    _combinableEvents: function ( event1, event2 ) {
        if (!event1.memo.hasOwnProperty("nodeID") || !event2.memo.hasOwnProperty("nodeID") || event1.memo.nodeID != event2.memo.nodeID)
            return false;        
        if (event1.memo.properties.hasOwnProperty("setFirstName") &&
            event2.memo.properties.hasOwnProperty("setFirstName") )
            return true;
        if (event1.memo.properties.hasOwnProperty("setLastName") &&
            event2.memo.properties.hasOwnProperty("setLastName") )
            return true;
        if (event1.memo.properties.hasOwnProperty("setLastNameAtBirth") &&
            event2.memo.properties.hasOwnProperty("setLastNameAtBirth") )
            return true;
        if (event1.memo.properties.hasOwnProperty("setComments") &&
            event2.memo.properties.hasOwnProperty("setComments") )
            return true;
        if (event1.memo.properties.hasOwnProperty("setChildlessReason") &&
            event2.memo.properties.hasOwnProperty("setChildlessReason") )
            return true;                        
        return false;
    },

    /**
     * Returns the number of elements in the stack
     *
     * @method _size
     * @return {Number}
     */
    _size: function() {
        return this._stack.length;
    },
    
    /**
     * Adds the given state as the latest state in the sequence
     *
     * @method _addNewest
     */    
    _addNewest: function(state) {
        this._stack.push(state);        
        this._currentState++;        
    },
    
    /**
     * Removes the front element of the stack (i.e. the oldest stored state)
     *
     * @method _removeOldest
     */
    _removeOldest: function() {
        this._stack.splice(0, 1);
        this._currentState--;
    },

    /**
     * Returns the current state
     *
     * @method _getCurrentState
     * @return {null|Object}
     */
    _getCurrentState: function() {
        return (this._size() == 0 || this._currentState == 0) ? null : this._stack[this._currentState - 1];
    },

    /**
     * Returns the next state
     *
     * @method _getNextState
     * @return {null|Object}
     */    
    _getNextState: function() {
        return (this._size() <= 1 || this._currentState >= this._size()) ? null : this._stack[this._currentState];
    },    

    /**
     * Returns the previous state
     *
     * @method _getPreviousState
     * @return {null|Object}
     */    
    _getPreviousState: function() {
        return (this._size() == 1 || this._currentState <= 1) ? null : this._stack[this._currentState - 2];
    },
    
    _debug_print_states: function() {
        console.log("------------");
        for (var i = 0; i < this._stack.length; i++) {
            console.log("[" + i + "] EventToState: " + stringifyObject(this._stack[i].eventToGetToThisState) + "\n" +
                                "    EventUndo: " + stringifyObject(this._stack[i].eventToUndo) + "\n" +
                                "    EventSerial: " + stringifyObject(this._stack[i].serializedState));
        }
        console.log("------------");
    }
});


var State = Class.create({
    initialize: function( serializedState, eventToGetToThisState, eventToUndo ) {
        this.serializedState       = serializedState; 
        this.eventToGetToThisState = eventToGetToThisState;
        this.eventToUndo           = eventToUndo;        
    }
});