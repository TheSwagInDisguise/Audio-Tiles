package com.sony.xm6controller.domain

/**
 * Pre-indexed AutoEq & PEQDB profiles pulled directly from official measurements
 * (oratory1990, Rtings Bruel & Kjaer 5128, Kuulokenurkka, DHRME).
 */
val AUTOEQ_OFFICIAL_ENTRIES: List<AutoEqCatalogEntry> = listOf(
    AutoEqCatalogEntry(
        id = "wh1000xm6_harman",
        modelName = "WH-1000XM6",
        targetName = "Harman Over-Ear Target",
        labSource = "Kuulokenurkka",
        description = "Kuulokenurkka lab measurement; sub-bass elevation, balanced 250Hz warmth, refined pinna gain at 3.2kHz",
        profile = PeqProfile(
            name = "WH-1000XM6 (Kuulokenurkka)",
            preampDb = -4.4f,
            filters = listOf(
                PeqFilter(PeqFilterType.LOW_SHELF, 105.0f, -4.0f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 181.0f, -2.7f, 0.79f),
                PeqFilter(PeqFilterType.PEAK, 7457.0f, 5.2f, 1.98f),
                PeqFilter(PeqFilterType.PEAK, 1298.0f, 4.2f, 1.37f),
                PeqFilter(PeqFilterType.PEAK, 2112.0f, -2.9f, 1.13f),
                PeqFilter(PeqFilterType.HIGH_SHELF, 10000.0f, -4.1f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 53.0f, 0.6f, 1.86f),
                PeqFilter(PeqFilterType.PEAK, 637.0f, -0.9f, 3.67f),
                PeqFilter(PeqFilterType.PEAK, 840.0f, 0.9f, 4.20f),
                PeqFilter(PeqFilterType.PEAK, 93.0f, -0.2f, 1.45f),
            )
        )
    ),
    AutoEqCatalogEntry(
        id = "wh1000xm5_oratory",
        modelName = "WH-1000XM5",
        targetName = "Harman Over-Ear 2018",
        labSource = "oratory1990",
        description = "oratory1990 acoustic lab; tames 180Hz mid-bass bloat and smooths upper treble response",
        profile = PeqProfile(
            name = "WH-1000XM5 (oratory1990)",
            preampDb = -6.2f,
            filters = listOf(
                PeqFilter(PeqFilterType.LOW_SHELF, 105.0f, -3.2f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 2448.0f, 6.9f, 2.46f),
                PeqFilter(PeqFilterType.PEAK, 173.0f, -5.6f, 0.96f),
                PeqFilter(PeqFilterType.PEAK, 3028.0f, -5.4f, 2.03f),
                PeqFilter(PeqFilterType.PEAK, 1327.0f, 3.3f, 0.58f),
                PeqFilter(PeqFilterType.HIGH_SHELF, 10000.0f, 4.9f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 6110.0f, -2.3f, 5.81f),
                PeqFilter(PeqFilterType.PEAK, 875.0f, -1.2f, 4.07f),
                PeqFilter(PeqFilterType.PEAK, 1197.0f, 1.0f, 3.28f),
                PeqFilter(PeqFilterType.PEAK, 63.0f, 0.4f, 2.13f),
            )
        )
    ),
    AutoEqCatalogEntry(
        id = "wh1000xm5_rtings",
        modelName = "WH-1000XM5",
        targetName = "Rtings B&K 5128 Target",
        labSource = "Rtings.com",
        description = "Rtings Bruel & Kjaer 5128 fixture; neutral studio target with linear midrange transition",
        profile = PeqProfile(
            name = "WH-1000XM5 (Rtings.com)",
            preampDb = -4.2f,
            filters = listOf(
                PeqFilter(PeqFilterType.LOW_SHELF, 105.0f, -2.4f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 162.0f, -4.9f, 0.77f),
                PeqFilter(PeqFilterType.PEAK, 1069.0f, 4.5f, 0.64f),
                PeqFilter(PeqFilterType.PEAK, 1672.0f, -3.6f, 2.91f),
                PeqFilter(PeqFilterType.PEAK, 72.0f, 1.7f, 1.77f),
                PeqFilter(PeqFilterType.HIGH_SHELF, 10000.0f, 4.2f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 3359.0f, -2.2f, 3.34f),
                PeqFilter(PeqFilterType.PEAK, 2550.0f, 1.9f, 5.34f),
                PeqFilter(PeqFilterType.PEAK, 5848.0f, 1.3f, 5.58f),
                PeqFilter(PeqFilterType.PEAK, 884.0f, -0.7f, 4.97f),
            )
        )
    ),
    AutoEqCatalogEntry(
        id = "wh1000xm4_oratory",
        modelName = "WH-1000XM4",
        targetName = "Harman Over-Ear 2018",
        labSource = "oratory1990",
        description = "oratory1990 acoustic lab; massive reduction in 200Hz muddiness with extended sub-bass rumble",
        profile = PeqProfile(
            name = "WH-1000XM4 (oratory1990)",
            preampDb = -6.1f,
            filters = listOf(
                PeqFilter(PeqFilterType.LOW_SHELF, 105.0f, -4.2f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 143.0f, -5.2f, 1.10f),
                PeqFilter(PeqFilterType.PEAK, 2289.0f, 6.1f, 1.57f),
                PeqFilter(PeqFilterType.PEAK, 56.0f, 1.2f, 1.19f),
                PeqFilter(PeqFilterType.PEAK, 5144.0f, -3.2f, 6.00f),
                PeqFilter(PeqFilterType.HIGH_SHELF, 10000.0f, -1.0f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 407.0f, 1.7f, 3.14f),
                PeqFilter(PeqFilterType.PEAK, 6715.0f, 3.0f, 5.99f),
                PeqFilter(PeqFilterType.PEAK, 1007.0f, 1.0f, 3.41f),
                PeqFilter(PeqFilterType.PEAK, 576.0f, -1.2f, 3.55f),
            )
        )
    ),
    AutoEqCatalogEntry(
        id = "wh1000xm4_rtings",
        modelName = "WH-1000XM4",
        targetName = "Rtings B&K 5128 Target",
        labSource = "Rtings.com",
        description = "Rtings Bruel & Kjaer 5128 fixture; compensates XM4 ear cup acoustic resonance",
        profile = PeqProfile(
            name = "WH-1000XM4 (Rtings.com)",
            preampDb = -6.6f,
            filters = listOf(
                PeqFilter(PeqFilterType.LOW_SHELF, 105.0f, -2.9f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 139.0f, -5.3f, 1.01f),
                PeqFilter(PeqFilterType.PEAK, 2221.0f, 6.3f, 2.85f),
                PeqFilter(PeqFilterType.PEAK, 728.0f, 2.0f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 66.0f, 1.5f, 1.28f),
                PeqFilter(PeqFilterType.HIGH_SHELF, 10000.0f, 2.0f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 8894.0f, -3.2f, 1.78f),
                PeqFilter(PeqFilterType.PEAK, 5132.0f, -2.7f, 6.00f),
                PeqFilter(PeqFilterType.PEAK, 6359.0f, 2.5f, 5.99f),
                PeqFilter(PeqFilterType.PEAK, 585.0f, -0.7f, 3.91f),
            )
        )
    ),
    AutoEqCatalogEntry(
        id = "wh1000xm3_oratory",
        modelName = "WH-1000XM3",
        targetName = "Harman Over-Ear",
        labSource = "oratory1990",
        description = "oratory1990 acoustic lab; controls XM3 heavy warm bass response and restores vocal presence",
        profile = PeqProfile(
            name = "WH-1000XM3 (oratory1990)",
            preampDb = -5.3f,
            filters = listOf(
                PeqFilter(PeqFilterType.LOW_SHELF, 105.0f, -4.1f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 143.0f, -5.9f, 1.02f),
                PeqFilter(PeqFilterType.PEAK, 1411.0f, 6.2f, 1.10f),
                PeqFilter(PeqFilterType.PEAK, 697.0f, -2.8f, 0.92f),
                PeqFilter(PeqFilterType.PEAK, 4315.0f, 4.4f, 1.98f),
                PeqFilter(PeqFilterType.HIGH_SHELF, 10000.0f, 1.4f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 9032.0f, -1.5f, 2.00f),
                PeqFilter(PeqFilterType.PEAK, 6657.0f, 2.1f, 6.00f),
                PeqFilter(PeqFilterType.PEAK, 5637.0f, -1.2f, 5.83f),
                PeqFilter(PeqFilterType.PEAK, 55.0f, 0.3f, 2.24f),
            )
        )
    ),
    AutoEqCatalogEntry(
        id = "wh1000xm2_oratory",
        modelName = "WH-1000XM2",
        targetName = "Harman Over-Ear",
        labSource = "oratory1990",
        description = "oratory1990 acoustic lab; balanced frequency curve for 2nd gen flagship",
        profile = PeqProfile(
            name = "WH-1000XM2 (oratory1990)",
            preampDb = -5.4f,
            filters = listOf(
                PeqFilter(PeqFilterType.LOW_SHELF, 105.0f, -0.7f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 1206.0f, 8.9f, 0.46f),
                PeqFilter(PeqFilterType.PEAK, 186.0f, -5.0f, 0.54f),
                PeqFilter(PeqFilterType.PEAK, 1741.0f, -11.8f, 1.24f),
                PeqFilter(PeqFilterType.PEAK, 80.0f, 1.7f, 0.51f),
                PeqFilter(PeqFilterType.HIGH_SHELF, 10000.0f, 3.8f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 5543.0f, -2.7f, 6.00f),
                PeqFilter(PeqFilterType.PEAK, 851.0f, 1.3f, 3.41f),
                PeqFilter(PeqFilterType.PEAK, 669.0f, -1.1f, 2.93f),
                PeqFilter(PeqFilterType.PEAK, 3522.0f, 1.5f, 6.00f),
            )
        )
    ),
    AutoEqCatalogEntry(
        id = "mdr1000x_oratory",
        modelName = "MDR-1000X",
        targetName = "Harman Over-Ear",
        labSource = "oratory1990",
        description = "oratory1990 acoustic lab; reference compensation for original 1000X over-ear model",
        profile = PeqProfile(
            name = "MDR-1000X (oratory1990)",
            preampDb = -5.6f,
            filters = listOf(
                PeqFilter(PeqFilterType.LOW_SHELF, 105.0f, 0.9f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 150.0f, -3.6f, 0.88f),
                PeqFilter(PeqFilterType.PEAK, 1161.0f, 6.0f, 1.95f),
                PeqFilter(PeqFilterType.PEAK, 3038.0f, 2.0f, 2.89f),
                PeqFilter(PeqFilterType.PEAK, 1773.0f, -3.0f, 2.92f),
                PeqFilter(PeqFilterType.HIGH_SHELF, 10000.0f, 2.5f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 5816.0f, -4.2f, 6.00f),
                PeqFilter(PeqFilterType.PEAK, 380.0f, 1.3f, 4.43f),
                PeqFilter(PeqFilterType.PEAK, 277.0f, -0.8f, 3.98f),
                PeqFilter(PeqFilterType.PEAK, 4259.0f, -1.1f, 6.00f),
            )
        )
    ),
    AutoEqCatalogEntry(
        id = "wf1000xm5_oratory",
        modelName = "WF-1000XM5",
        targetName = "Harman In-Ear 2019",
        labSource = "oratory1990",
        description = "oratory1990 acoustic lab; tight ear-canal target with punchy sub-bass and pristine vocal clarity",
        profile = PeqProfile(
            name = "WF-1000XM5 (oratory1990)",
            preampDb = -2.6f,
            filters = listOf(
                PeqFilter(PeqFilterType.LOW_SHELF, 105.0f, 5.4f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 68.0f, -4.8f, 0.27f),
                PeqFilter(PeqFilterType.PEAK, 1377.0f, 2.6f, 0.31f),
                PeqFilter(PeqFilterType.PEAK, 1806.0f, -3.5f, 1.16f),
                PeqFilter(PeqFilterType.PEAK, 6875.0f, 1.8f, 3.49f),
                PeqFilter(PeqFilterType.HIGH_SHELF, 10000.0f, 2.3f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 3757.0f, 0.3f, 2.56f),
                PeqFilter(PeqFilterType.PEAK, 5057.0f, -0.7f, 5.70f),
                PeqFilter(PeqFilterType.PEAK, 449.0f, 0.3f, 4.15f),
                PeqFilter(PeqFilterType.PEAK, 677.0f, -0.2f, 2.75f),
            )
        )
    ),
    AutoEqCatalogEntry(
        id = "wf1000xm5_rtings",
        modelName = "WF-1000XM5",
        targetName = "Rtings B&K 5128 In-Ear",
        labSource = "Rtings.com",
        description = "Rtings Bruel & Kjaer 5128 anthropomorphic ear fixture; neutral treble extension",
        profile = PeqProfile(
            name = "WF-1000XM5 (Rtings.com)",
            preampDb = -4.8f,
            filters = listOf(
                PeqFilter(PeqFilterType.LOW_SHELF, 105.0f, -1.0f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 135.0f, -3.3f, 0.48f),
                PeqFilter(PeqFilterType.PEAK, 540.0f, 4.5f, 1.53f),
                PeqFilter(PeqFilterType.PEAK, 399.0f, -0.9f, 1.11f),
                PeqFilter(PeqFilterType.PEAK, 927.0f, 1.4f, 1.60f),
                PeqFilter(PeqFilterType.HIGH_SHELF, 10000.0f, 4.8f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 9764.0f, -3.2f, 3.87f),
                PeqFilter(PeqFilterType.PEAK, 1975.0f, -0.8f, 3.15f),
                PeqFilter(PeqFilterType.PEAK, 10000.0f, 0.3f, 3.43f),
                PeqFilter(PeqFilterType.PEAK, 4313.0f, -1.5f, 6.00f),
            )
        )
    ),
    AutoEqCatalogEntry(
        id = "wf1000xm4_oratory",
        modelName = "WF-1000XM4",
        targetName = "Harman In-Ear 2019",
        labSource = "oratory1990",
        description = "oratory1990 acoustic lab; balances XM4 warm sound signature with enhanced pinna response",
        profile = PeqProfile(
            name = "WF-1000XM4 (oratory1990)",
            preampDb = -7.5f,
            filters = listOf(
                PeqFilter(PeqFilterType.LOW_SHELF, 105.0f, -0.6f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 176.0f, -4.2f, 0.60f),
                PeqFilter(PeqFilterType.PEAK, 9973.0f, 3.7f, 1.01f),
                PeqFilter(PeqFilterType.PEAK, 6316.0f, 4.4f, 0.91f),
                PeqFilter(PeqFilterType.PEAK, 498.0f, -1.4f, 1.46f),
                PeqFilter(PeqFilterType.HIGH_SHELF, 10000.0f, 3.7f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 1732.0f, 1.2f, 2.28f),
                PeqFilter(PeqFilterType.PEAK, 3942.0f, -0.8f, 3.10f),
                PeqFilter(PeqFilterType.PEAK, 978.0f, -0.3f, 1.49f),
                PeqFilter(PeqFilterType.PEAK, 38.0f, -0.2f, 2.02f),
            )
        )
    ),
    AutoEqCatalogEntry(
        id = "wf1000xm4_rtings",
        modelName = "WF-1000XM4",
        targetName = "Rtings In-Ear Target",
        labSource = "Rtings.com",
        description = "Rtings HMS II.3 fixture; linear vocal presentation and controlled sub-bass decay",
        profile = PeqProfile(
            name = "WF-1000XM4 (Rtings.com)",
            preampDb = -5.8f,
            filters = listOf(
                PeqFilter(PeqFilterType.LOW_SHELF, 105.0f, -0.1f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 6519.0f, 5.7f, 0.94f),
                PeqFilter(PeqFilterType.PEAK, 185.0f, -3.5f, 0.58f),
                PeqFilter(PeqFilterType.PEAK, 9028.0f, 1.9f, 2.38f),
                PeqFilter(PeqFilterType.PEAK, 415.0f, -1.4f, 1.62f),
                PeqFilter(PeqFilterType.HIGH_SHELF, 10000.0f, -2.7f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 1299.0f, -1.2f, 2.84f),
                PeqFilter(PeqFilterType.PEAK, 872.0f, 0.8f, 2.83f),
                PeqFilter(PeqFilterType.PEAK, 3745.0f, 0.9f, 3.06f),
                PeqFilter(PeqFilterType.PEAK, 4587.0f, -0.7f, 4.95f),
            )
        )
    ),
    AutoEqCatalogEntry(
        id = "wf1000xm3_oratory",
        modelName = "WF-1000XM3",
        targetName = "Harman In-Ear",
        labSource = "oratory1990",
        description = "oratory1990 acoustic lab; cleans up 300Hz lower-midrange congestion and extends highs",
        profile = PeqProfile(
            name = "WF-1000XM3 (oratory1990)",
            preampDb = -3.9f,
            filters = listOf(
                PeqFilter(PeqFilterType.LOW_SHELF, 105.0f, 4.2f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 121.0f, -3.6f, 0.55f),
                PeqFilter(PeqFilterType.PEAK, 4159.0f, 2.2f, 2.21f),
                PeqFilter(PeqFilterType.PEAK, 7193.0f, 2.5f, 3.75f),
                PeqFilter(PeqFilterType.PEAK, 925.0f, 1.4f, 2.39f),
                PeqFilter(PeqFilterType.HIGH_SHELF, 10000.0f, -4.6f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 8983.0f, 2.0f, 2.67f),
                PeqFilter(PeqFilterType.PEAK, 1941.0f, 1.2f, 2.99f),
                PeqFilter(PeqFilterType.PEAK, 2602.0f, -0.8f, 4.48f),
                PeqFilter(PeqFilterType.PEAK, 991.0f, -0.3f, 3.35f),
            )
        )
    ),
    AutoEqCatalogEntry(
        id = "linkbuds_s_rtings",
        modelName = "LinkBuds S",
        targetName = "Rtings B&K 5128 In-Ear",
        labSource = "Rtings.com",
        description = "Rtings B&K 5128; neutral balance compensation for LinkBuds S compact ANC buds",
        profile = PeqProfile(
            name = "LinkBuds S (Rtings.com)",
            preampDb = -7.1f,
            filters = listOf(
                PeqFilter(PeqFilterType.LOW_SHELF, 105.0f, -1.2f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 8757.0f, 6.6f, 1.67f),
                PeqFilter(PeqFilterType.PEAK, 168.0f, -2.7f, 0.53f),
                PeqFilter(PeqFilterType.PEAK, 1342.0f, -2.3f, 1.13f),
                PeqFilter(PeqFilterType.PEAK, 483.0f, 3.5f, 1.56f),
                PeqFilter(PeqFilterType.HIGH_SHELF, 10000.0f, 6.8f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 3416.0f, 2.2f, 2.67f),
                PeqFilter(PeqFilterType.PEAK, 9388.0f, -2.0f, 0.49f),
                PeqFilter(PeqFilterType.PEAK, 2188.0f, -1.3f, 3.31f),
                PeqFilter(PeqFilterType.PEAK, 2953.0f, 0.9f, 2.15f),
            )
        )
    ),
    AutoEqCatalogEntry(
        id = "linkbuds_wfl900_rtings",
        modelName = "LinkBuds (WF-L900)",
        targetName = "Rtings Open-Ear Target",
        labSource = "Rtings.com",
        description = "Rtings HMS II.3; open ring driver compensation boosting bass foundation and smoothing 4kHz",
        profile = PeqProfile(
            name = "LinkBuds (WF-L900) (Rtings.com)",
            preampDb = -6.2f,
            filters = listOf(
                PeqFilter(PeqFilterType.LOW_SHELF, 105.0f, 5.6f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 1600.0f, -6.2f, 0.22f),
                PeqFilter(PeqFilterType.PEAK, 3687.0f, 8.7f, 2.25f),
                PeqFilter(PeqFilterType.PEAK, 7572.0f, 7.4f, 1.22f),
                PeqFilter(PeqFilterType.PEAK, 154.0f, 4.7f, 0.70f),
                PeqFilter(PeqFilterType.HIGH_SHELF, 10000.0f, 1.2f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 1148.0f, 1.6f, 1.00f),
                PeqFilter(PeqFilterType.PEAK, 1856.0f, -2.8f, 3.88f),
                PeqFilter(PeqFilterType.PEAK, 432.0f, -1.6f, 0.86f),
                PeqFilter(PeqFilterType.PEAK, 307.0f, 2.8f, 3.64f),
            )
        )
    ),
    AutoEqCatalogEntry(
        id = "linkbuds_fit_dhrme",
        modelName = "LinkBuds Fit",
        targetName = "DHRME In-Ear Target",
        labSource = "DHRME",
        description = "DHRME acoustic measurements; lively tuning for LinkBuds Fit active ergonomics",
        profile = PeqProfile(
            name = "LinkBuds Fit (DHRME)",
            preampDb = -2.6f,
            filters = listOf(
                PeqFilter(PeqFilterType.LOW_SHELF, 105.0f, 1.1f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 4083.0f, 2.6f, 1.56f),
                PeqFilter(PeqFilterType.PEAK, 610.0f, -1.8f, 1.06f),
                PeqFilter(PeqFilterType.PEAK, 136.0f, -1.3f, 1.53f),
                PeqFilter(PeqFilterType.PEAK, 349.0f, 1.8f, 2.38f),
                PeqFilter(PeqFilterType.HIGH_SHELF, 10000.0f, 2.1f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 1742.0f, 1.4f, 4.79f),
                PeqFilter(PeqFilterType.PEAK, 1265.0f, -1.0f, 3.74f),
                PeqFilter(PeqFilterType.PEAK, 5705.0f, -1.4f, 6.00f),
                PeqFilter(PeqFilterType.PEAK, 29.0f, -0.5f, 3.50f),
            )
        )
    ),
    AutoEqCatalogEntry(
        id = "wh_ult900n_rtings",
        modelName = "WH-ULT900N",
        targetName = "Rtings B&K 5128 Target",
        labSource = "Rtings.com",
        description = "Rtings B&K 5128; controls ULT WEAR immense bass shelf and restores critical vocal frequencies",
        profile = PeqProfile(
            name = "WH-ULT900N (Rtings.com)",
            preampDb = -6.2f,
            filters = listOf(
                PeqFilter(PeqFilterType.LOW_SHELF, 105.0f, -6.5f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 124.0f, -6.8f, 0.89f),
                PeqFilter(PeqFilterType.PEAK, 2871.0f, 5.6f, 1.45f),
                PeqFilter(PeqFilterType.PEAK, 60.0f, 3.0f, 1.43f),
                PeqFilter(PeqFilterType.PEAK, 4198.0f, 3.9f, 3.46f),
                PeqFilter(PeqFilterType.HIGH_SHELF, 10000.0f, 3.0f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 9001.0f, -2.9f, 2.15f),
                PeqFilter(PeqFilterType.PEAK, 768.0f, 0.8f, 3.47f),
                PeqFilter(PeqFilterType.PEAK, 345.0f, 0.7f, 1.89f),
                PeqFilter(PeqFilterType.PEAK, 209.0f, -0.7f, 2.71f),
            )
        )
    ),
    AutoEqCatalogEntry(
        id = "wh_ult900n_kuulo",
        modelName = "WH-ULT900N",
        targetName = "Harman Over-Ear Target",
        labSource = "Kuulokenurkka",
        description = "Kuulokenurkka measurement; audiophile balance curve for ULT WEAR hardware drivers",
        profile = PeqProfile(
            name = "WH-ULT900N (Kuulokenurkka)",
            preampDb = -6.4f,
            filters = listOf(
                PeqFilter(PeqFilterType.LOW_SHELF, 105.0f, -8.4f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 114.0f, -8.4f, 0.59f),
                PeqFilter(PeqFilterType.PEAK, 4428.0f, 4.2f, 0.50f),
                PeqFilter(PeqFilterType.PEAK, 1985.0f, 2.3f, 1.80f),
                PeqFilter(PeqFilterType.PEAK, 58.0f, 6.0f, 0.99f),
                PeqFilter(PeqFilterType.HIGH_SHELF, 10000.0f, -0.8f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 8933.0f, 2.2f, 2.26f),
                PeqFilter(PeqFilterType.PEAK, 5917.0f, -5.8f, 5.58f),
                PeqFilter(PeqFilterType.PEAK, 4655.0f, 2.6f, 3.33f),
                PeqFilter(PeqFilterType.PEAK, 650.0f, -1.9f, 3.80f),
            )
        )
    ),
    AutoEqCatalogEntry(
        id = "wh_xb910n_rtings",
        modelName = "WH-XB910N",
        targetName = "Rtings B&K 5128 Target",
        labSource = "Rtings.com",
        description = "Rtings B&K 5128; Extra Bass calibration tightening mid-bass boom and opening treble",
        profile = PeqProfile(
            name = "WH-XB910N (Rtings.com)",
            preampDb = -7.3f,
            filters = listOf(
                PeqFilter(PeqFilterType.LOW_SHELF, 105.0f, -9.3f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 149.0f, -5.0f, 1.11f),
                PeqFilter(PeqFilterType.PEAK, 537.0f, 6.7f, 1.94f),
                PeqFilter(PeqFilterType.PEAK, 3897.0f, 7.8f, 1.85f),
                PeqFilter(PeqFilterType.PEAK, 7992.0f, -5.2f, 1.49f),
                PeqFilter(PeqFilterType.HIGH_SHELF, 10000.0f, -0.5f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 1289.0f, -5.9f, 1.54f),
                PeqFilter(PeqFilterType.PEAK, 786.0f, 3.9f, 3.17f),
                PeqFilter(PeqFilterType.PEAK, 2866.0f, 3.2f, 4.32f),
                PeqFilter(PeqFilterType.PEAK, 370.0f, 1.0f, 3.21f),
            )
        )
    ),
    AutoEqCatalogEntry(
        id = "wh_ch720n_rtings",
        modelName = "WH-CH720N",
        targetName = "Rtings B&K 5128 Target",
        labSource = "Rtings.com",
        description = "Rtings B&K 5128; neutral compensation transforming budget V1 processor into studio balance",
        profile = PeqProfile(
            name = "WH-CH720N (Rtings.com)",
            preampDb = -8.6f,
            filters = listOf(
                PeqFilter(PeqFilterType.LOW_SHELF, 105.0f, -1.4f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 152.0f, -5.9f, 1.16f),
                PeqFilter(PeqFilterType.PEAK, 1296.0f, 6.6f, 1.16f),
                PeqFilter(PeqFilterType.PEAK, 4381.0f, 8.8f, 1.34f),
                PeqFilter(PeqFilterType.PEAK, 8912.0f, -5.6f, 0.18f),
                PeqFilter(PeqFilterType.HIGH_SHELF, 10000.0f, 10.5f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 419.0f, 3.1f, 2.95f),
                PeqFilter(PeqFilterType.PEAK, 10000.0f, -5.1f, 1.66f),
                PeqFilter(PeqFilterType.PEAK, 299.0f, -1.5f, 1.98f),
                PeqFilter(PeqFilterType.PEAK, 504.0f, 0.2f, 2.46f),
            )
        )
    ),
    AutoEqCatalogEntry(
        id = "wh_ch520_rtings",
        modelName = "WH-CH520",
        targetName = "Rtings On-Ear Target",
        labSource = "Rtings.com",
        description = "Rtings HMS II.3; on-ear seal compensation with enhanced sub-bass extension",
        profile = PeqProfile(
            name = "WH-CH520 (Rtings.com)",
            preampDb = -6.7f,
            filters = listOf(
                PeqFilter(PeqFilterType.LOW_SHELF, 105.0f, 5.4f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 5060.0f, 7.1f, 2.23f),
                PeqFilter(PeqFilterType.PEAK, 64.0f, -6.4f, 0.83f),
                PeqFilter(PeqFilterType.PEAK, 3013.0f, -3.9f, 3.47f),
                PeqFilter(PeqFilterType.PEAK, 871.0f, -0.8f, 1.82f),
                PeqFilter(PeqFilterType.HIGH_SHELF, 10000.0f, -3.2f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 9568.0f, -1.7f, 1.47f),
                PeqFilter(PeqFilterType.PEAK, 6627.0f, 2.9f, 5.95f),
                PeqFilter(PeqFilterType.PEAK, 1837.0f, 1.5f, 4.58f),
                PeqFilter(PeqFilterType.PEAK, 386.0f, 0.7f, 4.78f),
            )
        )
    ),
    AutoEqCatalogEntry(
        id = "wh_ch510_oratory",
        modelName = "WH-CH510",
        targetName = "Harman On-Ear Target",
        labSource = "oratory1990",
        description = "oratory1990 acoustic lab; flattens mid-range peak and smooths treble on CH510 on-ear",
        profile = PeqProfile(
            name = "WH-CH510 (oratory1990)",
            preampDb = -6.4f,
            filters = listOf(
                PeqFilter(PeqFilterType.LOW_SHELF, 105.0f, 5.5f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 68.0f, -9.8f, 0.83f),
                PeqFilter(PeqFilterType.PEAK, 5368.0f, 6.4f, 2.11f),
                PeqFilter(PeqFilterType.PEAK, 1401.0f, -2.1f, 2.20f),
                PeqFilter(PeqFilterType.PEAK, 2257.0f, 0.9f, 2.59f),
                PeqFilter(PeqFilterType.HIGH_SHELF, 10000.0f, -0.7f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 239.0f, 1.2f, 1.05f),
                PeqFilter(PeqFilterType.PEAK, 120.0f, -1.1f, 1.75f),
                PeqFilter(PeqFilterType.PEAK, 576.0f, -0.7f, 2.91f),
                PeqFilter(PeqFilterType.PEAK, 68.0f, 0.4f, 2.76f),
            )
        )
    ),
    AutoEqCatalogEntry(
        id = "wf_c700n_rtings",
        modelName = "WF-C700N",
        targetName = "Rtings In-Ear Target",
        labSource = "Rtings.com",
        description = "Rtings HMS II.3; neutral target calibration for compact ANC earbuds",
        profile = PeqProfile(
            name = "WF-C700N (Rtings.com)",
            preampDb = -6.5f,
            filters = listOf(
                PeqFilter(PeqFilterType.LOW_SHELF, 105.0f, 7.5f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 4513.0f, 2.2f, 1.15f),
                PeqFilter(PeqFilterType.PEAK, 1318.0f, -2.5f, 1.79f),
                PeqFilter(PeqFilterType.PEAK, 44.0f, -9.3f, 0.49f),
                PeqFilter(PeqFilterType.PEAK, 640.0f, 1.2f, 1.08f),
                PeqFilter(PeqFilterType.HIGH_SHELF, 10000.0f, 6.5f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 9238.0f, -4.5f, 2.54f),
                PeqFilter(PeqFilterType.PEAK, 2641.0f, 0.6f, 3.25f),
                PeqFilter(PeqFilterType.PEAK, 3992.0f, -0.6f, 3.63f),
                PeqFilter(PeqFilterType.PEAK, 5588.0f, 0.9f, 4.62f),
            )
        )
    ),
    AutoEqCatalogEntry(
        id = "wf_c500_oratory",
        modelName = "WF-C500",
        targetName = "Harman In-Ear",
        labSource = "oratory1990",
        description = "oratory1990 acoustic lab; studio compensation for entry-level true wireless",
        profile = PeqProfile(
            name = "WF-C500 (oratory1990)",
            preampDb = -4.6f,
            filters = listOf(
                PeqFilter(PeqFilterType.LOW_SHELF, 105.0f, -4.8f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 2803.0f, -5.3f, 2.29f),
                PeqFilter(PeqFilterType.PEAK, 5036.0f, 5.1f, 2.47f),
                PeqFilter(PeqFilterType.PEAK, 77.0f, 3.6f, 0.68f),
                PeqFilter(PeqFilterType.PEAK, 1938.0f, -0.9f, 1.60f),
                PeqFilter(PeqFilterType.HIGH_SHELF, 10000.0f, -1.8f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 809.0f, 2.1f, 2.01f),
                PeqFilter(PeqFilterType.PEAK, 1281.0f, -1.0f, 2.36f),
                PeqFilter(PeqFilterType.PEAK, 7069.0f, -1.6f, 5.97f),
                PeqFilter(PeqFilterType.PEAK, 6011.0f, 1.4f, 6.00f),
            )
        )
    ),
    AutoEqCatalogEntry(
        id = "wi_1000xm2_rtings",
        modelName = "WI-1000XM2",
        targetName = "Rtings In-Ear Target",
        labSource = "Rtings.com",
        description = "Rtings HMS II.3; hybrid dynamic+BA driver calibration for flagship ANC neckband",
        profile = PeqProfile(
            name = "WI-1000XM2 (Rtings.com)",
            preampDb = -5.5f,
            filters = listOf(
                PeqFilter(PeqFilterType.LOW_SHELF, 105.0f, 4.0f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 85.0f, -3.5f, 0.38f),
                PeqFilter(PeqFilterType.PEAK, 845.0f, 2.8f, 2.03f),
                PeqFilter(PeqFilterType.PEAK, 3415.0f, 2.7f, 2.32f),
                PeqFilter(PeqFilterType.PEAK, 2221.0f, -1.9f, 2.12f),
                PeqFilter(PeqFilterType.HIGH_SHELF, 10000.0f, 5.5f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 9513.0f, -5.3f, 3.75f),
                PeqFilter(PeqFilterType.PEAK, 9925.0f, 0.3f, 6.00f),
                PeqFilter(PeqFilterType.PEAK, 82.0f, 0.1f, 4.75f),
                PeqFilter(PeqFilterType.PEAK, 730.0f, 0.0f, 1.87f),
            )
        )
    ),
    AutoEqCatalogEntry(
        id = "wi_1000x_oratory",
        modelName = "WI-1000X",
        targetName = "Harman In-Ear",
        labSource = "oratory1990",
        description = "oratory1990 acoustic lab; balanced presentation for original ANC neckband",
        profile = PeqProfile(
            name = "WI-1000X (oratory1990)",
            preampDb = -5.2f,
            filters = listOf(
                PeqFilter(PeqFilterType.LOW_SHELF, 105.0f, -1.1f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 167.0f, -4.3f, 0.90f),
                PeqFilter(PeqFilterType.PEAK, 1575.0f, 5.3f, 0.95f),
                PeqFilter(PeqFilterType.PEAK, 82.0f, 0.8f, 0.87f),
                PeqFilter(PeqFilterType.PEAK, 316.0f, -1.3f, 1.66f),
                PeqFilter(PeqFilterType.HIGH_SHELF, 10000.0f, -1.7f, 0.70f),
                PeqFilter(PeqFilterType.PEAK, 9083.0f, -2.7f, 1.80f),
                PeqFilter(PeqFilterType.PEAK, 4553.0f, 1.9f, 6.00f),
                PeqFilter(PeqFilterType.PEAK, 806.0f, 1.1f, 4.01f),
                PeqFilter(PeqFilterType.PEAK, 1016.0f, -0.6f, 3.39f),
            )
        )
    )
)
