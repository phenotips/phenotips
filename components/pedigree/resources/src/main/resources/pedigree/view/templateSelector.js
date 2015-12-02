/**
 * The UI Element for browsing and selecting pre-defined Pedigree templates
 *
 * @class TemplateSelector
 */
define([
        "pedigree/model/helpers"
    ], function(
        Helpers
    ){
    var TemplateSelector = Class.create( {

        initialize: function() {
            this.mainDiv = new Element('div', {'class': 'template-picture-container'});
            this.mainDiv.update("Loading list of templates...");
            new Ajax.Request(new XWiki.Document('WebHome').getRestURL('objects/PhenoTips.PedigreeClass/'), {
                method: 'GET',
                onSuccess: this._onTemplateListAvailable.bind(this)
            });
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
                // Use only the path, since the REST module returns the wrong host behind a reverse proxy
                var path = href.substring(href.indexOf("/", href.indexOf("//") + 2));
                new Ajax.Request(path, {
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
            pictureBox.innerHTML = getSubSelectorTextFromXML(response.responseXML, "property", "name", "image", "value").replace(/&amp;/, '&');
            pictureBox.pedigreeData = getSubSelectorTextFromXML(response.responseXML, "property", "name", "data", "value");
            pictureBox.type         = 'internal';
            pictureBox.description  = getSubSelectorTextFromXML(response.responseXML, "property", "name", "description", "value");
            pictureBox.title        = pictureBox.description;

            //console.log("[Data from Template] - " + Helpers.stringifyObject(pictureBox.pedigreeData));

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
            $$('.pedigree-import-chooser .msdialog-close')[0].click();
            if (pictureBox.type == 'internal') {
                editor.getSaveLoadEngine().createGraphFromSerializedData(pictureBox.pedigreeData, false /* add to undo stack */, true /*center around 0*/);
            } else if (pictureBox.type == 'simpleJSON') {
                editor.getSaveLoadEngine().createGraphFromImportData(pictureBox.pedigreeData, 'simpleJSON', {}, false /* add to undo stack */, true /*center around 0*/);
            }
        },

    });


    // TODO: replace XML loading with a service providing JSONs

    function unescapeRestData (data) {
        // http://stackoverflow.com/questions/4480757/how-do-i-unescape-html-entities-in-js-change-lt-to
        var tempNode = document.createElement('div');
        tempNode.innerHTML = data.replace(/&amp;/, '&');
        return tempNode.innerText || tempNode.text || tempNode.textContent;
    }

    function getSelectorFromXML(responseXML, selectorName, attributeName, attributeValue) {
        if (responseXML.querySelector) {
            // modern browsers
            return responseXML.querySelector(selectorName + "[" + attributeName + "='" + attributeValue + "']");
        } else {
            // IE7 && IE8 && some other older browsers
            // http://www.w3schools.com/XPath/xpath_syntax.asp
            // http://msdn.microsoft.com/en-us/library/ms757846%28v=vs.85%29.aspx
            var query = ".//" + selectorName + "[@" + attributeName + "='" + attributeValue + "']";
            try {
                return responseXML.selectSingleNode(query);
            } catch (e) {
                // Firefox v3.0-
                alert("your browser is unsupported");
                window.stop && window.stop();
                throw "Unsupported browser";
            }
        }
    }

    function getSubSelectorTextFromXML(responseXML, selectorName, attributeName, attributeValue, subselectorName) {
        var selector = getSelectorFromXML(responseXML, selectorName, attributeName, attributeValue);

        var value = selector.innerText || selector.text || selector.textContent;

        if (!value)     // fix IE behavior where (undefined || "" || undefined) == undefined
            value = "";

        return value;
    }

    return TemplateSelector;
});