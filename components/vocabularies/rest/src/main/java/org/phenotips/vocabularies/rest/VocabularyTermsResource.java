package org.phenotips.vocabularies.rest;

import org.phenotips.vocabularies.rest.model.VocabularyTerms;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

/**
 * Resource for working with lists of {@link org.phenotips.vocabulary.VocabularyTerm}
 * @version
 * @since
 */
@Path("/vocabularies/terms/{vocabulary}/search")
public interface VocabularyTermsResource
{
    @GET VocabularyTerms suggest(@PathParam("vocabulary") String vocabularyId,
        @QueryParam("input") String input,
        @QueryParam("maxResults") @DefaultValue("10") int maxResults,
        @QueryParam("sort") String sort,
        @QueryParam("customFilter") String customFilter);
}
