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
package org.phenotips.data.internal;

import org.phenotips.Constants;
import org.phenotips.data.Gene;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.EventListener;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import net.jcip.annotations.NotThreadSafe;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NotThreadSafe
public class EmptyGenotypeObjectsRemoverTest
{
    private static final EntityReference VARIANT_CLASS_REFERENCE = new EntityReference("GeneVariantClass",
        EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String GENE_KEY = "gene";

    private static final String VARIANT_KEY = "cdna";

    @Rule
    public MockitoComponentMockingRule<EventListener> mocker =
        new MockitoComponentMockingRule<>(EmptyGenotypeObjectsRemover.class);

    @Mock
    private XWikiContext context;

    private EmptyGenotypeObjectsRemover patientEmptyObjectsRemover;

    @Mock
    private BaseObject gene1;

    @Mock
    private BaseObject gene2;

    @Mock
    private BaseObject variant1;

    @Mock
    private BaseObject variant2;

    @Mock
    private XWikiDocument xWikiDocument;

    @Before
    public void setUp() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        this.patientEmptyObjectsRemover = (EmptyGenotypeObjectsRemover) this.mocker.getComponentUnderTest();
    }

    @Test
    public void emptyGenesRemovedTest() throws XWikiException
    {
        when(this.xWikiDocument.getXObjects(Gene.GENE_CLASS)).thenReturn(Arrays.asList(this.gene1, this.gene2));
        when(this.gene1.getStringValue(GENE_KEY)).thenReturn("");
        when(this.gene2.getStringValue(GENE_KEY)).thenReturn("BRCA1");

        this.patientEmptyObjectsRemover.onEvent(null, this.xWikiDocument, null);

        verify(this.xWikiDocument).removeXObject(this.gene1);
        verify(this.xWikiDocument, Mockito.never()).removeXObject(this.gene2);
    }

    @Test
    public void nullGeneObjectsAreIgnored() throws XWikiException
    {
        when(this.xWikiDocument.getXObjects(Gene.GENE_CLASS)).thenReturn(Arrays.asList(this.gene1, null, this.gene2));
        when(this.xWikiDocument.getXObjects(VARIANT_CLASS_REFERENCE)).thenReturn(Collections.emptyList());
        when(this.gene1.getStringValue(GENE_KEY)).thenReturn("BRCA1");
        when(this.gene2.getStringValue(GENE_KEY)).thenReturn("");

        this.patientEmptyObjectsRemover.onEvent(null, this.xWikiDocument, null);

        verify(this.xWikiDocument, never()).removeXObject(this.gene1);
        verify(this.xWikiDocument).removeXObject(this.gene2);
    }

    @Test
    public void ignoresEmptyDocuments() throws XWikiException
    {
        when(this.xWikiDocument.getXObjects(Gene.GENE_CLASS)).thenReturn(Collections.emptyList());
        when(this.xWikiDocument.getXObjects(VARIANT_CLASS_REFERENCE)).thenReturn(Collections.emptyList());

        this.patientEmptyObjectsRemover.onEvent(null, this.xWikiDocument, null);

        verify(this.xWikiDocument, never()).removeXObject((BaseObject) anyObject());
    }

    @Test
    public void noGenesDeletesAllVariants()
    {
        when(this.xWikiDocument.getXObjects(Gene.GENE_CLASS)).thenReturn(null);
        when(this.xWikiDocument.getXObjects(VARIANT_CLASS_REFERENCE))
            .thenReturn(Arrays.asList(this.variant1, this.variant2));

        this.patientEmptyObjectsRemover.onEvent(null, this.xWikiDocument, null);

        verify(this.xWikiDocument).removeXObjects(VARIANT_CLASS_REFERENCE);
    }

    @Test
    public void emptyGenesDeletesAllVariants()
    {
        when(this.xWikiDocument.getXObjects(Gene.GENE_CLASS)).thenReturn(Collections.emptyList());
        when(this.xWikiDocument.getXObjects(VARIANT_CLASS_REFERENCE))
            .thenReturn(Arrays.asList(this.variant1, this.variant2));

        this.patientEmptyObjectsRemover.onEvent(null, this.xWikiDocument, null);

        verify(this.xWikiDocument).removeXObjects(VARIANT_CLASS_REFERENCE);
    }

    @Test
    public void emptyVariantsAreRemoved()
    {
        when(this.xWikiDocument.getXObjects(Gene.GENE_CLASS)).thenReturn(Arrays.asList(this.gene1));
        when(this.xWikiDocument.getXObjects(VARIANT_CLASS_REFERENCE))
            .thenReturn(Arrays.asList(this.variant1, this.variant2));

        when(this.variant1.getStringValue(VARIANT_KEY)).thenReturn("cdna");
        when(this.variant2.getStringValue(VARIANT_KEY)).thenReturn("");

        this.patientEmptyObjectsRemover.onEvent(null, this.xWikiDocument, null);

        verify(this.xWikiDocument, never()).removeXObject(this.variant1);
        verify(this.xWikiDocument).removeXObject(this.variant2);
    }

    @Test
    public void variantsForUnsetGenesAreRemoved()
    {
        when(this.xWikiDocument.getXObjects(Gene.GENE_CLASS)).thenReturn(Arrays.asList(this.gene1));
        when(this.xWikiDocument.getXObjects(VARIANT_CLASS_REFERENCE))
            .thenReturn(Arrays.asList(this.variant1, this.variant2));

        when(this.gene1.getStringValue(GENE_KEY)).thenReturn("BRCA1");
        when(this.variant1.getStringValue(GENE_KEY)).thenReturn("BRCA1");
        when(this.variant1.getStringValue(VARIANT_KEY)).thenReturn("cdna");
        when(this.variant2.getStringValue(GENE_KEY)).thenReturn("BRCA3");
        when(this.variant2.getStringValue(VARIANT_KEY)).thenReturn("cdna");

        this.patientEmptyObjectsRemover.onEvent(null, this.xWikiDocument, null);

        verify(this.xWikiDocument, never()).removeXObject(this.variant1);
        verify(this.xWikiDocument).removeXObject(this.variant2);
    }

    @Test
    public void nullVariantObjectsAreIgnored()
    {
        when(this.xWikiDocument.getXObjects(Gene.GENE_CLASS)).thenReturn(Arrays.asList(this.gene1));
        when(this.xWikiDocument.getXObjects(VARIANT_CLASS_REFERENCE))
            .thenReturn(Arrays.asList(this.variant1, null, this.variant2));

        when(this.variant1.getStringValue(VARIANT_KEY)).thenReturn("cdna");
        when(this.variant2.getStringValue(VARIANT_KEY)).thenReturn("");

        this.patientEmptyObjectsRemover.onEvent(null, this.xWikiDocument, null);

        verify(this.xWikiDocument, never()).removeXObject(this.variant1);
        verify(this.xWikiDocument).removeXObject(this.variant2);
    }
}
