define([
      "pedigree/pedigreeDate",
      "pedigree/hpoTerm",
      "pedigree/model/baseGraph",
      "pedigree/model/ordering",
      "pedigree/model/helpers"
    ], function(
      PedigreeDate,
      HPOTerm,
      BaseGraph,
      Ordering,
      Helpers
    ){
    var PedigreeImport = function () {
    };

    PedigreeImport.prototype = {
    };

    /**
     * Returns the current version of the Full JSON represenation
     */
    PedigreeImport.getCurrentFullJSONVersion = function() {
        return "1.0";
    },

    /**
     * Returns the set of all supported Full JSON versions which can be imported
     */
    PedigreeImport.getSupportedPedigreeJSONVersions = function() {
        var versions = {};
        versions[PedigreeImport.getCurrentFullJSONVersion()] = true;
        return versions;
    },

    PedigreeImport.initFromFullJSON = function(inputText)
    {
        var KEY_PROBAND = "proband";
        var KEY_MEMBERS = "members";
        var KEY_RELATIONSHIPS = "relationships";
        var KEY_RELATIONSHIPS_MEMBERS = "members";
        var KEY_RELATIONSHIPS_CHILDREN = "children";
        var KEY_LAYOUT = "layout";
        var KEY_PROPERTIES_PHENOTIPSID = "id";
        var KEY_PROPERTIES_GENDER = "sex";

        try {
            var input = JSON.parse(inputText);
        } catch(err) {
            throw "Unable to import pedigree: input is not a valid pedigree JSON: " + err.message;
        }

        if (!input.hasOwnProperty("JSON_version")
            || !PedigreeImport.getSupportedPedigreeJSONVersions().hasOwnProperty(input["JSON_version"])) {
            throw("Can not initialize from JSON: unsupported version of pedigree JSON format [" + input["JSON_version"] + "]");
        }
        if (typeof input != 'object') {
            throw "Unable to import pedigree: JSON does not represent an object";
        }
        if (!input.hasOwnProperty(KEY_MEMBERS) || !Array.isArray(input[KEY_MEMBERS])) {
            throw "Unable to import pedigree: required field <" + KEY_MEMBERS + "> is missing or not an array";
        }
        if (input.hasOwnProperty(KEY_RELATIONSHIPS) && !Array.isArray(input[KEY_RELATIONSHIPS])) {
            throw "Unable to import pedigree: field <" + KEY_RELATIONSHIPS + "> is not an array";
        }

        // if import code has to create new virtual nodes (e.g. a missing second parent) it means
        // the layout can't possibly work, need to redo it from sratch
        var ignoreLayout = false;

        var memberIDToNodeID = {};
        var relationshipIDToNodeID = {};

        var newG = new BaseGraph();

        var unlinkedMembers = {};

        // first pass: add all vertices and assign vertex IDs
        var members = input[KEY_MEMBERS];
        for (var i = 0; i < members.length; i++) {
            var nextPerson = members[i];

            if (typeof nextPerson != 'object') {
                throw "Unable to import pedigree: a person is not represented as a JSON object";
            }
            if (!nextPerson.hasOwnProperty("id")) {
                throw "Unable to import pedigree: a person has no ID";
            }
            if (memberIDToNodeID.hasOwnProperty(nextPerson.id)) {
                throw "Unable to import pedigree: duplicate person ID <" + nextPerson.id + ">";
            }

            if (nextPerson.hasOwnProperty("notInPedigree") && nextPerson.notInPedigree) {
                if (!nextPerson.hasOwnProperty("properties") || typeof nextPerson.properties != 'object') {
                    throw "Unable to import pedigree: a not-in-pedigree member of the family has no PhenoTips ID";
                }
                if (nextPerson.properties.hasOwnProperty(KEY_PROPERTIES_PHENOTIPSID)) {
                    var id = nextPerson.properties[KEY_PROPERTIES_PHENOTIPSID];
                    unlinkedMembers[id] = nextPerson.properties;
                }
                // else: this is an unlinked patient with no patient record ID: ignore,
                // must be an unlinked patient removed from the family
                continue;
            }

            var properties = {};
            if (nextPerson.hasOwnProperty("pedigreeProperties") && typeof nextPerson.pedigreeProperties == 'object') {
                properties = nextPerson.pedigreeProperties;
                if (!properties.gender) {
                    properties.gender = "U";
                }
            }

            var nodeID = newG._addVertex( null, BaseGraph.TYPE.PERSON, properties, {} );
            memberIDToNodeID[nextPerson.id] = nodeID;

            if (nextPerson.hasOwnProperty("properties") && typeof nextPerson.properties == 'object') {
                newG.rawJSONProperties[nodeID] = nextPerson.properties;
                if (nextPerson.properties.hasOwnProperty(KEY_PROPERTIES_PHENOTIPSID)) {
                    newG.properties[nodeID]["phenotipsId"] = nextPerson.properties[KEY_PROPERTIES_PHENOTIPSID];
                }
                // layout algorithm, which is run before an updated JSON is loaded, uses gender as one of the parameters,
                // thus gender needs to be set at all times and from the very beginning
                if (nextPerson.properties.hasOwnProperty(KEY_PROPERTIES_GENDER)) {
                    newG.properties[nodeID]["gender"] = nextPerson.properties[KEY_PROPERTIES_GENDER];
                } else {
                    newG.properties[nodeID]["gender"] = "U";
                }
            } else {
                newG.rawJSONProperties[nodeID] = {"__ignore__": true};
            }
        }

        var relationshipTracker = new RelationshipTracker(newG);

        var childToRelationship = {};

        // once all node/vertex IDs are known: second pass, process relationships/parents/children and add edges
        if (input.hasOwnProperty(KEY_RELATIONSHIPS)) {
            var relationships = input[KEY_RELATIONSHIPS];
            for (var r = 0; r < relationships.length; r++) {
                var nextRelationship = relationships[r];

                if (!nextRelationship.hasOwnProperty(KEY_RELATIONSHIPS_MEMBERS) ||
                    !Array.isArray(nextRelationship[KEY_RELATIONSHIPS_MEMBERS])) {
                    throw "Unable to import pedigree: relationship is missing <" + KEY_RELATIONSHIPS_MEMBERS + "> array";
                }
                var members = nextRelationship[KEY_RELATIONSHIPS_MEMBERS];

                if (nextRelationship.hasOwnProperty(KEY_RELATIONSHIPS_CHILDREN) &&
                    Array.isArray(nextRelationship[KEY_RELATIONSHIPS_CHILDREN]) &&
                    nextRelationship[KEY_RELATIONSHIPS_CHILDREN].length > 0) {
                    var children = nextRelationship[KEY_RELATIONSHIPS_CHILDREN];
                } else {
                    var children = [];
                }

                if (members.length < 1) {
                    // can only happen if there are at least two children, e.g. relationship is used
                    // to indicate "sibling" relationship between two or more persons
                    if (children.length == 0) {
                        throw "Unable to import pedigree: relationship has no members (at least one required)";
                    }
                    ignoreLayout = true;
                    // create virtual second parent
                    var nodeID1 = newG._addVertex( null, BaseGraph.TYPE.PERSON, {"gender": "U"}, {} );
                } else {
                    var nodeID1 = memberIDToNodeID[members[0]];
                }

                if (members.length < 2) {
                    ignoreLayout = true;
                     // create virtual second parent
                    var nodeID2 = newG._addVertex( null, BaseGraph.TYPE.PERSON, {"gender": "U"}, {} );
                } else {
                    var nodeID2 = memberIDToNodeID[members[1]];
                }

                // get or create a relationship
                var chhubID = relationshipTracker.createOrGetChildhub(nodeID1, nodeID2);

                if (nextRelationship.hasOwnProperty("id")) {
                    relationshipIDToNodeID[nextRelationship.id] = relationshipTracker.getRelationshipIDForChildHub(chhubID);
                }

                // add links to all children (or create a placeholder child if there are none)
                if (children.length > 0) {
                    for (var i = 0; i < children.length; i++) {
                        var child = children[i];
                        if (!child.hasOwnProperty("id")) {
                            throw "Unable to import pedigree: a relationship child has no id";
                        }
                        if (!memberIDToNodeID.hasOwnProperty(child.id)) {
                            throw "Unable to import pedigree: child id <" + child.id + "> is not an id of an existing person";
                        }

                        if (childToRelationship.hasOwnProperty(child.id)) {
                            // TODO: in the future multiple relationships referencing the same child will be
                            //       supported for the case of one relationship adopting out, and another adopting in
                            throw "Unable to import pedigree: child id <" + child.id + "> belongs to two relationships";
                        }
                        childToRelationship[child.id] = chhubID;

                        var childNodeId = memberIDToNodeID[child.id];

                        newG.addEdge( chhubID, childNodeId );

                        // process supported relationship properties (TODO: for now only adopted status)
                        if (child.hasOwnProperty("adopted") && child.adopted != "no") {
                            newG.properties[childNodeId].adoptedStatus = (child.adopted == "out") ? "adoptedOut" : "adoptedIn";
                        }
                    }
                } else {
                    // add placehodlre child, as required by current version of pedigree
                    // TODO: get rid of this requirement, placehodlrer children are non-intuitive and add layout issues
                    ignoreLayout = true;
                    var placeholderID = newG._addVertex(null, BaseGraph.TYPE.PERSON, {"gender":"U", "placeholder":true}, {} );
                    newG.addEdge( chhubID, placeholderID );
                }

                if (nextRelationship.hasOwnProperty("properties")) {
                    // set relationship properties
                    var relationshipID = relationshipTracker.getRelationshipIDForChildHub(chhubID);
                    for (var property in nextRelationship.properties) {
                        if (nextRelationship.properties.hasOwnProperty(property)) {
                            var value    = nextRelationship.properties[property];
                            var property = property.toLowerCase();

                            var processed = PedigreeImport.convertRelationshipProperty(property, value);
                            if (processed !== null) {
                                // supported property
                                newG.properties[relationshipID][processed.propertyName] = processed.value;
                            }
                        }
                    }
                }
            }
        }

        try {
            var probandID = input.hasOwnProperty(KEY_PROBAND) ? memberIDToNodeID[input[KEY_PROBAND]] : null;
        } catch( err) {
            throw "Unable to import pedigree: proband is not specified correctly";
        }

        // process layout:
        var internalLayout = (ignoreLayout || !input.hasOwnProperty(KEY_LAYOUT)) ? null
                : PedigreeImport._processLayout(input[KEY_LAYOUT], newG, memberIDToNodeID, relationshipIDToNodeID);

        PedigreeImport.validateBaseGraph(newG);

        return {"baseGraph": newG, "probandNodeID": probandID, "layout": internalLayout, "unlinkedMembers": unlinkedMembers};
    }

    PedigreeImport._processLayout = function(inputLayout, newG, memberIDToNodeID, relationshipIDToNodeID)
    {
        // one complication is the difference between internal and external formats: internally
        // each relationship has a hidden "child hub" node, which have no layout information in
        // the input - however it can be derived from relationship data, which this code does
        //
        // another complication is mult-generation ("long") edges, which go from person nodes to
        // relationships nodes across multiple generations ("ranks") - "drawing" one requires replacing
        // a graph edge between them with multiple edges and "virtual" nodes, one per intermediate rank

        // TODO: verify that all possible error kinds have been accounted for

        try {
            var ranks = PedigreeImport._generateRanksFromLayout(inputLayout, newG, memberIDToNodeID, relationshipIDToNodeID);
        } catch (err) {
            console.log("Error reading generations from layout: [" + err.message + "]");
            return null;
        }

        try {
            var suggestedLayout = PedigreeImport._generateOrdersAndPositionsFromLayout(inputLayout, ranks, newG, memberIDToNodeID, relationshipIDToNodeID);
        } catch (err) {
            console.log("Error reading positioning data from layout: [" + err.message + "]");
            // cancel long edge expansion
            newG.collapseMultiRankEdges();
            suggestedLayout = { "ranks": ranks, "order": null, "positions": null };
        }

        return suggestedLayout;
    }

    PedigreeImport._generateRanksFromLayout = function(inputLayout, newG, memberIDToNodeID, relationshipIDToNodeID)
    {
        var ranks = [];

        // use specified person ranks (generations)
        var members = inputLayout["members"];
        for (var memberID in memberIDToNodeID) {
            if (memberIDToNodeID.hasOwnProperty(memberID)) {
                var nodeID = memberIDToNodeID[memberID];
                if (members.hasOwnProperty(memberID)) {
                    ranks[nodeID] = members[memberID]["generation"];
                    if (!Helpers.isInt(ranks[nodeID])) {
                        throw "Generations should be integers";
                    }
                } else {
                    throw "No generation specified for member " + memberID;
                }
            }
        }

        // normalize ranks to 1 ... N, close any gaps, and adjust ranks so that persons
        // are ranked on every second rank (need a gap between generations to insert childhubs on their own rank)
        var rankCopy = Helpers.filterUnique(ranks);
        rankCopy.sort(function(a, b) { return a - b; });  // numeric sort
        var rankToAdjustedRank = {};
        for (var i = 0; i < rankCopy.length; i++) {
            rankToAdjustedRank[rankCopy[i]] = i*2 + 1;
        }
        for (var i = 0; i < ranks.length; i++) {
            ranks[i] = rankToAdjustedRank[ranks[i]];
        }

        // given adjusted person ranks, assign ranks to relationships and childhubs
        for (var relationshipID in relationshipIDToNodeID) {
            if (relationshipIDToNodeID.hasOwnProperty(relationshipID)) {
                var nodeID = relationshipIDToNodeID[relationshipID];
                var parents = newG.getParents(nodeID);                           // at this point guaranteed to be 2 parents by construction
                ranks[nodeID] = Math.max(ranks[parents[0]], ranks[parents[1]]);  // relationship should be on the same generatrion with "youngest" partner

                var childHubID = newG.getOutEdges(nodeID)[0]; // relationships have only one outedge
                ranks[childHubID] = ranks[nodeID] + 1;

                // validate that all children produced by this relationship are on the same rank and
                // the rank is 2 rankes below the relationship (which it should be according to construction and assumptions)
                var children = newG.getAllChildren(nodeID);
                for (var i = 0; i < children.length; i++) {
                    var childRank = ranks[children[i]];
                    if (childRank != ranks[nodeID] + 2) {
                        throw "Child generation is not greater than parent generation for relationship " + relationshipID;
                    }
                }
            }
        }

        return ranks;
    },

    PedigreeImport._generateOrdersAndPositionsFromLayout = function(inputLayout, initialRanks, newG, memberIDToNodeID, relationshipIDToNodeID)
    {
        // Note: if inputLayout is missing some of the required fields the code below may throw when attempting to
        //       access that data - the calling code will know it needs to discard the layout in that case

        var ranks = initialRanks.slice();
        var vOrder = [];
        var positions = [];

        var setOrderAndPosition = function(id, nodeID, data) {
            vOrder[nodeID] = data[id]["order"];
            if (!Helpers.isInt(vOrder[nodeID])) {
                throw "Orders should be integers";
            }
            if (vOrder[nodeID] < 0) {
                throw "Orders should be positive integers starting from 1";
            }
            if (positions && data[id].hasOwnProperty("x") && Helpers.isInt(data[id]["x"]) ) {
                positions[nodeID] = data[id]["x"];
            } else {
                positions = null;
            }
        }

        var members = inputLayout["members"];
        for (var memberID in memberIDToNodeID) {
            if (memberIDToNodeID.hasOwnProperty(memberID)) {
                var nodeID = memberIDToNodeID[memberID];
                setOrderAndPosition(memberID, nodeID, members);
            }
        }

        var relationships = inputLayout["relationships"];
        var longedges     = inputLayout["longedges"];
        for (var relationshipID in relationshipIDToNodeID) {
            if (relationshipIDToNodeID.hasOwnProperty(relationshipID)) {
                var relNodeID = relationshipIDToNodeID[relationshipID];
                setOrderAndPosition(relationshipID, relNodeID, relationships);

                // set order/positions for childhub. It is ok to have gaps in orders for childhubs,
                // Ordering.createOrdering() will take care of those
                var childhubID = newG.getOutEdges(relNodeID)[0]; // relationships have only one outedge
                vOrder[childhubID] = vOrder[relNodeID];
                positions && (positions[childhubID] = positions[relNodeID]);

                // check if there are multi-generation ("long") edges and try to collect positioning data for them if
                // long-edge data is available. If there is a long edge and ther eis no data => discard layout
                var parents = newG.getParents(relNodeID);
                if (ranks[parents[0]] != ranks[parents[1]]) {
                    // multi-rank edge detected
                    var longEdgeParent = (ranks[parents[0]] > ranks[parents[1]]) ? parents[1] : parents[0];
                    var rankDifference = ranks[relNodeID] - ranks[longEdgeParent];

                    if (!longedges || !longedges.hasOwnProperty(relationshipID)
                        || !longedges[relationshipID].hasOwnProperty("member")
                        || memberIDToNodeID[longedges[relationshipID].member] != longEdgeParent
                        || !longedges[relationshipID].hasOwnProperty("path")) {
                        throw "There is a relationship connecting persons of different generations but the provided " +
                        "layout does not have enough data to draw this connection: discarding layout";
                    }

                    //-----------------------------------------------------------------------------------
                    // FIXME: logic below is valid, but need to make sure this assumption does not break
                    //        if/when new long-edge laying algorithm is implemented:
                    // There is a difficulty ordering long edges, since they go through childhub ranks,
                    // but ordering of childhubs is lost in the input format (we only know they are ordered
                    // in the same order as their relationships). However in the current long edge laying
                    // algorithm long edge is always going straight verticaly through childhub ranks, so
                    // since relationship order is used for childhubs, we can use the order from the previous
                    // segment of long edges to order long egde segments passing through chidhub ranks
                    //-----------------------------------------------------------------------------------

                    var path = longedges[relationshipID].path;
                    if (path.length != (rankDifference + 1)) {
                        throw "The provided long edge path has incorrect length " + path.length + ", expected " + (rankDifference + 1);
                    }

                    newG.removeEdge(longEdgeParent, relNodeID);
                    var prevV = longEdgeParent;
                    for (var p = 0; p < path.length; p++) {
                        var nextV = newG._addVertex( null, BaseGraph.TYPE.VIRTUALEDGE, {}, {});
                        newG.addEdge( prevV, nextV );
                        ranks[nextV] = ranks[longEdgeParent] + p;
                        positions[nextV] = path[p].x;

                        // now comes the complication: while order of vertices on person+relationship rank is
                        // known, the order of childhubs is lost and may be different from what is stored;
                        // thus for those ranks we'll use the same order previou segment used, since this is
                        // the same logic we use for ordering new chldhubs, thus there should be no conflicts
                        if (ranks[nextV] % 2 != 0) {
                            // person/relationship rank
                            vOrder[nextV] = path[p].order;
                        } else {
                            // childhub rank
                            vOrder[nextV] = path[p - 1].order;
                        }
                        prevV = nextV;
                    }
                    newG.addEdge(prevV, relNodeID);
                }
            }
        }

        return { "ranks": ranks, "order": Ordering.createOrdering(vOrder, ranks), "positions": positions };
    },


    /* ===============================================================================================
     *
     * Creates and returns a BaseGraph from a text string in the PED/LINKAGE format.
     *
     * PED format:
     * (from http://pngu.mgh.harvard.edu/~purcell/plink/data.shtml#ped)
     *   Family ID
     *   Individual ID
     *   Paternal ID
     *   Maternal ID
     *   Sex (1=male; 2=female; other=unknown)
     *   Phenotype
     *
     *   Phenotype, by default, should be coded as:
     *      -9 missing
     *       0 missing
     *       1 unaffected
     *       2 affected
     *
     * =================
     *
     * LINKAGE format:
     * (from http://www.helsinki.fi/~tsjuntun/autogscan/pedigreefile.html)
     *
     *   Column 1:   Pedigree number
     *   Column 2:   Individual ID number
     *   Column 3:   ID of father
     *   Column 4:   ID of mother
     *   Column 5:   First offspring ID
     *   Column 6:   Next paternal sibling ID
     *   Column 7:   Next maternal sibling ID
     *   Column 8:   Sex
     *   Column 9:   Proband status (1=proband, higher numbers indicate doubled individuals formed
     *                               in breaking loops. All other individuals have a 0 in this field.)
     *   Column 10+: Disease and marker phenotypes (as in the original pedigree file)
     * ===============================================================================================
     */
    PedigreeImport.initFromPED = function(inputText, acceptOtherPhenotypes, markEvaluated, saveIDAsExternalID, affectedCodeOne, disorderNames)
    {
        var inputLines = inputText.match(/[^\r\n]+/g);
        if (inputLines.length == 0) {
            throw "Unable to import: no data";
        }

        // autodetect if data is in pre-makeped or post-makeped format
        var postMakeped = false;
        if (inputLines[0].indexOf("Ped:") > 0 && inputLines[0].indexOf("Per:") > 0)
            postMakeped = true;

        var familyPrefix = "";

        var newG = new BaseGraph();

        var nameToId = {};

        var phenotypeValues = {};  // set of all posible valuesin the phenotype column

        var extendedPhenotypesFound = false;

        // support both automatic and user-defined assignment of proband
        var nextID = postMakeped ? 1 : 0;

        // first pass: add all vertices and assign vertex IDs
        for (var i = 0; i < inputLines.length; i++) {
            if (inputLines[i].charAt(0) == '#') {
                continue;
            }

            inputLines[i] = inputLines[i].replace(/[^a-zA-Z0-9_.\-\s*]/g, '');
            inputLines[i] = inputLines[i].replace(/^\s+|\s+$/g, '');  // trim()

            var parts = inputLines[i].split(/\s+/);
            //console.log("Parts: " + Helpers.stringifyObject(parts));

            if (parts.length < 6 || (postMakeped && parts.length < 10)) {
                throw "Input line has not enough columns: [" + inputLines[i] + "]";
            }

            if (familyPrefix == "") {
                familyPrefix = parts[0];
            } else {
                if (parts[0] != familyPrefix) {
                    throw "Unsupported feature: multiple families detected within the same pedigree";
                }
            }

            var pedID = parts[1];
            if (nameToId.hasOwnProperty(pedID)) {
                throw "Multiple persons with the same ID [" + pedID + "]";
            }

            var genderValue = postMakeped ? parts[7] : parts[4];
            var gender = "U";
            if (genderValue == 1) {
                gender = "M";
            } else if (genderValue == 2) {
                gender = "F";
            }
            var properties = {"gender": gender};

            if (saveIDAsExternalID) {
                properties["externalID"] = pedID;
            }

            var useID = (postMakeped && parts[8] == 1) ? 0 : nextID++;
            if (i == inputLines.length-1 && newG.v[0] === undefined) {
                // last node and no node with id 0 yet
                useID = 0;
            }

            var pedigreeID = newG._addVertex( useID, BaseGraph.TYPE.PERSON, properties, null );

            nameToId[pedID] = pedigreeID;

            var phenotype = postMakeped ? parts[9] : parts[5];
            phenotypeValues[phenotype] = true;
            if (acceptOtherPhenotypes && phenotype != "-9" && phenotype != "0" && phenotype != "1" && phenotype != "2") {
                extendedPhenotypesFound = true;
            }
        }

        // There are two popular schemes for the phenotype column (-9/0/1/2 or -9/0/1).
        // Use the "standard" by default, unless directed to use the other one by the user
        if (affectedCodeOne) {
            if (extendedPhenotypesFound || phenotypeValues.hasOwnProperty("2")) {
                throw "Phenotypes with codes other than 0 or 1 were found";
            }
            var affectedValues   = { "1":  true };
            var missingValues    = { "-9": true };
            var unaffectedValues = { "0":  true };
        } else {
            var affectedValues   = { "2": true };
            var missingValues    = { "0": true, "-9": true };
            var unaffectedValues = { "1": true };
        }

        if (!disorderNames) {
            disorderNames = {};
            if (extendedPhenotypesFound) {
                for (var phenotype in phenotypeValues)
                    if (phenotypeValues.hasOwnProperty(phenotype)) {
                        if (phenotype != "-9" && phenotype != "0" && phenotype != "1") {
                            disorderNames[phenotype]  = "affected (phenotype " + phenotype + ")";
                            affectedValues[phenotype] = true;
                        }
                    }
            }
        }

        var relationshipTracker = new RelationshipTracker(newG);

        // second pass (once all vertex IDs are known): process edges
        for (var i = 0; i < inputLines.length; i++) {
            if (inputLines[i].charAt(0) == '#') {
                continue;
            }

            var parts = inputLines[i].split(/\s+/);

            var thisPersonName = parts[1];
            var id = nameToId[thisPersonName];

            var phenotype = postMakeped ? parts[9] : parts[5];
            if (affectedValues.hasOwnProperty(phenotype)) {
                var disorder = disorderNames.hasOwnProperty(phenotype) ? disorderNames[phenotype] : "affected";
                newG.properties[id]["carrierStatus"] = 'affected';
                newG.properties[id]["disorders"]     = [disorder];
                if (markEvaluated) {
                    newG.properties[id]["evaluated"] = true;
                }
            } else if (unaffectedValues.hasOwnProperty(phenotype)) {
                newG.properties[id]["carrierStatus"] = '';
                if (markEvaluated) {
                    newG.properties[id]["evaluated"] = true;
                }
            } else if (!missingValues.hasOwnProperty(phenotype)) {
                //treat all unsupported values as "unknown/no evaluation"
                //throw "Individual with ID [" + thisPersonName + "] has unsupported phenotype value [" + phenotype + "]";
            }

            // check if parents are given for this individual; if at least one parent is given,
            // check if the corresponding relationship has already been created. If not, create it. If yes,
            // add an edge from childhub to this person

            var fatherID = parts[2];
            var motherID = parts[3];

            if (fatherID == 0 && motherID == 0) continue;

            // .PED supports specifying only mohter of father. Pedigree editor requires both (for now).
            // So create a virtual parent in case one of the parents is missing
            if (fatherID == 0) {
               fatherID = newG._addVertex( null, BaseGraph.TYPE.PERSON, {"gender": "M", "comments": "unknown"}, null );
            } else {
                fatherID = nameToId[fatherID];
                if (typeof fatherID === 'undefined') {
                    throw "Unable to import pedigree: incorrect father link on line " + (i+1) + "; Maybe import data is not in PED format?";
                }
            }
            if (motherID == 0) {
                motherID = newG._addVertex( null, BaseGraph.TYPE.PERSON, {"gender": "F", "comments": "unknown"}, null );
            } else {
                motherID = nameToId[motherID];
                if (typeof motherID === 'undefined') {
                    throw "Unable to import pedigree: incorrect mother link on line " + (i+1) + "; Maybe import data is not in PED format?";
                }
            }

            // both motherID and fatherID are now given and represent valid existing nodes in the pedigree

            // if there is a relationship between motherID and fatherID the corresponding childhub is returned
            // if there is no relationship, a new one is created together with the chldhub
            var chhubID = relationshipTracker.createOrGetChildhub(motherID, fatherID);

            newG.addEdge( chhubID, id );
        }

        PedigreeImport.validateBaseGraph(newG);

        return {"baseGraph": newG, "probandNodeID": 0};
    }


    /* ===============================================================================================
     *
     * Creates and returns a BaseGraph from a text string in the BOADICEA format.
     *
     *  BOADICEA format:
     *  (from https://pluto.srl.cam.ac.uk/bd3/v3/docs/BWA_v3_user_guide.pdf)
     *
     *  line1: BOADICEA import pedigree file format 2.0
     *  line2: column titles
     *  line3+: one patient per line, with values separated by spaces or tabs, as follows:
     *
     *   FamID: Family/pedigree ID, character string (maximum 13 characters)
     *   Name: First name/ID of the family member, character string (maximum 8 characters)
     *   Target: The family member for whom the BOADICEA risk calculation is made, 1 = target for BOADICEA risk calculation, 0 = other family members. There must only be one BOADICEA target individual.
     *   IndivID: Unique ID of the family member, character string (maximum 7 characters)
     *   FathID: Unique ID of their father, 0 = no father, or character string (maximum 7 characters)
     *   MothID: Unique ID of their mother, 0 = unspecified, or character string (maximum 7 characters)
     *   Sex: M or F
     *   Twin: Identical twins, 0 = no identical twin, any non-zero character = twin.
     *   Dead: The current status of the family member, 0 = alive, 1 = dead
     *   Age: Age at last follow up, 0 = unspecified, integer = age at last follow up
     *   Yob: Year of birth, 0 = unspecified, or integer (consistent with Age if the person is alive)
     *   1BrCa: Age at first breast cancer diagnosis, 0 = unaffected, integer = age at diagnosis, AU = unknown age at diagnosis (affected, age unknown)
     *   2BrCa: Age at contralateral breast cancer diagnosis, 0 = unaffected, integer = age at diagnosis, AU = unknown age at diagnosis (affected, age unknown)
     *   OvCa: Age at ovarian cancer diagnosis, 0 = unaffected, integer = age at diagnosis, AU = unknown age at diagnosis (affected, age unknown)
     *   ProCa: Age at prostate cancer diagnosis 0 = unaffected, integer = age at diagnosis, AU = unknown age at diagnosis (affected, age unknown)
     *   PanCa: Age at pancreatic cancer diagnosis 0 = unaffected, integer = age at diagnosis, AU = unknown age at diagnosis (affected, age unknown)
     *   Gtest: Genetic test status, 0 = untested, S = mutation search, T = direct gene test
     *   Mutn: 0 = untested, N = no mutation, 1 = BRCA1 positive, 2 = BRCA2 positive, 3 = BRCA1 and BRCA2 positive
     *   Ashkn: 0 = not Ashkenazi, 1 = Ashkenazi
     *   ER: Estrogen receptor status, 0 = unspecified, N = negative, P = positive
     *   PR: Progestrogen receptor status, 0 = unspecified, N = negative, P = positive
     *   HER2: Human epidermal growth factor receptor 2 status, 0 = unspecified, N = negative, P = positive
     *   CK14: Cytokeratin 14 status, 0 = unspecified, N = negative, P = positive
     *   CK56: Cytokeratin 56 status, 0 = unspecified, N = negative, P = positive
     * ===============================================================================================
     */
    PedigreeImport.initFromBOADICEA = function(inputText, saveIDAsExternalID)
    {
        var inputLines = inputText.match(/[^\r\n]+/g);

        if (inputLines.length <= 2) {
            throw "Unable to import: no data";
        }
        if (inputLines[0].match(/^BOADICEA import pedigree file format 2/i) === null) {
            throw "Unable to import: unsupported version of the BOADICEA format";
        }
        inputLines.splice(0,2); // remove 2 header lines

        var familyPrefix = "";

        var newG = new BaseGraph();

        var nameToId = {};

        var nextID = 1;

        // first pass: add all vertices and assign vertex IDs
        for (var i = 0; i < inputLines.length; i++) {

            inputLines[i] = inputLines[i].replace(/[^a-zA-Z0-9_.\-\s*]/g, ' ');
            inputLines[i] = inputLines[i].replace(/^\s+|\s+$/g, '');  // trim()

            var parts = inputLines[i].split(/\s+/);
            //console.log("Parts: " + Helpers.stringifyObject(parts));

            if (parts.length < 24) {
                throw "Input line has not enough columns: [" + inputLines[i] + "]";
            }

            if (familyPrefix == "") {
                familyPrefix = parts[0];
            } else {
                if (parts[0] != familyPrefix) {
                    throw "Unsupported feature: multiple families detected within the same pedigree";
                }
            }

            var extID = parts[3];
            if (nameToId.hasOwnProperty(extID)) {
                throw "Multiple persons with the same ID [" + extID + "]";
            }

            var genderValue = parts[6];
            var gender = "M";
            if (genderValue == "F") {
              gender = "F";
            }
            var name = parts[1];
            if (Helpers.isInt(name)) {
              name = "";
            }
            var properties = {"gender": gender, "fName": name};

            if (saveIDAsExternalID) {
              properties["externalID"] = extID;
            }

            var deadStatus = parts[8];
            if (deadStatus == "1") {
              properties["lifeStatus"] = "deceased";
            }

            var yob = parts[10];
            if (yob != "0" && Helpers.isInt(yob)) {
              properties["dob"] = {"year": parseInt(yob)};
            }

            // TODO: handle all the columns and proper cancer handling
            //
            // 11: 1BrCa: Age at first breast cancer diagnosis, 0 = unaffected, integer = age at diagnosis, AU = unknown age at diagnosis (affected unknown)
            // 12: 2BrCa: Age at contralateral breast cancer diagnosis, 0 = unaffected, integer = age at diagnosis, AU = unknown age at diagnosis (affected unknown)
            // 13: OvCa:  Age at ovarian cancer diagnosis, 0 = unaffected, integer = age at diagnosis, AU = unknown age at diagnosis (affected unknown)
            // 14: ProCa: Age at prostate cancer diagnosis 0 = unaffected, integer = age at diagnosis, AU = unknown age at diagnosis (affected unknown)
            // 15: PanCa: Age at pancreatic cancer diagnosis 0 = unaffected, integer = age at diagnosis, AU = unknown age at diagnosis (affected unknown)
            var cancers = [ { "column": 11, "cancer": {"id":"HP:0100013", "label":"Breast"} },
                            { "column": 12, "addQualifierToPreviousCancer": true,
                                            "qualifier": {"laterality":"bi", "primary":true, "notes": "Contralateral cancer (from BOADICEA)"} },
                            { "column": 13, "cancer": {"id":"HP:0100615", "label":"Ovarian"} },
                            { "column": 14, "cancer": {"id":"HP:0100787", "label":"Prostate"} },
                            { "column": 15, "cancer": {"id":"HP:0002894", "label":"Pancreatic"} } ];

            properties["cancers"] = [];

            for (var c = 0; c < cancers.length; c++) {
                var nextCancer = cancers[c];
                var age = parts[nextCancer["column"]];

                if (age != "0") {

                  if (nextCancer.addQualifierToPreviousCancer) {
                      var cancerData = properties["cancers"].pop();
                  } else {
                      var cancerData = nextCancer["cancer"];
                      cancerData.affected = true;
                      cancerData.qualifiers = [];
                  }

                  var qualifier = nextCancer.qualifier ? nextCancer.qualifier : { "laterality": "", "primary": true, "notes": "(from BOADICEA)" };

                  if (Helpers.isInt(age)) {
                      var numericAge = parseInt(age);
                      if (numericAge > 100) {
                          age = "after_100";
                      }
                      qualifier["numericAgeAtDiagnosis"] = numericAge;
                      qualifier["ageAtDiagnosis"] = age.toString();
                  } else {
                      qualifier["numericAgeAtDiagnosis"] = "";
                      qualifier["ageAtDiagnosis"] = "";
                  }

                  cancerData.qualifiers.push(qualifier);
                  properties["cancers"].push(cancerData);
                }
            }

            // Mutn: 0 = untested, N = no mutation, 1 = BRCA1 positive, 2 = BRCA2 positive, 3 = BRCA1 and BRCA2 positive
            var mutations = parts[17];
            if (mutations == "1" || mutations == "2" || mutations == "3") {
                properties["genes"] = [];
                if (mutations == 1 || mutations == 3) {
                    properties["genes"].push({"gene": "BRCA1", "status": "solved", "comment": "BOADICEA import"});
                }
                if (mutations == 2 || mutations == 3) {
                    properties["genes"].push({"gene": "BRCA2", "status": "solved", "comment": "BOADICEA import"});
                }
            } else if (mutations == "N") {
                properties["genes"].push({"gene": "BRCA1", "status": "rejected", "comment": "BOADICEA import"});
                properties["genes"].push({"gene": "BRCA2", "status": "rejected", "comment": "BOADICEA import"});
            }

            var ashkenazi = parts[18];
            if (ashkenazi != "0") {
              properties["ethnicities"] = ["Ashkenazi Jews"];
            }

            var proband = (parts[2] == 1);
            var useID = proband ? 0 : nextID++;
            if (i == inputLines.length-1 && newG.v[0] === undefined) {
              // last node and no proband yet
              useID = 0;
            }

            var pedigreeID = newG._addVertex( useID, BaseGraph.TYPE.PERSON, properties, null );

            nameToId[extID] = pedigreeID;
        }

        var relationshipTracker = new RelationshipTracker(newG);

        // second pass (once all vertex IDs are known): process edges
        for (var i = 0; i < inputLines.length; i++) {
          var parts = inputLines[i].split(/\s+/);

          var extID = parts[3];
          var id    = nameToId[extID];

          // check if parents are given for this individual; if at least one parent is given,
          // check if the corresponding relationship has already been created. If not, create it. If yes,
          // add an edge from childhub to this person

          var fatherID = parts[4];
          var motherID = parts[5];

          if (fatherID == 0 && motherID == 0) {
            continue;
          }

          // .PED supports specifying only mother or father. Pedigree editor requires both (for now).
          // So create a virtual parent in case one of the parents is missing
          if (fatherID == 0) {
           fatherID = newG._addVertex( null, BaseGraph.TYPE.PERSON, {"gender": "M", "comments": "unknown"}, null );
          } else {
            fatherID = nameToId[fatherID];
          }
          if (motherID == 0) {
            motherID = newG._addVertex( null, BaseGraph.TYPE.PERSON, {"gender": "F", "comments": "unknown"}, null );
          } else {
            motherID = nameToId[motherID];
          }

          // both motherID and fatherID are now given and represent valid existing nodes in the pedigree

          // if there is a relationship between motherID and fatherID the corresponding childhub is returned
          // if there is no relationship, a new one is created together with the childhub
          var chhubID = relationshipTracker.createOrGetChildhub(motherID, fatherID);

          newG.addEdge( chhubID, id );
        }

        PedigreeImport.validateBaseGraph(newG);

        return {"baseGraph": newG, "probandNodeID": 0};
    }

    /* ===============================================================================================
     *
     * Validates the generated basegraph and throws one of the following exceptions:
     *
     *  1) "Unsupported pedigree: some components of the imported pedigree are disconnected from each other"
     *  2) "Unable to import pedigree"
     *
     * The method is a wrapper around the internal vlaidate method, which may throw many exceptions
     * which change from version to version
     *
     * ===============================================================================================
     */
    PedigreeImport.validateBaseGraph = function(newG)
    {
        try {
            newG.validate();
        } catch( err) {
            if (err.indexOf("disconnected component")) {
                throw "Unsupported pedigree: some components of the imported pedigree are disconnected from each other";
            } else {
                throw "Unable to import pedigree";
            }
        }
    }

    /* ===============================================================================================
     *
     * Creates and returns a BaseGraph from a text string in the "simple JSON" format.
     *
     *  Simple JSON format: an array of objects, each object representing one person or one relationship, e.g.:
     *
     *    [ { "name": "f11", "sex": "female", "lifeStatus": "deceased" },
     *      { "name": "m11", "sex": "male" },
     *      { "name": "f12", "sex": "female", "disorders": [603235, "142763", "custom disorder"] },
     *      { "name": "m12", "sex": "male" },
     *      { "name": "m21", "sex": "male", "mother": "f11", "father": "m11" },
     *      { "name": "f21", "sex": "female", "mother": "f12", "father": "m12" },
     *      { "name": "ch1", "sex": "female", "mother": "f21", "father": "m21", "disorders": [603235], "proband": true },
     *      { "name": "m22", "sex": "male" },
     *      { "relationshipId": 1, "partner1": "f21", "partner2": "m22"} ]
     *
     *  Supported properties for person nodes:
     *   - "id": string or number (default: none). If two nodes with the same ID are found an error is reported.
     *           If present, this id is used only for the purpose of linking nodes to each other and is not recorded
     *           in the imported pedigree. Use "externalId" if an ID should be stored
     *   - "proband": boolean (default: true for the first object, false for all other objects. If another object
     *                                  is explicitly indicated as a proband, the firs tobject also defaults to false.
     *                                  If more than one node is indicated as a proband only the first one is considered ot be one)
     *   - "name" or "firstName": string (default: none). if both are defined "firstName" is used as "first name" and name is used for mother/father reference checks only
     *   - "lastName": string (default: none)
     *   - "lastNameAtBirth": string (default: none)
     *   - "comments": string (default: none)
     *   - "externalId": string (default: none)
     *   - "sex": one of "male" or "m", "female" or "f", "other" or "o", "unknown" or "u" (default: "unknown")
     *   - "features": array of objects, each representing one standard (documented in an ontology and with a proper ID) phenotype in PhenoTips JSON format
     *   - "nonstandard_features": array of objects, each representing one custom (user-defined free text) phenotype in PhenoTips JSON format
     *   - "genes": array of objects, each representing information about one gene in PhenoTips JSON format
     *   - "twinGroup": integer. All children of the sam eparents with the same twin group are considered twins. (fefault: none)
     *   - "monozygotic": boolean. (only applicable for twins)
     *   - "adoptedIn": boolean (default: false)
     *   - "evaluated": boolean (default: false)
     *   - "birthDate": string (default: none)
     *   - "deathDate": string (default: none)
     *   - "nodeNumber": string (default: none) pedigree node number as of last renumbering
     *   - "lostContact": boolean (default: false) "false" if proband lost contact with the given individual
     *   - "numPersons": integer. When present and not 0 this individual is treated as a "person group"
     *   - "lifeStatus": one of {"alive", "deceased", "aborted", "miscarriage", "stillborn", "unborn"}.
     *                   (default: "alive". If death date is given status defaults to "deceased" and overwrites
     *                             the explicitly given status if it were "alive")
     *   - "aliveandwell": boolean (default: flase), indicated whether individual is alive and well
     *   - "deceasedAge": string (default: none) the age of death
     *   - "deceasedCause": string (default: none) the cause of death
     *   - "disorders": array of strings or integers (a string representing an integer is considered to be an integer), integers treated as OMIM IDs. (default: none)
     *   - "carrierStatus": one of {'', 'carrier', 'affected', 'presymptomatic'}
     *                      (default: if a disorder is given, default is 'affected', otherwise: none.
     *                       also, if a disorder is given and status is explicitly '', it is automatically changed to 'affected')
     *   - "childlessStatus": one of {none, 'childless','infertile'} (default: none)
     *   - "childlessReason": string or null (default: none)
     *   - "mother" and "father": string, a reference to another node given in the JSON.
     *                            First a match versus an existing ID is checked, if not found a check against "externalId",
     *                            if not found a check against "name" and finally "firstName".
     *                            If one of the parents is given and the other one is not a virtual new node is created
     *   - "phenotipsId": The id of the PhenoTips document this node is linked to (default: none)
     *
     *  Supported properties for relationship nodes:
     *   - "relationshipId": string or number. The valu eis not used, only required ot indicate that this
     *                       entry represents a relationship.
     *   - "separated": boolean (default: false)
     *   - "consanguinity": one of {"Y","N"} (default: none). If not given it is computed automatically based ont the family graph
     *   - "childlessStatus": one of {none, 'childless','infertile'} (default: none). If there are no children with the same
     *                        set of parents, "childlessStatus" is automatically set to "childless".
     *   - "childlessReason": string or null (default: none)
     *   - "partner1" and "partner2": string, a reference to another node given in the JSON.
     *                                Used in the same way "mother" and "father" are used for person nodes.
     *
     *   Each person node should have at least one of {"id", "externalId", "name", "firstName"} defined,
     *   each relationship node should have "relationshipId", "mother" and "father".
     *
     *   Relationship nodes should only be included to indicate a relationship without children (as a way
     *   to indicate there is a link) or to spcify relationship properties, if different from default.
     * ===============================================================================================
     */
    PedigreeImport.initFromSimpleJSON = function(inputText)
    {
       try {
           var inputArray = JSON.parse(inputText);
       } catch(err) {
           throw "Unable to import pedigree: input is not a valid JSON string: " + err.message;
       }

       if (typeof inputArray != 'object' || Object.prototype.toString.call(inputArray) !== '[object Array]') {
           throw "Unable to import pedigree: JSON does not represent an array of objects";
       }
       if (inputArray.length == 0) {
           throw "Unable to import pedigree: input is empty";
       }

       var probandID = null;

       var newG = new BaseGraph();

       var nameToID            = {};
       var externalIDToID      = {};
       var ambiguousReferences = {};
       var hasID               = {}

       // TODO: fix once family studies are merged in
       // Detect proband node, and make it node with id = 0
       // Technical detail: all person nodes will have their IDs equal to their sequence number (among personnodes),
       // except proband node, that will have ID 0, and first person node, which will have proband's sequence ##.
       // If proband is not explicitly defined, the first node found will be considered to be the proband, which
       // is consistent with the way old versions generated simpole JSONs.
       var newPersonIDs = [];
       var personSeqNumber = 0;
       for (var i = 0; i < inputArray.length; i++) {
           if (inputArray[i].hasOwnProperty("relationshipId")) {
               continue;
           }
          if (inputArray[i].hasOwnProperty("proband")) {
              newPersonIDs.push(0);
              newPersonIDs[0] = personSeqNumber;
          } else {
              newPersonIDs.push(personSeqNumber);
          }
          personSeqNumber++;
       }

       // first pass: add all persons and assign pedigree node IDs to all persons
       var personSeqNumber = 0;
       for (var i = 0; i < inputArray.length; i++) {
           if (inputArray[i].hasOwnProperty("relationshipId")) {
               continue;
           }

           var nextPerson = inputArray[i];

           if (typeof nextPerson != 'object') {
               throw "Unable to import pedigree: JSON does not represent an array of objects";
           }

           if (!nextPerson.hasOwnProperty("id") && !nextPerson.hasOwnProperty("name") &&
               !nextPerson.hasOwnProperty("firstName") && !nextPerson.hasOwnProperty("externalId") ) {
               throw "Unable to import pedigree: a node with no ID or name is found";
           }

           var pedigreeID = newG._addVertex( newPersonIDs[personSeqNumber], BaseGraph.TYPE.PERSON, {}, null );
           personSeqNumber++;

           var properties = {};
           properties["gender"] = "U";     // each person should have some gender set

           for (var property in nextPerson) {
               if (nextPerson.hasOwnProperty(property)) {
                   var value    = nextPerson[property];
                   var property = property.toLowerCase();

                   if (property == "mother" || property == "father")  // those are processed on the second pass
                       continue;

                   if (property == "proband" && probandID === null) {
                       probandID = pedigreeID;
                   }
                   if (property == "sex") {
                       var genderString = value.toLowerCase();
                       if( genderString == "female" || genderString == "f")
                           properties["gender"] = "F";
                       else if( genderString == "male" || genderString == "m")
                           properties["gender"] = "M";
                       else if( genderString == "other" || genderString == "o")
                           properties["gender"] = "O";
                   } else if (property == "id") {
                       if (externalIDToID.hasOwnProperty(value)) {
                           throw "Unable to import pedigree: multiple persons with the same ID [" + value + "]";
                       }
                       if (nameToID.hasOwnProperty(value) && nameToID[value] != pedigreeID) {
                           delete nameToID[value];
                           ambiguousReferences[value] = true;
                       } else {
                           externalIDToID[value] = pedigreeID;
                           hasID[pedigreeID] = true;
                       }
                   } else if (property == "name" || property == "firstname" ) {
                       properties["fName"] = value;
                       if (nameToID.hasOwnProperty(value) && nameToID[value] != pedigreeID) {
                           // multiple nodes have this first name
                           delete nameToID[value];
                           ambiguousReferences[value] = true;
                       } else if (externalIDToID.hasOwnProperty(value) && externalIDToID[value] != pedigreeID) {
                           // some other node has this name as an ID
                           delete externalIDToID[value];
                           ambiguousReferences[value] = true;
                       } else {
                           nameToID[value] = pedigreeID;
                       }
                   } else {
                       var processed = PedigreeImport.convertProperty(property, value);
                       // one input (external) property may be represented by multiple internal properties
                       // if the property is not supported an empty array is returned, and no data gets transferred
                       for (var p = 0; p < processed.length; p++) {
                           properties[processed[p].propertyName] = processed[p].value;
                       }
                   }
               }
           }

           // only use externalID if id is not present
           if (nextPerson.hasOwnProperty("externalId") && !hasID.hasOwnProperty(pedigreeID)) {
               externalIDToID[nextPerson.externalId] = pedigreeID;
               hasID[pedigreeID] = true;
           }

           newG.properties[pedigreeID] = properties;
       }

       var getPersonID = function(person) {
           if (person.hasOwnProperty("id"))
               return externalIDToID[person.id];

           if (person.hasOwnProperty("firstName"))
               return nameToID[person.firstName];

           if (person.hasOwnProperty("name"))
               return nameToID[person.name];
       };

       var findReferencedPerson = function(reference, refType) {
           if (ambiguousReferences.hasOwnProperty(reference))
               throw "Unable to import pedigree: ambiguous reference to [" + reference + "]";

           if (externalIDToID.hasOwnProperty(reference))
               return externalIDToID[reference];

           if (nameToID.hasOwnProperty(reference))
               return nameToID[reference];

           throw "Unable to import pedigree: [" + reference + "] is not a valid " + refType + " reference (does not correspond to a name or an ID of another person)";
       };

       var relationshipTracker = new RelationshipTracker(newG);

       // second pass (once all person IDs are known): process parents/children & add edges
       for (var i = 0; i < inputArray.length; i++) {
           if (inputArray[i].hasOwnProperty("relationshipId")) {
               continue;
           }

           var nextPerson = inputArray[i];

           var personID = getPersonID(nextPerson);

           var motherLink = nextPerson.hasOwnProperty("mother") ? nextPerson["mother"] : null;
           var fatherLink = nextPerson.hasOwnProperty("father") ? nextPerson["father"] : null;

           if (motherLink == null && fatherLink == null)
               continue;

           // create a virtual parent in case one of the parents is missing
           if (fatherLink == null) {
               var fatherID = newG._addVertex( null, BaseGraph.TYPE.PERSON, {"gender": "M", "comments": "unknown"}, null );
           } else {
               var fatherID = findReferencedPerson(fatherLink, "father");
           }
           if (motherLink == null) {
               var motherID = newG._addVertex( null, BaseGraph.TYPE.PERSON, {"gender": "F", "comments": "unknown"}, null );
           } else {
               var motherID = findReferencedPerson(motherLink, "mother");
           }

           if (fatherID == personID || motherID == personID)
               throw "Unable to import pedigree: a person is declared to be his or hew own parent";

           // both motherID and fatherID are now given and represent valid existing nodes in the pedigree

           // if there is a relationship between motherID and fatherID the corresponding childhub is returned
           // if there is no relationship, a new one is created together with the chldhub
           var chhubID = relationshipTracker.createOrGetChildhub(motherID, fatherID);

           newG.addEdge( chhubID, personID );
       }

       // finally, go over relationships specified in the input, and either create those not yet created
       // (because they are childless) or set properties for those already created
       for (var i = 0; i < inputArray.length; i++) {
           if (!inputArray[i].hasOwnProperty("relationshipId")) {
               continue;
           }
           var nextRelationship = inputArray[i];

           var partnerLink1 = nextRelationship.hasOwnProperty("partner1") ? nextRelationship["partner1"] : null;
           var partnerLink2 = nextRelationship.hasOwnProperty("partner2") ? nextRelationship["partner2"] : null;

           if (partnerLink1 == null || partnerLink2 == null) {
               throw "Unable to import pedigree: a relationship has only one partner specified";
           }

           var partnerID1 = findReferencedPerson(partnerLink1);
           var partnerID2 = findReferencedPerson(partnerLink2);

           // get or create a relationship
           var chhubID = relationshipTracker.createOrGetChildhub(partnerID1, partnerID2);

           // if there are no children, create a placeholder child
           if (newG.getOutEdges(chhubID).length == 0) {
               var placeholderID = newG._addVertex(null, BaseGraph.TYPE.PERSON, {"gender": "U", "placeholder":true}, null );
               newG.addEdge( chhubID, placeholderID );
           }

           // set relationship properties
           var relationshipID = relationshipTracker.getRelationshipIDForChildHub(chhubID);
           for (var property in nextRelationship) {
               if (nextRelationship.hasOwnProperty(property)) {
                   var value    = nextRelationship[property];
                   var property = property.toLowerCase();

                   if (property == "partner1" || property == "partner2")  // those are already processed
                       continue;

                   var processed = PedigreeImport.convertRelationshipProperty(property, value);
                   if (processed !== null) {
                       // supported property
                       newG.properties[relationshipID][processed.propertyName] = processed.value;
                   }
               }
           }
       }

       PedigreeImport.validateBaseGraph(newG);

       if (probandID === null) {
           probandID = 0; // default to 0 if not defined explicitly
       }

       return {"baseGraph": newG, "probandNodeID": probandID};
    }


    /* ===============================================================================================
     *
     * GEDCOM file format: http://en.wikipedia.org/wiki/GEDCOM
     *
     * Supported individual (INDI) properties: NAME, SEX, NOTE, ADOP, BIRT, DEAT and DATE
     *  - Non-standard "_GENSTAT" is partially supported (for compatibility with Cyrillic v3)
     *  - Non-standard "_MAIDEN", "_INFO" and "_COMMENT" are supported (for compatibility with Cyrillic v3)
     *  - FAMS is ignored, instead 0-level FAM families are parsed/processed
     *  - only the first instance is used if the same property is given multiple times (e.g. multiple BIRT records)
     *
     * Suported family (FAM) properties: HUSB, WIFE, CHIL
     *
     * Note: reverse-engineered _GENSTAT values: the following symbols, in any position, mean:
     *   Disorder status:
     *    AFFECTED:  "O"
     *    HEARSAY:   "¬"  (hearsay graphic == pre-symptomatic graphic)
     *    UNTESTED:  "E"
     *    "carrier" and "examined" does not seem to be exported to GEDCOM or CSV
     *   Other:
     *    PROBAND:   "C"  (may be more than one)
     *    STILLBORN: "K"
     *    INFERTILE: "M"
     * ===============================================================================================
     */
    PedigreeImport.initFromGEDCOM = function(inputText, markEvaluated, saveIDAsExternalID)
    {
       var inputLines = inputText.match(/[^\r\n]+/g);
       if (inputLines.length == 0) throw "Unable to import: no data";

       var convertToObject = function(inputLines) {
           /* converts GEDCOM text into an object, where 0-level items are stored as "header", "individuals" and "families"
            * properties, and all items below are arrays of objects, e.g. an array of objects representing each individual.
            *
            * Each next-level keyword is a key in the object, and the associated value is an array of objects, each
            * object representing one encountered instance of the keyword (designed this way as ther emay be more than one
            * keyword with the same nem, e.g. multiple alternative DATEs for an event, or multiple CHILdren in a family)
            *
            * The value of the keyword itself (if any) is stored under the "value" key. In the example below the
            * all "DATA" keywords have no values, while all TEXT and DATE have some values assigned, and only some
            * EVEN keywords have a value.
            *
            * 0 @I1@ INDI
            *  1 EVEN AAA
            *   2 DATE 10 JAN 1800
            *   2 SOUR @S1@
            *    3 DATA
            *     4 TEXT ABC
            *    3 DATA
            *     4 TEXT DEF
            *    3 NOTE DEF
            *    3 ZZZZ 2
            *    3 ZZZZ 3
            *  1 EVEN
            *   2 DATE 1800
            *   2 SOUR @S2@
            *  1 EVEN BBB
            *   2 DATE 1900
            * 0 @I2@ INDI
            *  1 ...
            *
            * is stranslated to:
            *  // level
            *  //  1       2       3        4
            *  [{id: '@I1@'
            *    EVEN: [
            *           {value: 'AAA',
            *            DATE:  [{value: '10 JAN 1800'}],
            *            SOUR:  [{value: '@S1@',
            *                     DATA:  [{TEXT: [{value: 'ABC'}]}, {TEXT: [{value: 'DEF'}]}],
            *                     NOTE:  [{value: 'DEF'}],
            *                     ZZZZ:  [{value: '2'}, {value: '3'}]
            *                    }
            *                   ]
            *           },
            *           {DATE:  [{value: '1800'}],
            *            SOUR:  [{value: '@S2@'}]
            *           },
            *           {value: 'BBB',
            *            DATE:  [{value: '1900'}]
            *           }
            *          ]
            *   },
            *   {id: '@I2@' ...}
            *  ]
            */
           var obj = { "header": {}, "individuals": [], "families": [] };

           var currentObject = [];

           for (var i = 0; i < inputLines.length; i++) {
               var nextLine = inputLines[i].replace(/[^a-zA-Z0-9.@/\-\s*]/g, ' ').replace(/^\s+|\s+$/g, ''); // sanitize + trim

               var words = inputLines[i].split(/\s+/);
               var parts = words.splice(0,2);
               parts.push(words.join(' '));

               // now parts[0] = level, parts[1] = record type, parts[2] = value, if any

               var level = parseInt(parts[0]);

               currentObject.splice(level);

               if (level == 0) {
                   if (parts[1] == "HEAD") {
                       currentObject[0] = obj.header;
                   } else if (parts[1][0] == '@' && parts[2] == "INDI") {
                       obj.individuals.push({});
                       currentObject[0] = obj.individuals[obj.individuals.length - 1];
                       currentObject[0]["id"] = parts[1];
                   } else if (parts[1][0] == '@' && parts[2] == "FAM") {
                       obj.families.push({});
                       currentObject[0] = obj.families[obj.families.length - 1];
                       currentObject[0]["id"] = parts[1];
                   } else {
                       currentObject[0] = {};
                   }
               } else {
                   if (currentObject.length < level - 1) {
                       throw "Unable to import GEDCOM: a multi-level jump detected in line: [" + inputLines[i] + "]";
                   }

                   if (!currentObject[level-1].hasOwnProperty(parts[1]))
                       currentObject[level-1][parts[1]] = [];  // array of values

                   if (currentObject.length < level + 1) {
                       currentObject[level] = {};
                       currentObject[level - 1][parts[1]].push(currentObject[level]);
                   }

                   if (parts[2] != "") {
                       currentObject[level]["value"] = parts[2];
                   }
               }
           }

           return obj;
       };

       var gedcom = convertToObject(inputLines);
       console.log("GEDCOM object: " + Helpers.stringifyObject(gedcom));

       if (gedcom.header.hasOwnProperty("GEDC")) {
           if (gedcom.header.GEDC.hasOwnProperty("VERS")) {
               if (gedcom.header.GEDC.VERS != "5.5" && gedcom.header.GEDC.VERS != "5.5.1") {
                   alert("Unsupported GEDCOM version detected: [" + gedcom.header.GEDC.VERS + "]. "+
                         "Import will continue but the correctness is not guaranteed. Supportede versions are 5.5 and 5.5.1");
               }
           }
       }

       if (gedcom.individuals.length == 0) {
           throw "Unable to create a pedigree from GEDCOM: no individuals are defined in the import data";
       }

       var newG = new BaseGraph();

       var externalIDToID = {};

       // first pass: add all vertices and assign vertex IDs
       for (var i = 0; i < gedcom.individuals.length; i++) {
           var nextPerson =  gedcom.individuals[i];

           var pedigreeID = newG._addVertex( null, BaseGraph.TYPE.PERSON, {}, null );

           externalIDToID[nextPerson.id] = pedigreeID;

           var cleanedID = nextPerson.id.replace(/@/g, '')
           var properties = saveIDAsExternalID ? {"externalID": cleanedID} : {};

           properties["gender"] = "U";     // each person should have some gender set

           var getFirstValue = function(obj) {
               //if (Object.prototype.toString.call(obj) === '[object Array]')
               return obj[0].value;
           }

           var parseDate = function(gedcomDate) {
               gedcomDate = gedcomDate[0].value;

               // treat possible date modifiers
               //  "ABT" - "about"
               //  "EST" - "estimated"
               //  "BEF" - "before"
               //  "AFT" - "after"
               //  "BET ... AND ..." = "between ... and ..."
               // for all of the above the date itself is used as the date; for the "between" the first date is used.

               // TODO: add support for importing approximate dates, since those are now supported by PedigreeDate object
               gedcomDate = gedcomDate.replace(/^(\s*)ABT(\s*)/,"");
               gedcomDate = gedcomDate.replace(/^(\s*)EST(\s*)/,"");
               gedcomDate = gedcomDate.replace(/^(\s*)BEF(\s*)/,"");
               gedcomDate = gedcomDate.replace(/^(\s*)AFT(\s*)/,"");
               var getBetweenDate = /^\s*BET\s+(.+)\s+AND.*/;
               var match = getBetweenDate.exec(gedcomDate);
               if (match != null) {
                   gedcomDate = match[1];
               }

               if (gedcomDate == "?") return null;

               // TODO: can handle approx. dates (i.e. "ABT" and "EST") better using new Pedigree fuzzy dates
               //       conversion below would convert "ABT JAN 1999" to "JAN 1 1999" instead of fuzzy date "Jan 1999"
               var timestamp=Date.parse(gedcomDate)
               if (isNaN(timestamp)==false) {
                   return new PedigreeDate(new Date(timestamp));
               }
               return null;
           };

           for (var property in nextPerson) {
               if (nextPerson.hasOwnProperty(property)) {
                   if (property == "SEX") {
                       var genderString = getFirstValue(nextPerson[property])[0].toLowerCase(); // use first character only
                       if( genderString == "female" || genderString == "f")
                           properties["gender"] = "F";
                       else if( genderString == "male" || genderString == "m")
                           properties["gender"] = "M";
                   } else if (property == "BIRT") {
                       if (nextPerson[property][0].hasOwnProperty("DATE")) {
                           var date = parseDate(nextPerson[property][0]["DATE"]);
                           if (date !== null) {
                               properties["dob"] = new PedigreeDate(date).getSimpleObject()
                           }
                       }
                   } else if (property == "DEAT") {
                       if (properties.hasOwnProperty("lifeStatus") && properties["lifeStatus"] == "stillborn")
                           continue;
                       properties["lifeStatus"] = "deceased";
                       if (nextPerson[property][0].hasOwnProperty("DATE")) {
                           var date = parseDate(nextPerson[property][0]["DATE"]);
                           if (date !== null) {
                               properties["dod"] = new PedigreeDate(date).getSimpleObject();
                           }
                       }
                   } else if (property == "ADOP") {
                       properties["adoptedStatus"] = "adoptedIn";
                   } else if (property == "_INFO") {
                       if (!properties.hasOwnProperty("comments"))
                           properties["comments"] = "";
                       properties["comments"] += "(Info: " + getFirstValue(nextPerson[property]) + ")\n";
                   } else if (property == "NOTE" || property == "_COMMENT") {
                       if (!properties.hasOwnProperty("comments"))
                           properties["comments"] = "";
                       properties["comments"] += getFirstValue(nextPerson[property]) + "\n";
                       if (nextPerson[property][0].hasOwnProperty("CONT")) {
                           var more = nextPerson[property][0]["CONT"];
                           for (var cc = 0; cc < more.length; cc++) {
                               properties["comments"] += more[cc].value + "\n";
                           }
                       }
                   } else if (property == "NAME") {
                       var nameParts = getFirstValue(nextPerson[property]).split('/');
                       var firstName = nameParts[0].replace(/^\s+|\s+$/g, '');
                       var lastName  = nameParts.length > 1 ? nameParts[1].replace(/^\s+|\s+$/g, '') : "";
                       properties["fName"] = firstName;
                       if (lastName != "")
                           properties["lName"] = lastName;
                   } else if (property == "_MAIDEN") {
                       var nameParts = getFirstValue(nextPerson[property]).split('/');
                       var firstName = nameParts[0].replace(/^\s+|\s+$/g, '');
                       var lastName  = nameParts.length > 1 ? nameParts[1].replace(/^\s+|\s+$/g, '') : "";
                       properties["lNameAtB"] = firstName;
                       if (lastName != "")
                           properties["lNameAtB"] += " " + lastName;
                   } else if (property == "_GENSTAT") {
                       var props = getFirstValue(nextPerson[property]).split('');
                       for (var p = 0; p < props.length; p++) {
                           var value = props[p];
                           if (value.charCodeAt(0) == 65533 || value.charCodeAt(0) == 172) {
                               // one value is obtained via copy-paste, another via file upload
                               value = "HEARSAY";
                           }
                           switch(value) {
                           case "O":
                               properties["carrierStatus"] = 'affected';
                               properties["disorders"]     = ['affected'];
                               if (markEvaluated)
                                   properties["evaluated"] = true;
                               break;
                           case "HEARSAY":
                               properties["carrierStatus"] = 'presymptomatic'; // the closest graphic to cyrillic's "hearsay"
                               if (markEvaluated)
                                   properties["evaluated"] = true;
                               break;
                           case "K":
                               properties["lifeStatus"] = "stillborn";
                               break;
                           case "M":
                               properties["childlessStatus"] = "infertile";
                               break;
                           case "E":
                               if (!properties.hasOwnProperty("comments")) {
                                   properties["comments"] = "(untested)";
                               }
                               else {
                                   properties["comments"] = "(untested)\n" + properties["comments"];
                               }
                               break;
                           case "C":
                               // TODO: proband
                               break;
                           }
                       }
                   }
               }
           }
           if (properties.hasOwnProperty("comments")) {
               // remove trailing newlines and/or empty comments
               properties.comments = properties.comments.replace(/^\s+|\s+$/g, '');
               if (properties.comments == "")
                   delete properties.comments;
           }
           newG.properties[pedigreeID] = properties;
       }

       var relationshipTracker = new RelationshipTracker(newG);

       // second pass (once all vertex IDs are known): process families & add edges
       for (var i = 0; i < gedcom.families.length; i++) {
           var nextFamily = gedcom.families[i];

           var motherLink = nextFamily.hasOwnProperty("WIFE") ? getFirstValue(nextFamily["WIFE"]) : null;
           var fatherLink = nextFamily.hasOwnProperty("HUSB") ? getFirstValue(nextFamily["HUSB"]) : null;

           // create a virtual parent in case one of the parents is missing
           if (fatherLink == null) {
               var fatherID = newG._addVertex( null, BaseGraph.TYPE.PERSON, {"gender": "M", "comments": "unknown"}, null );
           } else {
               var fatherID = externalIDToID[fatherLink];
           }
           if (motherLink == null) {
               var motherID = newG._addVertex( null, BaseGraph.TYPE.PERSON, {"gender": "F", "comments": "unknown"}, null );
           } else {
               var motherID = externalIDToID[motherLink];
           }

           // both motherID and fatherID are now given and represent valid existing nodes in the pedigree

           // if there is a relationship between motherID and fatherID the corresponding childhub is returned
           // if there is no relationship, a new one is created together with the chldhub
           var chhubID = relationshipTracker.createOrGetChildhub(motherID, fatherID);

           var children = nextFamily.hasOwnProperty("CHIL") ? nextFamily["CHIL"] : null;

           if (children == null) {
               // create a virtual child
               var childID = newG._addVertex( null, BaseGraph.TYPE.PERSON, {"gender": "U", "placeholder": true}, null );
               externalIDToID[childID] = childID;
               children = [{"value": childID}];
               // TODO: add "infertile by choice" property to the relationship
           }

           for (var j = 0; j < children.length; j++) {
               var externalID = children[j].value;

               var childID = externalIDToID.hasOwnProperty(externalID) ? externalIDToID[externalID] : null;

               if (childID == null) {
                   throw "Unable to import pedigree: child link does not point to an existing individual: [" + externalID + "]";
               }

               newG.addEdge( chhubID, childID );
           }
       }

       PedigreeImport.validateBaseGraph(newG);

       return {"baseGraph": newG, "probandNodeID": 0};
    }


    // ===============================================================================================


    // TODO: convert internal properties to match public names and rename this to "supportedProperties"
    PedigreeImport.JSONToInternalPropertyMapping = {
            "proband":         "proband",
            "lastname":        "lName",
            "lastnameatbirth": "lNameAtB",
            "comments":        "comments",
            "twingroup":       "twinGroup",
            "monozygotic":     "monozygotic",
            "adoptedstatus":   "adoptedStatus",
            "evaluated":       "evaluated",
            "birthdate":       "dob",
            "deathdate":       "dod",
            "gestationage":    "gestationAge",
            "aliveandwell":    "aliveandwell",
            "lifestatus":      "lifeStatus",
            "deceasedage":     "deceasedAge",
            "deceasedcause":   "deceasedCause",
            "disorders":       "disorders",
            "features":        "features",
            "nonstandard_features": "nonstandard_features",
            "genes":           "genes",
            "ethnicities":     "ethnicities",
            "carrierstatus":   "carrierStatus",
            "externalid":      "externalID",
            "numpersons":      "numPersons",
            "lostcontact":     "lostContact",
            "nodenumber":      "nodeNumber",
            "cancers":         "cancers",
            "childlessstatus": "childlessStatus",
            "childlessreason": "childlessReason",
            "phenotipsid":     "phenotipsId"
        };


    /*
     * Converts property name from external JSON format to internal - also helps to
     * support aliases for some terms and weed out unsupported terms.
     */
    PedigreeImport.convertProperty = function(externalPropertyName, value) {
        try {
            // support old JSON format: "hpoTerms" instead of "features" and
            // "candidateGenes" instead of "genes"

            if (externalPropertyName.toLowerCase() == "hpoterms") {
                // old "hpoTerms" was an array of observed feature IDs
                var features = [];
                var nonstandard_features = [];
                for (var i = 0; i < value.length; i++) {
                    var id = value[i];
                    if (HPOTerm.isValidID(id)) {
                        var feature = { "id": value[i], "observed":"yes", "type":"phenotype" };
                        features.push(feature);
                    } else {
                        var nonstandard_feature = { "label": value[i], "observed":"yes", "type":"phenotype" };
                        nonstandard_features.push(nonstandard_feature);
                    }
                }
                var result = [];
                if (features.length > 0) {
                    result.push( {"propertyName": "features", "value": features } );
                }
                if (nonstandard_features.length > 0) {
                    result.push( {"propertyName": "nonstandard_features", "value": nonstandard_features } );
                }
                return result;
            }

            if (externalPropertyName.toLowerCase() == "candidategenes") {
                // old "candidateGenes" was an array of candidate gene IDs
                var genes = [];
                for (var i = 0; i < value.length; i++) {
                    var gene = { "gene": value[i], "status": "candidate" };
                    genes.push(gene);
                }
                return [ {"propertyName": "genes", "value": genes} ];
            }

            if (!PedigreeImport.JSONToInternalPropertyMapping.hasOwnProperty(externalPropertyName)) {
                return [];
            }

            var internalPropertyName = PedigreeImport.JSONToInternalPropertyMapping[externalPropertyName];

            if (externalPropertyName == "birthdate" || externalPropertyName == "deathdate") {
                // handle deprecated date formats by using PedigreeDate constructors which can handle those
                var pedigreeDate = new PedigreeDate(value);
                value = pedigreeDate.getSimpleObject();
            }

            return [ {"propertyName": internalPropertyName, "value": value } ];
        } catch (err) {
            console.log("Error importing property [" + externalPropertyName + "]");
            return [];
        }
    }

    PedigreeImport.JSONToInternalRelationshipPropertyMapping = {
            "childlessstatus": "childlessStatus",
            "childlessreason": "childlessReason",
            "consanguinity":   "consangr",
            "separated":       "broken"
        };

    // works for both SimpleJSON and FullJSON
    PedigreeImport.convertRelationshipProperty = function(externalPropertyName, value) {
        try {
            if (!PedigreeImport.JSONToInternalRelationshipPropertyMapping.hasOwnProperty(externalPropertyName)) {
                return null;
            }

            var internalPropertyName = PedigreeImport.JSONToInternalRelationshipPropertyMapping[externalPropertyName];

            if (externalPropertyName == "consanguinity") {
                if (value != "yes" && value != "no" && value != "Y" && value != "N") {
                    return null;  // equivalent to "auto"
                }
                if (value == "yes") {
                    value = "Y";
                } else if (value == "no") {
                    value = "N";
                }
            } else if (externalPropertyName == "childless") {
                if (value == "no") {
                    return null;  // not childless
                }
                // ... other properties are the same in external and internal formats
            }
            return {"propertyName": internalPropertyName, "value": value };
        } catch (err) {
            console.log("Error importing relationship property [" + externalPropertyName + "]");
            return null;
        }
    }

    //===============================================================================================

    /*
     * Helper class which keeps track of relationships already seen in pedigree being imported
     */
    var RelationshipTracker = function (newG) {
        this.newG = newG;

        this.relationships = {};
        this.relChildHubs  = {};
        this.chhubRels     = {};
    };

    RelationshipTracker.prototype = {

        // if there is a relationship between partnerID1 and partnerID2 the corresponding childhub is returned
        // if there is no relationship, a new one is created together with the chldhub
        createOrGetChildhub: function (partnerID1, partnerID2)
        {
            // both partnerID1 and partnerID2 are now given. Check if there is a relationship between the two of them
            if (this.relationships.hasOwnProperty(partnerID1) && this.relationships[partnerID1].hasOwnProperty(partnerID2)) {
                var relID   = this.relationships[partnerID1][partnerID2];
                var chhubID = this.relChildHubs[relID];
            } else {
                if (this.relationships[partnerID1] === undefined) this.relationships[partnerID1] = {};
                if (this.relationships[partnerID2] === undefined) this.relationships[partnerID2] = {};

                var relID   = this.newG._addVertex( null, BaseGraph.TYPE.RELATIONSHIP, {}, {} );
                var chhubID = this.newG._addVertex( null, BaseGraph.TYPE.CHILDHUB,     {}, {} );

                this.newG.addEdge( relID,    chhubID );
                this.newG.addEdge( partnerID1, relID );
                this.newG.addEdge( partnerID2, relID );

                this.relationships[partnerID1][partnerID2] = relID;
                this.relationships[partnerID2][partnerID1] = relID;
                this.relChildHubs[relID] = chhubID;
                this.chhubRels[chhubID]  = relID;
            }

            return chhubID;
        },

        getRelationshipIDForChildHub: function(chhubID) {
            return this.chhubRels[chhubID];
        }
    };

    return PedigreeImport;
});
