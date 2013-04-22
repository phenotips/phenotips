/*
 * PersonGroup is node that represents a grouping of an unknown number of nodes ("n"). This type of
 * node is usually used to indicate the existence of relatives without providing any other information.
 * Therefore the options for this node are limited.
 *
 * @param x the x coordinate on the Raphael canvas at which the node drawing will be centered
 * @param the y coordinate on the Raphael canvas at which the node drawing will be centered
 * @param gender either 'M', 'F' or 'U' depending on the gender
 * @param id a unique ID number
 */

var PersonGroup = Class.create(AbstractPerson, {

    initialize: function($super, x, y, gender, id) {
        this._type = "PersonGroup";
        $super(x, y, gender, id);
        this._numPersons = 1;
    },

    /*
     * Initializes the object responsible for creating graphics for this PlaceHolder
     *
     * @param x the x coordinate on the canvas at which the node is centered
     * @param y the y coordinate on the canvas at which the node is centered
     */
    generateGraphics: function(x, y) {
        return new PersonGroupVisuals(this, x, y);
    },

    /*
     * Deletes this node, it's placeholder partners and children and optionally
     * removes all the other nodes that are unrelated to the proband node.
     *
     * @param isRecursive set to true if you want to remove related nodes that are
     * not connected to the proband
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

    setNumPersons: function(numPersons) {
        this._numPersons = numPersons;
        this.getGraphics().setNumPersons(numPersons);
    },

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
     * @param info {Object} and object in the form
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