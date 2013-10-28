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
        var radius = PedigreeEditor.attributes.radius * 2;
        $super(personNode, -radius, -radius, radius * 2, radius * 2, centerX, centerY, nodeShapes);
        this._isMenuToggled = false;
        //if (editor.getGraph().getParentRelationship(this.getNode().getID()) !== null)
        //    this.hideParentHandle();
    },

    /**
     * Creates the handles used in this hoverbox
     *
     * @method generateHandles
     * @return {Raphael.st} A set of handles
     */
    generateHandles: function($super) {                  
        this._upHandle    = this.generateHandle('parent',   this.getNodeX(), this.getNodeY() - (PedigreeEditor.attributes.radius * 1.6), "Click to create new nodes or drag to an existing node or relationship");
        this._downHandle  = this.generateHandle('child',    this.getNodeX(), this.getNodeY() + (PedigreeEditor.attributes.radius * 1.6), "Click to create a new child node or drag to an existing parentless node");
        this._rightHandle = this.generateHandle('partnerR', this.getNodeX() + (PedigreeEditor.attributes.radius * 1.6), this.getNodeY(), "Click to create a new partner node or drag to an existing node. Valid choices are highlighted in green");
        this._leftHandle  = this.generateHandle('partnerL', this.getNodeX() - (PedigreeEditor.attributes.radius * 1.6), this.getNodeY(), "Click to create a new partner node or drag to an existing node. Valid choices are highlighted in green");
        return $super().push(this._upHandle, this._downHandle, this._rightHandle, this._leftHandle);
    },

    /**
     * Creates the buttons used in this hoverbox
     *
     * @method generateButtons
     * @return {Raphael.st} A set of buttons
     */
    generateButtons: function($super) {
        var buttons = $super().push(this.generateMenuBtn());
        (!this.getNode().isProband()) && buttons.push(this.generateDeleteBtn());
        this._twinButton = this.generateAddTwinButton();        
        buttons.push(this._twinButton);
        return buttons;
    },

    /**
     * Creates and returns a "create a twin" button
     *
     * @method generateAddTwinButton
     * @return {Raphael.st} the generated button
     */    
    generateAddTwinButton: function() {
        var me = this;        
        var action = function() {
            var id = me.getNode().getID(); // may chnage since graphics was created
            var event = { "nodeID": id, "modifications": { "addTwin": 1 } };
            document.fire("pedigree:node:modify", event);             
        };
        var path = "M0,25L10,0L20,25";
        var attributes = {}; //PedigreeEditor.attributes.menuBtnIcon;        
        var x = this.getX() + this.getWidth()*0.5 - 5.5;
        var y = this.getY() + this.getHeight()/70;
        return this.createButton(x, y, path, attributes, action, "twin", "add a twin");        
    },
    
    /**
     * Hides the partner and children handles
     *
     * @method hidePartnerHandles
     */
    hidePartnerHandles: function() {
        this.getCurrentHandles().exclude(this._rightHandle.hide());
        this.getCurrentHandles().exclude(this._leftHandle.hide());
    },

    /**
     * Displays the partner and children handles
     *
     * @method unhidePartnerHandles
     */
    unhidePartnerHandles: function() {
        if(this.isHovered() || this.isMenuToggled()) {
            this._rightHandle.show();
            this._leftHandle.show();
        }
        (!this.getCurrentHandles().contains(this._rightHandle)) && this.getCurrentHandles().push(this._rightHandle);
        (!this.getCurrentHandles().contains(this._leftHandle)) && this.getCurrentHandles().push(this._leftHandle);
    },

    /**
     * Hides the child handle
     *
     * @method hideChildHandle
     */
    hideChildHandle: function() {
        this.getCurrentHandles().exclude(this._downHandle.hide());
    },

    /**
     * Displays the child handle
     *
     * @method unhideChildHandle
     */
    unhideChildHandle: function() {
        if(this.isHovered() || this.isMenuToggled()) {
            this._downHandle.show();
        }
        (!this.getCurrentHandles().contains(this._downHandle)) && this.getCurrentHandles().push(this._downHandle);
    },

    /**
     * Hides the parent handle
     *
     * @method hideParentHandle
     */
    hideParentHandle: function() {
        this.getCurrentHandles().exclude(this._upHandle.hide());
        this._twinButton.show();
    },

    /**
     * Displays the parent handle
     *
     * @method unHideParentHandle
     */
    unHideParentHandle: function() {
        if(this.isHovered() || this.isMenuToggled()) {
            this._upHandle.show();
        }
        (!this.getCurrentHandles().contains(this._upHandle)) && this.getCurrentHandles().push(this._upHandle);
        this._twinButton.hide();        
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
        console.log("toggle menu");
        this._isMenuToggled = isMenuToggled;
        if(isMenuToggled) {
            this.getNode().getGraphics().unmark();
            var optBBox = this.getBoxOnHover().getBBox();
            var x = optBBox.x2;
            var y = optBBox.y;
            var position = editor.getWorkspace().canvasToDiv(x+5, y);
            editor.getNodeMenu().show(this.getNode(), position.x, position.y);
        }
        else {
            editor.getNodeMenu().hide();
        }
    },

    /**
     * Hides the hoverbox with a fade out animation
     *
     * @method animateHideHoverZone
     */
    animateHideHoverZone: function($super) {
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
        var parentPartnershipNode = editor.getGraph().getParentRelationship(this.getNode().getID());
        if (parentPartnershipNode && editor.getNode(parentPartnershipNode))
            editor.getNode(parentPartnershipNode).getGraphics().markPregnancy();
        $super();
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
                var event = { "personID": this.getNode().getID(), "parentID": curHoveredId };
                document.fire("pedigree:person:drag:newparent", event);
            }
            else if(handleType == "partnerR" || handleType == "partnerL") {
                var event = { "personID": this.getNode().getID(), "partnerID": curHoveredId };
                document.fire("pedigree:person:drag:newpartner", event);
            }
            else if(handleType == "child") {
                var event = { "personID": curHoveredId, "parentID": this.getNode().getID() };
                document.fire("pedigree:person:drag:newparent", event);                
            }
        }
        else if (!isDrag) {
            if(handleType == "partnerR" || handleType == "partnerL") {
                var preferLeft = (handleType == "partnerL");          
                var event = { "personID": this.getNode().getID(), "preferLeft": preferLeft };
                document.fire("pedigree:person:newpartnerandchild", event);
            }
            else if(handleType == "child") {
                var position = editor.getWorkspace().canvasToDiv(this.getNodeX(), (this.getNodeY() + PedigreeEditor.attributes.radius * 2.3));
                editor.getNodetypeSelectionBubble().show(this.getNode(), position.x, position.y);
                // if user selects anything the bubble will fire an even on its own
            }
            else if(handleType == "parent") {
                var event = { "personID": this.getNode().getID() };
                document.fire("pedigree:person:newparent", event);
            }
        }
        this.animateHideHoverZone();
    }
});
