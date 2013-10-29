/**
 * SaveLoadEngine is responsible for automatic and manual save and load operations.
 *
 * @class SaveLoadEngine
 * @constructor
 */

function unescapeRestData (dataNode) {
	// http://stackoverflow.com/questions/4480757/how-do-i-unescape-html-entities-in-js-change-lt-to
    var tempNode = document.createElement('div');
    tempNode.innerHTML = dataNode.textContent.replace(/&amp;/, '&');
    return tempNode.innerText || tempNode.text || tempNode.textContent;
}


var ProbandDataLoader = Class.create( {
    initialize: function() {
    	this.probandData = undefined;
    },
    
    load: function(callWhenReady) {       
        new Ajax.Request(XWiki.currentDocument.getRestURL('objects/PhenoTips.PatientClass/0'), {
            method: "GET",
            onSuccess: this.onProbandDataReady.bind(this),
            onComplete: callWhenReady ? callWhenReady : {}
        });
    },
    
    onProbandDataReady : function(response) {    	
    	var data = response.responseXML.documentElement;    	
        this.probandData = {};        
        this.probandData.firstName = unescapeRestData(data.querySelector("property[name='first_name'] > value"));
        this.probandData.lastName  = unescapeRestData(data.querySelector("property[name='last_name'] > value"));
        this.probandData.gender    = unescapeRestData(data.querySelector("property[name='gender'] > value")); 
        if (this.probandData.gender == '')
            this.probandData.gender = 'U';
        console.log("Proband data: " + stringifyObject(this.probandData));
    },    
});


