const path = require('path');
const merge = require('webpack-merge');
const webpackCommonConfig = require('./webpack.common.js');

// the display name of the war
const config = merge(webpackCommonConfig, {
  output: {
    path: path.resolve(__dirname, `./target/web/`),
    filename: 'javascript/[name].bundle.js',
    libraryTarget: 'amd'
  }
});

module.exports = config;
