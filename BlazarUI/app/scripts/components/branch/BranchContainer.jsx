import React, {Component, PropTypes} from 'react';
import Branch from './Branch.jsx';
import PageContainer from '../layout/PageContainer.jsx';
import BranchStore from '../../stores/branchStore';
import BranchActions from '../../actions/branchActions';

class BranchContainer extends Component {

  constructor() {
    this.state = {
      modules: [],
      loading: true
    };
  }

  componentDidMount() {
    this.unsubscribe = BranchStore.listen(this.onStatusChange.bind(this));
    BranchActions.loadModules(this.props.params);
  }

  componentWillUnmount() {
    BranchActions.updatePollingStatus(false);
    this.unsubscribe();
  }

  onStatusChange(state) {
    this.setState(state);
  }

  componentWillReceiveProps(nextprops) {
    BranchActions.loadModules(nextprops.params);
  }

  render() {

    return (
      <PageContainer>
        <Branch
          params={this.props.params}
          modules={this.state.modules}
          loading={this.state.loading}
        />
      </PageContainer>
    );
  }
}


BranchContainer.propTypes = {
  params: PropTypes.object.isRequired
};

export default BranchContainer;
