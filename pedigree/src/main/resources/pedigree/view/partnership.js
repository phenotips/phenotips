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

   initialize: function($super, x, y, id, properties) {
       //console.log("partnership");
       this._childlessStatus = null;
       this._childlessReason = "";
       this._type            = 'Partnership';
       
       this._broken       = false;
       this._consangrMode = "A";    //  Can be either "A" (autodetect), "Y" (always consider consangr.) or "N" (never)
                                    // "Autodetect": derived from the current pedigree                                    
       
       // assign some properties before drawing so that relationship lines are drawn properly
       this.setBrokenStatus (properties["broken"]);
       this.setConsanguinity(properties["consangr"]);
       
       $super(x, y, id);
       
       this.assignProperties(properties);       
       //console.log("partnership end");       
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
     */
    setChildlessStatus: function(status) {
        if(!this.isValidChildlessStatus(status))
            status = null;
        
        if(status != this.getChildlessStatus()) {
            this._childlessStatus = status;
            this.setChildlessReason(null);
            this.getGraphics().updateChildlessShapes();
            this.getGraphics().updateChildhubConnection();
            this.getGraphics().getHoverBox().regenerateHandles();
        }
                
        return this.getChildlessStatus();        
    },
    
    /**
     * Sets the consanguinity setting of this relationship. Valid inputs are "A" (automatic"), "Y" (yes) and "N" (no)
     *
     * @method setConsanguinity
     */        
    setConsanguinity: function(value) {
        if (value != "A" && value != "N" && value != "Y")
            value = "A";
        if (this._consangrMode != value) {
            this._consangrMode = value;
        }
        this.getGraphics() && this.getGraphics().getHoverBox().regenerateButtons();
    },
    
    /**
     * Returns the consanguinity setting of this relationship: "A" (automatic"), "Y" (yes) or "N" (no)
     *
     * @method getConsanguinity
     */    
    getConsanguinity: function() {
        return this._consangrMode;
    },

    /**
     * Sets relationship as either broken or not
     *
     * @method getBrokenStatus
     */    
    setBrokenStatus: function(value) {
        if (value === undefined)
            value = false;
        if (this._broken != value) {
            this._broken = value;            
        }        
    },
    
    /**
     * Returns the status of this relationship (broken or not)
     *
     * @method getBrokenStatus
     */        
    getBrokenStatus: function() {
        return this._broken;
    },
    
    /**
     * Returns an object (to be accepted by the menu) with information about this Partnership
     *
     * @method getSummary
     * @return {Object}
     */
    getSummary: function() {
        var childlessInactive = editor.getGraph().hasNonPlaceholderNonAdoptedChildren(this.getID());
        return {
            identifier:    {value : this.getID()},
            childlessSelect : {value : this.getChildlessStatus() ? this.getChildlessStatus() : 'none', inactive: childlessInactive},
            childlessText : {value : this.getChildlessReason() ? this.getChildlessReason() : 'none', inactive: childlessInactive},            
            consangr: {value: this._consangrMode, inactive: false},
            broken: {value: this.getBrokenStatus(), inactive: false}
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
    getProperties: function($super) {
        var info = $super();
        if (this.getChildlessStatus() != null) {
            info['childlessStatus'] = this.getChildlessStatus();
            info['childlessReason'] = this.getChildlessReason();
        }
        if (this.getConsanguinity() != "A") {
            info['consangr'] = this.getConsanguinity();
        }
        if (this.getBrokenStatus()) {
            info['broken'] = this.getBrokenStatus();
        }
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
            if (info.consangr && info.consangr != this.getConsanguinity()) {                
                this.setConsanguinity(info.consangr);
            }
            if (info.broken && info.broken != this.getBrokenStatus()) {
                this.setBrokenStatus(info.broken);
            }
            return true;
        }
        return false;
    }
});

//ATTACH CHILDLESS BEHAVIOR METHODS TO PARTNERSHIP OBJECTS
Partnership.addMethods(ChildlessBehavior);
