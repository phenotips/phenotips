/**
 * Specific version of GeneLegend for candidate gene display.
 *
 * @class GeneLegend
 * @constructor
 */
 define(["pedigree/view/geneLegend"], function(GeneLegend){
    var CandidateGeneLegend = Class.create( GeneLegend, {

        initialize: function($super) {
            $super('Candidate Genes',
                   "candidate",
                   ['#81a270', '#c4e8c4', '#56a270', '#b3b16f', '#4a775a', '#65caa3'], // green palette
                   "getCandidateGenes",
                   "setCandidateGenes"); // operation
        }
    });
    return CandidateGeneLegend;
});