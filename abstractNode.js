
/*
 * A general superclass for nodes on the Pedigree graph. Contains connections
 * and basic information about gender, ID and a graphics element.
 */

var AbstractNode = Class.create( {

    initialize: function(x, y) {
        this._graphics = this.generateGraphics(x, y);
    },

    generateGraphics: function(x, y) {
        return new AbstractNodeVisuals(this, x, y);
    },

    /*
     * Returns the object responsible for creating graphics
     */
    getGraphics: function() {
        return this._graphics;
    },

    getX: function() {
        return this.getGraphics().getAbsX();
    },

    setX: function(x) {
        this.getGraphics().setAbsX(x)
    },

    getY: function() {
        return this.getGraphics().getAbsY();
    },

    setY: function(y) {
        this.getGraphics().setAbsX(y)
    },

    getPos: function() {
        return [this.getX(), this.getY()];
    },

    moveTo: function(x,y) {
        this.getGraphics().moveTo(x,y);
    },

    moveToX: function(x) {
        this.moveTo(x, this.getY());
    },

    moveToY: function(y) {
        this.moveTo(this.getX(), y);
    },

    getNeighbors: function() {
        return (this.getLowerNeighbors().concat(this.getSideNeighbors())).concat(this.getUpperNeighbors());
    },

    getLowerNeighbors: function() {
        return [];
    },

    getSideNeighbors: function() {
        return [];
    },

    getUpperNeighbors: function() {
        return [];
    },

    remove: function(removeVisuals) {
        removeVisuals && this.getGraphics().remove();
    }
});