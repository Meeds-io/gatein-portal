package org.exoplatform.commons.file.services.impl;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.file.model.NameSpace;
import org.exoplatform.commons.file.resource.BinaryProvider;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.model.FileInfo;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.commons.file.services.FileStorageException;
import org.exoplatform.commons.file.services.NameSpaceService;
import org.exoplatform.commons.file.storage.DataStorage;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * File Service which stores the file metadata in a database, and uses a
 * BinaryProvider to store the file binary. Created by The eXo Platform SAS
 * Author : eXoPlatform exo@exoplatform.com
 */
public class FileServiceImpl implements FileService {

  private static final Log    LOG                = ExoLogger.getLogger(FileServiceImpl.class);

  private static final String FILE_CREATED_EVENT = "file.created";

  private static final String FILE_UPDATED_EVENT = "file.updated";

  private static final String FILE_DELETED_EVENT = "file.deleted";

  private DataStorage      dataStorage;

  private BinaryProvider   binaryProvider;

  private NameSpaceService nameSpaceService;

  private ListenerService  listenerService;

  public FileServiceImpl(DataStorage dataStorage,
                         BinaryProvider resourceProvider,
                         NameSpaceService nameSpaceService,
                         ListenerService listenerService) {
    this.dataStorage = dataStorage;
    this.binaryProvider = resourceProvider;
    this.nameSpaceService = nameSpaceService;
    this.listenerService = listenerService;
  }

  @Override
  public FileInfo getFileInfo(long id){
    return dataStorage.getFileInfo(id);
  }
  
  @Override
  public List<FileInfo> getFileInfoListByChecksum(String checksum){
    return dataStorage.getFileInfoListByChecksum(checksum);
  }

  @Override
  public FileItem getFile(long id) throws FileStorageException {
    FileItem fileItem;
    FileInfo fileInfo = getFileInfo(id);

    if (fileInfo == null || StringUtils.isEmpty(fileInfo.getChecksum())) {
      return null;
    }
    try {
      fileItem = new FileItem(fileInfo, null);
      InputStream inputStream = binaryProvider.getStream(fileInfo.getChecksum());
      fileItem.setInputStream(inputStream);
    } catch (Exception e) {
      throw new FileStorageException("Cannot get File Item ID=" + id, e);
    }

    return fileItem;
  }

  @Override
  @ExoTransactional
  public FileItem writeFile(FileItem file) throws FileStorageException, IOException {
    if (file.getFileInfo() == null || StringUtils.isEmpty(file.getFileInfo().getChecksum())) {
      throw new IllegalArgumentException("Checksum is required to persist the binary");
    }
    FileInfo fileInfo = file.getFileInfo();
    NameSpace nSpace;
    if (fileInfo.getNameSpace() != null && !fileInfo.getNameSpace().isEmpty()) {
      nSpace = dataStorage.getNameSpace(fileInfo.getNameSpace());
    } else {
      nSpace = dataStorage.getNameSpace(nameSpaceService.getDefaultNameSpace());
    }
    FileStorageTransaction transaction = new FileStorageTransaction(fileInfo, nSpace);
    FileInfo createdFileInfoEntity = transaction.twoPhaseCommit(2, file.getAsStream());
    if (createdFileInfoEntity != null) {
      fileInfo.setId(createdFileInfoEntity.getId());
      file.setFileInfo(fileInfo);
      try {
        listenerService.broadcast(FILE_CREATED_EVENT, fileInfo, null);
      } catch (Exception e) {
        LOG.error("Error while broadcasting event: {}", FILE_CREATED_EVENT, e);
      }
      return file;
    }
    return null;
  }

  @Override
  @ExoTransactional
  public FileItem updateFile(FileItem file) throws FileStorageException, IOException {
    if (file.getFileInfo() == null || StringUtils.isEmpty(file.getFileInfo().getChecksum())) {
      throw new IllegalArgumentException("Checksum is required to persist the binary");
    }
    FileInfo fileInfo = file.getFileInfo();
    NameSpace nSpace;
    if (fileInfo.getNameSpace() != null && !fileInfo.getNameSpace().isEmpty()) {
      nSpace = dataStorage.getNameSpace(fileInfo.getNameSpace());
    } else {
      nSpace = dataStorage.getNameSpace(nameSpaceService.getDefaultNameSpace());
    }
    FileStorageTransaction transaction = new FileStorageTransaction(fileInfo, nSpace);
    FileInfo createdFileInfoEntity = transaction.twoPhaseCommit(0, file.getAsStream());
    if (createdFileInfoEntity != null) {
      fileInfo.setId(createdFileInfoEntity.getId());
      file.setFileInfo(fileInfo);
      try {
        listenerService.broadcast(FILE_UPDATED_EVENT, fileInfo, null);
      } catch (Exception e) {
        LOG.error("Error while broadcasting event: {}", FILE_UPDATED_EVENT, e);
      }
      return file;
    }
    return null;
  }

