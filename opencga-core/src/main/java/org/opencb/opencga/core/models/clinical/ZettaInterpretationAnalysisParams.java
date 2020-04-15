package org.opencb.opencga.core.models.clinical;

import org.opencb.opencga.core.tools.ToolParams;

import java.util.List;

public class ZettaInterpretationAnalysisParams extends ToolParams {
    public static final String DESCRIPTION = "Zetta interpretation analysis params";

    private String clinicalAnalysis;

    // Variant filters
    private List<String> id;
    private String region;
    private String type;

    // Study filters
    private String study;
    private String file;
    private String filter;
    private String qual;
    private String fileData;

    private String sample;
    private String genotype;
    private String sampleData;
    private String sampleAnnotation;
    private String sampleMetadata;
    private String unknownGenotype;

    private String cohort;
    private String cohortStatsRef;
    private String cohortStatsAlt;
    private String cohortStatsMaf;
    private String cohortStatsMgf;
    private String cohortStatsPass;
    private String missingAlleles;
    private String missingGenotypes;
    private String score;

    // Annotation filters
    private String annotationExists;
    private String gene;
    private String ct;
    private String xref;
    private String biotype;
    private String proteinSubstitution;
    private String conservation;
    private String populationFrequencyAlt;
    private String populationFrequencyRef;
    private String populationFrequencyMaf;
    private String transcriptFlag;
    private String geneTraitId;
    private String go;
    private String expression;
    private String proteinKeyword;
    private String drug;
    private String functionalScore;
    private String clinicalSignificance;
    private String customAnnotation;

    private String panel;

    private String trait;

    private int maxLowCoverage;
    private boolean includeLowCoverage;

    public ZettaInterpretationAnalysisParams() {
    }

    public ZettaInterpretationAnalysisParams(String clinicalAnalysis, List<String> id, String region, String type,
                                             String study, String file, String filter, String qual,
                                             String fileData, String sample, String genotype, String sampleData, String sampleAnnotation,
                                             String sampleMetadata, String unknownGenotype, String cohort, String cohortStatsRef,
                                             String cohortStatsAlt, String cohortStatsMaf, String cohortStatsMgf, String cohortStatsPass,
                                             String missingAlleles, String missingGenotypes, String score, String annotationExists,
                                             String gene, String ct, String xref, String biotype, String proteinSubstitution,
                                             String conservation, String populationFrequencyAlt, String populationFrequencyRef,
                                             String populationFrequencyMaf, String transcriptFlag, String geneTraitId, String go,
                                             String expression, String proteinKeyword, String drug, String functionalScore,
                                             String clinicalSignificance, String customAnnotation, String panel, String trait,
                                             int maxLowCoverage, boolean includeLowCoverage) {
        this.clinicalAnalysis = clinicalAnalysis;
        this.id = id;
        this.region = region;
        this.type = type;
        this.study = study;
        this.file = file;
        this.filter = filter;
        this.qual = qual;
        this.fileData = fileData;
        this.sample = sample;
        this.genotype = genotype;
        this.sampleData = sampleData;
        this.sampleAnnotation = sampleAnnotation;
        this.sampleMetadata = sampleMetadata;
        this.unknownGenotype = unknownGenotype;
        this.cohort = cohort;
        this.cohortStatsRef = cohortStatsRef;
        this.cohortStatsAlt = cohortStatsAlt;
        this.cohortStatsMaf = cohortStatsMaf;
        this.cohortStatsMgf = cohortStatsMgf;
        this.cohortStatsPass = cohortStatsPass;
        this.missingAlleles = missingAlleles;
        this.missingGenotypes = missingGenotypes;
        this.score = score;
        this.annotationExists = annotationExists;
        this.gene = gene;
        this.ct = ct;
        this.xref = xref;
        this.biotype = biotype;
        this.proteinSubstitution = proteinSubstitution;
        this.conservation = conservation;
        this.populationFrequencyAlt = populationFrequencyAlt;
        this.populationFrequencyRef = populationFrequencyRef;
        this.populationFrequencyMaf = populationFrequencyMaf;
        this.transcriptFlag = transcriptFlag;
        this.geneTraitId = geneTraitId;
        this.go = go;
        this.expression = expression;
        this.proteinKeyword = proteinKeyword;
        this.drug = drug;
        this.functionalScore = functionalScore;
        this.clinicalSignificance = clinicalSignificance;
        this.customAnnotation = customAnnotation;
        this.panel = panel;
        this.trait = trait;
        this.maxLowCoverage = maxLowCoverage;
        this.includeLowCoverage = includeLowCoverage;
    }

    public String getClinicalAnalysis() {
        return clinicalAnalysis;
    }

    public ZettaInterpretationAnalysisParams setClinicalAnalysis(String clinicalAnalysis) {
        this.clinicalAnalysis = clinicalAnalysis;
        return this;
    }

