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
import java.util.Collections;

import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AbstractOutgoingPrimaryEntityConnectionsManager} when used as a property.
 */
public class PropertyPrimaryEntityConnectionsManagerTest
{
    private static final DocumentReference PERSON1_REFERENCE = new DocumentReference("main", "Persons", "P01");

    private static final DocumentReference JOB1_REFERENCE = new DocumentReference("main", "Jobs", "J01");

    private static final DocumentReference JOB2_REFERENCE = new DocumentReference("main", "Jobs", "J02");

    private static final String JOB1_SERIALIZED_REFERENCE = "main:Jobs.J01";

    private static final String JOB2_SERIALIZED_REFERENCE = "main:Jobs.J02";

    private static final String REMOVE_COMMENT = "Removed connection to main:Jobs.J01";

    private static final String ADD_COMMENT = "Added connection to main:Jobs.J01";

    @Rule
    public final MockitoComponentMockingRule<PrimaryEntityConnectionsManager<Person, Job>> mocker =
        new MockitoComponentMockingRule<>(PersonHasJobConnectionsManager.class);

    private PrimaryEntityConnectionsManager<Person, Job> manager;

    private PrimaryEntityManager<Person> persons;

    private PrimaryEntityManager<Job> jobs;

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
    private Job job1;

    @Mock
    private Job job2;

    @Mock
    private XWikiDocument person1Doc;

    @Mock
    private BaseObject person1Obj;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.manager = this.mocker.getComponentUnderTest();

        this.persons = this.mocker.getInstance(PersonsManager.TYPE, "Person");
        this.jobs = this.mocker.getInstance(JobsManager.TYPE, "Job");
        when(this.jobs.getEntityType()).thenReturn(Job.CLASS_REFERENCE);

        when(this.person1.getDocumentReference()).thenReturn(PERSON1_REFERENCE);
        when(this.person1.getXDocument()).thenReturn(this.person1Doc);
        when(this.person1.getDocumentReference()).thenReturn(PERSON1_REFERENCE);
        when(this.person1Doc.getXObject(Person.CLASS_REFERENCE, true, this.xcontext)).thenReturn(this.person1Obj);
        when(this.person1Doc.getXObject(Person.CLASS_REFERENCE, PersonHasJobConnectionsManager.REFERENCE_PROPERTY,
            JOB2_SERIALIZED_REFERENCE, false)).thenReturn(this.person1Obj);

        when(this.job1.getDocumentReference()).thenReturn(JOB1_REFERENCE);
        when(this.job2.getDocumentReference()).thenReturn(JOB2_REFERENCE);

