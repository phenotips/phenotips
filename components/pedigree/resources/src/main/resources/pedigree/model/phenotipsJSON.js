define([
      "pedigree/model/helpers",
      "pedigree/pedigreeDate"
    ], function(
      Helpers,
      PedigreeDate
    ){

    PhenotipsJSON = function () {
    };

    PhenotipsJSON.supportedPhenotipsVersionsRegexps = {
        "any 1.3": /^1.3/,
        "any 1.4": /^1.4/
    };

    /**
     * Returns an object in PhenoTips Patient JSON format representing all properties of a person that
     * are supported by PhenoTips (by updating the given initial JSON object).
     *
     * Since some of the properties in Phenotips JSON are not stored internally, need to use the original JSON
     * as a base and only update those fields which have equivalents in internal format
     */
    PhenotipsJSON.internalToPhenotipsJSON = function(internalProperties, relationshipProperties)
    {
        // if a PT patient is linked to this node, there may be properties which are not stored in pedigree,
        // which need to be preserved. Otherwise we start from a blank state and just convert what we have in
        // pedigree to PT JSON format
        var result = internalProperties.hasOwnProperty("phenotipsId")
                     ? Helpers.cloneObject(editor.getPatientRecordData().get(internalProperties.phenotipsId))
                     : {};

        result.phenotips_version = editor.getPhenotipsVersion();

        if (internalProperties.hasOwnProperty("phenotipsId")) {
            result.id = internalProperties.phenotipsId;
        }

        result.sex = internalProperties.gender;
        if (result.sex != "M" && result.sex != "F" && result.sex != "U") {
            result.sex = "U";  // pedigree supports more genders than PhenoTips as of right now
        }

        // ethnicities: not touched, since the meaning is different

        var setValueOrDefault = function(internalKey, externalKey, valueIfNoInternalKey, externalObject) {
            if (!externalObject) {
                externalObject = result;
            }
            if (internalProperties.hasOwnProperty(internalKey)) {
                externalObject[externalKey] = internalProperties[internalKey];
            } else {
                externalObject[externalKey] = valueIfNoInternalKey;
            }
        }

        setValueOrDefault("externalID", "external_id", "");

        result.patient_name = {};
        setValueOrDefault("fName", "first_name", "", result.patient_name);
        setValueOrDefault("lName", "last_name", "", result.patient_name);

        setValueOrDefault("lifeStatus", "life_status", "alive");

        setValueOrDefault("dob", "date_of_birth", {});
        setValueOrDefault("dod", "date_of_death", {});

        setValueOrDefault("genes", "genes", []);
        // TODO: if a gene is removed remove all of its variants as well?

        setValueOrDefault("features", "features", []);
        setValueOrDefault("nonstandard_features", "nonstandard_features", []);

        var outputDisorders = [];
        var disorders = internalProperties.hasOwnProperty("disorders")
                        ? internalProperties["disorders"]
                        : [];
        for (var i = 0; i < disorders.length; i++) {
            var disorderID = disorders[i];
            var disorderName = editor.getDisorderLegend().getName(disorderID);

            // TODO: review logic here
            var match = disorderID.match(/^(\d+)$/);
            if (disorderName != disorderID && match) {
                // must be a standard term
                disorderID = "MIM:" + match[1];
                outputDisorders.push( {"id": disorderID, "label": disorderName} );
            } else {
                outputDisorders.push( {"label": disorderName} );
            }
        }
        result["disorders"] = outputDisorders;

        if (relationshipProperties && relationshipProperties.consangr && relationshipProperties.consangr === "Y") {
            var familyHistory = result.hasOwnProperty("family_history")
                                ? result["family_history"]
                                : {};
            // if on the patient form the patient was marked as consanguineous the status will be kept as such,
            // otherwise it will be set to true if the parent relationship is consanguineous (and left as is
            // - "no" or "undefined" - otherwise)
            familyHistory["consanguinity"] = true;
            result["family_history"] = familyHistory;
        }

        // TODO: convert Alive&Well into "clinicalStatus"?

        return result;
    };

    /**
     * Returns an object representing all properties of a person in internal pedigree format, extending the
     * given property set using data from the given PhenoTips Patient JSON.
     *
     * Note: the reason existing properties are needed as an input is because some of the properties
     * are available only in pedigree, and some may depend on both pedigree and patientJSON data
     * (e.g. ethnicities, which are merged)
     */
    PhenotipsJSON.phenotipsJSONToInternal = function(patientJSON, pedigreeOnlyProperties)
    {
        // TODO: multiple converters for multiple (old) versions

        if (patientJSON.phenotips_version && !PhenotipsJSON.isVersionSupported(patientJSON.phenotips_version)) {
            console.log("Possibly unsuported Patient JSON version: " + patientJSON.phenotips_version);
        }

        var result = Helpers.cloneObject(pedigreeOnlyProperties);

        // Fields which are loaded from the patient document are:
        // - id (important and special, as presense of an ID indicates this patient is linked to a PhenoTips patient)
        // - first_name
        // - last_name
        // - sex
        // - date_of_birth
        // - date_of_death
        // - life_status
        // - external_id
        // - features + nonstandard_features
        // - disorders
        // - genes
        // - maternal_ethnicity + paternal_ethnicity (merged with own ethnicities entered in pedigree editor)
        // - family_history

        if (patientJSON.hasOwnProperty("id")) {
            result["phenotipsId"] = patientJSON.id;
        }

        if (patientJSON.hasOwnProperty("patient_name")) {
            if (patientJSON.patient_name.hasOwnProperty("first_name")) {
                result.fName = patientJSON.patient_name.first_name;
            } else {
                result.fName = "";
            }
            if (patientJSON.patient_name.hasOwnProperty("last_name")) {
                result.lName = patientJSON.patient_name.last_name;
            } else {
                result.lName = "";
            }
        }

        if (patientJSON.hasOwnProperty("sex")) {
            result.gender = patientJSON.sex;
        } else {
            result.gender = "U";
        }

        if (patientJSON.hasOwnProperty("date_of_birth")) {
            var birthDate = new PedigreeDate(patientJSON.date_of_birth);
            result.dob = birthDate.getSimpleObject();
        } else {
            delete result.dob;
        }
        if (patientJSON.hasOwnProperty("date_of_death")) {
            var deathDate = new PedigreeDate(patientJSON.date_of_death);
            result.dod = deathDate.getSimpleObject();
            if (deathDate.isSet()) {
                delete result.aliveandwell;
            }
        } else {
            delete result.dod;
        }
        if (patientJSON.hasOwnProperty("life_status")) {
            var lifeStatus = patientJSON["life_status"];
            result.lifeStatus = lifeStatus;
            if (lifeStatus != "alive") {
                // if not removed, it will overwrite the life status to 'alive' and thus remove death date
                delete result.aliveandwell;
            }
        } else {
            delete result.lifeStatus;
        }

        if (patientJSON.hasOwnProperty("ethnicity")) {
            // e.g.: "ethnicity":{"maternal_ethnicity":["Yugur"],"paternal_ethnicity":[]}
            var ethnicities = [];
            if (patientJSON.ethnicity.hasOwnProperty("maternal_ethnicity")) {
                ethnicities = patientJSON.ethnicity.maternal_ethnicity.slice(0);
            }
            if (patientJSON.ethnicity.hasOwnProperty("paternal_ethnicity")) {
                ethnicities = ethnicities.concat(patientJSON.ethnicity.paternal_ethnicity.slice(0));
            }
            if (ethnicities.length > 0) {
                result.ethnicities = Helpers.filterUnique(ethnicities);
            }
        }

        if (patientJSON.hasOwnProperty("external_id")) {
            result.externalID = patientJSON.external_id;
        } else {
            delete result.externalID;
        }

        if (patientJSON.hasOwnProperty("features") && patientJSON.features.length > 0) {
            result.features = patientJSON.features;
        } else {
            delete result.features;
        }

        if (patientJSON.hasOwnProperty("nonstandard_features") && patientJSON.nonstandard_features.length > 0) {
            result.nonstandard_features = patientJSON.nonstandard_features;
        } else {
            delete result.nonstandard_features;
        }

        var disorders = [];
        if (patientJSON.hasOwnProperty("disorders")) {
            // e.g.: "disorders":[{"id":"MIM:120970","label":"#120970 CONE-ROD DYSTROPHY 2; CORD2 ;;CONE-ROD DYSTROPHY; CORD;; CONE-ROD RETINAL DYSTROPHY; CRD; CRD2;; RETINAL CONE-ROD DYSTROPHY; RCRD2"},{"id":"MIM:190685","label":"#190685 DOWN SYNDROME TRISOMY 21, INCLUDED;; DOWN SYNDROME CHROMOSOME REGION, INCLUDED; DCR, INCLUDED;; DOWN SYNDROME CRITICAL REGION, INCLUDED; DSCR, INCLUDED;; TRANSIENT MYELOPROLIFERATIVE DISORDER OF DOWN SYNDROME, INCLUDED;; LEUKEMIA, MEGAKARYOBLASTIC, OF DOWN SYNDROME, INCLUDED"}]
            for (var i = 0; i < patientJSON.disorders.length; i++) {
                var disorderID = patientJSON.disorders[i].id || patientJSON.disorders[i].label;
                var match = disorderID.match(/^MIM:(\d+)$/);
                match && (disorderID = match[1]);
                disorders.push(disorderID);
            }
        }
        if (disorders.length > 0) {
            result.disorders = disorders;
        } else {
            delete result.disorders;
        }

        if (patientJSON.hasOwnProperty("genes")) {
            result.genes = patientJSON.genes;
        } else {
            delete result.genes;
        }

        if (patientJSON.hasOwnProperty("family_history")) {
            // stored so that on save consanguinity status can be set via family_history without losing
            // previous family_history
            result["family_history"] = patientJSON["family_history"];
        }

        return result;
    };

    PhenotipsJSON.isVersionSupported = function(phenotipsJSONVersion)
    {
        for (var versionRegExp in PhenotipsJSON.supportedPhenotipsVersionsRegexps) {
            if (PhenotipsJSON.supportedPhenotipsVersionsRegexps.hasOwnProperty(versionRegExp)) {
                if (PhenotipsJSON.supportedPhenotipsVersionsRegexps[versionRegExp].test(phenotipsJSONVersion)) {
                    return true;
                }
            }
        }
        return false;
    };

    return PhenotipsJSON;
});