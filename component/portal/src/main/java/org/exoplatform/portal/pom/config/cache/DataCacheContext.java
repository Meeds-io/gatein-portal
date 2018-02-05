package org.exoplatform.portal.pom.config.cache;

import org.exoplatform.portal.pom.config.POMSession;
import org.exoplatform.portal.pom.config.POMTask;
import org.exoplatform.portal.pom.config.TaskExecutionDecorator;

public class DataCacheContext {
  private TaskExecutionDecorator taskExecutionDecorator;

  private POMTask<?>             task;

  private POMSession             session;

  public DataCacheContext(TaskExecutionDecorator taskExecutionDecorator, POMTask<?> task, POMSession session) {
    this.taskExecutionDecorator = taskExecutionDecorator;
    this.task = task;
    this.session = session;
  }

  public POMSession getSession() {
    return session;
  }

  public POMTask<?> getTask() {
    return task;
  }

  public TaskExecutionDecorator getTaskExecutionDecorator() {
    return taskExecutionDecorator;
  }

  public Object execute() throws Exception {
    return taskExecutionDecorator.executeWithoutCache(session, task);
  }
}
