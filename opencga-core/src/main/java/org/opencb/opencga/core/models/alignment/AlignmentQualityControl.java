package org.opencb.opencga.core.models.alignment;

import org.opencb.biodata.formats.alignment.picard.HsMetrics;
import org.opencb.biodata.formats.alignment.samtools.SamtoolsFlagstats;
import org.opencb.biodata.formats.alignment.samtools.SamtoolsStats;
import org.opencb.biodata.formats.sequence.fastqc.FastQcMetrics;

import java.io.Serializable;

public class AlignmentQualityControl implements Serializable {

    private FastQcMetrics fastQcMetrics;
    private SamtoolsStats samtoolsStats;
    private SamtoolsFlagstats samtoolsFlagStats;
    private HsMetrics hsMetrics;

    public AlignmentQualityControl() {
        this(null, null, null, null);
    }

    public AlignmentQualityControl(FastQcMetrics fastQcMetrics, SamtoolsStats samtoolsStats, SamtoolsFlagstats samtoolsFlagStats, HsMetrics hsMetrics) {
        this.fastQcMetrics = fastQcMetrics;
        this.samtoolsStats = samtoolsStats;
        this.samtoolsFlagStats = samtoolsFlagStats;
        this.hsMetrics = hsMetrics;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Alignment{");
        sb.append("fastQcMetrics=").append(fastQcMetrics);
        sb.append(", samtoolsStats=").append(samtoolsStats);
        sb.append(", samtoolsFlagStats=").append(samtoolsFlagStats);
        sb.append(", hsMetrics=").append(hsMetrics);
        sb.append('}');
        return sb.toString();
    }

    public FastQcMetrics getFastQcMetrics() {
        return fastQcMetrics;
    }

    public AlignmentQualityControl setFastQcMetrics(FastQcMetrics fastQcMetrics) {
        this.fastQcMetrics = fastQcMetrics;
        return this;
    }

    public SamtoolsStats getSamtoolsStats() {
        return samtoolsStats;
    }

    public AlignmentQualityControl setSamtoolsStats(SamtoolsStats samtoolsStats) {
        this.samtoolsStats = samtoolsStats;
        return this;
    }

    public SamtoolsFlagstats getSamtoolsFlagStats() {
        return samtoolsFlagStats;
    }

    public AlignmentQualityControl setSamtoolsFlagStats(SamtoolsFlagstats samtoolsFlagStats) {
        this.samtoolsFlagStats = samtoolsFlagStats;
        return this;
    }

    public HsMetrics getHsMetrics() {
        return hsMetrics;
    }

    public AlignmentQualityControl setHsMetrics(HsMetrics hsMetrics) {
        this.hsMetrics = hsMetrics;
        return this;
    }
}
