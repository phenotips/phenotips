/**
 * Returns the age of a person with the given birth and death dates
 * @param {Date} birthDate
 * @param {Date} [deathDate]
 * @return {String} Age formatted with years, months, days
 */
function getAge(birthDate, deathDate)
{
    if (birthDate.onlyDecadeAvailable()) {
        return "";
    }

    var now;
    if (deathDate == null){
        now = new Date();
    }
    else {
        if (deathDate.onlyDecadeAvailable()) {
            return "";
        }
        now = deathDate.toJSDate();
    }

    birthDate = birthDate.toJSDate();

    var aSecond = 1000;
    var aMinute = aSecond * 60;
    var aHour = aMinute * 60;
    var aDay = aHour * 24;
    var aWeek = aDay * 7;
    var aMonth = aDay * 30.5;

    var age = now.getTime() - birthDate.getTime();

    if (age <= 0) {
        if (deathDate == null) {
            if (age < 0) {
                return "not born yet"
            }
        } else {
            return "";
        }
    }

    var years = now.getFullYear() - birthDate.getFullYear() - (now.getDayOfYear() < birthDate.getDayOfYear() ? 1 : 0);

    var agestr = "";

    // TODO: can do a bit better with up-to-a-day precision
    //       (e.g. born Apr 10, now May 9 => 0 month, May 10 => 1 month) - but don't need it here
    var months = Math.floor(age / aMonth);

    if (months < 12) {
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
        else if (days < 60) {
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