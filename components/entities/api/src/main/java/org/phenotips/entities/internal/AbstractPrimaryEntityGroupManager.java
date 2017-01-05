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
package org.phenotips.entities.internal;

import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.PrimaryEntityGroup;
import org.phenotips.entities.PrimaryEntityGroupManager;

import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.stability.Unstable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Base class for implementing specific entity group managers. This can be used as an almost complete base
 * implementation, since the only unimplemented method is {@link #getDataSpace()}, however, several requirements are
 * imposed on the subclasses to fully work:
 * <ul>
 * <li>the concrete implementation must have its {@code @Named} annotation set to reference the XClass used for the
 * primary entity group, the one that's also returned by the {@link PrimaryEntity#getType()}</li>
 * <li>the {@code <G>} parameter must be set to the concrete class being managed</li>
 * <li>the class used for {@code <G>} must have a constructor that takes as an argument a
 * {@link org.xwiki.bridge.DocumentModelBridge} or a {@link com.xpn.xwiki.doc.XWikiDocument} argument</li>
 * <li>all documents {@link #create() created} by this manager will have the name in the format
 * {@code <PREFIX><7 digit sequential number>}, unless {@link #getNextDocument()} is overridden, where:
 * <ul>
 * <li>the prefix is computed from the uppercase letters of the XClass name, excluding {@code Class}, e.g. for
 * {@code PhenoTips.FamilyGroupClass} the prefix will be {@code FG}; override {@link #getIdPrefix()} to change this
 * behavior</li>
 * <li>the number is a 0-padded 7 digit number, starting at {@code 0000001} and automatically incremented for each new
 * entity created</li>
 * </ul>
 * </li>
 * </ul>
 *
 * @param <G> the type of groups handled by this manager
 * @param <E> the type of entities belonging to the groups handled by this manager; if more than one type of entities
 *            can be part of the groups, then a generic {@code PrimaryEntity} should be used instead
 * @version $Id$
 * @since 1.3M2
 */
@Unstable("New class and interface added in 1.3")
public abstract class AbstractPrimaryEntityGroupManager<G extends PrimaryEntityGroup<E>, E extends PrimaryEntity>
    extends AbstractPrimaryEntityManager<G> implements PrimaryEntityGroupManager<G, E>
{
    @Override
    public Collection<G> getGroupsForEntity(PrimaryEntity entity)
    {
        try {
            // FIXME GROUP_MEMBERSHIP_CLASS should be replaced with a static method call
            Query q = this.qm.createQuery(
                "select gdoc.fullName from Document edoc, edoc.object("
                    + this.localSerializer.serialize(PrimaryEntityGroup.GROUP_MEMBERSHIP_CLASS)
                    + ") as binding, Document gdoc, gdoc.object("
                    + this.localSerializer.serialize(getEntityXClassReference())
                    + ") as grp where gdoc.space = :gspace and binding.reference = concat('"
                    + this.xcontextProvider.get().getWikiId()
                    + ":', gdoc.fullName) and edoc.fullName = :name order by gdoc.fullName asc",
                Query.XWQL);
            q.bindValue("gspace", this.getDataSpace().getName());
            q.bindValue("name", this.localSerializer.serialize(entity.getDocumentReference()));
            List<String> docNames = q.execute();
            Collection<G> result = new ArrayList<>(docNames.size());
            for (String docName : docNames) {
                result.add(get(docName));
            }
            return result;
        } catch (QueryException ex) {
            this.logger.warn("Failed to query all entities of type [{}]: {}", getEntityXClassReference(),
                ex.getMessage());
        }
        return Collections.emptyList();
    }
}
