/**
 * Partnership is a class that represents the relationship between two AbstractNodes
 * and their children.
 *
 * @class Partnership
 * @constructor
 * @extends AbstractNode
 * @param x the x coordinate at which the partnership junction will be placed
 * @param y the y coordinate at which the partnership junction will be placed
 * @param partner1 an AbstractPerson who's one of the partners in the relationship.
 * @param partner2 an AbstractPerson who's the other partner in the relationship. The order of partners is irrelevant.
 * @id the unique ID number of this node
 */

var Partnership = Class.create(AbstractNode, {

   initialize: function($super, x, y, id) {
       console.log("partnership");
       this._childlessStatus = null;
       this._childlessReason = "";
       this._type = 'Partnership';
       $super(x, y, id);       
       console.log("partnership end");
   },

    /**
     * Generates and returns an instance of PartnershipVisuals
     *
     * @method _generateGraphics
     * @param {Number} x X coordinate of this partnership
     * @param {Number} y Y coordinate of this partnership
     * @return {PartnershipVisuals}
     * @private
     */
    _generateGraphics: function(x, y) {
        return new PartnershipVisuals(this, x, y);
    },

    /**
     * Changes the status of this partnership. Nullifies the status if the given status is not
     * "childless" or "infertile".
     *
     * @method setChildlessStatus
     * @param {String} status Can be "childless", "infertile" or null
     * @param {Boolean} ignoreChildren If True, changing the status will not detach any children
     */
    setChildlessStatus: function(status, ignoreChildren) {
        if(!this.isValidChildlessStatus(status))
            status = null;
        
        if(status != this.getChildlessStatus()) {
            this._childlessStatus = status;
            this.setChildlessReason(null);
            this.getGraphics().updateChildlessShapes();
            this.getGraphics().this.updateChildhubConnection(); 
        }
        
        return this.getChildlessStatus();
    },

    /**
     * Returns an object (to be accepted by the menu) with information about this Partnership
     *
     * @method getSummary
     * @return {Object}
     */
    getSummary: function() {
        var childlessInactive = false; //TODO: use node.poroperties
        return {
            identifier:    {value : this.getID()},
            childlessSelect : {value : this.getChildlessStatus() ? this.getChildlessStatus() : 'none', inactive: childlessInactive},
            childlessText : {value : this.getChildlessReason() ? this.getChildlessReason() : 'none', inactive: childlessInactive}
        };
    },

    /**
     * Returns an object containing all the properties of this node
     * except id, x, y & type
     *
     * @method getProperties
     * @return {Object} in the form
     *
     */
    getProperties: function() {
        var info = $super();
        info['childlessStatus'] = this.getChildlessStatus();
        info['childlessReason'] = this.getChildlessReason();
        return info;
    },

    /**
     * Applies the properties found in info to this node.
     *
     * @method assignProperties
     * @param properties Object
     * @return {Boolean} True if info was successfully assigned
     */
    assignProperties: function($super, info) {    
        if($super(info)) {
            if(info.childlessStatus && info.childlessStatus != this.getChildlessStatus()) {
                this.setChildlessStatus(info.childlessStatus);
            }
            if(info.childlessReason && info.childlessReason != this.getChildlessReason()) {
                this.setChildlessReason(info.childlessReason);
            }
            return true;
        }
        return false;
    },

    /**
     * Changes the status of this partnership. Nullifies the status if the given status is not
     * "childless" or "infertile". Adds an action stack entry for this action.
     *
     * @method setChildlessStatus
     * @param {String} status Can be "childless", "infertile" or null
     */
    setChildlessStatusAction: function(status) {
        if(status != this.getChildlessStatus() && (status || this.getChildlessStatus())) {
            var me = this,
                nodeID = this.getID(),
                prevStatus = this.getChildlessStatus(),
                prevReason = this.getChildlessReason();
            this.setChildlessStatus(status, false);

            // TODO
            /*
            editor.getActionStack().push({
                undo: Partnership.childlessActionUndo,
                redo: Partnership.childlessActionRedo,
                nodeID: nodeID,
                status: status,
                prevStatus: prevStatus,
                prevReason: prevReason,
                nodesBeforeChange: nodesBeforeChange,
                nodesAfterChange: nodesAfterChange
            });
            */
        }
    },

    /**
     * Changes the reason for this Partnership's childlessStatus. Adds an entry in the action stack for this action.
     *
     * @method setChildlessReasonAction
     * @param {String} reason
     */
    setChildlessReasonAction: function(reason) {
        var nodeID = this.getID();
        var prevReason = this.getChildlessReason();
        this.setChildlessReason(reason);
        var undo = function() {
            var partnership = editor.getGraphicsSet().getNode(nodeID);
            partnership && partnership.setChildlessReason(prevReason);
        };
        var redo = function() {
            var partnership = editor.getGraphicsSet().getNode(nodeID);
            partnership && partnership.setChildlessReason(reason);
        };
        editor.getActionStack().push({undo: undo, redo: redo});
    }
});

//ATTACH CHILDLESS BEHAVIOR METHODS TO PARTNERSHIP OBJECTS
Partnership.addMethods(ChildlessBehavior);
