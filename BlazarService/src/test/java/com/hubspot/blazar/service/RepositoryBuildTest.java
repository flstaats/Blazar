package com.hubspot.blazar.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.blazar.BlazarServiceTestBase;
import com.hubspot.blazar.BlazarServiceTestModule;
import com.hubspot.blazar.base.GitInfo;
import com.hubspot.blazar.base.RepositoryBuild;
import com.hubspot.blazar.data.service.BranchService;
import com.hubspot.blazar.listener.BuildEventDispatcher;

import io.dropwizard.db.ManagedDataSource;

@RunWith(JukitoRunner.class)
@UseModules({BlazarServiceTestModule.class})
public class RepositoryBuildTest extends BlazarServiceTestBase {


  @Inject
  private BuildEventDispatcher buildEventDispatcher;
  @Inject
  private BranchService branchService;
  @Inject
  private TestUtils testUtils;
  @Inject
  private ManagedDataSource dataSource;

  @Before
  public void before() throws Exception {
    // set up the data for these inter-project build tests
    // todo migrate inter project data to a more common place
    runSql(dataSource, "schema.sql");
    runSql(dataSource, "InterProjectData.sql");
  }

  @Test
  public void testRepositoryBuild() throws InterruptedException {
    GitInfo gitInfo = branchService.get(3).get();
    RepositoryBuild build = testUtils.runDefaultRepositoryBuild(gitInfo);
    assertThat(build.getBranchId()).isEqualTo(3);
    assertThat(build.getState()).isEqualTo(RepositoryBuild.State.SUCCEEDED);
    assertThat(build.getDependencyGraph().get().getTopologicalSort()).isEqualTo(Lists.newArrayList(7,8,9));
  }
}
