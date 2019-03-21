package org.opencb.opencga.storage.hadoop.variant.index.sample;

import org.junit.Test;
import org.opencb.biodata.models.variant.avro.VariantType;
import org.opencb.commons.datastore.core.Query;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.opencb.opencga.storage.core.variant.adaptors.VariantQueryParam.*;
import static org.opencb.opencga.storage.hadoop.variant.index.IndexUtils.EMPTY_MASK;
import static org.opencb.opencga.storage.hadoop.variant.index.annotation.AnnotationIndexConverter.*;
import static org.opencb.opencga.storage.hadoop.variant.index.sample.SampleIndexQueryParser.parseAnnotationMask;
import static org.opencb.opencga.storage.hadoop.variant.index.sample.SampleIndexQueryParser.parseFileMask;
import static org.opencb.opencga.storage.hadoop.variant.index.sample.SampleIndexToHBaseConverter.*;

/**
 * Created on 08/01/19.
 *
 * @author Jacobo Coll &lt;jacobo167@gmail.com&gt;
 */
public class SampleIndexQueryParserTest {

    @Test
    public void parseFileMaskTest() {
        assertArrayEquals(new byte[]{EMPTY_MASK, EMPTY_MASK}, parseFileMask(new Query(), "", null));
        assertArrayEquals(new byte[]{SNV_MASK, SNV_MASK}, parseFileMask(new Query(TYPE.key(), "SNV"), "", null));
        assertArrayEquals(new byte[]{SNV_MASK, SNV_MASK}, parseFileMask(new Query(TYPE.key(), "SNP"), "", null));
        assertArrayEquals(new byte[]{SNV_MASK, EMPTY_MASK}, parseFileMask(new Query(TYPE.key(), "INDEL"), "", null));
        assertArrayEquals(new byte[]{EMPTY_MASK, EMPTY_MASK}, parseFileMask(new Query(TYPE.key(), "SNV,INDEL"), "", null));

        assertArrayEquals(new byte[]{FILTER_PASS_MASK, FILTER_PASS_MASK}, parseFileMask(new Query(FILTER.key(), "PASS"), "", null));
        assertArrayEquals(new byte[]{FILTER_PASS_MASK, EMPTY_MASK}, parseFileMask(new Query(FILTER.key(), "!PASS"), "", null));
        assertArrayEquals(new byte[]{FILTER_PASS_MASK, EMPTY_MASK}, parseFileMask(new Query(FILTER.key(), "LowQual"), "", null));
        assertArrayEquals(new byte[]{EMPTY_MASK, EMPTY_MASK}, parseFileMask(new Query(FILTER.key(), "!LowQual"), "", null));

        assertArrayEquals(new byte[]{FILTER_PASS_MASK, EMPTY_MASK}, parseFileMask(new Query(FILTER.key(), "LowGQX,LowQual"), "", null));
        assertArrayEquals(new byte[]{FILTER_PASS_MASK, EMPTY_MASK}, parseFileMask(new Query(FILTER.key(), "LowGQX;LowQual"), "", null));
        assertArrayEquals(new byte[]{EMPTY_MASK, EMPTY_MASK}, parseFileMask(new Query(FILTER.key(), "PASS,LowQual"), "", null));
        assertArrayEquals(new byte[]{EMPTY_MASK, EMPTY_MASK}, parseFileMask(new Query(FILTER.key(), "PASS;LowQual"), "", null));
        assertArrayEquals(new byte[]{EMPTY_MASK, EMPTY_MASK}, parseFileMask(new Query(FILTER.key(), "!LowGQX;!LowQual"), "", null));


        byte qual20_40 = QUAL_GT_20_MASK | QUAL_GT_40_MASK;
        assertArrayEquals("=10", new byte[]{qual20_40, EMPTY_MASK}, parseFileMask(new Query(QUAL.key(), "=10"), "", null));
        assertArrayEquals("=20", new byte[]{qual20_40, EMPTY_MASK}, parseFileMask(new Query(QUAL.key(), "=20"), "", null));
        assertArrayEquals("=30", new byte[]{qual20_40, QUAL_GT_20_MASK}, parseFileMask(new Query(QUAL.key(), "=30"), "", null));
        assertArrayEquals("=40", new byte[]{qual20_40, QUAL_GT_20_MASK}, parseFileMask(new Query(QUAL.key(), "=40"), "", null));
        assertArrayEquals("=50", new byte[]{qual20_40, qual20_40}, parseFileMask(new Query(QUAL.key(), "=50"), "", null));

        assertArrayEquals("<=10", new byte[]{qual20_40, EMPTY_MASK}, parseFileMask(new Query(QUAL.key(), "<=10"), "", null));
        assertArrayEquals("<=20", new byte[]{qual20_40, EMPTY_MASK}, parseFileMask(new Query(QUAL.key(), "<=20"), "", null));
        assertArrayEquals("<=30", new byte[]{QUAL_GT_40_MASK, EMPTY_MASK}, parseFileMask(new Query(QUAL.key(), "<=30"), "", null));
        assertArrayEquals("<=40", new byte[]{QUAL_GT_40_MASK, EMPTY_MASK}, parseFileMask(new Query(QUAL.key(), "<=40"), "", null));

        assertArrayEquals(">=10", new byte[]{EMPTY_MASK, EMPTY_MASK}, parseFileMask(new Query(QUAL.key(), ">=10"), "", null));
        assertArrayEquals(">=20", new byte[]{EMPTY_MASK, EMPTY_MASK}, parseFileMask(new Query(QUAL.key(), ">=20"), "", null));
        assertArrayEquals(">=30", new byte[]{QUAL_GT_20_MASK, QUAL_GT_20_MASK}, parseFileMask(new Query(QUAL.key(), ">=30"), "", null));
        assertArrayEquals(">=40", new byte[]{QUAL_GT_20_MASK, QUAL_GT_20_MASK}, parseFileMask(new Query(QUAL.key(), ">=40"), "", null));
        assertArrayEquals(">=50", new byte[]{qual20_40, qual20_40}, parseFileMask(new Query(QUAL.key(), ">=50"), "", null));



        assertArrayEquals("S1:DP>10", new byte[]{EMPTY_MASK, EMPTY_MASK}, parseFileMask(new Query(FORMAT.key(), "S1:DP>10"), "S1", null));
        assertArrayEquals("S1:DP>=20", new byte[]{EMPTY_MASK, EMPTY_MASK}, parseFileMask(new Query(FORMAT.key(), "S1:DP>=20"), "S1", null));
        assertArrayEquals("S1:DP>=20", new byte[]{EMPTY_MASK, EMPTY_MASK}, parseFileMask(new Query(FORMAT.key(), "S1:DP>=20"), "S1", null));
        assertArrayEquals("S1:DP>20", new byte[]{DP_GT_20_MASK, DP_GT_20_MASK}, parseFileMask(new Query(FORMAT.key(), "S1:DP>20"), "S1", null));
        assertArrayEquals("S1:DP>20", new byte[]{DP_GT_20_MASK, DP_GT_20_MASK}, parseFileMask(new Query(FORMAT.key(), "S1:GG>4,DP>20"), "S1", null));
        assertArrayEquals("S1:DP>20", new byte[]{DP_GT_20_MASK, DP_GT_20_MASK}, parseFileMask(new Query(FORMAT.key(), "S1:GG>4;DP>20"), "S1", null));

        assertArrayEquals("S1:DP<10", new byte[]{DP_GT_20_MASK, EMPTY_MASK}, parseFileMask(new Query(FORMAT.key(), "S1:DP<10"), "S1", null));

        assertArrayEquals("S1:DP>20", new byte[]{EMPTY_MASK, EMPTY_MASK}, parseFileMask(new Query(FORMAT.key(), "S1:DP>20"), "S2", null));
        assertArrayEquals("S1:DP<10", new byte[]{EMPTY_MASK, EMPTY_MASK}, parseFileMask(new Query(FORMAT.key(), "S1:DP<10"), "S2", null));

    }

