/*
 * Copyright 2015 OpenCB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencb.opencga.catalog.utils;

import org.opencb.datastore.core.ObjectMap;
import org.opencb.datastore.core.QueryOptions;
import org.opencb.datastore.core.QueryResult;
import org.opencb.opencga.catalog.db.api.CatalogFileDBAdaptor;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.catalog.CatalogManager;
import org.opencb.opencga.catalog.models.File;
import org.opencb.opencga.catalog.exceptions.CatalogDBException;
import org.opencb.opencga.catalog.io.CatalogIOManager;
import org.opencb.opencga.catalog.exceptions.CatalogIOException;
import org.opencb.opencga.core.common.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Jacobo Coll &lt;jacobo167@gmail.com&gt;
 */
public class CatalogFileUtils {

    private final CatalogManager catalogManager;
    private static Logger logger = LoggerFactory.getLogger(CatalogFileUtils.class);

    public CatalogFileUtils(CatalogManager catalogManager) {
        this.catalogManager = catalogManager;
    }

    public void upload(URI sourceUri, File file, String sourceChecksum, String sessionId,
                       boolean ignoreStatus, boolean overwrite, boolean deleteSource, boolean calculateChecksum)
            throws CatalogException {
        upload(sourceUri, file, sourceChecksum, sessionId, ignoreStatus, overwrite, deleteSource, calculateChecksum, 10000000);
    }
    /**
     * Upload file to a created entry file in Catalog.
     *
     *
     *
     * @param sourceUri         File URI to be moved into Catalog workspace
     * @param file              File from Catalog
     * @param sourceChecksum    Optional: Source checksum
     * @param sessionId         Valid sessionID to modify the File in Catalog
     * @param ignoreStatus      Ignore the status (uploading, uploaded, ready) from Catalog
     * @param overwrite         Overwrite if there is a file in the target
     * @param deleteSource      After moving, delete file. If false, force copy.
     * @param calculateChecksum Calculate checksum
     * @param moveThreshold     File size threshold to move a file instead of copy.
     * @throws org.opencb.opencga.catalog.exceptions.CatalogIOException
     */
    public void upload(URI sourceUri, File file, String sourceChecksum, String sessionId,
                       boolean ignoreStatus, boolean overwrite, boolean deleteSource, boolean calculateChecksum, long moveThreshold)
            throws CatalogException {

        URI targetUri;
        CatalogIOManager sourceIOManager;
        CatalogIOManager targetIOManager;
        try {
            targetUri = catalogManager.getFileUri(file);
            sourceIOManager = catalogManager.getCatalogIOManagerFactory().get(sourceUri.getScheme());
            targetIOManager = catalogManager.getCatalogIOManagerFactory().get(targetUri.getScheme());
        } catch (CatalogDBException | CatalogIOException e) {
            throw new CatalogIOException("Can't upload file.", e);
        }

        if (deleteSource) {
            //TODO: Check if can delete source
        }

        //If the source is equals to the target, calculate checksum (if needed) and exit
        if (sourceUri.equals(targetUri)) {
            logger.info("SourceURI equals to TargetURI. Only calculate checksum and update file entry");
            String targetChecksum;
            if (calculateChecksum) {
                targetChecksum = targetIOManager.calculateChecksum(targetUri);
            } else {
                targetChecksum = sourceChecksum;
            }
            updateFileAttributes(file, targetChecksum, targetUri, new ObjectMap("status", File.Status.READY), sessionId);
            return;
        }

        //Check status
        if (!ignoreStatus) {
            checkStatus(file);
        }

        //Check if there is any file in target
        checkTarget(file, targetUri, targetIOManager, overwrite);

        // Get file stats
        long size = sourceIOManager.getFileSize(sourceUri);

        //Calculate source checksum
        if (sourceChecksum == null || sourceChecksum.isEmpty()) {
            if (calculateChecksum) {
                sourceChecksum = sourceIOManager.calculateChecksum(sourceUri);
            } else {    //Do not calculate checksum.
                sourceChecksum = "null";
            }
        }

        //Move or copy file
        boolean fileMoved = false;
        String targetChecksum = "";
        if (size < moveThreshold && sourceIOManager == targetIOManager && deleteSource) {
            try {
                logger.info("Moving file {} -> {}", sourceUri, targetUri);
                sourceIOManager.moveFile(sourceUri, targetUri);
                targetChecksum = sourceChecksum;
                fileMoved = true;
            } catch (IOException | CatalogIOException e) {
                e.printStackTrace();
                logger.error("Error moving a file.", e);
            }
        }

        //If there was an error moving file or have to be copied
        if (!fileMoved) {
            try {
                copy(sourceIOManager, sourceUri, targetIOManager, targetUri);
            } catch (CatalogIOException | IOException e) {
                throw new CatalogIOException("Error while copying file. ", e);
            }

            try {
                if (calculateChecksum) {
                    targetChecksum = targetIOManager.calculateChecksum(targetUri);
                } else {
                    targetChecksum = sourceChecksum;
                }
            } catch (CatalogIOException catalogIOException) {
                try {
                    logger.info("Checksum fail. Delete target file.");
                    targetIOManager.deleteFile(targetUri);
                } catch (CatalogIOException e) {
                    e.printStackTrace();
                    //You fail at failing!
                    throw new CatalogIOException(
                            "Fail calculating target checksum : " + catalogIOException.getMessage() + "" +
                            "Fail deleting target file : " + e.getMessage(), catalogIOException);
                }
                throw catalogIOException;
            }
        }

        //Check status
        if(!calculateChecksum || targetChecksum.equals(sourceChecksum)) {
            if (calculateChecksum) {
                logger.info("Checksum matches {}", sourceChecksum);
            } else {
                logger.info("Checksum not computed.");
            }

            updateFileAttributes(file, sourceChecksum, targetUri, new ObjectMap("status", File.Status.READY), sessionId);

            if(deleteSource && !fileMoved) {
                logger.info("Deleting source file {} after moving", sourceUri);
                try {
                    sourceIOManager.deleteFile(sourceUri);
                } catch (CatalogIOException e) {
                    throw e;
                }
            }
        } else {
            throw new CatalogIOException("Checksum mismatches at moving files. " + sourceChecksum + " =! " + targetChecksum);
        }

    }

