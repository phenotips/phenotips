var AbstractNode = Class.create( {

    initialize: function(x, y, gender, id) {
        this._id = (id != null) ? id : editor.generateID();
        this._xPos = x;
        this._yPos = y;
        this._gender = this.parseGender(gender);
        this._mother = null;
        this._father = null;
        this._phMother = null;
        this._phFather = null;
        this._partnerConnections = [[/*nodes*/],[/*placeHolders*/]];
        this._children = [[/*nodes*/],[/*placeHolders*/]];
        this._siblings = [[/*nodes*/],[/*placeHolders*/]];
        this._drawing = this.generateDrawing();
    },

    generateGenderShape: function() {

        var radius = editor.graphics.getRadius(),
            shape;
        if (this.getGender() == 'M') {
            shape = editor.paper.rect(this.getX() - radius, this.getY() - radius, radius * 2, radius * 2, 2);
        }
        else if (this.getGender() == 'F') {
            shape = editor.paper.circle(this.getX(), this.getY(), radius);
        }
        else {
            shape = editor.paper.rect(this.getX() - radius * (Math.sqrt(3.5)/2), this.getY() - radius * (Math.sqrt(3.5)/2),
                radius * Math.sqrt(3.5), radius * Math.sqrt(3.5));
            shape.attr(editor.graphics._attributes.nodeShape);

        }
        shape.attr(editor.graphics._attributes.nodeShape);
        return (this.getGender() == 'U') ? shape.transform("...r45") : shape;
    },

    getDrawing: function() {
        return this._drawing;
    },

    setDrawing: function(drawing) {
        this._drawing = drawing;
    },

    parseGender: function(gender) {
        return (gender == 'M' || gender == 'F')?gender:'U';
    },

    getID: function() {
        return this._id;
    },

    getGender: function() {
        return this._gender;
    },

    setGender: function(gender) {
        this._gender = this.parseGender(gender);
        this.getDrawing().remove();
        this.setDrawing(this.generateDrawing());
    },

    getX: function() {
        return this._xPos;
    },

    getY: function() {
        return this._yPos;
    },

    setX: function(x) {
        this._xPos = x;
    },

    setY: function(y) {
        this._yPos = y;
    },

    setPosition: function(x,y) {
        this.setX(x);
        this.setY(y);
    },

    getMother: function() {
        return this._mother;
    },

    setMother: function(node) {
        this._mother = node;
    },

    getPhMother: function() {
        return this._phMother;
    },

    setPhMother: function(node) {
        this._phMother = node;
    },

    getFather: function() {
        return this._father;
    },

    setFather: function(node) {
        this._father = node;
    },

    getPhFather: function() {
        return this._phFather;
    },

    setPhFather: function(node) {
        this._phFather = node;
    },

    addParent: function(node) {
        var funct = (node.getType() == 'pn') ? this.addPersonParent(node) : this.addPhParent(node);
    },

    removeParent: function(node) {
        var funct = (node.getType() == 'pn') ? this.removePersonParent(node) : this.removePhParent(node);
    },

    addPhParent: function(node) {
        if(node.getGender() == 'F') {
            if(this.getPhMother() == null && this.getMother() == null) {
                this.setPhMother(node);
            }
            else if(this.getMother().getGender() == 'U' && this.getFather() == null) {
                this.setFather(this.getMother());
                this.setPhMother(node);
            }
        }
        else if(node.getGender() == 'M') {
            if(this.getFather() == null && this.getPhFather() == null) {
                this.setPhFather(node);
            }
            else if(this.getFather().getGender() == 'U' && this.getMother() == null) {
                this.setMother(this.getFather());
                this.setPhFather(node);
            }
        }
        else {
            if(this.getPhMother() == null && this.getMother() == null) {
                this.setPhMother(node);
            }
            else if(this.getFather() == null && this.getPhFather() == null) {
                this.setPhFather(node);
            }
        }
    },

    removePhParent: function(node) {
        if(this.getPhMother() == node) {
            this.getPhMother().remove();
            this.setPhMother(null);
        }
        if(this.getPhFather() == node) {
            this.getPhFather().remove();
            this.setPhFather(null);
        }
    },

    addPersonParent: function(node) {
        if(node.getGender() == 'F') {
            if(this.getMother() == null) {
                this.setMother(node);
                this.getPhMother() && this.getPhMother().remove();
            }
            else if(this.getMother().getGender() == 'U' && this.getFather() == null) {
                this.setFather(this.getMother());
                this.setMother(node);
                this.getPhMother() && this.getPhMother().remove();
                this.getPhFather() && this.getPhFather().remove();
            }
        }
        else if(node.getGender() == 'M') {
            if(this.getFather() == null) {
                this.setFather(node);
                this.getPhFather() && this.getPhFather().remove();
            }
            else if(this.getFather().getGender() == 'U' && this._mother == null) {
                this.setMother(this.getFather());
                this.setFather(node);
                this.getPhMother() && this.getPhMother().remove();
                this.getPhFather() && this.getPhFather().remove();
            }
        }
        else {
            if(this.getMother() == null) {
                this.setMother(node);
                this.getPhMother() && this.getPhMother().remove();
            }
            else if(this.getFather() == null) {
                this.setFather(node);
                this.getPhFather() && this.getPhFather().remove();
            }
        }
    },

    removePersonParent: function(node) {
        if(this.getMother() == node) {
            this.getMother().remove();
            this.setMother(null);
        }
        if(this.getFather() == node) {
            this.getFather().remove();
            this.setFather(null);
        }
    },

    removeRelativeFromList: function(array, node) {
            array[+(node.getType() != 'pn')] = array[+(node.getType() != 'pn')].without(node);
    },

    addRelativeToList: function(array, node) {
            array[+(node.getType() != 'pn')].push(node);
    },

    getPartners: function() {
        return this._partnerConnections;
    },

    getPersonPartners: function() {
        return this._partnerConnections[0];
    },

    setPersonPartners: function(partnersArray) {
        this._partnerConnections[0] = partnersArray;
    },

    getPhPartners: function() {
        return this._partnerConnections[1];
    },

    setPhPartners: function(partnersArray) {
        this._partnerConnections[1] = partnersArray;
    },

    getOppositeGender : function(){
        if (this.getGender() == "U") {
            return "U";
        }
        else if(this.getGender() == "M") {
            return "F";
        }
        else {
            return "M";
        }
    },

    addPartner: function(node) {
        var partner;
        if(node && (this.getGender() == 'U' || node.getGender() == 'U' || node.getGender() == this.getOppositeGender())) {
            partner = node;
        }
        else if(!node) {
            //TODO: set x and y using positioning algorithm
            var x = this.getX() + 300;
            var y = this.getY();
            partner = editor.addNode(x, y, this.getOppositeGender());
        }

        if(partner) {
        partner && partner.addRelativeToList(partner.getPartners(), this);
        partner && this.addRelativeToList(this.getPartners(), partner);
        //TODO: editor.addConnection(this, partner);
        }
    },

    removePartner: function(node) {
        this.removeRelativeFromList(this.getPartners(), node);
    },

    getChildren: function() {
        return this._children;
    },

    getPersonChildren: function() {
        return this._children[0];
    },

    getPhChildren: function() {
        return this._children[1];
    },

    setPersonChildren: function(childrenArray) {
        this._children[0] = childrenArray;
    },

    setPhChildren: function(childrenArray) {
        this._children[1] = childrenArray;
    },

    addChild: function(node) {
        this.addRelativeToList(this.getChildren(), node);
    },

    removeChild: function(node) {
        this.removeRelativeFromList(this.getChildren(), node);
    },

    addSibling: function(node) {
        this.addRelativeToList(this.getSiblings(), node);
    },

    removeSibling: function(node) {
        this.removeRelativeFromList(this.getSiblings(), node);
    },

    getSiblings: function() {
        return this._siblings;
    },

    setSiblings: function(siblingsArray) {
        this._siblings = siblingsArray;
    },

    getPersonSiblings: function() {
        return this.getSiblings()[0];
    },

    getPhSiblings: function() {
        return this.getSiblings()[1];
    },

    setPersonSiblings: function(childrenArray) {
        this._children[0] = childrenArray;
    },

    setPhSiblings: function(childrenArray) {
        this._children[1] = childrenArray;
    },

    isParentOf: function(node) {
        return (this._children.flatten().indexOf(node) > -1);
    },

    isDescendantOf: function(node) {
        if(node.isParentOf(this))
        {
            return true;
        }
        else
        {
            var found = false,
                children = node._children.flatten(),
                i = 0;
            while((i < children.length) && !found) {
                found = this.isDescendantOf(children[i]);
                i++;
            }
            return found;
        }
    },

    isAncestorOf: function(node) {
        return node.isDescendantOf(this);
    },

    isPartnerOf: function(node) {
        return this._partnerConnections.flatten().indexOf(node) > -1;
    },

    getGenderShape: function() {
        return this._genderShape;
    },

    setGenderShape: function(shape) {
        this._genderShape && this._genderShape.remove();
        this._genderShape = shape;
    },

    getAllShapes: function() {
        return this.getDrawing();
    },

    drawShapes: function() {
        this.getAllShapes().toFront();
    },

    remove: function(isRecursive) {
        var me = this;
        this.getMother() && this.getMother().removeChild(this);
        this.getFather() && this.getFather().removeChild(this);
        this.getPhMother() && this.getPhMother().removeChild(this);
        this.getPhFather() && this.getPhFather().removeChild(this);
        this.getPartners().flatten().each( function(partner) {
            me.removePartner(partner);
        });
        if(isRecursive) {
            this.getChildren().flatten().each(function(child) {
                child.remove(true);
            });
        }
        editor.removeNode(this);
        this.getAllGraphics().remove();
    }
});
