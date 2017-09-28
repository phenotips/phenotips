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
package org.phenotips.data.permissions.rest.internal;

import org.phenotips.Constants;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link NameAndEmailExtractor} class.
 */
public class NameAndEmailExtractorTest
{
    private static final String USER = "user";

    private static final String GROUP = "group";

    private static final String EMAIL = "email";

    private static final String FIRST_NAME = "first_name";

    private static final String LAST_NAME = "last_name";

    private static final String CONTACT = "contact";

    private static final EntityReference USER_OBJECT_REFERENCE =
        new EntityReference("XWikiUsers", EntityType.DOCUMENT, Constants.XWIKI_SPACE_REFERENCE);

    private static final EntityReference GROUP_OBJECT_REFERENCE =
        new EntityReference("PhenoTipsGroupClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "data", GROUP);
    
    @Rule
    public MockitoComponentMockingRule<NameAndEmailExtractor> mocker =
        new MockitoComponentMockingRule<>(NameAndEmailExtractor.class);

    @Mock
    private EntityReference entityReference;

    @Mock
    private XWikiDocument xDocument;

    @Mock
    private BaseObject entityObj;

    private DocumentAccessBridge documentAccessBridge;

    private DocumentReferenceResolver<EntityReference> referenceResolver;

    private Logger logger;

    private NameAndEmailExtractor component;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        this.component = this.mocker.getComponentUnderTest();
        this.logger = this.mocker.getMockedLogger();
        this.documentAccessBridge = this.mocker.getInstance(DocumentAccessBridge.class);
        this.referenceResolver = this.mocker.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "current");

        when(this.referenceResolver.resolve(this.entityReference)).thenReturn(DOCUMENT_REFERENCE);
        when(this.documentAccessBridge.getDocument(DOCUMENT_REFERENCE)).thenReturn(this.xDocument);
        when(this.xDocument.getXObject(USER_OBJECT_REFERENCE)).thenReturn(this.entityObj);
        when(this.xDocument.getXObject(GROUP_OBJECT_REFERENCE)).thenReturn(this.entityObj);

