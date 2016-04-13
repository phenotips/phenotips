/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.phenotips.studies.family.internal;

import org.phenotips.configuration.RecordConfigurationManager;
import org.phenotips.studies.family.Pedigree;
import org.phenotips.studies.family.PedigreeProcessor;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * Converts the JSON generated by the pedigree into the default format accepted by PhenoTips.
 *
 * @version $Id$
 * @since 1.2RC1
 */
@Component
public class PedigreeProcessorImpl implements PedigreeProcessor
{
    private static final String JSON_KEY_FEATURES = "features";

    private static final String JSON_KEY_NON_STANDARD_FEATURES = "nonstandard_features";

    private static final String JSON_KEY_FAMILY_HISTORY = "family_history";

    @Inject
    private Logger logger;

    @Inject
    private RecordConfigurationManager configurationManager;

    @Inject
    @Named("hpo")
    private Vocabulary hpoService;

    @Inject
    @Named("omim")
    private Vocabulary omimService;

    /**
     * Returns a list of Phenotips JSONs for each patient found in pedigree.
     *
     * @param pedigree a Pedigree object
     * @return a list of patient JSONs. if pedigree is not valid returns an empty list.
     */
    @Override
    public List<JSONObject> convert(Pedigree pedigree)
    {
        List<JSONObject> convertedPatients = new LinkedList<>();

        if (pedigree != null) {
            JSONObject data = pedigree.getData();

            String versionKey = "JSON_version";
            if (data.has(versionKey)
                && !StringUtils.equalsIgnoreCase(data.getString(versionKey), "1.0"))
            {
                this.logger.warn("The version of the pedigree JSON differs from the expected.");
            }

            List<JSONObject> patientJson = pedigree.extractPatientJSONProperties();

            DateFormat useDateFormat = this.getDateFormat();

            for (JSONObject singlePatient : patientJson) {
                convertedPatients.add(patientJsonToObject(singlePatient, useDateFormat));
            }
        }

        return convertedPatients;
    }

    private JSONObject patientJsonToObject(JSONObject externalPatient, DateFormat useDateFormat)
    {
        JSONObject phenotipsPatient = new JSONObject();

        try {
            phenotipsPatient = exchangeIds(externalPatient, phenotipsPatient);
            phenotipsPatient = exchangeBasicPatientData(externalPatient, phenotipsPatient);
            phenotipsPatient = exchangeDates(externalPatient, phenotipsPatient, useDateFormat);
            phenotipsPatient = exchangePhenotypes(externalPatient, phenotipsPatient, this.hpoService, this.logger);
            phenotipsPatient = exchangeDisorders(externalPatient, phenotipsPatient, this.omimService, this.logger);
            phenotipsPatient = exchangeFamilyHistory(externalPatient, phenotipsPatient);
        } catch (Exception ex) {
            this.logger.error("Could not convert patient. {}", ex.getMessage());
        }

        return phenotipsPatient;
    }

    private DateFormat getDateFormat()
    {
        return new SimpleDateFormat(this.configurationManager.getActiveConfiguration().getISODateFormat());
    }

    private static JSONObject exchangeFamilyHistory(JSONObject pedigreePatient, JSONObject phenotipsPatientJSON)
    {
        phenotipsPatientJSON.put(JSON_KEY_FAMILY_HISTORY, pedigreePatient.opt(JSON_KEY_FAMILY_HISTORY));
        return phenotipsPatientJSON;
    }

    private static JSONObject exchangeIds(JSONObject pedigreePatient, JSONObject phenotipsPatientJSON)
    {
        phenotipsPatientJSON.put("id", pedigreePatient.opt("phenotipsId"));
        phenotipsPatientJSON.put("external_id", pedigreePatient.opt("externalID"));
        return phenotipsPatientJSON;
    }

