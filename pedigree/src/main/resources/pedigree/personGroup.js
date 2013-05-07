/**
 * PersonGroup is node that represents a grouping of an unknown number of nodes ("n"). This type of
 * node is usually used to indicate the existence of relatives without providing any other information.
 * Therefore the options for this node are limited.
 *
 * @class PersonGroup
 * @constructor
 * @extends AbstractPerson
 * @param {Number} x The x coordinate on the Raphael canvas at which the node drawing will be centered
 * @param {Number} y The y coordinate on the Raphael canvas at which the node drawing will be centered
 * @param {String} gender Either 'M', 'F' or 'U' depending on the gender
 * @param {Number} id Unique ID number
 */

var PersonGroup = Class.create(AbstractPerson, {

    initialize: function($super, x, y, gender, id) {
        this._type = "PersonGroup";
        $super(x, y, gender, id);
        this._numPersons = 1;
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
        return new PersonGroupVisuals(this, x, y);
    },

    /**
     * Deletes this node, it's placeholder partners and children and optionally
     * removes all the other nodes that are unrelated to the proband node.
     *
     * @method remove
     * @param [$super]
     * @param {boolean} isRecursive Set to true if you want to remove related nodes that are
     * not connected to the proband
     * @param {boolean} skipConfirmation If true, will not display a confirmation pop-up
     */
    remove: function($super, isRecursive, skipConfirmation) {
        var parents = this.getParentPartnership();
        if(!isRecursive && parents && parents.getChildren().length == 1) {
            var placeholder = editor.getGraph().addPlaceHolder(this.getX(), this.getY(), "U");
            parents.removeChild(this);
            parents.addChild(placeholder);
        }
        return $super(isRecursive, skipConfirmation);
    },

    /**
     * Changes the number of people who are in this PersonGroup
     *
     * @method setNumPersons
     * @param {Number} numPersons The number of people in this grouping
     */
    setNumPersons: function(numPersons) {
        this._numPersons = numPersons;
        this.getGraphics().setNumPersons(numPersons);
    },

    /**
     * Returns the number of people who are in this PersonGroup
     *
     * @method getNumPersons
     * @return {Number}
     */
    getNumPersons: function() {
        return this._numPersons;
    },

    /**
     * Returns an object containing all the information about this node.
     *
     * @method getInfo
     * @return {Object} in the form
     *
     {
     type: // (type of the node),
     x:  // (x coordinate)
     y:  // (y coordinate)
     id: // id of the node
     gender: //gender of the node
     numPersons: //number of people in this grouping
     }
     */
    getInfo: function($super) {
        var info = $super();
        info['numPersons'] = this.getNumPersons();
        return info;
    },

    /**
     * Applies the properties found in info to this node.
     *
     * @method loadInfo
     * @param [$super]
     * @param info Object in the form
     *
     {
     type: // (type of the node),
     x:  // (x coordinate)
     y:  // (y coordinate)
     id: // id of the node
     gender: //gender of the node
     numPersons: //number of people in this grouping
     }
     * @return {Boolean} true if info was successfully loaded
     */
    loadInfo: function($super, info) {
        if($super(info) && info.numPersons) {
            if(this.getNumPersons() != info.numPersons) {
                this.setNumPersons(info.numPersons);
            }
            return true;
        }
        return false;
    }
});