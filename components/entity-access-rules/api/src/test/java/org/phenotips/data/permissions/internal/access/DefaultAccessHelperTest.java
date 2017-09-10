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
package org.phenotips.data.permissions.internal.access;

import org.phenotips.data.Patient;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.Collaborator;
import org.phenotips.data.permissions.Owner;
import org.phenotips.data.permissions.internal.DefaultCollaborator;
import org.phenotips.data.permissions.internal.PermissionsHelper;

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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultAccessHelper}.
 */
public class DefaultAccessHelperTest
{
    private static final String ACCESS = "access";

    private static final String NONE = "none";

    private static final String VIEW = "view";

    private static final String EDIT = "edit";

    private static final String MANAGE = "manage";

    private static final String OWNER = "owner";

    private static final String COLLABORATOR = "collaborator";

    private static final String COLLABORATOR_1 = "collaborator1";

    private static final String COLLABORATOR_3 = "collaborator3";

    private static final String COLLABORATOR_4 = "collaborator4";

    private static final String GROUP_1 = "group1";

    private static final String CURRENT_MIXED = "currentmixed";

    private static final String JOHN_DOE = "xwiki:XWiki.JohnDoe";

    private static final String JANE_DOE = "xwiki:XWiki.JaneDoe";

    @Rule
    public MockitoComponentMockingRule<AccessHelper> mocker =
        new MockitoComponentMockingRule<>(DefaultAccessHelper.class);

    @Mock
    private XWiki wiki;

    @Mock
    private XWikiContext context;

    @Mock
    private Patient patient;

    // General
    @Mock
    private DocumentReference patientDocRef;

    @Mock
    private DocumentReference ownerDocRef;

    @Mock
    private DocumentReference collaboratorDocRef;

    // For each item
    @Mock
    private DocumentReference group1DocRef;

    @Mock
    private DocumentReference collaborator1DocRef;

    @Mock
    private DocumentReference collaborator2DocRef;

    @Mock
    private DocumentReference collaborator3DocRef;

    @Mock
    private BaseObject collaborator1BaseObj;

    @Mock
    private BaseObject collaborator2BaseObj;

    @Mock
    private BaseObject collaborator3BaseObj;

    @Mock
    private BaseObject collaborator4BaseObj;

    @Mock
    private BaseObject group1BaseObj;

    @Mock
    private XWikiDocument patientXDoc;

    private ComponentManager cm;

    private AccessHelper component;

    private Logger logger;

    private PermissionsHelper permissionsHelper;

    private DocumentReferenceResolver<EntityReference> partialEntityResolver;

    private DocumentReferenceResolver<String> stringEntityResolver;

    private EntityReferenceSerializer<String> referenceSerializer;

    private AuthorizationManager rights;

    private final AccessLevel editAccess = new EditAccessLevel();

    private final AccessLevel manageAccess = new ManageAccessLevel();

    private final AccessLevel viewAccess = new ViewAccessLevel();

    private final AccessLevel noAccess = new NoAccessLevel();

    private final AccessLevel ownerAccess = new OwnerAccessLevel();

    private Collaborator collaborator1;

    private Collaborator collaborator3;

    @Before
    public void setUp() throws ComponentLookupException, XWikiException
    {
        MockitoAnnotations.initMocks(this);

        this.component = this.mocker.getComponentUnderTest();
        this.logger = this.mocker.getMockedLogger();
        this.permissionsHelper = this.mocker.getInstance(PermissionsHelper.class);
        this.rights = this.mocker.getInstance(AuthorizationManager.class);

        final Provider<XWikiContext> xcontextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(this.context);

        this.cm = this.mocker.getInstance(ComponentManager.class, "context");

        final List<AccessLevel> allLevels = Arrays.asList(this.viewAccess, this.editAccess, this.manageAccess,
            this.ownerAccess, this.noAccess);
        when(this.cm.<AccessLevel>getInstanceList(AccessLevel.class)).thenReturn(allLevels);

        when(this.cm.getInstance(AccessLevel.class, VIEW)).thenReturn(this.viewAccess);
        when(this.cm.getInstance(AccessLevel.class, EDIT)).thenReturn(this.editAccess);
        when(this.cm.getInstance(AccessLevel.class, MANAGE)).thenReturn(this.manageAccess);
        when(this.cm.getInstance(AccessLevel.class, OWNER)).thenReturn(this.ownerAccess);
        when(this.cm.getInstance(AccessLevel.class, NONE)).thenReturn(this.noAccess);

        final ParameterizedType entityResolverType = new DefaultParameterizedType(null,
            DocumentReferenceResolver.class, EntityReference.class);
        this.partialEntityResolver = this.mocker.getInstance(entityResolverType, CURRENT_MIXED);

        final ParameterizedType stringResolverType = new DefaultParameterizedType(null,
            DocumentReferenceResolver.class, String.class);
        this.stringEntityResolver = this.mocker.getInstance(stringResolverType, CURRENT_MIXED);

        when(this.patient.getDocumentReference()).thenReturn(this.patientDocRef);

        this.referenceSerializer = this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);
        when(this.referenceSerializer.serialize(this.patientDocRef)).thenReturn("wiki:patient.P0000001");

        when(this.partialEntityResolver.resolve(Owner.CLASS_REFERENCE, this.patientDocRef))
            .thenReturn(this.ownerDocRef);
        when(this.partialEntityResolver.resolve(this.ownerDocRef)).thenReturn(this.ownerDocRef);
        when(this.referenceSerializer.serialize(this.ownerDocRef)).thenReturn(JOHN_DOE);

