package edu.kit.kastel.mcse.ardoco.core.textextractor.agents;

import java.util.Map;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.kohsuke.MetaInfServices;

import edu.kit.kastel.mcse.ardoco.core.datastructures.agents.Configuration;
import edu.kit.kastel.mcse.ardoco.core.datastructures.agents.Loader;
import edu.kit.kastel.mcse.ardoco.core.datastructures.agents.TextAgent;
import edu.kit.kastel.mcse.ardoco.core.datastructures.definitions.IText;
import edu.kit.kastel.mcse.ardoco.core.datastructures.definitions.ITextState;
import edu.kit.kastel.mcse.ardoco.core.datastructures.definitions.IWord;
import edu.kit.kastel.mcse.ardoco.core.datastructures.extractors.IExtractor;
import edu.kit.kastel.mcse.ardoco.core.datastructures.extractors.TextExtractor;
import edu.kit.kastel.mcse.ardoco.core.textextractor.GenericTextConfig;

/**
 * The Class InitialTextAgent defines the agent that executes the extractors for the text stage.
 */
@MetaInfServices(TextAgent.class)
public class InitialTextAgent extends TextAgent {

    private MutableList<IExtractor> extractors = Lists.mutable.empty();

    /**
     * Instantiates a new initial text agent.
     */
    public InitialTextAgent() {
        super(GenericTextConfig.class);
    }

    private InitialTextAgent(IText text, ITextState textState, GenericTextConfig config) {
        super(GenericTextConfig.class, text, textState);
        initializeAgents(config.textExtractors, config);
    }

    @Override
    public TextAgent create(IText text, ITextState textExtractionState, Configuration config) {
        return new InitialTextAgent(text, textExtractionState, (GenericTextConfig) config);
    }

    @Override
    public void exec() {
        for (IWord word : text.getWords()) {
            for (IExtractor extractor : extractors) {
                extractor.exec(word);
            }
        }
    }

    private void initializeAgents(ImmutableList<String> extractorList, GenericTextConfig config) {
        Map<String, TextExtractor> loadedExtractors = Loader.loadLoadable(TextExtractor.class);

        for (String textExtractor : extractorList) {
            if (!loadedExtractors.containsKey(textExtractor)) {
                throw new IllegalArgumentException("TextAgent " + textExtractor + " not found");
            }
            extractors.add(loadedExtractors.get(textExtractor).create(textState, config));
        }
    }
}
