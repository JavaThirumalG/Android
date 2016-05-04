package aeq.com.image;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.soundcloud.android.crop.Crop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int GALLERY_RESULT = 101;
    ImageView mCropImageView;
    Button mSelectImg, mSave_btn;
    private static final int REQUEST_CHOOSE_FILE = 0xac23;
    private Uri avatarUri;
    private Uri defaultUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCropImageView = (ImageView) findViewById(R.id.cropImageView);
        mSelectImg = (Button) findViewById(R.id.select_img_btn);
        mSave_btn = (Button) findViewById(R.id.save_btn);

        mSelectImg.setOnClickListener(this);
        mSave_btn.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.select_img_btn:
                chooseAvatar();
                break;
            case R.id.save_btn:

        }
    }

    private Bitmap loadScaledBitmap(Uri uri, int reqSize) throws FileNotFoundException {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null, options);
        int rotation = ExifHelper.getOrientation(getContentResolver().openInputStream(uri));
        options.inSampleSize = FileBackend.calcSampleSize(options, reqSize);
        options.inJustDecodeBounds = false;
        Bitmap bm = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null, options);
        return FileBackend.rotate(bm, rotation);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CHOOSE_FILE:
                    Uri source = data.getData();
                    String original = FileUtils.getPath(this, source);
                    if (original != null) {
                        source = Uri.parse("file://" + original);
                    }
                    Uri destination = Uri.fromFile(new File(getCacheDir(), "croppedAvatar"));
                    final int size = getPixel(192);
                    Crop.of(source, destination).asSquare().withMaxSize(size, size).start(this);
                    break;
                case Crop.REQUEST_CROP:
                    this.avatarUri = Uri.fromFile(new File(getCacheDir(), "croppedAvatar"));
                    loadImageIntoPreview(this.avatarUri);
                    break;
            }
        } else {
            if (requestCode == Crop.REQUEST_CROP && data != null) {
                Throwable throwable = Crop.getError(data);
                if (throwable != null && throwable instanceof OutOfMemoryError) {
                    Toast.makeText(this, "The selected area is too large", Toast.LENGTH_SHORT).show();
                }
            }
        }
       // mCropImageView.setImageURI(data.getData());
    }

    public int getPixel(int dp) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        return ((int) (dp * metrics.density));
    }

    private void chooseAvatar() {
        Intent attachFileIntent = new Intent();
        attachFileIntent.setType("image/*");
        attachFileIntent.setAction(Intent.ACTION_GET_CONTENT);
        Intent chooser = Intent.createChooser(attachFileIntent, "Select File");
        startActivityForResult(chooser, REQUEST_CHOOSE_FILE);
    }

    /*
    *  Load image into preview
    *  */
    private void loadImageIntoPreview(Uri avatarUri) {

        Bitmap bm = null;
        try {
            bm = loadScaledBitmap(avatarUri, getPixel(192));
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.mCropImageView.setImageBitmap(bm);
    }

}
