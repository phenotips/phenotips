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

    convertToPerson: function() {
        var newNode = editor.addNode(this.getGraphics().getAbsX(), this.getGraphics().getAbsY(), this.getGender(), false);
        this.merge(newNode);
    },

    merge: function(node) {
        if(this.canMergeWith(node)) {

            var parents = this.getParents();
            parents && parents.removeChild(this);
            parents && (parents.getChildren().indexOf(node) == -1) && parents.addChild(node);
            var partnerships = this.getPartnerships();
            var me = this;

            partnerships.each(function(partnership){
                var newPartnership = node.addPartner(partnership.getPartnerOf(me));
                partnership.getVerticals().each(function(vertical){
                    var child = vertical.getChild();
                    vertical.remove();
                    child.addParents(newPartnership);
                });
                partnership.remove();
            });
            me && me.remove(false, true);
        }

        //this._child && node.addChild(this._child);
        //this._phChild && node.addPhChild(this._phChild);
        //this.

        //should merge parent/children placeholders
        //build an array of non null values from placeholder and replace/push them on to the node
    },

    canMergeWith: function(node) {
        return (node.getGender() == this.getGender() || this.getGender() == 'U' || node.getGender() == "U" ) &&
            !node.isPartnerOf(this) &&
            !this.hasConflictingParents(node) &&
            !node.isDescendantOf(this) &&
            !node.isAncestorOf(this);
    },

    hasConflictingParents: function(node) {
        return (this.getParents() != null && node.getParents() != null && this.getParents() != node.getParents());
//        var hasConflictingDads = this._father && node._father && this._father != node._father;
//        var hasConflictingMoms = this._mother && node._mother && this._mother != node._mother;
//        var notReversedParents = (this._mother && this._mother._gender != 'U') ||
//            (this._father && this._father._gender != 'U') ||
//            (node._mother && node._mother._gender != 'U') ||
//            (node._father && node._father._gender != 'U') ||
//            (this._mother && node._father && this._mother != node._father) ||
//            (this._father && node._mother && this._father != node._mother);
//
//        return notReversedParents && (hasConflictingDads || hasConflictingMoms);
    }
});