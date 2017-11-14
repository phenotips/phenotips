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
package org.phenotips.data.permissions.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.Collaborator;
import org.phenotips.data.permissions.Owner;
import org.phenotips.data.permissions.internal.access.EditAccessLevel;
import org.phenotips.data.permissions.internal.access.ManageAccessLevel;
import org.phenotips.data.permissions.internal.access.NoAccessLevel;
import org.phenotips.data.permissions.internal.access.OwnerAccessLevel;
import org.phenotips.data.permissions.internal.access.ViewAccessLevel;
import org.phenotips.entities.PrimaryEntity;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
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
import com.xpn.xwiki.user.api.XWikiGroupService;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the default {@link EntityAccessManager} implementation, {@link DefaultEntityAccessManager}.
 *
 * @version $Id$
 */
public class DefaultEntityAccessManagerTest
{
    private static final String WIKI_NAME = "xwiki";

    private static final String DATA = "data";

    private static final String SPACE_NAME = "Xwiki";

    private static final String PHENOTIPS = "PhenoTips";

    private static final String OWNER_NAME = "padams";

    private static final String COLLABORATOR_NAME = "hmccoy";

    private static final String OTHER_USER_NAME = "cxavier";

    private static final String OWNER_TITLE = "Owner";

    private static final String VISIBILITY_TITLE = "Visibility";

    private static final String COLLABORATOR_TITLE = "Collaborator";

    private static final String COLLABORATORS_LABEL = "collaborators";

    private static final String COLLABORATOR_LABEL = "collaborator";

    private static final String ACCESS_LABEL = "access";

    private static final String OWNER_LABEL = "owner";

    private static final String EDIT_LABEL = "edit";

    private static final String VIEW_LABEL = "view";

    private static final String UNKNOWN_LABEL = "unknown";

    private static final String MANAGE_LABEL = "manage";

    private static final String NONE_LABEL = "none";

    private static final String OWNER_STR = "xwiki:XWiki.padams";

    private static final String COLLABORATOR_STR = "xwiki:XWiki.hmccoy";

    private static final String OTHER_USER_STR = "xwiki:XWiki.cxavier";

    private static final String GROUP_STR = "xwiki:XWiki.collaborators";

    private static final String PATIENT_ID = "P0000001";

    private static final AccessLevel EDIT_ACCESS = new EditAccessLevel();

    private static final AccessLevel VIEW_ACCESS = new ViewAccessLevel();

    private static final AccessLevel MANAGE_ACCESS = new ManageAccessLevel();

    private static final AccessLevel OWNER_ACCESS = new OwnerAccessLevel();

    private static final AccessLevel NO_ACCESS = new NoAccessLevel();

    /** The patient used for tests. */
    private static final DocumentReference PATIENT_REFERENCE = new DocumentReference(WIKI_NAME, DATA, PATIENT_ID);

    /** The user used as the owner of the patient. */
    private static final DocumentReference OWNER = new DocumentReference(WIKI_NAME, SPACE_NAME, OWNER_NAME);

    /** The user used as a collaborator. */
    private static final DocumentReference COLLABORATOR = new DocumentReference(WIKI_NAME, SPACE_NAME,
        COLLABORATOR_NAME);

    /** The user used as a non-collaborator. */
    private static final DocumentReference OTHER_USER = new DocumentReference(WIKI_NAME, SPACE_NAME, OTHER_USER_NAME);

    /** Group used as collaborator. */
    private static final DocumentReference GROUP = new DocumentReference(WIKI_NAME, SPACE_NAME, COLLABORATORS_LABEL);

    private static final DocumentReference OWNER_CLASS = new DocumentReference(WIKI_NAME, PHENOTIPS, OWNER_TITLE);

    private static final DocumentReference VISIBILITY_CLASS = new DocumentReference(WIKI_NAME, PHENOTIPS,
        VISIBILITY_TITLE);

    private static final DocumentReference COLLABORATOR_CLASS = new DocumentReference(WIKI_NAME, PHENOTIPS,
        COLLABORATOR_TITLE);

    @Rule
    public MockitoComponentMockingRule<EntityAccessManager> mocker =
        new MockitoComponentMockingRule<>(DefaultEntityAccessManager.class);

    @Mock
    private Patient entity;

    @Mock
    private XWikiDocument entityDoc;

    @Mock
    private BaseObject ownerObject;

    @Mock
    private XWikiGroupService groupService;

    @Mock
    private BaseObject visibilityObject;

    @Mock
    private BaseObject collaboratorObject1;

    @Mock
    private BaseObject collaboratorObject2;

    @Mock
    private BaseObject collaboratorObject3;

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki xwiki;

    private ParameterizedType entityResolverType = new DefaultParameterizedType(null, DocumentReferenceResolver.class,
        EntityReference.class);

    private ParameterizedType stringResolverType = new DefaultParameterizedType(null, DocumentReferenceResolver.class,
        String.class);

    private ParameterizedType stringSerializerType = new DefaultParameterizedType(null,
        EntityReferenceSerializer.class, String.class);

    private DocumentReferenceResolver<EntityReference> partialEntityResolver;

    private ComponentManager componentManager;

    private EntityAccessManager component;

    private AuthorizationManager rights;

    private EntityAccessHelper helper;

    private AccessLevel noAccess;

    private Logger logger;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        final Provider<XWikiContext> xcontextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(this.context);
        when(this.context.getWiki()).thenReturn(this.xwiki);

        this.partialEntityResolver = this.mocker.getInstance(this.entityResolverType, "currentmixed");
        this.componentManager = this.mocker.getInstance(ComponentManager.class, "context");
        this.noAccess = this.mocker.getInstance(AccessLevel.class, NONE_LABEL);
        this.rights = this.mocker.getInstance(AuthorizationManager.class);
        this.helper = this.mocker.getInstance(EntityAccessHelper.class);

        this.component = this.mocker.getComponentUnderTest();
        this.logger = this.mocker.getMockedLogger();

        when(this.entity.getDocumentReference()).thenReturn(PATIENT_REFERENCE);
        when(this.entity.getXDocument()).thenReturn(this.entityDoc);
        when(this.entityDoc.getXObject(OWNER_CLASS)).thenReturn(this.ownerObject);
        when(this.entityDoc.getXObject(OWNER_CLASS, true, this.context)).thenReturn(this.ownerObject);
        when(this.ownerObject.getStringValue(OWNER_LABEL)).thenReturn(OWNER_STR);
        when(this.entityDoc.getXObject(VISIBILITY_CLASS)).thenReturn(this.visibilityObject);
        when(this.entityDoc.getXObject(VISIBILITY_CLASS, true, this.context)).thenReturn(this.visibilityObject);

        when(this.partialEntityResolver.resolve(Owner.CLASS_REFERENCE, PATIENT_REFERENCE)).thenReturn(
            OWNER_CLASS);
        when(this.helper.getStringProperty(this.entity.getXDocument(), OWNER_CLASS, OWNER_LABEL)).thenReturn(OWNER_STR);

        final DocumentReferenceResolver<String> stringEntityResolver =
            this.mocker.getInstance(this.stringResolverType, "currentmixed");
        when(stringEntityResolver.resolve(OWNER_STR, PATIENT_REFERENCE)).thenReturn(OWNER);
        when(this.partialEntityResolver.resolve(OWNER)).thenReturn(OWNER);

