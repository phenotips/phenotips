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
import org.phenotips.data.permissions.PatientAccess;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.data.permissions.internal.access.EditAccessLevel;
import org.phenotips.data.permissions.internal.access.NoAccessLevel;
import org.phenotips.data.permissions.internal.access.OwnerAccessLevel;
import org.phenotips.data.permissions.internal.access.ViewAccessLevel;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.user.api.XWikiGroupService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for the default {@link PatientAccessHelper} implementation, {@link DefaultPatientAccessHelper}.
 *
 * @version $Id$
 */
public class DefaultPatientAccessHelperTest
{
    /** The patient used for tests. */
    private static final DocumentReference PATIENT_REFERENCE = new DocumentReference("xwiki", "data", "P0000001");

    private Patient patient = mock(Patient.class);

    /** The user used as the owner of the patient. */
    private static final DocumentReference OWNER = new DocumentReference("xwiki", "XWiki", "padams");

    private static final EntityReference RELATIVE_OWNER =
        new EntityReference("padams", EntityType.DOCUMENT, Patient.DEFAULT_DATA_SPACE);

    private static final String OWNER_STR = "xwiki:XWiki.padams";

    /** The user used as a collaborator. */
    private static final DocumentReference COLLABORATOR = new DocumentReference("xwiki", "XWiki", "hmccoy");

    private static final EntityReference RELATIVE_COLLABORATOR =
        new EntityReference("hmccoy", EntityType.DOCUMENT, Patient.DEFAULT_DATA_SPACE);

    private static final String COLLABORATOR_STR = "xwiki:XWiki.hmccoy";

    /** The user used as a non-collaborator. */
    private static final DocumentReference OTHER_USER = new DocumentReference("xwiki", "XWiki", "cxavier");

    private static final String OTHER_USER_STR = "xwiki:XWiki.cxavier";

    private static final DocumentReference GROUP_REFERENCE = new DocumentReference("xwiki", "Groups", "group");

