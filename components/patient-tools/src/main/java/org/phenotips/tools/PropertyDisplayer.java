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
package org.phenotips.tools;

import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.xml.XMLUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;

import com.xpn.xwiki.api.Property;

public class PropertyDisplayer
{
    private static final String ROOT_ID = "HP:0000001";

    private static final String TYPE_KEY = "type";

    private static final String GROUP_TYPE_KEY = "group_type";

    private static final String ID_KEY = "id";

    private static final String TITLE_KEY = "title";

    private static final String CATEGORIES_KEY = "categories";

    private static final String DATA_KEY = "data";

    private static final String ITEM_TYPE_SECTION = "section";

    private static final String ITEM_TYPE_SUBSECTION = "subsection";

    private static final String ITEM_TYPE_CONDITIONAL_SUBSECTION = "conditionalSubsection";

    private static final String ITEM_TYPE_FIELD = "field";

    public static final String INDEXED_CATEGORY_KEY = "term_category";

    private static final String INDEXED_PARENT_KEY = "is_a";

    protected Vocabulary ontologyService;

    private final FormData data;

    protected final String[] fieldNames;

    protected final String propertyName;

    private Map<String, Map<String, String>> metadata;

    private List<FormSection> sections = new LinkedList<>();

    PropertyDisplayer(Collection<Map<String, ?>> template, FormData data, Vocabulary ontologyService)
    {
        this.data = data;
        this.ontologyService = ontologyService;
        this.fieldNames = new String[2];
        this.fieldNames[0] = data.getPositiveFieldName();
        this.fieldNames[1] = data.getNegativeFieldName();
        this.propertyName = data.getPositivePropertyName();
        this.prepareMetaData();
        List<String> customYesSelected = new LinkedList<>();
        if (data.getSelectedValues() != null) {
            customYesSelected.addAll(data.getSelectedValues());
        }
        List<String> customNoSelected = new LinkedList<>();
        if (data.getNegativeFieldName() != null && data.getSelectedNegativeValues() != null) {
            customNoSelected.addAll(data.getSelectedNegativeValues());
        }

        template = replaceOtherWithTopSections(template);
        for (Map<String, ?> sectionTemplate : template) {
            if (isSection(sectionTemplate)) {
                this.sections.add(generateSection(sectionTemplate, customYesSelected, customNoSelected));
            }
        }
        putTermsInSections(customYesSelected, this.data.getCustomCategories(), true);
        putTermsInSections(customNoSelected, this.data.getCustomNegativeCategories(), false);
    }

    protected void putTermsInSections(List<String> selectedTerms, Map<String, List<String>> customCategories,
        boolean positive)
    {
        for (String value : selectedTerms) {
            VocabularyTerm term = this.ontologyService.getTerm(value);
            VocabularyTerm root = this.ontologyService.getTerm(ROOT_ID);
            List<String> categories = new LinkedList<>();
            categories.addAll(this.getCategoriesFromOntology(value));
            categories.addAll(this.getCategoriesFromCustomMapping(value, customCategories));
            if (categories.isEmpty()) {
                categories.add(ROOT_ID);
            }
            FormSection mostSpecificSection = null;
            long bestDistance = Long.MAX_VALUE;
            for (FormSection section : this.sections) {
                Collection<String> categoriesInCommon =
                    CollectionUtils.intersection(section.getCategories(), categories);
                if (!categoriesInCommon.isEmpty()) {
                    for (String categoryId : categoriesInCommon) {
                        VocabularyTerm categoryTerm = getCategoryTerm(categoryId);
                        long distance = (term != null)
                            ? categoryTerm.getDistanceTo(term)
                            : 1000 - categoryTerm.getDistanceTo(root);
                        if (distance >= 0 && distance < bestDistance) {
                            bestDistance = distance;
                            mostSpecificSection = section;
                        }
                    }
                }
            }
            if (mostSpecificSection != null) {
                mostSpecificSection
                    .addCustomElement(this.generateField(value, null, false, positive, !positive));
            }
        }
    }

