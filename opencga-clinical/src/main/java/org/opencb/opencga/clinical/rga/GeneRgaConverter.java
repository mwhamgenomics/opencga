package org.opencb.opencga.clinical.rga;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.StringUtils;
import org.opencb.commons.datastore.core.ComplexTypeConverter;
import org.opencb.opencga.core.common.JacksonUtils;
import org.opencb.opencga.core.models.analysis.knockout.KnockoutByGene;
import org.opencb.opencga.core.models.analysis.knockout.KnockoutTranscript;
import org.opencb.opencga.core.models.analysis.knockout.KnockoutVariant;
import org.opencb.opencga.core.models.analysis.knockout.RgaKnockoutByGene;
import org.slf4j.LoggerFactory;

import java.util.*;

public class GeneRgaConverter extends AbstractRgaConverter implements ComplexTypeConverter<List<RgaKnockoutByGene>, List<RgaDataModel>> {

    // This object contains the list of solr fields that are required in order to fully build each of the RgaKnockoutByGene fields
    private static final Map<String, List<String>> CONVERTER_MAP;

    static {
        CONVERTER_MAP = new HashMap<>();
        // We always include individual id in the response because we always want to return the numIndividuals populated
        CONVERTER_MAP.put("id", Arrays.asList(RgaDataModel.GENE_ID, RgaDataModel.INDIVIDUAL_ID));
        CONVERTER_MAP.put("name", Arrays.asList(RgaDataModel.GENE_ID, RgaDataModel.GENE_NAME, RgaDataModel.INDIVIDUAL_ID));
        CONVERTER_MAP.put("chromosome", Arrays.asList(RgaDataModel.GENE_ID, RgaDataModel.CHROMOSOME, RgaDataModel.INDIVIDUAL_ID));
        CONVERTER_MAP.put("start", Arrays.asList(RgaDataModel.GENE_ID, RgaDataModel.START, RgaDataModel.INDIVIDUAL_ID));
        CONVERTER_MAP.put("end", Arrays.asList(RgaDataModel.GENE_ID, RgaDataModel.END, RgaDataModel.INDIVIDUAL_ID));
        CONVERTER_MAP.put("strand", Arrays.asList(RgaDataModel.GENE_ID, RgaDataModel.STRAND, RgaDataModel.INDIVIDUAL_ID));
        CONVERTER_MAP.put("biotype", Arrays.asList(RgaDataModel.GENE_ID, RgaDataModel.GENE_BIOTYPE, RgaDataModel.INDIVIDUAL_ID));
        CONVERTER_MAP.put("annotation", Collections.emptyList());
        CONVERTER_MAP.put("individuals.id", Arrays.asList(RgaDataModel.GENE_ID, RgaDataModel.INDIVIDUAL_ID));
        CONVERTER_MAP.put("individuals.sampleId", Arrays.asList(RgaDataModel.GENE_ID, RgaDataModel.INDIVIDUAL_ID, RgaDataModel.SAMPLE_ID));
        CONVERTER_MAP.put("individuals.numParents", Arrays.asList(RgaDataModel.GENE_ID, RgaDataModel.INDIVIDUAL_ID,
                RgaDataModel.NUM_PARENTS));
        CONVERTER_MAP.put("individuals.motherId", Arrays.asList(RgaDataModel.GENE_ID, RgaDataModel.INDIVIDUAL_ID, RgaDataModel.MOTHER_ID));
        CONVERTER_MAP.put("individuals.motherSampleId", Arrays.asList(RgaDataModel.GENE_ID, RgaDataModel.INDIVIDUAL_ID,
                RgaDataModel.MOTHER_SAMPLE_ID));
        CONVERTER_MAP.put("individuals.fatherId", Arrays.asList(RgaDataModel.GENE_ID, RgaDataModel.INDIVIDUAL_ID, RgaDataModel.FATHER_ID));
        CONVERTER_MAP.put("individuals.fatherSampleId", Arrays.asList(RgaDataModel.GENE_ID, RgaDataModel.INDIVIDUAL_ID,
                RgaDataModel.FATHER_SAMPLE_ID));
        CONVERTER_MAP.put("individuals.transcripts.id", Arrays.asList(RgaDataModel.GENE_ID, RgaDataModel.INDIVIDUAL_ID,
                RgaDataModel.TRANSCRIPT_ID));
        CONVERTER_MAP.put("individuals.transcripts.chromosome", Arrays.asList(RgaDataModel.GENE_ID, RgaDataModel.INDIVIDUAL_ID,
                RgaDataModel.TRANSCRIPT_ID, RgaDataModel.CHROMOSOME));
        CONVERTER_MAP.put("individuals.transcripts.start", Arrays.asList(RgaDataModel.GENE_ID, RgaDataModel.INDIVIDUAL_ID,
                RgaDataModel.TRANSCRIPT_ID, RgaDataModel.START));
        CONVERTER_MAP.put("individuals.transcripts.end", Arrays.asList(RgaDataModel.GENE_ID, RgaDataModel.INDIVIDUAL_ID,
                RgaDataModel.TRANSCRIPT_ID, RgaDataModel.END));
        CONVERTER_MAP.put("individuals.transcripts.biotype", Arrays.asList(RgaDataModel.GENE_ID, RgaDataModel.INDIVIDUAL_ID,
                RgaDataModel.TRANSCRIPT_ID, RgaDataModel.TRANSCRIPT_BIOTYPE));
        CONVERTER_MAP.put("individuals.transcripts.strand", Arrays.asList(RgaDataModel.GENE_ID, RgaDataModel.INDIVIDUAL_ID,
                RgaDataModel.TRANSCRIPT_ID, RgaDataModel.STRAND));
        CONVERTER_MAP.put("individuals.transcripts.variants.id", Arrays.asList(RgaDataModel.GENE_ID, RgaDataModel.INDIVIDUAL_ID,
                RgaDataModel.TRANSCRIPT_ID, RgaDataModel.VARIANTS, RgaDataModel.VARIANT_JSON));
        CONVERTER_MAP.put("individuals.transcripts.variants.genotype", Arrays.asList(RgaDataModel.GENE_ID, RgaDataModel.INDIVIDUAL_ID,
                RgaDataModel.TRANSCRIPT_ID, RgaDataModel.VARIANT_JSON));
        CONVERTER_MAP.put("individuals.transcripts.variants.filter", Arrays.asList(RgaDataModel.GENE_ID, RgaDataModel.INDIVIDUAL_ID,
                RgaDataModel.TRANSCRIPT_ID, RgaDataModel.VARIANT_JSON, RgaDataModel.FILTERS));
        CONVERTER_MAP.put("individuals.transcripts.variants.qual", Arrays.asList(RgaDataModel.GENE_ID, RgaDataModel.INDIVIDUAL_ID,
                RgaDataModel.TRANSCRIPT_ID, RgaDataModel.VARIANT_JSON));
        CONVERTER_MAP.put("individuals.transcripts.variants.type", Arrays.asList(RgaDataModel.GENE_ID, RgaDataModel.INDIVIDUAL_ID,
                RgaDataModel.TRANSCRIPT_ID, RgaDataModel.VARIANT_JSON, RgaDataModel.TYPES));
        CONVERTER_MAP.put("individuals.transcripts.variants.knockoutType", Arrays.asList(RgaDataModel.GENE_ID,
                RgaDataModel.INDIVIDUAL_ID, RgaDataModel.TRANSCRIPT_ID, RgaDataModel.VARIANT_JSON, RgaDataModel.KNOCKOUT_TYPES));
        CONVERTER_MAP.put("individuals.transcripts.variants.populationFrequencies", Arrays.asList(RgaDataModel.GENE_ID,
                RgaDataModel.INDIVIDUAL_ID, RgaDataModel.TRANSCRIPT_ID, RgaDataModel.VARIANT_JSON, RgaDataModel.POPULATION_FREQUENCIES));
        CONVERTER_MAP.put("individuals.transcripts.variants.clinicalSignificance", Arrays.asList(RgaDataModel.GENE_ID,
                RgaDataModel.INDIVIDUAL_ID, RgaDataModel.TRANSCRIPT_ID, RgaDataModel.VARIANT_JSON, RgaDataModel.CLINICAL_SIGNIFICANCES));
        CONVERTER_MAP.put("individuals.transcripts.variants.sequenceOntologyTerms", Arrays.asList(RgaDataModel.GENE_ID,
                RgaDataModel.INDIVIDUAL_ID, RgaDataModel.TRANSCRIPT_ID, RgaDataModel.VARIANT_JSON, RgaDataModel.CONSEQUENCE_TYPES));

        logger = LoggerFactory.getLogger(GeneRgaConverter.class);
    }

