/**
 * Class description
 *
 * @class ClassName
 * @constructor
 * @param x {Number} the x coordinate on the canvas
 * @param y {Number} the y coordinate on the canvas
 */

var TemplateSelector = Class.create( {

    initialize: function(isStartupTemplateSelector) {
        this._isStartupTemplateSelector = isStartupTemplateSelector;
        this.mainDiv = new Element('div', {'class': 'template-picture-container'});
        this.mainDiv.update("Loading list of templates...");
        var closeShortcut = isStartupTemplateSelector ? [] : ['Esc'];
        this.dialog = new MS.widgets.ModalPopup(this.mainDiv, {close: {method : this.closeDialog.bind(this), keys : closeShortcut}}, {extraClassName: "pedigree-template-chooser", title: "Please select a pedigree template", displayCloseButton: !isStartupTemplateSelector});
        isStartupTemplateSelector && this.dialog.show();
        new Ajax.Request(new XWiki.Document('WebHome').getRestURL('objects/PhenoTips.PedigreeClass/'), {
            method: 'GET',
            onSuccess: this.onTemplateListAvailable.bind(this)
        });
    },

    isStartupTemplateSelector: function() {
        return this._isStartupTemplateSelector;
    },

    onTemplateListAvailable: function(response) {
        this.mainDiv.update();
        var objects = response.responseXML.documentElement.getElementsByTagName('objectSummary');
        for (var i = 0; i < objects.length; ++i) {
            var pictureBox = new Element('div', {'class': 'picture-box'});
            pictureBox.update("Loading...");
            this.mainDiv.insert(pictureBox);
            new Ajax.Request(objects[i].querySelector("link[rel='http://www.xwiki.org/rel/properties']").getAttribute("href"), {
                method: 'GET',
                onSuccess: this.onTemplateAvailable.bind(this, pictureBox)
            });
        }
    },

    closeDialog: function() {
        this.dialog.closeDialog();
    },

    onTemplateAvailable: function(pictureBox, response) {
        pictureBox.innerHTML = response.responseXML.documentElement.querySelector("property[name='data'] > value").textContent.replace(/&amp;/, '&');
        pictureBox.pedigreeData = JSON.parse(pictureBox.textContent);
        pictureBox.innerHTML = response.responseXML.documentElement.querySelector("property[name='image'] > value").textContent.replace(/&amp;/, '&');
        pictureBox.innerHTML = pictureBox.textContent;
        pictureBox.observe('click', this.onTemplateSelected.bindAsEventListener(this, pictureBox));
    },

    onTemplateSelected: function(event, pictureBox) {
        this.dialog.close();
        if(this.isStartupTemplateSelector()) {
            editor.getSaveLoadEngine().load(pictureBox.pedigreeData);
        }
        else {
            editor.getSaveLoadEngine().loadTemplateAction(pictureBox.pedigreeData);
        }
    },

    show: function() {
        this.dialog.show();
    },

    hide: function() {
        this.dialog.close();
    }
});