    /**
     * Gets the {@link VocabularyTerm} associated with the provided {@code categoryId}. If {@code categoryId} refers to
     * a non-standard category, will set "phenotypic abnormality" ("HP:0000118") as the default.
     *
     * @param categoryId the ID of a category of interest
     * @return a {@link VocabularyTerm} associated with the provided {@code categoryId} or {@link VocabularyTerm} for
     *         "HP:0000001"
     */
    private VocabularyTerm getCategoryTerm(final String categoryId)
    {
        final VocabularyTerm categoryTerm = this.ontologyService.getTerm(categoryId);
        return categoryTerm != null ? categoryTerm : this.ontologyService.getTerm(ROOT_ID);
    }

    public String display()
    {
        StringBuilder str = new StringBuilder(128);
        for (FormSection section : this.sections) {
            str.append(section.display(this.data.getMode(), this.fieldNames));
        }
        if (DisplayMode.Edit.equals(this.data.getMode())) {
            str.append("<input type=\"hidden\" name=\"" + this.fieldNames[0] + "\" value=\"\" />");
            if (this.fieldNames[1] != null) {
                str.append("<input type=\"hidden\" name=\"" + this.fieldNames[1] + "\" value=\"\" />");
            }
        }
        return str.toString();
    }

    /**
     * Adds top sections (direct children of HP:0000118) to a copy of the existing templates list, if those are not
     * present. Also deletes any categories that are HP:0000118.
     *
     * @param originalTemplate the existing templates list
     * @return a modified templates list
     */
    protected Collection<Map<String, ?>> replaceOtherWithTopSections(Collection<Map<String, ?>> originalTemplate)
    {
        // Need to work with a copy to prevent concurrency problems.
        List<Map<String, ?>> template = new LinkedList<>();
        template.addAll(originalTemplate);

        Map<String, String> m = new HashMap<>();
        m.put("is_a", "HP:0000118");
        List<VocabularyTerm> topSections = this.ontologyService.search(m);
        Set<String> topSectionsId = new LinkedHashSet<>();
        for (VocabularyTerm section : topSections) {
            topSectionsId.add(section.getId());
        }
        // Explicitly add Death, since it's not part of the "Phenotypic abnormality" branch of HPO, but still makes
        // sense as a patient feature
        topSectionsId.add("HP:0011420");
        // Catch-all, in case someone wants to add a qualifier
        topSectionsId.add("HP:0000001");

        for (Map<String, ?> sectionTemplate : template) {
            try {
                Object templateCategoriesUC = sectionTemplate.get("categories");
                if (templateCategoriesUC instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> templateCategories = (List<String>) templateCategoriesUC;
                    for (String category : templateCategories) {
                        topSectionsId.remove(category);
                    }
                    templateCategories.remove("HP:0000118");
                    if (templateCategories.isEmpty()) {
                        template.remove(sectionTemplate);
                    }
                } else {
                    String templateCategory = (String) templateCategoriesUC;
                    if (StringUtils.equals(templateCategory, "HP:0000118")) {
                        template.remove(sectionTemplate);
                    } else {
                        topSectionsId.remove(templateCategory);
                    }
                }
            } catch (Exception ex) {
                continue;
            }
            if (topSectionsId.isEmpty()) {
                break;
            }
        }
        for (String sectionId : topSectionsId) {
            VocabularyTerm term = this.ontologyService.getTerm(sectionId);
            if (term == null) {
                continue;
            }
            Map<String, Object> templateSection = new HashMap<>();

            String title = term.getName();
            title = title.replace("Abnormality of the ", "").replace("Abnormality of ", "");
            title = WordUtils.capitalizeFully(title);
            templateSection.put(TYPE_KEY, ITEM_TYPE_SECTION);
            templateSection.put(TITLE_KEY, title);
            templateSection.put(CATEGORIES_KEY, Arrays.asList(sectionId));
            templateSection.put(DATA_KEY, new ArrayList<Map<String, String>>());

            template.add(templateSection);
        }

        return template;
    }

    private boolean isSection(Map<String, ?> item)
    {
        return ITEM_TYPE_SECTION.equals(item.get(TYPE_KEY)) && Collection.class.isInstance(item.get(CATEGORIES_KEY))
            && String.class.isInstance(item.get(TITLE_KEY)) && Collection.class.isInstance(item.get(DATA_KEY));
    }

