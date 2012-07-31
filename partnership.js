var Partnership = Class.create( {

    initialize: function(node1, node2) {
        if(node1.getType() != 'ph' || node2.getType() != 'ph') {
            this._partner1 = node1;
            this._partner2 = node2;
            this._verticals = [];
        }
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

//    replacePartner: function(partner, replacement) {
//        if(replacement.getType() == "ph" && this.getPartnerOf(partner).getType() == "ph") {
//            this.remove(false);
//        }
//        else if(this.getPartner1() == partner) {
//                this.setPartner1(replacement);
//            }
//        else if(this.getPartner2() == partner) {
//            this.setPartner2(replacement);
//        }
//    },

    contains: function(node) {
        return (this.getPartner1() == node || this.getPartner2() == node);
    },

    getVerticals: function() {
        return this._verticals;
    },

    getChildren: function() {
        var children = [];
        this.getVerticals().each(function(vertical) {
            var child = vertical.getChild();
            child && children.push(child);
        });
        return children;
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
        if(child && this.getChildren().indexOf(child) == -1 && (child.getParents() == null)) {
            var connection = new Vertical(this, child);
            this.getVerticals().push(connection);
            child.setParents(this);
            //TODO: editor.addVertical(this, child);
        }
        return child;
    },

    getVertical: function(child) {
        for(var i = 0; i < this.getVerticals().length; i++) {
            if(this.getVerticals()[i].getChild() == child) {
                return this.getVerticals()[i];
            }
        }
    },

    removeChild: function(child) {
        this.removeVertical(this.getVertical(child));
    },

    removeVertical: function(vertical) {
        this._verticals = this._verticals.without(vertical);
        vertical.remove();
    },

    remove: function() {
        this.getVerticals().each(function(vertical) {
           vertical.remove();
        });
        this.getPartner1().removePartnership(this);
        this.getPartner2().removePartnership(this);
    }
});