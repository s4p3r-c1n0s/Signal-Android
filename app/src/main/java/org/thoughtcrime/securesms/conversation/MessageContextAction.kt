/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.conversation

import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import org.thoughtcrime.securesms.R
import org.signal.core.ui.R as CoreUiR

/**
 * Defines all available context menu actions for messages in conversations.
 *
 * Each action is mapped to:
 * - An accessibility action ID for accessibility services to reference
 * - A localized label string resource
 * - An icon drawable resource
 *
 * Actions are conditionally displayed based on message state and permissions via [MenuState].
 * The [MessageActionPolicy] determines which actions are available for a given message context.
 */
enum class MessageContextAction(
  /**
   * Unique accessibility action ID registered with Android's accessibility framework.
   * Used by accessibility services to identify and invoke specific actions.
   */
  @IdRes val accessibilityActionId: Int,
  /**
   * String resource ID for the user-visible label of this action.
   * Used in menus, buttons, and accessibility announcements.
   */
  @StringRes val labelRes: Int,
  /**
   * Drawable resource ID for the action's icon.
   * Used in context menus and toolbars to visually represent the action.
   */
  @DrawableRes val iconRes: Int
) {
  REPLY(
    accessibilityActionId = R.id.conversation_message_accessibility_reply_action,
    labelRes = R.string.conversation_selection__menu_reply,
    iconRes = R.drawable.symbol_reply_24
  ),
  EDIT(
    accessibilityActionId = R.id.conversation_message_accessibility_edit_action,
    labelRes = R.string.conversation_selection__menu_edit,
    iconRes = CoreUiR.drawable.symbol_edit_24
  ),
  FORWARD(
    accessibilityActionId = R.id.conversation_message_accessibility_forward_action,
    labelRes = R.string.conversation_selection__menu_forward,
    iconRes = CoreUiR.drawable.symbol_forward_24
  ),
  RESEND(
    accessibilityActionId = R.id.conversation_message_accessibility_resend_action,
    labelRes = R.string.conversation_selection__menu_resend_message,
    iconRes = R.drawable.symbol_refresh_24
  ),
  SAVE(
    accessibilityActionId = R.id.conversation_message_accessibility_save_action,
    labelRes = R.string.conversation_selection__menu_save,
    iconRes = CoreUiR.drawable.symbol_save_android_24
  ),
  COPY(
    accessibilityActionId = R.id.conversation_message_accessibility_copy_action,
    labelRes = R.string.conversation_selection__menu_copy,
    iconRes = CoreUiR.drawable.symbol_copy_android_24
  ),
  PAYMENT_DETAILS(
    accessibilityActionId = R.id.conversation_message_accessibility_payment_details_action,
    labelRes = R.string.conversation_selection__menu_payment_details,
    iconRes = R.drawable.symbol_payment_24
  ),
  MULTI_SELECT(
    accessibilityActionId = R.id.conversation_message_accessibility_multiselect_action,
    labelRes = R.string.conversation_selection__menu_multi_select,
    iconRes = CoreUiR.drawable.symbol_check_circle_24
  ),
  VIEW_INFO(
    accessibilityActionId = R.id.conversation_message_accessibility_view_info_action,
    labelRes = R.string.conversation_selection__menu_message_details,
    iconRes = CoreUiR.drawable.symbol_info_24
  ),
  END_POLL(
    accessibilityActionId = R.id.conversation_message_accessibility_end_poll_action,
    labelRes = R.string.conversation_selection__menu_end_poll,
    iconRes = R.drawable.symbol_stop_24
  ),
  DELETE(
    accessibilityActionId = R.id.conversation_message_accessibility_delete_action,
    labelRes = R.string.conversation_selection__menu_delete,
    iconRes = CoreUiR.drawable.symbol_trash_24
  ),
  PIN(
    accessibilityActionId = R.id.conversation_message_accessibility_pin_action,
    labelRes = R.string.conversation_selection__menu_pin_message,
    iconRes = R.drawable.symbol_pin_24
  ),
  UNPIN(
    accessibilityActionId = R.id.conversation_message_accessibility_unpin_action,
    labelRes = R.string.conversation_selection__menu_unpin_message,
    iconRes = R.drawable.symbol_pin_slash_24
  ),
  STAR(
    accessibilityActionId = R.id.conversation_message_accessibility_star_action,
    labelRes = R.string.conversation_selection__menu_star,
    iconRes = R.drawable.symbol_star_outline_24
  ),
  UNSTAR(
    accessibilityActionId = R.id.conversation_message_accessibility_unstar_action,
    labelRes = R.string.conversation_selection__menu_unstar,
    iconRes = R.drawable.symbol_star_outline_24
  );

  companion object {
    private val BY_ACCESSIBILITY_ID: Map<Int, MessageContextAction> =
      values().associateBy(MessageContextAction::accessibilityActionId)

    /**
     * Looks up a [MessageContextAction] by its accessibility action ID.
     *
     * This is used when accessibility services dispatch custom actions to map the
     * numeric action ID back to the corresponding enum constant.
     *
     * @param actionId The accessibility action ID to look up
     * @return The matching [MessageContextAction], or null if not found
     */
    @JvmStatic
    fun fromAccessibilityActionId(actionId: Int): MessageContextAction? {
      return BY_ACCESSIBILITY_ID[actionId]
    }
  }
}
