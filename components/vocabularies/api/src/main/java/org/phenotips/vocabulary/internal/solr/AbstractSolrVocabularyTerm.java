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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Abstract implementation of common functionality of {@link VocabularyTerm} for solr documents.
 *
 * @version $Id$
 */
public abstract class AbstractSolrVocabularyTerm implements VocabularyTerm
{
    /**
     * The name of the id field.
     */
    protected static final String ID = "id";

    /**
     * The name of the name field.
     */
    protected static final String NAME = "name";

    /**
     * The name of the definition field.
     */
    protected static final String DEF = "def";

    /**
     * The name of the term category field.
     */
    protected static final String TERM_CATEGORY = "term_category";

    /**
     * The name of the is_a field.
     */
    protected static final String IS_A = "is_a";

    /**
     * The owner ontology.
     *
     * @see #getVocabulary()
     */
    protected final Vocabulary ontology;

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
     * Constructor linking to the vocabulary.
     *
     * @param ontology the {@link #ontology owner ontology}
     */
    public AbstractSolrVocabularyTerm(Vocabulary ontology)
    {
        this.ontology = ontology;
    }

    protected void initialize()
    {
        if (!isNull()) {
            this.removeSelfDuplicate();
            this.parents = new LazySolrTermSet(getValues(IS_A), this.ontology);
            this.ancestors = new LazySolrTermSet(getValues(TERM_CATEGORY), this.ontology);
            Collection<Object> termSet = getAncestorsAndSelfTermSet();
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
        Object value = getFirstValue(TERM_CATEGORY);
        if (!(value instanceof List)) {
            return;
        }
        @SuppressWarnings("unchecked")
        List<String> listValue = (List<String>) value;
        listValue.remove(this.getId());
    }

    /**
     * Return the term set for the getAncestorsAndSelf method.
     *
     * @return the set
     */
    protected Collection<Object> getAncestorsAndSelfTermSet()
    {
        Collection<Object> termSet = new LinkedHashSet<>();
        termSet.add(this.getId());
        if (getValues(TERM_CATEGORY) != null) {
            termSet.addAll(getValues(TERM_CATEGORY));
        }
        return termSet;
    }

    /**
     * Return whether the doc contained by this vocabulary term is null.
     *
     * @return whether it is null
     */
    protected abstract boolean isNull();

    /**
     * Get the values corresponding to the key given in the document.
     *
     * @param key the key
     * @return the collection of values
     */
    protected abstract Collection<Object> getValues(String key);

    /**
     * Get the first value corresponding to the key given in the document.
     *
     * @param key the key
     * @return the first value
     */
    protected abstract Object getFirstValue(String key);

    @Override
    public String getId()
    {
        return (String) getFirstValue(ID);
    }

    @Override
    public String getName()
    {
        return (String) getFirstValue(NAME);
    }

    @Override
    public String getDescription()
    {
        return (String) getFirstValue(DEF);
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

        long distance = Long.MAX_VALUE;

        Map<String, Integer> myLevelMap = new HashMap<>();
        myLevelMap.put(getId(), 0);
        Map<String, Integer> otherLevelMap = new HashMap<>();
        otherLevelMap.put(other.getId(), 0);

        Set<VocabularyTerm> myCrtLevel = new HashSet<>();
        myCrtLevel.add(this);
        Set<VocabularyTerm> otherCrtLevel = new HashSet<>();
        otherCrtLevel.add(other);

        for (int l = 1; l <= distance && (!myCrtLevel.isEmpty() || !otherCrtLevel.isEmpty()); ++l) {
            distance = Math.min(distance, processAncestorsAtDistance(l, myCrtLevel, myLevelMap, otherLevelMap));
            distance = Math.min(distance, processAncestorsAtDistance(l, otherCrtLevel, otherLevelMap, myLevelMap));
        }
        return distance == Long.MAX_VALUE ? -1 : distance;
    }

    private long processAncestorsAtDistance(int localDistance, Set<VocabularyTerm> sourceUnprocessedAncestors,
        Map<String, Integer> sourceDistanceMap, Map<String, Integer> targetDistanceMap)
    {
        long minDistance = Long.MAX_VALUE;
        Set<VocabularyTerm> nextLevel = new HashSet<>();
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

    /**
     * Get an iterable of the entires in this document.
     *
     * @return the entries as Map.Entry instances
     */
    protected abstract Iterable<Map.Entry<String, Object>> getEntrySet();

    @Override
    public JSONObject toJSON()
    {
        JSONObject json = new JSONObject();

        for (Map.Entry<String, ? extends Object> field : getEntrySet()) {
            addAsCorrectType(json, field.getKey(), field.getValue());
        }

        return json;
    }

    private void addAsCorrectType(JSONObject json, String name, Object toAdd)
    {
        if (toAdd instanceof Collection) {
            JSONArray array = new JSONArray();
            for (Object item : Collection.class.cast(toAdd)) {
                array.put(item);
            }
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
        if (!(obj instanceof VocabularyTerm)) {
            return false;
        }
        return StringUtils.equals(getId(), ((VocabularyTerm) obj).getId());
    }
}
