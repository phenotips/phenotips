/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.navigation.test.ui;

import org.phenotips.data.test.po.PatientRecordEditPage;
import org.phenotips.navigation.test.po.AllRecordsPage;
import org.phenotips.navigation.test.po.HomePage;

import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.AuthenticationRule;
import org.xwiki.test.ui.PAdamsAuthenticationRule;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * Verify the correct functionality of the PhenoTips homepage.
 *
 * @version $Id$
 * @since 1.0RC1
 */
public class HomePageTest extends AbstractTest
{
    @Rule
    public AuthenticationRule authenticationRule = new PAdamsAuthenticationRule(getUtil(), getDriver(), true);

    @Test
    public void verifyClickingOnNewPatientRecord()
    {
        HomePage page = HomePage.gotoPage();
        PatientRecordEditPage newPatient = page.clickNewPatientRecord();
        Assert.assertTrue(StringUtils.isNotBlank(newPatient.getPatientRecordId()));
    }

    @Test
    public void verifyBrowseAllRecords()
    {
        HomePage page = HomePage.gotoPage();
        AllRecordsPage allRecords = page.clickBrowseAllRecords();
        Assert.assertEquals(0, allRecords.getTable().getColumnIndex("Report name"));
    }

    @Test
    public void verifyGroupManagementNotDisplayedForNormalUsers()
    {
        HomePage page = HomePage.gotoPage();
        Assert.assertFalse(page.hasGroupManagement());
    }
}
