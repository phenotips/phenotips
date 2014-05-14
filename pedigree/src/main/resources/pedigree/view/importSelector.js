/**
 * The UI Element for importing pedigrees from text representationin various formats
 *
 * @class ImportSelector
 */

var ImportSelector = Class.create( {

    initialize: function() {
        if (editor.isReadOnlyMode()) return;
        
        var mainDiv = new Element('div', {'class': 'import-selector'});
                        
        var promptImport = new Element('div', {'class': 'import-section'}).update("Import data:");
        this.importValue = new Element("textarea", {"id": "import", "value": "", "class": "import-textarea"});
        mainDiv.insert(promptImport).insert(this.importValue);
        
        var _addTypeOption = function (checked, labelText, value) {
            var optionWrapper = new Element('tr');
            var input = new Element('input', {"type" : "radio", "value": value, "name": "select-type"});
            if (checked) {
              input.checked = true;
            }
            var label = new Element('label', {'class': 'import-type-label'}).insert(input).insert(labelText);
            optionWrapper.insert(label.wrap('td'));
            return optionWrapper;
          };          
        var typeListElement = new Element('table', {id : 'import-type'});        
        typeListElement.insert(_addTypeOption(true,  "PED file", "ped"));
        typeListElement.insert(_addTypeOption(false, "LINKAGE file", "linkage"));
        /*typeListElement.insert(_addTypeOption(false, "CSV file", "ped-csv"));
        typeListElement.insert(_addTypeOption(false, "GEDCOM file", "gedcom"));        
        typeListElement.insert(_addTypeOption(false, "Phenotips file", "phenotips"));
        typeListElement.insert(_addTypeOption(false, "Phenotips file with layout", "phenotips_full"));*/        
        var promptType = new Element('div', {'class': 'import-section'}).update("Import type:");
        var dataSection2 = new Element('div', {'class': 'import-block'});
        dataSection2.insert(promptType).insert(typeListElement);
        mainDiv.insert(dataSection2);
                
        var _addConfigOption = function (checked, labelText, value) {
            var optionWrapper = new Element('tr');
            var input = new Element('input', {"type" : "radio", "value": value, "name": "select-options"});
            if (checked) {
              input.checked = true;
            }
            var label = new Element('label', {'class': 'import-config-label'}).insert(input).insert(labelText);
            optionWrapper.insert(label.wrap('td'));
            return optionWrapper;
          };          
        var configListElement = new Element('table', {id : 'import-type'});        
        configListElement.insert(_addConfigOption(true,  "Treat non-standard phenotype values as new disorders", "accept"));
        configListElement.insert(_addConfigOption(false, "Treat non-standard phenotype values as \"no information\"", "dontaccept"));
        var promptConfig = new Element('div', {'class': 'import-section'}).update("Options:");
        var dataSection3 = new Element('div', {'class': 'import-block'});
        dataSection3.insert(promptConfig).insert(configListElement);
        mainDiv.insert(dataSection3);
        
        var buttons = new Element('div', {'class' : 'buttons import-block-bottom'});
        buttons.insert(new Element('input', {type: 'button', name : 'import', 'value': 'Import', 'class' : 'button', 'id': 'import_button'}).wrap('span', {'class' : 'buttonwrapper'}));
        buttons.insert(new Element('input', {type: 'button', name : 'cancel', 'value': 'Cancel', 'class' : 'button secondary'}).wrap('span', {'class' : 'buttonwrapper'}));
        mainDiv.insert(buttons);
      
        var _this = this;
        var cancelButton = buttons.down('input[name="cancel"]');
        cancelButton.observe('click', function(event) {
            _this.hide();
        })
        var importButton = buttons.down('input[name="import"]');
        importButton.observe('click', function(event) {
            _this._onImportStarted();
        })
        
        //var userNameBox = new Element("input", {type: 'text', "id": "newusername", "value": "", "placeholder": 'user name', size: 12});
        
        var closeShortcut = ['Esc'];
        this.dialog = new PhenoTips.widgets.ModalPopup(mainDiv, {close: {method : this.hide.bind(this), keys : closeShortcut}}, {extraClassName: "pedigree-import-chooser", title: "Pedigree import", displayCloseButton: true});
        //xxx.observe('click', this._onimportStarted.bindAsEventListener(this));
    },

    /**
     * Loads the template once it has been selected
     *
     * @param event
     * @param pictureBox
     * @private
     */
    _onImportStarted: function() {   
        var importValue = this.importValue.value;
        var importType  = $$('input:checked[type=radio][name="select-type"]')[0].value;
        
        this.hide();
        
        if (!importValue || importValue == "") {
            alert("Nothing to import!");
            return;
        }

        console.log("Importing:\n" + importValue);
        //console.log("Import type:\n" + importType);
        
        var optionSelected = $$('input:checked[type=radio][name="select-options"]')[0].value;
        var acceptUnknownPhenotypes = (optionSelected == "accept");
        
        editor.getSaveLoadEngine().createGraphFromImportData(importValue, false /* add to undo stack */, true /*center around 0*/, importType, acceptUnknownPhenotypes);
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
        this.importValue.value = "";
        this.dialog.closeDialog();
    }
});