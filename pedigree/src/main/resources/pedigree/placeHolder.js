/**
 * PlaceHolder objects act as reminders for the user that information about relatives is missing. A PlaceHolder
 * shares common traits with a Person, such as id, gender and position but it will not be displayed in the
 * final exported version of the graph. Placeholders use a dotted outline to distinguish themselves from
 * Person nodes.
 *
 * @class PlaceHolder
 * @extends AbstractPerson
 * @constructor
 * @param {Number} x X coordinate on the Raphael canvas at which the node drawing will be centered
 * @param {Number} y Y coordinate on the Raphael canvas at which the node drawing will be centered
 * @param {String} gender 'M', 'F' or 'U'
 * @param {Number} id Unique ID number
 */

var PlaceHolder = Class.create(AbstractPerson, {

    initialize: function($super, x, y, gender, id) {
        this._type = "PlaceHolder";
        $super(x, y, gender, id);
    },

    /**
     * Initializes the object responsible for creating graphics for this PlaceHolder
     *
     * @method _generateGraphics
     * @param {Number} x X coordinate on the Raphael canvas at which the node drawing will be centered
     * @param {Number} y Y coordinate on the Raphael canvas at which the node drawing will be centered
     * @return {PlaceHolderVisuals}
     * @private
     */
    _generateGraphics: function(x, y) {
        return new PlaceHolderVisuals(this, x, y);
    },

    /**
     * Replaces this PlaceHolder with new node of given type and gender.
     * Creates an action stack entry
     *
     * @method createNodeAction
     * @param {String} type "Person" or "PersonGroup"
     * @param {String} gender "M", "F", or "U"
     */
    createNodeAction: function(type, gender) {
        var replacement = editor.getGraph()["add" + type](this.getX(), this.getY(), gender);
        if(replacement) {
            var nodeInfo = replacement.getInfo();
            this.mergeAction(replacement, this.getX(), this.getY());
            var oldUndo = editor.getActionStack().peek().undo;
            var oldRedo = editor.getActionStack().peek().redo;
            editor.getActionStack().getStack()[editor.getActionStack().size() - 1] =
            {
                undo: function() {
                    oldUndo();
                    var node = editor.getGraph().getNodeMap()[nodeInfo.id];
                    node && node.remove(false);
                },

                redo: function() {
                    var node = editor.getGraph()["add" + type](nodeInfo.x, nodeInfo.y, gender, nodeInfo.id);
                    oldRedo();
                }
            }

        }
    },

    /**
     * Replaces this PlaceHolder with new node of given type and gender.
     * Creates an action stack entry
     *
     * @method createNodeAction
     * @param {String} type "Person" or "PersonGroup"
     * @param {String} gender "M", "F", or "U"
     */
    convertTo: function(type, gender) {
        var replacement = editor.getGraph()["add" + type](this.getX(), this.getY(), gender);
        this.merge(replacement);
        return replacement;
        //document.fire('pedigree:node:upgraded', {'node' : replacement, 'relatedNodes' : [], 'sourceNode' : this});
    },

    /**
     * Attributes all of the connections of this placeholder to person and removes the placeholder.
     *
     * @method merge
     * @param {Person} person
     * @return {Person} The person with which the node was merged or null if merge was unsuccessful
     */
    merge: function(person) {
        if(this.canMergeWith(person)) {
            var parents = this.getParentPartnership();
            if(parents) {
                (parents.getChildren().indexOf(person) == -1) && this.getParentPregnancy().addChild(person);
                this.getParentPregnancy().removeChild(this);
            }
            var partnerships = this.getPartnerships();
            var me = this;

            partnerships.each(function(partnership){
                var partner = partnership.getPartnerOf(me);
                var newPartnership;
                if(person.getPartners().indexOf(partner) == -1) {
                    newPartnership = person.addPartner(partnership.getPartnerOf(me), true);
                }
                else {
                    newPartnership = person.getPartnership(partner)
                }
                partnership.getPregnancies().each(function(pregnancy) {
                    var newPreg = newPartnership.createPregnancy();
                    pregnancy.getChildren().each(function(child) {
                        pregnancy.removeChild(child);
                        newPreg.addChild(child);
                    });
                });
                partnership.remove(false);
            });
            me && me.remove(false);
            return person;
        }
        return null;
    },

    /**
     * Returns true if this placeholder is not a partner, descendant or ancestor of person, and if person has
     * no parent conflict with the placeholder
     *
     * @method canMergeWith
     * @param {Person} person
     * @return {Boolean}
     */
    canMergeWith: function(person) {
        return (person.getGender() == this.getGender() || this.getGender() == 'U' || person.getGender() == "U" ) &&
            !person.isPartnerOf(this) &&
            !this.hasConflictingParents(person) &&
            !person.isDescendantOf(this) &&
            !person.isAncestorOf(this);
    },

    /**
     * Returns true if person has a different set of parents than this placeholder
     *
     * @method hasConflictingParents
     * @return {Boolean}
     */
    hasConflictingParents: function(person) {
        return (this.getParentPartnership() != null
            && person.getParentPartnership() != null
            && this.getParentPartnership() != person.getParentPartnership());
    },

    /**
     * Attributes all of the connections of this placeholder to person and removes the placeholder.
     * Creates entry in the action stack.
     *
     * @method merge
     * @param {Person} person
     * @param {Number} originalX The original x coordinate of the PlaceHolder
     * @param {Number} originalY The original y coordinate of the PlaceHolder
     */
    mergeAction: function(person, originalX, originalY) {
        var me = this,
            source = this.getInfo(),
            target = person.getInfo(),
            parentPreg = this.getParentPregnancy() ? this.getParentPregnancy().getInfo() : null;
        var phPartnerships = [];
        var phPregnancies = [];
        me.getPartnerships().each(function(partnership) {
            phPartnerships.push({
                id: partnership.getID(),
                x: partnership.getX(),
                y: partnership.getY(),
                partnerID: partnership.getPartnerOf(me).getID()
            });
            partnership.getPregnancies().each(function(pregnancy) {
                var info = {
                    id: pregnancy.getID(),
                    x: pregnancy.getX(),
                    y: pregnancy.getY(),
                    partnershipID: pregnancy.getPartnership().getID(),
                    childrenIDs: []
                };
                pregnancy.getChildren().each(function(child) {
                    info.childrenIDs.push(child.getID());
                });
                phPregnancies.push(info);
            });
        });

        var oldPartnerships = [];
        var oldPregnancies = [];

        person.getPartnerships().each(function(partnership) {
            oldPartnerships.push(partnership.getID());
            partnership.getPregnancies().each(function(pregnancy) {
                oldPregnancies.push(pregnancy.getID())
            });
        });

        if(this.merge(person)) {
            var newParentPreg = person.getParentPregnancy();
            if(newParentPreg) {
                newParentPreg = newParentPreg.getInfo();
            }
            var newPartnerships = [];
            var newPregnancies = [];
            person.getPartnerships().each(function(partnership) {
                if(oldPartnerships.indexOf(partnership.getID()) == -1) {
                    newPartnerships.push({
                        id: partnership.getID(),
                        x: partnership.getX(),
                        y: partnership.getY(),
                        partnerID: partnership.getPartnerOf(person).getID()
                    });
                }
                partnership.getPregnancies().each(function(pregnancy) {
                    if(oldPregnancies.indexOf(pregnancy.getID()) == -1) {
                        var info = {
                            id: pregnancy.getID(),
                            x: pregnancy.getX(),
                            y: pregnancy.getY(),
                            partnershipID: pregnancy.getPartnership().getID(),
                            childrenIDs: []
                        };
                        pregnancy.getChildren().each(function(child) {
                            info.childrenIDs.push(child.getID());
                        });
                        newPregnancies.push(info);
                    }
                });
            });

            var undoFunct = function() {
                var ph = editor.getGraph().addPlaceHolder(source.x, source.y, source.gender, source.id);
                if(parentPreg && newParentPreg) {
                    var pPregnancy = editor.getGraph().getNodeMap()[newParentPreg.id];
                    var pPartnership;
                    if(pPregnancy) {
                        pPartnership = pPregnancy.getPartnership();
                        pPregnancy.remove(false);
                    }
                    if(pPartnership) {
                        var ppreg = editor.getGraph().addPregnancy(parentPreg.x, parentPreg.y, pPartnership,
                            parentPreg.id);
                        ppreg.addChild(ph);
                    }
                }
                newPregnancies.each(function(pregnancy) {
                    var preg = editor.getGraph().getNodeMap()[pregnancy.id];
                    preg && preg.remove(false);
                });
                newPartnerships.each(function(partnership) {
                    var part = editor.getGraph().getNodeMap()[partnership.id];
                    part && part.remove(false);
                });

                phPartnerships.each(function(partnership) {
                    var partner = editor.getGraph().getNodeMap()[partnership.partnerID];
                    if(partner) {
                        editor.getGraph().addPartnership(partnership.x, partnership.y, ph, partner,partnership.id);
                    }
                });

                phPregnancies.each(function(pregnancy) {
                    var partnership = editor.getGraph().getNodeMap()[pregnancy.partnershipID];
                    if(partnership) {
                        var preg  = editor.getGraph().addPregnancy(pregnancy.x, pregnancy.y, partnership, pregnancy.id);
                        pregnancy.childrenIDs.each(function(child) {
                            var c = editor.getGraph().getNodeMap()[child];
                            c && preg.addChild(c);
                        });
                    }
                });

                ph.setPos(originalX,originalY, true);
            };

            var redoFunct = function() {
                var targetPerson = editor.getGraph().getNodeMap()[target.id];
                if(parentPreg && newParentPreg) {
                    var pPregnancy = editor.getGraph().getNodeMap()[parentPreg.id];
                    var pPartnership;
                    if(pPregnancy) {
                        pPartnership = pPregnancy.getPartnership();
                        pPregnancy.remove(false);
                    }
                    if(pPartnership) {
                        var ppreg = editor.getGraph().addPregnancy(newParentPreg.x, newParentPreg.y, pPartnership,
                                                                                                    newParentPreg.id);
                        ppreg.addChild(targetPerson);
                    }
                }
                if(targetPerson) {
                    phPregnancies.each(function(pregnancy) {
                        var preg = editor.getGraph().getNodeMap()[pregnancy.id];
                        preg && preg.remove(false);
                    });
                    phPartnerships.each(function(partnership) {
                        var part = editor.getGraph().getNodeMap()[partnership.id];
                        part && part.remove(false);
                    });
                    newPartnerships.each(function(partnership) {
                        var partner = editor.getGraph().getNodeMap()[partnership.partnerID];
                        if(partner) {
                            editor.getGraph().addPartnership(partnership.x, partnership.y, partner,
                                                                                targetPerson, partnership.id)
                        }
                    });

                    newPregnancies.each(function(pregnancy) {
                        var partnership = editor.getGraph().getNodeMap()[pregnancy.partnershipID];
                        if(partnership) {
                            var preg = editor.getGraph().addPregnancy(pregnancy.x, pregnancy.y, partnership, pregnancy.id)
                            pregnancy.childrenIDs.each(function(childID) {
                                var child = editor.getGraph().getNodeMap()[childID];
                                child && preg.addChild(child);
                            })
                        }
                    })
                }
            };
            editor.getActionStack().push({undo: undoFunct, redo: redoFunct});
        }
    }
});