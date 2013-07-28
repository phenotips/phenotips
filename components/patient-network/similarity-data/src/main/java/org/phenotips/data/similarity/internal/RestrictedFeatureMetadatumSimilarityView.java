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
package org.phenotips.data.similarity.internal;

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.FeatureMetadatum;
import org.phenotips.data.similarity.AccessType;
import org.phenotips.data.similarity.FeatureMetadatumSimilarityScorer;
import org.phenotips.data.similarity.FeatureMetadatumSimilarityView;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;

import org.apache.commons.lang3.StringUtils;

import net.sf.json.JSONObject;

/**
 * Implementation of {@link FeatureMetadatumSimilarityView} that only reveals information if the user has full access to
 * the patient.
 * 
 * @version $Id$
 * @since 1.0M8
 */
public class RestrictedFeatureMetadatumSimilarityView implements FeatureMetadatumSimilarityView
{
    /** The matched feature metadatum to represent. */
    private FeatureMetadatum match;

    /** The reference feature metadatum against which to compare. */
    private FeatureMetadatum reference;

    /** The access type the user has to the patient having this phenotype. */
    private AccessType access;

    /**
     * Simple constructor passing the {@link #match matched metadatum}, the {@link #reference reference metadatum}, and
     * the {@link #access patient access type}.
     * 
     * @param match the matched feature metadatum to represent
     * @param reference the reference feature metadatum against which to compare, can be {@code null}
     * @param access the access type the user has to the patient having the described feature
     */
    public RestrictedFeatureMetadatumSimilarityView(FeatureMetadatum match, FeatureMetadatum reference,
        AccessType access)
    {
        this.match = match;
        this.reference = reference;
        this.access = access != null ? access : AccessType.PRIVATE;
    }

    @Override
    public String getType()
    {
        if (!this.access.isOpenAccess()) {
            return null;
        }
        return this.match != null ? this.match.getType() : this.reference != null ? this.reference.getType() : null;
    }

    @Override
    public String getId()
    {
        return this.access.isOpenAccess() && this.match != null ? this.match.getId() : null;
    }

    @Override
    public String getName()
    {
        return this.access.isOpenAccess() && this.match != null ? this.match.getName() : null;
    }

    @Override
    public FeatureMetadatum getReference()
    {
        // This is what we started with, so it should be returned no matter the access type
        return this.reference;
    }

    @Override
    public double getScore()
    {
        if (this.match == null || this.reference == null || StringUtils.isEmpty(this.match.getId())
            || StringUtils.isEmpty(this.reference.getId())) {
            return Double.NaN;
        }
        ComponentManager cm = ComponentManagerRegistry.getContextComponentManager();
        FeatureMetadatumSimilarityScorer scorer = null;
        try {
            scorer = cm.getInstance(FeatureMetadatumSimilarityScorer.class, this.match.getType());
        } catch (ComponentLookupException ex) {
            try {
                scorer = cm.getInstance(FeatureMetadatumSimilarityScorer.class);
            } catch (ComponentLookupException e) {
                // This should not happen
                return Double.NaN;
            }
        }
        return scorer.getScore(this.match, this.reference);
    }

    @Override
    public JSONObject toJSON()
    {
        if (!this.access.isOpenAccess() || this.match == null && this.reference == null) {
            return new JSONObject(true);
        }
        JSONObject result = new JSONObject();

        if (this.match != null) {
            result.element("id", getId());
            result.element("name", getName());
        }
        if (this.reference != null) {
            result.element("queryId", this.reference.getId());
        }
        result.element("type", getType());
        Double score = getScore();
        if (!Double.isNaN(score)) {
            result.element("score", getScore());
        }

        return result;
    }

    @Override
    public boolean isMatchingPair()
    {
        return this.match != null && this.reference != null;
    }
}
