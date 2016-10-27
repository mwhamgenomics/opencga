/*
 * Copyright 2015-2016 OpenCB
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

package org.opencb.opencga.app.cli.analysis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ga4gh.Reads;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.commons.lang3.StringUtils;
import org.ga4gh.models.ReadAlignment;
import org.opencb.biodata.formats.feature.gff.Gff;
import org.opencb.biodata.formats.feature.gff.io.GffReader;
import org.opencb.biodata.formats.io.FileFormatException;
import org.opencb.biodata.models.core.Region;
import org.opencb.commons.datastore.core.*;
import org.opencb.opencga.analysis.AnalysisExecutionException;
import org.opencb.opencga.analysis.storage.AnalysisFileIndexer;
import org.opencb.opencga.analysis.variant.AbstractFileIndexer;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.catalog.models.DataStore;
import org.opencb.opencga.catalog.models.File;
import org.opencb.opencga.catalog.models.Job;
import org.opencb.opencga.catalog.models.Study;
import org.opencb.opencga.catalog.monitor.daemons.IndexDaemon;
import org.opencb.opencga.catalog.monitor.executors.old.ExecutorManager;
import org.opencb.opencga.client.rest.OpenCGAClient;
import org.opencb.opencga.server.grpc.AlignmentServiceGrpc;
import org.opencb.opencga.server.grpc.GenericAlignmentServiceModel;
import org.opencb.opencga.server.grpc.ServiceTypesModel;
import org.opencb.opencga.storage.core.StorageETLResult;
import org.opencb.opencga.storage.core.alignment.AlignmentStorageManager;
import org.opencb.opencga.storage.core.alignment.adaptors.AlignmentDBAdaptor;
import org.opencb.opencga.storage.core.exceptions.StorageETLException;
import org.opencb.opencga.storage.core.exceptions.StorageManagerException;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created on 09/05/16
 *
 * @author Jacobo Coll &lt;jacobo167@gmail.com&gt;
 */
public class AlignmentCommandExecutor extends AnalysisStorageCommandExecutor {
    private final AnalysisCliOptionsParser.AlignmentCommandOptions alignmentCommandOptions;
    private AlignmentStorageManager alignmentStorageManager;

    public AlignmentCommandExecutor(AnalysisCliOptionsParser.AlignmentCommandOptions options) {
        super(options.commonOptions);
        alignmentCommandOptions = options;
    }

    @Override
    public void execute() throws Exception {
        logger.debug("Executing variant command line");

        String subCommandString = alignmentCommandOptions.getParsedSubCommand();
        configure();
        switch (subCommandString) {
            case "index":
                index();
                break;
            case "query":
                query();
                break;
            case "query-grpc":
                queryGrpc();
                break;
            case "stats":
                stats();
                break;
            case "delete":
                delete();
                break;
            default:
                logger.error("Subcommand not valid");
                break;

        }
    }