    public void upload(InputStream inputStream, File file, String sessionId, boolean ignoreStatus,
                       boolean overwrite, boolean calculateChecksum) throws CatalogException {
        URI targetUri;
        CatalogIOManager targetIOManager;
        try {
            targetUri = catalogManager.getFileUri(file);
            targetIOManager = catalogManager.getCatalogIOManagerFactory().get(targetUri.getScheme());
        } catch (CatalogDBException | CatalogIOException e) {
            throw new CatalogIOException("Can't upload file.", e);
        }


        //Check status
        if (!ignoreStatus) {
            checkStatus(file);
        }

        //Check if there is any file in target
        checkTarget(file, targetUri, targetIOManager, overwrite);


        try {
            targetIOManager.createFile(targetUri, inputStream);
        } catch (CatalogIOException e) {
            e.printStackTrace();
        }


        String checksum = null;
        if (calculateChecksum) {
            try {
                checksum = targetIOManager.calculateChecksum(targetUri);
            } catch (CatalogIOException e) {
                e.printStackTrace();
            }
        }

        updateFileAttributes(file, checksum, targetUri, new ObjectMap("status", File.Status.READY), sessionId);

    }


    public File link(File file, boolean calculateChecksum, URI externalUri, boolean relink, String sessionId) throws CatalogException {
        ParamUtils.checkObj(file, "file");
        ParamUtils.checkObj(externalUri, "externalUri");
        ParamUtils.checkParameter(sessionId, "sessionId");

        File.Status fileStatus = file.getStatus();
        if (!fileStatus.equals(File.Status.STAGE) && !fileStatus.equals(File.Status.MISSING)) {
            if (relink) {
                if (!fileStatus.equals(File.Status.READY)) {
                    throw new CatalogIOException("Unable to relink a file with status : " + fileStatus);
                }
                if (!catalogManager.isExternal(file)) {
                    throw new CatalogIOException("Unable to relink a non linked file");
                }
            } else {
                throw new CatalogIOException("Unable to create a link a file with status : " + fileStatus);
            }
        }

        CatalogIOManager ioManager = catalogManager.getCatalogIOManagerFactory().get(externalUri);

        if (file.getType() == File.Type.FOLDER) {
            logger.info("Linking a folder");

            int studyId = catalogManager.getStudyIdByFileId(file.getId());
            List<File> files = new LinkedList<>();
            List<URI> uris = ioManager.listFiles(externalUri);
            Map<URI, String> uriStringMap = new HashMap<>();

            CatalogFileUtils catalogFileUtils = new CatalogFileUtils(catalogManager);
            Path folderPath = Paths.get(file.getPath());
            for (URI uri : uris) {
                if (ioManager.isDirectory(uri)) {
                    continue;       //Skip directories. Will be created automatically with "parents = true"
                }
                String relativePath = folderPath.resolve(externalUri.relativize(uri).getPath()).toString();
                uriStringMap.put(uri, relativePath);
            }

            //Search if there is any existing file in the folder with the path to use.
            QueryOptions pathsQuery = new QueryOptions(CatalogFileDBAdaptor.FileFilterOption.path.toString(),
                    new LinkedList<>(uriStringMap.values()));
            List<File> result = catalogManager.getAllFiles(studyId, pathsQuery, sessionId).getResult();
            if (result.size() != 0) {
                for (File f : result) {
                    logger.warn("File already existing: { id:{}, path:\"{}\"}", f.getId(), f.getPath());
                }
                throw new CatalogException("Unable to link folder {} to uri {}. Existing files on folder");
            }

            //Set URI to folder. This will mark the directory as "external"
            catalogManager.modifyFile(file.getId(), new ObjectMap("uri", externalUri), sessionId);

            //Create and link files.
            for (Map.Entry<URI, String> entry : uriStringMap.entrySet()) {
                String relativePath = entry.getValue();
                URI uri = entry.getKey();
                logger.info("Adding file \"{}\"", relativePath);
                //Create new file. Parents = true to create folders. Parameter "parents" should not be used here, it's
                //only related to the main folder creation.
                File newFile = catalogManager.createFile(studyId, null, null, relativePath, "", true, -1, sessionId).first();
//                files.add(catalogFileUtils.link(newFile, calculateChecksum, uri, false, sessionId));
                upload(uri, newFile, null, sessionId, false, false, false, calculateChecksum);
//                files.add();
            }

            ObjectMap objectMap = new ObjectMap();
            objectMap.put("status", File.Status.READY);
            updateFileAttributes(file, null, externalUri, objectMap, sessionId);
            return catalogManager.getFile(file.getId(), sessionId).first();
        } else {

            String checksum = null;
            if (calculateChecksum) {
                try {
                    checksum = ioManager.calculateChecksum(externalUri);
                } catch (CatalogIOException e) {
                    e.printStackTrace();
                }
            }

            ObjectMap objectMap = new ObjectMap("uri", externalUri);
            objectMap.put("status", File.Status.READY);
            updateFileAttributes(file, checksum, externalUri, objectMap, sessionId);
            return catalogManager.getFile(file.getId(), sessionId).first();
        }
    }

