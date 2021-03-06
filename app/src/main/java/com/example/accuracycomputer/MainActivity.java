package com.example.accuracycomputer;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private final int PICK_ORIGINAL_FILE_RESULT_CODE = 100;
    private final int PICK_RECOGNIZED_FILE_RESULT_CODE = 200;

    private TextView mText;

    private String mOriginalText;
    private String mRecognizedText;

    private int mK = 0;

    /**
     * Returns a minimal set of characters that have to be removed from (or added to) the respective
     * strings to make the strings equal.
     */
    public static Pair<String> diff(String a, String b) {
        return diffHelper(a, b, new HashMap<Long, Pair<String>>());
    }

    /**
     * Recursively compute a minimal set of characters while remembering already computed substrings.
     * Runs in O(n^2).
     */
    private static Pair<String> diffHelper(String a, String b, Map<Long, Pair<String>> lookup) {
        long key = ((long) a.length()) << 32 | b.length();
        if (!lookup.containsKey(key)) {
            Pair<String> value;
            if (a.isEmpty() || b.isEmpty()) {
                value = new Pair<>(a, b);
            } else if (a.charAt(0) == b.charAt(0)) {
                value = diffHelper(a.substring(1), b.substring(1), lookup);
            } else {
                Pair<String> aa = diffHelper(a.substring(1), b, lookup);
                Pair<String> bb = diffHelper(a, b.substring(1), lookup);
                if (aa.first.length() + aa.second.length() < bb.first.length() + bb.second.length()) {
                    value = new Pair<>(a.charAt(0) + aa.first, aa.second);
                } else {
                    value = new Pair<>(bb.first, b.charAt(0) + bb.second);
                }
            }
            lookup.put(key, value);
        }
        return lookup.get(key);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

        Button mOriginalButton = findViewById(R.id.original_button);
        Button mRecognizedButton = findViewById(R.id.recognized_button);
        //Find the view by its id
        mText = findViewById(R.id.text_view);

        mOriginalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseFile(PICK_ORIGINAL_FILE_RESULT_CODE);
            }
        });

        mRecognizedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseFile(PICK_RECOGNIZED_FILE_RESULT_CODE);
            }
        });

        //System.out.println(diff("this is a example", "this is a examp")); // prints (le,)
        //System.out.println(diff("Honda", "Hyundai")); // prints (o,yui)
        //System.out.println(diff("Toyota", "Coyote")); // prints (Ta,Ce)
        //System.out.println(diff("Flomax", "Volmax")); // prints (Fo,Vo)
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null)
            return;
        if (requestCode == PICK_ORIGINAL_FILE_RESULT_CODE) {
            if (resultCode == RESULT_OK) {
                String path = Objects.requireNonNull(data.getData()).getPath();
                List<String> pathList = Arrays.asList(Objects.requireNonNull(path).split("/"));
                //mOriginalText = readFile(pathList.get(pathList.size() - 1));
//                Uri url = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.original);
//
//                Log.d("aaaaaaaaaaaaaaa", url.getPath());
//                Log.d("bbbbbbbbbb", new File(url.toString()).getAbsolutePath());
//                mOriginalText = readFile(new File(url.toString()));

                try {
                    InputStream inputStream = getResources().openRawResource(R.raw.original_citylab);
                    File tempFile = File.createTempFile("pre", "suf");
                    copyFile(inputStream, new FileOutputStream(tempFile));

                    mOriginalText = readFile(tempFile);
                } catch (IOException e) {
                    throw new RuntimeException("Can't create temp file ", e);
                }
            }
        }
        if (requestCode == PICK_RECOGNIZED_FILE_RESULT_CODE) {
            if (resultCode == RESULT_OK) {
                String path = Objects.requireNonNull(data.getData()).getPath();
                List<String> pathList = Arrays.asList(Objects.requireNonNull(path).split("/"));
                // mRecognizedText = readFile(pathList.get(pathList.size() - 1));
//                Uri url = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.test);
//                mRecognizedText = readFile(new File(url.toString()));

                try {
                    InputStream inputStream = getResources().openRawResource(R.raw.test_citylab);
                    File tempFile = File.createTempFile("pre", "suf");
                    copyFile(inputStream, new FileOutputStream(tempFile));

                    mRecognizedText = readFile(tempFile);
                } catch (IOException e) {
                    throw new RuntimeException("Can't create temp file ", e);
                }

                if (mK == 2) {
                    mText.append("\n\n\naccuracy:\n\n\n");

                    int m = 10;
                    List<Float> prs = new ArrayList<>();
                    for (int i = 0;i<(m-1);i++)
                    {
                        float pr = (1 - ((float)diff(mOriginalText.substring((i*(mOriginalText.length()) / m), ((i+1)*mOriginalText.length()) / m),
                        mRecognizedText.substring((i*(mRecognizedText.length()) / m), ((i+1)*mRecognizedText.length()) / m)).first.length()) / ((float) mOriginalText.length() / m));
                        prs.add(pr);
                    }

                    float sum = 0;
                    for (int j = 0;j< m - 1;j++) {
                        sum += prs.get(j);
                    }

                    mText.append("                     " + sum/(m-1));
                }
            }
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    private String readFile(File file) {
        if (mK % 2 == 0)
            mText.setText("");
        //File sdcard = Environment.getExternalStorageDirectory();
        //Get the text file
        //File file = new File(sdcard, fileName);

        //Read text from file
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(file));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            String line = null;

            while (true) {
                try {
                    if ((line = Objects.requireNonNull(br).readLine()) == null) break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                text.append(line);
                text.append('\n');
            }
            br.close();
        } catch (IOException e) {
            //You'll need to add proper error handling here
        }

        if (mText.getText().length() != 0) {
            mText.append("\n\n\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n\n\n");
            mText.append(text.toString());
        } else {
            //Set the text
            mText.setText(text.toString());
        }

        mK++;

        return text.toString();
    }

    private void chooseFile(int result_code) {
        Intent file_intent = new Intent(Intent.ACTION_GET_CONTENT);
        file_intent.setType("text/plain");
        try {
            startActivityForResult(file_intent, result_code);
        } catch (ActivityNotFoundException e) {
            Log.e("tag", "No activity can handle picking a file. Showing alternatives.");
        }

    }

    public static class Pair<T> {
        final T first, second;

        Pair(T first, T second) {
            this.first = first;
            this.second = second;
        }
    }
}


