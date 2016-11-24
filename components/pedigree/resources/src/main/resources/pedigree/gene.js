/**
 * Gene is a class for storing gene information and loading it from the GeneName service.
 *
 * @param geneID the EnsembleID of a gene, e.g. "ENSG00000169181"
 * @param name gene symbol, e.g. "GSG1L"
 */
define([
        "pedigree/model/helpers"
    ], function(
        Helpers
    ){
    var Gene = Class.create( {

        initialize: function(geneID, symbol, callWhenReady) {

            // genes which are only known by their symbol can be initalized by either id or symbol
            // (depedning on pedigree data source, and given symbols serve as a temporary ID)
            if (geneID == null) {
                geneID = symbol;
            }
            if (symbol == null) {
                symbol = geneID;
            }

            this._geneID = geneID;
            this._symbol = symbol;

            // if we dont't have an ID or symbol we should try to get the missing data, but even if
            // both EnsebleID and gene symbol are known it is a good idea to check for update since
            // gene symbols may (in theory) change over time
            this.load(callWhenReady);

            // debug: for testing slow connections & UI behaviour: setTimeout(this.load.bind(this), 2000);
        },

        /**
         * Returns the ID of the gene (EnsembleID)
         */
        getID: function() {
            return this._geneID;
        },

        /**
         * Returns the associated gene symbol
         */
        getSymbol: function() {
            return this._symbol;
        },

        load: function(callWhenReady) {
            var baseServiceURL = editor.getExternalEndpoint().getGeneNameServiceURL();
            var queryURL       = baseServiceURL + "&q=" + this._geneID;
            new Ajax.Request(queryURL, {
                method: "GET",
                onSuccess: this.onDataReady.bind(this),
                onComplete: callWhenReady ? callWhenReady : {}
            });
        },

        onDataReady : function(response) {
            var oldID = this._geneID;
            var oldSymbol = this._symbol;
            var needUpdate = false;
            try {
                var parsed = JSON.parse(response.responseText);

                if (parsed.hasOwnProperty("docs") && parsed.docs.length > 0) {
                    console.log("LOADED GENE INFO: id = " + parsed.docs[0].id + ", name = " + parsed.docs[0].symbol);

                    // may have to change ID, if old ID was actually a symbol which has an EnsembleID
                    if (parsed.docs[0].id.toUpperCase() == this._geneID.toUpperCase()) {
                        if (this._symbol != parsed.docs[0].symbol) {
                            console.log("LOADED GENE INFO: loaded symbol for ID (new)");
                        }
                        needUpdate = true;
                    } else if (parsed.docs[0].symbol.toUpperCase() == this._geneID.toUpperCase()) {
                        console.log("LOADED GENE INFO: got ID for symbol");
                        needUpdate = true;
                    } else {
                        console.log("LOADED GENE INFO: no exact match");
                    }
                    if (needUpdate) {
                        // update even if it matched - in case of upper/lower case differences
                        this._geneID = parsed.docs[0].id;
                        this._symbol = parsed.docs[0].symbol;
                        if (oldID != this._geneID || oldSymbol != this._symbol) {
                            document.fire('gene:loaded', {'oldid' : oldID, 'newid': this._geneID, 'symbol': this._symbol});
                        }
                    }
                } else {
                    console.log("LOADED GENE INFO: id = " + this._geneID + " -> NO DATA");
                }
            } catch (err) {
                console.log("[LOAD GENE] Parse Error: " +  err);
            }
        }
    });

    return Gene;
});