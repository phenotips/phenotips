/**
 * SaveLoadIndicator is a window that notifies the user of loading and saving progress.
 *
 * @class SaveLoadIndicator
 * @constructor
 */

 var SaveLoadIndicator = Class.create({

    initialize: function() {
        var me = this;
        var mainDiv = new Element('div', {'class': 'load-status-container'});
        this._isHidden = true;
        this.dialog = new PhenoTips.widgets.ModalPopup(mainDiv, false, {extraClassName: "loading-indicator", displayCloseButton: false});
        document.observe("pedigree:load:start", function(event) {
            if(me._isHidden) {
                me.show();
            }
        });
        document.observe("pedigree:load:finish", function(event) {
            if(!me._isHidden) {
                me.hide();
            }
        });
    },

    /**
     * Displays the the loading window
     * @method show
     */
    show: function() {
        this.dialog.show();
        this._isHidden = false;
    },

    /**
     * Hides the the loading window
     * @method hide
     */
    hide: function() {
        this.dialog.close();
        this._isHidden = true;
    }
});