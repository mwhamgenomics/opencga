/*
 * Copyright 2015-2017 OpenCB
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

package org.opencb.opencga.analysis.clinical.interpretation;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.opencb.biodata.models.clinical.interpretation.ClinicalProperty;
import org.opencb.biodata.models.clinical.interpretation.DiseasePanel;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.core.annotations.Analysis;
import org.opencb.opencga.core.exception.AnalysisException;
import org.opencb.opencga.core.models.ClinicalAnalysis;
import org.opencb.opencga.core.results.OpenCGAResult;

import java.util.Collection;
import java.util.List;

@Analysis(id = TeamInterpretationAnalysis.ID, type = Analysis.AnalysisType.CLINICAL)
public class TeamInterpretationAnalysis extends InterpretationAnalysis {

    public final static String ID = "team-interpretation";

    private String studyId;
    private String clinicalAnalysisId;
    private List<String> diseasePanelIds;
    private ClinicalProperty.ModeOfInheritance moi;
    private TeamInterpretationConfiguration config;

    private ClinicalAnalysis clinicalAnalysis;
    private List<DiseasePanel> diseasePanels;

    @Override
    protected void check() throws AnalysisException {
        super.check();

        // Check study
        if (StringUtils.isEmpty(studyId)) {
            throw new AnalysisException("Missing study");
        }
        try {
            catalogManager.getStudyManager().get(studyId, null, sessionId).first().getFqn();
        } catch (CatalogException e) {
            throw new AnalysisException(e);
        }

        // Check clinical analysis
        if (StringUtils.isEmpty(clinicalAnalysisId)) {
            throw new AnalysisException("Missing clinical analysis ID");
        }

        // Get clinical analysis to ckeck proband sample ID, family ID
        OpenCGAResult<ClinicalAnalysis> clinicalAnalysisQueryResult;
        try {
            clinicalAnalysisQueryResult = catalogManager.getClinicalAnalysisManager().get(studyId, clinicalAnalysisId, QueryOptions.empty(),
                    sessionId);
        } catch (CatalogException e) {
            throw new AnalysisException(e);
        }
        if (clinicalAnalysisQueryResult.getNumResults() != 1) {
            throw new AnalysisException("Clinical analysis " + clinicalAnalysisId + " not found in study " + studyId);
        }
        clinicalAnalysis = clinicalAnalysisQueryResult.first();

        // Check disease panels
        if (CollectionUtils.isEmpty(diseasePanelIds)) {
            throw new AnalysisException("Missing disease panels for TEAM interpretation analysis");
        }
        diseasePanels = clinicalInterpretationManager.getDiseasePanels(studyId, diseasePanelIds, sessionId);
        if (CollectionUtils.isEmpty(diseasePanels)) {
            throw new AnalysisException("Disease panels not found for TEAM interpretation analysis: "
                    + StringUtils.join(diseasePanelIds, ","));
        }

        // Update executor params with OpenCGA home and session ID
        setUpStorageEngineExecutor(studyId);
    }

    @Override
    protected void run() throws AnalysisException {

        step(() -> {
            TeamInterpretationAnalysisExecutor executor = new TeamInterpretationAnalysisExecutor();
            setUpAnalysisExecutor(executor);

            executor.setStudyId(studyId)
                    .setClinicalAnalysisId(clinicalAnalysisId)
                    .setDiseasePanels(diseasePanels)
                    .setMoi(moi)
                    .setConfig(config)
                    .execute();

            saveInterpretation(studyId, clinicalAnalysis, diseasePanels, null, config);
        });
    }

    public String getStudyId() {
        return studyId;
    }

    public TeamInterpretationAnalysis setStudyId(String studyId) {
        this.studyId = studyId;
        return this;
    }

    public String getClinicalAnalysisId() {
        return clinicalAnalysisId;
    }

    public TeamInterpretationAnalysis setClinicalAnalysisId(String clinicalAnalysisId) {
        this.clinicalAnalysisId = clinicalAnalysisId;
        return this;
    }

    public List<String> getDiseasePanelIds() {
        return diseasePanelIds;
    }

    public TeamInterpretationAnalysis setDiseasePanelIds(List<String> diseasePanelIds) {
        this.diseasePanelIds = diseasePanelIds;
        return this;
    }

    public ClinicalProperty.ModeOfInheritance getMoi() {
        return moi;
    }

    public TeamInterpretationAnalysis setMoi(ClinicalProperty.ModeOfInheritance moi) {
        this.moi = moi;
        return this;
    }

    public TeamInterpretationConfiguration getConfig() {
        return config;
    }

    public TeamInterpretationAnalysis setConfig(TeamInterpretationConfiguration config) {
        this.config = config;
        return this;
    }
}
