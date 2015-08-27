define([], function(){
    var ChildlessBehavior = {
        /**
         * Returns the childless status of this node
         *
         * @method getChildlessStatus
         * @return {Null|String} null, childless or infertile
         */
        getChildlessStatus: function() {
            return this._childlessStatus;
        },

        /**
         * Returns true if the status is either 'infertile' or 'childless'
         *
         * @method isValidChildlessStatus
         * @return {Boolean}
         */
        isValidChildlessStatus: function(status) {
            return ((status == 'infertile' || status == 'childless'));
        },

        /**
         * Returns the reason for this node's status of 'infertile' or 'childless'
         *
         * @method getChildlessReason
         * @return {String}
         */
        getChildlessReason: function() {
            return this._childlessReason;
        },

        /**
         * Changes the reason for this node's 'childless' or 'infertile' status
         *
         * @method setChildlessReason
         * @param {String} reason Explanation for the condition (eg. "By Choice", "Vasectomy" etc)
         */
        setChildlessReason: function(reason) {
            if(this.getChildlessStatus() == null)
                reson = "";        
            this._childlessReason = reason;
            this.getGraphics().updateChildlessStatusLabel();
        }
    };
    return ChildlessBehavior;
});