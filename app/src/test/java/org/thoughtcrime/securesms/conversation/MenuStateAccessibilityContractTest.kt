/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.conversation

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.thoughtcrime.securesms.conversation.mutiselect.MultiselectCollection
import org.thoughtcrime.securesms.conversation.mutiselect.MultiselectPart
import org.thoughtcrime.securesms.database.model.MessageRecord
import org.thoughtcrime.securesms.database.model.MmsMessageRecord
import org.thoughtcrime.securesms.keyvalue.AccountValues
import org.thoughtcrime.securesms.keyvalue.LabsValues
import org.thoughtcrime.securesms.keyvalue.SignalStore
import org.thoughtcrime.securesms.polls.PollRecord
import org.thoughtcrime.securesms.recipients.Recipient
import org.thoughtcrime.securesms.util.RemoteConfig

/**
 * Unit test suite for [MenuState] action visibility logic.
 *
 * Verifies that [MenuState.getMenuState] correctly determines which message actions
 * should be available based on message properties, recipient state, and permissions.
 * Covers 29 test scenarios across all action types:
 *
 * - **Core Actions**: Forward, Reply, Details, Copy, Delete, Reactions
 * - **Edit**: Own messages, others' messages, failed messages
 * - **Save Attachment**: MMS vs SMS messages
 * - **Resend**: Failed and pending messages
 * - **Payment Details**: Payment notifications and tombstones
 * - **Polls**: Poll termination by poll owner
 * - **Pin/Unpin**: Group permissions, pin state
 * - **Star/Unstar**: Feature flag and starred state
 * - **Announcement Groups**: Admin vs non-admin restrictions
 * - **Message Requests**: Reply restrictions
 * - **Blocked Senders**: Reply restrictions
 *
 * Uses MockK to mock dependencies and test scenario builders to configure
 * message state without needing actual database or UI interactions.
 */
class MenuStateAccessibilityContractTest {

  @Before
  fun setUp() {
    RemoteConfig.REMOTE_VALUES.clear()
    RemoteConfig.initialized = true

    mockkObject(SignalStore)

    val labs = mockk<LabsValues>(relaxed = true)
    every { labs.starredMessages } returns true
    every { SignalStore.labs } returns labs

    val account = mockk<AccountValues>(relaxed = true)
    every { account.isPrimaryDevice } returns true
    every { SignalStore.account } returns account
  }

  @After
  fun tearDown() {
    RemoteConfig.REMOTE_VALUES.clear()
    RemoteConfig.initialized = false
    unmockkObject(SignalStore)
  }

  //region Core Actions Tests

  @Test
  fun singleMessage_exposesCoreActions() {
    val scenario = buildSingleMessageScenario()
    val menuState = getMenuState(scenario)

    assertTrue(menuState.shouldShowForwardAction())
    assertTrue(menuState.shouldShowReplyAction())
    assertTrue(menuState.shouldShowDetailsAction())
    assertFalse(menuState.shouldShowCopyAction())
    assertTrue(menuState.shouldShowDeleteAction())
    assertTrue(menuState.shouldShowReactions())
    assertFalse(menuState.shouldShowPinMessage())
    assertFalse(menuState.shouldShowUnpinMessage())
  }

  @Test
  fun messageRequest_hidesReply() {
    val scenario = buildSingleMessageScenario(shouldShowMessageRequest = true)
    val menuState = getMenuState(scenario)

    assertFalse(menuState.shouldShowReplyAction())
    assertTrue(menuState.shouldShowForwardAction())
    assertTrue(menuState.shouldShowDeleteAction())
  }

  @Test
  fun blockedSender_hidesReply() {
    val scenario = buildSingleMessageScenario(isSenderBlocked = true)
    val menuState = getMenuState(scenario)

    assertFalse(menuState.shouldShowReplyAction())
    assertTrue(menuState.shouldShowForwardAction())
    assertTrue(menuState.shouldShowDeleteAction())
  }

  //endregion

  //region Edit Action Tests

  @Test
  fun ownMessage_showsEditAction() {
    val scenario = buildSingleMessageScenario(isOwnMessage = true, hasText = true)
    val menuState = getMenuState(scenario)

    assertTrue(menuState.shouldShowEditAction())
  }

  @Test
  fun otherMessage_hidesEditAction() {
    val scenario = buildSingleMessageScenario(isOwnMessage = false, hasText = true)
    val menuState = getMenuState(scenario)

    assertFalse(menuState.shouldShowEditAction())
  }

