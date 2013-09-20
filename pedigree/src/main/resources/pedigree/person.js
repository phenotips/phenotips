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

var Person = Class.create(AbstractPerson, {

    initialize: function($super, x, y, gender, id) {
    	console.log("person");            
        this._firstName = "";
        this._lastName = "";
        this._birthDate = "";
        this._deathDate = "";
        this._conceptionDate = "";
        this._isAdopted = false;
        this._lifeStatus = 'alive';
        this._isProband = (id == 0);
        this._childlessStatus = null;
        this._childlessReason = "";
        this._disorders = [];
        this._evaluations = [];
        this._type = "Person";
        $super(x, y, gender, id);   // called after all the other variables are initialized because
                                    // one of the classes initializes graphics, which uses some of those
        console.log("person end");
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
        return this._isProband;
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
     * Returns the status of this Person
     *
     * @method getLifeStatus
     * @return {String} "alive", "deceased", "stillborn", "unborn" or "aborted"
     */
    getLifeStatus: function() {
        return this._lifeStatus;
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
     * Returns True is status is 'unborn', 'stillborn', 'aborted', 'alive' or 'deceased'
     *
     * @method _isValidLifeStatus
     * @param {String} status
     * @returns {boolean}
     * @private
     */
    _isValidLifeStatus: function(status) {
        return (status == 'unborn' || status == 'stillborn'
            || status == 'aborted'
            || status == 'alive' || status == 'deceased')
    },

    /**
     * Changes the life status of this Person to newStatus
     *
     * @method setLifeStatus
     * @param {String} newStatus "alive", "deceased", "stillborn", "unborn" or "aborted"
     */
    setLifeStatus: function(newStatus) {
        if(this._isValidLifeStatus(newStatus)) {
            this._lifeStatus = newStatus;

            (newStatus != 'deceased') && this.setDeathDate("");
            this.getGraphics().updateSBLabel();

            if(this.isFetus()) {
                this.setBirthDate("");
                this.setAdopted(false);
                this.setChildlessStatus(null);
            }
            this.getGraphics().updateLifeStatusShapes();
            editor.getNodeMenu().update(this,
                {
                    'gestation_age': {value : this.getGestationAge(), inactive : !this.isFetus()},
                    'date_of_birth': {value : this.getBirthDate(), inactive : this.isFetus()},
                    'adopted':       {value : this.isAdopted(), inactive: this.isFetus()},
                    'date_of_death': {value : this.getDeathDate(), inactive: newStatus != 'deceased'},
                    childlessSelect : {value : this.getChildlessStatus() ? this.getChildlessStatus() : 'none', inactive : this.isFetus()},
                    childlessText : {value : this.getChildlessReason() ? this.getChildlessReason() : 'none', inactive : this.isFetus()}
                });
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
        if(numWeeks){
            this._gestationAge = numWeeks;
            var daysAgo = numWeeks * 7,
                d = new Date();
            d.setDate(d.getDate() - daysAgo);
            this.setConceptionDate(d);
        }
        else {
            this.setConceptionDate(null);
        }
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
     * @param {Date} newDate Must be earlier date than deathDate and a later than conception date
     */
    setBirthDate: function(newDate) {
        newDate = newDate ? (new Date(newDate)) : '';
        if (!newDate || newDate && !this.getDeathDate() || newDate.getDate() < this.getDeathDate()) {
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
        deathDate = deathDate ? (new Date(deathDate)) : '';
        if(!deathDate || deathDate && !this.getBirthDate() || deathDate.getDate()>this.getBirthDate().getDate()) {
            this._deathDate =  deathDate;
            this._deathDate && (this.getLifeStatus() == 'alive') && this.setLifeStatus('deceased');
        }
        this.getGraphics().updateAgeLabel();
        return this.getDeathDate();
    },

    /**
     * Returns a list of disorders of this person.
     *
     * @method getDisorders
     * @return {Array} List of Disorder objects.
     */
    getDisorders: function() {
        return this._disorders;
    },

    /**
     * Adds disorder to the list of this node's disorders and updates the Legend.
     *
     * @method addDisorder
     * @param {Disorder} disorder Disorder object
     */
    addDisorder: function(disorder) {
        console.log("add disorder");
        if(!this.getDisorderByID(disorder.getDisorderID())) {
            editor.getLegend().addCase(disorder.getDisorderID(), disorder.getName(), this.getID());
            this.getDisorders().push(disorder);
        }
        this.getGraphics().updateDisorderShapes();
    },

    /**
     * Removes disorder from the list of this node's disorders and updates the Legend.
     *
     * @method removeDisorder
     * @param {Disorder} disorder
     * @param forceDisplay True if you want to display the change on the canvas
     */
    removeDisorder: function(disorder, forceDisplay) {
        console.log("remove disorder");
        var personsDisorder = null;
        if(personsDisorder = this.getDisorderByID(disorder.getDisorderID())) {
            editor.getLegend().removeCase(disorder.getDisorderID(), this.getID());
            this._disorders = this.getDisorders().without(personsDisorder);
        }
        else {
            alert("This person doesn't have the specified disorder");
        }
        forceDisplay && this.getGraphics().updateDisorderShapes();
    },

    /**
     * Given a list of disorders, adds and removes the disorders of this node to match
     * the new list
     *
     * @method setDisorders
     * @param {Array} disorders List of Disorder objects
     */
    setDisorders: function(disorders) {
        var me = this;
        this.getDisorders().each(function(disorder) {
            var found = false;
            disorders.each(function(newDisorder) {
                disorder.getDisorderID() == newDisorder.getDisorderID() && (found = true);
            });
            !found && me.removeDisorder(disorder, false);
        });
        disorders.each(function(newDisorder) {
            if (!me.getDisorderByID(newDisorder.getDisorderID())) {
                me.addDisorder(newDisorder);
            }
        });
        this.getGraphics().updateDisorderShapes();
    },

    /**
     * Returns disorder with given id if this person has it. Returns null otherwise.
     *
     * @method getDisorderByID
     * @param {Number} id Disorder ID, taken from the OMIM database
     * @return {Disorder}
     */
    getDisorderByID: function(id) {
        for(var i = 0; i < this.getDisorders().length; i++) {
            if(this.getDisorders()[i].getDisorderID() == id) {
                return this.getDisorders()[i];
            }
        }
        return null;
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
        }
        return this.getChildlessStatus();
    },

    /**
     * Returns an object (to be accepted by the menu) with information about this Person
     *
     * @method getSummary
     * @return {Object} Summary object for the menu
     */
    getSummary: function() {
        var childlessInactive = this.isFetus();  // TODO: can a person which already has children become childless?
        var disorders = [];
        this.getDisorders().forEach(function(disorder) {
            disorders.push({id: disorder.getDisorderID(), value: disorder.getName()});
        });
        return {
            identifier:    {value : this.getID()},
            first_name:    {value : this.getFirstName()},
            last_name:     {value : this.getLastName()},
            gender:        {value : this.getGender(), inactive: false}, // TODO: (this.getGender() != 'U' && this.getPartners().length > 0)},
            date_of_birth: {value : this.getBirthDate(), inactive: this.isFetus()},
            disorders:     {value : disorders},
            adopted:       {value : this.isAdopted(), inactive: this.isFetus()},
            state:         {value : this.getLifeStatus(), inactive: false},  // TODO: [(this.getPartnerships().length > 0) ? ['unborn','aborted','stillborn'] : ''].flatten()
            date_of_death: {value : this.getDeathDate(), inactive: this.getLifeStatus() != 'deceased'},
            gestation_age: {value : this.getGestationAge(), inactive : !this.isFetus()},
            childlessSelect : {value : this.getChildlessStatus() ? this.getChildlessStatus() : 'none', inactive : childlessInactive},
            childlessText : {value : this.getChildlessReason() ? this.getChildlessReason() : undefined, inactive : childlessInactive, disabled : !this.getChildlessStatus()}
        };
    },

    /**
     * Returns an object containing all the properties of this node
     * except id, x, y & type 
     *
     * @method getProperties
     * @return {Object} in the form
     *
     {
       property: value
     }
     */    
    getProperties: function($super) {
        var info = $super();
        info['fName']           = this.getFirstName();
        info['lName']           = this.getLastName();
        info['dob']             = this.getBirthDate();
        info['isAdopted']       = this.isAdopted();
        info['lifeStatus']      = this.getLifeStatus();
        info['dod']             = this.getDeathDate();
        info['gestationAge']    = this.getGestationAge();
        info['childlessStatus'] = this.getChildlessStatus();
        info['childlessReason'] = this.getChildlessReason();
        info['disorders']       = this.getDisorders();
        return info;
     },

     /**
      * Applies the properties found in info to this node.
      *
      * @method loadProperties
      * @param properties Object
      * @return {Boolean} True if info was successfully assigned
      */
     assignProperties: function($super, info) {
        if($super(info)) {
            if(info.fName && this.getFirstName() != info.fName) {
                this.setFirstName(info.fName);
            }
            if(info.lName && this.getLastName() != info.lName) {
                this.setLastName(info.lName);
            }
            if(info.dob && this.getBirthDate() != info.dob) {
                this.setBirthDate(info.dob);
            }
            if(info.disorders) {
                this.setDisorders(info.disorders);
            }
            if(info.isAdopted && this.isAdopted() != info.isAdopted) {
                this.setAdopted(info.isAdopted);
            }
            if(info.lifeStatus && this.getLifeStatus() != info.lifeStatus) {
                this.setLifeStatus(info.lifeStatus);
            }
            if(info.dod && this.getDeathDate() != info.dod) {
                this.setDeathDate(info.dod);
            }
            if(info.gestationAge && this.getGestationAge() != info.gestationAge) {
                this.setGestationAge(info.gestationAge);
            }
            if(info.childlessStatus && this.getChildlessStatus() != info.childlessStatus) {
                this.setChildlessStatus(info.childlessStatus);
            }
            if(info.childlessReason && this.getChildlessReason() != info.childlessReason) {
                this.setChildlessReason(info.childlessReason);
            }
            return true;
        }
        return false;
    }
});

//ATTACHES CHILDLESS BEHAVIOR METHODS TO THIS CLASS
Person.addMethods(ChildlessBehavior);