    private void queryGrpc() throws InterruptedException {
        // We create the OpenCGA gRPC request object with the query, queryOptions, storageEngine and database
        Map<String, String> query = new HashMap<>();
        addParam(query, "fileId", alignmentCommandOptions.queryGRPCAlignmentCommandOptions.fileId);
        addParam(query, "sid", alignmentCommandOptions.queryGRPCAlignmentCommandOptions.commonOptions.sessionId);
        addParam(query, AlignmentDBAdaptor.QueryParams.REGION.key(), alignmentCommandOptions.queryGRPCAlignmentCommandOptions.region);
        addParam(query, AlignmentDBAdaptor.QueryParams.MIN_MAPQ.key(),
                alignmentCommandOptions.queryGRPCAlignmentCommandOptions.minMappingQuality);

        Map<String, String> queryOptions = new HashMap<>();
        addParam(queryOptions, AlignmentDBAdaptor.QueryParams.CONTAINED.key(),
                alignmentCommandOptions.queryGRPCAlignmentCommandOptions.contained);
        addParam(queryOptions, AlignmentDBAdaptor.QueryParams.MD_FIELD.key(),
                alignmentCommandOptions.queryGRPCAlignmentCommandOptions.mdField);
        addParam(queryOptions, AlignmentDBAdaptor.QueryParams.BIN_QUALITIES.key(),
                alignmentCommandOptions.queryGRPCAlignmentCommandOptions.binQualities);
        addParam(queryOptions, AlignmentDBAdaptor.QueryParams.LIMIT.key(), alignmentCommandOptions.queryGRPCAlignmentCommandOptions.limit);
        addParam(queryOptions, AlignmentDBAdaptor.QueryParams.SKIP.key(), alignmentCommandOptions.queryGRPCAlignmentCommandOptions.skip);

        GenericAlignmentServiceModel.Request request = GenericAlignmentServiceModel.Request.newBuilder()
                .putAllQuery(query)
                .putAllOptions(queryOptions)
                .build();

        // Connecting to the server host and port
        String[] split = clientConfiguration.getGrpc().getHost().split(":");
        String grpcServerHost = split[0];
        int grpcServerPort = 9091;
        if (split.length == 2) {
            grpcServerPort = Integer.parseInt(split[1]);
        }

        logger.debug("Connecting to gRPC server at {}:{}", grpcServerHost, grpcServerPort);

        // We create the gRPC channel to the specified server host and port
        ManagedChannel channel = ManagedChannelBuilder.forAddress(grpcServerHost, grpcServerPort)
                .usePlaintext(true)
                .build();


        // We use a blocking stub to execute the query to gRPC
        AlignmentServiceGrpc.AlignmentServiceBlockingStub serviceBlockingStub = AlignmentServiceGrpc.newBlockingStub(channel);

        if (alignmentCommandOptions.queryGRPCAlignmentCommandOptions.count) {
            ServiceTypesModel.LongResponse count = serviceBlockingStub.count(request);
            System.out.println("\nThe number of alignments is " + count.getValue() + "\n");
        } else {
            Iterator<Reads.ReadAlignment> alignmentIterator = serviceBlockingStub.get(request);
            int limit = alignmentCommandOptions.queryGRPCAlignmentCommandOptions.limit;
            if (limit > 0) {
                long cont = 0;
                while (alignmentIterator.hasNext() && cont < limit) {
                    Reads.ReadAlignment next = alignmentIterator.next();
                    cont++;
//                    System.out.println(next.toString());
                }
            } else {
                while (alignmentIterator.hasNext()) {
                    Reads.ReadAlignment next = alignmentIterator.next();
//                    System.out.println(next.toString());
                }
            }
        }

        channel.shutdown().awaitTermination(2, TimeUnit.SECONDS);
    }

    private void addParam(Map<String, String> map, String key, Object value) {
        if (value == null) {
            return;
        }

        if (value instanceof String) {
            if (!((String) value).isEmpty()) {
                map.put(key, (String) value);
            }
        } else if (value instanceof Integer) {
            map.put(key, Integer.toString((int) value));
        } else if (value instanceof Boolean) {
            map.put(key, Boolean.toString((boolean) value));
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private AlignmentStorageManager initAlignmentStorageManager(DataStore dataStore)
            throws CatalogException, IllegalAccessException, InstantiationException, ClassNotFoundException {

        String storageEngine = dataStore.getStorageEngine();
        if (StringUtils.isEmpty(storageEngine)) {
            this.alignmentStorageManager = storageManagerFactory.getAlignmentStorageManager();
        } else {
            this.alignmentStorageManager = storageManagerFactory.getAlignmentStorageManager(storageEngine);
        }
        return alignmentStorageManager;
    }

    @Deprecated
    private void index()
            throws CatalogException, AnalysisExecutionException, JsonProcessingException, IllegalAccessException, InstantiationException,
            ClassNotFoundException, StorageManagerException {
        AnalysisCliOptionsParser.IndexAlignmentCommandOptions cliOptions = alignmentCommandOptions.indexAlignmentCommandOptions;

        String sessionId = cliOptions.commonOptions.sessionId;
        long inputFileId = catalogManager.getFileId(cliOptions.fileId);

        // 1) Create, if not provided, an indexation job
        if (StringUtils.isEmpty(cliOptions.job.jobId)) {
            long outDirId;
            if (cliOptions.outdirId == null) {
                outDirId = catalogManager.getFileParent(inputFileId, null, sessionId).first().getId();
            } else  {
                outDirId = catalogManager.getFileId(cliOptions.outdirId);
            }

            AnalysisFileIndexer analysisFileIndexer = new AnalysisFileIndexer(catalogManager);

            List<String> extraParams = cliOptions.commonOptions.params.entrySet()
                    .stream()
                    .map(entry -> "-D" + entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.toList());

            QueryOptions options = new QueryOptions()
                    .append(ExecutorManager.EXECUTE, !cliOptions.job.queue)
                    .append(ExecutorManager.SIMULATE, false)
                    .append(AnalysisFileIndexer.TRANSFORM, cliOptions.transform)
                    .append(AnalysisFileIndexer.LOAD, cliOptions.load)
                    .append(AnalysisFileIndexer.PARAMETERS, extraParams)
                    .append(AnalysisFileIndexer.LOG_LEVEL, cliOptions.commonOptions.logLevel);

            QueryResult<Job> result = analysisFileIndexer.index(inputFileId, outDirId, sessionId, options);
            if (cliOptions.job.queue) {
                System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result));
            }

        } else {
            long studyId = catalogManager.getStudyIdByFileId(inputFileId);
            index(getJob(studyId, cliOptions.job.jobId, sessionId));
        }
    }

