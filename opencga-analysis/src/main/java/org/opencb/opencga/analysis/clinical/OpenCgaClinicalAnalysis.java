package org.opencb.opencga.analysis.clinical;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.opencb.biodata.models.alignment.RegionCoverage;
import org.opencb.biodata.models.clinical.interpretation.ClinicalProperty;
import org.opencb.biodata.models.clinical.interpretation.DiseasePanel;
import org.opencb.biodata.models.clinical.interpretation.ReportedLowCoverage;
import org.opencb.biodata.models.core.Exon;
import org.opencb.biodata.models.core.Gene;
import org.opencb.biodata.models.core.Region;
import org.opencb.biodata.models.core.Transcript;
import org.opencb.cellbase.client.rest.CellBaseClient;
import org.opencb.commons.datastore.core.*;
import org.opencb.commons.utils.ListUtils;
import org.opencb.opencga.analysis.AnalysisResult;
import org.opencb.opencga.analysis.OpenCgaAnalysis;
import org.opencb.opencga.analysis.exceptions.AnalysisException;
import org.opencb.opencga.catalog.db.api.FileDBAdaptor;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.core.models.ClinicalAnalysis;
import org.opencb.opencga.core.models.File;
import org.opencb.opencga.core.models.Individual;
import org.opencb.opencga.storage.core.StorageEngineFactory;
import org.opencb.opencga.storage.core.manager.AlignmentStorageManager;

import java.util.*;

public abstract class OpenCgaClinicalAnalysis<T> extends OpenCgaAnalysis<T> {

    public final static int LOW_COVERAGE_DEFAULT = 20;

    public final static String INCLUDE_LOW_COVERAGE_PARAM = "includeLowCoverage";
    public final static String MAX_LOW_COVERAGE_PARAM = "maxLowCoverage";

    protected String clinicalAnalysisId;

    protected Map<String, ClinicalProperty.RoleInCancer> roleInCancer;
    protected Map<String, List<String>> actionableVariants;

    protected ObjectMap config;

    @Deprecated
    protected int maxCoverage;

    protected CellBaseClient cellBaseClient;
    protected AlignmentStorageManager alignmentStorageManager;

    public OpenCgaClinicalAnalysis(String clinicalAnalysisId, Map<String, ClinicalProperty.RoleInCancer> roleInCancer, Map<String, List<String>> actionableVariants, ObjectMap config, String opencgaHome, String studyStr, String token) {
        super(opencgaHome, studyStr, token);

        this.clinicalAnalysisId = clinicalAnalysisId;

        this.actionableVariants = actionableVariants;
        this.roleInCancer = roleInCancer;

        this.config = config != null ? config : new ObjectMap();

        this.maxCoverage = 20;

        this.cellBaseClient = new CellBaseClient(storageConfiguration.getCellbase().toClientConfiguration());
        this.alignmentStorageManager = new AlignmentStorageManager(catalogManager, StorageEngineFactory.get(storageConfiguration));
    }

    @Override
    public abstract AnalysisResult<T> execute() throws Exception;

    protected ClinicalAnalysis getClinicalAnalysis() throws AnalysisException {
        QueryResult<ClinicalAnalysis> clinicalAnalysisQueryResult;
        try {
            clinicalAnalysisQueryResult = catalogManager.getClinicalAnalysisManager()
                    .get(studyStr, clinicalAnalysisId, QueryOptions.empty(), token);
        } catch (CatalogException e) {
            throw new AnalysisException(e.getMessage(), e);
        }
        if (clinicalAnalysisQueryResult.getNumResults() == 0) {
            throw new AnalysisException("Clinical analysis " + clinicalAnalysisId + " not found in study " + studyStr);
        }

        ClinicalAnalysis clinicalAnalysis = clinicalAnalysisQueryResult.first();

        if (clinicalAnalysis.getProband() == null || StringUtils.isEmpty(clinicalAnalysis.getProband().getId())) {
            throw new AnalysisException("Missing proband in clinical analysis " + clinicalAnalysisId);
        }

        return clinicalAnalysis;
    }

