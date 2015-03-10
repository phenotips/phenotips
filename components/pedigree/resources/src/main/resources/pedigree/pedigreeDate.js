/**
 * Class for storing either exact or fuzzy dates.
 * 
 * Assert:
 *   if day is set   => all of {decade, year, month, day} are set
 *   if month is set => all of {decade, year, month} are set
 *   if year is set  => decade is also set
 *
 * Note:
 *   month is from 1 to 12
 *   day starts at 1
 */
var PedigreeDate = Class.create({

    /**
     * Accepts either a string representation of date or a javascript Date object or an
     * object with {year[,month][,day]} string or integer fields
     */
    initialize: function(date) {
        this.decade = null;
        this.year   = null;
        this.month  = null;
        this.day    = null;
        if (date == null || !date) return;

        var jsDate = null;
        if (typeof date === 'string' || date instanceof String) {
            // check if string matches the "decade only" format
            if (date.match(/^\d\d\d\ds$/)) {
                this.decade = date;
            }
            else if (!isNaN(Date.parse(date))) {  // empty string also parses to NaN
                // deal with timezone differences: treat all dates as being in the same timezone.
                // for that need to parse input string, if posible, and extract day/month/year
                // "as is", regardless of the timezone attached
                // As of now, expected/supported format is "Tue Dec 09 00:00:00 UTC 2014"
                var parsed = date.match(/\w\w\w (\w\w\w) (\d\d) \d\d:\d\d:\d\d \w\w\w (\d\d\d\d)/);
                if (parsed !== null) {
                    // use Date("Dec 09, 2014") constructor
                    var timezonelessDate = parsed[1] + " " + parsed[2] + ", " + parsed[3];
                    jsDate = new Date(timezonelessDate);
                } else {
                    // Also suport the PhenoTips patient JSON format "yyyy-mm-dd"
                    var parsed = date.match(/(\d\d\d\d)-(\d\d)-(\d\d)/);
                    if (parsed !== null) {
                        // use Date("Dec 09, 2014") constructor
                        var month0based = parseInt(parsed[2]) - 1;
                        var timezonelessDate = this._getMonthName("en",month0based) + " " + parsed[3] + ", " + parsed[1];
                        jsDate = new Date(timezonelessDate);
                    } else {
                        // parse any other format
                        jsDate = new Date(date);
                    }
                }
            }
        } else if (Object.prototype.toString.call(date) === '[object Date]') {
            jsDate = date;
        }

        if (jsDate !== null) {
            this.year   = jsDate.getFullYear();
            this.month  = jsDate.getMonth() + 1;   // js Date's months are 0 to 11, this.month is 1 to 12
            this.day    = jsDate.getDate();
        }
        else if (typeof date === 'object') {
            date.hasOwnProperty("decade") && ( this.decade = date.decade );
            // keep null-s, convert strings to integers
            date.hasOwnProperty("year")   && ( this.year   = this.parseIntOrNull(date.year) );
            date.hasOwnProperty("month")  && ( this.month  = this.parseIntOrNull(date.month) );
            date.hasOwnProperty("day")    && ( this.day    = this.parseIntOrNull(date.day) );
        }

        if (this.year !== null && this.decade === null) {
            this.decade = this.year.toString().slice(0,-1) + '0s';
        }
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
        // note: Date.toDateString() is used for date-to-string conversions instead of
        //       Date.toUTCString because output of toDateString is the only one which seems
        //       to be uniformly supported by all browsers, new and old.

        if (this.year === null && this.decade !== null) {
            return this.decade;
        }
        if (this.year !== null && this.month == null) {
            return this.year.toString();
        }
        if (this.year !== null && this.month !== null && this.day === null) {
            return this.getMonthName() + " " + this.year;
        }
        if (this.year !== null && this.month !== null && this.day !== null) {
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
        if (this.year === null && this.decade !== null) { 
            // getYear(true) returns first year of decade as integer
            return "ABT " + this.getYear(true).toString();
        }
        return this.toString();
    },

    isSet: function() {
        return (this.decade !== null || this.year !== null || this.month !== null || this.day !== null);
    },

    isComplete: function() {
        return (this.decade !== null);
    },

    onlyDecadeAvailable: function() {
        return (this.decade !== null && this.year == null);
    },

    // Returns a string or null
    getDecade: function() {
        return this.decade;
    },

    // Returns simple object with only the fields (decade, year, month, day) set
    getSimpleObject: function() {
        var date = {};
        if (this.decade !== null) date["decade"] = this.decade;
        if (this.year   !== null) date["year"]   = this.year;
        if (this.month  !== null) date["month"]  = this.month;
        if (this.day    !== null) date["day"]    = this.day;
        return date;
    },

    // Returns best possible estimation of this date as a javascript Date object.
    //
    // Aproximate dates (e.g. decades, or dates without a day or month) are set as 
    // oldest possible date satisfying the date set (e.g. first year of decade, first month of the year, etc.)
    toJSDate: function() {
        var year  = this.getYear(true);       // true: failsafe, get first year of decade if only decade is set
        var month = this.getMonth(true) - 1;  // "-1": js Date's months are 0 to 11, this.month is 1 to 12
        var day   = this.getDay(true);
        var jsDate = new Date(year, month, day);
        return jsDate;
    },

    // Returns either a decade or the year (both as string)
    getBestPrecisionStringYear: function() {
        if (!this.isComplete()) return "";
        if (this.year == null) return this.decade;
        return this.year.toString();
    },

    getMostConservativeYearEstimate: function() {
        // for any year returns the year;l for decades returns the first year of the decade
        return this.getBestPrecisionStringYear().replace(/s^/,"");
    },

    getBestPrecisionStringDDMMYYY: function() {
        if (!this.isComplete()) return "";
        if (this.year == null) return this.decade;
        var dateStr = this.getYear().toString();
        if (this.getMonth() != null) {
            dateStr = ("0" + this.getMonth()).slice(-2) + "-" + dateStr;
            if (this.getDay() != null) {
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
    // Iff "failsafe" returns first year of the decade if year is not set and decade is
    getYear: function(failsafe) {
        if (this.isComplete() && this.year == null && failsafe) {
            // remove trailing "s" from the decade && convert to integer
            var year = parseInt( this.decade.slice(0,-1) );
            return year;
        }
        return this.year;
    },

    // Returns an integer or null
    // Iff "failsafe" returns 1 if month is not set but at least some date (with any precision) is
    getMonth: function(failsafe) {
        if (this.isComplete() && this.month == null && failsafe) {
            return 1;
        }
        return this.month;
    },

    // Returns an integer or null
    // Iff "failsafe" returns 1 if day is not set but at least some date (with any precision) is
    getDay: function(failsafe) {
        if (this.isComplete() && this.day == null && failsafe) {
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

        var maxThisYear  = this.year  ? this.year  : this.getYear(true) + 9;
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