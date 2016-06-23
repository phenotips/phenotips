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
package org.phenotips.data.internal;

import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

/**
 * Implementation of a fuzzy date for internal use in Phenotips.
 *
 * @version $Id$
 * @since 1.3M2
 */
public class PhenoTipsDate
{
    /** JSON field names for year. Value should be an integer; negative values represent years BC. */
    public static final String JSON_YEAR_FIELDNAME = "year";
    /** JSON field names for month. Value should be a positive integer between 1 and 12. */
    public static final String JSON_MONTH_FIELDNAME = "month";
    /** JSON field names for day. Value should be a positive integer between 1 and 31. */
    public static final String JSON_DAY_FIELDNAME = "day";
    /** JSON field names for a `range` object (representing the degree of `fuzzyness` of this date). */
    public static final String JSON_RANGE_FIELDNAME = "range";
    /** JSON field name of the year part of fuzzyness. Value should be a positive integer. */
    public static final String JSON_RANGE_YEARS = "years";

    /** Regular expression corresponding to accepted date strings. */
    public static final Pattern DATE_STRING_REGEXP = Pattern.compile("(\\d\\d\\d\\d)(s)?(-(\\d\\d)(-(\\d\\d))?)?");

    /** JSON field name of the deprecated decades field. */
    private static final String DEPRECATED_JSON_DECADE = "decade";
    /** Regular expression for the decades field. */
    private static final Pattern DEPRECATED_DECADE_REGEXP = Pattern.compile("(\\d\\d\\d\\d)s");
    /** Range equivalent to having a decade. */
    private static final Integer DEPRECATED_DECADE_RANGE = 10;

    private static final Integer MIN_DAY = 1;
    private static final Integer MAX_DAY = 31;
    private static final Integer MIN_MONTH = 1;
    private static final Integer MAX_MONTH = 12;

    private final Integer year;
    private final Integer month;
    private final Integer day;
    // for ranges, both `null` and 0 means `exact, no fuzziness`
    private final Integer rangeYears;

    /**
     * Initialize from a date object. Note that time of day data will be lost.
     *
     * @param date a date object. Null date will result in an undefined date.
     */
    public PhenoTipsDate(Date date)
    {
        this.rangeYears = null;
        if (date != null) {
            // TODO: would be nice to be able to use Java8's LocalDate
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            this.year = calendar.get(Calendar.YEAR);
            // Calendar months are 0 to 11, months in this class are 1 to 12
            this.month = calendar.get(Calendar.MONTH) + 1;
            this.day = calendar.get(Calendar.DAY_OF_MONTH);
        } else {
            this.year = null;
            this.month = null;
            this.day = null;
        }
    }

    /**
     * Initialize from a JSON object. It is assumed to contain the fields defined above
     * (JSON_YEAR_FIELDNAME, etc). Partial input is supported, e.g. a month can be defined while
     * a year is not.
     *
     * @param jsonFuzzyDate a JSON object with all or some of the supported fields.
     * Incorrect input is treated as no input.
     */
    public PhenoTipsDate(JSONObject jsonFuzzyDate)
    {
        // treat null input as empty input
        JSONObject useFuzzyDate = (jsonFuzzyDate == null) ? new JSONObject() : jsonFuzzyDate;

        // check if the JSON is in the deprecated format which includes a "decade" field
        // (included to allow importing JSONs exported from old versions of PhenoTips)
        if (useFuzzyDate.has(DEPRECATED_JSON_DECADE)) {
            // just convert JSON to a modern format and use regular serializers
            Matcher m = DEPRECATED_DECADE_REGEXP.matcher(useFuzzyDate.optString(DEPRECATED_JSON_DECADE, ""));
            if (m.find()) {
                // remove the deprecated field in any case
                useFuzzyDate.remove(DEPRECATED_JSON_DECADE);
                // if there was only decade and no year, convert it to the current "year+range" format
                if (!useFuzzyDate.has(JSON_YEAR_FIELDNAME)) {
                    JSONObject range = new JSONObject();
                    range.put(JSON_RANGE_YEARS, DEPRECATED_DECADE_RANGE);
                    useFuzzyDate.put(JSON_RANGE_FIELDNAME, range);

                    Integer firstYearOfDecade = stringToIntegerInRange(m.group(1), null, null);
                    useFuzzyDate.put(JSON_YEAR_FIELDNAME, firstYearOfDecade);
                    useFuzzyDate.put(JSON_RANGE_FIELDNAME, range);
                }
            }
        }

        Integer inputYear = getNumericValueFromJSON(useFuzzyDate, JSON_YEAR_FIELDNAME, null, null);
        if (inputYear == null) {
            this.rangeYears = null;
            this.year = null;
        } else {
            this.year = inputYear;
            JSONObject rangeObject = useFuzzyDate.optJSONObject(JSON_RANGE_FIELDNAME);
            if (rangeObject != null && rangeObject.optInt(JSON_RANGE_YEARS, -1) > 0) {
                this.rangeYears = rangeObject.getInt(JSON_RANGE_YEARS);
            } else {
                this.rangeYears = null;
            }
        }
        this.month = getNumericValueFromJSON(useFuzzyDate, JSON_MONTH_FIELDNAME, MIN_MONTH, MAX_MONTH);
        this.day = getNumericValueFromJSON(useFuzzyDate, JSON_DAY_FIELDNAME, MIN_DAY, MAX_DAY);
    }