        Provider<XWikiContext> xcprovider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcprovider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);

        EntityReferenceSerializer<String> fullSerializer =
            this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);
        when(fullSerializer.serialize(JOB1_REFERENCE)).thenReturn(JOB1_SERIALIZED_REFERENCE);
        when(fullSerializer.serialize(JOB2_REFERENCE)).thenReturn(JOB2_SERIALIZED_REFERENCE);

        EntityReferenceSerializer<String> localSerializer =
            this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING, "local");
        when(localSerializer.serialize(PERSON1_REFERENCE)).thenReturn("Persons.P01");
        when(localSerializer.serialize(Person.CLASS_REFERENCE)).thenReturn("PhenoTips.PersonClass");
        when(localSerializer.serialize(Job.CLASS_REFERENCE)).thenReturn("PhenoTips.Job");

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
        when(this.connectionsQuery.execute()).thenReturn(Collections.singletonList("Jobs.J01"));

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
        when(this.reverseConnectionsQuery.execute()).thenReturn(Collections.singletonList("Persons.P01"));

        when(this.persons.get("Persons.P01")).thenReturn(this.person1);
        when(this.jobs.get("Jobs.J01")).thenReturn(this.job1);
        when(this.jobs.get("Jobs.J02")).thenReturn(this.job2);
    }

    @Test
    public void getReturnsConnectedJob()
    {
        Assert.assertSame(this.job1, this.manager.get(this.person1));

        verify(this.connectionsQuery).bindValue("referenceProperty", PersonHasJobConnectionsManager.REFERENCE_PROPERTY);
        verify(this.connectionsQuery).bindValue("subjectDocument", "Persons.P01");
        verify(this.connectionsQuery).bindValue("connectionClass", "PhenoTips.PersonClass");
        verify(this.connectionsQuery).bindValue("wikiId", "null:");
        verify(this.connectionsQuery).bindValue("objectClass", "PhenoTips.Job");
    }

    @Test
    public void getWithNoValueSetReturnsNull() throws QueryException
    {
        when(this.connectionsQuery.execute()).thenReturn(Collections.emptyList());
        Assert.assertNull(this.manager.get(this.person1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getWithNullSubjectThrowsException()
    {
        this.manager.get(null);
    }

    @Test
    public void setConnectsNewObject() throws QueryException, XWikiException
    {
        when(this.connectionsQuery.execute()).thenReturn(Arrays.asList("Jobs.J02"));

        Assert.assertTrue(this.manager.set(this.person1, this.job1));

        verify(this.person1Doc, Mockito.never()).removeXObject(Matchers.any());
        verify(this.person1Obj).setStringValue(PersonHasJobConnectionsManager.REFERENCE_PROPERTY,
            JOB1_SERIALIZED_REFERENCE);
        verify(this.xwiki).saveDocument(this.person1Doc, ADD_COMMENT, true, this.xcontext);
    }

    @Test
    public void setDeletesPreviousValueButStopsOnFailure() throws QueryException, XWikiException
    {
        when(this.connectionsQuery.execute()).thenReturn(Arrays.asList("Jobs.J02"));

        Mockito.doThrow(new XWikiException()).when(this.xwiki).saveDocument(this.person1Doc,
            "Removed connection to main:Jobs.J02", true, this.xcontext);

        Assert.assertFalse(this.manager.set(this.person1, this.job1));

        verify(this.xwiki, Mockito.never()).saveDocument(this.person1Doc, ADD_COMMENT, true,
            this.xcontext);
        verify(this.person1Doc, Mockito.never()).removeXObject(Matchers.any());
        verify(this.person1Obj).setStringValue(PersonHasJobConnectionsManager.REFERENCE_PROPERTY, "");
    }

    @Test
    public void setToNullDeletesPreviousValue() throws QueryException, XWikiException
    {
        when(this.connectionsQuery.execute()).thenReturn(Arrays.asList("Jobs.J02"));

        Assert.assertTrue(this.manager.set(this.person1, null));

        verify(this.person1Doc, Mockito.never()).removeXObject(Matchers.any());
        verify(this.person1Obj).setStringValue(PersonHasJobConnectionsManager.REFERENCE_PROPERTY, "");
        verify(this.xwiki).saveDocument(this.person1Doc, "Removed connection to main:Jobs.J02", true, this.xcontext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setWithNullSubjectThrowsException()
    {
        this.manager.set(null, this.job1);
    }

    @Test
    public void removeDeletesPreviousValue() throws QueryException, XWikiException
    {
        when(this.connectionsQuery.execute()).thenReturn(Arrays.asList("Jobs.J02"));

        Assert.assertTrue(this.manager.remove(this.person1));

        verify(this.person1Doc, Mockito.never()).removeXObject(Matchers.any());
        verify(this.person1Obj).setStringValue(PersonHasJobConnectionsManager.REFERENCE_PROPERTY, "");
        verify(this.xwiki).saveDocument(this.person1Doc, "Removed connection to main:Jobs.J02", true, this.xcontext);
    }

    @Test
    public void removeReturnsFalseOnFailure() throws QueryException, XWikiException
    {
        when(this.connectionsQuery.execute()).thenReturn(Arrays.asList("Jobs.J02"));

        Mockito.doThrow(new XWikiException()).when(this.xwiki).saveDocument(this.person1Doc,
            "Removed connection to main:Jobs.J02", true, this.xcontext);

        Assert.assertFalse(this.manager.remove(this.person1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeWithNullSubjectThrowsException()
    {
        this.manager.remove(null);
    }
}