        when(this.patient.getXDocument()).thenReturn(this.patientXDoc);
        when(this.permissionsHelper.getStringProperty(this.patientXDoc, this.ownerDocRef, OWNER)).thenReturn(JOHN_DOE);

        when(this.stringEntityResolver.resolve(JOHN_DOE, this.patientDocRef)).thenReturn(this.ownerDocRef);

        // Collaborators
        when(this.partialEntityResolver.resolve(Collaborator.CLASS_REFERENCE, this.patientDocRef))
            .thenReturn(this.collaboratorDocRef);
        final List<BaseObject> collaboratorBaseObjs = Arrays.asList(this.collaborator1BaseObj, null,
            this.collaborator3BaseObj, this.group1BaseObj);
        when(this.patientXDoc.getXObjects(this.collaboratorDocRef)).thenReturn(collaboratorBaseObjs);
        when(this.collaborator1BaseObj.getStringValue(COLLABORATOR)).thenReturn(COLLABORATOR_1);
        when(this.collaborator1BaseObj.getStringValue(ACCESS)).thenReturn(VIEW);
        when(this.collaborator3BaseObj.getStringValue(COLLABORATOR)).thenReturn(COLLABORATOR_3);
        when(this.collaborator3BaseObj.getStringValue(ACCESS)).thenReturn(EDIT);
        when(this.collaborator4BaseObj.getStringValue(COLLABORATOR)).thenReturn(COLLABORATOR_4);
        when(this.collaborator4BaseObj.getStringValue(ACCESS)).thenReturn(EDIT);

        when(this.group1BaseObj.getStringValue(COLLABORATOR)).thenReturn(GROUP_1);
        when(this.group1BaseObj.getStringValue(ACCESS)).thenReturn(VIEW);

        when(this.stringEntityResolver.resolve(COLLABORATOR_1, this.patientDocRef))
            .thenReturn(this.collaborator1DocRef);
        when(this.stringEntityResolver.resolve(COLLABORATOR_3, this.patientDocRef))
            .thenReturn(this.collaborator3DocRef);
        when(this.stringEntityResolver.resolve(GROUP_1, this.patientDocRef)).thenReturn(this.group1DocRef);

        // Group references
        when(this.context.getWiki()).thenReturn(this.wiki);
        final XWikiGroupService groupService = mock(XWikiGroupService.class);
        when(this.wiki.getGroupService(this.context)).thenReturn(groupService);
        when(groupService.getAllGroupsReferencesForMember(this.collaborator1DocRef, 0, 0, this.context))
            .thenReturn(Collections.singletonList(this.group1DocRef));
        when(groupService.getAllGroupsReferencesForMember(this.collaborator2DocRef, 0, 0, this.context))
            .thenReturn(Collections.singletonList(this.group1DocRef));

