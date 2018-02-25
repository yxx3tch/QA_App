package jp.techacademy.yxx3tch.qa_app;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class FavoriteActivity extends AppCompatActivity {

    private Toolbar mToolbar;

    private DatabaseReference mDatabaseReference;
    private DatabaseReference mContentsRef;
    private ListView mListView;

    private ArrayList<Question> mQuestionArrayList;
    private ArrayList<String> mFavoriteArrayList;
    private QuestionsListAdapter mAdapter;

    @Override
    protected void onStart() {
        super.onStart();
        setTitle("お気に入り");
        setContentView(R.layout.activity_favorite);

        mToolbar = (Toolbar) findViewById(R.id.favorite_toolbar);
        setSupportActionBar(mToolbar);

        // ナビゲーションドロワーの設定
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.favorite_drawer_layout);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, mToolbar, R.string.app_name, R.string.app_name);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.favorite_nav_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int id = item.getItemId();
                int mGenre = 0;
                if(id != R.id.nav_favorite) {
                    if (id == R.id.nav_hobby) {
                        mGenre = 1;
                    } else if (id == R.id.nav_life) {
                        mGenre = 2;
                    } else if (id == R.id.nav_health) {
                        mGenre = 3;
                    } else if (id == R.id.nav_compter) {
                        mGenre = 4;
                    }
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.putExtra("genre", mGenre);
                    startActivity(intent);
                }

                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.favorite_drawer_layout);
                drawer.closeDrawer(GravityCompat.START);
                return true;
            }
        });

        // Firebase
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();

        // ListViewの準備
        mListView = (ListView) findViewById(R.id.favoriteListView);
        mAdapter = new QuestionsListAdapter(this);
        mQuestionArrayList = new ArrayList<Question>();

        mContentsRef = mDatabaseReference.child(Const.ContentsPATH);

        // favoritesにユーザーのidが存在する質問をmQuestionArrayListに追加
        mContentsRef.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        HashMap<String, Object> contents = (HashMap<String, Object>)dataSnapshot.getValue(true);
                        System.out.println(dataSnapshot.getValue(true));
                        for (Object genre : contents.keySet()){
                            HashMap map = (HashMap) contents.get(genre);
                            for (Object key : map.keySet()) {
                                boolean isFavorited = false;
                                String favoritedKey = null;
                                HashMap temp = (HashMap) map.get((String) key);
                                String title = (String) temp.get("title");
                                String body = (String) temp.get("body");
                                String name = (String) temp.get("name");
                                String uid = (String) temp.get("uid");
                                String imageString = (String) temp.get("image");
                                byte[] bytes;
                                if (imageString != null) {
                                    bytes = Base64.decode(imageString, Base64.DEFAULT);
                                } else {
                                    bytes = new byte[0];
                                }

                                ArrayList<Answer> answerArrayList = new ArrayList<Answer>();
                                HashMap answerMap = (HashMap) temp.get("answers");
                                if (answerMap != null) {
                                    for (Object anskey : answerMap.keySet()) {
                                        HashMap anstemp = (HashMap) answerMap.get((String) anskey);
                                        String answerBody = (String) anstemp.get("body");
                                        String answerName = (String) anstemp.get("name");
                                        String answerUid = (String) anstemp.get("uid");
                                        Answer answer = new Answer(answerBody, answerName, answerUid, (String) anskey);
                                        answerArrayList.add(answer);
                                    }
                                }

                                ArrayList<String> favoritesArrayList = new ArrayList<String>();
                                HashMap favoritesMap = (HashMap) temp.get("favorites");
                                if (favoritesMap != null) {
                                    for (Object favkey : favoritesMap.keySet()) {
                                        HashMap favtemp = (HashMap) favoritesMap.get((String) favkey);
                                        String favorite = (String) favtemp.get("uid");
                                        if (favorite.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                                            isFavorited = true;
                                            favoritedKey = (String)key;
                                        }
                                        favoritesArrayList.add(favorite);
                                    }
                                }
                                if(isFavorited) {
                                    Question question = new Question(title, body, name, uid, favoritedKey, Integer.parseInt((String)genre), bytes, answerArrayList, favoritesArrayList);
                                    mQuestionArrayList.add(question);
                                }
                            }
                        }
                        mAdapter.setQuestionArrayList(mQuestionArrayList);
                        mListView.setAdapter(mAdapter);
                        mAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Questionのインスタンスを渡して質問詳細画面を起動する
                Intent intent = new Intent(getApplicationContext(), QuestionDetailActivity.class);
                intent.putExtra("question", mQuestionArrayList.get(position));
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
