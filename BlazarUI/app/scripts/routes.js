/*global config*/
import React from 'react';
import { Route, IndexRoute, Redirect } from 'react-router';
import $ from 'jquery';

// Pages
import App from './pages/app.jsx';
import Dashboard from './pages/dashboard.jsx';
import Org from './pages/org.jsx';
import Repo from './pages/repo.jsx';
import Branch from './pages/branch.jsx';
import Settings from './pages/settings.jsx';
import Build from './pages/build.jsx';
import RepoBuild from './pages/repoBuild.jsx';
import NotFound from './pages/notFound.jsx';

function redirectRepoBuildShortlink(nextState, replace, callback) {
  const data = $.ajax({
    url: `${config.apiRoot}/branches/builds/${nextState.params.repoBuildId}`,
    type: 'GET',
    dataType: 'json'
  }).then((resp) => {
    const {branchId, buildNumber} = resp;
    replace(`/builds/branch/${branchId}/build/${buildNumber}`);
  }, (error) => {
    replace('/not-found');
  }).always(callback);
}

function redirectCoordinates(nextState, replace, callback) {
  const {coordinates, buildNumber} = nextState.params;
  const data = $.ajax({
    url: `${config.apiRoot}/builds/coords/${coordinates}/build/${buildNumber}`
  }).then((resp) => {
    const {branchId, moduleName} = resp;
    replace(`/builds/branch/${branchId}/build/${buildNumber}/module/${moduleName}`);
  }, (error) => {
    replace('/not-found');
  }).always(callback);
}

const routes = (
  <Route name='app' path='/' component={App}>
    <IndexRoute name='dashboard' component={ Dashboard } />
    <Route name='host' path='/builds/org/:org' component={ Org } />
    <Route name='repo' path='/builds/repo/:repo' component={ Repo } />
    <Route name='branch' path='/builds/branch/:branchId' component={ Branch } />
    <Route name='settings' path='/settings/branch/:branchId' component={ Settings } />
    <Route name='repoBuild' path='/builds/branch/:branchId/build/:buildNumber' component={ RepoBuild } />
    <Route name='build' path='/builds/branch/:branchId/build/:buildNumber/module/:moduleName' component={ Build } />
    <Route name='repoBuildShortlink' path='/builds/repo-build/:repoBuildId' onEnter={redirectRepoBuildShortlink} />
    <Route name='coordinates' path='/builds/coords/:coords/build/:buildNumber' onEnter={redirectCoordinates} />
    <Route name='notFound' path="*" component={ NotFound } />
  </Route>
);

export default routes;
