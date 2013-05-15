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
package edu.toronto.cs.phenotips.vcf2solr;

import edu.toronto.cs.phenotips.solr.AbstractSolrScriptService;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.xwiki.component.annotation.Component;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 * Provides access to the Solr server, with the purpose of indexing genome variants from .vcf files.
 * @version $Id$
 */
@Component
@Named("vcf")
@Singleton
public class VCFService extends AbstractSolrScriptService
{

    /**
     * Collection name.
     */
    private static final String NAME = "variant";

    @Override
    protected String getName()
    {
        return NAME;
    }

    /**
     * Index a map of variant documents to Solr.
     * @param fields a map of variant documents
     * @return {@code 0} if the indexing succeeded, {@code 1} if writing to the Solr server failed,
     */
    public int index(Map<String, TermData> fields) {

        Collection<SolrInputDocument> allTerms = new HashSet<SolrInputDocument>();
        for (Map.Entry<String, TermData> item : fields.entrySet()) {

            SolrInputDocument doc = new SolrInputDocument();
            for (Map.Entry<String, Collection<String>> property : item.getValue().entrySet()) {
                String name = property.getKey();
                for (String value : property.getValue()) {
                    doc.addField(name, value);
                }
            }
            allTerms.add(doc);
        }
        try {
            this.server.add(allTerms);
            this.server.commit();
            this.cache.removeAll();
            return 0;
        } catch (SolrServerException ex) {
            this.logger.warn("Failed to index document: {}", ex.getMessage());
        } catch (IOException ex) {
            this.logger.warn("Failed to communicate with the Solr server while indexing variant: {}", ex.getMessage());
        }
        return 1;
    }
}
