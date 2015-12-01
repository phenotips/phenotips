/**
 * The UI Element for importing pedigrees from text representationin various formats
 *
 * @class ImportSelector
 */
define([
        "pedigree/view/tabbedSelectorTab"
       ], function(
           TabbedSelectorTab
    ){
    var ImportSelector = Class.create(TabbedSelectorTab, {

        initialize: function() {
            if (editor.isReadOnlyMode()) return;

            var _this = this;

            this.mainDiv = new Element('div', {'class': 'import-selector'});

            var promptImport = new Element('div', {'class': 'import-section'}).update("Import data:");
            this.importValue = new Element("textarea", {"id": "import", "value": "", "class": "import-textarea"});
            this.mainDiv.insert(promptImport).insert(this.importValue);

            if (!!window.FileReader && !!window.FileList) {
                // only show the upload link if browser supports FileReader/DOM File API
                // Of the browsers suported by pedigree editor, IE9 and Safari 4 & 5 do not support file API
                var uploadFileSelector = new Element('input', {"type" : "file", "id": 'pedigreeInputFile', "style": 'display:none'});
                uploadFileSelector.observe('change', function(event) {
                    _this.handleFileUpload(this.files);
                    try {
                        this.value = "";  // clear file selector
                    } catch (err) {
                        // some older browsers do not allow setting value of a file input element and may generate a security error
                    }
                })
                var uploadButton = new Element('input', {type: 'button', 'value': ' Select file ', 'class' : 'button import-file-button'}).wrap('span', {'class' : 'buttonwrapper import-file-button-wrapper'});
                uploadButton.observe('click', function(event) {
                    var fileElem = document.getElementById("pedigreeInputFile");
                    fileElem.click();
                })
                var promptImportFile = new Element('div', {'class': 'import-section', }).update("Import file:");
                this.mainDiv.insert(promptImportFile).insert(uploadFileSelector).insert(uploadButton);
            } else {
                this.mainDiv.insert(new Element('div', {'class': 'box infomessage import-warning-nofileupload'})
                            .update("Your browser does not support file upload.<br>Please copy-paste import data in the box above."));
            }

            var _addTypeOption = function (checked, labelText, value) {
                var optionWrapper = new Element('tr');
                var input = new Element('input', {"type" : "radio", "value": value, "name": "select-type"});
                input.observe('click', _this.disableEnableOptions );
                if (checked) {
                  input.checked = true;
                }
                var label = new Element('label', {'class': 'import-type-label'}).insert(input).insert(labelText);
                optionWrapper.insert(label.wrap('td'));
                return optionWrapper;
              };
            var typeListElement = new Element('table');
            //TODO: typeListElement.insert(_addTypeOption(true,  "Autodetect", "auto"));
            typeListElement.insert(_addTypeOption(true,  "PED or LINKAGE (pre- or post- makeped)", "ped"));
            typeListElement.insert(_addTypeOption(false, "GEDCOM", "gedcom"));
            typeListElement.insert(_addTypeOption(false, "BOADICEA", "BOADICEA"));
            typeListElement.insert(_addTypeOption(false, "Simple JSON", "simpleJSON"));
            //TODO: typeListElement.insert(_addTypeOption(false, "Phenotips Pedigree JSON", "phenotipsJSON"));

            var promptType = new Element('div', {'class': 'import-section'}).update("Data format:");
            var dataSection2 = new Element('div', {'class': 'import-block'});
            dataSection2.insert(promptType).insert(typeListElement);
            this.mainDiv.insert(dataSection2);

            var _addConfigOption = function (checked, labelText, value) {
                var optionWrapper = new Element('tr');
                var input = new Element('input', {"type" : "radio", "value": value, "name": "select-options" });
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

            var markEvaluated = new Element('input', {"type" : "checkbox", "value": "1", "name": "mark-evaluated"});
            var markLabel1     = new Element('label', {'class': 'import-mark-label1'}).insert(markEvaluated).insert("Mark all patients with known disorder status with 'documented evaluation' mark").wrap('td').wrap('tr');
            configListElement.insert(markLabel1);
            var markExternal = new Element('input', {"type" : "checkbox", "value": "1", "name": "mark-external"});
            markExternal.checked = true;
            var markLabel2   = new Element('label', {'class': 'import-mark-label2'}).insert(markExternal).insert("Save individual IDs as given in the input data as 'external ID'").wrap('td').wrap('tr');
            configListElement.insert(markLabel2);

            var promptConfig = new Element('div', {'class': 'import-section'}).update("Options:");
            var dataSection3 = new Element('div', {'class': 'import-block'});
            dataSection3.insert(promptConfig).insert(configListElement);
            this.mainDiv.insert(dataSection3);

            //TODO: [x] auto-combine multiple unaffected children when the number of children is greater than [5]

            this.importButton = new Element('input', {type: 'button', name : 'import', 'value': 'Import', 'class' : 'button', 'id': 'import_button'});
            this.importButton.disabled = true;
            this.cancelButton = new Element('input', {type: 'button', name : 'cancel', 'value': 'Cancel', 'class' : 'button secondary'});
            var buttons = new Element('div', {'class' : 'buttons import-block-bottom'});
            buttons.insert(this.importButton.wrap('span', {'class' : 'buttonwrapper'}));
            buttons.insert(this.cancelButton.wrap('span', {'class' : 'buttonwrapper'}));
            this.mainDiv.insert(buttons);

            this.cancelButton.observe('click', function(event) {
                _this.close();
            });
            this.importButton.observe('click', function(event) {
                _this._onImportStarted();
            });

            this.textareaChangeFunction = function() {
                if (_this.importValue.value == "") {
                    _this.importButton.disabled = true;
                } else {
                    _this.importButton.disabled = false;
                }
            };
            // different browsers require different event ofr onchange detection (special cases: IE, paste, backspace)
            var eventList = ["input","propertychange", "mousemove", "keyup"];
            eventList.each(function(item) {
                _this.importValue.observe(item, _this.textareaChangeFunction);
            });
        },

        getContentDiv: function() {
            return this.mainDiv;
        },

        getTitle: function() {
            return "Import";
        },

        /*
         * Populates the text input box with the selected file content (asynchronously)
         */
        handleFileUpload: function(files) {
            for (var i = 0, numFiles = files.length; i < numFiles; i++) {
                var nextFile = files[i];
                console.log("loading file: " + nextFile.name + ", size: " + nextFile.size);

                var _this = this;
                var fr = new FileReader();
                fr.onload = function(e) {
                    _this.importValue.value = e.target.result;  // e.target.result should contain the text
                    _this.textareaChangeFunction();
                };
                fr.readAsText(nextFile);
            }
        },

        /*
         * Disables unapplicable options on input type selection
         */
        disableEnableOptions: function() {
            var importType = $$('input:checked[type=radio][name="select-type"]')[0].value;
            //console.log("Import type: " + importType);
            var pedOnlyOptions = $$('input[type=radio][name="select-options"]');
            for (var i = 0; i < pedOnlyOptions.length; i++) {
                if (importType != "ped") {
                    pedOnlyOptions[i].disabled = true;
                } else {
                    pedOnlyOptions[i].disabled = false;
                }
            }
            var pedAndGedcomOption = $$('input[type=checkbox][name="mark-evaluated"]')[0];
            if (importType != "ped" && importType != "gedcom") {
                pedAndGedcomOption.disabled = true;
            } else {
                pedAndGedcomOption.disabled = false;
            }

            var saveExternalID = $$('input[type=checkbox][name="mark-external"]')[0];
            if (importType == "simpleJSON") {
                saveExternalID.disabled = true;
            } else {
                saveExternalID.disabled = false;
            }
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
            console.log("Importing:\n" + importValue);

            this.close();

            if (!importValue || importValue == "") {
                alert("Nothing to import!");
                return;
            }

            var importType = $$('input:checked[type=radio][name="select-type"]')[0].value;
            console.log("Import type: " + importType);

            var importMark = $$('input[type=checkbox][name="mark-evaluated"]')[0].checked;

            var externalIdMark = $$('input[type=checkbox][name="mark-external"]')[0].checked;

            var optionSelected = $$('input:checked[type=radio][name="select-options"]')[0].value;
            var acceptUnknownPhenotypes = (optionSelected == "accept");

            var importOptions = { "markEvaluated": importMark, "externalIdMark": externalIdMark, "acceptUnknownPhenotypes": acceptUnknownPhenotypes };

            editor.getSaveLoadEngine().createGraphFromImportData(importValue, importType, importOptions,
                                                                 false /* add to undo stack */, true /*center around 0*/);
        },

        onShow: function(allowCancel) {
            if (allowCancel) {
                this.cancelButton.show();
            } else {
                this.cancelButton.hide();
            }
        },

        onHide: function() {
            this.importValue.value = "";
        }
    });

    return ImportSelector;
});