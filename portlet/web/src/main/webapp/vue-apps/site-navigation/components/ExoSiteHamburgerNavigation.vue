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
        <v-list-item-group v-model="item">
          <v-list-item
            v-for="(item, i) in items"
            :key="i">
            <v-list-item-icon class="mr-6 my-2">
              <i :class="`uiIcon uiIconToolbarNavItem icon${item.text}`"></i>
            </v-list-item-icon>
            <v-list-item-content>
              <v-list-item-title
                class="subtitle-2"
                @click="navigateTo(item.url)"
                v-text="item.text"/>
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
    item: 0,
    items: [
      { text: 'Snapshot', url: '' },
      { text: 'Stream', url: 'stream' },
      { text: 'Spaces', url: 'spaces' },
      { text: 'People', url: 'connexions' },
    ],
  }),
  created(){
    const pageName = window.location.pathname.split(`${eXo.env.portal.context}/${eXo.env.portal.portalName}/`)[1];
    const index = this.items.findIndex(obj => obj.url === pageName);
    this.item = index;
  },
    
  methods: {
    navigateTo(pagelink) {
      location.href=`${eXo.env.portal.context}/${eXo.env.portal.portalName}/${pagelink}`;
    }
  }
};
</script>

