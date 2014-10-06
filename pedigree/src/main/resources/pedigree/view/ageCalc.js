/**
 * Returns the age of a person with the given birth and death dates
 * @param {Date} birthDate
 * @param {Date} [deathDate]
 * @return {String} Age formatted with years, months, days
 */
function getAge(birthDate, deathDate)
{
    var now;
    if (deathDate == null){
        now = new Date();
    }
    else {
        now = deathDate;
    }

    var aSecond = 1000;
    var aMinute = aSecond * 60;
    var aHour = aMinute * 60;
    var aDay = aHour * 24;
    var aWeek = aDay * 7;
    var aMonth = aDay * 30;

    var age = now.getTime() - birthDate.getTime();

    if (age < 0) {
        return "not born yet"
    }

    var years = (new Date(now.getTime() - aMonth* (birthDate.getMonth()) )).getFullYear()
                - (new Date(birthDate.getTime() - aMonth* (birthDate.getMonth()) )).getFullYear();

    var offsetNow = (new Date(now.getTime() - aDay* (birthDate.getDate() -1) ));
    var offsetBirth = (new Date(birthDate.getTime() - aDay* (birthDate.getDate() -1) ));
    if (years > 1){
        var months = years*12 + ( offsetNow.getMonth() - offsetBirth.getMonth()) ;
    }else{
        var months = (now.getFullYear() - birthDate.getFullYear())*12 + ( offsetNow.getMonth() - offsetBirth.getMonth()) ;
    }

    var agestr = "";

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