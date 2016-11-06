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
package org.phenotips.studies.family.rest.internal;

import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyTools;
import org.phenotips.studies.family.rest.FamiliesResource;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.rest.XWikiResource;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.net.URI;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * Default implementation for {@link FamiliesResource} using XWiki's support for REST resources.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("org.phenotips.studies.family.rest.internal.DefaultFamiliesResourceImpl")
@Singleton
public class DefaultFamiliesResourceImpl extends XWikiResource implements FamiliesResource
{
    private static final String METADATA_FIELD_NAME = "metadata";

    private static final String DATA_FIELD_NAME = "data";

    @Inject
    private Logger logger;

    @Inject
    private QueryManager queries;

    @Inject
    private AuthorizationManager access;

    @Inject
    private UserManager users;

    @Inject
    private FamilyTools familyTools;

    /** Fills in missing reference fields with those from the current context document to create a full reference. */
    @Inject
    @Named("current")
    private EntityReferenceResolver<EntityReference> currentResolver;

    /** Parses string representations of document references into proper references. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> stringResolver;

    @Override
    public Response addFamily(String json)
    {
        this.logger.error("Importing new family from JSON via REST: {}", json);

        User currentUser = this.users.getCurrentUser();
        if (!this.access.hasAccess(Right.EDIT, currentUser == null ? null : currentUser.getProfileDocument(),
            this.currentResolver.resolve(Family.DATA_SPACE, EntityType.SPACE))) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
        if (json == null) {
            // json == null does not create an exception when initializing a JSONObject
            // need to handle it separately to give explicit BAD_REQUEST to the user
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        JSONObject jsonInput;
        try {
            jsonInput = new JSONObject(json);
        } catch (Exception ex) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        try {
            Family family = this.familyTools.createFamily();
            // TODO: update from JSON

            URI targetURI =
                UriBuilder.fromUri(this.uriInfo.getBaseUri()).path(FamiliesResource.class).build(family.getId());
            ResponseBuilder response = Response.created(targetURI);
            return response.build();
        } catch (Exception ex) {
            this.logger.error("Could not process family creation request: {}", ex.getMessage(), ex);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response listFamilies(Integer start, Integer number, Boolean fullJSON)
    {
        this.logger.error("Listing families via via REST, start: {} number: {}, fullJSON: {}", start, number, fullJSON);
        long startTime = System.nanoTime();

        try {
            String safeOrderField = "doc.name";
            String safeOrder = " asc";
            Query query = this.queries.createQuery(
                "select doc.name, family.external_id, doc.creator, doc.creationDate, doc.version, doc.author, doc.date"
                    + " from Document doc, doc.object(PhenoTips.FamilyClass) as family where doc.name <> :t order by "
                    + safeOrderField + safeOrder,
                Query.XWQL);
            query.bindValue("t", "FamilyTemplate");

            JSONArray familyList = new JSONArray();

            List<Object[]> records = query.execute();

            User currentUser = this.users.getCurrentUser();
            DocumentReference userProfileDocument = (currentUser == null) ? null : currentUser.getProfileDocument();

            int total = 0;
            int skipped = 0;

            for (Object[] record : records) {

                String familyID = (String) record[0];
                this.logger.warn("REST: Found family: {}", familyID);

                if (!validateSummaryObject(record)) {
                    this.logger.warn("REST: Skipping family, misformatted data");
                    continue;
                }
                if (!hasViewRights(familyID, userProfileDocument)) {
                    this.logger.warn("REST: Skipping family, no view rights for the user");
                    continue;
                }

                total++;

                // Since raw queries can't take into account access rights, we must do our own paging with rights checks
                if (++skipped > start && familyList.length() < number) {
                    JSONObject familyJSON;
                    try {
                        if (fullJSON) {
                            familyJSON = getFullFamilyJSON(familyID, record, this.uriInfo);
                        } else {
                            familyJSON = getFamilySummaryJSON(record, this.uriInfo);
                        }
                        if (familyJSON != null) {
                            familyList.put(familyJSON);
                        }
                    } catch (Exception ex) {
                        this.logger.error("Error creating family JSON: {}", ex.getMessage(), ex);
                    }
                }
            }

            JSONObject metadata = new JSONObject();
            metadata.put("totalVisibleFamilies", total);
            metadata.put("returnedFamilies", familyList.length());
            metadata.put("requestedPageSize", number);

            JSONObject response = new JSONObject();
            response.put(METADATA_FIELD_NAME, metadata);
            response.put(DATA_FIELD_NAME, familyList);

            this.logger.error("Time to complete request: {} ms", Math.round((System.nanoTime() - startTime) / 1000000));

            return Response.ok(response, MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception ex) {
            this.logger.error("Failed to list families: {}", ex.getMessage(), ex);
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
    }

    private JSONObject getFullFamilyJSON(String familyID, Object[] summaryData, UriInfo uriInfo)
    {
        Family family = this.familyTools.getFamilyById(familyID);
        if (family != null) {
            JSONObject familyJSON = family.toJSON();
            addMetadata(familyJSON, summaryData);
            return familyJSON;
        }
        return null;
    }

    private JSONObject getFamilySummaryJSON(Object[] summaryData, UriInfo uriInfo)
    {
        JSONObject familySummary = new JSONObject();
        familySummary.put("id", StringUtils.defaultString((String) summaryData[0]));
        familySummary.put("externalId", StringUtils.defaultString((String) summaryData[1]));
        addMetadata(familySummary, summaryData);
        return familySummary;
    }

    private void addMetadata(JSONObject json, Object[] summaryData)
    {
        JSONObject metaData = new JSONObject();
        metaData.put("createdBy", String.valueOf(summaryData[2]));
        metaData.put("lastModifiedBy", String.valueOf(summaryData[5]));
        metaData.put("version", String.valueOf(summaryData[4]));
        metaData.put("createdOn", new DateTime(summaryData[3]));
        metaData.put("lastModifiedOn", new DateTime(summaryData[6]));
        json.put(METADATA_FIELD_NAME, metaData);
    }

    private boolean validateSummaryObject(Object[] summaryData)
    {
        if (summaryData == null || summaryData.length != 7
            || !(summaryData[3] instanceof Date && summaryData[6] instanceof Date)) {
            return false;
        }
        return true;
    }

    private boolean hasViewRights(String familyID, DocumentReference userProfileDocument)
    {
        DocumentReference doc = this.stringResolver.resolve(String.valueOf(familyID));
        if (!this.access.hasAccess(Right.VIEW, userProfileDocument, doc)) {
            return false;
        }
        return true;
    }
}
