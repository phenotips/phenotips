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
package org.phenotips.panels.rest.internal;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link PanelData}.
 */
public class PanelDataTest
{
    private static final String P_TERM_1 = "HP:001";

    private static final String P_TERM_2 = "HP:002";

    private static final String P_TERM_3 = "HP:003";

    private static final String P_TERM_4 = "HP:004";

    private static final String A_TERM_5 = "HP:005";

    private static final String A_TERM_6 = "HP:006";

    private static final String G_TERM_1 = "AAA";

    private static final String G_TERM_2 = "BBB";

    private static final String G_TERM_3 = "CCC";

    private PanelData panelData;

    private Set<String> presentTerms;

    private Set<String> absentTerms;

    private Set<String> rejectedGenes;

    @Before
    public void setUp()
    {
        this.presentTerms = new HashSet<>();
        this.presentTerms.add(P_TERM_1);
        this.presentTerms.add(P_TERM_2);
        this.presentTerms.add(P_TERM_3);
        this.presentTerms.add(P_TERM_4);

        this.absentTerms = new HashSet<>();
        this.absentTerms.add(A_TERM_5);
        this.absentTerms.add(A_TERM_6);

        this.rejectedGenes = new HashSet<>();
        this.rejectedGenes.add(G_TERM_1);
        this.rejectedGenes.add(G_TERM_2);
        this.rejectedGenes.add(G_TERM_3);

        this.panelData = new PanelData(this.presentTerms, this.absentTerms, this.rejectedGenes);
    }

    @Test
    public void getPresentTermsReturnsAllEnteredPresentTerms()
    {
        final Set<String> presentTerms = new HashSet<>();
        presentTerms.add(P_TERM_1);
        presentTerms.add(P_TERM_2);
        presentTerms.add(P_TERM_3);
        presentTerms.add(P_TERM_4);

        Assert.assertEquals(presentTerms, this.panelData.getPresentTerms());
    }

    @Test
    public void getAbsentTermsReturnsAllEnteredAbsentTerms()
    {
        final Set<String> absentTerms = new HashSet<>();
        absentTerms.add(A_TERM_5);
        absentTerms.add(A_TERM_6);

        Assert.assertEquals(absentTerms, this.panelData.getAbsentTerms());
    }

    @Test
    public void getRejectedGenesReturnsAllEnteredRejectedGenes()
    {
        final Set<String> rejectedGenes = new HashSet<>();
        rejectedGenes.add(G_TERM_1);
        rejectedGenes.add(G_TERM_2);
        rejectedGenes.add(G_TERM_3);

        Assert.assertEquals(rejectedGenes, this.panelData.getRejectedGenes());
    }

    @Test
    public void equalsTwoOfTheSameObjectReturnsTrue()
    {
        Assert.assertTrue(this.panelData.equals(this.panelData));
    }

    @Test
    public void equalsNullReturnsFalse()
    {
        Assert.assertFalse(this.panelData.equals(null));
    }

    @Test
    public void equalsDifferentClassReturnsFalse()
    {
        Assert.assertFalse(this.panelData.equals("string"));
    }

    @Test
    public void equalsPresentTermsDifferentReturnsFalse()
    {
        final Set<String> presentTerms2 = new HashSet<>();
        presentTerms2.add(P_TERM_1);
        presentTerms2.add(P_TERM_3);
        presentTerms2.add(P_TERM_4);

        final PanelData panelData2 = new PanelData(presentTerms2, this.absentTerms, this.rejectedGenes);
        Assert.assertFalse(this.panelData.equals(panelData2));
    }

    @Test
    public void equalsAbsentTermsDifferentReturnsFalse()
    {
        final Set<String> absentTerms2 = new HashSet<>();
        absentTerms2.add(A_TERM_5);

        final PanelData panelData2 = new PanelData(this.presentTerms, absentTerms2, this.rejectedGenes);
        Assert.assertFalse(this.panelData.equals(panelData2));
    }

    @Test
    public void equalsRejectedGenesDifferentReturnsFalse()
    {
        final Set<String> rejectedGenes2 = new HashSet<>();
        rejectedGenes2.add(G_TERM_1);
        rejectedGenes2.add(G_TERM_2);

        final PanelData panelData2 = new PanelData(this.presentTerms, this.absentTerms, rejectedGenes2);
        Assert.assertFalse(this.panelData.equals(panelData2));
    }

    @Test
    public void equalsDifferentObjectsSameDataReturnsTrue()
    {
        final Set<String> presentTerms2 = new HashSet<>();
        presentTerms2.add(P_TERM_1);
        presentTerms2.add(P_TERM_2);
        presentTerms2.add(P_TERM_3);
        presentTerms2.add(P_TERM_4);

        final Set<String> absentTerms2 = new HashSet<>();
        absentTerms2.add(A_TERM_5);
        absentTerms2.add(A_TERM_6);

        final Set<String> rejectedGenes2 = new HashSet<>();
        rejectedGenes2.add(G_TERM_1);
        rejectedGenes2.add(G_TERM_2);
        rejectedGenes2.add(G_TERM_3);

        final PanelData panelData2 = new PanelData(presentTerms2, absentTerms2, rejectedGenes2);
        Assert.assertTrue(this.panelData.equals(panelData2));
    }

    @Test
    public void hashCodeSameForSameObject()
    {
        Assert.assertEquals(this.panelData.hashCode(), this.panelData.hashCode());
    }

    @Test
    public void hashCodeSameForDifferentObjectWithSameData()
    {
        final Set<String> presentTerms2 = new HashSet<>();
        presentTerms2.add(P_TERM_1);
        presentTerms2.add(P_TERM_2);
        presentTerms2.add(P_TERM_3);
        presentTerms2.add(P_TERM_4);

        final Set<String> absentTerms2 = new HashSet<>();
        absentTerms2.add(A_TERM_5);
        absentTerms2.add(A_TERM_6);

        final Set<String> rejectedGenes2 = new HashSet<>();
        rejectedGenes2.add(G_TERM_1);
        rejectedGenes2.add(G_TERM_2);
        rejectedGenes2.add(G_TERM_3);

        final PanelData panelData2 = new PanelData(presentTerms2, absentTerms2, rejectedGenes2);
        Assert.assertEquals(this.panelData.hashCode(), panelData2.hashCode());
    }
}
