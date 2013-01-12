var Graph = Class.create({

    initialize: function() {
        this.hoverModeZones = editor.getPaper().set();
        this._placeHolderNodes = [];
        this._partnershipNodes = [];
        this._pregnancyNodes = [];
        this._personGroupNodes = [];
        this._personNodes = [];
        this.idCount = 1;
        this._nodeMap = {};

        var w = editor.getWorkspace().getWidth();
        var h = editor.getWorkspace().getHeight();
        this._proband = this.addPerson(w/2, h/2, 'M', this.generateID());

        this._currentHoveredNode = null;
        this._currentDraggable = null;
    },

    getNodeMap: function() {
        return this._nodeMap;
    },

    getCurrentHoveredNode: function() {
        return this._currentHoveredNode;
    },

    setCurrentHoveredNode: function(node) {
        this._currentHoveredNode = node;
    },

    getCurrentDraggable: function() {
        return this._currentDraggable;
    },

    setCurrentDraggable: function(draggable) {
        this._currentDraggable = draggable;
    },

    generateID: function() {
        return this.idCount++;
    },

    getProband: function() {
        return this.getNodeMap()[1];
    },

    addPartnership : function(x, y, node1, node2, id) {
        var partnership = new Partnership(x, y, node1, node2, id);
        this.getNodeMap()[partnership.getID()] = partnership;
        editor.getNodeIndex()._addNode(partnership, true);
        this._partnershipNodes.push(partnership);
        return partnership;
    },

    removePartnership: function(partnership) {
        delete this.getNodeMap()[partnership.getID()];
        this._partnershipNodes = this._partnershipNodes.without(partnership);
    },

    getPregnancyNodes: function() {
        return this._pregnancyNodes;
    },

    getPersonNodes: function() {
        return this._personNodes;
    },

    getPlaceHolderNodes: function() {
        return this._placeHolderNodes;
    },

    getPartnershipNodes: function() {
        return this._partnershipNodes;
    },

    getPersonGroupNodes: function() {
        return this._personGroupNodes;
    },

    addPerson: function(x, y, gender, id) {
        var isProband = this.getPersonNodes().length == 0;
        if(!isProband) {
        }
        var node = new Person(x, y, gender, id, isProband);
        this.getPersonNodes().push(node);
        this.getNodeMap()[node.getID()] = node;
        editor.getNodeIndex()._addNode(node, true);
        return node;
    },

    removePerson: function(person) {
        delete this.getNodeMap()[person.getID()];
        this._personNodes = this._personNodes.without(person);
    },

    addPlaceHolder: function(x, y, gender, id) {
        var node = new PlaceHolder(x, y, gender, id);
        this.getPlaceHolderNodes().push(node);
        this.getNodeMap()[node.getID()] = node;
        editor.getNodeIndex()._addNode(node, true);
        return node;
    },

    removePlaceHolder: function(placeholder) {
        delete this.getNodeMap()[placeholder.getID()];
        this._placeHolderNodes = this._placeHolderNodes.without(placeholder);
    },

    addPersonGroup: function(x, y, gender, id) {
        var node = new PersonGroup(x, y, gender, id);
        this.getPersonGroupNodes().push(node);
        this.getNodeMap()[node.getID()] = node;
        editor.getNodeIndex()._addNode(node, true);
        return node;
    },

    removePersonGroup: function(groupNode) {
        delete this.getNodeMap()[groupNode.getID()];
        this._personGroupNodes = this._personGroupNodes.without(groupNode);
    },

    addPregnancy: function(x, y, partnership, id) {
        var node = new Pregnancy(x, y, partnership, id);
        this.getPregnancyNodes().push(node);
        this.getNodeMap()[node.getID()] = node;
        editor.getNodeIndex()._addNode(node, true);
        return node;
    },

    removePregnancy: function(pregnancy) {
        delete this.getNodeMap()[pregnancy.getID()];
        this._pregnancyNodes = this._pregnancyNodes.without(pregnancy);
    },

    // HOVER MODE
    enterHoverMode: function(sourceNode, hoverTypes) {
        if(this.getCurrentDraggable().getType() == "parent") {
            this.getPartnershipNodes().each(function(partnershipBubble) {
                partnershipBubble.getGraphics().grow();
            })
        }
        var me = this,
            color,
            hoverNodes = [];
        hoverTypes.each(function(type) {
            hoverNodes = hoverNodes.concat(me["get" + type + "Nodes"]())
        });
        hoverNodes.without(sourceNode).each(function(node) {
            var hoverModeZone = node.getGraphics().getHoverBox().getHoverZoneMask().clone().toFront();
            hoverModeZone.attr("cursor", "pointer");
            hoverModeZone.hover(
                function() {
                    me._currentHoveredNode = node;
                    node.getGraphics().getHoverBox().setHovered(true);
                    node.getGraphics().getHoverBox().getBoxOnHover().attr(editor.attributes.boxOnHover);

                    if(me.getCurrentDraggable().getType() == 'PlaceHolder' && me.getCurrentDraggable().canMergeWith(node)) {
                        me.getCurrentDraggable().validHoveredNode = node;
                        color = "green";
                    }
                    else if(me.getCurrentDraggable().getType() == "partner" && sourceNode.canPartnerWith(node)) {
                        node.validPartnerSelected = true;
                        color = "green";
                    }
                    else if(me.getCurrentDraggable().getType() == "child" && sourceNode.canBeParentOf(node)) {
                        node.validChildSelected = true;
                        color = "green";
                    }
                    else if(me.getCurrentDraggable().getType() == "parent" && node.canBeParentOf(sourceNode)) {
                        if(node.getType() == 'Person') {
                            node.validParentSelected = true;
                        }
                        else {
                            node.validParentsSelected = true;
                        }
                        color = "green";
                    }
                    else {
                        color = "red";
                    }
                    node.getGraphics().getHoverBox().getBoxOnHover().attr("fill", color);
                },
                function() {
                    me.getCurrentDraggable() && (me.getCurrentDraggable().validHoveredNode = null);
                    node.getGraphics().getHoverBox().setHovered(false);
                    node.getGraphics().getHoverBox().getBoxOnHover().attr(editor.attributes.boxOnHover).attr('opacity', 0);
                    me._currentHoveredNode = null;
                    node.validPartnerSelected = node.validChildSelected =  node.validParentSelected = node.validParentsSelected = false;
                });
            me.hoverModeZones.push(hoverModeZone);
        });
    },

    exitHoverMode: function() {
        this.hoverModeZones.remove();
        this.getPartnershipNodes().each(function(partnership) {
            partnership.getGraphics().area && partnership.getGraphics().area.remove();
        });
    },

    serialize: function() {
        var nodes = {
            placeHolders : [],
            partnerships: [],
            pregnancies: [],
            personGroups: [],
            persons: []
        };

        this.getPlaceHolderNodes().forEach(function(placeHolder) {
            nodes.placeHolders.push(placeHolder.getInfo());
        });
        this.getPartnershipNodes().forEach(function(partnership) {
            nodes.partnerships.push(partnership.getInfo());
        });
        this.getPregnancyNodes().forEach(function(pregnancy) {
            nodes.pregnancies.push(pregnancy.getInfo());
        });
        this.getPersonGroupNodes().forEach(function(personGroup) {
            nodes.personGroups.push(personGroup.getInfo());
        });
        this.getPersonNodes().forEach(function(person) {
            nodes.persons.push(person.getInfo());
        });

        return nodes;
    },

    load: function(graphObj) {
        if(!graphObj) {
            new Ajax.Request('/some_url', {
                method:'get',
                requestHeaders: {Accept: 'application/json'},
                onSuccess: function(transport){
                    var json = transport.responseText.evalJSON(true);
                }
            });
        }
        var me = this;
        if(this.isValidGraphObject(graphObj)) {
            this.getNodeMap()[1] && this.getNodeMap()[1].remove(true, true);      //clears the graph
            var people = graphObj.persons.concat(graphObj.placeHolders, graphObj.personGroups);
            people.forEach(function(info) {
                var newPerson = me["add" + info.type](info.x, info.y, info.gender, info.id);
                newPerson.loadInfo(info);
            });
            graphObj.partnerships.forEach(function(info) {
                var partner1 = me.getNodeMap()[info.partner1ID],
                    partner2 = me.getNodeMap()[info.partner2ID];
                if(partner1 && partner2)
                    me.addPartnership(info.x, info.y, partner1, partner2, info.id);
            });
            graphObj.pregnancies.forEach(function(info) {
                var partnership = me.getNodeMap()[info.partnershipID];
                if(partnership) {
                    var preg = me.addPregnancy(info.x, info.y, partnership, info.id);
                    preg.loadInfo(info);
                }
            });
        }
    },

    isValidGraphObject: function(graphObj) {
        var foundProband = false;
        var missingProperty = (Object.prototype.toString.call(graphObj.persons) != '[object Array]')
                                    || (Object.prototype.toString.call(graphObj.personGroups) != '[object Array]')
                                    || (Object.prototype.toString.call(graphObj.pregnancies) != '[object Array]')
                                    || (Object.prototype.toString.call(graphObj.partnerships) != '[object Array]')
                                    || (Object.prototype.toString.call(graphObj.placeHolders) != '[object Array]');
        if(missingProperty)
            return false;
        var validBasics = function(node) {
            return (node.id && node.type && node.x && node.y)
        };
        var people = graphObj.persons.concat(graphObj.personGroups, graphObj.placeHolders);
        for (var i = 0; i < people.length; i++) {
            if(!validBasics(people[i]) || !people[i].gender)
                return false;
            if(people[i].id == 1) {
                foundProband = true;
            }
        }
        if(!foundProband)
            return false;
        for (i = 0; i < graphObj.partnerships.length; i++) {
            if(!validBasics(graphObj.partnerships[i]) || !graphObj.partnerships[i].partner1ID
                                                      || !graphObj.partnerships[i].partner2ID) {
                return false;
            }
        }
        for (i = 0; i < graphObj.pregnancies.length; i++) {
            if(!validBasics(graphObj.pregnancies[i]) || !graphObj.pregnancies[i].partnershipID ||
                    (Object.prototype.toString.call(graphObj.pregnancies[i].childrenIDs) != '[object Array]')) {
                return false;
            }
        }
        return true;
    }
});