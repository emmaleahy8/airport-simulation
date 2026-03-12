package com.group32.cs261project.frontend.compare;

import com.group32.cs261project.backend.compare.RunCompareCompletedRun;

/**
 * Data object passed when navigating to the RunComparePage.
 * It contains the two runs that should be compared.
 */
public record RunCompareSelectionData(

    RunCompareCompletedRun leftRun,
    RunCompareCompletedRun rightRun

) {}