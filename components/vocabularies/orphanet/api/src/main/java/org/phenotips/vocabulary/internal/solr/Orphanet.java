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

import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.SpellingParams;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.hp.hpl.jena.ontology.IntersectionClass;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * Provides access to the ORPHANET ontology. The ontology prefix is {@code ORPHANET}.
 *
 * @version $Id$
 * @since 1.3M6
 */
@Component
@Named("orphanet")
@Singleton
public class Orphanet extends AbstractOWLSolrVocabulary
{
    private static final String PHENOME_LABEL = "http://www.orpha.net/ORDO/Orphanet_C001";

    private static final String GENETIC_MATERIAL_LABEL = "http://www.orpha.net/ORDO/Orphanet_C010";

    private static final String HASDBXREF_LABEL = "hasDbXref";

    private static final String TERM_CATEGORY_LABEL = "term_category";

    private static final String IS_A_LABEL = "is_a";

    private static final String ON_PROPERTY_LABEL = "onProperty";

    private Set<OntClass> hierarchyRoots;

    @Override
    public String getIdentifier()
    {
        return "orphanet";
    }

    @Override
    public String getName()
    {
        return "Orphanet";
    }

    @Override
    protected String getCoreName()
    {
        return getIdentifier();
    }

    @Override
    public Set<String> getAliases()
    {
        final Set<String> aliases = new HashSet<>();
        aliases.add(getName());
        aliases.add(getIdentifier());
        aliases.add(getTermPrefix());
        return Collections.unmodifiableSet(aliases);
    }

    @Override
    int getSolrDocsPerBatch()
    {
        return 15000;
    }

    @Override
    String getBaseOntologyUri()
    {
        return "http://www.orpha.net/ontology/orphanet.owl";
    }

    @Override
    String getTermPrefix()
    {
        return "ORPHA";
    }

    @Override
    public String getDefaultSourceLocation()
    {
        return "http://data.bioontology.org/ontologies/ORDO/submissions/10/download"
            + "?apikey=8b5b7825-538d-40e0-9e9e-5ab9274a9aeb";
    }

    @Override
    public String getWebsite()
    {
        return "http://http://www.orpha.net/";
    }

    @Override
    public String getCitation()
    {
        return "Orphanet: an online database of rare diseases and orphan drugs. Copyright, INSERM 1997. "
            + "Available at http://www.orpha.net.";
    }

    @Override
    Collection<OntClass> getRootClasses(@Nonnull final OntModel ontModel)
    {
        this.hierarchyRoots = ImmutableSet.<OntClass>builder()
            .add(ontModel.getOntClass(PHENOME_LABEL))
            .add(ontModel.getOntClass(GENETIC_MATERIAL_LABEL))
            .build();

        final ImmutableSet.Builder<OntClass> selectedRoots = ImmutableSet.builder();
        for (final OntClass hierarchyRoot : this.hierarchyRoots) {
            selectedRoots.addAll(hierarchyRoot.listSubClasses(DIRECT));
        }
        return selectedRoots.build();
    }

    @Override
    void extractClassData(@Nonnull final SolrInputDocument doc, @Nonnull final OntClass ontClass,
        @Nonnull final OntClass parent)
    {
        if (parent.isRestriction()) {
            extractRestrictionData(doc, parent);
        } else if (parent.isIntersectionClass()) {
            // For Orphanet, an intersection class only contains one or several related restrictions.
            extractIntersectionData(doc, ontClass, parent);
        } else if (!parent.isAnon()) {
            // If not a restriction, nor an intersection class, then try to extract as a named class (if not anonymous).
            extractNamedClassData(doc, ontClass, parent);
        } else {
            this.logger.warn("Parent class {} of {} is an anonymous class that is neither restriction nor intersection",
                parent.getId(), ontClass.getLocalName());
        }
    }

    /**
     * Extracts hierarchy data from the parent {@link OntClass} to ontClass {@link OntClass}. Updates the
     * {@link SolrInputDocument} for ontClass.
     *
     * @param doc the Solr input document
     * @param ontClass the ontology class
     * @param parent the parent of the ontology class
     */
    private void extractNamedClassData(@Nonnull final SolrInputDocument doc,
        @Nonnull final OntClass ontClass, @Nonnull final OntClass parent)
    {
        // Note: in Orphanet, a subclass cannot have parents from different top categories (e.g. phenome and geography).
        if (!this.hierarchyRoots.contains(parent) && !hasHierarchyRootAsParent(parent, DIRECT)) {
            // This will not be null, since only anonymous classes have no local name. This check is performed in
            // the calling method (extractClassData).
            final String orphanetId = getFormattedOntClassId(parent.getLocalName());

            // All parents are added to "term_category".
            addField(doc, TERM_CATEGORY_LABEL, orphanetId);

            // If parent is a direct super-class to ontClass, then want to also add the parent to the "is_a" category.
            if (ontClass.hasSuperClass(parent, DIRECT)) {
                addField(doc, IS_A_LABEL, orphanetId);
            }
        }
    }

