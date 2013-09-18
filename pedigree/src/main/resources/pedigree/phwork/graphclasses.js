//-------------------------------------------------------------
var TYPE = {
  RELATIONSHIP: 1,
  CHILDHUB:     2,
  PERSON:       3,
  VIRTUALEDGE:  4
};

InternalGraph = function(defaultPersonNodeWidth, defaultNonPersonNodeWidth)
{
    this.v        = [];        // for each V lists (as unordered arrays of ids) vertices connected from V
    this.inedges  = [];        // for each V lists (as unordered arrays of ids) vertices connecting to V

    this.weights  = [];        // for each V contains outgoing edge weights as {target1: weight1, t2: w2}

    this.maxRealVertexId = -1; // used for quick separation of real vertices from virtual-multi-rank-edge-breaking ones

    this.idToName = [];
    this.nameToId = {};

    this.parentlessNodes = [];
    this.leafNodes       = [];

    this.vWidth = [];
    this.defaultPersonNodeWidth    = defaultPersonNodeWidth    ? defaultPersonNodeWidth    : 1;
    this.defaultNonPersonNodeWidth = defaultNonPersonNodeWidth ? defaultNonPersonNodeWidth : 1;

    this.type       = [];      // for each V node type (see TYPE)
    this.properties = [];      // for each V a set of type-specific properties {"gender": "M"/"F"/"U", etc.}
};


InternalGraph.init_from_user_graph = function(inputG, defaultPersonNodeWidth, defaultNonPersonNodeWidth, checkIDs)
{
    // note: serialize() produces the correct input for this function

    var newG = new InternalGraph();

    if (defaultNonPersonNodeWidth) newG.defaultNonPersonNodeWidth = defaultNonPersonNodeWidth;
    if (defaultPersonNodeWidth)    newG.defaultPersonNodeWidth    = defaultPersonNodeWidth;

    var relationshipHasExplicitChHub = {};

    // first pass: add all vertices and assign vertex IDs
    for (var v = 0; v < inputG.length; v++) {

        var type = TYPE.PERSON;
        if ( inputG[v].hasOwnProperty('relationship') || inputG[v].hasOwnProperty('rel') ) {
            type = TYPE.RELATIONSHIP;
            // normally users wont specify childhubs explicitly - but save via JSON does
            if (inputG[v].hasOwnProperty('hub') || inputG[v].hasOwnProperty('haschhub'))
                relationshipHasExplicitChHub[v] = true;
        }
        else if ( inputG[v].hasOwnProperty('chhub') ) {
            type = TYPE.CHILDHUB;
        }
        else if ( inputG[v].hasOwnProperty('virtual') || inputG[v].hasOwnProperty('virt')) {
            type = TYPE.VIRTUALEDGE;
        }

        var properties = {};
        if (inputG[v].hasOwnProperty('properties') || inputG[v].hasOwnProperty('prop'))
            properties = inputG[v].hasOwnProperty('properties') ? inputG[v]["properties"] : inputG[v]["prop"];

        if ( type == TYPE.PERSON ) {
            if (!properties.hasOwnProperty("gender"))
                properties["gender"] = "U";

            if (inputG[v].hasOwnProperty("gender")) {
                 var genderString = inputG[v]["gender"].toLowerCase();
                 if( genderString == "female" || genderString == "fem" || genderString == "f")
                    properties["gender"] = "F";
                else if( genderString == "male" || genderString == "m")
                    properties["gender"] = "U";
            }
        }

        var width = inputG[v].hasOwnProperty('width') ?
                    inputG[v].width :
                    (type == TYPE.PERSON ? defaultPersonNodeWidth : defaultNonPersonNodeWidth);

        var newID = newG._addVertex( inputG[v].name, null, type, properties, width );   // "null" since id is not known yet

        // should only be used for save/restore: to verify the new IDs match the old ones
        if (checkIDs) {
            if (!inputG[v].hasOwnProperty('id'))
                throw "Can't check IDs - no IDs in input data!";
            var expectID = inputG[v]['id'];
            if (expectID != newID)
                throw "Assertion failed: restored node ID (" + newID.toString() + ") does not match real node ID (" + expectID.toString() + ")!";
        }

        // when entered by user manually allow users to skip childhub nodes (and create them automatically)
        // (but when saving/restoring from a JSON need to save/restore childhub nodes as they
        //  may have some properties assigned by the user which we need to save/restore)
        if ( type == TYPE.RELATIONSHIP && !relationshipHasExplicitChHub.hasOwnProperty(v) )
            newG._addVertex( "chhub_" + inputG[v].name, null, TYPE.CHILDHUB, null, width );
    }

    // second pass (once all vertex IDs are known): process edges
    for (var v = 0; v < inputG.length; v++) {
        var nextV = inputG[v];

        var vID    = newG.getVertexIdByName( nextV.name );
        var origID = vID;

        var substitutedID = false;

        if (newG.type[vID] == TYPE.RELATIONSHIP && !relationshipHasExplicitChHub.hasOwnProperty(vID)) {
            // replace edges from rel node by edges from childhub node
            var childhubID = newG.getVertexIdByName( "chhub_" + nextV.name );
            vID = childhubID;
            substitutedID = true;
        }

        var maxChildEdgeWeight = 0;

        if (nextV.outedges) {
            for (var outE = 0; outE < nextV.outedges.length; outE++) {
                var targetName = nextV.outedges[outE].to;
                var targetID   = newG.getVertexIdByName( targetName );

                var weight = 1;
                if (nextV.outedges[outE].hasOwnProperty('weight'))
                    weight = nextV.outedges[outE].weight;
                if ( weight > maxChildEdgeWeight )
                    maxChildEdgeWeight = weight;

                newG._addEdge( vID, targetID, weight );
            }
        }

        if (substitutedID) {
            newG._addEdge( origID, vID, maxChildEdgeWeight );
        }
    }

    // find all vertices without an in-edge and vertices without out-edges
    newG._updateLeafAndParentlessNodes();

    newG.validate();

    return newG;
}


