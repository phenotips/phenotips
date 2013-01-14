/**
 * SaveLoadEngine is responsible for automatic and manual save and load operations.
 *
 * @class SaveLoadEngine
 * @constructor
 */

var SaveLoadEngine = Class.create( {

    initialize: function() {
        this.probandDataLoaded = false;
        this.graphLoaded = false;
        var me = this;
        this._timerID = null;
        document.observe("pedigree:actionEvent", function(event) {
            if(typeof me._timerID != "number") {
                me._timerID = function() {
                    me._timerID = null;
                    me.serialize();
                }.delay(30);
            }
        });
        new Ajax.Request(XWiki.currentDocument.getRestURL('objects/ClinicalInformationCode.PatientClass/0'), {
            method: "GET",
            onSuccess: this.onProbandDataReady.bind(this)
        });
    },

    resetTimerID: function() {
        this._timerID = null;
    },

    serialize: function() {
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
            var personInfo = person.getInfo();
            if(personInfo.id == 1)
                nodes.proband = personInfo;
            else
                nodes.persons.push(personInfo);
        });

        new Ajax.Request(XWiki.currentDocument.getRestURL('objects/ClinicalInformationCode.PedigreeClass/0', 'method=PUT'), {
            method: 'POST',
            onSuccess: function() {new XWiki.widgets.Notification("Successfuly saved");},
            parameters: {"property#data": JSON.stringify(nodes), "property#image": $('canvas').innerHTML}
        });
        return nodes;
    },

    load: function(graphObj) {
        if(graphObj) {
            if(this.isValidGraphObject(graphObj)) {
                var maxID = editor.getGraph().getIdCount();
                editor.getGraph().getNodeMap()[1] && editor.getGraph().getNodeMap()[1].remove(true, true);      //clears the graph
                var probandX = editor.getWorkspace().getWidth()/2;
                var probandY = editor.getWorkspace().getHeight()/2;
                var xOffset = probandX - graphObj.proband.x;
                var yOffset = probandY - graphObj.proband.y;
                var people = graphObj.persons;
                people.unshift(graphObj.proband);
                people = people.concat(graphObj.placeHolders, graphObj.personGroups);
                people.forEach(function(info) {
                    info.x = info.x + xOffset;
                    info.y = info.y + yOffset;
                    var newPerson = editor.getGraph()["add" + info.type](info.x, info.y, info.gender, info.id);
                    if(info.id >= maxID) {
                        maxID = info.id;
                        editor.getGraph().setIdCount(maxID + 1);
                    }
                    newPerson.loadInfo(info);
                });
                graphObj.partnerships.forEach(function(info) {
                    info.x = info.x + xOffset;
                    info.y = info.y + yOffset;
                    var partner1 = editor.getGraph().getNodeMap()[info.partner1ID],
                        partner2 = editor.getGraph().getNodeMap()[info.partner2ID];
                    if(info.id >= maxID) {
                        maxID = info.id;
                        editor.getGraph().setIdCount(maxID + 1);
                    }
                    if(partner1 && partner2)
                        editor.getGraph().addPartnership(info.x, info.y, partner1, partner2, info.id);
                });
                graphObj.pregnancies.forEach(function(info) {
                    info.x = info.x + xOffset;
                    info.y = info.y + yOffset;
                    var partnership = editor.getGraph().getNodeMap()[info.partnershipID];
                    if(partnership) {
                        var preg = editor.getGraph().addPregnancy(info.x, info.y, partnership, info.id);
                        if(info.id >= maxID) {
                            maxID = info.id;
                            editor.getGraph().setIdCount(maxID + 1);
                        }
                        preg.loadInfo(info);
                    }
                });
                if (this.probandDataLoaded) {
                    this.updateProbandData();
                }
                this.serialize();
            }
        } else {
            new Ajax.Request(XWiki.currentDocument.getRestURL('objects/ClinicalInformationCode.PedigreeClass/0/'), {
                method: 'GET', onSuccess: function (response) {
                    var tempNode = document.createElement('div');
                    tempNode.innerHTML = response.responseXML.documentElement.querySelector("property[name='data'] > value").textContent.replace(/&amp;/, '&');
                    if (tempNode.textContent.trim()) {
                        this.load(JSON.parse(tempNode.textContent));
                    } else {
                        new TemplateSelector(true);
                    }
                }.bind(this)
            })
        }
    },

    isValidGraphObject: function(graphObj) {
        var missingProperty = !graphObj.proband
            || (Object.prototype.toString.call(graphObj.persons) != '[object Array]')
            || (Object.prototype.toString.call(graphObj.personGroups) != '[object Array]')
            || (Object.prototype.toString.call(graphObj.pregnancies) != '[object Array]')
            || (Object.prototype.toString.call(graphObj.partnerships) != '[object Array]')
            || (Object.prototype.toString.call(graphObj.placeHolders) != '[object Array]');
        if(missingProperty)
            return false;
        var validBasics = function(node) {
            return (node.id && node.type && node.x && node.y)
        };
        var people = graphObj.persons.concat(graphObj.proband, graphObj.personGroups, graphObj.placeHolders);
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
    },

    onProbandDataReady : function(response) {
        this.probandData = {};
        var data = response.responseXML.documentElement;
        this.probandData.firstName = this.unescapeRestData(data.querySelector("property[name='first_name'] > value"));
        this.probandData.lastName = this.unescapeRestData(data.querySelector("property[name='last_name'] > value"));
        this.probandDataLoaded = true;
        if (this.graphLoaded) {
            this.updateProbandData();
        }
    },

    updateProbandData : function() {
        editor.getGraph().getProband().setFirstName(this.probandData.firstName);
        editor.getGraph().getProband().setLastName(this.probandData.lastName);
    },

    unescapeRestData: function(dataNode) {
        var tempNode = document.createElement('div');
        tempNode.innerHTML = dataNode.textContent.replace(/&amp;/, '&');
        return tempNode.textContent;
    }
});