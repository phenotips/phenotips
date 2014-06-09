/**
 * ...
 *
 * @class Controller
 * @constructor
 */

// TODO: undo/redo in all handlers

var Controller = Class.create({
    initialize: function() {
        document.observe("pedigree:autolayout",                this.handleAutoLayout);
        document.observe("pedigree:graph:clear",               this.handleClearGraph);
        document.observe("pedigree:undo",                      this.handleUndo);
        document.observe("pedigree:redo",                      this.handleRedo);
        document.observe("pedigree:node:remove",               this.handleRemove);
        document.observe("pedigree:node:setproperty",          this.handleSetProperty);
        document.observe("pedigree:node:modify",               this.handleModification);
        document.observe("pedigree:person:drag:newparent",     this.handlePersonDragToNewParent);
        document.observe("pedigree:person:drag:newpartner",    this.handlePersonDragToNewPartner);
        document.observe("pedigree:person:drag:newsibling",    this.handlePersonDragToNewSibling);
        document.observe("pedigree:person:newparent",          this.handlePersonNewParents);
        document.observe("pedigree:person:newsibling",         this.handlePersonNewSibling);
        document.observe("pedigree:person:newpartnerandchild", this.handlePersonNewPartnerAndChild);
        document.observe("pedigree:partnership:newchild",      this.handleRelationshipNewChild);
    },

    handleUndo: function(event)
    {
        console.log("event: " + event.eventName + ", memo: " + stringifyObject(event.memo));
        editor.getActionStack().undo();
    },

    handleRedo: function(event)
    {
        console.log("event: " + event.eventName + ", memo: " + stringifyObject(event.memo));
        editor.getActionStack().redo();
    },

    handleAutoLayout: function(event)
    {
        try {
            console.log("event: " + event.eventName + ", memo: " + stringifyObject(event.memo));
            var changeSet = editor.getGraph().redrawAll();
            editor.getView().applyChanges(changeSet, true);

            if (!event.memo.noUndoRedo)
                editor.getActionStack().addState( event );
        } catch(err) {
            console.log("Autolayout error: " + err);
        }
    },

    handleClearGraph: function(event)
    {
        console.log("event: " + event.eventName + ", memo: " + stringifyObject(event.memo));
        var changeSet = editor.getGraph().clearAll();
        editor.getView().applyChanges(changeSet, true);

        editor.getWorkspace().centerAroundNode(0, false);

        if (!event.memo.noUndoRedo)
            editor.getActionStack().addState( event );
    },

    handleRemove: function(event)
    {
        try {
            console.log("event: " + event.eventName + ", memo: " + stringifyObject(event.memo));
            var nodeID = event.memo.nodeID;

            //get the list of affected nodes
            var disconnectedList = editor.getGraph().getDisconnectedSetIfNodeRemoved(nodeID);

            if (disconnectedList.length > 1)
                editor.getView().unmarkAll();

                for (var i = 0; i < disconnectedList.length; i++) {
                    var nextHighlight = disconnectedList[i];
                    editor.getView().getNode(nextHighlight).getGraphics().markPermanently();
                }

            var changeSet = null;
            // iff this is not an undo/redo action and more tha none nod eis affected =>
            // show message box && proceed only iff user agrees to remove all the affected nodes
            if (disconnectedList.length <= 1 || event.memo.hasOwnProperty("noUndoRedo") ||
                confirm('All highlighted nodes will be removed. Do you want to proceed?')) {
                changeSet = editor.getGraph().removeNodes(disconnectedList);
            }
            else if (disconnectedList.length > 1) {
                // no removal: unhighlight the modes. If removal did happen there is nothing to unhighlight by now
                for (var i = 0; i < disconnectedList.length; i++) {
                    var nextHighlight = disconnectedList[i];
                    editor.getView().getNode(nextHighlight).getGraphics().unmark();
                }
            }

            if (changeSet) {
                editor.getView().applyChanges(changeSet, true);

                changeSet = editor.getGraph().improvePosition();
                editor.getView().applyChanges(changeSet, true);

                if (!event.memo.noUndoRedo)
                    editor.getActionStack().addState( event );
            }

        } catch(err) {
            console.log("Remove error: " + err);
        }
    },

    handleSetProperty: function(event)
    {
        console.log("event: " + event.eventName + ", memo: " + stringifyObject(event.memo));
        var nodeID     = event.memo.nodeID;
        var properties = event.memo.properties;
        var undoEvent  = {"eventName": event.eventName, "memo": {"nodeID": nodeID, "properties": cloneObject(event.memo.properties)}};

        var node    = editor.getView().getNode(nodeID);
        var changed = false;

        var twinUpdate = undefined;
        var needUpdateAncestors = false;
        var needUpdateRelationship = false;

        var changedValue = false;

        for (var propertySetFunction in properties) {
            if (properties.hasOwnProperty(propertySetFunction)) {
                var propValue = properties[propertySetFunction];

                //console.log("attmepting to set property " + propertySetFunction + " to " + propValue);
                if (!Controller._validatePropertyValue( nodeID, propertySetFunction, propValue)) continue;

                //console.log("validated");
                // prepare undo event
                var propertyGetFunction =  propertySetFunction.replace("set","get");
                var oldValue = node[propertyGetFunction]();
                if (oldValue == propValue) continue;

                if (Object.prototype.toString.call(oldValue) === '[object Array]') {
                    oldValue = oldValue.slice(0);
                }

                undoEvent.memo.properties[propertySetFunction] = oldValue;

                // sometimes UNDO includes more then the property itself: e.g. changing life status
                // from "dead" to "alive" also clears the death date. Need to add it to the "undo" event
                if (propertySetFunction == "setLifeStatus") {
                    undoEvent.memo.properties["setDeathDate"]    = node.getDeathDate();
                    undoEvent.memo.properties["setGestationAge"] = node.getGestationAge();
                    undoEvent.memo.properties["setBirthDate"]    = node.getBirthDate();
                    undoEvent.memo.properties["setAdopted"]      = node.getAdopted();
                }
                if (propertySetFunction == "setDeathDate") {
                    undoEvent.memo.properties["setLifeStatus"] = node.getLifeStatus();
                }
                if (propertySetFunction == "setChildlessStatus") {
                    undoEvent.memo.properties["setChildlessReason"] = node.getChildlessReason();
                }
                if (propertySetFunction == "setDisorders") {
                    undoEvent.memo.properties["setCarrierStatus"] = node.getCarrierStatus();
                }

                node[propertySetFunction](propValue);

                if (propertySetFunction == "setDisorders") {
                    var newDisorders = node[propertyGetFunction]();
                    if (JSON.stringify(oldValue) == JSON.stringify(newDisorders)) continue;
                }

                changedValue = true;

                if (propertySetFunction == "setLastName") {
                    if (PedigreeEditor.attributes.propagateLastName) {
                        if (node.getGender(nodeID) == 'M') {
                            if (propValue != "") {
                                // propagate last name as "last name at birth" to all descendants (by the male line)
                                Controller._propagateLastNameAtBirth(nodeID, propValue, oldValue);
                                undoEvent = null; // there is no easy undo other than just remember the previous graph state
                            }
                        }
                    }
                }

                if (propertySetFunction == "setGender") {
                    if (node.getMonozygotic()) {
                        if (!twinUpdate) twinUpdate = {};
                        twinUpdate[propertySetFunction] = propValue;
                    }
                }

                if (propertySetFunction == "setAdopted") {
                    needUpdateAncestors = true;
                    if (!twinUpdate) twinUpdate = {};
                    twinUpdate[propertySetFunction] = propValue;
                }

                if (propertySetFunction == "setMonozygotic") {
                    needUpdateRelationship = true;
                    if (!twinUpdate) twinUpdate = {};
                    twinUpdate[propertySetFunction] = propValue;
                }

                if (propertySetFunction == "setConsanguinity" || propertySetFunction == "setBrokenStatus") {
                    // this updates the relationship lines, as well as any lines
                    // crossed by the relationship llines to maintain correct crossing graphics
                    needUpdateRelationship = true;
                }
            }
        }

        // some properties should be the same for all the twins. If one of those
        // was changed, need to update all the twins
        if (twinUpdate) {
            var allTwins = editor.getGraph().getAllTwinsSortedByOrder(nodeID);
            for (propertySetFunction in twinUpdate) {
                if (twinUpdate.hasOwnProperty(propertySetFunction)) {
                    var propValue = twinUpdate[propertySetFunction];

                    for (var i = 0; i < allTwins.length; i++) {
                        var twin = allTwins[i];
                        if (twin == nodeID) continue;
                        var twinNode = editor.getView().getNode(twin);
                        twinNode[propertySetFunction](propValue);
                        var twinProperties = twinNode.getProperties();
                        console.log("Setting twin properties: " + stringifyObject(twinProperties));
                        editor.getGraph().setProperties( twin, twinProperties );
                    }
                }
            }
        }

        var allProperties = node.getProperties();
        editor.getGraph().setProperties( nodeID, allProperties );

        if (needUpdateAncestors) {
            var changeSet = editor.getGraph().updateAncestors();
            editor.getView().applyChanges(changeSet, true);
        }

        if (needUpdateRelationship) {
            var relID = editor.getGraph().isRelationship(nodeID) ? nodeID : editor.getGraph().getParentRelationship(nodeID);
            var changeSet = {"moved": [relID]};
            editor.getView().applyChanges(changeSet, true);
        }

        editor.getNodeMenu().update();  // for example, user selected a wrong gender in the nodeMenu, which
                                        // gets reverted back - need to select the correct one in the nodeMenu as well

        //console.log("event: " + event.eventName + ", memo: " + stringifyObject(event.memo));
        //console.log("Undo event: " + stringifyObject(undoEvent));
        if (!event.memo.noUndoRedo && changedValue)
            editor.getActionStack().addState( event, undoEvent );
    },

    handleModification: function(event)
    {
        try {
        console.log("event: " + event.eventName + ", memo: " + stringifyObject(event.memo));
        var nodeID        = event.memo.nodeID;
        var modifications = event.memo.modifications;

        var node = editor.getView().getNode(nodeID);

        //var allProperties = node.getProperties();

        for (modificationType in modifications)
            if (modifications.hasOwnProperty(modificationType)) {
                var modValue = modifications[modificationType];

                if (modificationType == "addTwin") {
                    var numNewTwins = modValue - 1; // current node is one of the twins, so need to create one less
                    for (var i = 0; i < numNewTwins; i++ ) {
                        var twinProperty = { "gender": node.getGender() };
                        var changeSet = editor.getGraph().addTwin( nodeID, twinProperty );
                        editor.getView().applyChanges(changeSet, true);
                    }
                    node.assignProperties(editor.getGraph().getProperties(nodeID));
                }

                if (modificationType == "makePlaceholder") {
                    // TODO
                }
            }

        if (!event.memo.noUndoRedo)
            editor.getActionStack().addState( event );

        } catch(err) {
            console.log("err: " + err);
        }
    },

    handlePersonDragToNewParent: function(event)
    {
        console.log("event: " + event.eventName + ", memo: " + stringifyObject(event.memo));

        var personID = event.memo.personID;
        var parentID = event.memo.parentID;
        if (!editor.getGraph().isPerson(personID) || !editor.getGraph().isValidID(parentID)) return;

        if (editor.getGraph().isChildless(parentID)) {
            editor.getController().handleSetProperty( { "memo": { "nodeID": personID, "properties": { "setAdopted": true }, "noUndoRedo": true } } );
        }

        try {
        var changeSet = editor.getGraph().assignParent(parentID, personID);
        editor.getView().applyChanges(changeSet, true);

        if (changeSet.moved.indexOf(personID) != -1)
            editor.getWorkspace().centerAroundNode(personID, true);

        if (!event.memo.noUndoRedo)
            editor.getActionStack().addState( event );

        } catch(err) {
            console.log("err: " + err);
        }
    },

    handlePersonNewParents: function(event)
    {
        console.log("event: " + event.eventName + ", memo: " + stringifyObject(event.memo));

        var personID = event.memo.personID;
        if (!editor.getGraph().isPerson(personID)) return;

        var changeSet = editor.getGraph().addNewParents(personID);
        editor.getView().applyChanges(changeSet, true);

        if (!event.memo.noUndoRedo)
            editor.getActionStack().addState( event );

        return changeSet["new"][0]; // new relationship
    },

    handlePersonNewSibling: function(event)
    {
        console.log("event: " + event.eventName + ", memo: " + stringifyObject(event.memo));

        // { "personID": id, "childParams": data.params.parameters, "preferLeft": false };
        var personID    = event.memo.personID;
        var childParams = event.memo.childParams ? cloneObject(event.memo.childParams) : {};
        var numTwins    = event.memo.twins ? event.memo.twins : 1;
        var numPersons  = event.memo.groupSize ? event.memo.groupSize : 0;

        var parentRelationship = editor.getGraph().getParentRelationship(personID);

        if (parentRelationship === null) {
            // need to add new parents
            parentRelationship = editor.getController().handlePersonNewParents( { "memo": { "personID": personID, "noUndoRedo": true } } );
        }

        if (event.memo.twins) {
            var nextEvent = { "nodeID": personID, "modifications": { "addTwin": event.memo.twins }, "noUndoRedo": true };
            editor.getController().handleModification( { "memo": nextEvent } );
        }
        else
        {
            var nextEvent = { "partnershipID": parentRelationship, "childParams": childParams, "noUndoRedo": true };
            if (event.memo.groupSize)
                nextEvent["groupSize"] = event.memo.groupSize;

            editor.getController().handleRelationshipNewChild( { "memo": nextEvent } );
        }

        if (!event.memo.noUndoRedo)
            editor.getActionStack().addState( event );
    },

    handlePersonDragToNewSibling: function(event)
    {
        console.log("event: " + event.eventName + ", memo: " + stringifyObject(event.memo));

        var sibling1 = event.memo.sibling1ID;
        var sibling2 = event.memo.sibling2ID;

        var parentRelationship = editor.getGraph().getParentRelationship(sibling1);
        if (parentRelationship == null)
            parentRelationship = editor.getGraph().getParentRelationship(sibling2);

        if (parentRelationship === null) {
            // need to add new parents
            parentRelationship = editor.getController().handlePersonNewParents( { "memo": { "personID": sibling1, "noUndoRedo": true } } );
        }

        if (editor.getGraph().getParentRelationship(sibling2) != parentRelationship) {
            // assign sibling 2 to this relationship: covers the case when none have parents or sibling1 has parents
            editor.getController().handlePersonDragToNewParent( { "memo": { "personID": sibling2, "parentID": parentRelationship, "noUndoRedo": true } } );
        } else {
            // assign sibling 1 to this relationship
            editor.getController().handlePersonDragToNewParent( { "memo": { "personID": sibling1, "parentID": parentRelationship, "noUndoRedo": true } } );
        }

        if (!event.memo.noUndoRedo)
            editor.getActionStack().addState( event );
    },

    handlePersonNewPartnerAndChild: function(event)
    {
        var timer = new Timer();

        try
        {
        console.log("event: " + event.eventName + ", memo: " + stringifyObject(event.memo));

        var personID    = event.memo.personID;
        if (!editor.getGraph().isPerson(personID)) return;
        var preferLeft  = event.memo.preferLeft;
        var childParams = event.memo.childParams ? cloneObject(event.memo.childParams) : {};
        var numTwins    = event.memo.twins ? event.memo.twins : 1;
        var numPersons  = event.memo.groupSize ? event.memo.groupSize : 0;

        if (editor.getGraph().isChildless(personID)) {
            childParams["isAdopted"] = true;
        }

        if (numPersons > 0) {
            childParams["numPersons"] = numPersons;
        }

        var changeSet = editor.getGraph().addNewRelationship(personID, childParams, preferLeft, numTwins);
        editor.getView().applyChanges(changeSet, true);

        if (!event.memo.noUndoRedo)
            editor.getActionStack().addState( event );

        } catch(err) {
            console.log("err: " + err);
        }

        timer.printSinceLast("=== Total new partner+child runtime: ");
    },

    handlePersonDragToNewPartner: function(event)
    {
        console.log("event: " + event.eventName + ", memo: " + stringifyObject(event.memo));

        var personID  = event.memo.personID;
        var partnerID = event.memo.partnerID;
        if (!editor.getGraph().isPerson(personID) || !editor.getGraph().isPerson(partnerID)) return;

        var childProperties = {};
        if (editor.getGraph().isChildless(personID) || editor.getGraph().isChildless(partnerID)) {
            childProperties = { "isAdopted": true };
        }

        // when partnering up a node with unknown gender with a node of known gender
        // change the unknown gender to the opposite of known
        var node1 = editor.getView().getNode(personID);
        var node2 = editor.getView().getNode(partnerID);

        if (node1.getGender() == "U" && node2.getGender() != "U") {
            var gender1 = editor.getGraph().getOppositeGender(partnerID);
            node1.setGender(gender1);
            editor.getGraph().setProperties( personID, node1.getProperties() );
        }
        else if (node1.getGender() != "U" && node2.getGender() == "U") {
            var gender2 = editor.getGraph().getOppositeGender(personID);
            node2.setGender(gender2);
            editor.getGraph().setProperties( partnerID, node2.getProperties() );
        }

        // TODO: propagate change of gender down the partnership chain

        var changeSet = editor.getGraph().assignPartner(personID, partnerID, childProperties);
        editor.getView().applyChanges(changeSet, true);

        if (!event.memo.noUndoRedo)
            editor.getActionStack().addState( event );
    },

    handleRelationshipNewChild: function(event)
    {
        console.log("event: " + event.eventName + ", memo: " + stringifyObject(event.memo));

        var partnershipID = event.memo.partnershipID;
        if (!editor.getGraph().isRelationship(partnershipID)) return;

        var numTwins = event.memo.twins ? event.memo.twins : 1;

        var childParams = cloneObject(event.memo.childParams);
        if (editor.getGraph().isChildless(partnershipID)) {
            childParams["isAdopted"] = true;
        }

        var numPersons = event.memo.groupSize ? event.memo.groupSize : 0;
        if (numPersons > 0) {
            childParams["numPersons"] = numPersons;
        }

        var changeSet = editor.getGraph().addNewChild(partnershipID, childParams, numTwins);
        editor.getView().applyChanges(changeSet, true);

        if (!event.memo.noUndoRedo)
            editor.getActionStack().addState( event );
    }
});

Controller._validatePropertyValue = function( nodeID, propertySetFunction, propValue)
{
    if (propertySetFunction == "setGender") {
        var possibleGenders = editor.getGraph().getPossibleGenders(nodeID);
        //console.log("valid genders: " + stringifyObject(possibleGenders));
        return possibleGenders[propValue];
    }
    return true;
}

Controller._propagateLastNameAtBirth = function( parentID, parentLastName, changeIfEqualTo )
{
    var children = editor.getGraph().getAllChildren(parentID);

    for (var i = 0; i < children.length; i++) {
        var childID   = children[i];
        var childNode = editor.getView().getNode(childID);

        if (childNode.getLastName() == "" &&
            (childNode.getLastNameAtBirth() == "" || childNode.getLastNameAtBirth() == changeIfEqualTo)) {
            childNode.setLastNameAtBirth(parentLastName);
            var allProperties = childNode.getProperties();
            editor.getGraph().setProperties( childID, allProperties );
            if (childNode.getGender() == 'M') {
                Controller._propagateLastNameAtBirth(childID, parentLastName, changeIfEqualTo);
            }
        }
    }
}

