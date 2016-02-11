import React, {Component, PropTypes} from 'react';
import {Link} from 'react-router';
import BuildStates from '../../constants/BuildStates.js';
import ProgressBar from 'react-bootstrap/lib/ProgressBar';
import {contains, has} from 'underscore';
import {humanizeText, timestampFormatted, timestampDuration, tableRowBuildState, truncate, buildResultIcon} from '../Helpers';

class RepoBuildModulesTableRow extends Component {
  
  renderBuildLink() {
    const {data, params} = this.props;

    if (data.state == BuildStates.SKIPPED) {
      return (
        <span>{data.name}</span>
      );
    }

    return (
      <span><Link to={data.blazarPath}>{data.name}</Link></span>
    );    
  }

  renderSingularityLink() {
    const {taskId} = this.props.data;

    if (!taskId) {
      return null;
    }

    //
    // to do: surface singularity env
    //
    const singularityPath = `https://tools.hubteamqa.com/singularity/task/${taskId}`;

    return (
      <a href={singularityPath} target="_blank">{truncate(taskId, 30, true)}</a>  
    );
  }

  renderDuration() {
    const {data} = this.props;
    let durationText;

    if (data.state === BuildStates.IN_PROGRESS) {
      durationText = 'In Progress';
    }

    else if (data.state === BuildStates.SKIPPED) {
      durationText = 'Skipped';
    }

    else if (data.state === BuildStates.QUEUED) {
      durationText = 'Queued';
    }

    else if (data.state === BuildStates.LAUNCHING) {
      durationText = 'Launching';
    }

    else {
      durationText = timestampDuration(data.startTimestamp, data.endTimestamp)
    }

    return durationText;
  }

  render() {
    const {data, params} = this.props;
    return (
      <tr className={tableRowBuildState(data.state)}>
        <td className='build-status'>
          {buildResultIcon(data.state)}
        </td>
        <td className='table-cell-link'>
          {this.renderBuildLink()}
        </td>
        <td>
          {timestampFormatted(data.startTimestamp)}
        </td>
        <td>
          {this.renderDuration()}
        </td>
        <td>
          {this.renderSingularityLink()}
        </td>
        <td>
        </td>
      </tr>
    );
  }

}

RepoBuildModulesTableRow.propTypes = {
  data: PropTypes.object.isRequired
};

export default RepoBuildModulesTableRow;
