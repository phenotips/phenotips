/**
 * DetailsDialogueGroup allows to create one or more instances of DetailsDialogue for some term.
 *
 * @class DetailsDialogueGroup
 * @constructor
 */
// TODO: Move and rename.
define([
        "pedigree/detailsDialogue"
    ], function(
        DetailsDialogue
    ){
    var DetailsDialogueGroup = Class.create({
        initialize: function (dataName, options) {
            this._dataName = dataName;
            // If this._allowMultiDialogues is true, more than one qualifier dialogue can be added per term.
            this._qualifierNo = 0;

            var groupOptions = options || {};
            this._allowMultiDialogues = groupOptions.allowMultiDialogues || false;
            this._disableTermDelete = groupOptions.disableTermDelete || false;
            this._doneTypingInterval = groupOptions.doneTypingInterval || 500;

            this._dialogueOptions = [];

            this._dialogueMap = {};

            this._crtFocus = null;

            // Builds an empty container for the term.
            this._buildEmptyContainer();

            // Attach listeners.
            this._addDialogueDeletedListener();
            this._addDialogueFocusManagers();
            this._attachKeyUpObserver();
        },

        getID: function() {
            return this._termID;
        },

        /**
         * Returns the constructed dialogue group element for the term.
         *
         * @return {Element|*} the dialogue group element for the term
         */
        get: function() {
            return this._qualifiersContainer;
        },

        /**
         * Associates the dialogue group with some term.
         *
         * @param label {String} the label for the term; if null or empty, will be set to ID
         * @param termID {String} the ID for the term; must not be null or empty
         * @param tooltip the class name of the tooltip to be created; null/undefined if no tooltip should be attached
         * @return {DetailsDialogueGroup} self
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

        dialoguesAddNumericSelect: function(options) {
            var addNumericSelect = function(currentDialogue) {
                currentDialogue.withNumericSelect(options);
            };
            this._dialogueOptions.push(addNumericSelect);
            return this;
        },

        dialoguesAddItemSelect: function(options) {
            var addItemSelect = function(currentDialogue) {
                currentDialogue.withItemSelect(options);
            };
            this._dialogueOptions.push(addItemSelect);
            return this;
        },

        dialoguesAddRadioList: function(collapsible, options) {
            var addRadioList = function(currentDialogue) {
                currentDialogue.withRadioList(collapsible, options);
            };
            this._dialogueOptions.push(addRadioList);
            return this;
        },

        dialoguesAddTextBox: function(collapsible, options) {
            var addTextBox = function(currentDialogue) {
                currentDialogue.withTextBox(collapsible, options);
            };
            this._dialogueOptions.push(addTextBox);
            return this;
        },

        dialoguesAddCustomElement: function(element, collapsible, options) {
            var addCustomElement = function(currentDialogue) {
                currentDialogue.withQualifierElement(element, collapsible, options);
            };
            this._dialogueOptions.push(addCustomElement);
            return this;
        },

        dialoguesAddDeleteAction: function() {
            var addDeleteAction = function(currentDialogue) {
                currentDialogue.withDeleteAction();
            };
            this._dialogueOptions.push(addDeleteAction);
            return this;
        },

        clearDialogueOptions: function() {
            this._dialogueOptions = [];
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
         * Sets as affected and adds an empty qualifier dialogue.
         *
         * @param silent true iff this should be performed silently, with no events fired
         */
        initAffected: function(silent) {
            this.affected(true);
            this.addDialogue(silent);
            this._allowMultiDialogues && this._addDetailsClickListener();
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
            for (var key in this._dialogueMap) {
                if (this._dialogueMap.hasOwnProperty(key)) {
                    qualifiers.push(this._dialogueMap[key].getValues());
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
         * Adds a qualifiers dialogue.
         *
         * @param silent true iff dialogue creation should be silent (that is, no event should be fired)
         * @return {DetailsDialogue} the dialogue just created
         */
        addDialogue: function(silent) {
            var dialogue = this._addDialogue();
            this._dialogueMap[dialogue.getID()] = dialogue;
            this._applyDialogueOptions(dialogue);
            !this._allowMultiDialogues && this._removeDetailsClickListener();
            this._dialogueHolder.show();
            if (!silent) {
                Event.fire(this._qualifiersContainer, this._dataName + ':dialogue:added', {'element': dialogue.getDialogue()});
            }
            return dialogue;
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
                _this.addDialogue(true).setValues(qualifier).blur();
            });
            this._allowMultiDialogues && this._addDetailsClickListener();
        },

        /**
         * Clears the details by removing all listeners, and emptying all containers and maps.
         * @private
         */
        _clearDetails: function() {
            this._qualifierNo = 0;
            this._dialogueHolder.descendants().forEach(function(elem) {
                elem.stopObserving();
            });
            this._dialogueMap = {};
            this._dialogueHolder.update();
            this._dialogueHolder.hide();
            this.affected(false);
            this._removeDetailsClickListener();
        },

        /**
         * Builds an empty container that will hold qualifiers dialogues for a term.
         * @private
         */
        _buildEmptyContainer: function() {
            this._qualifiersContainer = new Element('table', {'class' : 'summary-group ' + this._dataName + "-summary-group"});
            this._qualifiersContainer.name = this._dataName;

            this._dialogueHolder = new Element('td', {'class' : 'dialogue-holder'});

            var tbody = new Element('tbody')
              .insert(new Element('tr', {'class' : 'term-holder'})
                .insert(new Element('td')
                  .insert(new Element('span', {'class' : 'term-data'}))
                  .insert(new Element('span', {'class' : 'delete-button-holder'}))))
              .insert(new Element('tr')
                .insert(this._dialogueHolder))
              .insert(new Element('tr')
                .insert(new Element('td', {'class' : 'add-button-holder'})));

            this._qualifiersContainer.insert(tbody);
            this._dialogueHolder.hide();
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
         * Applies the predefined dialogue options onto some {DetailsDialogue}.
         * @param {DetailsDialogue} dialogue
         * @return {DetailsDialogue} the updated dialogue
         * @private
         */
        _applyDialogueOptions: function(dialogue) {
            this._dialogueOptions.forEach(function(applyOption) {
                applyOption(dialogue);
            });
            return dialogue;
        },

        /**
         * Adds an empty dialogue and returns it.
         *
         * @return {DetailsDialogue} the attached dialogue
         * @private
         */
        _addDialogue: function() {
            var qualifierID = this._termID + "_" + this._qualifierNo++;
            return new DetailsDialogue(qualifierID, this._dataName, this._dialogueHolder).attach();
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
                    _this._allowMultiDialogues && _this._addDetailsClickListener();
                    _this.addDialogue(false);
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
                _this.addDialogue(false)
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
         * Adds a listener for a dialogue being deleted.
         * @private
         */
        _addDialogueDeletedListener: function() {
            var _this = this;
            this._dialogueHolder.observe(_this._dataName + ':dialogue:deleted', function(event) {
                if (!this.down()) {
                    // Stop event propagation and clear everything.
                    event.stop();
                    _this.clearDetails();
                } else {
                    event.memo && event.memo.id && (delete _this._dialogueMap[event.memo.id]);
                }
            });
        },

        _addDialogueFocusManagers: function() {
            var _this = this;
            // Observe adding of a new dialogue. Want to blur any focused dialogues and focus the added dialogue.
            document.observe(this._dataName + ':dialogue:added', function(event) {
                var addedDialogue = event.memo && event.memo.element;
                if (!addedDialogue) { return; }
                if (addedDialogue.up('td.dialogue-holder') === _this._dialogueHolder) {
                    if (_this._crtFocus) {
                        _this._blur(_this._crtFocus);
                    }
                    _this._crtFocus = addedDialogue;
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
                var clickedDialogueHolder = event.findElement('td.dialogue-holder');
                if (clickedDialogueHolder === _this._dialogueHolder) {
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
         * Blurs a dialogue element.
         *
         * @param elem a dialogue element
         * @private
         */
        _blur: function(elem) {
            var elemID = elem.down('input').value;
            var dialogue = this._dialogueMap[elemID];
            if (dialogue) {
                dialogue.blur();
                Event.fire(this._qualifiersContainer, this._dataName + ':dialogue:blurred');
            }
        },

        /**
         * Focuses a dialogue element.
         *
         * @param elem a dialogue element
         * @private
         */
        _focus: function(elem) {
            var elemID = elem.down('input').value;
            var dialogue = this._dialogueMap[elemID];
            if (dialogue) {
                dialogue.focus();
                Event.fire(this._qualifiersContainer, this._dataName + ':dialogue:focused');
            }
        },

        /**
         * Attaches an observer to the _dialogueHolder, that monitors any 'keyup' events on any object with
         * class name 'qualifier-notes'. Fires a custom events when notes are updated.
         * Code adapted from https://stackoverflow.com/a/4220182
         *
         * @private
         */
        _attachKeyUpObserver: function() {
            var _this = this;
            var typingTimer;
            this._dialogueHolder.observe('keyup', function(event) {
                clearTimeout(typingTimer);
                typingTimer = setTimeout(doneTyping.bind(_this, event), _this._doneTypingInterval);
            });

            this._dialogueHolder.observe('keydown', function() {
                clearTimeout(typingTimer);
            });

            var doneTyping = function(event) {
                var _this = this;
                event.target.hasClassName('qualifier-notes') &&
                  Event.fire(_this._qualifiersContainer, _this._dataName + ':notes:updated', {'target' : event.target});
            };
        }
    });
    return DetailsDialogueGroup;
});
