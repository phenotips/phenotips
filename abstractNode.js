
/*
 * A general abstract class for nodes on the Pedigree graph. Contains information about
 * position on canvas and other neighboring nodes
 *
 * @param x the x coordinate on the canvas
 * @param x the y coordinate on the canvas
 */

var AbstractNode = Class.create( {

    initialize: function(x, y) {
        this._graphics = this.generateGraphics(x, y);
    },

    /*
     * Generates and returns an instance of AbstractNodeVisuals
     */
    generateGraphics: function(x, y) {
        return new AbstractNodeVisuals(this, x, y);
    },

    /*
     * Returns the object responsible for managing graphics
     */
    getGraphics: function() {
        return this._graphics;
    },

    /*
     * Returns the X coordinate of the node on the canvas
     */
    getX: function() {
        return this.getGraphics().getX();
    },

    /*
     * Transitions the node along the x axis to the x coordinate passed in the parameter
     *
     * @x the x coordinate on the canvas
     */
    setX: function(x) {
        this.getGraphics().setX(x)
    },

    /*
     * Returns the Y coordinate of the node on the canvas
     */
    getY: function() {
        return this.getGraphics().getY();
    },

    /*
     * Transitions the node along the y axis to the y coordinate passed in the parameter
     *
     * @y the y coordinate on the canvas
     */
    setY: function(y) {
        this.getGraphics().setX(y)
    },

    /*
     * Returns an array containing the x and y coordinates of the node on canvas.
     */
    getPos: function() {
        return [this.getX(), this.getY()];
    },

    /*
     * Transitions the node to the coordinate (x,y)
     *
     * @x the x coordinate
     * @y the y coordinate
     */
    setPos: function(x,y) {
        this.getGraphics().setPos(x, y);
    },

    /*
     * Returns an array of all adjacent nodes (neighbors).
     */
    getNeighbors: function() {
        return (this.getLowerNeighbors().concat(this.getSideNeighbors())).concat(this.getUpperNeighbors());
    },

    /*
     * Returns an array of all adjacent nodes (neighbors) located below this node.
     */
    getLowerNeighbors: function() {
        return [];
    },

    /*
     * Returns an array of all adjacent nodes (neighbors) located on the sides of this node.
     */
    getSideNeighbors: function() {
        return [];
    },

    /*
     * Returns an array of all adjacent nodes (neighbors) located above this node.
     */
    getUpperNeighbors: function() {
        return [];
    },

    /*
     * Removes all connections with neighboring nodes and (optionally) removes
     * all the graphics of this node.
     *
     * @param removeVisuals set to true if you want to remove the graphics of this
     * node as well.
     */
    remove: function(removeVisuals) {
        removeVisuals && this.getGraphics().remove();
    }
});