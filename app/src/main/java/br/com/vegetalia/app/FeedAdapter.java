package br.com.vegetalia.app;

import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.NativeExpressAdView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.ViewHolder> {

    public static final int ITEM_TYPE_POST = 0;
    public static final int ITEM_TYPE_NATIVE_AD = 1;

    private static final int NATIVE_AD_FREQUENCY = 2;

    private LastItemBindListener lastItemBindListener = null;
    private ItemBindListener itemBindListener = null;

    private SortedList<DataSnapshot> dataset = new SortedList<DataSnapshot>(DataSnapshot.class, new SortedList.Callback<DataSnapshot>() {

        @Override
        public int compare(DataSnapshot data1, DataSnapshot data2) {
            return (int)(data2.child("created_at").getValue(Long.class) - data1.child("created_at").getValue(Long.class));
        }

        @Override
        public void onInserted(int position, int count) {
            notifyItemRangeInserted(position, count);
        }

        @Override
        public void onRemoved(int position, int count) {
            notifyItemRangeRemoved(position, count);
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            notifyItemMoved(fromPosition, toPosition);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(DataSnapshot oldItem, DataSnapshot newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(DataSnapshot item1, DataSnapshot item2) {
            return item1.getKey().equals(item2.getKey());
        }
    });


    public void setLastItemBindListener(LastItemBindListener lastItemBindListener) {
        this.lastItemBindListener = lastItemBindListener;
    }

    public void setItemBindListener(ItemBindListener itemBindListener) {
        this.itemBindListener = itemBindListener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        String itemId;

        private DatabaseReference likeReference;
        private ValueEventListener likeEventListener;

        private DataSnapshotObserver userObserver;
        private DataSnapshotObserver likedObserver;

        public TextView title;
        public ImageView image;
        public ImageView likeButton;
        public TextView likesCount;

        public ImageView userImage;
        public TextView userName;

        public ViewHolder(View v) {
            super(v);
            title = (TextView)v.findViewById(R.id.title);
            likesCount = (TextView)v.findViewById(R.id.likes_count);
            image = (ImageView)v.findViewById(R.id.image);
            likeButton = (ImageView)v.findViewById(R.id.like_button);
            userImage = (ImageView)v.findViewById(R.id.user_image);
            userName = (TextView)v.findViewById(R.id.user_name);
        }

        private void reset() {
            title.setText("");
            likesCount.setText("0");
            image.setImageResource(R.drawable.placeholder_post);
            likeButton.setImageResource(R.drawable.ic_like_off);
            likeButton.setOnClickListener(null);

            userImage.setImageResource(R.drawable.placeholder_profile);
            userName.setText("");

            if (userObserver != null) userObserver.cancel();
            userObserver = null;

            if (likedObserver != null) likedObserver.cancel();
            likedObserver = null;

            if (likeReference != null) likeReference.removeEventListener(likeEventListener);
            likeReference = null;
            likeEventListener = null;
        }

        public void render(DataSnapshot dataSnapshot) {

            String newItemId = dataSnapshot.getKey();
            if (itemId != newItemId) {
                reset();

                userObserver = User.fetchProfile(dataSnapshot.child("user_id").getValue(String.class), new DataSnapshotObserver.OnDataSnapshotReceivedListener() {
                    @Override
                    public void onDataSnapshotReceived(DataSnapshot dataSnapshot) {
                        Picasso.with(itemView.getContext())
                                .load(dataSnapshot.child("image").getValue(String.class))
                                .placeholder(R.drawable.placeholder_profile)
                                .error(R.drawable.placeholder_profile)
                                .into(userImage);
                        userName.setText(dataSnapshot.child("name").getValue(String.class));
                    }
                });

                likedObserver = Post.fetchLiked(newItemId, new DataSnapshotObserver.OnDataSnapshotReceivedListener() {
                    @Override
                    public void onDataSnapshotReceived(DataSnapshot likeDataSnapshot) {
                        if (likeDataSnapshot.exists() && likeDataSnapshot.getValue(Boolean.class)) {
                            likeButton.setImageResource(R.drawable.ic_like_on);
                            likeButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Post.like(itemId, false);
                                }
                            });
                        }
                        else {
                            likeButton.setImageResource(R.drawable.ic_like_off);
                            likeButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Post.like(itemId, true);
                                }
                            });
                        }
                    }
                });
            }
            itemId = newItemId;

            title.setText(dataSnapshot.child("title").getValue(String.class));
            likesCount.setText(dataSnapshot.hasChild("likes_count") ? dataSnapshot.child("likes_count").getValue(Integer.class).toString() : "0");
            Picasso.with(itemView.getContext())
                    .load(dataSnapshot.child("image").getValue(String.class))
                    .placeholder(R.drawable.placeholder_post)
                    .error(R.drawable.placeholder_post)
                    .into(image);
        }
    }

    public FeedAdapter() {
    }

    public void addItem(DataSnapshot dataSnapshot) {
        dataset.add(dataSnapshot);
        notifyDataSetChanged();
    }

    @Override
    public FeedAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case ITEM_TYPE_NATIVE_AD:
                return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_main_feed_native_ad, parent, false));
            default:
                return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_main_feed, parent, false));
        }
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.image != null) holder.image.setImageResource(R.drawable.placeholder_post);
    }

    @Override
    public int getItemViewType(int position) {
        if ((position + 1) % (NATIVE_AD_FREQUENCY + 1) == 0) return ITEM_TYPE_NATIVE_AD;
        return ITEM_TYPE_POST;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (getItemViewType(position) == ITEM_TYPE_NATIVE_AD) {
            NativeExpressAdView adView = (NativeExpressAdView) holder.itemView.findViewById(R.id.ad_view);
            AdRequest request = new AdRequest.Builder()
                    .addTestDevice(holder.itemView.getResources().getString(R.string.admob_test_device_id))
                    .build();
            adView.loadAd(request);
        }
        else {
            int relativePosition = position - (position / (NATIVE_AD_FREQUENCY + 1));
            if (itemBindListener != null) itemBindListener.onitemBind(relativePosition);
            if (relativePosition == dataset.size() - 1 && lastItemBindListener != null) {
                lastItemBindListener.onLastItemBind();
            }
            DataSnapshot dataSnapshot = dataset.get(relativePosition);
            holder.render(dataSnapshot);
        }
    }

    @Override
    public int getItemCount() {
        return dataset.size() + (int)(dataset.size() / NATIVE_AD_FREQUENCY);
    }

    public interface ItemBindListener {
        void onitemBind(int index);
    }

    public interface LastItemBindListener {
        void onLastItemBind();
    }
}