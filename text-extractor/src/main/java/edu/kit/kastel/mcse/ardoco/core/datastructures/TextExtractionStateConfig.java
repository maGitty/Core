package edu.kit.kastel.mcse.ardoco.core.datastructures;

import edu.kit.kastel.mcse.ardoco.core.datastructures.extractors.TextExtractor;
import edu.kit.kastel.mcse.ardoco.core.util.ResourceAccessor;

/**
 * The TextExtractionStateConfig for {@link TextExtractor}.
 */
public final class TextExtractionStateConfig {

    private TextExtractionStateConfig() {
        throw new IllegalAccessError();
    }

    private static final ResourceAccessor CONFIG = loadParameters("/configs/TextExtractionState.properties");

    /** The Constant NORT_PROBABILITY_FOR_NAME_AND_TYPE. */
    public static final double NORT_PROBABILITY_FOR_NAME_AND_TYPE = CONFIG.getPropertyAsDouble("nortProbabilityForNameAndType");

    private static ResourceAccessor loadParameters(String filePath) {
        return new ResourceAccessor(filePath, true);
    }

}
