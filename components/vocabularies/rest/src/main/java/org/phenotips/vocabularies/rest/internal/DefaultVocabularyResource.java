package org.phenotips.vocabularies.rest.internal;

import org.phenotips.data.rest.Relations;
import org.phenotips.vocabularies.rest.DomainObjectFactory;
import org.phenotips.vocabularies.rest.VocabularyResource;
import org.phenotips.vocabularies.rest.VocabularyTermsResource;
import org.phenotips.vocabularies.rest.model.Link;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;
import org.xwiki.stability.Unstable;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang3.StringUtils;

/**
 * Default implementation of {@link VocabularyResource} using XWiki's support for REST resources.
 */
@Component
@Named("org.phenotips.vocabularies.rest.internal.DefaultVocabularyResource")
@Singleton
@Unstable
public class DefaultVocabularyResource extends XWikiResource implements VocabularyResource
{
    @Inject
    private VocabularyManager vm;

    @Inject
    private DomainObjectFactory objectFactory;

    @Override public Response reindex(String url, String vocabularyId)
    {
        if (StringUtils.isEmpty(url) || StringUtils.isEmpty(vocabularyId)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        Vocabulary vocabulary = this.vm.getVocabulary(vocabularyId);
        if (vocabulary == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        int reindexStatus = vocabulary.reindex(url);
        if (reindexStatus == 0) {
            return Response.ok().build();
        } else if (reindexStatus == 1) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @Override public org.phenotips.vocabularies.rest.model.Vocabulary getVocabulary(String vocabularyId)
    {

        Vocabulary vocabulary = this.vm.getVocabulary(vocabularyId);
        if (vocabulary == null) {
            return null;
        }
        org.phenotips.vocabularies.rest.model.Vocabulary rep = this.objectFactory.createVocabularyRepresentation(vocabulary);
        //create links
        Collection<Link> links = new ArrayList<>();
        links.add(new Link().withRel(Relations.SEARCH)
            .withHref(UriBuilder.fromUri(this.uriInfo.getBaseUri())
                .path(VocabularyTermsResource.class)
                .build(vocabularyId).toString())
            );
        rep.withLinks(links);
        return rep;
    }
}
