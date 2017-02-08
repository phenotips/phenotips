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
package org.phenotips.vocabulary.translation;

import org.phenotips.vocabulary.SolrVocabularyResourceManager;
import org.phenotips.vocabulary.VocabularyExtension;

import org.xwiki.component.phase.Initializable;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.schema.AnalyzerDefinition;
import org.apache.solr.client.solrj.request.schema.FieldTypeDefinition;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.schema.SchemaResponse.UpdateResponse;

/**
 * Implements {@link VocabularyExtension} to provide translation services for Solr-based vocabularies. Works with XLIFF
 * translations similar to the files up at {@link https://github.com/Human-Phenotype-Ontology/HPO-translations}. This
 * base class adds a new field type to the Solr schema for indexing and storing text in {@link #getTargetLocale() the
 * target language}, as a {@code TextField} with basic tokenization and {@link #getAnalyzerType() an analyzer specific
 * to the target language}, and sets up translations of the {@code name}, {@code def} and {@code synonym} fields. If a
 * more complex field definition is required, the {@link #addFieldType()} method should be overridden. If other fields
 * must be set up, the {@link #addFields()} or {@link #addField(String, boolean)} methods should be overridden.
 *
 * @version $Id$
 * @since 1.3
 */
public abstract class AbstractXliffTranslatedSolrVocabularyExtension extends AbstractXliffTranslatedVocabularyExtension
    implements Initializable
{
    @Inject
    protected SolrVocabularyResourceManager coreConnection;

    @Override
    public void initialize()
    {
        try {
            addFieldType();
            addFields();
        } catch (SolrServerException | IOException ex) {
            this.logger.error("Failed to add the required Solr fields for {}-{}: {}", getTargetLocale(),
                getTargetVocabularyId(), ex.getMessage(), ex);
        }
    }

    /**
     * Adds (or replaces) a type definition for the {@link #addFields() fields used by this translation}.
     *
     * @throws SolrServerException if communicating with the Solr server failed
     * @throws IOException if communicating with the Solr server failed
     */
    protected void addFieldType() throws SolrServerException, IOException
    {
        FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();

        Map<String, Object> fieldTypeAttributes = new LinkedHashMap<>();
        String name = "text_general_" + getTargetLocale().toString();
        fieldTypeAttributes.put("name", name);
        fieldTypeAttributes.put("class", "solr.TextField");
        fieldTypeDefinition.setAttributes(fieldTypeAttributes);

        AnalyzerDefinition analyzerDefinition = new AnalyzerDefinition();
        analyzerDefinition.setAttributes(
            Collections.<String, Object>singletonMap("class", getAnalyzerType()));
        fieldTypeDefinition.setAnalyzer(analyzerDefinition);

        try {
            // The current version (5.5) of SolrJ/EmbeddedSolrServer doesn't support getting schema information,
            // so we do this the ugly way: try to add, check for errors, try to replace
            UpdateResponse response =
                new SchemaRequest.AddFieldType(fieldTypeDefinition).process(getClient());
            if (response.getResponse().get("errors") != null) {
                response = new SchemaRequest.ReplaceFieldType(fieldTypeDefinition).process(getClient());
            }
            this.logger.debug(response.toString());
        } catch (Exception ex) {
        }
    }

    /**
     * Adds (or replaces) the fields targeted by this translation, by default {@code name}, {@code def} and
     * {@code synonym} with the {@link #getTargetLocale() targeted locale} as a suffix, for example {@code name_es}.
     *
     * @throws SolrServerException if communicating with the Solr server failed
     * @throws IOException if communicating with the Solr server failed
     */
    protected void addFields() throws SolrServerException, IOException
    {
        addField("name_" + getTargetLocale().toString(), false);
        addField("def_" + getTargetLocale().toString(), false);
        addField("synonym_" + getTargetLocale().toString(), true);
    }

    /**
     * Adds (or replaces) a field in the schema, set up to copy the values from the field with the locale suffix
     * trimmed.
     *
     * @param name the name of the field to set up
     * @param multiValued whether the field accepts multiple values or not
     * @throws SolrServerException if communicating with the Solr server failed
     * @throws IOException if communicating with the Solr server failed
     */
    protected void addField(String name, boolean multiValued)
        throws SolrServerException, IOException
    {
        Map<String, Object> fieldDefinition = new LinkedHashMap<>();
        fieldDefinition.put("name", name);
        fieldDefinition.put("type", "text_general_" + getTargetLocale().toString());
        fieldDefinition.put("indexed", true);
        fieldDefinition.put("stored", true);
        fieldDefinition.put("multiValued", multiValued);

        // Add or redefine the field
        UpdateResponse response = new SchemaRequest.AddField(fieldDefinition).process(getClient());
        if (response.getResponse().get("errors") != null) {
            response = new SchemaRequest.ReplaceField(fieldDefinition).process(getClient());
        }
    }

    /**
     * Gets a connection to the targeted core, used for sending the schema requests.
     *
     * @return a valid solr client
     */
    protected SolrClient getClient()
    {
        return this.coreConnection.getSolrConnection(getTargetVocabularyId());
    }

    /**
     * Gets the class name to be used for the custom analyzer set up for {@link #addFieldType() the field type set up
     * for this translation}.
     *
     * @return a canonical class name, such as {@code org.apache.lucene.analysis.es.SpanishAnalyzer}
     */
    protected abstract String getAnalyzerType();
}
