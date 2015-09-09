package org.phenotips.vocabularies.rest.internal;

import org.phenotips.vocabularies.rest.DomainObjectFactory;
import org.phenotips.vocabularies.rest.Relations;
import org.phenotips.vocabularies.rest.VocabularyResource;
import org.phenotips.vocabularies.rest.VocabularyTermResource;
import org.phenotips.vocabularies.rest.VocabularyTermsResource;
import org.phenotips.vocabularies.rest.model.Link;
import org.phenotips.vocabularies.rest.model.LinkCollection;
import org.phenotips.vocabularies.rest.model.VocabularyTermRep;
import org.phenotips.vocabularies.rest.model.VocabularyTermsRep;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;
import org.xwiki.stability.Unstable;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;

/**
 * Default implementation of the {@link org.phenotips.vocabularies.rest.VocabularyTermsResource}
 * @version
 * @since
 */
@Component
@Named("org.phenotips.vocabularies.rest.internal.DefaultVocabularyTermsResource")
@Singleton
@Unstable
public class DefaultVocabularyTermsResource extends XWikiResource implements VocabularyTermsResource
{

    @Inject
    private VocabularyManager vm;

    @Inject
    private DomainObjectFactory objectFactory;

    @Override
    public VocabularyTermsRep suggest(String vocabularyId, String input, @DefaultValue("10") int maxResults, String sort,
        String customFilter)
    {
        if (StringUtils.isEmpty(input) || StringUtils.isEmpty(vocabularyId)) {
            return null;
        }
        Vocabulary vocabulary = this.vm.getVocabulary(vocabularyId);
        if (vocabulary == null){ return null;}

        List<VocabularyTerm> termSuggestions = vocabulary.search(input, maxResults, sort, customFilter);

        List<VocabularyTermRep> termReps = new ArrayList<>();
        for (VocabularyTerm term : termSuggestions) {
            VocabularyTermRep termRep = objectFactory.createVocabularyTermRepresentation(term);
            List<Link> links = new ArrayList<>();
            links.add(new Link().withRel(Relations.VOCABULARY_TERM)
                .withHref(UriBuilder.fromUri(this.uriInfo.getBaseUri()).path(VocabularyTermResource.class)
                    .path(VocabularyTermResource.class, "getTerm")
                    .build(vocabularyId, term.getId())
                    .toString())
            );
            links.add(new Link().withRel(Relations.VOCABULARY)
                .withHref(UriBuilder.fromUri(this.uriInfo.getBaseUri()).path(VocabularyResource.class)
                    .build(vocabularyId)
                    .toString())
            );
            termRep.withLinks(links);
            termReps.add(termRep);
        }

        return this.objectFactory.createVocabularyTermsRepresentation(termReps);
    }
}
