# Publish Picabloom: Photo Puzzle with rewarded ads

## Set up the test

1. Open the Picabloom folder in Android Studio and sync Gradle. The Android application ID is `com.hb.puzz`. Confirm that this is the package you intend to publish before the first Play upload. The project uses version code 3 and version name 1.2. If Play already has code 3 or higher, increase it before uploading.
2. In AdMob, open the existing app for this package and update its display name, or add Picabloom: Photo Puzzle as an unpublished Android app if this is your first setup. Use your existing **Rewarded** ad unit or create one and set its reward to **1 Crystal**. Copy the App ID (contains a tilde) and the rewarded unit ID (contains a slash).
3. In AdMob **Privacy & messaging**, publish the messages appropriate for your distribution regions. To test your own consent setup, add your AdMob App ID to the project's existing `local.properties`. Keep its existing `sdk.dir` line. Add the public privacy policy URL so Settings can open it. Substitute your real values.

       ADMOB_APP_ID=ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY
       PRIVACY_POLICY_URL=https://your-domain.example/picabloom/privacy
       ADMOB_LIVE_ADS=false

4. Run the app and finish a puzzle. The regular completion reward and Next Level button remain available. When a test ad loads, tap **Watch an ad for 1 Crystal**. Complete it and verify the wallet increases by exactly one. Reopen the app and confirm that the balance persists. Also try closing before earning the reward, rapidly tapping the offer, and completing a level offline. Only the SDK's earned-reward callback credits the wallet. In applicable regions verify Settings → Ad privacy choices using your configured AdMob privacy message.
5. Confirm the intended age groups before enabling live ads. This integration has no age screen or child-specific request configuration. If children are in the target audience, review Google's Families requirements and configure age-appropriate treatment and certified SDK versions before distributing ads to that audience.

## Upload a test bundle

1. In Play Console, update the existing listing for this package or create one if this is your first release. Set the title to **Picabloom: Photo Puzzle**, upload `branding/play-icon-512.png` as the app icon, choose **Game**, and complete the store listing, screenshots, developer contact information and content rating. Under App content, declare **Contains ads**, the actual target audience and the privacy policy URL. Review the Advertising ID declaration against the release's merged manifest. Use Google's SDK disclosure together with your own app behavior, including optional online image loading, to complete Data safety.
2. In Android Studio select **Build → Generate Signed Bundle / APK → Android App Bundle**, select the app module, and create or select your upload keystore. Keep a private backup of the keystore and its credentials. Select **release** and generate the AAB. Upload the signed .aab to an internal testing release and configure Play App Signing as prompted.
3. Install via the tester opt-in link. On a newer personal developer account, run the required closed test and then apply for production access. Leave live ads off throughout testing.
4. For production, add your real unit ID to local.properties as ADMOB_REWARDED_ID, set ADMOB_LIVE_ADS=true, increase versionCode above the last uploaded bundle and generate a new signed release AAB. Debug builds always use the test unit. Use your developer website in the store listing and host the exact app-ads.txt snippet from AdMob at its root. Once publicly listed, link Picabloom: Photo Puzzle in AdMob, verify the app and check its readiness status.

## Google references

- [Rewarded ads and test units](https://developers.google.com/admob/android/rewarded)
- [AdMob consent and privacy choices](https://developers.google.com/admob/android/privacy)
- [Mobile Ads SDK data disclosure](https://developers.google.com/admob/android/privacy/play-data-disclosure)
- [Families ads SDK requirements](https://support.google.com/googleplay/android-developer/answer/12955712)
- [Play Console app content](https://support.google.com/googleplay/android-developer/answer/9859455)
- [Sign a release bundle](https://developer.android.com/studio/publish/app-signing)
- [Closed testing for new personal developer accounts](https://support.google.com/googleplay/android-developer/answer/14151465)
- [AdMob app verification](https://support.google.com/admob/answer/14538460)
