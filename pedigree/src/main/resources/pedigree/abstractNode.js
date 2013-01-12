/**
 * AbstractNode is the general abstract class for nodes on the Pedigree graph. An AbstractNode contains information
 * about its position on the canvas and about relationships with other nodes on the graph.
 *
 * @class AbstractNode
 * @constructor
 * @param x {Number} the x coordinate on the canvas
 * @param y {Number} the y coordinate on the canvas
 * @param [id] {Number} the id of the node
 */

var AbstractNode = Class.create( {

    initialize: function(x, y, id) {
        !this._type && (this._type = "AbstractNode");
        this._id = id ? id : editor.getGraph().generateID();
        this._graphics = this.generateGraphics(x, y);
    },
    
    /**
     * Returns the unique ID of this node
     *
     * @method getID
     * @return {Number} the id of the node
     */
    getID: function() {
        return this._id;
    },

    /**
     * Generates an instance of AbstractNodeVisuals
     *
     * @method generateGraphics
     * @param x {Number} the x coordinate of the node
     * @param y {Number} the y coordinate of the node
     * @return {AbstractNodeVisuals}
     */
    generateGraphics: function(x, y) {
        return new AbstractNodeVisuals(this, x, y);
    },

    /**
     * Returns the object responsible for managing graphics
     *
     * @method getGraphics
     * @return {AbstractNodeVisuals}
     */
    getGraphics: function() {
        return this._graphics;
    },

    /**
     * Returns the X coordinate of the node on the canvas
     *
     * @method getX
     * @return {Number} the x coordinate
     */
    getX: function() {
        return this.getGraphics().getX();
    },

    /**
     * Changes the X coordinate of the node
     *
     * @method setX
     * @param x {Number} the x coordinate on the canvas
     * @param [animate] {Boolean} set to true if you want to animate the transition
     * @param [callback] {Function} the function called at the end of the animation
     */
    setX: function(x, animate, callback) {
        this.getGraphics().setX(x, animate, callback)
    },

    /**
     * Returns the Y coordinate of the node on the canvas
     *
     * @method getY
     * @return {Number} the y coordinate
     */
    getY: function() {
        return this.getGraphics().getY();
    },

    /**
     * Changes the Y coordinate of the node
     *
     * @method setY
     * @param y {Number} the x coordinate on the canvas
     * @param [animate] {Boolean} set to true if you want to animate the transition
     * @param [callback] {Function} the function called at the end of the animation
     */
    setY: function(y, animate, callback) {
        this.getGraphics().setY(y, animate, callback)
    },

    /**
     * Returns an array containing the x and y coordinates of the node on canvas.
     *
     * @method getPos
     * @return {Array} Array in the form of [x, y]
     */
    getPos: function() {
        return [this.getX(), this.getY()];
    },

    /**
     * Changes the position of the node to (x,y)
     *
     * @method setPos
     * @param x {Number} the x coordinate on the canvas
     * @param y {Number} the y coordinate on the canvas
     * @param [animate] {Boolean} set to true if you want to animate the transition
     * @param [callback] {Function} the function called at the end of the animation
     */
    setPos: function(x,y, animate, callback) {
        this.getGraphics().setPos(x, y, animate, callback);
    },
    
    /**
     * Returns the type of this node
     *
     * @method getType
     * @return {String} the type (eg. "Partnership", "Person", etc)
     */
    getType: function() {
        return this._type;
    },

    /**
     * Returns an array of all adjacent nodes (neighbors).
     *
     * @method getNeighbors
     * @return {Array} in the form of [node1, node2, ...]
     */
    getNeighbors: function() {
        return (this.getLowerNeighbors().concat(this.getSideNeighbors())).concat(this.getUpperNeighbors());
    },

    /**
     * Returns an array of all adjacent nodes (neighbors) located below this node.
     *
     * @method getLowerNeighbors
     * @return {Array} in the form of [node1, node2, ...]
     */
    getLowerNeighbors: function() {
        return [];
    },

    /**
     * Returns an array of all adjacent nodes (neighbors) located on the sides of this node.
     *
     * @method getSideNeighbors
     * @return {Array} in the form of [node1, node2, ...]
     */
    getSideNeighbors: function() {
        return [];
    },

    /**
     * Returns an array of all adjacent nodes (neighbors) located above this node.
     *
     * @method getUpperNeighbors
     * @return {Array} in the form of [node1, node2, ...]
     */
    getUpperNeighbors: function() {
        return [];
    },

    /**
     * Breaks connections with related nodes and removes the node and its visuals.
     *
     * @method remove
     * @param [isRecursive=false] {Boolean} if true, will remove all nodes that will end up disconnected from the
     * Proband
     * @param [skipConfirmation] {Boolean} if true, no confirmation box will pop up
     * @return {Object} in the form
     *
         {
            confirmed: true/false,
            nodes: {
                PersonNodes : [Person1, Person2, ...],
                PartnershipNodes : [Partnership1, Partnership2, ...],
                PregnancyNodes : [Pregnancy1, Pregnancy2, ...],
                PersonGroupNodes : [PersonGroup1, PersonGroup2, ...],
                PlaceHolderNodes : [PlaceHolder1, PlaceHolder2, ...]
            },
            created: [PlaceHolder1, PlaceHolder2, ...]
         }
     */
    remove: function(isRecursive, skipConfirmation) {
        var me = this;
        var toRemove = [];
        var affectedNeighbors = [];
        var nodes = {
            PersonNodes : [],
            PartnershipNodes : [],
            PregnancyNodes : [],
            PersonGroupNodes : [],
            PlaceHolderNodes : []
        };
        if(isRecursive) {
            toRemove.push(me);
            this.getNeighbors().each(function(neighbor) {
                var result = neighbor.isRelatedTo(editor.getGraph().getProband(), toRemove.clone());
                if(!result[0]) {
                    toRemove = result[1];
                }
                else if(neighbor.getType() == "Partnership") {
                    affectedNeighbors.push(neighbor);
                    neighbor.getPregnancies().each(function(preg) {
                        affectedNeighbors.push(preg);
                        if(preg.isPlaceHolderPregnancy()) {
                            affectedNeighbors = affectedNeighbors.concat(preg.getChildren());
                        }
                    });
                }
                else if(neighbor.getType() == "Pregnancy") {
                    affectedNeighbors.push(neighbor);
                    if(neighbor.isPlaceHolderPregnancy()) {
                        affectedNeighbors = affectedNeighbors.concat(neighbor.getChildren());
                    }
                }
            });
            var confirmation = true;
            if(!skipConfirmation) {
                toRemove.each(function(node) {
                    node.getGraphics().getHighlightBox && node.getGraphics().highlight();
                });
                if(toRemove.length > 1) {
                    confirmation = confirm("Removing this person will also remove all the highlighted individuals. Are you sure you want to proceed?");
                }
                else {
                    confirmation = confirm("Removing this person will also remove all the related connections. Are you sure you want to proceed?");
                }
            }
            if(confirmation) {
                toRemove.concat(affectedNeighbors).each(function(node) {
                    nodes[node.getType() + "Nodes"].push(node.getInfo());
                });
                var nodeID = me.getID();
                var placeholders = [];
                toRemove.reverse().each(function(node) {
                    if(node) {
                        if(node.getID() == nodeID) {
                            var ph = node.remove(false);
                            ph && placeholders.push(ph.getInfo());
                        }
                        else
                            node.remove(false);
                    }
                });
            }
            else {
                toRemove.each(function(node) {
                    node && node.getGraphics().getHighlightBox && node.getGraphics().unHighlight();
                });
            }
            return {confirmed: confirmation, affected: nodes, created: placeholders};
        } else {
            editor.getGraph()["remove" + this.getType()](this);
            nodes[this.getType() + "Nodes"].push(this);
            return {confirmed: true, affected: nodes, created: []};
        }
    },

    /**
     * Removes this node and all nodes that will end up disconnected from the
     * Proband as a result. Pushes action onto actionStack.
     *
     * @method removeAction
     */
    removeAction: function() {
        var result = this.remove(true);
        if(result.confirmed) {
            var removed = result.affected,
                placeholders = result.created,
                phPartnerships = [],
                phPregnancies = [];
            placeholders.each(function(ph) {
                var node = editor.getGraph().getNodeMap()[ph.id];
                if(node) {
                    var parentPreg = node.getParentPregnancy();
                    parentPreg && phPregnancies.push(parentPreg.getInfo());
                    node.getPartnerships().each(function(p) {
                        phPartnerships.push(p.getInfo());
                        p.getPregnancies().each(function(preg) {
                            phPregnancies.push(preg.getInfo());
                        });
                    });
                }
            });
            var undoFunct = function() {
                placeholders.each(function(ph) {
                    var node = editor.getGraph().getNodeMap()[ph.id];
                    node && node.remove(false);
                });
                removed.PersonNodes.each(function(p) {
                    editor.getGraph().addPerson(p.x, p.y, p.gender, p.id).loadInfo(p);
                });

                removed.PlaceHolderNodes.each(function(p) {
                    editor.getGraph().addPlaceHolder(p.x, p.y, p.gender, p.id).loadInfo(p);
                });

                removed.PersonGroupNodes.each(function(p) {
                    editor.getGraph().addPersonGroup(p.x, p.y, p.id).loadInfo(p);
                });

                removed.PartnershipNodes.each(function(p) {
                    var p1 = editor.getGraph().getNodeMap()[p.partner1ID];
                    var p2 = editor.getGraph().getNodeMap()[p.partner2ID];
                    if(p1 && p2)
                        editor.getGraph().addPartnership(p.x, p.y, p1, p2, p.id).loadInfo(p);
                });

                removed.PregnancyNodes.each(function(p) {
                    var partnership = editor.getGraph().getNodeMap()[p.partnershipID];
                    if(partnership)
                        editor.getGraph().addPregnancy(p.x, p.y, partnership, p.id).loadInfo(p);
                });
            };

            var redoFunct = function() {
                removed.PregnancyNodes.each(function(p) {
                    var node = editor.getGraph().getNodeMap()[p.id];
                    node && node.remove(false);
                });

                removed.PartnershipNodes.each(function(p) {
                    var node = editor.getGraph().getNodeMap()[p.id];
                    node && node.remove(false);
                });

                removed.PersonGroupNodes.each(function(p) {
                    var node = editor.getGraph().getNodeMap()[p.id];
                    node && node.remove(false);
                });

                removed.PlaceHolderNodes.each(function(p) {
                    var node = editor.getGraph().getNodeMap()[p.id];
                    node && node.remove(false);
                });

                removed.PersonNodes.each(function(p) {
                    var node = editor.getGraph().getNodeMap()[p.id];
                    node && node.remove(false);
                });

                placeholders.each(function(p) {
                    editor.getGraph().addPlaceHolder(p.x, p.y, p.gender, p.id).loadInfo(p);
                });

                phPartnerships.each(function(p) {
                    var p1 = editor.getGraph().getNodeMap()[p.partner1ID];
                    var p2 = editor.getGraph().getNodeMap()[p.partner2ID];
                    if(p1 && p2) {
                        editor.getGraph().addPartnership(p.x, p.y, p1, p2, p.id).loadInfo(p);
                    }
                });

                phPregnancies.each(function(p) {
                    var partnership = editor.getGraph().getNodeMap()[p.partnershipID];
                    if(partnership) {
                        var preg = editor.getGraph().addPregnancy(p.x, p.y, partnership, p.id).loadInfo(p);
                        p.childrenIDs.each(function(id){
                            var child = editor.getGraph().getNodeMap()[id].loadInfo(p);
                            child && preg.addChild(child);
                        });
                    }
                });
            };
            editor.getActionStack().push({undo: undoFunct, redo: redoFunct});
        }
    },

    /**
     * Removes this node and all nodes that will end up disconnected from the
     * Proband as a result. Pushes action onto actionStack.
     *
     * @method isRelatedTo
     * @param node {AbstractNode} any node in the graph
     * @param [visited=[]] {Array} a list of nodes that shouldn't be considered
     * @return {Array} an array in the form of [result, visitedNodes]
     * @example
     var malePerson = editor.getGraph().addPerson(100,100, "M", 20);
     var femalePerson = editor.getGraph().addPerson(300,100, "F", 20);

     malePerson.isRelatedTo(femalePerson) // -> [false, [malePerson]]
     */
    isRelatedTo: function(node, visited) {
        var visitedNodes = (visited) ? visited : [];
        if(visitedNodes.indexOf(this) >= 0) {
            return [false, visitedNodes];
        }
        visitedNodes.push(this);
        if(node == this) {
            return [true, visitedNodes];
        }
        else {
            var result = false;
            var neighbors = this.getNeighbors();
            neighbors = neighbors.without.apply(neighbors, visitedNodes);
            neighbors.each(function(neighbor) {
                var result = neighbor.isRelatedTo(node, visitedNodes);
                visitedNodes = result[1];
                result[0] && (result = true);
            });
            return [result, visitedNodes];
        }
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
     }
     */
    getInfo: function() {
        return {type: this.getType(), x: this.getX(), y : this.getY(), id: this.getID()}
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
     }
     * @return {Boolean} true if info was successfully loaded
     */
    loadInfo: function(info) {
        if(info && info.id == this.getID() && info.type == this.getType()) {
            if(this.getX() != info.x)
                this.setX(info.x, false, null);
            if(this.getY() != info.y)
                this.setY(info.y, false, null.y);
            return true;
        }
        return false;
    },

    /**
     * Applies properties that happen to this node when a widget (such as the menu) is closed.
     *
     * @method onWidgetHide
     */
    onWidgetHide: function() {
        this.getGraphics().getHoverBox() && this.getGraphics().getHoverBox().onWidgetHide();
    }
});

