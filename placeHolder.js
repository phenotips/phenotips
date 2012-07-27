var PlaceHolder = Class.create(AbstractNode, {

    initialize: function($super, x, y, gender, id) {
        $super(x, y, gender, id);
    },

    generateGraphics: function(x, y) {
        return new PlaceHolderVisuals(this, x, y);
    },

    getType: function() {
        return "ph";
    },

    removeParent: function($super, parent) {
        $super(parent);
        this.removeIfDisconnected();
    },

    removeChild: function($super, child) {
        $super(child);
        this.removeIfDisconnected();
    },

    removePartner: function($super, partner) {
        $super(partner);
        this.removeIfDisconnected();
    },

    removeIfDisconnected: function(removeGraphics) {
        var hasNoParents = !this.getMother && !this.getPhMother() && !this.getFather() && !this.getPhFather();
        var hasNoKids = this.getChildren().flatten().length == 0;
        var isSingleParent = (this.getChildren().flatten().length > 0 && this.getPartnerConnections().flatten().length == 0);
        if (hasNoParents &&  (hasNoKids || isSingleParent)) {
            this.remove(false,removeGraphics);
        }
    },

    convertToPerson: function() {
        var newNode = new Person(this.getGraphics().getX(), this.getGraphics().getY(), this.getGender());
        this.merge(newNode);
    },

    merge: function(node) {
        alert('merging');
        //this._child && node.addChild(this._child);
        //this._phChild && node.addPhChild(this._phChild);
        //this.

        //should merge parent/children placeholders
        //build an array of non null values from placeholder and replace/push them on to the node
    },

    canMergeWith: function(node) {
        return (node._gender == this._gender || this._gender == 'U' ) &&
            !node.isPartnerOf(this) &&
            !this.hasConflictingParents(node) &&
            !node.isDescendantOf(this) &&
            !node.isAncestorOf(this);
    },

    hasConflictingParents: function(node) {
        var hasConflictingDads = this._father && node._father && this._father != node._father;
        var hasConflictingMoms = this._mother && node._mother && this._mother != node._mother;
        var notReversedParents = (this._mother && this._mother._gender != 'U') ||
            (this._father && this._father._gender != 'U') ||
            (node._mother && node._mother._gender != 'U') ||
            (node._father && node._father._gender != 'U') ||
            (this._mother && node._father && this._mother != node._father) ||
            (this._father && node._mother && this._father != node._mother);

        return notReversedParents && (hasConflictingDads || hasConflictingMoms);
    }
});