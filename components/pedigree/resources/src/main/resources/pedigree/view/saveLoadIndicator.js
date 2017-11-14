/**
 * SaveLoadIndicator is a window that notifies the user of a blocking operation, such as load, save or patient deletion.
 *
 * @class SaveLoadIndicator
 * @constructor
 */
define([], function(){
    var SaveLoadIndicator = Class.create({

        initialize: function() {
            var me = this;
            var mainDiv = new Element('div', {'class': 'busy-indicator-content'});

            this.textDiv = new Element('div', {'class': 'busy-indicator-text'});
            this.progressBarDiv = new Element('div', {'class': 'busy-progressbar-container loading-indicator'});

            mainDiv.insert(this.textDiv).insert(this.progressBarDiv);

            this._isHidden = true;
            this.dialog = new PhenoTips.widgets.ModalPopup(mainDiv, {'close': {'method': null, 'keys': []} }, {displayCloseButton: false});
            document.observe("pedigree:load:start", function(event) {
                if(me._isHidden) {
                    var message = event.memo.hasOwnProperty("message") ? event.memo.message : null;
                    me.show(message);
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
        show: function(message) {
            if (message) {
                this.textDiv.update(message);
                this.textDiv.show();
            } else {
                this.textDiv.hide();
            }
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
    return SaveLoadIndicator;
});