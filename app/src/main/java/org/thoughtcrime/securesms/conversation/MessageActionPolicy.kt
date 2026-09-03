/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.conversation

import org.thoughtcrime.securesms.recipients.Recipient

/**
 * Policy for determining which message context actions should be available.
 *
 * This singleton evaluates message state and user permissions to filter available actions
 * from the complete set of possible [MessageContextAction]s. It acts as a gatekeeper,
 * ensuring only valid actions are presented to users based on:
 * - Message type and state (failed, pending, polling, etc.)
 * - User permissions (can edit group info, is primary device, etc.)
 * - UI state (action mode active, multi-select mode, etc.)
 * - Special message types (payment notifications, announcements, etc.)
 */
object MessageActionPolicy {

  /**
   * Returns the list of available context actions for a message based on its state and context.
   *
   * Actions are filtered based on:
   * - Whether action mode or multi-select is active (returns empty if true)
   * - Message properties validated by [MenuState]
   * - User permissions and device state
   *
   * @param context Contains message, recipient, and permission information needed to evaluate actions
   * @return List of available [MessageContextAction]s, ordered for presentation to users
   */
  @JvmStatic
  fun availableActions(context: MessageActionPolicyContext): List<MessageContextAction> {
    if (context.isActionModeStarted || context.hasSelection) {
      return emptyList()
    }

    val menuState = getMenuState(context)
    val actions = mutableListOf<MessageContextAction>()

    if (menuState.shouldShowReplyAction()) {
      actions += MessageContextAction.REPLY
    }

    if (menuState.shouldShowEditAction()) {
      actions += MessageContextAction.EDIT
    }

    if (menuState.shouldShowForwardAction()) {
      actions += MessageContextAction.FORWARD
    }

    if (menuState.shouldShowResendAction()) {
      actions += MessageContextAction.RESEND
    }

    if (menuState.shouldShowSaveAttachmentAction()) {
      actions += MessageContextAction.SAVE
    }

    if (menuState.shouldShowCopyAction()) {
      actions += MessageContextAction.COPY
    }

    if (menuState.shouldShowPaymentDetails()) {
      actions += MessageContextAction.PAYMENT_DETAILS
    }

    actions += MessageContextAction.MULTI_SELECT

    if (menuState.shouldShowDetailsAction()) {
      actions += MessageContextAction.VIEW_INFO
    }

    if (menuState.shouldShowPollTerminateAction()) {
      actions += MessageContextAction.END_POLL
    }

    if (menuState.shouldShowPinMessage()) {
      actions += MessageContextAction.PIN
    }

    if (menuState.shouldShowUnpinMessage()) {
      actions += MessageContextAction.UNPIN
    }

    if (menuState.shouldShowStarMessage()) {
      actions += MessageContextAction.STAR
    }

    if (menuState.shouldShowUnstarMessage()) {
      actions += MessageContextAction.UNSTAR
    }

    actions += MessageContextAction.DELETE

    return actions
  }

  /**
   * Determines whether reaction controls should be shown for a message.
   *
   * Reactions are evaluated separately from [availableActions] because they use a different
   * UI pattern (emoji picker overlay) and accessibility mechanism, rather than being exposed
   * as context menu actions. This allows reactions to be handled through gesture-based
   * interactions while context actions use the accessibility action framework.
   *
   * @param context Contains message and permission information
   * @return true if reactions should be displayed, false if hidden due to UI state or message type
   */
  @JvmStatic
  fun shouldShowReactions(context: MessageActionPolicyContext): Boolean {
    // Reactions are not exposed as accessibility actions (see availableActions()) but are handled
    // through a separate UI mechanism (emoji picker overlay) with different accessibility patterns
    if (context.isActionModeStarted || context.hasSelection) {
      return false
    }

    return getMenuState(context).shouldShowReactions()
  }

  private fun getMenuState(context: MessageActionPolicyContext): MenuState {
    return MenuState.getMenuState(
      context.recipient,
      context.conversationMessage.multiselectCollection.toSet(),
      context.shouldShowMessageRequest,
      context.isNonAdminInAnnouncementGroup,
      context.canEditGroupInfo
    )
  }
}

/**
 * Encapsulates all context needed to evaluate which message actions should be available.
 *
 * This data class serves as the input to [MessageActionPolicy] decision functions,
 * aggregating message properties, recipient state, and UI/permission context in one place.
 *
 * @param recipient The conversation recipient (1-on-1 contact or group)
 * @param conversationMessage The message to evaluate actions for
 * @param shouldShowMessageRequest Whether the conversation is in message request state
 * @param isNonAdminInAnnouncementGroup Whether user is non-admin in an announcement-only group
 * @param canEditGroupInfo Whether the user has permission to edit group info (pin/unpin)
 * @param isActionModeStarted Whether selection mode is currently active
 * @param hasSelection Whether any messages are currently multi-selected
 */
data class MessageActionPolicyContext(
  val recipient: Recipient,
  val conversationMessage: ConversationMessage,
  val shouldShowMessageRequest: Boolean,
  val isNonAdminInAnnouncementGroup: Boolean,
  val canEditGroupInfo: Boolean,
  val isActionModeStarted: Boolean,
  val hasSelection: Boolean
)
