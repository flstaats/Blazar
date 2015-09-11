/*global config*/
import Reflux from 'reflux';
import ActionSettings from './utils/ActionSettings';
import BuildHistory from '../collections/BuildHistory';
import BranchDefinition from '../models/BranchDefinition';
import BranchModules from '../collections/BranchModules';

let gitInfo;

const buildHistoryActionSettings = new ActionSettings;

const BuildHistoryActions = Reflux.createActions([
  'loadBuildHistory',
  'loadModulesBuildHistory',
  'loadBuildHistorySuccess',
  'loadBuildHistoryError',
  'loadModulesBuildHistorySuccess',
  'updatePollingStatus'
]);

BuildHistoryActions.loadBuildHistory.preEmit = function(data) {
  startPolling(data);
};

BuildHistoryActions.loadModulesBuildHistory = function(options) {

  let modulesHistory = [];

  if (options.modules.length === 0) {
    BuildHistoryActions.loadModulesBuildHistorySuccess([]);
    return;
  }

  options.modules.forEach((module) => {
    const buildHistory = new BuildHistory(module.moduleId);
    const promise = buildHistory.fetch();

    promise.done( () => {
      const builds = buildHistory.limit(options.limit);
      modulesHistory.push({
        module: module,
        builds: builds
      });
      BuildHistoryActions.loadModulesBuildHistorySuccess(modulesHistory);
    });

  });

};

BuildHistoryActions.updatePollingStatus = function(status) {
  buildHistoryActionSettings.setPolling(status);
};

function getBranchId() {
    const branchDefinition = new BranchDefinition(gitInfo);
    const branchPromise =  branchDefinition.fetch();

    branchPromise.done( () => {
      gitInfo.branchId = branchDefinition.data.id;
      getModule();
    });
    branchPromise.error( () => {
      BuildHistoryActions.loadBuildHistoryError('an error occured');
    });
}

function getModule() {
  const branchModules = new BranchModules(gitInfo.branchId);
  const modulesPromise = branchModules.fetch();

  modulesPromise.done( () => {
    gitInfo.moduleId = branchModules.data.find((m) => {
      return m.name === gitInfo.module;
    }).id;
    getBuildHistory();
  });
  modulesPromise.error( () => {
    BuildHistoryActions.loadBuildHistoryError('an error occured');
  });
}

function getBuildHistory() {
  const buildHistory = new BuildHistory(gitInfo.moduleId);
  const promise = buildHistory.fetch();

  promise.done( () => {
    BuildHistoryActions.loadBuildHistorySuccess(buildHistory.data);
  });
  promise.error( () => {
    BuildHistoryActions.loadBuildHistoryError('an error occured');
  });
}

function startPolling(data) {
  gitInfo = data;

  (function doPoll() {
    getBranchId();
    if (buildHistoryActionSettings.polling) {
      setTimeout(doPoll, config.buildsRefresh);
    }
  })();
}


export default BuildHistoryActions;
