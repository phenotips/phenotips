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

import org.phenotips.data.permissions.rest.DomainObjectFactory;
import org.phenotips.data.permissions.rest.PrincipalsResource;
import org.phenotips.data.permissions.rest.internal.utils.EntityAccessContext;
import org.phenotips.data.permissions.rest.internal.utils.SecureContextFactory;
import org.phenotips.data.permissions.rest.model.PrincipalsRepresentation;
import org.phenotips.rest.Autolinker;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;

/**
 * Default implementation for {@link PrincipalsResource} using XWiki's support for REST resources.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("org.phenotips.data.permissions.rest.internal.DefaultPrincipalsResourceImpl")
@Singleton
public class DefaultPrincipalsResourceImpl extends XWikiResource implements PrincipalsResource
{
    @Inject
    private Logger logger;

    @Inject
    private SecureContextFactory secureContextFactory;

    @Inject
    private DomainObjectFactory factory;

    @Inject
    private Provider<Autolinker> autolinker;

    @Override
    public PrincipalsRepresentation getPrincipals(String entityType, String entityId)
    {
        this.logger.debug("Retrieving principals with access to entity record [{}] via REST", entityId);
        // Besides getting the entity, checks that the user has view access
        EntityAccessContext patientAccessContext = this.secureContextFactory.getReadContext(entityId, entityType);

        PrincipalsRepresentation result =
            this.factory.createPrincipalsRepresentation(patientAccessContext.getEntity(), entityType, this.uriInfo);

        result.withLinks(this.autolinker.get().forResource(this.getClass(), this.uriInfo)
            .withGrantedRight(patientAccessContext.getEntityAccess().getAccessLevel().getGrantedRight())
            .build());

        return result;
    }
}
