
/**
 * The graphical functionality of a pedigreeNode
 */

var NodeVisuals = Class.create( {

    initialize: function(canvas) {
        this._radius = 40;
        this._paper = canvas;
        this._patterns = ["black", "url(patterns/1.jpg)", "url(patterns/circles.jpg)", "url(patterns/deltoyds.jpg)",
            "url(patterns/lines.gif)", "url(patterns/pluses.jpg)", "url(patterns/triangles.jpg)",
            "url(patterns/zebra.jpg)"];
    },
    /*
     * Replaces the gender shape of 'node' with the appropriate shape for the specified gender.
     * Updates the disorders to follow the same gender shape.
     * @param node an instance of PedigreeNode
     * @param gender a string that can be either "male", "female" or "unknown"
     */
    setGender: function(node, gender) {
        var attributes = {fill: "white", stroke: "black"};
        node._shape && node._shape.remove();

        if (gender == 'male') {
            node._genderShape = this._paper.rect(node._xPos-this._radius,node._yPos-this._radius,this._radius * 2, this._radius * 2).attr(attributes);
        }
        else if (gender == 'female') {
            node._genderShape =  this._paper.circle(node._xPos,node._yPos,this._radius).attr(attributes);
        }
        else {
            node._genderShape = this._paper.rect(node._xPos-this._radius,node._yPos-this._radius, this._radius * 2, this._radius * 2).attr(attributes).rotate(-45);
        }
        node._shape = this._paper.set().push(node._genderShape.transform("..."));
        this.generateDisorderShapes(node);
    },

    /*
     * Maps the disorder shapes of the 'node'
     * @param node an instance of PedigreeNode
     */
    generateDisorderShapes: function(node)
    {
        var paper = this._paper,
            x = node._xPos,
            y = node._yPos,
            r = this._radius,
            gender = node._gender,
            map = disorderMap,
            disorderAngle = (node._disorders.length == 0)?0:(360/node._disorders.length).round();

        var mainShape = node._shape[0];
        for(var i = 1; i<node._shape.length; i++)
        {
            node._shape[i].remove();
        }
        node._shape.clear();
        node._shape.push(mainShape);

        for(i = 0; i<node._disorders.length; i++)
        {
            node._shape.push(sector(paper, x, y, r, gender, (i) * disorderAngle, (i+1) * disorderAngle,
                map[node._disorders[i]].color));
        }
        this.draw(node);
    },

    /*
     * Adds a new disorder shape to the node and coordinates the colors and patterns with disorderMap
     * @param node an instance of PedigreeNode
     * @param disorderName a string with the name of the disorder
     */
    addDisorder: function(node, disorderName) {
        if (Object.keys(disorderMap).indexOf(disorderName) == -1)
        {
            var randomColor = "#______" .replace(/_/g,function(){return (~~(Math.random()*14+2)).toString(16);});
            while(randomColor == "#ffffff")
            {
                randomColor = "#"+((1<<24)*Math.random()|0).toString(16);
            }
            disorderMap[disorderName] = {color: randomColor,
                                         numAffectedNodes: 0 }
        }
        this.generateDisorderShapes(node);
    },

    /*
     * Updates disorder shapes after a disorder was removed from the node
     * @param node an instance of PedigreeNode
     */
    removeDisorder: function(node) {
            this.generateDisorderShapes(node);
    },

    /*
     * Draws and hides square brackets around the 'node' based on the value of 'isAdopted'
     * @param node an instance of PedigreeNode
     * @param isAdopted is a boolean value that specifies whether the node is adopted
     */
    setAdopted: function(node, isAdopted) {
        if (isAdopted)
        {
            if (node._adoptedShape == null) {
                var x1 = node._xPos - ((0.8) * this._radius),
                    x2 = node._xPos + ((0.8) * this._radius),
                    y = node._yPos - ((1.3) * this._radius),
                    brackets = "M" + x1 + " " + y + "l" + (this._radius)/(-2) +
                        " " + 0 + "l0 " + (2.6 * this._radius) + "l" + (this._radius)/2 + " 0M" + x2 +
                        " " + y + "l" + (this._radius)/2 + " 0" + "l0 " + (2.6 * this._radius) + "l" +
                        (this._radius)/(-2) + " 0";
                node._adoptedShape = this._paper.path(brackets).attr("stroke-width", 3);
            }
            else {
                node._adoptedShape.show();
            }
        }
        else {
            (node._adoptedShape && node._adoptedShape.hide());
        }
        this.draw(node)
    },

    /*
     * Draws or hides a diagonal line on top of the 'node', based on the value of isDead
     * @param node an instance of PedigreeNode
     * @param isDead is a boolean value that specifies whether the node is dead
     */
    setDead: function(node, isDead) {
        if (isDead) {
            if (node._deadShape == null) {
                node._deadShape = this._paper.rect(node._xPos-2, node._yPos-70, 4, 140).attr(
                    {fill: "black"}).rotate(45);
            }
            else  {
                node._deadShape.show();
            }
        }
        else {
            (node._deadShape && node._deadShape.hide());
        }
        this.draw(node);
    },

    /*
     * Updates the label with the node's age
     * @param node an instance of PedigreeNode
     */
    setAge: function(node) {
        node._ageLabel && node._ageLabel.remove();
        node._ageLabel = this._paper.text(node._xPos, node._yPos + 70, node._age);
        node._ageLabel.attr({'font-size': 15, 'font-family': 'Cambria'});
        this.draw(node);
    },

    /**
     * Displays all the graphical elements of the specified node on the canvas
     * in the right layering order
     * @param node an instance of PedigreeNode
     */
    draw: function(node) {
        var toDraw = [node._hoverBox._backElements, node._shape, node._deadShape, node._ageLabel,
            node._adoptedShape, node._hoverBox._frontElements].flatten(),
            nodeSet = this._paper.set();

        toDraw.each(function(s){
            (s && s.toFront());
            s&&nodeSet.push(s);
        });
        return nodeSet;
    },

    /*
     * Removes all graphical elements related to the specified node from the canvas
     * @param node an instance of PedigreeNode
     */
    remove: function(node) {
        var toRemove = [node._hoverBox._backElements, node._shape, node._deadShape, node._ageLabel,
            node._adoptedShape, node._hoverBox._frontElements].flatten();

        toRemove.each(function(s){
            (s && s.remove());
        });
    }
});