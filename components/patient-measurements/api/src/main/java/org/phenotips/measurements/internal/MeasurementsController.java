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
package org.phenotips.measurements.internal;

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.measurements.MeasurementHandler;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Handles the patient's measurements.
 *
 * @version $Id$
 * @since 1.0M10
 */
@Component(roles = { PatientDataController.class })
@Named("measurements")
@Singleton
public class MeasurementsController implements PatientDataController<MeasurementsController.MeasurementEntry>
{
    private static final String XCLASS = "PhenoTips.MeasurementClass";

    private static final String DATE = "date";

    private static final String AGE = "age";

    private static final String TYPE = "type";

    private static final String SIDE = "side";

    private static final String VALUE = "value";

    private static final String UNIT = "unit";

    private static final String SD = "sd";

    private static final String PERCENTILE = "precentile";

    private static final String DATE_FORMAT = "yyyy-MM-dd";

    @Inject
    private Logger logger;

    /** Provides access to the underlying data storage. */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /** Parses string representations of document references into proper references. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> stringResolver;

    /** Provides access to the current execution context. */
    @Inject
    private Execution execution;

    @Override
    public PatientData<MeasurementEntry> load(Patient patient)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            List<BaseObject> objects = doc.getXObjects(getXClassReference());
            if (objects == null || objects.isEmpty()) {
                return null;
            }

            List<MeasurementHandler> handlers = getHandlers();
            List<MeasurementEntry> result = new LinkedList<>();

            for (BaseObject object : objects) {
                if (object == null) {
                    continue;
                }
                try {
                    final String age = object.getStringValue(AGE);
                    final Date date = object.getDateValue(DATE);
                    final String type = object.getStringValue(TYPE);
                    final String side = object.getStringValue(SIDE);
                    final Double value = object.getDoubleValue(VALUE);

                    if (date == null && age.isEmpty()) {
                        throw new Exception("Age or date is missing");
                    }
                    if (type.isEmpty()) {
                        throw new Exception("Type is missing");
                    }
                    // getting measurement units, could be null, but should not be
                    MeasurementHandler handler = getHandler(type, handlers);
                    final String units = handler.getUnit();

                    MeasurementEntry entry = new MeasurementEntry(date, age, type, side, value, units);
                    result.add(entry);
                } catch (Exception e) {
                    this.logger.error("Failed to load a particular measurement", e.getMessage());
                }
            }

