/*
This file is part of leafdigital kanjirecog.

kanjirecog is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

kanjirecog is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with kanjirecog.  If not, see <http://www.gnu.org/licenses/>.

Copyright 2011 Samuel Marshall.
*/
package com.leafdigital.kanji;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.LinkedList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Reads kanji stroke order data from a file in KanjiVG format and simplifies
 * it into the basic stroke data used for recognition.
 * http://kanjivg.tagaini.net/
 */
public class KanjiVgLoader
{
    private InputStream input;
    private LinkedList<KanjiInfo> read = new LinkedList<KanjiInfo>();
    private LinkedList<String> warnings = new LinkedList<String>();
    private HashSet<Integer> done = new HashSet<Integer>();

    /**
     * SAX handler.
     */
    private class Handler extends DefaultHandler
    {
        private KanjiInfo current = null;

        @Override
        public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException
        {
            if(qName.equals("kanji"))
            {
                // Clear current just in case
                current = null;

                // Note: I used the midashi attribute initially, but had problems
                // with the parser bizarrely misinterpreting some four-byte sequences.
                String id = attributes.getValue("id");
                if(id == null)
                {
                    warnings.add("<kanji> tag missing id=");
                    return;
                }
                int indexOfUnderscore = id.indexOf("_");
                if(indexOfUnderscore == -1)
                {
                    warnings.add("Id with wrong format id= (" + id + ")");
                    return;
                }
                String codePointStr = id.substring(indexOfUnderscore + 1);
                int codePoint;
                try
                {
                    codePoint = Integer.parseInt(codePointStr, 16);
                }
                catch(NumberFormatException e)
                {
                    warnings.add("<kanji> tag invalid id= (" + id + ")");
                    return;
                }
                if(!done.add(codePoint))
                {
                    warnings.add("<kanji> duplicate id= (" + id + ")");
                    return;
                }

                // Check if code point is actually a CJK ideograph
                String kanjiString = new String(Character.toChars(codePoint));
                if((codePoint >= 0x4e00 && codePoint <= 0x9fff)
                    || (codePoint >= 0x3400 && codePoint <= 0x4dff)
                    || (codePoint >= 0x20000 && codePoint <= 0x2a6df)
                    || (codePoint >= 0xf900 && codePoint <= 0xfaff)
                    || (codePoint >= 0x2f800 && codePoint <= 0x2fa1f))
                {
                    current = new KanjiInfo(kanjiString);
                }
                else
                {
                    // Ignore non-kanji characters
                    return;
                }
            }
            else if(qName.equals("path"))
            {
                if(current != null)
                {
                    String path = attributes.getValue("d");
                    if(path == null)
                    {
                        warnings.add("<path> tag in kanji " +
                            current.getKanji() + " missing d=, ignoring kanji");
                        current = null;
                        return;
                    }
                    try
                    {
                        InputStroke stroke = new InputStroke(path);
                        current.addStroke(stroke);
                    }
                    catch(IllegalArgumentException e)
                    {
                        warnings.add("<path> tag in kanji " + current.getKanji() +
                            " invalid d= (" + path + "): " + e.getMessage());
                        current = null;
                        return;
                    }
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName)
            throws SAXException
        {
            if(qName.equals("kanji"))
            {
                if(current != null)
                {
                    current.finish();
                    read.add(current);
                }
            }
        }
    }

    /**
     * Constructs ready to read data.
     * @param input Input stream (will be closed after {@link #loadKanji()}
     *   finishes)
     */
    public KanjiVgLoader(InputStream input)
    {
        this.input = input;
    }

    /**
     * Loads all kanji from the file and closes it.
     * @return All kanji as array
     * @throws IOException Any error reading data or with format
     */
    public synchronized KanjiInfo[] loadKanji() throws IOException
    {
        if(input == null)
        {
            throw new IOException("Cannot load kanji more than once");
        }

        // Parse data
        SAXParser parser;
        try
        {
            parser = SAXParserFactory.newInstance().newSAXParser();
            parser.parse(input, new Handler());
            input.close();
        }
        catch(ParserConfigurationException e)
        {
            IOException x = new IOException("Failed to initialise SAX parser");
            x.initCause(e);
            throw x;
        }
        catch(SAXException e)
        {
            IOException x = new IOException("Failed to parse KanjiVG file");
            x.initCause(e);
            throw x;
        }

        // Return result
        return read.toArray(new KanjiInfo[read.size()]);
    }

    /**
     * @return All warnings encountered while loading the file
     */
    public synchronized String[] getWarnings()
    {
        return warnings.toArray(new String[warnings.size()]);
    }
}
