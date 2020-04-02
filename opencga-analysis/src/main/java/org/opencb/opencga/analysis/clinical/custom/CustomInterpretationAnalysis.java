/*
 * Copyright 2015-2020 OpenCB
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

package org.opencb.opencga.analysis.clinical.custom;

import org.apache.commons.lang3.StringUtils;
import org.opencb.biodata.models.clinical.interpretation.DiseasePanel;
import org.opencb.commons.datastore.core.Query;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.opencga.analysis.clinical.InterpretationAnalysis;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.core.tools.annotations.Tool;
import org.opencb.opencga.core.exceptions.ToolException;
import org.opencb.opencga.core.models.clinical.ClinicalAnalysis;
import org.opencb.opencga.core.models.common.Enums;
import org.opencb.opencga.core.response.OpenCGAResult;
import org.opencb.opencga.storage.core.variant.adaptors.VariantQueryParam;

import java.util.List;

import static org.opencb.opencga.analysis.variant.manager.VariantCatalogQueryUtils.FAMILY;
import static org.opencb.opencga.analysis.variant.manager.VariantCatalogQueryUtils.FAMILY_DISORDER;

@Tool(id = CustomInterpretationAnalysis.ID, resource = Enums.Resource.CLINICAL)
public class CustomInterpretationAnalysis extends InterpretationAnalysis {

    public final static String ID = "custom";

    private String studyId;
    private String clinicalAnalysisId;
    private Query query;
    private QueryOptions queryOptions;
    private CustomInterpretationConfiguration config;

    private ClinicalAnalysis clinicalAnalysis;
    private List<DiseasePanel> diseasePanels;

    @Override
    protected void check() throws Exception {
        super.check();

        // Sanity check
        if (query == null) {
            query = new Query();
        }
        if (queryOptions == null) {
            queryOptions = QueryOptions.empty();
        }

        // Check study
        if (StringUtils.isNotEmpty(studyId)) {
            if (query.containsKey(VariantQueryParam.STUDY.key()) && !studyId.equals(query.get(VariantQueryParam.STUDY.key()))) {
                // Query contains a different study than the input parameter
                throw new ToolException("Mismatch study: query (" + query.getString(VariantQueryParam.STUDY.key())
                        + ") and input parameter (" + studyId + ")");
            } else {
                query.put(VariantQueryParam.STUDY.key(), studyId);
            }
        } else if (!query.containsKey(VariantQueryParam.STUDY.key())) {
            // Missing study
            throw new ToolException("Missing study ID");
        }

        // Check clinical analysis
        if (StringUtils.isEmpty(clinicalAnalysisId)) {
            throw new ToolException("Missing clinical analysis ID");
        }

        // Get clinical analysis to ckeck proband sample ID, family ID
        OpenCGAResult<ClinicalAnalysis> clinicalAnalysisQueryResult;
        try {
            clinicalAnalysisQueryResult = catalogManager.getClinicalAnalysisManager().get(studyId, clinicalAnalysisId, QueryOptions.empty(),
                    token);
        } catch (CatalogException e) {
            throw new ToolException(e);
        }
        if (clinicalAnalysisQueryResult.getNumResults() != 1) {
            throw new ToolException("Clinical analysis " + clinicalAnalysisId + " not found in study " + studyId);
        }

        clinicalAnalysis = clinicalAnalysisQueryResult.first();

        // Proband ID
        if (clinicalAnalysis.getProband() != null && StringUtils.isNotEmpty(clinicalAnalysis.getProband().getId())) {
            String probandSampleId = clinicalAnalysis.getProband().getSamples().get(0).getId();
            if (query.containsKey(VariantQueryParam.SAMPLE.key()) && !probandSampleId.equals(query.get(VariantQueryParam.SAMPLE.key()))) {
                // Query contains a different sample than clinical analysis
                throw new ToolException("Mismatch sample: query (" + query.getString(VariantQueryParam.SAMPLE.key())
                        + ") and clinical analysis (" + probandSampleId + ")");
//            } else {
//                query.put(VariantQueryParam.SAMPLE.key(), probandSampleId);
            }
        }

        // Family ID
        if (clinicalAnalysis.getFamily() != null && StringUtils.isNotEmpty(clinicalAnalysis.getFamily().getId())) {
            String familyId = clinicalAnalysis.getFamily().getId();
            if (query.containsKey(FAMILY.key()) && !familyId.equals(query.get(FAMILY.key()))) {
                // Query contains a different family than clinical analysis
                throw new ToolException("Mismatch family: query (" + query.getString(FAMILY.key()) + ") and clinical analysis ("
                        + familyId + ")");
//            } else {
//                query.put(FAMILY.key(), familyId);
            }
        }

        // Check disorder
        if (clinicalAnalysis.getDisorder() != null && StringUtils.isNotEmpty(clinicalAnalysis.getDisorder().getId())) {
            String disorderId = clinicalAnalysis.getDisorder().getId();
            if (query.containsKey(FAMILY_DISORDER.key())
                    && !disorderId.equals(query.get(FAMILY_DISORDER.key()))) {
                // Query contains a different disorder than clinical analysis
                throw new ToolException("Mismatch disorder: query (" + query.getString(FAMILY_DISORDER.key())
                        + ") and clinical analysis (" + disorderId + ")");
//            } else {
//                query.put(FAMILY_DISORDER.key(), disorderId);
            }
        }

        // Check disease panels
        diseasePanels = clinicalInterpretationManager.getDiseasePanels(query, token);

        // Update executor params with OpenCGA home and session ID
        setUpStorageEngineExecutor(studyId);
    }

    @Override
    protected void run() throws ToolException {
        step(() -> {
            getToolExecutor(CustomInterpretationAnalysisExecutor.class)
                    .setClinicalAnalysisId(clinicalAnalysisId)
                    .setQuery(query)
                    .setQueryOptions(queryOptions)
                    .setConfig(config)
                    .execute();

            saveInterpretation(studyId, clinicalAnalysis, diseasePanels, query, config);
        });
    }

    public String getStudyId() {
        return studyId;
    }

    public CustomInterpretationAnalysis setStudyId(String studyId) {
        this.studyId = studyId;
        return this;
    }

    public String getClinicalAnalysisId() {
        return clinicalAnalysisId;
    }

    public CustomInterpretationAnalysis setClinicalAnalysisId(String clinicalAnalysisId) {
        this.clinicalAnalysisId = clinicalAnalysisId;
        return this;
    }

    public Query getQuery() {
        return query;
    }

    public CustomInterpretationAnalysis setQuery(Query query) {
        this.query = query;
        return this;
    }

    public QueryOptions getQueryOptions() {
        return queryOptions;
    }

    public CustomInterpretationAnalysis setQueryOptions(QueryOptions queryOptions) {
        this.queryOptions = queryOptions;
        return this;
    }

    public CustomInterpretationConfiguration getConfig() {
        return config;
    }

    public CustomInterpretationAnalysis setConfig(CustomInterpretationConfiguration config) {
        this.config = config;
        return this;
    }
}
