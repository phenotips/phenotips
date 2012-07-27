
/*
 * A general superclass for nodes on the Pedigree graph. Contains connections
 * and basic information about gender, ID and a graphics element.
 */

var AbstractNode = Class.create( {

    initialize: function(x, y, gender, id) {
        this._id = (id != null) ? id : editor.generateID();
        this._gender = this.parseGender(gender);
        this._mother = null;
        this._father = null;
        this._phMother = null;
        this._phFather = null;
        this._partnerConnections = [];//[[/*nodes*/],[/*placeHolders*/]];
        this._children = [[/*nodes*/],[/*placeHolders*/]];
        this._siblings = [[/*nodes*/],[/*placeHolders*/]];
        this._graphics = this.generateGraphics(x, y);
      },

    /*
     * Initializes the object responsible for creating graphics for this node
     *
     * @param x the x coordinate on the canvas at which the node is centered
     * @param y the y coordinate on the canvas at which the node is centered
     */
    generateGraphics: function(x, y) {
        return new AbstractNodeVisuals(this, x, y);
    },

    /*
     * Returns the object responsible for creating graphics
     */
    getGraphics: function() {
        return this._graphics;
    },

    /*
     * Reads a string of input and converts it into the standard gender format of "M","F" or "U".
     * Defaults to "U" if string is not recognized
     *
     * @param gender the string to be parsed
     */
    parseGender: function(gender) {
        return (gender == 'M' || gender == 'F')?gender:'U';
    },

    /*
     * Returns the unique ID of this node
     */
    getID: function() {
        return this._id;
    },

    /*
     * Returns "U", "F" or "M" depending on the gender of this node
     */
    getGender: function() {
        return this._gender;
    },

    /*
     * Updates the gender of this node and (optionally) updates the
     * graphics
     *
     * @param gender should be "U", "F", or "M" depending on the gender
     * @param forceDraw set to true if you want to update the graphics
     */
    setGender: function(gender, forceDraw) {
        this._gender = this.parseGender(gender);
        forceDraw && this.getGraphics().drawShapes();
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

    getPartnerConnections: function() {
        return this._partnerConnections;
    },

    getPartners: function() {
        var partners = [];
        var me = this;
        this.getPartnerConnections().each(function(conn) {
            var partner = conn.getPartnerOf(me);
            partner && partners.push(partner);
        });
        return partners;
    },

    addPartnerConnection: function(connection) {
        this._partnerConnections.push(connection);
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

    /*
     * Returns a string representing the opposite gender of this node ("M" or "F"). Returns "U"
     * if the gender of this node is unknown
     */
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
        if(node && this.getPartners().indexOf(node) == -1 &&
            (this.getGender() == 'U' || node.getGender() == 'U' || node.getGender() == this.getOppositeGender())) {
            partner = node;
        }
        else if(!node) {
            //TODO: set x and y using positioning algorithm
            var x = this.getGraphics().getAbsX() + 300;
            var y = this.getGraphics().getAbsY();
            partner = editor.addNode(x, y, this.getOppositeGender());
        }

        if(partner) {
            var connection = new PartnerConnection(this, partner);
            this.getPartnerConnections().push(connection);
            partner.addPartnerConnection(connection);
//            partner && partner.addRelativeToList(partner.getPartnerConnections(), this);
//            partner && this.addRelativeToList(this.getPartnerConnections(), partner);
            //TODO: editor.addConnection(this, partner);
        }
    },

    removePartner: function(node) {
        //this.removeRelativeFromList(this.getPartners(), node);
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

    /*
     * Returns true if this node is a descendant of otherNode
     *
     * @param otherNode can be a Person or a PlaceHolder
     */
    isDescendantOf: function(otherNode) {
        if(otherNode.isParentOf(this)) {
            return true;
        }
        else {
            var found = false,
                children = otherNode._children.flatten(),
                i = 0;
            while((i < children.length) && !found) {
                found = this.isDescendantOf(children[i]);
                i++;
            }
            return found;
        }
    },

    /*
     * Returns true if this node is an ancestor of otherNode
     *
     * @param otherNode can be a Person or a PlaceHolder
     */
    isAncestorOf: function(otherNode) {
        return otherNode.isDescendantOf(this);
    },

    /*
     * Returns true if this node is a partner of otherNode
     *
     * @param otherNode can be a Person or a PlaceHolder
     */
    isPartnerOf: function(otherNode) {
        return this._partnerConnections.flatten().indexOf(otherNode) > -1;
    },

    /*
     * Breaks connections with all related nodes and removes this node from
     * the record.
     * (Optional) Removes all descendants of this node as well
     * (Optional) Removes all the graphics for this node and (optionally)
     * his descendants
     *
     * @param isRecursive set to true if you want to remove all descendants as well
     * @param removeVisuals set to true if you want to remove the graphics as well
     */
    remove: function(isRecursive, removeVisuals) {
        var me = this;
        this.getMother() && this.getMother().removeChild(this);
        this.getFather() && this.getFather().removeChild(this);
        this.getPhMother() && this.getPhMother().removeChild(this);
        this.getPhFather() && this.getPhFather().removeChild(this);
        this.getPartnerConnections().flatten().each( function(partner) {
            me.removePartner(partner);
        });
        if(isRecursive) {
            this.getChildren().flatten().each(function(child) {
                child.getPartnerConnections().flatten().each(function(partner) {
                   partner.remove(false, removeVisuals);
                });
                child.remove(true, removeVisuals);
            });
        }
        editor.removeNode(this);
        removeVisuals && this.getGraphics().remove();
    }
});