  @Override
  public FileInfo deleteFile(long id) {
    FileInfo fileInfo = dataStorage.getFileInfo(id);
    if (fileInfo != null) {
      fileInfo.setDeleted(true);
    }
    FileInfo newFileInfo = dataStorage.updateFileInfo(fileInfo);
    try {
      listenerService.broadcast(FILE_DELETED_EVENT, newFileInfo, null);
    } catch (Exception e) {
      LOG.error("Error while broadcasting event: {}", FILE_DELETED_EVENT, e);
    }
    return newFileInfo;
  }

  @Override
  public List<FileItem> getFilesByChecksum(String checksum) throws FileStorageException {
    List<FileItem> fileItemList = new ArrayList<FileItem>();
    List<FileInfo> fileInfoList = getFileInfoListByChecksum(checksum);

    try {
      for (FileInfo fileInfo : fileInfoList) {
        FileItem fileItem = new FileItem(fileInfo, null);
        InputStream inputStream = binaryProvider.getStream(fileInfo.getChecksum());
        fileItem.setInputStream(inputStream);
        fileItemList.add(fileItem);
      }
    } catch (Exception e) {
      throw new FileStorageException("Cannot get File Item CHECKSUM=" + checksum, e);
    }

    return fileItemList;
  }

  /* Manage two phase commit :file storage and datasource */
  private class FileStorageTransaction {
    /**
     * Update Operation.
     */
    final int         UPDATE = 0;

    /**
     * Remove Operation.
     */
    final int         REMOVE = 1;

    /**
     * Insert Operation.
     */
    final int         INSERT = 2;

    private FileInfo  fileInfo;

    private NameSpace nameSpace;

    public FileStorageTransaction(FileInfo fileInfo, NameSpace nameSpace) {
      this.fileInfo = fileInfo;
      this.nameSpace = nameSpace;
    }

    public FileInfo twoPhaseCommit(int operation, InputStream inputStream) throws FileStorageException {
      FileInfo createdFileInfoEntity = null;
      if (operation == INSERT) {
        boolean created = false;
        try {
          if (!binaryProvider.exists(fileInfo.getChecksum())) {
            binaryProvider.put(fileInfo.getChecksum(), inputStream);
            created = true;
          }
          createdFileInfoEntity = dataStorage.create(fileInfo, nameSpace);
          return createdFileInfoEntity;
        } catch (Exception e) {
          try {
            if (created) {
              binaryProvider.remove(fileInfo.getChecksum());
            }
          } catch (IOException e1) {
            LOG.error("Error while rollback writing file");
          }
          throw new FileStorageException("Error while writing file " + fileInfo.getName(), e);
        }

      } else if (operation == REMOVE) {
        fileInfo.setDeleted(true);
        dataStorage.updateFileInfo(fileInfo);
      } else if (operation == UPDATE) {
        try {
          boolean updated = false;
          FileInfo oldFile = dataStorage.getFileInfo(fileInfo.getId());
          if (oldFile == null || oldFile.getChecksum().isEmpty()
              || !oldFile.getChecksum().equals(fileInfo.getChecksum())) {
            if (!binaryProvider.exists(fileInfo.getChecksum())) {
              binaryProvider.put(fileInfo.getChecksum(), inputStream);
            }
            updated = true;
          }
          if (updated && dataStorage.sharedChecksum(oldFile.getChecksum()) ==1) {
            dataStorage.createOrphanFile(oldFile);
          }
          if (binaryProvider.exists(fileInfo.getChecksum())) {
            createdFileInfoEntity = dataStorage.updateFileInfo(fileInfo);
            return createdFileInfoEntity;
          } else {
            throw new FileStorageException("Error while writing file " + fileInfo.getName());
          }
        } catch (Exception e) {
          try {
            binaryProvider.remove(fileInfo.getChecksum());
          } catch (IOException e1) {
            LOG.error("Error while rollback writing file");
          }
          throw new FileStorageException("Error while writing file " + fileInfo.getName(), e);
        }
      }
      return null;
    }
  }

}
