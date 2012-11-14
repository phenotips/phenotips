/*
 * Pregnancy is a node contained by a Partnership and is used to group and manage
 * monozygotic twins.
 *
 * @param x the x coordinate at which the pregnancy node will be placed
 * @param y the y coordinate at which the pregnancy node will be placed
 * @param partnership the partnership at which this node is rooted from
 * @param id the id of this node
 */

var Pregnancy = Class.create(AbstractNode, {

    initialize: function($super, x, y, partnership, id) {
        this._partnership = partnership;
        this._PersonChildren = [];
        this._PlaceHolderChildren = [];
        this._PersonGroupChildren = [];
        this._isActive = false;
        this._type = 'Pregnancy';
        $super(x, y, id);
        this._gender = "U";
    },

    /*
     * Generates and returns an instance of PregnancyVisuals
     *
     * @param x,y the x and y coordinates of this pregnancy
     */
    generateGraphics: function(x, y) {
        return new PregnancyVisuals(this, x, y);
    },

    /*
     * Returns "M", "F", or "U" to represent the gender of nodes in this pregnancy
     */
    getGender: function() {
        return this._gender;
    },

    /*
     * Changes the gender of this pregnancy
     *
     * @param gender can be "M", "F", or "U"
     */
    setGender: function(gender) {
        if(this._gender != gender && !this.isGenderLocked()) {
            this._gender = gender;
            this.getChildren().each(function(child) {
                if(child.getGender() != gender) {
                    child.setGender(gender);
                }
            })
        }
    },

    /*
     * Returns true if editing the gender of this pregnancy is currently impossible
     */
    isGenderLocked: function() {
        var lockedGender = false;
        this.getChildren("Person").each(function(child) {
            if(child.getGender() != 'U' && child.getPartners().length != 0) {
                lockedGender = true;
                throw $break;
            }
        });
        return lockedGender;
    },


    /*
     * Returns the partnership from which this node stems
     */
    getPartnership: function() {
        return this._partnership;
    },

    /*
     * Returns an array of nodes that are children of this Pregnancy
     *
     * @param type can filter the array to the specified type (eg. "PlaceHolder", "Person", etc).
     * Multiple types can be passed (eg. getChildren(type1, type2,...,typeN)
     */
    getChildren: function(type) {
        if(arguments.length == 0) {
            return this._PlaceHolderChildren.concat(this._PersonGroupChildren, this._PersonChildren);
        }
        else {
            var children = [];
            for(var i = 0; i < arguments.length; i++) {
                if(this["_" + arguments[i] + "Children"]) {
                    children = children.concat(this["_" + arguments[i] + "Children"]);
                }
            }
            return children;
        }
    },

    /*
     * Returns true if someNode is a child of this pregnancy.
     *
     * @param someNode is an AbstractPerson
     */
    hasChild: function(someNode) {
        return this.getChildren(someNode.getType()).indexOf(someNode) > -1;
    },

    /*
     * Creates a new node and sets it as a child of this pregnancy. Returns the child.
     *
     * @param type can be any type of AbstractPerson
     */
    createChild: function(type) {
        if(this.isChildTypeValid(type) ) {
            var id = this.getID();
            var pos = editor.findPosition({below: id}, ['child']);
            var child = editor.getGraph()["add" + type](pos['child'].x, pos['child'].y, this.getGender());
            this.addChild(child);
            //document.fire("pedigree:child:added", {node: child, 'relatedNodes' : [], 'sourceNode' : this});
        }
    },

    /*
     * Attaches this pregnancy to someNode
     *
     * @param someNode is an AbstractPerson
     */
    addChild: function(someNode) {
        if(someNode && this.canBeParentOf(someNode)) {
            this["_" + someNode.getType() + "Children"].push(someNode);
            someNode.setParentPregnancy(this);
            this.getGraphics().addChild(someNode);
            this.updateActive()
            this.setGender(someNode.getGender());
        }
        return someNode;
    },

    /*
     * Breaks the connection between the pregnancy and child.
     * Returns child;
     *
     * @param child is an AbstractPerson
     */
    removeChild: function(child) {
        this.getGraphics().removeChild(child);
        child.setParentPregnancy(null);
        this["_" + child.getType() + "Children"] = this["_" + child.getType() + "Children"].without(child);
        if(this.getChildren().length == 0) {
            this.remove();
        }
        else {
            this.updateActive();
        }
        return child;
    },

    /*
     * Removes this pregnancy and all the visuals attached to it from the graph.
     * Set isRecursive to true to remove all children, unless they have some other connection
     * to the Proband
     *
     * @param isRecursive can be true or false
     */
    remove: function($super, isRecursive) {
        editor.getGraph().removePregnancy(this);
        if(isRecursive) {
            $super(isRecursive);
        }
        else {
            var me = this;
            this.getChildren().each(function(child) {
                me.removeChild(child);
                if(child.getType() == 'PlaceHolder') {
                    child.remove(false, true);
                }
            });
            this.getGraphics().remove();
            this.getPartnership().removePregnancy(me);
        }
    },

    /*
     * Returns an array of children nodes of this pregnancy
     */
    getLowerNeighbors: function() {
        return this.getChildren();
    },

    /*
     * Returns an empty array
     */
    getSideNeighbors: function() {
        return [];
    },

    /*
     * Returns the parent partnership
     */
    getUpperNeighbors: function() {
        return this.getPartnership();
    },

    /*
     * Returns true if someNode can be a child of this pregnancy
     *
     * @param someNode is an AbstractNode
     */
    canBeParentOf: function(someNode) {
        var compatibleGender = someNode.getGender() == this.getGender() || this.getGender() == "U";
        return compatibleGender && this.isChildTypeValid(someNode.getType()) && this.getPartnership().canBeParentOf(someNode);
    },

    /*
     * Returns true if a child of type 'nodeType' can be added to this pregnancy
     *
     * @nodeType can be any type of AbstractNode, such as "Person", "PlaceHolder" and "PersonGroup"
     */
    isChildTypeValid: function(nodeType) {
        var validType = editor.getGraph()["add" + nodeType];
        var validPlaceHolder = nodeType != "PlaceHolder" || this.getChildren("PlaceHolder").length == 0;
        var validPersonGroup = nodeType != "PersonGroup" || this.getChildren("Person").length == 0;
        var noPersonGroups = this.getChildren("PersonGroup").length == 0;
        return validType && validPlaceHolder && validPersonGroup && noPersonGroups;
    },

    /*
     * Returns true if the only child of this pregnancy is a PlaceHolder
     */
    isPlaceHolderPregnancy: function() {
        var children = this.getChildren();
        return (children.length == 0) || (children.length = 1 && children[0].getType() == "PlaceHolder");
    },

    /*
     * Removes someNode from this pregnancy, and places it into a new one
     *
     * @param someNode is an AbstractPerson
     */
    separateFromPregnancy: function(someNode) {
        if(this.hasChild(someNode)) {
            this.removeChild(someNode);
            this.getPartnership().addChild(someNode);
        }
    },

    /*
     * Toggles the pregnancy junction interactivity active and inactive
     *
     * @param isActive set to true if interacting with the junction is allowed
     */
    setActive: function(isActive) {
        if(this._isActive != isActive) {
            this._isActive = isActive;
            this.getGraphics().updateActive();
        }
    },

    /*
     * Returns true if the user can currently interactively add new children to this pregnancy
     */
    isActive: function() {
        return this._isActive;
    },

    /*
     * Checks whether the pregnancy junction should be active and updates the active status
     */
    updateActive: function() {
        this.setActive(this.getChildren("PlaceHolder", "PersonGroup").length == 0);
    }
});