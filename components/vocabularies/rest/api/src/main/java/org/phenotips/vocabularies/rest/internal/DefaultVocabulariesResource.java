package org.phenotips.vocabularies.rest.internal;
import org.phenotips.vocabularies.rest.VocabulariesResource;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;

/**
 * Default implementation for {@link VocabulariesResource} using XWiki's support for REST resources.
 * @version
 * @since
 */
@Component
@Named("org.phenotips.vocabularies.rest.internal.DefaultVocabulariesResource")
@Singleton
public class DefaultVocabulariesResource implements VocabulariesResource
{
    @Inject
    private VocabularyManager vm;

    @Override public Response resolveTerm(String termId)
    {
        if(StringUtils.isEmpty(termId)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        VocabularyTerm term = this.vm.resolveTerm(termId);
        if (term == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(term.toJSON(), MediaType.APPLICATION_JSON_TYPE).build();
    }

    @Override public Response getAllVocabularies()
    {
        return null;
    }
}
