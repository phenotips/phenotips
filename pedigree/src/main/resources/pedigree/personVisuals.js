/**
 * Class for organizing graphics for Person nodes.
 *
 * @class PersonVisuals
 * @extends AbstractPersonVisuals
 * @constructor
 * @param {Person} node The node for which the graphics are handled
 * @param {Number} x The x coordinate on the canvas
 * @param {Number} y The y coordinate on the canvas
 */

var PersonVisuals = Class.create(AbstractPersonVisuals, {
    
    initialize: function($super, node, x, y) {
        //var timer = new Timer();
    	//console.log("person visuals");
        $super(node, x, y);    	
        this._nameLabel = null;
        this._stillBirthLabel = null;
        this._ageLabel = null;
        this._childlessStatusLabel = null;
        this._disorderShapes = null;
        this._deadShape = null;
        this._unbornShape = null;
        this._childlessShape = null;
        this._isSelected = false;
        //console.log("person visuals end");
        //timer.printSinceLast("Person visuals time");
    },

    generateHoverbox: function(x, y) {
        if (editor.isReadOnlyMode()) {
            return new ReadOnlyHoverbox(this.getNode(), x, y, this.getGenderGraphics());
        } else {
            return new PersonHoverbox(this.getNode(), x, y, this.getGenderGraphics());
        }
    },    
 
    /**
     * Draws the icon for this Person depending on the gender, life status and whether this Person is the proband.
     * Updates the disorder shapes.
     *
     * @method setGenderGraphics
     */
    setGenderGraphics: function($super) {        
        //console.log("set gender graphics");
        if(this.getNode().getLifeStatus() == 'aborted') {
            this._genderGraphics && this._genderGraphics.remove();
            
            var radius = PedigreeEditor.attributes.radius;
            if (this.getNode().isPersonGroup())
                radius *= PedigreeEditor.attributes.groupNodesScale;  
            
            var side = radius * Math.sqrt(3.5),
                height = side/Math.sqrt(2),
                x = this.getX() - height,
                y = this.getY();
            var shape = editor.getPaper().path(["M",x, y, 'l', height, -height, 'l', height, height,"z"]);
            shape.attr(PedigreeEditor.attributes.nodeShape);
            this._genderShape = shape;
            shape = editor.getPaper().set(shape.glow({width: 5, fill: true, opacity: 0.1}).transform(["t",3,3,"..."]), shape);

            if(this.getNode().isProband()) {
                shape.transform(["...s", 1.07]);
                shape.attr("stroke-width", 5);
            }

            if(this.getNode().getGender() == 'U') {
                this._genderGraphics = shape;
            }
            else {
                x = this.getX();
                y = this.getY() + radius/1.4;
                var text = (this.getNode().getGender() == 'M') ? "Male" : "Female";
                var genderLabel = editor.getPaper().text(x, y, text).attr(PedigreeEditor.attributes.label);
                this._genderGraphics = editor.getPaper().set(shape, genderLabel);
            }
        }
        else {
            $super();
        }
        
        if(this.getNode().isProband()) {
            this._genderGraphics.push(this.generateProbandArrow());
            this.getGenderShape().transform(["...s", 1.08]);
            this.getGenderShape().attr("stroke-width", 5);
        }
        if(this.getHoverBox()) {
            this._genderGraphics.flatten().insertBefore(this.getFrontElements().flatten());
        }        
        this.updateDisorderShapes();      
    },
    
    generateProbandArrow: function() {        
        var icon = editor.getPaper().path(editor.getGraphicsSet().__probandArrowPath).attr({fill: "#595959", stroke: "none", opacity: 1});        
        icon.transform(["t" , this.getX()-this._shapeRadius-22, this.getY()+this._shapeRadius-11])
        return icon;
    },

    /**
     * Returns all graphical elements that are behind the gender graphics
     *
     * @method getBackElements
     * @return {Raphael.st}
     */
    getBackElements: function() {
        return this.getHoverBox().getBackElements().concat(editor.getPaper().set(this.getChildlessStatusLabel(), this.getChildlessShape()));
    },

    /**
     * Returns all graphical elements that should receive mouse focus/clicks
     *
     * @method getFrontElements
     * @return {Raphael.st}
     */    
    getFrontElements: function() {
        return this.getHoverBox().getFrontElements();
    },

    /**
     * Updates the name label for this Person
     *
     * @method updateNameLabel
     */
    updateNameLabel: function() {
        this._nameLabel && this._nameLabel.remove();
        var text =  "";
        this.getNode().getFirstName() && (text = this.getNode().getFirstName());
                
        if (this.getNode().getLastName()) {
            text += ' ' + this.getNode().getLastName();
            this.getNode().getLastNameAtBirth() && (text += ' (' + this.getNode().getLastNameAtBirth() + ')');
        }
        else
            this.getNode().getLastNameAtBirth() && (text += ' ' + this.getNode().getLastNameAtBirth());
        
        this._nameLabel && this._nameLabel.remove();
        if(text.strip() != '') {
            this._nameLabel = editor.getPaper().text(this.getX(), this.getY() + PedigreeEditor.attributes.radius, text).attr(PedigreeEditor.attributes.nameLabels);
        }
        else {
            this._nameLabel = null;
        }
        this.drawLabels();
    },

    /**
     * Returns the Person's name label
     *
     * @method getNameLabel
     * @return {Raphael.el}
     */
    getNameLabel: function() {
        return this._nameLabel;
    },

    /**
     * Returns colored blocks representing disorders
     *
     * @method getDisorderShapes
     * @return {Raphael.st} Set of disorder shapes
     */
    getDisorderShapes: function() {
        return this._disorderShapes;
    },

    /**
     * Displays the disorders currently registered for this node.
     *
     * @method updateDisorderShapes
     */
    updateDisorderShapes: function() {
        this._disorderShapes && this._disorderShapes.remove();
        if (this.getNode().getDisorders().length == 0) return;
        
        var gradient = function(color, angle) {
            var hsb = Raphael.rgb2hsb(color),
                darker = Raphael.hsb2rgb(hsb['h'],hsb['s'],hsb['b']-.25)['hex'];
            return angle +"-"+darker+":0-"+color+":100";
        };
        var disorderShapes = editor.getPaper().set(),
            person = this.getNode(),
            delta,
            color;

        if(this.getNode().getLifeStatus() == 'aborted') {

            var radius = PedigreeEditor.attributes.radius;
            if (this.getNode().isPersonGroup())
                radius *= PedigreeEditor.attributes.groupNodesScale;
            
            var side = radius * Math.sqrt(3.5),
                height = side/Math.sqrt(2),
                x1 = this.getX() - height,
                y1 = this.getY();
            delta = (height * 2)/(person.getDisorders().length);

            for(var k = 0; k < person.getDisorders().length; k++) {
                var corner = [];
                var x2 = x1 + delta;
                var y2 = this.getY() - (height - Math.abs(x2 - this.getX()));
                if (x1 < this.getX() && x2 >= this.getX()) {
                    corner = ["L", this.getX(), this.getY()-height];
                }
                var slice = editor.getPaper().path(["M", x1, y1, corner,"L", x2, y2, 'L',this.getX(), this.getY(),'z']);
                color = gradient(editor.getDisorderLegend().getDisorderColor(this.getNode().getDisorders()[k]), 70);
                disorderShapes.push(slice.attr({fill: color, 'stroke-width':.5, stroke: 'none' }));
                x1 = x2;
                y1 = y2;
            }
            if(this.getNode().isProband()) {
                disorderShapes.transform(["...s", 1.04, 1.04, this.getX(), this.getY()-this._shapeRadius]);
            }
        }
        else {
            var disorderAngle = (360/person.getDisorders().length).round();
            delta = (360/(person.getDisorders().length))/2;
            if (person.getDisorders().length == 1 && this.getNode().getGender() == 'U')
                delta -= 45; // since this will be rotated by shape transform later
            
            var radius = (this._shapeRadius-0.6);    // -0.6 to avoid disorder fills to overlap with shape borders (due to aliasing/Raphael pixel layout)
            if (this.getNode().getGender() == 'U')
                radius *= 1.155;                     // TODO: magic number hack: due to a Raphael transform bug (?) just using correct this._shapeRadius does not work
            
            for(var i = 0; i < person.getDisorders().length; i++) {
                color = gradient(editor.getDisorderLegend().getDisorderColor(person.getDisorders()[i]), (i * disorderAngle)+delta);
                disorderShapes.push(sector(editor.getPaper(), this.getX(), this.getY(), radius,
                    person.getGender(), i * disorderAngle, (i+1) * disorderAngle, color));
            }

            (disorderShapes.length < 2) ? disorderShapes.attr('stroke', 'none') : disorderShapes.attr({stroke: '#595959', 'stroke-width':.03});
            if(this.getNode().isProband()) {
                disorderShapes.transform(["...s", 1.04, 1.04, this.getX(), this.getY()]);
            }
        }
        this._disorderShapes = disorderShapes;
        this._disorderShapes.flatten().insertAfter(this.getGenderGraphics().flatten());
    },

    /**
     * Draws a line across the Person to display that he is dead (or aborted).
     *
     * @method drawDeadShape
     */
    drawDeadShape: function() {
        var strokeWidth = editor.getWorkspace().getSizeNormalizedToDefaultZoom(2.5);
        var x, y;
        if(this.getNode().getLifeStatus() == 'aborted') {
            var side   = PedigreeEditor.attributes.radius * Math.sqrt(3.5);
            var height = side/Math.sqrt(2);
            if (this.getNode().isPersonGroup())
                height *= PedigreeEditor.attributes.groupNodesScale;
            
            var x = this.getX() - height/1.5;
            if (this.getNode().isPersonGroup())
                x -= PedigreeEditor.attributes.radius/4;
            
            var y = this.getY() + height/3;
            this._deadShape = editor.getPaper().path(["M", x, y, 'l', height + height/3, -(height+ height/3), "z"]);
            this._deadShape.attr("stroke-width", strokeWidth);
        }
        else {
            x = this.getX();
            y = this.getY();
            var coeff = 10.0/8.0 * (this.getNode().isPersonGroup() ? PedigreeEditor.attributes.groupNodesScale : 1.0);
            var x1 = x - coeff * PedigreeEditor.attributes.radius,
                y1 = y + coeff * PedigreeEditor.attributes.radius,
                x2 = x + coeff * PedigreeEditor.attributes.radius,
                y2 = y - coeff * PedigreeEditor.attributes.radius;
            this._deadShape = editor.getPaper().path(["M", x1,y1,"L",x2, y2]).attr("stroke-width", strokeWidth);
        }
        this._deadShape.toFront();
        this._deadShape.node.setAttribute("class", "no-mouse-interaction");
    },

    /**
     * Returns the line drawn across a dead Person's icon
     *
     * @method getDeadShape
     * @return {Raphael.st}
     */
    getDeadShape: function() {
        return this._deadShape;
    },

    /**
     * Returns this Person's age label
     *
     * @method getAgeLabel
     * @return {Raphael.el}
     */
    getAgeLabel: function() {
        return this._ageLabel;
    },

    /**
     * Updates the age label for this Person
     *
     * @method updateAgeLabel
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
        this.getAgeLabel() && this.getAgeLabel().remove();
        this._ageLabel = text ? editor.getPaper().text(this.getX(), this.getY(), text).attr(PedigreeEditor.attributes.label) : null;
        this.drawLabels();
    },

    /**
     * Returns the shape marking a Person's 'unborn' life-status
     *
     * @method getUnbornShape
     * @return {Raphael.el}
     */
    getUnbornShape: function() {
        return this._unbornShape;
    },

    /**
     * Draws a "P" on top of the node to display this Person's 'unborn' life-status
     *
     * @method drawUnbornShape
     */
    drawUnbornShape: function() {
        this._unbornShape && this._unbornShape.remove();
        if(this.getNode().getLifeStatus() == 'unborn') {
            this._unbornShape = editor.getPaper().text(this.getX(), this.getY(), "P").attr(PedigreeEditor.attributes.unbornShape);
            this._unbornShape.insertBefore(this.getHoverBox().getFrontElements());
        }
    },

    /**
     * Returns this Person's stillbirth label
     *
     * @method getSBLabel
     * @return {Raphael.el}
     */
    getSBLabel: function() {
        return this._stillBirthLabel;
    },

    /**
     * Updates the stillbirth label for this Person
     *
     * @method updateSBLabel
     */
    updateSBLabel: function() {
        this.getSBLabel() && this.getSBLabel().remove();
        if (this.getNode().getLifeStatus() != 'stillborn') return;        
        this._stillBirthLabel = editor.getPaper().text(this.getX(), this.getY(), "SB").attr(PedigreeEditor.attributes.label);
        this.drawLabels();
    },

    /**
     * Displays the correct graphics to represent the current life status for this Person.
     *
     * @method updateLifeStatusShapes
     */
    updateLifeStatusShapes: function() {
        var status = this.getNode().getLifeStatus();
        
        this.getDeadShape()   && this.getDeadShape().remove();
        this.getUnbornShape() && this.getUnbornShape().remove();
        this.getSBLabel()     && this.getSBLabel().remove();
        
        this.setGenderGraphics();
        
        if(status == 'deceased' || status == 'aborted') {
            this.drawDeadShape();
        }
        else if (status == 'stillborn') {
            this.drawDeadShape();
            this.updateSBLabel();
        }
        else if (status == 'unborn') {
            this.drawUnbornShape();
        }        
    },

    /**
     * Marks this node as hovered, and moves the labels out of the way
     *
     * @method setSelected
     */
    setSelected: function($super, isSelected) {
        $super(isSelected);
        if(isSelected) {
            this.shiftLabels();
        }
        else {
            this.unshiftLabels();
        }
    },

    /**
     * Moves the labels down to make space for the hoverbox
     *
     * @method shiftLabels
     */
    shiftLabels: function() {
        var shift  = this._labelSelectionOffset(); 
        var labels = this.getLabels();
        for(var i = 0; i<labels.length; i++) {
            labels[i].stop().animate({"y": labels[i].oy + shift}, 200,">");
        }
    },

    /**
     * Animates the labels of this node to their original position under the node
     *
     * @method unshiftLabels
     */
    unshiftLabels: function() {
        var labels = this.getLabels();
        var firstLable = this._childlessStatusLabel ? 1 : 0;
        for(var i = 0; i<labels.length; i++) {
            labels[i].stop().animate({"y": labels[i].oy}, 200,">");
        }
    },

    /**
     * Returns set of labels for this Person
     *
     * @method getLabels
     * @return {Raphael.st}
     */
    getLabels: function() {
        var labels = editor.getPaper().set();        
        this.getSBLabel() && labels.push(this.getSBLabel());
        this.getNameLabel() && labels.push(this.getNameLabel());
        this.getAgeLabel() && labels.push(this.getAgeLabel());
        return labels;
    },

    /**
     * Displays all the appropriate labels for this Person in the correct layering order
     *
     * @method drawLabels
     */
    drawLabels: function() {
        var labels = this.getLabels();
        var selectionOffset = this._labelSelectionOffset();
        var childlessOffset = this.getChildlessStatusLabel() ? PedigreeEditor.attributes.label['font-size'] : 0;
        childlessOffset += ((this.getNode().getChildlessStatus() !== null) ? (PedigreeEditor.attributes.infertileMarkerHeight + 2) : 0);
                    
        var lowerBound = PedigreeEditor.attributes.radius * (this.getNode().isPersonGroup() ? PedigreeEditor.attributes.groupNodesScale : 1.0);
        var startY = this.getY() + lowerBound * 1.8 + selectionOffset + childlessOffset;
        for (var i = 0; i < labels.length; i++) {
            labels[i].attr("y", startY);
            //labels[i].attr(PedigreeEditor.attributes.label);
            labels[i].oy = (labels[i].attr("y") - selectionOffset);
            startY = labels[i].getBBox().y2 + 11;
        }
        labels.flatten().insertBefore(this.getHoverBox().getFrontElements().flatten());
    },
    
    _labelSelectionOffset: function() {
        var selectionOffset = this.isSelected() ? PedigreeEditor.attributes.radius/1.4 : 0;
        if (this.getChildlessStatusLabel())
            selectionOffset = selectionOffset/2;
        return selectionOffset;        
    },

    /**
     * Returns set with the gender icon, disorder shapes and life status shapes.
     *
     * @method getShapes
     * @return {Raphael.st}
     */
    getShapes: function($super) {
        var lifeStatusShapes = editor.getPaper().set();
        this.getUnbornShape() && lifeStatusShapes.push(this.getUnbornShape());
        this.getChildlessShape() && lifeStatusShapes.push(this.getChildlessShape());
        this.getChildlessStatusLabel() && lifeStatusShapes.push(this.getChildlessStatusLabel());
        this.getDeadShape() && lifeStatusShapes.push(this.getDeadShape());
        return $super().concat(editor.getPaper().set(this.getDisorderShapes(), lifeStatusShapes));
    },

    /**
     * Returns all the graphics and labels associated with this Person.
     *
     * @method getAllGraphics
     * @return {Raphael.st}
     */
    getAllGraphics: function($super) {
        //console.log("Node " + this.getNode().getID() + " getAllGraphics");
        return $super().push(this.getHoverBox().getBackElements(), this.getLabels(), this.getHoverBox().getFrontElements());
    },

    /**
     * Changes the position of the node to (x,y)
     *
     * @method setPos
     * @param [$super]
     * @param {Number} x the x coordinate on the canvas
     * @param {Number} y the y coordinate on the canvas
     * @param {Boolean} animate set to true if you want to animate the transition
     * @param {Function} callback a function that will be called at the end of the animation
     */
    setPos: function($super, x, y, animate, callback) {
        var funct = callback;        
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

    /**
     * [Helper for setPos] Saves the x and y values as current coordinates and updates connections with the new position
     *
     * @method _updatePositionData
     * @param [$super]
     * @param {Number} x The new x coordinate
     * @param {Number} y The new y coordinate
     * @private
     */
    //_updatePositionData: function($super, x, y) {
    //    this.getHoverBox().enable.bind(this.getHoverBox());
    //    $super(x, y)
    //}
});

//ATTACHES CHILDLESS BEHAVIOR METHODS
PersonVisuals.addMethods(ChildlessBehaviorVisuals);