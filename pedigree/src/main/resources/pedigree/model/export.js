PedigreeExport = function () {
};

PedigreeExport.prototype = {
};

/* ===============================================================================================
 * 
 * Creates and returns a JSON in the "simple JSON" format (see PedigreeImport.initFromSimpleJSON)
 * 
 *  Simple JSON format: an array of objects, each object representing one person, e.g.:
 *
 *    [ { "name": "f11", "sex": "female", "lifeStatus": "deceased" },
 *      { "name": "m11", "sex": "male" },
 *      { "name": "f12", "sex": "female", "disorders": [603235, "142763", "custom disorder"] },
 *      { "name": "m12", "sex": "male" },
 *      { "name": "m21", "sex": "male", "mother": "f11", "father": "m11" },
 *      { "name": "f21", "sex": "female", "mother": "f12", "father": "m12" },
 *      { "name": "ch1", "sex": "female", "mother": "f21", "father": "m21", "disorders": [603235], "proband": true } ]
 *
 * @param pedigree {PositionedGraph} 
 * ===============================================================================================
 */
PedigreeExport.exportAsSimpleJSON = function(pedigree, privacySetting)
{
   var exportObj = [];

   for (var i = 0; i <= pedigree.GG.getMaxRealVertexId(); i++) {
       if (!pedigree.GG.isPerson(i)) continue;
       
       var person = {"id": i};
       
       // mother & father
       var parents = pedigree.GG.getParents(i);
       if (parents.length > 0) {
           var father = parents[0];
           var mother = parents[1];
           
           if ( pedigree.GG.properties[parents[0]]["gender"] == "F" ||
                pedigree.GG.properties[parents[1]]["gender"] == "M" ) {
               father = parents[1];
               mother = parents[0];
           }
           person["father"] = father;
           person["mother"] = mother;
       }
       
       // all other properties
       var properties = pedigree.GG.properties[i];
       for (var property in properties) {
           if (properties.hasOwnProperty(property)) {
               if (privacySetting != "all") {
                   if (property == 'lName' || property == 'fName' || property == 'lNameAtB' ||
                       property == 'dob' || property == 'bob') continue;
                   if (privacySetting == "minimal" && property == "comments") continue
               }
               var converted = PedigreeExport.convertProperty(property, properties[property]);
               if (converted !== null) {
                   person[converted.propertyName] = converted.value;
               }
           }
       }

       exportObj.push(person);
   }

   return JSON.stringify(exportObj);
}

//===============================================================================================

/*
 *  PED format:
 *  (from http://pngu.mgh.harvard.edu/~purcell/plink/data.shtml#ped)
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
 */
PedigreeExport.exportAsPED = function(pedigree, idGenerationPreference)
{
   var output = "";
   
   var familyID = XWiki.currentDocument.page;

   var nextUnusedID = 1;
   
   var idToPedId = {};
   var usedIDs   = {};
   
   // first pass: assign new IDs
   for (var i = 0; i <= pedigree.GG.getMaxRealVertexId(); i++) {
       if (!pedigree.GG.isPerson(i)) continue;
       
       var id = nextUnusedID++;
       if (idGenerationPreference == "external" && pedigree.GG.properties[i].hasOwnProperty("externalID")) {
           nextUnusedID--;
           id = pedigree.GG.properties[i]["externalID"].replace(/\s/g, '_');           
       } else if (idGenerationPreference == "name" && pedigree.GG.properties[i].hasOwnProperty("fName")) {
           nextUnusedID--;
           id = pedigree.GG.properties[i]["fName"].replace(/\s/g, '_');
       }
       while ( usedIDs.hasOwnProperty(id) ) {
           id = "_" + id;
       }
           
       idToPedId[i] = id;
       usedIDs[id]  = true;
   }
   
   // second pass: generate PED file
   for (var i = 0; i <= pedigree.GG.getMaxRealVertexId(); i++) {
       if (!pedigree.GG.isPerson(i)) continue;
       
       output += familyID + " " + idToPedId[i] + " "; 
       
       // mother & father
       var parents = pedigree.GG.getParents(i);
       if (parents.length > 0) {
           var father = parents[0];
           var mother = parents[1];
           
           if ( pedigree.GG.properties[parents[0]]["gender"] == "F" ||
                pedigree.GG.properties[parents[1]]["gender"] == "M" ) {
               father = parents[1];
               mother = parents[0];
           }
           output += idToPedId[father] + " " + idToPedId[mother] + " ";
       } else {
           output += "0 0 ";
       }
       
       var sex = 3;
       if (pedigree.GG.properties[i]["gender"] == "M") {
           sex = 1;
       }
       else if (pedigree.GG.properties[i]["gender"] == "F") {
           sex = 2;
       }
       output += (sex + " ");
       
       var status = -9; //missing
       if (pedigree.GG.properties[i].hasOwnProperty("carrierStatus")) {
           if (pedigree.GG.properties[i]["carrierStatus"] == "affected" ||
               pedigree.GG.properties[i]["carrierStatus"] == "carrier"  ||
               pedigree.GG.properties[i]["carrierStatus"] == "presymptomatic")
               status = 2;
           else
               status = 1;
       }
       output += status + "\n";
   }

   return output;
}

// ===============================================================================================

// TODO: convert internal properties to match public names and rename this to "supportedProperties"
PedigreeExport.internalToJSONPropertyMapping = {
        "proband":       "proband",
        "fName":         "firstName",
        "lName":         "lastName",
        "lNameAtB":      "lastNameAtBirth",
        "comments":      "comments",
        "twinGroup":     "twinGroup",
        "monozygotic":   "monozygotic",
        "isAdopted":     "adoptedIn",
        "evaluated":     "evaluated",
        "dob":           "birthDate",
        "dod":           "deathDate",
        "gestationAge":  "gestationAge",
        "lifeStatus":    "lifeStatus",
        "disorders":     "disorders",
        "ethnicities":   "ethnicities",
        "carrierStatus": "carrierStatus",
        "externalID":    "externalId",
        "gender":        "sex",
        "numPersons":    "numPersons"
    };

/*
 * Converts property name from external JSON format to internal - also helps to
 * support aliases for some terms and weed out unsupported terms.
 */
PedigreeExport.convertProperty = function(internalPropertyName, value) {
    
    if (!PedigreeExport.internalToJSONPropertyMapping.hasOwnProperty(internalPropertyName))
        return null;
            
    var externalPropertyName = PedigreeExport.internalToJSONPropertyMapping[internalPropertyName];
    
    if (externalPropertyName == "sex") {
        if (value == "M")
            value = "male";
        else if (value == "F")
            value = "female";
        else
            value = "unknown";
    }
        
    return {"propertyName": externalPropertyName, "value": value };
}
