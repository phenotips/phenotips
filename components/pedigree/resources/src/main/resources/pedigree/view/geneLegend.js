/**
 * Class responsible for keeping track of some genes.
 * This information is graphically displayed in a 'Legend' box.
 *
 * @class GeneLegend
 * @constructor
 */
 define([ "pedigree/view/legend",
          "pedigree/model/helpers" ],
    function( Legend,
              Helpers) {
    var GeneLegend = Class.create( Legend, {

        initialize: function($super, title, prefix, palette, getOperation, setOperation) {
            this.prefix = prefix;
            this.prefColors = palette;
            this.setOperation = setOperation; // for drag and drop
            this.getOperation = getOperation; // for drag and drop
            $super(title, true);
        },

        _getPrefix: function(id) {
            return this.prefix + "gene";
        },

        /**
         * Retrieve the color associated with the given gene - regardless of which gene legend has it
         *
         * @method getGeneColor
         */
        getGeneColor: function(geneID, nodeID) {
            if (this._hasAffectedNodes(geneID) && Helpers.arrayIndexOf(this._affectedNodes[geneID], nodeID) >= 0){
                return this.getObjectColor(geneID);
            }
            return undefined;
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
                document.fire('gene:color', {'id' : geneID, "color": color, "prefix": this.prefix});
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
            var currentGenes = node[this.getOperation]();
            // TODO: check if indexof STILL MAKES SENSE
            if (currentGenes.indexOf(geneID) == -1) {   // only if the node does not have this gene yet
                currentGenes.push(geneID);
                editor.getView().unmarkAll();
                var properties = {};
                properties[this.setOperation] = currentGenes;
                var event = { "nodeID": node.getID(), "properties": properties };
                document.fire("pedigree:node:setproperty", event);
            } else {
                this._onFailedDrag(node, "This person already has the selected " + this.prefix + " gene", "Can't drag this gene to this person");
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

            var usedColors = Object.values(this._objectColors);

            if (this.getPreferedColor(geneID) !== null) {
                this.prefColors.unshift(this.getPreferedColor(geneID));
            }
            for (var i = 0; i < usedColors.length; i++) {
                this.prefColors = this.prefColors.without(usedColors[i]);
            };
            if(this.prefColors.length > 0) {
                return this.prefColors[0];
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
