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
        this._isActive = false;
        $super(x, y, id);
        this._type = 'Pregnancy';
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

    getGender: function() {
        return this._gender;
    },

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
     * @param type can filter the array to the specified type (eg. "PlaceHolder", "Person", etc)
     */
    getChildren: function(type) {
        if(this["_" + type + "Children"]) {
            return this["_" + type + "Children"];
        }
        else {
            return this._PlaceHolderChildren.concat(this._PersonChildren);
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
     * @param gender "M", "F" or "U"
     */
    createChild: function(type, gender) {
        if((gender == this.getGender() || this.getGender() == "U") && editor.getGraph()["add" + type]) {
            var id = this.getID();
            var pos = editor.findPosition({below: id}, ['child']);
            var child = editor.getGraph()["add" + type](pos['child'].x, pos['child'].y, gender);
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
        //TODO: figure out whether to remove placeholders as well
        if(someNode && this.canBeParentOf(someNode)) {
            this["_" + someNode.getType() + "Children"].push(someNode);
            someNode.setParentPregnancy(this);
            this.getGraphics().addChild(someNode);
            this.setActive(someNode.getType() == 'Person');
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
            this.setActive(child.getType() == "PlaceHolder");
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
        editor.getGraph().removePartnership(this);
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
     */
    canBeParentOf: function(someNode) {
        var validTwin = this.getGender() == 'U' || this.getGender() == someNode.getGender();
        return this.getPartnership().canBeParentOf(someNode) && validTwin;
    },

    /*
     * Returns true is the only child of this pregnancy is a PlaceHolder
     */
    isPlaceHolderPregnancy: function() {
        var children = this.getChildren();
        return children.length = 1 && children[0].getType() == "PlaceHolder";
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

    setActive: function(isActive) {
        if(this._isActive != isActive) {
            this._isActive = isActive;
            this.getGraphics().updateActive();
        }
    },

    isActive: function() {
        return this._isActive;
    }
});