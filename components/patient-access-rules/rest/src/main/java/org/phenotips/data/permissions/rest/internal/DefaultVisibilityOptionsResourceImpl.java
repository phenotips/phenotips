/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.phenotips.data.permissions.rest.internal;

import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.data.permissions.rest.DomainObjectFactory;
import org.phenotips.data.permissions.rest.Relations;
import org.phenotips.data.permissions.rest.VisibilityOptionsResource;
import org.phenotips.data.rest.model.Link;
import org.phenotips.data.rest.model.VisibilityOptionsRepresentation;
import org.phenotips.data.rest.model.VisibilityRepresentation;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Default implementation for {@link VisibilityOptionsResource} using XWiki's support for REST resources.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Component
@Named("org.phenotips.data.permissions.rest.internal.DefaultVisibilityOptionsResourceImpl")
@Singleton
public class DefaultVisibilityOptionsResourceImpl extends XWikiResource implements VisibilityOptionsResource
{
    @Inject
    private PermissionsManager manager;

    @Inject
    private DomainObjectFactory factory;

    @Override
    public VisibilityOptionsRepresentation getVisibilityOptions()
    {
        // todo. should this be world-visible?

        List<VisibilityRepresentation> visibilities = new LinkedList<>();
        for (Visibility visibility : this.manager.listVisibilityOptions()) {
            visibilities.add(this.factory.createVisibilityRepresentation(visibility));
        }
        VisibilityOptionsRepresentation result = (new VisibilityOptionsRepresentation()).withVisibilities(visibilities);
        result.getLinks().add(new Link().withRel(Relations.SELF).withHref(this.uriInfo.getRequestUri().toString()));
        return result;
    }
}
