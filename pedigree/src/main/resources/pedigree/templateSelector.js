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
            var href = getSelectorFromXML(objects[i], "link", "rel", "http://www.xwiki.org/rel/properties").getAttribute("href");
            new Ajax.Request(href, {
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
        pictureBox.innerHTML = getSubSelectorTextFromXML(response.responseXML, "property", "name", "data", "value").replace(/&amp;/, '&');
        var value = pictureBox.innerText || pictureBox.text || pictureBox.textContent; // damn IE ;)        
        var db_data = JSON.parse(value);
        // TODO
        // HACK until DB is updated: hardcoded templates
        if (db_data.partnerships.length == 0) {
            pictureBox.pedigreeData = '{"GG":[{"id":0,"prop":{"gender":"F","fName":"Proband"}}],"ranks":[1],"order":[[],[0]],"positions":[5]}';
            pictureBox.description  = "Proband only";
        }
        else {
            pictureBox.pedigreeData = '{"GG":[{"id":0,"prop":{"gender":"F","fName":"Proband"}},{"id":1,"chhub":true,"prop":{},"outedges":[{"to":0}]},{"id":2,"rel":true,"hub":true,"prop":{},"outedges":[{"to":1}]},{"id":3,"prop":{"gender":"F","fName":""},"outedges":[{"to":2}]},{"id":4,"prop":{"gender":"M","fName":""},"outedges":[{"to":2}]},{"id":5,"chhub":true,"prop":{},"outedges":[{"to":3}]},{"id":6,"rel":true,"hub":true,"prop":{},"outedges":[{"to":5}]},{"id":7,"prop":{"gender":"M"},"outedges":[{"to":6}]},{"id":8,"prop":{"gender":"F"},"outedges":[{"to":6}]},{"id":9,"chhub":true,"prop":{},"outedges":[{"to":4}]},{"id":10,"rel":true,"hub":true,"prop":{},"outedges":[{"to":9}]},{"id":11,"prop":{"gender":"M"},"outedges":[{"to":10}]},{"id":12,"prop":{"gender":"F"},"outedges":[{"to":10}]},{"id":13,"chhub":true,"prop":{},"outedges":[{"to":8}]},{"id":14,"rel":true,"hub":true,"prop":{},"outedges":[{"to":13}]},{"id":15,"prop":{"gender":"M"},"outedges":[{"to":14}]},{"id":16,"prop":{"gender":"F"},"outedges":[{"to":14}]},{"id":17,"chhub":true,"prop":{},"outedges":[{"to":11}]},{"id":18,"rel":true,"hub":true,"prop":{},"outedges":[{"to":17}]},{"id":19,"prop":{"gender":"M"},"outedges":[{"to":18}]},{"id":20,"prop":{"gender":"F"},"outedges":[{"to":18}]},{"id":21,"chhub":true,"prop":{},"outedges":[{"to":12}]},{"id":22,"rel":true,"hub":true,"prop":{},"outedges":[{"to":21}]},{"id":23,"prop":{"gender":"M"},"outedges":[{"to":22}]},{"id":24,"prop":{"gender":"F"},"outedges":[{"to":22}]},{"id":25,"chhub":true,"prop":{},"outedges":[{"to":7}]},{"id":26,"rel":true,"hub":true,"prop":{},"outedges":[{"to":25}]},{"id":27,"prop":{"gender":"M"},"outedges":[{"to":26}]},{"id":28,"prop":{"gender":"F"},"outedges":[{"to":26}]}],"ranks":[7,6,5,5,5,4,3,3,3,4,3,3,3,2,1,1,1,2,1,1,1,2,1,1,1,2,1,1,1],"order":[[],[15,14,16,27,26,28,23,22,24,19,18,20],[13,25,21,17],[8,6,7,12,10,11],[5,9],[3,2,4],[1],[0]],"positions":[83,83,83,40,126,40,40,61,17,126,126,149,105,17,17,5,29,149,149,137,161,105,105,93,117,61,61,49,73]}';
            pictureBox.description  = "Family with 3 generations";
        }
        
        //console.log("[Data from Template] - " + stringifyObject(pictureBox.pedigreeData));
        
        // TODO: render images with JavaScript instead
        if (window.SVGSVGElement &&
            document.implementation.hasFeature("http://www.w3.org/TR/SVG11/feature#Image", "1.1")) {
            pictureBox.update(getSubSelectorTextFromXML(response.responseXML, "property", "name", "image", "value"));
        } else {
            pictureBox.innerHTML = "<table bgcolor='#FFFAFA'><tr><td><br>&nbsp;" + pictureBox.description + "&nbsp;<br><br></td></tr></table>";
        }
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
        console.log("observe onTemplateSelected");
        this.dialog.close();
        editor.getSaveLoadEngine().createGraphFromSerializedData(pictureBox.pedigreeData, false /* add to undo stack */, true /*center around 0*/);
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