    public GeneRgaConverter() {
    }

    @Override
    public List<RgaKnockoutByGene> convertToDataModelType(List<RgaDataModel> rgaDataModelList) {
        Map<String, RgaKnockoutByGene> result = new HashMap<>();
        for (RgaDataModel rgaDataModel : rgaDataModelList) {
            if (!result.containsKey(rgaDataModel.getGeneId())) {
                RgaKnockoutByGene knockoutByGene = new RgaKnockoutByGene();
                knockoutByGene.setId(rgaDataModel.getGeneId());
                knockoutByGene.setName(rgaDataModel.getGeneName());
//                knockoutByGene.setChromosome(xxxxx);
//                knockoutByGene.setStart(xxxx);
//                knockoutByGene.setEnd(xxxx);
//                knockoutByGene.setStrand(xxxx);
//                knockoutByGene.setBiotype(xxxx);
//                knockoutByGene.setAnnotation(xxxx);

                knockoutByGene.setIndividuals(new LinkedList<>());
                knockoutByGene.setNumIndividuals(0);
                result.put(rgaDataModel.getGeneId(), knockoutByGene);
            }

            RgaKnockoutByGene knockoutByGene = result.get(rgaDataModel.getGeneId());
            RgaKnockoutByGene.KnockoutIndividual knockoutIndividual = null;
            if (StringUtils.isNotEmpty(rgaDataModel.getIndividualId())) {
                for (RgaKnockoutByGene.KnockoutIndividual individual : knockoutByGene.getIndividuals()) {
                    if (rgaDataModel.getIndividualId().equals(individual.getId())) {
                        knockoutIndividual = individual;
                    }
                }
            }
            if (knockoutIndividual == null) {
                knockoutIndividual = new KnockoutByGene.KnockoutIndividual(fillIndividualInfo(rgaDataModel));
                knockoutIndividual.setTranscripts(new LinkedList<>());

                knockoutByGene.addIndividual(knockoutIndividual);
                knockoutByGene.setNumIndividuals(knockoutByGene.getNumIndividuals() + 1);
            }

            if (StringUtils.isNotEmpty(rgaDataModel.getTranscriptId())) {
                // Add new transcript
                KnockoutTranscript knockoutTranscript = new KnockoutTranscript(rgaDataModel.getTranscriptId());
                knockoutTranscript.setBiotype(rgaDataModel.getTranscriptBiotype());

                knockoutIndividual.addTranscripts(Collections.singletonList(knockoutTranscript));

                if (rgaDataModel.getVariantJson() != null) {
                    List<KnockoutVariant> knockoutVariantList = new LinkedList<>();
                    for (String variantJson : rgaDataModel.getVariantJson()) {
                        try {
                            KnockoutVariant knockoutVariant = JacksonUtils.getDefaultObjectMapper().readValue(variantJson,
                                    KnockoutVariant.class);
                            knockoutVariantList.add(knockoutVariant);
                        } catch (JsonProcessingException e) {
                            logger.warn("Could not parse KnockoutVariants: {}", e.getMessage(), e);
                        }
                    }
                    knockoutTranscript.setVariants(knockoutVariantList);
                }
            }
        }

        return new ArrayList<>(result.values());
    }

    @Override
    public List<RgaDataModel> convertToStorageType(List<RgaKnockoutByGene> object) {
        return null;
    }


    public List<String> getIncludeFields(List<String> includeFields) {
        Set<String> toInclude = new HashSet<>();
        for (String includeField : includeFields) {
            for (String fieldKey : CONVERTER_MAP.keySet()) {
                if (fieldKey.startsWith(includeField)) {
                    toInclude.addAll(CONVERTER_MAP.get(fieldKey));
                }
            }
        }
        return new ArrayList<>(toInclude);
    }

    public List<String> getIncludeFromExcludeFields(List<String> excludeFields) {
        Set<String> excludedFields = new HashSet<>();

        for (String excludeField : excludeFields) {
            for (String fieldKey : CONVERTER_MAP.keySet()) {
                if (fieldKey.startsWith(excludeField)) {
                    excludedFields.add(fieldKey);
                }
            }
        }

        // Add everything that was not excluded
        Set<String> toInclude = new HashSet<>();
        for (String field : CONVERTER_MAP.keySet()) {
            if (!excludedFields.contains(field)) {
                toInclude.addAll(CONVERTER_MAP.get(field));
            }
        }

        return new ArrayList<>(toInclude);
    }

}
