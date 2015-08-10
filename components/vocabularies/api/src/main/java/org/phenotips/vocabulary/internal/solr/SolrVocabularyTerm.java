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
package org.phenotips.vocabulary.internal.solr;

import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyTerm;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Implementation for {@link VocabularyTerm} based on an indexed Solr document.
 *
 * @version $Id$
 * @since 1.2M4 (under different names since 1.0M8)
 */
public class SolrVocabularyTerm implements VocabularyTerm
{
    private static final String ID = "id";

    private static final String NAME = "name";

    private static final String DEF = "def";

    private static final String TERM_CATEGORY = "term_category";

    private static final String IS_A = "is_a";

    /** The Solr document representing this term. */
    private SolrDocument doc;

    /**
     * The owner ontology.
     *
     * @see #getVocabulary()
     */
    private Vocabulary ontology;

    /**
     * The parents of this term, transformed from a set of IDs into a real set of terms.
     *
     * @see #getParents()
     */
    private Set<VocabularyTerm> parents;

    /**
     * The ancestors of this term, transformed from a set of IDs into a real set of terms.
     *
     * @see #getAncestors()
     */
    private Set<VocabularyTerm> ancestors;

    /**
     * A set containing the term itself and its ancestors, transformed from a set of IDs into a real set of terms.
     *
     * @see #getAncestorsAndSelf()
     */
    private Set<VocabularyTerm> ancestorsAndSelf;

    /**
     * Constructor that provides the backing {@link #doc Solr document} and the {@link #ontology owner ontology}.
     *
     * @param doc the {@link #doc Solr document} representing this term
     * @param ontology the {@link #ontology owner ontology}
     */
    public SolrVocabularyTerm(SolrDocument doc, Vocabulary ontology)
    {
        this.doc = doc;
        this.ontology = ontology;
        if (doc != null) {
            this.removeSelfDuplicate();
            this.parents = new LazySolrTermSet(doc.getFieldValues(IS_A), ontology);
            this.ancestors = new LazySolrTermSet(doc.getFieldValues(TERM_CATEGORY), ontology);
            Collection<Object> termSet = new HashSet<Object>();
            termSet.add(this.getId());
            if (doc.getFieldValues(TERM_CATEGORY) != null) {
                termSet.addAll(doc.getFieldValues(TERM_CATEGORY));
            }
            this.ancestorsAndSelf = new LazySolrTermSet(termSet, this.ontology);
        }
    }

    /**
     * The field "term_category" in {@code this.doc} can contain the term itself. It appears that this only happens with
     * HPO. To avoid this problem, and to avoid writing a separate implementation for HPO specifically, this method
     * checks for existence of the term in the term_category and takes it out.
     */
    private void removeSelfDuplicate()
    {
        Object value = this.doc.getFieldValue(TERM_CATEGORY);
        if (!(value instanceof List)) {
            return;
        }
        @SuppressWarnings("unchecked")
        List<String> listValue = (List<String>) value;
        listValue.remove(this.getId());
    }

    @Override
    public String getId()
    {
        return this.doc != null ? (String) this.doc.getFirstValue(ID) : null;
    }

    @Override
    public String getName()
    {
        return this.doc != null ? (String) this.doc.getFirstValue(NAME) : null;
    }

    @Override
    public String getDescription()
    {
        return this.doc != null ? (String) this.doc.getFirstValue(DEF) : null;
    }

    @Override
    public Set<VocabularyTerm> getParents()
    {
        return this.parents != null ? this.parents : Collections.<VocabularyTerm>emptySet();
    }

    @Override
    public Set<VocabularyTerm> getAncestors()
    {
        return this.ancestors != null ? this.ancestors : Collections.<VocabularyTerm>emptySet();
    }

    @Override
    public Set<VocabularyTerm> getAncestorsAndSelf()
    {
        return this.ancestorsAndSelf != null ? this.ancestorsAndSelf : Collections.<VocabularyTerm>emptySet();
    }

    @Override
    public Object get(String name)
    {
        return this.doc != null ? this.doc.getFieldValue(name) : null;
    }

    @Override
    public Vocabulary getVocabulary()
    {
        return this.ontology;
    }

    @Override
    public String toString()
    {
        return "[" + this.getId() + "] " + this.getName();
    }

    @Override
    public long getDistanceTo(final VocabularyTerm other)
    {
        if (other == null) {
            return -1;
        }
        if (this.equals(other)) {
            return 0;
        }

        long distance = Integer.MAX_VALUE;

        Map<String, Integer> myLevelMap = new HashMap<String, Integer>();
        myLevelMap.put(getId(), 0);
        Map<String, Integer> otherLevelMap = new HashMap<String, Integer>();
        otherLevelMap.put(other.getId(), 0);

        Set<VocabularyTerm> myCrtLevel = new HashSet<VocabularyTerm>();
        myCrtLevel.add(this);
        Set<VocabularyTerm> otherCrtLevel = new HashSet<VocabularyTerm>();
        otherCrtLevel.add(other);

        for (int l = 1; l <= distance && !myCrtLevel.isEmpty() && !otherCrtLevel.isEmpty(); ++l) {
            distance = Math.min(distance, processAncestorsAtDistance(l, myCrtLevel, myLevelMap, otherLevelMap));
            distance = Math.min(distance, processAncestorsAtDistance(l, otherCrtLevel, otherLevelMap, myLevelMap));
        }
        return distance == Integer.MAX_VALUE ? -1 : distance;
    }

    private long processAncestorsAtDistance(int localDistance, Set<VocabularyTerm> sourceUnprocessedAncestors,
        Map<String, Integer> sourceDistanceMap, Map<String, Integer> targetDistanceMap)
    {
        long minDistance = Integer.MAX_VALUE;
        Set<VocabularyTerm> nextLevel = new HashSet<VocabularyTerm>();
        for (VocabularyTerm term : sourceUnprocessedAncestors) {
            for (VocabularyTerm parent : term.getParents()) {
                if (sourceDistanceMap.containsKey(parent.getId())) {
                    continue;
                }
                if (targetDistanceMap.containsKey(parent.getId())) {
                    minDistance = Math.min(minDistance, targetDistanceMap.get(parent.getId()) + localDistance);
                }
                nextLevel.add(parent);
                sourceDistanceMap.put(parent.getId(), localDistance);
            }
        }
        sourceUnprocessedAncestors.clear();
        sourceUnprocessedAncestors.addAll(nextLevel);

        return minDistance;
    }

    @Override
    public JSON toJSON()
    {
        JSONObject json = new JSONObject();

        Iterator<Map.Entry<String, Object>> fieldIterator = this.doc.iterator();

        while (fieldIterator.hasNext()) {
            Map.Entry<String, Object> field = fieldIterator.next();
            addAsCorrectType(json, field.getKey(), field.getValue());
        }

        return json;
    }

    private void addAsCorrectType(JSONObject json, String name, Object toAdd)
    {
        if (toAdd instanceof Collection) {
            JSONArray array = new JSONArray();
            array.addAll(Collection.class.cast(toAdd));
            json.put(name, array);
        } else {
            json.put(name, toAdd);
        }
    }

    @Override
    public int hashCode()
    {
        String id = getId();
        return (id != null ? id.hashCode() : 0);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof VocabularyTerm)) {
            return false;
        }
        return StringUtils.equals(getId(), ((VocabularyTerm) obj).getId());
    }
}