    /**
     * Extracts data from the parent of ontClass that is an {@link IntersectionClass}. Updates the
     * {@link SolrInputDocument} for ontClass.
     *
     * @param doc the Solr input document
     * @param parent the parent class that contains the intersection class data for the ontologyClass
     */
    private void extractIntersectionData(@Nonnull final SolrInputDocument doc,
        @Nonnull final OntClass ontClass, @Nonnull final OntClass parent)
    {
        final IntersectionClass intersection = parent.asIntersectionClass();
        final ExtendedIterator<? extends OntClass> operands = intersection.listOperands();

        while (operands.hasNext()) {
            final OntClass operand = operands.next();
            // For Orphanet, there should only be restrictions in intersection classes.
            extractClassData(doc, ontClass, operand);
        }
        operands.close();
    }

    /**
     * Extracts data from the parent of ontClass that is a {@link Restriction} and updates the{@link SolrInputDocument}
     * for ontClass.
     *
     * @param doc the Solr input document
     * @param parent the parent class that contains restriction data for the ontologyClass
     */
    private void extractRestrictionData(@Nonnull final SolrInputDocument doc,
        @Nonnull final OntClass parent)
    {
        final Restriction restriction = parent.asRestriction();

        // Restrictions can be someValuesFrom, hasValue, allValuesFrom, etc. Orphanet appears to only use the first two.
        if (restriction.isSomeValuesFromRestriction()) {
            extractSomeValuesFromRestriction(doc, restriction);
        } else if (restriction.isHasValueRestriction()) {
            extractHasValueRestriction(doc, restriction);
        } else {
            this.logger
                .warn("Restriction {} in class {} is neither someValuesFrom nor hasValue type.", restriction.getId(),
                    doc.getFieldValue(ID_FIELD_NAME));
        }
    }

    /**
     * Extracts data from the parent of ontClass that is a {@link Restriction} of type
     * {@link com.hp.hpl.jena.ontology.SomeValuesFromRestriction}. Updates the {@link SolrInputDocument} for ontClass.
     *
     * @param doc the input Solr document
     * @param restriction the restriction
     */
    private void extractSomeValuesFromRestriction(@Nonnull final SolrInputDocument doc,
        @Nonnull final Restriction restriction)
    {
        // someValuesFrom restrictions refer to the other "modifier" classes such as inheritance, geography, etc.
        // If a disease is part of a group of disorders, it will also be indicated here under a "part_of" property.
        final String fieldName = getOnPropertyFromRestriction(restriction);
        final String fieldValue = getSomeValuesFromRestriction(restriction);

        if (StringUtils.isNotBlank(fieldName) && StringUtils.isNotBlank(fieldValue)) {
            addField(doc, fieldName, fieldValue);
        } else {
            this.logger.warn("Could not extract data from someValuesFrom restriction {}, onProperty {}, in class {}",
                restriction.getId(), fieldName, doc.getFieldValue(ID_FIELD_NAME));
        }
    }

    /**
     * Extracts data from the parent of ontClass that is a {@link Restriction} of type
     * {@link com.hp.hpl.jena.ontology.HasValueRestriction}. Updates the {@link SolrInputDocument} for ontClass.
     *
     * @param doc the input Solr document
     * @param restriction the restriction
     */
    private void extractHasValueRestriction(@Nonnull final SolrInputDocument doc,
        @Nonnull final Restriction restriction)
    {
        // Not all of these have pretty names. Re-map these via schema.xml field configurations.
        final String fieldName = getOnPropertyFromRestriction(restriction);
        final String fieldValue = restriction.asHasValueRestriction().getHasValue().asLiteral().getLexicalForm();
        if (StringUtils.isNotBlank(fieldName) && StringUtils.isNotBlank(fieldValue)) {
            addField(doc, fieldName, fieldValue);
        } else {
            this.logger.warn("Could not extract data from hasValue restriction {}, onProperty {}, in class {}",
                restriction.getId(), fieldName, doc.getFieldValue(ID_FIELD_NAME));
        }
    }

