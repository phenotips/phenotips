/**
 * AbstractNode is the general abstract class for nodes on the Pedigree graph. An AbstractNode contains information
 * about its position on the canvas and about relationships with other nodes on the graph.
 *
 * @class AbstractNode
 * @constructor
 * @param {Number} x The x coordinate on the canvas
 * @param {Number} y The y coordinate on the canvas
 * @param {Number} [id] The id of the node
 */

var AbstractNode = Class.create( {

    initialize: function(x, y, id) {
    	//console.log("abstract node");
    	this._id = id;
        !this._type && (this._type = "AbstractNode");        
        this._graphics = this._generateGraphics(x, y);
        //console.log("abstract node end");
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
     * @method _generateGraphics
     * @param {Number} x The x coordinate of the node
     * @param {Number} y The y coordinate of the node
     * @return {AbstractNodeVisuals}
     * @private
     */
    _generateGraphics: function(x, y) {
    	console.log("abstract node - generate graphics");
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
     * Returns the Y coordinate of the node on the canvas
     *
     * @method getY
     * @return {Number} the y coordinate
     */
    getY: function() {
        return this.getGraphics().getY();
    },
  
    /**
     * Changes the position of the node to (x,y)
     *
     * @method setPos
     * @param {Number} x The x coordinate on the canvas
     * @param {Number} y The y coordinate on the canvas
     * @param {Boolean} [animate] Set to true if you want to animate the transition
     * @param {Function} [callback] The function called at the end of the animation
     */
    setPos: function(x,y, animate, callback) {
        this.getGraphics().setPos(x, y, animate, callback);
    },
    
    /**
     * Returns the type of this node
     *
     * @method getType
     * @return {String} The type (eg. "Partnership", "Person", etc)
     */
    getType: function() {
        return this._type;
    },

    /**
     * Removes the node and its visuals.
     *
     * @method remove
     * @param [skipConfirmation=false] {Boolean} if true, no confirmation box will pop up
     */
    remove: function(skipConfirmation) {
    	
    	// get the list of affected nodes
    	//editor.getGraph().getDisconnectedSetIfNodeRemoved(this.getID());
    	
    	this.getGraphics().remove();    	    
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
        	/*
            var removed = result.affected,
                placeholders = result.created,
                phPartnerships = [],
                phPregnancies = [];
            placeholders.each(function(ph) {
                var node = editor.getGraphicsSet().getNodeMap()[ph.id];
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
                    var node = editor.getGraphicsSet().getNode(ph.id);
                    node && node.remove(false);
                });
                removed.PersonNodes.each(function(p) {
                    editor.getGraphicsSet().addPerson(p.x, p.y, p.gender, p.id).loadInfo(p);
                });

                removed.PlaceHolderNodes.each(function(p) {
                    editor.getGraphicsSet().addPlaceHolder(p.x, p.y, p.gender, p.id).loadInfo(p);
                });

                removed.PersonGroupNodes.each(function(p) {
                    editor.getGraphicsSet().addPersonGroup(p.x, p.y, p.id).loadInfo(p);
                });

                removed.PartnershipNodes.each(function(p) {
                    var p1 = editor.getGraphicsSet().getNode(p.partner1ID);
                    var p2 = editor.getGraphicsSet().getNode(p.partner2ID);
                    if(p1 && p2)
                        editor.getGraphicsSet().addPartnership(p.x, p.y, p1, p2, p.id).loadInfo(p);
                });

                removed.PregnancyNodes.each(function(p) {
                    var partnership = editor.getGraphicsSet().getNode(p.partnershipID);
                    if(partnership)
                        editor.getGraphicsSet().addPregnancy(p.x, p.y, partnership, p.id).loadInfo(p);
                });
            };

            var redoFunct = function() {
                removed.PregnancyNodes.each(function(p) {
                    var node = editor.getGraphicsSet().getNode(p.id);
                    node && node.remove(false);
                });

                removed.PartnershipNodes.each(function(p) {
                    var node = editor.getGraphicsSet().getNode(p.id);
                    node && node.remove(false);
                });

                removed.PersonGroupNodes.each(function(p) {
                    var node = editor.getGraphicsSet().getNode(p.id);
                    node && node.remove(false);
                });

                removed.PlaceHolderNodes.each(function(p) {
                    var node = editor.getGraphicsSet().getNode(p.id);
                    node && node.remove(false);
                });

                removed.PersonNodes.each(function(p) {
                    var node = editor.getGraphicsSet().getNode(p.id);
                    node && node.remove(false);
                });

                placeholders.each(function(p) {
                    editor.getGraphicsSet().addPlaceHolder(p.x, p.y, p.gender, p.id).loadInfo(p);
                });

                phPartnerships.each(function(p) {
                    var p1 = editor.getGraphicsSet().getNode(p.partner1ID);
                    var p2 = editor.getGraphicsSet().getNode(p.partner2ID);
                    if(p1 && p2) {
                        editor.getGraphicsSet().addPartnership(p.x, p.y, p1, p2, p.id).loadInfo(p);
                    }
                });

                phPregnancies.each(function(p) {
                    var partnership = editor.getGraphicsSet().getNode(p.partnershipID);
                    if(partnership) {
                        var preg = editor.getGraphicsSet().addPregnancy(p.x, p.y, partnership, p.id).loadInfo(p);
                        p.childrenIDs.each(function(id){
                            var child = editor.getGraphicsSet().getNode(id).loadInfo(p);
                            child && preg.addChild(child);
                        });
                    }
                });
            };
            editor.getActionStack().push({undo: undoFunct, redo: redoFunct});
            */
        }
    },

    /**
     * Returns an object containing all the properties of this node
     * except id, x, y & type
     *
     * @method getProperties
     * @return {Object} in the form
     *
     */
    getProperties: function() {
        return {};
    },

    /**
     * Applies the properties found in info to this node.
     *
     * @method assignProperties
     * @param properties Object
     * @return {Boolean} True if info was successfully assigned
     */
    assignProperties: function(properties) {
        return true;
    },

    /**
     * Applies properties that happen to this node when a widget (such as the menu) is closed.
     *
     * @method onWidgetHide
     */
    onWidgetHide: function() {
        this.getGraphics().getHoverBox() && this.getGraphics().getHoverBox().onWidgetHide();
    },
    
    onWidgetShow: function() {
        this.getGraphics().getHoverBox() && this.getGraphics().getHoverBox().onWidgetShow();
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
     * @param {String} reason Explanation for the condition (eg. "By Choice", "Vasectomy" etc)
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
     * @param {String} reason Explanation for the condition (eg. "By Choice", "Vasectomy" etc)
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
    var node = editor.getGraphicsSet().getNode(actionElement.nodeID);
    node && node['set' + actionElement.property](actionElement.oldValue);
};
AbstractNode.setPropertyActionRedo = function(actionElement) {
    var node = editor.getGraphicsSet().getNode(actionElement.nodeID);
    node && node['set' + actionElement.property](actionElement.newValue);
};
AbstractNode.setPropertyToListActionUndo = function(actionElement) {
    Object.keys(actionElement.oldValues).forEach(function(key) {
        var node = editor.getGraphicsSet().getNode(key);
        node && node['set' + actionElement.property](actionElement.oldValues[key]);
    });
};
AbstractNode.setPropertyToListActionRedo = function(actionElement) {
    Object.keys(actionElement.newValues).forEach(function(key) {
        var node = editor.getGraphicsSet().getNode(key);
        node && node['set' + actionElement.property](actionElement.newValues[key]);
    });
};