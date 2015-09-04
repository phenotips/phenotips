package org.phenotips.vocabularies.rest;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.xml.ws.Response;

/**
 * Resource for working with the omim vocabulary.
 *
 * @version
 * @since
 */
@Path("/vocabularies/omim")
public interface OmimResources
{
    @GET
    Response getTerm(String term);

    @GET
    Response search(
        @QueryParam("input") String input,
        @QueryParam("maxResults") @DefaultValue("10") Integer maxResults,
        @QueryParam("sort") @DefaultValue("id") String sort,
        @QueryParam("customFilter") @DefaultValue("") String customFilter
    );
}
