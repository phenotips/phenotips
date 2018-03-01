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
package org.phenotips.data.internal.controller;

import org.phenotips.data.Cancer;
import org.phenotips.data.CancerQualifier;
import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.PatientWritePolicy;
import org.phenotips.data.internal.PhenoTipsCancer;

import org.xwiki.component.annotation.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Handles the patients cancers.
 *
 * @version $Id$
 * @since 1.4
 */
@Component(roles = { PatientDataController.class })
@Named("cancers")
@Singleton
public class CancersController extends AbstractComplexController<Cancer>
{
    private static final String CANCERS_FIELD_NAME = "cancers";

    private static final String CONTROLLER_NAME = CANCERS_FIELD_NAME;

    @Inject
    private Logger logger;

    /** Provides access to the current execution context. */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    @Nonnull
    protected List<String> getBooleanFields()
    {
        return Collections.singletonList(Cancer.CancerProperty.AFFECTED.getProperty());
    }

    @Override
    @Nonnull
    protected List<String> getCodeFields()
    {
        return Collections.emptyList();
    }

    @Override
    @Nonnull
    protected List<String> getProperties()
    {
        return Collections.emptyList();
    }

    @Override
    @Nonnull
    protected String getJsonPropertyName()
    {
        return CONTROLLER_NAME;
    }

    @Override
    @Nonnull
    public String getName()
    {
        return CONTROLLER_NAME;
    }

    @Override
    @Nullable
    public PatientData<Cancer> load(@Nonnull final Patient patient)
    {
        try {
            final XWikiDocument doc = patient.getXDocument();
            final List<BaseObject> cancerXWikiObjects = doc.getXObjects(Cancer.CLASS_REFERENCE);
            if (CollectionUtils.isEmpty(cancerXWikiObjects)) {
                return null;
            }
            final List<Cancer> cancers = cancerXWikiObjects.stream()
                    .filter(Objects::nonNull)
                    .filter(cancerObj -> !cancerObj.getFieldList().isEmpty())
                    .map(cancerObj -> new PhenoTipsCancer(doc, cancerObj))
                    .collect(Collectors.toList());
            return !cancers.isEmpty() ? new IndexedPatientData<>(getName(), cancers) : null;
        } catch (final Exception e) {
            this.logger.error(ERROR_MESSAGE_LOAD_FAILED, e.getMessage());
        }
        return null;
    }

    @Override
    public void writeJSON(@Nonnull final Patient patient,
                          @Nonnull final JSONObject json,
                          @Nullable final Collection<String> selectedFieldNames)
    {
        if (selectedFieldNames == null || selectedFieldNames.contains(getJsonPropertyName())) {
            final JSONArray cancersJson = new JSONArray();
            final PatientData<Cancer> data = patient.getData(getName());
            if (data != null && data.size() != 0 && data.isIndexed()) {
                data.forEach(cancer -> addCancerJson(cancer, cancersJson));
            }
            json.put(getJsonPropertyName(), cancersJson);
        }
    }

