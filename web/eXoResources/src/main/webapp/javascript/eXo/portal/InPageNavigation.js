(function($) {
  const inPageNavigation = {
    installNewCSS: (newHead, newBody) => {
      newHead.match(/<link.*id=".*".*>/g).forEach(link => {
        const id = link.match(/id="([^"]*)"/i)[1];
        if (!document.querySelector(`#${id}`)) {
          const skinTypeResult = link.match(/skin-type="([a-z]*-skin)"/);
          if (skinTypeResult && skinTypeResult.length === 2) {
            const skinType = skinTypeResult[1];
            const $lastSkinTypeDefinition = $(`[skin-type="${skinType}"]`).last();
            if ($lastSkinTypeDefinition.length) {
              // Install new CSS in the same skin files categories (portal, portlet or custom)
              $lastSkinTypeDefinition.after(link);
              return;
            }
          }
          // Install new CSS from newly downloaded head
          $(document.head).append(link);
        }
      });
      newBody.match(/<link.*id=".*".*>/g).forEach(link => {
        const id = link.match(/id="([^"]*)"/i)[1];
        if (!document.querySelector(`#${id}`)) {
          // Install new CSS from newly downloaded body
          $(document.head).append(link);
        }
        newBody = newBody.replace(link, '');
      });
      return newBody;
    },
    installNewJS: (newHead) => {
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
    },
    handleDownloadedContent: (htmlContent) => {
      const newDocument = $('<html />');
      newDocument.html(htmlContent);

      const newHead = htmlContent.substring(htmlContent.search('<head') + htmlContent.match(/<head.*>/g)[0].length, htmlContent.search('</head>'));
      let newBody = htmlContent.substring(htmlContent.search('<body') + htmlContent.match(/<body.*>/g)[0].length, htmlContent.lastIndexOf('</body>'));

      window.document.head.querySelector('title').innerHTML = $("<div/>").html(newHead.substring(newHead.search('<title>') + 7, newHead.search('</title>'))).text();
      document.removeEventListeners();
      document.body.innerHTML = '';

      newBody = inPageNavigation.installNewCSS(newHead, newBody);
      inPageNavigation.installNewJS(newHead);

      $(document.body).html(newBody);

      window.setTimeout(() => {
        $('body').removeClass('hide-scroll');
        $(window).trigger('resize');
        document.dispatchEvent(new CustomEvent('hideTopBarLoading'));
      }, 1000);
    },
    displayLoadingEffect: () => {
      document.dispatchEvent(new CustomEvent('closeAllDrawers'));
      document.dispatchEvent(new CustomEvent('displayTopBarLoading'));
      $('#UISiteBody').remove();
    },
    handleLinks: () => {
      const $linksToHandle = $('a[href]:not([href=""]):not([href="#"]):not([load-handled])');
      $linksToHandle.attr('load-handled', 'true');

      $linksToHandle.click(function(event) {
        const newLocationHref = $(this).attr('href');
        const newTarget = $(this).attr('target');
        const sameTargetWindow = !newTarget || newTarget === '_self';
        const sameSiteURI = newLocationHref && (newLocationHref.indexOf('/') === 0 || newLocationHref.indexOf(window.location.origin) === 0);

        if (sameTargetWindow && sameSiteURI && !event.ctrlKey && !event.altKey && !event.shiftKey) {
          event.preventDefault();
          event.stopPropagation();

          if (window.handlingLink) {
            return;
          }
          window.handlingLink = true;
          window.history.replaceState('', newLocationHref, newLocationHref);

          inPageNavigation.displayLoadingEffect();
          const oldAssetVersion = eXo.env.client.assetsVersion;
          fetch(newLocationHref, {
            credentials: 'include',
            method: 'GET'
          })
            .then(resp => {
              if (resp && resp.status == 200) {
                return resp.text();
              }
            })
            .then(inPageNavigation.handleDownloadedContent)
            .then(() => {
              // If feature has been disabled or the assets versions has been modified, then reload page
              if (!eXo.env.client.InPageNavigationEnabled || oldAssetVersion !== eXo.env.client.assetsVersion) {
                window.location.reload();
              }
            })
            .finally(() => {
              window.handlingLink = false;
            });
        }
      });
    },
    init: () => {
      document.addEventListener('hideTopBarLoading', () => {
        window.setTimeout(() => {
          inPageNavigation.handleLinks();
        }, 200);
      });
      document.addEventListener('drawerOpened', () => {
        window.setTimeout(() => {
          inPageNavigation.handleLinks();
        }, 500);
      });
      inPageNavigation.handleLinks();
    },
  };
  return inPageNavigation;
})($);