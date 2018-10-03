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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * Provides access to the Orphanet Rare Disease Ontology (ORDO) ontology. The ontology prefix is {@code ORDO}.
 *
 * @version $Id$
 * @since 1.3
 */
@Component
@Named("ordo")
@Singleton
public class OrphanetRareDiseaseOntology extends AbstractOWLSolrVocabulary
{
    private static final String PHENOME_LABEL = "http://www.orpha.net/ORDO/Orphanet_C001";

    private static final String GENETIC_MATERIAL_LABEL = "http://www.orpha.net/ORDO/Orphanet_C010";

    private static final String HASDBXREF_LABEL = "hasDbXref";

    private static final String TERM_CATEGORY_LABEL = "term_category";

    private static final String IS_A_LABEL = "is_a";

    private static final String ON_PROPERTY_LABEL = "onProperty";

    /**
     * The pattern for prevalence values. Values are expected to be in fraction format, and may include "<" or ">" or
     * ranges (e.g. "1-9"), or single digits in the numerator.
     */
    private static final Pattern PREV_PATTERN =
        Pattern.compile("^>?<?\\s*([0-9]+)(?:[^0-9]+)?([0-9]+)?(\\s*/\\s*)([0-9\\s]+)");

    private Set<OntClass> hierarchyRoots;

    private String region = StringUtils.EMPTY;

    private boolean isIntersection;

    @Override
    public String getIdentifier()
    {
        return "ordo";
    }

    @Override
    public String getName()
    {
        return "Orphanet Rare Disease Ontology";
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
        aliases.add("ORPHA");
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
        return "ORDO";
    }

    @Override
    public String getDefaultSourceLocation()
    {
        return "https://data.bioontology.org/ontologies/ORDO/download?apikey=8ac0298d-99f4-4793-8c70-fb7d3400f279";
    }

    @Override
    public String getWebsite()
    {
        return "http://www.orpha.net/";
    }