    /**
     * A workaround to obtain the label for the onProperty field for a {@link Restriction}. Ideally, this should be done
     * by using the {@link Restriction#onProperty(Property)}, however for Orphanet, the stored node cannot be converted
     * into an OntProperty class.
     *
     * @param restriction the restriction being examined
     * @return the onProperty label as a string
     */
    private String getOnPropertyFromRestriction(@Nonnull final Restriction restriction)
    {
        final ExtendedIterator<Statement> statements = restriction.listProperties();
        while (statements.hasNext()) {
            final Statement statement = statements.next();
            // Workaround for getting the property label.
            if (ON_PROPERTY_LABEL.equals(statement.getPredicate().getLocalName())) {
                final String onPropertyLink = statement.getObject().toString();
                return restriction.getOntModel().getOntResource(onPropertyLink).getLabel(null);
            }
        }
        statements.close();
        return null;
    }

    /**
     * Obtains the label for the {@link Restriction} of type {@link com.hp.hpl.jena.ontology.SomeValuesFromRestriction}.
     *
     * @param restriction the restriction being examined
     * @return the someValuesFrom restriction value as a string
     */
    private String getSomeValuesFromRestriction(@Nonnull final Restriction restriction)
    {
        final OntClass ontClass = restriction.asSomeValuesFromRestriction().getSomeValuesFrom().as(OntClass.class);
        return !hasHierarchyRootAsParent(ontClass, !DIRECT)
            ? ontClass.getLabel(null)
            : getFormattedOntClassId(ontClass.getLocalName());
    }

