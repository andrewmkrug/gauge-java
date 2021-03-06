// Copyright 2015 ThoughtWorks, Inc.

// This file is part of Gauge-Java.

// Gauge-Java is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// Gauge-Java is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with Gauge-Java.  If not, see <http://www.gnu.org/licenses/>.

package com.thoughtworks.gauge;

public class SpecificationInfo {
    private Specification currentSpecification;
    private Scenario currentScenario;
    private StepDetails currentStep;

    public SpecificationInfo(Specification specification, Scenario scenario, StepDetails stepDetails) {

        this.currentSpecification = specification;
        this.currentScenario = scenario;
        this.currentStep = stepDetails;
    }

    public SpecificationInfo() {
    }

    public Specification getCurrentSpecification() {
        return currentSpecification;
    }

    public Scenario getCurrentScenario() {
        return currentScenario;
    }

    public StepDetails getCurrentStep() {
        return currentStep;
    }

}
