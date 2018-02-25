package jp.techacademy.yxx3tch.qa_app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by yxx3tch on 2018/02/24.
 */

public class QuestionDetailListAdapter extends BaseAdapter implements View.OnClickListener, DatabaseReference.CompletionListener {
    private final static int TYPE_QUESTION = 0;
    private final static int TYPE_ANSWER = 1;

    private LayoutInflater mLayoutInflater = null;
    private Question mQuestion;

    private boolean isFavorited = false;
    private ImageButton favoriteButton;

    public QuestionDetailListAdapter(Context context, Question question) {
        mLayoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mQuestion = question;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return TYPE_QUESTION;
        } else {
            return TYPE_ANSWER;
        }
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getCount() {
        return 1 + mQuestion.getAnswers().size();
    }

    @Override
    public Object getItem(int position) {
        return mQuestion;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (getItemViewType(position) == TYPE_QUESTION) {
            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.list_question_detail, parent, false);
            }
            String body = mQuestion.getBody();
            String name = mQuestion.getName();

            TextView bodyTextView = (TextView) convertView.findViewById(R.id.bodyTextView);
            bodyTextView.setText(body);

            TextView nameTextView = (TextView) convertView.findViewById(R.id.nameTextView);
            nameTextView.setText(name);

            byte[] bytes = mQuestion.getImageBytes();
            if (bytes.length != 0) {
                Bitmap image = BitmapFactory.decodeByteArray(bytes, 0, bytes.length).copy(Bitmap.Config.ARGB_8888, true);
                ImageView imageView = (ImageView) convertView.findViewById(R.id.imageView);
                imageView.setImageBitmap(image);
            }

            favoriteButton = (ImageButton) convertView.findViewById(R.id.favoriteButton);
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                // ログインしていない場合お気に入りボタンを表示しない
                favoriteButton.setEnabled(false);
                favoriteButton.setVisibility(View.GONE);
            } else {
                favoriteButton.setEnabled(true);
                ArrayList<String> favorites = mQuestion.getFavorites();
                // すでにお気に入りに追加されているかどうかでボタンの状態を変更
                if(favorites.contains(user.getUid())){
                    isFavorited = true;
                    favoriteButton.setImageResource(R.drawable.like_exist);
                }
                else {
                    isFavorited = false;
                    favoriteButton.setImageResource(R.drawable.like_none);
                }
                favoriteButton.setOnClickListener(this);
            }
        } else {
            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.list_answer, parent, false);
            }

            Answer answer = mQuestion.getAnswers().get(position - 1);
            String body = answer.getBody();
            String name = answer.getName();

            TextView bodyTextView = (TextView) convertView.findViewById(R.id.bodyTextView);
            bodyTextView.setText(body);

            TextView nameTextView = (TextView) convertView.findViewById(R.id.nameTextView);
            nameTextView.setText(name);
        }

        return convertView;
    }

    @Override
    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
        if (databaseError == null) {
            return;
        } else {
            Log.d("QA_App", "failed to add favorite");
        }
    }

    @Override
    public void onClick(View v) {
        DatabaseReference mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        final DatabaseReference favoritesRef = mDatabaseReference.child(Const.ContentsPATH).child(String.valueOf(mQuestion.getGenre())).child(mQuestion.getQuestionUid()).child(Const.FavoritesPATH);
        final DatabaseReference usersRef = mDatabaseReference.child(Const.UsersPATH).child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child(Const.FavoritesPATH);

        // すでにお気に入りされている場合は削除、されていない場合は追加
        if(isFavorited){
            // 質問からお気に入りしたユーザーのIDを削除
            favoritesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    HashMap map = (HashMap) dataSnapshot.getValue();
                    if (map != null) {
                        for (Object key : map.keySet()) {
                            HashMap temp = (HashMap) map.get((String) key);
                            String uid = (String)temp.get("uid");
                            if (uid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                                favoritesRef.child((String)key).removeValue();
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            // ユーザーからお気に入りした質問のIDを削除
            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    System.out.println(dataSnapshot);
                    HashMap map = (HashMap) dataSnapshot.getValue();
                    if (map != null) {
                        for (Object key : map.keySet()) {
                            HashMap temp = (HashMap) map.get((String) key);
                            String uid = (String)temp.get("QuestionUid");
                            if (uid.equals(mQuestion.getQuestionUid())){
                                usersRef.child((String)key).removeValue();
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            isFavorited = false;
            favoriteButton.setImageResource(R.drawable.like_none);
        }else {
            Map<String, String> data1 = new HashMap<String, String>();
            Map<String, String> data2 = new HashMap<String, String>();

            // 質問にお気に入りしたユーザーのIDを追加
            data1.put("uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
            favoritesRef.push().setValue(data1, this);

            // ユーザーにお気に入りに追加した質問のIDを追加
            data2.put("QuestionUid", mQuestion.getQuestionUid());
            usersRef.push().setValue(data2, this);

            isFavorited = true;
            favoriteButton.setImageResource(R.drawable.like_exist);
        }
    }
}
