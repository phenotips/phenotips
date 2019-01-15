define(["pedigree/model/helpers"], function(Helpers){

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
            var leftMostChildId     = undefined;
            var leftMostChildOrder  = Infinity;
            var leftMostHasLParner  = false;
            var rightMostChildId    = undefined;
            var rightMostChildOrder = -Infinity;
            var rightMostHasRParner = false;
            var unrelatedNodesBetwenChildren = false;

            var onlyPlaceholder = false;
            if (children.length == 1 && this.DG.GG.isPlaceholder(children[0])) {
                onlyPlaceholder = true;
            }

            // all children are always on the samew rank, so get the rank of any child
            var rank = this.DG.ranks[children[0]];

            // a set of all childrent plus all nodes directly connected to the children (their relationships, partners)
            var allChildConnections = {};

            for (var i = 0; i < children.length; i++) {
                var childId = children[i];
                var order   = this.DG.order.vOrder[childId];

                allChildConnections[childId] = true;

                var childRels     = this.DG.GG.getAllRelationships(childId);
                var childPartners = this.DG.GG.getAllPartners(childId);

                childRels    .forEach(function(relId)     { allChildConnections[relId]     = true; });
                childPartners.forEach(function(partnerId) { allChildConnections[partnerId] = true; });

                // TODO: is there a need to add long edge pieces connected to these relationships to allChildConnections as well?

                if (order < leftMostChildOrder) {
                    leftMostChildId    = childId;
                    leftMostChildOrder = order;
                    // check if has partner or relationship to the left on the same rank:
                    leftMostHasLParner = this.nodeBetweenOrdersOnRank(childRels,     rank, 0, order-1) ||
                                         this.nodeBetweenOrdersOnRank(childPartners, rank, 0, order-1);
                }
                if (order > rightMostChildOrder) {
                    rightMostChildId    = childId;
                    rightMostChildOrder = order;
                    // check if has partner or relationship to the right on the same rank:
                    rightMostHasRParner = this.nodeBetweenOrdersOnRank(childRels,     rank, order+1, Infinity) ||
                                          this.nodeBetweenOrdersOnRank(childPartners, rank, order+1, Infinity);
                }
                if (childRels.length > 0) {
                    havePartners[childId] = true;
                    numWithPartners++;
                }
            }

            // check if there are unrelated nodes positioned between child nodes:
            // algorithm will place none there, but some can be placed there using manual rearrangement
            for (var order = leftMostChildOrder + 1; order < rightMostChildOrder; order++) {
                var nodeId = this.DG.order.order[rank][order];
                if (!allChildConnections.hasOwnProperty(nodeId)) {
                    unrelatedNodesBetwenChildren = true;
                    break;
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
                    "orderedChildren"    : orderedChildren,
                    "onlyPlaceholder"    : onlyPlaceholder,
                    "unrelatedNodesBetwenChildren": unrelatedNodesBetwenChildren};
        },

        nodeBetweenOrdersOnRank: function( nodeList, rank, minOrder, maxOrder )
        {
            for (var i = 0; i < nodeList.length; i++ ) {
                var node = nodeList[i];
                var nodeRank = this.DG.ranks[node];
                if (nodeRank != rank) continue;

                var nodeOrder = this.DG.order.vOrder[node];
                if (nodeOrder >= minOrder && nodeOrder <= maxOrder)
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

        improvePositioning: function ()
        {
            var timer = new Helpers.Timer();

            //console.log("pre-fix orders: " + Helpers.stringifyObject(this.DG.order));
            //var xcoord = new XCoord(this.DG.positions, this.DG);
            //this.DG.displayGraph(xcoord.xcoord, "pre-fix");

            //DEBUG: for testing how layout looks when multi-rank edges are not improved
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

                    var parents   = this.DG.GG.getInEdges(v);  // these are the nodes "preceding" v in the pedihgree: those can be
                                                               // person nodes or pieces of a multi-generation edge: for the purpose
                                                               // of moving nodes both cases are OK and getInEdges(v) is what we need

                    var childhub  = this.DG.GG.getRelationshipChildhub(v);

                    var relX      = xcoord.xcoord[v];
                    var childhubX = xcoord.xcoord[childhub];

                    var childInfo = this.analizeChildren(childhub);

                    if (childInfo.unrelatedNodesBetwenChildren) {
                        // this can be too complicated, can't apply this simple heurostic
                        continue;
                    }

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

                        // no need to move anything when parent line is within the range we want it to be
                        if (positionInfo.minPreferred <= childhubX && childhubX <= positionInfo.maxPreferred) continue;

                        // find the closest point within the "prefered" interval (which will always be one of the endpoints)
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

                    // FIXME: logic below is a bit overcomplicated, need to review

                    // OK, harder case: either move the parents or the children (with whatever nodes are connected to them, in both cases).
                    // (need to make sure we do not break what has already been good, or we may be stuck in an infinite improvement loop)

                    // try to either shift the entire distance (misalignment) or (if that fails) at least
                    // as far as parents can go without pushing other nodes

                    //var id = id ? (id+1) : 1; // DEBUG
                    //console.log("Analizing for childhub " + childhub);

                    var leftParent  = (xcoord.xcoord[parents[0]] < xcoord.xcoord[parents[1]]) ? parents[0] : parents[1];
                    var rightParent = (xcoord.xcoord[parents[0]] < xcoord.xcoord[parents[1]]) ? parents[1] : parents[0];

                    var shiftList = [v, childhub];
                    if (this.DG.order.vOrder[leftParent]  == this.DG.order.vOrder[v] - 1
                            && !this.DG.GG.isVirtual(leftParent)
                            && this.DG.ranks[leftParent] == this.DG.ranks[v]) {
                        if (misalignment > 0 || xcoord.getSlackOnTheLeft(v) < -misalignment)
                            shiftList.unshift(leftParent);
                    }
                    if (this.DG.order.vOrder[rightParent] == this.DG.order.vOrder[v] + 1
                            && !this.DG.GG.isVirtual(rightParent)
                            && this.DG.ranks[rightParent] == this.DG.ranks[v]) {
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

            // sometimes the heuristics above fail to shift parent and/or children in a way that makes
            // the pedigree look good. So as a last resort, try to at least fix remaining cases like the one
            // shown below by moving just one child (closest to the childhub line, marked with <*> below) -
            // only when the change is as simple as moving one node:
            //
            //  []--*--()                   []--*--()
            //      |                           |
            //      +---+--------+     -->      +------------+
            //          |        |              |            |
            //         <*> ...  <?>            <*>    ...   <?>
            //
            for (var k = 0; k < orderedRelationships.length; k++) {
                var v = orderedRelationships[k];

                var childhubID = this.DG.GG.getRelationshipChildhub(v);
                var childhubX  = xcoord.xcoord[childhubID];

                var childInfo = this.analizeChildren(childhubID);

                var leftMostX  = xcoord.xcoord[childInfo["leftMostChildId"]];
                var rightMostX = xcoord.xcoord[childInfo["rightMostChildId"]];

                var moveChildId       = null;
                var desiredMoveAmount = 0;
                if (leftMostX > childhubX && rightMostX > childhubX) {
                    moveChildId = childInfo["leftMostChildId"];
                    desiredMoveAmount = childhubX - leftMostX;   // negative amount: move left
                    if (xcoord.getSlackOnTheLeft(moveChildId) < Math.abs(desiredMoveAmount)) {
                        continue; // can't move
                    }
                } else if (leftMostX < childhubX && rightMostX < childhubX) {
                    moveChildId       = childInfo["rightMostChildId"];
                    desiredMoveAmount = childhubX - rightMostX;  // positive amount: move right
                    if (xcoord.getSlackOnTheRight(moveChildId) < desiredMoveAmount) {
                        continue; // can't move
                    }
                } else {
                    continue; // all other cases are not handled by this heuristic
                }

                if (moveChildId !== null && desiredMoveAmount != 0) {
                    xcoord.xcoord[moveChildId] += desiredMoveAmount;
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

            timer.printSinceLast("=== Improvement (heuristics) runtime: ");
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

                        // get v's siblings (if any) to simplify checking for connections spaning the gap between v
                        // and its right neighbour
                        var childhubID = null;
                        if (DG.GG.isPerson(v) && DG.GG.getProducingRelationship(v) !== null) {
                            childhubID = DG.GG.getRelationshipChildhub(DG.GG.getProducingRelationship(v));
                        }

                        var excludeEdgesSpanningOrder = function(from, to) {
                            var orderFrom = DG.order.vOrder[from];
                            var orderTo   = DG.order.vOrder[to];

                            // filter to exclude all edges spanning the gap between v and its right neighbour
                            if (DG.ranks[from] == rank && DG.ranks[to] == rank) {
                                if ((orderFrom <= order && orderTo   > order) ||
                                    (orderTo   <= order && orderFrom > order) ) {
                                    return false;
                                }
                            }

                            // the filter above will not exclude connections between children ordered before and after "order"
                            // on the rank we are interested in, because the connection spans two ranks, so need to handle this
                            // case separately
                            if (from == childhubID) {
                                // this is an edge from v's childhub to v's sibling (since all edges from v's childhub go to v's siblings)
                                if (orderTo > order) {
                                    // exclude if it is a sibling on the "other side" of the gap
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
                            var stopSet = {};   // note: no need for a stop set here, if nodes are connected the
                                                // connection would be found while analizing the left component
                            component = this.DG.findConnectedComponent(rightNeighbour, excludeEdgesSpanningOrder, stopSet, maxComponentSize );
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

            var xcoord = new XCoord(this.DG.positions, this.DG, true /* do not validate, we know initial positioning is wrong */);

            //console.log("Orders at insertion rank: " + Helpers.stringifyObject(this.DG.order.order[this.DG.ranks[newNodeId]]));
            //console.log("Positions of nodes: " + Helpers.stringifyObject(xcoord.xcoord));

            var leftBoundary  = xcoord.getLeftMostNoDisturbPosition(newNodeId);
            var rightBoundary = xcoord.getRightMostNoDisturbPosition(newNodeId);

            var desiredPosition = this.DG.positions[nodeToKeepEdgeStraightTo];             // insert right above or right below

            if (nodeToKeepEdgeStraightTo != newNodeId) {
                if (this.DG.ranks[nodeToKeepEdgeStraightTo] == originalDisturbRank) {     // insert on the same rank: then instead ot the left or to the right
                    if (this.DG.order.vOrder[newNodeId] > this.DG.order.vOrder[nodeToKeepEdgeStraightTo])
                        desiredPosition = xcoord.getRightEdge(nodeToKeepEdgeStraightTo) + xcoord.getSeparationByType(newNodeId, nodeToKeepEdgeStraightTo) + xcoord.halfWidth[newNodeId];
                    else {
                        desiredPosition = xcoord.getLeftEdge(nodeToKeepEdgeStraightTo) - xcoord.getSeparationByType(newNodeId, nodeToKeepEdgeStraightTo) - xcoord.halfWidth[newNodeId];
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

    return Heuristics;
});
