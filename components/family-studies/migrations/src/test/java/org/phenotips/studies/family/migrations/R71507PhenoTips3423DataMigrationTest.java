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
package org.phenotips.studies.family.migrations;

import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link R71507henoTips3423DataMigration}.
 */
public class R71507PhenoTips3423DataMigrationTest
{
    private static final String ERROR_READING_DATA = "Error checking pedigree data for document {}: [{}]";

    private static final String ERROR_UPDATING_DATA = "Error updating pedigree data format for document {}: [{}]";

    private static final String WARNING_SIMPLE_JSON =
            "Skipping conversion for family [{}] - pedigree is in SimpleJSON format";

    private static final String SERIALIZED_ENTITY = "serialized_entity";

    private static final String WIKI_ID = "wikiID";

    private static final String PEDIGREECLASS_JSONDATA_KEY = "data";

    private static final String FAMILY_1 = "family1";

    private static final String FAMILY_2 = "family2";

    // simple pedigree with 3 members (mother-father-child), only child linked to a patient, few properties
    private static final String PEDIGREE_1_DATA = ("{`GG`:[{`prop`:{`disorders`:[`220120`],`features`:[],"
        + "`family_history`:{},`carrierStatus`:`affected`,`gender`:`F`,`genes`:[{`gene`:`ACAP1`,`id`:`HGNC:16467`"
        + ",`status`:`candidate`}],`phenotipsId`:`P0000001`,`dob`:{`month`:2,`year`:2016},`nonstandard_features`:[]"
        + ",`aliveandwell`:true},`id`:0},{`chhub`:true,`outedges`:[{`to`:0}],`prop`:{},`id`:1},{`outedges`:"
        + "[{`to`:1}],`hub`:true,`prop`:{},`rel`:true,`id`:2},{`outedges`:[{`to`:2}],`prop`:{`gender`:`F`},`id`:3},"
        + "{`outedges`:[{`to`:2}],`prop`:{`gender`:`M`},`id`:4}],`settings`:{`legendSettings`:{`preferences`:{`style`"
        + ":`multisector`},`abnormalities`:{`disorders`:{`220120`:{`color`:`#D1E9E9`,`name`:`D-GLYCERIC ACIDURIA`,"
        + "`properties`:{`enabled`:true}}},`phenotypes`:{},`causalGenes`:{},`cancers`:{},`candidateGenes`:{"
        + "`HGNC:16467`:{`color`:`#81a270`,`name`:`ACAP1`,`properties`:{`enabled`:true}}},`carrierGenes`:{}}}},"
        + "`ranks`:[3,2,1,1,1],`probandNodeID`:0,`JSON_version`:`1.0`,`positions`:[5,5,5,17,-7],`order`:[[],"
        + "[4,2,3],[1],[0]]}").replace('`', '"');

