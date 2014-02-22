/*
 * xinit: coordinates of _center_ of every vertex, or null
 */
XCoord = function(xinit, graph)
{        
    // local copies just for convenience & performance
    this.halfWidth = [];
    for (var i = 0; i < graph.GG.vWidth.length; i++)
        this.halfWidth[i] = Math.floor(graph.GG.vWidth[i]/2);

    this.graph = graph;
    
    if (xinit)
        this.xcoord = xinit; // coordinates of _center_ of every vertex    
    else
        this.xcoord = this.init_xcoord();
};

XCoord.prototype = {

    getSeparation: function (v1, v2) {
        if (this.graph.GG.type[v1] == TYPE.RELATIONSHIP && this.graph.GG.type[v2] == TYPE.RELATIONSHIP)
            return this.graph.horizontalTwinSeparationDist;
        
        if (this.graph.GG.type[v1] == TYPE.RELATIONSHIP || this.graph.GG.type[v2] == TYPE.RELATIONSHIP)
            return this.graph.horizontalRelSeparationDist;

        if ((this.graph.GG.type[v1] == TYPE.VIRTUALEDGE || this.graph.GG.type[v2] == TYPE.VIRTUALEDGE) &&
            (this.graph.GG.hasEdge(v1,v2) || this.graph.GG.hasEdge(v2,v1)))
            return this.graph.horizontalRelSeparationDist;

        // separation between twins: a bit less than between other people
        if ((this.graph.GG.type[v1] == TYPE.PERSON || this.graph.GG.type[v2] == TYPE.PERSON) &&
            (this.graph.GG.getTwinGroupId(v1) == this.graph.GG.getTwinGroupId(v2)) &&
            (this.graph.GG.getTwinGroupId(v1) != null) )
            return this.graph.horizontalTwinSeparationDist;

        return this.graph.horizontalPersonSeparationDist;
    },

    init_xcoord: function()
    {
        var xinit = [];
        // For each rank, the left-most node is assigned coordinate 0 (actually, since xinit[v] is
        // the coordinate of the center, not 0 but halfWidth[node]). The coordinate of the next
        // node is then assigned a value sufficient to satisfy the minimal separation from the prev
        // one, and so on. Thus, on each rank, nodes are initially packed as far left as possible.
        for (var r = 0; r < this.graph.order.order.length; r++) {

            xinit[this.graph.order.order[r][0]] = this.halfWidth[this.graph.order.order[r][0]];
            
            for (var i = 1; i < this.graph.order.order[r].length; i++) {
                var vPrev      = this.graph.order.order[r][i-1];
                var v          = this.graph.order.order[r][i];
                var separation = this.getSeparation(vPrev,v);

                xinit[v] = xinit[vPrev] + this.halfWidth[vPrev] + separation + this.halfWidth[v]; 
            }
        }
        return xinit;
    },    
    
    getLeftMostNoDisturbPosition: function(v) {
        var leftBoundary = this.halfWidth[v];

        var order = this.graph.order.vOrder[v];
        if ( order > 0 ) {
            var leftNeighbour = this.graph.order.order[this.graph.ranks[v]][order-1];

            leftBoundary += this.getRightEdge(leftNeighbour) + this.getSeparation(v, leftNeighbour);

            //console.log("leftNeighbour: " + leftNeighbour + ", rightEdge: " + this.getRightEdge(leftNeighbour) + ", separation: " + this.getSeparation(v, leftNeighbour));
        }
        else
            return -Infinity;

        return leftBoundary;
    },

    getSlackOnTheLeft: function(v) {
        return this.xcoord[v] - this.getLeftMostNoDisturbPosition(v);
    },

    getRightMostNoDisturbPosition: function(v, alsoMoveRelationship) {
        var rightBoundary = Infinity;

        var order = this.graph.order.vOrder[v];
        if ( order < this.graph.order.order[this.graph.ranks[v]].length-1 ) {
            var rightNeighbour = this.graph.order.order[this.graph.ranks[v]][order+1];
            rightBoundary = this.getLeftEdge(rightNeighbour) - this.getSeparation(v, rightNeighbour) - this.halfWidth[v];

            if (alsoMoveRelationship && this.graph.GG.type[rightNeighbour] == TYPE.RELATIONSHIP) {
                var rightMost = this.getRightMostNoDisturbPosition(rightNeighbour);
                var slack     = rightMost - this.xcoord[rightNeighbour];
                rightBoundary += slack;
            }
        }

        return rightBoundary;
    },

    getSlackOnTheRight: function(v) {
        return this.getRightMostNoDisturbPosition(v,false) - this.xcoord[v];
    },

    getLeftEdge: function(v) {
        return this.xcoord[v] - this.halfWidth[v];
    },

    getRightEdge: function(v) {
        return this.xcoord[v] + this.halfWidth[v];
    },

    shiftLeftOneVertex: function (v, amount) {
        // attempts to move vertex v to the left by ``amount``, but stops
        // as soon as it get as close as allowed to it's left neighbour

        var leftBoundary = this.getLeftMostNoDisturbPosition(v);

        var actualShift = Math.min( amount, this.xcoord[v] - leftBoundary );

        this.xcoord[v] -= actualShift;

        return actualShift;
    },
    
    shiftRightOneVertex: function (v, amount) {
        // attempts to move vertex v to the right by ``amount``, but stops
        // as soon as it get as close as allowed to it's right neighbour

        var rightBoundary = this.getRightMostNoDisturbPosition(v);

        var actualShift = Math.min( amount, rightBoundary - this.xcoord[v] );

        this.xcoord[v] += actualShift;

        return actualShift;
    },    

    shiftRightAndShiftOtherIfNecessary: function (v, amount) {
        // shifts a vertext to the right by the given ``amount``, and shifts
        // all right neighbours, the minimal amount to accomodate this shift
        this.xcoord[v] += amount;

        var rightEdge = this.getRightEdge(v);
        var rank      = this.graph.ranks[v];
        var order     = this.graph.order.vOrder[v];

        for (var i = order + 1; i < this.graph.order.order[rank].length; i++) {
            var rightNeighbour = this.graph.order.order[rank][i];

            if (this.getLeftEdge(rightNeighbour) >= rightEdge + this.getSeparation(v, rightNeighbour)) {
                // we are not interfering with the vertex to the right
                break;

            }

            this.xcoord[rightNeighbour] = rightEdge + this.getSeparation(v, rightNeighbour) + this.halfWidth[rightNeighbour];

            rightEdge = this.getRightEdge(rightNeighbour);
            v         = rightNeighbour;
        }

        return amount;
    },

    moveNodeAsCloseToXAsPossible: function (v, targetX) {
        var x = this.xcoord[v];
        if (x > targetX) {
            var leftMostOK = this.getLeftMostNoDisturbPosition(v);
            if (leftMostOK <= targetX)
                this.xcoord[v] = targetX;
            else
                this.xcoord[v] = leftMostOK;
        }
        else {
            var rightMostOK = this.getRightMostNoDisturbPosition(v);
            if (rightMostOK >= targetX)
                this.xcoord[v] = targetX;
            else
                this.xcoord[v] = rightMostOK;
        }

        if (this.xcoord[v] != x) // we've moved the mode
            return true;

        return false;
    },

    normalize: function() {
        var leftMostElement  = indexOfLastMinElementInArray(this.xcoord);
        var moveAmount       = this.xcoord[leftMostElement] - this.halfWidth[leftMostElement];

        for (var i = 0; i < this.xcoord.length; i++)
            this.xcoord[i] -= moveAmount;
    },

    copy: function () {
        // returns an instance with deep copy of this.xcoord
        var newX = new XCoord(this.xcoord.slice(0), this.graph, this.halfWidth);

        return newX;
    }
};

