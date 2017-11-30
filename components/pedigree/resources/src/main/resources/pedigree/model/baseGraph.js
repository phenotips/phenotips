// BaseGraph represents the pedigree tree as a graph of nodes and edges with certain
//           properties attached to both (e.g. names for nodes and weights for edges).
//
//           Nodes represent either persons or partnerships, while edges represent
//           either "is part of this partnership" or "is a child of" relationships
//           between the connected nodes.
//
// BaseGraph contains only the information found in the pedigree (plus possibly
//           some cached data), i.e. there is no layout data here.
define([
        "pedigree/model/helpers"
    ], function(
        Helpers
    ){

    // options: Object (optional), see this.options
    BaseGraph = function (options)
    {
        this.v        = [];        // for each V lists (as unordered arrays of ids) vertices connected from V
        this.inedges  = [];        // for each V lists (as unordered arrays of ids) vertices connecting to V

        this.maxRealVertexId = -1; // used for separation of real vertices from virtual-multi-rank-edge-breaking ones (stored for performance)

        this.weights  = [];        // for each V contains outgoing edge weights as {target1: weight1, t2: w2}

        this.type       = [];      // for each V node type (see BaseGraph.TYPE)

        this.properties = [];      // for each V a set of type-specific properties (e.g. "gender", "name", etc.)
                                   // some of the properties affect the layout algorithm (e.g. "gender" and "age")
                                   // so those need to be in a known format independent of any external APIs

        this.rawJSONProperties = []; // node properties in whatever JSON format is used to export/import the data;
                                     // this is only supposed ot be used for exporting data back into the external format,
                                     // which may support more data than this code can store/is aware of

        // original algorithm supported nodes of different widths; in practice this functionality
        // is not useful in a pedigree, so all external API references to this are removed, however
        // internally the functionality is kept i ncas eit is needed in the future (it is hard
        // to rip it away and even harder to plug back in later)
        this.vWidth = [];

        this.options = { "defaultPersonNodeWidth": 10,
                         "defaultNonPersonNodeWidth": 2,
                         "defaultEdgeWeight": 1 };

        // use user provided options for all keys which match existing internal option keys
        Helpers.setByTemplate(this.options, options);
    };

    BaseGraph.TYPE = {
      RELATIONSHIP: 1,
      CHILDHUB:     2,
      PERSON:       3,
      VIRTUALEDGE:  4    // for nodes not present in the original graph; used as intermediate steps in multi-rank edges
    };

    BaseGraph.fromJSON = function(jsonObject)
    {
        var result = new BaseGraph();
        for (var prop in result) {
            if (result.hasOwnProperty(prop)) {
                if (!jsonObject.hasOwnProperty(prop) || typeof result[prop] != typeof jsonObject[prop]) {
                    throw "Can't initialize BaseGraph from provided JSON: [" + prop
                        + "] is either missing or has invalid type";
                }
                result[prop] = jsonObject[prop];
            }
        }
        result.validate();
        return result;
    },

    BaseGraph.prototype = {

        toJSONObject: function() {
            return JSON.parse(JSON.stringify(this));
        },

        //--------------------------[multi-rank edge handling]-

        // After ranks are known it is possible to detect if there are edges crossing multiple ranks
        hasMultiRankEdges: function(ranks) {
            for (var sourceV = 0; sourceV < this.v.length; sourceV++) {
                var sourceRank = ranks[sourceV];
                for (var i = 0; i < this.v[sourceV].length; i++) {
                    var targetV = this.v[sourceV][i];
                    var targetRank = ranks[targetV];
                    if (targetRank < sourceRank) {
                        throw "Assertion failed: only forward edges";
                    }
                    if (targetRank > sourceRank + 1) {
                        return true;
                    }
                }
            }
            return false;
        },

        // Given rank assignment, edges between nodes more than one rank apart are replaced by
        // chains of unit length edges between temporary or ‘‘virtual’’ nodes. The virtual nodes are
        // placed on the intermediate ranks, converting the original graph into one whose edges connect
        // only nodes on adjacent ranks
        //
        // Returns the modified ranks array which includes ranks of new virtual nodes (if any)
        splitMultiRankEdges: function (initialRanks) {
            var ranks = initialRanks.slice();
            var numLongEdges = 0;

            // go over all vertices
            for (var sourceV = 0; sourceV < this.v.length; sourceV++) {
                var sourceRank = ranks[sourceV];
                for (var i = 0; i < this.v[sourceV].length; i++) {
                    var targetV = this.v[sourceV][i];
                    var targetRank = ranks[targetV];

                    if (targetRank < sourceRank)
                        throw "Assertion failed: only forward edges";

                    if (targetRank > sourceRank + 1) {
                        // virtual edge detected: remnove original edge and instead create
                        // a series of virtual vertices and edges
                        numLongEdges++;

                        var weight = this.getEdgeWeight(sourceV, targetV);

                        var prevV = sourceV;
                        for (var midRank = sourceRank+1; midRank <= targetRank - 1; midRank++) {
                            // sourceV -> targetV, segment number (midRank-sourceRank-1)
                            var nextV = this._addVertex( null, BaseGraph.TYPE.VIRTUALEDGE, {}, {} );
                            ranks[nextV] = midRank;

                            if (prevV == sourceV) {
                                this.replaceEdge( prevV, targetV, nextV);
                            } else {
                                this.addEdge( prevV, nextV, weight );
                            }
                            prevV = nextV;
                        }
                        this.addEdge(prevV, targetV, weight);
                    }
                }
            }

            if (numLongEdges > 0) {
                console.log("Detected and expanded " + numLongEdges + " mult-rank edges");
                this.validate();    // just in case
            }
            return ranks;
        },

        collapseMultiRankEdges: function () {
            if (this.getNumVertices() == this.maxRealVertexId + 1) {
                return;
            }
            for (var sourceV = 0; sourceV <= this.maxRealVertexId; sourceV++) {
                for (var i = 0; i < this.v[sourceV].length; i++) {
                    var targetV = this.v[sourceV][i];
                    if (targetV > this.maxRealVertexId) {
                        var path = [];
                        while (targetV > this.maxRealVertexId) {
                            path.push(targetV);
                            targetV = this.getOutEdges(targetV)[0];  // [0] since virtual edges have only one outedge
                        }
                        var relationshipV = targetV;
                        // replace edges from person to virtual and from virtual to relationship with a single
                        // direct edge from person to relationship; note that edges between virtual nodes are left alone here,
                        // that is OK as they will be removed together with virtual vertices themselves
                        this.replaceEdge(sourceV, path[0], relationshipV);
                        this.removeEdge(path[path.length-1], relationshipV);
                    }
                }
            }

            // at this point there should be no more vertices left with ID > this.maxRealVertexId
            this.v.splice(this.maxRealVertexId+1);
            this.inedges.splice(this.maxRealVertexId+1);
            this.weights.splice(this.maxRealVertexId+1);
            this.vWidth.splice(this.maxRealVertexId+1);
            this.type.splice(this.maxRealVertexId+1);
            this.properties.splice(this.maxRealVertexId+1);
            this.rawJSONProperties.splice(this.maxRealVertexId+1);

            // for debug only
            this.validate();
        },

        //--------------------------[end multi-rank edge handling]-

        getLeafAndParentlessNodes: function () {
            var result = { "parentlessNodes": [],
                           "leafNodes":       [] };

            // find all vertices without an in-edge
            for (var vid = 0; vid <= this.maxRealVertexId; vid++) {
                if ( this.getInEdges(vid).length == 0 ) {
                    result.parentlessNodes.push(vid);
                }
                else
                if ( this.getOutEdges(vid).length == 0 ) {
                    result.leafNodes.push(vid);
                }
            }

            return result;
        },

        // id: optional. If not specified then next available is used.
        // note: unlike insertVetex() does not do any id shifting and should be used only for initialization of the graph
        _addVertex: function(id, type, properties, rawJSONProperties, width) {
            if (id && this.v[id]) throw "addVertex: vertex with id=" + id + " is already in G";

            var nextId = (id == null) ? this.v.length : id;

            this.v[nextId] = [];

            this.inedges[nextId] = [];

            this.weights[nextId] = {};

            var width = width ? width : ((type == BaseGraph.TYPE.PERSON) ? this.options.defaultPersonNodeWidth : this.options.defaultNonPersonNodeWidth);

            this.vWidth[nextId] = width;

            this.type[nextId] = type;

            this.properties[nextId] = properties;
            this.rawJSONProperties[nextId] = (rawJSONProperties == null) ? {"__ignore__": true} : rawJSONProperties;

            if (type != BaseGraph.TYPE.VIRTUALEDGE && nextId > this.maxRealVertexId)
                this.maxRealVertexId = nextId;

            return nextId;
        },

        addEdge: function(fromV, toV, weight) {
            if (!weight) {
                weight = this.options.defaultEdgeWeight;
            }
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

        removeEdge: function(fromV, toV) {
            if (!this.hasEdge(fromV,toV))
                throw "removeEdge: edge does not exist";

            Helpers.removeFirstOccurrenceByValue(this.v[fromV], toV);
            Helpers.removeFirstOccurrenceByValue(this.inedges[toV], fromV);

            var weight = this.weights[fromV][toV];
            delete this.weights[fromV][toV];

            return weight;
        },

        // this is equivalent to removeEdge() + addEdge(), exept that the new edge occupies the same position in the v[fromV] array
        replaceEdge: function(fromV, oldV, newV) {
            if (!this.hasEdge(fromV, oldV))
                throw "removeEdge: edge does not exist";

            this.weights[fromV][newV] = this.weights[fromV][oldV];
            delete this.weights[fromV][oldV];

            Helpers.replaceInArray(this.v[fromV], oldV, newV);
            Helpers.removeFirstOccurrenceByValue(this.inedges[oldV], fromV);

            this.inedges[newV].push(fromV);
        },

        insertVertex: function(type, properties, edgeWeights, inedges, outedges) {
            var width = (type == BaseGraph.TYPE.PERSON) ? this.options.defaultPersonNodeWidth : this.options.defaultNonPersonNodeWidth;

            if (type == BaseGraph.TYPE.PERSON && !properties.hasOwnProperty("gender"))
                properties["gender"] = "U";

            var newNodeId = (type == BaseGraph.TYPE.VIRTUALEDGE) ? this.v.length : this.maxRealVertexId + 1;    // all real node IDs should be <= maxRealVertexId, so have to insert new node here

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
            this.vWidth    .splice(newNodeId,0,width);
            this.type      .splice(newNodeId,0,type);
            this.properties.splice(newNodeId,0,properties);
            this.rawJSONProperties.splice(newNodeId,0,{});

            if (type != BaseGraph.TYPE.VIRTUALEDGE)
                this.maxRealVertexId++;

            // add new edges
            for (var i = 0; i < inedges.length; i++)
                this.addEdge( inedges[i], newNodeId, edgeWeights);
            for (var i = 0; i < outedges.length; i++)
                this.addEdge( newNodeId, outedges[i], edgeWeights);

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
            Helpers.replaceInArray(this.v[parent], v, child);
            this.weights[parent][child] = this.weights[parent][v];
            delete this.weights[parent][v];

            // replace incoming edge for child from V to parent
            Helpers.replaceInArray(this.inedges[child], v, parent);

            this.v[v] = [];
            this.inedges[v] = [];
            this.weights[v] = {};
        },

        remove: function(v) {
            for (var i = 0; i < this.v[v].length; i++) {
                var target = this.v[v][i];
                Helpers.removeFirstOccurrenceByValue(this.inedges[target], v);
            }
            for (var i = 0; i < this.inedges[v].length; i++) {
                var incoming = this.inedges[v][i];
                Helpers.removeFirstOccurrenceByValue(this.v[incoming], v);
                delete this.weights[incoming][v];
            }

            //console.log("V before: " + Helpers.stringifyObject(this.v));
            this.v.splice(v,1);
            //console.log("V after: " + Helpers.stringifyObject(this.v));
            this.inedges.splice(v,1);
            this.weights.splice(v,1);
            this.vWidth.splice(v,1);
            this.type.splice(v,1);
            this.properties.splice(v,1);
            this.rawJSONProperties.splice(v,1);
            if (v <= this.maxRealVertexId)
                this.maxRealVertexId--;

            // as all IDs above v are decreased by one, and all references should be updated
            var test         = function(u) { return (u > v); }
            var modification = function(u) { return u - 1; }
            this._updateAllReferencesToNewIDs( test, modification );
        },

        _updateAllReferencesToNewIDs: function ( test, modification ) {
            // updates all references (e.g. out- and in- edge targets, etc.) pointing to
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
                for (var u in weights) {
                    if (weights.hasOwnProperty(u))
                        u = parseInt(u);
                        if (test(u))
                            newWeights[modification(u)] = weights[u];
                        else
                            newWeights[u] = weights[u];
                }
                this.weights[i] = newWeights;
            }
        },

        validate: function() {
            //console.log("-- VALIDATING: " + Helpers.stringifyObject(this));

            if( this.v.length == 0 ) return;

            for (var v = 0; v < this.v.length; v++) {
                var outEdges = this.getOutEdges(v);
                var inEdges  = this.getInEdges(v);

                if (this.isPerson(v)) {
                    if (inEdges.length > 1)
                        throw "Assertion failed: person nodes can't have two in-edges as people are produced by a single pregnancy (failed for " + this.getVertexDescription(v) + ")";
                    for (var i = 0; i < outEdges.length; i++)
                        if (!this.isRelationship(outEdges[i]) && !this.isVirtual(outEdges[i]) )
                            throw "Assertion failed: person nodes have only out edges to relationships (failed for " + this.getVertexDescription(v) + ")";
                }
                else if (this.isRelationship(v)) {
                    // TODO: for childless relations this is not true!
                    if (outEdges.length == 0)
                        throw "Assertion failed: all relationships should have a childhub associated with them (failed for " + this.getVertexDescription(v) + ")";
                    if (outEdges.length > 1)
                        throw "Assertion failed: all relationships should have only one outedge (to a childhub) (failed for " + this.getVertexDescription(v) + ")";
                    if (!this.isChildhub(outEdges[0]))
                        throw "Assertion failed: relationships should only have out edges to childhubs (failed for " + this.getVertexDescription(v) + ")";
                    if (inEdges.length != 2)
                        throw "Assertion failed: relationships should always have exactly two associated persons (failed for " + this.getVertexDescription(v) + ")";
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
                            throw "Assertion failed: childhubs are only connected to people (failed for " + this.getVertexDescription(v) + ")";
                }
            }

            var leafAndRootlessInfo = this.getLeafAndParentlessNodes();

            // check for cycles - supposedly pedigrees can't have any
            if (leafAndRootlessInfo.parentlessNodes.length == 0)
                throw "Assertion failed: pedigrees should have no cycles (no parentless nodes found)";
            for (var j = 0; j < leafAndRootlessInfo.parentlessNodes.length; j++) {
                if ( this._DFSFindCycles( leafAndRootlessInfo.parentlessNodes[j], {} ) )
                    throw "Assertion failed: pedigrees should have no cycles";
            }

            // check for disconnected components
            var reachable = {};
            this._markAllReachableComponents( leafAndRootlessInfo.parentlessNodes[0], reachable );
            for (var v = 0; v < this.v.length; v++) {
                if (!reachable.hasOwnProperty(v))
                    throw "Assertion failed: disconnected component detected (" + this.getVertexDescription(v) + ")";
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

        getVertexNameById: function(v) {
            var firstname = this.properties[v].hasOwnProperty("fName") ? this.properties[v]["fName"] : "";
            var lastname  = this.properties[v].hasOwnProperty("lName") ? this.properties[v]["lName"] : "";

            if (firstname != "" && lastname != "" ) firstname += " ";

            return firstname + lastname;
        },

        getVertexDescription: function(v) {
            var desc = "id: " + v + ", name: <" + this.getVertexNameById(v) + ">, type: ";
            switch (this.type[v]) {
            case BaseGraph.TYPE.PERSON:       desc += "PERSON";   break;
            case BaseGraph.TYPE.RELATIONSHIP: desc += "RELATION"; break;
            case BaseGraph.TYPE.CHILDHUB:     desc += "CHILDHUB"; break;
            case BaseGraph.TYPE.VIRTUALEDGE:  desc += "VIRTUAL";  break;
            default:                desc += "ERROR";    break;
            }
            return "[" + desc + "]";
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

        isValidId: function(v) {
            return (v >=0 && v < this.v.length);
        },

        getNumVertices: function() {
            return this.v.length;
        },

        getMaxRealVertexId: function() {
            return this.maxRealVertexId; // all vertices with IDs greater than this are of type VIRTUALEDGE
        },

        getOutEdges: function(v) {
            return this.v[v];
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

        getAllEdges: function(v) {
            return this.getOutEdges(v).concat(this.getInEdges(v));
        },

        isRelationship: function(v) {
            return (this.type[v] == BaseGraph.TYPE.RELATIONSHIP);
        },

        isChildhub: function(v) {
            return (this.type[v] == BaseGraph.TYPE.CHILDHUB);
        },

        isPerson: function(v) {
            return (this.type[v] == BaseGraph.TYPE.PERSON);
        },

        isPlaceholder: function(v) {
            if (!this.isPerson(v) || !this.properties[v].hasOwnProperty("placeholder")) {
                return false;
            }
            if (this.properties[v]["placeholder"]) {
                return true;
            }
            return false;
        },

        isVirtual: function(v) {
            return (this.type[v] == BaseGraph.TYPE.VIRTUALEDGE);  // also: v > getmaxRealVertexId()
        },

        isAdoptedIn: function(v)
        {
            if (this.properties[v].hasOwnProperty("adoptedStatus"))
                return this.properties[v]["adoptedStatus"] == "adoptedIn";
            return false;
        },

        isAdoptedOut: function(v)
        {
            if (this.properties[v].hasOwnProperty("adoptedStatus"))
                return this.properties[v]["adoptedStatus"] == "adoptedOut";
            return false;
        },

        getGender: function(v) {
            if (!this.isPerson(v))
                throw "Assertion failed: attempting to get gender of a non-person";
            return this.properties[v]["gender"];
        },

        getLastName: function(v) {
            if (!this.isPerson(v))
                throw "Assertion failed: attempting to get last name of a non-person";
            if (!this.properties[v].hasOwnProperty("lName")) {
                if (!this.properties[v].hasOwnProperty("lNameAtB")) {
                    return "";
                } else {
                    return this.properties[v]["lNameAtB"];
                }
            }
            return this.properties[v]["lName"];
        },

        getLastNameAtBirth: function(v) {
            if (!this.isPerson(v))
                throw "Assertion failed: attempting to get last name at birth of a non-person";
            if (!this.properties[v].hasOwnProperty("lNameAtB"))
                return "";
            return this.properties[v]["lNameAtB"];
        },

        getOppositeGender: function(v) {
            if (!this.isPerson(v))
                throw "Assertion failed: attempting to get gender of a non-person";

            if (this.getGender(v) == "U") {
                return "U";
            }
            else if (this.getGender(v) == "O") {
                return "U";
            }
            else if (this.getGender(v) == "M") {
                return "F";
            }
            else {
                return "M";
            }
        },

        getRelationshipChildhub: function(v) {
            if (!this.isRelationship(v))
                throw "Assertion failed: applying getRelationshipChildhub() to a non-relationship node";

            return this.v[v][0];
        },

        getAllRelationships: function(v) {
            if (!this.isPerson(v))
                throw "Assertion failed: attempting to get relationships of a non-person";

            var relationships = this.v[v];

            var result = [];
            for (var r = 0; r < relationships.length; ++r) {
                var edgeTo       = relationships[r];
                var relationship = this.downTheChainUntilNonVirtual(edgeTo);
                result.push(relationship);
            }
            return result;
        },

        getAllPartners: function(v) {
            if (!this.isPerson(v))
                throw "Assertion failed: attempting to get partners of a non-person";

            var relationships = this.getAllRelationships(v);

            var result = [];
            for (var r = 0; r < relationships.length; ++r) {
                var partners = this.getParents(relationships[r]);
                if (partners[0] != v)
                    result.push(partners[0]);
                else
                    result.push(partners[1]);
            }
            return result;
        },

        getParents: function(v) {
            if (!this.isPerson(v) && !this.isRelationship(v))
                throw "Assertion failed: attempting to get parents of a non-person and non-relationship";

            // skips through relationship and child nodes and returns an array of two real parents. If none found returns []

            var parentRelationship = this.isPerson(v) ? this.getProducingRelationship(v) : v;

            if (parentRelationship == null)  // no parents
                return [];

            var inEdges = this.getInEdges(parentRelationship);

            if (inEdges.length != 2)
                throw "Assertion failed: exactly two parents";

            return [this.upTheChainUntilNonVirtual(inEdges[0]), this.upTheChainUntilNonVirtual(inEdges[1])];
        },

        // returns a bottom-to-top path
        getPathToParents: function(v)
        {
            // returns an array with two elements: path to parent1 (excluding v) and path to parent2 (excluding v):
            // [ [virtual_node_11, ..., virtual_node_1n, parent1], [virtual_node_21, ..., virtual_node_2n, parent21] ]

            var result = [];

            if (!this.isRelationship(v))
                throw "Assertion failed: incorrect v in getPathToParents()";

            var inEdges = this.getInEdges(v);

            result.push( this._getUpPathEndingInNonVirtual(inEdges[0]) );
            result.push( this._getUpPathEndingInNonVirtual(inEdges[1]) );

            return result;
        },

        _getUpPathEndingInNonVirtual: function(v)
        {
            var path = [];

            while (this.isVirtual(v))
            {
                path.push(v)
                v = this.inedges[v][0];
            }

            return {"parent": v, "path": path};
        },

        getAllPersons: function(includePlaceholders)
        {
            var result = [];
            for (var i = 0; i <= this.getMaxRealVertexId(); i++) {
                if (this.isPerson(i)) {
                    if (!this.isPlaceholder(i) || includePlaceholders) {
                        result.push(i);
                    }
                }
            }
            return result;
        },

        getAllChildren: function( v )
        {
            if (!this.isPerson(v) && !this.isRelationship(v))
                throw "Assertion failed: getAllChildren() is applied to a non-person non-relationship node";

            var rels = this.isRelationship(v) ? [v] : this.getAllRelationships(v);

            var allChildren = [];
            for (var i = 0; i < rels.length; i++) {
                var chhub    = this.getRelationshipChildhub(rels[i]);
                var children = this.getOutEdges(chhub);
                allChildren = allChildren.concat(children);
            }
            return allChildren;
        },

        getProducingRelationship: function(v) {
            if (!this.isPerson(v))
                throw "Assertion failed: attempting to get producing relationship of a non-person";

            // find the relationship which produces this node (or null if not present)
            if (this.inedges[v].length == 0) return null;
            var chHub = this.inedges[v][0];

            if (this.inedges[chHub].length == 0) return null;
            return this.inedges[chHub][0];
        },

        upTheChainUntilNonVirtual: function(v) {
            if (!this.isVirtual(v)) return v;

            return this.upTheChainUntilNonVirtual( this.inedges[v][0] );  // virtual nodes have only one in-edges, all the way up until a person node
        },

        downTheChainUntilNonVirtual: function(v) {
            if (!this.isVirtual(v)) return v;

            return this.downTheChainUntilNonVirtual( this.v[v][0] );  // virtual nodes have only one in-edges, all the way up until a person node
        },

        getUnusedTwinGroupId: function(v)
        {
            if (!this.isRelationship(v))
                throw "Assertion failed: incorrect v in getNumTwinGroups()";

            var childhubId = this.v[v][0];
            var children = this.v[childhubId];

            var twinGroupExists = [];
            for (var c = 0; c < children.length; c++) {
                var child = children[c];
                if (this.properties[child].hasOwnProperty('twinGroup'))
                    twinGroupExists[this.properties[child]['twinGroup']] = true;
            }

            var firstFreeTwinGroupId = 0;
            for (var i = 0; i < twinGroupExists.length; i++) {
                if (twinGroupExists[i] !== undefined)
                    firstFreeTwinGroupId = i+1;
                else
                    break;
            }
            return firstFreeTwinGroupId;
        },

        getTwinGroupId: function(v)
        {
            if (!this.properties[v].hasOwnProperty('twinGroup'))
                return null;
            return this.properties[v]['twinGroup'];
        },

        getAllSiblingsOf: function(v)
        {
            // note: includes v itself

            if (!this.isPerson(v))
                throw "Assertion failed: incorrect v in getAllSiblingsOf()";

            if (this.inedges[v].length == 0) {
                return [v];
            }

            var childhubId = this.inedges[v][0];
            var children = this.v[childhubId];
            return children.slice(0);
        },

        getAllTwinsOf: function(v)
        {
            if (!this.isPerson(v))
                throw "Assertion failed: incorrect v in getAllTwinsOf()";

            if (!this.properties[v].hasOwnProperty('twinGroup') || this.inedges[v].length == 0) {
                // no twinGroup or twinGroup is a leftover from a previous state and parents (and twins) have been deleted by now
                return [v];
            }

            var twinGroupId = this.properties[v]['twinGroup'];

            var childhubId = this.inedges[v][0];
            var children = this.v[childhubId];

            var twins = [];
            for (var c = 0; c < children.length; c++) {
                var child = children[c];
                if (this.properties[child].hasOwnProperty('twinGroup') && this.properties[child]['twinGroup'] == twinGroupId)
                    twins.push(child);
            }
            return twins;
        },

        isParentToTwinEdge: function (fromV, toV)
        {
            if (this.isPerson(toV) && this.isChildhub(fromV) &&
                this.getTwinGroupId(toV) != null) return true;

            return false;
        },

        getAllAncestors: function(v)
        {
            var ancestors = {};
            ancestors[v] = true;

            var q = new Queue();
            q.push(v);

            while( q.size() > 0) {
                var nextV = q.pop();

                var inEdges = this.getInEdges(nextV);
                for (var j = 0; j < inEdges.length; j++) {
                    var v = inEdges[j];
                    if ( !ancestors.hasOwnProperty(v) ) {
                        q.push(v);
                        ancestors[v] = true;
                    }
                }
            }
            return ancestors;
        },

        // Returns an object with "mother" and "father" properties. One or both can be undefined, or nodeID
        // "undefined' means there are no parent(s) or parent gender does not alow mother/father indentification
        // (e.g. gender is "other" or there is more than one parent of the same gender)
        // Includes all other parents in the "other" array
        getMotherFather: function(v) {
            var result = {"mother": undefined, "father": undefined, "other": []};

            var parents = this.getParents(v);
            for (var i = 0; i < parents.length; i++) {
                var gender = this.properties[parents[i]]["gender"];
                if (gender == "M") {
                    if (result.father === undefined) {
                        // father isnot defined yet
                        result.father = parents[i];
                        continue;
                    } else {
                        // more than one father -> dont know which one is father
                        result.father = undefined;
                    }
                } else if (gender == "F") {
                    if (result.mother === undefined) {
                        result.mother = parents[i];
                        continue;
                    } else {
                        result.mother = undefined;
                    }
                }
                result.other.push(parents[i]);
            }
            return result;
        }
    };

    return BaseGraph;
});
