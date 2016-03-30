package org.phenotips.data.permissions.rest.internal.utils;

import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.rest.internal.utils.annotations.Relation;
import org.phenotips.data.rest.model.Link;
import org.xwiki.component.annotation.Component;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.UriInfo;
import java.util.Set;

/**
 * Created by matthew on 2016-03-03.
 */
@Component
@Singleton
public class DefaultRESTLinkFactory implements RESTLinkFactory {

    @Inject
    private RESTActionResolver actionResolver;

    @Override
    public Link getActionableLinkToSelf(Class self, String patientID, AccessLevel accessLevel, UriInfo uriInfo) {
        Link link = new Link();

        link.withHref(uriInfo.getRequestUri().toString());
        link.withRel("self");
        link.withAllowedMethods(this.getAllowedMethods(self.getInterfaces()[0], accessLevel));
        return link;
    }

    @Override
    public Link getActionableLink(Class restInterface, String patientID, AccessLevel accessLevel, UriInfo uriInfo) {
        Link link = new Link();

        link.withHref(this.getPath(uriInfo, restInterface, patientID));
        link.withRel(this.getRel(restInterface));
        link.withAllowedMethods(this.getAllowedMethods(restInterface, accessLevel));

        return link;
    }

    private Set<String> getAllowedMethods(Class restInterface, AccessLevel accessLevel) {
        return actionResolver.resolveActions(restInterface, accessLevel);
    }

    private String getRel(Class restInterface) {
        String relation = null;
        Relation relationAnnotation = (Relation) restInterface.getAnnotation(Relation.class);
        if (relationAnnotation != null) {
            relation = relationAnnotation.value();
        }
        return relation;
    }

    private String getPath(UriInfo uriInfo, Class restInterface, String... params) {
        return uriInfo.getBaseUriBuilder().path(restInterface).build(params).toString();
    }
}
