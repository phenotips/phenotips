// TODO: compute independent sub-sets of crosing edges, and optimize those
//       sub-sets independently - problem size may be much smaller this way
//       (each sub-set may be optimized using already implemented methods, and results for different sub-sets combined)
//
// TODO: test performance: this.pairScoreFunc() is called for all possible combinations of (i,j,level_i,level_j)
//                         during computeCrosses(). Store the values and avoid calling the function again?

VerticalPosIntOptimizer = function ( pairScoreFunc, initLevels, minLevels ) {
    this.pairScoreFunc = pairScoreFunc;      // function(u, v, uLev, vLev) - returns penalty for interaction between two edges u and v when u's level is uLev and v's level is vLev

    this.initLevels = initLevels;            // array[int]
    this.numEdges   = initLevels.length;     // int

    this.maxOfMinlevels = 1;
    if (minLevels) {
        this.maxOfMinlevels = Math.max.apply(null, minLevels);
        if (this.maxOfMinlevels > 1)
            this.minLevels = minLevels;
    }

    var precompute          = this.computeCrosses();
    this.minPossiblePenalty = precompute.minPossiblePenalty; // int               - the minimum possible penalty (the sum of minimum penalties for each edge pair)
    this.crosses            = precompute.crosses;            // array[array[int]] - for each edge pre-compute which edges it intersects with (for performance optimization)
    this.edgesThatCross     = precompute.edgesThatCross;     // array[int]        - list of edges that cross at least one edge (for performance optimization)

    this.initScore = this.scoreFunc(initLevels);

    //console.log("Crosses: " +  stringifyObject(this.crosses));
    console.log("[Optimizer] MinPossiblePenalty: " + this.minPossiblePenalty + ", InitialPenalty: " + this.initScore);
};

