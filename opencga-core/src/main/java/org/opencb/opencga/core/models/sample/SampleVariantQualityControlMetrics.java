package org.opencb.opencga.core.models.sample;

import org.opencb.biodata.models.clinical.qc.GenomePlot;
import org.opencb.biodata.models.clinical.qc.SampleQcVariantStats;
import org.opencb.biodata.models.clinical.qc.Signature;

import java.util.ArrayList;
import java.util.List;

public class SampleVariantQualityControlMetrics {

    private List<SampleQcVariantStats> variantStats;
    private List<Signature> signatures;
    private List<GenomePlot> genomePlots;
    private List<String> vcfFileIds;

    public SampleVariantQualityControlMetrics() {
        this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    public SampleVariantQualityControlMetrics(List<SampleQcVariantStats> variantStats, List<Signature> signatures,
                                              List<String> vcfFileIds) {
        this.variantStats = variantStats;
        this.signatures = signatures;
        this.genomePlots = new ArrayList<>();
        this.vcfFileIds = vcfFileIds;
    }

    public SampleVariantQualityControlMetrics(List<SampleQcVariantStats> variantStats, List<Signature> signatures,
                                              List<GenomePlot> genomePlots, List<String> vcfFileIds) {
        this.variantStats = variantStats;
        this.signatures = signatures;
        this.genomePlots = genomePlots;
        this.vcfFileIds = vcfFileIds;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SampleVariantQualityControlMetrics{");
        sb.append("variantStats=").append(variantStats);
        sb.append(", signatures=").append(signatures);
        sb.append(", genomePlots=").append(genomePlots);
        sb.append(", vcfFileIds=").append(vcfFileIds);
        sb.append('}');
        return sb.toString();
    }

    public List<SampleQcVariantStats> getVariantStats() {
        return variantStats;
    }

    public SampleVariantQualityControlMetrics setVariantStats(List<SampleQcVariantStats> variantStats) {
        this.variantStats = variantStats;
        return this;
    }

    public List<Signature> getSignatures() {
        return signatures;
    }

    public SampleVariantQualityControlMetrics setSignatures(List<Signature> signatures) {
        this.signatures = signatures;
        return this;
    }

    public List<GenomePlot> getGenomePlots() {
        return genomePlots;
    }

    public SampleVariantQualityControlMetrics setGenomePlots(List<GenomePlot> genomePlots) {
        this.genomePlots = genomePlots;
        return this;
    }

    public List<String> getVcfFileIds() {
        return vcfFileIds;
    }

    public SampleVariantQualityControlMetrics setVcfFileIds(List<String> vcfFileIds) {
        this.vcfFileIds = vcfFileIds;
        return this;
    }
}
