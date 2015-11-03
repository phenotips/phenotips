/**
 * Class responsible for keeping track of disorders and their properties, and for
 * caching disorders data as loaded from the OMIM database.
 * This information is graphically displayed in a 'Legend' box.
 *
 * @class DisorderLegend
 * @constructor
 */
define([
        "pedigree/disorder",
        "pedigree/view/legend"
    ], function(
        Disorder,
        Legend
    ){
    var DisorderLegend = Class.create( Legend, {

        initialize: function($super) {
            $super('Disorders', true);

            this._disorderCache = {};
        },

        _getPrefix: function(id) {
            return "disorder";
        },

        /**
         * Returns the disorder object with the given ID. If object is not in cache yet
         * returns a newly created one which may have the disorder name & other attributes not loaded yet
         *
         * @method getDisorder
         * @return {Object}
         */    
        getDisorder: function(disorderID) {
            if (!this._disorderCache.hasOwnProperty(disorderID)) {
                var whenNameIsLoaded = function() { this._updateDisorderName(disorderID); }
                this._disorderCache[disorderID] = new Disorder(disorderID, null, whenNameIsLoaded.bind(this));            
            }
            return this._disorderCache[disorderID];
        },

        /**
         * Returns a map disorderID -> disorderName
         */
        getAllNames: function() {
            var result = {};
            for (var disorderID in this._affectedNodes) {
                if (this._affectedNodes.hasOwnProperty(disorderID)) {
                    result[disorderID] = this.getDisorder(disorderID).getName();
                }
            }
            return result;
        },

        /**
         * Registers an occurrence of a disorder. If disorder hasn't been documented yet,
         * designates a color for it.
         *
         * @method addCase
         * @param {Number|String} disorderID ID for this disorder taken from the OMIM database
         * @param {String} disorderName The name of the disorder
         * @param {Number} nodeID ID of the Person who has this disorder
         */
        addCase: function($super, disorderID, disorderName, nodeID) {
            if (!this._disorderCache.hasOwnProperty(disorderID))
                this._disorderCache[disorderID] = new Disorder(disorderID, disorderName);

            $super(disorderID, disorderName, nodeID);
        },

        /**
         * Updates the displayed disorder name for the given disorder
         *
         * @method _updateDisorderName
         * @param {Number} disorderID The identifier of the disorder to update
         * @private
         */    
        _updateDisorderName: function(disorderID) {
            //console.log("updating disorder display for " + disorderID + ", name = " + this.getDisorder(disorderID).getName());
            var name = this._legendBox.down('li#' + this._getPrefix() + '-' + disorderID + ' .disorder-name');
            name.update(this.getDisorder(disorderID).getName());
        },   

        /**
         * Generate the element that will display information about the given disorder in the legend
         *
         * @method _generateElement
         * @param {Number} disorderID The id for the disorder, taken from the OMIM database
         * @param {String} name The human-readable disorder name
         * @return {HTMLLIElement} List element to be insert in the legend
         */
        _generateElement: function($super, disorderID, name) {
            if (!this._objectColors.hasOwnProperty(disorderID)) {
                var color = this._generateColor(disorderID);
                this._objectColors[disorderID] = color;
                document.fire('disorder:color', {'id' : disorderID, color: color});
            }

            return $super(disorderID, name);
        },

        /**
         * Callback for dragging an object from the legend onto nodes
         *
         * @method _onDropGeneric
         * @param {Person} Person node
         * @param {String|Number} id ID of the disorder being dropped
         */
        _onDropObject: function(node, disorderID) {
            var currentDisorders = node.getDisorders().slice(0);
            if (currentDisorders.indexOf(disorderID) == -1) {   // only if the node does not have this disorder yet
                currentDisorders.push(disorderID);
                editor.getView().unmarkAll();
                var properties = { "setDisorders": currentDisorders };
                var event = { "nodeID": node.getID(), "properties": properties };
                document.fire("pedigree:node:setproperty", event);
            } else {
                alert("This person already has the specified disorder");
            }
        },

        /**
         * Generates a CSS color.
         * Has preference for some predefined colors that can be distinguished in gray-scale
         * and are distint from gene colors.
         *
         * @method generateColor
         * @return {String} CSS color
         */
        _generateColor: function(disorderID) {
            if(this._objectColors.hasOwnProperty(disorderID)) {
                return this._objectColors[disorderID];
            }

            var usedColors = Object.values(this._objectColors),
                // [red/yellow]           prefColors = ["#FEE090", '#f8ebb7', '#eac080', '#bf6632', '#9a4500', '#a47841', '#c95555', '#ae6c57'];        
                // [original yellow/blue] prefColors = ["#FEE090", '#E0F8F8', '#8ebbd6', '#4575B4', '#fca860', '#9a4500', '#81a270'];
                // [green]                prefColors = ['#81a270', '#c4e8c4', '#56a270', '#b3b16f', '#4a775a', '#65caa3'];
            //#E0F8F8
            prefColors = ['#D1E9E9', '#92c0db', '#4575B4', '#949ab8', "#FEE090", '#bf6632', '#fca860', '#9a4500', '#d12943', '#00a2bf'];
            if (disorderID == "affected") {
                prefColors = ["#FEE090", "#dbad71"];
            }
            if (this.getPreferedColor(disorderID) !== null) {
                prefColors.unshift(this.getPreferedColor(disorderID));
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
    return DisorderLegend;
});
