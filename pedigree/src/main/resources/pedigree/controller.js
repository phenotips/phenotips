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
        //document.observe("pedigree:undo",                      this.handleUndo);
        //document.observe("pedigree:redo",                      this.handleRedo);
        document.observe("pedigree:node:remove",               this.handleRemove);
        document.observe("pedigree:node:setproperty",          this.handleSetProperty);
        document.observe("pedigree:person:drag:newparent",     this.handlePersonDragToNewParent);
        document.observe("pedigree:person:newparent",          this.handlePersonNewParents);
        document.observe("pedigree:person:newpartnerandchild", this.handlePersonNewPartnerAndChild);        
        document.observe("pedigree:partnership:newchild",      this.handleRelationshipNewChild);
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
    },
    
    handleRemove: function(event)
    {
        //TODO
        //get the list of affected nodes
        //editor.getGraph().getDisconnectedSetIfNodeRemoved(nodeID);
        // show message box
        // proceed if user agrees
        // ...
    },
    
    handleSetProperty: function(event)
    {
        console.log("event: " + event.eventName + ", memo: " + stringifyObject(event.memo));
        var nodeID     = event.memo.nodeID;
        var properties = event.memo.properties;
        
        var node = editor.getGraphicsSet().getNode(nodeID);
        
        for (propertySetFunction in properties)
            if (properties.hasOwnProperty(propertySetFunction)) {
                var propValue = properties[propertySetFunction];                
                node[propertySetFunction](propValue);
            }
        
        var allProperties = node.getProperties();              
        editor.getGraph().setProperties( nodeID, allProperties );
    },
    
    handlePersonDragToNewParent: function(event)
    {
        console.log("event: " + event.eventName + ", memo: " + stringifyObject(event.memo));
        
        var personID = event.memo.personID;
        var parentID = event.memo.parentID;
        if (!editor.getGraph().isPerson(personID) || !editor.getGraph().isValidID(parentID)) return;
                        
        var changeSet = editor.getGraph().assignParent(parentID, personID);
        editor.getGraphicsSet().applyChanges(changeSet, true);
        editor.getNode(nodeId).getGraphics().getHoverBox().hideParentHandle();
    },
    
    handlePersonNewParents: function(event)
    {   
        console.log("event: " + event.eventName + ", memo: " + stringifyObject(event.memo));        
        
        var personID = event.memo.personID;
        if (!editor.getGraph().isPerson(personID)) return;        
        
        var changeSet = editor.getGraph().addNewParents(personID);
        editor.getGraphicsSet().applyChanges(changeSet, true);
        editor.getNode(personID).getGraphics().getHoverBox().hideParentHandle();
    },
    
    handlePersonNewPartnerAndChild: function(event)
    {
        console.log("event: " + event.eventName + ", memo: " + stringifyObject(event.memo));        
        
        var personID    = event.memo.personID;
        if (!editor.getGraph().isPerson(personID)) return;        
        var preferLeft  = event.memo.preferLeft;
        var childParams = event.memo.childParams ? event.memo.childParams : {};                
                         
        var changeSet = editor.getGraph().addNewRelationship(personID, childParams, preferLeft);                
        editor.getGraphicsSet().applyChanges(changeSet, true);
    },
        
    handleRelationshipNewChild: function(event)
    {
        console.log("event: " + event.eventName + ", memo: " + stringifyObject(event.memo));        
        
        var partnershipID = event.memo.partnershipID;
        if (!editor.getGraph().isRelationship(partnershipID)) return;
        var childParams = event.memo.childParams;
        
        // TODO: twins & groups
        
        var changeSet = editor.getGraph().addNewChild(partnershipID, childParams);                
        editor.getGraphicsSet().applyChanges(changeSet, true);        
    }
});


//var JSON = editor.getGraph().toJSON();
//console.log("JSON generated: " + JSON);
//editor.getGraphicsSet().clearGraph();
//editor.getSaveLoadEngine().createGraphFromSerializedData({"zzz": JSON});
//console.log("Changes: " + stringifyObject(changeSet));                
