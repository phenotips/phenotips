/**
 * DetailsDialogGroup allows to create one or more instances of DetailsDialog for some term.
 *
 * @class DetailsDialogGroup
 * @constructor
 */
// TODO: Move and rename.
define([
        "pedigree/detailsDialog"
    ], function(
        DetailsDialog
    ){
    var DetailsDialogGroup = Class.create({
        initialize: function (dataName, options) {
            this._dataName = dataName;
            // If this._allowMultiDialogs is true, more than one qualifier dialog can be added per term.
            this._qualifierNo = 0;

            var groupOptions = options || {};
            this._allowMultiDialogs = groupOptions.allowMultiDialogs || false;
            this._disableTermDelete = groupOptions.disableTermDelete || false;
            this._doneTypingInterval = groupOptions.doneTypingInterval || 500;

            this._dialogOptions = [];

            this._dialogMap = {};

            this._crtFocus = null;

            // Builds an empty container for the term.
            this._buildEmptyContainer();

            // Attach listeners.
            this._addDialogDeletedListener();
            this._addDialogFocusManagers();
            this._attachKeyUpObserver();
        },

        getID: function() {
            return this._termID;
        },

        /**
         * Returns the constructed dialog group element for the term.
         *
         * @return {Element|*} the dialog group element for the term
         */
        get: function() {
            return this._qualifiersContainer;
        },

        /**
         * Associates the dialog group with some term.
         *
         * @param label {String} the label for the term; if null or empty, will be set to ID
         * @param termID {String} the ID for the term; must not be null or empty
         * @param tooltip the class name of the tooltip to be created; null/undefined if no tooltip should be attached
         * @return {DetailsDialogGroup} self
         */
        withLabel: function(label, termID, tooltip) {
            var trimmedID = (termID && termID.strip()) || "";
            var trimmedLabel = (label && label.strip()) || "";
            this._termID = trimmedID;
            this._label = trimmedLabel || trimmedID;
            if (this._termID === "") {
                return this;
            }
            this._qualifiersContainer.id = this._dataName + "_" + this._termID;
            var termData = this._qualifiersContainer.down('span.term-data');
            var termDataHTML = '<label class="label-field">';
            termDataHTML += '<input id="status_' + this._termID + '" class="term-status" name="' + this._dataName +
              '" type="checkbox"> ';
            termDataHTML += this._label + '</label><input class="term-id" type="hidden" value="' + this._termID + '">';
            termData.innerHTML = termDataHTML;
            this._addTooltip(tooltip);
            this._initButtons();
            return this;
        },

        dialogsAddNumericSelect: function(options) {
            var addNumericSelect = function(currentDialog) {
                currentDialog.withNumericSelect(options);
            };
            this._dialogOptions.push(addNumericSelect);
            return this;
        },

        dialogsAddItemSelect: function(options) {
            var addItemSelect = function(currentDialog) {
                currentDialog.withItemSelect(options);
            };
            this._dialogOptions.push(addItemSelect);
            return this;
        },

        dialogsAddRadioList: function(collapsible, options) {
            var addRadioList = function(currentDialog) {
                currentDialog.withRadioList(collapsible, options);
            };
            this._dialogOptions.push(addRadioList);
            return this;
        },

        dialogsAddTextBox: function(collapsible, options) {
            var addTextBox = function(currentDialog) {
                currentDialog.withTextBox(collapsible, options);
            };
            this._dialogOptions.push(addTextBox);
            return this;
        },

        dialogsAddCustomElement: function(element, collapsible, options) {
            var addCustomElement = function(currentDialog) {
                currentDialog.withQualifierElement(element, collapsible, options);
            };
            this._dialogOptions.push(addCustomElement);
            return this;
        },

        dialogsAddDeleteAction: function() {
            var addDeleteAction = function(currentDialog) {
                currentDialog.withDeleteAction();
            };
            this._dialogOptions.push(addDeleteAction);
            return this;
        },

        clearDialogOptions: function() {
            this._dialogOptions = [];
            return this;
        },

        /**
         * Returns true iff status input is marked as selected, false otherwise.
         *
         * @return {Boolean}
         */
        isAffected: function() {
            var status = this._qualifiersContainer.down('input.term-status');
            return status.checked;
        },

        /**
         * Sets as affected and adds an empty qualifier dialog.
         *
         * @param silent true iff this should be performed silently, with no events fired
         */
        initAffected: function(silent) {
            this.affected(true);
            this.addDialog(silent);
            this._allowMultiDialogs && this._addDetailsClickListener();
        },

        /**
         * Sets as affected iff status is true, unaffected otherwise.
         *
         * @param status true iff affected, false otherwise
         */
        affected: function(status) {
            var statusElem = this._qualifiersContainer.down('input.term-status');
            statusElem.checked = status;
        },

        /**
         * Gets and returns all the qualifier values that were entered.
         *
         * @return {{id: (String|*|string), label: (String|*|string), affected: (*|Boolean), qualifiers: Array}}
         */
        getValues: function() {
            var qualifiers = [];
            for (var key in this._dialogMap) {
                if (this._dialogMap.hasOwnProperty(key)) {
                    qualifiers.push(this._dialogMap[key].getValues());
                }
            }
          return {
              "id" : this._termID,
              "label" : this._label,
              "affected" : this.isAffected(),
              "qualifiers" : qualifiers
            };
        },

        /**
         * Sets the qualifiers to the values provided.
         *
         * @param values the qualifier values to set
         */
        setValues: function(values) {
            // If no values provided, do nothing.
            if (!values) { return; }
            this._clearDetails();
            // Wrong term.
            if (values.id !== undefined && this._termID !== values.id) { return; }
            // Want to change the label.
            if (values.label !== undefined) {
                var trimmedLabel = values.label && values.label.strip();
                // The label should not be empty.
                this._label !== trimmedLabel && (this._label = trimmedLabel || this._termID);
            }
            // If affected and qualifiers are provided, set affected and add qualifiers.
            if (values.affected && values.qualifiers && values.qualifiers.length > 0) {
                this.affected(values.affected);
                this._setQualifiers(values.qualifiers);
            }
        },

        /**
         * Clear all qualifier details and un-select term.
         */
        clearDetails: function() {
            this._clearDetails();
            Event.fire(this._qualifiersContainer, this._dataName + ':term:cleared');
        },

        /**
         * Adds a qualifiers dialog.
         *
         * @param silent true iff dialog creation should be silent (that is, no event should be fired)
         * @return {DetailsDialog} the dialog just created
         */
        addDialog: function(silent) {
            var dialog = this._addDialog();
            this._dialogMap[dialog.getID()] = dialog;
            this._applyDialogOptions(dialog);
            !this._allowMultiDialogs && this._removeDetailsClickListener();
            this._dialogHolder.show();
            if (!silent) {
                Event.fire(this._qualifiersContainer, this._dataName + ':dialog:added', {'element': dialog.getDialog()});
            }
            return dialog;
        },

        /**
         * Returns the number of qualifiers contained by the term.
         *
         * @return {number}
         */
        size: function() {
            var summaryItems = this._qualifiersContainer.select('div.summary-item');
            return summaryItems ? summaryItems.length : 0;
        },

        _setQualifiers: function(qualifiers) {
            var _this = this;
            qualifiers.forEach(function(qualifier) {
                _this.addDialog(true).setValues(qualifier).blur();
            });
            this._allowMultiDialogs && this._addDetailsClickListener();
        },

        /**
         * Clears the details by removing all listeners, and emptying all containers and maps.
         * @private
         */
        _clearDetails: function() {
            this._qualifierNo = 0;
            this._dialogHolder.descendants().forEach(function(elem) {
                elem.stopObserving();
            });
            this._dialogMap = {};
            this._dialogHolder.update();
            this._dialogHolder.hide();
            this.affected(false);
            this._removeDetailsClickListener();
        },

        /**
         * Builds an empty container that will hold qualifiers dialogs for a term.
         * @private
         */
        _buildEmptyContainer: function() {
            this._qualifiersContainer = new Element('table', {'class' : 'summary-group ' + this._dataName + "-summary-group"});
            this._qualifiersContainer.name = this._dataName;

            this._dialogHolder = new Element('td', {'class' : 'dialog-holder'});

            var tbody = new Element('tbody')
              .insert(new Element('tr', {'class' : 'term-holder'})
                .insert(new Element('td')
                  .insert(new Element('span', {'class' : 'term-data'}))
                  .insert(new Element('span', {'class' : 'delete-button-holder'}))))
              .insert(new Element('tr')
                .insert(this._dialogHolder))
              .insert(new Element('tr')
                .insert(new Element('td', {'class' : 'add-button-holder'})));

            this._qualifiersContainer.insert(tbody);
            this._dialogHolder.hide();
        },

        _addTooltip: function(tooltip) {
            if (!tooltip) { return; }
            var infoTool = new Element('span', {'class' : 'fa fa-info-circle xHelpButton ' + tooltip, 'title' : this.getID()});
            new PhenoTips.widgets.HelpButton(infoTool);
            var termData = this._qualifiersContainer.down('span.term-data');
            termData.insert(infoTool);
        },

        /**
         * Sets the starting state for the "Add Details" and "Delete" buttons.
         *
         * @private
         */
        _initButtons: function() {
            this._addButtonHolder = this._qualifiersContainer.down('td.add-button-holder');
            this._addDetailsButton = new Element('span', {'id' : 'add_details_' + this._termID,
              'class' : 'patient-menu-button patient-details-add'}).update('<i class="fa fa-plus"></i> ...');

            this._addButtonHolder.insert(this._addDetailsButton);

            // Hide delete button or attach observers.
            this._deleteButtonHolder = this._qualifiersContainer.down('span.delete-button-holder');
            if (this._disableTermDelete) {
                this._deleteButtonHolder.hide();
            } else {
                this._deleteButton = new Element('span', {'id' : 'delete_term_' + this._termID,
                  'class' : 'action-done patient-term-delete clickable'}).update('✖');
                this._deleteButtonHolder.insert(this._deleteButton);
                this._addTermDeleteListener();
            }
            // Need to hide the add details button and add the select listener.
            this._addButtonHolder.hide();
            this._addOnTermSelectListener();
        },

        /**
         * Applies the predefined dialog options onto some {DetailsDialog}.
         * @param {DetailsDialog} dialog
         * @return {DetailsDialog} the updated dialog
         * @private
         */
        _applyDialogOptions: function(dialog) {
            this._dialogOptions.forEach(function(applyOption) {
                applyOption(dialog);
            });
            return dialog;
        },

        /**
         * Adds an empty dialog and returns it.
         *
         * @return {DetailsDialog} the attached dialog
         * @private
         */
        _addDialog: function() {
            var qualifierID = this._termID + "_" + this._qualifierNo++;
            return new DetailsDialog(qualifierID, this._dataName, this._dialogHolder).attach();
        },

        /**
         * Listens for click on the delete button.
         * @private
         */
        _addTermDeleteListener: function() {
            var _this = this;
            this._deleteButton.observe('click', function() {
                var idInput = _this._qualifiersContainer.down('input.term-id');
                var id = idInput && idInput.value;
                _this._clearDetails();
                _this._removeTermDeleteListener();
                _this._qualifiersContainer.remove();
                Event.fire(_this._qualifiersContainer, _this._dataName + ':term:deleted', {'id' : id});
            });
            this._deleteButtonHolder.show();
        },

        /**
         * Removes the click listener for delete button.
         * @private
         */
        _removeTermDeleteListener: function() {
            this._deleteButton.stopObserving('click');
            this._deleteButtonHolder.hide();
        },

        /**
         * Listens for term being selected.
         * @private
         */
        _addOnTermSelectListener: function() {
            var _this = this;
            var statusInput = this._qualifiersContainer.down('input.term-status');
            // IE fix.
            statusInput.observe('click', function(event) {
                event.stopPropagation();
            });
            statusInput.observe('change', function(event) {
                event.stop();
                if (statusInput.checked) {
                    _this._allowMultiDialogs && _this._addDetailsClickListener();
                    _this.addDialog(false);
                } else {
                    _this._removeDetailsClickListener();
                    _this.clearDetails();
                }
                Event.fire(_this._qualifiersContainer, _this._dataName + ':status:changed', {'id' : _this._termID});
            })
        },

        /**
         * Adds a listener for the add details button.
         * @private
         */
        _addDetailsClickListener: function() {
            var _this = this;
            this._addDetailsButton.observe('click', function(event) {
                event.stop();
                _this.addDialog(false)
            });
            this._addButtonHolder.show();
        },

        /**
         * Removes the listener for the add details button.
         * @private
         */
        _removeDetailsClickListener: function() {
            this._addDetailsButton.stopObserving('click');
            this._addButtonHolder.hide();
        },

        /**
         * Adds a listener for a dialog being deleted.
         * @private
         */
        _addDialogDeletedListener: function() {
            var _this = this;
            this._dialogHolder.observe(_this._dataName + ':dialog:deleted', function(event) {
                if (!this.down()) {
                    // Stop event propagation and clear everything.
                    event.stop();
                    _this.clearDetails();
                } else {
                    event.memo && event.memo.id && (delete _this._dialogMap[event.memo.id]);
                }
            });
        },

        _addDialogFocusManagers: function() {
            var _this = this;
            // Observe adding of a new dialog. Want to blur any focused dialogs and focus the added dialog.
            document.observe(this._dataName + ':dialog:added', function(event) {
                var addedDialog = event.memo && event.memo.element;
                if (!addedDialog) { return; }
                if (addedDialog.up('td.dialog-holder') === _this._dialogHolder) {
                    if (_this._crtFocus) {
                        _this._blur(_this._crtFocus);
                    }
                    _this._crtFocus = addedDialog;
                    _this._focus(_this._crtFocus);
                } else {
                    if (_this._crtFocus) {
                        _this._blur(_this._crtFocus);
                        _this._crtFocus = null;
                    }
                }
            });
            // Observe clicks.
            document.observe('click', function (event) {
                var clickedDialogHolder = event.findElement('td.dialog-holder');
                if (clickedDialogHolder === _this._dialogHolder) {
                    var summaryItem = event.findElement('div.summary-item');
                    if (_this._crtFocus) {
                        if (_this._crtFocus !== summaryItem) {
                            _this._blur(_this._crtFocus);
                            _this._crtFocus = summaryItem || null;
                            summaryItem && _this._focus(_this._crtFocus);
                        }
                    } else {
                        _this._crtFocus = summaryItem || null;
                        summaryItem && _this._focus(_this._crtFocus);
                    }
                } else {
                    if (_this._crtFocus && ('term-status' !== event.target.className)) {
                        // Blur anything in focus.
                        _this._blur(_this._crtFocus);
                        _this._crtFocus = null;
                    }
                }
            });
        },

        /**
         * Blurs a dialog element.
         *
         * @param elem a dialog element
         * @private
         */
        _blur: function(elem) {
            var elemID = elem.down('input').value;
            var dialog = this._dialogMap[elemID];
            if (dialog) {
                dialog.blur();
                Event.fire(this._qualifiersContainer, this._dataName + ':dialog:blurred');
            }
        },

        /**
         * Focuses a dialog element.
         *
         * @param elem a dialog element
         * @private
         */
        _focus: function(elem) {
            var elemID = elem.down('input').value;
            var dialog = this._dialogMap[elemID];
            if (dialog) {
                dialog.focus();
                Event.fire(this._qualifiersContainer, this._dataName + ':dialog:focused');
            }
        },

        /**
         * Attaches an observer to the _dialogHolder, that monitors any 'keyup' events on any object with
         * class name 'qualifier-notes'. Fires a custom events when notes are updated.
         * Code adapted from https://stackoverflow.com/a/4220182
         *
         * @private
         */
        _attachKeyUpObserver: function() {
            var _this = this;
            var typingTimer;
            this._dialogHolder.observe('keyup', function(event) {
                clearTimeout(typingTimer);
                typingTimer = setTimeout(doneTyping.bind(_this, event), _this._doneTypingInterval);
            });

            this._dialogHolder.observe('keydown', function() {
                clearTimeout(typingTimer);
            });

            var doneTyping = function(event) {
                var _this = this;
                event.target.hasClassName('qualifier-notes') &&
                  Event.fire(_this._qualifiersContainer, _this._dataName + ':notes:updated', {'target' : event.target});
            };
        }
    });
    return DetailsDialogGroup;
});
