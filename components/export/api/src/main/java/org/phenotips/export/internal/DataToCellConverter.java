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
package org.phenotips.export.internal;

import org.phenotips.data.Disorder;
import org.phenotips.data.Feature;
import org.phenotips.data.FeatureMetadatum;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.ontology.internal.solr.SolrOntologyTerm;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;

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

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.Utils;

/**
 * Each of functions need to be written with certain specification. Body producing functions must return null if they
 * produce no cells, and they must not remove from {@link #enabledHeaderIdsBySection}. If there are cells requested
 * (header present) but there is no data to put inside the cells, do not return null as cell value or no cell at all,
 * return a cell containing an empty string. Otherwise, the header will not be matched with the body.
 *
 * @version $Id$
 * @since 1.0RC1
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
        String[] fieldIds = { "phenotype", "phenotype_code", "phenotype_combined", "phenotype_code_meta",
            "phenotype_meta", "negative_phenotype", "negative_phenotype_code", "negative_phenotype_combined",
            "phenotype_by_section" };
        // FIXME These will not work properly in different configurations
        String[][] headerIds =
            { { "phenotype", "positive" }, { "code", "positive" }, { "phenotype", "code", "positive" },
                { "meta_code", "phenotype", "positive" }, { "meta", "phenotype", "positive" },
                { "negative", "phenotype" }, { "negative", "code" }, { "negative", "code", "phenotype" },
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
        this.enabledHeaderIdsBySection.put(sectionName, present);

        this.phenotypeHelper = new ConversionHelpers();
        this.phenotypeHelper
            .featureSetUp(present.contains("positive"), present.contains("negative"), present.contains("category"));
    }

    public DataSection phenotypeHeader() throws Exception
    {
        String sectionName = "phenotype";
        Set<String> present = this.enabledHeaderIdsBySection.get(sectionName);
        if (present.isEmpty()) {
            return null;
        }

        DataSection section = new DataSection();
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
        if (present.contains("positive") && present.contains("negative")) {
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
        Set<String> present = this.enabledHeaderIdsBySection.get(sectionName);
        if (present == null || present.isEmpty()) {
            return null;
        }

        Boolean bothTypes = present.contains("positive") && present.contains("negative");
        DataSection section = new DataSection();

        int x;
        int y = 0;
        Set<? extends Feature> features = patient.getFeatures();
        this.phenotypeHelper.newPatient();
        Boolean categoriesEnabled = present.contains("category");
        List<Feature> sortedFeatures;
        Map<String, String> sectionFeatureLookup = new HashMap<String, String>();
        if (!categoriesEnabled) {
            sortedFeatures = this.phenotypeHelper.sortFeaturesSimple(features);
        } else {
            sortedFeatures = this.phenotypeHelper.sortFeaturesWithSections(features);
            sectionFeatureLookup = this.phenotypeHelper.getSectionFeatureTree();
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
                Collection<? extends FeatureMetadatum> featureMetadatum = feature.getMetadata().values();
                Boolean metaPresent = featureMetadatum.size() > 0;
                int offset = 0;
                for (FeatureMetadatum meta : featureMetadatum) {
                    offset = 0;
                    if (present.contains("meta")) {
                        DataCell cell = new DataCell(meta.getName(), mX + offset, y);
                        section.addCell(cell);
                        offset += 1;
                    }
                    if (present.contains("meta_code")) {
                        DataCell cell = new DataCell(meta.getId(), mX + offset, y);
                        section.addCell(cell);
                        offset += 1;
                    }
                    y++;
                }
                // Because otherwise the section has smaller width than the header
                offset = 0;
                if (!metaPresent) {
                    if (present.contains("meta")) {
                        DataCell cell = new DataCell("", mX + offset, y);
                        section.addCell(cell);
                        offset += 1;
                    }
                    if (present.contains("meta_code")) {
                        DataCell cell = new DataCell("", mX + offset, y);
                        section.addCell(cell);
                        offset += 1;
                    }
                }
                if (metaPresent) {
                    y--;
                }
            }
            y++;
        }
        /* Creating empties */
        if (sortedFeatures.size() == 0) {
            // offset is included to account for the presence of both "positive" and "negative" in "present"
            int offset = bothTypes ? 1 : 0;
            Integer emptyX = 0;
            for (int i = 0; i < present.size() - offset; i++) {
                DataCell cell = new DataCell("", emptyX, 0);
                section.addCell(cell);
                emptyX++;
            }
        }
        // section.finalizeToMatrix();
        return section;
    }

    public void genesSetup(Set<String> enabledFields) throws Exception
    {
        String sectionName = "genes";
        String[] fieldIds = { "genes", "genes_comments", "rejectedGenes", "rejectedGenes_comments" };
        // FIXME These will not work properly in different configurations
        String[][] headerIds =
        { { "candidate" }, { "comments", "candidate" }, { "rejected" }, { "rejected_comments", "rejected" } };
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
        this.enabledHeaderIdsBySection.put(sectionName, present);
    }

    public DataSection genesHeader() throws Exception
    {
        String sectionName = "genes";
        Set<String> present = this.enabledHeaderIdsBySection.get(sectionName);

        if (present.isEmpty()) {
            return null;
        }

        DataSection section = new DataSection();

        int hX = 0;
        DataCell cell = new DataCell("Status", hX, 1, StyleOption.HEADER);
        section.addCell(cell);
        hX++;

        cell = new DataCell("Gene Name", hX, 1, StyleOption.HEADER);
        section.addCell(cell);
        hX++;

        if (present.contains("comments") || present.contains("rejected_comments")) {
            cell = new DataCell("Comments", hX, 1, StyleOption.HEADER);
            section.addCell(cell);
            hX++;
        }

        DataCell sectionHeader = new DataCell("Genotype", 0, 0, StyleOption.HEADER);
        sectionHeader.addStyle(StyleOption.LARGE_HEADER);
        section.addCell(sectionHeader);

        return section;
    }

    public DataSection genesBody(Patient patient) throws Exception
    {
        String sectionName = "genes";
        Set<String> present = this.enabledHeaderIdsBySection.get(sectionName);
        if (present == null || present.isEmpty()) {
            return null;
        }
        // empties should be created in the case that there are no genes to write
        boolean hasGenes = false;

        DataSection section = new DataSection();
        int y = 0;

        if (present.contains("candidate")) {
            PatientData<Map<String, String>> candidateGenes = patient.getData("genes");
            if (candidateGenes != null && candidateGenes.isIndexed()) {
                DataCell cell = new DataCell("Candidate", 0, y);
                section.addCell(cell);
                for (Map<String, String> candidateGene : candidateGenes) {
                    hasGenes = true;
                    String geneName = candidateGene.get("gene");
                    cell = new DataCell(geneName, 1, y);
                    section.addCell(cell);

                    if (present.contains("comments")) {
                        String comment = candidateGene.get("comments");
                        cell = new DataCell(comment, 2, y);
                        section.addCell(cell);
                    }
                    y++;
                }
            }
        }
        if (present.contains("rejected")) {
            PatientData<Map<String, String>> rejectedGenes = patient.getData("rejectedGenes");
            if (rejectedGenes != null && rejectedGenes.isIndexed()) {
                DataCell cell = new DataCell("Previously tested", 0, y, StyleOption.YES_NO_SEPARATOR);
                cell.addStyle(StyleOption.NO);
                section.addCell(cell);
                for (Map<String, String> rejectedGene : rejectedGenes) {
                    hasGenes = true;
                    String geneName = rejectedGene.get("gene");
                    cell = new DataCell(geneName, 1, y);
                    section.addCell(cell);

                    if (present.contains("rejected_comments")) {
                        String comment = rejectedGene.get("comments");
                        cell = new DataCell(comment, 2, y);
                        section.addCell(cell);
                    }
                    y++;
                }
            }
        }

        /* Creating empties */
        if (!hasGenes) {
            /* Status, and gene name columns are always present */
            int columns = present.contains("comments") || present.contains("rejected_comments") ? 3 : 2;
            Integer emptyX = 0;
            for (int i = 0; i < columns; i++) {
                DataCell cell = new DataCell("", emptyX, 0);
                section.addCell(cell);
                emptyX++;
            }
        }

        return section;
    }

    public DataSection idHeader(Set<String> enabledFields) throws Exception
    {
        String sectionName = "id";
        Set<String> present = new HashSet<String>();
        if (enabledFields.remove("doc.name")) {
            present.add("id");
        }
        if (enabledFields.remove("external_id")) {
            present.add("external_id");
        }
        if (present.size() == 0) {
            return null;
        }
        this.enabledHeaderIdsBySection.put(sectionName, present);

        DataSection section = new DataSection();
        DataCell topCell = new DataCell("Identifiers", 0, 0, StyleOption.HEADER);
        topCell.addStyle(StyleOption.LARGE_HEADER);
        section.addCell(topCell);
        if (present.contains("id")) {
            DataCell idCell = new DataCell("Report ID", 0, 1, StyleOption.HEADER);
            section.addCell(idCell);
        }
        if (present.contains("external_id")) {
            DataCell externalIdCell = new DataCell("Patient Identifier", 1, 1, StyleOption.HEADER);
            section.addCell(externalIdCell);
        }
        // section.finalizeToMatrix();
        return section;
    }

    public DataSection idBody(Patient patient) throws Exception
    {
        String sectionName = "id";
        Set<String> present = this.enabledHeaderIdsBySection.get(sectionName);
        if (present == null || present.isEmpty()) {
            return null;
        }
        DataSection section = new DataSection();

        if (present.contains("id")) {
            DataCell cell = new DataCell(patient.getId(), 0, 0);
            section.addCell(cell);
        }
        if (present.contains("external_id")) {
            DataCell cell = new DataCell(patient.getExternalId(), 1, 0);
            section.addCell(cell);
        }
        // section.finalizeToMatrix();
        return section;
    }

    public DataSection documentInfoHeader(Set<String> enabledFields) throws Exception
    {
        String sectionName = "documentInfo";

        // Must be linked to keep order; in other sections as well
        Map<String, String> fieldToHeaderMap = new LinkedHashMap<>();
        fieldToHeaderMap.put("referrer", "Referrer");
        fieldToHeaderMap.put("creationDate", "Creation date");
        fieldToHeaderMap.put("author", "Last modified by");
        fieldToHeaderMap.put("date", "Last modification date");

        Set<String> present = new LinkedHashSet<>();
        for (String fieldId : fieldToHeaderMap.keySet()) {
            if (enabledFields.remove(fieldId)) {
                present.add(fieldId);
            }
        }
        this.enabledHeaderIdsBySection.put(sectionName, present);

        DataSection headerSection = new DataSection();
        if (present.isEmpty()) {
            return null;
        }

        int x = 0;
        for (String fieldId : present) {
            DataCell headerCell = new DataCell(fieldToHeaderMap.get(fieldId), x, 1, StyleOption.HEADER);
            headerSection.addCell(headerCell);
            x++;
        }
        DataCell headerCell = new DataCell("Report Information", 0, 0, StyleOption.LARGE_HEADER);
        headerCell.addStyle(StyleOption.HEADER);
        headerSection.addCell(headerCell);

        return headerSection;
    }

    public DataSection documentInfoBody(Patient patient) throws Exception
    {
        @SuppressWarnings("deprecation")
        DocumentAccessBridge dab = Utils.getComponent(DocumentAccessBridge.class);
        XWikiDocument patientDoc = (XWikiDocument) dab.getDocument(patient.getDocument());
        String sectionName = "documentInfo";
        Set<String> present = this.enabledHeaderIdsBySection.get(sectionName);
        if (present == null || present.isEmpty()) {
            return null;
        }

        DataSection bodySection = new DataSection();
        Integer x = 0;
        if (present.contains("referrer")) {
            String creator = getUsername(patientDoc.getCreatorReference());
            DataCell cell = new DataCell(creator, x, 0);
            bodySection.addCell(cell);
            x++;
        }
        if (present.contains("creationDate")) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");
            Date creationDate = patientDoc.getCreationDate();
            DataCell cell = new DataCell(format.format(creationDate), x, 0);
            bodySection.addCell(cell);
            x++;
        }
        if (present.contains("author")) {
            String lastModifiedBy = getUsername(patientDoc.getAuthorReference());
            DataCell cell = new DataCell(lastModifiedBy, x, 0);
            bodySection.addCell(cell);
            x++;
        }
        if (present.contains("date")) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");
            Date modificationDate = patientDoc.getDate();
            DataCell cell = new DataCell(format.format(modificationDate), x, 0);
            bodySection.addCell(cell);
            x++;
        }

        return bodySection;
    }

    public DataSection patientInfoHeader(Set<String> enabledFields) throws Exception
    {
        String sectionName = "patientInfo";

        // Must be linked to keep order; in other sections as well
        Map<String, String> fieldToHeaderMap = new LinkedHashMap<>();
        fieldToHeaderMap.put("first_name", "First Name");
        fieldToHeaderMap.put("last_name", "Last Name");
        fieldToHeaderMap.put("date_of_birth", "Date of birth");
        fieldToHeaderMap.put("gender", "Sex");
        fieldToHeaderMap.put("indication_for_referral", "Indication for referral");

        Set<String> present = new LinkedHashSet<>();
        for (String fieldId : fieldToHeaderMap.keySet()) {
            if (enabledFields.remove(fieldId)) {
                present.add(fieldId);
            }
        }
        this.enabledHeaderIdsBySection.put(sectionName, present);

        DataSection headerSection = new DataSection();
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
        Set<String> present = this.enabledHeaderIdsBySection.get(sectionName);
        if (present == null || present.isEmpty()) {
            return null;
        }

        DataSection bodySection = new DataSection();
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
            Date dob = patient.<Date>getData("dates").get("date_of_birth");
            DataCell cell;
            if (dob != null) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");
                cell = new DataCell(format.format(dob), x, 0);
            } else {
                cell = new DataCell("", x, 0);
            }
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
            for (DataCell cell : ConversionHelpers.preventOverflow(indicationForReferral, x, 0)) {
                cell.setMultiline();
                bodySection.addCell(cell);
            }
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
        fieldToHeaderMap.put("family_history", "Family conditions");
        fieldToHeaderMap.put("maternal_ethnicity", "Maternal");
        fieldToHeaderMap.put("paternal_ethnicity", "Paternal");

        Set<String> present = new LinkedHashSet<String>();
        for (String fieldId : fieldToHeaderMap.keySet()) {
            if (enabledFields.remove(fieldId)) {
                present.add(fieldId);
            }
        }
        this.enabledHeaderIdsBySection.put(sectionName, present);
        if (present.isEmpty()) {
            return null;
        }

        DataSection headerSection = new DataSection();

        int bottomY = 1;
        int ethnicityOffset = 0;
        if (present.contains("maternal_ethnicity") || present.contains("paternal_ethnicity")) {
            bottomY = 2;
            if (fieldToHeaderMap.containsKey("maternal_ethnicity")
                && fieldToHeaderMap.containsKey("paternal_ethnicity"))
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
        Set<String> present = this.enabledHeaderIdsBySection.get(sectionName);
        if (present == null || present.isEmpty()) {
            return null;
        }

        DataSection bodySection = new DataSection();
        PatientData<Integer> familyHistory = patient.getData("familyHistory");
        PatientData<List<String>> ethnicities = patient.getData("ethnicity");
        Integer x = 0;
        if (present.contains("global_mode_of_inheritance")) {
            PatientData<SolrOntologyTerm> globalControllers = patient.<SolrOntologyTerm>getData("global-qualifiers");
            SolrOntologyTerm modeTerm =
                globalControllers != null ? globalControllers.get("global_mode_of_inheritance") : null;
            String mode = modeTerm != null ? modeTerm.getName() : "";
            DataCell cell = new DataCell(mode, x, 0);
            bodySection.addCell(cell);
            x++;
        }
        if (present.contains("miscarriages")) {
            Integer miscarriages = familyHistory.get("miscarriages");
            DataCell cell = new DataCell(ConversionHelpers.integerToStrBool(miscarriages), x, 0);
            bodySection.addCell(cell);
            x++;
        }
        if (present.contains("consanguinity")) {
            Integer consanguinity = familyHistory.get("consanguinity");
            DataCell cell = new DataCell(ConversionHelpers.integerToStrBool(consanguinity), x, 0);
            bodySection.addCell(cell);
            x++;
        }
        if (present.contains("family_history")) {
            PatientData<String> notes = patient.<String>getData("notes");
            String familyConditions = notes != null ? notes.get("family_history") : "";
            for (DataCell cell : ConversionHelpers.preventOverflow(familyConditions, x, 0)) {
                cell.setMultiline();
                bodySection.addCell(cell);
            }
            x++;
        }
        if (present.contains("maternal_ethnicity")) {
            List<String> maternalEthnicity = ethnicities.get("maternal_ethnicity");
            int y = 0;
            if (maternalEthnicity != null && !maternalEthnicity.isEmpty()) {
                for (String mEthnicity : maternalEthnicity) {
                    DataCell cell = new DataCell(mEthnicity, x, y);
                    bodySection.addCell(cell);
                    y++;
                }
            } else {
                DataCell cell = new DataCell("", x, y);
                bodySection.addCell(cell);
            }
            x++;
        }
        if (present.contains("paternal_ethnicity")) {
            List<String> paternalEthnicity = ethnicities.get("paternal_ethnicity");
            int y = 0;
            if (paternalEthnicity != null && !paternalEthnicity.isEmpty()) {
                for (String pEthnicity : paternalEthnicity) {
                    DataCell cell = new DataCell(pEthnicity, x, y);
                    bodySection.addCell(cell);
                    y++;
                }
            } else {
                DataCell cell = new DataCell("", x, y);
                bodySection.addCell(cell);
            }
            x++;
        }

        return bodySection;
    }

    public DataSection prenatalPerinatalHistoryHeader(Set<String> enabledFields) throws Exception
    {
        String sectionName = "prenatalPerinatalHistory";
        Map<String, String> fieldToHeaderMap = new LinkedHashMap<String, String>();
        fieldToHeaderMap.put("gestation", "Gestation at delivery");
        fieldToHeaderMap.put("prenatal_development", "Notes");
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
        this.enabledHeaderIdsBySection.put(sectionName, present);
        if (present.isEmpty()) {
            return null;
        }

        DataSection headerSection = new DataSection();

        List<String> apgarFields = new LinkedList<String>(Arrays.asList("apgar1", "apgar2"));
        List<String> assitedReproductionFields = new LinkedList<String>(
            Arrays.asList("ivf", "assistedReproduction_surrogacy", "assistedReproduction_fertilityMeds"));
        apgarFields.retainAll(present);
        assitedReproductionFields.retainAll(present);
        int apgarOffset = apgarFields.size();
        // there used to be a +1 for the offset
        int assistedReproductionOffset = apgarOffset + assitedReproductionFields.size();
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
        Set<String> present = this.enabledHeaderIdsBySection.get(sectionName);
        if (present == null || present.isEmpty()) {
            return null;
        }

        DataSection bodySection = new DataSection();
        PatientData<Integer> history = patient.getData("prenatalPerinatalHistory");
        PatientData<String> apgarScores = patient.getData("apgar");
        Integer x = 0;
        if (present.contains("gestation")) {
            Integer gestation = history.get("gestation");
            DataCell cell = new DataCell(gestation != null ? gestation.toString() : "", x, 0);
            bodySection.addCell(cell);
            x++;
        }
        if (present.contains("prenatal_development")) {
            PatientData<String> notes = patient.getData("notes");
            String prenatalNotes = notes != null ? notes.get("prenatal_development") : "";
            for (DataCell cell : ConversionHelpers.preventOverflow(prenatalNotes, x, 0)) {
                cell.setMultiline();
                bodySection.addCell(cell);
            }
            x++;
        }
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
            Object apgar = apgarScores.get("apgar1");
            String cellValue = apgar != null ? apgar.toString() : "";
            DataCell cell = new DataCell(cellValue, x, 0);
            bodySection.addCell(cell);
            x++;
        }
        if (present.contains("apgar5")) {
            Object apgar = apgarScores.get("apgar5");
            String cellValue = apgar != null ? apgar.toString() : "";
            DataCell cell = new DataCell(cellValue, x, 0);
            bodySection.addCell(cell);
            x++;
        }

        return bodySection;
    }

    public void prenatalPhenotypeSetup(Set<String> enabledFields) throws Exception
    {
        String sectionName = "prenatalPhenotype";
        String[] fieldIds = { "prenatal_phenotype", "prenatal_phenotype_code", "prenatal_phenotype_combined",
            "negative_prenatal_phenotype", "prenatal_phenotype_by_section" };
        /* FIXME These will not work properly in different configurations */
        String[][] headerIds = { { "phenotype" }, { "code" }, { "phenotype", "code" }, { "negative" }, { "category" } };
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
        this.enabledHeaderIdsBySection.put(sectionName, present);

        /* Needed for ordering phenotypes */
        this.prenatalPhenotypeHelper = new ConversionHelpers();
        this.prenatalPhenotypeHelper
            .featureSetUp(present.contains("phenotype"), present.contains("negative"), present.contains("category"));
    }

    public DataSection prenatalPhenotypeHeader() throws Exception
    {
        String sectionName = "prenatalPhenotype";
        Set<String> present = this.enabledHeaderIdsBySection.get(sectionName);
        if (present.isEmpty()) {
            return null;
        }

        DataSection section = new DataSection();
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
        Set<String> present = this.enabledHeaderIdsBySection.get(sectionName);
        if (present == null || present.isEmpty()) {
            return null;
        }

        Boolean bothTypes = present.contains("phenotype") && present.contains("negative");
        DataSection section = new DataSection();

        int x;
        int y = 0;
        Set<? extends Feature> features = patient.getFeatures();

        this.prenatalPhenotypeHelper.newPatient();
        features = this.prenatalPhenotypeHelper.filterFeaturesByPrenatal(features, true);
        Boolean categoriesEnabled = present.contains("category");
        List<Feature> sortedFeatures;
        Map<String, String> sectionFeatureLookup = new HashMap<String, String>();
        if (!categoriesEnabled) {
            sortedFeatures = this.prenatalPhenotypeHelper.sortFeaturesSimple(features);
        } else {
            sortedFeatures = this.prenatalPhenotypeHelper.sortFeaturesWithSections(features);
            sectionFeatureLookup = this.prenatalPhenotypeHelper.getSectionFeatureTree();
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
            y++;
        }
        /* Creating empites */
        if (sortedFeatures.size() == 0) {
            Integer emptyX = 0;
            for (String header : present) {
                DataCell cell = new DataCell("", emptyX, 0);
                section.addCell(cell);
                emptyX++;
            }
        }
        // section.finalizeToMatrix();
        return section;
    }

    public DataSection disordersHeaders(Set<String> enabledFields) throws Exception
    {
        String sectionName = "disorders";

        String[] fieldIds = { "omim_id", "omim_id_code", "omim_id_combined", "diagnosis_notes" };
        /* FIXME These will not work properly in different configurations */
        String[][] headerIds = { { "disorder" }, { "code" }, { "disorder", "code" }, { "notes" } };
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
        this.enabledHeaderIdsBySection.put(sectionName, present);

        // Must be linked to keep order; in other sections as well
        Map<String, String> fieldToHeaderMap = new LinkedHashMap<>();
        fieldToHeaderMap.put("disorder", "Label");
        fieldToHeaderMap.put("code", "ID");
        fieldToHeaderMap.put("notes", "Notes");

        DataSection headerSection = new DataSection();
        if (present.isEmpty()) {
            return null;
        }

        int x = 0;
        for (String fieldId : present) {
            DataCell headerCell = new DataCell(fieldToHeaderMap.get(fieldId), x, 1, StyleOption.HEADER);
            headerSection.addCell(headerCell);
            x++;
        }
        DataCell headerCell = new DataCell("Disorders", 0, 0, StyleOption.LARGE_HEADER);
        headerCell.addStyle(StyleOption.HEADER);
        headerSection.addCell(headerCell);

        return headerSection;
    }

    public DataSection disordersBody(Patient patient)
    {
        String sectionName = "disorders";
        Set<String> present = this.enabledHeaderIdsBySection.get(sectionName);
        if (present == null || present.isEmpty()) {
            return null;
        }

        DataSection bodySection = new DataSection();

        Set<? extends Disorder> disorders = patient.getDisorders();
        Integer y = 0;
        for (Disorder disorder : disorders) {
            Integer x = 0;
            if (present.contains("disorder")) {
                DataCell cell = new DataCell(disorder.getName(), x, y);
                cell.setMultiline();
                bodySection.addCell(cell);
                x++;
            }
            if (present.contains("code")) {
                DataCell cell = new DataCell(disorder.getId(), x, y);
                bodySection.addCell(cell);
                x++;
            }
            y++;
        }
        /* If there is no data, but there are headers present, create empty cells */
        if (disorders.size() == 0) {
            Integer x = 0;
            if (present.contains("disorder")) {
                DataCell cell = new DataCell("", x, y);
                bodySection.addCell(cell);
                x++;
            }
            if (present.contains("code")) {
                DataCell cell = new DataCell("", x, y);
                bodySection.addCell(cell);
                x++;
            }
        }
        /* Notes export */
        if (present.contains("notes")) {
            PatientData<String> notes = patient.getData("notes");
            String diagnosisNotes = notes != null ? notes.get("diagnosis_notes") : "";
            for (DataCell cell : ConversionHelpers.preventOverflow(diagnosisNotes, bodySection.getMaxX() + 1, 0)) {
                cell.setMultiline();
                bodySection.addCell(cell);
            }
        }

        return bodySection;
    }

    public DataSection medicalHistoryHeader(Set<String> enabledFields) throws Exception
    {
        String sectionName = "medicalHistory";

        // Must be linked to keep order; in other sections as well
        Map<String, String> fieldToHeaderMap = new LinkedHashMap<>();
        fieldToHeaderMap.put("global_age_of_onset", "Age of onset");
        fieldToHeaderMap.put("medical_history", "Notes");

        Set<String> present = new LinkedHashSet<>();
        for (String fieldId : fieldToHeaderMap.keySet()) {
            if (enabledFields.remove(fieldId)) {
                present.add(fieldId);
            }
        }
        this.enabledHeaderIdsBySection.put(sectionName, present);

        DataSection headerSection = new DataSection();
        if (present.isEmpty()) {
            return null;
        }

        int x = 0;
        for (String fieldId : present) {
            DataCell headerCell = new DataCell(fieldToHeaderMap.get(fieldId), x, 1, StyleOption.HEADER);
            headerSection.addCell(headerCell);
            x++;
        }
        DataCell headerCell = new DataCell("Medical History", 0, 0, StyleOption.LARGE_HEADER);
        headerCell.addStyle(StyleOption.HEADER);
        headerSection.addCell(headerCell);

        return headerSection;
    }

    public DataSection medicalHistoryBody(Patient patient)
    {
        String sectionName = "medicalHistory";
        Set<String> present = this.enabledHeaderIdsBySection.get(sectionName);
        if (present == null || present.isEmpty()) {
            return null;
        }
        DataSection bodySection = new DataSection();

        Integer x = 0;
        if (present.contains("global_age_of_onset")) {
            PatientData<SolrOntologyTerm> qualifiers = patient.getData("global-qualifiers");
            SolrOntologyTerm ageOfOnset = qualifiers != null ? qualifiers.get("global_age_of_onset") : null;
            DataCell cell = new DataCell(ageOfOnset != null ? ageOfOnset.getName() : "", x, 0);
            bodySection.addCell(cell);
            x++;
        }
        if (present.contains("medical_history")) {
            PatientData<String> notes = patient.getData("notes");
            String medicalNotes = notes != null ? notes.get("medical_history") : "";
            for (DataCell cell : ConversionHelpers.preventOverflow(medicalNotes, x, 0)) {
                cell.setMultiline();
                bodySection.addCell(cell);
            }
            x++;
        }

        return bodySection;
    }

    public DataSection isNormalHeader(Set<String> enabledFields) throws Exception
    {
        String sectionName = "isNormal";

        // Must be linked to keep order; in other sections as well
        Map<String, String> fieldToHeaderMap = new LinkedHashMap<>();
        fieldToHeaderMap.put("unaffected", "Clinically normal");

        Set<String> present = new LinkedHashSet<>();
        for (String fieldId : fieldToHeaderMap.keySet()) {
            if (enabledFields.remove(fieldId)) {
                present.add(fieldId);
            }
        }
        this.enabledHeaderIdsBySection.put(sectionName, present);

        DataSection headerSection = new DataSection();
        if (present.isEmpty()) {
            return null;
        }

        DataCell headerCell = new DataCell("Clinically Normal", 0, 0, StyleOption.LARGE_HEADER);
        headerCell.addStyle(StyleOption.HEADER);
        headerSection.addCell(headerCell);

        return headerSection;
    }

    public DataSection isNormalBody(Patient patient)
    {
        String sectionName = "isNormal";
        Set<String> present = this.enabledHeaderIdsBySection.get(sectionName);
        if (present == null || present.isEmpty()) {
            return null;
        }
        DataSection bodySection = new DataSection();

        if (present.contains("unaffected")) {
            PatientData<Integer> isNormal = patient.getData("isClinicallyNormal");
            Integer isNormalValue = isNormal != null ? isNormal.get("unaffected") : 0;
            DataCell cell = new DataCell(ConversionHelpers.integerToStrBool(isNormalValue), 0, 0);
            bodySection.addCell(cell);
        }

        return bodySection;
    }

    private String getUsername(DocumentReference reference)
    {
        if (reference == null) {
            return "Unknown user";
        }
        return reference.getName();
    }
}