  @Test
  fun failedMessage_hidesEditAction() {
    val scenario = buildSingleMessageScenario(isFailed = true, hasText = true)
    val menuState = getMenuState(scenario)

    assertFalse(menuState.shouldShowEditAction())
  }

  //endregion

  //region Copy Action Tests

  @Test
  fun messageWithText_showsCopyAction() {
    val scenario = buildSingleMessageScenario(hasText = true)
    val menuState = getMenuState(scenario)

    assertTrue(menuState.shouldShowCopyAction())
  }

  @Test
  fun messageWithoutText_hidesCopyAction() {
    val scenario = buildSingleMessageScenario(hasText = false)
    val menuState = getMenuState(scenario)

    assertFalse(menuState.shouldShowCopyAction())
  }

  //endregion

  //region Save Attachment Tests

  @Test
  fun mmsMessage_showsSaveAttachmentAction() {
    val scenario = buildMmsMessageScenario()
    val menuState = getMenuState(scenario)

    assertTrue(menuState.shouldShowSaveAttachmentAction())
  }

  @Test
  fun smsMessage_hidesSaveAttachmentAction() {
    val scenario = buildSingleMessageScenario(isMms = false)
    val menuState = getMenuState(scenario)

    assertFalse(menuState.shouldShowSaveAttachmentAction())
  }

  //endregion

  //region Resend Action Tests

  @Test
  fun failedMessage_showsResendAction() {
    val scenario = buildSingleMessageScenario(isFailed = true)
    val menuState = getMenuState(scenario)

    assertTrue(menuState.shouldShowResendAction())
  }

  @Test
  fun successfulMessage_hidesResendAction() {
    val scenario = buildSingleMessageScenario(isFailed = false)
    val menuState = getMenuState(scenario)

    assertFalse(menuState.shouldShowResendAction())
  }

  @Test
  fun pendingMessage_hidesResendAction() {
    val scenario = buildSingleMessageScenario(isPending = true)
    val menuState = getMenuState(scenario)

    assertFalse(menuState.shouldShowResendAction())
  }

  //endregion

  //region Payment Details Tests

  @Test
  fun paymentNotification_showsPaymentDetails() {
    val scenario = buildSingleMessageScenario(isPaymentNotification = true)
    val menuState = getMenuState(scenario)

    assertTrue(menuState.shouldShowPaymentDetails())
  }

  @Test
  fun paymentTombstone_showsPaymentDetails() {
    val scenario = buildSingleMessageScenario(isPaymentTombstone = true)
    val menuState = getMenuState(scenario)

    assertTrue(menuState.shouldShowPaymentDetails())
  }

  @Test
  fun regularMessage_hidesPaymentDetails() {
    val scenario = buildSingleMessageScenario(isPaymentNotification = false, isPaymentTombstone = false)
    val menuState = getMenuState(scenario)

    assertFalse(menuState.shouldShowPaymentDetails())
  }

  //endregion

  //region Poll Tests

  @Test
  fun activePoll_showsPollTerminateAction() {
    val scenario = buildMmsMessageScenario(hasActivePoll = true, isOwnMessage = true)
    val menuState = getMenuState(scenario)

    assertTrue(menuState.shouldShowPollTerminateAction())
  }

  @Test
  fun otherUserPoll_hidesPollTerminateAction() {
    val scenario = buildMmsMessageScenario(hasActivePoll = true, isOwnMessage = false)
    val menuState = getMenuState(scenario)

    assertFalse(menuState.shouldShowPollTerminateAction())
  }

  @Test
  fun noPoll_hidesPollTerminateAction() {
    val scenario = buildSingleMessageScenario(hasActivePoll = false)
    val menuState = getMenuState(scenario)

    assertFalse(menuState.shouldShowPollTerminateAction())
  }

  //endregion

  //region Pin Message Tests

  @Test
  fun canEditGroupInfo_showsPinMessage() {
    val scenario = buildSingleMessageScenario(canEditGroupInfo = true)
    val menuState = getMenuState(scenario)

    assertTrue(menuState.shouldShowPinMessage())
  }

  @Test
  fun cannotEditGroupInfo_hidesPinMessage() {
    val scenario = buildSingleMessageScenario(canEditGroupInfo = false)
    val menuState = getMenuState(scenario)

    assertFalse(menuState.shouldShowPinMessage())
  }

