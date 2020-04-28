/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
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
