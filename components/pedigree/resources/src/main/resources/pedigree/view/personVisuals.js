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
define([
        "pedigree/pedigreeEditorParameters",
        "pedigree/model/helpers",
        "pedigree/view/abstractPersonVisuals",
        "pedigree/view/ageCalc",
        "pedigree/view/childlessBehaviorVisuals",
        "pedigree/view/graphicHelpers",
        "pedigree/view/personHoverbox",
        "pedigree/view/readonlyHoverbox"
    ], function(
        PedigreeEditorParameters,
        Helpers,
        AbstractPersonVisuals,
        AgeCalc,
        ChildlessBehaviorVisuals,
        GraphicHelpers,
        PersonHoverbox,
        ReadOnlyHoverbox
    ){
    var PersonVisuals = Class.create(AbstractPersonVisuals, {

        initialize: function($super, node, x, y) {
            //var timer = new Helpers.Timer();
            $super(node, x, y);
            this._linkLabel = null;
            this._linkArea = null;
            this._nameLabel = null;
            this._stillBirthLabel = null;
            this._ageLabel = null;
            this._externalIDLabel = null;
            this._commentsLabel = null;
            this._cancerAgeOfOnsetLabels = {};
            this._childlessStatusLabel = null;
            this._disorderShapes = null;
            this._deadShape = null;
            this._unbornShape = null;
            this._childlessShape = null;
            this._isSelected = false;
            this._carrierGraphic = null;
            this._evalGraphic = null;
            this._awGraphic = null;
            //timer.printSinceLast("Person visuals time");
        },

        generateHoverbox: function(x, y) {
            if (editor.isReadOnlyMode()) {
                return new ReadOnlyHoverbox(this.getNode(), x, y, this.getGenderGraphics());
            } else {
                return new PersonHoverbox(this.getNode(), x, y, this.getGenderGraphics(), this.getNodeMenu());
            }
        },

        getNodeMenu: function() {
            return editor.getNodeMenu();
        },

        /**
         * Draws the icon for this Person depending on the gender, life status and whether this Person is the proband.
         * Updates the disorder shapes.
         *
         * @method setGenderGraphics
         */
        setGenderGraphics: function($super) {
            // this method may be slow yet is called after some property updates
            // skip it while initial properties are set and execute once after all labels have been generated
            if (this.getNode()._speedup_NOREDRAW) {
                this.getNode()._speedup_NEEDTOCALL["setGenderGraphics"] = true;
                return;
            }

            this.unmark();
            this._genderGraphics && this._genderGraphics.remove();

            //console.log("set gender graphics");
            if(this.getNode().getLifeStatus() == 'aborted' || this.getNode().getLifeStatus() == 'miscarriage') {
                var radius = PedigreeEditorParameters.attributes.radius;
                if (this.getNode().isPersonGroup())
                    radius *= PedigreeEditorParameters.attributes.groupNodesScale;
                this._shapeRadius = radius;

                var side = radius * Math.sqrt(3.5),
                    height = side/Math.sqrt(2),
                    x = this.getX() - height,
                    y = this.getY();
                var shape = editor.getPaper().path(["M",x, y, 'l', height, -height, 'l', height, height,"z"]);
                shape.attr(PedigreeEditorParameters.attributes.nodeShapeAborted);
                this._genderShape = shape;
                if (editor.getPreferencesManager().getConfigurationOption("drawNodeShadows")) {
                    var shadow = this.makeNodeShadow(shape);
                    shape = editor.getPaper().set(shadow, shape);
                } else {
                    shape = editor.getPaper().set(shape);
                }

                var gender = this.getNode().getGender();
                if(gender == 'U' || gender == 'O') {
                    this._genderGraphics = shape;
                }
                else {
                    x = this.getX();
                    y = this.getY() + radius/1.4;
                    var text = (gender == 'M') ? "Male" : "Female";
                    var genderLabel = editor.getPaper().text(x, y, text).attr(PedigreeEditorParameters.attributes.label);
                    this._genderGraphics = editor.getPaper().set(shape, genderLabel);
                }
            }
            else {
                $super();
            }

            this._genderShape.node.setAttribute("class", "node-shape-" + this.getNode().getID());

            if (this.getNode().isProband()) {
                // add arrow to the proband
                //
                // add a "P" marking to indicate that this arow is for the proband
                // if the patient is also the current patient, hide the "P"
                var showP = (!editor.isFamilyPage() && this.getNode().getPhenotipsPatientId() != editor.getGraph().getCurrentPatientId());
                this._genderGraphics.push(this.generateNodePointingArrow("P", showP));

                this.getGenderShape().node.setAttribute("isProband", "true");

                // slightly highlight proband when it is NOT the current node
                this.getGenderShape().transform(["...s", 1.05]);
                this.getGenderShape().attr("stroke-width", 3.0);
            } else if (this.getNode().getPhenotipsPatientId() == editor.getGraph().getCurrentPatientId()) {
                // add arrow for the current patient
                this._genderGraphics.push(this.generateNodePointingArrow("C", true));
            }

            if(!editor.isReadOnlyMode() && this.getHoverBox()) {
                this._genderGraphics.flatten().insertBefore(this.getFrontElements().flatten());
            }
            this.updateDisorderShapes();
            this.updateCarrierGraphic();
            this.updateEvaluationGraphic();
            if (this.getNode().getLifeStatus() == "unborn"){
                this.updateLifeStatusShapes("unborn");
            }
        },

        generateNodePointingArrow: function(drawLetter, showLetter) {
            var icon = editor.getPaper().path(editor.getView().__probandArrowPath).attr({fill: "#595959", stroke: "none", opacity: 1});
            icon.node.setAttribute("class", "node-arrow-shape node-arrow-type-" + drawLetter);
            if (this.getNode().getAdopted() != "") {
                var x = this.getX()-78;
                var y = this.getY()+34;
            } else {
                var x = this.getX()-this._shapeRadius-28;
                var y = this.getY()+this._shapeRadius-14;
                if(this.getNode().getLifeStatus() == 'deceased' || this.getNode().getLifeStatus() == 'stillborn') {
                    x -= 6;
                    y -= 10;
                }
                if(this.getNode().getLifeStatus() == 'aborted' || this.getNode().getLifeStatus() == 'miscarriage') {
                    x -= 12;
                    y -= 30;
                } else if (this.getNode().getGender() == 'F') {
                    x += 6;
                    y -= 6;
                } else if (this.getNode().getGender() == 'U' || this.getNode().getGender() == 'O') {
                    x += 2;
                    y -= 2;
                }
            }
            icon.transform(["t" , x, y])
            if (drawLetter && !this._pText) {
                var position = (drawLetter == "C") ? x-4 : x-2;
                var letter = editor.getPaper().text(position, y+21, drawLetter).attr(PedigreeEditorParameters.attributes.probandArrowLabel);
                letter.node.setAttribute("class", "node-arrow-text node-arrow-text-type-" + drawLetter);
                if (!showLetter) {
                    letter.node.setAttribute("class", "hidden");
                }
                icon = editor.getPaper().set(icon, letter);
            }
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
         * Updates the external ID label for this Person
         *
         * @method updateExternalIDLabel
         */
        updateExternalIDLabel: function() {
            this._externalIDLabel && this._externalIDLabel.remove();
            if (!editor.getPreferencesManager().getConfigurationOption("replaceIdWithExternalID")) {
                if (this.getNode().getExternalID()) {
                    var text = '[ ' + this.getNode().getExternalID() + " ]";
                    this._externalIDLabel = editor.getPaper().text(this.getX(), this.getY() + PedigreeEditorParameters.attributes.radius, text).attr(PedigreeEditorParameters.attributes.externalIDLabels);
                } else {
                    this._externalIDLabel = null;
                }
                this.drawLabels();
            } else {
                this._externalIDLabel = null;
                this.updateLinkLabel();
            }
        },

        /**
         * Returns the Person's external ID label
         *
         * @method getExternalIDLabel
         * @return {Raphael.el}
         */
        getExternalIDLabel: function() {
            return this._externalIDLabel;
        },

        /**
         * Updates the name label for this Person
         *
         * @method updateNameLabel
         */
        updateNameLabel: function() {
            this._nameLabel && this._nameLabel.remove();
            var disabledFields = editor.getPreferencesManager().getConfigurationOption("disabledFields");
            var text =  "";

            if (this.getNode().getFirstName() && !Helpers.arrayContains(disabledFields, 'first_name')) {
                text = this.getNode().getFirstName();
            }

            var lastNameAtBirth = (this.getNode().getLastNameAtBirth() && !Helpers.arrayContains(disabledFields, 'last_name_birth')) ?
                this.getNode().getLastNameAtBirth() : "";

            if (this.getNode().getLastName() && !Helpers.arrayContains(disabledFields, 'last_name')) {
                text += ' ' + this.getNode().getLastName();
                if (lastNameAtBirth == this.getNode().getLastName() || lastNameAtBirth === "") {
                    lastNameAtBirth = "";
                } else {
                    lastNameAtBirth = "(" + lastNameAtBirth + ")";
                }
            }

            text += " " + lastNameAtBirth;

            this._nameLabel && this._nameLabel.remove();
            if(text.strip() != '') {
                this._nameLabel = editor.getPaper().text(this.getX(), this.getY() + PedigreeEditorParameters.attributes.radius, text).attr(PedigreeEditorParameters.attributes.nameLabels);
                this._nameLabel.node.setAttribute("class", "field-no-user-select");
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
         * Updates the death details label for this Person
         *
         * @method updateDeathDetailsLabel
         */
        updateDeathDetailsLabel: function() {
            this._deathDetailsLabel && this._deathDetailsLabel.remove();
            var deceasedAgeKnown = (this.getNode().getDeceasedAge() != "");
            var deceasedCauseKnown = (this.getNode().getDeceasedCause() != "");
            if (!deceasedAgeKnown && !deceasedCauseKnown) {
                this._deathDetailsLabel = null;
            } else {
                // one of:
                //  a) (AGE, cause) or  (cause) or (AGE) -> if death date is known, "d." will already be present on the line above
                //  b) d. AGE (cause) or (d. cause) -> otherwise add a "d." inside or outside the brackets
                var deathDateKnown = this.getNode().getDeathDate() != null && this.getNode().getDeathDate().isComplete();

                var text = "";
                if (deathDateKnown) {
                    text += "(";
                    text += deceasedAgeKnown ? this.getNode().getDeceasedAge() : "";
                    text += (deceasedAgeKnown && deceasedCauseKnown) ? ", " : "";
                    text += deceasedCauseKnown ? this.getNode().getDeceasedCause() : "";
                    text += ")";
                } else {
                    text += deceasedAgeKnown ? ("d. " + this.getNode().getDeceasedAge() + (deceasedCauseKnown ? " (" : "")) : "(d. ";
                    text += deceasedCauseKnown ? this.getNode().getDeceasedCause() : "";
                    text += deceasedCauseKnown ? ")" : "";
                }
                this._deathDetailsLabel = editor.getPaper().text(this.getX(), this.getY() + PedigreeEditorParameters.attributes.radius, text).attr(PedigreeEditorParameters.attributes.externalIDLabels);
                this._deathDetailsLabel.addGapAfter = true;
            }
            this.drawLabels();
        },

        /**
         * Returns the Person's death details label
         *
         * @method getDeathDetailsLabel
         * @return {Raphael.el}
         */
        getDeathDetailsLabel: function() {
            return this._deathDetailsLabel;
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
            var colors = this.getNode().getAllNodeColors();
            if (colors.length == 0) return;

            var gradient = function(color, angle) {
                var hsb = Raphael.rgb2hsb(color),
                    darker = Raphael.hsb2rgb(hsb['h'],hsb['s'],hsb['b']-.25)['hex'];
                return angle +"-"+darker+":0-"+color+":100";
            };
            var disorderShapes = editor.getPaper().set();
            var delta, color;

            if (this.getNode().getLifeStatus() == 'aborted' || this.getNode().getLifeStatus() == 'miscarriage') {
                var radius = PedigreeEditorParameters.attributes.radius;
                if (this.getNode().isPersonGroup())
                    radius *= PedigreeEditorParameters.attributes.groupNodesScale;

                var side = radius * Math.sqrt(3.5),
                    height = side/Math.sqrt(2),
                    x1 = this.getX() - height,
                    y1 = this.getY();
                delta = (height * 2)/(colors.length);

                for(var k = 0; k < colors.length; k++) {
                    var corner = [];
                    var x2 = x1 + delta;
                    var y2 = this.getY() - (height - Math.abs(x2 - this.getX()));
                    if (x1 < this.getX() && x2 >= this.getX()) {
                        corner = ["L", this.getX(), this.getY()-height];
                    }
                    var slice = editor.getPaper().path(["M", x1, y1, corner,"L", x2, y2, 'L',this.getX(), this.getY(),'z']);
                    color = colors[k];
                    disorderShapes.push(slice.attr({fill: color, 'stroke-width':.5, stroke: 'none' }));
                    x1 = x2;
                    y1 = y2;
                }
                if(this.getNode().isProband()) {
                    disorderShapes.transform(["...s", 1.04, 1.04, this.getX(), this.getY()-this._shapeRadius]);
                }
            }
            else {
                var disorderAngle = (360/colors.length);
                delta = (360/(colors.length))/2;
                if (colors.length == 1 && (this.getNode().getGender() == 'U' || this.getNode().getGender() == 'O'))
                    delta -= 45; // since this will be rotated by shape transform later

                var radius = (this._shapeRadius-0.25);    // -0.25 to avoid disorder fills to overlap with shape borders (due to aliasing/Raphael pixel layout)
                if (this.getNode().getGender() == 'U' || this.getNode().getGender() == 'O')
                    radius *= 1.155;                     // TODO: magic number hack: due to a Raphael transform bug (?) just using correct this._shapeRadius does not work

                for(var i = 0; i < colors.length; i++) {
                    color = colors[i];
                    disorderShapes.push(GraphicHelpers.sector(editor.getPaper(), this.getX(), this.getY(), radius,
                                        this.getNode().getGender(), i * disorderAngle, (i+1) * disorderAngle, color));
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
            this._deadShape && this._deadShape.remove();
            var strokeWidth = editor.getWorkspace().getSizeNormalizedToDefaultZoom(2.5);
            var x, y;
            if (this.getNode().getLifeStatus() == 'aborted') {
                var side   = PedigreeEditorParameters.attributes.radius * Math.sqrt(3.5);
                var height = side/Math.sqrt(2);
                if (this.getNode().isPersonGroup())
                    height *= PedigreeEditorParameters.attributes.groupNodesScale;

                var x = this.getX() - height/1.5;
                if (this.getNode().isPersonGroup())
                    x -= PedigreeEditorParameters.attributes.radius/4;

                var y = this.getY() + height/3;
                this._deadShape = editor.getPaper().path(["M", x, y, 'l', height + height/3, -(height+ height/3), "z"]);
                this._deadShape.attr("stroke-width", strokeWidth);
            }
            else {
                x = this.getX();
                y = this.getY();
                var coeff = 10.0/8.0 * (this.getNode().isPersonGroup() ? PedigreeEditorParameters.attributes.groupNodesScale : 1.0);
                var x1 = x - coeff * PedigreeEditorParameters.attributes.radius,
                    y1 = y + coeff * PedigreeEditorParameters.attributes.radius,
                    x2 = x + coeff * PedigreeEditorParameters.attributes.radius,
                    y2 = y - coeff * PedigreeEditorParameters.attributes.radius;
                this._deadShape = editor.getPaper().path(["M", x1,y1,"L",x2, y2]).attr("stroke-width", strokeWidth);
            }
            if(!editor.isUnsupportedBrowser()) {
                this._deadShape.toFront();
                this._deadShape.node.setAttribute("class", "no-mouse-interaction");
            }
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
            else {
                var birthDate = person.getBirthDate();
                var deathDate = person.getDeathDate();

                var dateFormat = editor.getPreferencesManager().getConfigurationOption("dateDisplayFormat");
                if (dateFormat == "DMY" || dateFormat == "MY" || dateFormat == "Y") {
                    if(person.getLifeStatus() == 'alive') {
                        if (birthDate && birthDate.isComplete()) {
                            text = "b. " + person.getBirthDate().getBestPrecisionStringDDMMYYY(dateFormat);
                            if (person.getBirthDate().getYear() !== null) {
                                var ageString = AgeCalc.getAgeString(person.getBirthDate(), null, dateFormat);
                                if (ageString != "") {
                                    text += " (" + ageString + ")";
                                }
                            }
                        }
                    }
                    else {
                        if(deathDate && birthDate && deathDate.isComplete() && birthDate.isComplete()) {
                            text = person.getBirthDate().getBestPrecisionStringDDMMYYY(dateFormat) + " – " + person.getDeathDate().getBestPrecisionStringDDMMYYY(dateFormat);
                            if (person.getBirthDate().getYear() !== null && person.getDeathDate().getYear() !== null) {
                                var ageString = AgeCalc.getAgeString(person.getBirthDate(), person.getDeathDate(), dateFormat);
                                text += "\nd. " + ageString;
                            }
                        }
                        else if (deathDate && deathDate.isComplete()) {
                            text = "d. " + person.getDeathDate().getBestPrecisionStringDDMMYYY(dateFormat);
                        }
                        else if(birthDate && birthDate.isComplete()) {
                            text = person.getBirthDate().getBestPrecisionStringDDMMYYY(dateFormat) + " – ?";
                        }
                    }
                } else {
                    if(person.getLifeStatus() == 'alive') {
                        if (birthDate && birthDate.isComplete()) {
                            if (birthDate.onlyDecadeAvailable()) {
                                text = "b. " + birthDate.getDecade();
                            } else {
                                if (birthDate.getMonth() == null) {
                                    text = "b. " + birthDate.getYear();                          // b. 1972
                                } else {
                                    if (birthDate.getDay() == null || dateFormat == "MMY") {
                                        text = "b. " + birthDate.getMonthName() + " " +
                                        birthDate.getYear();                                     // b. Jan 1972
                                    } else {
                                        text = "b. " + birthDate.getMonthName() + " " +
                                        birthDate.getDay() + ", " +
                                        birthDate.getYear();                                     // b. Jan 13, 1972
                                        var ageString = AgeCalc.getAgeString(birthDate, null, dateFormat);
                                        if (ageString.indexOf("day") != -1 || ageString.indexOf("wk") != -1) {
                                            text += " (" + ageString + ")";                            // b. Jan 13, 1972 (5 days)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else {
                        if(deathDate && deathDate.isComplete() && birthDate && birthDate.isComplete()) {
                            var ageString = AgeCalc.getAgeString(birthDate, deathDate, dateFormat);
                            if (deathDate.getYear() != null && deathDate.getYear() != birthDate.getYear() && deathDate.getMonth() != null &&
                                (ageString.indexOf("day") != -1 || ageString.indexOf("wk") != -1 || ageString.indexOf("mo") != -1) ) {
                                text = "d. " + deathDate.getYear(true) + " (" + ageString + ")";
                            } else {
                                text = birthDate.getBestPrecisionStringYear() + " – " + deathDate.getBestPrecisionStringYear();
                                if (ageString !== "") {
                                    text += "\nd. " + ageString;
                                }
                            }
                        }
                        else if (deathDate && deathDate.isComplete()) {
                            text = "d. " + deathDate.getBestPrecisionStringYear();
                        }
                        else if (birthDate && birthDate.isComplete()) {
                            text = birthDate.getBestPrecisionStringYear() + " – ?";
                        }
                    }
                }
            }
            this.getAgeLabel() && this.getAgeLabel().remove();
            this._ageLabel = text ? editor.getPaper().text(this.getX(), this.getY(), text).attr(PedigreeEditorParameters.attributes.label) : null;
            if (this._ageLabel) {
                this._ageLabel.node.setAttribute("class", "field-no-user-select");
                if (text && text.indexOf("\n") > 0) {
                    this._ageLabel.alignTop = true;
                }
            }
            this.updateDeathDetailsLabel();  // ...which calls this.drawLabels()
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
                this._unbornShape = editor.getPaper().text(this.getX(), this.getY(), "P").attr(PedigreeEditorParameters.attributes.unbornShape);
                if(!editor.isUnsupportedBrowser())
                    this._unbornShape.insertBefore(this.getHoverBox().getFrontElements());
            } else {
                this._unbornShape = null;
            }
        },

        /**
         * Draws the evaluation status symbol for this Person
         *
         * @method updateEvaluationGraphic
         */
        updateEvaluationGraphic: function() {
            this._evalGraphic && this._evalGraphic.remove();
            var gender = this.getNode().getGender();
            if (this.getNode().getEvaluated()) {
                if (this.getNode().getLifeStatus() == 'aborted' || this.getNode().getLifeStatus() == 'miscarriage') {
                    var x = this.getX() + this._shapeRadius * 1.6;
                    var y = this.getY() + this._shapeRadius * 0.6;
                }
                else {
                    var mult = 1.1;
                    if (gender != 'F') {mult = 1.3;}
                    if (this.getNode().isProband) {mult *= 1.1;}
                    var x = this.getX() + this._shapeRadius*mult;
                    var y = this.getY() + this._shapeRadius*mult - 10; // adjusting position not to collide with A&W status
                    if (this.getNode().getAdopted() != "") { // to avoid intersection with adopted status brackets
                        if (gender == 'F') {y += 4; x -= 6;}
                        if (gender == 'M') {x += 3;}
                        if (gender == 'U' || gender == 'O') {y += 7; x -= 2;}
                    }
                }
                this._evalGraphic = editor.getPaper().text(x, y, "*").attr(PedigreeEditorParameters.attributes.evaluationShape).toBack();
            } else {
                this._evalGraphic = null;
            }
        },

        /**
         * Returns this Person's evaluation label
         *
         * @method getEvaluationGraphics
         * @return {Raphael.el}
         */
        getEvaluationGraphics: function() {
            return this._evalGraphic;
        },

        /**
         * Draws the "alive and well" status symbol for this Person
         *
         * @method updateAliveAndWellGraphic
         */
        updateAliveAndWellGraphic: function(isAliveAndWell) {
            this._awGraphic && this._awGraphic.remove();
            var gender = this.getNode().getGender();
            if (this.getNode().getAliveAndWell()) {
                var mult = 1.2;
                if (gender != 'F') {mult = 1.4;}
                if (this.getNode().isProband) {mult *= 1.1;}
                if (this.getNode().getType() == "PersonGroup") {mult *= 1.05;}
                var x = this.getX() + this._shapeRadius*mult - 10; // adjusting position not to collide with evaluation status
                var y = this.getY() + this._shapeRadius*mult;
                if (this.getNode().getAdopted() != "") { // to avoid intersection with adopted status brackets
                    if (gender == 'F') {y += 13; x -= 3;}
                    if (gender == 'M') {y += 4; x -= 6;}
                    if (gender == 'U' || gender == 'O') {y += 17;}
                }
                this._awGraphic = editor.getPaper().text(x, y, "A&W").attr(PedigreeEditorParameters.attributes.aliveAndWellShape).toBack();
            } else {
                this._awGraphic = null;
            }
        },

        /**
         * Draws various distorder carrier graphics such as a dot (for carriers) or
         * a vertical line (for pre-symptomatic)
         *
         * @method updateCarrierGraphic
         */
        updateCarrierGraphic: function() {
            this._carrierGraphic && this._carrierGraphic.remove();
            var status = this.getNode().getCarrierStatus();

            if (status != '' && status != 'affected') {
                if (status == 'carrier') {
                    if (this.getNode().getLifeStatus() == 'aborted' || this.getNode().getLifeStatus() == 'miscarriage') {
                        var x = this.getX();
                        var y = this.getY() - this._radius/2;
                    } else {
                        var x = this.getX();
                        var y = this.getY();
                    }
                    this._carrierGraphic = editor.getPaper().circle(x, y, PedigreeEditorParameters.attributes.carrierDotRadius).attr(PedigreeEditorParameters.attributes.carrierShape);
                } else if (status == 'uncertain') {
                    if (this.getNode().getLifeStatus() == 'aborted' || this.getNode().getLifeStatus() == 'miscarriage') {
                        var x = this.getX();
                        var y = this.getY() - this._radius/2;
                        var fontAttr = PedigreeEditorParameters.attributes.uncertainSmallShape;
                    } else {
                        var x = this.getX();
                        var y = this.getY();
                        var fontAttr = PedigreeEditorParameters.attributes.uncertainShape;
                    }
                    this._carrierGraphic = editor.getPaper().text(x, y, "?").attr(fontAttr);
                } else if (status == 'presymptomatic') {
                    if (this.getNode().getLifeStatus() == 'aborted' || this.getNode().getLifeStatus() == 'miscarriage') {
                        this._carrierGraphic = null;
                        return;
                    }
                    editor.getPaper().setStart();
                    var startX = (this.getX()-PedigreeEditorParameters.attributes.presymptomaticShapeWidth/2);
                    var startY = this.getY()-this._radius;
                    editor.getPaper().rect(startX, startY, PedigreeEditorParameters.attributes.presymptomaticShapeWidth, this._radius*2).attr(PedigreeEditorParameters.attributes.presymptomaticShape);
                    if (this.getNode().getGender() == 'U') {
                        editor.getPaper().path("M "+startX + " " + startY +
                                               "L " + (this.getX()) + " " + (this.getY()-this._radius*1.1) +
                                               "L " + (startX + PedigreeEditorParameters.attributes.presymptomaticShapeWidth) + " " + (startY) + "Z").attr(PedigreeEditorParameters.attributes.presymptomaticShape);
                        var endY = this.getY()+this._radius;
                        editor.getPaper().path("M "+startX + " " + endY +
                                               "L " + (this.getX()) + " " + (this.getY()+this._radius*1.1) +
                                               "L " + (startX + PedigreeEditorParameters.attributes.presymptomaticShapeWidth) + " " + endY + "Z").attr(PedigreeEditorParameters.attributes.presymptomaticShape);
                    }
                    this._carrierGraphic = editor.getPaper().setFinish();
                }
                if (editor.isReadOnlyMode())
                    this._carrierGraphic.toFront();
                else
                    this._carrierGraphic.insertBefore(this.getHoverBox().getFrontElements());
            } else {
                this._carrierGraphic = null;
            }
        },

        /**
         * Returns this Person's disorder carrier graphics
         *
         * @method getCarrierGraphics
         * @return {Raphael.el}
         */
        getCarrierGraphics: function() {
            return this._carrierGraphic;
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
            if (this.getNode().getLifeStatus() == 'stillborn') {
                this._stillBirthLabel = editor.getPaper().text(this.getX(), this.getY(), "SB").attr(PedigreeEditorParameters.attributes.label);
                this._stillBirthLabel.addLargeGapAfter = true;
                this._stillBirthLabel.shiftUp = 15;
            } else {
                this._stillBirthLabel = null;
            }
            this.drawLabels();
        },

        /**
         * Returns this Person's PhenoTips profile link label
         *
         * @method getLinkLabel
         * @return {Raphael.el}
         */
        getLinkLabel: function() {
            return this._linkLabel;
        },

        /**
         * Updates the PhenoTips profile link label
         *
         * @method updateLinkLabel
         */
        updateLinkLabel: function() {
            this._linkArea && this._linkArea.remove();
            this._linkArea = null;
            this._linkLabel && this._linkLabel.remove();
            var useExternalID = editor.getPreferencesManager().getConfigurationOption("replaceIdWithExternalID");
            if (this.getNode().getPhenotipsPatientId() == "" && (!useExternalID || this.getNode().getExternalID() == "")) {
                this._linkLabel = null;
            } else {
                var linkText = (useExternalID && this.getNode().getExternalID() != "") ? this.getNode().getExternalID() : this.getNode().getPhenotipsPatientId();
                if (this.getNode().getPhenotipsPatientId() == "") {
                    linkText = "[ " + linkText + " ]";
                }
                this._linkLabel = editor.getPaper().text(this.getX(), this.getY(), linkText).attr(PedigreeEditorParameters.attributes.label);
                this._linkLabel.addGapAfter = true;
                if (this.getNode().getPhenotipsPatientId() != "") {
                    this._linkLabel.node.setAttribute("class","pedigree-nodePatientTextLink");
                    var patientURL = this.getNode().getPhenotipsPatientURL();
                    var patientID = this.getNode().getPhenotipsPatientId();
                    this._linkLabel.attr({ "href": patientURL });
                    this._linkLabel.node.parentNode.setAttribute("target", patientID);
                    this._linkLabel.node.parentNode.setAttribute("class", "pedigree-patient-record-link");
                    this._linkLabel.attr("fill", "#00498A");
                }
            }
            this.drawLabels();
        },

        /**
         * Returns this Person's comments label
         *
         * @method getCommentsLabel
         * @return {Raphael.el}
         */
        getCommentsLabel: function() {
            return this._commentsLabel;
        },

        /**
         * Updates the comments label for this Person
         *
         * @method updateCommentsLabel
         */
        updateCommentsLabel: function() {
            this.getCommentsLabel() && this.getCommentsLabel().remove();
            if (this.getNode().getComments() != "") {
                // note: raphael positions text which starts with a new line in a strange way
                //       also, blank lines are ignored unless replaced with a space
                var text = this.getNode().getComments().replace(/^\s+|\s+$/g,'').replace(/\n\n/gi,'\n \n');
                this._commentsLabel = editor.getPaper().text(this.getX(), this.getY(), text).attr(PedigreeEditorParameters.attributes.commentLabel);
                this._commentsLabel.node.setAttribute("class", "field-no-user-select");
                this._commentsLabel.alignTop = true;
                this._commentsLabel.addGap   = true;
            } else {
                this._commentsLabel = null;
            }
            this.drawLabels();
        },


        /**
         * Returns this Person's cancer age of onset labels
         *
         * @method getCancerAgeOfOnsetLabels
         * @return {Raphael.el}
         */
        getCancerAgeOfOnsetLabels: function() {
            return this._cancerAgeOfOnsetLabels;
        },


        /**
         * Updates the cancer age of onset labels for this Person
         *
         * @method updateCancerAgeOfOnsetLabels
         */
        updateCancerAgeOfOnsetLabels: function() {
            var cancerLabels = this.getCancerAgeOfOnsetLabels();
            if (!Helpers.isObjectEmpty(cancerLabels)) {
                for (var cancerId in cancerLabels) {
                    cancerLabels[cancerId].remove();
                }
            }
            var cancerData = this.getNode().getCancers();
            if (!Helpers.isObjectEmpty(cancerData)
                && editor.getPreferencesManager().getConfigurationOption("displayCancerLabels")) {
                for (var cancerId in cancerData) {
                    if (cancerData.hasOwnProperty(cancerId) && cancerData[cancerId].affected) {
                        var cancerName = cancerData[cancerId].label || cancerId;
                        var text = cancerName.toString() + " ca.";
                        if (cancerData[cancerId].hasOwnProperty("qualifiers") && cancerData[cancerId].qualifiers.length > 0) {
                            var dx = "";
                            cancerData[cancerId].qualifiers.forEach(function(qualifier) {
                                if (qualifier.hasOwnProperty("ageAtDiagnosis") && qualifier.ageAtDiagnosis && qualifier.ageAtDiagnosis.length > 0) {
                                    var age = qualifier.ageAtDiagnosis;
                                    if (isNaN(parseInt(age))) {
                                        if (age === "before_1") {
                                            dx += ", <1";
                                        } else if (age === "before_10") {
                                            dx += ", <10";
                                        } else {
                                            dx += (age.indexOf('before_') > -1) ? ", " + (parseInt(age.substring(7))-10) + "'s": ", >100";
                                        }
                                    } else {
                                        dx += ", " + age;
                                    }
                                }
                            });
                            text += dx.length > 0 ? " dx" + dx.substring(1) : " dx ?";
                        }
                        this.getCancerAgeOfOnsetLabels()[cancerId] && this.getCancerAgeOfOnsetLabels()[cancerId].remove();
                        this._cancerAgeOfOnsetLabels[cancerId] = editor.getPaper().text(this.getX(), this.getY(), text).attr(PedigreeEditorParameters.attributes.cancerAgeOfOnsetLabels);
                        this._cancerAgeOfOnsetLabels[cancerId].node.setAttribute("class", "field-no-user-select");
                        this._cancerAgeOfOnsetLabels[cancerId].alignTop = true;
                        this._cancerAgeOfOnsetLabels[cancerId].addGap   = true;
                    }
                }

            } else {
                this._cancerAgeOfOnsetLabels = {};
            }
            this.drawLabels();
        },

        /**
         * Displays the correct graphics to represent the current life status for this Person.
         *
         * @method updateLifeStatusShapes
         */
        updateLifeStatusShapes: function(oldStatus) {
            var status = this.getNode().getLifeStatus();

            this.getDeadShape()   && this.getDeadShape().remove();
            this.getUnbornShape() && this.getUnbornShape().remove();
            this.getSBLabel()     && this.getSBLabel().remove();

            // save some redraws if possible
            var oldShapeType = (oldStatus == 'aborted' || oldStatus == 'miscarriage');
            var newShapeType = (status    == 'aborted' || status    == 'miscarriage');
            if (oldShapeType != newShapeType)
                this.setGenderGraphics();

            if(status == 'deceased' || status == 'aborted') {  // but not "miscarriage"
                this.drawDeadShape();
            }
            else if (status == 'stillborn') {
                this.drawDeadShape();
                this.updateSBLabel();
            }
            else if (status == 'unborn') {
                this.drawUnbornShape();
            }
            this.updateAgeLabel();
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
            this._linkArea && this._linkArea.stop().animate({"y": this._linkArea.oy + shift}, 200,">");
        },

        /**
         * Animates the labels of this node to their original position under the node
         *
         * @method unshiftLabels
         */
        unshiftLabels: function() {
            var labels = this.getLabels();
            for(var i = 0; i<labels.length; i++) {
                labels[i].stop().animate({"y": labels[i].oy}, 200,">");
            }
            this._linkArea && this._linkArea.stop().animate({"y": this._linkArea.oy}, 200,">");
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
            if (!this._anonymized.hasOwnProperty("removePII") || !this._anonymized.removePII) {
                if (this.getLinkLabel()) {
                    this.getLinkLabel().show();
                    labels.push(this.getLinkLabel());
                }
                if (this.getNameLabel()) {
                    this.getNameLabel().show();
                    labels.push(this.getNameLabel());
                }
                if (this.getAgeLabel()) {
                    this.getAgeLabel().show();
                    labels.push(this.getAgeLabel());
                }
                if (this.getDeathDetailsLabel()) {
                    this.getDeathDetailsLabel().show();
                    labels.push(this.getDeathDetailsLabel());
                }
                if (this.getExternalIDLabel()) {
                    this.getExternalIDLabel().show();
                    labels.push(this.getExternalIDLabel());
                }
            } else {
                this.getLinkLabel() && this.getLinkLabel().hide();
                this.getNameLabel() && this.getNameLabel().hide();
                this.getAgeLabel() && this.getAgeLabel().hide();
                this.getExternalIDLabel() && this.getExternalIDLabel().hide();
                this.getDeathDetailsLabel() && this.getDeathDetailsLabel().hide();
            }
            if (!this._anonymized.hasOwnProperty("removeComments") || !this._anonymized.removeComments) {
                if (this.getCommentsLabel()) {
                    this.getCommentsLabel().show();
                    labels.push(this.getCommentsLabel());
                }
            } else {
                this.getCommentsLabel() && this.getCommentsLabel().hide();
            }
            var cancerLabels = this.getCancerAgeOfOnsetLabels();
            if (!Helpers.isObjectEmpty(cancerLabels)) {
                for (var cancerName in cancerLabels) {
                    labels.push(cancerLabels[cancerName]);
                }
            }
            return labels;
        },

        /**
         * Removes all PII labels
         */
        setAnonymizedStatus: function($super, status) {
            $super(status);
            this.drawLabels();
        },

        /**
         * Displays all the appropriate labels for this Person in the correct layering order
         *
         * @method drawLabels
         */
        drawLabels: function() {
            // this method may be slow yet is called after each label is generated;
            // skip it while initial properties are set and execute once after all labels have been generated.
            if (this.getNode()._speedup_NOREDRAW) {
                this.getNode()._speedup_NEEDTOCALL["drawLabels"] = true;
                return;
            }
            var labels = this.getLabels();
            var selectionOffset = this._labelSelectionOffset();
            var childlessOffset = this._getChildlessOffset();

            var lowerBound = PedigreeEditorParameters.attributes.radius * (this.getNode().isPersonGroup() ? PedigreeEditorParameters.attributes.groupNodesScale : 1.0);

            var firstLabelPosition = this.getY() + lowerBound * 1.8 + childlessOffset + 15 + PedigreeEditorParameters.attributes.firstNodeLabelSpaceOnTop;
            var startY = firstLabelPosition + selectionOffset;
            for (var i = 0; i < labels.length; i++) {
                var shift = labels[i].addGap ? 4 : 0;
                var offset = (labels[i].alignTop) ? (GraphicHelpers.getElementHalfHeight(labels[i]) - shift) : shift;
                if (i == 0 && labels[i].shiftUp) {
                    offset -= labels[i].shiftUp;
                }
                labels[i].transform(""); // clear all transofrms, using new real x
                labels[i].attr("x", this.getX());
                labels[i].attr("y", startY + offset);
                labels[i].oy = (labels[i].attr("y") - selectionOffset);
                labels[i].toBack();
                if (i != labels.length - 1) {   // don't do getBBox() computation if don't need to, it is slow in IE9
                    startY = labels[i].getBBox().y2 + 11;
                    if (labels[i].addGapAfter) {
                        startY += 4;
                    }
                    if (labels[i].addLargeGapAfter) {
                        startY += 8;
                    }
                }
            }

            // a hack for node links which should be clickable without hoverbox obscuring them and making them move
            if (this._linkLabel && this.getNode().getPhenotipsPatientId() != "") {
                this._linkArea && this._linkArea.remove();
                var boundingBox = this._linkLabel.getBBox();
                var minVerticalClickArea = PedigreeEditorParameters.attributes.patientLinkClickAreaAbove;
                // the hack should cover the bottom part of the hoverbox to make sure mouse can be moved to the link from
                // the left, right and the bottom without making the hoverbox trigger and move the link
                var normalPositionTop = boundingBox.y - selectionOffset;
                var hackHeight = Math.max(boundingBox.height+minVerticalClickArea*2,
                                          this.getHoverBox().getY() + this.getHoverBox().getHeight() - normalPositionTop + minVerticalClickArea + 1);
                var linkAreaTop = boundingBox.y-minVerticalClickArea;
                this._linkArea = editor.getPaper().rect(this.getHoverBox().getX() - 1, linkAreaTop, this.getHoverBox().getWidth() + 2, hackHeight).attr({
                    fill: "#F00",
                    opacity: 0
                  });
                this._linkArea.oy = (this._linkArea.attr("y") - selectionOffset);
                this._linkArea.toFront();
                this.getLinkLabel().toFront();
            }

            this.onSetID(); // set IDs of SVG <text> and <a> elements

            //if(!editor.isUnsupportedBrowser())
            //    labels.flatten().insertBefore(this.getHoverBox().getFrontElements().flatten());
        },

        _getChildlessOffset: function() {
            var childlessOffset = this.getChildlessStatusLabel() ? PedigreeEditorParameters.attributes.label['font-size'] : 0;
            childlessOffset += ((this.getNode().getChildlessStatus() !== null) ? (PedigreeEditorParameters.attributes.infertileMarkerHeight + 2) : 0);
            return childlessOffset;
        },

        _labelSelectionOffset: function() {
            if (!this.isSelected()) {
                return 0;
            }
            var labelsOffset = this.getHoverBox().getBottomExtensionHeight();
            var selectionOffset = PedigreeEditorParameters.attributes.radius/1.4 + labelsOffset;

            if (this.getNode().isPersonGroup()) {
                selectionOffset += PedigreeEditorParameters.attributes.radius * (1-PedigreeEditorParameters.attributes.groupNodesScale) + 5;
            }

            if (this.getChildlessStatusLabel()) {
                selectionOffset = selectionOffset - this._getChildlessOffset();
            }
            return selectionOffset;
        },

        /**
         * Updates node's <text> and <a> id's when node ID changes
         * @method onSetID
         */
        onSetID: function($super, id) {
            $super(id);
            var labels = this.getLabels();
            for (var i = 0; i < labels.length; i++) {
                labels[i].node.setAttribute("pedigreeNodeID", this.getNode().getID());
            }
            this._linkLabel && (this._linkLabel.node.setAttribute("pedigreeLinkedPatient", this.getNode().getPhenotipsPatientId()));
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
            return $super().push(this.getHoverBox().getBackElements(), this.getLabels(), this._awGraphic, this._linkArea, this.getCarrierGraphics(), this.getEvaluationGraphics(), this.getHoverBox().getFrontElements());
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

            // to avoid visual distractions, re-draw hoverbox elements if they were on screen before re-positioning
            if (this.getHoverBox().isMenuToggled()) {
                this.getHoverBox().generateButtons();
                this.getHoverBox().generateHandles();
            }
        }
    });

    //ATTACHES CHILDLESS BEHAVIOR METHODS
    PersonVisuals.addMethods(ChildlessBehaviorVisuals);

    return PersonVisuals;
});
