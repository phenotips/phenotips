define([],function(){

    /**
     * Returns either death date or current time
     * @param {Date} deathDate
     * @return JS Date
     */
    function getLastDateAlive(deathDate)
    {
        return (deathDate == null || !deathDate.isComplete()) ? new Date() : deathDate.toJSDate();
    }

    /**
     * Returns the integer age in full years (as of today or as of death date, if not null.
     * @param {Date} birthDate
     */
    function getAgeInFullYears(birthDate, deathDate)
    {
        if (birthDate.onlyDecadeAvailable() || (deathDate && deathDate.onlyDecadeAvailable())) {
            var lastYearAlive = new Date().getFullYear();
            if (deathDate != null) {
                lastYearAlive = deathDate.getYear(true);
                if (deathDate.onlyDecadeAvailable()) {
                    lastYearAlive += 10;
                }
            }
            var oldestYear = birthDate.onlyDecadeAvailable() ? parseInt(birthDate.getDecade()) : birthDate.getYear();
            var age = Math.floor(lastYearAlive - oldestYear);
            return age;
        }

        var lastDateAliveJS = AgeCalc.getLastDateAlive(deathDate);
        var birthDateJS = birthDate.toJSDate();

        var years = Math.floor(lastDateAliveJS.getFullYear() - birthDateJS.getFullYear()
                               - (lastDateAliveJS.getDayOfYear() < birthDateJS.getDayOfYear() ? 1 : 0));

        return years;
    }

    /**
     * Returns age as used in date range dropdowns
     */
    function getAgeForCancersDropdown(birthDate, deathDate)
    {
        if (!birthDate.isComplete()) {
            return 0;
        }
        if (birthDate.onlyDecadeAvailable() || (deathDate && deathDate.onlyDecadeAvailable())) {
            var age = AgeCalc.getAgeInFullYears(birthDate, deathDate);
            var decadeOld = Math.ceil(age/10)*10;
            return "before_" + decadeOld;
        }
        var years = AgeCalc.getAgeInFullYears(birthDate, deathDate);
        if (years < 1) {
            return "before_1";
        }
        return years;
    }

    /**
     * Returns the text string representing the age of a person with the given birth and death dates (death date may be null)
     * @param {Date} birthDate
     * @param {Date} deathDate
     * @param {String} dateDisplayFormat, {"MDY"|"DMY"|"MY"|"Y"|"MMY"}
     * @return {String} Age formatted with years, months, days
     */
    function getAgeString(birthDate, deathDate, dateDisplayFormat)
    {
        if (!birthDate || !birthDate.isComplete() || birthDate.onlyDecadeAvailable() ||
            (deathDate && (!deathDate.isComplete() || deathDate.onlyDecadeAvailable()))) {
            return "";
        }

        var years = AgeCalc.getAgeInFullYears(birthDate, deathDate);
        if (dateDisplayFormat == "Y") {
            if (years > 0) {
                return years + " y";
            } else {
                return "";
            }
        }

        var lastDateAliveJS = AgeCalc.getLastDateAlive(deathDate);
        var birthDateJS = birthDate.toJSDate();

        // age in milliseconds
        var age = lastDateAliveJS.getTime() - birthDateJS.getTime();

        if (age == 0) {
            return "";
        }
        if (age < 0) {
            if (deathDate == null) {
                return "not born yet"
            } else {
                return "";
            }
        }

        var agestr = "";

        // TODO: can do a bit better with up-to-a-day precision
        //       (e.g. born Apr 10, now May 9 => 0 month, May 10 => 1 month) - but don't need it here
        var aSecond = 1000;
        var aMinute = aSecond * 60;
        var aHour = aMinute * 60;
        var aDay = aHour * 24;
        var aWeek = aDay * 7;
        var aMonth = aDay * 30;

        // round up to whole 1 or 2 month if age is just a days short, which may
        // also happen due to timezone differences; also February is shorter.
        var months = Math.floor((age + 2*aDay)/ aMonth);

        if (years == 0 && months < 12) {
            if (dateDisplayFormat == "MMY" || dateDisplayFormat == "MY") {
                if (months > 0) {
                    return months + " mo";
                } else {
                    return "";
                }
            }

            var days = Math.floor(age / aDay);

            if (days <21)
            {
                if (days == 1) {
                    agestr = days + ' day';
                }
                else {
                    agestr = days + ' days';
                }
            }
            else if (months < 2 && days < 60) {
                var weeks = Math.floor(age / aWeek);
                agestr = weeks + " wk";
            } else
            {
                agestr = months + ' mo';
            }
        } else {
            agestr = years + " y";
        }
        return agestr;
    }

    var AgeCalc = {};
    AgeCalc.getAgeString = getAgeString;
    AgeCalc.getLastDateAlive = getLastDateAlive;
    AgeCalc.getAgeForCancersDropdown = getAgeForCancersDropdown;
    AgeCalc.getAgeInFullYears = getAgeInFullYears;
    return AgeCalc;
});