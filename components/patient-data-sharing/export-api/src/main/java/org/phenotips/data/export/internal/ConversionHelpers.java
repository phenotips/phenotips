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

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.Feature;
import org.phenotips.ontology.OntologyService;
import org.phenotips.ontology.OntologyTerm;
import org.phenotips.tools.PhenotypeMappingService;
import org.phenotips.tools.PropertyDisplayer;

import org.xwiki.component.manager.ComponentManager;
import org.xwiki.script.service.ScriptService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * These are supplementary functions to the main conversion functions.
 */
public class ConversionHelpers
{
    private Boolean positive;

    private Boolean negative;

    private OntologyService ontologyService;

    private Map<String, ArrayList<String>> categoryMapping;

    /** Feature to Section */
    private Map<String, String> sectionFeatureTree;

    public void newPatient()
    {
        sectionFeatureTree = new HashMap<String, String>();
    }

    public void featureSetUp(Boolean positive, Boolean negative, Boolean mapCategories) throws Exception
    {
        this.positive = positive;
        this.negative = negative;
        if (!mapCategories) {
            return;
        }

        ComponentManager cm = ComponentManagerRegistry.getContextComponentManager();
        this.ontologyService = cm.getInstance(OntologyService.class, "hpo");
        PhenotypeMappingService mappingService = cm.getInstance(ScriptService.class, "phenotypeMapping");
        Object _mapping = mappingService.get("phenotype");
        if (_mapping instanceof ArrayList) {
            List<Map<String, Object>> fullMapping = (List<Map<String, Object>>) _mapping;
            categoryMapping = new LinkedHashMap<String, ArrayList<String>>();
            for (Map<String, Object> categoryEntry : fullMapping) {
                categoryMapping
                    .put(categoryEntry.get("title").toString(), (ArrayList<String>) categoryEntry.get("categories"));
            }
        } else {
            throw new Exception("The phenotype category list is not available");
        }
    }

    private List<Feature> sortFeaturesByPresentStatus(Set<? extends Feature> features, Boolean status)
    {
        List<Feature> sortedFeatures = new LinkedList<Feature>();
        for (Feature feature : features) {
            if (feature.isPresent() == status && positive) {
                sortedFeatures.add(0, feature);
            }
        }
        return sortedFeatures;
    }

    public List<Feature> sortFeaturesSimple(Set<? extends Feature> features)
    {
        List<Feature> positiveList = sortFeaturesByPresentStatus(features, true);
        List<Feature> negativeList = sortFeaturesByPresentStatus(features, false);

        positiveList.addAll(negativeList);
        return positiveList;
    }

    public List<Feature> sortFeaturesWithSections(Set<? extends Feature> features)
    {
        List<Feature> positiveList = sortFeaturesBySection(sortFeaturesByPresentStatus(features, true));
        List<Feature> negativeList = sortFeaturesBySection(sortFeaturesByPresentStatus(features, false));

        positiveList.addAll(negativeList);
        return positiveList;
    }

    private List<Feature> sortFeaturesBySection(List<Feature> features)
    {
        List<Feature> sortedFeatures = new LinkedList<Feature>();

        Map<String, ArrayList<String>> mapping = getCategoryMapping();
        for (String section : mapping.keySet()) {
            if (features.isEmpty()) {
                break;
            }

            for (String category : mapping.get(section)) {
                Set<Feature> toRemove = new HashSet<Feature>();
                for (Feature feature : features) {
                    if (getCategoriesFromOntology(feature.getId()).contains(category) ||
                        StringUtils.equals(feature.getId(), category))
                    {
                        sectionFeatureTree.put(feature.getId(), section);
                        sortedFeatures.add(feature);
                        toRemove.add(feature);
                    }
                }
                for (Feature feature : toRemove) {
                    features.remove(feature);
                }
            }
        }
        return sortedFeatures;
    }

    @SuppressWarnings("unchecked")
    private List<String> getCategoriesFromOntology(String value)
    {
        if (!value.startsWith("HP:")) {
            return Collections.emptyList();
        }
        OntologyTerm termObj = this.ontologyService.getTerm(value);
        if (termObj != null && termObj.get(PropertyDisplayer.INDEXED_CATEGORY_KEY) != null
            && List.class.isAssignableFrom(termObj.get(PropertyDisplayer.INDEXED_CATEGORY_KEY).getClass()))
        {
            /* Could use ancestorAndSelf, but not sure that is necessary and likely is slower */
            return (List<String>) termObj.get(PropertyDisplayer.INDEXED_CATEGORY_KEY);
        }
        return new LinkedList<String>();
    }

    public Map<String, ArrayList<String>> getCategoryMapping()
    {
        return categoryMapping;
    }

    public Map<String, String> getSectionFeatureTree()
    {
        return sectionFeatureTree;
    }

    public static String wrapString(String string, Integer charactersPerLine)
    {
        StringBuilder returnString = new StringBuilder(string);
        Integer counter = charactersPerLine;
        Character nextChar = null;
        while(counter < string.length()) {
            Boolean found = false;
            /* TODO. See if this breaks in Unicode */
            while (nextChar == null || nextChar.compareTo(' ') != 0) {
                nextChar = string.charAt(counter);
                counter++;
                found = true;
            }
            if (found) {
                returnString.insert(counter, "\n");
            }

            counter += charactersPerLine;
        }
        return returnString.toString();
    }

    public static String strIntegerToStrBool(String strInt)
    {
        if (StringUtils.equals("0", strInt)) {
            return "No";
        } else if (StringUtils.equals("1", strInt)) {
            return "Yes";
        } else if (StringUtils.equals("", strInt)) {
            return "";
        } else {
            return "N/A";
        }
    }

    public static String integerToStrBool(Integer integer)
    {
        if (integer == null) {
            return "";
        }
        if (integer == 0) {
            return "No";
        } else if (integer == 1) {
            return "Yes";
        } else {
            return "N/A";
        }
    }
}
