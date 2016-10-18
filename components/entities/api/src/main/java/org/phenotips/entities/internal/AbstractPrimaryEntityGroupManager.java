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

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.PrimaryEntityGroupManager;
import org.phenotips.entities.PrimaryEntityManager;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.QueryManager;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Base class for implementing specific entity groups, where members declare the groups that they belong to. The class
 * deals with logical group operations and not with implementation.
 * <p>
 *
 * @param <G> the type of the group containing the entities
 * @param <E> the type of entities belonging to this group; if more than one type of entities can be part of the group,
 *            then a generic {@code PrimaryEntity} should be used instead
 * @version $Id$
 * @since 1.3M2
 */
public abstract class AbstractPrimaryEntityGroupManager<G extends PrimaryEntity, E extends PrimaryEntity>
    implements PrimaryEntityGroupManager<G, E>
{
    /** Entity type of group object. */
    protected EntityReference groupEntityReference;

    /** Manager of group primary entities. */
    protected final PrimaryEntityManager<G> groupManager;

    /** Entity type of member object. */
    protected EntityReference memberEntityReference;

    /** Manager of member primary entities. */
    protected final PrimaryEntityManager<E> membersManager;

    /** Logging helper object. */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @SuppressWarnings("unchecked")
    protected AbstractPrimaryEntityGroupManager(
        EntityReference groupEntityReference, EntityReference memberEntityReference)
    {
        this.groupEntityReference = groupEntityReference;
        this.memberEntityReference = memberEntityReference;

        this.membersManager = (PrimaryEntityManager<E>) getManager(memberEntityReference);
        this.groupManager = (PrimaryEntityManager<G>) getManager(groupEntityReference);
    }

    private PrimaryEntityManager<PrimaryEntity> getManager(EntityReference entityReference)
    {
        PrimaryEntityManager<PrimaryEntity> manager = null;
        if (memberEntityReference != null) {
            ComponentManager cm = ComponentManagerRegistry.getContextComponentManager();
            String[] possibleRoles = new String[3];
            possibleRoles[0] = getLocalSerializer().serialize(entityReference);
            possibleRoles[1] = entityReference.getName();
            possibleRoles[2] = StringUtils.removeEnd(entityReference.getName(), "Class");
            for (String role : possibleRoles) {
                try {
                    manager = cm.getInstance(PrimaryEntityManager.class, role);
                    if (manager != null) {
                        break;
                    }
                } catch (ComponentLookupException ex) {
                    // TODO Use a generic manager that can work with any type of group
                }
            }
        }
        if (manager == null) {
            this.logger.info("No suitable primary entity manager found for entities of type [{}] available;"
                + " certain group operations will fail", entityReference);
        }
        return manager;
    }

    @Override
    public Collection<E> getMembers(G group)
    {
        return getMembersOfType(group, memberEntityReference);
    }

    @Override
    public boolean addAllMembersById(G group, Collection<String> memberIds)
    {
        Collection<E> members = new ArrayList<>(memberIds.size());
        for (String id : memberIds) {
            E member = this.membersManager.get(id);
            if (member != null) {
                members.add(member);
            }
        }
        return addAllMembers(group, members);
    }

    @Override
    public boolean addAllMembers(G group, Collection<E> members)
    {
        boolean success = true;
        for (E member : members) {
            if (!this.addMember(group, member)) {
                success = false;
            }
        }
        return success;
    }

    @Override
    public boolean removeFromAllGroups(E member)
    {
        boolean success = true;
        Collection<G> groups = this.getGroupsForMember(member);
        for (G group : groups) {
            if (!this.removeMember(group, member)) {
                success = false;
            }
        }
        return success;
    }

    @Override
    public boolean addToAllGroups(E member, Collection<G> groups)
    {
        boolean success = true;
        for (G group : groups) {
            if (!this.addMember(group,  member)) {
                success = false;
            }
        }
        return success;
    }

    @Override
    public boolean removeAllMembers(G group)
    {
        boolean success = true;
        Collection<E> existingMembers = this.getMembers(group);
        for (E member : existingMembers) {
            if (!this.removeMember(group, member)) {
                success = false;
            }
        }
        return success;
    }

    protected QueryManager getQueryManager()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager().getInstance(QueryManager.class);
        } catch (ComponentLookupException ex) {
            this.logger.error("Failed to access the query manager: {}", ex.getMessage(), ex);
        }
        return null;
    }

    protected EntityReferenceSerializer<String> getLocalSerializer()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager()
                .getInstance(EntityReferenceSerializer.TYPE_STRING, "local");
        } catch (Exception ex) {
            this.logger.error("Unexpected exception while getting the local reference serializer: {}", ex.getMessage());
        }
        return null;
    }

    protected EntityReferenceSerializer<String> getFullSerializer()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager()
                .getInstance(EntityReferenceSerializer.TYPE_STRING);
        } catch (Exception ex) {
            this.logger.error("Unexpected exception while getting the full reference serializer: {}", ex.getMessage());
        }
        return null;
    }

    protected XWikiContext getXContext()
    {
        try {
            Provider<XWikiContext> xcontextProvider =
                ComponentManagerRegistry.getContextComponentManager().getInstance(XWikiContext.TYPE_PROVIDER);
            return xcontextProvider.get();
        } catch (Exception ex) {
            this.logger.error("Unexpected exception while getting the current context: {}", ex.getMessage());
        }
        return null;
    }

    protected DocumentAccessBridge getDataAccessBridge()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager().getInstance(DocumentAccessBridge.class);
        } catch (Exception ex) {
            this.logger.error("Unexpected exception while getting the data access bridge: {}", ex.getMessage());
        }
        return null;
    }

    protected XWikiDocument getXWikiDocument(PrimaryEntity p) throws Exception
    {
        DocumentAccessBridge dab = getDataAccessBridge();
        // use getDocument()
        XWikiDocument doc = (XWikiDocument) dab.getDocument(p.getDocumentReference());
        return doc;
    }
}
