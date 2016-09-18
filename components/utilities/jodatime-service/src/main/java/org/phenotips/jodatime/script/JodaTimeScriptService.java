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
    /** ISO8601 date formatter, printing dates in the format {@code 2000-12-31}. */
    private static final DateTimeFormatter ISO_DATE_FORMATTER = ISODateTimeFormat.date().withZone(DateTimeZone.UTC);

    /** ISO8601 time formatter, printing dates in the format {@code 23:00:00.123Z}. */
    private static final DateTimeFormatter ISO_TIME_FORMATTER = ISODateTimeFormat.time().withZone(DateTimeZone.UTC);

    /** ISO8601 date and time formatter, printing dates in the format {@code 2000-12-31T23:00:00.123Z}. */
    private static final DateTimeFormatter ISO_DATETIME_FORMATTER = ISODateTimeFormat.dateTime().withZone(
        DateTimeZone.UTC);

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
     *         minute, or asking for April 31
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
     * @param instant the target moment, in milliseconds from 1970-01-01T00:00:00Z
     * @return a Joda Time {@link DateTime} object for the requested moment
     * @see org.joda.time.DateTime#DateTime(long)
     */
    public DateTime getDateTime(long instant)
    {
        return new DateTime(instant);
    }

    /**
     * Get the current date and time as a mutable object.
     *
     * @return a Joda Time {@link MutableDateTime} object for the current moment
     * @see org.joda.time.MutableDateTime#MutableDateTime()
     */
    public MutableDateTime getMutableDateTime()
    {
        return new MutableDateTime();
    }

    /**
     * Get a mutable datetime object representing the specified moment.
     *
     * @param year the target year, in the standard chronology
     * @param monthOfYear the month of the year, from 1 to 12
     * @param dayOfMonth the day of the month, from 1 to 31
     * @param hourOfDay the hour of the day, from 0 to 23
     * @param minuteOfHour the minute of the hour, from 0 to 59
     * @param secondOfMinute the second of the minute, from 0 to 59
     * @param millisOfSecond the millisecond of the second, from 0 to 999
     * @return a Joda Time {@link MutableDateTime} object for the requested moment, if the provided parameters represent
     *         a valid date, or {@code null} if the date is not valid, for example asking for an invalid month, hour,
     *         minute, or asking for April 31
     * @see org.joda.time.MutableDateTime#MutableDateTime(int, int, int, int, int, int, int)
     */
    public MutableDateTime getMutableDateTime(int year, int monthOfYear, int dayOfMonth, int hourOfDay,
        int minuteOfHour, int secondOfMinute, int millisOfSecond)
    {
        return new MutableDateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute,
            millisOfSecond);
    }

    /**
     * Get a mutable datetime object representing the specified moment.
     *
     * @param instant the target moment, in milliseconds from 1970-01-01T00:00:00Z
     * @return a Joda Time {@link MutableDateTime} object for the requested moment
     * @see org.joda.time.MutableDateTime#MutableDateTime(long)
     */
    public MutableDateTime getMutableDateTime(long instant)
    {
        return new MutableDateTime(instant);
    }

    /**
     * @return an ISO8601 date formatter, printing dates in the format {@code 2000-12-31}
     */
    public DateTimeFormatter getISODateFormatter()
    {
        return ISO_DATE_FORMATTER;
    }

    /**
     * @return an ISO8601 time formatter, printing dates in the format {@code 23:00:00.123Z}
     */
    public DateTimeFormatter getISOTimeFormatter()
    {
        return ISO_TIME_FORMATTER;
    }

    /**
     * @return an ISO8601 date and time formatter, printing dates in the format {@code 2000-12-31T23:00:00.123Z}
     */
    public DateTimeFormatter getISODateTimeFormatter()
    {
        return ISO_DATETIME_FORMATTER;
    }

    /**
     * Get a datetime formatter for the specified pattern.
     *
     * @param pattern the format used by the returned formatter, both for parsing and printing
     * @return a formatter object set up with the current locale, if the specified format is correct, or {@code null} if
     *         the format is invalid
     * @see org.joda.time.format.DateTimeFormat
     * @see org.joda.time.format.DateTimeFormat#forPattern(String)
     */
    public DateTimeFormatter getDateTimeFormatterForPattern(String pattern)
    {
        try {
            return DateTimeFormat.forPattern(pattern).withLocale(this.localizationContext.getCurrentLocale());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Get a datetime formatter for the specified date, time or datetime style. The style is specified as a two letter
     * code. The first character is the date style, and the second character is the time style. Specify a character of
     * 'S' for short style, 'M' for medium, 'L' for long, and 'F' for full. A date or time may be omitted by specifying
     * a style character '-'. The actual format used depends on the locale.
     *
     * @param style the type of date/time used by the returned formatter, both for parsing and printing; valid values
     *            contain two characters from the set {@code "S", "M", "L", "F", "-"}
     * @return a formatter object set up with the current locale, if the specified style is correct, or {@code null} if
     *         the style is invalid
     * @see org.joda.time.format.DateTimeFormat
     * @see org.joda.time.format.DateTimeFormat#forStyle(String)
     */
    public DateTimeFormatter getDateTimeFormatterForStyle(String style)
    {
        try {
            return DateTimeFormat.forStyle(style).withLocale(this.localizationContext.getCurrentLocale());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * @return the server's default timezone
     * @see org.joda.time.DateTimeZone#getDefault()
     */
    public DateTimeZone getServerTimezone()
    {
        return DateTimeZone.getDefault();
    }

    /**
     * @return the UTC timezone
     * @see org.joda.time.DateTimeZone#UTC
     */
    public DateTimeZone getUTCTimezone()
    {
        return DateTimeZone.UTC;
    }

    /**
     * @param locationOrOffset the standard identifier of the timezone (e.g. EST), or the official name of the location
     *            (e.g. America/Chicago), or a standard offset in the format {@code [+-]hh:mm} (e.g. +02:00)
     * @return the requested timezone, or {@code null} if the argument is not valid
     * @see org.joda.time.DateTimeZone#forID(String)
     */
    public DateTimeZone getTimezone(String locationOrOffset)
    {
        try {
            return DateTimeZone.forID(locationOrOffset);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * @param offsetHours the number of hours to offset from UTC, positive or negative integer
     * @return a timezone with the requested offset
     * @see #getTimezone(int, int)
     * @see org.joda.time.DateTimeZone#forOffsetHours(int)
     */
    public DateTimeZone getTimezone(int offsetHours)
    {
        return DateTimeZone.forOffsetHours(offsetHours);
    }

    /**
     * @param offsetHours the number of hours to offset from UTC, positive or negative integer
     * @param offsetMinutes the number of minutes to offset from UTC, a number between {@code 0} and {@code 59}
     * @return a timezone with the requested offset, or {@code null} if the {@code offsetMinutes} parameter is wrong
     * @see org.joda.time.DateTimeZone#forOffsetHoursMinutes(int, int)
     */
    public DateTimeZone getTimezone(int offsetHours, int offsetMinutes)
    {
        try {
            return DateTimeZone.forOffsetHoursMinutes(offsetHours, offsetMinutes);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Get a duration object equal to the specified number of milliseconds.
     *
     * @param millis the number of milliseconds to represent, both positive and negative values are accepted
     * @return the requested duration
     * @see org.joda.time.Duration#Duration(long)
     */
    public Duration getDuration(long millis)
    {
        return new Duration(millis);
    }

    /**
     * Compute the duration between two different moments.
     *
     * @param from the start moment
     * @param to the end moment
     * @return the difference between the two moments, or {@code null} if the parameters are invalid or the duration
     *         exceeds 64 bits
     * @see org.joda.time.Duration#Duration(ReadableInstant, ReadableInstant)
     */
    public Duration getDuration(ReadableInstant from, ReadableInstant to)
    {
        if (from == null || to == null) {
            return null;
        }
        try {
            return new Duration(from, to);
        } catch (ArithmeticException ex) {
            return null;
        }
    }
}
