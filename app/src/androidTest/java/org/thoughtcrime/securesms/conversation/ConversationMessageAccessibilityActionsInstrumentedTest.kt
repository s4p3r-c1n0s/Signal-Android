/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.conversation

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.thoughtcrime.securesms.MainActivity
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.database.MessageType
import org.thoughtcrime.securesms.database.SignalDatabase
import org.thoughtcrime.securesms.database.model.StoryType
import org.thoughtcrime.securesms.mms.IncomingMessage
import org.thoughtcrime.securesms.recipients.Recipient
import org.thoughtcrime.securesms.testing.SignalActivityRule
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class ConversationMessageAccessibilityActionsInstrumentedTest {

  @Rule
  @JvmField
  val harness = SignalActivityRule(1, false)

  @Test
  fun liveMessage_exposesAccessibilityActions_andMultiSelectActionStartsActionMode() {
    val other = Recipient.resolved(harness.others.first())
    insertIncomingText(other, "conversation message accessibility test")

    val scenario: ActivityScenario<MainActivity> = ActivityScenario.launch(Intent(harness.context, MainActivity::class.java))

    try {
      assertTrue(openFirstConversationRow(scenario, 15_000))
      assertTrue(waitForAction(scenario, R.id.conversation_message_accessibility_reply_action, 15_000))

      assertEquals(
        harness.context.getString(R.string.conversation_selection__menu_reply),
        getActionLabel(scenario, R.id.conversation_message_accessibility_reply_action)
      )
      assertEquals(
        harness.context.getString(R.string.conversation_selection__menu_multi_select),
        getActionLabel(scenario, R.id.conversation_message_accessibility_multiselect_action)
      )
      assertEquals(
        harness.context.getString(R.string.conversation_selection__menu_delete),
        getActionLabel(scenario, R.id.conversation_message_accessibility_delete_action)
      )

      assertTrue(performAction(scenario, R.id.conversation_message_accessibility_multiselect_action))
      assertTrue(waitForViewVisible(scenario, R.id.action_mode_top_bar, 5_000))
      assertFalse(waitForAction(scenario, R.id.conversation_message_accessibility_multiselect_action, 1_500))
    } finally {
      scenario.close()
    }
  }

  private fun insertIncomingText(other: Recipient, body: String): Long {
    val now = System.currentTimeMillis()
    val message = IncomingMessage(
      MessageType.NORMAL,
      other.id,
      now,
      now,
      now,
      null,
      null,
      body,
      StoryType.NONE,
      null,
      false,
      -1,
      0,
      null,
      false,
      false,
      null,
      null,
      Collections.emptyList(),
      Collections.emptyList(),
      Collections.emptyList(),
      Collections.emptyList(),
      null,
      null,
      false,
      null
    )

    val threadId = SignalDatabase.threads.getOrCreateThreadIdFor(other)
    try {
      SignalDatabase.messages.insertMessageInbox(message, threadId).get()
    } catch (e: Exception) {
      throw AssertionError("Failed to insert incoming message for test setup", e)
    }
    return threadId
  }

  companion object {
    private fun openFirstConversationRow(scenario: ActivityScenario<MainActivity>, timeoutMs: Long): Boolean {
      val deadline = SystemClock.uptimeMillis() + timeoutMs

      while (SystemClock.uptimeMillis() < deadline) {
        val opened = AtomicBoolean(false)

        scenario.onActivity { activity ->
          val conversationRecycler = activity.findViewById<RecyclerView>(R.id.conversation_item_recycler)
          if (conversationRecycler != null && conversationRecycler.isShown) {
            opened.set(true)
            return@onActivity
          }

          val list = activity.findViewById<RecyclerView>(R.id.list)
          if (list != null && list.childCount > 0) {
            val row = list.getChildAt(0)
            if (row != null) {
              row.performClick()
            }
          }
        }

        if (opened.get()) {
          return true
        }

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        SystemClock.sleep(100)
      }

      return false
    }

    private fun waitForAction(scenario: ActivityScenario<MainActivity>, actionId: Int, timeoutMs: Long): Boolean {
      val deadline = SystemClock.uptimeMillis() + timeoutMs

      while (SystemClock.uptimeMillis() < deadline) {
        val found = AtomicBoolean(false)

        scenario.onActivity { activity ->
          val recycler = activity.findViewById<RecyclerView>(R.id.conversation_item_recycler)
          found.set(findChildWithAction(recycler, actionId) != null)
        }

        if (found.get()) {
          return true
        }

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        SystemClock.sleep(100)
      }

      return false
    }

    private fun getActionLabel(scenario: ActivityScenario<MainActivity>, actionId: Int): String? {
      val label = AtomicReference<String?>()

      scenario.onActivity { activity ->
        val recycler = activity.findViewById<RecyclerView>(R.id.conversation_item_recycler)
        val child = findChildWithAction(recycler, actionId)
        if (child == null) {
          return@onActivity
        }

        val nodeInfo = child.createAccessibilityNodeInfo()
        try {
          val action = findActionById(nodeInfo, actionId)
          label.set(action?.label?.toString())
        } finally {
          nodeInfo.recycle()
        }
      }

      return label.get()
    }

    private fun performAction(scenario: ActivityScenario<MainActivity>, actionId: Int): Boolean {
      val performed = AtomicBoolean(false)

      scenario.onActivity { activity ->
        val recycler = activity.findViewById<RecyclerView>(R.id.conversation_item_recycler)
        val child = findChildWithAction(recycler, actionId)
        if (child != null) {
          performed.set(child.performAccessibilityAction(actionId, Bundle.EMPTY))
        }
      }

      return performed.get()
    }

    private fun waitForViewVisible(scenario: ActivityScenario<MainActivity>, viewId: Int, timeoutMs: Long): Boolean {
      val deadline = SystemClock.uptimeMillis() + timeoutMs

      while (SystemClock.uptimeMillis() < deadline) {
        val visible = AtomicBoolean(false)

        scenario.onActivity { activity ->
          val view = activity.findViewById<View>(viewId)
          visible.set(view != null && view.isShown)
        }

        if (visible.get()) {
          return true
        }

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        SystemClock.sleep(100)
      }

      return false
    }

    private fun findChildWithAction(recycler: RecyclerView?, actionId: Int): View? {
      if (recycler == null) {
        return null
      }

      for (index in 0 until recycler.childCount) {
        val child = recycler.getChildAt(index)
        val nodeInfo = child.createAccessibilityNodeInfo()
        try {
          if (findActionById(nodeInfo, actionId) != null) {
            return child
          }
        } finally {
          nodeInfo.recycle()
        }
      }

      return null
    }

    private fun findActionById(info: AccessibilityNodeInfo, actionId: Int): AccessibilityNodeInfo.AccessibilityAction? {
      val actionList = info.actionList ?: return null

      for (action in actionList) {
        if (action.id == actionId) {
          return action
        }
      }

      return null
    }
  }
}
