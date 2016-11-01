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

    private HashMap<String, byte[]  > classData  = new HashMap<String, byte[]  >();
    private HashMap<String, Class<?>> classCache = new HashMap<String, Class<?>>();

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
        ByteArrayOutputStream fileOut = new ByteArrayOutputStream();
        while((currentEntry = jarIn.getNextJarEntry()) != null) {
            String fileName = currentEntry.getName();
            if(fileName.endsWith(".class")) {
                int len;
                while((len = jarIn.read(buffer)) != -1) fileOut.write(buffer, 0, len);
                String className = fileName.substring(0, fileName.length() - 6).replace("/", ".");
                classData.put(className, fileOut.toByteArray());
                fileOut.reset();
            }
        }
    }

    private void startPackedProgram(String[] args) {
        try {
            loadPack200(getClass().getClassLoader().getResourceAsStream("moe/lymia/mppatch/installer.pack"));
        } catch (IOException e) {
            throw error("Could not parse pack200 contents.", e);
        }

        Class<?> clazz;
        try {
            clazz = Class.forName(mainClass, false, this);
        } catch (ClassNotFoundException e) {
            throw error("Main-Class not found.", e);
        }

        Method m;
        try {
            m = clazz.getMethod("main", String[].class);
        } catch (NoSuchMethodException e) {
            throw error("main method not found.", e);
        }

        if(m == null) throw error("method is null", null);

        try {
            m.invoke(null, new Object[] { args });
        } catch (InvocationTargetException e) {
            throw error("Failed to call main method.", e);
        } catch (IllegalAccessException e) {
            throw error("Failed to call main method.", e);
        }
    }

    private static RuntimeException error(String error, Exception e) {
        JOptionPane.showMessageDialog(null, "Could not start MPPatch installer:\n"+error, "MPPatch Installer",
                                      JOptionPane.ERROR_MESSAGE);
        return new RuntimeException(error, e);
    }

    private static boolean isJava8() {
        String version = System.getProperty("java.version");
        String[] components = version.split("\\.");
        return Integer.parseInt(components[0]) > 1 ||
               (Integer.parseInt(components[0]) == 1 && Integer.parseInt(components[1]) >= 8);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Warning: Failed to set system Look and Feel.");
            e.printStackTrace();
        }

        boolean isJava8;
        try {
            isJava8 = isJava8();
        } catch (Exception e) {
            throw error("Could not parse JVM version", e);
        }

        if(!isJava8) throw error("Java 1.8 or later is required to run this program.", null);

        new Loader().startPackedProgram(args);
    }
}
