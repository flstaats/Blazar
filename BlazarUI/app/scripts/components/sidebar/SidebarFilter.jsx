import React, {Component, PropTypes} from 'react';
import {bindAll} from 'underscore';
import SearchFilter from '../shared/SearchFilter.jsx';
import StarredToggle from './StarredToggle.jsx';

class SidebarFilter extends Component {

  constructor(props, context) {
    super(props, context);

    this.state = {
      inputValue: ''
    };

    bindAll(this,
      'handleFilterChange',
      'toggleFilter'
    );

  }


  setInputValue(value) {
    this.props.updateResults(value);
  }

  handleFilterChange(value) {
    this.setInputValue(value);
  }

  toggleFilter(filter) {
    let showStarred = filter === 'starred' ? true : false;
    this.props.updateStarred(showStarred);
  }

  render() {
    if (this.props.loading || this.props.repos.length === 0) {
      return <div />;
    }

    return (
      <div>
        <div className='sidebar__filter-search'>
          <SearchFilter
            ref="buildFilterSearch"
            placeholder='Filter modules...'
            inputValue={this.props.filterText}
            options={this.props.modules}
            onChange={this.handleFilterChange}
            showStarred={this.props.showStarred} />
        </div>
        <StarredToggle
          toggleFilter={this.toggleFilter}
          showStarred={this.props.showStarred} />
      </div>
    );

  }
}

SidebarFilter.contextTypes = {
  router: PropTypes.func.isRequired
};

SidebarFilter.propTypes = {
  updateResults: PropTypes.func.isRequired,
  modules: PropTypes.array.isRequired,
  loading: PropTypes.bool.isRequired,
  filterText: PropTypes.string.isRequired,
  repos: PropTypes.array.isRequired,
  updateStarred: PropTypes.func.isRequired,
  showStarred: PropTypes.bool.isRequired
};

export default SidebarFilter;
