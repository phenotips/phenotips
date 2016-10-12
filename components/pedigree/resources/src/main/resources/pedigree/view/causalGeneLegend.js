/**
 * Specific version of GeneLegend for confirmed causal gene display.
 *
 * @class GeneLegend
 * @constructor
 */
 define(["pedigree/view/geneLegend"], function(GeneLegend){
    var CausalGeneLegend = Class.create( GeneLegend, {

        initialize: function($super) {
            $super('Confirmed Causal Genes', 'genes',
                   "causal",
                   ["#FEE090", '#f8ebb7', '#eac080', '#bf6632', '#9a4500', '#a47841', '#c95555', '#ae6c57'], // TODO: replace
                   "getCausalGenes",
                   "setCausalGenes"); // operation
        }
    });
    return CausalGeneLegend;
});