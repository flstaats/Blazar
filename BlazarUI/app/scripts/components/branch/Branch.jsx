import React from 'react';
import config from '../../config.js';
import PageHeader from '../shared/PageHeader.jsx'
import PageSection from '../shared/PageSection.jsx'
import Breadcrumb from '../shared/Breadcrumb.jsx'
import UIGrid from '../shared/grid/UIGrid.jsx'
import UIGridItem from '../shared/grid/UIGridItem.jsx'


class Branch extends React.Component{

  constructor(props, context) {
   super(props);
  }


  render() {
    return (
      <div>
        <PageHeader>
          <h2 className='header-primary'> {this.props.params.branch} <span className='header-subheader'> / Branch Modules  </span> </h2>
          <Breadcrumb />
        </PageHeader>
        <UIGrid>
          <UIGridItem size={12}>
            <PageSection headline='Module Listing'></PageSection>
          </UIGridItem>
        </UIGrid>
      </div>
    );
  }

}


export default Branch;

Branch.defaultProps = { loading: true }

Branch.propTypes = {
  params : React.PropTypes.object.isRequired
}