    private boolean isSubsection(Map<String, ?> item)
    {
        return (ITEM_TYPE_SUBSECTION.equals(item.get(TYPE_KEY))
            || ITEM_TYPE_CONDITIONAL_SUBSECTION.equals(item.get(TYPE_KEY)))
            && (String.class.isInstance(item.get(TITLE_KEY)) || String.class.isInstance(item.get(ID_KEY)))
            && Collection.class.isInstance(item.get(DATA_KEY));
    }

    /**
     * This function is meant to be used on sections that are already know to be subsections.
     *
     * @param item the configuration object of the subsection
     * @return true if the subsection is conditional, false otherwise
     */
    private boolean isConditionalSubsection(Map<String, ?> item)
    {
        return ITEM_TYPE_CONDITIONAL_SUBSECTION.equals(item.get(TYPE_KEY));
    }

    private boolean isField(Map<String, ?> item)
    {
        return item.get(TYPE_KEY) == null || ITEM_TYPE_FIELD.equals(item.get(TYPE_KEY)) && item.get(ID_KEY) != null
            && String.class.isAssignableFrom(item.get(ID_KEY).getClass());
    }

    @SuppressWarnings("unchecked")
    private FormSection generateSection(Map<String, ?> sectionTemplate, List<String> customYesSelected,
        List<String> customNoSelected)
    {
        String title = (String) sectionTemplate.get(TITLE_KEY);
        Collection<String> categories = (Collection<String>) sectionTemplate.get(CATEGORIES_KEY);
        FormSection section = new FormSection(title, this.propertyName, categories);
        generateData(section, sectionTemplate, customYesSelected, customNoSelected);
        return section;
    }

    private FormElement generateSubsection(Map<String, ?> subsectionTemplate, List<String> customYesSelected,
        List<String> customNoSelected)
    {
        String title = (String) subsectionTemplate.get(TITLE_KEY);
        String id = (String) subsectionTemplate.get(ID_KEY);
        if (StringUtils.isEmpty(title) && StringUtils.isNotEmpty(id)) {
            title = getLabelFromOntology(id);
        }
        String type = (String) subsectionTemplate.get(GROUP_TYPE_KEY);
        if (type == null) {
            type = "";
        }
        FormGroup subsection;
        if (isConditionalSubsection(subsectionTemplate)) {
            boolean yesSelected = customYesSelected.remove(id);
            boolean noSelected = customNoSelected.remove(id);
            FormElement titleYesNoPicker = generateField(id, title, true, yesSelected, noSelected);
            subsection = new FormConditionalSubsection(title, type, titleYesNoPicker, yesSelected, noSelected);
        } else {
            subsection = new FormSubsection(title, type);
        }
        generateData(subsection, subsectionTemplate, customYesSelected, customNoSelected);
        return subsection;
    }

    @SuppressWarnings("unchecked")
    private void generateData(FormGroup formGroup, Map<String, ?> groupTemplate, List<String> customYesSelected,
        List<String> customNoSelected)
    {
        Collection<Map<String, ?>> data = (Collection<Map<String, ?>>) groupTemplate.get(DATA_KEY);
        for (Map<String, ?> item : data) {
            if (isSubsection(item)) {
                formGroup.addElement(generateSubsection(item, customYesSelected, customNoSelected));
            } else if (isField(item)) {
                formGroup.addElement(generateField(item, customYesSelected, customNoSelected));
            }
        }
    }

    private FormElement generateField(Map<String, ?> fieldTemplate, List<String> customYesSelected,
        List<String> customNoSelected)
    {
        String id = (String) fieldTemplate.get(ID_KEY);
        boolean yesSelected = customYesSelected.remove(id);
        boolean noSelected = customNoSelected.remove(id);
        return this.generateField(id, (String) fieldTemplate.get(TITLE_KEY), yesSelected, noSelected);

    }