    public List<String> getId() {
        return id;
    }

    public ZettaInterpretationAnalysisParams setId(List<String> id) {
        this.id = id;
        return this;
    }

    public String getRegion() {
        return region;
    }

    public ZettaInterpretationAnalysisParams setRegion(String region) {
        this.region = region;
        return this;
    }

    public String getType() {
        return type;
    }

    public ZettaInterpretationAnalysisParams setType(String type) {
        this.type = type;
        return this;
    }

    public String getStudy() {
        return study;
    }

    public ZettaInterpretationAnalysisParams setStudy(String study) {
        this.study = study;
        return this;
    }

    public String getFile() {
        return file;
    }

    public ZettaInterpretationAnalysisParams setFile(String file) {
        this.file = file;
        return this;
    }

    public String getFilter() {
        return filter;
    }

    public ZettaInterpretationAnalysisParams setFilter(String filter) {
        this.filter = filter;
        return this;
    }

    public String getQual() {
        return qual;
    }

    public ZettaInterpretationAnalysisParams setQual(String qual) {
        this.qual = qual;
        return this;
    }

    public String getFileData() {
        return fileData;
    }

    public ZettaInterpretationAnalysisParams setFileData(String fileData) {
        this.fileData = fileData;
        return this;
    }

    public String getSample() {
        return sample;
    }

    public ZettaInterpretationAnalysisParams setSample(String sample) {
        this.sample = sample;
        return this;
    }

    public String getGenotype() {
        return genotype;
    }

    public ZettaInterpretationAnalysisParams setGenotype(String genotype) {
        this.genotype = genotype;
        return this;
    }

    public String getSampleData() {
        return sampleData;
    }

    public ZettaInterpretationAnalysisParams setSampleData(String sampleData) {
        this.sampleData = sampleData;
        return this;
    }

    public String getSampleAnnotation() {
        return sampleAnnotation;
    }

    public ZettaInterpretationAnalysisParams setSampleAnnotation(String sampleAnnotation) {
        this.sampleAnnotation = sampleAnnotation;
        return this;
    }

    public String getSampleMetadata() {
        return sampleMetadata;
    }

    public ZettaInterpretationAnalysisParams setSampleMetadata(String sampleMetadata) {
        this.sampleMetadata = sampleMetadata;
        return this;
    }

    public String getUnknownGenotype() {
        return unknownGenotype;
    }

    public ZettaInterpretationAnalysisParams setUnknownGenotype(String unknownGenotype) {
        this.unknownGenotype = unknownGenotype;
        return this;
    }

    public String getCohort() {
        return cohort;
    }

    public ZettaInterpretationAnalysisParams setCohort(String cohort) {
        this.cohort = cohort;
        return this;
    }

    public String getCohortStatsRef() {
        return cohortStatsRef;
    }

    public ZettaInterpretationAnalysisParams setCohortStatsRef(String cohortStatsRef) {
        this.cohortStatsRef = cohortStatsRef;
        return this;
    }

    public String getCohortStatsAlt() {
        return cohortStatsAlt;
    }

    public ZettaInterpretationAnalysisParams setCohortStatsAlt(String cohortStatsAlt) {
        this.cohortStatsAlt = cohortStatsAlt;
        return this;
    }

    public String getCohortStatsMaf() {
        return cohortStatsMaf;
    }

    public ZettaInterpretationAnalysisParams setCohortStatsMaf(String cohortStatsMaf) {
        this.cohortStatsMaf = cohortStatsMaf;
        return this;
    }

    public String getCohortStatsMgf() {
        return cohortStatsMgf;
    }

    public ZettaInterpretationAnalysisParams setCohortStatsMgf(String cohortStatsMgf) {
        this.cohortStatsMgf = cohortStatsMgf;
        return this;
    }

    public String getCohortStatsPass() {
        return cohortStatsPass;
    }

    public ZettaInterpretationAnalysisParams setCohortStatsPass(String cohortStatsPass) {
        this.cohortStatsPass = cohortStatsPass;
        return this;
    }

    public String getMissingAlleles() {
        return missingAlleles;
    }

    public ZettaInterpretationAnalysisParams setMissingAlleles(String missingAlleles) {
        this.missingAlleles = missingAlleles;
        return this;
    }

    public String getMissingGenotypes() {
        return missingGenotypes;
    }

    public ZettaInterpretationAnalysisParams setMissingGenotypes(String missingGenotypes) {
        this.missingGenotypes = missingGenotypes;
        return this;
    }

    public String getScore() {
        return score;
    }

    public ZettaInterpretationAnalysisParams setScore(String score) {
        this.score = score;
        return this;
    }

    public String getAnnotationExists() {
        return annotationExists;
    }

