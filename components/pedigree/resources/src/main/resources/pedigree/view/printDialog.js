/**
 * The UI Element for selecting pedigree print settings.
 *
 * Should be instantiated after preferences have been loaded as default paper/printer
 * settings may be stored in the preferences.
 *
 * @class PrintDialog
 */
var PrintDialog = Class.create( {

    initialize: function() {
        var _this = this;

        this._defaultScale = 0.4;
        this._zoomLevel = 100;
        this._printPageSet = {};
        this._printEngine = new PrintEngine();

        var mainDiv = new Element('div', {'class': 'import-selector field-no-user-select cursor-normal'});

        var previewHeader = new Element('div', {'class': 'print-preview-header'}).update("Print preview:<br>(each block indicates a separate page; click on a page to include/exclude th epage from print job)");
        this.previewContainer = new Element("div", {"id": "preview", "class": "preview-container"});
        var previewFooter = new Element('div', {'class': 'print-preview-footer'}).update("(Note: in some browsers you need to manually select landscape print mode)");
        mainDiv.insert(previewHeader).insert(this.previewContainer).insert(previewFooter);

        var minusButton = new Element('input', {"type" : "button", "value": "-", "class": "print-zoom-button print-zoom-minus-button"});
        var plusButton  = new Element('input', {"type" : "button", "value": "+", "class": "print-zoom-button"});
        this.zoomValue  = new Element('label', {"class": "print-zoom-value no-mouse-interaction"}).update("100%");
        var zoomDiv = new Element('div', {'class': 'pedigree-print-zoom-container field-no-user-select'});
        zoomDiv.update("Print scale: ").insert(minusButton).insert(this.zoomValue).insert(plusButton);
        minusButton.observe('click', function(event) {
            if (_this._zoomLevel > 10) {
                _this._zoomLevel -= 10;
                _this.zoomValue.update(_this._zoomLevel + "%");
                _this._updatePreview();
            }
        });
        plusButton.observe('click', function(event) {
            if (_this._zoomLevel < 250) {
                _this._zoomLevel += 10;
                _this.zoomValue.update(_this._zoomLevel + "%");
                _this._updatePreview();
            }
        });
        mainDiv.insert(zoomDiv);

        var configListElement = new Element('table', {id : 'print-settings'});
        var addLegend = new Element('input', {"type" : "checkbox", "value": "1", "name": "add-legend"});
        addLegend.checked = true;
        var markLabel1 = new Element('label', {'class': 'import-mark-label1'}).insert(addLegend).insert("Print legend on the bottom left sheet").wrap('td').wrap('tr');
        configListElement.insert(markLabel1);
        var includeOverlaps = new Element('input', {"type" : "checkbox", "value": "1", "name": "add-overlap"});
        includeOverlaps.checked = true;
        includeOverlaps.observe('click', function() {
            _this._updatePreview();
        });
        var markLabel2 = new Element('label', {'class': 'import-mark-label2'}).insert(includeOverlaps).insert("Make pages slightly overlapped").wrap('td').wrap('tr');
        configListElement.insert(markLabel2);
        var closeAfterPrint = new Element('input', {"type" : "checkbox", "value": "1", "name": "close-print"});
        closeAfterPrint.checked = true;
        var markLabel3 = new Element('label', {'class': 'import-mark-label2'}).insert(closeAfterPrint).insert("Close window with printer-friendly version after printing").wrap('td').wrap('tr');
        configListElement.insert(markLabel3);

        var dataSection3 = new Element('div', {'class': 'print-settings-block'});
        dataSection3.insert(configListElement);
        mainDiv.insert(dataSection3);

        var buttons = new Element('div', {'class' : 'buttons import-block-bottom'});
        buttons.insert(new Element('input', {type: 'button', name : 'print', 'value': 'Print', 'class' : 'button', 'id': 'print_button'}).wrap('span', {'class' : 'buttonwrapper'}));
        buttons.insert(new Element('input', {type: 'button', name : 'done', 'value': 'Done', 'class' : 'button secondary'}).wrap('span', {'class' : 'buttonwrapper'}));
        mainDiv.insert(buttons);

        var doneButton = buttons.down('input[name="done"]');
        doneButton.observe('click', function(event) {
            _this.hide();
        });
        var printButton = buttons.down('input[name="print"]');
        printButton.observe('click', function(event) {
            _this._onPrint();
        });

        var closeShortcut = ['Esc'];
        this.dialog = new PhenoTips.widgets.ModalPopup(mainDiv, {close: {method : this.hide.bind(this), keys : closeShortcut}}, {extraClassName: "pedigree-print-dialog", title: "Print pedigree", displayCloseButton: true, verticalPosition: "top"});
    },

    /**
     * Prints the pedigree using the scale and option selected.
     */
    _onPrint: function() {
        var closePrintVersion = $$('input[type=checkbox][name="close-print"]')[0].checked;

        var addLegend = $$('input[type=checkbox][name="add-legend"]')[0].checked;

        var addOverlaps = $$('input[type=checkbox][name="add-overlap"]')[0].checked;

        this._printEngine.print(this._getSelectedPrintScale(),
                                addOverlaps,
                                addLegend,
                                closePrintVersion,
                                this._printPageSet);
    },

    /**
     * Displays the template selector
     *
     * @method show
     */
    show: function() {
        this.dialog.show();
        this._updatePreview();
    },

    /**
     * Updates print preview using currently selected zoom level.
     */
    _updatePreview: function() {
        var overlapCheckBox = $$('input[type=checkbox][name="add-overlap"]')[0];
        var addOverlaps = overlapCheckBox ? overlapCheckBox.checked : true;
        var previewHTML = this._printEngine.generatePreviewHTML(730, 390,
                                                                this._getSelectedPrintScale(),
                                                                addOverlaps);
        this.previewContainer.update(previewHTML);

        var _this = this;
        this._printPageSet = {};
        // add click-on-page handlers
        $$("div[id^=pedigree-page-]").forEach(function(page) {
            try {
                var pageIDParts = page.id.match(/pedigree-page-x(\d+)-y(\d+)/);
                var pageX = pageIDParts[1];
                var pageY = pageIDParts[2];

                _this._printPageSet["x" + pageX + "y" + pageY] = true;

                page.observe("click", function() {
                    //console.log("click on page " + pageX + "-" + pageY);
                    if (_this._printPageSet["x" + pageX + "y" + pageY]) {
                        _this._printPageSet["x" + pageX + "y" + pageY] = false;
                        page.style.backgroundColor = "#111";
                        page.style.opacity = 0.1;
                    } else {
                        _this._printPageSet["x" + pageX + "y" + pageY] = true;
                        page.style.backgroundColor = "";
                        page.style.opacity = 1;
                    }
                });
            } catch(err) {
                console.log("Preview page ID mismatch");
            }
        });
    },

    _getSelectedPrintScale: function() {
        return this._defaultScale * this._zoomLevel/100;
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