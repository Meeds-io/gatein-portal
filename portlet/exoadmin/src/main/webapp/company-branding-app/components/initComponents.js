import ExoModal from './modal/ExoModal.vue';
import ExoColorPicker from './modal/ExoColorPicker.vue';
import ExoCompanyBranding from './ExoCompanyBranding.vue';

const components = {
  'exo-company-branding': ExoCompanyBranding,
  'exo-color-picker': ExoColorPicker,
  'exo-modal' : ExoModal
};

for(const key in components) {
  Vue.component(key, components[key]);
}