package org.phenotips.vocabularies.rest;

import org.phenotips.vocabularies.rest.model.Vocabulary;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * A resource for working with an individual {@link org.phenotips.vocabulary.Vocabulary}
 * @version $Id$
 * @since
 */
@Path("/vocabularies/{vocabulary}")
public interface VocabularyResource
{
    @PUT
    Response reindex(@QueryParam("url") String url, @PathParam("vocabulary") String vocabularyId);

    @GET Vocabulary getVocabulary(@PathParam("vocabulary") String vocabularyId);
}
