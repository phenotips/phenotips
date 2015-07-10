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

import org.phenotips.data.test.po.PatientRecordEditPage;

import org.xwiki.test.ui.po.ViewPage;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Represents the actions possible on the PhenoTips homepage.
 *
 * @version $Id$
 * @since 1.0RC1
 */
public class HomePage extends ViewPage
{
    @FindBy(css = ".gadget-title label[for='create-patient-record']")
    WebElement newPatientRecord;

    @FindBy(xpath = "//*[contains(@class, 'gadget-title')]//a[text() = 'Browse all data »']")
    WebElement allData;

    @FindBy(xpath = "//div[@class='gadget'][//*[contains(text(), 'About')]]//div[@class='gadget-content']")
    WebElement aboutGadget;

    @FindBy(xpath = "//div[@class='gadget'][//*[contains(text(), 'Active groups')]]//div[@class='gadget-content']")
    WebElement activeGroupsGadget;

    @FindBy(xpath = "//div[@class='gadget'][//*[contains(text(), 'My Data')]]//div[@class='gadget-content']")
    WebElement myRecordsGadget;

    @FindBy(xpath = "//div[@class='gadget'][//*[contains(text(), 'Data shared with me')]]//div[@class='gadget-content']")
    WebElement sharedRecordsGadget;

    public static HomePage gotoPage()
    {
        getUtil().gotoPage("data", "WebHome", "view");
        return new HomePage();
    }

    public PatientRecordEditPage clickNewPatientRecord()
    {
        this.newPatientRecord.click();
        return new PatientRecordEditPage();
    }

    public AllRecordsPage clickBrowseAllRecords()
    {
        this.allData.click();
        return new AllRecordsPage();
    }

    public boolean hasActiveGroups()
    {
        return getUtil().hasElement(this.activeGroupsGadget, By.cssSelector("tr"));
    }

    public boolean hasGroupManagement()
    {
        return getUtil().hasElement(this.activeGroupsGadget, By.xpath(".//a[contains(text(), 'Manage groups')]"));
    }

    @Override
    public void waitUntilPageJSIsLoaded()
    {
        getDriver().waitUntilJavascriptCondition("return window.Prototype != null && window.Prototype.Version != null");
    }
}
