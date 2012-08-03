/*
 *  The class responsible for
 */

var PersonVisuals = Class.create(AbstractPersonVisuals, {
    
    initialize: function($super, node, x, y) {
        this._nameLabel = null;
        this._stillBirthLabel = null;
        this._ageLabel = null;
        this._evaluationLabels = [];
        this._disorderShapes = null;
        this._deadShape = null;
        this._adoptedShape = null;
        this._fetusShape = null;
        this._patientShape = null;
        this._isSelected = false;
        this._hoverBox = new Hoverbox(node, x, y);
        $super(node, x, y);
    },

    getHoverBox: function() {
        return this._hoverBox;
    },

    updateGenderShape: function($super) {
        if(this.getNode().getLifeStatus() == 'aborted') {
            this._genderShape && this._genderShape.remove();
            var side = editor.graphics.getRadius() * Math.sqrt(3.5),
                height = side/Math.sqrt(2),
                x = this.getX() - height,
                y = this.getY();
            var shape = editor.paper.path(["M",x, y, 'l', height, -height, 'l', height, height,"z"]);
            shape.attr(editor.graphics._attributes.nodeShape);
            this._bareGenderShape = shape;
            shape = editor.paper.set(shape.glow({width: 5, fill: true, opacity: 0.1}).translate(3,3), shape);
            x = this.getX() - 2 * height/ 3;
            y = this.getY() + height/ 3;
            var deadShape = editor.paper.path(["M", x, y, 'l', height + height/3, -(height+ height/3), "z"]);
            deadShape.attr("stroke-width", 3);
            shape.push(deadShape);

            if(this.getNode().getGender() == 'U') {
                this._genderShape = shape;
            }
            else {
                x = this.getX();
                y = this.getY() + editor.graphics.getRadius()/1.4;
                var text = (this.getNode().getGender() == 'M') ? "Male" : "Female";
                var genderLabel = editor.paper.text(x, y, text).attr(editor.graphics._attributes.label);
                this._genderShape = editor.paper.set(shape, genderLabel);
            }
        }
        else {
            $super();
        }
    },

    getEvaluationLabels: function() {
        return this._evaluationLabels;
    },

    updateEvaluationLabels: function() {
        var evalLabels = [];
        this.getNode().getEvaluations().each( function(e) {
            //TODO: get evaluations from legend
            var label = editor.paper.text(e);
            evalLabels.push(label);
        });
        this._evaluationLabels = evalLabels;
    },

    updateNameLabel: function() {
        this._nameLabel && this._nameLabel.remove();
        var text =  "";
        this.getNode().getFirstName() && (text += this.getNode().getFirstName());
        this.getNode().getLastName() && (text += ' ' + this.getNode().getLastName());
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

    getDisorderShapes: function() {
        return this._disorderShapes;
    },

    updateDisorderShapes: function() {
        this._disorderShapes && this._disorderShapes.remove();
        var gradient = function(color, angle) {
            var hsb = Raphael.rgb2hsb(color),
                darker = Raphael.hsb2rgb(hsb['h'],hsb['s'],hsb['b']-.25)['hex'];
            return angle +"-"+darker+":0-"+color+":100";
        };
        var disorderShapes = editor.paper.set(),
            person = this.getNode();
        if(this.getNode().getLifeStatus() == 'aborted') {

            var side = editor.graphics.getRadius() * Math.sqrt(3.5),
                height = side/Math.sqrt(2),
                delta = (height * 2)/(person.getDisorders().length),
                x1 = this.getX() - height,
                y1 = this.getY();

            for(var k = 0; k < person.getDisorders().length; k++) {
                var corner = [];
                var x2 = x1 + delta;
                var y2 = this.getY() - (height - Math.abs(x2 - this.getX()));
                if (x1 < this.getX() && x2 >= this.getX()) {
                    corner = ["L", this.getX(), this.getY()-height];
                }
                var slice = editor.paper.path(["M", x1, y1, corner,"L", x2, y2, 'L',this.getX(), this.getY(),'z']),
                    color = gradient(editor.getLegend().getDisorder(this.getNode().getDisorders()[k]['id']).getColor(), 70);
                slice.attr({fill: color, 'stroke-width':.5 });
                disorderShapes.push(slice.attr({fill: color, 'stroke-width':.5 }));
                x1 = x2;
                y1 = y2;
            }
        }
        else {
            var disorderAngle = (person.getDisorders().length == 0)?0:(360/person.getDisorders().length).round();
            var delta = (360/(person.getDisorders().length))/2;

            for(var i = 0; i < person.getDisorders().length; i++) {
                var color = gradient(editor.getLegend().getDisorder(person.getDisorders()[i]['id']).getColor(), (i * disorderAngle)+delta);
                disorderShapes.push(sector(editor.paper, this.getX(), this.getY(), editor.graphics.getRadius(),
                    person.getGender(), i * disorderAngle, (i+1) * disorderAngle, color));
            }
            (disorderShapes.length < 2) && disorderShapes.attr('stroke', 'none');
        }
        this._disorderShapes = disorderShapes;
    },

    drawDeadShape: function() {
        var x = this.getX(),
            y = this.getY(),
            x1 = x - (10/8) * editor.graphics.getRadius(),
            y1 = y + (10/8) * editor.graphics.getRadius(),
            x2 = x + (10/8) * editor.graphics.getRadius(),
            y2 = y - (10/8) * editor.graphics.getRadius();
        this._deadShape = editor.paper.path(["M", x1,y1,"L",x2, y2]).attr("stroke-width", 3);
    },

    getDeadShape: function() {
        return this._deadShape;
    },

    getStillBirthLabel: function() {
        return this._stillBirthLabel;
    },

    getAgeLabel: function() {
        return this._ageLabel;
    },

    setAgeLabel: function(label) {
        this.getAgeLabel() && this.getAgeLabel().remove();
        this._ageLabel = label;
    },

    updateAgeLabel: function() {
        var text,
            person = this.getNode();
        if (person.isFetus() && person.getLifeStatus() == 'alive') {
            person.getConceptionDate() && (text = getAge(person.getConceptionDate(), null));
        }
        else if(person.getLifeStatus() == 'alive') {
            person.getBirthDate() && (text = getAge(person.getBirthDate(), null));
        }
        else {
            var prefix = (person.getConceptionDate()) ? '' : "d. ";
            if(person.getDeathDate() && person.getBirthDate()) {
                text = prefix + getAge(person.getBirthDate(), person.getDeathDate());
            }
            else if(person.getDeathDate() && person.getConceptionDate()) {
                text = prefix + getAge(person.getConceptionDate(), person.getDeathDate());
            }
            else if (person.getDeathDate()) {
                text = prefix + person.getDeathDate().getFullYear();
            }
            else if(person.getBirthDate()) {
                text = person.getBirthDate().getFullYear() + " - ?";
            }
        }
        text && (text = editor.paper.text(this.getX(), this.getY(), text));
        this.setAgeLabel(text);
    },

    getFetusShape: function() {
        return this._fetusShape;
    },

    setFetusShape: function(shape) {
        this._fetusShape && this._fetusShape.remove();
        this._fetusShape = shape;
    },

    updateFetusShape: function() {
        var fetusShape,
            person = this.getNode();
        if(person.isFetus() && person.getLifeStatus() == 'alive') {
            fetusShape = editor.paper.text(this.getX(), this.getY(), "P");
            fetusShape.attr(editor.graphics._attributes.fetusShape);
        }
        this.setFetusShape(fetusShape);
    },

    getSBLabel: function() {
        return this._stillBirthLabel;
    },

    updateSBLabel: function() {
        var SBLabel;
        this.getNode().getLifeStatus() == 'stillborn' && (SBLabel = editor.paper.text(this.getX(), this.getY(), "SB"));
        this.getSBLabel() && this.getSBLabel().remove();
        this._stillBirthLabel = SBLabel;
    },

    updateLifeStatusShapes: function() {
        var status = this.getNode().getLifeStatus();
        this.getDeadShape() && this.getDeadShape().remove();
        this.getHoverBox().unhidePartnerHandles();

        if(status == 'deceased'){
            this.drawDeadShape();
        }
        else if(status == 'stillborn') {
            this.getHoverBox().hidePartnerHandles();
            this.drawDeadShape();
        }
        else if(status == 'aborted') {
            this.getHoverBox().hidePartnerHandles();
        }
    },

    updateAdoptedShape: function() {
        this._adoptedShape && this._adoptedShape.remove();
        if(this.getNode().isAdopted()) {
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
    },

    getAdoptedShape: function() {
        return this._adoptedShape;
    },

    isSelected: function() {
        return this._isSelected;
    },

    setSelected: function(isSelected) {
        this._isSelected = isSelected;
        if(isSelected) {
            this.shiftLabels();
        }
        else {
            this.unshiftLabels();
        }
    },

    shiftLabels: function() {
        var labels = this.getLabels();
        for(var i = 0; i<labels.length; i++) {
            labels[i].stop().animate({"y": labels[i].oy + 50}, 200,">");
        }
    },

    unshiftLabels: function() {
        var labels = this.getLabels();
        for(var i = 0; i<labels.length; i++) {
            labels[i].stop().animate({"y": labels[i].oy}, 200,">");
        }
    },

    getLabels: function() {
        var labels = editor.paper.set();
        this.getNameLabel() && labels.push(this.getNameLabel());
        this.getSBLabel() && labels.push(this.getSBLabel());
        this.getAgeLabel() && labels.push(this.getAgeLabel());
        this.getEvaluationLabels() && this.getEvaluationLabels().each(function(l) {labels.push(l); });
        return labels;
    },

    drawLabels: function() {
        this.updateAgeLabel();
        this.updateNameLabel();
        this.updateSBLabel();
        this.updateEvaluationLabels();

        var labels = this.getLabels(),
            yOffset = (this.isSelected()) ? 50 : 0,
            startY = this.getY() + editor.graphics.getRadius() * 1.5 + yOffset;
        for (var i = 0; i < labels.length; i++) {
            labels[i].attr("y", startY + 11);
            labels[i].attr(editor.graphics._attributes.label);
            labels[i].toFront();
            labels[i].oy = (labels[i].attr("y") - yOffset);
            startY = labels[i].getBBox().y2;
        }
        this.getHoverBox().getFrontElements().toFront();
    },

    getShapes: function() {
        var lifeStatusShapes = editor.paper.set();
        this.getFetusShape() && lifeStatusShapes.push(this.getFetusShape());
        this.getDeadShape() && lifeStatusShapes.push(this.getDeadShape());
        this.getAdoptedShape() && lifeStatusShapes.push(this.getAdoptedShape());
        return editor.paper.set(this.getHoverBox().getBackElements(), this.getGenderShape(), this.getDisorderShapes(),
            lifeStatusShapes, this.getHoverBox().getFrontElements());
    },

    drawShapes: function($super) {
        this.updateFetusShape();
        this.updateLifeStatusShapes();
        this.updateGenderShape();
        this.updateDisorderShapes();
        this.updateAdoptedShape();
        $super();
    },

    getAllGraphics: function() {
        var graphics = editor.paper.set(this.getShapes());
        graphics.push(this.getLabels());
        graphics.push(this.getHoverBox().getFrontElements());
        return graphics;
    },

    draw: function($super) {
        this.drawLabels();
        $super();
    },

    moveTo: function(x, y) {
        var xDisplacement = x - this.getAbsX();
        var yDisplacement = y - this.getAbsY();
        var displacement = Math.sqrt(xDisplacement * xDisplacement + yDisplacement * yDisplacement);
        this.getHoverBox().disable();
        this.getAllGraphics().stop().animate({'transform': "t " + (x-this.getAbsX()) + "," +(y-this.getAbsY()) + "..."},
            displacement /.4, "<>", this.getHoverBox().enable.bind(this.getHoverBox()));
        this.setAbsX(x);
        this.setAbsY(y);
    }
});