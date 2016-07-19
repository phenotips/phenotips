/**
 * Class for storing either exact or fuzzy dates.
 *
 * Assert:
 *   if day is set => all of {year, month, day} are set
 *   if month is set => all of {year, month} are set
 *   no range is given => range in years == 1
 *
 * Note:
 *   month is from 1 to 12
 *   day starts at 1
 */
define([], function(){
    var PedigreeDate = Class.create({

        /**
         * Accepts either a string representation of date or a javascript Date object or an
         * object with {year[,month][,day]} string or integer fields
         */
        initialize: function(date) {
            this.range = { "years" : 1 };   // this should always be defined, even if year is unknown
            this.year  = null;
            this.month = null;
            this.day   = null;
            if (date == null || !date) return;

            if (typeof date === 'string' || date instanceof String) {
                // suport the "yyyy(s)-mm-dd" format
                var parsed = date.match(/(\d\d\d\d)(s)?-(\d\d)-(\d\d)/);
                if (parsed != null) {
                    if (parsed[2]) { // "s" is present
                        this.range.years = 10;
                    }
                    this.year  = this.parseIntOrNull(parsed[1]);
                    this.month = this.parseIntOrNull(parsed[3]);
                    this.day   = this.parseIntOrNull(parsed[4]);
                }
                else if (!isNaN(Date.parse(date))) {  // empty string also parses to NaN
                    // deal with timezone differences: treat all dates as being in the same timezone.
                    // for that need to parse input string, if posible, and extract day/month/year
                    // "as is", regardless of the timezone specified
                    // As of now, expected/supported format is "Tue Dec 09 00:00:00 UTC 2014"
                    var parsed = date.match(/\w\w\w (\w\w\w) (\d\d) \d\d:\d\d:\d\d \w\w\w (\d\d\d\d)/);
                    if (parsed !== null) {
                        // use Date("Dec 09, 2014") constructor
                        var timezonelessDate = parsed[1] + " " + parsed[2] + ", " + parsed[3];
                        jsDate = new Date(timezonelessDate);
                        this._initFromJSDate(new Date(date));
                    } else {
                        // parse any other format
                        this._initFromJSDate(new Date(date));
                    }
                }
            } else if (Object.prototype.toString.call(date) === '[object Date]') {
                this._initFromJSDate(date);
            } else if (typeof date === 'object') {
                // keep null-s, convert strings to integers
                date.hasOwnProperty("year")  && ( this.year   = this.parseIntOrNull(date.year) );
                date.hasOwnProperty("month") && ( this.month  = this.parseIntOrNull(date.month) );
                date.hasOwnProperty("day")   && ( this.day    = this.parseIntOrNull(date.day) );
                if (date.hasOwnProperty("range")) {
                    this.range = date.range;
                } else if (date.hasOwnProperty("decade")) {
                    // support deprecated format which included a "decade"
                    var parsed = date.decade.match(/(\d\d\d\d)s?/);
                    if (parsed) {
                        if (!this.year) {
                            this.year = parsed[1];
                            this.range = { "years" : 10 };
                        }
                    }
                }
            }
            if (this.month < 1 || this.month > 12) {
                this.month = null;
            }
            if (this.day < 1 || this.day > 31) {
                this.day = null;
            }
        },

        _initFromJSDate: function(date) {
            this.range = { "years" : 1 };
            this.year  = date.getFullYear();
            this.month = date.getMonth() + 1;   // js Date's months are 0 to 11, this.month is 1 to 12
            this.day   = daate.getDate();
        },

        parseIntOrNull: function(expectedInteger) {
            var result = parseInt(expectedInteger);
            if (isNaN(result)) {
                return null;
            }
            return result;
        },

        // Returns a string which will either be a year with an "s" at the end (e.g. "1990s") or
        // a string in one of "year", "monthName year", "datOfWeek monthName day year" format.
        // initialize() is expected to accept this string as valid input
        toString: function() {
            if (this.year === null) {
                return "";
            }
            if (this.range.years > 1) {
                return this.year.toString() + "s";
            }
            if (this.year !== null && this.month == null) {
                return this.year.toString();
            }
            if (this.year !== null && this.month !== null && this.day === null) {
                return this.getMonthName() + " " + this.year;
            }
            if (this.year !== null && this.month !== null && this.day !== null) {
                // note: Date.toDateString() is used for date-to-string conversions instead of
                //       Date.toUTCString because output of toDateString is the only one which seems
                //       to be uniformly supported by all browsers, new and old.
                var jsDate = this.toJSDate();
                return jsDate.toDateString();
            }
            return "";
        },

        getMonthName: function(locale) {
            if (this.getMonth() == null) return "";
            locale = locale && (locale in localeMonthNames) ? locale : 'en';
            return this._getMonthName(locale, this.getMonth() - 1);
        },

        _getMonthName: function(locale, month0based) {
            var localeMonthNames = {"en": ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'] };
            return localeMonthNames[locale][month0based];
        },

        // Returns a string which is a valid GEDCOM date (GEDCOME supports "ABT" keyword)
        toGEDCOMString: function() {
            if (this.year !== null && this.range.years > 1) {
                return "ABT " + this.getYear(true).toString();
            }
            return this.toString();
        },

        /** Returns true if any part of the date has been set. */
        isSet: function() {
            return (this.year !== null || this.month !== null || this.day !== null);
        },

        /** Returns true iff the minimum precision level is set, ie. the year. Useful for displaying the date. */
        isComplete: function() {
            return (this.year !== null);
        },

        onlyDecadeAvailable: function() {
            return (this.range.years > 1);
        },

        // Returns a string or null
        getDecade: function() {
            if (this.year == null) {
                return "";
            }
            return this.year + "s";
        },

        // Returns an object with some of the {range, year, month, day} fields set, depending on what data is available
        getSimpleObject: function() {
            var date = {};
            if (this.range.years != 1) date["range"] = this.range;
            if (this.year   !== null) date["year"]   = this.year;
            if (this.month  !== null) date["month"]  = this.month;
            if (this.day    !== null) date["day"]    = this.day;
            return date;
        },

        // Returns best possible estimation of this date as a javascript Date object.
        //
        // Aproximate dates (e.g. dates with range > 1) are set as oldest possible date
        // satisfying the date set (e.g. first year of decade, first month of the year, etc.)
        toJSDate: function() {
            if (!this.isComplete()) return null;
            var year  = this.getYear(true);       // true: failsafe, get first year of decade if only decade is set
            var month = this.getMonth(true) - 1;  // "-1": js Date's months are 0 to 11, this.month is 1 to 12
            var day   = this.getDay(true);
            var jsDate = new Date(year, month, day);
            return jsDate;
        },

        // Returns either a decade or the year (as string, which may include non-numeric characters, e.g. "1920s")
        getBestPrecisionStringYear: function() {
            if (!this.isComplete()) return "";
            if (this.range.years > 1) return (this.year.toString() + "s");
            return this.year.toString();
        },

        // If year is given returns the year; for decades returns the first year of the decade
        // (as string representation of an integer)
        getMostConservativeYearEstimate: function() {
            if (!this.isComplete()) return "";
            return this.getYear(true, false).toString();
        },

        // If year is given returns the year; for decades returns the middle of the decade
        // (as string representation of an integer)
        getAverageYearEstimate: function() {
            if (!this.isComplete()) return "";
            return this.getYear(true, true).toString();
        },

        getBestPrecisionStringDDMMYYY: function(dateFormat) {
            if (!dateFormat) {
                dateFormat = "DMY";
            }
            if (this.month == null) {
                return this.getBestPrecisionStringYear();
            }
            var dateStr = this.getYear().toString();
            if (this.getMonth() != null && dateFormat != "Y") {
                dateStr = ("0" + this.getMonth()).slice(-2) + "-" + dateStr;
                if (this.getDay() != null && dateFormat == "DMY") {
                    dateStr = ("0" + this.getDay()).slice(-2) + "-" + dateStr;
                }
            }
            return dateStr;
        },

        // Returns the number of milliseconds since 1 January 1970 (same as Date.getTime())
        getTime: function() {
            return this.toJSDate().getTime();
        },

        // Returns an integer or null.
        // Iff "failsafe" returns a value even if only a range of years is known:
        //  - iff "average" the middle year of the range is returned, otherwise the first year of the range.
        getYear: function(failsafe, average) {
            if (this.isComplete()) {
                if (average && this.range.years > 1) {
                    return this.year + Math.floor(this.range.years/2);
                }
                if (this.range.years == 1 || failsafe) {
                    return this.year;
                }
            }
            return null; // exact year is unknown
        },

        // Returns an integer or null
        // Iff "failsafe" returns 1 if month is not set but at least some date (with any precision) is
        getMonth: function(failsafe) {
            if (this.month == null && failsafe) {
                return 1;
            }
            return this.month;
        },

        // Returns an integer or null
        // Iff "failsafe" returns 1 if day is not set but at least some date (with any precision) is
        getDay: function(failsafe) {
            if (this.day == null && failsafe) {
                return 1;
            }
            return this.day;
        },

        canBeAfterDate: function(otherPedigreeDate) {
            if (!this.isComplete()) {
                return true;
            }
            if (!otherPedigreeDate.isComplete()) {
                return true;
            }
            if (this.getTime() > otherPedigreeDate.getTime()) {
                return true;
            }
            var leastOtherYear  = otherPedigreeDate.getYear(true);
            var leastOtherMonth = otherPedigreeDate.getMonth(true);
            var leastOtherDay   = otherPedigreeDate.getDay(true);

            var maxThisYear  = this.year + this.range.years - 1;
            var maxThisMonth = this.month ? this.month : 12;
            var maxThisDay   = this.day   ? this.day   : 31;

            if (maxThisYear > leastOtherYear) {
                return true;
            }
            if (maxThisYear < leastOtherYear) {
                return false;
            }
            if (maxThisMonth > leastOtherMonth) {
                return true;
            }
            if (maxThisMonth < leastOtherMonth) {
                return false;
            }
            return (maxThisDay >= leastOtherDay);
        }
    });
    return PedigreeDate;
});