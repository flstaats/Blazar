import React, {Component, PropTypes} from 'react';
import PageHeader from '../shared/PageHeader.jsx';
import Breadcrumb from '../shared/Breadcrumb.jsx';
import UIGrid from '../shared/grid/UIGrid.jsx';
import UIGridItem from '../shared/grid/UIGridItem.jsx';
import BuildHistoryTable from './BuildHistoryTable.jsx';
import PageHeadline from '../shared/PageHeadline.jsx';
import SectionLoader from '../shared/SectionLoader.jsx';
import Icon from '../shared/Icon.jsx';

class Module extends Component {

  render() {

    if (this.props.loading) {
      return (
        <SectionLoader />
      );
    }
    let moduleName = this.props.params.module.substring(0, this.props.params.module.lastIndexOf('_'));
    let headline = (
      <span>
        <Icon prefix="mega" type="octicon" name="file-directory" classNames="headline-icon" />
        <span>{moduleName}</span>
      </span>
    );
    return (
      <div>
        <PageHeader>
          <Breadcrumb />
          <PageHeadline headline={headline} subheadline='Build History' />
        </PageHeader>
        <UIGrid>
          <UIGridItem size={12}>
            <BuildHistoryTable
              buildHistory={this.props.buildHistory}
              loading={this.props.loading}
            />
          </UIGridItem>
        </UIGrid>
      </div>
    );
  }

}


Module.propTypes = {
  loading: PropTypes.bool.isRequired,
  params: PropTypes.object.isRequired,
  buildHistory: PropTypes.array.isRequired
};

export default Module;
