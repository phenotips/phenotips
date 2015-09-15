package org.phenotips.vocabularies.rest;

import org.xwiki.component.annotation.Role;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

/**
 *Resource for working with individual {@link org.phenotips.vocabulary.VocabularyTerm}
 */
@Path("/vocabularies/terms")
public interface VocabularyTermResource
{
    @GET
    @Path("/{vocabulary}/{id}")
    Response getTerm(@PathParam("vocabulary") String vocabularyId, @PathParam("id") String termId);

    @GET
    @Path("/{id}")
    Response resolveTerm(@PathParam("id") String termId);
}
