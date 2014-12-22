/**
 * Class responsible for keeping track of HPO terms and their properties, and for
 * caching disorders data as loaded from the OMIM database.
 * This information is graphically displayed in a 'Legend' box
 *
 * @class HPOLegend
 * @constructor
 */
var HPOLegend = Class.create( Legend, {

    initialize: function($super) {
        $super("Phenotypes", true);

        this._termCache = {};
    },

    _getPrefix: function(id) {
        return "phenotype";
    },

    /**
     * Returns the HPOTerm object with the given ID. If object is not in cache yet
     * returns a newly created one which may have the term name & other attributes not loaded yet
     *
     * @method getTerm
     * @return {Object}
     */    
    getTerm: function(hpoID) {
        hpoID = HPOTerm.sanitizeID(hpoID);
        if (!this._termCache.hasOwnProperty(hpoID)) {
            var whenNameIsLoaded = function() { this._updateTermName(hpoID); }
            this._termCache[hpoID] = new HPOTerm(hpoID, null, whenNameIsLoaded.bind(this));            
        }
        return this._termCache[hpoID];
    },

    /**
     * Retrieve the color associated with the given object
     *
     * @method getObjectColor
     * @param {String|Number} id ID of the object
     * @return {String} CSS color value for that disorder
     */
    getObjectColor: function(id) {
        return "#CCCCCC";
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

        $super(id, name, nodeID);
    },

    /**
     * Updates the displayed phenotype name for the given phenotype
     *
     * @method _updateTermName
     * @param {Number} id The identifier of the phenotype to update
     * @private
     */    
    _updateTermName: function(id) {
        //console.log("updating phenotype display for " + id + ", name = " + this.getTerm(id).getName());
        var name = this._legendBox.down('li#' + this._getPrefix() + '-' + id + ' .disorder-name');
        name.update(this.getTerm(id).getName());
    },

    /**
     * Callback for dragging an object from the legend onto nodes
     *
     * @method _onDropGeneric
     * @param {Person} Person node
     * @param {String|Number} id ID of the phenotype being dropped
     */
    _onDropObject: function(node, hpoID) {
        if (node.isPersonGroup()) {
            return;
        }
        var currentHPO = node.getHPO().slice(0);
        if (currentHPO.indexOf(hpoID) == -1) {
            currentHPO.push(hpoID);
            editor.getView().unmarkAll();
            var properties = { "setHPO": currentHPO };
            var event = { "nodeID": node.getID(), "properties": properties };
            document.fire("pedigree:node:setproperty", event);
        } else {
            alert("This person already has the selected phenotype");
        }
    },
});
