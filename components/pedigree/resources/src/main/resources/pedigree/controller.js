/**
 * ...
 *
 * @class Controller
 * @constructor
 */

// TODO: undo/redo in all handlers
define([
        "pedigree/model/helpers",
        "pedigree/saveLoadEngine",
    ], function(
        Helpers,
        SaveLoadEngine
    ){
    var Controller = Class.create({
        initialize: function() {
            document.observe("pedigree:autolayout",                this.handleAutoLayout);
            document.observe("pedigree:graph:clear",               this.handleClearGraph);
            document.observe("pedigree:undo",                      this.handleUndo);
            document.observe("pedigree:redo",                      this.handleRedo);
            document.observe("pedigree:renumber",                  this.handleRenumber);
            document.observe("pedigree:historychange",             this.handleUndoHistoryChange);
            document.observe("pedigree:node:remove",               this.handleRemove);
            document.observe("pedigree:node:setproperty",          this.handleSetProperty);
            document.observe("pedigree:node:modify",               this.handleModification);
            document.observe("pedigree:person:drag:newparent",     this.handlePersonDragToNewParent);
            document.observe("pedigree:person:drag:newpartner",    this.handlePersonDragToNewPartner);
            document.observe("pedigree:person:drag:newsibling",    this.handlePersonDragToNewSibling);
            document.observe("pedigree:person:newparent",          this.handlePersonNewParents);
            document.observe("pedigree:person:newsibling",         this.handlePersonNewSibling);
            document.observe("pedigree:person:newpartnerandchild", this.handlePersonNewPartnerAndChild);
            document.observe("pedigree:partnership:newchild",      this.handleRelationshipNewChild);
        },

        handleUndo: function(event)
        {
            console.log("event: " + event.eventName + ", memo: " + Helpers.stringifyObject(event.memo));
            editor.getUndoRedoManager().undo();
        },

        handleRedo: function(event)
        {
            console.log("event: " + event.eventName + ", memo: " + Helpers.stringifyObject(event.memo));
            editor.getUndoRedoManager().redo();
        },

        handleRenumber: function(event)
        {
            // Assigns user-visible node labels for all person nodes, based on generation and order
            // ("I-1","I-2","I-3", "II-1", "II-2", etc.)

            //console.log("event: " + event.eventName + ", memo: " + Helpers.stringifyObject(event.memo));

            var check      = event.memo.hasOwnProperty("check");
            var clear      = false;
            var needRedraw = false;

            do {
                var secondPass = false;

                for (var nodeID in editor.getView().getNodeMap()) {
                    if (editor.getView().getNodeMap().hasOwnProperty(nodeID)) {
                        if (editor.getGraph().isPerson(nodeID) && !editor.getGraph().isPlaceholder(nodeID)) {
                            var node = editor.getView().getNode(nodeID);
                            var currentPedNumber = node.getPedNumber();

                            if (clear) {
                                var pedNumber = "";
                            } else {
                                var generation = editor.getGraph().getGeneration(nodeID);
                                var order      = editor.getGraph().getOrderWithinGeneration(nodeID);
                                var pedNumber  = Helpers.romanize(generation) + "-" + order;

                                if (check) {
                                    if (pedNumber != currentPedNumber) {
                                        // one of the nodes PED number is not correct
                                        clear = true;
                                        secondPass = true;
                                        break;
                                    }
                                }
                            }

                            if (currentPedNumber != pedNumber) {
                                needRedraw = true;
                                node.setPedNumber(pedNumber);
                                var allProperties = node.getProperties();
                                editor.getGraph().setProperties( nodeID, allProperties );
                            }
                        }
                    }
                }
            } while (secondPass);

            var renumberButton = $('action-number');
            if (clear) {
                renumberButton.removeClassName("disabled-menu-item");
                renumberButton.addClassName("menu-item");
            } else {
                renumberButton.removeClassName("menu-item");
                renumberButton.addClassName("disabled-menu-item");
            }

            if (!event.memo.noUndoRedo && needRedraw) {
                editor.getView().unmarkAll();
                editor.getUndoRedoManager().addState( event );
            }
        },

        handleUndoHistoryChange: function() {
            var redoButton = $('action-redo');
            if (editor.getUndoRedoManager().hasRedo()) {
                redoButton.removeClassName("disabled-menu-item");
                redoButton.addClassName("menu-item");
            } else {
                redoButton.removeClassName("menu-item");
                redoButton.addClassName("disabled-menu-item");
            }
            var undoButton = $('action-undo');
            if (editor.getUndoRedoManager().hasUndo()) {
                undoButton.removeClassName("disabled-menu-item");
                undoButton.addClassName("menu-item");
            } else {
                undoButton.removeClassName("menu-item");
                undoButton.addClassName("disabled-menu-item");
            }
        },

        handleAutoLayout: function(event)
        {
            try {
                console.log("event: " + event.eventName + ", memo: " + Helpers.stringifyObject(event.memo));
                var changeSet = editor.getGraph().redrawAll();
                editor.getView().applyChanges(changeSet, true);

                if (!event.memo.noUndoRedo)
                    editor.getUndoRedoManager().addState( event );
            } catch(err) {
                console.log("Autolayout error: " + err);
            }
        },

        handleClearGraph: function(event)
        {
            console.log("event: " + event.eventName + ", memo: " + Helpers.stringifyObject(event.memo));
            var changeSet = editor.getGraph().clearAll(editor.isFamilyPage());

            var noUndoRedo = event.memo ? ( event.memo.hasOwnProperty("noUndoRedo") ? event.memo.noUndoRedo : false ) : false;
            editor.getSaveLoadEngine()._finalizeCreateGraph(changeSet, noUndoRedo, true);

            editor.getView().unmarkAll();
        },

        handleRemove: function(event)
        {
            console.log("event: " + event.eventName + ", memo: " + Helpers.stringifyObject(event.memo));
            var nodeID = event.memo.nodeID;

            // get the list of affected nodes
            var disconnectedList = editor.getGraph().getDisconnectedSetIfNodeRemoved(nodeID);

            var onlyChild = false;
            // special case of removing the last child: need to convert to placeholder
            if (editor.getGraph().isPerson(nodeID) && editor.getGraph().isOnlyChild(nodeID)) {
                var producingRelationship = editor.getGraph().getParentRelationship(nodeID);
                if (!Helpers.arrayContains(disconnectedList, producingRelationship)) {
                    onlyChild = true;
                }
            }

            var removeSelected = function() {
                try {
                    if (onlyChild) {
                        // instea dof removing convert child to a placeholder.
                        //
                        // note: need to set properties before actual remove while we know the
                        //       ID which may change as part of remove process

                        Helpers.removeFirstOccurrenceByValue(disconnectedList, nodeID);

                        editor.getGraph().setProperties( nodeID, {"gender": "U", "placeholder": true} );
                        if (!editor.getGraph().isChildless(producingRelationship)) {
                            var partnership = editor.getView().getNode(producingRelationship);
                            partnership.setChildlessStatus('childless');
                            var newProperties = partnership.getProperties();
                            editor.getGraph().setProperties( producingRelationship, newProperties );
                        }
                    }

                    var changeSet = editor.getGraph().removeNodes(disconnectedList);

                    if (onlyChild) {
                        // mark that this needs to be recreated
                        changeSet.removed.push(nodeID);
                        var newNodeID = changeSet.changedIDSet.hasOwnProperty(nodeID) ? changeSet.changedIDSet[nodeID] : nodeID;
                        changeSet["new"] = [ newNodeID ];
                    }

                    editor.getView().applyChanges(changeSet, true);

                    var changeSet = editor.getGraph().improvePosition();
                    editor.getView().applyChanges(changeSet, true);

                    if (!event.memo.noUndoRedo)
                        editor.getUndoRedoManager().addState( event );
                } catch(err) {
                    console.log("[DEBUG] Remove error: " + err);
                }
            };

            // if there is only one node or this removal is done as part of an undo/redo action
            // => just remove without asking any questions or highlighting any nodes
            if (disconnectedList.length <= 1 || event.memo.hasOwnProperty("noUndoRedo")) {
                removeSelected();
                return;
            }

            // otherwise remove current highlighting and highlight all nodes which will be removed
            editor.getView().unmarkAll();
            for (var i = 0; i < disconnectedList.length; i++) {
                var nextHighlight = disconnectedList[i];
                editor.getView().getNode(nextHighlight).getGraphics().markPermanently();
            }

            var unhighlightSelected = function() {
                for (var i = 0; i < disconnectedList.length; i++) {
                    var nextHighlight = disconnectedList[i];
                    editor.getView().getNode(nextHighlight).getGraphics().unmark();
                }
            }

            // disallow removal of the node linked to the current patient
            for (var i = 0; i < disconnectedList.length; i++) {
                if (editor.getGraph().isPerson(disconnectedList[i])) {
                    var node = editor.getNode(disconnectedList[i]);
                    if (node.getPhenotipsPatientId() == editor.getGraph().getCurrentPatientId()) {
                        editor.getOkCancelDialogue().showError("<br>Can't remove this node because the current patient would have to be removed as well.<br><br>" +
                                "<font style='font-size:95%'>(a pedigree can't have disconnected components; " +
                                "removing this node would<br>cause all highlighted individuals to be disconnected from the<br>" +
                                "proband and thus all of them would have to be removed)</font>",
                                "Can't remove", "OK", unhighlightSelected);
                        return;
                    }
                }
            }

            if (!editor.isFamilyPage()) {
                // ...and display a OK/Cancel dialogue, calling "removeSelected()" on OK and "unhighlightSelected" on Cancel
                editor.getOkCancelDialogue().show( '<br>All highlighted nodes will be removed. Do you want to proceed?<br><br>' +
                                                   '<br><font style="font-size:95%">(all persons in a pedigree should be connected, so all nodes no longer<br>connected to the proband have to be removed as well)</font>',
                                                   'Delete nodes?', removeSelected, unhighlightSelected );
            } else {
                editor.getOkCancelDialogue().show( '<br>All highlighted nodes will be removed. Do you want to proceed?<br><br>' +
                        '<br><font style="font-size:95%">(all persons in a pedigree should be connected, so one of the disconnected sets of nodes has to be removed)</font>',
                        'Delete nodes?', removeSelected, unhighlightSelected );
            }
        },

        handleSetProperty: function(event)
        {
            //console.log("event: " + event.eventName + ", memo: " + Helpers.stringifyObject(event.memo));
            var nodeID     = event.memo.nodeID;
            var properties = event.memo.properties;
            var undoEvent  = {"eventName": event.eventName, "memo": {"nodeID": nodeID, "properties": Helpers.cloneObject(event.memo.properties)}};

            var node = editor.getView().getNode(nodeID);

            if (editor.getGraph().isPerson(nodeID) && !editor.getPatientAccessPermissions(node.getPhenotipsPatientId()).hasEdit) {
                // UI should forbid any changes from happenig in this case, but do a final check before applying any edits
                var updateMenuWithDiscardedChanges = function() {
                    editor.getNodeMenu().update();
                }
                editor.getOkCancelDialogue().showError("Can't save changes - you do not have edit rights for this patient",
                                                       "Can't save changes", "OK", updateMenuWithDiscardedChanges);
                return;
            }

            var changed = false;

            var twinUpdate = undefined;
            var needUpdateAncestors = false;
            var needUpdateRelationship = false;
            var needUpdateAllRelationships = false;
            var needUpdateYPositions = false;  // true iff: setting this property (e.g. extra long comments)
                                               //           may force stuff to move around in Y direction

            var changedValue = false;

            for (var propertySetFunction in properties) {
                if (properties.hasOwnProperty(propertySetFunction)) {
                    var propValue = properties[propertySetFunction];

                    //console.log("attmepting to set property " + propertySetFunction + " to " + propValue);
                    if (!Controller._validatePropertyValue( nodeID, propertySetFunction, propValue)) continue;

                    //console.log("validated");
                    // prepare undo event
                    var propertyGetFunction =  propertySetFunction.replace("set","get");
                    var oldValue = node[propertyGetFunction]();
                    if (oldValue == propValue) continue;
                    if (oldValue && typeof(oldValue) === 'object' && typeof(propValue) === 'object' &&
                        (propertySetFunction == "setDeathDate" || propertySetFunction == "setBirthDate")) {
                        // compare Date objects
                        try {
                            if ( oldValue.range == Helpers.cloneObject(propValue.range) &&
                                 oldValue.year  == propValue.year &&
                                 oldValue.month == propValue.month &&
                                 oldValue.day   == propValue.day )
                                continue;
                        } catch (err) {
                            // fine, one of the objects is in some other format, maybe date picker has changed
                            // and this code was not updated
                        }
                    }
                    if (Object.prototype.toString.call(oldValue) === '[object Array]') {
                        oldValue = oldValue.slice(0);
                    } else if (typeof(oldValue) == 'object') {
                        oldValue = Helpers.cloneObject(oldValue);
                    }

                    undoEvent.memo.properties[propertySetFunction] = oldValue;

                    // sometimes UNDO includes more then the property itself: e.g. changing life status
                    // from "dead" to "alive" also clears the death date. Need to add it to the "undo" event
                    if (propertySetFunction == "setLifeStatus") {
                        undoEvent.memo.properties["setDeathDate"]    = node.getDeathDate();
                        undoEvent.memo.properties["setGestationAge"] = node.getGestationAge();
                        undoEvent.memo.properties["setBirthDate"]    = node.getBirthDate();
                        undoEvent.memo.properties["setAdopted"]      = node.getAdopted();
                    }
                    if (propertySetFunction == "setDeathDate") {
                        undoEvent.memo.properties["setLifeStatus"] = node.getLifeStatus();
                    }
                    if (propertySetFunction == "setChildlessStatus") {
                        undoEvent.memo.properties["setChildlessReason"] = node.getChildlessReason();
                    }
                    if (propertySetFunction == "setDisorders") {
                        undoEvent.memo.properties["setCarrierStatus"] = node.getCarrierStatus();
                    }
                    if (propertySetFunction == "setCarrierStatus") {
                        undoEvent.memo.properties["setDisorders"] = node.getDisorders().slice(0);
                    }
                    if (propertySetFunction == "setCausalGenes" || propertySetFunction == "setCandidateGenes") {
                        undoEvent.memo.properties["setCausalGenes"]  = node.getCausalGenes();
                        undoEvent.memo.properties["setCandidateGenes"]  = node.getCandidateGenes();
                    }

                    node[propertySetFunction](propValue);

                    if (propertySetFunction == "setDisorders") {
                        var newDisorders = node[propertyGetFunction]();
                        if (JSON.stringify(oldValue) == JSON.stringify(newDisorders)) continue;
                    }

                    changedValue = true;

                    if (propertySetFunction == "setLastName") {
                        if (editor.getPreferencesManager().getConfigurationOption("propagateFatherLastName")) {
                            if (node.getGender(nodeID) == 'M') {
                                // propagate last name as "last name at birth" to all descendants (by the male line)
                                Controller._propagateLastNameAtBirth(nodeID, propValue, oldValue);
                                undoEvent = null; // there is no easy undo other than just remember the previous graph state
                            }
                        }
                    }

                    if (propertySetFunction == "setGender") {
                        if (node.getMonozygotic()) {
                            if (!twinUpdate) twinUpdate = {};
                            twinUpdate[propertySetFunction] = propValue;
                        }
                        if (oldValue == 'U' && propValue == 'M' &&
                            node.getLastName() == '' && node.getLastNameAtBirth() != '' &&
                            editor.getGraph().getAllChildren(nodeID).length == 0)
                        {
                            node.setLastName(node.getLastNameAtBirth());
                            node.setLastNameAtBirth("");
                            undoEvent = null; // there is no easy undo other than just remember the previous graph state
                        }
                    }
                    if (propertySetFunction == "setBirthDate") {
                        if (!twinUpdate) twinUpdate = {};
                        twinUpdate[propertySetFunction] = propValue;
                    }

                    if (propertySetFunction == "setAdopted") {
                        needUpdateAncestors = true;
                        if (propValue == "adoptedIn") {
                            // if one twin is adopted in the other must be as well
                            if (!twinUpdate) twinUpdate = {};
                            twinUpdate[propertySetFunction] = propValue;
                        }
                        if (oldValue == "adoptedIn") {
                            // if one twin was marked as adopted in the other must have been as well - but not
                            // necesserily adopted out as this one
                            if (!twinUpdate) twinUpdate = {};
                            twinUpdate[propertySetFunction] = "";
                        }
                    }

                    if (propertySetFunction == "setComments"  || propertySetFunction == "setExternalID" ||
                        propertySetFunction == "setFirstName" || propertySetFunction == "setLastName") {
                        // all the methods which may result in addition ort deletion of person labels
                        // (which may cause a shift up or down)
                        if (Helpers.numTextLines(oldValue) != Helpers.numTextLines(propValue)) {
                            needUpdateYPositions = true;
                        }
                    }
                    if (propertySetFunction == "setBirthDate" || propertySetFunction == "setDeathDate") {
                        // the number of lines may vary depending on age etc., it is easier to just recompute it
                        needUpdateYPositions = true;
                    }
                    if (propertySetFunction == "setCancers") {
                        needUpdateYPositions = true;
                    }

                    if (propertySetFunction == "setMonozygotic") {
                        needUpdateRelationship = true;
                        if (!twinUpdate) twinUpdate = {};
                        twinUpdate[propertySetFunction] = propValue;
                    }

                    if (propertySetFunction == "setConsanguinity" || propertySetFunction == "setBrokenStatus") {
                        // this updates the relationship lines, as well as any lines
                        // crossed by the relationship llines to maintain correct crossing graphics
                        needUpdateRelationship = true;
                    }

                    if (propertySetFunction == "setLostContact") {
                        // it is hard to say which of the incoming/outgoing lines needs to be redraws/updated,
                        // so it is easier to just redraw all
                        needUpdateAllRelationships = true;
                    }
                }
            }

            // some properties should be the same for all the twins. If one of those
            // was changed, need to update all the twins
            if (twinUpdate) {
                var allTwins = editor.getGraph().getAllTwinsSortedByOrder(nodeID);
                for (var propertySetFunction in twinUpdate) {
                    if (twinUpdate.hasOwnProperty(propertySetFunction)) {
                        var propValue = twinUpdate[propertySetFunction];

                        for (var i = 0; i < allTwins.length; i++) {
                            var twin = allTwins[i];
                            if (twin == nodeID) continue;
                            var twinNode = editor.getView().getNode(twin);
                            twinNode[propertySetFunction](propValue);
                            var twinProperties = twinNode.getProperties();
                            console.log("Setting twin properties: " + Helpers.stringifyObject(twinProperties));
                            editor.getGraph().setProperties( twin, twinProperties );
                        }
                    }
                }
            }

            // sync underlying graph with visual node representation's properties
            var allProperties = node.getProperties();
            editor.getGraph().setProperties( nodeID, allProperties );

            if (needUpdateAncestors) {
                var changeSet = editor.getGraph().updateAncestors();
                editor.getView().applyChanges(changeSet, true);
            }

            if (needUpdateAllRelationships) {
                var rels = editor.getGraph().getAllRelatedRelationships(nodeID);
                var changeSet = {"moved": rels};
                editor.getView().applyChanges(changeSet, true);
            }

            if (needUpdateRelationship) {
                var relID = editor.getGraph().isRelationship(nodeID) ? nodeID : editor.getGraph().getParentRelationship(nodeID);
                var changeSet = {"moved": [relID]};
                editor.getView().applyChanges(changeSet, true);
            }

            if (needUpdateYPositions) {
                var changeSet = editor.getGraph().updateYPositioning();
                editor.getView().applyChanges(changeSet, true);
            }

            //console.log("event: " + event.eventName + ", memo: " + Helpers.stringifyObject(event.memo));
            //console.log("Undo event: " + Helpers.stringifyObject(undoEvent));
            if (!event.memo.noUndoRedo && changedValue) {
                editor.getUndoRedoManager().addState( event, undoEvent );
            }

            editor.getNodeMenu().update();  // for example, user selected a wrong gender in the nodeMenu, which
                                            // gets reverted back - need to select the correct one in the nodeMenu as well
                                            //
                                            // note: need to do this after addState(), since addState() may trigger some
                                            //       extensions which modify valiues visible in node menu
        },

        handleModification: function(event)
        {
            console.log("event: " + event.eventName + ", memo: " + Helpers.stringifyObject(event.memo));
            var nodeID        = event.memo.nodeID;
            var modifications = event.memo.modifications;

            var node = editor.getView().getNode(nodeID);

            //var allProperties = node.getProperties();

            for (modificationType in modifications) {
                if (modifications.hasOwnProperty(modificationType)) {
                    var modValue = modifications[modificationType];

                    if (modificationType == "addTwin") {
                        var numNewTwins = modValue - 1; // current node is one of the twins, so need to create one less
                        for (var i = 0; i < numNewTwins; i++ ) {
                            var twinProperty = { "gender": node.getGender() };
                            if (node.getAdopted() == "adoptedIn") {
                                twinProperty["adoptedStatus"] = node.getAdopted();
                            }
                            var changeSet = editor.getGraph().addTwin( nodeID, twinProperty );
                            editor.getView().applyChanges(changeSet, true);
                        }
                        node.assignProperties(editor.getGraph().getProperties(nodeID));

                        if (!event.memo.noUndoRedo) {
                            editor.getUndoRedoManager().addState( event );
                        }
                    }
                    else
                    if (modificationType == "assignProband") {

                        var oldProbandID = editor.getGraph().getProbandId();

                        editor.getGraph().setProbandId(nodeID);

                        editor.getNode(nodeID).redrawProbandStatus();
                        if (oldProbandID >= 0) {
                            editor.getNode(oldProbandID).redrawProbandStatus();
                        }

                        editor.getNodeMenu().update();

                        if (!event.memo.noUndoRedo) {
                            editor.getUndoRedoManager().addState( event );
                        }
                    }
                    else
                    if (modificationType == "trySetPhenotipsPatientId") {

                        var setLink = function(clearOldData, loadPatientProperties) {

                            event.memo.clearOldData = clearOldData;

                            // load node properties from the linked patient's phenotips document

                            var onDataReady = function(loadedPatientData) {
                                // check if the value used is equal is already linked to some other node.
                                // If it is, unlink and, optionally, clean that node's properties
                                var allLinkedNodes = editor.getGraph().getAllPatientLinks();
                                if (allLinkedNodes.patientToNodeMapping.hasOwnProperty(modValue)) {
                                    var oldRepresentingNodeID = allLinkedNodes.patientToNodeMapping[modValue];

                                    var oldNode = editor.getView().getNode(oldRepresentingNodeID);
                                    oldNode.setPhenotipsPatientId("");
                                    if (clearOldData) {
                                        var oldProperties = oldNode.getPatientIndependentProperties();
                                    } else {
                                        var oldProperties = oldNode.getProperties();
                                    }
                                    editor.getGraph().setProperties( oldRepresentingNodeID, oldProperties );
                                    var changeSet = {"removed": [oldRepresentingNodeID], "new": [oldRepresentingNodeID]};
                                    editor.getView().applyChanges(changeSet, true);
                                }

                                // TODO: a very similar piece of code is used in saveLoadEngine.js, see _finalizeCreateGraph();
                                //       check if some refactoring/reusing is posible
                                if (loadedPatientData !== null && loadedPatientData.hasOwnProperty(modValue)) {

                                    var patientJSONObject = loadedPatientData[modValue];

                                    // TODO: review which properties to keep (likley all that are not saved in a patient profile?)
                                    //       for now save twin/monozygothic/adopted statuses, or pedigree may get inconsistent
                                    patientJSONObject.pedigreeProperties = node.getPatientIndependentProperties();
                                    delete patientJSONObject.pedigreeProperties.gender; // this one is loaded from the patient

                                    var genderOk = editor.getGraph().setNodeDataFromPhenotipsJSON( nodeID, patientJSONObject);
                                    if (!genderOk && !event.memo.noUndoRedo) {
                                        alert("Gender defined in Phenotips for patient " + modValue + " is incompatible with this pedigree. Setting pedigree node gender to 'Unknown'");
                                    }

                                    // update visual node's properties using data in graph model which was just loaded from phenotips
                                    node.assignProperties(editor.getGraph().getProperties(nodeID));
                                } else if (modValue == "") {
                                    if (clearOldData) {
                                        // preserve gender and some other pedigree-specific properties which
                                        // were manually set and were not loaded from a node
                                        var clearedProperties = node.getPatientIndependentProperties();
                                        editor.getGraph().setProperties(nodeID, clearedProperties);
                                        node.assignProperties(clearedProperties);
                                    } else {
                                        node.setPhenotipsPatientId("");
                                        editor.getGraph().setProperties( nodeID, node.getProperties() );
                                    }
                                } else {
                                    // not clearing data and no loaded data
                                    node.setPhenotipsPatientId(modValue);
                                    editor.getGraph().setProperties( nodeID, node.getProperties() );
                                }

                                var changeSet = editor.getGraph().updateYPositioning();
                                editor.getView().applyChanges(changeSet, true);

                                editor.getNodeMenu().update();

                                if (!event.memo.noUndoRedo) {
                                    editor.getUndoRedoManager().addState( event, null /* no easy undo event */);
                                }
                            }

                            if (modValue != "") {
                                if (loadPatientProperties) {
                                    var patientDataLoader = editor.getPatientDataLoader();

                                    // load data for only one patient with ID=="modValue", the one a node was just linked to
                                    patientDataLoader.load([modValue], onDataReady);
                                } else {
                                    onDataReady(null);
                                }
                            } else {
                                onDataReady(null);
                            }
                        }

                        if (!event.memo.noUndoRedo) {
                            var loadPatientProperties = true;
                            if (event.memo.hasOwnProperty("details")
                                && event.memo.details.hasOwnProperty("loadPatientProperties")) {
                                loadPatientProperties = event.memo.details.loadPatientProperties;
                            }
                            Controller._checkPatientLinkValidity(setLink, nodeID, modValue, loadPatientProperties);
                        } else {
                            // if this is a redo event skip all the warnings
                            setLink(event.memo.clearOldData);
                        }
                    }
                    else
                    if (modificationType == "makePlaceholder") {
                        // TODO
                    }
                }
            }
        },

        handlePersonDragToNewParent: function(event)
        {
            console.log("event: " + event.eventName + ", memo: " + Helpers.stringifyObject(event.memo));

            var personID = event.memo.personID;
            var parentID = event.memo.parentID;
            if (!editor.getGraph().isPerson(personID) || !editor.getGraph().isValidID(parentID)) return;

            if (editor.getGraph().isRelationship(parentID) && editor.getGraph().isChildlessByChoice(parentID)) {
                var partnership = editor.getView().getNode(parentID);
                partnership.setChildlessStatus(null);
                var newProperties = partnership.getProperties();
                editor.getGraph().setProperties( parentID, newProperties );
            }

            if (editor.getGraph().isChildless(parentID)) {
                editor.getController().handleSetProperty( { "memo": { "nodeID": personID, "properties": { "setAdopted": "adoptedIn" }, "noUndoRedo": true } } );
            }

            try {
            var changeSet = editor.getGraph().assignParent(parentID, personID);
            editor.getView().applyChanges(changeSet, true);

            if (changeSet.moved.indexOf(personID) != -1)
                editor.getWorkspace().centerAroundNode(personID, true);

            if (!event.memo.noUndoRedo)
                editor.getUndoRedoManager().addState( event );

            } catch(err) {
                console.log("err: " + err);
            }
        },

        handlePersonNewParents: function(event)
        {
            console.log("event: " + event.eventName + ", memo: " + Helpers.stringifyObject(event.memo));

            var personID = event.memo.personID;
            if (!editor.getGraph().isPerson(personID)) return;

            var changeSet = editor.getGraph().addNewParents(personID);
            editor.getView().applyChanges(changeSet, true);

            if (!event.memo.noUndoRedo)
                editor.getUndoRedoManager().addState( event );

            return changeSet["new"][0]; // new relationship
        },

        handlePersonNewSibling: function(event)
        {
            console.log("event: " + event.eventName + ", memo: " + Helpers.stringifyObject(event.memo));

            // { "personID": id, "childParams": data.params.parameters, "preferLeft": false };
            var personID    = event.memo.personID;
            var childParams = event.memo.childParams ? Helpers.cloneObject(event.memo.childParams) : {};
            var numTwins    = event.memo.twins ? event.memo.twins : 1;
            var numPersons  = event.memo.groupSize ? event.memo.groupSize : 0;

            var parentRelationship = editor.getGraph().getParentRelationship(personID);

            if (parentRelationship === null) {
                // need to add new parents
                parentRelationship = editor.getController().handlePersonNewParents( { "memo": { "personID": personID, "noUndoRedo": true } } );
            }

            if (event.memo.twins) {
                var nextEvent = { "nodeID": personID, "modifications": { "addTwin": event.memo.twins }, "noUndoRedo": true };
                editor.getController().handleModification( { "memo": nextEvent } );
            }
            else
            {
                var nextEvent = { "partnershipID": parentRelationship, "childParams": childParams, "noUndoRedo": true };
                if (event.memo.groupSize)
                    nextEvent["groupSize"] = event.memo.groupSize;

                editor.getController().handleRelationshipNewChild( { "memo": nextEvent } );
            }

            if (!event.memo.noUndoRedo)
                editor.getUndoRedoManager().addState( event );
        },

        handlePersonDragToNewSibling: function(event)
        {
            console.log("event: " + event.eventName + ", memo: " + Helpers.stringifyObject(event.memo));

            var sibling1 = event.memo.sibling1ID;
            var sibling2 = event.memo.sibling2ID;

            var parentRelationship = editor.getGraph().getParentRelationship(sibling1);
            if (parentRelationship == null)
                parentRelationship = editor.getGraph().getParentRelationship(sibling2);

            if (parentRelationship === null) {
                // need to add new parents
                parentRelationship = editor.getController().handlePersonNewParents( { "memo": { "personID": sibling1, "noUndoRedo": true } } );
            }

            if (editor.getGraph().getParentRelationship(sibling2) != parentRelationship) {
                // assign sibling 2 to this relationship: covers the case when none have parents or sibling1 has parents
                editor.getController().handlePersonDragToNewParent( { "memo": { "personID": sibling2, "parentID": parentRelationship, "noUndoRedo": true } } );
            } else {
                // assign sibling 1 to this relationship
                editor.getController().handlePersonDragToNewParent( { "memo": { "personID": sibling1, "parentID": parentRelationship, "noUndoRedo": true } } );
            }

            if (!event.memo.noUndoRedo)
                editor.getUndoRedoManager().addState( event );
        },

        handlePersonNewPartnerAndChild: function(event)
        {
            var timer = new Helpers.Timer();

            try
            {
            console.log("event: " + event.eventName + ", memo: " + Helpers.stringifyObject(event.memo));

            var personID    = event.memo.personID;
            if (!editor.getGraph().isPerson(personID)) return;
            var preferLeft  = event.memo.preferLeft;
            var childParams = event.memo.childParams ? Helpers.cloneObject(event.memo.childParams) : {};
            var numTwins    = event.memo.twins ? event.memo.twins : 1;
            var numPersons  = event.memo.groupSize ? event.memo.groupSize : 0;

            if (editor.getGraph().isChildless(personID)) {
                childParams["adoptedStatus"] = "adoptedIn";
            }

            if (numPersons > 0) {
                childParams["numPersons"] = numPersons;
            }

            if (editor.getPreferencesManager().getConfigurationOption("propagateFatherLastName")) {
                var lastName = null;
                if (editor.getGraph().getGender(personID) == "M") {
                    lastName = editor.getGraph().getLastName(personID);
                }
                if (lastName && lastName != "") {
                    if (childParams.hasOwnProperty("gender") && childParams.gender == 'M') {
                        childParams["lName"] = lastName;
                    } else {
                        childParams["lNameAtB"] = lastName;
                    }
                }
            }

            var changeSet = editor.getGraph().addNewRelationship(personID, childParams, preferLeft, numTwins);
            editor.getView().applyChanges(changeSet, true);

            if (!event.memo.noUndoRedo)
                editor.getUndoRedoManager().addState( event );

            } catch(err) {
                console.log("err: " + err);
            }

            timer.printSinceLast("=== Total new partner+child runtime: ");
        },

        handlePersonDragToNewPartner: function(event)
        {
            console.log("event: " + event.eventName + ", memo: " + Helpers.stringifyObject(event.memo));

            var personID  = event.memo.personID;
            var partnerID = event.memo.partnerID;
            if (!editor.getGraph().isPerson(personID) || !editor.getGraph().isPerson(partnerID)) return;

            var childProperties = {};
            if (editor.getGraph().isChildless(personID) || editor.getGraph().isChildless(partnerID)) {
                childProperties["adoptedStatus"] = "adoptedIn";
            }

            // when partnering up a node with unknown gender with a node of known gender
            // change the unknown gender to the opposite of known
            var node1 = editor.getView().getNode(personID);
            var node2 = editor.getView().getNode(partnerID);

            if (node1.getGender() == "U" && node2.getGender() != "U") {
                var gender1 = editor.getGraph().getOppositeGender(partnerID);
                node1.setGender(gender1);
                editor.getGraph().setProperties( personID, node1.getProperties() );
            }
            else if (node1.getGender() != "U" && node2.getGender() == "U") {
                var gender2 = editor.getGraph().getOppositeGender(personID);
                node2.setGender(gender2);
                editor.getGraph().setProperties( partnerID, node2.getProperties() );
            }

            if (editor.getPreferencesManager().getConfigurationOption("propagateFatherLastName")) {
                var lastName = null;
                if (node1.getGender() == "M") {
                    lastName = editor.getGraph().getLastName(personID);
                } else if (node2.getGender() == "M") {
                    lastName = editor.getGraph().getLastName(partnerID);
                }
                if (lastName && lastName != "") {
                    childProperties["lNameAtB"] = lastName;
                }
            }

            // TODO: propagate change of gender down the partnership chain

            var changeSet = editor.getGraph().assignPartner(personID, partnerID, childProperties);
            editor.getView().applyChanges(changeSet, true);

            if (!event.memo.noUndoRedo)
                editor.getUndoRedoManager().addState( event );
        },

        handleRelationshipNewChild: function(event)
        {
            console.log("event: " + event.eventName + ", memo: " + Helpers.stringifyObject(event.memo));

            var partnershipID = event.memo.partnershipID;
            if (!editor.getGraph().isRelationship(partnershipID)) return;

            var numTwins = event.memo.twins ? event.memo.twins : 1;

            var childParams = Helpers.cloneObject(event.memo.childParams);
            if (editor.getGraph().isInfertile(partnershipID)) {
                childParams["adoptedStatus"] = "adoptedIn";
            }

            var numPersons = event.memo.groupSize ? event.memo.groupSize : 0;
            if (numPersons > 0) {
                childParams["numPersons"] = numPersons;
            }

            if (editor.getPreferencesManager().getConfigurationOption("propagateFatherLastName")) {
                var lastName = editor.getGraph().getRelationshipChildLastName(partnershipID);
                if (lastName) {
                    if (childParams.gender == "M") {
                        childParams["lName"] = lastName;
                    } else {
                        childParams["lNameAtB"] = lastName;
                    }
                }
            }

            // check if there is a placeholder child which has to be replaced by the selected child type
            var children = editor.getGraph().getRelationshipChildrenSortedByOrder(partnershipID);
            if (children.length == 1 && editor.getGraph().isPlaceholder(children[0])) {
                var changeSet = editor.getGraph().convertPlaceholderTo(children[0], childParams);

                if (editor.getGraph().isChildlessByChoice(partnershipID)) {
                    var partnership = editor.getView().getNode(partnershipID);
                    partnership.setChildlessStatus(null);
                    var newProperties = partnership.getProperties();
                    editor.getGraph().setProperties( partnershipID, newProperties );
                }
            }
            else {
                var changeSet = editor.getGraph().addNewChild(partnershipID, childParams, numTwins);
            }

            editor.getView().applyChanges(changeSet, true);

            if (!event.memo.noUndoRedo)
                editor.getUndoRedoManager().addState( event );
        }
    });

    Controller._validatePropertyValue = function( nodeID, propertySetFunction, propValue)
    {
        if (propertySetFunction == "setGender") {
            var possibleGenders = editor.getGraph().getPossibleGenders(nodeID);
            //console.log("valid genders: " + Helpers.stringifyObject(possibleGenders));
            return possibleGenders[propValue];
        }
        return true;
    }

    Controller._propagateLastNameAtBirth = function( parentID, parentLastName, changeIfEqualTo )
    {
        var children = editor.getGraph().getAllChildren(parentID);

        for (var i = 0; i < children.length; i++) {
            var childID   = children[i];
            var childNode = editor.getView().getNode(childID);

            if (childNode.getLastNameAtBirth() == changeIfEqualTo ||
                (childNode.getLastNameAtBirth() == "" &&
                 (childNode.getLastName() == "" || (childNode.getGender() == 'M' && childNode.getLastName() == changeIfEqualTo))
               )) {
                if (childNode.getGender() == 'M' && childNode.getLastNameAtBirth() != changeIfEqualTo) {
                    childNode.setLastName(parentLastName);
                } else {
                    childNode.setLastNameAtBirth(parentLastName);
                }
                var allProperties = childNode.getProperties();
                editor.getGraph().setProperties( childID, allProperties );
                if (childNode.getGender() == 'M') {
                    Controller._propagateLastNameAtBirth(childID, parentLastName, changeIfEqualTo);
                }
            }
        }
    }

    Controller._checkPatientLinkValidity = function(callbackOnValid, nodeID, linkID, loadPatientProperties)
    {
        var onCancelAssignPatient = function() {
            // clear link input field in node menu
            editor.getNodeMenu().update();
        }

        if (loadPatientProperties) {
            var processLinkCallback = function(clearParameter) {
                callbackOnValid(clearParameter, true);
            }
        } else {
            var processLinkCallback = function(clearParameter) {
                callbackOnValid(clearParameter, false);
            }
        }

        if (linkID == "") {
            var oldLinkID = editor.getNode(nodeID).getPhenotipsPatientId();
            editor.getOkCancelDialogue().showWithCheckbox("<br><b>Do you want to remove the link between this individual and patient record " + oldLinkID + "?</b>" +
                    "<br><br><div style='margin-left: 40px; margin-right: 20px; text-align: left'>Note that, unless you link the patient record to another individual in this pedigree, " +
                    "the patient will be removed from this family and will no longer have a pedigree.</div>",
                    'Remove the connections?', 'Clear data from this pedigree node', true, "Remove link", processLinkCallback, "Cancel", onCancelAssignPatient );
            return;
        }

        var allLinkedNodes = editor.getGraph().getAllPatientLinks();
        if (allLinkedNodes.patientToNodeMapping.hasOwnProperty(linkID)) {
            var currentLinkedNodeID = allLinkedNodes.patientToNodeMapping[linkID];
            editor.getView().unmarkAll();
            editor.getView().markNode(currentLinkedNodeID);
            var onCancel = function() {
                editor.getView().unmarkAll();
                onCancelAssignPatient();
            }
            editor.getOkCancelDialogue().showWithCheckbox("<br>Patient record " + linkID + " is already linked to a different individual node in this pedigree. "+
                                                   "Do you want to link the patient record to this individual instead? "+
                                                   "If you do, the node currently representing the patient will no longer be linked to it.<br><br>",
                                                   "Re-link patient " + linkID + " to this node?",
                                                   'Clear data from the pedigree node currently linked to this patient', true,
                                                   "OK", processLinkCallback, "Cancel", onCancel );
            return;
        }

        var familyServiceURL = editor.getExternalEndpoint().getFamilyCheckLinkURL();
        new Ajax.Request(familyServiceURL, {
            method: 'POST',
            onSuccess: function(response) {
                if (response.responseJSON) {
                    if (!response.responseJSON.validLink) {
                        SaveLoadEngine._displayFamilyPedigreeInterfaceError(response.responseJSON,
                                "Can't link to this person", "Can't link to this person: ", onCancelAssignPatient);
                    } else {
                        if (loadPatientProperties) {
                            var clearPropertiesMsg = "<br><br>3) All data entered for this individual in the pedigree will be replaced by information pulled from the patient record  " + linkID + ".";
                        } else {
                            var clearPropertiesMsg = "";
                        }

                        var setDoNotShow = function(checkBoxStatus) {
                            if (checkBoxStatus) {
                                editor.getPreferencesManager().setConfigurationOption("user", "hideShareConsentDialog", true);
                            }
                        };

                        var processLinking = function(topMessage, notesMessage) {
                            var alreadyWasInFamily = editor.isFamilyMember(linkID);
                            if (!alreadyWasInFamily && !editor.getPreferencesManager().getConfigurationOption("hideShareConsentDialog")) {
                                editor.getOkCancelDialogue().showWithCheckbox("<br><b>" + topMessage + "</b><br><br><br>" +
                                        "<div style='margin-left: 30px; margin-right: 30px; text-align: left'>Please note that:<br><br>"+
                                        notesMessage +  "</div>",
                                        "Add patient to the family?",
                                        "Do not show this warning again<br>", false,
                                        "Confirm", function(checkBoxStatus) { setDoNotShow(checkBoxStatus); processLinkCallback() },
                                        "Cancel",  function(checkBoxStatus) { setDoNotShow(checkBoxStatus); onCancelAssignPatient() });
                            } else {
                                processLinkCallback();
                            }
                        }

                        processLinking("Do you want to add patient " + linkID + " to this family?",
                                "1) This pedigree will be shared between all members of the family, including this patient.<br><br>"+
                                "2) Adding a patient to a family will automatically grant users who can modify that patient's record the same level of access to the family page."+
                                clearPropertiesMsg);
                    }
                } else  {
                    editor.getOkCancelDialogue().showError('Server error - unable to verify validity of patient link',
                            'Error verifying patient link', "OK", onCancelAssignPatient );
                }
            },
            parameters: {"family_id": editor.getFamilyData().getFamilyId(), "patient_to_link_id": linkID }
        });
    }

    return Controller;
});