    private static final EntityReference GROUP_CLASS = new EntityReference("XWikiGroups", EntityType.DOCUMENT,
        new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE));

    private static final EntityReference USER_CLASS = new EntityReference("XWikiUsers", EntityType.DOCUMENT,
        new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE));

    /** Group used as collaborator. */
    private static final DocumentReference GROUP = new DocumentReference("xwiki", "XWiki", "collaborators");

    private static final String GROUP_STR = "xwiki:XWiki.collaborators";

    private static final DocumentReference OWNER_CLASS = new DocumentReference("xwiki", "PhenoTips", "Owner");

    private static final DocumentReference VISIBILITY_CLASS = new DocumentReference("xwiki", "PhenoTips", "Visibility");

    private static final DocumentReference COLLABORATOR_CLASS = new DocumentReference("xwiki", "PhenoTips",
        "Collaborator");

    @Rule
    public final MockitoComponentMockingRule<PatientAccessHelper> mocker =
        new MockitoComponentMockingRule<PatientAccessHelper>(DefaultPatientAccessHelper.class);

    private ParameterizedType entityResolverType = new DefaultParameterizedType(null, DocumentReferenceResolver.class,
        EntityReference.class);

    private DocumentReferenceResolver<EntityReference> partialEntityResolver;

    private ParameterizedType stringResolverType = new DefaultParameterizedType(null, DocumentReferenceResolver.class,
        String.class);

    private DocumentReferenceResolver<String> stringEntityResolver;

    private ParameterizedType stringSerializerType = new DefaultParameterizedType(null,
        EntityReferenceSerializer.class, String.class);

    private EntityReferenceSerializer<String> stringEntitySerializer;

    private DocumentAccessBridge bridge;

    private XWikiContext context;

    private AuthorizationManager rights;

    @Before
    public void setup() throws ComponentLookupException
    {
        this.bridge = this.mocker.getInstance(DocumentAccessBridge.class);
        this.partialEntityResolver = this.mocker.getInstance(this.entityResolverType, "currentmixed");
        this.stringEntityResolver = this.mocker.getInstance(this.stringResolverType, "currentmixed");
        this.stringEntitySerializer = this.mocker.getInstance(this.stringSerializerType);
        this.rights = this.mocker.getInstance(AuthorizationManager.class);

        when(this.partialEntityResolver.resolve(Owner.CLASS_REFERENCE, PATIENT_REFERENCE)).thenReturn(
            OWNER_CLASS);
        when(this.partialEntityResolver.resolve(Visibility.CLASS_REFERENCE, PATIENT_REFERENCE)).thenReturn(
            VISIBILITY_CLASS);
        when(this.partialEntityResolver.resolve(Collaborator.CLASS_REFERENCE, PATIENT_REFERENCE)).thenReturn(
            COLLABORATOR_CLASS);

        when(this.partialEntityResolver.resolve(OWNER)).thenReturn(OWNER);
        when(this.partialEntityResolver.resolve(RELATIVE_OWNER)).thenReturn(OWNER);
        when(this.partialEntityResolver.resolve(COLLABORATOR)).thenReturn(COLLABORATOR);
        when(this.partialEntityResolver.resolve(RELATIVE_COLLABORATOR)).thenReturn(COLLABORATOR);

        when(this.stringEntityResolver.resolve(OWNER_STR)).thenReturn(OWNER);
        when(this.stringEntityResolver.resolve(OWNER_STR, PATIENT_REFERENCE)).thenReturn(OWNER);
        when(this.stringEntityResolver.resolve(COLLABORATOR_STR, PATIENT_REFERENCE)).thenReturn(COLLABORATOR);
        when(this.stringEntityResolver.resolve(OTHER_USER_STR, PATIENT_REFERENCE)).thenReturn(OTHER_USER);
        when(this.stringEntityResolver.resolve(GROUP_STR, PATIENT_REFERENCE)).thenReturn(GROUP);

        when(this.stringEntitySerializer.serialize(OWNER)).thenReturn(OWNER_STR);
        when(this.stringEntitySerializer.serialize(COLLABORATOR)).thenReturn(COLLABORATOR_STR);
        when(this.stringEntitySerializer.serialize(OTHER_USER)).thenReturn(OTHER_USER_STR);

        when(this.patient.getDocument()).thenReturn(PATIENT_REFERENCE);
        when(this.bridge.getProperty(PATIENT_REFERENCE, OWNER_CLASS, "owner")).thenReturn(OWNER_STR);

        Execution e = this.mocker.getInstance(Execution.class);
        ExecutionContext ec = mock(ExecutionContext.class);
        when(e.getContext()).thenReturn(ec);
        this.context = mock(XWikiContext.class);
        when(ec.getProperty("xwikicontext")).thenReturn(this.context);
    }

    /** Basic tests for {@link PatientAccessHelper#getCurrentUser()}. */
    @Test
    public void getCurrentUser() throws ComponentLookupException
    {
        when(this.bridge.getCurrentUserReference()).thenReturn(OWNER);
        Assert.assertSame(OWNER, this.mocker.getComponentUnderTest().getCurrentUser());
    }

    /** Basic tests for {@link PatientAccessHelper#isAdministrator(Patient, DocumentReference)} for non-admin user. */
    @Test
    public void isAdministratorForNonAdminUser() throws Exception
    {
        final XWikiDocument xwikiDoc = mock(XWikiDocument.class);
        final BaseObject userObject = mock(BaseObject.class);
        when(this.bridge.getDocument(OWNER)).thenReturn(xwikiDoc);
        when(xwikiDoc.getXObject(USER_CLASS)).thenReturn(userObject);
        when(this.rights.hasAccess(Right.ADMIN, OWNER, PATIENT_REFERENCE)).thenReturn(false);
        Assert.assertFalse(this.mocker.getComponentUnderTest().isAdministrator(this.patient, OWNER));
        verify(xwikiDoc, times(1)).getXObject(USER_CLASS);
        verify(xwikiDoc, never()).getXObject(GROUP_CLASS);
    }

    /** Basic tests for {@link PatientAccessHelper#isAdministrator(Patient, DocumentReference)} for admin user. */
    @Test
    public void isAdministratorForAdminUser() throws Exception
    {
        final XWikiDocument xwikiDoc = mock(XWikiDocument.class);
        final BaseObject userObject = mock(BaseObject.class);
        when(this.bridge.getDocument(OWNER)).thenReturn(xwikiDoc);
        when(xwikiDoc.getXObject(USER_CLASS)).thenReturn(userObject);
        when(this.rights.hasAccess(Right.ADMIN, OWNER, PATIENT_REFERENCE)).thenReturn(true);
        Assert.assertTrue(this.mocker.getComponentUnderTest().isAdministrator(this.patient, OWNER));
        verify(xwikiDoc, times(1)).getXObject(USER_CLASS);
        verify(xwikiDoc, never()).getXObject(GROUP_CLASS);
    }

    /** Basic tests for {@link PatientAccessHelper#isAdministrator(Patient, DocumentReference)} for group. */
    @Test
    public void isAdministratorForGroup() throws Exception
    {
        final XWikiDocument xwikiDoc = mock(XWikiDocument.class);
        final BaseObject groupObject = mock(BaseObject.class);
        when(this.bridge.getDocument(GROUP_REFERENCE)).thenReturn(xwikiDoc);
        when(xwikiDoc.getXObject(GROUP_CLASS)).thenReturn(groupObject);
        Assert.assertFalse(this.mocker.getComponentUnderTest().isAdministrator(this.patient, GROUP_REFERENCE));
        verifyZeroInteractions(this.rights);
        verify(xwikiDoc, times(1)).getXObject(USER_CLASS);
        verify(xwikiDoc, times(1)).getXObject(GROUP_CLASS);
    }

    /** {@link PatientAccessHelper#getCurrentUser()} returns null for guests. */
    @Test
    public void getCurrentUserForGuest() throws ComponentLookupException
    {
        when(this.bridge.getCurrentUserReference()).thenReturn(null);
        Assert.assertNull(this.mocker.getComponentUnderTest().getCurrentUser());
    }

    /** Basic tests for {@link PatientAccessHelper#getOwner(Patient)}. */
    @Test
    public void getOwner() throws ComponentLookupException
    {
        Assert.assertSame(OWNER, this.mocker.getComponentUnderTest().getOwner(this.patient).getUser());
    }

    /** {@link PatientAccessHelper#getOwner(Patient)} returns a null user when the owner isn't specified. */
    @Test
    public void getOwnerWithMissingOwnerAndReferrer() throws ComponentLookupException
    {
        when(this.bridge.getProperty(PATIENT_REFERENCE, OWNER_CLASS, "owner")).thenReturn(null);
        Assert.assertNull(this.mocker.getComponentUnderTest().getOwner(this.patient).getUser());

        when(this.bridge.getProperty(PATIENT_REFERENCE, OWNER_CLASS, "owner")).thenReturn("");
        Assert.assertNull(this.mocker.getComponentUnderTest().getOwner(this.patient).getUser());

        Mockito.verify(this.patient, Mockito.never()).getReporter();
    }

    /** {@link PatientAccessHelper#getOwner(Patient)} returns {@code null} when the patient is missing. */
    @Test
    public void getOwnerWithMissingPatient() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().getOwner(null));
    }

    /** {@link PatientAccessHelper#getOwner(Patient)} returns {@code null} when the patient is missing. */
    @Test
    public void getOwnerWithMissingDocument() throws ComponentLookupException
    {
        when(this.patient.getDocument()).thenReturn(null);
        Assert.assertNull(this.mocker.getComponentUnderTest().getOwner(this.patient));
    }

    /** Basic tests for {@link PatientAccessHelper#setOwner(Patient, EntityReference)}. */
    @Test
    public void setOwner() throws Exception
    {
        Assert.assertTrue(this.mocker.getComponentUnderTest().setOwner(this.patient, OWNER));
        Mockito.verify(this.bridge).setProperty(PATIENT_REFERENCE, OWNER_CLASS, "owner", OWNER_STR);
    }

    /** Basic tests for {@link PatientAccessHelper#setOwner(Patient, EntityReference)}. */
    @Test
    public void setOwnerWithFailure() throws Exception
    {
        Mockito.doThrow(new Exception()).when(this.bridge)
            .setProperty(PATIENT_REFERENCE, OWNER_CLASS, "owner", OWNER_STR);
        Assert.assertFalse(this.mocker.getComponentUnderTest().setOwner(this.patient, OWNER));
    }

    /** Basic tests for {@link PatientAccessHelper#getVisibility(Patient)}. */
    @Test
    public void getVisibility() throws ComponentLookupException
    {
        when(this.bridge.getProperty(PATIENT_REFERENCE, VISIBILITY_CLASS, "visibility")).thenReturn("public");
        PermissionsManager manager = this.mocker.getInstance(PermissionsManager.class);
        Visibility publicV = mock(Visibility.class);
        when(manager.resolveVisibility("public")).thenReturn(publicV);
        Assert.assertSame(publicV, this.mocker.getComponentUnderTest().getVisibility(this.patient));
    }

    /** {@link PatientAccessHelper#getVisibility(Patient)} returns null when the owner isn't specified. */
    @Test
    public void getVisibilityWithMissingVisibility() throws ComponentLookupException
    {
        when(this.bridge.getProperty(PATIENT_REFERENCE, VISIBILITY_CLASS, "visibility")).thenReturn(null);
        Assert.assertNull(this.mocker.getComponentUnderTest().getVisibility(this.patient));

        when(this.bridge.getProperty(PATIENT_REFERENCE, VISIBILITY_CLASS, "visibility")).thenReturn("");
        Assert.assertNull(this.mocker.getComponentUnderTest().getVisibility(this.patient));
    }

    /** Basic tests for {@link PatientAccessHelper#setOwner(Patient, EntityReference)}. */
    @Test
    public void setVisibility() throws Exception
    {
        Visibility publicV = mock(Visibility.class);
        when(publicV.getName()).thenReturn("public");
        Assert.assertTrue(this.mocker.getComponentUnderTest().setVisibility(this.patient, publicV));
        Mockito.verify(this.bridge).setProperty(PATIENT_REFERENCE, VISIBILITY_CLASS, "visibility", "public");
    }

    /** Basic tests for {@link PatientAccessHelper#setOwner(Patient, EntityReference)}. */
    @Test
    public void setVisibilityWithNullVisibility() throws Exception
    {
        Assert.assertTrue(this.mocker.getComponentUnderTest().setVisibility(this.patient, null));
        Mockito.verify(this.bridge).setProperty(PATIENT_REFERENCE, VISIBILITY_CLASS, "visibility", "");
    }

    /** Basic tests for {@link PatientAccessHelper#setVisibility(Patient, Visibility)}. */
    @Test
    public void setVisibilityWithFailure() throws Exception
    {
        Visibility publicV = mock(Visibility.class);
        when(publicV.getName()).thenReturn("public");
        Mockito.doThrow(new Exception()).when(this.bridge)
            .setProperty(PATIENT_REFERENCE, VISIBILITY_CLASS, "visibility", "public");
        Assert.assertFalse(this.mocker.getComponentUnderTest().setVisibility(this.patient, publicV));
    }

    /** Basic tests for {@link PatientAccessHelper#getCollaborators(Patient)}. */
    @Test
    public void getCollaborators() throws Exception
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        when(this.bridge.getDocument(PATIENT_REFERENCE)).thenReturn(doc);
        List<BaseObject> objects = new ArrayList<BaseObject>();
        BaseObject collaborator = mock(BaseObject.class);
        when(collaborator.getStringValue("collaborator")).thenReturn(COLLABORATOR_STR);
        when(collaborator.getStringValue("access")).thenReturn("edit");
        objects.add(collaborator);
        collaborator = mock(BaseObject.class);
        when(collaborator.getStringValue("collaborator")).thenReturn(OTHER_USER_STR);
        when(collaborator.getStringValue("access")).thenReturn("view");
        objects.add(collaborator);
        when(doc.getXObjects(COLLABORATOR_CLASS)).thenReturn(objects);
        PermissionsManager manager = this.mocker.getInstance(PermissionsManager.class);
        AccessLevel edit = mock(AccessLevel.class);
        when(manager.resolveAccessLevel("edit")).thenReturn(edit);
        AccessLevel view = mock(AccessLevel.class);
        when(manager.resolveAccessLevel("view")).thenReturn(view);
        Collection<Collaborator> collaborators = this.mocker.getComponentUnderTest().getCollaborators(this.patient);
        Assert.assertEquals(2, collaborators.size());
        Collaborator c = new DefaultCollaborator(COLLABORATOR, edit, this.mocker.getComponentUnderTest());
        Assert.assertTrue(collaborators.contains(c));
        c = new DefaultCollaborator(OTHER_USER, view, this.mocker.getComponentUnderTest());
        Assert.assertTrue(collaborators.contains(c));
    }

    /**
     * {@link PatientAccessHelper#getCollaborators(Patient)} returns the most permissive access level when multiple
     * entries are present.
     */
    @Test
    public void getCollaboratorsWithMultipleEntries() throws Exception
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        when(this.bridge.getDocument(PATIENT_REFERENCE)).thenReturn(doc);
        List<BaseObject> objects = new ArrayList<BaseObject>();
        BaseObject collaborator = mock(BaseObject.class);
        when(collaborator.getStringValue("collaborator")).thenReturn(COLLABORATOR_STR);
        when(collaborator.getStringValue("access")).thenReturn("edit");
        objects.add(collaborator);
        collaborator = mock(BaseObject.class);
        when(collaborator.getStringValue("collaborator")).thenReturn(COLLABORATOR_STR);
        when(collaborator.getStringValue("access")).thenReturn("view");
        objects.add(collaborator);
        collaborator = mock(BaseObject.class);
        when(collaborator.getStringValue("collaborator")).thenReturn(COLLABORATOR_STR);
        when(collaborator.getStringValue("access")).thenReturn("manage");
        objects.add(collaborator);
        when(doc.getXObjects(COLLABORATOR_CLASS)).thenReturn(objects);
        PermissionsManager manager = this.mocker.getInstance(PermissionsManager.class);
        AccessLevel edit = mock(AccessLevel.class);
        when(manager.resolveAccessLevel("edit")).thenReturn(edit);
        AccessLevel view = mock(AccessLevel.class);
        when(manager.resolveAccessLevel("view")).thenReturn(view);
        AccessLevel manage = mock(AccessLevel.class);
        when(manager.resolveAccessLevel("manage")).thenReturn(manage);
        when(view.compareTo(edit)).thenReturn(-10);
        when(manage.compareTo(edit)).thenReturn(10);
        Collection<Collaborator> collaborators = this.mocker.getComponentUnderTest().getCollaborators(this.patient);
        Assert.assertEquals(1, collaborators.size());
        Collaborator c = new DefaultCollaborator(COLLABORATOR, manage, this.mocker.getComponentUnderTest());
        Assert.assertTrue(collaborators.contains(c));
    }

    /** {@link PatientAccessHelper#getCollaborators(Patient)} skips objects with missing values. */
    @Test
    public void getCollaboratorsWithMissingValues() throws Exception
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        when(this.bridge.getDocument(PATIENT_REFERENCE)).thenReturn(doc);
        List<BaseObject> objects = new ArrayList<BaseObject>();
        BaseObject collaborator = mock(BaseObject.class);
        when(collaborator.getStringValue("collaborator")).thenReturn(COLLABORATOR_STR);
        when(collaborator.getStringValue("access")).thenReturn("");
        objects.add(collaborator);
        objects.add(null);
        collaborator = mock(BaseObject.class);
        when(collaborator.getStringValue("collaborator")).thenReturn("");
        when(collaborator.getStringValue("access")).thenReturn("view");
        objects.add(collaborator);
        collaborator = mock(BaseObject.class);
        when(collaborator.getStringValue("collaborator")).thenReturn(null);
        when(collaborator.getStringValue("access")).thenReturn(null);
        objects.add(collaborator);
        when(doc.getXObjects(COLLABORATOR_CLASS)).thenReturn(objects);
        Collection<Collaborator> collaborators = this.mocker.getComponentUnderTest().getCollaborators(this.patient);
        Assert.assertTrue(collaborators.isEmpty());
    }

    /** {@link PatientAccessHelper#getCollaborators(Patient)} returns an empty set when accessing the patient fails. */
    @Test
    public void getCollaboratorsWithException() throws Exception
    {
        when(this.bridge.getDocument(PATIENT_REFERENCE)).thenThrow(new Exception());
        Collection<Collaborator> collaborators = this.mocker.getComponentUnderTest().getCollaborators(this.patient);
        Assert.assertNotNull(collaborators);
        Assert.assertTrue(collaborators.isEmpty());
    }

    /** Basic tests for {@link PatientAccessHelper#setCollaborators(Patient, Collection)}. */
    @Test
    public void setCollaborators() throws Exception
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        when(this.bridge.getDocument(PATIENT_REFERENCE)).thenReturn(doc);
        PermissionsManager manager = this.mocker.getInstance(PermissionsManager.class);
        AccessLevel edit = mock(AccessLevel.class);
        when(edit.getName()).thenReturn("edit");
        when(manager.resolveAccessLevel("edit")).thenReturn(edit);
        AccessLevel view = mock(AccessLevel.class);
        when(view.getName()).thenReturn("view");
        when(manager.resolveAccessLevel("view")).thenReturn(view);
        Collection<Collaborator> collaborators = new HashSet<Collaborator>();
        Collaborator c = new DefaultCollaborator(COLLABORATOR, edit, this.mocker.getComponentUnderTest());
        collaborators.add(c);
        c = new DefaultCollaborator(OTHER_USER, view, this.mocker.getComponentUnderTest());
        collaborators.add(c);
        BaseObject o = mock(BaseObject.class);
        when(doc.newXObject(COLLABORATOR_CLASS, this.context)).thenReturn(o);
        XWiki xwiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(xwiki);

        Assert.assertTrue(this.mocker.getComponentUnderTest().setCollaborators(this.patient, collaborators));
        Mockito.verify(o).setStringValue("collaborator", COLLABORATOR_STR);
        Mockito.verify(o).setStringValue("access", "edit");
        Mockito.verify(o).setStringValue("collaborator", OTHER_USER_STR);
        Mockito.verify(o).setStringValue("access", "view");
        Mockito.verify(doc).removeXObjects(COLLABORATOR_CLASS);
        Mockito.verify(xwiki).saveDocument(doc, "Updated collaborators", true, this.context);
    }

    /** {@link PatientAccessHelper#setCollaborators(Patient, Collection)} returns false when accessing the patient fails. */
    @Test
    public void setCollaboratorsWithFailure() throws Exception
    {
        Mockito.doThrow(new Exception()).when(this.bridge).getDocument(PATIENT_REFERENCE);
        Collection<Collaborator> collaborators = new HashSet<Collaborator>();
        Assert.assertFalse(this.mocker.getComponentUnderTest().setCollaborators(this.patient, collaborators));
    }

    /**
     * {@link PatientAccessHelper#addCollaborator(Patient, Collaborator) adds a new Collaborator object if one doesn't
     * exist already.
     */
    @Test
    public void addCollaboratorWithNewObject() throws Exception
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        when(this.bridge.getDocument(PATIENT_REFERENCE)).thenReturn(doc);
        PermissionsManager manager = this.mocker.getInstance(PermissionsManager.class);
        BaseObject o = mock(BaseObject.class);
        when(doc.getXObject(COLLABORATOR_CLASS, "collaborator", COLLABORATOR_STR, false)).thenReturn(null);
        when(doc.newXObject(COLLABORATOR_CLASS, this.context)).thenReturn(o);
        XWiki xwiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(xwiki);

        AccessLevel edit = mock(AccessLevel.class);
        when(edit.getName()).thenReturn("edit");
        when(manager.resolveAccessLevel("edit")).thenReturn(edit);
        Collaborator collaborator = new DefaultCollaborator(COLLABORATOR, edit, this.mocker.getComponentUnderTest());

        Assert.assertTrue(this.mocker.getComponentUnderTest().addCollaborator(this.patient, collaborator));
        Mockito.verify(o).setStringValue("collaborator", COLLABORATOR_STR);
        Mockito.verify(o).setStringValue("access", "edit");
        Mockito.verify(xwiki).saveDocument(doc, "Added collaborator: " + COLLABORATOR_STR, true, this.context);
    }

    /** {@link PatientAccessHelper#addCollaborator(Patient, Collaborator) modifies the existing Collaborator object. */
    @Test
    public void addCollaboratorWithExistingObject() throws Exception
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        when(this.bridge.getDocument(PATIENT_REFERENCE)).thenReturn(doc);
        PermissionsManager manager = this.mocker.getInstance(PermissionsManager.class);
        BaseObject o = mock(BaseObject.class);
        when(doc.getXObject(COLLABORATOR_CLASS, "collaborator", COLLABORATOR_STR, false)).thenReturn(o);
        XWiki xwiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(xwiki);

        AccessLevel edit = mock(AccessLevel.class);
        when(edit.getName()).thenReturn("edit");
        when(manager.resolveAccessLevel("edit")).thenReturn(edit);
        Collaborator collaborator = new DefaultCollaborator(COLLABORATOR, edit, this.mocker.getComponentUnderTest());

        Assert.assertTrue(this.mocker.getComponentUnderTest().addCollaborator(this.patient, collaborator));
        Mockito.verify(o).setStringValue("collaborator", COLLABORATOR_STR);
        Mockito.verify(o).setStringValue("access", "edit");
        Mockito.verify(xwiki).saveDocument(doc, "Added collaborator: " + COLLABORATOR_STR, true, this.context);
    }

    /**
     * {@link PatientAccessHelper#addCollaborator(Patient, Collaborator) returns false when accessing the document
     * fails.
     */
    @Test
    public void addCollaboratorWithFailure() throws Exception
    {
        Mockito.doThrow(new Exception()).when(this.bridge).getDocument(PATIENT_REFERENCE);

        AccessLevel edit = mock(AccessLevel.class);
        when(edit.getName()).thenReturn("edit");
        Collaborator collaborator = new DefaultCollaborator(COLLABORATOR, edit, this.mocker.getComponentUnderTest());

        Assert.assertFalse(this.mocker.getComponentUnderTest().addCollaborator(this.patient, collaborator));
    }

    /** {@link PatientAccessHelper#removeCollaborator(Patient, Collaborator) removes the existing Collaborator. */
    @Test
    public void removeCollaboratorWithExistingObject() throws Exception
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        when(this.bridge.getDocument(PATIENT_REFERENCE)).thenReturn(doc);
        BaseObject o = mock(BaseObject.class);
        when(doc.getXObject(COLLABORATOR_CLASS, "collaborator", COLLABORATOR_STR, false)).thenReturn(o);
        XWiki xwiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(xwiki);

        AccessLevel edit = mock(AccessLevel.class);
        Collaborator collaborator = new DefaultCollaborator(COLLABORATOR, edit, this.mocker.getComponentUnderTest());

        Assert.assertTrue(this.mocker.getComponentUnderTest().removeCollaborator(this.patient, collaborator));
        Mockito.verify(doc).removeXObject(o);
        Mockito.verify(xwiki).saveDocument(doc, "Removed collaborator: " + COLLABORATOR_STR, true, this.context);
    }

    /** {@link PatientAccessHelper#removeCollaborator(Patient, Collaborator) does nothing if the object isn't found. */
    @Test
    public void removeCollaboratorWithMissingObject() throws Exception
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        when(this.bridge.getDocument(PATIENT_REFERENCE)).thenReturn(doc);
        PermissionsManager manager = this.mocker.getInstance(PermissionsManager.class);
        when(doc.getXObject(COLLABORATOR_CLASS, "collaborator", COLLABORATOR_STR, false)).thenReturn(null);
        XWiki xwiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(xwiki);

        AccessLevel edit = mock(AccessLevel.class);
        when(edit.getName()).thenReturn("edit");
        when(manager.resolveAccessLevel("edit")).thenReturn(edit);
        Collaborator collaborator = new DefaultCollaborator(COLLABORATOR, edit, this.mocker.getComponentUnderTest());

        Assert.assertFalse(this.mocker.getComponentUnderTest().removeCollaborator(this.patient, collaborator));
        Mockito.verify(doc, Mockito.never()).removeXObject(Matchers.any(BaseObject.class));
        Mockito.verify(xwiki, Mockito.never()).saveDocument(doc, "Removed collaborator: " + COLLABORATOR_STR, true,
            this.context);
    }

    /**
     * {@link PatientAccessHelper#removeCollaborator(Patient, Collaborator) returns false when accessing the document
     * fails.
     */
    @Test
    public void removeCollaboratorWithFailure() throws Exception
    {
        Mockito.doThrow(new Exception()).when(this.bridge).getDocument(PATIENT_REFERENCE);

        AccessLevel edit = mock(AccessLevel.class);
        when(edit.getName()).thenReturn("edit");
        Collaborator collaborator = new DefaultCollaborator(COLLABORATOR, edit, this.mocker.getComponentUnderTest());

        Assert.assertFalse(this.mocker.getComponentUnderTest().removeCollaborator(this.patient, collaborator));
    }

    /** {@link PatientAccessHelper#getAccessLevel(Patient, EntityReference)} returns no access for guest users. */
    @Test
    public void getAccessLevelWithOwner() throws Exception
    {
        AccessLevel none = new NoAccessLevel();
        AccessLevel owner = new OwnerAccessLevel();
        PermissionsManager manager = this.mocker.getInstance(PermissionsManager.class);
        when(manager.resolveAccessLevel("none")).thenReturn(none);
        when(manager.resolveAccessLevel("owner")).thenReturn(owner);

        XWikiGroupService groupService = mock(XWikiGroupService.class);
        XWiki xwiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(xwiki);
        when(xwiki.getGroupService(this.context)).thenReturn(groupService);
        when(groupService.getAllGroupsReferencesForMember(COLLABORATOR, 0, 0, this.context))
            .thenReturn(Collections.<DocumentReference>emptyList());

        Assert.assertSame(owner, this.mocker.getComponentUnderTest().getAccessLevel(this.patient, OWNER));
    }

    /** {@link PatientAccessHelper#getAccessLevel(Patient, EntityReference)} returns no access for guest users. */
    @Test
    public void getAccessLevelWithGuestUser() throws ComponentLookupException
    {
        AccessLevel none = new NoAccessLevel();
        PermissionsManager manager = this.mocker.getInstance(PermissionsManager.class);
        when(manager.resolveAccessLevel("none")).thenReturn(none);
        Assert.assertSame(none, this.mocker.getComponentUnderTest().getAccessLevel(this.patient, null));
    }

    /** {@link PatientAccessHelper#getAccessLevel(Patient, EntityReference)} returns no access with missing patient. */
    @Test
    public void getAccessLevelWithMissingPatient() throws ComponentLookupException
    {
        AccessLevel none = new NoAccessLevel();
        PermissionsManager manager = this.mocker.getInstance(PermissionsManager.class);
        when(manager.resolveAccessLevel("none")).thenReturn(none);
        Assert.assertSame(none, this.mocker.getComponentUnderTest().getAccessLevel(null, OTHER_USER));
    }

    /**
     * {@link PatientAccessHelper#getAccessLevel(Patient, EntityReference)} returns no access with missing patient and
     * user.
     */
    @Test
    public void getAccessLevelWithMissingPatientAndGuestUser() throws ComponentLookupException
    {
        AccessLevel none = new NoAccessLevel();
        PermissionsManager manager = this.mocker.getInstance(PermissionsManager.class);
        when(manager.resolveAccessLevel("none")).thenReturn(none);
        Assert.assertSame(none, this.mocker.getComponentUnderTest().getAccessLevel(null, null));
    }

    /** {@link PatientAccess#getAccessLevel()} returns the specified access for a registered collaborator. */
    @Test
    public void getAccessLevelWithSpecifiedCollaborator() throws Exception
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        when(this.bridge.getDocument(PATIENT_REFERENCE)).thenReturn(doc);
        List<BaseObject> objects = new ArrayList<BaseObject>();
        BaseObject collaborator = mock(BaseObject.class);
        when(collaborator.getStringValue("collaborator")).thenReturn(COLLABORATOR_STR);
        when(collaborator.getStringValue("access")).thenReturn("edit");
        objects.add(collaborator);
        collaborator = mock(BaseObject.class);
        when(collaborator.getStringValue("collaborator")).thenReturn(OTHER_USER_STR);
        when(collaborator.getStringValue("access")).thenReturn("view");
        objects.add(collaborator);
        when(doc.getXObjects(COLLABORATOR_CLASS)).thenReturn(objects);
        PermissionsManager manager = this.mocker.getInstance(PermissionsManager.class);
        AccessLevel edit = new EditAccessLevel();
        when(manager.resolveAccessLevel("edit")).thenReturn(edit);
        AccessLevel view = new ViewAccessLevel();
        when(manager.resolveAccessLevel("view")).thenReturn(view);
        AccessLevel none = new NoAccessLevel();
        when(manager.resolveAccessLevel("none")).thenReturn(none);
        XWikiGroupService groupService = mock(XWikiGroupService.class);
        XWiki xwiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(xwiki);
        when(xwiki.getGroupService(this.context)).thenReturn(groupService);
        when(groupService.getAllGroupsReferencesForMember(COLLABORATOR, 0, 0, this.context))
            .thenReturn(Collections.<DocumentReference>emptyList());

        Assert.assertSame(edit, this.mocker.getComponentUnderTest().getAccessLevel(this.patient, COLLABORATOR));
    }

    /** {@link PatientAccess#getAccessLevel()} returns the specified access for a registered collaborator. */
    @Test
    public void getAccessLevelWithGroupMemberCollaborator() throws Exception
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        when(this.bridge.getDocument(PATIENT_REFERENCE)).thenReturn(doc);
        List<BaseObject> objects = new ArrayList<BaseObject>();
        BaseObject collaborator = mock(BaseObject.class);
        when(collaborator.getStringValue("collaborator")).thenReturn(GROUP_STR);
        when(collaborator.getStringValue("access")).thenReturn("edit");
        objects.add(collaborator);
        collaborator = mock(BaseObject.class);
        when(collaborator.getStringValue("collaborator")).thenReturn(OTHER_USER_STR);
        when(collaborator.getStringValue("access")).thenReturn("view");
        objects.add(collaborator);
        when(doc.getXObjects(COLLABORATOR_CLASS)).thenReturn(objects);
        PermissionsManager manager = this.mocker.getInstance(PermissionsManager.class);
        AccessLevel edit = new EditAccessLevel();
        when(manager.resolveAccessLevel("edit")).thenReturn(edit);
        AccessLevel view = new ViewAccessLevel();
        when(manager.resolveAccessLevel("view")).thenReturn(view);
        AccessLevel none = new NoAccessLevel();
        when(manager.resolveAccessLevel("none")).thenReturn(none);
        XWikiGroupService groupService = mock(XWikiGroupService.class);
        XWiki xwiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(xwiki);
        when(xwiki.getGroupService(this.context)).thenReturn(groupService);
        when(groupService.getAllGroupsReferencesForMember(COLLABORATOR, 0, 0, this.context))
            .thenReturn(Arrays.asList(GROUP));

        Assert.assertSame(edit, this.mocker.getComponentUnderTest().getAccessLevel(this.patient, COLLABORATOR));
    }

    /**
     * {@link PatientAccessHelper#getAccessLevel(Patient, EntityReference)} returns no access when XWiki throws
     * exceptions.
     */
    @Test
    public void getAccessLevelWithExceptions() throws ComponentLookupException, XWikiException
    {
        AccessLevel none = new NoAccessLevel();
        PermissionsManager manager = this.mocker.getInstance(PermissionsManager.class);
        when(manager.resolveAccessLevel("none")).thenReturn(none);
        XWiki xwiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(xwiki);
        when(xwiki.getGroupService(this.context)).thenThrow(new XWikiException());
        Assert.assertSame(none, this.mocker.getComponentUnderTest().getAccessLevel(this.patient, OTHER_USER));
    }

    /** Basic tests for {@link PatientAccessHelper#getType(EntityReference)}. */
    @Test
    public void getType() throws Exception
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        when(this.bridge.getDocument(OWNER)).thenReturn(doc);
        when(doc.getXObject(new EntityReference("XWikiUsers", EntityType.DOCUMENT,
            new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE)))).thenReturn(mock(BaseObject.class));

        doc = mock(XWikiDocument.class);
        when(this.bridge.getDocument(GROUP)).thenReturn(doc);
        when(doc.getXObject(new EntityReference("XWikiUsers", EntityType.DOCUMENT,
            new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE)))).thenReturn(null);
        when(doc.getXObject(new EntityReference("XWikiGroups", EntityType.DOCUMENT,
            new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE)))).thenReturn(mock(BaseObject.class));

        doc = mock(XWikiDocument.class);
        when(this.bridge.getDocument(COLLABORATOR)).thenReturn(doc);
        when(doc.getXObject(new EntityReference("XWikiUsers", EntityType.DOCUMENT,
            new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE)))).thenReturn(null);
        when(doc.getXObject(new EntityReference("XWikiGroups", EntityType.DOCUMENT,
            new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE)))).thenReturn(null);

        Assert.assertEquals("user", this.mocker.getComponentUnderTest().getType(OWNER));
        Assert.assertEquals("group", this.mocker.getComponentUnderTest().getType(GROUP));
        Assert.assertEquals("unknown", this.mocker.getComponentUnderTest().getType(COLLABORATOR));
    }
}