    // larger pedigree with 5 members, two are linked to patients, one multi-generation edge, many properties
    private static final String PEDIGREE_2_DATA = ("{`GG`:[{`outedges`:[{`to`:9}],`prop`:{`lNameAtB`:`Smith`,`family_h"
        + "istory`:{`consanguinity`:true},`carrierStatus`:`affected`,`comments`:`comment`,`lifeStatus`:`deceased`,`ge"
        + "nder`:`M`,`genes`:[],`phenotipsId`:`P0000002`,`externalID`:``,`features`:[{`id`:`HP:0030045`,`label`:`Serp"
        + "entine fibula`,`type`:`phenotype`,`observed`:`yes`},{`id`:`HP:0100512`,`label`:`Vitamin D deficiency`,`typ"
        + "e`:`phenotype`,`observed`:`yes`}],`disorders`:[`603383`,`220120`],`dob`:{},`dod`:{}},`id`:0},{`chhub`:true"
        + ",`outedges`:[{`to`:0}],`prop`:{},`id`:1},{`outedges`:[{`to`:1}],`hub`:true,`prop`:{`broken`:true,`consangr"
        + "`:`Y`},`rel`:true,`id`:2},{`outedges`:[{`to`:2}],`prop`:{`lName`:`Smith`,`lNameAtB`:`Johnson`,`features`:["
        + "],`lifeStatus`:`deceased`,`gender`:`F`,`genes`:[],`dod`:{`month`:2,`year`:2015,`day`:2},`nonstandard_featu"
        + "res`:[]},`id`:3},{`outedges`:[{`to`:2}],`prop`:{`lName`:`Smith`,`features`:[],`gender`:`M`,`genes`:[],`non"
        + "standard_features`:[]},`id`:4},{`chhub`:true,`outedges`:[{`to`:3}],`prop`:{},`id`:5},{`outedges`:[{`to`:5}"
        + "],`hub`:true,`prop`:{},`rel`:true,`id`:6},{`outedges`:[{`to`:6},{`to`:16}],`prop`:{`family_history`:{},`li"
        + "feStatus`:`alive`,`gender`:`F`,`genes`:[],`phenotipsId`:`P0000003`,`dob`:{},`externalID`:``},`id`:7},{`out"
        + "edges`:[{`to`:6}],`prop`:{`gender`:`M`},`id`:8},{`outedges`:[{`to`:10}],`hub`:true,`prop`:{},`rel`:true,`i"
        + "d`:9},{`chhub`:true,`outedges`:[{`to`:11}],`prop`:{},`id`:10},{`prop`:{`lNameAtB`:`Smith`,`features`:[],`l"
        + "ifeStatus`:`unborn`,`gender`:`U`,`genes`:[],`nonstandard_features`:[]},`id`:11},{`virt`:true,`outedges`:[{"
        + "`to`:9}],`prop`:{},`id`:12},{`virt`:true,`outedges`:[{`to`:12}],`prop`:{},`id`:13},{`virt`:true,`outedges`"
        + ":[{`to`:13}],`prop`:{},`id`:14},{`virt`:true,`outedges`:[{`to`:14}],`prop`:{},`id`:15},{`virt`:true,`outed"
        + "ges`:[{`to`:15}],`prop`:{},`id`:16}],`settings`:{`legendSettings`:{`preferences`:{`style`:`multisector`},`"
        + "abnormalities`:{`disorders`:{`220120`:{`color`:`#92c0db`,`name`:`D-GLYCERIC ACIDURIA`,`properties`:{`enabl"
        + "ed`:true}},`603383`:{`color`:`#D1E9E9`,`name`:`GLAUCOMA 1, OPEN ANGLE, F`,`properties`:{`enabled`:true}}},"
        + "`phenotypes`:{`HP:0100512`:{`color`:`#eedddd`,`name`:`Vitamin D deficiency`,`properties`:{`enabled`:false}"
        + "},`HP:0030045`:{`color`:`#bbaaaa`,`name`:`Serpentine fibula`,`properties`:{`enabled`:false}}},`causalGenes"
        + "`:{},`cancers`:{},`candidateGenes`:{},`carrierGenes`:{}}}},`ranks`:[5,4,3,3,3,2,1,1,1,5,6,7,5,4,3,2,1],`pr"
        + "obandNodeID`:0,`JSON_version`:`1.0`,`positions`:[5,5,5,17,-7,17,17,43,5,20,20,20,31,31,31,31,31],`order`:["
        + "[],[8,6,16,7],[5,15],[4,2,3,14],[1,13],[0,9,12],[10],[11]]}").replace('`', '"');

    private static final String PEDIGREE_SIMPLE_JSON_DATA = ("{`data`:[{`id`:2},{`id`:3},{`mother`:`3`,`phenotipsId`:"
        + "`P0000001`,`sex`:`U`,`father`:`2`,`id`:1,`proband`:true},{`mother`:3,`phenotipsId`:`P0000003`,`father`:2,`"
        + "id`:4},{`id`:5},{`mother`:5,`phenotipsId`:`P0000002`,`father`:1,`id`:6}]}").replace('`', '"');

