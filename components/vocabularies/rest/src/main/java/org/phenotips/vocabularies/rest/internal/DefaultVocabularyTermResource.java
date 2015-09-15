package org.phenotips.vocabularies.rest.internal;

import org.phenotips.data.rest.Relations;
import org.phenotips.vocabularies.rest.DomainObjectFactory;
import org.phenotips.vocabularies.rest.VocabularyResource;
import org.phenotips.vocabularies.rest.VocabularyTermResource;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang3.StringUtils;

import net.sf.json.JSONObject;

/**
 * Default implementation of the {@link VocabularyTermResource}
 * @version
 * @since
 */
@Component
@Named("org.phenotips.vocabularies.rest.internal.DefaultVocabularyTermResource")
@Singleton
public class DefaultVocabularyTermResource extends XWikiResource implements VocabularyTermResource
{
    @Inject
    VocabularyManager vm;

    @Inject
    DomainObjectFactory objectFactory;

    @Override public Response getTerm(String vocabularyId, String termId)
    {
        if (StringUtils.isEmpty(vocabularyId) || StringUtils.isEmpty(termId)){
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        Vocabulary vocabulary = this.vm.getVocabulary(vocabularyId);
        if (vocabulary == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        VocabularyTerm term = vocabulary.getTerm(termId);
        if( term == null){
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        JSONObject rep = this.createTermRepresentation(term);
        return Response.ok(rep, MediaType.APPLICATION_JSON_TYPE).build();
    }

    @Override public Response resolveTerm(String termId)
    {
        VocabularyTerm term = this.vm.resolveTerm(termId);
        if (term == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        JSONObject rep = this.createTermRepresentation(term);
        return Response.ok(rep, MediaType.APPLICATION_JSON_TYPE).build();
    }

    private JSONObject createTermRepresentation(VocabularyTerm term){
        JSONObject rep = JSONObject.fromObject(term.toJSON());
        //decorate with links
        JSONObject links = new JSONObject();
        links.accumulate(Relations.SELF, this.uriInfo.getRequestUri().toString());
        links.accumulate(Relations.VOCABULARY, UriBuilder.fromUri(this.uriInfo.getBaseUri())
                .path(VocabularyResource.class)
                .build(term.getVocabulary().getAliases().iterator().next())
                .toString()
        );
        rep.accumulate("links", links);
        return rep;
    }
}
