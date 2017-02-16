/**
 * Specific version of GeneLegend for carrier genes. It is not displayed but used
 * because of the way gene update machinery works (to update gene IDs for gene known only by their symbols)
 *
 * @class CarrierGeneLegend
 * @constructor
 */
 define(["pedigree/view/geneLegend"], function(GeneLegend){
    var CarrierGeneLegend = Class.create( GeneLegend, {

        initialize: function($super) {
            $super('Carrier Genes', 'genes',
                   "carrier",
                   [], // these are never displayed in a legend so don't need colours
                   "getCarrierGenes",
                   "setCarrierGenes", true); // operation
        }
    });
    return CarrierGeneLegend;
});