    @Deprecated
    private void index(Job job) throws CatalogException, IllegalAccessException, ClassNotFoundException, InstantiationException, StorageManagerException {

        AnalysisCliOptionsParser.IndexAlignmentCommandOptions cliOptions = alignmentCommandOptions.indexAlignmentCommandOptions;


        String sessionId = cliOptions.commonOptions.sessionId;
        long inputFileId = catalogManager.getFileId(cliOptions.fileId);

        // 1) Initialize VariantStorageManager
        long studyId = catalogManager.getStudyIdByFileId(inputFileId);
        Study study = catalogManager.getStudy(studyId, sessionId).first();

        /*
         * Getting VariantStorageManager
         * We need to find out the Storage Engine Id to be used from Catalog
         */
        DataStore dataStore = AbstractFileIndexer.getDataStore(catalogManager, studyId, File.Bioformat.ALIGNMENT, sessionId);
        initAlignmentStorageManager(dataStore);


        // 2) Read and validate cli args. Configure options
        ObjectMap alignmentOptions = alignmentStorageManager.getOptions();
        if (Integer.parseInt(cliOptions.fileId) != 0) {
            alignmentOptions.put(AlignmentStorageManager.Options.FILE_ID.key(), cliOptions.fileId);
        }

        alignmentOptions.put(AlignmentStorageManager.Options.DB_NAME.key(), dataStore.getDbName());

        if (cliOptions.commonOptions.params != null) {
            alignmentOptions.putAll(cliOptions.commonOptions.params);
        }

        alignmentOptions.put(AlignmentStorageManager.Options.PLAIN.key(), false);
        alignmentOptions.put(AlignmentStorageManager.Options.INCLUDE_COVERAGE.key(), cliOptions.calculateCoverage);
        if (cliOptions.meanCoverage != null && !cliOptions.meanCoverage.isEmpty()) {
            alignmentOptions.put(AlignmentStorageManager.Options.MEAN_COVERAGE_SIZE_LIST.key(), cliOptions.meanCoverage);
        }
        alignmentOptions.put(AlignmentStorageManager.Options.COPY_FILE.key(), false);
        alignmentOptions.put(AlignmentStorageManager.Options.ENCRYPT.key(), "null");
        logger.debug("Configuration options: {}", alignmentOptions.toJson());


        final boolean doExtract;
        final boolean doTransform;
        final boolean doLoad;
        StorageETLResult storageETLResult = null;
        Exception exception = null;

        File file = catalogManager.getFile(inputFileId, sessionId).first();
        URI inputUri = catalogManager.getFileUri(file);
//        FileUtils.checkFile(Paths.get(inputUri.getPath()));

//        URI outdirUri = job.getTmpOutDirUri();
        URI outdirUri = IndexDaemon.getJobTemporaryFolder(job.getId(), catalogConfiguration.getTempJobsDir()).toUri();
//        FileUtils.checkDirectory(Paths.get(outdirUri.getPath()));


        if (!cliOptions.load && !cliOptions.transform) {  // if not present --transform nor --load,
            // do both
            doExtract = true;
            doTransform = true;
            doLoad = true;
        } else {
            doExtract = cliOptions.transform;
            doTransform = cliOptions.transform;
            doLoad = cliOptions.load;
        }

        // 3) Execute indexation
        try {
            storageETLResult = alignmentStorageManager.index(Collections.singletonList(inputUri), outdirUri, doExtract, doTransform, doLoad).get(0);

        } catch (StorageETLException e) {
            storageETLResult = e.getResults().get(0);
            exception = e;
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            exception = e;
            e.printStackTrace();
            throw e;
        } finally {
            // 4) Save indexation result.
            // TODO: Uncomment this line
//            new ExecutionOutputRecorder(catalogManager, sessionId).saveStorageResult(job, storageETLResult);
        }
    }

