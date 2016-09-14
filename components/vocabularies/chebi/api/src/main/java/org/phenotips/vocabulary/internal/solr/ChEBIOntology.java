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
 * Provides access to the Chemical Entities of Biological Interest Ontology (ChEBI). The ontology prefix is
 * {@code CHEBI}.
 *
 * @version $Id$
 * @since 1.0M8
 */
@Component
@Named("chebi")
@Singleton
public class ChEBIOntology extends AbstractOBOSolrVocabulary
{
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

    @Override
    public String getWebsite() {
        return "https://www.ebi.ac.uk/chebi/";
    }

    @Override
    public String getCitation() {
        return "The ChEBI reference database and ontology for biologically relevant chemistry: enhancements for"
                + " 2013. Hastings, J., de Matos, P., Dekker, A., Ennis, M., Harsha, B., Kale, N., Muthukrishnan, V.,"
                + " Owen, G., Turner, S., Williams, M., and Steinbeck, C. (2013). Nucleic Acids Res.";
    }
}
