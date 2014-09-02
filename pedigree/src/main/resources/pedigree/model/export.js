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

   var idToPedId = PedigreeExport.createNewIDs(pedigree, idGenerationPreference);
   
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

//===============================================================================================

/*
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
 *   1BrCa: Age at first breast cancer diagnosis, 0 = unaffected, integer = age at diagnosis, AU = unknown age at diagnosis (affected unknown)
 *   2BrCa: Age at contralateral breast cancer diagnosis, 0 = unaffected, integer = age at diagnosis, AU = unknown age at diagnosis (affected unknown)
 *   OvCa: Age at ovarian cancer diagnosis, 0 = unaffected, integer = age at diagnosis, AU = unknown age at diagnosis (affected unknown)
 *   ProCa: Age at prostate cancer diagnosis 0 = unaffected, integer = age at diagnosis, AU = unknown age at diagnosis (affected unknown)
 *   PanCa: Age at pancreatic cancer diagnosis 0 = unaffected, integer = age at diagnosis, AU = unknown age at diagnosis (affected unknown)
 *   Gtest: Genetic test status, 0 = untested, S = mutation search, T = direct gene test
 *   Mutn: 0 = untested, N = no mutation, 1 = BRCA1 positive, 2 = BRCA2 positive, 3 = BRCA1 and BRCA2 positive
 *   Ashkn: 0 = not Ashkenazi, 1 = Ashkenazi
 *   ER: Estrogen receptor status, 0 = unspecified, N = negative, P = positive
 *   PR: Progestrogen receptor status, 0 = unspecified, N = negative, P = positive
 *   HER2: Human epidermal growth factor receptor 2 status, 0 = unspecified, N = negative, P = positive
 *   CK14: Cytokeratin 14 status, 0 = unspecified, N = negative, P = positive
 *   CK56: Cytoke ratin 56 status, 0 = unspecified, N = negative, P = positive
 */
PedigreeExport.exportAsBOADICEA = function(pedigree, idGenerationPreference)
{
   var output = "BOADICEA import pedigree file format 2.0\n";
   output    += "FamilyID\tName\tTarget\tIndivID\tFathID\tMothID\tSex\tTwin\tDead\tAge\tYob\tNot_implemented_yet_except_Ahkenazi\n";

   var familyID = XWiki.currentDocument.page;

   var idToBoadId = PedigreeExport.createNewIDs(pedigree, idGenerationPreference, 7 /* max ID length */);

   var alertUnknownGenderFound = false; // BOADICEA does not support unknown genders

   for (var i = 0; i <= pedigree.GG.getMaxRealVertexId(); i++) {
       if (!pedigree.GG.isPerson(i)) continue;

       var id = idToBoadId[i];

       var name = pedigree.GG.properties[i].hasOwnProperty("fName") ? pedigree.GG.properties[i]["fName"].substring(0,8) : id;

       var proband = (i == 0) ? "1" : "0";

       output += familyID + "\t" + name + "\t" + proband + "\t" + id + "\t";

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
           output += idToBoadId[father] + "\t" + idToBoadId[mother] + "\t";
       } else {
           output += "0\t0\t";
       }

       var sex = "M";
       if (pedigree.GG.properties[i]["gender"] == "F") {
           sex = "F";
       } else if (pedigree.GG.properties[i]["gender"] == "U") {
           alertUnknownGenderFound = true;
       }
       output += sex + "\t";

       if (pedigree.GG.getTwinGroupId(i) !== null) {
           output += "1\t";
       } else {
           output += "0\t";
       }

       var dead = "0";
       if (pedigree.GG.properties[i].hasOwnProperty("lifeStatus")) {
           if (pedigree.GG.properties[i]["lifeStatus"] != "alive") {
               var dead = "1";
           }
       }
       output += dead + "\t";

       var age = "0";
       var yob = "0";
       if (pedigree.GG.properties[i].hasOwnProperty("dob")) {
           var date = new Date(pedigree.GG.properties[i]["dob"]);
           yob = date.getFullYear();
           age = new Date().getFullYear() - yob;
       }
       output += age + "\t" + yob + "\t";

       output += "AU\tAU\tAU\tAU\tAU\t";   // unimplemented fields: age at cancer detection

       output += "0\t0\t";                 // unimplemented fields: Genetic test status + mutations

       var ashkenazi = "0";
       if (pedigree.GG.properties[i].hasOwnProperty("ethnicities")) {
           var ethnicities = pedigree.GG.properties[i]["ethnicities"];
           for (var k = 0; k < ethnicities.length; k++) {
               if (ethnicities[k].match(/ashkenaz/i) !== null) {
                   ashkenazi = "1";
                   break;
               }
           }
       }
       output += ashkenazi + "\t";

       output += "0\t0\t0\t0\t0";  // unimplemented fields: receptor status, etc.

       output += "\n";
   }

   if (alertUnknownGenderFound) {
       alert("BOADICEA format does not support unknown genders. All persons of unknown gender were saved as male in the export file");
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

PedigreeExport.createNewIDs = function(pedigree, idGenerationPreference, maxLength) {
    var idToNewId = {};
    var usedIDs   = {};

    var nextUnusedID = 1;

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
        if (maxLength && id.length > maxLength) {
            id = id.substring(0, maxLength);
        }
        while ( usedIDs.hasOwnProperty(id) ) {
            if (!maxLength || id.length < maxLength) {
                id = "_" + id;
            } else {
                id = nextUnusedID++;
            }
        }

        idToNewId[i] = id;
        usedIDs[id]  = true;
    }

    return idToNewId;
}
