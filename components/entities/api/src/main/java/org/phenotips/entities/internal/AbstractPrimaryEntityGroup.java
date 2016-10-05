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

import org.phenotips.Constants;
import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.PrimaryEntityGroup;
import org.phenotips.entities.PrimaryEntityManager;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.stability.Unstable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Base class for implementing specific entity groups, where members declare the groups that they belong to.
 * <p>
 * By default, this uses the document name as the identifier, the document title as the name, and either a
 * {@code description} property in the main XObject, or the document content, as the description. If two objects use the
 * same document for storage, they are assumed to be equal, and no actual data equality is checked.
 * </p>
 * <p>
 * Members declare their group membership by attaching an XObject of type {@link #GROUP_MEMBERSHIP_CLASS} pointing to
 * the group document.
 * </p>
 * <p>
 * In order to function properly, a {@link PrimaryEntityManager} component with the hint ({@code @Named}) set to the
 * name of {@link #getMemberType() the XClass used for members} must exist. Shorter versions of the XClass name are also
 * accepted. For example, for members of type {@code PhenoTips.PatientClass}, the following names are supported:
 * </p>
 * <ol>
 * <li>PhenoTips.PatientClass</li>
 * <li>PatientClass</li>
 * <li>Patient</li>
 * </ol>
 *
 * @param <G> the type of the group containing the entities
 * @param <E> the type of entities belonging to this group; if more than one type of entities can be part of the group,
 *            then a generic {@code PrimaryEntity} should be used instead
 * @version $Id$
 * @since 1.3M2
 */
@Unstable("New class and interface added in 1.3")
public abstract class AbstractPrimaryEntityGroup<G extends PrimaryEntity, E extends PrimaryEntity>
    implements PrimaryEntityGroup<G, E>
{
    /** The XClass used for storing membership information by default. */
    protected static final EntityReference GROUP_MEMBERSHIP_CLASS =
        new EntityReference("EntityBindingClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    protected EntityReference groupEntityReference;

    protected EntityReference memberEntityReference;

    protected final PrimaryEntityManager<E> membersManager;

    protected final PrimaryEntityManager<G> groupManager;

    /** Logging helper object. */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @SuppressWarnings("unchecked")
    protected AbstractPrimaryEntityGroup(EntityReference groupEntityReference, EntityReference memberEntityReference)
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
    public Collection<E> getMembersOfType(G group, EntityReference type)
    {
        Collection<E> result = new LinkedList<>();
        try {
            StringBuilder hql = new StringBuilder();
            hql.append("select distinct binding.name from BaseObject binding, StringProperty groupReference");
            if (type != null) {
                hql.append(", BaseObject entity");
            }
            hql.append(" where binding.className = :memberClass")
                .append(" and groupReference.id.id = binding.id and groupReference.id.name = :referenceProperty")
                .append(" and groupReference.value = :selfReference");
            if (type != null) {
                hql.append(" and entity.name = binding.name and entity.className = :entityType");
            }

            Query q = getQueryManager().createQuery(hql.toString(), Query.HQL);

            q.bindValue("memberClass", getLocalSerializer().serialize(GROUP_MEMBERSHIP_CLASS));
            q.bindValue("referenceProperty", getMembershipProperty());
            q.bindValue("selfReference", getFullSerializer().serialize(group.getDocument()));
            if (type != null) {
                q.bindValue("entityType", getLocalSerializer().serialize(type));
            }
            List<String> memberIds = q.execute();
            for (String memberId : memberIds) {
                result.add(this.membersManager.get(memberId));
            }
        } catch (QueryException ex) {
            this.logger.warn("Failed to query members: {}", ex.getMessage());
        }
        return result;
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

    @Override
    public boolean addMember(G group, E member)
    {
        try {
            DocumentAccessBridge dab =
                ComponentManagerRegistry.getContextComponentManager().getInstance(DocumentAccessBridge.class);
            XWikiDocument doc = (XWikiDocument) dab.getDocument(member.getDocument());
            BaseObject obj = doc.getXObject(GROUP_MEMBERSHIP_CLASS, getMembershipProperty(),
                getFullSerializer().serialize(group.getDocument()), false);
            if (obj != null) {
                return true;
            }
            obj = doc.newXObject(GROUP_MEMBERSHIP_CLASS, getXContext());
            obj.setStringValue(getMembershipProperty(), getFullSerializer().serialize(group.getDocument()));
            this.setMemberParameters(member, obj);
            getXContext().getWiki().saveDocument(doc, "Added to group " + group.getDocument(), true, getXContext());
            return true;
        } catch (Exception ex) {
            this.logger.warn("Failed to add member to group: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * If an extending class wants to record parameters with a member, override this.
     *
     * @param member that is saved
     * @param obj object for recording parameters
     */
    protected void setMemberParameters(E member, BaseObject obj)
    {
        // do nothing as default.
    }

    @Override
    public boolean removeMember(G group, E member)
    {
        try {
            XWikiDocument doc = getXWikiDocument(member);
            BaseObject obj = doc.getXObject(GROUP_MEMBERSHIP_CLASS, getMembershipProperty(),
                getFullSerializer().serialize(group.getDocument()), false);
            if (obj == null) {
                return true;
            }
            doc.removeXObject(obj);
            getXContext().getWiki().saveDocument(doc, "Removed from group " + group.getDocument(), true, getXContext());
            return true;
        } catch (Exception ex) {
            this.logger.warn("Failed to remove member from group: {}", ex.getMessage());
        }
        return false;
    }

    @Override
    public Collection<G> getGroupsForMember(PrimaryEntity member)
    {
        try {
            // FIXME GROUP_MEMBERSHIP_CLASS should be replaced with a static method call
            String bindingClassName = this.getLocalSerializer().serialize(GROUP_MEMBERSHIP_CLASS);
            String xwikiId = this.getXContext().getWikiId() + ":";
            String groupClass = this.getLocalSerializer().serialize(groupEntityReference);
            Query q = this.getQueryManager().createQuery(
                ", BaseObject obj, BaseObject groupObj, StringProperty property "
                + "where obj.id.id = property.id and "
                + "property.value = concat('" + xwikiId + "', doc.fullName) and "
                + "property.id.name = 'reference' and "
                + "obj.className = '" + bindingClassName + "' and "
                + "obj.name = :name and "
                + "doc.space = :gspace and "
                + "groupObj.name = doc.fullName and "
                + "groupObj.className = '" + groupClass + "'", Query.HQL);
            q.bindValue("gspace", this.groupManager.getDataSpace().getName());
            q.bindValue("name", this.getLocalSerializer().serialize(member.getDocument()));
            List<String> docNames = q.execute();
            Collection<G> result = new ArrayList<>(docNames.size());
            for (String docName : docNames) {
                result.add(this.groupManager.get(docName));
            }
            return result;
        } catch (QueryException ex) {
            this.logger.warn("Failed to query all entities of type [{}]: {}", groupEntityReference,
                ex.getMessage());
        }
        return Collections.emptyList();
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
        XWikiDocument doc = (XWikiDocument) dab.getDocument(p.getDocument());
        return doc;
    }

    protected String getMembershipProperty()
    {
        return REFERENCE_XPROPERTY;
    }
}
