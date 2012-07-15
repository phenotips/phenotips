    /**
     * A Pedigree node on the canvas
     */

    var PedigreeNode = Class.create(AbstractNode, {

        /**
         * Constructor for PedigreeNode
         * @param x the x coordinate for the object
         * @param y the y coordinate for the object
         * @param gender is a string that's either 'male', 'female' or 'unknown'
         */
        initialize: function($super, x, y, gender) {
            $super(x,y,gender);
            this._type = "pn";
            this._birthDate = null;
            this._deathDate = null;
            this._isDead = false;
            this._isSB = false;
            this._adopted = false;
            this._ageLabelText = null;
            this._disorders = [];
            this._labels = editor.paper.set();
            this._propertyShapes = editor.paper.set();
            this._hoverBox = new Hoverbox(this);
            this.setGender(this._gender);
        },
        getBirthDate: function() {
            return this._birthDate;
        },

        getDeathDate: function() {
            return this._deathDate;
        },

        setDeathDate: function(deathDate) {
            this._deathDate =  deathDate;
            if(deathDate && this.getBirthDate()) {
                this.ageLabelText = "d. " + getAge(this.getBirthDate(), deathDate);
                this.setDead(true);
            }
            else if (deathDate) {
                this.ageLabelText = "d. " + deathDate.getFullYear();
                this.setDead(true);
            }
            this.updateAgeLabel();


        },

        getHoverBox: function() {
            return this._hoverBox;
        },

        setGender: function($super, gender) {
            $super(gender);
            this.getDrawing().push(this.generateDisorderShapes());
        },

        getDisorders: function() {
            return this._disorders;
        },

        setDisorders: function(disorderArray) {
            this._disorders = disorderArray;
        },

        generateDisorderShapes: function() {
            var disorderAngle = (this.getDisorders().length == 0)?0:(360/this.getDisorders().length).round();
            var disorderShapes = editor.paper.set();

            this.getDrawing()[1] && this.getDrawing()[1].remove();

            for(var i = 0; i<this.getDisorders().length; i++) {
                disorderShapes.push(sector(editor.paper, this.getX(), this.getY(), editor.graphics.getRadius(),
                    this.getGender(), (i) * disorderAngle, (i+1) * disorderAngle, disorderMap[this.getDisorders()[i]].color));
            }

            this.getDrawing()[1] = disorderShapes;
            this.draw();
        },

        getPropertyShapes: function() {
            return this._propertyShapes;
        },

        getLabels: function() {
            return this._labels;
        },

        setLabels: function(set) {
            this._labels = set;
        },

        getType: function() {
            return this._type;
        },

        isDead: function() {
            return this._isDead;
        },

        /**
         * Sets the isDead property of the PedigreeNode to the value
         * passed in the isDead parameter
         * @param isDead boolean value indicating whether the PedigreeNode is dead or not
         */
        setDead: function(isDead) {
            this._isDead = isDead;
            !isDead && this.setSB(false);
            //show/hide agelabel based on isdead

            if (this.isDead()) {
                if (this.deadShape) {
                    this.deadShape.show();
                }
                else {
                    var x = this.getX() - 2,
                        y = this.getY() - (editor.graphics.getRadius() * 1.75),
                        w = 4,
                        h = editor.graphics.getRadius() * 3.5;
                    this.deadShape = editor.paper.rect(x,y,w,h).attr({fill: "black"}).rotate(45);
                    this.getPropertyShapes().push(this.deadShape);
                }
            }
            else {
                (this.deadShape && this.deadShape.hide());
            }
            this.draw();
        },

        isStillBorn: function() {
            return this._isSB;
        },

        toggleSB: function()
        {
            this.setSB(!this.isStillBorn());
            this.updateSBLabel();
            this.setDead(this.isStillBorn());
        },

        setSB: function(isSB)
        {
            isSB && this.setDead(true);
            this._isSB = isSB;
            this.updateSBLabel();
        },

        updateSBLabel: function() {
            if(this.isStillBorn() && (!this.getLabels()[0] || this.getLabels()[0].type != "SBLabel")) {
                var text = editor.paper.text(this.getX(), this.getY(), "SB");
                text.type = "SBLabel";
                this.setLabels(this.addToBeginningOfSet(text, this.getLabels()));
            }
            else if(!this.isStillBorn() && this.getLabels()[0] && this.getLabels()[0].type == "SBLabel") {
                this.getLabels()[0].remove();
                this.setLabels(this.withoutFirstSetElement(this.getLabels()));
            }
            this.repositionLabels();
        },

        withoutFirstSetElement: function(set)
        {
            var newSet = editor.paper.set();
            var i = 1;
            while (i < set.length)
            {
                newSet.push(set[i]);
                i++;
            }
            return newSet;
        },

        addToBeginningOfSet: function(element, set)
        {
            var newSet = editor.paper.set(element);
            for(var i = 0; i < set.length; i++)
            {
                newSet.push(set[i]);
            }
            return newSet;
        },

        /**
         * Replaces the age property of the PedigreeNode and
         * displays the appropriate label on the graph
         * @param: birth Year is a string in the format '56 y', '4 mo' or 'b. 1988'
         */
        setBirthDate: function(birthDate) {
            if(this.isDead()) {

            }
            this.birthDate = birthDate;
            this.ageLabelText = getAge(this.getBirthDate());
            this.updateAgeLabel();
        },

        updateAgeLabel: function() {
            this.ageLabel && this.ageLabel.remove();
            for(var i = 0; i < this.getLabels().length;i++) {
                if(this.getLabels()[i].type = "ageLabel") {
                    this.getLabels().pop(i);
                    return;
                    }
                }
            if(this.ageLabelText) {
                this.ageLabel = editor.paper.text(this.getX(), this.getY(), this.ageLabelText);
                this.ageLabel.type = "ageLabel";
                this.getLabels().push(this.ageLabel);
            }
            this.repositionLabels();
        },

        repositionLabels: function() {
            for (var i = 0; i < this.getLabels().length; i++)
            {
                this.getLabels()[i].attr("y", this.getY() + (editor.graphics.getRadius() *1.5) + (i * 18));
                this.getLabels()[i].attr(editor.graphics._attributes.label);
                this.getLabels()[i].oy = this.getLabels()[i].attr("y");
            }
            this.draw();
        },

    /**
         * Assigns disorderName to this pedigreeNode and displays it in the right
         * proportion with the other Disorders. Uses the color/pattern listed in
         * _disorderMap
         * @param: DisorderName a string representing the name of the Disorder.
         */
        addDisorder: function(disorderName) {
            if(this.getDisorders().indexOf(disorderName) < 0)
            {
                this.getDisorders().push(disorderName);
                disorderMap[disorderName].numAffectedNodes++;
                this.generateDisorderColor(disorderName);
                this.generateDisorderShapes();
            }
            else{
                alert("This disorder was already registered");
            }
        },

        generateDisorderColor: function(disorderName) {
            if (Object.keys(disorderMap).indexOf(disorderName) == -1) {
                var randomColor = "#______" .replace(/_/g,function(){return (~~(Math.random()*14+2)).toString(16);});
                while(randomColor == "#ffffff")
                {
                    randomColor = "#"+((1<<24)*Math.random()|0).toString(16);
                }
                disorderMap[disorderName] = {color: randomColor,
                    numAffectedNodes: 0 }
            }
        },

        /**
         * Removes disorderName from this pedigreeNode and updates the node graphics.
         * updates the NumAffectedNodes value in _disorderMap
         * @param: DisorderName a string representing the name of the Disorder.
         */
        removeDisorder: function(disorderName) {
            if(this.getDisorders().indexOf(disorderName) >= 0)
            {
                this.setDisorders(this.getDisorders().without(disorderName));
                disorderMap[disorderName].numAffectedNodes--;
                this.generateDisorderShapes();
            }
            else
            {
                alert("This person doesn't have the specified disorder");
            }
        },

        /**
         * Changes the '_adopted' property of pedigreeNode, and displays
         * the graphical representation
         */
        setAdopted: function(isAdopted) {
            this._adopted = isAdopted;
            if (isAdopted && this._adoptedShape) {
                this._adoptedShape.show();
            }
            else if(isAdopted) {
                    var r = editor.graphics.getRadius(),
                        x1 = this.getX() - ((0.8) * r),
                        x2 = this.getX() + ((0.8) * r),
                        y = this.getY() - ((1.3) * r),
                        brackets = "M" + x1 + " " + y + "l" + r/(-2) +
                            " " + 0 + "l0 " + (2.6 * r) + "l" + (r)/2 + " 0M" + x2 +
                            " " + y + "l" + (r)/2 + " 0" + "l0 " + (2.6 * r) + "l" +
                            (r)/(-2) + " 0";
                    this._adoptedShape = this._paper.path(brackets).attr("stroke-width", 3);
            }
            else {
                (this._adoptedShape && this._adoptedShape.hide());
            }
            this.draw();
        },

        toggleAdoption: function() {
            this._adopted = !this._adopted;
            this.setAdopted(this._adopted);
        },

        getAllGraphics: function() {
            return editor.paper.set(this.getHoverBox().getBackElements(),this.getDrawing(), this.getPropertyShapes(),
                                    this.getLabels(), this.getHoverBox().getFrontElements());
        },

        /**
         * Removes the properties and graphical elements of the PedigreeNode
         */
        remove: function() {
            this._disorders.each(function(s) {
                disorderMap[s].numAffectedNodes--;
            });
        }
    });

