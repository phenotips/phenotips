/**
 * Specific version of GeneLegend for known disease genes with a variant of unknown significance. It is not displayed but used
 * because of the way gene update machinery works (to update gene IDs for gene known only by their symbols)
 *
 * @class UmcVusGeneLegend
 * @constructor
 */
 define(["pedigree/view/geneLegend"], function(GeneLegend){
    var UmcVusGeneLegend = Class.create( GeneLegend, {

        initialize: function($super) {
            $super('Known disease genes with a variant of unknown significance', 'genes',
                   "umc>vus",
                   [], // these are never displayed in a legend so don't need colours
                   "getUmcVusGenes",
                   "setUmcVusGenes", true); // operation
        },

        addCase: function($super, id, symbol, nodeID) {
            $super(id, symbol, nodeID, true);
        }
    });
    return UmcVusGeneLegend;
});
