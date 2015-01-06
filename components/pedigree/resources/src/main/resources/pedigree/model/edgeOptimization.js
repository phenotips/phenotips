// TODO: test performance improvement: this.pairScoreFunc() is called for all possible combinations of (i,j,level_i,level_j)
//                                     during computeCrosses(). Store the values and avoid calling the function again?
//                                     (may not make any difference since function computation is trivial)

VerticalPosIntOptimizer = function ( pairScoreFunc, initLevels, minLevels ) {
    this.pairScoreFunc = pairScoreFunc;      // function(u, v, uLev, vLev) - returns penalty for interaction between two edges u and v when u's level is uLev and v's level is vLev
                                             //                              (assumed to be symetrical, i.e. f(a,b,levA,levB) = f(b,a,levB,levA)

    this.initLevels = initLevels;            // array[int]

    this.maxOfMinlevels = 1;
    if (minLevels) {
        this.maxOfMinlevels = Math.max.apply(null, minLevels);
        if (this.maxOfMinlevels > 1)
            this.minLevels = minLevels;
    }

    var precompute = this.computeComponents();
    //console.log("Precomputed: " + stringifyObject(precompute));
    this.components = precompute.components;  // class             - mapping between edges and connected components; see Components class
    this.crosses    = precompute.crosses;     // array[array[int]] - for each edge the list of edges it directly intersects with (for performance optimization)
};