  //endregion

  //region Unpin Message Tests

  @Test
  fun canEditGroupInfo_showsUnpinMessage() {
    val scenario = buildSingleMessageScenario(canEditGroupInfo = true, pinnedUntil = 1L)
    val menuState = getMenuState(scenario)

    assertTrue(menuState.shouldShowUnpinMessage())
  }

  @Test
  fun cannotEditGroupInfo_hidesUnpinMessage() {
    val scenario = buildSingleMessageScenario(canEditGroupInfo = false)
    val menuState = getMenuState(scenario)

    assertFalse(menuState.shouldShowUnpinMessage())
  }

  //endregion

  //region Star Message Tests

  @Test
  fun ownMessage_showsStarMessage() {
    val scenario = buildSingleMessageScenario(isOwnMessage = true)
    val menuState = getMenuState(scenario)

    assertTrue(menuState.shouldShowStarMessage())
  }

  @Test
  fun otherMessage_showsStarMessage() {
    val scenario = buildSingleMessageScenario(isOwnMessage = false)
    val menuState = getMenuState(scenario)

    assertTrue(menuState.shouldShowStarMessage())
  }

  //endregion

  //region Unstar Message Tests

  @Test
  fun ownMessage_showsUnstarMessage() {
    val scenario = buildSingleMessageScenario(isOwnMessage = true, isStarred = true)
    val menuState = getMenuState(scenario)

    assertTrue(menuState.shouldShowUnstarMessage())
  }

  @Test
  fun otherMessage_showsUnstarMessage() {
    val scenario = buildSingleMessageScenario(isOwnMessage = false, isStarred = true)
    val menuState = getMenuState(scenario)

    assertTrue(menuState.shouldShowUnstarMessage())
  }

  //endregion

  //region Announcement Group Tests

  @Test
  fun nonAdminInAnnouncementGroup_hidesReply() {
    val scenario = buildSingleMessageScenario(isNonAdminInAnnouncementGroup = true)
    val menuState = getMenuState(scenario)

    assertFalse(menuState.shouldShowReplyAction())
  }

  @Test
  fun adminInAnnouncementGroup_showsReply() {
    val scenario = buildSingleMessageScenario(isNonAdminInAnnouncementGroup = false)
    val menuState = getMenuState(scenario)

    assertTrue(menuState.shouldShowReplyAction())
  }

  //endregion

  private fun getMenuState(scenario: Scenario): MenuState {
    return MenuState.getMenuState(
      scenario.recipient,
      scenario.selectedParts,
      scenario.shouldShowMessageRequest,
      scenario.isNonAdminInAnnouncementGroup,
      scenario.canEditGroupInfo
    )
  }

  private fun buildMmsMessageScenario(
    hasActivePoll: Boolean = false,
    isOwnMessage: Boolean = false
  ): Scenario {
    val sender = mockk<Recipient>(relaxed = true).apply {
      every { isBlocked } returns false
    }

    val recipient = mockk<Recipient>(relaxed = true).apply {
      every { isReleaseNotes } returns false
      every { isGroup } returns false
      every { isActiveGroup } returns true
    }

    val poll: PollRecord? = if (hasActivePoll) {
      mockk<PollRecord>(relaxed = true).apply {
        every { hasEnded } returns false
      }
    } else {
      null
    }

    val slideDeck = mockk<org.thoughtcrime.securesms.mms.SlideDeck>(relaxed = true).apply {
      every { stickerSlide } returns null
    }

    val messageRecord = mockk<MmsMessageRecord>(relaxed = true).apply {
      every { body } returns ""
      every { isInMemoryMessageRecord() } returns false
      every { isUpdate() } returns false
      every { isMms() } returns true
      every { isMmsNotification() } returns false
      every { isViewOnce() } returns false
      every { isRemoteDelete() } returns false
      every { isFailed() } returns false
      every { isPending() } returns false
      every { isSecure() } returns true
      every { isPaymentNotification() } returns false
      every { isPaymentTombstone() } returns false
      every { fromRecipient } returns sender
      every { isOutgoing() } returns isOwnMessage
      every { getPinnedUntil() } returns 0L
      every { isStarred() } returns false
      every { this@apply.poll } returns poll
      every { sharedContacts } returns emptyList()
      every { containsMediaSlide() } returns true
      every { isMediaPending() } returns false
      every { this@apply.slideDeck } returns slideDeck
      every { giftBadge } returns null
    }

    val conversationMessage = mockk<ConversationMessage>(relaxed = true)
    every { conversationMessage.messageRecord } returns messageRecord

    val part = MultiselectPart.Attachments(conversationMessage)
    every { conversationMessage.multiselectCollection } returns MultiselectCollection.Single(part)

    return Scenario(
      recipient = recipient,
      conversationMessage = conversationMessage,
      selectedParts = setOf(part),
      shouldShowMessageRequest = false,
      isNonAdminInAnnouncementGroup = false,
      canEditGroupInfo = false
    )
  }

