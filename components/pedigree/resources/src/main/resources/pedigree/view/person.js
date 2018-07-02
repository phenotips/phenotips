/**
 * Person is a class representing any AbstractPerson that has sufficient information to be
 * displayed on the final pedigree graph (printed or exported). Person objects
 * contain information about disorders, age and other relevant properties, as well
 * as graphical data to visualize this information.
 *
 * @class Person
 * @constructor
 * @extends AbstractPerson
 * @param {Number} x X coordinate on the Raphael canvas at which the node drawing will be centered
 * @param {Number} y Y coordinate on the Raphael canvas at which the node drawing will be centered
 * @param {String} gender 'M', 'F' or 'U' depending on the gender
 * @param {Number} id Unique ID number
 * @param {Boolean} isProband True if this person is the proband
 */
define([
        "pedigree/pedigreeDate",
        "pedigree/model/helpers",
        "pedigree/model/phenotipsJSON",
        "pedigree/view/abstractPerson",
        "pedigree/view/childlessBehavior",
        "pedigree/view/childlessBehaviorVisuals",
        "pedigree/view/personVisuals",
        "pedigree/hpoTerm"
    ], function(
        PedigreeDate,
        Helpers,
        PhenotipsJSON,
        AbstractPerson,
        ChildlessBehavior,
        ChildlessBehaviorVisuals,
        PersonVisuals,
        HPOTerm
    ){
    var Person = Class.create(AbstractPerson, {

        initialize: function($super, x, y, id, properties) {
            //var timer = new Helpers.Timer();
            !this._type && (this._type = "Person");
            this._setDefault();
            var gender = properties.hasOwnProperty("gender") ? properties['gender'] : "U";
            $super(x, y, gender, id);

            var extensionParameters = { "node": this };
            editor.getExtensionManager().callExtensions("personNodeCreated", extensionParameters);

            // need to assign after super() and explicitly pass gender to super()
            // because changing properties requires a redraw, which relies on gender
            // shapes being there already
            this.assignProperties(properties);

            //timer.printSinceLast("=== new person runtime: ");
        },

        _setDefault: function() {
            this._phenotipsId = "";
            this._firstName = "";
            this._lastName = "";
            this._lastNameAtBirth = "";
            this._birthDate = null;
            this._deathDate = null;
            this._conceptionDate = "";
            this._gestationAge = "";
            this._adoptedStatus = "";
            this._externalID = "";
            this._lifeStatus = 'alive';
            this._childlessStatus = null;
            this._childlessReason = "";
            this._carrierStatus = "";
            this._disorders = [];
            this._cancers = {};
            this._hpo = [];
            this.features = [];
            this.nonStandardFeatures = [];
            this._ethnicities = [];
            this._genes = [];
            this._twinGroup = null;
            this._monozygotic = false;
            this._evaluated = false;
            this._pedNumber = "";
            this._lostContact = false;
            this._aliveandwell = false;
            this._deceasedAge = "";
            this._deceasedCause = "";
        },

        /**
         * Initializes the object responsible for creating graphics for this Person
         *
         * @method _generateGraphics
         * @param {Number} x X coordinate on the Raphael canvas at which the node drawing will be centered
         * @param {Number} y Y coordinate on the Raphael canvas at which the node drawing will be centered
         * @return {PersonVisuals}
         * @private
         */
        _generateGraphics: function(x, y) {
            return new PersonVisuals(this, x, y);
        },

        /**
         * Returns True if this node is the proband (i.e. the main patient)
         *
         * @method isProband
         * @return {Boolean}
         */
        isProband: function() {
            return (this.getID() == editor.getGraph().getProbandId());
        },

        /**
         * Redraws gender shape with or without proband indicators
         */
        redrawProbandStatus: function() {
            this.getGraphics().setGenderGraphics();
            this.getGraphics().getHoverBox().regenerateButtons();
        },

        /**
         * Returns the id of the PhenoTips patient represented by this node.
         * Returns an empty string for nodes not assosiated with any PhenoTips patients.
         *
         * @method getPhenotipsPatientId
         * @return {String}
         */
        getPhenotipsPatientId: function()
        {
            return this._phenotipsId;
        },

        /**
         * Returns the URL of the PhenoTips patient represented by this node.
         *
         * @method getPhenotipsPatientURL
         * @return {String}
         */
        getPhenotipsPatientURL: function()
        {
            return editor.getExternalEndpoint().getPhenotipsPatientURL(this.getPhenotipsPatientId());
        },

        /**
         * Replaces (or sets) the id of the PhenoTips patient represented by this node
         * with the given id, and updates the label.
         *
         * No error checking for the validity of this id is done.
         *
         * @method setPhenotipsPatientId
         * @param firstName
         */
        setPhenotipsPatientId: function(phenotipsId)
        {
            if (phenotipsId == this._phenotipsId) {
                return;
            }

            if (this._phenotipsId != "") {
                // Store curent state of the patient as "the last know aproved state for this patient record".
                //
                // assumption: this node (as of this moment when the patient record is unlinked from this pedigree node)
                //             has the latest known data for the given patient record, which is assumed to override data loaded
                //             for this patient record from the backend. E.g. if a patient record was linked to a node,
                //             edited, and then unliked, those changes should be saved.
                //
                // TODO: do not need this if every property is instantly placed into PT JSON, which is a better
                //       and more consistent way, but requires much more refactoring.
                // TODO: alternatively, this code can be triggered when data model is being updated, but since
                //       all other triggers are activated by changes in the "view" component the code is placed here
                //
                // note: ignoring relationship properties, since once unlinked there are no relationships
                //       may decideto preserve that as well, but that is hard since if this mthod is called when a
                //       part of a pedigree gets deleted => relationship properties may no longer be available by
                //       the point this patient is deleted and this code is called
                var relationshipProperties = {};
                var currentStateJSON = PhenotipsJSON.internalToPhenotipsJSON(this.getProperties(), relationshipProperties);
                editor.getPatientRecordData().update(this._phenotipsId, currentStateJSON);

                // fire "patient [this._phenotipsId] is no longer linked to a node" event
                var event = { "phenotipsID": this._phenotipsId,
                              "pedigreeProperties": this.getProperties() };
                document.fire("pedigree:patient:unlinked", event);
            } else {
                document.fire("pedigree:patient:linked", {"phenotipsID": phenotipsId});
            }

            this._phenotipsId = phenotipsId;

            this.getGraphics().setGenderGraphics();
            this.getGraphics().getHoverBox().regenerateButtons();
            this.getGraphics().updateLinkLabel();
        },

        /**
         * Returns the first name of this Person
         *
         * @method getFirstName
         * @return {String}
         */
        getFirstName: function() {
            return this._firstName;
        },

        /**
         * Replaces the first name of this Person with firstName, and displays the label
         *
         * @method setFirstName
         * @param firstName
         */
        setFirstName: function(firstName) {
            firstName && (firstName = firstName.charAt(0).toUpperCase() + firstName.slice(1));
            this._firstName = firstName;
            this.getGraphics().updateNameLabel();
        },

        /**
         * Returns the last name of this Person
         *
         * @method getLastName
         * @return {String}
         */
        getLastName: function() {
            return this._lastName;
        },

        /**
         * Replaces the last name of this Person with lastName, and displays the label
         *
         * @method setLastName
         * @param lastName
         */
        setLastName: function(lastName) {
            lastName && (lastName = lastName.charAt(0).toUpperCase() + lastName.slice(1));
            this._lastName = lastName;
            this.getGraphics().updateNameLabel();
            return lastName;
        },

        /**
         * Returns the externalID of this Person
         *
         * @method getExternalID
         * @return {String}
         */
        getExternalID: function() {
            return this._externalID;
        },

        /**
         * Sets the user-visible node ID for this person
         * ("I-1","I-2","I-3", "II-1", "II-2", etc.)
         *
         * @method setPedNumber
         */
        setPedNumber: function(pedNumberString) {
            this._pedNumber = pedNumberString;
            this.getGraphics().updateNumberLabel();
        },

        /**
         * Returns the user-visible node ID for this person, e.g. "I", "II", "III", "IV", etc.
         *
         * @method getPedNumber
         * @return {String}
         */
        getPedNumber: function() {
            return this._pedNumber;
        },

        /**
         * Replaces the external ID of this Person with the given ID, and displays the label
         *
         * @method setExternalID
         * @param externalID
         */
        setExternalID: function(externalID) {
            this._externalID = externalID;
            if (this.getPhenotipsPatientId()) {
                editor.getExternalIdManager().set(this.getPhenotipsPatientId(), externalID);
            }
            this.getGraphics().updateExternalIDLabel();
        },

        /**
         * Returns the last name at birth of this Person
         *
         * @method getLastNameAtBirth
         * @return {String}
         */
        getLastNameAtBirth: function() {
            return this._lastNameAtBirth;
        },

        /**
         * Replaces the last name at birth of this Person with the given name, and updates the label
         *
         * @method setLastNameAtBirth
         * @param lastNameAtBirth
         */
        setLastNameAtBirth: function(lastNameAtBirth) {
            lastNameAtBirth && (lastNameAtBirth = lastNameAtBirth.charAt(0).toUpperCase() + lastNameAtBirth.slice(1));
            this._lastNameAtBirth = lastNameAtBirth;
            this.getGraphics().updateNameLabel();
            return lastNameAtBirth;
        },

        /**
         * Replaces free-form comments associated with the node and redraws the label
         *
         * @method setComments
         * @param comment
         */
        setComments: function($super, comment) {
            if (comment != this.getComments()) {
                $super(comment);
                this.getGraphics().updateCommentsLabel();
            }
        },

        /**
         * Sets the type of twin
         *
         * @method setMonozygotic
         */
        setMonozygotic: function(monozygotic) {
            if (monozygotic == this._monozygotic) return;
            this._monozygotic = monozygotic;
        },

        /**
         * Returns the documented evaluation status
         *
         * @method getEvaluated
         * @return {Boolean}
         */
        getEvaluated: function() {
            return this._evaluated;
        },

        /**
         * Sets the documented evaluation status
         *
         * @method setEvaluated
         */
        setEvaluated: function(evaluationStatus) {
            if (evaluationStatus == this._evaluated) return;
            this._evaluated = evaluationStatus;
            this.getGraphics().updateEvaluationGraphic();
        },

        /**
         * Returns the "in contact" status of this node.
         * "False" means proband has lost contaxt with this individual
         *
         * @method getLostContact
         * @return {Boolean}
         */
        getLostContact: function() {
            return this._lostContact;
        },

        /**
         * Sets the "in contact" status of this node
         *
         * @method setLostContact
         */
        setLostContact: function(lostContact) {
            if (lostContact == this._lostContact) return;
            this._lostContact = lostContact;
        },

        /**
         * Returns the type of twin: monozygotic or not
         * (always false for non-twins)
         *
         * @method getMonozygotic
         * @return {Boolean}
         */
        getMonozygotic: function() {
            return this._monozygotic;
        },

        /**
         * Assigns this node to the given twin group
         * (a twin group is all the twins from a given pregnancy)
         *
         * @method setTwinGroup
         */
        setTwinGroup: function(groupId) {
            this._twinGroup = groupId;
        },

        /**
         * Returns the status of this Person
         *
         * @method getLifeStatus
         * @return {String} "alive", "deceased", "stillborn", "unborn", "aborted" or "miscarriage"
         */
        getLifeStatus: function() {
            return this._lifeStatus;
        },

        /**
         * Returns the if the Person is alive and well status
         *
         * @method getAliveAndWell
         * @return {boolean} true if the person is alive and well, false otherwise
         */
        getAliveAndWell: function() {
            return this._aliveandwell;
        },

        /**
         * Returns the age of person death
         *
         * @method getDeceasedAge
         * @return {String} the age of person's death
         */
        getDeceasedAge: function() {
            return this._deceasedAge;
        },

        /**
         * Returns the cause of person's death
         *
         * @method getDeceasedCause
         * @return {String} the cause of person's death
         */
        getDeceasedCause: function() {
            return this._deceasedCause;
        },

        /**
         * Changes the age of person death
         *
         * @method setDeceasedAge
         * @param {String} age the age of person's death
         */
        setDeceasedAge: function(age) {
            this._deceasedAge = age;
            this.getGraphics().updateDeathDetailsLabel();
        },

        /**
         * Changes the cause of person's death
         *
         * @method setDeceasedCause
         * @param {String} cause the cause of person's death
         */
        setDeceasedCause: function(cause) {
            this._deceasedCause = cause;
            this.getGraphics().updateDeathDetailsLabel();
        },

        /**
         * Returns True if this node's status is not 'alive' or 'deceased'.
         *
         * @method isFetus
         * @return {Boolean}
         */
        isFetus: function() {
            return (this.getLifeStatus() != 'alive' && this.getLifeStatus() != 'deceased');
        },

        /**
         * Returns True is status is 'unborn', 'stillborn', 'aborted', 'miscarriage', 'alive' or 'deceased'
         *
         * @method _isValidLifeStatus
         * @param {String} status
         * @returns {boolean}
         * @private
         */
        _isValidLifeStatus: function(status) {
            return (status == 'unborn' || status == 'stillborn'
                || status == 'aborted' || status == 'miscarriage'
                || status == 'alive' || status == 'deceased')
        },

        /**
         * Changes the life status of this Person to newStatus
         *
         * @method setLifeStatus
         * @param {String} newStatus "alive", "deceased", "stillborn", "unborn", "aborted" or "miscarriage"
         */
        setLifeStatus: function(newStatus) {
            if (newStatus == this._lifeStatus) {
                return;
            }
            if(this._isValidLifeStatus(newStatus)) {
                var oldStatus = this._lifeStatus;

                this._lifeStatus = newStatus;

                if (newStatus != 'deceased') {
                    this.setDeathDate("");
                    this.setDeceasedAge("");
                    this.setDeceasedCause("");
                }
                (newStatus == 'alive') && this.setGestationAge();
                (newStatus != 'alive') && this.setAliveAndWell(false);
                this.getGraphics().updateSBLabel();

                if(this.isFetus()) {
                    this.setBirthDate("");
                    this.setAdopted("");
                    this.setChildlessStatus(null);
                }
                if (this.isProband()) {
                    this.getGraphics().setGenderGraphics();
                }
                this.getGraphics().updateLifeStatusShapes(oldStatus);
                this.getGraphics().getHoverBox().regenerateHandles();
                this.getGraphics().getHoverBox().regenerateButtons();
            }
        },

        /**
         * Changes the alive and well status of the Person
         *
         * @method setAliveAndWell
         * @param {boolean} true if the person is alive and well, false otherwise
         */
        setAliveAndWell: function(newStatus) {
            if (typeof(newStatus) == "boolean" && newStatus != this._aliveandwell) {
                this._aliveandwell = newStatus;
                if (newStatus && this.getLifeStatus() != "alive") {
                        this.setLifeStatus("alive");
                }
                this.getGraphics().getHoverBox().regenerateButtons();
                this.getGraphics().updateAliveAndWellGraphic();
            }
        },

        /**
         * Returns the date of the conception date of this Person
         *
         * @method getConceptionDate
         * @return {Date}
         */
        getConceptionDate: function() {
            return this._conceptionDate;
        },

        /**
         * Replaces the conception date with newDate
         *
         * @method setConceptionDate
         * @param {Date} newDate Date of conception
         */
        setConceptionDate: function(newDate) {
            this._conceptionDate = newDate ? (new Date(newDate)) : '';
            this.getGraphics().updateAgeLabel();
        },

        /**
         * Returns the number of weeks since conception
         *
         * @method getGestationAge
         * @return {Number}
         */
        getGestationAge: function() {
            if(this.getLifeStatus() == 'unborn' && this.getConceptionDate()) {
                var oneWeek = 1000 * 60 * 60 * 24 * 7,
                    lastDay = new Date();
                return Math.round((lastDay.getTime() - this.getConceptionDate().getTime()) / oneWeek)
            }
            else if(this.isFetus()){
                return this._gestationAge;
            }
            else {
                return null;
            }
        },

        /**
         * Updates the conception age of the Person given the number of weeks passed since conception
         *
         * @method setGestationAge
         * @param {Number} numWeeks Greater than or equal to 0
         */
        setGestationAge: function(numWeeks) {
            try {
                numWeeks = parseInt(numWeeks);
            } catch (err) {
                numWeeks = "";
            }
            if(numWeeks){
                this._gestationAge = numWeeks;
                var daysAgo = numWeeks * 7,
                    d = new Date();
                d.setDate(d.getDate() - daysAgo);
                this.setConceptionDate(d);
            }
            else {
                this._gestationAge = "";
                this.setConceptionDate(null);
            }
            this.getGraphics().updateAgeLabel();
        },

        /**
         * Returns the the birth date of this Person
         *
         * @method getBirthDate
         * @return {Date}
         */
        getBirthDate: function() {
            return this._birthDate;
        },

        /**
         * Replaces the birth date with newDate
         *
         * @method setBirthDate
         * @param newDate Either a string or an object with "year" (mandatory), "month" (optional) and "day" (optional) fields.
         *                Must be earlier date than deathDate and a later than conception date
         */
        setBirthDate: function(newDate) {
            newDate = new PedigreeDate(newDate);  // parse input
            if (!newDate.isSet()) {
                newDate = null;
            }
            if (!newDate || !this.getDeathDate() || this.getDeathDate().canBeAfterDate(newDate)) {
                this._birthDate = newDate;
                this.getGraphics().updateAgeLabel();
            }
        },

        /**
         * Returns the death date of this Person
         *
         * @method getDeathDate
         * @return {Date}
         */
        getDeathDate: function() {
            return this._deathDate;
        },

        /**
         * Replaces the death date with deathDate
         *
         *
         * @method setDeathDate
         * @param {Date} deathDate Must be a later date than birthDate
         */
        setDeathDate: function(deathDate) {
            deathDate = new PedigreeDate(deathDate);  // parse input
            if (!deathDate.isSet()) {
                deathDate = null;
            }
            // only set death date if it happens ot be after the birth date, or there is no birth or death date
            if(!deathDate || !this.getBirthDate() || deathDate.canBeAfterDate(this.getBirthDate())) {
                this._deathDate =  deathDate;
                this._deathDate && (this.getLifeStatus() == 'alive') && this.setLifeStatus('deceased');
            }
            this.getGraphics().updateAgeLabel();
            return this.getDeathDate();
        },

        _isValidCarrierStatus: function(status) {
            return (status == '' || status == 'carrier' || status == 'uncertain'
                || status == 'affected' || status == 'presymptomatic');
        },

        /**
         * Sets the global disorder carrier status for this Person
         *
         * @method setCarrier
         * @param status One of {'', 'carrier', 'affected', 'presymptomatic', 'uncertain'}
         */
        setCarrierStatus: function(status) {
            var numDisorders = this.getDisorders().length;

            if (status === undefined || status === null) {
                if (numDisorders == 0) {
                    status = ""
                } else {
                    status = this.getCarrierStatus();
                    if (status == "") {
                        status = "affected";
                    }
                }
            }

            if (!this._isValidCarrierStatus(status)) return;

            if (numDisorders > 0 && status == '') {
                if (numDisorders == 1 && this.getDisorders()[0] == "affected") {
                    this.removeDisorder("affected");
                    this.getGraphics().updateDisorderShapes();
                } else {
                    status = 'affected';
                }
            } else if (numDisorders == 0 && status == 'affected') {
                this.addDisorder("affected");
                this.getGraphics().updateDisorderShapes();
            }

            if (status != this._carrierStatus) {
                this._carrierStatus = status;
                this.getGraphics().updateCarrierGraphic();
            }
        },

        /**
         * Returns the global disorder carrier status for this person.
         *
         * @method getCarrier
         * @return {String} Dissorder carrier status
         */
        getCarrierStatus: function() {
            return this._carrierStatus;
        },

        /**
         * Returns the list of all colors associated with the node
         * (e.g. all colors of all disorders and all colors of all the genes)
         * @method getAllNodeColors
         * @return {Array of Strings}
         */
        getAllNodeColors: function() {
            var result = [];
            var allLegends = editor.getAllLegends();
            for (var i = 0; i < allLegends.length; i++) {
                Array.prototype.push.apply(result, allLegends[i].getAllEnabledColorsForNode(this.getID()));
            }
            return result;
        },

        /**
         * Returns a list of disorders of this person.
         *
         * @method getDisorders
         * @return {Array} List of disorder IDs.
         */
        getDisorders: function() {
            //console.log("Get disorders: " + Helpers.stringifyObject(this._disorders));
            return this._disorders;
        },

        /**
         * Returns a list of disorders of this person, with non-scrambled IDs
         *
         * @method getDisordersForExport
         * @return {Array} List of human-readable versions of disorder IDs
         */
        getDisordersForExport: function() {
            var exportDisorders = this._disorders.slice(0);
            return exportDisorders;
        },

        /**
         * Adds disorder to the list of this node's disorders and updates the Legend.
         *
         * @method addDisorder
         * @param {Disorder} disorder Disorder object or a free-text name string
         */
        addDisorder: function(disorder) {
            if (typeof disorder != 'object') {
                disorder = editor.getDisorderLegend().getDisorder(disorder);
            }
            if(!this.hasDisorder(disorder.getDisorderID())) {
                editor.getDisorderLegend().addCase(disorder.getDisorderID(), disorder.getName(), this.getID());
                this.getDisorders().push(disorder.getDisorderID());
            }
            else {
                alert("This person already has the specified disorder");
            }

            // if any "real" disorder has been added
            // the virtual "affected" disorder should be automatically removed
            if (this.getDisorders().length > 1) {
                this.removeDisorder("affected");
            }
        },

        /**
         * Removes disorder from the list of this node's disorders and updates the Legend.
         *
         * @method removeDisorder
         * @param {Number} disorderID id of the disorder to be removed
         */
        removeDisorder: function(disorderID) {
            if(this.hasDisorder(disorderID)) {
                editor.getDisorderLegend().removeCase(disorderID, this.getID());
                this._disorders = this.getDisorders().without(disorderID);
            }
            else {
                if (disorderID != "affected") {
                    alert("This person doesn't have the specified disorder");
                }
            }
        },

        /**
         * Sets the list of disorders of this person to the given list
         *
         * @method setDisorders
         * @param {Array} disorders List of Disorder objects
         */
        setDisorders: function(disorders) {
            //console.log("Set disorders: " + Helpers.stringifyObject(disorders));
            for(var i = this.getDisorders().length-1; i >= 0; i--) {
                this.removeDisorder( this.getDisorders()[i] );
            }
            for(var i = 0; i < disorders.length; i++) {
                this.addDisorder( disorders[i] );
            }
            this.getGraphics().updateDisorderShapes();
            this.setCarrierStatus(); // update carrier status
        },

        /**
         * Returns a list of all HPO terms associated with the patient
         *
         * @method getHPO
         * @return {Array} List of HPO IDs.
         */
        getHPO: function() {
            return this._hpo;
        },

        /**
         * Adds HPO term to the list of this node's phenotypes and updates the Legend.
         *
         * @method addHPO
         * @param {HPOTerm} hpo HPOTerm object or a free-text name string
         */
        addHPO: function(hpo) {
            if (typeof hpo != 'object') {
                hpo = editor.getHPOLegend().getTerm(hpo);
            }
            if(!this.hasHPO(hpo.getID())) {
                editor.getHPOLegend().addCase(hpo.getID(), hpo.getName(), this.getID());
                this.getHPO().push(hpo.getID());
            }
            else {
                alert("This person already has the specified phenotype");
            }
        },

        /**
         * Removes HPO term from the list of this node's terms and updates the Legend.
         *
         * @method removeHPO
         * @param {Number} hpoID id of the term to be removed
         */
        removeHPO: function(hpoID) {
            if(this.hasHPO(hpoID)) {
                editor.getHPOLegend().removeCase(hpoID, this.getID());
                this._hpo = this.getHPO().without(hpoID);
            }
            else {
                alert("This person doesn't have the specified HPO term");
            }
        },

        /**
         * Sets the list of HPO temrs of this person to the given list
         *
         * @method setHPO
         * @param {Array} hpos List of HPOTerm objects
         */
        setHPO: function(hpos) {
            for(var i = this.getHPO().length-1; i >= 0; i--) {
                this.removeHPO( this.getHPO()[i] );
            }
            for(var i = 0; i < hpos.length; i++) {
                this.addHPO( hpos[i] );
            }
            this.getGraphics().updateDisorderShapes();
        },

        /**
         * @method hasHPO
         * @param {Number} id Term ID, taken from the HPO database
         */
        hasHPO: function(id) {
            return (this.getHPO().indexOf(id) != -1);
        },

        /**
         * Sets the list of ethnicities of this person to the given list
         *
         * @method setEthnicities
         * @param {Array} ethnicities List of ethnicity names (as strings)
         */
        setEthnicities: function(ethnicities) {
            this._ethnicities = ethnicities;
        },

        /**
         * Returns a list of ethnicities of this person.
         *
         * @method getEthnicities
         * @return {Array} List of ethnicity names.
         */
        getEthnicities: function() {
            return this._ethnicities;
        },

        /**
         * Adds gene to the list of this node's genes.
         *
         * @param {Object} gene an object with "id" and "gene" properties, and posibly other properties
         * @param {String} status Status of the gene ("candidate", "solved", etc.)
         * @method addGene
         */
        _addGene: function(gene, status) {
            if (!gene.hasOwnProperty("id")) {
                gene.id = gene.gene;
            }

            var geneIndex = Person.getGeneIndex(gene.id, this.getGenes());

            if (geneIndex < 0 || this.getGenes()[geneIndex].status != status) {
                // unless already have the gene with the same status

                if (geneIndex == -1) {
                    // new gene
                    var newGene = Helpers.cloneObject(gene);
                    newGene["status"] = status;
                    this.getGenes().push(newGene);
                    geneIndex = this.getGenes().length - 1;
                } else {
                    // change of status from the previous (by this point we know old_status != new_status)
                    var oldStatus = this.getGenes()[geneIndex].status;
                    this.getGenes()[geneIndex].status = status;
                    // some statuses may have no legend
                    editor.getGeneLegend(oldStatus) && editor.getGeneLegend(oldStatus).removeCase(gene.id, this.getID());
                }
                // add to the legend corresponding to new status
                editor.getGeneLegend(status) && editor.getGeneLegend(status).addCase(gene.id, gene.gene, this.getID());
            }

            // in all cases update gene symbol to match the latest known for this ID by geneLegend
            if (editor.getGeneLegend(status)) {
                this.getGenes()[geneIndex].gene = editor.getGeneLegend(status).getGene(gene.id).getSymbol();
            }
        },

        /**
         * Removes gene from the list of this node's genes
         *
         * @param {String} geneID gene id
         * @method removeGene
         */
        _removeGene: function(geneID, status) {
            var geneIndex = Person.getGeneIndex(geneID, this.getGenes());
            if (geneIndex >=0) {
                editor.getGeneLegend(status).removeCase(geneID, this.getID());
                this.getGenes().splice(geneIndex, 1);
            }
        },

        /**
         * Sets the genes for this person. If `status` is defined, all given genes will be given that status.
         * If not, each gene will preserve the status defined for the gene in the newGenes array.
         */
        _setGenes: function(newGenes, status) {
            // remove genes that are no longer in the list.
            // iterating in reverse since array size may get decreased during removals
            for (var i = this.getGenes().length - 1; i >= 0; i--) {
                if ((!status || this.getGenes()[i].status == status) && Person.getGeneIndex(this.getGenes()[i].id, newGenes) == -1) {
                    this._removeGene(this.getGenes()[i].id, status ? status : this.getGenes()[i].status);
                }
            }

            // add all genes which should be present (adding an already existing gene works as expected)
            for(var i = 0; i < newGenes.length; i++) {
                this._addGene( newGenes[i], status ? status : newGenes[i].status );
            }
            this.getGraphics().updateDisorderShapes();
        },

        /**
         * Sets the list of candidate genes of this person to the given list
         *
         * @method setCandidateGenes
         * @param {Array} genes List of gene names (as strings)
         */
        setCandidateGenes: function(genes) {
            this._setGenes(genes, "candidate");
        },

        /**
         * Sets the list of confirmed causal genes of this person to the given list
         *
         * @method setCausalGenes
         * @param {Array} genes List of gene names (as strings)
         */
        setCausalGenes: function(genes) {
            this._setGenes(genes, "solved");
        },

        /**
         * Sets the list of tested negative genes of this person to the given list
         *
         * @method setRejectedGenes
         * @param {Array} genes List of gene names (as strings)
         */
        setRejectedGenes: function(genes) {
            this._setGenes(genes, "rejected");
        },

        /**
         * Sets the list of rejected candidate genes of this person to the given list
         *
         * @method setRejectedCandidate
         * @param {Array} genes List of gene names (as strings)
         */
        setRejectedCandidateGenes: function(genes) {
            this._setGenes(genes, "rejected_candidate");
        },

        /**
         * Sets the list of carrier genes of this person to the given list
         *
         * @method setCarrierGenes
         * @param {Array} genes List of gene names (as strings)
         */
        setCarrierGenes: function(genes) {
            this._setGenes(genes, "carrier");
        },

        // used by controller in conjuntion with setCandidateGenes
        getCandidateGenes: function() {
            return this._getGeneArray("candidate");
        },

        getCausalGenes: function() {
            return this._getGeneArray("solved");
        },

        getRejectedGenes: function() {
            return this._getGeneArray("rejected");
        },

        getRejectedCandidateGenes: function() {
            return this._getGeneArray("rejected_candidate");
        },

        getCarrierGenes: function() {
            return this._getGeneArray("carrier");
        },

        // returns null if geneID is not present, or gene status if it is
        getGeneStatus: function(geneID) {
            var geneIndex = Person.getGeneIndex(geneID, this.getGenes());
            if (geneIndex == -1) {
                return null;
            }
            return this.getGenes()[geneIndex].status;
        },

        // returns an array of Gene objects (as accepted by NodeMenu) of genes which are
        // present in this patient with the given status
        _getGeneArray: function(geneStatus) {
            var geneArray = [];
            for (var i = 0; i < this.getGenes().length; i++) {
                if (this.getGenes()[i].status == geneStatus) {
                    geneArray.push(Helpers.cloneObject(this.getGenes()[i]));
                }
            }
            return geneArray;
        },

        /**
         * Returns a list of reported genes for this person (both causal and candidate and other)
         *
         * @method getGenes
         * @return {Array} Array of gene objects, each having properties "id","gene", "status" and possibly
         *                 others such as "comments","strategy", etc.
         */
        getGenes: function() {
            return this._genes;
        },

        /**
         * Adds cancer to the list of this node's common cancers
         *
         * @param cancerID String
         * @param cancerDetails Object {affected: Boolean, id: String, label: String, qualifiers: [{laterality: String, primary: Boolean, numericAgeAtDiagnosis: Number, ageAtDiagnosis: String, comments: String}]}
         * @method addCancer
         */
        addCancer: function(cancerID, cancerDetails) {
            if (!this.getCancers().hasOwnProperty(cancerID)) {
                if (cancerDetails.hasOwnProperty("affected") && cancerDetails.affected) {
                    var cancerName = cancerDetails.hasOwnProperty("label") ? cancerDetails.label : null;
                    editor.getCancerLegend().addCase(cancerID, cancerName, this.getID());
                }
                this.getCancers()[cancerID] = cancerDetails;
            }
        },

        /**
         * Removes cancer from the list of this node's common cancers
         *
         * @method removeCancer
         */
        removeCancer: function(cancerID) {
            if (this.getCancers().hasOwnProperty(cancerID)) {
                editor.getCancerLegend().removeCase(cancerID, this.getID());
                delete this._cancers[cancerID];
            }
        },

        /**
         * Sets the set of common cancers affecting this person to the given set
         *
         * @method setCancers
         * @param {Array} cancers Array of cancers, each with the following format: Object {affected: Boolean, id:
         *                        String, label: String, qualifiers: Object [{laterality: String, primary: Boolean,
         *                        numericAgeAtDiagnosis: Number, ageAtDiagnosis: String, comments: String}]}
         */
        setCancers: function(cancers) {
            for (var cancerID in this.getCancers()) {
                if (this.getCancers().hasOwnProperty(cancerID)) {
                    this.removeCancer(cancerID);
                }
            }

            for (var i = 0; i < cancers.length; i++) {
                var cancer = cancers[i];
                if (cancer.hasOwnProperty("id")) {
                    this.addCancer(cancer.id, cancer);
                }
            }
            this.getGraphics().updateDisorderShapes();
            this.getGraphics().updateCancerAgeOfOnsetLabels();
        },

        /**
         * Returns a list of common cancers affecting this person.
         *
         * @method getCancers
         * @return {Object}  { Name: {affected: Boolean, numericAgeAtDiagnosis: Number, ageAtDiagnosis: String, comments: String} }
         */
        getCancers: function() {
            return this._cancers;
        },

        /**
         * Removes the node and its visuals.
         *
         * @method remove
         * @param [skipConfirmation=false] {Boolean} if true, no confirmation box will pop up
         */
        remove: function($super) {
            var extensionParameters = { "node": this };
            editor.getExtensionManager().callExtensions("personNodeRemoved", extensionParameters);

            this.setPhenotipsPatientId("");
            this.setDisorders([]);  // remove disorders form the legend
            this.setHPO([]);
            this.setCandidateGenes([]);
            this.setCausalGenes([]);
            this.setCarrierGenes([]);
            this.setCancers([]);
            $super();
        },

        /**
         * Returns disorder with given id if this person has it. Returns null otherwise.
         *
         * @method getDisorderByID
         * @param {Number} id Disorder ID, taken from the OMIM database
         * @return {Disorder}
         */
        hasDisorder: function(id) {
            return (this.getDisorders().indexOf(id) != -1);
        },

        /**
         * Changes the childless status of this Person. Nullifies the status if the given status is not
         * "childless" or "infertile". Modifies the status of the partnerships as well.
         *
         * @method setChildlessStatus
         * @param {String} status Can be "childless", "infertile" or null
         * @param {Boolean} ignoreOthers If True, changing the status will not modify partnerships's statuses or
         * detach any children
         */
        setChildlessStatus: function(status) {
            if(!this.isValidChildlessStatus(status))
                status = null;
            if(status != this.getChildlessStatus()) {
                this._childlessStatus = status;
                this.setChildlessReason(null);
                this.getGraphics().updateChildlessShapes();
                this.getGraphics().getHoverBox().regenerateHandles();
            }
            return this.getChildlessStatus();
        },

       /**
        * Returns cancers as an array of cancer objects.
        *
        * @return {Array} of cancer objects
        */
        getPhenotipsFormattedCancers: function() {
            var formattedCancers = [];
            var cancers = this.getCancers();
            for (var cancerID in cancers) {
                if (cancers.hasOwnProperty(cancerID)) {
                    formattedCancers.push(cancers[cancerID]);
                }
            }
            return formattedCancers;
        },

        /**
         * Returns an object (to be accepted by node menu) with information about this Person
         *
         * @method getSummary
         * @return {Object} Summary object for the menu
         */
        getSummary: function() {
            var onceAlive = editor.getGraph().hasRelationships(this.getID());
            var inactiveStates = onceAlive ? ['unborn','aborted','miscarriage','stillborn'] : false;
            var disabledStates = false;
            // disallow states not suported by PhenoTips
            if (this.getPhenotipsPatientId() != "") {
                disabledStates = ['unborn','aborted','miscarriage','stillborn'];
                Helpers.removeFirstOccurrenceByValue(disabledStates,this.getLifeStatus())
            }

            var childlessInactive = this.isFetus();  // TODO: can a person which already has children become childless?
                                                     // maybe: use editor.getGraph().hasNonPlaceholderNonAdoptedChildren() ?
            var disorders = [];
            this.getDisorders().forEach(function(disorder) {
                var disorderName = editor.getDisorderLegend().getDisorder(disorder).getName();
                disorders.push({id: disorder, value: disorderName});
            });
            var hpoTerms = [];
            this.getHPO().forEach(function(hpo) {
                var termName = editor.getHPOLegend().getTerm(hpo).getName();
                hpoTerms.push({id: hpo, value: termName});
            });

            var cantChangeAdopted = this.isFetus() || editor.getGraph().hasToBeAdopted(this.getID());

            var inactiveMonozygothic = true;
            var disableMonozygothic  = true;
            if (this._twinGroup !== null) {
                var twins = editor.getGraph().getAllTwinsSortedByOrder(this.getID());
                if (twins.length > 1) {
                    // check that there are twins and that all twins
                    // have the same gender, otherwise can't be monozygothic
                    inactiveMonozygothic = false;
                    disableMonozygothic  = false;
                    for (var i = 0; i < twins.length; i++) {
                        if (editor.getGraph().getGender(twins[i]) != this.getGender()) {
                            disableMonozygothic = true;
                            break;
                        }
                    }
                }
            }

            var inactiveCarriers = [];
            if (disorders.length > 0) {
                if (disorders.length != 1 || disorders[0].id != "affected") {
                    inactiveCarriers = [''];
                }
            }
            if (this.getLifeStatus() == "aborted" || this.getLifeStatus() == "miscarriage") {
                inactiveCarriers.push('presymptomatic');
            }

            var inactiveLostContact = this.isProband() || !editor.getGraph().isRelatedToProband(this.getID());

            var rejectedGeneList = this.getRejectedGenes();
            var rejectedCandidateGeneList = this.getRejectedCandidateGenes();
            var carrierGeneList  = this.getCarrierGenes();

            var disabledDeathDetails = (this.getLifeStatus() != 'stillborn' && this.getLifeStatus() != 'miscarriage' && this.getLifeStatus() != 'deceased');

            // TODO: only suggest posible birth dates which are after the latest
            //       birth date of any ancestors; only suggest death dates which are after birth date

            var menuData = {
                identifier:      {value : this.getID()},
                first_name:      {value : this.getFirstName(), disabled: false},
                last_name:       {value : this.getLastName(), disabled: false},
                last_name_birth: {value : this.getLastNameAtBirth()}, //, inactive: (this.getGender() != 'F')},
                external_id:     {value : this.getExternalID(), disabled: false},
                gender:          {value : this.getGender()},
                date_of_birth:   {value : this.getBirthDate(), inactive: this.isFetus(), disabled: false},
                carrier:         {value : this.getCarrierStatus(), disabled: inactiveCarriers},
                disorders:       {value : disorders, disabled: false},
                ethnicity:       {value : this.getEthnicities()},
                candidate_genes: {value : this.getCandidateGenes(), disabled: false},
                causal_genes:    {value : this.getCausalGenes(), disabled: false},
                carrier_genes:   {value : this.getCarrierGenes(), disabled: false},
                rejected_genes:  {value : rejectedGeneList, disabled: true, inactive: (rejectedGeneList.length == 0)},
                rejected_candidate_genes:  {value : rejectedCandidateGeneList, disabled: true, inactive: (rejectedCandidateGeneList.length == 0)},
                adopted:         {value : this.getAdopted(), inactive: cantChangeAdopted},
                state:           {value : this.getLifeStatus(), inactive: inactiveStates, disabled: disabledStates},
                aliveandwell:    {value : this.getAliveAndWell(), inactive: this.isFetus()},
                deceasedAge:     {value : this.getDeceasedAge(), inactive: this.isFetus(), disabled : disabledDeathDetails},
                deceasedCause:   {value : this.getDeceasedCause(), disabled : disabledDeathDetails},
                date_of_death:   {value : this.getDeathDate(), inactive: this.isFetus(), disabled: false},
                commentsClinical:{value : this.getComments(), inactive: false},
                commentsPersonal:{value : this.getComments(), inactive: false},  // so far the same set of comments is displayed on all tabs
                commentsCancers: {value : this.getComments(), inactive: false},
                gestation_age:   {value : this.getGestationAge(), inactive : !this.isFetus()},
                childlessSelect: {value : this.getChildlessStatus() ? this.getChildlessStatus() : 'none', inactive : childlessInactive},
                childlessText:   {value : this.getChildlessReason() ? this.getChildlessReason() : undefined, inactive : childlessInactive, disabled : !this.getChildlessStatus()},
                placeholder:     {value : false, inactive: true },
                monozygotic:     {value : this.getMonozygotic(), inactive: inactiveMonozygothic, disabled: disableMonozygothic },
                evaluated:       {value : this.getEvaluated() },
                hpo_positive:    {value : hpoTerms, disabled: false },
                nocontact:       {value : this.getLostContact(), inactive: inactiveLostContact },
                cancers:         {value : this.getCancers() },
                cancers_picker:  {value : {} },
                phenotipsid:     {value : this.getPhenotipsPatientId() },
                setproband:      {value : "allow", inactive: this.isProband() }
            };

            var extensionParameters = { "menuData": menuData, "node": this };
            menuData = editor.getExtensionManager().callExtensions("personGetNodeMenuData", extensionParameters).extendedData.menuData;

            // Disable input fields iff current user does not have edit permissions for this patient
            if (!editor.getPatientAccessPermissions(this.getPhenotipsPatientId()).hasEdit) {
                for (var prop in menuData) {
                    if (menuData.hasOwnProperty(prop)) {
                        menuData[prop].disabled = true;
                    }
                }
            }

            return menuData;
        },

        /**
         * Returns an object containing all the properties of this node
         * (except graph properties {id, x, y} which are independent of logical pedigree node properties)
         *
         * @method getProperties
         * @return {Object} with all node properties
         */
        getProperties: function($super) {
            // note1: once new properties are added need to update
            //        getNodePropertiesNotStoredInPatientProfile() as well
            //
            //
            // note2: properties equivalent to default are not set

            var info = $super();
            if (this.getPhenotipsPatientId() != "")
                info['phenotipsId'] = this.getPhenotipsPatientId();
            if (this.getFirstName() != "")
                info['fName'] = this.getFirstName();
            if (this.getLastName() != "")
                info['lName'] = this.getLastName();
            if (this.getLastNameAtBirth() != "")
                info['lNameAtB'] = this.getLastNameAtBirth();
            if (this.getExternalID() != "")
                info['externalID'] = this.getExternalID();
            if (this.getBirthDate() != null)
                info['dob'] = this.getBirthDate().getSimpleObject();
            if (this.getAdopted() != "")
                info['adoptedStatus'] = this.getAdopted();
            if (this.getLifeStatus() != 'alive')
                info['lifeStatus'] = this.getLifeStatus();
            if (this.getAliveAndWell())
                info['aliveandwell'] = this.getAliveAndWell();
            if (this.getDeceasedAge() != "")
                info['deceasedAge'] = this.getDeceasedAge();
            if (this.getDeceasedCause() != "")
                info['deceasedCause'] = this.getDeceasedCause();
            if (this.getDeathDate() != null)
                info['dod'] = this.getDeathDate().getSimpleObject();
            if (this.getGestationAge() != null)
                info['gestationAge'] = this.getGestationAge();
            if (this.getChildlessStatus() != null) {
                info['childlessStatus'] = this.getChildlessStatus();
                info['childlessReason'] = this.getChildlessReason();
            }
            if (this.getDisorders().length > 0)
                info['disorders'] = this.getDisordersForExport();
            if (!Helpers.isObjectEmpty(this.getCancers()))
                info['cancers'] = this.getPhenotipsFormattedCancers();

            // convert HPO data to PhenoTips feature format
            // (which stores features and non-standard features separately)
            var phenotipsFeatures = this.getPhenotipsFormattedFeatures();
            info['features']             = phenotipsFeatures.features;
            info['nonstandard_features'] = phenotipsFeatures.nonstandard_features;

            // convert pedigree genes to PhenoTips gene format
            info['genes'] = this.getGenes().slice();

            if (this.getEthnicities().length > 0)
                info['ethnicities'] = this.getEthnicities().slice();
            if (this._twinGroup !== null)
                info['twinGroup'] = this._twinGroup;
            if (this._monozygotic)
                info['monozygotic'] = this._monozygotic;
            if (this._evaluated)
                info['evaluated'] = this._evaluated;
            if (this._carrierStatus)
                info['carrierStatus'] = this._carrierStatus;
            if (this.getLostContact())
                info['lostContact'] = this.getLostContact();
            if (this.getPedNumber() != "")
                info['nodeNumber'] = this.getPedNumber();

            var extensionParameters = { "modelData": info, "node": this };
            info = editor.getExtensionManager().callExtensions("personToModel", extensionParameters).extendedData.modelData;

            return info;
         },

         /**
          * Updates stored features as read from patient JSON to include
          * features manually added and exclude features explicitly removed.
          * Returns an array of features in Phenotips patient JSON format.
          *
          * 1) One complication is that pedigree only supports positive phenotypes,
          *    so in the following case:
          *  OLD:
          *     [ A: observed, B: observed, C: not observed, D: not observed, Z: not a phenotype ]
          *
          *  NEW:
          *     [ A, C, E ]    // all assumed ot be observed
          *
          *  The resulting set of features should be:
          *     [ A: observed, C: observed, D: not observed, E: observed, Z: not a phenotype ]
          *
          *  ...since B is assumed to have been removed, C and E added, and D and Z left as is.
          *
          *  2) Any qualifiers that A or D or Z above had should be preserved; new features are assumed
          *  to have no qualifiers since UI does not support them (yet). C is assumd ot have no qualifiers
          *  as well since old qualifiers applied to the "non observed" version of the feature.
          *
          *  3) Another complication is that PhenoTips stores "standard" (with an HPPO id) and
          *     custom (user-entered) features separately
          */
         getPhenotipsFormattedFeatures: function() {
             var newFeatures = [];
             var newNonStdFeatures = [];

             var hpo = Helpers.toObjectWithTrue(this.getHPO());

             // go over all old features and transfer:
             //  1) those which are non-phenotypes (transfer completely as is)
             //  2) those which are still observed (keep qualifiers)
             //  3) those which were not observed and still are not (keep qualifiers)
             for (var i = 0; i < this.features.length; i++) {
                 if ( (this.features[i].type != "phenotype") ||
                      (this.features[i].observed === "yes" && hpo.hasOwnProperty(this.features[i].id)) ||
                      (this.features[i].observed === "no"  && !hpo.hasOwnProperty(this.features[i].id))
                    ) {
                     newFeatures.push(this.features[i]);
                 }
             }

             // go over all old non-std-features and do the same as above
             for (var i = 0; i < this.nonStandardFeatures.length; i++) {
                 if ( (this.nonStandardFeatures[i].type != "phenotype") ||
                      (this.nonStandardFeatures[i].observed === "yes" && hpo.hasOwnProperty(this.nonStandardFeatures[i].label)) ||
                      (this.nonStandardFeatures[i].observed === "no"  && !hpo.hasOwnProperty(this.nonStandardFeatures[i].label))
                    ) {
                     newNonStdFeatures.push(this.nonStandardFeatures[i]);
                 }
             }

             var toObjectWithTrueByField = function(arrayOfObjects, objectFiledName) {
                 var arrayOfFieldValues = [];
                 for (var i = 0; i < arrayOfObjects.length; i++) {
                     arrayOfFieldValues.push(arrayOfObjects[i][objectFiledName]);
                 }
                 return Helpers.toObjectWithTrue(arrayOfFieldValues);
             };
             var featureHash       = toObjectWithTrueByField(newFeatures, "id");
             var nonStdFeatureHash = toObjectWithTrueByField(newNonStdFeatures, "label");

             // go over all observed features and add them to either newFeatures or newNonStdFeatures
             // (if not already there) - with no qualifiers
             for (var i = 0; i < this.getHPO().length; i++) {
                 var term = this.getHPO()[i];
                 if (HPOTerm.isValidID(term)) {
                     // this is supposed to be a standard term
                     if (!featureHash.hasOwnProperty(term)) {
                         newFeatures.push( { "id": term,
                                             "observed": "yes",
                                             "type": "phenotype",
                                             "label": editor.getHPOLegend().getTerm(term).getName() } );
                     }
                 } else {
                     if (!nonStdFeatureHash.hasOwnProperty(term)) {
                         newNonStdFeatures.push( { "observed": "yes",
                                                   "type": "phenotype",
                                                   "label": term } );
                     }
                 }
             }

             return {"features": newFeatures, "nonstandard_features": newNonStdFeatures};
         },

         /**
          * These properties are related to pedigree structure, but not to PhenoTips patient.
          *
          * Used to decide which properties to keep when the link to PhenoTips patient is removed
          */
         getPatientIndependentProperties: function() {
             // TODO: review the set of properties retained
             return  { "gender"        : this.getGender(),
                       "adoptedStatus" : this.getAdopted(),
                       "twinGroup"     : this._twinGroup,
                       "monozygotic"   : this._monozygotic,
                       "nodeNumber"    : this.getPedNumber() };
         },

         /**
          * Applies the properties found in info to this node.
          *
          * @method assignProperties
          * @param properties Object
          * @return {Boolean} True if info was successfully assigned
          */
         assignProperties: function($super, info) {
            if($super(info)) {
                // setGenderGraphics() and drawLabels() methods may be slow yet may be
                // called myultiple times while properties are set as part of property update procedure.
                // (e.g. setFirstname(), setComments(), etc. set labels, and setAdopted() calls setGenderGraphics.
                // This speedup disables the methods while initial properties are set and it is later execute once after
                // all properties have been set and labels have been generated
                this._speedup_NOREDRAW = true;
                this._speedup_NEEDTOCALL = {};

                if(info.phenotipsId) {
                    if (this.getPhenotipsPatientId() != info.phenotipsId) {
                        this.setPhenotipsPatientId(info.phenotipsId);
                    }
                } else {
                    this.setPhenotipsPatientId("");
                }

                if(info.fName) {
                    if (this.getFirstName() != info.fName) {
                        this.setFirstName(info.fName);
                    }
                } else {
                    this.setFirstName("");
                }
                if(info.lName) {
                    if (this.getLastName() != info.lName) {
                        this.setLastName(info.lName);
                    }
                } else {
                    this.setLastName("");
                }
                if(info.lNameAtB) {
                    if (this.getLastNameAtBirth() != info.lNameAtB) {
                        this.setLastNameAtBirth(info.lNameAtB);
                    }
                } else {
                    this.setLastNameAtBirth("");
                }
                if (info.externalID) {
                    if (this.getExternalID() != info.externalID) {
                        this.setExternalID(info.externalID);
                    }
                } else {
                    this.setExternalID("");
                }
                if(info.dob) {
                    if (this.getBirthDate() != info.dob) {
                        this.setBirthDate(info.dob);
                    }
                } else {
                    this.setBirthDate(null);
                }
                if(info.dod) {
                    if (this.getDeathDate() != info.dod) {
                        this.setDeathDate(info.dod);
                    }
                } else {
                    this.setDeathDate(null);
                }
                if(info.disorders) {
                    var disordersCopy = info.disorders.slice();
                    this.setDisorders(disordersCopy);
                } else {
                    this.setDisorders([]);
                }
                if(info.cancers) {
                    var cancersCopy = info.cancers.slice();
                    this.setCancers(cancersCopy);
                } else {
                    this.setCancers([]);
                }

                // save original feature data
                this.features = info.hasOwnProperty("features")? info.features.slice(0) : [];
                this.nonStandardFeatures = info.hasOwnProperty("nonstandard_features") ? info.nonstandard_features.slice(0) : [];

                var hpoTerms = [];
                if (info.features) {
                    // "features":
                    //    [
                    //     {"id":"HP:0000175","observed":"yes","label":"Cleft palate","type":"phenotype", "qualifiers":[{"id":"HP:0003577","label":"Congenital onset","type":"age_of_onset"}]},
                    //     {"id":"HP:0000204","observed":"no","label":"Cleft upper lip","type":"phenotype"}
                    //    ]
                    for (var i = 0; i < info.features.length; i++) {
                        if (info.features[i].observed === "yes" && info.features[i].type == "phenotype") {
                            hpoTerms.push(info.features[i].id);
                        }
                    }
                }
                if (info.nonstandard_features) {
                    // "nonstandard_features":
                    //    [
                    //     {"observed":"no","label":"xxx","type":"phenotype"},
                    //     {"observed":"yes","label":"zzz","type":"phenotype"}
                    //    ]
                    for (var i = 0; i < info.nonstandard_features.length; i++) {
                        if (info.nonstandard_features[i].observed === "yes" && info.nonstandard_features[i].type == "phenotype") {
                            hpoTerms.push(info.nonstandard_features[i].label);
                        }
                    }
                }
                this.setHPO(hpoTerms);

                if(info.ethnicities) {
                    var ethnicitiesCopy = info.ethnicities.slice();
                    this.setEthnicities(ethnicitiesCopy);
                } else {
                    this.setEthnicities([]);
                }

                if(info.genes) {
                    // genes: [ {id: 'ENSG00000168765', gene: 'GSTM4', status: 'candidate', comments: 'abc'},
                    //          {id: 'ENSG00000243055', gene: 'GK-AS1', status: 'rejected', strategy: ['deletion']},
                    //          {id: 'ENSG00000160688', gene: 'FLAD1', status: 'solved'},
                    //          {id: 'custom', 'gene': 'custom', status: 'candidate'} ]
                    this._setGenes(info.genes);
                } else {
                    this._setGenes([]);
                }

                if(info.hasOwnProperty("adoptedStatus")) {
                    if (this.getAdopted() != info.adoptedStatus) {
                        this.setAdopted(info.adoptedStatus);
                    }
                } else {
                    this.setAdopted("");
                }
                if(info.hasOwnProperty("lifeStatus")) {
                    if (this.getLifeStatus() != info.lifeStatus) {
                        this.setLifeStatus(info.lifeStatus);
                    }
                } else {
                    this.setLifeStatus('alive');
                }
                if(info.hasOwnProperty('aliveandwell')) {
                    if (this.getAliveAndWell() != info.aliveandwell) {
                        this.setAliveAndWell(info.aliveandwell);
                    }
                } else {
                    this.setAliveAndWell(false);
                }
                if(info.hasOwnProperty('deceasedAge')) {
                    if (this.getDeceasedAge() != info.deceasedAge) {
                        this.setDeceasedAge(info.deceasedAge);
                    }
                } else {
                    this.setDeceasedAge("");
                }
                if(info.hasOwnProperty('deceasedCause')) {
                    if (this.getDeceasedCause() != info.deceasedCause) {
                        this.setDeceasedCause(info.deceasedCause);
                    }
                } else {
                    this.setDeceasedCause("");
                }
                if(info.gestationAge) {
                    if (this.getGestationAge() != info.gestationAge) {
                        this.setGestationAge(info.gestationAge);
                    }
                } else {
                    this.setGestationAge("");
                }
                if(info.childlessStatus) {
                    if (this.getChildlessStatus() != info.childlessStatus) {
                        this.setChildlessStatus(info.childlessStatus);
                    }
                } else {
                    this.setChildlessStatus(null);
                }
                if(info.childlessReason) {
                    if (this.getChildlessReason() != info.childlessReason) {
                        this.setChildlessReason(info.childlessReason);
                    }
                } else {
                    this.setChildlessReason("");
                }
                if(info.hasOwnProperty("twinGroup")) {
                    if (this._twinGroup != info.twinGroup) {
                        this.setTwinGroup(info.twinGroup);
                    }
                } else {
                    this.setTwinGroup(null);
                }
                if(info.hasOwnProperty("monozygotic")) {
                    if (this._monozygotic != info.monozygotic) {
                        this.setMonozygotic(info.monozygotic);
                    }
                } else {
                    this.setMonozygotic(false);
                }
                if(info.hasOwnProperty("evaluated")) {
                    if (this._evaluated != info.evaluated) {
                        this.setEvaluated(info.evaluated);
                    }
                } else {
                    this.setEvaluated(false);
                }
                if(info.hasOwnProperty("carrierStatus")) {
                    if (this._carrierStatus != info.carrierStatus) {
                        this.setCarrierStatus(info.carrierStatus);
                    }
                } else {
                    this.setCarrierStatus("");
                }
                if (info.hasOwnProperty("nodeNumber")) {
                    if (this.getPedNumber() != info.nodeNumber) {
                        this.setPedNumber(info.nodeNumber);
                    }
                } else {
                    this.setPedNumber("");
                }
                if (info.hasOwnProperty("lostContact")) {
                    if (this.getLostContact() != info.lostContact) {
                        this.setLostContact(info.lostContact);
                    }
                } else {
                    this.setLostContact(false);
                }

                this._speedup_NOREDRAW = false;
                for (var method in this._speedup_NEEDTOCALL) {
                    if (this._speedup_NEEDTOCALL.hasOwnProperty(method)) {
                        this.getGraphics()[method]();
                    }
                }

                var extensionParameters = { "modelData": info, "node": this };
                editor.getExtensionManager().callExtensions("modelToPerson", extensionParameters);

                return true;
            }
            return false;
        }
    });

    //ATTACHES CHILDLESS BEHAVIOR METHODS TO THIS CLASS
    Person.addMethods(ChildlessBehavior);

    /**
     * Returns the index of the given gene in the given gene array
     * (assuming gene array format is the one used internally by Person),
     * or -1 if not present.
     *
     * @param {String} geneID
     * @param {Array} geneArray
     */
    Person.getGeneIndex = function(geneID, geneArray)
    {
        for (var i = 0; i < geneArray.length; i++) {
            if (geneArray[i].hasOwnProperty("id")) {
                if (geneArray[i].id.toUpperCase() == geneID.toUpperCase()) {
                    return i;
                }
            } else {
                if (geneArray.hasOwnProperty("gene") && geneArray[i].gene.toUpperCase() == geneID.toUpperCase()) {
                    return i;
                }
            }
        }
        return -1;
    }

    return Person;
});
