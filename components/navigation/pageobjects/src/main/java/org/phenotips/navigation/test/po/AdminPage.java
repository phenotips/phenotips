package org.phenotips.navigation.test.po;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.phenotips.data.test.po.PatientRecordEditPage;
import org.xwiki.test.ui.po.ViewPage;

public class AdminPage extends ViewPage{

    @FindBy(xpath = "//*[@id=\"vertical-menu-FormConfig\"]/a")
    WebElement patientFormStructure;

    /* Patient form configuration elements  for admin preferences page */

    @FindBy(xpath = "//*[@id=\"org.phenotips.patientSheet.obstetricHistory\"]/div[1]/p/input[1]")
    WebElement obstetricHistoryForm;


    public static AdminPage gotoAdminPage(){
        getUtil().gotoPage("Xwiki", "XwikiPreferences", "view");
        return new AdminPage();
    }

    public AdminPage clickPatientFormStructure(){
        this.patientFormStructure.click();
        return new AdminPage();
    }

    public void enableObstetricHistoryForm(){
        this.obstetricHistoryForm.click();
    }


}
