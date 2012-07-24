/*
 * Class responsible for keeping track of disorders on canvas and their properties.
 * This information is graphically displayed in a 'Legend' box
 */

var Legend = Class.create( {

    initialize: function() {
        this._disorders = {};
        this._evaluations = {};
        this._usedColors = [];
        //TODO: this.buildLegend
    },

    /*
     * Returns an object of disorder ID's mapped to Disorder objects.
     */
    getDisorders: function() {
        return this._disorders;
    },

    /*
     * Replaces the disorder map with the map passed in the parameter, and updates
     * the graphical 'Legend' to represent the new information
     *
     * @param map is an object that has disorder IDs as keys and Disorder objects as values
     */
    setDisorders: function(map) {
        this._disorders = map;
        //TODO: this.buildLegend
    },

    /*
     * Returns an object of evaluation ID's mapped to the evaluation properties.
     */
    getEvaluations: function() {
        return this._evaluations;
    },

    /*
     * Replaces the evaluation map with the map passed in the parameter, and updates
     * the graphical 'Legend' to represent the new information
     *
     * @param map is an object formatted as follows { E1 : {conclusion: '+', result: '(36n/18n)', numAEvaluations: 4}}
     */
    setEvaluations: function(map) {
        this._evaluations = map;
        //TODO: this.buildLegend
    },

    /*
     * Returns the Disorder object with the given disorder ID.
     *
     * @param disorderID the id number for the disorder, taken from the OMIM database
     */
    getDisorder: function(disorderID) {
        return this.getDisorders()[disorderID];
    },

    /*
     * Replaces the Disorder object with the given disorder ID.
     *
     * @param disorderID the id number for the disorder, taken from the OMIM database
     * @param disorder a Disorder object
     */
    setDisorder: function(disorderID, disorder) {
        this.getDisorders()[disorderID] = disorder;
    },

    /*
     * Registers an occurrence of a disorder. If disorder hasn't been documented yet,
     * designates a color for it.
     *
     * @param disorderID the id number for the disorder, taken from the OMIM database
     * @param disorderName the name of the disorder, taken from the OMIM database
     */
    addCase: function(disorderID, disorderName) {
        if (!this.containsDisorder(disorderID)) {
            this.setDisorder(disorderID, new Disorder(disorderID, disorderName, null, 0));
            this.addUsedColor(this.getDisorder(disorderID).getColor());
            //TODO: this.buildLegend
        }
            this.getDisorder(disorderID).incrementNumAffected();
    },

    /*
     * Removes an occurrence of a disorder if there are any. Removes the disorder
     * from the 'Legend' box if this disorder is not registered in any individual.
     *
     * @param disorderID the id number for the disorder, taken from the OMIM database
     */
    removeCase: function(disorderID) {
        if (this.containsDisorder(disorderID)) {
            this.getDisorder(disorderID).decrementNumAffected();
            if(this.getDisorder(disorderID).getNumAffected() < 1) {
                delete this.getDisorder(disorderID);
                //TODO: this.buildLegend
            }
        }
    },

    /*
     * Returns true if the disorder with the given ID is registered in the Legend
     *
     * @param disorderID the id number for the disorder, taken from the OMIM database
     */
    containsDisorder: function(disorderID) {
        return disorderID in this._disorders;
    },

    /*
     * Returns an array of colors associated with disorders in the Legend
     *
     * @param disorderID the id number for the disorder, taken from the OMIM database
     */
    getUsedColors: function() {
        return this._usedColors;
    },

    /*
     * Replaces the array of colors associated with disorders in the Legend
     *
     * @param colors an array of strings representing CSS colors. e.g. ['blue', '#DADADA']
     */
    setUsedColors: function(colors) {
        this._usedColors = colors;
    },

    /*
     * Adds color to the list of colors used in the Legend
     *
     * @param color is a string representing a CSS color e.g. 'blue' or '#DADADA'
     */
    addUsedColor: function(color) {
        this.getUsedColors().push(color);
    },

    /*
     * Removes color from the list of colors used in the Legend
     *
     * @param color is a string representing a CSS color e.g. 'blue' or '#DADADA'
     */
    removeUsedColor: function(color) {
        this.setUsedColors(this.getUsedColors().without(color));
    }
});
