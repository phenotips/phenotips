var PedigreeEditor = Class.create({
    initialize: function() {
        //create canvas
        var screenDimensions = document.viewport.getDimensions(),
            width = screenDimensions.width,
            height = screenDimensions.height,
            paper = this._paper = Raphael(0,0, width, height);
            this._nodes = [];
            paper.clear();

        //TODO: capture resize events

        //Generate a map of Disorders and patterns
        disorderMap = {};
        this._disorderMap = disorderMap;
        var graphics = new NodeVisuals(paper);
        var patientNode = new PedigreeNode(width/2, height/2, graphics, 'female');
        this._nodes.push(patientNode);

       // var anotherNode = new PedigreeNode(width/2 + patientNode._graphics._radius * 4, height/2, graphics, 'female');

       this.experiment(patientNode);
    },
    /**
     * Function for testing out features of a node.
     */
    experiment: function(patientNode) {
        patientNode.setAge(10, 6, 2011);
        patientNode.setDead(true);
        //patientNode.setGender('male');
        patientNode.addDisorder("Down Syndrome");
        //patientNode.addDisorder("Up Syndrome");
        patientNode.setAdopted(true);
        patientNode.setGender('male');
        //patientNode.removeDisorder("Down Syndrome");
        //patientNode.addDisorder("Down Syndrome");
        patientNode.setGender('female');
        var nodeElement = patientNode._graphics.draw(patientNode);
        //alert(nodeElement.transform());

        var ani = function() {

            patientNode._hoverBox.disable();
            patientNode._xPos += 100;
            patientNode._yPos += 100;
            nodeElement.stop().animate({'transform': "t " + 100 + "," + 100+"..."}, 2000, "linear", patientNode._hoverBox.enable.bind(patientNode._hoverBox));

        };
    }
});

var editor,
    disorderMap;

document.observe("dom:loaded",function() {

editor = new PedigreeEditor();
});