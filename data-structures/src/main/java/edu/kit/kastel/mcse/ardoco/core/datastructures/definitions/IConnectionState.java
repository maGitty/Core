package edu.kit.kastel.mcse.ardoco.core.datastructures.definitions;

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;

import edu.kit.kastel.mcse.ardoco.core.datastructures.modules.IState;

/**
 * The Interface IConnectionState.
 */
public interface IConnectionState extends IState<IConnectionState> {

    /**
     * Returns all instance links.
     *
     * @return all instance links
     */
    ImmutableList<IInstanceLink> getInstanceLinks();

    /**
     * Returns all instance links with a model instance containing the given name.
     *
     * @param name the name of a model instance
     * @return all instance links with a model instance containing the given name as list
     */
    ImmutableList<IInstanceLink> getInstanceLinksByName(String name);

    /**
     * Returns all instance links with a model instance containing the given type.
     *
     * @param type the type of a model instance
     * @return all instance links with a model instance containing the given type as list
     */
    ImmutableList<IInstanceLink> getInstanceLinksByType(String type);

    /**
     * Returns all instance links with a model instance containing the given recommended instance.
     *
     * @param recommendedInstance the recommended instance to consider
     * @return all instance links found
     */
    ImmutableList<IInstanceLink> getInstanceLinksByRecommendedInstance(IRecommendedInstance recommendedInstance);

    /**
     * Returns all instance links with a model instance containing the given name and type.
     *
     * @param name the name of a model instance
     * @param type the type of a model instance
     * @return all instance links with a model instance containing the given name and type as list
     */
    ImmutableList<IInstanceLink> getInstanceLinks(String name, String type);

    /**
     * Returns a list of tracelinks that are contained within this connection state.
     *
     * @return list of tracelinks within this connection state
     */
    default ImmutableSet<Tracelink> getTraceLinks() {
        MutableSet<Tracelink> tracelinks = Sets.mutable.empty();
        for (var instanceLink : getInstanceLinks()) {
            var textualInstance = instanceLink.getTextualInstance();
            for (var nm : textualInstance.getNameMappings()) {
                for (var word : nm.getWords()) {
                    var tracelink = new Tracelink(instanceLink, instanceLink.getModelInstance(), word);
                    tracelinks.add(tracelink);
                }
                for (var word : nm.getCoreferences()) {
                    var tracelink = new Tracelink(instanceLink, instanceLink.getModelInstance(), word);
                    tracelinks.add(tracelink);
                }
            }
        }
        return tracelinks.toImmutable();
    }

    /**
     * Adds the connection of a recommended instance and a model instance to the state. If the model instance is already
     * contained by the state it is extended. Elsewhere a new instance link is created
     *
     * @param recommendedModelInstance the recommended instance
     * @param instance                 the model instance
     * @param probability              the probability of the link
     */
    void addToLinks(IRecommendedInstance recommendedModelInstance, IModelInstance instance, double probability);

    /**
     * Checks if an instance link is already contained by the state.
     *
     * @param instanceLink the given instance link
     * @return true if it is already contained
     */
    boolean isContainedByInstanceLinks(IInstanceLink instanceLink);

    /**
     * Removes an instance link from the state.
     *
     * @param instanceMapping the instance link to remove
     */
    void removeFromMappings(IInstanceLink instanceMapping);

    /**
     * Removes all instance links containing the given instance.
     *
     * @param instance the given instance
     */
    void removeAllInstanceLinksWith(IModelInstance instance);

    /**
     * Removes all instance links containing the given recommended instance.
     *
     * @param instance the given recommended instance
     */
    void removeAllInstanceLinksWith(IRecommendedInstance instance);

    /**
     * Adds an instance link to the state.
     *
     * @param instanceMapping the instance link to add
     */
    void addToLinks(IInstanceLink instanceMapping);

    /**
     * Returns all relation links.
     *
     * @return all relation links of this state as list
     */
    ImmutableList<IRelationLink> getRelationLinks();

    /**
     * Adds the connection of a recommended relation and a model relation to the state if it is not already contained.
     *
     * @param recommendedModelRelation the recommended relation
     * @param relation                 the model relation
     * @param probability              the probability of the link
     */
    void addToLinks(IRecommendedRelation recommendedModelRelation, IModelRelation relation, double probability);

    /**
     * Adds a relation link to the state.
     *
     * @param relationMapping the relation link to add
     */
    void addToLinks(IRelationLink relationMapping);

    /**
     * Checks if a relation link is already contained by the state.
     *
     * @param relationMapping the relation mapping
     * @return true, if the relation link is already contained. False if not.
     */
    boolean isContainedByRelationLinks(IRelationLink relationMapping);

    /**
     * Removes a given relation link from the state.
     *
     * @param relationMapping the given relation link
     */
    void removeFromMappings(IRelationLink relationMapping);

    /**
     * Removes all relation links with a given recommended relation.
     *
     * @param relation the recommended relation to search for
     */
    void removeAllMappingsWith(IRecommendedRelation relation);

    /**
     * Removes all relation links with a given model relation.
     *
     * @param relation the relation to search for
     */
    void removeAllMappingsWith(IModelRelation relation);

}
