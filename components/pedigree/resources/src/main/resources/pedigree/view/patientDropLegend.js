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

            this._dragInfo = new Element('div', {'class' : 'legend-box legend-info pedigree_family_record_ui', id: 'legend-info'}).insert(
                    new Element('div', {'class' : 'infomessage no-word-wrap'}).insert(
                      "Drag and drop onto the pedigree")
                 );
            var closeButton = new Element('span', {'class' : 'close-button'}).update('x');
            closeButton.observe('click', this.hidePatientDragHint.bindAsEventListener(this));
            this._dragInfo.insert({'top': closeButton});
            if (editor.getPreferencesManager().getConfigurationOption("hidePatientDraggingHint")) {
                this._dragInfo.addClassName("legend-hidden");
            }

            this._legendBoxControls = new Element('div', {'class' : 'legend-box-controls-open', id: 'patient-legend-box-controls'});
            var minimizedLegendTitle = new Element('div', {'class': 'legend-minimized-title field-no-user-select'})
                .update("Other family members").hide();
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

            this.legendContainer = new Element('div', {'class' : 'patient-assign-legend generic-legend pedigree_family_record_ui', id: 'patient-assign'})
                                   .insert(this._legendBoxControls)
                                   .insert(this._dragInfo);

            this._list_unlinked = new Element('div', {'class' : 'patient-list unlinked'});
            var lg_box = new Element('div', {'class' : 'legend-box', 'id' : 'list_unlinked'});
            lg_box.insert(new Element('h2', {'class' : 'legend-title'}).update("Family members not in pedigree"));
            lg_box.insert(this._list_unlinked);

            this._addFamilyMembersButton = new Element('span', {'class' : 'add-family-member-button', 'id' : 'add_family_member'});
            this._addFamilyMembersButton.update("Add family member");
            this._addFamilyMembersButton.observe('click', function(event) {
                editor.getAddNewMemberDialog().show();
            })

            this._addButtonContainer = new Element('div', {'class': 'patient-record-add-section pedigree_family_record_ui'});
            this._addButtonContainer.insert(this._addFamilyMembersButton);
            lg_box.insert(this._addButtonContainer);
            this.legendContainer.insert(lg_box);

            editor.getWorkspace().getLegendContainer().insert(this.legendContainer);

            Droppables.add(editor.getWorkspace().canvas, {accept:  'drop-patient',
                                                          onDrop:  this._onDropWrapper.bind(this),
                                                          onHover: this._onHoverWrapper.bind(this)});

            // add patient to a legend on unlink patient from node event
            document.observe('pedigree:patient:unlinked', function (event) {
                    this.addCase(event.memo.phenotipsID,
                                 event.memo.pedigreeProperties);
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
            this.legendContainer.show();
        },

        hasPatient: function(phenotipsPatientID) {
            return this._notLinkedPatients.hasOwnProperty(phenotipsPatientID);
        },

        removeCase: function(phenotipsPatientID) {
            if (this.hasPatient(phenotipsPatientID)) {
                this._deletePatientElement(phenotipsPatientID);
                delete this._notLinkedPatients[phenotipsPatientID];

                this._updateDataModel();
            }
        },

        /**
         * Add patient to a legend
         *
         * @method addCase
         **/
        addCase: function(phenotipsPatientID, pedigreeProperties) {

            if (!this.hasPatient(phenotipsPatientID)) {
                // check if we have patient record data for this patient, if not load it
                if (!editor.getPatientRecordData().hasPatient(phenotipsPatientID)) {
                    this._loadPatientInfoAndAddToLegend(phenotipsPatientID);
                    return;
                } else {
                    var phenotipsProperties = editor.getPatientRecordData().get(phenotipsPatientID);
                }

                if (!pedigreeProperties) {
                    pedigreeProperties = {};
                }

                phenotipsProperties.id = phenotipsPatientID;

                // pedigree properties are higher priority than phenotips properties, but check both:
                var getProperty = function(pedigreePropertyName, phenotipsPropertyPath) {
                    if (pedigreeProperties.hasOwnProperty(pedigreePropertyName)) {
                        return pedigreeProperties[pedigreePropertyName];
                    }

                    if (phenotipsPropertyPath && phenotipsPropertyPath.length > 0) {
                        var data = phenotipsProperties;
                        for (var i = 0; i < phenotipsPropertyPath.length; i++) {
                            if (data.hasOwnProperty(phenotipsPropertyPath[i])) {
                                data = data[phenotipsPropertyPath[i]];
                            } else {
                                return "";
                            }
                        }
                        return data;
                    }

                    return "";
                };

                // properties displayed in the legend:
                var gender     = getProperty("gender", ["sex"]);
                var externalID = getProperty("externalID", ["external_id"]).trim();
                var firstName  = getProperty("fName", ["patient_name", "first_name"]).trim();
                var lastName   = getProperty("lName", ["patient_name", "last_name"]).trim();
                var name = (firstName ? firstName : "") + ((firstName && lastName) ? " " : "") + (lastName ? lastName : "");

                var patientDetails = [];
                if (name != "") {
                    patientDetails.push({"key": "name", "value": name});
                }
                if (externalID != "") {
                    patientDetails.push({"key": "id", "value": externalID});
                }
                var patientNotes = [];
                var currentPatient = (phenotipsPatientID == editor.getGraph().getCurrentPatientId());
                if (currentPatient) {
                    patientNotes.push("(current patient)");
                }
                if (name == "" && externalID == "" && !currentPatient) {
                    patientNotes.push("(no name or id specified)");
                }

                this._notLinkedPatients[phenotipsPatientID] = {"phenotipsID": phenotipsPatientID,
                                                               "gender": gender,
                                                               "patientDetails": patientDetails,
                                                               "patientNotes": patientNotes,
                                                               "pedigreeProperties": pedigreeProperties};

                var listElement = this._generateElement(this._notLinkedPatients[phenotipsPatientID]);

                this._list_unlinked.insert(listElement);

                this._updateDataModel();
            }

            // show legend in any case when addCase() is invoked
            this.legendContainer.show();
        },

        _updateDataModel: function() {
            var unlinked = [];
            for (var phenotipsPatientID in this._notLinkedPatients) {
                if (this._notLinkedPatients.hasOwnProperty(phenotipsPatientID)) {
                    unlinked.push(phenotipsPatientID);
                }
            }

            if (unlinked.length == 0) {
               this.legendContainer.addClassName("pedigree_family_record_ui");
            } else {
               this.legendContainer.removeClassName("pedigree_family_record_ui");
            }

            editor.getGraph().setUnlinkedPatients(unlinked);
        },

        _loadPatientInfoAndAddToLegend: function(phenotipsPatientID) {
            var _this = this;

            var onDataReady = function(responseJSON) {
                // a newly linked patient record has no pedigree-specific properties yet
                var pedigreeProperties = {};
                _this.addCase(phenotipsPatientID, pedigreeProperties);
            };

            editor.getPatientDataLoader().load([phenotipsPatientID], onDataReady);
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

            var createAnnotationDiv = function(label, value, fixedWidth) {
                if (value.length > 15) {
                    // to make sure patient legend is not too wide
                    value = value.substring(0, 13) + "...";
                }
                var innerDiv = new Element('div', {'class' : 'patient-legend-details-label'
                                                             + (fixedWidth ? ' min-column-width-45' : '')
                                                             + (value.length > 0 ? ' patient-legend-details-label-margin' : '')
                                                  })
                               .insert(label);
                var outerDiv = new Element('div', {'class' : 'patient-legend-details'}).insert(innerDiv).insert(value);
                return outerDiv;
            };

            var patientIdLink = new Element('div', {'class': 'pedigree-nodePatientTextLink legend-patient-link'})
                                .update(patientElement.phenotipsID);
            patientIdLink.observe("click", function() {
                window.open(editor.getExternalEndpoint().getPhenotipsPatientURL(patientElement.phenotipsID), patientElement.phenotipsID);
            });

            var patientData = new Element('div', {'class': 'patient-legend-patient'}).update(patientIdLink);
            for (var i = 0; i < patientElement.patientDetails.length; i++) {
                var data = patientElement.patientDetails[i];
                patientData.insert(createAnnotationDiv(data.key + ":", data.value, (patientElement.patientDetails.length > 1)));
            }
            for (var i = 0; i < patientElement.patientNotes.length; i++) {
                patientData.insert(createAnnotationDiv(patientElement.patientNotes[i], "", false));
            }

            var draggablePart = new Element('div', {'class' : 'abnormality drop-patient'})
                       .insert(new Element('span', {'class' : shape}))
                       .insert(patientData)
                       .insert(new Element('input', {'type' : 'hidden', 'value' : patientElement.phenotipsID}));

            var _this = this;

            new Draggable(draggablePart, {
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

            var editButton = new Element('div', {'class' : 'legend-patient-button legend-edit-patient-button'})
                             .update(new Element('span', {'class' : 'fa fa-edit'}));
            // FIXME: when node menu works for node-less patients
            editButton.hide();

            var removeButton = new Element('div', {'class' : 'legend-patient-button legend-remove-patient-button'})
                             .update(new Element('span', {'class' : 'fa fa-remove'}));

            removeButton.observe("click", function() {
                var onRemove = function() {
                    _this.removeCase(patientElement.phenotipsID);
                };
                editor.getOkCancelDialogue().show("Remove this patient record from the family?", "Remove?", onRemove);
            });

            var item = new Element('div', {'id' : patientElement.phenotipsID})
                       .insert(draggablePart).insert(editButton).insert(removeButton);

            return item;
        },

        _highlightDropTargets: function(isOn) {
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

            // highlight potential targets
            this._highlightDropTargets(true);

            // disable patient record link clicking
            label.down(".legend-patient-link").addClassName("no-mouse-interaction");

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

            label.down(".legend-patient-link").removeClassName("no-mouse-interaction");

            this._highlightDropTargets(false);
            this._unhighlightAfterDrag();

            var divPos = editor.getWorkspace().viewportToDiv(event.pointerX(), event.pointerY());
            var pos    = editor.getWorkspace().divToCanvas(divPos.x,divPos.y);
            var node   = editor.getView().getPersonNodeNear(pos.x, pos.y);
            if (node) {
                if (node.getPhenotipsPatientId() != "") {
                    editor.getOkCancelDialogue().showCustomized("This individual is already linked to another patient","Can't assign", "OK", null);
                    return;
                }

                var id = label.select('input')[0].value;
                var patient = this._notLinkedPatients[id];
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

            var event = { 'nodeID': node.getID(),
                          'details': {'skipConfirmDialogue': true } };

            event.modifications = {'trySetAllProperties': {"pedigreeProperties": patient.pedigreeProperties,
                                                           "phenotipsProperties": editor.getPatientRecordData().get(patient.phenotipsID)} };

            document.fire("pedigree:node:modify", event);
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