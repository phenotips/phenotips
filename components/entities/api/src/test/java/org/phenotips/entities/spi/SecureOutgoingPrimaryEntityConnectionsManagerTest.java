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
import org.phenotips.security.authorization.AuthorizationService;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

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
 * Unit tests for {@link AbstractSecureOutgoingPrimaryEntityConnectionsManager}.
 */
public class SecureOutgoingPrimaryEntityConnectionsManagerTest
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
        new MockitoComponentMockingRule<>(SecureFamilyContainsPersonConnectionsManager.class);

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

    private UserManager userManager;

    @Mock
    private User user;

    private AuthorizationService auth;

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
            this.xcontext)).thenReturn(this.connection1Obj);

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

        this.userManager = this.mocker.getInstance(UserManager.class);
        when(this.userManager.getCurrentUser()).thenReturn(this.user);

        this.auth = this.mocker.getInstance(AuthorizationService.class);
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

    @Test
    public void connectStoresConnectionInTheSubject() throws XWikiException
    {
        grantFullAccess();

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
        grantFullAccess();
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
    public void connectReturnsFalseWhenAccessDeniedToObject() throws XWikiException
    {
        when(this.auth.hasAccess(this.user, Right.EDIT, FAMILY1_REFERENCE)).thenReturn(true);
        when(this.auth.hasAccess(this.user, Right.EDIT, PERSON1_REFERENCE)).thenReturn(false);

        Assert.assertFalse(this.manager.connect(this.family1, this.person1));

        Mockito.verifyZeroInteractions(this.xwiki, this.family1Doc);
    }

    @Test
    public void connectReturnsFalseWhenAccessDeniedToSubject() throws XWikiException
    {
        when(this.auth.hasAccess(this.user, Right.EDIT, FAMILY1_REFERENCE)).thenReturn(false);
        when(this.auth.hasAccess(this.user, Right.EDIT, PERSON1_REFERENCE)).thenReturn(true);

        Assert.assertFalse(this.manager.connect(this.family1, this.person1));

        Mockito.verifyZeroInteractions(this.xwiki, this.family1Doc);
    }

    @Test
    public void connectAllConnectsEveryAccessiblePatient() throws XWikiException
    {
        grantAccessExcept2();

        Assert.assertFalse(
            this.manager.connectAll(this.family1, Arrays.asList(this.person1, this.person2, this.person3)));

        verify(this.family1Doc, Mockito.times(2)).newXObject(
            AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            this.xcontext);
        verify(this.connection1Obj).setStringValue(AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON1_SERIALIZED_REFERENCE);
        verify(this.connection1Obj, Mockito.never()).setStringValue(
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY, PERSON2_SERIALIZED_REFERENCE);
        verify(this.connection1Obj).setStringValue(AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON3_SERIALIZED_REFERENCE);
        verify(this.xwiki).saveDocument(this.family1Doc, ADD_COMMENT, true, this.xcontext);
        verify(this.xwiki, Mockito.never()).saveDocument(this.family1Doc, "Added connection to main:Persons.P02", true,
            this.xcontext);
        verify(this.xwiki).saveDocument(this.family1Doc, "Added connection to main:Persons.P03", true, this.xcontext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectAllWithNullSubjectThrowsException()
    {
        grantFullAccess();
        this.manager.connectAll(null, Arrays.asList(this.person1));
    }

    @Test
    public void connectAllWithInaccessibleSubjectDoesNothing()
    {
        grantFullAccess();
        when(this.auth.hasAccess(this.user, Right.EDIT, FAMILY1_REFERENCE)).thenReturn(false);

        Assert.assertFalse(this.manager.connectAll(this.family1, Arrays.asList(this.person1)));

        Mockito.verifyZeroInteractions(this.xwiki, this.family1Doc);
    }

    @Test
    public void connectFromAllConnectsEveryAccessibleFamily() throws XWikiException
    {
        grantAccessExcept2();

        Assert
            .assertFalse(
                this.manager.connectFromAll(Arrays.asList(this.family1, this.family2, this.family3), this.person1));

        verify(this.family1Doc).newXObject(AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            this.xcontext);
        verify(this.connection1Obj).setStringValue(AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON1_SERIALIZED_REFERENCE);
        verify(this.xwiki).saveDocument(this.family1Doc, ADD_COMMENT, true, this.xcontext);

        verify(this.family2Doc, Mockito.never()).newXObject(
            AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            this.xcontext);
        verify(this.xwiki, Mockito.never()).saveDocument(this.family2Doc, ADD_COMMENT, true, this.xcontext);

        verify(this.family3Doc).newXObject(
            AbstractOutgoingPrimaryEntityConnectionsManager.OUTGOING_CONNECTION_XCLASS,
            this.xcontext);
        verify(this.connection3Obj).setStringValue(
            AbstractOutgoingPrimaryEntityConnectionsManager.REFERENCE_XPROPERTY,
            PERSON1_SERIALIZED_REFERENCE);
        verify(this.xwiki).saveDocument(this.family3Doc, ADD_COMMENT, true, this.xcontext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectFromAllWithNullObjectThrowsException()
    {
        grantFullAccess();
        this.manager.connectFromAll(Arrays.asList(this.family1), null);
    }

    @Test
    public void connectFromAllWithInaccessibleObjectDoesNothing()
    {
        grantFullAccess();
        when(this.auth.hasAccess(this.user, Right.EDIT, PERSON1_REFERENCE)).thenReturn(false);

        Assert.assertFalse(this.manager.connectFromAll(Arrays.asList(this.family1), this.person1));

        Mockito.verifyZeroInteractions(this.xwiki, this.family1Doc);
    }

    @Test(expected = IllegalArgumentException.class)
    public void disconnectWithNullObjectThrowsException() throws Exception
    {
        grantFullAccess();
        this.manager.disconnect(this.family1, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void disconnectWithNullSubjectThrowsException() throws Exception
    {
        grantFullAccess();
        this.manager.disconnect(null, this.person1);
    }

    @Test
    public void disconnectDeletesConnectionFromTheSubject() throws XWikiException
    {
        grantFullAccess();

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
    public void disconnectDoesNothingWhenObjectIsInaccessible() throws XWikiException
    {
        when(this.auth.hasAccess(this.user, Right.EDIT, PERSON1_REFERENCE)).thenReturn(false);
        when(this.auth.hasAccess(this.user, Right.EDIT, FAMILY1_REFERENCE)).thenReturn(true);

        Assert.assertFalse(this.manager.disconnect(this.family1, this.person1));

        Mockito.verifyZeroInteractions(this.xwiki, this.family1Doc);
    }

    @Test
    public void disconnectDoesNothingWhenSubjectIsInaccessible() throws XWikiException
    {
        when(this.auth.hasAccess(this.user, Right.EDIT, PERSON1_REFERENCE)).thenReturn(true);
        when(this.auth.hasAccess(this.user, Right.EDIT, FAMILY1_REFERENCE)).thenReturn(false);

        Assert.assertFalse(this.manager.disconnect(this.family1, this.person1));

        Mockito.verifyZeroInteractions(this.xwiki, this.family1Doc);
    }

    @Test
    public void disconnectAllDisconnectsEveryAccessiblePerson() throws XWikiException
    {
        grantAccessTo1();

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
            .assertFalse(
                this.manager.disconnectAll(this.family1, Arrays.asList(this.person1, this.person2, this.person3)));

        verify(this.family1Doc).removeXObject(this.connection1Obj);
        verify(this.xwiki).saveDocument(this.family1Doc, REMOVE_COMMENT, true, this.xcontext);

        Mockito.verifyZeroInteractions(this.connection3Obj, this.connection2Obj);
        verify(this.xwiki, Mockito.never()).saveDocument(this.family1Doc, "Removed connection to main:Persons.P02",
            true, this.xcontext);
        verify(this.xwiki, Mockito.never()).saveDocument(this.family1Doc, "Removed connection to main:Persons.P03",
            true, this.xcontext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void disconnectAllWithNullSubjectThrowsException()
    {
        grantFullAccess();
        this.manager.disconnectAll(null, Arrays.asList(this.person1));
    }

    @Test
    public void disconnectSubjectRemovesAllAccessibleConnections() throws XWikiException, QueryException
    {
        grantAccessTo1();

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

        Assert.assertFalse(this.manager.disconnectAll(this.family1));

        verify(this.family1Doc).removeXObject(this.connection1Obj);
        verify(this.xwiki).saveDocument(this.family1Doc, REMOVE_COMMENT, true, this.xcontext);
        verify(this.family1Doc, Mockito.never()).removeXObject(this.connection2Obj);
        verify(this.xwiki, Mockito.never()).saveDocument(this.family1Doc, "Removed connection to main:Persons.P02",
            true, this.xcontext);
        verify(this.family1Doc, Mockito.never()).removeXObject(this.connection3Obj);
        verify(this.xwiki, Mockito.never()).saveDocument(this.family1Doc, "Removed connection to main:Persons.P03",
            true, this.xcontext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void disconnectNullSubjectThrowsException()
    {
        grantFullAccess();
        this.manager.disconnectAll(null);
    }

    @Test
    public void disconnectInaccessibleSubjectDoesNothing()
    {
        grantFullAccess();
        when(this.auth.hasAccess(this.user, Right.EDIT, FAMILY1_REFERENCE)).thenReturn(false);

        Assert.assertFalse(this.manager.disconnectAll(this.family1));

        Mockito.verifyZeroInteractions(this.qm, this.connectionsQuery, this.xwiki, this.person1, this.family1Doc);
    }

    @Test
    public void disconnectObjectRemovesAllAccessibleConnections() throws XWikiException, QueryException
    {
        grantAccessTo1();

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

        Assert.assertFalse(this.manager.disconnectFromAll(this.person1));

        verify(this.family1Doc).removeXObject(this.connection1Obj);
        verify(this.xwiki).saveDocument(this.family1Doc, REMOVE_COMMENT, true, this.xcontext);
        verify(this.family2Doc, Mockito.never()).removeXObject(this.connection2Obj);
        verify(this.xwiki, Mockito.never()).saveDocument(this.family2Doc, REMOVE_COMMENT, true, this.xcontext);
        verify(this.family3Doc, Mockito.never()).removeXObject(this.connection3Obj);
        verify(this.xwiki, Mockito.never()).saveDocument(this.family3Doc, REMOVE_COMMENT, true, this.xcontext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void disconnectNullObjectThrowsException()
    {
        grantFullAccess();
        this.manager.disconnectFromAll(null);
    }

    @Test
    public void disconnectInaccessibleObjectDoesNothing()
    {
        grantFullAccess();
        when(this.auth.hasAccess(this.user, Right.EDIT, PERSON1_REFERENCE)).thenReturn(false);

        Assert.assertFalse(this.manager.disconnectFromAll(this.person1));

        Mockito.verifyZeroInteractions(this.qm, this.connectionsQuery, this.xwiki, this.family1, this.family1Doc);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getAllConnectionsWithNullSubjectThrowsException()
    {
        grantFullAccess();
        this.manager.getAllConnections(null);
    }

    @Test
    public void getAllConnectionsReturnsAllAccessibleObjects() throws QueryException
    {
        grantAccessExcept2();
        when(this.connectionsQuery.execute()).thenReturn(Arrays.asList("Persons.P01", "Persons.P02", "Persons.P03"));

        Collection<Person> result = this.manager.getAllConnections(this.family1);

        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
        Iterator<Person> it = result.iterator();
        Assert.assertSame(this.person1, it.next());
        Assert.assertSame(this.person3, it.next());
    }

    @Test
    public void getAllConnectionsReturnsEmptyListForInaccessibleSubject()
    {
        grantFullAccess();
        when(this.auth.hasAccess(this.user, Right.VIEW, FAMILY1_REFERENCE)).thenReturn(false);

        Collection<Person> result = this.manager.getAllConnections(this.family1);

        Assert.assertTrue(result.isEmpty());
        Mockito.verifyZeroInteractions(this.qm, this.reverseConnectionsQuery, this.xwiki, this.person1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getAllReverseConnectionsWithNullObjectThrowsException()
    {
        this.manager.getAllReverseConnections(null);
    }

    @Test
    public void getAllReverseConnectionsReturnsAllAccessibleSubjects() throws QueryException
    {
        grantAccessExcept2();
        when(this.reverseConnectionsQuery.execute())
            .thenReturn(Arrays.asList("Families.F01", "Families.F02", "Families.F03"));

        Collection<Family> result = this.manager.getAllReverseConnections(this.person1);

        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
        Iterator<Family> it = result.iterator();
        Assert.assertSame(this.family1, it.next());
        Assert.assertSame(this.family3, it.next());
    }

    @Test
    public void getAllReverseConnectionsReturnsEmptyListForInaccessibleObject()
    {
        grantFullAccess();
        when(this.auth.hasAccess(this.user, Right.VIEW, PERSON1_REFERENCE)).thenReturn(false);

        Collection<Family> result = this.manager.getAllReverseConnections(this.person1);

        Assert.assertTrue(result.isEmpty());
        Mockito.verifyZeroInteractions(this.qm, this.reverseConnectionsQuery, this.xwiki, this.family1Doc,
            this.family1);
    }

    @Test
    public void isConnectedWithNullSubjectReturnsFalse()
    {
        grantFullAccess();
        Assert.assertFalse(this.manager.isConnected(null, this.person1));
    }

    @Test
    public void isConnectedWithNullObjectReturnsFalse()
    {
        grantFullAccess();
        Assert.assertFalse(this.manager.isConnected(this.family1, null));
    }

    @Test
    public void isConnectedWorks()
    {
        grantFullAccess();

        Assert.assertTrue(this.manager.isConnected(this.family1, this.person1));
        Assert.assertFalse(this.manager.isConnected(this.family3, this.person2));
    }

    @Test
    public void isConnectedReturnsFalseWhenAccessDeniedToObject()
    {
        when(this.auth.hasAccess(this.user, Right.VIEW, PERSON1_REFERENCE)).thenReturn(false);
        when(this.auth.hasAccess(this.user, Right.VIEW, FAMILY1_REFERENCE)).thenReturn(true);

        Assert.assertFalse(this.manager.isConnected(this.family1, this.person1));
    }

    @Test
    public void isConnectedReturnsFalseWhenAccessDeniedToSubject()
    {
        when(this.auth.hasAccess(this.user, Right.VIEW, PERSON1_REFERENCE)).thenReturn(true);
        when(this.auth.hasAccess(this.user, Right.VIEW, FAMILY1_REFERENCE)).thenReturn(false);

        Assert.assertFalse(this.manager.isConnected(this.family1, this.person1));
    }

    @Test
    public void getWithSingleConnectionReturnsConnectedPerson()
    {
        grantFullAccess();
        Assert.assertSame(this.person1, this.manager.get(this.family1));
    }

    @Test
    public void getWithAccessToObjectDeniedReturnsNull()
    {
        when(this.auth.hasAccess(this.user, Right.VIEW, PERSON1_REFERENCE)).thenReturn(false);
        when(this.auth.hasAccess(this.user, Right.VIEW, FAMILY1_REFERENCE)).thenReturn(true);
        Assert.assertNull(this.manager.get(this.family1));
    }

    @Test
    public void getWithAccessToSubjectDeniedReturnsNull()
    {
        when(this.auth.hasAccess(this.user, Right.VIEW, PERSON1_REFERENCE)).thenReturn(true);
        when(this.auth.hasAccess(this.user, Right.VIEW, FAMILY1_REFERENCE)).thenReturn(false);
        Assert.assertNull(this.manager.get(this.family1));
    }

    private void grantFullAccess()
    {
        when(this.auth.hasAccess(this.user, Right.VIEW, PERSON1_REFERENCE)).thenReturn(true);
        when(this.auth.hasAccess(this.user, Right.EDIT, PERSON1_REFERENCE)).thenReturn(true);
        when(this.auth.hasAccess(this.user, Right.VIEW, PERSON2_REFERENCE)).thenReturn(true);
        when(this.auth.hasAccess(this.user, Right.EDIT, PERSON2_REFERENCE)).thenReturn(true);
        when(this.auth.hasAccess(this.user, Right.VIEW, PERSON3_REFERENCE)).thenReturn(true);
        when(this.auth.hasAccess(this.user, Right.EDIT, PERSON3_REFERENCE)).thenReturn(true);

        when(this.auth.hasAccess(this.user, Right.VIEW, FAMILY1_REFERENCE)).thenReturn(true);
        when(this.auth.hasAccess(this.user, Right.EDIT, FAMILY1_REFERENCE)).thenReturn(true);
        when(this.auth.hasAccess(this.user, Right.VIEW, FAMILY2_REFERENCE)).thenReturn(true);
        when(this.auth.hasAccess(this.user, Right.EDIT, FAMILY2_REFERENCE)).thenReturn(true);
        when(this.auth.hasAccess(this.user, Right.VIEW, FAMILY3_REFERENCE)).thenReturn(true);
        when(this.auth.hasAccess(this.user, Right.EDIT, FAMILY3_REFERENCE)).thenReturn(true);
    }

    private void grantAccessExcept2()
    {
        when(this.auth.hasAccess(this.user, Right.VIEW, PERSON1_REFERENCE)).thenReturn(true);
        when(this.auth.hasAccess(this.user, Right.EDIT, PERSON1_REFERENCE)).thenReturn(true);
        when(this.auth.hasAccess(this.user, Right.VIEW, PERSON2_REFERENCE)).thenReturn(false);
        when(this.auth.hasAccess(this.user, Right.EDIT, PERSON2_REFERENCE)).thenReturn(false);
        when(this.auth.hasAccess(this.user, Right.VIEW, PERSON3_REFERENCE)).thenReturn(true);
        when(this.auth.hasAccess(this.user, Right.EDIT, PERSON3_REFERENCE)).thenReturn(true);

        when(this.auth.hasAccess(this.user, Right.VIEW, FAMILY1_REFERENCE)).thenReturn(true);
        when(this.auth.hasAccess(this.user, Right.EDIT, FAMILY1_REFERENCE)).thenReturn(true);
        when(this.auth.hasAccess(this.user, Right.VIEW, FAMILY2_REFERENCE)).thenReturn(false);
        when(this.auth.hasAccess(this.user, Right.EDIT, FAMILY2_REFERENCE)).thenReturn(false);
        when(this.auth.hasAccess(this.user, Right.VIEW, FAMILY3_REFERENCE)).thenReturn(true);
        when(this.auth.hasAccess(this.user, Right.EDIT, FAMILY3_REFERENCE)).thenReturn(true);
    }

    private void grantAccessTo1()
    {
        when(this.auth.hasAccess(this.user, Right.VIEW, PERSON1_REFERENCE)).thenReturn(true);
        when(this.auth.hasAccess(this.user, Right.EDIT, PERSON1_REFERENCE)).thenReturn(true);
        when(this.auth.hasAccess(this.user, Right.VIEW, PERSON2_REFERENCE)).thenReturn(true);
        when(this.auth.hasAccess(this.user, Right.EDIT, PERSON2_REFERENCE)).thenReturn(false);
        when(this.auth.hasAccess(this.user, Right.VIEW, PERSON3_REFERENCE)).thenReturn(false);
        when(this.auth.hasAccess(this.user, Right.EDIT, PERSON3_REFERENCE)).thenReturn(false);

        when(this.auth.hasAccess(this.user, Right.VIEW, FAMILY1_REFERENCE)).thenReturn(true);
        when(this.auth.hasAccess(this.user, Right.EDIT, FAMILY1_REFERENCE)).thenReturn(true);
        when(this.auth.hasAccess(this.user, Right.VIEW, FAMILY2_REFERENCE)).thenReturn(true);
        when(this.auth.hasAccess(this.user, Right.EDIT, FAMILY2_REFERENCE)).thenReturn(false);
        when(this.auth.hasAccess(this.user, Right.VIEW, FAMILY3_REFERENCE)).thenReturn(false);
        when(this.auth.hasAccess(this.user, Right.EDIT, FAMILY3_REFERENCE)).thenReturn(false);
    }
}