    private static final String PEDIGREE_1_MIGRATED_DATA = ("{`layout`:{`relationships`:{`2`:{`x`:5,`order`:1}},`membe"
        + "rs`:{`0`:{`generation`:3,`x`:5,`order`:0},`3`:{`generation`:1,`x`:17,`order`:2},`4`:{`generation`:1,`x`:-7"
        + ",`order`:0}},`longedges`:{}},`settings`:{`legendSettings`:{`preferences`:{`style`:`multisector`},`abnormal"
        + "ities`:{`disorders`:{`220120`:{`color`:`#D1E9E9`,`name`:`D-GLYCERIC ACIDURIA`,`properties`:{`enabled`:true"
        + "}}},`phenotypes`:{},`causalGenes`:{},`cancers`:{},`candidateGenes`:{`HGNC:16467`:{`color`:`#81a270`,`name`"
        + ":`ACAP1`,`properties`:{`enabled`:true}}},`carrierGenes`:{}}}},`relationships`:[{`children`:[{`id`:0}],`mem"
        + "bers`:[3,4],`id`:2,`properties`:{}}],`members`:[{`id`:0,`pedigreeProperties`:{`disorders`:[`220120`],`feat"
        + "ures`:[],`family_history`:{},`carrierStatus`:`affected`,`gender`:`F`,`genes`:[{`gene`:`ACAP1`,`id`:`HGNC:1"
        + "6467`,`status`:`candidate`}],`phenotipsId`:`P0000001`,`dob`:{`month`:2,`year`:2016},`nonstandard_features`"
        + ":[],`aliveandwell`:true}},{`id`:3,`pedigreeProperties`:{`gender`:`F`}},{`id`:4,`pedigreeProperties`:{`gend"
        + "er`:`M`}}],`JSON_version`:`1.0`,`proband`:0}").replace('`', '"');

    private static final String PEDIGREE_2_MIGRATED_DATA = ("{`layout`:{`relationships`:{`2`:{`x`:5,`order`:1},`6`:{`x"
        + "`:17,`order`:1},`9`:{`x`:20,`order`:1}},`members`:{`0`:{`generation`:5,`x`:5,`order`:0},`11`:{`generation`"
        + ":7,`x`:20,`order`:0},`3`:{`generation`:3,`x`:17,`order`:2},`4`:{`generation`:3,`x`:-7,`order`:0},`7`:{`gen"
        + "eration`:1,`x`:43,`order`:3},`8`:{`generation`:1,`x`:5,`order`:0}},`longedges`:{`9`:{`path`:[{`x`:31,`orde"
        + "r`:2},{`x`:31,`order`:1},{`x`:31,`order`:3},{`x`:31,`order`:1},{`x`:31,`order`:2}],`member`:7}}},`settings"
        + "`:{`legendSettings`:{`preferences`:{`style`:`multisector`},`abnormalities`:{`disorders`:{`220120`:{`color`"
        + ":`#92c0db`,`name`:`D-GLYCERIC ACIDURIA`,`properties`:{`enabled`:true}},`603383`:{`color`:`#D1E9E9`,`name`:"
        + "`GLAUCOMA 1, OPEN ANGLE, F`,`properties`:{`enabled`:true}}},`phenotypes`:{`HP:0100512`:{`color`:`#eedddd`,"
        + "`name`:`Vitamin D deficiency`,`properties`:{`enabled`:false}},`HP:0030045`:{`color`:`#bbaaaa`,`name`:`Serp"
        + "entine fibula`,`properties`:{`enabled`:false}}},`causalGenes`:{},`cancers`:{},`candidateGenes`:{},`carrier"
        + "Genes`:{}}}},`relationships`:[{`children`:[{`id`:0}],`members`:[3,4],`id`:2,`properties`:{`consanguinity`:"
        + "`yes`,`separated`:true}},{`children`:[{`id`:3}],`members`:[7,8],`id`:6,`properties`:{}},{`children`:[{`id`"
        + ":11}],`members`:[0,7],`id`:9,`properties`:{}}],`members`:[{`id`:0,`pedigreeProperties`:{`lNameAtB`:`Smith`"
        + ",`family_history`:{`consanguinity`:true},`carrierStatus`:`affected`,`comments`:`comment`,`lifeStatus`:`dec"
        + "eased`,`gender`:`M`,`genes`:[],`phenotipsId`:`P0000002`,`externalID`:``,`features`:[{`id`:`HP:0030045`,`la"
        + "bel`:`Serpentine fibula`,`type`:`phenotype`,`observed`:`yes`},{`id`:`HP:0100512`,`label`:`Vitamin D defici"
        + "ency`,`type`:`phenotype`,`observed`:`yes`}],`disorders`:[`603383`,`220120`],`dob`:{},`dod`:{}}},{`id`:3,`p"
        + "edigreeProperties`:{`lName`:`Smith`,`lNameAtB`:`Johnson`,`features`:[],`lifeStatus`:`deceased`,`gender`:`F"
        + "`,`genes`:[],`dod`:{`month`:2,`year`:2015,`day`:2},`nonstandard_features`:[]}},{`id`:4,`pedigreeProperties"
        + "`:{`lName`:`Smith`,`features`:[],`gender`:`M`,`genes`:[],`nonstandard_features`:[]}},{`id`:7,`pedigreeProp"
        + "erties`:{`family_history`:{},`lifeStatus`:`alive`,`gender`:`F`,`genes`:[],`phenotipsId`:`P0000003`,`dob`:{"
        + "},`externalID`:``}},{`id`:8,`pedigreeProperties`:{`gender`:`M`}},{`id`:11,`pedigreeProperties`:{`lNameAtB`"
        + ":`Smith`,`features`:[],`lifeStatus`:`unborn`,`gender`:`U`,`genes`:[],`nonstandard_features`:[]}}],`JSON_ve"
        + "rsion`:`1.0`,`proband`:0}").replace('`', '"');

