PedigreeImport = function () {
};

PedigreeImport.prototype = {

};

/*PedigreeImport.SUPORTED_FORMATS = {
  PED:                    1,      // standard .PED format. Can only import family structure, gender and the affected status
  PHENOTIPS_GRAPH:        2,      // Phenotips pedigree format, whithout positioning information (needs to be laid out automaticaly)
  PHENOTIPS_INTERNAL_OLD: 3       // Phenotips internal format used during development and in test cases (to be replaced)
};

PedigreeImport.autodetectFormat = function(input) {

}*/

PedigreeImport.initFromPhenotipsInternal = function(inputG)
{
    // note: serialize() produces the correct input for this function

    var newG = new BaseGraph();

    var nameToId = {};

    var relationshipHasExplicitChHub = {};

    // first pass: add all vertices and assign vertex IDs
    for (var v = 0; v < inputG.length; v++) {

        if (!inputG[v].hasOwnProperty("name") && !inputG[v].hasOwnProperty("id"))
            throw "Invalid inpiut: a node without id and without name";

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
                    properties["gender"] = "M";
            }
        }

        var width = inputG[v].hasOwnProperty('width') ?
                    inputG[v].width :
                    (type == TYPE.PERSON ? newG.defaultPersonNodeWidth : newG.defaultNonPersonNodeWidth);

        var newID = newG._addVertex( null, type, properties, width );   // "null" since id is not known yet

        if (inputG[v].hasOwnProperty("name")) {  // note: this means using user input (not produced by this.serialize)
            if (nameToId[inputG[v].name])
                throw "Invalid user input: multiple nodes with the same name";
            if (type == TYPE.PERSON)
                newG.properties[newID]["fName"] = inputG[v].name;
            nameToId[inputG[v].name] = newID;
        }

        // when entered by user manually allow users to skip childhub nodes (and create them automatically)
        // (but when saving/restoring from a JSON need to save/restore childhub nodes as they
        //  may have some properties assigned by the user which we need to save/restore)
        if ( type == TYPE.RELATIONSHIP && !relationshipHasExplicitChHub.hasOwnProperty(v) ) {
            var chHubId = newG._addVertex(null, TYPE.CHILDHUB, null, width );
            nameToId["_chhub_" + newID] = chHubId;
        }
    }

    // second pass (once all vertex IDs are known): process edges
    for (var v = 0; v < inputG.length; v++) {
        var nextV = inputG[v];

        var vID    = nextV.hasOwnProperty("id") ? nextV.id : nameToId[nextV.name];
        var origID = vID;

        var substitutedID = false;

        if (newG.type[vID] == TYPE.RELATIONSHIP && !relationshipHasExplicitChHub.hasOwnProperty(vID)) {
            // replace edges from rel node by edges from childhub node
            var childhubID = nameToId["_chhub_" + vID];
            vID = childhubID;
            substitutedID = true;
        }

        var maxChildEdgeWeight = 0;

        if (nextV.outedges) {
            for (var outE = 0; outE < nextV.outedges.length; outE++) {
                var target   = nextV.outedges[outE].to;
                var targetID = nameToId[target] ? nameToId[target] : target;  // can specify target either by name or ID

                if (!newG.isValidId(targetID))
                    throw "Invalid input: invalid edge target (" + target + ")";

                var weight = 1;
                if (nextV.outedges[outE].hasOwnProperty('weight'))
                    weight = nextV.outedges[outE].weight;
                if ( weight > maxChildEdgeWeight )
                    maxChildEdgeWeight = weight;

                newG.addEdge( vID, targetID, weight );
            }
        }

        if (substitutedID) {
            newG.addEdge( origID, vID, maxChildEdgeWeight );
        }
    }

    newG.validate();

    return newG;
}

/*
   ==================================================================================================
   PED format:
   (from http://pngu.mgh.harvard.edu/~purcell/plink/data.shtml#ped)
     Family ID
     Individual ID
     Paternal ID
     Maternal ID
     Sex (1=male; 2=female; other=unknown)
     Phenotype

     Phenotype, by default, should be coded as:
        -9 missing
         0 missing
         1 unaffected
         2 affected

   ==================================================================================================

   LINKAGE format:
   (from http://www.helsinki.fi/~tsjuntun/autogscan/pedigreefile.html)

     Column 1:   Pedigree number
     Column 2:   Individual ID number
     Column 3:   ID of father
     Column 4:   ID of mother
     Column 5:   First offspring ID
     Column 6:   Next paternal sibling ID
     Column 7:   Next maternal sibling ID
     Column 8:   Sex
     Column 9:   Proband status (1=proband, higher numbers indicate doubled individuals formed
                                 in breaking loops. All other individuals have a 0 in this field.)
     Column 10+: Disease and marker phenotypes (as in the original pedigree file)
   ==================================================================================================
*/

