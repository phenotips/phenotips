/**
 * Specific version of GeneLegend for confirmed causal gene display.
 *
 * @class GeneLegend
 * @constructor
 */
 define(["pedigree/view/geneLegend"], function(GeneLegend){
    var CasualGeneLegend = Class.create( GeneLegend, {

        initialize: function($super) {
            $super('Confirmed Casual Genes',
                   "casual",
                   ["#FEE090", '#f8ebb7', '#eac080', '#bf6632', '#9a4500', '#a47841', '#c95555', '#ae6c57'], // TODO: replace
                   "getCasualGenes",
                   "setCasualGenes"); // operation
        }
    });
    return CasualGeneLegend;
});