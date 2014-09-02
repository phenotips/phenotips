/**
 * PersonHoverbox is a class for all the UI elements and graphics surrounding a Person node and
 * its labels. This includes the box that appears around the node when it's hovered by a mouse, as
 * well as the handles used for creating connections and creating new nodes.
 *
 * @class PersonHoverbox
 * @extends AbstractHoverbox
 * @constructor
 * @param {Person} personNode The person for whom this hoverbox is being drawn.
 * @param {Number} centerX The X coordinate for the center of the hoverbox
 * @param {Number} centerY The Y coordinate for the center of the hoverbox
 * @param {Raphael.st} nodeShapes All shapes associated with the person node
 */

var PersonHoverbox = Class.create(AbstractHoverbox, {

    initialize: function($super, personNode, centerX, centerY, nodeShapes) {
        var radius = PedigreeEditor.attributes.personHoverBoxRadius;        
        $super(personNode, -radius, -radius, radius * 2, radius * 2, centerX, centerY, nodeShapes);                
    },

    /**
     * Creates the handles used in this hoverbox
     *
     * @method generateHandles
     * @return {Raphael.st} A set of handles
     */
    generateHandles: function($super) {
        if (this._currentHandles !== null) return;        
        $super();               
        
        //var timer = new Timer();
        
        var x          = this.getNodeX();
        var y          = this.getNodeY();
        var node       = this.getNode();
        var nodeShapes = node.getGraphics().getGenderGraphics().flatten();
                
        editor.getPaper().setStart();
        
        if (PedigreeEditor.attributes.newHandles) {            
            var strokeWidth = editor.getWorkspace().getSizeNormalizedToDefaultZoom(PedigreeEditor.attributes.handleStrokeWidth);
            
            var partnerGender = 'U';
            if (node.getGender() == 'F') partnerGender = 'M';
            if (node.getGender() == 'M') partnerGender = 'F';
                       
            // static part (2 lines: going above the node + going to the left)
            var splitLocationY = y-PedigreeEditor.attributes.personHandleBreakY-4;
            var path = [["M", x, y],["L", x, splitLocationY], ["L", x-PedigreeEditor.attributes.personSiblingHandleLengthX, splitLocationY]];
            editor.getPaper().path(path).attr({"stroke-width": strokeWidth, stroke: "gray"}).insertBefore(nodeShapes);
            
            // sibling handle
            this.generateHandle('sibling', x-PedigreeEditor.attributes.personSiblingHandleLengthX+strokeWidth/3, splitLocationY, x-PedigreeEditor.attributes.personSiblingHandleLengthX+strokeWidth/2, splitLocationY+PedigreeEditor.attributes.personSiblingHandleLengthY,
                                "Click to create a sibling or drag to an existing parentless person (valid choices will be highlighted in green)", "U");                

            if (editor.getGraph().getParentRelationship(node.getID()) === null) {
                // hint for the parent handle
                var topHandleHint = undefined;
                if (PedigreeEditor.attributes.enableHandleHintImages) {
                    var hintSize = PedigreeEditor.attributes.radius/2;
                    var path = [["M", x-hintSize, y- PedigreeEditor.attributes.personHandleLength],["L", x+hintSize, y- PedigreeEditor.attributes.personHandleLength]];
                    var line1  = editor.getPaper().path(path).attr({"stroke-width": strokeWidth/3, stroke: "#555555"}).toBack();
                    var father = editor.getPaper().rect(x-hintSize-11,y-PedigreeEditor.attributes.personHandleLength-5.5,11,11).attr({fill: "#CCCCCC"}).toBack();
                    var mother = editor.getPaper().circle(x+hintSize+6,y-PedigreeEditor.attributes.personHandleLength,6).attr({fill: "#CCCCCC"}).toBack();                    
                    var topHandleHint = editor.getPaper().set().push(line1, father, mother);
                }
                // parent handle
                this.generateHandle('parent', x, splitLocationY, x, y - PedigreeEditor.attributes.personHandleLength,
                                    "Click to create new nodes for the parents or drag to an existing person or partnership (valid choices will be highlighted in green). Dragging to a person will create a new relationship.", "F", topHandleHint);
            }
            else {
                if (PedigreeEditor.attributes.enableHandleHintImages) {
                    var path = [["M", x, splitLocationY],["L", x, y-PedigreeEditor.attributes.personHoverBoxRadius+4]];
                    editor.getPaper().path(path).attr({"stroke-width": strokeWidth, stroke: "gray"}).insertBefore(nodeShapes);                    
                }
            }
            
            if (!node.isFetus()) {
                
                if (node.getChildlessStatus() === null) {
                    // children handle
                    //static part (going right below the node)            
                    var path = [["M", x, y],["L", x, y+PedigreeEditor.attributes.personHandleBreakX]];
                    editor.getPaper().path(path).attr({"stroke-width": strokeWidth, stroke: "gray"}).insertBefore(nodeShapes);            
                    this.generateHandle('child', x, y+PedigreeEditor.attributes.personHandleBreakX-2, x, y+PedigreeEditor.attributes.personHandleLength,
                                        "Click to create a new child node or drag to an existing parentless person (valid choices will be highlighted in green)", "U");            
                }
                
                // partner handle
                var vertPosForPartnerHandles = y;                       
                //static part (going right form the node)            
                var path = [["M", x, vertPosForPartnerHandles],["L", x + PedigreeEditor.attributes.personHandleBreakX, vertPosForPartnerHandles]];
                editor.getPaper().path(path).attr({"stroke-width": strokeWidth, stroke: "gray"}).insertBefore(nodeShapes);
                this.generateHandle('partnerR', x + PedigreeEditor.attributes.personHandleBreakX - 2, vertPosForPartnerHandles, x + PedigreeEditor.attributes.personHandleLength, vertPosForPartnerHandles,
                                    "Click to create a new partner node or drag to an existing node (valid choices will be highlighted in green)", partnerGender);
            }
        }
        else {            
            if (editor.getGraph().getParentRelationship(node.getID()) === null)
                this.generateHandle('parent',   x, y, x, y - PedigreeEditor.attributes.personHandleLength, "Click to create new nodes for the parents or drag to an existing person or partnership (valid choices will be highlighted in green)");
            
            if (!node.isFetus()) {
                if (node.getChildlessStatus() === null)
                    this.generateHandle('child',x, y, x, y + PedigreeEditor.attributes.personHandleLength, "Click to create a new child node or drag to an existing parentless node (valid choices will be highlighted in green)");            
                this.generateHandle('partnerR', x, y, x + PedigreeEditor.attributes.personHandleLength, y, "Click to create a new partner node or drag to an existing node (valid choices will be highlighted in green)");
                this.generateHandle('partnerL', x, y, x - PedigreeEditor.attributes.personHandleLength, y, "Click to create a new partner node or drag to an existing node (valid choices will be highlighted in green)");
            }
        }
                       
        this._currentHandles.push( editor.getPaper().setFinish() );
                 
        //timer.printSinceLast("Generate handles ");
    },

    /**
     * Creates the buttons used in this hoverbox
     *
     * @method generateButtons
     */
    generateButtons: function($super) {  
        if (this._currentButtons !== null) return;
        $super();

        this.generateMenuBtn();

        // proband can't be removed
        if (!this.getNode().isProband())
            this.generateDeleteBtn();
    },

    /**
     * Creates a node-shaped show-menu button
     *
     * @method generateMenuBtn
     * @return {Raphael.st} The generated button
     */
    generateMenuBtn: function() {
        var me = this;
        var action = function() {
            me.toggleMenu(!me.isMenuToggled());
        };
        var genderShapedButton = this.getNode().getGraphics().getGenderShape().clone();
        genderShapedButton.attr(PedigreeEditor.attributes.nodeShapeMenuOff);
        genderShapedButton.click(action);
        genderShapedButton.hover(function() { genderShapedButton.attr(PedigreeEditor.attributes.nodeShapeMenuOn)},
                                 function() { genderShapedButton.attr(PedigreeEditor.attributes.nodeShapeMenuOff)});
        genderShapedButton.attr("cursor", "pointer");
        this._currentButtons.push(genderShapedButton);
        this.disable();
        this.getFrontElements().push(genderShapedButton);
        this.enable();
    },

    /**
     * Returns true if the menu for this node is open
     *
     * @method isMenuToggled
     * @return {Boolean}
     */
    isMenuToggled: function() {
        return this._isMenuToggled;
    },

    /**
     * Shows/hides the menu for this node
     *
     * @method toggleMenu
     */
    toggleMenu: function(isMenuToggled) {
        if (this._justClosedMenu) return;
        //console.log("toggle menu: current = " + this._isMenuToggled);
        this._isMenuToggled = isMenuToggled;
        if(isMenuToggled) {
            this.getNode().getGraphics().unmark();
            var optBBox = this.getBoxOnHover().getBBox();
            var x = optBBox.x2;
            var y = optBBox.y;
            var position = editor.getWorkspace().canvasToDiv(x+5, y);
            editor.getNodeMenu().show(this.getNode(), position.x, position.y);
        }
    },

    /**
     * Hides the hoverbox with a fade out animation
     *
     * @method animateHideHoverZone
     */
    animateHideHoverZone: function($super) {
        this._hidden = true;
        if(!this.isMenuToggled()){
            var parentPartnershipNode = editor.getGraph().getParentRelationship(this.getNode().getID());
            //console.log("Node: " + this.getNode().getID() + ", parentPartnershipNode: " + parentPartnershipNode);            
            if (parentPartnershipNode && editor.getNode(parentPartnershipNode))
                editor.getNode(parentPartnershipNode).getGraphics().unmarkPregnancy();
            $super();
        }
    },

    /**
     * Displays the hoverbox with a fade in animation
     *
     * @method animateDrawHoverZone
     */
    animateDrawHoverZone: function($super) {
        this._hidden = false;
        if(!this.isMenuToggled()){
            var parentPartnershipNode = editor.getGraph().getParentRelationship(this.getNode().getID());
            if (parentPartnershipNode && editor.getNode(parentPartnershipNode))
                editor.getNode(parentPartnershipNode).getGraphics().markPregnancy();
            $super();
        }
    },

    /**
     * Performs the appropriate action for clicking on the handle of type handleType
     *
     * @method handleAction
     * @param {String} handleType "child", "partner" or "parent"
     * @param {Boolean} isDrag True if this handle is being dragged
     */
    handleAction : function(handleType, isDrag, curHoveredId) {        
        console.log("handleType: " + handleType + ", isDrag: " + isDrag + ", curHovered: " + curHoveredId);        
        
        if(isDrag && curHoveredId !== null) {                   
            
            if(handleType == "parent") {
                this.removeHandles();
                this.removeButtons();
                var event = { "personID": this.getNode().getID(), "parentID": curHoveredId };
                document.fire("pedigree:person:drag:newparent", event);
            }
            else if(handleType == "partnerR" || handleType == "partnerL") {
                this.removeHandles();                
                var event = { "personID": this.getNode().getID(), "partnerID": curHoveredId };
                document.fire("pedigree:person:drag:newpartner", event);
            }
            else if(handleType == "child") {
                var event = { "personID": curHoveredId, "parentID": this.getNode().getID() };
                document.fire("pedigree:person:drag:newparent", event);                
            }
            else if(handleType == "sibling") {
                var event = { "sibling2ID": curHoveredId, "sibling1ID": this.getNode().getID() };
                document.fire("pedigree:person:drag:newsibling", event);                  
            }
        }
        else if (!isDrag) {
            if(handleType == "partnerR" || handleType == "partnerL") {
                this.removeHandles();                
                var preferLeft = (this.getNode().getGender() == 'F') || (handleType == "partnerL");
                var event = { "personID": this.getNode().getID(), "preferLeft": preferLeft };
                document.fire("pedigree:person:newpartnerandchild", event);
            }
            else if(handleType == "child") {
                var position = editor.getWorkspace().canvasToDiv(this.getNodeX(), (this.getNodeY() + PedigreeEditor.attributes.personHandleLength + 15));
                editor.getNodetypeSelectionBubble().show(this.getNode(), position.x, position.y);
                // if user selects anything the bubble will fire an even on its own
            }
            else if(handleType == "sibling") {                
                var position = editor.getWorkspace().canvasToDiv(this.getNodeX() - PedigreeEditor.attributes.personSiblingHandleLengthX,
                                                                 this.getNodeY() - PedigreeEditor.attributes.personHandleBreakY+PedigreeEditor.attributes.personSiblingHandleLengthY + 15);
                editor.getSiblingSelectionBubble().show(this.getNode(), position.x, position.y);                
            }
            else if(handleType == "parent") {
                this.removeHandles();
                this.removeButtons();
                var event = { "personID": this.getNode().getID() };
                document.fire("pedigree:person:newparent", event);
            }
        }
        this.animateHideHoverZone();
    }
});
