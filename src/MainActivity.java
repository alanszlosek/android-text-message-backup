package com.alanszlosek.messagebackup;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.JsonWriter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlSerializer;

import android.net.Uri;
import android.os.Environment;
import android.database.Cursor;
import android.util.Base64;
import android.util.Log;
import android.util.Xml;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

/*
 * I don't like text being an attribute ... text should be the contents of the XMl tag, right?
 *
 * Think I can optimize the creation of the base64 string ... stream from inputStream right through to final string? who knows.
 *
 * TODO
 * + use system timestamp in output messages.xml files
 * - maybe need to look up the schemas and write more columns to xml so we can re-import successfully
 */

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MessageBackup";
    private static final Uri CONVERSATIONS_URI = Uri.parse("content://mms-sms/conversations/");
    private static final Uri SMS_URI = Uri.parse("content://sms/");
    private static final String[] SMS_COLUMNS = new String[] { "address", "person", "date", "type",
            "subject", "body" };
    private static final Uri MMS_URI = Uri.parse("content://mms/");
    private static final String[] MMS_COLUMNS = new String[] { "date" };
    private static final Uri MMS_PART_URI = Uri.parse("content://mms/part");
    private static final String[] MMS_PART_COLUMNS = new String[] { "_id", "ct", "_data", "text", "name" };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
