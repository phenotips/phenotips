/**
 * The UI Element for exporting pedigrees
 *
 * @class ExportSelector
 */

var ExportSelector = Class.create( {

    initialize: function() {
        var _this = this;
        
        var mainDiv = new Element('div', {'class': 'import-selector'});
        
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
        //TODO: typeListElement.insert(_addTypeOption(false, "Phenotips Pedigree JSON", "phenotipsJSON"));
        
        var fileDownload = new Element('a', {"id": 'downloadLink', "style": 'display:none'});
        mainDiv.insert(fileDownload);

        var promptType = new Element('div', {'class': 'import-section'}).update("Data format:");
        var dataSection2 = new Element('div', {'class': 'import-block'});
        dataSection2.insert(promptType).insert(typeListElement);
        mainDiv.insert(dataSection2);
        
        var _addConfigOption = function (checked, name, cssClass, labelText, value) {
            var optionWrapper = new Element('tr');
            var input = new Element('input', {"type" : "radio", "value": value, "name": name });
            if (checked) {
              input.checked = true;
            }
            var label = new Element('label', {'class': cssClass}).insert(input).insert(labelText);
            optionWrapper.insert(label.wrap('td'));
            return optionWrapper;
          };
        var configListElementJSON = new Element('table', {"id": "jsonOptions", "style": 'display:none'});
        configListElementJSON.insert(_addConfigOption(true,  "export-options", "import-config-label", "All data", "all"));
        configListElementJSON.insert(_addConfigOption(false, "export-options", "import-config-label", "Remove personal information (name and age)", "nopersonal"));
        configListElementJSON.insert(_addConfigOption(false, "export-options", "import-config-label", "Remove personal information and free-form comments", "minimal"));

        var configListElementPED = new Element('table', {"id": "pedOptions"});
        var label = new Element('label', {'class': 'export-config-header'}).insert("How should person IDs be assigned in the generated file?");
        configListElementPED.insert(label.wrap('td').wrap('tr'));         
        configListElementPED.insert(_addConfigOption(true,  "ped-options", "export-subconfig-label", "Using External IDs (when available)", "external"));
        configListElementPED.insert(_addConfigOption(false, "ped-options", "export-subconfig-label", "Generate new numeric IDs", "newid"));
        configListElementPED.insert(_addConfigOption(false, "ped-options", "export-subconfig-label", "Using Names (when available)", "name"));        

        var promptConfig = new Element('div', {'class': 'import-section'}).update("Options:");
        var dataSection3 = new Element('div', {'class': 'import-block'});        
        dataSection3.insert(promptConfig).insert(configListElementJSON).insert(configListElementPED);
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
        var jsonOptionsTable = $("jsonOptions");
        
        if (exportType == "ped" || exportType == "BOADICEA") {
            pedOptionsTable.show();
            var idgenerator = $$('input[type=radio][name="ped-options"]')[2];
            // disable using names as IDs - not necessary for BOADICEA which suports a dedicated column for the name
            if (exportType == "BOADICEA") {
                if (idgenerator.checked) {
                    $$('input[type=radio][name="ped-options"]')[0].checked = true;
                }
                idgenerator.up().hide();
            } else {
                idgenerator.up().show();
            }
            jsonOptionsTable.hide();
        } else {
            pedOptionsTable.hide();
            jsonOptionsTable.show();            
        }
    },

    /**
     * Loads the template once it has been selected
     *
     * @param event
     * @param pictureBox
     * @private
     */
    _onExportStarted: function() {
        this.hide();

        var patientDocument = XWiki.currentDocument.page + " pedigree";

        var exportType = $$('input:checked[type=radio][name="export-type"]')[0].value;
        //console.log("Import type: " + exportType);

        if (exportType == "simpleJSON") { 
            var privacySetting = $$('input:checked[type=radio][name="export-options"]')[0].value;
            var exportString = PedigreeExport.exportAsSimpleJSON(editor.getGraph().DG, privacySetting);
            var fileName = patientDocument + ".json";
            var mimeType = "application/json";
        } else {
            var idGenerationSetting = $$('input:checked[type=radio][name="ped-options"]')[0].value;
            if (exportType == "ped") {
                var exportString = PedigreeExport.exportAsPED(editor.getGraph().DG, idGenerationSetting);
                var fileName = patientDocument + ".ped";
            } else if (exportType == "BOADICEA") {
                var exportString = PedigreeExport.exportAsBOADICEA(editor.getGraph(), idGenerationSetting);
                var fileName = patientDocument + ".txt";
            }
            var mimeType = "text/plain";
        }

        console.log("Export data: >>" + exportString + "<<");
        if (exportString != "") {
            saveTextAs(exportString, fileName);
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