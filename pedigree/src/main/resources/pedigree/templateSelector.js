/**
 * Class description
 *
 * @class ClassName
 * @constructor
 * @param x {Number} the x coordinate on the canvas
 * @param y {Number} the y coordinate on the canvas
 */

var TemplateSelector = Class.create( {

    initialize: function() {
        this.mainDiv = new Element('div', {'class': 'template-picture-container'});
        this.mainDiv.update("Loading list of templates...");
        this.dialog = new MS.widgets.ModalPopup(this.mainDiv, false, {extraClassName: "pedigree-template-chooser", title: "Please select a pedigree template", displayCloseButton: false});
        this.dialog.show();
        new Ajax.Request(new XWiki.Document('WebHome').getRestURL('objects/ClinicalInformationCode.PedigreeClass/'), {
            method: 'GET',
            onSuccess: this.onTemplateListAvailable.bind(this)
        });
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

    onTemplateAvailable: function(pictureBox, response) {
        pictureBox.innerHTML = response.responseXML.documentElement.querySelector("property[name='data'] > value").textContent.replace(/&amp;/, '&');
        pictureBox.pedigreeData = JSON.parse(pictureBox.textContent);
        pictureBox.innerHTML = response.responseXML.documentElement.querySelector("property[name='image'] > value").textContent.replace(/&amp;/, '&');
        pictureBox.innerHTML = pictureBox.textContent;
        pictureBox.observe('click', this.onTemplateSelected.bindAsEventListener(this, pictureBox));
    },

    onTemplateSelected: function(event, pictureBox) {
        this.dialog.close();
        editor.getSaveLoadEngine().load(pictureBox.pedigreeData);
    }
});