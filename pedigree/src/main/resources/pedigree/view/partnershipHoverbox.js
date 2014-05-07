/**
 * PartnershipHoverbox is a class for all the UI elements and graphics surrounding a Partnership node and
 * its labels. This includes the box that appears around the node when it's hovered by a mouse, as
 * well as the handles used for creating connections and creating new nodes.
 *
 * @class PartnershipHoverbox
 * @extends AbstractHoverbox
 * @constructor
 * @param {Partnership} partnership The Partnership for which the hoverbox is drawn
 * @param {Number} junctionX The x coordinate around which the partnership bubble is centered
 * @param {Number} junctionY The y coordinate around which the partnership bubble is centered
 * @param {Raphael.st} shapes Raphaël set containing the graphical elements that make up the node
 */

var PartnershipHoverbox = Class.create(AbstractHoverbox, {

    initialize: function($super, partnership, junctionX, junctionY, nodeShapes) {
        var radius = PedigreeEditor.attributes.radius;        
        $super(partnership, -radius*0.65, -radius*0.8, radius*1.3, radius*2.3, junctionX, junctionY, nodeShapes);
        this._isMenuToggled = false;
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

        if (this.getNode().getChildlessStatus() !== null) return;
        
        var x = this.getNodeX();
        var y = this.getNodeY();     
        var strokeWidth = editor.getWorkspace().getSizeNormalizedToDefaultZoom(PedigreeEditor.attributes.handleStrokeWidth);

        editor.getPaper().setStart();        
        //static part (going right below the node)            
        var path = [["M", x, y],["L", x, y+PedigreeEditor.attributes.partnershipHandleBreakY]];
        editor.getPaper().path(path).attr({"stroke-width": strokeWidth, stroke: "gray"}).insertBefore(this.getNode().getGraphics().getJunctionShape());            
        this.generateHandle('child', x, y+PedigreeEditor.attributes.partnershipHandleBreakY, x, y+PedigreeEditor.attributes.partnershipHandleLength);
                
        this._currentHandles.push( editor.getPaper().setFinish() );
    },

    /**
     * Creates the buttons used in this hoverbox
     *
     * @method generateButtons
     */
    generateButtons: function($super) {
        if (this._currentButtons !== null) return;
        $super();
        this.generateDeleteBtn();
        this.generateMenuBtn();        
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
        var junctionShapedButton = this.getNode().getGraphics().getJunctionShape().clone();
        junctionShapedButton.attr(PedigreeEditor.attributes.nodeShapeMenuOffPartner);
        junctionShapedButton.click(action);
        junctionShapedButton.hover(function() { junctionShapedButton.attr(PedigreeEditor.attributes.nodeShapeMenuOnPartner)},
                                   function() { junctionShapedButton.attr(PedigreeEditor.attributes.nodeShapeMenuOffPartner)});
        junctionShapedButton.attr("cursor", "pointer");
        this._currentButtons.push(junctionShapedButton);
        this.disable();
        this.getFrontElements().push(junctionShapedButton);        
        this.enable();           
    },     

    /**
     * Returns true if the menu is toggled for this partnership node
     *
     * @method isMenuToggled
     * @return {Boolean}
     */
    isMenuToggled: function() {
        return this._isMenuToggled;
    },
    
    /**
     * Shows/hides the menu for this partnership node
     *
     * @method toggleMenu
     * @param {Boolean} isMenuToggled Set to True to make the menu visible
     */
    toggleMenu: function(isMenuToggled) {
        if (this._justClosedMenu) return;
        this._isMenuToggled = isMenuToggled;
        if(isMenuToggled) {
            var optBBox = this.getBoxOnHover().getBBox();
            var x = optBBox.x2;
            var y = optBBox.y;
            var position = editor.getWorkspace().canvasToDiv(x+5, y);
            editor.getPartnershipMenu().show(this.getNode(), position.x, position.y);
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
            $super();
        }
    },
    
    /**
     * Performs the appropriate action for clicking on the handle of type handleType
     *
     * @method handleAction
     * @param {String} handleType Can be either "child", "partner" or "parent"
     * @param {Boolean} isDrag Set to True if the handle is being dragged at the time of the action
     */
    handleAction : function(handleType, isDrag, curHoveredId) {
        if(isDrag && curHoveredId) {            
            if(handleType == "child") { 
                var event = { "personID": curHoveredId, "parentID": this.getNode().getID() };
                document.fire("pedigree:person:drag:newparent", event);
            }
        }
        else if (!isDrag && handleType == "child") {
            var position = editor.getWorkspace().canvasToDiv(this.getNodeX(), (this.getNodeY() + PedigreeEditor.attributes.partnershipHandleLength + 15));
            var canBeChildless = !editor.getGraph().hasNonPlaceholderNonAdoptedChildren(this.getNode().getID());
            if (canBeChildless)
                editor.getNodetypeSelectionBubble().show(this.getNode(), position.x, position.y);
            else
                editor.getSiblingSelectionBubble().show(this.getNode(), position.x, position.y);
            // if user selects anything the bubble will fire an even on its own
        }
        this.animateHideHoverZone();        
    }
});
