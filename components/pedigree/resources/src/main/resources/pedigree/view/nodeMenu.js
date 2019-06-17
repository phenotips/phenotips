/**
 * NodeMenu is a UI dialogue that allows editing of multiple data fields of various types.
 * The data will be displayed in a tabbed dialogue, with field parameters determining which data goes to which tab.
 *
 * The dialog is supposed to be initialized once with a list of all posible fields (at which point
 * all the DOM elements for all the fields are created, and the dialog is hidden), and then displayed
 * when necessary using the `show()` method which takes a `node` object as a parameter and populates
 * the fields based on the data obtained via the `node.getSummary()` method (at which point some fields
 * may be hidden or disabled, depending on the data specific to the node).
 *
 * @class NodeMenu
 * @constructor
 * @param {Array} data - a list of objects each representing one input field (in the field definition format described below)
 * @param {String} additionalCSSClass - additional CSS added to the main dialog container/div element
 * @param {Object} options - a set of (optional) parameters modifying some aspects of dialog behaviour (in the format described below)
 *
 * Field definition format:
 *   [
 *      {
 *          'name'    : the internal name of the menu item,
 *          'label'   : the text label above the input element for this item,
 *          'type'    : determines what kind of input element is rendered for this input field, and sets its behaviour;
 *                      supported types are:
 *                         - generic: 'radio', 'text', 'textarea', 'date-picker', 'select'
 *                         - phenotips specific: 'phenotipsid-picker', 'gene-picker', 'disease-picker'
 *                      the type determines what kind of input element is rendered for this input field,
 *          'tab'     : name of the tab to display the data on. The set of tabs presented is determined based on the tabs specified
 *                      in this field,
 *          'function': (optional) the method name of a Node object which is used to set the value of this type of data;
 *                      if the function name starts with a 'set' (e.g. "setXXX") a "pedigree:node:setproperty" event will be
 *                      triggered for each edit of the field, otherwise a "pedigree:node:modify" is triggered for each edit;
 *                      note: events are not triggered in the "noDynamicUpdates" mode, see `options` description below)
 *          'default' : (optional) default value of the field, if none is provided when the dialog is displayed via `show()`
 *
 *          Some of the data types support additional parameters:
 *
 *          'values'  : specifies the set of options for the `radio`-typed inputs, should be an array of objects in the format below:
 *                      [
 *                        {'actual' : actual value of the option, 'displayed' : the way the option will be displayed in the dialog},
 *                        ...
 *                      ]
 *      }, ...
 *   ]
 *
 * `getSummary()` format which is used to set the value of the fields when dialog is displayed:
 *
 *   {
 *     name: {                     // should match the name of a field as specified in the field definition array above
 *       'value' : ...,            // the value that the element of this type (specified in the field definiton array) knows how to render
 *       'disabled': boolean,      // (optional) when true, the element is displayed but grayed out and not available for edit
 *       'inactive': boolean },    // (optional) when true, the element is not displayed at all
 *     ...
 *   }
 *
 * `options` object format:
 *   {
 *     'modalMode'       : boolean (optional),  // when true, the dialog is displayed as modal, and can only be closed by
 *                                              //            pressing a dedicated button (e.g. "cancel" or "create new patient" buttons)
 *     'noDynamicUpdates': boolean (optional),  // when true, no events are fired after every edit, and the only way to get the
 *                                              //            data entered in the dialog is by processing events triggered by
 *                                              //            special buttons such as the "create new patient" button
 *                                              // when false (default) an event is fired for every edit of every field that has a
 *                                              //            'function' property set (either "pedigree:node:modify"
 *                                              //            or "pedigree:node:setproperty", depending on function name, see above)
 *   }
 */
