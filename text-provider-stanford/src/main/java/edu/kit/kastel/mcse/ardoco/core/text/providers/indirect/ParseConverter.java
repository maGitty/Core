package edu.kit.kastel.mcse.ardoco.core.text.providers.indirect;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;

import edu.kit.ipd.parse.luna.graph.IArc;
import edu.kit.ipd.parse.luna.graph.IGraph;
import edu.kit.ipd.parse.luna.graph.INode;
import edu.kit.kastel.mcse.ardoco.core.datastructures.definitions.DependencyTag;
import edu.kit.kastel.mcse.ardoco.core.datastructures.definitions.ICorefCluster;
import edu.kit.kastel.mcse.ardoco.core.datastructures.definitions.IText;
import edu.kit.kastel.mcse.ardoco.core.datastructures.definitions.IWord;
import edu.kit.kastel.mcse.ardoco.core.datastructures.definitions.POSTag;

/**
 * The Class ParseConverter converts an {@link IGraph} to an {@link IText}.
 */
public class ParseConverter {

    private IText annotatedText;
    private IGraph graph;

    private Map<INode, Word> instances;
    private MutableList<Word> orderedWords;

    /**
     * Instantiates a new parses the converter.
     *
     * @param graph the graph
     */
    public ParseConverter(IGraph graph) {
        this.graph = graph;
    }

    /**
     * Converts the graph.
     */
    public void convert() {
        reset();

        createWords();
        createDeps();

        var corefClusters = getCorefClusters();

        annotatedText = new Text(orderedWords.toImmutable(), corefClusters);
    }

    /**
     * Gets the annotated text.
     *
     * @return the annotated text
     */
    public IText getAnnotatedText() {
        return annotatedText;
    }

    private void createWords() {
        instances = new HashMap<>();
        orderedWords = Lists.mutable.empty();

        ImmutableList<INode> tokens = Lists.immutable.withAll(graph.getNodesOfType(graph.getNodeType("token")));

        for (INode token : tokens) {
            var word = new Word(token);
            orderedWords.add(word);
            instances.put(token, word);
        }
    }

    private void createDeps() {
        // TODO: To Complete

        Map<String, DependencyTag> dependencyMap = Arrays.stream(DependencyTag.values())
                .collect(Collectors.toMap(d -> String.valueOf(d).toLowerCase(), d -> d));

        for (INode node : graph.getNodesOfType(graph.getNodeType("token"))) {
            var sourceWord = instances.get(node);
            for (IArc arc : node.getOutgoingArcsOfType(graph.getArcType("typedDependency"))) {
                var targetWord = instances.get(arc.getTargetNode());

                var arcAttributeValue = String.valueOf(arc.getAttributeValue("relationShort"));
                if (dependencyMap.containsKey(arcAttributeValue)) {
                    DependencyTag depTag = dependencyMap.get(arcAttributeValue);
                    sourceWord.wordsThatAreDependenciesOfThis.get(depTag).add(targetWord);
                    targetWord.wordsThatAreDependentOnThis.get(depTag).add(sourceWord);
                }
            }
        }
    }

    private ImmutableList<ICorefCluster> getCorefClusters() {
        MutableList<ICorefCluster> clusters = Lists.mutable.empty();

        for (var corefClusterNode : graph.getNodesOfType(graph.getNodeType("CorefCluster"))) {
            MutableList<ImmutableList<IWord>> mentions = Lists.mutable.empty();
            var id = (int) corefClusterNode.getAttributeValue("clusterId");
            var representativeMention = (String) corefClusterNode.getAttributeValue("representativeMention");

            MutableList<IWord> currMention = Lists.mutable.empty();
            var lastPosition = -1;
            for (var arc : corefClusterNode.getIncomingArcsOfType(graph.getArcType("coreference"))) {
                var wordNode = arc.getSourceNode();
                var word = instances.get(wordNode);
                var wordPosition = word.getPosition();
                if (lastPosition != -1 && lastPosition + 1 != wordPosition) {
                    mentions.add(currMention.toImmutable());
                    currMention = Lists.mutable.empty();
                }
                lastPosition = wordPosition;
                currMention.add(word);
            }

            var corefCluster = new CorefCluster(id, representativeMention, mentions.toImmutable());
            clusters.add(corefCluster);
        }

        return clusters.toImmutable();
    }

