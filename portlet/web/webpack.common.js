const path = require('path');
const VueLoaderPlugin = require('vue-loader/lib/plugin');

let config = {
  context: path.resolve(__dirname, '.'),
  // set the entry point of the application
  // can use multiple entry
  entry: {
    hamburgerMenu: './src/main/webapp/vue-apps/hamburger-menu/main.js',
    siteHamburgerMenu: './src/main/webapp/vue-apps/site-navigation/main.js',
    administrationHamburgerMenu: './src/main/webapp/vue-apps/administration-navigation/main.js',
    userHamburgerMenu: './src/main/webapp/vue-apps/user-navigation/main.js',
  },
  module: {
    rules: [
      {
        test: /\.js$/,
        exclude: /node_modules/,
        use: [
          'babel-loader',
          'eslint-loader',
        ]
      },
      {
        test: /\.vue$/,
        use: [
          'vue-loader',
          'eslint-loader',
        ]
      }
    ]
  },
  plugins: [
    new VueLoaderPlugin()
  ]
};

module.exports = config;