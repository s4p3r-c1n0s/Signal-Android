/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.conversationlist

import android.content.Context
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.conversationlist.model.Conversation

/**
 * Shared policy for conversation list accessibility actions.
 *
 * Centralizes the logic for which actions to expose, their labels, and action dispatch
 * to avoid duplication between ConversationListAdapter and ConversationListSearchModels.
 */
object ConversationListAccessibilityHelper {

  /**
   * Adds the appropriate accessibility actions to a node based on conversation state.
   *
   * Actions exposed (excluding archived rows):
   * - Read/unread toggle
   * - Pin/unpin toggle
   * - Mute/unmute toggle
   *
   * Actions always exposed:
   * - Select (unless in selection mode)
   * - Archive/unarchive toggle
   * - Delete
   *
   * @param info The AccessibilityNodeInfo to populate
   * @param context Android context for resources
   * @param conversation The conversation whose state determines which actions to expose
   * @param isInSelectionMode True if the list is in multi-select mode (suppresses action exposure)
   */
  fun addConversationActions(
    info: AccessibilityNodeInfo,
    context: Context,
    conversation: Conversation,
    isInSelectionMode: Boolean
  ) {
    if (isInSelectionMode) {
      return
    }

    val isArchived = conversation.threadRecord.isArchived

    if (!isArchived) {
      addAction(
        info,
        R.id.conversation_list_accessibility_read_action,
        context.resources.getQuantityString(
          if (conversation.threadRecord.isRead)
            R.plurals.ConversationListFragment_unread_plural
          else
            R.plurals.ConversationListFragment_read_plural,
          1
        )
      )

      addAction(
        info,
        R.id.conversation_list_accessibility_pin_action,
        context.getString(
          if (conversation.threadRecord.isPinned)
            R.string.ConversationListFragment_unpin
          else
            R.string.ConversationListFragment_pin
        )
      )

      addAction(
        info,
        R.id.conversation_list_accessibility_mute_action,
        context.getString(
          if (conversation.threadRecord.recipient.live().get().isMuted)
            R.string.ConversationListFragment_unmute
          else
            R.string.ConversationListFragment_mute
        )
      )
    }

    addAction(
      info,
      R.id.conversation_list_accessibility_select_action,
      context.getString(R.string.ConversationListFragment_select)
    )

    addAction(
      info,
      R.id.conversation_list_accessibility_archive_action,
      context.getString(
        if (isArchived)
          R.string.ConversationListFragment_unarchive
        else
          R.string.ConversationListFragment_archive
      )
    )

    addAction(
      info,
      R.id.conversation_list_accessibility_delete_action,
      context.getString(R.string.ConversationListFragment_delete)
    )
  }

  /**
   * Dispatches an accessibility action to the appropriate handler based on the action ID.
   *
   * Determines the correct action (read/unread, pin/unpin, mute/unmute, etc.) by inspecting
   * the conversation's current state and the action ID, then delegates to the listener.
   *
   * @param actionId The accessibility action ID
   * @param conversation The conversation being acted upon
   * @param listener The callback to invoke with the resolved action
   * @return true if the action was handled, false otherwise
   */
  fun dispatchConversationAction(
    actionId: Int,
    conversation: Conversation,
    listener: ConversationListAdapter.OnConversationClickListener
  ): Boolean {
    return when (actionId) {
      R.id.conversation_list_accessibility_read_action -> {
        listener.onConversationAccessibilityAction(
          conversation,
          if (conversation.threadRecord.isRead)
            ConversationListAdapter.ThreadAccessibilityAction.MARK_AS_UNREAD
          else
            ConversationListAdapter.ThreadAccessibilityAction.MARK_AS_READ
        )
        true
      }

      R.id.conversation_list_accessibility_pin_action -> {
        listener.onConversationAccessibilityAction(
          conversation,
          if (conversation.threadRecord.isPinned)
            ConversationListAdapter.ThreadAccessibilityAction.UNPIN
          else
            ConversationListAdapter.ThreadAccessibilityAction.PIN
        )
        true
      }

      R.id.conversation_list_accessibility_mute_action -> {
        listener.onConversationAccessibilityAction(
          conversation,
          if (conversation.threadRecord.recipient.live().get().isMuted)
            ConversationListAdapter.ThreadAccessibilityAction.UNMUTE
          else
            ConversationListAdapter.ThreadAccessibilityAction.MUTE
        )
        true
      }

      R.id.conversation_list_accessibility_select_action -> {
        listener.onConversationAccessibilityAction(
          conversation,
          ConversationListAdapter.ThreadAccessibilityAction.SELECT
        )
        true
      }

      R.id.conversation_list_accessibility_archive_action -> {
        listener.onConversationAccessibilityAction(
          conversation,
          if (conversation.threadRecord.isArchived)
            ConversationListAdapter.ThreadAccessibilityAction.UNARCHIVE
          else
            ConversationListAdapter.ThreadAccessibilityAction.ARCHIVE
        )
        true
      }

      R.id.conversation_list_accessibility_delete_action -> {
        listener.onConversationAccessibilityAction(
          conversation,
          ConversationListAdapter.ThreadAccessibilityAction.DELETE
        )
        true
      }

      else -> false
    }
  }

  private fun addAction(info: AccessibilityNodeInfo, id: Int, label: String) {
    info.addAction(AccessibilityNodeInfo.AccessibilityAction(id, label))
  }
}