var SaveLoadEngine = Class.create( {

    initialize: function() {
        this._saveInProgress = false;
    },

    /**
     * Saves the state of the graph
     *
     * @return Serialization data for the entire graph
     */
    serialize: function() {
        return editor.getGraph().toJSON();
    },

    createGraphFromSerializedData: function(JSONString, noUndo, centerAround0) {
        console.log("---- load: parsing data ----");
        var successfulLoad = false;

        document.fire("pedigree:load:start");
        
        try {            
            //var JSONString = '{"GG":[{"name":"f11","id":0,"prop":{"gender":"F"},"outedges":[{"to":"p1"}]},{"name":"m11","id":1,"prop":{"gender":"M"},"outedges":[{"to":"p1"}]},{"name":"p1","id":2,"rel":true,"hub":true,"prop":{},"outedges":[{"to":"chhub_p1"}]},{"name":"chhub_p1","id":3,"chhub":true,"prop":null,"outedges":[{"to":"ch1"},{"to":"ch2"},{"to":"ch3"}]},{"name":"f13","id":4,"prop":{"gender":"F"},"outedges":[{"to":"p3"}]},{"name":"m13","id":5,"prop":{"gender":"M"},"outedges":[{"to":"p3"}]},{"name":"p3","id":6,"rel":true,"hub":true,"prop":{},"outedges":[{"to":"chhub_p3"}]},{"name":"chhub_p3","id":7,"chhub":true,"prop":null,"outedges":[{"to":"ch7"},{"to":"ch9"},{"to":"ch8"}]},{"name":"f12","id":8,"prop":{"gender":"F"},"outedges":[{"to":"p2"}]},{"name":"m12","id":9,"prop":{"gender":"M"},"outedges":[{"to":"p2"}]},{"name":"p2","id":10,"rel":true,"hub":true,"prop":{},"outedges":[{"to":"chhub_p2"}]},{"name":"chhub_p2","id":11,"chhub":true,"prop":null,"outedges":[{"to":"ch4"},{"to":"ch5"},{"to":"ch6"}]},{"name":"ch2","id":12,"prop":{"gender":"F"},"outedges":[{"to":"p4"}]},{"name":"ch5","id":13,"prop":{"gender":"M"},"outedges":[{"to":"p4"}]},{"name":"p4","id":14,"rel":true,"hub":true,"prop":{},"outedges":[{"to":"chhub_p4"}]},{"name":"chhub_p4","id":15,"chhub":true,"prop":null,"outedges":[{"to":"leaf1"}]},{"name":"ch4","id":16,"prop":{"gender":"F"},"outedges":[{"to":"p5"}]},{"name":"ch9","id":17,"prop":{"gender":"M"},"outedges":[{"to":"p5"}]},{"name":"p5","id":18,"rel":true,"hub":true,"prop":{},"outedges":[{"to":"chhub_p5"}]},{"name":"chhub_p5","id":19,"chhub":true,"prop":null,"outedges":[{"to":"leaf2"}]},{"name":"ch1","id":20,"prop":{"gender":"M"}},{"name":"ch3","id":21,"prop":{"gender":"M"}},{"name":"ch6","id":22,"prop":{"gender":"M"}},{"name":"ch7","id":23,"prop":{"gender":"M"}},{"name":"ch8","id":24,"prop":{"gender":"M"}},{"name":"leaf1","id":25,"prop":{"gender":"M"}},{"name":"leaf2","id":26,"prop":{"gender":"M"}}],"ranks":[1,1,1,2,1,1,1,2,1,1,1,2,3,3,3,4,3,3,3,4,3,3,3,3,3,5,5],"order":[[],[0,2,1,8,10,9,4,6,5],[3,11,7],[21,20,12,14,13,22,16,18,17,24,23],[15,19],[25,26]],"positions":[13,37,25,25,141,165,153,153,77,101,89,89,45,69,57,57,109,133,121,121,25,5,89,173,153,57,121]}';                        
            //console.log("got json2: " + JSONString);
            
            var positionedGraph = editor.getGraph(); 
            
            var changeSet = positionedGraph.fromJSON(JSONString);
            
            var probandData = editor.getProbandDataFromPhenotips();            
            var genderOk = positionedGraph.setProbandData( probandData.firstName, probandData.lastName, probandData.gender );
            if (!genderOk && !noUndo)
                alert("Gender defined in phenotips is incompatible with this pedigree. Setting proband gender to 'Unknown'");
            
            if (editor.getGraphicsSet().applyChanges(changeSet, false)) {                       
                successfulLoad = true;
                editor.getWorkspace().adjustSizeToScreen();                                
            }        
            
            if (centerAround0)
                editor.getWorkspace().centerAroundNode(0);
            
            if (!noUndo)
                editor.getActionStack().addState(null, null, JSONString);
        }
        catch(err)
        {
        	console.log("ERROR loading the graph: " + err);
        }
        
        document.fire("pedigree:load:finish");
        return successfulLoad;
    },
    
    save: function() {        
        if (this._saveInProgress)          
            return;   // Don't send parallel save requests

        var me = this;

        var jsonData = this.serialize();
        
        console.log("saving, data: " + stringifyObject(jsonData));        
        
        var image = $('canvas');
        var background = image.getElementsByClassName('panning-background')[0];
        var backgroundPosition = background.nextSibling;
        var backgroundParent =  background.parentNode;
        backgroundParent.removeChild(background);
        var bbox = image.down().getBBox();
        var savingNotification = new XWiki.widgets.Notification("Saving", "inprogress");
        new Ajax.Request(XWiki.currentDocument.getRestURL('objects/PhenoTips.PedigreeClass/0', 'method=PUT'), {
            method: 'POST',
            onCreate: function() {
                me._saveInProgress = true;
            },
            onComplete: function() {
                me._saveInProgress = false;
            },
            onSuccess: function() {savingNotification.replace(new XWiki.widgets.Notification("Successfuly saved"));},
            parameters: {"property#data": jsonData, "property#image": image.innerHTML.replace(/viewBox=".*?"/, "viewBox=\"" + bbox.x + " " + bbox.y + " " + bbox.width + " " + bbox.height + " \"width=\"500\" height=\"500\"")}
        });
        backgroundParent.insertBefore(background, backgroundPosition);        
    },    
    
    load: function() {
    	console.log("initiating load process");
                
        new Ajax.Request(XWiki.currentDocument.getRestURL('objects/PhenoTips.PedigreeClass/0/'), {
            method: 'GET',
            onCreate: function() {
                document.fire("pedigree:load:start");
            },
            onSuccess: function (response) {
            	//console.log("Data from LOAD: " + stringifyObject(response));            	
            	console.log("[Data from LOAD]");
            	var rawdata = response.responseXML.documentElement.querySelector("property[name='data'] > value");
                var jsonData = unescapeRestData(rawdata);
                if (jsonData.trim()) {
                	console.log("recived JSON: " + stringifyObject(jsonData));
                    this.createGraphFromSerializedData(jsonData);
                } else {
                    new TemplateSelector(true);
                }
            }.bind(this)
        })
    }
});