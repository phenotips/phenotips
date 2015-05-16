/**
 * Some pedigrees that belong to a family may contain sensitive information, and should display a warning message to the user.
 *
 * @class Warning
 * @constructor
 */

var Warning = Class.create( {

    initialize: function() {
        if(!editor.hasWarningMessage()) return;

        this.mainDiv = new Element('div', {'class': 'pedigree-warning-container'});
        this.mainDiv.update(editor.getWarningMessage());
        var closeShortcut = ['Esc'];

        this.dialog = new PhenoTips.widgets.ModalPopup(this.mainDiv,
            {close: {method : this.hide.bind(this), keys : closeShortcut}},
            {extraClassName: "pedigree-warning-message", title: "Please be advised",
                displayCloseButton: true, verticalPosition: "top"});
        this.dialog.show();
    },

    /**
     * Displays the warning
     *
     * @method show
     */
    show: function() {
        this.dialog.show();
    },

    /**
     * Removes the warning
     *
     * @method hide
     */
    hide: function() {
        this.dialog.closeDialog();
    }
});