    public void delete(int fileId, String sessionId) throws CatalogException {
        delete(catalogManager.getFile(fileId, sessionId).first(), sessionId);
    }
    public void delete(File file, String sessionId) throws CatalogException {
        ParamUtils.checkObj(file, "file");

        if (!file.getStatus().equals(File.Status.TRASHED)) {
            throw new CatalogIOException("Only trashed files can be deleted");
        }
        int studyId = catalogManager.getStudyIdByFileId(file.getId());
        if (file.getType().equals(File.Type.FOLDER)) {
            List<File> files = catalogManager.getAllFiles(studyId,
                    new QueryOptions(CatalogFileDBAdaptor.FileFilterOption.path.toString(),
                            "~" + file.getPath() + "..*"), sessionId).getResult();
            for (File f : files) {
                if (f.getStatus() != File.Status.TRASHED && f.getStatus() != File.Status.DELETED) {
                    throw new CatalogIOException("Only trashed files can be deleted");
                }
            }
            for (File f : files) {
                if (f.getType() == File.Type.FILE && f.getStatus() != File.Status.DELETED) {
                    delete(f, sessionId);
                }
            }
            List<File> folders = files.stream().filter(f -> f.getType() == File.Type.FOLDER && f.getStatus() != File.Status.DELETED)
                    .sorted((f1, f2) -> f2.getPath().length() - f1.getPath().length()).collect(Collectors.toList());
            for (File folder : folders) {
                delete(folder, sessionId);
            }
        }

        if (!catalogManager.isExternal(file)) { //Do not delete file if is external
            URI fileUri = catalogManager.getFileUri(file);
            CatalogIOManager ioManager = catalogManager.getCatalogIOManagerFactory().get(fileUri);
            if (ioManager.isDirectory(fileUri)) {
                if (!ioManager.listFiles(fileUri).isEmpty()) {
                    throw new CatalogIOException("Unable to delete folder " + fileUri + ". Folder is not empty!");
                }
            }
            ioManager.deleteFile(fileUri);
        }
        catalogManager.modifyFile(file.getId(), new ObjectMap("status", File.Status.DELETED), sessionId);

    }

