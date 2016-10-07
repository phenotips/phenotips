/**
 * Base class for various "legend" widgets
 *
 * @class Legend
 * @constructor
 */

define([
        "pedigree/model/helpers"
    ], function(
        Helpers
    ){
    var Legend = Class.create( {

        initialize: function(title, droppableName, allowDrop, dropOnGroupNodes) {
            this._affectedNodes  = {};     // for each object: the list of affected person nodes

            this._objectColors = {};       // for each object: the corresponding object color

            this._preferredColors = {};    // in the future we'll have the ability to specify color
                                           // schemes, e.g. "green" for "that and that cancer", even
                                           // if that particular cancer is not yet present on the pedigree;
                                           // also used to assign the same disorder colors after save and load
            this._previousHighightedNode = null;

            var legendContainer = $('legend-container');
            if (legendContainer == undefined) {
              if (!editor.isReadOnlyMode()) {
                  this._legendInfo = new Element('div', {'class' : 'legend-box legend-info', id: 'legend-info'}).insert(
                     new Element('div', {'class' : 'infomessage'}).insert(
                       "You can drag and drop all items from the list(s) below onto individuals in the pedigree to mark them as affected.")
                  );
                  this.closeButton = new Element('span', {'class' : 'close-button'}).update('x');
                  this.closeButton.observe('click', this.hideDragHint.bindAsEventListener(this));
                  this._legendInfo.insert({'top': this.closeButton});
                  this._legendInfo.hide();
              }
              var legendContainer = new Element('div', {'class': 'legend-container field-no-user-select', 'id': 'legend-container'}).insert(this._legendInfo);
              editor.getWorkspace().getWorkArea().insert(legendContainer);
            } else {
              if (!editor.isReadOnlyMode()) {
                  this._legendInfo = legendContainer.down('#legend-info');
              }
            }

            this._legendBox = new Element('div', {'class' : 'legend-box', id: this._getPrefix() + '-legend-box'});
            this._legendBox.hide();
            legendContainer.insert(this._legendBox);

            this._droppableName = droppableName;

            var legendTitle= new Element('h2', {'class' : 'legend-title'}).update(title);
            this._legendBox.insert(legendTitle);

            this._list = new Element('ul', {'class' : this._getPrefix() +'-list abnormality-list'});
            this._legendBox.insert(this._list);

            Element.observe(this._legendBox, 'mouseover', function() {
                $$('.menu-box').invoke('setOpacity', .1);
            });
            Element.observe(this._legendBox, 'mouseout', function() {
                $$('.menu-box').invoke('setOpacity', 1);
            });

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
         * @return {String} some identifier which should be a valid HTML id value (e.g. no spaces)
         */
        _getPrefix: function(id) {
            // To be overwritten in derived classes
            throw "prefix not defined";
        },

        hideDragHint: function() {
            editor.getPreferencesManager().setConfigurationOption("user", "hideDraggingHint", true);
            this._legendInfo.hide();
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

        /*
         * Returns the map id->color of all the currently used colors.
         */
        getAllColors: function() {
            return this._objectColors;
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
         * Sets all preferred colors at once
         */
        setAllPreferredColors: function(allColors) {
            for (id in allColors) {
                if (allColors.hasOwnProperty(id)) {
                    this.addPreferredColor(id, allColors[id]);
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
        addCase: function(id, name, nodeID) {
            if(Object.keys(this._affectedNodes).length == 0) {
                this._legendBox.show();
                !editor.getPreferencesManager().getConfigurationOption("hideDraggingHint") &&
                    this._legendInfo && this._legendInfo.show();
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

                    var htmlElement = this._getListElementForObjectWithID(id)
                    htmlElement.remove();
                    if(Object.keys(this._affectedNodes).length == 0) {
                        this._legendBox.hide();
                        if (this._legendBox.up().select('.abnormality').size() == 0) {
                            this._legendInfo && this._legendInfo.hide();
                        }
                    }
                }
                else {
                    this._updateCaseNumbersForObject(id);
                }
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
            var item = new Element('li', {'class' : 'abnormality ' + 'drop-' + this._getPrefix(), 'id' : this._getPrefix() + '-' + HTMLid}).update(new Element('span', {'class' : 'disorder-name'}).update(name.escapeHTML()));
            item.insert(new Element('input', {'type' : 'hidden', 'value' : id}));
            var bubble = new Element('span', {'class' : 'abnormality-color'});
            bubble.style.backgroundColor = color;
            item.insert({'top' : bubble});
            var countLabel = new Element('span', {'class' : 'abnormality-cases'});
            var countLabelContainer = new Element('span', {'class' : 'abnormality-cases-container'}).insert("(").insert(countLabel).insert(")");
            item.insert(" ").insert(countLabelContainer);
            var me = this;
            Element.observe(item, 'mouseover', function() {
                //item.setStyle({'text-decoration':'underline', 'cursor' : 'default'});
                item.down('.disorder-name').setStyle({'background': color, 'cursor' : 'default'});
                me._highlightAllByItemID(id, true);
            });
            Element.observe(item, 'mouseout', function() {
                //item.setStyle({'text-decoration':'none'});
                item.down('.disorder-name').setStyle({'background':'', 'cursor' : 'default'});
                me._highlightAllByItemID(id, false);
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
            editor.getView().setCurrentDraggable(-1); // in drag mode but with no target
            var divPos = editor.getWorkspace().viewportToDiv(event.pointerX(), event.pointerY());
            var pos    = editor.getWorkspace().divToCanvas(divPos.x,divPos.y);
            var node   = editor.getView().getPersonNodeNear(pos.x, pos.y);
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
                this._onFailedDrag(node, "Can't drop onto this node: group nodes do not support " + this._droppableName, "Can't drag to this node");
                return false;
            }
            if (!editor.getPatientAccessPermissions(node.getPhenotipsPatientId()).hasEdit) {
                this._onFailedDrag(node, "You do not have edit right for this patient", "Can't drag to this patient");
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