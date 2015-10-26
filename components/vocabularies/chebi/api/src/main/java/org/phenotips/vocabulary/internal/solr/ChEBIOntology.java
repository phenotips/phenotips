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

import org.xwiki.component.annotation.Component;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Provides access to the Chemical Entities of Biological Interest Ontology (ChEBI).
 * The ontology prefix is {@code CHEBI}.
 *
 * @version $Id$
 * @since 1.0M8
 */
@Component
@Named("chebi")
@Singleton
public class ChEBIOntology extends AbstractOBOSolrVocabulary
{
    /**
     * TODO. Determine if this is still relevant.
     * The name of the Alternative ID field, used for older aliases of updated HPO terms.
     */
    protected static final String ALTERNATIVE_ID_FIELD_NAME = "alt_id";

    protected static final String VERSION_FIELD_NAME = "version";

    /** The number of documents to be added and committed to Solr at a time. */
    @Override
    protected int getSolrDocsPerBatch()
    {
        return 3000;
    }

    @Override
    protected String getCoreName()
    {
        return getIdentifier();
    }

    @Override
    public String getDefaultSourceLocation()
    {
        return "ftp://ftp.ebi.ac.uk/pub/databases/chebi/ontology/chebi.obo";
    }

    @Override
    public String getIdentifier()
    {
        return "chebi";
    }

    @Override
    public String getName()
    {
        return "Chemical Entities of Biological Interest (ChEBI)";
    }

    @Override
    public Set<String> getAliases()
    {
        Set<String> result = new HashSet<String>();
        result.add(getIdentifier());
        result.add("CHEBI");
        result.add("ChEBI");
        return result;
    }
}
