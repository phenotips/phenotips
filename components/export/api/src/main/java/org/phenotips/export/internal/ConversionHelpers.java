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
import org.phenotips.data.Feature;
import org.phenotips.tools.PhenotypeMappingService;
import org.phenotips.tools.PropertyDisplayer;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.manager.ComponentManager;
import org.xwiki.script.service.ScriptService;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * Contains supplementary functions to the main conversion functions. It is used as an instance, rather that a
 * collection of static functions.
 *
 * @version $Id$
 * @since 1.0RC1
 */
public class ConversionHelpers
{
    private static final String YES = "Yes";

    private static final String NO = "No";

    private static final String NA = "N/A";

    /** Global parameter for whether to include phenotypes with "present" status. */
    private Boolean positive;

    /** Global parameter for whether to include phenotypes with "not present" status. */
    private Boolean negative;

    /** Used for accessing HPO. */
    private Vocabulary ontologyService;

    /** The titles of phenotypic categories mapped to a list of HPO ids which represent that category. */
    private Map<String, List<String>> categoryMapping;

    /** Phenotypic feature to phenotypic category section map. Used for sorting features by category. */
    private Map<String, String> sectionFeatureTree;

    /**
     * Clears the {@link #sectionFeatureTree} and must be called before processing each patient, as each patient has a
     * different set of phenotypes.
     */
    public void newPatient()
    {
        this.sectionFeatureTree = new HashMap<>();
    }

    /**
     * Sets global parameters that are used by various other functions.
     *
     * @param positive sets the global parameter {@link #positive}
     * @param negative same as the above positive parameter, but for {@link #negative}
     * @param mapCategories whether the phenotypes will be sorted by which category they belong to
     * @throws java.lang.Exception Could happen if the {@link org.phenotips.vocabulary.Vocabulary} for HPO could not be
     *             accessed or is the phenotype category list is not available
     */
    public void featureSetUp(Boolean positive, Boolean negative, Boolean mapCategories) throws Exception
    {
        /* Set to true to include, and false to not include phenotypes with positive/negative status. */
        this.positive = positive;
        this.negative = negative;
        if (!mapCategories) {
            return;
        }

        /*
         * Gets a list of all categories, and maps each category's title to a list of HPO ids which represent it. This
         * step is necessary only if {@link #mapCategories} is true.
         */
        ComponentManager cm = getComponentManager();
        this.ontologyService = cm.getInstance(Vocabulary.class, "hpo");
        PhenotypeMappingService mappingService = cm.getInstance(ScriptService.class, "phenotypeMapping");
        Object mappingObject = mappingService.get("phenotype");
        if (mappingObject instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, List<String>>> mapping = (List<Map<String, List<String>>>) mappingObject;
            this.categoryMapping = new LinkedHashMap<>();
            for (Map<String, List<String>> categoryEntry : mapping) {
                this.categoryMapping.put(categoryEntry.get("title").toString(), categoryEntry.get("categories"));
            }
        } else {
            throw new Exception("The phenotype category list is not available");
        }
    }

    /** A necessary evil for testing. */
    protected ComponentManager getComponentManager()
    {
        return ComponentManagerRegistry.getContextComponentManager();
    }

    /**
     * Simple filter, which returns a subset of features of the passed in "features" parameter that have the status of
     * "status" parameter, or an empty list if the global setting disallows features with the status of { @param
     * status}.
     */
    private List<Feature> filterFeaturesByPresentStatus(Set<? extends Feature> features, Boolean status)
    {
        List<Feature> filteredFeatures = new LinkedList<>();
        boolean include = status ? this.positive : this.negative;
        if (include) {
            for (Feature feature : features) {
                if (feature.isPresent() == status) {
                    filteredFeatures.add(0, feature);
                }
            }
        }
        return filteredFeatures;
    }

    /**
     * Sorts the passed in features so that the features with the "is present" status are on top. Also filters out
     * features which are disallowed by the global {@link #positive} and {@link #negative}.
     *
     * @param features set of features to be filtered. Cannot be null
     * @return a subset of the passed in features in a specific order
     */
    public List<Feature> sortFeaturesSimple(Set<? extends Feature> features)
    {
        List<Feature> positiveList = filterFeaturesByPresentStatus(features, true);
        List<Feature> negativeList = filterFeaturesByPresentStatus(features, false);

        positiveList.addAll(negativeList);
        return positiveList;
    }

