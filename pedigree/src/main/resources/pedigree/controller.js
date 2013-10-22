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
        document.observe("pedigree:person:newparent",          this.handlePersonNewParents);
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
        console.log("event: " + event.eventName + ", memo: " + stringifyObject(event.memo));
        var changeSet = editor.getGraph().redrawAll();
        editor.getGraphicsSet().applyChanges(changeSet, true);
    },
    
    handleClearGraph: function(event)
    {
        console.log("event: " + event.eventName + ", memo: " + stringifyObject(event.memo));
        var changeSet = editor.getGraph().clearAll();
        editor.getGraphicsSet().applyChanges(changeSet, true);        
        
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
                editor.getGraphicsSet().unmarkAll();
            
                for (var i = 0; i < disconnectedList.length; i++) {
                    var nextHighlight = disconnectedList[i];
                    editor.getGraphicsSet().getNode(nextHighlight).getGraphics().markPermanently();                
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
                    editor.getGraphicsSet().getNode(nextHighlight).getGraphics().unmark();                
                }          
            }
            
            if (changeSet) {
                editor.getGraphicsSet().applyChanges(changeSet, true);
                        
                if (!event.memo.noUndoRedo)
                    editor.getActionStack().addState( event );
            }
            
        } catch(err) {
            console.log("err: " + err);
        }          
    },
    
    handleSetProperty: function(event)
    {
        try {
        console.log("event: " + event.eventName + ", memo: " + stringifyObject(event.memo));
        var nodeID     = event.memo.nodeID;
        var properties = event.memo.properties;
        var undoEvent  = {"eventName": event.eventName, "memo": {"nodeID": nodeID, "properties": cloneObject(event.memo.properties)}};
                
        var node    = editor.getGraphicsSet().getNode(nodeID);
        var changed = false;
        
        var needUpdateAncestors = false;
        var needUpdateRelationship = false;
        
        for (propertySetFunction in properties)
            if (properties.hasOwnProperty(propertySetFunction)) {
                var propValue = properties[propertySetFunction];  
                               
                //console.log("attmepting to set property " + propertySetFunction + " to " + propValue);
                if (Controller._validatePropertyValue( nodeID, propertySetFunction, propValue)) {
                    //console.log("validated");
                    
                    // prepare undo event
                    propertyGetFunction =  propertySetFunction.replace("set","get");
                    var oldValue = node[propertyGetFunction]();
                    undoEvent.memo.properties[propertySetFunction] = oldValue;
                    if (oldValue != propValue) changed = true;
                        
                    node[propertySetFunction](propValue);
                }
                
                if (propertySetFunction == "setAdopted")
                    needUpdateAncestors = true;
                
                if (propertySetFunction == "setMonozygotic") {
                    needUpdateRelationship = true;
                    // need to update all other twins in the group
                    var allTwins = editor.getGraph().getAllTwinsSortedByOrder(nodeID);
                    for (var i = 0; i < allTwins.length; i++) {
                        var twin = allTwins[i];
                        if (twin == nodeID) continue;
                        var twinNode = editor.getGraphicsSet().getNode(twin);
                        twinNode.setMonozygotic(propValue);
                        var twinProperties = twinNode.getProperties();              
                        editor.getGraph().setProperties( twin, twinProperties );                        
                    }
                }
            }
        
        var allProperties = node.getProperties();              
        editor.getGraph().setProperties( nodeID, allProperties );
        
        if (needUpdateAncestors) {
            var changeSet = editor.getGraph().updateAncestors();
            editor.getGraphicsSet().applyChanges(changeSet, true);                    
        }
        
        if (needUpdateRelationship) {
            var relID = editor.getGraph().getParentRelationship(nodeID);
            var changeSet = {"moved": [relID]};
            editor.getGraphicsSet().applyChanges(changeSet, true);
        }
        
        editor.getNodeMenu().update();  // for example, user selected a wrong gender in the nodeMenu, which
                                        // gets reverted back - need to select the correct one in the nodeMenu as well
        
        console.log("event: " + event.eventName + ", memo: " + stringifyObject(event.memo));
        if (!event.memo.noUndoRedo && changed)
            editor.getActionStack().addState( event, undoEvent );
        
        } catch(err) {
            console.log("err: " + err);
        }        
    },

    handleModification: function(event)
    {
        try {
        console.log("event: " + event.eventName + ", memo: " + stringifyObject(event.memo));
        var nodeID        = event.memo.nodeID;
        var modifications = event.memo.modifications;
        
        var node = editor.getGraphicsSet().getNode(nodeID);
        
        //var allProperties = node.getProperties();
        
        for (modificationType in modifications)
            if (modifications.hasOwnProperty(modificationType)) {
                var modValue = modifications[modificationType];  
                                               
                if (modificationType == "addTwin") {
                    for (var i = 0; i < modValue; i++ ) {
                        var twinProperty = { "gender": "U" };
                        var changeSet = editor.getGraph().addTwin( nodeID, twinProperty );        
                        editor.getGraphicsSet().applyChanges(changeSet, true);
                    }
                }
                
                if (modificationType == "makePlaceholder") {
                    // TODO
                }                
            }
                             
        editor.getNodeMenu().update(editor.getNode(nodeID));
        
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
            editor.getController().handleSetProperty( { "memo": { "nodeID": personID, "properties": { "setAdopted": true } } } );                  
        }
                  
        try {
        var changeSet = editor.getGraph().assignParent(parentID, personID);
        editor.getGraphicsSet().applyChanges(changeSet, true);
        
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
        editor.getGraphicsSet().applyChanges(changeSet, true);
        
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
        
        if (editor.getGraph().isChildless(personID)) {
            var childParams = { "isAdopted": true };                  
        }        
        
        var changeSet = editor.getGraph().addNewRelationship(personID, childParams, preferLeft, numTwins);                
        editor.getGraphicsSet().applyChanges(changeSet, true);
        
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
        
        try
        {        
        var personID  = event.memo.personID;
        var partnerID = event.memo.partnerID;
        if (!editor.getGraph().isPerson(personID) || !editor.getGraph().isPerson(partnerID)) return;
        
        var childProperties = {};
        if (editor.getGraph().isChildless(personID) || editor.getGraph().isChildless(partnerID)) {
            childProperties = { "isAdopted": true };                  
        }
            
        // when partnering up a node with unknown gender with a node of known gender
        // change the unknown gender to the opposite of known
        var node1 = editor.getGraphicsSet().getNode(personID);
        var node2 = editor.getGraphicsSet().getNode(partnerID);
        
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
                
        var changeSet = editor.getGraph().assignPartner(personID, partnerID, childProperties);
        editor.getGraphicsSet().applyChanges(changeSet, true);
        
        if (!event.memo.noUndoRedo)
            editor.getActionStack().addState( event );
        
        } catch(err) {
            console.log("err: " + err);
        }           
    },    
        
    handleRelationshipNewChild: function(event)
    {
        console.log("event: " + event.eventName + ", memo: " + stringifyObject(event.memo));        
        
        var partnershipID = event.memo.partnershipID;
        if (!editor.getGraph().isRelationship(partnershipID)) return;
        
        var numTwins = event.memo.twins ? event.memo.twins : 1;        
        
        var childParams = cloneObject(event.memo.childParams);        
        if (editor.getGraph().isChildless(partnershipID))
            childParams["isAdopted"] = true;
        
        // TODO: twins & groups
        
        var changeSet = editor.getGraph().addNewChild(partnershipID, childParams, numTwins);                
        editor.getGraphicsSet().applyChanges(changeSet, true);
        
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


//var JSON = editor.getGraph().toJSON();
//console.log("JSON generated: " + JSON);
//editor.getGraphicsSet().clearGraph();
//editor.getSaveLoadEngine().createGraphFromSerializedData({"zzz": JSON});
//console.log("Changes: " + stringifyObject(changeSet));                