    private void reset() {
        annotatedText = null;
        instances = null;
        orderedWords = null;
    }

    private static final class Word implements IWord {
        private final int sentence;
        private final int position;
        private final String text;
        private final POSTag posTag;
        private final String lemma;

        private final Map<DependencyTag, MutableList<IWord>> wordsThatAreDependenciesOfThis = Arrays.stream(DependencyTag.values())
                .collect(Collectors.toMap(t -> t, v -> Lists.mutable.empty()));
        private final Map<DependencyTag, MutableList<IWord>> wordsThatAreDependentOnThis = Arrays.stream(DependencyTag.values())
                .collect(Collectors.toMap(t -> t, v -> Lists.mutable.empty()));

        private IText parent;

        Word(INode node) {
            text = String.valueOf(node.getAttributeValue("value"));
            position = Integer.valueOf(String.valueOf(node.getAttributeValue("position")));
            lemma = String.valueOf(node.getAttributeValue("lemma"));
            posTag = getPosTag(node);
            sentence = Integer.valueOf(String.valueOf(node.getAttributeValue("sentenceNumber")));
        }

        private static POSTag getPosTag(INode node) {
            var posTagValue = String.valueOf(node.getAttributeValue("pos"));
            return POSTag.get(posTagValue);
        }

        @Override
        public int getSentenceNo() {
            return sentence;
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public POSTag getPosTag() {
            return posTag;
        }

        @Override
        public int getPosition() {
            return position;
        }

        @Override
        public String getLemma() {
            return lemma;
        }

        @Override
        public ImmutableList<IWord> getWordsThatAreDependencyOfThis(DependencyTag dependencyTag) {
            return wordsThatAreDependenciesOfThis.get(dependencyTag).toImmutable();
        }

        @Override
        public ImmutableList<IWord> getWordsThatAreDependentOnThis(DependencyTag dependencyTag) {
            return wordsThatAreDependentOnThis.get(dependencyTag).toImmutable();
        }

        @Override
        public IWord getPreWord() {
            if (position <= 0) {
                return null;
            }
            return parent.getWords().get(position - 1);
        }

        @Override
        public IWord getNextWord() {
            if (position >= parent.getLength() - 1) {
                return null;
            }
            return parent.getWords().get(position + 1);
        }

        @Override
        public int hashCode() {
            return Objects.hash(position, sentence, text);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Word other = (Word) obj;
            return position == other.position && sentence == other.sentence && Objects.equals(text, other.text);
        }
    }

    private static final class Text implements IText {

        private ImmutableList<IWord> words;
        private ImmutableList<ICorefCluster> corefClusters;

        private Text(ImmutableList<Word> orderedWords, ImmutableList<ICorefCluster> corefClusters) {
            orderedWords.stream().forEach(w -> w.parent = this);
            words = orderedWords.collect(w -> w);
            this.corefClusters = corefClusters;
        }

        @Override
        public IWord getFirstWord() {
            return words.isEmpty() ? null : words.get(0);
        }

        @Override
        public ImmutableList<IWord> getWords() {
            return words;
        }

        @Override
        public ImmutableList<ICorefCluster> getCorefClusters() {
            return corefClusters;
        }

    }

    private static final class CorefCluster implements ICorefCluster {
        private int id;
        private ImmutableList<ImmutableList<IWord>> mentions;
        private String representativeMention;

        private CorefCluster(int id, String representativeMention, ImmutableList<ImmutableList<IWord>> mentions) {
            this.mentions = mentions;
            this.representativeMention = representativeMention;
            this.id = id;
        }

        @Override
        public String getRepresentativeMention() {
            return representativeMention;
        }

        @Override
        public ImmutableList<ImmutableList<IWord>> getMentions() {
            return mentions;
        }

        @Override
        public int getId() {
            return id;
        }

    }
}