            return new IndexedPatientData<>(getName(), result);
        } catch (Exception e) {
            this.logger.error("Could not find requested document or some unforeseen"
                + " error has occurred during measurements controller loading ", e.getMessage());
        }
        return null;
    }

    private EntityReference getXClassReference()
    {
        return stringResolver.resolve(XCLASS);
    }

    private List<MeasurementHandler> getHandlers()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager().getInstanceList(MeasurementHandler.class);
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to find component", e);
        }
        return Collections.emptyList();
    }

    private MeasurementHandler getHandler(String name, List<MeasurementHandler> handlers)
    {
        // would be better if a lambda was returned in getHandlers()
        for (MeasurementHandler handler : handlers) {
            if (StringUtils.equals(handler.getName(), name)) {
                return handler;
            }
        }
        return null;
    }

    @Override
    public void save(Patient patient)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            XWikiContext context = (XWikiContext) this.execution.getContext().getProperty("xwikicontext");

            PatientData<MeasurementEntry> entries = patient.getData(getName());
            if (!entries.isIndexed()) {
                return;
            }
            Iterator<MeasurementEntry> iterator = entries.iterator();
            while (iterator.hasNext()) {
                MeasurementEntry entry = iterator.next();
                BaseObject wikiObject = doc.newXObject(this.getXClassReference(), context);
                wikiObject.set(DATE, entry.getDate(), context);
                wikiObject.set(AGE, entry.getAge(), context);
                wikiObject.set(TYPE, entry.getType(), context);
                wikiObject.set(SIDE, entry.getSide(), context);
                wikiObject.set(VALUE, entry.getValue(), context);
            }

            context.getWiki().saveDocument(doc, "Updated measurements from JSON", true, context);
        } catch (Exception e) {
            this.logger.error("Failed to save measurements: [{}]", e.getMessage());
        }
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json)
    {
        this.writeJSON(patient, json, null);
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        if (selectedFieldNames == null || selectedFieldNames.contains(getName())) {

            PatientData<MeasurementEntry> data = patient.getData(getName());
            if (data == null) {
                return;
            }

            JSONArray result = new JSONArray();
            Iterator<MeasurementEntry> iterator = data.iterator();
            List<MeasurementHandler> handlers = getHandlers();
            while (iterator.hasNext()) {
                MeasurementEntry entry = iterator.next();
                JSONObject jsonEntry = entryToJson(entry);
                jsonEntry.putAll(computeDependantInfo(entry, patient, handlers));
                result.add(jsonEntry);
            }

            if (!result.isEmpty()) {
                json.put(getName(), result);
            }
        }
    }

    private Map<String, Object> computeDependantInfo(MeasurementEntry entry,
        Patient patient, List<MeasurementHandler> handlers)
    {
        // not adding these constants into the class definition yet
        String maleConst = "M";
        String femaleConst = "F";
        // an age string and type must be present
        if (StringUtils.isBlank(entry.getAge()) || StringUtils.isBlank(entry.getType()) || entry.getValue() == null) {
            return new HashMap<>();
        }
        Double ageInMonths = MeasurementUtils.convertAgeStrToNumMonths(entry.getAge());

        // getting gender for computations
        PatientData<String> sexData = patient.getData("sex");
        if (sexData != null) {
            String sex = sexData.getValue();
            if (StringUtils.isNotBlank(sex)) {
                // ideally, the constants from the SexController should be used here,
                // but that would introduce a hard dependency. Maybe add it to constants?
                if (StringUtils.equals(sex, maleConst) || StringUtils.equals(sex, femaleConst)) {
                    // has to be either male or female, otherwise how can we compute SD?
                    // finding the handler
                    MeasurementHandler handler = getHandler(entry.getType(), handlers);
                    Boolean isMale = StringUtils.equals(maleConst, sex);
                    Double sd = handler.valueToStandardDeviation(isMale, ageInMonths.floatValue(), entry.getValue());
                    Integer percentile = handler.valueToPercentile(isMale, ageInMonths.floatValue(), entry.getValue());

                    Map<String, Object> result = new HashMap<>();
                    result.put(SD, sd);
                    result.put(PERCENTILE, percentile);
                    return result;
                }
            }
        }
        return new HashMap<>();
    }

    private JSONObject entryToJson(MeasurementEntry entry)
    {
        JSONObject json = new JSONObject();
        if (entry.getDate() != null) {
            // not efficient to create a new one for every entry
            SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
            json.put(DATE, formatter.format(entry.getDate()));
        }
        json.put(AGE, entry.getAge());
        json.put(TYPE, entry.getType());
        if (StringUtils.isNotBlank(entry.getSide())) {
            json.put(SIDE, entry.getSide());
        }
        json.put(VALUE, entry.getValue());
        json.put(UNIT, entry.getUnits());
        return json;
    }

    @Override
    public PatientData<MeasurementEntry> readJSON(JSONObject json)
    {
        try {
            if (json == null || json.get(getName()) == null) {
                return null;
            }
            // if not array, will err
            JSONArray entries = json.getJSONArray(getName());
            List<MeasurementEntry> measurements = new LinkedList<>();
            if (entries.isEmpty()) {
                return null;
            }

            for (Object e : entries) {
                try {
                    JSONObject entry = JSONObject.fromObject(e);
                    measurements.add(jsonToEntry(entry));
                } catch (Exception er) {
                    this.logger.error("Could not read a particular JSON block", er.getMessage());
                }
            }

            if (measurements.isEmpty()) {
                return null;
            }
            return new IndexedPatientData<>(getName(), measurements);
        } catch (Exception e) {
            this.logger.error("Could not read JSON", e.getMessage());
        }
        return null;
    }

    private MeasurementEntry jsonToEntry(JSONObject json) throws ParseException
    {
        final JSONObject j = json;
        // not efficient to create a new one for every entry
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
        final Date date =
            j.getString(DATE) != null ? formatter.parse(j.getString(DATE)) : null;

        return new MeasurementEntry(
            date, j.getString(AGE), j.getString(TYPE), j.getString(SIDE), j.getDouble(VALUE), j.getString(UNIT));
    }

    @Override
    public String getName()
    {
        return "measurements";
    }

    /** A class that represents a measurement entry with easy access. */
    public class MeasurementEntry
    {
        private Date date;

        private String age;

        private String type;

        private String side;

        private Double value;

        private String units;

        /** The default constructor that takes all of the data stored in the patient record.
         * @param date see the doc for the respective method
         * @param age see the doc for the respective method
         * @param type see the doc for the respective method
         * @param side see the doc for the respective method
         * @param value see the doc for the respective method
         * @param units see the doc for the respective method
         */
        public MeasurementEntry(Date date, String age, String type, String side, Double value, String units)
        {
            this.date = date;
            this.age = age;
            this.type = type;
            this.side = side;
            this.value = value;
            this.units = units;
        }

        /** @return the date of the measurement */
        public Date getDate()
        {
            return this.date;
        }

        /** @return string representing age */
        public String getAge()
        {
            return this.age;
        }

        /** @return the name of the measurement handler */
        public String getType()
        {
            return this.type;
        }

        /** @return a letter representing the side, if applicable */
        public String getSide()
        {
            return this.side;
        }

        /** @return the measurement itself */
        public Double getValue()
        {
            return this.value;
        }

        /** @return the units that the value is measured in */
        public String getUnits()
        {
            return this.units;
        }
    }
}