        final EntityReferenceSerializer<String> stringEntitySerializer =
            this.mocker.getInstance(this.stringSerializerType);
        when(stringEntitySerializer.serialize(OWNER)).thenReturn(OWNER_STR);

        when(this.componentManager.getInstance(AccessLevel.class, EDIT_LABEL)).thenReturn(EDIT_ACCESS);
        when(this.componentManager.getInstance(AccessLevel.class, VIEW_LABEL)).thenReturn(VIEW_ACCESS);
        when(this.componentManager.getInstance(AccessLevel.class, MANAGE_LABEL)).thenReturn(MANAGE_ACCESS);
        when(this.componentManager.getInstance(AccessLevel.class, OWNER_LABEL)).thenReturn(OWNER_ACCESS);
        when(this.componentManager.getInstance(AccessLevel.class, NONE_LABEL)).thenReturn(NO_ACCESS);

        when(this.partialEntityResolver.resolve(Collaborator.CLASS_REFERENCE, PATIENT_REFERENCE)).thenReturn(
            COLLABORATOR_CLASS);
        when(stringEntityResolver.resolve(OWNER_STR)).thenReturn(OWNER);
        when(stringEntityResolver.resolve(COLLABORATOR_STR, PATIENT_REFERENCE)).thenReturn(COLLABORATOR);
        when(stringEntityResolver.resolve(OTHER_USER_STR, PATIENT_REFERENCE)).thenReturn(OTHER_USER);
        when(stringEntityResolver.resolve(GROUP_STR, PATIENT_REFERENCE)).thenReturn(GROUP);

        when(this.partialEntityResolver.resolve(COLLABORATOR)).thenReturn(COLLABORATOR);

