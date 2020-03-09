<template>
  <v-list
    dense
    rounded
    class="adminMenu contentMenuWrapper py-0">
    <v-subheader class="text-uppercase subtitle-2 font-weight-bold">{{ itemTitle }}</v-subheader>
    <v-treeview
      :items="administrationItem"
      dense
      rounded
      hoverable
      class="subItemTitle"
      item-key="name">
      <template slot="label" slot-scope="{ item }">
        <a @click="navigateTo(item.path)">{{ item.name }}</a>
      </template>
    </v-treeview>
  </v-list>
</template>

<script>
export default {
  name: 'AdministrationMenuItemApp',
  props: {
    administrationItem: {
      type: Object,
      default: null
    },
    itemTitle: {
      type: String,
      default: null
    }
  },
  methods: {
    navigateTo(pagelink) {
      location.href=`/portal/g/:platform:${ pagelink }` ;
    }
  }
};
</script>