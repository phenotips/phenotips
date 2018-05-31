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
package org.phenotips.entities.spi;

import org.phenotips.entities.PrimaryEntityConnectionsManager;
import org.phenotips.entities.PrimaryEntityManager;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AbstractOutgoingPrimaryEntityConnectionsManager}.
 */
public class OutgoingPrimaryEntityConnectionsManagerTest
{
    private static final DocumentReference PERSON1_REFERENCE = new DocumentReference("main", "Persons", "P01");

    private static final DocumentReference PERSON2_REFERENCE = new DocumentReference("main", "Persons", "P02");

    private static final DocumentReference PERSON3_REFERENCE = new DocumentReference("main", "Persons", "P03");

    private static final String PERSON1_SERIALIZED_REFERENCE = "main:Persons.P01";

    private static final String PERSON2_SERIALIZED_REFERENCE = "main:Persons.P02";

    private static final String PERSON3_SERIALIZED_REFERENCE = "main:Persons.P03";

    private static final DocumentReference FAMILY1_REFERENCE = new DocumentReference("main", "Families", "F01");

    private static final DocumentReference FAMILY2_REFERENCE = new DocumentReference("main", "Families", "F02");

    private static final DocumentReference FAMILY3_REFERENCE = new DocumentReference("main", "Families", "F03");

    private static final String REMOVE_COMMENT = "Removed connection to main:Persons.P01";

    private static final String ADD_COMMENT = "Added connection to main:Persons.P01";

    @Rule
    public final MockitoComponentMockingRule<PrimaryEntityConnectionsManager<Family, Person>> mocker =
        new MockitoComponentMockingRule<>(FamilyContainsPersonConnectionsManager.class);

    private PrimaryEntityConnectionsManager<Family, Person> manager;

    private PrimaryEntityManager<Person> persons;

    private PrimaryEntityManager<Family> families;

    private EntityReferenceSerializer<String> fullSerializer;

    private QueryManager qm;

    @Mock
    private Query connectionsQuery;

    @Mock
    private Query reverseConnectionsQuery;

    @Mock
    private XWikiContext xcontext;

    @Mock
    private XWiki xwiki;

    @Mock
    private Person person1;

    @Mock
    private Person person2;

    @Mock
    private Person person3;

    @Mock
    private Family family1;

    @Mock
    private Family family2;

    @Mock
    private Family family3;

    @Mock
    private XWikiDocument family1Doc;

    @Mock
    private XWikiDocument family2Doc;

    @Mock
    private XWikiDocument family3Doc;

    @Mock
    private BaseObject connection1Obj;

    @Mock
    private BaseObject connection2Obj;

    @Mock
    private BaseObject connection3Obj;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.manager = this.mocker.getComponentUnderTest();

        this.persons = this.mocker.getInstance(PersonsManager.TYPE, "Person");
        this.families = this.mocker.getInstance(FamiliesManager.TYPE, "Family");

        when(this.person1.getDocumentReference()).thenReturn(PERSON1_REFERENCE);
        when(this.person2.getDocumentReference()).thenReturn(PERSON2_REFERENCE);
        when(this.person3.getDocumentReference()).thenReturn(PERSON3_REFERENCE);

