/**
 * Specific version of GeneLegend for candidate known disease genes with novel phenotype. It is not displayed but used
 * because of the way gene update machinery works (to update gene IDs for gene known only by their symbols)
 *
 * @class CandidateNovelPhenGeneLegend
 * @constructor
 */
 define(["pedigree/view/geneLegend"], function(GeneLegend){
    var CandidateNovelPhenGeneLegend = Class.create( GeneLegend, {

        initialize: function($super) {
            $super('Candidate Known Disease Genes With Novel Phenotype', 'genes',
                   "candidate>novel_phen",
                   [], // these are never displayed in a legend so don't need colours
                   "getCandidateNovelPhenGenes",
                   "setCandidateNovelPhenGenes", true); // operation
        },

        addCase: function($super, id, symbol, nodeID) {
            $super(id, symbol, nodeID, true);
        }
    });
    return CandidateNovelPhenGeneLegend;
});
