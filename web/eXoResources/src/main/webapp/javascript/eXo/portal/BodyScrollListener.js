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

    window.setTimeout(() => {
      const $linksToHandle = $('a[href]:not([href=""]):not([href="#"]):not([load-handled])');
      $linksToHandle.attr('load-handled', 'true');

      $linksToHandle.click(function(event) {
        const newLocationHref = $(this).attr('href');
        const newTarget = $(this).attr('target');
        if((newLocationHref && (newLocationHref.indexOf('/') === 0 || newLocationHref.indexOf(window.location.origin) === 0)) && (!newTarget || newTarget === '_self')) {
          event.preventDefault();
          event.stopPropagation();

          window.history.replaceState('', window.document.title, newLocationHref);
          fetch(newLocationHref, {
            credentials: 'include',
            method: 'GET'
          })
            .then(resp => {
              if (resp && resp.status == 200) {
                return resp.text();
              }
            })
            .then(htmlContent => {
              const newDocument = $('<html />');
              newDocument.html(htmlContent);

              document.body.innerHTML = '';
              const newBody = htmlContent.substring(htmlContent.search('<body') + htmlContent.match(/<body.*>/g)[0].length, htmlContent.lastIndexOf('</body>'));

              const newHead = htmlContent.substring(htmlContent.search('<head') + htmlContent.match(/<head.*>/g)[0].length, htmlContent.search('</head>'));
              newHead.match(/<link.*id=".*".*>/g).forEach(link => {
                const id = link.match(/id="([^"]*)"/i)[1];
                if (!document.querySelector(`#${id}`)) {
                  $(document.head).append(link);
                }
              });

              document.dispatchEvent(new CustomEvent('portal-page-clear'));

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
              };
              $(document.body).html(newBody);
              window.loadHTMLTime = Date.now();
              window.setTimeout(() => {
                $('body').removeClass('hide-scroll');
                $(window).trigger('resize');
              }, 1000);
            });
        }
      });
    }, 500);
  }

  $(window).resize(controlBodyScrollClass);
  controlBodyScrollClass();
  document.addEventListener('displayTopBarLoading', controlBodyScrollClass);
  document.addEventListener('hideTopBarLoading', controlBodyScrollClass);
})
