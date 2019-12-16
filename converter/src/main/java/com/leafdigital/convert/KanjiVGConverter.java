package com.leafdigital.convert;

import com.leafdigital.kanji.KanjiInfo;
import com.leafdigital.kanji.KanjiList;
import com.leafdigital.kanji.KanjiVgLoader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class KanjiVGConverter {

    /**
     * Convert KanjiVG file into new info file.
     * @param args Filename to convert and output filename
     */
    public static void main(String[] args)
    {
        if(args.length < 2 || args.length > 3
            || (args.length==3 && !args[2].matches("[0-9]{1,5}")))
        {
            System.err.println("Incorrect command line arguments. Syntax:\n"
                + "KanjiVgLoader <kanjivgfile> <output file> [max size in kb]\n"
                + "Max size is used to optionally split the file into multiple\n"
                + "parts.");
            return;
        }

        File in = new File(args[0]);
        if(!in.canRead())
        {
            System.err.println("Unable to read input file: " + args[0]);
            return;
        }

        File out;
        int maxBytes = -1;
        String fileName = args[1];
        if(args.length == 3)
        {
            maxBytes = Integer.parseInt(args[2]) * 1024;

            out = new File(fileName + ".1");
            if(out.exists())
            {
                System.err.println("Output file already exists: " + fileName + ".1");
                return;
            }
        }
        else
        {
            out = new File(fileName);
            if(out.exists())
            {
                System.err.println("Output file already exists: " + fileName);
                return;
            }
        }

        try
        {
            // Load everything
            KanjiVgLoader loader = new KanjiVgLoader(new BufferedInputStream(
                new FileInputStream(in)));
            System.out.println("Loading input file: " + in.getName());
            KanjiInfo[] allKanji = loader.loadKanji();
            System.out.println("Loaded " + allKanji.length + " kanji.");
            System.out.println();
            if(loader.getWarnings().length > 0)
            {
                System.out.println("Warnings:");
                for(String warning : loader.getWarnings())
                {
                    System.out.println("  " + warning);
                }
                System.out.println();
            }

            KanjiList list = new KanjiList();
            for(KanjiInfo kanji : allKanji)
            {
                list.add(kanji);
            }

            OutputStream stream;
            if(maxBytes == -1)
            {
                System.out.println("Writing output file: " + out.getName());
                stream = new FileOutputStream(out);
            }
            else
            {
                System.out.println("Writing output files: " + fileName + ".*");
                stream = new SplitOutputStream(fileName, maxBytes);
            }
            list.save(stream, in.getName());
            stream.close();
        }
        catch(IOException e)
        {
            System.err.println("Error processing file: " + e.getMessage());
            System.err.println();
            System.err.println("FULL STACK TRACE:");
            System.err.println();
            e.printStackTrace();
        }
    }
}
