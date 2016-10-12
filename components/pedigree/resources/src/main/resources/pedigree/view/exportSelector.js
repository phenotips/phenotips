/**
 * The UI Element for exporting pedigrees
 *
 * @class ExportSelector
 */
define([
        "pedigree/filesaver/FileSaver",
        "pedigree/model/export"
    ], function(
        FileSaver,
        PedigreeExport
    ){
    var ExportSelector = Class.create( {

        initialize: function() {
            var _this = this;

            var mainDiv = new Element('div', {'class': 'export-selector'});

            var _addTypeOption = function (checked, labelText, value) {
                var optionWrapper = new Element('tr');
                var input = new Element('input', {"type" : "radio", "value": value, "name": "export-type"});
                input.observe('click', _this.disableEnableOptions );
                if (checked) {
                  input.checked = true;
                }
                var label = new Element('label', {'class': 'import-type-label'}).insert(input).insert(labelText);
                optionWrapper.insert(label.wrap('td'));
                return optionWrapper;
              };
            var typeListElement = new Element('table');
            typeListElement.insert(_addTypeOption(true,  "PED", "ped"));
            typeListElement.insert(_addTypeOption(false, "BOADICEA", "BOADICEA"));
            typeListElement.insert(_addTypeOption(false, "Simple JSON", "simpleJSON"));
            typeListElement.insert(_addTypeOption(false, "Image", "image"));
            //TODO: typeListElement.insert(_addTypeOption(false, "Phenotips Pedigree JSON", "phenotipsJSON"));

            var fileDownload = new Element('a', {"id": 'downloadLink', "style": 'display:none'});
            mainDiv.insert(fileDownload);

            var promptType = new Element('div', {'class': 'import-section'}).update("Data format:");
            var dataSection2 = new Element('div', {'class': 'import-block'});
            dataSection2.insert(promptType).insert(typeListElement);
            mainDiv.insert(dataSection2);

            var configListElementJSON = new Element('table', {"id": "jsonOptions", "style": 'display:none'});
            configListElementJSON.insert(this._addConfigOption(true,  "export-options", "import-config-label", "All data", "all"));
            configListElementJSON.insert(this._addConfigOption(false, "export-options", "import-config-label", "Remove personal information (name and age)", "nopersonal"));
            configListElementJSON.insert(this._addConfigOption(false, "export-options", "import-config-label", "Remove personal information and free-form comments", "minimal"));

            var configListElementPED = new Element('table', {"id": "pedOptions"});
            var label = new Element('label', {'class': 'export-config-header'}).insert("How should person IDs be assigned in the generated file?");
            configListElementPED.insert(label.wrap('td').wrap('tr'));
            configListElementPED.insert(this._addConfigOption(true,  "ped-options", "export-subconfig-label", "Using External IDs (when available)", "external"));
            configListElementPED.insert(this._addConfigOption(false, "ped-options", "export-subconfig-label", "Generate new numeric IDs", "newid"));
            configListElementPED.insert(this._addConfigOption(false, "ped-options", "export-subconfig-label", "Using Names (when available)", "name"));

            var configListElementImage = new Element('table', {"id": "imageOptions", "style": 'display:none'});
            var label = new Element('label', {'class': 'export-config-header'}).insert("Select type of generated pedigree image");
            configListElementImage.insert(label.wrap('td').wrap('tr'));
            configListElementImage.insert(this._addConfigOption(true,  "image-options", "export-subconfig-label", "Raster image (PNG)", "png"));
            configListElementImage.insert(this._addConfigOption(false, "image-options", "export-subconfig-label", "Scalable image (SVG)", "svg"));

            var promptConfig = new Element('div', {'class': 'import-section'}).update("Options:");
            var dataSection3 = new Element('div', {'class': 'import-block'});
            dataSection3.insert(promptConfig).insert(configListElementJSON).insert(configListElementPED).insert(configListElementImage);
            mainDiv.insert(dataSection3);

            var buttons = new Element('div', {'class' : 'buttons import-block-bottom'});
            buttons.insert(new Element('input', {type: 'button', name : 'export', 'value': 'Export', 'class' : 'button', 'id': 'export_button'}).wrap('span', {'class' : 'buttonwrapper'}));
            buttons.insert(new Element('input', {type: 'button', name : 'cancel', 'value': 'Cancel', 'class' : 'button secondary'}).wrap('span', {'class' : 'buttonwrapper'}));
            mainDiv.insert(buttons);

            var cancelButton = buttons.down('input[name="cancel"]');
            cancelButton.observe('click', function(event) {
                _this.hide();
            })
            var exportButton = buttons.down('input[name="export"]');
            exportButton.observe('click', function(event) {
                _this._onExportStarted();
            })

            var closeShortcut = ['Esc'];
            this.dialog = new PhenoTips.widgets.ModalPopup(mainDiv, {close: {method : this.hide.bind(this), keys : closeShortcut}}, {extraClassName: "pedigree-import-chooser", title: "Pedigree export", displayCloseButton: true});
        },

        /*
         * Disables unapplicable options on input type selection
         */
        disableEnableOptions: function() {
            var exportType = $$('input:checked[type=radio][name="export-type"]')[0].value;

            var pedOptionsTable = $("pedOptions");
            var pedDisorderOptions = $$('[name="ped-disorders-options"]');
            var jsonOptionsTable = $("jsonOptions");
            var imageOptionsTable = $("imageOptions");

            if (exportType == "ped" || exportType == "BOADICEA") {
                pedOptionsTable.show();
                var idgenerator = $$('input[type=radio][name="ped-options"]')[2];
                // disable using names as IDs - not necessary for BOADICEA which supports a dedicated column for the name
                if (exportType == "BOADICEA") {
                    if (idgenerator.checked) {
                        $$('input[type=radio][name="ped-options"]')[0].checked = true;
                    }
                    idgenerator.up().hide();
                    pedDisorderOptions.each( function(item) {item.up('tr').hide();});
                } else {
                    idgenerator.up().show();
                    pedDisorderOptions.each( function(item) {item.up('tr').show();});
                }
                jsonOptionsTable.hide();
                imageOptionsTable.hide();
            } else {
                pedOptionsTable.hide();
                pedDisorderOptions.each( function(item) {item.up('tr').hide();});
                if (exportType == "simpleJSON") {
                    jsonOptionsTable.show();
                    imageOptionsTable.hide();
                } else {
                    jsonOptionsTable.hide();
                    imageOptionsTable.show();
                }
            }
        },

        _onExportStarted: function() {
            this.hide();

            var patientDocument = XWiki.currentDocument.page + "_pedigree";

            var exportType = $$('input:checked[type=radio][name="export-type"]')[0].value;
            //console.log("Import type: " + exportType);

            if (exportType == "image") {
                var imageType = $$('input:checked[type=radio][name="image-options"]')[0].value;

                var _bbox = $('canvas').down().getBBox();
                //set white background
                var svgEl = $$("#canvas svg")[0];
                var background = $$('.panning-background')[0];
                svgEl.insertBefore(new Element('rect', {"id": "white-bbox-background",
                                                        "width": _bbox.width,
                                                        "height": _bbox.height,
                                                        "x": _bbox.x,
                                                        "y": _bbox.y,
                                                        "fill": "white"}),
                                   svgEl.firstChild
                );

                var svg = editor.getWorkspace().getSVGCopy();
                var exportString = svg.getSVGText();
                $('white-bbox-background').remove();

                if (imageType == "png") {
                    // generate PNG image on the back end
                    var pedigreeImageExportServiceURL = editor.getExternalEndpoint().getPedigreeImageExportServiceURL();
                    new Ajax.Request(pedigreeImageExportServiceURL, {
                        method: 'POST',
                        onSuccess: function(response) {
                            if (response.responseJSON && response.responseJSON.url) {
                                window.open(response.responseJSON.url);
                            }
                        },
                        onFailure : function(response) {
                            var errorMessage = '';
                            if (response.statusText == '' /* No response */ || response.status == 12031 /* In IE */) {
                                  errorMessage = 'Server not responding';
                              } else if (response.getHeader('Content-Type').match(/^\s*text\/plain/)) {
                                  // Regard the body of plain text responses as custom status messages.
                                  errorMessage = response.responseText;
                              } else if (response.responseJSON && response.responseJSON.error) {
                                  errorMessage = response.responseJSON.error;
                            }
                            var content = new Element('div', {'class' : 'box errormessage'});
                            content.insert(new Element('p').update("Pedigree image export failed: " + errorMessage));
                            var d = new PhenoTips.widgets.ModalPopup(content, '', {'titleColor' : '#000'});
                            d.show();
                        },
                        parameters: {"image": exportString}
                    });
                } else {
                     var fileName = patientDocument + ".svg";
                }

            } else if (exportType == "simpleJSON") {
                var privacySetting = $$('input:checked[type=radio][name="export-options"]')[0].value;
                var exportString = PedigreeExport.exportAsSimpleJSON(editor.getGraph().DG, privacySetting);
                var fileName = patientDocument + ".json";
                var mimeType = "application/json";
            } else {
                var idGenerationSetting = $$('input:checked[type=radio][name="ped-options"]')[0].value;
                if (exportType == "ped") {
                    var selectedDisorders = $$('input:checked[name="ped-disorders-options"]');
                    if (selectedDisorders.indexOf("all") > -1) {
                        var exportString = PedigreeExport.exportAsPED(editor.getGraph().DG, idGenerationSetting);
                    } else {
                        var exportString = PedigreeExport.exportAsPED(editor.getGraph().DG, idGenerationSetting, selectedDisorders);
                    }
                    var fileName = patientDocument + ".ped";
                } else if (exportType == "BOADICEA") {
                    var exportString = PedigreeExport.exportAsBOADICEA(editor.getGraph(), idGenerationSetting);
                    var fileName = patientDocument + ".txt";
                }
                var mimeType = "text/plain";
            }

            console.log("Export data: >>" + exportString + "<<");
            if (exportString != "") {
                if (exportType != "image") {
                    FileSaver.saveTextFile(exportString, fileName);
                } else if (exportType == "image" && imageType == "svg") {
                    FileSaver.saveSVGFile(exportString, fileName);
                }
            }
        },

        /**
         * Displays the template selector
         *
         * @method show
         */
        show: function() {
            if ($$('input[name="ped-disorders-options"]').length > 0)
                $$('[name="ped-disorders-options"]').each( function(item) {item.up('tr').remove();});
            var disorders = editor.getDisorderLegend().getAllNames();
            var hasDisorders = false;
            var disordersLength = 0;
            for (var key in disorders) {
                if (hasOwnProperty.call(disorders, key)) {
                    hasDisorders = true;
                    disordersLength++;
                }
            }
            if (hasDisorders && disordersLength > 1) {
                var configListElementPED = this.dialog.content.select('#pedOptions')[0];
                var labelDisorderOptions = new Element('label', {'class': 'export-config-header ped-disorders-options', 'name' : 'ped-disorders-options', 'style' : 'margin-top: 0.5em;'}).insert("Which disorders should be reflected in the affected column in PED file? ");
                configListElementPED.insert(labelDisorderOptions.wrap('td').wrap('tr'));
                configListElementPED.insert(this._addConfigOption(true,  "ped-disorders-options", "export-subconfig-label", "All", "all", "checkbox"));
                for (var disorder in disorders) {
                    if (disorder == "affected") {
                        configListElementPED.insert(this._addConfigOption(false,  "ped-disorders-options", "export-subconfig-label", "Unspecified disorder", "affected", "checkbox"));
                    } else {
                        configListElementPED.insert(this._addConfigOption(false,  "ped-disorders-options", "export-subconfig-label", disorders[disorder], disorder, "checkbox"));
                    }
                }
                configListElementPED.on('click', 'input[type=checkbox]', function(event, element) {
                    if (element.value == "all" && element.checked) {
                        //check others
                        $$('input[name="ped-disorders-options"]').each( function(item) {item.checked = true;});
                    } else if (element.value != "all") {
                        // uncheck all
                        $$('input[name="ped-disorders-options"]')[0].checked = false;
                    }
                });
            }

            this.dialog.show();
        },

        /**
         * Removes the the template selector
         *
         * @method hide
         */
        hide: function() {
            this.dialog.closeDialog();
        },

        _addConfigOption: function (checked, name, cssClass, labelText, value, type) {
            var optionWrapper = new Element('tr');
            var itype = type ? type : "radio";
            var input = new Element('input', {"type" : itype, "value": value, "name": name });
            if (checked) {
              input.checked = true;
            }
            var label = new Element('label', {'class': cssClass}).insert(input).insert(labelText);
            optionWrapper.insert(label.wrap('td'));
            return optionWrapper;
        }
    });
    return ExportSelector;
});
