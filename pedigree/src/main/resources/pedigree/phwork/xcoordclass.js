XCoord = function() {
    this.xcoord = [] ; // coordinates of _center_ of every vertex

    // local copies just for convenience & performance
    this.halfWidth = [];

    this.horizPersSeparationDist = undefined;
    this.horizRelSeparationDist  = undefined;

    this.order = undefined;
    this.ranks = undefined;
    this.type  = undefined;
};

XCoord.prototype = {

    init: function (xinit, horizontalPersonSeparationDist, horizontalRelSeparationDist, widths, order, ranks, type) {
        this.xcoord = xinit;

        this.horizPersSeparationDist = horizontalPersonSeparationDist;
        this.horizRelSeparationDist  = horizontalRelSeparationDist;

        for (var i = 0; i < widths.length; i++)
            this.halfWidth[i] = Math.floor(widths[i]/2);

        this.order = order;

        this.ranks = ranks;

        this.type  = type;
    },

    getSeparation: function (v1, v2) {
        if (this.type[v1] == TYPE.RELATIONSHIP || this.type[v2] == TYPE.RELATIONSHIP)
            return this.horizRelSeparationDist;

        return this.horizPersSeparationDist;
    },

    getLeftMostNoDisturbPosition: function(v, allowNegative) {
        var leftBoundary = this.halfWidth[v];

        var order = this.order.vOrder[v];
        if ( order > 0 ) {
            var leftNeighbour = this.order.order[this.ranks[v]][order-1];

            leftBoundary += this.getRightEdge(leftNeighbour) + this.getSeparation(v, leftNeighbour);
        }
        else if (allowNegative)
            return -Infinity;

        return leftBoundary;
    },

    getRightMostNoDisturbPosition: function(v) {
        var rightBoundary = Infinity;

        var order = this.order.vOrder[v];
        if ( order < this.order.order[this.ranks[v]].length-1 ) {
            var rightNeighbour = this.order.order[this.ranks[v]][order+1];
            rightBoundary = this.getLeftEdge(rightNeighbour) - this.getSeparation(v, rightNeighbour) - this.halfWidth[v];
        }

        return rightBoundary;
    },

    getLeftEdge: function(v) {
        return this.xcoord[v] - this.halfWidth[v];
    },

    getRightEdge: function(v) {
        return this.xcoord[v] + this.halfWidth[v];
    },

    shiftLeftOneVertex: function (v, amount) {
        // attempts to move vertex v to the left by ``amount``, but stops
        // as soon as it hits it's left neighbour

        var leftBoundary = this.getLeftMostNoDisturbPosition(v);

        var actualShift = Math.min( amount, this.xcoord[v] - leftBoundary );

        this.xcoord[v] -= actualShift;

        return actualShift;
    },

    shiftRightAndShiftOtherIfNecessary: function (v, amount) {
        // shifts a vertext to the right by the given ``amount``, and shifts
        // all right neighbours, the minimal amount to accomodate this shift
        this.xcoord[v] += amount;

        var rightEdge = this.getRightEdge(v);
        var rank      = this.ranks[v];
        var order     = this.order.vOrder[v];

        for (var i = order + 1; i < this.order.order[rank].length; i++) {
            var rightNeighbour = this.order.order[rank][i];

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

    moveNodeAsCloseToXAsPossible: function (v, targetX, allowNegative) {
        var x = this.xcoord[v];
        if (x > targetX) {
            var leftMostOK = this.getLeftMostNoDisturbPosition(v, allowNegative);
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
        // finds the smallest margin on the left and shifts the entire graph to the left
        var minExtra = this.xcoord[0] - this.halfWidth[0];
        for (var i = 1; i < this.xcoord.length; i++) {
            if ((this.xcoord[i] - this.halfWidth[i]) < minExtra)
                minExtra = (this.xcoord[i] - this.halfWidth[i]);
        }

        for (var i = 0; i < this.xcoord.length; i++)
            this.xcoord[i] -= minExtra;
    },

    copy: function () {
        // returns a deep copy
        var newX = new XCoord();

        newX.xcoord = this.xcoord.slice(0);

        newX.halfWidth               = this.halfWidth;
        newX.horizPersSeparationDist = this.horizPersSeparationDist;
        newX.horizRelSeparationDist  = this.horizRelSeparationDist;
        newX.order                   = this.order;
        newX.ranks                   = this.ranks;
        newX.type                    = this.type;

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
    this.outEdgeVerticalLevel = [];   // for each "person" node contains outgoing relationship edge level as {target1: level1, t2: l2}
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

