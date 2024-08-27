package io.antmedia.webrtc_android_sample_app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static io.antmedia.webrtc_android_sample_app.TestableActivity.CONFERENCE_ROOM_ID_FOR_TEST;


import android.content.Intent;
import android.util.Log;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;


import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import java.io.IOException;

import io.antmedia.webrtc_android_sample_app.advanced.ConferenceActivityWithDifferentVideoSources;
import io.antmedia.webrtc_android_sample_app.basic.ConferenceActivity;
import io.antmedia.webrtcandroidframework.core.PermissionHandler;


/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ConferenceActivityWithDifferentVideoSourcesTest {
    private final String SCREEN_SHARE_PERMISSION_DIALOG_START_NOW_TEXT ="Start now";

    private float videoBytesSent = 0;

    @Rule
    public GrantPermissionRule permissionRule
            = GrantPermissionRule.grant(PermissionHandler.FULL_PERMISSIONS);

    private IdlingResource mIdlingResource;

    @Rule
    public ActivityScenarioRule<ConferenceActivityWithDifferentVideoSources> conferenceActivityWithDifferentVideoSourcesScenarioRule = new ActivityScenarioRule<>(ConferenceActivityWithDifferentVideoSources.class);

    private String runningTest;
    private String roomName;

    @Before
    public void before() {
        //try before method to make @Rule run properly
        getInstrumentation().waitForIdleSync();

        roomName = CONFERENCE_ROOM_ID_FOR_TEST;
    }

    @After
    public void after() {
        System.out.println("after test");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    @Rule
    public TestWatcher watchman= new TestWatcher() {

        @Override
        protected void failed(Throwable e, Description description) {
            Log.i("TestWatcher", "*** "+description + " failed!\n");
        }

        @Override
        protected void succeeded(Description description) {
            Log.i("TestWatcher", "*** "+description + " succeeded!\n");
        }

        protected void starting(Description description) {
            Log.i("TestWatcher", "******\n*** "+description + " starting!\n");
            runningTest = description.toString();
        }

        protected void finished(Description description) {
            Log.i("TestWatcher", "*** "+description + " finished!\n******\n");
        }
    };

    @Test
    public void testConferenceSwitchStreamSource() throws InterruptedException, UiObjectNotFoundException {
        conferenceActivityWithDifferentVideoSourcesScenarioRule.getScenario().onActivity(activity -> {
            mIdlingResource = activity.getIdlingResource();
            IdlingRegistry.getInstance().register(mIdlingResource);
            activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        });

        UiDevice device = UiDevice.getInstance(getInstrumentation());

        onView(withId(R.id.join_conference_button)).check(matches(withText("Join Conference")));
        onView(withId(R.id.join_conference_button)).perform(click());

        Thread.sleep(5000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.live)));

        onView(withId(R.id.show_stats_button)).perform(click());

        Thread.sleep(3000);
        onView(withId(R.id.multitrack_stats_popup_bytes_sent_video_textview)).check((view, noViewFoundException) -> {
            String text = ((TextView) view).getText().toString();
            float value = Float.parseFloat(text);
            assertTrue(value > 0f);
            videoBytesSent = value;
        });

        onView(withId(R.id. stats_popup_container)).perform(swipeUp());

        Thread.sleep(3000);

        onView(withId(R.id.multitrack_stats_popup_close_button)).perform(click());

        Thread.sleep(3000);

        onView(withId(R.id.screen_share_button)).perform(click());

        UiObject2 button2= device.wait(Until.findObject(By.text(SCREEN_SHARE_PERMISSION_DIALOG_START_NOW_TEXT)), 100000);
        assertNotNull(button2);
        button2.click();

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.live)));

        onView(withId(R.id.show_stats_button)).perform(click());

        Thread.sleep(3000);

        onView(withId(R.id.multitrack_stats_popup_bytes_sent_video_textview)).check((view, noViewFoundException) -> {
            String text = ((TextView) view).getText().toString();
            float value = Float.parseFloat(text);
            assertTrue( value > 0);
            assertTrue( value != videoBytesSent);
            videoBytesSent = value;

        });

        onView(withId(R.id. stats_popup_container)).perform(swipeUp());

        Thread.sleep(3000);

        onView(withId(R.id.multitrack_stats_popup_close_button)).perform(click());

        Thread.sleep(3000);


        onView(withId(R.id.front_camera_button)).perform(click());

        Thread.sleep(3000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.live)));

        onView(withId(R.id.show_stats_button)).perform(click());

        Thread.sleep(3000);

        onView(withId(R.id.multitrack_stats_popup_bytes_sent_video_textview)).check((view, noViewFoundException) -> {
            String text = ((TextView) view).getText().toString();
            float value = Float.parseFloat(text);
            assertTrue( value > 0);
            assertTrue( value != videoBytesSent);
            videoBytesSent = value;

        });

        onView(withId(R.id. stats_popup_container)).perform(swipeUp());

        Thread.sleep(3000);

        onView(withId(R.id.multitrack_stats_popup_close_button)).perform(click());

        Thread.sleep(3000);

        onView(withId(R.id.rear_camera_button)).perform(click());

        Thread.sleep(3000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.live)));

        onView(withId(R.id.show_stats_button)).perform(click());

        Thread.sleep(3000);

        //after source switch video sending should continue.
        onView(withId(R.id.multitrack_stats_popup_bytes_sent_video_textview)).check((view, noViewFoundException) -> {
            String text = ((TextView) view).getText().toString();
            float value = Float.parseFloat(text);
            assertTrue( value > 0);
            assertTrue( value != videoBytesSent);
        });

        onView(withId(R.id. stats_popup_container)).perform(swipeUp());

        Thread.sleep(3000);

        onView(withId(R.id.multitrack_stats_popup_close_button)).perform(click());

        Thread.sleep(3000);

        onView(withId(R.id.join_conference_button)).perform(click());

        Thread.sleep(5000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.disconnected)));

        onView(withId(R.id.front_camera_button)).perform(click());

        onView(withId(R.id.join_conference_button)).perform(click());

        Thread.sleep(3000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.live)));

        Thread.sleep(3000);

        onView(withId(R.id.show_stats_button)).perform(click());

        Thread.sleep(3000);

        //after source switch video sending should continue.
        onView(withId(R.id.multitrack_stats_popup_bytes_sent_video_textview)).check((view, noViewFoundException) -> {
            String text = ((TextView) view).getText().toString();
            float value = Float.parseFloat(text);
            assertTrue( value > 0);
            assertTrue( value != videoBytesSent);
        });

        onView(withId(R.id. stats_popup_container)).perform(swipeUp());

        Thread.sleep(3000);

        onView(withId(R.id.multitrack_stats_popup_close_button)).perform(click());

        onView(withId(R.id.join_conference_button)).perform(click());

        Thread.sleep(5000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.disconnected)));
    }

}