var ChildlessBehavior = {
    /**
     * Returns the childless status of this node
     *
     * @method getChildlessStatus
     * @return {Null|String} null, childless or infertile
     */
    getChildlessStatus: function() {
        return this._childlessStatus;
    },

    /**
     * Returns true if the status is either 'infertile' or 'childless'
     *
     * @method isValidChildlessStatus
     * @return {Boolean}
     */
    isValidChildlessStatus: function(status) {
        return ((status == 'infertile' || status == 'childless'));
    },

    /**
     * Returns the reason for this node's status of 'infertile' or 'childless'
     *
     * @method getChildlessReason
     * @return {String}
     */
    getChildlessReason: function() {
        return this._childlessReason;
    },

    /**
     * Changes the reason for this node's 'childless' or 'infertile' status
     *
     * @method setChildlessReason
     * @param reason {String} a string that explains the condition (eg. "By Choice", "Vasectomy" etc)
     */
    setChildlessReason: function(reason) {
        if(this.getChildlessStatus() != null) {
            this._childlessReason = reason;
            this.getGraphics().updateChildlessStatusLabel();
        }
    },

    /**
     * Changes the reason for this node's 'childless' or 'infertile' status and updates the action stack
     *
     * @method setChildlessReasonAction
     * @param reason {String} a string that explains the condition (eg. "By Choice", "Vasectomy" etc)
     */
    setChildlessReasonAction: function(reason) {
        var prevReason = this.getChildlessReason();
        var nodeID = this.getID();
        if(reason != prevReason && this.getChildlessStatus()) {
            this.setChildlessReason(reason);
            var actionElement = editor.getActionStack().peek();
            if (actionElement.nodeID == nodeID && actionElement.property == 'ChildlessReason') {
                actionElement.newValue = reason;
            } else {
                editor.getActionStack().push({
                    undo: AbstractNode.setPropertyActionUndo,
                    redo: AbstractNode.setPropertyActionRedo,
                    nodeID: nodeID,
                    property: 'ChildlessReason',
                    oldValue: prevReason,
                    newValue: reason
                });
            }
        }
    }
};

AbstractNode.setPropertyActionUndo = function(actionElement) {
    var node = editor.getGraph().getNodeMap()[actionElement.nodeID];
    node && node['set' + actionElement.property](actionElement.oldValue);
};
AbstractNode.setPropertyActionRedo = function(actionElement) {
    var node = editor.getGraph().getNodeMap()[actionElement.nodeID];
    node && node['set' + actionElement.property](actionElement.newValue);
};
AbstractNode.setPropertyToListActionUndo = function(actionElement) {
    Object.keys(actionElement.oldValues).forEach(function(key) {
        var node = editor.getGraph().getNodeMap()[key];
        node && node['set' + actionElement.property](actionElement.oldValues[key]);
    });
};
AbstractNode.setPropertyToListActionRedo = function(actionElement) {
    Object.keys(actionElement.newValues).forEach(function(key) {
        var node = editor.getGraph().getNodeMap()[key];
        node && node['set' + actionElement.property](actionElement.newValues[key]);
    });
};