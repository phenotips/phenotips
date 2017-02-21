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
package org.phenotips.data;

import org.phenotips.Constants;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

import org.json.JSONObject;

/**
 * Information about a specific gene recorded for a {@link Patient patient}.
 *
 * @version $Id$
 * @since 1.3M4
 */
@Unstable
public interface Gene extends VocabularyProperty
{
    /** The Gene XClass reference. */
    EntityReference GENE_CLASS = new EntityReference("GeneClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    /**
     * Return gene Ensembl ID.
     *
     * @return id
     */
    @Override
    String getId();

    /**
     * Return gene symbol.
     *
     * @return symbol
     */
    @Override
    String getName();

    /**
     * Return gene status.
     *
     * @return status
     */
    String getStatus();

    /**
     * Return gene strategy.
     *
     * @return strategy
     */
    String getStrategy();

    /**
     * Return gene comment.
     *
     * @return comment
     */
    String getComment();

    /**
     * Retrieve all information about this gene in a JSON format. For example:
     *
     * <pre>
     * {
     *   "id": "ENSG00000164458",
     *   "gene": "T",
     *   "status": "candidate",
     *   "strategy": ["sequencing","deletion","familial_mutation"],
     *   "comment": "some comments"
     * }
     * </pre>
     *
     * @return the gene data, using the org.json classes
     */
    @Override
    JSONObject toJSON();
}
