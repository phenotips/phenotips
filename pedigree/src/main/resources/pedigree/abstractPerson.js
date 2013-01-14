
/**
 * A general superclass for nodes on the Pedigree graph. Contains information about related nodes
 * and some properties specific for people. Creates an instance of AbstractNodeVisuals on initialization
 *
 * @class AbstractPerson
 * @extends AbstractNode
 * @constructor
 * @param x {Number} the x coordinate on the canvas
 * @param y {Number} the y coordinate on the canvas
 * @param gender {String} can be "M", "F", or "U"
 * @param [id] {Number} the id of the node
 */

var AbstractPerson = Class.create(AbstractNode, {

    initialize: function($super, x, y, gender, id) {
        this._partnershipNodes = [];
        this._gender = this.parseGender(gender);
        this._parentPregnancy = null;
        this._isAdopted = false;
        this._type = this._type ? this._type : "AbstractPerson";
        $super(x, y, id);
      },

    /**
     * Initializes the object responsible for creating graphics for this node
     *
     * @method generateGraphics
     * @param x {Number} the x coordinate on the canvas at which the node is centered
     * @param y {Number} the y coordinate on the canvas at which the node is centered
     * @return {AbstractPersonVisuals}
     */
    generateGraphics: function(x, y) {
        return new AbstractPersonVisuals(this, x, y);
    },

    /**
     * Returns the parents' Partnership node
     *
     * @method getParentPartnership
     * @return {Null|Partnership} returns null if this person has no parents
     */
    getParentPartnership: function() {
        var preg = this.getParentPregnancy();
        if(preg) {
            return preg.getPartnership();
        }
        return null;
    },

    /**
     * Returns the Pregnancy from which this node stems
     *
     * @method getParentPregnancy
     * @return {Pregnancy}
     */
    getParentPregnancy: function() {
        return this._parentPregnancy;
    },

    /**
     * Replaces the the parent Pregnancy associated with this node with the one passed in the parameter
     *
     * @method setParentPregnancy
     * @param pregnancy {Pregnancy} a Pregnancy object that has this node listed as a child
     */
    setParentPregnancy: function(pregnancy) {
        this._parentPregnancy = pregnancy;
    },

    /**
     * Returns an array containing the two parents of this node.
     *
     * @method getParents
     * @return {Null|Array} in the form of [parent1, parent2]. Null if this person has no parents.
     */
    getParents: function() {
        if(this.getParentPartnership()){
            return [this.getParentPartnership().getPartners()[0], this.getParentPartnership().getPartners()[1]]
        }
        return null;
    },

    /**
     * Returns true if this node is a descendant of otherNode
     *
     * @method isDescendantOf
     * @param otherNode {Person|PlaceHolder}
     * @return {Boolean}
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

    /**
     * Reads a string of input and converts it into the standard gender format of "M","F" or "U".
     * Defaults to "U" if string is not recognized
     *
     * @method parseGender
     * @param gender {String} the string to be parsed
     * @return {String} the gender in the standard form ("M", "F", or "U")
     */
    parseGender: function(gender) {
        return (gender == 'M' || gender == 'F')?gender:'U';
    },

    /**
     * Returns "U", "F" or "M" depending on the gender of this node
     *
     * @method getGender
     * @return {String}
     */
    getGender: function() {
        return this._gender;
    },

    /**
     * Collects gender information from twins and partners of this person
     *
     * @method getTwinPartnerGenders
     * @return {Object} in the form of {nodeID1 : gender1, nodeID2: gender2, ...}
     */
    getTwinPartnerGenders: function(visited) {
        visited = visited ? visited : {};
        if(!visited[this.getID()]) {
            visited[this.getID()] = this.getGender();
            var pp = this.getParentPregnancy();
            pp && (visited[pp.getID()] = pp.getGender());
            this.getPartners().each(function(partner) {
                visited = partner.getTwinPartnerGenders(visited);
            });
        }
        return visited;
    },

    /**
     * Updates the gender of this node and updates the
     * graphics. Updates gender of all partners and twins if it is unknown.
     * Returns an array of nodes visited during the partner/twin traversal.
     *
     * @method setGender
     * @param gender {String} should be "U", "F", or "M" depending on the gender
     * @param [visitedNodes] {Array} an array of nodes that were visited during the traversal up until
     *  this node. OMIT this parameter. It is used for internal functionality.
     * @return {Array} list of twins and partners of this node
     */
    setGender: function(gender, visitedNodes) {
        var visited = (visitedNodes instanceof Array) ? visitedNodes : [];
        visited.push(this);
        if(!this.getParentPregnancy()  || !this.getParentPregnancy().isGenderLocked()) {
            if(this.getPartners().length == 0) {
                this._gender = this.parseGender(gender);
                this.getGraphics().setGenderSymbol();
                this.getParentPregnancy() && this.getParentPregnancy().setGender(gender);
            }
            else {
                var me = this;
                this._gender = this.parseGender(gender);
                this.getGraphics().setGenderSymbol();
                this.getPartners().each(function(partner) {
                    if(visited.indexOf(partner) == -1) {
                        visited = partner.setGender(me.getOppositeGender(), visited);
                    }
                });
                this.getParentPregnancy() && this.getParentPregnancy().setGender(gender);
            }
        }
        return visited;
    },

    /**
     * Changes the gender of this node to gender, and updates the gender of all twins and partners
     * if they are affected. Updates the action stack.
     *
     * @method setGenderAction
     * @param gender {String} should be "M", "F", or "U"
     */
    setGenderAction: function(gender) {
        var prevGenders = this.getTwinPartnerGenders();
        this.setGender(gender, []);
        var newGenders = this.getTwinPartnerGenders();
        editor.getActionStack().push({
            undo: AbstractNode.setPropertyToListActionUndo,
            redo: AbstractNode.setPropertyToListActionRedo,
            oldValues: prevGenders,
            newValues: newGenders,
            property: 'Gender'
        });
    },

    /**
     * Changes the adoption status of this Person to isAdopted. Updates the graphics.
     *
     * @method setAdopted
     * @param isAdopted {Boolean} set to true if you want to mark the Person adopted
     */
    setAdopted: function(isAdopted) {
        this._isAdopted = isAdopted;
        //TODO: implement adopted and social parents
        if(isAdopted) {
            this.getGraphics().drawAdoptedShape();
            var twins = this.getTwins("PlaceHolder");
            if(twins.length > 0) {
                twins.each(function(twin) {
                    twin.remove();
                })
            }
        }
        else {
            this.getGraphics().removeAdoptedShape();
        }
        var preg = this.getParentPregnancy();
        preg && preg.updateActive();
    },

    /**
     * Changes the adoption status of this Person to isAdopted. Updates the graphics. Updates the
     * action stack.
     *
     * @method setAdoptedAction
     * @param isAdopted {Boolean} set to true if you want to mark the Person adopted
     */
    setAdoptedAction: function(isAdopted) {
        var oldStatus = this.isAdopted();
        if(oldStatus != isAdopted) {
            var twin = this.getTwins("PlaceHolder")[0],
                twinInfo = twin ? twin.getInfo() : null;
            this.setAdopted(isAdopted);
            var nodeID = this.getID();
            var undo = function() {
                var node = editor.getGraph().getNodeMap()[nodeID];
                if(node) {
                    node.setAdopted(oldStatus);
                    if(twinInfo && node.getParentPregnancy()) {
                        var newPh = editor.getGraph().addPlaceHolder(twinInfo.x, twinInfo.y, twinInfo.gender, twinInfo.id);
                        node.getParentPregnancy().addChild(newPh);
                    }
                }
            };
            var redo = function() {
                var node = editor.getGraph().getNodeMap()[nodeID];
                node && node.setAdopted(isAdopted);
            };
            editor.getActionStack().push({undo: undo, redo: redo});
        }
    },

    /**
     * Returns true if this Person is marked adopted
     *
     * @method isAdopted
     * @return {Boolean}
     */
    isAdopted: function() {
        return this._isAdopted;
    },

    /**
     * Returns true if this person has twin children
     *
     * @method hasTwins
     * @return {Boolean}
     */
    hasTwins: function() {
        var partnerships = this.getPartnerships();
        for(var i = 0; i < partnerships.length; i++) {
            if(partnerships[i].hasTwins())
                return true;
        }
        return false;
    },

    /**
     * Returns true if this person has non-adopted children
     *
     * @method hasNonAdoptedChildren
     * @return {Boolean}
     */
    hasNonAdoptedChildren: function() {
        var partnerships = this.getPartnerships();
        for(var i = 0; i < partnerships.length; i++) {
            if(partnerships[i].hasNonAdoptedChildren())
                return true;
        }
        return false;
    },

    /**
     * Returns an array of Partnership objects of this node
     *
     * @method getPartnerships
     * @return {Array}
     */
    getPartnerships: function() {
        return this._partnershipNodes;
    },

    /**
     * Returns the Partnership affiliated with partner
     *
     * @method getPartnership
     * @param partner {AbstractPerson}
     * @return {Null|Partnership} returns Null if this node has no Partnership with partner
     */
    getPartnership: function(partner) {
        if(partner) {
            var partnerships = this.getPartnerships();
            for(var i = 0; i < partnerships.length; i++) {
                if(partnerships[i].getPartnerOf(this).getID() == partner.getID()) {
                    return partnerships[i];
                }
            }
        }
        return null;
    },

    /**
     * Returns an array nodes that share a Partnership with this node
     *
     * @method getPartners
     * @return {Array} in the form of [partner1, partner2, ...]
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

    /**
     * Adds a new partnership to the list of partnerships of this node
     *
     * @method addPartnership
     * @param partnership {Partnership} should have this node as one of the partners
     */
    addPartnership: function(partnership) {
       if(this.getPartners().indexOf(partnership.getPartnerOf(this)) == -1) {
           this._partnershipNodes.push(partnership);
       }
    },

    /**
     * Removes partnership from the list of partnerships
     *
     * @method removePartnership
     * @param partnership {Partnership} should have this node as one of the partners
     */
    removePartnership: function(partnership) {
        if(partnership) {
            var target = null;
            this._partnershipNodes.each(function(p) {
                if(p.getID() == partnership.getID())
                    target = p;
            });
            if(target)
                this._partnershipNodes = this._partnershipNodes.without(target);
        }
    },

    /**
     * Creates a Partnership of two new Person nodes of opposite gender, and adds this node as a child of the
     * partnership
     *
     * @method createParents
     * @return {Partnership} the resulting partnership between the two new parents
     */
    createParents: function() {
        if(this.getParentPartnership() == null) {
            var positions = editor.findPosition ({above : this.getID()}, ['mom', 'dad']);
            var mother = editor.getGraph().addPerson(positions['mom'].x, positions['mom'].y, "F", false),
                father = editor.getGraph().addPerson(positions['dad'].x, positions['dad'].y, "M", false);

            var joinPosition = editor.findPosition({join : [mother.getID(), father.getID()]});
            var partnership = editor.getGraph().addPartnership(joinPosition.x, joinPosition.y, mother, father);
            
            //document.fire('pedigree:parents:added', {'node' : partnership, 'relatedNodes' : [mother, father], 'sourceNode' : this});
            return this.addParents(partnership);
        }
    },

    /**
     * Sets parents to the partnership passed in the parameter, and adds this node to partnership's list of children
     *
     * @method addParents
     * @param partnership {Partnership}
     * @return {Partnership} returns the partnership that was added9
     */
    addParents: function(partnership) {
        if(this.getParentPartnership() == null) {
            partnership.addChild(this);
        }
        return partnership;
    },

    /**
     * Sets this node as a child of a new partnership between parent and a new placeholder.
     *
     * @method addParent
     * @param parent {AbstractPerson}
     * @return {Null|Partnership} resulting parent partnership. Null if 'parent' can be a parent of this person
     */
    addParent: function(parent) {
        if(parent.canBeParentOf(this)) {
            var partnership = parent.createPartner(true, true);
            partnership.addChild(this);
            return partnership;
        }
        return null;
    },

    /**
     * Returns a string representing the opposite gender of this node ("M" or "F"). Returns "U"
     * if the gender of this node is unknown
     *
     * @method getOppositeGender
     * @return {String} "M", "F" or "U" depending on the gender of this person
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

    /**
     * Creates a new node and generates a Partnership with this node.
     * Returns the Partnership.
     *
     * @method createPartner
     * @param [isPlaceHolder=false] {Boolean} set to true if the new partner should be a PlaceHolder
     * @param [noChild=false] {Boolean} set true to refrain from creating a placeholder child
     * for the resulting partnership
     * @return {Partnership} the resulting partnership
     */
    createPartner: function(isPlaceHolder, noChild) {
        var pos = editor.findPosition({side: this.getID()}),
            gen = this.getOppositeGender(),
            partner = (isPlaceHolder) ? editor.getGraph().addPlaceHolder(pos.x, pos.y, gen) : editor.getGraph().addPerson(pos.x, pos.y, gen);
        var result = this.addPartner(partner, noChild);
        document.fire('pedigree:partner:added', {'node' : partner, 'relatedNodes' : [result], 'sourceNode' : this});
        return result;
    },

    /**
     * Creates a new Partnership with partner.
     * Does not duplicate a partnership if one already exists.
     * Returns the new Partnership or the preexisting partnership
     *
     * @method addPartner
     * @param partner {Person|PlaceHolder}
     * @param [noChild=false] {Boolean} set true to refrain from creating a placeholder child
     * for the resulting partnership
     */
    addPartner: function(partner, noChild) {
        if(this.getPartners().indexOf(partner) != -1){
            return this.getPartnership(partner);
        }
        else if(this.canPartnerWith(partner)) {
            var joinPosition = editor.findPosition({join : [this.getID(), partner.getID()]});
            var partnership = editor.getGraph().addPartnership(joinPosition.x, joinPosition.y, this, partner);

            if(this.getGender() == 'U' && partner.getGender() != 'U') {
                this.setGender(partner.getOppositeGender());
            }
            else if(this.getGender() != 'U' && partner.getGender() == 'U') {
                partner.setGender(this.getOppositeGender());
            }

            if(partnership.getChildren().length == 0 &&
                                            !noChild &&
                                            !(this.getChildlessStatus && this.getChildlessStatus()) &&
                                            !(partner.getChildlessStatus && partner.getChildlessStatus())) {
                partnership.createChild("PlaceHolder", "U", 1);
            }
            
            document.fire('pedigree:partnership:added', {
                'node' : partnership,
                'relatedNodes' : [partner],
                'sourceNode' : this
            });
            return partnership;
        }
    },

    /**
     * Returns an array of nodes that are children from all of this node's Partnerships.
     *
     * @method getChildren
     * @param [type]* {String} can be "Person", "PersonGroup" or "PlaceHolder".
     * @example
     *
     var myPerson = editor.addPerson(100,100, "M", 20);
     var child1 = editor.addPerson(200,200, "M", 21);
     var child2 = editor.addPlaceHolder(300,200, "M", 22);
     myPerson.addChild(child1);
     myPerson.addChild(child2);

     myPerson.getChildren("PlaceHolder", "Person") // -> [child2, child1]
     */
    getChildren: function(type) {
        var args = arguments;
        var children = [];
        this.getPartnerships().each(function(partnership) {
            children = children.concat(partnership.getChildren.apply(partnership, args));
        });
        return children;
    },

    /**
     * Returns true if this person is a parent of non-placeholder children.
     *
     * @method hadChildren
     * @return {Boolean}
     */
    hasChildren: function() {
        return this.getChildren("Person").concat(this.getChildren("PersonGroup")).length > 0;
    },

    /**
     * Creates node of type nodeType and gender nodeGender and a partnership with a new placeholder. Sets
     * the child as a child of this partnership.
     *
     * @method createChild
     * @param nodeType {String} the type for the new child. (eg. "Person", "PlaceHolder", "PersonGroup")
     * @param nodeGender {String} can be "M", "F" or "U".
     */
    createChild: function(nodeType, nodeGender) {
        return this.createPartner(true, true).createChild(nodeType, nodeGender);
    },

    /**
     * Creates a partnership with a new placeholder node and adds childNode to as a child of this partnership.
     *
     * @method addChild
     * @param childNode {AbstractPerson}
     * @return {Null|AbstractPerson} the child node or null in case of error
     */
    addChild: function(childNode) {
        if(this.canBeParentOf(childNode)) {
            var partnership = this.createPartner(true, true);
            partnership.addChild(childNode);
            return childNode;
        }
        return null;
    },

    /**
     * Returns all the nodes that come from the parent pregnancy.
     *
     * @method getTwins
     * @param type {String} the type for the new child. (eg. "Person", "PlaceHolder", "PersonGroup")
     * @return {Array} list of AbstractPerson objects.
     */
    getTwins: function(type) {
        return this.getParentPregnancy().getChildren(type).without(this);
    },

    /**
     * Returns true if this node is a parent of otherNode
     *
     * @method isParentOf
     * @param otherNode {PlaceHolder|Person}
     * @return {Boolean}
     */
    isParentOf: function(otherNode) {
        return (this.getChildren().indexOf(otherNode) > -1);
    },

    /**
     * Returns true if this node is an ancestor of otherNode
     *
     * @method isAncestorOf
     * @param otherNode {Person|PlaceHolder}
     * @return {Boolean}
     */
    isAncestorOf: function(otherNode) {
        return otherNode.isDescendantOf(this);
    },

    /**
     * Returns true if this node is a partner of otherNode
     *
     * @method isPartnerOf
     * @param otherNode {Person|PlaceHolder}
     * @return {Boolean}
     */
    isPartnerOf: function(otherNode) {
        if(otherNode) {
            for(var i = 0; i < this.getPartners().length; i++) {
                if(this.getPartners()[i].getID() == otherNode.getID())
                    return true;
            }
        }
        return false;
    },

    /**
     * Returns true if this node can have a heterosexual Partnership with otherNode
     *
     * @method canPartnerWith
     * @param otherNode {Person|PlaceHolder}
     * @return {Boolean}
     */
    canPartnerWith: function(otherNode) {
        var oppositeGender = (this.getOppositeGender() == otherNode.getGender() || this.getGender() == "U"
                                                                                || otherNode.getGender() == "U");
        var numSteps = this.getStepsToNode(otherNode)[0];
        var oddStepsAway = (numSteps == null || numSteps%2 == 1);
        return oppositeGender && oddStepsAway;
    },

    /**
     * Returns true if this node can be a parent of otherNode
     *
     * @method canBeParentOf
     * @param otherNode {AbstractPerson}
     * @return {Boolean}
     */
    canBeParentOf: function(otherNode) {
        var isDescendant = this.isDescendantOf(otherNode);
        return (this != otherNode) &&
            otherNode.getParentPartnership() == null &&
            this.getChildren().indexOf(otherNode) == -1 &&
            !isDescendant;
    },

    /**
     * Breaks connections with all related nodes and removes this node from
     * the record.
     *
     * @method remove
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
        if(isRecursive) {
            return $super(true, skipConfirmation)
        }
        else {
            this.getPartnerships().each(function(partnership) {
                partnership.remove(false);
            });
            var parentPregnancy = this.getParentPregnancy();
            parentPregnancy && parentPregnancy.removeChild(this);
            this.getGraphics().remove();
            return $super(isRecursive);
        }
    },

    /**
     * Returns all of this node's Partnerships
     *
     * @method getSideNeighbors
     * @return {Array} in the form of [Partnership1, Partnership2, ...]
     */
    getSideNeighbors: function() {
        return this.getPartnerships();
    },

    /**
     * Returns an array with the number of partnerships between this node and otherNode, and the nodes visited
     * in the process of the traversal
     *
     * @method getStepsToNode
     * @param otherNode {AbstractNode} the node whose distance (in partnerships) from this node you're trying to calculate
     * @param [visitedNodes] {Array} an array of nodes that were visited in the result of the traversal. This parameter is used
     * internally so omit it when calling the function
     */
    getStepsToNode: function(otherNode, visitedNodes) {
        var visited = (visitedNodes) ? visitedNodes : [];
        visited.push(this);
        if(this === otherNode) {
            return [0, visited];
        }
        else {
            var numSteps = null;
            this.getPartners().each(function(partner) {
                if(visited.indexOf(partner) == -1) {
                    numSteps = partner.getStepsToNode(otherNode, visited)[0];
                    if(numSteps != null) {
                        numSteps = 1 + numSteps;
                        throw $break;
                    }
                }
            });
            return [numSteps, visited];
        }
    },

    /**
     * Returns the parent's Partnership
     *
     * @method getUpperNeighbors
     * @return {Array}
     */
    getUpperNeighbors: function() {
        return this.getParentPregnancy() ? [this.getParentPregnancy()] : [];
    },

    /**
     * Returns an object containing all the information about this node.
     *
     * @method getInfo
     * @return {Object} in the form
     *
     {
     type: // (type of the node),
     x:  // (x coordinate)
     y:  // (y coordinate)
     id: // id of the node
     gender: //gender of the node
     }
     */
    getInfo: function($super) {
        var info = $super();
        info['gender'] = this.getGender();
        return info;
    },

    /**
     * Applies the properties found in info to this node.
     *
     * @method loadInfo
     * @param info {Object} and object in the form
     *
     {
     type: // (type of the node),
     x:  // (x coordinate)
     y:  // (y coordinate)
     id: // id of the node
     gender: //gender of the node
     }
     * @return {Boolean} true if info was successfully loaded
     */
    loadInfo: function($super, info) {
        if($super(info) && info.gender) {
            if(this.getGender() != info.gender)
                this.setGender(info.gender, null);
            return true;
        }
        return false;
    }
});

