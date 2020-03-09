<template>
  <v-app
    color="transaprent"
    class="HamburgerNavigationMenu"
    flat>
    <a class="HamburgerNavigationMenuLink">
      <div class="px-5 py-3 mt-2" @click="openOrHideMenu()">
        <v-icon size="22">fa-bars</v-icon>
      </div>
    </a>
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
      max-height="100vh">
      <v-row class="fill-height" no-gutters @mouseleave="hideSecondLevel()">
        <v-flex style="min-height:100%;max-width:310px;min-width:310px;">
          <div v-for="contentDetail in contents" :key="contentDetail.id">
            <div :id="contentDetail.id"></div>
          </div>
        </v-flex>
        <v-flex v-show="secondLevel">
          <div id="HamburgerMenuSecondLevel"></div>
        </v-flex>
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
      contents: [],
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
      extensions.sort((a, b) => a - b);
      this.contents = extensions;
      const contentsToLoad = this.contents.filter(contentDetail => !contentDetail.loaded);
      this.initializing = contentsToLoad.length;
      const vuetify = this.vuetify;
      contentsToLoad.forEach(contentDetail => {
        if (!contentDetail.loaded) {
          window.setTimeout(() => {
            try {
              if ($(`#${contentDetail.id}`).length) {
                const VueHamburgerMenuItem = Vue.extend(contentDetail.vueComponent);
                contentDetail.vueComponentInstance = new VueHamburgerMenuItem({
                  i18n: new VueI18n({
                    locale: this.$i18n.locale,
                    messages: this.$i18n.messages,
                  }),
                  vuetify,
                  el: `#${contentDetail.id}`,
                });
                contentDetail.vueComponentInstance.$on('open-second-level', () => {
                  this.openSecondLevel(contentDetail);
                });
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
      if (!contentDetail.secondLevel || !contentDetail.vueComponentInstance) {
        return;
      }
      this.secondLevel = true;
      if (contentDetail.vueComponentInstance.mountSecondLevel) {
        contentDetail.vueComponentInstance.mountSecondLevel('#HamburgerMenuSecondLevel');
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