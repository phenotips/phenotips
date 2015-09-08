package org.phenotips.vocabularies.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
/**
 * Root resource for working with vocabularies.
 *
 * @version
 * @since
 */
@Path("/vocabularies")
public interface VocabulariesResource
{
    @GET
    Response getAllVocabularies();
}
