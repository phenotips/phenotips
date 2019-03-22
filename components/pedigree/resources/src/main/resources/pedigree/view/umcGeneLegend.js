/**
 * Specific version of GeneLegend for uncertain molecular cause genes. It is not displayed but used
 * because of the way gene update machinery works (to update gene IDs for gene known only by their symbols)
 *
 * @class UmcGeneLegend
 * @constructor
 */
 define(["pedigree/view/geneLegend"], function(GeneLegend){
    var UmcGeneLegend = Class.create( GeneLegend, {

        initialize: function($super) {
            $super('Uncertain Molecular Cause Genes', 'genes',
                   "umc",
                   [], // these are never displayed in a legend so don't need colours
                   "getUmcGenes",
                   "setUmcGenes", true); // operation
        },

        addCase: function($super, id, symbol, nodeID) {
            $super(id, symbol, nodeID, true);
        }
    });
    return UmcGeneLegend;
});
