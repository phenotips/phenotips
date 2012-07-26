
/**
 * The graphical functionality of a pedigreeNode
 */

var NodeVisuals = Class.create( {

    initialize: function(canvas) {
        this._radius = 50;
        this._paper = canvas;
        this._patterns = ["black", "url(patterns/1.jpg)", "url(patterns/circles.jpg)", "url(patterns/deltoyds.jpg)",
            "url(patterns/lines.gif)", "url(patterns/pluses.jpg)", "url(patterns/triangles.jpg)",
            "url(patterns/zebra.jpg)"];
        this._attributes = {
            //TODO: fix fetusShape attributes
            fetusShape: {'font-size': 50, 'font-family': 'Cambria'},
            nodeShape: {fill: "0-#ffffff:0-#B8B8B8:100", stroke: "#595959"},
            boxOnHover : {fill: "gray", stroke: "none",opacity: 1, "fill-opacity":.25},
            //boxOnHover : {fill: "gray", stroke: "#8F8F8F",opacity: 1, "fill-opacity":.1},
            optionsBtnIcon : {fill: "#1F1F1F", stroke: "none"},
            optionsBtnMaskHoverOn : {opacity:.6, stroke: 'none'},
            optionsBtnMaskHoverOff : {opacity:0},
            optionsBtnMaskClick: {opacity:1},
            orbHue : .53,
            orbRadius: this._radius/8,
            phShape: {fill: "white","fill-opacity": 0, "stroke": 'black', "stroke-dasharray": "- "},
            dragMeLabel: {'font-size': 14, 'font-family': 'Tahoma'},
            label: {'font-size': 18, 'font-family': 'Cambria'},
            disorderShapes: {}
        };
    },

    getRadius: function() {
        return this._radius;
    },


    /*
     * Draws and hides square brackets around the 'node' based on the value of 'isAdopted'
     * @param node an instance of Person
     * @param isAdopted is a boolean value that specifies whether the node is adopted
     */


//    setSB: function(node) {
//        if(node._isSB)
//        {
//            var text = editor.paper.text(node._xPos, node._yPos, "SB");
//            text.type = "SBLabel";
//            node._labels = this.addToBeginning(text, node._labels);
//        }
//        else
//        {
//            node._labels[0].remove();
//            node._labels = this.popFirst(node._labels);
//        }
//        this.repositionLabels(node);
//    },
//
//



    generateOrb: function (x, y, r, hue) {
        hue = hue || 0;
        return this._paper.set(
            this._paper.ellipse(x, y, r, r).attr({fill: "r(.5,.9)hsb(" + hue + ", 1, .75)-hsb(" + hue + ", .5, .25)", stroke: "none"}),
            this._paper.ellipse(x, y, r - r / 5, r - r / 20).attr({stroke: "none", fill: "r(.5,.1)#ccc-#ccc", opacity: 0})
        );
    },
});