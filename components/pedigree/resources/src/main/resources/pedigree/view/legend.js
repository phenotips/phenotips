/**
 * Base class for various abnormality "legend" widgets
 *
 * TODO: rename to AbnormalityLegend (since PatientDropLegend is not a subclas sof this) and move common
 *       functionality between PatientDropLegend and this into a new more generic Legend class
 *
 * @class Legend
 * @constructor
 */

define([
        "pedigree/pedigreeEditorParameters",
        "pedigree/model/helpers"
    ], function(
        PedigreeEditorParameters,
        Helpers
    ){
    var Legend = Class.create( {

        initialize: function(title, droppableName, allowDrop, dropOnGroupNodes, hideLegend) {
            this._invisible = hideLegend;

            this._affectedNodes  = {};     // for each object: the list of affected person nodes

            this._objectColors = {};       // for each object: the corresponding object color

            this._preferredColors = {};    // in the future we'll have the ability to specify color
                                           // schemes, e.g. "green" for "that and that cancer", even
                                           // if that particular cancer is not yet present on the pedigree;
                                           // also used to assign the same disorder colors after save and load

            this._objectProperties = {};    // for each object: properties, for now {"enabled": true/false}
            this._preferredProperties = {}; // used to save last used properties in case an abnormality is removed and then re-added

            this._previousHighightedNode = null;

            this._DISABLED_ICON         = "fa-circle-o";
            this._DISABLED_ONHOVER_ICON = "fa-check-circle-o";
            this._ENABLED_ICON          = "fa-circle";
            this._ENABLED_ONHOVER_ICON  = "fa-times-circle";
            this._HIGHLIGHTED_CSSCLASS  = "highlighted";

            var legendContainer = $('abnormalities-legend');
            if (legendContainer == undefined) {
              var legendContainer = new Element('div', {'class': 'abnormalities-legend generic-legend', 'id': 'abnormalities-legend'});
              legendContainer.style.maxWidth = PedigreeEditorParameters.attributes.legendMaxWidthPixels + "px";
              legendContainer.style.minWidth = PedigreeEditorParameters.attributes.legendMinWidthPixels + "px";
              legendContainer.addClassName("legend-hidden");

              if (!editor.isReadOnlyMode()) {
                  this._legendInfo = new Element('div', {'class' : 'legend-box legend-info', id: 'legend-info'}).insert(
                     new Element('div', {'class' : 'infomessage'}).insert(
                       "Drag and drop items from the list(s) below onto individuals to mark them as affected.")
                  );
                  this.closeButton = new Element('span', {'class' : 'close-button'}).update('x');
                  this.closeButton.observe('click', this.hideDragHint.bindAsEventListener(this));
                  this._legendInfo.insert({'top': this.closeButton});
                  if (editor.getPreferencesManager().getConfigurationOption("hideDraggingHint")) {
                      this._legendInfo.addClassName("legend-hidden");
                  }

                  // add maximize/compact legend button
                  this._legendBoxControls = new Element('div', {'class' : 'legend-box-controls-open', id: 'legend-box-controls'});
                  var minimizedLegendTitle = new Element('div', {'class': 'legend-minimized-title field-no-user-select'}).update("Legend").hide();
                  minimizedLegendTitle.hide();
                  var minimizeButton = new Element('span', {'class': 'fa fa-angle-double-up legend-box-button-right legend-action-minimize', 'title': "minimize"});
                  var maximizeButton = new Element('span', {'class': 'fa fa-angle-double-left legend-box-button-left legend-action-maximize legend-maximize-button', 'title': "expand", id: 'legend-maximize-button'});
                  maximizeButton.addClassName("legend-hidden");
                  this._legendBoxControls.update(minimizedLegendTitle).insert(minimizeButton).insert(maximizeButton);
                  var restoreLegend = function() {
                      minimizeButton.addClassName("legend-action-minimize");
                      minimizeButton.removeClassName("legend-action-restore");
                      minimizeButton.removeClassName("fa-angle-double-down");
                      minimizeButton.addClassName("fa-angle-double-up");
                      minimizedLegendTitle.hide();
                      maximizeButton.show();
                      minimizeButton.title = "minimize";
                      $$('.abnormalities-legend .legend-box').forEach(function(box) {
                          box.show();
                      });
                      legendContainer.stopObserving("click", restoreLegend);
                      $('legend-box-controls').removeClassName("legend-box-controls-closed");
                      $('legend-box-controls').addClassName("legend-box-controls-open");
                      legendContainer.style.minWidth = PedigreeEditorParameters.attributes.legendMinWidthPixels + "px";
                  }
                  minimizeButton.observe("click", function(ev) {
                      ev.stop();
                      if (minimizeButton.hasClassName("legend-action-minimize")) {
                          minimizeButton.removeClassName("legend-action-minimize");
                          minimizeButton.addClassName("legend-action-restore");
                          minimizeButton.removeClassName("fa-angle-double-up");
                          minimizeButton.addClassName("fa-angle-double-down");
                          minimizeButton.title = "restore";
                          minimizedLegendTitle.show();
                          maximizeButton.hide();
                          $$('.abnormalities-legend .legend-box').forEach(function(box) {
                              box.hide();
                          });
                          legendContainer.observe("click", restoreLegend);
                          $('legend-box-controls').removeClassName("legend-box-controls-open");
                          $('legend-box-controls').addClassName("legend-box-controls-closed");
                          legendContainer.style.minWidth = PedigreeEditorParameters.attributes.legendMinimizedWidthPixels + "px";
                      } else {
                          restoreLegend();
                      };
                  });
                  maximizeButton.observe("click", function() {
                      if (maximizeButton.hasClassName("legend-action-maximize")) {
                          maximizeButton.removeClassName("legend-action-maximize");
                          maximizeButton.addClassName("legend-action-shrink");
                          maximizeButton.removeClassName("fa-angle-double-left");
                          maximizeButton.addClassName("fa-angle-double-right");
                          maximizeButton.title = "shrink";
                          legendContainer.style.maxWidth = "10000px"; // setting to none/empty does not work
                          legendContainer.style.width = "auto";
                      } else {
                          maximizeButton.addClassName("legend-action-maximize");
                          maximizeButton.removeClassName("legend-action-shrink");
                          maximizeButton.removeClassName("fa-angle-double-right");
                          maximizeButton.addClassName("fa-angle-double-left");
                          maximizeButton.title = "expand";
                          legendContainer.style.maxWidth = PedigreeEditorParameters.attributes.legendMaxWidthPixels + "px";
                          legendContainer.style.width = "100%";
                      }
                  });

                  legendContainer.insert(this._legendBoxControls);
                  legendContainer.insert(this._legendInfo);
              }
              editor.getWorkspace().getLegendContainer().insert(legendContainer);
            } else {
              if (!editor.isReadOnlyMode()) {
                  this._legendInfo = legendContainer.down('#legend-info');
                  this._legendBoxControls = legendContainer.down('#legend-box-controls');
              }
            }

            this._legendBox = new Element('div', {'class' : 'legend-box', id: this._getPrefix() + '-legend-box'});
            this._legendBox.addClassName("legend-hidden");
            legendContainer.insert(this._legendBox);

            this._droppableName = droppableName;

            var _this = this;
            this._hideShowCheckbox = new Element('input', {'type': 'checkbox', 'class': 'legend-hide-show-checkbox'});
            this._hideShowCheckbox.observe("click", function() {
                $$('#' + _this._getPrefix() + '-legend-box .abnormality-legend-icon').forEach( function(bubble) {
                    if (_this._hideShowCheckbox.checked) {
                        bubble.enableColor(true);
                    } else {
                        bubble.disableColor(true);
                    }
                });
                _this._updateCheckboxTitle();
            });

            var legendTitle= new Element('h2', {'class' : 'legend-title field-no-user-select'}).update(title).insert(this._hideShowCheckbox);
            this._legendBox.insert(legendTitle);

            this._list = new Element('ul', {'class' : this._getPrefix() +'-list abnormality-list'});
            this._legendBox.insert(this._list);

            this._dropOnGroupNodes = dropOnGroupNodes;

            if (allowDrop) {
                Droppables.add(editor.getWorkspace().canvas, {accept:  'drop-'+this._getPrefix(),
                                                              onDrop:  this._onDropWrapper.bind(this),
                                                              onHover: this._onHoverWrapper.bind(this)});
            }
        },

        /**
         * Returns the prefix to be used on elements related to the object
         * (of type tracked by this legend) with the given id.
         *
         * @method _getPrefix
         * @param {String|Number} id ID of the object
         * @param {humanReadablePlural} (optional) when true, the prefix is the human readable string instead of the HTML-valid id value
         * @return {String} some identifier which should be a valid HTML id value (e.g. no spaces), unless
         *                  `humanReadablePlural` is true, in which case it should be a plural form of the legend's object name
         */
        _getPrefix: function(humanReadablePlural) {
            // To be overwritten in derived classes
            throw "prefix not defined";
        },

        hideDragHint: function() {
            editor.getPreferencesManager().setConfigurationOption("user", "hideDraggingHint", true);
            this._legendInfo.addClassName("legend-hidden");
            if ($('abnormalities-legend').offsetWidth < PedigreeEditorParameters.attributes.legendMaxWidthPixels) {
                $('legend-maximize-button').addClassName("legend-hidden");
            }
        },

        setObjectColor: function(id, color) {
            this._objectColors[id] = color;
            // set the last assigned color for the object
            this.addPreferredColor(id, color);
        },

        /**
         * Retrieve the color associated with the given object
         *
         * @method getObjectColor
         * @param {String|Number} id ID of the object
         * @return {String} CSS color value for the object, displayed on affected nodes in the pedigree and in the legend
         */
        getObjectColor: function(id) {
            if (!this._objectColors.hasOwnProperty(id))
                return "#ff0000";
            return this._objectColors[id];
        },

        /**
         * Returns an array of colors.
         * @param nodeId if defined and not null, filters colors only for abnormalities affecting given node
         */
        getAllEnabledColorsForNode: function(nodeId) {
            var enabledColors = [];
            for (var id in this._objectColors) {
                if (this._objectColors.hasOwnProperty(id) && this.getObjectProperties(id).enabled) {
                    if (nodeId === undefined || Helpers.arrayContains(this._affectedNodes[id], nodeId)) {
                        enabledColors.push(this._objectColors[id]);
                    }
                }
            }
            return enabledColors;
        },

        /**
         * Returns a map of abnormalities
         */
        getAllNames: function() {
            var result = {};
            for (var abnormality in this._affectedNodes) {
                if (this._affectedNodes.hasOwnProperty(abnormality)) {
                    result[abnormality] = this.getName(abnormality);
                }
            }
            return result;
        },

        /**
         * Returns an abnormality name
         *
         * @param {String|Number} id ID of the abnormality
         * @return {String} associated name for the abnormality, displayed on affected nodes in the pedigree and in the legend
         */
        getName: function(id) {
            return id;
        },

        /**
         * Retrieve the properties associated with the given abnormality.
         *
         * @method getObjectProperties
         * @param {String|Number} id ID of the object
         * @return {Object} Properties of the object
         */
        getObjectProperties: function(id) {
            if (!this._objectProperties.hasOwnProperty(id))
                return {"enabled": true};
            return this._objectProperties[id];
        },

        /**
         * Set the default preferences for object object with the given id.
         */
        setDefaultProperties: function(id, preferences) {
            this._preferredProperties[id] = preferences;
        },

        /*
         * Returns the map id -> {"color": color, "properties": properties} object of all abnormalities.
         */
        getAllSettings: function() {
            var settings = {};
            for (var id in this._objectColors) {
                if (this._objectColors.hasOwnProperty(id)) {
                    settings[id] = {"color": this._objectColors[id],
                                    "name": this.getName(id),
                                    "properties": this.getObjectProperties(id) };
                }
            }
            return settings;
        },

        /**
         * Restores settings as of last getAllSettings() call. Only default preferences
         * are restored, any existing abnormallities won't be affected.
         */
        setAllSettings: function(settings) {
            for (var id in settings) {
                if (settings.hasOwnProperty(id)) {
                    this.addPreferredColor(id, settings[id].color);
                    this.setDefaultProperties(id, settings[id].properties);
                }
            }
        },

        /**
         * Set the preferred color for object with the given id. No check is performed to make
         * sure colors are unique or that object is present in the legend.
         */
        addPreferredColor: function(id, color) {
            this._preferredColors[id] = color;
        },

        /**
         * Get the preferred color for object with the given id. If the color is already
         * there is no guarantee as to what color will be used.
         */
        getPreferedColor: function(id) {
            if (this._preferredColors.hasOwnProperty(id)) {
                return this._preferredColors[id];
            }
            return null;
        },

        /**
         * Returns True if there are nodes reported to have the object with the given id
         *
         * @method _hasAffectedNodes
         * @param {String|Number} id ID of the object
         * @private
         */
        _hasAffectedNodes: function(id) {
            return this._affectedNodes.hasOwnProperty(id);
        },

        /**
         * Registers an occurrence of an object type being tracked by this legend.
         *
         * @method addCase
         * @param {String|Number} id ID of the object
         * @param {String} Name The description of the object to be displayed
         * @param {Number} nodeID ID of the Person who has this object associated with it
         */
        addCase: function(id, name, nodeID, disableByDefault) {
            if (!this._preferredProperties.hasOwnProperty(id)) {
                this._preferredProperties[id] = {"enabled":  disableByDefault ? false : true}
            }
            this._objectProperties[id] = this._preferredProperties[id];

            if(!this._invisible && Object.keys(this._affectedNodes).length == 0) {
                $('abnormalities-legend').removeClassName("legend-hidden");
                this._legendBox.removeClassName("legend-hidden");
            }
            if(!this._hasAffectedNodes(id)) {
                this._affectedNodes[id] = [nodeID];
                var listElement = this._generateElement(id, name);
                this._list.insert(listElement);
            }
            else {
                this._affectedNodes[id].push(nodeID);
            }
            this._updateCaseNumbersForObject(id);
            this._updateMinMaxButtons();
            this._updateShowHideButtons();
        },

        highlightObjectsForNode: function(nodeId) {
            for (var id in this._objectColors) {
                if (this._objectColors.hasOwnProperty(id)) {
                    var htmlElement = this._getListElementForObjectWithID(id);
                    if (Helpers.arrayContains(this._affectedNodes[id], nodeId)) {
                        htmlElement.addClassName(this._HIGHLIGHTED_CSSCLASS);
                    } else {
                        htmlElement.removeClassName(this._HIGHLIGHTED_CSSCLASS);
                    }
                }
            }
        },

        unhighlightAllObjects: function() {
            for (var id in this._objectColors) {
                if (this._objectColors.hasOwnProperty(id)) {
                    var htmlElement = this._getListElementForObjectWithID(id);
                    htmlElement.removeClassName(this._HIGHLIGHTED_CSSCLASS);
                }
            }
        },

        /**
         * Removes an occurrence of an object, if there are any. Removes the object
         * from the 'Legend' box if this object is not registered in any individual any more.
         *
         * @param {String|Number} id ID of the object
         * @param {Number} nodeID ID of the Person who has/is affected by this object
         */
        removeCase: function(id, nodeID) {
            if (this._hasAffectedNodes(id)) {
                this._affectedNodes[id] = this._affectedNodes[id].without(nodeID);
                if(this._affectedNodes[id].length == 0) {
                    delete this._affectedNodes[id];
                    delete this._objectColors[id];
                    delete this._objectProperties[id];

                    var htmlElement = this._getListElementForObjectWithID(id)
                    htmlElement.remove();
                    if(Object.keys(this._affectedNodes).length == 0) {
                        this._legendBox.addClassName("legend-hidden");
                        if (this._legendBox.up().select('.abnormality').size() == 0) {
                            $('abnormalities-legend').addClassName("legend-hidden");
                        }
                    }
                }
                else {
                    this._updateCaseNumbersForObject(id);
                }
                this._updateMinMaxButtons();
                this._updateShowHideButtons();
            }
        },

        _updateMinMaxButtons: function() {
            // check if the legend would expand itself if given more space. If it does, add an "expand" button
            var legendContainer = $('abnormalities-legend');
            var currentMaxWidth = legendContainer.style.maxWidth;
            var currentWidth = legendContainer.offsetWidth;

            legendContainer.style.maxWidth = "10000px";
            if (legendContainer.offsetWidth <= currentWidth) {
                $('legend-maximize-button').addClassName("legend-hidden");
            } else {
                $('legend-maximize-button').removeClassName("legend-hidden");
            }

            legendContainer.style.maxWidth = currentMaxWidth;
        },

        _updateShowHideButtons: function() {
            // hide or show "show all"/"hide all" buttons dependin gon if all/some/none of the abnormalities
            // in this category are shown/hidden
            var someEnabled = false;
            var allEnabled = true;
            for (var id in this._objectProperties) {
                if (this._objectProperties.hasOwnProperty(id)) {
                    if (this._objectProperties[id].enabled) {
                        someEnabled = true;
                    } else {
                        allEnabled = false;
                    }
                }
            }
            if (allEnabled) {
                this._hideShowCheckbox.checked = true;
                this._hideShowCheckbox.indeterminate = false;
            } else if (someEnabled) {
                this._hideShowCheckbox.checked = false;
                this._hideShowCheckbox.indeterminate = true;
            } else {
                this._hideShowCheckbox.checked = false;
                this._hideShowCheckbox.indeterminate = false;
            }
            this._updateCheckboxTitle();
        },

        _updateCheckboxTitle: function() {
            if (this._hideShowCheckbox.checked) {
                this._hideShowCheckbox.title = "Disable colouring of all " + this._getPrefix(true);
            } else {
                this._hideShowCheckbox.title = "Enable colouring of all " + this._getPrefix(true);
            }
        },

        /**
         * Updates internal references to nodes when node ids is/are changed (e.g. after a node deletion)
         */
        replaceIDs: function(changedIdsSet) {
            for (var abnormality in this._affectedNodes) {
                if (this._affectedNodes.hasOwnProperty(abnormality)) {

                    var affectedList = this._affectedNodes[abnormality];

                    for (var i = 0; i < affectedList.length; i++) {
                        var oldID = affectedList[i];
                        var newID = changedIdsSet.hasOwnProperty(oldID) ? changedIdsSet[oldID] : oldID;
                        affectedList[i] = newID;
                    }
                }
            }
        },

        _getListElementForObjectWithID: function(id) {
            var HTMLid = Helpers.isInt(id) ? id : this._hashID(id);
            return $(this._getPrefix() + '-' + HTMLid);
        },

        /**
         * Updates the displayed number of nodes assocated with/affected by the object
         *
         * @method _updateCaseNumbersForObject
         * @param {String|Number} id ID of the object
         * @private
         */
        _updateCaseNumbersForObject : function(id) {
          var HTMLid = Helpers.isInt(id) ? id : this._hashID(id);
          var label = this._legendBox.down('li#' + this._getPrefix() + '-' + HTMLid + ' .abnormality-cases');
          if (label) {
            var cases = this._affectedNodes.hasOwnProperty(id) ? this._affectedNodes[id].length : 0;
            label.update(cases + "&nbsp;case" + ((cases - 1) && "s" || ""));
          }
        },

        /**
         * Generate the element that will display information about the given object in the legend
         *
         * @method _generateElement
         * @param {String|Number} id ID of the object
         * @param {String} name The human-readable object name or description
         * @return {HTMLLIElement} List element to be insert in the legend
         */
        _generateElement: function(id, name) {
            var color = this.getObjectColor(id);
            var HTMLid = Helpers.isInt(id) ? id : this._hashID(id);
            var item = new Element('li', {'class' : 'abnormality ' + 'drop-' + this._getPrefix(),
                'id' : this._getPrefix() + '-' + HTMLid,
                'title': "Click to enable or disable node colouring; drag to mark other individuals as affected"})
                .update(new Element('span', {'class' : 'abnormality-' + this._getPrefix() + '-name'}).update(name.escapeHTML()));
            item.insert(new Element('input', {'type' : 'hidden', 'value' : id}));
            var iconClass = this._objectProperties[id].enabled ? this._ENABLED_ICON : this._DISABLED_ICON;
            var bubble = new Element('span', {'class' : 'abnormality-legend-icon fa ' + iconClass + ' icon-enabled-' + this._objectProperties[id].enabled.toString()});
            bubble.style.color = this._objectProperties[id].enabled ? color : PedigreeEditorParameters.attributes.legendIconDisabledColor;
            var _this = this;
            bubble.disableColor = function(skipMasterCheckboxUpdate) {
                _this._objectProperties[id].enabled = false;
                _this._preferredProperties[id].enabled = false;
                bubble.removeClassName(_this._ENABLED_ICON);
                bubble.removeClassName(_this._ENABLED_ONHOVER_ICON);
                bubble.addClassName(_this._DISABLED_ICON);
                bubble.removeClassName("icon-enabled-true");
                bubble.addClassName("icon-enabled-false");
                bubble.style.color = PedigreeEditorParameters.attributes.legendIconDisabledColor;
                for (var i = 0; i < _this._affectedNodes[id].length; i++) {
                    editor.getNode(_this._affectedNodes[id][i]).getGraphics().updateDisorderShapes();
                }
                !skipMasterCheckboxUpdate && _this._updateShowHideButtons();
            }
            bubble.enableColor = function(skipMasterCheckboxUpdate) {
                _this._objectProperties[id].enabled = true;
                _this._preferredProperties[id].enabled = true;
                bubble.removeClassName(_this._DISABLED_ICON);
                bubble.removeClassName(_this._DISABLED_ONHOVER_ICON);
                bubble.addClassName(_this._ENABLED_ICON);
                bubble.removeClassName("icon-enabled-false");
                bubble.addClassName("icon-enabled-true");
                bubble.style.color = color;
                for (var i = 0; i < _this._affectedNodes[id].length; i++) {
                    editor.getNode(_this._affectedNodes[id][i]).getGraphics().updateDisorderShapes();
                }
                !skipMasterCheckboxUpdate && _this._updateShowHideButtons();
            }
            bubble.hideOnHoverIcons = function() {
                if (_this._objectProperties[id].enabled) {
                    bubble.removeClassName(_this._ENABLED_ONHOVER_ICON);
                    bubble.addClassName(_this._ENABLED_ICON);
                } else {
                    bubble.removeClassName(_this._DISABLED_ONHOVER_ICON);
                    bubble.addClassName(_this._DISABLED_ICON);
                }
            }
            bubble.showOnHoverIcons = function() {
                if (_this._objectProperties[id].enabled) {
                    bubble.removeClassName(_this._ENABLED_ICON);
                    bubble.addClassName(_this._ENABLED_ONHOVER_ICON);
                } else {
                    bubble.removeClassName(_this._DISABLED_ICON);
                    bubble.addClassName(_this._DISABLED_ONHOVER_ICON);
                }
            }
            // Need to distonguish click from drag in legend
            // (based on http://stackoverflow.com/questions/6042202/how-to-distinguish-mouse-click-and-drag)
            item.observe("mousedown", function(evt) {
                item.clickStartX = evt.pageX;
                item.clickStartY = evt.pageY;
                item.moveX = 0;
                item.moveY = 0;
            });
            item.observe("mousemove", function(evt) {
                var movedX = Math.abs(item.clickStartX - evt.pageX);
                var movedY = Math.abs(item.clickStartY - evt.pageY);
                if (movedX > item.moveX) {
                    item.moveX = movedX;
                }
                if (movedY > item.moveY) {
                    item.moveY = movedY;
                }
            });
            item.observe("mouseup", function(evt) {
                if (item.moveX <= PedigreeEditorParameters.attributes.dragThresholdPixels &&
                    item.moveY <= PedigreeEditorParameters.attributes.dragThresholdPixels) {
                    if (bubble.hasClassName("icon-enabled-true")) {
                        bubble.disableColor();
                    } else {
                        bubble.enableColor();
                    }
                    bubble.showOnHoverIcons();
                } else {
                    bubble.hideOnHoverIcons();
                }
            });
            item.insert({'top' : bubble});
            var countLabel = new Element('span', {'class' : 'abnormality-cases'});
            var countLabelContainer = new Element('span', {'class' : 'abnormality-cases-container'}).insert("(").insert(countLabel).insert(")");
            item.insert(" ").insert(countLabelContainer);
            Element.observe(item, 'mouseover', function() {
                item.down('.abnormality-' + _this._getPrefix() + '-name').setStyle({'cursor' : 'pointer'});
                _this._highlightAllByItemID(id, true);
                bubble.showOnHoverIcons();
            });
            Element.observe(item, 'mouseout', function() {
                item.down('.abnormality-' + _this._getPrefix() + '-name').setStyle({'cursor' : 'default'});
                _this._highlightAllByItemID(id, false);
                bubble.hideOnHoverIcons();
            });
            new Draggable(item, {
                revert: true,
                reverteffect: function(segment) {
                // Reset the in-line style.
                  segment.setStyle({
                    height: '',
                    left: '',
                    position: '',
                    top: '',
                    zIndex: '',
                    width: ''
                  });
                },
                ghosting: true
              });
            return item;
        },

        _highlightAllByItemID: function(id, highlight) {
            if (editor.getView().getCurrentDraggable() == null) {
                this._affectedNodes[id] && this._affectedNodes[id].forEach(function(nodeID) {
                    var node = editor.getNode(nodeID);
                    if (node) {
                        if (highlight) {
                            node.getGraphics().highlight();
                        } else {
                            node.getGraphics().unHighlight()
                        }
                    }
                });
            }
        },

        /**
         * Callback for dragging an object from the legend onto nodes. Converts canvas coordinates
         * to nodeID and calls the actual drop holder once the grunt UI work is done.
         *
         * @method _onDropWrapper
         * @param {HTMLElement} [label]
         * @param {HTMLElement} [target]
         * @param {Event} [event]
         * @private
         */
        _onDropWrapper: function(label, target, event) {
            if (editor.isReadOnlyMode()) {
                return;
            }
            editor.getView().setCurrentDraggable(null);
            var id = label.select('input')[0].value;
            this._highlightAllByItemID(id, false); // remove highlight
            this._unhighlightAfterDrag();
            var divPos = editor.getWorkspace().viewportToDiv(event.pointerX(), event.pointerY());
            var pos    = editor.getWorkspace().divToCanvas(divPos.x,divPos.y);
            var node   = editor.getView().getPersonNodeNear(pos.x, pos.y);
            var _this  = this;
            //console.log("Position x: " + pos.x + " position y: " + pos.y);
            if (node) {
                this._onDropObject(node, id);
            }
        },

        _onFailedDrag: function(node, message, title) {
            editor.getOkCancelDialogue().showCustomized(message, title, "OK", function() {
                node.getGraphics().getHoverBox().animateHideHoverZone();
            });
        },

        /**
         * Callback for moving around/hovering an object from the legend over nodes. Converts canvas coordinates
         * to nodeID and calls the actual drop holder once the grunt UI work is done.
         *
         * @method _onHoverWrapper
         * @param {HTMLElement} [label]
         * @param {HTMLElement} [target]
         * @param {int} [the percentage of overlapping]
         * @private
         */
        _onHoverWrapper: function(label, target, overlap, event) {
            if (editor.isReadOnlyMode()) {
                return;
            }
            editor.getNodeMenu().hide();
            editor.getView().setCurrentDraggable(-1); // in drag mode but with no target

            // PhenoTips uses a modified version of the dragdrop.js library. The original does not pass the event into this method
            if (event) {
                var divPos = editor.getWorkspace().viewportToDiv(event.pointerX(), event.pointerY());
                var pos    = editor.getWorkspace().divToCanvas(divPos.x,divPos.y);
                var node   = editor.getView().getPersonNodeNear(pos.x, pos.y);
            } else {
                var node = undefined;
            }
            if (node) {
                node.getGraphics().getHoverBox().animateHideHoverZone();
                node.getGraphics().getHoverBox().setHighlighted(true);
                this._previousHighightedNode = node;
            } else {
                this._unhighlightAfterDrag();
            }
        },

        _unhighlightAfterDrag: function() {
            if (this._previousHighightedNode) {
                this._previousHighightedNode.getGraphics().getHoverBox().setHighlighted(false);
                this._previousHighightedNode = null;
             }
        },

        /**
         * Callback for dragging an object from the legend onto nodes
         *
         * @method _onDropGeneric
         * @param {Person} Person node
         * @param {String|Number} id ID of the object
         */
        _onDropObject: function(node, objectID) {
            if (!this._dropOnGroupNodes && node.isPersonGroup()) {
                this._onFailedDrag(node, this._droppableName + " cannot be assigned to groups of individuals.", "Unsupported operation");
                return false;
            }
            if (!editor.getPatientAccessPermissions(node.getPhenotipsPatientId()).hasEdit) {
                this._onFailedDrag(node, "You do not have the permission to modify this patient record.", "Unauthorized operation");
                return false;
            }
            return true;
        },

        /*
         * IDs are used as part of HTML IDs in the Legend box, which breaks when IDs contain some non-alphanumeric symbols.
         * For that purpose these symbols in IDs are converted in memory (but not in the stored pedigree) to a numeric value.
         *
         * @method _hashID
         * @param {id} ID string to be converted
         * @return {int} Hashed integer representation of input string
         */
        _hashID : function(s){
          s.toLowerCase();
          if (!Array.prototype.reduce) {
              var n = 0;
              for (var i = 0; i < s.length; i++) {
                  n += s.charCodeAt(i);
              }
              return "c" + n;
          }
          return "c" + s.split("").reduce(function(a, b) {
             a = ((a << 5) - a) + b.charCodeAt(0);
            return a & a;
          }, 0);
        }
    });
    return Legend;
});
