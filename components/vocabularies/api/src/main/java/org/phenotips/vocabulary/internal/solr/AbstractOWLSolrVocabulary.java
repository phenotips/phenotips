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

import org.phenotips.vocabulary.VocabularyExtension;
import org.phenotips.vocabulary.VocabularyTerm;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * Ontologies processed from OWL files share much of the processing code.
 *
 * @version $Id$
 * @since 1.3M6
 */
public abstract class AbstractOWLSolrVocabulary extends AbstractSolrVocabulary
{
    static final Boolean DIRECT = true;

    static final String SEPARATOR = ":";

    private static final String VERSION_FIELD_NAME = "version";

    private static final String TERM_GROUP_LABEL = "term_group";

    private static final String HEADER_INFO_LABEL = "HEADER_INFO";

    @Override
    public VocabularyTerm getTerm(@Nullable final String id)
    {
        return StringUtils.isNotBlank(id) ? getTerm(id, super.getTerm(id)) : null;
    }

    /**
     * Returns the result from the {@code firstAttempt first attempt} at search if not null, otherwise performs a search
     * for {@code id} without prefix (if the {@code id} has one).
     *
     * @param id the ID of the term of interest
     * @param firstAttempt the result of the first search attempt, can be null
     * @return the {@link VocabularyTerm} corresponding with the given ID, null if no such {@link VocabularyTerm} exists
     */
    private VocabularyTerm getTerm(@Nonnull final String id, @Nullable final VocabularyTerm firstAttempt)
    {
        return firstAttempt != null ? firstAttempt : searchTermWithoutPrefix(id);
    }

    /**
     * If the {@code id} stats with the optional prefix, removes the prefix and performs the search again.
     *
     * @param id the ID of the term of interest
     * @return the {@link VocabularyTerm} corresponding with the given ID, null if no such {@link VocabularyTerm} exists
     */
    private VocabularyTerm searchTermWithoutPrefix(@Nonnull final String id)
    {
        final String optPrefix = this.getTermPrefix() + SEPARATOR;
        return StringUtils.startsWith(id.toUpperCase(), optPrefix.toUpperCase())
            ? getTerm(StringUtils.substringAfter(id, SEPARATOR))
            : null;
    }

    /**
     * Delete all the data in the Solr index.
     *
     * @return {@code 0} if the command was successful, {@code 1} otherwise
     */
    protected int clear()
    {
        try {
            this.externalServicesAccess.getSolrConnection(getCoreName()).deleteByQuery("*:*");
            return 0;
        } catch (SolrServerException ex) {
            this.logger.error("SolrServerException while clearing the Solr index", ex);
        } catch (IOException ex) {
            this.logger.error("IOException while clearing the Solr index", ex);
        }
        return 1;
    }

    @Override
    public int reindex(@Nullable final String sourceUrl)
    {
        int retval;
        try {
            for (final VocabularyExtension ext : this.extensions.get()) {
                if (ext.isVocabularySupported(this)) {
                    ext.indexingStarted(this);
                }
            }
            this.clear();
            retval = this.index(sourceUrl);
        } finally {
            for (VocabularyExtension ext : this.extensions.get()) {
                if (ext.isVocabularySupported(this)) {
                    ext.indexingEnded(this);
                }
            }
        }
        return retval;
    }