VerticalPosIntOptimizer.prototype = {

    // computes penalty ofr the number of levels used.
    // value: (0 <= numberOfLevelsPenalty < 1) => this affects the total penalty less than a single extra crossing,
    // i.e. we want to use as few levels as possible, but it is beter to use an extra level than to have more crossings
    numberOfLevelsPenalty: function( maxLevelUsed, minRequired, numEdges ) {
        return ( maxLevelUsed - minRequired ) / (numEdges+1);       // 1 level => (penalty == 0), above 1 level => (0 < penalty < 1)
    },

    componentScoreFunc: function ( levels, componentID ) {
        //console.log("scoring: " + stringifyObject(levels));

        var penalty      = 0;
        var maxLevelUsed = 0;
        var minRequired  = 1;

        var component = this.components.getComponentEdges(componentID);

        for (var i = 0; i < component.length; i++) {
            var edge    = component[i];
            if (levels[edge] > maxLevelUsed)
                maxLevelUsed = levels[edge];
            if (this.minLevels && this.minLevels[edge] > minRequired)
                minRequired = this.minLevels[edge];
            var crosses = this.crosses[edge];
            for (var j = 0; j < crosses.length; j++) {
                var intersects = crosses[j];
                if (intersects > edge) {                  // because we want to only count each intersection only once, and score func is symmetrical
                    //console.log("[p] " + edge + " + " + intersects + " = " + this.pairScoreFunc( edge, intersects, levels[edge], levels[intersects] ));
                    penalty += this.pairScoreFunc( edge, intersects, levels[edge], levels[intersects], levels );
                    if (!isFinite(penalty))
                        return penalty;
                }
            }
        }

        // add penalty for the number of levels used: less levels is better given the same "crossing" score
        // (note: only care about max level used, as that affects the layout, we don't care if there are unused levels inbetween)
        var numLevelsPen = this.numberOfLevelsPenalty(maxLevelUsed, minRequired, component.length);
        penalty += numLevelsPen;
        //console.log("num levels penalty: " + numLevelsPen);

        //console.log("Score: " + penalty + " (numLevels: " + numLevelsPen + ")");
        return penalty;
    },

    computeComponents: function () {
        // find all connected components, for now - using plain (and likely non-optimal) O(n^2) algo
        // (as a side effect can compute minimum posible penalty score)

        var components = new Complonents();   // mapping between edges and connected components; see Components class

        var crosses = [];                     // for each edge the list of edges it directly intersects with

        var hasToBeAboveForPerfectScore = []; // used for heurisic computation of minimum number of levels required

        var numEdges = this.initLevels.length;
        for (var i = 0; i < numEdges; i++) {
            crosses[i] = [];
            hasToBeAboveForPerfectScore[i] = [];
        }

        for (var i = 0; i < numEdges-1; i++) {
            for (var j = i+1; j < numEdges; j++) {
                if (this.pairScoreFunc( i, j, 1, 1 ) == Infinity) {  // only happens when edges intersect and can't be at the same level
                    crosses[i].push(j);
                    crosses[j].push(i);

                    var componentI = components.getEdgeComponent(i);
                    var componentJ = components.getEdgeComponent(j);

                    if (componentI === undefined && componentJ === undefined) {
                        // both i and j are not in any component yet
                        components.addToNewComponent(i);
                        components.addToExistingComponent(j, components.getEdgeComponent(i));
                    }
                    else if (componentI !== undefined) {
                        if (componentJ !== undefined) {
                            // both i and j are assigned to a component
                            if (componentI != componentJ) {
                                // ...to different components: merge (ow we are ok)
                                components.mergeComponents(componentI, componentJ);
                            }
                        } else {
                            // i has a component, j does not
                            components.addToExistingComponent(j, componentI);
                        }
                    } else {
                        // j has a component, i does not
                        components.addToExistingComponent(i, componentJ);
                    }

                    // if edges cross => the best arrangement of these two edges is either one above the other or v.v.
                    // in any case there may be some penalty associated for that, but can't do beter than the best
                    // of these two options. Compute it so that later once (if) minimum possible penalty is achieved
                    // the algorithm stop without wasting more time
                    var scoreAbove = this.pairScoreFunc( i, j, 1, 2 );
                    var scoreBelow = this.pairScoreFunc( i, j, 2, 1 );

                    var compID = components.getEdgeComponent(i);
                    components.addRequiredPenaltyToComponent(compID, Math.min( scoreAbove, scoreBelow ));

                    if (scoreAbove < scoreBelow) {
                        hasToBeAboveForPerfectScore[i].push(j);
                    }
                    if (scoreAbove > scoreBelow) {
                        hasToBeAboveForPerfectScore[j].push(i);
                    }
                }
            }
        }

        // it is good to know best possible score so that algorithm may stops earlier if this score is
        // achieved; it is hard to compute precisely though, as that is equivalent to solving the problem.
        //
        // One posible heuristic is the size of the largest clique, but that is hard to compute (clique problem
        // is hard), and even if at most 2 edges overlap at the same time, may need many levels as they stack
        // below each other (e.g. in a stairs-like structure)
        //
        // So the following dumb heuristic is used to determine num levels needed for each component:
        //  - for each edge min level is max( minLevel given in the input, max(min levels of all edges it should be above) )

        for (var compID = 0; compID < components.getNumComponents(); compID++) {

            // if min penalty is above 0 it means at least one crossing => at least 2 levels are required
            var minNumLevels = 1;
            var minRequired  = 1;
            var minMinLevel  = Infinity;

            var component = components.getComponentEdges(compID);
            for (var i = 0; i < component.length; i++) {
                var edge = component[i];

                if (this.minLevels) {
                    if (this.minLevels[edge] > minRequired)
                        minRequired = this.minLevels[edge];
                    if (this.minLevels[edge] < minMinLevel)
                        minMinLevel = this.minLevels[edge];
                }

                var minForThisEdge = this.minLevels ? this.minLevels[edge] : 1;

                for (var j = 0; j < hasToBeAboveForPerfectScore[edge].length; j++) {
                    var needToBeAboveEdge = hasToBeAboveForPerfectScore[edge][j];
                    var minForOtherEdge = this.minLevels ? this.minLevels[edge] : 1;
                    // if the other edge also has to be above something and has min level
                    if (hasToBeAboveForPerfectScore[needToBeAboveEdge].length > 0 && minForOtherEdge == 1)
                        minForOtherEdge++;
                    minForThisEdge = Math.max( minForThisEdge, minForOtherEdge + 1 );
                }

                minNumLevels = Math.max( minNumLevels, minForThisEdge );
            }

            var needExtraLevelsAboveMin = (components.getMinPossiblePenalty(compID) == 0) ? 0 : 1;
            if (!isFinite(minMinLevel)) minMinLevel = 1;
            minNumLevels = Math.max(minNumLevels, minMinLevel + needExtraLevelsAboveMin);

            // this penalty is guaranteed to be less than 1 => this affects the penalty less than a single extra crossing
            var penaltyForNumLevelsUsed = this.numberOfLevelsPenalty(minNumLevels, minRequired, component.length);
            components.addRequiredPenaltyToComponent(compID, penaltyForNumLevelsUsed);
        }

        //console.log("Components: " + stringifyObject(components));
        return {"crosses": crosses, "components": components };
    },

    computeVerticalPositions: function( maxFullSearchSize, maxSteps, seed ) {
        // maxFullSearchSize - max number of edges in a cluster which can be searched fully via brute force; max time = C1 * maxFullSearchSize! per cluster
        // maxSteps          - max number of steps for a heuristic; max time = C2 * maxSteps per cluster
        //                     (where C2 > C1, and both include computing the penalty for a given arrangmenet, which is ~O(clusterSize^2)

        this.seed = seed ? seed : 1;

        var bestSoFar = this.initLevels;

        // fix component-by-component
        for (var compID = 0; compID < this.components.getNumComponents(); compID++) {

            //console.log("Optimizing next component [ID="+compID+"] with edges: " + stringifyObject(this.components.getComponentEdges(compID)));

            //console.log("CompID[" + compID + "]: MinPossiblePenalty: " + this.components.getMinPossiblePenalty(compID));
            //console.log("CompID[" + compID + "]: Initial Penalty:    " + this.componentScoreFunc(bestSoFar, compID));
            //console.log("CompID[" + compID + "]: Initial assignment: " + stringifyObject(bestSoFar));

            // problem size for exhaustiveSearch ~= numEdges!, can't afford to try all combinations for large problems
            if ( this.components.getComponentEdges(compID).length <= maxFullSearchSize )
                bestSoFar = this.exhaustiveSearch( compID, bestSoFar );
            else
                bestSoFar = this.simulatedAnnellingOptimizer( compID, bestSoFar, maxSteps );

            //console.log("CompID[" + compID + "]: Final assignment: " + stringifyObject(bestSoFar));
            //console.log("CompID[" + compID + "]: Final Penalty:    " + this.componentScoreFunc(bestSoFar, compID));
        }

        //console.log("Final assignment: " + stringifyObject(bestSoFar));

        return bestSoFar;
    },

    //----------------------------------------------------------------------------------
    exhaustiveSearch: function( componentID, bestSoFar ) {

        var initScore = this.componentScoreFunc(bestSoFar, componentID)

        //this.checkedNumber = 0; // TODO: debug
        //console.log("minPossiblePenalty: " + this.components.getMinPossiblePenalty(componentID));

        var result = this.recursiveExhaustiveSearch( componentID, bestSoFar.slice(0), 0, {"values":bestSoFar, "score":initScore} );

        //console.log("[fsearch] CompID[" + componentID + "]: Tried " +  this.checkedNumber + " combinations"); // TODO: debug

        return result.values;
    },

    recursiveExhaustiveSearch: function ( componentID, valuesSoFar, level, bestSoFar ) {

        var component = this.components.getComponentEdges(componentID);

        // reached the end of the recursion
        if (level == component.length) {
            var score = this.componentScoreFunc(valuesSoFar, componentID);
            //console.log("SCORING: " + stringifyObject(valuesSoFar) + " -> Score: " + score );
            if (score < bestSoFar.score) {
                bestSoFar.values = valuesSoFar.slice(0);
                bestSoFar.score  = score;
                //console.log("[fsearch] New best: " + stringifyObject(bestSoFar.values) + " (score: " + bestSoFar.score + ")");
            }
            //console.log("best value at enter [" + level + "]: " + stringifyObject(valuesSoFar));
            //this.checkedNumber++; // TODO: debug
            return bestSoFar;
        }

        var edge = component[level];

        // TODO: since we don't want any gaps in levels, a smarter search can be used which fills the gaps
        //       once it is known that if they are not filled now the arrangmenet will be incorrect

        // TODO: exclude assignments where two intersecting edges have the same level. Only consider edges
        //       with lover IDs since those are already assigned a value. Edges with higher ids haveno assignment yet

        var minValue = 1;
        var maxValue = component.length;

        if (this.minLevels) {
            minValue = this.minLevels[edge];
            maxValue += (minValue - 1);
        }

        for (var i = minValue; i <= maxValue; i++ ) {
            valuesSoFar[edge] = i;

            bestSoFar = this.recursiveExhaustiveSearch( componentID, valuesSoFar, level+1, bestSoFar );

            if (bestSoFar.score == this.components.getMinPossiblePenalty(componentID)) break;
        }
        //console.log("best value at exit [" + level + "]: " + stringifyObject(bestSoFar.values) + " (score: " + bestSoFar.score + ")");

        return bestSoFar;
    },
    //----------------------------------------------------------------------------------


    //--[ simulatedAnnelling() related ]------------------------------------------------
    makeBasicValidAssignment: function ( initLevels, componentID ) {
        var component = this.components.getComponentEdges(componentID);
        var value = 1;

        // give each edge that crosses a separate level - may not be optimal, but guaranteed to be "valid" with a penalty below Infinity
        var newAssignemnt = initLevels.slice(0);
        for (var i = 0; i < component.length; i++) {
            var edge = component[i];
            if (this.minLevels && value < this.minLevels[edge])
                value = this.minLevels[edge];
            newAssignemnt[edge] = value;
            value++; // for next age to be different form this one
        }

        //console.log("[asearch] CompID[" + componentID + "]: Initial assignment:   " + stringifyObject(initLevels)    + ", score: " + this.componentScoreFunc(initLevels, componentID));
        //console.log("[asearch] CompID[" + componentID + "]: InitValid assignment: " + stringifyObject(newAssignemnt) + ", score: " + this.componentScoreFunc(newAssignemnt, componentID));
        return newAssignemnt;
    },

    computeNeighbour: function ( currentState, componentID, step ) {
        // general idea: assign some other "level" to one of the edges (where "some" and "one of" can be picked randomly/depending on the `step`).
        //               If that level is forbidden due to a crossing with some other edges assign the first OK level in the direction of change
        //               + normalize after the change (e.g. remove unused levels + shift to make smallest used levle to be level #1)

        // 1. pick random edge
        // 2. pick new random level for the edge (different form the old value)
        // 3. while (value is forbidden or unchanged) increment value (if new>old) or decrement value (if new<old)

        //console.log("computeNeighbour - current: " + stringifyObject(currentState));

        var component = this.components.getComponentEdges(componentID);

        var newState = currentState.slice(0);

        do {
            // pick a random edge in the component
            var edge         = component[ Math.floor(this.random() * component.length) ];
            var oldLevel     = newState[edge];
            var maxUsedLevel = oldLevel;

            // check random value and if it makes sense to increment or decrement it
            var isBelowAll = true;
            var isAboveAll = true;
            var forbidden  = {};
            for (var i = 0; i < this.crosses[edge].length; i++) {
                var crossesWith = this.crosses[edge][i];
                var crossLevel  = newState[crossesWith];
                if (crossLevel > maxUsedLevel)
                    maxUsedLevel = crossLevel;
                forbidden[crossLevel] = true;
                if (crossLevel >= oldLevel)
                    isAboveAll = false;
                if (crossLevel <= oldLevel)
                    isBelowAll = false;
            }
            if (this.minLevels && oldLevel == this.minLevels[edge]) {
                isBelowAll = true;       // if level == minLevel for the edge does not make sense to decrese the level
            }
        }
        while (isAboveAll && isBelowAll);  // if both above all and below all no sense to play with the edge; need to pick another edge

        // pick new random level for the edge (different form the old value)
        var newLevel;
        do {
            // note: new level will be in the range = [0...maxUsedLevel+1]
            // note: 0 is not a valid "level" but lets the edge to be
            // positioned below any other (and later normalized to have proper minLevel)
            newLevel = Math.floor(this.random()*(maxUsedLevel + 2));
        }
        while ( newLevel == oldLevel || (isBelowAll && newLevel < oldLevel) || (isAboveAll && newLevel > oldLevel));

        if (forbidden.hasOwnProperty(newLevel)) {
            for (var i = 0; i < component.length; i++) {
                var e = component[i];
                if (newState[e] <= newLevel) {
                    newState[e]++;
                }
            }
        }
        newState[edge] = newLevel;
        //console.log("Edge: " + edge + ", oldLevel: " + oldLevel + ", newLevel: " + newLevel);

        this.normalize(newState, component);
        //console.log("computeNeighbour: " + stringifyObject(newState) + ", score: " + this.componentScoreFunc( newState, componentID ));

        return newState;
    },

    normalize: function( levels, component ) {
        //console.log("pre-normalized levels: " + stringifyObject(levels));

        // 1. if there are gaps (e.g. 1,2,4,5) decrement all levels above each gap to fill the gap
        var usedLevels = filterUnique(levels).sort();
        for (var i = usedLevels.length-1; i > 0; i--) {
            if (usedLevels[i] != usedLevels[i-1] + 1) {      // there may be only one gap so no need to implement a more robust algorithm
                for (var j = 0; j < component.length; j++) {
                    var e = component[j];
                    if (levels[e] >= usedLevels[i])
                        levels[e]--;
                }
                //console.log("found gap [" + usedLevels[i-1] + " - " + usedLevels[i] + "]");
                break;
            }
        }
        //console.log("post fill gap levels: " + stringifyObject(levels));

        // 2. make sure all edges satisfy their min levels (including fixing 0 level)
        for (var i = 0; i < component.length; i++) {
            var edge = component[i];

            var curLevel = levels[edge];
            var minLevel = this.minLevels ? this.minLevels[edge] : 1;

            if (curLevel < minLevel) {
                var adjust = minLevel - curLevel;
                for (var j = 0; j < component.length; j++) {
                    levels[component[j]] += adjust;
                }
            }
        }

        // 3. try to minimize the highest used level
        do {
            var changed = false;
            for (var i = 0; i < component.length; i++) {
                var edge = component[i];
                var curLevel = levels[edge];
                var minLevel = this.minLevels ? this.minLevels[edge] : 1;
                if (curLevel > minLevel) {
                    var highestBelow = 0;
                    for (var j = 0; j < this.crosses[edge].length; j++) {
                        var level = levels[this.crosses[edge][j]];
                        if (level < curLevel && level > highestBelow)
                            highestBelow = levels[this.crosses[edge][j]];
                    }
                    if (highestBelow < curLevel - 1) {
                        levels[edge] = highestBelow + 1;
                    }
                }
            }
        } while (changed);

        //console.log("post normalization: " + stringifyObject(levels));
    },

    localOptimization: function( levels, currentScore, componentID, untilFirstImprovement ) {
        /*
         * TODO: after improvements made to the main algorithm this heuristic may not be necessary,
         *       but leaving it here for now as a placeholder
         *
        // tries to find a better position for just one edge using complete search
        // (e.g. local not global optimization)

        var component = this.components.getComponentEdges(componentID);
        for (var e = 0; e < component.length; e++) {
            var edge     = component[e];
            var curLevel = levels[edge];
            var minLevel = this.minLevels ? this.minLevels[edge] : 1;

            var forbidden  = {};
            for (var i = 0; i < this.crosses[edge].length; i++) {
                var crossesWith = this.crosses[edge][i];
                var crossLevel  = levels[crossesWith];
                forbidden[crossLevel] = true;
            }
            // TODO
        }
        */
        return currentScore;
    },

    random: function() {
        // very crude but ok for the purpose of this algorithm in the context of vertical edge allocation
        // http://jsfiddle.net/bhrLT/3/
        var x = Math.sin(this.seed++)*16;
        return x - Math.floor(x);
    },

    doSwitchDuringAnneling: function ( oldScore, newScore, stepsSinceReset ) {
        if (newScore <= oldScore) return true;

        var probability = Math.exp( -(newScore - oldScore) * Math.log((stepsSinceReset+1)*5) );

        if (probability > this.random()) {
            //console.log("[debug] switch to worse score");
            return true;
        }
        return false;
    },

    simulatedAnnellingOptimizer: function ( componentID, bestSoFar, maxSteps ) {

        //console.log("[asearch] Starting simulatedAnnellingOptimizer");

        var bestScore = this.componentScoreFunc(bestSoFar, componentID);

        var bestState = isFinite(bestScore) ? bestSoFar : this.makeBasicValidAssignment(bestSoFar, componentID);
        var bestScore = isFinite(bestScore) ? bestScore : this.componentScoreFunc(bestState, componentID);
        var bestStep  = maxSteps;

        var currentState = bestState;
        var currentScore = bestScore;

        //console.log("Component: " + stringifyObject(this.components.getComponentEdges(componentID)) +
        //            ", init score: " + bestScore + ", best possible score: " + this.components.getMinPossiblePenalty(componentID));

        // alogrithm (as in wiki):
        //-------------------------------------------
        // s ← s0; e ← E(s)                                  // Initial state, energy.
        // sbest ← s; ebest ← e                              // Initial "best" solution
        // k ← 0                                              // Energy evaluation count.
        // while k < kmax and e > emax                        // While time left & not good enough:
        //   T ← temperature(k/kmax)                          // Temperature calculation.
        //   snew ← neighbour(s)                              // Pick some neighbour.
        //   enew ← E(snew)                                   // Compute its energy.
        //   if P(e, enew, T) > random() then                 // Should we move to it?
        //     s ← snew; e ← enew                            // Yes, change state.
        //   if enew < ebest then                             // Is this a new best?
        //     sbest ← snew; ebest ← enew                    // Save 'new neighbour' to 'best found'.
        //   k ← k + 1                                       // One more evaluation done
        // return sbest                                      // Return the best solution found.

        var maxWrongDirection = maxSteps/6;

        var step = maxSteps;
        while (bestScore > this.components.getMinPossiblePenalty(componentID) && step >= 0) {

            // reset once in the middle of the search (TODO: investigate if we need more resets or don't need resets at all)
            if (step < bestStep - maxWrongDirection) {
                currentState = bestState.slice(0);
                currentScore = this.localOptimization(currentState, bestScore, componentID, true);  // restart from a slightly optimized last best point
                bestStep     = step;
                console.log("[asearch] reset to: " + stringifyObject(currentState) + ", score: " + currentScore + " (@ step = " + (maxSteps - step + 1) + ")");                
            }

            var neighbourState = this.computeNeighbour( currentState, componentID, step );
            var neighbourScore = this.componentScoreFunc( neighbourState, componentID );

            if ( this.doSwitchDuringAnneling( currentScore, neighbourScore, bestStep - step ) ) {
                currentState = neighbourState;
                currentScore = neighbourScore;
            }

            if (currentScore < bestScore) {
                console.log("[asearch] New best: " + stringifyObject(currentState) + ", score: " + currentScore + " (@ step = " + (maxSteps - step + 1) + ")");
                bestState = currentState.slice(0);
                bestScore = currentScore;
                bestStep  = step;
            }

            step--;
        }

        //bestState = [1, 2, 3, 5, 6, 7, 4, 6, 7, 4];
        //bestScore = this.componentScoreFunc( bestState, componentID );
        bestScore = this.localOptimization(bestState, bestScore, componentID);
        console.log("[asearch] Final optimized best: " + stringifyObject(bestState) + ", score: " + bestScore);

        return bestState;
    }
    //-------------------------------------------------[ simulatedAnnelling() related ]--
};


