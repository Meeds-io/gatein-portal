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

    const scrollbarWidth = parseInt(window.innerWidth - document.body.offsetWidth);
    styleElement.textContent = `
    .hide-scroll.with-scroll {
      margin-right: ${scrollbarWidth}px !important;
    }`;

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
})
