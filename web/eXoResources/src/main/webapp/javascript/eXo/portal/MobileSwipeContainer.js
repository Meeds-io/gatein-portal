/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
$(document).ready(() => {

  $('.UIMobileSwipe.left').off('click').on('click', () => {
    $('.UIMobileSwipeContainer').trigger('swiperight');
  })

  $('.UIMobileSwipe.right').off('click').on('click', () => {
    $('.UIMobileSwipeContainer').trigger('swipeleft');
  })

  $(window).on( "touchstart", event => {
    const touchStartEvent = event.changedTouches[0];
    $('.UIMobileSwipeContainer').data('touchstart', {
      x: touchStartEvent.clientX,
      y: touchStartEvent.clientY,
    });
  });

  $(window).on( "touchend", event => {
    const touchStart = $('.UIMobileSwipeContainer').data('touchstart');
    if (!touchStart) {
      return;
    }

    const touchEndEvent = event.changedTouches[0];
    const touchEnd = {
      x: touchEndEvent.clientX,
      y: touchEndEvent.clientY,
    };

    const minWidthToTriggerEvent = 10;

    const offsetX = touchEnd.x - touchStart.x;
    const offsetY = touchEnd.y - touchStart.y;
    if (Math.abs(offsetY) < Math.abs(offsetX) * 0.5) {
      if (touchEnd.x < (touchStart.x - minWidthToTriggerEvent)) {
        $('.UIMobileSwipeContainer').trigger('swipeleft');

        event.preventDefault();
        event.stopPropagation();
      } else if(touchEnd.x > (touchStart.x + minWidthToTriggerEvent)) {
        $('.UIMobileSwipeContainer').trigger('swiperight');

        event.preventDefault();
        event.stopPropagation();
      }
    }
  });

  $('.UIMobileSwipeContainer').on('swipeleft', () => {
    const $children = $('.UIMobileSwipeContainer .UIMobileSwipeChildContainer');
    if ($children.length < 2) {
      return;
    }

    const $active = $('.UIMobileSwipeContainer .UIMobileSwipeChildContainer.active');
    if (!$active.length) {
      $('.UIMobileSwipeContainer .UIMobileSwipeChildContainer').first().addClass('active');
      return;
    }

    const $next = $active.next();
    if (!$next.length) {
      return;
    }

    $active.removeClass('active');
    $next.addClass('active');

    resetScroll();
    checkArrowsToDisplay();
  });

  $('.UIMobileSwipeContainer').on('swiperight', () => {
    if ($('.UIMobileSwipeContainer .UIMobileSwipeChildContainer').length < 2) {
      return;
    }

    const $active = $('.UIMobileSwipeContainer .UIMobileSwipeChildContainer.active');
    if (!$active.length) {
      $('.UIMobileSwipeContainer .UIMobileSwipeChildContainer').first().addClass('active');
      return;
    }

    const $next = $active.prev();
    if (!$next.length) {
      return;
    }

    $active.removeClass('active');
    $next.addClass('active');

    resetScroll();
    checkArrowsToDisplay();
  });

  function resetScroll() {
    $(window).scrollTop(0);
  }

  function checkArrowsToDisplay() {
    const $active = $('.UIMobileSwipeContainer .UIMobileSwipeChildContainer.active');
    if (!$active.length) {
      return;
    }

    if ($active.next().length && $active.next().hasClass('UIMobileSwipeChildContainer')) {
      $('.UIMobileSwipe.right').removeClass('hidden');
    } else {
      $('.UIMobileSwipe.right').addClass('hidden');
    }

    if ($active.prev().length && $active.prev().hasClass('UIMobileSwipeChildContainer')) {
      $('.UIMobileSwipe.left').removeClass('hidden');
    } else {
      $('.UIMobileSwipe.left').addClass('hidden');
    }
  }

});