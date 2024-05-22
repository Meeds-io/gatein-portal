/*
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.commons.file.resource;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: FileCleaner.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class FileCleaner extends WorkerThread {

  protected static final long                                       DEFAULT_TIMEOUT    = 30000;

  protected static final Log                                        LOG                =
                                                                        ExoLogger.getLogger(FileCleaner.class.getName());

  /**
   * In-share files database.
   */
  protected static final ConcurrentMap<String, WeakReference<File>> CURRENT_SWAP_FILES = new ConcurrentHashMap<>();

  protected final ConcurrentLinkedQueue<File>                       files              = new ConcurrentLinkedQueue<>();

  /**
   * The shutdown hook
   */
  private final Thread                                              hook               = new Thread() {
                                                                                         @Override
                                                                                         public void run() {
                                                                                           File file = null;
                                                                                           for (WeakReference<File> swapFileRef : CURRENT_SWAP_FILES.values()) {
                                                                                             addFile(swapFileRef.get()
                                                                                                                .getAbsoluteFile());
                                                                                           }
                                                                                           while ((file =
                                                                                                        files.poll()) != null) {
                                                                                             file.delete();
                                                                                           }
                                                                                         }
                                                                                       };

  public FileCleaner() {
    this(DEFAULT_TIMEOUT);
  }

  public FileCleaner(ExoContainerContext ctx) {
    this(null, ctx, DEFAULT_TIMEOUT);
  }

  public FileCleaner(long timeout) {
    this(timeout, true);
  }

  public FileCleaner(String prefix, ExoContainerContext ctx, long timeout) {
    this(ctx == null ? prefix : (prefix == null ? "" : prefix + " ") + ctx.getName(), timeout, true);
  }

  public FileCleaner(boolean start) {
    this(DEFAULT_TIMEOUT, start);
  }

  public FileCleaner(long timeout, boolean start) {
    this(null, timeout, start);
  }

  public FileCleaner(String id, long timeout, boolean start) {
    super(timeout);
    setName("File Cleaner " + (id == null ? getId() : id));
    setDaemon(true);
    setPriority(Thread.MIN_PRIORITY);

    if (start)
      start();

    registerShutdownHook();

    if (LOG.isDebugEnabled()) {
      LOG.debug("FileCleaner instantiated name= " + getName() + " timeout= " + timeout);
    }
  }

  /**
   * @param file
   */
  public void addFile(File file) {
    if (file.exists()) {
      files.offer(file);
    }
  }

  /**
   * @param file
   */
  public void removeFile(File file) {
    files.remove(file);
  }

  @Override
  public void halt() {
    try {
      callPeriodically();
    } catch (Exception e) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("An exception occurred: " + e.getMessage());
      }
    }
    // Remove the hook for final cleaning up
    Runtime.getRuntime().removeShutdownHook(hook);

    if (files != null && files.size() > 0)
      LOG.warn("There are uncleared files: " + files.size());

    super.halt();
  }

  /**
   * @see WorkerThread#callPeriodically()
   */
  @Override
  protected void callPeriodically() throws Exception {
    File file = null;
    Set<File> notRemovedFiles = new HashSet<File>();
    while ((file = files.poll()) != null) {
      if (file.exists()) {
        if (!file.delete()) {
          notRemovedFiles.add(file);

          if (LOG.isDebugEnabled())
            LOG.debug("Could not delete " + (file.isDirectory() ? "directory" : "file")
                + ". Will try next time: " + file.getAbsolutePath());
        } else if (LOG.isDebugEnabled()) {
          LOG.debug((file.isDirectory() ? "Directory" : "File") + " deleted : "
              + file.getAbsolutePath());
        }
      }
    }

    // add do lists tail all not removed files
    if (!notRemovedFiles.isEmpty()) {
      files.addAll(notRemovedFiles);
    }
  }

  private void registerShutdownHook() {
    // register shutdown hook for final cleaning up
    try {
      Runtime.getRuntime().addShutdownHook(hook);
    } catch (IllegalStateException e) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("An exception occurred: " + e.getMessage());
      }
    }
  }
}
