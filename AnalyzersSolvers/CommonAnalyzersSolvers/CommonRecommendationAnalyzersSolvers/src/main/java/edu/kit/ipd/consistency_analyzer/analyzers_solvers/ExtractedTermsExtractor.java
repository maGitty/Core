package edu.kit.ipd.consistency_analyzer.analyzers_solvers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.kohsuke.MetaInfServices;

import edu.kit.ipd.consistency_analyzer.agents.DependencyType;
import edu.kit.ipd.consistency_analyzer.common.SimilarityUtils;
import edu.kit.ipd.consistency_analyzer.datastructures.IModelState;
import edu.kit.ipd.consistency_analyzer.datastructures.INounMapping;
import edu.kit.ipd.consistency_analyzer.datastructures.IRecommendationState;
import edu.kit.ipd.consistency_analyzer.datastructures.ITermMapping;
import edu.kit.ipd.consistency_analyzer.datastructures.ITextState;
import edu.kit.ipd.consistency_analyzer.datastructures.IWord;
import edu.kit.ipd.consistency_analyzer.datastructures.MappingKind;
import edu.kit.ipd.consistency_analyzer.extractors.RecommendationExtractor;

/**
 * This analyzer identifies terms and examines their textual environment.
 *
 * @author Sophie
 *
 */
@MetaInfServices(RecommendationExtractor.class)
public class ExtractedTermsExtractor extends RecommendationExtractor {

    private double probabilityAdjacentTerm = GenericRecommendationConfig.EXTRACTED_TERMS_ANALYZER_PROBABILITY_ADJACENT_TERM;
    private double probabilityJustName = GenericRecommendationConfig.EXTRACTED_TERMS_ANALYZER_PROBABILITY_JUST_NAME;
    private double probabilityJustAdjacentNoun = GenericRecommendationConfig.EXTRACTED_TERMS_ANALYZER_PROBABILITY_ADJACENT_NOUN;

    /**
     * Instantiates a new extracted terms analyzer
     *
     * @param graph                the PARSE graph to work with
     * @param textExtractionState  the text extraction state to work with
     * @param modelExtractionState the model extraction state to work with
     * @param recommendationState  the recommendation state to write the results and read existing recommendations from
     */
    public ExtractedTermsExtractor(ITextState textExtractionState, IModelState modelExtractionState, IRecommendationState recommendationState) {
        super(DependencyType.TEXT_RECOMMENDATION, textExtractionState, modelExtractionState, recommendationState);
    }

    public ExtractedTermsExtractor() {
        this(null, null, null);
    }

    @Override
    public RecommendationExtractor create(ITextState textState, IModelState modelExtractionState, IRecommendationState recommendationState) {
        return new ExtractedTermsExtractor(textState, modelExtractionState, recommendationState);
    }

    @Override
    public void setProbability(List<Double> probabilities) {
        if (probabilities.size() > 3) {
            throw new IllegalArgumentException(getName() + ": The given probabilities are more than needed!");
        } else if (probabilities.isEmpty()) {
            throw new IllegalArgumentException(getName() + ": The given probabilities are empty!");
        } else {
            probabilityAdjacentTerm = probabilities.get(0);
            probabilityJustName = probabilities.get(1);
            probabilityJustAdjacentNoun = probabilities.get(2);
        }
    }

    @Override
    public void exec(IWord n) {

        createRecommendedInstancesForTerm(n);
    }

    private void createRecommendedInstancesForTerm(IWord node) {

        List<ITermMapping> termMappings = textState.getTermMappingsByNode(node);

        termMappings = getPossibleOccurredTermMappingsToThisSpot(termMappings, node);

        if (termMappings.isEmpty()) {
            return;

        }
        for (ITermMapping term : termMappings) {

            List<INounMapping> adjacentNounMappings = getTermAdjacentNounMappings(node, term);
            if (adjacentNounMappings.isEmpty() && term.getKind().equals(MappingKind.NAME)) {

                recommendationState.addRecommendedInstanceJustName(term.getReference(), probabilityJustName, term.getMappings());

            } else {
                createRecommendedInstancesForSurroundingNounMappings(term, adjacentNounMappings);

                List<ITermMapping> adjacentTermMappings = new ArrayList<>();

                for (INounMapping surroundingMapping : adjacentNounMappings) {
                    adjacentTermMappings.addAll(textState.getTermsByContainedMapping(surroundingMapping));
                }

                createRecommendedInstancesForAdjacentTermMappings(node, term, adjacentTermMappings);
            }
        }

    }

