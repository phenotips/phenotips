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
package org.phenotips.rest;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Uses JodaTime's DateTime for representing dates in REST resources instead of JAXB's default,
 * {@code XMLGregorianCalendar}.
 *
 * @version $Id$
 * @since 1.2M5
 */
public class DateTimeAdapter extends XmlAdapter<String, DateTime>
{
    private static final DateTimeFormatter ISO_DATETIME_FORMATTER =
        ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC);

    @Override
    public DateTime unmarshal(String v)
    {
        if (StringUtils.isBlank(v)) {
            return null;
        }
        return DateTime.parse(v);
    }

    @Override
    public String marshal(DateTime v)
    {
        if (v == null) {
            return null;
        }
        return ISO_DATETIME_FORMATTER.print(v);
    }
}
