/**
 * Specific version of GeneLegend for candidate novel disease genes. It is not displayed but used
 * because of the way gene update machinery works (to update gene IDs for gene known only by their symbols)
 *
 * @class CandidateNovelDiseaseGeneLegend
 * @constructor
 */
 define(["pedigree/view/geneLegend"], function(GeneLegend){
    var CandidateNovelDiseaseGeneLegend = Class.create( GeneLegend, {

        initialize: function($super) {
            $super('Candidate Novel Disease Genes', 'genes',
                   "candidate>novel_disease",
                   [], // these are never displayed in a legend so don't need colours
                   "getCandidateNovelDiseaseGenes",
                   "setCandidateNovelDiseaseGenes", true); // operation
        },

        addCase: function($super, id, symbol, nodeID) {
            $super(id, symbol, nodeID, true);
        }
    });
    return CandidateNovelDiseaseGeneLegend;
});
