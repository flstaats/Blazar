package com.hubspot.blazar.resources;

import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.hubspot.blazar.base.BuildOptions;
import com.hubspot.blazar.base.BuildTrigger;
import com.hubspot.blazar.base.GitInfo;
import com.hubspot.blazar.base.ModuleBuild;
import com.hubspot.blazar.base.RepositoryBuild;
import com.hubspot.blazar.data.service.BranchService;
import com.hubspot.blazar.data.service.ModuleBuildService;
import com.hubspot.blazar.data.service.RepositoryBuildService;
import com.hubspot.blazar.data.service.StateService;

@Path("/branches/builds")
@Produces(MediaType.APPLICATION_JSON)
public class BranchBuildResource {
  private final RepositoryBuildService repositoryBuildService;
  private final ModuleBuildService moduleBuildService;
  private StateService stateService;
  private final BranchService branchService;

  @Inject
  public BranchBuildResource(RepositoryBuildService repositoryBuildService,
                             ModuleBuildService moduleBuildService,
                             StateService stateService,
                             BranchService branchService) {
    this.repositoryBuildService = repositoryBuildService;
    this.moduleBuildService = moduleBuildService;
    this.stateService = stateService;
    this.branchService = branchService;
  }

  @POST
  @Path("/branch/{id}")
  @Consumes(MediaType.APPLICATION_JSON)
  public RepositoryBuild triggerWithOptions(@PathParam("id") int branchId, BuildOptions buildOptions, @QueryParam("username") Optional<String> username) {
    Optional<GitInfo> gitInfo = branchService.get(branchId);
    if (!gitInfo.isPresent()) {
      throw new NotFoundException("No branch found with id: " + branchId);
    }
    BuildTrigger buildTrigger = BuildTrigger.forUser(username.or("unknown"));
    long repositoryBuildId = repositoryBuildService.enqueue(gitInfo.get(), buildTrigger, buildOptions);
    return repositoryBuildService.get(repositoryBuildId).get();
  }

  @GET
  @Path("/{id}")
  public Optional<RepositoryBuild> get(@PathParam("id") long repositoryBuildId) {
    return repositoryBuildService.get(repositoryBuildId);
  }

  @GET
  @Path("/{id}/modules")
  public Set<ModuleBuild> getModuleBuilds(@PathParam("id") long repositoryBuildId) {
    return moduleBuildService.getByRepositoryBuild(repositoryBuildId);
  }

  @POST
  @Path("/{id}/cancel")
  public void cancel(@PathParam("id") long repositoryBuildId) {
    Optional<RepositoryBuild> build = get(repositoryBuildId);
    if (!build.isPresent()) {
      throw new NotFoundException("No build found for id: " + repositoryBuildId);
    }

    repositoryBuildService.cancel(build.get());
  }
}
