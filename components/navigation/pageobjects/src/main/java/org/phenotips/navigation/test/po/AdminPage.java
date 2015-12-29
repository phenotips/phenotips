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