    private static JSONObject exchangeBasicPatientData(JSONObject pedigreePatient, JSONObject phenotipsPatientJSON)
    {
        JSONObject name = new JSONObject();
        name.put("first_name", pedigreePatient.opt("fName"));
        name.put("last_name", pedigreePatient.opt("lName"));

        phenotipsPatientJSON.put("sex", pedigreePatient.opt("gender"));
        phenotipsPatientJSON.put("patient_name", name);
        return phenotipsPatientJSON;
    }

    private static JSONObject exchangeDates(JSONObject pedigreePatient,
        JSONObject phenotipsPatientJSON, DateFormat format)
    {
        String dob = "dob";
        String dod = "dod";
        if (pedigreePatient.has(dob)) {
            phenotipsPatientJSON.put("date_of_birth", format.format(
                PedigreeProcessorImpl.pedigreeDateToDate(pedigreePatient.getJSONObject(dob))
                ));
        }
        if (pedigreePatient.has(dod)) {
            phenotipsPatientJSON.put("date_of_death", format.format(
                PedigreeProcessorImpl.pedigreeDateToDate(pedigreePatient.getJSONObject(dod))
                ));
        }
        return phenotipsPatientJSON;
    }

    private static JSONObject exchangePhenotypes(JSONObject pedigreePatient,
        JSONObject phenotipsPatientJSON, Vocabulary hpoService, Logger logger)
    {
        JSONArray pedigreeFeatures = pedigreePatient.optJSONArray(JSON_KEY_FEATURES);
        if (pedigreeFeatures != null) {
            phenotipsPatientJSON.put(JSON_KEY_FEATURES, pedigreeFeatures);
        }

        JSONArray pedigreeNonStdFeatures = pedigreePatient.optJSONArray(JSON_KEY_NON_STANDARD_FEATURES);
        if (pedigreeNonStdFeatures != null) {
            phenotipsPatientJSON.put(JSON_KEY_NON_STANDARD_FEATURES, pedigreeNonStdFeatures);
        }

        return phenotipsPatientJSON;
    }

    private static JSONObject exchangeDisorders(JSONObject pedigreePatient,
        JSONObject phenotipsPatientJSON, Vocabulary omimService, Logger logger)
    {
        String disordersKey = "disorders";
        JSONArray internalTerms = new JSONArray();
        JSONArray externalTerms = pedigreePatient.optJSONArray(disordersKey);

        if (externalTerms != null) {
            for (Object termIdObj : externalTerms) {
                try {
                    VocabularyTerm term = omimService.getTerm(termIdObj.toString());
                    if (term != null) {
                        internalTerms.put(term.toJSON());
                    }
                } catch (Exception ex) {
                    logger.error("Could not convert disorder {} from pedigree JSON to patient JSON", termIdObj);
                }
            }
        }

        phenotipsPatientJSON.put(disordersKey, internalTerms);
        return phenotipsPatientJSON;
    }

    /**
     * Used for converting a pedigree date to a {@link Date}.
     *
     * @param pedigreeDate cannot be null. Must contain at least the decade field.
     */
    private static Date pedigreeDateToDate(JSONObject pedigreeDate)
    {
        String yearString = "year";
        String monthString = "month";
        String dayString = "day";
        DateTime jodaDate;
        if (pedigreeDate.has(yearString)) {
            Integer year = Integer.parseInt(pedigreeDate.getString(yearString));
            Integer month = pedigreeDate.has(monthString)
                ? Integer.parseInt(pedigreeDate.getString(monthString)) : 1;
            Integer day = pedigreeDate.has(dayString)
                ? Integer.parseInt(pedigreeDate.getString(dayString)) : 1;
            jodaDate = new DateTime(year, month, day, 0, 0);
        } else {
            String decade = pedigreeDate.getString("decade").substring(0, 4);
            jodaDate = new DateTime(Integer.parseInt(decade), 1, 1, 0, 0);
        }
        return new Date(jodaDate.getMillis());
    }
}
