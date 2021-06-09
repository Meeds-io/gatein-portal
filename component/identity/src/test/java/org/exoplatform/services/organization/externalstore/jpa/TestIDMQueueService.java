package org.exoplatform.services.organization.externalstore.jpa;

import java.time.*;
import java.util.List;

import org.junit.Test;

import org.exoplatform.component.test.*;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.organization.externalstore.IDMQueueService;
import org.exoplatform.services.organization.externalstore.model.*;
import org.exoplatform.services.organization.idm.externalstore.jpa.IDMQueueDAO;

@ConfiguredBy({
    @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/standalone/test-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/idm-queue-configuration.xml")
})
public class TestIDMQueueService extends AbstractKernelTest {

  PortalContainer container;

  IDMQueueService queueService = null;

  IDMQueueDAO     queueDAO     = null;

  @Override
  protected void beforeRunBare() {
    try {
      super.beforeRunBare();
      container = PortalContainer.getInstance();
      queueService = container.getComponentInstanceOfType(IDMQueueService.class);
      queueDAO = container.getComponentInstanceOfType(IDMQueueDAO.class);
      ExoContainerContext.setCurrentContainer(container);
      RequestLifeCycle.begin(container);
    } catch (Exception e) {
      fail(e);
    }
  }

  @Override
  protected void afterRunBare() {
    try {
      queueDAO.deleteAll();
      assertEquals("Number of entries must be 0 before any test runs.", 0, queueService.countAll());
    } catch (Exception e) {
      fail(e);
    }
    super.afterRunBare();
    RequestLifeCycle.end();
  }

  @Test
  public void testPush() throws Exception {
    assertEquals("Queue must be initially empty", 0, queueService.countAll());
    queueService.push(new IDMQueueEntry(IDMEntityType.USER, "testuser", IDMOperationType.ADD_OR_UPDATE));
    queueService.push(new IDMQueueEntry(IDMEntityType.USER, "testuser2", IDMOperationType.DELETE));
    assertEquals("Two new pushed elements must be detected", 2, queueService.countAll());
  }

  @Test
  public void testPop() throws Exception {
    assertEquals("Queue must be initially empty", 0, queueService.countAll());
    queueService.push(new IDMQueueEntry(IDMEntityType.USER, "testuser", IDMOperationType.ADD_OR_UPDATE));
    queueService.push(new IDMQueueEntry(IDMEntityType.USER, "testuser2", IDMOperationType.DELETE));

    IDMQueueEntry entryInError = new IDMQueueEntry(IDMEntityType.USER, "testuser3", IDMOperationType.ADD_OR_UPDATE);
    entryInError.setRetryCount(1);
    queueService.push(entryInError);

    IDMQueueEntry entryProcessed = new IDMQueueEntry(IDMEntityType.USER, "testuser4", IDMOperationType.ADD_OR_UPDATE);
    entryProcessed.setProcessed(true);
    queueService.push(entryProcessed);

    assertEquals("Only not processed and retry < maxRetries should be recognized.", 3, queueService.countAll());
    assertEquals("Only two entries are not processed and have nbRetries == 0.", 2, queueService.count(0));
    assertEquals("Only one entry is not processed and have nbRetries == 1.", 1, queueService.count(1));

    assertEquals("Only one entry is requested which has't been processed and have nbRetries == 0.",
                 1,
                 queueService.pop(1, 0, true).size());
    assertEquals("Only two entries are requested which have't been processed and have nbRetries == 0.",
                 2,
                 queueService.pop(2, 0, true).size());
    assertEquals("Only two entries are existing in queue which have't been processed and have nbRetries == 0.",
                 2,
                 queueService.pop(100, 0, true).size());

    assertEquals("Only one entry is existing in queue which has't been processed and have nbRetries == 1.",
                 1,
                 queueService.pop(100, 1, true).size());
    assertEquals("No entry is existing in queue which has't been processed and have nbRetries == 2.",
                 0,
                 queueService.pop(100, 2, true).size());

    List<IDMQueueEntry> entities = queueService.pop(1, 1, false);
    assertEquals("Only one entry is existing in queue which has't been processed and have nbRetries == 1.", 1, entities.size());
    assertEquals("Queue entry should has nbRetries == 1.", 1, entities.get(0).getRetryCount());
    assertEquals("Queue entry should has id = 'testuser3'.", "testuser3", entities.get(0).getEntityId());
    assertEquals("No entry should exists in queue which has't been processed and have nbRetries == 1.", 0, queueService.count(1));

    entities = queueService.pop(1, 0, false);
    assertEquals("Only one entry is requested which has't been processed and have nbRetries == 0.", 1, entities.size());
    assertEquals("One entry is requested which has't been processed and have nbRetries == 0. Thus nbRetries should equals to 0.",
                 0,
                 entities.get(0).getRetryCount());
    assertEquals("Last added entry should be retrieved", "testuser2", entities.get(0).getEntityId());
    assertEquals("The last request 'pop' operation should delete the requested entry."
        + " Thus, only one entry should remaining that has't been processed and have nbRetries == 0.", 1, queueService.count(0));
  }