//==================================================================================================

XCoordScore = function(maxRealVertexId) {
    this.score           = 0;
    this.inEdgeMaxLen    = [];
    this.maxRealVertexId = maxRealVertexId;
    this.numStraightLong = 0;
};

XCoordScore.prototype = {

    add: function(amount) {
        this.score += amount;
    },

    addEdge: function(v, u, length) {
        if (u > this.maxRealVertexId) {
            if (length == 0 && v > this.maxRealVertexId)
                this.numStraightLong++;

            length /= 2;
        }

        if (! this.inEdgeMaxLen[u] || length > this.inEdgeMaxLen[u]) {
            this.inEdgeMaxLen[u] = length;
        }

        for (var i = 0; i < u; i++)
            if ( this.inEdgeMaxLen[i] === undefined )
                this.inEdgeMaxLen[i] = 0;
    },

    isBettertThan: function(otherScore) {
        if (this.score == otherScore.score) {
            if (this.numStraightLong == otherScore.numStraightLong) {
                // if score is the same the arrangements with smaller sum of
                // longest in-edges wins
                //if (this.inEdgeMaxLen.length == 0 || otherScore.inEdgeMaxLen.length == 0 ) {
                //    printObject(this);
                //    printObject(otherScore);
                //}
                var score1 = this.inEdgeMaxLen.length       == 0 ? 0 : this.inEdgeMaxLen.reduce(function(a,b){return a+b;});
                var score2 = otherScore.inEdgeMaxLen.length == 0 ? 0 : otherScore.inEdgeMaxLen.reduce(function(a,b){return a+b;});

                if (score1 == score2)
                    return (Math.max.apply(null,this.inEdgeMaxLen) < Math.max.apply(null,otherScore.inEdgeMaxLen)); // given everything else equal, prefer layout with shorter longest edge

                return (score1 < score2); // prefer layout with smaller total edge length
            }
            return (this.numStraightLong > otherScore.numStraightLong);
        }
        return (this.score < otherScore.score);
    }
};

//==================================================================================================

VerticalLevels = function() {

    this.rankVerticalLevels   = [];   // for each rank: how many "levels" of horizontal edges are between this and next ranks
    this.childEdgeLevel       = [];   // for each "childhub" node contains the verticalLevelID to use for the child edges
                                      // (where levelID is for levels between this and next ranks)
    this.outEdgeVerticalLevel = [];   // for each "person" node contains outgoing relationship edge level as {target1: {attachLevel: level, lineLevel: level}, target2: ... }
                                      // (where levelID is for levels between this and previous ranks)
};

VerticalLevels.prototype = {
    copy: function() {
        var result = new VerticalLevels();

        result.rankVerticalLevels   = this.rankVerticalLevels.slice(0);
        result.childEdgeLevel       = this.childEdgeLevel.slice(0);
        result.outEdgeVerticalLevel = this.outEdgeVerticalLevel.slice(0);
        return result;
    }
};

