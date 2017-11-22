// DynamicPositionedGraph adds support for online modifications and provides a convenient API for UI implementations
define([
        "pedigree/pedigreeDate",
        "pedigree/model/baseGraph",
        "pedigree/model/positionedGraph",
        "pedigree/model/helpers",
        "pedigree/model/import"
    ], function(
        PedigreeDate,
        BaseGraph,
        PositionedGraph,
        Helpers,
        PedigreeImport
    ){
    DynamicPositionedGraph = function( drawGraph )
    {
        this.DG = drawGraph;

        this._heuristics = new Heuristics( drawGraph );  // heuristics & helper methods separated into a separate class

        this._heuristics.improvePositioning();

        this._onlyProbandGraph = [ { name :'proband' } ];
    };

    DynamicPositionedGraph.makeEmpty = function (layoutRelativePersonWidth, layoutRelativeOtherWidth)
    {
        var baseG       = new BaseGraph(layoutRelativePersonWidth, layoutRelativeOtherWidth);
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
            return this._getAllPersonsOfGenders(null);
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
                                   'deceasedAge', 'deceasedCause'];

            var result = {};
            for (var i = 0; i < keepProperties.length; i++) {
                if (allProperties.hasOwnProperty(keepProperties[i])) {
                    result[keepProperties[i]] = allProperties[keepProperties[i]];
                }
            }
            return result;
        },

        setNodeDataFromPhenotipsJSON: function( id, patientObject )
        {
            if (patientObject === null) {
                return true;
            }

            this.DG.GG.properties[id] = {"phenotipsId": patientObject.id };

            // pedigree-specific properties (added to patientObject by pedigree controller
            // to preserve them when a patient is assigned to an existing pedigree node)
            if (patientObject.hasOwnProperty("pedigreeProperties")) {
                for (var prop in patientObject.pedigreeProperties) {
                    if (patientObject.pedigreeProperties.hasOwnProperty(prop)) {
                        this.DG.GG.properties[id][prop] = patientObject.pedigreeProperties[prop];
                    }
                }
            }

            // Fields which are loaded from the patient document are:
            // - first_name
            // - last_name
            // - sex
            // - date_of_birth
            // - date_of_death
            // - life_status
            // - external_id
            // - features + nonstandard_features
            // - disorders
            // - genes
            // - maternal_ethnicity + paternal_ethnicity (merged with own ethnicities entered in pedigree editor)
            // - family_history

            if (patientObject.hasOwnProperty("patient_name")) {
                if (patientObject.patient_name.hasOwnProperty("first_name")) {
                    this.DG.GG.properties[id].fName = patientObject.patient_name.first_name;
                } else {
                    this.DG.GG.properties[id].fName = "";
                }
                if (patientObject.patient_name.hasOwnProperty("last_name")) {
                    this.DG.GG.properties[id].lName = patientObject.patient_name.last_name;
                } else {
                    this.DG.GG.properties[id].lName = "";
                }
            }

            if (patientObject.hasOwnProperty("sex")) {
                this.DG.GG.properties[id].gender = patientObject.sex;
            } else {
                this.DG.GG.properties[id].gender = "U";
            }

            if (patientObject.hasOwnProperty("date_of_birth")) {
                var birthDate = new PedigreeDate(patientObject.date_of_birth);
                this.DG.GG.properties[id].dob = birthDate.getSimpleObject();
            } else {
                delete this.DG.GG.properties[id].dob;
            }
            if (patientObject.hasOwnProperty("date_of_death")) {
                var deathDate = new PedigreeDate(patientObject.date_of_death);
                this.DG.GG.properties[id].dod = deathDate.getSimpleObject();
                if (deathDate.isSet()) {
                    delete this.DG.GG.properties[id].aliveandwell;
                }
            } else {
                delete this.DG.GG.properties[id].dod;
            }
            if (patientObject.hasOwnProperty("life_status")) {
                var lifeStatus = patientObject["life_status"];
                if (lifeStatus == "deceased" || lifeStatus == "alive") {
                    this.DG.GG.properties[id].lifeStatus = lifeStatus;
                }
                if (lifeStatus != "alive") {
                    // if not removed, it will overwrite the life status to 'alive' and thus remove death date
                    delete this.DG.GG.properties[id].aliveandwell;
                }
            } else {
                delete this.DG.GG.properties[id].lifeStatus;
            }

            if (patientObject.hasOwnProperty("ethnicity")) {
                // e.g.: "ethnicity":{"maternal_ethnicity":["Yugur"],"paternal_ethnicity":[]}
                var ethnicities = [];
                if (patientObject.ethnicity.hasOwnProperty("maternal_ethnicity")) {
                    ethnicities = patientObject.ethnicity.maternal_ethnicity.slice(0);
                }
                if (patientObject.ethnicity.hasOwnProperty("paternal_ethnicity")) {
                    ethnicities = ethnicities.concat(patientObject.ethnicity.paternal_ethnicity.slice(0));
                }
                if (ethnicities.length > 0) {
                    this.DG.GG.properties[id].ethnicities = Helpers.filterUnique(ethnicities);
                }
            }

            if (patientObject.hasOwnProperty("external_id")) {
                this.DG.GG.properties[id].externalID = patientObject.external_id;
            } else {
                delete this.DG.GG.properties[id].externalID;
            }

            if (patientObject.hasOwnProperty("features") && patientObject.features.length > 0) {
                this.DG.GG.properties[id].features = patientObject.features;
            } else {
                delete this.DG.GG.properties[id].features;
            }

            if (patientObject.hasOwnProperty("nonstandard_features") && patientObject.nonstandard_features.length > 0) {
                this.DG.GG.properties[id].nonstandard_features = patientObject.nonstandard_features;
            } else {
                delete this.DG.GG.properties[id].nonstandard_features;
            }

            var disorders = [];
            if (patientObject.hasOwnProperty("disorders")) {
                // e.g.: "disorders":[{"id":"MIM:120970","label":"#120970 CONE-ROD DYSTROPHY 2; CORD2 ;;CONE-ROD DYSTROPHY; CORD;; CONE-ROD RETINAL DYSTROPHY; CRD; CRD2;; RETINAL CONE-ROD DYSTROPHY; RCRD2"},{"id":"MIM:190685","label":"#190685 DOWN SYNDROME TRISOMY 21, INCLUDED;; DOWN SYNDROME CHROMOSOME REGION, INCLUDED; DCR, INCLUDED;; DOWN SYNDROME CRITICAL REGION, INCLUDED; DSCR, INCLUDED;; TRANSIENT MYELOPROLIFERATIVE DISORDER OF DOWN SYNDROME, INCLUDED;; LEUKEMIA, MEGAKARYOBLASTIC, OF DOWN SYNDROME, INCLUDED"}]
                for (var i = 0; i < patientObject.disorders.length; i++) {
                    var disorderID = patientObject.disorders[i].id || patientObject.disorders[i].label;
                    var match = disorderID.match(/^MIM:(\d+)$/);
                    match && (disorderID = match[1]);
                    disorders.push(disorderID);
                }
            }
            if (disorders.length > 0) {
                this.DG.GG.properties[id].disorders = disorders;
            } else {
                delete this.DG.GG.properties[id].disorders;
            }

            if (patientObject.hasOwnProperty("genes")) {
                this.DG.GG.properties[id].genes = patientObject.genes;
            } else {
                delete this.DG.GG.properties[id].genes;
            }

            if (patientObject.hasOwnProperty("family_history")) {
                // stored so that on save consanguinity status can be set via family_history without losing
                // previous family_history
                this.DG.GG.properties[id]["family_history"] = patientObject["family_history"];
            }
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
            if (!this.isPerson(v) && !this.isRelationship(v))
                throw "Assertion failed: getAllChildren() is applied to a non-person non-relationship node";

            var rels = this.isRelationship(v) ? [v] : this.DG.GG.getAllRelationships(v);

            var allChildren = [];
            for (var i = 0; i < rels.length; i++) {
                var chhub    = this.DG.GG.getOutEdges(rels[i])[0];
                var children = this.DG.GG.getOutEdges(chhub);

                allChildren = allChildren.concat(children);
            }
            return allChildren;
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
            for (var i = 0; i <= this.DG.GG.getMaxRealVertexId(); i++) {
               if (!this.isPerson(i)) continue;
               if (this.DG.GG.inedges[i].length != 0) continue;
               if (this.DG.ancestors[v].hasOwnProperty(i)) continue;
               result.push(i);
            }
            return result;
        },

        getPossibleSiblingsOf: function( v )
        {
            // all person nodes which are not ancestors and not descendants
            // if v has parents only nodes without parents are returned
            var hasParents = (this.getParentRelationship(v) !== null);
            var result = [];
            for (var i = 0; i <= this.DG.GG.getMaxRealVertexId(); i++) {
               if (!this.isPerson(i)) continue;
               if (this.DG.ancestors[v].hasOwnProperty(i)) continue;
               if (this.DG.ancestors[i].hasOwnProperty(v)) continue;
               if (hasParents && this.DG.GG.inedges[i].length != 0) continue;
               result.push(i);
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
            //  2) (updated) any gender

            var allPersonNodes = this.getAllPersonIDs();
            var result = [];
            // exclude those nodes which already have a phenotipsID link
            for (var i = 0; i < allPersonNodes.length; i++) {
                if (this.getPhenotipsLinkID(allPersonNodes[i]) == "") {
                    result.push(allPersonNodes[i]);
                }
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

            // fix common layout mistakes (e.g. relationship not right above the only child)
            // and update vertical positioning of all edges
            this._heuristics.improvePositioning(ranksBefore, rankYBefore);

            // update ancestors
            this.updateAncestors();

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
            this._heuristics.swapBeforeParentsToBringToSideIfPossible( personId );

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

            // fix common layout mistakes (e.g. relationship not right above the only child)
            // and update vertical positioning of all edges
            this._heuristics.improvePositioning(ranksBefore, rankYBefore);

            // update ancestors
            this.updateAncestors();

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
            this._heuristics.swapPartnerToBringToSideIfPossible( personId );   // TODO: only iff "needSwap"?
            this._heuristics.swapTwinsToBringToSideIfPossible( personId );

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

            // fix common layout mistakes (e.g. relationship not right above the only child)
            // and update vertical positioning of all edges
            this._heuristics.improvePositioning(ranksBefore, rankYBefore);

            // update ancestors
            this.updateAncestors();

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
                this._updateauxiliaryStructures(ranksBefore, rankYBefore);

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

                // fix common layout mistakes (e.g. relationship not right above the only child)
                // and update vertical positioning of all edges
                this._heuristics.improvePositioning(ranksBefore, rankYBefore);

                // update ancestors
                this.updateAncestors();

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

            // fix common layout mistakes (e.g. relationship not right above the only child)
            // and update vertical positioning of all edges
            this._heuristics.improvePositioning(ranksBefore, rankYBefore);

            // update ancestors
            this.updateAncestors();

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

            // update ancestors
            this.updateAncestors();

            // fix common layout mistakes (e.g. relationship not right above the only child)
            this._heuristics.improvePositioning(ranksBefore, rankYBefore);

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

            this._heuristics.improvePositioning(ranksBefore, rankYBefore);

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
                        for (var j = 0; j < pathToParents[p].length; j++)
                            if (this.DG.GG.isVirtual(pathToParents[p][j])) {
                                //console.log("adding " + pathToParents[p][j] + " to removal list (virtual of " + nodeList[i] + ")");
                                nodeList.push(pathToParents[p][j]);
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

            // note: do not update rankY, as we do not want to move anything (we know we don't need more Y space after a deletion)
            this.DG.vertLevel = this.DG.positionVertically();
            this.updateAncestors();

            // TODO: for now: redraw all relationships
            for (var i = 0 ; i <= this.getMaxNodeId(); i++)
                if (this.isRelationship(i))
                    moved.push(i);

            // note: _findMovedNodes() does not work when IDs have changed. TODO
            //var movedNodes = this._findMovedNodes( numNodesBefore, positionsBefore, ranksBefore, vertLevelsBefore, rankYBefore );
            //for (var i = 0; i < moved.length; i++)
            //    if (!Helpers.arrayContains(movedNodes, moved[i]))
            //        movedNodes.push(moved[i]);

            // note: moved now has the correct IDs valid in the graph with all affected nodes removed
            return {"removed": removed, "changedIDSet": changedIDSet, "moved": moved };
        },

        improvePosition: function ()
        {
            //this.DG.positions = this.DG.position(this.DG.horizontalPersonSeparationDist, this.DG.horizontalRelSeparationDist);
            //var movedNodes = this._getAllNodes();
            //return {"moved": movedNodes};
            var positionsBefore  = this.DG.positions.slice(0);
            var ranksBefore      = this.DG.ranks.slice(0);
            var vertLevelsBefore = this.DG.vertLevel.copy();
            var rankYBefore      = this.DG.rankY.slice(0);
            var numNodesBefore   = this.DG.GG.getMaxRealVertexId();

            // fix common layout mistakes (e.g. relationship not right above the only child)
            this._heuristics.improvePositioning(ranksBefore, rankYBefore);

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

            this.DG.rankY     = this.DG.computeRankY(ranksBefore, rankYBefore);

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

            var baseGraph = PedigreeImport.initFromPhenotipsInternal(this._onlyProbandGraph);

            this._recreateUsingBaseGraph(baseGraph, 0 /* mark the only remaining node as proband */);

            this.setProperties(0, remainingNodeProperties);

            if (emptyGraph) {
                return {"new": [0], "makevisible": [0]};
            }

            return {"removed": removedNodes, "new": [0], "makevisible": [0]};
        },

        redrawAll: function (removedBeforeRedrawList, animateList, newList, ranksBefore)
        {
            var ranksBefore = ranksBefore ? ranksBefore : this.DG.ranks.slice(0);  // sometimes we want to use ranksbefore as they were before some stuff was added to the graph before a redraw

            this._debugPrintAll("before");

            var baseGraph = this.DG.GG.makeGWithCollapsedMultiRankEdges();

            // collect current node ranks so that the new layout can be made more similar to the current one
            var oldRanks = Helpers.clone2DArray(this.DG.order.order);
            for (var i = oldRanks.length - 1; i >=0; i--) {
                oldRanks[i] = oldRanks[i].filter(this.DG.GG.isPerson.bind(this.DG.GG));
                if (oldRanks[i].length == 0)
                    oldRanks.splice(i, 1);
            }

            if (!this._recreateUsingBaseGraph(baseGraph, this.getProbandId(), oldRanks)) return {};  // no changes

            var movedNodes = this._getAllNodes();

            var probandReRankSize = (ranksBefore[this.getProbandId()] - this.DG.ranks[this.getProbandId()]);
            var reRankedDiffFrom0 = []
            var reRanked          = [];
            for (var i = 0; i <= this.DG.GG.getMaxRealVertexId(); i++) {
                if (this.DG.GG.isPerson(i)) {
                    if (this.DG.ranks[i] != ranksBefore[i]) {
                        reRanked.push(i);
                    }
                    if ((ranksBefore[i] - this.DG.ranks[i]) != probandReRankSize) {
                        reRankedDiffFrom0.push(i);
                    }
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

        // remove empty-values optional properties, e.g. "fName: ''" or "disorders: []"
        stripUnusedProperties: function() {
            for (var i = 0; i <= this.DG.GG.getMaxRealVertexId(); i++) {
                if (this.isPerson(i)) {
                    this.deleteEmptyProperty(i, "fName");
                    this.deleteEmptyProperty(i, "lName");
                    this.deleteEmptyProperty(i, "gestationAge");
                    this.deleteEmptyProperty(i, "carrierStatus");
                    this.deleteEmptyProperty(i, "comments");
                    this.deleteEmptyProperty(i, "disorders");
                }
             }
        },

        deleteEmptyProperty: function(nodeID, propName) {
            if (this.DG.GG.properties[nodeID].hasOwnProperty(propName)) {
                if (Object.prototype.toString.call(this.DG.GG.properties[nodeID][propName]) === '[object Array]' &&
                    this.DG.GG.properties[nodeID][propName].length == 0) {
                    delete this.DG.GG.properties[nodeID][propName];
                } else if (this.DG.GG.properties[nodeID][propName] == "") {
                    delete this.DG.GG.properties[nodeID][propName];
                }
            }
        },

        toJSONObject: function ()
        {
            this.stripUnusedProperties();

            //var timer = new Helpers.Timer();
            var output = {};

            // note: when saving positioned graph, need to save the version of the graph which has virtual edge pieces
            output["GG"] = this.DG.GG.serialize();

            output["ranks"]         = this.DG.ranks;
            output["order"]         = this.DG.order.serialize();
            output["positions"]     = this.DG.positions;
            output["probandNodeID"] = this.getProbandId();
            output["JSON_version"]  = editor.getInternalJSONVersion();

            // note: everything else can be recomputed based on the information above

            console.log("JSON represenation: " + JSON.stringify(output));
            //timer.printSinceLast("=== to JSON: ");

            return output;
        },

        fromJSONObject: function (jsonData)
        {
            if (!jsonData.hasOwnProperty("JSON_version") || jsonData["JSON_version"] != editor.getInternalJSONVersion()) {
                alert("Can not initialize from a JSON: unsupported version of pedigree JSON format");
                return {};
            }

            var removedNodes = this._getAllNodes();

            //console.log("Got serialization object: " + Helpers.stringifyObject(jsonData));

            this.DG.GG = PedigreeImport.initFromPhenotipsInternal(jsonData["GG"]);

            this.DG.ranks = jsonData["ranks"];

            this.DG.maxRank = Math.max.apply(null, this.DG.ranks);

            this.DG.order.deserialize(jsonData["order"]);

            this.DG.positions = jsonData["positions"];

            this.DG.probandId = jsonData.hasOwnProperty("probandNodeID") ? jsonData["probandNodeID"] : -1;

            this._updateauxiliaryStructures();

            this.screenRankShift = 0;

            var newNodes = this._getAllNodes();

            return {"new": newNodes, "removed": removedNodes};
        },

        fromImport: function (importString, importType, importOptions)
        {
            var removedNodes = this._getAllNodes();

            //this._debugPrintAll("before");

            var importData = null;

            if (importType == "ped") {
                importData = PedigreeImport.initFromPED(importString, importOptions.acceptUnknownPhenotypes, importOptions.markEvaluated, importOptions.externalIdMark);
            } else if (importType == "BOADICEA") {
                importData = PedigreeImport.initFromBOADICEA(importString, importOptions.externalIdMark);
            } else if (importType == "gedcom") {
                importData = PedigreeImport.initFromGEDCOM(importString, importOptions.markEvaluated, importOptions.externalIdMark);
            } else if (importType == "simpleJSON") {
                importData = PedigreeImport.initFromSimpleJSON(importString);
            }  else if (importType == "phenotipsJSON") {
                // TODO
            }
            //this._debugPrintAll("after");

            if (importData == null
                || !importData.hasOwnProperty("baseGraph")
                || !importData.hasOwnProperty("probandNodeID")) {
                return null;  // no changes
            }

            if (!this._recreateUsingBaseGraph(importData.baseGraph, importData.probandNodeID)) {
                return null;  // no changes
            }

            var newNodes = this._getAllNodes();

            return {"new": newNodes, "removed": removedNodes};
        },

        getPathToParents: function(v)
        {
            // returns an array with two elements: path to parent1 (excluding v) and path to parent2 (excluding v):
            // [ [virtual_node_11, ..., virtual_node_1n, parent1], [virtual_node_21, ..., virtual_node_2n, parent21] ]
            return this.DG.GG.getPathToParents(v);
        },

        //=============================================================

        // suggestedRanks: when provided, attempt to use the suggested rank for all nodes,
        //                 in order to keep the new layout as close as possible to the previous layout
        _recreateUsingBaseGraph: function (baseGraph, probandNodeID, suggestedRanks)
        {
            try {
                var newDG = new PositionedGraph( baseGraph,
                                                 probandNodeID,
                                                 this.DG.horizontalPersonSeparationDist,
                                                 this.DG.horizontalRelSeparationDist,
                                                 this.DG.maxInitOrderingBuckets,
                                                 this.DG.maxOrderingIterations,
                                                 this.DG.maxXcoordIterations,
                                                 false,
                                                 suggestedRanks );
            } catch (e) {
                console.log("ERROR re-creating graph: " + e);
                return false;
            }

            this.DG          = newDG;
            this._heuristics = new Heuristics( this.DG );

            //this._debugPrintAll("before improvement");
            this._heuristics.improvePositioning();
            //this._debugPrintAll("after improvement");

            return true;
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
            this._heuristics.moveToCorrectPositionAndMoveOtherNodesAsNecessary( newNodeId, nodeToKeepEdgeStraightTo );

            return newNodeId;
        },

        _updateauxiliaryStructures: function(ranksBefore, rankYBefore)
        {
            var timer = new Helpers.Timer();

            // update vertical levels
            this.DG.vertLevel = this.DG.positionVertically();
            this.DG.rankY     = this.DG.computeRankY(ranksBefore, rankYBefore);

            // update ancestors
            this.updateAncestors();

            timer.printSinceLast("=== Vertical spacing + ancestors runtime: ");
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
            var childrenInfo = this._heuristics.analizeChildren(vertex);
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
                vSibglingInfo = this._heuristics.analizeChildren(existingU);

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
                            var targetChildren = this._heuristics.analizeChildren(target);

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
                var childrenInfo = this._heuristics.analizeChildren(node);

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

            if (!validGendersSet) {
                validGendersSet = ['M','F','U','O'];
            }

            // validate input genders
            for (var i = 0; i < validGendersSet.length; i++) {
                validGendersSet[i] = validGendersSet[i].toLowerCase();
                if (validGendersSet[i] != 'u' && validGendersSet[i] != 'm' && validGendersSet[i] != 'f' && validGendersSet[i] != 'o') {
                    console.log("ERROR: Invalid gender: " + validGendersSet[i]);
                    return [];
                }
            }

             var result = [];

             for (var i = 0; i <= this.DG.GG.getMaxRealVertexId(); i++) {
                if (!this.isPerson(i)) continue;
                if (this.isPersonGroup(i)) continue;
                if (this.isPlaceholder(i)) continue;
                var gender = this.getProperties(i)["gender"].toLowerCase();
                //console.log("trying: " + i + ", gender: " + gender + ", validSet: " + Helpers.stringifyObject(validGendersSet));
                if (Helpers.arrayContains(validGendersSet, gender))
                    result.push(i);
             }

             return result;
        }
    };


    Heuristics = function( drawGraph )
    {
        this.DG = drawGraph;
    };

    Heuristics.prototype = {

        swapPartnerToBringToSideIfPossible: function ( personId )
        {
            // attempts to swap this person with it's existing partner if the swap makes the not-yet-parnered
            // side of the person on the side which favours child insertion (e.g. the side where the child closest
            // to the side has no parners)

            if (this.DG.GG.getTwinGroupId(personId) !== null) return;  // there is a separate heuristic for twin rearrangements

            var rank  = this.DG.ranks[personId];
            var order = this.DG.order.vOrder[personId];

            if (order == 0 || order == this.DG.order.order[rank].length - 1) return; // node on one of the sides: can do well without nay swaps

            var parnetships = this.DG.GG.getAllRelationships(personId);
            if (parnetships.length != 1) return;    // only if have exactly one parner
            var relationship = parnetships[0];
            var relOrder     = this.DG.order.vOrder[relationship];

            var partners  = this.DG.GG.getParents(relationship);
            var partnerId = (partners[0] == personId) ? partners[1] : partners[0];  // the only partner of personId
            var parnerOutEdges = this.DG.GG.getOutEdges(partnerId);
            if (parnerOutEdges.length != 1) return;  // only if parner also has exactly one parner (which is personId)

            if (this.DG.ranks[personId] != this.DG.ranks[partnerId]) return; // different ranks, heuristic does not apply

            var partnerOrder = this.DG.order.vOrder[partnerId];
            if (partnerOrder != order - 2 && partnerOrder != order + 2) return;  // only if next to each other

            // if both have parents do not swap so that parent edges are not crossed
            if (this.DG.GG.getInEdges(personId).length != 0 &&
                this.DG.GG.getInEdges(partnerId).length != 0 ) return;

            var childhubId = this.DG.GG.getOutEdges(relationship)[0]; // <=> getRelationshipChildhub(relationship)
            var children   = this.DG.GG.getOutEdges(childhubId);

            if (children.length == 0) return;

            // TODO: count how many edges will be crossed in each case and also swap if we save a few crossings?

            // idea:
            // if (to the left  of parner && leftmostChild  has parner to the left  && rightmostchid has no parner to the right) -> swap
            // if (to the right of parner && rightmostChild has parner to the right && leftmostchid  has no parner to the left) -> swap

            var toTheLeft = (order < partnerOrder);

            var childrenPartners = this.analizeChildren(childhubId);

            if ( (toTheLeft  && childrenPartners.leftMostHasLParner  && !childrenPartners.rightMostHasRParner) ||
                 (!toTheLeft && childrenPartners.rightMostHasRParner && !childrenPartners.leftMostHasLParner) ||
                 (order == 2 && childrenPartners.rightMostHasRParner) ||
                 (order == this.DG.order.order[rank].length - 3 && childrenPartners.leftMostHasLParner) ) {
                this.swapPartners( personId, partnerId, relationship );  // updates orders + positions
            }
        },

        swapTwinsToBringToSideIfPossible: function( personId )
        {
            var twinGroupId = this.DG.GG.getTwinGroupId(personId);
            if (twinGroupId === null) return;

            //TODO
        },

        analizeChildren: function (childhubId)
        {
            if (this.DG.GG.isRelationship(childhubId))
                childhubId = this.DG.GG.getOutEdges(childhubId)[0];

            if (!this.DG.GG.isChildhub(childhubId))
                throw "Assertion failed: applying analizeChildren() not to a childhub";

            var children = this.DG.GG.getOutEdges(childhubId);

            if (children.length == 0) return;

            var havePartners        = {};
            var numWithPartners     = 0;
            var numWithTwoPartners  = 0;
            var leftMostChildId     = undefined;
            var leftMostChildOrder  = Infinity;
            var leftMostHasLParner  = false;
            var rightMostChildId    = undefined;
            var rightMostChildOrder = -Infinity;
            var rightMostHasRParner = false;

            var onlyPlaceholder = false;
            if (children.length == 1 && this.DG.GG.isPlaceholder(children[0])) {
                onlyPlaceholder = true;
            }

            for (var i = 0; i < children.length; i++) {
                var childId = children[i];
                var order   = this.DG.order.vOrder[childId];

                if (order < leftMostChildOrder) {
                    leftMostChildId    = childId;
                    leftMostChildOrder = order;
                    leftMostHasLParner = this.hasParnerBetweenOrders(childId, 0, order-1);  // has partner to the left
                }
                if (order > rightMostChildOrder) {
                    rightMostChildId    = childId;
                    rightMostChildOrder = order;
                    rightMostHasRParner = this.hasParnerBetweenOrders(childId, order+1, Infinity);  // has partner to the right
                }
                if (this.DG.GG.getOutEdges(childId).length > 0) {
                    havePartners[childId] = true;
                    numWithPartners++;
                    if (this.DG.GG.getOutEdges(childId).length > 1) {
                        numWithTwoPartners++;
                    }
                }
            }

            var orderedChildren = this.DG.order.sortByOrder(children);
            //console.log("ordered ch: " + Helpers.stringifyObject(orderedChildren));

            return {"leftMostHasLParner" : leftMostHasLParner,
                    "leftMostChildId"    : leftMostChildId,
                    "leftMostChildOrder" : leftMostChildOrder,
                    "rightMostHasRParner": rightMostHasRParner,
                    "rightMostChildId"   : rightMostChildId,
                    "rightMostChildOrder": rightMostChildOrder,
                    "withPartnerSet"     : havePartners,
                    "numWithPartners"    : numWithPartners,
                    "numWithTwoPartners" : numWithTwoPartners,
                    "orderedChildren"    : orderedChildren,
                    "onlyPlaceholder"    : onlyPlaceholder };
        },

        hasParnerBetweenOrders: function( personId, minOrder, maxOrder )
        {
            var rank  = this.DG.ranks[personId];
            var order = this.DG.order.vOrder[personId];

            var outEdges = this.DG.GG.getOutEdges(personId);

            for (var i = 0; i < outEdges.length; i++ ) {
                var relationship = outEdges[i];
                var relRank      = this.DG.ranks[relationship];
                if (relRank != rank) continue;

                var relOrder = this.DG.order.vOrder[relationship];
                if (relOrder >= minOrder && relOrder <= maxOrder)
                    return true;
            }

            return false;
        },

        swapPartners: function( partner1, partner2, relationshipId)
        {
            var rank = this.DG.ranks[partner1];
            if (this.DG.ranks[partner2] != rank || this.DG.ranks[relationshipId] != rank)
                throw "Assertion failed: swapping nodes of different ranks";

            var order1   = this.DG.order.vOrder[partner1];
            var order2   = this.DG.order.vOrder[partner2];
            var orderRel = this.DG.order.vOrder[relationshipId];

            // normalize: partner1 always to the left pf partner2, relationship in the middle
            if (order1 > order2) {
                var tmpOrder = order1;
                var tmpId    = partner1;
                order1   = order2;
                partner1 = partner2;
                order2   = tmpOrder;
                partner2 = tmpId;
            }

            if ( (order1 + 1) != orderRel || (orderRel + 1) != order2 ) return;

            this.DG.order.exchange(rank, order1, order2);

            var widthDecrease = this.DG.GG.getVertexWidth(partner1) - this.DG.GG.getVertexWidth(partner2);

            var pos2 = this.DG.positions[partner2];
            this.DG.positions[partner2] = this.DG.positions[partner1];
            this.DG.positions[partner1] = pos2 - widthDecrease;
            this.DG.positions[relationshipId] -= widthDecrease;
        },

        moveSiblingPlusPartnerToOrder: function ( personId, partnerId, partnershipId, newOrder)
        {
            // transforms this
            //   [partnerSibling1 @ newOrder] ... [partnerSiblingN] [person]--[*]--[partner]
            // into
            //   [person @ newOrder]--[*]--[partner] [partnerSibling1] ... [partnerCiblingN]
            //
            // assumes 1. there are no relationship nodes between partnershipId & newOrder
            //         2. when moving left, partner is the rightmost node of the 3 given,
            //            when moving right partner is the leftmost node of the 3 given

            var rank         = this.DG.ranks[partnerId];
            var partnerOrder = this.DG.order.vOrder[partnerId];
            var personOrder  = this.DG.order.vOrder[personId];
            var relOrder     = this.DG.order.vOrder[partnershipId];

            var moveOrders = newOrder - personOrder;

            var moveDistance  = this.DG.positions[this.DG.order.order[rank][newOrder]] - this.DG.positions[personId];

            var moveRight     = (newOrder > personOrder);
            var firstSibling  = moveRight ? this.DG.order.order[rank][personOrder + 1] : this.DG.order.order[rank][personOrder - 1];
            var moveOtherDist = this.DG.positions[firstSibling] - this.DG.positions[partnerId];

            //console.log("before move: " + Helpers.stringifyObject(this.DG.order));

            this.DG.order.move(rank, personOrder,  moveOrders);
            this.DG.order.move(rank, relOrder,     moveOrders);
            this.DG.order.move(rank, partnerOrder, moveOrders);

            //console.log("after move: " + Helpers.stringifyObject(this.DG.order));

            this.DG.positions[personId]      += moveDistance;
            this.DG.positions[partnerId]     += moveDistance;
            this.DG.positions[partnershipId] += moveDistance;

            var minMovedOrder = moveRight ?  partnerOrder : newOrder + 3;
            var maxMovedOrder = moveRight ?  newOrder - 3 : partnerOrder;
            for (var o = minMovedOrder; o <= maxMovedOrder; o++) {
                var node = this.DG.order.order[rank][o];
                console.log("moving: " + node);
                this.DG.positions[node] -= moveOtherDist;
            }
        },

        swapBeforeParentsToBringToSideIfPossible: function ( personId )
        {
            // used to swap this node AND its only partner to bring the two to the side to clear
            // space above for new parents of this node

            // 1. check that we have exactly one partner and it has parents - if not nothing to move
            var parnetships = this.DG.GG.getAllRelationships(personId);
            if (parnetships.length != 1) return;
            var relationshipId = parnetships[0];

            var partners  = this.DG.GG.getParents(relationshipId);
            var partnerId = (partners[0] == personId) ? partners[1] : partners[0];  // the only partner of personId
            if (this.DG.GG.getInEdges(partnerId).length == 0) return; // partner has no parents!

            if (this.DG.ranks[personId] != this.DG.ranks[partnerId]) return; // different ranks, heuristic does not apply

            if (this.DG.GG.getOutEdges(partnerId).length > 1) return; // partner has multiple partnerships, too complicated

            var order        = this.DG.order.vOrder[personId];
            var partnerOrder = this.DG.order.vOrder[partnerId];
            if (partnerOrder != order - 2 && partnerOrder != order + 2) return;  // only if next to each other

            var toTheLeft = (order < partnerOrder);

            // 2. check where the partner stands among its siblings
            var partnerChildhubId   = this.DG.GG.getInEdges(partnerId)[0];
            var partnerSibglingInfo = this.analizeChildren(partnerChildhubId);

            //if (partnerSibglingInfo.orderedChildren.length == 1) return; // just one sibling, nothing to do
            if (partnerSibglingInfo.orderedChildren.length > 1) {
                // simple cases:  ...
                //                 |
                //       +---------+-----------|
                //       |                     |
                //   [sibling]--[personID]  [sibling]
                if (partnerSibglingInfo.leftMostChildId == partnerId) {
                    if (!toTheLeft)
                        this.swapPartners( personId, partnerId, relationshipId );
                    return;
                }
                if (partnerSibglingInfo.rightMostChildId == partnerId) {
                    if (toTheLeft)
                        this.swapPartners( personId, partnerId, relationshipId );
                    return;
                }
            }

            // ok, partner is in the middle => may need to move some nodes around to place personId in a
            //                                 position where parents can be inserted with least disturbance

            // 2. check how many partners partner's parents have. if both have more than one the case
            //    is too complicated and skip moving nodes around
            var partnerParents = this.DG.GG.getInEdges(this.DG.GG.getInEdges(partnerChildhubId)[0]);
            var order0 = this.DG.order.vOrder[partnerParents[0]];
            var order1 = this.DG.order.vOrder[partnerParents[1]];
            var leftParent  = (order0 > order1) ? partnerParents[1] : partnerParents[0];
            var rightParent = (order0 > order1) ? partnerParents[0] : partnerParents[1];
            console.log("parents: " + Helpers.stringifyObject(partnerParents));
            var numLeftPartners  = this.DG.GG.getOutEdges(leftParent).length;
            var numRightPartners = this.DG.GG.getOutEdges(rightParent).length;
            console.log("num left: " + numLeftPartners + ", numRight: " + numRightPartners);
            if (numLeftPartners > 1 && numRightPartners > 1) return;

            if (partnerSibglingInfo.orderedChildren.length == 1) {
                if (numLeftPartners == 1 && numRightPartners == 1) {
                    // no need to move anything enywhere, we are fine as we are now
                    return;
                }
                if (numLeftPartners == 1 && !toTheLeft) {
                    this.swapPartners( personId, partnerId, relationshipId );
                }
                if (numRightPartners == 1 && toTheLeft) {
                    this.swapPartners( personId, partnerId, relationshipId );
                }
                return; // the rest is for the case of multiple children
            }

            // 3. check how deep the tree below is.
            //    do nothing if any children have partners (too complicated for a heuristic)
            var childHubBelow = this.DG.GG.getRelationshipChildhub(relationshipId);
            var childrenInfo  = this.analizeChildren(childHubBelow);
            if (childrenInfo.numWithPartners > 0) return;  // too complicated for a heuristic

            // 4. ok, the tree below is not deep, partner is surrounded by siblings.
            //    check if we can move it right or left easily:
            //    move to the right iff: rightmostchild has no partners && rightParent has no partners
            //    move to the left iff: leftmostchild has no partners && leftParent has no partners
            if (numRightPartners == 1 && !partnerSibglingInfo.rightMostHasRParner) {
                for (var c = partnerSibglingInfo.orderedChildren.length - 1; c >= 0; c--) {
                    var sibling = partnerSibglingInfo.orderedChildren[c];
                    if (sibling == partnerId) {
                        if (toTheLeft)
                            this.swapPartners( personId, partnerId, relationshipId );
                        this.moveSiblingPlusPartnerToOrder( personId, partnerId, relationshipId, partnerSibglingInfo.rightMostChildOrder);
                        return;
                    }
                    if (partnerSibglingInfo.withPartnerSet.hasOwnProperty(sibling)) break; // does not work on this side
                }
            }
            if (numLeftPartners == 1 && !partnerSibglingInfo.leftMostHasLParner) {
                for (var c = 0; c < partnerSibglingInfo.orderedChildren.length; c++) {
                    var sibling = partnerSibglingInfo.orderedChildren[c];
                    if (sibling == partnerId) {
                        if (!toTheLeft)
                            this.swapPartners( personId, partnerId, relationshipId );
                        this.moveSiblingPlusPartnerToOrder( personId, partnerId, relationshipId, partnerSibglingInfo.leftMostChildOrder);
                        return;
                    }
                    if (partnerSibglingInfo.withPartnerSet.hasOwnProperty(sibling)) break; // does not work on this side
                }
            }
        },

        improvePositioning: function (ranksBefore, rankYBefore)
        {
            var timer = new Helpers.Timer();

            //console.log("pre-fix orders: " + Helpers.stringifyObject(this.DG.order));
            //var xcoord = new XCoord(this.DG.positions, this.DG);
            //this.DG.displayGraph(xcoord.xcoord, "pre-fix");

            //DEBUG: for testing how layout looks when multi-rank edges are not improved
            //this.DG.vertLevel = this.DG.positionVertically();
            //this.DG.rankY     = this.DG.computeRankY(ranksBefore, rankYBefore);
            //return;

            // given a finished positioned graph (asserts the graph is valid):
            //
            // 1. fix some display requirements, such as relationship lines always going to the right or left first before going down
            //
            // 2. fix some common layout imperfections, such as:
            //    A) the only relationship not right above the only child: can be fixed by
            //       a) moving the child, if possible without disturbiung other nodes
            //       b) moving relationship + one (or both, if possible) partners, if possible without disturbiung other nodes
            //    B) relationship not above one of it's children (preferably one in the middle) and not
            //       right in the midpoint between left and right child: can be fixed by
            //       a) moving relationship + both partners, if possible without disturbiung other nodes
            //    C) not nice long edge crossings (example pending) - TODO
            //    D) a relationship edge can be made shorter and bring two parts of the graph separated by the edge closer together
            //    E) after everything else try to center relationships between the partners (and move children accordingly)

            // 1) improve layout of multi-rank relationships:
            //    relationship lines should always going to the right or left first before going down
            var modified = false;
            for (var parent = 0; parent <= this.DG.GG.getMaxRealVertexId(); parent++) {
                if (!this.DG.GG.isPerson(parent)) continue;

                var rank  = this.DG.ranks[parent];
                var order = this.DG.order.vOrder[parent];

                var outEdges = this.DG.GG.getOutEdges(parent);

                var sameRankToTheLeft  = 0;
                var sameRankToTheRight = 0;

                var multiRankEdges = [];
                for (var i = 0; i < outEdges.length; i++) {
                    var node = outEdges[i];
                    if (this.DG.ranks[node] != rank)
                        multiRankEdges.push(node);
                    else {
                        if (this.DG.order.vOrder[node] < order)
                            sameRankToTheLeft++;
                        else
                            sameRankToTheRight++;
                    }
                }
                if (multiRankEdges.length == 0) continue;

                // sort all by their xcoordinate if to the left of parent, and in reverse order if to the right of parent
                // e.g. [1] [2] [3] NODE [4] [5] [6] gets sorted into [1,2,3, 6,5,4], so that edges that end up closer
                // to the node can be inserted closer as wel, and end up below other edges thus eliminating any intersections
                var _this = this;
                byXcoord = function(v1,v2) {
                        var rel1      = _this.DG.GG.downTheChainUntilNonVirtual(v1);
                        var rel2      = _this.DG.GG.downTheChainUntilNonVirtual(v2);
                        var position1 = _this.DG.positions[rel1];
                        var position2 = _this.DG.positions[rel2];
                        var parentPos = _this.DG.positions[parent];
                        //console.log("v1: " + v1 + ", pos: " + position1 + ", v2: " + v2 + ", pos: " + position2 + ", parPos: " + parentPos);
                        if (position1 >= parentPos && position2 >= parentPos)
                            return position2 - position1;
                        else
                            return position1 - position2;
                    };
                multiRankEdges.sort(byXcoord);

                console.log("multi-rank edges: " + Helpers.stringifyObject(multiRankEdges));

                for (var p = 0; p < multiRankEdges.length; p++) {

                    var firstOnPath = multiRankEdges[p];

                    var relNode = this.DG.GG.downTheChainUntilNonVirtual(firstOnPath);

                    // replace the edge from parent to firstOnPath by an edge from parent to newNodeId and
                    // from newNodeId to firstOnPath
                    var weight = this.DG.GG.removeEdge(parent, firstOnPath);

                    var newNodeId = this.DG.GG.insertVertex(BaseGraph.TYPE.VIRTUALEDGE, {}, weight, [parent], [firstOnPath]);

                    this.DG.ranks.splice(newNodeId, 0, rank);

                    var insertToTheRight = (this.DG.positions[relNode] < this.DG.positions[parent]) ? false : true;

                    if (this.DG.positions[relNode] == this.DG.positions[parent]) {
                        if (sameRankToTheRight > 0 && sameRankToTheLeft == 0 && multiRankEdges.length == 1) {
                            insertToTheRight = false;  // only one long edge and only one other edge: insert on the other side regardless of anything else
                        }
                    }

                    //console.log("inserting " + newNodeId + " (->" + firstOnPath + "), rightSide: " + insertToTheRight + " (pos[relNode]: " + this.DG.positions[relNode] + ", pos[parent]: " + this.DG.positions[parent]);

                    var parentOrder = this.DG.order.vOrder[parent]; // may have changed from what it was before due to insertions

                    var newOrder = insertToTheRight ? parentOrder + 1 : parentOrder;
                    if (insertToTheRight) {
                        while (newOrder < this.DG.order.order[rank].length &&
                               this.DG.positions[firstOnPath] > this.DG.positions[ this.DG.order.order[rank][newOrder] ])
                            newOrder++;

                        // fix common imperfection when this edge will cross a node-relationship edge. Testcase 4e covers this case.
                        var toTheLeft  = this.DG.order.order[rank][newOrder-1];
                        var toTheRight = this.DG.order.order[rank][newOrder];
                        if (this.DG.GG.isRelationship(toTheLeft) && this.DG.GG.isPerson(toTheRight) &&
                            this.DG.GG.hasEdge(toTheRight, toTheLeft) && this.DG.GG.getOutEdges(toTheRight).length ==1 )
                            newOrder++;
                        if (this.DG.GG.isRelationship(toTheRight) && this.DG.GG.isPerson(toTheLeft) &&
                            this.DG.GG.hasEdge(toTheLeft, toTheRight) && this.DG.GG.getOutEdges(toTheLeft).length ==1 )
                            newOrder--;
                    }
                    else {
                        while (newOrder > 0 &&
                               this.DG.positions[firstOnPath] < this.DG.positions[ this.DG.order.order[rank][newOrder-1] ])
                            newOrder--;

                        // fix common imprefetion when this edge will cross a node-relationship edge
                        var toTheLeft  = this.DG.order.order[rank][newOrder-1];
                        var toTheRight = this.DG.order.order[rank][newOrder];
                        if (this.DG.GG.isRelationship(toTheRight) && this.DG.GG.isPerson(toTheLeft) &&
                            this.DG.GG.hasEdge(toTheLeft, toTheRight) && this.DG.GG.getOutEdges(toTheLeft).length ==1 )
                            newOrder--;
                        if (this.DG.GG.isRelationship(toTheLeft) && this.DG.GG.isPerson(toTheRight) &&
                            this.DG.GG.hasEdge(toTheRight, toTheLeft) && this.DG.GG.getOutEdges(toTheRight).length ==1 )
                            newOrder++;
                    }

                    this.DG.order.insertAndShiftAllIdsAboveVByOne(newNodeId, rank, newOrder);

                    // update positions
                    this.DG.positions.splice( newNodeId, 0, -Infinity );  // temporary position: will move to the correct location and shift other nodes below
                    //this.DG.positions.splice( newNodeId, 0, 100 );

                    var nodeToKeepEdgeStraightTo = firstOnPath;
                    this.moveToCorrectPositionAndMoveOtherNodesAsNecessary( newNodeId, nodeToKeepEdgeStraightTo );

                    modified = true;
                }
            }

            this.optimizeLongEdgePlacement();

            timer.printSinceLast("=== Long edge handling runtime: ");

            //DEBUG: for testing how layout looks without any improvements
            //this.DG.vertLevel = this.DG.positionVertically();
            //this.DG.rankY     = this.DG.computeRankY(ranksBefore, rankYBefore);
            //return;

            // 2) fix some common layout imperfections
            var xcoord = new XCoord(this.DG.positions, this.DG);
            //this.DG.displayGraph(xcoord.xcoord, "after-long-edge-improvement");

            for (var v = 0; v <= this.DG.GG.getMaxRealVertexId(); v++) {
                if (!this.DG.GG.isRelationship(v)) continue;
                var childhub  = this.DG.GG.getRelationshipChildhub(v);
                var relX      = xcoord.xcoord[v];
                var childhubX = xcoord.xcoord[childhub];
                if (childhubX != relX) {
                    improved  = xcoord.moveNodeAsCloseToXAsPossible(childhub, relX);
                }
            }

            // search for gaps between children (which may happen due to deletions) and close them by moving children closer to each other
            for (var v = 0; v <= this.DG.GG.getMaxRealVertexId(); v++) {
                if (!this.DG.GG.isChildhub(v)) continue;
                var children = this.DG.GG.getOutEdges(v);
                if (children.length < 2) continue;

                var orderedChildren = this.DG.order.sortByOrder(children);

                // compress right-side children towards leftmost child, only moving childen withoout relationships
                for (var i = orderedChildren.length-1; i >= 0; i--) {
                    if (i == 0 || this.DG.GG.getOutEdges(orderedChildren[i]).length > 0) {
                        for (var j = i+1; j < orderedChildren.length; j++) {
                            xcoord.shiftLeftOneVertex(orderedChildren[j], Infinity);
                        }
                        break;
                    }
                }
                // compress left-side children towards rightmost child, only moving childen without relationships
                for (var i = 0; i < orderedChildren.length; i++) {
                    if (i == (orderedChildren.length-1) || this.DG.GG.getOutEdges(orderedChildren[i]).length > 0) {
                        for (var j = i-1; j >= 0; j--) {
                            xcoord.shiftRightOneVertex(orderedChildren[j], Infinity);
                        }
                        break;
                    }
                }
            }

            //this.DG.displayGraph(xcoord.xcoord, "after-basic-improvement");

            this._compactGraph(xcoord, 5);

            //this.DG.displayGraph(xcoord.xcoord, "after-compact1");

            var orderedRelationships = this.DG.order.getLeftToRightTopToBottomOrdering(BaseGraph.TYPE.RELATIONSHIP, this.DG.GG);
            //console.log("Ordered rels: " + Helpers.stringifyObject(orderedRelationships));

            var iter = 0;
            var improved = true;
            while (improved && iter < 20)
            {
                improved = false;
                iter++;
                //console.log("iter: " + iter);

                // fix relative positioning of relationships to their children
                for (var k = 0; k < orderedRelationships.length; k++) {
                    var v = orderedRelationships[k];

                    var parents   = this.DG.GG.getInEdges(v);
                    var childhub  = this.DG.GG.getRelationshipChildhub(v);

                    var relX      = xcoord.xcoord[v];
                    var childhubX = xcoord.xcoord[childhub];

                    var childInfo = this.analizeChildren(childhub);

                    //if (childInfo.onlyPlaceholder) continue;

                    var misalignment = 0;

                    // First try easy options: moving nodes without moving any other nodes (works in most cases and is fast)

                    // relationship withone child: special case for performance reasons
                    if (childInfo.orderedChildren.length == 1) {
                        var childId = childInfo.orderedChildren[0];
                        if (xcoord.xcoord[childId] == childhubX) continue;

                        improved = xcoord.moveNodeAsCloseToXAsPossible(childId, childhubX);
                        //console.log("moving " + childId + " to " + xcoord.xcoord[childId]);

                        if (xcoord.xcoord[childId] == childhubX) continue; // done

                        // ok, we can't move the child. Try to move the relationship & the parent(s)
                        misalignment = xcoord.xcoord[childId] - childhubX;
                    }
                    // relationships with many children: want to position in the "middle" inbetween the left and right child
                    //  (for one of the two definitionsof middle: exact center betoween leftmost and rightmost, or
                    //   right above the central child, e.g. 2nd of the 3)
                    else {
                        var positionInfo = this._computeDesiredChildhubLocation( childInfo, xcoord );

                        // no need to move anything when parent line is either above the mid-point between the leftmost and rightmost child
                        // or above the middle child of the three
                        if (positionInfo.minPreferred <= childhubX && childhubX <= positionInfo.maxPreferred) continue;

                        // of the two "OK" points pick the one which requires less movement
                        var shiftToX = (childhubX > positionInfo.maxPreferred) ? positionInfo.maxPreferred : positionInfo.minPreferred;

                        var needToShift = childhubX - shiftToX;

                        if (childInfo.numWithPartners == 0) {
                            // can shift children easily
                            if (needToShift < 0) {  // need to shift children left
                                var leftMost  = childInfo.leftMostChildId;
                                var leftSlack = xcoord.getSlackOnTheLeft(leftMost);
                                var haveSlack = Math.min(Math.abs(needToShift), leftSlack);
                                if (haveSlack > 0) {
                                    for (var i = 0; i < childInfo.orderedChildren.length; i++)
                                        xcoord.xcoord[childInfo.orderedChildren[i]] -= haveSlack;
                                    improved = true;
                                    needToShift += haveSlack;
                                }
                            }
                            else {  // need to shift children right
                                var rightMost  = childInfo.rightMostChildId;
                                var rightSlack = xcoord.getSlackOnTheRight(rightMost);
                                var haveSlack = Math.min(needToShift, rightSlack);
                                if (haveSlack > 0) {
                                    for (var i = 0; i < childInfo.orderedChildren.length; i++)
                                        xcoord.xcoord[childInfo.orderedChildren[i]] += haveSlack;
                                    improved = true;
                                    needToShift -= haveSlack;
                                }
                            }
                        }
                        misalignment = -needToShift;
                    }

                    if (misalignment == 0) continue;

                    // OK, harder case: either move the parents or the children (with whatever nodes are connected to them, in both cases).
                    // (need to make sure we do not break what has already been good, or we may be stuck in an infinite improvement loop)

                    // try to either shift the entire distance (misalignment) or (if that fails) at least
                    // as far as parents can go without pushing other nodes

                    //var id = id ? (id+1) : 1; // DEBUG
                    //console.log("Analizing for childhub " + childhub);

                    var leftParent  = (xcoord.xcoord[parents[0]] < xcoord.xcoord[parents[1]]) ? parents[0] : parents[1];
                    var rightParent = (xcoord.xcoord[parents[0]] < xcoord.xcoord[parents[1]]) ? parents[1] : parents[0];

                    var shiftList = [v, childhub];
                    if (this.DG.order.vOrder[leftParent]  == this.DG.order.vOrder[v] - 1 && !this.DG.GG.isVirtual(leftParent)) {
                        if (misalignment > 0 || xcoord.getSlackOnTheLeft(v) < -misalignment)
                            shiftList.unshift(leftParent);
                    }
                    if (this.DG.order.vOrder[rightParent] == this.DG.order.vOrder[v] + 1 && !this.DG.GG.isVirtual(rightParent)) {
                        if (misalignment < 0 || xcoord.getSlackOnTheRight(v) < misalignment)
                            shiftList.push(rightParent);
                    }
                    var noUpSet = {};
                    noUpSet[v] = true;
                    // findAffectedSet: function(v_list, dontmove_set, noUp_set, noDown_set, forbidden_set, shiftSize, xcoord, stopAtVirtual, minimizeMovement, stopAtPersons, stopAtRels)
                    var affectedInfoParentShift = this._findAffectedSet(shiftList, {}, noUpSet, Helpers.toObjectWithTrue(shiftList), Helpers.toObjectWithTrue(childInfo.orderedChildren),
                                                                        misalignment, xcoord, true, false, 7, 3);

                    var shiftList = childInfo.orderedChildren;
                    var forbiddenList = [v, childhub];
                    var affectedInfoChildShift = this._findAffectedSet(shiftList, {}, Helpers.toObjectWithTrue(childInfo.orderedChildren), {}, Helpers.toObjectWithTrue(forbiddenList),
                                                                       -misalignment, xcoord, true, false, 7, 3);

                    var parentShiftAcceptable = this._isShiftSizeAcceptable( affectedInfoParentShift, false, 7, 3);
                    var childShiftAcceptable  = this._isShiftSizeAcceptable( affectedInfoChildShift,  false, 7, 3);
                    //console.log("["+id+"] affectedInfoParentShift: " + Helpers.stringifyObject(affectedInfoParentShift) + ", acceptable: " + parentShiftAcceptable);
                    //console.log("["+id+"] affectedInfoChildShift:  " + Helpers.stringifyObject(affectedInfoChildShift) + ", acceptable: " + childShiftAcceptable);

                    if (parentShiftAcceptable || childShiftAcceptable) {
                        improved = true;   // at least one of the shifts is OK

                        // pick which one to use
                        if ( parentShiftAcceptable &&
                             (!childShiftAcceptable || this._isShiftBetter(affectedInfoParentShift, affectedInfoChildShift)) ) {
                            var nodes = affectedInfoParentShift.nodes;
                            //console.log("["+id+"] Shifting parents by [" + misalignment + "]: " + Helpers.stringifyObject(nodes));
                            for (var i = 0; i < nodes.length; i++)
                                xcoord.xcoord[nodes[i]] += misalignment;
                        } else {
                            var nodes = affectedInfoChildShift.nodes;
                            //console.log("["+id+"] Shifting children by [" + misalignment + "]: " + Helpers.stringifyObject(nodes));
                            for (var i = 0; i < nodes.length; i++)
                                xcoord.xcoord[nodes[i]] -= misalignment;
                        }

                        //xcoord.normalize();  // DEBUG
                        //this.DG.displayGraph(xcoord.xcoord, "shift-"+id);
                        continue;
                    }

                    // ok, can't move all the way: see if we can move parents at least a little in the desired direction
                    if (misalignment < 0) {
                        var leftShiftingNode = (this.DG.order.vOrder[leftParent] == this.DG.order.vOrder[v] - 1) ? leftParent : v;
                        var smallShift = Math.max(-xcoord.getSlackOnTheLeft(leftShiftingNode), misalignment);
                        if (smallShift == 0 || smallShift == misalignment) continue;
                    } else {
                        var rightShiftingNode = (this.DG.order.vOrder[rightParent] == this.DG.order.vOrder[v] + 1) ? rightParent : v;
                        var smallShift = Math.min(xcoord.getSlackOnTheLeft(rightShiftingNode), misalignment);
                        if (smallShift == 0 || smallShift == misalignment) continue;
                    }

                    var shiftList = [v, childhub];
                    if (this.DG.order.vOrder[leftParent]  == this.DG.order.vOrder[v] - 1 && !this.DG.GG.isVirtual(leftParent))
                        shiftList.unshift(leftParent);
                    if (this.DG.order.vOrder[rightParent] == this.DG.order.vOrder[v] + 1 && !this.DG.GG.isVirtual(rightParent))
                        shiftList.push(rightParent);
                    var noUpSet = {};
                    noUpSet[v] = true;
                    var affectedInfoParentShift = this._findAffectedSet(shiftList, {}, noUpSet, Helpers.toObjectWithTrue(shiftList), Helpers.toObjectWithTrue(childInfo.orderedChildren),
                                                                        smallShift, xcoord, true, false, 3, 2);

                    if (this._isShiftSizeAcceptable( affectedInfoParentShift, false, 3, 2)) {
                        var nodes = affectedInfoParentShift.nodes;
                        //console.log("["+id+"] Small-shifting parents by [" + smallShift + "]: " + Helpers.stringifyObject(nodes));
                        for (var i = 0; i < nodes.length; i++)
                            xcoord.xcoord[nodes[i]] += smallShift;

                        //xcoord.normalize();  // DEBUG
                        //this.DG.displayGraph(xcoord.xcoord, "shift-"+id);
                        continue;
                    }
                    //----------------------------------------------------------------
                }
            }

            // 2D) check if there is any extra whitespace in the graph, e.g. if a subgraph can be
            //     moved closer to the rest of the graph by shortening some edges (this may be
            //     the case after some imperfect insertion heuristics move stuff too far).
            //     E.g. see Testcase 5g with/without compacting
            //this.DG.displayGraph(xcoord.xcoord, "before-compact2");

            this._compactGraph(xcoord);

            //this.DG.displayGraph(xcoord.xcoord, "after-compact2");

            // 2E) center relationships between partners. Only do it if children-to-relationship positioning does not get worse
            //     (e.g. if it was centrered then if children can be shifted to stay centered)
            var iter = 0;
            var improved = true;
            while (improved && iter < 20) {
                improved = false;
                iter++;
                for (var k = 0; k < orderedRelationships.length; k++) {
                    var v = orderedRelationships[k];

                    var parents        = this.DG.GG.getInEdges(v);
                    var orderedParents = this.DG.order.sortByOrder(parents);

                    // only shift rel if partners are next to each other with only this relationship in between
                    if (Math.abs(this.DG.order.vOrder[parents[0]] - this.DG.order.vOrder[parents[1]]) != 2) continue;

                    var leftParentRightSide = xcoord.getRightEdge(orderedParents[0]);
                    var rightParentLeftSide = xcoord.getLeftEdge (orderedParents[1]);

                    var relX = xcoord.xcoord[v];
                    var midX = Math.floor((leftParentRightSide + rightParentLeftSide)/2);

                    if (relX == midX) continue;

                    //xcoord.normalize();  // DEBUG
                    //this.DG.displayGraph(xcoord.xcoord, "pre-imnprove");

                    var childhub  = this.DG.GG.getRelationshipChildhub(v);

                    var shiftSize = (midX - relX);
                    var shiftList = [v, childhub];
                    var noUpSet = {};
                    noUpSet[v] = true;
                    var affectedInfo = this._findAffectedSet(shiftList, {}, noUpSet, {}, {}, shiftSize, xcoord, true, false, 5, 3, this.DG.ranks[v]);

                    // need to check minAffectedRank to make sure we don't move relationships with lower ranks, which are supposedly well-positioned already
                    if (this._isShiftSizeAcceptable( affectedInfo, false, 5, 3) && affectedInfo.minAffectedRank > this.DG.ranks[v]) {
                        var nodes = affectedInfo.nodes;
                        //console.log("Middle-positioning relationship by [" + shiftSize + "]: " + Helpers.stringifyObject(nodes));
                        for (var i = 0; i < nodes.length; i++)
                            xcoord.xcoord[nodes[i]] += shiftSize;
                        improved = true;
                    }
                }
            }

            //xcoord.normalize();

            this.DG.positions = xcoord.xcoord;

            timer.printSinceLast("=== Improvement runtime: ");

            this.DG.vertLevel = this.DG.positionVertically();
            this.DG.rankY     = this.DG.computeRankY(ranksBefore, rankYBefore);

            timer.printSinceLast("=== Vertical spacing runtime: ");
        },

        _compactGraph: function( xcoord, maxComponentSize ) {
            // tries to shorten edges that can be shortened (thus compacting the graph)
            //
            // for each node checks if it has "slack" on the left and right, and iff slack > 0 computes
            // the disconnected components resulting from removal of all edges spanning the larger-than-necessary gap.
            // if components can be moved close to each other (because all nodes on the "edge" also have slack) does so.
            //
            // stops component computation when component size is greater than `maxComponentSize` (and does not move that component)
            // (for performance reasons: there is a small pass with a small value to fix minor imperfections before a lot
            // of other heuristics are applied, and then a pass with unlimited component size if performed at the end

            if (!maxComponentSize) maxComponentSize = Infinity;

            //console.log("---[maxCompSize: " + maxComponentSize  + "]---");

            var iter = 0;
            var improved = true;
            while (improved && iter < 20)
            {
                improved = false;
                iter++;

                // go rank-by-rank, node-by-node
                for (var rank = 1; rank < this.DG.order.order.length; rank++) {
                    for (var order = 0; order < this.DG.order.order[rank].length - 1; order++) {
                        var v = this.DG.order.order[rank][order];

                        if (this.DG.GG.isChildhub(v)) break; // skip childhub level entirely

                        var slack = xcoord.getSlackOnTheRight(v);
                        //console.log("V = " + v + ", slack: " + slack);
                        if (slack == 0) continue;

                        // so, v has some slack on the right
                        // let see if we can shorten the distance between v and its right neighbour (by shortening
                        // all edges spanning the gap between v and its right neighbour - without bumping any nodes
                        // connected by all other edges into each other)

                        var DG = this.DG;
                        var excludeEdgesSpanningOrder = function(from, to) {
                            // filter to exclude all edges spanning the gap between v and its right neighbour
                            if (DG.ranks[from] == rank && DG.ranks[to] == rank) {
                                var orderFrom = DG.order.vOrder[from];
                                var orderTo   = DG.order.vOrder[to];
                                if ((orderFrom <= order && orderTo   > order) ||
                                    (orderTo   <= order && orderFrom > order) ) {
                                    return false;
                                }
                            }
                            return true;
                        };

                        var rightNeighbour = this.DG.order.order[rank][order+1];

                        // either move V and nodes connected to V left, or rightNeighbour and nodes connected to it right
                        // (in both cases "connected" means "connected not using edges spanning V-rightNeighbour gap")
                        // If maxComponentSize is not limited, then no point to analize other component, since
                        var stopSet = {};
                        stopSet[rightNeighbour] = true;
                        var component = this.DG.findConnectedComponent(v, excludeEdgesSpanningOrder, stopSet, maxComponentSize );
                        var leftSide  = true;

                        if (component.stopSetReached) continue; // can't shorten here: nodes are firmly connected via other edges

                        if (component.size > maxComponentSize) {
                            // can't move component on the left - it is too big. Check the right side
                            component = this.DG.findConnectedComponent(rightNeighbour, excludeEdgesSpanningOrder, {}, maxComponentSize );
                            if (component.size > maxComponentSize) continue;  // can't move component on the right - too big as well
                            leftSide  = false;
                        }

                        slack = leftSide ? xcoord.findVertexSetSlacks(component.component).rightSlack // slack on the right side of left component
                                         : -xcoord.findVertexSetSlacks(component.component).leftSlack;

                        if (slack == 0) continue;
                        console.log("Moving: " + Helpers.stringifyObject(component.component) + " by " + slack);

                        improved = true;

                        for (var node in component.component) {
                            if (component.component.hasOwnProperty(node)) {
                                xcoord.xcoord[node] += slack;
                            }
                        }
                    }
                }

                if (!isFinite(maxComponentSize)) {
                    // after all other types of nodes have been moved check if childhub nodes need any movement as well
                    // this is similar to relationship-to-children positioning but is done globally not locally
                    for (var rank = 1; rank < this.DG.order.order.length; rank++) {
                        for (var order = 0; order < this.DG.order.order[rank].length; order++) {
                            var v = this.DG.order.order[rank][order];
                            if (this.DG.GG.isPerson(v)) break;        // wrong rank
                            if (this.DG.GG.isRelationship(v)) break;  // wrong rank
                            if (!this.DG.GG.isChildhub(v)) continue;  // childhub rank may have long edges in addition to childhubs

                            var childhubX = xcoord.xcoord[v];

                            var childInfo = this.analizeChildren(v);
                            var childPositionInfo = this._computeDesiredChildhubLocation( childInfo, xcoord );
                            if (childhubX >= childPositionInfo.leftX && childhubX <= childPositionInfo.rightX) continue;

                            var shiftChhub = (childhubX > childPositionInfo.maxPreferred) ?
                                             (childPositionInfo.maxPreferred - childhubX) :
                                             (childPositionInfo.minPreferred - childhubX);

                            // either move childhub and nodes connected to it towards the children, or children
                            // and nodes connected to it towards the childhub

                            var noChildEdges = function(from, to) {
                                if (from == v) return false;
                                return true;
                            };
                            var stopSet = Helpers.toObjectWithTrue(this.DG.GG.getOutEdges(v));
                            var component = this.DG.findConnectedComponent(v, noChildEdges, stopSet, Infinity );
                            if (component.stopSetReached) continue; // can't shorten here: nodes are firmly connected via other edges

                            var slack = (shiftChhub > 0) ? Math.min(shiftChhub, xcoord.findVertexSetSlacks(component.component).rightSlack) // slack on the right side of component
                                                         : Math.max(shiftChhub, -xcoord.findVertexSetSlacks(component.component).leftSlack);
                            if (slack == 0) continue;
                            console.log("Moving chhub: " + Helpers.stringifyObject(component.component) + " by " + slack);

                            improved = true;

                            for (var node in component.component) {
                                if (component.component.hasOwnProperty(node)) {
                                    xcoord.xcoord[node] += slack;
                                }
                            }
                        }
                    }
                }
            }
        },

        _findAffectedSet: function( v_list, dontmove_set, noUp_set, noDown_set, forbidden_set, shiftSize, xcoord, stopAtVirtual, minimizeMovement, stopAtPersons, stopAtRels, stopAtRank ) {
            // Given a list of nodes (v_list) and how much we want to move them (same amount for all the nodes, shiftSize)
            // figure out how many nodes would have to be moved to accomodate the desired movement.
            //
            // dontmove_set: nodes which should not be moved unless their neighbours push them.
            //
            // noUp_set: in-edges of nodes in the set willnot be followed when propagaitng movement
            //
            // noDown_set: out-edges of nodes in the set willnot be followed when propagaitng movement
            //
            // forbidden_set: if a node in the set has to move (due to any reason, e..g pushed by a neighbour or
            //                due to movement propagation in some other way) propagation stops and
            //                `forbiddenMoved` key is set to true in the return value
            //
            // stopAtVirtual: if `true` once a virtual node is found movement propagation is stopped
            //
            // stopAtPersons, stopAtRels: movement propagation stops once more than the given number of
            //                            persons/relationships has been added to the move set
            //
            // minimizeMovement: minimal propagation is used, and all nodes on the same rank opposite to the
            //                   movement direction are added to the dontmove_set

            var nodes = Helpers.toObjectWithTrue(v_list);

            var initialNodes = Helpers.toObjectWithTrue(v_list);

            // for each ignored node: add all nodes on the same rank which are to the left (if shifting right)
            // or to the right of (if shifting left) the node

            if (minimizeMovement) {
                for (var i = 0; i < v_list.length; i++) {
                    noUp_set  [v_list[i]] = true;
                    noDown_set[v_list[i]] = true;
                }
                for (var node in dontmove_set) {
                    if (dontmove_set.hasOwnProperty(node)) {
                        var rank  = this.DG.ranks[node];
                        var order = this.DG.order.vOrder[node];

                        var from  = (shiftSize > 0) ? 0     : order + 1;
                        var to    = (shiftSize > 0) ? order : this.DG.order.order[rank].length;

                        for (var i = from; i < to; i++) {
                            var u = this.DG.order.order[rank][i];
                            dontmove_set[u] = true;
                            noUp_set[u]     = true;
                            noDown_set[u]   = true;
                        }
                    }
                }
            }

            var numPersons     = 0;  // number of moved nodes (excluding nodes in the original list)
            var numRels        = 0;
            var numVirtual     = 0;
            var minRank        = Infinity;
            var forbiddenMoved = false;

            var toMove = new Queue();
            toMove.setTo(v_list);

            while(toMove.size() > 0) {
                if (stopAtPersons && numPersons > stopAtPersons) break;  // stop early and dont waste time if the caller already does not care
                if (stopAtRels    && numRels    > stopAtRels)    break;

                var nextV = toMove.pop();

                if (forbidden_set && forbidden_set.hasOwnProperty(nextV)) {
                    forbiddenMoved = true;
                    break;
                }

                if ( shiftSize > 0 ) {
                    var slack = xcoord.getSlackOnTheRight(nextV);
                    if (slack < shiftSize) {
                        var rightNeighbour = this.DG.order.getRightNeighbour(nextV, this.DG.ranks[nextV]);
                        if (!nodes.hasOwnProperty(rightNeighbour)) {
                            nodes[rightNeighbour] = true;
                            toMove.push(rightNeighbour);
                        }
                    }
                } else {
                    var slack = xcoord.getSlackOnTheLeft(nextV);
                    if (slack < -shiftSize) {
                        var leftNeighbour = this.DG.order.getLeftNeighbour(nextV, this.DG.ranks[nextV]);
                        if (!nodes.hasOwnProperty(leftNeighbour)) {
                            nodes[leftNeighbour] = true;
                            toMove.push(leftNeighbour);
                        }
                    }
                }

                // if we should ignore both in- and out-edges for the node - nothing else to do
                if (noUp_set.hasOwnProperty(nextV) && noDown_set.hasOwnProperty(nextV)) continue;

                if (this.DG.ranks[nextV] < minRank && !initialNodes.hasOwnProperty(nextV)) {
                    minRank = this.DG.ranks[nextV];
                    if (stopAtRank && minRank < stopAtRank) break;
                }

                if (this.DG.GG.isRelationship(nextV)) {
                    if (!initialNodes.hasOwnProperty(nextV)) numRels++;

                    var chhub = this.DG.GG.getOutEdges(nextV)[0];
                    if (!nodes.hasOwnProperty(chhub)) {
                        nodes[chhub] = true;
                        toMove.push(chhub);
                    }

                    if (minimizeMovement || noUp_set.hasOwnProperty(nextV)) continue;

                    var parents = this.DG.GG.getInEdges(nextV);
                    for (var i = 0; i < parents.length; i++) {
                        if (!dontmove_set.hasOwnProperty(parents[i]) && !nodes.hasOwnProperty(parents[i])) {
                            if ( (this.DG.order.vOrder[parents[i]] == this.DG.order.vOrder[nextV] + 1 && shiftSize < 0) ||  // if shiftSize > 0 it will get pushed anyway
                                 (this.DG.order.vOrder[parents[i]] == this.DG.order.vOrder[nextV] - 1 && shiftSize > 0)) {
                                nodes[parents[i]] = true;
                                toMove.push(parents[i]);
                            }
                        }
                    }
                }
                else
                if (this.DG.GG.isChildhub(nextV)) {
                    var rel = this.DG.GG.getInEdges(nextV)[0];
                    if (!nodes.hasOwnProperty(rel)) {
                        nodes[rel] = true;
                        toMove.push(rel);
                    }

                    if (minimizeMovement || noDown_set.hasOwnProperty(nextV)) continue;

                    // move children as not to break the supposedly nice layout
                    var childInfo    = this.analizeChildren(nextV);
                    var positionInfo = this._computeDesiredChildhubLocation( childInfo, xcoord, nodes, shiftSize );

                    // no need to move anything else when parent line is either above the mid-point between the leftmost and rightmost child
                    // or above the middle child of the three
                    var childhubX        = xcoord.xcoord[nextV];
                    var shiftedChildhubX = childhubX + shiftSize;
                    if (shiftedChildhubX == positionInfo.minPreferredWithShift || shiftedChildhubX == positionInfo.maxPreferredWithShift) continue;

                    // if we improve compared to what was before - also accept
                    if (childhubX < positionInfo.minPreferredWithShift && shiftSize > 0 && shiftedChildhubX < positionInfo.minPreferredWithShift) continue;
                    if (childhubX > positionInfo.maxPreferredWithShift && shiftSize < 0 && shiftedChildhubX > positionInfo.maxPreferredWithShift) continue;

                    var children = this.DG.GG.getOutEdges(nextV);
                    for (var j = 0; j  < children.length; j++) {
                        if (!dontmove_set.hasOwnProperty(children[j]) && !nodes.hasOwnProperty(children[j])) {
                            nodes[children[j]] = true;
                            toMove.push(children[j]);
                        }
                    }
                }
                else
                if (this.DG.GG.isPerson(nextV)) {
                    if (!initialNodes.hasOwnProperty(nextV) && !this.DG.GG.isPlaceholder(nextV)) numPersons++;

                    if (!noDown_set.hasOwnProperty(nextV)) {
                        var rels = this.DG.GG.getOutEdges(nextV);
                        for (var j = 0; j < rels.length; j++) {
                            if (!dontmove_set.hasOwnProperty(rels[j]) && !nodes.hasOwnProperty(rels[j])) {
                                if ( (this.DG.order.vOrder[rels[j]] == this.DG.order.vOrder[nextV] + 1 && shiftSize < 0) ||  // if shiftSize > 0 it will get pushed anyway
                                     (this.DG.order.vOrder[rels[j]] == this.DG.order.vOrder[nextV] - 1 && shiftSize > 0)) {

                                     // if there is already a long edge it is ok to make it longer. if not, try to keep stuff compact
                                     if ((shiftSize > 0 && xcoord.getSlackOnTheLeft(nextV) == 0) ||
                                         (shiftSize < 0 && xcoord.getSlackOnTheRight(nextV) == 0)) {
                                        nodes[rels[j]] = true;
                                        toMove.push(rels[j]);
                                     }
                                }
                            }
                        }
                    }

                    // move twins
                    var twins = this.DG.GG.getAllTwinsOf(nextV);
                    if (twins.length > 1) {
                        for (var t = 0; t < twins.length; t++) {
                            var twin = twins[t];
                            if (dontmove_set.hasOwnProperty(twin) || nodes.hasOwnProperty(twin)) continue;
                            toMove.push(twin);
                        }
                    }

                    //if (noUp_set.hasOwnProperty(nextV) || this.DG.GG.isPlaceholder(nextV)) continue;
                    if (noUp_set.hasOwnProperty(nextV)) continue;

                    // TODO: commenting out the piece below produces generally better layout, but for some reason
                    //       adds too much space in some cases, e.g. check testcase 4F (or MS_004, where layout
                    //       is better, but node "w1" is moved when it should not be
                    var inEdges = this.DG.GG.getInEdges(nextV);
                    if (inEdges.length > 0) {
                        var chhub = inEdges[0];

                        // check if we should even try to move chhub
                        if (dontmove_set.hasOwnProperty(chhub) || nodes.hasOwnProperty(chhub)) continue;

                        var childInfo    = this.analizeChildren(chhub);
                        var positionInfo = this._computeDesiredChildhubLocation( childInfo, xcoord, nodes, shiftSize );
                        var childhubX    = xcoord.xcoord[chhub];
                        // if it will become OK - no move
                        if (childhubX == positionInfo.minPreferredWithShift || childhubX == positionInfo.maxPreferredWithShift) continue;
                        // if we improve compared to what was before - also accept
                        if (childhubX < positionInfo.minPreferred && shiftSize < 0 && childhubX < positionInfo.minPreferredWithShift) continue;
                        if (childhubX > positionInfo.maxPreferred && shiftSize > 0 && childhubX > positionInfo.maxPreferredWithShift) continue;

                        nodes[chhub] = true;
                        toMove.push(chhub);
                    }/**/
                }
                else
                if (this.DG.GG.isVirtual(nextV)) {
                    if (!initialNodes.hasOwnProperty(nextV)) numVirtual++;

                    if (stopAtVirtual && numVirtual > 0) break;

                    if (!noUp_set.hasOwnProperty(nextV)) {
                        var v1 = this.DG.GG.getInEdges(nextV)[0];
                        if (!this.DG.GG.isPerson(v1) && !nodes.hasOwnProperty(v1) && !dontmove_set.hasOwnProperty(v1)) {
                            nodes[v1] = true;
                            toMove.push(v1);
                        }
                    }
                    if (!noDown_set.hasOwnProperty(nextV)) {
                        var v2 = this.DG.GG.getOutEdges(nextV)[0];
                        if (!this.DG.GG.isRelationship(v2) && !nodes.hasOwnProperty(v2) && !dontmove_set.hasOwnProperty(v2)) {
                            nodes[v2] = true;
                            toMove.push(v2);
                        }
                    }
                }
            }

            var affectedNodes = [];
            for (var node in nodes) {
                if (nodes.hasOwnProperty(node)) {
                    affectedNodes.push(node);
                }
            }
            return { "nodes": affectedNodes, "numPersons": numPersons, "numRelationships": numRels, "numVirtual": numVirtual,
                     "minAffectedRank": minRank, "forbiddenMoved": forbiddenMoved };
        },

        _computeDesiredChildhubLocation: function( childInfo, xcoord, nodesThatShift, shiftSize )
        {
            var leftMost  = childInfo.leftMostChildId;
            var rightMost = childInfo.rightMostChildId;

            var leftX  = xcoord.xcoord[leftMost];
            var rightX = xcoord.xcoord[rightMost];
            var middle = (leftX + rightX)/2;
            var median = (childInfo.orderedChildren.length == 3) ? xcoord.xcoord[childInfo.orderedChildren[1]] : middle;
            var minIntervalX = Math.min(middle, median);
            var maxIntervalX = Math.max(middle, median);

            var result = {"leftX": leftX, "rightX": rightX, "middle": middle, "median": median,
                          "minPreferred": minIntervalX, "maxPreferred": maxIntervalX };

            if (nodesThatShift) {
                var leftXShifted  = leftX  + (nodesThatShift.hasOwnProperty(leftMost)  ? shiftSize : 0);
                var rightXShifted = rightX + (nodesThatShift.hasOwnProperty(rightMost) ? shiftSize : 0);
                var middleShifted = (leftXShifted + rightXShifted)/2;
                var medianShifted = (childInfo.orderedChildren.length == 3)
                                    ? (xcoord.xcoord[childInfo.orderedChildren[1]] + (nodesThatShift.hasOwnProperty(childInfo.orderedChildren[1]) ? shiftSize : 0))
                                    : middleShifted;
                var minIntervalXShifted = Math.min(middleShifted, medianShifted);
                var maxIntervalXShifted = Math.max(middleShifted, medianShifted);

                result["minPreferredWithShift"] = minIntervalXShifted;
                result["maxPreferredWithShift"] = maxIntervalXShifted;
            }

            return result;
        },

        //=============================================================
        optimizeLongEdgePlacement: function()
        {
            // 1) decrease the number of crossed edges
            // TODO

            // 2) straighten long edges
            var xcoord = new XCoord(this.DG.positions, this.DG);
            //this.DG.displayGraph(xcoord.xcoord, "pre-long-improve");

            var longEdges = this.DG.find_long_edges();
            this.DG.try_straighten_long_edges(longEdges, xcoord);   // does so without moving other nodes

            var stillNotStraight = this.straighten_long_edges(longEdges, xcoord);   // attempts to straigthen more agressively

            this.DG.try_straighten_long_edges(stillNotStraight, xcoord);

            //this.DG.displayGraph(xcoord.xcoord, "past-long-improve");
            this.DG.positions = xcoord.xcoord;
        },

        // Straigthen edges more agressively that DG.try_straighten_long_edges(), willing to move
        // some nodes to make long edges look better (as when they don't, it looks more ugly than a regular non-straight edge)
        straighten_long_edges: function( longEdges, xcoord )
        {
            var stillNotStraight = [];

            for (var e = 0; e < longEdges.length; e++) {
                var chain = longEdges[e];
                //this.DG.displayGraph(xcoord.xcoord, "pre-straighten-"+Helpers.stringifyObject(chain));
                //console.log("trying to force-straighten edge " + Helpers.stringifyObject(chain));

                //var person = this.DG.GG.getInEdges(chain[0])[0];
                do {
                    var improved = false;
                    var headCenter = xcoord.xcoord[chain[0]];
                    // go over all nodes from head to tail looking for a bend
                    for (var i = 1; i < chain.length; i++) {
                        var nextV      = chain[i];
                        var nextCenter = xcoord.xcoord[nextV];
                        if (nextCenter != headCenter) {
                            // try to shift either the head or the tail of the edge, if the amount of movement is not too big
                            var head = chain.slice(0, i);
                            var tail = chain.slice(i);

                            var shiftHeadSize = nextCenter - headCenter;
                            var dontmove      = Helpers.toObjectWithTrue(tail);
                            var affectedInfoHeadShift = this._findAffectedSet(head, dontmove, {}, {}, {}, shiftHeadSize, xcoord, true, true, 5, 3);

                            var shiftTailSize = headCenter - nextCenter;
                            var dontmove      = Helpers.toObjectWithTrue(head);
                            var affectedInfoTailShift = this._findAffectedSet(tail, dontmove, {}, {}, {}, shiftTailSize, xcoord, true, true, 5, 3);

                            if (!this._isShiftSizeAcceptable( affectedInfoHeadShift, false, 5, 3) &&
                                !this._isShiftSizeAcceptable( affectedInfoTailShift, false, 5, 3) ) {
                                stillNotStraight.push(chain);
                                break;  // too much distortion and/or distorting other virtual edges
                            }

                            improved = true;   // at least one of the shifts is OK

                            // ok, pick which one to use
                            if ( this._isShiftBetter(affectedInfoTailShift, affectedInfoHeadShift) ) {
                                // use tail shift
                                var nodes = affectedInfoTailShift.nodes;
                                for (var i = 0; i < nodes.length; i++)
                                    xcoord.xcoord[nodes[i]] += shiftTailSize;
                            } else {
                                // use head shift
                                var nodes = affectedInfoHeadShift.nodes;
                                for (var i = 0; i < nodes.length; i++)
                                    xcoord.xcoord[nodes[i]] += shiftHeadSize;
                            }
                            break;
                        }
                    }
                } while(improved);
            }

            return stillNotStraight;
        },

        _isShiftSizeAcceptable: function( shiftInfo, allowShiftVirtual, maxPersonNodes, maxRelNodes )
        {
            if (shiftInfo.forbiddenMoved) return false;
            if (!allowShiftVirtual && shiftInfo.numVirtual > 0) return false;
            if (shiftInfo.numPersons > maxPersonNodes) return false;
            if (shiftInfo.numRelationships > maxRelNodes) return false;
            return true;
        },

        _isShiftBetter: function( shiftInfo1, shiftInfo2 )
        {
            // the one shifting less virtual nodes is better
            if (shiftInfo2.numVirtual > shiftInfo1.numVirtual) return true;
            if (shiftInfo2.numVirtual < shiftInfo1.numVirtual) return false;

            // the one shifting fewer person nodes is better
            if (shiftInfo2.numPersons > shiftInfo1.numPersons) return true;
            if (shiftInfo2.numPersons < shiftInfo1.numPersons) return false;

            // the one shifting fewer rel nodes (and everything else being equal) is better
            if (shiftInfo2.numRelationships > shiftInfo1.numRelationships) return true;
            return false;
        },
        //=============================================================

        moveToCorrectPositionAndMoveOtherNodesAsNecessary: function ( newNodeId, nodeToKeepEdgeStraightTo )
        {
            // Algorithm:
            //
            // Initially pick the new position for "newNodeId," which keeps the edge to node-"nodeToKeepEdgeStraightTo"
            // as straight as possible while not moving any nodes to the left of "newNodeId" in the current ordering.
            //
            // This new position may force the node next in the ordering to move further right to make space, in
            // which case that node is added to the queue and then the following heuristic is applied:
            //  while queue is not empty:
            //
            //  - pop a node from the queue and move it right just enough to have the desired spacing between the node
            //    and it's left neighbour. Check which nodes were affected because of this move:
            //    nodes to the right, parents & children. Shift those affected accordingly (see below) and add them to the queue.
            //
            //    The rules are:
            //    a) generally all shifted nodes will be shifted the same amount to keep the shape of
            //       the graph as unmodified as possible, with a few exception below
            //    b) all childhubs should stay right below their relationship nodes
            //    c) childhubs wont be shifted while they ramain between the leftmost and rightmost child
            //    d) when a part of the graph needs to be stretched prefer to strech relationship edges
            //       to the right of relationship node. Some of the heuristics below assume that this is the
            //       part that may have been stretched
            //
            // note: does not assert the graph satisfies all the assumptions in BaseGraph.validate(),
            //       in particular this can be called after a childhub was added but before it's relationship was added

            //console.log("Orders: " + Helpers.stringifyObject(this.DG.order));
            console.log("========== PLACING " + newNodeId);

            var originalDisturbRank = this.DG.ranks[newNodeId];

            var xcoord = new XCoord(this.DG.positions, this.DG);

            //console.log("Orders at insertion rank: " + Helpers.stringifyObject(this.DG.order.order[this.DG.ranks[newNodeId]]));
            //console.log("Positions of nodes: " + Helpers.stringifyObject(xcoord.xcoord));

            var leftBoundary  = xcoord.getLeftMostNoDisturbPosition(newNodeId);
            var rightBoundary = xcoord.getRightMostNoDisturbPosition(newNodeId);

            var desiredPosition = this.DG.positions[nodeToKeepEdgeStraightTo];             // insert right above or right below

            if (nodeToKeepEdgeStraightTo != newNodeId) {
                if (this.DG.ranks[nodeToKeepEdgeStraightTo] == originalDisturbRank) {     // insert on the same rank: then instead ot the left or to the right
                    if (this.DG.order.vOrder[newNodeId] > this.DG.order.vOrder[nodeToKeepEdgeStraightTo])
                        desiredPosition = xcoord.getRightEdge(nodeToKeepEdgeStraightTo) + xcoord.getSeparation(newNodeId, nodeToKeepEdgeStraightTo) + xcoord.halfWidth[newNodeId];
                    else {
                        desiredPosition = xcoord.getLeftEdge(nodeToKeepEdgeStraightTo) - xcoord.getSeparation(newNodeId, nodeToKeepEdgeStraightTo) - xcoord.halfWidth[newNodeId];
                        if (desiredPosition > rightBoundary)
                            desiredPosition = rightBoundary;
                    }
                }
                else if (this.DG.GG.isPerson(newNodeId) && desiredPosition > rightBoundary)
                    desiredPosition = rightBoundary;
            }

            var insertPosition = ( desiredPosition < leftBoundary ) ? leftBoundary : desiredPosition;

            xcoord.xcoord[newNodeId] = insertPosition;

            var shiftAmount = 0;
            if (insertPosition > desiredPosition)
                shiftAmount = (insertPosition - desiredPosition);

            // find which nodes we need to shift to accomodate this insertion via "domino effect"

            // each entry in the queue is a pair [node, moveAmount]. Once a node is popped from the queue
            // some of the linked nodes are moved the same amount to keep the existing shape of the graph.
            // That movement may in turn trigger anothe rmovement of the original node, so the same
            // node may appear more than once in the queue at the same itme, with different corresponding
            // move amounts. In theory there should be no circular dependencies (e.g. moving A requires moving B
            // which requires moving A again), but in case there is a mistake there is a check which terminates
            // the process after some time.
            var disturbedNodes = new Queue();
            disturbedNodes.push([newNodeId, shiftAmount]);

            var iterOuter = 0;
            var iter      = 0;

            var doNotTouch = {};
            var ancestors  = this.DG.GG.getAllAncestors(newNodeId);
            for (var node in ancestors) {
                doNotTouch[node] = true;
                var rank  = this.DG.ranks[node];
                var order = this.DG.order.vOrder[node];
                for (var i = 0; i < order; i++) {
                    var u = this.DG.order.order[rank][i];
                    doNotTouch[u] = true;
                }
            }
            //console.log("V:" + newNodeId + " -> DoNotTouch: " + Helpers.stringifyObject(doNotTouch));

            var totalMove = {};   // for each node: how much the node has been moved by this function

            // The movement algorithm is two-step:
            //  1) first nodes "firmly" linked to each other are moved in the inner while loop
            //  2) then some childhubs get moved depending on which of their children have been shifted -
            //     which may trigger move moves in the inner loop

            // Outer loop. Repeat at most 5 times, as more is likely a run-away algo due to some unexpected circular dependency
            while ( disturbedNodes.size() > 0 && iterOuter < 5 ) {
                iterOuter++;

                var childrenMoved = {};   // for each childhub: which children have been moved (we only move a chldhub if all its children were moved)

                var numNodes             = this.DG.ranks.length;
                var maxExpectedMovements = numNodes*5;

                // inner loop: shift all vertices except childhubs, which only shift if all their children shift
                while ( disturbedNodes.size() > 0 && iter < maxExpectedMovements) {
                    iter++;  // prevent unexpected run-away due to some weird circular dependency (should not happen but TODO: check)

                    //console.log("Disturbed nodes: " + Helpers.stringifyObject(disturbedNodes.data));

                    var next     = disturbedNodes.pop();
                    var v        = next[0];
                    shiftAmount  = next[1];

                    //console.log("Processing: " + v + " @position = " + xcoord.xcoord[v]);

                    var type   = this.DG.GG.type[v];
                    var vrank  = this.DG.ranks[v];
                    var vorder = this.DG.order.vOrder[v];

                    var position    = xcoord.xcoord[v];
                    var rightMostOK = xcoord.getRightMostNoDisturbPosition(v);

                    if (position > rightMostOK) {
                        // the node to the right was disturbed: shift it
                        var rightDisturbed = this.DG.order.order[vrank][vorder+1];

                        var toMove = position - rightMostOK;

                        xcoord.xcoord[rightDisturbed] += toMove;
                        totalMove[rightDisturbed]      = totalMove.hasOwnProperty(rightDisturbed) ? totalMove[rightDisturbed] + toMove : toMove;
                        disturbedNodes.push([rightDisturbed, toMove]);
                        //console.log("addRNK: " + rightDisturbed + " (toMove: " + toMove + " -> " + xcoord.xcoord[rightDisturbed] + ")");
                    }

                    if (v == newNodeId && type != BaseGraph.TYPE.VIRTUALEDGE) continue;

                    //if (type == BaseGraph.TYPE.VIRTUALEDGE && rank > 2) continue; // TODO: DEBUG: remove - needed for testing of edge-straightening algo

                    var inEdges  = this.DG.GG.getInEdges(v);
                    var outEdges = this.DG.GG.getOutEdges(v);

                    // go though out- and in- edges and propagate the movement
                    //---------
                    var skipInEdges = false;
                    if ((type == BaseGraph.TYPE.PERSON || type == BaseGraph.TYPE.VIRTUALEDGE) && v == newNodeId) {
                        skipInEdges = true;
                    }
                    if (type == BaseGraph.TYPE.VIRTUALEDGE) {
                        var inEdgeV = inEdges[0];
                        if (this.DG.ranks[inEdgeV] == vrank)
                            skipInEdges = true;
                    }
                    // if we need to strech something -> stretch relationship edges to the right of
                    if (type == BaseGraph.TYPE.RELATIONSHIP) {
                        skipInEdges = true;
                        // except the case when inedge comes from a vertex to the left with no other in- or out-edges (a node connected only to this reltionship)
                        if (inEdges.length == 2) {
                            var parent0 = inEdges[0];
                            var parent1 = inEdges[1];
                            var order0  = this.DG.order.vOrder[parent0];
                            var order1  = this.DG.order.vOrder[parent1];
                            if (order0 == vorder-1 && this.DG.GG.getOutEdges(parent0).length == 1 &&
                                this.DG.GG.getInEdges(parent0).length == 0 &&
                                !doNotTouch.hasOwnProperty(parent0) ) {
                                if (!totalMove.hasOwnProperty(parent0) || totalMove[parent0] < totalMove[v]) {
                                    xcoord.xcoord[parent0] += shiftAmount;  // note: we can avoid adding this node to any queues as it is only connected to v
                                    totalMove[parent0]      = totalMove.hasOwnProperty(parent0) ? totalMove[parent0] + shiftAmount : shiftAmount;
                                }
                            }
                            else if (order1 == vorder-1 && this.DG.GG.getOutEdges(parent1).length == 1 &&
                                     this.DG.GG.getInEdges(parent1).length == 0 &&
                                     !doNotTouch.hasOwnProperty(parent1)) {
                                if (!totalMove.hasOwnProperty(parent1) || totalMove[parent1] < totalMove[v]) {
                                    xcoord.xcoord[parent1] += shiftAmount;  // note: we can avoid adding this node to any queues as it is only connected to v
                                    totalMove[parent1]      = totalMove.hasOwnProperty(parent1) ? totalMove[parent1] + shiftAmount : shiftAmount;
                                }
                            }
                        }
                    }

                    if (!skipInEdges) {
                        for (var i = 0; i < inEdges.length; i++) {
                            var u     = inEdges[i];
                            var typeU = this.DG.GG.type[u];

                            if (doNotTouch.hasOwnProperty(u)) continue;
                            if (totalMove.hasOwnProperty(u) && totalMove[u] >= totalMove[v]) continue;

                            if (type == BaseGraph.TYPE.PERSON && typeU == BaseGraph.TYPE.CHILDHUB) {
                                if (childrenMoved.hasOwnProperty(u)) {
                                    childrenMoved[u]++;
                                }
                                else {
                                    childrenMoved[u] = 1;
                                }

                                continue;
                            }

                            if (typeU == BaseGraph.TYPE.VIRTUALEDGE && xcoord.xcoord[u] == xcoord.xcoord[v]) continue;

                            var shiftU = totalMove.hasOwnProperty(u) ? Math.min(shiftAmount, Math.max(totalMove[v] - totalMove[u], 0)) : shiftAmount;

                            xcoord.xcoord[u] += shiftU;
                            totalMove[u]      = totalMove.hasOwnProperty(u) ? totalMove[u] + shiftU : shiftU;
                            disturbedNodes.push([u, shiftU]);
                            //console.log("addINN: " + u + " (shift: " + shiftU + " -> " + xcoord.xcoord[u] + ")");
                        }
                    }
                    //---------

                    //---------
                    if (type == BaseGraph.TYPE.CHILDHUB) {
                        var rightMostChildPos = 0;
                        for (var i = 0; i < outEdges.length; i++) {
                            var u   = outEdges[i];
                            var pos = xcoord.xcoord[u];
                            if (pos > rightMostChildPos)
                                rightMostChildPos = pos;
                        }
                        if (rightMostChildPos >= xcoord.xcoord[v]) continue; // do not shift children if we are not creating a "bend"
                    }

                    for (var i = 0; i < outEdges.length; i++) {
                        var u = outEdges[i];

                        var shiftU = totalMove.hasOwnProperty(u) ? Math.min(shiftAmount, Math.max(totalMove[v] - totalMove[u], 0)) : shiftAmount;

                        if (doNotTouch.hasOwnProperty(u)) continue;
                        if (totalMove.hasOwnProperty(u) && totalMove[u] >= totalMove[v]) continue;

                        if ( this.DG.ranks[u] == vrank ) continue;   // vertices on the same rank will only be shifted if pushed on the right by left neighbours

                        if (type == BaseGraph.TYPE.RELATIONSHIP || type == BaseGraph.TYPE.VIRTUALEDGE) {
                            var diff = xcoord.xcoord[v] - xcoord.xcoord[u];
                            if (diff <= 0) continue;
                            if (diff < shiftU)
                                shiftU = diff;
                        }

                        xcoord.xcoord[u] += shiftU;
                        totalMove[u]      = totalMove.hasOwnProperty(u) ? totalMove[u] + shiftU : shiftU;
                        disturbedNodes.push([u, shiftU]);
                        //console.log("addOUT: " + u + " (shift: " + shiftU + " -> " + xcoord.xcoord[u] + ")");
                    }
                    //---------
                }


                // small loop 2: shift childhubs, if necessary
                for (var chhub in childrenMoved) {
                    if (childrenMoved.hasOwnProperty(chhub)) {
                        chhub = parseInt(chhub);
                        if (doNotTouch.hasOwnProperty(chhub)) continue;
                        var children = this.DG.GG.getOutEdges(chhub);
                        //if (children.length == 1 && this.DG.GG.isPlaceholder(children[0])) {
                        //    continue;
                        //}
                        if (children.length > 0 && children.length == childrenMoved[chhub]) {
                            var minShift = Infinity;
                            for (var j = 0; j < children.length; j++) {
                                if (totalMove[children[j]] < minShift)
                                    minShift = totalMove[children[j]];
                            }
                            if (totalMove.hasOwnProperty(chhub)) {
                                if (totalMove[chhub] > minShift) continue;
                                minShift -= totalMove[chhub];
                            }
                            xcoord.xcoord[chhub] += minShift;
                            totalMove[chhub]      = totalMove.hasOwnProperty(chhub) ? totalMove[chhub] + minShift : minShift;
                            disturbedNodes.push([chhub, minShift]);
                            //console.log("childhub: " + chhub + " (shift: " + minShift + ")");
                        }
                    }
                }

            } // big outer while()

            //this.DG.displayGraph(xcoord.xcoord, "after-insert-"+newNodeId);

            this.DG.positions = xcoord.xcoord;

            //console.log("Positions: 5-6-7: " + this.DG.positions[5] + " / " + this.DG.positions[6] + " / " + this.DG.positions[7]);
            console.log("PLACED/MOVED: " + newNodeId + " @ position " + this.DG.positions[newNodeId]);
        }
    };

    return DynamicPositionedGraph;
});
