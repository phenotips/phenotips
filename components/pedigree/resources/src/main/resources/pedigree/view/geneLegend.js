/**
 * Class responsible for keeping track of candidate genes.
 * This information is graphically displayed in a 'Legend' box.
 *
 * @class GeneLegend
 * @constructor
 */
 define(["pedigree/view/legend"], function(Legend){
    var GeneLegend = Class.create( Legend, {

        initialize: function($super) {
            $super('Candidate Genes', true);
        },

        _getPrefix: function(id) {
            return "gene";
        },

        /**
         * Generate the element that will display information about the given disorder in the legend
         *
         * @method _generateElement
         * @param {String} geneID The id for the gene
         * @param {String} name The human-readable gene description
         * @return {HTMLLIElement} List element to be insert in the legend
         */
        _generateElement: function($super, geneID, name) {
            if (!this._objectColors.hasOwnProperty(geneID)) {
                var color = this._generateColor(geneID);
                this._objectColors[geneID] = color;
                document.fire('gene:color', {'id' : geneID, color: color});
            }

            return $super(geneID, name);
        },

        /**
         * Callback for dragging an object from the legend onto nodes
         *
         * @method _onDropGeneric
         * @param {Person} Person node
         * @param {String|Number} id ID of the gene being dropped
         */
        _onDropObject: function(node, geneID) {
            if (node.isPersonGroup()) {
                return;
            }
            var currentGenes = node.getGenes().slice(0);
            if (currentGenes.indexOf(geneID) == -1) {   // only if the node does not have this gene yet
                currentGenes.push(geneID);
                editor.getView().unmarkAll();
                var properties = { "setGenes": currentGenes };
                var event = { "nodeID": node.getID(), "properties": properties };
                document.fire("pedigree:node:setproperty", event);
            } else {
                this._onFailedDrag(node, "This person already has the selected candidate gene", "Can't drag this gene to this person");
            }
        },

        /**
         * Generates a CSS color.
         * Has preference for some predefined colors that can be distinguished in gray-scale
         * and are distint from disorder colors.
         *
         * @method generateColor
         * @return {String} CSS color
         */
        _generateColor: function(geneID) {
            if(this._objectColors.hasOwnProperty(geneID)) {
                return this._objectColors[geneID];
            }

            var usedColors = Object.values(this._objectColors),
            // green palette
            prefColors = ['#81a270', '#c4e8c4', '#56a270', '#b3b16f', '#4a775a', '#65caa3'];
            if (this.getPreferedColor(geneID) !== null) {
                prefColors.unshift(this.getPreferedColor(geneID));
            }
            usedColors.each( function(color) {
                prefColors = prefColors.without(color);
            });
            if(prefColors.length > 0) {
                return prefColors[0];
            }
            else {
                var randomColor = Raphael.getColor();
                while(randomColor == "#ffffff" || usedColors.indexOf(randomColor) != -1) {
                    randomColor = "#"+((1<<24)*Math.random()|0).toString(16);
                }
                return randomColor;
            }
        }
    });
    return GeneLegend;
});