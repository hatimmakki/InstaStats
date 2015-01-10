package west.districtr.instastats;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;


public class TotalLikes extends Activity {
    public static final String PREFS_NAME = "MyPrefsFile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_total_likes);
        final SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        final SharedPreferences.Editor editor = prefs.edit();





        Button calcLikes = (Button) findViewById(R.id.CalculateLikesButton);
        calcLikes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView numOfPhotoLikesTV = (TextView) findViewById(R.id.NumberOfLikesTV);
                TextView numOfPhotosTV = (TextView) findViewById(R.id.NumberOfPhotosTV);

                int sum = 0;
                int picSum = 0;

                String userID = prefs.getString("API_USER_ID", null);
                String requestToken = prefs.getString("API_ACCESS_TOKEN", null);
                System.out.println(userID + " : user id");
                System.out.println(requestToken + " : request Token");

                try {
                    String url = "https://api.instagram.com/v1/users/" + userID + "/media/recent/?access_token=" + requestToken;
                    JSONObject jObject =  new APICall().execute(url).get();
                    JSONArray photos;
                    JSONObject pag = jObject.getJSONObject("pagination");

                    do {
                        // while this repeats the above code, we have to do it because we
                        // will need to reassign these with the new url each iteration
                        jObject =  new APICall().execute(url).get();
                        photos = jObject.getJSONArray("data");
                        String nextURL = pag.getString("next_url");
                        url = nextURL;
                        // program optimization
                        int numOfPhotos = photos.length();

                        // below sums up picture likes
                        for (int i = 0; i < numOfPhotos; ++i) {
                            // increments through array of photos and adds up likes
                            // based off each one
                            JSONObject photo = photos.getJSONObject(i);
                            JSONObject likes = photo.getJSONObject("likes");
                            sum += Integer.parseInt(likes.getString("count"));
                            picSum++;
                            System.out.println("Sum of " + picSum + " pictures: " + sum);
                        }
                        // make next url the next one
                        pag = jObject.getJSONObject("pagination");
                    }while(!(pag.toString().equals("{}")));
                    numOfPhotosTV.setText(picSum + " photos");
                    numOfPhotoLikesTV.setText(sum + " likes");
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (ExecutionException e){
                    e.printStackTrace();
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        });

        Button twitterShare = (Button) findViewById(R.id.TwitterShareButton);
        twitterShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // http://stackoverflow.com/questions/2077008/android-intent-for-twitter-application
                TextView numOfPhotoLikesTV = (TextView) findViewById(R.id.NumberOfLikesTV);
                TextView numOfPhotosTV = (TextView) findViewById(R.id.NumberOfPhotosTV);
                String[] likeSumArr = numOfPhotoLikesTV.getText().toString().split(" ");
                String[] photoSumArr = numOfPhotosTV.getText().toString().split(" ");
                // Create intent using ACTION_VIEW and a normal Twitter url:
                String tweetUrl =
                        String.format("https://twitter.com/intent/tweet?text=%s&url=%s",
                                urlEncode(NumberFormat.getNumberInstance(Locale.US).format(Integer.parseInt(likeSumArr[0])) + " total likes from my most recent "
                                        + NumberFormat.getNumberInstance(Locale.US).format(Integer.parseInt(photoSumArr[0])) + " Instagram photos. Calculated with InstaStats"),
                                urlEncode("https://www.google.fi/"));
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(tweetUrl));

                // Narrow down to official Twitter app, if available:
                List<ResolveInfo> matches = getPackageManager().queryIntentActivities(intent, 0);
                for (ResolveInfo info : matches) {
                    if (info.activityInfo.packageName.toLowerCase().startsWith("com.twitter")) {
                        intent.setPackage(info.activityInfo.packageName);
                    }
                }
                startActivity(intent);
            }
        });


    }

    public static String urlEncode(String s) {
        // http://stackoverflow.com/questions/2077008/android-intent-for-twitter-application
        try {
            return URLEncoder.encode(s, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new RuntimeException("URLEncoder.encode() failed for " + s);
        }
    }
}
