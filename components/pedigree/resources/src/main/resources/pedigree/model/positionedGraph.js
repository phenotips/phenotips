// PositionedGraph represents the pedigree tree projected to a 2D surface,
//                 i.e. both the underlying graph and node & edge X and Y coordinates

define([
        "pedigree/pedigreeDate",
        "pedigree/model/edgeOptimization",
        "pedigree/model/helpers",
        "pedigree/model/ordering",
        "pedigree/model/queues",
        "pedigree/model/xcoordclass",
        "pedigree/model/layoutHeuristics",
        "pedigree/view/ageCalc"
    ], function(
        PedigreeDate,
        VerticalPosIntOptimizer,
        Helpers,
        Ordering,
        Queue,
        XCoord,
        Heuristics,
        AgeCalc
    ){

    PositionedGraph = function( baseG,             // mandatory, BaseGraph
                                probandNodeId,     // optional, int (default: no proband)
                                options,           // optional, object, see PositionedGraph.options
                                suggestedLayout )  // optional, object
                                                   //           { "ranks": array[int], "order": ..., "positons": ... }
    {
        this.GG = undefined;         // BaseGraph: graph without any positioning info
                                     //            (most algorithms here assume there are no multi-rank edges, so one
                                     //            of the first steps once ranks are computed is to replaced multi-rank
                                     //            edges by virtual vertices/edges each crossing just one rank, thus
                                     //            baseG might be modified as a result)

        this.ranks     = undefined;  // 1D array: index = vertex id, value = (vertical) rank of the node
                                     //           (for person nodes rank is logically similar to their "generation"
                                     //           where parents have lower generation than their children; however
                                     //           ranks are not equivalent to generations, since there are intermediate
                                     //           ranks occupied by internal helper nodes)

        this.maxRank   = undefined;  // integer: max rank in the above array (maintained for performance reasons)

        this.order     = undefined;  // class: Ordering (data about horizontal order of nodes for each rank)

        this.positions = undefined;  // 1D array: index = vertex id, value = x-coordinate

        this.vertLevel = undefined;  // class: VerticalLevels (data about vertical positioning of various horizontal
                                     //        lines such as relationship lines or child/sibling lines, there might be
                                     //        a lot of such lines at any given rank, good positioning can minimize the
                                     //        number of intersections between them)

        this.rankY     = undefined;  // 1D aray: index = rank, value = y-coordinate of the "base level" of each rank

        this.ancestors = undefined;  // {}: for each node contains a set of all its ancestors and the closest relationship distance
        this.consangr  = undefined;  // {}: for each node a set of consanguineous relationship IDs

        this.probandId = undefined;  // int; need it at this level to order proband parents correctly; -1 means no proband

        this.heuristics = undefined; // class: Heuristics. Provides layout optimization methods not absolutely
                                     // necessary during the intitial layout, but which may be useful to apply after
                                     // the basic layout algorithm (global optimization) or which allow specific
                                     // localized layout modifications during interactive sessions (e.g. locally move
                                     // some nodes around before/after a modificatoin request to minimize edge
                                     // crossings without re-computing the entire layout from scratch)
                                     // TODO: refactor

        this.initialize( baseG,
                         probandNodeId,
                         options,
                         suggestedLayout );
    };

    PositionedGraph.prototype = {

        options: {
            maxInitOrderingBuckets: 5,           // it may take up to ~factorial_of_this_number iterations to generate initial ordering
            maxOrderingIterations:  24,          // up to so many iterations are spent optimizing initial ordering
            maxXcoordIterations:    4,
            xCoordEdgeWeightValue:  true,        // whether to take edge weight into account when optimizing edge length/curvature

            horizontalPersonSeparationDist: 10,  // same relative units as in BaseGraph.width fields. Min distance between two person nodes
            horizontalTwinSeparationDist:    8,  // same units; Min distance between two twins (overwrites horizontalPersonSeparationDist)
            horizontalRelRelSeparationDist:  8,  // same units; Min distance between a two relationship nodes
            horizontalRelSeparationDist:     6,  // same units; Min distance between a relationship node and other types of nodes

            yDistanceNodeToChildhub:      21.6,  // unit value has no meaning (a scaling factor which looks ok on a specific medium
            yDistanceChildhubToNode:        14,  // should be used when drawing, what matters is relative value of various y-distance values)
            yExtraPerHorizontalLine:         4,
            yAttachPortHeight:             1.5,
            yCommentLineHeight:            2.8,
        },

        initialize: function( baseG,
                              probandNodeId,
                              options,
                              suggestedLayout )
        {
            // use user provided values for all keys which match existing internal options keys
            Helpers.setByTemplate(this.options, options);
            if (this.options.maxInitOrderingBuckets > 8) {
                throw "Too many ordering buckets: number of permutations ("
                    + this.options.maxInitOrderingBuckets + "!) is too big";
            }

            this.GG = baseG;

            this.heuristics = new Heuristics(this);

            this.probandId = Helpers.isInt(probandNodeId) ? probandNodeId : -1;

            // 0)
            // input may include suggested layout, either complete or partial. Suported layouts may include:
            //  a) ranks
            //  b) ranks + orders
            //  c) ranks + orders + positions
            //
            //  In case of a) only ranks for person nodes are used and the graph can have myulti-rank edges which will be
            //  taken care of
            //  In case of b) and c) the graph should have no multi-rank edges (they should already be split) and ranks
            //  should include ranks for all nodes
            //
            //  It is assumed that the sugested layout has correct ranks and orders (of provided) and that calling code
            //  did all the validations. The check is still performed below to validate suggested positions.
            //  TODO: review this assumption. Maybe code will be cleaner if validation is also/instead performed here
            if (suggestedLayout) {
                this.ranks = suggestedLayout.ranks;
                this.maxRank = Math.max.apply(null, this.ranks);

                if (suggestedLayout.order) {
                    this.order = suggestedLayout.order;
                    this.GG = baseG;
                }
            }

            var timer = new Helpers.Timer();

            if (!this.ranks || !this.order) {
                // 1)
                var timer = new Helpers.Timer();

                this.GG.collapseMultiRankEdges();

                this.ranks = this.rank(this.GG, (suggestedLayout ? suggestedLayout.ranks : null));

                this.maxRank = Math.max.apply(null, this.ranks);

                timer.printSinceLast("=== Ranking runtime: ");

                // 1.1)
                // ordering algorithms need all edges to connect nodes on neighbouring ranks only;
                // to accomodate that multi-rank edges are split into a chain of edges between new
                // "virtual" nodes on intermediate ranks. The ranks array is modified to include ranks
                // of the new virtual nodes
                this.ranks = this.GG.splitMultiRankEdges(this.ranks);

                timer.printSinceLast("=== Multi-rank edge conversion runtime: ");

                //Helpers.printObject( this.GG );

                // 2)

                // 2.1
                // twins should always be next to each other. The easiest and fastest way to accomodate that is by
                // conbining all twins in each group into one node, connected to all the nodes all of the twins in
                // the group connect to. This reduces the size of the graph and keeps twins together
                var disconnectedTwins = this.disconnectTwins();

                // 2.2)
                this.order = this.ordering(this.options.maxInitOrderingBuckets, this.options.maxOrderingIterations, disconnectedTwins);

                // 2.3)
                // once ordering is known need to re-rank relationship nodes to be on the same level as the
                // lower ranked parent. Attempt to place next to one of the parents; having ordering info
                // helps to pick the parent in case parents are on the same level and not next to each other
                this.reRankRelationships();

                // 2.4) (the reversal of what was done in 2.1)
                // once all ordering and ranking is done twins in each twin group need to be separated back into separate nodes
                this.reconnectTwins(disconnectedTwins);

                timer.printSinceLast("=== Ordering runtime: ");

                // if new ordering was computed need to discard any suggested positioning data
                suggestedLayout && suggestedLayout.positions && (delete suggestedLayout.positions);
            }

            // 3.1)
            if (suggestedLayout && suggestedLayout.positions) {
                // validate positions, if they are provided. If positions are valid, set this.positions
                if (this.validatePositions(suggestedLayout.positions)) {
                    this.positions = suggestedLayout.positions;
                }
            }

            // 3.2) if suggested layout did not specify positions or suggested positions were unuseable
            if (!this.positions) {
                this.positions = this.position();

                timer.printSinceLast("=== Positioning runtime: ");

                this.getHeuristics().improvePositioning();
            }

            // 4) update ancestors and vertical positioning of nodes and childhub edges
            this.updateSecondaryStructures();
        },

        // ranksBefore, rankYBefore: optional, used to make sure nodes do not move around vertically when they dont have to
        updateSecondaryStructures: function(ranksBefore, rankYBefore, keepSameRankY)
        {
            var timer = new Helpers.Timer();

            var ancestors = this.findAllAncestors();
            this.ancestors = ancestors.ancestors;
            this.consangr  = ancestors.consangr;
            timer.printSinceLast("=== Ancestors runtime: ");

            this.vertLevel = this.positionVertically();
            if (!keepSameRankY) {
                this.rankY = this.computeRankY(ranksBefore, rankYBefore);
            } else {
                // usually this branch is taken after a deletion. Make sure rankY legnth is correct
                this.rankY.splice(this.maxRank+1);
            }
            timer.printSinceLast("=== Vertical spacing runtime: ");
        },

        getHeuristics: function()
        {
            return this.heuristics;
        },

        //=[rank]============================================================================
        rank: function (baseG, suggestedRanks)
        {
            // use the suggested ranks, if available.
            // Note: even if present, using suggested ranks may fail if inconsistencies are found
            //       e.g. if a user drew a new "child" edge from a node to a node with a higher or equal rank
            if (suggestedRanks) {
                var ranks = this.init_rank(baseG, suggestedRanks);
                if (ranks) {
                    return ranks;   // if suggested ranks are valid. if not ranks would be null
                }
            }

            // initial ranking via a spanning tree. Minimum rank == 1.
            var ranks = this.init_rank(baseG);

            // re-rank all nodes as far down the tree as possible (e.g. people with no
            // parents in the tree should be on the same level as their first documented
            // relationship partner)
            this.lower_ranks(baseG, ranks);

            return ranks;
        },

        // init ranks by computing a spanning tree over the directed graph, starting from the nodes with no parents
        init_rank: function (baseG, suggestedRanks)
        {
            //   Algorithm: nodes are placed in the queue when they have no unscanned in-edges.
            //   As nodes are taken off the queue, they are assigned the least rank
            //   that satisfies their in-edges, and their out-edges are marked as scanned.
            //
            //   [precondition] graph must be acyclic.

            if (baseG.v.length == 0) return [];

            var initRank = 1;

            var ranks            = [];       // index == vertexID, ranks[v] == rank assigned to v
            var numRankedParents = [];       // index == vertexID, numRanked[v] == number of nodes which have
                                             //                    edges to v which were already assigned a rank

            for (var i = 0; i < baseG.getNumVertices(); i++) {
                ranks.push(-1);
                numRankedParents.push(0);
            }

            var queue = new Queue();         // holds non-ranked nodes which have all their parents already ranked

            if (suggestedRanks) {
                var minSuggestedRank = Math.min.apply(null, suggestedRanks);
                for (var v = 0; v < baseG.getNumVertices(); v++) {
                    if (baseG.isPerson(v)) {
                        if (suggestedRanks.length > v) {
                            // since `suggestedRanks` supposedly contains ranks after the graph is "compressed" for final output,
                            // any suggested rank should be converted to the rank which work at this stage in the algorithm
                            // (the difference is that relationships occupy their own rank at this stage)
                            //
                            // Conversion logic:
                            //  initial ranks: X, X+2, X+4, etc  (persons are ranked on every other rank, starting with some rank X)
                            //  converted ranks: 1, 4, 7, etc    (persons are ranked on every 3rd rank, starting with rank 1)
                            var convertedRank = suggestedRanks[v] - minSuggestedRank;
                            if (convertedRank % 2 != 0) {
                                return null;   // assumption failed: persons are ranked on every other rank
                            }
                            convertedRank = (convertedRank/2 * 3) + 1;  // top generation is ranked 1, next is 4, etc.
                            ranks[v] = convertedRank;
                        } else {
                            return null;  // a person node is without a rank => suggestedRanks are bad
                        }
                    }
                    if (baseG.isRelationship(v)) {
                        queue.push(v);
                    }
                }
            } else {
                var parentlessNodes = baseG.getLeafAndParentlessNodes().parentlessNodes;
                queue.setTo(parentlessNodes);
            }

            while ( queue.size() > 0 ) {
                var nextNode = queue.pop();

                // ...assign the least rank satisfying nextParent's in-edges (which is max(parent_ranks) + 1)
                var inEdges = baseG.getInEdges(nextNode);
                var useRank = initRank;
                for (var i = 0; i < inEdges.length; i++) {
                    var v = inEdges[i];
                    if (ranks[v] >= useRank)
                        useRank = ranks[v] + 1;
                }

                ranks[nextNode] = useRank;

                // add edge to spanning tree (if we need the tree):
                //  parent[nextNode] = useParent;
                //
                //  if (useParent !== undefined)
                //      spanTreeEdges[useParent].push(nextNode);

                // ...mark out-edges as scanned
                var outEdges = baseG.getOutEdges(nextNode);

                for (var u = 0; u < outEdges.length; u++) {
                    var vertex = outEdges[u];

                    if (suggestedRanks && ranks[vertex] != -1) {
                        if (ranks[vertex] <= ranks[nextNode]) {
                            // suggested ranks are inconsistent
                            return null;
                        }
                    }

                    numRankedParents[vertex]++;

                    var numVertexInEdges = baseG.getInEdges(vertex).length;

                    if (numRankedParents[vertex] == numVertexInEdges) {
                        queue.push(vertex);    // all potential parents are ranked, now we can rank the vertex itself
                    }
                }
            }

            return ranks;
        },

        lower_ranks: function (baseG, ranks)
        {
            if (ranks.length <= 1) return; // no nodes or only 1 node

            // re-ranks all nodes as far down the tree as possible (e.g. people with no
            // parents in the tree should be on the same level as their first documented
            // relationship partner, or as low as possible given their children relationships)

            // Algorithm:
            // 1. find disconnected components when multi-rank edges are removed (using "flood fill")
            // 2. for each component find the incoming or outgoing milti-rank edge of minimum length
            //    note1: sometimes a component may have both incoming and outgoing muti-rank edges;
            //           only one of those can be shortened and the choice is made based on edge weight
            //    note2: we can only keep track of outgoing edges as for each incoming edge there is an
            //           outgoing edge in another component, and we only shorten one edge per re-ranking iteration
            // 3. reduce all ranks by that edge's length minus 1
            // 4. once any two components are merged need to redo the entire process because the new
            //    resuting component may have other minimum in/out multi-rnak edges

            console.log("Re-ranking ranks before: " + Helpers.stringifyObject(ranks));

            while(true) {
                var nodeColor        = [];   // for each node which component it belongs to
                var component        = [];   // for each component list of vertices in the component
                var minOutEdgeInfo   = [];   // for each component length & weight of the shortest outgoing multi-rank edge

                for (var v = 0; v < baseG.getNumVertices(); v++) {
                    nodeColor.push(null);
                }

                var currentComponentColor = 0;
                for (var v = 0; v < baseG.getNumVertices(); v++) {

                    if (nodeColor[v] == null) {
                        // This node will be the first node of the next component, which
                        // includes all nodes reachable using non-multi-rank edges (any direction).
                        // All nodes in the component will be colored as "maxComponentColor"

                        var thisComponent = [];

                        var potentialLongEdges = {};

                        var queue = new Queue();
                        queue.push( v );

                        while ( queue.size() > 0 ) {
                            var nextV = queue.pop();

                            //console.log("processing: " + nextV);
                            if (nodeColor[nextV] != null) continue;

                            nodeColor[nextV] = currentComponentColor;
                            thisComponent.push(nextV);

                            var rankV = ranks[nextV];

                            var allEdges = baseG.getAllEdgesWithWeights(nextV);
                            for (var vv in allEdges) {
                                if (allEdges.hasOwnProperty(vv) && nodeColor[vv] != currentComponentColor) {
                                    var edgeLength = Math.abs(rankV - ranks[vv]);
                                    if (edgeLength == 1) {          // using only edges between neighbouring ranks
                                        if (nodeColor[vv] == null)
                                            queue.push(vv);         // add nodes not in any component to this one
                                    } else {
                                        // save all long edges into a separate list, and check it once component is fully computed
                                        if (allEdges[vv].out) {
                                            potentialLongEdges[vv] = {"length": edgeLength, "weight": allEdges[vv].weight };
                                        }
                                    }
                                }
                            }
                        }

                        component[currentComponentColor]      = thisComponent;
                        minOutEdgeInfo[currentComponentColor] = { "length": Infinity, "weight": 0 };

                        // go over all long edges originating from nodes in the component,
                        // and find the shortest long edge which goes out of component
                        for (var vv in potentialLongEdges) {
                            if (potentialLongEdges.hasOwnProperty(vv)) {
                                    if (nodeColor[vv] == currentComponentColor) continue;  // ignore nodes which are now in the same component

                                    var nextEdge = potentialLongEdges[vv];

                                    if (nextEdge.length < minOutEdgeInfo[currentComponentColor].length ||
                                        (nextEdge.length == minOutEdgeInfo[currentComponentColor].length &&
                                         nextEdge.weight > minOutEdgeInfo[currentComponentColor].weight) ) {
                                        minOutEdgeInfo[currentComponentColor] = nextEdge;
                                    }
                                }
                        }

                        currentComponentColor++;
                    }
                }


                //console.log("components: " + Helpers.stringifyObject(component));
                if (currentComponentColor == 1) return; // only one component - done re-ranking

                // for each component we should either increase the rank (to shorten out edges) or
                // decrease it (to shorten in-edges. If only in- (or only out-) edges are present there
                // is no choice, if there are both pick the direction where minimum length edge has higher
                // weight (TODO: alternatively can pick the one which reduces total edge len*weight more,
                // but the way pedigrees are entered by the user the two methods are probably equivalent in practice)

                // However we do not want negative ranks, and we can accomodate this by always increasing
                // the rank (as for each decrease there is an equivalent increase in the other component).

                // so we find the heaviest out edge and increase the rank of the source component
                // - in case of a tie use the shortest of the heaviest edges
                var minComponent = 0;
                for (var i = 1; i < component.length; i++) {
                  if (minOutEdgeInfo[i].weight > minOutEdgeInfo[minComponent].weight ||
                      (minOutEdgeInfo[i].weight == minOutEdgeInfo[minComponent].weight &&
                       minOutEdgeInfo[i].length <  minOutEdgeInfo[minComponent].length) ) {
                      minComponent = i;
                  }
                }

                //console.log("MinLen: " + Helpers.stringifyObject(minOutEdgeLength));

                // reduce rank of all nodes in component "minComponent" by minEdgeLength[minComponent] - 1
                for (var v = 0; v < component[minComponent].length; v++) {
                    ranks[component[minComponent][v]] += (minOutEdgeInfo[minComponent].length - 1);
                }

                //console.log("Re-ranking ranks update: " + Helpers.stringifyObject(ranks));
            }

            console.log("Ranks after re-ranking: " + Helpers.stringifyObject(ranks));
        },
        //============================================================================[rank]=


        //=[ordering]========================================================================
        ordering: function(maxInitOrderingBuckets, maxOrderingIterations, disconnectedTwins)
        {
            if (this.GG.v.length == 0) return new Ordering([],[]);  // empty graph

            var best          = undefined;
            var bestCrossings = Infinity;

            // we find leaf nodes and rootless nodes because removing some of them improves both speed and quality of ordering algorithms
            // (e.g. we know that leaf siblings should be placed together, so might as well just leave one in the graph for ordering purposes;
            //  similarly if there is apartner with no parents and no other partnerships it is easy to position that person next to his or her
            //  partner once everything else is positioned, and removing nodes speed all the graph-traversal & edge-cross-computation algorithms)
            var leafAndRootlessInfo = this.GG.getLeafAndParentlessNodes();

            var rootlessPartners = this.findAllRootlessPartners(leafAndRootlessInfo);
            this.disconnectRootlessPartners(rootlessPartners);

            var leafSiblings = this.findLeafSiblings(leafAndRootlessInfo);
            this.disconnectLeafSiblings(leafSiblings);

            var permutationsRoots = this.computePossibleParentlessNodePermutations(maxInitOrderingBuckets, leafAndRootlessInfo, rootlessPartners);
            var permutationsLeafs = this.computePossibleLeafNodePermutations(maxInitOrderingBuckets, leafAndRootlessInfo, leafSiblings, disconnectedTwins);

            var initOrderIterTotal = 0;  // just for reporting

            var useStack = false;

            var timer = new Helpers.Timer();

            //permutationsRoots = [[27, 0, 5, 4, 8, 9, 1, 28]];
            //useStack = true;

            while ( true ) {
                //var timer2 = new Helpers.Timer();
                for (var initOrderIter = 0; initOrderIter < permutationsRoots.length; initOrderIter++ ) {
                    initOrderIterTotal++;

                    order = this.init_order_top_to_bottom(permutationsRoots[initOrderIter], useStack);

                    this.transpose(order, false, bestCrossings*4 + 5);  // fix locally-fixable edge crossings,
                                                                        // but do not bother doing minor optimizations (for performance reasons)

                    var numCrossings = this.edge_crossing(order);

                    if ( numCrossings < bestCrossings ) {
                        best          = order.copy();
                        bestCrossings = numCrossings;
                        //console.log("UsingP: " + Helpers.stringifyObject(permutationsRoots[initOrderIter]) + " " + useStack.toString() + "  SCORE: " + numCrossings);
                        if ( numCrossings == 0 ) break;
                    }
                }
                //timer2.printSinceLast("Top-to-bottom: ");

                if ( bestCrossings == 0 ) break;
                for (var initOrderIter2 = 0; initOrderIter2 < permutationsLeafs.length; initOrderIter2++ ) {
                    initOrderIterTotal++;

                    order = this.init_order_bottom_to_top(permutationsLeafs[initOrderIter2], useStack);

                    this.transpose(order, false, bestCrossings*4 + 5);  // fix locally-fixable edge crossings

                    var numCrossings = this.edge_crossing(order);

                    if ( numCrossings < bestCrossings ) {
                        best          = order.copy();
                        bestCrossings = numCrossings;
                        //console.log("UsingL: " + Helpers.stringifyObject(permutationsLeafs[initOrderIter2]) + " " + useStack.toString() + "  SCORE: " + numCrossings);
                        if ( numCrossings == 0 ) break;
                    }
                }
                //timer2.printSinceLast("Bottom-to-top: ");

                if ( bestCrossings == 0 ) break;
                if ( useStack ) break;
                useStack = true;
            }

            timer.printSinceLast("Initial ordering: ");
            var bestEdgeLengthScore = this.edge_length_score(best);

            console.log("Initial ordering: " + Helpers.stringifyObject(best.order));
            console.log("Initial ordering:  numCrossings= " + bestCrossings + ",  edhgeLengthScore= " + bestEdgeLengthScore);

            //this.reconnectRootlessPartners(best, rootlessPartners);
            //this.reconnectLeafSiblings(best, leafSiblings);
            //return best;

            var noChangeIterations = 0;

            var order = best.copy();

            for (var i = 0; i < maxOrderingIterations; i++) {
                //if (bestCrossings == 0) break;   // still want to optimize for edge lengths

                // try to optimize based on a heuristic: just do it without checking if the result
                // is good or not. The layout may be not as good rigth away but better after a few
                // iterations
                var changed = this.wmedian(order, i);

                //console.log("median: " + Helpers.stringifyObject(order.order));

                // try to optimize locally (fix easily-fixable edge crossings, put children
                // and partners closer to each other) checking if each step is useful and
                // discarding bad adjustments (i.e. guaranteed to either improve or leave as is)
                this.transpose(order, true);

                //console.log("transpose: " + Helpers.stringifyObject(order.order));

                var numCrossings = this.edge_crossing(order);

                var edgeLengthScore = this.edge_length_score(order);

                if ( numCrossings < bestCrossings ||
                     (numCrossings == bestCrossings && edgeLengthScore < bestEdgeLengthScore) )
                {
                    console.log("ordering: new better one selected (" + numCrossings + " crossings, " + edgeLengthScore + " edgeLengthScore)");

                    best                = order.copy();
                    bestCrossings       = numCrossings;
                    bestEdgeLengthScore = edgeLengthScore;
                    noChangeIterations  = 0;
                }
                else {
                    if (!changed) noChangeIterations++;
                    if (noChangeIterations == 6) break;
                }
            }

            this.transpose(best, true);

            this.reconnectRootlessPartners(best, rootlessPartners);
            this.transpose(best, true);

            // proband father left/mother right check
            this.adjustProbandParentOrder(best);

            this.reconnectLeafSiblings(best, leafSiblings);
            //this.transpose(best, true);

            timer.restart();

            //console.log("-----> " + Helpers.stringifyObject(this.GG));

            // try to optimize long edge placement (bad adjustments are discarded)
            var newBestCrossings = this.transposeLongEdges(best, bestCrossings);

            timer.printSinceLast("Ordering long edges: ");

            console.log("Ordering stats:  initOrderIter= " + initOrderIterTotal + ",  reOrderingIter= " + i + ",  noChangeIterations= " + noChangeIterations);
            console.log("Final ordering: " + Helpers.stringifyObject(best.order));
            console.log("Final ordering:  numCrossings= " + newBestCrossings);

            return best;
        },

        findAllRootlessPartners: function (leafAndRootlessInfo)
        {
            // finds all people without parents which are only connected to one non-rootless node.
            // we know it should be placed right next to that partner in any optimal ordering

            var rootlessPartners = {};

            for (var i = 0; i < leafAndRootlessInfo.parentlessNodes.length; i++) {
                var v = leafAndRootlessInfo.parentlessNodes[i];

                if ( this.GG.getOutEdges(v).length == 1) {
                    var relationShipNode = this.GG.downTheChainUntilNonVirtual(this.GG.getOutEdges(v)[0]);

                    var parents = this.GG.getParents(relationShipNode);

                    var otherParent = (parents[0] == v) ? parents[1] : parents[0];

                    // Note: can't ignore nodes which are parentless and are connected to only one partner,
                    //       but are not on the same rank with the partner. Ow they are dropped and the long edge
                    //       hangs with no input node and is not traversed/assigned an order when doing the ordering pass
                    if (this.ranks[v] != this.ranks[otherParent]) {
                        continue;
                    }

                    //if (this.GG.getInEdges(otherParent).length > 0 || v > otherParent || this.GG.getOutEdges(otherParent).length > 1) {
                    if (this.GG.getInEdges(otherParent).length > 0) {
                        if (rootlessPartners[otherParent])
                            rootlessPartners[otherParent].push(v);
                        else
                            rootlessPartners[otherParent] = [v];
                    }
                }
            }
            //console.log("Found rootless partners: " + Helpers.stringifyObject(rootlessPartners));
            return rootlessPartners;
        },

        disconnectRootlessPartners: function(rootlessPartners)
        {
            for (var p in rootlessPartners) {
                if (rootlessPartners.hasOwnProperty(p)) {
                    var rootless = rootlessPartners[p];
                    for (var i = 0; i < rootless.length; i++) {
                        var v        = rootless[i];
                        var outEdges = this.GG.getOutEdges(v);
                        var relNode  = outEdges[0];

                        // remove edge v->relNode from the graph. Only remove the in-edge, as outedge will never be scanned
                        Helpers.removeFirstOccurrenceByValue(this.GG.inedges[relNode], v);  // was [p,v], becomes [p]
                    }

                }
            }
        },

        reconnectRootlessPartners: function(order, rootlessPartners)
        {
            //console.log("Order before reconnect rootless: " + Helpers.stringifyObject(order));
            for (var p in rootlessPartners) {
                if (rootlessPartners.hasOwnProperty(p)) {

                    // now check all rootless partners of p. Will place them next to p in the ordering

                    var partnersToTheLeft  = 0;
                    var partnersToTheRight = 0;
                    var pOrder             = order.vOrder[p];
                    var rank               = this.ranks[p];

                    var pRelationships = this.GG.getOutEdges(p);
                    for (var j = 0; j < pRelationships.length; j++) {
                        if ( this.GG.isRelationship(pRelationships[j]) ) {   // ignore long edges
                            var parents = this.GG.getInEdges(pRelationships[j]);
                            if ( parents.length == 2 ) { // e.g. skip relationships with removed parents
                                var partner = (parents[0] == p) ? parents[1] : parents[0];
                                if ( order.vOrder[partner] > pOrder )
                                    partnersToTheRight++;
                                else
                                    partnersToTheLeft++;
                            }
                        }
                    }

                    var rootless = rootlessPartners[p];
                    for (var i = 0; i < rootless.length; i++) {
                        var v        = rootless[i];
                        var outEdges = this.GG.getOutEdges(v);
                        var relNode  = outEdges[0];

                        // add back the v->relNode edge
                        this.GG.inedges[relNode].push(v);   // was [p], becomes [p,v]

                        // insert v into ordering: insert on the side with less partners.
                        // ideally we should insert according to the relationship node order, but that will be auto-fixed by transpose()

                        if (partnersToTheRight <= partnersToTheLeft) {
                            partnersToTheRight++;
                            order.insert(rank, pOrder+1, v);
                        }
                        else {
                            partnersToTheLeft++;
                            order.insert(rank, pOrder, v);
                        }
                    }

                }
            }
            //console.log("Order after reconnect rootless: " + Helpers.stringifyObject(order));
       },

        findLeafSiblings: function (leafAndRootlessInfo)
        {
            // finds all siblings of non-leaf people which are leaves

            var leafSiblings = {};

            for (var i = 0; i < leafAndRootlessInfo.leafNodes.length; i++) {
                var v = leafAndRootlessInfo.leafNodes[i];

                var childHubNode = this.GG.getInEdges(v)[0];

                if (leafSiblings.hasOwnProperty(childHubNode)) continue;  // we've already processed children of this childhub

                var children = this.GG.getOutEdges(childHubNode);

                if (children.length > 1) {
                    leafSiblings[childHubNode] = [];

                    var keepChild = v;  // need to keep at least one leaf per childhub so that
                                        // bottom-to-top ordering has an option of juggling with this leaf/childhub

                    for (var j = 0; j < children.length; j++) {
                        var child  = children[j];
                        var outNum = this.GG.getOutEdges(child).length;

                        if (child != keepChild && outNum == 0 )
                            leafSiblings[childHubNode].push(child);
                    }
                }
            }

            //console.log("Found leaf siblings: " + Helpers.stringifyObject(leafSiblings));
            return leafSiblings;
        },

        disconnectLeafSiblings: function(leafSiblings)
        {
            for (var p in leafSiblings) {
                if (leafSiblings.hasOwnProperty(p)) {
                    var leaves = leafSiblings[p];
                    for (var i = 0; i < leaves.length; i++) {
                        var l = leaves[i];

                        // remove edge p->l from the graph
                        // (don't bother about the weight, and ignore in-edge as it wil never be scanned before re-connection)

                        Helpers.removeFirstOccurrenceByValue( this.GG.v[p], l);
                    }
                }
            }
        },

        reconnectLeafSiblings: function(order, leafSiblings)
        {
            //console.log("Order before reconnect leaves: " + Helpers.stringifyObject(order));
            for (var p in leafSiblings) {
                if (leafSiblings.hasOwnProperty(p)) {
                    var leaves = leafSiblings[p];
                    for (var i = 0; i < leaves.length; i++) {
                        var l = leaves[i];

                        //console.log("inserting: " + l);

                        // add back the p->l edge
                        this.GG.v[p].push(l);

                        var rank = this.ranks[l];

                        order.insert(rank, 0, l);

                        var bestO     = 0;
                        var bestCross = this.edge_crossing(order, rank);
                        var bestScore = this.edge_length_score(order,rank);

                        // insert l at the best possible order
                        for (var o = 0; o < order.order[rank].length-1; o++) {

                            order.exchange(rank, o, o+1);

                            var newEdgeCrossings = this.edge_crossing(order, rank);
                            var newLengthScore   = this.edge_length_score(order,rank);

                            //console.log("order: " + order.vOrder[l] + ", cross: " + newEdgeCrossings);

                            if (newEdgeCrossings < bestCross ||
                                (newEdgeCrossings == bestCross && newLengthScore < bestScore) ) {
                                bestO     = o+1;
                                bestCross = newEdgeCrossings;
                                bestScore = newLengthScore;
                            }
                        }

                        order.moveVertexToOrder(rank, order.vOrder[l], bestO);

                        //console.log("inserted at: " + order.vOrder[l]);
                    }
                }
            }
            //console.log("Order after reconnect leaves: " + Helpers.stringifyObject(order));
        },

        disconnectTwins: function()
        {
            var disconnectedTwins = {};

            var handled = {};
            for (var v = 0; v <= this.GG.getMaxRealVertexId(); v++) {
                if (handled[v]) continue;
                if (!this.GG.isPerson(v)) continue;
                var twinGroupId = this.GG.getTwinGroupId(v);
                if (twinGroupId == null) continue;

                disconnectedTwins[v] = [];

                var childhub = this.GG.getInEdges(v)[0];
                var allTwins = this.GG.getAllTwinsOf(v);
                for (var i = 0; i < allTwins.length; i++) {
                    var twin = allTwins[i];
                    if (twin == v) continue;

                    // 1) remove connection from childhub to twin
                    // 2) replace in-edges for all nodes twin connects to by an inedge from v
                    // 3) add all twin's outedges to v
                    // 4) add twin to the backup list of twins of v

                    // 1
                    Helpers.removeFirstOccurrenceByValue( this.GG.v[childhub], twin);
                    // 2 + 3
                    var outEdges = this.GG.getOutEdges(twin);
                    for (var j = 0; j < outEdges.length; j++) {
                        var rel = outEdges[j];
                        Helpers.replaceInArray(this.GG.inedges[rel], twin, v);
                        this.GG.v[v].push(rel);
                        // need to keep in mind the special case of two twins in a relationship
                        if (this.GG.weights[v].hasOwnProperty(rel))
                            this.GG.weights[v][rel] += this.GG.weights[twin][rel];    // sum the weights
                        else
                            this.GG.weights[v][rel] = this.GG.weights[twin][rel];     // use the other twin's weight
                    }
                    // 4
                    disconnectedTwins[v].push(twin);

                    handled[twin] = true;

                    //console.log("REMOVED TWIN " + twin);
                }
            }

            return disconnectedTwins;
        },

        reconnectTwins: function(disconnectedTwins)
        {
            //console.log("Order before reconnect twins: " + Helpers.stringifyObject(this.order));
            for (var v in disconnectedTwins) {
                if (disconnectedTwins.hasOwnProperty(v)) {

                    var rank = this.ranks[v];

                    var childhub = this.GG.getInEdges(v)[0];

                    var allDisconnectedTwins = disconnectedTwins[v];

                    // sort twins by number of reationships, so that twins with no relationships are inserted last
                    var GG = this.GG;
                    var byNumberOfRelationships = function(a,b) {
                               var an = GG.getOutEdges(a).length;
                               var bn = GG.getOutEdges(b).length;
                               return bn - an;
                            }
                    allDisconnectedTwins.sort( byNumberOfRelationships );

                    for (var i = 0; i < allDisconnectedTwins.length; i++) {
                        var twin = allDisconnectedTwins[i];

                        // 1) remove outedges which actually belong to twin from v (needed for next step)
                        // 2) find the position to reinsert the twin & insert it
                        // 3) restore connection from childhub to twin
                        // 4) restore in-edges for all nodes twin connects to to twin

                        //1
                        var outEdges = this.GG.getOutEdges(twin);
                        for (var j = 0; j < outEdges.length; j++) {
                            var rel = outEdges[j];
                            Helpers.removeFirstOccurrenceByValue(this.GG.inedges[rel], v);
                            Helpers.removeFirstOccurrenceByValue(this.GG.v[v], rel);

                            // need to keep in mind the spcial case of two twins in a relationship; if
                            // there is no relationship => weight[v][rel] == weight[twin][rel]
                            if (this.GG.weights[v][rel] == this.GG.weights[twin][rel])
                                delete this.GG.weights[v][rel];
                            else
                                // otherwise it is twice as big and need to cut in half to get back to original value
                                this.GG.weights[v][rel] -= this.GG.weights[twin][rel];

                            this.GG.inedges[rel].push(twin);
                        }
                        //2
                        var insertOrder = this.findBestTwinInsertPosition(twin, this.GG.getOutEdges(twin), this.order);
                        this.order.insert(rank, insertOrder, twin);
                        //3 + 4
                        this.GG.v[childhub].push(twin);

                        // handle special case of a relationship between two twins - best handle it after all twins have been reinserted
                        // tested by Testcase "3c"
                        var groupID  = this.GG.getTwinGroupId(twin);
                        var outEdges = this.GG.getOutEdges(twin);
                        for (var j = 0; j < outEdges.length; j++) {
                            var rel         = outEdges[j];
                            if (this.GG.isVirtual(rel)) continue;
                            var parents     = this.GG.getInEdges(rel);
                            var otherParent = (parents[0] == twin) ? parents[1] : parents[0];
                            if (this.GG.getTwinGroupId(otherParent) == groupID &&     // the other partner is this twin's twin
                                this.GG.hasEdge(childhub, otherParent)) {             // and both twins have been re-inserted already
                                // the twin just re-inserted has a relationship with another twin (which has been re-inserted as well)
                                //console.log("RELAT between " + twin + " and " + otherParent);
                                // TODO: can do a smarter thing and rearrange twins based on other relationships.
                                //       but since this is a rare case for now just do the simple improvement
                                var orderRel   = this.order.vOrder[rel];
                                var orderTwin1 = this.order.vOrder[twin];
                                var orderTwin2 = this.order.vOrder[otherParent];
                                if (Math.abs(orderTwin1 - orderTwin2) != 1) {
                                    if (this.GG.getOutEdges(twin).length == 1) {
                                        if (orderTwin1 < orderTwin2)
                                            this.order.moveVertexToOrder(rank, orderTwin1, orderTwin2);
                                        else
                                            this.order.moveVertexToOrder(rank, orderTwin1, orderTwin2+1);
                                    } else if (this.GG.getOutEdges(otherParent).length == 1) {
                                            if (orderTwin2 < orderTwin1)
                                                this.order.moveVertexToOrder(rank, orderTwin2, orderTwin1);
                                            else
                                                this.order.moveVertexToOrder(rank, orderTwin2, orderTwin1+1);
                                    } else
                                        continue; // twins are not next to each other and both have multiple relationships: TODO

                                    // update orders after possible re-arrangement of nodes
                                    orderRel   = this.order.vOrder[rel];
                                    orderTwin1 = this.order.vOrder[twin];
                                    orderTwin2 = this.order.vOrder[otherParent];
                                }
                                // insert rel inbetween the twins (e.g. after leftmost of the twins and before rightmost
                                //console.log("order rel: " + orderRel + ", Twin1: " + orderTwin1 + ", Twin2: " + orderTwin2);
                                if (orderTwin1 < orderTwin2)
                                    this.order.moveVertexToOrder(rank, orderRel, orderTwin2);
                                else
                                    this.order.moveVertexToOrder(rank, orderRel, orderTwin1);
                            }
                        }
                    }
                }
            }
            //console.log("Order after reconnect twins: " + Helpers.stringifyObject(this.order));
        },

        computePossibleParentlessNodePermutations: function(maxInitOrderingBuckets, leafAndRootlessInfo, rootlessPartners)
        {
            // 1) split all parentless nodes into at most maxInitOrderingBuckets groups/buckets
            // 2) compute all possible permutations of these groups discarding mirror copies (e.g. [1,2,3] and [3,2,1])
            // 3) return the list of permutations, with the default ordering first in the list

            var buckets = [];
            // 1) detect mini-groups: if two parentless nodes are connected by a relationship and not
            //    connected to anything else treat them as one node (they should be close to each other)
            // 2) split all nodes into at most maxInitOrderingBuckets buckets.
            //    note: if there are less nodes (counting a sub-group from #1 as one node) than max ##
            //           of buckets it is ok, if there are more then have to space them evenly into the buckets.
            // note: each bucket should be an array of node IDs, even if there is only one ID

            console.log("maxInitOrderingBuckets: " + maxInitOrderingBuckets);

            var handled = {};

            for (var p in rootlessPartners) {
                if (rootlessPartners.hasOwnProperty(p)) {
                    var rootless = rootlessPartners[p];
                    for (var i = 0; i < rootless.length; i++)
                        handled[rootless[i]] = true;   // those nodes will be automatically added at correct ordering later
                }
            }

            for (var i = 0; i < leafAndRootlessInfo.parentlessNodes.length; i++) {
                var v = leafAndRootlessInfo.parentlessNodes[i];

                if (handled.hasOwnProperty(v)) continue;

                var nextBucket = [];

                nextBucket.push(v);
                handled[v] = true;

                // essy grouping: place parents which are only connected to the same relationship in the same bucket
                if ( this.GG.getOutEdges(v).length == 1 ) {

                    var rel     = this.GG.downTheChainUntilNonVirtual(this.GG.getOutEdges(v)[0]);
                    var parents = this.GG.getParents(rel);

                    var otherPartner = (parents[0] == v) ? parents[1] : parents[0];

                    if (!handled.hasOwnProperty(otherPartner)
                        && this.GG.getInEdges (otherPartner).length == 0
                        && this.GG.getOutEdges(otherPartner).length == 1) {   // the other partner has no parents && only this relationhsip
                        nextBucket.push(otherPartner);
                        handled[otherPartner] = true;
                    }
                }

                buckets.push(nextBucket);
            }

            // if number of buckets is large, merge some (closely related) buckets
            // until the number of buckets is no more than the specified maximum
            if (buckets.length > maxInitOrderingBuckets)
                this.mergeBucketsUntilNoMoreThanGivenLeft(buckets, maxInitOrderingBuckets);

            var permutations = [];

            // Now compute all possible permutations of the buckets
            Helpers.permute2DArrayInFirstDimension( permutations, buckets, 0);

            Helpers.printObject(buckets);
            //Helpers.printObject(permutations);
            //permutations = [ leafAndRootlessInfo.parentlessNodes ];  //DEBUG: no permutations
            //permutations = [[5,4,0,1,2,9]];

            console.log("Found " + permutations.length + " permutations of parentless nodes");

            return permutations;
        },

        computePossibleLeafNodePermutations: function(maxInitOrderingBuckets, leafAndRootlessInfo, leafSiblings, disconnectedTwins)
        {
            // see computePossibleParentlessNodePermutations

            var buckets = [];
            // 1) detect mini-groups: if two leaf nodes are connected to a childhub and not
            //    connected to anything else treat them as one node (they should be close to each other)
            // 2) split all nodes into at most maxInitOrderingBuckets buckets.

            var handled = {};
            for (var p in leafSiblings) {
                if (leafSiblings.hasOwnProperty(p)) {
                    var leaves = leafSiblings[p];
                    for (var i = 0; i < leaves.length; i++)
                        handled[leaves[i]] = true;   // these nodes (leaves) will be automatically added at correct ordering later
                }
            }
            for (var p in disconnectedTwins) {
                if (disconnectedTwins.hasOwnProperty(p)) {
                    var twins = disconnectedTwins[p];
                    for (var i = 0; i < twins.length; i++)
                        handled[twins[i]] = true;   // these nodes (twis) will be automatically added at correct ordering later
                }
            }

            var nextBucket = 0;
            for (var i = 0; i < leafAndRootlessInfo.leafNodes.length; i++) {
                var v = leafAndRootlessInfo.leafNodes[i];

                if (handled.hasOwnProperty(v)) continue;

                var nextBucket = [];

                nextBucket.push(v);
                handled[v] = true;

                if ( this.GG.getInEdges(v).length != 1 )
                    throw "Assertion failed: only one in edge into a leaf node";
                var childhubNode = this.GG.getInEdges(v)[0];

                // find all nodes which are only connected to V's childhub
                for (var j = i+1; j < leafAndRootlessInfo.leafNodes.length; j++) {
                    var u = leafAndRootlessInfo.leafNodes[j];
                    if (handled.hasOwnProperty(u)) continue;

                    var childhubNodeU = this.GG.getInEdges(u)[0];

                    if (childhubNode == childhubNodeU)
                    {
                        nextBucket.push(u);
                        handled[u] = true;
                    }
                }

                buckets.push(nextBucket);
            }

            // if number of buckets is large, merge some (closely related) buckets
            // until the number of buckets is no more than the specified maximum
            if (buckets.length > maxInitOrderingBuckets)
                this.mergeBucketsUntilNoMoreThanGivenLeft(buckets, maxInitOrderingBuckets, true /* use in-edges when computing closeness */);

            var permutations = [];

            // Now compute all possible permutations of the buckets
            Helpers.permute2DArrayInFirstDimension( permutations, buckets, 0);

            Helpers.printObject(buckets);
            console.log("Found " + permutations.length + " permutations of leaf nodes");

            return permutations;
        },

        mergeBucketsUntilNoMoreThanGivenLeft: function(buckets, maxInitOrderingBuckets, useInEdges)
        {
            console.log("original buckets: " + Helpers.stringifyObject(buckets));

            while (buckets.length > maxInitOrderingBuckets && this.mergeMostRelatedBuckets(buckets, useInEdges));

            console.log("merged buckets: " + Helpers.stringifyObject(buckets));
        },

        mergeMostRelatedBuckets: function(buckets, useInEdges)
        {
            // 1. find two most related buckets
            // 2. merge the buckets

            //console.log("original buckets: " + Helpers.stringifyObject(buckets));

            var minDistance = Infinity;
            var bucket1     = 0;
            var bucket2     = 1;

            //var timer = new Helpers.Timer();

            for (var i = 0; i < buckets.length - 1; i++)
                for (var j = i + 1; j < buckets.length; j++)  {
                    var dist = this.findDistanceBetweenBuckets( buckets[i], buckets[j], useInEdges );

                    //console.log("distance between buckets " + i + " and " + j + " is " + dist);

                    // pick most closely related buckets for merging. Break ties by bucket size (prefer smaller resulting buckets)
                    if (dist < minDistance ||
                        (dist == minDistance && buckets[i].length + buckets[j].length < buckets[bucket1].length + buckets[bucket2].length) ) {
                        minDistance = dist;
                        bucket1     = i;
                        bucket2     = j;
                    }
                }

            //timer.printSinceLast("Compute distance between buckets: ");

            if (minDistance == Infinity) {
                throw "Assumption failed: unrelated buckets";
            }

            // merge all items from bucket1 into bucket2
            for (var i = 0; i < buckets[bucket2].length; i++) {
                buckets[bucket1].push(buckets[bucket2][i]);
            }

            buckets.splice(bucket2,1);  // remove bucket2

            //console.log("merged buckets: " + Helpers.stringifyObject(buckets));

            return true; // was able to merge some buckets
        },

        findDistanceBetweenBuckets: function(bucket1nodes, bucket2nodes, useInEdges)
        {
            // only looks for common relatives in one direction: using inEdges iff useInEdges and outEdges otherwise
            var distance = [];

            for (var i = 0; i < bucket1nodes.length; i++)
                distance[bucket1nodes[i]] = 1;
            for (var i = 0; i < bucket2nodes.length; i++)
                distance[bucket2nodes[i]] = -1;

            var queue1 = new Queue();
            queue1.setTo(bucket1nodes);

            var queue2 = new Queue();
            queue2.setTo(bucket2nodes);

            var iter = 0;  // safeguard against infinite loop
            while(iter < 100) {
                iter++;

                if (queue1.size() == 0 && queue2.size() == 0)
                    return Infinity;       // buckets are not related/not mergeable

                var nextQueue1 = new Queue();
                while (queue1.size() > 0){
                    var nextNode = queue1.pop();

                    var dist = distance[nextNode];

                    var edges = useInEdges ? this.GG.getInEdges(nextNode) : this.GG.getOutEdges(nextNode);

                    for (var j = 0; j < edges.length; j++) {
                        var nextV = edges[j];
                        if (distance[nextV] < 0)
                            return -distance[nextV] + dist;   // actually distance is (return_value - 1), but it does not matter for this algorithm
                        distance[nextV] = dist + 1;
                        nextQueue1.push(nextV);
                    }
                }
                queue1 = nextQueue1;

                var nextQueue2 = new Queue();
                while (queue2.size() > 0){
                    var nextNode = queue2.pop();

                    var dist = distance[nextNode];  // a negative number for nodes in queue2

                    var edges = useInEdges ? this.GG.getInEdges(nextNode) : this.GG.getOutEdges(nextNode);

                    for (var j = 0; j < edges.length; j++) {
                        var nextV = edges[j];
                        if (distance[nextV] > 0)
                            return distance[nextV] - dist;
                        distance[nextV] = dist - 1;
                        nextQueue2.push(nextV);
                    }
                }
                queue2 = nextQueue2;
            }

            throw "Assertion failed: possible loop detected";
        },

        init_order_top_to_bottom: function (parentlessNodes, useStack)
        {
            // initially orders the nodes in each rank by a depth-first or breadth-first
            // searches starting with vertices of minimum rank. Vertices are assigned positions
            // in their ranks in left-to-right order as the search progresses.

            var order  = [];          // array of arrays - for each rank list of vertices in order
            var vOrder = [];          // array - for each v vOrder[v] = order within rank

            for (var r = 0; r <= this.maxRank; r++)
                order[r] = [];

            for (var i = 0; i < this.GG.getNumVertices(); i++)
                vOrder[i] = undefined;

            // Use BFS -----------------------------
            var queue = useStack ? new Stack() : new Queue();
            queue.setTo(parentlessNodes);

            //console.log("Use stacK: " + useStack + ", parentless: " + Helpers.stringifyObject(parentlessNodes));

            while ( queue.size() > 0 ) {
                var next = queue.pop();
                // we may have already assigned this vertex a rank
                if (vOrder[next] != undefined) continue;

                // assign next available order at next's rank
                var rank = this.ranks[next];

                var nextOrder = order[rank].length;
                vOrder[next]  = nextOrder;
                order[rank].push(next);

                // add all children to the queue
                var outEdges = this.GG.getOutEdges(next);

                //alreadyOrderedSortFunc = function(a,b){return b-a};
                //outEdges.sort(alreadyOrderedSortFunc);

                for (var u = 0; u < outEdges.length; u++)
                    queue.push(outEdges[u]);
            }
            //--------------------------------------

            //var o = new Ordering(order, vOrder);
            //Helpers.printObject(o);
            //return o;
            return new Ordering(order, vOrder);
        },

        init_order_bottom_to_top: function (leafNodes, useStack)
        {
            // initially orders the nodes in each rank using depth-first or breadth-first
            // searches starting with he leaf vertices. Vertices are assigned positions in
            // their ranks in left-to-right order as the search progresses.

            var order  = [];          // array of arrays - for each rank list of vertices in order
            var vOrder = [];          // array - for each v vOrder[v] = order within rank

            for (var r = 0; r <= this.maxRank; r++)
                order[r] = [];

            for (var i = 0; i < this.GG.getNumVertices(); i++)
                vOrder[i] = undefined;

            var queue = useStack ? new Stack() : new Queue();
            queue.setTo(leafNodes);

            while ( queue.size() > 0 ) {
                var next = queue.pop();
                // we may have already assigned this vertex a rank
                if (vOrder[next] != undefined) continue;

                // assign next available order at next's rank
                var rank = this.ranks[next];

                var nextOrder = order[rank].length;
                vOrder[next]  = nextOrder;
                order[rank].push(next);

                // add all children to the queue
                var inEdges = this.GG.getInEdges(next);

                for (var u = 0; u < inEdges.length; u++)
                    queue.push(inEdges[u]);
            }

            return new Ordering(order, vOrder);
        },

        edge_length_score: function(order, onlyRank)
        {
            // Returns the penalty score that penalizes:
            //   higher priority: people in a relationship being far from each other
            //                    (higher penalty for greater distance)
            //   lower priority:  children of the same relationship not being close to each other
            //                    (higher penalty for greater distance between leftmost and rightmost child)
            //   lowest priority: father not being on the left, mother notbeing on the right
            //                    (constant penalty for each case)

            var totalEdgeLengthInPositions   = 0;
            var totalEdgeLengthInChildren    = 0;
            var totalPenaltyForFatherOnRight = 0;  // penalty for not having father on the left/mother on the right
            var totalPenaltyForChildAgeOrder = 0;  // penalty for not having children ordered by age

            for (var i = 0; i < this.GG.getNumVertices(); i++) {

                if (onlyRank) {
                    var rank = this.ranks[i];
                    if (rank < onlyRank - 1 || rank > onlyRank + 1) continue;
                }

                if (this.GG.isRelationship(i)) {
                    var parents = this.GG.getInEdges(i);

                    if (parents.length == 2) {     // while each relationship has 2 parents, during ordering some parents may be "unplugged" to simplify the graph
                        // only if parents have the same rank
                        if ( this.ranks[parents[0]] != this.ranks[parents[1]] )
                            continue;

                        var order1 = order.vOrder[parents[0]];
                        var order2 = order.vOrder[parents[1]];

                        totalEdgeLengthInPositions += Math.abs(order1 - order2);

                        // penalty, if any, for father on the left, mother on the right
                        var leftParent   = (order1 < order2) ? parents[0] : parents[1];
                        var genderOfLeft = this.GG.properties[leftParent]["gender"];
                        if (genderOfLeft == 'F')
                            totalPenaltyForFatherOnRight++;
                    }
                }

                if (this.GG.isChildhub(i)) {
                    // get the distance between the rightmost and leftmost child
                    var children = this.GG.getOutEdges(i);
                    if (children.length > 1) {
                        var orderedChildren = order.sortByOrder(children);

                        var minOrder = order.vOrder[orderedChildren[0]];
                        var maxOrder = order.vOrder[orderedChildren[orderedChildren.length-1]];
                        totalEdgeLengthInChildren += (maxOrder - minOrder);

                        var leftChildDOB = this.GG.properties[orderedChildren[0]].hasOwnProperty("dob") ?
                                           new PedigreeDate(this.GG.properties[orderedChildren[0]]["dob"]) : null;
                        for (var j = 1; j < orderedChildren.length; j++) {
                            var thisChildDOB = this.GG.properties[orderedChildren[j]].hasOwnProperty("dob") ?
                                               new PedigreeDate(this.GG.properties[orderedChildren[j]]["dob"]) : null;

                            if (thisChildDOB != null && thisChildDOB.isComplete()) {
                                if (leftChildDOB == null || !leftChildDOB.isComplete()) {
                                    // prefer all without date of birth to be on the right, i.e. penalty for no date on the left
                                    totalPenaltyForChildAgeOrder++;
                                } else {
                                    // both are not null: compare dates
                                    if (leftChildDOB.getTime() > thisChildDOB.getTime()) {
                                        // penalty for older child on the right
                                        totalPenaltyForChildAgeOrder++;
                                    }
                                }
                            }
                            leftChildDOB = thisChildDOB;
                        }
                    }
                }
            }

            //console.log("r = " + onlyRank + ", edgeLength = " + totalEdgeLengthInPositions + ", childLen = " + totalEdgeLengthInChildren);
            return totalEdgeLengthInPositions*100000 + totalEdgeLengthInChildren*1000 + totalPenaltyForFatherOnRight*5 + totalPenaltyForChildAgeOrder;
        },

        edge_crossing: function(order, onlyRank, dontUseApproximationForRelationshipEdges)
        {
            // Counts edge crossings in the graph accoring to given node ordering.
            //
            // Iff onlyRank is defined, only counts edge crossings affected by re-ordering of
            // nodes on given rank.
            //
            // Assumes that edges always go from lower ranks to higher ranks or
            // between nodes of the same rank

            var numCrossings = 0.0;

            var rankFrom = onlyRank ? Math.max(1, onlyRank - 1) : 1;
            var rankTo   = onlyRank ? onlyRank : this.maxRank;

            //Helpers.printObject(order);
            for (var r = rankFrom; r <= rankTo; ++r) {
                var numVert = order.order[r].length;

                // TODO: investigate about person nodes at the last position, and not counting crossings due to its relationship
                //       when "-1" is removed testcase "abraham" incorrectly places "rachel" on the other side of twins
                for (var i = 0; i < numVert-1; ++i) {   // -1 because we only check crossings of edges going out of vertices of higher orders
                    var v = order.order[r][i];

                    var isChhub  = this.GG.isChildhub(v);
                    var outEdges = this.GG.getOutEdges(v);
                    var len      = outEdges.length;

                    for (var j = 0; j < len; ++j) {
                        var targetV = outEdges[j];

                        // special considerations: after ordering is done all relationship nodes will be
                        // re-ranked one level higher. In most cases the number of edge crossings is the
                        // same, however it may not be. For most cases the following heuristic results
                        // in optimal arrangement:
                        // for each relationship node add the number of crossings equal to the number of
                        // nodes on the parent rank between it's parents according to current ordering
                        if ( !dontUseApproximationForRelationshipEdges && this.GG.isRelationship(targetV) ) {
                            var parents = this.GG.getInEdges(targetV);
                            if (parents.length == 2) {
                                var order1    = order.vOrder[parents[0]];
                                var order2    = order.vOrder[parents[1]];
                                var crossings = this.numNodesWithParentsInBetween(order, r, order1, order2);
                                numCrossings += crossings/2; // only assign it half a crossing because most will be fixed by transpose()
                                                             // - and if "1" is assigned transpose wont fix certain local cases
                            }
                            //if (i == numVert-1) continue;
                        }

                        // so we have an edge v->targetV. Have to check how many edges
                        // between rank[v] and rank[targetV] this particular edge corsses.
                        var crossings = this._edge_crossing_crossingsByOneEdge(order, v, targetV);

                        // special case: count edges from parents to twins twice
                        // (since all twins are combined into one, and this edge actually represents multiple parent-child edges)
                        //var twinCoeff = (isChhub && this.GG.isParentToTwinEdge(v, targetV)) ? 2.0 : 1.0;
                        var twinCoeff = (isChhub && this.GG.getTwinGroupId(targetV) != null) ? 2.0 : 1.0;

                        numCrossings += crossings * twinCoeff;
                    }
                }
            }

            //if (!onlyRank)
            //    console.log("crossings: " + numCrossings);

            return numCrossings;
        },

        _edge_crossing_crossingsByOneEdge: function (order, v, targetV)
        {
            // Crossing occurs if either
            // 1) there is an edge going from rank[v]-ranked vertex with a smaller order
            //     than v to a rank[targetV]-ranked vertex with a larger order than targetV
            // 2) there is an edge going from rank[v]-ranked vertex with a larger order
            //     than v to a rank[targetV]-ranked vertex with a smaller order than targetV
            //
            // However we don't want to count each crossing twice (once for each edge), so
            // here we only count crossings in the 2nd case. The first case will be counted
            // when we process that other vertex

            var rankV = this.ranks[v];
            var rankT = this.ranks[targetV];

            var orderV = order.vOrder[v];
            var orderT = order.vOrder[targetV];

            if (rankV == rankT) {
                return this.numNodesWithParentsInBetween(order, rankV, orderV, orderT);
            }

            var crossings = 0;

            var verticesAtRankV = order.order[ rankV ];    // all vertices at rank V

            // edges from rankV to rankT: only those after v (orderV+1)
            for (var ord = orderV+1; ord < verticesAtRankV.length; ++ord) {
                var vertex = verticesAtRankV[ord];

                var isChhub  = this.GG.isChildhub(vertex);
                var outEdges = this.GG.getOutEdges(vertex);
                var len      = outEdges.length;

                for (var j = 0; j < len; ++j) {
                    var target = outEdges[j];

                    if (order.vOrder[target] < orderT) {
                        crossings++;

                        // special case: count edges from parents to twins twice
                        // (since all twins are combined into one, and this edge actually represents multiple parent-child edges)
                        //if (isChhub && this.GG.isParentToTwinEdge(vertex, target))
                        if (isChhub && this.GG.getTwinGroupId(target) != null)
                            crossings++;
                    }
                }
            }

            // prefer long edges to cross other edges at the point they originate from, since
            // this generaly results in better long edge placement once head segment on the same
            // rank with person is added
            if (crossings > 0 && this.GG.isPerson(v) && this.GG.isVirtual(targetV))
                crossings -= 0.1;

            return crossings;
        },

        numNodesWithParentsInBetween: function (order, rank, order1, order2)
        {
            // TODO: while this function correctly computes what its name suggests, it is
            //       actually used to compute number of crossings for same-rank edges. And for that
            //       need not only to sompute nodes with parents, but correctly compute crossings of
            //       other same-rank edges (relationship edges). The difference is:
            //       - even if a node between order1 and order2 has an in-edge, that in-edge may not cross the
            //         edge from order1 to order2 because both source and target are between order1 & order2
            //       - it may be an out-edge instead of an in-edge, but still crosses as source is inside,
            //         but target is outside [order1, order2]

            var numNodes = 0;
            var fromBetween = Math.min(order1, order2) + 1;
            var toBetween   = Math.max(order1, order2) - 1;
            for (var o = fromBetween; o <= toBetween; o++) {
                var b = order.order[rank][o];

                if (this.GG.getInEdges(b).length > 0) {
                    // 1. edges which come from higher ranks create a crosing
                    // 2. edges that come from the same rank outside the order1-order2 interval create a crossing
                    for (var i = 0; i < this.GG.getInEdges(b).length; i++) {
                        var u = this.GG.getInEdges(b)[i];

                        if (this.ranks[u] != rank) {
                            numNodes++;                     // 1. u is of higher rank
                        }
                        else {
                            var orderOther = order.vOrder[u];
                            if (orderOther < (fromBetween-1) || orderOther > (toBetween+1)) // +1 to skip vertex itself, e.g. multiple relationships
                                numNodes++;                 // 2. same rank from outside [order1,order2];
                        }
                    }
                    //numNodes++;
                }

                if (this.GG.isPerson(b)) {
                    // count crossing twin's parental edge as a multiple crossing
                    // (since all twins are combined into one, and this one parent edge actually represents multiple edges)
                    var twinGroupId = this.GG.getTwinGroupId(b);
                    if (twinGroupId != null) {
                        numNodes++;
                    }
                }
            }
            return numNodes;
        },

        //-[wmedian]-------------------------------------------------------------------------
        wmedian: function(order, iter)
        {
            // The weighted median heuristic: depending on the parity of the current iteration number,
            // the ranks are traversed from top to bottom or from bottom to top.

            var changed = false;

            if (iter%2 == 0)
            {
                for (var r = 2; r <= this.maxRank; r++) {
                    if (order.order[r].length   <= 1 ||            // no need to re-order 1 vertex
                        order.order[r-1].length <= 1) continue;    // if only one same parent for all V:
                                                                   // all V will have equivalen median[]
                    var median = [];
                    var len    = order.order[r].length;
                    for (var i = 0; i < len; i++) {
                        var v = order.order[r][i];
                        median[v] = this.median_value(order, v, r-1);
                    }
                    changed != this.sort_orders(order, r, median);
                }
            }
            else
            {
                for (var r = this.maxRank-1; r >= 1; r--) {
                    if (order.order[r].length   <= 1 ||            // no need to re-order 1 vertex
                        order.order[r+1].length <= 1) continue;    // if only one same child for all V

                    var median = [];
                    var len    = order.order[r].length;
                    for (var i = 0; i < len; i++) {
                        var v = order.order[r][i];
                        median[v] = this.median_value(order, v, r+1);
                    }
                    changed != this.sort_orders(order, r, median);
                }
            }

            return changed;
        },

        median_value: function (order, v, adj_rank)
        {
            var P = this.adj_position(order, v, adj_rank);

            if (P.length == 0) return -1.0;

            var m = Math.floor(P.length/2);

            if (P.length % 2 == 1) return P[m];

            if (P.length == 2) return (P[0] + P[1])/2;

            var left  = P[m-1]        - P[0];
            var right = P[P.length-1] - P[m];

            return (P[m-1]*right + P[m]*left)/(left+right);
        },

        adj_position: function (order, v, adj_rank)
        {
            // returns an ordered array of the present positions of the nodes
            // adjacent to v in the given adjacent rank.
            var result = [];

            var verticesAtRankAdj = order.order[adj_rank];  // all vertices at rank adj_rank

            var len = verticesAtRankAdj.length;
            for (var j = 0; j < len; j++) {
                var adjV = verticesAtRankAdj[j];
                if ( this.GG.hasEdge(adjV, v) || this.GG.hasEdge(v, adjV) ) {
                    result.push(j);      // add order
                }
            }

            return result;
        },

        sort_orders: function(order, rank, weightToUseForThisRank) {

            var sortfunc = function(a,b) {
                return (weightToUseForThisRank[a] - weightToUseForThisRank[b]);
            };

            // re-order vertices within a rank according to weightToUseForThisRank
            order.order[rank].sort( sortfunc );

            var changed = false;

            // update order.vOrder[] accordingly, based on how we just sorted order.order[]
            for (var i = 0; i < order.order[rank].length; i++) {
                var v = order.order[rank][i];
                if (order.vOrder[v] != i )     // if it is not already at what it is
                {
                    order.vOrder[v] = i;
                    changed = true;
                }
            }

            return changed;
        },
        //-------------------------------------------------------------------------[wmedian]-

        isChhubsOrderOK: function(rank, order)
        {
            for (var i = 0; i < order.order[rank].length - 1; i++) {
                var v = order.order[rank][i];
                if (!this.GG.isChildhub(v)) continue;

                // take the next childhub to the right: skip all virtual edge pieces
                for (var next = i+1; next < order.order[rank].length; next++) {
                    if (this.GG.isChildhub(order.order[rank][next])) break;
                }
                var u = order.order[rank][next];
                if (!this.GG.isChildhub(u)) continue;

                var aboveV = this.GG.getInEdges(v)[0];
                var aboveU = this.GG.getInEdges(u)[0];

                if (order.vOrder[aboveV] > order.vOrder[aboveU])
                    return false;
            }
            return true;
        },

        placeChhubsInCorrectOrder: function(rank, order)
        {
            var GG = this.GG;
            var byRelOrder = function(a,b) {
                   var above1 = GG.getInEdges(a)[0];
                   var above2 = GG.getInEdges(b)[0];
                   if (above1 == above2) {
                       // keep existing order of pieces of virtual edges as well
                       return order.vOrder[a] - order.vOrder[b];
                   }
                   return order.vOrder[above1] - order.vOrder[above2];
                }
            order.order[rank].sort(byRelOrder);

            for (var i = 0; i < order.order[rank].length; i++)
                order.vOrder[ order.order[rank][i] ] = i;
        },

        adjustProbandParentOrder: function(order)
        {
            if (this.probandId >=0) {
                var motherFather = this.GG.getMotherFather(this.probandId);

                var isOrderWrong = function(v, otherV, shouldBeLeft) {
                    for (var i = 0; i < otherV.length; i++) {
                        if (this.ranks[v] == this.ranks[otherV[i]])
                            // either order should be less and shouldBeLeft, or order greater and !shouldBeLeft
                            // if not so, order is wrong
                            if ((order.vOrder[v] < order.vOrder[otherV[i]]) != shouldBeLeft) {
                                return true;
                            }
                    }
                    return false;
                }.bind(this);

                var fixOrder = function(order) {
                    // we know we can't easily change the order - it would be done in all algorithms above, if possible
                    // ..so do a simple mirror flip of the pedigree - father will be moved ot the left.mother to the right
                    order.flipOrders();
                    // ... but all other partners get fliped as well, and may become badly-ordered (which is a problem
                    // since most partners are probably ordered correctly). So fix whatever can be fixed easily
                    this.transpose(order, true);
                }.bind(this);

                // check that father (if there is only one) is on the left, OR
                // that mother (if there is only one) is on the right;
                // if not, flip all orders left-to-right
                if (motherFather.father !== undefined) {
                    // check that order of father is to the left of mother and all other parents
                    var allOther = motherFather.other.slice();
                    (motherFather.mother !== undefined) && allOther.push(motherFather.mother);
                    if (isOrderWrong(motherFather.father, allOther, true)) {
                        fixOrder(order);
                        return;
                    }
                }
                if (motherFather.mother !== undefined) {
                    // check that order of mother is to the right of father and all other parents
                    var allOther = motherFather.other.slice();
                    (motherFather.father !== undefined) && allOther.push(motherFather.father);
                    if (isOrderWrong(motherFather.mother, allOther, false)) {
                        fixOrder(order);
                        return;
                    }
                }
            }
        },

        transpose: function(order, doMinorImprovements, stopIfMoreThanCrossings)
        {
            // for each rank: goes over all vertices in the rank and tries to switch orders of two
            //                adjacent vertices. If numCrossings is improved keeps the new order.
            //                repeats for each rank, and if there was an improvementg tries again.
            var improved = true;

            //if (doMinorImprovements) Helpers.printObject(order);

            var totalEdgeCrossings = this.edge_crossing(order);
            if (stopIfMoreThanCrossings && totalEdgeCrossings > stopIfMoreThanCrossings) return;

            var iter = 0;
            while( improved )
            {
                iter++;
                if (!doMinorImprovements && iter > 4) break;
                if (iter > 100) { console.log("Assertion failed: too many iterations in transpose(), NumIter == " + iter); break; }

                improved = false;

                for (var r = 1; r <= this.maxRank; r++)
                {
                    if (r%3 == 0) {
                        // just place all childhubs in the same order as their relationships
                        // (..and keep existing order of pieces of virtual edges at this rank)
                        this.placeChhubsInCorrectOrder(r, order);
                        continue;
                    }

                    var numEdgeCrossings = this.edge_crossing(order, r);

                    if (!doMinorImprovements && numEdgeCrossings == 0) continue;

                    var edgeLengthScore = doMinorImprovements ? this.edge_length_score(order,r) : 0;

                    var rankImproved = false;

                    var maxIndex = order.order[r].length - 1;
                    for (var i = 0; i < maxIndex; ++i) {

                        order.exchange(r, i, i+1);

                        var newEdgeCrossings = this.edge_crossing(order, r);
                        var newLengthScore   = doMinorImprovements ? this.edge_length_score(order,r) : 0;

                        //if (doMinorImprovements)
                        //    console.log("trying: " + v1 + "  <-> " + v2);
                        //if ( v1 == 8 || v1 == 9 )  {
                        //    console.log("v = " + v1 + ", u = " + v2 + ", newScore= " + newEdgeCrossings +
                        //                ", lenScore= " + newLengthScore + ", oldScore= " + numEdgeCrossings +
                        //                ", oldLenScore= " + edgeLengthScore);
                        //}

                        if (newEdgeCrossings < numEdgeCrossings ||
                            (newEdgeCrossings == numEdgeCrossings && newLengthScore < edgeLengthScore) ) {
                            // this was a good exchange, apply it to the current real ordering
                            improved = true;
                            rankImproved = true;
                            var improvement = (numEdgeCrossings - newEdgeCrossings);
                            totalEdgeCrossings -= improvement;
                            numEdgeCrossings = newEdgeCrossings;
                            edgeLengthScore  = newLengthScore;
                            //console.log("exchanged "+ v1 + " <-> " + v2);
                            if (!doMinorImprovements) {
                                if (totalEdgeCrossings == 0) return; // still want to optimize for edge lengths
                                if (numEdgeCrossings == 0) break; // for this rank
                            }

                        }
                        else {
                            // exchange back
                            order.exchange(r, i, i+1);
                        }
                    }

                    if (rankImproved) r--; // repeat again for the same rank
                }
            }
            //if (doMinorImprovements) Helpers.printObject(order);
        },

        transposeLongEdges: function(order, numCrossings, postReRanking)
        {
            if (numCrossings == 0)
                return numCrossings;

            var maxRealId = this.GG.getMaxRealVertexId();
            var numVert   = this.GG.getNumVertices();

            var checked = [];
            for (var v = maxRealId+1; v < numVert; v++)
                checked[v] = false;

            for (var v = maxRealId+1; v < numVert; v++) {

                if (checked[v] || this.ranks[v] == 0) continue;

                // find a long edge - an edge connecting real nodes of non-neighbouring ranks,
                // consisting of virtual vertices on intermediate ranks (plus source/target)

                // start from head node - the first virtual node
                var head = v;
                while (this.GG.isVirtual(this.GG.getInEdges(head)[0]))
                    head = this.GG.getInEdges(head)[0];

                var chain = [];
                var next  = head;

                // go towards the tail through out-edges
                do {
                    checked[next] = true;
                    chain.push(next);
                    next = this.GG.getOutEdges(next)[0];
                }
                while (this.GG.isVirtual(next));
                chain.push(next);

                var bestScore = numCrossings;
                var bestOrder = undefined;

                console.log("Optimizing long edge placement: chain " + Helpers.stringifyObject(chain));

                // try to find best placement: brute force, try to reposition up to 3 pieces at a time simultaneously
                // checking all possible positions for the pieces in question up to 4 positions to the left or right
                if (chain.length <= 10) {
                    var beforeChainEnd = 2;
                    if (postReRanking)
                        beforeChainEnd = 4;
                    for (var i = 0; i < chain.length-beforeChainEnd; i++) {

                        var piece1 = chain[i];
                        var piece2 = chain[i+1];
                        var piece3 = chain[i+2];

                        var rank1 = this.ranks[piece1];
                        var rank2 = this.ranks[piece2];
                        var rank3 = this.ranks[piece3];

                        var ord1  = order.vOrder[piece1];
                        var ord2  = order.vOrder[piece2];
                        var ord3  = order.vOrder[piece3];

                        //console.log("chain piece " + piece1 + ", " + piece2 + ", " + piece3);

                        for (var move1 = -4; move1 <= 4; move1++ ) {
                            if (!order.canMove(rank1, ord1, move1)) continue;
                            for (var move2 = -4; move2 <= 4; move2++ ) {
                                if (!order.canMove(rank2, ord2, move2)) continue;
                                for (var move3 = -4; move3 <= 4; move3++ ) {
                                    if (move1 == 0 && move2 == 0 && move3 == 0) continue;
                                    if (!order.canMove(rank3, ord3, move3)) continue;

                                    var newOrder = order.copy();
                                    newOrder.move(rank1, ord1, move1);
                                    newOrder.move(rank2, ord2, move2);
                                    newOrder.move(rank3, ord3, move3);

                                    // TODO: performance: only count crossings caused by the long edge itself?
                                    var newCross = this.edge_crossing(newOrder, false, postReRanking);
                                    if (newCross < bestScore) {
                                        //console.log("+");
                                        bestScore = newCross;
                                        bestOrder = [rank1, ord1, move1, rank2, ord2, move2, rank3, ord3, move3];
                                    }

                                    //if (move1 == 1 && move2 == 0 && move3 == 0)
                                    //    console.log("New Score: " + newCross + ", best: " + bestScore );

                                }
                            }
                        }
                    }
                }

                if (bestScore < numCrossings) {
                    order.move(bestOrder[0], bestOrder[1], bestOrder[2]);
                    order.move(bestOrder[3], bestOrder[4], bestOrder[5]);
                    order.move(bestOrder[6], bestOrder[7], bestOrder[8]);
                    numCrossings = bestScore;
                }

                if (numCrossings == 0) break;
            }

            return numCrossings;
        },
        //========================================================================[ordering]=

        //=====================================================================[re-ordering]=
        reRankRelationships: function() {
            // re-rank all relationship nodes to be on the same level as the lower ranked
            // parent. Attempt to place the re-ranked node next to one of the parents;
            // having ordering info helps to pick the parent & the location.
            // Note1: we may not be able to place a relationship node right next to a
            //        parent (because both parents already have a relationship node on the
            //        requested side), but we can always place it next to another
            //        relationship by the same parent.
            // Note2: also need to shorten incoming multi-rank edges by one node
            //        (see removeRelationshipRanks())

            //console.log("GG: "  + Helpers.stringifyObject(this.GG));
            //console.log("Orders: " + Helpers.stringifyObject(this.order));

            if (this.maxRank === undefined || this.GG.v.length == 0) return;

            var handled = {};

            var initialOrdering = this.order.copy();

            for (var r = 2; r <= this.maxRank; r+=3) {

                // pass1: simple cases: parents are next to each other.
                //        looks better when all such cases are processed before more complicated cases
                //        (otherwise in case of say 3 relationship nodes may end up with two
                //         ugly placements (#2 and #3) instead of only one (#2) when #2 becomes ugly)
                for (var oo = 0; oo < initialOrdering.order[r].length; oo++) {
                    var i = initialOrdering.order[r][oo];   // i is the relationship ID
                    if (this.GG.isVirtual(i)) continue;
                    if (!this.GG.isRelationship(i)) throw "[1] Unexpected node " +i + " at rank " + r;

                    //console.log("==> [1] Handling: " + i);

                    var parents = this.GG.getInEdges(i);

                    // note: each "relationship" node is guaranteed to have exactly two "parent" nodes (validate() checks that)

                    if (this.ranks[parents[0]] != this.ranks[parents[1]])
                        throw "Assertion failed: edges betwen neighbouring ranks only";

                    var order1 = this.order.vOrder[parents[0]];
                    var order2 = this.order.vOrder[parents[1]];

                    var minOrder = Math.min(order1, order2);
                    var maxOrder = Math.max(order1, order2);

                    // if parents are next to each other in the ordering
                    if ( maxOrder == minOrder + 1 ) {
                        //console.log("=== is relationship: " + i + ", minOrder: " + minOrder + ", maxOrder: " + maxOrder );
                        this.moveVertexToRankAndOrder( i, this.ranks[parents[0]], maxOrder );
                        handled[i] = true;
                    }
                }

                // pass2: parents are not next to each other on the same rank
                for (var oo = 0; oo < initialOrdering.order[r].length; oo++) {
                    var i = initialOrdering.order[r][oo];   // i is the relationship ID
                    if (this.GG.isVirtual(i)) continue;
                    if (!this.GG.isRelationship(i)) throw "[2] Unexpected node " +i + " at rank " + r;

                    if ( handled.hasOwnProperty(i) )
                        continue; // this node has already been handled

                    //console.log("==> [2] Handling: " + i);

                    var parents = this.GG.getInEdges(i);

                    // rearrange so that parent0 is on the left - for simplicity in further logic
                    if (this.order.vOrder[parents[0]] > this.order.vOrder[parents[1]])
                        parents.reverse();

                    //console.log("NEED TO re-rank relatioship " + i + ", parents=" + Helpers.stringifyObject(parents));

                    var rank = this.ranks[parents[0]];

                    // 1. for each parent pick which side of the parent to use
                    // 2. pick which parent is a better target:
                    //    - prefer parent with no relationship node on the corect side
                    //    - somewhere in the middle if both parents have other nodes on the preferred side:
                    //      - try not to get inbetween well-placed relationships
                    //      - count edge crossings (TODO)

                    var order1 = this.order.vOrder[parents[0]];
                    var order2 = this.order.vOrder[parents[1]];

                    if (order2 == order1 + 1)
                        throw "Assertion failed: all relationship with parents next to each other are already handled (for parents: " + Helpers.stringifyObject(parents) + ")";

                    var insertOrder = order1 + 1;   // set some default in case all other heuroistics fail

                    var rightOfParent0 = this.order.order[rank][order1+1];
                    var leftOfParent1  = this.order.order[rank][order2-1];
                    //console.log("o1: " + order1 + ", o2: " + order2 + ", rp0: " + rightOfParent0 + ", lp1: " + leftOfParent1 );
                    var p0busy = false;
                    var p1busy = false;
                    if (this.GG.hasEdge(parents[0],rightOfParent0))
                        p0busy = true;
                    if (this.GG.hasEdge(parents[1],leftOfParent1))
                        p1busy = true;
                    //console.log("p0busy: " + p0busy + ", p1busy: " + p1busy);

                    if (p1busy && p0busy) {
                        // TODO: testcase 5K, relationship with ID=15 is inserted not optimaly
                        // both busy: find position which does not disturb "nice" relationship nodes
                        for (var o = order1+2; o <= order2-1; o++ ) {
                            var next = this.order.order[rank][o];
                            if (!this.GG.hasEdge(parents[0],next)) {
                                insertOrder = o;
                                break;
                            }
                        }
                        if (insertOrder == null) {
                            var parentsOfLeft = this.GG.getInEdges(leftOfParent1);
                            var otherP1 = (parentsOfLeft[0] != parents[1]) ? parentsOfLeft[0] : parentsOfLeft[1];
                            var orderP1 = this.order.vOrder[otherP1];
                            if (orderP1 < order1)
                                insertOrder = order2;
                            else
                                insertOrder = order1 + 1;
                        }
                    }
                    else if (p1busy) {
                        // p0 is free, p1 already has a relationship node next to it
                        insertOrder = order1+1;
                    }
                    else if (p0busy) {
                        // p1 is free, p0 already has a relationship node next to it
                        insertOrder = order2;
                    }
                    else {
                        // both p0 and p1 can have the relationship node right next to them
                        // for now arbitrarily pick p1
                        // TODO: try both pick the one with less crossed edges. Need a testcase
                        insertOrder = order2;
                    }

                    //console.log("==> inserting: " + i + " on order " + insertOrder + " (after " + this.order.order[rank][insertOrder-1] + " and before " + this.order.order[rank][insertOrder] + ")");
                    this.moveVertexToRankAndOrder( i, rank, insertOrder );

                    //-----
                    // fix the problem described in issue #664
                    var oldOrder = initialOrdering.vOrder[i];
                    if (oldOrder > 0) {
                        var oldNeighbourLeft = initialOrdering.order[r][oldOrder-1];
                        if (this.GG.isRelationship(oldNeighbourLeft) && this.order.vOrder[oldNeighbourLeft] > this.order.vOrder[i] && this.ranks[oldNeighbourLeft] == this.ranks[i]) {
                            //console.log("L: " + oldNeighbourLeft);
                            // fix the case when two relationships switched order during re-ranking - we may want to change the order of children as well
                            this.swapChildrenIfAllAToTheLeftOfB( oldNeighbourLeft, i );

                            // TODO: remove
                            // Also make sure childhubs are in the correct order
                            var chHubL = this.GG.getOutEdges(oldNeighbourLeft)[0];
                            var chHubR = this.GG.getOutEdges(i)[0];
                            if (this.order.vOrder[chHubL] < this.order.vOrder[chHubR])
                                this.order.exchange(this.ranks[chHubL], this.order.vOrder[chHubL], this.order.vOrder[chHubR]);
                        }
                    }
                    if (oldOrder < initialOrdering.order[rank+1].length - 1) {
                        var oldNeighbourRight = initialOrdering.order[r][oldOrder+1];
                        if (this.GG.isRelationship(oldNeighbourRight) && this.order.vOrder[oldNeighbourRight] < this.order.vOrder[i] && this.ranks[oldNeighbourRight] == this.ranks[i]) {
                            //console.log("R: " + oldNeighbourRight);
                            // same as above, but switch right-to-left instead of left-to-right
                            this.swapChildrenIfAllAToTheLeftOfB( i, oldNeighbourRight );

                            // TODO: remove
                            // Also make sure childhubs are in the correct order
                            var chHubL = this.GG.getOutEdges(i)[0];
                            var chHubR = this.GG.getOutEdges(oldNeighbourRight)[0];
                            if (this.order.vOrder[chHubL] < this.order.vOrder[chHubR])
                                this.order.exchange(this.ranks[chHubL], this.order.vOrder[chHubL], this.order.vOrder[chHubR]);
                        }
                    }
                    //-----
                }
            }

            this.removeRelationshipRanks();

            // Fix ordering mistakes introduced by the re-ranking algorithm. It is easier to fix post-factum
            // than to order correctly rigth away (e.g. generally the above algo is doing the same thing, but
            // in some special cases can do better once everything is complete)
            this.improveOrdering();

            // after re-ordering long edges are shorter, try to improve long edge placement again
            var edgeCrossings = this.edge_crossing(this.order, false, true);
            this.transposeLongEdges( this.order, edgeCrossings, true );

            console.log("Final re-ordering: " + Helpers.stringifyObject(this.order.order));
            console.log("Final ranks: "  + Helpers.stringifyObject(this.ranks));
        },

        moveVertexToRankAndOrder: function( v, newRank, newOrder ) {
            var oldRank  = this.ranks[v];
            var oldOrder = this.order.vOrder[v];

            this.order.moveVertexToRankAndOrder( oldRank, oldOrder, newRank, newOrder );
            this.ranks[v] = newRank;
        },

        swapChildrenIfAllAToTheLeftOfB: function ( leftRel, rightRel ) {
            // we assume that during re-ordering relationship `leftRel` which used ot be to the left or relationship `rightRel`
            // is now to the right of `rightRel`. This may have introduced some unnecessary crossed edges. Fix those by swapping
            // the order of relationship children as well, if it clearly wont break other things, e.g.
            // - if there are no nodes which are not children of `leftRel` of `rightRel` between the leftmost and rightmost child of either leftR or rightR
            // - all children of `leftRel` are to the left of all children of `rightRel`
            // - there are no relationships between `leftRel` children and any nodes to the left of `leftRel` OR on any other rank
            // - there are no relationships between `rightRel` children and any nodes to the right of `rightRel` OR on any other rank
            console.log("Attempting to swap children of " + leftRel + " and " + rightRel + " (due to change of order during re-ranking)");

            var chHubL = this.GG.getOutEdges(leftRel)[0];
            var chHubR = this.GG.getOutEdges(rightRel)[0];

            var childrenL   = this.GG.getOutEdges(chHubL);
            var childrenR   = this.GG.getOutEdges(chHubR);
            var allChildren = childrenL.concat(childrenR);

            var order   = this.order;
            var byOrder = function(a,b) { return order.vOrder[a] - order.vOrder[b]; }
            allChildren.sort(byOrder);
            //console.log("all children sorted by order: " + Helpers.stringifyObject(allChildren));

            var childRank = this.ranks[allChildren[0]];

            var leftMostOrder  = order.vOrder[allChildren[0]];
            var rightMostOrder = order.vOrder[allChildren[allChildren.length-1]];
            // we only swap orders if there are no other nodes inbetween the children of leftRel and rightRel
            if (rightMostOrder - leftMostOrder + 1 != childrenL.length + childrenR.length) return;

            for (var i = 0; i < allChildren.length; i++) {
                var nextInOrder = allChildren[i];

                var shouldBeLeftRelChild = (i < childrenL.length);

                if (shouldBeLeftRelChild && this.GG.getInEdges(nextInOrder)[0] != chHubL)    // a child of `rightRel` must be to the left of one of the children of `leftRel` => quit
                    return;

                // make sure there are no relationships which will result in extra edge crossings if we swap order of children
                var outEdges = this.GG.getOutEdges(nextInOrder);
                if (outEdges.length > 0) {
                    for (var j = 0; j < outEdges.length; j++) {
                        var rel     = outEdges[j];
                        if (this.GG.isVirtual(rel)) return; // do not touch long edges (TODO: can improve, see tescase HUGE1 and nodes with IDs 23 & 4)

                        var parents = this.GG.getParents(rel);

                        for (var k = 0; k < parents.length; k++) {
                            if (this.ranks[parents[k]] != childRank) return;   // relationship with a node on another rank - no swaps

                            if (shouldBeLeftRelChild && order.vOrder[parents[k]] < leftMostOrder) return;    // leftRel children in relationship with nodes to the left
                                                                                                             // ->after swap there will be many crossed edges
                            if (!shouldBeLeftRelChild && order.vOrder[parents[k]] > rightMostOrder) return;  // rightRel children in rel. with nodes to the right
                        }
                    }
                }
            }

            // swap by inverting the order
            //console.log("Performing swap");
            var middle = Math.floor((leftMostOrder + rightMostOrder)/2);

            for (var i = leftMostOrder; i <= middle; i++) {
                var tmp = this.order.order[childRank][i];
                this.order.order[childRank][i] = this.order.order[childRank][leftMostOrder + rightMostOrder - i];
                this.order.order[childRank][leftMostOrder + rightMostOrder - i] = tmp;

                this.order.vOrder[this.order.order[childRank][i]]                = i;
                this.order.vOrder[this.order.order[childRank][leftMostOrder + rightMostOrder-i]] = leftMostOrder + rightMostOrder - i;
            }
        },

        removeRelationshipRanks: function () {
            // removes ranks previously occupied by relationship nodes (which is every 3rd rank)
            // (these ranks have either no nodes at all or only virtual nodes
            // from multi-rank edges passing through)
            for (var r = 2; r <= this.maxRank; r+=2) {
                // r+=2 because each 3rd rank is a relationship rank, but once this rank is removed it becomes r+2 not r+3

                if ( this.order.order[r].length > 0 ) {
                    // there are some virtual nodes left
                    for (var i = 0; i < this.order.order[r].length; i++) {
                        var v = this.order.order[r][i];
                        // it takes a lot of work to completely remove a vertex from a graph.
                        // however it is easy to disconnect and place it into rank 0 which is ignored when drawing/assigning coordinates
                        this.GG.unplugVirtualVertex(v);
                        this.ranks[v] = 0;
                        this.order.vOrder[v] = this.order.order[0].length;
                        this.order.order[0].push(v);
                    }
                }

                this.order.order.splice(r,1);

                for ( var v = 0; v < this.ranks.length; v++ ) {
                    if ( this.ranks[v] > r )
                        this.ranks[v]--;
                }

                this.maxRank--;
            }

            // remove all unplugged vertices
            var unplugged = this.order.removeUnplugged().sort(function(a,b){return a-b});
            console.log("Removing unnecessary long edge pieces: " + Helpers.stringifyObject(unplugged));
            for (var i = 0; i < unplugged.length; i++) {
                var removedID = unplugged[i] - i;   // "-i" because after each splice array size decreases by one and item N is going to be at location N-1, then N-2, etc
                this.ranks.splice(removedID, 1);
                this.GG.remove(removedID);
            }
        },

        improveOrdering: function()
        {
            // 1) fix re-ranking mistakes:
            //
            //    A) in a case like the one below "partner1" can be placed right next to "relationship1"
            //                           /--------------\ /--------------- ...  ---[partner1]
            //               -----------/---\            X
            //      [person]/----[rel1]/     \--[rel2]--/ \____[partner1]
            //
            for (var r = 1; r <= this.maxRank; r+=1) {
                for (var oo = 0; oo < this.order.order[r].length; oo++) {
                    var v = this.order.order[r][oo];
                    if (!this.GG.isPerson(v)) continue;

                    if (this.GG.getInEdges(v).length != 0) continue;  // skip nodes with parents

                    var relationships = this.GG.getOutEdges(v);
                    if (relationships.length != 1) continue;          // only when one and only one relationship

                    var rel = relationships[0];
                    if (this.ranks[rel] != r) continue;               // only when relationship is on the same rank

                    var orderV   = this.order.vOrder[v];
                    var orderRel = this.order.vOrder[rel];

                    if (Math.abs(orderRel - orderV) != 1) {
                        // not next to each other
                        if (orderRel > orderV) {
                            this.order.move(r, orderV, (orderRel - orderV - 1));
                        } else {
                            this.order.move(r, orderV, (orderRel - orderV + 1));
                        }
                    }
                }
            }

            // 2) TODO: come up with heuristics which can be applied at this point
            //          (on a need-to-improve basis once an imperfection is discovered):
            // after re-ranking there may be some orderings which are equivalent in terms
            // of the number of edge crossings, but more or less visually pleasing
            // depending on what kinds of edges are crossing.
            // Until re-ordering is done it is computationally harder to make these tests,
            // but once reordering is complete it is easier in some cases
            // (e.g: testcase 5A, relationship with both a parent and parent's child)

            // 3) make sure chhubs are ordered in the same way their relationships are
            for (var r = 2; r <= this.maxRank; r+=2) {
                // place all childhubs in the same order as their relationships
                // (..and keep existing order of pieces of virtual edges at this rank)

                // check if we need to do that: if they are already OK it is usually better not to mess with orders
                // as chhubs will be OK anyway, but virtual edge pieces may get screwed
                if (!this.isChhubsOrderOK(r, this.order)) {
                    //console.log("Fixing chhub order @ rank " + r);
                    this.placeChhubsInCorrectOrder(r, this.order);
                }
            }
        },

        //=====================================================================[re-ordering]=

        findConnectedComponent: function( v, edgeIncludedFunc, stopSet, maxSize )
        {
            // computes connected component which includes vertex v (or all vertice sin array v)
            // and all vertices reachable from v not using edges which do not pass the `edgeIncludedFunc()` test
            //
            // stops when a vertex from the `stopSet` set is found and includes `reachedStopSet` key in the response
            // stops when component size exceeds `maxSize`

            var component = {};
            var size      = 0;
            var stopFound = false;

            // if v is not an arrya make it an array with one element
            if (Object.prototype.toString.call(v) !== '[object Array]') {
                v = [v];
            }

            var q = new Queue();
            for (var i = 0; i < v.length; i++) {
                q.push(v[i]);
                component[v[i]] = true;
            }

            while( q.size() > 0 ) {
                var nextV = q.pop();

                var allConnected = this.GG.getAllEdgesWithWeights(nextV);
                for (var u in allConnected) {
                    if (!allConnected.hasOwnProperty(u)) continue;

                    var from = allConnected[u].out ? nextV : u;
                    var to   = allConnected[u].out ? u     : nextV;

                    if (edgeIncludedFunc(from, to) && !component.hasOwnProperty(u)) {
                        component[u] = true;
                        q.push(u);
                        size++;

                        if (stopSet.hasOwnProperty(u)) {
                            stopFound = true;
                            break;
                        }
                    }

                    if (size > maxSize) {
                        q.clear();
                        break;
                    }
                }
            }
            return { "component": component, "size": size, "stopSetReached": stopFound };
        },

        //=[ancestors]=======================================================================
        findAllAncestors: function()
        {
            var ancestors = {};
            var consangr  = {};

            // got from low ranks to high ranks. For each node merge ancestors of its parents
            for (var r = 1; r <= this.maxRank; r++) {
                var nextRank = this.order.order[r];

                for (var i = 0; i < nextRank.length; i++) {
                    var v = this.order.order[r][i];
                    if (!this.GG.isPerson(v)) continue;
                    ancestors[v] = {};
                    ancestors[v][v] = 0;
                    if (this.GG.isAdoptedIn(v)) continue; // TODO: assume adopted in have no known parents
                    var parents = this.GG.getParents(v);
                    //console.log("v: " + v + ", parents: " + Helpers.stringifyObject(parents));
                    for (var j = 0; j < parents.length; j++) {
                        var familyBranch = ancestors[parents[j]];

                        for (var u in familyBranch) {
                            if (familyBranch.hasOwnProperty(u)) {
                                if (ancestors[v].hasOwnProperty(u)) {   // parents are relatives: pick the shortest path to that relative
                                    ancestors[v][u] = Math.min( familyBranch[u] + 1, ancestors[v][u] );
                                }
                                else
                                    ancestors[v][u] = familyBranch[u] + 1;
                            }
                        }
                    }
                }
            }

            // repeat the same for relationship nodes. Note that this second pas sis required because we need to make sure both
            // parents got their relatives computed before the relationship ancestors canbe updated.
            // Could do all in one pass iff relationship nodes were ranked below person nodes, but while they are
            // twins could nto be inserted correctly. So need to re-rank first, re-insert twins, compute ancestors for all including twins
            for (var r = 1; r <= this.maxRank; r++) {
                var nextRank = this.order.order[r];

                for (var i = 0; i < nextRank.length; i++) {
                    var v = this.order.order[r][i];
                    if (!this.GG.isRelationship(v)) continue;
                    ancestors[v] = {};
                    ancestors[v][v] = 0;
                    var parents = this.GG.getParents(v);
                    for (var j = 0; j < parents.length; j++) {
                        var familyBranch = ancestors[parents[j]];
                        for (var u in familyBranch) {
                            if (familyBranch.hasOwnProperty(u)) {
                                if (ancestors[v].hasOwnProperty(u)) {   // parents are relatives
                                    consangr[v] = true;
                                    ancestors[v][u] = Math.min( familyBranch[u] + 1, ancestors[v][u] );
                                }
                                else
                                    ancestors[v][u] = familyBranch[u] + 1;
                            }
                        }
                    }
                }
            }

            //console.log("Ancestors: " + Helpers.stringifyObject(ancestors));
            //console.log("Consangr:  " + Helpers.stringifyObject(consangr));

            return {ancestors: ancestors, consangr: consangr};
        },
        //=======================================================================[ancestors]=

        //=[vertical separation for horizontal edges]========================================
        positionVertically: function()
        {
            /*// debug: very useful debug case, which is very hard to reproduce without fiddling with
              //        a normally-processed MS_004 graph. TODO: remove
              if (this.positions.length > 60) {
              this.positions[50] = 80;
              this.positions[51] = 193;
              this.positions[53] = 217;
              this.positions[60] = 440;
              this.order.vOrder[45] = this.order.vOrder[50];
              this.order.vOrder[50] = 0;
            }*/

            var verticalLevels = new VerticalLevels();

            // for all ranks:
            //
            // 1) if rank has childhub nodes:
            //    pick vertical position (within the rank, as a discrete integer "level") for the horizontal part of the child edge
            //    (in a way which minimizes crossings between vertical and horizontal parts of different relationship-to-child lines)
            //
            // 2) if rank has person nodes:
            //    for all person which has a relationship vertically position all outgoing relationship edges
            //    (within the rank, as a discrete integer "level") in a way to avoid overlaps between different relationship edges

            var _startRank = 2;
            var _rankStep  = 2;
            /*
            // TODO: DEBUG: computations below are to accomodate debugging when reRankRelationships() may be omitted
            for (var i = 0; i<this.order.order[_startRank].length; i++)
                if ( !this.GG.isVirtual(this.order.order[_startRank][i]) && !this.GG.isChildhub( this.order.order[_startRank][i] ) )
                {
                    _startRank = 3;
                    _rankStep  = 3;
                    break;
                }*/

            for (var r = 1; r <= this.maxRank; r+=1)
                verticalLevels.rankVerticalLevels[r] = 1;    // number of "vertical levels" (i.e. parallel horizontal edges) between rank r and r+1. Start with 1 for all ranks

            if (this.GG.v.length <= 1) return verticalLevels;

            //console.log("GG: " + Helpers.stringifyObject(this.GG));
            //console.log("Order: " + Helpers.stringifyObject(this.order));

            // 1) go over childnode ranks.
            //    a) for each vertex on the rank, in the ordering order
            //    b) check if any edges cross any edges of the vertices earlier in the order
            //    c) if yes, add that level to the set of forbidden levels
            //    d) pick minimum level which is not forbidden
            for (var r = _startRank; r <= this.maxRank; r+=_rankStep) {

                var initLevels = [];
                var edgeInfo   = [];

                var len = this.order.order[r].length;
                for (var i = 0; i < len; i++) {
                    var v = this.order.order[r][i];

                    if (this.GG.isVirtual(v)) continue;
                    if (!this.GG.isChildhub(v)) throw "Assertion failed: childhub nodes at every other rank ("+v+")";

                    var realationship = this.GG.getInEdges(v)[0];
                    var top_x         = this.positions[realationship];  // x cooordinate of the line going up
                    var left_x        = top_x;                          // the leftmost edge of the horizontal part
                    var right_x       = top_x;                          // the rightmost edge of the horizontal part
                    var childCoords   = [];

                    var outEdges = this.GG.getOutEdges(v);

                    for (var j = 0; j < outEdges.length; j++) {
                        var child_x = this.positions[outEdges[j]];
                        childCoords.push(child_x);

                        left_x  = Math.min( left_x,  child_x );
                        right_x = Math.max( right_x, child_x );
                    }

                    if (left_x == right_x) {
                        // no horizontal part, just  astraight line - no need to optimize anything
                        verticalLevels.childEdgeLevel[v] = 0;
                    } else {
                        // special case improvement: single child which is leftmost on the rank and its parent is to the right of it
                        // (this may not produce less edge crossings, but is more visually pleasing)
                        var needTopmost = (outEdges.length == 1) &&
                                          ( ( (this.order.vOrder[outEdges[0]] == 0) && (top_x > this.positions[outEdges[0]]) ) ||
                                            ( (this.order.vOrder[outEdges[0]] == this.order.vOrder.length -1) && (top_x < this.positions[outEdges[0]]) ) );
                        edgeInfo.push( { "childhub": v, "top_x": top_x, "left_x": left_x, "right_x": right_x, "childCoords": childCoords, "needTopmost": needTopmost} );
                        initLevels.push(1);
                    }
                }
                //console.log("EdgeInfo: " + Helpers.stringifyObject(edgeInfo));

                // compose the "crossing score" function which, given edgeInfo + which horizontal line is higher,
                // can tell the number of crossings between two childhub-to-children sets of edges
                var pairScoreFunc = function( edge1, edge2, edge1level, edge2level ) {
                    //
                    // general form of a displayed edges is like:
                    //                                                             relationship_a    relationship_b
                    //              top_x                                                       |    |
                    //                |                                                         |    +-----+-------+---+   <--- level1
                    //  left_x   +----+-----+   right_x , multiple edges may be arranged like:  +------+---|---+   |   |   <--- level2
                    //           |  |    |  |                                                          |   |   |   |   |
                    //        (one or more childX)                                                    a1  b1  a2  b2  b3
                    //
                    // This function computes the number of intersections of two shapes like the ones
                    // draw above given their vertical "levels", i.e. which one is above the other.

                    if ( edgeInfo[edge1].right_x < edgeInfo[edge2].left_x ||
                         edgeInfo[edge1].left_x  > edgeInfo[edge2].right_x )
                         return 0;                                              // edges do not intersect/overlap => no penalty for any level assignment

                    if (edge1level == edge2level) return Infinity;              // intersect and at the same level => forbidden => (penalty == Infinity)

                    if (edge1level > edge2level) {
                        var tmpEdge  = edge1;
                        var tmpLevel = edge1level;
                        edge1        = edge2;
                        edge1level   = edge2level;
                        edge2        = tmpEdge;
                        edge2level   = tmpLevel;
                    }

                    // compute number of intersections
                    var intersections = 0;
                    // potentially childhub-to-horizontal segment of edge2 may cross horizontal segment of edge1
                    if (edgeInfo[edge2].top_x >= edgeInfo[edge1].left_x && edgeInfo[edge2].top_x <= edgeInfo[edge1].right_x)
                        intersections++;
                    // potentially horizontal-to-child vertical lines of edge1 may cross horizontal segment of edge2
                    for (var j = 0; j < edgeInfo[edge1].childCoords.length; j++) {
                        var childX = edgeInfo[edge1].childCoords[j];
                        if (childX >= edgeInfo[edge2].left_x && childX <= edgeInfo[edge2].right_x)
                            intersections++;
                    }
                    if (edgeInfo[edge2].needTopmost)    // single leftmost child should be topmost
                        return intersections += 0.1;
                    return intersections;
                };

                var optimizer = new VerticalPosIntOptimizer( pairScoreFunc, initLevels );

                // - full exhaustive search when up to 5 edges cross other edges on this rank
                // - heuristic with up to 600 steps is used otherwise
                //
                //   max full search running time: ~                 f(numEdgesThatCross) * scoreFuncTime  (*)
                //   max heuristic running time:   ~  bigC * numEdgesThatCross * numSteps * scoreFuncTime
                //
                //   (*) where f(numEdgesThatCross) is (currently) numEdgesThatCross^numEdgesThatCross, e.g. f(5) = 3125
                var edgeLevels = optimizer.computeVerticalPositions( 5, 600 );

                //console.log("[rank " + r + "] Final vertical childedge levels: " +  Helpers.stringifyObject(edgeLevels));

                for (var v = 0; v < edgeInfo.length; v++) {
                    verticalLevels.childEdgeLevel[edgeInfo[v]["childhub"]] = edgeLevels[v];
                    if (edgeLevels[v] > verticalLevels.rankVerticalLevels[r])
                        verticalLevels.rankVerticalLevels[r] = edgeLevels[v];
                }
            }

            // 2) vertical positioning of person-relationship edges:
            for (var r = 1; r <= this.maxRank; r++) {

                var numLevels = 1;

                var initLevels = [];
                var edgeInfo   = [];
                var relEdges   = {};

                var len = this.order.order[r].length;
                for (var i = 0; i < len; i++) {
                    var v = this.order.order[r][i];

                    if (this.GG.isPerson(v)) {
                        var outEdges = this.GG.getOutEdges(v);
                        if (outEdges.length <= 0) continue;

                        var v_x = this.positions[v];

                        verticalLevels.outEdgeVerticalLevel[v] = {};

                        //console.log("Vert Positioning rel edges from person " + v);
                        //console.log("leftEdges: " + Helpers.stringifyObject(leftEdges));
                        //console.log("rightEdges: " + Helpers.stringifyObject(rightEdges));

                        var vOrder = this.order.vOrder;

                        var positionEdgesOnOneSide = function( edges ) {
                            var nextAttachPort    = 0;      // attachment point of the line connecting the node and it's relationship
                            var nextVerticalLevel = 0;      // vertical level of the line
                            for (var k = 0; k < edges.length; k++) {
                                var u    = this.GG.downTheChainUntilNonVirtual( edges[k] );    // final destination for the edge - possibly at another rank
                                var dest = edges[k];                                           // immediate target - on the same rank

                                // check what lies between V and DEST. Need to raise the edge by different amounts depending on what lies there
                                // (nothing, relationship nodes, real nodes)
                                var minOrder = Math.min(vOrder[dest], vOrder[v]) + 1;
                                var maxOrder = Math.max(vOrder[dest], vOrder[v]);
                                var minLevel = (minOrder == maxOrder) ? 0 : 1;  // 0 if next to each other, 1 if at least anything inbetween
                                //console.log("v: " + v + "(" + vOrder[v] + "),  dest: " + dest +  "(" + vOrder[dest] + "), minOrder: " + minOrder + ", maxOrder: " + maxOrder);
                                var otherVirtualEdge = false;
                                var goesOver         = [];
                                for (var o = minOrder; o < maxOrder; o++) {
                                    var w = this.order.order[r][o];
                                    if (this.GG.isPlaceholder(w))  { continue; }
                                    if (this.GG.isRelationship(w)) { minLevel = Math.max(minLevel, 2); }
                                    if (this.GG.isPerson(w))       { minLevel = Math.max(minLevel, 3); }
                                    if (this.GG.isVirtual(w) && this.GG.getInEdges(w)[0] != v) {
                                        otherVirtualEdge = true;
                                    }
                                    goesOver.push(w);
                                }
                                nextVerticalLevel = Math.max(nextVerticalLevel, minLevel);
                                //console.log("attaching ->" + dest + "(" + u + ") at attach port " + nextAttachPort + " and level " + nextVerticalLevel);
                                verticalLevels.outEdgeVerticalLevel[v][u] = { attachlevel: nextAttachPort, verticalLevel: nextVerticalLevel, numAttachLevels: edges.length };

                                //------------------------------
                                if (minLevel >= 2 || (minLevel == 1 && otherVirtualEdge) ) {
                                    // potentially crossing some other relatioship edges: add to the set of edges to be optimized

                                    var left_x    = Math.min( v_x, this.positions[dest] );
                                    var right_x   = Math.max( v_x, this.positions[dest] );
                                    var down_x    = this.positions[dest];

                                    // if the edge is going upwards need to take that into account when positioning edges
                                    // on this rank, not on the rank of the other parent
                                    var top_x     = Infinity;
                                    if (dest == u) {                      // same as !this.GG.isVirtual(u)
                                        var parents = this.GG.getInEdges(u);
                                        var otherParent = (parents[0] == v) ? parents[1] : parents[0];
                                        if (this.GG.isVirtual(otherParent)) {       // can only be if the edge goes upwards since relationship nodes
                                            top_x = this.positions[otherParent];    //  are always ranked at the rank of the lower-ranked partner
                                            if (top_x > right_x)
                                                right_x = top_x;
                                            if (top_x < left_x)
                                                left_x = top_x;
                                        }
                                    }

                                    edgeInfo.push( { "v": v, "u": u, "v_x": v_x, "left_x": left_x, "right_x": right_x, "down_x": down_x, "top_x": top_x, "over": goesOver } );
                                    initLevels.push(nextVerticalLevel);
                                    if (!relEdges.hasOwnProperty(u))
                                        relEdges[u] = [];
                                    relEdges[u].push(edgeInfo.length-1);
                                }
                                //------------------------------

                                nextAttachPort++;
                                nextVerticalLevel++;
                            }

                            return (nextVerticalLevel-1);
                        }.bind(this);

                        var partnerInfo = this._findLeftAndRightPartners(v);
                        //console.log("Asigning left edges");
                        var maxLeftLevel = positionEdgesOnOneSide(partnerInfo.leftPartners);
                        //console.log("Asigning right edges");
                        var maxRightLevel = positionEdgesOnOneSide(partnerInfo.rightPartners);

                        numLevels = Math.max(numLevels, Math.max(maxLeftLevel, maxRightLevel));
                    }
                }

                if (edgeInfo.length > 1) {    // if at most one edge crosses other vertices - we know everything is already laid out perfectly

                    for (var e = 0; e < edgeInfo.length; e++) {
                        if (!edgeInfo[e].hasOwnProperty("edgeComplement")) {
                            var nextRel   = edgeInfo[e].u;
                            var nextEdges = relEdges[nextRel];
                            if (nextEdges.length > 1) {
                                var otherEdge = (nextEdges[0] == e) ? nextEdges[1] : nextEdges[0];
                                edgeInfo[e]["edgeComplement"]         = otherEdge;
                                edgeInfo[otherEdge]["edgeComplement"] = e;
                            }
                        }
                    }

                    // compose the "crossing score" function which, given edgeInfo + which horizontal line is higher,
                    // can tell the number of crossings between two node-to-relationship edges
                    var pairScoreFunc = function( edge1, edge2, edge1level, edge2level, levels ) {
                        //
                        // general form of a displayed edges is one of:
                        // (where the solid line is part of the edge and the dotted part is treated as a separate edge related to the other partner or some other node)
                        //
                        // a)             ___________                           .....                   <-- level 2   \
                        //            ___/           \                         .     .                                 }  <--- between rank R and R-1
                        //     [node1]......[other]   \_____[relationship]..../       \....[node2]      <-- level 1   /
                        //                    .                   |
                        //                    .                   |
                        //       ^                                ^
                        //     left_x & v_x               right_x & down_x      (no top_x)
                        //
                        // b)                                                    ........[node2]                          <--- on some other rank
                        //                _________                              |
                        //               /         \                             |
                        //     [node1]__/   [...]   \_____[relationship]_____[virtual]        <--- this virtual node is the "otherParent" of relationship
                        //                                      |
                        //                                      |
                        //       ^                              ^                ^
                        //     left_x & v_x                  down_x       right_x & top_x
                        //
                        // c)            _________
                        //              /         \
                        //     [node]__/   [...]   \__[virtual]
                        //                               |
                        //                               |
                        //                               .......[relationship].....[node2]                                <--- on some other rank
                        //       ^                       ^
                        //     left_x & v_x      right_x & down_x     (no top_x)

                        if ( edgeInfo[edge1].right_x <= edgeInfo[edge2].left_x ||
                             edgeInfo[edge1].left_x  >= edgeInfo[edge2].right_x )
                             return 0;                                              // edges do not overlap => no penalty for any level assignment

                        if (edge1level == edge2level) return Infinity;              // intersect and at the same level => forbidden => (penalty == Infinity)

                        if (edge2level > edge1level) {
                            var tmpEdge  = edge1;
                            var tmpLevel = edge1level;
                            edge1        = edge2;
                            edge1level   = edge2level;
                            edge2        = tmpEdge;
                            edge2level   = tmpLevel;
                        }

                        // edge1 is the upper edge by now

                        if (edgeInfo[edge1].v == edgeInfo[edge2].v &&
                            edgeInfo[edge1].direction == edgeInfo[edge2].direction &&
                            edgeInfo[edge1].attachLevel < edgeInfo[edge2].attachLevel ) {
                            return Infinity;                 // edges originating from the same vertex in the same direction
                                                             // should keep their relative positioning, period
                        }

                        // edge1 completely overlaps edge2 and is above - this is optimal, penalty = 0
                        if (edgeInfo[edge1].left_x <= edgeInfo[edge2].left_x && edgeInfo[edge1].right_x >= edgeInfo[edge2].right_x)
                            return 0;
                        // should overlap but instead is below - 2 unnecessary intersections
                        if (edgeInfo[edge1].left_x >= edgeInfo[edge2].left_x && edgeInfo[edge1].right_x <= edgeInfo[edge2].right_x)
                            return 2;

                        var extraIntersections = 1.0;

                        // edges cross: if lower edge has top_x and it crosses the other edge -> report 1 unnecessary crossing
                        if (edgeInfo[edge2].top_x >= edgeInfo[edge1].left_x && edgeInfo[edge2].top_x <= edgeInfo[edge1].right_x)
                            extraIntersections++;

                        // [edge1] ------\           /- - - - - [edge1-complement]
                        //               |           |
                        // [edge2] ------1-----------2-------
                        //               |           |
                        //               \___[rel]_ _/
                        //                     |
                        //
                        // in a case like this, [rel] will be moved to a position above [edge2] and instead of
                        // two intersection {1,2} there will be only one intersection of downward egge with edge2.
                        // So if a case like this is detected we subtract 0.4 crossings from intersections 1 and 2
                        // (and from minimum score as well) - as long as there is [edge1-complement]
                        //
                        if (edgeInfo[edge1].hasOwnProperty("edgeComplement") &&
                            (!levels || levels[edgeInfo[edge1].edgeComplement] > edge2level)) {
                            if (edgeInfo[edge1].down_x >= edgeInfo[edge2].left_x && edgeInfo[edge1].down_x <= edgeInfo[edge2].right_x)
                                extraIntersections -= 0.4;
                        }

                        return extraIntersections;
                    }

                    //console.log("[rank " + r + "] Init vertical relatioship levels: " +  Helpers.stringifyObject(initLevels));

                    var optimizer = new VerticalPosIntOptimizer( pairScoreFunc, initLevels, initLevels );  // init level == min level

                    var relEdgeLevels = optimizer.computeVerticalPositions( 5, 500 );

                    //console.log("[rank " + r + "] Final vertical relatioship levels: " +  Helpers.stringifyObject(relEdgeLevels));

                    numLevels = 0;
                    // place computed levels where they ultimately belong
                    for (var i = 0; i < edgeInfo.length; i++) {
                        verticalLevels.outEdgeVerticalLevel[ edgeInfo[i].v ][ edgeInfo[i].u ].verticalLevel = relEdgeLevels[i];
                        if (relEdgeLevels[i] > numLevels)
                            numLevels = relEdgeLevels[i];
                    }

                    // optimize cases where an edge has rank > 1 because it supposedly goes over a relationship, but all
                    // relationships it goes over are raised because all their edges have higher levels.
                    // In such a case it looks beter when the edge in question is re-ranked back to rank 1
                    for (var i = 0; i < edgeInfo.length; i++) {
                        // optimize cases where an edge has rank 2 (because it goes over a relationship), but all
                        // relationships it goes over are raised => re-rank to rank 1
                        var level = relEdgeLevels[i];
                        if (level > 1) {
                            var lowerLevel = true;
                            for (var e = 0; e < edgeInfo[i].over.length; e++) {
                                var w = edgeInfo[i].over[e];
                                if (!this.GG.isRelationship(w)) {
                                    lowerLevel = false;
                                    break;
                                }
                                var parents = this.GG.getParents(w);
                                if ( verticalLevels.outEdgeVerticalLevel[ parents[0] ][ w ].verticalLevel <= level ||
                                     verticalLevels.outEdgeVerticalLevel[ parents[1] ][ w ].verticalLevel <= level ) {
                                    lowerLevel = false;
                                    break;
                                }
                            }
                            if (lowerLevel)
                                verticalLevels.outEdgeVerticalLevel[ edgeInfo[i].v ][ edgeInfo[i].u ].verticalLevel = 1;
                        }
                    }
                }

                verticalLevels.rankVerticalLevels[r-1] += Math.max(0, (numLevels - 2));
            }

            //console.log("Vert child:         " + Helpers.stringifyObject(verticalLevels.childEdgeLevel));
            //console.log("Vert relationships: " + Helpers.stringifyObject(verticalLevels.outEdgeVerticalLevel));
            //console.log("Vert levels:        " + Helpers.stringifyObject(verticalLevels.rankVerticalLevels));

            return verticalLevels;
        },

        _findLeftAndRightPartners: function(v, useOrdering)
        {
            var ordering = useOrdering ? useOrdering.vOrder : this.order.vOrder;   // useOrdering is passed when thi sis called from the initial ordering procedure

            var rank     = this.ranks[v];
            var orderV   = ordering[v];
            var outEdges = this.GG.getOutEdges(v);

            var leftEdges  = [];
            var rightEdges = [];

            for (var j = 0; j < outEdges.length; j++) {
                var rel = outEdges[j];
                if (this.ranks[rel] != rank) continue;
                if (ordering[rel] < orderV)
                    leftEdges.push(rel);
                else
                    rightEdges.push(rel);
            }

            var byDistToV = function(a,b){
                    var dist1 = Math.abs( ordering[a] - orderV );
                    var dist2 = Math.abs( ordering[b] - orderV );
                    return dist1-dist2;
                };
            leftEdges.sort ( byDistToV );
            rightEdges.sort( byDistToV );

            //console.log("v: " + v + ", leftP: " + Helpers.stringifyObject(leftEdges) + ", rightP: " + Helpers.stringifyObject(rightEdges));
            return { "leftPartners": leftEdges, "rightPartners": rightEdges };
        },

        // finds the bes tposition to insert a new twin of v which has the given set of relationships
        findBestTwinInsertPosition: function(v, insertedTwinRelationships, useOrdering) {     // useOrdering is passed when this is called from the initial ordering procedure
            var allTwins = this.GG.getAllTwinsOf(v);

            var rank      = this.ranks[v];
            var rankOrder = useOrdering ? useOrdering.order[rank] : this.order.order[rank];
            var vOrder    = useOrdering ? useOrdering.vOrder      : this.order.vOrder;

            var byOrder = function(a,b){ return vOrder[a] - vOrder[b]; };
            allTwins.sort( byOrder );

            // for each position [left of leftmost twin, ... ,right of rightmost twin] the total number
            // of partnership edges originating form all twins right of position going left of position
            // plus all edges from left of position going right of position
            // plus number of crossings due to edges to nodes in "insertedTwinRelationships" crossing other twins
            var numEdgesAcross  = [];
            for (var i = 0; i <= allTwins.length; i++)
                numEdgesAcross[i] = 0;

            //console.log("edges across: " + Helpers.stringifyObject(numEdgesAcross));

            // for each position compute number of edge crossings due to new twin edges
            var leftMostTwinOrder = vOrder[allTwins[0]];
            for (var i = 0; i < insertedTwinRelationships.length; i++) {
                var rel      = insertedTwinRelationships[i];
                var relOrder = vOrder[rel];

                if (relOrder < leftMostTwinOrder)
                    for (var j = 1; j <= allTwins.length; j++) // if we insert the twin at any position other then leftmost new edges will cross all twins to the left
                        numEdgesAcross[j] += j;
                else
                    for (var j = 0; j < allTwins.length; j++)  // if we insert the twin at any position other then rightmost new edges will cross all twins to the right
                        numEdgesAcross[j] += (allTwins.length - j);
            }

            //console.log("after self edges - edges across: " + Helpers.stringifyObject(numEdgesAcross));

            // for each position compute number of edge crossings due to existing twin edges
            for (var i = 0; i < allTwins.length; i++) {
                var partnerInfo = this._findLeftAndRightPartners(allTwins[i], useOrdering);
                var numLeftOf   = partnerInfo.leftPartners.length;
                var numRightOf  = partnerInfo.rightPartners.length;

                // TODO: can improve a bit when two of the twins are in a relationship and there are other twins
                for (var j = 0; j <= i; j++)
                    numEdgesAcross[j] += numLeftOf;
                for (var j = i + 1; j <= allTwins.length; j++)
                    numEdgesAcross[j] += numRightOf;

                //console.log("after twin " + allTwins[i] + " (leftOf: " + numLeftOf + ", rightOf: " + numRightOf + ") -> edges across: " + Helpers.stringifyObject(numEdgesAcross));
            }

            //console.log("twin penalties: " + Helpers.stringifyObject(numEdgesAcross));
            var orderOfLeftMostTwin  = vOrder[allTwins[0]];
            var minEdgeCrossLocation = Helpers.indexOfLastMinElementInArray(numEdgesAcross);   // (index == 0) => "insert before leftmost" => (order := orderOfLeftMostTwin)

            var order = orderOfLeftMostTwin + minEdgeCrossLocation;
            // increment the order by the number of relaitonship nodes found inbetween the twins,
            // so that minEdgeCrossLocation corresponds to the gap between the expected two twins
            for (var i = orderOfLeftMostTwin+1; i < order; i++) {
                var nodeID = rankOrder[i];
                if (this.GG.isRelationship(nodeID))
                    order++;
            }

            //console.log("edges across: " + Helpers.stringifyObject(numEdgesAcross));
            //console.log("BEST INSERT POSITION for a twin of " + v + " with edges to " + Helpers.stringifyObject(insertedTwinRelationships) + " is " + order);
            return order;
        },

        computeRankY: function(oldRanks, oldRankY)
        {
            var rankY = [0, 0];  // rank 0 is virtual, rank 1 starts at relative 0

            for ( var r = 2; r <= this.maxRank; r++ ) {
                var yDistance = (this.isChildhubRank(r)) ? this._computePersonRankHeight(r-1) : this.options.yDistanceChildhubToNode;

                // note: yExtraPerHorizontalLine * vertLevel.rankVerticalLevels[r] part comes from the idea that if there are many
                //       horizontal lines (childlines & relationship lines) between two ranks it is good to separate those ranks vertically
                //       more than ranks with less horizontal lines between them
                rankY[r] = rankY[r-1] + yDistance + this.options.yExtraPerHorizontalLine*(Math.max(this.vertLevel.rankVerticalLevels[r-1],1) - 1);
            }

            return rankY;
        },

        isChildhubRank: function(r)
        {
            for (var i = 0; i < this.order.order[r].length; i++) {
                if (this.GG.isPerson(this.order.order[r][i]) ||
                    this.GG.isRelationship(this.order.order[r][i])) return false;
                if (this.GG.isChildhub(this.order.order[r][i])) return true;
            }
            return true;
        },

        _computePersonRankHeight: function(r)
        {
            var height = this.options.yDistanceNodeToChildhub;

            var maxLabelLinesOnRank = 0;
            for (var i = 0; i < this.order.order[r].length; i++) {
                if (this.GG.isPerson(this.order.order[r][i])) {
                    var person = this.order.order[r][i];
                    var numLabelLines = 0;
                    var personProperties = this.GG.properties[person];

                    if (personProperties.hasOwnProperty("comments")) {
                        var comments = personProperties.comments.replace(/^\s+|\s+$/g,'');
                        // count number of new lines
                        numLabelLines += ((comments.match(/\n/g) || []).length + 1);
                    }
                    if (this.GG.properties[person].hasOwnProperty("phenotipsId")) {
                        numLabelLines += 1.1;
                    }
                    var dob = personProperties.hasOwnProperty("dob") ? new PedigreeDate(personProperties.dob) : null;
                    var dod = personProperties.hasOwnProperty("dod") ? new PedigreeDate(personProperties.dod) : null;
                    if (dob !== null && dob.isComplete()) {
                        numLabelLines++;
                    }
                    if (dod !== null && dod.isComplete()) {
                        numLabelLines++;
                    }
                    // if both DOB and DOD is given 2 lines are assumed. However this only happens
                    // when age is in years (and thus is displayed as e.g. "45 y")
                    if (dob !== null && dob.isComplete() && dod !== null && dod.isComplete()) {
                        var ageString = AgeCalc.getAgeString(dob, dod, editor.getPreferencesManager().getConfigurationOption("dateDisplayFormat"));
                        if (ageString.length < 2 || ageString.indexOf(" y") < 0) {
                            numLabelLines--;
                        }
                    }
                    if (personProperties.hasOwnProperty("lName") || personProperties.hasOwnProperty("fName")) {
                        numLabelLines++;
                    }
                    if (personProperties.hasOwnProperty("externalID")) {
                        numLabelLines++;
                    }
                    if (personProperties.hasOwnProperty("deceasedCause") || personProperties.hasOwnProperty("deceasedAge")) {
                        numLabelLines++;
                    }
                    if (personProperties.hasOwnProperty("childlessReason") && personProperties.childlessReason) {
                        numLabelLines+=0.9;
                    }
                    if (editor && editor.getPreferencesManager().getConfigurationOption("displayCancerLabels")) {
                        // count number of cancer labels
                        if (personProperties.hasOwnProperty("cancers")) {
                            for (var cancer in personProperties.cancers) {
                                if (personProperties.cancers.hasOwnProperty(cancer)) {
                                    if (personProperties.cancers[cancer].hasOwnProperty("affected")) {
                                        if (personProperties.cancers[cancer].affected) {
                                            numLabelLines += 1.15;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (numLabelLines > maxLabelLinesOnRank) {
                        maxLabelLinesOnRank = numLabelLines;
                    }
                }
            }
            if (maxLabelLinesOnRank > 3) {
                height += (maxLabelLinesOnRank - 3)*this.options.yCommentLineHeight;
            }
            return height;
        },

        computeNodeY: function( rank, level )
        {
            return this.rankY[rank] + (level - 1)*this.options.yExtraPerHorizontalLine;
        },

        computeRelLineY: function( rank, attachLevel, verticalLevel )
        {
            var attachY = this.rankY[rank] - (attachLevel)*this.options.yAttachPortHeight;

            var relLineY = this.rankY[rank];

            if (verticalLevel == 1) {
                // going above another line
                relLineY -= (attachLevel)*this.options.yAttachPortHeight;
            } else if (verticalLevel == 2) {
                // going above relationship node
                relLineY -= this.options.yExtraPerHorizontalLine * 1.25;
            } else {
                relLineY -= verticalLevel * this.options.yExtraPerHorizontalLine;
            }

            return {"attachY":  attachY, "relLineY": relLineY };
        },
        //========================================[vertical separation for horizontal edges]=

        //=[position]========================================================================

        displayGraph: function(xcoord, message)
        {
            TIME_DRAWING_DEBUG = 0;
            if (!DISPLAY_POSITIONING_DEBUG) return;

            var debugTimer = new Helpers.Timer();

            var renderPackage = { convertedG: this.GG,
                                  ranks:      this.ranks,
                                  ordering:   this.order,
                                  positions:  xcoord,
                                  consangr:   this.consangr };

            debug_display_processed_graph(renderPackage, 'output', true, message);

            TIME_DRAWING_DEBUG = debugTimer.report();
        },

        position: function()
        {
            var longEdges = this.find_long_edges();  // pre-find long edges for performance reasons

            var xcoord = new XCoord(null, this);
            //Helpers.printObject(xcoord.xcoord);

            //this.displayGraph(xcoord.xcoord, 'init');

            this.try_shift_right(xcoord, true, false);
            this.try_shift_right(xcoord, false, true);
            xcoord.normalize();

            //this.displayGraph(xcoord.xcoord, 'firstAdj');
            Helpers.printObject(xcoord.xcoord);

            var xbest     = xcoord.copy();
            var bestScore = this.xcoord_score(xbest);

            for ( var i = 0; i <= this.options.maxXcoordIterations; i++ )
            {
                this.try_shift_right(xcoord, true, true);
                this.try_straighten_long_edges(longEdges, xcoord);

                //this.displayGraph(xcoord.xcoord, 'Adj' + i);
                xcoord.normalize();

                var score = this.xcoord_score(xcoord);

                if (score.isBettertThan(bestScore)) {
                    xbest = xcoord.copy();
                    bestScore = score;
                }
                else
                    break;
            }

            //this.displayGraph(xbest.xcoord, 'final');
            Helpers.printObject(xbest.xcoord);

            return xbest.xcoord;
        },

        xcoord_score: function( xcoord,
                                considerOnlyRank, considerEdgesFromAbove, considerEdgesToBelow,
                                fromOrderOnRank )
        {
            // Returns xcoord score, the less the better.
            //
            //  Score equal to the     Σ     (  Ω(e) * ω(e) * X[w] − X[v]  )
            //                      e = (v,w)
            //
            //   where  Ω(e) is an internal value distinct from the input edge weight ω(e),
            //   defined to favor straightening long edges and edges from relationships to child nodes:
            //   - for long edges, it is more important to reduce the horizontal distance between virtual
            //   nodes, so chains may be aligned vertically and thus straightened. The failure to
            //   straighten long edges can result in a ‘‘spaghetti effect’’. Thus Ω(e) is higher for
            //   edges between virtual edges
            //   - for relationship-to-child nodes, it just produces a visually more pleasing arrangement
            //   - Ω(e) is computed by the edge_importance_to_straighten() function

            var score = new XCoordScore(this.GG.getMaxRealVertexId());

            var rankFrom = 1;
            var rankTo   = this.maxRank;
            if (typeof(considerOnlyRank) != "undefined") {
                // we have edges:
                //   1) r-1 -> r
                //   2) r   -> r
                //   3) r   -> r+1
                // If not consider above, need to exclude edges of type 1
                // If not consider below, need to exclude edges of type 3
                // Always care about edges of type 2

                rankFrom = considerOnlyRank-1;
                rankTo   = considerOnlyRank;
                if (rankFrom == 0) rankFrom = 1;
                if (!considerEdgesFromAbove) rankFrom = considerOnlyRank;  // exclude edges of type 1
            }

            for (var r = rankFrom; r <= rankTo; r++) {
                var len = this.order.order[r].length;

                var fromOrder = 0;
                if (typeof(considerOnlyRank) != "undefined" && r == considerOnlyRank)
                    fromOrder = fromOrderOnRank;

                for (var i = fromOrder; i < len; ++i) {
                    var v = this.order.order[r][i];

                    var outEdges = this.GG.getOutEdges(v);
                    var lenO     = outEdges.length;

                    for (var j = 0; j < lenO; ++j) {
                        var u = outEdges[j];

                        if (typeof(considerOnlyRank) != "undefined") {
                            if (!considerEdgesToBelow && this.ranks[u] != considerOnlyRank)  // exclude edges of type 3
                                continue;
                            if (this.ranks[u] == considerOnlyRank && this.order.vOrder[u] < fromOrderOnRank)
                                continue;
                            //if (considerEdgesFromAbove && considerEdgesToBelow && this.ranks[u] == considerOnlyRank && this.GG.isRelationship(v))
                            //    continue;
                        }

                        // have an edge from 'v' to 'u' with weight this.GG.weights[v][u]

                        // determine edge type: from real vertex to real, real to/from virtual or v. to v.
                        var coeff = this.edge_importance_to_straighten(v, u);

                        var w = this.options.xCoordEdgeWeightValue ? this.GG.weights[v][u] : 1.0;

                        var dist = Math.abs(xcoord.xcoord[v] - xcoord.xcoord[u]);

                        var thisScore = coeff * w * dist;

                        //if (this.GG.isChildhub(v)) thisScore *= Math.min( 15.0, dist );
                        //if (mostCompact) thisScore *= dist;  // place higher value on shorter edges

                        score.add(thisScore);

                        score.addEdge(v, u, dist);
                    }
                }
            }

            //console.log("XcoordScore: " + Helpers.stringifyObject(score));
            return score;
        },

        edge_importance_to_straighten: function(fromV, toV) {
            if (this.GG.isRelationship(toV)) return 1.0;

            if (!this.GG.isVirtual(fromV) && this.GG.isVirtual(toV)) return 1.5;

            if (this.GG.isRelationship(fromV)) return 8.0;

            if (this.GG.isVirtual(fromV)) {
                if (this.GG.isVirtual(toV)) return 16.0;
                return 4.0;
            }
            return 2.0;
        },

        try_shift_right: function(xcoord, scoreQualityOfNodesBelow, scoreQualityOfNodesAbove)
        {
            // goes over all ranks (top to bottom or bottom to top, depending on iteration)
            // and tries to shift vertices right one at a time. If a shift is good leaves it,
            // if not keeps going further.
            //
            // more precisely, tries to shift the vertex to the desired position up to and including
            // the position optimal according to the median rule, searching the positions in between.
            // Since we are not guaranteed the strictly increasing/decreasing score "smart" searches
            // such as binary search might not work well.

            //this.displayGraph( xcoord.xcoord, "shiftright-start" );
            for (var rr = 0; rr <= this.maxRank; rr++) {

                // go from top to bottom or bottom to top depending on which ranks (above or below)
                // we consider when trying to shift the nodes
                var r;
                if (!scoreQualityOfNodesAbove)
                    r = this.maxRank - rr;
                else
                    r = rr;

                if (r == 0) continue;  // disregard all discarded vertices

                var considerEdgesToBelow   = (scoreQualityOfNodesBelow || (r == 1)) && (r != this.maxRank);
                var considerEdgesFromAbove = (scoreQualityOfNodesAbove || r == this.maxRank) && (r != 1);

                var toO   = 0;
                var fromO = this.order.order[r].length - 1;

                for (var i = fromO; i >= toO; i--) {

                    var v = this.order.order[r][i];

                    // we care about the quality of resulting graph only for some ranks: sometimes
                    // only above the change, sometimes only below the change; in any case we know
                    // the change of position of vertices on this rank is not going to affect ranks
                    // far away, so we can only compute the score for the ranks we care about.

                    var median = this.compute_median(v, xcoord, considerEdgesFromAbove, considerEdgesToBelow);

                    //if (v == 28)
                    //    console.log("[28] median: " + median);

                    if (median != median)
                        median = xcoord.xcoord[v];

                    var maxShift = median - xcoord.xcoord[v];

                    // speed optimization: shift which we can do without disturbing other vertices and
                    //                     thus requiring no backup/restore process
                    var noDisturbMax = xcoord.getRightMostNoDisturbPosition(v);
                    var maxSafeShift = (noDisturbMax < median) ? noDisturbMax - xcoord.xcoord[v] : maxShift;

                    //if (v==28)
                    //    console.log("shiftright-rank-" + r + "-v-" + v + "  -->  DesiredShift: " + maxShift + ", maxSafe: " + maxSafeShift);

                    if (maxShift <= 0) continue;

                    var bestShift = 0;
                    var bestScore = this.xcoord_score(xcoord, r, considerEdgesFromAbove, considerEdgesToBelow, i);
                    var shiftAmount = maxShift;

                    //if (v==28)
                    //    console.log("InitScore: " + Helpers.stringifyObject(bestScore));
                    //if ( r == 7 ) this.displayGraph( xcoord.xcoord, "shiftright-rank-" + r + "-v-" + v + "(before)");

                    do
                    {
                        var newScore;

                        if (shiftAmount <= maxSafeShift)
                        {
                            xcoord.xcoord[v] += shiftAmount;
                            newScore = this.xcoord_score(xcoord, r, considerEdgesFromAbove, considerEdgesToBelow, i);
                            xcoord.xcoord[v] -= shiftAmount;
                        }
                        else
                        {
                            var newCoord = xcoord.copy();
                            newCoord.shiftRightAndShiftOtherIfNecessary(v, shiftAmount);
                            newScore = this.xcoord_score(newCoord, r, considerEdgesFromAbove, considerEdgesToBelow, i);
                        }

                        //if (v==9 || v == 10)
                        //    console.log("Shift: " + shiftAmount + ", score: " + newScore.score + " / " + Helpers.stringifyObject(newScore.inEdgeMaxLen));

                        if (newScore.isBettertThan(bestScore)) {
                            bestShift = shiftAmount
                            bestScore = newScore;
                        }

                        shiftAmount -= 1;
                    }
                    while (shiftAmount >= Math.max(0, maxSafeShift));

                    if (bestShift > 0) {
                        xcoord.shiftRightAndShiftOtherIfNecessary(v, bestShift);
                    }
                    //if ( r == 7 ) this.displayGraph( xcoord.xcoord, "shiftright-rank-" + r + "-v-" + v );
                }

                //this.displayGraph( xcoord.xcoord, "shiftright-rank-" + r + "-end");
            }
        },

        compute_median: function (v, xcoord, considerAbove, considerBelow)
        {
            var positionsAbove = [];
            var positionsBelow = [];

            var allEdgesWithWeights = this.GG.getAllEdgesWithWeights(v);

            for (var u in allEdgesWithWeights) {
                if (allEdgesWithWeights.hasOwnProperty(u)) {
                    if (u == v) continue;
                    var weight = allEdgesWithWeights[u]["weight"];

                    var coeff = allEdgesWithWeights[u]["out"] ?
                                this.edge_importance_to_straighten(v, u) :
                                this.edge_importance_to_straighten(u, v);

                    var w = this.options.xCoordEdgeWeightValue ? weight : 1.0;

                    var score = coeff * w;

                    for (var i = 0; i < score; i++) {
                        if (this.ranks[u] <= this.ranks[v]) {
                            positionsAbove.push(xcoord.xcoord[u]);
                        }
                        if (this.ranks[u] >= this.ranks[v]) {
                            positionsBelow.push(xcoord.xcoord[u]);
                        }
                    }
                }
            }

            var numericSortFunc = function(a,b) { return a - b; };

            var median      = undefined;
            var medianAbove = undefined;
            var medianBelow = undefined;

            if ((considerAbove || this.GG.isVirtual(v)) && positionsAbove.length > 0) {
                positionsAbove.sort(numericSortFunc);
                var middle  = Math.ceil(positionsAbove.length/2);
                if (middle >= positionsAbove.length) {
                    middle = positionsAbove.length - 1;
                }
                if (positionsAbove.length % 2 == 0)
                    medianAbove = Math.floor((positionsAbove[middle] + positionsAbove[middle-1])/2);
                else
                    medianAbove = positionsAbove[middle];
            }
            if ((considerBelow || this.GG.isVirtual(v)) && positionsBelow.length > 0) {
                positionsBelow.sort(numericSortFunc);
                var middle  = Math.ceil(positionsBelow.length/2);
                if (middle >= positionsBelow.length)
                    middle = positionsBelow.length - 1;
                if (positionsBelow.length % 2 == 0)
                    medianBelow = Math.floor((positionsBelow[middle] + positionsBelow[middle-1])/2);
                else
                    medianBelow = positionsBelow[middle];
            }

            if (medianAbove !== undefined && medianBelow !== undefined)
                median = Math.max(medianAbove, medianBelow);
            else if (medianAbove !== undefined)
                median = medianAbove;
            else
                median = medianBelow;

            return Math.ceil(median);
        },

        try_straighten_long_edges: function (longEdges, xcoord)
        {
            // try to straigten long edges without moving any other vertices
            var improved = false;

            for (var e = 0; e < longEdges.length; ++e) {
                var chain = longEdges[e];
                //console.log("trying to straighten edge " + Helpers.stringifyObject(chain));

                // 1) try to straighten by shifting the head

                // go over all nodes from head to tail looking for a bend and trying
                // to move the head to remove the bend
                var currentCenter = xcoord.xcoord[chain[0]];
                var corridorLeft  = xcoord.getLeftMostNoDisturbPosition(chain[0]);
                var corridorRight = xcoord.getRightMostNoDisturbPosition(chain[0]);

                // go over all nodes from head to tail looking for a bend
                for (var i = 1; i < chain.length; ++i) {
                    var nextV      = chain[i];
                    var nextCenter = xcoord.xcoord[nextV];
                    if (nextCenter != currentCenter) {
                        if (nextCenter >= corridorLeft && nextCenter <= corridorRight) {
                            // all the nodes above can be shifted to this location!
                            for (var j = 0; j < i; ++j) {
                                xcoord.xcoord[chain[j]] = nextCenter;
                            }
                            improved      = true;
                            currentCenter = nextCenter;
                        }
                        else break;
                    }
                    // narrow the coridor to the common available space including this vertex as well
                    corridorLeft  = Math.max(corridorLeft,  xcoord.getLeftMostNoDisturbPosition(nextV));
                    corridorRight = Math.min(corridorRight, xcoord.getRightMostNoDisturbPosition(nextV));
                    if (corridorRight < corridorLeft) break;  // no luck, can't straighten
                }

                // 2) try to straighten by shifting the tail

                // go over all nodes from tail to head looking for a bend and trying
                // to move the tail to remove the bend
                var lastNode      = chain.length - 1;
                var currentCenter = xcoord.xcoord[chain[lastNode]];
                var corridorLeft  = xcoord.getLeftMostNoDisturbPosition(chain[lastNode]);
                var corridorRight = xcoord.getRightMostNoDisturbPosition(chain[lastNode]);

                // go over all nodes from head to tail looking for a bend
                for (var i = lastNode - 1; i >= 0; --i) {
                    var nextV      = chain[i];
                    var nextCenter = xcoord.xcoord[nextV];
                    if (nextCenter != currentCenter) {
                        if (nextCenter >= corridorLeft && nextCenter <= corridorRight) {
                            // all the nodes below can be shifted to this location!
                            for (var j = lastNode; j > i; j--) {
                                xcoord.xcoord[chain[j]] = nextCenter;
                            }
                            improved      = true;
                            currentCenter = nextCenter;
                        }
                        else break;
                    }
                    // narrow the coridor to the common available space including this vertex as well
                    corridorLeft  = Math.max(corridorLeft,  xcoord.getLeftMostNoDisturbPosition(nextV));
                    corridorRight = Math.min(corridorRight, xcoord.getRightMostNoDisturbPosition(nextV));
                    if (corridorRight < corridorLeft) break;  // no luck, can't straighten
                }
            }

            return improved;
        },

        validatePositions: function(positions)
        {
            try {
                var xcoord = new XCoord(positions, this);
            } catch(err) {
                return false;
            }
            return true;
        },

        //========================================================================[position]=

        find_long_edges: function()
        {
            var longEdges = [];

            var maxRealId = this.GG.getMaxRealVertexId();
            var numVert   = this.GG.getNumVertices();

            var checked = [];
            for (var v = maxRealId+1; v < numVert; v++)
                checked[v] = false;

            for (var v = maxRealId+1; v < numVert; v++) {

                if (checked[v] || this.ranks[v] == 0) continue;

                // find a long edge - an edge connecting real nodes of non-neighbouring ranks,
                // consisting of virtual vertices on intermediate ranks (plus source/target)

                // start from head node - the first virtual node
                var head = v;
                while (this.GG.isVirtual(this.GG.getInEdges(head)[0]))
                    head = this.GG.getInEdges(head)[0];

                var chain = [];
                var next  = head;

                // go towards the tail through out-edges
                do {
                    checked[next] = true;
                    chain.push(next);
                    next = this.GG.getOutEdges(next)[0];
                }
                while (this.GG.isVirtual(next));;

                console.log("Found long edge " + Helpers.stringifyObject(chain));
                longEdges.push(chain);
            }
            return longEdges;
        }
    };


    //-------------------------------------------------------------

    var DISPLAY_POSITIONING_DEBUG = false;
    var TIME_DRAWING_DEBUG        = 0;

    function make_dynamic_positioned_graph( inputG, probandNodeId, debugOutput )
    {
        var timer = new Helpers.Timer();

        if (debugOutput)
            DISPLAY_POSITIONING_DEBUG = true;
        else
            DISPLAY_POSITIONING_DEBUG = false;

        var drawGraph = new PositionedGraph( inputG, probandNodeId );

        console.log( "=== Running time: " + timer.report() + "ms ==========" );

        return new DynamicPositionedGraph(drawGraph);
    }

    return PositionedGraph;
});
