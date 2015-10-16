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

import org.phenotips.Constants;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration for PhenoTips issue PT-1969: Automatically migrate existing measurements to the new format. Searches for
 * all documents containing measurements, extracts the old measurements object and splits the data into several new
 * measurements objects. All data except age is copied; age is interpolated.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Component
@Named("R71291PhenoTips#1969")
@Singleton
public class R71291PhenoTips1969DataMigration extends AbstractHibernateDataMigration implements
    HibernateCallback<Object>
{
    private static final String DATE_FIELD = "date";

    private static final String AGE_FIELD = "age";

    private static final EntityReference PATIENT_CLASS = new EntityReference("PatientClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    private static final EntityReference MEASUREMENTS_CLASS = new EntityReference("MeasurementsClass",
        EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final EntityReference MEASUREMENT_CLASS = new EntityReference("MeasurementClass",
        EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

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

    /** Resolves class names to the current wiki. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> entityResolver;

    @Override
    public String getDescription()
    {
        return "Migrate existing measurements to the new format";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(71291);
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
        DocumentReference patientClassReference =
            R71291PhenoTips1969DataMigration.this.entityResolver.resolve(PATIENT_CLASS);
        DocumentReference oldClassReference =
            R71291PhenoTips1969DataMigration.this.entityResolver.resolve(MEASUREMENTS_CLASS);
        DocumentReference newClassReference =
            R71291PhenoTips1969DataMigration.this.entityResolver.resolve(MEASUREMENT_CLASS);

        Query q =
            session.createQuery("select distinct o.name from BaseObject o where o.className = '"
                + R71291PhenoTips1969DataMigration.this.serializer.serialize(oldClassReference) + "'");

        @SuppressWarnings("unchecked")
        List<String> documents = q.list();

        for (String docName : documents) {
            XWikiDocument doc =
                xwiki.getDocument(R71291PhenoTips1969DataMigration.this.resolver.resolve(docName), context);
            List<BaseObject> oldObjects = doc.getXObjects(oldClassReference);
            if (oldObjects.isEmpty()) {
                continue;
            }

            BaseObject patientObject = doc.getXObject(patientClassReference);
            Date birth = null;
            if (patientObject != null) {
                birth = patientObject.getDateValue("date_of_birth");
            }

            for (BaseObject object : oldObjects) {
                Date date = object.getDateValue(DATE_FIELD);
                String age = calculateAge(birth, date);
                Collection fieldList = object.getFieldList();
                migrateObject(date, age, fieldList, doc, context, newClassReference);
                doc.removeXObject(object);
            }
            doc.setComment("Migrated MeasurementsClass data into MeasurementClass instances");
            doc.setMinorEdit(true);
            try {
                // There's a bug in XWiki which prevents saving an object in the same session that it was loaded,
                // so we must clear the session cache first.
                session.clear();
                ((XWikiHibernateStore) getStore()).saveXWikiDoc(doc, context, false);
                session.flush();
            } catch (DataMigrationException e) {
                // We're in the middle of a migration, we're not expecting another migration
            }
        }
        return null;
    }

    private void migrateObject(Date date, String age, Collection fieldList, XWikiDocument doc, XWikiContext context,
        DocumentReference newClassReference) throws XWikiException
    {
        Map<String, WithSide> fieldNamesMap = mapNamesWithSides(fieldList.iterator());
        for (Object fieldUncast : fieldList) {
            BaseProperty field = (BaseProperty) fieldUncast;
            Object value = field.getValue();
            if (value != null && !(StringUtils.equals(field.getName(), DATE_FIELD)
                || StringUtils.equals(field.getName(), AGE_FIELD))) {
                BaseObject newObject = doc.newXObject(newClassReference, context);

                WithSide fieldInfo = fieldNamesMap.get(field.getName());

                newObject.setStringValue("type", fieldInfo.getName());
                newObject.setDoubleValue("value", (value instanceof Float) ? Double.valueOf((Float) value)
                    : null);
                newObject.setStringValue(AGE_FIELD, age);
                newObject.setDateValue(DATE_FIELD, date);
                if (fieldInfo.getSide() != null) {
                    newObject.setStringValue("side", fieldInfo.getSide());
                }
            }
        }
    }

    private String calculateAge(Date birth, Date measurement)
    {
        if (birth == null || measurement == null) {
            return "";
        }
        DateTime dtBirth = new DateTime(birth);
        DateTime dtMeasurement = new DateTime(measurement);
        Period age = new Period(dtBirth, dtMeasurement);
        StringBuilder ageString = new StringBuilder();
        ageString.append(age.getYears()).append("y")
            .append(age.getMonths()).append("m")
            .append(age.getDays()).append("d");
        return ageString.toString();
    }

    private Map<String, WithSide> mapNamesWithSides(Iterator<Object> fields)
    {
        final String rightMarker = "_right";

        Map<String, WithSide> mapping = new HashMap<>();
        Set<String> names = new HashSet<>();
        while (fields.hasNext()) {
            try {
                PropertyInterface field = (PropertyInterface) fields.next();
                names.add(field.getName());
            } catch (Exception ex) {
                // silently ignore
            }
        }
        for (final String fieldName : names) {
            if (fieldName.contains(rightMarker)) {
                mapping.put(fieldName, new WithSide(fieldName.replace(rightMarker, ""), "r"));
            } else if (names.contains(fieldName.concat(rightMarker))) {
                mapping.put(fieldName, new WithSide(fieldName, "l"));
            } else {
                mapping.put(fieldName, new WithSide(fieldName, null));
            }
        }
        return mapping;
    }

    private class WithSide
    {
        protected String name;

        protected String side;

        WithSide(String name, String side)
        {
            this.name = name;
            this.side = side;
        }

        public String getName()
        {
            return name;
        }

        public String getSide()
        {
            return side;
        }
    }

}
