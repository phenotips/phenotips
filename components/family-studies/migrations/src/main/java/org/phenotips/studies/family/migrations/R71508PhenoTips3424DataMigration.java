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

package org.phenotips.studies.family.migrations;

import org.phenotips.Constants;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migrate internal representation of pedigrees stored in the auto-generated SimpleJSON format.
 *
 * @version $Id$
 * @since 1.4M3
 */
@Component
@Named("R71508-PT-3424")
@Singleton
public class R71508PhenoTips3424DataMigration extends AbstractHibernateDataMigration implements
    HibernateCallback<Object>
{
    /**
     * Pedigree XClass that holds pedigree data (image, structure, etc).
     */
    private static final EntityReference PEDIGREE_CLASS_REFERENCE =
        new EntityReference("PedigreeClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String PEDIGREECLASS_JSONDATA_KEY = "data";

    private static final String SIMPLE_JSON_DATA_KEY = "data";

    private static final String SIMPLE_JSON_PATIENT_LINK_KEY = "phenotipsId";

    private static final String SIMPLE_JSON_EXTERNALID_KEY = "externalId";

    private static final String PEDIGREE_VERSION_KEY = "JSON_version";

    private static final String PEDIGREE_VERSION_VALUE = "1.0";

    private static final String PEDIGREE_PROBAND_KEY = "proband";

    private static final String PEDIGREE_MEMBERS_KEY = "members";

    private static final String PEDIGREE_RELATIONS_KEY = "relationships";

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Resolves unprefixed document names to the current wiki. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> resolver;

    /** Serializes the class name without the wiki prefix, to be used in the database query. */
    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> serializer;

    @Override
    public String getDescription()
    {
        return "Update pedigree data to new format";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(71508);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), this);
    }

    @Override
    public Object doInHibernate(Session session) throws HibernateException, XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();

        // Select all families
        Query q = session.createQuery("select distinct o.name from BaseObject o where o.className = '"
            + this.serializer.serialize(PEDIGREE_CLASS_REFERENCE)
            + "' and o.name <> 'PhenoTips.FamilyTemplate'");

        @SuppressWarnings("unchecked")
        List<String> docs = q.list();

        this.logger.debug("Found {} documents", docs.size());

        for (String docName : docs) {
            XWikiDocument xDocument;
            BaseObject pedigreeXObject;

            try {
                xDocument = xwiki.getDocument(this.resolver.resolve(docName), context);
                if (xDocument == null) {
                    continue;
                }

                pedigreeXObject = xDocument.getXObject(PEDIGREE_CLASS_REFERENCE);
                if (pedigreeXObject == null) {
                    continue;
                }
            } catch (Exception e) {
                this.logger.error("Error checking pedigree data for document {}: [{}]",
                        docName, e.getMessage());
                continue;
            }

            try {
                this.logger.debug("Updating pedigree for document {}.", docName);

                if (!this.updatePedigree(pedigreeXObject, context, docName)) {
                    continue;
                }

                xDocument.setComment(this.getDescription());
                xDocument.setMinorEdit(true);

            } catch (Exception e) {
                this.logger.error("Error updating pedigree data format for document {}: [{}]",
                        docName, e.getMessage());
                continue;
            }

            try {
                // There's a bug in XWiki which prevents saving an object in the same session that it was loaded,
                // so we must clear the session cache first.
                session.clear();
                ((XWikiHibernateStore) getStore()).saveXWikiDoc(xDocument, context, false);
                session.flush();
            } catch (DataMigrationException e) {
                this.logger.error("Error when saving XWiki document {}: [{}]", docName, e.getMessage());
            }

        }
        return null;
    }

    private boolean updatePedigree(BaseObject pedigreeXObject, XWikiContext context, String docName)
    {
        String oldPedigreeAsText = pedigreeXObject.getStringValue(PEDIGREECLASS_JSONDATA_KEY);
        if (!StringUtils.isEmpty(oldPedigreeAsText)) {
            if (pedigreeIsInSimpleJSONFormat(oldPedigreeAsText)) {
                String convertedPedigree = this.convertPedigreeData(oldPedigreeAsText);
                pedigreeXObject.set(PEDIGREECLASS_JSONDATA_KEY, convertedPedigree, context);
                return true;
            }
        }
        return false;
    }

    private boolean pedigreeIsInSimpleJSONFormat(String pedigreeAsText)
    {
        JSONObject pedigreeJSON = new JSONObject(pedigreeAsText);
        return (pedigreeJSON.optJSONArray(SIMPLE_JSON_DATA_KEY) != null);
    }

    // represents all data known about a particular relationship
    private static class Relationship
    {
        private Set<Integer> parents = new HashSet<>();

        private Set<Integer> children = new HashSet<>();

        private final String id;

        Relationship(Integer parentID1, Integer parentID2)
        {
            this.id = "rel_" + Math.min(parentID1, parentID2) + "_" + Math.max(parentID1, parentID2);

            if (parentID1 >= 0) {
                this.parents.add(parentID1);
            }
            if (parentID2 >= 0) {
                this.parents.add(parentID2);
            }
        }

        public String getCoupleId()
        {
            return this.id;
        }

        public Set<Integer> getParents()
        {
            return this.parents;
        }

        public Set<Integer> getChildren()
        {
            return this.children;
        }

        public void addChild(Integer id)
        {
            this.children.add(id);
        }
    }

    private String convertPedigreeData(String oldFormatData)
    {
        JSONObject oldPedigreeData = new JSONObject(oldFormatData);

        this.logger.debug("Old pedigree: [{}]", oldPedigreeData.toString());

        // create a sceleton JSON with all data structures present but blank
        JSONObject newPedigree = this.createBlankNewPedigreeJSON();

        // Note1: the assumption is that pedigree is consistent and correct, if a required key
        //        is missing an exception will be thrown (e.g. by getJSONArray()), which is expected

        JSONArray oldData = oldPedigreeData.getJSONArray(SIMPLE_JSON_DATA_KEY);

        // 1. process all persons nodes, collecting data about relationships

        // for each pair of parents/partners (represented by an integer "combination ID") -> a relationship object
        Map<String, Relationship> relationships = new HashMap<>();

        for (Object node : oldData) {
            JSONObject nodeObj = (JSONObject) node;
            this.processPerson(nodeObj, newPedigree, relationships);
        }

        // 2. process all relationships, now that we know all the partners
        int relationshipNumber = 1;
        for (Relationship relationship : relationships.values()) {
            this.processRelationship(relationshipNumber++, relationship, newPedigree);
        }

        this.logger.debug("Converted pedigree: [{}]", newPedigree.toString());

        return newPedigree.toString();
    }

    private JSONObject createBlankNewPedigreeJSON()
    {
        JSONObject newPedigree = new JSONObject();

        newPedigree.put(PEDIGREE_VERSION_KEY, PEDIGREE_VERSION_VALUE);

        newPedigree.put(PEDIGREE_MEMBERS_KEY, new JSONArray());

        newPedigree.put(PEDIGREE_RELATIONS_KEY, new JSONArray());

        return newPedigree;
    }

    private void processPerson(JSONObject oldJSON, JSONObject newPedigree, Map<String, Relationship> relationships)
    {
        Integer nodeId = oldJSON.getInt("id");

        JSONObject newObj = new JSONObject();
        newObj.put("id", nodeId);

        // convert properties into old phenotips properties
        JSONObject properties = new JSONObject();
        properties.put("gender", oldJSON.optString("sex", "U"));
        if (oldJSON.has(SIMPLE_JSON_PATIENT_LINK_KEY)) {
            properties.put("phenotipsId", oldJSON.getString(SIMPLE_JSON_PATIENT_LINK_KEY));
        }
        if (oldJSON.has(SIMPLE_JSON_EXTERNALID_KEY)) {
            properties.put("externalID", oldJSON.getString(SIMPLE_JSON_EXTERNALID_KEY));
        }

        newObj.put("pedigreeProperties", properties);

        // store migrated data for this person
        newPedigree.getJSONArray(PEDIGREE_MEMBERS_KEY).put(newObj);

        // if parents are known: create or update the producing relationship
        if (oldJSON.has("mother") || oldJSON.has("father")) {
            Relationship rel = new Relationship(oldJSON.optInt("mother", -1), oldJSON.optInt("father", -1));
            if (relationships.containsKey(rel.getCoupleId())) {
                rel = relationships.get(rel.getCoupleId());
            } else {
                relationships.put(rel.getCoupleId(), rel);
            }
            rel.addChild(nodeId);
        }

        if (oldJSON.optBoolean("proband", false)) {
            newPedigree.put(PEDIGREE_PROBAND_KEY, nodeId);
        }
    }

    private void processRelationship(int relationshipNumber, Relationship relationship, JSONObject newPedigree)
    {
        JSONObject newObj = new JSONObject();
        newObj.put("id", relationshipNumber);

        newObj.put("members", relationship.getParents());

        newObj.put("children", this.createRelationshipChildrenArray(relationship.getChildren()));

        // store migrated data for this relationship
        newPedigree.getJSONArray(PEDIGREE_RELATIONS_KEY).put(newObj);
    }

    private JSONArray createRelationshipChildrenArray(Set<Integer> childIDs)
    {
        JSONArray children = new JSONArray();
        for (Integer childID : childIDs) {
            JSONObject nextChild = new JSONObject();
            nextChild.put("id", childID);
            children.put(nextChild);
        }
        return children;
    }
}
