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
                    new Element('div', {'class' : 'infomessage'}).insert(
                      "You can drag and drop all items from the list(s) below onto individuals in the pedigree to mark them as affected.")
                 );
            this.closeButton = new Element('span', {'class' : 'close-button'}).update('x');
            this.closeButton.observe('click', this.hideDragHint.bindAsEventListener(this));
            this._legendInfo.insert({'top': this.closeButton});
            this._legendInfo.hide();

            this.legendContainer = new Element('div', {'class' : 'patient-assign-legend legend-container', id: 'patient-assign'}).insert(this._legendInfo);
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

            var workArea = editor.getWorkspace().getWorkArea();
            var parent = editor.getWorkspace().getWorkArea().up();
            parent.insertBefore(this.legendContainer, workArea);
        	$('list_new').hide();
        	$('list_unlinked').hide();

            Droppables.add(editor.getWorkspace().canvas, {accept: 'drop-patient', onDrop: this._onDropWrapper.bind(this)});

            //add patient to a legend on unlink patient from node event
            document.observe('pedigree:patient:unlinked', function (event) {
            	if ( event.memo.phenotipsID == this.assignNewPatientId) {
            		this.addCase(event.memo.phenotipsID, 'new', event.memo.gender, event.memo.name, event.memo.externalID);
            		
            	} else {
            		this.addCase(event.memo.phenotipsID, 'unlinked', event.memo.gender, event.memo.name, event.memo.externalID);
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

        /**
         * Add patient to a legend
         *
         * @method addCase
         **/
        addCase: function(patientID, type, gender, name, externalID) {
            if (!this._notLinkedPatients.hasOwnProperty(patientID)) {
            	
            	!editor.getPreferencesManager().getConfigurationOption("hideDraggingHint") &&
                this._legendInfo && this._legendInfo.show();
            	
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

                this.legendContainer.show();

            }
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
            if (patientElement.name && patientElement.name != ' ') {
                p_label+= ", " + patientElement.name;
            }
            if (patientElement.externalID && patientElement.externalID != ' ') {
                p_label += ", " + patientElement.externalID;
            }
    
            var item = new Element('li', {'class' : 'abnormality drop-patient', 'id' : patientElement.phenotipsID}).insert(new Element('span', {'class' : shape})).insert(new Element('span', {'class' : 'patient-name'}).update(p_label));
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
            if (isOn) {
            	editor.getView()._currentHoveredNode = 1;
            	this.validTargets = editor.getGraph().getPossiblePatientIDTarget(patient.gender);
	            this.hoverModeZones = editor.getPaper().set();            
	            var me = this;
	            this.validTargets.each(function(nodeID) {
	                var node = editor.getNode(nodeID);
	                node.getGraphics().grow();
	
	                var hoverModeZone = node.getGraphics().getHoverBox().getHoverZoneMask().clone().toFront();
	                hoverModeZone.hover(
	                    function() {
	                        node.getGraphics().getHoverBox().setHighlighted(true);
	                    },
	                    function() {
	                        node.getGraphics().getHoverBox().setHighlighted(false);
	                    });
	                me.hoverModeZones.push(hoverModeZone);
	            });
            } else {
            	editor.getView()._currentHoveredNode = null;
            	this.hoverModeZones.remove();
            	this.validTargets.each(function(nodeID) {
            		var node = editor.getNode(nodeID)
                    node.getGraphics().shrink();
                    node.getGraphics().getHoverBox().setHighlighted(false);
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
            
            var id = label.select('input')[0].value;
            var patient = this._notLinkedPatients[id];
            
            this._highlightDropTargets(patient, false);
            
            var divPos = editor.getWorkspace().viewportToDiv(event.pointerX(), event.pointerY());
            var pos    = editor.getWorkspace().divToCanvas(divPos.x,divPos.y);
            var node   = editor.getView().getPersonNodeNear(pos.x, pos.y);
            if (node) {
                if (node.getGender() != this._getGender(event.element()) && node.getGender() != 'U' && this._getGender(event.element()) != 'U') {
                	editor.getOkCancelDialogue().showCustomized(this.warningMessage,"You can not assign the patient to a different gender.", "OK", null);
                    return;
                }

                this._onDropObject(node, patient);
            }
        },

        /**
         * Get the gender of a patient element
         *
         * @method _getGender
         * @param {element} Person patient element
         * @private
         */
        _getGender: function(element) {
            return this._notLinkedPatients[element.up().identify()].gender;
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
            //hide legend if empty
            if (!this.legendContainer.down('li')) {
                this.legendContainer.hide();
            } else {
                if (!this._list_new.down('li')) {
                	$('list_new').hide();
                }
                
                if (!this._list_unlinked.down('li')) {
                	$('list_unlinked').hide();
                }
            }
        }

    });
    return PatientDropLegend;
});