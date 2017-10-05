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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
 * Migrate from old internal pedigree format to the new.
 *
 * The migration also includes all pedigree migrations previously implemented in javascript (in versionUpdater.js)
 *
 * @version $Id$
 * @since 1.4M3
 */
@Component
@Named("R71507-PT-3423")
@Singleton
public class R71507PhenoTips3423DataMigration extends AbstractHibernateDataMigration implements
    HibernateCallback<Object>
{
    /**
     * Pedigree XClass that holds pedigree data (image, structure, etc).
     */
    private static final EntityReference PEDIGREE_CLASS_REFERENCE =
        new EntityReference("PedigreeClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String PEDIGREECLASS_JSONDATA_KEY = "data";

    private static final String OLD_PEDIGREE_GRAPH_KEY = "GG";

    private static final String OLD_PEDIGREE_RANKS_KEY = "ranks";

    private static final String OLD_PEDIGREE_PROBAND_KEY = "probandNodeID";

    private static final String OLD_PEDIGREE_POSITIONS_KEY = "positions";

    private static final String OLD_PEDIGREE_ORDER_KEY = "order";

    private static final String OLD_PEDIGREE_SETTINGS_KEY = "settings";

    private static final String OLD_PEDIGREE_PROPERTIES_KEY = "prop";

    private static final String OLD_PEDIGREE_NODE_OUTEDGES = "outedges";

    private static final String OLD_PEDIGREE_NODE_OUTEDGES_TO = "to";

    private static final String OLD_PEDIGREE_NODE_TYPE_RELATION = "rel";

    private static final String OLD_PEDIGREE_NODE_TYPE_CHILDHUB = "chhub";

    private static final String OLD_PEDIGREE_NODE_TYPE_VIRTUAL = "virt";

    private static final String PEDIGREE_SETTINGS_KEY = OLD_PEDIGREE_SETTINGS_KEY;

    private static final String PEDIGREE_VERSION_KEY = "JSON_version";

    private static final String PEDIGREE_VERSION_VALUE = "1.0";

    private static final String PEDIGREE_PROBAND_KEY = "proband";

    private static final String PEDIGREE_MEMBERS_KEY = "members";

    private static final String PEDIGREE_RELATIONS_KEY = "relationships";

    private static final String PEDIGREE_LAYOUT_KEY = "layout";

    private static final String PEDIGREE_LAYOUT_MEMBERS = PEDIGREE_MEMBERS_KEY;

    private static final String PEDIGREE_LAYOUT_RELATIONS = PEDIGREE_RELATIONS_KEY;

    private static final String PEDIGREE_LAYOUT_LONGEDGES = "longedges";


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
        return new XWikiDBVersion(71507);
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

    // used to store data about a (potentially multi-generation) path (in a graph sense)
    // from partners to their relationships
    private static class PartnerPath
    {
        private Integer parentID;
        private List<Integer> path;

        PartnerPath(Integer parentID)
        {
            this.parentID = parentID;
            this.path = new LinkedList<>();
        }

        public void addPathElement(Integer virtualNodeID)
        {
            this.path.add(virtualNodeID);
        }

        public Integer getParentID()
        {
            return this.parentID;
        }

        public List<Integer> getPath()
        {
            return this.path;
        }

        public String toString()
        {
            if (this.path.size() == 0) {
                return this.parentID.toString() + " (direct)";
            } else {
                return this.parentID.toString() + " -> <" + this.path.toString() + ">";
            }
        }
    }

    private boolean updatePedigree(BaseObject pedigreeXObject, XWikiContext context, String documentName)
    {
        String oldPedigreeAsText = pedigreeXObject.getStringValue(PEDIGREECLASS_JSONDATA_KEY);
        if (!StringUtils.isEmpty(oldPedigreeAsText)) {
            String convertedPedigree = this.convertPedigreeData(oldPedigreeAsText);
            pedigreeXObject.set(PEDIGREECLASS_JSONDATA_KEY, convertedPedigree, context);
            return true;
        }
        return false;
    }

    private String convertPedigreeData(String oldFormatData)
    {
        JSONObject oldPedigreeData = new JSONObject(oldFormatData);

        this.logger.debug("Old pedigree: [{}]", oldPedigreeData.toString());

        // create  asceleton JSON with all data structures present but blank
        JSONObject newPedigree = this.createBlankNewPedigreeJSON();

        newPedigree.put(PEDIGREE_PROBAND_KEY, oldPedigreeData.optInt(OLD_PEDIGREE_PROBAND_KEY, 0));

        JSONObject pedigreeSettings = oldPedigreeData.optJSONObject(OLD_PEDIGREE_SETTINGS_KEY);
        if (pedigreeSettings != null) {
            newPedigree.put(PEDIGREE_SETTINGS_KEY, pedigreeSettings);
        }

        // Note1: the assumption is that pedigree is consistent and correct, if a required key
        //        is missing an exception will be thrown (e.g. by getJSONArray()), which is expected
        //
        // Note2: migrated pedigree will preserve all node IDs, so no ID mapping is required

        JSONArray oldData = oldPedigreeData.getJSONArray(OLD_PEDIGREE_GRAPH_KEY);

        // ranks and positions are stored as arrays of integer values indexed by nodeID
        JSONArray ranks = oldPedigreeData.getJSONArray(OLD_PEDIGREE_RANKS_KEY);
        JSONArray positions = oldPedigreeData.getJSONArray(OLD_PEDIGREE_POSITIONS_KEY);

        // convert orders as stored to an array of integer order values indexed by nodeID
        Map<Integer, Integer> order = this.convertOrders(oldPedigreeData.getJSONArray(OLD_PEDIGREE_ORDER_KEY));

        // 1. collect all data in a way addressable by nodeID
        Map<Integer, JSONObject> data = new HashMap<>();
        for (Object node : oldData) {
            JSONObject nodeObj = (JSONObject) node;
            data.put(nodeObj.getInt("id"), nodeObj);
        }

        // 2. process all person nodes, collecting data about relationships
        //    (each node only has outedges specified, so in order to know relationship members need
        //    to process person nodes first)

        // for each relationship indicated by its nodeID -> a list of paths to the partners involved
        Map<Integer, List<PartnerPath>> relationshipParents = new HashMap<>();

        for (Map.Entry<Integer, JSONObject> entry : data.entrySet()) {
            Integer id = entry.getKey();
            JSONObject nodeObj = entry.getValue();

            if (!nodeObj.has(OLD_PEDIGREE_NODE_TYPE_RELATION)
                && !nodeObj.has(OLD_PEDIGREE_NODE_TYPE_CHILDHUB)
                && !nodeObj.has(OLD_PEDIGREE_NODE_TYPE_VIRTUAL)) {

                this.processPerson(id, newPedigree, data, ranks, order, positions);

                // collect data about all relationships this person is part of
                // (the data is only available via person nodes)
                this.collectRelationshipData(id, data, relationshipParents);
            }
        }

        // 3. process all relationships, now that we know all the partners
        if (!relationshipParents.isEmpty()) {
            newPedigree.put(PEDIGREE_RELATIONS_KEY, new JSONArray());
        }
        for (Map.Entry<Integer, List<PartnerPath>> entry : relationshipParents.entrySet()) {
            Integer id = entry.getKey();
            List<PartnerPath> parents = entry.getValue();
            if (parents.size() != 2) {
                throw new IllegalArgumentException("Unsupported pedigree: a relationship with ID="
                    + id.toString() + "doesn't have exactly two partners");
            }
            this.processRelationship(id, parents, newPedigree, data, order, positions);
        }

        this.logger.debug("Converted pedigree: [{}]", newPedigree.toString());

        return newPedigree.toString();
    }

    private JSONObject createBlankNewPedigreeJSON()
    {
        JSONObject newPedigree = new JSONObject();

        newPedigree.put(PEDIGREE_VERSION_KEY, PEDIGREE_VERSION_VALUE);

        newPedigree.put(PEDIGREE_MEMBERS_KEY, new JSONArray());

        JSONObject layout = new JSONObject();
        layout.put(PEDIGREE_LAYOUT_MEMBERS, new JSONObject());
        layout.put(PEDIGREE_LAYOUT_RELATIONS, new JSONObject());
        layout.put(PEDIGREE_LAYOUT_LONGEDGES, new JSONObject());
        newPedigree.put(PEDIGREE_LAYOUT_KEY, layout);

        return newPedigree;
    }

    // convert orders as stored (a sequence or node IDs, one per each rank) to an array of orders indexed by ID
    private Map<Integer, Integer> convertOrders(JSONArray storedOrder)
    {
        Map<Integer, Integer> order = new HashMap<>();

        for (Object nodesOnRank : storedOrder) {
            JSONArray nodeList = (JSONArray) nodesOnRank;
            for (int index = 0; index < nodeList.length(); index++) {
                int nodeId = nodeList.getInt(index);
                order.put(nodeId, index);
            }
        }
        return order;
    }

    // adds data (and layout) for node with the given ID to `newPedigree`, and adds itself as a parent
    // to `relationshipParents` for each relationship this person is a pert of
    private void processPerson(Integer id, JSONObject newPedigree, Map<Integer, JSONObject> data,
            JSONArray ranks, Map<Integer, Integer> order, JSONArray positions)
    {
        JSONObject oldObj = data.get(id);

        JSONObject newObj = new JSONObject();
        newObj.put("id", id);

        // use properties "as is" from the internal pedigree format -
        // those are still supported in case `properties` key is missing
        newObj.put("pedigreeProperties", oldObj.get(OLD_PEDIGREE_PROPERTIES_KEY));

        // store migrated data for this person
        newPedigree.getJSONArray(PEDIGREE_MEMBERS_KEY).put(newObj);

        // store layout info for pedigree node representig this person
        JSONObject memberLayout = new JSONObject();
        memberLayout.put("generation", ranks.get(id));
        memberLayout.put("x", positions.get(id));
        memberLayout.put("order", order.get(id));
        newPedigree.getJSONObject(PEDIGREE_LAYOUT_KEY)
            .getJSONObject(PEDIGREE_LAYOUT_MEMBERS).put(id.toString(), memberLayout);
    }

    private void collectRelationshipData(Integer personID, Map<Integer, JSONObject> data,
            Map<Integer, List<PartnerPath>> relationshipParents)
    {
        // Each "outedge" is a link to a relationship (possibly via a chain of multi-rank edges)
        JSONArray relationships = data.get(personID).optJSONArray(OLD_PEDIGREE_NODE_OUTEDGES);
        if (relationships != null) {
            for (Object link : relationships) {
                PartnerPath relationshipPath = new PartnerPath(personID);

                JSONObject nextNode = (JSONObject) link;
                Integer nextID = nextNode.getInt(OLD_PEDIGREE_NODE_OUTEDGES_TO);

                while (data.get(nextID).has(OLD_PEDIGREE_NODE_TYPE_VIRTUAL)) {
                    relationshipPath.addPathElement(nextID);
                    JSONObject virtualNodeData = data.get(nextID);
                    nextID = virtualNodeData.getJSONArray(OLD_PEDIGREE_NODE_OUTEDGES)
                            .getJSONObject(0).getInt(OLD_PEDIGREE_NODE_OUTEDGES_TO);
                }
                Integer relationshipID = nextID;
                if (!relationshipParents.containsKey(relationshipID)) {
                    relationshipParents.put(relationshipID, new ArrayList<>(2));
                }
                relationshipParents.get(relationshipID).add(relationshipPath);
            }
        }
    }

    private void processRelationship(Integer id, List<PartnerPath> parents, JSONObject newPedigree,
            Map<Integer, JSONObject> data, Map<Integer, Integer> order, JSONArray positions)
    {
        JSONObject oldObj = data.get(id);

        JSONObject newObj = new JSONObject();
        newObj.put("id", id);

        newObj.put("properties", this.convertRelationshipProperties(oldObj));

        newObj.put("members", this.getRelationshipMembers(parents));

        newObj.put("children", this.getRelationshipChildren(oldObj, data));

        // long edge data (if any) is stored in the "layout" object outside of this relationship object,
        // so the method has a side effect of directly updating `newPedigree` object
        this.updateLongEdgeLayout(id, parents, newPedigree, order, positions);

        // store migrated data for this relationship
        newPedigree.getJSONArray(PEDIGREE_RELATIONS_KEY).put(newObj);

        // store layout info for pedigree node representing this relationship
        JSONObject relationshipLayout = new JSONObject();
        relationshipLayout.put("x", positions.get(id));
        relationshipLayout.put("order", order.get(id));
        newPedigree.getJSONObject(PEDIGREE_LAYOUT_KEY)
            .getJSONObject(PEDIGREE_LAYOUT_RELATIONS).put(id.toString(), relationshipLayout);
    }

    private JSONObject convertRelationshipProperties(JSONObject oldRelationshipJSON)
    {
        JSONObject properties = new JSONObject();

        JSONObject oldProperties = oldRelationshipJSON.optJSONObject(OLD_PEDIGREE_PROPERTIES_KEY);
        if (oldProperties == null) {
            return properties;
        }

        if (oldProperties.optBoolean("broken", false)) {
            properties.put("separated", true);
        }
        if (oldProperties.has("consangr")) {
            String consangr = oldProperties.getString("consangr");
            if ("Y".equals(consangr)) {
                properties.put("consanguinity", "yes");
            } else if ("N".equals(consangr)) {
                properties.put("consanguinity", "no");
            }
        }
        if (oldProperties.has("childlessStatus")) {
            properties.put("childlessStatus", oldProperties.getString("childlessStatus"));
            properties.put("childlessReason", oldProperties.optString("childlessReason", null));
        }
        return properties;
    }

    private JSONArray getRelationshipMembers(List<PartnerPath> parents)
    {
        // fill in members and long edge data
        JSONArray members = new JSONArray();
        for (PartnerPath path : parents) {
            Integer parentID = path.getParentID();
            members.put(parentID);
        }
        return members;
    }

    private void updateLongEdgeLayout(Integer relationshipID, List<PartnerPath> parents, JSONObject newPedigree,
            Map<Integer, Integer> order, JSONArray positions)
    {
        for (PartnerPath path : parents) {
            if (path.getPath().size() > 0) {
                // get the layout of the long edge, segment-by-segment
                JSONArray longEdgePath = new JSONArray();
                for (Integer virtualVerexID : path.getPath()) {
                    JSONObject pathElement = new JSONObject();
                    pathElement.put("order", order.get(virtualVerexID));
                    pathElement.put("x", positions.get(virtualVerexID));
                    longEdgePath.put(pathElement);
                }
                // update the layout data in the converted pedigree
                JSONObject longEdgeData = new JSONObject();
                longEdgeData.put("member", path.getParentID());
                longEdgeData.put("path", longEdgePath);
                newPedigree.getJSONObject(PEDIGREE_LAYOUT_KEY)
                    .getJSONObject(PEDIGREE_LAYOUT_LONGEDGES).put(relationshipID.toString(), longEdgeData);
                this.logger.debug("Long edge data for [{}]: [{}]", relationshipID, longEdgeData.toString());
            }
        }
    }

    private JSONArray getRelationshipChildren(JSONObject oldRelationshipData, Map<Integer, JSONObject> data)
    {
        JSONArray children = new JSONArray();

        Integer childHubID = oldRelationshipData.getJSONArray(OLD_PEDIGREE_NODE_OUTEDGES)
                .getJSONObject(0).getInt(OLD_PEDIGREE_NODE_OUTEDGES_TO);

        JSONObject childHubObj = data.get(childHubID);

        JSONArray childHubOutedges = childHubObj.getJSONArray(OLD_PEDIGREE_NODE_OUTEDGES);
        for (int i = 0; i < childHubOutedges.length(); i++) {
            JSONObject edge = childHubOutedges.getJSONObject(i);
            Integer childID = edge.getInt(OLD_PEDIGREE_NODE_OUTEDGES_TO);
            JSONObject oldChildProperties = data.get(childID).getJSONObject(OLD_PEDIGREE_PROPERTIES_KEY);

            JSONObject childObj = new JSONObject();
            childObj.put("id", childID);
            switch (oldChildProperties.optString("adoptedStatus", "")) {
                case "adoptedIn":
                    childObj.put("adopted", "in");
                    break;
                case "adoptedOut":
                    childObj.put("adopted", "out");
                    break;
                default:
            }
            children.put(childObj);
        }

        return children;
    }
}
