
/*
 * A general superclass for nodes on the Pedigree graph. Contains connections
 * and basic information about gender, ID and a graphics element.
 */

var AbstractNode = Class.create( {

    initialize: function(x, y, gender, id) {
        this._id = (id != null) ? id : editor.generateID();
        this._gender = this.parseGender(gender);
        this._parents = null;
        this._partnerships = [];
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

    /*
     * Returns an array of Partnership objects of this node
     */
    getPartnerships: function() {
        return this._partnerships;
    },

    /*
     * Returns an array nodes that share a Partnership with this node
     */
    getPartners: function() {
        var partners = [];
        var me = this;
        this.getPartnerships().each(function(partnership) {
            var partner = partnership.getPartnerOf(me);
            partner && partners.push(partner);
        });
        return partners;
    },

    /*
     * Adds a new partnership to the list of partnerships of this node
     *
     * @param partnership is a Partnership object with this node as one of the partners
     */
    addPartnership: function(partnership) {
        this._partnerships.push(partnership);
    },

    /*
     * Removes a partnership from the list of partnerships
     *
     * @param partnership is a Partnership object with this node as one of the partners
     */
    removePartnership: function(partnership) {
        this._partnerships = this._partnerships.without(partnership);
    },

    /*
     * Returns a Vertical containing this node and the parent nodes
     */
    getParents: function() {
        return this._parents;
    },

    /*
     * Replaces the parents Vertical with the one passed in the parameter
     *
     * @param vertical is a Vertical object that contains this node and its parents
     */
    setParents: function(vertical) {
        this._parents = vertical;
    },

    createParents: function() {
        if(this.getParents() == null) {
            var mother = editor.addNode(this.getGraphics().getAbsX() + 100, this.getGraphics().getAbsY() - 260, "F", false),
                father = editor.addNode(this.getGraphics().getAbsX() - 100, this.getGraphics().getAbsY() - 260, "M", false),
                partnership = new Partnership(mother, father);
            this.addParents(partnership);
        }
    },

    addParents: function(partnership) {
        if(this.getParents() == null) {
            partnership.addChild(this);
        }
    },

    /*
     * Returns a string representing the opposite gender of this node ("M" or "F"). Returns "U"
     * if the gender of this node is unknown
     */
    getOppositeGender : function() {
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
    /*
     * Creates a new node and generates a Partnership with this node.
     * Returns the Partnership.
     *
     * @param isPlaceHolder set to true if the new partner should be a PlaceHolder
     */
    createPartner: function(isPlaceHolder) {
        //TODO: set x and y using positioning algorithm
        var x = this.getGraphics().getAbsX() + 300,
            y = this.getGraphics().getAbsY(),
            partner = editor.addNode(x, y, this.getOppositeGender(), isPlaceHolder);
        return this.addPartner(partner);
    },

    /*
     * Creates a new Partnership with the partner passed in the parameter.
     * Returns the new Partnership
     *
     * @param partner a Person or PlaceHolder.
     */
    addPartner: function(partner) {
        if(partner && this.getPartners().indexOf(partner) == -1 && this.canPartnerWith(partner)) {
            var connection = new Partnership(this, partner);
            this.getPartnerships().push(connection);
            partner.addPartnership(connection);
            //TODO: editor.addConnection(this, partner);
            connection.createChild(true);
            return connection;
        }
    },

    /*
     * Returns an array of nodes that are children from all of this node's Partnerships.
     * The array includes PlaceHolders.
     */
    getChildren: function() {
        var children = [];
        this.getPartnerships().each(function(partnership) {
            children = children.concat(partnership.getChildren())
        });
        return children;
    },

    createChild: function(isPlaceHolder) {
        //TODO: set x and y using positioning algorithm
        var x = this.getGraphics().getAbsX() + 150,
            y = this.getGraphics().getAbsY() + 200,
            child = editor.addNode(x, y, "U", isPlaceHolder);
        return this.addChild(child);
    },

    addChild: function(node) {
        var partnership = this.createPartner(true);
        partnership.addChild(node);
    },

    isParentOf: function(node) {
        return (this.getChildren().indexOf(node) > -1);
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
                children = otherNode.getChildren(),
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
        return this.getPartners().indexOf(otherNode) > -1;
    },

    /*
     * Returns true if this node is related to otherNode
     *
     * @param otherNode can be a Person or a PlaceHolder
     */
    isRelatedTo: function(otherNode) {
        var getNeighbors = function(node) {
            var neighbors = [];
            node.getParents() && neighbors.push(node.getParents().getPartner1(), node.getParents().getPartner2());
            neighbors = neighbors.concat(node.getChildren());
            neighbors = neighbors.concat(node.getPartners());
            return neighbors;
        };

        var front = [this],
            next = [],
            visited = {};
        visited[this.getID()] = true;
        while(front.length > 0) {
            for(var i = 0; i<front.length; i++) {
                var neighbors = getNeighbors(front[i]);
                for(var k = 0; i<neighbors.length; k++) {
                    if(neighbors[k] == otherNode) {
                        return true;
                    }
                    else if(!visited[neighbors[k].getID()]) {
                        next.push(neighbors[k]);
                        visited[neighbors[k].getID()] = true;
                    }
                }
            }
            front.clear();
            front = next;
            next.clear();
        }
        return false;
    },

    canPartnerWith: function(node) {
        return (this.getOppositeGender() == node.getGender() || this.getGender() == "U" || node.getGender() == "U");
    },

    canBeParentOf: function(node) {
        var incompatibleParents = node.getParents();
        var incompatibleBirthDate = this.getBirthDate() && node.getBirthDate() && this.getBirthDate() < node.getBirthDate();
        var incompatibleDeathDate = this.getDeathDate() && node.getBirthDate() && this.getDeathDate() < node.getBirthDate().clone().setDate(node.getBirthDate().getDate()-700);
        var incompatibleState = this.isFetus();

        return !incompatibleParents && !incompatibleBirthDate && !incompatibleDeathDate && !incompatibleState;

    },

    /*
     * Breaks connections with all related nodes and removes this node from
     * the record.
     * (Optional) Removes all descendant nodes and their relatives that will become unrelated to the proband as a result
     * (Optional) Removes all the graphics for this node and (optionally)
     * his descendants
     *
     * @param isRecursive set to true if you want to remove all unrelated descendants as well
     * @param removeVisuals set to true if you want to remove the graphics as well
     */
    remove: function(isRecursive, removeVisuals) {
        var me = this,
            toRemove = [];
        this.getPartnerships().each(function(partnership) {
            partnership.getVerticals().each(function(vertical) {
                toRemove.push(vertical.getChild());
                vertical.remove();
            });
            toRemove.push(partnership.getPartnerOf(me));
            partnership.remove();
        });

        isRecursive && toRemove.each(function(node) {
            !(node.isRelatedTo(me)) && node.remove(true, true);
        });
        this.getParents() && this.getParents().removeChild(me);
        editor.removeNode(this);
        removeVisuals && this.getGraphics().remove();
    },

    getX: function() {
        return this.getGraphics().getAbsX();
    },

    setX: function(x) {
        this.getGraphics().setAbsX(x)
    },

    getY: function() {
        return this.getGraphics().getAbsY();
    },

    setY: function(y) {
        this.getGraphics().setAbsX(y)
    },

    getPos: function() {
        return [this.getX(), this.getY()];
    },

    move: function(x,y) {
        this.getGraphics().move(x,y);
    },

    moveX: function(x) {
        this.move(x, this.getY());
    },

    moveY: function(y) {
        this.move(this.getX(), y);
    }
});