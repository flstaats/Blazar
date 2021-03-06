import React, {Component, PropTypes} from 'react';
import {has, contains, bindAll, isEmpty} from 'underscore';
import { withRouter, routerShape } from 'react-router';
import {humanizeText, timestampFormatted} from '../Helpers';

import Loader from '../shared/Loader.jsx';
import CancelBuildButton from '../shared/branch-build/CancelBuildButton.jsx';

import Commits from './Commits.jsx';
import InterProjectAlert from './InterProjectAlert.jsx';

import BuildStates from '../../constants/BuildStates';
import FINAL_BUILD_STATES from '../../constants/finalBuildStates';
import {LABELS} from '../constants';

import { getBranchStatePath } from '../../utils/blazarPaths';

class RepoBuildDetail extends Component {

  constructor(props) {
    super(props);
    bindAll(this, 'flipShowCommits', 'handleCancelBuild');

    this.state = {
      showCommits: false
    };
  }

  flipShowCommits() {
    this.setState({
      showCommits: !this.state.showCommits
    });
  }

  handleCancelBuild() {
    const {router, params: {branchId}} = this.props;
    router.push(getBranchStatePath(branchId));
  }

  renderCommits() {
    const build = this.props.currentRepoBuild;

    if (!has(build, 'commitInfo')) {
      return null;
    }

    const currentCommit = build.commitInfo.current;
    const newCommits = build.commitInfo.newCommits;
    const commitList = newCommits.length === 0 ? [currentCommit] : newCommits;

    return (
      <Commits
        currentCommit={build.commitInfo.current}
        previousCommit={build.commitInfo.previous}
        commits={commitList}
        showCommits={this.state.showCommits}
        anyNewCommits={newCommits.length > 0}
        flipShowCommits={this.flipShowCommits}
      />
    );
  }

  renderInterProjectBuildMessage() {
    const {upAndDownstreamModules} = this.props;

    if (!upAndDownstreamModules.interProjectBuildId) {
      return null;
    }

    let suffix;

    if (isEmpty(upAndDownstreamModules.rootRepoBuilds)) {
      suffix = '.';
    } else {
      suffix = ', triggered by an upstream module.';
    }

    return (
      <span className="build-detail-header__unstable-message">
        This build was part of an <strong>inter-project build</strong>{suffix}
      </span>
    );
  }

  renderUnstableMessage() {
    const {currentRepoBuild} = this.props;

    if (currentRepoBuild.state !== BuildStates.UNSTABLE) {
      return null;
    }

    return (
      <span className="build-detail-header__unstable-message">
        This build succeeded, but one or more modules in previous builds have failed and need to be rebuilt.
      </span>
    );
  }

  render() {
    if (this.props.error) {
      return null;
    } else if (this.props.loading) {
      return (
        <div className="build-detail">
          <Loader align="left" roomy={true} />
        </div>
      );
    }

    const build = this.props.currentRepoBuild;

    const buildDetail = {
      endtime: '',
      duration: '',
      durationPrefix: '',
      buildResult: humanizeText(build.state)
    };

    if (contains(FINAL_BUILD_STATES, build.state)) {
      buildDetail.endtime = timestampFormatted(build.endTimestamp);
      const conjunction = build.state === BuildStates.CANCELLED ? 'after' : 'in';

      buildDetail.duration = `${conjunction} ${build.duration}`;
    } else if (build.state === BuildStates.IN_PROGRESS) {
      buildDetail.duration = `started ${timestampFormatted(build.startTimestamp)}`;
    }

    return (
      <div>
        <div className={`build-detail alert alert-${LABELS[build.state]}`}>
          <p className="build-detail-header__build-state">
            Build {buildDetail.buildResult}
            <span className="build-detail-header__timestamp">{buildDetail.duration}</span>
            {this.renderInterProjectBuildMessage()}
            {this.renderUnstableMessage()}
          </p>
          <CancelBuildButton
            onCancel={this.handleCancelBuild}
            build={build}
            btnStyle="danger"
            btnSize="xsmall"
          />
        </div>
        <InterProjectAlert
          branchInfo={this.props.branchInfo}
          upAndDownstreamModules={this.props.upAndDownstreamModules}
        />
        {this.renderCommits()}
      </div>
    );
  }

}


RepoBuildDetail.propTypes = {
  loading: PropTypes.bool.isRequired,
  currentRepoBuild: PropTypes.object,
  error: PropTypes.string,
  upAndDownstreamModules: PropTypes.object.isRequired,
  params: PropTypes.object.isRequired,
  router: routerShape.isRequired,
  branchInfo: PropTypes.object.isRequired
};

export default withRouter(RepoBuildDetail);
