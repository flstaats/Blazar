/*global app*/
import React from 'react';
import BuildingIcon from '../shared/BuildingIcon.jsx';
let Link = require('react-router').Link;

class Module extends React.Component {

  render() {
    let {buildState, gitInfo, module} = this.props.repo;
    let moduleLink = `${app.config.appRoot}/${gitInfo.host}/${gitInfo.organization}/${gitInfo.repository}/${gitInfo.branch}/${module.name}/${buildState.buildNumber}`;

    return (
      <Link to={moduleLink} className='sidebar__repo-module'>
        <BuildingIcon result={buildState.result} size='small' />
        {module.name}
      </Link>
    );
  }
}

Module.propTypes = {
  repo: React.PropTypes.object,
  name: React.PropTypes.string,
  link: React.PropTypes.string
};

export default Module;
