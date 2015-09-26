/*global config*/
import React, {Component, PropTypes} from 'react';
import Branches from './Branches.jsx';
import PageHeader from '../shared/PageHeader.jsx';
import Breadcrumb from '../shared/Breadcrumb.jsx';
import UIGrid from '../shared/grid/UIGrid.jsx';
import UIGridItem from '../shared/grid/UIGridItem.jsx';
import Headline from '../shared/headline/Headline.jsx';
import HeadlineDetail from '../shared/headline/HeadlineDetail.jsx';
import SectionLoader from '../shared/SectionLoader.jsx';
import Icon from '../shared/Icon.jsx';

class Repo extends Component {

  render() {

    if (this.props.loading) {
      return (
        <SectionLoader />
      );
    }

    return (
      <div>
        <PageHeader>
          <Breadcrumb
            appRoot={config.appRoot}
            params={this.props.params}
          />
        </PageHeader>
        <UIGrid>
          <UIGridItem size={12}>
            <Headline>
              <Icon type="octicon" name="repo" classNames="headline-icon" />
              <span>{this.props.params.repo}</span>
              <HeadlineDetail>
                Branches
              </HeadlineDetail>
            </Headline>
            <Branches
              branches={this.props.branches}
              loading={this.props.loading}
              branchToggleStates={this.props.branchToggleStates}
              updateBranchToggleState={this.props.updateBranchToggleState}
            />
          </UIGridItem>
        </UIGrid>
      </div>
    );
  }
}

Repo.propTypes = {
  loading: PropTypes.bool.isRequired,
  branches: PropTypes.array,
  params: PropTypes.object.isRequired,
  branchToggleStates: PropTypes.object.isRequired,
  updateBranchToggleState: PropTypes.func.isRequired
};

export default Repo;
