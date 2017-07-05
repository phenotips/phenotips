/**
 * Specific version of GeneLegend for rejected genes. It is nbot displayed but used
 * because of the way gene update machinery works (to update gene IDs for gene known only by their symbols)
 *
 * @class RejecetdGeneLegend
 * @constructor
 */
 define(["pedigree/view/geneLegend"], function(GeneLegend){
    var RejectedGeneLegend = Class.create( GeneLegend, {

        initialize: function($super) {
            $super('Negative Genes', 'genes',
                   "rejected",
                   [], // these are never displayed in a legend so don't need colours
                   "getRejectedGenes",
                   "setRejectedGenes", true); // operation
        }
    });
    return RejectedGeneLegend;
});
