<template>
  <v-container 
    id="SiteHamburgerNavigation"
    px-0
    py-0
    class="white">
    <v-row
      class="mx-0">
      <v-list 
        shaped 
        dense 
        min-width="90%"
        class="pb-0">
        <v-list-item-group v-model="selectedNavigationIndex">
          <v-list-item
            v-for="nav in navigations"
            :key="nav.uri"
            :input-value="nav.selected"
            :active="nav.selected"
            :selected="nav.selected"
            :href="`${BASE_SITE_URI}${nav.uri}`"
            link >
            <v-list-item-icon class="mr-6 my-2">
              <i :class="nav.iconClass"></i>
            </v-list-item-icon>
            <v-list-item-content>
              <v-list-item-title
                class="subtitle-2"
                v-text="nav.label" >
              </v-list-item-title>
            </v-list-item-content>
          </v-list-item>
        </v-list-item-group>
      </v-list>
    </v-row>
  </v-container>
</template>
<script>

export default {
  data: () => ({
    BASE_SITE_URI: `${eXo.env.portal.context}/${eXo.env.portal.portalName}/`,
    navigationScope: 'children',
    navigationVisibilities: ['displayed'],
    navigations: [],
  }),
  computed:{
    visibilityQueryParams() {
      return this.navigationVisibilities.map(visibilityName => `visibility=${visibilityName}`).join('&');
    },
    selectedNavigationIndex() {
      return this.navigations.findIndex(nav => nav.uri === eXo.env.portal.selectedNodeUri);
    },
  },
  watch: {
    navigations() {
      this.navigations.forEach(nav => {
        const capitilizedName = `${nav.name[0].toUpperCase()}${nav.name.slice(1)}`;
        nav.iconClass = `uiIcon uiIconFile uiIconToolbarNavItem uiIcon${capitilizedName} icon${capitilizedName} ${nav.icon}`;
      });
    },
  },
  created(){
    fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/v1/navigations/portal/${eXo.env.portal.portalName}?scope=${this.navigationScope}&${this.visibilityQueryParams}`)
      .then(resp => resp && resp.ok && resp.json())
      .then(data => this.navigations = data);
  },
};
</script>