        when(this.family1.getXDocument()).thenReturn(this.family1Doc);
        when(this.family1.getDocumentReference()).thenReturn(FAMILY1_REFERENCE);
        when(this.family1Doc.newXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            this.xcontext)).thenReturn(this.connection1Obj, this.connection2Obj, this.connection3Obj);

        when(this.family2.getXDocument()).thenReturn(this.family2Doc);
        when(this.family2.getDocumentReference()).thenReturn(FAMILY2_REFERENCE);
        when(this.family2Doc.newXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            this.xcontext)).thenReturn(this.connection2Obj);

        when(this.family3.getXDocument()).thenReturn(this.family3Doc);
        when(this.family3.getDocumentReference()).thenReturn(FAMILY3_REFERENCE);
        when(this.family3Doc.newXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            this.xcontext)).thenReturn(this.connection3Obj);

        Provider<XWikiContext> xcprovider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcprovider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);

        this.fullSerializer = this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);
        when(this.fullSerializer.serialize(PERSON1_REFERENCE)).thenReturn(PERSON1_SERIALIZED_REFERENCE);
        when(this.fullSerializer.serialize(PERSON2_REFERENCE)).thenReturn(PERSON2_SERIALIZED_REFERENCE);
        when(this.fullSerializer.serialize(PERSON3_REFERENCE)).thenReturn(PERSON3_SERIALIZED_REFERENCE);

        this.qm = this.mocker.getInstance(QueryManager.class);

        when(this.qm.createQuery(
            // Select the referenced Objects
            "select distinct objectObj.name "
                + "  from BaseObject connectionObj, StringProperty referenceProperty, BaseObject objectObj"
                + "  where "
                // The connection is stored in the Subject
                + "    connectionObj.name = :subjectDocument and "
                + "    connectionObj.className = :connectionClass and "
                // The reference property belongs to the connection
                + "    referenceProperty.id.id = connectionObj.id and "
                + "    referenceProperty.id.name = :referenceProperty and "
                // The connection points to the Object
                + "    referenceProperty.value = concat(:wikiId, objectObj.name) and "
                // The Object must have the right type
                + "    objectObj.className = :objectClass",
            Query.HQL)).thenReturn(this.connectionsQuery);
        when(this.connectionsQuery.execute()).thenReturn(Collections.singletonList("Persons.P01"));

        when(this.qm.createQuery(
            // Look for Subject documents
            ", BaseObject subjectObj, BaseObject connectionObj, StringProperty property "
                + "  where "
                // The Subject must have the right type
                + "    subjectObj.name = doc.fullName and "
                + "    subjectObj.className = :subjectClass and "
                // The connection is stored in the Subject
                + "    connectionObj.name = doc.fullName and "
                + "    connectionObj.className = :connectionClass and "
                // The reference property belongs to the connection
                + "    property.id.id = connectionObj.id and "
                + "    property.id.name = :referenceProperty and "
                // The reference property references the target Object
                + "    property.value = :objectDocument",
            Query.HQL)).thenReturn(this.reverseConnectionsQuery);
        when(this.reverseConnectionsQuery.execute()).thenReturn(Collections.singletonList("Families.F01"));

        when(this.persons.get("Persons.P01")).thenReturn(this.person1);
        when(this.persons.get("Persons.P02")).thenReturn(this.person2);
        when(this.persons.get("Persons.P03")).thenReturn(this.person3);
        when(this.families.get("Families.F01")).thenReturn(this.family1);
        when(this.families.get("Families.F02")).thenReturn(this.family2);
        when(this.families.get("Families.F03")).thenReturn(this.family3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectWithNullSubjectThrowsException() throws Exception
    {
        this.manager.connect(null, this.person1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectWithNullObjectThrowsException() throws Exception
    {
        this.manager.connect(this.family1, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectWithNoDocumentSubjectThrowsException() throws Exception
    {
        when(this.family1.getXDocument()).thenReturn(null);
        this.manager.connect(this.family1, this.person1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectWithNoReferenceObjectThrowsException() throws Exception
    {
        when(this.person1.getDocumentReference()).thenReturn(null);
        this.manager.connect(this.family1, this.person1);
    }

    @Test
    public void connectStoresConnectionInTheObject() throws XWikiException
    {
        Assert.assertTrue(this.manager.connect(this.family1, this.person1));

        verify(this.family1Doc).newXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            this.xcontext);
        verify(this.connection1Obj).setStringValue(AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON1_SERIALIZED_REFERENCE);
        verify(this.xwiki).saveDocument(this.family1Doc, ADD_COMMENT, true, this.xcontext);
    }

    @Test
    public void connectDoesNotDuplicateConnection() throws XWikiException
    {
        when(this.family1Doc.getXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON1_SERIALIZED_REFERENCE, false)).thenReturn(this.connection1Obj);
        Assert.assertTrue(this.manager.connect(this.family1, this.person1));
        verify(this.family1Doc, Mockito.never()).newXObject(
            AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            this.xcontext);
        Mockito.verifyZeroInteractions(this.xwiki);
    }

    @Test
    public void connectAllConnectsEveryFamily() throws XWikiException
    {
        Assert
            .assertTrue(this.manager.connectAll(this.family1, Arrays.asList(this.person1, this.person2, this.person3)));

        verify(this.family1Doc, Mockito.times(3)).newXObject(
            AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            this.xcontext);
        verify(this.connection1Obj).setStringValue(AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON1_SERIALIZED_REFERENCE);
        verify(this.connection2Obj).setStringValue(AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON2_SERIALIZED_REFERENCE);
        verify(this.connection3Obj).setStringValue(AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON3_SERIALIZED_REFERENCE);
        verify(this.xwiki).saveDocument(this.family1Doc, ADD_COMMENT, true, this.xcontext);
        verify(this.xwiki).saveDocument(this.family1Doc, "Added connection to main:Persons.P02", true, this.xcontext);
        verify(this.xwiki).saveDocument(this.family1Doc, "Added connection to main:Persons.P03", true, this.xcontext);
    }

    @Test
    public void connectAllSkipsNull() throws XWikiException
    {
        Assert.assertTrue(this.manager.connectAll(this.family1, Arrays.asList(null, this.person1, null, this.person2)));
        verify(this.family1Doc, Mockito.times(2)).newXObject(
            AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            this.xcontext);
        verify(this.connection1Obj).setStringValue(AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON1_SERIALIZED_REFERENCE);
        verify(this.connection2Obj).setStringValue(
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON2_SERIALIZED_REFERENCE);
    }

    @Test
    public void connectAllReturnsFalseOnAnyFailureAndCreatesAllOtherConnections() throws XWikiException
    {
        when(this.family1Doc.newXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            this.xcontext)).thenReturn(this.connection1Obj).thenThrow(new XWikiException())
                .thenReturn(this.connection3Obj);

        Assert.assertFalse(
            this.manager.connectAll(this.family1, Arrays.asList(this.person1, this.person2, this.person3)));

        verify(this.xwiki).saveDocument(this.family1Doc, ADD_COMMENT, true, this.xcontext);
        verify(this.xwiki).saveDocument(this.family1Doc, "Added connection to main:Persons.P03", true, this.xcontext);
    }

    @Test
    public void connectAllIgnoresEmptyList() throws XWikiException
    {
        Assert.assertTrue(this.manager.connectAll(this.family1, Collections.emptyList()));
    }

    @Test
    public void connectAllIgnoresNullList() throws XWikiException
    {
        Assert.assertTrue(this.manager.connectAll(this.family1, null));
    }

    @Test
    public void connectAllIgnoresAllNullList() throws XWikiException
    {
        Assert.assertTrue(this.manager.connectAll(this.family1, Arrays.asList(null, null)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectAllWithNullSubjectThrowsException()
    {
        this.manager.connectAll(null, Arrays.asList(this.person1));
    }

    @Test
    public void connectFromAllConnectsEveryPatient() throws XWikiException
    {
        Assert.assertTrue(
            this.manager.connectFromAll(Arrays.asList(this.family1, this.family2, this.family3), this.person1));

        verify(this.family1Doc).newXObject(
            AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            this.xcontext);
        verify(this.family2Doc).newXObject(
            AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            this.xcontext);
        verify(this.family3Doc).newXObject(
            AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            this.xcontext);
        verify(this.connection1Obj).setStringValue(AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON1_SERIALIZED_REFERENCE);
        verify(this.connection2Obj).setStringValue(AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON1_SERIALIZED_REFERENCE);
        verify(this.connection3Obj).setStringValue(AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON1_SERIALIZED_REFERENCE);
        verify(this.xwiki).saveDocument(this.family1Doc, ADD_COMMENT, true, this.xcontext);
        verify(this.xwiki).saveDocument(this.family2Doc, ADD_COMMENT, true, this.xcontext);
        verify(this.xwiki).saveDocument(this.family3Doc, ADD_COMMENT, true, this.xcontext);
    }

    @Test
    public void connectFromAllSkipsNull() throws XWikiException
    {
        Assert.assertTrue(
            this.manager.connectFromAll(Arrays.asList(null, this.family1, null, this.family2, null), this.person1));
        verify(this.family1Doc).newXObject(
            AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            this.xcontext);
        verify(this.family2Doc).newXObject(
            AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            this.xcontext);
        verify(this.connection1Obj).setStringValue(AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON1_SERIALIZED_REFERENCE);
        verify(this.connection2Obj).setStringValue(AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON1_SERIALIZED_REFERENCE);
        verify(this.xwiki).saveDocument(this.family1Doc, ADD_COMMENT, true, this.xcontext);
    }

    @Test
    public void connectFromAllReturnsFalseOnAnyFailureAndCreatesAllOtherConnections() throws XWikiException
    {
        when(this.family2Doc.newXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            this.xcontext)).thenThrow(new XWikiException());

        Assert.assertFalse(
            this.manager.connectFromAll(Arrays.asList(this.family1, this.family2, this.family3), this.person1));

        verify(this.xwiki).saveDocument(this.family1Doc, ADD_COMMENT, true, this.xcontext);
        verify(this.xwiki, Mockito.never()).saveDocument(this.family1Doc, "Added connection to main:Persons.P02", true,
            this.xcontext);
        verify(this.xwiki).saveDocument(this.family3Doc, ADD_COMMENT, true, this.xcontext);
    }

    @Test
    public void connectFromAllIgnoresEmptyList() throws XWikiException
    {
        Assert.assertTrue(this.manager.connectFromAll(Collections.emptyList(), this.person1));
    }

    @Test
    public void connectFromAllIgnoresNullList() throws XWikiException
    {
        Assert.assertTrue(this.manager.connectFromAll(null, this.person1));
    }

    @Test
    public void connectFromAllIgnoresAllNullList() throws XWikiException
    {
        Assert.assertTrue(this.manager.connectFromAll(Arrays.asList(null, null), this.person1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectFromAllWithNullObjectThrowsException()
    {
        this.manager.connectFromAll(Arrays.asList(this.family1), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void disconnectWithNullSubjectThrowsException() throws Exception
    {
        this.manager.disconnect(null, this.person1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void disconnectWithNullObjectThrowsException() throws Exception
    {
        this.manager.disconnect(this.family1, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void disconnectWithNoDocumentSubjectThrowsException() throws Exception
    {
        when(this.family1.getXDocument()).thenReturn(null);
        this.manager.disconnect(this.family1, this.person1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void disconnectWithNoReferenceObjectThrowsException() throws Exception
    {
        when(this.person1.getDocumentReference()).thenReturn(null);
        this.manager.disconnect(this.family1, this.person1);
    }

    @Test
    public void disconnectDeletesConnectionFromTheObject() throws XWikiException
    {
        when(this.family1Doc.getXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON1_SERIALIZED_REFERENCE, false)).thenReturn(this.connection1Obj);

        Assert.assertTrue(this.manager.disconnect(this.family1, this.person1));

        verify(this.family1Doc).getXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON1_SERIALIZED_REFERENCE, false);
        verify(this.family1Doc).removeXObject(this.connection1Obj);
        verify(this.xwiki).saveDocument(same(this.family1Doc), any(String.class), any(Boolean.class),
            same(this.xcontext));
    }

    @Test
    public void disconnectDoesNothingWhenConnectionNotFound() throws XWikiException
    {
        Assert.assertTrue(this.manager.disconnect(this.family1, this.person1));

        verify(this.family1Doc).getXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON1_SERIALIZED_REFERENCE, false);
        verify(this.family1Doc, Mockito.never()).removeXObject(this.connection1Obj);
        Mockito.verifyZeroInteractions(this.xwiki);
    }

    @Test
    public void disconnectAllDisonnectsEveryFamily() throws XWikiException
    {
        when(this.family1Doc.getXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON1_SERIALIZED_REFERENCE, false)).thenReturn(this.connection1Obj);
        when(this.family1Doc.getXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON2_SERIALIZED_REFERENCE, false)).thenReturn(this.connection2Obj);
        when(this.family1Doc.getXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON3_SERIALIZED_REFERENCE, false)).thenReturn(this.connection3Obj);

        Assert
            .assertTrue(
                this.manager.disconnectAll(this.family1, Arrays.asList(this.person1, this.person2, this.person3)));

        verify(this.family1Doc).removeXObject(this.connection1Obj);
        verify(this.family1Doc).removeXObject(this.connection2Obj);
        verify(this.family1Doc).removeXObject(this.connection3Obj);
        verify(this.xwiki).saveDocument(this.family1Doc, REMOVE_COMMENT, true, this.xcontext);
        verify(this.xwiki).saveDocument(this.family1Doc, "Removed connection to main:Persons.P02", true, this.xcontext);
        verify(this.xwiki).saveDocument(this.family1Doc, "Removed connection to main:Persons.P03", true, this.xcontext);
    }

    @Test
    public void disconnectAllSkipsNull() throws XWikiException
    {
        when(this.family1Doc.getXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON1_SERIALIZED_REFERENCE, false)).thenReturn(this.connection1Obj);
        when(this.family1Doc.getXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON2_SERIALIZED_REFERENCE, false)).thenReturn(this.connection2Obj);

        Assert.assertTrue(
            this.manager.disconnectAll(this.family1, Arrays.asList(null, this.person1, null, this.person2)));

        verify(this.family1Doc).removeXObject(this.connection1Obj);
        verify(this.family1Doc).removeXObject(this.connection2Obj);
        verify(this.xwiki).saveDocument(this.family1Doc, REMOVE_COMMENT, true, this.xcontext);
        verify(this.xwiki).saveDocument(this.family1Doc, "Removed connection to main:Persons.P02", true, this.xcontext);
    }

    @Test
    public void disconnectAllReturnsFalseOnAnyFailureAndRemovesAllOtherConnections() throws XWikiException
    {
        when(this.family1Doc.getXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON1_SERIALIZED_REFERENCE, false)).thenReturn(this.connection1Obj);
        when(this.family1Doc.getXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON2_SERIALIZED_REFERENCE, false)).thenReturn(this.connection2Obj);
        when(this.family1Doc.getXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON3_SERIALIZED_REFERENCE, false)).thenReturn(this.connection3Obj);
        Mockito.doThrow(new XWikiException()).when(this.xwiki).saveDocument(this.family1Doc,
            "Removed connection to main:Persons.P02", true, this.xcontext);

        Assert.assertFalse(
            this.manager.disconnectAll(this.family1, Arrays.asList(this.person1, this.person2, this.person3)));

        verify(this.family1Doc).removeXObject(this.connection1Obj);
        verify(this.family1Doc).removeXObject(this.connection3Obj);
        verify(this.xwiki).saveDocument(this.family1Doc, REMOVE_COMMENT, true, this.xcontext);
        verify(this.xwiki).saveDocument(this.family1Doc, "Removed connection to main:Persons.P03", true, this.xcontext);
    }

    @Test
    public void disconnectAllIgnoresEmptyList() throws XWikiException
    {
        Assert.assertTrue(this.manager.disconnectAll(this.family1, Collections.emptyList()));
    }

    @Test
    public void disconnectAllIgnoresNullList() throws XWikiException
    {
        Assert.assertTrue(this.manager.disconnectAll(this.family1, null));
    }

    @Test
    public void disconnectAllIgnoresAllNullList() throws XWikiException
    {
        Assert.assertTrue(this.manager.disconnectAll(this.family1, Arrays.asList(null, null)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void disconnectAllWithNullSubjectThrowsException()
    {
        this.manager.disconnectAll(null, Arrays.asList(this.person1));
    }

    @Test
    public void disconnectSubjectRemovesAllConnections() throws XWikiException
    {
        when(this.family1Doc.getXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON1_SERIALIZED_REFERENCE, false)).thenReturn(this.connection1Obj);

        Assert.assertTrue(this.manager.disconnectAll(this.family1));

        verify(this.family1Doc).removeXObject(this.connection1Obj);
        verify(this.xwiki).saveDocument(this.family1Doc, REMOVE_COMMENT, true, this.xcontext);
    }

    @Test
    public void disconnectSubjectReturnsFalseOnAnyFailureAndRemovesAllOtherConnections()
        throws XWikiException, QueryException
    {
        when(this.connectionsQuery.execute()).thenReturn(Arrays.asList("Persons.P01", "Persons.P02", "Persons.P03"));

        when(this.family1Doc.getXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON1_SERIALIZED_REFERENCE, false)).thenReturn(this.connection1Obj);
        when(this.family1Doc.getXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON2_SERIALIZED_REFERENCE, false)).thenReturn(this.connection2Obj);
        when(this.family1Doc.getXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON3_SERIALIZED_REFERENCE, false)).thenReturn(this.connection3Obj);
        Mockito.doThrow(new XWikiException()).when(this.xwiki).saveDocument(this.family1Doc,
            "Removed connection to main:Persons.P02", true, this.xcontext);

        Assert.assertFalse(this.manager.disconnectAll(this.family1));

        verify(this.family1Doc).removeXObject(this.connection1Obj);
        verify(this.xwiki).saveDocument(this.family1Doc, REMOVE_COMMENT, true, this.xcontext);
        verify(this.family1Doc).removeXObject(this.connection2Obj);
        verify(this.xwiki).saveDocument(this.family1Doc, "Removed connection to main:Persons.P02", true, this.xcontext);
        verify(this.family1Doc).removeXObject(this.connection3Obj);
        verify(this.xwiki).saveDocument(this.family1Doc, "Removed connection to main:Persons.P03", true, this.xcontext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void disconnectNullSubjectThrowsException()
    {
        this.manager.disconnectAll(null);
    }

    @Test
    public void disconnectObjectRemovesAllConnections() throws XWikiException
    {
        when(this.family1Doc.getXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON1_SERIALIZED_REFERENCE, false)).thenReturn(this.connection1Obj);

        Assert.assertTrue(this.manager.disconnectFromAll(this.person1));

        verify(this.family1Doc).removeXObject(this.connection1Obj);
        verify(this.xwiki).saveDocument(this.family1Doc, REMOVE_COMMENT, true, this.xcontext);
    }

    @Test
    public void disconnectObjectReturnsFalseOnAnyFailureAndRemovesAllOtherConnections()
        throws XWikiException, QueryException
    {
        when(this.reverseConnectionsQuery.execute())
            .thenReturn(Arrays.asList("Families.F01", "Families.F02", "Families.F03"));
        when(this.family1Doc.getXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON1_SERIALIZED_REFERENCE, false)).thenReturn(this.connection1Obj);
        when(this.family2Doc.getXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON1_SERIALIZED_REFERENCE, false)).thenReturn(this.connection2Obj);
        when(this.family3Doc.getXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON1_SERIALIZED_REFERENCE, false)).thenReturn(this.connection3Obj);
        Mockito.doThrow(new XWikiException()).when(this.xwiki).saveDocument(this.family2Doc,
            "Removed connection to main:Persons.P01", true, this.xcontext);

        Assert.assertFalse(this.manager.disconnectFromAll(this.person1));

        verify(this.family1Doc).removeXObject(this.connection1Obj);
        verify(this.family3Doc).removeXObject(this.connection3Obj);
        verify(this.xwiki).saveDocument(this.family1Doc, REMOVE_COMMENT, true, this.xcontext);
        verify(this.xwiki).saveDocument(this.family3Doc, REMOVE_COMMENT, true, this.xcontext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void disconnectNullObjectThrowsException()
    {
        this.manager.disconnectFromAll(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getAllConnectionsWithNullSubjectThrowsException()
    {
        this.manager.getAllConnections(null);
    }

    @Test
    public void getAllConnectionsWorks()
    {
        Collection<Person> result = this.manager.getAllConnections(this.family1);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertSame(this.person1, result.iterator().next());
    }

    @Test
    public void getAllConnectionsReturnsEmptyListOnException() throws QueryException
    {
        when(this.connectionsQuery.execute()).thenThrow(new QueryException("", null, null));
        Collection<Person> result = this.manager.getAllConnections(this.family1);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getAllReverseConnectionsWithNullObjectThrowsException()
    {
        this.manager.getAllReverseConnections(null);
    }

    @Test
    public void getAllReverseConnectionsWorks()
    {
        Collection<Family> result = this.manager.getAllReverseConnections(this.person1);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertSame(this.family1, result.iterator().next());
    }

    @Test
    public void getAllReverseConnectionsReturnsEmptyListOnException() throws QueryException
    {
        when(this.reverseConnectionsQuery.execute()).thenThrow(new QueryException("", null, null));
        Collection<Family> result = this.manager.getAllReverseConnections(this.person1);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void isConnectedWithNullSubjectReturnsFalse()
    {
        Assert.assertFalse(this.manager.isConnected(null, this.person1));
    }

    @Test
    public void isConnectedWithNullObjectReturnsFalse()
    {
        Assert.assertFalse(this.manager.isConnected(this.family1, null));
    }

    @Test
    public void isConnectedWorks()
    {
        Assert.assertTrue(this.manager.isConnected(this.family1, this.person1));
        Assert.assertFalse(this.manager.isConnected(this.family2, this.person3));
    }

    @Test
    public void getWithSingleConnectionReturnsConnectedPerson()
    {
        Assert.assertSame(this.person1, this.manager.get(this.family1));
    }

    @Test
    public void getWithNoConnectionReturnsNull() throws QueryException
    {
        when(this.connectionsQuery.execute()).thenReturn(Collections.emptyList());
        Assert.assertNull(this.manager.get(this.family1));
    }

    @Test
    public void getWithMultipleConnectionsReturnsNull() throws QueryException
    {
        when(this.connectionsQuery.execute()).thenReturn(Arrays.asList("Persons.P01", "Persons.P02"));
        Assert.assertNull(this.manager.get(this.family1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getWithNullSubjectThrowsException()
    {
        this.manager.get(null);
    }

    @Test
    public void setDeletesPreviousConnectionsAndConnectsNewObject() throws QueryException, XWikiException
    {
        when(this.connectionsQuery.execute()).thenReturn(Arrays.asList("Persons.P02", "Persons.P03"));

        when(this.family1Doc.getXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON2_SERIALIZED_REFERENCE, false)).thenReturn(this.connection2Obj);
        when(this.family1Doc.getXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON3_SERIALIZED_REFERENCE, false)).thenReturn(this.connection3Obj);

        Assert.assertTrue(this.manager.set(this.family1, this.person1));

        verify(this.family1Doc).removeXObject(this.connection2Obj);
        verify(this.xwiki).saveDocument(this.family1Doc, "Removed connection to main:Persons.P02", true, this.xcontext);
        verify(this.family1Doc).removeXObject(this.connection3Obj);
        verify(this.xwiki).saveDocument(this.family1Doc, "Removed connection to main:Persons.P03", true, this.xcontext);

        verify(this.connection1Obj).setStringValue(AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON1_SERIALIZED_REFERENCE);
        verify(this.xwiki).saveDocument(this.family1Doc, ADD_COMMENT, true, this.xcontext);
    }

    @Test
    public void setDeletesPreviousConnectionsButStopsOnFailure() throws QueryException, XWikiException
    {
        when(this.connectionsQuery.execute()).thenReturn(Arrays.asList("Persons.P02", "Persons.P03"));

        when(this.family1Doc.getXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON2_SERIALIZED_REFERENCE, false)).thenReturn(this.connection2Obj);
        when(this.family1Doc.getXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON3_SERIALIZED_REFERENCE, false)).thenReturn(this.connection3Obj);
        Mockito.doThrow(new XWikiException()).when(this.xwiki).saveDocument(this.family1Doc,
            "Removed connection to main:Persons.P02", true, this.xcontext);

        Assert.assertFalse(this.manager.set(this.family1, this.person1));

        verify(this.xwiki, Mockito.never()).saveDocument(this.family1Doc, ADD_COMMENT, true,
            this.xcontext);
        verify(this.family1Doc).removeXObject(this.connection2Obj);
        verify(this.xwiki).saveDocument(this.family1Doc, "Removed connection to main:Persons.P02", true, this.xcontext);
        verify(this.family1Doc).removeXObject(this.connection3Obj);
        verify(this.xwiki).saveDocument(this.family1Doc, "Removed connection to main:Persons.P03", true, this.xcontext);
    }

    @Test
    public void setToNullDeletesPreviousConnections() throws QueryException, XWikiException
    {
        when(this.connectionsQuery.execute()).thenReturn(Arrays.asList("Persons.P02", "Persons.P03"));

        when(this.family1Doc.getXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON2_SERIALIZED_REFERENCE, false)).thenReturn(this.connection2Obj);
        when(this.family1Doc.getXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON3_SERIALIZED_REFERENCE, false)).thenReturn(this.connection3Obj);

        Assert.assertTrue(this.manager.set(this.family1, null));

        verify(this.family1Doc).removeXObject(this.connection2Obj);
        verify(this.xwiki).saveDocument(this.family1Doc, "Removed connection to main:Persons.P02", true, this.xcontext);
        verify(this.family1Doc).removeXObject(this.connection3Obj);
        verify(this.xwiki).saveDocument(this.family1Doc, "Removed connection to main:Persons.P03", true, this.xcontext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setWithNullSubjectThrowsException()
    {
        this.manager.set(null, this.person1);
    }

    @Test
    public void removeDeletesPreviousConnections() throws QueryException, XWikiException
    {
        when(this.connectionsQuery.execute()).thenReturn(Arrays.asList("Persons.P01", "Persons.P02", "Persons.P03"));

        when(this.family1Doc.getXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON1_SERIALIZED_REFERENCE, false)).thenReturn(this.connection1Obj);
        when(this.family1Doc.getXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON2_SERIALIZED_REFERENCE, false)).thenReturn(this.connection2Obj);
        when(this.family1Doc.getXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON3_SERIALIZED_REFERENCE, false)).thenReturn(this.connection3Obj);

        Assert.assertTrue(this.manager.remove(this.family1));

        verify(this.family1Doc).removeXObject(this.connection1Obj);
        verify(this.xwiki).saveDocument(this.family1Doc, REMOVE_COMMENT, true, this.xcontext);
        verify(this.family1Doc).removeXObject(this.connection2Obj);
        verify(this.xwiki).saveDocument(this.family1Doc, "Removed connection to main:Persons.P02", true,
            this.xcontext);
        verify(this.family1Doc).removeXObject(this.connection3Obj);
        verify(this.xwiki).saveDocument(this.family1Doc, "Removed connection to main:Persons.P03", true,
            this.xcontext);
    }

    @Test
    public void removeReturnsFalseOnFailureButRemovesAllOtherConnections() throws QueryException, XWikiException
    {
        when(this.connectionsQuery.execute()).thenReturn(Arrays.asList("Persons.P01", "Persons.P02", "Persons.P03"));

        when(this.family1Doc.getXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON1_SERIALIZED_REFERENCE, false)).thenReturn(this.connection1Obj);
        when(this.family1Doc.getXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON2_SERIALIZED_REFERENCE, false)).thenReturn(this.connection2Obj);
        when(this.family1Doc.getXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON3_SERIALIZED_REFERENCE, false)).thenReturn(this.connection3Obj);
        Mockito.doThrow(new XWikiException()).when(this.xwiki).saveDocument(this.family1Doc,
            "Removed connection to main:Persons.P02", true, this.xcontext);

        Assert.assertFalse(this.manager.remove(this.family1));

        verify(this.family1Doc).removeXObject(this.connection1Obj);
        verify(this.xwiki).saveDocument(this.family1Doc, REMOVE_COMMENT, true, this.xcontext);
        verify(this.family1Doc).removeXObject(this.connection2Obj);
        verify(this.xwiki).saveDocument(this.family1Doc, "Removed connection to main:Persons.P02", true, this.xcontext);
        verify(this.family1Doc).removeXObject(this.connection3Obj);
        verify(this.xwiki).saveDocument(this.family1Doc, "Removed connection to main:Persons.P03", true, this.xcontext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeWithNullSubjectThrowsException()
    {
        this.manager.remove(null);
    }
}
