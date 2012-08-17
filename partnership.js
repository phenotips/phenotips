/*
 * Partnership is a class that represents the relationship between two AbstractNodes
 * and their children.
 *
 * @param x the x coordinate at which the partnership junction will be placed
 * @param y the y coordinate at which the partnership junction will be placed
 * @param partner1 an AbstractPerson who's one of the partners in the relationship.
 * @param partner2 an AbstractPerson who's the other partner in the relationship. The order of partners is irrelevant.
 */

var Partnership = Class.create(AbstractNode, {

   initialize: function($super, x, y, partner1, partner2) {
       if(partner1.getType() != 'ph' || partner2.getType() != 'ph') {
           this._partners = [partner1, partner2];
           this._children = [];
           this._partners[0].addPartnership(this);
           this._partners[1].addPartnership(this);
           $super(x,y);
       }
   },

    /*
     * Generates and returns an instance of PartnershipVisuals
     */
    generateGraphics: function(x, y) {
        return new PartnershipVisuals(this, x, y);
    },

    /*
     * Returns an array containing the two partners. Partners are AbstractPerson objects
     */
    getPartners: function() {
        return this._partners;
    },

    /*
     * Returns the female partner in the partnership. Returns null if none of the parents are female
     */
    getMother: function() {
        if(this.getPartners()[0].getGender() == "F") {
            return this.getPartners()[0];
        }
        else if(this.getPartners()[1].getGender() == "F") {
            return this.getPartners()[1];
        }
        else {
            return null;
        }
    },

    /*
     * Returns the male partner in the partnership. Returns null if none of the parents are male
     */
    getFather: function() {
        if(this.getPartners()[0].getGender() == "M") {
            return this.getPartners()[0];
        }
        else if(this.getPartners()[1].getGender() == "M") {
            return this.getPartners()[1];
        }
        else {
            return null;
        }
    },

    /*
     * Returns the partner of someNode if someNode is a partner in this relationship. Otherwise, returns null.
     *
     * @param someNode is an AbstractPerson
     */
    getPartnerOf: function(someNode) {
        if(someNode == this.getPartners()[0]) {
            return this.getPartners()[1];
        }
        else if(someNode == this.getPartners()[1]) {
            return this.getPartners()[0];
        }
        else {
            return null;
        }
    },

    /*
     * Returns true if someNode is a partner in this relationship.
     *
     * @param someNode is an AbstractPerson
     */
    contains: function(someNode) {
        return (this.getPartners()[0] == someNode || this.getPartners()[1] == someNode);
    },

    /*
     * Returns an array of AbstractNodes that are children of this partnership
     */
    getChildren: function() {
        return this._children;
    },

    /*
     * Returns true if someNode is a child of this partnership.
     *
     * @param someNode is an AbstractPerson
     */
    hasChild: function(someNode) {
        return this.getChildren().indexOf(someNode) > -1;
    },

    /*
     * Creates a new AbstractNode and sets it as a child of this partnership. Returns the child.
     *
     * @param isPlaceHolder set to true if the child is a placeholder
     */
    createChild: function(isPlaceHolder) {
        var position = editor.findPosition({below: this.getID()}, ['child']);
        var child = editor.addNode(position['child'].x, position['child'].y, "U", isPlaceHolder);
        return this.addChild(child);
    },

    /*
     * Adds someNode to the list of children of this partnership, and stores this partnership
     * as it's parent partnership. Returns someNode.
     *
     * @param someNode is an AbstractPerson
     */
    addChild: function(someNode) {
        //TODO: elaborate on restrictions for adding parents to existing node
        if(someNode && !this.hasChild(someNode) && (someNode.getParentPartnership() == null)) {
            this.getChildren().push(someNode);
            someNode.parentConnection = this.getGraphics().updateChildConnection(someNode, someNode.getX(), someNode.getY(), this.getX(), this.getY());
            someNode.setParentPartnership(this);
        }
        return someNode;
    },
    
    /*
     * Removes someNode from the list of children of this partnership, and removes this partnership as its parents
     * reference. Returns someNode.
     *
     * @param someNode is an AbstractPerson
     */
    removeChild: function(someNode) {
        someNode.setParentPartnership(null);
        this._children = this._children.without(someNode);
        someNode.parentConnection.remove();
        someNode.parentConnection = null;
    },

    remove: function() {
        var me = this;
        this.getChildren().each(function(child) {
            child.setParentPartnership(null);
            me.removeChild(child);
        });
        this.getPartners()[0].removePartnership(this);
        this.getPartners()[1].removePartnership(this);
        this.getGraphics().remove();
    },

    /*
     * Returns an array of children nodes of this partnership
     */
    getLowerNeighbors: function() {
        return this.getChildren();
    },

    /*
     * Returns an array containing the two partners of this relationship
     */
    getSideNeighbors: function() {
        return this.getPartners();
    },

    canBeParentOf: function(someNode) {
        return (this.getPartners()[0].canBeParentOf(someNode) && this.getPartners()[1].canBeParentOf(someNode));
    }
});