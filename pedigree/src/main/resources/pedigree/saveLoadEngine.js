/**
 * SaveLoadEngine is responsible for automatic and manual save and load operations.
 *
 * @class SaveLoadEngine
 * @constructor
 */

var SaveLoadEngine = Class.create( {

    initialize: function() {
        var me = this;
        this._timerID = null;
        document.observe("pedigree:actionEvent", function(event) {
            if(typeof me._timerID != "number") {
                me.serialize();
                (function(){
                    me._timerID = me.serialize();
                }).delay(3);
            }
        });
    },

    resetTimerID: function() {
        this._timerID = null;
    },

    serialize: function() {
        alert("serialized!");
        this.resetTimerID();
        var nodes = {
            placeHolders : [],
            partnerships: [],
            pregnancies: [],
            personGroups: [],
            persons: []
        };

        editor.getGraph().getPlaceHolderNodes().forEach(function(placeHolder) {
            nodes.placeHolders.push(placeHolder.getInfo());
        });
        editor.getGraph().getPartnershipNodes().forEach(function(partnership) {
            nodes.partnerships.push(partnership.getInfo());
        });
        editor.getGraph().getPregnancyNodes().forEach(function(pregnancy) {
            nodes.pregnancies.push(pregnancy.getInfo());
        });
        editor.getGraph().getPersonGroupNodes().forEach(function(personGroup) {
            nodes.personGroups.push(personGroup.getInfo());
        });
        editor.getGraph().getPersonNodes().forEach(function(person) {
            nodes.persons.push(person.getInfo());
        });

        return nodes;
    },

    load: function(graphObj) {
        if(this.isValidGraphObject(graphObj)) {
            editor.getGraph().getNodeMap()[1] && editor.getGraph().getNodeMap()[1].remove(true, true);      //clears the graph
            var people = graphObj.persons.concat(graphObj.placeHolders, graphObj.personGroups);
            people.forEach(function(info) {
                var newPerson = editor.getGraph()["add" + info.type](info.x, info.y, info.gender, info.id);
                newPerson.loadInfo(info);
            });
            graphObj.partnerships.forEach(function(info) {
                var partner1 = editor.getGraph().getNodeMap()[info.partner1ID],
                    partner2 = editor.getGraph().getNodeMap()[info.partner2ID];
                if(partner1 && partner2)
                    editor.getGraph().addPartnership(info.x, info.y, partner1, partner2, info.id);
            });
            graphObj.pregnancies.forEach(function(info) {
                var partnership = editor.getGraph().getNodeMap()[info.partnershipID];
                if(partnership) {
                    var preg = editor.getGraph().addPregnancy(info.x, info.y, partnership, info.id);
                    preg.loadInfo(info);
                }
            });
            this.serialize();
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