    public ZettaInterpretationAnalysisParams setAnnotationExists(String annotationExists) {
        this.annotationExists = annotationExists;
        return this;
    }

    public String getGene() {
        return gene;
    }

    public ZettaInterpretationAnalysisParams setGene(String gene) {
        this.gene = gene;
        return this;
    }

    public String getCt() {
        return ct;
    }

    public ZettaInterpretationAnalysisParams setCt(String ct) {
        this.ct = ct;
        return this;
    }

    public String getXref() {
        return xref;
    }

    public ZettaInterpretationAnalysisParams setXref(String xref) {
        this.xref = xref;
        return this;
    }

    public String getBiotype() {
        return biotype;
    }

    public ZettaInterpretationAnalysisParams setBiotype(String biotype) {
        this.biotype = biotype;
        return this;
    }

    public String getProteinSubstitution() {
        return proteinSubstitution;
    }

    public ZettaInterpretationAnalysisParams setProteinSubstitution(String proteinSubstitution) {
        this.proteinSubstitution = proteinSubstitution;
        return this;
    }

    public String getConservation() {
        return conservation;
    }

    public ZettaInterpretationAnalysisParams setConservation(String conservation) {
        this.conservation = conservation;
        return this;
    }

    public String getPopulationFrequencyAlt() {
        return populationFrequencyAlt;
    }

    public ZettaInterpretationAnalysisParams setPopulationFrequencyAlt(String populationFrequencyAlt) {
        this.populationFrequencyAlt = populationFrequencyAlt;
        return this;
    }

    public String getPopulationFrequencyRef() {
        return populationFrequencyRef;
    }

    public ZettaInterpretationAnalysisParams setPopulationFrequencyRef(String populationFrequencyRef) {
        this.populationFrequencyRef = populationFrequencyRef;
        return this;
    }

    public String getPopulationFrequencyMaf() {
        return populationFrequencyMaf;
    }

    public ZettaInterpretationAnalysisParams setPopulationFrequencyMaf(String populationFrequencyMaf) {
        this.populationFrequencyMaf = populationFrequencyMaf;
        return this;
    }

    public String getTranscriptFlag() {
        return transcriptFlag;
    }

    public ZettaInterpretationAnalysisParams setTranscriptFlag(String transcriptFlag) {
        this.transcriptFlag = transcriptFlag;
        return this;
    }

    public String getGeneTraitId() {
        return geneTraitId;
    }

    public ZettaInterpretationAnalysisParams setGeneTraitId(String geneTraitId) {
        this.geneTraitId = geneTraitId;
        return this;
    }

    public String getGo() {
        return go;
    }

    public ZettaInterpretationAnalysisParams setGo(String go) {
        this.go = go;
        return this;
    }

    public String getExpression() {
        return expression;
    }

    public ZettaInterpretationAnalysisParams setExpression(String expression) {
        this.expression = expression;
        return this;
    }

    public String getProteinKeyword() {
        return proteinKeyword;
    }

    public ZettaInterpretationAnalysisParams setProteinKeyword(String proteinKeyword) {
        this.proteinKeyword = proteinKeyword;
        return this;
    }

    public String getDrug() {
        return drug;
    }

    public ZettaInterpretationAnalysisParams setDrug(String drug) {
        this.drug = drug;
        return this;
    }

    public String getFunctionalScore() {
        return functionalScore;
    }

    public ZettaInterpretationAnalysisParams setFunctionalScore(String functionalScore) {
        this.functionalScore = functionalScore;
        return this;
    }

    public String getClinicalSignificance() {
        return clinicalSignificance;
    }

    public ZettaInterpretationAnalysisParams setClinicalSignificance(String clinicalSignificance) {
        this.clinicalSignificance = clinicalSignificance;
        return this;
    }

    public String getCustomAnnotation() {
        return customAnnotation;
    }

    public ZettaInterpretationAnalysisParams setCustomAnnotation(String customAnnotation) {
        this.customAnnotation = customAnnotation;
        return this;
    }

    public String getPanel() {
        return panel;
    }

    public ZettaInterpretationAnalysisParams setPanel(String panel) {
        this.panel = panel;
        return this;
    }

    public String getTrait() {
        return trait;
    }

    public ZettaInterpretationAnalysisParams setTrait(String trait) {
        this.trait = trait;
        return this;
    }

    public int getMaxLowCoverage() {
        return maxLowCoverage;
    }

    public ZettaInterpretationAnalysisParams setMaxLowCoverage(int maxLowCoverage) {
        this.maxLowCoverage = maxLowCoverage;
        return this;
    }

    public boolean isIncludeLowCoverage() {
        return includeLowCoverage;
    }

    public ZettaInterpretationAnalysisParams setIncludeLowCoverage(boolean includeLowCoverage) {
        this.includeLowCoverage = includeLowCoverage;
        return this;
    }
}