    @Override
    @Nullable
    public PatientData<Cancer> readJSON(@Nullable final JSONObject json)
    {
        if (json == null || json.optJSONArray(getJsonPropertyName()) == null) {
            return null;
        }
        try {
            final JSONArray cancersJson = json.getJSONArray(getJsonPropertyName());
            final List<Cancer> cancers = IntStream.range(0, cancersJson.length())
                    .mapToObj(cancersJson::optJSONObject)
                    .filter(Objects::nonNull)
                    .map(PhenoTipsCancer::new)
                    .collect(Collectors.toList());
            return new IndexedPatientData<>(getName(), cancers);
        } catch (final Exception e) {
            this.logger.error("Could not load cancers from JSON: [{}]", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void save(@Nonnull final Patient patient)
    {
        save(patient, PatientWritePolicy.UPDATE);
    }

    @Override
    public void save(@Nonnull final Patient patient, @Nonnull final PatientWritePolicy policy)
    {
        try {
            final XWikiDocument docX = patient.getXDocument();
            final PatientData<Cancer> cancers = patient.getData(getName());
            if (cancers == null) {
                if (PatientWritePolicy.REPLACE.equals(policy)) {
                    clearOldCancerData(docX);
                }
            } else {
                if (!cancers.isIndexed()) {
                    this.logger.error(ERROR_MESSAGE_DATA_IN_MEMORY_IN_WRONG_FORMAT);
                    return;
                }
                saveCancers(docX, patient, cancers, policy, this.xcontextProvider.get());
            }
        } catch (final Exception ex) {
            this.logger.error("Failed to save cancers data: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Adds the {@link JSONObject} generated from {@code cancer} to the {@code cancersJson}.
     *
     * @param cancer the {@link Cancer} object containing cancer data
     * @param cancersJson the {@link JSONArray} containing all cancer data for a {@link Patient patient}
     */
    private void addCancerJson(@Nonnull final Cancer cancer, @Nonnull final JSONArray cancersJson)
    {
        if (StringUtils.isNotBlank(cancer.getId()) || StringUtils.isNotBlank(cancer.getName())) {
            cancersJson.put(cancer.toJSON());
        }
    }

    /**
     * Saves the {@code cancers} retrieved from {@code patient} according to the specified save {@code policy}.
     *
     * @param docX the {@link XWikiDocument} where data will be saved
     * @param patient the {@link Patient} object that contains the required data
     * @param cancers the cancers {@link PatientData} object
     * @param policy the {@link PatientWritePolicy} according to which data should be saved
     * @param context the current {@link XWikiContext}
     */
    private void saveCancers(@Nonnull final XWikiDocument docX,
                             @Nonnull final Patient patient,
                             @Nonnull final PatientData<Cancer> cancers,
                             @Nonnull final PatientWritePolicy policy,
                             @Nonnull final XWikiContext context)
    {
        if (PatientWritePolicy.MERGE.equals(policy)) {
            final Map<String, Cancer> mergedCancers = getMergedCancers(cancers, load(patient));
            clearOldCancerData(docX);
            mergedCancers.forEach((id, cancer) -> cancer.write(docX, context));
        } else {
            clearOldCancerData(docX);
            cancers.forEach(cancer -> cancer.write(docX, context));
        }
    }

    /**
     * Clears any cancer and qualifier data that is currently stored.
     *
     * @param docX the {@link XWikiDocument} that contains the data
     */
    private void clearOldCancerData(@Nonnull final XWikiDocument docX)
    {
        docX.removeXObjects(Cancer.CLASS_REFERENCE);
        docX.removeXObjects(CancerQualifier.CLASS_REFERENCE);
    }

    /**
     * Merges stored and new cancer data.
     *
     * @param cancers the {@link PatientData} object that contains new cancer data
     * @param storedCancers the {@link PatientData} object that contains stored cancer data
     * @return a map of cancer identifiers to {@link Cancer} objects with merged data
     */
    @Nonnull
    private Map<String, Cancer> getMergedCancers(@Nonnull final PatientData<Cancer> cancers,
                                                 @Nullable final PatientData<Cancer> storedCancers)
    {
        final Stream<Cancer> storedCancerStream = storedCancers != null
                ? IntStream.range(0, storedCancers.size()).mapToObj(storedCancers::get)
                : Stream.empty();
        final Stream<Cancer> cancerStream = IntStream.range(0, cancers.size()).mapToObj(cancers::get);
        return Stream.concat(storedCancerStream, cancerStream)
            .collect(Collectors.toMap(this::getCancerValue, Function.identity(), this::mergeCancers,
                LinkedHashMap::new));
    }

    /**
     * Merges the data in {@code oldCancer} and {@code newCancer}. Any conflicts in data will be resolved in favour of
     * {@code newCancer}.
     *
     * @param oldCancer the {@link Cancer} object with currently stored cancer data
     * @param newCancer the {@link Cancer} object with updated cancer data
     * @return the {@link Cancer} object with merged cancer data
     */
    @Nonnull
    private Cancer mergeCancers(@Nonnull final Cancer oldCancer, @Nonnull final Cancer newCancer)
    {
        return oldCancer.mergeData(newCancer);
    }

    private String getCancerValue(@Nonnull final Cancer cancer)
    {
        return StringUtils.defaultIfBlank(cancer.getId(), cancer.getName());
    }
}