    protected Individual getProband(ClinicalAnalysis clinicalAnalysis) throws AnalysisException {
        Individual proband = clinicalAnalysis.getProband();

        String clinicalAnalysisId = clinicalAnalysis.getId();
        // Sanity checks
        if (proband == null) {
            throw new AnalysisException("Missing proband in clinical analysis " + clinicalAnalysisId);
        }

        if (ListUtils.isEmpty(proband.getSamples())) {
            throw new AnalysisException("Missing samples in proband " + proband.getId() + " in clinical analysis " + clinicalAnalysisId);
        }

        if (proband.getSamples().size() > 1) {
            throw new AnalysisException("Found more than one sample for proband " + proband.getId() + " in clinical analysis "
                    + clinicalAnalysisId);
        }

        // Fill with parent information
        String fatherId = null;
        String motherId = null;
        if (proband.getFather() != null && StringUtils.isNotEmpty(proband.getFather().getId())) {
            fatherId = proband.getFather().getId();
        }
        if (proband.getMother() != null && StringUtils.isNotEmpty(proband.getMother().getId())) {
            motherId = proband.getMother().getId();
        }
        if (fatherId != null && motherId != null && clinicalAnalysis.getFamily() != null
                && ListUtils.isNotEmpty(clinicalAnalysis.getFamily().getMembers())) {
            for (Individual member : clinicalAnalysis.getFamily().getMembers()) {
                if (member.getId().equals(fatherId)) {
                    proband.setFather(member);
                } else if (member.getId().equals(motherId)) {
                    proband.setMother(member);
                }
            }
        }

        return proband;
    }


    protected List<String> getSampleNames(ClinicalAnalysis clinicalAnalysis) throws AnalysisException {
        return getSampleNames(clinicalAnalysis, null);
    }

    protected List<String> getSampleNames(ClinicalAnalysis clinicalAnalysis, Individual proband) throws AnalysisException {
        List<String> sampleList = new ArrayList<>();
        // Sanity check
        if (clinicalAnalysis != null && clinicalAnalysis.getFamily() != null
                && CollectionUtils.isNotEmpty(clinicalAnalysis.getFamily().getMembers())) {

            Map<String, Individual> individualMap = new HashMap<>();
            for (Individual member : clinicalAnalysis.getFamily().getMembers()) {
                if (ListUtils.isEmpty(member.getSamples())) {
//                    throw new AnalysisException("No samples found for member " + member.getId());
                    continue;
                }
                if (member.getSamples().size() > 1) {
                    throw new AnalysisException("More than one sample found for member " + member.getId());
                }
                sampleList.add(member.getSamples().get(0).getId());
                individualMap.put(member.getId(), member);
            }

            if (proband != null) {
                // Fill proband information to be able to navigate to the parents and their samples easily
                // Sanity check
                if (proband.getFather() != null && StringUtils.isNotEmpty(proband.getFather().getId())
                        && individualMap.containsKey(proband.getFather().getId())) {
                    proband.setFather(individualMap.get(proband.getFather().getId()));
                }
                if (proband.getMother() != null && StringUtils.isNotEmpty(proband.getMother().getId())
                        && individualMap.containsKey(proband.getMother().getId())) {
                    proband.setMother(individualMap.get(proband.getMother().getId()));
                }
            }
        }
        return sampleList;
    }

    protected Map<String, String> getSampleMap(ClinicalAnalysis clinicalAnalysis, Individual proband) throws AnalysisException {
        Map<String, String> individualSampleMap = new HashMap<>();
        // Sanity check
        if (clinicalAnalysis != null && clinicalAnalysis.getFamily() != null
                && CollectionUtils.isNotEmpty(clinicalAnalysis.getFamily().getMembers())) {

            Map<String, Individual> individualMap = new HashMap<>();
            for (Individual member : clinicalAnalysis.getFamily().getMembers()) {
                if (ListUtils.isEmpty(member.getSamples())) {
//                    throw new AnalysisException("No samples found for member " + member.getId());
                    continue;
                }
                if (member.getSamples().size() > 1) {
                    throw new AnalysisException("More than one sample found for member " + member.getId());
                }
                individualSampleMap.put(member.getId(), member.getSamples().get(0).getId());
                individualMap.put(member.getId(), member);
            }

            if (proband != null) {
                // Fill proband information to be able to navigate to the parents and their samples easily
                // Sanity check
                if (proband.getFather() != null && StringUtils.isNotEmpty(proband.getFather().getId())
                        && individualMap.containsKey(proband.getFather().getId())) {
                    proband.setFather(individualMap.get(proband.getFather().getId()));
                }
                if (proband.getMother() != null && StringUtils.isNotEmpty(proband.getMother().getId())
                        && individualMap.containsKey(proband.getMother().getId())) {
                    proband.setMother(individualMap.get(proband.getMother().getId()));
                }
            }
        }
        return individualSampleMap;
    }

