/**
 * The UI Element for browsing and selecting pre-defined Pedigree templates
 *
 * @class TemplateSelector
 * @constructor
 * @param {Boolean} isStartupTemplateSelector Set to True if no pedigree has been loaded yet
 */

var TemplateSelector = Class.create( {

    initialize: function(isStartupTemplateSelector) {
        this._isStartupTemplateSelector = isStartupTemplateSelector;
        this.mainDiv = new Element('div', {'class': 'template-picture-container'});
        this.mainDiv.update("Loading list of templates...");
        var closeShortcut = isStartupTemplateSelector ? [] : ['Esc'];
        this.dialog = new MS.widgets.ModalPopup(this.mainDiv, {close: {method : this.hide.bind(this), keys : closeShortcut}}, {extraClassName: "pedigree-template-chooser", title: "Please select a pedigree template", displayCloseButton: !isStartupTemplateSelector});
        isStartupTemplateSelector && this.dialog.show();
        new Ajax.Request(new XWiki.Document('WebHome').getRestURL('objects/PhenoTips.PedigreeClass/'), {
            method: 'GET',
            onSuccess: this._onTemplateListAvailable.bind(this)
        });
    },

    /**
     * Returns True if this template selector is the one displayed on startup
     *
     * @method isStartupTemplateSelector
     * @return {Boolean}
     */
    isStartupTemplateSelector: function() {
        return this._isStartupTemplateSelector;
    },

    /**
     * Displays the templates once they have been downloaded
     *
     * @param response
     * @private
     */
    _onTemplateListAvailable: function(response) {
        this.mainDiv.update();
        var objects = response.responseXML.documentElement.getElementsByTagName('objectSummary');
        for (var i = 0; i < objects.length; ++i) {
            var pictureBox = new Element('div', {'class': 'picture-box'});
            pictureBox.update("Loading...");
            this.mainDiv.insert(pictureBox);
            new Ajax.Request(objects[i].querySelector("link[rel='http://www.xwiki.org/rel/properties']").getAttribute("href"), {
                method: 'GET',
                onSuccess: this._onTemplateAvailable.bind(this, pictureBox)
            });
        }
    },

    /**
     * Creates a clickable template thumbnail once the information has been downloaded
     *
     * @param pictureBox
     * @param response
     * @private
     */
    _onTemplateAvailable: function(pictureBox, response) {
        pictureBox.innerHTML = response.responseXML.documentElement.querySelector("property[name='data'] > value").textContent.replace(/&amp;/, '&');
        pictureBox.pedigreeData = JSON.parse(pictureBox.textContent);
        pictureBox.innerHTML = response.responseXML.documentElement.querySelector("property[name='image'] > value").textContent.replace(/&amp;/, '&');
        pictureBox.innerHTML = pictureBox.textContent;
        pictureBox.observe('click', this._onTemplateSelected.bindAsEventListener(this, pictureBox));
    },

    /**
     * Loads the template once it has been selected
     *
     * @param event
     * @param pictureBox
     * @private
     */
    _onTemplateSelected: function(event, pictureBox) {
        this.dialog.close();
        if(this.isStartupTemplateSelector()) {
            editor.getSaveLoadEngine().load(pictureBox.pedigreeData);
        }
        else {
            editor.getSaveLoadEngine().loadTemplateAction(pictureBox.pedigreeData);
        }
    },

    /**
     * Displays the template selector
     *
     * @method show
     */
    show: function() {
        this.dialog.show();
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