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
            this.dialog = new PhenoTips.widgets.ModalPopup(mainDiv, {close: {method : this.hide.bind(this), keys : closeShortcut}}, {extraClassName: "pedigree-import-chooser", title: "Pedigree export", displayCloseButton: false, verticalPosition: "top"});
        },

        /*
         * Disables unapplicable options on input type selection
         */
        disableEnableOptions: function() {
            var exportType = $$('input:checked[type=radio][name="export-type"]')[0].value;

            var pedOptionsTable = $("pedOptions");
            var pedSpecialOptions = $$('.ped-special-options');
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
                    pedSpecialOptions.each( function(item) {item.hide();});
                } else {
                    idgenerator.up().show();
                    pedSpecialOptions.each( function(item) {item.show();});
                }
                jsonOptionsTable.hide();
                imageOptionsTable.hide();
            } else {
                pedOptionsTable.hide();
                pedSpecialOptions.each( function(item) {item.hide();});

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
            var patientDocument = XWiki.currentDocument.page + "_pedigree";

            var exportType = $$('input:checked[type=radio][name="export-type"]')[0].value;
            //console.log("Export type: " + exportType);

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
                                $('body').insert('<iframe width="0" height="0" src="' + response.responseJSON.url + '"></iframe>');
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
                        on1223 : function(response) {
                            response.request.options.onSuccess(response);
                        },
                        on0 : function(response) {
                            response.request.options.onFailure(response);
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
                    var selectedOptions = $$('.ped-special-options input:checked');
                    if (selectedOptions.length > 0) {
                        var selectedMap = {};
                        selectedOptions.each( function(item) {
                            if (!selectedMap.hasOwnProperty(item.name)){
                                selectedMap[item.name] = [];
                            }
                            selectedMap[item.name].push(item.value);
                        });
                        var exportString = PedigreeExport.exportAsPED(editor.getGraph().DG, idGenerationSetting, selectedMap);
                    } else {
                        var exportString = PedigreeExport.exportAsPED(editor.getGraph().DG, idGenerationSetting);
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

            this.hide();
        },

        /**
         * Displays the export selector dialog
         *
         * @method show
         */
        show: function() {
            var disorders = editor.getDisorderLegend().getAllNames();
            var hpos = editor.getHPOLegend().getAllNames();
            var cancers = editor.getCancerLegend().getAllNames();
            var candidateGenes = editor.getCandidateGeneLegend().getAllNames();
            var causalGenes = editor.getCausalGeneLegend().getAllNames();

            var pedContainer = this.dialog.content.select('#pedOptions')[0];

            var hasDisorders = Object.keys(disorders).length > 0;
            var hasPhenotypes = Object.keys(hpos).length > 0;
            var hasCancers = Object.keys(cancers).length > 0;
            var hasCandidateGenes = Object.keys(candidateGenes).length > 0;
            var hasCausalGenes = Object.keys(causalGenes).length > 0;

            if (hasDisorders || hasPhenotypes || hasCancers || hasCandidateGenes || hasCausalGenes) {
                var label = new Element('label', {'class': 'export-config-header ped-header'}).insert("Which of the following should be reflected in the affected column in PED file? ");
                pedContainer.insert(label.wrap('td').wrap('tr', {'class' : "ped-special-options"}));

                hasDisorders && this._addPedOption("disorders", disorders, pedContainer);
                hasPhenotypes && this._addPedOption("phenotypes", hpos, pedContainer);
                hasCancers && this._addPedOption("cancers", cancers, pedContainer);
                hasCandidateGenes && this._addPedOption("candidateGenes", candidateGenes, pedContainer, "candidate genes");
                hasCausalGenes && this._addPedOption("causalGenes", causalGenes, pedContainer, "confirmed causal genes");
            }

            this.dialog.show();
        },

        /**
         * Removes the export selector dialog
         *
         * @method hide
         */
        hide: function() {
            $$('.ped-special-options').each( function(item) {item.remove();});
            this.dialog.closeDialog();
        },

        _addConfigOption: function (checked, name, cssClass, labelText, value, type, isPedSpecialOption) {
            var optionWrapper = new Element('tr', {'class' : (isPedSpecialOption) ? "ped-special-options" : ""});
            var itype = type ? type : "radio";
            var input = new Element('input', {"type" : itype, "value": value, "name": name });
            if (checked) {
              input.checked = true;
            }
            var label = new Element('label', {'class': cssClass}).insert(input).insert(labelText);
            optionWrapper.insert(label.wrap('td'));
            return optionWrapper;
        },

        _addPedOption: function (type, data, pedContainer, labelText) {
            var cssClass = "ped-" + type + "-options";
            var text = (labelText) ? labelText : type;
            var label = new Element('label', {'class': 'export-config-header ' + cssClass, 'name' : cssClass}).insert(text + ":");
            pedContainer.insert(label.wrap('td').wrap('tr', {'class' : "ped-special-options"}));

            // adding "All" checkbox
            if (Object.keys(data).length > 1) {
                pedContainer.insert(this._addConfigOption((type == "disorders"), cssClass, "export-subconfig-label", "All", "all", "checkbox", true));
                pedContainer.on('click', 'input[type=checkbox][name="' + cssClass + '"]', function(event, element) {
                    if (element.value == "all") {
                        $$('input[name="' + cssClass + '"]').each( function(item) {item.checked = element.checked;});
                    } else {

                        // either uncheck checkbox for "All" if some sub-items are unchcked, or check it if all
                        // subitems became checked
                        var unchecked = false;
                        $$('input[name="' + cssClass + '"]').each( function(item) {if (item.value != "all" && !item.checked) { unchecked = true; };});

                        $$('input[name="' + cssClass + '"]')[0].checked = !unchecked;
                    }
                });
            }

            for (var item in data) {
                if (type == "disorders" && item == "affected") {
                    pedContainer.insert(this._addConfigOption((type == "disorders"), cssClass, "export-subconfig-label ped-option", "Unspecified disorder", "affected", "checkbox", true));
                } else {
                    pedContainer.insert(this._addConfigOption((type == "disorders"), cssClass, "export-subconfig-label ped-option", data[item], item, "checkbox", true));
                }
            }
        }

    });
    return ExportSelector;
});
