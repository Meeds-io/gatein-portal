/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.commons.file.services.job;

import org.exoplatform.commons.utils.ExoProperties;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.scheduler.CronJob;
import org.quartz.JobDataMap;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 */
public class FileStorageCronJob extends CronJob {
  public static String RETENTION_PARAM = "retention-time";

  public static String ENABLED_PARAM   = "enabled";

  private JobDataMap   jdatamap_;

  public FileStorageCronJob(InitParams params) throws Exception {
    super(params);
    ExoProperties props = params.getPropertiesParam("FileStorageCleanJob.Param").getProperties();
    jdatamap_ = new JobDataMap();
    String days = props.getProperty(RETENTION_PARAM).trim();
    jdatamap_.put(RETENTION_PARAM, days);
    String state = props.getProperty(ENABLED_PARAM).trim();
    jdatamap_.put(ENABLED_PARAM, state);
  }

  public JobDataMap getJobDataMap() {
    return jdatamap_;
  }
}