    /**
     * Sorts passed in features by phenotypic category section, first sorting the features with the "is present" status
     * and then with the "not present" status. Since it uses
     * {@link #filterFeaturesByPresentStatus(java.util.Set, Boolean)}, the returned list of features is subject to the
     * global {@link #positive} and {@link #negative}.
     *
     * @param features set of features to sort. Cannot be null
     * @return a subset of the passed in features in a specific order
     */
    public List<Feature> sortFeaturesWithSections(Set<? extends Feature> features)
    {
        List<Feature> positiveList = sortFeaturesBySection(filterFeaturesByPresentStatus(features, true));
        List<Feature> negativeList = sortFeaturesBySection(filterFeaturesByPresentStatus(features, false));

        positiveList.addAll(negativeList);
        return positiveList;
    }

    /**
     * Fills {@link #sectionFeatureTree} with feature ids mapped to section names. This function is used internally only
     * in {@link #sortFeaturesWithSections(java.util.Set)}; if changing that, keep in mind the mutation of
     * {@link #sectionFeatureTree}.
     *
     * @param features list of features to be sorted. Cannot be null
     * @return list of features sorted in the same order as {@link #categoryMapping}
     */
    private List<Feature> sortFeaturesBySection(List<Feature> features)
    {
        List<Feature> sortedFeatures = new LinkedList<>();

        Map<String, List<String>> mapping = this.getCategoryMapping();
        for (String section : mapping.keySet()) {
            if (features.isEmpty()) {
                break;
            }

            /* Each section can have several HPO ids (categories) attached to it. */
            for (String category : mapping.get(section)) {
                Iterator<Feature> iter = features.iterator();
                while (iter.hasNext()) {
                    Feature feature = iter.next();
                    if (getCategoriesFromOntology(feature.getId()).contains(category)
                        || StringUtils.equals(feature.getId(), category)) {
                        this.sectionFeatureTree.put(feature.getId(), section);
                        sortedFeatures.add(feature);
                        iter.remove();
                    }
                }
            }
        }
        for (Feature feature : features) {
            this.sectionFeatureTree.put(feature.getId(), "No category");
        }
        sortedFeatures.addAll(features);
        return sortedFeatures;
    }

    /**
     * Filters features based on their prenatal status.
     *
     * @param features set of features to be filtered. Cannot be null
     * @param prenatal if true returns prenatal features, if false non-prenatal
     * @return a subset of passed in features
     */
    public Set<Feature> filterFeaturesByPrenatal(Set<? extends Feature> features, Boolean prenatal)
    {
        Set<Feature> filtered = new HashSet<>();
        for (Feature feature : features) {
            if (StringUtils.equals(feature.getType(), "prenatal_phenotype") == prenatal) {
                filtered.add(feature);
            }
        }
        return filtered;
    }

    /**
     * Given an HPO id, finds categories to which the id belongs to.
     *
     * @param value must start with "HP:"
     * @return a list of categories as HPO ids, excluding the passed in id, or an empty list if the categories could not
     *         be determined
     */
    @SuppressWarnings("unchecked")
    private List<String> getCategoriesFromOntology(String value)
    {
        if (!value.startsWith("HP:")) {
            return Collections.emptyList();
        }
        VocabularyTerm termObj = this.ontologyService.getTerm(value);
        if (termObj != null && termObj.get(PropertyDisplayer.INDEXED_CATEGORY_KEY) != null
            && List.class.isAssignableFrom(termObj.get(PropertyDisplayer.INDEXED_CATEGORY_KEY).getClass())) {
            return (List<String>) termObj.get(PropertyDisplayer.INDEXED_CATEGORY_KEY);
        }
        return new LinkedList<>();
    }

    /**
     * @return mapping of category titles to their respective lists of HPO ids
     */
    public Map<String, List<String>> getCategoryMapping()
    {
        return this.categoryMapping;
    }

