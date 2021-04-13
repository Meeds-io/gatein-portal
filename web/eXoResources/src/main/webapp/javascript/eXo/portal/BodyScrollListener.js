$(document).ready(() => {
  // Avoid applying margin on Mobile view
  if (window.innerWidth <= 768) {
    return;
  }
  function controlBodyScrollClass() {
    // Add a style to define scrollbar width
    let styleElement;
    if ($('#bodyScrollStyle').length) {
      styleElement = $('#bodyScrollStyle')[0];
    } else {
      styleElement = document.createElement('style');
      styleElement.id = 'bodyScrollStyle';
      styleElement.setAttribute('type', 'text/css');
      document.head.append(styleElement);
    }

    if (window.scrollbars.visible) {
      const scrollbarWidth = parseFloat(window.innerWidth - document.body.getBoundingClientRect().width);
      const isScrollBarOnLeft = document.body.getBoundingClientRect().left > 0;
      if (isScrollBarOnLeft) {
        styleElement.textContent = `
          @media (min-width: 768px) {
            .hide-scroll.with-scroll {
              margin-left: ${scrollbarWidth}px !important;
            }
            .hide-scroll.with-scroll #UITopBarContainer {
              width: calc(100% - ${scrollbarWidth}px);
              right: 0;
            }
          }`;
      } else {
        styleElement.textContent = `
          @media (min-width: 768px) {
            .hide-scroll.with-scroll {
              margin-right: ${scrollbarWidth}px !important;
            }
            .hide-scroll.with-scroll #UITopBarContainer {
              width: calc(100% - ${scrollbarWidth}px);
              left: 0;
            }
          }`;
      }
    }

    if ($('body').height() > ($(window).height() + 1)) {
      $('body').addClass('with-scroll');
      $('body').removeClass('no-scroll');
    } else {
      $('body').removeClass('with-scroll');
      $('body').addClass('no-scroll');
    }
  }
  $(window).resize(controlBodyScrollClass);
  controlBodyScrollClass();
  document.addEventListener('displayTopBarLoading', controlBodyScrollClass);
  document.addEventListener('hideTopBarLoading', controlBodyScrollClass);
})
