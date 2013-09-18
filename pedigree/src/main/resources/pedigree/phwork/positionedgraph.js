PositionedGraph = function( drawGraph )
{
    this.DG = drawGraph;

    this._heuristics = new Heuristics( drawGraph );  // heuristics & helper methods separated into a separate class

    this._heuristics.improvePositioning();
};

PositionedGraph.prototype = {

    isValidID: function( id )
    {
      if (id < 0 || id > this.DG.GG.getMaxRealVertexId())
        return false;
      if (!this.DG.GG.isPerson(id) && !this.DG.GG.isRelationship(id))
        return false;
      return true;
    },

    isPerson: function( id )
    {
        return this.DG.GG.isPerson(id);
    },

    isRelationship: function( id )
    {
        return this.DG.GG.isRelationship(id);
    },

    getProperties: function( id )
    {
        return this.DG.GG.properties[id];
    },

    getPosition: function( v )
    {
        // returns coordinates of node v
        var x = this.DG.positions[v];

        var rank = this.DG.ranks[v];

        var vertLevel = this.DG.GG.isChildhub(v) ? this.DG.vertLevel.childEdgeLevel[v] : 1;

        var y = this.DG.computeNodeY(rank, vertLevel);

        /* TODO
        if (this.DG.GG.isRelationship(v)) {
            var partners = this.DG.GG.getInEdges(v);

            var level1 = this.DG.vertLevel.outEdgeVerticalLevel[partners[0]][v];
            var level2 = this.DG.vertLevel.outEdgeVerticalLevel[partners[1]][v];

            var y1 = this.DG.computeNodeY(rank, level1);
            var y2 = this.DG.computeNodeY(rank, level2);

            var edgeLevels = {};
            edgeLevels[partners[0]] = y1;
            edgeLevels[partners[1]] = y2;
            return {"x": x, "y": y, "partners": edgeLevels};
        }
        */

        return {"x": x, "y": y};
    },

    getRelationshipChildhubPosition: function( v )
    {
        if (!this.DG.GG.isRelationship(v))
            throw "Assertion failed: getRelationshipChildhubPosition() is applied to a non-relationship";

        var childhubId = this.DG.GG.getRelationshipChildhub(v);

        return this.getPosition(childhubId);
    },

    getRelationshipChildren: function( v )
    {
        if (!this.DG.GG.isRelationship(v))
            throw "Assertion failed: getRelationshipChildren() is applied to a non-relationship";

        var childhubId = this.DG.GG.getRelationshipChildhub(v);

        return this.DG.GG.getOutEdges(childhubId);
    },
    
    getParentRelationship: function( v )
    {
        if (!this.DG.GG.isPerson(v))
            throw "Assertion failed: getParentRelationship() is applied to a non-person";
        
        return this.DG.GG.getProducingRelationship(v);
    },

    getPossibleChildrenOf: function( v )
    {
        // all person nodes which are not ancestors of v and which do not already have parents
        var result = [];

        for (var i = 0; i <= this.DG.GG.getMaxRealVertexId(); i++) {
           if (!this.DG.GG.isPerson(i)) continue;
           if (this.DG.GG.inedges[i].length != 0) continue;
           if (this.DG.ancestors[v].hasOwnProperty(i)) continue;
           result.push(i);
        }

        return result;
    },

    getPossibleParentsOf: function( v )
    {
        // all person nodes which are not descendants of source node
        var result = [];

        console.log("Ancestors: " + stringifyObject(this.DG.ancestors));
        for (var i = 0; i <= this.DG.GG.maxRealVertexId; i++) {
           if (!this.DG.GG.isRelationship(i) && !this.DG.GG.isPerson(i)) continue;
           if (this.DG.ancestors[i].hasOwnProperty(v)) continue;
           result.push(i);
        }

        return result;
    },

    getAllPersonsOfGenders: function (validGendersSet)
    {
        // all person nodes whose gender matches one of genders in the validGendersSet array

        // validate input genders
        for (var i = 0; i < validGendersSet.length; i++) {
            validGendersSet[i] = validGendersSet[i].toLowerCase();
            if (validGendersSet[i] != 'u' && validGendersSet[i] != 'm' && validGendersSet[i] != 'f')
                throw "Invalid gender: " + validGendersSet[i];
        }

         var result = [];

         for (var i = 0; i <= this.DG.GG.maxRealVertexId; i++) {
            if (!this.DG.GG.isPerson(i)) continue;
            var gender = this.DG.GG.properties[i]["gender"].toLowerCase();
            //console.log("trying: " + i + ", gender: " + gender + ", validSet: " + stringifyObject(validGendersSet));
            if (arrayContains(validGendersSet, gender))
                result.push(i);
         }

         return result;
    },

    _debugPrintAll: function( headerMessage )
    {
        console.log("========== " + headerMessage + " ==========");
        console.log("== GG:");
        console.log(stringifyObject(this.DG.GG));
        console.log("== Ranks:");
        console.log(stringifyObject(this.DG.ranks));
        console.log("== Orders:");
        console.log(stringifyObject(this.DG.order));
        //console.log("== Positions:");
        //console.log(stringifyObject(this.DG.positions));
        //console.log("== RankY:");
        //console.log(stringifyObject(this.DG.rankY));
    },

    addNewChild: function( childhubId, properties )
    {
        this._debugPrintAll("before");
        var timer = new Timer();

        if (!this.DG.GG.isChildhub(childhubId)) {
            if (this.DG.GG.isRelationship(childhubId))
                childhubId = this.DG.GG.getRelationshipChildhub(childhubId);
            else
                throw "Assertion failed: adding children to a non-childhub node";
        }

        var positionsBefore  = this.DG.positions.slice(0);
        var ranksBefore      = this.DG.ranks.slice(0);
        var vertLevelsBefore = this.DG.vertLevel.copy();
        var rankYBefore      = this.DG.rankY;

        if (!properties) properties = {};

        var insertRank = this.DG.ranks[childhubId] + 1;

        // find the best order to use for this new vertex: scan all orders on the rank, check number of crossed edges
        var insertOrder = this._findBestInsertPosition( insertRank, childhubId );

        // insert the vertex into the base graph
        var newNodeId = this.DG.GG.insertVertex("", TYPE.PERSON, properties, 1.0, [childhubId], []);  // for debug use name = "_" + (this.DG.GG.getMaxRealVertexId()+1)

        // update GG, ranks, orders and positions - should make all those structures consistent
        // (once those are updated, regular DrawGraph methods - which may assume consistency - can be used to update the rest of the graph structures)
        this._updateGGRankOrderPositionsAfterNodeChange( newNodeId, insertRank, insertOrder, childhubId );

        // validate: by now the graph should satisfy all assumptions
        this.DG.GG.validate();

        // TODO
        //this.transpose

        // fix common layout mistakes (e.g. relationship not right above the only child)
        this._heuristics.improvePositioning();

        // update vertical separation for all nodes & compute ancestors
        this._updateauxiliaryStructures(ranksBefore, rankYBefore);

        timer.printSinceLast("=== AddChild runtime: ");
        this._debugPrintAll("after");

        this.DG.displayGraph(this.DG.positions, "zz");

        var movedNodes = this._findMovedNodes( positionsBefore, ranksBefore, vertLevelsBefore, rankYBefore );
        var relationshipId = this.DG.GG.getInEdges(childhubId)[0];
        if (!arrayContains(movedNodes,relationshipId))
            movedNodes.push(relationshipId);
        var animateNodes = this.DG.GG.getInEdges(relationshipId);  // animate parents if they move. if not, nothing will be done with them
        var newNodes   = [newNodeId];
        return {"new": newNodes, "moved": movedNodes, "animate": animateNodes};
    },

    addNewParents: function( personId )
    {
        this._debugPrintAll("before");
        var timer = new Timer();

        if (!this.DG.GG.isPerson(personId))
            throw "Assertion failed: adding parents to a non-person node";

        if (this.DG.GG.getInEdges(personId).length > 0)
            throw "Assertion failed: adding parents to a person with parents";

        var positionsBefore  = this.DG.positions.slice(0);
        var ranksBefore      = this.DG.ranks.slice(0);
        var vertLevelsBefore = this.DG.vertLevel.copy();
        var rankYBefore      = this.DG.rankY;

        // a few special cases which involve not only insertions but also existing node rearrangements:
        this._heuristics.swapBeforeParentsToBringToSideIfPossible( personId );

        var insertChildhubRank = this.DG.ranks[personId] - 1;

        // find the best order to use for this new vertex: scan all orders on the rank, check number of crossed edges
        var insertChildhubOrder = this._findBestInsertPosition( insertChildhubRank, personId );

        // insert the vertex into the base graph
        var newChildhubId = this.DG.GG.insertVertex("", TYPE.CHILDHUB, {}, 1.0, [], [personId]);  // for debug use name "_chhub" + (this.DG.GG.getMaxRealVertexId()+1)
        // update GG, ranks, orders and positions - should make all those structures consistent
        // (once those are updated, regular DrawGraph methods - which may assume consistency - can be used to update the rest of the graph structures)
        this._updateGGRankOrderPositionsAfterNodeChange( newChildhubId, insertChildhubRank, insertChildhubOrder, personId );

        var insertParentsRank = this.DG.ranks[newChildhubId] - 1;   // note: rank may have changed since last insertion
                                                                    //       (iff childhub was insertion above all at rank 0 - which becomes rank1)

        // find the best order to use for this new vertex: scan all orders on the rank, check number of crossed edges
        var insertParentOrder = this._findBestInsertPosition( insertParentsRank, newChildhubId );

        var newRelationshipId = this.DG.GG.insertVertex("", TYPE.RELATIONSHIP, {}, 1.0, [], [newChildhubId]); // for debug use name "_rel" + (this.DG.GG.getMaxRealVertexId()+1)
        this._updateGGRankOrderPositionsAfterNodeChange( newRelationshipId, insertParentsRank, insertParentOrder, newChildhubId );

        insertParentsRank = this.DG.ranks[newRelationshipId];       // note: rank may have changed since last insertion again
                                                                    //       (iff relationship was insertion above all at rank 0 - which becomes rank1)

        var newParent1Id = this.DG.GG.insertVertex("", TYPE.PERSON, {"gender": "M"}, 1.0, [], [newRelationshipId]);  // for debug use name "_" + (this.DG.GG.getMaxRealVertexId()+1)
        this._updateGGRankOrderPositionsAfterNodeChange( newParent1Id, insertParentsRank, insertParentOrder + 1, newRelationshipId );

        var newParent2Id = this.DG.GG.insertVertex("", TYPE.PERSON, {"gender": "F"}, 1.0, [], [newRelationshipId]);  // for debug use name "_" + (this.DG.GG.getMaxRealVertexId()+1)
        this._updateGGRankOrderPositionsAfterNodeChange( newParent2Id, insertParentsRank, insertParentOrder, newRelationshipId );

        // validate: by now the graph should satisfy all assumptions
        this.DG.GG.validate();

        // fix common layout mistakes (e.g. relationship not right above the only child)
        this._heuristics.improvePositioning();

        // update vertical separation for all nodes & compute ancestors
        this._updateauxiliaryStructures(ranksBefore, rankYBefore);

        timer.printSinceLast("=== NewParents runtime: ");
        this._debugPrintAll("after");

        this.DG.displayGraph(this.DG.positions, "zz");

        var animateNodes = this.DG.GG.getAllPartners(personId);
        if (animateNodes.length == 1)  // only animate node partners if there is only one - ow it may get too confusing with a lot of stuff animating around
            animateNodes.push(personId);
        else
            animateNodes = [personId];
        var movedNodes = this._findMovedNodes( positionsBefore, ranksBefore, vertLevelsBefore, rankYBefore );
        var newNodes   = [newRelationshipId, newParent1Id, newParent2Id];
        return {"new": newNodes, "moved": movedNodes, "highlight": [personId], "animate": animateNodes};
    },

    addNewRelationship: function( personId, childProperties, preferLeft )
    {
        this._debugPrintAll("before");
        var timer = new Timer();

        if (!this.DG.GG.isPerson(personId))
            throw "Assertion failed: adding relationship to a non-person node";

        var positionsBefore  = this.DG.positions.slice(0);
        var ranksBefore      = this.DG.ranks.slice(0);
        var vertLevelsBefore = this.DG.vertLevel.copy();
        var rankYBefore      = this.DG.rankY;

        if (!childProperties) childProperties = {};

        var parentProperties = {};
        if (this.DG.GG.properties[personId]["gender"] != "U")
            parentProperties["gender"] = (this.DG.GG.properties[personId]["gender"] == "M") ? "F" : "M";

        var insertRank  = this.DG.ranks[personId];
        var personOrder = this.DG.order.vOrder[personId];

        // a few special cases which involve not only insertions but also existing node rearrangements:
        this._heuristics.swapPartnerToBringToSideIfPossible( personId );

        // find the best order to use for this new vertex: scan all orders on the rank, check number of crossed edges
        var insertOrder = this._findBestInsertPosition( insertRank, personId, preferLeft );

        var newRelationshipId = this.DG.GG.insertVertex("", TYPE.RELATIONSHIP, {}, 1.0, [personId], []);
        this._updateGGRankOrderPositionsAfterNodeChange( newRelationshipId, insertRank, insertOrder, personId );

        var insertPersonOrder = (insertOrder > personOrder) ? insertOrder + 1 : insertOrder;

        var newPersonId = this.DG.GG.insertVertex("", TYPE.PERSON, parentProperties, 1.0, [], [newRelationshipId]);
        this._updateGGRankOrderPositionsAfterNodeChange( newPersonId, insertRank, insertPersonOrder, newRelationshipId );

        var insertChildhubRank  = insertRank + 1;
        var insertChildhubOrder = this._findBestInsertPosition( insertChildhubRank, newRelationshipId );
        var newChildhubId       = this.DG.GG.insertVertex("", TYPE.CHILDHUB, {}, 1.0, [newRelationshipId], []);
        this._updateGGRankOrderPositionsAfterNodeChange( newChildhubId, insertChildhubRank, insertChildhubOrder, newRelationshipId );

        var insertChildRank  = insertChildhubRank + 1;
        var insertChildOrder = this._findBestInsertPosition( insertChildRank, newChildhubId );
        var newChildId       = this.DG.GG.insertVertex("", TYPE.PERSON, childProperties, 1.0, [newChildhubId], []);
        this._updateGGRankOrderPositionsAfterNodeChange( newChildId, insertChildRank, insertChildOrder, newChildhubId );

        // validate: by now the graph should satisfy all assumptions
        this.DG.GG.validate();

        this._debugPrintAll("middle");

        // fix common layout mistakes (e.g. relationship not right above the only child)
        this._heuristics.improvePositioning();

        // update vertical separation for all nodes & compute ancestors
        this._updateauxiliaryStructures(ranksBefore, rankYBefore);

        timer.printSinceLast("=== NewRelationship runtime: ");
        this._debugPrintAll("after");

        this.DG.displayGraph(this.DG.positions, "zz");

        var movedNodes = this._findMovedNodes( positionsBefore, ranksBefore, vertLevelsBefore, rankYBefore );
        var newNodes   = [newRelationshipId, newPersonId, newChildId];
        return {"new": newNodes, "moved": movedNodes, "highlight": [personId]};
    },

    assignParent: function( parentId, childId )
    {
        if (this.DG.GG.isRelationship(parentId)) {
            var childHubId   = this.DG.GG.getRelationshipChildhub(parentId);
            var rankChildHub = this.DG.ranks[childHubId];
            var rankChild    = this.DG.ranks[childId];

            var weight = 1;
            this.DG.GG.addEdge(childHubId, childId, weight);

            var animateList = [childId];

            if (rankChildHub != rankChild - 1) {
                return this.redrawAll(animateList);
            }

            var positionsBefore  = this.DG.positions.slice(0);
            var ranksBefore      = this.DG.ranks.slice(0);
            var vertLevelsBefore = this.DG.vertLevel.copy();
            var rankYBefore      = this.DG.rankY;

            // TODO: move vertex closer to other children, if possible?

            // validate: by now the graph should satisfy all assumptions
            this.DG.GG.validate();

            // update vertical separation for all nodes & compute ancestors
            this._updateauxiliaryStructures(ranksBefore, rankYBefore);

            positionsBefore[parentId] = -1;
            var movedNodes = this._findMovedNodes( positionsBefore, ranksBefore, vertLevelsBefore, rankYBefore );
            return {"moved": movedNodes, "animate": [childId]};
        }
        else {
            var rankParent = this.DG.ranks[parentId];
            var rankChild  = this.DG.ranks[childId];

            if (rankParent != rankChild - 2) {
                // add new partner, add new relationship/childhub, connect childhub to child, redraw all

                // TODO
            }
            else {
                // can avoid total redraw, have to add considering existing layout (orders/ranks/positions)
                // TODO
                return {};
            }
        }

    },

    repositionAll: function ()
    {
        this.DG.positions = this.DG.position(this.DG.horizontalPersonSeparationDist, this.DG.horizontalRelSeparationDist);

        var movedNodes = this._getAllNodes();
        return {"moved": movedNodes};
    },

    redrawAll: function (animateList)
    {
        var ranksBefore = this.DG.ranks.slice(0);

        this._debugPrintAll("before");

        var baseGraph = this.DG.GG.makeGWithCollapsedMultiRankEdges();

        this.DG = new DrawGraph( baseGraph,
                                 this.DG.horizontalPersonSeparationDist,
                                 this.DG.horizontalRelSeparationDist,
                                 this.DG.maxInitOrderingBuckets,
                                 this.DG.maxOrderingIterations,
                                 this.DG.maxXcoordIterations );

        this._heuristics = new Heuristics( this.DG );
        this._heuristics.improvePositioning();

        var movedNodes = this._getAllNodes();

        var reRanked = [];
        for (var i = 0; i <= this.DG.GG.getMaxRealVertexId(); i++) {
            if (this.DG.GG.isPerson(i))
                if (this.DG.ranks[i] != ranksBefore[i]) {
                    reRanked.push(i);
                }
        }

        if (!animateList) animateList = [];

        this._debugPrintAll("after");

        return {"moved": movedNodes, "highlight": reRanked, "animate": animateList};
    },

    toJSON: function ()
    {
        var output = {};

        // note: need to save GG not base G becaus eof the graph was dynamically modified
        //       some new virtual edges may have different ID than if underlying G were
        //       converted to GG (as during such a conversion ranks would be correctly
        //       recomputed, but orders may mismatch). Thus to keep ordering valid need
        //       to save GG and restore G from it on de-serialization
        output["GG"] = this.DG.GG.serialize();

        output["ranks"]     = this.DG.ranks;
        output["order"]     = this.DG.order.serialize();
        output["positions"] = this.DG.positions;

        // note: everything else can be recomputed based on the information above

        console.log(JSON.stringify(output));

        return JSON.stringify(output);
    },

    fromJSON: function (serializedAsJSON)
    {
        var removedNodes = this._getAllNodes();

        serializedData = JSON.parse(serializedAsJSON);

        console.log("Got serialization object: " + stringifyObject(serializedData));

        this.DG.GG = InternalGraph.init_from_user_graph(serializedData["GG"],
                                                        this.DG.GG.defaultPersonNodeWidth, this.DG.GG.defaultNonPersonNodeWidth,
                                                        true);

        this.DG.G = this.DG.GG.makeGWithCollapsedMultiRankEdges();

        this.DG.ranks = serializedData["ranks"];

        this.DG.maxRank = Math.max.apply(null, this.DG.ranks);

        this.DG.order.deserialize(serializedData["order"]);

        this.DG.positions = serializedData["positions"];

        this._updateauxiliaryStructures();

        this.screenRankShift = 0;

        var newNodes = this._getAllNodes();

        return {"new": newNodes, "removed": removedNodes};
    },

    getPathToParents: function(v)
    {
        // returns [ [virtual_node_1, virtual_node_2, ..., parent1] [virtual_node_1, virtual_node_2, ..., parent2] ]

        var result = [];

        if (!this.DG.GG.isRelationship(v))
            throw "Assertion failed: incorrect v in getPathToParents()";

        var inEdges = this.DG.GG.getInEdges(v);

        result.push( this._getPathEndingInPerson(inEdges[0]) );
        result.push( this._getPathEndingInPerson(inEdges[1]) );

        return result;
    },

    //=============================================================

    _updateGGRankOrderPositionsAfterNodeChange: function (newNodeId, insertRank, insertOrder, connectedNodeId)
    {
        // does not assume the graph is consistent at this point, e.g. ther emay be childhubs with no relationships,
        // or relationships without persons attached

        if (newNodeId != this.DG.GG.getMaxRealVertexId())
            throw "Assertion failed: trying to update after an insert with a strange ID (not max ID)!";

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
        this._updatePositionsAfterNodeInsert( newNodeId, insertOrder, insertRank, connectedNodeId );
    },

    _updateauxiliaryStructures: function(ranksBefore, rankYBefore)
    {
        // update vertical levels
        this.DG.vertLevel = this.DG.positionVertically();
        this.DG.rankY     = this.DG.computeRankY(ranksBefore, rankYBefore);

        // update ancestors
        var ancestors = this.DG.findAllAncestors();
        this.DG.ancestors = ancestors.ancestors;
        this.DG.consangr  = ancestors.consangr;
    },

    _getAllNodes: function (minID, maxID)
    {
        var nodes = [];
        var minID = minID ? minID : 0;
        var maxID = maxID ? Math.min( maxID, this.DG.GG.getMaxRealVertexId()) : this.DG.GG.getMaxRealVertexId();
        for (var i = minID; i <= maxID; i++) {
            if ( this.DG.GG.type[i] == TYPE.PERSON || this.DG.GG.type[i] == TYPE.RELATIONSHIP )
                nodes.push(i);
        }
        return nodes;
    },

    _findMovedNodes: function (positionsBefore, ranksBefore, vertLevelsBefore, rankYBefore)
    {
        //console.log("Before: " + stringifyObject(vertLevelsBefore));
        //console.log("After:  " + stringifyObject(this.DG.vertLevel));
        //console.log("Before: " + stringifyObject(positionsBefore));
        //console.log("After: " + stringifyObject(this.DG.positions));

        var numNewInserted = this.DG.positions.length - positionsBefore.length;
        if (numNewInserted != this.DG.ranks.length - ranksBefore.length)
            throw "Assertion failed: num new nodes in positions does not match num new in ranks";

        var maxOldID = this.DG.GG.maxRealVertexId - numNewInserted;

        // TODO: check vertLevelsBefore.rankVerticalLevels + move all vertices at lower ranks

        var result = {};

        for (var i = 0; i <= maxOldID; i++) {
            // this node was moved
            if (this.DG.GG.type[i] == TYPE.RELATIONSHIP || this.DG.GG.type[i] == TYPE.PERSON)
            {
                var rank = this.DG.ranks[i];
                //if (rank != ranksBefore[i]) {
                //    this._addNodeAndAssociatedRelationships(i, result, maxOldID);
                //    continue;
                //}
                if (this.DG.rankY[rank] != rankYBefore[ranksBefore[i]]) {
                    this._addNodeAndAssociatedRelationships(i, result, maxOldID);
                    continue;
                }
                if (this.DG.positions[i] != positionsBefore[i]) {
                    this._addNodeAndAssociatedRelationships(i, result, maxOldID);
                    continue;
                }
                // or it is a relationship with a long edge - redraw just in case since long edges may have complicated curves around other nodes
                if (this.DG.GG.type[i] == TYPE.RELATIONSHIP) {
                    var inEdges = this.DG.GG.getInEdges(i);
                    if (inEdges[0] > this.DG.GG.maxRealVertexId || inEdges[1] > this.DG.GG.maxRealVertexId) {
                        result[i] = true;
                        continue;
                    }
                    var childHub = this.DG.GG.getRelationshipChildhub(i);
                    if (vertLevelsBefore.childEdgeLevel[childHub] != this.DG.vertLevel.childEdgeLevel[childHub]) {
                        result[i] = true;
                        continue;
                    }
                }
            }
        }

        var resultArray = [];
        for (var node in result) {
            if (result.hasOwnProperty(node)) {
                resultArray.push(node);
            }
        }

        return resultArray;
    },

    _addNodeAndAssociatedRelationships: function ( node, addToSet, maxOldID )
    {
        addToSet[node] = true;
        if (this.DG.GG.type[node] != TYPE.PERSON) return;

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

    _findBestInsertPosition: function ( rank, edgeToV, preferLeft )
    {
        if (rank == 0 || rank > this.DG.maxRank)
            return 0;

        // note: does not assert the graph satisfies all the assumptions in InternalGraph.validate()

        // find the order on rank 'rank' to insert a new vertex so that the edge connecting this new vertex
        // and vertex 'edgeToV' crosses the smallest number of edges.
        var edgeToRank      = this.DG.ranks[ edgeToV ];
        var edgeToOrder     = this.DG.order.vOrder[edgeToV];

        if (edgeToRank == rank && this.DG.GG.type[edgeToV] == TYPE.PERSON)
            // for same-rank edges we don't need to scan through all the vertices on the rank,
            // we just need to pick on which side of the initial vertex to insert new one
            return this._findBestRelationshipPosition( rank, edgeToV, edgeToOrder, preferLeft );

        var bestInsertOrder  = 0;
        var bestCrossings    = Infinity;
        var bestDistance     = Infinity;

        var crossingChildhubEdgesPenalty = false;
        if (this.DG.GG.type[edgeToV] == TYPE.CHILDHUB)
            crossingChildhubEdgesPenalty = true;

        var desiredPosition = this.DG.order.order[rank].length;  // by default: the later in the order the better: fewer vertices shifted

        if (this.DG.GG.type[edgeToV] == TYPE.CHILDHUB && this.DG.GG.getOutEdges(edgeToV).length > 0)   // for childhubs with children - next to other children
            desiredPosition = this._findRightmostChildPosition(edgeToV) + 1;

        for (var o = 0; o <= this.DG.order.order[rank].length; o++) {
            var numCrossings = this._edgeCrossingsByFutureEdge( rank, o - 0.5, edgeToRank, edgeToOrder, crossingChildhubEdgesPenalty );

            //console.log("position: " + o + ", numCross: " + numCrossings);

            if ( numCrossings < bestCrossings ||                           // less crossings
                 (numCrossings == bestCrossings && Math.abs(o - desiredPosition) <= bestDistance )   // closer to desired position
               ) {
               bestInsertOrder = o;
               bestCrossings   = numCrossings;
               bestDistance    = Math.abs(o - desiredPosition);
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

    _edgeCrossingsByFutureEdge: function ( fromRank, fromOrder, toRank, toOrder, crossingChildhubEdgesPenalty )
    {
        // counts how many existing edges a new edge from given rank&order to given rank&order would cross
        // if order is an integer, it is assumed it goes form an existing vertex
        // if order is inbetween two integers, it is assumed it is the position used for a new-to-be-inserted vertex

        // for simplicity (to know if we need to check outEdges or inEdges) get the edge in the correct direction
        // (i..e from lower ranks to higher ranks)
        var rankFrom  = Math.min( fromRank, toRank );
        var rankTo    = Math.max( fromRank, toRank );
        var orderFrom = (fromRank < toRank) ? fromOrder : toOrder;
        var orderTo   = (fromRank < toRank) ? toOrder : fromOrder;

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
                if (crossingChildhubEdgesPenalty && this.DG.GG.type[target] == TYPE.CHILDHUB)
                    penalty = Infinity;

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

        // try not to insert inbetween other relationships
        // (for that only need check edges on the same rank)
        var verticesAtRankFrom = this.DG.order.order[ rankFrom ];
        for (var ord = 0; ord < verticesAtRankFrom.length; ord++) {
            if ( ord == orderFrom ) continue;

            var vertex = verticesAtRankFrom[ord];

            var outEdges = this.DG.GG.getOutEdges(vertex);
            var len      = outEdges.length;

            for (var j = 0; j < len; j++) {
                var target = outEdges[j];

                var orderTarget = this.DG.order.vOrder[target];
                var rankTarget  = this.DG.ranks[target];

                if (rankTarget == rankFrom)
                {
                    if ( fromOrder < ord && fromOrder > orderTarget ||
                         fromOrder > ord && fromOrder < orderTarget )
                         crossings += 0.1;
                }
            }
        }


        return crossings;
    },

    _findBestRelationshipPosition: function ( rank, v, vOrder, preferLeft )
    {
        // for same-rank edges we don't need to scan through all the vertices on the rank,
        // we just need to pick on which side of the initial vertex to insert new one
        // (given everything else being equal, prefer the right side - to move fewer nodes)

        if (preferLeft && vOrder == 0) return 0;

        var outEdges = this.DG.GG.getOutEdges(v);

        var rightOf = 0;
        var leftOf  = 0;

        for (var i = 0; i < outEdges.length; i++) {
            var u = outEdges[i];
            if (this.DG.ranks[u] != rank) continue;

            if (this.DG.order.vOrder[u] > vOrder)
                rightOf++;
            else
                leftOf++;
        }

        if (rightOf > leftOf || (preferLeft && rightOf == leftOf)) return vOrder;
        return vOrder + 1;
    },

    //=============================================================

    _updatePositionsAfterNodeInsert: function ( newNodeId, insertOrder, insertRank, connectedNodeId )
    {
        // algorithm:
        // 1) insert new node according to its order, the exact X-coordinate is picked accoridng to
        //    a heuristic which tries to keep edges straight and move as few vertices as possible
        //    (vertices earlier in the order are never moved). If the node to the right has to move,
        //    move the minimum required distance, and add it to the queue of disturbed nodes
        //
        // 2) while queue is not empty: check which nodes were affected because of the shift of the
        ///   node poped from the queue: nodes to the right, parents & children. Shift those
        //    accordingly (see below) and add them to the queue.
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
        // note: does not assert the graph satisfies all the assumptions in InternalGraph.validate(),
        //       in particular this can be called after a childhub was added but before it's relationship was added

        this.DG.positions.splice( newNodeId, 0, -1 );  // temporary -1 position: to bring this.DG.positions in sync with new node IDs

        var xcoord = new XCoord();
        xcoord.init(this.DG.positions, this.DG.horizontalPersonSeparationDist, this.DG.horizontalRelSeparationDist,
                    this.DG.GG.vWidth, this.DG.order, this.DG.ranks, this.DG.GG.type);

        var leftBoundary  = xcoord.getLeftMostNoDisturbPosition(newNodeId, true);   // true: allow negative coordinates: will be normalized
        var rightBoundary = xcoord.getRightMostNoDisturbPosition(newNodeId);

        var desiredPosition = this.DG.positions[connectedNodeId];   // insert right above or right below
        if (this.DG.ranks[connectedNodeId] == insertRank) {         // insert on the same rank: then instead ot the left or to the right
            if (this.DG.order.vOrder[newNodeId] > this.DG.order.vOrder[connectedNodeId])
                desiredPosition = xcoord.getRightEdge(connectedNodeId) + xcoord.getSeparation(newNodeId, connectedNodeId) + xcoord.halfWidth[newNodeId];
            else
                desiredPosition = xcoord.getLeftEdge(connectedNodeId) - xcoord.getSeparation(newNodeId, connectedNodeId) - xcoord.halfWidth[newNodeId];
        }

        if ( desiredPosition < leftBoundary )
            insertPosition = leftBoundary;
        else
        if ( desiredPosition > rightBoundary )
            insertPosition = Math.max(leftBoundary, rightBoundary);   // if we insert between two closedly packed nodes, leftBoundary will be greater than rightBoundary
        else
            insertPosition = desiredPosition;

        //console.log("Position: " + insertOrder + ", leftBoundary: " + leftBoundary + ", right: " + rightBoundary + ", desired: " + desiredPosition + ", actualInsert: " + insertPosition);

        xcoord.xcoord[newNodeId] = insertPosition;

        // find which nodes we need to shift to accomodate this insertion via "domino effect"

        var alreadyProcessed = {};
        alreadyProcessed[newNodeId] = true;

        var shiftAmount = 0;

        var disturbedNodes = new Queue();
        disturbedNodes.push( newNodeId );

        var iter = 0;

        do {

            var childrenMoved = {};   // we only move a chldhub if all its nodes have moved

            // small loop 1: shift all vertices except chldhubs, which only shift if all children shift
            while ( disturbedNodes.size() > 0 && iter < 100) {

                iter++;

                var v = disturbedNodes.pop();

                var type  = this.DG.GG.type[v];
                var vrank = this.DG.ranks[v];

                var position    = xcoord.xcoord[v];
                var rightMostOK = xcoord.getRightMostNoDisturbPosition(v);

                //console.log("iter: " + iter + ", v: " + v + ", pos: " + position + ", righNoDisturb: " + rightMostOK + ", shift: " + shiftAmount + ", al[7]: " + alreadyProcessed[7]);

                if (position > rightMostOK) {
                    // the node to the right was disturbed: shift it
                    var vorder         = this.DG.order.vOrder[v];
                    var rightDisturbed = this.DG.order.order[vrank][vorder+1];

                    if (alreadyProcessed.hasOwnProperty(rightDisturbed)) continue;

                    var toMove = position - rightMostOK;
                    if (toMove > shiftAmount)
                        shiftAmount = toMove;

                    alreadyProcessed[rightDisturbed] = true;
                    xcoord.xcoord[rightDisturbed] += shiftAmount;
                    disturbedNodes.push(rightDisturbed);
                    //console.log("add1: " + rightDisturbed + " (toMove: " + toMove +")");
                }

                var inEdges  = this.DG.GG.getInEdges(v);
                var outEdges = this.DG.GG.getOutEdges(v);

                // force childhubs right below relationships.
                if (type == TYPE.RELATIONSHIP && outEdges.length == 1) {
                    var childHubId = outEdges[0];
                    var childPos   = xcoord.xcoord[childHubId];
                    var toMove     = position - childPos;
                    if (toMove > shiftAmount)
                        shiftAmount = toMove;
                    //console.log("----- id: " + childHubId + ", pos: " + childPos + ", move: " + toMove);
                }

                // go though out- and in- edges and propagate the movement

                //---------
                var skipInEdges = false;
                if (type == TYPE.PERSON && v == newNodeId)
                    skipInEdges = true;
                // if we need to strech something -> stretch relationship edges to the right of
                if (type == TYPE.RELATIONSHIP) {
                    skipInEdges = true;
                    // except the case when inedge comes from a vertex to the left with no other in- or out-edges (a node connected only to this reltionship)
                    if (inEdges.length == 2) {
                        var parent0 = inEdges[0];
                        var parent1 = inEdges[1];
                        var order0 = this.DG.order.vOrder[parent0];
                        var order1 = this.DG.order.vOrder[parent1];
                        if (order0 < order1 && this.DG.GG.getOutEdges(parent0).length == 1 && this.DG.GG.getInEdges(parent0).length == 0)
                            skipInEdges = false;
                        else if (order1 < order0 && this.DG.GG.getOutEdges(parent1).length == 1 && this.DG.GG.getInEdges(parent1).length == 0)
                            skipInEdges = false;
                    }
                }

                if (!skipInEdges) {
                    for (var i = 0; i < inEdges.length; i++) {
                        var u     = inEdges[i];
                        var typeU = this.DG.GG.type[u];

                        if (alreadyProcessed.hasOwnProperty(u)) continue;

                        if (type == TYPE.PERSON && typeU == TYPE.CHILDHUB) {
                            if (childrenMoved.hasOwnProperty(u))
                                childrenMoved[u]++;
                            else
                                childrenMoved[u] = 1;

                            continue;
                        }

                        alreadyProcessed[u] = true;
                        xcoord.xcoord[u] += shiftAmount;
                        disturbedNodes.push(u);
                        //console.log("add2: " + u);
                    }
                }
                //---------

                //---------
                if (type == TYPE.CHILDHUB) {
                    //if (inEdges.length > 0) {
                    //    var relNodeId = inEdges[0];
                    //    if (xcoord.xcoord[relNodeId] > xcoord.xcoord[v]
                    //}

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

                    if ( this.DG.ranks[u] == vrank ) continue;   // vertices on the same rank will only be shifted if pushed ot the right by left neighbours
                    if ( alreadyProcessed.hasOwnProperty(u) ) continue;
                    if (type == TYPE.RELATIONSHIP && xcoord.xcoord[u] >= xcoord.xcoord[v]) continue;

                    alreadyProcessed[u] = true;
                    xcoord.xcoord[u] += shiftAmount;
                    disturbedNodes.push(u);
                    //console.log("add3: " + u);
                }
                //---------
            }


            // small loop 2: shift childhubs, if necessary
            for (var chhub in childrenMoved) {
                if (childrenMoved.hasOwnProperty(chhub)) {
                    if (this.DG.GG.getOutEdges(chhub).length == childrenMoved[chhub]) {
                        if (!alreadyProcessed.hasOwnProperty(chhub)) {
                            alreadyProcessed[chhub] = true;
                            xcoord.xcoord[chhub] += shiftAmount;
                            disturbedNodes.push(chhub);
                        }
                    }
                }
            }

        // propagate this childhub movement and keep going
        }
        while ( disturbedNodes.size() > 0 && iter < 100 );

        //xcoord.normalize();  <- no normalization to minimize the numbe rof moved nodes; UI is ok with negative coords

        this.DG.positions = xcoord.xcoord;

        console.log("ADDED: " + newNodeId + " @ position " + this.DG.positions[newNodeId]);
    },
    //=============================================================

    _getPathEndingInPerson: function(v)
    {
        var path = [v];

        while (this.DG.GG.type[v] != TYPE.PERSON)
        {
            v = this.DG.GG.getInEdges(v)[0];
            path.push(v);
        }

        return path;
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

        var rank  = this.DG.ranks[personId];
        var order = this.DG.order.vOrder[personId];

        var outEdges = this.DG.GG.getOutEdges(personId);
        if (outEdges.length != 1) return;  // only if have exactly one parner

        var relationship = outEdges[0];
        var relOrder     = this.DG.order.vOrder[relationship];

        var partners = this.DG.GG.getInEdges(relationship);

        var partnerId      = (partners[0] == personId) ? partners[1] : partners[0];
        var parnerOutEdges = this.DG.GG.getOutEdges(partnerId);
        if (parnerOutEdges.length != 1) return;  // only if parner also has exactly one parner (which is personId)

        var partnerOrder = this.DG.order.vOrder[partnerId];
        if (partnerOrder != order - 2 && partnerOrder != order + 2) return;  // only if next to each other

        var childhubId = this.DG.GG.getOutEdges(relationship)[0]; // <=> getRelationshipChildhub(relationship)
        var children   = this.DG.GG.getOutEdges(childhubId);

        if (children.length == 0) return;

        // TODO: count how many edges will be crossed in each case and only swap if we save a few crossings

        // idea:
        // if (to the left  of parner && leftmostChild  has parner to the left  && rightmostchid has no parner to the right) -> swap
        // if (to the right of parner && rightmostChild has parner to the right && leftmostchid  has no parner to the left) -> swap

        var toTheLeft = (order < partnerOrder);

        var childrenPartners = this.analizeChildren(childhubId);

        if ( (toTheLeft  && childrenPartners.leftMostHasLParner  && !childrenPartners.rightMostHasRParner) ||
             (!toTheLeft && childrenPartners.rightMostHasRParner && !childrenPartners.leftMostHasLParner) ) {
            this.swapParners( personId, partnerId, relationship );  // updates orders + positions
        }
    },

    analizeChildren: function (childhubId)
    {
        if (!this.DG.GG.isChildhub(childhubId))
            throw "Assertion failed: applying analizeChildren() not to a childhub";

        var children = this.DG.GG.getOutEdges(childhubId);

        if (children.length == 0) return;

        var havePartners        = {};
        var numWithPartners     = 0;
        var leftMostChildId     = undefined;
        var leftMostChildOrder  = Infinity;
        var leftMostHasLParner  = false;
        var rightMostChildId    = undefined;
        var rightMostChildOrder = -Infinity;
        var rightMostHasRParner = false;
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
            }

        }

        var vorders = this.DG.order.vOrder;
        var orderedChildren = children.slice(0);
        orderedChildren.sort(function(x, y){ return vorders[x] > vorders[y] });

        //console.log("ordered ch: " + stringifyObject(orderedChildren));

        return {"leftMostHasLParner" : leftMostHasLParner,
                "leftMostChildId"    : leftMostChildId,
                "leftMostChildOrder" : leftMostChildOrder,
                "rightMostHasRParner": rightMostHasRParner,
                "rightMostChildId"   : rightMostChildId,
                "rightMostChildOrder": rightMostChildOrder,
                "withPartnerSet"     : havePartners,
                "numWithPartners"    : numWithPartners,
                "orderedChildren"    : orderedChildren };
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

    swapParners: function( partner1, partner2, relationshipId)
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

        //console.log("before move: " + stringifyObject(this.DG.order));

        this.DG.order.move(rank, personOrder,  moveOrders);
        this.DG.order.move(rank, relOrder,     moveOrders);
        this.DG.order.move(rank, partnerOrder, moveOrders);

        //console.log("after move: " + stringifyObject(this.DG.order));

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

        // 1. check tghat we have exactly one partner and it has parents - if not nothing to move
        var parnetships = this.DG.GG.getOutEdges(personId);
        if (parnetships.length != 1) return;
        var relationshipId = parnetships[0];

        var partners  = this.DG.GG.getInEdges(relationshipId);
        var partnerId = (partners[0] == personId) ? partners[1] : partners[0];  // the only partner of personId
        if (this.DG.GG.getInEdges(partnerId).length == 0) return; // partner has no parents!

        if (this.DG.GG.getOutEdges(partnerId).length > 1) return; // partner has multiple partnerships, too complicated

        var order        = this.DG.order.vOrder[personId];
        var partnerOrder = this.DG.order.vOrder[partnerId];
        if (partnerOrder != order - 2 && partnerOrder != order + 2) return;  // only if next to each other

        var toTheLeft = (order < partnerOrder);

        // 2. check where the partner stands among its sinblings
        var partnerChildhubId   = this.DG.GG.getInEdges(partnerId)[0];
        var partnerSibglingInfo = this.analizeChildren(partnerChildhubId);

        if (partnerSibglingInfo.orderedChildren.length == 1) return; // just one sibling, nothing to do

        // simple cases:
        if (partnerSibglingInfo.leftMostChildId == partnerId) {
            if (!toTheLeft)
                this.swapParners( personId, partnerId, relationshipId );
            return;
        }
        if (partnerSibglingInfo.rightMostChildId == partnerId) {
            if (toTheLeft)
                this.swapParners( personId, partnerId, relationshipId );
            return;
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
        console.log("parents: " + stringifyObject(partnerParents));
        var numLeftPartners  = this.DG.GG.getOutEdges(leftParent).length;
        var numRightPartners = this.DG.GG.getOutEdges(rightParent).length;
        console.log("num left: " + numLeftPartners + ", numRight: " + numRightPartners);
        if (numLeftPartners > 1 && numRightPartners > 1) return;

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
                        this.swapParners( personId, partnerId, relationshipId );
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
                        this.swapParners( personId, partnerId, relationshipId );
                    this.moveSiblingPlusPartnerToOrder( personId, partnerId, relationshipId, partnerSibglingInfo.leftMostChildOrder);
                    return;
                }
                if (partnerSibglingInfo.withPartnerSet.hasOwnProperty(sibling)) break; // does not work on this side
            }
        }
    },

    improvePositioning: function ()
    {
        // given a finished positioned graph (asserts the graph is valid) try to fix common mistakes such as:
        //  1) the only relationship not rigth above the only child: can be fixed by
        //     a) moving the child, if possible without disturbiung other nodes
        //     b) moving relationship + one (or both, if possible) partners, if possible without disturbiung other nodes
        //  2) relationship not above one of it's children (preferably one in the middle) and not
        //     right in the midpoint between left and right child: can be fixed by
        //     a) moving relationship + both partners, if possible without disturbiung other nodes
        //  3) not nice long edge crossings (example pending) - TODO

        var xcoord = new XCoord();
        xcoord.init(this.DG.positions, this.DG.horizontalPersonSeparationDist, this.DG.horizontalRelSeparationDist,
                    this.DG.GG.vWidth, this.DG.order, this.DG.ranks, this.DG.GG.type);

        var iter = 0;
        var improved = true;
        while (improved && iter < 100) {
            improved = false;
            iter++;
            for (var v = 0; v <= this.DG.GG.getMaxRealVertexId(); v++) {
                if (!this.DG.GG.isRelationship(v)) continue;

                var parents   = this.DG.GG.getInEdges(v);
                var childhub  = this.DG.GG.getRelationshipChildhub(v);

                var relX      = xcoord.xcoord[v];
                var childhubX = xcoord.xcoord[childhub];

                if (childhubX != relX) {
                    improved = xcoord.moveNodeAsCloseToXAsPossible(childhub, relX, true);
                    childhubX = xcoord.xcoord[childhub];
                    console.log("moving " + childhub + " to " + xcoord.xcoord[childhub]);
                }

                var childInfo = this.analizeChildren(childhub);

                if (childInfo.orderedChildren.length == 1) {
                    var childId = childInfo.orderedChildren[0];
                    if (xcoord.xcoord[childId] == childhubX) continue;

                    improved = xcoord.moveNodeAsCloseToXAsPossible(childId, childhubX, true);
                    console.log("moving " + childId + " to " + xcoord.xcoord[childId]);

                    if (xcoord.xcoord[childId] == childhubX) continue;

                    // ok, we can't move the child. Try to move the relationship & the parent(s)

                }
                else {

                }
            }
            console.log("iter: " + iter);
        }

        xcoord.normalize();  //<- no normalization to minimize the number of moved nodes; UI is ok with negative coords

        this.DG.positions = xcoord.xcoord;
    }
};

