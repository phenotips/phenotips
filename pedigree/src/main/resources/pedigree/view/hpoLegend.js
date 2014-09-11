/**
 * Class responsible for keeping track of HPO terms and their properties.
 * This information is graphically displayed in a 'Legend' box
 *
 * @class HPOLegend
 * @constructor
 */
           
var HPOLegend = Class.create( Legend, {

    initialize: function($super) {
        $super("Phenotypes");

        this._termCache = {};        
        this._affectedNodes  = {};

        this._termList = new Element('ul', {'class' : 'disorder-list'});
        this._legendBox.insert(this._termList);
    },
   
    /**
     * Returns the name of HPO term with the given ID.
     * Returns "" for unknown terms and temrs not loaded form the database
     *
     * @method getTermName
     * @return {String} Name/description of the given term
     */
    getTermName: function(hpoID) {
        return this.getTerm(hpoID).getName();
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
     * Registers an occurrence of a phenotype.
     *
     * @method addCase
     * @param {Number|String} id ID for this term taken from the HPO database
     * @param {String} name The description of the phenotype
     * @param {Number} nodeID ID of the Person who has this phenotype
     */
    addCase: function(id, name, nodeID) {
        if (!this._termCache.hasOwnProperty(id))
            this._termCache[id] = new HPOTerm(id, name);
                                
        if(Object.keys(this._affectedNodes).length == 0) {
            this._legendBox.show();
        }
        if(!this._hasAffectedNodes(id)) {
            this._affectedNodes[id] = [nodeID];
            var listElement = this._generateTermElement(id, name);
            this._termList.insert(listElement);
        }
        else {
            this._affectedNodes[id].push(nodeID);
        }
        this._displayCasesForTerm(id);
    },

    /**
     * Removes an occurrence of a phenotype if there are any. Removes the term
     * from the 'Legend' box if this phenotype is not registered in any individual.
     *
     * @param {Number} id ID for this phenotype taken from the HPO database
     * @param {Number} nodeID ID of the Person who has this phenotype
     */
    removeCase: function(id, nodeID) {
        if (this._hasAffectedNodes(id)) {
            this._affectedNodes[id] = this._affectedNodes[id].without(nodeID);
            if(this._affectedNodes[id].length == 0) {
                delete this._affectedNodes[id];
                $('phenotype-' + id).remove();                
                if(Object.keys(this._affectedNodes).length == 0) {
                    this._legendBox.hide();
                }
            }
            else
                this._displayCasesForTerm(id);
        }
    },
    
    /**
     * Updates the displayed number of affected cases for for the phenoype in the legend UI.
     *
     * @method _displayCasesForTerm
     * @param {Number} id The identifier of the phenotype to update
     * @private
     */
    _displayCasesForTerm : function(id) {
      var label = this._legendBox.down('li#phenotype-' + id + ' .disorder-cases');
      if (label) {
        var cases = this._affectedNodes.hasOwnProperty(id) ? this._affectedNodes[id].length : 0;
        label.update(cases + "&nbsp;case" + ((cases - 1) && "s" || ""));
      }
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
        var name = this._legendBox.down('li#phenotype-' + id + ' .disorder-name');
        name.update(this.getTerm(id).getName());
    },   

    /**
     * Returns True if there are nodes reported to have this phenotype in the Legend
     *
     * @method _hasAffectedNodes
     * @param {Number} id The id for the phenotype
     * @private 
     */
    _hasAffectedNodes: function(id) {
        return this._affectedNodes.hasOwnProperty(id);
    },

    /**
     * Generate the element that will display information about the given phenotype in the legend
     *
     * @method _generateTermElement
     * @param {Number} id The id for the phenotype, taken from the HPO database
     * @param {String} name The human-readable term name
     * @return {HTMLLIElement} List element to be insert in the legend
     */
    _generateTermElement: function(id, name) {
        var color = "#CCCCCC";
        var item = new Element('li', {'class' : 'disorder', 'id' : 'phenotype-' + id}).update(new Element('span', {'class' : 'disorder-name'}).update(name));
        var bubble = new Element('span', {'class' : 'disorder-color'});
        bubble.style.backgroundColor = color;
        item.insert({'top' : bubble});        
        var countLabel = new Element('span', {'class' : 'disorder-cases'});
        var countLabelContainer = new Element('span', {'class' : 'disorder-cases-container'}).insert("(").insert(countLabel).insert(")");
        item.insert(" ").insert(countLabelContainer);
        var me = this;
        Element.observe(item, 'mouseover', function() {
            //item.setStyle({'text-decoration':'underline', 'cursor' : 'default'});
            item.down('.disorder-name').setStyle({'background': color, 'cursor' : 'default'});
            me._affectedNodes[id] && me._affectedNodes[id].forEach(function(nodeID) {
                var node = editor.getNode(nodeID);
                node && node.getGraphics().highlight();
            });
        });
        Element.observe(item, 'mouseout', function() {
            //item.setStyle({'text-decoration':'none'});
            item.down('.disorder-name').setStyle({'background':'', 'cursor' : 'default'});
            me._affectedNodes[id] && me._affectedNodes[id].forEach(function(nodeID) {
                var node = editor.getNode(nodeID);
                node && node.getGraphics().unHighlight();
            });
        });
        return item;
    }
});
