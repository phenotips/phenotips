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

import org.phenotips.Constants;
import org.phenotips.entities.PrimaryEntityManager;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Arrays;
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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AbstractPrimaryEntityManager}.
 */
public class AbstractPrimaryEntityManagerTest
{
    private static final DocumentReference PERSON_CLASS_REFERENCE =
        new DocumentReference("main", "PhenoTips", "PersonClass");

    private static final DocumentReference PERSON_REFERENCE = new DocumentReference("main", "Persons", "jdoe");

    private static final String PERSON_ID = "jdoe";

    private static final String PERSON_DOCNAME = "Persons.jdoe";

    private static final String PERSON_NAME = "John Doe";

    @Rule
    public final MockitoComponentMockingRule<PrimaryEntityManager<Person>> mocker =
        new MockitoComponentMockingRule<>(PersonsManager.class);

    private PrimaryEntityManager<Person> manager;

    private DocumentAccessBridge dab;

    private QueryManager qm;

    private DocumentReferenceResolver<String> resolver;

    private DocumentReferenceResolver<EntityReference> referenceResolver;

    private EntityReferenceSerializer<String> localSerializer;

    private DocumentReference currentUser = new DocumentReference("main", "XWiki", "padams");

    @Mock
    private XWiki xwiki;

    @Mock
    private XWikiDocument doc;

    @Mock
    private Query getByNameQuery;

    @Mock
    private Query getLastIdQuery;

