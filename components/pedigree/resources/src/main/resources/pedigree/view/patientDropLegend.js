/**
 * Class responsible for display and drop patients to the pedigree tree nodes
 *
 * @class PatientDropLegend
 * @constructor
 */
define([
        "pedigree/model/helpers"
    ], function(
        Helpers
    ){
    var PatientDropLegend = Class.create( {

        initialize: function() {
            if (editor.isReadOnlyMode()) {
                return;
            }

            this.assignNewPatientId = null;

            this._notLinkedPatients  = {};

            this._legendInfo = new Element('div', {'class' : 'legend-box legend-info', id: 'legend-info'}).insert(
                    new Element('div', {'class' : 'infomessage no-word-wrap'}).insert(
                      "Drag and drop onto the pedigree")
                 );
            this.closeButton = new Element('span', {'class' : 'close-button'}).update('x');
            this.closeButton.observe('click', this.hideDragHint.bindAsEventListener(this));
            this._legendInfo.insert({'top': this.closeButton});
            this._legendInfo.hide();

            this.legendContainer = new Element('div', {'class' : 'patient-assign-legend', id: 'patient-assign'}).insert(this._legendInfo);
            this.legendContainer.hide();

            this._list_new = new Element('ul', {'class' : 'patient-list new'});
            this._list_unlinked = new Element('ul', {'class' : 'patient-list unlinked'});
            var l_box = new Element('div', {'class' : 'legend-box', 'id' : 'list_new'});
            l_box.insert(new Element('h2', {'class' : 'legend-title'}).update("New patient: "));
            l_box.insert(this._list_new);
            this.legendContainer.insert(l_box);
            var lg_box = new Element('div', {'class' : 'legend-box', 'id' : 'list_unlinked'});
            lg_box.insert(new Element('h2', {'class' : 'legend-title'}).update("Unlinked patients: "));
            lg_box.insert(this._list_unlinked);
            this.legendContainer.insert(lg_box);

            editor.getWorkspace().getWorkArea().insert(this.legendContainer);
            $('list_new').hide();
            $('list_unlinked').hide();

            Droppables.add(editor.getWorkspace().canvas, {accept:  'drop-patient',
                                                          onDrop:  this._onDropWrapper.bind(this),
                                                          onHover: this._onHoverWrapper.bind(this)});

            //add patient to a legend on unlink patient from node event
            document.observe('pedigree:patient:unlinked', function (event) {
                if ( event.memo.phenotipsID == this.assignNewPatientId) {
                    this.addCase(event.memo.phenotipsID, 'new', event.memo.gender, event.memo.firstName, event.memo.lastName, event.memo.externalID);
                } else {
                    this.addCase(event.memo.phenotipsID, 'unlinked', event.memo.gender, event.memo.firstName, event.memo.lastName, event.memo.externalID);
                }
            }.bind(this));
            //remove patient from a legend on link patient to node event
            document.observe('pedigree:patient:linked', function (event){
                if (this._notLinkedPatients.hasOwnProperty(event.memo.phenotipsID)) {
                    this._deletePatientElement(event.memo.phenotipsID, this._notLinkedPatients[event.memo.phenotipsID].type);
                    delete this._notLinkedPatients[event.memo.phenotipsID];
                }
            }.bind(this));
        },

        hideDragHint: function() {
            editor.getPreferencesManager().setConfigurationOption("user", "hideDraggingHint", true);
            this._legendInfo.hide();
        },

        hide: function() {
            this.legendContainer.hide();
        },

        show: function() {
            if (!this._hasPatients()) {
                this.legendContainer.show();
            }
        },

        /**
         * Add patient to a legend
         *
         * @method addCase
         **/
        addCase: function(patientID, type, gender, firstName, lastName, externalID) {

            if (!this._notLinkedPatients.hasOwnProperty(patientID)) {
                // if data about this patient is not available need ot load it
                if (gender === undefined) {
                    this._loadPatientInfoAndAddToLegend(patientID, type);
                    return;
                }

                var name = firstName + " " + lastName;

                if (!editor.getPreferencesManager().getConfigurationOption("hideDraggingHint")) {
                    this._legendInfo && this._legendInfo.show();
                }

                this._notLinkedPatients[patientID] = {"type" : type, "phenotipsID": patientID, "gender": gender, "name":  name, "externalID": externalID};
                var listElement = this._generateElement(this._notLinkedPatients[patientID]);
                if (type == 'new') {
                    this._list_new.insert(listElement);
                    // if called from familySelector, then assigning a new patient to an existing family
                    this.assignNewPatientId = patientID;
                    this.assignToFamilyId = editor.getFamilySelector().familyID;
                    $('list_new').show();
                } else {
                    this._list_unlinked.insert(listElement);
                    $('list_unlinked').show();
                }
            }

            // show legend in any case when addCase() is invoked
            this.legendContainer.show();
        },

        _loadPatientInfoAndAddToLegend: function(patientID, type) {
            var _this = this;

            var patientDataJsonURL = editor.getExternalEndpoint().getLoadPatientDataJSONURL([patientID]);

            new Ajax.Request(patientDataJsonURL, {
                method: "GET",
                onSuccess: function (response) {
                    if (response.responseJSON) {
                      var patient = response.responseJSON[patientID];
                      var firstName = patient.hasOwnProperty('patient_name') && patient.patient_name.hasOwnProperty("first_name")
                                      ? patient.patient_name.first_name.trim() : "";
                      var lastName  = patient.hasOwnProperty('patient_name') && patient.patient_name.hasOwnProperty("last_name")
                                      ? patient.patient_name.last_name.trim() : "";
                      _this.addCase(patientID, type, patient.sex, firstName, lastName, patient.external_id);
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

            var p_label = patientElement.phenotipsID;
            var hasName       = patientElement.name && patientElement.name != ' ';
            var hasExternalID = patientElement.externalID && patientElement.externalID != ' ';
            if (hasName || hasExternalID) {
                p_label += " (";
                if (hasName) {
                    var displayName = patientElement.name;
                    if (displayName.length > 25) {
                        // to make sure patient legend is not too wide
                        displayName = displayName.substring(0, 24) + "...";
                    }
                    p_label+= "name: " + displayName;
                }
                if (hasExternalID) {
                    p_label += (hasName ? ", " : "") + "identifier: " + patientElement.externalID;
                }
                p_label += ")";
            }

            var item = new Element('li', {'class' : 'abnormality drop-patient', 'id' : patientElement.phenotipsID}).insert(new Element('span', {'class' : shape})).insert(new Element('span', {'class' : 'patient-name no-word-wrap'}).update(p_label));
            item.insert(new Element('input', {'type' : 'hidden', 'value' : patientElement.phenotipsID}));
            var _this = this;
            Element.observe(item, 'mouseover', function() {
                item.down('.patient-name').setStyle({'background': '#DDD'});
                // TODO: only highlight on start drag
                _this._highlightDropTargets(patientElement, true);
            });
            Element.observe(item, 'mouseout', function() {
                // item.setStyle({'text-decoration':'none'});
                item.down('.patient-name').setStyle({'background':''});
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
            this.validTargets = editor.getGraph().getPossiblePatientIDTarget(patient.gender);
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
                if (node.getGender() != patient.gender && node.getGender() != 'U' && patient.gender != 'U') {
                    editor.getOkCancelDialogue().showCustomized("Can not drag the patient to a different gender","Can't assign", "OK", null);
                    return;
                }
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
            document.fire("pedigree:node:modify", {'nodeID': node.getID(), 'modifications': {'trySetPhenotipsPatientId': patient.phenotipsID}});
        },

        /**
         * Remove patient from legend
         *
         * @method _deletePatientElement
         * @param {patientId} Person patient id
         * @param {type} Person patient's type
         * @private
         */
        _deletePatientElement: function(patientId, type) {
            $(patientId).remove();
            delete this._notLinkedPatients[patientId];
            // hide legend if empty
            if (!this._hasPatients()) {
                this.legendContainer.hide();
            }
            // independently, hide new section, if empty
            if (!this._list_new.down('li')) {
              $('list_new').hide();
            }
            // independently, hide unlinked section, if empty
            if (!this._list_unlinked.down('li')) {
              $('list_unlinked').hide();
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