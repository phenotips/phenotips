/*
 * A class responsible for handling the visual representation of a Person. The graphical elements include
 * the general shape of the node, the disorders, information labels and life status shapes.
 *
 * @param node the AbstractPerson object for which this graphics are handled
 * @param x the x coordinate on the canvas
 * @param x the y coordinate on the canvas
 */

var PersonVisuals = Class.create(AbstractPersonVisuals, {
    
    initialize: function($super, node, x, y) {
        this._nameLabel = null;
        this._stillBirthLabel = null;
        this._ageLabel = null;
        this._disorderShapes = null;
        this._deadShape = null;
        this._adoptedShape = null;
        this._unbornShape = null;
        this._isSelected = false;
        $super(node, x, y);
        this._hoverBox = new PersonHoverbox(node, x, y, this.getGenderSymbol());
    },

    /*
     * Returns the PersonHoverbox object for this Person
     */
    getHoverBox: function() {
        return this._hoverBox;
    },

    /*
     * Draws the icon for this Person depending on the gender, life status and whether this Person is the proband.
     * Updates the disorder shapes.
     */
    setGenderSymbol: function($super) {
        if(this.getNode().getLifeStatus() == 'aborted') {
            this._genderSymbol && this._genderSymbol.remove();
            var side = editor.attributes.radius * Math.sqrt(3.5),
                height = side/Math.sqrt(2),
                x = this.getX() - height,
                y = this.getY();
            var shape = editor.getPaper().path(["M",x, y, 'l', height, -height, 'l', height, height,"z"]);
            shape.attr(editor.attributes.nodeShape);
            this._genderShape = shape;
            shape = editor.getPaper().set(shape.glow({width: 5, fill: true, opacity: 0.1}).transform(["t",3,3,"..."]), shape);

            if(this.getNode().isProband()) {
                shape.transform(["...s", 1.07]);
                shape.attr("stroke-width", 5);
            }

            if(this.getNode().getGender() == 'U') {
                this._genderSymbol = shape;
            }
            else {
                x = this.getX();
                y = this.getY() + editor.attributes.radius/1.4;
                var text = (this.getNode().getGender() == 'M') ? "Male" : "Female";
                var genderLabel = editor.getPaper().text(x, y, text).attr(editor.attributes.label);
                this._genderSymbol = editor.getPaper().set(shape, genderLabel);
            }
        }
        else {
            $super();
        }
        if(this.getNode().isProband()) {
            this.getGenderShape().transform(["...s", 1.07]);
            this.getGenderShape().attr("stroke-width", 5);
        }
        if(this.getHoverBox()) {
            this._genderSymbol.flatten().insertAfter(this.getHoverBox().getBackElements().flatten());
        }
        else if (!this.getNode().isProband()) {
            this._genderSymbol.flatten().insertAfter(editor.getProband().getGraphics().getAllGraphics().flatten());
        }
        this.updateDisorderShapes();
    },
    /*
     * Updates the name label for this Person
     */
    updateNameLabel: function() {
        this._nameLabel && this._nameLabel.remove();
        var text =  "";
        this.getNode().getFirstName() && (text += this.getNode().getFirstName());
        this.getNode().getLastName() && (text += ' ' + this.getNode().getLastName());
        if(text.strip() != ''){
            this.setNameLabel(editor.getPaper().text(this.getRelativeX(), this.getRelativeY() + editor.attributes.radius, text));
            this.getNameLabel().attr({'font-size': 18, 'font-family': 'Cambria'});
        }
        else {
            this.setNameLabel(null);
        }
    },

    /*
     * Returns the Raphaël element for this Person's name label
     */
    getNameLabel: function() {
        return this._nameLabel;
    },

    setNameLabel: function(label) {
        this._nameLabel && this._nameLabel.remove();
        this._nameLabel = label;
    },

    /*
     * Returns the Raphaël set with colorful blocks representing disorders
     */
    getDisorderShapes: function() {
        return this._disorderShapes;
    },

    /*
     * Displays the disorders currently registered for this node.
     */
    updateDisorderShapes: function() {
        this._disorderShapes && this._disorderShapes.remove();
        var gradient = function(color, angle) {
            var hsb = Raphael.rgb2hsb(color),
                darker = Raphael.hsb2rgb(hsb['h'],hsb['s'],hsb['b']-.25)['hex'];
            return angle +"-"+darker+":0-"+color+":100";
        };
        var disorderShapes = editor.getPaper().set(),
            person = this.getNode();
        if(this.getNode().getLifeStatus() == 'aborted') {

            var side = editor.attributes.radius * Math.sqrt(3.5),
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
                var slice = editor.getPaper().path(["M", x1, y1, corner,"L", x2, y2, 'L',this.getX(), this.getY(),'z']),
                    color = gradient(editor.getLegend().getDisorder(this.getNode().getDisorders()[k]['id']).getColor(), 70);
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
                disorderShapes.push(sector(editor.getPaper(), this.getX(), this.getY(), editor.attributes.radius,
                    person.getGender(), i * disorderAngle, (i+1) * disorderAngle, color));
            }

            (disorderShapes.length < 2) ? disorderShapes.attr('stroke', 'none') : disorderShapes.attr({stroke: '#595959', 'stroke-width':.03});
        }
        this._disorderShapes = disorderShapes;
    },

    /*
     * Draws a line across the Person to display that he is dead (or aborted).
     */
    drawDeadShape: function() {
        var x, y;
        if(this.getNode().getLifeStatus() == 'aborted') {
            var side = editor.attributes.radius * Math.sqrt(3.5),
                height = side/Math.sqrt(2);
            x = this.getX() - height/1.5;
            y = this.getY() + height/3;
            this._deadShape = editor.getPaper().path(["M", x, y, 'l', height + height/3, -(height+ height/3), "z"]);
            this._deadShape.attr("stroke-width", 3);
        }
        else {
            x = this.getX();
            y = this.getY();
            var
                x1 = x - (10/8) * editor.attributes.radius,
                y1 = y + (10/8) * editor.attributes.radius,
                x2 = x + (10/8) * editor.attributes.radius,
                y2 = y - (10/8) * editor.attributes.radius;
            this._deadShape = editor.getPaper().path(["M", x1,y1,"L",x2, y2]).attr("stroke-width", 3);
        }
        this._deadShape.insertAfter(this.getHoverBox().getFrontElements().flatten());
    },

    /*
     * Returns the Raphaël element for the line drawn across a dead Person
     */
    getDeadShape: function() {
        return this._deadShape;
    },

    /*
     * Returns the Raphaël element for this Person's age label
     */
    getAgeLabel: function() {
        return this._ageLabel;
    },

    setAgeLabel: function(label) {
        this.getAgeLabel() && this.getAgeLabel().remove();
        this._ageLabel = label;
    },

    /*
     * Updates the age label for this Person
     */
    updateAgeLabel: function() {
        var text,
            person = this.getNode();
        if (person.isFetus()) {
            var date = person.getGestationAge();
            text = (date) ? date + " weeks" : null;
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
        text && (text = editor.getPaper().text(this.getRelativeX(), this.getRelativeY(), text));
        this.setAgeLabel(text);
    },

    /*
     * Returns the Raphaël element used to display this Person's 'unborn' life-status
     */
    getUnbornShape: function() {
        return this._unbornShape;
    },

    /*
     * Draws a "P" on top of the node to display this Person's 'unborn' life-status
     */
    drawUnbornShape: function() {
        this._unbornShape && this._unbornShape.remove();
        if(this.getNode().getLifeStatus() == 'unborn') {
            this._unbornShape = editor.getPaper().text(this.getX(), this.getY(), "P").attr(editor.attributes.unbornShape);
            this._unbornShape.insertBefore(this.getHoverBox().getFrontElements());
        }
    },

    /*
     * Returns the Raphaël element for this Person's age label
     */
    getSBLabel: function() {
        return this._stillBirthLabel;
    },

    /*
     * Updates the stillbirth label for this Person
     */
    updateSBLabel: function() {
        var SBLabel;
        this.getNode().getLifeStatus() == 'stillborn' && (SBLabel = editor.getPaper().text(this.getX(), this.getY(), "SB"));
        this.getSBLabel() && this.getSBLabel().remove();
        this._stillBirthLabel = SBLabel;
        this.drawLabels();
    },

    /*
     * Displays the correct graphics to represent the current life status for this Person.
     */
    updateLifeStatusShapes: function() {
        var status = this.getNode().getLifeStatus();
        this.getDeadShape() && this.getDeadShape().remove();
        this.getUnbornShape() && this.getUnbornShape().remove();
        this.setGenderSymbol();
        (!this.getNode().getPartnerships()[0]) && this.getHoverBox().unhideChildHandle();
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
        else if(status == 'unborn') {
            this.drawUnbornShape();
        }
        if(this.getNode().isFetus()){
            this.getHoverBox().hidePartnerHandles();
            this.getHoverBox().hideChildHandle();
        }
    },

    /*
     * Draws brackets around the node icon to show that this node is adopted
     */
    drawAdoptedShape: function() {
        this._adoptedShape && this._adoptedShape.remove();
        var r = editor.attributes.radius,
            x1 = this.getX() - ((0.8) * r),
            x2 = this.getX() + ((0.8) * r),
            y = this.getY() - ((1.3) * r),
            brackets = "M" + x1 + " " + y + "l" + r/(-2) +
                " " + 0 + "l0 " + (2.6 * r) + "l" + (r)/2 + " 0M" + x2 +
                " " + y + "l" + (r)/2 + " 0" + "l0 " + (2.6 * r) + "l" +
                (r)/(-2) + " 0";
        this._adoptedShape = editor.getPaper().path(brackets).attr("stroke-width", 3);
        this._adoptedShape.insertBefore(this.getHoverBox().getFrontElements().flatten());
    },

    /*
     * Removes the brackets around the node icon that show that this node is adopted
     */
    removeAdoptedShape: function() {
        this._adoptedShape && this._adoptedShape.remove();
    },

    /*
     * Returns the raphael element or set containing the adoption shape
     */
    getAdoptedShape: function() {
        return this._adoptedShape;
    },

    /*
     * Returns true if this node is hovered
     */
    isSelected: function() {
        return this._isSelected;
    },

    /*
     * Marks this node as hovered, and moves the labels out of the way
     */
    setSelected: function($super, isSelected) {
        $super();
        if(isSelected) {
            this.shiftLabels();
        }
        else {
            this.unshiftLabels();
        }
    },

    /*
     * Moves the labels down to make space for the hoverbox
     */
    shiftLabels: function() {
        var labels = this.getLabels();
        for(var i = 0; i<labels.length; i++) {
            labels[i].stop().animate({"y": labels[i].oy + 50}, 200,">");
        }
    },
    /*
     * Animates the labels of this node to their original position under the node
     */
    unshiftLabels: function() {
        var labels = this.getLabels();
        for(var i = 0; i<labels.length; i++) {
            labels[i].stop().animate({"y": labels[i].oy}, 200,">");
        }
    },

    /*
     * Returns a Raphael set or element that contains the labels
     */
    getLabels: function() {
        var labels = editor.getPaper().set();
        this.getNameLabel() && labels.push(this.getNameLabel());
        this.getSBLabel() && labels.push(this.getSBLabel());
        this.getAgeLabel() && labels.push(this.getAgeLabel());
        this.getEvaluationLabels() && this.getEvaluationLabels().each(function(l) {labels.push(l); });
        return labels;
    },

    /*
     * Displays all the appropriate labels for this Person in the correct order
     */
    drawLabels: function() {
        var labels = this.getLabels(),
            yOffset = (this.isSelected()) ? editor.attributes.radius/1.5 : 0,
            startY = this.getY() + editor.attributes.radius * 1.5 + yOffset;
        for (var i = 0; i < labels.length; i++) {
            labels[i].attr("y", startY + 11);
            labels[i].attr(editor.attributes.label);
            labels[i].oy = (labels[i].attr("y") - yOffset);
            startY = labels[i].getBBox().y2;
        }
        labels.flatten().insertBefore(this.getHoverBox().getFrontElements().flatten());
    },

    /*
     * Returns a Raphael set with the gender icon, disorder shapes and life status shapes.
     */
    getShapes: function($super) {
        var lifeStatusShapes = editor.getPaper().set();
        this.getUnbornShape() && lifeStatusShapes.push(this.getUnbornShape());
        this.getDeadShape() && lifeStatusShapes.push(this.getDeadShape());
        this.getAdoptedShape() && lifeStatusShapes.push(this.getAdoptedShape());
        return $super().concat(editor.getPaper().set(this.getDisorderShapes(), lifeStatusShapes));
    },

    /*
     * Returns a Raphael set or element that contains all the graphics and labels associated with this Person.
     */
    getAllGraphics: function($super) {
        return $super().push(this.getHoverBox().getBackElements(), this.getLabels(), this.getHoverBox().getFrontElements());
    },

    /*
     * Changes the position of the node to (X,Y)
     *
     * @param x the x coordinate on the canvas
     * @param y the y coordinate on the canvas
     * @param animate set to true if you want to animate the transition
     * @param callback a function that will be called at the end of the animation
     */
    setPos: function($super, x, y, animate, callback) {
    var funct;
        if(animate) {
            var me = this;
            this.getHoverBox().disable();
            funct = function () {
                me.getHoverBox().enable();
                callback && callback();
            };
        }
        $super(x, y, animate, funct);
    },

    /*
     * [Helper for setPos] Saves the x and y values as current coordinates and updates connections with the new position
     *
     * @param x the new x coordinate
     * @param y the new y coordinate
     */
    updatePositionData: function($super, x, y) {
        this.getHoverBox().enable.bind(this.getHoverBox());
        $super(x, y)
    }
});