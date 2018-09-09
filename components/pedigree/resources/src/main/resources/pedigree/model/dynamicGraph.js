// DynamicPositionedGraph adds support for online modifications and provides a convenient API for UI implementations
define([
        "pedigree/model/baseGraph",
        "pedigree/model/positionedGraph",
        "pedigree/model/helpers",
        "pedigree/model/import",
        "pedigree/model/phenotipsJSON"
    ], function(
        BaseGraph,
        PositionedGraph,
        Helpers,
        PedigreeImport,
        PhenotipsJSON
    ){
    DynamicPositionedGraph = function( drawGraph )
    {
        this.DG = drawGraph;

        this._unlinkedMembers = []; // array of PT ids

        this._onlyProbandGraph = '[{"id": 0, "proband":true}]';  // a string in SimpleJSON format, used to create a new blank pedigree
    };

    DynamicPositionedGraph.makeEmpty = function()
    {
        var baseG = new BaseGraph();
        var positionedG = new PositionedGraph(baseG);
        return new DynamicPositionedGraph(positionedG);
    }

    DynamicPositionedGraph.prototype = {

        setProbandId: function(id)
        {
            this.DG.probandId = id;
        },

        getProbandId: function()
        {
            return this.DG.probandId;
        },

        setUnlinkedPatients: function(unlinkedMembers)
        {
            this._unlinkedMembers = unlinkedMembers;
        },

        getAllPatientLinks: function()
        {
            var linkedPatientsList = { "linkedPatients": [], "patientToNodeMapping": {} };

            for (var i = 0 ; i <= this.getMaxNodeId(); i++) {
                if (this.isPerson(i)) {
                    if (this.getProperties(i).hasOwnProperty("phenotipsId")) {
                        linkedPatientsList.linkedPatients.push( this.getProperties(i).phenotipsId );
                        linkedPatientsList.patientToNodeMapping[this.getProperties(i).phenotipsId] = i;
                    }
                }
            }
            return linkedPatientsList;
        },

        getCurrentPatientId: function()
        {
            return editor.getExternalEndpoint().getParentDocument().id;
        },

        getCurrentPatientNodeID: function()
        {
            var allLinkedNodes = this.getAllPatientLinks();
            if (!allLinkedNodes.patientToNodeMapping.hasOwnProperty(this.getCurrentPatientId())) {
                return null;
            }
            return allLinkedNodes.patientToNodeMapping[this.getCurrentPatientId()];
        },

        getPhenotipsLinkID: function( id ) {
            return this.getProperties(id).hasOwnProperty("phenotipsId") ? this.getProperties(id).phenotipsId : "";
        },

        getPatientDescription: function( id, htmlPatientLink ) {
            var phenotipsId = this.getPhenotipsLinkID(id);
            var space = " ";
            if (htmlPatientLink) {
                var space = "&nbsp;";
                if (phenotipsId != "") {
                    phenotipsId = editor.getExternalEndpoint().getPhenotipsPatientHTMLLink(phenotipsId);
                }
            }
            var props = this.getProperties(id);
            var firstName = props.hasOwnProperty("fName") ? props.fName : "";
            var lastName = props.hasOwnProperty("lName") ? props.lName : "";
            var fullName = firstName + ((firstName != "" && lastName != "") ? space : "") + lastName;
            var description = phenotipsId + ((phenotipsId != "" && fullName != "")? "," + space : "") + fullName;
            return description;
        },

        isValidID: function( id )
        {
          if (id < 0 || id > this.DG.GG.getMaxRealVertexId())
            return false;
          if (!this.DG.GG.isPerson(id) && !this.DG.GG.isRelationship(id))
            return false;
          return true;
        },

        getMaxNodeId: function()
        {
            return this.DG.GG.getMaxRealVertexId();
        },

        isPersonGroup: function( id )
        {
            return this.getProperties(id).hasOwnProperty("numPersons");
        },

        isPerson: function( id )
        {
            return this.DG.GG.isPerson(id);
        },

        isRelationship: function( id )
        {
            return this.DG.GG.isRelationship(id);
        },

        isPlaceholder: function( id )
        {
            return this.DG.GG.isPlaceholder(id);
        },

        isAdoptedIn: function( id )
        {
            if (!this.isPerson(id))
                throw "Assertion failed: isAdopted() is applied to a non-person";
            return this.DG.GG.isAdoptedIn(id);
        },

        isAdoptedOut: function( id )
        {
            if (!this.isPerson(id))
                throw "Assertion failed: isAdopted() is applied to a non-person";
            return this.DG.GG.isAdoptedOut(id);
        },

        getAllPersonIDs: function()
        {
            return this.DG.GG.getAllPersons();
        },

        getGeneration: function( id )
        {
            var minRank = Math.min.apply(null, this.DG.ranks);
            return (this.DG.ranks[id] - minRank)/2 + 1;
        },

        getOrderWithinGeneration: function( id )
        {
            if (!this.isPerson(id))
                throw "Assertion failed: getOrderWithinGeneration() is applied to a non-person";

            var order = 0;
            var rank  = this.DG.ranks[id];
            for (var i = 0; i < this.DG.order.order[rank].length; i++) {
                var next = this.DG.order.order[rank][i];
                if (this.DG.GG.isPerson(next) && !this.DG.GG.isPlaceholder(next)) order++;
                if (next == id) break;
            }
            return order;
        },

        allowManualNodeRepositionLeft: function( id )
        {
            var rank  = this.DG.ranks[id];
            return (this._findOrderOfNeighbourPerson(this.DG.order, rank, this.DG.order.vOrder[id], -1) !== null);
        },

        allowManualNodeRepositionRight: function( id )
        {
            var rank  = this.DG.ranks[id];
            return (this._findOrderOfNeighbourPerson(this.DG.order, rank, this.DG.order.vOrder[id], +1) !== null);
        },

        // returns null if person has no twins
        getTwinGroupId: function( id )
        {
            return this.DG.GG.getTwinGroupId(id);
        },

        // returns and array of twins, sorted by order left to right. Always contains at least "id" itself
        getAllTwinsSortedByOrder: function( id )
        {
            var twins = this.DG.GG.getAllTwinsOf(id);
            var vOrder = this.DG.order.vOrder;
            var byOrder = function(a,b){ return vOrder[a] - vOrder[b]; };
            twins.sort( byOrder );
            return twins;
        },

        isFetus: function( id )
        {
            if (!this.isPerson(id)) {
                throw "Assertion failed: isFetus() is applied to a non-person";
            }
            if (this.getProperties(id).hasOwnProperty("lifeStatus")) {
                if (this.getProperties(id).lifeStatus != "alive" &&
                    this.getProperties(id).lifeStatus != "deceased") {
                    return true;
                }
            }
            return false;
        },

        isChildless: function( id )
        {
            if (!this.getProperties(id).hasOwnProperty("childlessStatus"))
                return false;
            var res = (this.getProperties(id)["childlessStatus"] !== null);
            //console.log("childless status of " + id + " : " + res);
            return res;
        },

        isChildlessByChoice: function( id )
        {
            if (!this.getProperties(id).hasOwnProperty("childlessStatus"))
                return false;
            var res = (this.getProperties(id)["childlessStatus"] == 'childless');
            return res;
        },

        isInfertile: function( id )
        {
            if (!this.getProperties(id).hasOwnProperty("childlessStatus"))
                return false;
            var res = (this.getProperties(id)["childlessStatus"] == 'infertile');
            return res;
        },

        isConsangrRelationship: function( id )
        {
            if (!this.isRelationship(id))
                throw "Assertion failed: isConsangrRelationship() is applied to a non-relationship";

            return this.DG.consangr.hasOwnProperty(id);
        },

        getProperties: function( id )
        {
            return this.DG.GG.properties[id];
        },

        setProperties: function( id, newSetOfProperties )
        {
            this.DG.GG.properties[id] = newSetOfProperties;
        },

        // TODO: completely remove RAW JSON from base graph, it is only needed for loading pedigree
        //       (so can convert to internal pedigre JSON before storing in BaseGraph) and saving full
        //       patient record JSON for drag/dop and saving (implemented in PatientRecordData class)
        //       Raw JSON is only left here for now to reduce the amount of refactoring
        getRawJSONProperties: function( id )
        {
            return this.DG.GG.rawJSONProperties[id];
        },

        setRawJSONProperties: function( id, newSetOfProperties )
        {
            this.DG.GG.rawJSONProperties[id] = newSetOfProperties;
        },

        getNodePropertiesNotStoredInPatientProfile: function(id)
        {
            var allProperties = this.getProperties(id);
            if (allProperties === undefined) {
                return {};
            }

            // TODO: review
            var keepProperties = [ 'lNameAtB', 'adoptedStatus', 'childlessStatus', 'childlessReason',
                                   'cancers', 'ethnicities', 'twinGroup', 'monozygotic', 'evaluated',
                                   'carrierStatus', 'lostContact', 'nodeNumber', 'comments', 'aliveandwell',
                                   'deceasedAge', 'deceasedCause', 'placeholder','numPersons'];

            var result = {};
            for (var i = 0; i < keepProperties.length; i++) {
                if (allProperties.hasOwnProperty(keepProperties[i])) {
                    result[keepProperties[i]] = allProperties[keepProperties[i]];
                }
            }
            return result;
        },

        setPersonNodeDataFromPhenotipsJSON: function( id, patientObject )
        {
            if (!this.isPerson(id)) {
                throw "Assertion failed: setPersonNodeDataFromPhenotipsJSON() is applied to a non-person";
            }

            if (patientObject != null && !patientObject.hasOwnProperty("__ignore__")) {
                var pedigreeOnlyProperties = this.getNodePropertiesNotStoredInPatientProfile(id);
                this.setProperties(id, PhenotipsJSON.phenotipsJSONToInternal(patientObject, pedigreeOnlyProperties));
            }
        },

        _isDisplayedAsConsanguinous: function( v ) {
            var consangr = this.isConsangrRelationship(v);
            var nodeConsangrPreference = this.getProperties(v).consangr;
            if (nodeConsangrPreference == "N")
                consangr = false;
            if (nodeConsangrPreference == "Y")
                consangr = true;
            return consangr;
        },

        getPatientPhenotipsJSON: function( v )
        {
            if (!this.isPerson(v)) {
                throw "Assertion failed: getPatientPhenotipsJSON() is applied to a non-person";
            }

            var consangr = "N";
            var parentPartnershipNode = editor.getGraph().getParentRelationship(v);
            if (parentPartnershipNode && parentPartnershipNode !== null && this._isDisplayedAsConsanguinous(parentPartnershipNode)) {
                consangr = "Y";
            }

            var relationshipProperties = {};
            relationshipProperties.consangr = consangr;

            return PhenotipsJSON.internalToPhenotipsJSON(this.getProperties(v), relationshipProperties);
        },

        getRelationshipExternalJSON: function( v )
        {
            if (!this.isRelationship(v)) {
                throw "Assertion failed: getRelationshipExternalJSON() is applied to a non-relationship";
            }

            var externalProperties = {};

            var properties = this.getProperties(v);
            if (properties.hasOwnProperty("broken")) {
                externalProperties["separated"] = properties.broken;
            }
            if (properties.hasOwnProperty("consangr") && (properties.consangr == "Y" || properties.consangr == "N")) {
                externalProperties["consanguinity"] = (properties.consangr == "Y") ? "yes" : "no";
            }
            if (properties.hasOwnProperty("childlessReason") && properties.childlessReason) {
                externalProperties["childlessReason"] = properties.childlessReason;
            }
            if (properties.hasOwnProperty("childlessStatus") && properties.childlessStatus) {
                externalProperties["childlessStatus"] = properties.childlessStatus;
            }
            return externalProperties;
        },

        getPosition: function( v )
        {
            // returns coordinates of node v
            var x = this.DG.positions[v];

            var rank = this.DG.ranks[v];

            var vertLevel = this.DG.GG.isChildhub(v) ? this.DG.vertLevel.childEdgeLevel[v] : 1;

            var y = this.DG.computeNodeY(rank, vertLevel);

            if (this.DG.GG.isVirtual(v)) {
                var relId    = this.DG.GG.downTheChainUntilNonVirtual(v);
                var personId = this.DG.GG.upTheChainUntilNonVirtual(v);

                var rankPerson = this.DG.ranks[personId];
                if (rank == rankPerson) {
                    var level = this.DG.vertLevel.outEdgeVerticalLevel[personId][relId].verticalLevel;
                    y = this.DG.computeRelLineY(rank, 0, level).relLineY;
                }

                var rankRelationship = this.DG.ranks[relId];
                if (rank == rankRelationship) {
                    y = this.getPosition(relId).y;
                }
            }
            else
            if (this.isRelationship(v)) {
                var partners = this.DG.GG.getParents(v);
                var level1   = this.DG.vertLevel.outEdgeVerticalLevel[partners[0]].hasOwnProperty(v) ? this.DG.vertLevel.outEdgeVerticalLevel[partners[0]][v].verticalLevel : 0;
                var level2   = this.DG.vertLevel.outEdgeVerticalLevel[partners[1]].hasOwnProperty(v) ? this.DG.vertLevel.outEdgeVerticalLevel[partners[1]][v].verticalLevel : 0;
                var level    = Math.min(level1, level2);
                var attach1  = this.DG.vertLevel.outEdgeVerticalLevel[partners[0]].hasOwnProperty(v) ? this.DG.vertLevel.outEdgeVerticalLevel[partners[0]][v].attachlevel : 0;
                var attach2  = this.DG.vertLevel.outEdgeVerticalLevel[partners[1]].hasOwnProperty(v) ? this.DG.vertLevel.outEdgeVerticalLevel[partners[1]][v].attachlevel : 0;
                var attach   = Math.min(attach1, attach2);
                y = this.DG.computeRelLineY(rank, attach, level).relLineY;
            }

            return {"x": x, "y": y};
        },

        getRelationshipChildLastName: function( v )
        {
            // 1) use father's last name, if available. If not return null.
            // 2) if all children have either no lastNameAtBirth or it matches fathers l-name, return fathers l-name;
            //    otherwise return null.

            if (!this.isRelationship(v))
                throw "Assertion failed: getRelationshipChildLastName() is applied to a non-relationship";

            var fatherLastName = null;

            var parents = this.DG.GG.getParents(v);
            if (this.getGender(parents[0]) == "M") {
                fatherLastName = this.DG.GG.getLastName(parents[0]);
            } else if (this.getGender(parents[1]) == "M") {
                fatherLastName = this.DG.GG.getLastName(parents[1]);
            } else {
                return null;
            }
            if (fatherLastName == "") return null;

            var childhubId = this.DG.GG.getRelationshipChildhub(v);
            var children = this.DG.GG.getOutEdges(childhubId);
            for (var i = 0; i < children.length; i++) {
                var childLastName = this.DG.GG.getLastNameAtBirth(children[i]);
                if (childLastName != "" && childLastName != fatherLastName) {
                    return null;
                }
            }
            return fatherLastName;
        },

        getRelationshipChildhubPosition: function( v )
        {
            if (!this.isRelationship(v))
                throw "Assertion failed: getRelationshipChildhubPosition() is applied to a non-relationship";

            var childhubId = this.DG.GG.getRelationshipChildhub(v);

            return this.getPosition(childhubId);
        },

        getRelationshipLineInfo: function( relationship, person )
        {
            if (!this.isRelationship(relationship))
                throw "Assertion failed: getRelationshipToPersonLinePosition() is applied to a non-relationship";
            if (!this.isPerson(person))
                throw "Assertion failed: getRelationshipToPersonLinePosition() is applied to a non-person";

            var info = this.DG.vertLevel.outEdgeVerticalLevel[person].hasOwnProperty(relationship) ?
                       this.DG.vertLevel.outEdgeVerticalLevel[person][relationship] :
                       { attachlevel: 0, verticalLevel: 0, numAttachLevels: 1 };

            //console.log("Info: " +  Helpers.stringifyObject(info));

            var verticalRelInfo = this.DG.computeRelLineY(this.DG.ranks[person], info.attachlevel, info.verticalLevel);

            var result = {"attachmentPort": info.attachlevel,
                          "attachY":        verticalRelInfo.attachY,
                          "verticalLevel":  info.verticalLevel,
                          "verticalY":      verticalRelInfo.relLineY,
                          "numAttachPorts": info.numAttachLevels };

            //console.log("rel: " + relationship + ", person: " + person + " => " + Helpers.stringifyObject(result));
            return result;
        },

        // returns all the children sorted by their order in the graph (left to right)
        getRelationshipChildrenSortedByOrder: function( v )
        {
            if (!this.isRelationship(v))
                throw "Assertion failed: getRelationshipChildren() is applied to a non-relationship";

            var childhubId = this.DG.GG.getRelationshipChildhub(v);

            var children = this.DG.GG.getOutEdges(childhubId);

            var vOrder = this.DG.order.vOrder;
            var byOrder = function(a,b){ return vOrder[a] - vOrder[b]; };
            children.sort( byOrder );

            return children;
        },

        getAllChildren: function( v )
        {
            return this.DG.GG.getAllChildren(v);
        },

        isChildOfProband: function( v )
        {
            var parents = this.DG.GG.getParents(v);
            if (Helpers.arrayContains(parents,this.getProbandId())) return true;
            return false;
        },

        isSiblingOfProband: function( v )
        {
            var siblings = this.DG.GG.getAllSiblingsOf(v);
            if (Helpers.arrayContains(siblings,this.getProbandId())) return true;
            return false;
        },

        isPartnershipRelatedToProband: function( v )
        {
            var parents = this.DG.GG.getParents(v);
            if (Helpers.arrayContains(parents, this.getProbandId())) return true;
            if (v == this.DG.GG.getProducingRelationship(this.getProbandId()))
            {
                return true;
            }
            return false;
        },

        // returns true iff node v is either a sibling, a child or a parent of proband node
        isRelatedToProband: function( v )
        {
            if (this.getProbandId() < 0) {
                return false;
            }

            var probandRelatedRels = this.getAllRelatedRelationships(this.getProbandId());
            for (var i = 0; i < probandRelatedRels.length; i++) {
                var rel = probandRelatedRels[i];

                var parents = this.DG.GG.getParents(rel);
                if (Helpers.arrayContains(parents, v)) return true;

                var children = this.getAllChildren(rel);
                if (Helpers.arrayContains(children, v)) return true;
            }
            return false;
        },

        // returns all relationships of node v and its parent relationship, if any
        getAllRelatedRelationships: function( v )
        {
            var allRels = this.DG.GG.getAllRelationships(v);
            var parentRel = this.DG.GG.getProducingRelationship(v);
            if (parentRel != null) {
                allRels.push(parentRel);
            }
            return allRels;
        },

        getAllSiblings: function( v )
        {
            if (!this.isPerson(v))
                throw "Assertion failed: getAllSiblings() is applied to a non-person";

            if (this.DG.GG.getInEdges(v).length == 0) {
                return [];
            }

            var chhub = this.DG.GG.getInEdges(v)[0];

            var siblings = this.DG.GG.getOutEdges(chhub).slice(0);

            Helpers.removeFirstOccurrenceByValue(siblings, v);

            return siblings;
        },

        isOnlyChild: function( v )
        {
            if (!this.isPerson(v))
                throw "Assertion failed: isOnlyPartnerlessChild() is applied to a non-person";
            if (this.DG.GG.getInEdges(v).length == 0) {
                return false;  // no parents => not a child of anyone
            }
            if (this.getAllSiblings(v).length == 0) {
                return true;  // no parents and no other isblings => only child
            }
            return false;     // has siblings
        },

        hasNonPlaceholderNonAdoptedChildren: function( v )
        {
            var children = [];
            if (this.isRelationship(v)) {
                children = this.getRelationshipChildrenSortedByOrder(v);
            }
            else if (this.isPerson(v)) {
                children = this.getAllChildren(v);
            }

            //console.log("Childtren: " + children);
            for (var i = 0; i < children.length; i++) {
                var child = children[i];
                if (!this.isPlaceholder(child) && !this.isAdoptedIn(child)) {
                    //console.log("child: " + child + ", isAdopted: " + this.isAdopted(child));
                    return true;
                }
            }

            return false;
        },

        hasNoNonPlaceholderChildren: function( v )
        {
            var children = [];
            if (this.isRelationship(v)) {
                children = this.getRelationshipChildrenSortedByOrder(v);
            }
            else if (this.isPerson(v)) {
                children = this.getAllChildren(v);
            }

            //console.log("Childtren: " + children);
            for (var i = 0; i < children.length; i++) {
                var child = children[i];
                if (!this.isPlaceholder(child)) {
                    //console.log("child: " + child + ", isAdopted: " + this.isAdopted(child));
                    return false;
                }
            }

            return true;
        },

        getParentRelationship: function( v )
        {
            if (!this.isPerson(v))
                throw "Assertion failed: getParentRelationship() is applied to a non-person";

            return this.DG.GG.getProducingRelationship(v);
        },

        // returns a bottom-to-top path
        getPathToParents: function(v)
        {
            // returns an array with two elements: path to parent1 (excluding v) and path to parent2 (excluding v):
            // [ [virtual_node_11, ..., virtual_node_1n, parent1], [virtual_node_21, ..., virtual_node_2n, parent21] ]
            return this.DG.GG.getPathToParents(v);
        },

        hasToBeAdopted: function( v )
        {
            if (!this.isPerson(v))
                throw "Assertion failed: hasToBeAdopted() is applied to a non-person";

            var parentRel = this.getParentRelationship(v);
            if (parentRel !== null && this.isChildless(parentRel))
                return true;
            return false;
        },

        hasRelationships: function( v )
        {
            if (!this.isPerson(v))
                throw "Assertion failed: hasRelationships() is applied to a non-person";

            return (this.DG.GG.v[v].length > 0);
        },

        getPossibleChildrenOf: function( v )
        {
            // all person nodes which are not ancestors of v and which do not already have parents
            var result = [];
            var persons = this.getAllPersonIDs();
            for (var i = 0; i < persons.length; i++) {
               var potentialChild = persons[i];
               if (this.DG.GG.inedges[potentialChild].length != 0) continue;
               if (this.DG.ancestors[v].hasOwnProperty(potentialChild)) continue;
               result.push(potentialChild);
            }
            return result;
        },

        getPossibleSiblingsOf: function( v )
        {
            // all person nodes which are not ancestors and not descendants
            // if v has parents only nodes without parents are returned
            var hasParents = (this.getParentRelationship(v) !== null);
            var result = [];
            var persons = this.getAllPersonIDs();
            for (var i = 0; i < persons.length; i++) {
               var personID = persons[i];
               if (this.DG.ancestors[v].hasOwnProperty(personID)) continue;
               if (this.DG.ancestors[personID].hasOwnProperty(v)) continue;
               if (hasParents && this.DG.GG.inedges[personID].length != 0) continue;
               result.push(personID);
            }
            return result;
        },

        getPossibleParentsOf: function( v )
        {
            // all person nodes which are not descendants of source node
            var result = [];
            //console.log("Ancestors: " + Helpers.stringifyObject(this.DG.ancestors));
            for (var i = 0; i <= this.DG.GG.getMaxRealVertexId(); i++) {
               if (!this.isRelationship(i) && !this.isPerson(i)) continue;
               if (this.isPersonGroup(i)) continue;
               if (this.isPlaceholder(i)) continue;
               if (this.DG.ancestors[i].hasOwnProperty(v)) continue;
               result.push(i);
            }
            return result;
        },

        /**
         * Returns all person who are not already a partner and who are not a fetus.
         */
        getPossiblePartnersOf: function( v )
        {
            var oppositeGender  = this.DG.GG.getOppositeGender(v);
            var preferredGendersSet = (oppositeGender == 'U') ? ['M','F','U','O'] : [oppositeGender,'U','O'];

            var validList = this.getAllPersonIDs();

            // exclude itself
            Helpers.removeFirstOccurrenceByValue( validList, v );

            // exclude partners
            var partners = this.DG.GG.getAllPartners(v);
            for (var i = 0; i < partners.length; i++) {
                Helpers.removeFirstOccurrenceByValue( validList, partners[i] );
            }

            var result = [];
            for (var i = 0; i < validList.length; i++) {
                var nodeId = validList[i];
                if (this.isFetus(nodeId)) {
                    continue;
                }
                // ok to add - either add as "preferred" or not
                var preferred = Helpers.arrayContains(preferredGendersSet, this.getGender(nodeId));

                var node = { "nodeID": nodeId, "preferred": preferred };
                result.push(node);
            }

            return result;
        },

        getPossiblePatientIDTarget: function() {
            // Valid targets:
            //  1) not currently linked to other patients
            //  2) not a group (n-person) node

            var allPersonNodes = this.getAllPersonIDs();
            var result = [];

            for (var i = 0; i < allPersonNodes.length; i++) {
                // exclude those nodes which already have a phenotipsID link
                if (this.getPhenotipsLinkID(allPersonNodes[i]) != "") {
                    continue;
                }
                // exclude group nodes
                if (this.isPersonGroup(i)) {
                    continue;
                }
                result.push(allPersonNodes[i]);
            }
            return result;
        },

        getOppositeGender: function( v )
        {
            if (!this.isPerson(v))
                throw "Assertion failed: getOppositeGender() is applied to a non-person";
            return this.DG.GG.getOppositeGender(v);
        },

        getGender: function( v )
        {
            if (!this.isPerson(v))
                throw "Assertion failed: getGender() is applied to a non-person";
            return this.DG.GG.getGender(v);
        },

        getLastName: function( v )
        {
            if (!this.isPerson(v))
                throw "Assertion failed: getLastName() is applied to a non-person";
            return this.DG.GG.getLastName(v);
        },

        getDisconnectedSetIfNodeRemoved: function( v )
        {
            var removedList = {};
            removedList[v] = true;

            if (this.isPerson(v)) {
                // note: removing the only child should not removes the relationship
                //       (need to convert this child to a placeholder instead)

                // also remove all relationships by this person
                var allRels = this.DG.GG.getAllRelationships(v);
                for (var i = 0; i < allRels.length; i++) {
                    removedList[allRels[i]] = true;
                }
            }

            // remove all childhubs of all relationships that need to be removed
            for (var node in removedList) {
                if (removedList.hasOwnProperty(node) && this.isRelationship(node)) {
                    var chhubId = this.DG.GG.getOutEdges(node)[0];
                    removedList[chhubId] = true;
                }
            }

            // go through all the edges in the tree starting from proband and disregarding any edges going to or from v
            var connected = {};

            var queue = new Queue();

            if (this.getProbandId() >= 0) {
                // proband is the node which is guaranteed to stay,
                // so need to find all nodes no longer connected to it - they will become the set of nodes to be removed
                queue.push( this.getProbandId() );
            } else {
                // TODO: review
                // we don't know which nodes the user wants to keep. For now, just use the node
                // with the smallest ID that is not being explicitly removed, and only keep nodes
                // connected to this smallest-id-node (e.g. will pick node with ID=0 in most cases,
                // effectively acting as before for old pedigrees)
                for (var i = 0; i < this.DG.GG.getNumVertices(); i++) {
                    if (i != v && this.isPerson(i) && !this.isPlaceholder(i)) {
                        queue.push( i );
                        break;
                    }
                }
            }

            while ( queue.size() > 0 ) {
                var next = parseInt(queue.pop());

                if (connected.hasOwnProperty(next)) continue;
                connected[next] = true;

                var outEdges = this.DG.GG.getOutEdges(next);
                for (var i = 0; i < outEdges.length; i++) {
                    if (!removedList.hasOwnProperty(outEdges[i]))
                        queue.push(outEdges[i]);
                }
                var inEdges = this.DG.GG.getInEdges(next);
                for (var i = 0; i < inEdges.length; i++) {
                    if (!removedList.hasOwnProperty(inEdges[i]))
                        queue.push(inEdges[i]);
                }
            }
            console.log("Connected nodes: " + Helpers.stringifyObject(connected));

            var affected = [];
            for (var i = 0; i < this.DG.GG.getNumVertices(); i++) {
                if (this.isPerson(i) || this.isRelationship(i)) {
                    if (!connected.hasOwnProperty(i))
                        affected.push(i);
                }
            }

            console.log("Affected nodes: " + Helpers.stringifyObject(affected));
            return affected;
        },

        _debugPrintAll: function( headerMessage )
        {
            console.log("========== " + headerMessage + " ==========");
            //console.log("== GG:");
            //console.log(Helpers.stringifyObject(this.DG.GG));
            //console.log("== Ranks:");
            //console.log(Helpers.stringifyObject(this.DG.ranks));
            //console.log("== Orders:");
            //console.log(Helpers.stringifyObject(this.DG.order));
            //console.log("== Positions:");
            //console.log(Helpers.stringifyObject(this.DG.positions));
            //console.log("== RankY:");
            //console.log(Helpers.stringifyObject(this.DG.rankY));
        },

        updateAncestors: function()   // sometimes have to do this after the "adopted" property change
        {
            var ancestors = this.DG.findAllAncestors();
            this.DG.ancestors = ancestors.ancestors;
            this.DG.consangr  = ancestors.consangr;

            // after consang has changes a random set or relationships may become/no longer be a consangr. relationship
            var movedNodes = [];
            for (var i = 0; i <= this.DG.GG.getMaxRealVertexId(); i++) {
                if (!this.isRelationship(i)) continue;
                movedNodes.push(i);
            }

            return { "moved": movedNodes };
        },

        addNewChild: function( childhubId, properties, numTwins )
        {
            this._debugPrintAll("before");
            var timer = new Helpers.Timer();

            if (!this.DG.GG.isChildhub(childhubId)) {
                if (this.DG.GG.isRelationship(childhubId))
                    childhubId = this.DG.GG.getRelationshipChildhub(childhubId);
                else
                    throw "Assertion failed: adding children to a non-childhub node";
            }

            var positionsBefore  = this.DG.positions.slice(0);
            var ranksBefore      = this.DG.ranks.slice(0);
            var vertLevelsBefore = this.DG.vertLevel.copy();
            var rankYBefore      = this.DG.rankY.slice(0);
            var numNodesBefore   = this.DG.GG.getMaxRealVertexId();

            if (!properties) properties = {};
            if (!numTwins) numTwins = 1;

            var insertRank = this.DG.ranks[childhubId] + 1;

            // find the best order to use for this new vertex: scan all orders on the rank, check number of crossed edges
            var insertOrder = this._findBestInsertPosition( insertRank, childhubId );

            // insert the vertex into the base graph and update ranks, orders & positions
            var newNodeId = this._insertVertex(BaseGraph.TYPE.PERSON, properties, 1.0, childhubId, null, insertRank, insertOrder);

            var newNodes = [newNodeId];
            for (var i = 0; i < numTwins - 1; i++ ) {
                var changeSet = this.addTwin( newNodeId, properties );
                newNodes.push(changeSet["new"][0]);
            }

            // validate: by now the graph should satisfy all assumptions
            this.DG.GG.validate();

            // fix common layout mistakes (e.g. relationship not right above the only child) - may mnove stuff horizontally
            this.DG.getHeuristics().improvePositioning();

            // update ancestors and vertical positioning of all edges
            this.DG.updateSecondaryStructures(ranksBefore, rankYBefore);

            timer.printSinceLast("=== AddChild runtime: ");
            this._debugPrintAll("after");

            var movedNodes = this._findMovedNodes( numNodesBefore, positionsBefore, ranksBefore, vertLevelsBefore, rankYBefore );
            var relationshipId = this.DG.GG.getInEdges(childhubId)[0];
            if (!Helpers.arrayContains(movedNodes,relationshipId))
                movedNodes.push(relationshipId);
            var animateNodes = this.DG.GG.getInEdges(relationshipId);  // animate parents if they move. if not, nothing will be done with them
            return {"new": newNodes, "moved": movedNodes, "animate": animateNodes};
        },

        addNewParents: function( personId )
        {
            this._debugPrintAll("before");
            var timer = new Helpers.Timer();

            if (!this.DG.GG.isPerson(personId))
                throw "Assertion failed: adding parents to a non-person node";

            if (this.DG.GG.getInEdges(personId).length > 0)
                throw "Assertion failed: adding parents to a person with parents";

            var positionsBefore  = this.DG.positions.slice(0);
            var ranksBefore      = this.DG.ranks.slice(0);
            var vertLevelsBefore = this.DG.vertLevel.copy();
            var rankYBefore      = this.DG.rankY.slice(0);
            var numNodesBefore   = this.DG.GG.getMaxRealVertexId();

            // a few special cases which involve not only insertions but also existing node rearrangements:
            this.DG.getHeuristics().swapBeforeParentsToBringToSideIfPossible( personId );

            var insertChildhubRank = this.DG.ranks[personId] - 1;

            // find the best order to use for this new vertex: scan all orders on the rank, check number of crossed edges
            var insertChildhubOrder = this._findBestInsertPosition( insertChildhubRank, personId );

            // insert the vertex into the base graph and update ranks, orders & positions
            var newChildhubId = this._insertVertex(BaseGraph.TYPE.CHILDHUB, {}, 1.0, null, personId, insertChildhubRank, insertChildhubOrder);

            var insertParentsRank = this.DG.ranks[newChildhubId] - 1;   // note: rank may have changed since last insertion
                                                                        //       (iff childhub was insertion above all at rank 0 - which becomes rank1)

            // find the best order to use for this new vertex: scan all orders on the rank, check number of crossed edges
            var insertParentOrder = this._findBestInsertPosition( insertParentsRank, newChildhubId );

            var newRelationshipId = this._insertVertex(BaseGraph.TYPE.RELATIONSHIP, {}, 1.0, null, newChildhubId, insertParentsRank, insertParentOrder);

            insertParentsRank = this.DG.ranks[newRelationshipId];       // note: rank may have changed since last insertion again
                                                                        //       (iff relationship was insertion above all at rank 0 - which becomes rank1)

            var newParent1Id = this._insertVertex(BaseGraph.TYPE.PERSON, {"gender": "F"}, 1.0, null, newRelationshipId, insertParentsRank, insertParentOrder + 1);
            var newParent2Id = this._insertVertex(BaseGraph.TYPE.PERSON, {"gender": "M"}, 1.0, null, newRelationshipId, insertParentsRank, insertParentOrder);

            // validate: by now the graph should satisfy all assumptions
            this.DG.GG.validate();

            // fix common layout mistakes (e.g. relationship not right above the only child) - may mnove stuff horizontally
            this.DG.getHeuristics().improvePositioning();

            // update ancestors and vertical positioning of all edges
            this.DG.updateSecondaryStructures(ranksBefore, rankYBefore);

            timer.printSinceLast("=== NewParents runtime: ");
            this._debugPrintAll("after");

            var animateNodes = this.DG.GG.getAllPartners(personId);
            if (animateNodes.length == 1)  // only animate node partners if there is only one - ow it may get too confusing with a lot of stuff animating around
                animateNodes.push(personId);
            else
                animateNodes = [personId];
            var movedNodes = this._findMovedNodes( numNodesBefore, positionsBefore, ranksBefore, vertLevelsBefore, rankYBefore );
            var newNodes   = [newRelationshipId, newParent1Id, newParent2Id];
            return {"new": newNodes, "moved": movedNodes, "highlight": [personId], "animate": animateNodes};
        },

        addNewRelationship: function( personId, childProperties, preferLeft, numTwins )
        {
            this._debugPrintAll("before");
            var timer = new Helpers.Timer();

            if (!this.DG.GG.isPerson(personId))
                throw "Assertion failed: adding relationship to a non-person node";

            var positionsBefore  = this.DG.positions.slice(0);
            var ranksBefore      = this.DG.ranks.slice(0);
            var vertLevelsBefore = this.DG.vertLevel.copy();
            var rankYBefore      = this.DG.rankY.slice(0);
            var consangrBefore   = this.DG.consangr;
            var numNodesBefore   = this.DG.GG.getMaxRealVertexId();

            if (!childProperties) childProperties = {};

            if (!numTwins) numTwins = 1;

            var partnerProperties = { "gender": this.DG.GG.getOppositeGender(personId) };

            var insertRank  = this.DG.ranks[personId];
            var personOrder = this.DG.order.vOrder[personId];

            var needSwap = true;
            var relProperties = {};
            if (childProperties.hasOwnProperty("placeholder") && childProperties["placeholder"]) {
                relProperties["childlessStatus"] = 'childless';
                var needSwap = false;
            }

            // a few special cases which involve not only insertions but also existing node rearrangements:
            this.DG.getHeuristics().swapPartnerToBringToSideIfPossible( personId );   // TODO: only iff "needSwap"?
            this.DG.getHeuristics().swapTwinsToBringToSideIfPossible( personId );

            // find the best order to use for this new vertex: scan all orders on the rank, check number of crossed edges
            var insertOrder = this._findBestInsertPosition( insertRank, personId, preferLeft );

            console.log("vOrder: " + personOrder + ", inserting @ " + insertOrder);
            console.log("Orders before: " + Helpers.stringifyObject(this.DG.order.order[this.DG.ranks[personId]]));

            var newRelationshipId = this._insertVertex(BaseGraph.TYPE.RELATIONSHIP, relProperties, 1.0, personId, null, insertRank, insertOrder);

            console.log("Orders after: " + Helpers.stringifyObject(this.DG.order.order[this.DG.ranks[personId]]));

            var insertPersonOrder = (insertOrder > personOrder) ? insertOrder + 1 : insertOrder;

            var newPersonId = this._insertVertex(BaseGraph.TYPE.PERSON, partnerProperties, 1.0, null, newRelationshipId, insertRank, insertPersonOrder);

            var insertChildhubRank  = insertRank + 1;
            var insertChildhubOrder = this._findBestInsertPosition( insertChildhubRank, newRelationshipId );
            var newChildhubId       = this._insertVertex(BaseGraph.TYPE.CHILDHUB, {}, 1.0, newRelationshipId, null, insertChildhubRank, insertChildhubOrder);

            var insertChildRank  = insertChildhubRank + 1;
            var insertChildOrder = this._findBestInsertPosition( insertChildRank, newChildhubId );
            var newChildId       = this._insertVertex(BaseGraph.TYPE.PERSON, childProperties, 1.0, newChildhubId, null, insertChildRank, insertChildOrder);

            var newNodes = [newRelationshipId, newPersonId, newChildId];
            for (var i = 0; i < numTwins - 1; i++ ) {
                var changeSet = this.addTwin( newChildId, childProperties );
                newNodes.push(changeSet["new"][0]);
            }

            console.log("Orders after all: " + Helpers.stringifyObject(this.DG.order.order[this.DG.ranks[personId]]));

            // validate: by now the graph should satisfy all assumptions
            this.DG.GG.validate();

            //this._debugPrintAll("middle");

            // fix common layout mistakes (e.g. relationship not right above the only child) - may mnove stuff horizontally
            this.DG.getHeuristics().improvePositioning();

            // update ancestors and vertical positioning of all edges
            this.DG.updateSecondaryStructures(ranksBefore, rankYBefore);

            timer.printSinceLast("=== NewRelationship runtime: ");
            this._debugPrintAll("after");

            var movedNodes = this._findMovedNodes( numNodesBefore, positionsBefore, ranksBefore, vertLevelsBefore, rankYBefore, consangrBefore );
            return {"new": newNodes, "moved": movedNodes, "highlight": [personId]};
        },

        assignParent: function( parentId, childId )
        {
            if (this.isRelationship(parentId)) {
                var childHubId   = this.DG.GG.getRelationshipChildhub(parentId);
                var rankChildHub = this.DG.ranks[childHubId];
                var rankChild    = this.DG.ranks[childId];

                var otherChildren = this.getAllChildren(parentId);

                var weight = 1;
                this.DG.GG.addEdge(childHubId, childId, weight);

                // this should be done after addEdge(), or the assumption of at least one child will
                // be violated at the time removeNodes() is executed, resulting in a validation failure
                if (otherChildren.length == 1 && this.isPlaceholder(otherChildren[0])) {
                    var removeChangeSet = this.removeNodes([otherChildren[0]]);
                }

                var animateList = [childId];

                if (rankChildHub != rankChild - 1) {
                    var removedList = removeChangeSet ? removeChangeSet.removed : [];
                    return this.redrawAll(removedList, animateList);
                }

                var positionsBefore  = this.DG.positions.slice(0);
                var ranksBefore      = this.DG.ranks.slice(0);
                var vertLevelsBefore = this.DG.vertLevel.copy();
                var rankYBefore      = this.DG.rankY.slice(0);
                var consangrBefore   = this.DG.consangr;
                var numNodesBefore   = this.DG.GG.getMaxRealVertexId();

                // TODO: move vertex closer to other children, if possible?

                // validate: by now the graph should satisfy all assumptions
                this.DG.GG.validate();

                // update vertical separation for all nodes & compute ancestors
                this.DG.updateSecondaryStructures(ranksBefore, rankYBefore);

                positionsBefore[parentId] = Infinity; // so that it is added to the list of moved nodes
                var movedNodes = this._findMovedNodes( numNodesBefore, positionsBefore, ranksBefore, vertLevelsBefore, rankYBefore, consangrBefore );

                if (removeChangeSet) {
                    removeChangeSet.moved = removeChangeSet.moved.concat(movedNodes);
                    removeChangeSet.moved = Helpers.filterUnique(removeChangeSet.moved);
                    removeChangeSet.animate = [childId];
                    return removeChangeSet;
                } else {
                    return {"moved": movedNodes, "animate": [childId]};
                }
            }
            else {
                var rankParent = this.DG.ranks[parentId];
                var rankChild  = this.DG.ranks[childId];

                var partnerProperties = { "gender": this.DG.GG.getOppositeGender(parentId) };

                //console.log("rankParent: " + rankParent + ", rankChild: " + rankChild );

                if (rankParent >= rankChild) {
                    var ranksBefore        = this.DG.ranks.slice(0);
                    // need a complete redraw, since this violates the core layout rule. In this case insert orders do not matter
                    var insertChildhubRank = rankChild - 1;
                    var newChildhubId      = this._insertVertex(BaseGraph.TYPE.CHILDHUB, {}, 1.0, null, childId, insertChildhubRank, 0);
                    var insertParentsRank  = this.DG.ranks[newChildhubId] - 1;   // note: rank may have changed since last insertion
                    var newRelationshipId  = this._insertVertex(BaseGraph.TYPE.RELATIONSHIP, {}, 1.0, null, newChildhubId, insertParentsRank, 0);
                    var newParentId        = this._insertVertex(BaseGraph.TYPE.PERSON, partnerProperties, 1.0, null, newRelationshipId, insertParentsRank, 0);
                    this.DG.GG.addEdge(parentId, newRelationshipId, 1);
                    var animateList = [childId, parentId];
                    var newList     = [newRelationshipId, newParentId];
                    return this.redrawAll(null, animateList, newList, ranksBefore);
                }

                // add new childhub     @ rank (rankChild - 1)
                // add new relationship @ rank (rankChild - 2)
                // add new parent       @ rank (rankChild - 2) right next to new relationship
                //                        (left or right depends on if the other parent is right or left)
                // depending on other parent rank either draw a multi-rank relationship edge or regular relationship edge

                this._debugPrintAll("before");
                var timer = new Helpers.Timer();

                var positionsBefore  = this.DG.positions.slice(0);
                var ranksBefore      = this.DG.ranks.slice(0);
                var vertLevelsBefore = this.DG.vertLevel.copy();
                var rankYBefore      = this.DG.rankY.slice(0);
                var consangrBefore   = this.DG.consangr;
                var numNodesBefore   = this.DG.GG.getMaxRealVertexId();

                var x_parent     = this.DG.positions[parentId];
                var x_child      = this.DG.positions[childId];

                if (rankParent == rankChild - 2) {
                    // the order of new node creation is then:
                    // 1) new relationship node
                    // 2) new partner
                    // 3) new childhub
                    var preferLeft = (x_child < x_parent);

                    // add same-rank relationship edge
                    var insertRelatOrder  = this._findBestInsertPosition( rankParent, parentId, preferLeft);
                    var newRelationshipId = this._insertVertex(BaseGraph.TYPE.RELATIONSHIP, {}, 1.0, parentId, null, rankParent, insertRelatOrder);

                    var newParentOrder = (this.DG.order.vOrder[parentId] > this.DG.order.vOrder[newRelationshipId]) ? insertRelatOrder : (insertRelatOrder+1);
                    var newParentId    = this._insertVertex(BaseGraph.TYPE.PERSON, partnerProperties, 1.0, null, newRelationshipId, rankParent, newParentOrder);

                    var insertChildhubRank  = rankChild - 1;
                    var insertChildhubOrder = this._findBestInsertPosition( insertChildhubRank, newRelationshipId );
                    var newChildhubId       = this._insertVertex(BaseGraph.TYPE.CHILDHUB, {}, 1.0, newRelationshipId, null, insertChildhubRank, insertChildhubOrder);

                    this.DG.GG.addEdge(newChildhubId, childId, 1);
                } else {
                    // need to add a multi-rank edge: order of node creation is different:
                    // 1) new childhub
                    // 2) new relationship node
                    // 3) new partner
                    // 4) multi-rank edge
                    // add a multi-rank relationship edge (e.g. a sequence of edges between virtual nodes on intermediate ranks)

                    var insertChildhubRank  = rankChild - 1;
                    var insertChildhubOrder = this._findBestInsertPosition( insertChildhubRank, childId );
                    var newChildhubId       = this._insertVertex(BaseGraph.TYPE.CHILDHUB, {}, 1.0, null, childId, insertChildhubRank, insertChildhubOrder);

                    var insertParentsRank = rankChild - 2;

                    var insertRelatOrder  = this._findBestInsertPosition( insertParentsRank, newChildhubId );
                    var newRelationshipId = this._insertVertex(BaseGraph.TYPE.RELATIONSHIP, {}, 1.0, null, newChildhubId, insertParentsRank, insertRelatOrder);

                    var newParentOrder = (this.DG.positions[parentId] > this.DG.positions[newRelationshipId]) ? insertRelatOrder : (insertRelatOrder+1);
                    var newParentId    = this._insertVertex(BaseGraph.TYPE.PERSON, partnerProperties, 1.0, null, newRelationshipId, insertParentsRank, newParentOrder);

                    this._addMultiRankEdge(parentId, newRelationshipId);
                }

                // validate: by now the graph should satisfy all assumptions
                this.DG.GG.validate();

                // fix common layout mistakes (e.g. relationship not right above the only child) - may mnove stuff horizontally
                this.DG.getHeuristics().improvePositioning();

                // update ancestors and vertical positioning of all edges
                this.DG.updateSecondaryStructures(ranksBefore, rankYBefore);

                timer.printSinceLast("=== DragToParentOrChild runtime: ");
                this._debugPrintAll("after");

                if (this.DG.positions.length >= 31)
                    console.log("position of node 32: " + this.DG.positions[32] + ", was: " + positionsBefore[32]);
                var movedNodes = this._findMovedNodes( numNodesBefore, positionsBefore, ranksBefore, vertLevelsBefore, rankYBefore, consangrBefore );
                var newNodes   = [newRelationshipId, newParentId];
                return {"new": newNodes, "moved": movedNodes, "highlight": [parentId, newParentId, childId]};
            }

        },

        assignPartner: function( person1, person2, childProperties ) {
            var positionsBefore  = this.DG.positions.slice(0);
            var ranksBefore      = this.DG.ranks.slice(0);
            var vertLevelsBefore = this.DG.vertLevel.copy();
            var rankYBefore      = this.DG.rankY.slice(0);
            var consangrBefore   = this.DG.consangr;
            var numNodesBefore   = this.DG.GG.getMaxRealVertexId();

            var rankP1 = this.DG.ranks[person1];
            var rankP2 = this.DG.ranks[person2];

            if (rankP1 < rankP2 ||
                (rankP1 == rankP2 && this.DG.order.vOrder[person2] < this.DG.order.vOrder[person1])
               ) {
                var tmpPerson = person2;
                person2       = person1;
                person1       = tmpPerson;

                rankP1 = rankP2;
                rankP2 = this.DG.ranks[person2];
            }

            var x_person1 = this.DG.positions[person1];
            var x_person2 = this.DG.positions[person2];

            var weight = 1;

            var preferLeft        = (x_person2 < x_person1);
            var insertRelatOrder  = (rankP1 == rankP2) ? this._findBestRelationshipPosition( person1, false, person2 ) :
                                                         this._findBestRelationshipPosition( person1, preferLeft);

            var relProperties = {};
            if (childProperties.hasOwnProperty("placeholder") && childProperties["placeholder"]) {
                relProperties["childlessStatus"] = 'childless';
            }
            var newRelationshipId = this._insertVertex(BaseGraph.TYPE.RELATIONSHIP, relProperties, weight, person1, null, rankP1, insertRelatOrder);

            var insertChildhubRank  = this.DG.ranks[newRelationshipId] + 1;
            var insertChildhubOrder = this._findBestInsertPosition( insertChildhubRank, newRelationshipId );
            var newChildhubId       = this._insertVertex(BaseGraph.TYPE.CHILDHUB, {}, 1.0, newRelationshipId, null, insertChildhubRank, insertChildhubOrder);

            var insertChildRank  = insertChildhubRank + 1;
            var insertChildOrder = this._findBestInsertPosition( insertChildRank, newChildhubId );
            var newChildId       = this._insertVertex(BaseGraph.TYPE.PERSON, childProperties, 1.0, newChildhubId, null, insertChildRank, insertChildOrder);

            if (rankP1 == rankP2) {
                this.DG.GG.addEdge(person2, newRelationshipId, weight);
            } else {
                this._addMultiRankEdge(person2, newRelationshipId);
            }

            // validate: by now the graph should satisfy all assumptions
            this.DG.GG.validate();

            // fix common layout mistakes (e.g. relationship not right above the only child) - may mnove stuff horizontally
            this.DG.getHeuristics().improvePositioning();

            // update ancestors and vertical positioning of all edges
            this.DG.updateSecondaryStructures(ranksBefore, rankYBefore);

            this._debugPrintAll("after");

            var movedNodes = this._findMovedNodes( numNodesBefore, positionsBefore, ranksBefore, vertLevelsBefore, rankYBefore, consangrBefore );
            var newNodes   = [newRelationshipId, newChildId];
            return {"new": newNodes, "moved": movedNodes, "highlight": [person1, person2, newChildId]};
        },

        addTwin: function( personId, properties )
        {
            var positionsBefore  = this.DG.positions.slice(0);
            var ranksBefore      = this.DG.ranks.slice(0);
            var vertLevelsBefore = this.DG.vertLevel.copy();
            var rankYBefore      = this.DG.rankY.slice(0);
            var numNodesBefore   = this.DG.GG.getMaxRealVertexId();

            var parentRel = this.DG.GG.getProducingRelationship(personId);

            var twinGroupId = this.DG.GG.getTwinGroupId(personId);
            if (twinGroupId === null) {
                twinGroupId = this.DG.GG.getUnusedTwinGroupId(parentRel);
                console.log("new twin id: " + twinGroupId);
                this.DG.GG.properties[personId]['twinGroup'] = twinGroupId;
            }
            properties['twinGroup'] = twinGroupId;

            var insertRank = this.DG.ranks[personId];

            // find the best order to use for this new vertex: scan all orders on the rank, check number of crossed edges
            var insertOrder = this.DG.findBestTwinInsertPosition(personId, []);

            // insert the vertex into the base graph and update ranks, orders & positions
            var childhubId = this.DG.GG.getInEdges(personId)[0];
            var newNodeId = this._insertVertex(BaseGraph.TYPE.PERSON, properties, 1.0, childhubId, null, insertRank, insertOrder);

            // validate: by now the graph should satisfy all assumptions
            this.DG.GG.validate();

            // fix common layout mistakes (e.g. relationship not right above the only child) - may mnove stuff horizontally
            this.DG.getHeuristics().improvePositioning();

            // update ancestors and vertical positioning of all edges
            this.DG.updateSecondaryStructures(ranksBefore, rankYBefore);

            var movedNodes = this._findMovedNodes( numNodesBefore, positionsBefore, ranksBefore, vertLevelsBefore, rankYBefore );
            if (!Helpers.arrayContains(movedNodes, parentRel))
                movedNodes.push(parentRel);
            var animateNodes = this.DG.GG.getInEdges(parentRel).slice(0);  // animate parents if they move. if not, nothing will be done with them
            animateNodes.push(personId);
            var newNodes   = [newNodeId];
            return {"new": newNodes, "moved": movedNodes, "animate": animateNodes};
        },

        convertPlaceholderTo: function( placeholderId, childParams )
        {
            var positionsBefore  = this.DG.positions.slice(0);
            var ranksBefore      = this.DG.ranks.slice(0);
            var vertLevelsBefore = this.DG.vertLevel.copy();
            var rankYBefore      = this.DG.rankY.slice(0);
            var numNodesBefore   = this.DG.GG.getMaxRealVertexId();

            if (!this.isPlaceholder(placeholderId)) {
                throw "Attemp to access a non-paceholder node as a placeholder";
            }

            if (!childParams.hasOwnProperty("gender")) {
                childParams["gender"] = "U";
            }
            this.setProperties(placeholderId, childParams);

            this.DG.getHeuristics().improvePositioning();

            // update ancestors and vertical positioning of all edges
            this.DG.updateSecondaryStructures(ranksBefore, rankYBefore);

            var parentRelationship = this.getParentRelationship(placeholderId);

            // need to redraw (move) the partnership-child line
            var moved = [ parentRelationship ];

            // may also need to redraw all partnership lines on this rank, and
            // some other nodes as well, since person width > placeholder width
            var rank = this.DG.ranks[placeholderId];
            for (var order = 0; order < this.DG.order.order[rank].length - 1; order++) {
                var v = this.DG.order.order[rank][order];

                if (this.DG.GG.isRelationship(v) && !Helpers.arrayContains(moved, v)) {
                    moved.push(v);
                }
            }

            var movedNodes = this._findMovedNodes( numNodesBefore, positionsBefore, ranksBefore, vertLevelsBefore, rankYBefore );

            moved = moved.concat(movedNodes);
            moved = Helpers.filterUnique(moved);
            Helpers.removeFirstOccurrenceByValue(moved, placeholderId);

            return {"removed": [ placeholderId ] , "new": [ placeholderId ], "moved": moved };
        },

        removeNodes: function( nodeList )
        {
            this._debugPrintAll("before");

            //var positionsBefore  = this.DG.positions.slice(0);
            //var ranksBefore      = this.DG.ranks.slice(0);
            //var vertLevelsBefore = this.DG.vertLevel.copy();
            //var rankYBefore      = this.DG.rankY.slice(0);
            //var consangrBefore   = this.DG.consangr;
            //var numNodesBefore   = this.DG.GG.getMaxRealVertexId();

            var oldMaxID = this.getMaxNodeId();

            var removed = nodeList.slice(0);
            removed.sort();
            var moved = [];

            for (var i = 0; i < nodeList.length; i++) {
                if (this.isRelationship(nodeList[i])) {
                    // also add its childhub
                    var chHub = this.DG.GG.getOutEdges(nodeList[i])[0];
                    nodeList.push(chHub);
                    //console.log("adding " + chHub + " to removal list (chhub of " + nodeList[i] + ")");

                    // also add its long multi-rank edges
                    var pathToParents = this.getPathToParents(nodeList[i]);
                    for (var p = 0; p < pathToParents.length; p++) {
                        for (var j = 0; j < pathToParents[p].path.length; j++) {
                            if (!this.DG.GG.isVirtual(pathToParents[p].path[j])) {
                                throw "Assertion failed: pathToParents has a non-virtual edge";
                            }
                            //console.log("adding " + pathToParents[p][j] + " to removal list (virtual of " + nodeList[i] + ")");
                            nodeList.push(pathToParents[p].path[j]);
                        }
                    }
                }
            }

            nodeList.sort(function(a,b){return a-b});

            //console.log("nodeList: " + Helpers.stringifyObject(nodeList));

            var changedIDSet = {};

            for (var i = nodeList.length-1; i >= 0; i--) {
                var v = nodeList[i];
                //console.log("removing: " + v);

                //// add person't relationship to the list of moved nodes
                //if (this.isPerson(v)) {
                //    var rel = this.DG.GG.getProducingRelationship(v);
                //    // rel may have been already removed
                //    if (rel !== null && !Helpers.arrayContains(nodeList, rel))
                //        moved.push(rel);
                //}

                // TODO: if this is one of only two twins remove twinGroupID from the remainin twin as it is no longer a twin

                this.DG.GG.remove(v);
                //console.log("order before: " + Helpers.stringifyObject(this.DG.order));
                this.DG.order.remove(v, this.DG.ranks[v]);
                //console.log("order after: " + Helpers.stringifyObject(this.DG.order));
                this.DG.ranks.splice(v,1);
                this.DG.positions.splice(v, 1);

                // for each removed node all nodes with higher ids get their IDs shifted down by 1
                for (var u = v + 1; u <= oldMaxID; u++) {
                    if (!changedIDSet.hasOwnProperty(u))
                        changedIDSet[u] = u - 1;
                    else
                        changedIDSet[u]--;
                }
            }

            // update proband ID, if it has changed
            if (changedIDSet.hasOwnProperty(this.getProbandId())) {
                this.setProbandId(changedIDSet[this.getProbandId()]);
            }

            this.DG.maxRank = Math.max.apply(null, this.DG.ranks);

            this.DG.GG.validate();

            // it is hard to provide old ranks since potentially all IDs have changed after a deletion
            // however we know after a deletion we dont need to update rank Y positioning, so we just skip the update
            this.DG.updateSecondaryStructures(null, null, true);

            // TODO: for now: redraw all relationships
            for (var i = 0 ; i <= this.getMaxNodeId(); i++) {
                if (this.isRelationship(i)) {
                    moved.push(i);
                }
            }

            // note: _findMovedNodes() does not work when IDs have changed. TODO
            //var movedNodes = this._findMovedNodes( numNodesBefore, positionsBefore, ranksBefore, vertLevelsBefore, rankYBefore );
            //for (var i = 0; i < moved.length; i++)
            //    if (!Helpers.arrayContains(movedNodes, moved[i]))
            //        movedNodes.push(moved[i]);

            // note: moved now has the correct IDs valid in the graph with all affected nodes removed
            return {"removed": removed, "changedIDSet": changedIDSet, "moved": moved };
        },

        // Not used at the moment
        // TODO: may need to call this after manual modifications of the graph
        improvePosition: function ()
        {
            var positionsBefore  = this.DG.positions.slice(0);
            var ranksBefore      = this.DG.ranks.slice(0);
            var vertLevelsBefore = this.DG.vertLevel.copy();
            var rankYBefore      = this.DG.rankY.slice(0);
            var numNodesBefore   = this.DG.GG.getMaxRealVertexId();

            // fix common layout mistakes (e.g. relationship not right above the only child) - may mnove stuff horizontally
            this.DG.getHeuristics().improvePositioning();

            // update vertical positioning of all edges: in theory it may be affected by the moves done in optimize() above
            this.DG.updateSecondaryStructures(ranksBefore, rankYBefore);

            var movedNodes = this._findMovedNodes( numNodesBefore, positionsBefore, ranksBefore, vertLevelsBefore, rankYBefore );

            return {"moved": movedNodes};
        },

        updateYPositioning: function ()
        {
            var positionsBefore  = this.DG.positions; //.slice(0); not changing, no need to copy
            var ranksBefore      = this.DG.ranks;     //.slice(0); not changing, no need to copy
            var vertLevelsBefore = this.DG.vertLevel; //.copy();   not changing, no need to copy
            var rankYBefore      = this.DG.rankY.slice(0);
            var numNodesBefore   = this.DG.GG.getMaxRealVertexId();

            this.DG.rankY = this.DG.computeRankY(ranksBefore, rankYBefore);

            var movedNodes = this._findMovedNodes( numNodesBefore, positionsBefore, ranksBefore, vertLevelsBefore, rankYBefore, null, true );

            if (movedNodes.length == 0) {
                return {};
            }
            return {"moved": movedNodes};
        },

        clearAll: function(isFamilyPage)
        {
            var removedNodes = this._getAllNodes();

            var emptyGraph = (this.DG.GG.getNumVertices() == 0);

            if (isFamilyPage) {
                // keep the proband, if any
                var remainingNodeProperties = (emptyGraph || (this.getProbandId() < 0)) ? {} : this.getProperties(this.getProbandId());
                var probandID = 0;
            } else {
                // keep the node linked to the current patient; if it was not the proband will have no proband
                var currentNodeId = this.getCurrentPatientNodeID();
                var remainingNodeProperties = (emptyGraph || (currentNodeId == null)) ? {} : this.getProperties(currentNodeId);
            }

            // it is easier to create abrand new graph transferirng node 0 propertie sthna to remove on-by-one
            // each time updating ranks, orders, etc

            var importData = PedigreeImport.initFromSimpleJSON(this._onlyProbandGraph);

            this._initializeFromBaseGraphAndLayout(importData.baseGraph, importData.probandNodeID, []);

            this.setProperties(0, remainingNodeProperties);

            if (emptyGraph) {
                return {"new": [0], "makevisible": [0]};
            }

            return {"removed": removedNodes, "new": [0], "makevisible": [0]};
        },

        // returns: "change set" object
        redrawAll: function (removedBeforeRedrawList, animateList, newList, ranksBefore)
        {
            var ranksBefore = ranksBefore ? ranksBefore : this.DG.ranks.slice(0);  // sometimes we want to use ranksbefore as they were before some stuff was added to the graph before a redraw

            this._debugPrintAll("before");

            this.DG.GG.collapseMultiRankEdges();

            // collect current node ranks so that the new layout can be made more similar to the current one
            var oldRanks = this.DG.ranks.slice(0);

            // attempt to re-use existing ranks in order to keep the new layout as close as possible to the current layout
            var suggestedLayout = { "ranks": oldRanks };

            if (!this._initializeFromBaseGraphAndLayout(this.DG.GG, this.getProbandId(), suggestedLayout)) {
                return {};  // no changes
            }

            var movedNodes = this._getAllNodes();

            var probandReRankSize = (ranksBefore[this.getProbandId()] - this.DG.ranks[this.getProbandId()]);
            var reRankedDiffFrom0 = []
            var reRanked          = [];
            var persons = this.getAllPersonIDs();
            for (var i = 0; i < persons.length; i++) {
                var nodeID = persons[i];
                if (this.DG.ranks[nodeID] != ranksBefore[nodeID]) {
                    reRanked.push(nodeID);
                }
                if ((ranksBefore[nodeID] - this.DG.ranks[nodeID]) != probandReRankSize) {
                    reRankedDiffFrom0.push(nodeID);
                }
            }
            if (reRankedDiffFrom0.length < reRanked.length) {
                reRanked = reRankedDiffFrom0;
            }

            if (!animateList) animateList = [];

            if (!removedBeforeRedrawList) removedBeforeRedrawList = [];

            if (!newList)
                newList = [];
            else {
                // nodes which are force-marked as new can't be in the "moved" list
                for (var i = 0; i < newList.length; i++)
                    Helpers.removeFirstOccurrenceByValue(movedNodes, newList[i]);
            }

            this._debugPrintAll("after");

            return { "new": newList,
                     "moved": movedNodes,
                     "highlight": reRanked,
                     "animate": animateList,
                     "removed": removedBeforeRedrawList };
        },

        //=============================================================

        /**
         * Creates a special serializationof the pedigree which is quick to save and load, and which
         * preserves IDs which is good for efficient undo/redos
         *
         * @return Serialization data for the entire graph
         */
        toUndoRedoState: function()
        {
            var serializationJSON = { "baseGraph": this.DG.GG.toJSONObject(),
                                      "probandNodeId": this.getProbandId(),
                                      "ranks": this.DG.ranks.slice(),
                                      "order": this.DG.order.toJSONObject(),
                                      "positions": this.DG.positions.slice()
                                    };

            return JSON.stringify(serializationJSON);
        },

        /**
         * A shortcut initializer which avoids error checkinhg and ensures node IDs are preserved
         * (which is essential for fast/efficient/reliable undo/redo)
         */
        fromUndoRedoState: function(undoRedoState)
        {
            var timer = new Helpers.Timer();

            var removedNodes = this._getAllNodes();

            var undoRedoJSON = JSON.parse(undoRedoState);

            var baseGraph = BaseGraph.fromJSON(undoRedoJSON.baseGraph);

            var order = Ordering.fromJSON(undoRedoJSON.order);

            if (!this._initializeFromBaseGraphAndLayout(baseGraph,
                                                        undoRedoJSON.probandNodeId,
                                                        { "ranks": undoRedoJSON.ranks,
                                                          "order": order,
                                                          "positions": undoRedoJSON.positions }) ) {
                return null;  // unable to genersate pedigree using import data, no import => no changes
            }

            var newNodes = this._getAllNodes();

            timer.printSinceLast("=== FromUndoRedo runtime: ");

            return {"new": newNodes, "removed": removedNodes};
        },

        //=============================================================

        toJSONObject: function(options)
        {
            var timer = new Helpers.Timer();

            var output = {
                           "JSON_version": PedigreeImport.getCurrentFullJSONVersion(),
                           "members": [],
                           "relationships": [],
                           "layout": {
                               "members": {},
                               "relationships": {},
                               "longedges": {}
                           }
                         };

            if (this.getProbandId() != null) {
                output["proband"] = this.getProbandId();
            }

            for (var i = 0 ; i <= this.DG.GG.getMaxRealVertexId(); i++) {
                if (this.isPerson(i) || this.isPlaceholder(i)) {
                    var memberData = { "id": i };
                    var pedigreeSpecificProperties = this.getNodePropertiesNotStoredInPatientProfile(i);
                    if (!Helpers.isObjectEmpty(pedigreeSpecificProperties)) {
                        memberData["pedigreeProperties"] = pedigreeSpecificProperties;
                    }
                    var phenotipsJSON = this.getPatientPhenotipsJSON(i);
                    if (!Helpers.isObjectEmpty(phenotipsJSON)) {
                        memberData["properties"] = phenotipsJSON;
                    }
                    if (options) {
                        if (options.hasOwnProperty("includePatientLinks") && !options.includePatientLinks) {
                            delete memberData["properties"].id;
                        }
                        if (options.hasOwnProperty("PII") && (options.PII == "nopersonal" || options.PII == "minimal")) {
                            delete memberData["properties"].patient_name;
                            delete memberData["properties"].date_of_birth;
                            delete memberData["properties"].date_of_death;
                            memberData.hasOwnProperty("pedigreeProperties") && delete memberData["pedigreeProperties"].lNameAtB;
                        }
                        if (options.hasOwnProperty("PII") && options.PII == "minimal") {
                            memberData.hasOwnProperty("pedigreeProperties") && delete memberData["pedigreeProperties"].comments;
                        }
                    }
                    output.members.push(memberData);
                    output.layout.members[i] = { "generation": this.DG.ranks[i],
                                                 "order": this.DG.order.vOrder[i],
                                                 "x": this.DG.positions[i] };
                } else if (this.isRelationship(i)) {
                    var childrenList = this.getAllChildren(i);
                    var children = [];
                    for (var j = 0; j < childrenList.length; j++) {
                        var childId = childrenList[j];
                        var child = {"id": childId };
                        var adopted = this.isAdoptedIn(childId) ? "in" : (this.isAdoptedOut(childId) ? "out" : null);
                        if (adopted != null) {
                            child["adopted"] = adopted;
                        }
                        children.push(child);
                    }
                    var relationshipJSON = { "id": i,
                                             "members": this.DG.GG.getParents(i),
                                             "children": children };

                    var propertiesJSON = this.getRelationshipExternalJSON(i);
                    if (!Helpers.isObjectEmpty(propertiesJSON)) {
                        relationshipJSON["properties"] = propertiesJSON;
                    }

                    output.relationships.push(relationshipJSON);
                    output.layout.relationships[i] = { "order": this.DG.order.vOrder[i],
                                                       "x": this.DG.positions[i] };

                    // check for multi-generation edges and add the edge positionig data to the output
                    var pathToParents = this.getPathToParents(i);
                    for (var p = 0; p < pathToParents.length; p++) {
                        if (pathToParents[p].path.length > 0) {
                            // it is not a direct connection between parent and relationship
                            //  => have a multi-generation (long) edge
                            var parent = pathToParents[p].parent;
                            var path   = pathToParents[p].path;

                            output.layout.longedges[i] = { "member": parent, "path": [] };

                            // note: path is given in the is bottom-to-top order, but need top-to-bottom for output
                            for (var v = path.length-1; v >= 0; v--) {
                                var virtualVertexID = path[v];
                                output.layout.longedges[i].path.push( { "order": this.DG.order.vOrder[virtualVertexID],
                                                                        "x": this.DG.positions[virtualVertexID] } );
                            }
                        }
                    }
                }
            }

            // add unlinked members
            var nextFreeID = this.DG.GG.getMaxRealVertexId() + 1;
            for (var i = 0; i < this._unlinkedMembers.length; i++) {
                var ptID = this._unlinkedMembers[i];
                var id = nextFreeID++;
                var patientJSON = Helpers.cloneObject(editor.getPatientRecordData().get(ptID));
                if (options && options.hasOwnProperty("includePatientLinks") && !options.includePatientLinks) {
                    delete patientJSON.id;
                }
                output.members.push( { "id": id,
                                       "notInPedigree": true,
                                       "properties": patientJSON } );
            }

            // note: everything else can be recomputed based on the information above

            this._debug_printJSONSummary(output, false, true);

            timer.printSinceLast("=== to JSON runtime: ");
            return output;
        },

        _debug_printJSONSummary: function(jsonOutput, printFullProperties, includeInternalData)
        {
            var timer = new Helpers.Timer();

            var debugOutput = JSON.parse(JSON.stringify(jsonOutput));

            if (!printFullProperties) {
                for (var i = 0; i < debugOutput.members.length; i++) {
                    var useProperties = { "other": "..." };
                    // only keep essential properties
                    if (debugOutput.members[i].properties) {
                        if (debugOutput.members[i].properties.id) {
                            useProperties.id = debugOutput.members[i].properties.id;
                        }
                        if (debugOutput.members[i].properties.sex) {
                            useProperties.sex = debugOutput.members[i].properties.sex;
                        }
                    }
                    debugOutput.members[i].properties = useProperties;
                }
            }

            console.log("JSON represenation: " + JSON.stringify(debugOutput));

            if (includeInternalData) {
                console.log("Ranks: [" + this.DG.ranks + "], Orders: ["
                        + Helpers.stringifyObject(this.DG.order) + "], Positions: [" + this.DG.positions + "]");
            }

            timer.printSinceLast("=== Debug output runtime: ");
        },

        //=============================================================

        fromImport: function (importString, importType, importOptions)
        {
            var timer = new Helpers.Timer();

            var removedNodes = this._getAllNodes();

            var importData = null;

            if (importType == "ped") {
                importData = PedigreeImport.initFromPED(importString, importOptions.acceptUnknownPhenotypes, importOptions.markEvaluated, importOptions.externalIdMark);
            } else if (importType == "BOADICEA") {
                importData = PedigreeImport.initFromBOADICEA(importString, importOptions.externalIdMark);
            } else if (importType == "gedcom") {
                importData = PedigreeImport.initFromGEDCOM(importString, importOptions.markEvaluated, importOptions.externalIdMark);
            } else if (importType == "simpleJSON") {
                importData = PedigreeImport.initFromSimpleJSON(importString);
            } else if (importType == "phenotipsJSON") {
                // as stored in PhenoTips and used in Phenotips REST; also used for saving internal pedigree states
                importData = PedigreeImport.initFromFullJSON(importString);
            }

            if (importData == null
                || !importData.hasOwnProperty("baseGraph")
                || !importData.hasOwnProperty("probandNodeID")) {
                return null;  // incorrect input data, no import => no changes
            }

            if (!importData.unlinkedMembers) {
                importData.unlinkedMembers = {};
            }

            // TODO: for now, we do not allow linking patients via import dialogue, so remove
            //       all links to all patients records that are not already in the family
            if (importOptions && importOptions.doNotLinkPatients) {
                // remove all unlinked patients loaded form import data that are not already in family:
                for (var patientRecordId in importData.unlinkedMembers) {
                    if (importData.unlinkedMembers.hasOwnProperty(patientRecordId)) {
                        if (!editor.isFamilyMember(patientRecordId)) {
                            console.log("Ignoring a link to patient record " + patientRecordId);
                            delete importData.unlinkedMembers[patientRecordId];
                        }
                    }
                }
                // remove all linked patients loaded form import data that are not already in family:
                var personIDs = importData.baseGraph.getAllPersons(true);
                for (var p = 0; p < personIDs.length; p++) {
                    var patientRecordId = importData.baseGraph.properties[personIDs[p]].phenotipsId;
                    if (patientRecordId && !editor.isFamilyMember(patientRecordId)) {
                        console.log("Ignoring a link to patient record " + patientRecordId);
                        delete importData.baseGraph.rawJSONProperties[personIDs[p]].id;
                        delete importData.baseGraph.properties[personIDs[p]].phenotipsId;
                    }
                }
            }

            if (!this._initializeFromBaseGraphAndLayout( importData.baseGraph,
                                                         importData.probandNodeID,
                                                         importData.layout,
                                                         importData.unlinkedMembers)) {
                return null;  // unable to generate pedigree using import data, no import => no changes
            }

            var newNodes = this._getAllNodes();

            timer.printSinceLast("=== Import runtime: ");

            return {"new": newNodes, "removed": removedNodes, "unlinked": this._unlinkedMembers};
        },

        // moves the given node to the left or right within the same rank
        performNodeOrderChange: function(nodeID, moveAmount)
        {
            var rank = this.DG.ranks[nodeID];
            var updatedOrder = this.DG.order.copy();

            // move node 1 step at a time, as it simplifies the update logic and simplifies
            // handling of special cases (e.g. change order of partners, change position of attached reltionship(s), etc.)
            for (var i = 0; i < Math.abs(moveAmount); i++) {
                updatedOrder = this._moveNodePastOnePersonWithinRank(updatedOrder, rank, nodeID, (moveAmount > 0) ? +1 : -1);
                if (updatedOrder == null) {
                    return null;
                }
            }

            var suggestedLayout = { "ranks": this.DG.ranks.slice(),
                                    "order": updatedOrder
                                  };

            if (this._initializeFromBaseGraphAndLayout(this.DG.GG, this.getProbandId(), suggestedLayout, null)) {
                return {"moved": this._getAllNodes()};
            }

            return null;
        },

        _initializeFromBaseGraphAndLayout: function (baseGraph, probandNodeID, suggestedLayout, unlinkedMembersData)
        {
            try {
                var newDG = new PositionedGraph( baseGraph,
                                                 probandNodeID,
                                                 this.DG.options,             // preserve whatever options are currently used
                                                 suggestedLayout );
                this.DG = newDG;

                if (unlinkedMembersData) {
                    this.setUnlinkedPatients(Object.keys(unlinkedMembersData));

                    // for every patient record present in the family (either linked or unlinked)
                    // save/update the patient PhenoTips JSON as loaded from the pedigree: normally all patient record
                    // JSONs will be re-loaded separately from the back-end, but current user may not have
                    // view rights for some of them (however should still be able to view and edit the pedigree)
                    this._updatedPatientJSONsAsLoadedFromPedigree(baseGraph, unlinkedMembersData)
                } // else: keep unlinked set "as is"
            } catch (e) {
                console.log("ERROR creating a pedigree from input data: " + e);
                return false;
            }

            return true;
        },

        _updatedPatientJSONsAsLoadedFromPedigree: function(baseGraph, unlinkedMembersData)
        {
            // linked patient records:
            var personIDs = baseGraph.getAllPersons(true);
            for (var p = 0; p < personIDs.length; p++) {
                var linkedPatientRecordID = baseGraph.properties[personIDs[p]].phenotipsId;
                if (linkedPatientRecordID) {
                    var patientJSONInPedigree = baseGraph.rawJSONProperties[personIDs[p]];
                    editor.getPatientRecordData().updateFromPedigree(linkedPatientRecordID, patientJSONInPedigree);
                }
            }

            // unlinked patient records:
            for (var patientRecordID in unlinkedMembersData) {
                if (unlinkedMembersData.hasOwnProperty(patientRecordID)) {
                    var patientJSONInPedigree = unlinkedMembersData[patientRecordID];
                    editor.getPatientRecordData().updateFromPedigree(patientRecordID, patientJSONInPedigree);
                }
            }
        },

        //=============================================================

        // useOrder: use this order data instead of this.DG.order
        // direction: +1 (right) or -1 (left)
        _findOrderOfNeighbourPerson: function(useOrder, rank, fromOrder, direction) {
            for (var i = fromOrder + direction; i >= 0 && i < useOrder.order[rank].length; i = i + direction) {
                if (this.isPerson(useOrder.order[rank][i])) {
                    return i;
                }
            }
            return null;
        },

        // moveDirection: +1 (right) or -1 (left)
        _moveNodePastOnePersonWithinRank: function (currentOrder, rank, nodeID, moveDirection)
        {
            var currentNodeOrder = currentOrder.vOrder[nodeID];
            var nextPersonOrder = this._findOrderOfNeighbourPerson(currentOrder, rank, currentNodeOrder, moveDirection);
            if (nextPersonOrder === null) {
                return null;
            }
            var nextPersonID = currentOrder.order[rank][nextPersonOrder];

            // for now handle two cases:
            // 1) nearest person node in the direction of movement is a partner: swap order of the partners (keep relationship inbetween)
            // 2) not a partner: move nodeID, shifting all orders in between by 1
            if (Helpers.arrayContains(this.DG.GG.getAllPartners(nodeID), nextPersonID)) {
                // switch partners: [a]-*---- ? ? ? ----[b] -> [b]-*---- ? ? ? ----[a]
                currentOrder.exchange(rank, currentNodeOrder, nextPersonOrder);
            } else {
                // move node: [a] ? ? ? [b] -> [b] [a] ? ? ?
                //
                // a few special cases need to be considered to keep the layout nice:
                //
                //  special case1: when there is nextPerson's relationship next to nextPerson in the direction of movement: ? ? *-[a] [b]
                //                 => need to move past the relationship as well, e.g. instead of <<? ? *- [b] [a]>> do <<? ? [b] *-[a]>>
                //
                //  special case2a: when node being moved has one mor more relationship node(s) next to it (in the direction of movement)
                //                  but not next to the other partner (which we know is the case here - that case is handled above by an exchange)
                //                  => need to move it together with the node, e.g.:
                //                  [b_partner] ? ? ? [a] *-[b]    -> [b_partner] ? ? ? *-[b] [a] instead of [b_partner] ? ? ? [b] [a] -*
                //
                //  possible case2b: when node being moved has a relationship node next to it (in the direction opposite of movement)
                //                   but not next to the other partner => may want to move it together with the node, e.g.:
                //                   [a] [b]-* ? ? ? [b_partner]    -> [b]-* [a] ? ? ? [b_partner] instead of [b] [a] -*- ? ? ? [b_partner]
                //                   BUT: it may be ok to leave relationship "as is" in this case, as it minimizes overall node movement

                // check case 1:
                // TODO: decide whats the best thing to do and implement

                // check case 2a: check all node between "currentNode" and "nextPerson", move all relationships which
                // are atached to "currentNode" past "nextPerson", so that both "curentNode" and all its relationships
                // appear together on the other side of "nextPerson"
                var currentNodeRelationships = this.DG.GG.getAllRelationships(nodeID);
                var numRelationshipsMoved = 0;
                for (var inbetweenOrder = currentNodeOrder + moveDirection; inbetweenOrder != nextPersonOrder; inbetweenOrder += moveDirection) {
                    var nextNode = currentOrder.order[rank][inbetweenOrder];
                    if (Helpers.arrayContains(currentNodeRelationships, nextNode)) {
                        currentOrder.move(rank, inbetweenOrder, (nextPersonOrder - inbetweenOrder + moveDirection*numRelationshipsMoved));
                        // nextPerson has moved!
                        nextPersonOrder = currentOrder.vOrder[nextPersonID];
                        // re-start search from the node which is next to currentNode after te modification
                        inbetweenOrder = currentNodeOrder;
                        numRelationshipsMoved++;
                    }
                }

                // check case 2b:
                // -> not implemented on purpose (TODO: discuss)

                currentOrder.move(rank, currentNodeOrder, (nextPersonOrder - currentNodeOrder));
            }

            return currentOrder;
        },

        _insertVertex: function (type, properties, edgeWeights, inedge, outedge, insertRank, insertOrder)
        {
            // all nodes are connected to some other node, so either inedge or outedge should be given
            if (inedge === null && outedge === null)
                throw "Assertion failed: each node should be connected to at least one other node";
            if (inedge !== null && outedge !== null)
                throw "Assertion failed: not clear which edge crossing to optimize, can only insert one edge";

            var inedges  = (inedge  !== null) ? [inedge]  : [];
            var outedges = (outedge !== null) ? [outedge] : [];

            var newNodeId = this.DG.GG.insertVertex(type, properties, edgeWeights, inedges, outedges);

            // note: the graph may be inconsistent at this point, e.g. there may be childhubs with
            // no relationships or relationships without any people attached

            if (insertRank == 0) {
                for (var i = 0; i < this.DG.ranks.length; i++)
                    this.DG.ranks[i]++;
                this.DG.maxRank++;

                this.DG.order.insertRank(1);

                insertRank = 1;
            }
            else if (insertRank > this.DG.maxRank) {
                this.DG.maxRank = insertRank;
                this.DG.order.insertRank(insertRank);
            }

            this.DG.ranks.splice(newNodeId, 0, insertRank);

            this.DG.order.insertAndShiftAllIdsAboveVByOne(newNodeId, insertRank, insertOrder);

            // update positions
            this.DG.positions.splice( newNodeId, 0, -Infinity );  // temporary position: will move to the correct location and shift other nodes below

            var nodeToKeepEdgeStraightTo = (inedge != null) ? inedge : outedge;
            this.DG.getHeuristics().moveToCorrectPositionAndMoveOtherNodesAsNecessary( newNodeId, nodeToKeepEdgeStraightTo );

            return newNodeId;
        },

        _getAllNodes: function (minID, maxID)
        {
            var nodes = [];
            var minID = minID ? minID : 0;
            var maxID = maxID ? Math.min( maxID, this.DG.GG.getMaxRealVertexId()) : this.DG.GG.getMaxRealVertexId();
            for (var i = minID; i <= maxID; i++) {
                if ( this.DG.GG.type[i] == BaseGraph.TYPE.PERSON || this.DG.GG.type[i] == BaseGraph.TYPE.RELATIONSHIP )
                    nodes.push(i);
            }
            return nodes;
        },

        _findMovedNodes: function (maxOldID, positionsBefore, ranksBefore, vertLevelsBefore, rankYBefore, consangrBefore, fastCheck)
        {
            //console.log("Before: " + Helpers.stringifyObject(vertLevelsBefore));
            //console.log("After:  " + Helpers.stringifyObject(this.DG.vertLevel));
            //console.log("Before: " + Helpers.stringifyObject(positionsBefore));
            //console.log("After: " + Helpers.stringifyObject(this.DG.positions));

            // TODO: some heuristics cause this behaviour. Easy to fix by normalization, but better look into root cause later
            // normalize positions: if the leftmost coordinate is now greater than it was before
            // make the old leftmost node keep it's coordinate
            var oldMin = Math.min.apply( Math, positionsBefore );
            var newMin = Math.min.apply( Math, this.DG.positions );
            if (newMin > oldMin) {
                var oldMinNodeID = Helpers.arrayIndexOf(positionsBefore, oldMin);
                if (oldMinNodeID > maxOldID) {
                    // minNodeID is a virtual edge, its ID may have increased due to new real node insertions
                    oldMinNodeID += (this.DG.GG.getMaxRealVertexId() - maxOldID);
                }
                var newMinValue  = this.DG.positions[oldMinNodeID];
                var shiftAmount  = newMinValue - oldMin;

                for (var i = 0; i < this.DG.positions.length; i++)
                    this.DG.positions[i] -= shiftAmount;
            }


            var result = {};
            for (var i = 0; i <= maxOldID; i++) {
                // this node was moved
                if (this.DG.GG.type[i] == BaseGraph.TYPE.RELATIONSHIP || this.DG.GG.type[i] == BaseGraph.TYPE.PERSON)
                {
                    var rank = this.DG.ranks[i];
                    //if (rank != ranksBefore[i]) {
                    //    this._addNodeAndAssociatedRelationships(i, result, maxOldID);
                    //    continue;
                    //}
                    if (rankYBefore && this.DG.rankY[rank] != rankYBefore[ranksBefore[i]]) {
                        this._addNodeAndAssociatedRelationships(i, result, maxOldID);
                        continue;
                    }
                    if (this.DG.positions[i] != positionsBefore[i]) {
                        this._addNodeAndAssociatedRelationships(i, result, maxOldID);
                        continue;
                    }
                    // or it is a relationship with a long edge - redraw just in case since long edges may have complicated curves around other nodes
                    if (this.DG.GG.type[i] == BaseGraph.TYPE.RELATIONSHIP) {
                        if (consangrBefore && !consangrBefore.hasOwnProperty(i) && this.DG.consangr.hasOwnProperty(i)) {
                            result[i] = true;
                            continue;
                        }
                        if (!fastCheck) {
                            var inEdges = this.DG.GG.getInEdges(i);
                            if (inEdges[0] > this.DG.GG.maxRealVertexId || inEdges[1] > this.DG.GG.maxRealVertexId) {
                                result[i] = true;
                                continue;
                            }
                        }
                        // check vertical positioning changes
                        var parents = this.DG.GG.getParents(i);
                        if (vertLevelsBefore.outEdgeVerticalLevel[parents[0]] !== undefined &&    // vertical levels may be outdated if multiple nodes were created in one batch
                            vertLevelsBefore.outEdgeVerticalLevel[parents[1]] !== undefined) {
                            if (vertLevelsBefore.outEdgeVerticalLevel[parents[0]][i].verticalLevel !=  this.DG.vertLevel.outEdgeVerticalLevel[parents[0]][i].verticalLevel ||
                                vertLevelsBefore.outEdgeVerticalLevel[parents[1]][i].verticalLevel !=  this.DG.vertLevel.outEdgeVerticalLevel[parents[1]][i].verticalLevel)
                            {
                                result[i] = true;
                                continue;
                            }
                        }

                        var childHub = this.DG.GG.getRelationshipChildhub(i);
                        if (vertLevelsBefore.childEdgeLevel[childHub] !== undefined && vertLevelsBefore.childEdgeLevel[childHub] != this.DG.vertLevel.childEdgeLevel[childHub]) {
                            result[i] = true;
                            continue;
                        }
                    }
                }
            }

            // check virtual edges: even if relationshipo node is not moved if the rank on which virtual edges reside moves
            // the relationship should redraw the virtual edges
            for (var i = this.DG.GG.getMaxRealVertexId() + 1; i <= this.DG.GG.getNumVertices(); i++) {
                var rank    = this.DG.ranks[i];
                if (rankYBefore && rankYBefore.length >= rank && this.DG.rankY[rank] != rankYBefore[rank]) {
                    var relationship = this.DG.GG.downTheChainUntilNonVirtual(i);
                    if (relationship <= maxOldID) {
                        result[relationship] = true;
                    }
                }
            }

            var resultArray = [];
            for (var node in result) {
                if (result.hasOwnProperty(node)) {
                    resultArray.push(parseInt(node));
                }
            }

            return resultArray;
        },

        _addNodeAndAssociatedRelationships: function ( node, addToSet, maxOldID )
        {
            addToSet[node] = true;
            if (this.DG.GG.type[node] != BaseGraph.TYPE.PERSON) return;

            var inEdges = this.DG.GG.getInEdges(node);
            if (inEdges.length > 0) {
                var parentChildhub     = inEdges[0];
                var parentRelationship = this.DG.GG.getInEdges(parentChildhub)[0];
                if (parentRelationship <= maxOldID)
                    addToSet[parentRelationship] = true;
            }

            var outEdges = this.DG.GG.getOutEdges(node);
            for (var i = 0; i < outEdges.length; i++) {
                if (outEdges[i] <= maxOldID)
                    addToSet[ outEdges[i] ] = true;
            }
        },

        //=============================================================

        _addMultiRankEdge: function ( personId, relationshipId, _weight )
        {
            var weight = _weight ? _weight : 1.0;

            var rankPerson       = this.DG.ranks[personId];
            var rankRelationship = this.DG.ranks[relationshipId];

            if (rankPerson > rankRelationship - 2)
                throw "Assertion failed: attempt to make a multi-rank edge between non-multirank ranks";

            var otherpartner   = this.DG.GG.getInEdges(relationshipId)[0];

            var order_person   = this.DG.order.vOrder[personId];
            var order_rel      = this.DG.order.vOrder[relationshipId];

            var x_person       = this.DG.positions[otherpartner];
            var x_relationship = this.DG.positions[relationshipId];

            var prevPieceOrder = (x_person < x_relationship) ? (order_rel+1) : order_rel;
            var prevPieceId    = this._insertVertex(BaseGraph.TYPE.VIRTUALEDGE, {}, weight, null, relationshipId, rankRelationship, prevPieceOrder);

            // TODO: an algorithm which optimizes the entire edge placement globally (not one piece at a time)

            var rankNext = rankRelationship;
            while (--rankNext > rankPerson) {

                var prevNodeX = this.DG.positions[prevPieceId];
                var orderToMakeEdgeStraight = this.DG.order.order[rankNext].length;
                for (var o = 0; o < this.DG.order.order[rankNext].length; o++)
                    if (this.DG.positions[this.DG.order.order[rankNext][o]] >= prevNodeX) {
                        orderToMakeEdgeStraight = o;
                        break;
                    }

                console.log("adding piece @ rank: " + rankNext + " @ order " + orderToMakeEdgeStraight);

                prevPieceId = this._insertVertex(BaseGraph.TYPE.VIRTUALEDGE, {}, weight, null, prevPieceId, rankNext, orderToMakeEdgeStraight);
            }

            //connect last piece with personId
            this.DG.GG.addEdge(personId, prevPieceId, weight);
        },


        //=============================================================

        _findBestInsertPosition: function ( rank, edgeToV, preferLeft, _fromOrder, _toOrder )
        {
            // note: does not assert that the graph satisfies all the assumptions in BaseGraph.validate()

            if (rank == 0 || rank > this.DG.maxRank)
                return 0;

            // find the order on rank 'rank' to insert a new vertex so that the edge connecting this new vertex
            // and vertex 'edgeToV' crosses the smallest number of edges.
            var edgeToRank      = this.DG.ranks[ edgeToV ];
            var edgeToOrder     = this.DG.order.vOrder[edgeToV];

            if (edgeToRank == rank && this.isPerson(edgeToV))
                return this._findBestRelationshipPosition( edgeToV, preferLeft );

            var bestInsertOrder  = 0;
            var bestCrossings    = Infinity;
            var bestDistance     = Infinity;

            var crossingChildhubEdgesPenalty = false;
            if (this.DG.GG.type[edgeToV] == BaseGraph.TYPE.CHILDHUB)
                crossingChildhubEdgesPenalty = true;

            var desiredOrder = 0;

            var edgeToX = this.DG.positions[edgeToV];
            for (var o = 0; o < this.DG.order.order[rank].length; o++) {
                var uAtPos = this.DG.order.order[rank][o];
                var uX     = this.DG.positions[uAtPos];
                if (uX < edgeToX) {
                    desiredOrder = o+1;
                }
                else {
                    break;
                }
            }

            // when inserting children below childhubs: next to other children
            if (this.DG.GG.type[edgeToV] == BaseGraph.TYPE.CHILDHUB && rank > edgeToRank && this.DG.GG.getOutEdges(edgeToV).length > 0)
                desiredOrder = this._findRightmostChildPosition(edgeToV) + 1;

            var fromOrder = _fromOrder ? Math.max(_fromOrder,0) : 0;
            var toOrder   = _toOrder   ? Math.min(_toOrder,this.DG.order.order[rank].length) : this.DG.order.order[rank].length;
            for (var o = fromOrder; o <= toOrder; o++) {

                // make sure not inserting inbetween some twins
                if (o > 0 && o < this.DG.order.order[rank].length) {
                    // skip virtual edges which may appear between twins
                    var leftNodePos = o-1;
                    while (leftNodePos > 0 && this.DG.GG.isVirtual(this.DG.order.order[rank][leftNodePos]))
                        leftNodePos--;
                    rightNodePos = o;
                    while (rightNodePos < this.DG.order.order[rank].length-1 && this.DG.GG.isVirtual(this.DG.order.order[rank][rightNodePos]))
                        rightNodePos--;
                    var nodeToTheLeft  = this.DG.order.order[rank][leftNodePos];
                    var nodeToTheRight = this.DG.order.order[rank][rightNodePos];

                    if (this.isPerson(nodeToTheLeft) && this.isPerson(nodeToTheRight)) {
                        var rel1 = this.DG.GG.getProducingRelationship(nodeToTheLeft);
                        var rel2 = this.DG.GG.getProducingRelationship(nodeToTheRight);
                        if (rel1 == rel2) {
                            var twinGroupId1 = this.DG.GG.getTwinGroupId(nodeToTheLeft);
                            var twinGroupId2 = this.DG.GG.getTwinGroupId(nodeToTheRight);
                            if (twinGroupId1 !== null && twinGroupId1 == twinGroupId2)
                                continue;
                        }
                    }
                }

                var numCrossings = this._edgeCrossingsByFutureEdge( rank, o - 0.5, edgeToRank, edgeToOrder, crossingChildhubEdgesPenalty, edgeToV );

                //console.log("position: " + o + ", numCross: " + numCrossings);

                if ( numCrossings < bestCrossings ||                           // less crossings
                     (numCrossings == bestCrossings && Math.abs(o - desiredOrder) <= bestDistance )   // closer to desired position
                   ) {
                   bestInsertOrder = o;
                   bestCrossings   = numCrossings;
                   bestDistance    = Math.abs(o - desiredOrder);
                }
            }

            //console.log("inserting @ rank " + rank + " with edge from " + edgeToV + " --> " + bestInsertOrder);
            return bestInsertOrder;
        },

        _findRightmostChildPosition: function ( vertex )
        {
            var childrenInfo = this.DG.getHeuristics().analizeChildren(vertex);
            return childrenInfo.rightMostChildOrder;
        },

        _edgeCrossingsByFutureEdge: function ( newVRank, newVOrder, existingURank, existingUOrder, crossingChildhubEdgesPenalty, existingU )
        {
            // Note: newVOrder is expected to be a number between two existing orders, or higher than all, or lower than all

            // counts how many existing edges a new edge from given rank&order to given rank&order would cross
            // if order is an integer, it is assumed it goes from an existing vertex
            // if order is inbetween two integers, it is assumed it is the position used for a new-to-be-inserted vertex

            // for simplicity (to know if we need to check outEdges or inEdges) get the edge in the correct direction
            // (i.e. from lower ranks to higher ranks)
            var rankFrom  = Math.min( newVRank, existingURank );
            var rankTo    = Math.max( newVRank, existingURank );
            var orderFrom = (newVRank < existingURank) ? newVOrder : existingUOrder;
            var orderTo   = (newVRank < existingURank) ? existingUOrder : newVOrder;

            // for better penalty computation handle the special case of adding a new child to an existing childhub
            var vSibglingInfo = undefined;
            if (this.DG.GG.isChildhub(existingU) && (newVRank > existingURank) &&
                this.DG.GG.getOutEdges(existingU).length > 0) {
                vSibglingInfo = this.DG.getHeuristics().analizeChildren(existingU);

                if (vSibglingInfo.numWithTwoPartners < vSibglingInfo.orderedChildren.length) {
                    // need to insert new node next to a sibling
                    var okPosition = false;
                    if (newVOrder > 0) {                                         // check left neighbour
                        var leftNeighbour = this.DG.order.order[newVRank][ Math.floor(newVOrder)];
                        var neighbourInEdges = this.DG.GG.getInEdges(leftNeighbour);
                        if (neighbourInEdges.length == 1 && neighbourInEdges[0] == existingU) {
                            okPosition = true;  // left neighbour is a sibling
                        }
                    }
                    if (newVOrder < this.DG.order.order[newVRank].length - 1) {  // check right neighbour
                        var rightNeighbour = this.DG.order.order[newVRank][ Math.ceil(newVOrder)];
                        var neighbourInEdges = this.DG.GG.getInEdges(rightNeighbour);
                        if (neighbourInEdges.length == 1 && neighbourInEdges[0] == existingU) {
                            okPosition = true;  // right neighbour is a sibling
                        }
                    }
                    if (!okPosition) {
                        return Infinity;
                    }
                }
            }

            var crossings = 0;

            if (rankFrom == rankTo) throw "TODO: probably not needed";

            // For multi-rank edges, crossing occurs if either
            // 1) there is an edge going from rank[v]-ranked vertex with a smaller order
            //     than v to a rank[targetV]-ranked vertex with a larger order than targetV
            // 2) there is an edge going from rank[v]-ranked vertex with a larger order
            //     than v to a rank[targetV]-ranked vertex with a smaller order than targetV

            var verticesAtRankTo = this.DG.order.order[ rankTo ];

            for (var ord = 0; ord < verticesAtRankTo.length; ord++) {
                if ( ord == orderTo ) continue;

                var vertex = verticesAtRankTo[ord];

                var inEdges = this.DG.GG.getInEdges(vertex);
                var len     = inEdges.length;

                for (var j = 0; j < len; j++) {
                    var target = inEdges[j];

                    var penalty = 1;
                    if (crossingChildhubEdgesPenalty && this.DG.GG.isChildhub(target)) {
                        // don't want to insert a node inbetween siblings
                        penalty = 100000;
                        // ...unless siblings of the inserted node are already inbetween those siblings:
                        if (vSibglingInfo) {
                            var targetChildren = this.DG.getHeuristics().analizeChildren(target);

                            if (targetChildren.leftMostChildOrder < vSibglingInfo.rightMostChildOrder &&
                                targetChildren.rightMostChildOrder > vSibglingInfo.leftMostChildOrder) {
                                penalty = 1;
                            }
                        }
                    }

                    var orderTarget = this.DG.order.vOrder[target];
                    var rankTarget  = this.DG.ranks[target];

                    if (rankTarget == rankTo)
                    {
                        if ( ord < orderTo && orderTarget > orderTo ||
                             ord > orderTo && orderTarget < orderTo )
                             crossings += 2;
                    }
                    else
                    {
                        if (ord < orderTo && orderTarget > orderFrom ||
                            ord > orderTo && orderTarget < orderFrom )
                            crossings += penalty;
                    }
                }
            }

            // try not to insert between a node and it's relationship
            // (for that only need check edges on the insertion rank)
            var verticesAtNewRank = this.DG.order.order[ newVRank ];
            for (var ord = 0; ord < verticesAtNewRank.length; ord++) {
                if ( ord == newVOrder ) continue;

                var vertex = verticesAtNewRank[ord];

                var outEdges = this.DG.GG.getOutEdges(vertex);
                var len      = outEdges.length;

                for (var j = 0; j < len; j++) {
                    var target = outEdges[j];

                    var orderTarget = this.DG.order.vOrder[target];
                    var rankTarget  = this.DG.ranks[target];

                    if (rankTarget == newVRank)
                    {
                        if ( newVOrder < ord && newVOrder > orderTarget ||
                             newVOrder > ord && newVOrder < orderTarget )
                             crossings += 0.1;
                    }
                }
            }


            return crossings;
        },

        _findBestRelationshipPosition: function ( v, preferLeft, u )
        {
            // Handles two different cases:
            // 1) both partners are given ("v" and "u"). Then need to insert between v and u
            // 2) only one partner is given ("v"). Then given the choice prefer the left side if "preferleft" is true

            var rank   = this.DG.ranks[v];
            var orderR = this.DG.order.order[rank];
            var isTwin = (this.DG.GG.getTwinGroupId(v) != null);
            var vOrder = this.DG.order.vOrder[v];

            var penaltyBelow    = [];
            var penaltySameRank = [];
            for (var o = 0; o <= orderR.length; o++) {
                penaltyBelow[o]    = 0;
                penaltySameRank[o] = 0;
            }

            // for each order on "rank" compute heuristic penalty for inserting a node before that order
            // based on the structure of nodes below
            for (var o = 0; o < orderR.length; o++) {
                var node = orderR[o];
                if (!this.isRelationship(node)) continue;
                var childrenInfo = this.DG.getHeuristics().analizeChildren(node);

                // TODO: do a complete analysis without any heuristics
                if (childrenInfo.leftMostHasLParner)  { penaltyBelow[o]   += 1; penaltyBelow[o-1] += 0.25; }   // 0.25 is just a heuristic estimation of how busy the level below is.
                if (childrenInfo.rightMostHasRParner) { penaltyBelow[o+1] += 1; penaltyBelow[o+2] += 0.25; }
            }

            // for each order on "rank" compute heuristic penalty for inserting a node before that order
            // based on the edges on that rank
            for (var o = 0; o < orderR.length; o++) {
                var node = orderR[o];
                if (!this.isRelationship(node)) continue;

                var relOrder = this.DG.order.vOrder[node];

                var parents = this.DG.GG.getInEdges(node);

                for (var p = 0; p < parents.length; p++) {
                    var parent = parents[p];
                    if (parent != v && this.DG.ranks[parent] == rank && parent != u) {
                        var parentOrder = this.DG.order.vOrder[parent];

                        var from = (parentOrder > relOrder) ? relOrder + 1 : parentOrder + 1;
                        var to   = (parentOrder > relOrder) ? parentOrder : relOrder;
                        for (var j = from; j <= to; j++)
                            penaltySameRank[j] = Infinity;
                    }
                }
            }

            // add penalties for crossing child-to-parent lines, and forbid inserting inbetween twin nodes
            for (var o = 0; o < orderR.length; o++) {
                if (o == vOrder) continue;

                var node = orderR[o];
                if (!this.isPerson(node)) continue;
                var allTwins = this.getAllTwinsSortedByOrder(node);

                // forbid inserting inbetween twins
                if (allTwins.length > 1) {
                    var leftMostTwinOrder  = this.DG.order.vOrder[ allTwins[0] ];
                    var rightMostTwinOrder = this.DG.order.vOrder[ allTwins[allTwins.length-1] ];
                    for (var j = leftMostTwinOrder+1; j <= rightMostTwinOrder; j++)
                        penaltySameRank[j] = Infinity;
                    o = rightMostTwinOrder; // skip thorugh all other twins in this group
                }

                // penalty for crossing peron-to-parent line
                if (this.DG.GG.getProducingRelationship(node) != null) {
                    if (o < vOrder) {
                        for (var j = 0; j <= o; j++)
                            penaltySameRank[j]++;
                    }
                    else {
                        for (var j = o+1; j <= orderR.length; j++)
                            penaltySameRank[j]++;
                    }
                }
            }

            console.log("Insertion same rank penalties: " + Helpers.stringifyObject(penaltySameRank));
            console.log("Insertion below penalties:     " + Helpers.stringifyObject(penaltyBelow));

            if (u === undefined) {
                if (preferLeft && vOrder == 0) return 0;

                var partnerInfo = this.DG._findLeftAndRightPartners(v);
                var numLeftOf   = partnerInfo.leftPartners.length;
                var numRightOf  = partnerInfo.rightPartners.length;

                // Note: given everything else being equal, prefer the right side - to move fewer nodes

                console.log("v: " + v + ", vOrder: " + vOrder + ", numL: " + numLeftOf + ", numR: " + numRightOf);

                if (!isTwin && numLeftOf  == 0 && (preferLeft || numRightOf > 0) ) return vOrder;
                if (!isTwin && numRightOf == 0 )                                   return vOrder + 1;

                var bestPosition = vOrder + 1;
                var bestPenalty  = Infinity;
                for (var o = 0; o <= orderR.length; o++) {
                    var penalty = penaltyBelow[o] + penaltySameRank[o];
                    if (o <= vOrder) {
                        penalty += numLeftOf + (vOrder - o);        // o == order     => insert immediately to the left of, distance penalty = 0
                        if (preferLeft)
                            penalty -= 0.5;   // preferLeft => given equal penalty prefer left (0.5 is less than penalty diff due to other factors)
                        else
                            penalty += 0.5;   //
                    }
                    else {
                        penalty += numRightOf + (o - vOrder - 1);   // o == (order+1) => insert immediately to the right of, distance penalty = 0
                    }

                    //console.log("order: " + o + ", penalty: " + penalty);
                    if (penalty < bestPenalty) {
                        bestPenalty  = penalty;
                        bestPosition = o;
                    }
                }
                return bestPosition;
            }

            // for simplicity, lets make sure v is to the left of u
            if (this.DG.order.vOrder[v] > this.DG.order.vOrder[u]) {
                var tmp = u;
                u       = v;
                v       = tmp;
            }

            var orderV = this.DG.order.vOrder[v];
            var orderU = this.DG.order.vOrder[u];

            var partnerInfoV = this.DG._findLeftAndRightPartners(v);
            var numRightOf  = partnerInfoV.rightPartners.length;
            var partnerInfoU = this.DG._findLeftAndRightPartners(u);
            var numLeftOf   = partnerInfoU.leftPartners.length;

            if (numRightOf == 0 && numLeftOf > 0)  return orderV + 1;
            if (numRightOf > 0  && numLeftOf == 0) return orderU;

            var bestPosition = orderV + 1;
            var bestPenalty  = Infinity;
            for (var o = orderV+1; o <= orderU; o++) {
                var penalty = penaltyBelow[o] + penaltySameRank[o];

                for (var p = 0; p < partnerInfoV.rightPartners.length; p++) {
                    var partner = partnerInfoV.rightPartners[p];
                    if (o <= this.DG.order.vOrder[partner]) penalty++;
                }
                for (var p = 0; p < partnerInfoU.leftPartners.length; p++) {
                    var partner = partnerInfoU.leftPartners[p];
                    if (o > this.DG.order.vOrder[partner]) penalty++;
                }

                //console.log("order: " + o + ", penalty: " + penalty);

                if (penalty <= bestPenalty) {
                    bestPenalty  = penalty;
                    bestPosition = o;
                }
            }
            return bestPosition;
        },

        //=============================================================

        _getAllPersonsOfGenders: function (validGendersSet)
        {
            // all person nodes whose gender matches one of genders in the validGendersSet array

            if (validGendersSet) {
                // validate input genders
                for (var i = 0; i < validGendersSet.length; i++) {
                    validGendersSet[i] = validGendersSet[i].toLowerCase();
                    if (validGendersSet[i] != 'u' && validGendersSet[i] != 'm' && validGendersSet[i] != 'f' && validGendersSet[i] != 'o') {
                        console.log("ERROR: Invalid gender: " + validGendersSet[i]);
                        return [];
                    }
                }
            }

             var result = [];

             var persons = this.getAllPersonIDs(); // get all person nodes which are not placeholders

             for (var i = 0; i < persons.length; i++) {
                var nodeID = persons[i];
                if (this.isPersonGroup(nodeID)) continue;

                var filterPassed = true;
                if (validGendersSet) {
                    var gender = this.getProperties(nodeID)["gender"].toLowerCase();
                    if (!Helpers.arrayContains(validGendersSet, gender)) {
                        filterPassed = false;
                    }
                }

                if (filterPassed) {
                    result.push(nodeID);
                }
             }

             return result;
        }
    };

    return DynamicPositionedGraph;
});
