package org.phenotips.data.rest.internal;

import static org.mockito.Mockito.*;

import com.xpn.xwiki.XWikiContext;
import net.sf.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.phenotips.data.PatientRepository;
import org.phenotips.data.rest.DomainObjectFactory;
import org.phenotips.data.rest.PatientsResource;
import org.phenotips.data.rest.model.Patient;
import org.phenotips.data.rest.model.Patients;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.net.URI;
import org.slf4j.Logger;

public class DefaultPatientsResourceImplTest {

    @Rule
    public MockitoComponentMockingRule<PatientsResource> mocker =
        new MockitoComponentMockingRule<PatientsResource>(DefaultPatientsResourceImpl.class);

    @Mock
    private User currentUser;

    @Mock
    private Logger logger;

    private PatientRepository repository;

    private QueryManager queries;
    private AuthorizationManager access;
    private UserManager users;

    private EntityReferenceResolver<EntityReference> currentResolver;

    private DomainObjectFactory factory;

    private Patient patient;
    private URI uri;
    private DefaultPatientsResourceImpl patientsResource;


    @Before
    public void setUp() throws ComponentLookupException {
        MockitoAnnotations.initMocks(this);
        Execution execution = mock(Execution.class);
        ExecutionContext executionContext = mock(ExecutionContext.class);
        ComponentManager compManager = this.mocker.getInstance(ComponentManager.class, "context");
        when(compManager.getInstance(Execution.class)).thenReturn(execution);
        doReturn(executionContext).when(execution).getContext();
        doReturn(mock(XWikiContext.class)).when(executionContext).getProperty("xwikicontext");

        this.repository = this.mocker.getInstance(PatientRepository.class);
        this.users = this.mocker.getInstance(UserManager.class);
        this.access = this.mocker.getInstance(AuthorizationManager.class);
        this.patientsResource = (DefaultPatientsResourceImpl)this.mocker.getComponentUnderTest();
        this.logger = this.mocker.getMockedLogger();
        this.queries = this.mocker.getInstance(QueryManager.class);

        doReturn(this.currentUser).when(this.users).getCurrentUser();
        doReturn(null).when(this.currentUser).getProfileDocument();
    }

    @Test(expected = WebApplicationException.class)
    public void addPatientUserDoesNotHaveAccess() throws XWikiRestException {
        doReturn(false).when(this.access).hasAccess(Right.EDIT, null, mock(EntityReference.class));
        Response response = this.patientsResource.addPatient("");
        verify(this.logger).debug("Importing new patient from JSON via REST: {}", "");
    }

    @Test(expected = Exception.class)
    public void addNullPatient() throws XWikiRestException {
        doReturn(true).when(this.access).hasAccess(Right.EDIT, null, mock(EntityReference.class));
        Response response = this.patientsResource.addPatient(null);
        verify(this.logger).error("Could not process remote matching request: {}", anyString(), anyObject());
    }

    @Test
    public void addPatientAsJSON() throws XWikiRestException {
        doReturn(true).when(this.access).hasAccess(Right.EDIT, null, mock(EntityReference.class));
        JSONObject json = new JSONObject();
        Response response = this.patientsResource.addPatient(json.toString());
        System.out.println(response);
    }

    @Test
    public void listPatientsNullOrderField() throws XWikiRestException {
        WebApplicationException exception = new WebApplicationException();
        try {
            Patients result = this.patientsResource.listPatients(0, 30, null, "asc");
        }
        catch (WebApplicationException ex){
            exception = ex;
        }
        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), exception.getResponse().getStatus());
        verify(this.logger).error(eq("Failed to list patients: {}"), anyString(), anyObject());
    }

    @Test(expected = WebApplicationException.class)
    public void listPatientsNullOrder() throws XWikiRestException {
        Patients result = this.patientsResource.listPatients(0, 30, "id", null);
        verify(this.logger).error("Failed to list patients: {}", anyString(), anyObject());
    }

    @Test
    public void listPatientsDefaultBehaviour() throws WebApplicationException, XWikiRestException, QueryException {
        Patients result = this.patientsResource.listPatients(0, 30, "id", "asc");
        verify(this.queries.createQuery("select doc.fullName, p.external_id, doc.creator, doc.creationDate, doc.version, doc.author, doc.date"
                + " from Document doc, doc.object(PhenoTips.PatientClass) p where doc.name <> :t order by "
                + "doc.name" + " asc", "xwql"));
    }

}