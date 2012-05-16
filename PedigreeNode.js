document.observe("dom:loaded",function() {
    /**
     * A Pedigree node on the canvas
     */
    var PedigreeNode = Class.create( {
        _xPos: null,
        _yPos: null,
        _gender: 'unknown',
        //father: null,
        //mother: null,
        //siblings: [],
        //connections: [],
        _paper: null,
        _age: null,
        _ageLabel: null,
        _isDead: false,
        _shape: null,
        _deadShape: null,
        _diseaseMap: null,
        _radius: 40,
        _patterns: ["black", "url(patterns/1.jpg)", "url(patterns/circles.jpg)", "url(patterns/deltoyds.jpg)",
        "url(patterns/lines.gif)", "url(patterns/pluses.jpg)", "url(patterns/triangles.jpg)",
        "url(patterns/zebra.jpg)"],

        /**
         * Constructor for PedigreeNode
         * @param canvas the drawing area
         * @param xPosition the x coordinate for the object
         * @param yPosition the y coordinate for the object
         */
        initialize: function(canvas, xPosition, yPosition, diseaseMap, radius) {
            this._adoptedShape = null;
            this._adopted = false;
            this._radius = radius;
            this._diseases = {};
            this._xPos = xPosition;
            this._yPos = yPosition;
            this._paper = canvas;
            this._diseaseMap = diseaseMap;
            this._shape = this._paper.rect(xPosition-40,yPosition-40,80,80).attr(
                {fill: "white", stroke: "black"}).rotate(45);

        },

        /**
         * Replaces the gender of the PedigreeNode and displays the
         * appropriate shape on the graph
         * @param: gender can be either 'male', 'female' or 'unknown'.
         */
        setGender: function(gender) {
            if (gender == 'male' || gender == 'female' || gender == 'unknown')
            {
                var attributes = {fill: "white", stroke: "black"};
                this._gender = gender;
                this._shape.remove();

                if (gender == 'male') {
                    this._shape = this._paper.rect(this._xPos-40,this._yPos-40,80,80).attr(attributes);
                }
                else if (gender == 'female') {
                    this._shape =  this._paper.circle(this._xPos,this._yPos,40).attr(attributes);
                }
                else {
                    this._shape = this._paper.rect(this._xPos-40,this._yPos-40,80,80).attr(attributes).rotate(-45);
                }

                this.draw();
            }
        },

        /**
         * Sets the isDead property of the PedigreeNode to the value
         * passed in the isDead parameter
         * @param isDead boolean value indicating whether the PedigreeNode is dead or not
         */
        setDead: function(isDead) {
            if (isDead === true) {
                this._isDead = isDead;
                if (this._deadShape == null) {
                    this._deadShape = this._paper.rect(this._xPos-2, this._yPos-70, 4, 140).attr(
                        {fill: "black"}).rotate(45);
                }
                else  {
                    this._deadShape.show();
                }
            }
            else if (isDead === false) {
                this._isDead = isDead;
                (this._deadShape && this._deadShape.hide());
            }
        },

        /**
         * Replaces the age property of the PedigreeNode and
         * displays the appropriate label on the graph
         * @param: birthYear is a string in the format '56 y', '4 mo' or 'b. 1988'
         */
        setAge: function(birthDay, birthMonth, birthYear) {
            var birthDate =new Date(birthYear, birthMonth-1, birthDay);
            this._age = getAge(birthDate);
            this._ageLabel = this._paper.text(this._xPos, this._yPos + 70, this._age);
            this._ageLabel.attr({'font-size': 15, 'font-family': 'Cambria'});
        },

        /**
         * Assigns diseaseName to this pedigreeNode and displays it in the right
         * proportion with the other diseases. Uses the color/pattern listed in
         * _diseaseMap
         * @param: diseaseName a string representing the name of the disease.
         */
        addDisease: function(diseaseName) {
            if (!this._diseases[diseaseName]) {
                var numDiseases = Object.keys(this._diseases).length + 1,
                    diseaseAngle = (360/numDiseases).round(),
                    pattern;

                Object.values(this._diseases).each(function(s) {
                    s.remove();
                });

                if (Object.keys(this._diseaseMap).indexOf(diseaseName) == -1) {
                    var usedPatterns = Object.values(this._diseaseMap);
                    var i = 0;
                    while (i<this._patterns.length && (usedPatterns.indexOf(this._patterns[i]) != -1)) {
                        i++;
                    }
                    pattern = i < this._patterns.length ? this._patterns[i]:"blue";
                    this._diseaseMap[diseaseName] = pattern;
                }
                else {
                    pattern = this._diseaseMap[diseaseName];
                }
                this._diseases[diseaseName] = null;
                var map = this._diseaseMap,
                    paper = this._paper,
                    x = this._xPos,
                    y = this._yPos,
                    r = this._radius,
                    gen = this._gender,
                    diseases = this._diseases,
                    i = 0;

                Object.keys(this._diseases).each(function(s) {
                    diseases[s] = sector(paper, x, y, r, gen, i * diseaseAngle, (++i) * diseaseAngle, map[s]);
                });
                this.draw();
            }
        },

        /**
         * Changes the '_adopted' property of pedigreeNode, and displays
         * the graphical representation
         */
        setAdopted: function(isAdopted) {
            if (isAdopted === true)
            {
                this._adopted = isAdopted;
                if (this._adoptedShape == null) {
                    var x1 = this._xPos - ((3.5/3) * this._radius),
                        x2 = this._xPos + ((3.5/3) * this._radius),
                         y = this._yPos - ((3/2) * this._radius),
                         brackets = "M" + x1 + " " + y + "l" + (this._radius)/(-2) +
                                    " " + 0 + "l0 " + (3 * this._radius) + "l" + (this._radius)/2 + " 0M" + x2 +
                                    " " + y + "l" + (this._radius)/2 + " 0" + "l0 " + (3 * this._radius) + "l" +
                                     (this._radius)/(-2) + " 0";

                    this._adoptedShape = paper.path(brackets).attr("stroke-width", 3);
                }
                else {
                    this._adoptedShape.show();
                }
            }
            else if (isAdopted === false) {
                this._adopted = isAdopted;
               (this._adoptedShape && this._adoptedShape.hide());
            }
        },

        /**
         * Displays all the graphical elements of this PedigreeNode on the canvas
         * in the right layering order
         */
        draw: function() {
            var toDraw = [this._shape, Object.values(this._diseases), this._deadShape, this._ageLabel,
                this._adoptedShape].flatten();

            toDraw.each(function(s){
                (s && s.toFront());
            });
        },

        /**
         * Hides the graphical elements of the PedigreeNode
         */
        hideNode: function() {
            var toHide = [this._shape, Object.values(this._diseases), this._deadShape, this._ageLabel,
                this._adoptedShape].flatten();

            toHide.each(function(s){
                (s && s.hide());
            });
        },

        /**
         * displays hidden graphical elements of the PedigreeNode
         */
        unhideNode: function() {
            var toUnhide = [this._shape, Object.values(this._diseases), this._deadShape, this._ageLabel,
                this._adoptedShape].flatten();

            toUnhide.each(function(s){
                (s && s.show());
            });
        },

        /**
         * Removes the properties and graphical elements of the PedigreeNode
         */
        remove: function() {
            var toRemove = [this._shape, Object.values(this._diseases), this._deadShape, this._ageLabel,
                this._adoptedShape].flatten();

            toRemove.each(function(s){
                (s && s.remove());
                s = null;
            });
        }
    });

    //Generate a map of diseases and patterns
    var diseaseMap = {};

    //Generate canvas
    var paper = Raphael("canvas", 640, 480);

    (createCanvas = function () {
        paper.clear();
        paper.rect(0, 0, 640, 480, 10).attr({fill: "#fff", stroke: "none"});
//        document.documentElement.down('defs').insert(new Element('pattern', {
//            id : "TrianglePattern",
//            patternUnits: "userSpaceOnUse",
//            x: "0",
//            y: "0",
//            width: "100",
//            height: "100",
//            viewBox: "0 0 10 10"
//        })
//            .insert(new Element('path', {
//            d: "M 0 0 L 7 0 L 3.5 7 z",
//            fill: "red",
//            stroke: "blue"
//        })));
    })();

    //paper.circle(100,100,20).attr("fill", "url(patterns/checker.jpg)");

    //Put main patient on canvas
    var patientNode = new PedigreeNode(paper, 320, 240, diseaseMap, 40);
    patientNode.draw();
    patientNode.setGender('male');
    patientNode.setAge(10, 6, 2011);
    patientNode.setDead(true);
    patientNode.setDead(false);
    patientNode.draw();
    patientNode.setGender('male');
    //patientNode.tempRemove();
    patientNode.draw();
    patientNode.addDisease("Down Syndrome");
    patientNode.addDisease("Up Syndrome");
    patientNode.addDisease("Left Syndrome");
    //patientNode.setAdopted("true");
    //patientNode.remove();
    //patientNode.draw();

    //    var anotherNode = new PedigreeNode(paper, 450, 240, diseaseMap, 40);
    //    anotherNode.draw();
    //    anotherNode.setGender('male');
    //    anotherNode.setAge(10, 6, 2011);
    //    anotherNode.setDead(true);
    //    anotherNode.setGender('male');
    //    anotherNode.tempRemove();
    //    anotherNode.draw();
    //    anotherNode.addDisease("bbb");


});