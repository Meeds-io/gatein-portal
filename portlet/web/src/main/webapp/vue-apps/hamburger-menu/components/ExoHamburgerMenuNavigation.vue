<template>
  <v-app
    color="transaprent"
    class="HamburgerNavigationMenu"
    flat>
    <a class="HamburgerNavigationMenuLink">
      <div class="px-5 py-3" @click="openOrHideMenu()">
        <v-icon size="24">fa-bars</v-icon>
      </div>
    </a>
    <v-divider vertical />
    <v-navigation-drawer
      id="HamburgerMenuNavigation"
      v-model="hamburgerMenu"
      :hide-overlay="initializing"
      :style="hamburgerMenuStyle"
      :width="secondLevel ? 620 : 310"
      absolute
      left
      temporary
      max-width="100vw"
      max-height="100vh"
      height="100vh">
      <v-row class="HamburgerMenuLevelsParent fill-height" no-gutters @mouseleave="hideSecondLevel()">
        <div class="HamburgerMenuFirstLevelParent border-box-sizing">
          <v-flex v-for="contentDetail in contents" :key="contentDetail.id">
            <div :id="contentDetail.id"></div>
          </v-flex>
        </div>
        <div v-show="secondLevel" :class="secondLevel && 'open'" class="HamburgerMenuSecondLevelParent border-box-sizing">
          <div id="HamburgerMenuSecondLevel"></div>
        </div>
      </v-row>
    </v-navigation-drawer>
  </v-app>
</template>
<script>
export default {
  data(){
    return {
      initializing: false,
      hamburgerMenu: false,
      secondLevel: false,
      openedSecondLevel: null,
      contents: [],
      vueChildInstances: {},
      idleTime: 20,
      vuetify: new Vuetify({
        dark: true,
        iconfont: 'mdi',
      }),
    };
  },
  computed: {
    hamburgerMenuStyle() {
      return this.initializing ? 'left: -5000px;' : '';
    },
  },
  watch: {
    hamburgerMenu() {
      if (this.hamburgerMenu) {
        $('body').addClass('hide-scroll');
      } else {
        $('body').removeClass('hide-scroll');
      }
      this.$nextTick().then(() => {
        $('.HamburgerNavigationMenu .v-overlay').click(() => {
          this.hamburgerMenu = false;
        });
      });
    },
  },
  created() {
    document.addEventListener('exo-hamburger-menu-navigation-refresh', this.refreshMenu);
    $(document).on('keydown', (event) => {
      if (event.key === 'Escape') {
        this.hamburgerMenu = false;
      }
    });
    this.refreshMenu();
  },
  methods: {
    refreshMenu() {
      const extensions = extensionRegistry.loadExtensions('exo-hamburger-menu-navigation', 'exo-hamburger-menu-navigation-items');
      if (extensions.length < eXo.portal.hamburgerMenuItems) {
        return;
      }
      extensions.sort((a, b) => a.priority - b.priority);
      this.contents = extensions;
      const contentsToLoad = this.contents.filter(contentDetail => !contentDetail.loaded);
      this.initializing = contentsToLoad.length;
      const vuetify = this.vuetify;
      contentsToLoad.forEach(contentDetail => {
        if (!contentDetail.loaded) {
          window.setTimeout(() => {
            try {
              if ($(`#${contentDetail.id}`).length) {
                if (!this.vueChildInstances[contentDetail.id]) {
                  const VueHamburgerMenuItem = Vue.extend(contentDetail.vueComponent);
                  this.vueChildInstances[contentDetail.id] = new VueHamburgerMenuItem({
                    i18n: new VueI18n({
                      locale: this.$i18n.locale,
                      messages: this.$i18n.messages,
                    }),
                    vuetify,
                    el: `#${contentDetail.id}`,
                  });
                  this.vueChildInstances[contentDetail.id].$on('open-second-level', () => {
                    this.openSecondLevel(contentDetail);
                  });
                }
              }
            } finally {
              contentDetail.loaded = true;
              this.initializing --;
            }
          }, this.idleTime);
        }
      });
    },
    openSecondLevel(contentDetail) {
      if (!contentDetail.secondLevel || !this.vueChildInstances[contentDetail.id]) {
        return;
      }
      this.secondLevel = true;

      if (this.openedSecondLevel !== contentDetail.id && this.vueChildInstances[contentDetail.id].mountSecondLevel) {
        this.openedSecondLevel = contentDetail.id;
        this.vueChildInstances[contentDetail.id].mountSecondLevel('.HamburgerMenuSecondLevelParent > div');
      }
    },
    hideSecondLevel() {
      this.secondLevel = false;
    },
    openOrHideMenu() {
      this.hamburgerMenu = !this.hamburgerMenu;
    },
  },
};
</script>