    /**
     * Update some file attributes.
     *      diskUsage
     *      modificationDate
     *      attributes.checksum
     * @param file              File to update
     * @param calculateChecksum Do calculate checksum
     * @param sessionId         users sessionId
     * @throws CatalogException
     */
    public void updateFileAttributes(File file, boolean calculateChecksum, String sessionId) throws CatalogException {
        URI fileUri = catalogManager.getFileUri(file);
        String checksum = null;
        if (calculateChecksum) {
            checksum = catalogManager.getCatalogIOManagerFactory().get(fileUri).calculateChecksum(fileUri);
        }
        updateFileAttributes(file, checksum, null, null, sessionId);
    }

    /**
     * Get a ObjectMap with some fields if they have been modified.
     *      diskUsage
     *      modificationDate
     *      attributes.checksum
     *
     * @param fileUri If null, calls to getFileUri()
     *
     * TODO: Lazy checksum: Only calculate checksum if the diskUsage has changed.
     * @param calculateChecksum     Calculate checksum to check if have changed
     * @throws CatalogException
     */
    public ObjectMap getModifiedFileAttributes(File file, URI fileUri, boolean calculateChecksum) throws CatalogException {
        if (fileUri == null) {
            fileUri = catalogManager.getFileUri(file);
        }
        String checksum = null;
        if (calculateChecksum) {
            checksum = catalogManager.getCatalogIOManagerFactory().get(fileUri).calculateChecksum(fileUri);
        }
        return getModifiedFileAttributes(file, checksum, fileUri, null);
    }


    /**
     * Update some file attributes.
     *      diskUsage
     *      modificationDate
     *      attributes.checksum
     * @throws CatalogException
     */
    private void updateFileAttributes(File file, String checksum, URI fileUri, ObjectMap parameters, String sessionId)
            throws CatalogException {
        parameters = getModifiedFileAttributes(file, checksum, fileUri, parameters);

        //Update file
        try {
            if (!parameters.isEmpty()) {    //If there is something to update
                catalogManager.modifyFile(file.getId(), parameters, sessionId);
            }
        } catch (CatalogException e) {
            throw new CatalogIOException("Can't update file properties in Catalog.", e);
        }
    }

