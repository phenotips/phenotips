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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
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
 * The migrator can only auto-generate a family/pedigree if there is no existing pedigree, all
 * members of the family are owned by the same user, and all members of the family can be included
 * in restricted pedigree shape (or a subset of the shape) as described below:
 *
 *      < >--*--< >     < >--*--< >
 *           |               |
 *     +-----+-----+         |
 *     |     |     |         |
 *    < >  <...>  <R>---*---<P>
 *                      |
 *              +---+---+------------+---...
 *              |   |      |       /   \
 *             < > <...> <...>  <...> <...>
 *
 *  - R denotes "root" patient (not necessarily the proband)
 *  - R may have at most one partner, P
 *  - at most 3 generations: root patient, root parents and root children
 *  - at most one partner for the root patient (P)
 *  - R may have some siblings or twins, none of which should have a child
 *  - P's parents are only related to P (and possibly as grandparents to P+R's children)
 *  - R's parents are only related to R and R's siblings (and possibly as grandparents to R+P's children)
 *  - all other nodes are children of R and P, can't be related to any other node other than
 *     R, P or other children as twins (or their parents as grandchilren)
 *  - "supported" relations are PARENT/CHILD/TWIN/SIBLING (because all other relationships
 *     can not be converted to a pedigree in a uniform way, e.g. for "uncle" relationships it is not clear
 *     if the uncle is on the maternal or paternal side)
 *  - there may be other patients which are only linked by non-supported relation types
 *     (e.g a patient which is only an uncle of someone - but not a parent or child or sibling of anyone).
 *     Those patients are added as comments to the generated pedigree.
 *  - a pedigree must have the root patient, and at least one more patient, which can be either a child,
 *     a parent or a partner. It may also have more relatives, as long as they fit the shape outlined above.
 *
 * It is assumed that the proband is the patient in the family with the earliest PT document creation time
 * (not necessarily the "root" patient in the pedigree above).
 *
 * Otherwise, if there already exists a pedigree for the node which has an old family studies relationship,
 * that relationship is added to that node's pedigree comment.
 *
 * Additionally and independently of the above, if a node in an existing pedigree has an external ID of an
 * existing PhenoTips record, that patient is linked to the pedigree node (unless that patient is a member
 * of another family or is also referenced from another pedigree).
 *
 * @version $Id$
 * @since 1.3M4
 */
@Component
@Named("R71498-PT-2155")
@Singleton
public class R71498PhenoTips2155DataMigration extends AbstractHibernateDataMigration
    implements HibernateCallback<Object>
{
    private static final String RELATIVE_PROPERTY_NAME = "relative_type";

    private static final String RELATIVEOF_PROPERTY_NAME = "relative_of";

    private static final String REFERENCE_PROPERTY_NAME = "reference";

    private static final String PEDIGREE_MIGRATED_RELATIVE_COMMENT_HEADER =
        "Data migrated from Family Studies:";

    private static final String SEX_MALE = "M";

    private static final String SEX_FEMALE = "F";

    private static final String SEX_UNKNOWN = "U";

    private static final String JSON_NODE_ID = "id";

    private static final String JSON_SEX = "sex";

    private static final String JSON_EXTERNALID = "externalId";

    private static final String JSON_PHENOTIPSID = "phenotipsId";

    private static final String JSON_PROBAND = "proband";

    private static final String JSON_COMMENTS = "comments";

    private static final String JSON_TWINGROUP = "twinGroup";

    private static final String JSON_MOTHER = "mother";

    private static final String JSON_FATHER = "father";

    private static final String PATIENT_DOCUMENT_GENDER_FIELD = "gender";

    private static final String PATIENT_DOCUMENT_EXTERNALID_FIELD = "external_id";

    private static final String FAMSTUDIES_PARENT = "parent";

    private static final String FAMSTUDIES_CHILD = "child";

    private static final String FAMSTUDIES_SIBLING = "sibling";

    private static final String FAMSTUDIES_TWIN = "twin";

    private static final String FAMSTUDIES_COUSIN = "cousin";

    private static final String FAMSTUDIES_AUNTUNCLE = "aunt_uncle";

    private static final String FAMSTUDIES_NIECENEPHEW = "niece_nephew";

    private static final String FAMSTUDIES_GRANDPARENT = "grandparent";

    private static final String FAMSTUDIES_GRANDCHILD = "grandchild";

    // derived from child-parent relationships; used for internal consistency checks
    private static final String INTERNAL_PARTNER = "partner";

    private static final Map<String, String> INVERSE_RELATIONSHIP =
            Collections.unmodifiableMap(MapUtils.putAll(new HashMap<String, String>(), new String[][] {
                { FAMSTUDIES_PARENT, FAMSTUDIES_CHILD },
                { FAMSTUDIES_CHILD, FAMSTUDIES_PARENT },
                { FAMSTUDIES_SIBLING, FAMSTUDIES_SIBLING },
                { FAMSTUDIES_TWIN, FAMSTUDIES_TWIN },
                { FAMSTUDIES_COUSIN, FAMSTUDIES_COUSIN },
                { FAMSTUDIES_AUNTUNCLE, FAMSTUDIES_NIECENEPHEW },
                { FAMSTUDIES_NIECENEPHEW, FAMSTUDIES_AUNTUNCLE },
                { FAMSTUDIES_GRANDPARENT, FAMSTUDIES_GRANDCHILD },
                { FAMSTUDIES_GRANDCHILD, FAMSTUDIES_GRANDPARENT },
                { INTERNAL_PARTNER, INTERNAL_PARTNER }
            }));

    private static final Map<String, String> RELATIVE_ID_TO_NAME =
        Collections.unmodifiableMap(MapUtils.putAll(new HashMap<String, String>(), new String[][] {
            { FAMSTUDIES_PARENT, "a parent" },
            { FAMSTUDIES_CHILD, "a child" },
            { FAMSTUDIES_SIBLING, "a sibling" },
            { FAMSTUDIES_TWIN, "a twin" },
            { FAMSTUDIES_COUSIN, "a cousin" },
            { FAMSTUDIES_AUNTUNCLE, "an aunt/uncle" },
            { FAMSTUDIES_NIECENEPHEW, "a niece/nephew" },
            { FAMSTUDIES_GRANDPARENT, "a grandparent" },
            { FAMSTUDIES_GRANDCHILD, "a grandchild" }
        }));

    private static final Set<String> AUTO_LINKING_SUPPORTED_RELATIVE_TYPES = new HashSet<>(
            Arrays.asList(FAMSTUDIES_PARENT, FAMSTUDIES_CHILD, FAMSTUDIES_SIBLING, FAMSTUDIES_TWIN));

    // derived relationships:
    //                      relation of A to B
    //    +---------+-------------+------------+------------+------------
    //    |         | PARENT      | CHILD      | SIBLING    | TWIN
    //    +---------+-------------+------------+------------+------------
    // B  | PARENT  | GRANDPARENT | SIBLING    | AUNT/UNCLE | AUNT/UNCLE
    // to | CHILD   | PARTNER     | GRANDCHILD | CHILD      | CHILD
    // C  | SIBLING | PARENT      | NIECE/NEPH | SIBLING    | SIBLING
    //    | TWIN    | PARENT      | NIECE/NEPH | SIBLING    | TWIN
    //          (cell value == derived/expected relation of A to C)
    private static final String[][] DERIVED_RELATIONS_TABLE = new String[][] {
            { FAMSTUDIES_GRANDPARENT, FAMSTUDIES_SIBLING,     FAMSTUDIES_AUNTUNCLE, FAMSTUDIES_AUNTUNCLE },
            { INTERNAL_PARTNER,       FAMSTUDIES_GRANDCHILD,  FAMSTUDIES_CHILD,     FAMSTUDIES_CHILD },
            { FAMSTUDIES_PARENT,      FAMSTUDIES_NIECENEPHEW, FAMSTUDIES_SIBLING,   FAMSTUDIES_SIBLING },
            { FAMSTUDIES_PARENT,      FAMSTUDIES_NIECENEPHEW, FAMSTUDIES_SIBLING,   FAMSTUDIES_TWIN }
        };
    // index into both dimensions of the 2D array above
    private static final Map<String, Integer> DERIVED_RELATIONS_INDEX =
        Collections.unmodifiableMap(MapUtils.putAll(new HashMap<String, Integer>(), new Object[][] {
            { FAMSTUDIES_PARENT, 0 },
            { FAMSTUDIES_CHILD, 1 },
            { FAMSTUDIES_SIBLING, 2 },
            { FAMSTUDIES_TWIN, 3 }
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

    // represents raw unprocessed data about a patient and patient's family studies, as read from the database
    private class MigrationPatient
    {
        private String familyID;

        private String phenotipsId;

        private String externalId;

        // gender is needed to correctly set the partner's gender in pedigree
        private String gender = SEX_UNKNOWN;

        private String owner;

        // creation date is needed to decide which patient is the proband
        private Date creationDate;

        // map of { anotherPatientExternalID -> relation type of THIS patient to that patient }
        // (as specified in this patient's family study)
        private Map<String, String> refersTo;

        // map of { anotherPatientPhenotipsID -> relation type of THAT patient to this patient }
        // (as specified in the other patient's family study)
        private Map<String, String> referredBy = new HashMap<>();

        MigrationPatient(XWikiDocument patientXDocument)
        {
            this.setBasicPatientData(patientXDocument);

            this.familyID = this.getPatientsFamily(patientXDocument);

            this.refersTo = this.getRelatives(patientXDocument);
        }

        private void setBasicPatientData(XWikiDocument patientDoc)
        {
            try {
                this.phenotipsId = patientDoc.getDocumentReference().getName();
                this.owner = familyMigrations.getOwner(patientDoc);
                this.creationDate = patientDoc.getCreationDate();

                BaseObject data = patientDoc.getXObject(Patient.CLASS_REFERENCE);
                if (data != null) {
                    this.gender = data.getStringValue(PATIENT_DOCUMENT_GENDER_FIELD).toUpperCase();
                    this.externalId = data.getStringValue(PATIENT_DOCUMENT_EXTERNALID_FIELD);
                }
            } catch (Exception ex) {
                this.phenotipsId = "ERROR";
                logger.error("Failed to get data from patient [{}] document: {}", patientDoc.getDocumentReference(),
                    ex.getMessage());
            }
        }

        // Gets the family that patient belongs to, or null if there is no family
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
                logger.info("Failed to retrieve the family of patient [{}]: {}",
                    patientXDocument.getDocumentReference(), ex.getMessage(), ex);
            }
            return null;
        }

        // returns the mapping of relatives to relation types, where relative is given by external Id
        // (note: external Id may not be a valid id)
        private Map<String, String> getRelatives(XWikiDocument patientXDocument)
        {
            // externalID -> relationType
            Map<String, String> result = new HashMap<>();

            try {
                List<BaseObject> relativeXObjects = patientXDocument.getXObjects(relativeClassReference);
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
                    if (relativeOf.equals(this.getExternalId())) {
                        logger.info("ignoring self-reference for patient {}", this.getPhenotipsId());
                        continue;
                    }
                    result.put(relativeOf, relativeType);
                }
            } catch (Exception ex) {
                logger.info("Failed to get the relatives of patient [{}]: {}", patientXDocument.getDocumentReference(),
                    ex.getMessage());
            }
            return result;
        }

        public String getFamilyId()
        {
            return this.familyID;
        }

        public String getPhenotipsId()
        {
            return this.phenotipsId;
        }

        public String getExternalId()
        {
            return this.externalId;
        }

        public JSONObject getPatientSimpleJSON()
        {
            JSONObject patientJSON = new JSONObject();
            patientJSON.put(JSON_PHENOTIPSID, this.getPhenotipsId());
            patientJSON.put(JSON_SEX, this.getGender());
            if (this.getExternalId() != null) {
                patientJSON.put(JSON_EXTERNALID, this.getExternalId());
            }
            return patientJSON;
        }

        public String getGender()
        {
            return this.gender;
        }

        public String getSuggestedPartnerGender()
        {
            if (SEX_MALE.equals(this.gender)) {
                return SEX_FEMALE;
            }
            if (SEX_FEMALE.equals(this.gender)) {
                return SEX_MALE;
            }
            return SEX_UNKNOWN;
        }

        public Date getCreationDate()
        {
            return this.creationDate;
        }

        public String getOwner()
        {
            return this.owner;
        }

        public Map<String, String> getRefersTo()
        {
            return this.refersTo;
        }

        public Map<String, String> getReferredBy()
        {
            return this.referredBy;
        }

        public void addReferenceBy(String referredPatientId, String referredRelationship)
        {
            this.referredBy.put(referredPatientId, referredRelationship);
        }
    }

    // map of {patientID -> all_relevant_family_studies_data}
    private Map<String, MigrationPatient> patientData;

    // map of {externalID -> patientID}
    private Map<String, String> externalToPTIds;

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
        // need to to reinitiqlizes the in-memory representation of the patients since the same class
        // with the same state is invoked for different virtual wikis (if present)
        this.patientData = new HashMap<>();
        this.externalToPTIds = new HashMap<>();

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

                MigrationPatient data = new MigrationPatient(patientXDocument);

                String patientId = data.getPhenotipsId();

                this.patientData.put(patientId, data);

                if (StringUtils.isNotBlank(data.getExternalId())) {
                    externalToPTIds.put(data.getExternalId(), patientId);
                }

            }

            // 2) fill in the referred-by part (to simplify further processing) and
            //    remove self-references since those can screw up other parts of the code (and those are possible)
            for (Map.Entry<String, MigrationPatient> entry : this.patientData.entrySet()) {
                String patientId = entry.getKey();
                MigrationPatient data = entry.getValue();

                for (Map.Entry<String, String> ref : data.getRefersTo().entrySet()) {
                    String referredId = ref.getKey();
                    String referredRelation = ref.getValue();
                    MigrationPatient referencedData = this.patientData.get(this.externalToPTIds.get(referredId));
                    if (referencedData != null) {
                        referencedData.addReferenceBy(patientId, referredRelation);
                    }
                }
            }

            // 3) go patient by patient, find "proband" patients, and create new families using family studies
            // data (when possible) or add comments to existing pedigrees using family studies data
            // (when a pedigree is already present)
            this.processData();

        } catch (Exception ex) {
            this.logger.error("Family Studies migration exception: [{}]", ex.getMessage(), ex);
        } finally {
            this.context.getWiki().flushCache(this.context);
        }
        return null;
    }

    /**
     * This class represents a set of related patients, e.g. a set where patient A is parent of patient B,
     * patient B is uncle of patient C, and patient D is grandchild of patient C.
     *
     * Each patient in the set is guaranteed to relate to at least one other patient in the set, but some
     * of the patients may not be explicitly mutually related (e.g. patients D and A in the example above).
     *
     * The class provides machinery to fill in the missing links that can be derived based on exisitng links
     * (e.g. "a parent of my sibling is also my parent") as well as a helper class which tries to fit
     * the patients in the set into the supported pedigree scheme.
     */
    private class RelatedSet
    {
        /**
         * Represents a generic reference to a patient, either with or without a PhenoTips record.
         *
         * It is necessary because some family studies links reference non-existing (in PhenoTips) patients
         * by their external ID and at the same time some patients that reference other patients may not have
         * an external ID themself, so neither PhenoTips id alone nor external Id alone can be used to identify
         * any patient that can be encountered.
         */
        private class PatientReference
        {
            public final String phenotipsID;
            public final String externalID;
            private final int hashCode;

            PatientReference(String phenotipsID, String externalID)
            {
                this.phenotipsID = phenotipsID;
                this.externalID = StringUtils.isNotBlank(externalID) ? externalID : null;
                this.hashCode = ((phenotipsID == null ? "" : phenotipsID)
                        + "__" + (externalID == null ? "" : externalID)).hashCode();
            }

            // creates a "fake" patient, e.g. a required (by pedigree) but missing parent
            PatientReference(String fakeID)
            {
                this.phenotipsID = null;
                this.externalID = null;
                this.hashCode = fakeID.hashCode();
            }

            // represents a patient not present in family studies and PT, but added because
            // pedigree editor requires it (e.g. if two patients are siblings with no indicated parents,
            // since pedigree editor requires them to have a parent to be able to represent that)
            public boolean isVirtual()
            {
                return (this.phenotipsID == null) && (this.externalID == null);
            }

            @Override
            public String toString()
            {
                return phenotipsID == null ? (externalID == null ? "<virtual>" : externalID) : phenotipsID;
            }

            @Override
            public int hashCode()
            {
                return this.hashCode;
            }

            @Override
            public boolean equals(Object obj)
            {
                return (obj == null) ? false : (hashCode == ((PatientReference) obj).hashCode);
            }
        }

        /**
         * This class represents a pedigree in the "supported shape", and is used to see if the set
         * of patients that need to be added to the pedigree can fit the shape (assuming the given patient is the
         * "root" patient. The idea is that the outside loop tries different patients as "roots" until either
         * all other patients fit such a pedigree, or it is inferred a pedigree can't be created).
         *
         * (currently) the migrator can only auto-generate a family/pedigree if all members in the family
         * can be included in a restricted pedigree shape (or a subset of the shape) as outlined below:
         *
         *      < >--*--< >     < >--*--< >
         *           |               |
         *     +-----+-----+         |
         *     |     |     |         |
         *    < >  <...>  <R>---*---<P>
         *                      |
         *              +---+---+------------+---...
         *              |   |      |       /   \
         *             < > <...> <...>  <...> <...>
         *
         *  - R denotes "root" patient (not necessarily the proband)
         *  - R may have at most one partner, P
         *  - at most 3 generations: root patient, root parents and root children
         *  - at most one partner for the root patient (P)
         *  - R may have some siblings or twins, none of which should have a child
         *  - P's parents are only related to P (and possibly as grandparents to P+R's children)
         *  - R's parents are only related to R and R's siblings (and possibly as grandparents to R+P's children)
         *  - all other nodes are children of R and P, can't be related to any other node other than
         *     R, P or other children as twins (or their parents as grandchilren)
         *  - "supported" relations are PARENT/CHILD/TWIN/SIBLING (because all other relationships
         *     can not be converted to a pedigree in a uniform way, e.g. for "uncle" relationships it is not clear
         *     if the uncle is on the maternal or paternal side)
         *  - there may be other patients which are only linked by non-supported relation types
         *     (e.g a patient which is only an uncle of someone - but not a parent or child or sibling of anyone).
         *     Those patients are added as comments to the generated pedigree.
         *  - a pedigree must have the root patient, and at least one more patient, which can be either a child,
         *     a parent or a partner. It may also have more relatives, as long as they fit the shape outlined above.
         */
        public class Pedigree
        {
            private PatientReference root;
            private PatientReference partner;
            private Set<PatientReference> rootParents = new HashSet<>();
            private Set<PatientReference> rootSiblings = new HashSet<>();
            private Set<PatientReference> partnerParents = new HashSet<>();
            private Set<PatientReference> children = new HashSet<>();
            private Map<PatientReference, Integer> patientToNodeId = new HashMap<>();
            private Map<PatientReference, PatientReference> partners = new HashMap<>();

            // can be any of the above
            private PatientReference proband;

            Pedigree(PatientReference rootPatient, PatientReference proband)
            {
                logger.debug("Pedigree with root patient {}, proband {}", rootPatient, proband);

                this.root = rootPatient;
                this.addPatientNode(this.root);

                this.proband = proband;

                // find if there is a partner. If more than one found keep as null
                // note: need to know partner from the start, as otherwise partner parents will be
                //       rejected as unrelated if processed before the partner
                Set<PatientReference> rootPartners = getAllRelativesOfType(root, INTERNAL_PARTNER);
                if (rootPartners.size() == 1) {
                    this.setPartner(rootPartners.iterator().next());
                }
            }

            private void addPatientNode(PatientReference patient)
            {
                this.patientToNodeId.put(patient, this.patientToNodeId.size());
            }

            private void addPartners(PatientReference patient1, PatientReference patient2)
            {
                this.partners.put(patient1, patient2);
                this.partners.put(patient2, patient1);
            }

            private void setPartner(PatientReference patient)
            {
                this.partner = patient;
                this.addPatientNode(this.partner);
                this.addPartners(this.root, this.partner);
            }

            private void addRootParent(PatientReference patient)
            {
                if (this.rootParents.size() == 1) {
                    // this is the partner of the only other root parent
                    this.addPartners(this.rootParents.iterator().next(), patient);
                }
                this.rootParents.add(patient);
                this.addPatientNode(patient);
            }

            // returns false if can't add because the patient does not fit into the supported pedigree structure
            // a patient may still have other relationships, that is checked later after all patients are sorted into
            // grandparents-parents-children (e.g. before we know the children or 2nd set of grandparents we don't
            // know if a "grandparent" relationship between this and that is OK to have)
            public boolean addPatient(PatientReference patient)
            {
                String rootToPatient = getRelation(root, patient);
                if (rootToPatient == null && partner != null) {
                    // if it fits the scheme, this patient can only be a parent of the partner
                    String partnerToPatient = getRelation(partner, patient);
                    if (FAMSTUDIES_CHILD.equals(partnerToPatient) && partnerParents.size() < 2) {
                        if (partnerParents.size() == 1) {
                            // this is the partner of the only other partner parent
                            this.addPartners(partnerParents.iterator().next(), patient);
                        }
                        partnerParents.add(patient);
                        this.addPatientNode(patient);
                        return true;
                    }
                } else if (FAMSTUDIES_CHILD.equals(rootToPatient)) {
                    if (rootParents.size() < 2) {
                        this.addRootParent(patient);
                        return true;
                    }
                } else if (FAMSTUDIES_PARENT.equals(rootToPatient)) {
                    children.add(patient);
                    this.addPatientNode(patient);
                    return true;
                } else if (FAMSTUDIES_SIBLING.equals(rootToPatient) || FAMSTUDIES_TWIN.equals(rootToPatient)) {
                    rootSiblings.add(patient);
                    this.addPatientNode(patient);
                    return true;
                } else if (INTERNAL_PARTNER.equals(rootToPatient) && patient.equals(partner)) {
                    return true;
                }
                logger.debug("Can't add patient {} to pedigree", patient);
                return false;
            }

            // checks that in addition to the relationships that defined the pedigree structure
            // there are no other relationshipo\s between all the persons in the pedigree.
            // there can still be relationships of "unsupported" type to other nodes, e.g. cousins/uncles,
            // but no one in the pedigree can be an uncle to anyone else in the pedigree (except root siblings
            // being uncles to root's children)
            public boolean noExtraRelationships()
            {
                // All CHILD/TWIN/PARENT/CHILD should be taken care of either via conflict detection
                // or via inability to add to pedigree due to a "no match for the structure"

                //  root/partner:
                //  - GRANDPARENT/GRANDCHILD: none
                //  - UNCLE/COUSIN/NIECE: only nodes not in pedigree
                if (!this.checkNoRelationsOfType(Arrays.asList(root, partner),
                        Arrays.asList(FAMSTUDIES_GRANDPARENT, FAMSTUDIES_GRANDCHILD))
                    || !this.checkRelationsOnlyTo(Arrays.asList(root, partner),
                        Arrays.asList(FAMSTUDIES_NIECENEPHEW, FAMSTUDIES_AUNTUNCLE, FAMSTUDIES_COUSIN), null, true)) {
                    return false;
                }

                // rootSiblings:
                // - GRANDPARENT/GRANDCHILD: none
                // - COUSIN/NIECE: only nodes not in pedigree
                // - UNCLE: only to root's children or nodes not in pedigree
                if (!this.checkNoRelationsOfType(rootSiblings,
                        Arrays.asList(FAMSTUDIES_GRANDPARENT, FAMSTUDIES_GRANDCHILD))
                    || !this.checkRelationsOnlyTo(Arrays.asList(root, partner),
                        Arrays.asList(FAMSTUDIES_NIECENEPHEW, FAMSTUDIES_COUSIN), null, true)
                    || !this.checkRelationsOnlyTo(rootSiblings, Arrays.asList(FAMSTUDIES_AUNTUNCLE), children, true)) {
                    return false;
                }

                // root/partner parents:
                //  - GRANDPARENT: only to children or nodes not in pedigree
                //  - UNCLE/COUSIN/NIECE/GRANDCHILD: only nodes not in pedigree
                Set<PatientReference> allGrandParents = new HashSet<>();
                allGrandParents.addAll(rootParents);
                allGrandParents.addAll(partnerParents);
                if (!this.checkRelationsOnlyTo(allGrandParents, Arrays.asList(FAMSTUDIES_GRANDPARENT), children, true)
                    || !this.checkRelationsOnlyTo(allGrandParents, Arrays.asList(FAMSTUDIES_NIECENEPHEW,
                            FAMSTUDIES_AUNTUNCLE, FAMSTUDIES_COUSIN, FAMSTUDIES_GRANDCHILD), null, true)) {
                    return false;
                }

                //  children:
                //  - GRANDCHILD: only rootParents and partnerParents,
                //                or nodes not in pedigree if not all 4 grandparents are known
                //  - NIECE: only root's siblings or nodes not in pedigree
                //  - COUSIN: only nodes not in pedigree
                //  - GRANDPARENT/UNCLE: none
                boolean allGrandparentsKnown = (allGrandParents.size() == 4);
                if (!this.checkRelationsOnlyTo(children,
                        Arrays.asList(FAMSTUDIES_GRANDCHILD), allGrandParents, !allGrandparentsKnown)
                    || !this.checkRelationsOnlyTo(children, Arrays.asList(FAMSTUDIES_NIECENEPHEW), rootSiblings, true)
                    || !this.checkNoRelationsOfType(children, Arrays.asList(
                        FAMSTUDIES_GRANDPARENT, FAMSTUDIES_AUNTUNCLE))
                    || !this.checkRelationsOnlyTo(children, Arrays.asList(FAMSTUDIES_COUSIN), null, true)) {
                    return false;
                }

                return true;
            }

            private boolean checkNoRelationsOfType(Collection<PatientReference> patients,
                    Collection<String> forbiddenRelationshipTypes)
            {
                for (PatientReference p : patients) {
                    if (p == null) {
                        continue;
                    }
                    for (String relation : getPatientRelations(p).values()) {
                        if (forbiddenRelationshipTypes.contains(relation)) {
                            return false;
                        }
                    }
                }
                return true;
            }

            // checks that all relations of types listed in `relationTypes` only refer patients in the
            // `allowedTargets` or nodes not in the pedigree, if `allowNodesNotInPedigree == true`
            private boolean checkRelationsOnlyTo(Collection<PatientReference> patients,
                    Collection<String> relationTypes, Collection<PatientReference> allowedTargets,
                    boolean allowNodesNotInPedigree)
            {
                for (PatientReference p : patients) {
                    if (p == null) {
                        continue;
                    }
                    for (Map.Entry<PatientReference, String> relation : getPatientRelations(p).entrySet()) {
                        if (relationTypes.contains(relation.getValue())) {
                            if (allowedTargets == null || !allowedTargets.contains(relation.getKey())) {
                                if (!allowNodesNotInPedigree
                                    || this.patientToNodeId.keySet().contains(relation.getKey())) {
                                    return false;
                                }
                            }
                        }
                    }
                }
                return true;
            }

            public String getProbandPatientId() {
                return this.proband.phenotipsID;
            }

            // Returns all patient with a PT record that re included in the pedigree.
            // This may be different from RelatedSet.getPatientIDs() since not all patients may be included in pedigree
            public Set<String> getAllPTPatients()
            {
                Set<String> result = new HashSet<>();
                for (PatientReference member: this.patientToNodeId.keySet()) {
                    if (member.phenotipsID != null) {
                        result.add(member.phenotipsID);
                    }
                }
                return result;
            }

            public JSONArray getPedigreeJSON()
            {
                // due to the way pedigree editor simpleJSON import works, siblings should
                // have two parent specified, otherwise each one will be added to a separate relationship
                // of the only specified parent
                if (this.children.size() > 0 && this.partner == null) {
                    // root+partner children have only one parent -> need ot add another one
                    this.setPartner(new PatientReference("virtual partner"));
                }
                if (this.rootSiblings.size() > 0 && this.rootParents.size() < 2) {
                    // root and siblings have only one parent -> need ot add another one
                    this.addRootParent(new PatientReference("virtual grandparent"));
                }

                JSONArray allNodes = new JSONArray();

                this.addNode(allNodes, root, this.rootParents);
                for (PatientReference parent : this.rootParents) {
                    this.addNode(allNodes, parent, null);
                }

                for (PatientReference sibling : this.rootSiblings) {
                    this.addNode(allNodes, sibling, this.rootParents);
                }

                if (partner != null) {
                    this.addNode(allNodes, partner, this.partnerParents);
                    for (PatientReference parent : this.partnerParents) {
                        this.addNode(allNodes, parent, null);
                    }
                }

                Set<PatientReference> baseFamily = new HashSet<>(partner != null
                        ? Arrays.asList(root, partner) : Arrays.asList(root));
                for (PatientReference child : this.children) {
                    this.addNode(allNodes, child, baseFamily);
                }
                return allNodes;
            }

            private void addNode(JSONArray allNodes,
                    PatientReference patient, Collection<PatientReference> parents)
            {
                if (patient != null) {
                    JSONObject patientJSON = this.createNodeJSON(patient);
                    this.addParents(patientJSON, parents);
                    allNodes.put(patientJSON);
                }
            }

            private JSONObject createNodeJSON(PatientReference patient)
            {
                JSONObject result;
                if (patient.phenotipsID == null) {
                    result = new JSONObject();
                    result.put(JSON_SEX, this.deductNodeGender(patient));
                    if (patient.externalID != null) {
                        result.put(JSON_EXTERNALID, patient.externalID);
                    } else {
                        result.put(JSON_COMMENTS, "unknown");
                    }
                } else {
                    result = patientData.get(patient.phenotipsID).getPatientSimpleJSON();

                    String comment = "";
                    // add all nodes this patient refers to and which are not in pedigree as comments
                    for (Map.Entry<PatientReference, String> relation : getPatientRelations(patient).entrySet()) {
                        PatientReference relative = relation.getKey();
                        if (!this.patientToNodeId.keySet().contains(relative)) {
                            // check if patient refers to the relative, or the other way around
                            // here we only care about outgoing references
                            if (patientData.get(patient.phenotipsID).getRefersTo().containsKey(relative.externalID)) {
                                comment += generateOneCommentLine(relative.externalID, relation.getValue());
                            }
                        }
                    }
                    if (comment.length() > 0) {
                        result.put(JSON_COMMENTS, PEDIGREE_MIGRATED_RELATIVE_COMMENT_HEADER + comment);
                    }
                }
                result.put(JSON_NODE_ID, this.patientToNodeId.get(patient));
                if (patient.equals(this.proband)) {
                    result.put(JSON_PROBAND, true);
                }
                if (getPatientRelations(patient).containsValue(FAMSTUDIES_TWIN)) {
                    // TODO: check if there are multiple twin groups
                    result.put(JSON_TWINGROUP, 0);
                }
                return result;
            }

            private void addParents(JSONObject patientJSON, Collection<PatientReference> parents)
            {
                if (parents != null) {
                    PatientReference father = null;
                    PatientReference mother = null;
                    // figure out which one is the mother, which one is the mother:
                    // current SimpleJSON requires to specify father/mother, not parent1/parent2
                    for (PatientReference parent : parents) {
                        String gender = (parent.phenotipsID == null) ? SEX_UNKNOWN
                                : patientData.get(parent.phenotipsID).getGender();
                        if (SEX_FEMALE.equals(gender)) {
                            if (mother != null) {
                                father = mother;
                            }
                            mother = parent;
                        } else if (SEX_MALE.equals(gender)) {
                            if (father != null) {
                                mother = father;
                            }
                            father = parent;
                        } else {
                            if (father == null) {
                                father = parent;
                            } else {
                                mother = parent;
                            }
                        }
                    }
                    if (father != null) {
                        patientJSON.put(JSON_FATHER, this.patientToNodeId.get(father));
                    }
                    if (mother != null) {
                        patientJSON.put(JSON_MOTHER, this.patientToNodeId.get(mother));
                    }
                }
            }

            private String deductNodeGender(PatientReference patient)
            {
                if (!patient.isVirtual()) {
                    // nodes which have an external ID and are no completely virtual should have an unknown gender,
                    // can't assign a gender to a real patient, even if the patient has no PT record
                    return SEX_UNKNOWN;
                }

                PatientReference patientPartner = this.partners.get(patient);
                if (patientPartner != null) {
                    if (patientPartner.phenotipsID != null) {
                        return patientData.get(patientPartner.phenotipsID).getSuggestedPartnerGender();
                    } else if (patientPartner.isVirtual()) {
                        // both parents are virtual - randomly assign male/female to them, since
                        // both will end up in this code branch
                        if (patient.hashCode() > patientPartner.hashCode()) {
                            return SEX_FEMALE;
                        } else {
                            return SEX_MALE;
                        }
                    }
                } else {
                    // only one parent: gender does not matter, since pedigree will auto-add another partner,
                    // but to make it look good in pedigree this virtual parent needs to have some defined gender
                    // (so that the other parent added by pedigree gets the opposite gender and the result is the
                    // expected mostly likely case of male+female parents)
                    return SEX_MALE;
                }
                return SEX_UNKNOWN;
            }
        }

        // set of all patients in the family - including those with no PhenoTips record (only external ID is known)
        private Set<PatientReference> patients = new HashSet<>();

        // all patient which have a phenotips record
        private Map<String, PatientReference> phenotipsPatients = new HashMap<>();

        // set of all owners of all patient records
        private Set<String> owners = new HashSet<>();

        // familyID -> set of patientIDs
        private Map<String, Set<String>> familyIds = new HashMap<>();

        // set of all relationships (regardless of which patient stated it), assuming all are symmetric
        // used for simplicity, to avoid having to look both ways when checking a relation, also helps
        // detect inconsistencies easier
        private Map<PatientReference, Map<PatientReference, String>> allRelations = new HashMap<>();

        // a flag that some relation is directly inconsistent (e.g. A is B's parent, but B is A's sibling)
        private boolean inconsistenciesDetected;

        // a flag that the relationships are either ambiguous or more complicated than the migrator can handle
        // or inconsistent in an indirect manner (e.g. A is a child of B, B is a child of C, but C is something
        // other than a grandparent of A)
        private boolean complicatedDetected;

        // the patient (with a PhenoTips record) which is assumed to be the proband in the family
        private PatientReference assumedProband;

        RelatedSet(String rootPatientId)
        {
            logger.debug("=== New RelatedSet, rootPatient: {} =======================================", rootPatientId);

            String rootPatientExternalId = patientData.get(rootPatientId).getExternalId();
            this.addPatient(rootPatientId, rootPatientExternalId);

            // compute relationships that can be derived but are not specified, e.g.
            // if A is a parent of both B and C then B and C must be siblings
            // or if A is a twin of B and B is a sibling of C then A and C are sinlings
            this.computeDerivedRelationships();

            // check for special case, when a bunch of children do not have a parent
            // in that case create a fake parent since pedigree needs all children to have a parent
            this.createNecessaryVirtualParents();
        }

        public Map<String, Set<String>> getFamiliesInvolved()
        {
            return this.familyIds;
        }

        // returns all relatives from this set which are currently in the given family
        public Set<String> getAllFamilyMembers(String familyId)
        {
            return this.familyIds.get(familyId);
        }

        public int getNumberOfPatients()
        {
            return this.patients.size();
        }

        public Set<String> getPatientIDs()
        {
            return this.phenotipsPatients.keySet();
        }

        public Set<String> getAllOwners()
        {
            return this.owners;
        }

        public boolean hasInconsistentRelationships()
        {
            return inconsistenciesDetected;
        }

        // This method tries to find if there is a patient in the family which can be the root patient for the
        // pedigree (see Pedigree class), and returns pedigree build around that patient as an array of nodes in
        // pedigree SimpleJSON format supported by PhenoTiups pedigree editor as input.
        public Pedigree buildPedigree()
        {
            if (this.complicatedDetected) {
                return null;
            }
            Set<PatientReference> needToInclude = this.getPatientsRequiredToBeIncluded();

            for (PatientReference rootPatient : needToInclude) {
                Pedigree pedigree = new Pedigree(rootPatient, this.assumedProband);
                // try to create the pedigree with this patient as the "root" patient
                // the pedigree should include all patients in the `needToInclude` list
                for (PatientReference other : needToInclude) {
                    if (!other.equals(rootPatient)) {
                        if (!pedigree.addPatient(other)) {
                            pedigree = null;
                            break;
                        }
                    }
                }
                if (pedigree != null && pedigree.noExtraRelationships()) {
                    return pedigree;
                }
            }
            // can't create pedigree
            return null;
        }

        // returns 0 if proband is not included
        private Set<PatientReference> getPatientsRequiredToBeIncluded()
        {
            Set<PatientReference> result = new HashSet<>();

            for (PatientReference patient : patients) {
                for (String relation : this.getPatientRelations(patient).values()) {
                    if (AUTO_LINKING_SUPPORTED_RELATIVE_TYPES.contains(relation)) {
                        result.add(patient);
                        break;
                    }
                }
            }
            // in the unlikely case the first patient created (the proband) is only related to all
            // other patients via e.g. "uncle" or "cousin" or "grandparent" relationships
            if (!result.contains(this.assumedProband)) {
                result.add(this.assumedProband);
            }
            return result;
        }

        private void addPatient(String patientId, String externalId)
        {
            PatientReference patient = new PatientReference(patientId, externalId);
            if (!patients.add(patient)) {
                // already in the set, means already has been processed
                return;
            }

            logger.debug("Adding patient {}/{}", patientId, externalId);

            if (patientId == null) {
                // not a real patient
                return;
            }
            phenotipsPatients.put(patientId, patient);

            MigrationPatient data = patientData.get(patientId);

            owners.add(data.getOwner());

            // check if this is the proband patient - assumed to be the patient with
            // the earliest PT record creation time
            Date creationDate = patientData.get(patientId).getCreationDate();
            if (this.assumedProband == null
                || creationDate.before(patientData.get(this.assumedProband.phenotipsID).getCreationDate())) {
                this.assumedProband = patient;
            }

            String familyId = data.getFamilyId();
            if (StringUtils.isNotBlank(familyId)) {
                Set<String> familyMembers = familyIds.get(familyId);
                if (familyMembers == null) {
                    familyMembers = new HashSet<>();
                    familyIds.put(familyId, familyMembers);
                }
                familyMembers.add(patientId);
            }

            // add all patients referencing this one or referred to by this patient to the "extended family"
            // represented by this class

            for (Map.Entry<String, String> relation : data.getRefersTo().entrySet()) {
                String eid = relation.getKey();
                String pid = externalToPTIds.get(eid);
                this.addPatient(pid, eid);

                // fill the `allRelations` map
                String relationType = relation.getValue();
                PatientReference otherPatient = new PatientReference(pid, eid);
                this.addRelation(patient, otherPatient, relationType);
            }

            for (String pid : data.getReferredBy().keySet()) {
                String eid = patientData.get(pid).getExternalId();
                this.addPatient(pid, eid);
            }
        }

        private void addRelation(PatientReference patient, PatientReference otherPatient, String relation)
        {
            logger.debug("Adding relation {} ---[{}]---> {}", patient, relation, otherPatient);

            Map<PatientReference, String> patientRelations = this.getPatientRelations(patient);
            String patientToOther = patientRelations.get(otherPatient);
            if (patientToOther != null) {
                if (!patientToOther.equals(relation)) {
                    this.inconsistenciesDetected = true;
                }
                return;
            } else {
                patientRelations.put(otherPatient, relation);
            }

            String inverseRelation = INVERSE_RELATIONSHIP.get(relation);
            Map<PatientReference, String> otherRelations = this.getPatientRelations(otherPatient);
            String otherToPatient = otherRelations.get(patient);
            if (otherToPatient != null) {
                if (!otherToPatient.equals(inverseRelation)) {
                    this.inconsistenciesDetected = true;
                }
                return;
            } else {
                otherRelations.put(patient, inverseRelation);
            }
        }

        // Derived relationships are:
        //  1) TWINS: a patient is a twin of all twins-of-twins
        //  2) SIBLINGS: a patient is a sibling of all twins/siblings of all their twins/siblings
        //  3) PARENTS: a parent of a sibling/twin is (assumed to be) a parent of all other siblings/twin
        //  4) GRANDPARENT: a child of a child is a grandchild (a parent of a parent is a grandparent)
        //  5) PARTNERS: if a patient is a child of two parents => parents are partners
        //
        // ...or can compute all at once, and keep computing until there are changes, according to this table:
        //
        //                      relation of A to B
        //    +---------+-------------+------------+------------+------------
        //    |         | PARENT      | CHILD      | SIBLING    | TWIN
        //    +---------+-------------+------------+------------+------------
        // B  | PARENT  | GRANDPARENT | SIBLING    | AUNT/UNCLE | AUNT/UNCLE
        // to | CHILD   | PARTNER     | GRANDCHILD | CHILD      | CHILD
        // C  | SIBLING | PARENT      | NIECE/NEPH | SIBLING    | SIBLING
        //    | TWIN    | PARENT      | NIECE/NEPH | SIBLING    | TWIN
        //
        //          (cell value == derived/expected relation of A to C)
        //
        private void computeDerivedRelationships()
        {
            // after each update other relations may get updated, so need to repeat until convergence
            int indirectionLevel = 0;
            boolean updated;
            do {
                updated = false;
                for (PatientReference patientA : patients) {
                    Map<PatientReference, String> patientARelations = this.getPatientRelations(patientA);
                    for (Map.Entry<PatientReference, String> relationA : patientARelations.entrySet()) {
                        PatientReference patientB = relationA.getKey();
                        String aToB = relationA.getValue();
                        Map<PatientReference, String> patientBRelations = this.getPatientRelations(patientB);
                        boolean updatedA = false;
                        for (Map.Entry<PatientReference, String> relationB : patientBRelations.entrySet()) {
                            PatientReference patientC = relationB.getKey();
                            String bToC = relationB.getValue();
                            updatedA = updatedA || this.setOrVerifyRelation(patientA, patientC, aToB, bToC);
                        }
                        if (updatedA) {
                            // setOrVerifyRelation updated patientARelations, iterator is now broken
                            updated = true;
                            break;
                        }
                    }
                    if (this.getNumberOfParents(patientA) > 2) {
                        logger.debug("Complicated - getNumberOfParents([{}]) > 2", patientA);
                        this.complicatedDetected = true;
                    }
                    if (this.getNumberOfPartners(patientA) > 1) {
                        logger.debug("Complicated - getNumberOfPartners([{}]) > 1", patientA);
                        this.complicatedDetected = true;
                    }
                }
                if (indirectionLevel++ >= 5) {
                    // the chain of relations is likely too long to be reliably represented by
                    // an auto-generated pedigree
                    logger.debug("Complicated - indirectionLevel >= 5");
                    this.complicatedDetected = true;
                }
            }
            while (updated && !this.complicatedDetected);
        }

        // Find siblings which do not have any parents, and adds a virtual parent
        // (since pedigree needs all children to have a parent)
        private void createNecessaryVirtualParents()
        {
            boolean updated;
            do {
                updated = false;
                for (PatientReference patient : patients) {
                    Set<PatientReference> siblings = this.getAllRelativesOfType(patient, FAMSTUDIES_SIBLING);
                    siblings.addAll(this.getAllRelativesOfType(patient, FAMSTUDIES_TWIN));

                    Set<PatientReference> parents = this.getAllRelativesOfType(patient, FAMSTUDIES_CHILD);
                    if (siblings.size() > 0 && parents.size() == 0) {
                        PatientReference virtualParent = new PatientReference("virtual parent " + patient.toString());
                        patients.add(virtualParent);
                        siblings.add(patient);
                        for (PatientReference sibling : siblings) {
                            addRelation(sibling, virtualParent, FAMSTUDIES_CHILD);
                            updated = true;
                        }
                    }
                    // iterator is invalidated, need to start again
                    if (updated) {
                        break;
                    }
                }
            }
            while (updated);
        }

        // tries to derive a relationship between patients A and C based on relations between A and
        // some B and that B and C. If there is no relationship between A and C sets it, if there is
        // - verifies that derived and actual match.
        //
        // return `true` if there were any changes in the relations table
        private boolean setOrVerifyRelation(PatientReference patientA, PatientReference patientC,
                String aToBrelation, String bToCrelation)
        {
            if (patientC.equals(patientA)) {
                return false;
            }
            Integer indexAtoB = DERIVED_RELATIONS_INDEX.get(aToBrelation);
            Integer indexBtoC = DERIVED_RELATIONS_INDEX.get(bToCrelation);
            if (indexAtoB == null || indexBtoC == null) {
                // no changes
                return false;
            }
            String expectedAtoC = DERIVED_RELATIONS_TABLE[indexBtoC][indexAtoB];

            String actualAtoC = this.getRelation(patientA, patientC);

            if (actualAtoC != null) {
                if (!actualAtoC.equals(expectedAtoC)) {
                    logger.debug("Complicated - [{}]-to-[{}] is [{}], not equal to expected [{}]",
                            patientA, patientC, actualAtoC, expectedAtoC);
                    this.complicatedDetected = true;
                }
            } else {
                this.addRelation(patientA, patientC, expectedAtoC);
                return true;
            }
            return false;
        }

        private String getRelation(PatientReference patient, PatientReference otherPatient)
        {
            return this.getPatientRelations(patient).get(otherPatient);
        }

        private Map<PatientReference, String> getPatientRelations(PatientReference patient)
        {
            Map<PatientReference, String> patientRelations = allRelations.get(patient);
            if (patientRelations == null) {
                patientRelations = new HashMap<>();
                allRelations.put(patient, patientRelations);
            }
            return patientRelations;
        }

        private int getNumberOfParents(PatientReference patient)
        {
            return this.getAllRelativesOfType(patient, FAMSTUDIES_CHILD).size();
        }

        private int getNumberOfPartners(PatientReference patient)
        {
            return this.getAllRelativesOfType(patient, INTERNAL_PARTNER).size();
        }

        private Set<PatientReference> getAllRelativesOfType(PatientReference patient, String relationType)
        {
            Set<PatientReference> result = new HashSet<>();
            Map<PatientReference, String> patientRelations = this.getPatientRelations(patient);
            for (Map.Entry<PatientReference, String> relation : patientRelations.entrySet()) {
                if (relationType.equals(relation.getValue())) {
                    result.add(relation.getKey());
                }
            }
            return result;
        }

        // returns a map from external ID to Phenotips ID for all patients in the family (which have an external ID)
        public Map<String, String> getExternalToPhenotipsIdMapping()
        {
            Map<String, String> result = new HashMap<>();
            for (PatientReference patient : phenotipsPatients.values()) {
                if (StringUtils.isNotBlank(patient.externalID)) {
                    result.put(patient.externalID, patient.phenotipsID);
                }
            }
            return result;
        }

        // for all patients in the set, collect their family studies data as a single string
        // returns: map<phenotipsID> -> all_family_studies-data_as_a_comment_string
        public Map<String, String> getFamilyStudiesDataAsPedigreeComments()
        {
            Map<String, String> result = new HashMap<>();
            for (PatientReference patient : this.phenotipsPatients.values()) {
                String comment = this.generatePedigreeCommentForRelatives(patient.phenotipsID);
                if (StringUtils.isNotBlank(comment)) {
                    result.put(patient.phenotipsID, comment);
                }
            }
            return result;
        }

        // generates comments for every (outgoing, e.g. indicated in this patient's document)
        // family studies relation this patient has
        private String generatePedigreeCommentForRelatives(String phenotipsID)
        {
            Map<String, String> refersTo = patientData.get(phenotipsID).getRefersTo();
            if (refersTo.size() == 0) {
                return null;
            }
            String comment = PEDIGREE_MIGRATED_RELATIVE_COMMENT_HEADER;
            for (Map.Entry<String, String> relation : refersTo.entrySet()) {
                String relativeExternalID = relation.getKey();
                String relationshipType = relation.getValue();
                comment += generateOneCommentLine(relativeExternalID, relationshipType);
            }
            return comment;
        }

        private String generateOneCommentLine(String patientExternalId, String relationshipType)
        {
            String relativeNamePlusArticle = RELATIVE_ID_TO_NAME.containsKey(relationshipType)
                    ? RELATIVE_ID_TO_NAME.get(relationshipType) : relationshipType;
            return "\n- this patient is " + relativeNamePlusArticle + " of patient " + patientExternalId;
        }
    }

    /**
     * The actual migration work: go patient by patient, try to figure out which patients are related
     * (using family studies data), and try to create a pedigree for each set of related patients (when possible)
     * or add comments detailing family studies data to existing pedigrees (when a pedigree is already present).
     */
    private void processData()
    {
        // some patients may not be linked to a family (because we don't know where to place them in the pedigree,
        // or don't know into which pedigree to place if there is more than one relative with a pedigree)
        // thus their familyID link is blank, but we already know they belong to _some_ family, and they have been
        // already mentioned in the comments for some pedigree, so we should not process them again
        Set<String> alreadyProcessedPatients = new HashSet<>();

        for (Map.Entry<String, MigrationPatient> patientEntry : patientData.entrySet()) {

            String patientID = patientEntry.getKey();

            // to speed up processing: this patient has already been processed as part of a family
            // initiated by another already processed patient
            if (alreadyProcessedPatients.contains(patientID)) {
                continue;
            }

            try {
                // collect all related patients (by any chain of references, see RelatedSet docs)
                RelatedSet relatedPatients = new RelatedSet(patientID);

                // mark all patients in the family as already processed
                alreadyProcessedPatients.addAll(relatedPatients.getPatientIDs());

                // 1) there is one or more pedigree in the patient set: process each pedigree separately,
                //    only add (as comments) those patients directly referenced to/by the patients in the pedigree
                //
                // 2) there is no pedigree AND more than one patient in the set: check if all links are valid
                //    (not contradictory, owned by the same user, etc., etc.) and create a new pedigree if more
                //    than one valid patient connected by supported relationship is in the set

                if (relatedPatients.getFamiliesInvolved().size() > 0) {
                    // one or more of the related patients have a family/pedigree: for each pedigree
                    // update is as best as possible (by adding comments or linking patients to existing nodes)
                    for (String familyId : relatedPatients.getFamiliesInvolved().keySet()) {

                        // only link pedigree nodes with externalIDs corresponding to real PT patients to those patients
                        // if there is only one family already defined, otherwise there can be too many complications
                        boolean linkPatientsByExternalId = (relatedPatients.getFamiliesInvolved().size() == 1);

                        this.updatePedigree(familyId, relatedPatients, linkPatientsByExternalId);
                    }
                } else {
                    if (relatedPatients.getNumberOfPatients() < 2) {
                        // only one patient: nothing to do
                        continue;
                    }

                    if (relatedPatients.hasInconsistentRelationships()) {
                        this.logger.error("Inconsistent relationships - can not create a family based on"
                                + " family studies data for related patients [{}]", relatedPatients.getPatientIDs());
                        continue;
                    }

                    if (relatedPatients.getAllOwners().size() > 1) {
                        this.logger.error("Can not create a family: some of the related patients [{}] are owned by"
                                + " different users", relatedPatients.getPatientIDs());
                        continue;
                    }

                    RelatedSet.Pedigree pedigreeData = relatedPatients.buildPedigree();

                    if (pedigreeData == null) {
                        // can not find a set of patients that can be grouped into a supported pedigree: relations
                        // are not uniquely identifiable (e.g. 3 siblings have 3 different parents, which may be
                        // valid if they are half-siblings, but we don't know) or too complicated and not supported
                        this.logger.error("Can not create a family based on family studies data for related"
                                + " patients [{}]: family is too complicated or not uniquely indentifiable",
                                relatedPatients.getPatientIDs());
                        continue;
                    }

                    // need to create a new pedigree, and add all "OK" relatives as pedigree nodes and link them,
                    // add all others as comments
                    this.createNewFamilyAndLinkRelatives(pedigreeData);
                }
            } catch (Exception ex) {
                this.logger.error("Error processing old family studies for patient [{}]: {}",
                        patientID, ex.getMessage(), ex);
            }
        }
    }

    /**
     * Updates an existing pedigree.
     *
     *  - (if requested) finds all nodes with external IDs matching existing relatives with phenotips records
     *    and - if not already linked to a PT record - links them to the given PT record
     *
     *  - (always) for each node linked to a PT record (including nodes linked in step1 above) appends the summary
     *    of old family studies data as a comment, for verification purposes
     *
     * @param familyID the id of the family with pedigree
     * @param relatedPatients the set of patients related to at least one of the patients already the pedigree
     * @param linkPatientsByExternalId when true, attempt to auto-link pedigree nodes to PT records based on externalID
     */
    private void updatePedigree(String familyId, RelatedSet relatedPatients, boolean linkPatientsByExternalId)
    {
        try {
            XWikiDocument familyXDocument =
                this.context.getWiki().getDocument(this.resolver.resolve(familyId), this.context);

            BaseObject pedigreeObj = familyXDocument.getXObject(Pedigree.CLASS_REFERENCE);
            if (pedigreeObj == null) {
                this.logger.error("Error updating existing family: no pedigree object for [{}]", familyId);
                return;
            }

            BaseStringProperty data = (BaseStringProperty) pedigreeObj.get(Pedigree.DATA);
            if (StringUtils.isNotBlank(data.toText())) {
                JSONObject pedigree = new JSONObject(data.toText());

                Map<String, String> externalIdsToLink = relatedPatients.getExternalToPhenotipsIdMapping();
                Map<String, String> commentsToAdd = relatedPatients.getFamilyStudiesDataAsPedigreeComments();

                logger.debug("Pedigree: externalIDsToLink: [{}]", externalIdsToLink.keySet());
                logger.debug("Pedigree: comments to add: [{}] -> [{}]", commentsToAdd.keySet(), commentsToAdd.values());

                Set<String> newlyLinkedPatients = this.familyMigrations.updatePedigree(pedigree,
                        externalIdsToLink, commentsToAdd);

                String summaryOfChanges = "(updated pedigree: ";
                if (newlyLinkedPatients.size() > 0) {
                    // some patients were linked to the pedigree based on pedigree node externalIDs - need to
                    // link PhenoTips documents to the family now
                    this.setAllFamilyRefs(relatedPatients.getAllFamilyMembers(familyId),
                            newlyLinkedPatients, familyXDocument);
                    summaryOfChanges += "linked patients to nodes based on their external IDs and ";
                }
                summaryOfChanges += "added family studies summary to node comments)";

                pedigreeObj.set(Pedigree.DATA, pedigree.toString(), this.context);

                String comment = this.getDescription() + summaryOfChanges;
                if (!this.saveXWikiDocument(familyXDocument, comment)) {
                    this.logger.error("Updated family document was not saved for family [{}]", familyId);
                }
            }
        } catch (Exception ex) {
            this.logger.error("Error updating pedigree for family [{}]: {}", familyId, ex.getMessage(), ex);
        }
    }

    /**
     * Creates a new family, adds the generated pedigree JSON to it and links all patients (patient documents)
     * that made it into the pedigree.
     */
    private void createNewFamilyAndLinkRelatives(RelatedSet.Pedigree pedigreeData)
    {
        try {
            JSONObject pedigree = this.generateFamilyPagePedigreeJSON(pedigreeData);

            Set<String> patients = pedigreeData.getAllPTPatients();

            // generate new family document and set links
            XWikiDocument familyXDocument = this.createNewFamily(
                    pedigree, pedigreeData.getProbandPatientId(), patients);

            String documentUpdateDescription = this.getDescription()
                + " (generated a pedigree using Family Studies data)";

            // try to save the new document, if it was created
            if (familyXDocument == null || !this.saveXWikiDocument(familyXDocument, documentUpdateDescription)) {
                this.logger.error("Failed to create new family document for patients [{}]", patients);
            } else {
                this.logger.info("Created new family [{}] for patients [{}]",
                    familyXDocument.getDocumentReference().getName(), patients);
            }
        } catch (Exception ex) {
            this.logger.error("Error creating new family: {}", ex.getMessage(), ex);
        }
    }

    private JSONObject generateFamilyPagePedigreeJSON(RelatedSet.Pedigree pedigree)
    {
        JSONObject result = new JSONObject();
        result.put("data", pedigree.getPedigreeJSON());
        this.logger.debug("Created pedigree JSON: [{}]", result.toString());
        return result;
    }

    /**
     * Creates a new family document, sets the pedigree to the given one and links given patients of the family and
     * family to the patients. Only relatives from the list ["parent", "child", "sibling", "twin"] may be auto-added to
     * the generated pedigree.
     */
    private XWikiDocument createNewFamily(JSONObject pedigree, String probandPatientId, Set<String> allPatients)
    {
        // TODO: Generate the correct image as well? Or may use one of the predefined images.
        // For now using the same SVG with explanation text for all the families.
        String pedigreeImage =
            ("<svg xmlns=`http://www.w3.org/2000/svg` xmlns:xlink=`http://www.w3.org/1999/xlink` "
                + "width=`200` height=`200` preserveAspectRatio=`xMidYMid` viewBox=`0 0 480 480`>"
                + "<text fill=`black` x=`25` y=`78` style=`font-family: Arial; font-size: 38px;`>"
                + "This pedigree is auto-"
                + "<tspan x=`25` dy=`1.5em`>generated based on the </tspan>"
                + "<tspan x=`25` dy=`1.5em`>data from the deprecated </tspan>"
                + "<tspan x=`25` dy=`1.5em`>Family Studies section.</tspan></text>"
                + "<text fill=`black` x=`25` y=`360` style=`font-family: Arial; font-size: 38px;`>"
                + "Please open pedigree "
                + "<tspan x=`25` dy=`1.5em`>editor to see the pedigree.</tspan></text></svg>").replace('`', '"');

        this.logger.debug("Creating new family for patient [{}]", probandPatientId);

        XWikiDocument newFamilyXDocument =
            this.familyMigrations.createFamilyDocument(this.getPatientXDocument(probandPatientId),
                pedigree, pedigreeImage, this.context, this.session);

        if (newFamilyXDocument != null) {
            this.setAllFamilyRefs(null, allPatients, newFamilyXDocument);
        }
        return newFamilyXDocument;
    }

    /**
     * Set family references for all relatives.
     */
    private void setAllFamilyRefs(Set<String> existingMembers, Set<String> newFamilyMembers,
            XWikiDocument familyXDocument)
    {
        String familyID = familyXDocument.getDocumentReference().getName();

        try {
            String familyDocumentRef = familyXDocument.getDocumentReference().toString();

            // has to be a list because of setStringListValue() interface
            List<String> membersRefsList = existingMembers == null
                    ? new LinkedList<String>() : new LinkedList<String>(existingMembers);

            for (String memberId : newFamilyMembers) {
                membersRefsList.add(memberId);
                // set the family reference to a relative doc
                XWikiDocument patientDoc = this.getPatientXDocument(memberId);
                if (!this.familyMigrations.setFamilyReference(patientDoc, familyDocumentRef, this.context)
                    || !this.saveXWikiDocument(patientDoc, this.getDescription())) {
                    this.logger.error("Failed to link patient [{}] to family [{}]", memberId, familyID);
                }
            }

            BaseObject familyObject = familyXDocument.getXObject(Family.CLASS_REFERENCE, true, this.context);
            familyObject.setStringListValue("members", membersRefsList);
        } catch (Exception ex) {
            this.logger.error("Failed to link family [{}] to its new members [{}]", familyID, newFamilyMembers);
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

    private boolean saveXWikiDocument(XWikiDocument doc, String documentHistoryComment)
    {
        try {
            this.logger.debug("Migration: saving document [{}]...", doc.getDocumentReference().getName());
            doc.setComment(documentHistoryComment);
            this.session.clear();
            ((XWikiHibernateStore) getStore()).saveXWikiDoc(doc, this.context, false);
            this.session.flush();
            return true;
        } catch (Exception ex) {
            this.logger.error("Error saving document [{}]: {}", doc.getDocumentReference().getName(), ex.getMessage(),
                ex);
            return false;
        }
    }
}
