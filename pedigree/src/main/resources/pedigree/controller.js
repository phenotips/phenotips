/**
 * ...
 *
 * @class Controller
 * @constructor
 */
var Controller = Class.create({
    initialize: function() {     
        document.observe("pedigree:person:drag:newparent",     this.handlePersonDragToNewParent);
        document.observe("pedigree:person:newparent",          this.handlePersonNewParents);
        document.observe("pedigree:person:newpartnerandchild", this.handlePersonNewPartnerAndChild);        
        document.observe("pedigree:partnership:newchild",      this.handleRelationshipNewChild);
    },      
    
    handlePersonDragToNewParent: function(event)
    {
        console.log("event: " + stringifyObject(event));
        
        var personID = event.memo.personID;
        var parentID = event.memo.parentID;
        if (!editor.getGraph().isPerson(personID) || !editor.getGraph().isValidID(parentID)) return;
                
        var changeSet = editor.getGraph().assignParent(parentID, personID);
        editor.getGraphicsSet().applyChanges(changeSet, true);
        editor.getNode(nodeId).getGraphics().getHoverBox().hideParentHandle();
    },
    
    handlePersonNewParents: function(event)
    {   
        console.log("event: " + stringifyObject(event));
        
        var personID = event.memo.personID;
        if (!editor.getGraph().isPerson(personID)) return;        
        
        var changeSet = editor.getGraph().addNewParents(personID);
        editor.getGraphicsSet().applyChanges(changeSet, true);
        editor.getNode(personID).getGraphics().getHoverBox().hideParentHandle();
    },
    
    handlePersonNewPartnerAndChild: function(event)
    {
        console.log("event: " + stringifyObject(event));
        
        var personID    = event.memo.personID;
        if (!editor.getGraph().isPerson(personID)) return;        
        var preferLeft  = event.memo.preferLeft;
        var childParams = event.memo.childParams ? event.memo.childParams : {};                
                         
        var changeSet = editor.getGraph().addNewRelationship(personID, childParams, preferLeft);                
        editor.getGraphicsSet().applyChanges(changeSet, true);
    },
        
    handleRelationshipNewChild: function(event)
    {
        console.log("event: " + stringifyObject(event));
        
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