define([
        "pedigree/disorder",
        "pedigree/hpoTerm",
        "pedigree/gene",
        "pedigree/pedigreeDate",
        "pedigree/model/helpers",
        "pedigree/view/datepicker",
        "pedigree/view/graphicHelpers",
        "pedigree/view/ageCalc"
    ], function(
        Disorder,
        HPOTerm,
        Gene,
        PedigreeDate,
        Helpers,
        DatePicker,
        GraphicHelpers,
        AgeCalc
    ){
    NodeMenu = Class.create({
        initialize : function(data, additionalCSSClass, options) {

            var tabs = this._findActiveTabs(data);

            //console.log("nodeMenu initialize");
            this._justOpened = false;
            this._forceValidation = false;

            this._modalMode = options && options.hasOwnProperty("modalMode") ? options.modalMode : false;
            this._noDynamicUpdates = options && options.hasOwnProperty("noDynamicUpdates") ? options.noDynamicUpdates : false;

            this._notValidMessageOnCurrentTab = options && options.hasOwnProperty("sameTabInvalidMessage")
                                                ? options.sameTabInvalidMessage
                                                : "All the required fields should be filled before the data can be saved";
            this._notValidMessageOnOtherTab = options && options.hasOwnProperty("otherTabInvalidMessage")
                                              ? options.otherTabInvalidMessage
                                              : "All the required fields on another tab should be filled before the data can be saved";

            "All the required fields on another tab should be filled before the data can be saved"
            this.canvas = editor.getWorkspace().canvas || $('body');
            var cssClass = 'menu-box' + (additionalCSSClass ? (" " + additionalCSSClass) : "");
            this.menuBox = new Element('div', {'class' : cssClass});
            // width: 27em with 3 tabs, add 6.5em for each additional tab
            var menuWidthEM = 27 + Math.max(0,(tabs.length - 3))*6.5;
            this.menuBox.style.width = menuWidthEM + "em";

            this.closeButton = new Element('span', {'class' : 'close-button field-no-user-select'}).update('×');
            this.menuBox.insert({'top': this.closeButton});
            this.closeButton.observe('click', this.hide.bindAsEventListener(this));

            this.form = new Element('form', {'method' : 'get', 'action' : '', 'class': 'tabs-content'});

            this.tabs = {};
            this.tabHeaders = {};
            if (tabs && tabs.length > 0) {
                this.tabTop = new Element('dl', {'class':'tabs'});
                for (var i = 0; i < tabs.length; i++) {
                    var tabName = tabs[i];
                    var activeClass = (i == 0) ? "active" : "";
                    this.tabs[tabName] = new Element('div', {'id': 'tab_' + tabName.replace(' ',''), 'class': 'content ' + activeClass});

                    this.tabs[tabName].messageContainer = new Element('div', {'class': 'tab-message-container'});
                    this.tabs[tabName].messageContainer.hide();
                    this.tabs[tabName].insert(this.tabs[tabName].messageContainer);

                    this.form.insert(this.tabs[tabName]);

                    this.tabHeaders[tabName] = new Element('dd', {"class": activeClass}).insert("<a>" + tabName + "</a>");
                    var _this = this;
                    var switchTab = function(tabName) {
                        return function() {
                            for (var tab in _this.tabs) {
                                if (_this.tabs.hasOwnProperty(tab)) {
                                    if (tab != tabName) {
                                        _this.tabs[tab].className = "content";
                                        _this.tabHeaders[tab].className = "";
                                    } else {
                                        _this.tabs[tab].className = "content active";
                                        _this.tabHeaders[tab].className = "active";
                                    }
                                }
                            }
                            _this.reposition();
                        }
                    }
                    this.tabHeaders[tabName].observe('click', switchTab(tabName));
                    this.tabTop.insert(this.tabHeaders[tabName]);
                }
                var div = new Element('div', {'class': 'tabholder'}).insert(this.tabTop).insert(this.form);
                this.menuBox.insert({'bottom' : div});
            } else {
                this.singleTab = new Element('div', {'class': 'tabholder'}).insert(this.form);
                this.menuBox.insert({'bottom' : this.singleTab});
                this.closeButton.addClassName("close-button-old");
                this.form.addClassName("content");
            }

            this.fieldMap = {};
            // Generate fields
            var _this = this;
            data.each(function(d) {
                if (typeof (_this._generateField[d.type]) == 'function') {
                    var insertLocation = _this.form;
                    if (d.tab && _this.tabs.hasOwnProperty(d.tab)) {
                        insertLocation = _this.tabs[d.tab];
                    }
                    insertLocation.insert(_this._generateField[d.type].call(_this, d));
                }
            });

            // Insert in document
            this.menuBox.hide();
            editor.getWorkspace().getWorkArea().insert(this.menuBox);


            this.invalidValueScreen = new Element('div', {'class' : 'invalidValueScreen'});
            this.invalidValueScreen.hide();
            editor.getWorkspace().getWorkArea().insert(this.invalidValueScreen);

            this._onClickOutside = this._onClickOutside.bindAsEventListener(this);

            // Attach pickers and suggest widgets
            this._generatePickersAndSuggests();

            // Update disorder data (color & name)
            this._updateDisorder = function(id, color, name) {
                this.menuBox.select('.field-disorders li input[value="' + id + '"]').each(function(item) {
                    var colorBubble = item.up('li').down('.abnormality-color');
                    if (!colorBubble) {
                        colorBubble = new Element('span', {'class' : 'abnormality-color'});
                        item.up('li').insert({top : colorBubble});
                    }
                    colorBubble.setStyle({background : color});
                    var nameElement = item.up().down('span');
                    nameElement && (nameElement.innerText = name);
                });
            }.bind(this);
            document.observe('disorder:name', function(event) {
                if (!event.memo || !event.memo.id || !event.memo.name) {
                    return;
                }
                _this._updateDisorder(event.memo.id, editor.getDisorderLegend().getObjectColor(event.memo.id), event.memo.name);
            });
            document.observe('disorder:color', function(event) {
                if (!event.memo || !event.memo.id || !event.memo.color) {
                    return;
                }
                _this._updateDisorder(event.memo.id, event.memo.color, editor.getDisorderLegend().getName(event.memo.id) );
            });

            // Update HPO term human readable name
            this._updateHPOTerm = function(id, name) {
                this.menuBox.select('.field-hpo_positive li input[value="' + id + '"]').each(function(item) {
                    var nameElement = item.up().down('span');
                    nameElement && (nameElement.innerText = name);
                });
            }.bind(this);
            document.observe('term:name', function(event) {
                if (!event.memo || !event.memo.id || !event.memo.name) {
                    return;
                }
                _this._updateHPOTerm(event.memo.id, event.memo.name);
            });

            // Update gene: maybe only the color, maybe the symbol, maybe ID has changed
            //              (when old ID was actually the symbol and an ensemble id was found for it)
            this._updateGene = function(container, oldid, newid, symbol, color) {
                // expected container HTML stucture:
                // <li>
                //   <span class="abnormality-color" style="background: rgb(nn,nn,nn);"></span>
                //   <label class="accepted-suggestion" for="candidate_genes_ENSG00000213281">
                //     <input type="hidden" name="candidate_genes" id="candidate_genes_ENSG00000213281" value="ENSG00000213281">
                //     <span class="value">NRAS</span>
                //   </label>
                // </li>
                container.select('li input[value="' + oldid + '"]').each(function(input_item) {
                    if (color) {
                        var colorBubble = input_item.up('li').down('.abnormality-color');
                        if (!colorBubble) {
                            colorBubble = new Element('span', {'class' : 'abnormality-color'});
                            input_item.up('li').insert({top : colorBubble});
                        }
                        colorBubble.setStyle({background : color});
                    }
                    if (newid != oldid) {
                        // need to update
                        // 1) for="..._genes_OLDID" to for="..._genes_NEWID"
                        // 2) <input id="..._genes_OLDID" to <input id="..._genes_NEWID"
                        // 3) <input value="OLDID" to value="NEWID"
                        input_item.value = newid;

                        var oldCompleteId = input_item.id;
                        var newCompleteId = oldCompleteId.replace(oldid, newid);
                        input_item.id = newCompleteId;
                        input_item.up('label').for = newCompleteId;
                    }
                    if (symbol != null) {
                        input_item.up('label').down('span').innerHTML = symbol;
                    }
                });
            }.bind(this);
            document.observe('gene:color', function(event) {
                if (!event.memo || !event.memo.id || !event.memo.color || !event.memo.prefix) {
                    return;
                }
                try {
                    var container = $$('.field-' + event.memo.prefix + "_genes")[0];
                    _this._updateGene(container, event.memo.id, event.memo.id, null, event.memo.color);
                } catch (err) {
                    //in case of any CSS selector/DOM manipulation errors
                }
            });
            document.observe('gene:loaded', function(event) {
                if (!event.memo || !event.memo.oldid || !event.memo.newid || !event.memo.symbol) {
                    return;
                }
                try {
                    // need to update all gene fields, as more than one field may have the gene
                    // (find all elements which start with "field-" and end with "_genes")
                    var containers = $$("div[class^='field-'][class$='_genes']");
                    for (var i = 0; i < containers.length; i++) {
                        _this._updateGene(containers[i], event.memo.oldid, event.memo.newid, event.memo.symbol, null);
                    }
                } catch (err) {
                    //in case of any CSS selector/DOM manipulation errors
                }
            });
        },

        _findActiveTabs: function (data) {
            var tabsInOrder = [];
            var tabSet = {};
            data.each(function(field) {
                if (field.hasOwnProperty("tab")) {
                    if (!tabSet.hasOwnProperty(field.tab)) {
                        tabSet[field.tab] = true;
                        tabsInOrder.push(field.tab);
                    }
                }
            });
            return tabsInOrder;
        },

        _generatePickersAndSuggests: function() {
            // date
            this.form.select('.fuzzy-date').each(function(item) {
                if (!item.__datePicker) {
                    var inputMode = editor.getPreferencesManager().getConfigurationOption("dateEditFormat");
                    item.__datePicker = new DatePicker(item, inputMode);
                }
            });
            var suggestOptions = {
                    'omim' : {
                        script: editor.getExternalEndpoint().getOMIMServiceURL() + "/suggest?",
                        noresults: "No matching terms",
                        resultsParameter : "rows",
                        resultValue : "name",
                        resultInfo : {},
                        tooltip: 'omim-disease-info'
                    },
                    'ethnicity' : {
                        script: editor.getExternalEndpoint().getEthnicitySearchURL() + "/suggest?",
                        noresults: "No matching terms",
                        resultsParameter : "rows",
                        resultValue : "name",
                        resultInfo : {}
                    },
                    'genes' : {
                        script: editor.getExternalEndpoint().getHGNCServiceURL() + "/suggest?",
                        noresults: "No matching terms",
                        resultsParameter : "rows",
                        resultValue : "symbol",
                        resultId : "ensembl_gene_id",
                        forceFirstId : true,
                        resultInfo : {},
                        tooltip : 'gene-info'
                    },
                    'hpo' : {
                        script: editor.getExternalEndpoint().getHPOServiceURL() + "/suggest?start=0&rows=100&",
                        noresults: "No matching terms",
                        resultsParameter : "rows",
                        resultValue : "name",
                        resultAltName: "synonym",
                        resultCategory : "term_category",
                        resultInfo : {},
                        resultParent : "is_a",
                        tooltip: 'phenotype-info'
                    },
                    'cancers' : {
                        script: editor.getExternalEndpoint().getHPOServiceURL() + "/suggest?start=0&rows=100&customFilter=term_category:HP%5C%3A0002664&",
                        noresults: "No matching terms",
                        resultsParameter : "rows",
                        resultValue : "name",
                        resultAltName: "synonym",
                        resultCategory : "term_category",
                        resultInfo : {},
                        resultParent : "is_a",
                        tooltip: 'phenotype-info'
                    },
                    'patients' : {
                        script: editor.getExternalEndpoint().getPatientSuggestServiceURL(),
                        noresults: "No matching patients",
                        width: 337,
                        resultsParameter: "matchedPatients",
                        resultValue: "textSummary",
                        resultInfo: {}
                    }
            };
            var suggetTypes = Object.keys(suggestOptions);
            // -----------------------------------------------------------------
            // Create and attach the suggests and suggest pickers
            // -----------------------------------------------------------------
            for (var i = 0; i < suggetTypes.length; i++) {
                var options = suggestOptions[suggetTypes[i]];
                this.form.select('input.suggest-' + suggetTypes[i]).each(function(item) {
                    if (!item.hasClassName('initialized')) {
                        // Create the Suggest.
                        options.parentContainer = item.up();
                        item._suggest = new PhenoTips.widgets.Suggest(item, options);
                        if (item.hasClassName('multi') && typeof(PhenoTips.widgets.SuggestPicker) != "undefined") {
                            item._suggestPicker = new PhenoTips.widgets.SuggestPicker(item, item._suggest, {
                                'showKey' : false,
                                'enableSort' : false,
                                'listInsertionElt' : 'input',
                                'acceptFreeText' : true
                            });
                        }
                        item.addClassName('initialized');
                        document.observe('ms:suggest:containerCreated', function(event) {
                            if (event.memo && event.memo.suggest === item._suggest) {
                                item._suggest.container.setStyle({'overflow': 'auto', 'maxHeight': document.viewport.getHeight() - item._suggest.container.cumulativeOffset().top + 'px'})
                            }
                        });
                    }
                });
            }
        },

        _generateEmptyField : function (data) {
            var result = new Element('div', {'class' : 'field-box field-' + data.name});
            var fieldNameClass = 'field-name';
            if (data.type == "radio") {
                fieldNameClass += ' field-no-user-select';
            }
            if (data.type == "checkbox") {
                fieldNameClass += ' field-checkbox field-no-user-select';
            }
            if (data.addCSS && typeof data.addCSS === 'object') {
                for(var styleName in data.addCSS) {
                    result.style[styleName] = data.addCSS[styleName];
                }
            }
            var label = new Element('label', {'class' : fieldNameClass}).update(data.label);
            result.inputsContainer = new Element('div', {'class' : 'field-inputs'});
            result.insert(label).insert(result.inputsContainer);
            this.fieldMap[data.name] = {
                'type' : data.type,
                'tab'  : data.tab,
                'element' : result,
                'default' : data["default"] || '',
                'crtValue' : data["default"] || '',
                'function' : data['function'],
                'inactive' : false,
                'required' : data["required"] || false,
                'validators': data["validators"] || []
            };
            return result;
        },

        _attachFieldEventListeners : function (field, eventNames, values) {

            // for each field, this.fieldMap[field_name].element is the container
            // which contains all UI elements for this data. But sometimes the element that is
            // the actual input element is needed to be known (e.g. for validation to highlight it),
            // so this.fieldMap[field_name].inputElement stores that element
            this.fieldMap[field.name].inputElement = field;

            var validateAllFunc = this._validateAllFields.bind(this);

            var _this = this;
            eventNames.each(function(eventName) {

                //-------------------------------------------------------------------------------------------
                // this is the function which is called for each and every change of the value for the field
                // (e.g. radio value i schanged or any single charatcer is added or removed in a text field
                //-------------------------------------------------------------------------------------------
                field.observe(eventName, function(event) {
                    _this._saveCursorPositionIfNecessary(field);
                    if (_this._updating) return; // otherwise a field change triggers an update which triggers field change etc

                    // update the internal representation of the data
                    if (event.hasOwnProperty("memo") && event.memo.hasOwnProperty("useValue")) {
                        _this.fieldMap[field.name].crtValue = event.memo.useValue;
                    } else {
                        _this.fieldMap[field.name].crtValue = field._getValue && field._getValue()[0];
                    }

                    // normally for every change an event is fired so that the change can be immediately reflected
                    // on the pedigree image. In the "noDynamicUpdates" mode the change is stored intenrally but
                    // nothing happens until an explicit update is triggered by some dedicated "submit" button is
                    // pressed (currently the only suported button is a 'create patient' field/button)
                    if (_this._noDynamicUpdates) {

                        // normally in the "noDynamicUpdates" mode no validations are done until the dedicated
                        // "submit" button is pressed. However if vlaidations are forced they are still done
                        // after every single edit
                        if (_this._forceValidation) {
                            validateAllFunc();
                        }

                        // "noDynamicUpdates" mode - nothing else to do
                        return;
                    }

                    var target = _this.targetNode;
                    if (!target) return;
                    if (target.getSummary()[field.name].value == _this.fieldMap[field.name].crtValue) {
                        return;
                    }

                    // validate all fields as they are after this change
                    var allFieldsValid = validateAllFunc();

                    var method = _this.fieldMap[field.name]['function'];

                    // fire an "update" event: either a "pedigree:node:setproperty" or a "pedigree:node:modify",
                    // depending on the name of the function specified for this field ("setXXX" is presumed to
                    // require a "pedigree:node:setproperty" event, everything else a "pedigree:node:modify")
                    // FIXME: this logic is non-obvious and can be improved
                    if (method.indexOf("set") == 0 && typeof(target[method]) == 'function') {
                        var properties = {};
                        properties[method] = _this.fieldMap[field.name].crtValue;
                        // note: properties which fail validation are allowed to be set for technical reasons (since this is the
                        //  only way partial data can be communicated to the rest of pedigree editor in the "yes dynamic updates" mode),
                        //  but the state will be marked as "failedValidation" so that it is excluded from the undo/redo sequence
                        var fireEvent = { "nodeID": target.getID(), "properties": properties, "failedValidation": !allFieldsValid };
                        document.fire("pedigree:node:setproperty", fireEvent);
                    } else {
                        var properties = {};
                        properties[method] = _this.fieldMap[field.name].crtValue;
                        var fireEvent = { "nodeID": target.getID(), "modifications": properties };
                        if (event.memo.hasOwnProperty("useValue") && event.memo.hasOwnProperty("eventDetails")) {
                            fireEvent["details"] = event.memo.eventDetails;
                        }
                        document.fire("pedigree:node:modify", fireEvent);
                    }
                    field.fire('pedigree:change');
                });
            });
        },

        _validateAllFields: function(forceRequiredFieldValidation, forceErrorMessage) {
            var validationFailed = false;

            var hasLinkedRecord = (this.fieldMap["phenotipsid"] && this.fieldMap["phenotipsid"].crtValue);
            var phenotipsRecordID = hasLinkedRecord ? this.fieldMap["phenotipsid"].crtValue : null;

            var tabsWithMissingData = {};

            for (var field in this.fieldMap) {
                if (this.fieldMap.hasOwnProperty(field)) {
                    var fieldData = this.fieldMap[field];
                    var validationFunction = this._validateFieldValue[fieldData.type].bind(this);

                    // only apply "required" field validation for nodes which are linked to PhenoTips records,
                    // or if validation is explicitly requested. If not, validate other stuff but not "required" status
                    if (!hasLinkedRecord && !forceRequiredFieldValidation && !this._forceValidation) {
                        fieldData = Helpers.cloneObject(fieldData);
                        fieldData.required = false;
                    }

                    // only apply "uniqueExternalID" validator to nodes linkied to PhenoTips records
                    if (!hasLinkedRecord && !forceRequiredFieldValidation
                        && fieldData.validators && Helpers.arrayContains(fieldData.validators, "uniqueExternalID")) {
                        fieldData = Helpers.cloneObject(fieldData);
                        fieldData.validators = fieldData.validators.slice();
                        Helpers.removeFirstOccurrenceByValue(fieldData.validators, "uniqueExternalID");
                    }

                    if (!validationFunction(fieldData.inputElement, fieldData, fieldData.crtValue, phenotipsRecordID)) {
                        validationFailed = true;
                        tabsWithMissingData[fieldData.tab] = true;
                    }
                }
            }

            if (validationFailed && (hasLinkedRecord || forceErrorMessage || this._forceValidation)) {
                this._activateInvalidValueScreen(tabsWithMissingData);
            } else {
                this._deactivateInvalidValueScreen();
            }

            return !validationFailed;
        },

        _activateInvalidValueScreen: function(tabsWithMissingData, skipErrorMessage) {
            this.closeButton.addClassName("no-mouse-interaction");
            this.closeButton.addClassName("button-disabled");

            if (!window.getComputedStyle) {
                var invalidBlockerZIndex = document.defaultView.getComputedStyle(this.menuBox,null).getPropertyValue("z-index") - 1;
                this.invalidValueScreen.style.zIndex = invalidBlockerZIndex-1;
            }
            this.invalidValueScreen.style.zIndex = invalidBlockerZIndex-1;
            this.invalidValueScreen.show();

            if (!skipErrorMessage) {
                for (var tab in this.tabs) {
                    if (this.tabs.hasOwnProperty(tab)) {
                        var tabContainer = this.tabs[tab];
                        tabContainer.messageContainer.addClassName("error-message-container");
                        if (tabsWithMissingData.hasOwnProperty(tab)) {
                            tabContainer.messageContainer.update(this._notValidMessageOnCurrentTab);
                        } else {
                            tabContainer.messageContainer.update(this._notValidMessageOnOtherTab);
                        }
                        tabContainer.messageContainer.show();
                    }
                }
            }
        },

        _deactivateInvalidValueScreen: function(removeModalScreen) {
            this.closeButton.removeClassName("no-mouse-interaction");
            this.closeButton.removeClassName("button-disabled");

            if (!this._modalMode || removeModalScreen) {
                this.invalidValueScreen.hide();
            }

            for (var tab in this.tabs) {
                if (this.tabs.hasOwnProperty(tab)) {
                    var tabContainer = this.tabs[tab];
                    tabContainer.messageContainer.removeClassName("error-message-container");
                    if (!tabContainer.hasOwnProperty("tabMessage")) {
                        tabContainer.messageContainer.hide();
                    } else {
                        tabContainer.messageContainer.addClassName("info-message-container");
                        tabContainer.messageContainer.update(tabContainer.tabMessage);
                        tabContainer.messageContainer.show();
                    }
                }
            }
        },

        _saveCursorPositionIfNecessary: function(field) {
            this.__lastSelectedField  = field.name;
            this.__lastNodeID         = this.targetNode;
            // for text fields in all browsers, and textarea only in IE
            var isIE = navigator && navigator.appVersion.indexOf('Trident/') > -1;
            if (field.type == "text" || (isIE && field.type == "textarea")) {
                this.__lastCursorPosition = GraphicHelpers.getCaretPosition(field);
            }
        },

        _restoreCursorPositionIfNecessary: function(field) {
            if (this.targetNode == this.__lastNodeID &&
                field.name      == this.__lastSelectedField) {
                // for text fields in all browsers, and textarea only in IE
                if (this.__lastCursorPosition !== undefined && (field.type == "text" || field.type == "textarea")) {
                    GraphicHelpers.setCaretPosition(field, this.__lastCursorPosition);
                    this.__lastCursorPosition = undefined;
                }
            }
        },

        update: function(newTarget) {
            //console.log("Node menu: update");
            if (newTarget) {
                this.targetNode = newTarget;
            }

            if (this.targetNode) {
                this._updating = true;   // needed to avoid infinite loop: update -> _attachFieldEventListeners -> update -> ...
                this._setCrtData(this.targetNode.getSummary());
                if (newTarget) {
                    this.reposition();
                }
                delete this._updating;
            }

            this._validateAllFields();
        },

        _attachDependencyBehavior : function(field, data) {
            if (data.dependency) {
                var dependency = data.dependency.split(' ', 3);
                var element = this.fieldMap[dependency[0]].element;
                dependency[0] = this.form[dependency[0]];
                element.inputsContainer.insert(field.up());
                this.fieldMap[field.name].element = element;
                this._updatedDependency(field, dependency);
                dependency[0].observe('pedigree:change', function() {
                    this._updatedDependency(field, dependency);
                    field.value = '';
                    //console.log("dependency observed: " + field + ", dep: " + dependency);
                }.bindAsEventListener(this));
            }
        },

        _updatedDependency : function (field, dependency) {
            switch (dependency[1]) {
                case '!=':
                    field.disabled =  (dependency[0].value == dependency[2]);
                    break;
                default:
                    field.disabled =  (dependency[0].value == dependency[2]);
                    break;
            }
        },

        _generateField : {
            'radio' : function (data) {
                var result = this._generateEmptyField(data);
                var columnClass = data.columns ? "field-values-" + data.columns + "-columns" : "field-values";
                columnClass += " field-no-user-select";
                var values = new Element('div', {'class' : columnClass});
                result.inputsContainer.insert(values);
                var _this = this;
                var _generateRadioButton = function(v) {
                    var radioLabel = new Element('label', {'class' : data.name + '_' + v.actual}).update(v.displayed);
                    if (v.hasOwnProperty("columnshiftPX")) {
                        radioLabel.setStyle({"marginLeft": "" + v.columnshiftPX + "px"});
                    }
                    var radioButton = new Element('input', {type: 'radio', name: data.name, value: v.actual});
                    radioLabel.insert({'top': radioButton});
                    radioButton._getValue = function() { return [this.value]; }.bind(radioButton);
                    values.insert(radioLabel);
                    _this._attachFieldEventListeners(radioButton, ['click']);
                    _this._attachDependencyBehavior(radioButton, data);
                };
                if (data.hasOwnProperty("valuesIE9") && navigator && navigator.appVersion.indexOf("MSIE 9") != -1) {
                    data.valuesIE9.each(_generateRadioButton);
                } else {
                    data.values.each(_generateRadioButton);
                }

                return result;
            },
            'checkbox' : function (data) {
                var result = this._generateEmptyField(data);
                var checkbox = new Element('input', {type: 'checkbox', name: data.name, value: '1'});
                result.down('label').insert({'top': checkbox});
                checkbox._getValue = function() { return [this.checked];}.bind(checkbox);
                this._attachFieldEventListeners(checkbox, ['click']);
                return result;
            },
            'text' : function (data) {
                var result = this._generateEmptyField(data);
                // disable enter for text fields to avoid submitting the form and closing the pedigree editor on enter
                var text = new Element('input', {type: 'text', name: data.name, onkeypress:'return event.keyCode != 13;'});
                if (data.tip) {
                    text.placeholder = data.tip;
                }
                result.inputsContainer.insert(text);
                var span = text.wrap('span');
                var inputErrorDescription = new Element('span', {'class': 'text-field-input-error'});
                inputErrorDescription.hide();
                span.insert(inputErrorDescription);
                text._getValue = function() { return [this.value]; }.bind(text);
                //this._attachFieldEventListeners(text, ['keypress', 'keyup'], [true]);
                this._attachFieldEventListeners(text, ['keyup'], [true]);
                this._attachDependencyBehavior(text, data);
                return result;
            },
            'textarea' : function (data) {
                var result = this._generateEmptyField(data);
                var properties = {name: data.name};
                properties["class"] = "textarea-"+data.rows+"-rows"; // for compatibiloity with older browsers not accepting {class: ...}
                var text = new Element('textarea', properties);
                result.inputsContainer.insert(text);
                //text.wrap('span');
                text._getValue = function() { return [this.value]; }.bind(text);
                this._attachFieldEventListeners(text, ['keyup'], [true]);
                this._attachDependencyBehavior(text, data);
                return result;
            },
            'date-picker' : function (data) {
                var result = this._generateEmptyField(data);
                var datePicker = new Element('input', {type: 'text', 'class': 'fuzzy-date', name: data.name, 'title': data.format || '', alt : '' });
                datePicker._getValue = function() { /*console.log("DATE UPDATE: " + this.value);*/ return [new PedigreeDate(JSON.parse(this.value))]; }.bind(datePicker);
                this._attachFieldEventListeners(datePicker, ['xwiki:date:changed']);

                var inputErrorDescription = new Element('span', {'class': 'date-field-input-error'});
                inputErrorDescription.hide();

                result.inputsContainer.insert(datePicker);
                result.insert(inputErrorDescription);
                return result;
            },
            'disease-picker' : function (data) {
                var result = this._generateEmptyField(data);
                var diseasePicker = new Element('input', {type: 'text', 'class': 'suggest multi suggest-omim', name: data.name});
                result.insert(diseasePicker);
                diseasePicker._getValue = function() {
                    var results = [];
                    var container = this.up('.field-box');
                    if (container) {
                        container.select('input[type=hidden][name=' + data.name + ']').each(function(item){
                            results.push(new Disorder(item.value, item.next('.value') && item.next('.value').firstChild.nodeValue || item.value));
                        });
                    }
                    return [results];
                }.bind(diseasePicker);
                // Forward the 'custom:selection:changed' to the input
                var _this = this;
                document.observe('custom:selection:changed', function(event) {
                    if (event.memo && event.memo.fieldName == data.name && event.memo.trigger && event.findElement() != event.memo.trigger && !event.memo.trigger._silent) {
                        Event.fire(event.memo.trigger, 'custom:selection:changed');
                        _this.reposition();
                    }
                });
                this._attachFieldEventListeners(diseasePicker, ['custom:selection:changed']);
                return result;
            },
            'ethnicity-picker' : function (data) {
                var result = this._generateEmptyField(data);
                var ethnicityPicker = new Element('input', {type: 'text', 'class': 'suggest multi suggest-ethnicity', name: data.name});
                result.insert(ethnicityPicker);
                ethnicityPicker._getValue = function() {
                    var results = [];
                    var container = this.up('.field-box');
                    if (container) {
                        container.select('input[type=hidden][name=' + data.name + ']').each(function(item){
                        results.push(item.next('.value') && item.next('.value').firstChild.nodeValue || item.value);
                        });
                    }
                    return [results];
                }.bind(ethnicityPicker);
                // Forward the 'custom:selection:changed' to the input
                var _this = this;
                document.observe('custom:selection:changed', function(event) {
                    if (event.memo && event.memo.fieldName == data.name && event.memo.trigger && event.findElement() != event.memo.trigger && !event.memo.trigger._silent) {
                        Event.fire(event.memo.trigger, 'custom:selection:changed');
                        _this.reposition();
                    }
                });
                this._attachFieldEventListeners(ethnicityPicker, ['custom:selection:changed']);
                return result;
            },
            'hpo-picker' : function (data) {
                var result = this._generateEmptyField(data);
                var hpoPicker = new Element('input', {type: 'text', 'class': 'suggest multi suggest-hpo', name: data.name});
                result.insert(hpoPicker);
                hpoPicker._getValue = function() {
                    var results = [];
                    var container = this.up('.field-box');
                    if (container) {
                        container.select('input[type=hidden][name=' + data.name + ']').each(function(item){
                        results.push(new HPOTerm(item.value, item.next('.value') && item.next('.value').firstChild.nodeValue || item.value));
                        });
                    }
                    return [results];
                }.bind(hpoPicker);
                // Forward the 'custom:selection:changed' to the input
                var _this = this;
                document.observe('custom:selection:changed', function(event) {
                    if (event.memo && event.memo.fieldName == data.name && event.memo.trigger && event.findElement() != event.memo.trigger && !event.memo.trigger._silent) {
                        Event.fire(event.memo.trigger, 'custom:selection:changed');
                        _this.reposition();
                    }
                });
                this._attachFieldEventListeners(hpoPicker, ['custom:selection:changed']);
                return result;
            },
            'cancers-picker' : function (data) {
                var result = this._generateEmptyField(data);
                var cancersPicker = new Element('input', {'type': 'text', 'class': 'suggest suggest-cancers accept-value', 'name': data.name});
                result.insert(cancersPicker);
                cancersPicker._getValue = function() {
                    var results = [];
                    var container = this.up('.field-box');
                    if (container) {
                        container.select('input[type=hidden][name=' + data.name + ']').each(function(item) {
                            results.push(new HPOTerm(item.value, item.next('.value') && item.next('.value').firstChild.nodeValue || item.value));
                        });
                    }
                    return [results];
                }.bind(cancersPicker);
                return result;
            },
            'submit-cancel' : function(data) {
                this.closeButton.hide();
                var _this = this;

                if (data.not_valid_message) {
                    this._notValidMessageOnCurrentTab = data.not_valid_message;
                }

                if (data.tab_message && data.tab) {
                    if (this.tabs.hasOwnProperty(data.tab)) {
                        this.tabs[data.tab].tabMessage = data.tab_message;
                    }
                }

                var newPatientButton = new Element('span', {'class': 'patient-menu-button patient-create-new-ok-button field-no-user-select'}).update("Create new patient");
                newPatientButton.observe("click", function() {
                    if (!_this._validateAllFields(true, true)) {
                        // after one failed validation force re-validation after every input
                        _this._forceValidation = true;
                        return;
                    }

                    var onPatientCreated = function(newID) {
                        // a newly linked patient record has no pedigree-specific properties yet
                        var pedigreeProperties = {};
                        editor.getPatientLegend().addCase(newID, pedigreeProperties);
                    };

                    var pedigreeData = _this._getDataForNewPatient();
                    document.fire("pedigree:patient:createrequest", {"onCreatedHandler": onPatientCreated, "patientData": pedigreeData } );
                    _this.hide();
                });

                var cancelButton = new Element('span', {'class': 'patient-menu-button patient-create-cancel-button field-no-user-select'}).update("Cancel");
                cancelButton.observe("click", function() {
                    _this.hide();
                });

                var result = this._generateEmptyField(data);
                result.inputsContainer.insert(newPatientButton).insert(cancelButton);
                return result;
            },
            'phenotipsid-picker' : function (data) {
                var result = this._generateEmptyField(data);
                result.addClassName("pedigree_family_record_ui");
                var patientNewLinkContainer = new Element('div', { 'class': 'patient-newlink-container'});
                var patientPicker = new Element('input', {type: 'text', 'class': 'suggest suggest-patients', name: data.name});
                var newPatientButton = new Element('span', {'class': 'patient-menu-button patient-create-button field-no-user-select'}).update("Create new");
                var _this = this;
                newPatientButton.observe('click', function(event) {
                    // force re-validation of all fields - to display red borders around incomplete required fields
                    if (!_this._validateAllFields(true)) {
                        var afterDialogClosed = function() {
                            // re-validate again (this time without forcing) to dismiss red borders -
                            // they are not relevant when patient record is not linked
                            _this._validateAllFields();
                        };

                        editor.getOkCancelDialogue().showError("Can not create a new patient until all the required (highlighted) fields are populated",
                                "Please populate all the required fields", "OK", afterDialogClosed);
                        return;
                    }

                    var setDoNotShow = function(checkBoxStatus) {
                        if (checkBoxStatus) {
                            editor.getPreferencesManager().setConfigurationOption("user", "hideShareConsentDialog", true);
                        }
                    };

                    var _onPatientCreated = function(newID, patientJSON) {
                        Event.fire(patientPicker, 'custom:selection:changed', { "useValue": newID, "eventDetails": {"loadPatientProperties": false, "skipConfirmDialogue" : true} });
                        Event.fire(patientPicker, 'pedigree:patient:created', { "phenotipsPatientID": newID });
                        _this.reposition();
                    }

                    var processCreatePatient = function() {
                        var pedigreeData = _this._getDataForNewPatient();
                        document.fire("pedigree:patient:createrequest", {"onCreatedHandler": _onPatientCreated, "patientData": pedigreeData } );
                    }

                    if (!editor.getPreferencesManager().getConfigurationOption("hideShareConsentDialog")) {
                        var processLinking = function(topMessage, notesMessage) {
                            editor.getOkCancelDialogue().showWithCheckbox("<br/><b>" + topMessage + "</b><br/>" +
                                "<div style='margin-left: 30px; margin-right: 30px; text-align: left'>Please note that:<br/><br/>"+
                                notesMessage + "</div>",
                                "Add patient to the family?",
                                "Do not show this warning again<br/>", false,
                                "Confirm", function(checkBoxStatus) { setDoNotShow(checkBoxStatus); processCreatePatient() },
                                "Cancel",  function(checkBoxStatus) { setDoNotShow(checkBoxStatus); });
                        }

                        processLinking("When you create a new patient and add to this family:<br/>",
                                "1) A copy of this pedigree will be placed in the electronic record of each family member.<br/><br/>"+
                                "2) This pedigree can be edited by any user with access to any member of the family.");
                    } else {
                        processCreatePatient();
                    }

                    _this.reposition();
                });
                patientNewLinkContainer.insert(patientPicker).insert("&nbsp;&nbsp;&nbsp;or").insert(newPatientButton);
                result.insert(patientNewLinkContainer);

                var patientLinkContainer = new Element('div', { 'class': 'patient-link-container'});
                var patientLink = new Element('a', {'class': 'patient-link-url', 'target': "_blank", name: data.name + "_link"});
                var removeLink = new Element('span', {'class': 'patient-menu-button patient-link-remove'});
                removeLink.observe('click', function(event) {
                    Event.fire(patientPicker, 'custom:selection:changed', { "useValue": "" });
                    _this.reposition();
                });
                removeLink.insert("Unlink");
                patientLinkContainer.insert(patientLink).insert(removeLink);
                result.insert(patientLinkContainer);
                var _this = this;
                patientPicker.observe('ms:suggest:selected', function(event) {
                     //Event.fire(patientPicker, 'custom:selection:changed', { "useValue": { "phenotipsid": event.memo.id } });
                    Event.fire(patientPicker, 'custom:selection:changed', { "useValue": event.memo.id });
                    _this.reposition();
                });
                this._attachFieldEventListeners(patientPicker, ['custom:selection:changed']);
                return result;
            },
            'gene-picker' : function (data) {
                var result = this._generateEmptyField(data);
                result.setAttribute("legendName", data.legendName);
                var genePicker = new Element('input', {type: 'text', 'class': 'suggest multi suggest-genes', name: data.name});
                result.insert(genePicker);
                genePicker._getValue = function() {
                    var results = [];
                    var container = this.up('.field-box');
                    if (container) {
                        container.select('input[type=hidden][name=' + data.name + ']').each(function(item){
                            results.push({"id": item.value, "gene": item.next('.value') && item.next('.value').firstChild.nodeValue || item.value});
                        });
                    }
                    return [results];
                }.bind(genePicker);
                // Forward the 'custom:selection:changed' to the input
                var _this = this;
                document.observe('custom:selection:changed', function(event) {
                    if (event.memo && event.memo.fieldName == data.name && event.memo.trigger && event.findElement() != event.memo.trigger && !event.memo.trigger._silent) {
                        Event.fire(event.memo.trigger, 'custom:selection:changed');
                        _this.reposition();
                    }
                });
                this._attachFieldEventListeners(genePicker, ['custom:selection:changed']);
                return result;
            },
            'button' : function (data ) {
                var result = this._generateEmptyField(data);
                var buttonContainer = new Element('div', { 'class': 'button-container'});
                var classes = 'patient-menu-button patient-generic-button';
                if (data.buttoncss) {
                    classes += " " + data.buttoncss;
                }
                var button = new Element('span', {'class': classes}).update(data.label);
                // using nameHolder below is a workaround: existing code uses field.name ot get the name -
                // but DIVs and SPANs can't have "name" attribute
                var nameHolder = new Element('input', {'type': 'hidden', 'name': data.name});
                var _this = this;
                button.observe('click', function(event) {
                        Event.fire(nameHolder, 'custom:selection:changed', { "useValue": "" });
                        _this.reposition();
                    });
                this._attachFieldEventListeners(nameHolder, ['custom:selection:changed']);
                buttonContainer.update(button).insert(nameHolder);;
                result.down('label').update(buttonContainer);
                return result;
            },
            'select' : function (data) {
                var result = this._generateEmptyField(data);
                var span = new Element('span');
                // using raw HTML for options for performace reasons: generating e.g. 50 different gestation week
                // options is noticeably slow when using more generic methods (e.g. new Element("option"))
                var optionHTML = '<select name="' + data.name + '">';
                var _generateSelectOption = function(v) {
                    optionHTML += '<option value="' + v.actual + '">' + v.displayed + '</option>';
                };
                if(data.nullValue) {
                    _generateSelectOption({'actual' : '', displayed : '-'});
                }
                if (data.values) {
                    data.values.each(_generateSelectOption);
                } else if (data.range) {
                    var createSelectionLabel = function(i, data) {
                        if (data.range.replacementLabels && data.range.replacementLabels.hasOwnProperty(i)) {
                            return data.range.replacementLabels[i];
                        }
                        return i + (data.range.labelSuffix ? (' ' + data.range.labelSuffix[+(i!=data.range.start)]) : '');
                    }
                    $A($R(data.range.start, data.range.end)).each(function(i) {_generateSelectOption({'actual': i, 'displayed' : createSelectionLabel(i, data)})});
                }
                optionHTML += "</select>";
                span.innerHTML = optionHTML;
                select = span.firstChild;
                result.inputsContainer.insert(span);
                select._getValue = function() { return [(this.selectedIndex >= 0) && this.options[this.selectedIndex].value || '']; }.bind(select);
                this._attachFieldEventListeners(select, ['change']);
                return result;
            },
            'cancerlist' : function (data) {
                //var timer = new Helpers.Timer();
                var result = this._generateEmptyField(data);
                var cancerLegend = editor.getCancerLegend();
                var cancerList = cancerLegend._getAllSupportedCancers();
                var table = new Element('table', {'id' : data.name + '_data_table'});

                // Add all the cancers displayed by default.
                for (var i = 0; i < cancerList.length; i++) {
                    var cancerId = cancerList[i];
                    var cancerName = cancerLegend.getCancer(cancerId).getName();
                    this._addNewCancerRow(cancerId, cancerName, table, true, [
                        data.name + ':term:cleared',
                        data.name + ':dialog:deleted',
                        data.name + ':dialog:added',
                        data.name + ':notes:updated',
                        'change'
                    ]);
                    result.inputsContainer.insert(table);
                }

                var _this = this;
                // Watch for any custom cancers being added.
                this.form.observe('ms:suggest:selected', function(event) {
                    if (!event.memo) { return; }
                    if (event.target.hasClassName('suggest-cancers')) {
                        event.stop();
                        var cancerId = event.memo.id;
                        // Try to find the element in the form. If not already there, create a new one.
                        var qualifiers = _this.form.down('table[id="' + data.name + '_' + cancerId + '"]')
                            || _this._addNewCancerRow.bind(_this)(cancerId, event.memo.value, table, false, [
                                data.name + ':term:deleted',
                                data.name + ':term:cleared',
                                data.name + ':dialog:deleted',
                                data.name + ':dialog:added',
                                data.name + ':notes:updated',
                                'change'
                            ]);
                        // If the cancer is not yet marked as affected, initialise affected, otherwise do nothing.
                        var statusElem = qualifiers.down('input[id="status_' + cancerId + '"]');
                        statusElem && !statusElem.checked && qualifiers._widget.initAffected(false);
                        event.target._suggest.clearSuggestions();
                        event.target.value = '';
                    }
                });

                [data.name + ':dialog:focused', data.name + ':dialog:added'].each(function(eventName) {
                    _this.form.observe(eventName, function() {
                        _this.reposition();
                    });
                });
                return result;
            },
            'hidden' : function (data) {
                var result = this._generateEmptyField(data);
                result.addClassName('hidden');
                var input = new Element('input', {type: 'hidden', name: data.name, value: ''});
                result.update(input);
                return result;
            }
        },

        _addNewCancerRow: function(cancerId, label, table, disableDelete, toObserve) {
            var dataName = "cancers";
            var tableRow = new Element('tr', {'class': 'cancer_field'} );
            var qualifiers = this._buildNewCancerElement(cancerId, label, dataName,
              {'allowMultiDialogs': true, 'disableTermDelete': disableDelete});
            this._setCancersValueFx(qualifiers);
            this._attachFieldEventListeners(qualifiers, toObserve);
            table.insert(tableRow.insert(qualifiers));
            return qualifiers;
        },

        _setCancersValueFx: function(cancerElem) {
            var _this = this;
            cancerElem._getValue = function() {
                var data = [];
                _this.form.select('table.cancers-summary-group').each(function(qualifier) {
                    var cancerWidget = qualifier._widget;
                    if (cancerWidget.isAffected()) {

                        var getBestNumericAgeApproximation = function(value) {
                            if (!value) {
                                return "";
                            }
                            if (isNaN(parseInt(value))) {
                                if (value.indexOf('after_') > -1) {
                                    return parseInt(value.replace('after_', '')) + 1;
                                }
                                if (value == "before_1") {
                                    return 0;
                                }
                                return parseInt(value.replace('before_', '')) - 9;
                            }
                            return parseInt(value);
                        }

                        var cancerData = cancerWidget.getValues();
                        cancerData.qualifiers.forEach(function(qualifier) {
                            qualifier.numericAgeAtDiagnosis = getBestNumericAgeApproximation(qualifier.ageAtDiagnosis);
                        });
                        data.push(cancerData);
                    }
                });
                return [ data ];
            };
        },

        _buildNewCancerElement: function(cancerId, cancerName, dataName, optionsParam) {
            var toStoredLateralityMap = {"Bilateral" : "bi", "Unilateral" : "u", "Right" : "r", "Left" : "l"};
            var toDisplayedLateralityMap = {"bi" : "Bilateral", "u" : "Unilateral", "r" : "Right", "l" : "Left"};

            var toStoredAgeFx = function(value) {
                if (isNaN(parseInt(value))) {
                    if (!value) {
                        return "";
                    }
                    if (value.indexOf('>') > -1) {
                        return value.replace('>', 'after_');
                    }
                    if (value.indexOf('<') > -1) {
                        return value.replace('<', 'before_');
                    }
                } else {
                    var rangeIdx = value.indexOf('-');
                    if (rangeIdx > -1) {
                          return "before_" + value.substring(rangeIdx + 1);
                    }
                    return value;
                }
            };

            var toDisplayedAgeFx = function(value) {
                if (value && isNaN(parseInt(value))) {
                    if (value.indexOf('after_') > -1) {
                        return value.replace('after_', '>');
                    }
                    if (value == "before_1") {
                        return value.replace('before_', '<');
                    }
                    var to = parseInt(value.substring(7));
                    var from = to - 9;
                    return from + "-" + to;
                } else {
                    return value;
                }
            };

            var toStoredLateralityFx = function(value) {
                return toStoredLateralityMap[value] || "";
            };

            var toDisplayedLateralityFx = function(value) {
                return toDisplayedLateralityMap[value] || "Unknown";
            };

            var toStoredTypeFx = function(value) {
                return value === "Primary";
            };

            var toDisplayedTypeFx = function(value) {
                return value ? "Primary" : "Mets";
            };
            var qualifiersWidget = new PhenoTips.widgets.DetailsDialogGroup(dataName, optionsParam)
                .withLabel(cancerName, cancerId, 'phenotype-info')
                .dialogsAddDeleteAction()
                .dialogsAddNumericSelect({
                    'from': 1,
                    'to': 100,
                    'majorStepSize' : 10,
                    'step': 1,
                    'defListItemClass': 'age_of_onset',
                    'inputSourceClass': 'cancer_age_select field-no-user-select',
                    'qualifierLabel': 'Age:',
                    'qualifierName': 'ageAtDiagnosis',
                    'displayedToStoredMapper': toStoredAgeFx,
                    'storedToDisplayedMapper': toDisplayedAgeFx})
                .dialogsAddItemSelect({
                    'data': ['Unknown', 'Bilateral', 'Unilateral', 'Right', 'Left'],
                    'defListItemClass': 'laterality',
                    'inputSourceClass': 'cancer_laterality_select',
                    'qualifierLabel': 'Laterality:',
                    'qualifierName': 'laterality',
                    'displayedToStoredMapper': toStoredLateralityFx,
                    'storedToDisplayedMapper': toDisplayedLateralityFx})
                .dialogsAddItemSelect({
                    'data': ['Primary', 'Mets'],
                    'defListItemClass': 'type',
                    'inputSourceClass': 'cancer_type_select',
                    'qualifierLabel': 'Type:',
                    'qualifierName': 'primary',
                    'displayedToStoredMapper': toStoredTypeFx,
                    'storedToDisplayedMapper': toDisplayedTypeFx})
                .dialogsAddTextBox(false, {
                    'defListItemClass': 'comments',
                    'inputSourceClass': 'cancer_notes',
                    'qualifierLabel': 'Notes:',
                    'qualifierName': 'notes'});
            var qualifiers = qualifiersWidget.get();
            qualifiers._widget = qualifiersWidget;
            var focus = qualifiersWidget._focus.bind(qualifiersWidget);
            qualifiersWidget._focus = function(elem) {
                if (!qualifiers.disabled) {
                    focus(elem);
                }
            };
            return qualifiers;
        },

        /**
         * Displays the dialogue (which is hidden otherwise by default).
         *
         * @param {Object} node: an object which should have a `getSummary()` method defined which is used to populate the inputs
         *
         *      if defined, an `onWidgetHide()` method of the node object is used when this dialog is closed
         *
         *      if defined, a `getProperties()` method is used when creating a new patient based on data entered in this dialog
         *
         *      if defined, and if not in "noDynamicUpdates" mode (by default), getID() method is used to specify node id
         *      when firing "pedigree:node:modify" or "pedigree:node:setproperty" events for fields that have
         *      a setter function defined (see this._attachFieldEventListeners()).
         */
        show : function(node, x, y) {
            var me = this;
            this._justOpened = true;
            setTimeout(function() { me._justOpened = false; }, 150);

            this._forceValidation = false;

            this._onscreen = true;
            //console.log("nodeMenu show");
            this.targetNode = node;
            this._setCrtData(this.targetNode.getSummary());

            this.menuBox.show();
            this.reposition(x, y);
            document.observe('mousedown', this._onClickOutside);
            this._validateAllFields();
            if (this._modalMode) {
                this._activateInvalidValueScreen({}, true);
            }
        },

        hide : function() {
            if (this._justOpened || !this._onscreen) {
                return;
            }
            this.hideSuggestPicker();
            this._onscreen = false;
            //console.log("nodeMenu hide");
            document.stopObserving('mousedown', this._onClickOutside);
            if (this.targetNode && this.targetNode.onWidgetHide) {
                this.targetNode.onWidgetHide();
            }
            this.targetNode = undefined;
            this.menuBox.hide();
            this._clearCrtData();
            this._deactivateInvalidValueScreen(true);
        },

        hideSuggestPicker: function() {
            this.form.select('input.suggest').each(function(item) {
                if (item._suggest) {
                    item._suggest.clearSuggestions();
                }
            });
        },

        // FIXME: it is better to reuse the standard way of converting node menu data to pedigree data;
        //        however when creating a new patient which is not represented by a Person-type pedigree node
        //        the standard way does not work (and needs a lot of refactoring to work)
        //        (note: this method generally does not work for non-trivial data types, e.g. genes/HPO terms/cancers)
        _getDataForNewPatient: function() {
            var includedFields = {};
            var pedigreeJSON = {};
            var nodeMenuToPedigree = { "first_name": "fName",
                                       "last_name": "lName",
                                       "gender": "gender",
                                       "external_id": "externalID",
                                       "date_of_birth": "dob",
                                       "date_of_death": "dod",
                                       "state": "lifeStatus" };

            for (var field in this.fieldMap) {
                if (this.fieldMap.hasOwnProperty(field)) {
                    if (this.fieldMap[field].required && this.fieldMap[field].crtValue != "" && this.fieldMap[field].crtValue != null) {
                        includedFields[field] = this.fieldMap[field].crtValue;
                        pedigreeJSON[nodeMenuToPedigree[field]] = this.fieldMap[field].crtValue;
                    }
                }
            }

            // if data is backed by a node which supports getProperties() use that instead of the hacki-sh conversion method
            if (this.targetNode && this.targetNode.getProperties) {
                pedigreeJSON = this.targetNode.getProperties();
            }

            return { "includedFields": includedFields, "pedigreeJSON": pedigreeJSON };
        },

        getDialogWidth: function() {
            if (!this._onscreen) {
                this.menuBox.show();
                var width = this.menuBox.offsetWidth;
                this.menuBox.hide();
                return width;
            }

            return this.menuBox.offsetWidth;
        },

        isVisible: function() {
            return this._onscreen;
        },

        _onClickOutside: function (event) {
            //console.log("nodeMenu clickoutside");
            if (!event.findElement('.suggestItems')) {
                this.hideSuggestPicker();
            }
            if (!event.findElement('.menu-box')
                && !event.findElement('.msdialog-box')
                && !event.findElement('.suggestItems')
                && !event.findElement('.ok-cancel-dialogue')
                && !event.findElement('.msdialog-screen')
                && !event.findElement('.invalidValueScreen')) {
                this.hide();
            }
        },

        reposition : function(x, y) {
            x = Math.floor(x);
            if (x !== undefined && isFinite(x)) {
                if (this.canvas && x + this.menuBox.getWidth() > (this.canvas.getWidth() + 10)) {
                    var delta = x + this.menuBox.getWidth() - this.canvas.getWidth();
                    editor.getWorkspace().panByX(delta, true);
                    x -= delta;
                }
                this.menuBox.style.left = x + 'px';
            }

            this.menuBox.style.height = '';
            var height = '';
            var top    = '';
            if (y !== undefined && isFinite(y)) {
                y = Math.floor(y);
            } else {
                if (this.menuBox.style.top.length > 0) {
                    try {
                        y  = parseInt(this.menuBox.style.top.match( /^(\d+)/g )[0]);
                    } catch (err) {
                        // ignore: strange style or negative y, y will b set to 0
                    }
                }
            }
            if (y === undefined || !isFinite(y) || y < 0) {
                y = 0;
            }

            // Make sure the menu fits inside the screen
            if (this.canvas && this.menuBox.getHeight() >= (this.canvas.getHeight() - 1)) {
                // menu is too big to fit the screen
                top    = 0;
                height = (this.canvas.getHeight() - 1) + 'px';
            } else if (this.canvas.getHeight() < y + this.menuBox.getHeight() + 1) {
                // menu fits the screen, but have to move it higher for that
                var diff = y + this.menuBox.getHeight() - this.canvas.getHeight() + 1;
                var position = (y - diff);
                if (position < 0) {
                    top    = 0;
                    height = (this.canvas.getHeight() - 1) + 'px';
                } else {
                    top    = position + 'px';
                    height = '';
                }
            } else {
                top = y + 'px';
                height = '';
            }

            this.menuBox.style.top      = top;
            this.menuBox.style.height   = height;
            this.menuBox.style.overflow = 'auto';
        },

        _clearCrtData : function () {
            var _this = this;
            Object.keys(this.fieldMap).each(function (name) {
                _this.fieldMap[name].crtValue = _this.fieldMap[name]["default"];
                _this._setFieldValue[_this.fieldMap[name].type].call(_this, _this.fieldMap[name].element, _this.fieldMap[name].crtValue);
                _this.fieldMap[name].inactive = false;
            });
        },

        _setCrtData : function (data) {
            var _this = this;
            Object.keys(this.fieldMap).each(function (name) {
                _this.fieldMap[name].crtValue = data && data[name] && typeof(data[name].value) != "undefined" ? data[name].value : _this.fieldMap[name].crtValue || _this.fieldMap[name]["default"];
                _this.fieldMap[name].inactive = (data && data[name] && (typeof(data[name].inactive) == 'boolean' || typeof(data[name].inactive) == 'object')) ? data[name].inactive : _this.fieldMap[name].inactive;
                _this.fieldMap[name].disabled = (data && data[name] && (typeof(data[name].disabled) == 'boolean' || typeof(data[name].disabled) == 'object')) ? data[name].disabled : false;
                _this._setFieldValue[_this.fieldMap[name].type].call(_this, _this.fieldMap[name].element, _this.fieldMap[name].crtValue, data);
                _this._setFieldInactive[_this.fieldMap[name].type].call(_this, _this.fieldMap[name].element, _this.fieldMap[name].inactive);
                _this._setFieldDisabled[_this.fieldMap[name].type].call(_this, _this.fieldMap[name].element, _this.fieldMap[name].disabled, _this.fieldMap[name].inactive, _this.fieldMap[name].crtValue);
                //_this._updatedDependency(_this.fieldMap[name].element, _this.fieldMap[name].element);
            });
        },

        _setFieldValue : {
            'radio' : function (container, value) {
                var target = container.down('input[type=radio][value="' + value + '"]');
                if (target) {
                    target.checked = true;
                }
            },
            'checkbox' : function (container, value) {
                var checkbox = container.down('input[type=checkbox]');
                if (checkbox) {
                    checkbox.checked = value;
                }
            },
            'text' : function (container, value) {
                var target = container.down('input[type=text]');
                if (target) {
                    target.value = value;
                    this._restoreCursorPositionIfNecessary(target);
                }
            },
            'textarea' : function (container, value) {
                var target = container.down('textarea');
                if (target) {
                    target.value = value;
                }
                this._restoreCursorPositionIfNecessary(target);
            },
            'date-picker' : function (container, value) {
                if (!value) {
                    value = {};
                }

                var range = value.hasOwnProperty("range") && value.range.hasOwnProperty("years") ? value.range.years : 1;
                var year  = value.hasOwnProperty("year") && value.year ? value.year.toString() : "";
                if (range > 1 && year != "") {
                    year = year + "s";
                }
                var month = "";
                var day   = "";

                var dateEditFormat = editor.getPreferencesManager().getConfigurationOption("dateEditFormat");
                var dmyInputMode = (dateEditFormat == "DMY" || dateEditFormat == "MY");
                if ((dmyInputMode || value.year) && value.month) {
                    month = value.hasOwnProperty("month") ? value.month.toString() : "";
                }
                if ((dmyInputMode || (value.year && value.month)) && value.day) {
                    day = value.hasOwnProperty("day") ? value.day.toString() : "";
                }

                var updated = false;
                var yearSelect = container.down('select.year');
                if (yearSelect) {
                    var option = yearSelect.down('option[value="' + year + '"]');
                    if (!option) {
                        option = new Element("option", {"value": year}).update(year.toString());
                        yearSelect.insert(option);
                    }
                    if (option && !option.selected) {
                        option.selected = true;
                        updated = true;
                    }
                }
                var monthSelect = container.down('select.month');
                if (monthSelect) {
                    var option = monthSelect.down('option[value="' + month + '"]');
                    if (option && !option.selected) {
                        option.selected = true;
                        updated = true;
                    }
                }
                var daySelect = container.down('select.day');
                if (daySelect) {
                    var option = daySelect.down('option[value="' + day + '"]');
                    if (option && !option.selected) {
                        option.selected = true;
                        updated = true;
                    }
                }
                // TODO: replace the code above with an even request to change year-month-date
                if (updated) {
                    var updateElement = container.down('.fuzzy-date-picker');
                    if (updateElement) {
                        Event.fire(updateElement, 'datepicker:date:changed');
                    }
                }
            },
            'disease-picker' : function (container, values) {
                var _this = this;
                var target = container.down('input[type=text].suggest-omim');
                if (target && target._suggestPicker) {
                    target._silent = true;
                    target._suggestPicker.clearAcceptedList();
                    if (values) {
                        values.each(function(v) {
                            target._suggestPicker.addItem(v.id, v.value, '');
                            _this._updateDisorder(v.id, editor.getDisorderLegend().getObjectColor(v.id), v.value);
                        })
                    }
                    target._silent = false;
                    target.value = '';
                }
            },
            'ethnicity-picker' : function (container, values) {
                var _this = this;
                var target = container.down('input[type=text].suggest-ethnicity');
                if (target && target._suggestPicker) {
                    target._silent = true;
                    target._suggestPicker.clearAcceptedList();
                    if (values) {
                        values.each(function(v) {
                            target._suggestPicker.addItem(v, v, '');
                        })
                    }
                    target._silent = false;
                    target.value = '';
                }
            },
            'hpo-picker' : function (container, values) {
                var _this = this;
                var target = container.down('input[type=text].suggest-hpo');
                if (target && target._suggestPicker) {
                    target._silent = true;
                    target._suggestPicker.clearAcceptedList();
                    if (values) {
                        values.each(function(v) {
                            target._suggestPicker.addItem(v.id, v.value, '');
                        })
                    }
                    target._silent = false;
                    target.value = '';
                }
            },
            'cancers-picker' : function (container, values) {
                var target = container.down('input[type=text].suggest-cancers');
                target.placeholder = "Search for cancers";
                target.value = '';
            },
            'gene-picker' : function (container, values) {
                var _this = this;
                var target = container.down('input[type=text].suggest-genes');
                if (target && target._suggestPicker) {
                    target._silent = true;
                    target._suggestPicker.clearAcceptedList();
                    if (values) {
                        values.each(function(v) {
                            target._suggestPicker.addItem(v.id, v.gene, '');
                            var legendType = container.getAttribute("legendName");
                            var geneColor = (legendType && editor.getGeneLegend(legendType)) ? // Check if getGeneLegend returns null value
                                            editor.getGeneLegend(legendType).getGeneColor(v.id) : undefined;
                            _this._updateGene(container, v.id, v.id, v.gene, geneColor);
                        })
                    }
                    target._silent = false;
                    target.value = '';
                }
            },
            'submit-cancel' : function (container, value) {
                // this element is data-independent, nothing else to do
            },
            'phenotipsid-picker' : function (container, value, personData) {
                var _this = this;

                var suggestContainer = container.down('div.patient-newlink-container');
                var suggestInput     = container.down('input[type=text].suggest-patients');
                suggestInput.placeholder = "type patient name or identifier";

                var linkContainer = container.down('div.patient-link-container');
                var link          = container.down('a.patient-link-url');
                var linkRemove    = container.down('span.patient-link-remove');
                var label         = container.down('.field-name');

                suggestInput.value = "";

                if (value == "") {
                    if (label) {label.update("Link to an existing patient record");}
                    linkContainer.hide();
                    suggestContainer.show();
                } else {
                    if (label) {label.update("Linked to patient record");}
                    suggestContainer.hide();
                    link.target = value;
                    link.href = editor.getExternalEndpoint().getPhenotipsPatientURL(value);
                    var externalID = (!personData.external_id || personData.external_id.value == "") ? value : personData.external_id.value;
                    link.innerHTML = editor.getPreferencesManager().getConfigurationOption("replaceIdWithExternalID") ? externalID : value;
                    linkContainer.show();
                }
            },
            'button' : function (container, value) {
                // does not depend on input data, nothing to set
            },
            'select' : function (container, value) {
                var target = container.down('select option[value="' + value + '"]');
                if (target) {
                    target.selected = 'selected';
                }
            },
            'cancerlist': function (container, value) {
                var cancerLegend = editor.getCancerLegend();
                if (typeof value === 'object' && Object.getOwnPropertyNames(value).length === 0) {
                    container.select('table.cancers-summary-group').each(function(qualifiers) {
                        var cancerWidget = qualifiers._widget;
                        var cancerID = cancerWidget.getID();
                        if (cancerLegend._isSupportedCancer(cancerID)) {
                            cancerWidget.setValues({});
                        } else {
                            qualifiers.up().remove();
                        }
                    });
                } else {
                    for (var cancerID in value) {
                        var cancerData;
                        if (value.hasOwnProperty(cancerID)) {
                            cancerData = value[cancerID];
                            var qualifiers = container.down('table[id="cancers_' + cancerID + '"]');
                            if (cancerLegend._isSupportedCancer(cancerID)) {
                                // Only want to rebuild everything if importing data.
                                qualifiers._widget.size() === 0 && qualifiers._widget.setValues(cancerData);
                            } else {
                                if (qualifiers) {
                                    qualifiers._widget.size() === 0 && qualifiers._widget.setValues(cancerData);
                                } else {
                                    var cancerContainer = container.down('#cancers_data_table');
                                    cancerData = value[cancerID];
                                    var dataName = "cancers";
                                    qualifiers = this._addNewCancerRow(cancerID, cancerData.label, cancerContainer, false, [
                                        dataName + ':term:deleted',
                                        dataName + ':term:cleared',
                                        dataName + ':dialog:deleted',
                                        dataName + ':dialog:added',
                                        dataName + ':notes:updated',
                                        'change'
                                    ]);
                                    qualifiers._widget.setValues(cancerData);
                                }
                            }
                        }
                    }
                }
            },
            'hidden' : function (container, value) {
                var target = container.down('input[type=hidden]');
                if (target) {
                    target.value = value;
                }
            }
        },

        _toggleFieldVisibility : function(container, doHide) {
            if (doHide) {
            container.addClassName('hidden');
            } else {
            container.removeClassName('hidden');
            }
        },

        _setFieldInactive : {
            'radio' : function (container, inactive) {
                if (inactive === true) {
                    container.addClassName('hidden');
                } else {
                    container.removeClassName('hidden');
                    container.select('input[type=radio]').each(function(item) {
                        if (inactive && Object.prototype.toString.call(inactive) === '[object Array]') {
                            var disableViaOpacity = (inactive.indexOf("disableViaOpacity") >= 0);
                            if (inactive.indexOf(item.value) >= 0) {
                                item.disable();
                                if (disableViaOpacity) {
                                    Element.setOpacity(item.up(), 0);
                                } else {
                                    item.up().addClassName('hidden');
                                }
                            }
                            else {
                                item.enable();
                                if (disableViaOpacity) {
                                    Element.setOpacity(item.up(), 1);
                                } else {
                                    item.up().removeClassName('hidden');
                                }
                            }
                        } else if (!inactive) {
                            item.enable();
                            Element.setOpacity(item.up(), 1);
                            item.up().removeClassName('hidden');
                        }
                    });
                }
            },
            'checkbox' : function (container, inactive) {
                this._toggleFieldVisibility(container, inactive);
            },
            'text' : function (container, inactive) {
                this._toggleFieldVisibility(container, inactive);
            },
            'button' : function (container, inactive) {
                this._toggleFieldVisibility(container, inactive);
            },
            'textarea' : function (container, inactive) {
                this._toggleFieldVisibility(container, inactive);
            },
            'date-picker' : function (container, inactive) {
                this._toggleFieldVisibility(container, inactive);
            },
            'disease-picker' : function (container, inactive) {
                this._toggleFieldVisibility(container, inactive);
            },
            'ethnicity-picker' : function (container, inactive) {
                this._toggleFieldVisibility(container, inactive);
            },
            'hpo-picker' : function (container, inactive) {
                this._toggleFieldVisibility(container, inactive);
            },
            'cancers-picker' : function (container, inactive) {
                this._toggleFieldVisibility(container, inactive);
            },
            'gene-picker' : function (container, inactive) {
                this._toggleFieldVisibility(container, inactive);
            },
            'submit-cancel': function (container, inactive) {
                // this element can't be set inactive, if present
            },
            'phenotipsid-picker' : function (container, inactive) {
                this._toggleFieldVisibility(container, inactive);
            },
            'select' : function (container, inactive) {
                if (inactive === true) {
                    container.addClassName('hidden');
                } else {
                    container.removeClassName('hidden');
                    container.select('option').each(function(item) {
                        if (inactive && Object.prototype.toString.call(inactive) === '[object Array]') {
                            if (inactive.indexOf(item.value) >= 0)
                                item.addClassName('hidden');
                            else
                                item.removeClassName('hidden');
                        } else if (!inactive) {
                            item.removeClassName('hidden');
                        }
                    });
                }
            },
            'cancerlist' : function (container, inactive) {
                this._toggleFieldVisibility(container, inactive);
            },
            'hidden' : function (container, inactive) {
                this._toggleFieldVisibility(container, inactive);
            }
        },

        _setFieldDisabled : {
            'radio' : function (container, disabled) {
                if (disabled) {
                    if (Object.prototype.toString.call(disabled) === '[object Array]') {
                        container.select('input[type=radio]').each(function(item) {
                            item.disabled = (disabled.indexOf(item.value) >= 0);
                        });
                    } else if (disabled === true || disabled === false) {
                        container.select('input[type=radio]').each(function(item) {
                            item.disabled = disabled;
                        });
                    }
                }
            },
            'checkbox' : function (container, disabled) {
                var target = container.down('input[type=checkbox]');
                if (target) {
                    target.disabled = disabled;
                }
            },
            'button' : function (container, disabled, inactive) {
                if (disabled) {
                    this._toggleFieldVisibility(container, disabled);
                } else {
                    if (!inactive) {
                        this._toggleFieldVisibility(container, disabled);
                    }
                }
            },
            'text' : function (container, disabled) {
                var target = container.down('input[type=text]');
                if (target) {
                    target.disabled = disabled;
                }
            },
            'textarea' : function (container, disabled) {
                var target = container.down('textarea');
                if (target) {
                    target.disabled = disabled;
                }
            },
            'date-picker' : function (container, disabled) {
                Element.select(container,'select').forEach(function(element) {
                    if (disabled) {
                        // IE9 & IE10 do not support "pointer-events:none" (and IE11 does not seem to support this for <select>)
                        // so add some JS to prevent clicks on disabled select
                        Helpers.disableMouseclicks(element);
                        element.addClassName('disabled-select');
                        element.addClassName('no-mouse-interaction');
                    } else {
                        // remove IE-specific workaround handler
                        Helpers.enableMouseclicks(element);
                        element.removeClassName('disabled-select');
                        element.removeClassName('no-mouse-interaction');
                    }
                });
            },
            'disease-picker' : function (container, disabled) {
                this.__disableEnableSuggestModification(container, disabled);
            },
            'ethnicity-picker' : function (container, disabled) {
                this.__disableEnableSuggestModification(container, disabled);
            },
            'hpo-picker' : function (container, disabled) {
                this.__disableEnableSuggestModification(container, disabled);
            },
            'cancers-picker' : function (container, disabled) {
                container.select('input.suggest').each(function(item) {
                    item.disabled = disabled;
                    disabled && item.hide() || item.show();
                });
            },
            'gene-picker' : function (container, disabled) {
                this.__disableEnableSuggestModification(container, disabled);
            },
            'select' : function (container, disabled) {
                var target = container.down('select');
                if (target) {
                    target.disabled = disabled;
                }
            },
            'cancerlist' : function (container, disabled) {
                container.select('input[type=checkbox]').each(function(item) {
                    item.disabled = disabled;
                });
                container.select('textarea').each(function(item) {
                    item.disabled = disabled;
                });
                container.select('table.cancers-summary-group').each(function(qualifiers) {
                    qualifiers.disabled = disabled;
                    qualifiers.select(".action-edit", ".action-done", ".patient-details-add").invoke(disabled ? "hide" : "show");
                });
            },
            'submit-cancel': function (container, inactive) {
                // this element can't be disabled, if present
            },
            'phenotipsid-picker' : function (container, disabled, inactive, value) {
                if (!disabled) {
                    this._toggleFieldVisibility(container, disabled);
                } else {
                    if (value == "") {
                        this._toggleFieldVisibility(container, disabled);
                    }
                }
                container.select('span').each(function(item) {
                    item.style.display = disabled ? "none" : "inherit";
                });
            },
            'hidden' : function (container, disabled) {
                // FIXME: Not implemented
            }
        },

        // checks if fields satisfy their requirements: at the moment only
        // requirement suported is a "mandatory" field which should not be blankl
        _validateFieldValue : {
            'radio' : function (container, field_parameters, field_value, linkedRecordID) {
                // radio can not be blank, so always valid
                return true;
            },
            'checkbox' : function (container, field_parameters, field_value, linkedRecordID) {
                // does not make sense to even have a checkbox if it has to be always selected, so always valid
                return true;
            },
            'text' : function (container, field_parameters, field_value, linkedRecordID) {
                var errorField = container.up().down(".text-field-input-error");
                var addBadFieldComment = function(message) {
                    container.style.border = "1px solid red";
                    container.style.outline = "none";
                    if (errorField) {
                        errorField.update(message);
                        errorField.show();
                    }
                };
                var removeBadFieldComment = function() {
                    container.style.border = "";
                    container.style.outline = "";
                    if (errorField) {
                        errorField.update("");
                        errorField.hide();
                    }
                }
                removeBadFieldComment();

                if (field_parameters.required && !field_value) {
                    container.style.border = "1px solid red";
                    container.style.outline = "none";
                    // FIXME: can not use `addBadFieldComment("This field is required")` here due to nodeMenu layout problems:
                    // when e.g. last_name has this message, and first_name does not, the field below first_name gets shifted
                    // in an ugly way
                    return false;
                }
                if (field_value && field_parameters.validators && Helpers.arrayContains(field_parameters.validators, "uniqueExternalID")) {
                    if (!this._extIDValidationInProgress) {
                        // store the node for which the request is being made: the response may return after the current node is changed
                        this._idCheckForNode = this.targetNode;
                        this._extIDValidationInProgress = true;
                        var _this = this;
                        var onValidID = function() {
                            removeBadFieldComment();
                            _this._extIDValidationInProgress = false;
                        };
                        var onInvalidID = function() {
                            if (_this._idCheckForNode == _this.targetNode) {
                                // request returned for the same id we are showing node menu for
                                addBadFieldComment("This identifier already exists");
                            }
                            _this._extIDValidationInProgress = false;
                        };
                        editor.getExternalIdManager().isUniqueID(linkedRecordID, field_value, onValidID, onInvalidID);
                    }
                }
                container.style.border = "";
                container.style.outline = "";
                return true;
            },
            'textarea' : function (container, field_parameters, field_value, linkedRecordID) {
                if (field_parameters.required && !field_value) {
                    container.style.border = "1px solid red";
                    container.style.outline = "none";
                    return false;
                }
                container.style.border = "";
                container.style.outline = "";
                return true;
            },
            'date-picker' : function (container, field_parameters, field_value, linkedRecordID) {
                var errorField = container.up().up().down(".date-field-input-error");
                var addBadFieldComment = function(message) {
                    if (errorField) {
                        errorField.update(message);
                        errorField.show();
                    }
                };
                var removeBadFieldComment = function() {
                    if (errorField) {
                        errorField.update("");
                        errorField.hide();
                    }
                }
                removeBadFieldComment();

                var checkDateComponent = function(name) {
                    if (field_parameters.required
                        && (!field_value || !field_value.hasOwnProperty(name) || !field_value[name])) {
                        container.parentElement.select("." + name)[0].style.border="1px solid red";
                        container.parentElement.select("." + name)[0].style.outline = "none";
                        return false;
                    } else {
                        container.parentElement.select("." + name)[0].style.border="";
                        container.parentElement.select("." + name)[0].style.outline = "";
                        return true;
                    }
                }
                var yearOK  = checkDateComponent("year");
                var monthOK = checkDateComponent("month");
                var dayOK   = checkDateComponent("day");
                // note: can't use checkDateComponent("year") && checkDateComponent("month") swince if "year check"
                //       fails "month check" will never be run, and we need side-effects (red borders) of a failed check
                var result = yearOK && monthOK && dayOK;
                if (!result) {
                    if (yearOK || monthOK || dayOK) {
                        addBadFieldComment("Please enter full date");
                    }
                }
                return result;
            },
            'button' : function (container, field_parameters, field_value, linkedRecordID) {
                // not supported
                return true;
            },
            'disease-picker' : function (container, field_parameters, field_value, linkedRecordID) {
                // not supported
                return true;
            },
            'ethnicity-picker' : function (container, field_parameters, field_value, linkedRecordID) {
                // not supported
                return true;
            },
            'hpo-picker' : function (container, field_parameters, field_value, linkedRecordID) {
                // not supported
                return true;
            },
            'cancers-picker' : function (container, field_parameters, field_value, linkedRecordID) {
                // not supported
                return true;
            },
            'gene-picker' : function (container, field_parameters, field_value, linkedRecordID) {
                // not supported
                return true;
            },
            'submit-cancel' : function (container, field_parameters, field_value, linkedRecordID) {
                // this element is always valid
                return true;
            },
            'phenotipsid-picker' : function (container, field_parameters, field_value, linkedRecordID) {
                // not supported
                return true;
            },
            'select' : function (container, field_parameters, field_value, linkedRecordID) {
                // not supported
                return true;
            },
            'cancerlist' : function (container, field_parameters, field_value, linkedRecordID) {
                // not supported
                return true;
            },
            'hidden' : function (container, field_parameters, field_value, linkedRecordID) {
                // non-user editable, so always valid
                return true;
            }
        },

        // Either enables a suggest picker, or disables/hides all input fields while leaving
        // the list of current selections visible, with "delete" tool disabled
        __disableEnableSuggestModification: function(container, disabled) {
            var numElements = 0;
            Element.select(container,'.delete-tool').forEach(function(element) {
                numElements++;
                if (disabled) {
                    element.addClassName('hidden');
                } else {
                    element.removeClassName('hidden');
                }
            });
            Element.select(container,'input').forEach(function(element) {
                element.disabled = disabled;
                if (disabled) {
                    if (numElements > 1) {  // 1 for delete all; if there is more than one no need to display empty input box
                        // if there are selections show the list of selections and hide the input field completely
                        element.addClassName('hidden');
                    } else {
                        // if there are no selections show "none" in the (disabled) input field
                        element.placeholder = "none";
                    }
                } else {
                    element.removeClassName('hidden');
                    element.placeholder = "";
                }
            });
        }
    });
    return NodeMenu;
});