        this.collaborator1 = new DefaultCollaborator(this.collaborator1DocRef, this.editAccess, this.permissionsHelper);
        this.collaborator3 = new DefaultCollaborator(this.collaborator3DocRef, this.viewAccess, this.permissionsHelper);
    }

    @Test
    public void listAccessLevelsReturnsEmptyCollectionOnComponentLookupException() throws ComponentLookupException
    {
        when(this.cm.<AccessLevel>getInstanceList(AccessLevel.class)).thenThrow(new ComponentLookupException(NONE));
        final Collection<AccessLevel> result = this.component.listAccessLevels();
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void listAccessLevelsReturnsEmptyCollectionIfInstanceListIsEmpty() throws ComponentLookupException
    {
        when(this.cm.<AccessLevel>getInstanceList(AccessLevel.class)).thenReturn(Collections.emptyList());
        final Collection<AccessLevel> result = this.component.listAccessLevels();
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void listAccessLevelsReturnsEmptyCollectionIfInstanceListHasOnlyUnassignableAccessLevels()
        throws ComponentLookupException
    {
        when(this.cm.<AccessLevel>getInstanceList(AccessLevel.class)).thenReturn(Arrays.asList(this.ownerAccess,
            this.noAccess));
        final Collection<AccessLevel> result = this.component.listAccessLevels();
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void listAccessLevelsDoesNotReturnInaccessibleAccessLevels()
    {
        final Collection<AccessLevel> accessible = new TreeSet<>();
        accessible.add(this.viewAccess);
        accessible.add(this.editAccess);
        accessible.add(this.manageAccess);

        final Collection<AccessLevel> result = this.component.listAccessLevels();
        Assert.assertEquals(accessible, result);
    }

    @Test
    public void listAllAccessLevelsReturnsEmptyCollectionOnComponentLookupException() throws ComponentLookupException
    {
        when(this.cm.<AccessLevel>getInstanceList(AccessLevel.class)).thenThrow(new ComponentLookupException(NONE));
        final Collection<AccessLevel> result = this.component.listAllAccessLevels();
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void listAllAccessLevelsReturnsEmptyCollectionIfInstanceListIsEmpty() throws ComponentLookupException
    {
        when(this.cm.<AccessLevel>getInstanceList(AccessLevel.class)).thenReturn(Collections.emptyList());
        final Collection<AccessLevel> result = this.component.listAllAccessLevels();
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void listAllAccessLevelsReturnsAccessibleAndInaccessibleAccessLevels()
    {
        final Collection<AccessLevel> levels = new TreeSet<>();
        levels.add(this.viewAccess);
        levels.add(this.editAccess);
        levels.add(this.manageAccess);
        levels.add(this.ownerAccess);
        levels.add(this.noAccess);

        final Collection<AccessLevel> result = this.component.listAllAccessLevels();
        Assert.assertEquals(levels, result);
    }

    @Test
    public void resolveAccessLevelReturnsNullIfNameIsNull()
    {
        final AccessLevel level = this.component.resolveAccessLevel(null);
        Assert.assertNull(level);
    }

    @Test
    public void resolveAccessLevelReturnsNullIfNameIsEmpty()
    {
        final AccessLevel level = this.component.resolveAccessLevel(StringUtils.EMPTY);
        Assert.assertNull(level);
    }

    @Test
    public void resolveAccessLevelReturnsNullIfNameIsBlank()
    {
        final AccessLevel level = this.component.resolveAccessLevel(StringUtils.SPACE);
        Assert.assertNull(level);
    }

    @Test
    public void resolveAccessLevelReturnsNullIfRequestedAccessLevelInvalid() throws ComponentLookupException
    {
        when(this.cm.getInstance(AccessLevel.class, NONE)).thenThrow(new ComponentLookupException(NONE));

        final AccessLevel level = this.component.resolveAccessLevel(NONE);
        verify(this.logger, times(1)).warn("Invalid patient access level requested: {}", NONE);
        Assert.assertNull(level);
    }

    @Test
    public void resolveAccessLevelReturnsCorrectAccessLevelWhenValidNameRequested() throws ComponentLookupException
    {
        final AccessLevel level = this.component.resolveAccessLevel(EDIT);
        Assert.assertEquals(this.editAccess, level);
    }

    @Test
    public void getAccessLevelReturnsNoAccessLevelWhenUserToCheckReferenceIsNull()
    {
        final AccessLevel level = this.component.getAccessLevel(this.patient, null);
        Assert.assertEquals(this.noAccess, level);
    }

    @Test
    public void getAccessLevelReturnsNoAccessLevelIfPatientHasNullDocumentReference()
    {
        when(this.patient.getDocumentReference()).thenReturn(null);
        final AccessLevel level = this.component.getAccessLevel(this.patient, this.ownerDocRef);
        Assert.assertEquals(this.noAccess, level);
        verifyZeroInteractions(this.logger);
    }

    @Test
    public void getAccessLevelReturnsNoAccessLevelIfOwnerStringIsNullAndThereAreNoCollaborators()
    {
        when(this.permissionsHelper.getStringProperty(this.patientXDoc, this.ownerDocRef, OWNER)).thenReturn(null);
        when(this.partialEntityResolver.resolve(Collaborator.CLASS_REFERENCE, this.patientXDoc)).thenReturn(null);
        final AccessLevel level = this.component.getAccessLevel(this.patient, this.ownerDocRef);
        Assert.assertEquals(this.noAccess, level);
        verifyZeroInteractions(this.logger);
    }

    @Test
    public void getAccessLevelReturnsNoAccessLevelIfOwnerStringIsNullStrAndThereAreNoCollaborators()
    {
        when(this.permissionsHelper.getStringProperty(this.patientXDoc, this.ownerDocRef, OWNER))
            .thenReturn("null");
        when(this.partialEntityResolver.resolve(Collaborator.CLASS_REFERENCE, this.patientXDoc)).thenReturn(null);
        final AccessLevel level = this.component.getAccessLevel(this.patient, this.ownerDocRef);
        Assert.assertEquals(this.noAccess, level);
        verifyZeroInteractions(this.logger);
    }

    @Test
    public void getAccessLevelReturnsNoAccessLevelIfOwnerStringIsEmptyAndThereAreNoCollaborators()
    {
        when(this.permissionsHelper.getStringProperty(this.patientXDoc, this.ownerDocRef, OWNER))
            .thenReturn(StringUtils.EMPTY);
        when(this.partialEntityResolver.resolve(Collaborator.CLASS_REFERENCE, this.patientXDoc)).thenReturn(null);
        final AccessLevel level = this.component.getAccessLevel(this.patient, this.ownerDocRef);
        Assert.assertEquals(this.noAccess, level);
        verifyZeroInteractions(this.logger);
    }

    @Test
    public void getAccessLevelReturnsNoAccessLevelIfOwnerStringIsBlankAndThereAreNoCollaborators()
    {
        when(this.permissionsHelper.getStringProperty(this.patientXDoc, this.ownerDocRef, OWNER))
            .thenReturn(StringUtils.SPACE);
        when(this.partialEntityResolver.resolve(Collaborator.CLASS_REFERENCE, this.patientXDoc)).thenReturn(null);
        final AccessLevel level = this.component.getAccessLevel(this.patient, this.ownerDocRef);
        Assert.assertEquals(this.noAccess, level);
        verifyZeroInteractions(this.logger);
    }

    @Test
    public void getAccessLevelReturnsNoAccessLevelIfUserIsNotCollaborator()
    {
        final List<BaseObject> collaboratorBaseObjs = Arrays.asList(this.collaborator1BaseObj,
            this.collaborator2BaseObj);
        when(this.patientXDoc.getXObjects(this.collaboratorDocRef)).thenReturn(collaboratorBaseObjs);
        final AccessLevel level = this.component.getAccessLevel(this.patient, this.collaborator3DocRef);
        Assert.assertEquals(this.noAccess, level);
        verifyZeroInteractions(this.logger);
    }

    @Test
    public void getAccessLevelReturnsOwnerAccessLevelIfUserIsOwner()
    {
        final AccessLevel level = this.component.getAccessLevel(this.patient, this.ownerDocRef);
        Assert.assertEquals(this.ownerAccess, level);
        verifyZeroInteractions(this.logger);
    }

    @Test
    public void getAccessLevelReturnsCorrectAccessLevelIfUserIsCollaboratorWithViewAccess()
    {
        final AccessLevel level = this.component.getAccessLevel(this.patient, this.collaborator1DocRef);
        Assert.assertEquals(this.viewAccess, level);
        verifyZeroInteractions(this.logger);
    }

    @Test
    public void getAccessLevelReturnsCorrectAccessLevelIfUserIsInGroupWithHigherAccess()
    {
        when(this.group1BaseObj.getStringValue(ACCESS)).thenReturn(MANAGE);
        // Collaborator 1 has view access and belongs to group with manage access.
        final AccessLevel level = this.component.getAccessLevel(this.patient, this.collaborator1DocRef);
        Assert.assertEquals(this.manageAccess, level);
        verifyZeroInteractions(this.logger);
    }

    @Test
    public void isAdministratorReturnsFalseIfCurrentUserIsNull()
    {
        when(this.permissionsHelper.getCurrentUser()).thenReturn(null);
        final boolean isAdmin = this.component.isAdministrator(this.patient);
        Assert.assertFalse(isAdmin);
    }

    @Test
    public void isAdministratorReturnsFalseIfCurrentUserIsNotAnAdministrator()
    {
        final DocumentReference currentUser = mock(DocumentReference.class);
        when(this.rights.hasAccess(Right.ADMIN, currentUser, this.patientDocRef)).thenReturn(false);
        when(this.permissionsHelper.getCurrentUser()).thenReturn(currentUser);
        final boolean isAdmin = this.component.isAdministrator(this.patient);
        Assert.assertFalse(isAdmin);
    }

    @Test
    public void isAdministratorReturnsTrueIfCurrentUserIsAnAdministrator()
    {
        final DocumentReference currentUser = mock(DocumentReference.class);
        when(this.rights.hasAccess(Right.ADMIN, currentUser, this.patientDocRef)).thenReturn(true);
        when(this.permissionsHelper.getCurrentUser()).thenReturn(currentUser);
        final boolean isAdmin = this.component.isAdministrator(this.patient);
        Assert.assertTrue(isAdmin);
    }

    @Test
    public void isAdministratorReturnsFalseIfPatientDocumentReferenceIsNull()
    {
        final DocumentReference currentUser = mock(DocumentReference.class);
        when(this.patient.getDocumentReference()).thenReturn(null);
        when(this.rights.hasAccess(Right.ADMIN, currentUser, this.patientDocRef)).thenReturn(true);
        final boolean isAdmin = this.component.isAdministrator(this.patient, currentUser);
        Assert.assertFalse(isAdmin);
    }

    @Test
    public void isAdministratorReturnsFalseIfUserIsNull()
    {
        when(this.patient.getDocumentReference()).thenReturn(null);
        final boolean isAdmin = this.component.isAdministrator(this.patient, null);
        Assert.assertFalse(isAdmin);
    }

    @Test
    public void isAdministratorReturnsFalseIfUserIsNotAdministrator()
    {
        final DocumentReference currentUser = mock(DocumentReference.class);
        when(this.rights.hasAccess(Right.ADMIN, currentUser, this.patientDocRef)).thenReturn(false);
        final boolean isAdmin = this.component.isAdministrator(this.patient, currentUser);
        Assert.assertFalse(isAdmin);
    }

    @Test
    public void isAdministratorReturnsTrueIfUserIsAnAdministrator()
    {
        final DocumentReference currentUser = mock(DocumentReference.class);
        when(this.rights.hasAccess(Right.ADMIN, currentUser, this.patientDocRef)).thenReturn(true);
        final boolean isAdmin = this.component.isAdministrator(this.patient, currentUser);
        Assert.assertTrue(isAdmin);
    }

    @Test
    public void getOwnerReturnsOwnerWithNullUserIfPatientDocumentReferenceIsNull()
    {
        when(this.patient.getDocumentReference()).thenReturn(null);
        final Owner owner = this.component.getOwner(this.patient);
        Assert.assertNull(owner.getUser());
    }

    @Test
    public void getOwnerReturnsOwnerWithNullUserIfOwnerStringIsNull()
    {
        when(this.permissionsHelper.getStringProperty(this.patientXDoc, this.ownerDocRef, OWNER)).thenReturn(null);
        final Owner owner = this.component.getOwner(this.patient);
        Assert.assertNull(owner.getUser());
    }

    @Test
    public void getOwnerReturnsOwnerWithNullUserIfOwnerStringIsNullStr()
    {
        when(this.permissionsHelper.getStringProperty(this.patientXDoc, this.ownerDocRef, OWNER)).thenReturn("null");
        final Owner owner = this.component.getOwner(this.patient);
        Assert.assertNull(owner.getUser());
    }

    @Test
    public void getOwnerReturnsOwnerWithNullUserIfOwnerStringIsEmpty()
    {
        when(this.permissionsHelper.getStringProperty(this.patientXDoc, this.ownerDocRef, OWNER))
            .thenReturn(StringUtils.EMPTY);
        final Owner owner = this.component.getOwner(this.patient);
        Assert.assertNull(owner.getUser());
    }

    @Test
    public void getOwnerReturnsOwnerWithNullUserIfOwnerStringIsBlank()
    {
        when(this.permissionsHelper.getStringProperty(this.patientXDoc, this.ownerDocRef, OWNER))
            .thenReturn(StringUtils.SPACE);
        final Owner owner = this.component.getOwner(this.patient);
        Assert.assertNull(owner.getUser());
    }

    @Test
    public void getOwnerReturnsCorrectOwner()
    {
        final Owner owner = this.component.getOwner(this.patient);
        Assert.assertEquals(this.ownerDocRef, owner.getUser());
    }

    @Test
    public void setOwnerUserIsNullRemovesOldOwner() throws Exception
    {
        final boolean ownerSet = this.component.setOwner(this.patient, null);

        verify(this.permissionsHelper, times(1)).setProperty(eq(this.patientXDoc), any(DocumentReference.class),
            eq(OWNER), eq(StringUtils.EMPTY));
        Assert.assertTrue(ownerSet);
    }

    @Test
    public void setOwnerEntityDocReferenceIsNullReturnsFalse()
    {
        when(this.patient.getDocumentReference()).thenReturn(null);

        final boolean ownerSet = this.component.setOwner(this.patient, this.collaborator1DocRef);
        Assert.assertFalse(ownerSet);
    }

    @Test
    public void setOwnerClassReferenceCannotBeDeterminedReturnsFalse()
    {
        when(this.partialEntityResolver.resolve(Owner.CLASS_REFERENCE, this.patientDocRef)).thenReturn(null);

        final boolean ownerSet = this.component.setOwner(this.patient, this.collaborator1DocRef);
        Assert.assertFalse(ownerSet);
    }

    @Test
    public void setOwnerPreviousOwnerIsNullNoCollaboratorIsAddedAndOneIsDeleted() throws XWikiException
    {
        when(this.permissionsHelper.getStringProperty(this.patientXDoc, this.ownerDocRef, OWNER)).thenReturn(null);

        when(this.partialEntityResolver.resolve(Collaborator.CLASS_REFERENCE, this.patientDocRef)).thenReturn(this.collaboratorDocRef);
        when(this.partialEntityResolver.resolve(this.collaborator1DocRef)).thenReturn(this.collaborator1DocRef);
        when(this.referenceSerializer.serialize(this.collaborator1DocRef)).thenReturn(JOHN_DOE);
        when(this.patientXDoc.getXObject(this.collaboratorDocRef, COLLABORATOR, JOHN_DOE, false)).thenReturn(this.collaborator1BaseObj);

        final boolean ownerSet = this.component.setOwner(this.patient, this.collaborator1DocRef);

        verify(this.patientXDoc, times(1)).getXObject(this.collaboratorDocRef, COLLABORATOR, JOHN_DOE, false);
        verify(this.patientXDoc, times(1)).removeXObject(this.collaborator1BaseObj);
        verify(this.patientXDoc, never()).newXObject(eq(this.collaboratorDocRef), eq(this.context));
        verify(this.wiki, times(1)).saveDocument(this.patientXDoc, "Set owner: " + JOHN_DOE, true, this.context);
        Assert.assertTrue(ownerSet);
    }

    @Test
    public void setOwnerPreviousOwnerIsNotNullOneCollaboratorIsAddedAndOneDeleted() throws XWikiException
    {
        when(this.partialEntityResolver.resolve(Owner.CLASS_REFERENCE, this.patientDocRef)).thenReturn(this.collaborator2DocRef);
        when(this.permissionsHelper.getStringProperty(this.patientXDoc, this.collaborator2DocRef, OWNER)).thenReturn(JANE_DOE);
        when(this.stringEntityResolver.resolve(JANE_DOE, this.patientDocRef)).thenReturn(this.collaborator2DocRef);
        when(this.partialEntityResolver.resolve(Collaborator.CLASS_REFERENCE, this.patientDocRef)).thenReturn(this.collaboratorDocRef);
        when(this.partialEntityResolver.resolve(this.collaborator1DocRef)).thenReturn(this.collaborator1DocRef);
        when(this.partialEntityResolver.resolve(this.collaborator2DocRef)).thenReturn(this.collaborator2DocRef);
        when(this.referenceSerializer.serialize(this.collaborator1DocRef)).thenReturn(JOHN_DOE);
        when(this.referenceSerializer.serialize(this.collaborator2DocRef)).thenReturn(JANE_DOE);
        when(this.patientXDoc.getXObject(this.collaboratorDocRef, COLLABORATOR, JOHN_DOE, false)).thenReturn(this.collaborator1BaseObj);

        final boolean ownerSet = this.component.setOwner(this.patient, this.collaborator1DocRef);

        verify(this.patientXDoc, times(1)).getXObject(this.collaboratorDocRef, COLLABORATOR, JOHN_DOE, false);
        verify(this.patientXDoc, times(1)).getXObject(this.collaboratorDocRef, COLLABORATOR, JANE_DOE, false);
        verify(this.patientXDoc, times(1)).removeXObject(this.collaborator1BaseObj);
        verify(this.patientXDoc, times(1)).newXObject(eq(this.collaboratorDocRef), eq(this.context));
        verify(this.wiki, times(1)).saveDocument(this.patientXDoc, "Set owner: " + JOHN_DOE, true, this.context);
        Assert.assertTrue(ownerSet);
    }

    @Test
    public void getCollaboratorsEntityHasNoDocumentReferenceResultsInEmptyCollection()
    {
        when(this.patient.getDocumentReference()).thenReturn(null);

        final Collection<Collaborator> result = this.component.getCollaborators(this.patient);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void getCollaboratorsNullsAreFilteredOut()
    {
        when(this.patientXDoc.getXObjects(this.collaboratorDocRef)).thenReturn(Arrays.asList(null, null, null));

        final Collection<Collaborator> result = this.component.getCollaborators(this.patient);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void getCollaboratorsCollaboratorsWithNullAccessAreFilteredOut()
    {
        when(this.collaborator4BaseObj.getStringValue(ACCESS)).thenReturn(null);
        when(this.patientXDoc.getXObjects(this.collaboratorDocRef)).thenReturn(Arrays.asList(this.collaborator1BaseObj,
            null, this.collaborator3BaseObj, this.collaborator4BaseObj));

        final Collection<Collaborator> result = this.component.getCollaborators(this.patient);
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.contains(new DefaultCollaborator(this.collaborator1DocRef, this.viewAccess,
            this.permissionsHelper)));
        Assert.assertTrue(result.contains(new DefaultCollaborator(this.collaborator3DocRef, this.editAccess,
            this.permissionsHelper)));
    }

    @Test
    public void getCollaboratorsCollaboratorsWithEmptyAccessAreFilteredOut()
    {
        when(this.collaborator4BaseObj.getStringValue(ACCESS)).thenReturn(StringUtils.EMPTY);
        when(this.patientXDoc.getXObjects(this.collaboratorDocRef)).thenReturn(Arrays.asList(this.collaborator1BaseObj,
            null, this.collaborator3BaseObj, this.collaborator4BaseObj));

        final Collection<Collaborator> result = this.component.getCollaborators(this.patient);
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.contains(new DefaultCollaborator(this.collaborator1DocRef, this.viewAccess,
            this.permissionsHelper)));
        Assert.assertTrue(result.contains(new DefaultCollaborator(this.collaborator3DocRef, this.editAccess,
            this.permissionsHelper)));
    }

    @Test
    public void getCollaboratorsCollaboratorsWithBlankAccessAreFilteredOut()
    {
        when(this.collaborator4BaseObj.getStringValue(ACCESS)).thenReturn(StringUtils.SPACE);
        when(this.patientXDoc.getXObjects(this.collaboratorDocRef)).thenReturn(Arrays.asList(this.collaborator1BaseObj,
            null, this.collaborator3BaseObj, this.collaborator4BaseObj));

        final Collection<Collaborator> result = this.component.getCollaborators(this.patient);
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.contains(new DefaultCollaborator(this.collaborator1DocRef, this.viewAccess,
            this.permissionsHelper)));
        Assert.assertTrue(result.contains(new DefaultCollaborator(this.collaborator3DocRef, this.editAccess,
            this.permissionsHelper)));
    }

    @Test
    public void getCollaboratorsCollaboratorsWithInvalidAccessAreNotReturned()
    {
        when(this.collaborator3BaseObj.getStringValue(ACCESS)).thenReturn("wrong");
        when(this.patientXDoc.getXObjects(this.collaboratorDocRef)).thenReturn(Arrays.asList(this.collaborator1BaseObj,
            null, this.collaborator3BaseObj));

        final Collection<Collaborator> result = this.component.getCollaborators(this.patient);
        Assert.assertEquals(1, result.size());
        Assert.assertTrue(result.contains(new DefaultCollaborator(this.collaborator1DocRef, this.viewAccess,
            this.permissionsHelper)));
    }

    @Test
    public void getCollaboratorsCollaboratorsWithNullNameAreFilteredOut()
    {
        when(this.collaborator4BaseObj.getStringValue(COLLABORATOR)).thenReturn(null);
        when(this.patientXDoc.getXObjects(this.collaboratorDocRef)).thenReturn(Arrays.asList(this.collaborator1BaseObj,
            null, this.collaborator3BaseObj, this.collaborator4BaseObj));

        final Collection<Collaborator> result = this.component.getCollaborators(this.patient);
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.contains(new DefaultCollaborator(this.collaborator1DocRef, this.viewAccess,
            this.permissionsHelper)));
        Assert.assertTrue(result.contains(new DefaultCollaborator(this.collaborator3DocRef, this.editAccess,
            this.permissionsHelper)));
    }

    @Test
    public void getCollaboratorsCollaboratorsWithEmptyNameAreFilteredOut()
    {
        when(this.collaborator4BaseObj.getStringValue(COLLABORATOR)).thenReturn(StringUtils.EMPTY);
        when(this.patientXDoc.getXObjects(this.collaboratorDocRef)).thenReturn(Arrays.asList(this.collaborator1BaseObj,
            null, this.collaborator3BaseObj, this.collaborator4BaseObj));

        final Collection<Collaborator> result = this.component.getCollaborators(this.patient);
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.contains(new DefaultCollaborator(this.collaborator1DocRef, this.viewAccess,
            this.permissionsHelper)));
        Assert.assertTrue(result.contains(new DefaultCollaborator(this.collaborator3DocRef, this.editAccess,
            this.permissionsHelper)));
    }

    @Test
    public void getCollaboratorsCollaboratorsWithBlankNameAreFilteredOut()
    {
        when(this.collaborator4BaseObj.getStringValue(COLLABORATOR)).thenReturn(StringUtils.SPACE);
        when(this.patientXDoc.getXObjects(this.collaboratorDocRef)).thenReturn(Arrays.asList(this.collaborator1BaseObj,
            null, this.collaborator3BaseObj, this.collaborator4BaseObj));

        final Collection<Collaborator> result = this.component.getCollaborators(this.patient);
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.contains(new DefaultCollaborator(this.collaborator1DocRef, this.viewAccess,
            this.permissionsHelper)));
        Assert.assertTrue(result.contains(new DefaultCollaborator(this.collaborator3DocRef, this.editAccess,
            this.permissionsHelper)));
    }

    @Test
    public void getCollaboratorsMostPermissiveAccessLevelIsSelected()
    {
        when(this.collaborator4BaseObj.getStringValue(COLLABORATOR)).thenReturn(COLLABORATOR_1);
        when(this.collaborator4BaseObj.getStringValue(ACCESS)).thenReturn(MANAGE);

        when(this.patientXDoc.getXObjects(this.collaboratorDocRef)).thenReturn(Arrays.asList(this.collaborator1BaseObj,
            null, this.collaborator3BaseObj, this.collaborator4BaseObj));

        final Collection<Collaborator> result = this.component.getCollaborators(this.patient);
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.contains(new DefaultCollaborator(this.collaborator1DocRef, this.manageAccess,
            this.permissionsHelper)));
        Assert.assertTrue(result.contains(new DefaultCollaborator(this.collaborator3DocRef, this.editAccess,
            this.permissionsHelper)));
    }

    @Test
    public void getCollaboratorsUnexpectedExceptionsAreLoggedAndEmptyCollaboratorsAreReturned()
    {
        when(this.patient.getXDocument()).thenThrow(new NullPointerException());

        final Collection<Collaborator> result = this.component.getCollaborators(this.patient);
        Assert.assertTrue(result.isEmpty());
        verify(this.logger, times(1)).error("Unexpected exception occurred when retrieving collaborators for entity "
            + "[{}]", this.patient);
    }

    @Test
    public void setCollaboratorsIgnoresAnyNullCollaborators() throws XWikiException
    {
        when(this.patientXDoc.newXObject(this.collaboratorDocRef, this.context)).thenReturn(this.collaborator1BaseObj,
            this.collaborator3BaseObj);
        final Collection<Collaborator> newCollaborators = Arrays.asList(this.collaborator1, null, this.collaborator3);
        final boolean isSet = this.component.setCollaborators(this.patient, newCollaborators);
        verify(this.patientXDoc, times(1)).removeXObjects(this.collaboratorDocRef);
        verify(this.patientXDoc, times(2)).newXObject(this.collaboratorDocRef, this.context);
        Assert.assertTrue(isSet);
    }

    @Test
    public void setCollaboratorsIgnoresAnyCollaboratorsWithInvalidName() throws XWikiException
    {
        when(this.patientXDoc.newXObject(this.collaboratorDocRef, this.context)).thenReturn(this.collaborator1BaseObj,
            this.collaborator3BaseObj);
        final Collection<Collaborator> newCollaborators = Arrays.asList(this.collaborator1,
            new DefaultCollaborator(null, this.viewAccess, this.permissionsHelper));
        final boolean isSet = this.component.setCollaborators(this.patient, newCollaborators);
        verify(this.patientXDoc, times(1)).removeXObjects(this.collaboratorDocRef);
        verify(this.patientXDoc, times(1)).newXObject(this.collaboratorDocRef, this.context);
        Assert.assertTrue(isSet);
    }

    @Test
    public void setCollaboratorsExceptionsAreCaught() throws XWikiException
    {
        when(this.patientXDoc.newXObject(this.collaboratorDocRef, this.context)).thenThrow(new XWikiException())
            .thenReturn( this.collaborator3BaseObj);
        final Collection<Collaborator> newCollaborators = Arrays.asList(this.collaborator1, this.collaborator3);
        final boolean isSet = this.component.setCollaborators(this.patient, newCollaborators);
        verify(this.patientXDoc, times(1)).removeXObjects(this.collaboratorDocRef);
        verify(this.patientXDoc, times(2)).newXObject(this.collaboratorDocRef, this.context);
        verify(this.logger, times(1)).error("Unexpected exception occurred when setting "
                + "properties for collaborator [{}]", collaborator1);
        Assert.assertTrue(isSet);
    }

    @Test
    public void setCollaboratorsExceptionsAreCaught2() throws XWikiException
    {
        when(this.patientXDoc.newXObject(this.collaboratorDocRef, this.context)).thenReturn(this.collaborator1BaseObj, this.collaborator3BaseObj);
        when(this.patient.getXDocument()).thenThrow(new NullPointerException());
        final Collection<Collaborator> newCollaborators = Arrays.asList(this.collaborator1, null, this.collaborator3);
        final boolean isSet = this.component.setCollaborators(this.patient, newCollaborators);
        verify(this.logger, times(1)).error("Unexpected exception occurred when setting "
            + "collaborators [{}] for entity [{}]", newCollaborators, this.patient);
        Assert.assertFalse(isSet);
    }

    @Test
    public void addCollaboratorReturnsFalseIfCollaboratorIsNull() throws XWikiException
    {
        final boolean isSet = this.component.addCollaborator(this.patient, null);
        verify(this.wiki, never()).saveDocument(any(), anyString(), anyBoolean(), any());
        Assert.assertFalse(isSet);
    }

    @Test
    public void addCollaboratorExceptionsAreCaughtAndLogged() throws XWikiException
    {
        when(this.patient.getXDocument()).thenThrow(new NullPointerException());
        final boolean isSet = this.component.addCollaborator(this.patient, this.collaborator1);
        verify(this.wiki, never()).saveDocument(any(), anyString(), anyBoolean(), any());
        verify(this.logger, times(1)).error("Unexpected exception occurred when adding a "
            + "collaborator [{}]", this.collaborator1);
        Assert.assertFalse(isSet);
    }

    @Test
    public void addCollaboratorCollaboratorAlreadyExistsUpdatesItsValues() throws XWikiException
    {
        when(this.patientXDoc.getXObject(this.collaboratorDocRef, COLLABORATOR, JOHN_DOE, false))
            .thenReturn(this.collaborator1BaseObj);
        when(this.partialEntityResolver.resolve(this.collaborator1DocRef)).thenReturn(this.collaborator1DocRef);
        when(this.referenceSerializer.serialize(this.collaborator1DocRef)).thenReturn(JOHN_DOE);
        final boolean isSet = this.component.addCollaborator(this.patient, this.collaborator1);

        verify(this.collaborator1BaseObj, times(1)).setStringValue(COLLABORATOR, JOHN_DOE);
        verify(this.collaborator1BaseObj, times(1)).setStringValue(ACCESS, EDIT);

        verify(this.patientXDoc, times(1)).getXObject(this.collaboratorDocRef, COLLABORATOR, JOHN_DOE, false);
        verify(this.patientXDoc, never()).newXObject(any(), any());

        verify(this.patientXDoc, times(1)).setAuthorReference(this.permissionsHelper.getCurrentUser());
        verify(this.patientXDoc, times(1)).setMetaDataDirty(true);
        verify(this.wiki, times(1)).saveDocument(this.patientXDoc, "Added collaborator: " + JOHN_DOE, true,
            this.context);
        Assert.assertTrue(isSet);
    }

    @Test
    public void addCollaboratorNewCollaboratorAddedIfDoesNotAlreadyExist() throws XWikiException
    {
        when(this.patientXDoc.getXObject(this.collaboratorDocRef, COLLABORATOR, JOHN_DOE, false)).thenReturn(null);
        when(this.patientXDoc.newXObject(this.collaboratorDocRef, this.context)).thenReturn(this.collaborator1BaseObj);
        when(this.partialEntityResolver.resolve(this.collaborator1DocRef)).thenReturn(this.collaborator1DocRef);
        when(this.referenceSerializer.serialize(this.collaborator1DocRef)).thenReturn(JOHN_DOE);
        final boolean isSet = this.component.addCollaborator(this.patient, this.collaborator1);

        verify(this.collaborator1BaseObj, times(1)).setStringValue(COLLABORATOR, JOHN_DOE);
        verify(this.collaborator1BaseObj, times(1)).setStringValue(ACCESS, EDIT);

        verify(this.patientXDoc, times(1)).getXObject(this.collaboratorDocRef, COLLABORATOR, JOHN_DOE, false);
        verify(this.patientXDoc, times(1)).newXObject(this.collaboratorDocRef, this.context);

        verify(this.patientXDoc, times(1)).setAuthorReference(this.permissionsHelper.getCurrentUser());
        verify(this.patientXDoc, times(1)).setMetaDataDirty(true);
        verify(this.wiki, times(1)).saveDocument(this.patientXDoc, "Added collaborator: " + JOHN_DOE, true,
            this.context);
        Assert.assertTrue(isSet);
    }

    @Test
    public void removeCollaboratorReturnsFalseIfCollaboratorIsNull() throws XWikiException
    {
        final boolean isRemoved = this.component.removeCollaborator(this.patient, null);
        verify(this.wiki, never()).saveDocument(any(), anyString(), anyBoolean(), any());
        Assert.assertFalse(isRemoved);
    }

    @Test
    public void removeCollaboratorExceptionsAreCaughtAndLogged() throws XWikiException
    {
        when(this.patient.getXDocument()).thenThrow(new NullPointerException());
        final boolean isRemoved = this.component.removeCollaborator(this.patient, this.collaborator1);
        verify(this.wiki, never()).saveDocument(any(), anyString(), anyBoolean(), any());
        verify(this.logger, times(1)).error("Unexpected exception occurred when removing a "
            + "collaborator [{}]", this.collaborator1);
        Assert.assertFalse(isRemoved);
    }

    @Test
    public void removeCollaboratorReturnsFalseIfCollaboratorBaseObjectDoesNotExist() throws XWikiException
    {
        when(this.patientXDoc.getXObject(this.collaboratorDocRef, COLLABORATOR, JOHN_DOE, false)).thenReturn(null);
        when(this.partialEntityResolver.resolve(this.collaborator1DocRef)).thenReturn(this.collaborator1DocRef);
        when(this.referenceSerializer.serialize(this.collaborator1DocRef)).thenReturn(JOHN_DOE);
        final boolean isRemoved = this.component.removeCollaborator(this.patient, this.collaborator1);
        verify(this.wiki, never()).saveDocument(any(), anyString(), anyBoolean(), any());
        Assert.assertFalse(isRemoved);
    }

    @Test
    public void removeCollaboratorRemovesExistingCollaboratorSuccessfully() throws XWikiException
    {
        when(this.patientXDoc.getXObject(this.collaboratorDocRef, COLLABORATOR, JOHN_DOE, false))
            .thenReturn(this.collaborator1BaseObj);
        when(this.partialEntityResolver.resolve(this.collaborator1DocRef)).thenReturn(this.collaborator1DocRef);
        when(this.referenceSerializer.serialize(this.collaborator1DocRef)).thenReturn(JOHN_DOE);
        final boolean isRemoved = this.component.removeCollaborator(this.patient, this.collaborator1);
        verify(this.patientXDoc, times(1)).removeXObject(this.collaborator1BaseObj);
        verify(this.wiki, times(1)).saveDocument(this.patientXDoc, "Removed collaborator: " + JOHN_DOE, true,
            this.context);
        Assert.assertTrue(isRemoved);
    }
}
