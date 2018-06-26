package org.exoplatform.services.organization.idm.externalstore;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.externalstore.IDMQueueService;
import org.exoplatform.services.organization.externalstore.model.IDMEntityType;
import org.exoplatform.services.organization.externalstore.model.IDMQueueEntry;

/**
 * Placeholder IDM Queue in memory implementation
 */
public class IDMInMemoryQueueServiceImpl implements IDMQueueService {

  private static final Log                     LOG                              =
                                                   ExoLogger.getLogger(IDMInMemoryQueueServiceImpl.class);

  private static final String                  IDM_QUEUE_PROCESSING_MAX_RETRIES = "exo.idm.queue.processing.error.retries.max";

  private static final int                     DEFAULT_MAX_RETRIES              = 5;

  private Map<IDMEntityType<?>, LocalDateTime> lastSuccessTimeByEntityType      = new HashMap<>();

  private List<IDMQueueEntry>                  queue                            = Collections.synchronizedList(new ArrayList<>());

  private int                                  maxRetries                       = DEFAULT_MAX_RETRIES;

  public IDMInMemoryQueueServiceImpl(InitParams params) {
    if (params != null && params.containsKey(IDM_QUEUE_PROCESSING_MAX_RETRIES)) {
      String maxRetriesString = params.getValueParam(IDM_QUEUE_PROCESSING_MAX_RETRIES).getValue();
      try {
        maxRetries = Integer.parseInt(maxRetriesString);
      } catch (NumberFormatException e) {
        LOG.warn("Unable to parse max retries " + maxRetriesString + ". Default value " + DEFAULT_MAX_RETRIES + " will be used",
                 e);
      }
    }
  }

  @Override
  public LocalDateTime getLastCheckedTime(IDMEntityType<?> entityType) {
    return lastSuccessTimeByEntityType.get(entityType);
  }

  @Override
  public void setLastCheckedTime(IDMEntityType<?> entityType, LocalDateTime dateTime) {
    lastSuccessTimeByEntityType.put(entityType, dateTime);
  }

  @Override
  public int countAll() throws Exception {
    return (int) queue.stream().filter(entry -> !entry.isProcessed() && entry.getRetryCount() < getMaxRetries()).count();
  }

  @Override
  public int count(int nbRetries) throws Exception {
    return (int) queue.stream().filter(entry -> !entry.isProcessed() && entry.getRetryCount() == nbRetries).count();
  }

  @Override
  public void push(IDMQueueEntry object) throws Exception {
    queue.add(0, object);
  }

  @Override
  public List<IDMQueueEntry> pop(int limit, int nbRetries, boolean keepInQueue) throws Exception {
    if (queue.size() == 0) {
      return Collections.emptyList();
    }
    List<IDMQueueEntry> entries = queue.stream()
                                       .filter(entry -> !entry.isProcessed() && entry.getRetryCount() == nbRetries)
                                       .limit(limit)
                                       .collect(Collectors.toList());
    if (!keepInQueue) {
      queue.removeAll(entries);
    }
    return entries;
  }

  @Override
  public void storeAsProcessed(List<IDMQueueEntry> queueEntries) {
    for (IDMQueueEntry queueEntry : queueEntries) {
      queueEntry.setProcessed(true);
    }
  }

  @Override
  public void incrementRetry(List<IDMQueueEntry> queueEntries) {
    for (IDMQueueEntry queueEntry : queueEntries) {
      queueEntry.setRetryCount(queueEntry.getRetryCount() + 1);
    }
  }

  @Override
  public void deleteProcessedEntries() {
    List<IDMQueueEntry> processedElements = queue.stream().filter(entry -> entry.isProcessed()).collect(Collectors.toList());
    queue.removeAll(processedElements);
  }

  @Override
  public void deleteExceededRetriesEntries() {
    List<IDMQueueEntry> processedElements = queue.stream()
                                                 .filter(entry -> entry.getRetryCount() >= getMaxRetries())
                                                 .collect(Collectors.toList());
    queue.removeAll(processedElements);
  }

  @Override
  public int getMaxRetries() {
    return maxRetries;
  }
}
