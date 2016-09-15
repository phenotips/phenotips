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
import org.phenotips.entities.PrimaryEntityGroup;
import org.phenotips.entities.PrimaryEntityManager;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.stability.Unstable;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

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
 * @param <E> the type of entities belonging to this group; if more than one type of entities can be part of the group,
 *            then a generic {@code PrimaryEntity} should be used instead
 * @version $Id$
 * @since 1.3M2
 */
@Unstable("New class and interface added in 1.3")
public abstract class AbstractPrimaryEntityGroup<E extends PrimaryEntity>
    extends AbstractPrimaryEntity implements PrimaryEntityGroup<E>
{
    protected final PrimaryEntityManager<E> membersManager;

    protected AbstractPrimaryEntityGroup(XWikiDocument document)
    {
        super(document);
        this.membersManager = getEntityManager();
    }

    protected AbstractPrimaryEntityGroup(DocumentReference reference)
    {
        super(reference);
        this.membersManager = getEntityManager();
    }

    private PrimaryEntityManager<E> getEntityManager()
    {
        PrimaryEntityManager<E> manager = null;
        if (getMemberType() != null) {
            ComponentManager cm = ComponentManagerRegistry.getContextComponentManager();
            String[] possibleRoles = new String[3];
            possibleRoles[0] = getLocalSerializer().serialize(getMemberType());
            possibleRoles[1] = getMemberType().getName();
            possibleRoles[2] = StringUtils.removeEnd(getMemberType().getName(), "Class");
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
            this.logger.error("No suitable primary entity manager found for entities of type [{}] available;"
                + " certain group operations will fail", getMemberType());
        }
        return manager;
    }

    @Override
    public Collection<E> getMembers()
    {
        return getMembersOfType(getMemberType());
    }

    @Override
    public Collection<E> getMembersOfType(EntityReference type)
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

            q.bindValue("memberClass", getLocalSerializer().serialize(getMembershipClass()));
            q.bindValue("referenceProperty", getMembershipProperty());
            q.bindValue("selfReference", getFullSerializer().serialize(getDocument()));
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
    public boolean addMember(E member)
    {
        try {
            DocumentAccessBridge dab =
                ComponentManagerRegistry.getContextComponentManager().getInstance(DocumentAccessBridge.class);
            XWikiDocument doc = (XWikiDocument) dab.getDocument(member.getDocument());
            BaseObject obj = doc.getXObject(getMembershipClass(), getMembershipProperty(),
                getFullSerializer().serialize(getDocument()), false);
            if (obj != null) {
                return true;
            }
            obj = doc.newXObject(getMembershipClass(), getXContext());
            obj.setStringValue(getMembershipProperty(), getFullSerializer().serialize(getDocument()));
            this.setMemberParameters(member, obj);
            getXContext().getWiki().saveDocument(doc, "Added to group " + getDocument(), true, getXContext());
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
    public boolean removeMember(E member)
    {
        try {
            DocumentAccessBridge dab =
                ComponentManagerRegistry.getContextComponentManager().getInstance(DocumentAccessBridge.class);
            XWikiDocument doc = (XWikiDocument) dab.getDocument(member.getDocument());
            BaseObject obj = doc.getXObject(getMembershipClass(), getMembershipProperty(),
                getFullSerializer().serialize(getDocument()), false);
            if (obj == null) {
                return true;
            }
            doc.removeXObject(obj);
            getXContext().getWiki().saveDocument(doc, "Removed from group " + getDocument(), true, getXContext());
            return true;
        } catch (Exception ex) {
            this.logger.warn("Failed to remove member from group: {}", ex.getMessage());
        }
        return false;
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

    protected EntityReference getMembershipClass()
    {
        return GROUP_MEMBERSHIP_CLASS;
    }

    protected String getMembershipProperty()
    {
        return REFERENCE_XPROPERTY;
    }
}
