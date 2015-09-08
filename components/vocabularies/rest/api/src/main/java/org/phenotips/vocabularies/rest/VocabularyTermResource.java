package org.phenotips.vocabularies.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

/**
 *Resource for working with individual {@link org.phenotips.vocabulary.VocabularyTerm}
 */
@Path("vocabularies")
public interface VocabularyTermResource
{
    @GET
    @Path("/{vocabulary}/{id: \\d+}")
    Response getTerm(@PathParam("vocabulary") String vocabulary, @PathParam("id") String id);

    @GET
    @Path("/{id}")
    Response resolveTerm(@PathParam("id") String id);
}
