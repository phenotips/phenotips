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

import java.util.Collection;

import org.json.JSONObject;

/**
 * Information about a specific gene of interest recorded for a {@link Patient patient}.
 *
 * @version $Id$
 * @since 1.4
 */
@Unstable
public interface Gene extends VocabularyProperty
{
    /** The Gene XClass reference. */
    EntityReference GENE_CLASS = new EntityReference("GeneClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    /**
     * The Ensembl ID of this gene.
     *
     * @return Ensembl identifier in the format {@code ENSG00123456789}, or {@code null} if the gene doesn't have an
     *         assigned Ensembl ID
     */
    @Override
    String getId();

    /**
     * The HGNC symbol of this gene.
     *
     * @return symbol in the format {@code BRCA1}, or {@code null} if the gene doesn't have an HGNC official symbol
     */
    @Override
    String getName();

    /**
     * The status of this gene for the described patient.
     *
     * @return one of the values listed in the {@code status} property of {@code PhenoTips.GeneClass}
     */
    String getStatus();

    /**
     * The strategies used for investigating the gene status.
     *
     * @return a collection of short labels, such as {@code sequencing}, or an empty collection if unknown
     */
    Collection<String> getStrategy();

    /**
     * A short comment about the gene.
     *
     * @return a short text, or blank or {@code null} if no comment was recorded
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
