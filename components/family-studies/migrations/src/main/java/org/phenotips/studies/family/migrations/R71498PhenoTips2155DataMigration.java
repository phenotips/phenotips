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
import org.phenotips.data.Patient;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.Pedigree;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.collections4.MapUtils;
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
import com.xpn.xwiki.objects.BaseStringProperty;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration for PhenoTips issue #2155: migrating old family studies data.
 *
 * It is assumed that all old family studies relationships are specified for the proband node.
 * Thus if node A has a relationship to node B, and node B also has a relationship to node A,
 * then no link is made since it is not clear which one is the proband (this case is considered
 * unlikely); such cases should be resolved manually.
 *
 * Otherwise, if there already exists a pedigree for the node which has an old family
 * studies relationship, that relationship is added to that node's pedigree comment.
 * A comment without any pedigree links is also added in case relationship can not be
 * converted to a pedigree in a uniform way, e.g. for "uncle" relationships it is not clear
 * if the uncle is on the maternal or paternal side.
 *
 * In all other cases (no pedigree, relationships are only specified onw-way for one node,
 * and relationship can be one-to-one mapped to a pedigree layout) a new family+pedigree is created
 * and all mentioned patients are linked to the corresponding nodes in that pedigree.
 *
 * @version $Id$
 * @since 1.3M4
 */