InternalGraph.prototype = {

    serialize: function(saveWidth) {
        var output = [];

        for (var v = 0; v < this.v.length; v++) {
            var data = {};
            data["name"] = this.idToName[v];
            data["id"]   = v;

            if (saveWidth) // may not want this for compactness of output when all width are equal to default
                data["width"] = this.vWidth[v];

            if (this.type[v] == TYPE.PERSON) {
                //
            }
            else if (this.type[v] == TYPE.RELATIONSHIP) {
                data["rel"] = true;
                data["hub"] = true;
            }
            else if (this.type[v] == TYPE.CHILDHUB) {
                data["chhub"] = true;
            }
            else
                data["virt"] = true;

            data["prop"] = this.properties[v];

            out = [];
            var outEdges = this.getOutEdges(v);
            for (var i = 0; i < outEdges.length; i++)
                out.push({"to": this.idToName[outEdges[i]]});

            if (out.length > 0)
                data["outedges"] = out;

            output.push(data);
        }

        return output;
    },

    //-[construction for ordering]--------------------------

    // After rank assignment, edges between nodes more than one rank apart are replaced by
    // chains of unit length edges between temporary or ‘‘virtual’’ nodes. The virtual nodes are
    // placed on the intermediate ranks, converting the original graph into one whose edges connect
    // only nodes on adjacent ranks
    //
    // Note: ranks is modified to contain ranks of virtual nodes as well

    makeGWithSplitMultiRankEdges: function (ranks, maxRank, doNotValidate, addOneMoreOnTheSameRank) {
        var newG = new InternalGraph();

        newG.defaultNonPersonNodeWidth = this.defaultNonPersonNodeWidth;
        newG.defaultPersonNodeWidth    = this.defaultPersonNodeWidth;

        // add all original vertices
        for (var i = 0; i < this.idToName.length; i++) {
            newG._addVertex( this.idToName[i], i, this.type[i], this.properties[i], this.vWidth[i] );
        }

        // go over all original edges:
        // - if edge conects vertices with adjacent ranks just add it
        // - else create a series of virtual vertices and edges and add them together
        for (var sourceV = 0; sourceV < this.v.length; sourceV++) {

            var sourceRank = ranks[sourceV];

            for (var i = 0; i < this.v[sourceV].length; i++) {
                var targetV = this.v[sourceV][i];

                var weight = this.getEdgeWeight(sourceV, targetV);

                var targetRank = ranks[targetV];

                if (targetRank < sourceRank)
                    throw "Assertion failed: only forward edges";

                if (targetRank == sourceRank + 1 || targetRank == sourceRank ) {
                    newG._addEdge( sourceV, targetV, weight );
                }
                else {
                    var sourceName = this.getVertexNameById(sourceV);
                    var targetName = this.getVertexNameById(targetV);

                    // create virtual vertices & edges
                    var prevV = sourceV;
                    var lastVirtualRank = addOneMoreOnTheSameRank ? targetRank : targetRank - 1;

                    for (var midRank = sourceRank+1; midRank <= lastVirtualRank; midRank++) {
                        var nextV = newG._addVertex( sourceName + '->' + targetName + '_' + (midRank-sourceRank-1), null, TYPE.VIRTUALEDGE, null, this.defaultNonPersonNodeWidth);
                        ranks[nextV] = midRank;
                        newG._addEdge( prevV, nextV, weight );
                        prevV = nextV;
                    }
                    newG._addEdge(prevV, targetV, weight);
                }
            }
        }

        newG.parentlessNodes = this.parentlessNodes;
        newG.leafNodes       = this.leafNodes;

        if (!doNotValidate)
            newG.validate();

        return newG;
    },

    makeGWithCollapsedMultiRankEdges: function () {
        // performs the opposite of what makeGWithSplitMultiRankEdges() does
        var newG = new InternalGraph();

        newG.defaultNonPersonNodeWidth = this.defaultNonPersonNodeWidth;
        newG.defaultPersonNodeWidth    = this.defaultPersonNodeWidth;

        // add all original vertices
        for (var i = 0; i <= this.maxRealVertexId; i++) {
            newG._addVertex( this.idToName[i], i, this.type[i], this.properties[i], this.vWidth[i] );
        }

        // go over all original edges:
        // - if edge conects two non-virtual vertices just add it
        // - else add an edge to the first non-virtual edge at the end of the chain of virtual edges
        for (var sourceV = 0; sourceV <= this.maxRealVertexId; sourceV++) {

            for (var i = 0; i < this.v[sourceV].length; i++) {
                var targetV = this.v[sourceV][i];

                var weight = this.getEdgeWeight(sourceV, targetV);

                while (targetV > this.maxRealVertexId)
                    targetV = this.getOutEdges(targetV)[0];

                newG._addEdge( sourceV, targetV, weight );
            }
        }

        newG.parentlessNodes = this.parentlessNodes;
        newG.leafNodes       = this.leafNodes;

        newG.validate();

        return newG;
    },

    //--------------------------[construction for ordering]-

    _updateLeafAndParentlessNodes: function () {
        this.parentlessNodes = [];
        this.leafNodes       = [];

        // find all vertices without an in-edge
        for (var vid = 0; vid <= this.maxRealVertexId; vid++) {
            if ( this.getInEdges(vid).length == 0 ) {
                this.parentlessNodes.push(vid);
            }
            else
            if ( this.getOutEdges(vid).length == 0 ) {
                this.leafNodes.push(vid);
            }
        }
    },

    // id: optional. If not specified then next available is used.
    // note: unlike insertVetex() does not do any id shifting, i.e. asserts that v[id] is ok to overwrite, if present
    _addVertex: function(name, id, type, properties, width) {
        if (name != "" && this.nameToId.hasOwnProperty(name)) throw "addVertex: [" + name + "] is already in G";
        if (id && this.idToName[id]) throw "addVertex: vertex with id=" + id + " is already in G";

        var nextId = (id == null) ? this.v.length : id;

        this.v[nextId] = [];

        this.inedges[nextId] = [];

        this.weights[nextId] = {};

        this.idToName[nextId] = name;

        if (name != "")
            this.nameToId[name] = nextId;

        this.vWidth[nextId] = width;

        this.type[nextId] = type;

        this.properties[nextId] = properties;

        if (type != TYPE.VIRTUALEDGE && nextId > this.maxRealVertexId)
            this.maxRealVertexId = nextId;

        return nextId;
    },

    _addEdge: function(fromV, toV, weight) {
        // adds an edge, but does not update all the internal structures for performance reasons.
        // shoudl be used for bulk updates where it makes sense to do one maintenance run for all the nodes
        if (this.v.length < Math.max(fromV, toV))
            throw "addEdge: vertex ID=" + Math.max(fromV, toV) + "] is not in G";

        if (this.hasEdge(fromV,toV))
            throw "addEdge: edge from ID="+fromV+" to ID="+toV+" already exists";
            // [maybe] add weights if the same edge is present more than once?

        this.v[fromV].push(toV);
        this.inedges[toV].push(fromV);
        this.weights[fromV][toV] = weight;
    },

    addEdge: function(fromV, toV, weight) {
            // same as _addEdge, but also updates leaf/parentless nodes
            this._addEdge(fromV, toV, weight);
            this._updateLeafAndParentlessNodes();
    },

    insertVertex: function(vname, type, properties, edgeWeights, inedges, outedges) {
        if (vname != "" && this.nameToId.hasOwnProperty(vname)) throw "insertVertex: [" + vname + "] is already in G";

        var width = (type == TYPE.PERSON) ? this.defaultPersonNodeWidth : this.defaultNonPersonNodeWidth;

        if (type == TYPE.PERSON && !properties.hasOwnProperty("gender"))
            properties["gender"] = "U";

        var newNodeId = this.maxRealVertexId + 1;    // all real node IDs should be <= maxRealVertexId, so have to insert new node here

        // shift all IDs greater or equal to newNodeId up by one (can only hapen when virtual nodes are present)
        if (this.v.length >= newNodeId) {
            // as all existing IDs >= v are increased by one, and all references should be updated
            var test         = function(u) { return (u >= newNodeId); }
            var modification = function(u) { return u + 1; }
            this._updateAllReferencesToNewIDs( test, modification );
        }

        this.v         .splice(newNodeId,0,[]);
        this.inedges   .splice(newNodeId,0,[]);
        this.weights   .splice(newNodeId,0,{});
        this.idToName  .splice(newNodeId,0,vname);
        this.vWidth    .splice(newNodeId,0,width);
        this.type      .splice(newNodeId,0,type);
        this.properties.splice(newNodeId,0,properties);
        if (vname != "") this.nameToId[vname] = newNodeId;
        this.maxRealVertexId++;

        // add new edges
        for (var i = 0; i < inedges.length; i++)
            this._addEdge( inedges[i], newNodeId, edgeWeights);
        for (var i = 0; i < outedges.length; i++)
            this._addEdge( newNodeId, outedges[i], edgeWeights);

        this._updateLeafAndParentlessNodes();

        return newNodeId;
    },

    unplugVirtualVertex: function(v) {
        // disconnectes virtual node from parent/child so that it is easy to recycle/remove later
        if (v <= this.getMaxRealVertexId())
            throw "Attempting to unplug a non-virtual vertex";

        // virtiual nodes guaranteed to have only one in and one out edge
        var parent = this.inedges[v][0];
        var child  = this.v[v][0];

        // replace outgoing edge for parent from V to child
        replaceInArray(this.v[parent], v, child);
        this.weights[parent][child] = this.weights[parent][v];
        delete this.weights[parent][v];

        // replace incoming edge for child from V to parent
        replaceInArray(this.inedges[child], v, parent);

        this.v[v] = [];
        this.inedges[v] = [];
        this.weights[v] = {};
    },

    remove: function(v) {
        for (var i = 0; i < this.v[v].length; i++) {
            var target = this.v[v][i];
            removeFirstOccurrenceByValue(this.inedges[target], v);
        }
        for (var i = 0; i < this.inedges[v].length; i++) {
            var incoming = this.inedges[v][i];
            removeFirstOccurrenceByValue(this.v[incoming], v);
            delete this.weights[incoming][v];
        }

        var vname = this.idToName[v];
        if (vname != "")
            delete this.nameToId[vname];

        //console.log("V before: " + stringifyObject(this.v));
        this.v.splice(v,1);
        //console.log("V after: " + stringifyObject(this.v));
        this.inedges.splice(v,1);
        this.weights.splice(v,1);
        this.idToName.splice(v,1);
        this.vWidth.splice(v,1);
        this.type.splice(v,1);
        this.properties.splice(v,1);
        if (v <= this.maxRealVertexId)
            this.maxRealVertexId--;

        // as all IDs above v are decreased by one, and all references should be updated
        var test         = function(u) { return (u > v); }
        var modification = function(u) { return u - 1; }
        this._updateAllReferencesToNewIDs( test, modification );

        this._updateLeafAndParentlessNodes();
    },

    _updateAllReferencesToNewIDs: function ( test, modification ) {
        // updates all references (e.g. out- and in- edge targets, nameToId, etc.) pointing to
        // ids passing the test() according to modification()
        // decrease all IDs above v by one in all the arrays (v, inedges, weights)

        for (var i = 0; i < this.v.length; i++) {
            for (var j = 0; j < this.v[i].length; j++)
                if ( test(this.v[i][j]) )
                    this.v[i][j] = modification(this.v[i][j]);
            for (var j = 0; j < this.inedges[i].length; j++)
                if ( test(this.inedges[i][j]) )
                    this.inedges[i][j] = modification(this.inedges[i][j]);

            var newWeights = {};
            var weights = this.weights[i];
            for (u in weights) {
                if (weights.hasOwnProperty(u))
                    if (test(u))
                        newWeights[modification(u)] = weights[u];
                    else
                        newWeights[u] = weights[u];
            }
            this.weights[i] = newWeights;
        }

        for (n in this.nameToId) {
            if (this.nameToId.hasOwnProperty(n)) {
                var id = this.nameToId[n];
                if (test(id))
                    this.nameToId[n] = modification(id);
            }
        }
    },

    validate: function() {
        if( this.v.length == 0 ) return;

        for (var v = 0; v < this.v.length; v++) {
            var outEdges = this.getOutEdges(v);
            var inEdges  = this.getInEdges(v);

            if (this.isPerson(v)) {
                if (inEdges.length > 1)
                    throw "Assertion failed: person nodes can't have two in-edges as people are produced by a single pregnancy (failed for " + this.getVertexNameById(v) + ")";
                for (var i = 0; i < outEdges.length; i++)
                    if (!this.isRelationship(outEdges[i]) && !this.isVirtual(outEdges[i]) )
                        throw "Assertion failed: person nodes have only out edges to relationships (failed for " + this.getVertexNameById(v) + ")";
            }
            else if (this.isRelationship(v)) {
                // TODO: for childless relations this is not true!
                if (outEdges.length == 0)
                    throw "Assertion failed: all relationships should have a childhub associated with them (failed for " + this.getVertexNameById(v) + ")";
                if (outEdges.length > 1)
                    throw "Assertion failed: all relationships should have only one outedge (to a childhub) (failed for " + this.getVertexNameById(v) + ")";
                if (!this.isChildhub(outEdges[0]))
                    throw "Assertion failed: relationships should only have out edges to childhubs (failed for " + this.getVertexNameById(v) + ")";
                if (inEdges.length != 2)
                    throw "Assertion failed: relationships should always have exactly two associated persons (failed for " + this.getVertexNameById(v) + ")";
            }
            else if (this.isVirtual(v)) {
                if (outEdges.length != 1)
                    throw "Assertion failed: all virtual nodes have exactly one out edge (to a virtual node or a relationship)";
                if (inEdges.length != 1)
                    throw "Assertion failed: all virtual nodes have exactly one in edge (from a person or a virtual node)";
                if (!this.isRelationship(outEdges[0]) && !this.isVirtual(outEdges[0]) )
                    throw "Assertion failed: all virtual nodes may only have an outedge to a virtual node or a relationship";
            }
            else if (this.isChildhub(v)) {
                if (outEdges.length < 1)
                    throw "Assertion failed: all childhubs should have at least one child associated with them";  // if not, re-ranking relationship nodes breaks
                for (var i = 0; i < outEdges.length; i++)
                    if (!this.isPerson(outEdges[i]))
                        throw "Assertion failed: childhubs are only connected to people (failed for " + this.getVertexNameById(v) + ")";
            }
        }

        // check for cycles - supposedly pedigrees can't have any
        if (this.parentlessNodes.length == 0)
            throw "Assertion failed: pedigrees should have no cycles (no parentless nodes found)";

        for (var j = 0; j < this.parentlessNodes.length; j++) {
            if ( this._DFSFindCycles( this.parentlessNodes[j], {} ) )
                throw "Assertion failed: pedigrees should have no cycles";
        }

        // check for disconnected components
        var reachable = {};
        this._markAllReachableComponents( this.parentlessNodes[0], reachable );
        for (var v = 0; v < this.v.length; v++) {
            if (!reachable.hasOwnProperty(v))
                throw "Assertion failed: disconnected component detected (" + this.getVertexNameById(v) + ")";
        }

    },

    _DFSFindCycles: function( vertex, visited ) {
        visited[vertex] = true;

        var outEdges = this.getOutEdges(vertex);

        for (var i = 0; i < outEdges.length; i++) {
            var v = outEdges[i];

            if ( visited.hasOwnProperty(v) ) {
                return true;
            }
            else if (this._DFSFindCycles( v, visited )) {
                return true;
            }
        }

        delete visited[vertex];
        return false;
    },

    _markAllReachableComponents: function( vertex, reachable ) {
        reachable[vertex] = true;

        var outEdges = this.getOutEdges(vertex);
        for (var i = 0; i < outEdges.length; i++) {
            var v = outEdges[i];
            if ( !reachable.hasOwnProperty(v) )
                this._markAllReachableComponents( v, reachable );
        }

        var inEdges = this.getInEdges(vertex);
        for (var j = 0; j < inEdges.length; j++) {
            var v = inEdges[j];
            if ( !reachable.hasOwnProperty(v) )
                this._markAllReachableComponents( v, reachable );
        }
    },

    getVertexIdByName: function(name) {
        if (!this.nameToId.hasOwnProperty(name))
            throw "getVertexIdByName: No such vertex [" + name + "]";
        return this.nameToId[name];
    },

    getVertexNameById: function(v) {
        if (!this.idToName.hasOwnProperty(v))
            throw "getVertexNameById: No such vertex [" + v + "]";
        return this.idToName[v];
    },

    getVertexWidth: function(v) {
        return this.vWidth[v];
    },

    getVertexHalfWidth: function(v) {
        return Math.floor(this.vWidth[v]/2);
    },

    getEdgeWeight: function(fromV, toV) {
        return this.weights[fromV][toV];
    },

    hasEdge: function(fromV, toV) {
        return this.weights[fromV].hasOwnProperty(toV);
    },

    getNumVertices: function() {
        return this.v.length;
    },

    getMaxRealVertexId: function() {
        return this.maxRealVertexId; // vertices with IDs less or equal to this are guaranteed to be "real"
    },

    getOutEdges: function(v) {
        return this.v[v];
    },

    getNumOutEdges: function(v) {
        return this.v[v].length;
    },

    getInEdges: function(v) {
        return this.inedges[v];
    },

    getAllEdgesWithWeights: function(v) {
        var edgeToWeight = {};

        var outEdges = this.getOutEdges(v);
        for (var i = 0; i < outEdges.length; i++) {
            var u = outEdges[i];
            edgeToWeight[u] = {"weight": this.weights[v][u], "out": true };
        }

        var inEdges = this.getInEdges(v);
        for (var i = 0; i < inEdges.length; i++) {
            var u = inEdges[i];
            edgeToWeight[u] = {"weight": this.weights[u][v], "out": false };
        }

        return edgeToWeight;
    },

	isRelationship: function(v) {
        // TODO
	    return (this.type[v] == TYPE.RELATIONSHIP);
	},

	isChildhub: function(v) {
        // TODO
	    return (this.type[v] == TYPE.CHILDHUB);
	},

	isPerson: function(v) {
	    return (this.type[v] == TYPE.PERSON);
	},

	isVirtual: function(v) {
        return (v > this.getMaxRealVertexId());
	},

	getRelationshipChildhub: function(v) {
	    if (!this.isRelationship(v))
	        throw "Assertion failed: applying getRelationshipChildhub() to a non-relationship node";

	    return this.v[v][0];
	},

	getAllPartners: function(v) {
	    if (!this.isPerson(v))
	        throw "Assertion failed: attempting to get partners of a non-person";

        var relationships = this.v[v];

        var result = [];
        for (var r = 0; r < relationships.length; r++) {
            var partners = this.inedges[relationships[r]];
            if (partners[0] != v)
                result.push(partners[0]);
            else
                result.push(partners[1]);
        }
        return result;
	},

	getParents: function(v) {
	    if (!this.isPerson(v))
	        throw "Assertion failed: attempting to get parents of a non-person";

	    if (this.inedges[v].length == 0)
	        return [];

	    // skips through relationship and child nodes and returns an array of two real parents. If none found returns []

	    var parentRelationship = this.getProducingRelationship(v);

	    var inEdges = this.getInEdges(parentRelationship);

	    if (inEdges.length != 2)
	        throw "Assertion failed: exactly two parents";

	    return [this.upTheChainUntilPerson(inEdges[0]), this.upTheChainUntilPerson(inEdges[1])];
	},

	getProducingRelationship: function(v) {
	    if (!this.isPerson(v))
	        throw "Assertion failed: attempting to get producing relationship of a non-person";

	    // find the relationship which produces this node (or null if not present)
	    if (this.inedges[v].length == 0) return null;
	    return this.inedges[this.inedges[v][0]][0];
	},

	upTheChainUntilPerson: function(v) {
	    if (this.isPerson(v)) return v;

	    if (!this.isVirtual(v))
	        throw "Assertion failed: upTheChainUntilPerson() can be applied to a perosn or a virtual node only";

	    return this.upTheChainUntilPerson( this.inedges[v][0] );  // virtual nodes have only one in-edges, all the way up until a person node
	},
};