    private List<ITermMapping> getPossibleOccurredTermMappingsToThisSpot(List<ITermMapping> termMappings, IWord n) {

        List<ITermMapping> possibleOccuredTermMappings = new ArrayList<>();
        String word = n.getText();

        for (ITermMapping term : termMappings) {
            List<INounMapping> termNounMappings = new ArrayList<>(term.getMappings());

            List<INounMapping> wordMappings = SimilarityUtils.getMostLikelyNMappingsByReference(word, termNounMappings);

            for (INounMapping wordMapping : wordMappings) {

                int position = termNounMappings.indexOf(wordMapping);

                if (//
                !arePreviousWordsEqualToReferences(termNounMappings, n, position) && //
                        !areNextWordsEqualToReferences(termNounMappings, n, position)) {
                    possibleOccuredTermMappings.add(term);
                    break; // TODO: poss. rework -> assumption: There is only one corresponding term
                           // mapping -> change return type
                }

            }

        }
        return possibleOccuredTermMappings;

    }

    private boolean areNextWordsEqualToReferences(List<INounMapping> references, IWord currentWord, int currentPosition) {
        boolean stop = false;

        for (int i = currentPosition + 1; i < references.size() && !stop; i++) {
            String postWord = currentWord.getNextWord().getText();
            String reference = references.get(i).getReference();

            if (!SimilarityUtils.areWordsSimilar(reference, postWord)) {
                stop = true;
            }
        }

        return stop;
    }

    private boolean arePreviousWordsEqualToReferences(List<INounMapping> references, IWord currentWord, int currentPosition) {
        boolean stop = false;

        for (int i = currentPosition - 1; i >= 0 && !stop; i--) {
            String preWord = currentWord.getPreWord().getText();
            String reference = references.get(i).getReference();
            if (!SimilarityUtils.areWordsSimilar(reference, preWord)) {
                stop = true;
            }
        }

        return stop;
    }

    private void createRecommendedInstancesForAdjacentTermMappings(IWord termStartNode, ITermMapping term, List<ITermMapping> adjacentTermMappings) {

        List<ITermMapping> adjCompleteTermMappings = new ArrayList<>(getCompletePreAdjTermMappings(adjacentTermMappings, termStartNode));
        adjCompleteTermMappings.addAll(getCompleteAfterAdjTermMappings(adjacentTermMappings, termStartNode, term));

        createRecommendedInstancesOfAdjacentTerms(term, adjCompleteTermMappings);
    }

    private List<ITermMapping> getCompletePreAdjTermMappings(List<ITermMapping> possibleTermMappings, IWord termStartNode) {
        int sentence = termStartNode.getSentenceNo();
        IWord preTermNode = termStartNode.getPreWord();

        List<ITermMapping> adjCompleteTermMappings = new ArrayList<>();

        for (ITermMapping adjTerm : possibleTermMappings) {

            List<INounMapping> nounMappings = new ArrayList<>(adjTerm.getMappings());

            while (!nounMappings.isEmpty()) {
                INounMapping resultOfPreMatch = matchNode(nounMappings, preTermNode);

                if (sentence == preTermNode.getSentenceNo() || resultOfPreMatch == null) {
                    break;
                }

                nounMappings.remove(resultOfPreMatch);
                preTermNode = preTermNode.getPreWord();

            }
            if (nounMappings.isEmpty()) {
                adjCompleteTermMappings.add(adjTerm);
            }
        }

        return adjCompleteTermMappings;
    }

