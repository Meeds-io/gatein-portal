import ExoAdministrationHamburgerNavigation from './components/ExoAdministrationHamburgerNavigation.vue';
import ExoAdministrationMenuItem from './components/ExoAdministrationMenuItem.vue';
import ExoAdministrationNavigations from './components/ExoAdministrationNavigations.vue';

const components = {
  'exo-administration-hamburger-menu-navigation': ExoAdministrationHamburgerNavigation,
  'exo-administration-menu-item': ExoAdministrationMenuItem,
  'exo-administration-navigations': ExoAdministrationNavigations,
};

for(const key in components) {
  Vue.component(key, components[key]);
}

if (extensionRegistry) {
  extensionRegistry.registerExtension(
    'exo-hamburger-menu-navigation',
    'exo-hamburger-menu-navigation-items', {
      id: 'HamburgerMenuNavigationAdministration',
      priority: 30,
      secondLevel: true,
      vueComponent: ExoAdministrationHamburgerNavigation,
    },
  );
}
