/**
 * Pregnancy is a node contained by a Partnership and is used to group and manage
 * monozygotic twins.
 *
 * @class Pregnancy
 * @extends AbstractNode
 * @constructor
 * @param {Number} x X coordinate at which the pregnancy node will be placed
 * @param {Number} y Y coordinate at which the pregnancy node will be placed
 * @param {Partnership} partnership The partnership that has this pregnancy
 * @param {Number} id ID of this node
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
        partnership.addPregnancy(this);
    },

    /**
     * Generates and returns an instance of PregnancyVisuals
     *
     * @method _generateGraphics
     * @param {Number} x X coordinate on the canvas
     * @param {Number} y X coordinate on the canvas
     * @return {PregnancyVisuals}
     * @private
     */
    _generateGraphics: function(x, y) {
        return new PregnancyVisuals(this, x, y);
    },

    /**
     * Returns the gender of the twins in this pregnancy
     *
     * @method getGender
     * @return {String} "M", "F", or "U"
     */
    getGender: function() {
        return this._gender;
    },

    /**
     * Changes the gender of this pregnancy
     *
     * @method setGender
     * @param {String} gender "M", "F", or "U"
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

    /**
     * Returns true if editing the gender of this pregnancy is currently impossible
     *
     * @method isGenderLocked
     * @return {Boolean}
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

    /**
     * Returns the partnership that has this pregnancy
     *
     * @method getPartnership
     * @return {Partnership}
     */
    getPartnership: function() {
        return this._partnership;
    },

    /**
     * Returns an array of nodes that are children of this Pregnancy
     *
     * @method getChildren
     * @param {String} [type]* Filters for types of children to include (such as "Person", "PlaceHolder, etc)
     * @return {Array}
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

    /**
     * Returns true if someNode is a child of this pregnancy.
     *
     * @method hasChild
     * @param {AbstractPerson} someNode
     * @return {Boolean}
     */
    hasChild: function(someNode) {
        return this.getChildren(someNode.getType()).indexOf(someNode) > -1;
    },

    /**
     * Creates a new node and sets it as a child of this pregnancy
     *
     * @method createChild
     * @param {String} type Any type of AbstractPerson
     * @return {AbstractPerson} The created child or null if creation failed
     */
    createChild: function(type) {
        if(this.isChildTypeValid(type) ) {
            var id = this.getID();
            var pos = editor.findPosition({below: id}, ['child']);
            var child = editor.getGraph()["add" + type](pos['child'].x, pos['child'].y, this.getGender());
            this.addChild(child);
            return child;
            //document.fire("pedigree:child:added", {node: child, 'relatedNodes' : [], 'sourceNode' : this});
        }
        return null;
    },

    /**
     * Creates a new node and sets it as a child of this pregnancy.
     * Creates action stack entry.
     *
     * @method createChildAction
     */
    createChildAction: function() {
        var childInfo = this.createChild("PlaceHolder").getInfo();
        var nodeID = this.getID();
        var undo = function() {
            var child = editor.getGraph().getNodeMap()[childInfo.id];
            child && child.remove();
        };
        var redo = function() {
            var preg = editor.getGraph().getNodeMap()[nodeID];
            var child = editor.getGraph().addPlaceHolder(childInfo.x, childInfo.y, preg.getGender(), childInfo.id);
            preg.addChild(child);
        }
        editor.getActionStack().push({undo: undo, redo: redo});
    },

    /**
     * Adds someNode as this pregnancy's child
     *
     * @method addChild
     * @param someNode is an AbstractPerson
     */
    addChild: function(someNode) {
        if(someNode && this.canBeParentOf(someNode)) {
            this.setGender(someNode.getGender());
            this["_" + someNode.getType() + "Children"].push(someNode);
            someNode.setParentPregnancy(this);
            this.getGraphics().addChild(someNode);
            this.updateActive()
        }
    },

    /**
     * Breaks the connection between the pregnancy and child.
     * Returns child;
     *
     * @method removeChild
     * @param {AbstractPerson} child Child of this pregnancy
     */
    removeChild: function(child) {
        this.getGraphics().removeChild(child);
        child.setParentPregnancy(null);
        this["_" + child.getType() + "Children"] = this["_" + child.getType() + "Children"].without(child);
        if(this.getChildren().length == 0) {
            this.remove(false);
        }
        else {
            this.updateActive();
        }
        var p = this.getPartnership().getPartners();
        if(this.isPlaceHolderPregnancy() && (p[0].getType() == "PlaceHolder" || p[1].getType() == "PlaceHolder")) {
            this.remove(false);
        }
    },

    /**
     * Breaks connections with all related nodes and removes this node from
     * the record.
     *
     * @method remove
     * @param [$super]
     * @param [isRecursive=false] {Boolean} set to true to remove all nodes that will result in being unrelated to the proband
     * @param [skipConfirmation=false] {Boolean} if true, no confirmation box will pop up
     * @return {Object} in the form
     *
     {
     confirmed: true/false,
     affected: {
     PersonNodes : [Person1, Person2, ...],
     PartnershipNodes : [Partnership1, Partnership2, ...],
     PregnancyNodes : [Pregnancy1, Pregnancy2, ...],
     PersonGroupNodes : [PersonGroup1, PersonGroup2, ...],
     PlaceHolderNodes : [PlaceHolder1, PlaceHolder2, ...]
     },
     created: [PlaceHolder1, PlaceHolder2, ...]
     }
     */
    remove: function($super, isRecursive, skipConfirmation) {
        editor.getGraph().removePregnancy(this);
        if(isRecursive) {
            return $super(isRecursive, skipConfirmation);
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
            return $super(false);
        }
    },

    /***
     * Returns lower neighbors of this node in the tree graph
     *
     * @method getLowerNeighbors
     * @return {Array}
     */
    getLowerNeighbors: function() {
        return this.getChildren();
    },

    /**
     * Returns side neighbors of this node in the tree graph
     *
     * @method getSideNeighbors
     * @return {Array}
     */
    getSideNeighbors: function() {
        return [];
    },

    /**
     * Returns upper neighbors of this node in the tree graph
     *
     * @method getUpperNeighbors
     * @return {Array}
     */
    getUpperNeighbors: function() {
        return this.getPartnership();
    },

    /**
     * Returns true if someNode can be a child of this pregnancy
     *
     * @method canBeParentOf
     * @param {AbstractPerson} someNode
     * @return {Boolean}
     */
    canBeParentOf: function(someNode) {
        var compatibleGender = someNode.getGender() == this.getGender() || this.getGender() == "U";
        return compatibleGender && this.isChildTypeValid(someNode.getType()) && this.getPartnership().canBeParentOf(someNode);
    },

    /**
     * Returns true if a child of type 'nodeType' can be added to this pregnancy
     *
     * @method isChildTypeValid
     * @nodeType can be any type of AbstractNode, such as "Person", "PlaceHolder" and "PersonGroup"
     * @return {Boolean}
     */
    isChildTypeValid: function(nodeType) {
        var validType = editor.getGraph()["add" + nodeType];
        var validPlaceHolder = nodeType != "PlaceHolder" || this.getChildren("PlaceHolder").length == 0;
        var validPersonGroup = nodeType != "PersonGroup" || this.getChildren("Person").length == 0;
        var noPersonGroups = this.getChildren("PersonGroup").length == 0;
        return validType && validPlaceHolder && validPersonGroup && noPersonGroups;
    },

    /**
     * Returns true if the only child of this pregnancy is a PlaceHolder
     *
     * @method isPlaceHolderPregnancy
     * @return {Boolean}
     */
    isPlaceHolderPregnancy: function() {
        var children = this.getChildren();
        return (children.length == 0) || (children.length = 1 && children[0].getType() == "PlaceHolder");
    },

    /**
     * Creates a new pregnancy for parent partnership and makes someNode a child of that pregnancy
     *
     * @method separateFromPregnancy
     * @param {AbstractPerson} someNode Child of this pregnancy
     */
    separateFromPregnancy: function(someNode) {
        if(this.hasChild(someNode)) {
            this.removeChild(someNode);
            this.getPartnership().addChild(someNode);
        }
    },

    /**
     * Returns True if this pregnancy has any children that are not adopted
     *
     * @method hasNonAdoptedChildren
     * @returns {Boolean}
     */
    hasNonAdoptedChildren: function() {
        var children = this.getChildren("Person");
        for(var i = 0; i < children.length; i++) {
            if(!children[i].isAdopted())
                return true;
        }
        return false;
    },

    /**
     * Toggles the pregnancy bubble interactivity active or inactive
     *
     * @method setActive
     * @param {Boolean} isActive Set to True to make pregnancy interactive
     */
    setActive: function(isActive) {
        if(this._isActive != isActive) {
            this._isActive = isActive;
            this.getGraphics().updateActive();
        }
    },

    /**
     * Returns True if the user can currently interactively add new children to this pregnancy
     *
     * @method isActive
     * @return {Boolean}
     */
    isActive: function() {
        return this._isActive;
    },

    /**
     * Checks whether the pregnancy junction should be active and updates the active status
     *
     * @method updateActive
     */
    updateActive: function() {
        var adoptedChild = this.getChildren()[0];
        adoptedChild = (adoptedChild && adoptedChild.isAdopted());
        this.setActive(this.getChildren("PlaceHolder", "PersonGroup").length == 0 && !adoptedChild);
    },

    /**
     * Returns object with serialization data
     *
     * @method getInfo
     * @param [$super]
     * @return {Object}
     */
    getInfo: function($super) {
        var info = $super();
        info['partnershipID'] = this.getPartnership().getID();
        info['gender'] = this.getGender();
        info['childrenIDs'] = [];
        this.getChildren().each(function(child) {
            info['childrenIDs'].push(child.getID());
        });
        return info;
    },

    /**
     * Applies properties found in info to this Pregnancy
     *
     * @method loadInfo
     * @param [$super]
     * @param info
     */
    loadInfo: function($super, info) {
        var me = this;
        if($super(info) && info.gender) {
            if(this.getGender() != info.gender)
                this.setGender(info.gender);
            if(info.childrenIDs){
                info.childrenIDs.each(function(id) {
                    var child = editor.getGraph().getNodeMap()[id];
                    child && me.addChild(child);
                });
            }
            return true;
        }
        return false;
    }
});