    private List<ITermMapping> getCompleteAfterAdjTermMappings(List<ITermMapping> possibleTermMappings, IWord termStartNode, ITermMapping term) {

        int sentence = termStartNode.getSentenceNo();
        IWord afterTermNode = getAfterTermNode(termStartNode, term);

        List<ITermMapping> adjCompleteTermMappings = new ArrayList<>();

        for (ITermMapping adjTerm : possibleTermMappings) {

            List<INounMapping> nounMappings = new ArrayList<>(adjTerm.getMappings());

            while (!nounMappings.isEmpty()) {
                INounMapping resultOfPostMatch = matchNode(nounMappings, afterTermNode);

                if (sentence == afterTermNode.getSentenceNo() || resultOfPostMatch == null) {
                    break;
                }
                nounMappings.remove(resultOfPostMatch);
                afterTermNode = afterTermNode.getNextWord();

            }
            if (nounMappings.isEmpty()) {
                adjCompleteTermMappings.add(adjTerm);
            }

        }

        return adjCompleteTermMappings;

    }

    private INounMapping matchNode(List<INounMapping> nounMappings, IWord node) {
        for (INounMapping mapping : nounMappings) {
            if (mapping.getNodes().contains(node)) {
                return mapping;
            }
        }
        return null;
    }

    private IWord getAfterTermNode(IWord termStartNode, ITermMapping term) {
        IWord afterTermNode = termStartNode.getNextWord();
        for (int i = 0; i < term.getMappings().size(); i++) { afterTermNode = afterTermNode.getNextWord(); }
        return afterTermNode;
    }

    private List<INounMapping> getTermAdjacentNounMappings(IWord node, ITermMapping term) {

        MappingKind kind = term.getKind();
        IWord preTermNode = node.getPreWord();
        IWord afterTermNode = getAfterTermNode(node, term);
        int sentence = node.getSentenceNo();

        List<INounMapping> possibleMappings = new ArrayList<>();

        if (sentence == preTermNode.getSentenceNo()) {

            List<INounMapping> nounMappingsOfPreTermNode = textState.getNounMappingsByNode(preTermNode);
            possibleMappings.addAll(nounMappingsOfPreTermNode);
        }
        if (sentence == afterTermNode.getSentenceNo()) {
            List<INounMapping> nounMappingsOfAfterTermNode = textState.getNounMappingsByNode(afterTermNode);
            possibleMappings.addAll(nounMappingsOfAfterTermNode);
        }
        possibleMappings = possibleMappings.stream().filter(nounMapping -> !nounMapping.getKind().equals(kind)).collect(Collectors.toList());

        return possibleMappings;
    }

    private void createRecommendedInstancesOfAdjacentTerms(ITermMapping term, List<ITermMapping> adjacentTerms) {
        MappingKind kind = term.getKind();

        if (kind.equals(MappingKind.NAME) && !adjacentTerms.isEmpty()) {
            for (ITermMapping adjTerm : adjacentTerms) {
                recommendationState.addRecommendedInstance(term.getReference(), adjTerm.getReference(), probabilityAdjacentTerm, term.getMappings(),
                        adjTerm.getMappings());

            }
        } else if (kind.equals(MappingKind.TYPE) && !adjacentTerms.isEmpty()) {
            for (ITermMapping adjTerm : adjacentTerms) {
                recommendationState.addRecommendedInstance(adjTerm.getReference(), term.getReference(), probabilityAdjacentTerm, adjTerm.getMappings(),
                        term.getMappings());
            }

        }
    }

    private void createRecommendedInstancesForSurroundingNounMappings(ITermMapping term, List<INounMapping> surroundingNounMappings) {
        MappingKind kind = term.getKind();

        if (kind.equals(MappingKind.NAME) && !surroundingNounMappings.isEmpty()) {
            for (INounMapping nounMapping : surroundingNounMappings) {

                recommendationState.addRecommendedInstance(term.getReference(), nounMapping.getReference(), probabilityJustAdjacentNoun, term.getMappings(),
                        List.of(nounMapping));
            }
        } else if (kind.equals(MappingKind.TYPE) && !surroundingNounMappings.isEmpty()) {
            for (INounMapping nounMapping : surroundingNounMappings) {

                recommendationState.addRecommendedInstance(nounMapping.getReference(), term.getReference(), probabilityJustAdjacentNoun, List.of(nounMapping),
                        term.getMappings());
            }
        }

    }

}
