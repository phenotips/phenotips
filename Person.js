/*
 * Person is a class representing any node that has sufficient information to be
 * displayed on the final pedigree graph (printed or exported). Person objects
 * contain information about disorders, age and other relevant properties, as well
 * as graphical data to visualize this information.
 *
 * @param x the x coordinate on the Raphael canvas at which the node drawing will be centered
 * @param the y coordinate on the Raphael canvas at which the node drawing will be centered
 * @param gender is a string that's either 'M', 'F' or 'U'
 */

    var Person = Class.create(AbstractNode, {

        initialize: function($super, x, y, gender) {
            $super(x,y,gender);
            this._type = "pn";
            this._firstName = null;
            this._lastName = null;
            this._nameLabel = null;
            this._stillBirthLabel = null;
            this._ageLabel = null;
            this._evaluationLabels = [];
            this._disorderShapes = null;
            this._deadShape = null;
            this._adoptedShape = null;
            this._fetusShape = null;
            this._patientShape = null;
            this._abortedShape = null;
            this._birthDate = null;
            this._deathDate = null;
            this._conceptionDate = null;
            this._isAdopted = false;
            this._isFetus = false; //TODO: implement pregnancy
            this._lifeStatus = 'alive';
            this._patientStatus = null;//TODO: implement proband/consultand
            this._disorders = [];
            this._evaluations = [];
            this._isSelected = false;
            this._hoverBox = new Hoverbox(this);
            this.setGender(this._gender, false);
            this.draw();
        },

        generateGraphics: function() {
            return new PersonVisuals(this);
        },

        getEvaluations: function() {
            return this._evaluations;
        },

        setEvaluations: function(evaluationsArray) {
            this._evaluations = evaluationsArray;
        },

        isSelected: function() {
            return this._isSelected;
        },

        setSelected: function(isSelected) {
            this._isSelected = isSelected;
        },

        getEvaluationLabels: function() {
            return this._evaluationLabels;
        },

        setEvaluationLabels: function(setOfLabels) {
            this._evaluationLabels && this._evaluationLabels.each(function(label) {
                label.remove();
            });
            this._evaluationLabels = setOfLabels;
        },

        getFirstName: function() {
            return this._firstName;
        },

        setFirstName: function(firstName, forceDisplay) {
            firstName && (firstName = firstName.charAt(0).toUpperCase() + firstName.slice(1));
            this._firstName = firstName;
            forceDisplay && this.drawLabels();
        },

        setFullName: function(first, last, forceDisplay) {
            first && this.setFirstName(first);
            last && this.setLastName(last);
            forceDisplay && this.drawLabels();
        },

        getLastName: function() {
            return this._lastName;
        },

        setLastName: function(lastName, forceDisplay) {
            lastName && (lastName = lastName.charAt(0).toUpperCase() + lastName.slice(1));
            this._lastName = lastName;
            forceDisplay && this.drawLabels();
        },

        updateNameLabel: function() {
            this._nameLabel && this._nameLabel.remove();
            var text =  "";
            this.getFirstName() && (text += this.getFirstName());
            this.getLastName() && (text += ' ' + this.getLastName());
            if(text.strip() != ''){
                this.setNameLabel(editor.paper.text(this.getX(), this.getY() + editor.graphics.getRadius(), text));
                this.getNameLabel().attr({'font-size': 18, 'font-family': 'Cambria'});
            }
            else {
                this.setNameLabel(null);
            }
        },

        getNameLabel: function() {
            return this._nameLabel;
        },

        setNameLabel: function(label) {
            this._nameLabel && this._nameLabel.remove();
            this._nameLabel = label;
        },

        getLifeStatus: function() {
            return this._lifeStatus;
        },

        getDisorderShapes: function() {
            return this._disorderShapes;
           // return this.getDrawing()[2];
        },

        setDisorderShapes: function(set) {
            this._disorderShapes && this._disorderShapes.remove();
//            if(this.getDrawing().length > 2) {
//                this.getDrawing().pop().remove();
//            }
//            this.getDrawing().push(set);
        },

        isFetus: function() {
            return this._isFetus;
        },
        
        setFetus: function(isFetus, forceShape) {
            this._isFetus = isFetus;
            forceShape && this.drawShapes();
        },

        setFetusShape: function(raphaelElement) {
            this._fetusShape && this._fetusShape.remove();
            this._fetusShape = raphaelElement;
        },

        updateFetusShape: function() {
            var fetusShape;
            if(this.isFetus() && this.getLifeStatus() == 'alive') {
                fetusShape = editor.paper.text(this.getX(), this.getY(), "P");
                fetusShape.attr(editor.graphics._attributes.fetusShape);
            }
            this.setFetusShape(fetusShape);
        },

        getConceptionDate: function() {
            return this._conceptionDate;
        },

        getGestationAge: function() {
            if(this.getConceptionDate()) {
                var oneWeek = 1000 * 60 * 60 * 24 * 7,
                    lastDay = this.getDeathDate() || new Date();
                return Math.round((lastDay.getTime()- this.getConceptionDate().getTime())/oneWeek)
            }
            else {
                return null;
            }
        },

        setGestationAge: function(numWeeks) {
            if(Object.isNumber(numWeeks)){
                var daysAgo = numWeeks * 7,
                    d = new Date();
                d.setDate(d.getDate() - daysAgo);
                this.setConceptionDate(d);
            }
        },

        setConceptionDate: function(date) {
            if(this.getLifeStatus() == 'aborted' || this.getLifeStatus() == 'stillborn') {
                this._conceptionDate = date;
                this.updateAgeLabel();
            }
        },

        setLifeStatus: function(status, forceDisplay) {
            if (status == 'deceased') {
                this.setDeceased(forceDisplay);
            }
            else if (status == 'aborted') {
                this.setAborted(forceDisplay);
            }
            else if (status == 'stillborn') {
                this.setSB(forceDisplay)
            }
            else {
                this.setAlive(forceDisplay);
            }
        },

        setAborted: function(forceDisplay) {
            if (!this.isAdopted()) {
                //TODO: update menu with set fetus: true and inactive, set adopted inactive
                this._lifeStatus = 'aborted';
                this.setFetus(true, false);
                this.setBirthDate(null);
                forceDisplay && this.draw();
            }
        },

        setAlive: function(forceDisplay) {
            this.setConceptionDate(null);


            var prevState = this.getLifeStatus();
            this._lifeStatus = 'alive';
            this.getAbortedShape() && this.getAbortedShape().remove();
            this.getDeadShape() && this.getDeadShape().remove();
            this.setDeathDate(null);
            prevState == 'aborted' && this.setGender(this.getGender());
            this.getStillBirthLabel() && this.getStillBirthLabel().remove();
            if(prevState == 'aborted' || prevState == 'stillborn') {
                this.setFetus(true);
            }
            this.drawLabels();
            this.getHoverBox().unhidePartnerHandles();
        },

        setDeceased: function(forceDisplay) {
            if(!this.isFetus()) {

                this.setConceptionDate(null);

                //TODO: update menu with set fetus: true and inactive, set adopted inactive

                var prevState = this.getLifeStatus();
            this._lifeStatus = 'deceased';
            this.getAbortedShape() && this.getAbortedShape().remove();
            prevState == 'aborted' && this.setGender(this.getGender());
            this.getStillBirthLabel() && this.getStillBirthLabel().remove();
            this.setConceptionDate(null);
            prevState != 'stillborn' && this.setDeadShape();
            this.updateAgeLabel();
            this.getHoverBox().unhidePartnerHandles();
            }
        },

        setSB: function(forceDisplay) {
            if (!this.isAdopted()) {
                //TODO: update menu with set fetus: true and inactive, set adopted inactive
                this._lifeStatus = 'aborted';
                this.setFetus(true, false);
                this.setBirthDate(null);
                forceDisplay && this.draw();
            }
        },

        getSBLabel: function() {
            return this._stillBirthLabel;
        },

        setSBLabel: function(label) {
            this.getSBLabel() && this.getSBLabel().remove();
            this._stillBirthLabel = label;
        },

        updateSBLabel: function() {
            var SBLabel;
            this.getLifeStatus() == 'stillborn' && (SBLabel = editor.paper.text(this.getX(), this.getY(), "SB"));
            this.setSBLabel(SBLabel);
        },

        updateLifeStatusShapes: function() {
            var status = this.getLifeStatus();
            this.getAbortedShape() && this.getAbortedShape().remove();
            this.getDeadShape() && this.getDeadShape().remove();
            this.getHoverBox().unhidePartnerHandles();



            this.setGenderShape(this.generateGenderShape());
            this.getGenderShape().push(this.generateDisorderShapes());

            if(status == 'alive'){

            }
            else if(status == 'deceased'){
                this.setDeadShape();
            }
            else if(status == 'stillborn') {
                this.setDeadShape();
                this.setBirthDate(null);
                this.getHoverBox().hidePartnerHandles();
                this.setFetus(true, false);
            }
            else if(status == 'aborted') {
                this.setBirthDate(null);
                this.getHoverBox().hidePartnerHandles();
                this.setFetus(true, false);
            }
        },

        generateGenderShape: function($super) {
            if(this.getLifeStatus() == 'aborted') {
                var side = editor.graphics.getRadius() * Math.sqrt(3.5),
                    height = side/Math.sqrt(2),
                    x = this.getX() - height,
                    y = this.getY();
                var shape = editor.paper.path(["M",x, y, 'l', height, -height, 'l', height, height,"z"]);
                shape.attr(editor.graphics._attributes.nodeShape);

                x = this.getX() - 2 * height/ 3;
                y = this.getY() + height/ 3;
                var deadShape = editor.paper.path(["M", x, y, 'l', height + height/3, -(height+ height/3), "z"]);
                deadShape.attr("stroke-width", 3);
                this.setAbortedShape(deadShape);


                if(this.getGender() == 'U') {
                    return shape;
                }
                else {
                    x = this.getX();
                    y = this.getY() + editor.graphics.getRadius()/1.1;
                    var text = (this.getGender() == 'M') ? "Male" : "Female";
                    var genderLabel = editor.paper.text(x, y, text).attr(editor.graphics._attributes.label);
                    return editor.paper.set(shape, genderLabel);
                }
             }
            else {
                return $super();
            }
        },

        getAbortedShape: function() {
            return this._abortedShape;
        },

        setAbortedShape: function(shape) {
            this.getAbortedShape() && this.getAbortedShape().remove();
            this._abortedShape = shape;
        },

        generateGenderShape: function($super) {
            if(this.getLifeStatus() == 'aborted' && this.getGender() != "U") {
                var shape = this.generateGenderShape()[0],
                    glow = shape.glow({width: 5, fill: true, opacity: 0.1}).translate(3,3);
                return editor.paper.set( glow, shape);
            }
            else {
                return $super();
            }
        },

        getBirthDate: function() {
            return this._birthDate;
        },

        getDeathDate: function() {
            return this._deathDate;
        },

        setDeathDate: function(deathDate) {
            if(!deathDate || deathDate && !this.getBirthDate() || deathDate.getDate()>this.getBirthDate().getDate()) {
                this._deathDate =  deathDate;
                this._deathDate && (this.getLifeStatus() == 'alive') && this.setDeceased();
                this.updateAgeLabel();
            }
        },

        getHoverBox: function() {
            return this._hoverBox;
        },

        setGender: function($super, gender, forceDraw) {
            $super(gender);
            forceDraw && this.drawShapes();
        },

        drawShapes: function($super) {
            this.updateFetusShape();
            this.setDisorderShapes(this.generateDisorderShapes());
            $super();

        },

        getDisorders: function() {
            return this._disorders;
        },

        setDisorders: function(disorderArray) {
            this._disorders = disorderArray;
        },

        generateDisorderShapes: function() {
            var gradient = function(color, angle) {
                var hsb = Raphael.rgb2hsb(color),
                    darker = Raphael.hsb2rgb(hsb['h'],hsb['s'],hsb['b']-.25)['hex'];
                return angle +"-"+darker+":0-"+color+":100";
            };
            var disorderShapes = editor.paper.set();
            if(this.getLifeStatus() == 'aborted') {

                var side = editor.graphics.getRadius() * Math.sqrt(3.5),
                    height = side/Math.sqrt(2),
                    delta = (height * 2)/(this.getDisorders().length),
                    x1 = this.getX() - height,
                    y1 = this.getY();

                for(var k = 0; k < this.getDisorders().length; k++) {
                    var corner = [];
                    var x2 = x1 + delta;
                    var y2 = this.getY() - (height - Math.abs(x2 - this.getX()));
                    if (x1 < this.getX() && x2 >= this.getX()) {
                        corner = ["L", this.getX(), this.getY()-height];
                    }
                    var slice = editor.paper.path(["M", x1, y1, corner,"L", x2, y2, 'L',this.getX(), this.getY(),'z']),
                    color = gradient(editor.getLegend().getDisorder(this.getDisorders()[k]).getColor());
                    slice.attr({fill: color, 'stroke-width':.5 });
                    disorderShapes.push(slice.attr({fill: color, 'stroke-width':.5 }));
                    x1 = x2;
                    y1 = y2;
                }
            }
            else {

                var disorderAngle = (this.getDisorders().length == 0)?0:(360/this.getDisorders().length).round();
                var delta = (360/(this.getDisorders().length))/2;

                for(var i = 0; i < this.getDisorders().length; i++) {
                    var color = gradient(editor.getLegend().getDisorder(this.getDisorders()[i]).getColor(), (i * disorderAngle)+delta);
                    disorderShapes.push(sector(editor.paper, this.getX(), this.getY(), editor.graphics.getRadius(),
                        this.getGender(), i * disorderAngle, (i+1) * disorderAngle, color));
                }
                (disorderShapes.length < 2) && disorderShapes.attr('stroke', 'none');
            }

           return disorderShapes;
        },

        getType: function() {
            return this._type;
        },

        setDeadShape: function() {
            var x1 = this.getX() - (10/8) * editor.graphics.getRadius(),
                y1 = this.getY() + (10/8) * editor.graphics.getRadius(),
                x2 = this.getX() + (10/8) * editor.graphics.getRadius(),
                y2 = this.getY() - (10/8) * editor.graphics.getRadius();
            this._deadShape = editor.paper.path(["M", x1,y1,"L",x2, y2]).attr("stroke-width", 3);
            this.drawShapes();
        },

        getDeadShape: function() {
            return this._deadShape;
        },

        getStillBirthLabel: function() {
            return this._stillBirthLabel;
        },



        /**
         * Replaces the age property of the Person and
         * displays the appropriate label on the graph
         * @param: birth Year is a string in the format '56 y', '4 mo' or 'b. 1988'
         */
        setBirthDate: function(birthDate, forceDraw) {
            if (!this.isFetus() && (!birthDate || birthDate && !this.getDeathDate() || birthDate.getDate() < this.getDeathDate())) {
                this._birthDate = birthDate;
                forceDraw && this.drawLabels();
            }
        },
        getAgeLabel: function() {
            return this._ageLabel;
        },

        setAgeLabel: function(label) {
            this.getAgeLabel() && this.getAgeLabel().remove();
            this._ageLabel = label;
        },

        updateAgeLabel: function() {
            var text;
            if (this.isFetus() && this.getLifeStatus() == 'alive') {
                this.getConceptionDate() && (text = getAge(this.getConceptionDate()));
            }
            else if(this.getLifeStatus() == 'alive') {
                this.getBirthDate();
                this.getBirthDate() && (text = getAge(this.getBirthDate()));
            }
            else {
                var prefix = (this.getConceptionDate()) ? '' : "d. ";
                if(this.getDeathDate() && this.getBirthDate()) {
                    text = prefix + getAge(this.getBirthDate(), this.getDeathDate());
                }
                else if(this.getDeathDate() && this.getConceptionDate()) {
                    text = prefix + getAge(this.getConceptionDate(), this.getDeathDate());
                }
                else if (this.getDeathDate()) {
                    text = prefix + this.getDeathDate().getFullYear();
                }
            }
            text && (text = editor.paper.text(this.getX(), this.getY(), text));
            this.setAgeLabel(text);
        },

    /**
         * Assigns disorderName to this pedigreeNode and displays it in the right
         * proportion with the other Disorders. Uses the color/pattern listed in
         * _disorderMap
         * @param: DisorderName a string representing the name of the Disorder.
         */
        addDisorder: function(disorderID, disorderName) {
            if(this.getDisorders().indexOf(disorderID) < 0) {
                editor.getLegend().addCase(disorderID, disorderName);
                this.getDisorders().push(disorderID);
                this.generateDisorderShapes();
                this.drawShapes();
            }
            else {
                alert("This disorder was already registered");
            }
        },

        /**
         * Removes disorderName from this pedigreeNode and updates the node graphics.
         * updates the NumAffectedNodes value in _disorderMap
         * @param: DisorderName a string representing the name of the Disorder.
         */
        removeDisorder: function(disorderID) {
            if(this.getDisorders().indexOf(disorderID) >= 0) {
                editor.getLegend().removeCase(disorderID);
                this.setDisorders(this.getDisorders().without(disorderID));
                this.generateDisorderShapes();
            }
            else  {
                alert("This person doesn't have the specified disorder");
            }
        },

        /**
         * Changes the '_adopted' property of pedigreeNode, and displays
         * the graphical representation
         */
        setAdopted: function(isAdopted) {
            //TODO: implement adopted and social parents

            if(isAdopted && (this.getLifeStatus() == 'alive' || this.getLifeStatus() =='deceased')) {
                this._isAdopted = isAdopted;
                var r = editor.graphics.getRadius(),
                    x1 = this.getX() - ((0.8) * r),
                    x2 = this.getX() + ((0.8) * r),
                    y = this.getY() - ((1.3) * r),
                    brackets = "M" + x1 + " " + y + "l" + r/(-2) +
                        " " + 0 + "l0 " + (2.6 * r) + "l" + (r)/2 + " 0M" + x2 +
                        " " + y + "l" + (r)/2 + " 0" + "l0 " + (2.6 * r) + "l" +
                        (r)/(-2) + " 0";
                this._adoptedShape = editor.paper.path(brackets).attr("stroke-width", 3);
            }
            else if(!isAdopted){
                    this._isAdopted = isAdopted;
                    this._adoptedShape && this._adoptedShape.remove();
            }
            this.drawShapes();
        },

        isAdopted: function() {
            return this._isAdopted;
        },

        getAdoptedShape: function() {
            return this._adoptedShape;
        },

        getFetusShape: function() {
            return this._fetusShape;
        },

        getAllGraphics: function() {
            var shapes = this.getShapes();
            shapes[shapes.length -1] = this.getLabels();
            shapes.push(this.getHoverBox().getFrontElements());
            return shapes;
        },

        getShapes: function() {
            var lifeStatusShapes = editor.paper.set();
            this.getFetusShape() && lifeStatusShapes.push(this.getFetusShape());
            this.getDeadShape() && lifeStatusShapes.push(this.getDeadShape());
            this.getAdoptedShape() && lifeStatusShapes.push(this.getAdoptedShape());
            this.getAbortedShape() && lifeStatusShapes.push(this.getAbortedShape());
            return editor.paper.set(this.getHoverBox().getBackElements(), this.getGenderShape(), this.getDisorderShapes(),
                                    lifeStatusShapes, this.getHoverBox().getFrontElements());
        },

        drawLabels: function() {
            this.updateAgeLabel();
            this.updateNameLabel();
            this.updateSBLabel();
            this.getLabels().toFront();
            this.getHoverBox().getFrontElements().toFront();
        },

        getLabelsYCoord: function() {
            return this.getY() + editor.graphics.getRadius() * 1.5;
        },

        getLabels: function() {
            var labels = editor.paper.set();
            this.getNameLabel() && labels.push(this.getNameLabel());
            this.getSBLabel() && labels.push(this.getSBLabel());
            this.getAgeLabel() && labels.push(this.getAgeLabel());
            this.getEvaluationLabels() && labels.concat(this.getEvaluationLabels());

            var yOffset = (this.isSelected()) ? 50 : 0;
            var startY = this.getLabelsYCoord() + yOffset;
            for (var i = 0; i < labels.length; i++) {
                labels[i].attr("y", startY + 7);
                labels[i].attr(editor.graphics._attributes.label);
                labels[i].oy = (labels[i].attr("y") - yOffset);
                startY = labels[i].getBBox().y2;
            }
            return labels;
        },

        draw: function($super) {
            this.drawLabels();
            $super();
        },

        updateEvaluationLabels: function() {
            var evalLabels = [];
            this.getEvaluations().each( function(e) {
                //TODO: get evaluations from legend
                var label = editor.paper.text(e);
                labels.push(label);
                evalLabels.push(label);
            });
            this.setEvaluationLabels(evalLabels);
        },

        /**
         * Removes the properties and graphical elements of the Person
         */
        remove: function($super) {
            this.getDisorders().each(function(disorderID) {
                editor.getLegend().removeCase(disorderID);
            });
            $super();
        },


        getSummary: function() {
            return {
                identifier:    {value : this.getID()},
                first_name:    {value : this.getFirstName()},
                last_name:     {value : this.getLastName()},
                gender:        {value : this.getGender()},
                date_of_birth: {value : this.getBirthDate(), inactive : (this.getLifeStatus() != 'alive')},
                disorders:     {value : this.getDisorders()},
                adopted:       {value : this.isAdopted()},
                state:         {value : this.getLifeStatus(), inactive : [this.getDeathDate() ? 'alive' : '']},
                date_of_death: {value : this.getDeathDate()},
                gestation_age: {value : this.getGestationAge(), inactive : (this.getLifeStatus() == 'alive')}
            };
        }
    });

