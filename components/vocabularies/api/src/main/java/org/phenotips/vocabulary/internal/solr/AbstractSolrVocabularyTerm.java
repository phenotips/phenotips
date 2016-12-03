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

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocumentBase;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Abstract implementation of common functionality of {@link VocabularyTerm} for Solr documents.
 *
 * @version $Id$
 * @since 1.3M5
 */
public abstract class AbstractSolrVocabularyTerm implements VocabularyTerm
{
    /**
     * The name of the Solr field used for storing the identifier of a term.
     *
     * @see #getId()
     */
    protected static final String ID_KEY = "id";

    /**
     * The name of the Solr field used for storing the name of a term.
     *
     * @see #getName()
     */
    protected static final String NAME = "name";

    /**
     * The name of the Solr field used for storing the description of a term.
     *
     * @see #getDescription()
     */
    protected static final String DESCRIPTION = "def";

    /**
     * The name of the Solr field used for storing the ancestors of a term.
     *
     * @see #getAncestors()
     */
    protected static final String ANCESTORS_KEY = "term_category";

    /**
     * The name of the Solr field used for storing the direct parents of a term.
     *
     * @see #getParents()
     */
    protected static final String PARENTS_KEY = "is_a";

    /**
     * The owner vocabulary.
     *
     * @see #getVocabulary()
     */
    protected final Vocabulary vocabulary;

    /** The Solr document representing this term. */
    protected final SolrDocumentBase<? extends Object, ? extends SolrDocumentBase<?, ?>> doc;

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
     * @param vocabulary the {@link #vocabulary owner vocabulary}
     * @param doc the Solr document holding the actual data
     */
    public AbstractSolrVocabularyTerm(SolrDocumentBase<? extends Object, ? extends SolrDocumentBase<?, ?>> doc,
        Vocabulary vocabulary)
    {
        this.doc = doc;
        this.vocabulary = vocabulary;
    }

    protected void initialize()
    {
        if (!isNull()) {
            this.removeSelfFromAncestors();
            this.parents = new LazySolrTermSet(getValues(PARENTS_KEY), this.vocabulary);
            this.ancestors = new LazySolrTermSet(getValues(ANCESTORS_KEY), this.vocabulary);
            this.ancestorsAndSelf = getUncachedAncestorsAndSelf();
        }
    }

    @Override
    public String getId()
    {
        return (String) getFirstValue(ID_KEY);
    }

    @Override
    public String getName()
    {
        return (String) getFirstValue(NAME);
    }

    @Override
    public String getDescription()
    {
        return (String) getFirstValue(DESCRIPTION);
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
    public Object get(String key)
    {
        if (isNull()) {
            return null;
        }
        return this.doc.getFieldValue(key);
    }

    @Override
    public Vocabulary getVocabulary()
    {
        return this.vocabulary;
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

    @Override
    public JSONObject toJSON()
    {
        JSONObject json = new JSONObject();

        for (Map.Entry<String, ? extends Object> field : getEntrySet()) {
            addAsCorrectType(json, field.getKey(), field.getValue());
        }

        return json;
    }

    /**
     * Get all the entries in this document.
     *
     * @return the entries as Map.Entry instances, or an empty collection if no entries are set
     */
    protected Set<Map.Entry<String, Object>> getEntrySet()
    {
        if (isNull()) {
            return Collections.emptySet();
        }
        Set<String> keys = this.doc.keySet();
        Set<Map.Entry<String, Object>> result = new LinkedHashSet<>(keys.size());
        for (String key : keys) {
            result.add(new AbstractMap.SimpleImmutableEntry<>(key, get(key)));
        }
        return result;
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

    /**
     * Returns whether the Solr document defining this vocabulary term is {@code null}.
     *
     * @return {@code true} if this vocabulary term doesn't have a valid Solr document set, and is thus unusable
     */
    protected boolean isNull()
    {
        return this.doc == null;
    }

    /**
     * Gets the values corresponding to the target key.
     *
     * @param key the name of the field to retrieve
     * @return the collection of values, or {@code null} if no values are set for the target key
     */
    protected Collection<Object> getValues(String key)
    {
        if (isNull()) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Collection<Object> values = this.doc.getFieldValues(key);
        return values;
    }

    /**
     * Retrieves the first value corresponding to the target key. This may be the only value set for a simple field, or
     * the first in a collection of values for a multi-valued field.
     *
     * @param key the name of the field to retrieve
     * @return the first value, or {@code null} if there's no value set for the target key
     */
    protected Object getFirstValue(String key)
    {
        Collection<Object> values = getValues(key);
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }
        return CollectionUtils.get(values, 0);
    }

    /**
     * The field "term_category" in {@code this.doc} can contain the term itself. It appears that this only happens with
     * HPO. To avoid this problem, and to avoid writing a separate implementation for HPO specifically, this method
     * checks for existence of the term in the term_category and takes it out.
     */
    private void removeSelfFromAncestors()
    {
        Object value = getFirstValue(ANCESTORS_KEY);
        if (!(value instanceof List)) {
            return;
        }
        @SuppressWarnings("unchecked")
        List<String> listValue = (List<String>) value;
        listValue.remove(this.getId());
    }

    /**
     * Returns the term set for the {@link #getAncestorsAndSelf()} method.
     *
     * @return a set of identifiers, containing at least the identifier of this term
     */
    protected Set<VocabularyTerm> getUncachedAncestorsAndSelf()
    {
        Collection<Object> termSet = new LinkedHashSet<>();
        termSet.add(this.getId());
        if (getValues(ANCESTORS_KEY) != null) {
            termSet.addAll(getValues(ANCESTORS_KEY));
        }
        return new LazySolrTermSet(termSet, this.vocabulary);
    }
}
