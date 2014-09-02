/**
 * The UI Element for displaying prompts to the user in a movable/semi-transparent dialogue
 *
 * @class OkCancelDialogue
 */

var OkCancelDialogue = Class.create( {

    initialize: function() {        
        var _this = this;
        
        this._onOK     = undefined;
        this._onCancel = undefined;
        
        var mainDiv = new Element('div', {'class': 'ok-cancel-dialogue'});

        this._promptBody = new Element('div', {'class': 'ok-cancel-body'});
        mainDiv.insert(this._promptBody);
            
        var buttons = new Element('div', {'class' : 'buttons import-block-bottom'});
        buttons.insert(new Element('input', {type: 'button', name : 'ok',     'value': 'OK', 'class' : 'button width80px', 'id': 'OK_button'}).wrap('span', {'class' : 'buttonwrapper'}));
        buttons.insert(new Element('input', {type: 'button', name : 'cancel', 'value': 'Cancel', 'class' : 'button secondary width80px'}).wrap('span', {'class' : 'buttonwrapper'}));
        mainDiv.insert(buttons);

        var cancelButton = buttons.down('input[name="cancel"]');
        cancelButton.observe('click', function(event) {
            _this.hide();
            _this._onCancel && _this._onCancel();
        })
        var okButton = buttons.down('input[name="ok"]');
        okButton.observe('click', function(event) {
            _this.hide();
            _this._onOK && _this._onOK();
        })

        var closeShortcut = ['Esc'];
        this.dialog = new PhenoTips.widgets.ModalPopup(mainDiv, {close: {method : this.hide.bind(this), keys : closeShortcut}}, {extraClassName: "pedigree-import-chooser", title: "?", displayCloseButton: false});
    },

    /**
     * Displays the template selector
     *
     * @method show
     */
    show: function(message, title, onOKFunction, onCancelFunction) {
        this._onOK     = onOKFunction;
        this._onCancel = onCancelFunction;
        this._promptBody.update(message);
        this.dialog.show();
        this.dialog.dialogBox.down("div.msdialog-title").update(title);  // this.dialog.dialogBox is available only after show() 
    },

    /**
     * Removes the the template selector
     *
     * @method hide
     */
    hide: function() {
        this.dialog.closeDialog();
    }
});