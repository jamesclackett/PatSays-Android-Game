package com.jimboidin.patsays.Social;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;

import android.text.InputType;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jimboidin.patsays.LobbyActivity;
import com.jimboidin.patsays.R;

import java.util.HashMap;


public class FriendsFragment extends Fragment {
    private final String TAG = "FriendsFragment";
    private Context mContext;
    private FirebaseAuth mAuth;
    private DatabaseReference mFriendsDB, mRequestsDB;
    private ValueEventListener mFriendsListener, mRequestsListener;
    private ListView mFriendsListView, mRequestsListView;
    private ArrayAdapter<String> mFriendAdapter, mRequestAdapter;
    private HashMap<String, String> mRequestIDMap, mFriendsIDMap;
    private String mUsername;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friends, container, false);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }


    @Override
    public void onStart() {
        super.onStart();
        if (getView() != null){
            mAuth = FirebaseAuth.getInstance();

            getMyUsername();
            initializeListViews();
            getFriendsList();
            getRequestsList();
            attachContextMenus();

            FloatingActionButton fab = getView().findViewById(R.id.add_friend_fab);
            fab.setOnClickListener(v -> openRequestDialog());
        }
    }

    private void getMyUsername(){
        FirebaseDatabase.getInstance().getReference().child("Users")
                .child(mAuth.getUid()).child("username").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists())
                    mUsername = snapshot.getValue(String.class);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void initializeListViews(){
        mFriendsListView = getView().findViewById(R.id.friends_list_view);
        mRequestsListView = getView().findViewById(R.id.requests_list_view);
        mFriendAdapter = new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1);
        mRequestAdapter = new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1);
        mFriendsListView.setAdapter(mFriendAdapter);
        mRequestsListView.setAdapter(mRequestAdapter);
    }

    private void attachContextMenus() {
        registerForContextMenu(mFriendsListView);
        registerForContextMenu(mRequestsListView);
        mFriendsListView.setOnItemClickListener((parent, view, position, id) -> parent.showContextMenuForChild(view));
        mRequestsListView.setOnItemClickListener((parent, view, position, id) -> parent.showContextMenuForChild(view));
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu,
                                    @NonNull View v, @Nullable ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        String name = ((TextView) info.targetView).getText().toString();
        menu.setHeaderTitle(name);
        menu.setHeaderIcon(R.drawable.ic_add_friend);

        if (v.getId() == R.id.friends_list_view){
            inflater.inflate(R.menu.friends_menu, menu);
        }
        if (v.getId() == R.id.requests_list_view){
            inflater.inflate(R.menu.requests_menu, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        String name = ((TextView) info.targetView).getText().toString();

        if (item.getItemId() == R.id.accept_friend)
            acceptFriendRequest(name, true);
        else if (item.getItemId() == R.id.decline_friend)
            acceptFriendRequest(name, false);
        else if (item.getItemId() == R.id.invite_friend)
            inviteToGame(name);
        else if (item.getItemId() == R.id.remove_friend)
            removeFriend(name);

        return super.onContextItemSelected(item);
    }

    private void removeFriend(String name) {
    }

    private void inviteToGame(String name) {
    }

    private void acceptFriendRequest(String name, boolean isAccept) {
        Log.i(TAG, name + ". Accept Request: " + isAccept);
        String key = mRequestIDMap.get(name);

        if (isAccept){
            mFriendsDB.child(key).child("username").setValue(name);
            FirebaseDatabase.getInstance().getReference()
                    .child("Users").child(key).child("Friends")
                    .child(mAuth.getUid()).child("username").setValue(mUsername);
        }

        mRequestsDB.child(key).removeValue();
    }

    private void getFriendsList(){
        mFriendsIDMap = new HashMap<>();
        mFriendsDB = FirebaseDatabase.getInstance().getReference()
                .child("Users").child(mAuth.getUid()).child("Friends");
        mFriendsListener = mFriendsDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mFriendAdapter.clear();
                if (snapshot.exists())
                    for (DataSnapshot childData : snapshot.getChildren()){
                        getView().findViewById(R.id.friends_title).setVisibility(View.VISIBLE);
                        String username = childData.child("username").getValue(String.class);
                        mFriendAdapter.add(username);
                        mFriendsIDMap.put(username, childData.getKey());
                        //IDMap saves having to add both key and name to adapter. Not clever but hey its simple
                    }
                if (mFriendAdapter.isEmpty())
                    getView().findViewById(R.id.friends_title).setVisibility(View.GONE);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void getRequestsList(){
        mRequestIDMap = new HashMap<>();
        mRequestsDB = FirebaseDatabase.getInstance().getReference()
                .child("Users").child(mAuth.getUid()).child("Requests");
        mRequestsListener = mRequestsDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mRequestAdapter.clear();
                if (snapshot.exists())
                    for (DataSnapshot childData: snapshot.getChildren()) {
                        getView().findViewById(R.id.requests_title).setVisibility(View.VISIBLE);
                        String username = childData.child("username").getValue(String.class);
                        mRequestAdapter.add(username);
                        mRequestIDMap.put(username, childData.getKey());
                    }
                if (mRequestAdapter.isEmpty())
                    getView().findViewById(R.id.requests_title).setVisibility(View.GONE);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }




    private void openRequestDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Add Friend");

        final EditText input = new EditText(mContext);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> sendFriendRequest(input.getText().toString()));
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void sendFriendRequest(String requestInput) {
        if (!requestInput.equals(mUsername)) {
            DatabaseReference usersDB = FirebaseDatabase.getInstance().getReference().child("Users");
            usersDB.orderByChild("username").equalTo(requestInput).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String invitedUserID = null;
                    for (DataSnapshot childData : snapshot.getChildren()) {
                        invitedUserID = childData.getKey();
                        Log.i(TAG, "add friend: user found");
                    }
                    if (invitedUserID != null && mAuth.getUid() != null) {
                        usersDB.child(invitedUserID).child("Requests")
                                .child(mAuth.getUid()).child("username").setValue(mUsername);
                        Log.i(TAG, "add friend: request sent");
                    } else {
                        displayToast("Request not sent - try again");
                        Log.i(TAG, "add friend: request not sent");
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        }
        else {
            displayToast("You cannot add yourself as a friend");
            Log.i(TAG, "Sent friend request to self, discarded");
        }
    }



    private void removeListeners(){
        if (mFriendsListener != null)
            mFriendsDB.removeEventListener(mFriendsListener);
        if (mRequestsListener != null)
            mRequestsDB.removeEventListener(mRequestsListener);
    }

    private void displayToast(String message) {
        Toast.makeText(mContext, message,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeListeners();
    }
}