    private void query() throws InterruptedException, CatalogException, IOException {
        ObjectMap objectMap = new ObjectMap();
        objectMap.putIfNotNull("fileId", alignmentCommandOptions.queryAlignmentCommandOptions.fileId);
        objectMap.putIfNotNull("sid", alignmentCommandOptions.queryAlignmentCommandOptions.commonOptions.sessionId);
        objectMap.putIfNotNull(AlignmentDBAdaptor.QueryParams.REGION.key(), alignmentCommandOptions.queryAlignmentCommandOptions.region);
        objectMap.putIfNotNull(AlignmentDBAdaptor.QueryParams.MIN_MAPQ.key(),
                alignmentCommandOptions.queryAlignmentCommandOptions.minMappingQuality);
        objectMap.putIfNotNull(AlignmentDBAdaptor.QueryParams.CONTAINED.key(),
                alignmentCommandOptions.queryAlignmentCommandOptions.contained);
        objectMap.putIfNotNull(AlignmentDBAdaptor.QueryParams.MD_FIELD.key(),
                alignmentCommandOptions.queryAlignmentCommandOptions.mdField);
        objectMap.putIfNotNull(AlignmentDBAdaptor.QueryParams.BIN_QUALITIES.key(),
                alignmentCommandOptions.queryAlignmentCommandOptions.binQualities);
        objectMap.putIfNotNull("count", alignmentCommandOptions.queryAlignmentCommandOptions.count);
        objectMap.putIfNotNull(AlignmentDBAdaptor.QueryParams.LIMIT.key(), alignmentCommandOptions.queryAlignmentCommandOptions.limit);
        objectMap.putIfNotNull(AlignmentDBAdaptor.QueryParams.SKIP.key(), alignmentCommandOptions.queryAlignmentCommandOptions.skip);

        OpenCGAClient openCGAClient = new OpenCGAClient(clientConfiguration);
        QueryResponse<ReadAlignment> alignments =
                openCGAClient.getFileClient().alignments(alignmentCommandOptions.queryAlignmentCommandOptions.fileId, objectMap);

        for (ReadAlignment readAlignment : alignments.allResults()) {
//            System.out.println(readAlignment);
        }
    }

//    private void query() throws FileFormatException, ClassNotFoundException, InstantiationException, CatalogException, IllegalAccessException, StorageManagerException, IOException, NoSuchMethodException {
//
//
//        AnalysisCliOptionsParser.QueryAlignmentCommandOptions cliOptions = alignmentCommandOptions.queryAlignmentCommandOptions;
//
//        String sessionId = cliOptions.commonOptions.sessionId;
//
//        long studyId;
//        if (StringUtils.isEmpty(cliOptions.study)) {
//            Map<Long, String> studyIds = getStudyIds(sessionId);
//            if (studyIds.size() != 1) {
//                throw new IllegalArgumentException("Missing study. Please select one from: " + studyIds.entrySet()
//                        .stream()
//                        .map(entry -> entry.getKey() + ":" + entry.getValue())
//                        .collect(Collectors.joining(",", "[", "]")));
//            }
//            else {
//                studyId = studyIds.entrySet().iterator().next().getKey();
//            }
//        } else {
//            studyId = catalogManager.getStudyId(cliOptions.study);
//        }
//
//        /*
//         * Getting VariantStorageManager
//         * We need to find out the Storage Engine Id to be used from Catalog
//         */
//        DataStore dataStore = AbstractFileIndexer.getDataStore(catalogManager, studyId, File.Bioformat.ALIGNMENT, sessionId);
//        initAlignmentStorageManager(dataStore);
//
//        AlignmentDBAdaptor dbAdaptor = alignmentStorageManager.getDBAdaptor(dataStore.getDbName());
//
//        /**
//         * Parse Regions
//         */
//        GffReader gffReader = null;
//        List<Region> regions = Collections.emptyList();
//        if (StringUtils.isNotEmpty(cliOptions.region)) {
//            regions = Region.parseRegions(cliOptions.region);
//            logger.debug("Processed regions: '{}'", regions);
//        } else if (StringUtils.isNotEmpty(cliOptions.regionFile)) {
//            gffReader = new GffReader(cliOptions.regionFile);
//            //throw new UnsupportedOperationException("Unsuppoted GFF file");
//        }
//
//        /**
//         * Parse QueryOptions
//         */
//        QueryOptions options = new QueryOptions();
//
//        if (cliOptions.fileId != null && !cliOptions.fileId.isEmpty()) {
//            long fileId = catalogManager.getFileId(cliOptions.fileId);
//            File file = catalogManager.getFile(fileId, sessionId).first();
//            URI fileUri = catalogManager.getFileUri(file);
//            options.add(AlignmentDBAdaptor.QO_FILE_ID, cliOptions.fileId);
//            options.add(AlignmentDBAdaptor.QO_BAM_PATH, fileUri.getPath());
//        }
//        options.add(AlignmentDBAdaptor.QO_INCLUDE_COVERAGE, cliOptions.coverage);
//        options.add(AlignmentDBAdaptor.QO_VIEW_AS_PAIRS, cliOptions.asPairs);
//        options.add(AlignmentDBAdaptor.QO_PROCESS_DIFFERENCES, cliOptions.processDifferences);
//        if (cliOptions.histogram) {
//            options.add(AlignmentDBAdaptor.QO_INCLUDE_COVERAGE, true);
//            options.add(AlignmentDBAdaptor.QO_HISTOGRAM, true);
//            options.add(AlignmentDBAdaptor.QO_INTERVAL_SIZE, cliOptions.histogram);
//        }
////        if (cliOptions.filePath != null && !cliOptions.filePath.isEmpty()) {
////            options.add(AlignmentDBAdaptor.QO_BAM_PATH, cliOptions.filePath);
////        }
//
//
//        if (cliOptions.stats != null && !cliOptions.stats.isEmpty()) {
//            for (String csvStat : cliOptions.stats) {
//                for (String stat : csvStat.split(",")) {
//                    int index = stat.indexOf("<");
//                    index = index >= 0 ? index : stat.indexOf("!");
//                    index = index >= 0 ? index : stat.indexOf("~");
//                    index = index >= 0 ? index : stat.indexOf("<");
//                    index = index >= 0 ? index : stat.indexOf(">");
//                    index = index >= 0 ? index : stat.indexOf("=");
//                    if (index < 0) {
//                        throw new UnsupportedOperationException("Unknown stat filter operation: " + stat);
//                    }
//                    String name = stat.substring(0, index);
//                    String cond = stat.substring(index);
//
//                    if (name.matches("")) {
//                        options.put(name, cond);
//                    } else {
//                        throw new UnsupportedOperationException("Unknown stat filter name: " + name);
//                    }
//                    logger.info("Parsed stat filter: {} {}", name, cond);
//                }
//            }
//        }
//
//
//        /**
//         * Run query
//         */
//        int subListSize = 20;
//        logger.info("options = {}", options.toJson());
//        if (cliOptions.histogram) {
//            for (Region region : regions) {
//                System.out.println(dbAdaptor.getAllIntervalFrequencies(region, options));
//            }
//        } else if (regions != null && !regions.isEmpty()) {
//            for (int i = 0; i < (regions.size() + subListSize - 1) / subListSize; i++) {
//                List<Region> subRegions = regions.subList(
//                        i * subListSize,
//                        Math.min((i + 1) * subListSize, regions.size()));
//
//                logger.info("subRegions = " + subRegions);
//                QueryResult queryResult = dbAdaptor.getAllAlignmentsByRegion(subRegions, options);
//                logger.info("{}", queryResult);
//                System.out.println(new ObjectMap("queryResult", queryResult).toJson());
//            }
//        } else if (gffReader != null) {
//            List<Gff> gffList;
//            List<Region> subRegions;
//            while ((gffList = gffReader.read(subListSize)) != null) {
//                subRegions = new ArrayList<>(subListSize);
//                for (Gff gff : gffList) {
//                    subRegions.add(new Region(gff.getSequenceName(), gff.getStart(), gff.getEnd()));
//                }
//
//                logger.info("subRegions = " + subRegions);
//                QueryResult queryResult = dbAdaptor.getAllAlignmentsByRegion(subRegions, options);
//                logger.info("{}", queryResult);
//                System.out.println(new ObjectMap("queryResult", queryResult).toJson());
//            }
//        } else {
//            throw new UnsupportedOperationException("Unable to fetch over all the genome");
////                System.out.println(dbAdaptor.getAllAlignments(options));
//        }
//
//    }

    private void stats() {
        throw new UnsupportedOperationException();
    }

    private void delete() {
        throw new UnsupportedOperationException();
    }


}