Complonents = function() {
    this.components         = []; // arrat[arrat[int]] - for each component -> a list of edges
    this.minPossiblePenalty = []; // array[double]     - for each component min possible penalty per component

    // Note: data in edgeComponents can be derived from data in components (and v.v.),
    //       but both are stored for performance reasons
    this.edgeComponents = [];  // array[int] - for each edge -> component ID
};

Complonents.prototype = {
    getNumComponents: function() {
        return this.components.length;
    },

    getComponentEdges: function(componentID) {
        return this.components[componentID];
    },

    // returns component ID of the edge
    getEdgeComponent: function( edge ) {
        return this.edgeComponents[edge];
    },

    addToNewComponent: function( edge ) {
        this.edgeComponents[edge] = this.components.length;
        this.components.push([edge]);   // new component has just one edge
        this.minPossiblePenalty.push(0);        // new component has 0 min penalty
    },

    addToExistingComponent: function( edge, componentID) {
        this.components[componentID].push(edge);
        this.edgeComponents[edge] = componentID;
    },

    // NOTE: may reassign some unrelated component IDs
    mergeComponents: function( component1, component2) {
        if (component1 == component2)
            return;
        var minID = Math.min(component1, component2);
        var maxID = Math.max(component1, component2);

        // move all edges in maxID to minID
        for (var i = 0; i < this.components[maxID].length; i++) {
            var edge = this.components[maxID][i];
            this.addToExistingComponent(edge, minID);
        }

        // add penalties
        this.minPossiblePenalty[minID] += this.minPossiblePenalty[maxID];

        // remove component maxID
        this.components.splice(maxID,1);
        this.minPossiblePenalty.splice(maxID,1);
    },

    addRequiredPenaltyToComponent: function( componentID, penalty ) {
        this.minPossiblePenalty[componentID] += penalty;
    },

    // for performance reasons we want to stop as soon as we hit the best possible assignment;
    // while it is hard to compute the actual optimal score, we can estimate it from below,
    // such that if this low estimate is achieved we are guaranteed we have the best assignment
    getMinPossiblePenalty: function( componentID ) {
        return this.minPossiblePenalty[componentID];
    }
};

