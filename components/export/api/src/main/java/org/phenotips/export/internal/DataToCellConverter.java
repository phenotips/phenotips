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
package org.phenotips.export.internal;

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.Disorder;
import org.phenotips.data.Feature;
import org.phenotips.data.FeatureMetadatum;
import org.phenotips.data.Gene;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PhenoTipsDate;
import org.phenotips.data.internal.SolvedData;
import org.phenotips.translation.TranslationManager;
import org.phenotips.vocabulary.internal.solr.SolrVocabularyTerm;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Each of functions need to be written with certain specification. Body producing functions must return null if they
 * produce no cells, and they must not remove from {@link #enabledHeaderIdsBySection}. If there are cells requested
 * (header present) but there is no data to put inside the cells, do not return null as cell value or no cell at all,
 * return a cell containing an empty string. Otherwise, the header will not be matched with the body.
 *
 * @version $Id$
 * @since 1.0RC1
 */
@SuppressWarnings({ "checkstyle:NPathComplexity", "checkstyle:CyclomaticComplexity", "checkstyle:JavadocMethod",
    "checkstyle:ExecutableStatementCount", "checkstyle:MultipleStringLiterals", "checkstyle:JavaNCSS" })
public class DataToCellConverter
{
    /** The maximum number of characters to allow in free-text cells; longer text is wrapped in shorter lines. */
    public static final Integer MAX_CHARACTERS_PER_LINE = 100;

    private static final String ALLERGIES = "allergies";

    private Map<String, Set<String>> enabledHeaderIdsBySection = new HashMap<>();

    private ConversionHelpers phenotypeHelper;

    private ConversionHelpers prenatalPhenotypeHelper;

    private TranslationManager translationManager;

    public DataToCellConverter()
    {
        try {
            this.translationManager = ComponentManagerRegistry.getContextComponentManager().getInstance(
                TranslationManager.class);
        } catch (ComponentLookupException ex) {
            LoggerFactory.getLogger(getClass()).warn("Failed to lookup TranslationManager component: [{}]",
                ex.getMessage());
        }
    }

    public void phenotypeSetup(Set<String> enabledFields) throws Exception
    {
        String sectionName = "phenotype";
        String[] fieldIds = { "phenotype", "phenotype_meta" };
        String[][] headerIds = { { "present", "name", "id" }, { "present", "name", "id", "meta.name", "meta.id" } };

        Set<String> present = addHeaders(fieldIds, headerIds, enabledFields);
        this.enabledHeaderIdsBySection.put(sectionName, present);
        this.phenotypeHelper = new ConversionHelpers();
        // FIXME "type" is never exposed
        this.phenotypeHelper.featureSetUp(present.contains("type"));
    }

    public DataSection phenotypeHeader() throws Exception
    {
        String sectionName = "phenotype";
        Set<String> present = this.enabledHeaderIdsBySection.get(sectionName);
        if (present == null || present.isEmpty()) {
            return null;
        }

        DataSection section = new DataSection();
        int hX = 0;
        for (String headerId : present) {
            DataCell cell = new DataCell(this.translationManager.translate("phenotips.export.excel.label.phenotype."
                + headerId), hX, 1, StyleOption.HEADER);
            section.addCell(cell);
            hX++;
        }
        DataCell sectionHeader =
            new DataCell(this.translationManager.translate("phenotips.export.excel.label.phenotype"), 0, 0,
                StyleOption.HEADER);
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

        DataSection section = new DataSection();

        int x;
        int y = 0;
        Set<? extends Feature> features = patient.getFeatures();

        boolean categoriesEnabled = present.contains("type");
        List<Feature> sortedFeatures;
        Map<String, String> sectionFeatureLookup = new HashMap<>();
        if (!categoriesEnabled) {
            sortedFeatures = this.phenotypeHelper.sortFeaturesSimple(features);
        } else {
            sortedFeatures = this.phenotypeHelper.sortFeaturesWithSections(features);
            sectionFeatureLookup = this.phenotypeHelper.getSectionFeatureTree();
        }

        Boolean lastStatus = null;
        String lastSection = "";
        for (Feature feature : sortedFeatures) {
            x = 0;

            if (!Boolean.valueOf(feature.isPresent()).equals(lastStatus)) {
                lastSection = "";
                DataCell cell = new DataCell(feature.isPresent()
                    ? this.translationManager.translate("yes")
                    : this.translationManager.translate("no"),
                    x, y);
                if (lastStatus != null) {
                    cell.addStyle(StyleOption.YES_NO_SEPARATOR);
                }
                lastStatus = feature.isPresent();
                cell.addStyle(lastStatus ? StyleOption.YES : StyleOption.NO);
                section.addCell(cell);
            }
            if (categoriesEnabled) {
                ++x;
                String currentSection = sectionFeatureLookup.get(feature.getId());
                if (!StringUtils.equals(currentSection, lastSection)) {
                    DataCell cell = new DataCell(currentSection, x, y);
                    section.addCell(cell);
                    lastSection = currentSection;
                }
            }
            section.addCell(new DataCell(feature.getName(), ++x, y, StyleOption.FEATURE_SEPARATOR));
            section.addCell(new DataCell(feature.getId(), ++x, y, StyleOption.FEATURE_SEPARATOR));
            if (present.contains("meta.name")) {
                Collection<? extends FeatureMetadatum> featureMetadatum = feature.getMetadata().values();
                Boolean metaPresent = !featureMetadatum.isEmpty();
                for (FeatureMetadatum meta : featureMetadatum) {
                    section.addCell(new DataCell(meta.getName(), x + 1, y));
                    section.addCell(new DataCell(meta.getId(), x + 2, y));
                    ++y;
                }
                // Add empty cells if there's no meta, since otherwise the section will be smaller than the header
                if (!metaPresent) {
                    section.addCell(new DataCell("", x + 1, y));
                    section.addCell(new DataCell("", x + 2, y));
                } else {
                    // Since each meta item moves to the next row, and we also move down once more after each feature,
                    // move back up to the last meta row
                    --y;
                }
            }
            ++y;
        }
        // Creating empties
        if (sortedFeatures.isEmpty()) {
            for (int emptyX = 0; emptyX < present.size(); emptyX++) {
                section.addCell(new DataCell("", emptyX, 0));
            }
        }
        // section.finalizeToMatrix();
        return section;
    }

    public void genesSetup(Set<String> enabledFields) throws Exception
    {
        String sectionName = "genes";
        if (!enabledFields.remove(sectionName)) {
            return;
        }

        this.enabledHeaderIdsBySection.put(sectionName,
            new LinkedHashSet<>(Arrays.asList("gene", "status", "strategy", "comments")));
    }

    public DataSection genesHeader() throws Exception
    {
        String sectionName = "genes";
        Set<String> present = this.enabledHeaderIdsBySection.get(sectionName);
        if (present == null || present.isEmpty()) {
            return null;
        }

        int hX = 0;
        DataSection section = new DataSection();

        for (String field : present) {
            section.addCell(new DataCell(this.translationManager.translate("PhenoTips.GeneClass_" + field), hX++, 1,
                StyleOption.HEADER));
        }

        DataCell sectionHeader =
            new DataCell(this.translationManager.translate("phenotips.export.excel.label.genotype"),
                0, 0, StyleOption.HEADER);
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

        DataSection section = new DataSection();
        int y = 0;

        PatientData<Gene> allGenes = patient.getData("genes");

        // empties should be created in the case that there are no genes to write
        if (allGenes == null || !allGenes.isIndexed()) {
            for (int i = 0; i < present.size(); i++) {
                DataCell cell = new DataCell("", i, y);
                section.addCell(cell);
            }
            return section;
        }
        List<String> strategyTranslates =
            Arrays.asList("sequencing", "deletion", "familial_mutation", "common_mutations");

        for (Gene gene : allGenes) {
            int x = 0;
            for (String field : present) {
                String value = "";
                switch (field) {
                    case "gene":
                        value = gene.getName();
                        break;
                    case "strategy":
                        value =
                            getMultivalueField(gene.getStrategy(), strategyTranslates,
                                "PhenoTips.GeneClass_strategy_");
                        break;
                    case "status":
                        value = this.translationManager.translate("PhenoTips.GeneClass_status_" + gene.getStatus());
                        break;
                    case "comments":
                        value = gene.getComment();
                        break;
                    default:
                        // No other fields supported yet
                        break;
                }
                section.addCell(new DataCell(value, x++, y));
            }
            y++;
        }

        return section;
    }

    public void variantsSetup(Set<String> enabledFields) throws Exception
    {
        String sectionName = "variants";
        Set<String> present = this.enabledHeaderIdsBySection.get("genes");
        // if genes are not included or variants are not enabled
        if (present == null || present.isEmpty() || !enabledFields.remove(sectionName)) {
            return;
        }
        List<String> columns =
            Arrays.asList("gene", "cdna", "protein", "transcript", "dbsnp", "zygosity", "effect", "interpretation",
                "inheritance", "evidence", "segregation", "sanger", "chromosome", "start_position",
                "end_position", "reference_genome");
        this.enabledHeaderIdsBySection.put(sectionName, new LinkedHashSet<>(columns));
    }

    public DataSection variantsHeader() throws Exception
    {
        String sectionName = "variants";
        Set<String> present = this.enabledHeaderIdsBySection.get(sectionName);
        if (present == null || present.isEmpty()) {
            return null;
        }

        int hX = 0;
        DataSection section = new DataSection();

        for (String field : present) {
            String head = this.translationManager.translate("PhenoTips.GeneVariantClass_" + field);
            section.addCell(new DataCell(head, hX++, 1, StyleOption.HEADER));
        }

        DataCell sectionHeader =
            new DataCell(this.translationManager.translate("phenotips.export.excel.label.genotype.variant"),
                0, 0, StyleOption.HEADER);
        sectionHeader.addStyle(StyleOption.LARGE_HEADER);
        section.addCell(sectionHeader);

        return section;
    }

    public DataSection variantsBody(Patient patient) throws Exception
    {
        String sectionName = "variants";
        Set<String> present = this.enabledHeaderIdsBySection.get(sectionName);
        if (present == null || present.isEmpty()) {
            return null;
        }

        DataSection section = new DataSection();
        int y = 0;

        PatientData<Map<String, String>> variants = patient.getData("variants");
        // empties should be created in the case that there are no variants to write
        if (variants == null || !variants.isIndexed()) {
            for (int i = 0; i < present.size(); i++) {
                DataCell cell = new DataCell("", i, y);
                section.addCell(cell);
            }
            return section;
        }

        List<String> translatables =
            Arrays.asList("zygosity", "effect", "interpretation", "inheritance", "segregation", "evidence", "sanger");
        List<String> evidenceTranslates = Arrays.asList("rare", "predicted", "reported");

        for (Map<String, String> variant : variants) {
            int x = 0;
            for (String field : present) {
                String value = variant.get(field);
                if ("evidence".equals(field)) {
                    value = parseMultivalueField(value, evidenceTranslates, "PhenoTips.GeneVariantClass_evidence_");
                } else if (translatables.contains(field)) {
                    value = this.translationManager.translate("PhenoTips.GeneVariantClass_" + field + "_" + value);
                }
                section.addCell(new DataCell(value, x++, y));
            }
            y++;
        }

        return section;
    }

    public DataSection geneticNotesHeader(Set<String> enabledFields) throws Exception
    {
        String sectionName = "genetic_notes";
        if (!enabledFields.remove(sectionName)) {
            return null;
        }
        Set<String> present = Collections.singleton(sectionName);
        this.enabledHeaderIdsBySection.put(sectionName, present);

        DataSection headerSection = new DataSection();

        DataCell headerCell =
            new DataCell(this.translationManager.translate("phenotips.exportPreferences.field.geneticNotes"),
                0, 0, StyleOption.LARGE_HEADER);
        headerCell.addStyle(StyleOption.HEADER);
        headerSection.addCell(headerCell);

        return headerSection;
    }

    public DataSection geneticNotesBody(Patient patient)
    {
        String sectionName = "genetic_notes";
        Set<String> present = this.enabledHeaderIdsBySection.get(sectionName);
        if (present == null || present.isEmpty()) {
            return null;
        }
        DataSection bodySection = new DataSection();

        PatientData<String> notes = patient.getData("notes");
        String comments = notes != null ? notes.get("genetic_notes") : "";
        for (DataCell gcell : ConversionHelpers.preventOverflow(comments, 0, 0)) {
            gcell.setMultiline();
            bodySection.addCell(gcell);
        }

        return bodySection;
    }

    public DataSection idHeader(Set<String> enabledFields) throws Exception
    {
        String sectionName = "id";
        Set<String> present = new HashSet<>();
        if (enabledFields.remove("doc.name")) {
            present.add("id");
        }
        if (enabledFields.remove("external_id")) {
            present.add("external_id");
        }
        if (present.isEmpty()) {
            return null;
        }
        this.enabledHeaderIdsBySection.put(sectionName, present);

        DataSection section = new DataSection();
        DataCell topCell = new DataCell(this.translationManager.translate("phenotips.export.excel.label.identifiers"),
            0, 0, StyleOption.HEADER);
        topCell.addStyle(StyleOption.LARGE_HEADER);
        section.addCell(topCell);
        int hX = 0;
        if (present.contains("id")) {
            DataCell idCell =
                new DataCell(this.translationManager.translate("phenotips.export.excel.label.identifiers.internal"),
                    hX, 1, StyleOption.HEADER);
            section.addCell(idCell);
            hX++;
        }
        if (present.contains("external_id")) {
            DataCell externalIdCell =
                new DataCell(this.translationManager.translate("phenotips.export.excel.label.identifiers.external"),
                    hX, 1, StyleOption.HEADER);
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

        int x = 0;
        if (present.contains("id")) {
            DataCell cell = new DataCell(patient.getId(), x, 0);
            section.addCell(cell);
            x++;
        }
        if (present.contains("external_id")) {
            DataCell cell = new DataCell(patient.getExternalId(), x, 0);
            section.addCell(cell);
        }
        // section.finalizeToMatrix();
        return section;
    }

    public DataSection documentInfoHeader(Set<String> enabledFields) throws Exception
    {
        String sectionName = "documentInfo";

        // Must be linked to keep order; in other sections as well

        List<String> fields = new ArrayList<>(Arrays.asList("referrer", "creationDate", "author", "date"));
        fields.retainAll(enabledFields);
        Set<String> fieldSet = new HashSet<>(fields);
        this.enabledHeaderIdsBySection.put(sectionName, fieldSet);

        DataSection headerSection = new DataSection();
        if (fields.isEmpty()) {
            return null;
        }

        int hX = 0;
        for (String fieldId : fields) {
            DataCell headerCell =
                new DataCell(this.translationManager.translate("phenotips.exportPreferences.field." + fieldId), hX, 1,
                    StyleOption.HEADER);
            headerSection.addCell(headerCell);
            hX++;
        }
        DataCell headerCell =
            new DataCell(this.translationManager.translate("phenotips.export.excel.label.documentInfo"), 0, 0,
                StyleOption.LARGE_HEADER);
        headerCell.addStyle(StyleOption.HEADER);
        headerSection.addCell(headerCell);

        return headerSection;
    }

    public DataSection documentInfoBody(Patient patient) throws Exception
    {
        XWikiDocument patientDoc = patient.getXDocument();
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
        List<String> fields = new ArrayList<>(
            Arrays.asList("first_name", "last_name", "date_of_birth", "gender", "indication_for_referral"));
        fields.retainAll(enabledFields);
        Set<String> fieldSet = new HashSet<>(fields);
        this.enabledHeaderIdsBySection.put(sectionName, fieldSet);

        DataSection headerSection = new DataSection();
        if (fields.isEmpty()) {
            return null;
        }

        int hX = 0;
        for (String fieldId : fields) {
            DataCell headerCell = new DataCell(this.translationManager.translate("PhenoTips.PatientClass_" + fieldId),
                hX, 1, StyleOption.HEADER);
            headerSection.addCell(headerCell);
            hX++;
        }

        DataCell headerCell =
            new DataCell(this.translationManager.translate("phenotips.export.excel.label.patientInformation"), 0, 0,
                StyleOption.LARGE_HEADER);
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
        int x = 0;
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
            PhenoTipsDate dob = patient.<PhenoTipsDate>getData("dates").get("date_of_birth");
            DataCell cell;
            if (dob != null && dob.toEarliestPossibleISODate() != null) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");
                cell = new DataCell(format.format(dob.toEarliestPossibleISODate()), x, 0);
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
        List<String> fields = new ArrayList<>(Arrays.asList("global_mode_of_inheritance", "miscarriages",
            "consanguinity", "family_history", "paternal_ethnicity", "maternal_ethnicity"));
        fields.retainAll(enabledFields);
        Set<String> fieldSet = new HashSet<>(fields);
        this.enabledHeaderIdsBySection.put(sectionName, fieldSet);
        if (fields.isEmpty()) {
            return null;
        }

        DataSection headerSection = new DataSection();

        int bottomY = 1;
        int ethnicityOffset = 0;
        if (fields.contains("maternal_ethnicity") || fields.contains("paternal_ethnicity")) {
            bottomY = 2;
            if (fields.contains("maternal_ethnicity")
                && fields.contains("paternal_ethnicity")) {
                ethnicityOffset = 2;
            } else {
                ethnicityOffset = 1;
            }
        }
        int x = 0;
        for (String fieldId : fields) {
            DataCell headerCell =
                new DataCell(
                    this.translationManager.translate("phenotips.export.excel.label.familyHistory." + fieldId),
                    x, bottomY,
                    StyleOption.HEADER);
            headerSection.addCell(headerCell);
            x++;
        }
        if (ethnicityOffset > 0) {
            DataCell headerCell =
                new DataCell(this.translationManager.translate("phenotips.export.excel.label.familyHistory.ethnicity"),
                    x - ethnicityOffset, 1, StyleOption.HEADER);
            headerSection.addCell(headerCell);
        }

        DataCell headerCell = new DataCell(
            this.translationManager.translate("phenotips.export.excel.label.familyHistory"), 0, 0,
            StyleOption.LARGE_HEADER);
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
        int x = 0;
        if (present.contains("global_mode_of_inheritance")) {
            PatientData<List<SolrVocabularyTerm>> globalControllers = patient.getData("global-qualifiers");
            List<SolrVocabularyTerm> modeTermList =
                globalControllers != null ? globalControllers.get("global_mode_of_inheritance") : null;
            int y = 0;
            if (modeTermList != null && !modeTermList.isEmpty()) {
                for (SolrVocabularyTerm term : modeTermList) {
                    String mode = term != null ? term.getName() : "";
                    DataCell cell = new DataCell(mode, x, y);
                    bodySection.addCell(cell);
                    y++;
                }
            } else {
                DataCell cell = new DataCell("", x, y);
                bodySection.addCell(cell);
            }
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

        return bodySection;
    }

    public DataSection prenatalPerinatalHistoryHeader(Set<String> enabledFields) throws Exception
    {
        String sectionName = "prenatalPerinatalHistory";
        List<String> fields = new ArrayList<>(Arrays.asList("gestation", "prenatal_development", "parentalAge",
            "assistedReproduction_fertilityMeds", "assistedReproduction_iui", "ivf", "icsi",
            "assistedReproduction_surrogacy", "assistedReproduction_donoregg", "assistedReproduction_donorsperm",
            "obstetricHistory", "apgar1"));
        fields.retainAll(enabledFields);
        Set<String> fieldSet = new HashSet<>(fields);
        this.enabledHeaderIdsBySection.put(sectionName, fieldSet);
        if (fields.isEmpty()) {
            return null;
        }

        DataSection headerSection = new DataSection();
        List<String> assitedReproductionFields = new ArrayList<>(Arrays.asList("assistedReproduction_fertilityMeds",
            "assistedReproduction_iui", "ivf", "icsi", "assistedReproduction_surrogacy",
            "assistedReproduction_donoregg", "assistedReproduction_donorsperm"));
        List<String> parentalFields = Arrays.asList("maternalAge", "paternalAge");
        List<String> obstetricFields = Arrays.asList("gravida", "para", "term", "preterm", "sab", "tab", "births");
        List<String> apgarFields = new ArrayList<>(Arrays.asList("apgar1", "apgar5"));
        assitedReproductionFields.retainAll(fieldSet);
        int assistedReproductionOffset = assitedReproductionFields.size();
        int parentalOffset = fields.contains("parentalAge") ? parentalFields.size() : 0;
        int apgarOffset = fields.contains("apgar1") ? 2 : 0;
        int obstetricOffset = fields.contains("obstetricHistory") ? obstetricFields.size() : 0;
        int bottomY =
            (apgarOffset > 0 || obstetricOffset > 0 || parentalOffset > 0 || assistedReproductionOffset > 0) ? 2 : 1;
        int hX = 0;

        for (String fieldId : fields) {
            if ("parentalAge".equals(fieldId)) {
                for (String field : parentalFields) {
                    DataCell headerCell = new DataCell(this.translationManager
                        .translate("phenotips.export.excel.label.prenatalPerinatalHistory." + field),
                        hX, bottomY, StyleOption.HEADER);
                    headerSection.addCell(headerCell);
                    hX++;
                }
            } else if ("apgar1".equals(fieldId)) {
                for (String apgarId : apgarFields) {
                    DataCell headerCell = new DataCell(this.translationManager
                        .translate("phenotips.export.excel.label.prenatalPerinatalHistory." + apgarId),
                        hX, bottomY, StyleOption.HEADER);
                    headerSection.addCell(headerCell);
                    hX++;
                }
            } else if ("obstetricHistory".equals(fieldId)) {
                for (String field : obstetricFields) {
                    DataCell headerCell = new DataCell(this.translationManager
                        .translate("phenotips.export.excel.label.prenatalPerinatalHistory." + field),
                        hX, bottomY, StyleOption.HEADER);
                    headerSection.addCell(headerCell);
                    hX++;
                }
            } else {
                DataCell headerCell = new DataCell(
                    this.translationManager
                        .translate("phenotips.export.excel.label.prenatalPerinatalHistory." + fieldId),
                    hX, bottomY, StyleOption.HEADER);
                headerSection.addCell(headerCell);
                hX++;
            }
        }
        if (parentalOffset > 0) {
            DataCell headerCell = new DataCell(
                this.translationManager.translate("phenotips.export.excel.label.prenatalPerinatalHistory.parentalAge"),
                hX - obstetricOffset - apgarOffset - assistedReproductionOffset - parentalOffset, 1,
                StyleOption.HEADER);
            headerSection.addCell(headerCell);
        }
        if (assistedReproductionOffset > 0) {
            DataCell headerCell = new DataCell(this.translationManager
                .translate("phenotips.export.excel.label.prenatalPerinatalHistory.assistedReproduction"),
                hX - apgarOffset - assistedReproductionOffset - obstetricOffset, 1, StyleOption.HEADER);
            headerSection.addCell(headerCell);
        }
        if (obstetricOffset > 0) {
            DataCell headerCell = new DataCell(
                this.translationManager
                    .translate("phenotips.export.excel.label.prenatalPerinatalHistory.obstetricHistory"),
                hX - obstetricOffset - apgarOffset, 1, StyleOption.HEADER);
            headerSection.addCell(headerCell);
        }
        if (apgarOffset > 0) {
            DataCell headerCell = new DataCell(
                this.translationManager.translate("phenotips.export.excel.label.prenatalPerinatalHistory.apgarScore"),
                hX - apgarOffset, 1, StyleOption.HEADER);
            headerSection.addCell(headerCell);
        }

        DataCell headerCell =
            new DataCell(this.translationManager.translate("phenotips.export.excel.label.prenatalPerinatalHistory"),
                0, 0, StyleOption.LARGE_HEADER);
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
        PatientData<Integer> apgarScores = patient.getData("apgar");
        PatientData<Integer> parentalAge = patient.getData("parentalAge");
        PatientData<Integer> obstetricHistory = patient.getData("obstetric-history");
        int x = 0;

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

        if (present.contains("parentalAge")) {
            List<String> parentalFields = Arrays.asList("maternal_age", "paternal_age");
            for (String field : parentalFields) {
                Integer age = parentalAge != null ? parentalAge.get(field) : null;
                String cellValue = age != null ? age.toString() : "";
                DataCell cell = new DataCell(cellValue, x, 0);
                bodySection.addCell(cell);
                x++;
            }
        }

        List<String> fields =
            Arrays.asList("assistedReproduction_fertilityMeds", "assistedReproduction_iui", "ivf", "icsi",
                "assistedReproduction_surrogacy", "assistedReproduction_donoregg", "assistedReproduction_donorsperm");
        for (String field : fields) {
            if (!present.contains(field)) {
                continue;
            }
            Integer assisted = history.get(field);
            DataCell cell = new DataCell(ConversionHelpers.integerToStrBool(assisted), x, 0);
            bodySection.addCell(cell);
            x++;
        }

        if (present.contains("obstetricHistory")) {
            List<String> obstetricFields = Arrays.asList("gravida", "para", "term", "preterm", "sab", "tab", "births");
            for (String field : obstetricFields) {
                Integer oData = obstetricHistory != null ? obstetricHistory.get(field) : null;
                String cellValue = oData != null ? oData.toString() : "";
                DataCell cell = new DataCell(cellValue, x, 0);
                bodySection.addCell(cell);
                x++;
            }
        }

        if (present.contains("apgar1")) {
            List<String> apgarFields = Arrays.asList("apgar1", "apgar5");
            for (String aField : apgarFields) {
                Integer apgar = apgarScores.get(aField);
                String cellValue = apgar != null ? apgar.toString() : "";
                DataCell cell = new DataCell(cellValue, x, 0);
                bodySection.addCell(cell);
                x++;
            }
        }

        return bodySection;
    }

    public void prenatalPhenotypeSetup(Set<String> enabledFields) throws Exception
    {

        String sectionName = "prenatalPhenotype";
        Set<String> present = new LinkedHashSet<>();
        present.add("present");
        present.add("name");
        present.add("id");
        if (enabledFields.remove("prenatal_phenotype")) {
            this.enabledHeaderIdsBySection.put(sectionName, present);
        }

        /* Needed for ordering phenotypes */
        this.prenatalPhenotypeHelper = new ConversionHelpers();
        this.prenatalPhenotypeHelper.featureSetUp(false);
    }

    public DataSection prenatalPhenotypeHeader() throws Exception
    {
        String sectionName = "prenatalPhenotype";
        Set<String> present = this.enabledHeaderIdsBySection.get(sectionName);
        if (present == null || present.isEmpty()) {
            return null;
        }

        DataSection section = new DataSection();

        int hX = 0;
        for (String headerId : present) {
            section.addCell(
                new DataCell(this.translationManager.translate("phenotips.export.excel.label.phenotype." + headerId),
                    hX, 1, StyleOption.HEADER));
            hX++;
        }
        DataCell sectionHeader = new DataCell(
            this.translationManager.translate("phenotips.export.excel.label.prenatalPhenotype"), 0,
            0, StyleOption.HEADER);
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

        DataSection section = new DataSection();

        int x;
        int y = 0;
        Set<? extends Feature> features = patient.getFeatures();

        features = this.prenatalPhenotypeHelper.filterFeaturesByPrenatal(features, true);
        List<Feature> sortedFeatures;
        sortedFeatures = this.prenatalPhenotypeHelper.sortFeaturesSimple(features);

        Boolean lastStatus = null;
        for (Feature feature : sortedFeatures) {
            x = 0;

            if (!Boolean.valueOf(feature.isPresent()).equals(lastStatus)) {
                DataCell cell = new DataCell(feature.isPresent()
                    ? this.translationManager.translate("yes")
                    : this.translationManager.translate("no"),
                    x, y);
                if (lastStatus != null) {
                    cell.addStyle(StyleOption.YES_NO_SEPARATOR);
                }
                lastStatus = feature.isPresent();
                cell.addStyle(lastStatus ? StyleOption.YES : StyleOption.NO);
                section.addCell(cell);
            }
            section.addCell(new DataCell(feature.getName(), ++x, y, StyleOption.FEATURE_SEPARATOR));
            section.addCell(new DataCell(feature.getId(), ++x, y, StyleOption.FEATURE_SEPARATOR));
            ++y;
        }
        // Creating empties
        if (sortedFeatures.isEmpty()) {
            for (int emptyX = 0; emptyX < present.size(); emptyX++) {
                section.addCell(new DataCell("", emptyX, 0));
            }
        }
        // section.finalizeToMatrix();
        return section;
    }

    public DataSection disordersHeaders(Set<String> enabledFields) throws Exception
    {
        String sectionName = "disorders";
        Set<String> present = Collections.singleton("omim_id");

        if (enabledFields.remove("omim_id")) {
            this.enabledHeaderIdsBySection.put(sectionName, present);

            DataSection headerSection = new DataSection();
            int hX = 0;
            for (String fieldId : Arrays.asList("name", "id")) {
                DataCell headerCell = new DataCell(
                    this.translationManager.translate("phenotips.export.excel.label.disorders." + fieldId), hX, 1,
                    StyleOption.HEADER);
                headerSection.addCell(headerCell);
                hX++;
            }
            DataCell headerCell =
                new DataCell(this.translationManager.translate("phenotips.export.excel.label.disorders"), 0,
                    0, StyleOption.LARGE_HEADER);
            headerCell.addStyle(StyleOption.HEADER);
            headerSection.addCell(headerCell);

            return headerSection;
        }
        return null;
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
            bodySection.addCell(new DataCell(disorder.getName(), 0, y));
            bodySection.addCell(new DataCell(disorder.getId(), 1, y));
            y++;
        }
        /* If there is no data, but there are headers present, create empty cells */
        if (disorders.isEmpty()) {
            bodySection.addCell(new DataCell("", 0, y));
            bodySection.addCell(new DataCell("", 1, y));
        }

        return bodySection;
    }

    public DataSection clinicalDiagnosisHeaders(Set<String> enabledFields) throws Exception
    {
        String sectionName = "clinicalDiagnosis";
        Set<String> present = Collections.singleton("clinical_diagnosis");

        if (enabledFields.remove("clinical_diagnosis")) {
            this.enabledHeaderIdsBySection.put(sectionName, present);

            DataSection headerSection = new DataSection();
            int hX = 0;
            for (String fieldId : Arrays.asList("name", "id")) {
                DataCell headerCell = new DataCell(
                    this.translationManager.translate("phenotips.export.excel.label.clinicalDiagnosis." + fieldId), hX,
                    1,
                    StyleOption.HEADER);
                headerSection.addCell(headerCell);
                hX++;
            }
            DataCell headerCell =
                new DataCell(this.translationManager.translate("phenotips.export.excel.label.clinicalDiagnosis"), 0,
                    0, StyleOption.LARGE_HEADER);
            headerCell.addStyle(StyleOption.HEADER);
            headerSection.addCell(headerCell);

            return headerSection;
        }
        return null;
    }

    public DataSection clinicalDiagnosisBody(Patient patient)
    {
        String sectionName = "clinicalDiagnosis";
        Set<String> present = this.enabledHeaderIdsBySection.get(sectionName);
        if (present == null || present.isEmpty()) {
            return null;
        }

        int y = 0;
        DataSection bodySection = new DataSection();

        PatientData<Disorder> clinicalDisorders = patient.getData("clinical-diagnosis");
        /* If there is no data, but there are headers present, create empty cells */
        if (clinicalDisorders == null || clinicalDisorders.size() == 0) {
            bodySection.addCell(new DataCell("", 0, y));
            bodySection.addCell(new DataCell("", 1, y));
            return bodySection;
        }
        for (Disorder disorder : clinicalDisorders) {
            bodySection.addCell(new DataCell(disorder.getName(), 0, y));
            bodySection.addCell(new DataCell(disorder.getId(), 1, y));
            y++;
        }

        return bodySection;
    }

    public DataSection diagnosisNotesHeader(Set<String> enabledFields) throws Exception
    {
        String sectionName = "diagnosis_notes";

        Set<String> present = new LinkedHashSet<>();
        if (enabledFields.remove(sectionName)) {
            present.add(sectionName);
        }
        this.enabledHeaderIdsBySection.put(sectionName, present);

        DataSection headerSection = new DataSection();

        if (present.isEmpty()) {
            return null;
        }

        DataCell headerCell =
            new DataCell(this.translationManager.translate("phenotips.export.excel.label.diagnosisNotes"), 0,
                0, StyleOption.LARGE_HEADER);
        headerCell.addStyle(StyleOption.HEADER);
        headerSection.addCell(headerCell);

        return headerSection;
    }

    public DataSection diagnosisNotesBody(Patient patient)
    {
        String sectionName = "diagnosis_notes";
        Set<String> present = this.enabledHeaderIdsBySection.get(sectionName);
        if (present == null || present.isEmpty()) {
            return null;
        }

        DataSection bodySection = new DataSection();

        PatientData<String> notes = patient.getData("notes");
        String comments = notes != null ? notes.get("diagnosis_notes") : "";
        for (DataCell gcell : ConversionHelpers.preventOverflow(comments, 0, 0)) {
            gcell.setMultiline();
            bodySection.addCell(gcell);
        }

        return bodySection;
    }

    public DataSection medicalHistoryHeader(Set<String> enabledFields) throws Exception
    {
        String sectionName = "medicalHistory";
        List<String> fields =
            new ArrayList<>(Arrays.asList("allergies", "global_age_of_onset", "medical_history"));
        fields.retainAll(enabledFields);
        Set<String> fieldSet = new HashSet<>(fields);
        this.enabledHeaderIdsBySection.put(sectionName, fieldSet);

        DataSection headerSection = new DataSection();
        if (fields.isEmpty()) {
            return null;
        }

        int hX = 0;
        for (String fieldId : fields) {
            DataCell headerCell = new DataCell(
                this.translationManager.translate("phenotips.export.excel.label.medicalHistory." + fieldId), hX, 1,
                StyleOption.HEADER);
            headerSection.addCell(headerCell);
            hX++;
        }
        DataCell headerCell =
            new DataCell(this.translationManager.translate("phenotips.export.excel.label.medicalHistory"),
                0, 0, StyleOption.LARGE_HEADER);
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

        if (present.contains(ALLERGIES)) {
            PatientData<String> allergiesData = patient.getData(ALLERGIES);
            int y = 0;
            if (allergiesData != null && allergiesData.isIndexed()) {
                for (String allergy : allergiesData) {
                    DataCell cell = new DataCell(allergy, x, y);
                    if ("NKDA".equals(allergy)) {
                        cell.addStyle(StyleOption.YES);
                    }
                    bodySection.addCell(cell);
                    y++;
                }
            }
            x++;
        }

        if (present.contains("global_age_of_onset")) {
            PatientData<List<SolrVocabularyTerm>> qualifiers = patient.getData("global-qualifiers");
            List<SolrVocabularyTerm> ageOfOnsetList = qualifiers != null ? qualifiers.get("global_age_of_onset") : null;
            int y = 0;
            if (ageOfOnsetList != null && !ageOfOnsetList.isEmpty()) {
                for (SolrVocabularyTerm term : ageOfOnsetList) {
                    String onset = term != null ? term.getName() : "";
                    DataCell cell = new DataCell(onset, x, y);
                    bodySection.addCell(cell);
                    y++;
                }
            } else {
                DataCell cell = new DataCell("", x, y);
                bodySection.addCell(cell);
            }
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
        Set<String> present = new LinkedHashSet<>();
        if (enabledFields.remove("unaffected")) {
            present.add("unaffected");
        }
        this.enabledHeaderIdsBySection.put(sectionName, present);

        DataSection headerSection = new DataSection();
        if (present.isEmpty()) {
            return null;
        }

        DataCell headerCell =
            new DataCell(this.translationManager.translate("phenotips.export.excel.label.unaffected"),
                0, 0, StyleOption.LARGE_HEADER);
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
            PatientData<String> isNormal = patient.getData("clinicalStatus");
            String isNormalString = (isNormal != null) ? isNormal.getValue() : "unaffected";
            Integer isNormalValue = ("unaffected".equals(isNormalString)) ? 1 : 0;
            DataCell cell = new DataCell(ConversionHelpers.integerToStrBool(isNormalValue), 0, 0);
            bodySection.addCell(cell);
        }

        return bodySection;
    }

    public DataSection isSolvedHeader(Set<String> enabledFields) throws Exception
    {
        String sectionName = "isSolved";
        List<String> fields = new ArrayList<>(Arrays.asList("solved", "solved__pubmed_id", "solved__notes"));
        fields.retainAll(enabledFields);
        Set<String> fieldSet = new HashSet<>(fields);
        this.enabledHeaderIdsBySection.put(sectionName, fieldSet);

        DataSection headerSection = new DataSection();
        if (fields.isEmpty()) {
            return null;
        }

        int hX = 0;
        for (String fieldId : fields) {
            DataCell headerCell = new DataCell(
                this.translationManager.translate("phenotips.export.excel.label.solvedStatus." + fieldId), hX, 1,
                StyleOption.HEADER);
            headerSection.addCell(headerCell);
            hX++;
        }
        DataCell headerCell = new DataCell(
            this.translationManager.translate("phenotips.export.excel.label.solvedStatus"), 0, 0,
            StyleOption.LARGE_HEADER);
        headerCell.addStyle(StyleOption.HEADER);
        headerSection.addCell(headerCell);

        return headerSection;
    }

    public DataSection isSolvedBody(Patient patient)
    {
        String sectionName = "isSolved";
        Set<String> present = this.enabledHeaderIdsBySection.get(sectionName);
        if (present == null || present.isEmpty()) {
            return null;
        }
        DataSection bodySection = new DataSection();
        PatientData<SolvedData> patientData = patient.getData("solved");
        SolvedData data = patientData != null ? patientData.getValue() : null;

        int x = 0;
        if (present.contains(SolvedData.STATUS_PROPERTY_NAME)) {
            String solved = data != null ? data.getStatus() : null;
            DataCell cell = new DataCell(solved != null ? ConversionHelpers.strIntegerToStrBool(solved) : "", x, 0);
            bodySection.addCell(cell);
            x++;
        }

        if (present.contains(SolvedData.PUBMED_ID_PROPERTY_NAME)) {
            List<String> pubmedIds = data != null ? data.getPubmedIds() : Collections.emptyList();
            if (!pubmedIds.isEmpty()) {
                int y = 0;
                for (String id : pubmedIds) {
                    DataCell cell = new DataCell(StringUtils.isBlank(id) ? "" : id, x, y);
                    bodySection.addCell(cell);
                    y++;
                }
                x++;
            }
        }

        if (present.contains(SolvedData.NOTES_PROPERTY_NAME)) {
            String solvedNotes = data != null ? data.getNotes() : null;
            for (DataCell cell : ConversionHelpers.preventOverflow(solvedNotes, x, 0)) {
                cell.setMultiline();
                bodySection.addCell(cell);
            }
            x++;
        }

        return bodySection;
    }

    private String getUsername(DocumentReference reference)
    {
        if (reference == null) {
            return this.translationManager.translate("phenotips.export.excel.label.unknownUser");
        }
        return reference.getName();
    }

    private Set<String> addHeaders(String[] fieldIds, String[][] headerIds, Set<String> enabledFields)
    {
        Set<String> present = new LinkedHashSet<>();
        int counter = 0;
        for (String fieldId : fieldIds) {
            if (enabledFields.remove(fieldId)) {
                for (String headerId : headerIds[counter]) {
                    present.add(headerId);
                }
            }
            counter++;
        }
        return present;
    }

    private String parseMultivalueField(String value, List<String> valueTranslates, String className)
    {
        if (StringUtils.isBlank(value)) {
            return "";
        }
        String field = "";
        for (String property : valueTranslates) {
            if (value.contains(property)) {
                if (field.length() != 0) {
                    field += "; ";
                }
                field += this.translationManager.translate(className + property);
            }
        }
        return field;
    }

    private String getMultivalueField(Collection<String> value, List<String> valueTranslates, String className)
    {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String field = "";
        for (String property : valueTranslates) {
            if (value.contains(property)) {
                if (field.length() != 0) {
                    field += "; ";
                }
                field += this.translationManager.translate(className + property);
            }
        }
        return field;
    }
}
