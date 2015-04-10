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

       if (editor.getGraph().getProbandId() == i) {
           person["proband"] = true;
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
       if (pedigree.GG.isPlaceholder(i)) continue;
       
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
 *   CK56: Cytoke ratin 56 status, 0 = unspecified, N = negative, P = positive
 */
PedigreeExport.exportAsBOADICEA = function(dynamicPedigree, idGenerationPreference)
{
   var pedigree = dynamicPedigree.DG;

   var output = "BOADICEA import pedigree file format 2.0\n";
   output    += "FamID\t\tName\tTarget\tIndivID\tFathID\tMothID\tSex\tTwin\tDead\tAge\tYob\t1BrCa\t2BrCa\tOvCa\tProCa\tPanCa\tGtest\tMutn\tAshkn\tNot_implemented_yet\n";

   // note: a bug in BOADICEA online app requires that the column header line starts with "FamID" followed by a space.
   //       Anything else, including "FamilyID ..." will be reported as an error in pedigree.

   var familyID = XWiki.currentDocument.page;

   var idToBoadId = PedigreeExport.createNewIDs(pedigree, idGenerationPreference, 7 /* max ID length */, true /* forbid non-alphanum */);

   var alertUnknownGenderFound = false; // BOADICEA does not support unknown genders
   var warnAboutMissingDOB     = false; // BOADICEA seem to require all individuals with cancer to have some age specified

   for (var i = 0; i <= pedigree.GG.getMaxRealVertexId(); i++) {
       if (!pedigree.GG.isPerson(i)) continue;
       if (pedigree.GG.isPlaceholder(i)) continue;

       var id = idToBoadId[i];

       var name = pedigree.GG.properties[i].hasOwnProperty("fName") ? pedigree.GG.properties[i]["fName"].substring(0,8).replace(/[^A-Za-z0-9]/g, '') : id;

       var proband = (i == editor.getGraph().getProbandId()) ? "1" : "0";

       output += familyID + "\t" + name + "\t" + proband + "\t" + id + "\t";

       // mother & father
       var parents = pedigree.GG.getParents(i);
       if (parents.length > 0) {
           if ( pedigree.GG.properties[parents[0]]["gender"] == "U" &&
                pedigree.GG.properties[parents[1]]["gender"] == "U" ) {
               editor.getOkCancelDialogue().showCustomized("Unable to export in BOADICEA format when both parents of any node are of unknown gender",
                                                           "Can't export: missing gender data", "OK", null );
               return "";
           }

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
           // check partner gender(s) and if possible assign the opposite gender
           var possibleGenders = dynamicPedigree.getPossibleGenders(i);
           if (!possibleGenders["F"] && !possibleGenders["M"]) {
               // there is a person which can't be assigned both M and F because both conflict with other partner genders
               editor.getOkCancelDialogue().showCustomized("Unable to export in BOADICEA format since gender assignment in pedigree is inconsistent",
                                                           "Can't export: gender inconsistency in pedigree", "OK", null );
               return "";
           }
           if (possibleGenders["F"] && !possibleGenders["M"]) {
               sex = "F";
           } else if (possibleGenders["M"] && !possibleGenders["F"]) {
               sex = "M";
           } else {
               // can be both, no restrictions: assign "M"
               sex = "M";
           }
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
           var date = new PedigreeDate(pedigree.GG.properties[i]["dob"]);
           // BOADICEA file format does not support fuzzy dates, so get an estimate if only decade is available
           yob = parseInt(date.getMostConservativeYearEstimate());
           age = new Date().getFullYear() - yob;
       }
       output += age + "\t" + yob + "\t";

       // TODO: Contralateral breast cancer export/field?
       var cancerSequence = [ "Breast", "", "Ovarian", "Prostate", "Pancreatic" ];

       for (var c = 0; c < cancerSequence.length; c++) {
           cancer = cancerSequence[c];
           if (cancer == "" || !pedigree.GG.properties[i].hasOwnProperty("cancers")) {
               output += "0\t";
               continue;
           }

           if (pedigree.GG.properties[i].cancers.hasOwnProperty(cancer)) {
               var cancerData = pedigree.GG.properties[i].cancers[cancer];
               if (!cancerData.affected) {
                   output += "0\t";
               } else {
                   var ageAtDetection = cancerData.hasOwnProperty("numericAgeAtDiagnosis") && (cancerData.numericAgeAtDiagnosis > 0)
                                        ? cancerData.numericAgeAtDiagnosis : "AU";
                   output += ageAtDetection.toString() + "\t";
                   if (yob == "0") {
                       warnAboutMissingDOB = true;
                   }
               }
           } else {
               output += "0\t";
           }
       }

       output += "0\t"; // TODO: Genetic test status

       // BRCA1/BRCA2 mutations
       if (pedigree.GG.properties[i].hasOwnProperty("candidateGenes")) {
           var genes = pedigree.GG.properties[i].candidateGenes;
           var status = "0";
           if (arrayIndexOf(genes, "BRCA1") >= 0) {
               status = "1";
           }
           if (arrayIndexOf(genes, "BRCA2") >= 0) {
               if (status == "1") {
                   status = "3";
               } else {
                   status = "2";
               }
           }
           // TODO: if BRCA1 and/or BRCA2 are among rejected genes set status to "N"
           output += status + "\t";
       } else {
           output += "0\t";
       }

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

   if (alertUnknownGenderFound || warnAboutMissingDOB) {
       var warningText = "Pedigree can be exported, but there are warnings:\n\n\n";

       var numberWarnings = false;
       if (alertUnknownGenderFound && warnAboutMissingDOB) {
           numberWarnings = true;
       }
       if (alertUnknownGenderFound) {
           warningText += (numberWarnings ? "1) " : "") +
                           "BOADICEA format does not support unknown genders.\n\n" +
                          "All persons of unknown gender were either assigned a gender "+
                          "opposite to their partner's gender or saved as male in the export file";
       }
       if (warnAboutMissingDOB) {
           warningText += (numberWarnings ? "\n\n\n2) " : "") +
                          "BOADICEA requires that all individuals with cancer have their age specified or estimated.\n\n" +
                          "A person with cancer is missing age data, data will be exported but may not be accepted by BOADICEA";
       }
       alert(warningText);
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
        "adoptedStatus": "adoptedStatus",
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
        "numPersons":    "numPersons",
        "hpoTerms":      "hpoTerms",
        "candidateGenes":"candidateGenes",
        "lostContact":   "lostContact",
        "nodeNumber":    "nodeNumber",
        "cancers":       "cancers"
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

PedigreeExport.createNewIDs = function(pedigree, idGenerationPreference, maxLength, forbidNonAlphaNum) {
    var idToNewId = {};
    var usedIDs   = {};

    var nextUnusedID = 1;

    for (var i = 0; i <= pedigree.GG.getMaxRealVertexId(); i++) {
        if (!pedigree.GG.isPerson(i)) continue;
        if (pedigree.GG.isPlaceholder(i)) continue;

        var id = nextUnusedID++;
        if (idGenerationPreference == "external" && pedigree.GG.properties[i].hasOwnProperty("externalID")) {
            nextUnusedID--;
            id = pedigree.GG.properties[i]["externalID"].replace(/\s/g, '_');
        } else if (idGenerationPreference == "name" && pedigree.GG.properties[i].hasOwnProperty("fName")) {
            nextUnusedID--;
            id = pedigree.GG.properties[i]["fName"].replace(/\s/g, '_');
        }
        id = String(id);
        if (forbidNonAlphaNum) {
            id = id.replace(/[^A-Za-z0-9]/g, '');  // can't use \W since that allows "_"
        }
        if (maxLength && id.length > maxLength) {
            id = id.substring(0, maxLength);
        }
        var baseID     = id;
        var numSimilar = 2;
        while ( usedIDs.hasOwnProperty(id) ) {
            if (maxLength) {
                var length = baseID.length + String(numSimilar).length;
                var cutSize = length - maxLength;
                id = baseID.substring(0, id.length - cutSize);
            } else {
                id = baseID;
            }
            id = id + String(numSimilar);
            numSimilar++;
        }

        idToNewId[i] = id;
        usedIDs[id]  = true;
    }

    return idToNewId;
}
