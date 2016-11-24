/**
 * Class responsible for keeping track of some genes.
 * This information is graphically displayed in a 'Legend' box.
 *
 * @class GeneLegend
 * @constructor
 */
 define([ "pedigree/view/legend",
          "pedigree/gene",
          "pedigree/view/person",
          "pedigree/model/helpers" ],
    function( Legend,
              Gene,
              Person,
              Helpers) {
    var GeneLegend = Class.create( Legend, {

        initialize: function($super, title, droppableName, prefix, palette, getOperation, setOperation, hiddenLegend) {
            this.prefix = prefix;
            this.prefColors = palette;
            this.setOperation = setOperation; // for drag and drop
            this.getOperation = getOperation; // for drag and drop

            $super(title, droppableName, true, false, hiddenLegend);

            if (editor._geneCache == undefined) {
                // one cache for all gene types
                editor._geneCache = {};
            }

            document.observe('gene:loaded', function(event) {
                if (!event.memo || !event.memo.oldid || !event.memo.newid || !event.memo.symbol) {
                    return;
                }

                // get the list affected patients before it gets removed
                var affectedPatients = this._affectedNodes.hasOwnProperty(event.memo.oldid) ? this._affectedNodes[event.memo.oldid].slice(0) : [];

                console.log("[" + this._getPrefix() + "] updating gene " + event.memo.oldid + " to ID=" + event.memo.newid + ", SYMBOL=" + event.memo.symbol);
                this.updateGeneData(event.memo.oldid, event.memo.newid, event.memo.symbol);

                // update all patient data
                for (var i = 0; i < affectedPatients.length; i++) {
                    var node = editor.getNode(affectedPatients[i]);

                    var currentGenes = node[this.getOperation]();
                    var geneIndex = Person.getGeneIndex(event.memo.oldid, currentGenes);
                    if (geneIndex >= 0) {
                        currentGenes[geneIndex].id = event.memo.newid;
                        currentGenes[geneIndex].gene = event.memo.symbol;
                    }
                    var properties = {};
                    properties[this.setOperation] = currentGenes;
                    // note: users shouldnot be able to "undo" a gene update operation
                    var personUpdateEvent = { "nodeID": node.getID(), "properties": properties, "replaceLastUndoState": true };
                    document.fire("pedigree:node:setproperty", personUpdateEvent);
                }
            }.bind(this));
        },

        _getPrefix: function(id) {
            return this.prefix + "gene";
        },

        /**
         * Returns the Gene object with the given ID. If object is not in cache yet
         * returns a newly created one which may have the gene symbol & other attributes not loaded yet
         *
         * @method getGene
         * @return {Object}
         */
        getGene: function(geneID) {
            if (!editor._geneCache.hasOwnProperty(geneID)) {
                editor._geneCache[geneID] = new Gene(geneID);
            }
            return editor._geneCache[geneID];
        },

        /**
         * Returns a symbol for a gene with the given ID (ID and symbol are the same for genes without EnsembleID)
         *
         * @param {String|Number} Gene ID
         * @return {String} associated gene name taken from the vocabulary and loaded via AJAX call
         */
        getSymbol: function($super, geneID) {
            return this.getGene(geneID).getSymbol();
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
         * When an ID changes (e.g. a gene was added by symbol and an ID was loaded using geneNameService)
         * need to remove all cases with oldID and add cases with newID, this will take care of correctly
         * updating counts in case newID already has some cases independent form oldID.
         */
        updateGeneData: function(oldId, newId, symbol) {
            // update symbol
            var name = this._legendBox.down('li#' + this._getPrefix() + '-' + this._hashID(oldId) + ' .disorder-name');
            name && name.update(symbol);

            if (oldId != newId) {
                if (!editor._geneCache.hasOwnProperty(newId) && editor._geneCache.hasOwnProperty(oldId)) {
                    editor._geneCache[newId] = editor._geneCache[oldId];
                }
                delete editor._geneCache[oldId];

                if (!this._objectColors.hasOwnProperty(newId) && this._objectColors.hasOwnProperty(oldId)) {
                    this._objectColors[newId] = this._objectColors[oldId];
                }
                if (!this._preferredColors.hasOwnProperty(newId) && this._preferredColors.hasOwnProperty(oldId)) {
                    this._preferredColors[newId] = this._preferredColors[oldId];
                }
                if (this._hasAffectedNodes(oldId)) {
                    delete this._affectedNodes[oldId];
                    delete this._objectColors[oldId];
                    var htmlElement = this._getListElementForObjectWithID(oldId);
                    htmlElement && htmlElement.remove();
                }
            }
        },

        /**
         * Registers an occurrence of a gene.
         *
         * @method addCase
         * @param {Number|String} id ID for this gene (not displayed)
         * @param {String} symbol The gene symbol (displayed)
         * @param {Number} nodeID ID of the Person who has this phenotype
         */
        addCase: function($super, id, symbol, nodeID) {
            if (!editor._geneCache.hasOwnProperty(id)) {
                console.log("[" + this._getPrefix() + "] adding new case " + id);
                editor._geneCache[id] = new Gene(id, symbol);
            }
            if (symbol != this.getSymbol(id)) {
                symbol = this.getSymbol(id);
            }

            $super(id, symbol, nodeID);
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
                this.setObjectColor(geneID, color);
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
        _onDropObject: function($super, node, geneID) {
            if (!$super(node, geneID)) {
                return false;
            }

            if (node.getGeneStatus(geneID) != this._droppableName) {
                // only if the node does not already have this gene in this status
                var gene = this.getGene(geneID);

                var currentGenes = node[this.getOperation]();
                currentGenes.push({"id": geneID, "gene": gene.getSymbol()});

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
