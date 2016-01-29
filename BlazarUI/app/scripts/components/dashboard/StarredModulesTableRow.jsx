import React, {Component, PropTypes} from 'react';
const Link = require('react-router').Link;
import {has} from 'underscore';
import {buildResultIcon, tableRowBuildState, timestampFormatted} from '../Helpers';

import Icon from '../shared/Icon.jsx';
import Sha from '../shared/Sha.jsx';
import CommitMessage from '../shared/CommitMessage.jsx';

class StarredModulesTableRow extends Component {


  render() {

    const item = this.props.item;

    if (item.get('lastBuild') === undefined) {
      return (
        <tr>
          <td />
          <td>
            <Link to={item.get('gitInfo').get('blazarRepositoryPath')}>{item.get('gitInfo').get('repository')}</Link>
          </td>
          <td> No build history </td>
          <td />
          <td />
          <td />
        </tr>
      );
    }

    const latestBuild = item.get('lastBuild');
    const latestBuildGitInfo = item.get('gitInfo');

    let commitMessage;
    let sha;

    commitMessage = (
      <CommitMessage message={latestBuild.get('commitInfo').get('current').get('message')} />
    );
    
    if (latestBuild.get('sha')) {
      const gitInfo = {
        host: latestBuildGitInfo.get('host'),
        organization: latestBuildGitInfo.get('organization'),
        repository: latestBuildGitInfo.get('repository')
      }

      sha = <Sha gitInfo={gitInfo} build={latestBuild} />;
    }

    return (
      <tr className={tableRowBuildState(latestBuild.state)}>
        <td className='build-status'>
          {buildResultIcon(latestBuild.get('state'))}
        </td>
        <td>
          <Link to={latestBuildGitInfo.get('blazarRepositoryPath')}>{latestBuildGitInfo.get('repository')}</Link>
        </td>
        <td> 
          <Link to={latestBuildGitInfo.get('blazarBranchPath')}>{latestBuildGitInfo.get('branch')}</Link>
        </td>
        <td>
          <Link to={latestBuild.get('blazarPath')}>{latestBuild.get('buildNumber')}</Link>
        </td>
        <td>
          {timestampFormatted(latestBuild.get('startTimestamp'))}
        </td>
        <td>
          {sha}
        </td>
        <td>
          {commitMessage}
        </td>
      </tr>
    );

  }
  
}

StarredModulesTableRow.propTypes = {
  item: PropTypes.object,
  index: PropTypes.number,
};

export default StarredModulesTableRow;
