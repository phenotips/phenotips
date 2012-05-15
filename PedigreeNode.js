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

        /**
         * Constructor for PedigreeNode
         * @param canvas the drawing area
         * @param xPosition the x coordinate for the object
         * @param yPosition the y coordinate for the object
         */
        initialize: function(canvas, xPosition, yPosition, diseaseMap, radius) {
            this._radius = radius;
            this._diseases = [];
            this._diseaseShapes = [];
            this._xPos = xPosition;
            this._yPos = yPosition;
            this._paper = canvas;
            this._diseaseMap = diseaseMap;
            this._shape = this._paper.rect(xPosition-40,yPosition-40,80,80).attr({fill: "white", stroke: "black"}).rotate(45);

        },

        /**
         * Replaces the gender of the PedigreeNode and displays the
         * appropriate shape on the graph
         * @param: gender can be either 'male', 'female' or 'unknown'.
         */
        setGender: function(gender) {
            if(gender == 'male' || gender == 'female' || gender == 'unknown')
            {
                this._gender = gender;
                this._shape.remove();
                if(gender == 'male')
                {
                    this._shape = this._paper.rect(this._xPos-40,this._yPos-40,80,80).attr({fill: "white", stroke: "black"});
                }
                else if(gender == 'female')
                {
                    this._shape =  this._paper.circle(this._xPos,this._yPos,40).attr({fill: "white", stroke: "black"});
                }
                else
                {
                    this._shape = this._paper.rect(this._xPos-40,this._yPos-40,80,80).attr({fill: "white", stroke: "black"}).rotate(-45);
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
            if(isDead === true)
            {
                this._isDead = isDead;
                if(this._deadShape == null)
                {
                    this._deadShape = this._paper.rect(this._xPos-2, this._yPos-70, 4, 140).attr({fill: "black"}).rotate(45);
                }
                else
                {
                    this._deadShape.show();
                }
            }
            else if(isDead === false)
            {
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
        addDisease: function(diseaseName) {
            if(this._diseaseMap.get(diseaseName) == null) {
                var letters = '0123456789ABCDEF'.split('');
                var color = '#';
                for (var i = 0; i < 6; i++ ) {
                    color += letters[Math.round(Math.random() * 15)];
                }
                this._diseaseMap.put("diseaseName", color);
            }
            var diseaseShape = sector(paper, this._xPos, this._yPos, this._radius, this._gender, 0, 45, "blue");
            this._diseaseShapes.push(diseaseShape);
            this._diseases.push(diseaseName);
            this.draw();
        },

        /**
         * Displays all the graphical elements of this PedigreeNode on the canvas
         * in the right layering order
         */
        draw: function() {
            var toDraw = [this._shape, this._diseaseShapes, this._deadShape, this._ageLabel].flatten();
            toDraw.each(function(s){
                (s && s.show() && s.toFront());
            });
        },
        /**
         * Hides the graphical elements of the PedigreeNode
         */
        tempRemove: function() {
            var toRemove = [this._shape, this._diseaseShapes, this._deadShape, this._ageLabel].flatten();
            toRemove.each(function(s){
                (s && s.hide());
            });
        },

        /**
         * Removes the properties and graphical elements of the PedigreeNode
         */
        remove: function() {
            var toRemove = [this._shape, this._diseaseShapes, this._deadShape, this._ageLabel].flatten();
            toRemove.each(function(s){
                (s && s.remove());
                s = null;
            });
        }
    });

    //Generate a map of diseases and colors
    var diseaseMap = new Map();
    diseaseMap.put("down syndrome", "red");

    //Generate canvas
    var paper = Raphael("canvas", 640, 480);
    (createCanvas = function () {
        paper.clear();
        paper.rect(0, 0, 640, 480, 10).attr({fill: "#fff", stroke: "none"});
    })();

    //Put main patient on canvas
    var patientNode = new PedigreeNode(paper, 320, 240, diseaseMap, 40);
    patientNode.draw();
    patientNode.setGender('male');
    patientNode.setAge(10, 6, 2011);
    patientNode.setDead(true);
    patientNode.setGender('unknown');
    patientNode.tempRemove();
    patientNode.draw();
    patientNode.addDisease("aaa");
    //patientNode.remove();
    //patientNode.draw();

    var anotherNode = new PedigreeNode(paper, 450, 240, diseaseMap, 40);
    anotherNode.draw();
    anotherNode.setGender('male');
    anotherNode.setAge(10, 6, 2011);
    anotherNode.setDead(true);
    anotherNode.setGender('male');
    anotherNode.tempRemove();
    anotherNode.draw();
    anotherNode.addDisease("bbb");
});