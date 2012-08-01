var PlaceHolder = Class.create(AbstractPerson, {

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
            var parents = this.getParentPartnership();
            parents && parents.removeChild(this);
            parents && (parents.getChildren().indexOf(node) == -1) && parents.addChild(node);
            var partnerships = this.getPartnerships();
            var me = this;

            partnerships.each(function(partnership){
                var partner = partnership.getPartnerOf(me);
                if(node.getPartners().indexOf(partner) == -1) {
                    var newPartnership = node.addPartner(partnership.getPartnerOf(me));
                    partnership.getChildren().each(function(child){
                        child.addParents(newPartnership);
                    });
                }
                else {
                    var redundantPartnership = node.getPartnership(partner),
                        existingChildren = redundantPartnership.getChildren();
                    partnership.getChildren().each(function(child){
                        if(existingChildren.indexOf(child) == -1) {
                            redundantPartnership.addChild(child);
                        }
                    });
                partnership.remove();
                }
            });
            me && me.remove(false, true);
        }
    },

    canMergeWith: function(node) {
        return (node.getGender() == this.getGender() || this.getGender() == 'U' || node.getGender() == "U" ) &&
            !node.isPartnerOf(this) &&
            !this.hasConflictingParents(node) &&
            !node.isDescendantOf(this) &&
            !node.isAncestorOf(this);
    },

    hasConflictingParents: function(node) {
        return (this.getParentPartnership() != null && node.getParentPartnership() != null && this.getParentPartnership() != node.getParentPartnership());
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