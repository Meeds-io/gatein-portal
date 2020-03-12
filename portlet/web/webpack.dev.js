const path = require('path');
const merge = require('webpack-merge');
const webpackCommonConfig = require('./webpack.common.js');

// add the server path to your server location path
const exoServerPath = "/exo-server";

let config = merge(webpackCommonConfig, {
  output: {
    path: path.resolve(`${exoServerPath}/webapps/web/`),
    filename: 'javascript/[name].bundle.js'
  }
});

module.exports = config;
