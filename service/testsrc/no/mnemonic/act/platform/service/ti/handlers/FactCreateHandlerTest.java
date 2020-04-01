package no.mnemonic.act.platform.service.ti.handlers;

import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.api.model.v1.Subject;
import no.mnemonic.act.platform.api.request.v1.AccessMode;
import no.mnemonic.act.platform.auth.OrganizationResolver;
import no.mnemonic.act.platform.auth.SubjectResolver;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.record.FactAclEntryRecord;
import no.mnemonic.act.platform.dao.api.record.FactCommentRecord;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.result.ResultContainer;
import no.mnemonic.act.platform.dao.cassandra.OriginManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.act.platform.service.contexts.TriggerContext;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.TiServiceEvent;
import no.mnemonic.act.platform.service.ti.converters.response.FactResponseConverter;
import no.mnemonic.act.platform.service.ti.resolvers.OriginResolver;
import no.mnemonic.act.platform.service.validators.Validator;
import no.mnemonic.act.platform.service.validators.ValidatorFactory;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;
import java.util.UUID;

import static no.mnemonic.commons.utilities.collections.ListUtils.list;
import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactCreateHandlerTest {

  @Mock
  private TiSecurityContext securityContext;
  @Mock
  private SubjectResolver subjectResolver;
  @Mock
  private OrganizationResolver organizationResolver;
  @Mock
  private OriginResolver originResolver;
  @Mock
  private OriginManager originManager;
  @Mock
  private ValidatorFactory validatorFactory;
  @Mock
  private ObjectFactDao objectFactDao;
  @Mock
  private FactResponseConverter factResponseConverter;
  @Mock
  private TriggerContext triggerContext;

  private FactCreateHandler handler;

  @Before
  public void setUp() {
    initMocks(this);
    handler = new FactCreateHandler(securityContext, subjectResolver, organizationResolver, originResolver, originManager, validatorFactory, objectFactDao, factResponseConverter, triggerContext);
  }

  @Test(expected = InvalidArgumentException.class)
  public void testResolveOrganizationProvidedOrganizationNotExists() throws Exception {
    handler.resolveOrganization(UUID.randomUUID(), null);
  }

  @Test
  public void testResolveOrganizationProvidedFetchesExistingOrganization() throws Exception {
    UUID organizationID = UUID.randomUUID();
    Organization organization = Organization.builder().build();
    when(organizationResolver.resolveOrganization(organizationID)).thenReturn(organization);

    assertSame(organization, handler.resolveOrganization(organizationID, null));
    verify(organizationResolver).resolveOrganization(organizationID);
    verify(securityContext).checkReadPermission(organization);
  }

  @Test(expected = InvalidArgumentException.class)
  public void testResolveOrganizationFallbackToOriginOrganizationNotExists() throws Exception {
    handler.resolveOrganization(null, new OriginEntity().setOrganizationID(UUID.randomUUID()));
  }

  @Test
  public void testResolveOrganizationFallbackToOriginFetchesExistingOrganization() throws Exception {
    OriginEntity origin = new OriginEntity().setOrganizationID(UUID.randomUUID());
    Organization organization = Organization.builder().build();
    when(organizationResolver.resolveOrganization(origin.getOrganizationID())).thenReturn(organization);

    assertSame(organization, handler.resolveOrganization(null, origin));
    verify(organizationResolver).resolveOrganization(origin.getOrganizationID());
  }

  @Test(expected = InvalidArgumentException.class)
  public void testResolveOrganizationFallbackToCurrentUserOrganizationNotExists() throws Exception {
    when(securityContext.getCurrentUserOrganizationID()).thenReturn(UUID.randomUUID());
    handler.resolveOrganization(null, new OriginEntity());
  }

  @Test
  public void testResolveOrganizationFallbackToCurrentUserFetchesExistingOrganization() throws Exception {
    UUID currentUserOrganizationID = UUID.randomUUID();
    Organization organization = Organization.builder().build();
    when(securityContext.getCurrentUserOrganizationID()).thenReturn(currentUserOrganizationID);
    when(organizationResolver.resolveOrganization(currentUserOrganizationID)).thenReturn(organization);

    assertSame(organization, handler.resolveOrganization(null, new OriginEntity()));
    verify(securityContext).getCurrentUserOrganizationID();
    verify(organizationResolver).resolveOrganization(currentUserOrganizationID);
  }

  @Test(expected = InvalidArgumentException.class)
  public void testResolveOriginProvidedOriginNotExists() throws Exception {
    handler.resolveOrigin(UUID.randomUUID());
  }

  @Test
  public void testResolveOriginProvidedFetchesExistingOrigin() throws Exception {
    UUID originID = UUID.randomUUID();
    OriginEntity origin = new OriginEntity().setId(originID);
    when(originResolver.apply(originID)).thenReturn(origin);

    assertSame(origin, handler.resolveOrigin(originID));
    verify(originResolver).apply(originID);
    verify(securityContext).checkReadPermission(origin);
    verifyNoInteractions(originManager);
  }

  @Test(expected = InvalidArgumentException.class)
  public void testResolveOriginProvidedFetchesDeletedOrigin() throws Exception {
    UUID originID = UUID.randomUUID();
    OriginEntity origin = new OriginEntity()
            .setId(originID)
            .addFlag(OriginEntity.Flag.Deleted);
    when(originResolver.apply(originID)).thenReturn(origin);

    handler.resolveOrigin(originID);
  }

  @Test
  public void testResolveOriginNonProvidedFetchesExistingOrigin() throws Exception {
    UUID currentUserID = UUID.randomUUID();
    OriginEntity origin = new OriginEntity().setId(currentUserID);
    when(securityContext.getCurrentUserID()).thenReturn(currentUserID);
    when(originResolver.apply(currentUserID)).thenReturn(origin);

    assertSame(origin, handler.resolveOrigin(null));
    verify(originResolver).apply(currentUserID);
    verifyNoInteractions(originManager);
  }

  @Test(expected = InvalidArgumentException.class)
  public void testResolveOriginNonProvidedFetchesDeletedOrigin() throws Exception {
    UUID currentUserID = UUID.randomUUID();
    OriginEntity origin = new OriginEntity()
            .setId(currentUserID)
            .addFlag(OriginEntity.Flag.Deleted);
    when(securityContext.getCurrentUserID()).thenReturn(currentUserID);
    when(originResolver.apply(currentUserID)).thenReturn(origin);

    handler.resolveOrigin(null);
  }

  @Test
  public void testResolveOriginNonProvidedCreatesNewOrigin() throws Exception {
    UUID currentUserID = UUID.randomUUID();
    Subject currentUser = Subject.builder()
            .setId(currentUserID)
            .setName("name")
            .setOrganization(Organization.builder().setId(UUID.randomUUID()).build().toInfo())
            .build();
    OriginEntity newOrigin = new OriginEntity().setId(currentUserID);
    when(securityContext.getCurrentUserID()).thenReturn(currentUserID);
    when(subjectResolver.resolveSubject(currentUserID)).thenReturn(currentUser);
    when(originManager.saveOrigin(notNull())).thenReturn(newOrigin);

    assertSame(newOrigin, handler.resolveOrigin(null));
    verify(originResolver).apply(currentUserID);
    verify(subjectResolver).resolveSubject(currentUserID);
    verify(originManager).saveOrigin(argThat(entity -> {
      assertEquals(currentUserID, entity.getId());
      assertNotNull(entity.getNamespaceID());
      assertEquals(currentUser.getOrganization().getId(), entity.getOrganizationID());
      assertEquals(currentUser.getName(), entity.getName());
      assertEquals(0.8f, entity.getTrust(), 0.0);
      assertEquals(OriginEntity.Type.User, entity.getType());
      return true;
    }));
  }

  @Test
  public void testResolveOriginNonProvidedCreatesNewOriginAvoidingNameCollision() throws Exception {
    UUID currentUserID = UUID.randomUUID();
    Subject currentUser = Subject.builder()
            .setId(currentUserID)
            .setName("name")
            .setOrganization(Organization.builder().setId(UUID.randomUUID()).build().toInfo())
            .build();
    when(securityContext.getCurrentUserID()).thenReturn(currentUserID);
    when(subjectResolver.resolveSubject(currentUserID)).thenReturn(currentUser);
    when(originManager.getOrigin(currentUser.getName())).thenReturn(new OriginEntity()
            .setId(UUID.randomUUID())
            .setName(currentUser.getName()));

    handler.resolveOrigin(null);
    verify(originManager).getOrigin(currentUser.getName());
    verify(originManager).saveOrigin(argThat(entity -> {
      assertEquals(currentUserID, entity.getId());
      assertNotEquals(currentUser.getName(), entity.getName());
      assertTrue(entity.getName().startsWith(currentUser.getName()));
      return true;
    }));
  }

  @Test
  public void testResolveAccessModeWithNull() throws Exception {
    assertNull(handler.resolveAccessMode(null, AccessMode.Public));
    assertNull(handler.resolveAccessMode(new FactRecord(), AccessMode.Public));
  }

  @Test
  public void testResolveAccessModeFallsBackToReferencedFact() throws Exception {
    FactRecord referencedFact = new FactRecord().setAccessMode(FactRecord.AccessMode.Public);
    assertEquals(referencedFact.getAccessMode(), handler.resolveAccessMode(referencedFact, null));
  }

  @Test
  public void testResolveAccessModeAllowsEqualOrMoreRestrictive() throws Exception {
    assertEquals(FactRecord.AccessMode.Public, handler.resolveAccessMode(new FactRecord().setAccessMode(FactRecord.AccessMode.Public), AccessMode.Public));
    assertEquals(FactRecord.AccessMode.RoleBased, handler.resolveAccessMode(new FactRecord().setAccessMode(FactRecord.AccessMode.Public), AccessMode.RoleBased));
    assertEquals(FactRecord.AccessMode.Explicit, handler.resolveAccessMode(new FactRecord().setAccessMode(FactRecord.AccessMode.Public), AccessMode.Explicit));

    assertEquals(FactRecord.AccessMode.RoleBased, handler.resolveAccessMode(new FactRecord().setAccessMode(FactRecord.AccessMode.RoleBased), AccessMode.RoleBased));
    assertEquals(FactRecord.AccessMode.Explicit, handler.resolveAccessMode(new FactRecord().setAccessMode(FactRecord.AccessMode.RoleBased), AccessMode.Explicit));

    assertEquals(FactRecord.AccessMode.Explicit, handler.resolveAccessMode(new FactRecord().setAccessMode(FactRecord.AccessMode.Explicit), AccessMode.Explicit));
  }

  @Test
  public void testResolveAccessModeDisallowsLessRestrictive() {
    assertThrows(InvalidArgumentException.class, () -> handler.resolveAccessMode(new FactRecord().setAccessMode(FactRecord.AccessMode.Explicit), AccessMode.RoleBased));
    assertThrows(InvalidArgumentException.class, () -> handler.resolveAccessMode(new FactRecord().setAccessMode(FactRecord.AccessMode.Explicit), AccessMode.Public));
    assertThrows(InvalidArgumentException.class, () -> handler.resolveAccessMode(new FactRecord().setAccessMode(FactRecord.AccessMode.RoleBased), AccessMode.Public));
  }

  @Test
  public void testAssertValidFactValue() {
    FactTypeEntity factType = new FactTypeEntity().setValidator("SomeValidator").setValidatorParameter("SomeParam");
    Validator validator = mock(Validator.class);
    when(validator.validate(eq("test"))).thenReturn(false);
    when(validatorFactory.get(factType.getValidator(), factType.getValidatorParameter())).thenReturn(validator);

    InvalidArgumentException ex = assertThrows(InvalidArgumentException.class,
      () -> handler.assertValidFactValue(factType, "test"));
    assertEquals(SetUtils.set("fact.not.valid"), SetUtils.set(ex.getValidationErrors(), InvalidArgumentException.ValidationError::getMessageTemplate));
  }

  @Test
  public void testSaveNewFact() {
    FactRecord factToSave = new FactRecord()
      .setId(UUID.randomUUID())
      .setAccessMode(FactRecord.AccessMode.RoleBased)
      .setOrganizationID(UUID.randomUUID());
    mockSaveFact();

    List<UUID> subjectIds = list(UUID.randomUUID());
    handler.saveFact(factToSave, "some comment", subjectIds);

    verify(objectFactDao).storeFact(argThat(fact -> {
      assertEquals(factToSave, fact);
      assertEquals(set("some comment"), set(fact.getComments(), FactCommentRecord::getComment));
      assertEquals(set(subjectIds), set(fact.getAcl(), FactAclEntryRecord::getSubjectID));
      return true;
    }));

    verify(objectFactDao, never()).refreshFact(any());
    verify(factResponseConverter).apply(same(factToSave));
  }

  @Test
  public void testRefreshExistingFact() {
    FactRecord factToSave = new FactRecord()
      .setId(UUID.randomUUID())
      .setAccessMode(FactRecord.AccessMode.Public)
      .setOrganizationID(UUID.randomUUID());
    mockSaveFact();

    // Mock fetching of existing Fact.
    FactRecord existingFact = new FactRecord()
      .setId(UUID.randomUUID())
      .setAccessMode(FactRecord.AccessMode.RoleBased)
      .setOrganizationID(UUID.randomUUID());
    when(objectFactDao.retrieveExistingFacts(factToSave))
      .thenReturn(ResultContainer.<FactRecord>builder().setValues(list(existingFact).iterator()).build());
    when(securityContext.hasReadPermission(existingFact)).thenReturn(true);

    // Mock stuff needed for refreshing Fact.
    when(objectFactDao.refreshFact(existingFact)).thenReturn(existingFact);

    List<UUID> subjectIds = list(UUID.randomUUID());
    handler.saveFact(factToSave, "some comment", subjectIds);

    verify(objectFactDao).refreshFact(argThat(fact -> {
      assertEquals(existingFact, fact);
      assertEquals(set("some comment"), set(fact.getComments(), FactCommentRecord::getComment));
      assertEquals(set(subjectIds), set(fact.getAcl(), FactAclEntryRecord::getSubjectID));
      return true;
    }));

    verify(objectFactDao, never()).storeFact(any());
    verify(factResponseConverter).apply(same(existingFact));
  }

  @Test
  public void testSaveFactRegisterTriggerEvent() {
    FactRecord fact = new FactRecord()
      .setId(UUID.randomUUID())
      .setAccessMode(FactRecord.AccessMode.Public)
      .setOrganizationID(UUID.randomUUID());
    mockSaveFact();

    Fact addedFact = handler.saveFact(fact, null, null);

    verify(triggerContext).registerTriggerEvent(argThat(event -> {
      assertNotNull(event);
      assertEquals(TiServiceEvent.EventName.FactAdded.name(), event.getEvent());
      assertEquals(fact.getOrganizationID(), event.getOrganization());
      assertEquals(fact.getAccessMode().name(), event.getAccessMode().name());
      assertSame(addedFact, event.getContextParameters().get(TiServiceEvent.ContextParameter.AddedFact.name()));
      return true;
    }));
  }

  private void mockSaveFact() {
    // Mocking
    mockFactConverter();
    // Mock fetching of existing Fact.
    when(objectFactDao.retrieveExistingFacts(any())).thenReturn(ResultContainer.<FactRecord>builder().build());
    // Mock stuff needed for saving Fact.
    when(objectFactDao.storeFact(any())).thenAnswer(i -> i.getArgument(0));
  }

  private void mockFactConverter() {
    // Mock FactConverter needed for registering TriggerEvent.
    when(factResponseConverter.apply(any())).then(i -> {
      FactRecord record = i.getArgument(0);
      return Fact.builder()
        .setId(record.getId())
        .setAccessMode(no.mnemonic.act.platform.api.model.v1.AccessMode.valueOf(record.getAccessMode().name()))
        .setOrganization(Organization.builder().setId(record.getOrganizationID()).build().toInfo())
        .build();
    });
  }
}
