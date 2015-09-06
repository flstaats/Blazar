import React, {Component, PropTypes} from 'react';
import SectionLoader from '../shared/SectionLoader.jsx';
import DashboardStarredModule from './DashboardStarredModule.jsx';

import PageHeader from '../shared/PageHeader.jsx';
import UIGrid from '../shared/grid/UIGrid.jsx';
import UIGridItem from '../shared/grid/UIGridItem.jsx';
import Headline from '../shared/headline/Headline.jsx';
import MutedMessage from '../shared/MutedMessage.jsx';
import Icon from '../shared/Icon.jsx';
import Helpers from '../ComponentHelpers';

class Dashboard extends Component {

  render() {
    if (this.props.loading) {
      return (
        <SectionLoader />
      );
    }

    let starredModuleTables = this.props.modulesBuildHistory.map( (modules, i) => {
      if (modules.length === 0) {
        return [];
      };
      
      return (
        <DashboardStarredModule 
          key={modules[i].build.id}
          modules={modules} />
      );

    })

    if (starredModuleTables.length === 0) {
      starredModuleTables = (
        <MutedMessage> You have no starred repos. </MutedMessage>
      );
    }

    return (
      <div>
        <PageHeader>
          <Headline>
            <Icon name="tachometer" classNames="headline-icon"></Icon>
            Dashboard
          </Headline>
        </PageHeader>

        <UIGrid>
          <UIGridItem size={12}>
            <h4 className="dashboard__section-title">Starred Modules</h4>
            {starredModuleTables}
          </UIGridItem>
        </UIGrid>

      </div>
    );
  }
}

Dashboard.propTypes = {
  loading: PropTypes.bool,
  builds: PropTypes.object,
  modulesBuildHistory: PropTypes.array
};

export default Dashboard;