    /**
     * Returns true iff an object of {@link OntClass} has one of the hierarchy roots as a parent.
     *
     * @param ontClass the restriction class
     * @param level specifies the level to search: direct iff true, traverse entire tree otherwise
     * @return true iff the someValuesFrom restriction value should be stored as a name
     */
    private Boolean hasHierarchyRootAsParent(@Nonnull final OntClass ontClass, @Nonnull final Boolean level)
    {
        for (final OntClass hierarcyRoot : this.hierarchyRoots) {
            if (ontClass.hasSuperClass(hierarcyRoot, level)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extracts property hasDbXref from an {@link RDFNode} and adds it to the {@link SolrInputDocument}.
     *
     * @param doc the Solr input document for an {@link OntClass} of interest
     * @param object the {@link RDFNode} object contained within the {@link OntClass} of interest
     */
    private void extractDbxRef(@Nonnull final SolrInputDocument doc, @Nonnull final RDFNode object)
    {
        // If the node is not a literal, will throw a {@link LiteralRequiredException}. For Orphanet, this is always
        // a literal.
        if (object.isLiteral()) {
            final String externalRef = object.asLiteral().getLexicalForm();
            final String ontology = StringUtils.substringBefore(externalRef, SEPARATOR);
            final String externalId = StringUtils.substringAfter(externalRef, SEPARATOR);
            addField(doc, ontology.toLowerCase() + "_id", externalId);
        }
    }

    /**
     * Extracts the fieldName property from the provided {@link RDFNode}, and adds this data to the
     * {@link SolrInputDocument}.
     *
     * @param doc the Solr input document
     * @param fieldName the name of the field to be stored
     * @param object the {@link RDFNode} object
     */
    private void extractField(@Nonnull final SolrInputDocument doc, @Nonnull final String fieldName,
        @Nonnull final RDFNode object)
    {
        // If the node is not a literal, will throw a {@link LiteralRequiredException}, so need to check. Non literals
        // will be properties like Class or subClassOf. This kind of data is already added via parents.
        if (object.isLiteral()) {
            final String fieldValue = object.asLiteral().getLexicalForm();
            addField(doc, fieldName, fieldValue);
        }
    }

    /**
     * Adds field name and field value to the {@link SolrInputDocument} iff this value isn't already stored in given
     * field.
     *
     * @param doc the Solr input document
     * @param fieldName the name of the field to be added
     * @param fieldValue the value of the field being added
     */
    private void addField(@Nonnull final SolrInputDocument doc, @Nonnull final String fieldName,
        @Nonnull final String fieldValue)
    {
        if (!Optional.fromNullable(doc.getFieldValues(fieldName)).or(Collections.emptyList()).contains(fieldValue)) {
            doc.addField(fieldName, fieldValue);
        }
    }

    @Override
    void extractProperty(@Nonnull final SolrInputDocument doc, @Nonnull final String relation,
        @Nonnull final RDFNode object)
    {
        // hasDBXRef stores references to other databases (e.g. OMIM).
        if (HASDBXREF_LABEL.equals(relation)) {
            extractDbxRef(doc, object);
        } else {
            extractField(doc, relation, object);
        }
    }

    @Override
    String getFormattedOntClassId(@Nullable final String localName)
    {
        return StringUtils.isNotBlank(localName) ? localName.replace("Orphanet_", "") : null;
    }

    @Override
    public List<VocabularyTerm> search(@Nullable final String input, final int maxResults, @Nullable final String sort,
        @Nullable final String customFilter)
    {
        return StringUtils.isBlank(input)
            ? Collections.<VocabularyTerm>emptyList()
            : searchMatches(input, maxResults, sort, customFilter);
    }

    /**
     * Searches the Solr index for matches to the input string.
     *
     * @param input string to match
     * @param maxResults the maximum number of results
     * @param sort the optional sort parameter
     * @param customFilter custom filter for results
     * @return a list of matching {@link VocabularyTerm} objects; empty if no suitable matches found
     */
    private List<VocabularyTerm> searchMatches(@Nonnull final String input, final int maxResults,
        @Nullable final String sort, @Nullable final String customFilter)
    {
        final SolrQuery query = new SolrQuery();
        addGlobalQueryParam(query);
        addFieldQueryParam(query);
        final List<SolrDocument> searchResults = search(addDynamicQueryParam(input, maxResults, sort, customFilter, query));
        final List<VocabularyTerm> results = new LinkedList<>();
        for (final SolrDocument doc : searchResults) {
            results.add(new SolrVocabularyTerm(doc, this));
        }
        return Collections.unmodifiableList(results);
    }

    /**
     * Adds dynamic solr query parameters to {@code query}, based on the received {@code rawQuery raw query string},
     * {@code rows the maximum number of results to return}, {@code sort the sorting order}, and {@code customFilter a
     * custom filter}.
     *
     * @param rawQuery unprocessed query string
     * @param rows the maximum number of search items to return
     * @param sort the optional sort parameter
     * @param customFilter custom filter for the results
     * @param query a {@link SolrQuery solr query} object
     * @return the updated {@link SolrQuery solr query} object
     */
    private SolrQuery addDynamicQueryParam(@Nonnull final String rawQuery, final Integer rows,
        @Nullable final String sort, @Nullable final String customFilter, @Nonnull SolrQuery query)
    {
        final String queryString = rawQuery.trim();
        final String escapedQuery = ClientUtils.escapeQueryChars(queryString);
        if (StringUtils.isNotBlank(customFilter)) {
            query.setFilterQueries(customFilter);
        }
        query.setQuery(escapedQuery);
        query.set(SpellingParams.SPELLCHECK_Q, queryString);
        final String lastWord = StringUtils.defaultIfBlank(StringUtils.substringAfterLast(escapedQuery, " "),
            escapedQuery) + "*";
        query.set(DisMaxParams.BQ,
            String.format("nameSpell:%1$s^20 defSpell:%1$s^3 text:%1$s^1 textSpell:%1$s^2", lastWord));
        query.setRows(rows);
        if (StringUtils.isNotBlank(sort)) {
            for (final String sortItem : sort.split("\\s*,\\s*")) {
                query.addSort(StringUtils.substringBefore(sortItem, " "),
                    sortItem.endsWith(" desc") || sortItem.startsWith("-") ? ORDER.desc : ORDER.asc);
            }
        }
        return query;
    }

    /**
     * Given a {@code query} object, adds global query parameters.
     * @param query a {@link SolrQuery solr query} object
     * @return the {@code query} with global query parameters added
     */
    private SolrQuery addGlobalQueryParam(@Nonnull final SolrQuery query)
    {
        // Add global query parameters.
        query.set("spellcheck", Boolean.toString(true));
        query.set(SpellingParams.SPELLCHECK_COLLATE, Boolean.toString(true));
        query.set(SpellingParams.SPELLCHECK_COUNT, "100");
        query.set(SpellingParams.SPELLCHECK_MAX_COLLATION_TRIES, "3");
        query.set("lowercaseOperators", Boolean.toString(false));
        query.set("defType", "edismax");
        return query;
    }

    /**
     * Given a {@code query} object, adds field query parameters.
     *
     * @param query a {@link SolrQuery solr query} object
     * @return the {@code query} with field parameters added
     */
    private SolrQuery addFieldQueryParam(@Nonnull final SolrQuery query)
    {
        // Add static field parameters.
        query.set(DisMaxParams.PF, "name^40 nameSpell^70 nameExact^100 namePrefix^30 alternative_term^15 "
            + "alternative_termSpell^25 alternative_termExact^70 alternative_termPrefix^20 def^7 "
            + "defSpell^14 text^3 textSpell^5");
        query.set(DisMaxParams.QF,
            "id^100 idStub^75 name^10 nameSpell^18 nameStub^5 alternative_term^6 alternative_termSpell^10 "
                + "alternative_termStub^4 def^3 defSpell^5 text^1 textSpell^2 textStub^0.5");
        return query;
    }
}
