Ordering = function (order, vOrder) {
    this.order  = order;        // 1D array of 1D arrays - for each rank list of vertices in order
    this.vOrder = vOrder;       // 1D array - for each v vOrder[v] = order within rank

    // TODO: verify validity?
};

Ordering.prototype = {

    serialize: function() {
        return this.order;
    },

    deserialize: function(data) {
        this.order  = data;
        this.vOrder = [];
        //console.log("Order deserialization: [" + stringifyObject(this.order) + "]");

        // recompute vOrders
        for (var r = 0; r < this.order.length; r++) {
            var ordersAtRank = this.order[r];
            for (var i = 0; i < ordersAtRank.length; i++) {
                this.vOrder[ordersAtRank[i]] = i;
            }
        }
    },

    insert: function(rank, insertOrder, vertex) {
       this.order[rank].splice(insertOrder, 0, vertex);
       this.vOrder[vertex] = insertOrder;
       for (var next = insertOrder+1; next < this.order[rank].length; ++next)
           this.vOrder[ this.order[rank][next] ]++;
    },

    exchange: function(rank, index1, index2) {
        // exchanges vertices at two given indices within the same given rank

        var v1 = this.order[rank][index1];
        var v2 = this.order[rank][index2];

        this.order[rank][index2] = v1;
        this.order[rank][index1] = v2;

        this.vOrder[v1] = index2;
        this.vOrder[v2] = index1;
    },

    canMove: function(rank, index, amount) {
        var newIndex = index + amount;
        if (newIndex < 0) return false;
        if (newIndex > this.order[rank].length - 1) return false;
        return true;
    },

    move: function(rank, index, amount) {
        // changes vertex order within the same rank. Moves "amount" positions to the right or to the left
        if (amount == 0) return true;

        var newIndex = index + amount;
        if (newIndex < 0) return false;

        var ord = this.order[rank];
        if (newIndex > ord.length - 1) return false;

        var v = ord[index];

        if (newIndex > index) {
            for (var i = index; i< newIndex; ++i) {
                var vv          = ord[i+1];
                ord[i]          = vv;
                this.vOrder[vv] = i;
            }
        }
        else {
            for (var i = index; i > newIndex; --i) {
                var vv          = ord[i-1];
                ord[i]          = vv;
                this.vOrder[vv] = i;
            }
        }

        ord[newIndex]  = v;
        this.vOrder[v] = newIndex;

        return true;
    },

    copy: function () {
        // returns a deep copy
        return new Ordering(clone2DArray(this.order), this.vOrder.slice());
    },

    moveVertexToRankAndOrder: function ( oldRank, oldOrder, newRank, newOrder ) {
        // changes vertex rank and order. Insertion happens right before the node currently occupying the newOrder position on rank newRank
        var v = this.order[oldRank][oldOrder];

        this.order[oldRank].splice(oldOrder, 1);

        this.order[newRank].splice(newOrder, 0, v);

        this.vOrder[v] = newOrder;
        for ( var i = newOrder+1; i < this.order[newRank].length; ++i ) {
            var nextV = this.order[newRank][i];
            this.vOrder[nextV]++;
        }
        for ( var i = oldOrder; i < this.order[oldRank].length; ++i ) {
            var nextV = this.order[oldRank][i];
            this.vOrder[nextV]--;
        }
    },

    moveVertexToOrder: function ( rank, oldOrder, newOrder ) {
        // changes vertex order within the same rank. Insertion happens right before the node currently occupying the newOrder position
        // (i.e. changing order form 3 to 4 does nothing, as before position 4 is still position 3)
        var shiftAmount = (newOrder <= oldOrder) ? (newOrder - oldOrder) : (newOrder - oldOrder - 1);
        this.move( rank, oldOrder, shiftAmount );
    },

    removeUnplugged: function() {
        var result = this.order[0].slice(0); //copy of original unplugged IDs

        for (var u = 0; u < this.order[0].length; ++u) {
            var unplugged = this.order[0][u];

            for (var i = 0; i < this.order.length; ++i)
                for (var j = 0; j < this.order[i].length; ++j ) {
                    if (this.order[i][j] > unplugged)
                        this.order[i][j]--;
                }

                this.vOrder.splice(unplugged, 1);
        }

        this.order[0] = [];

        return result;
    },

    remove: function(v, rank) {
        var order = this.vOrder[v];
        this.moveVertexToRankAndOrder(rank, order, 0, 0);
        this.removeUnplugged();
    },

    insertAndShiftAllIdsAboveVByOne: function ( v, rank, newOrder ) {
        // used when when a new vertex is inserted into the graph, which increases all IDs above v by one
        // so need to modify the data for all existing vertices first, and then insert the new vertex

        for (var i = this.vOrder.length; i > v; --i ) {
            this.vOrder[i] = this.vOrder[i-1];
        }

        for (var i = 0; i < this.order.length; ++i)
            for (var j = 0; j < this.order[i].length; ++j ) {
                if (this.order[i][j] >= v)
                    this.order[i][j]++;
            }

        this.insert(rank, newOrder, v);
    },

    insertRank: function (insertBeforeRank) {
        this.order.splice(insertBeforeRank, 0, []);
    },

    getRightNeighbour: function(v, rank) {
        var order = this.vOrder[v];
        if ( order < this.order[rank].length-1 )
            return this.order[rank][order+1];
        return null;
    },

    getLeftNeighbour: function(v, rank) {
        var order = this.vOrder[v];
        if ( order > 0 )
            return this.order[rank][order-1];
        return null;
    },

    sortByOrder: function(v_list) {
        var vorders = this.vOrder;
        var result = v_list.slice(0);
        result.sort(function(x, y){ return vorders[x] > vorders[y] });
        return result;
    },

    // returns all vertices ordered from left-to-right and from top-to-bottom
    getLeftToRightTopToBottomOrdering: function(onlyType, GG) {
        var result = [];
        for (var i = 1; i < this.order.length; ++i) {
            for (var j = 0; j < this.order[i].length; ++j) {
                var v = this.order[i][j];
                if (!onlyType || GG.type[v] == onlyType)
                    result.push(this.order[i][j]);
            }
        }
        return result;
    }
};

