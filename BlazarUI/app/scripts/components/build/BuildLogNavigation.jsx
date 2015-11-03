import React, {Component, PropTypes} from 'react';
import {dataTagValue} from '../Helpers';

class BuildLogNavigation extends Component {
  constructor() {
    this.handleNavClick = this.handleNavClick.bind(this);
  }

  handleNavClick(e) {
    const position = dataTagValue(e, 'position');
    this.props.navigateLogChange(position);
  }

  render() {
    return (
      <nav className='text-right'>
        <button data-position='top' onClick={this.handleNavClick} className='log-nav-btn btn btn-default'>To Top</button>
        <button data-position='bottom' onClick={this.handleNavClick} className='log-nav-btn btn btn-default'>To Bottom</button>
      </nav>
    );
  }
}
// 
BuildLogNavigation.propTypes = {
  navigateLogChange: PropTypes.func.isRequired
};

export default BuildLogNavigation;
