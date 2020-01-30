import { createLocalVue, shallowMount } from '@vue/test-utils';
import fetchMock from 'fetch-mock';
import flushPromises from 'flush-promises';
import ExoCompanyBranding from '../../main/webapp/company-branding-app/components/ExoCompanyBranding';

const localVue = createLocalVue();

describe('ExoCompanyBranding.test.js', () => {
  let cmp;

  beforeEach(() => {
    fetchMock.get('/rest/v1/platform/branding', {
      'companyName': 'Default Company Name',
      'topBarTheme': 'Dark',
      'logo': {
        'data': [],
        'size': 123
      }
    });
    fetchMock.put('/rest/v1/platform/branding', 200);
    fetchMock.get('/rest/v1/platform/branding/defaultLogo?defaultLogo=404', 404);

    Object.defineProperty(document.location, 'reload', {
      configurable: true
    });

    cmp = shallowMount(ExoCompanyBranding, {
      localVue,
      mocks: {
        $t: () => {}
      }
    });
  });

  it('should display default company branding information in form', () => {
    // Given
    cmp.vm.branding.companyName = 'My Company';
    cmp.vm.branding.topBarTheme = 'Light';

    // When
    const companyNameInput = cmp.find('#companyNameInput');
    const topBarThemeLightInput = cmp.find('#navigationStyle').find('input[value=Light]');
    const topBarThemeDarkInput = cmp.find('#navigationStyle').find('input[value=Dark]');

    // Then
    expect(companyNameInput.element.value).toBe('My Company');
    expect(topBarThemeLightInput.element.checked).toBe(true);
    expect(topBarThemeDarkInput.element.checked).toBe(false);
  });

  it('should disable Save button when Company name field is empty', () => {
    // Given
    cmp.vm.branding.companyName = ' ';

    // When
    const saveButton = cmp.find('#save');

    // Then
    expect(saveButton.element.disabled).toBe(true);
  });

  it('should display error message when saving with a non PNG image', () => {
    // Given
    document.location.reload = jest.fn();

    cmp.vm.branding.companyName = 'Company';
    cmp.vm.branding.logo.uploadId = 123456;
    cmp.vm.branding.logo.name = 'logo.jpg';
    cmp.vm.branding.logo.size = 1024;

    // When
    cmp.find('#save').trigger('click');

    // Then
    expect(document.location.reload).not.toHaveBeenCalled();
    expect(cmp.vm.branding.logo.uploadId).toBe(null);
    expect(cmp.find('#savenotok').attributes().style).toBe('display: none;');
    expect(cmp.find('#mustpng').attributes().style).toBe('display: block;');
    expect(cmp.find('#toobigfile').attributes().style).toBe('display: none;');
  });

  it('should save form data when clicking on Save button', done => {
    // Given
    document.location.reload = jest.fn();

    cmp.vm.branding.companyName = 'My New Company';
    cmp.vm.branding.topBarTheme = 'Light';

    // When
    cmp.find('#save').trigger('click');

    flushPromises().then(() => {
      // Then
      expect(document.location.reload).toHaveBeenCalled();

      expect(cmp.find('#savenotok').attributes().style).toBe('display: none;');
      expect(cmp.find('#mustpng').attributes().style).toBe('display: none;');
      expect(cmp.find('#toobigfile').attributes().style).toBe('display: none;');

      done();
    });
  });

  afterEach(() => {
    fetchMock.restore();
  });
});