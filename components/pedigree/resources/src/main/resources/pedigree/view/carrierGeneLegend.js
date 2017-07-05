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
                   ['#0684ed', '#6db5f1', '#55c6e7', '#0e5b9b', '#006ff5', '#9ecaff'], // blue palette
                   "getCarrierGenes",
                   "setCarrierGenes"); // operation
        }
    });
    return CarrierGeneLegend;
});
