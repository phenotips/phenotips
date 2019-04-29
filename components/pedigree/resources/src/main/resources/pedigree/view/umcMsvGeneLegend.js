/**
 * Specific version of GeneLegend for known disease genes missing a second variant. It is not displayed but used
 * because of the way gene update machinery works (to update gene IDs for gene known only by their symbols)
 *
 * @class UmcMsvGeneLegend
 * @constructor
 */
 define(["pedigree/view/geneLegend"], function(GeneLegend){
    var UmcMsvGeneLegend = Class.create( GeneLegend, {

        initialize: function($super) {
            $super('Known disease genes missing a second variant', 'genes',
                   "umc>msv",
                   [], // these are never displayed in a legend so don't need colours
                   "getUmcMsvGenes",
                   "setUmcMsvGenes", true); // operation
        },

        addCase: function($super, id, symbol, nodeID) {
            $super(id, symbol, nodeID, true);
        }
    });
    return UmcMsvGeneLegend;
});