    /**
     * @return mappings of HPO id to the title of the category the id belongs to
     */
    public Map<String, String> getSectionFeatureTree()
    {
        return this.sectionFeatureTree;
    }

    /**
     * Converts an integer (which is of type {@link java.lang.String}) to a human readable boolean.
     *
     * @param strInt an integer in a string
     * @return "No" for "0", "Yes" for "1", an empty string for an empty string, and "N/A" in all other cases
     */
    public static String strIntegerToStrBool(String strInt)
    {
        if (StringUtils.equals("0", strInt)) {
            return NO;
        } else if (StringUtils.equals("1", strInt)) {
            return YES;
        } else if (StringUtils.equals("", strInt)) {
            return "";
        } else {
            return NA;
        }
    }

    /**
     * Converts an integer to a human readable boolean.
     *
     * @param integer Could be any value including {@code null}, but should be either 0 or 1
     * @return "No" for "0", "Yes" for "1", an empty string for null, and "N/A" in all other cases
     */
    public static String integerToStrBool(Integer integer)
    {
        if (integer == null) {
            return "";
        }
        if (integer == 0) {
            return NO;
        } else if (integer == 1) {
            return YES;
        } else {
            return NA;
        }
    }

    /**
     * MS excel is unable to open a spreadsheet with cells containing more than 32k characters. To prevent that, the
     * cell's contents are split into several cells, which are positioned under each other.
     *
     * @param value the value for a {@link org.phenotips.export.internal.DataCell}, which is checked to be shorter than
     *            32k characters. Can be {@code null}
     * @param x the initial x position for the returned cells
     * @param y the initial y position for the returned cells
     * @return a list of {@link org.phenotips.export.internal.DataCell}s, which in combination will contain the whole
     *         string passed in under `value` parameter
     */
    public static List<DataCell> preventOverflow(String value, int x, int y)
    {
        final int maxSize = 32000;
        List<DataCell> processed = new LinkedList<>();
        if (value == null || value.length() < maxSize) {
            processed.add(new DataCell(value, x, y));
        } else {
            int iY = y;
            List<String> chunks = new LinkedList<>();
            determineSplit(value, maxSize, chunks);
            for (String chunk : chunks) {
                processed.add(new DataCell(chunk, x, iY));
                iY++;
            }
        }
        return processed;
    }

    /**
     * Splits a sting into chunks with size equal or smaller than the specified guided by paragraphs and sentences.
     *
     * @param holder there are side effects on this variable; chunks are recursively added into this list
     */
    private static void determineSplit(String value, int chunkSizeLimit, List<String> holder)
    {
        final int tailSize = 1000;
        final String newline = "\n";
        final String period = ".";
        // Relative to the tail start
        int chunkEndIndex = -1;
        boolean foundBreakIndex = false;
        String chunkTail = value.substring(chunkSizeLimit - tailSize, chunkSizeLimit);

        if (chunkTail.contains(newline)) {
            chunkEndIndex = chunkTail.lastIndexOf(newline);
            foundBreakIndex = chunkEndIndex >= 0;
            chunkEndIndex += newline.length();
        } else if (chunkTail.contains(period)) {
            chunkEndIndex = chunkTail.lastIndexOf(period);
            foundBreakIndex = chunkEndIndex >= 0;
            chunkEndIndex += period.length();
        } else {
            chunkEndIndex = chunkTail.lastIndexOf(' ');
            foundBreakIndex = chunkEndIndex >= 0;
            chunkEndIndex += 1;
        }
        /* In case all checks failed, splitting at maximum length */
        chunkEndIndex = foundBreakIndex ? chunkEndIndex : tailSize;

        int chunkSize = chunkSizeLimit - tailSize + chunkEndIndex;
        String chunk = value.substring(0, chunkSize);
        String chunkOverflow = value.substring(chunkSize);

        holder.add(chunk.trim());
        if (chunkOverflow.length() > chunkSizeLimit) {
            determineSplit(chunkOverflow, chunkSizeLimit, holder);
        } else {
            holder.add(chunkOverflow.trim());
        }
    }
}
