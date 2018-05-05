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
package org.phenotips.entities.spi;

import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.PrimaryEntityConnectionsManager;
import org.phenotips.security.authorization.AuthorizationService;

import org.xwiki.component.phase.Initializable;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Collection;
import java.util.stream.Collectors;

import javax.inject.Inject;

/**
 * Base class for implementing predicates, where the connections are stored in the Subjects, referencing the Objects,
 * and each operation checks that the user has edit rights on the affected entities.
 * <p>
 * The abstract class works as-is. In order to create a proper connection manager component all that is needed is to
 * extend this abstract class, set the right values for {@link AbstractPrimaryEntityConnectionsManager#subjectsManager}
 * and {@link AbstractPrimaryEntityConnectionsManager#objectsManager} in {@link Initializable#initialize()}, add a
 * {@code @Component} annotation and a proper {@code @Named} name following the recommended convention of
 * {@code subjectType-predicate-objectType}.
 * <p>
 * The default behavior of this base class is to store connections in the Subject XDocument, as a new XObject of type
 * {@code PhenoTips.EntityConnectionClass}, with a full reference to the Subject stored in the {@code reference}
 * XProperty.
 * </p>
 *
 * @param <S> the type of entities being the subject of the connection
 * @param <O> the type of entities being the object of the connection
 * @version $Id$
 * @since 1.4
 */
@Unstable("New SPI introduced in 1.4")
public abstract class
    AbstractSecureOutgoingPrimaryEntityConnectionsManager<S extends PrimaryEntity, O extends PrimaryEntity>
    extends AbstractOutgoingPrimaryEntityConnectionsManager<S, O> implements PrimaryEntityConnectionsManager<S, O>
{
    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private UserManager userManager;

    @Override
    public boolean connect(S subject, O object)
    {
        User user = this.userManager.getCurrentUser();
        if (!this.authorizationService.hasAccess(user, Right.EDIT, subject.getDocumentReference())
            || !this.authorizationService.hasAccess(user, Right.EDIT, object.getDocumentReference())) {
            return false;
        }
        return super.connect(subject, object);
    }

    @Override
    public boolean disconnect(S subject, O object)
    {
        User user = this.userManager.getCurrentUser();
        if (!this.authorizationService.hasAccess(user, Right.EDIT, subject.getDocumentReference())
            || !this.authorizationService.hasAccess(user, Right.EDIT, object.getDocumentReference())) {
            return false;
        }
        return super.disconnect(subject, object);
    }

    @Override
    public Collection<O> getAllConnections(S subject)
    {
        return super.getAllConnections(subject).stream()
            .filter(entity -> this.authorizationService.hasAccess(this.userManager.getCurrentUser(), Right.VIEW,
                entity.getDocumentReference()))
            .collect(Collectors.toList());
    }

    @Override
    public Collection<S> getAllReverseConnections(O object)
    {
        return super.getAllReverseConnections(object).stream()
            .filter(entity -> this.authorizationService.hasAccess(this.userManager.getCurrentUser(), Right.VIEW,
                entity.getDocumentReference()))
            .collect(Collectors.toList());
    }
}
