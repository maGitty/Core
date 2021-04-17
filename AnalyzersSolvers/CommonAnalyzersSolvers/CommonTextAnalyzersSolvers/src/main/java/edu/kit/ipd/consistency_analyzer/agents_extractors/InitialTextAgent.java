package edu.kit.ipd.consistency_analyzer.agents_extractors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.kohsuke.MetaInfServices;

import edu.kit.ipd.consistency_analyzer.agents_extractors.agents.Configuration;
import edu.kit.ipd.consistency_analyzer.agents_extractors.agents.DependencyType;
import edu.kit.ipd.consistency_analyzer.agents_extractors.agents.Loader;
import edu.kit.ipd.consistency_analyzer.agents_extractors.agents.TextAgent;
import edu.kit.ipd.consistency_analyzer.agents_extractors.extractors.IExtractor;
import edu.kit.ipd.consistency_analyzer.agents_extractors.extractors.TextExtractor;
import edu.kit.ipd.consistency_analyzer.datastructures.IText;
import edu.kit.ipd.consistency_analyzer.datastructures.ITextState;
import edu.kit.ipd.consistency_analyzer.datastructures.IWord;

@MetaInfServices(TextAgent.class)
public class InitialTextAgent extends TextAgent {

    private List<IExtractor> extractors = new ArrayList<>();

    public InitialTextAgent() {
        super(GenericTextConfig.class);
    }

    private InitialTextAgent(IText text, ITextState textState, GenericTextConfig config) {
        super(DependencyType.TEXT, GenericTextConfig.class, text, textState);
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

    private void initializeAgents(List<String> extractorList, GenericTextConfig config) {
        Map<String, TextExtractor> loadedExtractors = Loader.loadLoadable(TextExtractor.class);

        for (String textExtractor : extractorList) {
            if (!loadedExtractors.containsKey(textExtractor)) {
                throw new IllegalArgumentException("TextAgent " + textExtractor + " not found");
            }
            extractors.add(loadedExtractors.get(textExtractor).create(textState, config));
        }
    }
}