    /**
     * Given a {@code sourceUrl source URL} for the vocabulary, return {@code 0} iff the vocabulary is indexed
     * successfully, {@code 1} otherwise.
     *
     * @param sourceUrl the source URL for the vocabulary, as string
     * @return {@code 0} iff the vocabulary is indexed successfully, {@code 1} otherwise
     */
    protected int index(@Nullable final String sourceUrl)
    {
        final String url = StringUtils.isNotBlank(sourceUrl) ? sourceUrl : getDefaultSourceLocation();
        // Fetch the ontology. If this is over the network, it may take a while.
        final OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_TRANS_INF);
        ontModel.read(url);
        // Get the root classes of the ontology that we can start the parsing with.
        final Collection<OntClass> roots = getRootClasses(ontModel);
        // Reusing doc for speed (see http://wiki.apache.org/lucene-java/ImproveIndexingSpeed).
        final SolrInputDocument doc = new SolrInputDocument();
        try {
            // Set the ontology model version.
            setVersion(doc, ontModel);
            // Create and add solr documents for each of the roots.
            for (final OntClass root : roots) {
                // Don't want to add Solr documents for general root categories, so start adding children.
                addChildDocs(doc, root);
            }
            commitDocs();
            return 0;
        } catch (SolrServerException ex) {
            this.logger.warn("Failed to index ontology: {}", ex.getMessage());
        } catch (IOException ex) {
            this.logger.warn("Failed to communicate with the Solr server while indexing ontology: {}", ex.getMessage());
        } catch (OutOfMemoryError ex) {
            this.logger.warn("Failed to add terms to the Solr. Ran out of memory. {}", ex.getMessage());
        }
        return 1;
    }

    /**
     * Create a document for the ontology class, and add it to the index.
     *
     * @param doc the reusable Solr input document
     * @param ontClass the ontology class that should be parsed
     * @param root the top root category for ontClass
     */
    private void addDoc(@Nonnull final SolrInputDocument doc, @Nonnull final OntClass ontClass,
        @Nonnull final OntClass root) throws IOException, SolrServerException
    {
        parseSolrDocumentFromOntClass(doc, ontClass, root);
        parseSolrDocumentFromOntParentClasses(doc, ontClass);
        this.externalServicesAccess.getSolrConnection(getCoreName()).add(doc);
        doc.clear();
    }

    /**
     * Adds any of the sub-documents of the specified ontology class.
     *
     * @param doc the reusable Solr input document
     * @param ontClass the ontology class that should be parsed
     */
    private void addChildDocs(@Nonnull final SolrInputDocument doc, @Nonnull final OntClass ontClass)
        throws IOException, SolrServerException
    {
        // Get all the subclasses of ontClass, and add a Solr document for each of them.
        final ExtendedIterator<OntClass> subClasses = ontClass.listSubClasses();
        int counter = 0;
        while (subClasses.hasNext()) {
            if (counter == getSolrDocsPerBatch()) {
                commitDocs();
                counter = 0;
            }
            final OntClass subClass = subClasses.next();
            addDoc(doc, subClass, ontClass);
            counter++;
        }
        subClasses.close();
    }

    /**
     * Commits the batch of newly-processed documents.
     */
    private void commitDocs() throws IOException, SolrServerException
    {
        this.externalServicesAccess.getSolrConnection(getCoreName()).commit();
        this.externalServicesAccess.getTermCache(getCoreName()).removeAll();
    }

    @Override
    public String getVersion()
    {
        final SolrQuery query = new SolrQuery();
        query.setQuery("version:*");
        query.set(CommonParams.ROWS, "1");
        try {
            final QueryResponse response = this.externalServicesAccess.getSolrConnection(getCoreName()).query(query);
            final SolrDocumentList termList = response.getResults();

            if (!termList.isEmpty()) {
                final SolrDocument firstDoc = termList.get(0);
                return firstDoc.getFieldValue(VERSION_FIELD_NAME).toString();
            }
        } catch (SolrServerException | SolrException | IOException ex) {
            this.logger.warn("Failed to query ontology version: {}", ex.getMessage());
        }
        return null;
    }

    /**
     * Sets the ontology version data.
     *
     * @param doc the Solr input document
     * @param ontModel the ontology model
     * @throws IOException if failed to communicate with Solr server while indexing ontology
     * @throws SolrServerException if failed to index ontology
     */
    private void setVersion(@Nonnull final SolrInputDocument doc, @Nonnull final OntModel ontModel)
        throws IOException, SolrServerException
    {
        final String version = ontModel.getOntology(getBaseOntologyUri()).getVersionInfo();
        if (StringUtils.isNotBlank(version)) {
            doc.addField(ID_FIELD_NAME, HEADER_INFO_LABEL);
            doc.addField(VERSION_FIELD_NAME, version);
            this.externalServicesAccess.getSolrConnection(getCoreName()).add(doc);
            doc.clear();
        }
    }

    /**
     * Creates a Solr document from the provided ontology class.
     *
     * @param doc Solr input document
     * @param ontClass the ontology class
     * @param root the top root category for ontClass
     */
    private void parseSolrDocumentFromOntClass(@Nonnull final SolrInputDocument doc,
        @Nonnull final OntClass ontClass, @Nonnull final OntClass root)
    {
        doc.addField(ID_FIELD_NAME, getFormattedOntClassId(ontClass.getLocalName()));
        doc.addField(TERM_GROUP_LABEL, root.getLabel(null));
        extractProperties(doc, ontClass);
    }

    /**
     * Adds parent data for provided ontology class to the Solr document.
     *
     * @param doc Solr input document
     * @param ontClass the ontology class
     */
    private void parseSolrDocumentFromOntParentClasses(@Nonnull final SolrInputDocument doc,
        @Nonnull final OntClass ontClass)
    {
        // This will list all superclasses for ontClass.
        final ExtendedIterator<OntClass> allParents = ontClass.listSuperClasses(!DIRECT);
        // For anonymous classes, we're only interested in the direct parents.
        final List<OntClass> directParents = ontClass.listSuperClasses(DIRECT).toList();
        while (allParents.hasNext()) {
            final OntClass parent = allParents.next();
            // We're interested in all non-anonymous parents (these are parent disorders), but only the direct anonymous
            // parents (these are the class properties).
            if (!parent.isAnon() || directParents.contains(parent)) {
                extractClassData(doc, ontClass, parent);
            }
        }
        allParents.close();
    }

    /**
     * Extracts properties from the ontology class, and adds the data to the Solr input document.
     *
     * @param doc the Solr input document
     * @param ontClass the ontology class
     */
    private void extractProperties(@Nonnull final SolrInputDocument doc, @Nonnull final OntClass ontClass)
    {
        final ExtendedIterator<Statement> statements = ontClass.listProperties();
        while (statements.hasNext()) {
            final Statement statement = statements.next();

            final RDFNode object = statement.getObject();
            final String relation = statement.getPredicate().getLocalName();

            writeProperty(doc, relation, object);
        }
        statements.close();
    }

    /**
     * Returns a prefix for the vocabulary term (e.g. ORPHA, HPO).
     *
     * @return the prefix for the vocabulary term, as string
     */
    abstract String getTermPrefix();

    /**
     * Extracts relevant data from the the parent class of ontClass, and writes it to the Solr input document associated
     * with ontClass.
     *
     * @param doc the Solr input document
     * @param ontClass the ontology class of interest
     * @param parent the parent of ontClass
     */
    abstract void extractClassData(@Nonnull SolrInputDocument doc,
        @Nonnull OntClass ontClass, @Nonnull OntClass parent);

    /**
     * Get a numerical id string from a localName. Assuming the localName is in the form "Orphanet_XXX". If localName is
     * an empty string or is null, will return null.
     *
     * @param localName the localName of an OWL class if localName is not null or empty, null otherwise.
     * @return the string id.
     */
    abstract String getFormattedOntClassId(@Nullable String localName);

    /**
     * Adds the property value to the Solr input document, if it is an item of interest.
     *
     * @param doc the Solr input document
     * @param relation property name
     * @param object the rdf data node
     */
    abstract void writeProperty(@Nonnull SolrInputDocument doc, @Nonnull String relation,
        @Nonnull RDFNode object);

    /**
     * Get a collection of root classes from the provided ontology model.
     *
     * @param ontModel the provided ontology model
     * @return a collection of root classes
     */
    abstract Collection<OntClass> getRootClasses(@Nonnull OntModel ontModel);

    /**
     * The number of documents to be added and committed to Solr at a time.
     *
     * @return the number of documents as an integer
     */
    abstract int getSolrDocsPerBatch();

    /**
     * Retrieves the base URI for the ontology.
     *
     * @return the base URI of the ontology, as string
     */
    abstract String getBaseOntologyUri();
}
