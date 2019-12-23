package com.leafdigital.kanji.android;

import android.inputmethodservice.InputMethodService;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.TextView;

import com.leafdigital.kanji.KanjiInfo;

public class KanjiDrawIME extends InputMethodService implements AsyncKanjiList.KanjiUpdateListener {
    private AsyncKanjiList kanji;
    private KanjiInfo.MatchAlgorithm algorithm;
    private Button undoButton;
    private Button clearButton;
    private KanjiDrawing drawView;
    private boolean loaded;
    private ViewGroup candidates;

    @Override
    public void onCreate() {
        super.onCreate();

        // TODO allow search algorithm selection
        algorithm = KanjiInfo.MatchAlgorithm.STRICT;

        // load the kanji assets on service creation
        kanji = new AsyncKanjiList(getBaseContext());
        kanji.addKanjiUpdateListener(this);
        kanji.loadList();
    }

    @Override
    public View onCreateInputView() {
        View inputView = getLayoutInflater().inflate(R.layout.keyboard, null);
        final TextView strokesView = inputView.findViewById(R.id.strokes);
        drawView = inputView.findViewById(R.id.drawcontainer);

        clearButton = inputView.findViewById(R.id.clear);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawView.clear();
            }
        });
        undoButton = inputView.findViewById(R.id.undo);
        undoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawView.undo();
            }
        });
        final Button backspaceButton = inputView.findViewById(R.id.backspace);
        backspaceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputConnection ic = getCurrentInputConnection();
                ic.deleteSurroundingText(1, 0);
            }
        });

        drawView.setListener(new KanjiDrawing.Listener() {
            @Override
            public void strokes(DrawnStroke[] strokes) {
                strokesView.setText(String.valueOf(strokes.length));
                clearButton.setEnabled(strokes.length > 0);
                undoButton.setEnabled(strokes.length > 0);
                kanji.lookupKanji(strokes, algorithm);
            }
        });
        drawView.setEnabled(loaded);

        candidates = inputView.findViewById(R.id.matches);

        return inputView;
    }

    // Doesn't seem to scale correctly, but we don't really need it anyway.
    // The candidates view is part of the main layout and will always be displayed.
    /*@Override
    public View onCreateCandidatesView() {
        View candidateView = getLayoutInflater().inflate(R.layout.candidates, null);
        candidates = candidateView.findViewById(R.id.matches);
        return candidateView;
    }*/

    @Override
    public void onKanjiLoaded() {
        Log.d(getClass().getName(), "Kanji list loaded");
        loaded = true;
        if (drawView != null) {
            drawView.setEnabled(loaded);
        }
        if (clearButton != null) {
            clearButton.setEnabled(loaded);
        }
        if (undoButton != null) {
            undoButton.setEnabled(loaded);
        }
    }

    @Override
    public void onProgressUpdated(int progress, int results) {
        Log.d(getClass().getName(), String.format("Lookup progress: %d (%d results)", progress, results));
    }

    @Override
    public void onKanjiFound(String[] matches) {
        candidates.removeAllViews();
        //StringBuilder joiner = new StringBuilder();
        for (String match : matches) {
            //joiner.append(match);
            //joiner.append(",");
            candidates.addView(createCandidateButton(match));
        }
        //setCandidatesViewShown(matches.length > 0);
        //Log.d(getClass().getName(), String.format("Found [%s]", joiner.toString()));
    }

    private View createCandidateButton(final String match) {
        View v = getLayoutInflater().inflate(R.layout.candidatebutton, null);
        Button ret = v.findViewById(R.id.candidate);
        ret.setText(match);
        ret.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputConnection ic = getCurrentInputConnection();
                ic.commitText(match, 1);
            }
        });
        return ret;
    }
}