    @Rule
    public MockitoComponentMockingRule<HibernateDataMigration> mocker =
        new MockitoComponentMockingRule<>(R71507PhenoTips3423DataMigration.class, HibernateDataMigration.class,
            "R71507-PT-3423");

    @Mock
    private Session session;

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki xwiki;

    @Mock
    private Query query;

    @Mock
    private DocumentReference documentReference1;

    @Mock
    private DocumentReference documentReference2;

    @Mock
    private BaseObject pedigreeBaseObject1;

    @Mock
    private BaseObject pedigreeBaseObject2;

    @Mock
    private XWikiDocument xDocument1;

    @Mock
    private XWikiDocument xDocument2;

    @Mock
    private XWikiHibernateStore store;

    private R71507PhenoTips3423DataMigration component;

    private Logger logger;

    private ComponentManager componentManager;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.mocker.registerMockComponent(ComponentManager.class);
        this.componentManager = this.mocker.getInstance(ComponentManager.class);
        when(this.componentManager.getInstance(XWikiStoreInterface.class, "hibernate")).thenReturn(this.store);

        this.component = (R71507PhenoTips3423DataMigration) this.mocker.getComponentUnderTest();
        this.logger = this.mocker.getMockedLogger();

        final EntityReferenceSerializer<String> serializer =
            this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING, "compactwiki");
        when(serializer.serialize(any(EntityReference.class))).thenReturn(SERIALIZED_ENTITY);

        final Execution execution = this.mocker.getInstance(Execution.class);
        final ExecutionContext executionContext = mock(ExecutionContext.class);
        when(execution.getContext()).thenReturn(executionContext);
        when(executionContext.getProperty(anyString())).thenReturn(this.context);
        when(this.context.getWiki()).thenReturn(this.xwiki);
        when(this.context.getWikiId()).thenReturn(WIKI_ID);

        when(this.session.createQuery(anyString())).thenReturn(this.query);

        final DocumentReferenceResolver<String> resolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        when(resolver.resolve(FAMILY_1)).thenReturn(this.documentReference1);
        when(resolver.resolve(FAMILY_2)).thenReturn(this.documentReference2);

        when(this.xwiki.getDocument(this.documentReference1, this.context)).thenReturn(this.xDocument1);
        when(this.xwiki.getDocument(this.documentReference2, this.context)).thenReturn(this.xDocument2);

        when(this.xDocument1.getXObject(any(EntityReference.class))).thenReturn(this.pedigreeBaseObject1);
        when(this.xDocument2.getXObject(any(EntityReference.class))).thenReturn(this.pedigreeBaseObject2);

        when(this.pedigreeBaseObject1.getStringValue(PEDIGREECLASS_JSONDATA_KEY)).thenReturn(PEDIGREE_1_DATA);
        when(this.pedigreeBaseObject2.getStringValue(PEDIGREECLASS_JSONDATA_KEY)).thenReturn(PEDIGREE_2_DATA);
    }

    @Test
    public void doInHibernateDoesNothingWhenFamiliesHaveNoXWikiDocument() throws XWikiException
    {
        when(this.query.list()).thenReturn(Collections.singletonList(FAMILY_1));
        when(this.xwiki.getDocument(this.documentReference1, this.context)).thenReturn(null);

        this.component.doInHibernate(this.session);

        verify(this.xwiki, times(1)).getDocument(any(DocumentReference.class), any(XWikiContext.class));
        verifyNoMoreInteractions(this.xwiki);
    }

    @Test
    public void doInHibernateDoesNothingWhenXWikiExceptionIsThrownForEachFamily() throws XWikiException
    {
        when(this.query.list()).thenReturn(Collections.singletonList(FAMILY_1));
        when(this.xwiki.getDocument(this.documentReference1, this.context)).thenThrow(new XWikiException());

        this.component.doInHibernate(this.session);

        verify(this.xwiki, times(1)).getDocument(any(DocumentReference.class), any(XWikiContext.class));
        verify(this.logger, times(1)).error(eq(ERROR_READING_DATA), eq(FAMILY_1), any());
        verifyNoMoreInteractions(this.xwiki);
    }

    @Test
    public void doInHibernateDoesNothingWhenFamiliesHaveNoPedigreeBaseObjects() throws XWikiException
    {
        when(this.query.list()).thenReturn(Collections.singletonList(FAMILY_1));
        when(this.xDocument1.getXObject(any(EntityReference.class))).thenReturn(null);

        this.component.doInHibernate(this.session);

        verify(this.xwiki, times(1)).getDocument(any(DocumentReference.class), any(XWikiContext.class));
        verify(this.xDocument1, times(1)).getXObject(any(EntityReference.class));

        verifyNoMoreInteractions(this.xwiki, this.xDocument1);
    }

    @Test
    public void doInHibernateDoesNothingWhenFamilyPedigreeHasNullData() throws XWikiException
    {
        when(this.query.list()).thenReturn(Collections.singletonList(FAMILY_1));
        when(this.pedigreeBaseObject1.getStringValue(PEDIGREECLASS_JSONDATA_KEY)).thenReturn(null);

        this.component.doInHibernate(this.session);

        verify(this.xwiki, times(1)).getDocument(any(DocumentReference.class), any(XWikiContext.class));
        verify(this.xDocument1, times(1)).getXObject(any(EntityReference.class));
        verify(this.pedigreeBaseObject1, times(1)).getStringValue(PEDIGREECLASS_JSONDATA_KEY);
        verifyNoMoreInteractions(this.xwiki, this.xDocument1, this.pedigreeBaseObject1);
    }

    @Test
    public void doInHibernateDoesNothingWhenFamilyPedigreeHasEmptyData() throws XWikiException
    {
        when(this.query.list()).thenReturn(Collections.singletonList(FAMILY_1));

        when(this.pedigreeBaseObject1.getStringValue(PEDIGREECLASS_JSONDATA_KEY)).thenReturn(StringUtils.EMPTY);

        this.component.doInHibernate(this.session);

        verify(this.xwiki, times(1)).getDocument(any(DocumentReference.class), any(XWikiContext.class));
        verify(this.xDocument1, times(1)).getXObject(any(EntityReference.class));
        verify(this.pedigreeBaseObject1, times(1)).getStringValue(PEDIGREECLASS_JSONDATA_KEY);
        verifyNoMoreInteractions(this.xwiki, this.xDocument1, this.pedigreeBaseObject1);
    }

    @Test
    public void doInHibernateDoesNothingWhenFamilyPedigreeHasInvalidData() throws XWikiException
    {
        when(this.query.list()).thenReturn(Collections.singletonList(FAMILY_1));
        when(this.pedigreeBaseObject1.getStringValue(PEDIGREECLASS_JSONDATA_KEY)).thenReturn("I'm not JSON");

        this.component.doInHibernate(this.session);

        verify(this.xwiki, times(1)).getDocument(any(DocumentReference.class), any(XWikiContext.class));
        verify(this.xDocument1, times(1)).getXObject(any(EntityReference.class));
        verify(this.pedigreeBaseObject1, times(1)).getStringValue(PEDIGREECLASS_JSONDATA_KEY);

        verify(this.logger, times(1)).error(eq(ERROR_UPDATING_DATA), eq(FAMILY_1), any());
        verifyNoMoreInteractions(this.xwiki, this.xDocument1, this.pedigreeBaseObject1);
    }

    @Test
    public void doInHibernateDoesNothingWhenPedigreeIsInSimpleJSONFormat() throws XWikiException
    {
        when(this.query.list()).thenReturn(Collections.singletonList(FAMILY_1));
        when(this.pedigreeBaseObject1.getStringValue(PEDIGREECLASS_JSONDATA_KEY))
            .thenReturn(PEDIGREE_SIMPLE_JSON_DATA);

        this.component.doInHibernate(this.session);

        verify(this.xwiki, times(1)).getDocument(any(DocumentReference.class), any(XWikiContext.class));
        verify(this.xDocument1, times(1)).getXObject(any(EntityReference.class));
        verify(this.pedigreeBaseObject1, times(1)).getStringValue(PEDIGREECLASS_JSONDATA_KEY);

        verify(this.logger, times(1)).warn(eq(WARNING_SIMPLE_JSON), eq(FAMILY_1));
        verifyNoMoreInteractions(this.xwiki, this.xDocument1, this.pedigreeBaseObject1);
    }

    @Test
    public void doInHibernateBehavesAsExpectedForSimplePedigree() throws XWikiException
    {
        when(this.query.list()).thenReturn(Collections.singletonList(FAMILY_1));

        this.component.doInHibernate(this.session);

        verify(this.xwiki, times(1)).getDocument(any(DocumentReference.class), any(XWikiContext.class));
        verify(this.xDocument1, times(1)).getXObject(any(EntityReference.class));

        verify(this.pedigreeBaseObject1, times(1)).getStringValue(PEDIGREECLASS_JSONDATA_KEY);

        verify(this.pedigreeBaseObject1, times(1)).set(eq(PEDIGREECLASS_JSONDATA_KEY), eq(PEDIGREE_1_MIGRATED_DATA),
            eq(this.context));

        verify(this.xDocument1, times(1)).setComment(this.component.getDescription());
        verify(this.xDocument1, times(1)).setMinorEdit(true);

        verify(this.store, times(1)).saveXWikiDoc(this.xDocument1, this.context, false);
    }

    @Test
    public void doInHibernateBehavesAsExpectedForComplexPedigreeWithMultipleProperties() throws XWikiException
    {
        when(this.query.list()).thenReturn(Collections.singletonList(FAMILY_2));

        this.component.doInHibernate(this.session);

        verify(this.xwiki, times(1)).getDocument(any(DocumentReference.class), any(XWikiContext.class));
        verify(this.xDocument2, times(1)).getXObject(any(EntityReference.class));

        verify(this.pedigreeBaseObject2, times(1)).getStringValue(PEDIGREECLASS_JSONDATA_KEY);

        verify(this.pedigreeBaseObject2, times(1)).set(eq(PEDIGREECLASS_JSONDATA_KEY), eq(PEDIGREE_2_MIGRATED_DATA),
            eq(this.context));

        verify(this.xDocument2, times(1)).setComment(this.component.getDescription());
        verify(this.xDocument2, times(1)).setMinorEdit(true);

        verify(this.store, times(1)).saveXWikiDoc(this.xDocument2, this.context, false);
    }

    @Test
    public void doInHibernateDoesNothingAndContinuesMigratingOnXWikiException()
        throws XWikiException
    {
        when(this.query.list()).thenReturn(Arrays.asList(FAMILY_1, FAMILY_2));

        when(this.xwiki.getDocument(this.documentReference1, this.context)).thenThrow(new XWikiException());

        this.component.doInHibernate(this.session);

        verify(this.xwiki, times(2)).getDocument(any(DocumentReference.class), eq(this.context));

        verify(this.xDocument1, never()).getXObject(any(EntityReference.class));
        verify(this.logger, times(1)).error(eq(ERROR_READING_DATA), eq(FAMILY_1), any());

        verify(this.xDocument2, times(1)).getXObject(any(EntityReference.class));

        verify(this.pedigreeBaseObject2, times(1)).getStringValue(PEDIGREECLASS_JSONDATA_KEY);

        verify(this.pedigreeBaseObject2, times(1)).set(eq(PEDIGREECLASS_JSONDATA_KEY), eq(PEDIGREE_2_MIGRATED_DATA),
            eq(this.context));


        verify(this.xDocument2, times(1)).setComment(this.component.getDescription());
        verify(this.xDocument2, times(1)).setMinorEdit(true);

        verify(this.store, never()).saveXWikiDoc(this.xDocument1, this.context, false);
        verify(this.store, times(1)).saveXWikiDoc(this.xDocument2, this.context, false);
    }
}