    @Test
    public void parseAnnotationMaskTest() {
        assertEquals(EMPTY_MASK, parseAnnotationMask(new Query()));
        assertEquals(EMPTY_MASK, parseAnnotationMask(new Query(TYPE.key(), VariantType.SNV)));
        assertEquals(EMPTY_MASK, parseAnnotationMask(new Query(TYPE.key(), VariantType.SNP)));
        assertEquals(EMPTY_MASK /*UNUSED_6_MASK*/, parseAnnotationMask(new Query(TYPE.key(), VariantType.INDEL)));
        assertEquals(EMPTY_MASK /*UNUSED_6_MASK*/, parseAnnotationMask(new Query(TYPE.key(), VariantType.INSERTION)));
        assertEquals(EMPTY_MASK /*UNUSED_6_MASK*/, parseAnnotationMask(new Query(TYPE.key(), VariantType.DELETION)));

        assertEquals(PROTEIN_CODING_MASK, parseAnnotationMask(new Query(ANNOT_BIOTYPE.key(), "protein_coding")));
        assertEquals(EMPTY_MASK, parseAnnotationMask(new Query(ANNOT_BIOTYPE.key(), "other_than_protein_coding")));
        assertEquals(LOF_MASK, parseAnnotationMask(new Query(ANNOT_PROTEIN_SUBSTITUTION.key(), "sift<0.1")));
        assertEquals(EMPTY_MASK, parseAnnotationMask(new Query(ANNOT_PROTEIN_SUBSTITUTION.key(), "sift<<0.1")));

        assertEquals(LOF_MISSENSE_MASK, parseAnnotationMask(new Query(ANNOT_CONSEQUENCE_TYPE.key(), "missense_variant")));
        assertEquals(LOF_MISSENSE_MASK, parseAnnotationMask(new Query(ANNOT_CONSEQUENCE_TYPE.key(), "stop_lost,missense_variant")));
        assertEquals(LOF_MASK | LOF_MISSENSE_MASK, parseAnnotationMask(new Query(ANNOT_CONSEQUENCE_TYPE.key(), "stop_lost")));
        assertEquals(LOF_MASK | LOF_MISSENSE_MASK, parseAnnotationMask(new Query(ANNOT_CONSEQUENCE_TYPE.key(), "stop_lost,stop_gained")));

        assertEquals(LOF_MISSENSE_MASK | LOF_MISSENSE_BASIC_MASK, parseAnnotationMask(new Query(ANNOT_CONSEQUENCE_TYPE.key(), "missense_variant,stop_lost,stop_gained")
                .append(ANNOT_TRANSCRIPT_FLAG.key(), "basic")));
        assertEquals(LOF_MASK | LOF_MISSENSE_MASK | LOF_MISSENSE_BASIC_MASK, parseAnnotationMask(new Query(ANNOT_CONSEQUENCE_TYPE.key(), "stop_lost,stop_gained")
                .append(ANNOT_TRANSCRIPT_FLAG.key(), "basic")));

        assertEquals(EMPTY_MASK, parseAnnotationMask(new Query(ANNOT_POPULATION_ALTERNATE_FREQUENCY.key(), "1kG_phase3:ALL<0.01")));
        assertEquals(POP_FREQ_ANY_001_MASK, parseAnnotationMask(new Query(ANNOT_POPULATION_ALTERNATE_FREQUENCY.key(), "1kG_phase3:ALL<0.001")));
        assertEquals(POP_FREQ_ANY_001_MASK, parseAnnotationMask(new Query(ANNOT_POPULATION_ALTERNATE_FREQUENCY.key(), "GNOMAD_GENOMES:ALL<0.001")));
        assertEquals(POP_FREQ_ANY_001_MASK, parseAnnotationMask(new Query(ANNOT_POPULATION_ALTERNATE_FREQUENCY.key(), "GNOMAD_GENOMES:ALL<0.0005")));
        assertEquals(POP_FREQ_ANY_001_MASK, parseAnnotationMask(new Query(ANNOT_POPULATION_ALTERNATE_FREQUENCY.key(), "1kG_phase3:ALL<0.001,GNOMAD_GENOMES:ALL<0.001")));
        assertEquals(POP_FREQ_ANY_001_MASK, parseAnnotationMask(new Query(ANNOT_POPULATION_ALTERNATE_FREQUENCY.key(), "1kG_phase3:ALL<0.001;GNOMAD_GENOMES:ALL<0.001")));

        // Mix
        assertEquals(POP_FREQ_ANY_001_MASK, parseAnnotationMask(new Query(ANNOT_POPULATION_ALTERNATE_FREQUENCY.key(), "1kG_phase3:ALL<0.1;GNOMAD_GENOMES:ALL<0.001")));
        assertEquals(EMPTY_MASK, parseAnnotationMask(new Query(ANNOT_POPULATION_ALTERNATE_FREQUENCY.key(), "1kG_phase3:ALL<0.1,GNOMAD_GENOMES:ALL<0.001")));
        assertEquals(EMPTY_MASK, parseAnnotationMask(new Query(ANNOT_POPULATION_ALTERNATE_FREQUENCY.key(), "1kG_phase3:ALL<0.1;GNOMAD_GENOMES:ALL<0.1")));


        assertEquals(POP_FREQ_ALL_01_MASK, parseAnnotationMask(new Query(ANNOT_POPULATION_ALTERNATE_FREQUENCY.key(),
                  "GNOMAD_EXOMES:AFR<0.01;"
                + "GNOMAD_EXOMES:AMR<0.01;"
                + "GNOMAD_EXOMES:EAS<0.01;"
                + "GNOMAD_EXOMES:FIN<0.01;"
                + "GNOMAD_EXOMES:NFE<0.01;"
                + "GNOMAD_EXOMES:ASJ<0.01;"
                + "GNOMAD_EXOMES:OTH<0.01;"
                + "1kG_phase3:AFR<0.01;"
                + "1kG_phase3:AMR<0.01;"
                + "1kG_phase3:EAS<0.01;"
                + "1kG_phase3:EUR<0.01;"
                + "1kG_phase3:SAS<0.01")));

        // Adding more populations should use the filter
        assertEquals(POP_FREQ_ALL_01_MASK, parseAnnotationMask(new Query(ANNOT_POPULATION_ALTERNATE_FREQUENCY.key(),
                  "GNOMAD_EXOMES:ALL<0.01;" // added
                + "GNOMAD_EXOMES:AFR<0.01;"
                + "GNOMAD_EXOMES:AMR<0.01;"
                + "GNOMAD_EXOMES:EAS<0.01;"
                + "GNOMAD_EXOMES:FIN<0.01;"
                + "GNOMAD_EXOMES:NFE<0.01;"
                + "GNOMAD_EXOMES:ASJ<0.01;"
                + "GNOMAD_EXOMES:OTH<0.01;"
                + "1kG_phase3:AFR<0.01;"
                + "1kG_phase3:AMR<0.01;"
                + "1kG_phase3:EAS<0.01;"
                + "1kG_phase3:EUR<0.01;"
                + "1kG_phase3:SAS<0.01")));

        // Removing any populations should not use the filter
        assertEquals(EMPTY_MASK, parseAnnotationMask(new Query(ANNOT_POPULATION_ALTERNATE_FREQUENCY.key(),
                  "GNOMAD_EXOMES:AFR<0.01;"
                + "GNOMAD_EXOMES:AMR<0.01;"
//                + "GNOMAD_EXOMES:EAS<0.01;"
                + "GNOMAD_EXOMES:FIN<0.01;"
                + "GNOMAD_EXOMES:NFE<0.01;"
                + "GNOMAD_EXOMES:ASJ<0.01;"
                + "GNOMAD_EXOMES:OTH<0.01;"
                + "1kG_phase3:AFR<0.01;"
                + "1kG_phase3:AMR<0.01;"
                + "1kG_phase3:EAS<0.01;"
                + "1kG_phase3:EUR<0.01;"
                + "1kG_phase3:SAS<0.01")));

        // With OR instead of AND should not use the filter
        assertEquals(EMPTY_MASK, parseAnnotationMask(new Query(ANNOT_POPULATION_ALTERNATE_FREQUENCY.key(),
                  "GNOMAD_EXOMES:AFR<0.01,"
                + "GNOMAD_EXOMES:AMR<0.01,"
                + "GNOMAD_EXOMES:EAS<0.01,"
                + "GNOMAD_EXOMES:FIN<0.01,"
                + "GNOMAD_EXOMES:NFE<0.01,"
                + "GNOMAD_EXOMES:ASJ<0.01,"
                + "GNOMAD_EXOMES:OTH<0.01,"
                + "1kG_phase3:AFR<0.01,"
                + "1kG_phase3:AMR<0.01,"
                + "1kG_phase3:EAS<0.01,"
                + "1kG_phase3:EUR<0.01,"
                + "1kG_phase3:SAS<0.01")));

        // With any filter greater than 0.1, should not use the filter
        assertEquals(EMPTY_MASK, parseAnnotationMask(new Query(ANNOT_POPULATION_ALTERNATE_FREQUENCY.key(),
                  "GNOMAD_EXOMES:AFR<0.01;"
                + "GNOMAD_EXOMES:AMR<0.02;" // Increased from 0.01 to 0.02
                + "GNOMAD_EXOMES:EAS<0.01;"
                + "GNOMAD_EXOMES:FIN<0.01;"
                + "GNOMAD_EXOMES:NFE<0.01;"
                + "GNOMAD_EXOMES:ASJ<0.01;"
                + "GNOMAD_EXOMES:OTH<0.01;"
                + "1kG_phase3:AFR<0.01;"
                + "1kG_phase3:AMR<0.01;"
                + "1kG_phase3:EAS<0.01;"
                + "1kG_phase3:EUR<0.01;"
                + "1kG_phase3:SAS<0.01")));

    }
}