  private fun buildSingleMessageScenario(
    shouldShowMessageRequest: Boolean = false,
    isNonAdminInAnnouncementGroup: Boolean = false,
    canEditGroupInfo: Boolean = false,
    isSenderBlocked: Boolean = false,
    isOwnMessage: Boolean = false,
    hasText: Boolean = false,
    isMms: Boolean = false,
    isFailed: Boolean = false,
    isPending: Boolean = false,
    isPaymentNotification: Boolean = false,
    isPaymentTombstone: Boolean = false,
    hasActivePoll: Boolean = false,
    pinnedUntil: Long = 0L,
    isStarred: Boolean = false
  ): Scenario {
    val sender = mockk<Recipient>(relaxed = true).apply {
      every { isBlocked } returns isSenderBlocked
    }

    val recipient = mockk<Recipient>(relaxed = true).apply {
      every { isReleaseNotes } returns false
      every { isGroup } returns false
      every { isActiveGroup } returns true
    }

    val toRecipient = mockk<Recipient>(relaxed = true).apply {
      every { isSelf } returns true
      every { isGroup } returns false
      every { isActiveGroup } returns true
    }

    val messageRecord = mockk<MessageRecord>(relaxed = true).apply {
      every { body } returns if (hasText) "Sample message body" else ""
      every { isInMemoryMessageRecord() } returns false
      every { isUpdate() } returns false
      every { isMms() } returns isMms
      every { isViewOnce() } returns false
      every { isRemoteDelete() } returns false
      every { isFailed() } returns isFailed
      every { isPending() } returns isPending
      every { isSecure() } returns true
      every { isPush() } returns true
      every { isPaymentNotification() } returns isPaymentNotification
      every { isPaymentTombstone() } returns isPaymentTombstone
      every { fromRecipient } returns sender
      every { this@apply.toRecipient } returns toRecipient
      every { isOutgoing() } returns isOwnMessage
      every { getPinnedUntil() } returns pinnedUntil
      every { isStarred() } returns isStarred
      every { isEditMessage() } returns false
      every { dateSent } returns System.currentTimeMillis()
      every { revisionNumber } returns 0
    }

    val conversationMessage = mockk<ConversationMessage>(relaxed = true)
    every { conversationMessage.messageRecord } returns messageRecord
    every { conversationMessage.originalMessage } returns messageRecord

    val part = MultiselectPart.Text(conversationMessage)
    every { conversationMessage.multiselectCollection } returns MultiselectCollection.Single(part)

    return Scenario(
      recipient = recipient,
      conversationMessage = conversationMessage,
      selectedParts = setOf(part),
      shouldShowMessageRequest = shouldShowMessageRequest,
      isNonAdminInAnnouncementGroup = isNonAdminInAnnouncementGroup,
      canEditGroupInfo = canEditGroupInfo
    )
  }

  /**
   * Encapsulates a complete test scenario for message action evaluation.
   *
   * Aggregates all mocked dependencies and context needed to evaluate [MenuState]
   * action visibility. Used to pass test configuration to [getMenuState].
   *
   * @param recipient Mocked recipient (1-on-1 contact or group)
   * @param conversationMessage Mocked conversation message wrapper
   * @param selectedParts Set of multiselect parts (e.g., text or attachments)
   * @param shouldShowMessageRequest Whether in message request state
   * @param isNonAdminInAnnouncementGroup Whether user is non-admin in announcement group
   * @param canEditGroupInfo Whether user has group edit permissions
   */
  private data class Scenario(
    val recipient: Recipient,
    val conversationMessage: ConversationMessage,
    val selectedParts: Set<MultiselectPart>,
    val shouldShowMessageRequest: Boolean,
    val isNonAdminInAnnouncementGroup: Boolean,
    val canEditGroupInfo: Boolean
  )
}
