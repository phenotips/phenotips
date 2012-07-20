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
            this._evaluationLabels = [];
            this._stillBirthLabel = null;
            this._ageLabel = null;
            this._labels = [this._nameLabel, this._stillBirthLabel, this._ageLabel, this._evaluationLabels];
            this._deadShape = null;
            this._adoptedShape = null;
            this._fetusShape = null;
            this._patientShape = null;
            this._abortedShape = null;
            this._propertyShapes = [this._fetusShape, this._deadShape, this._adoptedShape, this._patientShape,this._abortedShape];
            this._birthDate = null;
            this._deathDate = null;
            this._conceptionDate = null;
            this._isAdopted = false;
            this._isFetus = false; //TODO: implement pregnancy
            this._lifeStatus = 'alive';
            this._patientStatus = null;//TODO: implement proband/consultand
            this._ageLabelText = null;
            this._disorders = [];
            this._hoverBox = new Hoverbox(this);
            this.setGender(this._gender);
        },

        getFirstName: function() {
            return this._firstName;
        },

        setFirstName: function(firstName, forceDisplay) {
            firstName && (firstName = firstName.charAt(0).toUpperCase() + firstName.slice(1));
            this._firstName = firstName;
            forceDisplay && this.updateNameLabel();
        },

        setFullName: function(first, last) {
            first && this.setFirstName(first);
            last && this.setLastName(last);
        },

        getLastName: function() {
            return this._lastName;
        },

        setLastName: function(lastName, forceDisplay) {
            lastName && (lastName = lastName.charAt(0).toUpperCase() + lastName.slice(1));
            this._lastName = lastName;
            forceDisplay && this.updateNameLabel();
        },

        updateNameLabel: function() {
            this._nameLabel && this._nameLabel.remove();
            var text =  "";
            this.getFirstName() && (text += this.getFirstName());
            this.getLastName() && (text += ' ' + this.getLastName());
            if(text.strip() != ''){
                this._nameLabel = editor.paper.text(this.getX(), this.getY() + editor.graphics.getRadius(), text);
                this._nameLabel.attr({'font-size': 18, 'font-family': 'Cambria'});
                this.getLabels()[0] = this._nameLabel
            }
            this.drawLabels();

        },

        getNameLabel: function() {
            return this._nameLabel;
        },

        getLifeStatus: function() {
            return this._lifeStatus;
        },

        getDisorderShapes: function() {
            return this.getDrawing()[2];
        },

        setDisorderShapes: function(set) {
            if(this.getDrawing().length > 2) {
                this.getDrawing().pop().remove();
            }
            this.getDrawing().push(set);
        },

        isFetus: function() {
            return this._isFetus;
        },
        
        setFetus: function(isFetus) {
            if(this.getLifeStatus() != 'deceased' && !this.isAdopted()) {
                this._isFetus = isFetus;
                if(isFetus){
                    this._fetusShape = editor.paper.text(this.getX(), this.getY(), "P");
                    this._fetusShape.attr(editor.graphics._attributes.fetusShape);
                    this.getPropertyShapes()[0] = this._fetusShape;
                    this.setBirthDate(null);
                    this.setDeathDate(null);
                }
                else {
                    this._fetusShape && this._fetusShape.remove();
                }
            }
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

        setLifeStatus: function(status) {
            if (status == 'deceased') {
                this.setDeceased();
            }
            else if (status == 'aborted') {
                this.setAborted();
            }
            else if (status == 'stillborn') {
                this.setSB()
            }
            else {
                this.setAlive();
            }
        },

        setAborted: function() {
            this._lifeStatus = 'aborted';
            this.getDrawing().remove();
            this.setDrawing(this.generateDrawing());
            this.getDrawing().push(this.generateDisorderShapes());
            this.setBirthDate(null);
            this.updateAgeLabel();
            this.getHoverBox().hidePartnerHandles();
            this.getDeadShape() && this.getDeadShape().remove();
            this.drawShapes();
            this.setFetus(false);
        },

        setAlive: function() {
            var prevState = this.getLifeStatus();
            this._lifeStatus = 'alive';
            this.getAbortedShape() && this.getAbortedShape().remove();
            this.getDeadShape() && this.getDeadShape().remove();
            this.setDeathDate(null);
            prevState == 'aborted' && this.setGender(this.getGender());
            this.getStillBirthLabel() && this.getStillBirthLabel().remove();
            this.setConceptionDate(null);
            if(prevState == 'aborted' || prevState == 'stillborn') {
                this.setFetus(true);
            }
            this.updateAgeLabel();
            this.getHoverBox().unhidePartnerHandles();
        },

        setDeceased: function() {
            var prevState = this.getLifeStatus();
            this._lifeStatus = 'deceased';
            this.getAbortedShape() && this.getAbortedShape().remove();
            prevState == 'aborted' && this.setGender(this.getGender());
            this.getStillBirthLabel() && this.getStillBirthLabel().remove();
            this.setConceptionDate(null);
            prevState != 'stillborn' && this.setDeadShape();
            this.updateAgeLabel();
            this.getHoverBox().unhidePartnerHandles();

        },

        setSB: function() {
            if (!this.isAdopted()) {
                var prevState = this.getLifeStatus();
                this._lifeStatus = 'stillborn';
                this.getAbortedShape() && this.getAbortedShape().remove();
                prevState == 'aborted' && this.setGender(this.getGender());
                prevState != 'deceased' && this.setDeadShape();
                this.setBirthDate(null);
                this.getStillBirthLabel() && this.getStillBirthLabel().remove();
                this._stillBirthLabel = editor.paper.text(this.getX(), this.getY(), "SB");
                this.getLabels()[1] = this.getStillBirthLabel();
                this.updateAgeLabel();
                this.getHoverBox().hidePartnerHandles();
                this.setFetus(false);
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
            return this.getPropertyShapes()[4];
        },

        setAbortedShape: function(shape) {
            this.getAbortedShape() && this.getAbortedShape().remove();
            this.getPropertyShapes()[4] = shape;
            this._abortedShape = shape;
        },

        generateDrawing: function() {
            var drawing = this.generateGenderShape(),
                shape = drawing;
            if(this.getGender() != "U" && this.getLifeStatus() == 'aborted' ) {
               shape = drawing[0];
            }
            var glow = shape.clone().attr({"fill": "gray", "opacity":.2, stroke: 'none', 'x': shape.attr("x") + 4, 'y': shape.attr("y") +4});
                //shape.glow({width: 5, fill: true, opacity: 0.1}).translate(3,3);
            return editor.paper.set( glow, drawing);
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

        setGender: function($super, gender) {
            $super(gender);
            this.getDrawing().push(this.generateDisorderShapes());
            this.drawShapes();
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
                    darker = Raphael.hsb2rgb(hsb['h'],hsb['s'],hsb['b']-.4)['hex'];
                //TODO: calculate angle
                return "250-"+darker+":0-"+color+":100";
            };
            var disorderShapes = editor.paper.set();
            this.getDisorderShapes() && this.getDisorderShapes().remove();
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

                for(var i = 0; i < this.getDisorders().length; i++) {
                    var color = gradient(editor.getLegend().getDisorder(this.getDisorders()[i]).getColor(), i * disorderAngle);
                    disorderShapes.push(sector(editor.paper, this.getX(), this.getY(), editor.graphics.getRadius(),
                        this.getGender(), i * disorderAngle, (i+1) * disorderAngle, color));
                }
            }

            this.setDisorderShapes(disorderShapes);
        },

        getPropertyShapes: function() {
            return this._propertyShapes;
        },

        getLabels: function() {
            return this._labels;
        },

        setLabels: function(array) {
            this._labels = array;
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
            this.getPropertyShapes()[1] = this._deadShape;
            this.drawShapes();
        },

        getDeadShape: function() {
            return this._deadShape;
        },

        getStillBirthLabel: function() {
            return this._stillBirthLabel;
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
         * Replaces the age property of the Person and
         * displays the appropriate label on the graph
         * @param: birth Year is a string in the format '56 y', '4 mo' or 'b. 1988'
         */
        setBirthDate: function(birthDate) {
            if (!this.isFetus() && (!birthDate || birthDate && !this.getDeathDate() || birthDate.getDate() < this.getDeathDate())) {
                this._birthDate = birthDate;
                this.updateAgeLabel();
            }
        },
        getAgeLabel: function() {
            return this._ageLabel;
        },

        setAgeLabel: function(text) {
            this.getAgeLabel() && this.getAgeLabel().remove();
            if(text) {
                this._ageLabel = this.getLabels()[2] = editor.paper.text(this.getX(),0,text);
            }
            else {
                this._ageLabel = this.getLabels()[2] = null;
            }
            this.drawLabels();
        },

        updateAgeLabel: function() {
            var text;
            if (this.isFetus() && this.getLifeStatus() == 'alive') {
                this.getConceptionDate() && (text = getAge(this.getConceptionDate()));
            }
            else if(this.getLifeStatus() == 'alive') {
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
            this.setAgeLabel(text);
        },

        drawLabels: function() {
            var lineNum = 0;
            var labels = this.getLabels().flatten().compact();
            for (var i = 0; i < labels.length; i++) {
                labels[i].attr("y", this.getY() + (editor.graphics.getRadius() * 1.5) + (lineNum * 18));
                labels[i].attr(editor.graphics._attributes.label);
                labels[i].oy = labels[i].attr("y");
                labels[i].toFront();
                lineNum++;
            }
            this.getHoverBox().getFrontElements().toFront();
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
                this.getPropertyShapes()[2] = this._adoptedShape;
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

        getfetusShape: function() {
            return this._fetusShape;
        },

        getAllGraphics: function() {
            var shapes = this.getAllShapes();
            var labels = editor.paper.set();
            this.getLabels().flatten().compact().each(function(label) {
                labels.push(label);
            });
            shapes[shapes.length -1] = labels;
            shapes.push(this.getHoverBox().getFrontElements());
            return shapes;
        },

        getAllShapes: function() {
            var propertyShapes = editor.paper.set();
            this.getPropertyShapes().flatten().compact().each(function(shape) {
               propertyShapes.push(shape);
            });
            return editor.paper.set(this.getHoverBox().getBackElements(), this.getDrawing(),
                                    propertyShapes, this.getHoverBox().getFrontElements());
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

        generateMenuData: function() {
            return {
                node : this,
                identifier: this.getID(),
                gender: this.getGender(),
                date_of_birth: this.getBirthDate(),
                disorders: this.getDisorders(),
                adopted: this.isAdopted(),
                state: this.getLifeStatus(),
                date_of_death: this.getDeathDate(),
                gestation_age: this.getGestationAge(),
                first_name: this.getFirstName(),
                last_name: this.getLastName()
            };
        }
    });

