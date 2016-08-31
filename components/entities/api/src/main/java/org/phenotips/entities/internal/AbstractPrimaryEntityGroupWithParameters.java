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

import org.xwiki.query.Query;
import org.xwiki.query.QueryException;

import java.util.Collection;
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
public abstract class AbstractPrimaryEntityGroupWithParameters<E extends PrimaryEntity>
    extends AbstractPrimaryEntityGroup<E>
{
    private static final String ENTITY_TYPE = "entityType";

    protected AbstractPrimaryEntityGroupWithParameters(XWikiDocument document)
    {
        super(document);
    }

    /**
     * Reads all members and their parameters. Returns a map with key: member name, value: parameters map (name:value).
     *
     * @return parameters map
     */
    protected Map<String, Map<String, String>> getMembersMap(Collection<String> types)
    {
        String inPhrase = this.getInPhrase(ENTITY_TYPE, types.size());
        try {
            //HQL: select member name, parameter name, parameter value ....
            //     where member name in (select all members of this documents)
            StringBuilder hql = new StringBuilder();
            hql.append("select distinct binding.name, groupReference.id.name, groupReference.value ");
            hql.append("from BaseObject binding, StringProperty groupReference, BaseObject entity ");
            hql.append(" where binding.className = :memberClass ");
            hql.append(" and groupReference.id.id = binding.id ");
            hql.append(" and groupReference.id.name != :referenceProperty ");
            hql.append(" and entity.name = binding.name ");
            hql.append(" and entity.className ").append(inPhrase);
            hql.append(" and binding.name in (");

            // Subselect: names of all members of this document (of a certain type)
            hql.append("select distinct binding.name ");
            hql.append(" from BaseObject binding, StringProperty groupReference, BaseObject entity ");
            hql.append(" where binding.className = :memberClass");
            hql.append(" and groupReference.id.id = binding.id ");
            hql.append(" and groupReference.id.name = :referenceProperty");
            hql.append(" and groupReference.value = :selfReference");
            hql.append(" and entity.className ").append(inPhrase);

            hql.append(" )");

            Query q = getQueryManager().createQuery(hql.toString(), Query.HQL);

            q.bindValue("memberClass", getLocalSerializer().serialize(getMembershipClass()));
            q.bindValue("referenceProperty", getMembershipProperty());
            q.bindValue("selfReference", getFullSerializer().serialize(getDocument()));
            this.bindTypesParameters(q, ENTITY_TYPE, types);

            List<Object> members = q.execute();

            return this.buildParametersMap(members);
        } catch (QueryException ex) {
            this.logger.warn("Failed to query members: {}", ex.getMessage());
        }
        return Collections.emptyMap();
    }

    private String getInPhrase(String paramName, int numberOfAllowedValues)
    {
        StringBuilder sb = new StringBuilder(" in (");
        for (int i = 0; i < numberOfAllowedValues; i++) {
            sb.append(":").append(paramName).append(i);
            if (i + 1 < numberOfAllowedValues) {
                sb.append(", ");
            }
        }
        sb.append(") ");
        return sb.toString();
    }

    private void bindTypesParameters(Query q, String paramName, Collection<String> types)
    {
        int i = 0;
        for (String type : types) {
            q.bindValue(paramName + i, type);
            i++;
        }
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
