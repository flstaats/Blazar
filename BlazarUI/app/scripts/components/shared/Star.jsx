import React, {Component, PropTypes} from 'react';
import Icon from '../shared/Icon.jsx';
import { bindAll } from 'underscore';
import classnames from 'classnames';

class Star extends Component {

  constructor(props) {
    super(props);
    bindAll(this, 'handleClick');
  }

  handleClick(event) {
    event.stopPropagation();

    if (this.props.disabled) {
      return;
    }

    const starInfo = {
      moduleId: this.props.moduleId,
      moduleName: this.props.moduleName,
      modulePath: this.props.modulePath
    };

    this.props.toggleStar(this.props.isStarred, starInfo);
  }

  getContainerClassNames() {
    return classnames([
       'star',
       this.props.className,
       {disabled: this.props.disabled},
       {selected: this.props.isStarred},
       {unselected: !this.props.isStarred}
    ]);
  }

  getIconClassNames() {
    return classnames([
       {'star': this.props.isStarred},
       {'star-o': !this.props.isStarred}
    ]);
  }

  render() {
    return (
      <span onClick={this.handleClick} className={this.getContainerClassNames()}>
          <Icon name={this.getIconClassNames()}></Icon>
      </span>
    );
  }

}

Star.defaultProps = {
  isStarred: false,
  disabled: false
};

Star.propTypes = {
  className: PropTypes.string,
  isStarred: PropTypes.bool.isRequired,
  toggleStar: PropTypes.func.isRequired,
  moduleId: PropTypes.number,
  modulePath: PropTypes.string.isRequired,
  moduleName: PropTypes.string.isRequired,
  disabled: PropTypes.bool
};

export default Star;
