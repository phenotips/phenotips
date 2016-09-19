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

import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Extending AbstractPrimaryEntityGroup to save and retrieve members with parameters.
 * @param <E> see super
 *
 * @version $Id$
 * @since 1.3M2
 *
 */
public abstract class AbstractContainerPrimaryEntityGroupWithParameters<E extends PrimaryEntity>
    extends AbstractContainerPrimaryEntityGroup<E>
{
    protected AbstractContainerPrimaryEntityGroupWithParameters(XWikiDocument document)
    {
        super(document);
    }

    /**
     * Reads all members and their parameters. Returns a map with key: member name, value: parameters map (name:value).
     *
     * @return parameters map
     */
    protected Map<String, Map<String, String>> getMembersMap(EntityReference type)
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

            // FIXME
            q.bindValue("selfReference", getFullSerializer().serialize(getDocument()).split(":")[1]);
            q.bindValue("memberClass", getLocalSerializer().serialize(getMembershipClass()));
            q.bindValue("entityType", getLocalSerializer().serialize(type));
            q.bindValue("referenceProperty", getMembershipProperty());
            q.bindValue("classProperty", getClassProperty());

            List<Object> members = q.execute();

            return this.buildParametersMap(members);
        } catch (QueryException ex) {
            this.logger.warn("Failed to query members: {}", ex.getMessage());
        }
        return Collections.emptyMap();
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
