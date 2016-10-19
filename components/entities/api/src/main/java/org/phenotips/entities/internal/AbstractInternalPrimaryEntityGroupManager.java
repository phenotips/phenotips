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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
 * @param <G> the type of the group containing the entities
 * @param <E> the type of entities belonging to this group; if more than one type of entities can be part of the group,
 *            then a generic {@code PrimaryEntity} should be used instead
 * @version $Id$
 * @since 1.3M2
 */
@Unstable("New class and interface added in 1.3")
public abstract class AbstractInternalPrimaryEntityGroupManager<G extends PrimaryEntity, E extends PrimaryEntity>
    extends AbstractExternalPrimaryEntityGroupManager<G, E>
    implements PrimaryEntityGroupManager<G, E>
{
    /** The XClass used for storing membership information by default. */
    public static final EntityReference GROUP_MEMBER_CLASS =
        new EntityReference("GroupMemberClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    protected AbstractInternalPrimaryEntityGroupManager(
        EntityReference groupEntityReference, EntityReference memberEntityReference)
    {
        super(groupEntityReference, memberEntityReference);
    }

    /**
     * Unlike getMembersMap, this will not read the parameters that may be saved with the members.
     */
    @Override
    public Collection<E> getMembersOfType(G group, EntityReference type)
    {
        Collection<E> result = new LinkedList<>();
        try {
            StringBuilder hql = new StringBuilder();
            hql.append("select distinct binding.value ")
                .append(" from BaseObject groupReference, StringProperty binding ");
            if (type != null) {
                hql.append(",BaseObject entity ,StringProperty entityBinding ");
            }
            hql.append(" where groupReference.id.id = binding.id and ")
                .append("       groupReference.number = entity.number and")
                .append("       binding.id.name = :referenceProperty and ")
                .append("       groupReference.name = :selfReference and ")
                .append("       groupReference.className = :memberClass");
            if (type != null) {
                hql.append("   and entityBinding.id.name= :classProperty ")
                    .append("   and entityBinding.value = :entityType ")
                    .append("   and entity.name = groupReference.name ")
                    .append("   and entity.id.id = entityBinding.id ");
            }

            Query q = getQueryManager().createQuery(hql.toString(), Query.HQL);

            q.bindValue("selfReference", getLocalSerializer().serialize(group.getDocumentReference()));
            q.bindValue("referenceProperty", getMembershipProperty());
            q.bindValue("classProperty", CLASS_XPROPERTY);
            q.bindValue("entityType", getLocalSerializer().serialize(type));
            q.bindValue("memberClass", getLocalSerializer().serialize(GROUP_MEMBER_CLASS));
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
            XWikiDocument groupDocument = getXWikiDocument(group);
            BaseObject obj = groupDocument.getXObject(GROUP_MEMBER_CLASS, getMembershipProperty(),
                getFullSerializer().serialize(member.getDocumentReference()), false);
            if (obj != null) {
                return true;
            }
            obj = groupDocument.newXObject(GROUP_MEMBER_CLASS, getXContext());
            obj.setStringValue(getMembershipProperty(), getFullSerializer().serialize(member.getDocumentReference()));
            obj.setStringValue(CLASS_XPROPERTY, getFullSerializer().serialize(member.getType()));
            this.setMemberParameters(member, obj);
            getXContext().getWiki().saveDocument(groupDocument, "Added member " + member.getDocumentReference(), true,
                getXContext());
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
            XWikiDocument groupDocument = getXWikiDocument(group);
            BaseObject obj = groupDocument.getXObject(GROUP_MEMBER_CLASS, getMembershipProperty(),
                getFullSerializer().serialize(member.getDocumentReference()), false);
            if (obj == null) {
                return true;
            }
            groupDocument.removeXObject(obj);
            getXContext().getWiki().saveDocument(
                groupDocument, "Removed member " + member.getDocumentReference(), true, getXContext());
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
            // FIXME GROUP_MEMBER_CLASS should be replaced with a static method call
            String bindingClassName = getLocalSerializer().serialize(
                    AbstractInternalPrimaryEntityGroupManager.GROUP_MEMBER_CLASS);
            String groupClass = getLocalSerializer().serialize(groupEntityReference);
            Query q = getQueryManager().createQuery(
                ", BaseObject obj, BaseObject groupObj, StringProperty property "
                + "where obj.id.id = property.id and "
                + "groupObj.name = doc.fullName and "
                + "groupObj.name = obj.name and "
                + "groupObj.className = '" + groupClass + "' and "
                + "property.id.name = 'reference' and "
                + "property.value = :referenceValue and "
                + "obj.className = '" + bindingClassName + "' and "
                + "doc.space = :gspace", Query.HQL);
            q.bindValue("gspace", this.groupManager.getDataSpace().getName());
            q.bindValue("referenceValue", this.getFullSerializer().serialize(member.getDocumentReference()));
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

    /**
     * Reads all members and their parameters. Returns a map with key: member name, value: parameters map (name:value).
     * This function is not exposed to users of the group. If you need to save a member with parameters, override
     * {@link setMemberParameters} and wrap this method. See {@link CollaboratorInProjectManager}.
     *
     * Saving members with parameters is used in cases where rebuilding them requires more information than is saved
     * in the XWiki document. For example, a Collaborator is a User (which is a PrimaryEntity) combined with
     * an AccessLevel. A Collaborator reference is saved together with information about creating the AccessLevel -
     * this is handled in {@link CollaboratorInProjectManager.setMemberParameters()}. This method returns the
     * parameters needed to rebuild a Collaborator, and the rebuilding of these objects is done in
     * {@link CollaboratorInProjectManager.getMembers()}.
     *
     * @return parameters map
     */
    protected Map<String, Map<String, String>> getMembersMap(G group, EntityReference type)
    {
        try {
            StringBuilder hql = new StringBuilder();

            hql.append("select binding.value, propertyTable.name, propertyTable.value ")
                .append("from BaseObject groupReference, StringProperty binding, BaseObject entity, ")
                .append("     StringProperty propertyTable  ")
                .append("where groupReference.id.id = binding.id and ")
                .append("      binding.id.name = :referenceProperty and  ")
                .append("      groupReference.name = :selfReference and  ")
                .append("      entity.name = groupReference.name and ")
                .append("      entity.id.id = propertyTable.id and ")
                .append("      groupReference.className = :memberClass and ")
                .append("      groupReference.number = entity.number and ")
                .append("      binding.value in ");

            // Subselect: names of all members of this document (of a certain type)
            hql.append("(select distinct binding.value ")
                .append(" from BaseObject groupReference, StringProperty binding, BaseObject entity, ")
                .append("      StringProperty entityBinding ")
                .append(" where groupReference.id.id = binding.id and ")
                .append("       entity.name = groupReference.name and")
                .append("       entity.id.id = entityBinding.id and")
                .append("       groupReference.number = entity.number and")
                .append("       binding.id.name = :referenceProperty and ")
                .append("       groupReference.name = :selfReference and ")
                .append("       entityBinding.id.name= :classProperty and ")
                .append("       entityBinding.value = :entityType and ")
                .append("       groupReference.className = :memberClass)");

            Query q = getQueryManager().createQuery(hql.toString(), Query.HQL);

            q.bindValue("selfReference", getLocalSerializer().serialize(group.getDocumentReference()));
            q.bindValue("memberClass", getLocalSerializer().serialize(GROUP_MEMBER_CLASS));
            q.bindValue("entityType", getLocalSerializer().serialize(type));
            q.bindValue("referenceProperty", getMembershipProperty());
            q.bindValue("classProperty", CLASS_XPROPERTY);

            List<Object> members = q.execute();

            return this.buildParametersMap(members);
        } catch (QueryException ex) {
            this.logger.warn("Failed to query members: {}", ex.getMessage());
        }
        return Collections.emptyMap();
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

    private Map<String, Map<String, String>> buildParametersMap(List<Object> members)
    {
        Map<String, Map<String, String>> parameters = new HashMap<>();

        for (Object o : members) {
            Object[] asArray = (Object[]) o;

            String entityName = (String) asArray[0];
            String propertyName = (String) asArray[1];
            String propertyValue = (String) asArray[2];

            Map<String, String> map = parameters.get(entityName);
            if (map == null) {
                map = new HashMap<>();
                parameters.put(entityName, map);
            }

            map.put(propertyName, propertyValue);
        }

        return parameters;
    }
}
