
/*
 * A general abstract class for nodes on the Pedigree graph. Contains information about
 * position on canvas and other neighboring nodes
 *
 * @param x the x coordinate on the canvas
 * @param x the y coordinate on the canvas
 */

var AbstractNode = Class.create( {

    initialize: function(x, y, id) {
        this._id = id ? id : editor.generateID();
        this._graphics = this.generateGraphics(x, y);
    },
    
    /*
     * Returns the unique ID of this node
     */
    getID: function() {
        return this._id;
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
     * Changes the X coordinate of the node
     *
     * @param x the x coordinate on the canvas
     * @param animate set to true if you want to animate, callback the transition
     * @param callback the function called at the end of the animation
     */
    setX: function(x, animate, callback) {
        this.getGraphics().setX(x, animate, callback)
    },

    /*
     * Returns the Y coordinate of the node on the canvas
     */
    getY: function() {
        return this.getGraphics().getY();
    },

    /*
     * Changes the Y coordinate of the node
     *
     * @param y the y coordinate on the canvas
     * @param animate set to true if you want to animate the transition
     * @param callback the function called at the end of the animation
     */
    setY: function(y, animate, callback) {
        this.getGraphics().setX(y, animate, callback)
    },

    /*
     * Returns an array containing the x and y coordinates of the node on canvas.
     */
    getPos: function() {
        return [this.getX(), this.getY()];
    },

    /*
     * Changes the position of the node to (X,Y)
     *
     * @param x the x coordinate on the canvas
     * @param y the y coordinate on the canvas
     * @param animate set to true if you want to animate the transition
     * @param callback the function called at the end of the animation
     */
    setPos: function(x,y, animate, callback) {
        this.getGraphics().setPos(x, y, animate, callback);
    },
    
    /**
     * Provides access to the type of the node
     * 
     * @return a string expressing the type
     */
    getType: function() {
        return "";
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
    remove: function(isRecursive) {
        var me = this;
        var toRemove = [];
        if(isRecursive) {
            toRemove.push(me);
            this.getNeighbors().each(function(neighbor) {
                var result = neighbor.isRelatedTo(editor.getProband(), toRemove.clone());
                if(!result[0]) {
                    toRemove = result[1];
                }
            });
            toRemove.each(function(node) {
                node.getGraphics().getHighlightBox && node.getGraphics().highlight();
            });
            var confirmation;
            if(toRemove.length > 1) {
                confirmation = confirm("Removing this person will also remove all the highlighted individuals. Are you sure you want to proceed?");
            }
            else {
                confirmation = confirm("Removing this person will also remove all the related connections. Are you sure you want to proceed?");
            }
            if(confirmation) {
                toRemove.reverse().each(function(node) {
                    node && node.remove(false);
                });
            }
            else {
                toRemove.each(function(node) {
                    node && node.getGraphics().getHighlightBox && node.getGraphics().unHighlight();
                });
            }
        }
    },

    isRelatedTo: function(node, visited) {
        var visitedNodes = (visited) ? visited : [];
        if(visitedNodes.indexOf(this) >= 0) {
            return [false, visitedNodes];
        }
        visitedNodes.push(this);
        if(node == this) {
            return [true, visitedNodes];
        }
        else {
            var found = false;
            var neighbors = this.getNeighbors();
            neighbors = neighbors.without.apply(neighbors, visitedNodes);
            neighbors.each(function(neighbor) {
                var result = neighbor.isRelatedTo(node, visitedNodes);
                visitedNodes = result[1];
                result[0] && (found = true);
            });
            return [found, visitedNodes];
        }
    }
});