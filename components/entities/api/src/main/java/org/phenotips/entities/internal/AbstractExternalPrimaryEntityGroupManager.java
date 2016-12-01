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
import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.PrimaryEntityGroupManager;
import org.phenotips.entities.PrimaryEntityManager;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.stability.Unstable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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
public abstract class AbstractExternalPrimaryEntityGroupManager<G extends PrimaryEntity, E extends PrimaryEntity>
    extends AbstractPrimaryEntityGroupManager<G, E>
    implements PrimaryEntityGroupManager<G, E>
{
    /** The XClass used for storing membership information by default. */
    protected static final EntityReference GROUP_MEMBERSHIP_CLASS =
        new EntityReference("EntityBindingClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    protected AbstractExternalPrimaryEntityGroupManager(
        EntityReference groupEntityReference, EntityReference memberEntityReference)
    {
        super(groupEntityReference, memberEntityReference);
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
            q.bindValue("selfReference", getFullSerializer().serialize(group.getDocumentReference()));
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
    public boolean addMember(G group, E member)
    {
        try {
            XWikiDocument doc = member.getDocument();
            BaseObject obj = doc.getXObject(GROUP_MEMBERSHIP_CLASS, getMembershipProperty(),
                getFullSerializer().serialize(group.getDocumentReference()), false);
            if (obj != null) {
                return true;
            }
            obj = doc.newXObject(GROUP_MEMBERSHIP_CLASS, getXContext());
            obj.setStringValue(getMembershipProperty(), getFullSerializer().serialize(group.getDocumentReference()));
            getXContext().getWiki().saveDocument(
                doc, "Added to group " + group.getDocumentReference(), true, getXContext());
            return true;
        } catch (Exception ex) {
            this.logger.warn("Failed to add member to group: {}", ex.getMessage());
        }
        return false;
    }

    @Override
    public boolean removeMember(G group, E member)
    {
        try {
            XWikiDocument doc = getXWikiDocument(member);
            BaseObject obj = doc.getXObject(GROUP_MEMBERSHIP_CLASS, getMembershipProperty(),
                getFullSerializer().serialize(group.getDocumentReference()), false);
            if (obj == null) {
                return true;
            }
            doc.removeXObject(obj);
            getXContext().getWiki().saveDocument(
                doc, "Removed from group " + group.getDocumentReference(), true, getXContext());
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
            q.bindValue("name", this.getLocalSerializer().serialize(member.getDocumentReference()));
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

    protected String getMembershipProperty()
    {
        return REFERENCE_XPROPERTY;
    }
}
