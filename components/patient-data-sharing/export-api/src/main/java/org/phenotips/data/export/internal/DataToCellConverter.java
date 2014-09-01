/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.data.export.internal;

import org.phenotips.data.Feature;
import org.phenotips.data.FeatureMetadatum;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.ontology.internal.solr.SolrOntologyTerm;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * Each of functions need to be written with certain specification. Body producing functions must return null if they
 * produce no cells, and they must not remove from {@link #enabledHeaderIdsBySection}.
 */
public class DataToCellConverter
{
    private Map<String, Set<String>> enabledHeaderIdsBySection = new HashMap<String, Set<String>>();

    private ConversionHelpers phenotypeHelper;

    private ConversionHelpers prenatalPhenotypeHelper;

    public static final Integer charactersPerLine = 100;

    public void phenotypeSetup(Set<String> enabledFields) throws Exception
    {
        String sectionName = "phenotype";
        String[] fieldIds =
            { "phenotype", "phenotype_code", "phenotype_combined", "phenotype_code_meta", "phenotype_meta",
                "negative_phenotype", "phenotype_by_section" };
        /* FIXME These will not work properly in different configurations */
        String[][] headerIds =
            { { "phenotype" }, { "code" }, { "phenotype", "code" }, { "meta_code" }, { "meta" }, { "negative" },
                { "category" } };
        Set<String> present = new HashSet<String>();

        int counter = 0;
        for (String fieldId : fieldIds) {
            if (enabledFields.remove(fieldId)) {
                for (String headerId : headerIds[counter]) {
                    present.add(headerId);
                }
            }
            counter++;
        }
        enabledHeaderIdsBySection.put(sectionName, present);

        phenotypeHelper = new ConversionHelpers();
        phenotypeHelper
            .featureSetUp(present.contains("phenotype"), present.contains("negative"), present.contains("category"));
    }

    public DataSection phenotypeHeader() throws Exception
    {
        String sectionName = "phenotype";
        Set<String> present = enabledHeaderIdsBySection.get(sectionName);
        if (present.isEmpty()) {
            return null;
        }

        DataSection section = new DataSection(sectionName);
        List<String> orderedHeaderIds = new LinkedList<String>();
        orderedHeaderIds.add("category");
        orderedHeaderIds.add("phenotype");
        orderedHeaderIds.add("code");
        orderedHeaderIds.add("meta");
        orderedHeaderIds.add("meta_code");
        List<String> orderedHeaderNames = new LinkedList<String>();
        orderedHeaderNames.add("Category");
        orderedHeaderNames.add("Label");
        orderedHeaderNames.add("ID");
        orderedHeaderNames.add("Meta");
        orderedHeaderNames.add("ID");

        int counter = 0;
        int hX = 0;
        if (present.contains("phenotype") && present.contains("negative")) {
            DataCell cell = new DataCell("Present", hX, 1, StyleOption.HEADER);
            section.addCell(cell);
            hX++;
        }
        for (String headerId : orderedHeaderIds) {
            if (!present.contains(headerId)) {
                counter++;
                continue;
            }
            DataCell cell = new DataCell(orderedHeaderNames.get(counter), hX, 1, StyleOption.HEADER);
            section.addCell(cell);
            hX++;
            counter++;
        }
        DataCell sectionHeader = new DataCell("Phenotype", 0, 0, StyleOption.HEADER);
        sectionHeader.addStyle(StyleOption.LARGE_HEADER);
        section.addCell(sectionHeader);

        return section;
    }

    public DataSection phenotypeBody(Patient patient) throws Exception
    {
        String sectionName = "phenotype";
        Set<String> present = enabledHeaderIdsBySection.get(sectionName);
        if (present == null || present.isEmpty()) {
            return null;
        }

        Boolean bothTypes = present.contains("phenotype") && present.contains("negative");
        DataSection section = new DataSection(sectionName);

        int x;
        int y = 0;
        Set<? extends Feature> features = patient.getFeatures();
        phenotypeHelper.newPatient();
        Boolean categoriesEnabled = present.contains("category");
        List<Feature> sortedFeatures;
        Map<String, String> sectionFeatureLookup = new HashMap<String, String>();
        if (!categoriesEnabled) {
            sortedFeatures = phenotypeHelper.sortFeaturesSimple(features);
        } else {
            sortedFeatures = phenotypeHelper.sortFeaturesWithSections(features);
            sectionFeatureLookup = phenotypeHelper.getSectionFeatureTree();
        }

        Boolean lastStatus = false;
        String lastSection = "";
        for (Feature feature : sortedFeatures) {
            x = 0;

            if (bothTypes && lastStatus != feature.isPresent()) {
                lastStatus = feature.isPresent();
                lastSection = "";
                DataCell cell = new DataCell(lastStatus ? "Yes" : "No", x, y);
                if (!lastStatus) {
                    cell.addStyle(StyleOption.YES_NO_SEPARATOR);
                }
                cell.addStyle(lastStatus ? StyleOption.YES : StyleOption.NO);
                section.addCell(cell);
            }
            if (bothTypes) {
                x++;
            }
            if (categoriesEnabled) {
                String currentSection = sectionFeatureLookup.get(feature.getId());
                if (!StringUtils.equals(currentSection, lastSection)) {
                    DataCell cell = new DataCell(currentSection, x, y);
                    section.addCell(cell);
                    lastSection = currentSection;
                }
                x++;
            }
            if (present.contains("phenotype")) {
                DataCell cell = new DataCell(feature.getName(), x, y, StyleOption.FEATURE_SEPARATOR);
                section.addCell(cell);
                x++;
            }
            if (present.contains("code")) {
                DataCell cell = new DataCell(feature.getId(), x, y, StyleOption.FEATURE_SEPARATOR);
                section.addCell(cell);
                x++;
            }
            if (present.contains("meta") || present.contains("meta_code")) {
                int mX = x;
                int mCX = x + 1;
                Collection<? extends FeatureMetadatum> featureMetadatum = feature.getMetadata().values();
                Boolean metaPresent = featureMetadatum.size() > 0;
                for (FeatureMetadatum meta : featureMetadatum) {
                    if (present.contains("meta")) {
                        DataCell cell = new DataCell(meta.getName(), mX, y);
                        section.addCell(cell);
                    }
                    if (present.contains("meta_code")) {
                        DataCell cell = new DataCell(meta.getId(), mCX, y);
                        section.addCell(cell);
                    }
                    y++;
                }
                if (metaPresent) {
                    y--;
                }
            }
            y++;
        }

//        section.finalizeToMatrix();
        return section;
    }

    public DataSection idHeader(Set<String> enabledFields) throws Exception
    {
        String sectionName = "id";
        Set<String> present = new HashSet<String>();
        if (enabledFields.remove("doc.name")) {
            present.add("external_id");
        } else {
            return null;
        }
        DataSection section = new DataSection(sectionName);
        enabledHeaderIdsBySection.put(sectionName, present);
        DataCell cell = new DataCell("Identifier", 0, 0, StyleOption.HEADER);
        cell.addStyle(StyleOption.LARGE_HEADER);
        section.addCell(cell);

//        section.finalizeToMatrix();
        return section;
    }

    public DataSection idBody(Patient patient) throws Exception
    {
        String sectionName = "id";
        Set<String> present = enabledHeaderIdsBySection.get(sectionName);
        if (present == null || present.isEmpty()) {
            return null;
        }
        DataSection section = new DataSection(sectionName);

        DataCell cell = new DataCell(patient.getExternalId(), 0, 0);
        section.addCell(cell);

//        section.finalizeToMatrix();
        return section;
    }

    public DataSection patientInfoHeader(Set<String> enabledFields) throws Exception
    {
        String sectionName = "patientInfo";
        Map<String, String> fieldToHeaderMap = new HashMap<String, String>();
        fieldToHeaderMap.put("first_name", "First Name");
        fieldToHeaderMap.put("last_name", "Last Name");
        fieldToHeaderMap.put("date_of_birth", "Date of birth");
        fieldToHeaderMap.put("gender", "Sex");
        fieldToHeaderMap.put("indication_for_referral", "Indication for referral");

        Set<String> present = new HashSet<String>();
        for (String fieldId : fieldToHeaderMap.keySet()) {
            if (enabledFields.remove(fieldId)) {
                present.add(fieldId);
            }
        }
        enabledHeaderIdsBySection.put(sectionName, present);

        DataSection headerSection = new DataSection(sectionName);
        if (present.isEmpty()) {
            return null;
        }

        int x = 0;
        for (String fieldId : present) {
            DataCell headerCell = new DataCell(fieldToHeaderMap.get(fieldId), x, 1, StyleOption.HEADER);
            headerSection.addCell(headerCell);
            x++;
        }
        DataCell headerCell = new DataCell("Patient Information", 0, 0, StyleOption.LARGE_HEADER);
        headerCell.addStyle(StyleOption.HEADER);
        headerSection.addCell(headerCell);

        return headerSection;
    }

    public DataSection patientInfoBody(Patient patient)
    {
        String sectionName = "patientInfo";
        Set<String> present = enabledHeaderIdsBySection.get(sectionName);
        if (present == null || present.isEmpty()) {
            return null;
        }

        DataSection bodySection = new DataSection(sectionName);
        Integer x = 0;
        if (present.contains("first_name")) {
            String firstName = patient.<String>getData("patientName").get("first_name");
            DataCell cell = new DataCell(firstName, x, 0);
            bodySection.addCell(cell);
            x++;
        }
        if (present.contains("last_name")) {
            String lastName = patient.<String>getData("patientName").get("last_name");
            DataCell cell = new DataCell(lastName, x, 0);
            bodySection.addCell(cell);
            x++;
        }
        if (present.contains("date_of_birth")) {
            SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
            Date dob = patient.<Date>getData("dates").get("date_of_birth");
            DataCell cell = new DataCell(format.format(dob), x, 0);
            bodySection.addCell(cell);
            x++;
        }
        if (present.contains("gender")) {
            String sex = patient.<String>getData("sex").getValue();
            DataCell cell = new DataCell(sex, x, 0);
            bodySection.addCell(cell);
            x++;
        }
        if (present.contains("indication_for_referral")) {
            String indicationForReferral = patient.<String>getData("notes").get("indication_for_referral");
            indicationForReferral = wrapString(indicationForReferral, this.charactersPerLine);
            DataCell cell = new DataCell(indicationForReferral, x, 0);
            bodySection.addCell(cell);
            x++;
        }

        return bodySection;
    }

    public DataSection familyHistoryHeader(Set<String> enabledFields) throws Exception
    {
        String sectionName = "familyHistory";
        Map<String, String> fieldToHeaderMap = new LinkedHashMap<String, String>();
        fieldToHeaderMap.put("global_mode_of_inheritance", "Mode of inheritance");
        fieldToHeaderMap.put("miscarriages", "3+ miscarriages");
        fieldToHeaderMap.put("consanguinity", "Consanguinity");
        fieldToHeaderMap.put("maternal_ethnicity", "Maternal");
        fieldToHeaderMap.put("paternal_ethnicity", "Paternal");

        Set<String> present = new LinkedHashSet<String>();
        for (String fieldId : fieldToHeaderMap.keySet()) {
            if (enabledFields.remove(fieldId)) {
                present.add(fieldId);
            }
        }
        enabledHeaderIdsBySection.put(sectionName, present);
        if (present.isEmpty()) {
            return null;
        }

        DataSection headerSection = new DataSection(sectionName);

        int bottomY = 1;
        int ethnicityOffset = 0;
        if (present.contains("maternal_ethnicity") || present.contains("paternal_ethnicity")) {
            bottomY = 2;
            if (fieldToHeaderMap.containsKey("maternal_ethnicity") &&
                fieldToHeaderMap.containsKey("paternal_ethnicity"))
            {
                ethnicityOffset = 2;
            } else {
                ethnicityOffset = 1;
            }
        }
        int x = 0;
        for (String fieldId : present) {
            DataCell headerCell = new DataCell(fieldToHeaderMap.get(fieldId), x, bottomY, StyleOption.HEADER);
            headerSection.addCell(headerCell);
            x++;
        }
        if (ethnicityOffset > 0) {
            DataCell headerCell = new DataCell("Ethnicity", x - ethnicityOffset, 1, StyleOption.HEADER);
            headerSection.addCell(headerCell);
        }
        DataCell headerCell = new DataCell("Family History", 0, 0, StyleOption.LARGE_HEADER);
        headerCell.addStyle(StyleOption.HEADER);
        headerSection.addCell(headerCell);

        return headerSection;
    }

    public DataSection familyHistoryBody(Patient patient)
    {
        String sectionName = "familyHistory";
        Set<String> present = enabledHeaderIdsBySection.get(sectionName);
        if (present == null || present.isEmpty()) {
            return null;
        }

        DataSection bodySection = new DataSection(sectionName);
        PatientData<Integer> familyHistory = patient.getData("familyHistory");
        PatientData<List<String>> ethnicities = patient.getData("ethnicity");
        Integer x = 0;
        if (present.contains("global_mode_of_inheritance")) {
            String mode =
                patient.<SolrOntologyTerm>getData("global-qualifiers").get("global_mode_of_inheritance").getName();
            DataCell cell = new DataCell(mode, x, 0);
            bodySection.addCell(cell);
            x++;
        }
        if (present.contains("consanguinity")) {
            Integer consanguinity = familyHistory.get("consanguinity");
            DataCell cell = new DataCell(ConversionHelpers.integerToStrBool(consanguinity), x, 0);
            bodySection.addCell(cell);
            x++;
        }
        if (present.contains("miscarriages")) {
            Integer miscarriages = familyHistory.get("miscarriages");
            DataCell cell = new DataCell(ConversionHelpers.integerToStrBool(miscarriages), x, 0);
            bodySection.addCell(cell);
            x++;
        }
        if (present.contains("maternal_ethnicity")) {
            List<String> maternalEthnicity = ethnicities.get("maternal_ethnicity");
            int y = 0;
            for (String mEthnicity : maternalEthnicity) {
                DataCell cell = new DataCell(mEthnicity, x, y);
                bodySection.addCell(cell);
                y++;
            }
            x++;
        }
        if (present.contains("paternal_ethnicity")) {
            List<String> paternalEthnicity = ethnicities.get("paternal_ethnicity");
            int y = 0;
            for (String pEthnicity : paternalEthnicity) {
                DataCell cell = new DataCell(pEthnicity, x, y);
                bodySection.addCell(cell);
                y++;
            }
            x++;
        }

        return bodySection;
    }

    public DataSection prenatalPerinatalHistoryHeader(Set<String> enabledFields) throws Exception
    {
        String sectionName = "prenatalPerinatalHistory";
        Map<String, String> fieldToHeaderMap = new LinkedHashMap<String, String>();
        fieldToHeaderMap.put("assistedReproduction_fertilityMeds", "Fertility medication");
        fieldToHeaderMap.put("ivf", "In vitro fertilization");
        fieldToHeaderMap.put("assistedReproduction_surrogacy", "Surrogacy");
        fieldToHeaderMap.put("apgar1", "1 min");
        fieldToHeaderMap.put("apgar5", "5 min");

        Set<String> present = new LinkedHashSet<String>();
        for (String fieldId : fieldToHeaderMap.keySet()) {
            if (enabledFields.remove(fieldId)) {
                present.add(fieldId);
            }
        }
        enabledHeaderIdsBySection.put(sectionName, present);
        if (present.isEmpty()) {
            return null;
        }

        DataSection headerSection = new DataSection(sectionName);

        List<String> apgarFields = new LinkedList<String>(Arrays.asList("apgar1", "apgar2"));
        List<String> assitedReproductionFields = new LinkedList<String>(
            Arrays.asList("ivf", "assistedReproduction_surrogacy", "assistedReproduction_fertilityMeds"));
        apgarFields.retainAll(present);
        assitedReproductionFields.retainAll(present);
        int apgarOffset = apgarFields.size();
        int assistedReproductionOffset = apgarOffset + assitedReproductionFields.size() + 1;
        int bottomY = (apgarOffset > 0 || assistedReproductionOffset > 0) ? 2 : 1;

        int x = 0;
        for (String fieldId : present) {
            DataCell headerCell = new DataCell(fieldToHeaderMap.get(fieldId), x, bottomY, StyleOption.HEADER);
            headerSection.addCell(headerCell);
            x++;
        }
        if (apgarOffset > 0) {
            DataCell headerCell = new DataCell("APGAR Score", x - apgarOffset, 1, StyleOption.HEADER);
            headerSection.addCell(headerCell);
            x++;
        }
        if (assistedReproductionOffset > 0) {
            DataCell headerCell =
                new DataCell("Assisted Reproduction", x - assistedReproductionOffset, 1, StyleOption.HEADER);
            headerSection.addCell(headerCell);
        }
        DataCell headerCell = new DataCell("Prenatal and Perinatal History", 0, 0, StyleOption.LARGE_HEADER);
        headerCell.addStyle(StyleOption.HEADER);
        headerSection.addCell(headerCell);

        return headerSection;
    }

    public DataSection prenatalPerinatalHistoryBody(Patient patient)
    {
        String sectionName = "prenatalPerinatalHistory";
        Set<String> present = enabledHeaderIdsBySection.get(sectionName);
        if (present == null || present.isEmpty()) {
            return null;
        }

        DataSection bodySection = new DataSection(sectionName);
        PatientData<Integer> history = patient.getData("prenatalPerinatalHistory");
        PatientData<String> apgarScores = patient.getData("apgar");
        Integer x = 0;
        if (present.contains("assistedReproduction_fertilityMeds")) {
            Integer assisted = history.get("assistedReproduction_fertilityMeds");
            DataCell cell = new DataCell(ConversionHelpers.integerToStrBool(assisted), x, 0);
            bodySection.addCell(cell);
            x++;
        }
        if (present.contains("ivf")) {
            Integer assisted = history.get("ivf");
            DataCell cell = new DataCell(ConversionHelpers.integerToStrBool(assisted), x, 0);
            bodySection.addCell(cell);
            x++;
        }
        if (present.contains("assistedReproduction_surrogacy")) {
            Integer assisted = history.get("assistedReproduction_surrogacy");
            DataCell cell = new DataCell(ConversionHelpers.integerToStrBool(assisted), x, 0);
            bodySection.addCell(cell);
            x++;
        }
        if (present.contains("apgar1")) {
            String apgar = apgarScores.get("apgar1");
            DataCell cell = new DataCell(apgar, x, 0);
            bodySection.addCell(cell);
            x++;
        }
        if (present.contains("apgar5")) {
            String apgar = apgarScores.get("apgar5");
            DataCell cell = new DataCell(apgar, x, 0);
            bodySection.addCell(cell);
            x++;
        }

        return bodySection;
    }

    public void prenatalPhenotypeSetup(Set<String> enabledFields) throws Exception
    {
        String sectionName = "prenatalPhenotype";
        String[] fieldIds =
            { "prenatal_phenotype", "prenatal_phenotype_code", "prenatal_phenotype_combined",
                "negative_prenatal__phenotype", "prenatal_phenotype_by_section" };
        /* FIXME These will not work properly in different configurations */
        String[][] headerIds =
            { { "phenotype" }, { "code" }, { "phenotype", "code" }, { "negative" }, { "category" } };
        Set<String> present = new HashSet<String>();

        int counter = 0;
        for (String fieldId : fieldIds) {
            if (enabledFields.remove(fieldId)) {
                for (String headerId : headerIds[counter]) {
                    present.add(headerId);
                }
            }
            counter++;
        }
        enabledHeaderIdsBySection.put(sectionName, present);

        /* Needed for ordering phenotypes */
        prenatalPhenotypeHelper = new ConversionHelpers();
        prenatalPhenotypeHelper
            .featureSetUp(present.contains("phenotype"), present.contains("negative"), present.contains("category"));
    }


    public DataSection prenatalPhenotypeHeader() throws Exception
    {
        String sectionName = "prenatalPhenotype";
        Set<String> present = enabledHeaderIdsBySection.get(sectionName);
        if (present.isEmpty()) {
            return null;
        }

        DataSection section = new DataSection(sectionName);
        List<String> orderedHeaderIds = new LinkedList<String>();
        orderedHeaderIds.add("category");
        orderedHeaderIds.add("phenotype");
        orderedHeaderIds.add("code");
        orderedHeaderIds.add("meta");
        orderedHeaderIds.add("meta_code");
        List<String> orderedHeaderNames = new LinkedList<String>();
        orderedHeaderNames.add("Category");
        orderedHeaderNames.add("Label");
        orderedHeaderNames.add("ID");
        orderedHeaderNames.add("Meta");
        orderedHeaderNames.add("ID");

        int counter = 0;
        int hX = 0;
        if (present.contains("phenotype") && present.contains("negative")) {
            DataCell cell = new DataCell("Present", hX, 1, StyleOption.HEADER);
            section.addCell(cell);
            hX++;
        }
        for (String headerId : orderedHeaderIds) {
            if (!present.contains(headerId)) {
                counter++;
                continue;
            }
            DataCell cell = new DataCell(orderedHeaderNames.get(counter), hX, 1, StyleOption.HEADER);
            section.addCell(cell);
            hX++;
            counter++;
        }
        DataCell sectionHeader = new DataCell("Prenatal Phenotype", 0, 0, StyleOption.HEADER);
        sectionHeader.addStyle(StyleOption.LARGE_HEADER);
        section.addCell(sectionHeader);

        return section;
    }

    public DataSection prenatalPhenotypeBody(Patient patient) throws Exception
    {
        String sectionName = "prenatalPhenotype";
        Set<String> present = enabledHeaderIdsBySection.get(sectionName);
        if (present == null || present.isEmpty()) {
            return null;
        }

        Boolean bothTypes = present.contains("phenotype") && present.contains("negative");
        DataSection section = new DataSection(sectionName);

        int x;
        int y = 0;
        Set<? extends Feature> features = patient.getFeatures();
        phenotypeHelper.newPatient();
        Boolean categoriesEnabled = present.contains("category");
        List<Feature> sortedFeatures;
        Map<String, String> sectionFeatureLookup = new HashMap<String, String>();
        if (!categoriesEnabled) {
            sortedFeatures = phenotypeHelper.sortFeaturesSimple(features);
        } else {
            sortedFeatures = phenotypeHelper.sortFeaturesWithSections(features);
            sectionFeatureLookup = phenotypeHelper.getSectionFeatureTree();
        }

        Boolean lastStatus = false;
        String lastSection = "";
        for (Feature feature : sortedFeatures) {
            x = 0;

            if (bothTypes && lastStatus != feature.isPresent()) {
                lastStatus = feature.isPresent();
                lastSection = "";
                DataCell cell = new DataCell(lastStatus ? "Yes" : "No", x, y);
                if (!lastStatus) {
                    cell.addStyle(StyleOption.YES_NO_SEPARATOR);
                }
                cell.addStyle(lastStatus ? StyleOption.YES : StyleOption.NO);
                section.addCell(cell);
            }
            if (bothTypes) {
                x++;
            }
            if (categoriesEnabled) {
                String currentSection = sectionFeatureLookup.get(feature.getId());
                if (!StringUtils.equals(currentSection, lastSection)) {
                    DataCell cell = new DataCell(currentSection, x, y);
                    section.addCell(cell);
                    lastSection = currentSection;
                }
                x++;
            }
            if (present.contains("phenotype")) {
                DataCell cell = new DataCell(feature.getName(), x, y, StyleOption.FEATURE_SEPARATOR);
                section.addCell(cell);
                x++;
            }
            if (present.contains("code")) {
                DataCell cell = new DataCell(feature.getId(), x, y, StyleOption.FEATURE_SEPARATOR);
                section.addCell(cell);
                x++;
            }
            if (present.contains("meta") || present.contains("meta_code")) {
                int mX = x;
                int mCX = x + 1;
                Collection<? extends FeatureMetadatum> featureMetadatum = feature.getMetadata().values();
                Boolean metaPresent = featureMetadatum.size() > 0;
                for (FeatureMetadatum meta : featureMetadatum) {
                    if (present.contains("meta")) {
                        DataCell cell = new DataCell(meta.getName(), mX, y);
                        section.addCell(cell);
                    }
                    if (present.contains("meta_code")) {
                        DataCell cell = new DataCell(meta.getId(), mCX, y);
                        section.addCell(cell);
                    }
                    y++;
                }
                if (metaPresent) {
                    y--;
                }
            }
            y++;
        }

//        section.finalizeToMatrix();
        return section;
    }

}
