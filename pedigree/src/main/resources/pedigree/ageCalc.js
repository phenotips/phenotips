function getAge(birth, death)
{
    var now;
    if (death == null){
    now = new Date();
    }
    else {
    now = death;
    }


    var aSecond = 1000;
    var aMinute = aSecond * 60;
    var aHour = aMinute * 60;
    var aDay = aHour * 24;
    var aWeek = aDay * 7;
    var aMonth = aDay * 30;


    var age = now.getTime() - birth.getTime();

    if (age < 0) {
        return "not born yet"
    }


    var years = (new Date(now.getTime() - aMonth* (birth.getMonth()) )).getFullYear()
        - (new Date(birth.getTime() - aMonth* (birth.getMonth()) )).getFullYear();


    offsetNow = (new Date(now.getTime() - aDay* (birth.getDate() -1) ));
    offsetBirth = (new Date(birth.getTime() - aDay* (birth.getDate() -1) ));
    if(years > 1){
        months = years*12 + ( offsetNow.getMonth() - offsetBirth.getMonth()) ;
    }else{
        months = (now.getFullYear() - birth.getFullYear())*12 + ( offsetNow.getMonth() - offsetBirth.getMonth()) ;
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