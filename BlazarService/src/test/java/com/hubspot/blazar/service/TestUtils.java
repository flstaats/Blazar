package com.hubspot.blazar.service;

import static org.assertj.core.api.Fail.fail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.hubspot.blazar.base.BuildOptions;
import com.hubspot.blazar.base.BuildTrigger;
import com.hubspot.blazar.base.GitInfo;
import com.hubspot.blazar.base.InterProjectBuild;
import com.hubspot.blazar.base.RepositoryBuild;
import com.hubspot.blazar.data.service.InterProjectBuildService;
import com.hubspot.blazar.data.service.RepositoryBuildService;

public class TestUtils {
  private final InterProjectBuildService interProjectBuildService;
  private final RepositoryBuildService repositoryBuildService;

  public static final Logger LOG = LoggerFactory.getLogger(TestUtils.class);

  @Inject
  public TestUtils (InterProjectBuildService interProjectBuildService,
                    RepositoryBuildService repositoryBuildService) {
    this.interProjectBuildService = interProjectBuildService;
    this.repositoryBuildService = repositoryBuildService;
  }




  public InterProjectBuild runInterProjectBuild(int rootModuleId, Optional<BuildTrigger> triggerOptional) throws InterruptedException {
    BuildTrigger trigger;
    if (triggerOptional.isPresent()) {
      trigger = triggerOptional.get();
    } else {
      trigger = new BuildTrigger(BuildTrigger.Type.MANUAL, String.format("Test inter-project build root: %d", rootModuleId));
    }
    LOG.info("Starting inter-project-build for id {}", rootModuleId);
    InterProjectBuild build = InterProjectBuild.getQueuedBuild(Sets.newHashSet(rootModuleId), trigger);
    long id = interProjectBuildService.enqueue(build);
    return waitForInterProjectBuild(interProjectBuildService.getWithId(id).get());
  }

  public RepositoryBuild runDefaultRepositoryBuild(GitInfo gitInfo) throws InterruptedException {
    long id = repositoryBuildService.enqueue(gitInfo, BuildTrigger.forCommit("1111111111111111111111111111111111111111"), BuildOptions.defaultOptions());
    RepositoryBuild build = repositoryBuildService.get(id).get();
    return waitForRepositoryBuild(build);
  }

  public RepositoryBuild runAndWaitForRepositoryBuild(GitInfo gitInfo, BuildTrigger buildTrigger, BuildOptions buildOptions) throws InterruptedException {
    long id = repositoryBuildService.enqueue(gitInfo, buildTrigger, buildOptions);
    RepositoryBuild build = repositoryBuildService.get(id).get();
    return waitForRepositoryBuild(build);
  }

  public InterProjectBuild waitForInterProjectBuild(InterProjectBuild build) throws InterruptedException {
    int count = 0;
    while (!build.getState().isComplete()) {
      if (count > 10) {
        fail(String.format("Build %s took more than 10s to complete", build));
      }
      count++;
      Thread.sleep(1000);
      build = interProjectBuildService.getWithId(build.getId().get()).get();
    }
    return build;
  }

  public RepositoryBuild waitForRepositoryBuild(RepositoryBuild build) throws InterruptedException {
    int count = 0;
    while (!build.getState().isComplete()) {
      if (count > 10) {
        fail(String.format("Build %s took more than 10s to complete", build));
      }
      count++;
      Thread.sleep(1000);
      build = repositoryBuildService.get(build.getId().get()).get();
    }
    return build;
  }
}
