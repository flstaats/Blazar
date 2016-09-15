package com.hubspot.blazar.base;

import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.hubspot.rosetta.annotations.StoredAsJson;

public class InterProjectBuild {
  private final Optional<Long> id;
  private final State state;
  @StoredAsJson
  private Set<Integer> moduleIds;
  @StoredAsJson
  private final BuildTrigger buildTrigger;
  private final Optional<Long> startTimestamp;
  private final Optional<Long> endTimestamp;
  @StoredAsJson
  private final Optional<DependencyGraph> dependencyGraph;

  public enum State {
    QUEUED(false),
    IN_PROGRESS(false),
    CANCELLED(true),
    FAILED(true),
    SUCCEEDED(true);

    private final boolean completed;

    State(boolean completed) {
      this.completed = completed;
    }

    public boolean isFinished() {
      return this.completed;
    }
  }

  @JsonCreator
  public InterProjectBuild(@JsonProperty("id") Optional<Long> id,
                           @JsonProperty("state") State state,
                           @JsonProperty("moduleIds") Set<Integer> moduleIds,
                           @JsonProperty("buildTrigger") BuildTrigger buildTrigger,
                           @JsonProperty("startTimestamp") Optional<Long> startTimestamp,
                           @JsonProperty("endTimestamp") Optional<Long> endTimestamp,
                           @JsonProperty("dependencyGraph") Optional<DependencyGraph> dependencyGraph) {
    this.id = id;
    this.state = state;
    moduleIds.stream().forEach(Preconditions::checkNotNull);
    this.moduleIds = moduleIds;
    this.buildTrigger = buildTrigger;
    this.startTimestamp = startTimestamp;
    this.endTimestamp = endTimestamp;
    this.dependencyGraph = dependencyGraph;
  }

  public static InterProjectBuild getQueuedBuild(Set<Integer> moduleIds, BuildTrigger buildTrigger) {
    return new InterProjectBuild(Optional.absent(), State.QUEUED, moduleIds, buildTrigger, Optional.of(System.currentTimeMillis()), Optional.absent(),Optional.absent());
  }

  public static InterProjectBuild getStarted(InterProjectBuild build) {
    return new InterProjectBuild(build.getId(), State.IN_PROGRESS, build.getModuleIds(), build.getBuildTrigger(), build.getStartTimestamp(), build.getEndTimestamp(), build.getDependencyGraph());
  }

  public static InterProjectBuild getFinishedBuild(InterProjectBuild old, State state) {
    return new InterProjectBuild(old.getId(), state, old.getModuleIds(), old.getBuildTrigger(), old.getStartTimestamp(), Optional.of(System.currentTimeMillis()), old.getDependencyGraph());
  }

  public InterProjectBuild withDependencyGraph(DependencyGraph d){
    return new InterProjectBuild(id, state, moduleIds, buildTrigger, startTimestamp, endTimestamp, Optional.of(d));
  }

  public InterProjectBuild withModuleIds(Set<Integer> ids) {
    return new InterProjectBuild(id, state, ids, buildTrigger, startTimestamp, endTimestamp, dependencyGraph);
  }


  public Optional<Long> getId() {
    return id;
  }

  public State getState() {
    return state;
  }

  public Set<Integer> getModuleIds() {
    return moduleIds;
  }

  public BuildTrigger getBuildTrigger() {
    return buildTrigger;
  }

  public Optional<Long> getStartTimestamp() {
    return startTimestamp;
  }

  public Optional<Long> getEndTimestamp() {
    return endTimestamp;
  }

  public Optional<DependencyGraph> getDependencyGraph() {
    return dependencyGraph;
  }

  @Override
  public String toString() {
    return com.google.common.base.Objects.toStringHelper(this)
        .add("id", id)
        .add("state", state)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    InterProjectBuild build = (InterProjectBuild) o;
    return Objects.equals(id, build.id) && Objects.equals(moduleIds, build.moduleIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, moduleIds);
  }
}
