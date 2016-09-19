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
import org.phenotips.entities.PrimaryEntityGroup;
import org.phenotips.entities.PrimaryEntityManager;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.stability.Unstable;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Base class for implementing specific entity groups, where groups declare the members that they contain.
 * <p>
 * By default, this uses the document name as the identifier, the document title as the name, and either a
 * {@code description} property in the main XObject, or the document content, as the description. If two objects use the
 * same document for storage, they are assumed to be equal, and no actual data equality is checked.
 * </p>
 * <p>
 * The group document declares its members by attaching XObjects of type {@link #GROUP_MEMBER_CLASS} pointing to each
 * member document.
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
public abstract class AbstractContainerPrimaryEntityGroup<E extends PrimaryEntity>
    extends AbstractPrimaryEntityGroup<E> implements PrimaryEntityGroup<E>
{
    /** The XClass used for storing membership information by default. */
    public static final EntityReference GROUP_MEMBER_CLASS =
        new EntityReference("GroupMemberClass", EntityType.DOCUMENT,
            Constants.CODE_SPACE_REFERENCE);

    /** The XProperty used to save the class of contained members. */
    private static final String CLASS_XPROPERTY = "class";

    protected AbstractContainerPrimaryEntityGroup(XWikiDocument document)
    {
        super(document);
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
            q.bindValue("entityType", getLocalSerializer().serialize(type));
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
            BaseObject obj = this.document.getXObject(getMembershipClass(), getMembershipProperty(),
                getFullSerializer().serialize(member.getDocument()), false);
            if (obj != null) {
                return true;
            }
            obj = this.document.newXObject(getMembershipClass(), getXContext());
            obj.setStringValue(getMembershipProperty(), getFullSerializer().serialize(member.getDocument()));
            obj.setStringValue(getClassProperty(), getFullSerializer().serialize(member.getType()));
            getXContext().getWiki().saveDocument(this.document, "Added member " + member.getDocument(), true,
                getXContext());
            return true;
        } catch (Exception ex) {
            this.logger.warn("Failed to add member to group: {}", ex.getMessage());
        }
        return false;
    }

    @Override
    public boolean removeMember(E member)
    {
        try {
            BaseObject obj = this.document.getXObject(getMembershipClass(), getMembershipProperty(),
                getFullSerializer().serialize(member.getDocument()), false);
            if (obj == null) {
                return true;
            }
            this.document.removeXObject(obj);
            getXContext().getWiki().saveDocument(this.document, "Removed member " + member.getDocument(), true,
                getXContext());
            return true;
        } catch (Exception ex) {
            this.logger.warn("Failed to remove member from group: {}", ex.getMessage());
        }
        return false;
    }

    @Override
    protected EntityReference getMembershipClass()
    {
        return GROUP_MEMBER_CLASS;
    }

    protected String getClassProperty()
    {
        return CLASS_XPROPERTY;
    }
}
