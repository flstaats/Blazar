import React, {Component, PropTypes} from 'react';
import {Link} from 'react-router';
import {Button} from 'react-bootstrap';
import {bindAll} from 'underscore';
import PageContainer from '../shared/PageContainer.jsx';
import UIGrid from '../shared/grid/UIGrid.jsx';
import UIGridItem from '../shared/grid/UIGridItem.jsx';
import Headline from '../shared/headline/Headline.jsx';
import HeadlineDetail from '../shared/headline/HeadlineDetail.jsx';
import BranchBuildHistoryTable from './BranchBuildHistoryTable.jsx';
import BranchHeadline from './BranchHeadline.jsx';
import Loader from '../shared/Loader.jsx';
import GenericErrorMessage from '../shared/GenericErrorMessage.jsx';
import BuildButton from './BuildButton.jsx';
import ModuleModal from '../shared/ModuleModal.jsx';
import MalformedFileNotification from '../shared/MalformedFileNotification.jsx';
import Immutable from 'immutable';

import StarStore from '../../stores/starStore';
import StarActions from '../../actions/starActions';

import BranchStore from '../../stores/branchStore';
import BranchActions from '../../actions/branchActions';
import RepoStore from '../../stores/repoStore';
import RepoActions from '../../actions/repoActions';

import {getPreviousBuildState} from '../Helpers.js';

let initialState = {
  builds: null,
  stars: [],
  loadingBranches: true,
  loadingStars: true,
  loadingModules: true,
  loadingMalformedFiles: true,
  loadingRepo: true,
  malformedFiles: [],
  showModuleModal: false,
  modules: Immutable.List.of(),
  selectedModules: [],
  buildDownstreamModules: 'WITHIN_REPOSITORY',
  branchId: 0,
  branchInfo: {},
  branches: []
};

class BranchContainer extends Component {

  constructor() {
    this.state = initialState;

    bindAll(this, 'openModuleModal', 'closeModuleModal', 'onStatusChange', 'updateSelectedModules', 'updateDownstreamModules', 'triggerBuild');
  }

  componentDidMount() {
    this.setup(this.props.params);
  }

  componentWillReceiveProps(nextprops) {
    this.tearDown();
    this.setup(nextprops.params);
    this.setState(initialState);
  }

  componentWillUnmount() {
    this.tearDown();
  }

  onStatusChange(state) {
    this.setState(state);
  }
    
  setup(params) {
    this.unsubscribeFromBranch = BranchStore.listen(this.onStatusChange);
    this.unsubscribeFromRepo = RepoStore.listen(this.onStatusChange);
    this.unsubscribeFromStars = StarStore.listen(this.onStatusChange);
    StarActions.loadStars('repoBuildContainer');
    BranchActions.loadBranchBuildHistory(params);
    BranchActions.loadBranchInfo(params);
    BranchActions.loadBranchModules(params);
    BranchActions.loadMalformedFiles(params);
    BranchActions.startPolling(params);
    this.tryLoadBranches();
  }
  
  tearDown() {
    BranchActions.stopPolling();
    this.unsubscribeFromStars();
    this.unsubscribeFromBranch();
    this.unsubscribeFromRepo();
  }

  tryLoadBranches() {
    if (!this.state || !this.state.branchInfo.repositoryId) {
      setTimeout(this.tryLoadBranches.bind(this), 200);
      return;
    }

    RepoActions.loadBranches(this.state.branchInfo.repositoryId);
  }

  refreshBranches() {
    this.tryLoadBranches();
  }

  openModuleModal() {
    this.setState({
      showModuleModal: true
    });
  }

  closeModuleModal() {
    this.setState({
      showModuleModal: false
    });
  }

  updateSelectedModules(modules) {
    this.setState({
      selectedModules: modules
    });
  }

  updateDownstreamModules(enumValue) {
    this.setState({
      buildDownstreamModules: enumValue
    });
  }

  triggerBuild() {
    BranchActions.triggerBuild(this.props.params, this.state.selectedModules, this.state.buildDownstreamModules);
  }

  isLoading() {
    return this.state.loadingBranches
      || this.state.loadingStars
      || this.state.loadingModules
      || this.state.loadingRepo;
  }
  
  renderTable() {
    if (this.state.error) {
      return (
        <GenericErrorMessage
          message={this.state.error}
        />
      );
    }
    
    else {
      if (this.state.builds) {
        this.props.params.prevBuildState = getPreviousBuildState(this.state.builds);
      }

      return (
        <BranchBuildHistoryTable
          data={this.state.builds}
          loading={this.state.loadingBranches}
          {...this.state}
          {...this.props}
        />
      );
    }
    
  }

  renderMalformedFileAlert() {
    if (this.state.loadingMalformedFiles || this.state.malformedFiles.length === 0) {
      return null;
    }

    return (
      <UIGrid>
        <UIGridItem size={12}>
          <MalformedFileNotification
          loading={this.state.loadingMalformedFiles}
          malformedFiles={this.state.malformedFiles} />
        </UIGridItem>
      </UIGrid>
    );
  }

  renderBuildSettingsButton() {
    if (this.state.loadingBranches || this.state.loadingRepo) {
      return;
    }

    const buildSettingsLink = `/settings/branch/${this.props.params.branchId}`;

    return (
      <Link to={buildSettingsLink}>
        <Button>
            Build Settings
        </Button>
      </Link>
    );
  }

  render() {
    return (
      <PageContainer>
        {this.renderMalformedFileAlert()}
        <UIGrid>
          <UIGridItem size={7}>
            <BranchHeadline
              loading={this.isLoading()}
              branchInfo={this.state.branchInfo}
              branches={this.state.branches}
              refreshBranches={this.refreshBranches.bind(this)}
              {...this.state}
              {...this.props}
            />
          </UIGridItem>
          <UIGridItem style={{'paddingTop': '6px'}} size={5} align='RIGHT'>
            <BuildButton 
              openModuleModal={this.openModuleModal}
              loading={this.isLoading()}
              error={this.state.error}
            />
            <ModuleModal
              loadingModules={this.isLoading()}
              showModal={this.state.showModuleModal}
              closeModal={this.closeModuleModal}
              triggerBuild={this.triggerBuild}
              onSelectUpdate={this.updateSelectedModules}
              onCheckboxUpdate={this.updateDownstreamModules}
              modules={this.state.modules}
            />
            {this.renderBuildSettingsButton()}
          </UIGridItem>
        </UIGrid>
        <UIGrid>
          <UIGridItem size={12}>
            {this.renderTable()}
          </UIGridItem>
        </UIGrid>
      </PageContainer>
    );
  }
}


BranchContainer.propTypes = {
  params: PropTypes.object.isRequired
};

export default BranchContainer;
