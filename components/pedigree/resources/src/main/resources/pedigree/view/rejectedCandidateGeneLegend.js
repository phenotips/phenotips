/**
 * Specific version of GeneLegend for rejected candidate genes. It is not displayed but used
 * because of the way gene update machinery works (to update gene IDs for gene known only by their symbols)
 *
 * @class RejectedCandidateGeneLegend
 * @constructor
 */
 define(["pedigree/view/geneLegend"], function(GeneLegend){
    var RejectedCandidateGeneLegend = Class.create( GeneLegend, {

        initialize: function($super) {
            $super('Rejected Candidate Genes', 'genes',
                   "rejected_candidate",
                   [], // these are never displayed in a legend so don't need colours
                   "getRejectedCandidateGenes",
                   "setRejectedCandidateGenes", true); // operation
        },

        addCase: function($super, id, symbol, nodeID) {
            $super(id, symbol, nodeID, true);
        }
    });
    return RejectedCandidateGeneLegend;
});
