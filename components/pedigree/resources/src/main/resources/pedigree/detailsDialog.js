/**
 * DetailsDialog allows to build a dialog for collecting additional "qualifiers" data for some term.
 *
 * @class DetailsDialog
 * @constructor
 */
// TODO: Move and rename.
define([], function() {
    var DetailsDialog = Class.create({
        initialize: function (elementID, dataName, parentContainer) {
            this._container = parentContainer;
            this._dataName = dataName;
            this._elementID = elementID;
            this._qualifierMap = {};
            this._buildEmptyDialog();
        },

        /**
         * Generates a numeric select element.
         *
         * @param options {Object} an options object containing:
         *                         - "from" the starting value, as integer, for the numeric select
         *                         - "to" the final value, as integer, for the numeric select
         *                         - "step" the step size, as integer
         *                         - "majorStepSize" the major step size after which to generate a numeric range
         *                                           (e.g. majorStepSize = 10, means every 10th numeric element will be
         *                                           a range); if not specified, no ranges will be generated
         *                         - "defListItemClass" the css class for the definition list item
         *                         - "qualifierLabel" the label for the qualifier definition list element
         *                         - "qualifierName" the name of the qualifier definition list element
         *                         - "inputSourceClass" the css class for the input source element
         *                         - "displayedToStoredMapper" custom function mapping displayed value to how it should be stored
         *                         - "storedToDisplayedMapper" custom function mapping stored value to how it should be displayed
         * @return {*|DetailsDialog}
         */
        withNumericSelect: function(options) {
            // Define data ranges for the numeric select element.
            options = options || {};
            if (!options.from || !options.to) { return this; }
            var from = options.from;
            var to = options.to;
            var step = options.step || 1;
            var majorStepSize = options.majorStepSize || null;
            var inputSourceClass = options.inputSourceClass || "";
            var spanElem = new Element('span');
            var optionsHTML = '<select name="' + this._dataName + '" class="'+inputSourceClass+'"><option value=""></option>';
            majorStepSize && (optionsHTML += '<option value="<' + from + '">before ' + from + '</option>');
            var counter = 1;
            var startRange = from;
            for (var num = from; num <= to; num += step, counter++) {
                if (majorStepSize && counter === majorStepSize) {
                    counter = 0;
                    optionsHTML += '<option value="' + startRange + '-' + num + '">' + startRange + '-' + num + '</option>';
                    startRange = num + step;
                }
                optionsHTML += '<option value="' + num + '">' + num + '</option>';
            }
            majorStepSize && (optionsHTML += '<option value=">' + to + '">after ' + to + '</option></select>');
            spanElem.innerHTML = optionsHTML;
            spanElem._getValue = this._defaultGetSelectValueFx(spanElem);
            spanElem._setValue = this._setSelectValueFx(spanElem, options.storedToDisplayedMapper);
            spanElem._addValue = this._addSelectValueFx(spanElem, options.qualifierName, options.displayedToStoredMapper);
            options.inline = 'inline';
            return this.withQualifierElement(spanElem, false, options);
        },

        /**
         * Generates a select element for an item array.
         *
         * @param options {Object} an options object containing:
         *                         - "data" the select list items, as array
         *                         - "defListItemClass" the css class for the definition list item
         *                         - "qualifierLabel" the label for the qualifier definition list element
         *                         - "qualifierName" the name of the qualifier definition list element
         *                         - "inputSourceClass" the css class for the input source element
         *                         - "displayedToStoredMapper" custom function mapping displayed value to how it should be stored
         *                         - "storedToDisplayedMapper" custom function mapping stored value to how it should be displayed
         */
        withItemSelect: function(options) {
            options = options || {};
            var inputSourceClass = options.inputSourceClass || "";
            var data = options.data || [""];
            var spanElem = new Element('span');
            var optionsHTML = '<select name="' + this._dataName + '" class="'+inputSourceClass+'">';
            data.forEach(function(item) {
                optionsHTML += '<option value="' + item + '">' + item + '</option>';
            });
            optionsHTML += '</select>';
            spanElem.innerHTML = optionsHTML;
            spanElem._getValue = this._defaultGetSelectValueFx(spanElem);
            spanElem._setValue = this._setSelectValueFx(spanElem, options.storedToDisplayedMapper);
            spanElem._addValue = this._addSelectValueFx(spanElem, options.qualifierName, options.displayedToStoredMapper);
            options.inline = 'inline';
            return this.withQualifierElement(spanElem, false, options);
        },

        /**
         * Generates a list with radio button elements for each item in provided array.
         *
         * @param collapsible true iff the qualifier element is collapsible
         * @param options {Object} an options object containing:
         *                         - "data" the select list items, as array
         *                         - "defListItemClass" the css class for the definition list item
         *                         - "qualifierLabel" the label for the qualifier definition list element
         *                         - "qualifierName" the name of the qualifier definition list element
         *                         - "inputSourceClass" the css class for the input source element
         *                         - "displayedToStoredMapper" custom function mapping displayed value to how it should be stored
         *                         - "storedToDisplayedMapper" custom function mapping stored value to how it should be displayed
         */
        withRadioList: function(collapsible, options) {
            options = options || {};
            var _this = this;
            var inputSourceClass = options.inputSourceClass || "";
            var data = options.data || [];
            var spanElem = new Element('span');
            var radioHTML = '<ul>';
            data.forEach(function(item) {
                var inputId = inputSourceClass + "_" + _this._elementID + "_" + item;
                radioHTML +=
                    '<li class="term-entry">' +
                      '<input class="' + inputSourceClass + '" id="' + inputId + '" name="' + _this._dataName + '_' + _this._elementID + '_' + options.qualifierName + '" title="' + item + '" type="radio">' +
                      '<label for="' + inputId + '" title="' + item + '">' + item + '</label>' +
                    '</li>'
            });
            radioHTML += '</ul>';
            spanElem.innerHTML = radioHTML;
            spanElem.down("li.term-entry").down('input').checked=true;
            spanElem._getValue = this._getRadioValueFx(spanElem, options.qualifierName);
            spanElem._setValue = this._setRadioValueFx(spanElem, options.storedToDisplayedMapper, collapsible);
            spanElem._addValue = this._addRadioValueFx(spanElem, options.qualifierName, options.displayedToStoredMapper);
            return this.withQualifierElement(spanElem, collapsible, options);
        },

        /**
         * Generates and adds a text-box.
         *
         * @param collapsible true iff the qualifier element is collapsible
         * @param options {Object} an options object containing:
         *                         - "defListItemClass" the css class for the definition list item
         *                         - "qualifierLabel" the label for the qualifier definition list element
         *                         - "qualifierName" the name of the qualifier definition list element
         *                         - "inputSourceClass" the css class for the input source element
         *                         - "displayedToStoredMapper" custom function mapping displayed value to how it should be stored
         *                         - "storedToDisplayedMapper" custom function mapping stored value to how it should be displayed
         */
        withTextBox: function(collapsible, options) {
            var inputSourceClass = options && options.inputSourceClass || "";
            var spanElem = new Element('span');
            spanElem.innerHTML = '<textarea class="qualifier-notes '+inputSourceClass+'" name="' + this._dataName + '">'
              + '</textarea>';
            spanElem._getValue = this._getTextboxValueFx(spanElem);
            spanElem._setValue = this._setTextboxValueFx(spanElem, options.storedToDisplayedMapper, collapsible);
            spanElem._addValue = this._addTextboxValueFx(spanElem, options.qualifierName, options.displayedToStoredMapper);
            return this.withQualifierElement(spanElem, collapsible, options);
        },

        /**
         * Adds a custom qualifier definition list element.
         *
         * @param element {Element} the custom data collection element to add (e.g. a textbox)
         * @param collapsible {Boolean} true iff this is a collapsible element
         * @param options {Object} an options object containing:
         *                         - "defListItemClass" the css class for the definition list item
         *                         - "qualifierLabel" the label for the qualifier definition list element
         *                         - "qualifierName" the name of the qualifier definition list element
         *                         - "inline" contains the inline class name iff element is inline, not specified otherwise
         * @return {DetailsDialog}
         */
        withQualifierElement: function(element, collapsible, options) {
            var defListItemClass = options.defListItemClass || "";
            var qualifierLabel = options.qualifierLabel || "";
            var qualifierName = options.qualifierName || "";
            var dtElem = new Element('dt');
            var ddElem = new Element('dd');
            dtElem.addClassName(defListItemClass);
            if (options.inline) {
                dtElem.addClassName(options.inline);
                ddElem.addClassName(options.inline);
            }
            ddElem.addClassName(defListItemClass);
            var selectedValue;
            if (collapsible) {
                dtElem.addClassName("collapsible");
                ddElem.addClassName("collapsed");
                dtElem.insert('<span class="collapse-button">►</span>');
                var termEntry = element.down("li.term-entry");
                var termInput = termEntry && termEntry.down('input');

                var selectedTitle = termInput && termInput.title || "";
                selectedValue = '<span class="selected-value">' + selectedTitle + '</span>';
                ddElem.hide();
            }
            dtElem.insert('<label>' + qualifierLabel + '</label>');
            selectedValue && dtElem.insert(selectedValue);
            ddElem.insert(element);
            this._attachSummaryFx(element, dtElem, ddElem);
            this._qualifierList.insert(dtElem).insert(ddElem);
            qualifierName && (this._qualifierMap[qualifierName] = element);
            return this;
        },

        /**
         * Adds a delete action element to the dialog.
         *
         * @return {DetailsDialog} self
         */
        withDeleteAction: function() {
            var deleteAction = new Element('span', {'id' : 'delete_' + this._elementID, 'class' : this._dataName + '-dialog-delete action-done clickable'})
              .update("✖");
            this._termDetails.insert({top: deleteAction});
            this._attachOnDeleteListener(deleteAction);
            return this;
        },

        /**
         * Attaches the dialog to the parent container (this._container).
         *
         * @return {DetailsDialog} self
         */
        attach: function() {
            this._container.insert(this._termDetails);
            return this;
        },

        /**
         * Returns the constructed dialog element.
         *
         * @return {Element|*} the constructed dialog element
         */
        getDialog: function() {
            return this._termDetails;
        },

        /**
         * Gets the qualifier ID.
         *
         * @return {String} the qualifier ID
         */
        getID: function() {
            return this._elementID;
        },

        /**
         * Gets the values for all the input sources in the dialog.
         *
         * @return {Object} containing the custom element name to value mapping
         */
        getValues: function() {
            var values = {};
            for (var key in this._qualifierMap) {
                if (this._qualifierMap.hasOwnProperty(key)) {
                  var element = this._qualifierMap[key];
                  element._addValue(values);
                }
            }
            return values;
        },

        /**
         * Sets values for the dialog.
         *
         * @param values {Object} the values are key-value pairs, where the keys should be the same as in _qualifierMap
         */
        setValues: function(values) {
            if (!values) { return; }
            for (var key in values) {
                if (values.hasOwnProperty(key)) {
                    var elem = this._qualifierMap[key];
                    if (elem) {
                        elem._setValue(values[key]);
                    }
                }
            }
            return this;
        },

        /**
         * Blurs the dialog.
         */
        blur: function() {
            if (this._termDetails.hasClassName('focused')) {
                this._termDetails.removeClassName('focused');
                this._termDetails.addClassName('blurred');
                this._toggleSummarize();
            }
        },

        /**
         * Focuses the dialog.
         */
        focus: function() {
            if (!this._termDetails.hasClassName('focused')) {
                this._termDetails.removeClassName('blurred');
                this._termDetails.addClassName('focused');
                this._toggleSummarize();
            }

        },

        /**
         * Attaches a function for creating a summary of the element.
         *
         * @param element the element holding the qualifier input element being summarized
         * @param dtElem the qualifier item title element
         * @param ddElem the qualifier item value element
         * @private
         */
        _attachSummaryFx: function(element, dtElem, ddElem) {
            var _this = this;
            element._toggleSummarize = function() {
                var isCollapsed = ddElem.hasClassName('collapsed');
                if (ddElem.visible() || (dtElem.visible() && isCollapsed)) {
                    _this._showSummary(dtElem, ddElem, element._getValue());
                } else {
                    _this._showEditable(dtElem, ddElem);
                }
            };
        },

        /**
         * Triggers the display of the summary qualifier item version.
         *
         * @param dtElem the qualifier item title element
         * @param ddElem the qualifier item value element
         * @param val the value that was entered for the qualifier element
         * @private
         */
        _showSummary: function(dtElem, ddElem, val) {
            ddElem.hide();
            // If some value was entered for the qualifier, create the summary.
            if (val.length > 0 && val !== "Unknown") {
                var dtSummary;
                var ddSummary;
                if (dtElem.hasClassName('collapsible')) {
                    dtElem.hide();
                    dtSummary = '<dt class="' + dtElem.className + ' preview">' + dtElem.down('label').innerHTML + '</dt>';
                    dtElem.insert({'after': dtSummary});
                }
                ddSummary = '<dd class="' + ddElem.className + ' preview">' + val + '</dd>';
                ddElem.insert({'after': ddSummary});
            } else {
                dtElem.hide();
            }
        },

        /**
         * Triggers the display of the editable qualifier item version.
         *
         * @param dtElem the qualifier item title element
         * @param ddElem the qualifier item value element
         * @private
         */
        _showEditable: function(dtElem, ddElem) {
            dtElem.show();
            // Show only if not collapsed.
            ddElem && !ddElem.hasClassName('collapsed') && ddElem.show();
            var dtSummary;
            var ddSummary;
            // Remove the summary elements.
            dtSummary = dtElem.next('dt.preview');
            ddSummary = ddElem.next('dd.preview');
            dtSummary && dtSummary.remove();
            ddSummary && ddSummary.remove();
        },

        /**
         * A function for retrieving the selected select list value from the element.
         *
         * @param element the parent element holding the select list
         * @return {Function} a function for retrieving the selected value from the select list element
         * @private
         */
        _defaultGetSelectValueFx: function(element) {
            return function() {
                var select = element.down('select');
                return select.selectedIndex >= 0 ? select.options[select.selectedIndex].value : "";
            };
        },

        /**
         * A function for setting a selected select list value.
         *
         * @param element the parent element holding the select list
         * @param valueMapperFx the custom value mapper function; null if value should be used as is
         * @return {Function} a function for selecting the provided value in the select list element
         * @private
         */
        _setSelectValueFx: function(element, valueMapperFx) {
            return function(value) {
                element.down('select').value = valueMapperFx ? valueMapperFx(value) : value;
            };
        },

        /**
         * A function for adding a selected select list value to the summary {Object}.
         *
         * @param element the parent element holding the select list
         * @param qualifierName the user-provided qualifier name
         * @param valueMapperFx the custom value mapper function; null if value should be used as is
         * @return {Function} a function for adding the selected select list value to the summary {Object}
         * @private
         */
        _addSelectValueFx: function(element, qualifierName, valueMapperFx) {
            return function(values) {
                var fieldName = qualifierName || null;
                if (fieldName) {
                    values[fieldName] = valueMapperFx ? valueMapperFx(element._getValue()) : element._getValue();
                }
            };
        },

        /**
         * A function for retrieving the selected radio list value from the element.
         *
         * @param element the parent element holding the radio list
         * @param qualifierName the user-provided qualifier name
         * @return {Function} a function for retrieving the selected value from the radio list element
         * @private
         */
        _getRadioValueFx: function(element, qualifierName) {
            var _this = this;
            return function() {
              return element.down('input[name="' + _this._dataName + '_' + _this._elementID + '_' + qualifierName
                  + '"]:checked').title;
            };
        },

        /**
         * A function for setting a selected radio list value.
         *
         * @param element the parent element holding the radio list
         * @param valueMapperFx the custom value mapper function; null if value should be used as is
         * @param collapsible true iff the element is collapsible
         * @return {Function} a function for selecting the provided value in the radio list element
         * @private
         */
        _setRadioValueFx: function(element, valueMapperFx, collapsible) {
            var _this = this;
            return function(value) {
                var checkedTitle = valueMapperFx ? valueMapperFx(value) : value;
                var selection = element.down('input[title="' + checkedTitle + '"]');
                if (!selection) { return; }
                selection.checked = true;
                var ddElem = selection.up('dd');
                var dtElem = ddElem && ddElem.previous('dt');
                if (dtElem) {
                    _this._updateLabelWithSelection(selection, dtElem);
                    collapsible && _this._collapseElem(dtElem, ddElem);
                }
            };
        },

        /**
         * A function for adding a selected radio list value to the summary {Object}.
         *
         * @param element the parent element holding the radio list
         * @param qualifierName the user-provided qualifier name
         * @param valueMapperFx the custom value mapper function; null if value should be used as is
         * @return {Function} a function for adding the selected radio list value to the summary {Object}
         * @private
         */
        _addRadioValueFx: function(element, qualifierName, valueMapperFx) {
            return function(values) {
                var fieldName = qualifierName || null;
                if (fieldName) {
                    values[fieldName] = valueMapperFx ? valueMapperFx(element._getValue()) : element._getValue();
                }
            };
        },

        /**
         * A function for retrieving the text box value from the textarea element.
         *
         * @param element the parent element holding the text box
         * @return {Function} a function for retrieving the value from the textarea element
         * @private
         */
        _getTextboxValueFx: function(element) {
            return function() {
                return element.down('textarea').value;
            };
        },

        /**
         * A function for setting a textarea value.
         *
         * @param element the parent element holding the text box
         * @param valueMapperFx the custom value mapper function; null if value should be used as is
         * @param collapsible true iff the element is collapsible
         * @return {Function} a function for setting a provided value to the textarea element
         * @private
         */
        _setTextboxValueFx: function(element, valueMapperFx, collapsible) {
            return function(value) {
                element.down('textarea').value = valueMapperFx ? valueMapperFx(value) : value;
            };
        },

        /**
         * A function for adding a textarea value to the summary {Object}.
         *
         * @param element the parent element holding the text box
         * @param qualifierName the user-provided name of the qualifier being processed
         * @param valueMapperFx the custom value mapper function; null if value should be used as is
         * @return {Function} a function for adding the textarea value to the summary {Object}
         * @private
         */
        _addTextboxValueFx: function(element, qualifierName, valueMapperFx) {
            return function(values) {
                var fieldName = qualifierName || null;
                if (fieldName) {
                    values[fieldName] = valueMapperFx ? valueMapperFx(element.down('textarea').value) : element.down('textarea').value;
                }
            };
        },

        /**
         * Toggles the summary/edit mode for the dialog.
         * @private
         */
        _toggleSummarize: function() {
            for (var key in this._qualifierMap) {
                if (this._qualifierMap.hasOwnProperty(key)) {
                    var elem = this._qualifierMap[key];
                    elem._toggleSummarize();
                }
            }
        },

        /**
         * Builds an empty dialog where qualifiers will be added.
         * @private
         */
        _buildEmptyDialog: function () {
            this._termDetails = new Element('div', {'class' : 'summary-item focused'});
            this._termDetails.innerHTML = '<input type="hidden" value="' + this._elementID + '">' +
              '<div id="term_details_' + this._elementID + '" class="term-details">' +
                  '<dl></dl>' +
                  '<div class="clearfloats"></div>' +
              '</div>';

            this._qualifierList = this._termDetails.down('dl');
            // Attach listeners for the collapse action.
            this._attachQualifierStateChangeListener();
        },

        /**
         * Attaches a listener for clicks on the dialog delete button.
         * @param deleteAction
         * @private
         */
        _attachOnDeleteListener: function(deleteAction) {
            var _this = this;
            deleteAction.observe('click', function() {
                _this._termDetails.remove();
                _this._qualifierMap = {};
                Event.fire(_this._container, _this._dataName + ':dialog:deleted', {'id' : _this._elementID});
            });
        },

        /**
         * Attaches an onclick listener to observe click events on specified elements.
         * @private
         */
        _attachQualifierStateChangeListener: function() {
            var _this = this;
            this._termDetails.observe('click', function(event) {
                var elem = event.target.up();
                var ddElem;
                // The on-click event is to collapse/un-collapse an element
                if (elem && elem.hasClassName('collapsible')) {
                    ddElem = elem.next('dd');
                    ddElem && _this._toggleCollapsed(elem, ddElem);
                // The on-click event is to select some list item
                } else if (elem && elem.hasClassName('term-entry')) {
                    ddElem = elem.up('dd');
                    ddElem && _this._updateLabelWithSelection(elem.down('input'), ddElem.previous('dt'));
                }
            });
        },

        /**
         * Updates the definition list element label with the current selection.
         *
         * @param elInput the current selection made by user
         * @param dtElem the element holding the label
         * @private
         */
        _updateLabelWithSelection: function(elInput, dtElem) {
            var currentValHolder = dtElem && dtElem.down('span.selected-value');
            if (currentValHolder) {
                var selectedValue = elInput && elInput.title || "";
                currentValHolder.update(selectedValue);
                if (selectedValue === "Unknown" || selectedValue === "") {
                    currentValHolder.removeClassName("selected");
                } else {
                    currentValHolder.addClassName("selected");
                }
            }
        },

        /**
         * Toggles collapsed for a collapsible element.
         *
         * @param dtElem the element containing the collapse button
         * @param ddElem the element containing the data to be collapsed
         * @private
         */
        _toggleCollapsed: function(dtElem, ddElem) {
            if (ddElem.hasClassName('collapsed')) {
                this._expandElem(dtElem, ddElem);
            } else {
                this._collapseElem(dtElem, ddElem);
            }
        },

        /**
         * Collapse the collapsible element.
         *
         * @param dtElem the label element
         * @param ddElem the value element
         * @private
         */
        _collapseElem: function(dtElem, ddElem) {
            var collapseSpan = dtElem.down('span.collapse-button');
            ddElem.addClassName('collapsed');
            collapseSpan.innerHTML = "►";
            ddElem.hide()
        },

        /**
         * Expand the collapsible element.
         *
         * @param dtElem the label element
         * @param ddElem the value element
         * @private
         */
        _expandElem: function(dtElem, ddElem) {
            var collapseSpan = dtElem.down('span.collapse-button');
            ddElem.removeClassName('collapsed');
            collapseSpan.innerHTML = "▼";
            ddElem.show();
        }
    });

    return DetailsDialog;
});
