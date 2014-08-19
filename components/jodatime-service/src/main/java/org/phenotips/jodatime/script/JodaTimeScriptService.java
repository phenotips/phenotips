/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.phenotips.jodatime.script;

import org.xwiki.component.annotation.Component;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.IllegalFieldValueException;
import org.joda.time.MutableDateTime;
import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Script service for manipulating dates using the <a href="http://www.joda.org/joda-time/">JodaTime framework</a>, a
 * quality replacement for the Java date and time classes.
 *
 * @version $Id$
 * @since 1.0RC1
 */
@Unstable
@Component
@Named("jodatime")
@Singleton
public class JodaTimeScriptService implements ScriptService
{
    /** ISO8601 date time formatter, printing dates in the format {@code 2000-12-31T23:00:00.123Z}. */
    private static final DateTimeFormatter ISO_DATE_FORMATTER = ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC);

    /** Provides access to the currently active locale. */
    @Inject
    private LocalizationContext localizationContext;

    /**
     * Get the current date and time.
     *
     * @return a Joda Time {@link DateTime} object for the current moment
     * @see org.joda.time.DateTime#DateTime()
     */
    public DateTime getDateTime()
    {
        return new DateTime();
    }

    /**
     * Get a datetime object representing the specified moment.
     *
     * @param year the target year, in the standard chronology
     * @param monthOfYear the month of the year, from 1 to 12
     * @param dayOfMonth the day of the month, from 1 to 31
     * @param hourOfDay the hour of the day, from 0 to 23
     * @param minuteOfHour the minute of the hour, from 0 to 59
     * @param secondOfMinute the second of the minute, from 0 to 59
     * @param millisOfSecond the millisecond of the second, from 0 to 999
     * @return a Joda Time {@link DateTime} object for the requested moment, if the provided parameters represent a
     *         valid date, or {@code null} if the date is not valid, for example asking for an invalid month, hour,
     *          minute, or asking for April 31
     * @see org.joda.time.DateTime#DateTime(int, int, int, int, int, int, int)
     */
    public DateTime getDateTime(int year, int monthOfYear, int dayOfMonth, int hourOfDay, int minuteOfHour,
        int secondOfMinute, int millisOfSecond)
    {
        try {
            return new DateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute, millisOfSecond);
        } catch (IllegalFieldValueException ex) {
            // TODO Put a message in the script logs
            return null;
        }
    }

    /**
     * Get a datetime object representing the specified moment.
     *
     * @param instant the 
     * @see org.joda.time.DateTime#DateTime(long)
     */
    public DateTime getDateTime(long instant)
    {
        return new DateTime(instant);
    }

    /**
     * @see org.joda.time.MutableDateTime#MutableDateTime()
     */
    public MutableDateTime getMutableDateTime()
    {
        return new MutableDateTime();
    }

    /**
     * @see org.joda.time.MutableDateTime#MutableDateTime(int, int, int, int, int, int, int)
     */
    public MutableDateTime getMutableDateTime(int year, int monthOfYear, int dayOfMonth, int hourOfDay,
        int minuteOfHour, int secondOfMinute, int millisOfSecond)
    {
        return new MutableDateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute,
            millisOfSecond);
    }

    /**
     * @see org.joda.time.MutableDateTime#MutableDateTime(long)
     */
    public MutableDateTime getMutableDateTime(long instant)
    {
        return new MutableDateTime(instant);
    }

    /**
     * @see org.joda.time.format.DateTimeFormat#forPattern(String)
     */
    public DateTimeFormatter getDateTimeFormatterForPattern(String pattern)
    {
        return DateTimeFormat.forPattern(pattern).withLocale(this.localizationContext.getCurrentLocale());
    }

    /**
     * @see org.joda.time.format.DateTimeFormat#forStyle(String)
     */
    public DateTimeFormatter getDateTimeFormatterForStyle(String style)
    {
        return DateTimeFormat.forStyle(style).withLocale(this.localizationContext.getCurrentLocale());
    }

    /**
     * @see org.joda.time.DateTimeZone#getDefault()
     */
    public DateTimeZone getServerTimezone()
    {
        return DateTimeZone.getDefault();
    }

    /**
     * @see org.joda.time.DateTimeZone#UTC
     */
    public DateTimeZone getUTCTimezone()
    {
        return DateTimeZone.UTC;
    }

    /**
     * @see org.joda.time.DateTimeZone#forID(String)
     */
    public DateTimeZone getTimezone(String locationOrOffset)
    {
        return DateTimeZone.forID(locationOrOffset);
    }

    /**
     * @see org.joda.time.DateTimeZone#forOffsetHours(int)
     */
    public DateTimeZone getTimezone(int offsetHours)
    {
        return DateTimeZone.forOffsetHours(offsetHours);
    }

    /**
     * @see org.joda.time.DateTimeZone#forOffsetHoursMinutes(int, int)
     */
    public DateTimeZone getTimezone(int offsetHours, int offsetMinutes)
    {
        return DateTimeZone.forOffsetHoursMinutes(offsetHours, offsetMinutes);
    }

    /**
     * @see org.joda.time.Duration#Duration(long)
     */
    public Duration getDuration(long millis)
    {
        return new Duration(millis);
    }

    /**
     * @see org.joda.time.Duration#Duration(ReadableInstant, ReadableInstant)
     */
    public Duration getDuration(ReadableInstant from, ReadableInstant to)
    {
        return new Duration(from, to);
    }

    /**
     * @return an ISO8601 date time formatter
     * @since 5.2RC1
     */
    public DateTimeFormatter getISODateTimeFormatter()
    {
        return ISO_DATE_FORMATTER;
    }
}
