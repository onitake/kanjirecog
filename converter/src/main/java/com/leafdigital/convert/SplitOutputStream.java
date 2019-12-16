package com.leafdigital.convert;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Output stream capable of writing multiple files; the first has .1, the
 * second .2, etc.
 */
public class SplitOutputStream extends OutputStream
{
    private String basePath;
    private int maxBytes;

    private OutputStream out;
    private int bytesLeft;
    private int index;

    public SplitOutputStream(String basePath, int maxBytes) throws IOException
    {
        this.basePath = basePath;
        this.maxBytes = maxBytes;
        index = 0;
    }

    @Override
    public void close() throws IOException
    {
        if(out != null)
        {
            out.close();
            out = null;
        }
    }

    private void checkOutput() throws IOException
    {
        if(out == null)
        {
            index++;
            File file = new File(basePath + "." + index);
            if(file.exists())
            {
                throw new IOException("File already exists: " + file.getPath());
            }
            out = new FileOutputStream(file);
            bytesLeft = maxBytes;
        }
    }

    private void wroteBytes(int wrote) throws IOException
    {
        bytesLeft -= wrote;
        if(bytesLeft <= 0)
        {
            out.close();
            out = null;
        }
    }

    @Override
    public void write(int oneByte) throws IOException
    {
        checkOutput();
        out.write(oneByte);
        wroteBytes(1);
    }

    @Override
    public void write(byte[] buffer, int offset, int count) throws IOException
    {
        checkOutput();
        if(count > bytesLeft)
        {
            int wasLeft = bytesLeft;
            write(buffer, offset, bytesLeft);
            write(buffer, offset + wasLeft, count - wasLeft);
            return;
        }
        out.write(buffer, offset, count);
        wroteBytes(count);
    }

    @Override
    public void write(byte[] buffer) throws IOException
    {
        write(buffer, 0, buffer.length);
    }
}
