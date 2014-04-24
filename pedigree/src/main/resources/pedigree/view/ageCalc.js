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


    offsetNow = (new Date(now.getTime() - aDay* (birthDate.getDate() -1) ));
    offsetBirth = (new Date(birthDate.getTime() - aDay* (birthDate.getDate() -1) ));
    if(years > 1){
        months = years*12 + ( offsetNow.getMonth() - offsetBirth.getMonth()) ;
    }else{
        months = (now.getFullYear() - birthDate.getFullYear())*12 + ( offsetNow.getMonth() - offsetBirth.getMonth()) ;
    }

    agestr="";

    if (months < 12){
        days = Math.floor(age / aDay);

        if(days <30)
        {
            agestr = days + 'd';
        }
        else
        {
            agestr = months + 'mo';
        }
    }else{
        agestr = agestr + years;
        agestr = agestr + "y";
    }
    return agestr;
}