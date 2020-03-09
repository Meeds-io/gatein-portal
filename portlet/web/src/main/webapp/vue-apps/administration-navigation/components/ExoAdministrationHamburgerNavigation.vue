<template>
  <v-container
    id="AdministrationHamburgerNavigation"
    px-0
    py-0
    class="white">
    <v-row class="mx-0 administrationTitle">
      <v-list-item @mouseover="openDrawer()" @click="openDrawer()">
        <v-list-item-icon class="mb-2 mt-3 mr-6 titleIcon"><i class="uiIcon uiAdministrationIcon"></i></v-list-item-icon>
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
      drawer: null,
      vuetify: new Vuetify({
        dark: true,
        iconfont: 'mdi',
      }),
      usersItems: [
        {
          name: 'Add users',
          path: 'administrators/administration/newStaff'
        },
        {
          name: 'Users & Roles',
          path:'administrators/administration/management'
        }
      ],
      contentItems: [
        {
          name: 'Explorer',
          path: 'web-contributors/siteExplorer'
        },
        {
          name: 'Repository Admin',
          path: 'web-contributors/wcmAdmin'
        }
      ],
      gamificationItems: [
        {
          name: 'Connector',
          children: [
            {
              name: 'Github',
              path: '#'
            }
          ]
        },
        {
          name: 'Rules',
          path: 'administrators/gamification/rules'
        },
        {
          name: 'Badges',
          path: 'administrators/gamification/badges'
        },
        {
          name: 'Domains',
          path: 'administrators/gamification/domains'
        },
      ],
      rewardsItems: [
        {
          name: 'Kudos',
          path: 'administrators/rewardAdministration/kudosAdministration'
        },
        {
          name: 'Wallet',
          path: 'rewarding/rewardAdministration/walletAdministration'
        },
        {
          name: 'Rewards',
          path: 'rewarding/rewardAdministration'
        }
      ],
      searchItems: [
        {
          name: 'Indexing',
          path: 'administrators/searchIndexing'
        },
        {
          name: 'Connectors',
          path: '#'
        }
      ],
      portalItems: [
        {
          name: 'Sites',
          path: 'administrators/portalnavigation'
        },
        {
          name: 'Pages',
          path: 'administrators/administration/pageManagement'
        },
        {
          name: 'Branding',
          path: 'administrators/branding'
        },
        {
          name: 'Layout composer categories',
          path: 'administrators/administration/registry'
        }
      ],
      spacesItems: [
        {
          name: 'Manage Spaces',
          path: 'users/spacesAdministration'
        },
        {
          name: 'Manage Templates',
          path: 'administrators/spacesTemplates'
        }
      ],
      othersItems: [
        {
          name: 'Applications',
          path: 'administrators/appCenterAdminSetup'
        },
        {
          name: 'Notifications',
          path: 'administrators/notification'
        },
        {
          name: 'Web Conferencing',
          path: 'administrators/webconferencing'
        }
      ],
    };
  },
  methods: {
    mountSecondLevel(parentId) {
      const VueHamburgerMenuItem = Vue.extend({
        data: () => {
          return {
            usersItems: this.usersItems,
            contentItems: this.contentItems,
            gamificationItems: this.gamificationItems,
            rewardsItems: this.rewardsItems,
            searchItems: this.searchItems,
            portalItems: this.portalItems,
            spacesItems: this.spacesItems,
            othersItems: this.othersItems,
          };
        },
        template: `
          <div>
            <exo-administration-menu-item :administration-item="usersItems" item-title="Users"/>
            <exo-administration-menu-item :administration-item="contentItems" item-title="Content"/>
            <exo-administration-menu-item :administration-item="gamificationItems" item-title="Gamification"/>
            <exo-administration-menu-item :administration-item="rewardsItems" item-title="Reward"/>
            <exo-administration-menu-item :administration-item="searchItems" item-title="Search"/>
            <exo-administration-menu-item :administration-item="portalItems" item-title="Portal"/>
            <exo-administration-menu-item :administration-item="spacesItems" item-title="Spaces"/>
            <exo-administration-menu-item :administration-item="othersItems" item-title="Other"/>
          </div>
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
    }
  }
};
</script>
