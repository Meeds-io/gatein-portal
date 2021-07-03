$(document).ready(installScrollControlListener);

function installScrollControlListener() {
  const $siteBody = $('#UISiteBody');
  if (!$siteBody.data('scroll-control')) {
    $siteBody.data('scroll-control', 'true');
    $siteBody.scroll(controlBodyScrollClass);
    controlBodyScrollClass();
  }
}

function controlBodyScrollClass() {
  const $siteBody = $('#UISiteBody');
  if($siteBody.scrollTop()) {
    if (!$('#UIWorkingWorkspace').hasClass('body-scrolled')) {
      $('#UIWorkingWorkspace').addClass('body-scrolled');
    }
  } else {
    $('#UIWorkingWorkspace').removeClass('body-scrolled');
  }
}