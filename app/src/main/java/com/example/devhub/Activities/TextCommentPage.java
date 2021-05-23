package com.example.devhub.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.ColorSpace;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.bumptech.glide.load.model.Model;
import com.example.devhub.AdaptersClasses.GetTextCommentAdapter;
import com.example.devhub.AppClasses.AddNotifications;
import com.example.devhub.ModelClasses.Model_GetTextComments;
import com.example.devhub.R;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TextCommentPage extends AppCompatActivity {

    // XML Variables

    private RecyclerView objectRecyclerView;
    private Button addCommentBtn;
    private EditText commentET;

    // Class Variables

    private Bundle objectBundle;
    private String documentID;
    private String recUserEmail;
    private Date currentDate;
    private SimpleDateFormat objectSimpleDateFormat;

    int noOfComments;
    private String currentLoggedInUser;

    private GetTextCommentAdapter objectGetTextCommentAdapter;
    private AddNotifications objectAddNotifications;

    // Firebase Variables

    private FirebaseFirestore objectFirebaseFirestore;
    private FirebaseAuth objectFirebaseAuth;
    private DocumentReference objectDocumentReference;
    private CollectionReference objectCollectionReference;

    public TextCommentPage() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_comment_page);

        try {
            attachJavaViewsToXML();
            objectBundle = getIntent().getExtras();
            documentID = objectBundle.getString("documentId");
            recUserEmail=objectBundle.getString("userEmailID");
            getCommentsIntoRV();

            addCommentBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    addCommentToFB();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void getCommentsIntoRV(){
        try {
            Query objectQuery=objectFirebaseFirestore.collection("TextStatus")
                    .document(documentID).collection("Comments")
                    .orderBy("currentdatetime",Query.Direction.ASCENDING);

            FirestoreRecyclerOptions<Model_GetTextComments> objectOptions=
                    new FirestoreRecyclerOptions.Builder<Model_GetTextComments>()
                    .setQuery(objectQuery,Model_GetTextComments.class).build();

            objectGetTextCommentAdapter=new GetTextCommentAdapter(objectOptions);
            objectRecyclerView.setAdapter(objectGetTextCommentAdapter);

            objectRecyclerView.setLayoutManager(new LinearLayoutManager(this));


        }
        catch (Exception e){
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        try{
            objectGetTextCommentAdapter.startListening();
        }
        catch (Exception e){
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try{
            objectGetTextCommentAdapter.stopListening();
        }
        catch (Exception e){
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void addCommentToFB() {
        try {
            if (objectFirebaseAuth != null && !commentET.getText().toString().isEmpty()) {
                currentLoggedInUser = objectFirebaseAuth.getCurrentUser().getEmail();
                objectFirebaseFirestore = FirebaseFirestore.getInstance();
                objectDocumentReference = objectFirebaseFirestore.collection("UserProfileData")
                        .document(currentLoggedInUser);
                objectDocumentReference.get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                String profileURL = documentSnapshot.getString("profileimageurl");

                                Map<String, Object> objectMap = new HashMap<>();
                                objectMap.put("commentperson", currentLoggedInUser);
                                objectMap.put("comment", commentET.getText().toString());
                                objectMap.put("profilepicurl", profileURL);
                                objectMap.put("currentdatetime", getCurrentDate());

                                objectFirebaseFirestore.collection("TextStatus")
                                        .document(documentID).collection("Comments")
                                        .add(objectMap)
                                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                            @Override
                                            public void onSuccess(DocumentReference documentReference) {
                                                Toast.makeText(TextCommentPage.this, R.string.comment_added, Toast.LENGTH_SHORT).show();
                                                objectAddNotifications.generateNotification(currentLoggedInUser,"comment","text status",recUserEmail);
                                                commentET.setText("");
                                                objectCollectionReference = objectFirebaseFirestore.collection("TextStatus")
                                                        .document(documentID)
                                                        .collection("Comments");
                                                objectCollectionReference.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                                    @Override
                                                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                                        int commentCount = queryDocumentSnapshots.size();

                                                        objectDocumentReference = objectFirebaseFirestore.collection("TextStatus")
                                                                .document(documentID);

                                                        objectDocumentReference.update("noofcomments", commentCount);
                                                    }
                                                }).addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {

                                                    }
                                                });
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Toast.makeText(TextCommentPage.this, R.string.comment_not_added, Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        });

            } else if (objectFirebaseAuth == null) {
                Toast.makeText(this, R.string.no_user_online, Toast.LENGTH_SHORT).show();
            } else if (commentET.getText().toString().isEmpty()) {
                Toast.makeText(this, "Please add a comment ", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String getCurrentDate() {
        try {
            currentDate = Calendar.getInstance().getTime();
            objectSimpleDateFormat = new SimpleDateFormat("HH:mm:ss dd:MMM:yyyy");
            return objectSimpleDateFormat.format(currentDate);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void attachJavaViewsToXML() {
        try {
            objectFirebaseAuth = FirebaseAuth.getInstance();
            objectFirebaseFirestore = FirebaseFirestore.getInstance();

            objectRecyclerView = findViewById(R.id.techStatus_commentRV);
            commentET = findViewById(R.id.techStatus_commentET);

            addCommentBtn = findViewById(R.id.techStatus_commentAddCommentBtn);
            objectAddNotifications=new AddNotifications();
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }
}