    /**
     * Initialize from a string, which may either be an ISO date in the "yyyy-mm-dd" format,
     * or a modified ISO string where the year is followed by an "s" to indicade that decade only is known.
     * Also supported: "yyyy[s]-mm" and "yyyy[s]". Formally, the format is: "yyyy[s][-mm[-dd]]".
     *
     * @param phenoTipsDateString a date string.
     */
    public PhenoTipsDate(String phenoTipsDateString)
    {
        // treat null input as empty input
        String useDateString = (phenoTipsDateString == null) ? "" : phenoTipsDateString;

        // string: "1920s" or "1990" or "1990-05" or "1990-01-21" or "1990s-01-01"
        Matcher m = DATE_STRING_REGEXP.matcher(useDateString);
        if (m.find()) {
            // note: month (#4) and day (#6) groups may be null, that is supported
            this.year  = stringToIntegerInRange(m.group(1), null, null);
            this.month = stringToIntegerInRange(m.group(4), MIN_MONTH, MAX_MONTH);
            this.day   = stringToIntegerInRange(m.group(6), MIN_DAY, MAX_DAY);
            if (m.group(2) != null) {
                this.rangeYears = 10;
            } else {
                this.rangeYears = null;
            }
        } else {
            this.rangeYears = null;
            this.year = null;
            this.month = null;
            this.day = null;
        }
    }

    // Accepts both numeric and string representations of a number.
    // If range is given, returns null if the number is not in range
    private Integer getNumericValueFromJSON(JSONObject json, String key, Integer validRangeMin, Integer validRangeMax)
    {
        if (!json.has(key)) {
            return null;
        }

        // check if the key is an integer
        int intValue = json.optInt(key, Integer.MIN_VALUE);
        if (intValue != Integer.MIN_VALUE) {
            if (valueInRange(intValue, validRangeMin, validRangeMax)) {
                return intValue;
            }
            return null;
        }

        // check if the key is a string and try to parse the value from it
        return stringToIntegerInRange(json.optString(key, ""), validRangeMin, validRangeMax);
    }

    // If range is given, returns null if the number is not in range
    private Integer stringToIntegerInRange(String str, Integer validRangeMin, Integer validRangeMax)
    {
        try {
            Integer value = Integer.parseInt(str);
            if (!valueInRange(value, validRangeMin, validRangeMax)) {
                return null;
            }
            return value;
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean valueInRange(Integer value, Integer validRangeMin, Integer validRangeMax)
    {
        if (validRangeMin != null && value < validRangeMin) {
            return false;
        }
        if (validRangeMax != null && value > validRangeMax) {
            return false;
        }
        return true;
    }

    /**
     * Returns some values even if they do not represent a valid date, e.g. day without a year.
     *
     * @return JSONObject representing this date. It may be empty (but not null) if no data is available.
     */
    public JSONObject toJSON()
    {
        JSONObject result = new JSONObject();
        if (this.year != null) {
            result.put(JSON_YEAR_FIELDNAME, this.year);
        }
        if (this.month != null) {
            result.put(JSON_MONTH_FIELDNAME, this.month);
        }
        if (this.day != null) {
            result.put(JSON_DAY_FIELDNAME, this.day);
        }
        if (this.rangeYears != null) {
            JSONObject rangeObject = new JSONObject();
            rangeObject.put(JSON_RANGE_YEARS, this.rangeYears);
            result.put(JSON_RANGE_FIELDNAME, rangeObject);
        }
        return result;
    }

    /**
     * If this object does not hold any useful information (the year isnot defined in any way, even
     * with an up-to-decade precision) `null` is returned.
     *
     * @return Date a java Date object representing the earliest posible date this fuzzy PhenoTips
     * date may represent.
     */
    public Date toEarliestPossibleISODate()
    {
        if (this.year == null) {
            return null;
        }
        int earliestMonth = (this.month == null) ? 1 : this.month;
        int earliestDay = (this.day == null || this.month == null) ? 1 : this.day;

        Calendar calendar = Calendar.getInstance();
        // note: Calendar months are 0-based, so need to use "month - 1"
        calendar.set(this.year, earliestMonth - 1, earliestDay, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    @Override
    public String toString()
    {
        return toJSON().toString();
    }
}
