package com.hubspot.blazar.resources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.hubspot.blazar.base.LogChunk;
import com.hubspot.blazar.base.ModuleBuild;
import com.hubspot.blazar.base.ModuleBuild.State;
import com.hubspot.blazar.data.service.ModuleBuildService;
import com.hubspot.blazar.externalservice.badbuilds.BadBuildClient;
import com.hubspot.horizon.AsyncHttpClient;
import com.hubspot.horizon.HttpRequest;
import com.hubspot.horizon.HttpResponse;
import com.hubspot.mesos.json.MesosFileChunkObject;
import com.hubspot.singularity.SingularityS3Log;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskHistoryUpdate.SimplifiedTaskState;
import com.hubspot.singularity.client.SingularityClient;

@Path("/modules/builds")
@Produces(MediaType.APPLICATION_JSON)
public class ModuleBuildResource {
  private static final Logger LOG = LoggerFactory.getLogger(ModuleBuildResource.class);

  private final ModuleBuildService moduleBuildService;
  private final SingularityClient singularityClient;
  private BadBuildClient badBuildClient;
  private final AsyncHttpClient asyncHttpClient;

  @Inject
  public ModuleBuildResource(ModuleBuildService moduleBuildService,
                             SingularityClient singularityClient,
                             BadBuildClient badBuildClient,
                             AsyncHttpClient asyncHttpClient) {
    this.moduleBuildService = moduleBuildService;
    this.singularityClient = singularityClient;
    this.badBuildClient = badBuildClient;
    this.asyncHttpClient = asyncHttpClient;
  }

  @GET
  @Path("/{buildId}")
  public Optional<ModuleBuild> get(@PathParam("buildId") long moduleBuildId) {
    return moduleBuildService.get(moduleBuildId);
  }

  @PUT
  @Path("/{buildId}/start")
  public ModuleBuild start(@PathParam("buildId") long moduleBuildId, @QueryParam("taskId") String taskId) {
    ModuleBuild build = moduleBuildService.getBuildWithExpectedState(moduleBuildId, State.LAUNCHING);

    if (taskId.isEmpty()) {
      throw new IllegalArgumentException("Task ID is required");
    }

    ModuleBuild inProgress = build.toBuilder().setState(State.IN_PROGRESS).setTaskId(Optional.of(taskId)).build();
    moduleBuildService.update(inProgress);
    return inProgress;
  }

  @PUT
  @Path("/{moduleBuildId}/success")
  public ModuleBuild completeSuccess(@PathParam("moduleBuildId") long moduleBuildId) {
    ModuleBuild build = moduleBuildService.getBuildWithExpectedState(moduleBuildId, State.IN_PROGRESS);

    ModuleBuild succeeded = build.toBuilder().setState(State.SUCCEEDED).setEndTimestamp(Optional.of(System.currentTimeMillis())).build();
    moduleBuildService.update(succeeded);
    return succeeded;
  }

  @PUT
  @Path("/{moduleBuildId}/failure")
  public ModuleBuild completeFailure(@PathParam("moduleBuildId") long moduleBuildId) {
    ModuleBuild build = moduleBuildService.getBuildWithExpectedState(moduleBuildId, State.IN_PROGRESS);

    ModuleBuild failed = build.toBuilder().setState(State.FAILED).setEndTimestamp(Optional.of(System.currentTimeMillis())).build();
    moduleBuildService.update(failed);
    return failed;
  }

  @GET
  @Path("/{moduleBuildId}/log")
  public LogChunk getLog(@PathParam("moduleBuildId") long moduleBuildId,
                         @QueryParam("offset") @DefaultValue("0") long offset,
                         @QueryParam("length") @DefaultValue("65536") long length) throws Exception {
    ModuleBuild build = moduleBuildService.getWithId(moduleBuildId);

    Optional<String> taskId = build.getTaskId();
    if (!taskId.isPresent()) {
      throw new NotFoundException("No taskId found for build: " + moduleBuildId);
    }

    String path = taskId.get() + "/service.log";
    Optional<String> grep = Optional.absent();

    Optional<MesosFileChunkObject> chunk = singularityClient.readSandBoxFile(taskId.get(), path, grep, Optional.of(offset), Optional.of(length));
    if (chunk.isPresent()) {
      if (chunk.get().getData().isEmpty() && logCompleted(build)) {
        return new LogChunk(chunk.get().getData(), chunk.get().getOffset(), -1);
      } else {
        return new LogChunk(chunk.get().getData(), chunk.get().getOffset());
      }
    } else {
      SingularityS3Log s3Log = findS3ServiceLog(taskId.get());
      if (offset >= s3Log.getSize()) {
        return new LogChunk("", s3Log.getSize(), -1);
      }

      return readS3LogChunk(s3Log.getGetUrl(), offset, length);
    }
  }

