/* 
 * VersionUpdater is responsible for updating pedigree JSON represenatation to the current version.
 */
VersionUpdater = Class.create( {
    initialize: function() {
        this.availableUpdates = [ { "comment":    "group node comment representation",
                                    "introduced": "May2014",
                                    "func":       "updateGroupNodeComments"} ];
    },

    updateToCurrentVersion: function(pedigreeJSON) {
        for (var i = 0; i < this.availableUpdates.length; i++) {
            var update = this.availableUpdates[i];
            
            var updateResult = this[update.func](pedigreeJSON);
            
            if (updateResult !== null) {
                console.log("[update #" + i + "] [updating to " + update.introduced + " version] - performing " + update.comment + " update");
                pedigreeJSON = updateResult;
            }
        }
        
        return pedigreeJSON;
    },

    /* - assumes input is in the pre-May-2014 format
     * - returns true if there was a change
     */
    updateGroupNodeComments: function(pedigreeJSON) {
        var change = false;
        var data = JSON.parse(pedigreeJSON);
        for (var i = 0; i < data.GG.length; i++) {
            var node = data.GG[i];
            
            if (node.hasOwnProperty("prop")) {
                if (node.prop.hasOwnProperty("numPersons") && !node.prop.hasOwnProperty("comments") && node.prop.hasOwnProperty("fName") && node.prop.hasOwnProperty("fName") != "") {
                    node.prop["comments"] = node.prop.fName;
                    delete node.prop.fName;
                    change = true;
                }
            }
        }
        
        if (!change)
            return null;
        
        return JSON.stringify(data);
    }
});
