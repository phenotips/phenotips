    /**
     * A Pedigree node on the canvas
     */

    var PedigreeNode = Class.create( {

        /**
         * Constructor for PedigreeNode
         * @param xPosition the x coordinate for the object
         * @param yPosition the y coordinate for the object
         * @param graphics a NodeVisuals object
         * @param gender (optional), the gender of the node
         */
        initialize: function(xPosition, yPosition, graphics, gender) {

            this._graphics = graphics;
            this._partners = [];
            this._isDead = false;
            this._adopted = false;
            this._disorders = [];
            this._xPos = xPosition;
            this._yPos = yPosition;
            this._hoverBox = new Hoverbox(this);
            this._gender = (gender == 'male' || gender == 'female')?gender:'unknown';
            this.setGender(this._gender);
        },

        /**
         * Replaces the gender of the PedigreeNode and displays the
         * appropriate shape on the graph
         * @param: gender can be either 'male', 'female' or 'unknown'.
         */
        setGender: function(gender) {

            if (gender == 'male' || gender == 'female' || gender == 'unknown')
            {
                this._gender = gender;
                this._graphics.setGender(this, gender);
            }
        },

        /**
         * Sets the isDead property of the PedigreeNode to the value
         * passed in the isDead parameter
         * @param isDead boolean value indicating whether the PedigreeNode is dead or not
         */
        setDead: function(isDead) {
            this._isDead = isDead;
            this._graphics.setDead(this, isDead);
        },

        /**
         * Replaces the age property of the PedigreeNode and
         * displays the appropriate label on the graph
         * @param: birth Year is a string in the format '56 y', '4 mo' or 'b. 1988'
         */
        setAge: function(birthDay, birthMonth, birthYear) {
            var birthDate = new Date(birthYear, birthMonth-1, birthDay);
            this._age = getAge(birthDate);
            this._graphics.setAge(this);
        },

        /**
         * Assigns disorderName to this pedigreeNode and displays it in the right
         * proportion with the other Disorders. Uses the color/pattern listed in
         * _disorderMap
         * @param: DisorderName a string representing the name of the Disorder.
         */
        addDisorder: function(disorderName) {
            if(this._disorders.indexOf(disorderName) < 0)
            {
                this._disorders.push(disorderName);
                this._graphics.addDisorder(this, disorderName);
                disorderMap[disorderName].numAffectedNodes++;
            }
            else{
                alert("This disorder was already registered");
            }
        },

        /**
         * Removes disorderName from this pedigreeNode and updates the node graphics.
         * updates the NumAffectedNodes value in _disorderMap
         * @param: DisorderName a string representing the name of the Disorder.
         */
        removeDisorder: function(disorderName) {
            if(this._disorders.indexOf(disorderName) >= 0)
            {
                this._disorders = this._disorders.without(disorderName);
                disorderMap[disorderName].numAffectedNodes--;
                this._graphics.removeDisorder(this)
            }
            else
            {
                alert("This person doesn't have the specified disorder");
            }
        },

        /**
         * Changes the '_adopted' property of pedigreeNode, and displays
         * the graphical representation
         */
        setAdopted: function(isAdopted) {
            this._adopted = isAdopted;
            this._graphics.setAdopted(this, isAdopted);
        },

        /**
         * Removes the properties and graphical elements of the PedigreeNode
         */
        remove: function() {
            this._graphics.remove(this);
            this._disorders.each(function(s) {
                disorderMap[s].numAffectedNodes--;
            });
        }
    });

