/*
 * Person is a class representing any AbstractPerson that has sufficient information to be
 * displayed on the final pedigree graph (printed or exported). Person objects
 * contain information about disorders, age and other relevant properties, as well
 * as graphical data to visualize this information.
 *
 * @param x the x coordinate on the Raphael canvas at which the node drawing will be centered
 * @param the y coordinate on the Raphael canvas at which the node drawing will be centered
 * @param gender either 'M', 'F' or 'U' depending on the gender
 * @param id a unique ID number
 */

var Person = Class.create(AbstractPerson, {

    initialize: function($super, x, y, gender, id, isProband) {
        this._firstName = null;
        this._lastName = null;
        this._birthDate = null;
        this._deathDate = null;
        this._conceptionDate = null;
        this._isAdopted = false;
        this._lifeStatus = 'alive';
        this._isProband = isProband;
        this._disorders = [];
        this._evaluations = [];
        $super(x,y,gender, id);
    },

    /*
     * Initializes the object responsible for creating graphics for this Person
     *
     * @param x the x coordinate on the canvas at which the node is centered
     * @param y the y coordinate on the canvas at which the node is centered
     */
    generateGraphics: function(x, y) {
        return new PersonVisuals(this, x, y);
    },

    /*
     * Returns "pn" ("Person Node")
     */
    getType: function() {
        return "pn";
    },

    isProband: function() {
        return this._isProband;
    },

    getEvaluations: function() {
        return this._evaluations;
    },

    setEvaluations: function(evaluationsArray) {
        this._evaluations = evaluationsArray;
    },

    /*
     * Returns the first name of this Person
     */
    getFirstName: function() {
        return this._firstName;
    },

    /*
     * Replaces the first name of this Person with firstName, and
     * (optionally) updates all the labels of this node, including the first name
     *
     * @param firstName any string that represents the first name of this Person
     * @param forceDisplay set to true if you want to display the change on the canvas
     */
    setFirstName: function(firstName, forceDisplay) {
        firstName && (firstName = firstName.charAt(0).toUpperCase() + firstName.slice(1));
        this._firstName = firstName;
        forceDisplay && this.getGraphics().drawLabels();
    },

    /*
     * Returns the last name of this Person
     */
    getLastName: function() {
        return this._lastName;
    },

    /*
     * Replaces the last name of this Person with lastName, and
     * (optionally) updates all the labels of this node, including the last name
     *
     * @param lastName any string that represents the last name of this Person
     * @param forceDisplay set to true if you want to display the change on the canvas
     */
    setLastName: function(lastName, forceDisplay) {
        lastName && (lastName = lastName.charAt(0).toUpperCase() + lastName.slice(1));
        this._lastName = lastName;
        forceDisplay && this.getGraphics().drawLabels();
    },
    /*
     * Returns the status of this Person, which can be "alive", "deceased", "stillborn" or "aborted"
     */
    getLifeStatus: function() {
        return this._lifeStatus;
    },

    isFetus: function() {
        return (this.getLifeStatus() == 'unborn' || this.getLifeStatus() == 'stillborn' || this.getLifeStatus() == 'aborted');
    },

    /*
     * Changes the status of this Person to newStatus, updates the menu and (optionally) updates
     * the graphics
     *
     * @param newStatus can be "alive", "deceased", "stillborn", "unborn" or "aborted"
     * @param forceDisplay set to true if you want to display the change on the canvas
     */
    setLifeStatus: function(newStatus, forceDisplay) {
        if(newStatus == 'unborn' || newStatus == 'stillborn' || newStatus == 'aborted' || newStatus == 'alive' || newStatus == 'deceased'){
            this._lifeStatus = newStatus;
            forceDisplay && this.getGraphics().draw();

            if(newStatus != 'deceased') {
                this.setDeathDate(null, false);
            }

            if(this.isFetus()) {
                this.setBirthDate(null, false);
            }

            forceDisplay && this.getGraphics().draw();
            editor.nodeMenu.update(this,
                {
                    'gestation_age': {value : this.getGestationAge(), inactive : !this.isFetus()},
                    'date_of_birth': {value : this.getBirthDate(), inactive : this.isFetus()},
                    'adopted':       {value : this.isAdopted(), inactive: this.isFetus()},
                    'date_of_death': {value : this.getDeathDate(), inactive: this.isFetus()}
                });
        }
    },

    /*
     * Returns the date object representing the conception date of this Person
     */
    getConceptionDate: function() {
        return this._conceptionDate;
    },

    /*
     * If this Person is marked as a Fetus, this method replaces the conception date with newDate
     * and (optionally) updates the graphics
     *
     * @param newDate a javascript Date object
     * @param forceDisplay set to true if you want to display the change on the canvas
     */
    setConceptionDate: function(newDate, forceDisplay) {
        this._conceptionDate = newDate;
        forceDisplay && this.getGraphics().drawLabels();
    },

    /*
     * Returns the number of weeks passed since conception
     */
    getGestationAge: function() {
        if(this.getLifeStatus() == 'unborn' && this.getConceptionDate()) {
            var oneWeek = 1000 * 60 * 60 * 24 * 7,
            lastDay = new Date();
            return Math.round((lastDay.getTime()- this.getConceptionDate().getTime())/oneWeek)
        }
        else if(this.isFetus()){
            return this._gestationAge;
        }
        else {
            return null;
        }
    },

    /*
     * Updates the conception age of the Person given the number of weeks passed since conception,
     * and (optionally) updates the graphics
     *
     * @param numWeeks a number greater than or equal to 0
     * @param forceDisplay set to true if you want to display the change on the canvas
     */
    setGestationAge: function(numWeeks, forceDisplay) {
        if(numWeeks){
            this._gestationAge = numWeeks;
            var daysAgo = numWeeks * 7,
                d = new Date();
            d.setDate(d.getDate() - daysAgo);
            this.setConceptionDate(d, forceDisplay);
        }
        else {
            this.setConceptionDate(null, forceDisplay);
        }
    },

    /*
     * Returns the date object representing the birth date of this Person
     */
    getBirthDate: function() {
        return this._birthDate;
    },

    /*
     * If this Person is not marked as a Fetus, this method replaces the birth date with newDate
     * and (optionally) updates the graphics
     *
     * @param newDate a javascript Date object, that must be an earlier date than deathDate and
     * a later date than conception date
     * @param forceDisplay set to true if you want to display the change on the canvas
     */
    setBirthDate: function(newDate, forceDraw) {
        if (!newDate || newDate && !this.getDeathDate() || newDate.getDate() < this.getDeathDate()) {
            this._birthDate = newDate;
            forceDraw && this.getGraphics().drawLabels();
        }
    },

    /*
     * Returns the date object representing the death date of this Person
     */
    getDeathDate: function() {
        return this._deathDate;
    },

    /*
     * Replaces the death date with newDate and (optionally) updates the graphics
     *
     * @param newDate a javascript Date object, that must be a later date than deathDate and
     * a later date than conception date
     * @param forceDisplay set to true if you want to display the change on the canvas
     */
    setDeathDate: function(deathDate, forceDraw) {
        if(!deathDate || deathDate && !this.getBirthDate() || deathDate.getDate()>this.getBirthDate().getDate()) {
            this._deathDate =  deathDate;
            this._deathDate && (this.getLifeStatus() == 'alive') && this.setLifeStatus('deceased', forceDraw);
        }
        forceDraw && this.getGraphics().drawLabels();
    },

    /*
     * Returns an array of objects with fields 'id' and 'value', where id is the id number
     * for the disorder, taken from the OMIM database, and 'value' is the name of the disorder.
     * eg. [{id: 33244, value: 'Down Syndrome'}, {id: 13241, value: 'Huntington's Disease'}, ...]
     */
    getDisorders: function() {
        return this._disorders;
    },

    /*
     * Replaces the list of disorder IDs of this person with disorderArray
     *
     * @param disorderArray should be an array of objects with fields 'id' and 'value', where id is the id number
     * for the disorder, taken from the OMIM database, and 'value' is the name of the disorder.
     * eg. [{id: 33244, value: 'Down Syndrome'}, {id: 13241, value: 'Huntington's Disease'}, ...]
     */
    setDisorders: function(disorderArray) {
        this._disorders = disorderArray;
    },

    /*
     * Adds disorder to the list of this node's disorders and updates the Legend.
     * (Optionally) updates the graphics of this node
     *
     * @param disorder an object with fields 'id' and 'value', where id is the id number
     * for the disorder, taken from the OMIM database, and 'value' is the name of the disorder.
     * eg. {id: 33244, value: 'Down Syndrome'}
     * @param forceDisplay set to true if you want to display the change on the canvas
     */
    addDisorder: function(disorder, forceDisplay) {
        if(!this.hasDisorder(disorder['id'])) {
            editor.getLegend().addCase(disorder, this);
            this.getDisorders().push(disorder);
        }
        forceDisplay && this.getGraphics().drawShapes();
    },

    /*
     * Removes disorder to the list of this node's disorders and updates the Legend.
     * (Optionally) updates the graphics of this node
     *
     * @param disorder an object with fields 'id' and 'value', where id is the id number
     * for the disorder, taken from the OMIM database, and 'value' is the name of the disorder.
     * eg. {id: 33244, value: 'Down Syndrome'}
     * @param forceDisplay set to true if you want to display the change on the canvas
     */
    removeDisorder: function(disorder, forceDisplay) {
        if(this.getDisorders().indexOf(disorder) >= 0) {
            editor.getLegend().removeCase(disorder, this);
            this.setDisorders(this.getDisorders().without(disorder));
        }
        else {
            alert("This person doesn't have the specified disorder");
        }
        forceDisplay && this.getGraphics().drawShapes();
    },

    updateDisorders: function(disorders, forceDisplay) {
        var me = this;
        this.getDisorders().each(function(disorder) {
            var found = false;
            disorders.each(function(newDisorder) {
                disorder['id'] == newDisorder['id'] && (found = true);
            });
            !found && me.removeDisorder(disorder, false);
        });
        disorders.each(function(newDisorder) {
            if (!me.hasDisorder(newDisorder.id)) {
                me.addDisorder(newDisorder, false);
            }
        });
        forceDisplay && this.getGraphics().draw();
    },

    hasDisorder: function(id) {
        for(var i = 0; i < this.getDisorders().length; i++) {
            if(this.getDisorders()[i].id == id) {
                return true;
            }
        }
        return false;
    },

    /**
     * Changes the adoption status of this Person to isAdopted and (optionally) updates the
     * graphics of this node
     *
     * @param isAdopted set to true if you want to mark the Person adopted
     * @param forceDisplay set to true if you want to display the change on the canvas
     */
    setAdopted: function(isAdopted, forceDisplay) {
        //TODO: implement adopted and social parents

        if((this.getLifeStatus() == 'stillborn' || this.getLifeStatus() =='aborted')) {
            this._isAdopted = false;
        }
        else {
            this._isAdopted = isAdopted;
        }
        forceDisplay && this.getGraphics().drawShapes();
    },

    /**
     * Returns true if this Person is marked adopted
     */
    isAdopted: function() {
        return this._isAdopted;
    },

    /*
     * Breaks connections with all related nodes and removes this node and its properties
     * from the record.
     * (Optional) Removes all descendants of this node as well
     * (Optional) Removes all the graphics for this node and (optionally)
     * his descendants
     *
     * @param isRecursive set to true if you want to remove all descendants as well
     * @param removeVisuals set to true if you want to remove the graphics as well
     */
    remove: function($super, isRecursive, removeGraphics) {
        this.getDisorders().each(function(disorder) {
            editor.getLegend().removeCase(disorder, this);
        });
        $super(isRecursive, removeGraphics);
    },

    /*
     * Returns true if this node can be a parent of otherNode
     *
     * @param otherNode is a Person
     */
    canBeParentOf: function($super, otherNode) {
        var preliminary = $super(otherNode);
        var incompatibleBirthDate = this.getBirthDate() && otherNode.getBirthDate() && this.getBirthDate() < otherNode.getBirthDate();
        var incompatibleDeathDate = this.getDeathDate() && otherNode.getBirthDate() && this.getDeathDate() < otherNode.getBirthDate().clone().setDate(otherNode.getBirthDate().getDate()-700);

        return preliminary && !incompatibleBirthDate && !incompatibleDeathDate && !this.isFetus();

    },


    setParentPartnership: function($super, partnership) {
        $super(partnership);
        if(partnership) {
            this.getGraphics().getHoverBox().hideParentHandle();
        }
        else {
            this.getGraphics().getHoverBox().unHideParentHandle();
        }
    },

    /*
     * Returns an object (to be accepted by the menu) with information about this Person
     */
    getSummary: function() {
        return {
            identifier:    {value : this.getID()},
            first_name:    {value : this.getFirstName()},
            last_name:     {value : this.getLastName()},
            gender:        {value : this.getGender(), inactive: (this.getGender() != 'U' && this.getPartners().length > 0)},
            date_of_birth: {value : this.getBirthDate(), inactive: this.isFetus()},
            disorders:     {value : this.getDisorders()},
            adopted:       {value : this.isAdopted(), inactive: this.isFetus()},
            state:         {value : this.getLifeStatus(), inactive: [(this.getPartnerships().length > 0) ? ['unborn','aborted','stillborn'] : ''].flatten()},
            date_of_death: {value : this.getDeathDate(), inactive: this.isFetus()},
            gestation_age: {value : this.getGestationAge(), inactive : !this.isFetus()}
        };
    }
});

