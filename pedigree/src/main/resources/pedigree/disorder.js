/*
 * Disorder is a class for managing visual representation and information regarding any
 * genetic disorder that is found in the OMIM database, and that can be attributed to
 * an individual in the Pedigree.
 *
 * @param disorderID the id number for the disorder, taken from the OMIM database
 * @param name a string representing the name of the disorder e.g. "Down Syndrome"
 */

var Disorder = Class.create( {

    initialize: function(disorderID, name, color, affectedNodes) {
        this._disorderID = disorderID;
        this._name = name;
    },

    /*
     * Returns the disorderID of the disorder
     */
    getDisorderID: function() {
        return this._disorderID;
    },

    /*
     * Replaces the ID of the disorder with the disorder ID passed. Does not update Legend!
     *
     * @param disorderID the id number for the disorder, taken from the OMIM database
     */
    setDisorderID: function(disorderID) {
        this._disorderID = disorderID;
    },

    /*
     * Returns the name of the disorder
     */
    getName: function() {
        return this._name;
    },

    /*
     * Replaces the name of the disorder with the name passed. Does not update Legend!
     *
     * @param name a string representing the name of the disorder e.g. "Down Syndrome"
     */
    setName: function(name) {
        this._name = name;
    },

    /*
     * Returns the number of registered individuals carrying the disorder.
     */
    //getNumAffected: function() {
    //    return this.getAffectedNodes().length;
    //}
});
