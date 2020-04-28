<!--
This file is part of the Meeds project (https://meeds.io/).
Copyright (C) 2020 Meeds Association
contact@meeds.io
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 3 of the License, or (at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.
You should have received a copy of the GNU Lesser General Public License
along with this program; if not, write to the Free Software Foundation,
Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
-->
<template>
  <v-container
    id="AdministrationHamburgerNavigation"
    px-0
    py-0
    class="white d-none d-sm-block">
    <v-row v-if="navigationTree && navigationTree.length" class="mx-0 administrationTitle">
      <v-list-item @mouseover="openDrawer()" @click="openDrawer()">
        <v-list-item-icon class="mb-2 mt-3 mr-6 titleIcon"><i class="uiIcon uiIconToolbarNavItem uiAdministrationIcon"></i></v-list-item-icon>
        <v-list-item-content class="subtitle-2 titleLabel clickable">
          {{ this.$t('menu.administration.title') }}
        </v-list-item-content>
        <v-list-item-action class="my-0">
          <i class="uiIcon uiArrowRightIcon" color="grey lighten-1"></i>
        </v-list-item-action>
      </v-list-item>
    </v-row>
  </v-container>
</template>
<script>

export default {
  data() {
    return {
      vuetify: new Vuetify({
        dark: true,
        iconfont: 'mdi',
      }),
      navigationScope: 'ALL',
      navigationVisibilities: ['displayed'],
      navigations: [],
      embeddedTree: {
        // users and spaces
        'usersManagement': 'usersAndSpaces',
        'groupsManagement': 'usersAndSpaces',
        'membershipsManagement': 'usersAndSpaces',
        'spacesAdministration': 'usersAndSpaces',
        // content
        'siteExplorer': 'content',
        'wcmAdmin': 'content',
        'editors': 'content',
        // gamification
        'hook_management': 'gamification',
        'gamification/rules': 'gamification',
        'gamification/badges': 'gamification',
        'gamification/domains': 'gamification',
        // rewards
        'rewardAdministration/kudosAdministration': 'reward',
        'rewardAdministration/walletAdministration': 'reward',
        'rewardAdministration/rewardAdministration': 'reward',
        // portal
        'portalnavigation': 'portal',
        'groupnavigation': 'portal',
        'administration/pageManagement': 'portal',
        'administration/registry': 'portal',
      },
    };
  },
  computed:{
    visibilityQueryParams() {
      return this.navigationVisibilities.map(visibilityName => `visibility=${visibilityName}`).join('&');
    },
    navigationTree() {
      const navigationTree = [];
      const navigationParentObjects = {};

      let navigationsList = this.navigations.slice();
      navigationsList = this.filterDisplayedNavigations(navigationsList);
      this.computeLink(navigationsList);

      Object.keys(this.embeddedTree).forEach(embeddedTreeUri => {
        let nav = this.findNodeByUri(embeddedTreeUri, navigationsList);
        if (nav) {
          nav.displayed = true;
          const key = this.embeddedTree[embeddedTreeUri];
          nav = Object.assign({}, nav);
          nav.children = nav.children && nav.children.slice();

          if (navigationParentObjects[key]) {
            navigationParentObjects[key].children.push(nav);
          } else {
            navigationParentObjects[key] = {
              key: key,
              label: this.$t(`menu.administration.navigation.${key}`),
              children: [nav],
            };
            navigationTree.push(navigationParentObjects[key]);
          }
        }
      });

      navigationsList = this.filterDisplayedNavigations(navigationsList, true);
      const key = 'other';

      navigationsList.forEach(nav => {
        if (navigationParentObjects[key]) {
          navigationParentObjects[key].children.push(nav);
        } else {
          navigationParentObjects[key] = {
            key: key,
            label: this.$t(`menu.administration.navigation.${key}`),
            children: [nav],
          };
          navigationTree.unshift(navigationParentObjects[key]);
        }
      });
      return navigationTree;
    },
  },
  created() {
    fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/v1/navigations/group?exclude=/spaces.*&${this.visibilityQueryParams}`, {
      method: 'GET',
      credentials: 'include',
    })
      .then(resp => resp && resp.ok && resp.json())
      .then(data => this.navigations = data || [])
      .finally(() => {
        document.dispatchEvent(new CustomEvent('hideTopBarLoading'));
      });
  },
  methods: {
    mountSecondLevel(parentId) {
      const VueHamburgerMenuItem = Vue.extend({
        data: () => {
          return {
            navigations: this.navigationTree,
          };
        },
        template: `
          <exo-administration-navigations :navigations="navigations" />
        `,
      });
      const vuetify = this.vuetify;
      new VueHamburgerMenuItem({
        i18n: new VueI18n({
          locale: this.$i18n.locale,
          messages: this.$i18n.messages,
        }),
        vuetify,
      }).$mount(parentId);
    },
    openDrawer() {
      this.$emit('open-second-level');
    },
    filterDisplayedNavigations(navigations, excludeHidden) {
      return navigations.filter(nav => {
        if (nav.children) {
          nav.children = this.filterDisplayedNavigations(nav.children);
        }
        // eslint-disable-next-line no-extra-parens
        return !nav.displayed && (!excludeHidden || nav.visibility !== 'HIDDEN') && (nav.pageKey || (nav.children && nav.children.length));
      });
    },
    computeLink(navigations) {
      navigations.forEach(nav => {
        if (nav.children) {
          this.computeLink(nav.children);
        }
        const uriPart = nav.siteKey.name.replace(/\//g, ':');
        nav.link = `${eXo.env.portal.context}/g/${uriPart}/${nav.uri}`;
      });
    },
    findNodeByUri(uri, navigations) {
      for (const index in navigations) {
        const nav = navigations[index];
        if (nav.uri === uri) {
          return nav;
        } else if (nav.children) {
          const result = this.findNodeByUri(uri, nav.children);
          if (result) {
            return result;
          }
        }
      }
    },
  }
};
</script>