  @Test
  public void testLastCheckedTime() throws Exception {
    assertNotNull(getLocalDateTime());
    assertNull(queueService.getLastCheckedTime(IDMEntityType.USER));
    assertNull(queueService.getLastCheckedTime(IDMEntityType.ROLE));
    assertNull(queueService.getLastCheckedTime(IDMEntityType.GROUP));

    // Test add checked users time
    LocalDateTime userCheckedTime = getLocalDateTime();
    queueService.setLastCheckedTime(IDMEntityType.USER, userCheckedTime);
    assertEquals(userCheckedTime, queueService.getLastCheckedTime(IDMEntityType.USER));
    assertNull(queueService.getLastCheckedTime(IDMEntityType.ROLE));
    assertNull(queueService.getLastCheckedTime(IDMEntityType.GROUP));

    // Test modify checked users time
    userCheckedTime = getLocalDateTime();
    queueService.setLastCheckedTime(IDMEntityType.USER, userCheckedTime);
    assertEquals(userCheckedTime, queueService.getLastCheckedTime(IDMEntityType.USER));
    assertNull(queueService.getLastCheckedTime(IDMEntityType.ROLE));
    assertNull(queueService.getLastCheckedTime(IDMEntityType.GROUP));

    // Test add checked groups time
    LocalDateTime groupCheckedTime = getLocalDateTime();
    queueService.setLastCheckedTime(IDMEntityType.GROUP, groupCheckedTime);
    assertEquals(userCheckedTime, queueService.getLastCheckedTime(IDMEntityType.USER));
    assertEquals(groupCheckedTime, queueService.getLastCheckedTime(IDMEntityType.GROUP));
    assertNull(queueService.getLastCheckedTime(IDMEntityType.ROLE));

    // Test add checked roles time
    LocalDateTime roleCheckedTime = getLocalDateTime();
    queueService.setLastCheckedTime(IDMEntityType.ROLE, roleCheckedTime);
    assertEquals(userCheckedTime, queueService.getLastCheckedTime(IDMEntityType.USER));
    assertEquals(groupCheckedTime, queueService.getLastCheckedTime(IDMEntityType.GROUP));
    assertEquals(roleCheckedTime, queueService.getLastCheckedTime(IDMEntityType.ROLE));
  }

  @Test
  public void testProcessed() throws Exception {
    assertEquals("Queue must be initially empty", 0, queueService.countAll());
    queueService.push(new IDMQueueEntry(IDMEntityType.USER,
                                        "testuser",
                                        IDMOperationType.ADD_OR_UPDATE));
    queueService.push(new IDMQueueEntry(IDMEntityType.USER,
                                        "testuser2",
                                        IDMOperationType.DELETE));

    IDMQueueEntry entryInError = new IDMQueueEntry(IDMEntityType.USER,
                                                   "testuser3",
                                                   IDMOperationType.ADD_OR_UPDATE);
    entryInError.setRetryCount(1);
    queueService.push(entryInError);

    IDMQueueEntry entryProcessed = new IDMQueueEntry(IDMEntityType.USER,
                                                     "testuser4",
                                                     IDMOperationType.ADD_OR_UPDATE);
    entryProcessed.setProcessed(true);
    queueService.push(entryProcessed);

    assertEquals(3, queueService.countAll());
    assertEquals(2, queueService.count(0));
    assertEquals(1, queueService.count(1));

    List<IDMQueueEntry> entities = queueService.pop(1, 0, true);
    assertEquals(1, entities.size());
    assertFalse(entities.get(0).isProcessed());
    assertEquals(0, entities.get(0).getRetryCount());

    RequestLifeCycle.end();
    RequestLifeCycle.begin(container);

    queueService.storeAsProcessed(entities);

    assertEquals(2, queueService.countAll());
    assertEquals(1, queueService.count(0));
    assertEquals(1, queueService.count(1));

    entities = queueService.pop(1, 0, true);
    assertEquals(1, entities.size());
    assertFalse(entities.get(0).isProcessed());
    assertEquals(0, entities.get(0).getRetryCount());

    queueService.deleteProcessedEntries();

    assertEquals(2, queueService.countAll());
    assertEquals(1, queueService.count(0));
    assertEquals(1, queueService.count(1));

    entities = queueService.pop(1, 0, true);
    assertEquals(1, entities.size());
    assertFalse(entities.get(0).isProcessed());
    assertEquals(0, entities.get(0).getRetryCount());
  }

  @Test
  public void testIncrementRetry() throws Exception {
    ExoContainerContext.setCurrentContainer(container);

    assertEquals("Queue must be initially empty", 0, queueService.countAll());
    queueService.push(new IDMQueueEntry(IDMEntityType.USER, "testuser", IDMOperationType.ADD_OR_UPDATE));
    queueService.push(new IDMQueueEntry(IDMEntityType.USER, "testuser2", IDMOperationType.DELETE));

    IDMQueueEntry entryInError = new IDMQueueEntry(IDMEntityType.USER, "testuser3", IDMOperationType.ADD_OR_UPDATE);
    entryInError.setRetryCount(1);
    queueService.push(entryInError);

    IDMQueueEntry entryProcessed = new IDMQueueEntry(IDMEntityType.USER, "testuser4", IDMOperationType.ADD_OR_UPDATE);
    entryProcessed.setProcessed(true);
    queueService.push(entryProcessed);

    List<IDMQueueEntry> entities = queueService.pop(100, 1, true);
    assertEquals(1, entities.size());
    queueService.incrementRetry(entities);
    assertEquals(0, queueService.pop(100, 1, true).size());
    assertEquals(1, queueService.pop(100, 2, true).size());

    entities = queueService.pop(100, 0, true);
    assertEquals(2, entities.size());
    queueService.incrementRetry(entities);
    assertEquals(2, queueService.pop(100, 1, true).size());
    assertEquals(1, queueService.pop(100, 2, true).size());
    assertEquals(0, queueService.pop(100, 0, true).size());
    assertEquals(0, queueService.pop(100, 3, true).size());
  }

  private LocalDateTime getLocalDateTime() {
    LocalDateTime localDateTime = ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime();
    localDateTime = localDateTime.minusNanos(localDateTime.getNano());
    return localDateTime;
  }

}
