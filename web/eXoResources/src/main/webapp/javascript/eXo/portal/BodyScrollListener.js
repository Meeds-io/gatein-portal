$(document).ready(() => {
  // Avoid applying margin on Mobile view
  if (window.innerWidth <= 768) {
    return;
  }
  function controlBodyScrollClass() {
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

  // Add a style to define scrollbar width
  const scrollbarWidth = parseInt(window.innerWidth - document.body.offsetWidth);
  const styleElement = document.createElement('style');
  styleElement.textContent = `
  .hide-scroll.with-scroll {
    margin-right: ${scrollbarWidth}px !important;
  }`;
  styleElement.setAttribute('type', 'text/css');
  document.head.append(styleElement);
})
