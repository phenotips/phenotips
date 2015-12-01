/**
 * Base class for selectors which ar eplace din tabbed dialogue
 */
define([], function(){
    var TabbedSelectorTab = Class.create( {

        /**
         * Returns an Element (presumably a <div>) which has tab content.
         */
        getContentDiv: function() {
            throw "Not implemented";
        },

        /**
         * Returns tab title.
         */
        getTitle: function() {
        },

        /**
         * Is called prior to dialogue coming on screen.
         */
        onShow: function(allowCancel) {
        },

        /**
         * Is called when the dialogue is closed.
         */
        onHide: function() {
        },

        /**
         * Called whenever the tab is selected after being inactive
         */
        onActivatedTab: function() {
        },

        /**
         * A way to let tabs know what is the parent DIV and which method to call
         * to close the entire tabed dialogue.
         */
        setParent: function(dialogID, closeFunc) {
            this.dialogID  = dialogID;
            this.closeFunc = closeFunc;
        },

        /**
         * Closes entire tabbed dialogue.
         */
        close: function() {
            this.closeFunc && this.closeFunc();
        },

        getParentDiv: function() {
            return $('' + this.dialogID + '');
        }
    });

    return TabbedSelectorTab;
});