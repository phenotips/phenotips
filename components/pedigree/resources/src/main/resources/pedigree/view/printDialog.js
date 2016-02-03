/**
 * The UI Element for selecting pedigree print settings.
 *
 * Should be instantiated after preferences have been loaded as default paper/printer
 * settings may be stored in the preferences.
 *
 * @class PrintDialog
 */
define([
        "pedigree/pedigreeEditorParameters",
        "pedigree/view/printEngine"
    ], function(
        PedigreeEditorParameters,
        PrintEngine
    ){
    var PrintDialog = Class.create( {

        initialize: function() {
            var _this = this;

            this._defaultScale = 0.4;
            this._landscape = true;
            this._zoomLevel = 100;
            this._moveHorizontally = 0;
            this._printPageSet = {};
            this._printEngine = new PrintEngine();

            var mainDiv = new Element('div', {'class': 'import-selector field-no-user-select cursor-normal'});

            var previewHeader = new Element('div', {'class': 'print-preview-header'}).update("Print preview:<br>(each block indicates a separate printed page; click on a page to exclude/include the page from print job)");
            this.previewContainer = new Element("div", {"id": "preview", "class": "preview-container"});
            var previewFooter = new Element('div', {'class': 'print-preview-footer fa fa-exclamation-triangle'});
            previewFooter.update(new Element('span', {'class': "print-preview-footer-text"}).update("Note: in some browsers you need to manually select correct orientation (landscape or portrait)"));
            mainDiv.insert(previewHeader).insert(this.previewContainer).insert(previewFooter);

            var minusButton = new Element('input', {"type" : "button", "value": "-", "class": "print-small-button print-left-margin"});
            var plusButton  = new Element('input', {"type" : "button", "value": "+", "class": "print-small-button"});
            this.zoomValue  = new Element('label', {"class": "print-zoom-value no-mouse-interaction"}).update("100%");
            var zoom = new Element('span', {"class": "print-zoom-span"});
            zoom.update("Print scale:").insert(minusButton).insert(this.zoomValue).insert(plusButton);

            var leftButton = new Element('input', {"type" : "button", "value": "<", "class": "print-small-button print-small-left-margin"});
            var rightButton  = new Element('input', {"type" : "button", "value": ">", "class": "print-small-button print-small-left-margin"});
            this._centerButton = new Element('input', {"type" : "button", "value": "default", "class": "print-long-button print-small-left-margin"});
            var move = new Element('span', {"class": "print-move-span"});
            move.update("Move on page:").insert(leftButton).insert(this._centerButton).insert(rightButton);

            var landscape = new Element('input', {"type" : "button", "value": "landscape", "class": "print-long-button", "id": "landscape-button"});
            var portrait  = new Element('input', {"type" : "button", "value": "portrait", "class": "print-long-button print-small-left-margin", "id": "portrait-button"});
            var orientation = new Element('span', {"class": "print-orientation-span"});
            orientation.update(landscape).insert(portrait);

            var controlsDiv = new Element('div', {'class': 'pedigree-print-controls-container field-no-user-select'});
            controlsDiv.update(zoom).insert(move).insert(orientation);
            minusButton.observe('click', function(event) {
                if (_this._zoomLevel > 10) {
                    _this._zoomLevel -= 10;
                    _this._moveHorizontally = 0;
                    _this.zoomValue.update(_this._zoomLevel + "%");
                    _this._updatePreview();
                }
            });
            plusButton.observe('click', function(event) {
                if (_this._zoomLevel < 250) {
                    _this._zoomLevel += 10;
                    _this._moveHorizontally = 0;
                    _this.zoomValue.update(_this._zoomLevel + "%");
                    _this._updatePreview();
                }
            });
            leftButton.observe('click', function(event) {
                _this._moveHorizontally += 40;
                _this._updatePreview();
            });
            rightButton.observe('click', function(event) {
                _this._moveHorizontally -= 40;
                _this._updatePreview();
            });
            this._centerButton.observe('click', function(event) {
                _this._moveHorizontally = 0;
                _this._updatePreview();
            });
            landscape.observe('click', function(event) {
                _this._landscape = true;
                _this._moveHorizontally = 0;
                $('landscape-button').disable();
                $('portrait-button').enable();
                _this._updatePreview();
            });
            portrait.observe('click', function(event) {
                _this._landscape = false;
                _this._moveHorizontally = 0;
                $('landscape-button').enable();
                $('portrait-button').disable();
                _this._updatePreview();
            });

            mainDiv.insert(controlsDiv);

            var configListElement = new Element('table', {id : 'print-settings'});
            var removePII = new Element('input', {"type" : "checkbox", "value": "0", "name": "removePII"});
            removePII.checked = false;
            removePII.observe('click', function() {
                _this._updatePreview();
            });
            configListElement.insert(new Element('label', {'class': 'import-mark-label1'}).insert(removePII).insert("Remove PII information (anonymize)").wrap('td').wrap('tr'));
            var removeComments = new Element('input', {"type" : "checkbox", "value": "0", "name": "removeComments"});
            removeComments.checked = false;
            removeComments.observe('click', function() {
                _this._updatePreview();
            });
            configListElement.insert(new Element('label', {'class': 'import-mark-label2'}).insert(removeComments).insert("Remove comments").wrap('td').wrap('tr'));
            var addLegend = new Element('input', {"type" : "checkbox", "value": "1", "name": "add-legend"});
            addLegend.checked = true;
            configListElement.insert(new Element('label', {'class': 'import-mark-label2'}).insert(addLegend).insert("Print legend on the bottom left sheet").wrap('td').wrap('tr'));
            var includeOverlaps = new Element('input', {"type" : "checkbox", "value": "1", "name": "add-overlap", "id": "add-overlaps-checkbox"});
            includeOverlaps.checked = false;
            includeOverlaps.observe('click', function() {
                _this._updatePreview();
            });
            configListElement.insert(new Element('label', {'class': 'import-mark-label2'}).insert(includeOverlaps).insert("Make pages slightly overlapped").wrap('td').wrap('tr'));
            var info = new Element('input', {"type" : "checkbox", "value": "1", "name": "patient-info"});
            info.checked = true;
            info.observe('click', function() {
                _this._updatePreview();
            });
            configListElement.insert(new Element('label', {'class': 'import-mark-label2'}).insert(info).insert("Include patient information and print date at the top of the first page").wrap('td').wrap('tr'));
            var closeAfterPrint = new Element('input', {"type" : "checkbox", "value": "1", "name": "close-print"});
            closeAfterPrint.checked = true;
            configListElement.insert(new Element('label', {'class': 'import-mark-label2'}).insert(closeAfterPrint).insert("Close window with printer-friendly version after printing").wrap('td').wrap('tr'));

            var dataSection3 = new Element('div', {'class': 'print-settings-block'});
            dataSection3.insert(configListElement);
            mainDiv.insert(dataSection3);

            var buttons = new Element('div', {'class' : 'buttons import-block-bottom'});
            this._printButton = new Element('input', {type: 'button', name : 'print', 'value': 'Print', 'class' : 'button', 'id': 'print_button'});
            buttons.insert(this._printButton.wrap('span', {'class' : 'buttonwrapper'}));
            buttons.insert(new Element('input', {type: 'button', name : 'done', 'value': 'Close', 'class' : 'button secondary'}).wrap('span', {'class' : 'buttonwrapper'}));
            mainDiv.insert(buttons);

            var doneButton = buttons.down('input[name="done"]');
            doneButton.observe('click', function(event) {
                _this.hide();
            });
            this._printButton.observe('click', function(event) {
                _this._onPrint();
            });

            var closeShortcut = ['Esc'];
            this.dialog = new PhenoTips.widgets.ModalPopup(mainDiv, {close: {method : this.hide.bind(this), keys : closeShortcut}}, {extraClassName: "pedigree-print-dialog", title: "Print pedigree", displayCloseButton: true, verticalPosition: "top"});

            Event.observe(window, 'resize', _this._adjustPreviewWindowHeight.bind(_this));
        },

        /**
         * Prints the pedigree using the scale and option selected.
         */
        _onPrint: function() {
            var options = this._generateOptions();

            this._printEngine.print(this._landscape,
                                    this._getSelectedPrintScale(),
                                    this._moveHorizontally,
                                    options,
                                    this._printPageSet);
        },

        /**
         * Creates an options object based on state of UI input elements
         */
        _generateOptions: function() {
            var patientInfo = $$('input[type=checkbox][name="patient-info"]')[0].checked;

            var removePII = $$('input[type=checkbox][name="removePII"]')[0].checked;
            var removeComments = $$('input[type=checkbox][name="removeComments"]')[0].checked;
            var anonymize = { "removePII": removePII,
                              "removeComments": removeComments };

            var closePrintVersion = $$('input[type=checkbox][name="close-print"]')[0].checked;

            var addLegend = $$('input[type=checkbox][name="add-legend"]')[0].checked;

            var addOverlaps = $$('input[type=checkbox][name="add-overlap"]')[0].checked;

            return { "includeLegend": addLegend,
                     "legendAtBottom": true,
                     "addOverlaps": addOverlaps,
                     "closeAfterPrint": closePrintVersion,
                     "anonymize": anonymize,
                     "includePatientInfo": patientInfo};
        },

        /**
         * Displays the template selector
         *
         * @method show
         */
        show: function() {
            this.dialog.show();
            this._moveHorizontally = 0;
            this._updatePreview();
        },

        /**
         * Attempts to make preview window fit on screen by adjusting the preview pane height
         */
        _adjustPreviewWindowHeight: function() {
            var canvas = editor.getWorkspace().canvas || $('body');
            var pedigreeDialogue = $$('.pedigree-print-dialog')[0];
            if (!pedigreeDialogue) {
                return;
            }
            var screenHeight = canvas.getHeight() - 10;
            var dialogueHeight = pedigreeDialogue.getHeight();
            var freeSpace = screenHeight - dialogueHeight;
            var previewPaneHeight = $('printPreview').getHeight();
            if (freeSpace < 0) {
                var newPreviewHeight = Math.max(PedigreeEditorParameters.attributes.minPrintPreviewPaneHeight, previewPaneHeight + freeSpace);
                $('printPreview').style.height = newPreviewHeight + "px";
            }
            if (freeSpace > 0 && previewPaneHeight < PedigreeEditorParameters.attributes.maxPrintPreviewPaneHeight) {
                var newPreviewHeight = Math.min(PedigreeEditorParameters.attributes.maxPrintPreviewPaneHeight, previewPaneHeight + freeSpace);
                $('printPreview').style.height = newPreviewHeight + "px";
            }
        },

        /**
         * Updates print preview using currently selected zoom level.
         */
        _updatePreview: function() {
            var options = this._generateOptions();
            var previewHTML = this._printEngine.generatePreviewHTML(this._landscape,
                                                                    730, PedigreeEditorParameters.attributes.maxPrintPreviewPaneHeight,
                                                                    this._getSelectedPrintScale(),
                                                                    this._moveHorizontally,
                                                                    options);
            this.previewContainer.update(previewHTML);
            this._adjustPreviewWindowHeight();

            var _this = this;
            this._printPageSet = {};
            var numPrinted = 0;
            // add click-on-page handlers
            $$("div[id^=pedigree-page-]").forEach(function(page) {
                try {
                    var pageIDParts = page.id.match(/pedigree-page-x(\d+)-y(\d+)/);
                    var pageX = pageIDParts[1];
                    var pageY = pageIDParts[2];

                    _this._printPageSet["x" + pageX + "y" + pageY] = true;
                    numPrinted++;

                    page.observe("click", function() {
                        if (_this._printPageSet["x" + pageX + "y" + pageY]) {
                            _this._printPageSet["x" + pageX + "y" + pageY] = false;
                            page.style.backgroundColor = "#111";
                            page.style.opacity = 0.1;
                            _this._checkPrintbuttonStatus();  // check if there are any pages left
                        } else {
                            _this._printPageSet["x" + pageX + "y" + pageY] = true;
                            page.style.backgroundColor = "";
                            page.style.opacity = 1;
                            _this._checkPrintbuttonStatus();
                        }
                    });
                } catch(err) {
                    console.log("Preview page ID mismatch");
                }
            });
            this._printButton.enable();

            if (numPrinted < 2) {
                $('add-overlaps-checkbox').disable();
            } else {
                $('add-overlaps-checkbox').enable();
            }

            if (this._landscape) {
                $('landscape-button').disable();
                $('portrait-button').enable();
            } else {
                $('landscape-button').enable();
                $('portrait-button').disable();
            }
            if (_this._moveHorizontally == 0) {
                this._centerButton.disable();
            } else {
                this._centerButton.enable();
            }
        },

        /**
         * Disabled print button if there are no pages selected; enables otherwise
         */
        _checkPrintbuttonStatus: function() {
            for (var page in this._printPageSet) {
                if (this._printPageSet.hasOwnProperty(page)) {
                    if (this._printPageSet[page]) {
                        this._printButton.enable();
                        return;
                    }
                }
            }
            this._printButton.disable();
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

    return PrintDialog;
});