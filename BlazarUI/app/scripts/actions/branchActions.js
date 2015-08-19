import Reflux from 'reflux';
import $ from 'jQuery';
import ActionSettings from './utils/ActionSettings';
import Builds from '../collections/Builds';

let BranchActionSettings = new ActionSettings;

let BranchActions = Reflux.createActions([
  'loadModules',
  'loadModulesSuccess',
  'loadModulesError',
  'updatePollingStatus'
]);

BranchActions.loadModules.preEmit = function(data) {
  startPolling(data);
}

BranchActions.updatePollingStatus = function (status) {
  BranchActionSettings.setPolling(status);
};

function startPolling(data){

  (function doPoll(){
    let builds = new Builds();
    let promise = builds.fetch();

    promise.done( () => {
      let branch = builds.getBranchModules(data);
      BranchActions.loadModulesSuccess(branch.modules);
    });

    promise.error( (err) => {
      console.warn('Error connecting to the API. Check that you are connected to the VPN');
      BranchActions.loadModulesError('an error occured');
    })

    promise.always( () => {
      if (BranchActionSettings.polling) {
        setTimeout(doPoll, config.buildsRefresh);
      }
    });

  })();

};

export default BranchActions;