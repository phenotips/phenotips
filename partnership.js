var Partnership = Class.create(AbstractNode, {

    initialize: function(x, y, node1, node2) {
        if(node1.getType() != 'ph' || node2.getType() != 'ph') {
            this._partner1 = node1;
            this._partner2 = node2;
            this._children = [];
        }
    },

    generateGraphics: function(x, y) {
        return new PartnershipVisuals(this, x, y);
    },

    getPartner1: function() {
        return this._partner1;
    },

    setPartner1: function(node) {
        this._partner1 = node;
    },

    getPartner2: function() {
        return this._partner2;
    },

    setPartner2: function(node) {
        this._partner2 = node;
    },

    getMother: function() {
        if(this.getPartner1().getGender() == "F") {
            return this.getPartner1();
        }
        else if(this.getPartner2().getGender() == "F") {
            return this.getPartner2();
        }
        else {
            return null;
        }
    },

    getFather: function() {
        return this.getPartnerOf(this.getMother());
    },

    getPartnerOf: function(node) {
        if(node == this.getPartner1()) {
            return this.getPartner2();
        }
        else if(node == this.getPartner2()) {
            return this.getPartner1();
        }
        else {
            return null;
        }
    },

    contains: function(node) {
        return (this.getPartner1() == node || this.getPartner2() == node);
    },

    getChildren: function() {
        return this._children;
    },

    setChildren: function(arrayOfChildren) {
        this._children = arrayOfChildren;
    },

    createChild: function(isPlaceHolder) {
        //TODO: set x and y using positioning algorithm
        var x = this.getPartner1().getGraphics().getAbsX() + 150,
            y = this.getPartner1().getGraphics().getAbsY() + 200,
            child = editor.addNode(x, y, "U", isPlaceHolder);
        return this.addChild(child);
    },

    addChild: function(child) {
        //TODO: elaborate on restrictions for adding parents to existing node
        if(child && this.getChildren().indexOf(child) == -1 && (child.getParentPartnership() == null)) {
            this.getChildren().push(child);
            child.setParentPartnership(this);
        }
        return child;
    },

    removeChild: function(child) {
        this.setChildren(this.getChildren().without(child));
    },

    remove: function() {
        this.getChildren().each(function(child) {
            child.setParentPartnership(null);
        });
        this.getPartner1().removePartnership(this);
        this.getPartner2().removePartnership(this);
    },

    /*
     * Returns the children nodes of this partnership
     */
    getLowerNeighbors: function() {
        return this.getChildren();
    },

    /*
     * Returns an array containing partner1 and partner2
     */
    getSideNeighbors: function() {
        return [this.getPartner1(), this.getPartner2()];
    }
});