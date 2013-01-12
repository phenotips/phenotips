
/*
 * A general abstract class for nodes on the Pedigree graph. Contains information about
 * position on canvas and other neighboring nodes
 *
 * @param x the x coordinate on the canvas
 * @param x the y coordinate on the canvas
 * @param id the id of the node
 */

var AbstractNode = Class.create( {

    initialize: function(x, y, id) {
        !this._type && (this._type = "AbstractNode");
        this._id = id ? id : editor.getGraph().generateID();
        this._graphics = this.generateGraphics(x, y);
    },
    
    /*
     * Returns the unique ID of this node
     */
    getID: function() {
        return this._id;
    },

    /*
     * Generates and returns an instance of AbstractNodeVisuals
     */
    generateGraphics: function(x, y) {
        return new AbstractNodeVisuals(this, x, y);
    },

    /*
     * Returns the object responsible for managing graphics
     */
    getGraphics: function() {
        return this._graphics;
    },

    /*
     * Returns the X coordinate of the node on the canvas
     */
    getX: function() {
        return this.getGraphics().getX();
    },

    /*
     * Changes the X coordinate of the nodek
     *
     * @param x the x coordinate on the canvas
     * @param animate set to true if you want to animate, callback the transition
     * @param callback the function called at the end of the animation
     */
    setX: function(x, animate, callback) {
        this.getGraphics().setX(x, animate, callback)
    },

    /*
     * Returns the Y coordinate of the node on the canvas
     */
    getY: function() {
        return this.getGraphics().getY();
    },

    /*
     * Changes the Y coordinate of the node
     *
     * @param y the y coordinate on the canvas
     * @param animate set to true if you want to animate the transition
     * @param callback the function called at the end of the animation
     */
    setY: function(y, animate, callback) {
        this.getGraphics().setY(y, animate, callback)
    },

    /*
     * Returns an array containing the x and y coordinates of the node on canvas.
     */
    getPos: function() {
        return [this.getX(), this.getY()];
    },

    /*
     * Changes the position of the node to (X,Y)
     *
     * @param x the x coordinate on the canvas
     * @param y the y coordinate on the canvas
     * @param animate set to true if you want to animate the transition
     * @param callback the function called at the end of the animation
     */
    setPos: function(x,y, animate, callback) {
        this.getGraphics().setPos(x, y, animate, callback);
    },
    
    /**
     * Returns the type of this node
     * 
     * @return a string expressing the type
     */
    getType: function() {
        return this._type;
    },

    /*
     * Returns an array of all adjacent nodes (neighbors).
     */
    getNeighbors: function() {
        return (this.getLowerNeighbors().concat(this.getSideNeighbors())).concat(this.getUpperNeighbors());
    },

    /*
     * Returns an array of all adjacent nodes (neighbors) located below this node.
     */
    getLowerNeighbors: function() {
        return [];
    },

    /*
     * Returns an array of all adjacent nodes (neighbors) located on the sides of this node.
     */
    getSideNeighbors: function() {
        return [];
    },

    /*
     * Returns an array of all adjacent nodes (neighbors) located above this node.
     */
    getUpperNeighbors: function() {
        return [];
    },

    /*
     * Removes all connections with neighboring nodes and (optionally) removes
     * all the graphics of this node.
     *
     * @param removeVisuals set to true if you want to remove the graphics of this
     * node as well.
     */
    remove: function(isRecursive) {
        var me = this;
        var toRemove = [];
        var affectedNeighbors = [];
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
            toRemove.each(function(node) {
                node.getGraphics().getHighlightBox && node.getGraphics().highlight();
            });
            var confirmation;
            if(toRemove.length > 1) {
                confirmation = confirm("Removing this person will also remove all the highlighted individuals. Are you sure you want to proceed?");
            }
            else {
                confirmation = confirm("Removing this person will also remove all the related connections. Are you sure you want to proceed?");
            }
            if(confirmation) {
                var nodes = {
                    PersonNodes : [],
                    PartnershipNodes : [],
                    PregnancyNodes : [],
                    PersonGroupNodes : [],
                    PlaceHolderNodes : []
                };
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
                this.removeAction(nodes, placeholders);
            }
            else {
                toRemove.each(function(node) {
                    node && node.getGraphics().getHighlightBox && node.getGraphics().unHighlight();
                });
            }
        }
    },

    removeAction: function(removed, placeholders) {
        var phPartnerships = [],
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
    },

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
            var found = false;
            var neighbors = this.getNeighbors();
            neighbors = neighbors.without.apply(neighbors, visitedNodes);
            neighbors.each(function(neighbor) {
                var result = neighbor.isRelatedTo(node, visitedNodes);
                visitedNodes = result[1];
                result[0] && (found = true);
            });
            return [found, visitedNodes];
        }
    },

    getInfo: function() {
        return {type: this.getType(), x: this.getX(), y : this.getY(), id: this.getID()}
    },

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

    onWidgetHide: function() {
        this.getGraphics().getHoverBox().onWidgetHide();
    }
});

var ChildlessBehavior = {

    /*
     * Returns null if the node has no childless markers.
     * Returns 'childless' if this node is marked childless
     * Returns 'infertile' if this node is marked infertile
     */
    getChildlessStatus: function() {
        return this._childlessStatus;
    },

    /*
     * Changes the childless status of this node
     *
     * @param status set to null, 'infertile' or 'childless'
     */
    setChildlessStatus: function(status) {
        var nonPlaceHolders = this.getChildren("Person","PersonGroup");
        if((status == 'infertile' || status == 'childless') && nonPlaceHolders.length == 0) {
            this._childlessStatus = status;
            var phChildren = this.getChildren("PlaceHolder");
            phChildren.each(function(child) {
                child.remove(false);
            });
        }
        else {
            this._childlessStatus = null;
            this.restorePlaceholders();
        }
        this.setChildlessReason(null);
        this.getGraphics().updateChildlessShapes();
    },

    /*
     * Returns the reason for this node's status of 'infertile' or 'childless'
     */
    getChildlessReason: function() {
        return this._childlessReason;
    },

    /*
     * Changes the reason for this node's 'childless' or 'infertile' status
     *
     * @param reason a string that explains the condition (eg. "By Choice", "Vasectomy" etc)
     */
    setChildlessReason: function(reason) {
        if(this.getChildlessStatus() != null) {
            this._childlessReason = reason;
            this.getGraphics().updateChildlessStatusLabel();
        }
    }
};