    protected List<ReportedLowCoverage> getReportedLowCoverage(ClinicalAnalysis clinicalAnalysis, List<DiseasePanel> diseasePanels)
            throws AnalysisException {
        String clinicalAnalysisId = clinicalAnalysis.getId();

        // Sanity check
        if (clinicalAnalysis.getProband() == null || CollectionUtils.isEmpty(clinicalAnalysis.getProband().getSamples())) {
            throw new AnalysisException("Missing proband when computing reported low coverage");
        }
        String probandId;
        try {
            probandId = clinicalAnalysis.getProband().getSamples().get(0).getId();
        } catch (Exception e) {
            throw new AnalysisException("Missing proband when computing reported low coverage", e);
        }

        // Reported low coverage map
        List<ReportedLowCoverage> reportedLowCoverages = new ArrayList<>();

        Set<String> lowCoverageByGeneDone = new HashSet<>();

        // Look for the bam file of the proband
        QueryResult<File> fileQueryResult;
        try {
            fileQueryResult = catalogManager.getFileManager().get(studyStr, new Query()
                            .append(FileDBAdaptor.QueryParams.SAMPLES.key(), probandId)
                            .append(FileDBAdaptor.QueryParams.FORMAT.key(), File.Format.BAM),
                    new QueryOptions(QueryOptions.INCLUDE, FileDBAdaptor.QueryParams.UUID.key()), token);
        } catch (CatalogException e) {
            throw new AnalysisException(e.getMessage(), e);
        }
        if (fileQueryResult.getNumResults() > 1) {
            throw new AnalysisException("More than one BAM file found for proband " + probandId + " in clinical analysis "
                    + clinicalAnalysisId);
        }

        String bamFileId = fileQueryResult.getNumResults() == 1 ? fileQueryResult.first().getUuid() : null;

        if (bamFileId != null) {
            for (DiseasePanel diseasePanel : diseasePanels) {
                for (DiseasePanel.GenePanel genePanel : diseasePanel.getGenes()) {
                    String geneName = genePanel.getId();
                    if (!lowCoverageByGeneDone.contains(geneName)) {
                        reportedLowCoverages.addAll(getReportedLowCoverages(geneName, bamFileId, maxCoverage));
                        lowCoverageByGeneDone.add(geneName);
                    }
                }
            }
        }

        return reportedLowCoverages;
    }

    protected List<ReportedLowCoverage> getReportedLowCoverages(String geneName, String bamFileId, int maxCoverage) {
        List<ReportedLowCoverage> reportedLowCoverages = new ArrayList<>();
        try {
            // Get gene exons from CellBase
            QueryResponse<Gene> geneQueryResponse = cellBaseClient.getGeneClient().get(Collections.singletonList(geneName),
                    QueryOptions.empty());
            List<RegionCoverage> regionCoverages;
            for (Transcript transcript: geneQueryResponse.getResponse().get(0).first().getTranscripts()) {
                for (Exon exon: transcript.getExons()) {
                    regionCoverages = alignmentStorageManager.getLowCoverageRegions(studyStr, bamFileId,
                            new Region(exon.getChromosome(), exon.getStart(), exon.getEnd()), maxCoverage, token).getResult();
                    for (RegionCoverage regionCoverage: regionCoverages) {
                        ReportedLowCoverage reportedLowCoverage = new ReportedLowCoverage(regionCoverage)
                                .setGeneName(geneName)
                                .setId(exon.getId());
                        reportedLowCoverages.add(reportedLowCoverage);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error getting low coverage regions for panel genes.", e.getMessage());
        }
        // And for that exon regions, get low coverage regions
        return reportedLowCoverages;
    }
}