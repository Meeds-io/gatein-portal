$(document).ready(() => {

  function isFeatureEnabled(featureName) {
    return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/v1/features/${featureName}`, {
      method: 'GET',
      credentials: 'include',
    }).then(resp => {
      if (!resp || !resp.ok) {
        throw new Error('Response code indicates a server error', resp);
      } else {
        return resp.text();
      }
    }).then(featureEnabled => featureEnabled === 'true');
  }

  function isEligibleLink(newLocationHref, newTarget) {
    const sameTargetWindow = !newTarget || newTarget === '_self';
    const sameSiteURI = newLocationHref && (newLocationHref.indexOf('/') === 0 || newLocationHref.indexOf(window.location.origin) === 0);
    return sameTargetWindow && sameSiteURI;
  }

  function installNewCSS(newHead, newBody) {
    newHead.match(/<link.*id=".*".*>/g).forEach(link => {
      const id = link.match(/id="([^"]*)"/i)[1];
      if (!document.querySelector(`#${id}`)) {
        console.warn('installNewCSS - newHead', id);
        $(document.head).append(link);
      }
    });
    newBody.match(/<link.*id=".*".*>/g).forEach(link => {
      const id = link.match(/id="([^"]*)"/i)[1];
      if (!document.querySelector(`#${id}`)) {
        console.warn('installNewCSS - newBody', id);
        $(document.head).append(link);
      }
      newBody = newBody.replace(link, '');
    });
    return newBody;
  }

  function installNewJS(newHead) {
    const replacableScriptsIterator = newHead.matchAll(/<script[^>]*id="[^>]*"[^>]*>/g);
    let scriptIteratorElement = replacableScriptsIterator.next().value;
    while (scriptIteratorElement) {
      const script = scriptIteratorElement[0];
      const id = script.match(/id="([^"]*)"/i)[1];
      const scriptContent = newHead.substring(scriptIteratorElement.index, newHead.indexOf('</script>', scriptIteratorElement.index));
      const oldScriptElement = document.querySelector(`#${id}`);
      if (oldScriptElement) {
        oldScriptElement.remove();
      }
      const scriptElement = `${scriptContent}</script>`;
      $(document.head).append(scriptElement);
      scriptIteratorElement = replacableScriptsIterator.next().value;
    }
  }

  function handleDownloadedContent(htmlContent) {
    const newDocument = $('<html />');
    newDocument.html(htmlContent);

    const newHead = htmlContent.substring(htmlContent.search('<head') + htmlContent.match(/<head.*>/g)[0].length, htmlContent.search('</head>'));
    let newBody = htmlContent.substring(htmlContent.search('<body') + htmlContent.match(/<body.*>/g)[0].length, htmlContent.lastIndexOf('</body>'));

    document.body.innerHTML = '';

    newBody = installNewCSS(newHead, newBody);
    installNewJS(newHead);

    $(document.body).html(newBody);
    window.setTimeout(() => {
      $('body').removeClass('hide-scroll');
      $(window).trigger('resize');
      document.dispatchEvent(new CustomEvent('hideTopBarLoading'));
    }, 1000);
  }

  function displayLoadingEffect() {
    document.dispatchEvent(new CustomEvent('closeAllDrawers'));
    document.dispatchEvent(new CustomEvent('displayTopBarLoading'));
    $('#UISiteBody').remove();
  }

  function handleLinks() {
      const $linksToHandle = $('a[href]:not([href=""]):not([href="#"]):not([load-handled])');
      $linksToHandle.attr('load-handled', 'true');

      $linksToHandle.click(function(event) {
        const newLocationHref = $(this).attr('href');
        const newTarget = $(this).attr('target');
        if(isEligibleLink(newLocationHref, newTarget)) {
          event.preventDefault();
          event.stopPropagation();

          if (window.handlingLink) {
            return;
          }
          window.handlingLink = true;

          window.history.replaceState('', window.document.title, newLocationHref);

          displayLoadingEffect();

          fetch(newLocationHref, {
            credentials: 'include',
            method: 'GET'
          })
            .then(resp => {
              if (resp && resp.status == 200) {
                return resp.text();
              }
            })
            .then(handleDownloadedContent)
            .finally(() => window.handlingLink = false);
        }
      });
  }

  isFeatureEnabled('InPageNavigation')
    .then(enabled => {
      if (!enabled) {
        console.debug('InPageNavigation  feature is not enabled, abort !');
        return;
      }
      window.setTimeout(handleLinks, 500);
    });

  document.addEventListener('hideTopBarLoading', handleLinks);
  document.addEventListener('drawerOpened', () => {
    window.setTimeout(() => {
      handleLinks();
    }, 500);
  });
});