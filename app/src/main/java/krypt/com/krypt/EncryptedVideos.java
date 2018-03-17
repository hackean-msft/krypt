package krypt.com.krypt;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;
import io.realm.RealmQuery;
import krypt.com.krypt.utils.MessageToast;
import krypt.com.krypt.video.EncryptedVideo;
import krypt.com.krypt.video.EncryptedVideoViewAdapter;
import krypt.com.krypt.video.Video;
import krypt.com.krypt.video.VideoEncryptionException;
import krypt.com.krypt.video.VideoEncryptionHandler;
import krypt.com.krypt.video.VideoEvent;

import static krypt.com.krypt.video.VideoEncryptionHandler.EncryptionHandler;

public class EncryptedVideos extends Fragment implements VideoEvent.EncryptedVideoActionListener, VideoEncryptionHandler.EncryptionHandler{

    @BindView(R.id.encrypted_videos)
    RecyclerView encryptedVideosRecyclerView;

    @BindView(R.id.progress_bar)
    ProgressBar progressBar;

    EncryptedVideoViewAdapter adapter;

    VideoEncryptionHandler handler;

    int decryptAdapterPosition;

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

        Realm realm = Realm.getDefaultInstance();
        RealmQuery<EncryptedVideo> query = realm.where(EncryptedVideo.class);
        List<EncryptedVideo> encryptedVideoList = query.findAll();

        // Creates new realm unmanaged objects
        for(EncryptedVideo v: encryptedVideoList){

        }


        adapter = new EncryptedVideoViewAdapter(getContext(), new ArrayList<>(encryptedVideoList), this);

        encryptedVideosRecyclerView.setAdapter(adapter);
        encryptedVideosRecyclerView.setHasFixedSize(true);
        encryptedVideosRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

    }

    @Override
    public void onResume() {
        super.onResume();
        handler.subscribe(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.unSubscribe(this);
    }

    @Override
    public void onPlayClicked(EncryptedVideo encryptedVideo) {
        VideoEncryptionHandler handler = VideoEncryptionHandler.newInstance();
        try {
            String kryptDirectory = handler.getKryptifiedDirectory();
            File currentFile = getContext().getDir("temp", Context.MODE_PRIVATE);

            String path = currentFile.getAbsolutePath();

            if (path.endsWith("/")){
                path = path.concat("temp.run");
            } else {
                path = path.concat("/temp.run");
            }

            FileInputStream source = new FileInputStream(kryptDirectory + "/" + encryptedVideo.getId() + ".enc");
            FileOutputStream destination =  new FileOutputStream(new File(path));

            handler.decrypt(source, destination);
            destination.flush();
            destination.close();

            Intent i = new Intent(getContext(), VideoPlayerActivity.class);
            i.putExtra("path", path);
            startActivity(i);
        } catch (Exception e){
            MessageToast.showSnackBar(getContext(), e.getMessage());
        }
    }

    @Override
    public void onDecryptClicked(EncryptedVideo encryptedVideo, int pos) {
        this.decryptAdapterPosition = pos;
        this.progressBar.setVisibility(View.VISIBLE);
        new DecryptionTask(encryptedVideo).execute();
    }

    @Override
    public void onVideoEncrypted(EncryptedVideo encryptedVideo) {
        adapter.addEncryptedVideo(encryptedVideo);
    }

    @Override
    public void onVideoDecrypted(Video video) {
        adapter.removeEncryptedVideo(decryptAdapterPosition);
        this.progressBar.setVisibility(View.GONE);
        try {
            MessageToast.showSnackBar(getContext(), "Video was decrypted successfully");
        } catch(NullPointerException e){
            e.printStackTrace();
        }
    }

    public class DecryptionTask extends AsyncTask<Void, Void, Video> {

        VideoEncryptionHandler handler;
        private EncryptedVideo video;

        public DecryptionTask(EncryptedVideo video) {
            handler = VideoEncryptionHandler.newInstance();
            this.video = new EncryptedVideo();

            this.video.setId(video.getId());
            this.video.setOriginalPath(video.getOriginalPath());
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Video doInBackground(Void... params) {
            Video vid = null;
            try {
                vid =  handler.decrypt(video);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (VideoEncryptionException e) {
                e.printStackTrace();
            }
            return vid;
        }

        @Override
        protected void onPostExecute(Video video) {
            Set<EncryptionHandler> observers = handler.getRegisteredObservers();
            for (VideoEncryptionHandler.EncryptionHandler e : observers) {
                e.onVideoDecrypted(video);
            }
        }
    }
}
