package org.phenotips.vocabularies.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * Resource for working with lists of {@link org.phenotips.vocabulary.VocabularyTerm}
 * @version
 * @since
 */
@Path("/vocabularies/{vocabulary}")
public interface VocabularyTermsResource
{
    @GET
    @Path("/suggest")
    Response suggest(@PathParam("vocabulary") String vocabulary, @Context UriInfo ui);
}