    @Mock
    private XWikiContext xcontext;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.localSerializer = this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING, "local");
        when(this.localSerializer.serialize(PERSON_CLASS_REFERENCE)).thenReturn("PhenoTips.PersonClass");

        this.resolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        when(this.resolver.resolve("Person", Constants.CODE_SPACE_REFERENCE))
            .thenReturn(new DocumentReference("main", "PhenoTips", "Person"));
        when(this.resolver.resolve("PersonClass", Constants.CODE_SPACE_REFERENCE)).thenReturn(PERSON_CLASS_REFERENCE);

        this.referenceResolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "current");

        this.dab = this.mocker.getInstance(DocumentAccessBridge.class);
        when(this.dab.exists(PERSON_CLASS_REFERENCE)).thenReturn(true);

        this.manager = this.mocker.getComponentUnderTest();

        when(this.resolver.resolve(PERSON_DOCNAME, this.manager.getDataSpace())).thenReturn(PERSON_REFERENCE);
        when(this.resolver.resolve(PERSON_ID, this.manager.getDataSpace())).thenReturn(PERSON_REFERENCE);

        this.qm = this.mocker.getInstance(QueryManager.class);

        when(this.qm.createQuery(any(String.class), eq(Query.XWQL))).thenReturn(this.getByNameQuery);
        when(this.getByNameQuery.bindValue(any(String.class), any(String.class))).thenReturn(this.getByNameQuery);

        when(this.qm.createQuery(
            "select doc.name from Document doc, doc.object(PhenoTips.PersonClass) as entity "
                + "where doc.space = :space order by doc.name desc",
            Query.XWQL)).thenReturn(this.getLastIdQuery);
        when(this.getLastIdQuery.bindValue("space", "Persons")).thenReturn(this.getLastIdQuery);
        when(this.getLastIdQuery.setLimit(1)).thenReturn(this.getLastIdQuery);
        when(this.getLastIdQuery.execute()).thenReturn(Collections.singletonList("P0000004"));

        Provider<XWikiContext> xcprovider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcprovider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);

        when(this.dab.getCurrentUserReference()).thenReturn(this.currentUser);
    }

    @Test
    public void createWithNoExistingPersonsReturnsPerson00000001() throws Exception
    {
        when(this.getLastIdQuery.execute()).thenReturn(Collections.emptyList());
        DocumentReference ref = new DocumentReference("main", "Persons", "P0000001");
        when(this.referenceResolver
            .resolve(new EntityReference("P0000001", EntityType.DOCUMENT, this.manager.getDataSpace())))
                .thenReturn(ref);
        when(this.dab.getDocument(ref)).thenReturn(this.doc);
        when(this.doc.getDocumentReference()).thenReturn(ref);

        Person result = this.manager.create();

        Assert.assertNotNull(result);
        Assert.assertEquals("P0000001", result.getId());
        Mockito.verify(this.xwiki).saveDocument(this.doc, this.xcontext);
    }

    @Test
    public void createReturnsNextAvailablePerson() throws Exception
    {
        DocumentReference ref = new DocumentReference("main", "Persons", "P0000005");
        when(this.referenceResolver
            .resolve(new EntityReference("P0000005", EntityType.DOCUMENT, this.manager.getDataSpace())))
                .thenReturn(ref);
        when(this.dab.getDocument(ref)).thenReturn(this.doc);
        when(this.doc.getDocumentReference()).thenReturn(ref);

        Person result = this.manager.create();

        Assert.assertNotNull(result);
        Assert.assertEquals("P0000005", result.getId());
        Mockito.verify(this.xwiki).saveDocument(this.doc, this.xcontext);
    }

    @Test
    public void createSkipsExistingDocuments() throws Exception
    {
        DocumentReference ref = new DocumentReference("main", "Persons", "P0000005");
        when(this.referenceResolver
            .resolve(new EntityReference("P0000005", EntityType.DOCUMENT, this.manager.getDataSpace())))
                .thenReturn(ref);
        when(this.dab.exists(ref)).thenReturn(true);
        ref = new DocumentReference("main", "Persons", "P0000006");
        when(this.referenceResolver
            .resolve(new EntityReference("P0000006", EntityType.DOCUMENT, this.manager.getDataSpace())))
                .thenReturn(ref);
        when(this.dab.exists(ref)).thenReturn(false);
        when(this.dab.getDocument(ref)).thenReturn(this.doc);
        when(this.doc.getDocumentReference()).thenReturn(ref);

        Person result = this.manager.create();

        Assert.assertNotNull(result);
        Assert.assertEquals("P0000006", result.getId());
        Mockito.verify(this.xwiki).saveDocument(this.doc, this.xcontext);
    }

    @Test
    public void createSetsCurrentUserAsAuthor() throws Exception
    {
        DocumentReference ref = new DocumentReference("main", "Persons", "P0000005");
        when(this.referenceResolver
            .resolve(new EntityReference("P0000005", EntityType.DOCUMENT, this.manager.getDataSpace())))
                .thenReturn(ref);
        when(this.dab.getDocument(ref)).thenReturn(this.doc);
        when(this.doc.getDocumentReference()).thenReturn(ref);

        this.manager.create();

        Mockito.verify(this.doc).setCreatorReference(this.currentUser);
        Mockito.verify(this.doc).setAuthorReference(this.currentUser);
        Mockito.verify(this.doc).setContentAuthorReference(this.currentUser);
    }

    @Test
    public void createLeavesAuthorEmptyWithGuestUser() throws Exception
    {
        DocumentReference ref = new DocumentReference("main", "Persons", "P0000005");
        when(this.referenceResolver
            .resolve(new EntityReference("P0000005", EntityType.DOCUMENT, this.manager.getDataSpace())))
                .thenReturn(ref);
        when(this.dab.getDocument(ref)).thenReturn(this.doc);
        when(this.doc.getDocumentReference()).thenReturn(ref);

        when(this.dab.getCurrentUserReference()).thenReturn(null);

        this.manager.create();

        Mockito.verify(this.doc, Mockito.never()).setCreatorReference(any());
        Mockito.verify(this.doc, Mockito.never()).setAuthorReference(any());
        Mockito.verify(this.doc, Mockito.never()).setContentAuthorReference(any());
    }

    @Test
    public void createUsesClassTemplateWhenAvailable() throws Exception
    {
        DocumentReference ref = new DocumentReference("main", "Persons", "P0000005");
        when(this.referenceResolver
            .resolve(new EntityReference("P0000005", EntityType.DOCUMENT, this.manager.getDataSpace())))
                .thenReturn(ref);
        when(this.dab.getDocument(ref)).thenReturn(this.doc);
        when(this.doc.getDocumentReference()).thenReturn(ref);

        DocumentReference template = new DocumentReference("main", "PhenoTips", "PersonClassTemplate");
        when(this.dab.exists(template)).thenReturn(true);

        this.manager.create();

        Mockito.verify(this.doc).readFromTemplate(template, this.xcontext);
    }

    @Test
    public void getFromReferenceReturnsAPerson() throws Exception
    {
        when(this.dab.getDocument(PERSON_REFERENCE)).thenReturn(this.doc);
        when(this.doc.getDocumentReference()).thenReturn(PERSON_REFERENCE);

        Person result = this.manager.get(PERSON_REFERENCE);
        Assert.assertNotNull(result);
        Assert.assertEquals(PERSON_REFERENCE, result.getDocumentReference());
    }

    @Test
    public void getFromReferenceReturnsNullWhenDocumentNotLoaded() throws Exception
    {
        when(this.dab.getDocument(PERSON_REFERENCE)).thenReturn(null);

        Person result = this.manager.get(PERSON_REFERENCE);
        Assert.assertNull(result);
    }

    @Test
    public void getFromReferenceReturnsNullWhenDocumentIsNew() throws Exception
    {
        when(this.dab.getDocument(PERSON_REFERENCE)).thenReturn(this.doc);
        when(this.doc.isNew()).thenReturn(true);

        Person result = this.manager.get(PERSON_REFERENCE);
        Assert.assertNull(result);
    }

    @Test
    public void getFromReferenceReturnsNullWhenDABThrowsException() throws Exception
    {
        when(this.dab.getDocument(PERSON_REFERENCE)).thenThrow(new Exception());

        Person result = this.manager.get(PERSON_REFERENCE);
        Assert.assertNull(result);
    }

    @Test
    public void getFromIdReturnsAPerson() throws Exception
    {
        when(this.dab.getDocument(PERSON_REFERENCE)).thenReturn(this.doc);
        when(this.doc.getDocumentReference()).thenReturn(PERSON_REFERENCE);

        Person result = this.manager.get(PERSON_ID);
        Assert.assertNotNull(result);
        Assert.assertEquals(PERSON_REFERENCE, result.getDocumentReference());
    }

    @Test
    public void getFromIdReturnsNullWhenDocumentNotLoaded() throws Exception
    {
        when(this.dab.getDocument(PERSON_REFERENCE)).thenReturn(null);

        Person result = this.manager.get(PERSON_ID);
        Assert.assertNull(result);
    }

    @Test
    public void getFromIdReturnsNullWhenDocumentIsNew() throws Exception
    {
        when(this.dab.getDocument(PERSON_REFERENCE)).thenReturn(this.doc);
        when(this.doc.isNew()).thenReturn(true);

        Person result = this.manager.get(PERSON_ID);
        Assert.assertNull(result);
    }

    @Test
    public void getFromIdReturnsNullWhenDABThrowsException() throws Exception
    {
        when(this.dab.getDocument(PERSON_REFERENCE)).thenThrow(new Exception());

        Person result = this.manager.get(PERSON_ID);
        Assert.assertNull(result);
    }

    @Test
    public void getByNameReturnsAPerson() throws Exception
    {
        when(this.getByNameQuery.execute()).thenReturn(Collections.singletonList(PERSON_DOCNAME));
        when(this.dab.getDocument(PERSON_REFERENCE)).thenReturn(this.doc);
        when(this.doc.getDocumentReference()).thenReturn(PERSON_REFERENCE);

        Person result = this.manager.getByName(PERSON_NAME);
        Assert.assertNotNull(result);
        Assert.assertEquals(PERSON_REFERENCE, result.getDocumentReference());
    }

    @Test
    public void getByNameReturnsNullWhenPersonNotFound() throws Exception
    {
        when(this.getByNameQuery.execute()).thenReturn(Collections.emptyList());

        Person result = this.manager.getByName(PERSON_NAME);
        Assert.assertNull(result);
    }

    @Test
    public void getByNameReturnsNullWhenDocumentNotLoaded() throws Exception
    {
        when(this.getByNameQuery.execute()).thenReturn(Collections.singletonList(PERSON_DOCNAME));
        when(this.dab.getDocument(PERSON_REFERENCE)).thenReturn(null);

        Person result = this.manager.getByName(PERSON_NAME);
        Assert.assertNull(result);
    }

    @Test
    public void getByNameReturnsNullWhenDocumentIsNew() throws Exception
    {
        when(this.getByNameQuery.execute()).thenReturn(Collections.singletonList(PERSON_DOCNAME));
        when(this.dab.getDocument(PERSON_REFERENCE)).thenReturn(this.doc);
        when(this.doc.isNew()).thenReturn(true);

        Person result = this.manager.getByName(PERSON_NAME);
        Assert.assertNull(result);
    }

    @Test
    public void getByNameReturnsNullWhenDABThrowsException() throws Exception
    {
        when(this.getByNameQuery.execute()).thenReturn(Collections.singletonList(PERSON_DOCNAME));
        when(this.dab.getDocument(PERSON_REFERENCE)).thenThrow(new Exception());

        Person result = this.manager.getByName(PERSON_NAME);
        Assert.assertNull(result);
    }

    @Test
    public void getByNameReturnsFirstPerson() throws Exception
    {
        when(this.getByNameQuery.execute()).thenReturn(Arrays.asList(PERSON_DOCNAME, "Persons.johndoe"));
        when(this.dab.getDocument(PERSON_REFERENCE)).thenReturn(this.doc);
        when(this.doc.getDocumentReference()).thenReturn(PERSON_REFERENCE);

        Person result = this.manager.getByName(PERSON_NAME);
        Assert.assertNotNull(result);
        Assert.assertEquals(PERSON_REFERENCE, result.getDocumentReference());

        Mockito.verify(this.resolver, Mockito.never()).resolve("Persons.johndoe", this.manager.getDataSpace());
    }

    @Test
    public void loadReturnsNewPerson()
    {
        Person result = this.manager.load(this.doc);
        Assert.assertNotNull(result);
    }

    @Test
    public void loadWithNullReturnsNull()
    {
        Assert.assertNull(this.manager.load(null));
    }

    @Test
    public void loadWithDMBReturnsNull()
    {
        DocumentModelBridge dmb = Mockito.mock(DocumentModelBridge.class);
        Assert.assertNull(this.manager.load(dmb));
    }

    @Test
    public void deleteDeletesTheDocument() throws XWikiException
    {
        Assert.assertTrue(this.manager.delete(new Person(this.doc)));
        Mockito.verify(this.xwiki).deleteDocument(this.doc, this.xcontext);
    }

    @Test
    public void deleteReturnsFalseOnException() throws XWikiException
    {
        Mockito.doThrow(new XWikiException()).when(this.xwiki).deleteDocument(this.doc, this.xcontext);
        Assert.assertFalse(this.manager.delete(new Person(this.doc)));
    }

    @Test
    public void getIdPrefixIsP()
    {
        Assert.assertEquals("P", this.manager.getIdPrefix());
    }

    @Test
    public void getEntityTypeReturnsPersonClass()
    {
        EntityReference result = this.manager.getEntityType();
        Assert.assertEquals("PersonClass", result.getName());
        Assert.assertEquals("PhenoTips", result.getParent().getName());
        Assert.assertNull(result.getParent().getParent());
    }
}
