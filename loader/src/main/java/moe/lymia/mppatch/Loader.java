/*
 * Copyright (c) 2015-2016 Lymia Alusyia <lymia@lymiahugs.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package moe.lymia.mppatch;

import javax.swing.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;

public class Loader extends ClassLoader {
    private static final Pack200.Unpacker unpacker = Pack200.newUnpacker();
    private static final int bufferSize = 1024 * 16;

    private HashMap<String, byte[]>   classData  = new HashMap<>();
    private HashMap<String, Class<?>> classCache = new HashMap<>();

    private String mainClass;

    private Class<?> loadClassData(String name) {
        if(classCache.containsKey(name)) return classCache.get(name);
        if(classData .containsKey(name)) {
            byte[] data = classData.get(name);
            Class<?> clazz = defineClass(name, data, 0, data.length);
            classData.remove(name);
            classCache.put(name, clazz);
            return clazz;
        }
        return null;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> clazz = loadClassData(name);
        if(clazz == null) return super.findClass(name);
        return clazz;
    }

    private void loadPack200(InputStream in) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        unpacker.unpack(in, new JarOutputStream(byteOut));
        JarInputStream jarIn = new JarInputStream(new ByteArrayInputStream(byteOut.toByteArray()));

        mainClass = jarIn.getManifest().getMainAttributes().getValue("Main-Class");

        JarEntry currentEntry;
        byte[] buffer = new byte[bufferSize];
        while((currentEntry = jarIn.getNextJarEntry()) != null) {
            String fileName = currentEntry.getName();
            if(fileName.endsWith(".class")) {
                ByteArrayOutputStream fileOut = new ByteArrayOutputStream();
                int len;
                while((len = jarIn.read(buffer)) != -1) fileOut.write(buffer, 0, len);
                String className = fileName.substring(0, fileName.length() - 6).replace("/", ".");
                classData.put(className, fileOut.toByteArray());
            }
        }
    }

    private void error(String error, Exception e) {
        JOptionPane.showMessageDialog(null, "Could not startPackedProgram MPPatch installer:\n"+e, "MPPatch Installer",
                                      JOptionPane.ERROR_MESSAGE);
        throw new RuntimeException(error, e);
    }

    private void startPackedProgram(String[] args) {
        try {
            loadPack200(getClass().getClassLoader().getResourceAsStream("moe/lymia/mppatch/installer.pack"));
        } catch (IOException e) {
            error("Could not parse pack200 contents.", e);
        }

        Class<?> clazz = null;
        try {
            clazz = Class.forName(mainClass, false, this);
        } catch (ClassNotFoundException e) {
            error("Main-Class not found.", e);
        }

        Method m = null;
        try {
            m = clazz != null ? clazz.getMethod("main", String[].class) : null;
        } catch (NoSuchMethodException e) {
            error("main method not found.", e);
        }

        if(m == null) error("method is null", null);

        try {
            m.invoke(null, new Object[] { args });
        } catch (IllegalAccessException | InvocationTargetException e) {
            error("Failed to call main method.", e);
        }
    }
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | UnsupportedLookAndFeelException |
                 IllegalAccessException | InstantiationException e) {
            System.err.println("Warning: Failed to set system Look and Feel.");
            e.printStackTrace();
        }

        new Loader().startPackedProgram(args);
    }
}
