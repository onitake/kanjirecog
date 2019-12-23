package com.leafdigital.kanji.android;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.leafdigital.kanji.InputStroke;
import com.leafdigital.kanji.KanjiInfo;
import com.leafdigital.kanji.KanjiList;
import com.leafdigital.kanji.KanjiMatch;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class AsyncKanjiList {
    private static final String STROKES_FILE = "strokes.xml";

    private final Context context;
    private final Set<KanjiUpdateListener> listeners;

    private KanjiList list;
    private boolean listLoading;

    public AsyncKanjiList(Context context) {
        this.context = context;
        listeners = new HashSet<>();
    }

    public void addKanjiUpdateListener(KanjiUpdateListener listener) {
        listeners.add(listener);
    }

    public void removeKanjiUpdateListener(KanjiUpdateListener listener) {
        listeners.remove(listener);
    }

    public boolean isListLoaded() {
        return list != null;
    }

    public boolean isListLoading() {
        return listLoading;
    }

    public void loadList() {
        if (isListLoaded()) {
            Log.i(getClass().getName(), "Kanji list already loaded");
            return;
        }
        if (isListLoading()) {
            Log.i(getClass().getName(), "Kanji list load in progress");
            return;
        }
        listLoading = true;
        try {
            InputStream input = context.getAssets().open(STROKES_FILE);
            new LoadTask(new LoadListener() {
                @Override
                public void listLoaded(KanjiList list) {
                    AsyncKanjiList.this.list = list;
                    listLoading = false;
                    for (KanjiUpdateListener listener : listeners) {
                        listener.onKanjiLoaded();
                    }
                }
            }).execute(input);
        } catch (IOException e) {
            Log.e(getClass().getName(), "Can't load kanji list", e);
            Toast.makeText(context, R.string.loaderror, Toast.LENGTH_LONG).show();
        }
    }

    public void lookupKanji(final DrawnStroke[] strokes, final KanjiInfo.MatchAlgorithm algo) {
        if (!isListLoaded()) {
            Log.w(getClass().getName(), "Kanji list not loaded yet");
            listeners.add(new KanjiUpdateListener() {
                @Override
                public void onKanjiLoaded() {
                    // defer lookup until list is loaded
                    listeners.remove(this);
                    AsyncKanjiList.this.lookupKanji(strokes, algo);
                }
                @Override
                public void onProgressUpdated(int progress, int results) { }
                @Override
                public void onKanjiFound(String[] matches) { }
            });
            loadList();
            return;
        }
        new MatchTask(list, algo, new MatchListener() {
            @Override
            public void progressUpdated(int done, int max) {
                for (final KanjiUpdateListener listener : listeners) {
                    listener.onProgressUpdated(done, max);
                }
            }
            @Override
            public void matchCompleted(String[] results) {
                for (final KanjiUpdateListener listener : listeners) {
                    listener.onKanjiFound(results);
                }
            }
        }).execute(strokes);
    }

    private interface LoadListener {
        void listLoaded(KanjiList list);
    }

    private static class LoadTask extends AsyncTask<InputStream, Void, KanjiList> {
        private final LoadListener listener;
        private LoadTask(LoadListener listener) {
            this.listener = listener;
        }
        @Override
        protected void onPostExecute(KanjiList result) {
            listener.listLoaded(result);
        }
        @Override
        protected KanjiList doInBackground(InputStream... inputs) {
            KanjiList loaded = null;
            try {
                long start = System.currentTimeMillis();
                Log.d(getClass().getName(), "Kanji drawing dictionary loading");
                loaded = new KanjiList(inputs[0]);
                long time = System.currentTimeMillis() - start;
                Log.d(getClass().getName(), "Kanji drawing dictionary loaded (" + time + "ms)");
            } catch (IOException e) {
                Log.e(getClass().getName(), "Error loading dictionary", e);
            }
            return loaded;
        }
    }

    private interface MatchListener {
        void progressUpdated(int done, int max);
        void matchCompleted(String[] results);
    }

    private static class MatchTask extends AsyncTask<DrawnStroke, Integer, String[]> {
        private final KanjiList list;
        private final KanjiInfo.MatchAlgorithm algo;
        private final MatchListener listener;
        private MatchTask(KanjiList list, KanjiInfo.MatchAlgorithm algo, MatchListener listener) {
            this.list = list;
            this.algo = algo;
            this.listener = listener;
        }
        @Override
        protected void onProgressUpdate(Integer... values) {
            listener.progressUpdated(values[0], values[1]);
        }

        @Override
        protected void onPostExecute(String[] result) {
            listener.matchCompleted(result);
        }
        @Override
        protected String[] doInBackground(DrawnStroke... strokes) {
            KanjiList.Progress progress = new KanjiList.Progress() {
                @Override
                public void progress(int done, int max) {
                    listener.progressUpdated(done, max);
                }
            };
            KanjiInfo info = getKanjiInfo(strokes);
            final KanjiMatch[] matches = list.getTopMatches(info, algo, progress);
            String[] chars = new String[matches.length];
            for (int i = 0; i < matches.length; i++) {
                chars[i] = matches[i].getKanji().getKanji();
            }
            return chars;
        }
    }

    /**
     * Converts from drawn strokes to the KanjiInfo object that
     * com.leafdigital.kanji classes expect.
     *
     * @param strokes Strokes
     * @return Equivalent KanjiInfo object
     */
    private static KanjiInfo getKanjiInfo(DrawnStroke[] strokes) {
        KanjiInfo info = new KanjiInfo("?");
        for (DrawnStroke stroke : strokes) {
            InputStroke inputStroke = new InputStroke(
                stroke.getStartX(), stroke.getStartY(),
                stroke.getEndX(), stroke.getEndY());
            info.addStroke(inputStroke);
        }
        info.finish();
        return info;
    }

    public interface KanjiUpdateListener {
        public void onKanjiLoaded();
        public void onProgressUpdated(int progress, int results);
        public void onKanjiFound(String[] matches);
    }
}