        when(stringEntitySerializer.serialize(COLLABORATOR)).thenReturn(COLLABORATOR_STR);
        when(stringEntitySerializer.serialize(OTHER_USER)).thenReturn(OTHER_USER_STR);
    }

    /** Basic tests for {@link EntityAccessManager#getOwner(PrimaryEntity)}. */
    @Test
    public void getOwner()
    {
        final Owner owner = this.component.getOwner(this.entity);
        Assert.assertNotNull(owner);
        Assert.assertSame(OWNER, owner.getUser());
    }

    /** {@link EntityAccessManager#getOwner(PrimaryEntity)} returns a null user when the owner isn't specified. */
    @Test
    public void getOwnerWithMissingOwnerAndReferrer()
    {
        when(this.helper.getStringProperty(this.entity.getXDocument(), OWNER_CLASS, OWNER_LABEL)).thenReturn(null);
        when(this.ownerObject.getStringValue(OWNER_LABEL)).thenReturn(null);
        Owner owner = this.component.getOwner(this.entity);
        Assert.assertNotNull(owner);
        Assert.assertNull(owner.getUser());

        when(this.helper.getStringProperty(this.entity.getXDocument(), OWNER_CLASS, OWNER_LABEL))
            .thenReturn(StringUtils.EMPTY);
        owner = this.component.getOwner(this.entity);
        Assert.assertNotNull(owner);
        Assert.assertNull(owner.getUser());

        verify(this.entity, never()).getReporter();
    }

    /** {@link EntityAccessManager#getOwner(PrimaryEntity)} returns {@code null} when the patient is missing. */
    @Test
    public void getOwnerWithMissingPatient()
    {
        Assert.assertNull(this.component.getOwner(null));
    }

    /** {@link EntityAccessManager#getOwner(PrimaryEntity)} returns {@code null} when the patient is missing. */
    @Test
    public void getOwnerWithMissingDocument()
    {
        when(this.entity.getDocumentReference()).thenReturn(null);
        Assert.assertNull(this.component.getOwner(this.entity));
    }

    @Test
    public void setOwnerUserIsNullRemovesOldOwner() throws XWikiException
    {
        final boolean ownerSet = this.component.setOwner(this.entity, null);

        Assert.assertTrue(ownerSet);
        verify(this.helper, times(1)).setProperty(this.entityDoc, OWNER_CLASS, OWNER_LABEL, StringUtils.EMPTY);
        verify(this.xwiki, times(1)).saveDocument(this.entityDoc, "Set owner: " + StringUtils.EMPTY, true,
            this.context);
    }

    @Test
    public void setOwnerEntityDocReferenceIsNullReturnsFalse() throws XWikiException
    {
        when(this.entity.getDocumentReference()).thenReturn(null);

        final boolean ownerSet = this.component.setOwner(this.entity, COLLABORATOR);
        Assert.assertFalse(ownerSet);
        verify(this.helper, never()).setProperty(any(), any(), anyString(), anyString());
        verify(this.xwiki, never()).saveDocument(any(), anyString(), anyBoolean(), any());
    }

    @Test
    public void setOwnerClassReferenceCannotBeDeterminedReturnsFalse() throws XWikiException
    {
        when(this.partialEntityResolver.resolve(Owner.CLASS_REFERENCE, PATIENT_REFERENCE)).thenReturn(null);

        final boolean ownerSet = this.component.setOwner(this.entity, COLLABORATOR);
        Assert.assertFalse(ownerSet);
        verify(this.helper, never()).setProperty(any(), any(), anyString(), anyString());
        verify(this.xwiki, never()).saveDocument(any(), anyString(), anyBoolean(), any());
    }

    @Test
    public void setOwnerPreviousOwnerIsNullNoCollaboratorIsAddedAndOneIsDeleted() throws XWikiException
    {
        when(this.helper.getStringProperty(this.entityDoc, OWNER, OWNER_LABEL)).thenReturn(null);

        when(this.entityDoc.getXObject(COLLABORATOR_CLASS, COLLABORATOR_LABEL, COLLABORATOR_STR, false))
            .thenReturn(this.collaboratorObject1);

        final boolean ownerSet = this.component.setOwner(this.entity, COLLABORATOR);

        verify(this.entityDoc, times(1)).getXObject(COLLABORATOR_CLASS, COLLABORATOR_LABEL, COLLABORATOR_STR, false);
        verify(this.entityDoc, times(1)).removeXObject(this.collaboratorObject1);
        verify(this.entityDoc, never()).newXObject(eq(COLLABORATOR), eq(this.context));
        verify(this.xwiki, times(1)).saveDocument(this.entityDoc, "Set owner: " + COLLABORATOR_STR, true,
            this.context);
        Assert.assertTrue(ownerSet);
    }

    @Test
    public void setOwnerPreviousOwnerIsNotNullOneCollaboratorIsAddedAndOneDeleted() throws XWikiException
    {
        when(this.entityDoc.getXObject(COLLABORATOR_CLASS, COLLABORATOR_LABEL, COLLABORATOR_STR, false))
            .thenReturn(this.collaboratorObject1);

        final boolean ownerSet = this.component.setOwner(this.entity, COLLABORATOR);

        verify(this.entityDoc, times(1)).getXObject(COLLABORATOR_CLASS, COLLABORATOR_LABEL, COLLABORATOR_STR, false);
        verify(this.entityDoc, times(1)).getXObject(COLLABORATOR_CLASS, COLLABORATOR_LABEL, OWNER_STR, false);
        verify(this.entityDoc, times(1)).removeXObject(this.collaboratorObject1);
        verify(this.entityDoc, times(1)).newXObject(COLLABORATOR_CLASS, this.context);
        verify(this.xwiki, times(1)).saveDocument(this.entityDoc, "Set owner: " + COLLABORATOR_STR, true,
            this.context);
        Assert.assertTrue(ownerSet);
    }

    /** Basic tests for {@link EntityAccessManager#setOwner(PrimaryEntity, EntityReference)}. */
    @Test
    public void setOwner() throws XWikiException
    {
        Assert.assertTrue(this.component.setOwner(this.entity, OWNER));
        verify(this.helper).setProperty(this.entityDoc, OWNER_CLASS, OWNER_LABEL, OWNER_STR);
        verify(this.xwiki).saveDocument(this.entityDoc, "Set owner: " + OWNER_STR, true, this.context);
    }

    /** Basic tests for {@link EntityAccessManager#setOwner(PrimaryEntity, EntityReference)}. */
    @Test
    public void setOwnerWithFailure()
    {
        doThrow(new RuntimeException()).when(this.entity).getXDocument();
        Assert.assertFalse(this.component.setOwner(this.entity, OWNER));
    }

    /** Basic tests for {@link EntityAccessManager#getCollaborators(PrimaryEntity)}. */
    @Test
    public void getCollaborators()
    {
        List<BaseObject> objects = new ArrayList<>();
        when(this.collaboratorObject1.getStringValue(COLLABORATOR_LABEL)).thenReturn(COLLABORATOR_STR);
        when(this.collaboratorObject1.getStringValue(ACCESS_LABEL)).thenReturn(EDIT_LABEL);
        objects.add(this.collaboratorObject1);
        when(this.collaboratorObject2.getStringValue(COLLABORATOR_LABEL)).thenReturn(OTHER_USER_STR);
        when(this.collaboratorObject2.getStringValue(ACCESS_LABEL)).thenReturn(VIEW_LABEL);
        objects.add(this.collaboratorObject2);
        when(this.entityDoc.getXObjects(COLLABORATOR_CLASS)).thenReturn(objects);

        Collection<Collaborator> collaborators = this.component.getCollaborators(this.entity);
        Assert.assertEquals(2, collaborators.size());
        Collaborator c = new DefaultCollaborator(COLLABORATOR, EDIT_ACCESS, this.helper);
        Assert.assertTrue(collaborators.contains(c));
        c = new DefaultCollaborator(OTHER_USER, VIEW_ACCESS, this.helper);
        Assert.assertTrue(collaborators.contains(c));
    }

    /**
     * {@link EntityAccessManager#getCollaborators(PrimaryEntity)} returns the most permissive access level when
     * multiple entries are present.
     */
    @Test
    public void getCollaboratorsWithMultipleEntries()
    {
        List<BaseObject> objects = new ArrayList<>();
        when(this.collaboratorObject1.getStringValue(COLLABORATOR_LABEL)).thenReturn(COLLABORATOR_STR);
        when(this.collaboratorObject1.getStringValue(ACCESS_LABEL)).thenReturn(EDIT_LABEL);
        objects.add(this.collaboratorObject1);
        when(this.collaboratorObject2.getStringValue(COLLABORATOR_LABEL)).thenReturn(COLLABORATOR_STR);
        when(this.collaboratorObject2.getStringValue(ACCESS_LABEL)).thenReturn(VIEW_LABEL);
        objects.add(this.collaboratorObject2);
        when(this.collaboratorObject3.getStringValue(COLLABORATOR_LABEL)).thenReturn(COLLABORATOR_STR);
        when(this.collaboratorObject3.getStringValue(ACCESS_LABEL)).thenReturn(MANAGE_LABEL);
        objects.add(this.collaboratorObject3);
        when(this.entityDoc.getXObjects(COLLABORATOR_CLASS)).thenReturn(objects);

        Collection<Collaborator> collaborators = this.component.getCollaborators(this.entity);
        Assert.assertEquals(1, collaborators.size());
        Collaborator c = new DefaultCollaborator(COLLABORATOR, MANAGE_ACCESS, this.helper);
        Assert.assertTrue(collaborators.contains(c));
    }

    /**
     * {@link EntityAccessManager#getCollaborators(PrimaryEntity)} filters out collaborators with null access.
     */
    @Test
    public void getCollaboratorsCollaboratorsWithNullAccessAreFilteredOut()
    {
        List<BaseObject> objects = new ArrayList<>();
        when(this.collaboratorObject1.getStringValue(COLLABORATOR_LABEL)).thenReturn(COLLABORATOR_STR);
        when(this.collaboratorObject1.getStringValue(ACCESS_LABEL)).thenReturn(null);
        objects.add(this.collaboratorObject1);
        when(this.collaboratorObject2.getStringValue(COLLABORATOR_LABEL)).thenReturn(OTHER_USER_STR);
        when(this.collaboratorObject2.getStringValue(ACCESS_LABEL)).thenReturn(VIEW_LABEL);
        objects.add(this.collaboratorObject2);
        when(this.collaboratorObject3.getStringValue(COLLABORATOR_LABEL)).thenReturn(GROUP_STR);
        when(this.collaboratorObject3.getStringValue(ACCESS_LABEL)).thenReturn(EDIT_LABEL);
        objects.add(this.collaboratorObject3);
        when(this.entityDoc.getXObjects(COLLABORATOR_CLASS)).thenReturn(objects);

        Collection<Collaborator> collaborators = this.component.getCollaborators(this.entity);
        Assert.assertEquals(2, collaborators.size());
        Collaborator c = new DefaultCollaborator(OTHER_USER, VIEW_ACCESS, this.helper);
        Assert.assertTrue(collaborators.contains(c));
        c = new DefaultCollaborator(GROUP, EDIT_ACCESS, this.helper);
        Assert.assertTrue(collaborators.contains(c));
    }

    /**
     * {@link EntityAccessManager#getCollaborators(PrimaryEntity)} filters out collaborators with empty access.
     */
    @Test
    public void getCollaboratorsCollaboratorsWithEmptyAccessAreFilteredOut()
    {
        List<BaseObject> objects = new ArrayList<>();
        when(this.collaboratorObject1.getStringValue(COLLABORATOR_LABEL)).thenReturn(COLLABORATOR_STR);
        when(this.collaboratorObject1.getStringValue(ACCESS_LABEL)).thenReturn(StringUtils.EMPTY);
        objects.add(this.collaboratorObject1);
        when(this.collaboratorObject2.getStringValue(COLLABORATOR_LABEL)).thenReturn(OTHER_USER_STR);
        when(this.collaboratorObject2.getStringValue(ACCESS_LABEL)).thenReturn(VIEW_LABEL);
        objects.add(this.collaboratorObject2);
        when(this.collaboratorObject3.getStringValue(COLLABORATOR_LABEL)).thenReturn(GROUP_STR);
        when(this.collaboratorObject3.getStringValue(ACCESS_LABEL)).thenReturn(EDIT_LABEL);
        objects.add(this.collaboratorObject3);
        when(this.entityDoc.getXObjects(COLLABORATOR_CLASS)).thenReturn(objects);

        Collection<Collaborator> collaborators = this.component.getCollaborators(this.entity);
        Assert.assertEquals(2, collaborators.size());
        Collaborator c = new DefaultCollaborator(OTHER_USER, VIEW_ACCESS, this.helper);
        Assert.assertTrue(collaborators.contains(c));
        c = new DefaultCollaborator(GROUP, EDIT_ACCESS, this.helper);
        Assert.assertTrue(collaborators.contains(c));
    }

    /**
     * {@link EntityAccessManager#getCollaborators(PrimaryEntity)} filters out collaborators with blank access.
     */
    @Test
    public void getCollaboratorsCollaboratorsWithBlankAccessAreFilteredOut()
    {
        List<BaseObject> objects = new ArrayList<>();
        when(this.collaboratorObject1.getStringValue(COLLABORATOR_LABEL)).thenReturn(COLLABORATOR_STR);
        when(this.collaboratorObject1.getStringValue(ACCESS_LABEL)).thenReturn(StringUtils.SPACE);
        objects.add(this.collaboratorObject1);
        when(this.collaboratorObject2.getStringValue(COLLABORATOR_LABEL)).thenReturn(OTHER_USER_STR);
        when(this.collaboratorObject2.getStringValue(ACCESS_LABEL)).thenReturn(VIEW_LABEL);
        objects.add(this.collaboratorObject2);
        when(this.collaboratorObject3.getStringValue(COLLABORATOR_LABEL)).thenReturn(GROUP_STR);
        when(this.collaboratorObject3.getStringValue(ACCESS_LABEL)).thenReturn(EDIT_LABEL);
        objects.add(this.collaboratorObject3);
        when(this.entityDoc.getXObjects(COLLABORATOR_CLASS)).thenReturn(objects);

        Collection<Collaborator> collaborators = this.component.getCollaborators(this.entity);
        Assert.assertEquals(2, collaborators.size());
        Collaborator c = new DefaultCollaborator(OTHER_USER, VIEW_ACCESS, this.helper);
        Assert.assertTrue(collaborators.contains(c));
        c = new DefaultCollaborator(GROUP, EDIT_ACCESS, this.helper);
        Assert.assertTrue(collaborators.contains(c));
    }

    /**
     * {@link EntityAccessManager#getCollaborators(PrimaryEntity)} gives no access to collaborators with invalid access.
     */
    @Test
    public void getCollaboratorsCollaboratorsWithInvalidAccessHaveNoAccess()
    {
        List<BaseObject> objects = new ArrayList<>();
        when(this.collaboratorObject1.getStringValue(COLLABORATOR_LABEL)).thenReturn(COLLABORATOR_STR);
        when(this.collaboratorObject1.getStringValue(ACCESS_LABEL)).thenReturn("wrgfhfdhgong");
        objects.add(this.collaboratorObject1);
        when(this.collaboratorObject2.getStringValue(COLLABORATOR_LABEL)).thenReturn(OTHER_USER_STR);
        when(this.collaboratorObject2.getStringValue(ACCESS_LABEL)).thenReturn(VIEW_LABEL);
        objects.add(this.collaboratorObject2);
        when(this.collaboratorObject3.getStringValue(COLLABORATOR_LABEL)).thenReturn(GROUP_STR);
        when(this.collaboratorObject3.getStringValue(ACCESS_LABEL)).thenReturn(EDIT_LABEL);
        objects.add(this.collaboratorObject3);
        when(this.entityDoc.getXObjects(COLLABORATOR_CLASS)).thenReturn(objects);

        Collection<Collaborator> collaborators = this.component.getCollaborators(this.entity);
        Assert.assertEquals(3, collaborators.size());
        Collaborator c = new DefaultCollaborator(OTHER_USER, VIEW_ACCESS, this.helper);
        Assert.assertTrue(collaborators.contains(c));
        c = new DefaultCollaborator(GROUP, EDIT_ACCESS, this.helper);
        Assert.assertTrue(collaborators.contains(c));
        c = new DefaultCollaborator(COLLABORATOR, this.noAccess, this.helper);
        Assert.assertTrue(collaborators.contains(c));
    }

    /**
     * {@link EntityAccessManager#getCollaborators(PrimaryEntity)} filters out collaborators with null name.
     */
    @Test
    public void getCollaboratorsCollaboratorsWithNullNameAreFilteredOut()
    {
        List<BaseObject> objects = new ArrayList<>();
        when(this.collaboratorObject1.getStringValue(COLLABORATOR_LABEL)).thenReturn(null);
        when(this.collaboratorObject1.getStringValue(ACCESS_LABEL)).thenReturn(EDIT_LABEL);
        objects.add(this.collaboratorObject1);
        when(this.collaboratorObject2.getStringValue(COLLABORATOR_LABEL)).thenReturn(OTHER_USER_STR);
        when(this.collaboratorObject2.getStringValue(ACCESS_LABEL)).thenReturn(VIEW_LABEL);
        objects.add(this.collaboratorObject2);
        when(this.collaboratorObject3.getStringValue(COLLABORATOR_LABEL)).thenReturn(GROUP_STR);
        when(this.collaboratorObject3.getStringValue(ACCESS_LABEL)).thenReturn(EDIT_LABEL);
        objects.add(this.collaboratorObject3);
        when(this.entityDoc.getXObjects(COLLABORATOR_CLASS)).thenReturn(objects);

        Collection<Collaborator> collaborators = this.component.getCollaborators(this.entity);
        Assert.assertEquals(2, collaborators.size());
        Collaborator c = new DefaultCollaborator(OTHER_USER, VIEW_ACCESS, this.helper);
        Assert.assertTrue(collaborators.contains(c));
        c = new DefaultCollaborator(GROUP, EDIT_ACCESS, this.helper);
        Assert.assertTrue(collaborators.contains(c));
    }

    /**
     * {@link EntityAccessManager#getCollaborators(PrimaryEntity)} filters out collaborators with empty name.
     */
    @Test
    public void getCollaboratorsCollaboratorsWithEmptyNameAreFilteredOut()
    {
        List<BaseObject> objects = new ArrayList<>();
        when(this.collaboratorObject1.getStringValue(COLLABORATOR_LABEL)).thenReturn(StringUtils.EMPTY);
        when(this.collaboratorObject1.getStringValue(ACCESS_LABEL)).thenReturn(EDIT_LABEL);
        objects.add(this.collaboratorObject1);
        when(this.collaboratorObject2.getStringValue(COLLABORATOR_LABEL)).thenReturn(OTHER_USER_STR);
        when(this.collaboratorObject2.getStringValue(ACCESS_LABEL)).thenReturn(VIEW_LABEL);
        objects.add(this.collaboratorObject2);
        when(this.collaboratorObject3.getStringValue(COLLABORATOR_LABEL)).thenReturn(GROUP_STR);
        when(this.collaboratorObject3.getStringValue(ACCESS_LABEL)).thenReturn(EDIT_LABEL);
        objects.add(this.collaboratorObject3);
        when(this.entityDoc.getXObjects(COLLABORATOR_CLASS)).thenReturn(objects);

        Collection<Collaborator> collaborators = this.component.getCollaborators(this.entity);
        Assert.assertEquals(2, collaborators.size());
        Collaborator c = new DefaultCollaborator(OTHER_USER, VIEW_ACCESS, this.helper);
        Assert.assertTrue(collaborators.contains(c));
        c = new DefaultCollaborator(GROUP, EDIT_ACCESS, this.helper);
        Assert.assertTrue(collaborators.contains(c));
    }

    /**
     * {@link EntityAccessManager#getCollaborators(PrimaryEntity)} filters out collaborators with blank name.
     */
    @Test
    public void getCollaboratorsCollaboratorsWithBlankNameAreFilteredOut()
    {
        List<BaseObject> objects = new ArrayList<>();
        when(this.collaboratorObject1.getStringValue(COLLABORATOR_LABEL)).thenReturn(StringUtils.SPACE);
        when(this.collaboratorObject1.getStringValue(ACCESS_LABEL)).thenReturn(EDIT_LABEL);
        objects.add(this.collaboratorObject1);
        when(this.collaboratorObject2.getStringValue(COLLABORATOR_LABEL)).thenReturn(OTHER_USER_STR);
        when(this.collaboratorObject2.getStringValue(ACCESS_LABEL)).thenReturn(VIEW_LABEL);
        objects.add(this.collaboratorObject2);
        when(this.collaboratorObject3.getStringValue(COLLABORATOR_LABEL)).thenReturn(GROUP_STR);
        when(this.collaboratorObject3.getStringValue(ACCESS_LABEL)).thenReturn(EDIT_LABEL);
        objects.add(this.collaboratorObject3);
        when(this.entityDoc.getXObjects(COLLABORATOR_CLASS)).thenReturn(objects);

        Collection<Collaborator> collaborators = this.component.getCollaborators(this.entity);
        Assert.assertEquals(2, collaborators.size());
        Collaborator c = new DefaultCollaborator(OTHER_USER, VIEW_ACCESS, this.helper);
        Assert.assertTrue(collaborators.contains(c));
        c = new DefaultCollaborator(GROUP, EDIT_ACCESS, this.helper);
        Assert.assertTrue(collaborators.contains(c));
    }

    /**
     * {@link EntityAccessManager#getCollaborators(PrimaryEntity)} logs unexpected exceptions and returns empty
     * collaborators.
     */
    @Test
    public void getCollaboratorsUnexpectedExceptionsAreLoggedAndEmptyCollaboratorsAreReturned()
    {
        when(this.entity.getXDocument()).thenThrow(new NullPointerException());

        final Collection<Collaborator> result = this.component.getCollaborators(this.entity);
        Assert.assertTrue(result.isEmpty());
        verify(this.logger, times(1)).error("Unexpected exception occurred when retrieving collaborators for entity "
            + "[{}]", this.entity);
    }

    /** {@link EntityAccessManager#getCollaborators(PrimaryEntity)} skips objects with missing values. */
    @Test
    public void getCollaboratorsWithMissingValues()
    {
        List<BaseObject> objects = new ArrayList<>();
        when(this.collaboratorObject1.getStringValue(COLLABORATOR_LABEL)).thenReturn(COLLABORATOR_STR);
        when(this.collaboratorObject1.getStringValue(ACCESS_LABEL)).thenReturn(StringUtils.EMPTY);
        objects.add(this.collaboratorObject1);
        objects.add(null);
        when(this.collaboratorObject2.getStringValue(COLLABORATOR_LABEL)).thenReturn(StringUtils.EMPTY);
        when(this.collaboratorObject2.getStringValue(ACCESS_LABEL)).thenReturn(VIEW_LABEL);
        objects.add(this.collaboratorObject2);
        when(this.collaboratorObject3.getStringValue(COLLABORATOR_LABEL)).thenReturn(null);
        when(this.collaboratorObject3.getStringValue(ACCESS_LABEL)).thenReturn(null);
        objects.add(this.collaboratorObject3);
        when(this.entityDoc.getXObjects(COLLABORATOR_CLASS)).thenReturn(objects);
        Collection<Collaborator> collaborators = this.component.getCollaborators(this.entity);
        Assert.assertTrue(collaborators.isEmpty());
    }

    /**
     * {@link EntityAccessManager#getCollaborators(PrimaryEntity)} returns an empty set when accessing the patient
     * fails.
     */
    @Test
    public void getCollaboratorsWithException()
    {
        when(this.entity.getXDocument()).thenThrow(new RuntimeException());
        Collection<Collaborator> collaborators = this.component.getCollaborators(this.entity);
        Assert.assertNotNull(collaborators);
        Assert.assertTrue(collaborators.isEmpty());
    }

    /**
     * {@link EntityAccessManager#getCollaborators(PrimaryEntity)} returns an empty collection if patient has no
     * document reference.
     */
    @Test
    public void getCollaboratorsEntityHasNoDocumentReferenceResultsInEmptyCollection()
    {
        when(this.entity.getDocumentReference()).thenReturn(null);

        final Collection<Collaborator> result = this.component.getCollaborators(this.entity);
        Assert.assertTrue(result.isEmpty());
    }

    /** Basic tests for {@link EntityAccessManager#setCollaborators(PrimaryEntity, Collection)}. */
    @Test
    public void setCollaborators() throws XWikiException
    {
        Collection<Collaborator> collaborators = new HashSet<>();
        Collaborator c = new DefaultCollaborator(COLLABORATOR, EDIT_ACCESS, this.helper);
        collaborators.add(c);
        c = new DefaultCollaborator(OTHER_USER, VIEW_ACCESS, this.helper);
        collaborators.add(c);
        when(this.entityDoc.newXObject(COLLABORATOR_CLASS, this.context)).thenReturn(this.collaboratorObject1);

        Assert.assertTrue(this.component.setCollaborators(this.entity, collaborators));
        verify(this.collaboratorObject1).setStringValue(COLLABORATOR_LABEL, COLLABORATOR_STR);
        verify(this.collaboratorObject1).setStringValue(ACCESS_LABEL, EDIT_LABEL);
        verify(this.collaboratorObject1).setStringValue(COLLABORATOR_LABEL, OTHER_USER_STR);
        verify(this.collaboratorObject1).setStringValue(ACCESS_LABEL, VIEW_LABEL);
        verify(this.entityDoc).removeXObjects(COLLABORATOR_CLASS);
        verify(this.xwiki).saveDocument(this.entityDoc, "Updated collaborators", true, this.context);
    }

    /** {@link EntityAccessManager#setCollaborators(PrimaryEntity, Collection)} ignores null collaborators. */
    @Test
    public void setCollaboratorsIgnoresAnyNullCollaborators() throws XWikiException
    {
        Collection<Collaborator> collaborators = new HashSet<>();
        Collaborator c = new DefaultCollaborator(COLLABORATOR, EDIT_ACCESS, this.helper);
        collaborators.add(c);
        c = new DefaultCollaborator(OTHER_USER, VIEW_ACCESS, this.helper);
        collaborators.add(c);
        collaborators.add(null);
        when(this.entityDoc.newXObject(COLLABORATOR_CLASS, this.context)).thenReturn(this.collaboratorObject1);

        Assert.assertTrue(this.component.setCollaborators(this.entity, collaborators));
        verify(this.collaboratorObject1).setStringValue(COLLABORATOR_LABEL, COLLABORATOR_STR);
        verify(this.collaboratorObject1).setStringValue(ACCESS_LABEL, EDIT_LABEL);
        verify(this.collaboratorObject1).setStringValue(COLLABORATOR_LABEL, OTHER_USER_STR);
        verify(this.collaboratorObject1).setStringValue(ACCESS_LABEL, VIEW_LABEL);
        verify(this.entityDoc, times(1)).removeXObjects(COLLABORATOR_CLASS);
        verify(this.entityDoc, times(2)).newXObject(COLLABORATOR_CLASS, this.context);
        verify(this.xwiki).saveDocument(this.entityDoc, "Updated collaborators", true, this.context);
    }

    /** {@link EntityAccessManager#setCollaborators(PrimaryEntity, Collection)} ignores collaborators with null user. */
    @Test
    public void setCollaboratorsIgnoresAnyCollaboratorsWithNullUser() throws XWikiException
    {
        Collection<Collaborator> collaborators = new ArrayList<>();
        Collaborator c = new DefaultCollaborator(COLLABORATOR, EDIT_ACCESS, this.helper);
        collaborators.add(c);
        c = new DefaultCollaborator(null, VIEW_ACCESS, this.helper);
        collaborators.add(c);
        when(this.entityDoc.newXObject(COLLABORATOR_CLASS, this.context)).thenReturn(this.collaboratorObject1);

        Assert.assertTrue(this.component.setCollaborators(this.entity, collaborators));
        verify(this.collaboratorObject1).setStringValue(COLLABORATOR_LABEL, COLLABORATOR_STR);
        verify(this.collaboratorObject1).setStringValue(ACCESS_LABEL, EDIT_LABEL);
        verify(this.entityDoc, times(1)).removeXObjects(COLLABORATOR_CLASS);
        verify(this.entityDoc, times(1)).newXObject(COLLABORATOR_CLASS, this.context);
        verify(this.xwiki).saveDocument(this.entityDoc, "Updated collaborators", true, this.context);
    }

    /**
     * {@link EntityAccessManager#setCollaborators(PrimaryEntity, Collection)} returns false when accessing the patient
     * fails.
     */
    @Test
    public void setCollaboratorsWithFailure()
    {
        doThrow(new RuntimeException()).when(this.entity).getXDocument();
        Collection<Collaborator> collaborators = new HashSet<>();
        Assert.assertFalse(this.component.setCollaborators(this.entity, collaborators));
    }

    /**
     * {@link EntityAccessManager#addCollaborator(PrimaryEntity, Collaborator)} adds a new Collaborator object if one
     * doesn't exist already.
     */
    @Test
    public void addCollaboratorWithNewObject() throws XWikiException
    {
        when(this.entityDoc.getXObject(COLLABORATOR_CLASS, COLLABORATOR_LABEL, COLLABORATOR_STR, false))
            .thenReturn(null);
        when(this.entityDoc.newXObject(COLLABORATOR_CLASS, this.context)).thenReturn(this.collaboratorObject1);

        Collaborator collaborator = new DefaultCollaborator(COLLABORATOR, EDIT_ACCESS, this.helper);

        Assert.assertTrue(this.component.addCollaborator(this.entity, collaborator));
        verify(this.collaboratorObject1).setStringValue(COLLABORATOR_LABEL, COLLABORATOR_STR);
        verify(this.collaboratorObject1).setStringValue(ACCESS_LABEL, EDIT_LABEL);
        verify(this.xwiki).saveDocument(this.entityDoc, "Added collaborator: " + COLLABORATOR_STR, true, this.context);
    }

    /**
     * {@link EntityAccessManager#addCollaborator(PrimaryEntity, Collaborator)} does not add null collaborators.
     */
    @Test
    public void addCollaboratorWithNullCollaboratorObject() throws XWikiException
    {
        when(this.entityDoc.getXObject(COLLABORATOR_CLASS, COLLABORATOR_LABEL, COLLABORATOR_STR, false))
            .thenReturn(this.collaboratorObject1);

        Assert.assertFalse(this.component.addCollaborator(this.entity, null));
        verify(this.collaboratorObject1, never()).setStringValue(anyString(), anyString());
        verify(this.collaboratorObject1, never()).setStringValue(anyString(), anyString());
        verify(this.xwiki, never()).saveDocument(any(), anyString(), anyBoolean(), any());
    }

    /** {@link EntityAccessManager#addCollaborator(PrimaryEntity, Collaborator)} modifies the existing Collaborator
     * object.
     */
    @Test
    public void addCollaboratorWithExistingObject() throws XWikiException
    {
        when(this.entityDoc.getXObject(COLLABORATOR_CLASS, COLLABORATOR_LABEL, COLLABORATOR_STR, false))
            .thenReturn(this.collaboratorObject1);

        Collaborator collaborator = new DefaultCollaborator(COLLABORATOR, EDIT_ACCESS, this.helper);

        Assert.assertTrue(this.component.addCollaborator(this.entity, collaborator));
        verify(this.collaboratorObject1).setStringValue(COLLABORATOR_LABEL, COLLABORATOR_STR);
        verify(this.collaboratorObject1).setStringValue(ACCESS_LABEL, EDIT_LABEL);
        verify(this.xwiki).saveDocument(this.entityDoc, "Added collaborator: " + COLLABORATOR_STR, true, this.context);
    }

    /**
     * {@link EntityAccessManager#addCollaborator(PrimaryEntity, Collaborator)} returns false when accessing the
     * document fails.
     */
    @Test
    public void addCollaboratorWithFailure()
    {
        doThrow(new RuntimeException()).when(this.entity).getXDocument();

        Collaborator collaborator = new DefaultCollaborator(COLLABORATOR, EDIT_ACCESS, this.helper);

        Assert.assertFalse(this.component.addCollaborator(this.entity, collaborator));
    }

    /**
     * {@link EntityAccessManager#removeCollaborator(PrimaryEntity, Collaborator)} returns false with null collaborator.
     */
    @Test
    public void removeCollaboratorReturnsFalseIfCollaboratorIsNull() throws XWikiException
    {
        Assert.assertFalse(this.component.removeCollaborator(this.entity, null));
        verify(this.entityDoc, never()).removeXObject(any());
        verify(this.xwiki, never()).saveDocument(any(), anyString(), anyBoolean(), any());
    }

    /**
     * {@link EntityAccessManager#removeCollaborator(PrimaryEntity, Collaborator)} removes the existing Collaborator.
     */
    @Test
    public void removeCollaboratorWithExistingObject() throws XWikiException
    {
        when(this.entityDoc.getXObject(COLLABORATOR_CLASS, COLLABORATOR_LABEL, COLLABORATOR_STR, false))
            .thenReturn(this.collaboratorObject1);

        Collaborator collaborator = new DefaultCollaborator(COLLABORATOR, EDIT_ACCESS, this.helper);

        Assert.assertTrue(this.component.removeCollaborator(this.entity, collaborator));
        verify(this.entityDoc).removeXObject(this.collaboratorObject1);
        verify(this.xwiki).saveDocument(this.entityDoc, "Removed collaborator: " + COLLABORATOR_STR, true,
            this.context);
    }

    /**
     * {@link EntityAccessManager#removeCollaborator(PrimaryEntity, Collaborator)} does nothing if the object isn't
     * found.
     */
    @Test
    public void removeCollaboratorWithMissingObject() throws XWikiException
    {
        when(this.entityDoc.getXObject(COLLABORATOR_CLASS, COLLABORATOR_LABEL, COLLABORATOR_STR, false))
            .thenReturn(null);

        Collaborator collaborator = new DefaultCollaborator(COLLABORATOR, EDIT_ACCESS, this.helper);

        Assert.assertFalse(this.component.removeCollaborator(this.entity, collaborator));
        verify(this.entityDoc, never()).removeXObject(any(BaseObject.class));
        verify(this.xwiki, never()).saveDocument(this.entityDoc, "Removed collaborator: " + COLLABORATOR_STR, true,
            this.context);
    }

    /**
     * {@link EntityAccessManager#removeCollaborator(PrimaryEntity, Collaborator)} returns false when accessing the
     * document fails.
     */
    @Test
    public void removeCollaboratorWithFailure()
    {
        doThrow(new RuntimeException()).when(this.entity).getXDocument();

        Collaborator collaborator = new DefaultCollaborator(COLLABORATOR, EDIT_ACCESS, this.helper);

        Assert.assertFalse(this.component.removeCollaborator(this.entity, collaborator));
    }

    /** {@link EntityAccessManager#getAccessLevel(PrimaryEntity, EntityReference)} returns no access for guest users. */
    @Test
    public void getAccessLevelWithOwner() throws Exception
    {
        XWikiGroupService grpService = mock(XWikiGroupService.class);
        when(this.xwiki.getGroupService(this.context)).thenReturn(grpService);
        when(grpService.getAllGroupsReferencesForMember(COLLABORATOR, 0, 0, this.context))
            .thenReturn(Collections.emptyList());

        Assert.assertSame(OWNER_ACCESS, this.component.getAccessLevel(this.entity, OWNER));
    }

    /** {@link EntityAccessManager#getAccessLevel(PrimaryEntity, EntityReference)} returns no access for guest users. */
    @Test
    public void getAccessLevelWithGuestUser()
    {
        Assert.assertSame(NO_ACCESS, this.component.getAccessLevel(this.entity, null));
    }

    /**
     * {@link EntityAccessManager#getAccessLevel(PrimaryEntity, EntityReference)} returns no access with missing
     * patient.
     */
    @Test
    public void getAccessLevelWithMissingPatient()
    {
        Assert.assertSame(NO_ACCESS, this.component.getAccessLevel(null, OTHER_USER));
    }

    /**
     * {@link EntityAccessManager#getAccessLevel(PrimaryEntity, EntityReference)} returns no access with missing patient
     * and user.
     */
    @Test
    public void getAccessLevelWithMissingPatientAndGuestUser()
    {
        Assert.assertSame(NO_ACCESS, this.component.getAccessLevel(null, null));
    }

    /**
     * {@link EntityAccessManager#getAccessLevel(PrimaryEntity, EntityReference)} returns the specified access for a
     * registered collaborator.
     */
    @Test
    public void getAccessLevelWithSpecifiedCollaborator() throws XWikiException
    {
        List<BaseObject> objects = new ArrayList<>();
        when(this.collaboratorObject1.getStringValue(COLLABORATOR_LABEL)).thenReturn(COLLABORATOR_STR);
        when(this.collaboratorObject1.getStringValue(ACCESS_LABEL)).thenReturn(EDIT_LABEL);
        objects.add(this.collaboratorObject1);
        when(this.collaboratorObject2.getStringValue(COLLABORATOR_LABEL)).thenReturn(OTHER_USER_STR);
        when(this.collaboratorObject2.getStringValue(ACCESS_LABEL)).thenReturn(VIEW_LABEL);
        objects.add(this.collaboratorObject2);
        when(this.entityDoc.getXObjects(COLLABORATOR_CLASS)).thenReturn(objects);
        when(this.xwiki.getGroupService(this.context)).thenReturn(this.groupService);
        when(this.groupService.getAllGroupsReferencesForMember(COLLABORATOR, 0, 0, this.context))
            .thenReturn(Collections.emptyList());

        Assert.assertSame(EDIT_ACCESS, this.component.getAccessLevel(this.entity, COLLABORATOR));
    }

    /**
     * {@link EntityAccessManager#getAccessLevel(PrimaryEntity, EntityReference)} returns the specified access for a
     * registered collaborator.
     */
    @Test
    public void getAccessLevelWithGroupMemberCollaborator() throws XWikiException
    {
        List<BaseObject> objects = new ArrayList<>();
        when(this.collaboratorObject1.getStringValue(COLLABORATOR_LABEL)).thenReturn(GROUP_STR);
        when(this.collaboratorObject1.getStringValue(ACCESS_LABEL)).thenReturn(EDIT_LABEL);
        objects.add(this.collaboratorObject1);
        when(this.collaboratorObject2.getStringValue(COLLABORATOR_LABEL)).thenReturn(OTHER_USER_STR);
        when(this.collaboratorObject2.getStringValue(ACCESS_LABEL)).thenReturn(VIEW_LABEL);
        objects.add(this.collaboratorObject2);
        when(this.entityDoc.getXObjects(COLLABORATOR_CLASS)).thenReturn(objects);
        when(this.xwiki.getGroupService(this.context)).thenReturn(this.groupService);
        when(this.groupService.getAllGroupsReferencesForMember(COLLABORATOR, 0, 0, this.context))
            .thenReturn(Collections.singletonList(GROUP));

        Assert.assertSame(EDIT_ACCESS, this.component.getAccessLevel(this.entity, COLLABORATOR));
    }

    /**
     * {@link EntityAccessManager#getAccessLevel(PrimaryEntity, EntityReference)} returns no access when XWiki throws
     * exceptions.
     */
    @Test
    public void getAccessLevelWithExceptions() throws ComponentLookupException, XWikiException
    {
        when(this.xwiki.getGroupService(this.context)).thenThrow(new XWikiException());
        Assert.assertSame(NO_ACCESS, this.mocker.getComponentUnderTest().getAccessLevel(this.entity, OTHER_USER));
    }

    /** Basic tests for {@link EntityAccessManager#listAccessLevels()}. */
    @Test
    public void listAccessLevels() throws ComponentLookupException
    {
        List<AccessLevel> levels = new ArrayList<>();
        levels.add(EDIT_ACCESS);
        levels.add(NO_ACCESS);
        levels.add(OWNER_ACCESS);
        levels.add(VIEW_ACCESS);
        levels.add(MANAGE_ACCESS);
        when(this.componentManager.<AccessLevel>getInstanceList(AccessLevel.class)).thenReturn(levels);
        Collection<AccessLevel> returnedLevels = this.component.listAccessLevels();
        Assert.assertEquals(3, returnedLevels.size());
        Iterator<AccessLevel> it = returnedLevels.iterator();
        Assert.assertSame(VIEW_ACCESS, it.next());
        Assert.assertSame(EDIT_ACCESS, it.next());
        Assert.assertSame(MANAGE_ACCESS, it.next());
        Assert.assertFalse(returnedLevels.contains(NO_ACCESS));
        Assert.assertFalse(returnedLevels.contains(OWNER_ACCESS));
    }

    /** Basic tests for {@link EntityAccessManager#listAllAccessLevels()}. */
    @Test
    public void listAllAccessLevels() throws ComponentLookupException
    {
        List<AccessLevel> levels = new ArrayList<>();
        levels.add(EDIT_ACCESS);
        levels.add(NO_ACCESS);
        levels.add(OWNER_ACCESS);
        levels.add(VIEW_ACCESS);
        levels.add(MANAGE_ACCESS);
        when(this.componentManager.<AccessLevel>getInstanceList(AccessLevel.class)).thenReturn(levels);
        Collection<AccessLevel> returnedLevels = this.component.listAllAccessLevels();
        Assert.assertEquals(5, returnedLevels.size());
        Iterator<AccessLevel> it = returnedLevels.iterator();
        Assert.assertSame(NO_ACCESS, it.next());
        Assert.assertSame(VIEW_ACCESS, it.next());
        Assert.assertSame(EDIT_ACCESS, it.next());
        Assert.assertSame(MANAGE_ACCESS, it.next());
        Assert.assertSame(OWNER_ACCESS, it.next());
    }

    /** {@link EntityAccessManager#listAccessLevels()} returns an empty list when no implementations available. */
    @Test
    public void listAccessLevelsWithNoComponents() throws ComponentLookupException
    {
        when(this.componentManager.<AccessLevel>getInstanceList(AccessLevel.class)).thenReturn(Collections.emptyList());
        Collection<AccessLevel> returnedLevels = this.component.listAccessLevels();
        Assert.assertTrue(returnedLevels.isEmpty());
    }

    /** {@link EntityAccessManager#listAllAccessLevels()} returns an empty list when no implementations available. */
    @Test
    public void listAllAccessLevelsWithNoComponents() throws ComponentLookupException
    {
        when(this.componentManager.<AccessLevel>getInstanceList(AccessLevel.class)).thenReturn(Collections.emptyList());
        Collection<AccessLevel> returnedLevels = this.component.listAllAccessLevels();
        Assert.assertTrue(returnedLevels.isEmpty());
    }

    /** {@link EntityAccessManager#listAccessLevels()} returns an empty list when looking up components fails. */
    @Test
    public void listAccessLevelsWithLookupExceptions() throws ComponentLookupException
    {
        when(this.componentManager.<AccessLevel>getInstanceList(AccessLevel.class))
            .thenThrow(new ComponentLookupException("None"));
        Collection<AccessLevel> returnedLevels = this.component.listAccessLevels();
        Assert.assertTrue(returnedLevels.isEmpty());
    }

    /** {@link EntityAccessManager#listAllAccessLevels()} returns an empty list when looking up components fails. */
    @Test
    public void listAllAccessLevelsWithLookupExceptions() throws ComponentLookupException
    {
        when(this.componentManager.<AccessLevel>getInstanceList(AccessLevel.class))
            .thenThrow(new ComponentLookupException("None"));
        Collection<AccessLevel> returnedLevels = this.component.listAllAccessLevels();
        Assert.assertTrue(returnedLevels.isEmpty());
    }

    /** {@link EntityAccessManager#resolveAccessLevel(String)} returns the right implementation. */
    @Test
    public void resolveAccessLevel()
    {
        Assert.assertSame(EDIT_ACCESS, this.component.resolveAccessLevel(EDIT_LABEL));
    }

    /** {@link EntityAccessManager#resolveAccessLevel(String)} returns null if an unknown level is requested. */
    @Test
    public void resolveAccessLevelWithUnknownAccess() throws ComponentLookupException
    {
        when(this.componentManager.getInstance(AccessLevel.class, UNKNOWN_LABEL))
            .thenThrow(new ComponentLookupException("No such component"));
        Assert.assertSame(this.noAccess, this.component.resolveAccessLevel(UNKNOWN_LABEL));
    }

    /**
     * {@link EntityAccessManager#resolveAccessLevel(String)} returns no access if a null or blank level is requested.
     */
    @Test
    public void resolveAccessLevelWithNoAccess()
    {
        Assert.assertSame(this.noAccess, this.component.resolveAccessLevel(null));
        Assert.assertSame(this.noAccess, this.component.resolveAccessLevel(StringUtils.EMPTY));
        Assert.assertSame(this.noAccess, this.component.resolveAccessLevel(StringUtils.SPACE));
    }

    @Test
    public void isAdministratorReturnsFalseIfCurrentUserIsNull()
    {
        when(this.helper.getCurrentUser()).thenReturn(null);
        final boolean isAdmin = this.component.isAdministrator(this.entity);
        Assert.assertFalse(isAdmin);
    }

    @Test
    public void isAdministratorReturnsFalseIfCurrentUserIsNotAnAdministrator()
    {
        when(this.rights.hasAccess(Right.ADMIN, COLLABORATOR, PATIENT_REFERENCE)).thenReturn(false);
        when(this.helper.getCurrentUser()).thenReturn(COLLABORATOR);
        final boolean isAdmin = this.component.isAdministrator(this.entity);
        Assert.assertFalse(isAdmin);
    }

    @Test
    public void isAdministratorReturnsTrueIfCurrentUserIsAnAdministrator()
    {
        when(this.rights.hasAccess(Right.ADMIN, OWNER, PATIENT_REFERENCE)).thenReturn(true);
        when(this.helper.getCurrentUser()).thenReturn(OWNER);
        final boolean isAdmin = this.component.isAdministrator(this.entity);
        Assert.assertTrue(isAdmin);
    }

    @Test
    public void isAdministratorReturnsFalseIfPatientDocumentReferenceIsNull()
    {
        when(this.entity.getDocumentReference()).thenReturn(null);
        when(this.rights.hasAccess(Right.ADMIN, OWNER, PATIENT_REFERENCE)).thenReturn(true);
        final boolean isAdmin = this.component.isAdministrator(this.entity);
        Assert.assertFalse(isAdmin);
    }

    @Test
    public void isAdministratorReturnsFalseIfUserIsNull()
    {
        final boolean isAdmin = this.component.isAdministrator(this.entity, null);
        Assert.assertFalse(isAdmin);
    }

    @Test
    public void isAdministratorReturnsFalseIfUserIsNotAnAdministrator()
    {
        when(this.rights.hasAccess(Right.ADMIN, OWNER, PATIENT_REFERENCE)).thenReturn(false);
        final boolean isAdmin = this.component.isAdministrator(this.entity, null);
        Assert.assertFalse(isAdmin);
    }

    @Test
    public void isAdministratorReturnsTrueIfUserIsAnAdministrator()
    {
        when(this.rights.hasAccess(Right.ADMIN, OWNER, PATIENT_REFERENCE)).thenReturn(true);
        final boolean isAdmin = this.component.isAdministrator(this.entity, OWNER);
        Assert.assertTrue(isAdmin);
    }
}
