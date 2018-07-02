define([], function(){

    var XCoordScore = function(maxRealVertexId) {
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
                    //    Helpers.printObject(this);
                    //    Helpers.printObject(otherScore);
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

    return XCoordScore;
});
