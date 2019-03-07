package natekamp.ideas;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class PostActivity extends AppCompatActivity
{
    private ProgressDialog loadingBar;
    private Toolbar mToolbar;
    private Button finishButton;
    private ImageButton attachmentButton;
    private EditText titleText, descriptionText;
    private final static int Gallery_Vid = 1, Gallery_Img = 2;
    private Uri attachmentUri;
    private String postTitle, postDescription;
    boolean postTypeIsVideo = getIntent().getBooleanExtra("EXTRA_POST_TYPE", true);
    private StorageReference postAttachmentsRef;
    private DatabaseReference usersRef, postedVideosRef, postedEventsRef;
    private FirebaseAuth mAuth;
    String currentUserID, attachmentValue, currentDate, currentTime, postName;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        mToolbar = (Toolbar) findViewById(R.id.post_toolbar);
            setSupportActionBar(mToolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(
                    postTypeIsVideo ? R.string.post_video_toolbar_title : R.string.post_event_toolbar_title
            );
        attachmentButton = (ImageButton) findViewById(R.id.post_attachment);
        titleText = (EditText) findViewById(R.id.post_title);
        descriptionText = (EditText) findViewById(R.id.post_description);
        postAttachmentsRef = FirebaseStorage.getInstance().getReference();
        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        postedVideosRef = FirebaseDatabase.getInstance().getReference().child("Posts").child("Videos");
        postedEventsRef = FirebaseDatabase.getInstance().getReference().child("Posts").child("Events");
        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        loadingBar = new ProgressDialog(this);

        attachmentButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (postTypeIsVideo) getVideo();
                else getImage();
            }
        });

        finishButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                validatePost();
            }
        });

    }

    private void validatePost()
    {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat date = new SimpleDateFormat("yyyy-MMMM-dd");
        SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss");
        currentDate = date.format(cal.getTime());
        currentTime = time.format(cal.getTime());

        postTitle = titleText.getText().toString();
        postDescription = descriptionText.getText().toString();

        if (attachmentUri==null && postTypeIsVideo)
            Toast.makeText(this, R.string.error_missing_vid, Toast.LENGTH_SHORT).show();
        else if (TextUtils.isEmpty(postTitle))
            Toast.makeText(this, R.string.error_missing_title, Toast.LENGTH_SHORT).show();
        else
        {
            loadingBar.setTitle(PostActivity.this.getString(R.string.progress_title));
            loadingBar.setMessage(PostActivity.this.getString(postTypeIsVideo ?
                    R.string.progress_post_video_msg :
                    R.string.progress_post_event_msg
            ));
            loadingBar.show();
            loadingBar.setCanceledOnTouchOutside(true);

            uploadAttachment(attachmentUri!=null);
        }
    }

    private void uploadAttachment(boolean postingEventWithoutAttachment)
    {
        postName = currentUserID+"_"+currentDate+"_"+currentTime;

        if (postingEventWithoutAttachment)
        {
            attachmentValue = "NO_ATTACHMENT";
            savePostInformation();
        }
        else {
            StorageReference attachmentPath = postAttachmentsRef.child("Post Attachments").child(
                    postName + "_attached" + (postTypeIsVideo ? "Video.mp4" : "Image.png")
            );
            attachmentPath.putFile(attachmentUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    String resultMsg = task.isSuccessful() ?
                            PostActivity.this.getString(R.string.success_upload_attachment_msg) :
                            "Error: " + task.getException().getMessage();

                    Toast.makeText(PostActivity.this, resultMsg, Toast.LENGTH_SHORT).show();

                    if (task.isSuccessful()) {
                        Task<Uri> result = task.getResult().getMetadata().getReference().getDownloadUrl();
                        result.addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                attachmentValue = uri.toString();
                                savePostInformation();
                            }
                        });
                    }
                }
            });
        }
    }

    private void savePostInformation()
    {
        usersRef.child(currentUserID).addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists())
                {
                    String currentUsername = dataSnapshot.child("Username").getValue().toString();
                    String currentProfilePicture = dataSnapshot.child("Profile Picture").getValue().toString();

                    HashMap postMap = new HashMap();
                        postMap.put("UID", currentUserID);
                        postMap.put("Date", currentDate);
                        postMap.put("Time", currentTime);
                        postMap.put("Title", postTitle);
                        postMap.put("Description", postDescription);
                        postMap.put("Attachment", attachmentValue);
                        postMap.put("Profile Picture", currentProfilePicture);
                        postMap.put("Username", currentUsername);
                    if (postTypeIsVideo) {
                        postedVideosRef.child(postName).updateChildren(postMap)
                                .addOnCompleteListener(new OnCompleteListener()
                                {
                                    @Override
                                    public void onComplete(@NonNull Task task)
                                    {
                                        String resultMsg = task.isSuccessful() ?
                                                PostActivity.this.getString(R.string.success_post_video_msg) :
                                                "Error: " + task.getException().getMessage();

                                        Toast.makeText(PostActivity.this, resultMsg, Toast.LENGTH_SHORT).show();

                                        loadingBar.dismiss();
                                        sendToMainActivity();
                                    }
                                });
                    } else {
                        postedEventsRef.child(postName).updateChildren(postMap)
                                .addOnCompleteListener(new OnCompleteListener()
                                {
                                    @Override
                                    public void onComplete(@NonNull Task task)
                                    {
                                        String resultMsg = task.isSuccessful() ?
                                                PostActivity.this.getString(R.string.success_post_event_msg) :
                                                "Error: " + task.getException().getMessage();

                                        Toast.makeText(PostActivity.this, resultMsg, Toast.LENGTH_SHORT).show();

                                        loadingBar.dismiss();
                                        sendToMainActivity();
                                    }
                                });
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {
                //do nothing I guess
            }
        });
    }

    public void getVideo()
    {
        Intent galleryIntent = new Intent();
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("video/*");
        startActivityForResult(galleryIntent, Gallery_Vid);
    }
    public void getImage()
    {
        Intent galleryIntent = new Intent();
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, Gallery_Img);
//        startActivityForResult(
//                Intent.createChooser(galleryIntent,PostActivity.this.getString(R.string.post_image_choose)),
//                Gallery_Img);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode==RESULT_OK && data!=null)
        {
            attachmentUri = data.getData();
            attachmentButton.setImageURI(attachmentUri);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id==android.R.id.home) sendToMainActivity();

        return super.onOptionsItemSelected(item);
    }

    private void sendToMainActivity()
    {
        Intent mainIntent = new Intent(PostActivity.this, MainActivity.class);
        startActivity(mainIntent);
        finish();
    }
}