//==================================================================================================

RankedSpanningTree = function() {
    this.maxRank = 0;

    this.edges  = [];         // similar to G.v - list of list of edges: [ [...], [...] ]
                              // but each edge is listed twice: both for in- and out-vertex

    this.rank   = [];         // list of ranks, index == vertexID
    this.parent = [];         // list of parents, index == vertexID
};

RankedSpanningTree.prototype = {

    initTreeByInEdgeScanning: function(G, initRank) {
        //   [precondition] graph must be acyclic.
        //
        //   Nodes are placed in the queue when they have no unscanned in-edges.
        //   As nodes are taken off the queue, they are assigned the least rank
        //   that satisfies their in-edges, and their out-edges are marked as scanned.

        if (G.v.length == 0) return;

        this.maxRank = initRank;

        var numScanedInEdges = [];

        for (var i = 0; i < G.getNumVertices(); i++) {
            this.rank.push(undefined);
            this.parent.push(undefined);
            this.edges.push([]);
            numScanedInEdges.push(0);
        }

        var queue = new Queue();
        for (var i = 0; i < G.parentlessNodes.length; i++ )
            queue.push( G.parentlessNodes[i] );

        while ( queue.size() > 0 ) {
            var nextParent = queue.pop();

            // ...assign the least rank satisfying nextParent's in-edges
            var inEdges = G.getInEdges(nextParent);
            var useRank = initRank;
            var parent  = undefined;
            for (var i = 0; i < inEdges.length; i++) {
                var v = inEdges[i];
                if (this.rank[v] >= useRank)
                {
                    useRank = this.rank[v] + 1;
                    parent  = v;
                }
            }

            // add edge to spanning tree
            this.rank[nextParent]   = useRank;
            if (useRank > this.maxRank)
                this.maxRank = useRank;
            this.parent[nextParent] = parent;
            if (parent != undefined)
                this.edges[parent].push(nextParent);

            // ...mark out-edges as scanned
            var outEdges = G.getOutEdges(nextParent);

            for (var u = 0; u < outEdges.length; u++) {
                var vertex = outEdges[u];

                numScanedInEdges[vertex]++;

                var numRealInEdges = G.getInEdges(vertex).length;

                if (numScanedInEdges[vertex] == numRealInEdges) {
                    queue.push(vertex);
                }
            }
        }

        // Note: so far resulting tree only uses edges in the direction they are
        //       used in the original graph. Some other algorithms in the paper
        //       (the "cut_values" part) may violate this property, if applied
        //       to this tree
    },

    getRanks: function() {
        return this.rank;
    },

    getMaxRank: function() {
        return this.maxRank;
    }
};

