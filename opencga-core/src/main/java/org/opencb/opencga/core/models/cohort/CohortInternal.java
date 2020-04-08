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

package org.opencb.opencga.core.models.cohort;

public class CohortInternal {

    private CohortStatus status;

    public CohortInternal() {
    }

    public CohortInternal(CohortStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CohortInternal{");
        sb.append("status=").append(status);
        sb.append('}');
        return sb.toString();
    }

    public CohortStatus getStatus() {
        return status;
    }

    public CohortInternal setStatus(CohortStatus status) {
        this.status = status;
        return this;
    }
}