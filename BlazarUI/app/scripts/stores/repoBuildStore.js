//
// Holds individual modules builds triggered by a Repo Build
//
import Reflux from 'reflux';
import RepoBuildActions from '../actions/repoBuildActions';
import RepoBuildApi from '../data/RepoBuildApi';

const RepoBuildStore = Reflux.createStore({

  listenables: RepoBuildActions,

  getModuleBuilds() {
    return this.builds;
  },
  
  onStopPolling() {
    this.api.stopPolling();
  },

  onLoadModuleBuilds(params) {

    this.api = new RepoBuildApi(params);
    
    this.api.startPolling((err, resp) => {
      if (err) {
        this.trigger({
          loadingModuleBuilds: false,
          error: {
            status: err.status,
            statusText: err.statusText
          }
        });
        return;
      }

      this.builds = resp;

      this.trigger({
        moduleBuilds: resp.moduleBuilds,
        currentRepoBuild: resp.currentRepoBuild,
        branchId: resp.branchId,
        loadingModuleBuilds: false
      });
    });  
  
  }

});

export default RepoBuildStore;