/*
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
        */
    }

    private void addJsonFields(JSONObject target, Cursor source) {
        for(int i = 0; i < source.getColumnCount(); i++) {
            try {
                switch (source.getType(i)) {
                    case Cursor.FIELD_TYPE_BLOB:
                        target.put(source.getColumnName(i), source.getBlob(i));
                        break;
                    case Cursor.FIELD_TYPE_FLOAT:
                        target.put(source.getColumnName(i), source.getFloat(i));
                        break;
                    case Cursor.FIELD_TYPE_INTEGER:
                        target.put(source.getColumnName(i), source.getInt(i));
                        break;
                    case Cursor.FIELD_TYPE_NULL:
                        target.put(source.getColumnName(i), null);
                        break;
                    case Cursor.FIELD_TYPE_STRING:
                        target.put(source.getColumnName(i), source.getString(i));
                        break;
                }
            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
    }

    private byte[] readBytes(InputStream inputStream) throws IOException {
        // this dynamically extends to take the bytes you read
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

        // this is storage overwritten on each iteration with bytes
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        // we need to know how may bytes were read to write them to the byteBuffer
        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }

        // and then we can return your byte array.
        return byteBuffer.toByteArray();
    }

    // COPIED
    public void startBackup(View view) throws Exception {
        Resources res = getResources();
        TextView tv = (TextView) this.findViewById(R.id.textView);
        ProgressBar mainProgressBar = (ProgressBar) this.findViewById(R.id.progressBar1);
        File[] externalStorageVolumes = ContextCompat.getExternalFilesDirs(getApplicationContext(), null);
        File newxmlfile;
        String filename = "messages-" + (System.currentTimeMillis() / 1000) + ".json";

        mainProgressBar.setProgress(0);

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Log.e(TAG, "External storage is mounted and writeable");
            newxmlfile = new File(externalStorageVolumes[0], filename);
        } else {
            newxmlfile = new File(this.getFilesDir(), filename);
        }
        tv.append(String.format(res.getString(R.string.debug), newxmlfile.getAbsolutePath()));

        FileOutputStream fileos = null;
        try {
            newxmlfile.createNewFile();
            fileos = new FileOutputStream(newxmlfile);
        } catch (Exception e) {
            Log.e(TAG, "can't create FileOutputStream");
            return;
        }

        /*
        conversations
        sms-mms
         */
        /*
        {
            'sms-mms': [
                {
                    '_id': 'bla',
                    'thread_id': 'bla',
                    'messages': [
                        {
                            '_id': 'bla',
                        }
                    ]
                }
            ],
            'drafts': [
            ]
         */

        // Code pieced together from this great SO answer: https://stackoverflow.com/questions/3012287/how-to-read-mms-data-in-android

        JSONObject jsonObject1;
        JSONArray jsonArray1;
        JSONArray jsonArray2;
        ContentResolver contentResolver = getContentResolver();
        fileos.write("{".getBytes());



        // TODO: handle drafts too
        boolean hasItems1;
        Cursor cursor1;
        Cursor cursor2;

        cursor1 = contentResolver.query(Uri.parse("content://sms/"), null, null, null, null);
        cursor2 = contentResolver.query(Uri.parse("content://mms/"), null, null, null, null);
        cursor1.moveToFirst();
        cursor2.moveToFirst();
        int numMessages = cursor1.getCount() + cursor2.getCount();
        mainProgressBar.setMax(numMessages);
        Log.e(TAG, "num messages " + numMessages);

        int progress = 0;

        // cursor1 = contentResolver.query(Uri.parse("content://sms/"), null, null, null, null);
        fileos.write("\"sms\":[".getBytes());
        if (!cursor1.moveToFirst()) {
            Log.e(TAG, "No sms to export");
        } else {
            hasItems1 = false;
            do {
                if (hasItems1) {
                    fileos.write(",".getBytes());
                }
                jsonObject1 = new JSONObject();
                addJsonFields(jsonObject1, cursor1);
                fileos.write(jsonObject1.toString().getBytes());
                hasItems1 = true;
                progress++;
                mainProgressBar.setProgress(progress);
            } while (cursor1.moveToNext());
            cursor1.close();
        }
        fileos.write("],".getBytes());


        //cursor2 = contentResolver.query(Uri.parse("content://mms/"), null, null, null, null);
        fileos.write("\"mms\":[".getBytes());
        if (!cursor2.moveToFirst()) {
            Log.e(TAG, "No mms to export");
        } else {
            hasItems1 = false;
            do { // looping over mms
                String id = cursor2.getString(cursor2.getColumnIndex(("_id")));
                byte[] b = new byte[3000]; // buffer for reading MMS part data (images, video)
                jsonObject1 = new JSONObject();
                jsonArray1 = new JSONArray();
                jsonArray2 = new JSONArray();
                addJsonFields(jsonObject1, cursor2);
                jsonObject1.put("parts", jsonArray1);
                jsonObject1.put("addresses", jsonArray2);

                // Get sender, just push each address onto an addresses list for now
                String selectionAddress = new String("msg_id=" + id);
                Uri uriAddress = Uri.parse(MessageFormat.format("content://mms/{0}/addr", id));
                Cursor addresses = getContentResolver().query(uriAddress, null, selectionAddress, null, null);
                String name = null;
                if (addresses.moveToFirst()) {
                    do {
                        JSONObject jsonObject3 = new JSONObject();
                        addJsonFields(jsonObject3, addresses);
                        jsonArray2.put(jsonObject3);
                    } while (addresses.moveToNext());
                }
                if (addresses != null) {
                    addresses.close();
                }



                // Now get MMS parts
                String selectionPart = "mid=" + id;
                Cursor parts = getContentResolver().query(Uri.parse("content://mms/part"), null, selectionPart, null, null);
                if (!parts.moveToFirst()) {
                    Log.e(TAG, "No parts for mid " + id);
                    parts.close();
                    continue;
                }

                if (hasItems1) {
                    fileos.write(",".getBytes());
                }


                do { // looping over parts in this mms message
                    JSONObject jsonObject2 = new JSONObject();
                    // TODO: feel we need to append text parts in order
                    String partId = parts.getString(parts.getColumnIndex("_id"));
                    String partType = parts.getString(parts.getColumnIndex("ct"));

                    addJsonFields(jsonObject2, parts);
                    jsonArray1.put(jsonObject2);

                    if ("text/plain".equals(partType)) {
                        String data = parts.getString(parts.getColumnIndex("_data"));
                        String body;
                        if (data != null) {
                            InputStream is = null;
                            StringBuilder sb = new StringBuilder();
                            try {
                                is = getContentResolver().openInputStream(Uri.parse("content://mms/part/" + partId));
                                if (is != null) {
                                    InputStreamReader isr = new InputStreamReader(is, "UTF-8");
                                    BufferedReader reader = new BufferedReader(isr);
                                    String temp = reader.readLine();
                                    while (temp != null) {
                                        sb.append(temp);
                                        temp = reader.readLine();
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Exception while fetching contents for message " + id);
                                if (is != null) {
                                    is.close();
                                }
                                continue;
                            }
                            is.close();
                            body = sb.toString();
                        } else {
                            body = parts.getString(parts.getColumnIndex("text"));
                        }
                        jsonObject2.put("body", body);


                    } else if ("image/jpeg".equals(partType) || "image/bmp".equals(partType) ||
                            "image/gif".equals(partType) || "image/jpg".equals(partType) ||
                            "image/png".equals(partType) ||
                            "video/3gpp".equals(partType) ||
                            "video/mp4".equals(partType) ) {
                        InputStream is = null;
                        Log.e(TAG, "Fetching data for part " + partId);
                        try {
                            is = contentResolver.openInputStream(Uri.parse("content://mms/part/" + partId));
                        } catch (FileNotFoundException e) {
                            Log.e(TAG, "Part " + partId + " data not found for message " + id);
                            if (is != null) {
                                is.close();
                            }
                            continue;
                        } catch (Exception e) {
                            Log.e(TAG, "Exception while handling data for part " + partId);
                            Log.e(TAG, Log.getStackTraceString(e));
                            if (is != null) {
                                is.close();
                            }
                            continue;
                        }
                        // TODO: perhaps try incremental base64 encoding
                        jsonObject2.put("base64", Base64.encodeToString(readBytes(is), Base64.DEFAULT));
                    }
                } while (parts.moveToNext());
                parts.close();

                fileos.write(jsonObject1.toString().getBytes());
                hasItems1 = true;
                progress++;
                mainProgressBar.setProgress(progress);

            } while (cursor2.moveToNext());
            cursor2.close();
            fileos.write("]".getBytes());
        }


        fileos.write("}".getBytes());
        fileos.close();
        Log.e(TAG, "Done");
    }

}