    /**
     * Get a ObjectMap with some fields if they have been modified.
     *      diskUsage
     *      modificationDate
     *      attributes.checksum
     *
     * @throws CatalogException
     */
    private ObjectMap getModifiedFileAttributes(File file, String checksum, URI fileUri, ObjectMap parameters) throws CatalogException {
        parameters = ParamUtils.defaultObject(parameters, ObjectMap::new);
        if (fileUri == null) {
            fileUri = catalogManager.getFileUri(file);
        }
        CatalogIOManager catalogIOManager = catalogManager.getCatalogIOManagerFactory().get(fileUri);

        if (checksum != null && !checksum.isEmpty() && !checksum.equals("null")) {
            if (file.getAttributes() == null || !Objects.equals(file.getAttributes().get("checksum"), checksum)) {
                parameters.put("attributes", new ObjectMap("checksum", checksum));
            }
        }

        try {
            long size = catalogIOManager.getFileSize(fileUri);
            if (file.getDiskUsage() != size) {
                parameters.put("diskUsage", size);
            }
        } catch (CatalogIOException e) {
            e.printStackTrace();
            logger.error("Can't get fileSize", e);
        }

        String modificationDate = TimeUtils.getTime(catalogIOManager.getModificationDate(fileUri));
        if (!modificationDate.equals(file.getModificationDate())) {
            parameters.put("modificationDate", modificationDate);
        }

        return parameters;
    }

    /**
     * Check if there is any file in the URI target.
     *
     * @throws org.opencb.opencga.catalog.exceptions.CatalogIOException
     */
    private void checkTarget(File file, URI targetUri, CatalogIOManager targetIOManager, boolean overwrite) throws CatalogIOException {
        if (!overwrite && targetIOManager.exists(targetUri)) {
            throw new CatalogIOException("There is a file in the target!" +
                    "file:{id:" + file.getId() + ", targetUri: '" + targetUri + "' } " +
                    "Needs 'overwrite = true' for continue.");
        }
    }

    /**
     * Check status. To upload a fileObject, the catalog file entry must be in status "uploading".
     * Set "ignoreStatus = true" to ignore the actual file status and replace the content.
     *
     * @throws org.opencb.opencga.catalog.exceptions.CatalogIOException
     */
    private void checkStatus(File file) throws CatalogIOException {
        if (file.getStatus() != File.Status.STAGE) {
            throw new CatalogIOException("File status is already uploaded and ready! " +
                    "file:{id:" + file.getId() + ", status: '" + file.getStatus() + "' } " +
                    "Needs 'ignoreStatus = true' for continue.");
        }
    }

    /**
     *
     * @param sourceIOManager   Source IOManager
     * @param source            Source file
     * @param targetIOManager   Target IOManager
     * @param target            Target file
     * @throws IOException
     * @throws org.opencb.opencga.catalog.exceptions.CatalogIOException
     */
    private void copy(CatalogIOManager sourceIOManager, URI source, CatalogIOManager targetIOManager, URI target)
            throws IOException, CatalogIOException {

        logger.info("Coping file from {} to {}", source, target);
        //If it's the same IOManager, copy intra FS
        if(sourceIOManager == targetIOManager) {
            sourceIOManager.copyFile(source, target);
            return;
        }

        //TODO: Implement copies inter FS
//        HDFS -> POSIX
//        POSIX -> HDFS

        logger.info("Unable to copy directly from {} to {} . Doing manual copy.", source.getScheme(), target.getScheme());
        DataInputStream fileObject = sourceIOManager.getFileObject(source, -1, -1);
        targetIOManager.createFile(target, fileObject);

        //throw new CatalogIOManagerException("Unable to copy from " + source.getScheme() + " to " + target.getScheme());
    }

}
