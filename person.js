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
 * @param isProband set to true if this person is the proband
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
        this._childlessStatus = null;
        this._childlessReason = null;
        this._disorders = [];
        this._evaluations = [];
        $super(x, y, gender, id);
        this._type = "Person"
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
     * Returns true if this node is the proband (i.e. the main patient)
     */
    isProband: function() {
        return this._isProband;
    },

    /*
     * Adds a new partnership to the list of partnerships of this node
     *
     * @param partnership is a Partnership object with this node as one of the partners
     */
    addPartnership: function($super, partnership) {
        this.getGraphics().getHoverBox().hideChildHandle();
        $super(partnership);
    },

    /*
     * Removes a partnership from the list of partnerships
     *
     * @param partnership is a Partnership object with this node as one of the partners
     */
    removePartnership: function($super, partnership) {
        this.getGraphics().getHoverBox().unhideChildHandle();
        $super(partnership);
    },

    /*
     * Replaces the parent Pregnancy
     *
     * @param pregnancy is a Pregnancy object
     */
    setParentPregnancy: function($super, pregnancy) {
        $super(pregnancy);
        if(pregnancy) {
            this.getGraphics().getHoverBox().hideParentHandle();
        }
        else {
            this.getGraphics().getHoverBox().unHideParentHandle();
        }
    },

    /*
     * Returns the first name of this Person
     */
    getFirstName: function() {
        return this._firstName;
    },

    /*
     * Replaces the first name of this Person with firstName, and displays the label
     *
     * @param firstName any string that represents the first name of this Person
     */
    setFirstName: function(firstName) {
        firstName && (firstName = firstName.charAt(0).toUpperCase() + firstName.slice(1));
        this._firstName = firstName;
        this.getGraphics().updateNameLabel();
    },

    /*
     * Returns the last name of this Person
     */
    getLastName: function() {
        return this._lastName;
    },

    /*
     * Replaces the last name of this Person with lastName, and displays the label
     *
     * @param lastName any string that represents the last name of this Person
     */
    setLastName: function(lastName) {
        lastName && (lastName = lastName.charAt(0).toUpperCase() + lastName.slice(1));
        this._lastName = lastName;
        this.getGraphics().updateNameLabel();
    },
    /*
     * Returns the status of this Person, which can be "alive", "deceased", "stillborn", "unborn" or "aborted"
     */
    getLifeStatus: function() {
        return this._lifeStatus;
    },

    /*
     * Returns true if this node's status is not 'alive' or 'deceased'.
     */
    isFetus: function() {
        return (this.getLifeStatus() != 'alive' && this.getLifeStatus() != 'deceased');
    },

    /*
     * Changes the life status of this Person to newStatus
     *
     * @param newStatus can be "alive", "deceased", "stillborn", "unborn" or "aborted"
     */
    setLifeStatus: function(newStatus) {
        if(newStatus == 'unborn' || newStatus == 'stillborn' || newStatus == 'aborted' || newStatus == 'alive' || newStatus == 'deceased') {
            this._lifeStatus = newStatus;

            (newStatus != 'deceased') && this.setDeathDate(null);
            this.getGraphics().updateSBLabel();

            if(this.isFetus()) {
                this.setBirthDate(null);
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

    /*
     * Returns the date object for the conception date of this Person
     */
    getConceptionDate: function() {
        return this._conceptionDate;
    },

    /*
     * Replaces the conception date with newDate
     *
     * @param newDate a javascript Date object
     */
    setConceptionDate: function(newDate) {
        this._conceptionDate = newDate;
        this.getGraphics().updateAgeLabel();
    },

    /*
     * Returns the number of weeks since conception
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

    /*
     * Updates the conception age of the Person given the number of weeks passed since conception,
     *
     * @param numWeeks a number greater than or equal to 0
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

    /*
     * Returns the date object for the birth date of this Person
     */
    getBirthDate: function() {
        return this._birthDate;
    },

    /*
     * Replaces the birth date with newDate
     *
     * @param newDate a javascript Date object, that must be an earlier date than deathDate and
     * a later date than conception date
     */
    setBirthDate: function(newDate) {
        if (!newDate || newDate && !this.getDeathDate() || newDate.getDate() < this.getDeathDate()) {
            this._birthDate = newDate;
            this.getGraphics().updateAgeLabel();
        }
    },

    /*
     * Returns the date object for the death date of this Person
     */
    getDeathDate: function() {
        return this._deathDate;
    },

    /*
     * Replaces the death date with newDate
     *
     * @param newDate a javascript Date object, that must be a later date than deathDate and
     * a later date than conception date
     */
    setDeathDate: function(deathDate) {
        if(!deathDate || deathDate && !this.getBirthDate() || deathDate.getDate()>this.getBirthDate().getDate()) {
            this._deathDate =  deathDate;
            this._deathDate && (this.getLifeStatus() == 'alive') && this.setLifeStatus('deceased');
        }
        this.getGraphics().updateAgeLabel();
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
        forceDisplay && this.getGraphics().updateDisorderShapes();
    },

    /*
     * Removes disorder to the list of this node's disorders and updates the Legend.
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
        forceDisplay && this.getGraphics().updateDisorderShapes();
    },

    /*
     * Given a list of disorders, adds and removes the disorders of this node to match
     * the new list
     *
     * @param disorderArray should be an array of objects with fields 'id' and 'value', where id is the id number
     * for the disorder, taken from the OMIM database, and 'value' is the name of the disorder.
     * eg. [{id: 33244, value: 'Down Syndrome'}, {id: 13241, value: 'Huntington's Disease'}, ...]
     */
    updateDisorders: function(disorders) {
        var me = this;
        this.getDisorders().each(function(disorder) {
            var found = false;
            disorders.each(function(newDisorder) {
                disorder['id'] == newDisorder['id'] && (found = true);
            });
            !found && me.removeDisorder(disorder);
        });
        disorders.each(function(newDisorder) {
            if (!me.hasDisorder(newDisorder.id)) {
                me.addDisorder(newDisorder);
            }
        });
        this.getGraphics().updateDisorderShapes();
    },

    /*
     * Returns true if this person has the disorder with id
     *
     * @param id a string id for the disorder, taken from the OMIM database
     */
    hasDisorder: function(id) {
        for(var i = 0; i < this.getDisorders().length; i++) {
            if(this.getDisorders()[i].id == id) {
                return true;
            }
        }
        return false;
    },

    /**
     * Changes the adoption status of this Person to isAdopted
     *
     * @param isAdopted set to true if you want to mark the Person adopted
     */
    setAdopted: function(isAdopted) {
        //TODO: implement adopted and social parents
        if(isAdopted) {
            this.getGraphics().drawAdoptedShape();
        }
        else {
            this.getGraphics().removeAdoptedShape();
        }
    },

    /*
     * Returns true if this Person is marked adopted
     */
    isAdopted: function() {
        return this._isAdopted;
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

    /*
     * Replaces this Person with a placeholder without breaking all the connections.
     *
     * @param otherNode is a Person
     */
    convertToPlaceholder: function() {
        var me = this;
        var gender = (this.getPartnerships().length == 0) ? "U" : this.getGender();
        var placeholder = editor.getGraph().addPlaceHolder(this.getX(), this.getY(), gender);
        var parents = this.getUpperNeighbors()[0];
        if(parents) {
            parents.addChild(placeholder);
            parents.removeChild(me);
        }
        this.getPartnerships().each(function(partnership) {
            var newPartnership = editor.getGraph().addPartnership(partnership.getX(), partnership.getY(), partnership.getPartnerOf(me), placeholder);
            partnership.getChildren().each(function(child) {
                partnership.removeChild(child);
                newPartnership.addChild(child);
            });
        });
        me.remove(false);
        return placeholder;
    },

    /*
     * Deletes this node, it's placeholder partners and children and optionally
     * removes all the other nodes that are unrelated to the proband node.
     *
     * @param isRecursive set to true if you want to remove related nodes that are
     * not connected to the proband
     */
    remove: function($super, isRecursive) {
        editor.getGraph().removePerson(this);
        if(!isRecursive) {
            this.getPartners().each(function(partner) {
                if(partner.getType() == 'PlaceHolder') {
                    partner.remove(false);
                }
            });
            var parents = this.getParentPartnership();
            var singleChild = parents && parents.getChildren().length == 1;
            var hasChildren = this.getChildren("Person").concat(this.getChildren("PersonGroup")).length != 0;
            if(singleChild || hasChildren) {
                this.convertToPlaceholder();
            }
            else {
                this.getDisorders().each(function(disorder) {
                    editor.getLegend().removeCase(disorder, this);
                });
                this.getGraphics().getHoverBox().remove();
                $super(isRecursive);
            }
        }
        else {
            $super(isRecursive);
        }
    },

    /*
     * Adds a placeholder child to all partnerships that are missing it.
     */
    restorePlaceholders: function() {
        var me = this;
        this.getPartnerships().each(function(partnership) {
            if(!me.getChildlessStatus() && !partnership.getChildlessStatus() &&
                !partnership.getPartnerOf(me).getChildlessStatus() &&
                partnership.getChildren().length == 0) {
                partnership.createChild('PlaceHolder', 'U')
            }
        });
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
            date_of_death: {value : this.getDeathDate(), inactive: this.getLifeStatus() != 'deceased'},
            gestation_age: {value : this.getGestationAge(), inactive : !this.isFetus()},
            childlessSelect : {value : this.getChildlessStatus() ? this.getChildlessStatus() : 'none', inactive : this.isFetus()},
            childlessText : {value : this.getChildlessReason() ? this.getChildlessReason() : 'none', inactive : this.isFetus()}
        };
    },

    getInfo: function($super) {
        var info = $super();
        info['fName'] = this.getFirstName();
        info['lName'] = this.getLastName();
        info['dob'] = this.getBirthDate();
        info['disorders'] = this.getDisorders();
        info['isAdopted'] = this.isAdopted();
        info['lifeStatus'] = this.getLifeStatus();
        info['dod'] = this.getDeathDate();
        info['gestationAge'] = this.getGestationAge();
        info['childlessStatus'] = this.getChildlessStatus();
        info['childlessReason'] = this.getChildlessReason();
        return info;
    }
});

Person.addMethods(ChildlessBehavior);