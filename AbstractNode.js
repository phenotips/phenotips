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
        this._partners = [[/*nodes*/],[/*placeHolders*/]];
        this._children = [[/*nodes*/],[/*placeHolders*/]];
        this._siblings = [[/*nodes*/],[/*placeHolders*/]];
        this._drawing = this.generateDrawing();
    },

    generateGenderShape: function() {
        var radius = editor.graphics.getRadius(),
            shape;
        if (this.getGender() == 'male') {
            shape = editor.paper.rect(this.getX() - radius, this.getY() - radius, radius * 2, radius * 2);
        }
        else if (this.getGender() == 'female') {
            shape = editor.paper.circle(this.getX(), this.getY(), radius);
        }
        else {
            shape = editor.paper.rect(this.getX() - radius * (Math.sqrt(2.5)/2), this.getY() - radius * (Math.sqrt(2.5)/2),
                radius * Math.sqrt(2.5), radius * Math.sqrt(2.5)).transform("...r45");
        }
        return shape.attr(editor.graphics._attributes.nodeShape);
    },

    generateDrawing: function() {
        return editor.paper.set(this.generateGenderShape());
    },

    getDrawing: function() {
        return this._drawing;
    },

    setDrawing: function(drawing) {
        this._drawing = drawing;
    },

    parseGender: function(gender) {
        return (gender == 'male' || gender == 'female')?gender:'unknown';
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
        if(node.getGender() == 'female') {
            if(this.getPhMother() == null && this.getMother() == null) {
                this.setPhMother(node);
            }
            else if(this.getMother().getGender() == 'unknown' && this.getFather() == null) {
                this.setFather(this.getMother());
                this.setPhMother(node);
            }
        }
        else if(node.getGender() == 'male') {
            if(this.getFather() == null && this.getPhFather() == null) {
                this.setPhFather(node);
            }
            else if(this.getFather().getGender() == 'unknown' && this.getMother() == null) {
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
        if(node.getGender() == 'female') {
            if(this.getMother() == null) {
                this.setMother(node);
                this.getPhMother() && this.getPhMother().remove();
            }
            else if(this.getMother().getGender() == 'unknown' && this.getFather() == null) {
                this.setFather(this.getMother());
                this.setMother(node);
                this.getPhMother() && this.getPhMother().remove();
                this.getPhFather() && this.getPhFather().remove();
            }
        }
        else if(node.getGender() == 'male') {
            if(this.getFather() == null) {
                this.setFather(node);
                this.getPhFather() && this.getPhFather().remove();
            }
            else if(this.getFather().getGender() == 'unknown' && this._mother == null) {
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
        return this._partners;
    },

    getPersonPartners: function() {
        return this._partners[0];
    },

    setPersonPartners: function(partnersArray) {
        this._partners[0] = partnersArray;
    },

    getPhPartners: function() {
        return this._partners[1];
    },

    setPhPartners: function(partnersArray) {
        this._partners[1] = partnersArray;
    },

    addPartner: function(node) {
        this.addRelativeToList(this.getPartners(), node);
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
        return this._partners.flatten().indexOf(node) > -1;
    },

    getGenderShape: function() {
        return this._genderShape;
    },

    setGenderShape: function(shape) {
        this._genderShape && this._genderShape.remove();
        this._genderShape = shape;
    },
    getAllGraphics: function() {
        return this.getDrawing();
    },
    draw: function() {
        this.getAllGraphics() && this.getAllGraphics().toFront()
    },

    remove: function(isRecursive) {
        this._mother && this._mother.removeChild(this);
        this._father && this._father.removeChild(this);
        if(isRecursive) {
            this._children.flatten().each(function(child) {
                child.remove(true);
            });
        }
        editor.removeNode(this);
        this.getAllGraphics().remove();
    }
});
