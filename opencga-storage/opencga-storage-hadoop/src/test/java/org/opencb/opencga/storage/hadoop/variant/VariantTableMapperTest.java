package org.opencb.opencga.storage.hadoop.variant;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opencb.biodata.models.variant.StudyEntry;
import org.opencb.biodata.models.variant.Variant;
import org.opencb.biodata.models.variant.VariantSource;
import org.opencb.biodata.models.variant.avro.FileEntry;
import org.opencb.datastore.core.ObjectMap;
import org.opencb.opencga.storage.core.StudyConfiguration;
import org.opencb.opencga.storage.core.config.StorageEngineConfiguration;
import org.opencb.opencga.storage.core.variant.VariantStorageManagerTestUtils;
import org.opencb.opencga.storage.hadoop.utils.HBaseManager;
import org.opencb.opencga.storage.hadoop.variant.index.HBaseToVariantConverter;

public class VariantTableMapperTest extends HadoopVariantStorageManagerTestUtils {

    private VariantHadoopDBAdaptor dbAdaptor;

    private VariantSource loadFile(String resource, StudyConfiguration studyConfiguration, Map<? extends String, ?> map) throws Exception {
        return VariantHbaseTestUtils.loadFile(getVariantStorageManager(), DB_NAME, outputUri, resource, studyConfiguration, map);
    }

    @Before
    public void setUp() throws Exception {
        clearDB(DB_NAME);
        //Force HBaseConverter to fail if something goes wrong
        HBaseToVariantConverter.setFailOnWrongVariants(true);
        HadoopVariantStorageManager variantStorageManager = getVariantStorageManager();
        dbAdaptor = variantStorageManager.getDBAdaptor(DB_NAME);
        
    }

    @After
    public void tearDown() throws Exception {
    }

    private HBaseStudyConfigurationManager buildStudyManager() throws IOException{
        StorageEngineConfiguration se = variantStorageManager.getConfiguration().getStorageEngine(variantStorageManager.getStorageEngineId());
        ObjectMap opts = se.getVariant().getOptions();
        return new HBaseStudyConfigurationManager(DB_NAME, configuration.get(), opts);
    }
    
    @Test
    public void testMap() throws Exception {
        StudyConfiguration studyConfiguration = VariantStorageManagerTestUtils.newStudyConfiguration();
        VariantSource source1 = loadFile("s1.genome.vcf", studyConfiguration, Collections.emptyMap());
        System.out.println("Query from HBase : " + DB_NAME);
        Configuration conf = configuration.get();
        HBaseToVariantConverter conv = new HBaseToVariantConverter(dbAdaptor.getGenomeHelper(), buildStudyManager());
        HBaseManager hm = new HBaseManager(conf);
        GenomeHelper genomeHelper = dbAdaptor.getGenomeHelper();
        for(Variant variant : dbAdaptor){
            List<StudyEntry> studies = variant.getStudies();
            StudyEntry se = studies.get(0);
            FileEntry fe = se.getFiles().get(0);
            String passString = fe.getAttributes().get("PASS");
            Integer passCnt = Integer.parseInt(passString);
            System.out.println(String.format("Variant = %s; Studies=%s; Pass=%s;",variant,studies.size(),passCnt));
            assertEquals("Position issue with PASS", variant.getStart().equals(10032), passCnt.equals(1));
        }
        System.out.println("End query from HBase : " + DB_NAME);
//        assertEquals(source.getStats().getVariantTypeCount(VariantType.SNP) + source.getStats().getVariantTypeCount(VariantType.SNV), numVariants);
        
    }

    @Test
    public void testFilterMap() throws Exception {
        StudyConfiguration studyConfiguration = VariantStorageManagerTestUtils.newStudyConfiguration();
        VariantSource source1 = loadFile("s1.genome.vcf", studyConfiguration, Collections.emptyMap());
        VariantSource source2 = loadFile("s2.genome.vcf", studyConfiguration, Collections.emptyMap());
        System.out.println("Query from HBase : " + DB_NAME);
        Configuration conf = configuration.get();
        HBaseToVariantConverter conv = new HBaseToVariantConverter(dbAdaptor.getGenomeHelper(), buildStudyManager());
        HBaseManager hm = new HBaseManager(conf);
        GenomeHelper genomeHelper = dbAdaptor.getGenomeHelper();
        for(Variant variant : dbAdaptor){
            List<StudyEntry> studies = variant.getStudies();
            StudyEntry se = studies.get(0);
            FileEntry fe = se.getFiles().get(0);
            System.out.println(se.getSamplesData());
            String passString = fe.getAttributes().get("PASS");
            Integer passCnt = Integer.parseInt(passString);
            System.out.println(String.format("Variant = %s; Studies=%s; Pass=%s;",variant,studies.size(),passCnt));
            assertEquals("Position issue with PASS", variant.getStart().equals(10032), passCnt.equals(1));
        }
        System.out.println("End query from HBase : " + DB_NAME);
//        assertEquals(source.getStats().getVariantTypeCount(VariantType.SNP) + source.getStats().getVariantTypeCount(VariantType.SNV), numVariants);
        
    }

}