//==================================================================================================

Ordering = function (order, vOrder) {
    this.order  = order;        // 1D array of 1D arrays - for each rank list of vertices in order
    this.vOrder = vOrder;       // 1D array - for each v vOrder[v] = order within rank

    // TODO: verify validity
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
       for (var next = insertOrder+1; next < this.order[rank].length; next++)
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

    move: function(rank, index, amount) {
        // changes vertex order within the same rank. Moves "amount" positions to the right or to the left
        if (amount == 0) return true;

        newIndex = index + amount;
        if (newIndex < 0) return false;

        var ord = this.order[rank];
        if (newIndex > ord.length - 1) return false;

        var v = ord[index];

        if (newIndex > index) {
            for (var i = index; i< newIndex;i++) {
                var vv          = ord[i+1];
                ord[i]          = vv;
                this.vOrder[vv] = i;
            }
        }
        else {
            for (var i = index; i > newIndex; i--) {
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
        var newO = new Ordering([],[]);

        _copy2DArray(this.order, newO.order);     // copy a 2D array
        newO.vOrder = this.vOrder.slice(0);       // fast copy of 1D arrays

        return newO;
    },

    moveVertexToRankAndOrder: function ( oldRank, oldOrder, newRank, newOrder ) {
        // changes vertex rank and order. Insertion happens right before the node currently occupying the newOrder position on rank newRank
        var v = this.order[oldRank][oldOrder];

        this.order[oldRank].splice(oldOrder, 1);

        this.order[newRank].splice(newOrder, 0, v);

        this.vOrder[v] = newOrder;
        for ( var i = newOrder+1; i < this.order[newRank].length; i++ ) {
            var nextV = this.order[newRank][i];
            this.vOrder[nextV]++;
        }
        for ( var i = oldOrder; i < this.order[oldRank].length; i++ ) {
            var nextV = this.order[oldRank][i];
            this.vOrder[nextV]--;
        }
	},

    moveVertexToOrder: function ( rank, oldOrder, newOrder ) {
        // changes vertex order within the same rank. Insertion happens right before the node currently occupying the newOrder position
        // (i.e. changing order form 3 to 4 does nothing, as before position 4 is still position 3)
        var shiftAmount = newOrder - oldOrder;
        this.move( rank, oldOrder, shiftAmount );
	},

    removeUnplugged: function() {
        var result = this.order[0].slice(0); //copy of original unplugged IDs

        for (var u = 0; u < this.order[0].length; u++) {
            var unplugged = this.order[0][u];

	        for (var i = 0; i < this.order.length; i++)
	            for (var j = 0; j < this.order[i].length; j++ ) {
	                if (this.order[i][j] > unplugged)
	                    this.order[i][j]--;
	            }

	        this.vOrder.splice(unplugged, 1);
        }

        this.order[0] = [];

        return result;
    },

	insertAndShiftAllIdsAboveVByOne: function ( v, rank, newOrder ) {
	    // used when when a new vertex is inserted into the graph, which increases all IDs above v by one
	    // so need to modify the data for all existing vertices first, and then insert the new vertex

	    for (var i = this.vOrder.length; i > v; i-- ) {
	        this.vOrder[i] = this.vOrder[i-1];
	    }

	    for (var i = 0; i < this.order.length; i++)
	        for (var j = 0; j < this.order[i].length; j++ ) {
	            if (this.order[i][j] >= v)
	                this.order[i][j]++;
	        }

	    this.insert(rank, newOrder, v);
	},

	insertRank: function (insertBeforeRank) {
	    this.order.splice(insertBeforeRank, 0, []);
	}
};


//==================================================================================================

_copy2DArray = function(from, to) {
    for (var i = 0; i < from.length; i++) {
        to.push(from[i].slice(0));
    }
}

_makeFlattened2DArrayCopy = function(array) {
    var flattenedcopy = [].concat.apply([], array);
    return flattenedcopy;
}


/*
function shuffleArray (array) {
    // Using Fisher-Yates Shuffle algorithm

    var counter = array.length, temp, index;

    // While there are elements in the array
    while (counter > 0) {
        // Pick a random index
        index = (Math.random() * counter--) | 0;

        // And swap the last element with it
        temp = array[counter];
        array[counter] = array[index];
        array[index] = temp;
    }

    return array;
}
*/

function cloneObject(obj) {
    var target = {};
    for (var i in obj) {
        if (obj.hasOwnProperty(i))
            target[i] = obj[i];
    }
    return target;
}

function arrayContains(array, item) {
    if (Array.prototype.indexOf) {
        return !(array.indexOf(item) < 0);
    }
    else {
        for (var i = 0; i < array.length; i++) {
            if (array[i] === item)
                return true;
        }
        return false;
    }
}

function replaceInArray(array, value, newValue) {
    for(var i in array){
        if(array[i] == value) {
            array[i] = newValue;
            break;
        }
    }
}

function removeFirstOccurrenceByValue(array, item) {
    for(var i in array) {
        if(array[i] == item) {
            array.splice(i,1);
            break;
        }
    }
}

function isInt(n) {
    //return +n === n && !(n % 1);
    return !(n % 1);
}

function swap (array, x, y) {
    var b = array[y];
    array[y] = array[x];
    array[x] = b;
}

function permute2DArrayInFirstDimension (permutations, array, from) {
   var len = array.length;

   if (from == len-1) {
       //if ( len == 1 || Math.max.apply(null, array[0]) < Math.max.apply(null, array[len-1]) )   // discard mirror copies of other permutations
           permutations.push(_makeFlattened2DArrayCopy(array));
       return;
   }

   for (var j = from; j < len; j++) {
      swap(array, from, j);
      permute2DArrayInFirstDimension(permutations, array, from+1);
      swap(array, from, j);
   }
}



// used for profiling code
Timer = function() {
    this.startTime = undefined;
    this.lastCheck = undefined;
    this.start();
};

Timer.prototype = {

    start: function() {
        this.startTime = new Date().getTime();
        this.lastCheck = this.startTime;
    },

    restart: function() {
        this.start();
    },

    report: function() {
        var current = new Date().getTime();
        var elapsed = current - this.lastCheck;
        return elapsed;
    },

    printSinceLast: function( msg ) {
        var current = new Date().getTime();
        var elapsed = current - this.lastCheck;
        this.lastCheck = current;
        console.log( msg + elapsed + "ms" );
    },
};

