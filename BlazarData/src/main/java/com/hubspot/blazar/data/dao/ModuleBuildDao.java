package com.hubspot.blazar.data.dao;

import com.hubspot.blazar.base.ModuleBuild;
import com.hubspot.blazar.data.util.BuildNumbers;
import com.hubspot.rosetta.jdbi.BindWithRosetta;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

import java.util.Set;

public interface ModuleBuildDao {

  @SqlQuery("" +
      "SELECT pendingBuild.id AS pendingBuildId, " +
      "pendingBuild.buildNumber AS pendingBuildNumber, " +
      "inProgressBuild.id AS inProgressBuildId, " +
      "inProgressBuild.buildNumber AS inProgressBuildNumber, " +
      "lastBuild.id AS lastBuildId, " +
      "lastBuild.buildNumber AS lastBuildNumber " +
      "FROM modules_v2 m " +
      "LEFT OUTER JOIN module_builds_v2 AS pendingBuild ON (m.pendingBuildId = pendingBuild.id) " +
      "LEFT OUTER JOIN module_builds_v2 AS inProgressBuild ON (m.inProgressBuildId = inProgressBuild.id) " +
      "LEFT OUTER JOIN module_builds_v2 AS lastBuild ON (m.lastBuildId = lastBuild.id) " +
      "WHERE m.id = :moduleId")
  BuildNumbers getBuildNumbers(@Bind("moduleId") int moduleId);

  @SqlQuery("SELECT * FROM module_builds_v2 WHERE repoBuildId = :repoBuildId")
  Set<ModuleBuild> getByRepositoryBuild(@Bind("repoBuildId") long repoBuildId);

  @GetGeneratedKeys
  @SqlUpdate("INSERT INTO module_builds_v2 (repoBuildId, moduleId, buildNumber, state) VALUES (:repoBuildId, :moduleId, :buildNumber, :state)")
  long enqueue(@BindWithRosetta ModuleBuild build);

  @SqlUpdate("UPDATE module_builds_v2 SET startTimestamp = :startTimestamp, state = :state, buildConfig = :buildConfig, resolvedConfig = :resolvedConfig WHERE id = :id AND state = 'QUEUED'")
  int begin(@BindWithRosetta ModuleBuild build);
}
