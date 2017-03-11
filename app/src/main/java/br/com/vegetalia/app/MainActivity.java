package br.com.vegetalia.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int INTERSTITIAL_AD_FREQUENCY = 4;
    private static final int RESULTS_PER_PAGE = 4;

    private FeedAdapter adapter;

    private Long lastTimestamp = null;

    private int lastShownInterstitialIndex;
    private InterstitialAd interstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupRecyclerView();

        setupToolbar();
        setupDrawer();
        setupFab();
        setupBanner();
        setupInterstitial();

    }

    private void setupInterstitial() {
        interstitialAd = new InterstitialAd(this);
        interstitialAd.setAdUnitId(getString(R.string.admob_interstitial_ad_unit_id));
        interstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                requestNewInterstitial();
            }
        });
        requestNewInterstitial();
    }

    private void setupBanner() {
        AdView mAdView = (AdView) findViewById(R.id.ad_view);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(getString(R.string.admob_test_device_id))
                .build();
        mAdView.loadAd(adRequest);
    }

    private void setupRecyclerView() {
        adapter = new FeedAdapter();
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();
        db.child("posts").limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getChildrenCount() == 0 ) {
                    //TODO: SHOW EMPTY STATE...
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        adapter.setLastItemBindListener(new FeedAdapter.LastItemBindListener() {
            @Override
            public void onLastItemBind() {
                loadMore();
            }
        });
        adapter.setItemBindListener(new FeedAdapter.ItemBindListener() {
            @Override
            public void onitemBind(int index) {
                if ((index + 1) % INTERSTITIAL_AD_FREQUENCY == 0 && index > lastShownInterstitialIndex) {
                    if (interstitialAd.isLoaded()) {
                        interstitialAd.show();
                        lastShownInterstitialIndex = index;
                    }
                }
            }
        });
        loadMore();
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void setupDrawer() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void setupFab() {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, NewPostActivity.class));
            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(getString(R.string.admob_test_device_id))
                .build();
        interstitialAd.loadAd(adRequest);
    }

    private void loadMore() {
        Log.d("AC-DEBUG", "Loading more lastkey: " + (lastTimestamp == null ? "null" : lastTimestamp));
        DatabaseReference db = FirebaseDatabase.getInstance().getReference("posts");
        Query query = db.orderByChild("created_at");
        if (lastTimestamp != null) query = query.endAt(lastTimestamp);
        query.limitToLast(RESULTS_PER_PAGE).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildKey) {
                Long timestamp = (Long)dataSnapshot.child("created_at").getValue();
                if (lastTimestamp == null || lastTimestamp > timestamp) { lastTimestamp = timestamp - 1; }
                adapter.addItem(dataSnapshot);
                Log.d("AC-DEBUG", "addChildEventListener Added child: " + dataSnapshot.child("message").getValue());
            }
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildKey) {
                adapter.addItem(dataSnapshot);
                Log.d("AC-DEBUG", "addChildEventListener Changed child: " + previousChildKey);
            }
            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Log.d("AC-DEBUG", "addChildEventListener Removed child: ");
            }
            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildKey) {
                adapter.addItem(dataSnapshot);
                Log.d("AC-DEBUG", "addChildEventListener Moved child: " + previousChildKey);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("AC-DEBUG", "addChildEventListener Cancelled");
            }
        });
    }
}
