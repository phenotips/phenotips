/**
 * UndoRedoManager is responsible for keeping track of user actions and providing an undo/redo functionality
 *
 * @class UndoRedoManager
 * @constructor
 */
define([
        "pedigree/model/helpers"
    ], function(
        Helpers
    ){
    var UndoRedoManager = Class.create({
        initialize: function() {
            this._currentState = 0;
            this._stack        = [];
            this._MAXUNDOSIZE  = 100;
            this._savedState   = "";

            // set of patient records that have been deleted and are now completely unavailable
            this._deletedPatients = {};
            // observe pation deletion/creation events to update this._deletedPatients accordingly
            document.observe("pedigree:patient:deleted", this.handlePatientDeleted.bind(this));
            document.observe("pedigree:patient:created", this.handlePatientCreated.bind(this));
        },

        handlePatientDeleted: function(event)
        {
            var removedID = event.memo.phenotipsPatientID;

            this._deletedPatients[removedID] = true;

            // Need to modify the undo/redo stack and remoe all references o the deleted patient:
            // 1) change all events to assign/unassign this PT patient to/from a node to a "no op" event
            // 2) remove all links to this patient from all stored states
            this._stack.forEach(function(state) {

                // 1)
                if (state.eventToGetToThisState
                    && state.eventToGetToThisState.eventName == "pedigree:node:modify"
                    && state.eventToGetToThisState.memo
                    && state.eventToGetToThisState.memo.modifications
                    && state.eventToGetToThisState.memo.modifications.trySetPhenotipsPatientId
                    && state.eventToGetToThisState.memo.modifications.trySetPhenotipsPatientId == removedID) {
                    state.eventToGetToThisState.eventName = "operationWithDeletedPatient";
                    delete state.eventToGetToThisState.memo;
                }

                // 2)
                var stateJSON = JSON.parse(state.serializedState);
                stateJSON.GG.forEach(function(nodeInternalJSON) {
                    if (nodeInternalJSON.prop && nodeInternalJSON.prop.phenotipsId
                        && nodeInternalJSON.prop.phenotipsId == removedID) {
                        delete nodeInternalJSON.prop.phenotipsId;
                    }
                });
                state.serializedState = JSON.stringify(stateJSON);
            });
        },

        handlePatientCreated: function(event)
        {
            delete this._deletedPatients[event.memo.phenotipsPatientID];
        },

        hasUnsavedChanges: function() {
            var state = this._getCurrentState();
            if (state == null) {
                return false;
            }
            if (this._savedState == state.serializedState) {
                return false;
            }
            return true;
        },

        hasRedo: function() {
            if (this._getNextState()) return true;
            return false;
        },

        hasUndo: function() {
            if (this._getPreviousState()) return true;
            return false;
        },

        addSaveEvent: function() {
            var state = this._getCurrentState();
            if (state == null) {
                return;
            }
            this._savedState = state.serializedState;
        },
        /**
         * Moves one state forward in the action stack
         *
         * @method redo
         */
        redo: function() {
            var nextState = this._getNextState();
            //console.log("Next state: " + Helpers.stringifyObject(nextState));
            if (!nextState) return;

            this._currentState++;
            if (nextState.eventToGetToThisState && nextState.eventToGetToThisState.hasOwnProperty("memo")) {
                var memo = nextState.eventToGetToThisState.memo;
                memo["noUndoRedo"] = true;  // so that this event is not added to the undo/redo stack again
                document.fire( nextState.eventToGetToThisState.eventName, memo );
            } else {
                editor.getSaveLoadEngine().createGraphFromSerializedData( nextState.serializedState, true /* do not re-add to undo/redo stack */,
                                                                          false /* do not center around proband */, null /* no callback */, "redo" );
            }
            document.fire("pedigree:historychange", null);
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
            //console.log("Current state: " + Helpers.stringifyObject(currentState));

            this._currentState--;
            if (currentState.eventToUndo) {
                var memo = currentState.eventToUndo.memo;
                memo["noUndoRedo"] = true; // so that this event is not added to the undo/redo stack again
                document.fire( currentState.eventToUndo.eventName, memo );
            } else {
                // no easy way - have to recreate the graph from serialization
                editor.getSaveLoadEngine().createGraphFromSerializedData( prevState.serializedState, true /* do not re-add to undo/redo stack */,
                                                                          false /* do not center around proband */, null /* no callback */, "undo");
            }
            document.fire("pedigree:historychange", null);
        },

        /**
         * Some operations should not be "undoable" and should be merged with the previous state which will
         * be re-done and un-done as an atomic event.
         *
         * This method takes the current state of pedigree and makes the last undo/redo history state be equal to this state.
         */
        updateLastState: function() {
            this._stack[this._currentState - 1].serializedState = editor.getSaveLoadEngine().serialize();
            this._stack[this._currentState - 1].eventToGetToThisState = null;
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
            if (this._currentState < this._size()) {
                this._stack.splice(this._currentState, this._stack.length - this._currentState);
            }

            // 2. let any extensions which should operate on any change do their modifications
            //
            //    If there are any modifications => need to ignore "eventToGetToThisState" and
            //    "eventToGoBack" and create a new serialization (so that undo/redo operations
            //    do not depend on extensions behaving consistently)
            var eventInfoForExtension = { "originalEvent": eventToGetToThisState };
            // convert specific implementation's details to a more standardized way
            if (eventToGetToThisState && (typeof eventToGetToThisState == "object") && eventToGetToThisState.hasOwnProperty("eventName")) {
                eventInfoForExtension.eventName = eventToGetToThisState.eventName;
            } else {
                eventInfoForExtension.eventName = "unknown";
            }
            if (eventToGetToThisState.eventName == "pedigree:node:setproperty") {
                eventInfoForExtension.propertiesChanged = Helpers.cloneObject(eventToGetToThisState.memo.properties);
                eventInfoForExtension.afectedNodeIDs = [ eventToGetToThisState.memo.nodeID ];
            }

            var extensionParameters = { "modificationEvent": eventInfoForExtension,
                                        "serializedState": serializedState };
            var extensionRunResult = editor.getExtensionManager().callExtensions("newPedigreeState", extensionParameters);
            if (extensionRunResult.pedigreeChanged) {
                eventToGetToThisState = null;
                eventToUndo = null;
                serializedState = null;
            }

            if (!serializedState) {
                serializedState = editor.getSaveLoadEngine().serialize();
            }
            //console.log("Serialized state: " + Helpers.stringifyObject(serializedState));

            var state = new State( serializedState, eventToGetToThisState, eventToUndo );

            // 3. push this new state to the array and increment the current index

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

            if (this._size() > this._MAXUNDOSIZE) {
                this._removeOldest();
            }

            document.fire( "pedigree:historychange", null );
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
            if (!event1.hasOwnProperty("memo") || !event2.hasOwnProperty("memo")) {
                return false;
            }
            if (!event1.memo.hasOwnProperty("nodeID") || !event2.memo.hasOwnProperty("nodeID") || event1.memo.nodeID != event2.memo.nodeID)
                return false;
            if (!event1.memo.hasOwnProperty("properties") || !event2.memo.hasOwnProperty("properties")) {
                return false;
            }
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
                console.log("[" + i + "] EventToState: " + Helpers.stringifyObject(this._stack[i].eventToGetToThisState) + "\n" +
                                    "    EventUndo: " + Helpers.stringifyObject(this._stack[i].eventToUndo) + "\n" +
                                    "    EventSerial: " + Helpers.stringifyObject(this._stack[i].serializedState));
            }
            console.log("------------");
        }
    });

    /**
     * State is used by @class UndoRedoManager to enable undo redo actions.
     *
     * @class State
     * @constructor
     */
    var State = Class.create({
      initialize: function( serializedState, eventToGetToThisState, eventToUndo ) {
          this.serializedState       = serializedState;
          this.eventToGetToThisState = eventToGetToThisState;
          this.eventToUndo           = eventToUndo;
      }
    });

    return UndoRedoManager;
});
