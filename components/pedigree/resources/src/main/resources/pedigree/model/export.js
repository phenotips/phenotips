define([
    "pedigree/pedigreeDate",
    "pedigree/model/helpers"
  ], function(
    PedigreeDate,
    Helpers
  ){
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
   *      { "name": "ch1", "sex": "female", "mother": "f21", "father": "m21", "disorders": [603235], "proband": true },
   *      { "name": "m22", "sex": "male" },
   *      { "relationshipId": 1, "partner1": "f21", "partner2": "m22"} ]
   *
   * See import.js/PedigreeImport.initFromSimpleJSON() for more detailed description of supported properties
   *
   * @param pedigree {PositionedGraph}
   * ===============================================================================================
   */
  PedigreeExport.exportAsSimpleJSON = function(pedigree, privacySetting)
  {
     var exportObj = [];

     var getMotherFather = function(nodeID) {
         // mother & father
         var parents = pedigree.GG.getParents(nodeID);
         if (parents.length > 0) {
             var father = parents[0];
             var mother = parents[1];
             if ( pedigree.GG.properties[parents[0]]["gender"] == "F" ||
                  pedigree.GG.properties[parents[1]]["gender"] == "M" ) {
                 father = parents[1];
                 mother = parents[0];
             }
         }
         return {"mother": mother, "father": father};
     };

     var idToJSONId = PedigreeExport.createNewIDs(pedigree);

     for (var i = 0; i <= pedigree.GG.getMaxRealVertexId(); i++) {
         if (!pedigree.GG.isPerson(i) || pedigree.GG.isPlaceholder(i)) continue;

         var person = {"id": idToJSONId[i]};

         if (i == pedigree.probandId) {
             person["proband"] = true;
         }

         var parents = getMotherFather(i);
         person["father"] = idToJSONId[parents.father];
         person["mother"] = idToJSONId[parents.mother];

         // all other properties
         var properties = pedigree.GG.properties[i];
         for (var property in properties) {
             if (properties.hasOwnProperty(property)) {
                 if (privacySetting != "all") {
                     if (property == 'lName' || property == 'fName' || property == 'lNameAtB' ||
                         property == 'dob' || property == 'bob') continue;
                     if (privacySetting == "minimal" && property == "comments") continue
                 }
                 var converted = PedigreeExport.convertPropertyToSimpleJSON(property, properties[property]);
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

     var nextRelId = 1;
     for (var i = 0; i <= pedigree.GG.getMaxRealVertexId(); i++) {
         if (!pedigree.GG.isRelationship(i)) continue;

         var relationship = {"relationshipId": nextRelId++};

         // only indicate childless relationships or relationships with properties
         var properties = pedigree.GG.properties[i];
         var hasProperties = false;
         for (var property in properties) {
             if (properties.hasOwnProperty(property)) {
                 hasProperties = true;
                 var converted = PedigreeExport.convertRelationshipProperty(property, properties[property]);
                 if (converted !== null) {
                     relationship[converted.propertyName] = converted.value;
                 }
             }
         }

         if (!hasProperties) {
             continue;
         }

         var parents = getMotherFather(i);
         relationship["partner1"] = idToJSONId[parents.father];
         relationship["partner2"] = idToJSONId[parents.mother];

         exportObj.push(relationship);
     }

     return JSON.stringify(exportObj);
  }

  //===============================================================================================

  /*
   *  See http://zzz.bwh.harvard.edu/plink/data.shtml#ped,
   *      https://gatkforums.broadinstitute.org/gatk/discussion/7696/pedigree-ped-files
   *
   *  PED format:
   *   Family ID
   *   Individual ID
   *   Paternal ID
   *   Maternal ID
   *   Sex (1=male; 2=female; other=unknown)
   *   Phenotype
   *
   *   Note: the IDs should be alphanumeric
   *
   *   Phenotype, by default, should be coded as:
   *      -9 missing
   *       0 missing
   *       1 unaffected
   *       2 affected
   *
   *   NOTE: see PedigreeExport._computeDisorderStatusForPED() below for detailed explanation of
   *         when "-9" is used and when "1" is used for unaffected nodes
   */
  PedigreeExport.exportAsPED = function(pedigree, idGenerationPreference, selectedMap)
  {
     var output = "";

     var familyID = XWiki.currentDocument.page;

     var idToPedId = PedigreeExport.createNewIDs(pedigree, idGenerationPreference, 20/* max ID length */, true /* forbid non-alphanum */);

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

         var status = PedigreeExport._computeDisorderStatusForPED(selectedMap, pedigree.GG.properties[i]);

         output += status + "\n";
     }

     return output;
  }

  PedigreeExport._computeDisorderStatusForPED = function(selectedMap, nodeProperties)
  {
      if (!selectedMap) {
          return -9;
      }
      //
      // status == "2" means "affected by one or more of the selected abnormalities"
      //               (even if some other abnormalities are explicitly selected as "unaffected")
      //
      // status == "1" only for nodes which have a phenotype explicitly selected as "not present" or
      //               a cancer explicitly selected as "unaffected" or a gene is explicitly selected as "rejected"
      //               AND
      //               only if one type of abnormality is selected, e.g. only "phenotypes" or only "cancers"
      //               or only genes (e.g. if both "phenotypes" and "cancers" are selected, code "1" is never used).
      //
      //               (the reason is it becomes ambiguous what "unaffected" means when multiple abnormalities
      //               are selected, since with the current datamodel for most abnormalities it is hard to
      //               distinguish "missing data" from "not affected")
      //
      // status == "-9" is used in all other cases (for all other abnormalities, including disorders,
      //                and including phenotypes when both phenotypes and some other abnormalities are selected.
      //

      var disordersSelected     = selectedMap.hasOwnProperty("ped-disorders-options");
      var phenotypesSelected    = selectedMap.hasOwnProperty("ped-phenotypes-options");
      var cancersSelected       = selectedMap.hasOwnProperty("ped-cancers-options");
      var candidateGeneSelected = selectedMap.hasOwnProperty("ped-candidateGenes-options");
      var causalGeneSelected    = selectedMap.hasOwnProperty("ped-causalGenes-options");
      var carrierGeneSelected   = selectedMap.hasOwnProperty("ped-carrierGenes-options");
      var genesSelected         = candidateGeneSelected || causalGeneSelected || carrierGeneSelected;

      var atMostOneSelected = function(a, b, c) {
          return a ? (!b && !c) : (!b || !c);
      }

      var useStatus1 = !disordersSelected &&
                       atMostOneSelected(genesSelected, phenotypesSelected, cancersSelected);

      var listsIntersect = function(list1, list2) {
          var intersection = list1.filter(function (item) { return list2.indexOf(item) > -1;});
          return (intersection.length > 0);
      };

      var listIsASubsetOf = function(subset, superset) {
          return subset.every(function (val) { return superset.indexOf(val) >= 0; });
      };

      if (disordersSelected) {
          if (nodeProperties.hasOwnProperty("disorders")) {
              var nodeDisorders = nodeProperties["disorders"];
              if (listsIntersect(selectedMap["ped-disorders-options"], nodeDisorders)) {
                  return 2; // affected
              }
          }
      }

      if (genesSelected) {
          if (nodeProperties.hasOwnProperty("genes")) {
              var nodeGenes = nodeProperties["genes"];
              var candidateGenes = [];
              var solvedGenes    = [];
              var negativeGenes  = [];
              var rejectedCandidateGenes  = [];
              var carrierGenes   = [];
              nodeGenes.each( function(item) {
                  if (candidateGeneSelected && (item.status == "candidate")) {
                      candidateGenes.push(item.id);
                  }
                  if (causalGeneSelected && (item.status == "solved")) {
                      solvedGenes.push(item.id);
                  }
                  if (item.status == "rejected") {
                      negativeGenes.push(item.id);
                  }
                  if (item.status == "rejected_candidate") {
                      rejectedCandidateGenes.push(item.id);
                  }
                  if (carrierGeneSelected && (item.status == "carrier")) {
                      carrierGenes.push(item.id);
                  }
              });
              // if at least one of the possible genes is in the selected list, mark as affected
              if (candidateGeneSelected && listsIntersect(selectedMap["ped-candidateGenes-options"], candidateGenes)) {
                  return 2; // affected
              }
              if (causalGeneSelected && listsIntersect(selectedMap["ped-causalGenes-options"], solvedGenes)) {
                  return 2; // affected
              }
              if (carrierGeneSelected && listsIntersect(selectedMap["ped-carrierGenes-options"], carrierGenes)) {
                  return 2; // affected
              }
              // if not explicitly affected AND all of the selected genes are explicitly not present
              // AND useStatus1 => return 1
              if (useStatus1) {
                  var allSelectedAreNegative = true;
                  if (candidateGeneSelected && !listIsASubsetOf(selectedMap["ped-candidateGenes-options"], rejectedCandidateGenes) && !listIsASubsetOf(selectedMap["ped-candidateGenes-options"], negativeGenes)) {
                      allSelectedAreNegative = false;
                  }
                  if (causalGeneSelected && !listIsASubsetOf(selectedMap["ped-causalGenes-options"], rejectedCandidateGenes) && !listIsASubsetOf(selectedMap["ped-causalGenes-options"], negativeGenes)) {
                      allSelectedAreNegative = false;
                  }
                  if (carrierGeneSelected && !listIsASubsetOf(selectedMap["ped-carrierGenes-options"], rejectedCandidateGenes) && !listIsASubsetOf(selectedMap["ped-carrierGenes-options"], negativeGenes)) {
                      allSelectedAreNegative = false;
                  }
                  if (allSelectedAreNegative) {
                      return 1; // explicitly unaffected
                  }
                  // else: none are affecting, but some of the selected genes are not reported in any status for
                  //       this node => use the default "missing" status
              }
          }
      }

      if (phenotypesSelected) {
          if (nodeProperties.hasOwnProperty("features")) {
              var nodeFeatures = nodeProperties["features"];

              var presentFeatures = [];
              var absentFeatures = [];   //features explicitly absent
              nodeFeatures.each( function(item) {
                  if (item.observed == "yes") {
                      presentFeatures.push(item.id);
                  } else {
                      absentFeatures.push(item.id);
                  }
              });

              // if at least one of the present features is in the selected list, mark as affected
              if (listsIntersect(selectedMap["ped-phenotypes-options"], presentFeatures)) {
                  return 2;
              }
              // if not explicitly affected AND all of the selected features are explicitly not present
              // AND useStatus1 => return 1
              if (useStatus1 && listIsASubsetOf(selectedMap["ped-phenotypes-options"], absentFeatures)) {
                  return 1;  // explicitly unaffected
              }
          }
      }

      if (cancersSelected) {
          if (nodeProperties.hasOwnProperty("cancers")) {
              var nodeCancers = nodeProperties["cancers"];
              var affectedCancers = [];
              var unaffectedCancers = [];
              nodeCancers.each( function(item) {
                  if (item.hasOwnProperty("id")) {
                      if (item.affected) {
                          affectedCancers.push(item.id);
                      } else {
                          unaffectedCancers.push(item.id);
                      }
                  }
              });

              // if at least one of the present cancers is in the selected list, mark as affected
              if (listsIntersect(selectedMap["ped-cancers-options"], affectedCancers)) {
                  return 2;
              }
              // if not explicitly affected AND all of the selected cancers are explicitly not present
              // AND useStatus1 => return 1
              if (useStatus1 && listIsASubsetOf(selectedMap["ped-cancers-options"], unaffectedCancers)) {
                  return 1;  // explicitly unaffected
              }
          }
      }

      return -9; // default: "missing"
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
     var warnMissingDOBUnaff     = false; // BOADICEA recommends age information for unaffected individuals

     for (var i = 0; i <= pedigree.GG.getMaxRealVertexId(); i++) {
         if (!pedigree.GG.isPerson(i)) continue;
         if (pedigree.GG.isPlaceholder(i)) continue;

         var id = idToBoadId[i];

         var name = pedigree.GG.properties[i].hasOwnProperty("fName") ? pedigree.GG.properties[i]["fName"].substring(0,8).replace(/[^A-Za-z0-9]/g, '') : "";
         if (name.length == 0) {
             name = id;
         }

         var proband = (i == editor.getGraph().getProbandId()) ? "1" : "0";

         output += familyID + "\t" + name + "\t" + proband + "\t" + id + "\t";

         // mother & father
         var parents = pedigree.GG.getParents(i);
         if (parents.length > 0) {
             if ( pedigree.GG.properties[parents[0]]["gender"] == "U" &&
                  pedigree.GG.properties[parents[1]]["gender"] == "U" ) {
                 editor.getOkCancelDialogue().showCustomized("Unable to export in BOADICEA format when both parents of any individual are of unknown gender",
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
         var gender = pedigree.GG.properties[i]["gender"];
         if (gender == "F") {
             sex = "F";
         } else if (gender == "U" || gender == "O") {
             // check partner gender(s) and if possible assign the opposite gender
             var possibleGenders = PedigreeExport.guessPossibleGender(dynamicPedigree, i);
             if (!possibleGenders["F"] && !possibleGenders["M"]) {
                 // there is a person which can't be assigned both M and F because both conflict with other partner genders
                 editor.getOkCancelDialogue().showCustomized("Unable to export in BOADICEA format since some genders in pedigree can not be determined",
                                                             "Can't export: some individuals have neither Male nor Female gender", "OK", null );
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

         if (pedigree.GG.getTwinGroupId(i) !== null
             && pedigree.GG.properties[i].hasOwnProperty('monozygotic')
             && pedigree.GG.properties[i].monozygotic) {
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
         var birthDate = new PedigreeDate(pedigree.GG.properties[i]["dob"]);
         if (birthDate && birthDate.isComplete()) {
             // BOADICEA file format does not support fuzzy dates, so get an estimate if only decade is available
             yob = parseInt(birthDate.getAverageYearEstimate());
             var deathDate = new PedigreeDate(pedigree.GG.properties[i]["dod"]);
             if (deathDate && deathDate.isComplete()) {
                 var deathDate = new PedigreeDate(pedigree.GG.properties[i]["dod"]);
                 var lastYearAlive = parseInt(deathDate.getAverageYearEstimate());
                 if (deathDate.toJSDate().getDayOfYear() < birthDate.toJSDate().getDayOfYear()) {
                     lastYearAlive--;
                 }
             } else {
                 var lastYearAlive = new Date().getFullYear();
             }
             age = lastYearAlive - yob;
             if (age < 0) {  // e.g.: Birth: 2007, Death: 2000s
                 age = "0";
             }
         }
         output += age + "\t" + yob + "\t";

         // breast (plus "breast bilateral" column, hardcoded) -> ovarian -> prostate -> pancreatic
         var cancerSequence = [ "HP:0100013", "HP:0100615", "HP:0100787", "HP:0002894" ];

         // index cancers by id
         var cancerIndex = {};
         if (pedigree.GG.properties[i].hasOwnProperty("cancers")) {
             pedigree.GG.properties[i].cancers.forEach(function(cancer){
                 cancerIndex[cancer.id] = cancer;
               });
         }

         var is_affected = false;
         for (var c = 0; c < cancerSequence.length; c++) {
             cancerID = cancerSequence[c];

             if (!cancerIndex.hasOwnProperty(cancerID)) {
                 output += "0\t";
                 if (cancerID == "HP:0100013") {
                     output += "0\t";
                 }
                 continue;
             }

             var cancerData = cancerIndex[cancerID];
             if (!cancerData.affected) {
                 output += "0\t";
             } else {
                 is_affected = true;

                 // TODO: in case of multiple occurences, should the first or the last be reported? Add this question as an export option?
                 // find the earliest age the cancer was diagnosed
                 var age = "AU";
                 var minAge = Infinity;
                 cancerData.qualifiers.forEach(function(qualifier){
                     if (qualifier.numericAgeAtDiagnosis && qualifier.numericAgeAtDiagnosis < minAge) {
                         minAge = qualifier.numericAgeAtDiagnosis;
                     }
                 });
                 if (minAge != Infinity) {
                     age = minAge;
                 }

                 output += age + "\t";

                 // BOADICEA format has a special handling of breast cancer: an extra column for bilateral cancers
                 if (cancerID == "HP:0100013") {
                     var bilateralAge = "0";
                     cancerData.qualifiers.forEach(function(qualifier){
                         if (qualifier.laterality == "bi") {
                             bilateralAge = qualifier.numericAgeAtDiagnosis ? qualifier.numericAgeAtDiagnosis : "AU";
                         }
                     });
                     output += bilateralAge + "\t";
                 }
             }
         }

         if (yob == "0") {
            warnAboutMissingDOB = warnAboutMissingDOB || is_affected;
            warnMissingDOBUnaff = warnMissingDOBUnaff || !is_affected;
         }

         output += "0\t"; // TODO: Genetic test status

         // BRCA1/BRCA2 mutations
         if (pedigree.GG.properties[i].hasOwnProperty("genes")) {
             var genes = pedigree.GG.properties[i].genes;

             var hasGeneWithOneOfStatuses = function(geneName, statusList) {
                 var statusSet = Helpers.toObjectWithTrue(statusList);
                 for (var i = 0; i < genes.length; i++) {
                     var geneObject = genes[i];
                     if (geneObject.gene == geneName && statusSet.hasOwnProperty(geneObject.status)) {
                         return true;
                     }
                 }
                 return false;
             };

             var status = "0";
             if (hasGeneWithOneOfStatuses("BRCA1", ["candidate","solved"])) {
                 status = "1";
             }
             if (hasGeneWithOneOfStatuses("BRCA2", ["candidate","solved"])) {
                 if (status == "1") {
                     status = "3";
                 } else {
                     status = "2";
                 }
             }
             if (status == "0") {
                 // if BRCA1 and BRCA2 are among rejected genes set status to "N"
                 // TODO: what if only one is rejected and another untested?
                 if (hasGeneWithOneOfStatuses("BRCA1", ["rejected","rejected_candidate"]) &&
                     hasGeneWithOneOfStatuses("BRCA2", ["rejected","rejected_candidate"])) {
                     status = "N";
                 }
             }
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

     if (alertUnknownGenderFound || warnAboutMissingDOB || warnMissingDOBUnaff) {
         var warningText = "Pedigree can be exported, but there are warnings:\n\n\n";
         var warnings = [];
         if (alertUnknownGenderFound) {
             warnings.push("BOADICEA format does not support unknown or other genders.\n\n" +
                           "All persons of unknown or other gender were either assigned a gender " +
                           "opposite to their partner's gender or saved as male in the export file");
         }
         if (warnAboutMissingDOB) {
              warnings.push("BOADICEA requires that all individuals with cancer have their year of " +
                            "birth and age at cancer diagnosis specified or estimated\n\n" +
                            "A person with cancer is missing age data, data will be exported but may not be accepted by BOADICEA");
         }
         if(warnMissingDOBUnaff) {
             warnings.push("BOADICEA recommends that all unaffected individuals have their year of birth and" +
                           " year of death, if applicable, specified or estimated." +
                           " Not doing so may lead to an overestimation of risk.");
         }
         if(warnings.length > 1) {
             warnings = warnings.map(function(v, i, a) { return (i + 1) + ") " + v; });
         }
         warningText += warnings.join('\n\n\n');
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
          "aliveandwell":  "aliveAndWell",
          "lifeStatus":    "lifeStatus",
          "deceasedAge":   "deceasedAge",
          "deceasedCause": "deceasedCause",
          "disorders":     "disorders",
          "ethnicities":   "ethnicities",
          "carrierStatus": "carrierStatus",
          "externalID":    "externalId",
          "gender":        "sex",
          "numPersons":    "numPersons",
          "features":      "features",
          "nonstandard_features": "nonstandard_features",
          "genes":         "genes",
          "lostContact":   "lostContact",
          "nodeNumber":    "nodeNumber",
          "cancers":       "cancers",
          "childlessStatus": "childlessStatus",
          "childlessReason": "childlessReason"
      };

  PedigreeExport.internalToJSONRelationshipPropertyMapping = {
          "childlessStatus": "childlessStatus",
          "childlessReason": "childlessReason",
          "consangr":        "consanguinity",
          "broken":          "separated"
      };

  /*
   * Converts property name from internal format to external JSON format - also helps to
   * support aliases for some terms and weed out unsupported terms.
   */
  PedigreeExport.convertPropertyToSimpleJSON = function(internalPropertyName, value) {

      if (!PedigreeExport.internalToJSONPropertyMapping.hasOwnProperty(internalPropertyName)) {
          return null;
      }

      var externalPropertyName = PedigreeExport.internalToJSONPropertyMapping[internalPropertyName];

      if (externalPropertyName == "sex") {
          if (value == "M") {
              value = "male";
          } else if (value == "F") {
              value = "female";
          } else if (value == "O") {
              value = "other";
          } else {
              value = "unknown";
          }
      }

      if (externalPropertyName == "cancers") {
          value = PedigreeExport.convertCancersToSimpleJSON(value);
      }

      return {"propertyName": externalPropertyName, "value": value };
  }

  PedigreeExport.convertCancersToSimpleJSON = function(cancerData) {
      // a set of cancers that simpleJSON format supports
      // note: this list is fixed as it is defined by the PT1.3 implementation
      var simpleJSONCancers = Helpers.toObjectWithTrue(
                                [ "Breast", "Ovarian", "Colon", "Uterus", "Prostate",
                                  "Pancreatic", "Melanoma", "Kidney", "Gastric",
                                  "Lung", "Brain", "Oesophagus", "Thyroid", "Liver",
                                  "Cervix", "Myeloma", "Leukemia" ] );

      var result = {};
      for (var i = 0; i < cancerData.length; i++) {
          var nextCancer = cancerData[i];
          var cancerName = nextCancer["label"];
          if (simpleJSONCancers.hasOwnProperty(cancerName)) {
              var convertedCancerObj = {};
              convertedCancerObj["affected"] = nextCancer.affected;
              if (nextCancer.hasOwnProperty("qualifiers") && nextCancer.qualifiers.length > 0) {
                  convertedCancerObj["ageAtDiagnosis"] = nextCancer.qualifiers[0].ageAtDiagnosis;
                  convertedCancerObj["numericAgeAtDiagnosis"] = nextCancer.qualifiers[0].numericAgeAtDiagnosis;
                  convertedCancerObj["notes"] = nextCancer.qualifiers[0].notes;
              }
              result[cancerName] = convertedCancerObj;
          }
      }
      return result;
  }

  /**
   * Converts property name from internal format to external JSON format.
   */
  PedigreeExport.convertRelationshipProperty = function(internalPropertyName, value) {

      if (!PedigreeExport.internalToJSONRelationshipPropertyMapping.hasOwnProperty(internalPropertyName)) {
          return null;
      }

      var externalPropertyName = PedigreeExport.internalToJSONRelationshipPropertyMapping[internalPropertyName];

      if (externalPropertyName == "consanguinity") {
          if (value != "Y" && value != "N") {
              return null;
          }
      }
      return {"propertyName": externalPropertyName, "value": value };
  }

  /**
   * Tries to guess a gender based on partner gender - required by BOADICEA which does not support
   * Unknown and Other genders.
   */
  PedigreeExport.guessPossibleGender = function(dynamicPedigree, v)
  {
        // returns: - any gender if no partners or all partners are of unknown genders;
        //          - opposite of the partner gender if partner genders do not conflict
        var possible = {"M": true, "F": true};

        var partners = dynamicPedigree.DG.GG.getAllPartners(v);

        for (var i = 0; i < partners.length; i++) {
            var partnerGender = dynamicPedigree.getGender(partners[i]);
            if (partnerGender != "U" && partnerGender != "O") {
                possible[partnerGender] = false;
            }
        }
        return possible;
  },

  /**
   * idGenerationPreference: {"newid"|"external"|"name"}, default: "newid"
   */
  PedigreeExport.createNewIDs = function(pedigree, idGenerationPreference, maxLength, forbidNonAlphaNum) {
      if (!idGenerationPreference) {
          idGenerationPreference = "newid";
      }

      var idToNewId = {};
      var usedIDs   = {};

      var nextUnusedID = 1;

      for (var i = 0; i <= pedigree.GG.getMaxRealVertexId(); i++) {
          if (!pedigree.GG.isPerson(i)) continue;
          if (pedigree.GG.isPlaceholder(i)) continue;

          var alternativeId = "";
          if (idGenerationPreference == "external" && pedigree.GG.properties[i].hasOwnProperty("externalID")) {
              alternativeId = pedigree.GG.properties[i]["externalID"].replace(/\s/g, '');
          } else if (idGenerationPreference == "name" && pedigree.GG.properties[i].hasOwnProperty("fName")) {
              alternativeId = pedigree.GG.properties[i]["fName"].replace(/\s/g, '');
          }
          if (forbidNonAlphaNum) {
              alternativeId = alternativeId.replace(/[^A-Za-z0-9]/g, '');  // can't use \W since that allows "_"
          }
          if (alternativeId.length > 0) {
              var id = alternativeId;
          } else {
              var id = String(nextUnusedID++);
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

  return PedigreeExport;
});