@Component
@Named("R71498PhenoTips#2155")
@Singleton
public class R71498PhenoTips2155DataMigration extends AbstractHibernateDataMigration
    implements HibernateCallback<Object>
{
    private static final String RELATIVE_PROPERTY_NAME = "relative_type";

    private static final String RELATIVEOF_PROPERTY_NAME = "relative_of";

    private static final String REFERENCE_PROPERTY_NAME = "reference";

    private static final String PEDIGREE_MIGRATED_RELATIVE_COMMENT_HEADER =
        "Data migrated from Family Studies:";

    private static final String GENDER = "gender";

    private static final String MALE = "M";

    private static final String FEMALE = "F";

    private static final String LASTNAME = "lastName";

    private static final String OWNER = "owner";

    private static final String JSON_ID = "id";

    private static final String JSON_SEX = "sex";

    private static final String JSON_PHENOTIPSID = "phenotipsId";

    private static final String JSON_PROBAND = "proband";

    private static final String JSON_COMMENTS = "comments";

    private static final String JSON_TWINGROUP = "twinGroup";

    private static final String JSON_MOTHER = "mother";

    private static final String JSON_FATHER = "father";

    private static final String PATIENT_DOCUMENT_GENDER_FIELD = GENDER;

    private static final String PATIENT_DOCUMENT_LASTNAME_FIELD = "last_name";

    private static final String FAMSTUDIES_PARENT = "parent";

    private static final String FAMSTUDIES_CHILD = "child";

    private static final String FAMSTUDIES_SIBLING = "sibling";

    private static final String FAMSTUDIES_TWIN = "twin";

    private static final Set<String> AUTO_LINKING_SUPPORTED_RELATIVE_TYPES = new HashSet<>(
        Arrays.asList(FAMSTUDIES_PARENT, FAMSTUDIES_CHILD, FAMSTUDIES_SIBLING, FAMSTUDIES_TWIN));

    private static final Map<String, String> RELATIVE_ID_TO_NAME =
        Collections.unmodifiableMap(MapUtils.putAll(new HashMap<String, String>(), new String[][] {
            { FAMSTUDIES_PARENT, "a parent" },
            { FAMSTUDIES_CHILD, "a child" },
            { FAMSTUDIES_SIBLING, "a sibling" },
            { FAMSTUDIES_TWIN, "a twin" },
            { "cousin", "a cousin" },
            { "aunt_uncle", "an aunt/uncle" },
            { "niece_nephew", "a niece/nephew" },
            { "grandparent", "a grandparent" },
            { "grandchild", "a grandchild" }
        }));

    private EntityReference relativeClassReference = new EntityReference("RelativeClass",
        EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private Session session;

    private XWikiContext context;

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

    @Inject
    private PhenotipsFamilyMigrations familyMigrations;

    @Override
    public String getDescription()
    {
        return "Migrating old family studies data";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(71498);
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), this);
    }

    @Override
    public Object doInHibernate(Session hSession) throws HibernateException, XWikiException
    {
        // patientID -> familyID
        Map<String, String> existingPatientFamilies = new HashMap<>();

        // patientID -> { anotherPatientID -> relationType }
        Map<String, Map<String, String>> patientsWithStudies = new HashMap<>();

        // patientID -> {list of patients referring to this patient}
        Map<String, List<String>> patientReferredBy = new HashMap<>();

        this.session = hSession;
        this.context = getXWikiContext();
        XWiki xwiki = this.context.getWiki();

        // Select all patients
        Query q =
            this.session.createQuery("select distinct o.name from BaseObject o where"
                + " o.className = :patclass and o.name <> 'PhenoTips.PatientTemplate'");
        q.setString("patclass", this.serializer.serialize(Patient.CLASS_REFERENCE));

        @SuppressWarnings("unchecked")
        List<String> documents = q.list();

        try {
            // 1) get all the data for all patients - we may need to know about patient X
            // when processing patient Y
            for (String docName : documents) {
                XWikiDocument patientXDocument =
                    xwiki.getDocument(this.resolver.resolve(docName), this.context);
                if (patientXDocument == null) {
                    continue;
                }

                String patientId = patientXDocument.getDocumentReference().getName();

                // get existing families - will be not null for patients who already have a pedigree
                String patientFamilyRef = this.getPatientsFamily(patientXDocument);
                existingPatientFamilies.put(patientId, patientFamilyRef);

                Map<String, String> patientRelatives = this.getRelatives(patientXDocument);
                patientsWithStudies.put(patientId, patientRelatives);

                for (String relativeId : patientRelatives.keySet()) {
                    List<String> relativeList = patientReferredBy.get(relativeId);
                    if (relativeList == null) {
                        patientReferredBy.put(relativeId, new LinkedList<String>());
                        relativeList = patientReferredBy.get(relativeId);
                    }
                    relativeList.add(patientId);
                }
            }

            // 2) go patient by patient, find "proband" patients, and create new families using family studies
            // data (when possible) or add comments to existing pedigrees using family studies data
            // (when a pedigree is already present)
            this.processData(patientsWithStudies, patientReferredBy, existingPatientFamilies);

        } catch (Exception ex) {
            this.logger.error("Family Studies migration exception: [{}] [{}]", ex.getMessage(), ex);
        } finally {
            this.context.getWiki().flushCache(this.context);
        }
        return null;
    }

    /**
     * The actual migration work: go patient by patient, find "proband" patients, and create new families using family
     * studies data (when possible) or add comments to existing pedigrees using family studies data (when a pedigree is
     * already present).
     */
    private void processData(Map<String, Map<String, String>> patientsWithStudies,
        Map<String, List<String>> patientReferredBy, Map<String, String> existingPatientFamilies)
    {
        for (Map.Entry<String, Map<String, String>> patientEntry : patientsWithStudies.entrySet()) {

            if (patientEntry.getValue().size() == 0) {
                // no old family studies data
                continue;
            }

            String patientID = patientEntry.getKey();

            JSONObject probandData = this.getBasicPatientData(this.getPatientXDocument(patientID));

            // check if nodes referred to are ok to link: they don't reference other relatives, don't have
            // a pedigree, are owned by the same user, etc. etc. - if they do, we don't know which node should
            // have the pedigree with links and if is ok to link for privacy reasons, and thus
            // no pedigree will be created
            List<String> relativesToLinkList = this.getOkToAddRelatives(patientID, probandData,
                patientsWithStudies, existingPatientFamilies, patientReferredBy);

            String familyID = existingPatientFamilies.get(patientID);

            if (familyID != null) {
                // the patient already has a pedigree: add all relatives as comments
                this.addRelativesAsComment(patientID, familyID, patientsWithStudies.get(patientID));
            } else {
                // the patient has no existing pedigree
                if (relativesToLinkList.size() == 0) {
                    // maybe no relatives at all, maybe all relatives are not "OK" to add (as described above)
                    // in any case can't create a pedigree, and thus nowhere to place any comments
                    continue;
                }

                Map<String, String> relativesToMention = new HashMap<>(patientsWithStudies.get(patientID));
                // leave only relatives which should be mentioned but not linked
                relativesToMention.keySet().removeAll(relativesToLinkList);

                Map<String, String> relativesToLink = new HashMap<>(patientsWithStudies.get(patientID));
                relativesToLink.keySet().removeAll(relativesToMention.keySet());

                // need to create a new pedigree, and add all "OK" relatives as pedigree nodes and link them,
                // add all others as comments
                this.createNewFamilyAndLinkRelativesForPatient(patientID, probandData,
                    relativesToLink, relativesToMention);
            }
        }
    }

    private boolean saveXWikiDocument(XWikiDocument doc, String documentHistoryComment)
    {
        try {
            this.logger.debug("Migration: saving document {}...", doc.getDocumentReference().getName());
            doc.setComment(documentHistoryComment);
            this.session.clear();
            ((XWikiHibernateStore) getStore()).saveXWikiDoc(doc, this.context, false);
            this.session.flush();
            return true;
        } catch (Exception ex) {
            this.logger.error("Error saving a document: {} - [{}]", ex.getMessage(), ex);
            return false;
        }
    }

    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    private List<String> getOkToAddRelatives(String patientID, JSONObject probandData,
        Map<String, Map<String, String>> patientsWithStudies, Map<String, String> existingPatientFamilies,
        Map<String, List<String>> patientReferredBy)
    {
        List<String> okRelativesList = new LinkedList<>();

        Map<String, String> relatives = patientsWithStudies.get(patientID);
        if (relatives.size() == 0) {
            return okRelativesList;
        }

        int numberOfParents = 0;
        for (Map.Entry<String, String> relativeEntry : relatives.entrySet()) {
            String relationshipType = relativeEntry.getValue();
            if (!R71498PhenoTips2155DataMigration.AUTO_LINKING_SUPPORTED_RELATIVE_TYPES.contains(relationshipType)) {
                // relative is of an unsupported (for auto pedigree creation) relationship type
                continue;
            }
            String relativeID = relativeEntry.getKey();
            if (existingPatientFamilies.containsKey(relativeID)
                && existingPatientFamilies.get(relativeID) != null) {
                // relative has a pedigree
                continue;
            }
            if (patientsWithStudies.containsKey(relativeID)
                && patientsWithStudies.get(relativeID).size() > 0) {
                // relative also has relatives specified in old family studies section
                continue;
            }
            if (patientReferredBy.containsKey(relativeID)
                && patientReferredBy.get(relativeID).size() > 1) {
                // this patient is referred to by more than one patient - can't put into more
                // than one pedigree so don't put into any
                continue;
            }
            if (FAMSTUDIES_CHILD.equals(relationshipType)) {
                numberOfParents++;
                if (numberOfParents > 2) {
                    // can't be a child of more than 2 parents
                    continue;
                }
            }
            String owner = this.getBasicPatientData(this.getPatientXDocument(relativeID)).getString(OWNER);
            if (!StringUtils.equals(owner, probandData.getString(OWNER))) {
                // patients are owned by different users
                continue;
            }

            okRelativesList.add(relativeID);
        }

        return okRelativesList;
    }

    /**
     * In an existing pedigree finds a node linked to the given patient and adds all given relatives to pedigree node
     * comments.
     *
     * @param patientID the id of the patient to look for
     * @param familyID the id of the family with pedigree
     * @param relativesToAdd the list of relatives
     */
    private void addRelativesAsComment(String patientId, String familyId,
        Map<String, String> relativesToAdd)
    {
        try {
            XWikiDocument familyXDocument =
                this.context.getWiki().getDocument(this.resolver.resolve(familyId), this.context);

            BaseObject pedigreeObj = familyXDocument.getXObject(Pedigree.CLASS_REFERENCE);
            if (pedigreeObj == null) {
                this.logger.error("Error updating existing family: no pedigree object for {}", familyId);
                return;
            }

            BaseStringProperty data = (BaseStringProperty) pedigreeObj.get(Pedigree.DATA);
            if (StringUtils.isNotBlank(data.toText())) {
                JSONObject pedigree = new JSONObject(data.toText());

                String pedigreeCommentAddition = this.generatePedigreeCommentForRelatives(relativesToAdd);

                this.familyMigrations.updatePedigreeComment(pedigree,
                    patientId, pedigreeCommentAddition);

                pedigreeObj.set(Pedigree.DATA, pedigree.toString(), this.context);

                String comment = this.getDescription() + " (adding comments to pedigree)";
                if (!this.saveXWikiDocument(familyXDocument, comment)) {
                    this.logger.error("Updated family document was not saved for family {}", familyId);
                }
            }
        } catch (Exception ex) {
            this.logger.error("Error updating pedigree for family {}: {}", familyId, ex.getMessage());
        }
    }

    private String generatePedigreeCommentForRelatives(Map<String, String> relativesToAdd)
    {
        if (relativesToAdd.size() == 0) {
            return null;
        }

        String comment = PEDIGREE_MIGRATED_RELATIVE_COMMENT_HEADER;
        for (Map.Entry<String, String> relativeEntry : relativesToAdd.entrySet()) {
            String relationshipType = relativeEntry.getValue();
            String relativeNamePlusArticle = RELATIVE_ID_TO_NAME.containsKey(relationshipType)
                ? RELATIVE_ID_TO_NAME.get(relationshipType) : relationshipType;
            comment += "\n- this patient is " + relativeNamePlusArticle + " of patient "
                + relativeEntry.getKey();
        }
        return comment;
    }

    /**
     * Auto-generates a pedigree JSON and creates a family with the generated pedigree linking to the patients that are
     * OK to be linked to (and adds others as comments in the pedigree).
     *
     * @param patientID proband patient ID, all relations are relations of this guy to other patients
     * @param relativesToLink patients that can be added to the auto-generated pedigree as nodes
     * @param relativesToMention patients that can't be added as nodes and can only be mentioned in the comments
     */
    private void createNewFamilyAndLinkRelativesForPatient(String probandId, JSONObject probandData,
        Map<String, String> relativesToLink, Map<String, String> relativesToMention)
    {
        try {
            String pedigreeCommentAddition = this.generatePedigreeCommentForRelatives(relativesToMention);

            // Generate pedigree based on relativesToLink
            JSONObject pedigree = this.generatePedigree(probandId, probandData, relativesToLink,
                pedigreeCommentAddition);

            // generate new family document and set links
            XWikiDocument familyXDocument = this.createNewFamily(pedigree, probandId, probandData, relativesToLink);

            String documentUpdateDescription = this.getDescription()
                + " (generated a pedigree using Family Studies data)";

            // try to save the new document, if it was created
            if (familyXDocument == null || !this.saveXWikiDocument(familyXDocument, documentUpdateDescription)) {
                this.logger.error("Failed to create new family document for patient {}", probandId);
            }

            this.logger.info("Created new family {} for patient {}",
                familyXDocument.getDocumentReference().getName(), probandId);

        } catch (Exception ex) {
            this.logger.error("Error creating new family: [{}] [{}]", ex.getMessage(), ex);
        }
    }

    /**
     * Generates a pedigree JSON. Only relatives from the list ["parent", "child", "sibling", "twin"] may be auto-added
     * to the generated pedigree.
     */
    private JSONObject generatePedigree(String patientID, JSONObject probandData,
        Map<String, String> relativesToLink, String probandComment)
    {
        // split relatives into groups for easier handling
        List<String> twinIDs = new LinkedList<>();
        List<String> siblingIDs = new LinkedList<>();
        List<String> childrenIDs = new LinkedList<>();
        List<String> parentIDs = new LinkedList<>();
        for (Map.Entry<String, String> relativeEntry : relativesToLink.entrySet()) {
            String relationshipType = relativeEntry.getValue();
            if (FAMSTUDIES_PARENT.equals(relationshipType)) {
                childrenIDs.add(relativeEntry.getKey());
            } else if (FAMSTUDIES_CHILD.equals(relationshipType)) {
                parentIDs.add(relativeEntry.getKey());
            } else if (FAMSTUDIES_SIBLING.equals(relationshipType)) {
                siblingIDs.add(relativeEntry.getKey());
            } else if (FAMSTUDIES_TWIN.equals(relationshipType)) {
                twinIDs.add(relativeEntry.getKey());
            }
        }

        return this.generatePedigree(patientID, probandData, probandComment,
            twinIDs, siblingIDs, childrenIDs, parentIDs);
    }

    private JSONObject generatePedigree(String patientID, JSONObject probandData, String probandComment,
        List<String> twinIDs, List<String> siblingIDs, List<String> childrenIDs, List<String> parentIDs)
    {
        JSONArray nodeArray = new JSONArray();

        int probandPedigreeId = 1;
        int firtstFreeId = probandPedigreeId + 1;

        String probandSex = probandData.getString(GENDER);

        JSONObject probandNode = this.generateProbandNode(probandPedigreeId, patientID,
            probandComment, probandSex, twinIDs);

        // if there are parents OR sibling/twins - create parent nodes
        if (parentIDs.size() > 0 || siblingIDs.size() > 0 || twinIDs.size() > 0) {
            // create parents
            JSONObject fatherNode = new JSONObject("{\"id\": 2}");
            JSONObject motherNode = new JSONObject("{\"id\": 3}");
            firtstFreeId = 4;
            this.fillParentDetails(parentIDs, fatherNode, motherNode);
            // set proband parents
            probandNode.put(JSON_FATHER, "2");
            probandNode.put(JSON_MOTHER, "3");
            nodeArray.put(fatherNode);
            nodeArray.put(motherNode);
        }
        nodeArray.put(probandNode);

        // create siblings & twins
        firtstFreeId = this.addSiblings(firtstFreeId, nodeArray, siblingIDs, false, 2, 3);
        firtstFreeId = this.addSiblings(firtstFreeId, nodeArray, twinIDs, true, 2, 3);

        // if there are children - create children and a spouse
        if (childrenIDs.size() > 0) {
            // create spouse
            int partnerId = firtstFreeId++;
            nodeArray.put(this.generatePartnerNode(partnerId, probandSex));

            int fatherID = (FEMALE.equalsIgnoreCase(probandSex)) ? partnerId : probandPedigreeId;
            int motherID = (fatherID == probandPedigreeId) ? partnerId : probandPedigreeId;

            // create children
            this.addSiblings(firtstFreeId, nodeArray, childrenIDs, false, fatherID, motherID);
        }

        JSONObject result = new JSONObject();
        result.put("data", nodeArray);
        this.logger.debug("Created pedigree JSON: [{}]", result.toString());
        return result;
    }

    /**
     * Generates JSON representation of the proband node in pedigree SimpleJSON format.
     */
    private JSONObject generateProbandNode(int probandPedigreeId, String patientID, String probandComment,
        String probandSex, List<String> twinIDs)
    {
        JSONObject probandNode = new JSONObject();
        probandNode.put(JSON_ID, probandPedigreeId);
        probandNode.put(JSON_PROBAND, true);
        probandNode.put(JSON_PHENOTIPSID, patientID);
        if (!StringUtils.isEmpty(probandComment)) {
            probandNode.put(JSON_COMMENTS, probandComment);
        }
        if (!StringUtils.isEmpty(probandSex)) {
            probandNode.put(JSON_SEX, probandSex);
        }
        if (twinIDs.size() > 0) {
            probandNode.put(JSON_TWINGROUP, 0);
        }
        return probandNode;
    }

    /**
     * Figure out which of the parents is the father (if any), which is the mother (if any) and updates node properties
     * accordingly. In case data can be interpreted in multiple ways sets first parent to be the father.
     *
     * @param parentIDs IDs of the parents, may be empty or have 1 or 2 parents in the list
     * @param fatherNode father node properties as stored in SimpleJSON pedigree format (to be modified)
     * @param motherNode mother node properties as stored in SimpleJSON pedigree format (to be modified)
     */
    private void fillParentDetails(List<String> parentIDs, JSONObject fatherNode, JSONObject motherNode)
    {
        if (parentIDs.size() == 0) {
            return;
        }

        String firstParentSex =
            this.getBasicPatientData(this.getPatientXDocument(parentIDs.get(0))).getString(GENDER);

        if (parentIDs.size() == 1) {
            if (FEMALE.equals(firstParentSex)) {
                this.setSimpleJSONNodeData(motherNode, firstParentSex, parentIDs.get(0));
            } else {
                this.setSimpleJSONNodeData(fatherNode, firstParentSex, parentIDs.get(0));
            }
        } else {
            String secondParentSex =
                this.getBasicPatientData(this.getPatientXDocument(parentIDs.get(1))).getString(GENDER);
            if (FEMALE.equals(firstParentSex) || MALE.equals(secondParentSex)) {
                this.setSimpleJSONNodeData(motherNode, firstParentSex, parentIDs.get(0));
                this.setSimpleJSONNodeData(fatherNode, secondParentSex, parentIDs.get(1));
            } else {
                this.setSimpleJSONNodeData(motherNode, secondParentSex, parentIDs.get(1));
                this.setSimpleJSONNodeData(fatherNode, firstParentSex, parentIDs.get(0));
            }
        }
    }

    private JSONObject generatePartnerNode(int pedigreeId, String otherPartnerGender)
    {
        JSONObject result = new JSONObject();
        result.put(JSON_ID, pedigreeId);
        // auto-guess spouse gender if proband is either "M" or "F"
        if (MALE.equals(otherPartnerGender)) {
            result.put(JSON_SEX, FEMALE);
        } else if (FEMALE.equals(otherPartnerGender)) {
            result.put(JSON_SEX, MALE);
        }
        return result;
    }

    private void setSimpleJSONNodeData(JSONObject nodeData, String gender, String phenotipsId)
    {
        if (!StringUtils.isEmpty(gender)) {
            nodeData.put(JSON_SEX, gender);
        }
        nodeData.put(JSON_PHENOTIPSID, phenotipsId);
    }

    private int addSiblings(int firtstFreeId, JSONArray nodeArray, List<String> siblingIDs,
        boolean twins, int fatherId, int motherId)
    {
        int freeId = firtstFreeId;
        for (String siblingID : siblingIDs) {
            JSONObject childNode = new JSONObject();
            childNode.put(JSON_ID, (freeId++));
            childNode.put(JSON_FATHER, fatherId);
            childNode.put(JSON_MOTHER, motherId);
            childNode.put(JSON_PHENOTIPSID, siblingID);
            if (twins) {
                childNode.put(JSON_TWINGROUP, 0);
            }
            nodeArray.put(childNode);
        }
        return freeId;
    }

    /**
     * Creates anew family document, sets th epedigree to the given one and links given patients ot the family and
     * family to the patients. Only relatives from the list ["parent", "child", "sibling", "twin"] may be auto-added to
     * the generated pedigree.
     */
    private XWikiDocument createNewFamily(JSONObject pedigree, String probandPatientId, JSONObject probandData,
        Map<String, String> relativesToLink)
    {
        // TODO: Generate the correct image as well? Or may use one of the predefined images.
        // For now using the same svg with explanation text for all the famlies.
        String pedigreeImage =
            ("<svg xmlns=`http://www.w3.org/2000/svg` xmlns:xlink=`http://www.w3.org/1999/xlink` "
                + "preserveAspectRatio=`xMidYMid` viewBox=`0 0 480 480`>"
                + "<text fill=`black` x=`25` y=`78` style=`font-family: Arial; font-size: 38px;`>"
                + "This pedigree is auto-"
                + "<tspan x=`25` dy=`1.5em`>generated based on the </tspan>"
                + "<tspan x=`25` dy=`1.5em`>data from the deprecated </tspan>"
                + "<tspan x=`25` dy=`1.5em`>Family Studies section.</tspan></text>"
                + "<text fill=`black` x=`25` y=`360` style=`font-family: Arial; font-size: 38px;`>"
                + "Please open pedigree "
                + "<tspan x=`25` dy=`1.5em`>editor to see the pedigree.</tspan></text></svg>").replace('`', '"');

        this.logger.debug("Creating new family for patient {}.", probandPatientId);

        XWikiDocument newFamilyXDocument =
            this.familyMigrations.createFamilyDocument(this.getPatientXDocument(probandPatientId),
                pedigree, pedigreeImage, this.context, this.session);

        if (newFamilyXDocument != null) {
            Set<String> allPatients = new HashSet<>(relativesToLink.keySet());
            allPatients.add(probandPatientId);
            this.setAllFamilyRefs(allPatients, newFamilyXDocument, probandData);
        }
        return newFamilyXDocument;
    }

    /**
     * Set family references for all relatives.
     */
    private void setAllFamilyRefs(Set<String> familyMembers, XWikiDocument familyXDocument, JSONObject probandData)
    {
        String familyID = familyXDocument.getDocumentReference().getName();

        try {
            String familyDocumentRef = familyXDocument.getDocumentReference().toString();

            List<String> membersRefsList = new LinkedList<>();

            for (String memberId : familyMembers) {
                membersRefsList.add(memberId);
                // set the family reference to a relative doc
                XWikiDocument patientDoc = this.getPatientXDocument(memberId);
                if (!this.familyMigrations.setFamilyReference(patientDoc, familyDocumentRef, this.context)
                    || !this.saveXWikiDocument(patientDoc, this.getDescription())) {
                    this.logger.error("Failed to link patient {} to family {}", memberId, familyID);
                }
            }

            BaseObject familyObject = familyXDocument.getXObject(Family.CLASS_REFERENCE);
            if (familyObject == null) {
                familyObject = familyXDocument.newXObject(Family.CLASS_REFERENCE, this.context);
            }
            familyObject.setStringListValue("members", membersRefsList);

            String probandLastName = probandData.getString(LASTNAME);
            if (!StringUtils.isEmpty(probandLastName)) {
                familyObject.set("external_id", probandLastName, this.context);
            }
        } catch (Exception ex) {
            this.logger.error("Failed to link family {} to its members [{}]", familyID, familyMembers);
        }
    }

    /**
     * Gets the family that patient belongs to, or null if there is no family.
     */
    private String getPatientsFamily(XWikiDocument patientXDocument)
    {
        try {
            BaseObject pointer = patientXDocument.getXObject(Family.REFERENCE_CLASS_REFERENCE);
            if (pointer == null) {
                return null;
            }
            String famReference = pointer.getStringValue(REFERENCE_PROPERTY_NAME);
            if (!StringUtils.isBlank(famReference)) {
                return famReference;
            }
        } catch (Exception ex) {
            this.logger.info("Failed to retrieve the family of patient [{}]: {}",
                patientXDocument.getDocumentReference(), ex.getMessage());
        }
        return null;
    }

    /**
     * Gets the map of patient relative type mapped to relative XWiki document, or null.
     */
    private Map<String, String> getRelatives(XWikiDocument patientXDocument)
    {
        Map<String, String> result = new HashMap<>();

        try {
            List<BaseObject> relativeXObjects = patientXDocument.getXObjects(this.relativeClassReference);
            if (relativeXObjects == null || relativeXObjects.isEmpty()) {
                return result;
            }

            for (BaseObject object : relativeXObjects) {
                if (object == null) {
                    continue;
                }

                String relativeType = object.getStringValue(RELATIVE_PROPERTY_NAME);
                String relativeOf = object.getStringValue(RELATIVEOF_PROPERTY_NAME);

                if (StringUtils.isBlank(relativeType) || StringUtils.isBlank(relativeOf)) {
                    continue;
                }
                XWikiDocument relativeXDocument = getRelativeDoc(relativeOf, this.context.getWiki());
                if (relativeXDocument == null) {
                    continue;
                }
                result.put(relativeXDocument.getDocumentReference().getName(), relativeType);
            }
        } catch (Exception ex) {
            this.logger.info("Failed to get the relatives of patient [{}]: {}", patientXDocument.getDocumentReference(),
                ex.getMessage());
        }

        return result;
    }

    /**
     * Gets the relative XWiki document or null.
     */
    private XWikiDocument getRelativeDoc(String relativeDoc, XWiki xwiki)
    {
        try {
            Query rq = this.session.createQuery("select distinct o.name from BaseObject o, StringProperty p where "
                + "o.className = :patclass and p.id.id = o.id and p.id.name = 'external_id' and p.value = :reldoc");
            rq.setString("patclass", this.serializer.serialize(Patient.CLASS_REFERENCE));
            rq.setString("reldoc", relativeDoc);

            @SuppressWarnings("unchecked")
            List<String> relativeDocuments = rq.list();

            if (relativeDocuments.isEmpty()) {
                return null;
            }

            String relativeDocName = relativeDocuments.get(0);
            XWikiDocument relativeXDocument =
                xwiki.getDocument(this.resolver.resolve(relativeDocName), this.context);
            return relativeXDocument;
        } catch (Exception ex) {
            return null;
        }
    }

    private XWikiDocument getPatientXDocument(String patientId)
    {
        try {
            return this.context.getWiki().getDocument(this.resolver.resolve(patientId), this.context);
        } catch (Exception ex) {
            return null;
        }
    }

    private JSONObject getBasicPatientData(XWikiDocument patientDoc)
    {
        JSONObject result = new JSONObject();
        result.put(GENDER, "U");
        result.put(LASTNAME, "");
        result.put(OWNER, "");

        if (patientDoc != null) {
            BaseObject data = patientDoc.getXObject(Patient.CLASS_REFERENCE);
            if (data != null) {
                result.put(GENDER, data.getStringValue(PATIENT_DOCUMENT_GENDER_FIELD));
                result.put(LASTNAME, data.getStringValue(PATIENT_DOCUMENT_LASTNAME_FIELD));
            }
            result.put(OWNER, this.familyMigrations.getOwner(patientDoc));
        }
        return result;
    }
}