PedigreeImport.initFromPED = function(inputText, linkageMod, acceptOtherPhenotypes, affectedCodeOne, disorderNames)
{
    var inputLines = inputText.match(/[^\r\n]+/g);

    var familyPrefix = "";

    var newG = new BaseGraph();

    var nameToId = {};

    var phenotypeValues = {};  // set of all posible valuesin the phenotype column

    var extendedPhenotypesFound = false;

    // support both automatic and user-defined assignment of proband
    var nextID = linkageMod ? 1 : 0;

    // first pass: add all vertices and assign vertex IDs
    for (var i = 0; i < inputLines.length; i++) {

        inputLines[i] = inputLines[i].replace(/[^a-zA-Z0-9.\-\s*]/g, ' ');
        inputLines[i] = inputLines[i].replace(/^\s+|\s+$/g, '');  // trim()

        var parts = inputLines[i].split(/\s+/);
        //console.log("Parts: " + stringifyObject(parts));

        if (parts.length < 6 || (linkageMod && parts.length < 10)) {
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
        if (nameToId.hasOwnProperty(pedID))
            throw "Multiple persons with the same ID [" + pedID + "]";

        var genderValue = linkageMod ? parts[7] : parts[4];
        var gender = "U";
        if (genderValue == 1)
            gender = "M";
        else if (genderValue == 2)
            gender = "F";
        var properties = {"gender": gender};

        properties["externalID"] = pedID;

        var useID = (linkageMod && parts[8] == 1) ? 0 : nextID++;
        if (i == inputLines.length-1 && newG.v[0] === undefined) {
            // last node and no node with id 0 yet
            useID = 0;
        }

        var pedigreeID = newG._addVertex( useID, TYPE.PERSON, properties, newG.defaultPersonNodeWidth );

        nameToId[pedID] = pedigreeID;

        var phenotype = linkageMod ? parts[9] : parts[5];
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

    var relationships = {};
    var relChildHubs  = {};

    var defaultEdgeWeight = 1;

    // second pass (once all vertex IDs are known): process edges
    for (var i = 0; i < inputLines.length; i++) {
        var parts = inputLines[i].split(/\s+/);

        var thisPersonName = parts[1];
        var id = nameToId[thisPersonName];

        var phenotype = linkageMod ? parts[9] : parts[5];
        if (affectedValues.hasOwnProperty(phenotype)) {
            var disorder = disorderNames.hasOwnProperty(phenotype) ? disorderNames[phenotype] : "affected";
            newG.properties[id]["carrierStatus"] = 'affected';
            newG.properties[id]["disorders"]     = [disorder];
            newG.properties[id]["evaluated"]     = true;
        } else if (unaffectedValues.hasOwnProperty(phenotype)) {
            newG.properties[id]["carrierStatus"] = '';
            newG.properties[id]["evaluated"]     = true;
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
        // So create a virtual parent in case one if missing
        if (fatherID == 0) {
            fatherID = newG._addVertex( null, TYPE.PERSON, {"gender": "M", "comments": "unknown"}, newG.defaultPersonNodeWidth );
        } else {
            fatherID = nameToId[fatherID];
        }
        if (motherID == 0) {
            motherID = newG._addVertex( null, TYPE.PERSON, {"gender": "F", "comments": "unknown"}, newG.defaultPersonNodeWidth );
        } else {
            motherID = nameToId[motherID];
        }

        // both motherID and fatherID are now given. Check if there is a relationship between the two of them
        if (relationships.hasOwnProperty(motherID) && relationships[motherID].hasOwnProperty(fatherID)) {
            var relID   = relationships[motherID][fatherID];
            var chhubID = relChildHubs[relID];
        } else {
            if (relationships[motherID] === undefined) relationships[motherID] = {};
            if (relationships[fatherID] === undefined) relationships[fatherID] = {};

            var relID   = newG._addVertex( null, TYPE.RELATIONSHIP, {}, newG.defaultNonPersonNodeWidth );
            var chhubID = newG._addVertex( null, TYPE.CHILDHUB,     {}, newG.defaultNonPersonNodeWidth );

            newG.addEdge( relID, chhubID, defaultEdgeWeight );
            newG.addEdge( motherID, relID, defaultEdgeWeight );
            newG.addEdge( fatherID, relID, defaultEdgeWeight );

            relationships[motherID][fatherID] = relID;
            relationships[fatherID][motherID] = relID;
            relChildHubs[relID] = chhubID;
        }

        newG.addEdge( chhubID, id, defaultEdgeWeight );
    }

    try {
        newG.validate();
    } catch( err) {
        if (err.indexOf("disconnected component")) {
            throw "Unsupported feature: some components of the imported pedigree are disconnected from each other";
        } else {
            throw "Unable to import pedigree";
        }
    }

    return newG;
}

