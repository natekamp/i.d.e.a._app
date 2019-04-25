package natekamp.ideas;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.VideoView;

public class VideoActivity extends AppCompatActivity
{
    private VideoView videoView;
    String videoURL;
    private MediaController mediaController;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

    //extras
        videoURL = getIntent().getExtras().getString("EXTRA_VIDEO_URL", "https://youtu.be/dQw4w9WgXcQ");


        configureVideoView();
    }

    private void configureVideoView()
    {
        videoView = findViewById(R.id.video_player);
        videoView.setVideoPath(videoURL);

        mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        videoView.start();
    }
}