VerticalPosIntOptimizer.prototype = {

    numberOfLevelsPenalty: function( numLevelsUsed ) {
        return ( numLevelsUsed - 1 ) / this.numEdges;       // 1 level => (penalty == 0), above 1 level => (0 < penalty < 1)
    },

    scoreFunc: function ( levels ) {
        //console.log("scoring: " + stringifyObject(levels));

        var penalty = 0;

        for (var i = 0; i < this.edgesThatCross.length; i++) {       // only check the penalty for edges that cross other edges, it is known to be 0 ow
            var edge    = this.edgesThatCross[i];
            var crosses = this.crosses[edge];
            for (var j = 0; j < crosses.length; j++) {
                var intersects = crosses[j];
                if (intersects > edge) {                  // because we want to only count each intersection once (
                    penalty += this.pairScoreFunc( edge, intersects, levels[edge], levels[intersects] );
                    if (!isFinite(penalty))
                        return penalty;
                }
            }
        }

        // + penalty for the number of levels used: less levels is better given the same "crossing" score
        var usedLevels = filterUnique(levels).length;
        penalty += this.numberOfLevelsPenalty(usedLevels);

        //console.log("total penalty: " + penalty);

        return penalty;
    },

    computeCrosses: function () {
        // find all edges which cross, for now - using plain (and likely non-optimal) O(n^2) algo

        var minPossiblePenalty = 0;

        var edgesThatCross = [];
        var handled        = {};

        var hasToBeBelowForPerfectScore = [];
        var hasToBeAboveForPerfectScore = [];

        var crosses = [];
        for (var i = 0; i < this.numEdges; i++) {
            crosses[i] = [];
            hasToBeBelowForPerfectScore[i] = 0;
            hasToBeAboveForPerfectScore[i] = 0;
        }

        for (var i = 0; i < this.numEdges-1; i++) {
            for (var j = i+1; j < this.numEdges; j++) {
                if (this.pairScoreFunc( i, j, 1, 1 ) == Infinity) {  // only happens when edges intersect
                    crosses[i].push(j);
                    crosses[j].push(i);

                    if (!handled.hasOwnProperty(i)) {
                        edgesThatCross.push(i);
                        handled[i] = true;
                    }
                    if (!handled.hasOwnProperty(j))  {
                        edgesThatCross.push(j);
                        handled[j] = true;
                    }

                    // if they cross the best arrangement of these two edges is either one above the other or v.v.
                    // in any case there may be some penalty associated for that, but can't do beter than the better
                    // of these two options. Compute it so that later once we achieve the minimum possible penalty we stop
                    var scoreAbove = this.pairScoreFunc( i, j, 1, 2 );
                    var scoreBelow = this.pairScoreFunc( i, j, 2, 1 );

                    minPossiblePenalty += Math.min( scoreAbove, scoreBelow );

                    if (scoreAbove < scoreBelow) {
                        hasToBeAboveForPerfectScore[i] = 1;
                        hasToBeBelowForPerfectScore[j] = 1;
                    }
                    if (scoreAbove > scoreBelow) {
                        hasToBeAboveForPerfectScore[j] = 1;
                        hasToBeBelowForPerfectScore[i] = 1;
                    }
                }
            }
        }

        // it is hard to know the exact number of levels needed: even if at most 2 edges overlap at the same time,
        // may need many levels as they stack below each other. Yet we know that if for the perfect score we need
        // an edge to be below one and above the other, we need at least 3 levels. That 3-level arrangement may
        // not be valid and we may end up with only 2 levels, but then the penalty will be larger due to
        // non-pairwise-perfect crossing arrangement
        var minNumLevels = (minPossiblePenalty == 0) ? 1 : 2;
        for (var i = 0; i < this.numEdges; i++) {
            minNumLevels = Math.max( minNumLevels, 1 + hasToBeBelowForPerfectScore[i] + hasToBeAboveForPerfectScore[i] );
        }
        minPossiblePenalty += this.numberOfLevelsPenalty(minNumLevels);   // (0 <= numberOfLevelsPenalty < 1) => this affects the penalty less than a single extra crossing

        return {"crosses": crosses, "minPossiblePenalty": minPossiblePenalty, "edgesThatCross": edgesThatCross };
    },

    computeVerticalPositions: function( maxFullSearchSize, maxSteps, seed ) {
        // maxFullSearchSize - max number of edges in a cluster which can be searched fully via brute force; max time = C1 * maxFullSearchSize! per cluster
        // maxSteps          - max number of steps for a heuristic; max time = C2 * maxSteps per cluster
        //                     (where C2 > C1, and both include computing the penalty for a given arrangmenet, which is ~O(clusterSize^2)

        if (this.initScore == this.minPossiblePenalty)
            return this.initLevels;

        if ( this.edgesThatCross.length <= maxFullSearchSize )     // problem size for exhaustiveSearch == numEdges!
            return this.exhaustiveSearch();

        this.seed = seed ? seed : 1;                               // random seed for simulatedAnnellingOptimizer
        return this.simulatedAnnellingOptimizer( maxSteps );
    },

    //----------------------------------------------------------------------------------
    exhaustiveSearch: function() {
        this.checkedNumber  = 0;

        var result = this.recursiveExhaustiveSearch( this.initLevels.slice(0), 0, { "score": this.initScore, "values": this.initLevels } );

        console.log("[fsearch] Found best assignment: " + stringifyObject(result.values) + " (score: " + result.score + ")");

        console.log("[fsearch] tried " +  this.checkedNumber + " combinations");

        return result.values;
    },

    recursiveExhaustiveSearch: function ( valuesSoFar, level, bestSoFar ) {

        //console.log("best value at enter [" + level + "]: " + stringifyObject(bestSoFar.values) + " (score: " + bestSoFar.score + ")");

        // reached the end of the recursion
        if (level == this.edgesThatCross.length) {
            //console.log("trying complete: " + stringifyObject(valuesSoFar));
            var score = this.scoreFunc(valuesSoFar);
            if (score < bestSoFar.score) {
                bestSoFar.values = valuesSoFar.slice(0);
                bestSoFar.score  = score;
                console.log("[fsearch] New best: " + stringifyObject(bestSoFar.values) + " (score: " + bestSoFar.score + ")");
            }
            this.checkedNumber++;
            return bestSoFar;
        }

        var edge = this.edgesThatCross[level];

        // TODO: since we don't want any gaps in levels, a smarter search can be used which fills the gaps
        //       once it is known that if they are not filled now the arrangmenet will be incorrect

        // TODO: exclude assignments where two intersecting edges have the same level. Only consider edges
        //       with lover IDs since those are already assigned a value. Edges with higher ids haveno assignment yet

        var minValue = 1;
        var maxValue = this.edgesThatCross.length;

        if (this.minLevels)
            minValue = this.minLevels[edge];
        if (this.minLevels)
            maxValue++;

        for (var i = minValue; i <= maxValue; i++ ) {
            valuesSoFar[edge] = i;

            bestSoFar = this.recursiveExhaustiveSearch( valuesSoFar, level+1, bestSoFar );

            if (bestSoFar.score == this.minPossiblePenalty) break;
        }
        //console.log("best value at exit [" + level + "]: " + stringifyObject(bestSoFar.values) + " (score: " + bestSoFar.score + ")");

        return bestSoFar;
    },
    //----------------------------------------------------------------------------------


    //--[ simulatedAnnelling() related ]------------------------------------------------
    makeBasicValidAssignment: function ( initLevels ) {
        // give each edge that crosses a separate level - may not be optimal, but guaranteed ot be "valid" with a penalty below Infinity
        var newAssignemnt = initLevels.slice(0);
        for (var i = 0; i < this.edgesThatCross.length; i++)
            newAssignemnt[this.edgesThatCross[i]] = (i+1);    // levels start from 1
        //console.log("Initialvalid assignment: " + stringifyObject(newAssignemnt) + ", score: " + this.scoreFunc(newAssignemnt));
        return newAssignemnt;
    },

    computeNeighbour: function ( currentState, step ) {
        // general idea: assign some other "level" to one of the edges (where "some" and "one of" can be picked randomly/depending on the `step`).
        //               If that level is forbidden due to a crossing with some other edges assign the first OK level in the direction of change
        //               + normalize after the change (e.g. remove unused levels + shift to make smallest used levle to be level #1)

        // 1. pick random edge
        // 2. pick new random level for the edge (different form the old value)
        // 3. while (value is forbidden or unchanged) increment value (if new>old) or decrement value (if new<old)

        //console.log("computeNeighbour - current: " + stringifyObject(currentState));

        var newState = currentState.slice(0);

        // pick a random edge - among those which cross some other edges (all edges not crossing any others are always assigned level 1)
        var edge = this.edgesThatCross[ Math.floor(this.random() * this.edgesThatCross.length) ];

        var maxUsedLevel = Math.max.apply(null, currentState);

        // check random value an dif it makes sense to increment or decrement it
        var oldValue   = newState[edge];
        var isBelowAll = true;
        var isAboveAll = true;
        for (var i = 0; i < this.crosses[edge].length; i++) {
            var crossesWith = this.crosses[edge][i];
            if (newState[crossesWith] > oldValue)
                isAboveAll = false;
            else
                isBelowAll = false;
        }

        // pick new random level for the edge (different form the old value)
        var newValue;
        do {
            newValue = Math.floor(this.random()*(maxUsedLevel + 2));   // value = [0...maxUsedLevel+1].
        }                                                              // note: 0 is not a valid "level" but lets the edge to be positioned above any other (and later normalized to have level 1)
        while ( newValue == oldValue || (isBelowAll && newValue < oldValue) || (isAboveAll && newValue > oldValue) );

        //console.log("new value: " + newValue);

        var increment = (newValue > oldValue) ? +1 : -1;

        do {
            var changed = false;
            for (var i = 0; i < this.crosses[edge].length; i++) {
                var crossedEdgeValue = newState[this.crosses[edge][i]];
                if (newValue == crossedEdgeValue) {    // if this edge level == level of an edge it intersects with. Such an assignment wont work anyhow
                    newValue += increment;
                    changed = true;
                }
            }
        }
        while (changed);

        newState[edge] = newValue;

        this.normalize(newState);

        //console.log("computeNeighbour - final: " + stringifyObject(newState));

        return newState;
    },

    normalize: function( levels ) {
        //console.log("pre-normalized levels: " + stringifyObject(levels));

        // 1. normalize so that the smallest used level is this.maxOfMinlevels
        var minUsedLevel = Math.min.apply(null, levels);
        if (minUsedLevel != this.maxOfMinlevels) {
            var increment = this.maxOfMinlevels - minUsedLevel;
            for (var i = 0; i < this.edgesThatCross.length; i++)
                levels[this.edgesThatCross[i]] += increment;
        }

        // 2. if there are gaps (e.g. 1,2,4,5) decrement all levels above each gap to fill the gap
        var usedLevels = filterUnique(levels).sort();
        for (var i = usedLevels.length-1; i > 0; i--) {
            if (usedLevels[i] != usedLevels[i-1] + 1) {      // there may be only one gap so no need to implement a more effficient algorithm
                for (var j = 0; j < levels.length; j++)
                    if (levels[j] >= usedLevels[i])
                        levels[j]--;
                //console.log("found gap [" + usedLevels[i-1] + " - " + usedLevels[i] + "]");
                break;
            }
        }

        // 3. normalize so that the lowest edge in each overlapping stack has rank 1
        do {
            var changed = false;
            for (var i = 0; i < this.edgesThatCross.length; i++) {
                var edge = this.edgesThatCross[i];

                var maxBelow = 0;
                for (var j = 0; j < this.crosses[edge].length; j++) {
                    var crossesWith = this.crosses[edge][j];
                    if (levels[crossesWith] < levels[edge] && levels[crossesWith] > maxBelow)
                        maxBelow = levels[crossesWith];
                }

                if (levels[edge] != maxBelow + 1) {
                    levels[edge] = maxBelow + 1;
                    changed = true;
                }
            }
        }
        while (changed);

        //console.log("normalized levels: " + stringifyObject(levels));
    },

    random: function() {
        // very crude but ok for the purpose of this algorithm in the context of vertical edge allocation
        // http://jsfiddle.net/bhrLT/3/
        var x = Math.sin(this.seed++)*16;
        return x - Math.floor(x);
    },

    doSwitchDuringAnneling: function ( oldScore, newScore, stepsLeft ) {
        if (newScore < oldScore) return true;

        if (stepsLeft <= 5) return false;

        var probability = Math.exp( -(newScore - oldScore) / Math.log(stepsLeft/2) );

        if (probability > this.random()) return true;
        return false;
    },

    simulatedAnnellingOptimizer: function ( maxSteps ) {

        console.log("[asearch] Starting simulatedAnnellingOptimizer");

        var bestState = isFinite(this.initScore) ? this.initLevels : this.makeBasicValidAssignment(this.initLevels);
        var bestScore = isFinite(this.initScore) ? this.initScore  : this.scoreFunc(bestState);

        var currentState = bestState;
        var currentScore = bestScore;

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

        var step = maxSteps;
        while (bestScore > this.minPossiblePenalty && step >= 0) {

            // reset once in the middle of the search (TODO: investigate if we need more resets or don't need resets at all)
            if (step == maxSteps/2 && currentScore > bestScore) {
                currentState = bestState;
                currentScore = bestScore;
            }

            var neighbourState = this.computeNeighbour( currentState, step );
            var neighbourScore = this.scoreFunc( neighbourState );

            if ( this.doSwitchDuringAnneling( currentScore, neighbourScore, step ) ) {
                currentState = neighbourState;
                currentScore = neighbourScore;
            }

            if (currentScore < bestScore) {
                console.log("[asearch] New best: " + stringifyObject(currentState) + ", score: " + currentScore + " (@ step = " + step + ")");
                bestState = currentState.slice(0);
                bestScore = currentScore;
            }

            step--;
        }

        return bestState;
    }
    //-------------------------------------------------[ simulatedAnnelling() related ]--
};

