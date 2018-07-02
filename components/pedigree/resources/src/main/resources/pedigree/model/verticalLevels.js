define([], function(){

    var VerticalLevels = function() {

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

    return VerticalLevels;
});
