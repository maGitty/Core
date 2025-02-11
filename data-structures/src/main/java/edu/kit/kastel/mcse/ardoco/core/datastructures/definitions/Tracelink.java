package edu.kit.kastel.mcse.ardoco.core.datastructures.definitions;

import java.util.Objects;

/**
 * Represents a tracelink. Is a convenience data class that takes the necessary info from {@link IInstanceLink} and the
 * specific {@link IModelInstance} and {@link IWord} that are used in this tracelink.
 *
 * @author Jan Keim
 *
 */
public class Tracelink {
    private IInstanceLink instanceLink;

    private IModelInstance modelInstance;
    private IWord word;

    /**
     * Create a tracelink based on a {@link IInstanceLink} and a concrete {@link IModelInstance} along with a concrete
     * {@link IWord}.
     *
     * @param instanceLink  InstanceLink of this tracelink
     * @param modelInstance modelInstance that the tracelink points to
     * @param word          word that the tracelink points to
     */
    public Tracelink(IInstanceLink instanceLink, IModelInstance modelInstance, IWord word) {
        this.instanceLink = instanceLink;
        this.modelInstance = modelInstance;
        this.word = word;
    }

    /**
     * Get the sentence number of the word that the tracelink is based on.
     *
     * @return sentence number of the word that the tracelink is based on.
     */
    public int getSentenceNumber() {
        return word.getSentenceNo();
    }

    /**
     * Get the UID of the model element that the tracelink is based on.
     *
     * @return Uid of the model element that the tracelink is based on.
     */
    public String getModelElementUid() {
        return modelInstance.getUid();
    }

    /**
     * Get the {@link IInstanceLink} that the tracelink is based on.
     *
     * @return {@link IInstanceLink} that the tracelink is based on.
     */
    public IInstanceLink getInstanceLink() {
        return instanceLink;
    }

    /**
     * Get the probability/confidence of this tracelink
     *
     * @return probability/confidence of this tracelink
     */
    public double getProbability() {
        return instanceLink.getProbability();
    }

    /**
     * See {@link Object#equals(Object)}. Uses the Uid of the model element and the sentence number of the word
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Tracelink other) {
            var otherId = other.getModelElementUid();
            var otherSentenceNo = other.getSentenceNumber();
            if (getModelElementUid().equals(otherId) && getSentenceNumber() == otherSentenceNo) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getModelElementUid(), getSentenceNumber());
    }

}
