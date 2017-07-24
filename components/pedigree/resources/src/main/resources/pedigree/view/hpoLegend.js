/**
 * Class responsible for keeping track of HPO terms and their properties, and for
 * caching disorders data as loaded from the OMIM database.
 * This information is graphically displayed in a 'Legend' box
 *
 * @class HPOLegend
 * @constructor
 */
define([
        "pedigree/hpoTerm",
        "pedigree/view/legend"
    ], function(
        HPOTerm,
        Legend
    ){
    var HPOLegend = Class.create( Legend, {

        initialize: function($super) {
            $super("Phenotypes", "phenotypes", true);

            this._termCache = {};
        },

        _getPrefix: function(humanReadablePlural) {
            return "phenotype" + (humanReadablePlural ? "s" : "");
        },

        /**
         * Returns the HPOTerm object with the given ID. If object is not in cache yet
         * returns a newly created one which may have the term name & other attributes not loaded yet
         *
         * @method getTerm
         * @return {Object}
         */
        getTerm: function(hpoID) {
            if (!this._termCache.hasOwnProperty(hpoID)) {
                var whenNameIsLoaded = function() { this._updateTermName(hpoID); };
                this._termCache[hpoID] = new HPOTerm(hpoID, null, whenNameIsLoaded.bind(this));
            }
            return this._termCache[hpoID];
        },

        /**
         * Returns a name of a phenotype
         *
         * @param {String|Number} HPOTerm ID
         * @return {String} associated phenotype name taken from the HPO database
         */
        getName: function($super, hpoID) {
            return this.getTerm(hpoID).getName();
        },

        /**
         * Registers an occurrence of a phenotype.
         *
         * @method addCase
         * @param {Number|String} id ID for this term taken from the HPO database
         * @param {String} name The description of the phenotype
         * @param {Number} nodeID ID of the Person who has this phenotype
         */
        addCase: function($super, id, name, nodeID) {
            if (!this._termCache.hasOwnProperty(id))
                this._termCache[id] = new HPOTerm(id, name);

            $super(id, name, nodeID, true);
        },

        /**
         * Updates the displayed phenotype name for the given phenotype
         *
         * @method _updateTermName
         * @param {Number} id The identifier of the phenotype to update
         * @private
         */
        _updateTermName: function(id) {
            var name = this._legendBox.down('li#' + this._getPrefix() + '-' + this._hashID(id) + ' .abnormality-phenotype-name');
            name.update(this.getTerm(id).getName());
            this._updateMinMaxButtons();
        },

        /**
         * Generate the element that will display information about the given phenotype in the legend
         *
         * @method _generateElement
         * @param {Number} hpoID The id for the phenotype
         * @param {String} name Human-readable name
         * @return {HTMLLIElement} List element to be insert in the legend
         */
        _generateElement: function($super, hpoID, name) {
            if (!this._objectColors.hasOwnProperty(hpoID)) {
                var color = this._generateColor(hpoID);
                this._objectColors[hpoID] = color;
                document.fire('hpo:color', {'id' : hpoID, color: color});
            }
            return $super(hpoID, name);
        },

        /**
         * Callback for dragging an object from the legend onto nodes
         *
         * @method _onDropGeneric
         * @param {Person} Person node
         * @param {String|Number} id ID of the phenotype being dropped
         */
        _onDropObject: function($super, node, hpoID) {
            if (!$super(node, hpoID)) {
                return false;
            }
            var currentHPO = node.getHPO().slice(0);
            if (currentHPO.indexOf(hpoID) == -1) {
                currentHPO.push(hpoID);
                editor.getView().unmarkAll();
                var properties = { "setHPO": currentHPO };
                var event = { "nodeID": node.getID(), "properties": properties };
                document.fire("pedigree:node:setproperty", event);
            } else {
                this._onFailedDrag(node, "This person already has the selected phenotype", "Can't drag this phenotype to this person");
            }
        },

        /**
         * Generates a CSS color.
         * Has preference for some predefined colors that can be distinguished in gray-scale
         * and are distint from disorder/gene colors.
         *
         * @method generateColor
         * @return {String} CSS color
         */
        _generateColor: function(hpoID) {
            if(this._objectColors.hasOwnProperty(hpoID)) {
                return this._objectColors[hpoID];
            }

            var usedColors = Object.values(this._objectColors);
            // red/yellow palette
            var prefColors = ['#eedddd', '#bbaaaa', '#998888', '#887766'];
            if (this.getPreferedColor(hpoID) !== null) {
                prefColors.unshift(this.getPreferedColor(hpoID));
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
    return HPOLegend;
});
