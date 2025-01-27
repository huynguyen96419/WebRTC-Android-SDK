package io.antmedia.webrtc_android_sample_app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.antmedia.webrtc_android_sample_app.basic.ScreenCaptureActivity;
import io.antmedia.webrtcandroidframework.core.PermissionsHandler;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ScreenCaptureActivityTest {

    //match
    private static final String START_NOW_TEXT = "Start now";

    private IdlingResource mIdlingResource;

    @Rule
    public GrantPermissionRule permissionRule
            = GrantPermissionRule.grant(PermissionsHandler.REQUIRED_EXTENDED_PERMISSIONS);

    @Before
    public void before() {
        //try before method to make @Rule run properly
    }

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = getInstrumentation().getTargetContext();
        assertEquals("io.antmedia.webrtc_android_sample_app", appContext.getPackageName());
    }


    /**
     * This test should be in another method but cannot get the full logcat so it's moved here
     */
    @Test
    public void testPublishScreen() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ScreenCaptureActivity.class);
        ActivityScenario<ScreenCaptureActivity> scenario = ActivityScenario.launch(intent);


        scenario.onActivity(new ActivityScenario.ActivityAction<ScreenCaptureActivity>() {

            @Override
            public void perform(ScreenCaptureActivity activity) {
                mIdlingResource = activity.getIdlingResource();
                IdlingRegistry.getInstance().register(mIdlingResource);
                activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            }
        });

        UiDevice device = UiDevice.getInstance(getInstrumentation());

        onView(withId(R.id.rbScreen)).perform(click());
        UiObject2 button = device.wait(Until.findObject(By.text("Start now")), 100000);
        assertNotNull(button);
        button.click();

        //this switch operation causes to crash so that it's added here as test
        onView(withId(R.id.rbFront)).perform(click());
        onView(withId(R.id.rbScreen)).perform(click());

        button = device.wait(Until.findObject(By.text("Start now")), 100000);
        assertNotNull(button);
        button.click();

        onView(withId(R.id.start_streaming_button)).check(matches(withText("Start")));
        //Espresso.closeSoftKeyboard();
        onView(withId(R.id.start_streaming_button)).perform(click());

        onView(withId(R.id.start_streaming_button)).check(matches(withText("Stop")));
        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        //2. stop stream and check that it's stopped
        onView(withId(R.id.start_streaming_button)).perform(click());
        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));


        //Publish again without because it was failing
        onView(withId(R.id.start_streaming_button)).check(matches(withText("Start")));

        //FIXME: without this sleep, it's failing because onFinish event received but resources are not closed yet
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        onView(withId(R.id.start_streaming_button)).perform(click());

        //Check it's publishing again
        onView(withId(R.id.start_streaming_button)).check(matches(withText("Stop")));
        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        //Stop publishing
        onView(withId(R.id.start_streaming_button)).perform(click());
        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));


        IdlingRegistry.getInstance().unregister(mIdlingResource);
    }
}
