/*
 * PlaceHolder objects act as reminders for the user that information about relatives is missing. A PlaceHolder
 * shares common traits with a Person, such as id, gender and position but it will not be displayed in the
 * final exported version of the graph. Placeholders use a dotted outline to distinguish themselves from
 * Person nodes.
 *
 * @param x the x coordinate on the Raphael canvas at which the node drawing will be centered
 * @param the y coordinate on the Raphael canvas at which the node drawing will be centered
 * @param gender either 'M', 'F' or 'U' depending on the gender
 * @param id a unique ID number
 */

var PlaceHolder = Class.create(AbstractPerson, {

    initialize: function($super, x, y, gender, id) {
        $super(x, y, gender, id);
        this._type = "PlaceHolder"
    },

    /*
     * Initializes the object responsible for creating graphics for this PlaceHolder
     *
     * @param x the x coordinate on the canvas at which the node is centered
     * @param y the y coordinate on the canvas at which the node is centered
     */
    generateGraphics: function(x, y) {
        return new PlaceHolderVisuals(this, x, y);
    },

    /*
     * Creates a new Person in the place of this PlaceHolder and merges the PlaceHolder with the person
     */
    convertTo: function(type, gender) {
        var replacement = editor.getGraph()["add" + type](this.getX(), this.getY(), gender);
        this.merge(replacement);
        //document.fire('pedigree:node:upgraded', {'node' : replacement, 'relatedNodes' : [], 'sourceNode' : this});
    },

    /*
     * Attributes all of the connections of this placeholder to person and removes the placeholder.
     *
     * @param person a Person node
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

    /*
     * Returns true if this placeholder is not a partner, descendant or ancestor of person, and if person has
     * no parent conflict with the placeholder
     *
     * @param person a Person node
     */
    canMergeWith: function(person) {
        return (person.getGender() == this.getGender() || this.getGender() == 'U' || person.getGender() == "U" ) &&
            !person.isPartnerOf(this) &&
            !this.hasConflictingParents(person) &&
            !person.isDescendantOf(this) &&
            !person.isAncestorOf(this);
    },

    /*
     * Returns true if person has a different set of parents than this placeholder
     */
    hasConflictingParents: function(person) {
        return (this.getParentPartnership() != null && person.getParentPartnership() != null && this.getParentPartnership() != person.getParentPartnership());
//        var hasConflictingDads = this._father && node._father && this._father != node._father;
//        var hasConflictingMoms = this._mother && node._mother && this._mother != node._mother;
//        var notReversedParents = (this._mother && this._mother._gender != 'U') ||
//            (this._father && this._father._gender != 'U') ||
//            (node._mother && node._mother._gender != 'U') ||
//            (node._father && node._father._gender != 'U') ||
//            (this._mother && node._father && this._mother != node._father) ||
//            (this._father && node._mother && this._father != node._mother);
//
//        return notReversedParents && (hasConflictingDads || hasConflictingMoms);
    },

    remove: function($super) {
        editor.getGraph().removePlaceHolder(this);
        return $super()
    },

    /*
     * Generates an actionStack entry for merging with a person and calls merge
     * @param person the Person this placeholder is merged with
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
                        editor.getGraph().addPartnership(partnership.x, partnership.y, ph, partner);
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
                            editor.getGraph().addPregnancy(pregnancy.x, pregnancy.y, partnership, pregnancy.id)
                        }
                    })
                }
            };
            editor.getActionStack().push({undo: undoFunct, redo: redoFunct});
        }
    }
});