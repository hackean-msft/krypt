package krypt.com.krypt;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import krypt.com.krypt.utils.MessageToast;
import krypt.com.krypt.video.EncryptedVideo;
import krypt.com.krypt.video.EncryptedVideoViewAdapter;
import krypt.com.krypt.video.Video;
import krypt.com.krypt.video.VideoEncryptionHandler;
import krypt.com.krypt.video.VideoEvent;

public class EncryptedVideos extends Fragment implements VideoEvent.EncryptedVideoActionListener, VideoEncryptionHandler.EncryptionHandler{

    @BindView(R.id.encrypted_videos)
    RecyclerView encryptedVideosRecyclerView;

    EncryptedVideoViewAdapter adapter;

    VideoEncryptionHandler handler;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        handler = VideoEncryptionHandler.newInstance();
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.encrypted_videos, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        handler.register(this);

        Realm realm = Realm.getDefaultInstance();
        RealmQuery<EncryptedVideo> query = realm.where(EncryptedVideo.class);
        List<EncryptedVideo> encryptedVideoList = query.findAll();
        adapter = new EncryptedVideoViewAdapter(getContext(), encryptedVideoList, this);

        encryptedVideosRecyclerView.setAdapter(adapter);
        encryptedVideosRecyclerView.setHasFixedSize(true);
        encryptedVideosRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPlayClicked(EncryptedVideo encryptedVideo) {

    }

    @Override
    public void onDecryptClicked(EncryptedVideo encryptedVideo) {
        try {
            handler.decrypt(encryptedVideo);
        } catch (IOException e){
            MessageToast.showSnackBar(getContext(), e.getMessage());
        }
    }

    @Override
    public void onVideoEncrypted(EncryptedVideo encryptedVideo) {
        adapter.addEncryptedVideo(encryptedVideo);
    }

    @Override
    public void onVideoDecrypted(Video video) {
        MessageToast.showSnackBar(getContext(), "Video was decrypted successfully");
    }
}
