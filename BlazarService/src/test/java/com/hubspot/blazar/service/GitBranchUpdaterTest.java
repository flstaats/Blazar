package com.hubspot.blazar.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.BlazarGHRepository;
import org.kohsuke.github.BlazarGHUser;
import org.kohsuke.github.GHRepository;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.blazar.BlazarServiceTestBase;
import com.hubspot.blazar.BlazarServiceTestModule;
import com.hubspot.blazar.base.GitInfo;
import com.hubspot.blazar.command.GitBranchUpdater;
import com.hubspot.blazar.config.BlazarConfiguration;
import com.hubspot.blazar.data.service.BranchService;
import com.hubspot.blazar.util.GitHubHelper;

import io.dropwizard.db.ManagedDataSource;


@RunWith(JukitoRunner.class)
@UseModules({BlazarServiceTestModule.class})
public class GitBranchUpdaterTest extends BlazarServiceTestBase {
  @Inject
  private BranchService branchService;
  @Inject
  private BlazarConfiguration blazarConfiguration;
  @Inject
  private GitHubHelper gitHubHelper;

  @Before
  public void before(ManagedDataSource dataSource) throws Exception {
    runSql(dataSource, "InterProjectData.sql");
  }

  @Test
  public void itDoesNotChangeUnchangedBranch() {
    GitInfo unchangedBranch = branchService.get(1).get();
    buildUpdater(unchangedBranch).run();
    GitInfo maybeChangedBranch = branchService.get(1).get();
    assertThat(maybeChangedBranch).isEqualTo(unchangedBranch);
  }

  @Test
  public void itDeletesBranchWithDuplicateRepoId() {
    GitInfo unchangedBranch = branchService.get(1).get();
    GitInfo branchWithNewGitHubId = new GitInfo(unchangedBranch.getId(),
        unchangedBranch.getHost(),
        unchangedBranch.getOrganization(),
        unchangedBranch.getRepository(),
        1337,
        unchangedBranch.getBranch(),
        unchangedBranch.isActive(),
        unchangedBranch.getCreatedTimestamp(),
        unchangedBranch.getUpdatedTimestamp());

    // This creates a new branch
    branchWithNewGitHubId = branchService.upsert(branchWithNewGitHubId);

    // Verify that the name information is the same, repoId is different and it is active
    assertThat(branchWithNewGitHubId.getHost()).isEqualTo(unchangedBranch.getHost());
    assertThat(branchWithNewGitHubId.getOrganization()).isEqualTo(unchangedBranch.getOrganization());
    assertThat(branchWithNewGitHubId.getRepository()).isEqualTo(unchangedBranch.getRepository());
    assertThat(branchWithNewGitHubId.getRepositoryId()).isNotEqualTo(unchangedBranch.getRepositoryId());
    assertThat(branchWithNewGitHubId.isActive()).isTrue();

    buildUpdater(branchWithNewGitHubId).run();

    Optional<GitInfo> maybeDeletedBranch = branchService.get(branchWithNewGitHubId.getId().get());
    // Check the branch is now inactive but present
    assertThat(maybeDeletedBranch.isPresent()).isTrue();
    assertThat(maybeDeletedBranch.get().isActive()).isFalse();


  }

  @Test
  public void itUpdatesAndSetsABranchToInactiveIfItWasMovedToAnUnManagedOrganization() {
    GitInfo branchWithoutConfiguredHost = buildGitInfo("git.bar.example.com/org/repo#branch");
    branchWithoutConfiguredHost = branchService.upsert(branchWithoutConfiguredHost);
    assertThat(branchWithoutConfiguredHost.isActive()).isTrue();

    buildUpdater(branchWithoutConfiguredHost).run();

    GitInfo maybeChangedBranch = branchService.get(branchWithoutConfiguredHost.getId().get()).get();
    assertThat(maybeChangedBranch.isActive()).isFalse();
  }

  @Test
  public void itArchivesABranchThatMovedOrganizations() throws IOException {
    GitInfo movedBranch = branchService.get(1).get();
    GitInfo unChangedBranch = branchService.get(2).get();

    // Alter the owner of the repo inside our fake GitHub
    BlazarGHRepository unModifiedRepo = (BlazarGHRepository) gitHubHelper.repositoryFor(movedBranch);
    BlazarGHUser oldOwner = unModifiedRepo.getOwner();
    try {
      unModifiedRepo.setOwner(new BlazarGHUser("new-user", "new-user-login", "new.user@example.com"));
      // Verify that we changed the fake github
      GHRepository repository = gitHubHelper.repositoryFor(movedBranch);
      assertThat(repository.getOwnerName()).isEqualTo("new-user-login");

      buildUpdater(movedBranch).run();
      buildUpdater(unChangedBranch).run();
      GitInfo maybeUpdatedBranch = branchService.get(movedBranch.getId().get()).get();
      GitInfo maybeUnChangedBranch = branchService.get(2).get();
      assertThat(maybeUpdatedBranch.isActive()).isFalse();
      assertThat(maybeUpdatedBranch.getOrganization()).isEqualTo("new-user-login");
      assertThat(maybeUnChangedBranch.isActive()).isTrue();
      assertThat(maybeUnChangedBranch.getOrganization()).isEqualTo(maybeUnChangedBranch.getOrganization());
    } finally {
      // return owner to its old state.
      unModifiedRepo.setOwner(oldOwner);
    }
  }

  private GitBranchUpdater buildUpdater(GitInfo gitInfo) {
    return new GitBranchUpdater(gitInfo, gitHubHelper, blazarConfiguration, branchService);
  }

  private GitInfo buildGitInfo(String uri) {
    GitInfo gitInfo = GitInfo.fromString(uri);
    return new GitInfo(Optional.absent(),
        gitInfo.getHost(),
        gitInfo.getOrganization(),
        gitInfo.getRepository(),
        ThreadLocalRandom.current().nextInt(1337, 100000),
        gitInfo.getBranch(),
        true,
        System.currentTimeMillis(),
        System.currentTimeMillis());
  }
}
