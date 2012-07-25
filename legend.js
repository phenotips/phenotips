var Legend = Class.create( {


/*
 * Class responsible for keeping track of disorders on canvas and their properties.
 * This information is graphically displayed in a 'Legend' box
 */
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
     * @param disorder an object with fields 'id' and 'value', where id is the id number
     * for the disorder, taken from the OMIM database, and 'value' is the name of the disorder.
     * eg. {id: 33244, value: 'Down Syndrome'}
     */
    addCase: function(disorder) {
        if (!this.containsDisorder(disorder['id'])) {
            this.setDisorder(disorder['id'], new Disorder(disorder['id'], disorder['value'], null, 0));
            this.addUsedColor(this.getDisorder(disorder['id']).getColor());
            //TODO: this.buildLegend
        }
            this.getDisorder(disorder['id']).incrementNumAffected();
    },

    /*
     * Removes an occurrence of a disorder if there are any. Removes the disorder
     * from the 'Legend' box if this disorder is not registered in any individual.
     *
     * @param disorder an object with fields 'id' and 'value', where id is the id number
     * for the disorder, taken from the OMIM database, and 'value' is the name of the disorder.
     * eg. {id: 33244, value: 'Down Syndrome'}
     */
    removeCase: function(disorder) {
        if (this.containsDisorder(disorder['id'])) {
            this.getDisorder(disorder['id']).decrementNumAffected();
            if(this.getDisorder(disorder['id']).getNumAffected() < 1) {
                delete this.getDisorder(disorder['id']);
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
