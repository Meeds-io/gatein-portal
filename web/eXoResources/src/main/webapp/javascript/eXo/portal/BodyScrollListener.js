$(document).ready(() => {
  function controlBodyScrollClass() {
    if ($('body').height() > $(window).height()) {
      $('body').addClass('with-scroll');
      $('body').removeClass('no-scroll');
    } else {
      $('body').removeClass('with-scroll');
      $('body').addClass('no-scroll');
    }
  }
  $(window).resize(controlBodyScrollClass);
  controlBodyScrollClass();
})