        when(this.xDocument.getDocumentReference()).thenReturn(DOCUMENT_REFERENCE);
        when(this.entityObj.getStringValue(EMAIL)).thenReturn(EMAIL);
        when(this.entityObj.getStringValue(FIRST_NAME)).thenReturn(FIRST_NAME);
        when(this.entityObj.getStringValue(LAST_NAME)).thenReturn(LAST_NAME);
        when(this.entityObj.getStringValue(CONTACT)).thenReturn(CONTACT);
    }

    @Test
    public void getNameAndEmailWithBlankType()
    {
        Assert.assertNull(this.component.getNameAndEmail(null, this.entityReference));
        Assert.assertNull(this.component.getNameAndEmail(StringUtils.EMPTY, this.entityReference));
        Assert.assertNull(this.component.getNameAndEmail(StringUtils.SPACE, this.entityReference));
    }

    @Test
    public void getNameAndEmailWithWrongType()
    {
        Assert.assertNull(this.component.getNameAndEmail("wrong", this.entityReference));
    }

    @Test
    public void getNameAndEmailWithNullReference() throws Exception
    {
        when(this.referenceResolver.resolve(null)).thenReturn(null);
        when(this.documentAccessBridge.getDocument((DocumentReference) null)).thenReturn(null);
        Assert.assertNull(this.component.getNameAndEmail(USER, null));
        verify(this.logger).error("Could not load user's or group's document", (Object) null);
    }

    @Test
    public void getNameAndEmailReferenceCannotBeResolved() throws Exception
    {
        when(this.referenceResolver.resolve(this.entityReference)).thenReturn(null);
        when(this.documentAccessBridge.getDocument((DocumentReference) null)).thenReturn(null);
        Assert.assertNull(this.component.getNameAndEmail(USER, this.entityReference));
        verify(this.logger).error("Could not load user's or group's document", (Object) null);
    }

    @Test
    public void getNameAndEmailEntityDocumentCannotBeRetrieved() throws Exception
    {
        when(this.documentAccessBridge.getDocument(DOCUMENT_REFERENCE)).thenReturn(null);
        Assert.assertNull(this.component.getNameAndEmail(USER, this.entityReference));
        verify(this.logger).error("Could not load user's or group's document", (Object) null);
    }

    @Test
    public void getNameAndEmailCannotGetUserObject()
    {
        when(this.xDocument.getXObject(USER_OBJECT_REFERENCE)).thenReturn(null);
        Assert.assertNull(this.component.getNameAndEmail(USER, this.entityReference));
        verify(this.logger).error("Could not load user's or group's document", (Object) null);
    }

    @Test
    public void getNameAndEmailCannotGetGroupObject()
    {
        when(this.xDocument.getXObject(GROUP_OBJECT_REFERENCE)).thenReturn(null);
        Assert.assertNull(this.component.getNameAndEmail(GROUP, this.entityReference));
        verify(this.logger).error("Could not load user's or group's document", (Object) null);
    }

    @Test
    public void getNameAndEmailForUser() {
        final Pair<String, String> pair = Pair.of(FIRST_NAME + StringUtils.SPACE + LAST_NAME, EMAIL);
        final Pair<String, String> result = this.component.getNameAndEmail(USER, this.entityReference);
        Assert.assertEquals(pair, result);
    }

    @Test
    public void getNameAndEmailEmailStringValueIsEmptyForUser()
    {
        when(this.entityObj.getStringValue(EMAIL)).thenReturn(StringUtils.EMPTY);
        final Pair<String, String> pair = Pair.of(FIRST_NAME + StringUtils.SPACE + LAST_NAME, StringUtils.EMPTY);
        final Pair<String, String> result = this.component.getNameAndEmail(USER, this.entityReference);
        Assert.assertEquals(pair, result);
    }

    @Test
    public void getNameAndEmailNameStringsValuesEmptyForUser()
    {
        when(this.entityObj.getStringValue(FIRST_NAME)).thenReturn(StringUtils.EMPTY);
        when(this.entityObj.getStringValue(LAST_NAME)).thenReturn(StringUtils.EMPTY);
        final Pair<String, String> pair = Pair.of(StringUtils.EMPTY, EMAIL);
        final Pair<String, String> result = this.component.getNameAndEmail(USER, this.entityReference);
        Assert.assertEquals(pair, result);
    }

    @Test
    public void getNameAndEmailForGroup() {
        final Pair<String, String> pair = Pair.of(GROUP, CONTACT);
        final Pair<String, String> result = this.component.getNameAndEmail(GROUP, this.entityReference);
        Assert.assertEquals(pair, result);
    }

    @Test
    public void getNameAndEmailContactStringValueIsEmptyForGroup()
    {
        when(this.entityObj.getStringValue(CONTACT)).thenReturn(StringUtils.EMPTY);
        final Pair<String, String> pair = Pair.of(GROUP, StringUtils.EMPTY);
        final Pair<String, String> result = this.component.getNameAndEmail(GROUP, this.entityReference);
        Assert.assertEquals(pair, result);
    }

    @Test
    public void getNameAndEmailNameValueIsBlankForGroup()
    {
        final DocumentReference newRef = new DocumentReference("xwiki", "data", StringUtils.SPACE);
        when(this.xDocument.getDocumentReference()).thenReturn(newRef);
        final Pair<String, String> pair = Pair.of(StringUtils.SPACE, CONTACT);
        final Pair<String, String> result = this.component.getNameAndEmail(GROUP, this.entityReference);
        Assert.assertEquals(pair, result);
    }

    @Test
    public void getNameAndEmailWithException()
    {
        when(this.xDocument.getXObject(GROUP_OBJECT_REFERENCE)).thenThrow(new RuntimeException());
        Assert.assertNull(this.component.getNameAndEmail(GROUP, this.entityReference));
        verify(this.logger).error("Could not load user's or group's document", (Object) null);
    }
}
