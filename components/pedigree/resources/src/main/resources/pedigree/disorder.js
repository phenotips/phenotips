/*
 * Disorder is a class for storing genetic disorder info and loading it from the
 * the OMIM database. These disorders can be attributed to an individual in the Pedigree.
 *
 * @param disorderID the id number for the disorder, taken from the OMIM database
 * @param name a string representing the name of the disorder e.g. "Down Syndrome"
 */
define([
        "pedigree/model/helpers"
    ], function(
        Helpers
    ){
    var Disorder = Class.create( {

        initialize: function(disorderID, name, callWhenReady) {
            // user-defined disorders
            if (name == null && !Helpers.isInt(disorderID)) {
                name = disorderID;
            }

            this._disorderID = disorderID;
            this._name       = name ? name : "loading...";

            if (!name && callWhenReady)
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
            var baseOMIMServiceURL = editor.getExternalEndpoint().getOMIMServiceURL();
            var queryURL           = baseOMIMServiceURL + "&q=id:" + this._disorderID;
            //console.log("queryURL: " + queryURL);
            new Ajax.Request(queryURL, {
                method: "GET",
                onSuccess: this.onDataReady.bind(this),
                //onComplete: complete.bind(this)
                onComplete: callWhenReady ? callWhenReady : {}
            });
        },

        onDataReady : function(response) {
            this._name = this._disorderID;
            try {
                var parsed = JSON.parse(response.responseText);
                //console.log(Helpers.stringifyObject(parsed));
                if (parsed.hasOwnProperty("rows") && parsed.rows.length > 0) {
                    console.log("LOADED DISORDER: disorder id = " + this._disorderID + ", name = " + parsed.rows[0].name);
                    this._name = parsed.rows[0].name;
                } else {
                    console.log("LOADED DISORDER: id = " + this._disorderID + " -> NO DATA");
                }
            } catch (err) {
                console.log("[LOAD DISORDER] Parse error: " +  err);
            }
        }
    });

    return Disorder;
});