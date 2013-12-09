/*
 * Disorder is a class for storing genetic disorder info and loading it from the
 * the OMIM database. These disorders can be attributed to an individual in the Pedigree.
 *
 * @param disorderID the id number for the disorder, taken from the OMIM database
 * @param name a string representing the name of the disorder e.g. "Down Syndrome"
 */

var Disorder = Class.create( {

    initialize: function(disorderID, name, callWhenReady) {
        // user-defined disorders
        if (name == null && !isInt(disorderID)) {
            name = disorderID.replace("___", " ");            
        }
        
        this._disorderID = disorderID;                        
        this._name       = name ? name : "loading...";
        
        if (!name)
            this.load(callWhenReady);
    },

    /*
     * Returns the disorderID of the disorder
     */
    getDisorderID: function() {
        return this._disorderID;
    },

    /*
     * Returns the name of the disorder
     */
    getName: function() {
        return this._name;
    },
    
    load: function(callWhenReady) {       
        var URL = "/bin/get/PhenoTips/OmimService?outputSyntax=plain&q=id:" + this._disorderID;      
        console.log("URL: " + URL);
        //var complete = function() { console.log("complete " + this._disorderID); }
        //var complete = function() { try { callWhenReady(); } catch (err) { console.log("ERR: " + err); } };
        new Ajax.Request(URL, {
            method: "GET",
            onSuccess: this.onDataReady.bind(this),
            //onComplete: complete.bind(this)
            onComplete: callWhenReady ? callWhenReady : {}
        });
    },
    
    onDataReady : function(response) {
        try {
            var parsed = JSON.parse(response.responseText);
            //console.log(stringifyObject(parsed));
            console.log("RESPONSE: disorder id = " + this._disorderID + ", name = " + parsed.rows[0].name);
            this._name = parsed.rows[0].name;
        } catch (err) {
            console.log("Error: " +  err);
        }
    }    
});