  @GET
  @Path("/{moduleBuildId}/log/size")
  public Object getLogSize(@PathParam("moduleBuildId") long moduleBuildId) {
    ModuleBuild build = moduleBuildService.getWithId(moduleBuildId);

    Optional<String> taskId = build.getTaskId();
    if (!taskId.isPresent()) {
      throw new NotFoundException("No taskId found for build: " + moduleBuildId);
    }

    String path = taskId.get() + "/service.log";
    Optional<Long> absent = Optional.absent();
    Optional<String> grep = Optional.absent();

    Optional<MesosFileChunkObject> chunk = singularityClient.readSandBoxFile(taskId.get(), path, grep, absent, absent);
    final long size;
    if (chunk.isPresent()) {
      size = chunk.get().getOffset();
    } else {
      size = findS3ServiceLog(taskId.get()).getSize();
    }

    return new Object() {

      public long getSize() {
        return size;
      }
    };
  }

  @POST
  @Path("{moduleBuildId}/mark-as-bad")
  public void markBuildAsBad(@PathParam("moduleBuildId") long moduleBuildId) {

    badBuildClient.markBuildAsBad(moduleBuildId);
  }

  @POST
  @Path("{moduleBuildId}/un-mark-as-bad")
  public void unMarkBuildAsBad(@PathParam("moduleBuildId") long moduleBuildId) {
    badBuildClient.unMarkBuildAsBad(moduleBuildId);
  }

  private SingularityS3Log findS3ServiceLog(String taskId) {
    Collection<SingularityS3Log> s3Logs = singularityClient.getTaskLogs(taskId);
    List<SingularityS3Log> serviceLogs = new ArrayList<>();
    for (SingularityS3Log s3Log : s3Logs) {
      if (s3Log.getGetUrl().contains("service.log")) {
        serviceLogs.add(s3Log);
      }
    }

    if (serviceLogs.isEmpty()) {
      throw new NotFoundException("No S3 log found for task " + taskId);
    } else if (serviceLogs.size() > 1) {
      throw new NotFoundException("Multiple S3 logs found for task " + taskId);
    } else {
      return serviceLogs.get(0);
    }
  }

  private LogChunk readS3LogChunk(String url, long offset, long length) throws Exception {
    HttpRequest request = HttpRequest.newBuilder()
        .setUrl(url)
        .addHeader("Range", String.format("bytes=%d-%d", offset, offset + length - 1))
        .build();

    HttpResponse response = asyncHttpClient.execute(request).get();
    if (response.isSuccess()) {
      return new LogChunk(response.getAsBytes(), offset);
    } else {
      String message = String.format("Error reading S3 log, status code %d, response %s", response.getStatusCode(), response.getAsString());
      LOG.warn(message);
      throw new WebApplicationException(Response.serverError().entity(message).type(MediaType.TEXT_PLAIN_TYPE).build());
    }
  }

  private boolean logCompleted(ModuleBuild build) {
    return build.getState().isComplete() && taskComplete(build.getTaskId().get());
  }

  private boolean taskComplete(String taskId) {
    Optional<SingularityTaskHistory> taskHistory = singularityClient.getHistoryForTask(taskId);
    if (!taskHistory.isPresent()) {
      return true;
    }

    SimplifiedTaskState taskState = SingularityTaskHistoryUpdate.getCurrentState(taskHistory.get().getTaskUpdates());
    return taskState == SimplifiedTaskState.DONE;
  }
}
