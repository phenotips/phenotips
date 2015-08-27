/**
 * Personplaceholder represents a non-existing or unknown person
 *
 * @class PersonPlaceholder
 * @constructor
 * @extends AbstractPerson
 */
 define([
        "pedigree/view/person",
        "pedigree/view/personPlaceholderVisuals"
    ], function(
        Person,
        PersonPlaceholderVisuals
    ){
    var PersonPlaceholder = Class.create(Person, {

        initialize: function($super, x, y, id, properties) {
            this._comment    = "";
            this._type       = "PersonPlaceholder";
            $super(x, y, id, {"gender": "U"});
        },

        /**
         * Initializes the object responsible for creating graphics for this PersonGroup
         *
         * @method _generateGraphics
         * @param {Number} x The x coordinate of hte PersonGroup Node
         * @param {Number} y The y coordinate of hte PersonGroup Node
         * @return {PersonGroupVisuals}
         */
        _generateGraphics: function(x, y) {
            return new PersonPlaceholderVisuals(this, x, y);
        },

        /**
         * Always returns False - proband is never a placehodler
         *
         * @method isProband
         */
        isProband: function() {
            return false;
        },

        /**
         * Returns an object containing all the properties of this node
         * except id, x, y & type
         *
         * @method getProperties
         * @return {Object} in the form
         *
         {
           property: value
         }
         */
        getProperties: function($super) {
            //var info = $super();
            var info = {};
            if (this.getComments() != "")
                info['comments'] = this.getComments();
            info['placeholder'] = true;
            return info;
        },

        /**
         * Applies the properties found in info to this node.
         *
         * @method loadProperties
         * @param properties Object
         * @return {Boolean} True if info was successfully assigned
         */
        assignProperties: function($super, info) {
            return $super(info)
        },

        /**
         * Returns an object (to be accepted by the menu) with information about this Node
         *
         * @method getSummary
         * @return {Object} Summary object for the menu
         */
        getSummary: function() {

            return {
                type: {value : "placeholder"}
            };
        }
    });
    return PersonPlaceholder;
});