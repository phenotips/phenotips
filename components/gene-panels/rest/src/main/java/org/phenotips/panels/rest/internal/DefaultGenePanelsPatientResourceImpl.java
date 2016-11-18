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
package org.phenotips.panels.rest.internal;

import org.phenotips.data.Feature;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.panels.internal.DefaultGenePanelFactoryImpl;
import org.phenotips.panels.rest.GenePanelsPatientResource;
import org.phenotips.vocabulary.VocabularyManager;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONObject;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;

/**
 * Default implementation of the {@link GenePanelsPatientResource}.
 *
 * @version $Id$
 * @since 1.3M5
 */
@Component
@Named("org.phenotips.panels.rest.internal.DefaultGenePanelsPatientResourceImpl")
@Singleton
public class DefaultGenePanelsPatientResourceImpl extends XWikiResource implements GenePanelsPatientResource
{
    @Inject
    private VocabularyManager vocabularyManager;

    @Inject
    private Logger logger;

    @Inject
    private PatientRepository repository;

    @Inject
    private AuthorizationManager access;

    @Inject
    private UserManager users;

    @Override
    public Response getPatientGeneCounts(final String patientId)
    {
        final Patient patient = repository.get(patientId);
        final User currentUser = this.users.getCurrentUser();

        if (patient == null) {
            this.logger.warn("Could not find patient with ID {}", patientId);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (!this.access.hasAccess(Right.VIEW, currentUser == null ? null : currentUser.getProfileDocument(),
            patient.getDocument())) {
            this.logger.warn("View access denied to user [{}] on patient record [{}]", currentUser, patientId);
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        final Set<? extends Feature> features = patient.getFeatures();
        final List<String> presentTerms = getPresentTerms(features);
        final JSONObject genePanels = new DefaultGenePanelFactoryImpl()
            .makeGenePanel(presentTerms, this.vocabularyManager)
            .toJSON();
        return Response.ok(genePanels, MediaType.APPLICATION_JSON_TYPE).build();
    }

    /**
     * Extracts present terms from the provided set of features.
     *
     * @param features the features recorded for a patient
     */
    private List<String> getPresentTerms(final Set<? extends Feature> features)
    {
        final ImmutableList.Builder<String> presentFeaturesBuilder = ImmutableList.builder();
        for (final Feature feature : features) {
            if (feature.isPresent()) {
                presentFeaturesBuilder.add(feature.getValue());
            }
        }
        return presentFeaturesBuilder.build();
    }
}