    @Override
    public String getCitation()
    {
        return "Orphadata: Free access data from Orphanet. © INSERM 1997";
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
            // For ORDO, an intersection class only contains one or several related restrictions.
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
    private void extractNamedClassData(@Nonnull final SolrInputDocument doc, @Nonnull final OntClass ontClass,
        @Nonnull final OntClass parent)
    {
        // Note: in ORDO, a subclass cannot have parents from different top categories (e.g. phenome and geography).
        if (!this.hierarchyRoots.contains(parent) && !hasHierarchyRootAsParent(parent, DIRECT)) {
            // This will not be null, since only anonymous classes have no local name. This check is performed in
            // the calling method (extractClassData).
            final String ordoId = getFormattedOntClassId(parent.getLocalName());

            // All parents are added to "term_category".
            addMultivaluedField(doc, TERM_CATEGORY_LABEL, ordoId);

            // If parent is a direct super-class to ontClass, then want to also add the parent to the "is_a" category.
            if (ontClass.hasSuperClass(parent, DIRECT)) {
                addMultivaluedField(doc, IS_A_LABEL, ordoId);
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
    private void extractIntersectionData(@Nonnull final SolrInputDocument doc, @Nonnull final OntClass ontClass,
        @Nonnull final OntClass parent)
    {
        this.isIntersection = true;
        final IntersectionClass intersection = parent.asIntersectionClass();
        final ExtendedIterator<? extends OntClass> operands = intersection.listOperands();

        while (operands.hasNext()) {
            final OntClass operand = operands.next();
            // For ORDO, there should only be restrictions in intersection classes.
            extractClassData(doc, ontClass, operand);
        }
        this.region = StringUtils.EMPTY;
        this.isIntersection = false;
        operands.close();
    }

    /**
     * Extracts data from the parent of ontClass that is a {@link Restriction} and updates the{@link SolrInputDocument}
     * for ontClass.
     *
     * @param doc the Solr input document
     * @param parent the parent class that contains restriction data for the ontologyClass
     */
    private void extractRestrictionData(@Nonnull final SolrInputDocument doc, @Nonnull final OntClass parent)
    {
        final Restriction restriction = parent.asRestriction();

        // Restrictions can be someValuesFrom, hasValue, allValuesFrom, etc. ORDO appears to only use the first two.
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
            if ("present_in".equals(fieldName)) {
                this.region = fieldValue;
                addMultivaluedField(doc, fieldName, fieldValue);
                return;
            }
            if (!this.isIntersection) {
                addMultivaluedField(doc, fieldName, fieldValue);
            } else {
                writeWorldwideDataFromRestriction(doc, fieldName, fieldValue);
            }
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
        // Not all of these have pretty names. Re-map these via managed-schema.xml field configurations.
        final String fieldName = getOnPropertyFromRestriction(restriction);
        final String fieldValue = restriction.asHasValueRestriction().getHasValue().asLiteral().getLexicalForm();
        if (StringUtils.isNotBlank(fieldName) && StringUtils.isNotBlank(fieldValue)) {
            if (!this.isIntersection) {
                addMultivaluedField(doc, fieldName, fieldValue);
            } else {
                writeWorldwideDataFromRestriction(doc, fieldName, fieldValue);
            }
        } else {
            this.logger.warn("Could not extract data from hasValue restriction {}, onProperty {}, in class {}",
                restriction.getId(), fieldName, doc.getFieldValue(ID_FIELD_NAME));
        }
    }

    /**
     * A workaround to obtain the label for the onProperty field for a {@link Restriction}. Ideally, this should be done
     * by using the {@link Restriction#onProperty(Property)}, however for ORDO, the stored node cannot be converted into
     * an OntProperty class.
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
        for (final OntClass hierarchyRoot : this.hierarchyRoots) {
            if (ontClass.hasSuperClass(hierarchyRoot, level)) {
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
        // If the node is not a literal, will throw a {@link LiteralRequiredException}. For ORDO, this is always a
        // literal.
        if (object.isLiteral()) {
            final String externalRef = object.asLiteral().getLexicalForm();
            final String ontology = StringUtils.substringBefore(externalRef, SEPARATOR);
            final String externalId = StringUtils.substringAfter(externalRef, SEPARATOR);
            addMultivaluedField(doc, ontology.toLowerCase() + "_id", externalId);
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
            addMultivaluedField(doc, fieldName, fieldValue);
        }
    }

    /**
     * Adds field name and multi-valued field value to the {@link SolrInputDocument} iff this value isn't already stored
     * in given field.
     *
     * @param doc the Solr input document
     * @param fieldName the name of the field to be added
     * @param fieldValue the value of the field being added
     */
    private void addMultivaluedField(@Nonnull final SolrInputDocument doc, @Nonnull final String fieldName,
        @Nonnull final Object fieldValue)
    {
        if (!Optional.fromNullable(doc.getFieldValues(fieldName)).or(Collections.emptyList()).contains(fieldValue)) {
            doc.addField(fieldName, fieldValue);
        }
    }

    /**
     * Adds field name and single-valued value to the {@link SolrInputDocument} iff the value isn't already stored in
     * the given field.
     *
     * @param doc the Solr input document
     * @param fieldName the name of the field to be added
     * @param fieldValue the value of the field being added
     */
    private void addSingleValuedField(@Nonnull final SolrInputDocument doc, @Nonnull final String fieldName,
        @Nonnull final Object fieldValue)
    {
        if (!fieldValue.equals(doc.getFieldValue(fieldName))) {
            doc.addField(fieldName, fieldValue);
        }
    }

    /**
     * Writes {@code fieldName} and {@code fieldValue} to {@code doc} iff this is worldwide data.
     *
     * @param doc the {@link SolrInputDocument} being modified
     * @param fieldName the name of the field to be added
     * @param fieldValue the value of the field to be added
     */
    private void writeWorldwideDataFromRestriction(@Nonnull final SolrInputDocument doc,
        @Nonnull final String fieldName, @Nonnull final String fieldValue)
    {
        if ("Worldwide".equals(this.region)) {
            if ("has_point_prevalence_range".equals(fieldName)
                || "has_birth_prevalence_range".equals(fieldName)
                || "has_lifetime_prevalence_range".equals(fieldName)) {
                addSingleValuedField(doc, fieldName + "_numeric", getNumericPrevalenceValue(fieldValue));
            }
        }

        final String regionInfo = StringUtils.isNotBlank(this.region) ? " (" + this.region + ")" : StringUtils.EMPTY;
        // Also add as is, with region data included.
        addMultivaluedField(doc, fieldName, fieldValue + regionInfo);
    }

    /**
     * Calculates numeric prevalence data based on a {@code fieldValue prevalence value} string.
     *
     * @param fieldValue the string containing the prevalence data range
     * @return the calculated prevalence, as double, -1 if an error occurred
     */
    private double getNumericPrevalenceValue(@Nonnull final String fieldValue)
    {
        try {
            final Matcher matcher = PREV_PATTERN.matcher(fieldValue);
            if (matcher.find()) {
                final double numerator = getNumerator(matcher.group(1), matcher.group(2));
                final double denominator = getDenominator(matcher.group(4));
                return numerator / denominator;
            }
        } catch (final Exception ex) {
            // Do nothing.
            this.logger.error("Regex matching failed: [{}]", ex.getMessage());
        }
        this.logger.error("The provided prevalence value: [{}] did not match the expected pattern.", fieldValue);
        return -1;
    }

    /**
     * Retrieves the denominator from {@code rawDenominatorStr} string, as double.
     *
     * @param rawDenominatorStr the denominator, as string
     * @return the denominator, as double
     */
    private double getDenominator(@Nonnull final String rawDenominatorStr)
    {
        final String denominatorStr = rawDenominatorStr.replaceAll("\\s*", StringUtils.EMPTY);
        return Double.parseDouble(denominatorStr);
    }

    /**
     * Calculates the numerator from {@code firstVal} and {@code secondVal}, expressed as strings.
     *
     * @param firstVal the first value in the numerator range
     * @param secondVal the second value in the numerator range; can be null if the numerator is not a range
     * @return the calculated numerator, as double
     */
    private double getNumerator(@Nonnull final String firstVal, @Nullable final String secondVal)
    {
        final double firstNum = Double.parseDouble(firstVal);
        if (StringUtils.isBlank(secondVal)) {
            return firstNum;
        }
        final double secondNum = Double.parseDouble(secondVal);
        return (firstNum + secondNum) / 2;
    }

    @Override
    void writeProperty(@Nonnull final SolrInputDocument doc, @Nonnull final String relation,
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
        return StringUtils.isNotBlank(localName) ? localName.replace("Orphanet_", "ORDO:") : null;
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
        final List<SolrDocument> searchResults = search(addDynamicQueryParam(input, maxResults, sort, customFilter,
            query));
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
     *
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
        query.set(DisMaxParams.PF, "name^20 nameSpell^36 nameExact^100 namePrefix^30 "
            + "synonym^15 synonymSpell^25 synonymExact^70 synonymPrefix^20 "
            + "def^7 defSpell^14 text^3 textSpell^5");
        query.set(DisMaxParams.QF, "id^100 name^10 nameSpell^18 nameStub^5 "
            + "synonym^6 synonymSpell^10 synonymStub^4 "
            + "def^3 defSpell^5 text^1 textSpell^2 textStub^0.5");
        return query;
    }
}
