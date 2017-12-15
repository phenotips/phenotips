/**
 * Class responsible for display and drop patients to the pedigree tree nodes
 *
 * @class PatientDropLegend
 * @constructor
 */
define([
        "pedigree/pedigreeEditorParameters",
        "pedigree/model/helpers"
    ], function(
        PedigreeEditorParameters,
        Helpers
    ){
    var PatientDropLegend = Class.create( {

        initialize: function() {
            if (editor.isReadOnlyMode()) {
                return;
            }

            this._notLinkedPatients  = {};

            this._dragInfo = new Element('div', {'class' : 'legend-box legend-info', id: 'legend-info'}).insert(
                    new Element('div', {'class' : 'infomessage no-word-wrap'}).insert(
                      "Drag and drop onto the pedigree")
                 );
            var closeButton = new Element('span', {'class' : 'close-button'}).update('x');
            closeButton.observe('click', this.hidePatientDragHint.bindAsEventListener(this));
            this._dragInfo.insert({'top': closeButton});
            if (editor.getPreferencesManager().getConfigurationOption("hidePatientDraggingHint")) {
                this._dragInfo.addClassName("legend-hidden");
            }

            // only need this for 1.3.x
            this._legendInfo = new Element('div', {'class' : 'legend-box legend-info', id: 'legend-info'}).insert(
                    new Element('div', {'class' : 'warningmessage legend-warning'}).insert(
                      "All patients listed here will be removed from the pedigree when pedigree is saved")
                 );

            this._legendBoxControls = new Element('div', {'class' : 'legend-box-controls-open', id: 'patient-legend-box-controls'});
            var minimizedLegendTitle = new Element('div', {'class': 'legend-minimized-title field-no-user-select'}).update("Other Patients").hide();
            minimizedLegendTitle.hide();
            var minimizeButton = new Element('span', {'class': 'fa fa-angle-double-up legend-box-button-right legend-action-minimize', 'title': "minimize"});
            this._legendBoxControls.update(minimizedLegendTitle).insert(minimizeButton);
            var restoreLegend = function() {
                minimizeButton.addClassName("legend-action-minimize");
                minimizeButton.removeClassName("legend-action-restore");
                minimizeButton.removeClassName("fa-angle-double-down");
                minimizeButton.addClassName("fa-angle-double-up");
                minimizedLegendTitle.hide();
                minimizeButton.title = "minimize";
                $$('.patient-assign-legend .legend-box').forEach(function(box) {
                    box.show();
                });
                $('patient-legend-box-controls').removeClassName("legend-box-controls-closed");
                $('patient-legend-box-controls').addClassName("legend-box-controls-open");
                $('patient-assign').stopObserving("click", restoreLegend);
                $('patient-assign').style.minWidth = PedigreeEditorParameters.attributes.legendMinWidthPixels + "px";
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
                    $$('.patient-assign-legend .legend-box').forEach(function(box) {
                        box.hide();
                    });
                    $('patient-legend-box-controls').removeClassName("legend-box-controls-open");
                    $('patient-legend-box-controls').addClassName("legend-box-controls-closed");
                    $('patient-assign').observe("click", restoreLegend);
                    $('patient-assign').style.minWidth = PedigreeEditorParameters.attributes.legendMinimizedWidthPixels + "px";
                } else {
                    restoreLegend();
                };
            });

            this.legendContainer = new Element('div', {'class' : 'patient-assign-legend generic-legend', id: 'patient-assign'})
                                   .insert(this._legendBoxControls)
                                   .insert(this._legendInfo)
                                   .insert(this._dragInfo);
            this.legendContainer.hide();

            this._list_current = new Element('ul', {'class' : 'patient-list new'});
            this._list_unlinked = new Element('ul', {'class' : 'patient-list unlinked'});
            var l_box = new Element('div', {'class' : 'legend-box', 'id' : 'list_current'});
            l_box.insert(new Element('h2', {'class' : 'legend-title'}).update("Current patient: "));
            l_box.insert(this._list_current);
            this.legendContainer.insert(l_box);
            var lg_box = new Element('div', {'class' : 'legend-box', 'id' : 'list_unlinked'});
            lg_box.insert(new Element('h2', {'class' : 'legend-title'}).update("Other family members: "));
            lg_box.insert(this._list_unlinked);
            this.legendContainer.insert(lg_box);

            editor.getWorkspace().getLegendContainer().insert(this.legendContainer);
            $('list_current').addClassName("legend-hidden");
            $('list_unlinked').addClassName("legend-hidden");

            Droppables.add(editor.getWorkspace().canvas, {accept:  'drop-patient',
                                                          onDrop:  this._onDropWrapper.bind(this),
                                                          onHover: this._onHoverWrapper.bind(this)});

            // add patient to a legend on unlink patient from node event
            document.observe('pedigree:patient:unlinked', function (event) {
                    this.addCase(event.memo.phenotipsID, event.memo.type, event.memo.gender, event.memo.firstName, event.memo.lastName, event.memo.externalID);
            }.bind(this));

            var removeCase = this.removeCase.bind(this);

            // remove patient from a legend on link patient to node event
            document.observe('pedigree:patient:linked', function (event){
                removeCase(event.memo.phenotipsID);
            });

            // remove patient from a legend on patient record deleted event
            document.observe('pedigree:patient:deleted', function (event){
                removeCase(event.memo.phenotipsPatientID);
            });
        },

        hidePatientDragHint: function() {
            editor.getPreferencesManager().setConfigurationOption("user", "hidePatientDraggingHint", true);
            this._dragInfo.addClassName("legend-hidden");
        },

        hide: function() {
            this.legendContainer.hide();
        },

        show: function() {
            if (!this._hasPatients()) {
                this.legendContainer.show();
            }
        },

        removeCase: function(phenotipsPatientID) {
            if (this._notLinkedPatients.hasOwnProperty(phenotipsPatientID)) {
                this._deletePatientElement(phenotipsPatientID);
                delete this._notLinkedPatients[phenotipsPatientID];
            }
        },

        /**
         * Add patient to a legend
         *
         * @method addCase
         **/
        // TODO: replace pedigreeProperties with patientJSON and remove gender/name/extID parameters (since that data is
        //       contained in PatientJSON.
        //       For now this is left as is to simplify porting to 1.3.x where patientJSON is not available
        addCase: function(phenotipsPatientID, type, gender, firstName, lastName, externalID, pedigreeProperties) {

            if (!this._notLinkedPatients.hasOwnProperty(phenotipsPatientID)) {
                // if data about this patient is not available need to load it
                if (gender === undefined) {
                    this._loadPatientInfoAndAddToLegend(phenotipsPatientID, type);
                    return;
                }

                var name = firstName + " " + lastName;

                this._notLinkedPatients[phenotipsPatientID] = {"type" : type,
                                                               "phenotipsID": phenotipsPatientID,
                                                               "gender": gender,
                                                               "name":  name,
                                                               "externalID": externalID,
                                                               "pedigreeProperties": pedigreeProperties };

                var listElement = this._generateElement(this._notLinkedPatients[phenotipsPatientID]);
                if (phenotipsPatientID == editor.getGraph().getCurrentPatientId()) {
                    this._list_current.insert(listElement);
                    this.assignNewPatientId = phenotipsPatientID;
                    $('list_current').removeClassName("legend-hidden");
                } else {
                    this._list_unlinked.insert(listElement);
                    $('list_unlinked').removeClassName("legend-hidden");
                }
            }

            // show legend in any case when addCase() is invoked
            this.legendContainer.show();
        },

        _loadPatientInfoAndAddToLegend: function(phenotipsPatientID, type) {
            var _this = this;

            var patientDataJsonURL = editor.getExternalEndpoint().getLoadPatientDataJSONURL([phenotipsPatientID]);

            new Ajax.Request(patientDataJsonURL, {
                method: "GET",
                onSuccess: function (response) {
                    if (response.responseJSON) {
                      var patient = response.responseJSON[phenotipsPatientID];
                      var firstName = patient.hasOwnProperty('patient_name') && patient.patient_name.hasOwnProperty("first_name")
                                      ? patient.patient_name.first_name.trim() : "";
                      var lastName  = patient.hasOwnProperty('patient_name') && patient.patient_name.hasOwnProperty("last_name")
                                      ? patient.patient_name.last_name.trim() : "";
                      _this.addCase(phenotipsPatientID, type, patient.sex, firstName, lastName, patient.external_id);
                    }
                }
            });
        },

        /**
         * Returns the list of unassigned patients that are in the in the patient legend
         *
         * @method getListOfPatientsInTheLegend
         * @return {patientList} List of patients in the legend
         */
        getListOfPatientsInTheLegend: function() {
          var patientList = [];
          for (var patient in this._notLinkedPatients) {
            if (this._notLinkedPatients.hasOwnProperty(patient)) {
              patientList.push(patient);
            }
          }
          return patientList;
        },

        /**
         * Generate the element that will display information about the given patient in the patient legend
         *
         * @method _generateElement
         * @return {HTMLLIElement} List element to be insert in the legend
         * @private
         */
        _generateElement: function(patientElement) {
            var shape = 'circle';
            if (patientElement.gender == "M"){
                 shape = 'square';
            }
            if (patientElement.gender == "U" || patientElement.gender == "O"){
                 shape = 'diamond';
            }

            var hasName       = patientElement.name && /\S/.test(patientElement.name);
            var hasExternalID = patientElement.externalID && /\S/.test(patientElement.externalID);

            var createAnnotationDiv = function(label, value, fixedWidth) {
                if (value.length > 20) {
                    // to make sure patient legend is not too wide
                    value = value.substring(0, 18) + "...";
                }
                var innerDiv = new Element('div', {'class' : 'patient-legend-details-label' + (fixedWidth ? ' min-column-width-45' : '')})
                               .insert(label);
                var outerDiv = new Element('div', {'class' : 'patient-legend-details'}).insert(innerDiv).insert(value);
                return outerDiv;
            };

            var patientData = new Element('div', {'class': 'patient-legend-patient'}).update(patientElement.phenotipsID);
            if (hasName) {
                patientData.insert(createAnnotationDiv('name:', patientElement.name, hasExternalID));
            }
            if (hasExternalID) {
                patientData.insert(createAnnotationDiv('id:', patientElement.externalID, hasName));
            }

            var item = new Element('li', {'class' : 'abnormality drop-patient', 'id' : patientElement.phenotipsID})
                       .insert(new Element('span', {'class' : shape}))
                       .insert(patientData);

            item.insert(new Element('input', {'type' : 'hidden', 'value' : patientElement.phenotipsID}));
            var _this = this;
            Element.observe(item, 'mouseover', function() {
                _this._highlightDropTargets(patientElement, true);
            });
            Element.observe(item, 'mouseout', function() {
                _this._highlightDropTargets(patientElement, false);
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

        _highlightDropTargets: function(patient, isOn) {
            if (editor.getView().getCurrentDraggable() != null) {
                return;
            }
            //console.log("null");
            this.validTargets = editor.getGraph().getPossiblePatientIDTarget();
            this.validTargets.forEach(function(nodeID) {
                var node = editor.getNode(nodeID);
                if (node) {
                    if (isOn) {
                        node.getGraphics().highlight();
                    } else {
                        node.getGraphics().unHighlight()
                    }
                }
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
            var patient = this._notLinkedPatients[id];

            this._highlightDropTargets(patient, false);
            this._unhighlightAfterDrag();

            var divPos = editor.getWorkspace().viewportToDiv(event.pointerX(), event.pointerY());
            var pos    = editor.getWorkspace().divToCanvas(divPos.x,divPos.y);
            var node   = editor.getView().getPersonNodeNear(pos.x, pos.y);
            if (node) {
                if (node.getPhenotipsPatientId() != "") {
                    editor.getOkCancelDialogue().showCustomized("This individual is already linked to another patient","Can't assign", "OK", null);
                    return;
                }
                this._onDropObject(node, patient);
            }
        },

        /**
         * Callback for dragging an object from the legend onto nodes
         *
         * @method _onDropGeneric
         * @param {Person} Person node
         * @param {String|Number} id ID of the object
         */
        _onDropObject: function(node, patient) {
            if (node.isPersonGroup()) {
                return;
            }
            editor.getView().unmarkAll();
            var properties = { "setPhenotipsPatientId": patient.phenotipsID };
            document.fire("pedigree:node:modify", { 'nodeID': node.getID(),
                                                    'modifications': {'trySetPhenotipsPatientId': patient.phenotipsID},
                                                    'details': {'skipConfirmDialogue': true} });
        },

        /**
         * Remove patient from legend
         *
         * @method _deletePatientElement
         * @param {patientId} Person patient id
         * @private
         */
        _deletePatientElement: function(phenotipsPatientID) {
            $(phenotipsPatientID).remove();
            delete this._notLinkedPatients[phenotipsPatientID];
            // independently, hide new section, if empty
            if (!this._list_current.down('li')) {
              $('list_current').addClassName("legend-hidden");
            }
            // independently, hide unlinked section, if empty
            if (!this._list_unlinked.down('li')) {
              $('list_unlinked').addClassName("legend-hidden");
            }
            // hide legend if empty
            if (!this._hasPatients()) {
                this.legendContainer.hide();
            }
        },

        _hasPatients: function() {
            if (!this.legendContainer.down('li')) {
                return false;
            }
            return true;
        }

    });
    return PatientDropLegend;
});