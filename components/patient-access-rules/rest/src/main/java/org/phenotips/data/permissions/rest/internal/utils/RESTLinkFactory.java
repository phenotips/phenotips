package org.phenotips.data.permissions.rest.internal.utils;

import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.rest.model.Link;
import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import javax.ws.rs.core.UriInfo;

/**
 * Created by matthew on 2016-03-03.
 */
@Unstable
@Role
public interface RESTLinkFactory {

    Link getActionableLinkToSelf(Class self, String patientID, AccessLevel accessLevel, UriInfo uriInfo);

    /**
     *
     * @param restInterface
     * @return
     */
    Link getActionableLink(Class restInterface, String patientID, AccessLevel accessLevel, UriInfo uriInfo);


}