    private FormElement generateField(String id, String title, boolean yesSelected, boolean noSelected)
    {
        return generateField(id, title, hasDescendantsInOntology(id), yesSelected, noSelected);
    }

    private FormElement generateField(String id, String title, boolean expandable, boolean yesSelected,
        boolean noSelected)
    {
        String hint = getLabelFromOntology(id);
        if (id.equals(hint) && title != null) {
            hint = title;
        }
        String metadata = "";
        Map<String, String> metadataValues = this.metadata.get(id);
        if (metadataValues != null) {
            metadata =
                metadataValues.get(noSelected ? this.data.getNegativePropertyName() : this.data
                    .getPositivePropertyName());
        }
        return new FormField(id, StringUtils.defaultIfEmpty(title, hint), hint, StringUtils.defaultString(metadata),
            expandable, yesSelected, noSelected);
    }

    private String getLabelFromOntology(String id)
    {
        if (!id.startsWith("HP:")) {
            return id;
        }
        VocabularyTerm phObj = this.ontologyService.getTerm(id);
        if (phObj != null) {
            return phObj.getTranslatedName();
        }
        return id;
    }

    private boolean hasDescendantsInOntology(String id)
    {
        if (!id.startsWith("HP:")) {
            return false;
        }
        Map<String, String> params = new HashMap<>();
        params.put(INDEXED_PARENT_KEY, id);
        return (this.ontologyService.count(params) > 0);
    }

    @SuppressWarnings("unchecked")
    private List<String> getCategoriesFromOntology(String value)
    {
        if (!value.startsWith("HP:")) {
            return Collections.emptyList();
        }
        VocabularyTerm termObj = this.ontologyService.getTerm(value);
        if (termObj != null && termObj.get(INDEXED_CATEGORY_KEY) != null
            && List.class.isAssignableFrom(termObj.get(INDEXED_CATEGORY_KEY).getClass())) {
            return (List<String>) termObj.get(INDEXED_CATEGORY_KEY);
        }
        return new LinkedList<>();
    }

    private List<String> getCategoriesFromCustomMapping(String value, Map<String, List<String>> customCategories)
    {
        for (Map.Entry<String, List<String>> category : customCategories.entrySet()) {
            if (StringUtils.equals(value, category.getKey()) && category.getValue() != null) {
                return category.getValue();
            }
        }
        return new LinkedList<>();
    }

    private void prepareMetaData()
    {
        this.metadata = new HashMap<>();
        for (com.xpn.xwiki.api.Object o : this.data.getDocument().getObjects("PhenoTips.PhenotypeMetaClass")) {
            String name = "";
            String category = "";
            StringBuilder value = new StringBuilder();
            for (String propname : o.getxWikiClass().getEnabledPropertyNames()) {
                Property property = o.getProperty(propname);
                if (property == null || property.getValue() == null) {
                    continue;
                }
                Object propvalue = property.getValue();
                if (StringUtils.equals("target_property_name", propname)) {
                    category = propvalue.toString();
                } else if (StringUtils.equals("target_property_value", propname)) {
                    name = propvalue.toString();
                } else {
                    String str = o.get(propname).toString();
                    str = str.replaceAll("\\{\\{/?html[^}]*+}}", "");
                    if (StringUtils.isBlank(str)) {
                        continue;
                    }
                    str = str.replaceFirst("^(<p>)?", "<dd>").replaceFirst("(</p>)?$", "</dd>");
                    // Additional images and documents shouldn't be escaped, since they produce a complex HTML fragment
                    // which already takes care of properly escaping user-entered content
                    if (!propname.startsWith("supporting_")) {
                        str = XMLUtils.escapeElementContent(str);
                        str = str.replaceAll("&#60;(/?)dd&#62;", "<$1dd>");
                    }
                    value.append(str);
                }
            }
            if (StringUtils.isNotBlank(name) && value.length() > 0) {
                Map<String, String> subvalues = this.metadata.get(name);
                if (subvalues == null) {
                    subvalues = new HashMap<>();
                    this.metadata.put(name, subvalues);
                }
                subvalues.put(category, "<div class='phenotype-details'><dl>" + value.toString() + "</dl></div>");
            }

        }
    }
}
