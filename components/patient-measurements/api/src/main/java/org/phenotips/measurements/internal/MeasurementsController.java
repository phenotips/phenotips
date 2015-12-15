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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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

@Component(roles = { PatientDataController.class })
@Named("measurements")
@Singleton
public class MeasurementsController implements PatientDataController<MeasurementsController.MeasurementEntry>
{
    private final static String xClass = "PhenoTips.MeasurementClass";

    private final static String DATE = "date";

    private final static String AGE = "age";

    private final static String TYPE = "type";

    private final static String SIDE = "side";

    private final static String VALUE = "value";

    private final static String UNIT = "unit";

    private final static String DATE_FORMAT = "yyyy-MM-dd";

    @Inject
    private Logger logger;

    /** Provides access to the underlying data storage. */
    @Inject
    protected DocumentAccessBridge documentAccessBridge;

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

                    MeasurementEntry entry = new MeasurementEntry()
                    {
                        @Override public Date getDate()
                        {
                            return date;
                        }

                        @Override public String getAge()
                        {
                            return age;
                        }

                        @Override public String getType()
                        {
                            return type;
                        }

                        @Override public String getSide()
                        {
                            return side;
                        }

                        @Override public Double getValue()
                        {
                            return value;
                        }

                        @Override public String getUnits()
                        {
                            return units;
                        }
                    };
                    result.add(entry);
                } catch (Exception e) {
                    this.logger.error("Failed to load a particular measurement", e.getMessage());
                }
            }

            if (result.isEmpty()) {
                return null;
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
        return stringResolver.resolve(xClass);
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
            while (iterator.hasNext()) {
                MeasurementEntry entry = iterator.next();
                result.add(entryToJson(entry));
                // todo. add additional info
            }

            if (!result.isEmpty()) {
                json.put(getName(), result);
            }
        }
    }

    private JSONObject entryToJson(MeasurementEntry entry)
    {
        JSONObject json = new JSONObject();
        if (entry.getDate() != null) {
            // not efficient to create a new one for every entry
            SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
            json.put("date", formatter.format(entry.getDate()));
        }
        json.put("age", entry.getAge());
        json.put("type", entry.getType());
        if (StringUtils.isNotBlank(entry.getSide())) {
            json.put("side", entry.getSide());
        }
        json.put("value", entry.getValue());
        json.put("unit", entry.getUnits());
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

        return new MeasurementEntry()
        {
            @Override public Date getDate()
            {
                return date;
            }

            @Override public String getAge()
            {
                return j.getString(AGE);
            }

            @Override public String getType()
            {
                return j.getString(TYPE);
            }

            @Override public String getSide()
            {
                return j.getString(SIDE);
            }

            @Override public Double getValue()
            {
                return j.getDouble(VALUE);
            }

            @Override public String getUnits()
            {
                // although this does not get saved, and currently affects nothing
                return j.getString(UNIT);
            }
        };
    }

    @Override
    public String getName()
    {
        return "measurements";
    }

    abstract public class MeasurementEntry
    {
        abstract public Date getDate();

        abstract public String getAge();

        abstract public String getType();

        abstract public String getSide();

        abstract public Double getValue();

        abstract public String getUnits();
    }
}
