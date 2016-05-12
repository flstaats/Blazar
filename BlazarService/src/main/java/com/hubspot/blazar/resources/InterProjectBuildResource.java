package com.hubspot.blazar.resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.hubspot.blazar.base.BuildOptions;
import com.hubspot.blazar.base.BuildTrigger;
import com.hubspot.blazar.base.D3GraphData;
import com.hubspot.blazar.base.D3GraphLink;
import com.hubspot.blazar.base.D3GraphNode;
import com.hubspot.blazar.base.DependencyGraph;
import com.hubspot.blazar.base.GitInfo;
import com.hubspot.blazar.base.InterProjectBuild;
import com.hubspot.blazar.base.InterProjectBuildMapping;
import com.hubspot.blazar.base.Module;
import com.hubspot.blazar.data.service.BranchService;
import com.hubspot.blazar.data.service.DependenciesService;
import com.hubspot.blazar.data.service.InterProjectBuildMappingService;
import com.hubspot.blazar.data.service.InterProjectBuildService;
import com.hubspot.blazar.data.service.ModuleService;
import com.sun.jersey.api.NotFoundException;

@Path("/inter-project-builds")
@Produces(MediaType.APPLICATION_JSON)
public class InterProjectBuildResource {
  private final DependenciesService dependenciesService;
  private InterProjectBuildService interProjectBuildService;
  private InterProjectBuildMappingService interProjectBuildMappingService;
  private BranchService branchService;
  private final ModuleService moduleService;

  @Inject
  public InterProjectBuildResource(DependenciesService dependenciesService,
                                   InterProjectBuildService interProjectBuildService,
                                   InterProjectBuildMappingService interProjectBuildMappingService,
                                   BranchService branchService,
                                   ModuleService moduleService) {
    this.dependenciesService = dependenciesService;
    this.interProjectBuildService = interProjectBuildService;
    this.interProjectBuildMappingService = interProjectBuildMappingService;
    this.branchService = branchService;
    this.moduleService = moduleService;
  }

  @POST
  @Path("/")
  public InterProjectBuild triggerWithOptions(BuildOptions buildOptions, @QueryParam("username") Optional<String> username) {
    InterProjectBuild build = InterProjectBuild.getQueuedBuild(buildOptions.getModuleIds(), BuildTrigger.forUser(username.or("unknown")));
    long id = interProjectBuildService.enqueue(build);
    return interProjectBuildService.getWithId(id).get();
  }

  @POST
  @Path("/cancel/{id}")
  public void cancel(@PathParam("id") long interProjectBuildId) {
    Optional<InterProjectBuild> build = interProjectBuildService.getWithId(interProjectBuildId);
    if (!build.isPresent()) {
      throw new NotFoundException("No build found for id: " + interProjectBuildId);
    }
    interProjectBuildService.cancel(build.get());
  }

  @GET
  @Path("/drawableGraph")
  public D3GraphData getDrawableGraph(@QueryParam("moduleId") Set<Integer> moduleIds) {
    Set<Module> modules = new HashSet<>();
    Map<Integer, InterProjectBuild.State> moduleIdToState = new HashMap<>();
    for (int i : moduleIds) {
      Optional<Module> m = moduleService.get(i);
      if (m.isPresent()) {
        modules.add(m.get());
      }
    }
    DependencyGraph graph = dependenciesService.buildInterProjectDependencyGraph(modules);
    for (int i : graph.getTopologicalSort()) {
      moduleIdToState.put(i, InterProjectBuild.State.QUEUED);
    }
    List<D3GraphNode> nodes = getNodes(graph, moduleIdToState);
    List<D3GraphLink> links = drawLinks(nodes, graph);
    return new D3GraphData(links, nodes);
  }

  @GET
  @Path("/drawableGraph/{id}")
  public D3GraphData getDrawableGraphForBuild(@PathParam("id") long interProjectBuildId) {
    InterProjectBuild build = interProjectBuildService.getWithId(interProjectBuildId).get();
    Set<InterProjectBuildMapping> mappings = interProjectBuildMappingService.getMappingsForInterProjectBuild(build);
    Map<Integer, InterProjectBuild.State> moduleIdToState = new HashMap<>();
    for (InterProjectBuildMapping mapping : mappings) {
      moduleIdToState.put(mapping.getModuleId(), mapping.getState());
    }
    for (int i : build.getDependencyGraph().get().getTopologicalSort()) {
      if (!moduleIdToState.containsKey(i)) {
        moduleIdToState.put(i, InterProjectBuild.State.QUEUED);
      }
    }
    List<D3GraphNode> nodes = getNodes(build.getDependencyGraph().get(), moduleIdToState);
    List<D3GraphLink> links = drawLinks(nodes, build.getDependencyGraph().get());
    return new D3GraphData(links, nodes);
  }

  private List<D3GraphNode> getNodes(DependencyGraph graph, Map<Integer, InterProjectBuild.State> moduleIdToState) {
    Set<Module> modules = new HashSet<>();
    for (int i: graph.getTopologicalSort()) {
      Optional<Module> m = moduleService.get(i);
      if (m.isPresent()) {
        modules.add(m.get());
      }
    }
    List<D3GraphNode> nodes = new ArrayList<>();
    for (Module module : modules) {
      GitInfo gitInfo = branchService.get(moduleService.getBranchIdFromModuleId(module.getId().get())).get();
      String source = String.format("%s-%s", gitInfo.getRepository(), module.getName());
      D3GraphNode node = new D3GraphNode(source, module.getId().get(), 100, 100, moduleIdToState.get(module.getId().get()));
      nodes.add(node);
    }
    return nodes;
  }

  private List<D3GraphLink> drawLinks(List<D3GraphNode> nodes, DependencyGraph graph) {
    List<D3GraphLink> links = new ArrayList<>();
    int pos = 0;
    for (D3GraphNode node : nodes) {
      Set<Integer> outgoingModules = graph.outgoingVertices(node.getModuleId());
      for (int module : outgoingModules) {
        links.add(new D3GraphLink(getPos(nodes, module), pos));
      }
      pos++;
    }
    return links;
  }

  private int getPos(List<D3GraphNode> nodes, int moduleId) {
    int pos = 0;
    for (D3GraphNode node : nodes) {
      if (node.getModuleId() == moduleId) {
        return pos;
      }
      pos++;
    }
    throw new IllegalStateException(String.format("No node with moduleId %d", moduleId));
  }
}