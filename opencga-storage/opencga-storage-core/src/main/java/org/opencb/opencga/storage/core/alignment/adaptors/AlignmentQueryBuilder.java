package org.opencb.opencga.storage.core.alignment.adaptors;

import org.opencb.biodata.models.feature.Region;
import org.opencb.datastore.core.QueryOptions;
import org.opencb.datastore.core.QueryResult;

/**
 *
 * @author Cristina Yenyxe Gonzalez Garcia <cgonzalez@cipf.es>
 */
public interface AlignmentQueryBuilder {
    //Query Options
    public static final String QO_BAM_PATH = "bam_path";
    public static final String QO_BAI_PATH = "bai_path";
    public static final String QO_VIEW_AS_PAIRS = "view_as_pairs";
    public static final String QO_PROCESS_DIFFERENCES = "process_differences";
    public static final String QO_FILE_ID = "file_id";
    public static final String QO_HISTOGRAM = "histogram";
    public static final String QO_INCLUDE_COVERAGE = "include_coverage";
    //public static final String QO_AVERAGE = "average";
    public static final String QO_BATCH_SIZE = "batch_size";

    QueryResult getAllAlignmentsByRegion(Region region, QueryOptions options);

//    List<QueryResult> getAllAlignmentsByRegionList(List<Region> region, QueryOptions options);
    
    
//    QueryResult getAllAlignmentBlocksByRegion(Region region, QueryOptions options);
//    
//    List<QueryResult> getAllAlignmentBlocksByRegionList(List<Region> region, QueryOptions options);
    
    QueryResult getAllAlignmentsByGene(String gene, QueryOptions options);
    
    QueryResult getCoverageByRegion(Region region, QueryOptions options);
    
    QueryResult getAlignmentsHistogramByRegion(Region region, boolean histogramLogarithm, int histogramMax);
    
    QueryResult getAlignmentRegionInfo(Region region, QueryOptions options);
    
}
